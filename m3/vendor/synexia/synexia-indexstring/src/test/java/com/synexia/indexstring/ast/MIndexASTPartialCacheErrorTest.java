// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.ExactStringIndexResolver;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

/** Deterministic Error injection, not a claim of recovery from genuine JVM exhaustion. */
public class MIndexASTPartialCacheErrorTest {
  private static final String SOURCE = "class A { int value() { return 1; } }";
  private static final long WAIT_SECONDS = 5L;

  @Test void loadErrorCompletesWaitersAndAllowsRetry() throws Exception { errorAndRetry(false); }
  @Test void upgradeErrorCompletesWaitersAndAllowsRetry() throws Exception { errorAndRetry(true); }
  @Test void closeCancelsLoadWaiterBeforeOwnerError() throws Exception { closeBeforeError(false); }
  @Test void closeCancelsUpgradeWaiterBeforeOwnerError() throws Exception { closeBeforeError(true); }
  @Test void loadErrorRemainsObservableAfterClose() throws Exception { closeAfterError(false); }
  @Test void upgradeErrorRemainsObservableAfterClose() throws Exception { closeAfterError(true); }
  @Test void interruptedLoadWaiterDoesNotCancelOwner() throws Exception { interruptedWaiter(false); }
  @Test void interruptedUpgradeWaiterDoesNotCancelOwner() throws Exception { interruptedWaiter(true); }

  private static void errorAndRetry(boolean upgrade) throws Exception {
    try (Harness h = new Harness(upgrade)) {
      Future<MIndexASTPartialCache.Resident> owner = h.startOwner();
      Future<MIndexASTPartialCache.Resident> waiter = h.startWaiter();
      h.release.countDown();
      same(h.injected, failure(owner), "owner must propagate identical Error");
      Throwable waitingFailure = failure(waiter);
      check(waitingFailure instanceof MIndexASTPartialCache.LoadException, "waiter LoadException");
      same(h.injected, waitingFailure.getCause(), "waiter preserves Error cause");
      same(h.key, ((MIndexASTPartialCache.LoadException) waitingFailure).key(), "waiter key");
      check(h.calls.get() == 1, "single failed owner");
      check(h.cache.contains(h.key) == upgrade, "no partial failed admission");
      if (upgrade) same(h.base, h.cache.getIfPresent(h.key).orElseThrow(), "base remains resident");
      var retried = h.cache.get(h.key, h.detail);
      check(retried.satisfies(h.detail), "retry satisfies detail");
      same(h.document, retried.document(), "retry uses genuine document");
      check(h.calls.get() == 2, "failure removed flight so retry can run");
      same(retried, h.cache.get(h.key, h.detail), "retry admitted normally");
      if (!upgrade) check(h.cache.snapshot().loadFailures() == 1, "Error counted as failed load");
    }
  }

  private static void closeBeforeError(boolean upgrade) throws Exception {
    try (Harness h = new Harness(upgrade)) {
      Future<MIndexASTPartialCache.Resident> owner = h.startOwner();
      Future<MIndexASTPartialCache.Resident> waiter = h.startWaiter();
      h.cache.close();
      Throwable cancelled = failure(waiter);
      check(cancelled instanceof MIndexASTPartialCache.LoadException, "closed waiter envelope");
      check(cancelled.getCause() instanceof java.util.concurrent.CancellationException,
          "close retains cancellation outcome");
      h.release.countDown();
      same(h.injected, failure(owner), "close must not swallow owner's Error");
      check(h.cache.snapshot().lru().entries() == 0, "closed cache empty");
      try { h.cache.get(h.key); throw new AssertionError("closed cache admitted retry"); }
      catch (IllegalStateException expected) { /* Explicit close remains terminal. */ }
    }
  }

  private static void interruptedWaiter(boolean upgrade) throws Exception {
    try (Harness h = new Harness(upgrade)) {
      Future<MIndexASTPartialCache.Resident> owner = h.startOwner();
      AtomicReference<Thread> waiting = new AtomicReference<>();
      Future<Boolean> interrupted = h.executor.submit(() -> {
        waiting.set(Thread.currentThread());
        try { h.cache.get(h.key, h.detail); throw new AssertionError("unexpected success"); }
        catch (MIndexASTPartialCache.LoadException expected) {
          check(expected.getCause() instanceof InterruptedException, "interruption cause");
          return Thread.currentThread().isInterrupted();
        }
      });
      awaitJoined(waiting);
      waiting.get().interrupt();
      check(interrupted.get(WAIT_SECONDS, TimeUnit.SECONDS), "interrupt flag restored");
      check(!owner.isDone(), "waiter interruption must not cancel owner");
      Future<MIndexASTPartialCache.Resident> survivor = h.startWaiter();
      h.release.countDown();
      same(h.injected, failure(owner), "owner Error after waiter interruption");
      same(h.injected, failure(survivor).getCause(), "other waiter still gets owner Error");
      check(h.cache.get(h.key, h.detail).satisfies(h.detail), "retry after interrupted waiter");
    }
  }

  private static void closeAfterError(boolean upgrade) throws Exception {
    try (Harness h = new Harness(upgrade)) {
      Future<MIndexASTPartialCache.Resident> owner = h.startOwner();
      Future<MIndexASTPartialCache.Resident> waiter = h.startWaiter();
      h.release.countDown();
      same(h.injected, failure(owner), "owner has finished failure cleanup");
      h.cache.close();
      same(h.injected, failure(waiter).getCause(), "completed Error survives subsequent close");
      check(h.cache.snapshot().lru().entries() == 0, "closed cache empty after Error");
    }
  }

  private static final class Harness implements AutoCloseable {
    final MIndexASTPartialCache.Key key = MIndexASTPartialCache.Key.java21Source("A.java", SOURCE);
    final MIndexASTDocument document = new MIndexASTParser(
        new MIndexASTPool(new ExactStringIndexResolver(), 0)).parse("A.java", SOURCE);
    final OutOfMemoryError injected = new OutOfMemoryError("deterministic test injection; no exhaustion");
    final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
    final AtomicInteger calls = new AtomicInteger();
    final ExecutorService executor = Executors.newFixedThreadPool(3, runnable -> {
      Thread thread = new Thread(runnable, "partial-ast-error-proof");
      thread.setDaemon(true);
      return thread;
    });
    final MIndexASTPartialCache cache;
    final MIndexASTPartialCache.Detail detail;
    final MIndexASTPartialCache.Resident base;

    Harness(boolean upgrade) throws Exception {
      detail = upgrade ? MIndexASTPartialCache.Detail.NAVIGATION : MIndexASTPartialCache.Detail.DOCUMENT;
      MIndexASTPartialCache.Loader loader = upgrade ? new MIndexASTPartialCache.DetailLoader() {
        public MIndexASTPartialCache.Resident load(MIndexASTPartialCache.Key ignored,
            MIndexASTPartialCache.Detail requested) {
          return MIndexASTPartialCache.Resident.document(document);
        }
        public MIndexASTPartialCache.Resident upgrade(MIndexASTPartialCache.Key ignored,
            MIndexASTPartialCache.Resident resident, MIndexASTPartialCache.Detail requested) throws Exception {
          maybeFail();
          return resident.upgrade(requested);
        }
      } : ignored -> { maybeFail(); return document; };
      cache = new MIndexASTPartialCache(8, 10_000_000L, loader);
      base = upgrade ? cache.get(key) : null;
    }

    void maybeFail() throws InterruptedException {
      if (calls.incrementAndGet() == 1) {
        entered.countDown();
        check(release.await(WAIT_SECONDS, TimeUnit.SECONDS), "owner release timeout");
        throw injected;
      }
    }

    Future<MIndexASTPartialCache.Resident> startOwner() throws Exception {
      var owner = executor.submit(() -> cache.get(key, detail));
      check(entered.await(WAIT_SECONDS, TimeUnit.SECONDS), "owner entry timeout");
      return owner;
    }

    Future<MIndexASTPartialCache.Resident> startWaiter() throws Exception {
      AtomicReference<Thread> waiting = new AtomicReference<>();
      var waiter = executor.submit(() -> {
        waiting.set(Thread.currentThread());
        return cache.get(key, detail);
      });
      awaitJoined(waiting);
      return waiter;
    }

    public void close() {
      release.countDown();
      cache.close();
      executor.shutdownNow();
      try {
        check(executor.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS), "worker cleanup timeout");
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new AssertionError("worker cleanup interrupted", interrupted);
      }
    }
  }

  private static void awaitJoined(AtomicReference<Thread> waiting) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
    while (System.nanoTime() < deadline) {
      Thread thread = waiting.get();
      if (thread != null && Arrays.stream(thread.getStackTrace()).anyMatch(frame ->
          frame.getClassName().equals(MIndexASTPartialCache.class.getName())
              && frame.getMethodName().equals("await"))) return;
      LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
    }
    throw new AssertionError("waiter did not join registered flight");
  }

  private static Throwable failure(Future<?> future) throws Exception {
    try { future.get(2, TimeUnit.SECONDS); }
    catch (ExecutionException expected) { return expected.getCause(); }
    throw new AssertionError("expected exceptional completion");
  }
  private static void same(Object expected, Object actual, String message) {
    check(expected == actual, message);
  }
  private static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  /** Standalone bounded runner for scoped source verification. */
  public static void main(String[] args) throws Exception {
    int failures = 0;
    for (var method : MIndexASTPartialCacheErrorTest.class.getDeclaredMethods()) {
      if (!method.isAnnotationPresent(Test.class)) continue;
      try { method.invoke(new MIndexASTPartialCacheErrorTest()); System.out.println("PASS " + method.getName()); }
      catch (java.lang.reflect.InvocationTargetException failed) {
        failures++;
        System.out.println("FAIL " + method.getName() + " " + failed.getCause());
      }
    }
    if (failures != 0) throw new AssertionError(failures + " Error lifecycle checks failed");
  }
}
