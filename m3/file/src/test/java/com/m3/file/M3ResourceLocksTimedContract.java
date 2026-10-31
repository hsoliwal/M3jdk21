// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Contract for the original M3 resource lock owner with bounded admission. */
@SuppressWarnings("try")
public final class M3ResourceLocksTimedContract {
    private M3ResourceLocksTimedContract() { }

    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }

    public static void main(String[] args) throws Exception {
        M3ResourceLocks locks = new M3ResourceLocks();
        M3ResourceLocks.Scope root =
                M3ResourceLocks.Scope.subtree(Path.of("/m3/timed-lock-proof"));
        M3ResourceLocks.Scope child =
                M3ResourceLocks.Scope.exact(Path.of("/m3/timed-lock-proof/a"));

        try {
            locks.tryAcquire(child, -1L, TimeUnit.MILLISECONDS);
            throw new AssertionError("negative timeout accepted");
        } catch (IllegalArgumentException expected) {
            // Rejected without mutation.
        }
        try (M3ResourceLocks.Lease lease = locks.tryAcquire(child, 0, TimeUnit.NANOSECONDS)) {
            check(lease != null && locks.activeScopes() == 1, "free immediate acquisition");
        }

        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch timedStarted = new CountDownLatch(1);
        AtomicBoolean expired = new AtomicBoolean();
        M3ResourceLocks.Lease outer = locks.acquire(root);
        try {
            Thread timeout = new Thread(() -> {
                timedStarted.countDown();
                try (M3ResourceLocks.Lease lease =
                        locks.tryAcquire(child, 75, TimeUnit.MILLISECONDS)) {
                    expired.set(lease == null);
                } catch (Throwable thrown) {
                    failure.set(thrown);
                }
            }, "m3-timeout-contender");
            timeout.start();
            check(timedStarted.await(5, TimeUnit.SECONDS), "timeout contender not started");
            timeout.join(5000);
            check(!timeout.isAlive() && failure.get() == null && expired.get(),
                    "conflicting wait must time out without a lease");
            check(locks.activeScopes() == 1, "timeout published a hidden lease");

            CountDownLatch followerStarted = new CountDownLatch(1);
            AtomicBoolean acquired = new AtomicBoolean();
            Thread follower = new Thread(() -> {
                followerStarted.countDown();
                try (M3ResourceLocks.Lease lease =
                        locks.tryAcquire(child, 2, TimeUnit.SECONDS)) {
                    acquired.set(lease != null);
                } catch (Throwable thrown) {
                    failure.set(thrown);
                }
            }, "m3-release-contender");
            follower.start();
            check(followerStarted.await(5, TimeUnit.SECONDS), "release contender not started");
            check(!acquired.get(), "conflicting scope entered before release");
            outer.close();
            follower.join(5000);
            check(!follower.isAlive() && failure.get() == null && acquired.get(),
                    "release did not admit waiting scope");
        } finally {
            if (!outer.isClosed()) outer.close();
        }
        check(locks.activeScopes() == 0, "lease not released");

        try (M3ResourceLocks.Lease held = locks.acquire(root)) {
            CountDownLatch interruptStarted = new CountDownLatch(1);
            AtomicBoolean interrupted = new AtomicBoolean();
            Thread blocked = new Thread(() -> {
                interruptStarted.countDown();
                try {
                    locks.tryAcquire(child, 5, TimeUnit.SECONDS);
                } catch (InterruptedException expected) {
                    interrupted.set(true);
                } catch (Throwable thrown) {
                    failure.set(thrown);
                }
            }, "m3-interrupted-contender");
            blocked.start();
            check(interruptStarted.await(5, TimeUnit.SECONDS), "interrupt contender not started");
            blocked.interrupt();
            blocked.join(5000);
            check(interrupted.get() && !blocked.isAlive() && failure.get() == null,
                    "interrupt was not respected");
            check(locks.activeScopes() == 1, "interrupted waiter published lease");
            try (M3ResourceLocks.Lease nested =
                    locks.tryAcquire(child, 0, TimeUnit.NANOSECONDS)) {
                check(nested != null, "containing same-thread lock must be admitted");
            }
        }
        check(locks.activeScopes() == 0, "resource lease leak");
        System.out.println("M3_TIMED_RESOURCE_LOCKS_PASS");
    }
}
