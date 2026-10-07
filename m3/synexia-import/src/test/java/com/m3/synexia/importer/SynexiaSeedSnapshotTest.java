// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class SynexiaSeedSnapshotTest {
    @Test
    void checkedInSeedManifestMatchesEveryVendoredSynexiaFile() throws Exception {
        Path repository = repositoryRoot();
        Path manifestPath =
                repository.resolve("m3/synexia-import/synexia-seed-export.tsv");
        assertTrue(Files.isRegularFile(manifestPath));

        SynexiaImportManifest manifest =
                SynexiaImportManifest.parse(Files.readString(manifestPath));
        assertEquals(
                "62aea466cf2f2bb4668f38aad9343d59525965b5",
                manifest.sourceRevision());
        assertEquals(
                "ca570e874586c327f98e3982bf9e62a404e87e266b3195df69be06a0b5111ef6",
                manifest.root());
        assertEquals(15, manifest.entries().size());

        SynexiaImporter.verifyTargetSnapshot(repository, manifest);
    }

    private static Path repositoryRoot() {
        Path module = Path.of("").toAbsolutePath().normalize();
        if ("synexia-import".equals(module.getFileName().toString())) {
            return module.getParent().getParent();
        }
        Path multi = Path.of(
                        System.getProperty("maven.multiModuleProjectDirectory", "."))
                .toAbsolutePath()
                .normalize();
        return "m3".equals(multi.getFileName().toString())
                ? multi.getParent()
                : multi;
    }
}
