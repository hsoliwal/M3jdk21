package jdk.m3.collections.packed;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.function.LongConsumer;

/**
 * RunLengthLongList with a prefix-end lane against master's class (RefRunLengthLongList): 600 seeds
 * x 400 random operations, results, exceptions, callback logs and private lanes compared.
 */
//FLLDCIM: LEETCODIFY — differential proof for the run-end offset lane
final class RunLengthPrefixTest {
    interface Pair { void accept(long v, int c); }

    interface Api {
        void add(long v);
        void addRepeated(long v, int c);
        long get(long i);
        String stats();
        void forEachRun(Pair p);
        void forEach(LongConsumer c);
        void clear();
    }

    static final long[] ALPHA = {0L, 1L, -1L, 7L, Long.MIN_VALUE, Long.MAX_VALUE};
    static final int[] BIG = {Integer.MAX_VALUE, Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 2,
            1 << 30, (1 << 30) + 1};

    static Api ref(RefRunLengthLongList l) {
        return new Api() {
            public void add(long v) { l.add(v); }
            public void addRepeated(long v, int c) { l.addRepeated(v, c); }
            public long get(long i) { return l.get(i); }
            public String stats() {
                return l.size() + "/" + l.runCount() + "/" + l.isEmpty() + "/" + l.payloadBytes();
            }
            public void forEachRun(Pair p) { l.forEachRun(p::accept); }
            public void forEach(LongConsumer c) { l.forEach(c); }
            public void clear() { l.clear(); }
        };
    }

    static Api cut(RunLengthLongList l) {
        return new Api() {
            public void add(long v) { l.add(v); }
            public void addRepeated(long v, int c) { l.addRepeated(v, c); }
            public long get(long i) { return l.get(i); }
            public String stats() {
                return l.size() + "/" + l.runCount() + "/" + l.isEmpty() + "/" + l.payloadBytes();
            }
            public void forEachRun(Pair p) { l.forEachRun(p::accept); }
            public void forEach(LongConsumer c) { l.forEach(c); }
            public void clear() { l.clear(); }
        };
    }

    static String call(Callable<Object> c) {
        try {
            return "R:" + c.call();
        } catch (Throwable t) {
            return "E:" + t.getClass().getName() + ":" + t.getMessage();
        }
    }

    static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static void setSize(Object o, long v) throws Exception {
        Field f = o.getClass().getDeclaredField("size");
        f.setAccessible(true);
        f.setLong(o, v);
    }

    static long op = 0;
    static int seed;

    static void fail(String what, Object a, Object b) {
        throw new IllegalStateException("MISMATCH seed=" + seed + " op=" + op + " " + what
                + "\n ref=" + a + "\n cut=" + b);
    }

    static void lanes(RefRunLengthLongList r, RunLengthLongList c) throws Exception {
        long[] rv = (long[]) field(r, "runValues");
        int[] rc = (int[]) field(r, "runCounts");
        long[] cv = (long[]) field(c, "runValues");
        long[] ce = (long[]) field(c, "runEnds");
        int rn = (int) field(r, "runCount");
        int cn = (int) field(c, "runCount");
        if (rn != cn) fail("runCount", rn, cn);
        if ((long) field(r, "size") != (long) field(c, "size")) fail("size", 0, 0);
        if (!java.util.Arrays.equals(rv, cv)) fail("runValues", rv.length, cv.length);
        if (rc.length != ce.length) fail("lane length", rc.length, ce.length);
        long sum = 0;
        for (int i = 0; i < rc.length; i++) {
            sum += rc[i];
            long want = i < rn ? sum : 0L;
            if (i >= rn && rc[i] != 0) fail("ref stale count", i, rc[i]);
            if (ce[i] != want) fail("runEnds[" + i + "]", want, ce[i]);
        }
    }

    static long laneTotal(RefRunLengthLongList r) throws Exception {
        int rn = (int) field(r, "runCount");
        int[] rc = (int[]) field(r, "runCounts");
        long sum = 0;
        for (int i = 0; i < rn; i++) sum += rc[i];
        return sum;
    }

    static long pickValue(SplittableRandom g) {
        return g.nextInt(4) == 0 ? g.nextLong() : ALPHA[g.nextInt(ALPHA.length)];
    }

    static int pickCount(SplittableRandom g) {
        int k = g.nextInt(10);
        if (k == 0) return -g.nextInt(1, 4);
        if (k == 1) return 0;
        if (k == 2) return BIG[g.nextInt(BIG.length)];
        return g.nextInt(1, k < 6 ? 4 : 60);
    }

    static long pickIndex(SplittableRandom g, RefRunLengthLongList r) throws Exception {
        long size = r.size();
        int k = g.nextInt(9);
        if (k == 0) {
            return new long[]{-1L, Long.MIN_VALUE, Long.MAX_VALUE, size, size + 1}[g.nextInt(5)];
        }
        if (k == 1 && size > 0) return size - 1;
        int rn = (int) field(r, "runCount");
        int[] rc = (int[]) field(r, "runCounts");
        if (k < 6 && rn > 0) {
            int run = g.nextInt(rn);
            long end = 0;
            for (int i = 0; i <= run; i++) end += rc[i];
            return end + g.nextInt(-1, 2);
        }
        return size > 0 ? g.nextLong(size) : 0L;
    }

    /** Re-entrant mutation plan shared by both instances. */
    static final class Plan {
        final int every, kind, c; final long v; int budget = 3;
        Plan(SplittableRandom g) {
            every = g.nextInt(1, 5); kind = g.nextInt(5); v = pickValue(g); c = g.nextInt(1, 40);
        }
        Plan copy() { return new Plan(every, kind, v, c); }
        Plan(int e, int k, long v, int c) { every = e; kind = k; this.v = v; this.c = c; }
        void fire(Api a, int calls) {
            if (kind == 4 || budget == 0 || calls % every != 0) return;
            budget--;
            if (kind == 0) a.add(v);
            else if (kind == 1) a.addRepeated(v, c);
            else if (kind == 2) a.clear();
            else { a.clear(); a.addRepeated(v, c); }
        }
    }

    static String runs(Api a, Plan p) {
        List<String> log = new ArrayList<>();
        int[] calls = {0};
        String out = call(() -> {
            a.forEachRun((v, c) -> { log.add(v + "x" + c); p.fire(a, ++calls[0]); });
            return null;
        });
        return log + out + a.stats();
    }

    static String each(Api a, Plan p) {
        List<String> log = new ArrayList<>();
        int[] calls = {0};
        String out = call(() -> {
            a.forEach(v -> { log.add(Long.toString(v)); p.fire(a, ++calls[0]); });
            return null;
        });
        return log + out + a.stats();
    }

    static void step(SplittableRandom g, RefRunLengthLongList rl, RunLengthLongList cl,
                     Api r, Api c, boolean tamper) throws Exception {
        int k = g.nextInt(tamper ? 9 : 8);
        String a, b;
        switch (k) {
            case 0 -> { long v = pickValue(g); a = call(() -> { r.add(v); return r.stats(); });
                        b = call(() -> { c.add(v); return c.stats(); }); }
            case 1 -> { long v = pickValue(g); int n = pickCount(g);
                        a = call(() -> { r.addRepeated(v, n); return r.stats(); });
                        b = call(() -> { c.addRepeated(v, n); return c.stats(); }); }
            case 2, 3 -> {
                long i = pickIndex(g, rl);
                a = call(() -> r.get(i));
                b = call(() -> c.get(i));
            }
            case 4 -> { Plan p = new Plan(g); a = runs(r, p.copy()); b = runs(c, p.copy()); }
            case 5 -> { if (rl.size() > 4000 || laneTotal(rl) > 4000) return;
                        Plan p = new Plan(g); a = each(r, p.copy()); b = each(c, p.copy()); }
            case 6 -> {
                if (g.nextInt(4) != 0) return;
                a = call(() -> { r.clear(); return r.stats(); });
                b = call(() -> { c.clear(); return c.stats(); });
            }
            case 7 -> { a = r.stats(); b = c.stats(); }
            default -> { long s = g.nextInt(3) == 0 ? Long.MAX_VALUE - g.nextInt(3)
                                 : Math.max(0L, rl.size() + g.nextInt(-2, 3));
                         setSize(rl, s); setSize(cl, s); a = r.stats(); b = c.stats(); }
        }
        if (!a.equals(b)) fail("op" + k, a, b);
    }

    static void runSeed(int s, int ops) throws Exception {
        seed = s;
        SplittableRandom g = new SplittableRandom(0x5EED0000L + s);
        int init = new int[]{-1, 0, 1, 2, 8, g.nextInt(0, 40)}[g.nextInt(6)];
        String a = call(() -> new RefRunLengthLongList(init).payloadBytes());
        String b = call(() -> new RunLengthLongList(init).payloadBytes());
        if (!a.equals(b)) fail("ctor", a, b);
        boolean dflt = init < 0 || g.nextInt(5) == 0;
        RefRunLengthLongList rl = dflt
                ? new RefRunLengthLongList() : new RefRunLengthLongList(init);
        RunLengthLongList cl = dflt ? new RunLengthLongList() : new RunLengthLongList(init);
        Api r = ref(rl), c = cut(cl);
        boolean tamper = s % 4 == 3;
        for (op = 0; op < ops; op++) {
            step(g, rl, cl, r, c, tamper);
            lanes(rl, cl);
        }
    }

    @Test
    void sameAsMaster() throws Exception {
        for (int s = 0; s < 600; s++) runSeed(s, 400);
    }
}
