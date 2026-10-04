// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** ContractProbe for the existing content store; fixtures never touch global/JDK state. */
final class M3IndexDbStoreBoundaryTest {
    @TempDir Path home;
    private static final String NAME = "artifact";
    private static final byte[] PAYLOAD = "persisted-bytes".getBytes(StandardCharsets.UTF_8);

    @Test
    void rootEmptyStoreAndClosedLifecycleHaveExplicitOutcomes() throws Exception {
        try (M3IndexDB db = M3IndexDB.open(home)) {
            assertEquals(home.toAbsolutePath().normalize(), db.root());
            assertEquals(0L, db.blobBytes());
            assertFalse(db.containsArtifact(NAME));
            Files.createDirectory(home.resolve("blobs/non-payload-directory"));
            assertEquals(0L, db.blobBytes());
            assertThrows(NullPointerException.class, () -> db.putSemanticIndex(NAME, null));
        }
        M3IndexDB closed = M3IndexDB.open(home);
        closed.close();
        closed.close();
        assertThrows(IllegalStateException.class, closed::blobBytes);
        assertThrows(IllegalStateException.class, () -> closed.requireArtifact(NAME));
        assertThrows(IllegalStateException.class, () -> closed.requireSemanticIndex(NAME));
        assertThrows(IllegalStateException.class, () -> closed.putSemanticIndex(NAME, null));
        assertThrows(NullPointerException.class, () -> M3IndexDB.open(null));
        Path occupied = Files.writeString(home.resolve("occupied"), "not a directory");
        assertThrows(IOException.class, () -> M3IndexDB.open(occupied));
    }

    @Test
    void allReferenceEnvelopeFieldsAreCheckedOnRead() throws Exception {
        try (M3IndexDB db = M3IndexDB.open(home)) {
            db.putArtifact(NAME, "bytes", 1, PAYLOAD);
            List<String> valid = Files.readAllLines(ref());
            List<List<String>> corruptions = new ArrayList<>();
            corruptions.add(valid.subList(0, 5));
            for (var mutation : List.of(
                    new Field(0, "OTHER"), new Field(1, "different-name"),
                    new Field(3, "NaN"), new Field(5, "NaN"),
                    new Field(4, "not-sha"), new Field(2, ""),
                    new Field(3, "0"), new Field(5, "-1"),
                    new Field(5, Integer.toString(PAYLOAD.length + 1)))) {
                List<String> changed = new ArrayList<>(valid);
                changed.set(mutation.index(), mutation.value());
                corruptions.add(changed);
            }
            for (List<String> row : corruptions) {
                Files.writeString(ref(), String.join("\n", row) + "\n");
                assertThrows(IOException.class, () -> db.requireArtifact(NAME), row.toString());
            }
            Files.writeString(ref(), String.join("\n", valid) + "\n");
            assertArrayEquals(PAYLOAD, db.requireArtifact(NAME).payload());
        }
    }

    @Test
    void missingAndCorruptBlobCannotBeReturnedOrSilentlyReused() throws Exception {
        try (M3IndexDB db = M3IndexDB.open(home)) {
            M3IndexDbArtifact artifact = db.putArtifact(NAME, "bytes", 1, PAYLOAD);
            Path blob = home.resolve("blobs/" + artifact.contentSha256() + ".blob");
            Files.delete(blob);
            assertThrows(IOException.class, () -> db.requireArtifact(NAME));
            Files.write(blob, PAYLOAD);
            byte[] corrupted = PAYLOAD.clone();
            corrupted[0] ^= 1;
            Files.write(blob, corrupted);
            assertThrows(IOException.class, () -> db.requireArtifact(NAME));
            assertThrows(IOException.class, () -> db.putArtifact("alias", "bytes", 1, PAYLOAD));
            assertFalse(db.containsArtifact("alias"));
            Files.write(blob, PAYLOAD);
            assertArrayEquals(PAYLOAD, db.requireArtifact(NAME).payload());
        }
    }

    @Test
    void invalidSemanticPayloadIsTranslatedWithItsCause() throws Exception {
        try (M3IndexDB db = M3IndexDB.open(home)) {
            db.putArtifact(NAME, M3IndexDbSemanticIndex.ARTIFACT_KIND,
                    M3IndexDbSemanticIndex.FORMAT_VERSION, new byte[] {1, 2, 3});
            IOException failure = assertThrows(IOException.class, () -> db.requireSemanticIndex(NAME));
            assertInstanceOf(IllegalArgumentException.class, failure.getCause());
            assertTrue(failure.getMessage().contains("payload"));
        }
    }

    @Test
    void failedReferencePublicationLeavesNoTemporaryFileOrFalseReference() throws Exception {
        try (M3IndexDB db = M3IndexDB.open(home)) {
            Files.createDirectories(ref());
            Files.writeString(ref().resolve("occupied-child"), "keep");
            assertThrows(IOException.class, () -> db.putArtifact(NAME, "bytes", 1, PAYLOAD));
            assertFalse(db.containsArtifact(NAME));
            try (var files = Files.walk(home)) {
                assertTrue(files.noneMatch(path -> path.toString().endsWith(".tmp")));
            }
            assertEquals("keep", Files.readString(ref().resolve("occupied-child")));
        }
    }

    @Test
    void artifactMetadataAndPayloadBoundariesDoNotLeakMutableArrays() {
        byte[] bytes = {1, 2};
        M3IndexDbArtifact value = M3IndexDbArtifact.create("n".repeat(512), "k".repeat(64), 1, bytes);
        bytes[0] = 9;
        byte[] copy = value.payload();
        copy[0] = 8;
        assertArrayEquals(new byte[] {1, 2}, value.payload());
        assertEquals(2L, value.payloadLength());
        for (String invalid : List.of("", "  ", "n".repeat(513), "a\0b")) {
            assertThrows(IllegalArgumentException.class,
                    () -> M3IndexDbArtifact.create(invalid, "kind", 1, new byte[0]));
        }
        for (String invalid : List.of("", " ", "k".repeat(65), "a\0b")) {
            assertThrows(IllegalArgumentException.class,
                    () -> M3IndexDbArtifact.create("name", invalid, 1, new byte[0]));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new M3IndexDbArtifact("name", "kind", 1, "A".repeat(64), new byte[0]));
        assertThrows(NullPointerException.class,
                () -> M3IndexDbArtifact.create(null, "kind", 1, new byte[0]));
        assertThrows(NullPointerException.class,
                () -> M3IndexDbArtifact.create("name", "kind", 1, null));
    }

    private Path ref() throws Exception {
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(NAME.getBytes(StandardCharsets.UTF_8)));
        return home.resolve("refs/" + hash + ".ref");
    }

    private record Field(int index, String value) {}
}
