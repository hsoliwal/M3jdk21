// SPDX-License-Identifier: Apache-2.0
package com.m3.gc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.Notification;

/** Executable Java 21 contract oracle; no artificial System.gc() calls. */
public final class M3GcIntegrationProof {
    private static int checks;
    private M3GcIntegrationProof() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        compilerAdmission();
        notificationAndSafePoint();
        reentrancyAndFailure();
        parallelPoll();
        System.out.println("M3_GC_INTEGRATION_PASS checks=" + checks);
    }

    private static void compilerAdmission() {
        M3GcUsePlan.Facts verifiedLocal = new M3GcUsePlan.Facts(true, true, false, false, false, true);
        check(M3GcUsePlan.classify(verifiedLocal) == M3GcUsePlan.Mode.COMPACT_PARENT_STATE,
                "compiler-proven local projection may use parent lanes");
        check(M3GcUsePlan.classify(new M3GcUsePlan.Facts(false, true, false, false, false, true))
                == M3GcUsePlan.Mode.PRESERVE_JVM_REACHABILITY, "annotation without evidence refuses");
        check(M3GcUsePlan.classify(new M3GcUsePlan.Facts(true, false, false, false, false, true))
                == M3GcUsePlan.Mode.PRESERVE_JVM_REACHABILITY, "unknown escape refuses");
        check(M3GcUsePlan.classify(new M3GcUsePlan.Facts(true, true, true, false, false, true))
                == M3GcUsePlan.Mode.PRESERVE_JVM_REACHABILITY, "Java/JNI observers refuse");
        check(M3GcUsePlan.classify(new M3GcUsePlan.Facts(true, true, false, true, false, true))
                == M3GcUsePlan.Mode.PRESERVE_JVM_REACHABILITY, "shared no-lease refuses");
        check(M3GcUsePlan.classify(new M3GcUsePlan.Facts(true, true, false, true, true, true))
                == M3GcUsePlan.Mode.COOPERATIVE_LEASE, "shared owner uses existing lease");
        check(M3GcUsePlan.classify(new M3GcUsePlan.Facts(true, true, false, false, false, false))
                == M3GcUsePlan.Mode.PRESERVE_JVM_REACHABILITY, "unreconstructible owner refuses");
    }

    private static void event(String type) {
        M3GcNotifications.onNotification(new Notification(type, "test-only", 1L));
    }

    private static void notificationAndSafePoint() {
        AtomicInteger signals = new AtomicInteger();
        M3GcPressureGate gate = new M3GcPressureGate(signals::incrementAndGet);
        long before = M3GcNotifications.epoch();
        check(!gate.applyAtSafePoint(), "no GC event means no work");
        event("unrelated.notification");
        check(M3GcNotifications.epoch() == before, "ignore unrelated JMX notifications");
        event(M3GcNotifications.GC_NOTIFICATION_TYPE);
        event(M3GcNotifications.GC_NOTIFICATION_TYPE);
        check(signals.get() == 0, "GC notification must not call cache/eviction code");
        check(gate.applyAtSafePoint(), "safe boundary applies signal");
        check(signals.get() == 1, "two events coalesce into one safe-boundary call");
        check(!gate.applyAtSafePoint(), "no duplicate action without a new epoch");
    }

    private static void reentrancyAndFailure() {
        AtomicInteger invocations = new AtomicInteger();
        M3GcPressureGate[] gates = new M3GcPressureGate[1];
        gates[0] = new M3GcPressureGate(() -> {
            invocations.incrementAndGet();
            check(!gates[0].applyAtSafePoint(), "nested poll cannot recursively evict");
            if (invocations.get() == 1) throw new IllegalStateException("pressure action unavailable");
        });
        event(M3GcNotifications.GC_NOTIFICATION_TYPE);
        try {
            gates[0].applyAtSafePoint();
            throw new AssertionError("expected failure");
        } catch (IllegalStateException expected) {
            check("pressure action unavailable".equals(expected.getMessage()), "failure propagated");
        }
        check(gates[0].applyAtSafePoint(), "failed pressure action remains pending");
        check(invocations.get() == 2, "retry invoked exactly once");
    }

    private static void parallelPoll() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger();
        M3GcPressureGate gate = new M3GcPressureGate(calls::incrementAndGet);
        event(M3GcNotifications.GC_NOTIFICATION_TYPE);
        CountDownLatch start = new CountDownLatch(1);
        Thread[] workers = new Thread[8];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(() -> {
                try { start.await(); gate.applyAtSafePoint(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            workers[i].start();
        }
        start.countDown();
        for (Thread worker : workers) worker.join();
        check(calls.get() == 1, "parallel poll never runs pressure action twice");
    }
}
