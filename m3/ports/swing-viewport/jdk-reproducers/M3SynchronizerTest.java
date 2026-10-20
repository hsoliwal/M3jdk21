/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary M3JDK brief rule 28: M3Synchronizer priorities, keyed coalescing, frame ticks, the invokeAndWait wait log
 * @run main/othervm -Djava.awt.headless=true -Dswing.synchronizer.syncWarnMillis=200 M3SynchronizerTest
 */

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.M3Synchronizer;
import javax.swing.SwingUtilities;
import javax.swing.event.FrameListener;

/**
 * Brief rule 28, M3Synchronizer: the stock FIFO of unkeyed runnables; keyed coalescing in place (the newest wins,
 * the key is free again once run); INPUT before LAYOUT before ordinary events already queued; MODEL runnables run
 * together at one frame tick, then the frame listeners with the next generation; frame pacing; IDLE after the queued
 * events; requestFrame with nothing queued; a throwing MODEL runnable does not lose the others; ordinary events keep
 * their posting order with keyed runnables interleaved (10^4 posts); and a long invokeAndWait wait is logged once at
 * WARNING with both stacks while the call still completes. Run with -Dswing.synchronizer.syncWarnMillis=200.
 */
public final class M3SynchronizerTest {

    static final List<String> RUN = Collections.synchronizedList(new ArrayList<>());
    static int failures;
    static int passes;

    static void check(String what, boolean ok, Object detail) {
        if (ok) {
            passes++;
        } else {
            failures++;
        }
        System.out.println((ok ? "PASS\t" : "FAIL\t") + what + "\t" + detail);
    }

    static Runnable rec(String name) {
        return () -> RUN.add(name);
    }

    /** Blocks the EDT until the returned latch is released, so posts queue up behind it. */
    static CountDownLatch blockEdt() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch blocked = new CountDownLatch(1);
        EventQueue.invokeLater(() -> {
            blocked.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        blocked.await();
        return release;
    }

    static void drain() throws Exception {
        for (int i = 0; i < 3; i++) {
            Thread.sleep(60);
            SwingUtilities.invokeAndWait(() -> { });
        }
    }

    static List<String> take() throws Exception {
        drain();
        List<String> r = new ArrayList<>(RUN);
        RUN.clear();
        return r;
    }

    public static void main(String[] args) throws Exception {
        CountDownLatch g = blockEdt();
        M3Synchronizer.asyncExec(null, rec("a"));
        EventQueue.invokeLater(rec("b"));
        M3Synchronizer.asyncExec(M3Synchronizer.DEFAULT, null, rec("c"));
        g.countDown();
        check("unkeyed DEFAULT is the stock FIFO", take().equals(List.of("a", "b", "c")), RUN);

        g = blockEdt();
        Object k = "k";
        M3Synchronizer.asyncExec(k, rec("k1"));
        EventQueue.invokeLater(rec("x"));
        M3Synchronizer.asyncExec(k, rec("k2"));
        g.countDown();
        List<String> got = take();
        check("keyed DEFAULT: the newest replaces the queued one in place", got.equals(List.of("k2", "x")), got);
        M3Synchronizer.asyncExec(k, rec("k3"));
        got = take();
        check("the key is free again once run", got.equals(List.of("k3")), got);

        g = blockEdt();
        EventQueue.invokeLater(rec("ordinary"));
        M3Synchronizer.asyncExec(M3Synchronizer.LAYOUT, null, rec("layout"));
        M3Synchronizer.asyncExec(M3Synchronizer.INPUT, null, rec("input"));
        M3Synchronizer.asyncExec(M3Synchronizer.IDLE, null, rec("idle"));
        g.countDown();
        got = take();
        check("INPUT, then LAYOUT, then the queued ordinary event, then IDLE",
                got.equals(List.of("input", "layout", "ordinary", "idle")), got);

        List<Long> frames = Collections.synchronizedList(new ArrayList<>());
        FrameListener fl = gen -> {
            frames.add(gen);
            RUN.add("frame");
        };
        M3Synchronizer.addFrameListener(fl);
        long g0 = M3Synchronizer.getFrameGeneration();
        Object m1 = new Object();
        Object m2 = new Object();
        Thread model = new Thread(() -> {
            M3Synchronizer.asyncExec(M3Synchronizer.MODEL, m1, rec("m1a"));
            M3Synchronizer.asyncExec(M3Synchronizer.MODEL, m2, rec("m2"));
            M3Synchronizer.asyncExec(M3Synchronizer.MODEL, m1, rec("m1b"));
            M3Synchronizer.asyncExec(M3Synchronizer.MODEL, null, rec("m3"));
        });
        model.start();
        model.join();
        got = take();
        check("MODEL runnables run together at one tick, coalesced, then the frame listener",
                got.equals(List.of("m1b", "m2", "m3", "frame")), got);
        check("one frame, generation + 1", frames.equals(List.of(g0 + 1))
                && M3Synchronizer.getFrameGeneration() == g0 + 1, frames);

        frames.clear();
        long t0 = System.nanoTime();
        M3Synchronizer.asyncExec(M3Synchronizer.MODEL, null, rec("p1"));
        Thread.sleep(1);
        while (frames.isEmpty()) {
            Thread.sleep(1);
        }
        M3Synchronizer.asyncExec(M3Synchronizer.MODEL, null, rec("p2"));
        long first = System.nanoTime();
        while (frames.size() < 2) {
            Thread.sleep(1);
        }
        long gap = (System.nanoTime() - first) / 1_000_000L;
        take();
        check("frames are paced by swing.synchronizer.frameMillis (16 ms)", gap >= 10, gap + " ms after the "
                + "previous frame, first after " + (first - t0) / 1_000_000L + " ms");

        frames.clear();
        M3Synchronizer.requestFrame();
        got = take();
        check("requestFrame runs the frame listeners with nothing queued", got.equals(List.of("frame"))
                && frames.size() == 1, got);

        Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();
        List<Throwable> thrown = Collections.synchronizedList(new ArrayList<>());
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> thrown.add(e));
        M3Synchronizer.asyncExec(M3Synchronizer.MODEL, null, () -> {
            throw new IllegalStateException("model runnable fails");
        });
        M3Synchronizer.asyncExec(M3Synchronizer.MODEL, null, rec("after"));
        Thread.sleep(200);
        got = take();
        check("a throwing MODEL runnable does not lose the next one", got.contains("after") && thrown.size() == 1,
                got + " " + thrown);
        Thread.setDefaultUncaughtExceptionHandler(old);
        M3Synchronizer.removeFrameListener(fl);

        g = blockEdt();
        List<Integer> expected = new ArrayList<>();
        List<Integer> order = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 10_000; i++) {
            int n = i;
            if (i % 3 == 0) {
                M3Synchronizer.asyncExec("key" + (i % 7), () -> { });
            }
            if (i % 5 == 0) {
                M3Synchronizer.asyncExec(M3Synchronizer.MODEL, "m" + (i % 11), () -> { });
            }
            EventQueue.invokeLater(() -> order.add(n));
            expected.add(n);
        }
        g.countDown();
        drain();
        check("10^4 ordinary events keep their posting order with keyed runnables interleaved",
                order.equals(expected), order.size() + " run");

        List<LogRecord> logs = Collections.synchronizedList(new ArrayList<>());
        Logger log = Logger.getLogger("javax.swing.M3Synchronizer");
        Handler h = new Handler() {
            @Override
            public void publish(LogRecord r) {
                logs.add(r);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        log.addHandler(h);
        g = blockEdt();
        CountDownLatch release = g;
        new Thread(() -> {
            try {
                Thread.sleep(700);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            release.countDown();
        }).start();
        long w0 = System.nanoTime();
        SwingUtilities.invokeAndWait(rec("waited"));
        long waited = (System.nanoTime() - w0) / 1_000_000L;
        log.removeHandler(h);
        got = take();
        LogRecord r = logs.isEmpty() ? null : logs.get(0);
        check("a long invokeAndWait wait is logged once at WARNING, then completes", logs.size() == 1
                && r.getLevel() == Level.WARNING && r.getThrown() != null && r.getMessage().contains("AWT-EventQueue")
                && got.equals(List.of("waited")) && waited >= 600, logs.size() + " records, waited " + waited + " ms");

        System.out.println("SUMMARY\tM3SynchronizerTest\t" + passes + " passed, " + failures + " failed");
        System.exit(failures == 0 ? 0 : 1);
    }
}
