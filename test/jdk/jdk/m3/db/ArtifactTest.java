// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class ArtifactTest {
    @Test
    void createIsContentAddressedAndDefensivelyCopiesPayload() {
        byte[] input = "atom-index".getBytes(StandardCharsets.UTF_8);
        Artifact artifact =
                Artifact.create("semantic-nodes", "m3.semantic-index", 1, input);

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
                () -> Artifact.create("", "kind", 1, payload));
        assertThrows(
                IllegalArgumentException.class,
                () -> Artifact.create("name", "", 1, payload));
        assertThrows(
                IllegalArgumentException.class,
                () -> Artifact.create("name", "kind", 0, payload));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Artifact(
                        "name",
                        "kind",
                        1,
                        "0".repeat(64),
                        payload));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Artifact(
                        "name",
                        "kind",
                        1,
                        "not-a-sha",
                        payload));
    }
}
