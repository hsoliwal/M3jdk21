/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.swing;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.FrameListener;

import sun.awt.PeerEvent;
import sun.awt.SunToolkit;

/**
 * The model-thread protocol of the event dispatch thread (M3JDK, brief rule 28; the Swing side of SWT's
 * {@code Synchronizer}, with the same priorities, coalescing rule and frame protocol).
 * <ul>
 * <li>Model threads produce patches and hand them over with {@link #asyncExec(int, Object, Runnable)} at
 * {@link #MODEL}. Model runnables run only at a frame tick, all together, in submission order. Then
 * {@link FrameListener#frameApplied} runs and {@link #getFrameGeneration()} goes up by one. Ticks are paced by the
 * property {@code swing.synchronizer.frameMillis} (default 16). At most one tick is pending, and there is none when
 * nothing is queued and no frame was {@linkplain #requestFrame() requested}.</li>
 * <li>A runnable with a key replaces a queued runnable with an equal key ({@code equals}/{@code hashCode}) of the same
 * priority in place, keeping its position: the newest wins, as with {@code EventQueue.coalesceEvents}. Once it has
 * run, the key is free again. The usual key is the model or the view.</li>
 * <li>{@link #INPUT} and {@link #LAYOUT} run ahead of ordinary events (INPUT first), {@link #DEFAULT} is the stock FIFO
 * of {@code EventQueue.invokeLater}, and {@link #IDLE} runs when the event queue is empty.</li>
 * </ul>
 * The stock order of ordinary events is unchanged: a runnable without a key at {@code DEFAULT} is posted exactly as
 * {@code EventQueue.invokeLater} posts it. The event dispatch thread never waits on a model thread here, and an
 * {@code invokeAndWait} that waits longer than {@code swing.synchronizer.syncWarnMillis} (default 5000 ms) is logged
 * once at WARNING on the logger {@code javax.swing.M3Synchronizer}; the wait then goes on as in the stock JDK.
 *
 * @since 21 (M3JDK)
 */
//FLLDCIM: M3 synchronizer - keyed coalescing per priority, model patches applied once per frame
public final class M3Synchronizer {

    /** Runs before ordinary events, ahead of {@link #LAYOUT}. */
    public static final int INPUT = 0;

    /** Layout and paint work; runs before ordinary events, after {@link #INPUT}. */
    public static final int LAYOUT = 1;

    /** The stock FIFO of {@code EventQueue.invokeLater}. */
    public static final int DEFAULT = 2;

    /** Model patches: run only at a frame tick, all together, then the frame listeners run. */
    public static final int MODEL = 3;

    /** Runs when the event queue is empty. */
    public static final int IDLE = 4;

    private static final Object LOCK = new Object();
    @SuppressWarnings("removal")
    private static final int FRAME_MILLIS = Math.max(0, java.security.AccessController.doPrivileged(
            new sun.security.action.GetIntegerAction("swing.synchronizer.frameMillis", 16)));

    /** One queued runnable; a newer runnable with the same key replaces {@code task} in place. */
    private static final class Entry implements Runnable {
        final Object key;
        final int priority;
        Runnable task;

        Entry(Object key, int priority, Runnable task) {
            this.key = key;
            this.priority = priority;
            this.task = task;
        }

        /** A keyed DEFAULT entry is its own InvocationEvent runnable: it frees its key, then runs the newest task. */
        @Override
        public void run() {
            Runnable r;
            synchronized (LOCK) {
                KEYED[priority].remove(key);
                r = task;
            }
            r.run();
        }
    }

    @SuppressWarnings("unchecked")
    private static final Map<Object, Entry>[] KEYED = new Map[] {new HashMap<>(), new HashMap<>(), new HashMap<>(),
            new HashMap<>(), new HashMap<>()};
    @SuppressWarnings("unchecked")
    private static final List<Entry>[] QUEUED = new List[] {new ArrayList<>(), new ArrayList<>(), null,
            new ArrayList<>(), new ArrayList<>()};
    private static final List<FrameListener> FRAME_LISTENERS = new CopyOnWriteArrayList<>();
    private static boolean urgentPosted;
    private static boolean idlePosted;
    private static boolean tickPending;
    private static boolean frameRequested;
    private static long lastFrameNanos;
    private static Timer tick;
    private static volatile long generation;

    private M3Synchronizer() {
    }

    /**
     * Runs {@code r} on the event dispatch thread at {@link #DEFAULT} priority, coalesced by {@code key}.
     *
     * @param key the coalescing key; null for none (then this is {@code EventQueue.invokeLater})
     * @param r the runnable
     * @throws NullPointerException if {@code r} is null
     */
    public static void asyncExec(Object key, Runnable r) {
        asyncExec(DEFAULT, key, r);
    }

    /**
     * Runs {@code r} on the event dispatch thread at {@code priority}, coalesced by {@code key}: while a runnable
     * with an equal key and the same priority is queued, {@code r} replaces it in place.
     *
     * @param priority {@link #INPUT}, {@link #LAYOUT}, {@link #DEFAULT}, {@link #MODEL} or {@link #IDLE}
     * @param key the coalescing key; null for none
     * @param r the runnable
     * @throws IllegalArgumentException if {@code priority} is not one of the five
     * @throws NullPointerException if {@code r} is null
     */
    public static void asyncExec(int priority, Object key, Runnable r) {
        java.util.Objects.requireNonNull(r);
        if (priority < INPUT || priority > IDLE) {
            throw new IllegalArgumentException("priority " + priority);
        }
        if (priority == DEFAULT && key == null) {
            EventQueue.invokeLater(r);
            return;
        }
        Entry posted = null;
        boolean urgent = false;
        boolean idle = false;
        boolean frame = false;
        synchronized (LOCK) {
            Entry e = key == null ? null : KEYED[priority].get(key);
            if (e != null) {
                e.task = r;
                return;
            }
            e = new Entry(key, priority, r);
            if (key != null) {
                KEYED[priority].put(key, e);
            }
            switch (priority) {
                case DEFAULT -> posted = e;
                case INPUT, LAYOUT -> {
                    QUEUED[priority].add(e);
                    urgent = !urgentPosted;
                    urgentPosted = true;
                }
                case MODEL -> {
                    QUEUED[priority].add(e);
                    frame = true;
                }
                default -> {
                    QUEUED[priority].add(e);
                    idle = !idlePosted;
                    idlePosted = true;
                }
            }
        }
        if (posted != null) {
            EventQueue.invokeLater(posted);
        }
        if (urgent) {
            SunToolkit.postPriorityEvent(new PeerEvent(Toolkit.getDefaultToolkit(), M3Synchronizer::drainUrgent,
                    PeerEvent.PRIORITY_EVENT));
        }
        if (idle) {
            EventQueue.invokeLater(M3Synchronizer::drainIdle);
        }
        if (frame) {
            scheduleTick();
        }
    }

    /**
     * The number of frames applied so far: 0 before the first frame, then one more per frame tick.
     *
     * @return the frame generation
     */
    public static long getFrameGeneration() {
        return generation;
    }

    /**
     * Asks for a frame tick even with no model runnable queued; the frame listeners then run once.
     */
    public static void requestFrame() {
        synchronized (LOCK) {
            frameRequested = true;
        }
        scheduleTick();
    }

    /**
     * Adds a listener told after each frame tick, on the event dispatch thread.
     *
     * @param l the listener; null is ignored
     */
    public static void addFrameListener(FrameListener l) {
        if (l != null) {
            FRAME_LISTENERS.add(l);
        }
    }

    /**
     * Removes a listener added with {@link #addFrameListener}.
     *
     * @param l the listener
     */
    public static void removeFrameListener(FrameListener l) {
        FRAME_LISTENERS.remove(l);
    }

    private static void scheduleTick() {
        Timer t;
        int delay;
        synchronized (LOCK) {
            if (tickPending) {
                return;
            }
            tickPending = true;
            long since = (System.nanoTime() - lastFrameNanos) / 1_000_000L;
            delay = lastFrameNanos == 0 ? 0 : (int) Math.max(0, FRAME_MILLIS - since);
            if (tick == null) {
                tick = new Timer(FRAME_MILLIS, e -> frame());
                tick.setRepeats(false);
                tick.setCoalesce(true);
            }
            t = tick;
        }
        t.setInitialDelay(delay);
        t.restart();
    }

    /** The frame tick: every queued model runnable, in order, then the frame listeners. */
    private static void frame() {
        List<Entry> due;
        synchronized (LOCK) {
            tickPending = false;
            lastFrameNanos = System.nanoTime();
            due = take(MODEL);
            frameRequested = false;
        }
        runAll(due, MODEL);
        long g = ++generation;
        for (FrameListener l : FRAME_LISTENERS) {
            l.frameApplied(g);
        }
    }

    private static void drainUrgent() {
        List<Entry> input;
        List<Entry> layout;
        synchronized (LOCK) {
            urgentPosted = false;
            input = take(INPUT);
            layout = take(LAYOUT);
        }
        runAll(input, INPUT);
        runAll(layout, LAYOUT);
    }

    private static void drainIdle() {
        if (Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent() != null) {
            EventQueue.invokeLater(M3Synchronizer::drainIdle);
            return;
        }
        List<Entry> idle;
        synchronized (LOCK) {
            idlePosted = false;
            idle = take(IDLE);
        }
        runAll(idle, IDLE);
    }

    /** Takes the queued entries of a priority and frees their keys; callers hold LOCK. */
    private static List<Entry> take(int priority) {
        List<Entry> q = QUEUED[priority];
        if (q.isEmpty()) {
            return List.of();
        }
        List<Entry> due = new ArrayList<>(q);
        q.clear();
        KEYED[priority].clear();
        return due;
    }

    /** Runs the entries in order; when one throws, the rest go back to the front of the queue first. */
    private static void runAll(List<Entry> due, int priority) {
        for (int i = 0; i < due.size(); i++) {
            try {
                due.get(i).task.run();
            } catch (RuntimeException | Error t) {
                requeue(due.subList(i + 1, due.size()), priority);
                throw t;
            }
        }
    }

    private static void requeue(List<Entry> rest, int priority) {
        if (rest.isEmpty()) {
            return;
        }
        synchronized (LOCK) {
            QUEUED[priority].addAll(0, rest);
            for (Entry e : rest) {
                if (e.key != null) {
                    KEYED[priority].putIfAbsent(e.key, e);
                }
            }
        }
        switch (priority) {
            case MODEL -> scheduleTick();
            case IDLE -> EventQueue.invokeLater(M3Synchronizer::drainIdle);
            default -> SunToolkit.postPriorityEvent(new PeerEvent(Toolkit.getDefaultToolkit(),
                    M3Synchronizer::drainUrgent, PeerEvent.PRIORITY_EVENT));
        }
    }
}
