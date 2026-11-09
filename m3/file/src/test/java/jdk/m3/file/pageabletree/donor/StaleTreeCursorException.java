/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3scale donor repository (synexia, apache2 mirror; exact name in qualification/pageable-tree-donor-verify-20261107/PROVENANCE.tsv) commit 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, StaleTreeCursorException.java (blob 61a47f42b86468ee587bdd35d3d72aeb4f99f2c0).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

/** Raised when a branch cursor is reused against another immutable tree generation. */
public final class StaleTreeCursorException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public StaleTreeCursorException(String message) {
        super(message);
    }
}
