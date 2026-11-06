// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Differential dump and reference model (gates 3 and 4 of recipe/int-ring-buffer-20261106/differential.sh). Replays
 * one seeded corpus of operation sequences (capacities 1..1,025 around every power of two plus seeded capacities up
 * to 5,000; offerFirst / offerLast with boundary and seeded values, peek and remove at both ends, snapshots,
 * fill-to-full and drain phases) through the ring class named by args[0], reached by reflection so that the donor
 * class and the relocated class run the same driver. For each sequence it prints the capacity, payload bytes, the
 * counts of accepted and refused offers and of empty-buffer exceptions, a 64-bit digest of every result in order
 * (return values, exception classes, size / isEmpty / isFull after each step, snapshot contents) and the final
 * snapshot length and digest. Every result is also checked against an independent brute-force model (a plain int
 * array shifted by System.arraycopy, bounded by the capacity); a mismatch aborts the run. Two sides agree when their
 * dumps are byte-identical.
 * Usage: {@code java -cp classes;test-classes jdk.m3.arrays.M3IntRingBufferDump FQN [CASES]}
 */
//FLLDCIM: TFG - dump driver from the template M3CsrIndexDump (recipe csr-index-20261106): reflective, serves donor and relocated class
public final class M3IntRingBufferDump {
    private static final long SEED = 0x5249_4E47_4255_4632L;
    private static final int DEFAULT_CASES = 2_000;
    private static final int MAX_POWER = 10;
    private static final int SEEDED_CAPACITY = 5_000;
    private static final int OPS_PER_CAPACITY = 4;
    private static final int OPS_MIN = 64;
    private static final int ACTIONS = 12;
    private static final int SNAPSHOT_EVERY = 64;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final long FNV_BASIS = 0xcbf29ce484222325L;
    private static final int[] BOUNDARY_VALUES = {Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 1};

    private final Constructor<?> create;
    private final Method offerFirst;
    private final Method offerLast;
    private final Method peekFirst;
    private final Method peekLast;
    private final Method removeFirst;
    private final Method removeLast;
    private final Method size;
    private final Method isEmpty;
    private final Method isFull;
    private final Method capacity;
    private final Method snapshot;
    private final Method payload;
    private final PrintStream out;
    private long references;
    private long refusals;
    private long digest;

    private M3IntRingBufferDump(Class<?> ring, PrintStream out) throws ReflectiveOperationException {
        this.create = ring.getConstructor(int.class);
        this.offerFirst = ring.getMethod("offerFirst", int.class);
        this.offerLast = ring.getMethod("offerLast", int.class);
        this.peekFirst = ring.getMethod("peekFirst");
        this.peekLast = ring.getMethod("peekLast");
        this.removeFirst = ring.getMethod("removeFirst");
        this.removeLast = ring.getMethod("removeLast");
        this.size = ring.getMethod("size");
        this.isEmpty = ring.getMethod("isEmpty");
        this.isFull = ring.getMethod("isFull");
        this.capacity = ring.getMethod("capacity");
        this.snapshot = ring.getMethod("snapshot");
        this.payload = ring.getMethod("primitivePayloadBytes");
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        run(Class.forName(args[0]), args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_CASES, out);
        out.flush();
    }

    /** Runs the corpus; returns {cases, reference comparisons, refusals}. */
    static long[] run(Class<?> ring, int cases, PrintStream out) throws Exception {
        M3IntRingBufferDump dump = new M3IntRingBufferDump(ring, out);
        dump.refusalCases();
        SplittableRandom random = new SplittableRandom(SEED);
        for (int id = 0; id < cases; id++) {
            dump.sequence(id, capacityOf(id, random), random);
        }
        out.println("TOTAL cases=" + cases + " references=" + dump.references + " refusals=" + dump.refusals);
        return new long[] {cases, dump.references, dump.refusals};
    }

    static PrintStream discard() {
        return new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);
    }

    private static int capacityOf(int id, SplittableRandom random) {
        int power = 1 << (id % (MAX_POWER + 1));
        return switch (id % 4) {
            case 0 -> power;
            case 1 -> power + 1;
            case 2 -> Math.max(1, power - 1);
            default -> 1 + random.nextInt(SEEDED_CAPACITY);
        };
    }

    private void sequence(int id, int cap, SplittableRandom random) throws Exception {
        Object ring = create.newInstance(cap);
        int[] model = new int[cap];
        int[] counts = new int[4];
        digest = FNV_BASIS;
        expect(cap == (int) capacity.invoke(ring) && 4L * cap == (long) payload.invoke(ring), "capacity");
        int ops = OPS_MIN + cap * OPS_PER_CAPACITY;
        int n = 0;
        for (int step = 0; step < ops; step++) {
            n = step(ring, model, n, cap, step, random, counts);
        }
        n = fillAndDrain(ring, model, n, cap, counts);
        int[] last = (int[]) snapshot.invoke(ring);
        expect(Arrays.equals(last, Arrays.copyOf(model, n)), "final snapshot");
        out.println("CASE " + id + " capacity=" + cap + " payload=" + payload.invoke(ring) + " ops=" + ops
                + " accepted=" + counts[0] + " refusedFull=" + counts[1] + " emptyThrows=" + counts[2]
                + " snapshots=" + counts[3] + " size=" + n + " digest=" + Long.toHexString(digest)
                + " last=" + last.length + ':' + Integer.toHexString(Arrays.hashCode(last)));
    }

    private int step(Object ring, int[] model, int n, int cap, int step, SplittableRandom random, int[] counts) throws Exception {
        int value = step % 7 < BOUNDARY_VALUES.length ? BOUNDARY_VALUES[step % 7] : random.nextInt();
        int action = random.nextInt(ACTIONS);
        int next = switch (action) {
            case 0, 1, 2 -> offer(ring, model, n, cap, value, false, counts);
            case 3, 4 -> offer(ring, model, n, cap, value, true, counts);
            case 5, 6 -> take(ring, model, n, removeFirst, true, true, counts);
            case 7 -> take(ring, model, n, removeLast, false, true, counts);
            case 8 -> take(ring, model, n, peekFirst, true, false, counts);
            case 9 -> take(ring, model, n, peekLast, false, false, counts);
            default -> snap(ring, model, n, counts);
        };
        expect(next == (int) size.invoke(ring) && (next == 0) == (boolean) isEmpty.invoke(ring)
                && (next == cap) == (boolean) isFull.invoke(ring), "size/isEmpty/isFull");
        mix(next);
        return step % SNAPSHOT_EVERY == 0 ? snap(ring, model, next, counts) : next;
    }

    private int offer(Object ring, int[] model, int n, int cap, int value, boolean first, int[] counts) throws Exception {
        boolean accepted = (boolean) (first ? offerFirst : offerLast).invoke(ring, value);
        expect(accepted == (n < cap), first ? "offerFirst" : "offerLast");
        mix(accepted ? 1 : 0);
        if (!accepted) {
            counts[1]++;
            return n;
        }
        counts[0]++;
        if (first) {
            System.arraycopy(model, 0, model, 1, n);
            model[0] = value;
        } else {
            model[n] = value;
        }
        return n + 1;
    }

    private int take(Object ring, int[] model, int n, Method method, boolean first, boolean remove, int[] counts) throws Exception {
        String result = call(() -> method.invoke(ring));
        if (n == 0) {
            expect(result.equals("java.util.NoSuchElementException"), "empty " + method.getName());
            counts[2]++;
            mix(result.hashCode());
            return 0;
        }
        int expected = first ? model[0] : model[n - 1];
        expect(result.equals(Integer.toString(expected)), method.getName());
        mix(expected);
        if (remove && first) {
            System.arraycopy(model, 1, model, 0, n - 1);
        }
        return remove ? n - 1 : n;
    }

    private int snap(Object ring, int[] model, int n, int[] counts) throws Exception {
        int[] copy = (int[]) snapshot.invoke(ring);
        expect(Arrays.equals(copy, Arrays.copyOf(model, n)), "snapshot");
        if (copy.length > 0) {
            copy[0] ^= 0x5A5A_5A5A;
            expect((int) peekFirst.invoke(ring) == model[0], "snapshot independent");
        }
        counts[3]++;
        mix(copy.length);
        mix(Arrays.hashCode(Arrays.copyOf(model, n)));
        return n;
    }

    private int fillAndDrain(Object ring, int[] model, int n, int cap, int[] counts) throws Exception {
        int filled = n;
        while (filled < cap) {
            filled = offer(ring, model, filled, cap, filled, (filled & 1) == 0, counts);
        }
        filled = offer(ring, model, filled, cap, -2, false, counts);
        int left = filled;
        while (left > 0) {
            left = take(ring, model, left, (left & 1) == 0 ? removeFirst : removeLast, (left & 1) == 0, true, counts);
        }
        return take(ring, model, 0, peekLast, false, false, counts);
    }

    private void mix(long value) {
        digest = (digest ^ value) * FNV_PRIME;
    }

    private void expect(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError("REFERENCE_DRIFT " + what);
        }
        references++;
    }

    private void refusalCases() {
        StringBuilder line = new StringBuilder("REFUSALS");
        for (int bad : new int[] {0, -1, Integer.MIN_VALUE}) {
            line.append(" capacity").append(bad).append('=').append(call(() -> create.newInstance(bad)));
        }
        out.println(line);
    }

    @FunctionalInterface
    private interface Call { Object call() throws ReflectiveOperationException; }

    /** The returned value as text, or the exception class of a refused call. */
    private String call(Call call) {
        try {
            return String.valueOf(call.call());
        } catch (InvocationTargetException thrown) {
            refusals++;
            return thrown.getCause().getClass().getName();
        } catch (ReflectiveOperationException broken) {
            throw new AssertionError("reflection failed", broken);
        }
    }
}
