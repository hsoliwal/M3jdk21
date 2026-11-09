/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
/*
 * @test
 * @summary M3Object weak-index identity and bounded-heap strong-retention leak refusal
 * @run main/othervm -Xmx96m M3ObjectWeakLifetimeTest
 * @run main/othervm -Xint -Xmx96m M3ObjectWeakLifetimeTest
 */
import java.lang.ref.WeakReference;
import java.util.Random;

public final class M3ObjectWeakLifetimeTest {
    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }

    private static WeakReference<Object> abandonedPayload() {
        Object source = new byte[4096];
        M3Object facade = M3Object.of(source);
        check(facade.source() == source, "fresh facade owns source");
        // No strong reference escapes this helper.
        return new WeakReference<>(source);
    }

    private static WeakReference<M3Object> discardedFacade(Object source) {
        M3Object facade = M3Object.of(source);
        return new WeakReference<>(facade);
    }

    /**
     * The 96 MiB test VM cannot accommodate the cumulative 256 MiB of
     * abandoned arrays unless GC can reclaim the source/facade graph.
     * A strong-value cache retaining M3Object (which owns its source) fails
     * this test with OutOfMemoryError, regardless of System.gc() policy.
     */
    private static WeakReference<Object> abandonedLargePayload() {
        byte[] source = new byte[256 * 1024];
        M3Object facade = M3Object.of(source);
        check(facade.source() == source, "large facade source identity");
        return new WeakReference<>(source);
    }

    private static void boundedHeapMustReclaimAbandonedPayloads() {
        final int allocations = 1024;
        WeakReference<?>[] weakSources = new WeakReference<?>[allocations];
        for (int i = 0; i < allocations; i++) {
            weakSources[i] = abandonedLargePayload();
        }
        int reclaimed = 0;
        for (WeakReference<?> weak : weakSources) {
            if (weak.get() == null) reclaimed++;
        }
        check(reclaimed >= 128,
                "weak-value registry retained unreachable large payloads: reclaimed="
                        + reclaimed);
        System.out.println("Bounded heap reclaimed " + reclaimed
                + " of " + allocations + " abandoned sources");
    }

    private static void gcOpportunity() {
        // Never assert that System.gc() runs. This is only a collection opportunity.
        System.gc();
        byte[] churn = new byte[128 * 1024];
        churn[0] = 1;
    }

    public static void main(String[] args) {
        final int count = 4096;
        Object[] payloads = new Object[count];
        M3Object[] facades = new M3Object[count];
        for (int i = 0; i < count; i++) {
            payloads[i] = new Object();
            facades[i] = M3Object.of(payloads[i]);
        }
        Random random = new Random(0x4D334A444BL);
        for (int i = 0; i < 32_768; i++) {
            int slot = random.nextInt(count);
            check(M3Object.of(payloads[slot]) == facades[slot],
                    "live facade identity at " + i);
        }

        Object reachablePayload = new Object();
        WeakReference<Object> heldSource = new WeakReference<>(reachablePayload);
        M3Object retainedFacade = M3Object.of(reachablePayload);
        reachablePayload = null;
        for (int i = 0; i < 4; i++) gcOpportunity();
        check(heldSource.get() == retainedFacade.source(),
                "retained facade must retain its source");
        check(M3Object.of(retainedFacade.source()) == retainedFacade,
                "retained facade cannot be evicted");

        WeakReference<Object> abandoned = abandonedPayload();
        Object retainedSource = new Object();
        WeakReference<M3Object> discarded = discardedFacade(retainedSource);
        for (int i = 0; i < 24 && (abandoned.get() != null || discarded.get() != null); i++) {
            gcOpportunity();
        }
        // Explicit GC can be disabled: lack of collection is inconclusive.
        if (abandoned.get() == null) {
            System.out.println("GC observed: abandoned payload collected");
        } else {
            System.out.println("GC not observed for abandoned payload (inconclusive)");
        }
        if (discarded.get() == null) {
            M3Object reacquired = M3Object.of(retainedSource);
            check(reacquired.source() == retainedSource, "reacquisition payload identity");
            check(M3Object.of(retainedSource) == reacquired,
                    "reacquired facade retains identity while reachable");
            System.out.println("GC observed: dead facade re-created safely");
        } else {
            System.out.println("GC not observed for discarded facade (inconclusive)");
        }
        boundedHeapMustReclaimAbandonedPayloads();
        for (int i = 0; i < count; i++) {
            check(M3Object.of(payloads[i]) == facades[i],
                    "live facade survived GC pressure at " + i);
        }
        System.out.println("PASS M3ObjectWeakLifetimeTest live identity, weak GC and bounded-heap leak refusal");
    }
}
