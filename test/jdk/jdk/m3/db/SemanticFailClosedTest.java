// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

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

final class SemanticFailClosedTest {
    private static final long MAGIC = 0x4d3349444253454dL;

    @Test
    void fingerprintLeafComposeAndSimilarityCoverEmptyAndPopulatedPaths() {
        SemanticFingerprint empty =
                SemanticFingerprint.leaf("EMPTY", List.of(), List.of(), "");
        SemanticFingerprint populated =
                SemanticFingerprint.leaf(
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
                SemanticFingerprint.hammingDistance(
                        empty.simHash64(), populated.simHash64()));

        SemanticFingerprint noChildren =
                SemanticFingerprint.compose("PARENT", List.of());
        SemanticFingerprint composed =
                SemanticFingerprint.compose(
                        "PARENT",
                        List.of(
                                new SemanticFingerprint.Component("LEFT", empty),
                                new SemanticFingerprint.Component("RIGHT", populated)));
        SemanticFingerprint reversed =
                SemanticFingerprint.compose(
                        "PARENT",
                        List.of(
                                new SemanticFingerprint.Component("RIGHT", populated),
                                new SemanticFingerprint.Component("LEFT", empty)));

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
                () -> new SemanticFingerprint(
                        "bad", sha, sha, 0, 0, 0, "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticFingerprint(
                        sha, "bad", sha, 0, 0, 0, "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticFingerprint(
                        sha, sha, "bad", 0, 0, 0, "x"));
        assertThrows(
                NullPointerException.class,
                () -> new SemanticFingerprint(
                        sha, sha, sha, 0, 0, 0, null));

        assertThrows(
                NullPointerException.class,
                () -> SemanticFingerprint.leaf(
                        null, List.of(), List.of(), "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticFingerprint.leaf(
                        " ", List.of(), List.of(), "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticFingerprint.leaf(
                        "A\0B", List.of(), List.of(), "x"));
        assertThrows(
                NullPointerException.class,
                () -> SemanticFingerprint.leaf(
                        "D", null, List.of(), "x"));
        assertThrows(
                NullPointerException.class,
                () -> SemanticFingerprint.leaf(
                        "D", List.of(), null, "x"));
        assertThrows(
                NullPointerException.class,
                () -> SemanticFingerprint.leaf(
                        "D", List.of((String) null), List.of(), "x"));
        assertThrows(
                NullPointerException.class,
                () -> SemanticFingerprint.leaf(
                        "D", List.of(), List.of((String) null), "x"));
        assertThrows(
                NullPointerException.class,
                () -> SemanticFingerprint.leaf(
                        "D", List.of(), List.of(), null));

        assertThrows(
                NullPointerException.class,
                () -> SemanticFingerprint.compose(null, List.of()));
        assertThrows(
                NullPointerException.class,
                () -> SemanticFingerprint.compose("D", null));
        assertThrows(
                NullPointerException.class,
                () -> SemanticFingerprint.compose(
                        "D", java.util.Arrays.asList((SemanticFingerprint.Component) null)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticFingerprint.Component(
                        " ", fp("x")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticFingerprint.Component(
                        "A\0B", fp("x")));
        assertThrows(
                NullPointerException.class,
                () -> new SemanticFingerprint.Component(
                        "ROLE", null));
        assertThrows(
                NullPointerException.class,
                () -> SemanticFingerprint.utf16Sha256(null));
    }

    @Test
    void nodeAndEdgeRecordsRejectInvalidIdentityMetadataAndOrdering() {
        SemanticFingerprint fingerprint = fp("node");
        String nodeId =
                SemanticIndex.nodeId(
                        SemanticKind.ATOM, "node");

        assertThrows(
                NullPointerException.class,
                () -> SemanticIndex.nodeId(null, "node"));
        assertThrows(
                NullPointerException.class,
                () -> SemanticIndex.nodeId(
                        SemanticKind.ATOM, null));

        assertThrows(
                NullPointerException.class,
                () -> new SemanticNode(
                        null,
                        SemanticKind.ATOM,
                        "node",
                        "",
                        "symbol",
                        fingerprint));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticNode(
                        "bad",
                        SemanticKind.ATOM,
                        "node",
                        "",
                        "symbol",
                        fingerprint));
        assertThrows(
                NullPointerException.class,
                () -> new SemanticNode(
                        nodeId, null, "node", "", "symbol", fingerprint));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticNode(
                        nodeId,
                        SemanticKind.ATOM,
                        " ",
                        "",
                        "symbol",
                        fingerprint));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticNode(
                        nodeId,
                        SemanticKind.ATOM,
                        "A\0B",
                        "",
                        "symbol",
                        fingerprint));
        assertThrows(
                NullPointerException.class,
                () -> new SemanticNode(
                        nodeId,
                        SemanticKind.ATOM,
                        "node",
                        null,
                        "symbol",
                        fingerprint));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticNode(
                        nodeId,
                        SemanticKind.ATOM,
                        "node",
                        "",
                        " ",
                        fingerprint));
        assertThrows(
                NullPointerException.class,
                () -> new SemanticNode(
                        nodeId,
                        SemanticKind.ATOM,
                        "node",
                        "",
                        "symbol",
                        (SemanticFingerprint) null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticNode(
                        "1".repeat(64),
                        SemanticKind.ATOM,
                        "node",
                        "",
                        "symbol",
                        fingerprint));

        String a = SemanticIndex.nodeId(SemanticKind.ATOM, "a");
        String b = SemanticIndex.nodeId(SemanticKind.ATOM, "b");
        assertThrows(
                NullPointerException.class,
                () -> new SemanticEdge(null, b, "ROLE", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticEdge("bad", b, "ROLE", 0));
        assertThrows(
                NullPointerException.class,
                () -> new SemanticEdge(a, null, "ROLE", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticEdge(a, "bad", "ROLE", 0));
        assertThrows(
                NullPointerException.class,
                () -> new SemanticEdge(a, b, null, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticEdge(a, b, " ", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticEdge(a, b, "A\0B", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticEdge(a, b, "ROLE", -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticEdge(a, a, "ROLE", 0));
    }

    @Test
    void semanticIndexValidationCoversDuplicateDanglingCycleAndOrderBranches() {
        SemanticNode a = node(SemanticKind.ATOM, "a");
        SemanticNode b = node(SemanticKind.ATOM, "b");
        SemanticNode method = node(SemanticKind.METHOD, "method");

        assertThrows(
                NullPointerException.class,
                () -> SemanticIndex.of(null, List.of()));
        assertThrows(
                NullPointerException.class,
                () -> SemanticIndex.of(List.of(), null));
        assertThrows(
                NullPointerException.class,
                () -> SemanticIndex.of(
                        java.util.Arrays.asList((SemanticNode) null),
                        List.of()));
        assertThrows(
                NullPointerException.class,
                () -> SemanticIndex.of(
                        List.of(a),
                        java.util.Arrays.asList((SemanticEdge) null)));

        SemanticIndex duplicateEqual =
                SemanticIndex.of(List.of(a, a), List.of());
        assertEquals(1, duplicateEqual.nodes().size());

        SemanticNode conflicting =
                new SemanticNode(
                        a.nodeId(),
                        a.kind(),
                        a.semanticKey(),
                        "different.java",
                        a.symbol(),
                        a.patternRole(),
                        a.fingerprint());
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(
                        List.of(a, conflicting), List.of()));

        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(
                        List.of(a),
                        List.of(
                                new SemanticEdge(
                                        "f".repeat(64), a.nodeId(), "ROLE", 0))));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(
                        List.of(a),
                        List.of(
                                new SemanticEdge(
                                        a.nodeId(), "f".repeat(64), "ROLE", 0))));

        SemanticNode cycleA = node(SemanticKind.FILE, "cycle-a");
        SemanticNode cycleB = node(SemanticKind.FILE, "cycle-b");
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(
                        List.of(cycleA, cycleB),
                        List.of(
                                new SemanticEdge(
                                        cycleA.nodeId(), cycleB.nodeId(), "NEXT", 0),
                                new SemanticEdge(
                                        cycleB.nodeId(), cycleA.nodeId(), "NEXT", 0))));

        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(
                        List.of(method, a, b),
                        List.of(
                                new SemanticEdge(
                                        method.nodeId(), a.nodeId(), "ATOM", 0),
                                new SemanticEdge(
                                        method.nodeId(), b.nodeId(), "ATOM", 0))));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(
                        List.of(method, a, b),
                        List.of(
                                new SemanticEdge(
                                        method.nodeId(), a.nodeId(), "FIELD", 0),
                                new SemanticEdge(
                                        method.nodeId(), b.nodeId(), "FIELD", 0))));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(
                        List.of(method, a, b),
                        List.of(
                                new SemanticEdge(
                                        method.nodeId(), a.nodeId(), "ORDERED:ARG", 0),
                                new SemanticEdge(
                                        method.nodeId(), b.nodeId(), "ORDERED:ARG", 0))));

        SemanticIndex unordered =
                SemanticIndex.of(
                        List.of(method, a, b),
                        List.of(
                                new SemanticEdge(
                                        method.nodeId(), b.nodeId(), "METHOD", 99),
                                new SemanticEdge(
                                        method.nodeId(), a.nodeId(), "METHOD", 42),
                                new SemanticEdge(
                                        method.nodeId(), a.nodeId(), "METHOD", 42)));
        assertEquals(2, unordered.edges().size());
        assertEquals(List.of(a, b), unordered.children(method.nodeId()));
        // Parents are recomposed child-first; the original leaf fingerprint is not retained.
        SemanticNode expectedParent = method.withFingerprint(
                SemanticFingerprint.compose("METHOD:method", List.of(
                        new SemanticFingerprint.Component("METHOD", a.fingerprint()),
                        new SemanticFingerprint.Component("METHOD", b.fingerprint()))));
        assertNotEquals(method.fingerprint(), expectedParent.fingerprint());
        assertEquals(List.of(expectedParent), unordered.parents(a.nodeId()));
        assertEquals(List.of(expectedParent), unordered.parents(b.nodeId()));
        assertEquals(expectedParent, unordered.require(method.nodeId()));
    }

    @Test
    void querySurfaceIsDeterministicBoundedAndFailClosed() {
        SemanticNode a = node(SemanticKind.ATOM, "a");
        SemanticNode b = node(SemanticKind.METHOD, "b");
        SemanticIndex index =
                SemanticIndex.of(
                        List.of(a, b),
                        List.of(
                                new SemanticEdge(
                                        b.nodeId(), a.nodeId(), "ATOM", 0)));

        assertTrue(index.find("f".repeat(64)).isEmpty());
        assertEquals(a, index.require(a.nodeId()));
        assertThrows(
                IllegalArgumentException.class,
                () -> index.require("f".repeat(64)));
        assertThrows(NullPointerException.class, () -> index.find(null));
        assertThrows(NullPointerException.class, () -> index.nodesOfKind(null));
        assertEquals(List.of(a), index.nodesOfKind(SemanticKind.ATOM));

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
                () -> new SemanticIndex.Similarity(null, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticIndex.Similarity(a, -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticIndex.Similarity(
                        a, Long.SIZE + 1));
        assertEquals(
                0,
                new SemanticIndex.Similarity(a, 0)
                        .hammingDistance());
    }

    @Test
    void codecRejectsHeaderCountsLengthsKindsReferencesEdgesAndTrailingBytes()
            throws Exception {
        assertThrows(
                NullPointerException.class,
                () -> SemanticIndex.decode(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(header(2, 0, 0, 0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(header(1, -1, 0, 0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(
                        header(1, 50_000_001, 0, 0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(
                        stringHeader(-1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(
                        stringHeader(16 * 1024 * 1024 + 1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(
                        stringHeader(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(
                        oneNodePayload(255, 0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(
                        oneNodePayload(0, 1)));

        SemanticNode a = node(SemanticKind.ATOM, "codec-a");
        SemanticNode b = node(SemanticKind.METHOD, "codec-b");
        byte[] valid =
                SemanticIndex.of(
                                List.of(a, b),
                                List.of(
                                        new SemanticEdge(
                                                b.nodeId(), a.nodeId(), "ATOM", 0)))
                        .encode();
        assertArrayEquals(
                valid,
                SemanticIndex.decode(valid).encode());

        byte[] badParent = valid.clone();
        putInt(badParent, valid.length - 16, 2);
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(badParent));

        byte[] badChild = valid.clone();
        putInt(badChild, valid.length - 12, 2);
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(badChild));

        byte[] badRole = valid.clone();
        putInt(badRole, valid.length - 8, Integer.MAX_VALUE);
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(badRole));

        byte[] negativeProducer = valid.clone();
        putInt(negativeProducer, valid.length - 4, -1);
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(negativeProducer));

        byte[] trailing = java.util.Arrays.copyOf(valid, valid.length + 1);
        trailing[trailing.length - 1] = 1;
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(trailing));

        byte[] truncated = java.util.Arrays.copyOf(valid, 30);
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(truncated));
    }

    @Test
    void storeLoadNullAndEmptyContractsAreExplicit() throws Exception {
        SemanticIndex empty =
                SemanticIndex.of(List.of(), List.of());
        assertEquals(0, empty.nodes().size());
        assertEquals(0, empty.edges().size());
        assertThrows(NullPointerException.class, () -> empty.store(null, "x"));
        assertThrows(
                NullPointerException.class,
                () -> SemanticIndex.load(null, "x"));

        Path root = Files.createTempDirectory("m3indexdb-semantic-store-");
        try (M3DB db = M3DB.open(root)) {
            Artifact artifact = empty.store(db, "empty");
            assertEquals(0, SemanticIndex.load(db, "empty").nodes().size());
            assertEquals(SemanticIndex.ARTIFACT_KIND, artifact.kind());
        }
    }

    private static SemanticFingerprint fp(String value) {
        return SemanticFingerprint.leaf(
                "TEST",
                List.of("S:" + value),
                List.of("L:" + value),
                value);
    }

    private static SemanticNode node(
            SemanticKind kind, String key) {
        return new SemanticNode(
                SemanticIndex.nodeId(kind, key),
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
                    SemanticIndex.nodeId(
                            SemanticKind.ATOM, "x"));
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
