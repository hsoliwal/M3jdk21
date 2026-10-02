// SPDX-License-Identifier: Apache-2.0
package com.m3.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** File-system tests: failures must refuse before touching an unrelated output. */
public final class PortRecipeTest {
  private static int checks;
  private PortRecipeTest() {}
  public static void main(String[] args) throws Exception {
    replayRollback(); crossProcessReplay(); driftRefusal(); partialInstall(); pathRefusal(); symlinkRefusal();
    System.out.println("PORT_RECIPE " + checks + " checks passed");
  }
  private static void replayRollback() throws Exception {
    Fixture f = fixture();
    M3PortRecipe.execute("plan", f.bundle, f.source, f.target, true);
    check(!Files.exists(f.target.resolve("out/Example.java")));
    M3PortRecipe.execute("apply", f.bundle, f.source, f.target, true);
    check(Files.readString(f.target.resolve("out/Example.java")).equals(f.after));
    Path journal = f.target.resolve("m3/migration/.state/port-journal.json");
    String receipt = Files.readString(journal);
    M3PortRecipe.execute("apply", f.bundle, f.source, f.target, true);
    check(Files.readString(journal).equals(receipt));
    M3PortRecipe.execute("check", f.bundle, f.source, f.target, true);
    Files.writeString(f.target.resolve("keep.txt"), "unrelated");
    Path recovered = Files.createTempDirectory("m3-recovered-");
    M3PortRecipe.extractSources(f.bundle, f.target, recovered);
    check(Files.readString(recovered.resolve("src/Example.java")).equals(f.before));
    M3PortRecipe.execute("rollback", f.bundle, f.source, f.target, true);
    check(!Files.exists(f.target.resolve("out/Example.java")));
    check(Files.readString(f.target.resolve("keep.txt")).equals("unrelated"));
    M3PortRecipe.execute("rollback", f.bundle, f.source, f.target, true);
  }
  private static void crossProcessReplay() throws Exception {
    Fixture f = fixture();
    child(f, "apply");
    String receipt = Files.readString(f.target.resolve("m3/migration/.state/port-journal.json"));
    for (int i = 0; i < 8; i++) child(f, "apply");
    check(Files.readString(f.target.resolve("m3/migration/.state/port-journal.json")).equals(receipt));
    child(f, "rollback");
    check(!Files.exists(f.target.resolve("out/Example.java")));
  }
  private static void child(Fixture f, String action) throws Exception {
    String launcher = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    Process p = new ProcessBuilder(launcher, "-cp", System.getProperty("java.class.path"),
        M3PortRecipe.class.getName(), action, f.bundle.toString(), f.source.toString(),
        f.target.toString(), "--snapshot").redirectErrorStream(true).start();
    String output = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    if (p.waitFor() != 0) throw new AssertionError("child " + action + ": " + output);
  }
  private static void driftRefusal() throws Exception {
    Fixture f = fixture();
    Files.writeString(f.source.resolve("src/Example.java"), f.before + "// drift\n");
    refuses(() -> M3PortRecipe.execute("apply", f.bundle, f.source, f.target, true));
    check(!Files.exists(f.target.resolve("out")));
    Files.writeString(f.source.resolve("src/Example.java"), f.before);
    Files.createDirectories(f.target.resolve("out"));
    Files.writeString(f.target.resolve("out/Example.java"), "owned by somebody else");
    refuses(() -> M3PortRecipe.execute("apply", f.bundle, f.source, f.target, true));
    check(Files.readString(f.target.resolve("out/Example.java")).equals("owned by somebody else"));
    Files.delete(f.target.resolve("out/Example.java"));
    Files.delete(f.source.resolve("src/Example.java"));
    refuses(() -> M3PortRecipe.execute("apply", f.bundle, f.source, f.target, true));
    check(!Files.exists(f.target.resolve("out/Example.java")));
    Files.writeString(f.source.resolve("src/Example.java"), f.before);
    Files.writeString(f.bundle.resolve("patch.json"), "{}");
    refuses(() -> M3PortRecipe.execute("apply", f.bundle, f.source, f.target, true));
  }
  private static void partialInstall() throws Exception {
    Fixture f = fixture();
    Files.createDirectories(f.target.resolve("out"));
    Files.writeString(f.target.resolve("out/Example.java"), f.after);
    M3PortRecipe.execute("apply", f.bundle, f.source, f.target, true);
    M3PortRecipe.execute("rollback", f.bundle, f.source, f.target, true);
    // An exact pre-existing output was not created by this transaction and is not ours to delete.
    check(Files.readString(f.target.resolve("out/Example.java")).equals(f.after));
  }
  private static void pathRefusal() throws Exception {
    Fixture f = fixture();
    Map<String, Object> document = Json.object(Json.parse(Files.readString(f.manifest)));
    Map<String, Object> mapping = Json.object(Json.array(document.get("mappings")).get(0));
    Map<String, Object> target = Json.object(Json.array(mapping.get("target_refs")).get(0));
    target.put("path", "../escape.java"); mapping.put("target_refs", List.of(target));
    document.put("mappings", List.of(mapping)); Files.writeString(f.manifest, Json.write(document));
    refuses(() -> M3PortRecipe.execute("apply", f.bundle, f.source, f.target, true));
    check(!Files.exists(f.target.getParent().resolve("escape.java")));
  }
  private static void symlinkRefusal() throws Exception {
    Fixture f = fixture();
    Path outside = Files.createTempDirectory("m3-outside-");
    try { Files.createSymbolicLink(f.target.resolve("out"), outside); }
    catch (UnsupportedOperationException | java.nio.file.FileSystemException unsupported) {
      System.out.println("SKIP symlink creation: " + unsupported.getClass().getSimpleName()); return;
    }
    refuses(() -> M3PortRecipe.execute("apply", f.bundle, f.source, f.target, true));
    check(!Files.exists(outside.resolve("Example.java")));
  }
  private static Fixture fixture() throws Exception {
    Path home = Files.createTempDirectory("m3-port-recipe-");
    Path bundle = Files.createDirectory(home.resolve("bundle"));
    Path source = Files.createDirectory(home.resolve("source"));
    Path target = Files.createDirectory(home.resolve("target"));
    String before = "// SPDX-License-Identifier: Apache-2.0\nclass Example {}\n";
    String after = "// SPDX-License-Identifier: Apache-2.0\nfinal class Example {}\n";
    Files.createDirectories(source.resolve("src")); Files.writeString(source.resolve("src/Example.java"), before);
    String patch = Json.write(Map.of("operations", List.of(Map.of("before", "class Example {}", "after", "final class Example {}"))));
    Files.writeString(bundle.resolve("patch.json"), patch);
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("id", "fixture.example");
    entry.put("source_refs", List.of(Map.of("path", "src/Example.java", "sha256", M3PortRecipe.hash(before.getBytes(java.nio.charset.StandardCharsets.UTF_8)))));
    entry.put("target_refs", List.of(Map.of("path", "out/Example.java", "sha256", M3PortRecipe.hash(after.getBytes(java.nio.charset.StandardCharsets.UTF_8)))));
    entry.put("recipe", Map.of("mode", "port", "id", "fixture.exact-port", "version", "1", "patch_path", "patch.json", "patch_sha256", M3PortRecipe.hash(patch.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    Map<String, Object> document = Map.of("schema", 1, "migration", Map.of("schema_version", 1, "source_commit", "0".repeat(40)), "mappings", List.of(entry));
    Path manifest = bundle.resolve("m3/docs/name-mapping.json"); Files.createDirectories(manifest.getParent()); Files.writeString(manifest, Json.write(document));
    return new Fixture(bundle, source, target, manifest, before, after);
  }
  private static void check(boolean ok) { if (!ok) throw new AssertionError("check " + checks); checks++; }
  private static void refuses(Action action) throws Exception {
    try { action.run(); } catch (IllegalArgumentException | java.io.IOException expected) { checks++; return; }
    throw new AssertionError("unsafe operation was accepted");
  }
  private record Fixture(Path bundle, Path source, Path target, Path manifest, String before, String after) {}
  @FunctionalInterface private interface Action { void run() throws Exception; }
}
