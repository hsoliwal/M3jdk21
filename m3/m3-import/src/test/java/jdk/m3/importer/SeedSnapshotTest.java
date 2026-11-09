// SPDX-License-Identifier: Apache-2.0
package jdk.m3.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class SeedSnapshotTest {
    @Test
    void checkedInSeedManifestMatchesEveryVendoredM3File() throws Exception {
        Path repository = repositoryRoot();
        Path manifestPath =
                repository.resolve("m3/m3-import/m3-seed-export.tsv");
        assertTrue(Files.isRegularFile(manifestPath));

        ImportManifest manifest =
                ImportManifest.parse(Files.readString(manifestPath));
        assertEquals(
                "62aea466cf2f2bb4668f38aad9343d59525965b5",
                manifest.sourceRevision());
        assertEquals(
                "aca867c7bc1f7beda95f99641c68559cf03df5b0b60d6b9ac010214c4d3eaba8",
                manifest.root());
        assertEquals(15, manifest.entries().size());

        Importer.verifyTargetSnapshot(repository, manifest);
    }

    private static Path repositoryRoot() {
        Path module = Path.of("").toAbsolutePath().normalize();
        if ("m3-import".equals(module.getFileName().toString())) {
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
