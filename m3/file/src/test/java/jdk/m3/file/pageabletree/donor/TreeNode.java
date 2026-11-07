/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3-m3scale-apache2 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, TreeNode.java (blob a4b2d5f693fd67ed382b8cf140c4145e92bf9407).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

import java.util.Objects;
import java.util.Optional;

/** Immutable tree-node projection. Children are not embedded. */
public record TreeNode<K, V>(
        K id,
        Optional<K> parentId,
        V value,
        boolean mayHaveChildren,
        long ordinal) {

    public TreeNode {
        Objects.requireNonNull(id, "id");
        parentId = parentId == null ? Optional.empty() : parentId;
        Objects.requireNonNull(value, "value");
        if (ordinal < 0) throw new IllegalArgumentException("negative sibling ordinal");
    }

    public static <K,V> TreeNode<K,V> root(
            K id, V value, boolean mayHaveChildren, long ordinal) {
        return new TreeNode<>(id, Optional.empty(), value, mayHaveChildren, ordinal);
    }

    public static <K,V> TreeNode<K,V> child(
            K id, K parentId, V value, boolean mayHaveChildren, long ordinal) {
        return new TreeNode<>(id, Optional.of(parentId), value, mayHaveChildren, ordinal);
    }
}
