// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-17: adapt the existing source-sealed Text SDK proof for one YAML bootstrap.
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Named Text2db execution over one exact workflow; never runs its shell commands. */
final class M3NamedPurposeSdkBootstrapRecipeTest {
    private static final String CRATE = "m3-named-purpose-sdk-bootstrap-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-text/" + CRATE + "/";
    private static final String NAME = "com.synexia.rewrite.M3NamedPurposeSdkBootstrap20261017";
    private static final String TARGET =
            ".github/workflows/m3-generic-purpose-map-receiver-20261017.yml";
    private static final String TEMPLATE = "m3-generic-purpose-map-receiver-20261017.yml.txt";
    private static final String BEFORE_SHA =
            "8c0c5c9077cddbee219c9d3eb65ccac482a1f726adb5323f275d7fe38dd6ff49";
    private static final String AFTER_SHA =
            "dd5d332d9f9c09a6500baa1b99ce7f76c5c7ca241103d82c42e13bec0bb4bf1c";
    private static final String OWNED_TRIGGER =
            "      - 'm3/collections/qualification/named-purpose-sdk-20261017/**'\n";
    private static final String APPENDED_STEP =
            "      - name: Qualify named purpose recipes and exact bootstrap\n";
    private static final String OPAQUE = "opaque/Unrelated.txt";
    private record Applied(Map<String, String> files, int changes) { }

    @Test
    void namedWorkflowReachesExactFixedPointAndPreservesExistingProof() throws Exception {
        var activated = namedRecipe();
        assertEquals(1, activated.getRecipeList().size());
        var named = activated.getRecipeList().getFirst();
        assertEquals(NAME, named.getName());
        assertEquals(1, named.getRecipeList().size());
        var provider = assertInstanceOf(M3HashPinnedTextSnapshotRecipe.class,
                named.getRecipeList().getFirst());
        assertEquals(CRATE, provider.getCrateName());
        assertEquals(List.of(TARGET), provider.targetPaths());
        assertEquals(Map.of(TARGET, BEFORE_SHA), provider.preimageManifest());

        String before = resource("before.yml.txt");
        String after = resource(TEMPLATE);
        assertEquals(BEFORE_SHA, digest(before, "SHA-256"));
        assertEquals(AFTER_SHA, digest(after, "SHA-256"));
        assertEquals("1a3ac1cbccf374af0b7079022dbb5c2368c3a189", gitBlob(before));
        assertEquals("be38d7e13ef100d7a1a0e61ea4facdc9d138cc95", gitBlob(after));
        assertEquals("# path\tbefore_sha256\tafter_sha256\ttemplate\n"
                + TARGET + "\t" + BEFORE_SHA + "\t" + AFTER_SHA + "\t" + TEMPLATE + "\n",
                resource("manifest.tsv"));
        assertPreservation(before, after);

        for (int state = 0; state < 2; state++) {
            Map<String, String> input = new TreeMap<>();
            input.put(TARGET, state == 0 ? before : after);
            input.put(OPAQUE, "unrelated workflow words stay opaque\n");
            for (boolean reverse : new boolean[]{false, true}) {
                var sources = sources(input);
                if (reverse) java.util.Collections.reverse(sources);
                var result = apply(sources);
                assertEquals(1 - state, result.changes());
                Map<String, String> expected = new TreeMap<>(input);
                expected.put(TARGET, after);
                assertEquals(expected, result.files());
                assertEquals(0, apply(sources(result.files())).changes());
            }
        }
        Path output = Path.of(System.getProperty("m3.fixture.root"))
                .resolve("target/bootstrap-emitted").toAbsolutePath().normalize();
        Path emitted = output.resolve(TARGET).normalize();
        assertTrue(emitted.startsWith(output));
        Files.createDirectories(emitted.getParent());
        Files.writeString(emitted, after, StandardCharsets.UTF_8);
    }

    @Test
    void workflowRejectsDriftMissingAndDuplicateTargets() {
        List<SourceFile> missing = List.of(plain(OPAQUE, "unrelated\n"));
        reject(missing, "required source missing: " + TARGET);
        reject(List.of(plain(TARGET, resource("before.yml.txt") + "\n# drift\n")),
                "source drift: " + TARGET);
        reject(List.of(plain(TARGET, resource("before.yml.txt")),
                       plain(TARGET, resource("before.yml.txt"))),
                "duplicate target: " + TARGET);
    }

    private static void assertPreservation(String before, String after) {
        assertEquals(1L, after.lines().filter(line ->
                line.equals(OWNED_TRIGGER.stripTrailing())).count());
        String withoutTrigger = after.replace(OWNED_TRIGGER, "");
        int start = withoutTrigger.indexOf(APPENDED_STEP);
        assertTrue(start > withoutTrigger.indexOf(
                "      - name: Retain exact purpose SDK receipt and generated repairs\n"));
        assertEquals(before, withoutTrigger.substring(0, start));
        String insertion = withoutTrigger.substring(start);
        assertEquals(2L, insertion.lines().filter(line -> line.startsWith("      - ")).count());
        assertTrue(insertion.contains("canonical-source.json"));
        assertTrue(insertion.contains("assert actual == metadata[\"moduleGitTree\"]"));
        assertTrue(insertion.contains("metadata[\"inputPinsSha256\"]"));
        assertTrue(insertion.contains("73b93b1c3f1f7d50414edf3bfe74d7760956d319"));
        assertTrue(insertion.contains("shutil.disk_usage(\".\").free - (512 << 20) >= (8 << 30)"));
        assertTrue(insertion.contains("          include-hidden-files: true\n"));
        assertTrue(insertion.contains("          if-no-files-found: error\n"));
    }

    private static Recipe namedRecipe() {
        return Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes(NAME);
    }

    private static Applied apply(List<SourceFile> sources) {
        var errors = new ArrayList<Throwable>();
        var run = namedRecipe().run(new InMemoryLargeSourceSet(sources),
                new InMemoryExecutionContext(errors::add), 2);
        assertTrue(errors.isEmpty(), errors.toString());
        Map<String, String> output = new TreeMap<>();
        for (SourceFile source : sources) {
            output.put(source.getSourcePath().toString().replace('\\', '/'), source.printAll());
        }
        var changes = run.getChangeset().getAllResults();
        for (var result : changes) {
            assertNotNull(result.getAfter());
            output.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                    result.getAfter().printAll());
        }
        return new Applied(output, changes.size());
    }

    private static void reject(List<SourceFile> sources, String signature) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = namedRecipe().run(new InMemoryLargeSourceSet(sources),
                    new InMemoryExecutionContext(errors::add), 2);
            assertTrue(run.getChangeset().getAllResults().isEmpty());
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
    }

    private static ArrayList<SourceFile> sources(Map<String, String> input) {
        var result = new ArrayList<SourceFile>();
        input.forEach((path, text) -> result.add(plain(path, text)));
        return result;
    }

    private static PlainText plain(String path, String text) {
        return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
    }

    private static String resource(String name) {
        try (var stream = M3NamedPurposeSdkBootstrapRecipeTest.class
                .getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String digest(String text, String algorithm) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm)
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String gitBlob(String text) throws Exception {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(("blob " + data.length + "\0").getBytes(StandardCharsets.US_ASCII));
        return HexFormat.of().formatHex(digest.digest(data));
    }
}
