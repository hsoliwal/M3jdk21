// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.optane;

/** Read-only operator profile report, not Optane/BIOS detection. */
public final class OptaneDiagnostics {
    private OptaneDiagnostics() {}
    public static void main(String[] args) {
        System.out.println("com.m3.optane candidate 0.1.0");
        System.out.println("Hardware mode: NOT PROBED; no App Direct/persistence claim");
        System.out.println("Mapped-file advice JNI: " + MappedReadAdvice.nativeStatus());
        if (args.length == 0) {
            System.out.println("Usage: m3-optane DRAM_GiB PMEM_GiB SOCKETS WORKERS");
            return;
        }
        if (args.length != 4) throw new IllegalArgumentException("four explicit profile arguments required");
        long dram = Math.multiplyExact(Long.parseLong(args[0]), 1L << 30);
        long pmem = Math.multiplyExact(Long.parseLong(args[1]), 1L << 30);
        var profile = new MemoryModeProfile(dram, pmem, Integer.parseInt(args[2]));
        var plan = profile.plan(dram / 2, Integer.parseInt(args[3]), 2,
                                ScanPlan.MAX_WINDOW_BYTES, null);
        System.out.println("Operator-declared Memory Mode profile: " + profile);
        System.out.println("PMem/DRAM ratio: " + profile.pmemToDramRatio());
        System.out.println("Addressable estimate excludes DRAM cache: " + profile.declaredAddressableBytes());
        System.out.println("Example half-cache soft budget, two temporaries per worker: " + plan);
        System.out.println("Hardware DRAM cache placement is NOT controllable by this module.");
    }
}
