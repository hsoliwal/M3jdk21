// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import com.m3.indexdb.M3IndexDbSemanticEdge;
import com.m3.indexdb.M3IndexDbSemanticFingerprint;
import com.m3.indexdb.M3IndexDbSemanticIndex;
import com.m3.indexdb.M3IndexDbSemanticKind;
import com.m3.indexdb.M3IndexDbSemanticNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Thin OpenRewrite -> M3IndexDB semantic adapter.
 *
 * <p>M3IndexDB owns the binary format, canonical child-first recomposition and query semantics.
 * OpenRewrite owns only source analysis and row production.
 */
public final class M3SemanticIndexPayload {
    private M3SemanticIndexPayload() {}

    public static byte[] encode(
            List<M3SemanticNodeTable.Row> nodeRows,
            List<M3SemanticEdgeTable.Row> edgeRows) {
        return toIndex(nodeRows, edgeRows).encode();
    }

    public static M3IndexDbSemanticIndex toIndex(
            List<M3SemanticNodeTable.Row> nodeRows,
            List<M3SemanticEdgeTable.Row> edgeRows) {
        Objects.requireNonNull(nodeRows, "nodeRows");
        Objects.requireNonNull(edgeRows, "edgeRows");

        ArrayList<M3IndexDbSemanticNode> nodes = new ArrayList<>(nodeRows.size());
        for (M3SemanticNodeTable.Row row : nodeRows) {
            Objects.requireNonNull(row, "nodeRow");
            nodes.add(
                    new M3IndexDbSemanticNode(
                            row.nodeId(),
                            M3IndexDbSemanticKind.valueOf(row.kind()),
                            row.semanticKey(),
                            row.sourcePath(),
                            row.symbol(),
                            new M3IndexDbSemanticFingerprint(
                                    row.exactSha256(),
                                    row.structuralSha256(),
                                    row.logicSha256(),
                                    Long.parseUnsignedLong(row.structuralHash64(), 16),
                                    Long.parseUnsignedLong(row.logicHash64(), 16),
                                    Long.parseUnsignedLong(row.simHash64(), 16),
                                    row.normalizedComposition())));
        }

        ArrayList<M3IndexDbSemanticEdge> edges = new ArrayList<>(edgeRows.size());
        for (M3SemanticEdgeTable.Row row : edgeRows) {
            Objects.requireNonNull(row, "edgeRow");
            edges.add(
                    new M3IndexDbSemanticEdge(
                            row.parentId(),
                            row.childId(),
                            row.role(),
                            row.ordinal()));
        }
        return M3IndexDbSemanticIndex.of(nodes, edges);
    }

    /** Integrity inspection retained for recipe/JUnit proof without owning the payload format. */
    public static Header inspect(byte[] payload) {
        byte[] checked = Objects.requireNonNull(payload, "payload");
        M3IndexDbSemanticIndex index = M3IndexDbSemanticIndex.decode(checked);
        Set<String> strings = new HashSet<>();
        for (M3IndexDbSemanticNode node : index.nodes()) {
            strings.add(node.semanticKey());
            strings.add(node.sourcePath());
            strings.add(node.symbol());
            strings.add(node.fingerprint().normalizedComposition());
        }
        for (M3IndexDbSemanticEdge edge : index.edges()) strings.add(edge.role());
        return new Header(
                M3IndexDbSemanticIndex.FORMAT_VERSION,
                strings.size(),
                index.nodes().size(),
                index.edges().size(),
                checked.length);
    }

    public record Header(
            int version,
            int dictionaryCount,
            int nodeCount,
            int edgeCount,
            int payloadBytes) {}
}
