// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Real OpenRewrite XML SDK candidate emission before any live POM modifications. */
final class M3CollectionDependencyRepairTest {
    private static final Path CRATE = Path.of(System.getProperty("user.dir"));
    private static final Path ROOT = CRATE.resolve("../../../..").normalize();

    private static Map<String, String> original() throws IOException {
        return Map.of(
                M3CollectionDependencyRepair.MODULE,
                M3CollectionDependencyRepair.resource(CRATE, "pre/collections-pom.xml.txt"),
                M3CollectionDependencyRepair.RECIPE,
                M3CollectionDependencyRepair.resource(CRATE, "pre/recipe-pom.xml.txt"));
    }

    private static Map<String, String> finalImages() throws IOException {
        return Map.of(
                M3CollectionDependencyRepair.MODULE,
                M3CollectionDependencyRepair.resource(CRATE, "collections-pom.xml.txt"),
                M3CollectionDependencyRepair.RECIPE,
                M3CollectionDependencyRepair.resource(CRATE, "recipe-pom.xml.txt"));
    }

    @Test
    void sourceManifestMatchesTwoActualKnownOriginalOrGeneratedPoms() throws IOException {
        String manifest = M3CollectionDependencyRepair.resource(CRATE, "pom-manifest.tsv");
        String[] rows = manifest.strip().split("\\R");
        assertEquals(2, rows.length);
        for (String row : rows) {
            String[] fields = row.split("\\t", -1);
            assertEquals(4, fields.length);
            String original = original().get(fields[0]);
            String postimage = finalImages().get(fields[0]);
            assertEquals(fields[1], M3CollectionDependencyRepair.sha256(original));
            assertEquals(fields[2], M3CollectionDependencyRepair.sha256(postimage));
            String real = Files.readString(ROOT.resolve(fields[0]));
            assertTrue(real.equals(original) || real.equals(postimage),
                    "source drift must fail before OpenRewrite: " + fields[0]);
        }
    }

    @Test
    void actualOpenRewriteXmlTwoPostimagesAndIdempotentReplay() throws IOException {
        var received = M3CollectionDependencyRepair.emit(CRATE, original());
        assertEquals(finalImages(), received,
                "actual OpenRewrite XML postimages must exactly equal SHA-sealed sources");
        assertTrue(M3CollectionDependencyRepair.emit(CRATE, received).isEmpty(),
                "OpenRewrite recipe must reach a no-change fixed point");
    }

    @Test
    void incrementalPartialEmissionCannotAlterAlreadyQualifiedPom() throws IOException {
        Map<String, String> halfway = new HashMap<>(original());
        halfway.put(M3CollectionDependencyRepair.RECIPE,
                finalImages().get(M3CollectionDependencyRepair.RECIPE));
        var delta = M3CollectionDependencyRepair.emit(CRATE, halfway);
        assertEquals(1, delta.size());
        assertFalse(delta.containsKey(M3CollectionDependencyRepair.RECIPE));
        assertEquals(finalImages().get(M3CollectionDependencyRepair.MODULE),
                delta.get(M3CollectionDependencyRepair.MODULE));
    }

    @Test
    void missingTamperedAndOccupiedSourceFailClosed() throws IOException {
        assertThrows(IllegalStateException.class,
                () -> M3CollectionDependencyRepair.emit(CRATE, Map.of()));
        Map<String, String> edited = new HashMap<>(original());
        edited.put(M3CollectionDependencyRepair.MODULE,
                edited.get(M3CollectionDependencyRepair.MODULE) + "<!-- attacker -->\n");
        assertThrows(IllegalStateException.class,
                () -> M3CollectionDependencyRepair.emit(CRATE, edited));
        edited = new HashMap<>(original());
        edited.put(M3CollectionDependencyRepair.RECIPE, "not XML at all");
        Map<String, String> occupied = edited;
        assertThrows(IllegalStateException.class,
                () -> M3CollectionDependencyRepair.emit(CRATE, occupied));
        Map<String, String> unrelated = new HashMap<>(original());
        unrelated.put("m3/collections/src/main/java/com/m3/collections/Incorrect.java", "x");
        assertThrows(IllegalStateException.class,
                () -> M3CollectionDependencyRepair.emit(CRATE, unrelated));
    }
}
