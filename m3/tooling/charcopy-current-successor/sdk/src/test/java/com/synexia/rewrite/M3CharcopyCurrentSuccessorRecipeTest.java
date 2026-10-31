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

/** Actual named SDK scheduling over data; target Java/LST/runtime compilation remains separate. */
final class M3CharcopyCurrentSuccessorRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/charcopy-current-successor-20261019/";
    private static final String DESCRIPTOR = "/META-INF/rewrite/charcopy-current-successor-20261019.yml";
    private static final String NAME = "com.synexia.m3.CharcopyCurrentSuccessor20261019";
    private static final String MANIFEST_SHA = "db44f3abf5270084eaa529469f664333fcef6abfe4df73e78d7548f2a3287303";
    private static final String DESCRIPTOR_SHA = "c97c3ef219bffd8177e01cd2bb585724408d5e3612253152c3a29d4af12998bf";
    private static final String OWNER = "src/java.base/share/classes/java/lang/M3String.java";
    private static final String FACTS = "src/java.base/share/classes/java/lang/M3StringFacts.java";
    private static final String OVERFLOW = "test/jdk/java/lang/String/M3StringConcatLengthOverflowTest.java";
    private static final List<String> LEGACY = List.of(
            "src/java.base/share/classes/java/lang/MIndexString.java",
            "m3/runtime-integration/tests/MIndexStringInvariant.java");
    private static final Set<String> PASSED = new LinkedHashSet<>();
    private static int successfulRuns;
    private static int refusedRuns;
    private record Applied(Map<String, String> files, int changes) { }

    private static String resource(String name) throws IOException {
        try (var stream = M3CharcopyCurrentSuccessorRecipeTest.class.getResourceAsStream(name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3CharcopyCurrentSuccessorRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static Recipe named() throws IOException {
        assertEquals(DESCRIPTOR_SHA, M3GpuToolJoinManifest.sha256(resource(DESCRIPTOR)));
        try (var stream = M3CharcopyCurrentSuccessorRecipeTest.class.getResourceAsStream(DESCRIPTOR)) {
            assertNotNull(stream);
            var environment = Environment.builder().load(new YamlResourceLoader(stream,
                    URI.create("classpath:" + DESCRIPTOR), new Properties(),
                    M3CharcopyCurrentSuccessorRecipe.class.getClassLoader())).build();
            assertThrows(RecipeException.class, () -> environment.activateRecipes(NAME + ".Unknown"));
            Recipe recipe = environment.activateRecipes(NAME);
            assertEquals(1, recipe.getRecipeList().size());
            Recipe declaration = recipe.getRecipeList().get(0);
            assertEquals(NAME, declaration.getName());
            assertEquals(DeclarativeRecipe.class, declaration.getClass());
            assertEquals(List.of(M3CharcopyCurrentSuccessorRecipe.class),
                    declaration.getRecipeList().stream().map(Object::getClass).toList());
            return recipe;
        }
    }
    private static Path receiver() throws IOException {
        String value = System.getenv("M3_RECEIVER_ROOT");
        assertNotNull(value, "explicit exact receiver required");
        Path root = Path.of(value);
        assertTrue(root.isAbsolute() && root.equals(root.toRealPath()));
        assertFalse(Files.isSymbolicLink(root));
        return root;
    }
    private static Map<String, String> current() throws IOException {
        assertEquals(MANIFEST_SHA, M3GpuToolJoinManifest.sha256(resource(ROOT + "manifest.tsv")));
        var rows = manifest().targets();
        assertEquals(8, rows.size());
        var files = new LinkedHashMap<String, String>();
        Path root = receiver();
        for (String legacy : LEGACY) {
            assertFalse(Files.exists(root.resolve(legacy), LinkOption.NOFOLLOW_LINKS));
        }
        for (var row : rows) {
            assertEquals(row.before(), row.after(), row.path());
            assertFalse(row.addition() || row.removal());
            Path path = root.resolve(row.path());
            for (Path parent = path; parent.startsWith(root); parent = parent.getParent()) {
                assertFalse(Files.isSymbolicLink(parent), parent.toString());
                if (parent.equals(root)) break;
            }
            assertTrue(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS), row.path());
            String contents = Files.readString(path, StandardCharsets.UTF_8);
            assertEquals(row.after(), M3GpuToolJoinManifest.sha256(contents), row.path());
            files.put(row.path(), contents);
        }
        files.put("opaque/Unrelated.txt", "legacy-looking identifiers are opaque outside owned paths\n");
        return files;
    }
    private static List<SourceFile> sources(Map<String, String> input, boolean reverse) {
        var entries = new ArrayList<>(input.entrySet());
        if (reverse) Collections.reverse(entries);
        var result = new ArrayList<SourceFile>();
        for (var entry : entries) result.add(PlainText.builder()
                .sourcePath(Path.of(entry.getKey())).text(entry.getValue()).build());
        return result;
    }
    private static Applied apply(Map<String, String> input, boolean reverse) throws IOException {
        var errors = new ArrayList<Throwable>();
        var run = named().run(new InMemoryLargeSourceSet(sources(input, reverse)),
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
    private static void reject(List<SourceFile> input, String expected) throws IOException {
        var before = input.stream().map(f -> f.getSourcePath() + "\n" + f.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        try {
            var run = named().run(new InMemoryLargeSourceSet(input), new InMemoryExecutionContext(errors::add));
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
        assertTrue(observed.toString().contains(expected), observed.toString());
        assertEquals(before, input.stream().map(f -> f.getSourcePath() + "\n" + f.printAll()).toList());
        refusedRuns++;
    }
    private static String overflowGuard(String current) {
        int offset = current.indexOf("        if (prepared.utf8Length < 0) {");
        assertTrue(offset >= 0);
        String guard = current.substring(offset, offset + 143);
        assertEquals("01ff1152e9e3100536675add7ff80f0d551bb7de1ea18999bcae803ff6da5b74",
                M3GpuToolJoinManifest.sha256(guard));
        int second = current.indexOf(guard, offset + guard.length());
        assertTrue(second > offset && current.indexOf(guard, second + guard.length()) == -1);
        return guard;
    }

    @Test void namedCurrentEightKeepInputsReplayAsExactFixedPoint() throws Exception {
        for (boolean reverse : new boolean[]{false, true}) {
            var input = current();
            var first = apply(input, reverse);
            assertEquals(0, first.changes());
            assertEquals(input, first.files());
            var second = apply(first.files(), !reverse);
            assertEquals(0, second.changes());
            assertEquals(first.files(), second.files());
        }
        PASSED.add("namedCurrentEightKeepInputsReplayAsExactFixedPoint");
    }

    @Test void everyDriftMissingDuplicateLegacyAndMixedInputRefuses() throws Exception {
        for (var row : manifest().targets()) {
            var drift = current();
            drift.put(row.path(), drift.get(row.path()) + "\nunknown source drift\n");
            reject(sources(drift, false), "source drift: " + row.path());
            var missing = current();
            missing.remove(row.path());
            reject(sources(missing, true), "required source missing: " + row.path());
            var duplicate = sources(current(), false);
            duplicate.add(PlainText.builder().sourcePath(Path.of(row.path()))
                    .text(current().get(row.path())).build());
            reject(duplicate, "duplicate source target: " + row.path());
        }
        for (String legacy : LEGACY) {
            for (boolean reverse : new boolean[]{false, true}) {
                for (boolean backslash : new boolean[]{false, true}) {
                    var input = sources(current(), reverse);
                    String spelling = backslash ? legacy.replace('/', '\\') : legacy;
                    input.add(PlainText.builder().sourcePath(Path.of(spelling))
                            .text("an unexpected legacy source is forbidden regardless of its body\n").build());
                    reject(input, "legacy source forbidden: " + legacy);
                }
            }
        }
        var exact = current();
        String owner = exact.get(OWNER), guard = overflowGuard(owner);
        for (boolean last : new boolean[]{false, true}) {
            int offset = last ? owner.lastIndexOf(guard) : owner.indexOf(guard);
            var oneGuard = current();
            oneGuard.put(OWNER, owner.substring(0, offset) + owner.substring(offset + guard.length()));
            reject(sources(oneGuard, last), "source drift: " + OWNER);
        }
        for (var older : Map.of(OWNER, "M3String-predecessor.java.txt",
                FACTS, "M3StringFacts-predecessor.java.txt",
                OVERFLOW, "M3StringConcatLengthOverflowTest-predecessor.java.txt").entrySet()) {
            var mixed = current();
            mixed.put(older.getKey(), resource(ROOT + older.getValue()));
            reject(sources(mixed, false), "source drift: " + older.getKey());
        }
        PASSED.add("everyDriftMissingDuplicateLegacyAndMixedInputRefuses");
    }

    @Test void exactInverseAndFullHistoricalPreimagesRetainTheirIdentity() throws Exception {
        var files = current();
        String owner = files.get(OWNER), guard = overflowGuard(owner);
        String predecessor = resource(ROOT + "M3String-predecessor.java.txt");
        assertEquals(55467, owner.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(55181, predecessor.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(predecessor, owner.replace(guard, ""));
        assertEquals("cd1192e27c981da03aeae2385d3d54ad31d912f043cf8c7dfba18c7b9106b728",
                M3GpuToolJoinManifest.sha256(predecessor));
        assertEquals("21e373dbe450c7cd7b85ede587209f88b4546b1833a75c83370b213aa66d97d0",
                M3GpuToolJoinManifest.sha256(resource(ROOT + "MIndexString-before.java.txt")));
        assertEquals("c4623ce4627dffb0b70c9b7d8cc8e429c50300a4fa6ec90ea5c7d1f6bbc4aee1",
                M3GpuToolJoinManifest.sha256(resource(ROOT + "MIndexStringInvariant-before.java.txt")));
        assertEquals("2152c154d3cdece330667ee09675fd21d86f3b483a23a3156b8a3626c3f9f420",
                M3GpuToolJoinManifest.sha256(resource(ROOT + "M3StringFacts-predecessor.java.txt")));
        assertEquals("587f7f63fb419970ffdc889bca05335eb3f49ebcc97d3810906b3587ff9e7ab4",
                M3GpuToolJoinManifest.sha256(resource(ROOT + "M3StringConcatLengthOverflowTest-predecessor.java.txt")));
        PASSED.add("exactInverseAndFullHistoricalPreimagesRetainTheirIdentity");
    }

    @AfterAll static void retainFreshSdkReceiptAfterAllMethodsPass() throws IOException {
        assertEquals(Set.of("namedCurrentEightKeepInputsReplayAsExactFixedPoint",
                "everyDriftMissingDuplicateLegacyAndMixedInputRefuses",
                "exactInverseAndFullHistoricalPreimagesRetainTheirIdentity"), PASSED);
        assertEquals(4, successfulRuns);
        assertEquals(37, refusedRuns);
        String rootValue = System.getenv("M3_QUALIFICATION_ROOT"), id = System.getenv("M3_QUALIFICATION_ID");
        assertNotNull(rootValue); assertNotNull(id);
        assertTrue(id.matches("[A-Za-z0-9_.-]{16,128}"));
        Path root = Path.of(rootValue);
        assertTrue(root.isAbsolute() && root.equals(root.toRealPath()));
        Path artifacts = root.resolve("artifacts");
        assertTrue(Files.isDirectory(artifacts, LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.isSymbolicLink(artifacts));
        String value = "{\n  \"schema\": 1,\n  \"state\": \"PASS_SOURCE_ONLY\",\n"
                + "  \"qualificationId\": \"" + id + "\",\n"
                + "  \"manifestSha256\": \"" + MANIFEST_SHA + "\",\n"
                + "  \"descriptorSha256\": \"" + DESCRIPTOR_SHA + "\",\n"
                + "  \"tests\": 3,\n  \"successfulSdkRuns\": 4,\n  \"refusedSdkRuns\": 37,\n"
                + "  \"runtimeApplied\": false,\n  \"phaseAdvanced\": false\n}\n";
        Files.writeString(artifacts.resolve("charcopy-guard-sdk.json"), value, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }
}
