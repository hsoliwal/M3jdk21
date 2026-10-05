// SPDX-License-Identifier: Apache-2.0
package com.synexia.primitives;

import java.io.IOException;

/** Dependency-free runtime receipt for the current callback/residency/version composition. */
public final class ProviderCallbackSupersetProbe {
    private ProviderCallbackSupersetProbe() { }

    public static void main(String[] args) {
        CallbackProvider callback = new CallbackProvider();
        ProviderBackedPrimitiveRows rows = new ProviderBackedPrimitiveRows(
                2, 2, 6, callback, PrimitiveKind.LONG);
        callback.rows = rows;
        check(rows.prefetchMaterialize(1L, 1) == 1, "warm prefetch");
        check(rows.touch(2L), "outer demand");
        check(callback.cachedReads == 1, "internal cached read");
        check(callback.beforeTouches == 1 && callback.afterTouches == 1,
                "internal read did not recursively notify");
        check(rows.getBits(0, 1L) == 10L && rows.getBits(0, 1L) == 10L, "warm values");
        check(callback.beforeTouches == 3 && callback.afterTouches == 3,
                "two external warm accesses remain observable");
        check(callback.loads == 2, "warm accesses did not reload");

        FailingObserver provider = new FailingObserver();
        BoundedProviderBackedPrimitiveRows bounded = new BoundedProviderBackedPrimitiveRows(
                2, 2, 6, 1, provider, PrimitiveKind.LONG);
        check(bounded.touch(1L), "incumbent load");
        bounded.applicationFlags(2L, LazyElementFlags.APPLICATION_3);
        try {
            bounded.touch(2L);
            throw new AssertionError("expected after-touch failure");
        } catch (ProviderLoadException failure) {
            check(failure.getCause() == provider.failure, "primary callback cause");
        }
        check(bounded.residentCount() == 1 && bounded.materializedSize() == 1L,
                "failure admission keeps residency ledger complete");
        check(bounded.isResident(2L) && !bounded.isResident(1L), "CLOCK admission");
        check(bounded.applicationFlags(2L) == LazyElementFlags.APPLICATION_3,
                "application flags follow logical ID");
        check(bounded.stats().loads() == 1L && bounded.stats().loadFailures() == 1L,
                "failed-demand statistic policy");
        bounded.pin(2L, true);
        bounded.pin(3L, true);
        try {
            bounded.touch(3L);
            throw new AssertionError("expected pinned admission refusal");
        } catch (IllegalStateException failure) {
            check("PROVIDER_RESIDENCY_ALL_PINNED".equals(failure.getMessage()),
                    "admission refusal contract");
        }
        check(!bounded.isMaterialized(3L) && !bounded.isResident(3L),
                "pre-pinned unadmitted payload rolled back");
        check(LazyElementFlags.has(bounded.flags(3L), LazyElementFlags.PINNED),
                "rollback preserves pin metadata");
        check(bounded.isResident(2L) && bounded.residentCount() == 1,
                "pinned incumbent survives");

        VersionProvider versions = new VersionProvider();
        VersionedProviderBackedPrimitiveLongTree tree =
                new VersionedProviderBackedPrimitiveLongTree(
                        2, 2, 6, versions, 1, PrimitiveKind.LONG);
        check(tree.loadNextChildPage(1L) == 1, "versioned page");
        check(tree.loadNextChildPage(1L) == 0, "completed page");
        check(tree.versionStats().versionLookups() == 1L, "one initial version pin");
        tree.invalidateChildren(1L);
        check(tree.loadNextChildPage(1L) == 1, "new versioned branch");
        check(tree.versionStats().versionLookups() == 2L, "second pin after invalidation");
        check(tree.firstLoadedChild(1L) == 201L, "new version's child identity");

        System.out.println("{\"internalCachedReads\":" + callback.cachedReads
                + ",\"externalTouchPairs\":" + callback.afterTouches
                + ",\"rowProviderLoads\":" + callback.loads
                + ",\"residentRows\":" + bounded.residentCount()
                + ",\"admissionFailures\":" + bounded.stats().admissionFailures()
                + ",\"versionLookups\":" + tree.versionStats().versionLookups() + "}");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class CallbackProvider
            implements PrimitiveRowProvider, PrimitiveTouchObserver {
        private ProviderBackedPrimitiveRows rows;
        private int loads;
        private int cachedReads;
        private int beforeTouches;
        private int afterTouches;

        @Override
        public void load(long id, long[] target) {
            loads++;
            if (id == 2L) {
                check(rows.getBits(0, 1L) == 10L, "provider's cached row value");
                cachedReads++;
            }
            target[0] = id * 10L;
        }

        @Override
        public void beforeTouch(long id, short flags) {
            beforeTouches++;
        }

        @Override
        public void afterTouch(long id, short flags, boolean loaded) {
            afterTouches++;
        }
    }

    private static final class FailingObserver
            implements PrimitiveRowProvider, PrimitiveTouchObserver {
        private final IOException failure = new IOException("after-touch failure");

        @Override
        public void load(long id, long[] target) {
            target[0] = id * 10L;
        }

        @Override
        public void afterTouch(long id, short flags, boolean loaded) throws Exception {
            if (id == 2L) throw failure;
        }
    }

    private static final class VersionProvider implements VersionedCursorPagedPrimitiveTreeProvider {
        private long version;

        @Override
        public void load(long id, long[] target) {
            target[0] = id;
        }

        @Override
        public long childVersion(long parentId) {
            return ++version;
        }

        @Override
        public int loadChildren(
                long parentId, long versionToken, long cursor, int limit,
                long[] childIds, long[] nextCursorOut) {
            childIds[0] = versionToken * 100L + parentId;
            nextCursorOut[0] = END_CURSOR;
            return 1;
        }
    }
}
