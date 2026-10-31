// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3IndexDBIntegrityTest {
    @Test
    void artifactMetadataValidationCoversEveryFailClosedBranch() {
        byte[] payload = {1, 2, 3};
        String hash = M3IndexDbArtifact.create("ok", "kind", 1, payload).contentSha256();

        assertThrows(NullPointerException.class, () -> M3IndexDbArtifact.create(null, "kind", 1, payload));
        assertThrows(IllegalArgumentException.class, () -> M3IndexDbArtifact.create(" ", "kind", 1, payload));
        assertThrows(IllegalArgumentException.class, () -> M3IndexDbArtifact.create("x".repeat(513), "kind", 1, payload));
        assertThrows(IllegalArgumentException.class, () -> M3IndexDbArtifact.create("a\0b", "kind", 1, payload));
        assertThrows(NullPointerException.class, () -> M3IndexDbArtifact.create("ok", null, 1, payload));
        assertThrows(IllegalArgumentException.class, () -> M3IndexDbArtifact.create("ok", " ", 1, payload));
        assertThrows(IllegalArgumentException.class, () -> M3IndexDbArtifact.create("ok", "x".repeat(65), 1, payload));
        assertThrows(IllegalArgumentException.class, () -> M3IndexDbArtifact.create("ok", "a\0b", 1, payload));
        assertThrows(IllegalArgumentException.class, () -> M3IndexDbArtifact.create("ok", "kind", -1, payload));
        assertThrows(NullPointerException.class, () -> M3IndexDbArtifact.create("ok", "kind", 1, null));
        assertThrows(NullPointerException.class, () -> new M3IndexDbArtifact("ok", "kind", 1, null, payload));
        assertThrows(IllegalArgumentException.class, () -> new M3IndexDbArtifact("ok", "kind", 1, "A".repeat(64), payload));
        assertThrows(NullPointerException.class, () -> new M3IndexDbArtifact("ok", "kind", 1, hash, null));

        M3IndexDbArtifact artifact = new M3IndexDbArtifact("ok", "kind", 1, hash, payload);
        assertEquals(3L, artifact.payloadLength());
        assertArrayEquals(payload, artifact.payload());
    }

    @Test
    void rootContainsAndAllClosedOperationsHaveExplicitContracts() throws Exception {
        Path root = Files.createTempDirectory("m3indexdb-integrity-close-");
        M3IndexDB db = M3IndexDB.open(root);
        assertEquals(root.toAbsolutePath().normalize(), db.root());
        assertFalse(db.containsArtifact("missing"));
        assertEquals(0L, db.blobBytes());

        db.close();

        assertThrows(IllegalStateException.class, db::root);
        assertThrows(IllegalStateException.class, () -> db.containsArtifact("x"));
        assertThrows(IllegalStateException.class, () -> db.requireArtifact("x"));
        assertThrows(IllegalStateException.class, db::blobBytes);
        assertThrows(IllegalStateException.class, () -> db.putArtifact("x", "k", 1, new byte[] {1}));
        assertThrows(IllegalStateException.class, () -> db.putSemanticIndex("x", emptyIndex()));
        assertThrows(IllegalStateException.class, () -> db.requireSemanticIndex("x"));
    }

    @Test
    void existingBlobIntegrityIsCheckedBeforeRefPublication() throws Exception {
        Path root = Files.createTempDirectory("m3indexdb-corrupt-existing-");
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
        try (M3IndexDB db = M3IndexDB.open(root)) {
            M3IndexDbArtifact artifact = db.putArtifact("first", "kind", 1, payload);
            Path blob = only(root.resolve("blobs"));
            assertTrue(blob.getFileName().toString().startsWith(artifact.contentSha256()));
            byte[] corrupt = payload.clone();
            corrupt[0] ^= 1;
            Files.write(blob, corrupt);

            assertThrows(IOException.class, () -> db.putArtifact("second", "kind", 1, payload));
            assertFalse(db.containsArtifact("second"));
        }
    }

    @Test
    void refStructureMagicAndNameAreIndependentlyValidated() throws Exception {
        assertRefFailure(List.of("too", "short"), "short");

        List<String> valid = validRefLines("name");
        List<String> badMagic = new java.util.ArrayList<>(valid);
        badMagic.set(0, "BAD");
        assertRefFailure(badMagic, "name");

        List<String> badName = new java.util.ArrayList<>(valid);
        badName.set(1, "other");
        assertRefFailure(badName, "name");
    }

    @Test
    void refNumericHashBlobAndMetadataFailuresAreIndependent() throws Exception {
        List<String> valid = validRefLines("name");

        List<String> badFormat = new java.util.ArrayList<>(valid);
        badFormat.set(3, "not-int");
        assertRefFailure(badFormat, "name");

        List<String> badLength = new java.util.ArrayList<>(valid);
        badLength.set(5, "not-long");
        assertRefFailure(badLength, "name");

        List<String> badHash = new java.util.ArrayList<>(valid);
        badHash.set(4, "not-sha");
        assertRefFailure(badHash, "name");

        Path missingRoot = Files.createTempDirectory("m3indexdb-ref-missing-");
        try (M3IndexDB db = M3IndexDB.open(missingRoot)) {
            db.putArtifact("name", "kind", 1, new byte[] {1, 2, 3});
            Path ref = only(missingRoot.resolve("refs"));
            Path blob = only(missingRoot.resolve("blobs"));
            Files.delete(blob);
            assertThrows(IOException.class, () -> db.requireArtifact("name"));
            assertTrue(Files.isRegularFile(ref));
        }

        Path lengthRoot = Files.createTempDirectory("m3indexdb-ref-length-");
        try (M3IndexDB db = M3IndexDB.open(lengthRoot)) {
            db.putArtifact("name", "kind", 1, new byte[] {1, 2, 3});
            Path ref = only(lengthRoot.resolve("refs"));
            List<String> lines = Files.readAllLines(ref, StandardCharsets.UTF_8);
            lines.set(5, "4");
            Files.write(ref, lines, StandardCharsets.UTF_8);
            assertThrows(IOException.class, () -> db.requireArtifact("name"));
        }

        Path hashRoot = Files.createTempDirectory("m3indexdb-ref-content-");
        try (M3IndexDB db = M3IndexDB.open(hashRoot)) {
            db.putArtifact("name", "kind", 1, new byte[] {1, 2, 3});
            Path blob = only(hashRoot.resolve("blobs"));
            Files.write(blob, new byte[] {3, 2, 1});
            assertThrows(IOException.class, () -> db.requireArtifact("name"));
        }

        Path metadataRoot = Files.createTempDirectory("m3indexdb-ref-metadata-");
        try (M3IndexDB db = M3IndexDB.open(metadataRoot)) {
            db.putArtifact("name", "kind", 1, new byte[] {1, 2, 3});
            Path ref = only(metadataRoot.resolve("refs"));
            List<String> lines = Files.readAllLines(ref, StandardCharsets.UTF_8);
            lines.set(2, " ");
            Files.write(ref, lines, StandardCharsets.UTF_8);
            assertThrows(IOException.class, () -> db.requireArtifact("name"));
        }
    }

    @Test
    void semanticIndexKindVersionPayloadAndNullContractsFailClosed() throws Exception {
        Path root = Files.createTempDirectory("m3indexdb-semantic-invalid-");
        try (M3IndexDB db = M3IndexDB.open(root)) {
            assertThrows(NullPointerException.class, () -> db.putSemanticIndex("null", null));

            db.putArtifact("wrong-kind", "other", 1, new byte[] {1});
            assertThrows(IOException.class, () -> db.requireSemanticIndex("wrong-kind"));

            db.putArtifact("wrong-version", M3IndexDbSemanticIndex.ARTIFACT_KIND, 2, new byte[] {1});
            assertThrows(IOException.class, () -> db.requireSemanticIndex("wrong-version"));

            db.putArtifact("bad-payload", M3IndexDbSemanticIndex.ARTIFACT_KIND, 1, new byte[] {1, 2, 3});
            IOException failure = assertThrows(IOException.class, () -> db.requireSemanticIndex("bad-payload"));
            assertTrue(failure.getCause() instanceof IllegalArgumentException);

            M3IndexDbSemanticIndex empty = emptyIndex();
            M3IndexDbArtifact stored = db.putSemanticIndex("empty", empty);
            assertEquals(M3IndexDbSemanticIndex.ARTIFACT_KIND, stored.kind());
            assertEquals(empty.nodes(), db.requireSemanticIndex("empty").nodes());
        }
    }

    private static M3IndexDbSemanticIndex emptyIndex() {
        return M3IndexDbSemanticIndex.of(List.of(), List.of());
    }

    private static Path only(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream.findFirst().orElseThrow();
        }
    }

    private static List<String> validRefLines(String name) throws Exception {
        Path root = Files.createTempDirectory("m3indexdb-valid-ref-");
        try (M3IndexDB db = M3IndexDB.open(root)) {
            db.putArtifact(name, "kind", 1, new byte[] {1, 2, 3});
            return Files.readAllLines(only(root.resolve("refs")), StandardCharsets.UTF_8);
        }
    }

    private static void assertRefFailure(List<String> lines, String name) throws Exception {
        Path root = Files.createTempDirectory("m3indexdb-ref-failure-");
        try (M3IndexDB db = M3IndexDB.open(root)) {
            db.putArtifact(name, "kind", 1, new byte[] {1, 2, 3});
            Path ref = only(root.resolve("refs"));
            Files.write(ref, lines, StandardCharsets.UTF_8);
            assertThrows(IOException.class, () -> db.requireArtifact(name));
        }
    }
}
