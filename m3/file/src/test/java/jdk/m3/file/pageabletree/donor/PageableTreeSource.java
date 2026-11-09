/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3scale donor repository (synexia, apache2 mirror; exact name in qualification/pageable-tree-donor-verify-20261107/PROVENANCE.tsv) commit 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, PageableTreeSource.java (blob 7c01763fe0aec8244ba5da18209526e3ef525330).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

import java.util.Optional;

/**
 * Read-only pageable hierarchy.
 *
 * <p>Every parent defines an independent sibling sequence and therefore an independent cursor
 * stream. Implementations may use databases, mmap files, partial ASTs, REST APIs, or generated
 * trees without changing callers.</p>
 */
public interface PageableTreeSource<K, V> {
    String snapshotId();

    TreePage<K,V> roots(
            TreePageRequest request,
            TreeProgressMonitor monitor);

    TreePage<K,V> children(
            K parentId,
            TreePageRequest request,
            TreeProgressMonitor monitor);

    Optional<TreeNode<K,V>> node(
            K id,
            TreeProgressMonitor monitor);
}
