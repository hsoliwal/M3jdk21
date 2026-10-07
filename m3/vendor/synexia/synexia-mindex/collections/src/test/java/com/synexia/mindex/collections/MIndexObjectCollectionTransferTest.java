// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.mindex.MIndexString;
import com.synexia.mindex.MIndexValue;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class MIndexObjectCollectionTransferTest {
    @MIndexValue
    private static final class ValueBox {
        private final MIndexString name;
        private final int count;

        ValueBox(String name, int count) {
            this.name = MIndexString.literal(name);
            this.count = count;
        }

        MIndexString name() {
            return name;
        }

        int count() {
            return count;
        }
    }

    @Test
    void objectCollectionsUseMIndexContentInsteadOfJavaEquals() {
        MIndexObjectSpace<ValueBox> objects = MIndexSpaces.values();
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        ValueBox first = new ValueBox("same", 7);
        ValueBox structurallySame = new ValueBox("same", 7);

        assertNotEquals(first, structurallySame);
        int firstId = objects.id(first);
        assertEquals(firstId, objects.id(structurallySame));

        MIndexMap<ValueBox, MIndexString> map =
                new MIndexMap<>(objects, strings, 2);
        assertTrue(map.put(first, MIndexString.literal("first")));
        assertFalse(map.put(
                structurallySame,
                MIndexString.literal("replacement")));
        assertEquals(1, map.size());
        assertEquals(
                MIndexString.literal("replacement"),
                map.get(new ValueBox("same", 7)));

        assertThrows(
                IllegalArgumentException.class,
                () -> MIndexJavaViews.mutableMap(map));
    }

    @Test
    void objectSpaceTransferReusesExactTargetCanonicalHandle() {
        MIndexObjectSpace<ValueBox> source = MIndexSpaces.values();
        MIndexObjectSpace<ValueBox> target = MIndexSpaces.values();

        ValueBox sourceValue = new ValueBox("coordinate", 11);
        int sourceId = source.id(sourceValue);

        ValueBox targetCanonical = new ValueBox("coordinate", 11);
        int expectedTargetId = target.id(targetCanonical);
        var expectedTargetHandle =
                target.indexed(expectedTargetId);

        assertEquals(
                expectedTargetId,
                target.findTransferredId(source, sourceId));
        assertEquals(
                expectedTargetId,
                target.transferIdFrom(source, sourceId));
        assertSame(
                expectedTargetHandle,
                target.indexed(expectedTargetId));
        assertTrue(target.sameContentId(
                expectedTargetId, source, sourceId));

        int[] transferred = target.transferIdsFrom(
                source,
                new int[] {sourceId, sourceId});
        assertArrayEquals(
                new int[] {expectedTargetId, expectedTargetId},
                transferred);
    }

    @Test
    void transfersEveryCanonicalFamilyAcrossObjectAndStringDomains() {
        MIndexObjectSpace<ValueBox> sourceObjects = MIndexSpaces.values();
        MIndexObjectSpace<ValueBox> targetObjects = MIndexSpaces.values();
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();

        ValueBox sourceA = new ValueBox("a", 1);
        ValueBox sourceB = new ValueBox("b", 2);
        ValueBox sourceC = new ValueBox("c", 3);
        int sourceAId = sourceObjects.id(sourceA);
        int sourceBId = sourceObjects.id(sourceB);
        int sourceCId = sourceObjects.id(sourceC);

        ValueBox targetB = new ValueBox("b", 2);
        ValueBox targetA = new ValueBox("a", 1);
        ValueBox targetC = new ValueBox("c", 3);
        int targetBId = targetObjects.id(targetB);
        int targetAId = targetObjects.id(targetA);
        int targetCId = targetObjects.id(targetC);
        ValueBox targetBValue =
                targetObjects.value(targetBId);
        ValueBox targetAValue =
                targetObjects.value(targetAId);
        ValueBox targetCValue =
                targetObjects.value(targetCId);

        assertNotEquals(sourceAId, targetAId);
        assertNotEquals(sourceBId, targetBId);
        assertEquals(sourceCId, targetCId);

        int oneId = strings.id(MIndexString.literal("one"));
        int twoId = strings.id(MIndexString.literal("two"));
        int relationId = strings.id(MIndexString.literal("depends-on"));

        MIndexCompositeIndex sourceIndex = new MIndexCompositeIndex();
        MIndexCompositeIndex targetIndex = new MIndexCompositeIndex();
        MIndexDomainTransfer<ValueBox> objects =
                MIndexDomainTransfers.objects();
        MIndexDomainTransfer<MIndexString> text =
                MIndexDomainTransfers.strings();

        MIndexFrozenList<ValueBox> list =
                MIndexFrozenList.ofIds(
                        sourceObjects,
                        sourceIndex,
                        new int[] {sourceAId, sourceBId, sourceAId});
        MIndexFrozenList<ValueBox> transferredList =
                MIndexCollectionTransfers.transfer(
                        list,
                        targetObjects,
                        targetIndex,
                        objects);
        assertSame(targetAValue, transferredList.get(0));
        assertSame(targetBValue, transferredList.get(1));
        assertSame(targetAValue, transferredList.get(2));

        MIndexFrozenSet<ValueBox> set =
                MIndexFrozenSet.ofIds(
                        sourceObjects,
                        sourceIndex,
                        new int[] {sourceAId, sourceBId, sourceCId});
        MIndexFrozenSet<ValueBox> transferredSet =
                MIndexCollectionTransfers.transfer(
                        set,
                        targetObjects,
                        targetIndex,
                        objects);
        assertEquals(3, transferredSet.size());
        assertTrue(transferredSet.contains(new ValueBox("a", 1)));
        assertTrue(transferredSet.contains(new ValueBox("b", 2)));
        assertTrue(transferredSet.contains(new ValueBox("c", 3)));

        MIndexFrozenMap<ValueBox, MIndexString> map =
                MIndexFrozenMap.ofIds(
                        sourceObjects,
                        strings,
                        sourceIndex,
                        new int[] {
                            sourceAId, oneId,
                            sourceBId, twoId
                        });
        MIndexFrozenMap<ValueBox, MIndexString> transferredMap =
                MIndexCollectionTransfers.transfer(
                        map,
                        targetObjects,
                        strings,
                        targetIndex,
                        objects,
                        text);
        assertEquals(
                MIndexString.literal("one"),
                transferredMap.get(new ValueBox("a", 1)));
        assertEquals(
                MIndexString.literal("two"),
                transferredMap.get(new ValueBox("b", 2)));

        MIndexFrozenTuple<ValueBox> tuple =
                MIndexFrozenTuple.ofIds(
                        sourceObjects,
                        sourceIndex,
                        new int[] {sourceCId, sourceAId});
        MIndexFrozenTuple<ValueBox> transferredTuple =
                MIndexCollectionTransfers.transfer(
                        tuple,
                        targetObjects,
                        targetIndex,
                        objects);
        assertSame(targetCValue, transferredTuple.get(0));
        assertSame(targetAValue, transferredTuple.get(1));

        MIndexFrozenDeque<ValueBox> deque =
                MIndexFrozenDeque.ofIds(
                        sourceObjects,
                        sourceIndex,
                        new int[] {sourceBId, sourceAId, sourceCId});
        MIndexFrozenDeque<ValueBox> transferredDeque =
                MIndexCollectionTransfers.transfer(
                        deque,
                        targetObjects,
                        targetIndex,
                        objects);
        assertSame(targetBValue, transferredDeque.first());
        assertSame(targetCValue, transferredDeque.last());

        MIndexFrozenMultiMap<ValueBox, MIndexString> multiMap =
                MIndexFrozenMultiMap.ofIds(
                        sourceObjects,
                        strings,
                        sourceIndex,
                        new int[] {
                            sourceAId, oneId,
                            sourceAId, twoId,
                            sourceBId, twoId
                        });
        MIndexFrozenMultiMap<ValueBox, MIndexString> transferredMultiMap =
                MIndexCollectionTransfers.transfer(
                        multiMap,
                        targetObjects,
                        strings,
                        targetIndex,
                        objects,
                        text);
        assertEquals(3, transferredMultiMap.size());
        assertTrue(transferredMultiMap.containsEntry(
                new ValueBox("a", 1),
                MIndexString.literal("one")));
        assertTrue(transferredMultiMap.containsEntry(
                new ValueBox("a", 1),
                MIndexString.literal("two")));

        MIndexFrozenPriorityQueue<ValueBox> queue =
                MIndexFrozenPriorityQueue.ofOrderedLane(
                        sourceObjects,
                        sourceIndex,
                        new int[] {
                            0, 5, sourceBId,
                            0, 9, sourceAId,
                            0, 9, sourceCId
                        });
        MIndexFrozenPriorityQueue<ValueBox> transferredQueue =
                MIndexCollectionTransfers.transfer(
                        queue,
                        targetObjects,
                        targetIndex,
                        objects);
        assertEquals(5L, transferredQueue.firstPriority());
        assertSame(targetBValue, transferredQueue.first());
        assertSame(targetAValue, transferredQueue.get(1));
        assertSame(targetCValue, transferredQueue.get(2));

        MIndexFrozenGraph<ValueBox, MIndexString> graph =
                MIndexFrozenGraph.ofIds(
                        sourceObjects,
                        strings,
                        sourceIndex,
                        new int[] {
                            sourceAId, relationId, sourceBId,
                            sourceBId, relationId, sourceCId
                        });
        MIndexFrozenGraph<ValueBox, MIndexString> transferredGraph =
                MIndexCollectionTransfers.transfer(
                        graph,
                        targetObjects,
                        strings,
                        targetIndex,
                        objects,
                        text);
        assertEquals(2, transferredGraph.edgeCount());
        assertTrue(transferredGraph.containsEdge(
                new ValueBox("a", 1),
                MIndexString.literal("depends-on"),
                new ValueBox("b", 2)));

        int nameId = strings.id(MIndexString.literal("value"));
        int typeId = strings.id(MIndexString.literal(ValueBox.class.getName()));
        MIndexFrozenTable<ValueBox> table =
                MIndexFrozenTable.ofIds(
                        sourceObjects,
                        sourceIndex,
                        1,
                        new int[] {nameId, typeId},
                        new int[] {sourceAId, sourceBId, sourceCId});
        MIndexFrozenTable<ValueBox> transferredTable =
                MIndexCollectionTransfers.transfer(
                        table,
                        targetObjects,
                        targetIndex,
                        objects);
        assertEquals(3, transferredTable.rows());
        assertSame(targetAValue, transferredTable.get(0, 0));
        assertSame(targetBValue, transferredTable.get(1, 0));
        assertSame(targetCValue, transferredTable.get(2, 0));
        assertArrayEquals(
                table.copyShapeLane(),
                transferredTable.copyShapeLane());
    }

    @Test
    void setMapAndGraphTransfersFailClosedWhenPolicyCollapsesSemantics() {
        MIndexObjectSpace<ValueBox> source = MIndexSpaces.values();
        MIndexObjectSpace<ValueBox> target = MIndexSpaces.values();
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        int first = source.id(new ValueBox("first", 1));
        int second = source.id(new ValueBox("second", 2));
        int targetOnly = target.id(new ValueBox("target", 9));
        int relation = strings.id(MIndexString.literal("r"));

        MIndexCompositeIndex sourceIndex = new MIndexCompositeIndex();
        MIndexCompositeIndex targetIndex = new MIndexCompositeIndex();
        MIndexDomainTransfer<ValueBox> collapsing =
                (ignoredSource, ignoredId, targetSpace) -> {
                    targetSpace.requireId(targetOnly);
                    return targetOnly;
                };

        MIndexFrozenSet<ValueBox> set =
                MIndexFrozenSet.ofIds(
                        source, sourceIndex, new int[] {first, second});
        assertThrows(
                IllegalArgumentException.class,
                () -> MIndexCollectionTransfers.transfer(
                        set, target, targetIndex, collapsing));

        MIndexFrozenMap<ValueBox, MIndexString> map =
                MIndexFrozenMap.ofIds(
                        source,
                        strings,
                        sourceIndex,
                        new int[] {
                            first, strings.id(MIndexString.literal("1")),
                            second, strings.id(MIndexString.literal("2"))
                        });
        assertThrows(
                IllegalArgumentException.class,
                () -> MIndexCollectionTransfers.transfer(
                        map,
                        target,
                        strings,
                        targetIndex,
                        collapsing,
                        MIndexDomainTransfers.strings()));

        MIndexFrozenGraph<ValueBox, MIndexString> graph =
                MIndexFrozenGraph.ofIds(
                        source,
                        strings,
                        sourceIndex,
                        new int[] {
                            first, relation, second,
                            second, relation, first
                        });
        assertThrows(
                IllegalArgumentException.class,
                () -> MIndexCollectionTransfers.transfer(
                        graph,
                        target,
                        strings,
                        targetIndex,
                        collapsing,
                        MIndexDomainTransfers.strings()));
    }

    @Test
    void sameSpaceAndCompositeStoreTransferReturnsCanonicalIdentity() {
        MIndexObjectSpace<ValueBox> objects = MIndexSpaces.values();
        MIndexCompositeIndex index = new MIndexCompositeIndex();
        int id = objects.id(new ValueBox("identity", 1));
        MIndexFrozenList<ValueBox> list =
                MIndexFrozenList.ofIds(
                        objects, index, new int[] {id});

        assertSame(
                list,
                MIndexCollectionTransfers.transfer(
                        list,
                        objects,
                        index,
                        MIndexDomainTransfers.objects()));
    }
    @Test
    void compiledTransferPlanMemoizesIdsAndSurvivesSourceGrowth() {
        MIndexObjectSpace<ValueBox> source = MIndexSpaces.values();
        MIndexObjectSpace<ValueBox> target = MIndexSpaces.values();
        int first = source.id(new ValueBox("plan-a", 1));
        int second = source.id(new ValueBox("plan-b", 2));

        int[] delegateCalls = {0};
        MIndexDomainTransfer<ValueBox> exact =
                MIndexDomainTransfers.objects();
        MIndexDomainTransfer<ValueBox> counting =
                (actualSource, sourceId, actualTarget) -> {
                    delegateCalls[0]++;
                    return exact.transferId(
                            actualSource, sourceId, actualTarget);
                };

        MIndexTransferPlan<ValueBox> plan =
                MIndexTransferPlan.bind(
                        source, target, counting);

        int[] firstPass = plan.transferIds(
                new int[] {first, second, first, first, second});
        assertEquals(2, delegateCalls[0]);
        assertEquals(2, plan.mappedCount());
        assertEquals(firstPass[0], firstPass[2]);
        assertEquals(firstPass[0], firstPass[3]);
        assertEquals(firstPass[1], firstPass[4]);

        int third = source.id(new ValueBox("plan-c", 3));
        int thirdTarget = plan.transferId(third);
        assertEquals(3, delegateCalls[0]);
        assertEquals(3, plan.mappedCount());
        assertEquals(thirdTarget, plan.transferId(third));
        assertEquals(3, delegateCalls[0]);

        MIndexObjectSpace<ValueBox> foreign = MIndexSpaces.values();
        int foreignId = foreign.id(new ValueBox("foreign", 4));
        assertThrows(
                IllegalArgumentException.class,
                () -> plan.transferId(
                        foreign, foreignId, target));
    }

    @Test
    void duplicateHeavyCollectionsCompileEachDistinctObjectCoordinateOnce() {
        MIndexObjectSpace<ValueBox> source = MIndexSpaces.values();
        MIndexObjectSpace<ValueBox> target = MIndexSpaces.values();
        int[] sourceIds = new int[4];
        for (int index = 0; index < sourceIds.length; index++) {
            sourceIds[index] =
                    source.id(new ValueBox(
                            "duplicate-" + index, index));
        }

        int[] delegateCalls = {0};
        MIndexDomainTransfer<ValueBox> exact =
                MIndexDomainTransfers.objects();
        MIndexDomainTransfer<ValueBox> counting =
                (actualSource, sourceId, actualTarget) -> {
                    delegateCalls[0]++;
                    return exact.transferId(
                            actualSource, sourceId, actualTarget);
                };
        MIndexTransferPlan<ValueBox> plan =
                MIndexTransferPlan.bind(
                        source, target, counting);

        int[] lane = new int[20_000];
        for (int index = 0; index < lane.length; index++) {
            lane[index] = sourceIds[index & 3];
        }

        MIndexCompositeIndex sourceIndex =
                new MIndexCompositeIndex();
        MIndexCompositeIndex targetIndex =
                new MIndexCompositeIndex();
        MIndexFrozenList<ValueBox> list =
                MIndexFrozenList.ofIds(
                        source, sourceIndex, lane);

        MIndexFrozenList<ValueBox> movedList =
                MIndexCollectionTransfers.transfer(
                        list, target, targetIndex, plan);
        assertEquals(20_000, movedList.size());
        assertEquals(4, delegateCalls[0]);
        assertEquals(4, plan.mappedCount());

        MIndexFrozenTable<ValueBox> table =
                MIndexFrozenTable.ofIds(
                        source,
                        sourceIndex,
                        4,
                        lane);
        MIndexFrozenTable<ValueBox> movedTable =
                MIndexCollectionTransfers.transfer(
                        table, target, targetIndex, plan);
        assertEquals(5_000, movedTable.rows());
        assertEquals(4, delegateCalls[0]);
        assertEquals(4, plan.mappedCount());

        for (int column = 0; column < 4; column++) {
            ValueBox value = movedTable.get(0, column);
            assertEquals(
                    "duplicate-" + column,
                    value.name().toString());
            assertEquals(column, value.count());
        }
    }

    @Test
    void rawCollectionTransferAlsoMemoizesWithinOneOperation() {
        MIndexObjectSpace<ValueBox> source = MIndexSpaces.values();
        MIndexObjectSpace<ValueBox> target = MIndexSpaces.values();
        int first = source.id(new ValueBox("raw-a", 1));
        int second = source.id(new ValueBox("raw-b", 2));

        int[] lane = new int[4_096];
        for (int index = 0; index < lane.length; index++) {
            lane[index] = (index & 1) == 0 ? first : second;
        }

        int[] delegateCalls = {0};
        MIndexDomainTransfer<ValueBox> exact =
                MIndexDomainTransfers.objects();
        MIndexDomainTransfer<ValueBox> counting =
                (actualSource, sourceId, actualTarget) -> {
                    delegateCalls[0]++;
                    return exact.transferId(
                            actualSource, sourceId, actualTarget);
                };

        MIndexFrozenList<ValueBox> list =
                MIndexFrozenList.ofIds(
                        source,
                        new MIndexCompositeIndex(),
                        lane);
        MIndexFrozenList<ValueBox> moved =
                MIndexCollectionTransfers.transfer(
                        list,
                        target,
                        new MIndexCompositeIndex(),
                        counting);

        assertEquals(lane.length, moved.size());
        assertEquals(2, delegateCalls[0]);
    }

    @Test
    void stringTransferPlanUsesAllocationFreeIdentityMode() {
        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();
        MIndexString first =
                MIndexString.literal(
                        "identity-string-plan-first");
        MIndexString second =
                MIndexString.literal(
                        "identity-string-plan-second");

        MIndexTransferPlan<MIndexString> plan =
                MIndexTransferPlan.bind(
                        strings,
                        strings,
                        MIndexDomainTransfers.strings());

        assertTrue(plan.identity());
        assertEquals(
                first.contentIndex(),
                plan.transferId(first.contentIndex()));
        assertEquals(
                second.contentIndex(),
                plan.transferId(second.contentIndex()));
        assertEquals(
                first.contentIndex(),
                plan.transferId(first.contentIndex()));
        assertEquals(0, plan.mappedCount());

        MIndexTransferPlan<MIndexString> sameSpace =
                MIndexTransferPlan.bind(
                        strings,
                        strings,
                        MIndexDomainTransfers.sameSpace());
        assertTrue(sameSpace.identity());
        assertEquals(
                second.contentIndex(),
                sameSpace.transferId(second.contentIndex()));
        assertEquals(0, sameSpace.mappedCount());

        MIndexObjectSpace<ValueBox> objects =
                MIndexSpaces.values();
        int objectId =
                objects.id(
                        new ValueBox(
                                "identity-object-plan", 17));
        MIndexTransferPlan<ValueBox> objectPlan =
                MIndexTransferPlan.bind(
                        objects,
                        objects,
                        MIndexDomainTransfers.objects());
        assertTrue(objectPlan.identity());
        assertEquals(
                objectId,
                objectPlan.transferId(objectId));
        assertEquals(0, objectPlan.mappedCount());
    }

    @Test
    void transfersCanonicalBagAndOrderedSetAcrossObjectSpaces() {
        MIndexObjectSpace<ValueBox> source =
                MIndexSpaces.values();
        MIndexObjectSpace<ValueBox> target =
                MIndexSpaces.values();

        int sourceA =
                source.id(new ValueBox("bag-a", 1));
        int sourceB =
                source.id(new ValueBox("bag-b", 2));

        int targetB =
                target.id(new ValueBox("bag-b", 2));
        int targetA =
                target.id(new ValueBox("bag-a", 1));
        ValueBox targetAValue = target.value(targetA);
        ValueBox targetBValue = target.value(targetB);

        MIndexCompositeIndex sourceIndex =
                new MIndexCompositeIndex();
        MIndexCompositeIndex targetIndex =
                new MIndexCompositeIndex();
        MIndexDomainTransfer<ValueBox> transfer =
                MIndexDomainTransfers.objects();

        MIndexFrozenBag<ValueBox> bag =
                MIndexFrozenBag.ofIdentityCounts(
                        source,
                        sourceIndex,
                        new int[] {
                            sourceA, 3,
                            sourceB, 2,
                            sourceA, 4
                        });
        MIndexFrozenBag<ValueBox> movedBag =
                MIndexCollectionTransfers.transfer(
                        bag,
                        target,
                        targetIndex,
                        transfer);
        assertEquals(2, movedBag.distinctSize());
        assertEquals(9L, movedBag.totalSize());
        assertEquals(
                7,
                movedBag.count(
                        new ValueBox("bag-a", 1)));
        assertEquals(
                2,
                movedBag.count(
                        new ValueBox("bag-b", 2)));
        assertSame(
                targetAValue,
                movedBag.valueAt(
                        indexOfBagId(movedBag, targetA)));
        assertSame(
                targetBValue,
                movedBag.valueAt(
                        indexOfBagId(movedBag, targetB)));

        MIndexFrozenOrderedSet<ValueBox> ordered =
                MIndexFrozenOrderedSet.ofIds(
                        source,
                        sourceIndex,
                        new int[] {
                            sourceB, sourceA, sourceB
                        });
        MIndexFrozenOrderedSet<ValueBox> movedOrdered =
                MIndexCollectionTransfers.transfer(
                        ordered,
                        target,
                        targetIndex,
                        transfer);
        assertEquals(2, movedOrdered.size());
        assertSame(targetBValue, movedOrdered.get(0));
        assertSame(targetAValue, movedOrdered.get(1));
    }

    @Test
    void bagAndOrderedSetTransfersRejectSemanticCollapse() {
        MIndexObjectSpace<ValueBox> source =
                MIndexSpaces.values();
        MIndexObjectSpace<ValueBox> target =
                MIndexSpaces.values();
        int first =
                source.id(new ValueBox("collapse-bag-a", 1));
        int second =
                source.id(new ValueBox("collapse-bag-b", 2));
        int targetOnly =
                target.id(new ValueBox("collapse-target", 9));

        MIndexDomainTransfer<ValueBox> collapsing =
                (ignoredSource, ignoredId, targetSpace) -> {
                    targetSpace.requireId(targetOnly);
                    return targetOnly;
                };
        MIndexCompositeIndex sourceIndex =
                new MIndexCompositeIndex();
        MIndexCompositeIndex targetIndex =
                new MIndexCompositeIndex();

        MIndexFrozenBag<ValueBox> bag =
                MIndexFrozenBag.ofIdentityCounts(
                        source,
                        sourceIndex,
                        new int[] {
                            first, 2,
                            second, 3
                        });
        assertThrows(
                IllegalArgumentException.class,
                () -> MIndexCollectionTransfers.transfer(
                        bag,
                        target,
                        targetIndex,
                        collapsing));

        MIndexFrozenOrderedSet<ValueBox> ordered =
                MIndexFrozenOrderedSet.ofIds(
                        source,
                        sourceIndex,
                        new int[] {first, second});
        assertThrows(
                IllegalArgumentException.class,
                () -> MIndexCollectionTransfers.transfer(
                        ordered,
                        target,
                        targetIndex,
                        collapsing));
    }

    private static <E> int indexOfBagId(
            MIndexFrozenBag<E> bag,
            int id) {
        for (int entry = 0;
                entry < bag.distinctSize();
                entry++) {
            if (bag.idAt(entry) == id) {
                return entry;
            }
        }
        throw new AssertionError("missing bag id " + id);
    }

}
