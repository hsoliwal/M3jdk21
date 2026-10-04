// SPDX-License-Identifier: Apache-2.0
package com.m3.cb;

import static org.junit.jupiter.api.Assertions.*;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Recipe;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** CB proof: exact recipe, class metadata, weak boundaries and effective version lookup. */
final class CBTest {
    private static final Path ROOT = Path.of(System.getProperty("m3.root")).normalize();
    private static final Path CRATE = Path.of(System.getProperty("m3.crate"));
    private static final String RES = "com/synexia/rewrite/hash-pinned-java/";
    private static final String TARGET = "src/java.base/share/classes/jdk/internal/mindex/M3CI.java";

    @Test void generateCompileAndRun() throws Exception {
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-cb");
        var after = new ArrayList<SourceFile>();
        for (var result : recipe.run(new InMemoryLargeSourceSet(List.of()), context, 1).getChangeset().getAllResults()) {
            assertNull(result.getBefore()); assertNotNull(result.getAfter()); after.add(result.getAfter());
        }
        assertEquals(16, after.size());
        bootstrapInventory(after);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context, 1).getChangeset().getAllResults().isEmpty());
        var generated = CRATE.resolve("target/generated");
        for (SourceFile source : after) {
            Path path = generated.resolve(source.getSourcePath()).normalize();
            assertTrue(path.startsWith(generated)); Files.createDirectories(path.getParent());
            Files.writeString(path, source.printAll());
            if (source.getSourcePath().toString().startsWith("src/java.base/")
                    && Files.exists(ROOT.resolve(source.getSourcePath()))) {
                assertEquals(source.printAll(), Files.readString(ROOT.resolve(source.getSourcePath())), "retained target drift");
            }
        }
        for (String line : Files.readAllLines(CRATE.resolve("recipe-pin.tsv"))) {
            if (line.startsWith("#") || line.isBlank()) continue;
            var cells = line.split("\t", -1);
            assertEquals(2, cells.length); assertEquals(cells[1], hash(Files.readString(ROOT.resolve(cells[0]))));
        }
        Path product = generated.resolve("src/java.base/share/classes");
        Path classes = CRATE.resolve("target/patch"); Files.createDirectories(classes);
        Path testClasses = CRATE.resolve("target/probes"); Files.createDirectories(testClasses);
        String javac = Path.of(System.getProperty("java.home"), "bin/javac").toString();
        String java = Path.of(System.getProperty("java.home"), "bin/java").toString();
        Path reference = CRATE.resolve("target/reference");
        for (String name : List.of("M3VI", "M3Release")) {
            Path relative = Path.of("jdk/internal/mindex/" + name + ".java");
            Path destination = reference.resolve(relative); Files.createDirectories(destination.getParent());
            Files.copy(ROOT.resolve("src/java.base/share/classes").resolve(relative), destination,
                    StandardCopyOption.REPLACE_EXISTING);
        }
        var compile = new ArrayList<>(List.of(javac, "-source", "21", "-target", "21", "-Xlint:all", "-Werror", "-proc:none",
                "--patch-module", "java.base=" + product + File.pathSeparator + reference, "-d", classes.toString()));
        try (var paths = Files.walk(product)) { paths.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> compile.add(p.toString())); }
        compile.add(reference.resolve("jdk/internal/mindex/M3VI.java").toString());
        compile.add(reference.resolve("jdk/internal/mindex/M3Release.java").toString());
        execute(compile, "compile-product.log");
        execute(List.of(javac, "-source", "21", "-target", "21", "-Xlint:all", "-Werror", "-proc:none",
                "--patch-module", "java.base=" + classes,
                "--add-exports", "java.base/jdk.internal.mindex=ALL-UNNAMED", "-d", testClasses.toString(),
                generated.resolve("src/test/java/com/m3/cb/CBProbe.java").toString(),
                generated.resolve("src/test/java/com/m3/cb/VmFixture.java").toString()), "compile-probe.log");
        for (String mode : List.of("-Xint", "-Xmixed")) {
            execute(List.of(java, mode, "-Xmx256m", "-XX:+ClassUnloading", "--patch-module", "java.base=" + classes,
                    "--add-exports", "java.base/jdk.internal.mindex=ALL-UNNAMED",
                    "-cp", testClasses.toString(), "com.m3.cb.CBProbe"), mode.substring(1) + ".log");
        }
        execute(List.of(Path.of(System.getProperty("java.home"), "bin/jdeps").toString(), "-s", classes.toString()), "jdeps.log");
        assertEquals("patch -> java.base", Files.readString(CRATE.resolve("target/jdeps.log")).strip());
    }

    @Test void refusesDriftAndWrongParser() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-cb");
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        var conflict = JavaParser.fromJavaVersion().build().parse("class Conflict {}").findFirst().orElseThrow().withSourcePath(Path.of(TARGET));
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of(conflict)), context, 1));
        var wrongParser = PlainText.builder().text("class Conflict {}").sourcePath(Path.of(TARGET)).build();
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of(wrongParser)), context, 1));
    }

    @Test void preservesUnrelatedInputs() {
        var unrelated = JavaParser.fromJavaVersion().build().parse("class Unrelated { int value() { return 42; } }")
                .findFirst().orElseThrow().withSourcePath(Path.of("src/other/Unrelated.java"));
        for (var result : new M3HashPinnedJavaSnapshotRecipe("m3-cb")
                .run(new InMemoryLargeSourceSet(List.of(unrelated)), new InMemoryExecutionContext(), 1).getChangeset().getAllResults()) {
            assertNotEquals(unrelated.getSourcePath(), result.getAfter().getSourcePath());
        }
    }

    @Test void refusesTamperedTemplates() throws Exception {
        var target = CRATE.resolve("target/test-classes/" + RES + "m3-cb-bad"); Files.createDirectories(target);
        try (var paths = Files.list(CRATE.resolve("src/main/resources/" + RES + "m3-cb"))) {
            for (Path path : paths.toList()) {
                String text = Files.readString(path);
                if (path.toString().endsWith(".java.txt")) text += "// drift\n";
                Files.writeString(target.resolve(path.getFileName()), text);
            }
        }
        assertThrows(IllegalStateException.class, () -> new M3HashPinnedJavaSnapshotRecipe("m3-cb-bad")
                .getInitialValue(new InMemoryExecutionContext()));
    }

    @Test void verifiesInlinedSourceAndLicense() throws Exception {
        var pins = new ObjectMapper().readTree(CRATE.resolve("pins.json").toFile());
        for (var fields = pins.path("source_templates").fieldNames(); fields.hasNext();) {
            String name = fields.next();
            byte[] bytes = Files.readAllBytes(CRATE.resolve("donor/" + name + ".java.txt"));
            assertEquals(pins.path("source_templates").path(name).path("sha256").asText(),
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            var git = MessageDigest.getInstance("SHA-1");
            git.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8));
            assertEquals(pins.path("source_templates").path(name).path("git_blob").asText(),
                    HexFormat.of().formatHex(git.digest(bytes)));
            assertTrue(new String(bytes, StandardCharsets.UTF_8).startsWith("// SPDX-License-Identifier: Apache-2.0\n"));
        }
        for (String name : List.of("LICENSE", "NOTICE")) {
            byte[] bytes = Files.readAllBytes(CRATE.resolve("donor/" + name));
            assertEquals(pins.path("legal").path(name).path("sha256").asText(),
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        }
    }

    private static String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static void bootstrapInventory(List<SourceFile> files) throws Exception {
        var classes = CRATE.resolve("target/bootstrap"); Files.createDirectories(classes);
        String owner = ".m3/openrewrite-recipes/src/main/java/com/synexia/m3/bootstrap/";
        execute(List.of(Path.of(System.getProperty("java.home"), "bin/javac").toString(),
                "--release", "21", "-Xlint:all", "-Werror", "-proc:none", "-cp", System.getProperty("java.class.path"),
                "-d", classes.toString(), ROOT.resolve(owner + "M3BootstrapTaskRecipe.java").toString(),
                ROOT.resolve(owner + "M3BootstrapInventoryRecipe.java").toString()), "compile-bootstrap.log");
        var manifest = CRATE.resolve("src/main/resources/" + RES + "m3-cb/manifest.tsv");
        String oldFile = System.getProperty("m3.llm.taskCrateFile");
        String oldRoot = System.getProperty("m3.llm.taskCrateRoot");
        try (var loader = new URLClassLoader(new java.net.URL[] {classes.toUri().toURL()}, CBTest.class.getClassLoader())) {
            System.setProperty("m3.llm.taskCrateFile", manifest.toString());
            System.setProperty("m3.llm.taskCrateRoot", hash(Files.readString(manifest)));
            var type = loader.loadClass("com.synexia.m3.bootstrap.M3BootstrapTaskRecipe");
            var recipe = (Recipe) type.getConstructor().newInstance();
            assertEquals(false, type.getMethod("directTargetFileMutationAuthority").invoke(recipe));
            assertEquals(1, recipe.getRecipeList().size());
            assertTrue(recipe.run(new InMemoryLargeSourceSet(files), new InMemoryExecutionContext(), 1)
                    .getChangeset().getAllResults().isEmpty(), "read-only bootstrap inventory");
            System.setProperty("m3.llm.taskCrateRoot", "0".repeat(64));
            assertThrows(IllegalStateException.class, recipe::getRecipeList);
        } finally {
            if (oldFile == null) System.clearProperty("m3.llm.taskCrateFile"); else System.setProperty("m3.llm.taskCrateFile", oldFile);
            if (oldRoot == null) System.clearProperty("m3.llm.taskCrateRoot"); else System.setProperty("m3.llm.taskCrateRoot", oldRoot);
        }
    }

    private static void execute(List<String> command, String log) throws Exception {
        Path output = CRATE.resolve("target/" + log);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("timed out: " + log); }
        assertEquals(0, process.exitValue(), () -> {
            try { return Files.readString(output); } catch (Exception error) { return error.toString(); }
        });
    }
}
