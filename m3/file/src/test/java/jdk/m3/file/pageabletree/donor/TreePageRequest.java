/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3scale donor repository (synexia, apache2 mirror; exact name in qualification/pageable-tree-donor-verify-20261107/PROVENANCE.tsv) commit 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, TreePageRequest.java (blob 6fb00f703d70d5a8d67ab25486e363ac0e754e85).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

import java.util.Optional;

/** One page request inside one sibling branch. */
public record TreePageRequest(int limit, Optional<TreeCursor> cursor) {
    public TreePageRequest {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        cursor = cursor == null ? Optional.empty() : cursor;
    }

    public static TreePageRequest first(int limit) {
        return new TreePageRequest(limit, Optional.empty());
    }

    public static TreePageRequest after(int limit, TreeCursor cursor) {
        return new TreePageRequest(limit, Optional.of(cursor));
    }
}
