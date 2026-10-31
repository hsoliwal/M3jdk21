// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.text.PlainText;

/** Actual named SDK execution and output capture, following M3MaterializationSdkTest. */
final class M3ParserCollectionsSdkTest {
    private record Target(String path, String beforeHash, String afterHash,
                          String before, String after, boolean javaSource) { }
    private record Lane(boolean javaSource, Recipe recipe, List<Target> targets) { }

    @Test void actualLoadedVersionsMatchExplicitBuildTuple() throws Exception {
        assertEquals("8.17.1", System.getProperty("sdk.rewrite.version"));
        // Maven Central's published core 8.17.1 jar retains the older integration manifest.
        // Admit its primary-checksum-verified bytes explicitly; never relabel the binary.
        assertEquals("8.17.0-SNAPSHOT", Recipe.class.getPackage().getImplementationVersion());
        assertEquals("d7c07f91ab085a39bf1b40d7f2a7796a52619aaeefeca1481435e685aa663efa",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(
                        Path.of(Recipe.class.getProtectionDomain().getCodeSource().getLocation().toURI())))));
        assertEquals("8.17.1", JavaParser.class.getPackage().getImplementationVersion());
        assertEquals(System.getProperty("sdk.junit.version"), Test.class.getPackage().getImplementationVersion());
        assertEquals(21, Runtime.version().feature(), "explicit Java 21 SDK qualification tuple");
    }

    @Test void namedSchedulerProducesExactOutputsReplaysAndExportsOnlyActualResults() throws Exception {
        List<Lane> lanes = lanes();
        List<SourceFile> before = inputs(lanes);
        Map<String, SourceFile> after = execute(named(), lanes, before);
        List<SourceFile> reversed = new ArrayList<>(before);
        Collections.reverse(reversed);
        assertEquals(texts(after), texts(execute(named(), lanes, reversed)), "fresh scheduler and reversed inputs");
        for (Lane lane : lanes) {
            assertTrue(lane.recipe().run(new InMemoryLargeSourceSet(new ArrayList<>(after.values())), context(), 1)
                    .getChangeset().getAllResults().isEmpty(), "each leaf stage must be unchanged");
        }
        assertTrue(named().run(new InMemoryLargeSourceSet(new ArrayList<>(after.values())), context(), 1)
                .getChangeset().getAllResults().isEmpty(), "whole named recipe replay");
        // Export only immutable SourceFiles returned by the real SDK, never template copies.
        Path output = Path.of(System.getProperty("sdk.output")).toAbsolutePath().normalize();
        assertFalse(Files.exists(output), "retain earlier attempts and choose a fresh proof.directory");
        StringBuilder receipt = new StringBuilder("recipe\tpath\tbefore_sha256\tafter_sha256\tsource_kind\n");
        for (Lane lane : lanes) for (Target target : lane.targets()) {
            SourceFile source = after.get(target.path());
            Path destination = output.resolve(target.path()).normalize();
            assertTrue(destination.startsWith(output));
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, source.printAll(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            receipt.append(System.getProperty("m3.recipe")).append('\t').append(target.path()).append('\t')
                    .append(target.beforeHash()).append('\t').append(sha(source.printAll())).append('\t')
                    .append(target.javaSource() ? "JAVA_LST" : "TEXT_EXACT").append('\n');
        }
        Files.writeString(output.resolve("SDK_EXECUTION.tsv"), receipt, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
    }

    @Test void everyExistingTargetRefusesMissingDriftDuplicatesAndInterleavedEdits() throws Exception {
        for (Lane lane : lanes()) {
            List<SourceFile> before = inputs(List.of(lane));
            for (Target target : lane.targets()) {
                if (target.before() == null) continue;
                List<SourceFile> missing = before.stream().filter(file -> !path(file).equals(target.path())).toList();
                refuses(lane, missing, "required source missing");
                List<SourceFile> duplicate = new ArrayList<>(before);
                duplicate.add(before.stream().filter(file -> path(file).equals(target.path())).findFirst().orElseThrow());
                refuses(lane, duplicate, "duplicate target");
                List<SourceFile> drift = new ArrayList<>(missing);
                SourceFile changed = parse(target.path(), target.before() + "\n// exact-source drift witness\n", lane.javaSource());
                drift.add(changed);
                refuses(lane, drift, "source drift");
                if (lane.javaSource()) {
                    List<SourceFile> impostor = new ArrayList<>(missing);
                    impostor.add(PlainText.builder().sourcePath(Path.of(target.path())).text(target.before()).build());
                    refuses(lane, impostor, "Java");
                }
                if (lane.recipe() instanceof M3HashPinnedJavaSnapshotRecipe java) {
                    interleaved(java, before, changed);
                } else interleaved((M3HashPinnedTextSnapshotRecipe) lane.recipe(), before, changed);
            }
        }
    }

    private static <T> void interleaved(ScanningRecipe<T> recipe, List<SourceFile> before, SourceFile edited) {
        var ctx = context();
        T inventory = recipe.getInitialValue(ctx);
        before.forEach(source -> recipe.getScanner(inventory).visit(source, ctx));
        recipe.generate(inventory, ctx);
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> recipe.getVisitor(inventory).visit(edited, ctx));
        assertTrue(hasMessage(failure, "target changed after scan"), failure.toString());
    }

    private static void refuses(Lane lane, List<SourceFile> sources, String reason) {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            if (lane.recipe() instanceof M3HashPinnedJavaSnapshotRecipe java) scan(java, sources);
            else scan((M3HashPinnedTextSnapshotRecipe) lane.recipe(), sources);
        });
        assertTrue(hasMessage(failure, reason), failure.toString());
    }

    private static <T> void scan(ScanningRecipe<T> recipe, List<SourceFile> sources) {
        var ctx = context();
        T inventory = recipe.getInitialValue(ctx);
        sources.forEach(source -> recipe.getScanner(inventory).visit(source, ctx));
        recipe.generate(inventory, ctx);
    }

    private static Map<String, SourceFile> execute(Recipe recipe, List<Lane> lanes, List<SourceFile> before) {
        List<SourceFile> withCanary = new ArrayList<>(before);
        PlainText canary = PlainText.builder().sourcePath(Path.of("docs/parser-collections-canary.md"))
                .text("```java\nclass Decoy { String regex = \"[A-Z]+\"; }\n```\n").build();
        withCanary.add(canary);
        var changes = recipe.run(new InMemoryLargeSourceSet(withCanary), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(lanes.stream().mapToInt(lane -> lane.targets().size()).sum(), changes.size());
        Map<String, SourceFile> outputs = new TreeMap<>();
        for (var change : changes) {
            SourceFile after = change.getAfter();
            assertNotNull(after, "no deletion authority");
            assertNull(outputs.put(path(after), after), "duplicate output");
            assertTrue(after.getMarkers().findFirst(RecipesThatMadeChanges.class).isPresent());
            if (change.getBefore() != null) {
                assertEquals(change.getBefore().getId(), after.getId());
                assertEquals(change.getBefore().getCharset(), after.getCharset());
                assertEquals(change.getBefore().isCharsetBomMarked(), after.isCharsetBomMarked());
                assertTrue(after.getMarkers().getMarkers().containsAll(change.getBefore().getMarkers().getMarkers()));
            }
        }
        assertFalse(outputs.containsKey(path(canary)));
        for (Lane lane : lanes) for (Target target : lane.targets()) {
            SourceFile actual = outputs.get(target.path());
            assertNotNull(actual, target.path());
            if (target.javaSource()) assertInstanceOf(J.CompilationUnit.class, actual);
            else {
                assertInstanceOf(PlainText.class, actual);
                if (target.path().endsWith(".java")) {
                    // A narrowly admitted syntax bootstrap is not a Java text bypass:
                    // every returned successor must enter the same lossless typed parser.
                    parse(target.path(), actual.printAll(), true);
                }
            }
            assertEquals(target.after(), actual.printAll());
            assertEquals(target.afterHash(), sha(actual.printAll()));
        }
        return outputs;
    }

    private static Recipe named() {
        Recipe recipe = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes(System.getProperty("m3.recipe"));
        assertTrue(recipe.validateAll().stream().allMatch(value -> value.isValid()));
        return recipe;
    }

    private static List<SourceFile> inputs(List<Lane> lanes) {
        List<SourceFile> result = new ArrayList<>();
        for (Lane lane : lanes) for (Target target : lane.targets()) {
            if (target.before() != null) result.add(parse(target.path(), target.before(), lane.javaSource()));
        }
        return result;
    }

    private static SourceFile parse(String path, String text, boolean javaSource) {
        if (!javaSource) return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
        SourceFile source = JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()).build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), text)), null, context())
                .findFirst().orElseThrow();
        assertInstanceOf(J.CompilationUnit.class, source, path + ": " + source.getMarkers().getMarkers());
        assertEquals(text, source.printAll(), "lossless source input");
        return source;
    }

    private static List<Lane> lanes() throws IOException {
        String crate = System.getProperty("m3.crate");
        assertNotNull(crate);
        assertTrue(crate.matches("[a-z0-9][a-z0-9-]{0,79}"));
        List<Lane> result = new ArrayList<>();
        for (boolean javaSource : List.of(true, false)) {
            String root = "/com/synexia/rewrite/hash-pinned-" + (javaSource ? "java/" : "text/") + crate + "/";
            String manifest = resource(root + "manifest.tsv", true);
            if (manifest == null) continue;
            List<Target> targets = new ArrayList<>();
            String previous = "";
            for (String line : manifest.lines().toList()) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] cells = line.split("\t", -1);
                assertEquals(4, cells.length);
                assertTrue(previous.compareTo(cells[0]) < 0, "sorted unique target paths");
                previous = cells[0];
                String after = resource(root + cells[3], false);
                String before = cells[1].equals("ABSENT") ? null : resource(root + "pre-" + cells[3], false);
                assertEquals(cells[2], sha(after));
                if (before != null) assertEquals(cells[1], sha(before));
                targets.add(new Target(cells[0], cells[1], cells[2], before, after, javaSource));
            }
            assertFalse(targets.isEmpty());
            result.add(new Lane(javaSource, javaSource ? new M3HashPinnedJavaSnapshotRecipe(crate)
                    : new M3HashPinnedTextSnapshotRecipe(crate), List.copyOf(targets)));
        }
        assertFalse(result.isEmpty(), "at least one exact manifest required");
        return List.copyOf(result);
    }

    private static String resource(String path, boolean optional) throws IOException {
        try (var stream = M3ParserCollectionsSdkTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                if (optional) return null;
                throw new IOException("missing resource " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new IllegalStateException(failure); });
    }
    private static String path(SourceFile source) { return source.getSourcePath().toString().replace('\\', '/'); }
    private static Map<String, String> texts(Map<String, SourceFile> sources) {
        Map<String, String> result = new TreeMap<>();
        sources.forEach((path, source) -> result.put(path, source.printAll()));
        return result;
    }
    private static boolean hasMessage(Throwable failure, String text) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains(text)) return true;
        }
        return false;
    }
    private static String sha(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
