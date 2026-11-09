package jdk.internal.m3.collections;

import jdk.m3.collections.PrimitiveKind;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderBackedPrimitiveTreeTest {

    @Test
    void pagesChildrenPerParentWithoutMaterializingChildPayload() {
        ModelProvider provider = new ModelProvider();
        ProviderBackedPrimitiveTree tree =
                DenseCollections.providerTree(
                        provider,
                        2,
                        PrimitiveKind.LONG,
                        PrimitiveKind.INT);

        assertEquals(2, tree.loadNextChildPage(0L));
        assertEquals(1, provider.rowLoads, "only parent payload loaded");
        assertEquals(1L, tree.materializedSize());
        assertFalse(tree.isMaterialized(1L));
        assertFalse(tree.isMaterialized(2L));
        assertTrue(LazyElementFlags.has(tree.flags(1L), LazyElementFlags.DISCOVERED));
        assertFalse(LazyElementFlags.has(tree.flags(1L), LazyElementFlags.MATERIALIZED));

        assertEquals(1, tree.firstLoadedChild(0L));
        assertEquals(2, tree.nextLoadedSibling(1L));
        assertEquals(-1, tree.nextLoadedSibling(2L));
        assertEquals(2, tree.loadedChildCount(0L));
        assertEquals(2, tree.nextChildOffset(0L));

        assertEquals(2, tree.loadNextChildPage(0L));
        assertEquals(1, tree.loadNextChildPage(0L));
        assertEquals(0, tree.loadNextChildPage(0L));
        assertEquals(5, tree.loadedChildCount(0L));
        assertEquals(-1, tree.nextChildOffset(0L));
        assertTrue(LazyElementFlags.has(
                tree.flags(0L), LazyElementFlags.CHILDREN_COMPLETE));

        List<Long> children = new ArrayList<>();
        tree.forEachLoadedChild(0L, children::add);
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), children);

        assertEquals(20L, tree.getBits(0, 2L));
        assertTrue(tree.isMaterialized(2L));
        assertEquals(2, provider.rowLoads);
    }

    @Test
    void everyParentOwnsIndependentPagingCursor() {
        ModelProvider provider = new ModelProvider();
        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(
                        provider, 2, PrimitiveKind.LONG);

        tree.loadNextChildPage(0L);
        assertEquals(2, tree.nextChildOffset(0L));

        assertEquals(2, tree.loadNextChildPage(1L));
        assertEquals(-1, tree.nextChildOffset(1L));
        assertEquals(2, tree.loadedChildCount(1L));
        assertEquals(10, tree.firstLoadedChild(1L));
        assertEquals(11, tree.nextLoadedSibling(10L));

        assertEquals(2, tree.nextChildOffset(0L), "root cursor unchanged by child branch");
        assertEquals(0, tree.parentOf(1L));
        assertEquals(1, tree.parentOf(10L));
    }

    @Test
    void childStateSurvivesPayloadInvalidationAndReload() {
        ModelProvider provider = new ModelProvider();
        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(
                        provider, 2, PrimitiveKind.LONG);

        tree.loadNextChildPage(0L);
        tree.invalidatePayload(0L);
        assertTrue(LazyElementFlags.has(
                tree.flags(0L), LazyElementFlags.CHILDREN_PARTIAL));

        tree.touch(0L);
        short flags = tree.flags(0L);
        assertTrue(LazyElementFlags.has(flags, LazyElementFlags.MATERIALIZED));
        assertTrue(LazyElementFlags.has(flags, LazyElementFlags.CHILDREN_PARTIAL));
        assertEquals(2, tree.nextChildOffset(0L));
    }

    @Test
    void invalidateChildrenDropsOnlyRelationshipCache() {
        ModelProvider provider = new ModelProvider();
        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(
                        provider, 3, PrimitiveKind.LONG);

        tree.loadNextChildPage(0L);
        tree.touch(1L);
        assertTrue(tree.isMaterialized(1L));

        tree.invalidateChildren(0L);
        assertEquals(-1, tree.firstLoadedChild(0L));
        assertEquals(0, tree.loadedChildCount(0L));
        assertEquals(-1, tree.parentOf(1L));
        assertTrue(tree.isMaterialized(1L), "payload survives relationship invalidation");

        assertEquals(3, tree.loadNextChildPage(0L));
        assertEquals(0, tree.parentOf(1L));
    }

    @Test
    void duplicateAndNonProgressPagesFailBeforeRelationshipMutation() {
        PagedPrimitiveTreeProvider duplicate = new PagedPrimitiveTreeProvider() {
            @Override
            public void load(long rowId, long[] target) {
                target[0] = rowId;
            }

            @Override
            public ChildHint childHint(long nodeId) {
                return ChildHint.SOME;
            }

            @Override
            public long loadChildren(
                    long parentId, int offset, int limit, long[] childIds) {
                childIds[0] = 7L;
                childIds[1] = 7L;
                return PagedPrimitiveTreeProvider.pageResult(2, -1);
            }
        };
        ProviderBackedPrimitiveTree duplicateTree =
                new ProviderBackedPrimitiveTree(duplicate, 2, PrimitiveKind.LONG);

        assertThrows(ProviderLoadException.class, () -> duplicateTree.loadNextChildPage(0L));
        assertEquals(-1, duplicateTree.firstLoadedChild(0L));
        assertEquals(-1, duplicateTree.parentOf(7L));

        PagedPrimitiveTreeProvider noProgress = new PagedPrimitiveTreeProvider() {
            @Override
            public void load(long rowId, long[] target) {
                target[0] = rowId;
            }

            @Override
            public long loadChildren(
                    long parentId, int offset, int limit, long[] childIds) {
                return PagedPrimitiveTreeProvider.pageResult(0, offset);
            }
        };
        ProviderBackedPrimitiveTree noProgressTree =
                new ProviderBackedPrimitiveTree(noProgress, 2, PrimitiveKind.LONG);
        assertThrows(ProviderLoadException.class, () -> noProgressTree.loadNextChildPage(0L));
        assertEquals(-1, noProgressTree.firstLoadedChild(0L));
    }

    @Test
    void childHintNoneCompletesWithoutFetch() {
        int[] pageLoads = {0};
        PagedPrimitiveTreeProvider provider = new PagedPrimitiveTreeProvider() {
            @Override
            public void load(long rowId, long[] target) {
                target[0] = rowId;
            }

            @Override
            public ChildHint childHint(long nodeId) {
                return ChildHint.NONE;
            }

            @Override
            public long loadChildren(
                    long parentId, int offset, int limit, long[] childIds) {
                pageLoads[0]++;
                throw new AssertionError("must not fetch children");
            }
        };

        ProviderBackedPrimitiveTree tree =
                new ProviderBackedPrimitiveTree(provider, 4, PrimitiveKind.LONG);
        assertEquals(0, tree.loadNextChildPage(12L));
        assertEquals(0, pageLoads[0]);
        assertEquals(-1, tree.nextChildOffset(12L));
        assertEquals(PagedPrimitiveTreeProvider.ChildHint.NONE, tree.childHint(12L));
    }

    private static final class ModelProvider implements PagedPrimitiveTreeProvider {
        int rowLoads;
        int pageLoads;

        @Override
        public void load(long rowId, long[] target) {
            rowLoads++;
            target[0] = rowId * 10L;
            if (target.length > 1) target[1] = rowId + 100L;
        }

        @Override
        public ChildHint childHint(long nodeId) {
            return nodeId == 0L || nodeId == 1L ? ChildHint.SOME : ChildHint.NONE;
        }

        @Override
        public long loadChildren(
                long parentId, int offset, int limit, long[] childIds) {
            pageLoads++;
            long[] source =
                    parentId == 0L
                            ? new long[] {1L, 2L, 3L, 4L, 5L}
                            : parentId == 1L
                                    ? new long[] {10L, 11L}
                                    : new long[0];
            int count = Math.min(limit, Math.max(0, source.length - offset));
            for (int i = 0; i < count; i++) childIds[i] = source[offset + i];
            int next = offset + count == source.length ? -1 : offset + count;
            return PagedPrimitiveTreeProvider.pageResult(count, next);
        }
    }
}
