// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import jdk.internal.m3.collections.InternalLeaseRecycler;

/** JDK21-only lifecycle oracle: close may occur inside factory, rebind or action. */
public final class InternalLeaseRecyclerCloseBoundarySelfTest {
    private InternalLeaseRecyclerCloseBoundarySelfTest() { }
    private static int checks;

    static final class Scratch {
        long id = -1;
        int resetCount;
    }

    public static void main(String[] args) throws Exception {
        factoryClosesBeforeRebind();
        rebindClosesBeforeAction();
        rebindClosureSuppressesResetFailure();
        actionClosureStillRunsReset();
        factoryFailureReleasesActiveReservation();
        normalBorrowAndReuseRemainUnchanged();
        nestedBorrowNeverAliasesAnActiveObject();
        highPressureAndConcurrentCloseOnlyDropIdle();
        System.out.println("M3_DOOP_REENTRANT_CLOSE_PASS checks=" + checks);
    }

    private static void factoryClosesBeforeRebind() {
        AtomicReference<InternalLeaseRecycler<Scratch>> ref = new AtomicReference<>();
        AtomicInteger rebound = new AtomicInteger();
        AtomicInteger acted = new AtomicInteger();
        AtomicInteger reset = new AtomicInteger();
        var pool = new InternalLeaseRecycler<>(
                () -> { ref.get().close(); return new Scratch(); },
                (scratch, id) -> { rebound.incrementAndGet(); scratch.id = id; },
                scratch -> { reset.incrementAndGet(); scratch.id = -1; }, 4);
        ref.set(pool);
        expect(IllegalStateException.class, () -> pool.withBorrowed(1, s -> acted.incrementAndGet()));
        equal(0, rebound.get()); equal(0, acted.get()); equal(1, reset.get());
        var stats = pool.stats();
        equal(0, stats.idle()); equal(0, stats.active());
        equal(1L, stats.created()); equal(1L, stats.resets()); equal(1L, stats.discarded());
        expect(IllegalStateException.class, () -> pool.withBorrowed(2, s -> { }));
    }

    private static void rebindClosesBeforeAction() {
        AtomicReference<InternalLeaseRecycler<Scratch>> ref = new AtomicReference<>();
        AtomicInteger acted = new AtomicInteger();
        AtomicInteger reset = new AtomicInteger();
        var pool = new InternalLeaseRecycler<>(Scratch::new,
                (s, id) -> { s.id = id; ref.get().close(); },
                s -> { reset.incrementAndGet(); s.id = -1; }, 8);
        ref.set(pool);
        expect(IllegalStateException.class, () -> pool.withBorrowed(4, s -> acted.incrementAndGet()));
        equal(0, acted.get()); equal(1, reset.get());
        equal(0, pool.stats().active()); equal(0, pool.stats().idle());
        equal(1L, pool.stats().discarded());
    }

    private static void rebindClosureSuppressesResetFailure() {
        AtomicReference<InternalLeaseRecycler<Scratch>> ref = new AtomicReference<>();
        var pool = new InternalLeaseRecycler<>(Scratch::new,
                (s, id) -> ref.get().close(),
                s -> { throw new IllegalArgumentException("reset failed"); }, 4);
        ref.set(pool);
        IllegalStateException thrown = expect(IllegalStateException.class,
                () -> pool.withBorrowed(2, s -> fail("closed-owner action executed")));
        equal(1, thrown.getSuppressed().length);
        check(thrown.getSuppressed()[0] instanceof IllegalArgumentException);
        equal(0, pool.stats().active()); equal(0, pool.stats().idle());
        equal(1L, pool.stats().discarded());
    }

    private static void actionClosureStillRunsReset() {
        AtomicReference<InternalLeaseRecycler<Scratch>> ref = new AtomicReference<>();
        AtomicInteger reset = new AtomicInteger();
        var pool = new InternalLeaseRecycler<>(Scratch::new,
                (s, id) -> s.id = id,
                s -> { s.id = -1; reset.incrementAndGet(); }, 4);
        ref.set(pool);
        IllegalArgumentException original = new IllegalArgumentException("action failure");
        Throwable thrown = expect(IllegalArgumentException.class,
                () -> pool.withBorrowed(5, s -> { ref.get().close(); throw original; }));
        check(thrown == original);
        equal(1, reset.get()); equal(0, pool.stats().idle());
        equal(0, pool.stats().active()); equal(1L, pool.stats().discarded());
    }

    private static void factoryFailureReleasesActiveReservation() {
        var pool = new InternalLeaseRecycler<Scratch>(
                () -> { throw new IllegalStateException("factory failed"); },
                (s, id) -> fail("rebind after failed construction"),
                s -> fail("reset of nonexistent object"), 4);
        expect(IllegalStateException.class, () -> pool.withBorrowed(1, s -> { }));
        equal(0, pool.stats().active()); equal(0, pool.stats().idle());
        equal(0L, pool.stats().created()); equal(0L, pool.stats().resets());
    }

    private static void normalBorrowAndReuseRemainUnchanged() {
        AtomicInteger created = new AtomicInteger();
        var pool = new InternalLeaseRecycler<>(
                () -> { created.incrementAndGet(); return new Scratch(); },
                (s, id) -> s.id = id,
                s -> { s.id = -1; s.resetCount++; }, 2);
        pool.withBorrowed(1, s -> equal(1L, s.id));
        pool.withBorrowed(2, s -> { equal(2L, s.id); equal(1, s.resetCount); });
        equal(1, created.get());
        equal(1, pool.stats().idle()); equal(0, pool.stats().active());
        equal(1L, pool.stats().reused()); equal(2L, pool.stats().resets());
        pool.close();
        equal(0, pool.stats().idle());
    }

    private static void nestedBorrowNeverAliasesAnActiveObject() {
        var pool = new InternalLeaseRecycler<>(Scratch::new,
                (s, id) -> s.id = id,
                s -> s.id = -1, 2);
        pool.withBorrowed(1, outer -> pool.withBorrowed(2, inner -> {
            check(outer != inner);
            equal(1L, outer.id);
            equal(2L, inner.id);
        }));
        equal(2L, pool.stats().created());
        equal(0L, pool.stats().reused());
        equal(2, pool.stats().idle());
        pool.close();
    }

    private static void highPressureAndConcurrentCloseOnlyDropIdle() throws Exception {
        var entered = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicInteger reset = new AtomicInteger();
        var pool = new InternalLeaseRecycler<>(Scratch::new,
                (s, id) -> s.id = id,
                s -> { s.id = -1; reset.incrementAndGet(); }, 2);
        pool.withBorrowed(9, s -> equal(9L, s.id));
        equal(1, pool.stats().idle());
        equal(1, pool.onPressure(RuntimeHeap.Pressure.HIGH));
        equal(0, pool.stats().idle());
        Thread worker = new Thread(() -> {
            try { pool.withBorrowed(10, s -> {
                entered.countDown();
                try { if (!finish.await(3, TimeUnit.SECONDS)) fail("timeout"); }
                catch (InterruptedException e) { throw new IllegalStateException(e); }
            }); } catch (Throwable t) { failure.set(t); }
        });
        worker.start();
        check(entered.await(3, TimeUnit.SECONDS));
        equal(1, pool.stats().active());
        pool.close();
        equal(1, pool.stats().active());
        finish.countDown();
        worker.join(4000);
        check(!worker.isAlive()); check(failure.get() == null);
        equal(0, pool.stats().active()); equal(0, pool.stats().idle());
        equal(2, reset.get());
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks);
    }
    private static void equal(long expected, long actual) { check(expected == actual); }
    private static void fail(String message) { throw new AssertionError(message); }
    private static <E extends Throwable> E expect(Class<E> type, Runnable r) {
        checks++;
        try { r.run(); }
        catch (Throwable t) {
            if (type.isInstance(t)) return type.cast(t);
            throw new AssertionError("wrong exception type", t);
        }
        throw new AssertionError("missing " + type.getName());
    }
}
