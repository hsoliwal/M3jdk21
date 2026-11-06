/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Runnable Java21 regression oracle; does not assume GC or JUnit availability. */
public final class M3InternalLeaseRecyclerSelfTest {
    private M3InternalLeaseRecyclerSelfTest() { }
    private static final class Buffer {
        long id = -1;
        final byte[] bits = new byte[32];
        Object reference;
    }
    private static M3InternalLeaseRecycler<Buffer> pool(int maxIdle) {
        return new M3InternalLeaseRecycler<>(Buffer::new,
                (buffer, id) -> {
                    check(buffer.id == -1, "dirty prior ID");
                    check(buffer.reference == null, "dirty reference");
                    buffer.id = id;
                },
                buffer -> {
                    buffer.id = -1;
                    buffer.reference = null;
                    Arrays.fill(buffer.bits, (byte) 0);
                }, maxIdle);
    }
    private static void check(boolean success, String detail) {
        if (!success) throw new AssertionError(detail);
    }
    private static void failed(Runnable action) {
        try { action.run(); } catch (RuntimeException expected) { return; }
        throw new AssertionError("expected exception");
    }
    public static void main(String[] args) throws Exception {
        try (var recycler = pool(2)) {
            AtomicReference<Buffer> identity = new AtomicReference<>();
            for (int i = 0; i < 1000; i++) {
                final long id = i;
                recycler.withBorrowed(id, buffer -> {
                    if (identity.get() == null) identity.set(buffer);
                    else check(identity.get() == buffer, "serial borrow did not reuse");
                    check(buffer.id == id, "wrong rebound ID");
                    check(buffer.reference == null && buffer.bits[0] == 0, "not cleared");
                    buffer.reference = new Object();
                    buffer.bits[0] = 1;
                });
            }
            check(recycler.stats().created() == 1 && recycler.stats().reused() == 999,
                    "expected one allocation and 999 object reuses");
            recycler.withBorrowed(1L, outer -> recycler.withBorrowed(2L, inner ->
                    check(outer != inner, "nested checkout aliased a live borrow")));
            check(recycler.stats().active() == 0 && recycler.stats().idle() == 2,
                    "nested release mismatch");
            failed(() -> recycler.withBorrowed(3L, buffer -> {
                buffer.reference = new Object();
                throw new IllegalArgumentException("callback");
            }));
            recycler.withBorrowed(4L, buffer ->
                    check(buffer.reference == null && buffer.id == 4L, "exception left dirty state"));
            check(recycler.onPressure(M3RuntimeHeap.Pressure.HIGH) == 2,
                    "high pressure must trim all idle objects");
            check(recycler.stats().idle() == 0, "idle not emptied");
            recycler.withBorrowed(5L, buffer -> {});
            check(recycler.stats().idle() == 0, "high pressure re-admitted idle");
            recycler.onPressure(M3RuntimeHeap.Pressure.NORMAL);
            recycler.withBorrowed(6L, buffer -> {});
            check(recycler.stats().idle() == 1, "normal-pressure recovery failed");
            check(recycler.trimIdle(0) == 1, "explicit trim failed");
            failed(() -> recycler.withBorrowed(-1L, ignored -> {}));
        }
        // A failed reset must drop the object and release the active count.
        var badReset = new M3InternalLeaseRecycler<>(Buffer::new,
                (buffer, id) -> buffer.id = id,
                buffer -> { throw new IllegalStateException("reset refused"); }, 1);
        failed(() -> badReset.withBorrowed(1L, ignored -> {}));
        check(badReset.stats().active() == 0 && badReset.stats().idle() == 0,
                "failed sanitizer retained the object");
        badReset.close();
        failed(() -> badReset.withBorrowed(2L, ignored -> {}));
        // Prove distinct identities while two concurrent threads hold leases.
        try (var concurrent = pool(1)) {
            CountDownLatch entered = new CountDownLatch(2);
            CountDownLatch release = new CountDownLatch(1);
            AtomicReference<Buffer> first = new AtomicReference<>();
            AtomicReference<Buffer> second = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread a = Thread.ofPlatform().start(() -> borrow(concurrent, 1, first, entered, release, error));
            Thread b = Thread.ofPlatform().start(() -> borrow(concurrent, 2, second, entered, release, error));
            try {
                check(entered.await(10, TimeUnit.SECONDS), "concurrent checkout stalled");
                check(first.get() != second.get(), "concurrent checkout shared an object");
            } finally {
                release.countDown();
            }
            a.join(10000); b.join(10000);
            check(!a.isAlive() && !b.isAlive(), "borrow threads did not finish");
            check(error.get() == null, "parallel borrow failure: " + error.get());
            check(concurrent.stats().active() == 0, "outstanding lease");
        }
        var closing = pool(1);
        {
            closing.withBorrowed(7, b -> closing.close());
            check(closing.stats().idle() == 0 && closing.stats().active() == 0,
                    "close recycled a live object");
            failed(() -> closing.withBorrowed(8, ignored -> {}));
        }
        System.out.println("PASS 1000 serial borrows, nested, concurrent, reset failure, pressure and close");
    }
    private static void borrow(M3InternalLeaseRecycler<Buffer> recycler, long id,
            AtomicReference<Buffer> output, CountDownLatch entered,
            CountDownLatch release, AtomicReference<Throwable> error) {
        try {
            recycler.withBorrowed(id, value -> {
                output.set(value);
                entered.countDown();
                try { release.await(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            });
        } catch (Throwable failure) { error.compareAndSet(null, failure); }
    }
}
