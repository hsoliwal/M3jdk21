// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.function.Function;

/**
 * Residency fold leaf PS allocation probe: bytes this thread allocates per call at each budget boundary, for a
 * fixed-budget owner and a PRESSURE_AWARE owner (reserve 0). Admission rows are net of the one Decision record an
 * admission publishes (measured with the same loop shape). Run with -Xint for interpreter-exact figures (C2 escape
 * analysis may remove a non-escaping HeapSnapshot). Same method as the crate evidence
 * (m3-doop-reuse-20261030 enhancement evidence/runtime/pressure: three samples of 100,000 calls).
 * usage: java -Xint -cp CLASSES:ORACLE jdk.m3.collections.PressureSamplingAllocationProbe [calls=100000]
 */
public final class PressureSamplingAllocationProbe {
    private static final Object PROJECTION = new Object();
    private static final Function<Object, Boolean> TOUCH = projection -> Boolean.TRUE;
    private static final M3ObjectResidency.Reinitializer<Object> RESET = (projection, slot) -> { };
    private static final int REUSABLE = M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.RECREATE
            | M3ObjectResidency.STRICT_NO_ESCAPE;
    private static final com.sun.management.ThreadMXBean BEAN =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static volatile Object sink;
    private static int calls;

    private PressureSamplingAllocationProbe() { }

    public static void main(String[] args) {
        calls = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;
        if (!BEAN.isThreadAllocatedMemorySupported()) throw new IllegalStateException("no allocation accounting");
        BEAN.setThreadAllocatedMemoryEnabled(true);
        for (boolean aware : new boolean[] {false, true}) {
            String mode = aware ? "pressure_aware" : "fixed_budget";
            int flag = aware ? M3ObjectResidency.PRESSURE_AWARE : 0;
            for (int sample = 1; sample <= 3; sample++) {
                M3ObjectResidency<Object> signal = owner(2, REUSABLE | flag, 64L);
                signal.use(0, 8L, TOUCH);
                signal.signalMemoryPressure();
                report(mode, "signalMemoryPressure", sample, measure(() -> {
                    for (int i = 0; i < calls; i++) signal.signalMemoryPressure();
                }));
                report(mode, "resident_use", sample, measure(() -> {
                    for (int i = 0; i < calls; i++) signal.use(0, 8L, TOUCH);
                }));
                if (signal.reuseUnborrowedProjection(0, 1, 65L, RESET)) throw new AssertionError("over budget");
                report(mode, "refused_reuse", sample, measure(() -> {
                    for (int i = 0; i < calls; i++) signal.reuseUnborrowedProjection(0, 1, 65L, RESET);
                }));
                M3ObjectResidency<Object> admit = owner(calls + 1, M3ObjectResidency.LAZY_CREATE | flag, Long.MAX_VALUE);
                admit.use(calls, 1L, TOUCH);
                long admissions = measure(() -> {
                    for (int slot = 0; slot < calls; slot++) admit.use(slot, 1L, TOUCH);
                });
                long decisions = measure(() -> {
                    for (int slot = 0; slot < calls; slot++) {
                        sink = new M3ObjectResidency.Decision(slot, M3ObjectResidency.State.ABSENT,
                                M3ObjectResidency.State.RESIDENT, slot, Long.MAX_VALUE, "created");
                    }
                });
                report(mode, "admission_net_of_decision", sample, admissions - decisions);
            }
        }
    }

    private static M3ObjectResidency<Object> owner(int slots, int flags, long budget) {
        return new M3ObjectResidency<>(slots, flags, budget, 0L, slot -> PROJECTION, null, null);
    }

    private static long measure(Runnable loop) {
        long empty = BEAN.getCurrentThreadAllocatedBytes();
        empty = BEAN.getCurrentThreadAllocatedBytes() - empty;
        long before = BEAN.getCurrentThreadAllocatedBytes();
        loop.run();
        return BEAN.getCurrentThreadAllocatedBytes() - before - empty;
    }

    private static void report(String mode, String boundary, int sample, long bytes) {
        System.out.println("ALLOC " + mode + " " + boundary + " sample=" + sample + " calls=" + calls + " bytes=" + bytes
                + " perCall=" + (bytes / calls));
    }
}
