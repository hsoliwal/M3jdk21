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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.*;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Named SDK source custody only; actual owner compilation and JNI remain separate. */
@Execution(ExecutionMode.SAME_THREAD)
final class M3StateRelationCurrentC11RecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/state-relation-current-c11-20261019/";
    private static final String DESCRIPTOR = "/META-INF/rewrite/state-relation-current-c11-20261019.yml";
    private static final String NAME = "com.synexia.m3.StateRelationCurrentC11";
    private static final String MANIFEST_SHA = "fd764eea326f6167bf8447d17fbe953341f6536b416a6e88fd518275c73ea78b";
    private static final String DESCRIPTOR_SHA = "ad3e3dcc318bd0adf19b6a4552a1b4666189e2ce04b66415201b1df9aa7a355a";
    private static final String C_OWNER = "src/java.base/share/native/libjava/M3StateRelation.c";
    private static final String HISTORICAL = "/com/synexia/rewrite/state-relation-current-c11-20261019/historical/M3StateRelation.c.txt";
    private static final String HISTORICAL_SHA = "66d28e350f4894498b15c0041c0a43c075f12c9e3a384214e45d9868ce1950b6";
    private static final Map<String, String> EXPECTED = Map.ofEntries(
            Map.entry("src/java.base/share/classes/jdk/internal/mindex/M3StateRelation.java", "3270f7889a8c94fa15c90e88491b7fbe483d5ad3053ab36d7e07a63c4c734caa"),
            Map.entry("src/java.base/share/classes/jdk/internal/mindex/M3DotStarRelations.java", "bbc2901ebe5b2de9951b672127bd47e9d13c70b5a39482fa0727703d0a2ff757"),
            Map.entry("src/java.base/share/native/libjava/M3StateRelation.c", "b1bfd040a1454ac06ab3f96952d97c8c5a6509dcf55dbba9797b6517eea2ef16"),
            Map.entry("src/java.base/share/native/libjava/m3_state_relation.h", "c948c0ac34180f1f9412a764c3c9fd8c59e11509467aa9e04f8825e903526885"),
            Map.entry("m3/compatibility/regex-relations-20261014/M3RelationPortProof.java", "bb77a4d48ed803e0503a8e497ec144748dd87d05b1c659971273d15b5bd89606"));
    private static final Set<String> PASSED = new LinkedHashSet<>();
    private static int successfulRuns;
    private static int refusedRuns;
    private record Applied(Map<String, String> files, int changes) { }

    private static String resource(String name) throws IOException {
        try (var stream = M3StateRelationCurrentC11RecipeTest.class.getResourceAsStream(name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3StateRelationCurrentC11RecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static void validateComposition() throws IOException {
        assertEquals(MANIFEST_SHA, M3GpuToolJoinManifest.sha256(resource(ROOT + "manifest.tsv")));
        var rows = manifest().targets();
        assertEquals(5, rows.size());
        var seen = new HashSet<String>();
        for (var row : rows) {
            assertTrue(seen.add(row.path()), "duplicate manifest owner");
            assertEquals(EXPECTED.get(row.path()), row.before(), row.path());
            assertEquals(row.before(), row.after(), row.path());
            assertFalse(row.addition() || row.removal(), row.path());
            // KEEP deliberately has no payload: never dereference row.text().
            assertNull(row.text(), row.path());
            String before = resource(ROOT + "before/" + row.path());
            String after = resource(ROOT + "out/" + row.path());
            assertEquals(row.before(), M3GpuToolJoinManifest.sha256(before), row.path());
            assertEquals(row.after(), M3GpuToolJoinManifest.sha256(after), row.path());
            assertTrue(before.equals(after), "KEEP fixture bytes differ: " + row.path());
        }
        assertEquals(EXPECTED.keySet(), seen);
    }
    private static Map<String, String> current() throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var row : manifest().targets()) {
            input.put(row.path(), resource(ROOT + "before/" + row.path()));
        }
        input.put("opaque/Unrelated.txt", "StateRelation C/JNI-looking text remains opaque\n");
        return input;
    }
    private static Recipe named() throws IOException {
        assertEquals(DESCRIPTOR_SHA, M3GpuToolJoinManifest.sha256(resource(DESCRIPTOR)));
        try (var stream = M3StateRelationCurrentC11RecipeTest.class.getResourceAsStream(DESCRIPTOR)) {
            assertNotNull(stream, DESCRIPTOR);
            var environment = Environment.builder().load(new YamlResourceLoader(stream,
                    URI.create("classpath:" + DESCRIPTOR), new Properties(),
                    M3StateRelationCurrentC11Recipe.class.getClassLoader())).build();
            assertThrows(RecipeException.class, () -> environment.activateRecipes(NAME + ".Unknown"));
            Recipe activated = environment.activateRecipes(NAME);
            assertEquals(1, activated.getRecipeList().size());
            Recipe declaration = activated.getRecipeList().get(0);
            assertEquals(NAME, declaration.getName());
            assertEquals(DeclarativeRecipe.class, declaration.getClass());
            assertEquals(List.of(M3StateRelationCurrentC11Recipe.class),
                    declaration.getRecipeList().stream().map(Object::getClass).toList());
            return activated;
        }
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
    private static Applied apply(Map<String, String> input, boolean reverse) throws IOException {
        var original = new LinkedHashMap<>(input);
        var files = sources(input, reverse);
        var snapshot = files.stream().map(f -> f.getSourcePath() + "\n" + f.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        var run = named().run(new InMemoryLargeSourceSet(files),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var results = run.getChangeset().getAllResults();
        assertTrue(results.isEmpty(), "KEEP recipe emitted product source");
        assertTrue(original.equals(input), "proposal mutated input map");
        assertEquals(snapshot, files.stream().map(f -> f.getSourcePath() + "\n" + f.printAll()).toList());
        successfulRuns++;
        return new Applied(new LinkedHashMap<>(input), results.size());
    }
    private static void reject(List<SourceFile> input, String signature) throws IOException {
        var before = input.stream().map(f -> f.getSourcePath() + "\n" + f.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        try {
            var run = named().run(new InMemoryLargeSourceSet(input),
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
        assertEquals(before, input.stream().map(f -> f.getSourcePath() + "\n" + f.printAll()).toList());
        refusedRuns++;
    }

    @Test void exactFiveOwnersReplayAsFixedPointInBothOrders() throws Exception {
        validateComposition();
        for (boolean reverse : new boolean[]{false, true}) {
            var input = current();
            var first = apply(input, reverse);
            assertEquals(0, first.changes());
            assertEquals(input, first.files());
            var second = apply(first.files(), !reverse);
            assertEquals(0, second.changes());
            assertEquals(first.files(), second.files());
        }
        PASSED.add("exactFiveOwnersReplayAsFixedPointInBothOrders");
    }

    @Test void everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation() throws Exception {
        for (String path : EXPECTED.keySet()) {
            var drift = current();
            drift.put(path, drift.get(path) + "\n/* unknown owner drift */\n");
            reject(sources(drift, false), "source drift: " + path);
            var missing = current();
            missing.remove(path);
            reject(sources(missing, true), "required source missing: " + path);
            var duplicate = sources(current(), false);
            duplicate.add(PlainText.builder().sourcePath(Path.of(path)).text(current().get(path)).build());
            reject(duplicate, "duplicate source target: " + path);
        }
        PASSED.add("everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation");
    }

    @Test void historicalCRefusesCurrentOwnership() throws Exception {
        String old = resource(HISTORICAL);
        assertEquals(HISTORICAL_SHA, M3GpuToolJoinManifest.sha256(old));
        assertNotEquals(EXPECTED.get(C_OWNER), HISTORICAL_SHA);
        assertNull(manifest().target(HISTORICAL), "audit fixture is not a recipe target");
        var input = current();
        input.put(C_OWNER, old);
        reject(sources(input, true), "source drift: " + C_OWNER);
        PASSED.add("historicalCRefusesCurrentOwnership");
    }

    @AfterEach void retainCountersOnlyAfterAllNamedMethodsPass() throws IOException {
        if (PASSED.size() < 3) return;
        assertEquals(Set.of("exactFiveOwnersReplayAsFixedPointInBothOrders",
                "everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation",
                "historicalCRefusesCurrentOwnership"), PASSED);
        assertEquals(4, successfulRuns);
        assertEquals(16, refusedRuns);
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
                  "successfulRuns": 4,
                  "refusedRuns": 16,
                  "emittedOutputs": 0,
                  "sourceOnly": true,
                  "runtimeApplied": false,
                  "phaseAdvanced": false
                }
                """.formatted(id, head);
        Files.writeString(target.resolve("recipe-test-counters.json"), json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.println("M3_STATE_RELATION_CURRENT_C11_RECIPE_COUNTERS successful=4"
                + " refused=16 outputs=0 qualificationId=" + id);
    }
}
