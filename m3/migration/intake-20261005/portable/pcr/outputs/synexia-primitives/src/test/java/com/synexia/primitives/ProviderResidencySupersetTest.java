package com.synexia.primitives;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderResidencySupersetTest {

    @Test
    void boundedListKeepsLogicalSizeWhilePayloadResidencyStaysBounded() {
        int[] loads = {0};
        BoundedProviderBackedPrimitiveList list =
                new BoundedProviderBackedPrimitiveList(
                        1_000_000L,
                        2,
                        (rowId, target) -> {
                            loads[0]++;
                            target[0] = rowId * 10L;
                        },
                        PrimitiveKind.LONG);

        assertEquals(1_000_000L, list.size());
        assertEquals(10L, list.getBits(0, 1L));
        assertEquals(20L, list.getBits(0, 2L));
        assertEquals(2, list.residentCount());

        assertEquals(10L, list.getBits(0, 1L)); // cache hit / second chance
        assertEquals(30L, list.getBits(0, 3L)); // forces one payload eviction

        assertEquals(2, list.residentCount());
        assertTrue(list.isResident(3L));
        assertEquals(3, loads[0]);
        assertEquals(1L, list.stats().policyEvictions());
        assertEquals(4L, list.stats().touches());
        assertEquals(1L, list.stats().cacheHits());
        assertThrows(IndexOutOfBoundsException.class, () -> list.getBits(0, 1_000_000L));
    }

    @Test
    void failedListMissDoesNotEvictResidentPayload() {
        PrimitiveRowProvider provider = (rowId, target) -> {
            if (rowId == 2L) throw new IllegalStateException("boom");
            target[0] = rowId;
        };
        BoundedProviderBackedPrimitiveList list =
                new BoundedProviderBackedPrimitiveList(
                        10L, 1, provider, PrimitiveKind.LONG);

        assertEquals(1L, list.getBits(0, 1L));
        assertThrows(ProviderLoadException.class, () -> list.getBits(0, 2L));

        assertTrue(list.isResident(1L));
        assertTrue(list.isMaterialized(1L));
        assertFalse(list.isMaterialized(2L));
        assertEquals(1, list.residentCount());
        assertEquals(0L, list.stats().policyEvictions());
        assertEquals(1L, list.stats().loadFailures());
    }

    @Test
    void boundedOffsetTreeEvictsPayloadButKeepsRelationships() {
        OffsetTreeProvider provider = new OffsetTreeProvider();
        BoundedProviderBackedPrimitiveTree tree =
                new BoundedProviderBackedPrimitiveTree(
                        1, provider, 8, PrimitiveKind.LONG);

        assertEquals(3, tree.loadNextChildPage(10L));
        assertEquals(1, tree.firstLoadedChild(10L));
        assertEquals(2, tree.nextLoadedSibling(1L));
        assertEquals(10, tree.parentOf(1L));
        assertEquals(1L, tree.materializedSize(), "page demand materializes the parent");
        assertTrue(tree.isResident(10L), "the materialized parent joins bounded residency");
        assertEquals(1, provider.rowLoads);

        assertEquals(10L, tree.getBits(0, 1L));
        assertTrue(tree.isResident(1L));
        assertEquals(20L, tree.getBits(0, 2L));

        assertFalse(tree.isMaterialized(1L), "payload must be evicted");
        assertFalse(tree.isResident(1L));
        assertTrue(tree.isMaterialized(2L));
        assertTrue(tree.isResident(2L));

        assertEquals(10, tree.parentOf(1L), "relationship survives payload eviction");
        assertEquals(2, tree.nextLoadedSibling(1L));
        assertEquals(3, tree.loadedChildCount(10L));
        assertEquals(2L, tree.stats().policyEvictions(), "parent and first child were evicted");

        assertEquals(10L, tree.getBits(0, 1L), "evicted payload reloads through provider");
        assertEquals(4, provider.rowLoads, "parent, two child loads, and one child reload");
    }

    @Test
    void allPinnedTreeResidentsRejectNewAdmissionAndRollbackNewPayload() {
        OffsetTreeProvider provider = new OffsetTreeProvider();
        BoundedProviderBackedPrimitiveTree tree =
                new BoundedProviderBackedPrimitiveTree(
                        1, provider, 8, PrimitiveKind.LONG);

        assertEquals(10L, tree.getBits(0, 1L));
        tree.pin(1L, true);

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> tree.getBits(0, 2L));
        assertEquals("PROVIDER_RESIDENCY_ALL_PINNED", failure.getMessage());

        assertTrue(tree.isMaterialized(1L));
        assertTrue(tree.isResident(1L));
        assertFalse(tree.isMaterialized(2L), "failed admission must roll back new payload");
        assertFalse(tree.isResident(2L));
        assertEquals(1, tree.residentCount());
        assertEquals(1L, tree.stats().admissionFailures());
    }

    @Test
    void boundedPrefetchCannotBypassTreeResidencyBudget() {
        OffsetTreeProvider provider = new OffsetTreeProvider();
        BoundedProviderBackedPrimitiveTree tree =
                new BoundedProviderBackedPrimitiveTree(
                        2, provider, 8, PrimitiveKind.LONG);

        assertEquals(3, tree.loadNextChildPage(10L));
        assertEquals(3, tree.prefetchLoadedChildren(10L, 0, 3));

        assertEquals(2, tree.residentCount());
        assertTrue(tree.stats().policyEvictions() >= 1L);
        assertEquals(3, tree.loadedChildCount(10L));
    }

    @Test
    void fuzzyProviderFactorySelectsBoundedListAndEveryBoundedTreePagingMode() {
        FuzzyProviderCollectionFactory factory = FuzzyProviderCollectionFactory.standard();

        ProviderCollectionRequirements listRequirements =
                ProviderCollectionRequirements.builder(
                                ProviderCollectionRequirements.Purpose.LIST,
                                PrimitiveKind.LONG)
                        .logicalSize(1_000_000L)
                        .maximumLogicalId(999_999L)
                        .maxResidentRows(1024)
                        .build();
        assertEquals("bounded-provider-list", factory.plan(listRequirements).candidateId());
        assertInstanceOf(
                BoundedProviderBackedPrimitiveList.class,
                factory.createList(
                        listRequirements,
                        (rowId, target) -> target[0] = rowId));

        ProviderCollectionRequirements offset =
                ProviderCollectionRequirements.builder(
                                ProviderCollectionRequirements.Purpose.TREE,
                                PrimitiveKind.LONG)
                        .maximumLogicalId(1_000_000L)
                        .paging(ProviderCollectionRequirements.Paging.OFFSET)
                        .pageSize(64)
                        .maxResidentRows(512)
                        .build();
        assertEquals("bounded-offset-provider-tree", factory.plan(offset).candidateId());
        assertInstanceOf(
                BoundedProviderBackedPrimitiveTree.class,
                factory.createBoundedOffsetTree(offset, new OffsetTreeProvider()));

        ProviderCollectionRequirements cursor =
                ProviderCollectionRequirements.builder(
                                ProviderCollectionRequirements.Purpose.TREE,
                                PrimitiveKind.LONG)
                        .maximumLogicalId(6_000_000_000L)
                        .paging(ProviderCollectionRequirements.Paging.CURSOR)
                        .pageSize(64)
                        .maxResidentRows(512)
                        .build();
        assertEquals("bounded-cursor-provider-long-tree", factory.plan(cursor).candidateId());
        assertInstanceOf(
                BoundedProviderBackedPrimitiveLongTree.class,
                factory.createBoundedCursorTree(cursor, new CursorTreeProvider()));

        ProviderCollectionRequirements versioned =
                ProviderCollectionRequirements.builder(
                                ProviderCollectionRequirements.Purpose.TREE,
                                PrimitiveKind.LONG)
                        .maximumLogicalId(6_000_000_000L)
                        .paging(ProviderCollectionRequirements.Paging.VERSIONED_CURSOR)
                        .pageSize(64)
                        .maxResidentRows(512)
                        .build();
        assertEquals(
                "bounded-versioned-cursor-provider-long-tree",
                factory.plan(versioned).candidateId());
        assertInstanceOf(
                BoundedVersionedProviderBackedPrimitiveLongTree.class,
                factory.createBoundedVersionedCursorTree(
                        versioned,
                        new VersionedCursorTreeProvider()));
    }

    @Test
    void directFactoryRemainsAvailableWithoutFuzzySelection() {
        ProviderCollectionFactory factory = ProviderCollectionFactory.standard();

        assertInstanceOf(
                BoundedProviderBackedPrimitiveList.class,
                factory.boundedList(
                        100L,
                        8,
                        (rowId, target) -> target[0] = rowId,
                        PrimitiveKind.LONG));

        assertInstanceOf(
                BoundedProviderBackedPrimitiveTree.class,
                factory.boundedTree(
                        8,
                        new OffsetTreeProvider(),
                        16,
                        PrimitiveKind.LONG));
    }

    @Test
    void semanticShortFlagsSurvivePayloadEviction() {
        BoundedProviderBackedPrimitiveList list =
                new BoundedProviderBackedPrimitiveList(
                        10L,
                        1,
                        (rowId, target) -> target[0] = rowId,
                        PrimitiveKind.LONG);

        short application =
                (short) (LazyElementFlags.APPLICATION_0 | LazyElementFlags.APPLICATION_3);
        list.applicationFlags(1L, application);
        list.getBits(0, 1L);
        list.getBits(0, 2L); // evicts index 1

        assertEquals(application, list.applicationFlags(1L));
        assertTrue(LazyElementFlags.has(list.flags(1L), LazyElementFlags.INVALIDATED));
        assertFalse(LazyElementFlags.has(list.flags(1L), LazyElementFlags.MATERIALIZED));
    }

    private static final class OffsetTreeProvider implements PagedPrimitiveTreeProvider {
        int rowLoads;

        @Override
        public void load(long rowId, long[] target) {
            rowLoads++;
            target[0] = rowId * 10L;
        }

        @Override
        public ChildHint childHint(long nodeId) {
            return nodeId == 10L ? ChildHint.SOME : ChildHint.NONE;
        }

        @Override
        public long loadChildren(long parentId, int offset, int limit, long[] childIds) {
            long[] children = parentId == 10L
                    ? new long[] {1L, 2L, 3L}
                    : new long[0];
            int count = Math.min(limit, Math.max(0, children.length - offset));
            for (int i = 0; i < count; i++) childIds[i] = children[offset + i];
            int next = offset + count == children.length ? -1 : offset + count;
            return PagedPrimitiveTreeProvider.pageResult(count, next);
        }
    }

    private static class CursorTreeProvider implements CursorPagedPrimitiveTreeProvider {
        @Override
        public void load(long rowId, long[] target) {
            target[0] = rowId;
        }

        @Override
        public int loadChildren(
                long parentId,
                long cursor,
                int limit,
                long[] childIds,
                long[] nextCursorOut) {
            nextCursorOut[0] = END_CURSOR;
            return 0;
        }
    }

    private static final class VersionedCursorTreeProvider
            implements VersionedCursorPagedPrimitiveTreeProvider {
        @Override
        public void load(long rowId, long[] target) {
            target[0] = rowId;
        }

        @Override
        public long childVersion(long parentId) {
            return 1L;
        }

        @Override
        public int loadChildren(
                long parentId,
                long versionToken,
                long cursor,
                int limit,
                long[] childIds,
                long[] nextCursorOut) {
            nextCursorOut[0] = END_CURSOR;
            return 0;
        }
    }
}
