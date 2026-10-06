// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class A3SynexiaTest {

    @Test
    void exactApacheSnapshotIsPinnedAndAuthorityBounded() throws Exception {
        Path root = repositoryRoot();
        List<A3Synexia.Entry> entries = A3Synexia.load(root);
        A3Synexia.Report report = A3Synexia.verify(root);

        assertEquals("hsoliwal/com.synexia", report.repository());
        assertEquals("ae955ea9b7e246d780269525b15fa38b95ffba67", report.revision());
        assertEquals(11, report.entries());
        assertEquals(3, report.recipeEntries());
        assertEquals(2, report.masteryEntries());
        assertEquals(4, report.publicPolishEntries());
        assertEquals(64, report.root().length());
        assertTrue(report.toolingSourceBorrowAuthority());
        assertFalse(report.productSourceMutationAuthority());
        assertFalse(report.promotionAuthority());

        assertTrue(entries.stream().allMatch(entry -> "Apache-2.0".equals(entry.license())));
        assertTrue(entries.stream().anyMatch(
                entry -> entry.sourcePath().endsWith("M3AtomizeRecipe.java")));
        assertTrue(entries.stream().anyMatch(
                entry -> entry.sourcePath().endsWith("M3PatternizeRecipe.java")));
        assertTrue(entries.stream().anyMatch(
                entry -> entry.sourcePath().endsWith("M3RecipeMasteryLab.java")));
        assertTrue(entries.stream().anyMatch(
                entry -> entry.sourcePath().endsWith("M3RegexStringMasteryCorpus.java")));
        assertTrue(entries.stream().anyMatch(
                entry -> entry.sourcePath().endsWith("RepositoryPublicPolishWorkspace.java")));
        assertTrue(entries.stream().anyMatch(
                entry -> entry.sourcePath().endsWith("CompetitiveProgrammingDonorCatalog.java")));
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            if (Files.isRegularFile(current.resolve("configure"))
                    && Files.isDirectory(current.resolve("m3/tooling/a3"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate M3JDK21 repository root");
    }
}
