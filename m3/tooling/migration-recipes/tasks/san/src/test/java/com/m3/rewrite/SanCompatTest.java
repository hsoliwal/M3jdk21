// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class SanCompatTest {
    private static final Path CRATE = Path.of(System.getProperty("san.crate"));
    private static final Path MODULE = CRATE.resolve("../..").normalize();
    private static final String RES = "/com/synexia/rewrite/hash-pinned-java/san-a3-compat/";
    private static final String PREFIX = "src/main/java/com/m3/rewrite/a3/";
    private static final List<String> NAMES = List.of("M3A3AlgorithmCatalogueRecipe", "M3A3SerialFileWorkRecipe");

    private static String read(String name) throws Exception {
        try (var stream = SanCompatTest.class.getResourceAsStream(RES + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static SourceFile parse(String name, String value) {
        return JavaParser.fromJavaVersion().build().parse(value).findFirst().orElseThrow()
                .withSourcePath(Path.of(PREFIX + name + ".java"));
    }
    private static List<SourceFile> before() throws Exception {
        var files = new ArrayList<SourceFile>();
        for (String name : NAMES) files.add(parse(name, read("before-" + name + ".java.txt")));
        files.add(parse("M3A3SerialFileWorkRecipeTest", read("before-M3A3SerialFileWorkRecipeTest.java.txt"))
                .withSourcePath(Path.of("src/test/java/com/m3/rewrite/a3/M3A3SerialFileWorkRecipeTest.java")));
        return files;
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
    }
    @Test void replayCompilesActualA3OwnersAndReachesFixedPoint() throws Exception {
        var recipe = Environment.builder().scanRuntimeClasspath("com.m3.rewrite").build()
                .activateRecipes("com.m3.rewrite.RepairSanA3Types");
        var results = recipe.run(new InMemoryLargeSourceSet(before()), context(), 1).getChangeset().getAllResults();
        assertEquals(3, results.size());
        Path generated = CRATE.resolve("target/generated-a3"), classes = CRATE.resolve("target/a3-classes");
        Files.createDirectories(generated); Files.createDirectories(classes);
        var after = new ArrayList<SourceFile>(); var sources = new ArrayList<java.io.File>();
        for (var result : results) {
            var file = result.getAfter(); assertNotNull(file); after.add(file);
            String name = file.getSourcePath().getFileName().toString();
            assertEquals(read(name + ".txt"), file.printAll());
            Path path = generated.resolve(name); Files.writeString(path, file.printAll()); sources.add(path.toFile());
        }
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1).getChangeset().getAllResults().isEmpty());
        for (String name : List.of("M3A3AlgorithmCatalogueManifest", "M3A3SerialFileWorkManifest"))
            sources.add(MODULE.resolve("src/main/java/com/m3/rewrite/a3/" + name + ".java").toFile());
        sources.add(MODULE.resolve("src/test/java/com/m3/rewrite/a3/M3A3AlgorithmCatalogueRecipeTest.java").toFile());
        var compiler = ToolProvider.getSystemJavaCompiler(); assertNotNull(compiler);
        var output = new StringWriter();
        try (var manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            boolean ok = compiler.getTask(output, manager, null,
                    List.of("--release", "21", "-Xlint:all", "-Werror", "-classpath",
                            System.getProperty("java.class.path"), "-d", classes.toString()),
                    null, manager.getJavaFileObjectsFromFiles(sources)).call();
            Files.writeString(CRATE.resolve("target/a3-compile.log"), output.toString());
            assertTrue(ok, output::toString);
        }
        try (var loader = new java.net.URLClassLoader(new java.net.URL[] {
                classes.toUri().toURL(), MODULE.resolve("src/main/resources").toUri().toURL()
        }, SanCompatTest.class.getClassLoader())) {
            int count = 0;
            for (String name : NAMES) {
                var type = loader.loadClass("com.m3.rewrite.a3." + name + "Test");
                var constructor = type.getDeclaredConstructor(); constructor.setAccessible(true);
                Object instance = constructor.newInstance();
                for (var method : type.getDeclaredMethods()) if (method.getAnnotation(Test.class) != null) {
                    method.setAccessible(true); method.invoke(instance); count++;
                }
            }
            assertEquals(6, count);
            Files.writeString(CRATE.resolve("target/a3-existing-tests.log"),
                    "EXISTING_A3_TEST_METHODS=" + count + "\nRESULT=PASS\n");
        }
    }
    @Test void refusesDriftAndMissingA3Owner() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("san-a3-compat");
        var drift = before(); drift.set(0, parse(NAMES.getFirst(), drift.getFirst().printAll() + "// drift\n"));
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(drift), context(), 1));
        var missing = before(); missing.removeFirst();
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(missing), context(), 1));
    }
}
