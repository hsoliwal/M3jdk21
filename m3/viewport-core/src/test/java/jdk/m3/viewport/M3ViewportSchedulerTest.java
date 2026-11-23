/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.viewport;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class M3ViewportSchedulerTest {

    static final class ManualExecutor implements Executor, M3ViewportScheduler.UiExecutor {
        final Queue<Runnable> tasks = new ArrayDeque<>();
        @Override public void execute(Runnable task) { tasks.add(task); }
        void step() { tasks.remove().run(); }
        void finish() { while (!tasks.isEmpty()) step(); }
    }

    @Test
    void burstCoalescesCpuAndUiWork() {
        ManualExecutor worker = new ManualExecutor(), ui = new ManualExecutor();
        List<String> applied = new ArrayList<>();
        var scheduler = new M3ViewportScheduler<Integer, String>(worker, ui,
                (request, token) -> "row:" + request,
                (g, snapshot) -> applied.add(g + ":" + snapshot));
        for (int i = 1; i <= 100_000; i++) scheduler.request(i);
        assertEquals(1, worker.tasks.size());
        worker.finish();
        assertEquals(1, ui.tasks.size());
        ui.finish();
        assertEquals(List.of("100000:row:100000"), applied);
    }

    @Test
    void invalidatesComputedButNotPresentedFrame() {
        ManualExecutor worker = new ManualExecutor(), ui = new ManualExecutor();
        List<String> applied = new ArrayList<>();
        var scheduler = new M3ViewportScheduler<Integer, String>(worker, ui,
                (request, token) -> "frame:" + request,
                (g, snapshot) -> applied.add(snapshot));
        scheduler.request(1);
        worker.finish(); // frame 1 is waiting on the UI queue
        scheduler.request(2);
        worker.finish();
        assertEquals(1, ui.tasks.size());
        ui.finish();
        assertEquals(List.of("frame:2"), applied);
    }

    @Test
    void invalidatesInFlightCpuComputation() {
        ManualExecutor worker = new ManualExecutor(), ui = new ManualExecutor();
        List<Integer> applied = new ArrayList<>();
        AtomicReference<M3ViewportScheduler<Integer, Integer>> ref = new AtomicReference<>();
        var scheduler = new M3ViewportScheduler<Integer, Integer>(worker, ui,
                (request, token) -> {
                    if (request == 1) {
                        ref.get().request(2); // simulate a resize while calculating
                        token.check();
                    }
                    return request;
                }, (g, result) -> applied.add(result));
        ref.set(scheduler);
        scheduler.request(1);
        worker.finish();
        ui.finish();
        assertEquals(List.of(2), applied);
    }

    @Test
    void closeDiscardsPendingAndRejectsRequests() {
        ManualExecutor worker = new ManualExecutor(), ui = new ManualExecutor();
        List<Integer> applied = new ArrayList<>();
        var scheduler = new M3ViewportScheduler<Integer, Integer>(worker, ui,
                (request, token) -> request, (g, result) -> applied.add(result));
        scheduler.request(3);
        worker.finish();
        scheduler.close();
        ui.finish();
        assertTrue(applied.isEmpty());
        assertThrows(IllegalStateException.class, () -> scheduler.request(4));
    }

    @Test
    void checkedCpuExecutorRejectsUiCallerRunsFallback() {
        Thread uiThread = Thread.currentThread();
        Executor callerRuns = Runnable::run;
        Executor guarded = M3ViewportScheduler.requireBackgroundExecutor(uiThread, callerRuns);
        AtomicReference<String> computed = new AtomicReference<>();
        assertThrows(IllegalStateException.class, () -> guarded.execute(() -> computed.set("unsafe")));
        assertNull(computed.get(), "GUI thread must never perform CPU viewport computation");
        assertThrows(NullPointerException.class, () ->
                M3ViewportScheduler.requireBackgroundExecutor(null, callerRuns));
        assertThrows(NullPointerException.class, () ->
                M3ViewportScheduler.requireBackgroundExecutor(uiThread, null));
    }

    @Test
    void checkedUiExecutorRefusesOffThreadPresentation() throws InterruptedException {
        Thread uiThread = Thread.currentThread();
        M3ViewportScheduler.UiExecutor direct = Runnable::run;
        M3ViewportScheduler.UiExecutor guarded = M3ViewportScheduler.requireUiExecutor(uiThread, direct);
        AtomicReference<Thread> consumer = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread wrong = new Thread(() -> {
            try {
                guarded.execute(() -> consumer.set(Thread.currentThread()));
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        }, "wrong-viewport-publication");
        wrong.start();
        wrong.join(3000);
        assertFalse(wrong.isAlive(), "wrong-thread dispatch must finish or fail");
        assertInstanceOf(IllegalStateException.class, failure.get());
        assertNull(consumer.get(), "never invoke GUI callbacks from a CPU worker");
        guarded.execute(() -> consumer.set(Thread.currentThread()));
        assertSame(uiThread, consumer.get());
        assertThrows(NullPointerException.class, () ->
                M3ViewportScheduler.requireUiExecutor(null, direct));
        assertThrows(NullPointerException.class, () ->
                M3ViewportScheduler.requireUiExecutor(uiThread, null));
    }

    @Test
    void checkedCpuAndUiExecutorsPreserveLatestGeneration() throws InterruptedException {
        Thread uiThread = Thread.currentThread();
        ManualExecutor cpu = new ManualExecutor(), ui = new ManualExecutor();
        AtomicReference<String> presented = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        var scheduler = new M3ViewportScheduler<Integer, String>(
                M3ViewportScheduler.requireBackgroundExecutor(uiThread, cpu),
                M3ViewportScheduler.requireUiExecutor(uiThread, ui),
                (request, token) -> {
                    assertNotSame(uiThread, Thread.currentThread());
                    return "cpu:" + request;
                },
                (generation, snapshot) -> {
                    assertSame(uiThread, Thread.currentThread());
                    presented.set(generation + ":" + snapshot);
                });
        scheduler.request(1);
        scheduler.request(2);
        Thread worker = new Thread(() -> {
            try {
                cpu.finish();
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        }, "cpu-viewport-worker");
        worker.start();
        worker.join(3000);
        assertFalse(worker.isAlive(), "worker must complete");
        assertNull(failure.get(), "guarded worker failed");
        assertNull(presented.get(), "CPU worker cannot present");
        assertEquals(1, ui.tasks.size());
        ui.finish();
        assertEquals("2:cpu:2", presented.get());
    }

    @Test
    void computesFailureOnWorkerButDeliversOnUi() {
        ManualExecutor worker = new ManualExecutor(), ui = new ManualExecutor();
        List<String> seen = new ArrayList<>();
        var scheduler = new M3ViewportScheduler<Integer, Integer>(worker, ui,
                (request, token) -> { throw new IllegalArgumentException("bad row"); },
                new M3ViewportScheduler.Receiver<Integer>() {
                    @Override public void present(long generation, Integer snapshot) {
                        fail("unexpected snapshot");
                    }
                    @Override public void failed(long generation, Exception error) {
                        seen.add(generation + ":" + error.getMessage());
                    }
                });
        scheduler.request(9);
        worker.finish();
        assertTrue(seen.isEmpty());
        ui.finish();
        assertEquals(List.of("1:bad row"), seen);
    }
}
