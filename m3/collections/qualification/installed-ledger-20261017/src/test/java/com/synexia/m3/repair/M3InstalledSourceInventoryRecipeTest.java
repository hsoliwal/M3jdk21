// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Maven source inventory recipe proof: never whitelist an unknown or changed digest. */
final class M3InstalledSourceInventoryRecipeTest {
    private static final Path CRATE = Path.of(System.getProperty("user.dir"));
    private static final Path ROOT = CRATE.resolve("../../../..").normalize();
    private static final String SOURCE_COMMIT =
            "80332fcefd2b9c23501a86cb0ca01ee476460795";

    private static String generate() throws IOException {
        return M3InstalledSourceInventoryRecipe.emit(ROOT, SOURCE_COMMIT);
    }

    @Test
    void newInventoryIsCompleteStableSortedAndBoundToCurrentActualSource() throws IOException {
        String candidate = generate();
        List<String> rows = candidate.lines().filter(l -> !l.startsWith("#")).toList();
        assertEquals(136, rows.size());
        assertTrue(candidate.contains(SOURCE_COMMIT));
        Set<String> seen = new HashSet<>();
        String previous = "";
        int javaSources = 0;
        for (String row : rows) {
            String[] parts = row.split("\t", -1);
            assertEquals(2, parts.length);
            assertTrue(seen.add(parts[0]), "duplicates forbidden");
            assertTrue(previous.compareTo(parts[0]) < 0, "must sort exact receiver paths");
            previous = parts[0];
            String contentSha = M3InstalledSourceInventoryRecipe.digest(ROOT.resolve(parts[0]));
            assertEquals(contentSha, parts[1]);
            if (parts[0].contains("/src/") && parts[0].endsWith(".java")) javaSources++;
        }
        assertEquals(132, javaSources);
        assertTrue(seen.contains("m3/collections/LICENSE"));
        assertTrue(seen.contains("m3/collections/NOTICE"));
        String old = M3InstalledSourceInventoryRecipe.digest(ROOT.resolve(
                "m3/collections/qualification/installed-source-enum-set-20261014.tsv"));
        assertEquals("90e6e4adcc6a9224b6ca7f85b5d01b950ed8c368fcc660b04ce56fda59eae689",
                old, "preserve independently observed historical manifest content");
        assertEquals(candidate, generate(), "repeated source image differs");
        Path installed = ROOT.resolve("m3/collections/qualification/installed-source-20261017.tsv");
        assertTrue(!Files.exists(installed) || Files.readString(installed).equals(candidate),
                "installed successor manifest differs from qualified emitter");
        Path out = CRATE.resolve("target/receipt-candidates");
        Files.createDirectories(out);
        Files.writeString(out.resolve("installed-source-20261017.tsv"),
                candidate, StandardCharsets.UTF_8);
        System.out.println("M3_FULL_SOURCE_MANIFEST_SHA256="
                + M3InstalledSourceInventoryRecipe.sha256(candidate.getBytes(StandardCharsets.UTF_8)));
        System.out.println("M3_FULL_SOURCE_MANIFEST_ENTRIES=" + rows.size());
    }

    @Test
    void onlyOriginalOrExactlyGeneratedTopLevelJsonReceiptIsAdmitted() throws IOException {
        String candidate = generate();
        String manifestSha = M3InstalledSourceInventoryRecipe.sha256(
                candidate.getBytes(StandardCharsets.UTF_8));
        String original = M3CollectionDependencyRepair.resource(
                CRATE, "pre/synexia-donation.json.txt");
        String live = Files.readString(ROOT.resolve("m3/collections/synexia-donation.json"));
        String approved = M3DonationPointerRecipe.emit(live, manifestSha, original);
        String reviewed = M3CollectionDependencyRepair.resource(
                CRATE, "synexia-donation-v2.json.txt");
        assertEquals(reviewed, approved, "emitted JSON must match reviewed V2 postimage byte-for-byte");
        assertEquals("a8a4ada0975933a8f80d7377a34c199603025f02",
                M3DonationPointerRecipe.gitBlob(approved));
        assertEquals("36cf1740400fc4dc0d0c4669969f58948bfd34f4e5147d1f4f5d8549cc4bddf9",
                M3InstalledSourceInventoryRecipe.sha256(approved.getBytes(StandardCharsets.UTF_8)));
        assertEquals(approved, M3DonationPointerRecipe.emit(approved, manifestSha, original));
        assertTrue(approved.contains("\"installed_source_manifest\": \"qualification/installed-source-20261017.tsv\""));
        assertTrue(approved.contains("\"installed_source_manifest_sha256\": \"" + manifestSha + "\""));
        // Red baseline: the old root had duplicate JSON keys; Python json.loads
        // silently preferred the historical value over the new verified manifest.
        String rootKey = "  \"installed_source_manifest\": ";
        assertEquals(2, original.lines().filter(line -> line.startsWith(rootKey)).count());
        assertEquals(2, live.lines().filter(line -> line.startsWith(rootKey)).count());
        assertEquals(1, approved.lines().filter(line -> line.startsWith(rootKey)).count());
        assertTrue(approved.contains(
                "\"historical_installed_source_manifest_20261008\": "
                + "\"qualification/installed-source-enum-set-20261008.tsv\""));
        assertTrue(!approved.contains(
                "  \"installed_source_manifest\": \"qualification/installed-source-enum-set-20261008.tsv\""));
        assertEquals(approved, M3DonationPointerRecipe.emit(
                M3CollectionDependencyRepair.resource(CRATE, "synexia-donation.json.txt"),
                manifestSha, original));
        assertThrows(IllegalStateException.class,
                () -> M3DonationPointerRecipe.emit(live + "\n{ \"attacker\": true }",
                        manifestSha, original));
        assertThrows(IllegalStateException.class,
                () -> M3DonationPointerRecipe.emit(live, "not-a-sha256", original));
        assertThrows(IllegalStateException.class,
                () -> M3DonationPointerRecipe.emit(live, manifestSha,
                        original.replace("Apache-2.0", "MALFORMED")));
        Path out = CRATE.resolve("target/receipt-candidates");
        Files.createDirectories(out);
        Files.writeString(out.resolve("synexia-donation.json"), approved, StandardCharsets.UTF_8);
        System.out.println("M3_DONATION_RECEIPT_SHA256="
                + M3InstalledSourceInventoryRecipe.sha256(approved.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void invalidRootOrCommitRefUsesFailClosedReceiptNotSilentRegeneration() {
        assertThrows(IllegalStateException.class,
                () -> M3InstalledSourceInventoryRecipe.emit(ROOT, "main"));
        assertThrows(IllegalStateException.class,
                () -> M3InstalledSourceInventoryRecipe.emit(ROOT.resolve("missing"), SOURCE_COMMIT));
    }
}
