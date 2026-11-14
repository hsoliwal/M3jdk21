/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3scale donor repository (synexia, apache2 mirror; exact name in qualification/pageable-tree-donor-verify-20261107/PROVENANCE.tsv) commit 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, InMemoryPageableTree.java (blob e5f017268a3abd91f3c8540634598c9541c50cc4).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Deterministic reference source.
 *
 * <p>The complete topology is supplied at build time, but reads obey exactly the same branch-page
 * contract as remote/database implementations. It is useful for tests, static trees and adapters.</p>
 */
public final class InMemoryPageableTree<K,V> implements PageableTreeSource<K,V> {
    private static final Object ROOT = new Object();

    private final String snapshotId;
    private final Map<K,TreeNode<K,V>> byId;
    private final Map<Object,List<TreeNode<K,V>>> children;

    private InMemoryPageableTree(
            String snapshotId,
            Map<K,TreeNode<K,V>> byId,
            Map<Object,List<TreeNode<K,V>>> children) {
        this.snapshotId = snapshotId;
        this.byId = Map.copyOf(byId);
        this.children = Map.copyOf(children);
    }

    public static <K,V> Builder<K,V> builder(String snapshotId) {
        return new Builder<>(snapshotId);
    }

    @Override public String snapshotId() { return snapshotId; }

    @Override
    public TreePage<K,V> roots(TreePageRequest request, TreeProgressMonitor monitor) {
        return page(Optional.empty(), ROOT, request, monitor);
    }

    @Override
    public TreePage<K,V> children(
            K parentId,
            TreePageRequest request,
            TreeProgressMonitor monitor) {
        Objects.requireNonNull(parentId, "parentId");
        if (!byId.containsKey(parentId)) throw new IllegalArgumentException("unknown parent " + parentId);
        return page(Optional.of(parentId), parentId, request, monitor);
    }

    @Override
    public Optional<TreeNode<K,V>> node(K id, TreeProgressMonitor monitor) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(monitor, "monitor").checkCanceled();
        return Optional.ofNullable(byId.get(id));
    }

    private TreePage<K,V> page(
            Optional<K> parent,
            Object branch,
            TreePageRequest request,
            TreeProgressMonitor monitor) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(monitor, "monitor").checkCanceled();
        List<TreeNode<K,V>> siblings = children.getOrDefault(branch, List.of());
        long offsetLong = request.cursor().map(cursor -> {
            requireSnapshot(cursor);
            return cursor.position();
        }).orElse(0L);
        if (offsetLong > siblings.size()) {
            throw new IllegalArgumentException("cursor beyond branch end: " + offsetLong);
        }
        int offset = Math.toIntExact(offsetLong);
        int end = Math.min(siblings.size(), Math.addExact(offset, request.limit()));

        monitor.begin("Load tree page", end - offset);
        List<TreeNode<K,V>> values = new ArrayList<>(end - offset);
        try {
            for (int i = offset; i < end; i++) {
                monitor.checkCanceled();
                values.add(siblings.get(i));
                monitor.worked(1);
            }
        } finally {
            monitor.done();
        }

        Optional<TreeCursor> next = end < siblings.size()
                ? Optional.of(TreeCursor.offset(snapshotId, end))
                : Optional.empty();
        return new TreePage<>(
                snapshotId,
                parent,
                values,
                next,
                OptionalLong.of(siblings.size()));
    }

    private void requireSnapshot(TreeCursor cursor) {
        if (!snapshotId.equals(cursor.snapshotId())) {
            throw new StaleTreeCursorException(
                    "cursor snapshot " + cursor.snapshotId() + " != " + snapshotId);
        }
    }

    public static final class Builder<K,V> {
        private final String snapshotId;
        private final Map<K,TreeNode<K,V>> nodes = new LinkedHashMap<>();

        private Builder(String snapshotId) {
            this.snapshotId = Objects.requireNonNull(snapshotId, "snapshotId");
            if (snapshotId.isBlank()) throw new IllegalArgumentException("snapshotId is blank");
        }

        public Builder<K,V> add(TreeNode<K,V> node) {
            Objects.requireNonNull(node, "node");
            if (nodes.putIfAbsent(node.id(), node) != null) {
                throw new IllegalArgumentException("duplicate node id " + node.id());
            }
            return this;
        }

        public Builder<K,V> addAll(Collection<TreeNode<K,V>> values) {
            values.forEach(this::add);
            return this;
        }

        public InMemoryPageableTree<K,V> build() {
            Map<Object,List<TreeNode<K,V>>> branches = new HashMap<>();
            for (TreeNode<K,V> node : nodes.values()) {
                Object key = node.parentId().<Object>map(value -> value).orElse(ROOT);
                if (node.parentId().isPresent() && !nodes.containsKey(node.parentId().orElseThrow())) {
                    throw new IllegalArgumentException(
                            "missing parent " + node.parentId().orElseThrow() + " for " + node.id());
                }
                branches.computeIfAbsent(key, ignored -> new ArrayList<>()).add(node);
            }

            Comparator<TreeNode<K,V>> order = Comparator
                    .comparingLong(TreeNode<K,V>::ordinal)
                    .thenComparing(node -> node.id().toString());
            Map<Object,List<TreeNode<K,V>>> frozen = new HashMap<>();
            for (Map.Entry<Object,List<TreeNode<K,V>>> entry : branches.entrySet()) {
                List<TreeNode<K,V>> values = entry.getValue();
                values.sort(order);
                long previous = -1;
                for (TreeNode<K,V> node : values) {
                    if (node.ordinal() < previous) {
                        throw new IllegalStateException("sibling order invariant broken");
                    }
                    previous = node.ordinal();
                }
                frozen.put(entry.getKey(), List.copyOf(values));
            }
            validateAcyclic(nodes);
            return new InMemoryPageableTree<>(snapshotId, nodes, frozen);
        }

        private static <K,V> void validateAcyclic(Map<K,TreeNode<K,V>> nodes) {
            for (TreeNode<K,V> node : nodes.values()) {
                Map<K,Boolean> seen = new HashMap<>();
                TreeNode<K,V> current = node;
                while (current.parentId().isPresent()) {
                    if (seen.put(current.id(), Boolean.TRUE) != null) {
                        throw new IllegalArgumentException("cycle at " + current.id());
                    }
                    current = nodes.get(current.parentId().orElseThrow());
                }
            }
        }
    }
}
