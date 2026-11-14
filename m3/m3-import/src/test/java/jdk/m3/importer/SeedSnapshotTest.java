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
                "153e76e966c790046057caa72a238c13fdfb10815b26e7956154b3dcb828fd8d",
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
