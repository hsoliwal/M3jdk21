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
