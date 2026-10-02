// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Compact deterministic binary payload for the M3 semantic index.
 *
 * <p>All repeated strings live once in a sorted dictionary. Node/edge rows store primitive
 * dictionary IDs and 64-bit hashes, matching the M3IndexDB preference for compact coordinate lanes
 * rather than repeated String objects.
 */
public final class M3SemanticIndexPayload {
    private static final int MAGIC = 0x4d335349; // M3SI
    private static final int VERSION = 1;

    private M3SemanticIndexPayload() {}

    public static byte[] encode(
            List<M3SemanticNodeTable.Row> nodeRows,
            List<M3SemanticEdgeTable.Row> edgeRows) {
        List<M3SemanticNodeTable.Row> nodes = canonicalNodes(nodeRows);
        List<M3SemanticEdgeTable.Row> edges = canonicalEdges(edgeRows);
        List<String> dictionary = dictionary(nodes, edges);
        Map<String, Integer> ids = dictionaryIds(dictionary);

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.writeInt(dictionary.size());
                for (String value : dictionary) {
                    byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
                    out.writeInt(utf8.length);
                    out.write(utf8);
                }

                out.writeInt(nodes.size());
                for (M3SemanticNodeTable.Row row : nodes) {
                    out.writeInt(id(ids, row.nodeId()));
                    out.writeInt(id(ids, row.kind()));
                    out.writeInt(id(ids, row.semanticKey()));
                    out.writeInt(id(ids, row.sourcePath()));
                    out.writeInt(id(ids, row.symbol()));
                    out.writeInt(id(ids, row.exactSha256()));
                    out.writeInt(id(ids, row.structuralSha256()));
                    out.writeInt(id(ids, row.logicSha256()));
                    out.writeLong(Long.parseUnsignedLong(row.structuralHash64(), 16));
                    out.writeLong(Long.parseUnsignedLong(row.logicHash64(), 16));
                    out.writeLong(Long.parseUnsignedLong(row.simHash64(), 16));
                    out.writeInt(id(ids, row.normalizedComposition()));
                }

                out.writeInt(edges.size());
                for (M3SemanticEdgeTable.Row row : edges) {
                    out.writeInt(id(ids, row.parentId()));
                    out.writeInt(id(ids, row.childId()));
                    out.writeInt(id(ids, row.role()));
                    out.writeInt(row.ordinal());
                }
            }
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Minimal integrity inspection used by tests and M3IndexDB admission gates. */
    public static Header inspect(byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (in.readInt() != MAGIC) throw new IllegalArgumentException("semantic index magic");
            int version = in.readInt();
            if (version != VERSION) throw new IllegalArgumentException("semantic index version");
            int dictionaryCount = positiveOrZero(in.readInt(), "dictionaryCount");
            for (int index = 0; index < dictionaryCount; index++) {
                int length = positiveOrZero(in.readInt(), "dictionary length");
                if (length > payload.length) throw new IllegalArgumentException("dictionary length");
                in.readNBytes(length);
            }
            int nodeCount = positiveOrZero(in.readInt(), "nodeCount");
            long nodeBytes = Math.multiplyExact((long) nodeCount, 9L * Integer.BYTES + 3L * Long.BYTES);
            if (nodeBytes > in.available()) throw new IllegalArgumentException("node payload");
            in.skipNBytes(nodeBytes);
            int edgeCount = positiveOrZero(in.readInt(), "edgeCount");
            long edgeBytes = Math.multiplyExact((long) edgeCount, 4L * Integer.BYTES);
            if (edgeBytes != in.available()) throw new IllegalArgumentException("edge payload");
            return new Header(version, dictionaryCount, nodeCount, edgeCount, payload.length);
        } catch (IOException | ArithmeticException malformed) {
            throw new IllegalArgumentException("invalid semantic index payload", malformed);
        }
    }

    private static List<M3SemanticNodeTable.Row> canonicalNodes(
            List<M3SemanticNodeTable.Row> rows) {
        Objects.requireNonNull(rows, "nodeRows");
        Map<String, M3SemanticNodeTable.Row> byId = new LinkedHashMap<>();
        for (M3SemanticNodeTable.Row row : rows) {
            Objects.requireNonNull(row, "nodeRow");
            M3SemanticNodeTable.Row prior = byId.putIfAbsent(row.nodeId(), row);
            if (prior != null && !sameNode(prior, row)) {
                throw new IllegalArgumentException("semantic node identity collision: " + row.nodeId());
            }
        }
        return byId.values().stream()
                .sorted(Comparator.comparing(M3SemanticNodeTable.Row::semanticKey)
                        .thenComparing(M3SemanticNodeTable.Row::nodeId))
                .toList();
    }

    private static List<M3SemanticEdgeTable.Row> canonicalEdges(
            List<M3SemanticEdgeTable.Row> rows) {
        Objects.requireNonNull(rows, "edgeRows");
        Map<String, M3SemanticEdgeTable.Row> unique = new LinkedHashMap<>();
        for (M3SemanticEdgeTable.Row row : rows) {
            Objects.requireNonNull(row, "edgeRow");
            String key = row.parentId() + "\u0000" + row.role() + "\u0000"
                    + row.ordinal() + "\u0000" + row.childId();
            unique.putIfAbsent(key, row);
        }
        return unique.values().stream()
                .sorted(Comparator.comparing(M3SemanticEdgeTable.Row::parentId)
                        .thenComparing(M3SemanticEdgeTable.Row::role)
                        .thenComparingInt(M3SemanticEdgeTable.Row::ordinal)
                        .thenComparing(M3SemanticEdgeTable.Row::childId))
                .toList();
    }

    private static List<String> dictionary(
            List<M3SemanticNodeTable.Row> nodes,
            List<M3SemanticEdgeTable.Row> edges) {
        TreeSet<String> values = new TreeSet<>();
        for (M3SemanticNodeTable.Row row : nodes) {
            values.add(row.nodeId());
            values.add(row.kind());
            values.add(row.semanticKey());
            values.add(row.sourcePath());
            values.add(row.symbol());
            values.add(row.exactSha256());
            values.add(row.structuralSha256());
            values.add(row.logicSha256());
            values.add(row.normalizedComposition());
        }
        for (M3SemanticEdgeTable.Row row : edges) {
            values.add(row.parentId());
            values.add(row.childId());
            values.add(row.role());
        }
        return List.copyOf(values);
    }

    private static Map<String, Integer> dictionaryIds(List<String> dictionary) {
        Map<String, Integer> ids = new LinkedHashMap<>(Math.max(16, dictionary.size() * 2));
        for (int index = 0; index < dictionary.size(); index++) ids.put(dictionary.get(index), index);
        return Map.copyOf(ids);
    }

    private static int id(Map<String, Integer> ids, String value) {
        Integer id = ids.get(value);
        if (id == null) throw new IllegalStateException("missing semantic dictionary value");
        return id;
    }

    private static boolean sameNode(
            M3SemanticNodeTable.Row left,
            M3SemanticNodeTable.Row right) {
        return left.kind().equals(right.kind())
                && left.semanticKey().equals(right.semanticKey())
                && left.sourcePath().equals(right.sourcePath())
                && left.symbol().equals(right.symbol())
                && left.exactSha256().equals(right.exactSha256())
                && left.structuralSha256().equals(right.structuralSha256())
                && left.logicSha256().equals(right.logicSha256())
                && left.structuralHash64().equals(right.structuralHash64())
                && left.logicHash64().equals(right.logicHash64())
                && left.simHash64().equals(right.simHash64())
                && left.normalizedComposition().equals(right.normalizedComposition());
    }

    private static int positiveOrZero(int value, String field) {
        if (value < 0) throw new IllegalArgumentException(field);
        return value;
    }

    public record Header(
            int version,
            int dictionaryCount,
            int nodeCount,
            int edgeCount,
            int payloadBytes) {}
}
