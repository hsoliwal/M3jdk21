package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.M3BoundedProviderBackedPrimitiveRows;
import jdk.internal.m3.collections.M3FuzzyProviderCollectionFactory;
import jdk.internal.m3.collections.M3PagedPrimitiveTreeProvider;
import jdk.internal.m3.collections.M3PrimitiveRowProvider;
import jdk.internal.m3.collections.M3ProviderCollectionRequirements;
import jdk.internal.m3.collections.M3ProviderLoadException;
import jdk.internal.m3.collections.M3VersionedCursorPagedPrimitiveTreeProvider;
import jdk.internal.m3.collections.M3VersionedProviderBackedPrimitiveLongTree;

class M3ProviderResidencyVersionedTreeTest {

    @Test
    void boundedRowsUseClockWithoutPerRowCacheNodes() {
        int[] loads = {0};
        M3PrimitiveRowProvider provider = (rowId, target) -> {
            loads[0]++;
            target[0] = rowId * 10L;
        };
        M3BoundedProviderBackedPrimitiveRows rows =
                new M3BoundedProviderBackedPrimitiveRows(2, provider, M3PrimitiveKind.LONG);

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

        for (var field : M3BoundedProviderBackedPrimitiveRows.class.getDeclaredFields()) {
            assertFalse(field.getType().getSimpleName().contains("Node"), field.toString());
            assertFalse(java.util.Map.class.isAssignableFrom(field.getType()), field.toString());
            assertFalse(java.util.Collection.class.isAssignableFrom(field.getType()), field.toString());
        }
    }

    @Test
    void failedMissDoesNotEvictAResidentRow() {
        M3PrimitiveRowProvider provider = (rowId, target) -> {
            if (rowId == 2L) throw new IllegalStateException("boom");
            target[0] = rowId;
        };
        M3BoundedProviderBackedPrimitiveRows rows =
                new M3BoundedProviderBackedPrimitiveRows(1, provider, M3PrimitiveKind.LONG);

        assertEquals(1L, rows.getBits(0, 1L));
        assertThrows(M3ProviderLoadException.class, () -> rows.getBits(0, 2L));

        assertTrue(rows.isMaterialized(1L));
        assertTrue(rows.isResident(1L));
        assertEquals(1, rows.residentCount());
        assertEquals(1L, rows.stats().loadFailures());
        assertEquals(0L, rows.stats().policyEvictions());
    }

    @Test
    void allPinnedResidentsRejectAdmissionAndRestoreBound() {
        M3PrimitiveRowProvider provider = (rowId, target) -> target[0] = rowId;
        M3BoundedProviderBackedPrimitiveRows rows =
                new M3BoundedProviderBackedPrimitiveRows(2, provider, M3PrimitiveKind.LONG);

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
        M3PrimitiveRowProvider provider = (rowId, target) -> {
            attempt[0]++;
            if (rowId == 7L && attempt[0] == 1) throw new IllegalStateException("retry");
            target[0] = 77L;
        };
        M3BoundedProviderBackedPrimitiveRows rows =
                new M3BoundedProviderBackedPrimitiveRows(2, provider, M3PrimitiveKind.LONG);

        assertThrows(M3ProviderLoadException.class, () -> rows.touch(7L));
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
        M3VersionedProviderBackedPrimitiveLongTree tree =
                new M3VersionedProviderBackedPrimitiveLongTree(
                        13, 8, 12,
                        provider, 2, M3PrimitiveKind.LONG);

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
        M3FuzzyProviderCollectionFactory factory = M3FuzzyProviderCollectionFactory.standard();

        M3ProviderCollectionRequirements boundedRows =
                M3ProviderCollectionRequirements.builder(
                                M3ProviderCollectionRequirements.Purpose.ROWS,
                                M3PrimitiveKind.LONG,
                                M3PrimitiveKind.INT)
                        .maximumLogicalId(1_000_000L)
                        .maxResidentRows(1024)
                        .build();
        assertEquals("bounded-provider-rows", factory.plan(boundedRows).candidateId());
        assertInstanceOf(
                M3BoundedProviderBackedPrimitiveRows.class,
                factory.createRows(boundedRows, (rowId, target) -> target[0] = rowId));

        M3ProviderCollectionRequirements offsetTree =
                M3ProviderCollectionRequirements.builder(
                                M3ProviderCollectionRequirements.Purpose.TREE,
                                M3PrimitiveKind.LONG)
                        .maximumLogicalId(1_000_000L)
                        .paging(M3ProviderCollectionRequirements.Paging.OFFSET)
                        .pageSize(64)
                        .build();
        assertEquals("offset-provider-tree", factory.plan(offsetTree).candidateId());

        M3ProviderCollectionRequirements versionedTree =
                M3ProviderCollectionRequirements.builder(
                                M3ProviderCollectionRequirements.Purpose.TREE,
                                M3PrimitiveKind.LONG)
                        .maximumLogicalId(6_000_000_000L)
                        .paging(M3ProviderCollectionRequirements.Paging.VERSIONED_CURSOR)
                        .pageSize(128)
                        .build();
        assertEquals(
                M3ProviderCollectionRequirements.IdWidth.LONG64,
                factory.plan(versionedTree).resolvedIdWidth());
        assertEquals(
                "versioned-cursor-provider-long-tree",
                factory.plan(versionedTree).candidateId());

        M3ProviderCollectionRequirements impossible =
                M3ProviderCollectionRequirements.builder(
                                M3ProviderCollectionRequirements.Purpose.TREE,
                                M3PrimitiveKind.LONG)
                        .maximumLogicalId(6_000_000_000L)
                        .paging(M3ProviderCollectionRequirements.Paging.OFFSET)
                        .pageSize(10)
                        .build();
        assertThrows(IllegalArgumentException.class, () -> factory.plan(impossible));
    }

    private static final class VersionedModelProvider
            implements M3VersionedCursorPagedPrimitiveTreeProvider {
        long currentVersion = 1L;

        @Override
        public void load(long rowId, long[] target) {
            target[0] = rowId;
        }

        @Override
        public M3PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) {
            return nodeId == 100L
                    ? M3PagedPrimitiveTreeProvider.ChildHint.SOME
                    : M3PagedPrimitiveTreeProvider.ChildHint.NONE;
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
