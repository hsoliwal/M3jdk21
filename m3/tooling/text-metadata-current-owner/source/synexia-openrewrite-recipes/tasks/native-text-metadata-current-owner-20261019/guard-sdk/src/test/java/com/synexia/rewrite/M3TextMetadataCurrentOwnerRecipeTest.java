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
import org.openrewrite.*;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Real SDK scheduling; source custody grants no native execution authority. */
final class M3TextMetadataCurrentOwnerRecipeTest {
    private static final String CURRENT = "/com/synexia/rewrite/text-metadata-current-owner-20261019/";
    private static final String OLD = "/com/synexia/rewrite/composite-summary/";
    private static final String DESCRIPTOR = "/META-INF/rewrite/text-metadata-current-owner.yml";
    private static final String NAME = "com.synexia.rewrite.TextMetadataCurrentOwner";
    private static final String CURRENT_SHA = "05ce3668b4347c364a4b00d3dbef2da221ad0291911cac3620277e855eb1c708";
    private static final String OLD_SHA = "25764b426bd401b2196745a0ef5f2a13008501ae742f3111def7169f6cb73bda";
    private static final String DESCRIPTOR_SHA = "57673d14355f9afb43340cd379dc7a2a0115ea56849df069578ad203add654cc";
    private static final String CMAKE = "synexia-mindex/native-jni/CMakeLists.txt";
    private static final String COMPOSITE =
            "synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexCompositeString.java";
    private static final Map<String, String> EXPECTED = Map.ofEntries(
            Map.entry(CMAKE, "f503b4482078d539cf55cec128b584beaea44f7fbd3683262bf607fe5143d547"),
            Map.entry("synexia-mindex/native-jni/src/mindex_text_metadata_jni.c",
                    "08b8871d0e57d6f64caaec775f4f99157ea302ce3b59ac707d222f383988edf8"),
            Map.entry(COMPOSITE, "8037a689789fc351ccdeef8105122a961a6722bb27e8f54cfe42cfd2370365e3"),
            Map.entry("synexia-mindex/runtime/src/main/java/com/synexia/mindex/TextMetadata.java",
                    "6407591f0da1d3e0d79007ad39b55922892e4127686aad29f3e35eb187b44e84"),
            Map.entry("synexia-mindex/runtime/src/test/java/com/synexia/mindex/MIndexCompositeStructuralOpsTest.java",
                    "08a7f0b09bb04578a08b906230dc4099583a4dadfa385089a98abe0f331b40e2"),
            Map.entry("synexia-mindex/runtime/src/test/java/com/synexia/mindex/TextMetadataSummaryTest.java",
                    "7f0f4d26f641b4778ad4215034259bf468f1be30bd10990cace8cf624a3906ce"));
    private static final Set<String> PASSED = new LinkedHashSet<>();
    private static int successfulRuns;
    private static int refusedRuns;
    private record Applied(Map<String, String> files, int changes) { }

    private static String resource(String name) throws IOException {
        try (var stream = M3TextMetadataCurrentOwnerRecipeTest.class.getResourceAsStream(name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static M3GpuToolJoinManifest manifest(String root) {
        return M3GpuToolJoinManifest.load(root,
                M3TextMetadataCurrentOwnerRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }

    private static Recipe named() throws IOException {
        assertEquals(DESCRIPTOR_SHA, M3GpuToolJoinManifest.sha256(resource(DESCRIPTOR)));
        try (var stream = M3TextMetadataCurrentOwnerRecipeTest.class.getResourceAsStream(DESCRIPTOR)) {
            assertNotNull(stream);
            var loader = new YamlResourceLoader(stream, URI.create("classpath:" + DESCRIPTOR),
                    new Properties(), M3TextMetadataCurrentOwnerRecipe.class.getClassLoader());
            var environment = Environment.builder().load(loader).build();
            assertThrows(RecipeException.class, () -> environment.activateRecipes(NAME + ".Unknown"));
            Recipe activated = environment.activateRecipes(NAME);
            assertEquals(1, activated.getRecipeList().size());
            Recipe declared = activated.getRecipeList().get(0);
            assertEquals(NAME, declared.getName());
            assertEquals(DeclarativeRecipe.class, declared.getClass());
            assertEquals(List.of(M3TextMetadataCurrentOwnerRecipe.class),
                    declared.getRecipeList().stream().map(Object::getClass).toList());
            return activated;
        }
    }

    private static Map<String, String> current() throws IOException {
        assertEquals(CURRENT_SHA, M3GpuToolJoinManifest.sha256(resource(CURRENT + "manifest.tsv")));
        var rows = manifest(CURRENT).targets();
        assertEquals(6, rows.size());
        var input = new LinkedHashMap<String, String>();
        for (var target : rows) {
            assertEquals(EXPECTED.get(target.path()), target.before(), target.path());
            assertEquals(target.before(), target.after(), target.path());
            assertTrue(!target.addition() && !target.removal(), target.path());
            String contents = resource(CURRENT + "current/"
                    + Path.of(target.path()).getFileName() + ".txt");
            assertEquals(target.after(), M3GpuToolJoinManifest.sha256(contents));
            input.put(target.path(), contents);
        }
        assertEquals(EXPECTED.keySet(), input.keySet());
        input.put("unrelated/opaque.txt", "MIndex text markers are opaque here\n");
        return input;
    }

    private static Map<String, String> historical(boolean after) throws IOException {
        assertEquals(OLD_SHA, M3GpuToolJoinManifest.sha256(resource(OLD + "manifest.tsv")));
        var input = new LinkedHashMap<String, String>();
        for (var target : manifest(OLD).targets()) {
            if (after) {
                assertEquals(target.after(), M3GpuToolJoinManifest.sha256(target.text()));
                input.put(target.path(), target.text());
            } else if (!target.addition()) {
                String contents = resource(CURRENT + "historical-before/"
                        + Path.of(target.path()).getFileName() + ".txt");
                assertEquals(target.before(), M3GpuToolJoinManifest.sha256(contents));
                input.put(target.path(), contents);
            }
        }
        input.put("unrelated/opaque.txt", "MIndex text markers are opaque here\n");
        return input;
    }

    private static List<SourceFile> sources(Map<String, String> input, boolean reverse) {
        var entries = new ArrayList<>(input.entrySet());
        if (reverse) Collections.reverse(entries);
        var result = new ArrayList<SourceFile>();
        for (var entry : entries) result.add(PlainText.builder()
                .sourcePath(Path.of(entry.getKey())).text(entry.getValue()).build());
        return result;
    }

    private static Applied apply(Recipe recipe, Map<String, String> input, boolean reverse) {
        var errors = new ArrayList<Throwable>();
        var run = recipe.run(new InMemoryLargeSourceSet(sources(input, reverse)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var output = new LinkedHashMap<>(input);
        var results = run.getChangeset().getAllResults();
        for (var result : results) {
            assertNotNull(result.getAfter());
            output.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                    result.getAfter().printAll());
        }
        successfulRuns++;
        return new Applied(output, results.size());
    }

    private static void reject(Recipe recipe, List<SourceFile> input, String signature) {
        var before = input.stream().map(f -> f.getSourcePath() + "\n" + f.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        try {
            var run = recipe.run(new InMemoryLargeSourceSet(input), new InMemoryExecutionContext(errors::add));
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

    @Test void namedCurrentTupleReplaysAsExactFixedPointInBothOrders() throws Exception {
        for (boolean reverse : new boolean[]{false, true}) {
            var input = current();
            var first = apply(named(), input, reverse);
            assertEquals(0, first.changes());
            assertEquals(input, first.files());
            var second = apply(named(), first.files(), !reverse);
            assertEquals(0, second.changes());
            assertEquals(first.files(), second.files());
        }
        PASSED.add("namedCurrentTupleReplaysAsExactFixedPointInBothOrders");
    }

    @Test void historicalRecipeAndArchivesRetainTheirOriginalAdmissionBoundary() throws Exception {
        for (boolean reverse : new boolean[]{false, true}) {
            var oldAfter = historical(true);
            var emitted = apply(new M3CompositeSummaryRecipe(), historical(false), reverse);
            assertEquals(6, emitted.changes());
            assertEquals(oldAfter, emitted.files());
            assertEquals(0, apply(new M3CompositeSummaryRecipe(), emitted.files(), !reverse).changes());
            reject(new M3CompositeSummaryRecipe(), sources(current(), reverse), "source drift: ");
            reject(named(), sources(historical(false), reverse), "source drift: ");
            reject(named(), sources(oldAfter, reverse), "source drift: ");
        }
        assertEquals(13912, historical(true).get(CMAKE).getBytes(StandardCharsets.UTF_8).length);
        assertEquals(14661, current().get(CMAKE).getBytes(StandardCharsets.UTF_8).length);
        PASSED.add("historicalRecipeAndArchivesRetainTheirOriginalAdmissionBoundary");
    }

    @Test void refusesUnknownMixedMissingAndDuplicateOwnersWithoutMutation() throws Exception {
        for (String path : EXPECTED.keySet()) {
            var drift = current();
            drift.put(path, drift.get(path) + "\n/* unknown source drift */\n");
            reject(named(), sources(drift, false), "source drift: " + path);
            var missing = current();
            missing.remove(path);
            reject(named(), sources(missing, true), "required source missing: " + path);
            var duplicate = sources(current(), false);
            duplicate.add(PlainText.builder().sourcePath(Path.of(path)).text(current().get(path)).build());
            reject(named(), duplicate, "duplicate source target: " + path);
        }
        var older = historical(true);
        var mixedCmake = current();
        mixedCmake.put(CMAKE, older.get(CMAKE));
        reject(named(), sources(mixedCmake, false), "source drift: " + CMAKE);
        var mixedComposite = current();
        mixedComposite.put(COMPOSITE, older.get(COMPOSITE));
        reject(named(), sources(mixedComposite, true), "source drift: " + COMPOSITE);
        PASSED.add("refusesUnknownMixedMissingAndDuplicateOwnersWithoutMutation");
    }

    @AfterAll static void retainFreshSdkReceiptOnlyAfterAllThreeMethodsPass() throws IOException {
        assertEquals(Set.of("namedCurrentTupleReplaysAsExactFixedPointInBothOrders",
                "historicalRecipeAndArchivesRetainTheirOriginalAdmissionBoundary",
                "refusesUnknownMixedMissingAndDuplicateOwnersWithoutMutation"), PASSED,
                "no receipt after missing or failed methods");
        assertEquals(8, successfulRuns);
        assertEquals(26, refusedRuns);
        String rootValue = System.getenv("M3_QUALIFICATION_ROOT");
        String id = System.getenv("M3_QUALIFICATION_ID");
        assertNotNull(rootValue, "explicit isolated qualification root required");
        assertNotNull(id, "fresh qualification ID required");
        assertTrue(id.matches("[A-Za-z0-9_.-]{16,128}"), "invalid qualification ID");
        Path root = Path.of(rootValue);
        assertTrue(root.isAbsolute() && root.equals(root.toRealPath()), "canonical absolute root required");
        Path artifacts = root.resolve("artifacts");
        assertTrue(Files.isDirectory(artifacts, LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.isSymbolicLink(artifacts));
        String value = "{\n  \"schema\": 1,\n  \"state\": \"PASS\",\n"
                + "  \"qualificationId\": \"" + id + "\",\n"
                + "  \"scope\": \"ACTUAL_NAMED_OPENREWRITE_SOURCE_CUSTODY_ONLY\",\n"
                + "  \"currentManifestSha256\": \"" + CURRENT_SHA + "\",\n"
                + "  \"historicalManifestSha256\": \"" + OLD_SHA + "\",\n"
                + "  \"descriptorSha256\": \"" + DESCRIPTOR_SHA + "\",\n"
                + "  \"successfulSdkRuns\": 8,\n  \"refusedSdkRuns\": 26,\n"
                + "  \"tests\": 3,\n  \"nativeExecuted\": false,\n"
                + "  \"runtimeApplied\": false,\n  \"phaseAdvanced\": false\n}\n";
        Files.writeString(artifacts.resolve("text-metadata-guard-sdk.json"), value,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }
}
