// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite.sorts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;

/** Qualifies the real sealed product packet and its additive filesystem receiver. */
final class M3JavaUtilSortsPacketTest {
    @TempDir Path temp;

    @Test
    void realNamedRecipeCreatesExactPacketAndReplaysToZeroChanges() throws Exception {
        var url = getClass().getResource("/META-INF/rewrite/m3-java-util-sorts-20261018.yml");
        assertNotNull(url);
        try (var stream = url.openStream()) {
            var environment = Environment.builder().load(new YamlResourceLoader(
                    stream, url.toURI(), new Properties(), getClass().getClassLoader())).build();
            var named = environment.activateRecipes("com.synexia.m3.JavaUtilSorts20261018");
            named.validateAll().forEach(validation ->
                    assertTrue(validation.isValid(), () -> validation.failures().toString()));
            var result = M3JavaUtilSortsApply.evaluate(named, List.of());
            var targets = M3JavaUtilSortsApply.targets();
            assertTrue(targets.size() >= 7, "The complete sorting packet must be sealed");
            assertEquals(targets.size(), result.changed().size());
            assertTrue(targets.stream().allMatch(target -> target.before().equals("ABSENT")));
            Map<String, String> output = M3RecipeReplayTest.texts(result.sources());
            for (var target : targets) {
                assertEquals(target.after(), M3JavaUtilSortsApply.sha256(output.get(target.path())));
            }
            assertTrue(M3JavaUtilSortsApply.evaluate(named, result.sources()).changed().isEmpty());
        }
    }

    @Test
    void dryRunDoesNotCreateAnyTargetAndApplyReadbackIsAnExactFixedPoint() throws Exception {
        assertThrows(IllegalStateException.class, () -> M3JavaUtilSortsApply.check(temp));
        var dry = M3JavaUtilSortsApply.apply(temp, false);
        try (var entries = Files.list(temp)) {
            assertEquals(0L, entries.count());
        }
        var applied = M3JavaUtilSortsApply.apply(temp, true);
        assertEquals(dry.treeSha256(), applied.treeSha256());
        assertEquals(dry.changed(), applied.changed());
        assertEquals(applied.targets(), applied.changed().size());
        for (var target : M3JavaUtilSortsApply.targets()) {
            assertEquals(target.after(), M3JavaUtilSortsApply.sha256(
                    Files.readString(temp.resolve(target.path()), StandardCharsets.UTF_8)));
        }
        var repeat = M3JavaUtilSortsApply.apply(temp, true);
        assertEquals(applied.treeSha256(), repeat.treeSha256());
        assertTrue(repeat.changed().isEmpty());
        assertTrue(repeat.receipt().contains("fixed_point_changed_count\t0\n"));
        var checked = M3JavaUtilSortsApply.check(temp);
        assertEquals(repeat.treeSha256(), checked.treeSha256());
        assertTrue(checked.receipt().contains("mode\tcheck\n"));
    }

    @Test
    void aPartialExactReceiverCompletesTheSameSealedTree() throws Exception {
        var generated = M3JavaUtilSortsApply.evaluate(new M3JavaUtilSortsRecipe(), List.of());
        Map<String, String> output = M3RecipeReplayTest.texts(generated.sources());
        List<String> paths = output.keySet().stream().sorted().toList();
        int present = 0;
        for (int index = 0; index < paths.size(); index += 2) {
            String relative = paths.get(index);
            Path file = temp.resolve(relative);
            Files.createDirectories(file.getParent());
            Files.writeString(file, output.get(relative), StandardCharsets.UTF_8);
            present++;
        }
        var applied = M3JavaUtilSortsApply.apply(temp, true);
        assertEquals(paths.size() - present, applied.changed().size());
        assertTrue(M3JavaUtilSortsApply.apply(temp, true).changed().isEmpty());
    }

    @Test
    void aForeignOccupantRefusesBeforeAnyProductFileIsWritten() throws Exception {
        var target = M3JavaUtilSortsApply.targets().getFirst();
        Path file = temp.resolve(target.path());
        Files.createDirectories(file.getParent());
        Files.writeString(file, "Foreign occupant: do not overwrite\n", StandardCharsets.UTF_8);
        IllegalStateException refusal = assertThrows(IllegalStateException.class,
                () -> M3JavaUtilSortsApply.apply(temp, true));
        assertTrue(refusal.getMessage().contains("Foreign occupant or source drift"));
        assertEquals("Foreign occupant: do not overwrite\n", Files.readString(file));
        try (var files = Files.walk(temp)) {
            assertEquals(1L, files.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void targetSymlinkParentsRefuseBeforeAnyFilesystemMutation() throws Exception {
        Path root = Files.createDirectory(temp.resolve("receiver"));
        Path outside = Files.createDirectory(temp.resolve("outside"));
        Files.createSymbolicLink(root.resolve("src"), outside);
        IllegalStateException refusal = assertThrows(IllegalStateException.class,
                () -> M3JavaUtilSortsApply.apply(root, true));
        assertTrue(refusal.getMessage().contains("Symlink in target path"));
        try (var files = Files.list(outside)) {
            assertEquals(0L, files.count());
        }
        assertFalse(Files.exists(root.resolve("m3")));
    }

    @Test
    void checkModeAcceptsPublishedHeadAndRecordsItWhileApplyKeepsTheOriginalBase() throws Exception {
        Path root = Files.createDirectory(temp.resolve("receiver"));
        M3JavaUtilSortsApply.apply(root, true);
        git(root, "init", "--quiet", "--object-format=sha1");
        git(root, "-c", "user.name=M3 recipe fixture",
                "-c", "user.email=recipe-fixture@example.invalid",
                "commit", "--quiet", "--allow-empty", "-m", "Isolated check-mode fixture");
        String actual = git(root, "rev-parse", "HEAD").strip();
        assertFalse(actual.equals(M3JavaUtilSortsRecipe.TARGET_BASE));
        Path receipt = temp.resolve("check-receipt.tsv");
        M3JavaUtilSortsApply.main(new String[] {root.toString(), "check", receipt.toString()});
        String proof = Files.readString(receipt);
        assertTrue(proof.contains("checked_revision\t" + actual + "\n"));
        assertTrue(proof.contains("target_base\t" + M3JavaUtilSortsRecipe.TARGET_BASE + "\n"));
        assertTrue(proof.contains("mode\tcheck\n"));
        assertThrows(IllegalStateException.class, () -> M3JavaUtilSortsApply.main(
                new String[] {root.toString(), "dry-run", receipt.toString()}));
        assertThrows(IllegalStateException.class, () -> M3JavaUtilSortsApply.main(
                new String[] {root.toString(), "apply", receipt.toString()}));
        assertTrue(M3JavaUtilSortsApply.check(root).changed().isEmpty());
    }

    private static String git(Path root, String... options) throws Exception {
        var command = new java.util.ArrayList<String>();
        command.add("git");
        command.addAll(List.of(options));
        Process process = new ProcessBuilder(command).directory(root.toFile())
                .redirectErrorStream(true).start();
        assertTrue(process.waitFor(15, TimeUnit.SECONDS), "Git fixture exceeded its budget");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        return output;
    }

    @Test
    void reusedCanonicalEnginesRemainExactPinnedSourceCopies() throws Exception {
        Path root = Path.of(System.getProperty("crateRoot"));
        Map<String, String> originals = Map.of(
                "M3HashPinnedJavaSnapshotRecipe.java", "3bdeeaea9a212fe0c7d1f544510f87c4cd49eb40",
                "M3HashPinnedTextSnapshotRecipe.java", "9d0788d3717e017d58345c5c3e62022c2915a3c8",
                "M3ScopeInference.java", "d24729950508857280ea1b56d4e930f05137ec85",
                "M3EditScope.java", "274360816db1e44a2f1796a3bc5cdeff6b0ce6db");
        for (var original : originals.entrySet()) {
            byte[] content = Files.readAllBytes(root.resolve(
                    "src/main/java/com/synexia/rewrite/" + original.getKey()));
            var digest = MessageDigest.getInstance("SHA-1");
            digest.update(("blob " + content.length + "\0").getBytes(StandardCharsets.UTF_8));
            assertEquals(original.getValue(), HexFormat.of().formatHex(digest.digest(content)));
        }
        assertTrue(Files.readString(root.resolve("LICENSE")).contains("Apache License"));
        assertTrue(Files.readString(root.resolve("provenance/SYNEXIA-NOTICE.txt"))
                .contains("Hitesh Soliwal"));
        try (var lines = Files.lines(root.resolve("provenance/engine-sources.tsv"))) {
            assertEquals(5L, lines.count());
        }
    }
}
