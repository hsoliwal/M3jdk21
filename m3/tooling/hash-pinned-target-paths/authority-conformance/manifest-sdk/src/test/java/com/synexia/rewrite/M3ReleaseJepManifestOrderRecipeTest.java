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

/** Named reversible recipe plus unchanged strict-provider fixture replay; original receiving Maven model remains separate. */
@Execution(ExecutionMode.SAME_THREAD)
final class M3ReleaseJepManifestOrderRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/release-jep-manifest-order-20261020/";
    private static final String DESCRIPTOR = "/META-INF/rewrite/release-jep-manifest-order-20261020";
    private static final String NAME = "com.synexia.m3.ReleaseJepManifestOrder";
    private static final String FORWARD_MANIFEST_SHA = "3ff1405843a6647c26b036dcfcc5b865820347cd089055821945413a1f74b322";
    private static final String INVERSE_MANIFEST_SHA = "b14e152448b19ecb25ad3563f5d467de2dd10644d0d25f7c5a32652268ba242e";
    private static final String FORWARD_DESCRIPTOR_SHA = "35bacbafd8feda55871925e8753a8fb5636386dcf3bdbb908e4f66bdaa3ac708";
    private static final String INVERSE_DESCRIPTOR_SHA = "3a67c2d9ecf76adcefc6f5d45d57a390cdc8b9567e32cf504ba5f4cebc3a710f";
    private static final String TARGET = "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/manifest.tsv";
    private static final String AFTER = "ce113825c8e34a7379f65da06d9e203a84666d85051acc2fe1fec672d8374aa9";
    private static final Map<String, String> BEFORE = Map.of(TARGET, "cc775d76fdcf53442c69ccd83558d498a53a6f0e78893bee851660612370f789");
    private static final Set<String> PASSED = new LinkedHashSet<>();
    private static final Map<Boolean, String> EMITTED = new HashMap<>();
    private static int successfulRuns;
    private static int refusedRuns;
    private static int authenticProviderRuns;
    private static int authenticProviderRefusals;
    private static int authenticProviderOutputs;
    private record Applied(Map<String, String> files, int changes) { }

    private static String resource(String path) throws IOException {
        try (var stream = M3ReleaseJepManifestOrderRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest(boolean inverse) {
        return M3GpuToolJoinManifest.load(ROOT + (inverse ? "inverse/" : ""),
                M3ReleaseJepManifestOrderRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static void validateComposition() throws IOException {
        for (boolean inverse : new boolean[]{false, true}) {
            String manifestPath = ROOT + (inverse ? "inverse/" : "") + "manifest.tsv";
            assertEquals(inverse ? INVERSE_MANIFEST_SHA : FORWARD_MANIFEST_SHA,
                    M3GpuToolJoinManifest.sha256(resource(manifestPath)));
            var rows = manifest(inverse).targets();
            assertEquals(1, rows.size());
            var seen = new HashSet<String>();
            int changed = 0;
            for (var row : rows) {
                assertTrue(seen.add(row.path()), "duplicate manifest owner");
                boolean replace = row.path().equals(TARGET);
                String oldHash = inverse && replace ? AFTER : BEFORE.get(row.path());
                String newHash = !inverse && replace ? AFTER : BEFORE.get(row.path());
                assertEquals(oldHash, row.before(), row.path());
                assertEquals(newHash, row.after(), row.path());
                assertFalse(row.addition() || row.removal(), row.path());
                String before = resource(ROOT + "before/" + row.path());
                String after = resource(ROOT + "out/" + row.path());
                assertEquals(BEFORE.get(row.path()), M3GpuToolJoinManifest.sha256(before));
                assertEquals(replace ? AFTER : BEFORE.get(row.path()),
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
        assertEquals(resource(ROOT + "before/" + TARGET),
                resource(ROOT + "inverse/out/" + TARGET));
    }
    private static Map<String, String> state(boolean after) throws IOException {
        var files = new LinkedHashMap<String, String>();
        for (var row : manifest(false).targets()) {
            files.put(row.path(), resource(ROOT + (after ? "out/" : "before/") + row.path()));
        }
        files.put("opaque/Unrelated.txt", "Manifest-looking text remains opaque\n");
        return files;
    }
    private static Recipe named(boolean inverse) throws IOException {
        String descriptor = DESCRIPTOR + (inverse ? "-inverse" : "") + ".yml";
        assertEquals(inverse ? INVERSE_DESCRIPTOR_SHA : FORWARD_DESCRIPTOR_SHA,
                M3GpuToolJoinManifest.sha256(resource(descriptor)));
        try (var stream = M3ReleaseJepManifestOrderRecipeTest.class.getResourceAsStream(descriptor)) {
            assertNotNull(stream, descriptor);
            var environment = Environment.builder().load(new YamlResourceLoader(stream,
                    URI.create("classpath:" + descriptor), new Properties(),
                    M3ReleaseJepManifestOrderRecipe.class.getClassLoader())).build();
            String name = NAME + (inverse ? ".Inverse" : "");
            assertThrows(RecipeException.class, () -> environment.activateRecipes(name + ".Unknown"));
            Recipe activated = environment.activateRecipes(name);
            assertEquals(1, activated.getRecipeList().size());
            Recipe declaration = activated.getRecipeList().get(0);
            assertEquals(name, declaration.getName());
            assertEquals(DeclarativeRecipe.class, declaration.getClass());
            Class<?> expected = inverse ? M3ReleaseJepManifestOrderInverseRecipe.class
                    : M3ReleaseJepManifestOrderRecipe.class;
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
            assertEquals(TARGET, before.getSourcePath().toString().replace('\\', '/'));
            assertEquals(before.getSourcePath(), after.getSourcePath());
            assertEquals(before.getId(), after.getId());
            assertEquals(resource(ROOT + (inverse ? "before/" : "out/") + TARGET), after.printAll());
            actual.put(TARGET, after.printAll());
            String previous = EMITTED.putIfAbsent(inverse, after.printAll());
            if (previous != null) assertEquals(previous, after.printAll());
        }
        assertEquals(input.get(TARGET).equals(resource(ROOT + (inverse ? "before/" : "out/") + TARGET))
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

    private static int firstInversion(String text) {
        var lines = text.lines().filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index - 1).split("\t", -1)[0]
                    .compareTo(lines.get(index).split("\t", -1)[0]) >= 0) return index + 1;
        }
        return -1;
    }

    @Test void authenticStrictProviderReplaysElevenTargetsAndRejectsBothOrderFailures() throws Exception {
        String authentic = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/";
        String crate = "release-jep-authority-repair-20261006";
        String repaired = resource(authentic + crate + "/manifest.tsv");
        assertEquals(resource(ROOT + "out/" + TARGET), repaired);
        assertEquals(-1, firstInversion(repaired));
        var strict = new com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe(crate);
        var lines = repaired.lines().map(line -> line.split("\t", -1)).toList();
        assertEquals(11, lines.size());
        assertEquals(lines.stream().map(cells -> cells[0]).toList(), strict.targetPaths());
        var before = new ArrayList<SourceFile>();
        for (var cells : lines) {
            assertEquals(4, cells.length);
            assertEquals(cells[2], M3GpuToolJoinManifest.sha256(resource(authentic + crate + "/" + cells[3])));
            if (!"ABSENT".equals(cells[1])) {
                String text = resource(authentic + crate + "/before-" + cells[3]);
                assertEquals(cells[1], M3GpuToolJoinManifest.sha256(text));
                before.add(PlainText.builder().sourcePath(Path.of(cells[0])).text(text).build());
            }
        }
        assertEquals(8, before.size());
        var snapshot = before.stream().map(file -> file.getId() + "\n" + file.getSourcePath() + "\n" + file.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        var first = strict.run(new InMemoryLargeSourceSet(before), new InMemoryExecutionContext(errors::add), 1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(11, first.size());
        authenticProviderRuns++;
        authenticProviderOutputs += first.size();
        var after = new TreeMap<String, SourceFile>();
        before.forEach(file -> after.put(file.getSourcePath().toString().replace('\\', '/'), file));
        for (var change : first) {
            assertNotNull(change.getAfter());
            var file = change.getAfter();
            after.put(file.getSourcePath().toString().replace('\\', '/'), file);
        }
        assertEquals(11, after.size());
        for (var cells : lines) assertEquals(resource(authentic + crate + "/" + cells[3]), after.get(cells[0]).printAll());
        assertEquals(snapshot, before.stream().map(file -> file.getId() + "\n" + file.getSourcePath() + "\n" + file.printAll()).toList());
        var fixed = new com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe(crate)
                .run(new InMemoryLargeSourceSet(new ArrayList<>(after.values())), new InMemoryExecutionContext(errors::add), 1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(fixed.isEmpty());
        authenticProviderRuns++;
        for (var witness : Map.of("release-jep-authority-original-order", 6,
                                  "release-jep-authority-row8-order", 8).entrySet()) {
            String broken = resource(authentic + witness.getKey() + "/manifest.tsv");
            assertEquals(witness.getValue().intValue(), firstInversion(broken));
            var failure = assertThrows(IllegalStateException.class,
                    () -> new com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe(witness.getKey()).targetPaths());
            assertEquals("invalid M3JDK21 text crate manifest row", failure.getMessage());
            authenticProviderRefusals++;
        }
        PASSED.add("authenticStrictProviderReplaysElevenTargetsAndRejectsBothOrderFailures");
    }

    @AfterEach void retainAuthenticOutputsOnlyAfterAllNamedMethodsPass() throws IOException {
        if (PASSED.size() < 3) return;
        assertEquals(Set.of("bothPartialStatesRoundTripAndReachFixedPointsInBothOrders",
                "everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation",
                "authenticStrictProviderReplaysElevenTargetsAndRejectsBothOrderFailures"), PASSED);
        assertEquals(16, successfulRuns);
        assertEquals(6, refusedRuns);
        assertEquals(2, authenticProviderRuns);
        assertEquals(2, authenticProviderRefusals);
        assertEquals(11, authenticProviderOutputs);
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
            Path output = target.resolve(inverse ? "inverse-emitted" : "emitted").resolve(TARGET);
            Files.createDirectories(output.getParent());
            for (Path cursor = output.getParent(); cursor != null; cursor = cursor.getParent()) {
                assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
            }
            // Retain the actual SourceFile postimage observed in a successful named recipe run.
            String text = EMITTED.get(inverse);
            assertNotNull(text);
            assertEquals(resource(ROOT + (inverse ? "before/" : "out/") + TARGET), text);
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
                  "refusedRuns": 6,
                  "emittedOutputs": 2,
                  "authenticProviderRuns": 2,
                  "authenticProviderRefusals": 2,
                  "authenticProviderOutputs": 11,
                  "sourceOnly": true,
                  "runtimeApplied": false,
                  "phaseAdvanced": false
                }
                """.formatted(id, head);
        Files.writeString(target.resolve("recipe-test-counters.json"), json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.println("M3_AUTHORITY_MANIFEST_ORDER_RECIPE_COUNTERS successful=16"
                + " refused=6 outputs=2 authenticRuns=2 authenticRefusals=2 authenticOutputs=11 qualificationId=" + id);
    }
}
