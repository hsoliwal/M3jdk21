// SPDX-License-Identifier: Apache-2.0
package com.m3.migration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact-source, class-backed Maven recipe. Never edits a source checkout or overwrites drift. */
public final class M3PortRecipe {
  private static final String MANIFEST = "m3/docs/name-mapping.json";
  private static final String STATE = "m3/migration/.state/";
  private static final String LICENSE = "// SPDX-License-Identifier: Apache-2.0\n";
  private static final int FILE_LIMIT = 16 * 1024 * 1024;
  private M3PortRecipe() {}

  public static void main(String[] args) throws Exception {
    if (args.length < 4 || args.length > 5 || (args.length == 5 && !args[4].equals("--snapshot"))) {
      throw new IllegalArgumentException("usage: <plan|apply|check|rollback|extract> <bundle> <source-or-destination> <target> [--snapshot]");
    }
    Path bundle = Path.of(args[1]);
    Path source = args[2].equals("-") ? null : Path.of(args[2]);
    Path target = Path.of(args[3]);
    if (args[0].equals("extract")) extractSources(bundle, target, source);
    else execute(args[0], bundle, source, target, args.length == 5);
  }

  public static void execute(String action, Path bundle, Path source, Path target, boolean snapshot) throws Exception {
    if (!Set.of("plan", "apply", "check", "rollback").contains(action)) throw new IllegalArgumentException("unknown action: " + action);
    bundle = root(bundle); target = root(target);
    Plan plan = load(bundle);
    if (action.equals("check")) {
      for (Entry entry : plan.entries) {
        Path file = safe(target, entry.targetPath);
        requireHash(read(file), entry.targetHash, "target " + entry.targetPath);
      }
      System.out.println("CHECK " + plan.entries.size() + " exact outputs; source checkout and runtime acceptance not implied");
      return;
    }
    if (action.equals("rollback")) { rollback(plan, target); return; }
    source = root(source);
    if (source.equals(target)) throw new IllegalArgumentException("source and target roots must differ");
    if (!snapshot) verifyCommit(source, plan.sourceCommit);
    List<Output> outputs = prepare(plan, source, target);
    if (action.equals("plan")) {
      for (Output output : outputs) System.out.println((output.existing ? "UNCHANGED " : "ADD ") + output.entry.targetPath);
      return;
    }
    Path state = safe(target, STATE + "port-journal.json");
    Files.createDirectories(state.getParent());
    try (FileChannel channel = FileChannel.open(safe(target, STATE + "port.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
         FileLock lock = channel.tryLock()) {
      if (lock == null) throw new IOException("another migration recipe holds the workspace lock");
      outputs = prepare(plan, source, target); // Recheck after acquiring the cooperative workspace lock.
      Map<String, Object> previous = Files.exists(state) ? Json.object(Json.parse(text(read(state)))) : null;
      List<Object> owned = new ArrayList<>();
      if (previous != null) {
        if (!plan.digest.equals(Json.string(previous.get("plan_sha256")))) throw new IllegalArgumentException("journal belongs to a different recipe plan");
        if (!"rolled_back".equals(previous.get("phase"))) owned.addAll(Json.array(previous.get("owned")));
      }
      Set<String> ownedPaths = new LinkedHashSet<>();
      for (Object item : owned) ownedPaths.add(Json.string(Json.object(item).get("path")));
      for (Output output : outputs) if (!output.existing && ownedPaths.add(output.entry.targetPath)) {
        owned.add(Map.of("path", output.entry.targetPath, "sha256", output.entry.targetHash));
      }
      boolean complete = outputs.stream().allMatch(Output::existing);
      if (complete && previous != null && "applied".equals(previous.get("phase"))) {
        System.out.println("FIXED_POINT " + outputs.size() + " outputs; journal preserved"); return;
      }
      Map<String, Object> journal = journal(plan, owned, "prepared");
      writeJournal(state, journal);
      int written = 0;
      for (Output output : outputs) if (!output.existing) {
        createOnly(safe(target, output.entry.targetPath), output.bytes); written++;
      }
      journal.put("phase", "applied"); writeJournal(state, journal);
      System.out.println("APPLY " + written + " files; " + (outputs.size() - written) + " preserved; source verification=" + (snapshot ? "file-hashes-only" : "exact-checkout-head"));
    }
  }

  /** Recover pinned production preimages from a verified port for differential/replay tests. */
  public static void extractSources(Path bundle, Path target, Path destination) throws Exception {
    bundle = root(bundle); target = root(target); destination = root(destination);
    if (destination.equals(target) || destination.equals(bundle)) throw new IllegalArgumentException("extraction needs a separate workspace");
    Plan plan = load(bundle);
    Map<Path, byte[]> originals = new LinkedHashMap<>();
    for (Entry entry : plan.entries) if (entry.sourcePath != null) {
      byte[] postimage = read(safe(target, entry.targetPath));
      requireHash(postimage, entry.targetHash, "postimage " + entry.targetPath);
      String original = text(postimage);
      List<Operation> reverse = new ArrayList<>(entry.operations); Collections.reverse(reverse);
      for (Operation operation : reverse) original = replaceExactlyOnce(original, operation.after, operation.before);
      byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
      requireHash(bytes, entry.sourceHash, "reconstructed source " + entry.sourcePath);
      Path output = safe(destination, entry.sourcePath);
      byte[] previous = originals.putIfAbsent(output, bytes);
      if (previous != null && !java.util.Arrays.equals(previous, bytes)) throw new IllegalArgumentException("conflicting source lineage");
      if (Files.exists(output)) requireHash(read(output), entry.sourceHash, "existing extracted source");
    }
    for (var entry : originals.entrySet()) if (!Files.exists(entry.getKey())) createOnly(entry.getKey(), entry.getValue());
    System.out.println("EXTRACT " + originals.size() + " hash-verified production preimages; no repository ancestry claim");
  }

  private static Plan load(Path bundle) throws IOException {
    Map<String, Object> document = Json.object(Json.parse(text(read(safe(bundle, MANIFEST)))));
    if (Json.integer(document.get("schema")) != 1) throw new IllegalArgumentException("unsupported name-map schema");
    Map<String, Object> migration = Json.object(document.get("migration"));
    if (Json.integer(migration.get("schema_version")) != 1) throw new IllegalArgumentException("unsupported migration schema");
    String commit = Json.string(migration.get("source_commit"));
    if (!commit.matches("[0-9a-f]{40}")) throw new IllegalArgumentException("source commit must be pinned");
    List<Entry> entries = new ArrayList<>();
    Set<String> paths = new LinkedHashSet<>(), ids = new LinkedHashSet<>();
    List<Object> executable = new ArrayList<>();
    for (Object item : Json.array(document.get("mappings"))) {
      Map<String, Object> mapping = Json.object(item);
      if (!mapping.containsKey("recipe") || mapping.get("recipe") == null) continue;
      Map<String, Object> recipe = Json.object(mapping.get("recipe"));
      String mode = Json.string(recipe.get("mode"));
      if (!Set.of("port", "generate").contains(mode)) throw new IllegalArgumentException("unsupported executable recipe mode");
      String id = Json.string(mapping.get("id"));
      if (!ids.add(id)) throw new IllegalArgumentException("duplicate executable mapping ID " + id);
      List<Object> targets = Json.array(mapping.get("target_refs"));
      if (targets.size() != 1) throw new IllegalArgumentException("this file-atom recipe requires one target per executable mapping");
      Map<String, Object> target = Json.object(targets.get(0));
      String targetPath = relative(Json.string(target.get("path")));
      String targetHash = sha(Json.string(target.get("sha256")));
      if (!paths.add(targetPath)) throw new IllegalArgumentException("duplicate target path " + targetPath);
      List<Operation> operations = new ArrayList<>();
      String sourcePath = null, sourceHash = null;
      byte[] template = null;
      if (mode.equals("port")) {
        List<Object> sources = Json.array(mapping.get("source_refs"));
        if (sources.size() != 1) throw new IllegalArgumentException("source-port atom requires one pinned source");
        Map<String, Object> source = Json.object(sources.get(0));
        sourcePath = relative(Json.string(source.get("path")));
        sourceHash = sha(Json.string(source.get("sha256")));
        if (recipe.get("patch_path") != null) {
          byte[] patch = read(safe(bundle, Json.string(recipe.get("patch_path"))));
          requireHash(patch, sha(Json.string(recipe.get("patch_sha256"))), "recipe patch");
          Map<String, Object> patchDoc = Json.object(Json.parse(text(patch)));
          for (Object operation : Json.array(patchDoc.get("operations"))) {
            Map<String, Object> change = Json.object(operation);
            operations.add(new Operation(Json.string(change.get("before")), Json.string(change.get("after"))));
          }
        }
      } else {
        template = read(safe(bundle, Json.string(recipe.get("template_path"))));
        requireHash(template, sha(Json.string(recipe.get("template_sha256"))), "generation template");
        requireHash(template, targetHash, "generated output declaration");
      }
      Map<String, Object> identity = new LinkedHashMap<>();
      identity.put("id", id); identity.put("mode", mode);
      identity.put("recipe_id", Json.string(recipe.get("id"))); identity.put("recipe_version", Json.string(recipe.get("version")));
      identity.put("source_path", sourcePath); identity.put("source_sha256", sourceHash);
      identity.put("target_path", targetPath); identity.put("target_sha256", targetHash);
      identity.put("patch_sha256", recipe.get("patch_sha256"));
      identity.put("template_sha256", recipe.get("template_sha256"));
      executable.add(identity);
      entries.add(new Entry(id, sourcePath, sourceHash, targetPath, targetHash, List.copyOf(operations), template));
    }
    if (entries.isEmpty()) throw new IllegalArgumentException("no executable mappings");
    entries.sort(Comparator.comparing(Entry::targetPath));
    return new Plan(commit, hash(Json.write(Map.of("source_commit", commit, "entries", executable)).getBytes(StandardCharsets.UTF_8)), List.copyOf(entries));
  }

  private static List<Output> prepare(Plan plan, Path source, Path target) throws IOException {
    List<Output> result = new ArrayList<>();
    for (Entry entry : plan.entries) {
      byte[] bytes = entry.template;
      if (entry.sourcePath != null) {
        bytes = read(safe(source, entry.sourcePath));
        requireHash(bytes, entry.sourceHash, "source " + entry.sourcePath);
        String changed = text(bytes);
        if (!changed.startsWith(LICENSE)) throw new IllegalArgumentException("reviewed Apache-2.0 source header missing");
        for (Operation operation : entry.operations) changed = replaceExactlyOnce(changed, operation.before, operation.after);
        if (!changed.startsWith(LICENSE)) throw new IllegalArgumentException("recipe changed the source license header");
        bytes = changed.getBytes(StandardCharsets.UTF_8);
      }
      requireHash(bytes, entry.targetHash, "recipe output " + entry.targetPath);
      Path path = safe(target, entry.targetPath);
      boolean existing = Files.exists(path, LinkOption.NOFOLLOW_LINKS);
      if (existing) requireHash(read(path), entry.targetHash, "target drift " + entry.targetPath);
      result.add(new Output(entry, bytes, existing));
    }
    return result;
  }

  private static void rollback(Plan plan, Path target) throws Exception {
    Path state = safe(target, STATE + "port-journal.json");
    if (!Files.isRegularFile(state, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("no owned transaction journal; refusing deletion");
    try (FileChannel channel = FileChannel.open(safe(target, STATE + "port.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
         FileLock lock = channel.tryLock()) {
      if (lock == null) throw new IOException("workspace is locked");
      Map<String, Object> journal = Json.object(Json.parse(text(read(state))));
      if (!plan.digest.equals(Json.string(journal.get("plan_sha256")))) throw new IllegalArgumentException("journal/recipe mismatch");
      if ("rolled_back".equals(journal.get("phase"))) { System.out.println("ROLLBACK_FIXED_POINT"); return; }
      Map<String, String> allowed = new LinkedHashMap<>();
      for (Entry entry : plan.entries) allowed.put(entry.targetPath, entry.targetHash);
      List<Path> remove = new ArrayList<>();
      for (Object item : Json.array(journal.get("owned"))) {
        Map<String, Object> owned = Json.object(item);
        String path = Json.string(owned.get("path")), expected = Json.string(owned.get("sha256"));
        if (!expected.equals(allowed.get(path))) throw new IllegalArgumentException("journal contains an unowned output");
        Path file = safe(target, path);
        if (Files.exists(file)) { requireHash(read(file), expected, "rollback drift " + path); remove.add(file); }
      }
      for (Path file : remove) Files.delete(file);
      journal.put("phase", "rolled_back"); writeJournal(state, journal);
      System.out.println("ROLLBACK " + remove.size() + " recipe-owned outputs; pre-existing and unrelated files preserved");
    }
  }

  private static Map<String, Object> journal(Plan plan, List<Object> owned, String phase) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("schema", 1); result.put("plan_sha256", plan.digest); result.put("phase", phase); result.put("owned", owned);
    return result;
  }
  private static void writeJournal(Path path, Map<String, Object> journal) throws IOException {
    Path temporary = Files.createTempFile(path.getParent(), "journal-", ".tmp");
    try {
      Files.writeString(temporary, Json.write(journal) + "\n", StandardCharsets.UTF_8);
      try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
      catch (java.nio.file.AtomicMoveNotSupportedException unsupported) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
    } finally { Files.deleteIfExists(temporary); }
  }
  private static void createOnly(Path path, byte[] bytes) throws IOException {
    Files.createDirectories(path.getParent());
    // CREATE_NEW never overwrites a concurrently appearing file. A failed/crashed write
    // remains visible to strict hash checks; recovery refuses unknown partial bytes.
    try (FileChannel output = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
      ByteBuffer data = ByteBuffer.wrap(bytes);
      while (data.hasRemaining()) output.write(data);
      output.force(true);
    }
  }
  private static void verifyCommit(Path source, String expected) throws Exception {
    Process process = new ProcessBuilder("git", "-C", source.toString(), "rev-parse", "HEAD").redirectErrorStream(true).start();
    String actual = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    if (process.waitFor() != 0 || !actual.equals(expected)) throw new IllegalArgumentException("source HEAD is not " + expected + "; --snapshot verifies file hashes only");
  }
  static String replaceExactlyOnce(String input, String before, String after) {
    if (before.isEmpty()) throw new IllegalArgumentException("empty rewrite anchor");
    int at = input.indexOf(before);
    if (at < 0 || input.indexOf(before, at + 1) >= 0) throw new IllegalArgumentException("rewrite anchor is absent or ambiguous");
    return input.substring(0, at) + after + input.substring(at + before.length());
  }
  static Path root(Path path) throws IOException {
    if (path == null) throw new IllegalArgumentException("missing workspace root");
    Path root = path.toAbsolutePath().normalize();
    if (root.getParent() == null || Files.isSymbolicLink(root) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("expected an existing, non-root, non-symlink workspace");
    if (!root.equals(root.toRealPath())) throw new IllegalArgumentException("workspace ancestry contains symbolic aliases");
    return root;
  }
  static String relative(String value) {
    if (value.isEmpty() || value.indexOf('\\') >= 0 || value.indexOf(':') >= 0 || value.indexOf('\0') >= 0 || value.startsWith("/")) throw new IllegalArgumentException("unsafe relative path");
    for (String part : value.split("/", -1)) if (part.isEmpty() || part.equals(".") || part.equals("..") || part.equals(".git")) throw new IllegalArgumentException("unsafe path component");
    return value;
  }
  static Path safe(Path root, String value) throws IOException {
    relative(value); Path current = root;
    for (String part : value.split("/")) {
      current = current.resolve(part);
      if (Files.isSymbolicLink(current)) throw new IllegalArgumentException("symlink in recipe path: " + value);
    }
    if (!current.normalize().startsWith(root)) throw new IllegalArgumentException("path escapes workspace");
    return current;
  }
  static byte[] read(Path path) throws IOException {
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > FILE_LIMIT) throw new IOException("missing, non-regular or oversized recipe input: " + path);
    return Files.readAllBytes(path);
  }
  static String text(byte[] bytes) throws IOException {
    return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
  }
  static String hash(byte[] bytes) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
  }
  private static String sha(String value) {
    if (!value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("invalid SHA-256"); return value;
  }
  private static void requireHash(byte[] bytes, String expected, String label) {
    if (!hash(bytes).equals(expected)) throw new IllegalArgumentException(label + ": SHA-256 drift");
  }
  private record Operation(String before, String after) {}
  private record Entry(String id, String sourcePath, String sourceHash, String targetPath, String targetHash, List<Operation> operations, byte[] template) {}
  private record Output(Entry entry, byte[] bytes, boolean existing) {}
  private record Plan(String sourceCommit, String digest, List<Entry> entries) {}
}
