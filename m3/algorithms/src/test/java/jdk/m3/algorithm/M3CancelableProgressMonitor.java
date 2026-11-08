// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

/**
 * Test-scope monitor with a settable cancellation flag: the owner-side shape of the donor tests' default progress
 * monitor (the MEMBER row of recipe/corpus-search-20261108/RELOCATION_MAP.tsv). Every other operation keeps the
 * adapter's no-op defaults.
 */
//FLLDCIM: TOP - test helper of recipe/corpus-search-20261108 (M3JDK-authored, test scope)
final class M3CancelableProgressMonitor implements M3ProgressMonitor {
    private volatile boolean canceled;

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean value) {
        canceled = value;
    }
}
