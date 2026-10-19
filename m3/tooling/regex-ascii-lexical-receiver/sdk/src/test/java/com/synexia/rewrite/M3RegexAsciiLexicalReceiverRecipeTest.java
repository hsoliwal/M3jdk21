/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.*;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Public receiver-only source recipes; private owner, component runtime and matched JDK proof remain separate. */
@Execution(ExecutionMode.SAME_THREAD)
final class M3RegexAsciiLexicalReceiverRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/regex-ascii-lexical-receiver-20261019/";
    private static final String DESCRIPTOR = "/META-INF/rewrite/regex-ascii-lexical-receiver-20261019.yml";
    private static final Set<String> CHANGE = Set.of(
            "src/java.base/share/classes/jdk/internal/mindex/M3RegExpCompiler.java",
            "test/jdk/java/util/regex/M3AutomatonDifferentialTest.java",
            "m3/docs/name-mapping.json");
    private static final Set<String> KEEP = Set.of(
            "src/java.base/share/classes/jdk/internal/mindex/M3Automaton.java",
            "src/java.base/share/classes/jdk/internal/mindex/M3AutomatonOps.java",
            "src/java.base/share/classes/jdk/internal/mindex/M3RunTable.java",
            "src/java.base/share/classes/jdk/internal/mindex/M3ChunkCursor.java",
            "src/java.base/share/classes/jdk/internal/mindex/M3NotYetReceived.java");
    private static final Set<String> PASSED = new LinkedHashSet<>();
    private static int forwardApplies;
    private static int inverseApplies;
    private static int refusedRuns;
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3RegexAsciiLexicalReceiverRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest(String direction) {
        assertTrue(direction.equals("forward") || direction.equals("inverse"));
        return M3GpuToolJoinManifest.load(ROOT + direction + "/",
                M3RegexAsciiLexicalReceiverRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static void validateComposition() throws IOException {
        var forward = manifest("forward");
        var inverse = manifest("inverse");
        assertEquals(8, forward.targets().size());
        assertEquals(8, inverse.targets().size());
        Set<String> all = new HashSet<>(CHANGE);
        all.addAll(KEEP);
        assertEquals(8, all.size());
        assertEquals(all, forward.targets().stream().map(M3GpuToolJoinManifest.Target::path)
                .collect(java.util.stream.Collectors.toSet()));
        assertEquals(all, inverse.targets().stream().map(M3GpuToolJoinManifest.Target::path)
                .collect(java.util.stream.Collectors.toSet()));
        for (String direction : List.of("forward", "inverse")) {
            var current = manifest(direction);
            assertEquals(CHANGE, current.targets().stream()
                    .filter(row -> !row.before().equals(row.after()))
                    .map(M3GpuToolJoinManifest.Target::path)
                    .collect(java.util.stream.Collectors.toSet()));
            for (var row : current.targets()) {
                assertFalse(row.addition() || row.removal(), row.path());
                String before = contents(direction + "/before/" + row.path());
                String after = contents(direction + "/out/" + row.path());
                assertEquals(row.before(), M3GpuToolJoinManifest.sha256(before), row.path());
                assertEquals(row.after(), M3GpuToolJoinManifest.sha256(after), row.path());
                if (KEEP.contains(row.path())) {
                    assertTrue(before.equals(after), "KEEP bytes differ: " + row.path());
                } else {
                    assertTrue(after.equals(row.text()), "sealed output differs: " + row.path());
                }
            }
        }
        for (var row : forward.targets()) {
            var reverse = inverse.target(row.path());
            assertNotNull(reverse, row.path());
            assertEquals(row.after(), reverse.before(), row.path());
            assertEquals(row.before(), reverse.after(), row.path());
            assertTrue(contents("forward/before/" + row.path()).equals(
                    contents("inverse/out/" + row.path())), "full inverse differs: " + row.path());
            assertTrue(contents("forward/out/" + row.path()).equals(
                    contents("inverse/before/" + row.path())), "full inverse preimage differs: " + row.path());
        }
    }
    private static Map<String, String> before(String direction) throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var row : manifest(direction).targets()) {
            input.put(row.path(), contents(direction + "/before/" + row.path()));
        }
        input.put("opaque/Unrelated.txt", "Character.digit and regex-looking text stay opaque here\n");
        return input;
    }
    private static List<SourceFile> sources(Map<String, String> input, boolean reverse) {
        var entries = new ArrayList<>(input.entrySet());
        if (reverse) Collections.reverse(entries);
        var result = new ArrayList<SourceFile>();
        for (var entry : entries) {
            result.add(PlainText.builder().sourcePath(Path.of(entry.getKey()))
                    .text(entry.getValue()).build());
        }
        return result;
    }
    private static Recipe named(String direction) throws IOException {
        String name = direction.equals("forward")
                ? "com.synexia.m3.RegexAsciiLexicalReceiver" : "com.synexia.m3.RegexAsciiLexicalReceiverInverse";
        Class<?> adapter = direction.equals("forward")
                ? M3RegexAsciiLexicalReceiverRecipe.class : M3RegexAsciiLexicalReceiverInverseRecipe.class;
        try (var stream = M3RegexAsciiLexicalReceiverRecipeTest.class.getResourceAsStream(DESCRIPTOR)) {
            assertNotNull(stream, DESCRIPTOR);
            var environment = Environment.builder().load(new YamlResourceLoader(stream,
                    URI.create("classpath:" + DESCRIPTOR), new Properties(),
                    M3RegexAsciiLexicalReceiverRecipe.class.getClassLoader())).build();
            assertThrows(RecipeException.class, () -> environment.activateRecipes(name + ".Unknown"));
            Recipe recipe = environment.activateRecipes(name);
            assertEquals(1, recipe.getRecipeList().size());
            Recipe declaration = recipe.getRecipeList().get(0);
            assertEquals(name, declaration.getName());
            assertEquals(DeclarativeRecipe.class, declaration.getClass());
            assertEquals(List.of(adapter),
                    declaration.getRecipeList().stream().map(Object::getClass).toList());
            return recipe;
        }
    }
    private static Applied apply(Map<String, String> input, String direction, boolean reverse)
            throws IOException {
        var original = new LinkedHashMap<>(input);
        var errors = new ArrayList<Throwable>();
        var run = named(direction).run(new InMemoryLargeSourceSet(sources(input, reverse)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var output = new LinkedHashMap<>(input);
        var results = run.getChangeset().getAllResults();
        for (var result : results) {
            assertNotNull(result.getAfter());
            output.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                    result.getAfter().printAll());
        }
        assertTrue(original.equals(input), "proposal mutated its source input");
        if (direction.equals("forward")) forwardApplies++; else inverseApplies++;
        return new Applied(output, results.size());
    }
    private static void exerciseStates(String direction) throws IOException {
        validateComposition();
        var targets = manifest(direction).targets().stream().toList();
        var changed = targets.stream().filter(row -> !row.before().equals(row.after())).toList();
        assertEquals(3, changed.size());
        for (int state = 0; state < 8; state++) {
            var input = before(direction);
            for (int bit = 0; bit < changed.size(); bit++) {
                if ((state & (1 << bit)) != 0) {
                    var target = changed.get(bit);
                    input.put(target.path(), target.text());
                }
            }
            for (boolean reverse : new boolean[]{false, true}) {
                var first = apply(input, direction, reverse);
                assertEquals(3 - Integer.bitCount(state), first.changes());
                assertEquals(input.keySet(), first.files().keySet());
                for (var row : targets) {
                    assertEquals(row.after(), M3GpuToolJoinManifest.sha256(
                            first.files().get(row.path())), row.path());
                    assertTrue(contents(direction + "/out/" + row.path()).equals(
                            first.files().get(row.path())), "exact output differs: " + row.path());
                }
                assertEquals(input.get("opaque/Unrelated.txt"), first.files().get("opaque/Unrelated.txt"));
                var second = apply(first.files(), direction, !reverse);
                assertEquals(0, second.changes());
                assertTrue(first.files().equals(second.files()), "fixed point changed source bytes");
                if (state == 0 && !reverse) {
                    for (var row : changed) retain(direction, row.path(), first.files().get(row.path()));
                }
            }
        }
    }

    @Test void all8PartialStatesBothOrdersReachExactFixedPoint() throws Exception {
        exerciseStates("forward");
        PASSED.add("all8PartialStatesBothOrdersReachExactFixedPoint");
    }
    @Test void all8InverseStatesBothOrdersRecoverExactBefore() throws Exception {
        exerciseStates("inverse");
        PASSED.add("all8InverseStatesBothOrdersRecoverExactBefore");
    }
    @Test void eachOwnedInputRefusesDriftMissingAndDuplicateOwnership() throws Exception {
        validateComposition();
        for (String direction : List.of("forward", "inverse")) {
            for (var row : manifest(direction).targets()) {
                var drift = before(direction);
                drift.put(row.path(), drift.get(row.path()) + "\nunknown source drift\n");
                reject(sources(drift, false), direction, "source drift: " + row.path());
                var missing = before(direction);
                missing.remove(row.path());
                reject(sources(missing, true), direction, "required source missing: " + row.path());
                var duplicate = sources(before(direction), false);
                duplicate.add(PlainText.builder().sourcePath(Path.of(row.path()))
                        .text(before(direction).get(row.path())).build());
                reject(duplicate, direction, "duplicate source target: " + row.path());
            }
        }
        PASSED.add("eachOwnedInputRefusesDriftMissingAndDuplicateOwnership");
    }
    private static void reject(List<SourceFile> input, String direction, String signature)
            throws IOException {
        var original = input.stream().map(file -> file.getSourcePath() + "\n" + file.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        try {
            var run = named(direction).run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add));
            assertTrue(run.getChangeset().getAllResults().isEmpty(), "refusal produced output");
        } catch (IllegalStateException refusal) {
            errors.add(refusal);
        }
        StringBuilder observed = new StringBuilder();
        for (Throwable error : errors) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                observed.append(cause).append('\n');
            }
        }
        assertTrue(observed.toString().contains(signature), observed.toString());
        assertTrue(original.equals(input.stream()
                .map(file -> file.getSourcePath() + "\n" + file.printAll()).toList()),
                "refusal changed original sources");
        refusedRuns++;
    }
    private static void retain(String direction, String name, String text) throws IOException {
        Path relative = Path.of(name);
        assertFalse(relative.isAbsolute());
        assertEquals(relative, relative.normalize());
        Path root = Path.of("target/emitted/regex-ascii-lexical-receiver-20261019").toAbsolutePath().normalize();
        Path output = root.resolve(direction).resolve(relative).normalize();
        assertTrue(output.startsWith(root));
        for (Path cursor = output.getParent(); cursor != null; cursor = cursor.getParent()) {
            assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
        }
        Files.createDirectories(output.getParent());
        for (Path cursor = output.getParent(); cursor != null; cursor = cursor.getParent()) {
            assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
        }
        Files.writeString(output, text, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    @AfterAll static void retainCountersOnlyAfterAllNamedMethodsPass() throws IOException {
        assertEquals(Set.of("all8PartialStatesBothOrdersReachExactFixedPoint",
                "all8InverseStatesBothOrdersRecoverExactBefore",
                "eachOwnedInputRefusesDriftMissingAndDuplicateOwnership"), PASSED);
        assertEquals(32, forwardApplies);
        assertEquals(32, inverseApplies);
        assertEquals(48, refusedRuns);
        String id = System.getenv("M3_QUALIFICATION_ID"), head = System.getenv("EXPECTED_SOURCE_SHA");
        assertNotNull(id); assertNotNull(head);
        assertTrue(id.matches("[A-Za-z0-9_.-]{16,128}"));
        assertTrue(head.matches("[0-9a-f]{40}"));
        Path target = Path.of("target").toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS));
        for (Path cursor = target; cursor != null; cursor = cursor.getParent()) {
            assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
        }
        String json = """
                {
                  "schema": 1,
                  "qualificationId": "%s",
                  "checkoutCommit": "%s",
                  "tests": 3,
                  "forwardApplies": 32,
                  "inverseApplies": 32,
                  "refusedRuns": 48,
                  "emittedOutputs": 6,
                  "sourceOnly": true,
                  "runtimeApplied": false,
                  "phaseAdvanced": false
                }
                """.formatted(id, head);
        Files.writeString(target.resolve("recipe-test-counters.json"), json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.println("M3_REGEX_ASCII_LEXICAL_RECEIVER_RECIPE_COUNTERS forward=32 inverse=32"
                + " refused=48 outputs=6 qualificationId=" + id);
    }
}
