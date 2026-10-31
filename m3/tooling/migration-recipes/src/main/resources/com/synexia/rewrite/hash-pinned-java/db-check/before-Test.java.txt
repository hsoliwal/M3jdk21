// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3IndexDbSemanticFailClosedTest {
    private static final long MAGIC = 0x4d3349444253454dL;

    @Test
    void fingerprintLeafComposeAndSimilarityCoverEmptyAndPopulatedPaths() {
        M3IndexDbSemanticFingerprint empty =
                M3IndexDbSemanticFingerprint.leaf("EMPTY", List.of(), List.of(), "");
        M3IndexDbSemanticFingerprint populated =
                M3IndexDbSemanticFingerprint.leaf(
                        "ATOM",
                        List.of("Return", "Literal"),
                        List.of("RETURN", "CONST:1"),
                        "return 1;");

        assertEquals(64, empty.exactSha256().length());
        assertEquals(16, empty.structuralHash64Hex().length());
        assertEquals(16, empty.logicHash64Hex().length());
        assertEquals(16, empty.simHash64Hex().length());
        assertEquals(
                Long.bitCount(empty.simHash64() ^ populated.simHash64()),
                M3IndexDbSemanticFingerprint.hammingDistance(
                        empty.simHash64(), populated.simHash64()));

        M3IndexDbSemanticFingerprint noChildren =
                M3IndexDbSemanticFingerprint.compose("PARENT", List.of());
        M3IndexDbSemanticFingerprint composed =
                M3IndexDbSemanticFingerprint.compose(
                        "PARENT",
                        List.of(
                                new M3IndexDbSemanticFingerprint.Component("LEFT", empty),
                                new M3IndexDbSemanticFingerprint.Component("RIGHT", populated)));
        M3IndexDbSemanticFingerprint reversed =
                M3IndexDbSemanticFingerprint.compose(
                        "PARENT",
                        List.of(
                                new M3IndexDbSemanticFingerprint.Component("RIGHT", populated),
                                new M3IndexDbSemanticFingerprint.Component("LEFT", empty)));

        assertNotEquals(noChildren.logicSha256(), composed.logicSha256());
        assertNotEquals(composed.logicSha256(), reversed.logicSha256());
        assertNotEquals(composed.structuralSha256(), reversed.structuralSha256());
        assertTrue(composed.normalizedComposition().contains("LEFT#0"));
    }

    @Test
    void fingerprintValidationRejectsEveryPublicInvalidShape() {
        String sha = "0".repeat(64);
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticFingerprint(
                        "bad", sha, sha, 0, 0, 0, "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticFingerprint(
                        sha, "bad", sha, 0, 0, 0, "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticFingerprint(
                        sha, sha, "bad", 0, 0, 0, "x"));
        assertThrows(
                NullPointerException.class,
                () -> new M3IndexDbSemanticFingerprint(
                        sha, sha, sha, 0, 0, 0, null));

        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticFingerprint.leaf(
                        null, List.of(), List.of(), "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticFingerprint.leaf(
                        " ", List.of(), List.of(), "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticFingerprint.leaf(
                        "A\0B", List.of(), List.of(), "x"));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticFingerprint.leaf(
                        "D", null, List.of(), "x"));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticFingerprint.leaf(
                        "D", List.of(), null, "x"));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticFingerprint.leaf(
                        "D", List.of((String) null), List.of(), "x"));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticFingerprint.leaf(
                        "D", List.of(), List.of((String) null), "x"));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticFingerprint.leaf(
                        "D", List.of(), List.of(), null));

        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticFingerprint.compose(null, List.of()));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticFingerprint.compose("D", null));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticFingerprint.compose(
                        "D", java.util.Arrays.asList((M3IndexDbSemanticFingerprint.Component) null)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticFingerprint.Component(
                        " ", fp("x")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticFingerprint.Component(
                        "A\0B", fp("x")));
        assertThrows(
                NullPointerException.class,
                () -> new M3IndexDbSemanticFingerprint.Component(
                        "ROLE", null));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticFingerprint.utf16Sha256(null));
    }

    @Test
    void nodeAndEdgeRecordsRejectInvalidIdentityMetadataAndOrdering() {
        M3IndexDbSemanticFingerprint fingerprint = fp("node");
        String nodeId =
                M3IndexDbSemanticIndex.nodeId(
                        M3IndexDbSemanticKind.ATOM, "node");

        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticIndex.nodeId(null, "node"));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticIndex.nodeId(
                        M3IndexDbSemanticKind.ATOM, null));

        assertThrows(
                NullPointerException.class,
                () -> new M3IndexDbSemanticNode(
                        null,
                        M3IndexDbSemanticKind.ATOM,
                        "node",
                        "",
                        "symbol",
                        fingerprint));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticNode(
                        "bad",
                        M3IndexDbSemanticKind.ATOM,
                        "node",
                        "",
                        "symbol",
                        fingerprint));
        assertThrows(
                NullPointerException.class,
                () -> new M3IndexDbSemanticNode(
                        nodeId, null, "node", "", "symbol", fingerprint));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticNode(
                        nodeId,
                        M3IndexDbSemanticKind.ATOM,
                        " ",
                        "",
                        "symbol",
                        fingerprint));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticNode(
                        nodeId,
                        M3IndexDbSemanticKind.ATOM,
                        "A\0B",
                        "",
                        "symbol",
                        fingerprint));
        assertThrows(
                NullPointerException.class,
                () -> new M3IndexDbSemanticNode(
                        nodeId,
                        M3IndexDbSemanticKind.ATOM,
                        "node",
                        null,
                        "symbol",
                        fingerprint));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticNode(
                        nodeId,
                        M3IndexDbSemanticKind.ATOM,
                        "node",
                        "",
                        " ",
                        fingerprint));
        assertThrows(
                NullPointerException.class,
                () -> new M3IndexDbSemanticNode(
                        nodeId,
                        M3IndexDbSemanticKind.ATOM,
                        "node",
                        "",
                        "symbol",
                        (M3IndexDbSemanticFingerprint) null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticNode(
                        "1".repeat(64),
                        M3IndexDbSemanticKind.ATOM,
                        "node",
                        "",
                        "symbol",
                        fingerprint));

        String a = M3IndexDbSemanticIndex.nodeId(M3IndexDbSemanticKind.ATOM, "a");
        String b = M3IndexDbSemanticIndex.nodeId(M3IndexDbSemanticKind.ATOM, "b");
        assertThrows(
                NullPointerException.class,
                () -> new M3IndexDbSemanticEdge(null, b, "ROLE", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticEdge("bad", b, "ROLE", 0));
        assertThrows(
                NullPointerException.class,
                () -> new M3IndexDbSemanticEdge(a, null, "ROLE", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticEdge(a, "bad", "ROLE", 0));
        assertThrows(
                NullPointerException.class,
                () -> new M3IndexDbSemanticEdge(a, b, null, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticEdge(a, b, " ", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticEdge(a, b, "A\0B", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticEdge(a, b, "ROLE", -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticEdge(a, a, "ROLE", 0));
    }

    @Test
    void semanticIndexValidationCoversDuplicateDanglingCycleAndOrderBranches() {
        M3IndexDbSemanticNode a = node(M3IndexDbSemanticKind.ATOM, "a");
        M3IndexDbSemanticNode b = node(M3IndexDbSemanticKind.ATOM, "b");
        M3IndexDbSemanticNode method = node(M3IndexDbSemanticKind.METHOD, "method");

        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticIndex.of(null, List.of()));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticIndex.of(List.of(), null));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticIndex.of(
                        java.util.Arrays.asList((M3IndexDbSemanticNode) null),
                        List.of()));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticIndex.of(
                        List.of(a),
                        java.util.Arrays.asList((M3IndexDbSemanticEdge) null)));

        M3IndexDbSemanticIndex duplicateEqual =
                M3IndexDbSemanticIndex.of(List.of(a, a), List.of());
        assertEquals(1, duplicateEqual.nodes().size());

        M3IndexDbSemanticNode conflicting =
                new M3IndexDbSemanticNode(
                        a.nodeId(),
                        a.kind(),
                        a.semanticKey(),
                        "different.java",
                        a.symbol(),
                        a.patternRole(),
                        a.fingerprint());
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(
                        List.of(a, conflicting), List.of()));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(
                        List.of(a),
                        List.of(
                                new M3IndexDbSemanticEdge(
                                        "f".repeat(64), a.nodeId(), "ROLE", 0))));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(
                        List.of(a),
                        List.of(
                                new M3IndexDbSemanticEdge(
                                        a.nodeId(), "f".repeat(64), "ROLE", 0))));

        M3IndexDbSemanticNode cycleA = node(M3IndexDbSemanticKind.FILE, "cycle-a");
        M3IndexDbSemanticNode cycleB = node(M3IndexDbSemanticKind.FILE, "cycle-b");
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(
                        List.of(cycleA, cycleB),
                        List.of(
                                new M3IndexDbSemanticEdge(
                                        cycleA.nodeId(), cycleB.nodeId(), "NEXT", 0),
                                new M3IndexDbSemanticEdge(
                                        cycleB.nodeId(), cycleA.nodeId(), "NEXT", 0))));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(
                        List.of(method, a, b),
                        List.of(
                                new M3IndexDbSemanticEdge(
                                        method.nodeId(), a.nodeId(), "ATOM", 0),
                                new M3IndexDbSemanticEdge(
                                        method.nodeId(), b.nodeId(), "ATOM", 0))));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(
                        List.of(method, a, b),
                        List.of(
                                new M3IndexDbSemanticEdge(
                                        method.nodeId(), a.nodeId(), "FIELD", 0),
                                new M3IndexDbSemanticEdge(
                                        method.nodeId(), b.nodeId(), "FIELD", 0))));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(
                        List.of(method, a, b),
                        List.of(
                                new M3IndexDbSemanticEdge(
                                        method.nodeId(), a.nodeId(), "ORDERED:ARG", 0),
                                new M3IndexDbSemanticEdge(
                                        method.nodeId(), b.nodeId(), "ORDERED:ARG", 0))));

        M3IndexDbSemanticIndex unordered =
                M3IndexDbSemanticIndex.of(
                        List.of(method, a, b),
                        List.of(
                                new M3IndexDbSemanticEdge(
                                        method.nodeId(), b.nodeId(), "METHOD", 99),
                                new M3IndexDbSemanticEdge(
                                        method.nodeId(), a.nodeId(), "METHOD", 42),
                                new M3IndexDbSemanticEdge(
                                        method.nodeId(), a.nodeId(), "METHOD", 42)));
        assertEquals(2, unordered.edges().size());
        assertEquals(List.of(a, b), unordered.children(method.nodeId()));
        assertEquals(List.of(method), unordered.parents(a.nodeId()));
    }

    @Test
    void querySurfaceIsDeterministicBoundedAndFailClosed() {
        M3IndexDbSemanticNode a = node(M3IndexDbSemanticKind.ATOM, "a");
        M3IndexDbSemanticNode b = node(M3IndexDbSemanticKind.METHOD, "b");
        M3IndexDbSemanticIndex index =
                M3IndexDbSemanticIndex.of(
                        List.of(a, b),
                        List.of(
                                new M3IndexDbSemanticEdge(
                                        b.nodeId(), a.nodeId(), "ATOM", 0)));

        assertTrue(index.find("f".repeat(64)).isEmpty());
        assertEquals(a, index.require(a.nodeId()));
        assertThrows(
                IllegalArgumentException.class,
                () -> index.require("f".repeat(64)));
        assertThrows(NullPointerException.class, () -> index.find(null));
        assertThrows(NullPointerException.class, () -> index.nodesOfKind(null));
        assertEquals(List.of(a), index.nodesOfKind(M3IndexDbSemanticKind.ATOM));

        assertEquals(List.of(a), index.exactSha256(a.fingerprint().exactSha256()));
        assertEquals(
                List.of(a),
                index.structuralSha256(a.fingerprint().structuralSha256()));
        assertEquals(List.of(a), index.logicSha256(a.fingerprint().logicSha256()));
        assertTrue(index.exactSha256("f".repeat(64)).isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () -> index.structuralSha256("bad"));
        assertThrows(
                IllegalArgumentException.class,
                () -> index.logicSha256("A".repeat(64)));

        assertEquals(List.of(a), index.patternRole(a.patternRole()));
        assertTrue(index.patternRole("missing").isEmpty());
        assertThrows(NullPointerException.class, () -> index.patternRole(null));
        assertThrows(IllegalArgumentException.class, () -> index.patternRole(" "));

        assertEquals(
                List.of(a),
                index.structuralHash64(a.fingerprint().structuralHash64()));
        assertEquals(List.of(a), index.logicHash64(a.fingerprint().logicHash64()));
        assertTrue(index.structuralHash64(Long.MIN_VALUE).isEmpty());

        assertFalse(index.nearSimHash(a.fingerprint().simHash64(), 0).isEmpty());
        assertEquals(2, index.nearSimHash(0L, Long.SIZE).size());
        assertThrows(
                IllegalArgumentException.class,
                () -> index.nearSimHash(0L, -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> index.nearSimHash(0L, Long.SIZE + 1));

        assertTrue(index.children("f".repeat(64)).isEmpty());
        assertTrue(index.parents("f".repeat(64)).isEmpty());
        assertThrows(NullPointerException.class, () -> index.children(null));
        assertThrows(NullPointerException.class, () -> index.parents(null));

        assertThrows(
                NullPointerException.class,
                () -> new M3IndexDbSemanticIndex.Similarity(null, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticIndex.Similarity(a, -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticIndex.Similarity(
                        a, Long.SIZE + 1));
        assertEquals(
                0,
                new M3IndexDbSemanticIndex.Similarity(a, 0)
                        .hammingDistance());
    }

    @Test
    void codecRejectsHeaderCountsLengthsKindsReferencesEdgesAndTrailingBytes()
            throws Exception {
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticIndex.decode(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(header(2, 0, 0, 0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(header(1, -1, 0, 0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(
                        header(1, 50_000_001, 0, 0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(
                        stringHeader(-1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(
                        stringHeader(16 * 1024 * 1024 + 1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(
                        stringHeader(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(
                        oneNodePayload(255, 0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(
                        oneNodePayload(0, 1)));

        M3IndexDbSemanticNode a = node(M3IndexDbSemanticKind.ATOM, "codec-a");
        M3IndexDbSemanticNode b = node(M3IndexDbSemanticKind.METHOD, "codec-b");
        byte[] valid =
                M3IndexDbSemanticIndex.of(
                                List.of(a, b),
                                List.of(
                                        new M3IndexDbSemanticEdge(
                                                b.nodeId(), a.nodeId(), "ATOM", 0)))
                        .encode();
        assertArrayEquals(
                valid,
                M3IndexDbSemanticIndex.decode(valid).encode());

        byte[] badParent = valid.clone();
        putInt(badParent, valid.length - 16, 2);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(badParent));

        byte[] badChild = valid.clone();
        putInt(badChild, valid.length - 12, 2);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(badChild));

        byte[] badRole = valid.clone();
        putInt(badRole, valid.length - 8, Integer.MAX_VALUE);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(badRole));

        byte[] negativeProducer = valid.clone();
        putInt(negativeProducer, valid.length - 4, -1);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(negativeProducer));

        byte[] trailing = java.util.Arrays.copyOf(valid, valid.length + 1);
        trailing[trailing.length - 1] = 1;
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(trailing));

        byte[] truncated = java.util.Arrays.copyOf(valid, 30);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(truncated));
    }

    @Test
    void storeLoadNullAndEmptyContractsAreExplicit() throws Exception {
        M3IndexDbSemanticIndex empty =
                M3IndexDbSemanticIndex.of(List.of(), List.of());
        assertEquals(0, empty.nodes().size());
        assertEquals(0, empty.edges().size());
        assertThrows(NullPointerException.class, () -> empty.store(null, "x"));
        assertThrows(
                NullPointerException.class,
                () -> M3IndexDbSemanticIndex.load(null, "x"));

        Path root = Files.createTempDirectory("m3indexdb-semantic-store-");
        try (M3IndexDB db = M3IndexDB.open(root)) {
            M3IndexDbArtifact artifact = empty.store(db, "empty");
            assertEquals(0, M3IndexDbSemanticIndex.load(db, "empty").nodes().size());
            assertEquals(M3IndexDbSemanticIndex.ARTIFACT_KIND, artifact.kind());
        }
    }

    private static M3IndexDbSemanticFingerprint fp(String value) {
        return M3IndexDbSemanticFingerprint.leaf(
                "TEST",
                List.of("S:" + value),
                List.of("L:" + value),
                value);
    }

    private static M3IndexDbSemanticNode node(
            M3IndexDbSemanticKind kind, String key) {
        return new M3IndexDbSemanticNode(
                M3IndexDbSemanticIndex.nodeId(kind, key),
                kind,
                key,
                "src/" + key + ".java",
                key,
                "M3:" + kind,
                fp(key));
    }

    private static byte[] header(
            int version, int stringCount, int nodeCount, int edgeCount)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeLong(MAGIC);
            out.writeInt(version);
            out.writeInt(stringCount);
            out.writeInt(nodeCount);
            out.writeInt(edgeCount);
        }
        return bytes.toByteArray();
    }

    private static byte[] stringHeader(int length) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.write(header(1, 1, 0, 0));
            out.writeInt(length);
        }
        return bytes.toByteArray();
    }

    private static byte[] oneNodePayload(int kindOrdinal, int semanticKeyId)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeLong(MAGIC);
            out.writeInt(1);
            out.writeInt(1);
            out.writeInt(1);
            out.writeInt(0);
            out.writeInt(1);
            out.writeByte('x');

            writeSha(
                    out,
                    M3IndexDbSemanticIndex.nodeId(
                            M3IndexDbSemanticKind.ATOM, "x"));
            out.writeByte(kindOrdinal);
            out.writeInt(semanticKeyId);
            out.writeInt(0);
            out.writeInt(0);
            out.writeInt(0);
            writeSha(out, "0".repeat(64));
            writeSha(out, "0".repeat(64));
            writeSha(out, "0".repeat(64));
            out.writeLong(0L);
            out.writeLong(0L);
            out.writeLong(0L);
            out.writeInt(0);
        }
        return bytes.toByteArray();
    }

    private static void writeSha(DataOutputStream out, String value)
            throws IOException {
        out.write(java.util.HexFormat.of().parseHex(value));
    }

    private static void putInt(byte[] target, int offset, int value) {
        target[offset] = (byte) (value >>> 24);
        target[offset + 1] = (byte) (value >>> 16);
        target[offset + 2] = (byte) (value >>> 8);
        target[offset + 3] = (byte) value;
    }
}
