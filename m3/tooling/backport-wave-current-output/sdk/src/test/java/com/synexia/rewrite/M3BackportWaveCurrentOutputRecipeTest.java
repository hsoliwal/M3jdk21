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

/** Real named recipe replay only; executing the actual workflow remains a separate gate. */
@Execution(ExecutionMode.SAME_THREAD)
final class M3BackportWaveCurrentOutputRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/backport-wave-current-output-20261020/";
    private static final String DESCRIPTOR = "/META-INF/rewrite/backport-wave-current-output-20261020";
    private static final String NAME = "com.synexia.m3.BackportWaveCurrentOutput";
    private static final String FORWARD_MANIFEST_SHA = "8478e660b6e535bf1f069ce16da558a9f1d0dd1e1e17626cd8ec908d041cdf9a";
    private static final String INVERSE_MANIFEST_SHA = "e2064f6bdf6d301653c9e1112dba61bc9f6a543023b44f69805129b5866baf52";
    private static final String FORWARD_DESCRIPTOR_SHA = "80d565a20347d2afc987ac59ee6303cb298fc218fb1dc880583ad3bfe489292f";
    private static final String INVERSE_DESCRIPTOR_SHA = "3fd03781353bcf05bf26228fc0df2817d177fe012ecc3eb4b38a451bdc9df624";
    private static final String WAVE_OWNER = ".github/workflows/m3-jdk21-backport-wave-admission.yml";
    private static final String WAVE_AFTER = "b64ac1da918bd1b5fd5dd56adc16aeb9705a854cfd262e0793fe79428ac450d6";
    private static final Map<String, String> BEFORE = Map.ofEntries(
            Map.entry("m3/backports/test_compatibility_policy.py", "69694e6e3725aeee8687a8c58bce477c7256f626d8425c04a9ce9772250a2bb6"),
            Map.entry("m3/backports/compatibility_policy.py", "d51301d3ea225e89b000a2256855ab7806a1faf20c5fa493eb5666de1e7880c2"),
            Map.entry("m3/backports/POST21_PRIORITY_COMPATIBILITY.tsv", "4d813438bb68106c65781b3891c43afd4b35d288e0440ec4d27f83305b780c1a"),
            Map.entry("m3/backports/JEP_CATALOGUE.tsv", "d47f07c2ee9829bfb91f4c4e2162bb221e86206aada802e0580aaef79d7fd250"),
            Map.entry("m3/backports/RELEASE_JEP_AUTHORITY.tsv", "05341181f89e150ae602644ddaca9d53596be533cb2695e4032f11c0e1a70709"),
            Map.entry("m3/backports/test_backport_execution_waves.py", "5a59d038e108010c484f66f545ab5bc745a791182ed4794556b414ffde315dd0"),
            Map.entry("m3/backports/backport_execution_waves.py", "2cb19465a6a0d4f24dc228921eb1a2d7c2bbc4e53ce68e635b0e60ddb2a563dc"),
            Map.entry("m3/backports/BACKPORT_EXECUTION_WAVES.md", "df2aca41ab120bdbc8c165d4abe3552c54a91c591aab116b718b94283ae23f28"),
            Map.entry("m3/compatibility/synexia-wave-admission-pin-20261014.tsv", "984af6023eb4f4e0b917a700c5e75bd936083b7ae2f0daef07c37ad6a3e2ddd4"),
            Map.entry(".github/workflows/m3-jdk21-backport-wave-admission.yml", "622491eea231863f0b595ead49711b5c60f63ae8ea0bf408f44a71ec84de2c85"));
    private static final Set<String> PASSED = new LinkedHashSet<>();
    private static final Map<Boolean, String> EMITTED = new HashMap<>();
    private static int successfulRuns;
    private static int refusedRuns;
    private record Applied(Map<String, String> files, int changes) { }

    private static String resource(String path) throws IOException {
        try (var stream = M3BackportWaveCurrentOutputRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest(boolean inverse) {
        return M3GpuToolJoinManifest.load(ROOT + (inverse ? "inverse/" : ""),
                M3BackportWaveCurrentOutputRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static void validateComposition() throws IOException {
        for (boolean inverse : new boolean[]{false, true}) {
            String manifestPath = ROOT + (inverse ? "inverse/" : "") + "manifest.tsv";
            assertEquals(inverse ? INVERSE_MANIFEST_SHA : FORWARD_MANIFEST_SHA,
                    M3GpuToolJoinManifest.sha256(resource(manifestPath)));
            var rows = manifest(inverse).targets();
            assertEquals(10, rows.size());
            var seen = new HashSet<String>();
            int changed = 0;
            for (var row : rows) {
                assertTrue(seen.add(row.path()), "duplicate manifest owner");
                boolean replace = row.path().equals(WAVE_OWNER);
                String oldHash = inverse && replace ? WAVE_AFTER : BEFORE.get(row.path());
                String newHash = !inverse && replace ? WAVE_AFTER : BEFORE.get(row.path());
                assertEquals(oldHash, row.before(), row.path());
                assertEquals(newHash, row.after(), row.path());
                assertFalse(row.addition() || row.removal(), row.path());
                String before = resource(ROOT + "before/" + row.path());
                String after = resource(ROOT + "out/" + row.path());
                assertEquals(BEFORE.get(row.path()), M3GpuToolJoinManifest.sha256(before));
                assertEquals(replace ? WAVE_AFTER : BEFORE.get(row.path()),
                        M3GpuToolJoinManifest.sha256(after));
                if (replace) {
                    changed++;
                    assertNotNull(row.text(), row.path());
                    assertEquals(inverse ? before : after, row.text());
                } else {
                    assertNull(row.text(), "KEEP has no replacement payload");
                    assertEquals(before, after, row.path());
                }
            }
            assertEquals(1, changed);
            assertEquals(BEFORE.keySet(), seen);
        }
        assertEquals(resource(ROOT + "before/" + WAVE_OWNER),
                resource(ROOT + "inverse/out/" + WAVE_OWNER));
    }
    private static Map<String, String> state(boolean after) throws IOException {
        var files = new LinkedHashMap<String, String>();
        for (var row : manifest(false).targets()) {
            files.put(row.path(), resource(ROOT + (after ? "out/" : "before/") + row.path()));
        }
        files.put("opaque/Unrelated.txt", "Backport workflow-looking text remains opaque\n");
        return files;
    }
    private static Recipe named(boolean inverse) throws IOException {
        String descriptor = DESCRIPTOR + (inverse ? "-inverse" : "") + ".yml";
        assertEquals(inverse ? INVERSE_DESCRIPTOR_SHA : FORWARD_DESCRIPTOR_SHA,
                M3GpuToolJoinManifest.sha256(resource(descriptor)));
        try (var stream = M3BackportWaveCurrentOutputRecipeTest.class.getResourceAsStream(descriptor)) {
            assertNotNull(stream, descriptor);
            var environment = Environment.builder().load(new YamlResourceLoader(stream,
                    URI.create("classpath:" + descriptor), new Properties(),
                    M3BackportWaveCurrentOutputRecipe.class.getClassLoader())).build();
            String name = NAME + (inverse ? ".Inverse" : "");
            assertThrows(RecipeException.class, () -> environment.activateRecipes(name + ".Unknown"));
            Recipe activated = environment.activateRecipes(name);
            assertEquals(1, activated.getRecipeList().size());
            Recipe declaration = activated.getRecipeList().get(0);
            assertEquals(name, declaration.getName());
            assertEquals(DeclarativeRecipe.class, declaration.getClass());
            Class<?> expected = inverse ? M3BackportWaveCurrentOutputInverseRecipe.class
                    : M3BackportWaveCurrentOutputRecipe.class;
            assertEquals(List.of(expected), declaration.getRecipeList().stream().map(Object::getClass).toList());
            return activated;
        }
    }
    private static List<SourceFile> sources(Map<String, String> input, boolean reverse) {
        var entries = new ArrayList<>(input.entrySet());
        if (reverse) Collections.reverse(entries);
        var result = new ArrayList<SourceFile>();
        for (var entry : entries) {
            result.add(PlainText.builder().sourcePath(Path.of(entry.getKey())).text(entry.getValue()).build());
        }
        return result;
    }
    private static Applied apply(Map<String, String> input, boolean reverse, boolean inverse) throws IOException {
        var original = new LinkedHashMap<>(input);
        var files = sources(input, reverse);
        var snapshot = files.stream().map(file -> file.getId() + "\n"
                + file.getSourcePath() + "\n" + file.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        var run = named(inverse).run(new InMemoryLargeSourceSet(files),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var results = run.getChangeset().getAllResults();
        var actual = new LinkedHashMap<>(input);
        for (var result : results) {
            SourceFile before = result.getBefore(), after = result.getAfter();
            assertNotNull(before); assertNotNull(after);
            assertEquals(WAVE_OWNER, before.getSourcePath().toString().replace('\\', '/'));
            assertEquals(before.getSourcePath(), after.getSourcePath());
            assertEquals(before.getId(), after.getId());
            assertEquals(resource(ROOT + (inverse ? "before/" : "out/") + WAVE_OWNER), after.printAll());
            actual.put(WAVE_OWNER, after.printAll());
            String previous = EMITTED.putIfAbsent(inverse, after.printAll());
            if (previous != null) assertEquals(previous, after.printAll());
        }
        assertEquals(input.get(WAVE_OWNER).equals(resource(ROOT + (inverse ? "before/" : "out/") + WAVE_OWNER))
                ? 0 : 1, results.size());
        assertEquals(state(!inverse), actual);
        assertEquals(original, input, "proposal mutated input map");
        assertEquals(snapshot, files.stream().map(file -> file.getId() + "\n"
                + file.getSourcePath() + "\n" + file.printAll()).toList());
        successfulRuns++;
        return new Applied(actual, results.size());
    }
    private static void reject(List<SourceFile> input, String signature, boolean inverse) throws IOException {
        var before = input.stream().map(file -> file.getId() + "\n"
                + file.getSourcePath() + "\n" + file.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        try {
            var run = named(inverse).run(new InMemoryLargeSourceSet(input),
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
        assertEquals(before, input.stream().map(file -> file.getId() + "\n"
                + file.getSourcePath() + "\n" + file.printAll()).toList());
        refusedRuns++;
    }

    @Test void bothPartialStatesRoundTripAndReachFixedPointsInBothOrders() throws Exception {
        validateComposition();
        for (boolean after : new boolean[]{false, true}) {
            for (boolean reverse : new boolean[]{false, true}) {
                var forward = apply(state(after), reverse, false);
                var fixed = apply(forward.files(), !reverse, false);
                assertEquals(0, fixed.changes());
                var inverse = apply(fixed.files(), reverse, true);
                var inverseFixed = apply(inverse.files(), !reverse, true);
                assertEquals(0, inverseFixed.changes());
                assertEquals(state(false), inverseFixed.files());
            }
        }
        PASSED.add("bothPartialStatesRoundTripAndReachFixedPointsInBothOrders");
    }

    @Test void everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation() throws Exception {
        for (boolean inverse : new boolean[]{false, true}) {
            for (String path : BEFORE.keySet()) {
                var drift = state(inverse);
                drift.put(path, drift.get(path) + "\n/* unknown owner drift */\n");
                reject(sources(drift, false), "source drift: " + path, inverse);
                var missing = state(inverse);
                missing.remove(path);
                reject(sources(missing, true), "required source missing: " + path, inverse);
                var duplicate = sources(state(inverse), false);
                duplicate.add(PlainText.builder().sourcePath(Path.of(path))
                        .text(state(inverse).get(path)).build());
                reject(duplicate, "duplicate source target: " + path, inverse);
            }
        }
        PASSED.add("everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation");
    }

    private static Map<String, String> workflowHashes(String source) {
        var matcher = java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([0-9a-f]{64})\"").matcher(source);
        var actual = new LinkedHashMap<String, String>();
        while (matcher.find()) {
            assertNull(actual.put(matcher.group(1), matcher.group(2)), "duplicate workflow pin");
        }
        return actual;
    }

    @Test void exactCurrentWorkflowPinsPreserveHistoricalReceipt() throws Exception {
        validateComposition();
        Map<String, String> expected = Map.of(
                "m3/backports/backport_execution_waves.py", BEFORE.get("m3/backports/backport_execution_waves.py"),
                "m3/backports/test_backport_execution_waves.py", BEFORE.get("m3/backports/test_backport_execution_waves.py"),
                "m3/backports/BACKPORT_EXECUTION_WAVES.md", BEFORE.get("m3/backports/BACKPORT_EXECUTION_WAVES.md"));
        assertEquals(expected, workflowHashes(resource(ROOT + "out/" + WAVE_OWNER)));
        assertNotEquals(expected, workflowHashes(resource(ROOT + "before/" + WAVE_OWNER)));
        String receipt = "m3/compatibility/synexia-wave-admission-pin-20261014.tsv";
        assertEquals(resource(ROOT + "before/" + receipt), resource(ROOT + "out/" + receipt));
        assertTrue(resource(ROOT + "out/" + receipt).contains(
                "canonical_recipe_commit\t0537c17d77d2613362ec0e3b8cad4830f179a74f\n"));
        assertTrue(resource(ROOT + "out/" + receipt).contains(
                "path_2_after_sha256\tacd87a7a6d6c8427b700ddb03859dfaaa3ff4e3ef83fa1555d18022126e97323\n"));
        PASSED.add("exactCurrentWorkflowPinsPreserveHistoricalReceipt");
    }

    @AfterEach void retainAuthenticOutputsOnlyAfterAllNamedMethodsPass() throws IOException {
        if (PASSED.size() < 3) return;
        assertEquals(Set.of("bothPartialStatesRoundTripAndReachFixedPointsInBothOrders",
                "everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation",
                "exactCurrentWorkflowPinsPreserveHistoricalReceipt"), PASSED);
        assertEquals(16, successfulRuns);
        assertEquals(60, refusedRuns);
        String id = System.getenv("M3_QUALIFICATION_ID"), head = System.getenv("EXPECTED_SOURCE_SHA");
        assertNotNull(id); assertNotNull(head);
        assertTrue(id.matches("[A-Za-z0-9_.-]{16,128}"));
        assertTrue(head.matches("[0-9a-f]{40}"));
        Path target = Path.of("target").toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS));
        for (Path cursor = target; cursor != null; cursor = cursor.getParent()) {
            assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
        }
        assertEquals(Set.of(false, true), EMITTED.keySet());
        for (boolean inverse : new boolean[]{false, true}) {
            Path output = target.resolve(inverse ? "inverse-emitted" : "emitted").resolve(WAVE_OWNER);
            Files.createDirectories(output.getParent());
            for (Path cursor = output.getParent(); cursor != null; cursor = cursor.getParent()) {
                assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
            }
            // Retain the actual SourceFile postimage observed in a successful named recipe run.
            String text = EMITTED.get(inverse);
            assertNotNull(text);
            assertEquals(resource(ROOT + (inverse ? "before/" : "out/") + WAVE_OWNER), text);
            Files.writeString(output, text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        String json = """
                {
                  "schema": 1,
                  "qualificationId": "%s",
                  "checkoutCommit": "%s",
                  "tests": 3,
                  "successfulRuns": 16,
                  "refusedRuns": 60,
                  "emittedOutputs": 2,
                  "sourceOnly": true,
                  "runtimeApplied": false,
                  "phaseAdvanced": false
                }
                """.formatted(id, head);
        Files.writeString(target.resolve("recipe-test-counters.json"), json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.println("M3_BACKPORT_WAVE_CURRENT_OUTPUT_RECIPE_COUNTERS successful=16"
                + " refused=60 outputs=2 qualificationId=" + id);
    }
}
