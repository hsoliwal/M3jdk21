/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026: qualify both source-sealed composition orders and exact SDK exports.
 * Reuses the parser/scheduler/refusal proof approach of FilteredBatchRecipeTest.
 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Tests actual named OpenRewrite execution; runtime and JNI oracles remain independent. */
final class WrapperCompositionRecipeTest {
    private static final String MAIN =
            "m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java";
    private static final String BATCH =
            "m3/collections/src/test/java/com/m3/collections/FilteredBatchContracts.java";
    private static final String REPRESENTATIVE =
            "m3/collections/wrappers/src/test/java/com/m3/collections/M3StoredRepresentativeProof.java";
    private static final String KEEP = "src/main/java/Keep.java";
    private static final List<String> STATES = List.of("representative", "batch");
    private static final String DECOYS = """
            import java.util.regex.Pattern;
            class Keep<T> {
                String code = "return backing.addAll(elements);";
                String block = \"""
                    public boolean remove(Object candidate) { return false; }
                    \""";
                Pattern regex = Pattern.compile("addAll\\\\(elements\\\\)");
                // public boolean contains(Object candidate) { return true; }
                java.util.function.Predicate<T> same = java.util.Objects::nonNull;
                record Item(String value) {}
            }
            """;

    private static String resource(String state, String name) throws Exception {
        String path = "/com/synexia/rewrite/hash-pinned-java/wrapper-composition-"
                + state + "-20261017/" + name;
        try (var input = WrapperCompositionRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String added(String state) {
        return state.equals("representative") ? BATCH : REPRESENTATIVE;
    }

    private static String opposite(String state) {
        return state.equals("representative") ? "batch" : "representative";
    }

    private static String expected(String path) throws Exception {
        if (path.equals(KEEP)) return DECOYS;
        String state = path.equals(REPRESENTATIVE) ? "batch" : "representative";
        return resource(state, Path.of(path).getFileName() + ".txt");
    }

    private static Map<String, String> source(String state, int partial) throws Exception {
        var texts = new LinkedHashMap<String, String>();
        texts.put(MAIN, (partial & 1) == 0
                ? resource(state, "pre/M3CollectionViews.java.txt") : expected(MAIN));
        texts.put(added(opposite(state)), expected(added(opposite(state))));
        if ((partial & 2) != 0) texts.put(added(state), expected(added(state)));
        texts.put(KEEP, DECOYS);
        return texts;
    }

    private static List<SourceFile> parse(Map<String, String> texts) {
        var errors = new ArrayList<Throwable>();
        var inputs = texts.entrySet().stream()
                .map(entry -> Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue())).toList();
        var parsed = JavaParser.fromJavaVersion().build()
                .parseInputs(inputs, null, new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(), errors::toString);
        assertEquals(texts.size(), parsed.size());
        for (var file : parsed) assertInstanceOf(J.CompilationUnit.class, file);
        return parsed;
    }

    private static Recipe named(String state) {
        String name = "com.synexia.m3.WrapperComposition"
                + (state.equals("batch") ? "FromBatch" : "") + "20261017";
        return configured(name);
    }

    private static Recipe configured(String name) {
        String descriptor = "/META-INF/rewrite/wrapper-composition.yml";
        try (var input = WrapperCompositionRecipeTest.class.getResourceAsStream(descriptor)) {
            assertNotNull(input, descriptor);
            return Environment.builder().load(new YamlResourceLoader(input,
                    URI.create("classpath:" + descriptor), new Properties()))
                    .build().activateRecipes(name);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot load task recipe descriptor", failure);
        }
    }

    private record Application(List<SourceFile> full, Map<String, String> emitted) {}

    private static Application apply(String state, List<SourceFile> input, int expectedChanges)
            throws Exception {
        var errors = new ArrayList<Throwable>();
        var run = named(state).run(new InMemoryLargeSourceSet(input),
                new InMemoryExecutionContext(errors::add), 1);
        assertTrue(errors.isEmpty(), errors::toString);
        var results = run.getChangeset().getAllResults();
        assertEquals(expectedChanges, results.size());
        var full = new LinkedHashMap<Path, SourceFile>();
        for (var file : input) full.put(file.getSourcePath(), file);
        Map<String, String> emitted = new TreeMap<>();
        for (var result : results) {
            SourceFile after = result.getAfter();
            assertNotNull(after);
            assertInstanceOf(J.CompilationUnit.class, after);
            if (result.getBefore() != null) assertEquals(result.getBefore().getId(), after.getId());
            String path = after.getSourcePath().toString().replace('\\', '/');
            assertTrue(path.equals(MAIN) || path.equals(added(state)));
            assertEquals(expected(path), after.printAll());
            assertNull(emitted.put(path, after.printAll()));
            full.put(after.getSourcePath(), after);
        }
        for (var original : input) {
            String path = original.getSourcePath().toString().replace('\\', '/');
            if (!emitted.containsKey(path)) assertSame(original, full.get(original.getSourcePath()));
        }
        return new Application(List.copyOf(full.values()), emitted);
    }

    private static Map<String, String> texts(List<SourceFile> files) {
        Map<String, String> texts = new TreeMap<>();
        for (var file : files) texts.put(file.getSourcePath().toString(), file.printAll());
        return texts;
    }

    private static void refuses(String state, List<SourceFile> input, String signature) {
        var errors = new ArrayList<Throwable>();
        try {
            named(state).run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add), 1);
        } catch (RuntimeException refused) {
            // SDK 8.17.1 can propagate a scan/generate/visitor refusal directly.
            // Keep the same exact failure fingerprint as callback-reported failures.
            errors.add(refused);
        }
        assertFalse(errors.isEmpty(), "missing refusal: " + signature);
        for (Throwable error : errors) {
            while (error.getCause() != null) error = error.getCause();
            assertInstanceOf(IllegalStateException.class, error);
            assertTrue(error.getMessage().contains(signature), error.toString());
        }
    }

    @Test
    void namedOrdersConvergeAndExportActualChanges() throws Exception {
        Application representative = apply("representative", parse(source("representative", 0)), 2);
        Application batch = apply("batch", parse(source("batch", 0)), 2);
        assertEquals(texts(representative.full()), texts(batch.full()));
        for (String state : STATES) {
            apply(state, representative.full(), 0);
            apply(state, batch.full(), 0);
        }
        Path root = Path.of(System.getProperty("m3.composition.output", "target/emitted"))
                .toAbsolutePath().normalize();
        for (Path path = root; path != null; path = path.getParent()) {
            assertFalse(Files.isSymbolicLink(path), "symlink in SDK output");
        }
        assertFalse(Files.exists(root, LinkOption.NOFOLLOW_LINKS), "output must be absent");
        export(root.resolve("representative"), representative.emitted());
        export(root.resolve("batch"), batch.emitted());
        System.out.println("WRAPPER_COMPOSITION_REAL_SDK_EMISSION routes=2 changes_per_route=2");
    }

    private static void export(Path root, Map<String, String> results) throws Exception {
        StringBuilder receipt = new StringBuilder("# actual OpenRewrite Results\n");
        for (var entry : results.entrySet()) {
            Path file = root.resolve(entry.getKey()).normalize();
            assertTrue(file.startsWith(root));
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardOpenOption.CREATE_NEW);
            receipt.append(entry.getKey()).append('\t').append(sha(entry.getValue())).append('\n');
        }
        Files.writeString(root.resolve("SDK_RECEIPT.tsv"), receipt, StandardOpenOption.CREATE_NEW);
    }

    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void everyPartialStateAndInputOrderPreservesDecoys() throws Exception {
        for (String state : STATES) for (int partial = 0; partial < 4; partial++) {
            for (int order = 0; order < 2; order++) {
                var input = new ArrayList<>(parse(source(state, partial)));
                if (order == 1) Collections.reverse(input);
                Application output = apply(state, input, 2 - Integer.bitCount(partial));
                apply(state, output.full(), 0);
            }
        }
    }

    @Test
    void serialAndTwoIndependentWorkersAgree() throws Exception {
        Map<String, String> serial = texts(apply("representative",
                parse(source("representative", 0)), 2).full());
        List<Callable<Map<String, String>>> tasks = new ArrayList<>();
        for (String state : STATES) {
            tasks.add(() -> texts(apply(state, parse(source(state, 0)), 2).full()));
        }
        try (var workers = Executors.newFixedThreadPool(2)) {
            for (var completed : workers.invokeAll(tasks)) assertEquals(serial, completed.get());
        }
    }

    @Test
    void exactManifestsAndIndependentAtoms() throws Exception {
        for (String state : STATES) {
            for (String row : resource(state, "manifest.tsv").lines().toList()) {
                String[] cells = row.split("\t", -1);
                assertEquals(4, cells.length);
                assertEquals(cells[2], sha(resource(state, cells[3])));
                if (!cells[1].equals("ABSENT")) {
                    assertEquals(cells[1], sha(resource(state, "pre/" + cells[3])));
                }
            }
        }
        String representative = resource("representative", "pre/M3CollectionViews.java.txt");
        String batch = resource("batch", "pre/M3CollectionViews.java.txt");
        String after = expected(MAIN);
        String originalAtom = addAllAtom(representative);
        String stagedAtom = addAllAtom(batch);
        assertNotEquals(originalAtom, stagedAtom);
        assertEquals(after, representative.replace(originalAtom, stagedAtom),
                "primary route may only change the exact existing addAll atom");
        assertEquals(after, resource("batch", "M3CollectionViews.java.txt"));
    }

    private static String addAllAtom(String source) {
        int first = source.indexOf("        @Override\n        public boolean addAll(");
        assertTrue(first >= 0);
        int next = source.indexOf("\n        @Override", first + 1);
        assertTrue(next > first);
        return source.substring(first, next);
    }

    @Test
    void refusesMissingDuplicateDriftAndOccupiedTargets() throws Exception {
        for (String state : STATES) {
            refuses(state, List.of(), "required source missing");
            var duplicate = new ArrayList<>(parse(source(state, 0)));
            duplicate.add(duplicate.stream().filter(file -> file.getSourcePath().toString().equals(MAIN))
                    .findFirst().orElseThrow());
            refuses(state, duplicate, "duplicate target");
            var drift = source(state, 0);
            drift.put(MAIN, drift.get(MAIN) + "\n// unreviewed drift\n");
            refuses(state, parse(drift), "source drift");
            var occupied = source(state, 0);
            occupied.put(added(state), "package com.m3.collections; class Occupied {}\n");
            refuses(state, parse(occupied), "source drift");
            refuses(state, parse(source(opposite(state), 0)), "source drift");
        }
    }

    @Test
    void refusesWrongRepresentationAndUnknownPredecessor() throws Exception {
        for (String state : STATES) {
            refuses(state, List.of(PlainText.builder().sourcePath(Path.of(MAIN))
                    .text(resource(state, "pre/M3CollectionViews.java.txt")).build()), "target representation");
        }
        assertThrows(IllegalArgumentException.class, () -> new M3WrapperCompositionRecipe("unknown"));
        assertThrows(IllegalArgumentException.class, () -> new M3WrapperCompositionRecipe(null));
    }

    @Test
    void workflowRecipePreservesPinsReplaysAndRefuses() throws Exception {
        String root = "/com/synexia/rewrite/hash-pinned-text/wrapper-composition-workflow-20261017/";
        String before;
        String after;
        String manifest;
        try (var first = getClass().getResourceAsStream(root + "pre/workflow.yml.txt");
             var second = getClass().getResourceAsStream(root + "workflow.yml.txt");
             var seal = getClass().getResourceAsStream(root + "manifest.tsv")) {
            assertNotNull(first); assertNotNull(second); assertNotNull(seal);
            before = new String(first.readAllBytes(), StandardCharsets.UTF_8);
            after = new String(second.readAllBytes(), StandardCharsets.UTF_8);
            manifest = new String(seal.readAllBytes(), StandardCharsets.UTF_8);
        }
        String path = ".github/workflows/m3jdk-stored-representative-20261017.yml";
        String[] row = manifest.strip().split("\t", -1);
        assertEquals(4, row.length);
        assertEquals(path, row[0]); assertEquals(sha(before), row[1]); assertEquals(sha(after), row[2]);
        Recipe recipe = configured("com.synexia.m3.WrapperCompositionWorkflow20261017");
        SourceFile input = PlainText.builder().sourcePath(Path.of(path)).text(before).build();
        SourceFile keep = PlainText.builder().sourcePath(Path.of("unrelated.yml"))
                .text("literal: 'M3CollectionViews.java'\n").build();
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        var results = recipe.run(new InMemoryLargeSourceSet(List.of(input, keep)), context, 1)
                .getChangeset().getAllResults();
        assertEquals(1, results.size());
        SourceFile actual = results.getFirst().getAfter();
        assertNotNull(actual); assertEquals(input.getId(), actual.getId());
        assertEquals(after, actual.printAll());
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(actual, keep)), context, 1)
                .getChangeset().getAllResults().isEmpty());
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of()), context, 1));
        SourceFile drift = PlainText.builder().sourcePath(Path.of(path)).text(before + "\n# drift\n").build();
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context, 1));
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of(input, input)), context, 1));
        Path destination = Path.of(System.getProperty("m3.composition.workflow.output", "target/workflow-emitted"))
                .toAbsolutePath().normalize();
        assertFalse(Files.exists(destination, LinkOption.NOFOLLOW_LINKS));
        for (Path ancestor = destination; ancestor != null; ancestor = ancestor.getParent()) {
            assertFalse(Files.isSymbolicLink(ancestor));
        }
        export(destination, Map.of(path, actual.printAll()));
        System.out.println("WRAPPER_WORKFLOW_REAL_SDK_EMISSION paths=1 fixed_point=true");
    }
}
