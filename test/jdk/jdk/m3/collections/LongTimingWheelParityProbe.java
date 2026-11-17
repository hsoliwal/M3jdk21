/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.LongConsumer;

/**
 * Differential oracle for LongTimingWheel's bucket-minimum and group-occupancy lanes: the
 * master copy LongTimingWheelReference and the candidate run the same operations in lockstep;
 * results, callback order, exceptions and the original lanes must match, and the new index must
 * equal a full recomputation after every operation.
 */
//FLLDCIM: LEETCODIFY — lockstep proof for nextScheduledTick (offset lanes + bit masks)
public final class LongTimingWheelParityProbe {
    private LongTimingWheelParityProbe() { }

    static long checks;
    static long indexedQueries;
    static long reentrantIse;
    static long taints;
    static final String[] LANES = {"bucketHead", "values", "deadlineTick", "next", "highWater",
        "freeHead", "size", "currentTick", "finalTickConsumed", "mask", "tickNanos"};

    interface Wheel {
        void schedule(long v, long d);
        int advanceTo(long now, LongConsumer c);
        void clear();
        long currentTick();
        int size();
        long payloadBytes();
        Object raw();
        void grow(int failAt, boolean malformed);
    }

    static Wheel cand(LongTimingWheel w) {
        return new Wheel() {
            public void schedule(long v, long d) { w.schedule(v, d); }
            public int advanceTo(long n, LongConsumer c) { return w.advanceTo(n, c); }
            public void clear() { w.clear(); }
            public long currentTick() { return w.currentTick(); }
            public int size() { return w.size(); }
            public long payloadBytes() { return w.payloadBytes() + w.wheelSize() + w.tickNanos(); }
            public Object raw() { return w; }
            public void grow(int failAt, boolean malformed) {
                int[] at = {0};
                w.grow(new LongTimingWheel.ArrayCopier() {
                    public long[] copy(long[] s, int c) { return Copy.longs(s, c, ++at[0], failAt,
                            malformed); }
                    public int[] copy(int[] s, int c) { return Copy.ints(s, c, ++at[0], failAt,
                            malformed); }
                });
            }
        };
    }

    static Wheel ref(LongTimingWheelReference w) {
        return new Wheel() {
            public void schedule(long v, long d) { w.schedule(v, d); }
            public int advanceTo(long n, LongConsumer c) { return w.advanceTo(n, c); }
            public void clear() { w.clear(); }
            public long currentTick() { return w.currentTick(); }
            public int size() { return w.size(); }
            public long payloadBytes() { return w.payloadBytes() + w.wheelSize() + w.tickNanos(); }
            public Object raw() { return w; }
            public void grow(int failAt, boolean malformed) {
                int[] at = {0};
                w.grow(new LongTimingWheelReference.ArrayCopier() {
                    public long[] copy(long[] s, int c) { return Copy.longs(s, c, ++at[0], failAt,
                            malformed); }
                    public int[] copy(int[] s, int c) { return Copy.ints(s, c, ++at[0], failAt,
                            malformed); }
                });
            }
        };
    }

    static final class Copy {
        static long[] longs(long[] s, int c, int at, int failAt, boolean malformed) {
            if (at == failAt && !malformed) throw new OutOfMemoryError("synthetic " + at);
            if (at == failAt) return null;
            return Arrays.copyOf(s, c);
        }
        static int[] ints(int[] s, int c, int at, int failAt, boolean malformed) {
            if (at == failAt && !malformed) throw new OutOfMemoryError("synthetic " + at);
            if (at == failAt) return Arrays.copyOf(s, c - 1);
            return Arrays.copyOf(s, c);
        }
    }

    static long mix(long a, long b) {
        long z = a * 0x9E3779B97F4A7C15L + b + 0x632BE59BD9B4E5E9L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> void sneaky(Throwable t) throws T { throw (T) t; }

    /** Deterministic callback script shared by both wheels. */
    static final class Script implements LongConsumer {
        final Wheel wheel; final long seed; final List<String> trace; final int depth;
        final int w; final long tick; final boolean clearAllowed;
        int calls;
        Script(Wheel wheel, long seed, List<String> trace, int depth, int w, long tick, boolean clr)
                {
            this.wheel = wheel; this.seed = seed; this.trace = trace; this.depth = depth;
            this.w = w; this.tick = tick; this.clearAllowed = clr;
        }
        public void accept(long v) {
            int call = calls++;
            trace.add("cb" + depth + " v=" + v + " t=" + wheel.currentTick() + " s=" + wheel.size())
                    ;
            if (call > 4000) throw new RuntimeException("cap" + depth);
            long r = mix(seed, call);
            int pick = (int) Long.remainderUnsigned(r, 100);
            long now = wheel.currentTick() * tick;
            long r2 = mix(r, 7);
            if (pick < 55) return;
            if (pick < 68) {
                long[] deltas = {-3 * tick, 0, tick, (long) w * tick, 3L * w * tick + tick,
                    Long.remainderUnsigned(r2, 64L * w) * tick};
                long d = now + deltas[(int) Long.remainderUnsigned(r2 >>> 8, deltas.length)];
                trace.add("rs " + outcome(() -> { wheel.schedule(9_000_000L + depth * 100_000L +
                        call, d); return 0; }));
                return;
            }
            if (pick < 74 && depth < 2) {
                long target = now + Long.remainderUnsigned(r2, 2L * w) * tick;
                trace.add("ra " + outcome(() -> wheel.advanceTo(target, sub(r2))));
                return;
            }
            if (pick < 79 && depth < 2) {
                long target = now + (2L * w + Long.remainderUnsigned(r2, 9L * w)) * tick;
                trace.add("rA " + outcome(() -> wheel.advanceTo(target, sub(r2))));
                return;
            }
            if (pick < 82) throw new RuntimeException("rt" + depth + ":" + call);
            if (pick < 84) throw new AssertionError("err" + depth + ":" + call);
            if (pick < 86) LongTimingWheelParityProbe.<RuntimeException>sneaky(new Exception("chk" +
                    depth + ":" + call));
            if (pick < 88 && depth < 2) {
                wheel.advanceTo(now + (2L * w + 3) * tick, sub(r2));
                return;
            }
            if (pick < 89 && clearAllowed) { trace.add("rc"); wheel.clear(); }
        }
        Script sub(long s) { return new Script(wheel, s, trace, depth + 1, w, tick, clearAllowed); }
    }

    static String outcome(Callable<Integer> op) {
        try {
            return "R" + op.call();
        } catch (Throwable t) {
            return "T" + t.getClass().getName() + ":" + t.getMessage();
        }
    }

    static Object get(Object o, String name) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(o);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static String invoke(Object o, String name) {
        try {
            Method m = o.getClass().getDeclaredMethod(name);
            m.setAccessible(true);
            return "R" + m.invoke(o);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            return "T" + t.getClass().getName() + ":" + t.getMessage();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }

    static void sameLanes(Wheel a, Wheel b, String where) {
        for (String lane : LANES) {
            Object x = get(a.raw(), lane), y = get(b.raw(), lane);
            boolean eq = x instanceof long[] ? Arrays.equals((long[]) x, (long[]) y)
                    : x instanceof int[] ? Arrays.equals((int[]) x, (int[]) y) : x.equals(y);
            check(eq, where + " lane " + lane);
        }
        check(a.payloadBytes() == b.payloadBytes(), where + " payloadBytes");
    }

    /** White-box: lanes exact while indexed; fast query equals the verbatim scan. */
    static void indexExact(LongTimingWheel w, String where) {
        boolean indexed = (boolean) get(w, "indexed");
        check((int) get(w, "walkDepth") == 0, where + " walkDepth");
        if ((boolean) get(w, "tainted")) taints++;
        if (!indexed) return;
        check(!(boolean) get(w, "tainted"), where + " indexed+tainted");
        int[] head = (int[]) get(w, "bucketHead");
        int[] next = (int[]) get(w, "next");
        long[] dl = (long[]) get(w, "deadlineTick");
        long[] bmin = (long[]) get(w, "bucketMin");
        long[] lane = (long[]) get(w, "groupLane");
        long[] bits = (long[]) get(w, "groupBits");
        long[] gmin = new long[lane.length / 2];
        Arrays.fill(gmin, Long.MAX_VALUE);
        boolean[] gLive = new boolean[gmin.length];
        for (int b = 0; b < head.length; b++) {
            long m = Long.MAX_VALUE;
            int n = 0;
            for (int s = head[b]; s >= 0; s = next[s]) { m = Math.min(m, dl[s]); check(++n <= w.size
                    (), where + " cycle"); }
            check(bmin[b] == m, where + " bucketMin " + b);
            check(((lane[2 * (b >>> 6) + 1] >>> (b & 63)) & 1L) == (head[b] >= 0 ? 1L : 0L), where +
                    " occ " + b);
            gmin[b >>> 6] = Math.min(gmin[b >>> 6], m);
            gLive[b >>> 6] |= head[b] >= 0;
        }
        for (int g = 0; g < gmin.length; g++) {
            boolean dirty = ((bits[2 * (g >>> 6) + 1] >>> (g & 63)) & 1L) != 0;
            boolean live = ((bits[2 * (g >>> 6)] >>> (g & 63)) & 1L) != 0;
            if (!dirty) check(lane[2 * g] == gmin[g], where + " groupMin " + g);
            else check(lane[2 * g] <= gmin[g], where + " groupMin lower bound " + g);
            if (gLive[g]) check(live, where + " live " + g);
        }
        if (w.size() > 0) {
            String scan = invoke(w, "scanScheduledTick");
            String fast = invoke(w, "nextScheduledTick");
            check(scan.equals(fast), where + " fast " + fast + " vs scan " + scan);
            indexedQueries++;
        }
    }

    static String timed(Callable<String> op, boolean threaded) {
        if (!threaded) {
            try { return op.call(); } catch (Exception e) { throw new AssertionError(e); }
        }
        String[] box = {"TIMEOUT"};
        Thread t = new Thread(() -> { try { box[0] = op.call(); } catch (Exception e) { box[0] = "X"
                + e; } });
        t.setDaemon(true);
        t.start();
        try { t.join(1500); } catch (InterruptedException e) { throw new AssertionError(e); }
        synchronized (box) { return t.isAlive() ? "TIMEOUT" : box[0]; }
    }

    static boolean run(int w, long tick, long start, long seed, int steps, boolean clearAllowed) {
        LongTimingWheelReference rw = new LongTimingWheelReference(w, tick, start);
        LongTimingWheel cw = new LongTimingWheel(w, tick, start);
        Wheel a = ref(rw), b = cand(cw);
        Random rnd = new Random(seed);
        long id = 1;
        for (int step = 0; step < steps; step++) {
            String where = "w=" + w + " tick=" + tick + " start=" + start + " seed=" + seed +
                    " step=" + step;
            int op = rnd.nextInt(100);
            long now = a.currentTick() * tick;
            long r = rnd.nextLong();
            List<String> ta = new ArrayList<>(), tb = new ArrayList<>();
            Callable<String> fa, fb;
            if (op < 40) {
                long d;
                switch (rnd.nextInt(6)) {
                    case 0: d = now + rnd.nextInt(3 * w + 1) * tick; break;
                    case 1: d = now + ((1L + rnd.nextInt(40)) * w + rnd.nextInt(w)) * tick; break;
                    case 2: d = now - rnd.nextInt(6) * tick; break;
                    case 3: d = now + rnd.nextInt(1 << 20) * tick; break;
                    case 4: d = rnd.nextBoolean() ? Long.MAX_VALUE : Long.MIN_VALUE; break;
                    default: d = now + (1L << 40); break;
                }
                long v = id++;
                fa = () -> outcome(() -> { a.schedule(v, d); return 0; });
                fb = () -> outcome(() -> { b.schedule(v, d); return 0; });
            } else if (op < 76 || op >= 80) {
                long delta;
                switch (rnd.nextInt(9)) {
                    case 0: delta = 0; break;
                    case 1: delta = 1; break;
                    case 2: delta = rnd.nextInt(2 * w); break;
                    case 3: delta = 2L * w; break;
                    case 4: delta = 2L * w + 1; break;
                    case 5: delta = 2L * w + rnd.nextInt(50 * w); break;
                    case 6: delta = 1L << 33; break;
                    case 7: delta = -rnd.nextInt(5); break;
                    default: delta = rnd.nextInt(1 << 21); break;
                }
                long target = now + delta * tick;
                fa = () -> outcome(() -> a.advanceTo(target, new Script(a, r, ta, 0, w, tick,
                        clearAllowed)));
                fb = () -> outcome(() -> b.advanceTo(target, new Script(b, r, tb, 0, w, tick,
                        clearAllowed)));
            } else if (op < 78) {
                fa = () -> outcome(() -> { a.clear(); return 0; });
                fb = () -> outcome(() -> { b.clear(); return 0; });
            } else {
                int failAt = 1 + rnd.nextInt(4);
                boolean malformed = rnd.nextBoolean();
                fa = () -> outcome(() -> { a.grow(failAt, malformed); return 0; });
                fb = () -> outcome(() -> { b.grow(failAt, malformed); return 0; });
            }
            String oa = timed(fa, clearAllowed), ob = timed(fb, clearAllowed);
            if (oa.equals("TIMEOUT") || ob.equals("TIMEOUT")) {
                check(oa.equals(ob), where + " timeout parity " + oa + " vs " + ob);
                return false;
            }
            check(oa.equals(ob), where + " outcome " + oa + " vs " + ob);
            if (oa.contains("IllegalStateException:invalid timing wheel occupancy")) reentrantIse++;
            check(ta.equals(tb), where + " trace\n" + ta + "\n" + tb);
            for (String s : ta) if (s.contains("occupancy")) reentrantIse++;
            sameLanes(a, b, where);
            if (!clearAllowed) indexExact(cw, where);
        }
        return true;
    }

    /** Re-entrant clear() on an indexed wheel, then slot reuse: the scan must still own it. */
    static void reentrantClearScenario() {
        for (int variant = 0; variant < 2; variant++) {
            LongTimingWheelReference rw = new LongTimingWheelReference(4, 1L, 0L);
            LongTimingWheel cw = new LongTimingWheel(4, 1L, 0L);
            Wheel[] both = {ref(rw), cand(cw)};
            String[] out = new String[2];
            for (int i = 0; i < 2; i++) {
                Wheel x = both[i];
                StringBuilder sb = new StringBuilder();
                x.schedule(9, 100);
                sb.append(outcome(() -> x.advanceTo(10, v -> { }))).append(' ');
                x.schedule(5, 11);
                sb.append(outcome(() -> x.advanceTo(11, v -> x.clear()))).append(' ');
                if (i == 1) check((boolean) get(cw, "tainted"), "re-entrant clear did not taint");
                x.schedule(7, 500);
                x.schedule(8, variant == 0 ? 600 : 501);
                List<Long> seen = new ArrayList<>();
                sb.append(outcome(() -> x.advanceTo(1000, seen::add))).append(' ').append(seen);
                sb.append(' ').append(x.currentTick()).append(' ').append(x.size());
                x.clear();
                if (i == 1) check(!(boolean) get(cw, "tainted"), "clean clear did not untaint");
                x.schedule(1, 2000);
                sb.append(' ').append(outcome(() -> x.advanceTo(5000, seen::add))).append(seen);
                out[i] = sb.toString();
            }
            check(out[0].equals(out[1]), "re-entrant clear scenario\n" + out[0] + "\n" + out[1]);
            System.out.println("reentrant-clear v" + variant + ": " + out[0]);
        }
    }

    static void perf() {
        int w = 1024, n = 20000;
        long[] times = new long[2];
        int[] counts = new int[2];
        for (int k = 0; k < 2; k++) {
            Wheel x = k == 0 ? ref(new LongTimingWheelReference(w, 1L, 0L)) : cand(new
                    LongTimingWheel(w, 1L, 0L));
            for (int i = 0; i < n; i++) x.schedule(i, (long) (n - i) * (3L * w + 1));
            long t0 = System.nanoTime();
            counts[k] = x.advanceTo(Long.MAX_VALUE - 1, v -> { });
            times[k] = System.nanoTime() - t0;
        }
        check(counts[0] == counts[1] && counts[0] == n, "perf counts");
        System.out.printf("perf n=%d W=%d reference=%.1f ms candidate=%.1f ms%n", n, w, times[0] /
                1e6, times[1] / 1e6);
    }

    public static void main(String[] args) {
        int steps = args.length > 0 ? Integer.parseInt(args[0]) : 1500;
        int[] widths = {1, 2, 4, 8, 64, 128, 8192};
        long[] ticks = {1L, 5L};
        long[] starts = {0L, -12345L, Long.MAX_VALUE - 40_000L, Long.MIN_VALUE + 100L};
        for (int w : widths)
            for (long tick : ticks)
                for (long start : starts)
                    for (long seed = 1; seed <= 3; seed++)
                        run(w, tick, start, seed * 0x5DEECE66DL + w, w == 8192 ? steps / 3 : steps,
                                false);
        System.out.println("lockstep PASS checks=" + checks + " indexedQueries=" + indexedQueries
                + " reentrantIse=" + reentrantIse + " taintedObservations=" + taints);
        reentrantClearScenario();
        int timeouts = 0, completed = 0;
        for (long seed = 1; seed <= 40 && timeouts < 6; seed++) {
            if (run(seed % 2 == 0 ? 4 : 64, 1L, 0L, seed, 300, true)) completed++;
            else timeouts++;
        }
        System.out.println("clear-chaos PASS completed=" + completed + " pairedTimeouts=" + timeouts
                );
        if (args.length < 2) perf();
        System.out.println("PASS parity checks=" + checks);
    }
}
