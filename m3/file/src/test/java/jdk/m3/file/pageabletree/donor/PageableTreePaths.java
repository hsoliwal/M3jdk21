/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3scale donor repository (synexia, apache2 mirror; exact name in qualification/pageable-tree-donor-verify-20261107/PROVENANCE.tsv) commit 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, PageableTreePaths.java (blob 7213ac94f8fefaaae75cb30e33b97f9d81cdcacc).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Parent-pointer path operations that do not materialize sibling branches. */
public final class PageableTreePaths {
    private PageableTreePaths() {}

    public static <K,V> List<TreeNode<K,V>> pathToRoot(
            PageableTreeSource<K,V> source,
            K nodeId,
            TreeProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(monitor, "monitor");
        ArrayDeque<TreeNode<K,V>> result = new ArrayDeque<>();
        K current = nodeId;

        monitor.begin("Resolve pageable tree path", TreeProgressMonitor.UNKNOWN);
        try {
            while (current != null) {
                monitor.checkCanceled();
                Optional<TreeNode<K,V>> found = source.node(current, monitor);
                TreeNode<K,V> node = found.orElseThrow(
                        () -> new IllegalArgumentException("unknown tree node " + nodeId));
                result.addFirst(node);
                monitor.worked(1);
                current = node.parentId().orElse(null);
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }
}
