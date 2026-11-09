/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3scale donor repository (synexia, apache2 mirror; exact name in qualification/pageable-tree-donor-verify-20261107/PROVENANCE.tsv) commit 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, PageableTreeSession.java (blob cfc3c0f466db4e1a197920b27d27ae16181558df).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Stateful lazy view over a read-only tree snapshot.
 *
 * <p>Pages are cached independently per parent branch. Expansion state is orthogonal to paging:
 * expanding a node does not imply all of its children are materialized.</p>
 */
public final class PageableTreeSession<K,V> {
    private static final Object ROOT = new Object();

    private final PageableTreeSource<K,V> source;
    private final String snapshotId;
    private final int branchPageSize;
    private final int maxCachedPages;
    private final LinkedHashMap<PageKey<K>, TreePage<K,V>> pageCache;
    private final Set<K> expanded = new LinkedHashSet<>();

    public PageableTreeSession(
            PageableTreeSource<K,V> source,
            int branchPageSize,
            int maxCachedPages) {
        this.source = Objects.requireNonNull(source, "source");
        this.snapshotId = Objects.requireNonNull(source.snapshotId(), "snapshotId");
        if (snapshotId.isBlank()) throw new IllegalArgumentException("source snapshotId is blank");
        if (branchPageSize < 1) throw new IllegalArgumentException("branchPageSize must be positive");
        if (maxCachedPages < 1) throw new IllegalArgumentException("maxCachedPages must be positive");
        this.branchPageSize = branchPageSize;
        this.maxCachedPages = maxCachedPages;
        this.pageCache = new LinkedHashMap<>(16, 0.75f, true);
    }

    public PageableTreeSource<K,V> source() { return source; }
    public String snapshotId() { return snapshotId; }
    public int branchPageSize() { return branchPageSize; }

    public void expand(K nodeId) {
        expanded.add(Objects.requireNonNull(nodeId, "nodeId"));
    }

    public void collapse(K nodeId) {
        expanded.remove(Objects.requireNonNull(nodeId, "nodeId"));
    }

    public boolean isExpanded(K nodeId) {
        return expanded.contains(Objects.requireNonNull(nodeId, "nodeId"));
    }

    public Set<K> expandedNodes() {
        return Set.copyOf(expanded);
    }

    public TreePage<K,V> rootPage(
            Optional<TreeCursor> cursor,
            TreeProgressMonitor monitor) {
        return page(null, cursor, monitor);
    }

    public TreePage<K,V> childPage(
            K parentId,
            Optional<TreeCursor> cursor,
            TreeProgressMonitor monitor) {
        Objects.requireNonNull(parentId, "parentId");
        return page(parentId, cursor, monitor);
    }

    public List<TreeNode<K,V>> loadAllChildren(
            K parentId,
            TreeProgressMonitor monitor) {
        Objects.requireNonNull(parentId, "parentId");
        Objects.requireNonNull(monitor, "monitor");
        List<TreeNode<K,V>> result = new ArrayList<>();
        Optional<TreeCursor> cursor = Optional.empty();
        do {
            monitor.checkCanceled();
            TreePage<K,V> page = childPage(parentId, cursor, monitor);
            result.addAll(page.items());
            cursor = page.nextCursor();
        } while (cursor.isPresent());
        return List.copyOf(result);
    }

    public int cachedPageCount() { return pageCache.size(); }

    public void clearCache() {
        pageCache.clear();
    }

    private TreePage<K,V> page(
            K parentId,
            Optional<TreeCursor> cursor,
            TreeProgressMonitor monitor) {
        Objects.requireNonNull(cursor, "cursor");
        Objects.requireNonNull(monitor, "monitor").checkCanceled();
        requireSourceSnapshot();
        long position = cursor.map(TreeCursor::position).orElse(0L);
        PageKey<K> key = new PageKey<>(parentId, position);
        TreePage<K,V> cached = pageCache.get(key);
        if (cached != null) {
            if (!snapshotId.equals(cached.snapshotId())) {
                throw new StaleTreeCursorException("cached page belongs to another snapshot");
            }
            return cached;
        }

        TreePageRequest request = cursor.isEmpty()
                ? TreePageRequest.first(branchPageSize)
                : TreePageRequest.after(branchPageSize, cursor.orElseThrow());
        TreePage<K,V> loaded = parentId == null
                ? source.roots(request, monitor)
                : source.children(parentId, request, monitor);
        requireSnapshot(loaded);
        pageCache.put(key, loaded);
        evict();
        return loaded;
    }

    private void requireSnapshot(TreePage<K,V> page) {
        if (!snapshotId.equals(page.snapshotId())) {
            throw new StaleTreeCursorException(
                    "source returned page from snapshot " + page.snapshotId()
                            + " while session is pinned to " + snapshotId);
        }
    }

    private void requireSourceSnapshot() {
        String current = source.snapshotId();
        if (!snapshotId.equals(current)) {
            throw new StaleTreeCursorException(
                    "tree source advanced from snapshot " + snapshotId + " to " + current
                            + "; create a new PageableTreeSession");
        }
    }

    private void evict() {
        while (pageCache.size() > maxCachedPages) {
            Map.Entry<PageKey<K>,TreePage<K,V>> eldest = pageCache.entrySet().iterator().next();
            pageCache.remove(eldest.getKey());
        }
    }

    private record PageKey<K>(K parentId, long position) {}
}
