/* SPDX-License-Identifier: Apache-2.0 */
// Test-only donor fixture: m3scale donor repository (synexia, apache2 mirror; exact name in qualification/pageable-tree-donor-verify-20261107/PROVENANCE.tsv) commit 3385d79a2968c770aedf1d3398b2a62f05ebc092, module m3-pageable-tree, TreeProgressMonitor.java (blob c1b206190d876eada340ee2a76ae918345b4819f).
// Byte-identical to the donor except this note and the package line; the differential oracle for jdk.m3.file.M3PageableTree.
package jdk.m3.file.pageabletree.donor;

import java.util.concurrent.CancellationException;

/** Minimal progress/cancellation SPI; the M3 core monitor can be adapted without a module cycle. */
public interface TreeProgressMonitor {
    int UNKNOWN = -1;

    default void begin(String name, long totalWork) {}
    default void subTask(String name) {}
    default void worked(long work) {}
    default boolean isCanceled() { return false; }
    default void done() {}

    default void checkCanceled() {
        if (isCanceled()) throw new CancellationException("pageable tree operation canceled");
    }

    static TreeProgressMonitor none() { return None.INSTANCE; }

    enum None implements TreeProgressMonitor { INSTANCE }
}
