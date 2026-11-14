package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.BoundedProviderBackedPrimitiveRows;
import jdk.internal.m3.collections.FuzzyProviderCollectionFactory;
import jdk.internal.m3.collections.PagedPrimitiveTreeProvider;
import jdk.internal.m3.collections.PrimitiveRowProvider;
import jdk.internal.m3.collections.ProviderCollectionRequirements;
import jdk.internal.m3.collections.ProviderLoadException;
import jdk.internal.m3.collections.VersionedCursorPagedPrimitiveTreeProvider;
import jdk.internal.m3.collections.VersionedProviderBackedPrimitiveLongTree;

class ProviderResidencyVersionedTreeTest {

    @Test
    void boundedRowsUseClockWithoutPerRowCacheNodes() {
        int[] loads = {0};
        PrimitiveRowProvider provider = (rowId, target) -> {
            loads[0]++;
            target[0] = rowId * 10L;
        };
        BoundedProviderBackedPrimitiveRows rows =
                new BoundedProviderBackedPrimitiveRows(2, provider, PrimitiveKind.LONG);

        assertEquals(10L, rows.getBits(0, 1L));
        assertEquals(20L, rows.getBits(0, 2L));
        assertEquals(2, rows.residentCount());
        assertEquals(10L, rows.getBits(0, 1L)); // cache hit / second chance

        assertEquals(30L, rows.getBits(0, 3L));
        assertEquals(2, rows.residentCount());
        assertEquals(3, loads[0]);
        assertTrue(rows.isResident(3L));
        assertEquals(1L, rows.stats().policyEvictions());
        assertEquals(4L, rows.stats().touches());
        assertEquals(1L, rows.stats().cacheHits());

        for (var field : BoundedProviderBackedPrimitiveRows.class.getDeclaredFields()) {
            assertFalse(field.getType().getSimpleName().contains("Node"), field.toString());
            assertFalse(java.util.Map.class.isAssignableFrom(field.getType()), field.toString());
            assertFalse(java.util.Collection.class.isAssignableFrom(field.getType()), field.toString());
        }
    }

    @Test
    void failedMissDoesNotEvictAResidentRow() {
        PrimitiveRowProvider provider = (rowId, target) -> {
            if (rowId == 2L) throw new IllegalStateException("boom");
            target[0] = rowId;
        };
        BoundedProviderBackedPrimitiveRows rows =
                new BoundedProviderBackedPrimitiveRows(1, provider, PrimitiveKind.LONG);

        assertEquals(1L, rows.getBits(0, 1L));
        assertThrows(ProviderLoadException.class, () -> rows.getBits(0, 2L));

        assertTrue(rows.isMaterialized(1L));
        assertTrue(rows.isResident(1L));
        assertEquals(1, rows.residentCount());
        assertEquals(1L, rows.stats().loadFailures());
        assertEquals(0L, rows.stats().policyEvictions());
    }

    @Test
    void allPinnedResidentsRejectAdmissionAndRestoreBound() {
        PrimitiveRowProvider provider = (rowId, target) -> target[0] = rowId;
        BoundedProviderBackedPrimitiveRows rows =
                new BoundedProviderBackedPrimitiveRows(2, provider, PrimitiveKind.LONG);

        rows.getBits(0, 1L);
        rows.getBits(0, 2L);
        rows.pin(1L, true);
        rows.pin(2L, true);

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> rows.getBits(0, 3L));
        assertEquals("PROVIDER_RESIDENCY_ALL_PINNED", failure.getMessage());
        assertEquals(2, rows.residentCount());
        assertTrue(rows.isResident(1L));
        assertTrue(rows.isResident(2L));
        assertFalse(rows.isMaterialized(3L));
        assertEquals(1L, rows.stats().admissionFailures());
    }

    @Test
    void explicitInvalidateAndRetryKeepResidencyLedgerCoherent() {
        int[] attempt = {0};
        PrimitiveRowProvider provider = (rowId, target) -> {
            attempt[0]++;
            if (rowId == 7L && attempt[0] == 1) throw new IllegalStateException("retry");
            target[0] = 77L;
        };
        BoundedProviderBackedPrimitiveRows rows =
                new BoundedProviderBackedPrimitiveRows(2, provider, PrimitiveKind.LONG);

        assertThrows(ProviderLoadException.class, () -> rows.touch(7L));
        assertTrue(rows.retry(7L));
        assertTrue(rows.isResident(7L));
        assertEquals(77L, rows.getBits(0, 7L));
        assertTrue(rows.invalidate(7L));
        assertFalse(rows.isResident(7L));
        assertEquals(0, rows.residentCount());
        assertEquals(1L, rows.stats().retries());
        assertEquals(1L, rows.stats().invalidations());
    }

    @Test
    void versionedTreePinsOneVersionAcrossAllPagesUntilInvalidation() {
        VersionedModelProvider provider = new VersionedModelProvider();
        VersionedProviderBackedPrimitiveLongTree tree =
                new VersionedProviderBackedPrimitiveLongTree(
                        13, 8, 12,
                        provider, 2, PrimitiveKind.LONG);

        assertEquals(2, tree.loadNextChildPage(100L));
        assertTrue(tree.hasPinnedChildVersion(100L));
        assertEquals(1L, tree.pinnedChildVersionOrDefault(100L, -1L));

        provider.currentVersion = 2L; // backend advances between pages
        assertEquals(1, tree.loadNextChildPage(100L));

        List<Long> children = new ArrayList<>();
        tree.forEachLoadedChild(100L, children::add);
        assertEquals(List.of(1L, 2L, 3L), children, "all pages must come from pinned v1");
        assertEquals(1L, tree.versionStats().versionLookups());
        assertEquals(2L, tree.versionStats().childPageLoads());

        tree.invalidateChildren(100L);
        assertFalse(tree.hasPinnedChildVersion(100L));
        assertEquals(2, tree.loadNextChildPage(100L));
        children.clear();
        tree.forEachLoadedChild(100L, children::add);
        assertEquals(List.of(10L, 20L), children, "new cache lifetime must pin v2");
        assertEquals(2L, tree.versionStats().versionLookups());
    }

    @Test
    void fuzzyProviderFactorySelectsOnlyExactProviderSemantics() {
        FuzzyProviderCollectionFactory factory = FuzzyProviderCollectionFactory.standard();

        ProviderCollectionRequirements boundedRows =
                ProviderCollectionRequirements.builder(
                                ProviderCollectionRequirements.Purpose.ROWS,
                                PrimitiveKind.LONG,
                                PrimitiveKind.INT)
                        .maximumLogicalId(1_000_000L)
                        .maxResidentRows(1024)
                        .build();
        assertEquals("bounded-provider-rows", factory.plan(boundedRows).candidateId());
        assertInstanceOf(
                BoundedProviderBackedPrimitiveRows.class,
                factory.createRows(boundedRows, (rowId, target) -> target[0] = rowId));

        ProviderCollectionRequirements offsetTree =
                ProviderCollectionRequirements.builder(
                                ProviderCollectionRequirements.Purpose.TREE,
                                PrimitiveKind.LONG)
                        .maximumLogicalId(1_000_000L)
                        .paging(ProviderCollectionRequirements.Paging.OFFSET)
                        .pageSize(64)
                        .build();
        assertEquals("offset-provider-tree", factory.plan(offsetTree).candidateId());

        ProviderCollectionRequirements versionedTree =
                ProviderCollectionRequirements.builder(
                                ProviderCollectionRequirements.Purpose.TREE,
                                PrimitiveKind.LONG)
                        .maximumLogicalId(6_000_000_000L)
                        .paging(ProviderCollectionRequirements.Paging.VERSIONED_CURSOR)
                        .pageSize(128)
                        .build();
        assertEquals(
                ProviderCollectionRequirements.IdWidth.LONG64,
                factory.plan(versionedTree).resolvedIdWidth());
        assertEquals(
                "versioned-cursor-provider-long-tree",
                factory.plan(versionedTree).candidateId());

        ProviderCollectionRequirements impossible =
                ProviderCollectionRequirements.builder(
                                ProviderCollectionRequirements.Purpose.TREE,
                                PrimitiveKind.LONG)
                        .maximumLogicalId(6_000_000_000L)
                        .paging(ProviderCollectionRequirements.Paging.OFFSET)
                        .pageSize(10)
                        .build();
        assertThrows(IllegalArgumentException.class, () -> factory.plan(impossible));
    }

    private static final class VersionedModelProvider
            implements VersionedCursorPagedPrimitiveTreeProvider {
        long currentVersion = 1L;

        @Override
        public void load(long rowId, long[] target) {
            target[0] = rowId;
        }

        @Override
        public PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) {
            return nodeId == 100L
                    ? PagedPrimitiveTreeProvider.ChildHint.SOME
                    : PagedPrimitiveTreeProvider.ChildHint.NONE;
        }

        @Override
        public long childVersion(long parentId) {
            return currentVersion;
        }

        @Override
        public int loadChildren(
                long parentId,
                long versionToken,
                long cursor,
                int limit,
                long[] childIds,
                long[] nextCursorOut) {
            long[] source = versionToken == 1L
                    ? new long[] {1L, 2L, 3L}
                    : new long[] {10L, 20L, 30L, 40L};
            int offset = Math.toIntExact(cursor);
            int count = Math.min(limit, source.length - offset);
            for (int i = 0; i < count; i++) childIds[i] = source[offset + i];
            int next = offset + count;
            nextCursorOut[0] = next == source.length ? END_CURSOR : next;
            return count;
        }
    }
}
