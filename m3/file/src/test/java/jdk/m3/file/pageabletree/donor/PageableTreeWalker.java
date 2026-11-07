/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3scale donor repository (synexia, apache2 mirror; exact name in qualification/pageable-tree-donor-verify-20261107/PROVENANCE.tsv) commit 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, PageableTreeWalker.java (blob 4998cf56c43fd7272b673ac942c4cd09ab7bf15b).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Lazy full-tree traversal that advances branch pages only when needed. */
public final class PageableTreeWalker {
    private PageableTreeWalker() {}

    public enum Order { DEPTH_FIRST, BREADTH_FIRST }

    public record Visit<K,V>(TreeNode<K,V> node, int depth) {}

    public static <K,V> List<Visit<K,V>> walk(
            PageableTreeSource<K,V> source,
            int pageSize,
            Order order,
            TreeProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(monitor, "monitor");
        if (pageSize < 1) throw new IllegalArgumentException("pageSize must be positive");

        return order == Order.DEPTH_FIRST
                ? depthFirst(source, pageSize, monitor)
                : breadthFirst(source, pageSize, monitor);
    }

    private static <K,V> List<Visit<K,V>> depthFirst(
            PageableTreeSource<K,V> source,
            int pageSize,
            TreeProgressMonitor monitor) {
        List<Visit<K,V>> result = new ArrayList<>();
        ArrayDeque<Branch<K,V>> stack = new ArrayDeque<>();
        stack.push(new Branch<>(null, 0, Optional.empty(), List.of(), 0, false));

        monitor.begin("Walk pageable tree depth-first", TreeProgressMonitor.UNKNOWN);
        try {
            while (!stack.isEmpty()) {
                monitor.checkCanceled();
                Branch<K,V> branch = stack.peek();
                if (branch.index >= branch.items.size()) {
                    if (branch.loaded && branch.nextCursor.isEmpty()) {
                        stack.pop();
                        continue;
                    }
                    TreePage<K,V> page = load(source, branch.parentId, branch.nextCursor, pageSize, monitor);
                    branch.items = page.items();
                    branch.index = 0;
                    branch.nextCursor = page.nextCursor();
                    branch.loaded = true;
                    if (branch.items.isEmpty() && branch.nextCursor.isEmpty()) {
                        stack.pop();
                        continue;
                    }
                }

                TreeNode<K,V> node = branch.items.get(branch.index++);
                result.add(new Visit<>(node, branch.depth));
                monitor.worked(1);
                if (node.mayHaveChildren()) {
                    stack.push(new Branch<>(
                            node.id(),
                            branch.depth + 1,
                            Optional.empty(),
                            List.of(),
                            0,
                            false));
                }
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    private static <K,V> List<Visit<K,V>> breadthFirst(
            PageableTreeSource<K,V> source,
            int pageSize,
            TreeProgressMonitor monitor) {
        List<Visit<K,V>> result = new ArrayList<>();
        ArrayDeque<ParentDepth<K>> parents = new ArrayDeque<>();
        parents.add(new ParentDepth<>(null, 0));

        monitor.begin("Walk pageable tree breadth-first", TreeProgressMonitor.UNKNOWN);
        try {
            while (!parents.isEmpty()) {
                monitor.checkCanceled();
                ParentDepth<K> parent = parents.removeFirst();
                Optional<TreeCursor> cursor = Optional.empty();
                do {
                    TreePage<K,V> page = load(source, parent.parentId, cursor, pageSize, monitor);
                    for (TreeNode<K,V> node : page.items()) {
                        result.add(new Visit<>(node, parent.depth));
                        monitor.worked(1);
                        if (node.mayHaveChildren()) {
                            parents.addLast(new ParentDepth<>(node.id(), parent.depth + 1));
                        }
                    }
                    cursor = page.nextCursor();
                } while (cursor.isPresent());
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    private static <K,V> TreePage<K,V> load(
            PageableTreeSource<K,V> source,
            K parentId,
            Optional<TreeCursor> cursor,
            int pageSize,
            TreeProgressMonitor monitor) {
        TreePageRequest request = cursor.isEmpty()
                ? TreePageRequest.first(pageSize)
                : TreePageRequest.after(pageSize, cursor.orElseThrow());
        return parentId == null
                ? source.roots(request, monitor)
                : source.children(parentId, request, monitor);
    }

    private static final class Branch<K,V> {
        private final K parentId;
        private final int depth;
        private Optional<TreeCursor> nextCursor;
        private List<TreeNode<K,V>> items;
        private int index;
        private boolean loaded;

        private Branch(
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
    }

    private record ParentDepth<K>(K parentId, int depth) {}
}
