// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M3IndexDBTest {
    @Test
    void deduplicatesPayloadsByContentHashAndRestoresNamedArtifacts() throws Exception {
        Path root = Files.createTempDirectory("m3indexdb-");
        byte[] payload = "same-payload".getBytes(StandardCharsets.UTF_8);

        try (M3IndexDB db = M3IndexDB.open(root)) {
            M3IndexDbArtifact first =
                    db.putArtifact("nodes", "semantic", 1, payload);
            M3IndexDbArtifact second =
                    db.putArtifact("edges", "semantic", 1, payload);

            assertEquals(first.contentSha256(), second.contentSha256());
            assertEquals(payload.length, db.blobBytes());
            assertTrue(db.containsArtifact("nodes"));
            assertTrue(db.containsArtifact("edges"));
            assertArrayEquals(payload, db.requireArtifact("nodes").payload());
            assertArrayEquals(payload, db.requireArtifact("edges").payload());
        }
    }

    @Test
    void closedStoreRefusesOperationsAndMissingArtifactsFailClosed() throws Exception {
        Path root = Files.createTempDirectory("m3indexdb-closed-");
        M3IndexDB db = M3IndexDB.open(root);

        assertThrows(java.io.IOException.class, () -> db.requireArtifact("missing"));
        db.close();

        assertThrows(IllegalStateException.class, db::root);
        assertThrows(IllegalStateException.class, () -> db.containsArtifact("missing"));
        assertThrows(
                IllegalStateException.class,
                () -> db.putArtifact("x", "kind", 1, new byte[] {1}));
    }
}
