/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3scale donor repository (synexia, apache2 mirror; exact name in qualification/pageable-tree-donor-verify-20261107/PROVENANCE.tsv) commit 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, TreePage.java (blob 4abe3b8bde1d93ec479345c3daa5dc00f70aed98).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Immutable page of one sibling branch. */
public record TreePage<K, V>(
        String snapshotId,
        Optional<K> parentId,
        List<TreeNode<K,V>> items,
        Optional<TreeCursor> nextCursor,
        OptionalLong totalChildren) {

    public TreePage {
        Objects.requireNonNull(snapshotId, "snapshotId");
        parentId = parentId == null ? Optional.empty() : parentId;
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        totalChildren = totalChildren == null ? OptionalLong.empty() : totalChildren;
        nextCursor.ifPresent(cursor -> {
            if (!snapshotId.equals(cursor.snapshotId())) {
                throw new IllegalArgumentException("next cursor belongs to another snapshot");
            }
        });
        long previousOrdinal = -1;
        for (TreeNode<K,V> node : items) {
            if (!node.parentId().equals(parentId)) {
                throw new IllegalArgumentException(
                        "page item " + node.id() + " belongs to parent " + node.parentId()
                                + " instead of " + parentId);
            }
            if (node.ordinal() < previousOrdinal) {
                throw new IllegalArgumentException("page item ordinals are not nondecreasing");
            }
            previousOrdinal = node.ordinal();
        }
    }

    public boolean hasNext() { return nextCursor.isPresent(); }
}
