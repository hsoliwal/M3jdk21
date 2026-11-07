/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3scale donor repository (synexia, apache2 mirror; exact name in qualification/pageable-tree-donor-verify-20261107/PROVENANCE.tsv) commit 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, TreeCursor.java (blob 2f6ac68fe572a3516bb20a6dcb317506bd0b3b8d).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

import java.util.Objects;

/**
 * Opaque branch cursor tied to one immutable tree snapshot.
 *
 * <p>The reference implementation uses {@code position} as an offset. External data sources may
 * also put a provider cursor in {@code token}. Consumers must not combine cursors from different
 * snapshots.</p>
 */
public record TreeCursor(String snapshotId, long position, String token) {
    public TreeCursor {
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(token, "token");
        if (snapshotId.isBlank()) throw new IllegalArgumentException("snapshotId is blank");
        if (position < 0) throw new IllegalArgumentException("negative cursor position");
    }

    public static TreeCursor offset(String snapshotId, long position) {
        return new TreeCursor(snapshotId, position, Long.toUnsignedString(position, 36));
    }
}
