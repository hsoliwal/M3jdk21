// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.lang.management.ManagementFactory;
import jdk.m3.collections.M3ObjectResidency;

/**
 * Merge-loss leaf 45 probe: thread-allocated bytes per signalMemoryPressure() call for a fixed budget and for a
 * PRESSURE_AWARE budget (run with -Xint so escape analysis cannot remove the snapshot). Argument fixed|pressure
 * measures one configuration in a fresh JVM. The origin receipt
 * m3/qualification/m3-doop-pressure-sampling-20261030.json records 48 -> 0 (fixed) and 48 -> 48 (pressure-aware).
 */
public final class ResidencyPressureSamplingProbe {
    private static final int CALLS = 100_000;

    private ResidencyPressureSamplingProbe() { }

    public static void main(String[] args) {
        //FLLDCIM: MRL - merge-loss leaf 45 probe: heap snapshots only for PRESSURE_AWARE budgets
        com.sun.management.ThreadMXBean threads =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        String only = args.length > 0 ? args[0] : "both";
        String fixed = "pressure".equals(only) ? "-" : Long.toString(perCall(threads, M3ObjectResidency.LAZY_CREATE));
        String pressured = "fixed".equals(only) ? "-"
                : Long.toString(perCall(threads, M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.PRESSURE_AWARE));
        System.out.println("bytes_per_call fixed_budget_signalMemoryPressure=" + fixed
                + " pressure_enabled_signalMemoryPressure=" + pressured);
    }

    private static long perCall(com.sun.management.ThreadMXBean threads, int flags) {
        M3ObjectResidency<Object> residency =
                new M3ObjectResidency<>(4, flags, Long.MAX_VALUE / 4, 0L, slot -> new Object(), null, null);
        long thread = Thread.currentThread().threadId();
        for (int i = 0; i < CALLS; i++) residency.signalMemoryPressure();
        long before = threads.getThreadAllocatedBytes(thread);
        for (int i = 0; i < CALLS; i++) residency.signalMemoryPressure();
        long after = threads.getThreadAllocatedBytes(thread);
        return Math.round((after - before) / (double) CALLS);
    }
}
