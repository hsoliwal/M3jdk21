// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * ContractProbe for the actual persisted semantic binary format.
 * This decoder test is not a Java source parser or a second serialization engine.
 */
final class CodecBoundaryTest {
    @Test
    void emptySnapshotAndEveryTruncatedPrefixHaveDefinedOutcomes() {
        byte[] empty = SemanticIndex.of(List.of(), List.of()).encode();
        assertArrayEquals(empty, SemanticIndex.decode(empty).encode());
        byte[] encoded = snapshot().encode();
        for (int length = 0; length < encoded.length; length++) {
            byte[] prefix = Arrays.copyOf(encoded, length);
            assertThrows(IllegalArgumentException.class, () -> SemanticIndex.decode(prefix),
                    "truncated prefix=" + length);
        }
        assertThrows(NullPointerException.class, () -> SemanticIndex.decode(null));
        assertThrows(NullPointerException.class, () -> SemanticCodec.encode(null));
    }

    @Test
    void versionCountsLengthsAndTrailingDataAreRejected() {
        byte[] valid = snapshot().encode();
        invalidInt(valid, 8, 2);
        for (int position : new int[] {12, 16, 20}) {
            invalidInt(valid, position, -1);
            invalidInt(valid, position, 50_000_001);
        }
        invalidInt(valid, 24, -1);
        invalidInt(valid, 24, 16 * 1024 * 1024 + 1);
        invalidInt(valid, 24, valid.length + 1);
        byte[] trailing = Arrays.copyOf(valid, valid.length + 1);
        assertThrows(IllegalArgumentException.class, () -> SemanticIndex.decode(trailing));
    }

    @Test
    void nodeKindsAndStringOrdinalsAreRangeChecked() {
        byte[] valid = snapshot().encode();
        int nodeStart = nodeStart(valid);
        byte[] kind = valid.clone();
        kind[nodeStart + 32] = (byte) 255;
        assertThrows(IllegalArgumentException.class, () -> SemanticIndex.decode(kind));
        int stringCount = ByteBuffer.wrap(valid).getInt(12);
        for (int position : new int[] {nodeStart + 33, nodeStart + 37,
                nodeStart + 41, nodeStart + 45, nodeStart + 169}) {
            invalidInt(valid, position, -1);
            invalidInt(valid, position, stringCount);
        }
        byte[] identity = valid.clone();
        identity[nodeStart] ^= 1;
        assertThrows(IllegalArgumentException.class, () -> SemanticIndex.decode(identity));
    }

    @Test
    void edgeOrdinalsRolesAndOrderingAreValidated() {
        byte[] valid = snapshot().encode();
        int nodeCount = ByteBuffer.wrap(valid).getInt(16);
        int edgeStart = nodeStart(valid) + nodeCount * 173;
        assertEquals(1, ByteBuffer.wrap(valid).getInt(20));
        for (int position : new int[] {edgeStart, edgeStart + 4}) {
            invalidInt(valid, position, -1);
            invalidInt(valid, position, nodeCount);
        }
        invalidInt(valid, edgeStart + 8, -1);
        invalidInt(valid, edgeStart + 8, ByteBuffer.wrap(valid).getInt(12));
        invalidInt(valid, edgeStart + 12, -1);
        int parent = ByteBuffer.wrap(valid).getInt(edgeStart);
        invalidInt(valid, edgeStart + 4, parent);
        assertArrayEquals(valid, SemanticIndex.decode(valid).encode());
    }

    private static void invalidInt(byte[] source, int position, int value) {
        byte[] candidate = source.clone();
        ByteBuffer.wrap(candidate).putInt(position, value);
        assertThrows(IllegalArgumentException.class, () -> SemanticIndex.decode(candidate),
                "offset=" + position + " value=" + value);
    }

    private static int nodeStart(byte[] encoded) {
        ByteBuffer view = ByteBuffer.wrap(encoded);
        int strings = view.getInt(12);
        view.position(24);
        for (int index = 0; index < strings; index++) {
            int length = view.getInt();
            view.position(view.position() + length);
        }
        return view.position();
    }

    private static SemanticIndex snapshot() {
        var child = node("codec/child");
        var parent = node("codec/parent");
        return SemanticIndex.of(List.of(child, parent), List.of(
                new SemanticEdge(parent.nodeId(), child.nodeId(), "ORDERED:CHILD", 0)));
    }

    private static SemanticNode node(String key) {
        var kind = SemanticKind.ATOM;
        return new SemanticNode(SemanticIndex.nodeId(kind, key), kind,
                key, "src/Codec.java", key, "ContractProbe",
                SemanticFingerprint.leaf("ATOM", List.of("node"), List.of(key), key));
    }
}
