// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class M3IndexDbArtifactTest {
    @Test
    void createIsContentAddressedAndDefensivelyCopiesPayload() {
        byte[] input = "atom-index".getBytes(StandardCharsets.UTF_8);
        M3IndexDbArtifact artifact =
                M3IndexDbArtifact.create("semantic-nodes", "m3.semantic-index", 1, input);

        input[0] = 0;
        byte[] first = artifact.payload();
        byte[] second = artifact.payload();

        assertArrayEquals("atom-index".getBytes(StandardCharsets.UTF_8), first);
        assertArrayEquals(first, second);
        assertNotSame(first, second);
        assertEquals(10L, artifact.payloadLength());
        assertEquals(64, artifact.contentSha256().length());
    }

    @Test
    void rejectsInvalidMetadataAndContentDrift() {
        byte[] payload = {1, 2, 3};

        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbArtifact.create("", "kind", 1, payload));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbArtifact.create("name", "", 1, payload));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbArtifact.create("name", "kind", 0, payload));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbArtifact(
                        "name",
                        "kind",
                        1,
                        "0".repeat(64),
                        payload));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbArtifact(
                        "name",
                        "kind",
                        1,
                        "not-a-sha",
                        payload));
    }
}
