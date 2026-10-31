/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.rewrite;

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

/** Real named recipe replay only; compiling the actual receiving provider remains a separate gate. */
@Execution(ExecutionMode.SAME_THREAD)
final class M3HashPinnedTargetPathsCompatRecipeTest {
    private static final String ROOT = "/jdk/internal/m3/rewrite/hash-pinned-target-paths-20261020/";
    private static final String DESCRIPTOR = "/META-INF/rewrite/hash-pinned-target-paths-20261020";
    private static final String NAME = "jdk.internal.m3.HashPinnedTargetPathsCompat";
    private static final String FORWARD_MANIFEST_SHA = "a5519c55b50da387e76d0b0ce2241cc25f4216eff62734557f0ed72e27051403";
    private static final String INVERSE_MANIFEST_SHA = "1f67be7b8a07cda0e4746a516df0f637211ee7b99a2bb2ace48015539e84e499";
    private static final String FORWARD_DESCRIPTOR_SHA = "7b5e266f805a3a8da289fcc4358f346d5b475789672f30fd153f25d5eb116784";
    private static final String INVERSE_DESCRIPTOR_SHA = "2c3273d747559d4756cb49df36de5fddadb119a610b9fe4a8f383562a0e8ce2b";
    private static final String PROVIDER_OWNER = "m3/tooling/migration-recipes/src/main/java/jdk/internal/m3/rewrite/M3HashPinnedJavaSnapshotRecipe.java";
    private static final String PROVIDER_AFTER = "8c43e01532a256cfc2b8c55bf5ed7e3c9719be6a37b223b0cd93258c823289f4";
    private static final Map<String, String> BEFORE = Map.ofEntries(
            Map.entry("m3/tooling/migration-recipes/src/main/java/jdk/internal/m3/rewrite/M3HashPinnedJavaSnapshotRecipe.java", "9dc65793cb1f4fb20866f99f4e5d0043d54734aa03cf0fe297c0cadbba2d1c37"),
            Map.entry("m3/tooling/migration-recipes/src/test/java/jdk/internal/m3/rewrite/M3ObjectSlotIndexSnapshot20261017RecipeTest.java", "3223a170646c0ad111c4319894c231e1ed5690ea0a3ed1cc4d86c3ae0ba14dae"),
            Map.entry("m3/tooling/migration-recipes/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-object-slot-index-snapshot-20261017/M3ObjectIndexSnapshot.java.txt", "67116969a93d0da9beffafdb36a8310247a206a96d7cb58ffbce3c053e1ad4f1"),
            Map.entry("m3/tooling/migration-recipes/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-object-slot-index-snapshot-20261017/M3ObjectSlotIndexSelfTest.java.txt", "976975d979ffbde17bdf877349d60950da984b5f7207dc9f28ffeff6fe060ff7"),
            Map.entry("m3/tooling/migration-recipes/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-object-slot-index-snapshot-20261017/M3ObjectSlotIndexTest.java.txt", "6f2b492c0be59b4c6d784b3d3be3623d5da69e7217047657c36635852b14d3b7"),
            Map.entry("m3/tooling/migration-recipes/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-object-slot-index-snapshot-20261017/M3ObjectSlotStore28.java.txt", "032f837fea2e97b166fc08e7f7601d66e5d31513897a03632a08ec17bcf407bb"),
            Map.entry("m3/tooling/migration-recipes/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-object-slot-index-snapshot-20261017/manifest.tsv", "ff8ff72e0f0d03eaeb5f2012a56d3eca6dce0572e2dbe3214c5d3ce2ade31a2c"));
    private static final Set<String> PASSED = new LinkedHashSet<>();
    private static final Map<Boolean, String> EMITTED = new HashMap<>();
    private static int successfulRuns;
    private static int refusedRuns;
    private record Applied(Map<String, String> files, int changes) { }

    private static String resource(String path) throws IOException {
        try (var stream = M3HashPinnedTargetPathsCompatRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest(boolean inverse) {
        return M3GpuToolJoinManifest.load(ROOT + (inverse ? "inverse/" : ""),
                M3HashPinnedTargetPathsCompatRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static void validateComposition() throws IOException {
        for (boolean inverse : new boolean[]{false, true}) {
            String manifestPath = ROOT + (inverse ? "inverse/" : "") + "manifest.tsv";
            assertEquals(inverse ? INVERSE_MANIFEST_SHA : FORWARD_MANIFEST_SHA,
                    M3GpuToolJoinManifest.sha256(resource(manifestPath)));
            var rows = manifest(inverse).targets();
            assertEquals(7, rows.size());
            var seen = new HashSet<String>();
            int changed = 0;
            for (var row : rows) {
                assertTrue(seen.add(row.path()), "duplicate manifest owner");
                boolean replace = row.path().equals(PROVIDER_OWNER);
                String oldHash = inverse && replace ? PROVIDER_AFTER : BEFORE.get(row.path());
                String newHash = !inverse && replace ? PROVIDER_AFTER : BEFORE.get(row.path());
                assertEquals(oldHash, row.before(), row.path());
                assertEquals(newHash, row.after(), row.path());
                assertFalse(row.addition() || row.removal(), row.path());
                String before = resource(ROOT + "before/" + row.path());
                String after = resource(ROOT + "out/" + row.path());
                assertEquals(BEFORE.get(row.path()), M3GpuToolJoinManifest.sha256(before));
                assertEquals(replace ? PROVIDER_AFTER : BEFORE.get(row.path()),
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
        assertEquals(resource(ROOT + "before/" + PROVIDER_OWNER),
                resource(ROOT + "inverse/out/" + PROVIDER_OWNER));
    }
    private static Map<String, String> state(boolean after) throws IOException {
        var files = new LinkedHashMap<String, String>();
        for (var row : manifest(false).targets()) {
            files.put(row.path(), resource(ROOT + (after ? "out/" : "before/") + row.path()));
        }
        files.put("opaque/Unrelated.txt", "Provider-looking text remains opaque\n");
        return files;
    }
    private static Recipe named(boolean inverse) throws IOException {
        String descriptor = DESCRIPTOR + (inverse ? "-inverse" : "") + ".yml";
        assertEquals(inverse ? INVERSE_DESCRIPTOR_SHA : FORWARD_DESCRIPTOR_SHA,
                M3GpuToolJoinManifest.sha256(resource(descriptor)));
        try (var stream = M3HashPinnedTargetPathsCompatRecipeTest.class.getResourceAsStream(descriptor)) {
            assertNotNull(stream, descriptor);
            var environment = Environment.builder().load(new YamlResourceLoader(stream,
                    URI.create("classpath:" + descriptor), new Properties(),
                    M3HashPinnedTargetPathsCompatRecipe.class.getClassLoader())).build();
            String name = NAME + (inverse ? ".Inverse" : "");
            assertThrows(RecipeException.class, () -> environment.activateRecipes(name + ".Unknown"));
            Recipe activated = environment.activateRecipes(name);
            assertEquals(1, activated.getRecipeList().size());
            Recipe declaration = activated.getRecipeList().get(0);
            assertEquals(name, declaration.getName());
            assertEquals(DeclarativeRecipe.class, declaration.getClass());
            Class<?> expected = inverse ? M3HashPinnedTargetPathsCompatInverseRecipe.class
                    : M3HashPinnedTargetPathsCompatRecipe.class;
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
            assertEquals(PROVIDER_OWNER, before.getSourcePath().toString().replace('\\', '/'));
            assertEquals(before.getSourcePath(), after.getSourcePath());
            assertEquals(before.getId(), after.getId());
            assertEquals(resource(ROOT + (inverse ? "before/" : "out/") + PROVIDER_OWNER), after.printAll());
            actual.put(PROVIDER_OWNER, after.printAll());
            String previous = EMITTED.putIfAbsent(inverse, after.printAll());
            if (previous != null) assertEquals(previous, after.printAll());
        }
        assertEquals(input.get(PROVIDER_OWNER).equals(resource(ROOT + (inverse ? "before/" : "out/") + PROVIDER_OWNER))
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

    @Test void canonicalAccessorPreservesAllReceivingModes() throws Exception {
        validateComposition();
        String original = resource(ROOT + "before/" + PROVIDER_OWNER);
        String emitted = resource(ROOT + "out/" + PROVIDER_OWNER);
        String leaf = resource(ROOT + "canonical-targetPaths.java.txt");
        String anchor = "    public String getCrateName() { return crateName; }\n\n";
        assertEquals(1, original.split(java.util.regex.Pattern.quote(anchor), -1).length - 1);
        assertFalse(original.contains("public List<String> targetPaths()"));
        assertEquals(original.replace(anchor, anchor + leaf + "\n"), emitted,
                "restore only the canonical accessor; preserve the complete receiving successor");
        assertTrue(emitted.contains("public boolean isLst() { return lst; }"));
        assertTrue(emitted.contains("if (!lst)"));
        String test = "m3/tooling/migration-recipes/src/test/java/jdk/internal/m3/rewrite/"
                + "M3ObjectSlotIndexSnapshot20261017RecipeTest.java";
        assertEquals(resource(ROOT + "before/" + test), resource(ROOT + "out/" + test));
        assertTrue(resource(ROOT + "out/" + test).contains("recipe.targetPaths()"));
        PASSED.add("canonicalAccessorPreservesAllReceivingModes");
    }

    @AfterEach void retainAuthenticOutputsOnlyAfterAllNamedMethodsPass() throws IOException {
        if (PASSED.size() < 3) return;
        assertEquals(Set.of("bothPartialStatesRoundTripAndReachFixedPointsInBothOrders",
                "everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation",
                "canonicalAccessorPreservesAllReceivingModes"), PASSED);
        assertEquals(16, successfulRuns);
        assertEquals(42, refusedRuns);
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
            Path output = target.resolve(inverse ? "inverse-emitted" : "emitted").resolve(PROVIDER_OWNER);
            Files.createDirectories(output.getParent());
            for (Path cursor = output.getParent(); cursor != null; cursor = cursor.getParent()) {
                assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
            }
            // Retain the actual SourceFile postimage observed in a successful named recipe run.
            String text = EMITTED.get(inverse);
            assertNotNull(text);
            assertEquals(resource(ROOT + (inverse ? "before/" : "out/") + PROVIDER_OWNER), text);
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
                  "refusedRuns": 42,
                  "emittedOutputs": 2,
                  "sourceOnly": true,
                  "runtimeApplied": false,
                  "phaseAdvanced": false
                }
                """.formatted(id, head);
        Files.writeString(target.resolve("recipe-test-counters.json"), json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.println("M3_HASH_PINNED_TARGET_PATHS_COMPAT_RECIPE_COUNTERS successful=16"
                + " refused=42 outputs=2 qualificationId=" + id);
    }
}
