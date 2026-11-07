// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.Random;

/** Diagnostic only: allocation and time; no acceptance thresholds or claimed speedup. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-archive-copyto-20261030/proof/M3ArchiveAllocationProbe.java blob 08099fc6af36; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
public final class M3ArchiveAllocationProbe {
    private M3ArchiveAllocationProbe() { }

    public static void main(String[] args) {
        M3ClassSpace owner = new M3ClassSpace(1 << 21);
        Random random = new Random(0x67c3984L);
        for (int i = 0; i < 1700; i++) {
            byte[] atom = new byte[64 + (i % 7) * 43];
            random.nextBytes(atom);
            owner.intern(atom);
        }
        for (int i = 0; i < 45; i++) M3ClassSpaceArchive.freeze(owner);
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) {
            System.out.println("ARCHIVE_ALLOCATION_UNAVAILABLE");
            return;
        }
        bean.setThreadAllocatedMemoryEnabled(true);
        int runs = 100;
        long id = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(id);
        long start = System.nanoTime();
        int used = 0;
        for (int i = 0; i < runs; i++) used ^= M3ClassSpaceArchive.freeze(owner).length;
        long elapsed = System.nanoTime() - start;
        long bytes = bean.getThreadAllocatedBytes(id) - before;
        System.out.println("ARCHIVE_ALLOCATION_DIAGNOSTIC atoms=" + owner.imageCount()
                + " archiveBytes=" + M3ClassSpaceArchive.freeze(owner).length
                + " allocBytesPerFreeze=" + (bytes / runs)
                + " nanosPerFreeze=" + (elapsed / runs) + " witness=" + used);
    }
}
