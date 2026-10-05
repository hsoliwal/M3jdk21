// SPDX-License-Identifier: Apache-2.0
package com.synexia.primitives;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class VersionedProviderLookupCounterTest {
    @Test
    void versionIsCountedOncePerPinnedBranchAcrossPagesAndInvalidation() {
        VersionedProvider provider = new VersionedProvider();
        VersionedProviderBackedPrimitiveLongTree tree = tree(provider);

        assertEquals(1, tree.loadNextChildPage(1L));
        assertTrue(tree.hasPinnedChildVersion(1L));
        assertEquals(7L, tree.pinnedChildVersionOrDefault(1L, -1L));
        assertEquals(1L, tree.versionStats().versionLookups());
        assertEquals(1L, tree.versionStats().childPageLoads());
        assertEquals(1L, tree.versionStats().pinnedBranches());

        provider.version = 8L;
        assertEquals(1, tree.loadNextChildPage(1L));
        assertEquals(700L, tree.firstLoadedChild(1L));
        assertEquals(701L, tree.nextLoadedSibling(700L));
        assertEquals(1L, tree.parentOf(701L));
        assertTrue(tree.childrenComplete(1L));
        assertEquals(1, provider.versionCalls);
        assertEquals(1L, tree.versionStats().versionLookups());
        assertEquals(2L, tree.versionStats().childPageLoads());

        tree.invalidateChildren(1L);
        assertFalse(tree.hasPinnedChildVersion(1L));
        assertEquals(-1L, tree.pinnedChildVersionOrDefault(1L, -1L));
        assertEquals(0L, tree.versionStats().pinnedBranches());
        assertEquals(1, tree.loadNextChildPage(1L));
        assertEquals(800L, tree.firstLoadedChild(1L));
        assertEquals(8L, tree.pinnedChildVersionOrDefault(1L, -1L));
        assertEquals(2, provider.versionCalls);
        assertEquals(2L, tree.versionStats().versionLookups());
        assertEquals(3L, tree.versionStats().childPageLoads());
        assertEquals(1L, tree.versionStats().pinnedBranches());
    }

    @Test
    void failedVersionLookupDoesNotIncrementOrPublishVersionState() {
        VersionedProvider provider = new VersionedProvider();
        VersionedProviderBackedPrimitiveLongTree tree = tree(provider);
        provider.failVersion = true;

        ProviderLoadException failure = assertThrows(
                ProviderLoadException.class, () -> tree.loadNextChildPage(1L));
        assertSame(provider.versionFailure, failure.getCause());
        assertFalse(tree.hasPinnedChildVersion(1L));
        assertEquals(0L, tree.versionStats().versionLookups());
        assertEquals(0L, tree.versionStats().childPageLoads());
        assertEquals(0L, tree.versionStats().pinnedBranches());
        assertEquals(0L, tree.loadedChildCount(1L));
        assertEquals(0L, tree.nextChildCursor(1L));
        assertFalse(LazyElementFlags.has(tree.flags(1L), LazyElementFlags.CHILDREN_LOADING));

        provider.failVersion = false;
        assertEquals(1, tree.loadNextChildPage(1L));
        assertEquals(2, provider.versionCalls);
        assertEquals(1L, tree.versionStats().versionLookups());
        assertEquals(1L, tree.versionStats().childPageLoads());
        assertEquals(1L, tree.versionStats().pinnedBranches());
    }

    @Test
    void failedPageKeepsItsAlreadyPinnedVersionWithoutCountingAnotherLookup() {
        VersionedProvider provider = new VersionedProvider();
        VersionedProviderBackedPrimitiveLongTree tree = tree(provider);
        provider.failPage = true;

        ProviderLoadException failure = assertThrows(
                ProviderLoadException.class, () -> tree.loadNextChildPage(1L));
        assertSame(provider.pageFailure, failure.getCause());
        assertTrue(tree.hasPinnedChildVersion(1L));
        assertEquals(7L, tree.pinnedChildVersionOrDefault(1L, -1L));
        assertEquals(1L, tree.versionStats().versionLookups());
        assertEquals(1L, tree.versionStats().childPageLoads());
        assertEquals(0L, tree.loadedChildCount(1L));
        assertEquals(0L, tree.nextChildCursor(1L));
        assertFalse(LazyElementFlags.has(tree.flags(1L), LazyElementFlags.CHILDREN_LOADING));

        provider.failPage = false;
        provider.version = 8L;
        assertEquals(1, tree.loadNextChildPage(1L));
        assertEquals(700L, tree.firstLoadedChild(1L));
        assertEquals(1, provider.versionCalls);
        assertEquals(1L, tree.versionStats().versionLookups());
        assertEquals(2L, tree.versionStats().childPageLoads());
    }

    private static VersionedProviderBackedPrimitiveLongTree tree(VersionedProvider provider) {
        return new VersionedProviderBackedPrimitiveLongTree(
                2, 2, 6, provider, 1, PrimitiveKind.LONG);
    }

    private static final class VersionedProvider
            implements VersionedCursorPagedPrimitiveTreeProvider {
        private long version = 7L;
        private int versionCalls;
        private boolean failVersion;
        private boolean failPage;
        private final IOException versionFailure = new IOException("version lookup failed");
        private final IOException pageFailure = new IOException("versioned page failed");

        @Override
        public void load(long rowId, long[] target) {
            target[0] = rowId * 10L;
        }

        @Override
        public PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) {
            return PagedPrimitiveTreeProvider.ChildHint.SOME;
        }

        @Override
        public long childVersion(long parentId) throws Exception {
            versionCalls++;
            if (failVersion) throw versionFailure;
            return version;
        }

        @Override
        public int loadChildren(
                long parentId,
                long versionToken,
                long cursor,
                int limit,
                long[] childIds,
                long[] nextCursorOut) throws Exception {
            if (failPage) throw pageFailure;
            assertEquals(1, limit);
            assertTrue(cursor == 0L || cursor == 1L);
            childIds[0] = versionToken * 100L + cursor;
            nextCursorOut[0] = cursor == 0L ? 1L : END_CURSOR;
            return 1;
        }
    }
}
