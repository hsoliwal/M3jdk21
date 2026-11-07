/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3scale donor repository (synexia, apache2 mirror; exact name in qualification/pageable-tree-donor-verify-20261107/PROVENANCE.tsv) commit 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, VisibleTreeWindow.java (blob d5d109e3cebf0710b627e6bbfae9252168bc6612).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Virtual-scroll projection over an expanded pageable tree.
 *
 * <p>Collapsed branches are never queried. Expanded branches are fetched page by page only until
 * the requested visible window plus one look-ahead row is satisfied.</p>
 */
public final class VisibleTreeWindow {
    private VisibleTreeWindow() {}

    public record Row<K,V>(
            TreeNode<K,V> node,
            int depth,
            boolean expanded) {}

    public record Window<K,V>(
            long offset,
            List<Row<K,V>> rows,
            boolean hasMore,
            long scannedVisibleRows) {
        public Window {
            rows = List.copyOf(rows);
        }
    }

    public static <K,V> Window<K,V> read(
            PageableTreeSession<K,V> session,
            long visibleOffset,
            int limit,
            TreeProgressMonitor monitor) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(monitor, "monitor");
        if (visibleOffset < 0) throw new IllegalArgumentException("negative visibleOffset");
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");

        long target = Math.addExact(visibleOffset, (long) limit);
        List<Row<K,V>> output = new ArrayList<>(limit);
        ArrayDeque<Frame<K,V>> stack = new ArrayDeque<>();
        stack.push(Frame.root());

        long visible = 0;
        boolean hasMore = false;

        monitor.begin("Read virtual tree window", limit);
        try {
            while (!stack.isEmpty()) {
                monitor.checkCanceled();
                Frame<K,V> frame = stack.peek();
                if (frame.index >= frame.items.size()) {
                    if (frame.loaded && frame.nextCursor.isEmpty()) {
                        stack.pop();
                        continue;
                    }
                    TreePage<K,V> page = frame.parentId == null
                            ? session.rootPage(frame.nextCursor, monitor)
                            : session.childPage(frame.parentId, frame.nextCursor, monitor);
                    frame.items = page.items();
                    frame.index = 0;
                    frame.nextCursor = page.nextCursor();
                    frame.loaded = true;
                    if (frame.items.isEmpty() && frame.nextCursor.isEmpty()) {
                        stack.pop();
                        continue;
                    }
                }

                TreeNode<K,V> node = frame.items.get(frame.index++);
                boolean expanded = node.mayHaveChildren() && session.isExpanded(node.id());

                if (visible >= visibleOffset && visible < target) {
                    output.add(new Row<>(node, frame.depth, expanded));
                    monitor.worked(1);
                }
                visible++;

                if (visible > target) {
                    hasMore = true;
                    break;
                }

                if (expanded) {
                    stack.push(new Frame<>(
                            node.id(),
                            frame.depth + 1,
                            Optional.empty(),
                            List.of(),
                            0,
                            false));
                }
            }
            if (!hasMore && visible == target && !stack.isEmpty()) hasMore = true;
            return new Window<>(visibleOffset, output, hasMore, visible);
        } finally {
            monitor.done();
        }
    }

    private static final class Frame<K,V> {
        private final K parentId;
        private final int depth;
        private Optional<TreeCursor> nextCursor;
        private List<TreeNode<K,V>> items;
        private int index;
        private boolean loaded;

        private Frame(
                K parentId,
                int depth,
                Optional<TreeCursor> nextCursor,
                List<TreeNode<K,V>> items,
                int index,
                boolean loaded) {
            this.parentId = parentId;
            this.depth = depth;
            this.nextCursor = nextCursor;
            this.items = items;
            this.index = index;
            this.loaded = loaded;
        }

        static <K,V> Frame<K,V> root() {
            return new Frame<>(null, 0, Optional.empty(), List.of(), 0, false);
        }
    }
}
