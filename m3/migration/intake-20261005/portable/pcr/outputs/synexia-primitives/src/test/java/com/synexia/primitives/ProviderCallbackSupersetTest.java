// SPDX-License-Identifier: Apache-2.0
package com.synexia.primitives;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ProviderCallbackSupersetTest {
    private static final long WARM = 10L;
    private static final long OUTER = 20L;
    private static final long COLD = 900L;
    private static final short APPLICATION = LazyElementFlags.APPLICATION_3;

    @Test
    void observerEnabledRowsAllowCachedReadsAcrossEveryCallbackPhase() {
        for (Phase phase : Phase.values()) {
            ObservingRowsProvider provider = new ObservingRowsProvider(phase);
            ProviderBackedPrimitiveRows rows = new ProviderBackedPrimitiveRows(
                    2, 2, 6, provider, PrimitiveKind.LONG, PrimitiveKind.LONG);
            provider.rows = rows;
            assertEquals(1, rows.prefetchMaterialize(WARM, 1));
            rows.applicationFlags(WARM, APPLICATION);
            rows.pin(WARM, true);
            provider.armed = true;

            if (provider.failingLoad()) {
                ProviderLoadException failure = assertThrows(
                        ProviderLoadException.class, () -> rows.touch(OUTER), phase.name());
                assertSame(provider.failure, failure.getCause(), phase.name());
                assertFalse(rows.isMaterialized(OUTER), phase.name());
                assertTrue(LazyElementFlags.has(rows.flags(OUTER), LazyElementFlags.FAILED));
            } else {
                assertTrue(rows.touch(OUTER), phase.name());
                assertTrue(rows.isMaterialized(OUTER), phase.name());
            }

            assertEquals(1, provider.cachedChecks, phase.name());
            assertEquals(1, provider.beforeTouches, phase.name());
            assertEquals(provider.failingLoad() ? 0 : 1, provider.afterTouches, phase.name());
            assertEquals(provider.failingLoad() ? 1 : 0, provider.touchFailures, phase.name());
            assertEquals(2, provider.loads, "only warm preparation and outer load: " + phase);
            assertEquals(0, rows.flags(COLD), phase.name());
            assertFalse(rows.isMaterialized(COLD), phase.name());

            provider.armed = false;
            if (provider.failingLoad()) assertTrue(rows.retry(OUTER), phase.name());
            else assertTrue(rows.touch(COLD), phase.name());
        }
    }

    @Test
    void twoExternalWarmReadsEmitTwoTouchPairsAfterSilentPrefetch() {
        ObservingRowsProvider provider = new ObservingRowsProvider(Phase.LOAD);
        ProviderBackedPrimitiveRows rows = new ProviderBackedPrimitiveRows(
                2, 2, 6, provider, PrimitiveKind.LONG, PrimitiveKind.LONG);
        provider.rows = rows;

        assertEquals(1, rows.prefetchMaterialize(WARM, 1));
        assertEquals(0, provider.beforeTouches);
        assertEquals(0, provider.afterTouches);
        assertEquals(WARM * 10L, rows.getBits(0, WARM));
        assertEquals(WARM + 100L, rows.getBits(1, WARM));
        assertEquals(2, provider.beforeTouches);
        assertEquals(2, provider.afterTouches);
        assertEquals(1, provider.loads);

        assertEquals(0, rows.prefetchMaterialize(WARM, 1));
        assertEquals(2, provider.beforeTouches);
        assertEquals(2, provider.afterTouches);
    }

    @Test
    void observedTreeRowsHintsAndPagesShareCachedReadAllowance() {
        ObservingTreeProvider provider = new ObservingTreeProvider();
        ProviderBackedPrimitiveTree tree = new ProviderBackedPrimitiveTree(
                2, 2, 6, provider, 2, PrimitiveKind.LONG);
        provider.tree = tree;
        assertTrue(tree.touch(90L));
        tree.pin(90L, true);
        provider.beforeTouches = 0;
        provider.afterTouches = 0;
        provider.rowLoads = 0;
        provider.armed = true;

        assertEquals(1, tree.loadNextRootPage());
        assertEquals(1, tree.loadNextChildPage(10L));
        assertEquals(5, provider.cachedChecks,
                "root hint, root page, parent load, child hint and child page");
        assertEquals(1, provider.beforeTouches);
        assertEquals(1, provider.afterTouches);
        assertEquals(1, provider.rowLoads);
        assertEquals(10, tree.firstLoadedRoot());
        assertEquals(20, tree.firstLoadedChild(10L));
        assertEquals(10, tree.parentOf(20L));
        assertFalse(tree.isMaterialized(20L), "discovered child payload stays lazy");
        assertEquals(0, tree.flags(901L));
        assertEquals(0, tree.flags(902L));

        provider.armed = false;
        assertEquals(900L, tree.getBits(0, 90L));
        assertEquals(2, provider.beforeTouches);
        assertEquals(2, provider.afterTouches);
        assertEquals(1, provider.rowLoads);
    }

    private enum Phase {
        BEFORE_TOUCH, BEFORE_LOAD, LOAD, AFTER_LOAD, AFTER_TOUCH,
        ON_LOAD_FAILURE, ON_TOUCH_FAILURE
    }

    private static final class ObservingRowsProvider
            implements PrimitiveRowProvider, PrimitiveTouchObserver {
        private final Phase phase;
        private final IOException failure = new IOException("outer provider failure");
        private ProviderBackedPrimitiveRows rows;
        private boolean armed;
        private int loads;
        private int beforeTouches;
        private int afterTouches;
        private int touchFailures;
        private int cachedChecks;

        private ObservingRowsProvider(Phase phase) {
            this.phase = phase;
        }

        private boolean failingLoad() {
            return phase == Phase.ON_LOAD_FAILURE || phase == Phase.ON_TOUCH_FAILURE;
        }

        @Override
        public void beforeTouch(long rowId, short flags) {
            beforeTouches++;
            check(Phase.BEFORE_TOUCH);
        }

        @Override
        public void beforeLoad(long rowId, short flags) {
            check(Phase.BEFORE_LOAD);
        }

        @Override
        public void load(long rowId, long[] target) throws Exception {
            loads++;
            check(Phase.LOAD);
            target[0] = rowId * 10L;
            target[1] = rowId + 100L;
            if (armed && rowId == OUTER && failingLoad()) throw failure;
        }

        @Override
        public void afterLoad(long rowId, short flags) {
            check(Phase.AFTER_LOAD);
        }

        @Override
        public void afterTouch(long rowId, short flags, boolean materializedNow) {
            afterTouches++;
            check(Phase.AFTER_TOUCH);
        }

        @Override
        public void onLoadFailure(long rowId, Exception observed) {
            assertSame(failure, observed);
            check(Phase.ON_LOAD_FAILURE);
        }

        @Override
        public void onTouchFailure(long rowId, short flags, Exception observed) {
            touchFailures++;
            assertSame(failure, observed);
            check(Phase.ON_TOUCH_FAILURE);
        }

        private void check(Phase current) {
            if (!armed || phase != current) return;
            cachedChecks++;
            int leaves = rows.allocatedFlagLeafCount();
            assertEquals(WARM * 10L, rows.getBits(0, WARM));
            long[] target = new long[2];
            rows.readRow(WARM, target);
            assertArrayEquals(new long[] {WARM * 10L, WARM + 100L}, target);
            assertFalse(rows.touch(WARM));
            rows.prefetch(WARM, WARM);
            assertEquals(APPLICATION, rows.applicationFlags(WARM));
            assertTrue(LazyElementFlags.has(rows.flags(WARM), LazyElementFlags.PINNED));
            assertThrows(IllegalStateException.class, () -> rows.touch(COLD));
            assertThrows(IllegalStateException.class, () -> rows.pin(COLD, true));
            assertThrows(IllegalStateException.class, () -> rows.invalidate(WARM));
            assertEquals(0, rows.flags(COLD));
            assertEquals(leaves, rows.allocatedFlagLeafCount());
        }
    }

    private static final class ObservingTreeProvider
            implements PagedPrimitiveTreeProvider, PrimitiveTouchObserver {
        private ProviderBackedPrimitiveTree tree;
        private boolean armed;
        private int beforeTouches;
        private int afterTouches;
        private int rowLoads;
        private int cachedChecks;

        @Override
        public void beforeTouch(long rowId, short flags) {
            beforeTouches++;
        }

        @Override
        public void afterTouch(long rowId, short flags, boolean materializedNow) {
            afterTouches++;
        }

        @Override
        public void load(long rowId, long[] target) {
            rowLoads++;
            if (armed) checkCached();
            target[0] = rowId * 10L;
        }

        @Override
        public ChildHint rootHint() {
            checkCached();
            return ChildHint.SOME;
        }

        @Override
        public ChildHint childHint(long nodeId) {
            checkCached();
            return ChildHint.SOME;
        }

        @Override
        public long loadRoots(int offset, int limit, long[] rootIds) {
            checkCached();
            assertEquals(0, offset);
            rootIds[0] = 10L;
            return PagedPrimitiveTreeProvider.pageResult(1, -1);
        }

        @Override
        public long loadChildren(long parentId, int offset, int limit, long[] childIds) {
            checkCached();
            assertEquals(10L, parentId);
            assertEquals(0, offset);
            childIds[0] = 20L;
            return PagedPrimitiveTreeProvider.pageResult(1, -1);
        }

        private void checkCached() {
            if (!armed) return;
            cachedChecks++;
            assertEquals(900L, tree.getBits(0, 90L));
            long[] target = new long[1];
            tree.readRow(90L, target);
            assertEquals(900L, target[0]);
            assertFalse(tree.touch(90L));
            assertThrows(IllegalStateException.class, () -> tree.touch(901L));
            assertThrows(IllegalStateException.class, () -> tree.loadNextChildPage(902L));
            assertThrows(IllegalStateException.class, () -> tree.pin(90L, false));
            assertTrue(LazyElementFlags.has(tree.flags(90L), LazyElementFlags.PINNED));
        }
    }
}
