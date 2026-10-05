package com.synexia.primitives;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderBackedPrimitiveTreeSupersetTest {

    @Test
    void rootsPageIndependentlyWithoutMaterializingPayload() {
        ForestProvider provider = new ForestProvider();
        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(provider, 2, PrimitiveKind.LONG);

        assertEquals(PagedPrimitiveTreeProvider.ChildHint.SOME, tree.rootHint());
        assertEquals(2, tree.loadNextRootPage());
        assertEquals(0, provider.rowLoads, "root discovery must not touch payload");
        assertEquals(2, tree.loadedRootCount());
        assertEquals(2, tree.nextRootOffset());

        assertEquals(10, tree.firstLoadedRoot());
        assertEquals(20, tree.lastLoadedRoot());
        assertEquals(20, tree.nextLoadedRoot(10L));
        assertEquals(10, tree.previousLoadedRoot(20L));
        assertTrue(tree.isLoadedRoot(10L));
        assertTrue(tree.isLoadedRoot(20L));

        assertEquals(1, tree.loadNextRootPage());
        assertEquals(0, tree.loadNextRootPage());
        assertEquals(3, tree.loadedRootCount());
        assertEquals(-1, tree.nextRootOffset());
        assertEquals(30, tree.lastLoadedRoot());
        assertEquals(20, tree.previousLoadedRoot(30L));
        assertTrue(LazyElementFlags.has(
                tree.rootFlags(), LazyElementFlags.CHILDREN_COMPLETE));

        List<Long> roots = new ArrayList<>();
        tree.forEachLoadedRoot(roots::add);
        assertEquals(List.of(10L, 20L, 30L), roots);
        assertEquals(0L, tree.materializedSize());
    }

    @Test
    void defaultRootBridgeUsesPseudoParentWithoutChangingOrdinaryChildPaging() {
        long[] seenParent = {Long.MIN_VALUE};
        PagedPrimitiveTreeProvider provider = new PagedPrimitiveTreeProvider() {
            @Override
            public void load(long rowId, long[] target) {
                target[0] = rowId;
            }

            @Override
            public long loadChildren(
                    long parentId, int offset, int limit, long[] childIds) {
                seenParent[0] = parentId;
                childIds[0] = 7L;
                return PagedPrimitiveTreeProvider.pageResult(1, -1);
            }
        };

        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(provider, 4, PrimitiveKind.LONG);
        assertEquals(1, tree.loadNextRootPage());
        assertEquals(PagedPrimitiveTreeProvider.ROOT_PARENT_ID, seenParent[0]);
        assertEquals(7, tree.firstLoadedRoot());
        assertEquals(-1, tree.parentOf(7L));
    }

    @Test
    void childSiblingLinksAreBidirectionalAndInvalidateCleanly() {
        ForestProvider provider = new ForestProvider();
        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(provider, 8, PrimitiveKind.LONG);

        assertEquals(3, tree.loadNextChildPage(10L));
        assertEquals(101, tree.firstLoadedChild(10L));
        assertEquals(103, tree.lastLoadedChild(10L));
        assertEquals(-1, tree.previousLoadedSibling(101L));
        assertEquals(101, tree.previousLoadedSibling(102L));
        assertEquals(102, tree.previousLoadedSibling(103L));
        assertEquals(102, tree.nextLoadedSibling(101L));
        assertEquals(103, tree.nextLoadedSibling(102L));

        tree.invalidateChildren(10L);
        assertEquals(-1, tree.firstLoadedChild(10L));
        assertEquals(-1, tree.lastLoadedChild(10L));
        assertEquals(-1, tree.parentOf(101L));
        assertEquals(-1, tree.previousLoadedSibling(102L));
        assertEquals(-1, tree.nextLoadedSibling(102L));
    }

    @Test
    void nodeCannotBeBothRootAndChild() {
        PagedPrimitiveTreeProvider provider = new PagedPrimitiveTreeProvider() {
            @Override
            public void load(long rowId, long[] target) {
                target[0] = rowId;
            }

            @Override
            public long loadRoots(int offset, int limit, long[] rootIds) {
                rootIds[0] = 9L;
                return PagedPrimitiveTreeProvider.pageResult(1, -1);
            }

            @Override
            public long loadChildren(
                    long parentId, int offset, int limit, long[] childIds) {
                childIds[0] = 9L;
                return PagedPrimitiveTreeProvider.pageResult(1, -1);
            }
        };

        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(provider, 2, PrimitiveKind.LONG);
        assertEquals(1, tree.loadNextRootPage());
        assertThrows(ProviderLoadException.class, () -> tree.loadNextChildPage(1L));
        assertEquals(-1, tree.parentOf(9L));
        assertTrue(tree.isLoadedRoot(9L));
    }

    @Test
    void boundedEnsureLoadsOnlyRequiredPages() {
        ForestProvider provider = new ForestProvider();
        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(provider, 2, PrimitiveKind.LONG);

        assertEquals(2, tree.ensureLoadedRoots(3, 1));
        assertEquals(1, provider.rootPageLoads);
        assertEquals(3, tree.ensureLoadedRoots(3, 1));
        assertEquals(2, provider.rootPageLoads);

        assertEquals(2, tree.ensureLoadedChildren(10L, 3, 1));
        assertEquals(1, provider.childPageLoads);
        assertEquals(3, tree.ensureLoadedChildren(10L, 3, 1));
        assertEquals(2, provider.childPageLoads);
    }

    @Test
    void visibleLoadedCursorUsesExpansionBitsWithoutProviderCalls() {
        ForestProvider provider = new ForestProvider();
        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(provider, 8, PrimitiveKind.LONG);

        tree.loadNextRootPage();
        tree.loadNextRootPage();
        tree.loadNextChildPage(10L);
        tree.loadNextChildPage(101L);
        long materializedBeforeTraversal = tree.materializedSize();
        assertEquals(2L, materializedBeforeTraversal, "paging setup materializes the two parents");
        int callsBeforeTraversal = provider.totalProviderCalls();

        tree.setExpanded(10L, true);
        tree.setExpanded(101L, true);

        List<Long> visible = new ArrayList<>();
        ProviderBackedPrimitiveTree.VisibleCursor cursor = tree.visibleLoadedCursor();
        while (cursor.hasNext()) visible.add(cursor.nextLong());

        assertEquals(List.of(10L, 101L, 1001L, 102L, 103L, 20L, 30L), visible);
        assertEquals(callsBeforeTraversal, provider.totalProviderCalls(),
                "loaded-only traversal must not invoke provider");
        assertEquals(materializedBeforeTraversal, tree.materializedSize(),
                "expansion and visible traversal must not materialize additional payload");

        tree.setExpanded(101L, false);
        long[] window = new long[4];
        assertEquals(4, tree.fillVisibleLoadedWindow(1, 4, window));
        assertArrayEquals(new long[] {101L, 102L, 103L, 20L}, window);
        assertEquals(callsBeforeTraversal, provider.totalProviderCalls(),
                "collapsed-window traversal must not invoke provider");
        assertEquals(materializedBeforeTraversal, tree.materializedSize(),
                "collapsed-window traversal must not materialize additional payload");
    }

    @Test
    void visibleCursorFailsFastWhenRelationshipsOrExpansionChange() {
        ForestProvider provider = new ForestProvider();
        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(provider, 8, PrimitiveKind.LONG);
        tree.loadNextRootPage();

        ProviderBackedPrimitiveTree.VisibleCursor cursor = tree.visibleLoadedCursor();
        assertTrue(cursor.hasNext());
        tree.setExpanded(10L, true);
        assertThrows(ConcurrentModificationException.class, cursor::hasNext);
    }

    @Test
    void rootInvalidationDropsOnlyForestLinksAndKeepsPayload() {
        ForestProvider provider = new ForestProvider();
        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(provider, 8, PrimitiveKind.LONG);

        tree.loadNextRootPage();
        tree.touch(10L);
        assertTrue(tree.isMaterialized(10L));

        tree.invalidateRoots();
        assertEquals(-1, tree.firstLoadedRoot());
        assertEquals(0, tree.loadedRootCount());
        assertFalse(tree.isLoadedRoot(10L));
        assertTrue(tree.isMaterialized(10L), "payload survives root relationship invalidation");

        assertEquals(3, tree.loadNextRootPage());
        assertEquals(10, tree.firstLoadedRoot());
    }

    private static final class ForestProvider implements PagedPrimitiveTreeProvider {
        int rowLoads;
        int rootPageLoads;
        int childPageLoads;
        int hintCalls;

        @Override
        public void load(long rowId, long[] target) {
            rowLoads++;
            target[0] = rowId * 10L;
        }

        @Override
        public ChildHint rootHint() {
            hintCalls++;
            return ChildHint.SOME;
        }

        @Override
        public ChildHint childHint(long nodeId) {
            hintCalls++;
            return nodeId == 10L || nodeId == 101L ? ChildHint.SOME : ChildHint.NONE;
        }

        @Override
        public long loadRoots(int offset, int limit, long[] rootIds) {
            rootPageLoads++;
            long[] roots = {10L, 20L, 30L};
            return copyPage(roots, offset, limit, rootIds);
        }

        @Override
        public long loadChildren(
                long parentId, int offset, int limit, long[] childIds) {
            childPageLoads++;
            long[] children =
                    parentId == 10L
                            ? new long[] {101L, 102L, 103L}
                            : parentId == 101L
                                    ? new long[] {1001L}
                                    : new long[0];
            return copyPage(children, offset, limit, childIds);
        }

        int totalProviderCalls() {
            return rowLoads + rootPageLoads + childPageLoads + hintCalls;
        }

        private static long copyPage(
                long[] source, int offset, int limit, long[] target) {
            int count = Math.min(limit, Math.max(0, source.length - offset));
            for (int i = 0; i < count; i++) target[i] = source[offset + i];
            int next = offset + count == source.length ? -1 : offset + count;
            return PagedPrimitiveTreeProvider.pageResult(count, next);
        }
    }
}
