// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.mindex.MIndexString;
import com.synexia.mindex.collection.MIndexCollectionSpec;
import com.synexia.mindex.collection.MIndexCollections;
import com.synexia.mindex.collection.MIndexStringDomain;
import org.junit.jupiter.api.Test;

final class MIndexLegacyCollectionConvergenceTest {
    @Test
    void legacyBagAndOrderedSetPromoteIntoCanonicalArena() {
        MIndexStringDomain domain =
                MIndexStringDomain.INSTANCE;
        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();
        MIndexCompositeIndex index =
                new MIndexCompositeIndex();

        MIndexString alpha =
                MIndexString.literal("legacy-bag-alpha");
        MIndexString beta =
                MIndexString.literal("legacy-bag-beta");

        com.synexia.mindex.collection.MIndexBag<MIndexString> bag =
                MIndexCollections.bag(domain, 4);
        bag.add(beta, 2);
        bag.add(alpha, 3);
        bag.add(beta, 4);

        MIndexFrozenBag<MIndexString> canonicalBag =
                MIndexLegacyCollections.canonicalize(
                        bag, index);
        assertEquals(2, canonicalBag.distinctSize());
        assertEquals(9L, canonicalBag.totalSize());
        assertEquals(3, canonicalBag.count(alpha));
        assertEquals(6, canonicalBag.count(beta));

        MIndexFrozenBag<MIndexString> directBag =
                MIndexFrozenBag.ofIdentityCounts(
                        strings,
                        index,
                        new int[] {
                            beta.contentIndex(), 2,
                            alpha.contentIndex(), 3,
                            beta.contentIndex(), 4
                        });
        assertEquals(directBag, canonicalBag);

        com.synexia.mindex.collection.MIndexOrderedSet<MIndexString>
                ordered =
                new com.synexia.mindex.collection.MIndexOrderedSet<>(
                        domain, 4);
        assertTrue(ordered.add(beta));
        assertTrue(ordered.add(alpha));
        ordered.add(beta);

        MIndexFrozenOrderedSet<MIndexString> canonicalOrdered =
                MIndexLegacyCollections.canonicalize(
                        ordered, index);
        assertEquals(2, canonicalOrdered.size());
        assertEquals(beta, canonicalOrdered.get(0));
        assertEquals(alpha, canonicalOrdered.get(1));

        MIndexFrozenOrderedSet<MIndexString> directOrdered =
                MIndexFrozenOrderedSet.ofIds(
                        strings,
                        index,
                        new int[] {
                            beta.contentIndex(),
                            alpha.contentIndex(),
                            beta.contentIndex()
                        });
        assertEquals(directOrdered, canonicalOrdered);
    }

    @Test
    void overlappingLegacyShapesCanonicalizeWithoutValueMaterialization() {
        MIndexStringDomain domain =
                MIndexStringDomain.INSTANCE;
        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();
        MIndexCompositeIndex index =
                new MIndexCompositeIndex();

        MIndexString alpha =
                MIndexString.literal("legacy-overlap-alpha");
        MIndexString beta =
                MIndexString.literal("legacy-overlap-beta");
        MIndexString gamma =
                MIndexString.literal("legacy-overlap-gamma");

        com.synexia.mindex.collection.MIndexList<MIndexString> list =
                MIndexCollections.list(domain, 4);
        list.add(alpha);
        list.add(beta);
        list.add(alpha);
        MIndexFrozenList<MIndexString> canonicalList =
                MIndexLegacyCollections.canonicalize(
                        list, index);
        assertEquals(
                MIndexFrozenList.ofIds(
                        strings,
                        index,
                        new int[] {
                            alpha.contentIndex(),
                            beta.contentIndex(),
                            alpha.contentIndex()
                        }),
                canonicalList);

        com.synexia.mindex.collection.MIndexTuple<MIndexString> tuple =
                list.freeze();
        MIndexFrozenTuple<MIndexString> canonicalTuple =
                MIndexLegacyCollections.canonicalize(
                        tuple, index);
        assertEquals(
                MIndexFrozenTuple.ofIds(
                        strings,
                        index,
                        tuple.copyIdentities()),
                canonicalTuple);

        com.synexia.mindex.collection.MIndexDeque<MIndexString> deque =
                MIndexCollections.deque(domain, 4);
        deque.addLast(beta);
        deque.addFirst(alpha);
        deque.addLast(gamma);
        MIndexFrozenDeque<MIndexString> canonicalDeque =
                MIndexLegacyCollections.canonicalize(
                        deque, index);
        assertEquals(alpha, canonicalDeque.first());
        assertEquals(gamma, canonicalDeque.last());

        com.synexia.mindex.collection.MIndexPriorityQueue<MIndexString>
                queue =
                MIndexCollections.priorityQueue(domain, 4);
        queue.add(gamma, 20);
        queue.add(alpha, 5);
        queue.add(beta, 5);
        MIndexFrozenPriorityQueue<MIndexString> canonicalQueue =
                MIndexLegacyCollections.canonicalize(
                        queue, index);
        assertEquals(alpha, canonicalQueue.first());
        assertEquals(5L, canonicalQueue.firstPriority());
        assertEquals(beta, canonicalQueue.get(1));
        assertEquals(gamma, canonicalQueue.get(2));

        com.synexia.mindex.collection.MIndexSet<MIndexString> legacySet =
                MIndexCollections.set(
                        domain,
                        MIndexCollectionSpec.general(4));
        legacySet.add(gamma);
        legacySet.add(alpha);
        legacySet.add(beta);
        com.synexia.mindex.collection.MIndexFrozenSet<MIndexString>
                frozenSet =
                MIndexCollections.freeze(legacySet);
        MIndexFrozenSet<MIndexString> canonicalSet =
                MIndexLegacyCollections.canonicalize(
                        frozenSet, index);
        assertEquals(3, canonicalSet.size());
        assertTrue(canonicalSet.contains(alpha));
        assertTrue(canonicalSet.contains(beta));
        assertTrue(canonicalSet.contains(gamma));

        com.synexia.mindex.collection.MIndexMap<
                MIndexString, MIndexString> legacyMap =
                MIndexCollections.map(
                        domain,
                        domain,
                        MIndexCollectionSpec.general(4));
        legacyMap.put(alpha, beta);
        legacyMap.put(gamma, alpha);
        com.synexia.mindex.collection.MIndexFrozenMap<
                MIndexString, MIndexString> frozenMap =
                MIndexCollections.freeze(legacyMap);
        MIndexFrozenMap<MIndexString, MIndexString> canonicalMap =
                MIndexLegacyCollections.canonicalize(
                        frozenMap, index);
        assertEquals(beta, canonicalMap.get(alpha));
        assertEquals(alpha, canonicalMap.get(gamma));
    }

    @Test
    void legacyObjectDomainAuthoritySurvivesCanonicalPromotion() {
        com.synexia.mindex.collection.MIndexObjectDomain<
                MIndexString> legacy =
                new com.synexia.mindex.collection.MIndexObjectDomain<>();
        MIndexSpace<com.synexia.mindex.MIndexObject<MIndexString>> first =
                MIndexSpaces.fromDomain(legacy);
        MIndexSpace<com.synexia.mindex.MIndexObject<MIndexString>> second =
                MIndexSpaces.fromDomain(legacy);

        assertSame(legacy, first.identityAuthority());
        assertSame(legacy, second.identityAuthority());

        var value =
                com.synexia.mindex.MIndexObjects.index(
                        MIndexString.literal(
                                "legacy-object-authority"));
        int id = first.id(value);
        assertEquals(id, second.id(value));

        MIndexCompositeIndex index =
                new MIndexCompositeIndex();
        MIndexFrozenTuple<com.synexia.mindex.MIndexObject<MIndexString>>
                left =
                MIndexFrozenTuple.ofIds(
                        first, index, new int[] {id});
        MIndexFrozenTuple<com.synexia.mindex.MIndexObject<MIndexString>>
                right =
                MIndexFrozenTuple.ofIds(
                        second, index, new int[] {id});
        assertEquals(left, right);
    }
    @Test
    void bagAndOrderedSetExposeOnlySafeReadOnlyJavaViews() {
        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();
        MIndexCompositeIndex index =
                new MIndexCompositeIndex();
        MIndexString alpha =
                MIndexString.literal("view-alpha");
        MIndexString beta =
                MIndexString.literal("view-beta");

        MIndexFrozenBag<MIndexString> bag =
                MIndexFrozenBag.ofIdentityCounts(
                        strings,
                        index,
                        new int[] {
                            alpha.contentIndex(), 4,
                            beta.contentIndex(), 2
                        });
        java.util.Map<MIndexString, Integer> counts =
                MIndexJavaViews.readOnlyBagCounts(bag);
        assertEquals(2, counts.size());
        assertEquals(4, counts.get(alpha));
        assertEquals(2, counts.get(beta));
        assertThrows(
                UnsupportedOperationException.class,
                () -> counts.put(alpha, 9));

        MIndexFrozenOrderedSet<MIndexString> ordered =
                MIndexFrozenOrderedSet.ofIds(
                        strings,
                        index,
                        new int[] {
                            beta.contentIndex(),
                            alpha.contentIndex()
                        });
        java.util.Set<MIndexString> view =
                MIndexJavaViews.readOnlySet(ordered);
        assertEquals(
                java.util.List.of(beta, alpha),
                java.util.List.copyOf(view));
        assertThrows(
                UnsupportedOperationException.class,
                () -> view.add(
                        MIndexString.literal("forbidden")));

        MIndexObjectSpace<LegacyValue> objects =
                MIndexSpaces.values();
        int objectId =
                objects.id(
                        new LegacyValue(
                                MIndexString.literal("object-view"),
                                1));
        MIndexFrozenBag<LegacyValue> objectBag =
                MIndexFrozenBag.ofIdentityCounts(
                        objects,
                        index,
                        new int[] {objectId, 1});
        MIndexFrozenOrderedSet<LegacyValue> objectOrdered =
                MIndexFrozenOrderedSet.ofIds(
                        objects,
                        index,
                        new int[] {objectId});

        assertThrows(
                IllegalArgumentException.class,
                () -> MIndexJavaViews.readOnlyBagCounts(objectBag));
        assertThrows(
                IllegalArgumentException.class,
                () -> MIndexJavaViews.readOnlySet(objectOrdered));
    }

    @Test
    void genericLegacyInterfacesPreserveSemanticKindDuringPromotion() {
        MIndexStringDomain domain =
                MIndexStringDomain.INSTANCE;
        MIndexCompositeIndex index =
                new MIndexCompositeIndex();
        MIndexString alpha =
                MIndexString.literal("interface-alpha");
        MIndexString beta =
                MIndexString.literal("interface-beta");

        com.synexia.mindex.collection.MIndexSet<MIndexString>
                orderedInterface =
                MIndexCollections.set(
                        domain,
                        MIndexCollectionSpec.ordered(4));
        orderedInterface.add(beta);
        orderedInterface.add(alpha);
        MIndexCanonicalCollection ordered =
                MIndexLegacyCollections.canonicalize(
                        orderedInterface, index);
        assertTrue(
                ordered instanceof MIndexFrozenOrderedSet<?>);
        MIndexFrozenOrderedSet<?> orderedValue =
                (MIndexFrozenOrderedSet<?>) ordered;
        assertEquals(beta, orderedValue.get(0));
        assertEquals(alpha, orderedValue.get(1));

        com.synexia.mindex.collection.MIndexSet<MIndexString>
                plainInterface =
                MIndexCollections.set(
                        domain,
                        MIndexCollectionSpec.general(4));
        plainInterface.add(beta);
        plainInterface.add(alpha);
        MIndexCanonicalCollection plain =
                MIndexLegacyCollections.canonicalize(
                        plainInterface, index);
        assertTrue(plain instanceof MIndexFrozenSet<?>);

        com.synexia.mindex.collection.MIndexMap<
                MIndexString, MIndexString> mapInterface =
                MIndexCollections.map(
                        domain,
                        domain,
                        MIndexCollectionSpec.general(4));
        mapInterface.put(alpha, beta);
        MIndexFrozenMap<MIndexString, MIndexString> map =
                MIndexLegacyCollections.canonicalize(
                        mapInterface, index);
        assertEquals(beta, map.get(alpha));
    }

}
