// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite.sorts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import com.synexia.rewrite.M3HashPinnedTextSnapshotRecipe;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Actual OpenRewrite runs, frozen compiler observations and permanent refusal fixtures. */
final class M3RecipeReplayTest {
    private static final String PREFIX = "src/main/java/fixture/";
    private static final String CARRIER = PREFIX + "Carrier.java";
    private static final String NUMBERS = PREFIX + "Numbers.java";
    private static final String EXTRA = PREFIX + "Extra.java";
    @TempDir Path temp;

    @Test
    void absentOnlyRunCreatesEveryFileAndSecondRunHasNoChanges() throws Exception {
        Recipe recipe = new M3HashPinnedJavaSnapshotRecipe("sort-add-fixture");
        var first = M3JavaUtilSortsApply.evaluate(recipe, List.of());
        assertEquals(expected(), texts(first.sources()));
        assertEquals(List.of(CARRIER, EXTRA, NUMBERS), first.changed());
        assertTrue(first.sources().stream().allMatch(J.CompilationUnit.class::isInstance));
        assertTrue(M3JavaUtilSortsApply.evaluate(recipe, first.sources()).changed().isEmpty());
        List<String> observed = compileAndObserve(texts(first.sources()));
        assertTrue(observed.stream().anyMatch(value -> value.startsWith("carrier=69:5:")));
        assertTrue(observed.stream().anyMatch(value -> value.equals("fold=6")));
    }

    @Test
    void everyPartialPostimageSubsetAndInputOrderConvergesExactly() {
        Map<String, String> all = expected();
        List<String> paths = List.copyOf(all.keySet());
        String sealedRoot = root(all);
        for (int mask = 0; mask < 8; mask++) {
            for (boolean reverse : List.of(false, true)) {
                List<SourceFile> before = new ArrayList<>();
                for (int bit = 0; bit < paths.size(); bit++) {
                    if ((mask & (1 << bit)) != 0) {
                        before.addAll(parse(Map.of(paths.get(bit), all.get(paths.get(bit)))));
                    }
                }
                if (reverse) Collections.reverse(before);
                var run = M3JavaUtilSortsApply.evaluate(
                        new M3HashPinnedJavaSnapshotRecipe("sort-add-fixture"), before);
                assertEquals(3 - Integer.bitCount(mask), run.changed().size());
                assertEquals(sealedRoot, root(texts(run.sources())));
                assertEquals(all, texts(run.sources()));
                assertTrue(M3JavaUtilSortsApply.evaluate(
                        new M3HashPinnedJavaSnapshotRecipe("sort-add-fixture"),
                        run.sources()).changed().isEmpty());
            }
        }
    }

    @Test
    void frozenBehaviorAndPublicContractSurviveEachAdmittedReplacementPass() throws Exception {
        Map<String, String> before = new TreeMap<>(expected());
        before.put(NUMBERS, resource("java", "sort-replace-fixture", "Numbers-before.java.txt"));
        List<String> baseline = compileAndObserve(before);
        Recipe recipe = new M3HashPinnedJavaSnapshotRecipe("sort-replace-fixture");
        var first = M3JavaUtilSortsApply.evaluate(recipe, parse(before));
        assertEquals(List.of(NUMBERS), first.changed());
        assertEquals(expected(), texts(first.sources()));
        assertEquals(baseline, compileAndObserve(texts(first.sources())));
        var fixed = M3JavaUtilSortsApply.evaluate(recipe, first.sources());
        assertTrue(fixed.changed().isEmpty());
        assertEquals(first.sources(), fixed.sources());
    }

    @Test
    void commentsStringsRegexesAndTextBlocksRemainByteExactAndCompile() throws Exception {
        var run = M3JavaUtilSortsApply.evaluate(
                new M3HashPinnedJavaSnapshotRecipe("sort-add-fixture"), List.of());
        String carrier = texts(run.sources()).get(CARRIER);
        assertEquals(resource("java", "sort-add-fixture", "Carrier.java.txt"), carrier);
        assertTrue(carrier.contains("for (;;) { return -1; }"));
        assertTrue(carrier.contains("public void rewriteMe() { dangerous(); }"));
        assertTrue(carrier.contains("public int method() { return 999; }"));
        assertTrue(carrier.contains("\\\\breturn\\\\s+\\\\d+\\\\b"));
        assertTrue(compileAndObserve(texts(run.sources())).stream()
                .anyMatch(value -> value.contains("if (pretend)")));
    }

    @Test
    void oneAndTwoWorkerPermutationsHaveTheSameSerialReplayRoot() throws Exception {
        String expectedRoot = root(expected());
        for (int workers : List.of(1, 2)) {
            try (var executor = Executors.newFixedThreadPool(workers)) {
                List<Callable<String>> trials = new ArrayList<>();
                for (int trial = 0; trial < 4; trial++) {
                    final int selected = trial;
                    trials.add(() -> {
                        Map<String, String> input = new TreeMap<>();
                        if ((selected & 1) != 0) input.put(CARRIER, expected().get(CARRIER));
                        if ((selected & 2) != 0) input.put(EXTRA, expected().get(EXTRA));
                        List<SourceFile> parsed = new ArrayList<>(parse(input));
                        Collections.reverse(parsed);
                        var recipe = new M3HashPinnedJavaSnapshotRecipe("sort-add-fixture");
                        var first = M3JavaUtilSortsApply.evaluate(recipe, parsed);
                        assertTrue(M3JavaUtilSortsApply.evaluate(
                                recipe, first.sources()).changed().isEmpty());
                        return root(texts(first.sources()));
                    });
                }
                for (var future : executor.invokeAll(trials)) {
                    assertEquals(expectedRoot, future.get());
                }
            }
        }
        assertEquals(expectedRoot, root(texts(M3JavaUtilSortsApply.evaluate(
                new M3HashPinnedJavaSnapshotRecipe("sort-add-fixture"), List.of()).sources())));
    }

    @Test
    void foreignJavaOccupantRefusesBeforeAnyAbsentFileIsCreated() {
        List<SourceFile> drift = parse(Map.of(NUMBERS,
                expected().get(NUMBERS) + "// unreviewed source drift\n"));
        assertActualRunRefuses(new M3HashPinnedJavaSnapshotRecipe("sort-add-fixture"),
                drift, "source drift");
    }

    @Test
    void anExpectedByteSequenceInPlainTextIsNotJavaParserAdmission() {
        SourceFile text = PlainText.builder().sourcePath(Path.of(NUMBERS))
                .text(expected().get(NUMBERS)).build();
        assertActualRunRefuses(new M3HashPinnedJavaSnapshotRecipe("sort-add-fixture"),
                List.of(text), "not a Java compilation unit");
    }

    @Test
    void duplicatePhysicalJavaTargetsRefuseInActualRun() {
        SourceFile one = parse(Map.of(NUMBERS, expected().get(NUMBERS))).getFirst();
        SourceFile duplicate = one.withId(UUID.randomUUID());
        assertActualRunRefuses(new M3HashPinnedJavaSnapshotRecipe("sort-add-fixture"),
                List.of(one, duplicate), "duplicate target");
    }

    @Test
    void missingRequiredPreimageIsNotMistakenForAbsentAdmission() {
        assertActualRunRefuses(new M3HashPinnedJavaSnapshotRecipe("sort-replace-fixture"),
                List.of(), "required source missing");
    }

    @Test
    void sealedTemplateHashMalformedJavaAndNonCanonicalOrderAllRefuse() {
        assertActualRunRefuses(new M3HashPinnedJavaSnapshotRecipe("sort-bad-hash-fixture"),
                List.of(), "Java template hash drift");
        assertActualRunRefuses(new M3HashPinnedJavaSnapshotRecipe("sort-malformed-fixture"),
                List.of(), "Java template parse");
        assertActualRunRefuses(new M3HashPinnedJavaSnapshotRecipe("sort-unsorted-fixture"),
                List.of(), "invalid hash-pinned Java manifest row");
        assertThrows(IllegalArgumentException.class,
                () -> new M3HashPinnedJavaSnapshotRecipe("../escape"));
    }

    @Test
    void textLaneKeepsCodeLookingDataOpaqueAndHasItsOwnFixedPointAndRefusal() {
        Recipe recipe = new M3HashPinnedTextSnapshotRecipe("sort-text-fixture");
        var first = M3JavaUtilSortsApply.evaluate(recipe, List.of());
        assertEquals(2, first.changed().size());
        assertTrue(first.sources().stream().allMatch(PlainText.class::isInstance));
        assertEquals(resource("text", "sort-text-fixture", "Fixture.md.txt"),
                texts(first.sources()).get("m3/docs/sort-fixture.md"));
        assertTrue(M3JavaUtilSortsApply.evaluate(recipe, first.sources()).changed().isEmpty());
        SourceFile occupant = PlainText.builder().sourcePath(Path.of("m3/docs/sort-fixture.md"))
                .text("# unrelated owner\n").build();
        assertActualRunRefuses(recipe, List.of(occupant), "source drift");
        assertActualRunRefuses(new M3HashPinnedTextSnapshotRecipe("sort-text-bad-hash-fixture"),
                List.of(), "text template hash drift");
    }

    @Test
    void disjointJavaAndTextRecipeOrdersPreserveTheSameExactOutput() {
        Map<String, String> sealed = null;
        for (boolean reverse : List.of(false, true)) {
            Recipe combined = new Recipe() {
                @Override public String getDisplayName() { return "Bounded fixture composition"; }
                @Override public String getDescription() { return "Tests the two existing owners."; }
                @Override public int maxCycles() { return 1; }
                @Override public List<Recipe> getRecipeList() {
                    Recipe java = new M3HashPinnedJavaSnapshotRecipe("sort-add-fixture");
                    Recipe text = new M3HashPinnedTextSnapshotRecipe("sort-text-fixture");
                    return reverse ? List.of(text, java) : List.of(java, text);
                }
            };
            SourceFile sentinel = PlainText.builder().sourcePath(Path.of("unrelated.txt"))
                    .text("Do not change: class Fake { return 123; }\n").build();
            var run = M3JavaUtilSortsApply.evaluate(combined, List.of(sentinel));
            assertEquals(5, run.changed().size());
            assertEquals(sentinel.printAll(), texts(run.sources()).get("unrelated.txt"));
            assertTrue(M3JavaUtilSortsApply.evaluate(combined, run.sources()).changed().isEmpty());
            if (sealed == null) sealed = texts(run.sources());
            else assertEquals(sealed, texts(run.sources()));
        }
    }

    @Test
    void interleavedEditAfterScanStillRefusesInsteadOfOverwritingTheNewOwner() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("sort-add-fixture");
        var context = new InMemoryExecutionContext(error -> {
            throw new IllegalStateException(error);
        });
        var inventory = recipe.getInitialValue(context);
        SourceFile original = parse(Map.of(NUMBERS, expected().get(NUMBERS))).getFirst();
        recipe.getScanner(inventory).visit(original, context);
        recipe.generate(inventory, context);
        SourceFile edited = parse(Map.of(NUMBERS,
                expected().get(NUMBERS) + "// changed between scan and visit\n")).getFirst();
        RuntimeException refusal = assertThrows(RuntimeException.class,
                () -> recipe.getVisitor(inventory).visit(edited, context));
        assertTrue(messages(refusal).contains("target changed after scan"));
    }

    private static void assertActualRunRefuses(
            Recipe recipe, List<SourceFile> inputs, String signature) {
        List<Throwable> failures = new ArrayList<>();
        RecipeRun run = null;
        try {
            run = recipe.run(new InMemoryLargeSourceSet(inputs),
                    new InMemoryExecutionContext(failures::add), 1);
        } catch (RuntimeException failure) {
            failures.add(failure);
        }
        assertFalse(failures.isEmpty(), "Expected a real OpenRewrite refusal");
        assertTrue(failures.stream().anyMatch(failure -> messages(failure).contains(signature)),
                () -> "Wrong refusal fingerprint: " + failures.stream()
                        .map(M3RecipeReplayTest::messages).toList());
        if (run != null) {
            assertTrue(run.getChangeset().getAllResults().stream()
                    .noneMatch(change -> change.getBefore() == null && change.getAfter() != null),
                    "Refusal emitted an absent target");
        }
    }

    private static List<SourceFile> parse(Map<String, String> texts) {
        if (texts.isEmpty()) return List.of();
        List<Throwable> failures = new ArrayList<>();
        List<Parser.Input> inputs = texts.entrySet().stream()
                .map(entry -> Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue()))
                .toList();
        List<SourceFile> result = JavaParser.fromJavaVersion().build()
                .parseInputs(inputs, null, new InMemoryExecutionContext(failures::add)).toList();
        assertTrue(failures.isEmpty(), () -> failures.toString());
        assertEquals(texts, texts(result));
        assertTrue(result.stream().allMatch(J.CompilationUnit.class::isInstance));
        return result;
    }

    private List<String> compileAndObserve(Map<String, String> texts) throws Exception {
        Path work = Files.createTempDirectory(temp, "compile-");
        Path classes = Files.createDirectory(work.resolve("classes"));
        List<String> arguments = new ArrayList<>(
                List.of("--release", "21", "-proc:none", "-Xlint:all", "-Werror", "-d", classes.toString()));
        for (var entry : texts.entrySet()) {
            if (!entry.getKey().endsWith(".java")) continue;
            Path file = work.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
            arguments.add(file.toString());
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Java 21 compiler is a required authority");
        assertEquals(0, compiler.run(null, null, null, arguments.toArray(String[]::new)));
        List<String> observations = new ArrayList<>();
        try (var loader = new URLClassLoader(new URL[] {classes.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> numbers = loader.loadClass("fixture.Numbers");
            observations.add("class=" + Modifier.toString(numbers.getModifiers()));
            Arrays.stream(numbers.getDeclaredMethods())
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .map(method -> "api=" + method.toGenericString()).sorted()
                    .forEach(observations::add);
            var method = numbers.getMethod("fold", int[].class);
            List<int[]> samples = Arrays.asList(new int[0], new int[] {1, 2, 3},
                    new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE},
                    new int[] {9, -8, -1}, new int[] {Integer.MAX_VALUE, 1}, null);
            for (int[] sample : samples) {
                try {
                    observations.add("fold=" + method.invoke(null, new Object[] {sample}));
                } catch (InvocationTargetException failure) {
                    Throwable cause = failure.getCause();
                    observations.add("throw=" + cause.getClass().getName() + ":" + cause.getMessage());
                }
            }
            observations.add("carrier=" + loader.loadClass("fixture.Carrier")
                    .getMethod("observe").invoke(null));
            observations.add("extra=" + loader.loadClass("fixture.Extra")
                    .getMethod("value", int.class).invoke(null, 7));
        }
        return List.copyOf(observations);
    }

    private static Map<String, String> expected() {
        Map<String, String> result = new TreeMap<>();
        result.put(CARRIER, resource("java", "sort-add-fixture", "Carrier.java.txt"));
        result.put(EXTRA, resource("java", "sort-add-fixture", "Extra.java.txt"));
        result.put(NUMBERS, resource("java", "sort-add-fixture", "Numbers.java.txt"));
        return result;
    }

    static Map<String, String> texts(List<SourceFile> sources) {
        Map<String, String> result = new TreeMap<>();
        for (SourceFile source : sources) {
            result.put(source.getSourcePath().toString().replace('\\', '/'), source.printAll());
        }
        return result;
    }

    private static String root(Map<String, String> sources) {
        StringBuilder text = new StringBuilder();
        new TreeMap<>(sources).forEach((path, value) -> text.append(path).append('\t')
                .append(M3JavaUtilSortsApply.sha256(value)).append('\n'));
        return M3JavaUtilSortsApply.sha256(text.toString());
    }

    private static String resource(String lane, String crate, String file) {
        String path = "/com/synexia/rewrite/hash-pinned-" + lane + "/" + crate + "/" + file;
        try (var input = M3RecipeReplayTest.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String messages(Throwable failure) {
        StringBuilder result = new StringBuilder();
        for (Throwable next = failure; next != null; next = next.getCause()) {
            result.append(next.getClass().getName()).append(':').append(next.getMessage()).append('\n');
        }
        return result.toString();
    }
}
