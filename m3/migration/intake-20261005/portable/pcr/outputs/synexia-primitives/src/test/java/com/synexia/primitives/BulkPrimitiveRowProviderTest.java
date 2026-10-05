package com.synexia.primitives;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BulkPrimitiveRowProviderTest {

    @Test
    void bulkPrefetchDeduplicatesPublishesRowsAndEmitsNoTouchEvents() {
        final class Provider implements BulkPrimitiveRowProvider, PrimitiveTouchObserver {
            int scalarLoads;
            int bulkLoads;
            int beforeTouches;
            int afterTouches;

            @Override
            public void load(long rowId, long[] target) {
                scalarLoads++;
                target[0] = rowId * 10L;
                target[1] = rowId + 100L;
            }

            @Override
            public void loadRows(
                    long[] rowIds,
                    int from,
                    int to,
                    int laneCount,
                    long[] rowScratch,
                    long[] rowMajorTarget) {
                bulkLoads++;
                int offset = 0;
                for (int index = from; index < to; index++) {
                    long id = rowIds[index];
                    rowMajorTarget[offset++] = id * 10L;
                    rowMajorTarget[offset++] = id + 100L;
                }
            }

            @Override
            public void beforeTouch(long logicalId, short currentFlags) {
                beforeTouches++;
            }

            @Override
            public void afterTouch(
                    long logicalId,
                    short resultingFlags,
                    boolean materializedNow) {
                afterTouches++;
            }
        }

        Provider provider = new Provider();
        ProviderBackedPrimitiveRows rows =
                new ProviderBackedPrimitiveRows(
                        provider,
                        PrimitiveKind.LONG,
                        PrimitiveKind.LONG);

        long[] requested = {7L, 5L, 7L, 6L};
        assertEquals(3, rows.prefetchMaterialize(requested, 0, requested.length));
        assertEquals(1, provider.bulkLoads);
        assertEquals(0, provider.scalarLoads);
        assertEquals(0, provider.beforeTouches);
        assertEquals(0, provider.afterTouches);
        assertEquals(3L, rows.materializedSize());

        assertEquals(50L, rows.getBits(0, 5L));
        assertEquals(105L, rows.getBits(1, 5L));
        assertEquals(2, provider.beforeTouches, "each actual access is a warm logical touch");
        assertEquals(2, provider.afterTouches);
        assertEquals(0, provider.scalarLoads, "prefetched row must not reload");

        assertEquals(0, rows.prefetchMaterialize(requested, 0, requested.length));
        assertEquals(1, provider.bulkLoads, "fully resident batch must not call provider");
    }

    @Test
    void defaultBulkContractReusesSingleRowScratchWithoutChangingSemantics() {
        final class Provider implements BulkPrimitiveRowProvider {
            int scalarLoads;

            @Override
            public void load(long rowId, long[] target) {
                scalarLoads++;
                target[0] = rowId;
                target[1] = rowId * 2L;
            }
        }

        Provider provider = new Provider();
        ProviderBackedPrimitiveRows rows =
                new ProviderBackedPrimitiveRows(
                        provider,
                        PrimitiveKind.LONG,
                        PrimitiveKind.LONG);

        assertEquals(4, rows.prefetchMaterialize(20L, 4));
        assertEquals(4, provider.scalarLoads, "default bulk method is scalar-compatible");
        for (long id = 20L; id < 24L; id++) {
            assertEquals(id, rows.getBits(0, id));
            assertEquals(id * 2L, rows.getBits(1, id));
        }
        assertEquals(4, provider.scalarLoads, "warm reads must remain resident");
    }

    @Test
    void batchFailureRollsBackEveryRequestedRowAndPreservesApplicationBits() {
        final class Provider implements BulkPrimitiveRowProvider {
            int attempts;

            @Override
            public void load(long rowId, long[] target) {
                target[0] = rowId;
            }

            @Override
            public void loadRows(
                    long[] rowIds,
                    int from,
                    int to,
                    int laneCount,
                    long[] rowScratch,
                    long[] rowMajorTarget) {
                attempts++;
                if (attempts == 1) throw new IllegalStateException("batch failure");
                for (int index = from; index < to; index++) {
                    rowMajorTarget[index - from] = rowIds[index];
                }
            }
        }

        Provider provider = new Provider();
        ProviderBackedPrimitiveRows rows =
                new ProviderBackedPrimitiveRows(provider, PrimitiveKind.LONG);
        short app = (short) (LazyElementFlags.APPLICATION_1 | LazyElementFlags.APPLICATION_3);
        rows.applicationFlags(30L, app);
        rows.pin(31L, true);

        long[] ids = {30L, 31L};
        ProviderLoadException failure =
                assertThrows(
                        ProviderLoadException.class,
                        () -> rows.prefetchMaterialize(ids, 0, ids.length));
        assertEquals("batch failure", failure.getCause().getMessage());
        for (long id : ids) {
            assertFalse(rows.isMaterialized(id));
            assertTrue(LazyElementFlags.has(rows.flags(id), LazyElementFlags.FAILED));
            assertFalse(LazyElementFlags.has(rows.flags(id), LazyElementFlags.LOADING));
        }
        assertEquals(app, rows.applicationFlags(30L));
        assertTrue(LazyElementFlags.has(rows.flags(31L), LazyElementFlags.PINNED));

        rows.invalidate(30L);
        rows.invalidate(31L);
        assertEquals(2, rows.prefetchMaterialize(ids, 0, ids.length));
        assertEquals(30L, rows.getBits(0, 30L));
        assertEquals(31L, rows.getBits(0, 31L));
    }

    @Test
    void providerListMaterializesLargeRangeInBoundedChunks() {
        final class Provider implements BulkPrimitiveRowProvider {
            int bulkCalls;

            @Override
            public void load(long rowId, long[] target) {
                target[0] = rowId;
            }

            @Override
            public void loadRows(
                    long[] rowIds,
                    int from,
                    int to,
                    int laneCount,
                    long[] rowScratch,
                    long[] rowMajorTarget) {
                bulkCalls++;
                for (int index = from; index < to; index++) {
                    rowMajorTarget[index - from] = rowIds[index];
                }
            }
        }

        Provider provider = new Provider();
        ProviderBackedPrimitiveList list =
                new ProviderBackedPrimitiveList(
                        10_000L,
                        provider,
                        PrimitiveKind.LONG);

        assertEquals(9_000L, list.prefetchMaterialize(500L, 9_500L));
        assertEquals(3, provider.bulkCalls, "4096/4096/808 bounded batches");
        assertEquals(9_000L, list.materializedSize());
        assertEquals(500L, list.getBits(0, 500L));
        assertEquals(9_499L, list.getBits(0, 9_499L));
    }

    @Test
    void longTreeUsesSameBulkPayloadContractOverCursorPagedTopology() {
        final class Provider implements CursorPagedPrimitiveTreeProvider,
                BulkPrimitiveRowProvider, PrimitiveTouchObserver {
            int pageLoads;
            int bulkLoads;
            int scalarLoads;
            int touches;

            @Override
            public void load(long rowId, long[] target) {
                scalarLoads++;
                target[0] = rowId * 3L;
            }

            @Override
            public void loadRows(
                    long[] rowIds,
                    int from,
                    int to,
                    int laneCount,
                    long[] rowScratch,
                    long[] rowMajorTarget) {
                bulkLoads++;
                for (int index = from; index < to; index++) {
                    rowMajorTarget[index - from] = rowIds[index] * 3L;
                }
            }

            @Override
            public void beforeTouch(long logicalId, short currentFlags) {
                touches++;
            }

            @Override
            public int loadChildren(
                    long parentId,
                    long cursor,
                    int limit,
                    long[] childIds,
                    long[] nextCursorOut) {
                pageLoads++;
                long[] children = {100_000_000L, 100_000_001L, 100_000_002L};
                int offset = Math.toIntExact(cursor);
                int count = Math.min(limit, children.length - offset);
                for (int index = 0; index < count; index++) {
                    childIds[index] = children[offset + index];
                }
                int next = offset + count;
                nextCursorOut[0] =
                        next == children.length ? END_CURSOR : next;
                return count;
            }
        }

        Provider provider = new Provider();
        ProviderBackedPrimitiveLongTree tree =
                new ProviderBackedPrimitiveLongTree(
                        8, 8, 12,
                        provider,
                        2,
                        PrimitiveKind.LONG);

        assertEquals(3L, tree.ensureLoadedChildren(50_000_000L, 3L, 2));
        int topologyCalls = provider.pageLoads;
        int touchesBeforePrefetch = provider.touches;
        int scalarLoadsBeforePrefetch = provider.scalarLoads;

        assertEquals(3, tree.prefetchLoadedChildren(50_000_000L, 0L, 3));
        assertEquals(topologyCalls, provider.pageLoads);
        assertEquals(touchesBeforePrefetch, provider.touches);
        assertEquals(scalarLoadsBeforePrefetch, provider.scalarLoads);
        assertEquals(2, provider.bulkLoads);

        assertEquals(
                300_000_000L,
                tree.getBits(0, 100_000_000L));
        assertEquals(touchesBeforePrefetch + 1, provider.touches);
        assertEquals(scalarLoadsBeforePrefetch, provider.scalarLoads);
    }

    @Test
    void loadedTreeWindowsBulkPrefetchPayloadWithoutTopologyCallbacksOrTouches() {
        final class Provider implements PagedPrimitiveTreeProvider,
                BulkPrimitiveRowProvider, PrimitiveTouchObserver {
            int pageLoads;
            int bulkLoads;
            int scalarLoads;
            int touches;

            @Override
            public void load(long rowId, long[] target) {
                scalarLoads++;
                target[0] = rowId * 10L;
            }

            @Override
            public void loadRows(
                    long[] rowIds,
                    int from,
                    int to,
                    int laneCount,
                    long[] rowScratch,
                    long[] rowMajorTarget) {
                bulkLoads++;
                for (int index = from; index < to; index++) {
                    rowMajorTarget[index - from] = rowIds[index] * 10L;
                }
            }

            @Override
            public void beforeTouch(long logicalId, short currentFlags) {
                touches++;
            }

            @Override
            public ChildHint rootHint() {
                return ChildHint.SOME;
            }

            @Override
            public ChildHint childHint(long nodeId) {
                return nodeId == 1L ? ChildHint.SOME : ChildHint.NONE;
            }

            @Override
            public long loadRoots(int offset, int limit, long[] rootIds) {
                pageLoads++;
                long[] roots = {1L, 2L, 3L};
                int count = Math.min(limit, roots.length - offset);
                for (int i = 0; i < count; i++) rootIds[i] = roots[offset + i];
                int next = offset + count == roots.length ? -1 : offset + count;
                return PagedPrimitiveTreeProvider.pageResult(count, next);
            }

            @Override
            public long loadChildren(
                    long parentId,
                    int offset,
                    int limit,
                    long[] childIds) {
                pageLoads++;
                if (parentId != 1L) return PagedPrimitiveTreeProvider.pageResult(0, -1);
                long[] children = {10L, 11L, 12L};
                int count = Math.min(limit, children.length - offset);
                for (int i = 0; i < count; i++) childIds[i] = children[offset + i];
                int next = offset + count == children.length ? -1 : offset + count;
                return PagedPrimitiveTreeProvider.pageResult(count, next);
            }
        }

        Provider provider = new Provider();
        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(provider, 2, PrimitiveKind.LONG);

        assertEquals(3, tree.ensureLoadedRoots(3, 2));
        assertEquals(3, tree.loadedRootCount());
        assertEquals(0L, tree.materializedSize(), "topology discovery must not load root payloads");

        assertEquals(3, tree.prefetchLoadedRoots(0, 3));
        assertEquals(2, provider.bulkLoads, "root payloads use page-size bounded batches");
        assertEquals(0, provider.touches);
        assertEquals(0, provider.scalarLoads);

        assertEquals(3, tree.ensureLoadedChildren(1L, 3, 2));
        int topologyCalls = provider.pageLoads;
        int touchesBeforeChildPrefetch = provider.touches;
        assertEquals(3, tree.prefetchLoadedChildren(1L, 0, 3));
        assertEquals(topologyCalls, provider.pageLoads, "payload prefetch must not fetch topology");
        assertEquals(4, provider.bulkLoads);
        assertEquals(touchesBeforeChildPrefetch, provider.touches);

        tree.setExpanded(1L, true);
        int beforeVisibleBulk = provider.bulkLoads;
        int touchesBeforeVisiblePrefetch = provider.touches;
        assertEquals(0, tree.prefetchVisibleLoadedWindow(0, 6),
                "all visible nodes already resident");
        assertEquals(beforeVisibleBulk, provider.bulkLoads);
        assertEquals(touchesBeforeVisiblePrefetch, provider.touches);

        assertEquals(10L, tree.getBits(0, 1L));
        assertEquals(touchesBeforeVisiblePrefetch + 1, provider.touches,
                "actual user read is still observable");
        assertEquals(0, provider.scalarLoads);
    }
}
