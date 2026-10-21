/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.file;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Source-owned Java21 run: exact rule geometry, thread ownership and interruption. */
@SuppressWarnings("try")
public final class M3ResourceLocksContract {
    private static int checks;
    private M3ResourceLocksContract() { }
    private static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why + " at " + checks);
    }
    private static void rejects(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong failure", failure);
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }

    private static void scopeGeometry() {
        var root = M3ResourceLocks.Scope.subtree(Path.of("/m3/project"));
        var path = M3ResourceLocks.Scope.exact(Path.of("/m3/project/src/X.java"));
        var sibling = M3ResourceLocks.Scope.exact(Path.of("/m3/projects/src/X.java"));
        var child = M3ResourceLocks.Scope.subtree(Path.of("/m3/project/src"));
        check(root.contains(root) && path.contains(path), "scope containment reflexive");
        check(root.contains(path) && root.contains(child), "ancestor containment");
        check(!path.contains(root) && !path.contains(child), "not a larger-scope containment");
        check(root.conflicts(path) && path.conflicts(root), "symmetric ancestor conflicts");
        check(root.conflicts(root) && path.conflicts(path), "self conflict");
        check(!root.conflicts(sibling) && !sibling.conflicts(root), "path component prefixes aren't ancestry");
        check(!path.conflicts(sibling) && !path.contains(sibling), "unrelated scope");
        check(!root.contains(null) && !root.conflicts(null), "unknown rule is not admitted");
        check(M3ResourceLocks.Scope.exact(Path.of("/m3/project/a/../src/X.java")).equals(path),
                "lexical dot-segment normalization without IO");
        var exactDir = M3ResourceLocks.Scope.exact(Path.of("/m3/project"));
        check(!exactDir.contains(child) && exactDir.conflicts(root),
                "exact directory conflicts with identical subtree root but doesn't contain descendants");
        Random random = new Random(0x4d3352554c45534cL);
        for (int i = 0; i < 70_000; i++) {
            int a = random.nextInt(45), b = random.nextInt(45);
            boolean recA = random.nextBoolean(), recB = random.nextBoolean();
            String left = "/tree/" + (a / 9) + "/" + (a % 9);
            String right = "/tree/" + (b / 9) + "/" + (b % 9);
            var sa = new M3ResourceLocks.Scope(Path.of(left), recA);
            var sb = new M3ResourceLocks.Scope(Path.of(right), recB);
            boolean same = a==b;
            boolean expectContains = same && (recA || !recB);
            check(sa.contains(sb) == expectContains, "exact two-segment containment reference");
            check(sa.conflicts(sb) == same && sb.conflicts(sa) == same,
                    "same-path exclusive conflicts remain symmetric");
        }
    }

    private static void lifecycle() throws Exception {
        M3ResourceLocks locks = new M3ResourceLocks();
        var root = M3ResourceLocks.Scope.subtree(Path.of("/m3/root"));
        var child = M3ResourceLocks.Scope.exact(Path.of("/m3/root/a"));
        var sibling = M3ResourceLocks.Scope.exact(Path.of("/m3/root/b"));
        var unrelated = M3ResourceLocks.Scope.exact(Path.of("/other"));
        check(locks.activeScopes()==0, "initial empty");
        M3ResourceLocks.Lease parent = locks.acquire(root);
        check(locks.activeScopes()==1, "one held scope");
        M3ResourceLocks.Lease nested = locks.acquire(child);
        check(locks.activeScopes()==2, "nested child admitted");
        rejects(IllegalStateException.class, parent::close);
        rejects(IllegalStateException.class, () -> locks.tryAcquire(unrelated));
        rejects(IllegalStateException.class, () -> locks.tryAcquire(sibling));
        nested.close();
        check(locks.activeScopes()==1 && nested.isClosed(), "nested closure retains outer lock");
        rejects(IllegalStateException.class, nested::close);
        parent.close();
        check(locks.activeScopes()==0 && parent.isClosed(), "final lock released");
        check(locks.call(child, () -> "returned").equals("returned"), "Callable result preserved");
        RuntimeException original = new RuntimeException("task witness");
        try {
            locks.run(child, () -> { throw original; });
            throw new AssertionError("missing task exception");
        } catch (RuntimeException error) { check(error==original, "Runnable error reference unchanged"); }
        check(locks.activeScopes()==0, "callback failure releases scope");
    }

    private static void contention() throws Exception {
        M3ResourceLocks locks = new M3ResourceLocks();
        var root = M3ResourceLocks.Scope.subtree(Path.of("/state"));
        var child = M3ResourceLocks.Scope.exact(Path.of("/state/sub"));
        var sibling = M3ResourceLocks.Scope.exact(Path.of("/unrelated"));
        CountDownLatch contenderStarted = new CountDownLatch(1);
        CountDownLatch contenderFinished = new CountDownLatch(1);
        AtomicBoolean entered = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (M3ResourceLocks.Lease outer = locks.acquire(root)) {
            Thread wait = new Thread(() -> {
                contenderStarted.countDown();
                try (M3ResourceLocks.Lease ignored = locks.acquire(child)) {
                    entered.set(true);
                } catch (Throwable thrown) { failure.set(thrown); }
                finally { contenderFinished.countDown(); }
            }, "resource-conflict-contender");
            wait.start();
            check(contenderStarted.await(5,TimeUnit.SECONDS), "worker started");
            check(!contenderFinished.await(100,TimeUnit.MILLISECONDS) && !entered.get(),
                    "conflicting child cannot publish under locked ancestor");
            check(locks.activeScopes()==1, "queued lock is not active");
            CountDownLatch disjointEntered = new CountDownLatch(1);
            CountDownLatch disjointRelease = new CountDownLatch(1);
            Thread independent = new Thread(() -> {
                try (M3ResourceLocks.Lease disjoint = locks.acquire(sibling)) {
                    disjointEntered.countDown();
                    disjointRelease.await();
                } catch (Throwable thrown) { failure.set(thrown); }
            });
            independent.start();
            check(disjointEntered.await(5,TimeUnit.SECONDS) && locks.activeScopes()==2,
                    "unrelated scopes coexist on different threads");
            disjointRelease.countDown();
            independent.join(5000);
            check(locks.activeScopes()==1, "disjoint closed without altering root");
            wait.interrupt();
            check(contenderFinished.await(5,TimeUnit.SECONDS), "interrupted wait completed");
            check(failure.get() instanceof InterruptedException && !entered.get(),
                    "no lock was taken after interruption");
            wait.join(5000);
        }
        check(locks.activeScopes()==0, "outer release leaves empty lock array");
        CountDownLatch workerDone = new CountDownLatch(1);
        AtomicBoolean secondEntered = new AtomicBoolean();
        M3ResourceLocks.Lease outer = locks.acquire(root);
        Thread follower = new Thread(() -> {
            try (M3ResourceLocks.Lease lease = locks.acquire(child)) {
                secondEntered.set(true);
            } catch (Throwable thrown) { failure.set(thrown); }
            finally { workerDone.countDown(); }
        });
        follower.start();
        check(!workerDone.await(100,TimeUnit.MILLISECONDS), "child blocked while parent retained");
        AtomicReference<Throwable> foreignClose = new AtomicReference<>();
        Thread incorrect = new Thread(() -> {
            try { outer.close(); } catch (Throwable thrown) { foreignClose.set(thrown); }
        });
        incorrect.start(); incorrect.join(5000);
        check(foreignClose.get() instanceof IllegalStateException && !outer.isClosed(),
                "wrong thread cannot release another owner's scope");
        outer.close();
        check(workerDone.await(5,TimeUnit.SECONDS) && secondEntered.get(),
                "blocked child acquired only after release");
        follower.join(5000);
        check(locks.activeScopes()==0, "all lease references released after contention");
    }

    public static void main(String[] args) throws Exception {
        scopeGeometry(); lifecycle(); contention();
        System.out.println("M3_RESOURCE_LOCKS_PASS checks="+checks);
    }
}
