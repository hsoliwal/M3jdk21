package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.M3CursorPagedPrimitiveTreeProvider;
import jdk.internal.m3.collections.M3PagedPrimitiveTreeProvider;
import jdk.internal.m3.collections.M3ProviderBackedPrimitiveLongTree;
import jdk.internal.m3.collections.M3ProviderLoadException;

class M3ProviderBackedPrimitiveLongTreeTest {

    private static final long ROOT = 5_000_000_000L;
    private static final long CHILD_1 = 5_000_000_101L;
    private static final long CHILD_2 = 5_000_000_102L;
    private static final long CHILD_3 = 5_000_000_103L;
    private static final long GRANDCHILD = 6_000_000_001L;
    private static final long ROOT_CURSOR_2 = 4_500_000_000L;
    private static final long CHILD_CURSOR_2 = 7_000_000_000L;

    @Test
    void supportsLongNodeIdsAndOpaqueCursorsBeyondSignedIntRange() {
        ModelProvider provider = new ModelProvider();
        M3ProviderBackedPrimitiveLongTree tree =
                new M3ProviderBackedPrimitiveLongTree(
                        13, 8, 12,
                        provider,
                        2,
                        M3PrimitiveKind.LONG,
                        M3PrimitiveKind.INT);

        assertTrue(tree.logicalCapacity() > Integer.MAX_VALUE);
        assertEquals(2, tree.loadNextChildPage(ROOT));
        assertEquals(ROOT_CURSOR_2, tree.nextChildCursor(ROOT));
        assertEquals(2L, tree.loadedChildCount(ROOT));

        assertEquals(CHILD_1, tree.firstLoadedChild(ROOT));
        assertEquals(CHILD_2, tree.nextLoadedSibling(CHILD_1));
        assertEquals(M3ProviderBackedPrimitiveLongTree.NO_NODE, tree.nextLoadedSibling(CHILD_2));
        assertEquals(ROOT, tree.parentOf(CHILD_1));

        assertFalse(tree.isMaterialized(CHILD_1));
        assertFalse(tree.isMaterialized(CHILD_2));
        assertEquals(1, provider.rowLoads, "only parent payload should be materialized");

        assertEquals(3L, tree.ensureLoadedChildren(ROOT, 3L, 2));
        assertTrue(tree.childrenComplete(ROOT));
        assertEquals(M3CursorPagedPrimitiveTreeProvider.END_CURSOR, tree.nextChildCursor(ROOT));
        assertEquals(CHILD_3, tree.loadedChildAt(ROOT, 2L));

        List<Long> children = new ArrayList<>();
        tree.forEachLoadedChild(ROOT, children::add);
        assertEquals(List.of(CHILD_1, CHILD_2, CHILD_3), children);

        assertEquals(CHILD_2 * 10L, tree.getBits(0, CHILD_2));
        assertTrue(tree.isMaterialized(CHILD_2));
        assertEquals(2, provider.rowLoads);
    }

    @Test
    void everyParentOwnsAnIndependentOpaqueCursor() {
        ModelProvider provider = new ModelProvider();
        M3ProviderBackedPrimitiveLongTree tree =
                new M3ProviderBackedPrimitiveLongTree(
                        13, 8, 12,
                        provider,
                        2,
                        M3PrimitiveKind.LONG);

        tree.loadNextChildPage(ROOT);
        assertEquals(ROOT_CURSOR_2, tree.nextChildCursor(ROOT));

        assertEquals(0L, tree.loadedChildCount(CHILD_1));
        assertEquals(1, tree.loadNextChildPage(CHILD_1));
        assertEquals(M3CursorPagedPrimitiveTreeProvider.END_CURSOR, tree.nextChildCursor(CHILD_1));
        assertEquals(GRANDCHILD, tree.firstLoadedChild(CHILD_1));
        assertEquals(CHILD_1, tree.parentOf(GRANDCHILD));

        assertEquals(ROOT_CURSOR_2, tree.nextChildCursor(ROOT), "root cursor must remain unchanged");
    }

    @Test
    void relationshipInvalidationPreservesMaterializedPayload() {
        ModelProvider provider = new ModelProvider();
        M3ProviderBackedPrimitiveLongTree tree =
                new M3ProviderBackedPrimitiveLongTree(
                        13, 8, 12,
                        provider,
                        2,
                        M3PrimitiveKind.LONG);

        tree.loadNextChildPage(ROOT);
        tree.touch(CHILD_1);
        assertTrue(tree.isMaterialized(CHILD_1));

        tree.invalidateChildren(ROOT);
        assertEquals(M3ProviderBackedPrimitiveLongTree.NO_NODE, tree.firstLoadedChild(ROOT));
        assertEquals(0L, tree.loadedChildCount(ROOT));
        assertEquals(M3ProviderBackedPrimitiveLongTree.NO_NODE, tree.parentOf(CHILD_1));
        assertTrue(tree.isMaterialized(CHILD_1));

        assertEquals(2, tree.loadNextChildPage(ROOT));
        assertEquals(ROOT, tree.parentOf(CHILD_1));
    }

    @Test
    void nonProgressCursorAndAncestorCycleFailBeforeMutation() {
        M3CursorPagedPrimitiveTreeProvider noProgress = new M3CursorPagedPrimitiveTreeProvider() {
            @Override
            public void load(long rowId, long[] target) {
                target[0] = rowId;
            }

            @Override
            public int loadChildren(
                    long parentId, long cursor, int limit,
                    long[] childIds, long[] nextCursorOut) {
                nextCursorOut[0] = cursor;
                return 0;
            }
        };
        M3ProviderBackedPrimitiveLongTree noProgressTree =
                new M3ProviderBackedPrimitiveLongTree(
                        13, 8, 12,
                        noProgress,
                        2,
                        M3PrimitiveKind.LONG);

        assertThrows(
                M3ProviderLoadException.class,
                () -> noProgressTree.loadNextChildPage(ROOT));
        assertEquals(M3ProviderBackedPrimitiveLongTree.NO_NODE,
                noProgressTree.firstLoadedChild(ROOT));

        M3CursorPagedPrimitiveTreeProvider cycle = new M3CursorPagedPrimitiveTreeProvider() {
            @Override
            public void load(long rowId, long[] target) {
                target[0] = rowId;
            }

            @Override
            public M3PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) {
                return nodeId == ROOT || nodeId == CHILD_1
                        ? M3PagedPrimitiveTreeProvider.ChildHint.SOME
                        : M3PagedPrimitiveTreeProvider.ChildHint.NONE;
            }

            @Override
            public int loadChildren(
                    long parentId, long cursor, int limit,
                    long[] childIds, long[] nextCursorOut) {
                if (parentId == ROOT) {
                    childIds[0] = CHILD_1;
                } else {
                    childIds[0] = ROOT;
                }
                nextCursorOut[0] = END_CURSOR;
                return 1;
            }
        };
        M3ProviderBackedPrimitiveLongTree cycleTree =
                new M3ProviderBackedPrimitiveLongTree(
                        13, 8, 12,
                        cycle,
                        2,
                        M3PrimitiveKind.LONG);
        assertEquals(1, cycleTree.loadNextChildPage(ROOT));

        assertThrows(
                M3ProviderLoadException.class,
                () -> cycleTree.loadNextChildPage(CHILD_1));
        assertEquals(M3ProviderBackedPrimitiveLongTree.NO_NODE,
                cycleTree.firstLoadedChild(CHILD_1));
    }

    private static final class ModelProvider implements M3CursorPagedPrimitiveTreeProvider {
        int rowLoads;
        int pageLoads;

        @Override
        public void load(long rowId, long[] target) {
            rowLoads++;
            target[0] = rowId * 10L;
            if (target.length > 1) target[1] = rowId & 0xffff_ffffL;
        }

        @Override
        public M3PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) {
            return nodeId == ROOT || nodeId == CHILD_1
                    ? M3PagedPrimitiveTreeProvider.ChildHint.SOME
                    : M3PagedPrimitiveTreeProvider.ChildHint.NONE;
        }

        @Override
        public int loadChildren(
                long parentId,
                long cursor,
                int limit,
                long[] childIds,
                long[] nextCursorOut) {
            pageLoads++;
            if (parentId == ROOT) {
                if (cursor == 0L) {
                    childIds[0] = CHILD_1;
                    childIds[1] = CHILD_2;
                    nextCursorOut[0] = ROOT_CURSOR_2;
                    return 2;
                }
                if (cursor == ROOT_CURSOR_2) {
                    childIds[0] = CHILD_3;
                    nextCursorOut[0] = END_CURSOR;
                    return 1;
                }
            } else if (parentId == CHILD_1) {
                if (cursor == 0L) {
                    childIds[0] = GRANDCHILD;
                    nextCursorOut[0] = END_CURSOR;
                    return 1;
                }
                nextCursorOut[0] = CHILD_CURSOR_2;
                return 0;
            }
            nextCursorOut[0] = END_CURSOR;
            return 0;
        }
    }
}
