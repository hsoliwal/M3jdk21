// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.mindex.MIndexString;
import com.synexia.mindex.MIndexValue;
import org.junit.jupiter.api.Test;

final class MIndexRecursiveTransferTest {
    @MIndexValue
    private static final class NodeValue {
        private final MIndexString name;
        private final int ordinal;

        NodeValue(String name, int ordinal) {
            this.name = MIndexString.literal(name);
            this.ordinal = ordinal;
        }
    }

    @Test
    void recursivelyTransfersNestedCanonicalDagWithObjectLeaves() {
        MIndexCompositeIndex sourceIndex =
                new MIndexCompositeIndex();
        MIndexCompositeIndex targetIndex =
                new MIndexCompositeIndex();
        MIndexObjectSpace<NodeValue> sourceObjects =
                MIndexSpaces.values();
        MIndexObjectSpace<NodeValue> targetObjects =
                MIndexSpaces.values();
        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();

        int sourceA =
                sourceObjects.id(
                        new NodeValue("recursive-a", 1));
        int sourceB =
                sourceObjects.id(
                        new NodeValue("recursive-b", 2));
        int sourceC =
                sourceObjects.id(
                        new NodeValue("recursive-c", 3));

        NodeValue targetB =
                new NodeValue("recursive-b", 2);
        NodeValue targetA =
                new NodeValue("recursive-a", 1);
        NodeValue targetC =
                new NodeValue("recursive-c", 3);
        int targetBId = targetObjects.id(targetB);
        int targetAId = targetObjects.id(targetA);
        int targetCId = targetObjects.id(targetC);
        NodeValue targetBValue =
                targetObjects.value(targetBId);
        NodeValue targetAValue =
                targetObjects.value(targetAId);
        NodeValue targetCValue =
                targetObjects.value(targetCId);

        MIndexFrozenSet<NodeValue> objectSet =
                MIndexFrozenSet.ofIds(
                        sourceObjects,
                        sourceIndex,
                        new int[] {
                            sourceA, sourceB, sourceC
                        });

        int relation =
                strings.id(
                        MIndexString.literal("recursive-edge"));
        MIndexFrozenGraph<NodeValue, MIndexString> graph =
                MIndexFrozenGraph.ofIds(
                        sourceObjects,
                        strings,
                        sourceIndex,
                        new int[] {
                            sourceA, relation, sourceB,
                            sourceB, relation, sourceC
                        });

        MIndexCompositeSpace sourceComposites =
                sourceIndex.space();
        MIndexFrozenList<MIndexCompositeRef> parent =
                MIndexFrozenList.ofIds(
                        sourceComposites,
                        sourceIndex,
                        new int[] {
                            objectSet.canonicalId(),
                            graph.canonicalId(),
                            objectSet.canonicalId()
                        });

        MIndexFrozenTuple<MIndexCompositeRef> root =
                MIndexFrozenTuple.ofIds(
                        sourceComposites,
                        sourceIndex,
                        new int[] {
                            parent.canonicalId(),
                            objectSet.canonicalId()
                        });

        MIndexCompositeTransferPlan transfer =
                MIndexCompositeTransferPlan.between(
                                sourceIndex, targetIndex)
                        .bindObjects(
                                sourceObjects, targetObjects);

        MIndexCompositeRef movedRoot =
                transfer.transfer(
                        sourceComposites.ref(root));

        assertEquals(
                MIndexCompositeIndex.KIND_TUPLE,
                movedRoot.kind());
        assertEquals(4, transfer.mappedCompositeCount());

        MIndexFrozenTuple<MIndexCompositeRef> targetRoot =
                MIndexFrozenTuple.ofIds(
                        targetIndex.space(),
                        targetIndex,
                        movedRoot.copyLane());
        MIndexCompositeRef targetParentRef =
                targetRoot.get(0);
        MIndexCompositeRef targetSetRef =
                targetRoot.get(1);
        assertEquals(
                MIndexCompositeIndex.KIND_LIST,
                targetParentRef.kind());
        assertEquals(
                MIndexCompositeIndex.KIND_SET,
                targetSetRef.kind());

        MIndexFrozenList<MIndexCompositeRef> targetParent =
                MIndexFrozenList.ofIds(
                        targetIndex.space(),
                        targetIndex,
                        targetParentRef.copyLane());
        assertEquals(
                targetSetRef.canonicalId(),
                targetParent.get(0).canonicalId());
        assertEquals(
                targetSetRef.canonicalId(),
                targetParent.get(2).canonicalId());

        MIndexFrozenSet<NodeValue> targetSet =
                MIndexFrozenSet.ofIds(
                        targetObjects,
                        targetIndex,
                        targetSetRef.copyLane());
        assertEquals(3, targetSet.size());
        assertTrue(
                targetSet.contains(
                        new NodeValue("recursive-a", 1)));
        assertTrue(
                targetSet.contains(
                        new NodeValue("recursive-b", 2)));
        assertTrue(
                targetSet.contains(
                        new NodeValue("recursive-c", 3)));

        MIndexCompositeRef targetGraphRef =
                targetParent.get(1);
        MIndexFrozenGraph<NodeValue, MIndexString> targetGraph =
                MIndexFrozenGraph.ofIds(
                        targetObjects,
                        strings,
                        targetIndex,
                        targetGraphRef.copyLane());
        assertEquals(2, targetGraph.edgeCount());
        assertTrue(
                targetGraph.containsEdge(
                        new NodeValue("recursive-a", 1),
                        MIndexString.literal("recursive-edge"),
                        new NodeValue("recursive-b", 2)));

        int transferredA =
                targetObjects.findId(
                        new NodeValue("recursive-a", 1));
        int transferredB =
                targetObjects.findId(
                        new NodeValue("recursive-b", 2));
        int transferredC =
                targetObjects.findId(
                        new NodeValue("recursive-c", 3));
        assertSame(
                targetAValue,
                targetObjects.value(transferredA));
        assertSame(
                targetBValue,
                targetObjects.value(transferredB));
        assertSame(
                targetCValue,
                targetObjects.value(transferredC));
    }

    @Test
    void recursivelyTransfersShapesAndObjectTables() {
        MIndexCompositeIndex sourceIndex =
                new MIndexCompositeIndex();
        MIndexCompositeIndex targetIndex =
                new MIndexCompositeIndex();
        MIndexObjectSpace<NodeValue> sourceObjects =
                MIndexSpaces.values();
        MIndexObjectSpace<NodeValue> targetObjects =
                MIndexSpaces.values();
        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();

        int nameId =
                strings.id(MIndexString.literal("node"));
        int typeId =
                strings.id(
                        MIndexString.literal(
                                NodeValue.class.getName()));
        MIndexShapeIndex sourceShapes =
                new MIndexShapeIndex(sourceIndex);
        int shapeId =
                sourceShapes.intern(
                        new int[] {nameId},
                        new int[] {typeId});

        int sourceNode =
                sourceObjects.id(
                        new NodeValue("table-node", 7));
        MIndexFrozenTable<NodeValue> table =
                MIndexFrozenTable.ofIds(
                        sourceObjects,
                        sourceIndex,
                        1,
                        sourceShapes.copyLane(shapeId),
                        new int[] {sourceNode});

        MIndexCompositeSpace composites =
                sourceIndex.space();
        MIndexFrozenList<MIndexCompositeRef> root =
                MIndexFrozenList.ofIds(
                        composites,
                        sourceIndex,
                        new int[] {
                            shapeId,
                            table.canonicalId()
                        });

        MIndexCompositeTransferPlan plan =
                MIndexCompositeTransferPlan.between(
                                sourceIndex, targetIndex)
                        .bindObjects(
                                sourceObjects, targetObjects);
        MIndexCompositeRef moved =
                plan.transfer(composites.ref(root));

        MIndexFrozenList<MIndexCompositeRef> targetRoot =
                MIndexFrozenList.ofIds(
                        targetIndex.space(),
                        targetIndex,
                        moved.copyLane());
        MIndexCompositeRef targetShapeRef =
                targetRoot.get(0);
        MIndexCompositeRef targetTableRef =
                targetRoot.get(1);

        assertEquals(
                MIndexCompositeIndex.KIND_SHAPE,
                targetShapeRef.kind());
        MIndexShapeIndex targetShapes =
                new MIndexShapeIndex(targetIndex);
        assertArrayEquals(
                sourceShapes.copyLane(shapeId),
                targetShapes.copyLane(
                        targetShapeRef.canonicalId()));

        MIndexFrozenTable<NodeValue> targetTable =
                MIndexFrozenTable.ofIds(
                        targetObjects,
                        targetIndex,
                        1,
                        targetShapes.copyLane(
                                targetShapeRef.canonicalId()),
                        extractTableCells(targetTableRef));
        assertEquals(1, targetTable.rows());
        assertTrue(targetTable.isShaped());
        assertEquals(
                "table-node",
                targetTable.get(0, 0).name.toString());
    }

    @Test
    void recursiveTransferRequiresExplicitLeafBinding() {
        MIndexCompositeIndex sourceIndex =
                new MIndexCompositeIndex();
        MIndexCompositeIndex targetIndex =
                new MIndexCompositeIndex();
        MIndexObjectSpace<NodeValue> sourceObjects =
                MIndexSpaces.values();
        int id =
                sourceObjects.id(
                        new NodeValue("unbound", 1));

        MIndexFrozenSet<NodeValue> set =
                MIndexFrozenSet.ofIds(
                        sourceObjects,
                        sourceIndex,
                        new int[] {id});

        MIndexCompositeTransferPlan plan =
                MIndexCompositeTransferPlan.between(
                        sourceIndex, targetIndex);

        assertThrows(
                IllegalArgumentException.class,
                () -> plan.transfer(
                        sourceIndex.space().ref(set)));
    }

    @Test
    void recursiveTransferRejectsUnknownKindAndCycles() {
        MIndexCompositeIndex customSource =
                new MIndexCompositeIndex();
        MIndexCompositeIndex customTarget =
                new MIndexCompositeIndex();
        int custom =
                customSource.intern(
                        999, new int[] {1, 2, 3});

        MIndexCompositeTransferPlan customPlan =
                MIndexCompositeTransferPlan.between(
                        customSource, customTarget);
        assertThrows(
                IllegalArgumentException.class,
                () -> customPlan.transfer(
                        customSource.space().value(custom)));

        MIndexCompositeIndex cyclicSource =
                new MIndexCompositeIndex();
        MIndexCompositeIndex cyclicTarget =
                new MIndexCompositeIndex();

        // Deliberately bypass the typed frozen-list constructor to construct
        // malformed generic composite input: ID 0 points to itself.
        int cycle =
                cyclicSource.intern(
                        MIndexCompositeIndex.KIND_LIST,
                        cyclicSource.space(),
                        null,
                        new int[] {0});
        assertEquals(0, cycle);

        MIndexCompositeTransferPlan cyclePlan =
                MIndexCompositeTransferPlan.between(
                        cyclicSource, cyclicTarget);
        assertThrows(
                IllegalArgumentException.class,
                () -> cyclePlan.transfer(
                        cyclicSource.space().value(cycle)));
    }

    private static int[] extractTableCells(
            MIndexCompositeRef table) {
        int[] lane = table.copyLane();
        int shapeLength = lane[2];
        return java.util.Arrays.copyOfRange(
                lane, 3 + shapeLength, lane.length);
    }
    @Test
    void recursiveTransferRejectsMalformedBuiltInNormalForms() {
        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();
        int left =
                strings.id(
                        MIndexString.literal(
                                "malformed-left"));
        int right =
                strings.id(
                        MIndexString.literal(
                                "malformed-right"));
        int low = Math.min(left, right);
        int high = Math.max(left, right);

        MIndexCompositeIndex source =
                new MIndexCompositeIndex();
        MIndexCompositeIndex target =
                new MIndexCompositeIndex();

        int badSet =
                source.intern(
                        MIndexCompositeIndex.KIND_SET,
                        strings,
                        null,
                        new int[] {high, low});
        int badMap =
                source.intern(
                        MIndexCompositeIndex.KIND_MAP,
                        strings,
                        strings,
                        new int[] {
                            high, left,
                            low, right
                        });
        int badGraph =
                source.intern(
                        MIndexCompositeIndex.KIND_GRAPH_EDGE_SET,
                        strings,
                        strings,
                        new int[] {
                            low, low, high,
                            low, low, high
                        });
        int badQueue =
                source.intern(
                        MIndexCompositeIndex.KIND_PRIORITY_QUEUE,
                        strings,
                        null,
                        new int[] {
                            0, 10, low,
                            0, 5, high
                        });
        int badShape =
                source.intern(
                        MIndexCompositeIndex.KIND_SHAPE,
                        strings,
                        null,
                        new int[] {low, high});

        int badBagOrder =
                source.intern(
                        MIndexCompositeIndex.KIND_BAG,
                        strings,
                        null,
                        new int[] {
                            high, 1,
                            low, 2
                        });
        int badBagCount =
                source.intern(
                        MIndexCompositeIndex.KIND_BAG,
                        strings,
                        null,
                        new int[] {low, 0});
        int badOrderedSet =
                source.intern(
                        MIndexCompositeIndex.KIND_ORDERED_SET,
                        strings,
                        null,
                        new int[] {low, high, low});

        MIndexCompositeTransferPlan plan =
                MIndexCompositeTransferPlan.between(
                        source, target);

        assertThrows(
                IllegalArgumentException.class,
                () -> plan.transfer(
                        source.space().value(badSet)));
        assertThrows(
                IllegalArgumentException.class,
                () -> plan.transfer(
                        source.space().value(badMap)));
        assertThrows(
                IllegalArgumentException.class,
                () -> plan.transfer(
                        source.space().value(badGraph)));
        assertThrows(
                IllegalArgumentException.class,
                () -> plan.transfer(
                        source.space().value(badQueue)));
        assertThrows(
                IllegalArgumentException.class,
                () -> plan.transfer(
                        source.space().value(badShape)));
        assertThrows(
                IllegalArgumentException.class,
                () -> plan.transfer(
                        source.space().value(badBagOrder)));
        assertThrows(
                IllegalArgumentException.class,
                () -> plan.transfer(
                        source.space().value(badBagCount)));
        assertThrows(
                IllegalArgumentException.class,
                () -> plan.transfer(
                        source.space().value(badOrderedSet)));
    }

    @Test
    void recursiveTransferPlanGrowsWithSourceCompositeStore() {
        MIndexCompositeIndex source =
                new MIndexCompositeIndex();
        MIndexCompositeIndex target =
                new MIndexCompositeIndex();
        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();

        MIndexCompositeTransferPlan plan =
                MIndexCompositeTransferPlan.between(
                        source, target);

        MIndexFrozenTuple<MIndexString> first =
                MIndexFrozenTuple.ofIds(
                        strings,
                        source,
                        new int[] {
                            strings.id(
                                    MIndexString.literal(
                                            "late-first"))
                        });
        MIndexCompositeRef movedFirst =
                plan.transfer(
                        source.space().ref(first));
        assertEquals(
                MIndexCompositeIndex.KIND_TUPLE,
                movedFirst.kind());
        assertEquals(1, plan.mappedCompositeCount());

        MIndexFrozenTuple<MIndexString> second =
                MIndexFrozenTuple.ofIds(
                        strings,
                        source,
                        new int[] {
                            strings.id(
                                    MIndexString.literal(
                                            "late-second"))
                        });
        MIndexCompositeRef movedSecond =
                plan.transfer(
                        source.space().ref(second));
        assertEquals(
                MIndexCompositeIndex.KIND_TUPLE,
                movedSecond.kind());
        assertEquals(2, plan.mappedCompositeCount());

        assertEquals(
                movedFirst.canonicalId(),
                plan.transfer(
                        source.space().ref(first))
                        .canonicalId());
        assertEquals(2, plan.mappedCompositeCount());
    }

    @Test
    void recursivelyTransfersBagAndOrderedSetChildren() {
        MIndexCompositeIndex source =
                new MIndexCompositeIndex();
        MIndexCompositeIndex target =
                new MIndexCompositeIndex();
        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();

        MIndexString alpha =
                MIndexString.literal("recursive-bag-alpha");
        MIndexString beta =
                MIndexString.literal("recursive-bag-beta");
        int alphaId = strings.id(alpha);
        int betaId = strings.id(beta);

        MIndexFrozenBag<MIndexString> bag =
                MIndexFrozenBag.ofIdentityCounts(
                        strings,
                        source,
                        new int[] {
                            betaId, 2,
                            alphaId, 5
                        });
        MIndexFrozenOrderedSet<MIndexString> ordered =
                MIndexFrozenOrderedSet.ofIds(
                        strings,
                        source,
                        new int[] {
                            betaId, alphaId, betaId
                        });

        MIndexFrozenList<MIndexCompositeRef> root =
                MIndexFrozenList.ofIds(
                        source.space(),
                        source,
                        new int[] {
                            bag.canonicalId(),
                            ordered.canonicalId(),
                            bag.canonicalId()
                        });

        MIndexCompositeTransferPlan plan =
                MIndexCompositeTransferPlan.between(
                        source, target);
        MIndexCompositeRef movedRoot =
                plan.transfer(
                        source.space().ref(root));

        assertEquals(3, plan.mappedCompositeCount());
        MIndexFrozenList<MIndexCompositeRef> targetRoot =
                MIndexFrozenList.ofIds(
                        target.space(),
                        target,
                        movedRoot.copyLane());

        MIndexCompositeRef bagRef = targetRoot.get(0);
        MIndexCompositeRef orderedRef = targetRoot.get(1);
        assertEquals(
                bagRef.canonicalId(),
                targetRoot.get(2).canonicalId());
        assertEquals(
                MIndexCompositeIndex.KIND_BAG,
                bagRef.kind());
        assertEquals(
                MIndexCompositeIndex.KIND_ORDERED_SET,
                orderedRef.kind());

        MIndexFrozenBag<MIndexString> movedBag =
                MIndexFrozenBag.ofIdentityCounts(
                        strings,
                        target,
                        bagRef.copyLane());
        assertEquals(2, movedBag.distinctSize());
        assertEquals(7L, movedBag.totalSize());
        assertEquals(5, movedBag.count(alpha));
        assertEquals(2, movedBag.count(beta));

        MIndexFrozenOrderedSet<MIndexString> movedOrdered =
                MIndexFrozenOrderedSet.ofIds(
                        strings,
                        target,
                        orderedRef.copyLane());
        assertEquals(2, movedOrdered.size());
        assertEquals(beta, movedOrdered.get(0));
        assertEquals(alpha, movedOrdered.get(1));
    }

}
