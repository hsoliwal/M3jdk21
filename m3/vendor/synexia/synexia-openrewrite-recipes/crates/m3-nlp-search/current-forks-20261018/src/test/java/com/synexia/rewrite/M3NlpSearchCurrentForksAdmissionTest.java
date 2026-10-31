// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;

class M3NlpSearchCurrentForksAdmissionTest {
    static final String ID = "m3-lucene-jdk21-current-fork-20261018";
    static final Path CRATE = Path.of(System.getProperty("m3.nlp.crate"));
    static final Path RESOURCES = CRATE.resolve("../../../src/main/resources").normalize();
    @TempDir Path temporary;

    Path inputs() throws Exception {
        Path root = temporary.resolve("inputs");
        Path fixtures = CRATE.resolve("preimages/lucene");
        try (var files = Files.walk(fixtures)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".java.txt")).toList()) {
                String relative = fixtures.relativize(file).toString();
                assertTrue(relative.endsWith(".txt"));
                Path output = root.resolve(relative.substring(0, relative.length() - 4));
                Files.createDirectories(output.getParent());
                Files.write(output, Files.readAllBytes(file));
            }
        }
        return root;
    }

    @Test void reviewedCurrentForkBatchUsesSdkAndRetainsReplayRefusals() throws Exception {
        var receipt = M3NlpSearchCurrentForksRecipeRunner.qualify("text", ID, RESOURCES,
                inputs(), temporary.resolve("overlay"));
        assertEquals(176, receipt.get("targets"));
        assertEquals(176, receipt.get("first_application_changes"));
        assertEquals(0, receipt.get("fixed_point_changes"));
        assertEquals(1, receipt.get("missing_refusals"));
    }

    @Test void receiverJavaKeepsExactOwnersAndModuleScope() throws Exception {
        String id = "m3-nlp-current-forks-receiver-java-20261018";
        var recipe = new M3HashPinnedJavaSnapshotRecipe(id);
        assertEquals(6, recipe.targetPaths().size());
        assertEquals(M3EditScope.MODULE, recipe.requiredScope());
        Path empty = temporary.resolve("empty");
        Files.createDirectories(empty);
        var receipt = M3NlpSearchCurrentForksRecipeRunner.qualify("java", id, RESOURCES,
                empty, temporary.resolve("receiver"));
        assertEquals(6, receipt.get("first_application_changes"));
        assertThrows(IllegalArgumentException.class, () -> M3ScopeInference.describeJavaTarget(
                "m3/tooling/module-packs/nlp-search/tests/UnapprovedProbe.java"));
    }

    @Test void publicMirrorAdmitsOnlyItsExactLuceneDataOwner() throws Exception {
        Path empty = temporary.resolve("empty-mirror");
        Files.createDirectories(empty);
        var receipt = M3NlpSearchCurrentForksRecipeRunner.qualify("text",
                "m3-nlp-current-forks-mirror-data-test-20261018", RESOURCES,
                empty, temporary.resolve("mirror-data"));
        assertEquals(1, receipt.get("first_application_changes"));
        var neighbor = new M3HashPinnedTextSnapshotRecipe(
                "m3-nlp-current-forks-mirror-neighbor-test-20261018");
        assertThrows(IllegalStateException.class, () -> neighbor.getInitialValue(new InMemoryExecutionContext()));
    }

    @Test void adjacentCrateCannotAdmitJavaSources() {
        var recipe = new M3HashPinnedTextSnapshotRecipe("m3-nlp-current-forks-unsealed-test-20261018");
        assertThrows(IllegalStateException.class, () -> recipe.getInitialValue(new InMemoryExecutionContext()));
    }

    @Test void changedNamedManifestCannotAdmitJavaSources() throws Exception {
        Path resource = temporary.resolve("resources/com/synexia/rewrite/hash-pinned-text/" + ID);
        Files.createDirectories(resource);
        Files.writeString(resource.resolve("manifest.tsv"), Files.readString(RESOURCES.resolve(
                "com/synexia/rewrite/hash-pinned-text/" + ID + "/manifest.tsv")) + "# unsealed revision\n");
        Path root = temporary.resolve("resources");
        String cp = root + java.io.File.pathSeparator + System.getProperty("java.class.path");
        var command = List.of(Path.of(System.getProperty("java.home"), "bin/java").toString(), "-cp", cp,
                M3NlpSearchCurrentForksRecipeRunner.class.getName(), "text", ID, root.toString(),
                inputs().toString(), temporary.resolve("rejected").toString(),
                temporary.resolve("receipt.json").toString());
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertNotEquals(0, process.waitFor());
        assertTrue(output.contains("invalid hash-pinned text manifest row"), output);
        assertFalse(Files.exists(temporary.resolve("receipt.json")));
        assertFalse(Files.exists(temporary.resolve("rejected")));
    }
}
