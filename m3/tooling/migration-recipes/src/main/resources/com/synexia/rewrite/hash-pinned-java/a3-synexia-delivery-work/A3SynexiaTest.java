// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.synexia.importer.SynexiaImportManifest;
import com.m3.synexia.importer.SynexiaImportPlan;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class A3SynexiaTest {
    @TempDir
    Path temp;

    @Test
    void deliveryPlanBecomesDeterministicReviewWork() throws Exception {
        Fixture fixture = fixture();
        List<A3Synexia.Row> rows =
                A3Synexia.load(fixture.root(), fixture.manifestPath());
        Map<String, A3Synexia.Row> byPath =
                rows.stream()
                        .collect(
                                Collectors.toMap(
                                        A3Synexia.Row::targetPath,
                                        Function.identity()));

        assertEquals(4, rows.size());
        assertEquals(List.of(0, 1, 2, 3), rows.stream().map(A3Synexia.Row::order).toList());
        assertTrue(
                rows.stream()
                        .map(A3Synexia.Row::targetPath)
                        .toList()
                        .equals(
                                rows.stream()
                                        .map(A3Synexia.Row::targetPath)
                                        .sorted()
                                        .toList()));

        A3Synexia.Row keep = byPath.get(fixture.keepTarget());
        assertEquals(SynexiaImportPlan.Action.KEEP, keep.action());
        assertEquals(
                A3Synexia.ReviewLane.RECIPE_EXECUTION_REVIEW,
                keep.reviewLane());
        assertEquals("REUSE_VERIFIED_VENDOR", keep.nextAction());
        assertFalse(keep.candidateRequired());
        assertFalse(keep.promotionAuthority());

        A3Synexia.Row replace = byPath.get(fixture.replaceTarget());
        assertEquals(SynexiaImportPlan.Action.REPLACE, replace.action());
        assertEquals(
                A3Synexia.ReviewLane.JAVA_ATOM_PATTERN_REVIEW,
                replace.reviewLane());
        assertEquals("STAGE_THEN_REVIEW", replace.nextAction());
        assertTrue(replace.candidateRequired());

        A3Synexia.Row add = byPath.get(fixture.addTarget());
        assertEquals(SynexiaImportPlan.Action.ADD, add.action());
        assertEquals(
                A3Synexia.ReviewLane.JNI_NATIVE_PARITY_REVIEW,
                add.reviewLane());
        assertTrue(add.candidateRequired());

        A3Synexia.Row stale = byPath.get(fixture.staleTarget());
        assertEquals(SynexiaImportPlan.Action.STALE, stale.action());
        assertEquals(
                A3Synexia.ReviewLane.APACHE_MANUAL_REVIEW,
                stale.reviewLane());
        assertEquals("REVIEW_STALE_NO_DELETE", stale.nextAction());
        assertFalse(stale.candidateRequired());

        assertTrue(rows.stream().allMatch(row -> row.sourceRevision().equals("a".repeat(40))));
        assertTrue(rows.stream().allMatch(row -> row.manifestRoot().length() == 64));
        assertTrue(rows.stream().allMatch(row -> row.planRoot().length() == 64));
    }

    @Test
    void everyDeliveryLaneMapsToExactlyOneReviewLane() {
        assertEquals(
                A3Synexia.ReviewLane.RECIPE_EXECUTION_REVIEW,
                A3Synexia.review(SynexiaImportPlan.Lane.OPENREWRITE_RECIPE));
        assertEquals(
                A3Synexia.ReviewLane.RECIPE_PROOF_REVIEW,
                A3Synexia.review(SynexiaImportPlan.Lane.OPENREWRITE_TEST));
        assertEquals(
                A3Synexia.ReviewLane.RECIPE_RESOURCE_REVIEW,
                A3Synexia.review(SynexiaImportPlan.Lane.OPENREWRITE_RESOURCE));
        assertEquals(
                A3Synexia.ReviewLane.JAVA_ATOM_PATTERN_REVIEW,
                A3Synexia.review(SynexiaImportPlan.Lane.CONVERGENCE_JAVA));
        assertEquals(
                A3Synexia.ReviewLane.JAVA_CONTRACT_PROOF_REVIEW,
                A3Synexia.review(SynexiaImportPlan.Lane.CONVERGENCE_TEST));
        assertEquals(
                A3Synexia.ReviewLane.JNI_NATIVE_PARITY_REVIEW,
                A3Synexia.review(SynexiaImportPlan.Lane.CONVERGENCE_NATIVE));
        assertEquals(
                A3Synexia.ReviewLane.DELIVERY_TOOL_REVIEW,
                A3Synexia.review(SynexiaImportPlan.Lane.M3_CLONER));
        assertEquals(
                A3Synexia.ReviewLane.INDEX_PRECOMPUTE_REVIEW,
                A3Synexia.review(SynexiaImportPlan.Lane.M3INDEX));
        assertEquals(
                A3Synexia.ReviewLane.RECIPE_DAG_REVIEW,
                A3Synexia.review(SynexiaImportPlan.Lane.M3_RECIPE));
        assertEquals(
                A3Synexia.ReviewLane.APACHE_MANUAL_REVIEW,
                A3Synexia.review(SynexiaImportPlan.Lane.OTHER_APACHE));
    }

    @Test
    void deltaActionsMapToOneNextStep() {
        assertEquals(
                "REUSE_VERIFIED_VENDOR",
                A3Synexia.next(SynexiaImportPlan.Action.KEEP));
        assertEquals(
                "STAGE_THEN_REVIEW",
                A3Synexia.next(SynexiaImportPlan.Action.ADD));
        assertEquals(
                "STAGE_THEN_REVIEW",
                A3Synexia.next(SynexiaImportPlan.Action.REPLACE));
        assertEquals(
                "REVIEW_STALE_NO_DELETE",
                A3Synexia.next(SynexiaImportPlan.Action.STALE));
    }

    @Test
    void derivedRowRefusesCandidateReviewAndActionDrift() {
        String sha = "1".repeat(64);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new A3Synexia.Row(
                                0,
                                "a".repeat(40),
                                "b".repeat(64),
                                "c".repeat(64),
                                "m3/vendor/synexia/A.java",
                                SynexiaImportPlan.Action.ADD,
                                SynexiaImportPlan.Lane.OPENREWRITE_RECIPE,
                                A3Synexia.ReviewLane.RECIPE_EXECUTION_REVIEW,
                                sha,
                                "ABSENT",
                                false,
                                "STAGE_THEN_REVIEW"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new A3Synexia.Row(
                                0,
                                "a".repeat(40),
                                "b".repeat(64),
                                "c".repeat(64),
                                "m3/vendor/synexia/A.java",
                                SynexiaImportPlan.Action.KEEP,
                                SynexiaImportPlan.Lane.OPENREWRITE_RECIPE,
                                A3Synexia.ReviewLane.JNI_NATIVE_PARITY_REVIEW,
                                sha,
                                sha,
                                false,
                                "REUSE_VERIFIED_VENDOR"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new A3Synexia.Row(
                                0,
                                "a".repeat(40),
                                "b".repeat(64),
                                "c".repeat(64),
                                "m3/vendor/synexia/A.java",
                                SynexiaImportPlan.Action.KEEP,
                                SynexiaImportPlan.Lane.OPENREWRITE_RECIPE,
                                A3Synexia.ReviewLane.RECIPE_EXECUTION_REVIEW,
                                sha,
                                sha,
                                false,
                                "STAGE_THEN_REVIEW"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new A3Synexia.Row(
                                0,
                                "a".repeat(40),
                                "b".repeat(64),
                                "c".repeat(64),
                                "outside/A.java",
                                SynexiaImportPlan.Action.KEEP,
                                SynexiaImportPlan.Lane.OPENREWRITE_RECIPE,
                                A3Synexia.ReviewLane.RECIPE_EXECUTION_REVIEW,
                                sha,
                                sha,
                                false,
                                "REUSE_VERIFIED_VENDOR"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new A3Synexia.Row(
                                0,
                                "a".repeat(40),
                                "b".repeat(64),
                                "c".repeat(64),
                                "m3/vendor/synexia/A.java",
                                SynexiaImportPlan.Action.KEEP,
                                SynexiaImportPlan.Lane.OPENREWRITE_RECIPE,
                                A3Synexia.ReviewLane.RECIPE_EXECUTION_REVIEW,
                                "ABSENT",
                                sha,
                                false,
                                "REUSE_VERIFIED_VENDOR"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new A3Synexia.Row(
                                0,
                                "a".repeat(40),
                                "b".repeat(64),
                                "c".repeat(64),
                                "m3/vendor/synexia/A.java",
                                SynexiaImportPlan.Action.REPLACE,
                                SynexiaImportPlan.Lane.OPENREWRITE_RECIPE,
                                A3Synexia.ReviewLane.RECIPE_EXECUTION_REVIEW,
                                sha,
                                sha,
                                true,
                                "STAGE_THEN_REVIEW"));
    }

    @Test
    void writeAndTopLevelCliProduceSameReadOnlyWorkTable() throws Exception {
        Fixture fixture = fixture();
        Path direct = Path.of("m3/build/a3/synexia-direct.tsv");
        A3Synexia.write(fixture.root(), fixture.manifestPath(), direct);

        Path cli = Path.of("m3/build/a3/synexia-cli.tsv");
        A3.main(
                new String[] {
                    "synexia",
                    "--root",
                    fixture.root().toString(),
                    "--manifest",
                    fixture.manifestPath().toString(),
                    "--out",
                    cli.toString()
                });

        String directText = Files.readString(fixture.root().resolve(direct));
        String cliText = Files.readString(fixture.root().resolve(cli));
        assertEquals(directText, cliText);
        assertTrue(
                directText.startsWith(
                        "order\tsource_revision\tmanifest_root\tplan_root\ttarget_path\t"));
        assertTrue(directText.contains("\tfalse\n"));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3.main(
                                new String[] {
                                    "synexia",
                                    "--root",
                                    fixture.root().toString()
                                }));
    }

    @Test
    void missingUnsafeAndMalformedManifestFailClosed() throws Exception {
        Path root = temp.resolve("fail-root");
        Files.createDirectories(root.resolve("m3"));

        assertThrows(
                IOException.class,
                () -> A3Synexia.load(root, Path.of("missing.tsv")));

        Path malformed = root.resolve("m3/malformed.tsv");
        Files.writeString(malformed, "bad\n");
        assertThrows(
                IllegalArgumentException.class,
                () -> A3Synexia.load(root, Path.of("m3/malformed.tsv")));

        Path outside = temp.resolve("outside.tsv");
        Files.writeString(outside, "bad\n");
        assertThrows(
                IllegalArgumentException.class,
                () -> A3Synexia.load(root, Path.of("../outside.tsv")));
    }

    private Fixture fixture() throws Exception {
        Path root = temp.resolve("jdk");
        Files.createDirectories(root.resolve("m3/build"));

        String keepSource =
                "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/Keep.java";
        String replaceSource =
                "synexia-code-convergence/src/main/java/com/synexia/convergence/Replace.java";
        String addSource =
                "synexia-code-convergence/src/main/native/add.c";

        String keepTarget = "m3/vendor/synexia/" + keepSource;
        String replaceTarget = "m3/vendor/synexia/" + replaceSource;
        String addTarget = "m3/vendor/synexia/" + addSource;
        String staleTarget = "m3/vendor/synexia/obsolete/Old.java";

        byte[] keep = bytes("keep");
        byte[] replacement = bytes("replacement");
        byte[] oldReplacement = bytes("old-replacement");
        byte[] nativeBytes = bytes("native");
        byte[] stale = bytes("stale");

        write(root.resolve(keepTarget), keep);
        write(root.resolve(replaceTarget), oldReplacement);
        write(root.resolve(staleTarget), stale);

        SynexiaImportManifest manifest =
                new SynexiaImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(
                                entry(
                                        "openrewrite-java",
                                        keepSource,
                                        keepTarget,
                                        keep,
                                        SynexiaImportManifest.Mode.APACHE_SOURCE),
                                entry(
                                        "convergence-java",
                                        replaceSource,
                                        replaceTarget,
                                        replacement,
                                        SynexiaImportManifest.Mode.APACHE_SOURCE),
                                entry(
                                        "convergence-native",
                                        addSource,
                                        addTarget,
                                        nativeBytes,
                                        SynexiaImportManifest.Mode.APACHE_SOURCE)),
                        "");

        Path manifestPath = Path.of("m3/build/synexia-export.tsv");
        Files.writeString(root.resolve(manifestPath), manifest.toTsv());
        return new Fixture(
                root,
                manifestPath,
                keepTarget,
                replaceTarget,
                addTarget,
                staleTarget);
    }

    private static SynexiaImportManifest.Entry entry(
            String category,
            String source,
            String target,
            byte[] bytes,
            SynexiaImportManifest.Mode mode) {
        return new SynexiaImportManifest.Entry(
                category,
                source,
                target,
                sha(bytes),
                "Apache-2.0",
                mode);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void write(Path path, byte[] bytes) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
    }

    private static String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private record Fixture(
            Path root,
            Path manifestPath,
            String keepTarget,
            String replaceTarget,
            String addTarget,
            String staleTarget) {}
}
