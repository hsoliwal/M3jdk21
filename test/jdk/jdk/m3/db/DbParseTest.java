// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** ContractProbe for the existing DB wire decoder, not a Java source parser. */
final class DbParseTest {
    @TempDir Path home;

    @Test
    void claimedCountsCannotExhaustBoundedHeap() throws Exception {
        for (int column = 0; column < 3; column++) {
            String operation = "count-" + column;
            String classpath = System.getProperty("surefire.test.class.path",
                    System.getProperty("java.class.path"));
            Process child = new ProcessBuilder(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-Xmx32m", "-cp", classpath, DbProbe.class.getName(), operation)
                    .redirectErrorStream(true).start();
            try {
                assertTrue(child.waitFor(30, TimeUnit.SECONDS), "bounded probe timeout");
                String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertEquals(0, child.exitValue(), output);
                assertTrue(output.contains("DB_PARSE_PASS " + operation), output);
            } finally {
                child.destroyForcibly();
            }
        }
    }

    @Test
    void eachTableMinimumAndCombinedCountsAreCheckedBeforeAllocation() {
        for (int[] counts : List.of(new int[] {1, 0, 0}, new int[] {0, 1, 0},
                new int[] {0, 0, 1}, new int[] {50_000_000, 50_000_000, 50_000_000})) {
            byte[] frame = DbProbe.header(counts[0], counts[1], counts[2]);
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> SemanticIndex.decode(frame));
            assertEquals("semantic row counts exceed payload", failure.getMessage());
        }
    }

    @Test
    void utf8CorruptionIsNotReplacedWithDifferentMetadata() {
        for (byte[] invalid : List.of(
                new byte[] {(byte) 0x80},
                new byte[] {(byte) 0xc0, (byte) 0xaf},
                new byte[] {(byte) 0xed, (byte) 0xa0, (byte) 0x80},
                new byte[] {(byte) 0xf4, (byte) 0x90, (byte) 0x80, (byte) 0x80},
                new byte[] {(byte) 0xe2, (byte) 0x82})) {
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> SemanticIndex.decode(DbProbe.corrupt(invalid)));
            assertInstanceOf(CharacterCodingException.class, failure.getCause());
        }
    }

    @Test
    void validUnicodeAndEmptyPathsRoundTripWithoutWireChanges() {
        for (String path : List.of("", "src/Db.java", "src/क/数据库/🙂.java", "\u0000")) {
            var index = DbProbe.sample(path);
            byte[] encoded = index.encode();
            var read = SemanticIndex.decode(encoded);
            assertEquals(index.nodes(), read.nodes());
            assertArrayEquals(encoded, read.encode());
        }
        byte[] empty = DbProbe.header(0, 0, 0);
        assertArrayEquals(empty, SemanticIndex.decode(empty).encode());
    }

    @Test
    void inputBytesAreNotMutatedAndNoExtraBytesAreAccepted() {
        byte[] valid = DbProbe.sample("src/Db.java").encode();
        byte[] before = valid.clone();
        SemanticIndex.decode(valid);
        assertArrayEquals(before, valid);
        assertThrows(IllegalArgumentException.class,
                () -> SemanticIndex.decode(Arrays.copyOf(valid, valid.length + 1)));
        assertThrows(NullPointerException.class, () -> SemanticIndex.decode(null));
    }

    @Test
    void everyTruncatedPrefixRemainsAnExplicitParseFailure() {
        byte[] valid = DbProbe.sample("src/🙂.java").encode();
        for (int length = 0; length < valid.length; length++) {
            byte[] truncated = Arrays.copyOf(valid, length);
            assertThrows(IllegalArgumentException.class,
                    () -> SemanticIndex.decode(truncated), "prefix=" + length);
        }
    }

    @Test
    void exactNodeSizeBoundaryIsNeitherUnderestimatedNorOverestimated() {
        byte[] valid = DbProbe.sample("src/Db.java").encode();
        ByteBuffer view = ByteBuffer.wrap(valid);
        int strings = view.getInt(12);
        view.position(24);
        for (int i = 0; i < strings; i++) {
            int length = view.getInt();
            view.position(view.position() + length);
        }
        assertEquals(173, view.remaining());
        assertArrayEquals(valid, SemanticIndex.decode(valid).encode());
        byte[] shortNode = Arrays.copyOf(valid, valid.length - 1);
        assertThrows(IllegalArgumentException.class, () -> SemanticIndex.decode(shortNode));
    }

    @Test
    void corruptStoredTextSurfacesAsIOExceptionWithDecoderCause() throws Exception {
        try (M3DB db = M3DB.open(home)) {
            byte[] corrupt = DbProbe.corrupt(new byte[] {(byte) 0x80});
            db.putArtifact("bad", SemanticIndex.ARTIFACT_KIND, 1, corrupt);
            IOException failure = assertThrows(IOException.class, () -> db.requireSemanticIndex("bad"));
            assertInstanceOf(IllegalArgumentException.class, failure.getCause());
            assertInstanceOf(CharacterCodingException.class, failure.getCause().getCause());
        }
    }

    @Test
    void tiedSimilarityAndCompositionHaveStableIdentityOrdering() {
        var fingerprint = SemanticFingerprint.leaf("TIE", List.of("node"),
                List.of("same"), "same");
        var a = node(SemanticKind.ATOM, "key", fingerprint);
        var b = node(SemanticKind.FIELD, "key", fingerprint);
        var parent = node(SemanticKind.METHOD, "parent", fingerprint);
        var first = SemanticIndex.of(List.of(b, a), List.of());
        List<String> expected = List.of(a, b).stream().map(SemanticNode::nodeId).sorted().toList();
        assertEquals(expected, first.nearSimHash(fingerprint.simHash64(), 0).stream()
                .map(match -> match.node().nodeId()).toList());
        var edges = List.of(new SemanticEdge(parent.nodeId(), b.nodeId(), "MEMBER", 0),
                new SemanticEdge(parent.nodeId(), a.nodeId(), "MEMBER", 0));
        var graph = SemanticIndex.of(List.of(parent, b, a), edges);
        assertEquals(List.of(a, b).stream().sorted(Comparator.comparing(SemanticNode::nodeId))
                .toList(), graph.children(parent.nodeId()));
        assertArrayEquals(graph.encode(), SemanticIndex.decode(graph.encode()).encode());
    }

    private static SemanticNode node(SemanticKind kind, String key,
            SemanticFingerprint fingerprint) {
        return new SemanticNode(SemanticIndex.nodeId(kind, key), kind,
                key, "src/Db.java", key, "ContractProbe", fingerprint);
    }
}
