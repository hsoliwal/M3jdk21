// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Bounded SDK execution driver for this sealed crate, not a second recipe engine. */
public final class M3CollectionWrapperReconciliationApply {
  private static final String CRATE = System.getProperty("m3.wrapper.recipe.crate",
      M3CollectionWrapperReconciliationRecipe.CRATE);

  record Target(String kind, String path, String before, String after, String resource) {}
  record Pass(List<SourceFile> sources, List<Result> changes) {}
  record Applied(int changes, String receipt) {}

  private M3CollectionWrapperReconciliationApply() {}

  public static void main(String[] args) throws IOException {
    if (args.length == 2 && args[0].equals("--catalogue")) {
      catalogue(args[1]).forEach(System.out::println);
      return;
    }
    if (args.length != 2) {
      throw new IllegalArgumentException("usage: <target-root> <SDK-execution-receipt.tsv> or --catalogue <query>");
    }
    Path root = Path.of(args[0]).toRealPath();
    Path receipt = receiptPath(root, Path.of(args[1]));
    Applied result = apply(root);
    Files.createDirectories(receipt.getParent());
    Files.writeString(receipt, result.receipt(), StandardCharsets.UTF_8);
    System.out.println("SDK_EXECUTION first_pass_changes=" + result.changes()
        + " second_pass_changes=0; bounded Java/JNI receiver, no JDK phase advancement");
  }

  /** Literal case-insensitive search of the existing category catalogue; no new catalogue owner. */
  static List<String> catalogue(String query) {
    String key = java.util.Objects.requireNonNull(query, "query").toLowerCase(java.util.Locale.ROOT);
    return resource("text", "catalogue.tsv.txt").lines()
        .filter(line -> line.startsWith("#") || line.toLowerCase(java.util.Locale.ROOT).contains(key))
        .toList();
  }

  private static Path receiptPath(Path root, Path requested) throws IOException {
    Path absolute = requested.toAbsolutePath().normalize();
    if (Files.isSymbolicLink(absolute)) {
      throw new IllegalStateException("receipt must not be a symbolic link");
    }
    Path ancestor = absolute;
    while (!Files.exists(ancestor, LinkOption.NOFOLLOW_LINKS)) ancestor = ancestor.getParent();
    if (!ancestor.equals(absolute) && !Files.isDirectory(ancestor)) {
      throw new IllegalStateException("receipt parent must resolve to a directory");
    }
    Path destination = ancestor.toRealPath().resolve(ancestor.relativize(absolute)).normalize();
    if (destination.startsWith(root)) {
      throw new IllegalStateException("receipt must be outside the application target tree");
    }
    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
      if (!Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)) {
        throw new IllegalStateException("receipt must be a regular file");
      }
      for (Target target : targets()) {
        Path file = safePath(root, target.path());
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS) && Files.isSameFile(file, destination)) {
          throw new IllegalStateException("receipt must not alias an application target");
        }
      }
    }
    return destination;
  }

  static List<Target> targets() {
    List<Target> result = new ArrayList<>();
    for (String kind : List.of("java", "text")) {
      for (String line : resource(kind, "manifest.tsv").lines().toList()) {
        if (line.isBlank() || line.startsWith("#")) continue;
        String[] cells = line.split("\t", -1);
        if (cells.length != 4) throw new IllegalStateException("invalid sealed manifest");
        result.add(new Target(kind, cells[0], cells[1], cells[2], cells[3]));
      }
    }
    result.sort(java.util.Comparator.comparing(Target::path));
    if (result.stream().map(Target::path).distinct().count() != result.size()) {
      throw new IllegalStateException("Java/text target collision");
    }
    return List.copyOf(result);
  }

  static String resource(String kind, String name) {
    try (var stream = M3CollectionWrapperReconciliationApply.class.getResourceAsStream(
        "/com/synexia/rewrite/hash-pinned-" + kind + "/" + CRATE + "/" + name)) {
      if (stream == null) throw new IllegalStateException("missing fixture resource: " + name);
      return utf8(stream.readAllBytes());
    } catch (IOException failure) {
      throw new IllegalStateException("cannot read sealed fixture: " + name, failure);
    }
  }

  static SourceFile source(String path, String text) {
    if (path.endsWith(".java")) {
      List<Throwable> failures = new ArrayList<>();
      var context = new InMemoryExecutionContext(failures::add);
      List<SourceFile> parsed = JavaParser.fromJavaVersion()
          .classpath(JavaParser.runtimeClasspath()).build().parseInputs(
              List.of(Parser.Input.fromString(Path.of(path), text)), null, context).toList();
      if (!failures.isEmpty() || parsed.size() != 1
          || !(parsed.getFirst() instanceof J.CompilationUnit)
          || !parsed.getFirst().printAll().equals(text)) {
        throw new IllegalStateException("Java input parser refused: " + path);
      }
      return parsed.getFirst();
    }
    return PlainText.builder().sourcePath(Path.of(path)).text(text)
        .charsetName(StandardCharsets.UTF_8.name()).build();
  }

  static List<SourceFile> beforeSources() {
    List<SourceFile> result = new ArrayList<>();
    for (Target target : targets()) {
      if (!target.before().equals("ABSENT")) {
        String text = resource(target.kind(), "pre/" + target.resource());
        if (!sha(text).equals(target.before())) {
          throw new IllegalStateException("preimage fixture drift: " + target.path());
        }
        result.add(source(target.path(), text));
      }
    }
    return result;
  }

  static Pass run(List<SourceFile> before) {
    List<Throwable> failures = new ArrayList<>();
    var context = new InMemoryExecutionContext(failures::add);
    var run = new M3CollectionWrapperReconciliationRecipe(CRATE).run(
        new InMemoryLargeSourceSet(before), context, 1);
    if (!failures.isEmpty()) {
      throw new IllegalStateException("SDK recipe refused input: " + failures.getFirst(),
          failures.getFirst());
    }
    List<Result> changes = run.getChangeset().getAllResults();
    Map<String, SourceFile> after = new TreeMap<>();
    for (SourceFile source : before) after.put(path(source), source);
    for (Result change : changes) {
      if (change.getBefore() != null) after.remove(path(change.getBefore()));
      if (change.getAfter() != null) after.put(path(change.getAfter()), change.getAfter());
    }
    return new Pass(List.copyOf(after.values()), List.copyOf(changes));
  }

  static Applied apply(Path requestedRoot) throws IOException {
    Path root = requestedRoot.toRealPath();
    if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
      throw new IllegalStateException("target root must be a directory");
    }
    List<Target> targets = targets();
    List<SourceFile> before = new ArrayList<>();
    Map<String, String> observed = new LinkedHashMap<>();
    for (Target target : targets) {
      Path file = safePath(root, target.path());
      if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
          throw new IllegalStateException("target is not a regular file: " + target.path());
        }
        String text = utf8(Files.readAllBytes(file));
        before.add(source(target.path(), text));
        observed.put(target.path(), sha(text));
      } else {
        observed.put(target.path(), "ABSENT");
      }
    }

    // All refusal, exact postimage and fixed-point checks precede the first source write.
    Pass first = run(before);
    Map<String, SourceFile> after = new TreeMap<>();
    for (SourceFile source : first.sources()) after.put(path(source), source);
    if (after.size() != targets.size()) throw new IllegalStateException("unexpected SDK output set");
    for (Target target : targets) {
      SourceFile source = after.get(target.path());
      if (source == null || !sha(source.printAll()).equals(target.after())) {
        throw new IllegalStateException("SDK postimage drift: " + target.path());
      }
    }
    for (Result change : first.changes()) {
      if (change.getAfter() == null || !after.containsKey(path(change.getAfter()))) {
        throw new IllegalStateException("unexpected SDK deletion or path");
      }
    }
    Pass fixedPoint = run(first.sources());
    if (!fixedPoint.changes().isEmpty()) throw new IllegalStateException("SDK fixed point failed");

    for (Target target : targets) {
      Path file = safePath(root, target.path());
      String current = Files.exists(file, LinkOption.NOFOLLOW_LINKS)
          ? sha(utf8(Files.readAllBytes(file))) : "ABSENT";
      if (!current.equals(observed.get(target.path()))) {
        throw new IllegalStateException("source changed before write: " + target.path());
      }
    }
    for (Result change : first.changes()) {
      SourceFile source = change.getAfter();
      Path file = safePath(root, path(source));
      Files.createDirectories(file.getParent());
      Files.writeString(file, source.printAll(), StandardCharsets.UTF_8);
    }

    StringBuilder receipt = new StringBuilder(
        "recipe\tproof_scope\ttarget_path\tobserved_before_sha256\tsealed_before_sha256\t"
        + "sealed_after_sha256\tactual_after_sha256\tfirst_pass_changes\tsecond_pass_changes\t"
        + "whole_jdk_acceptance\tphase_advance\tperformance_superiority\n");
    for (Target target : targets) {
      String actual = sha(utf8(Files.readAllBytes(safePath(root, target.path()))));
      if (!actual.equals(target.after())) {
        throw new IllegalStateException("written postimage drift: " + target.path());
      }
      receipt.append(M3CollectionWrapperReconciliationRecipe.class.getName()).append('\t')
          .append("SDK_EXECUTION\t").append(target.path()).append('\t')
          .append(observed.get(target.path())).append('\t').append(target.before()).append('\t')
          .append(target.after()).append('\t').append(actual).append('\t')
          .append(first.changes().size()).append("\t0\tfalse\tfalse\tfalse\n");
    }
    return new Applied(first.changes().size(), receipt.toString());
  }

  private static Path safePath(Path root, String relative) {
    Path path = root.resolve(relative).normalize();
    if (!path.startsWith(root) || path.equals(root)) {
      throw new IllegalStateException("target path escapes root");
    }
    Path cursor = root;
    for (Path part : root.relativize(path)) {
      cursor = cursor.resolve(part);
      if (Files.isSymbolicLink(cursor)) throw new IllegalStateException("symbolic target path");
      if (!cursor.equals(path) && Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)
          && !Files.isDirectory(cursor, LinkOption.NOFOLLOW_LINKS)) {
        throw new IllegalStateException("target ancestor is not a directory: " + cursor);
      }
    }
    return path;
  }

  static String path(SourceFile source) {
    return source.getSourcePath().normalize().toString().replace('\\', '/');
  }

  static String sha(String text) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }

  private static String utf8(byte[] bytes) throws IOException {
    return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
  }
}
