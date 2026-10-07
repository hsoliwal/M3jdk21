// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static com.synexia.mindex.collections.MIndexCollectionMetadata.Role.ELEMENT;
import static com.synexia.mindex.collections.MIndexCollectionMetadata.Role.RELATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import org.junit.jupiter.api.Test;

final class MIndexRelationsTest {
    private final MIndexSpace<Integer> nodes = new MIndexPrecomputeTest.NumericSpace();
    private final MIndexSpace<Integer> labels = new MIndexPrecomputeTest.NumericSpace();
    private final MIndexCompositeIndex store = new MIndexCompositeIndex();
    private final MIndexCollectionPrecompute cache = new MIndexCollectionPrecompute(store, 4, 1_000_000);

    @Test
    void weightedForwardAndReverseRelationsPreserveEdgeOrdinalsAndLabels() {
        MIndexCollectionMetadata metadata = MIndexFrozenGraph.ofIds(nodes, labels, store,
                new int[] {1, 8, 2, 1, 9, 2, 3, 8, 2, 2, 8, 1}).precompute(cache);
        MIndexRelations relations = metadata.relations();
        assertEquals(2, relations.edgeCount(1, 2));
        assertEquals(3, relations.inDegree(2));
        int edge = relations.outgoingEdgeAt(1, 1);
        assertEquals(9, metadata.column(RELATION).idAt(edge));
        long[] weights = {0, 7, 3, 5};
        MIndexWeights outgoing = relations.outgoingWeights(weights);
        MIndexWeights incoming = relations.incomingWeights(weights);
        assertEquals(7, outgoing.weightOfId(1));
        assertEquals(12, incoming.weightOfId(2));
        assertEquals(1, relations.selectOutgoingEdge(outgoing, 1, 0));
        assertEquals(3, relations.selectIncomingEdge(incoming, 2, 7));
        assertThrows(IllegalArgumentException.class, () -> relations.selectOutgoingEdge(incoming, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> relations.selectIncomingEdge(outgoing, 2, 0));
    }

    @Test
    void sequenceSuccessorsRetainRepeatedPairsAndSelfLoops() {
        MIndexIdIndex sequence = MIndexFrozenList.ofIds(nodes, store, new int[] {1, 2, 1, 2, 2})
                .precompute(cache).column(ELEMENT);
        MIndexRelations relation = MIndexRelations.successors(sequence);
        assertEquals(4, relation.size());
        assertEquals(2, relation.edgeCount(1, 2));
        assertEquals(1, relation.edgeCount(2, 1));
        assertEquals(1, relation.edgeCount(2, 2));
        assertEquals(3, relation.inDegree(2));
        assertEquals(0, relation.edgeCount(-1, 2));
    }

    @Test
    void unrelatedSnapshotPositionsCannotBeAccidentallyZipped() {
        MIndexIdIndex a = MIndexFrozenList.ofIds(nodes, store, new int[] {1, 2})
                .precompute(cache).column(ELEMENT);
        MIndexIdIndex b = MIndexFrozenList.ofIds(nodes, store, new int[] {2, 1})
                .precompute(cache).column(ELEMENT);
        assertThrows(IllegalArgumentException.class, () -> new MIndexRelations(a, b));
        assertThrows(IllegalArgumentException.class, () -> new MIndexRelations(a, a.slice(0, 1)));
    }

    @Test
    void randomizedGraphQueriesMatchIndependentTripleScan() {
        Random random = new Random(0x52454c4154494f4eL);
        for (int trial = 0; trial < 150; trial++) {
            int[] input = new int[random.nextInt(180) * 3];
            for (int offset = 0; offset < input.length; offset += 3) {
                input[offset] = random.nextInt(15);
                input[offset + 1] = random.nextInt(4);
                input[offset + 2] = random.nextInt(15);
            }
            MIndexFrozenGraph<Integer, Integer> graph = MIndexFrozenGraph.ofIds(nodes, labels, store, input);
            int[] triples = graph.copyEdgeLane();
            MIndexRelations relation = graph.precompute(cache).relations();
            for (int source = -1; source <= 15; source++) {
                int outgoing = 0;
                int incoming = 0;
                for (int edge = 0; edge < triples.length / 3; edge++) {
                    if (triples[edge * 3] == source) {
                        assertEquals(edge, relation.outgoingEdgeAt(source, outgoing));
                        assertEquals(triples[edge * 3 + 2], relation.outgoingTargetAt(source, outgoing++));
                    }
                    if (triples[edge * 3 + 2] == source) {
                        assertEquals(edge, relation.incomingEdgeAt(source, incoming));
                        assertEquals(triples[edge * 3], relation.incomingSourceAt(source, incoming++));
                    }
                }
                assertEquals(outgoing, relation.outDegree(source));
                assertEquals(incoming, relation.inDegree(source));
                for (int target = -1; target <= 15; target++) {
                    int expected = 0;
                    for (int offset = 0; offset < triples.length; offset += 3) {
                        if (triples[offset] == source && triples[offset + 2] == target) { expected++; }
                    }
                    assertEquals(expected, relation.edgeCount(source, target));
                }
            }
        }
    }
}
