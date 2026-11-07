// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;

/**
 * Reference-model oracle of recipe four-library-bags-20261106 (main, no JUnit/third-party dependency).
 *
 * <p>Drives {@link M3PackedLongBag} against two plain-Java models written here: exact counts in a
 * {@code HashMap<Long, Long>} with a {@link BigInteger} total, and Guava-style multiset semantics ({@code add},
 * {@code remove} and {@code setCount} return the OLD count, {@code Multisets.union/intersection/sum/difference}
 * as count formulas). The relocated contract is checked through its documented mapping: add returns old + n,
 * remove returns max(0, old - n), setCount returns old. Drives {@link M3PackedLongLongHashMap} against
 * {@code HashMap<Long, Long>}. Failure paths the crate proofs pin are replayed with the model deciding the outcome:
 * total overflow (ArithmeticException, inputs unchanged), nonpositive amounts and negative counts
 * (IllegalArgumentException, even on an expected-count mismatch), null operands (NullPointerException), missing
 * keys (getOrThrow), and distinct-key sweeps across the 1023/1024/1025/2048 lane edges of the chunk proof.
 * The constrained-heap OutOfMemoryError paths are M3BagMapFailureProof's (forked JVM).</p>
 *
 * <p>usage: M3FourLibraryBagsReferenceModel [ops] [seed]; prints PASS reference-model ... or throws AssertionError.</p>
 */
public final class M3FourLibraryBagsReferenceModel {
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private final Random random;
    private final M3PackedLongBag[] bags = {new M3PackedLongBag(), new M3PackedLongBag(3)};
    private final GuavaStyle[] models = {new GuavaStyle(), new GuavaStyle()};
    private final M3PackedLongLongHashMap map = new M3PackedLongLongHashMap();
    private final Map<Long, Long> mapModel = new HashMap<>();
    private long checks, bagOps, mapOps, algebra, overflow, argument, nulls, sweeps, missing;

    private M3FourLibraryBagsReferenceModel(long seed) {
        random = new Random(seed);
    }

    /**
     * Runs the model.
     * @param args optional operation count (default 1000000) and seed (default 20261106)
     */
    public static void main(String[] args) {
        long ops = args.length > 0 ? Long.parseLong(args[0]) : 1_000_000L;
        long seed = args.length > 1 ? Long.parseLong(args[1]) : 20261106L;
        M3FourLibraryBagsReferenceModel model = new M3FourLibraryBagsReferenceModel(seed);
        for (long i = 0; i < ops; i++) model.step(i);
        model.verifyAll();
        System.out.println("PASS reference-model ops=" + ops + " seed=" + seed + " checks=" + model.checks
                + " bag_ops=" + model.bagOps + " map_ops=" + model.mapOps + " algebra=" + model.algebra
                + " overflow_refusals=" + model.overflow + " argument_refusals=" + model.argument
                + " null_refusals=" + model.nulls + " missing_key_refusals=" + model.missing
                + " edge_sweeps=" + model.sweeps);
    }

    /** Guava-style multiset semantics over exact counts: mutators return the OLD count. */
    private static final class GuavaStyle {
        final Map<Long, Long> counts = new HashMap<>();
        BigInteger total = BigInteger.ZERO;
        long count(long e) { return counts.getOrDefault(e, 0L); }
        long setCount(long e, long n) {
            long old = count(e);
            if (n == 0) counts.remove(e); else counts.put(e, n);
            total = total.subtract(BigInteger.valueOf(old)).add(BigInteger.valueOf(n));
            return old;
        }
        long add(long e, long n) { long old = count(e); setCount(e, old + n); return old; }
        long remove(long e, long n) { long old = count(e); setCount(e, Math.max(0L, old - n)); return old; }
        void clear() { counts.clear(); total = BigInteger.ZERO; }
        GuavaStyle combine(GuavaStyle other, int op) {
            GuavaStyle result = new GuavaStyle();
            TreeSet<Long> keys = new TreeSet<>(counts.keySet());
            keys.addAll(other.counts.keySet());
            for (long k : keys) {
                long a = count(k), b = other.count(k);
                BigInteger n = switch (op) {
                    case 0 -> BigInteger.valueOf(a).add(BigInteger.valueOf(b));
                    case 1 -> BigInteger.valueOf(Math.max(a, b));
                    case 2 -> BigInteger.valueOf(Math.min(a, b));
                    default -> BigInteger.valueOf(Math.max(0L, a - b));
                };
                if (n.signum() > 0) { result.counts.put(k, n.compareTo(LONG_MAX) > 0 ? -1L : n.longValue()); result.total = result.total.add(n); }
            }
            return result;
        }
        boolean containsOccurrences(GuavaStyle other) {
            return other.counts.entrySet().stream().allMatch(e -> count(e.getKey()) >= e.getValue());
        }
    }

    private long key() {
        return switch (random.nextInt(12)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> random.nextLong();
            default -> random.nextInt(161) - 80;
        };
    }

    private long amount() {
        return switch (random.nextInt(20)) {
            case 0 -> 0L;
            case 1 -> -1L;
            case 2 -> Long.MAX_VALUE - random.nextInt(2);
            case 3 -> Long.MAX_VALUE / 2;
            default -> 1L + random.nextInt(25);
        };
    }

    private void step(long i) {
        if (random.nextInt(3) < 2) bagStep(random.nextInt(2)); else mapStep();
        if ((i & 63) == 0) algebraStep();
        if ((i & 255) == 0) verifyAll();
        if (i % 25_000 == 0) sweepStep();
    }

    private void bagStep(int which) {
        bagOps++;
        M3PackedLongBag bag = bags[which];
        GuavaStyle model = models[which];
        long v = key(), n = amount(), old = model.count(v);
        switch (random.nextInt(9)) {
            case 0, 1, 2 -> {
                if (n <= 0) { refuse(IllegalArgumentException.class, () -> bag.add(v, n)); argument++; }
                else if (model.total.add(BigInteger.valueOf(n)).compareTo(LONG_MAX) > 0) {
                    refuse(ArithmeticException.class, () -> bag.add(v, n)); overflow++;
                } else eq(model.add(v, n) + n, bag.add(v, n));
            }
            case 3, 4 -> {
                if (n <= 0) { refuse(IllegalArgumentException.class, () -> bag.remove(v, n)); argument++; }
                else { long guavaOld = model.remove(v, n); eq(Math.max(0L, guavaOld - n), bag.remove(v, n)); }
            }
            case 5 -> { eq(old, bag.removeAll(v)); model.setCount(v, 0); }
            case 6 -> setCountStep(bag, model, v, n < 0 ? n : n % 1000, old);
            case 7 -> expectedStep(bag, model, v, n, old);
            default -> {
                eq(old, bag.count(v)); eq(old != 0, bag.contains(v));
                if (random.nextInt(64) == 0) { bag.clear(); model.clear(); }
                else if (random.nextInt(16) == 0) bag.trimToSize();
            }
        }
        eq(model.total.longValueExact(), bag.size());
        eq(model.counts.size(), bag.distinctSize());
    }

    private void setCountStep(M3PackedLongBag bag, GuavaStyle model, long v, long n, long old) {
        if (n < 0) { refuse(IllegalArgumentException.class, () -> bag.setCount(v, n)); argument++; return; }
        if (model.total.subtract(BigInteger.valueOf(old)).add(BigInteger.valueOf(n)).compareTo(LONG_MAX) > 0) {
            refuse(ArithmeticException.class, () -> bag.setCount(v, n)); overflow++; return;
        }
        eq(model.setCount(v, n), bag.setCount(v, n));
    }

    private void expectedStep(M3PackedLongBag bag, GuavaStyle model, long v, long n, long old) {
        long expected = random.nextBoolean() ? old : (random.nextInt(8) == 0 ? -1L : old + 1);
        long desired = n < 0 ? n : n % 1000;
        if (expected < 0 || desired < 0) { refuse(IllegalArgumentException.class, () -> bag.setCount(v, expected, desired)); argument++; return; }
        boolean matched = expected == old;
        boolean fits = model.total.subtract(BigInteger.valueOf(old)).add(BigInteger.valueOf(desired)).compareTo(LONG_MAX) <= 0;
        if (matched && !fits) { refuse(ArithmeticException.class, () -> bag.setCount(v, expected, desired)); overflow++; return; }
        eq(matched, bag.setCount(v, expected, desired));
        if (matched) model.setCount(v, desired);
    }

    private void algebraStep() {
        algebra++;
        M3PackedLongBag a = bags[0], b = bags[1];
        GuavaStyle ma = models[0], mb = models[1];
        if (random.nextInt(32) == 0) {
            refuse(NullPointerException.class, () -> a.union(null)); refuse(NullPointerException.class, () -> a.sum(null));
            refuse(NullPointerException.class, () -> a.intersection(null)); refuse(NullPointerException.class, () -> a.difference(null));
            refuse(NullPointerException.class, () -> a.containsOccurrences(null)); nulls += 5;
        }
        checkAlgebra(a, ma, b, mb);
        checkAlgebra(b, mb, a, ma);
        checkAlgebra(a, ma, a, ma);
        M3PackedLongBag copy = a.copy(); verify(ma, copy); copy.clear(); verify(ma, a);
    }

    private void checkAlgebra(M3PackedLongBag a, GuavaStyle ma, M3PackedLongBag b, GuavaStyle mb) {
        GuavaStyle sum = ma.combine(mb, 0), union = ma.combine(mb, 1);
        if (ma.total.add(mb.total).compareTo(LONG_MAX) > 0) { refuse(ArithmeticException.class, () -> a.sum(b)); overflow++; }
        else verify(sum, a.sum(b));
        if (union.total.compareTo(LONG_MAX) > 0) { refuse(ArithmeticException.class, () -> a.union(b)); overflow++; }
        else verify(union, a.union(b));
        verify(ma.combine(mb, 2), a.intersection(b));
        verify(ma.combine(mb, 3), a.difference(b));
        eq(ma.containsOccurrences(mb), a.containsOccurrences(b));
        verify(ma, a); verify(mb, b);
    }

    // Distinct-key sweeps across the lane edges of the crate's chunk proof (1023/1024/1025/2048), with a carry case.
    private void sweepStep() {
        for (int edge : new int[] {1023, 1024, 1025, 2048}) {
            sweeps++;
            M3PackedLongBag wide = new M3PackedLongBag(sweeps % 2 == 0 ? edge : 0), narrow = new M3PackedLongBag();
            GuavaStyle mw = new GuavaStyle(), mn = new GuavaStyle();
            for (int i = 0; i < edge; i++) { long k = (long) i * 0x9E3779B97F4A7C15L; wide.add(k, 1 + i % 5); mw.add(k, 1 + i % 5); }
            for (int i = 0; i < edge; i += 3) { long k = (long) i * 0x9E3779B97F4A7C15L; narrow.add(k, 2); mn.add(k, 2); }
            verify(mw, wide);
            checkAlgebra(wide, mw, narrow, mn);
            long k = (long) (edge - 1) * 0x9E3779B97F4A7C15L, room = Long.MAX_VALUE - mw.total.longValueExact();
            eq(mw.add(k, room) + room, wide.add(k, room));
            refuse(ArithmeticException.class, () -> wide.add(k, 1)); overflow++;
            refuse(ArithmeticException.class, () -> wide.sum(narrow)); overflow++;
            verify(mw, wide);
            wide.trimToSize(); verify(mw, wide);
        }
    }

    private void mapStep() {
        mapOps++;
        long k = key(), v = random.nextInt(4) == 0 ? random.nextLong() : random.nextInt(2001) - 1000;
        switch (random.nextInt(11)) {
            case 0, 1 -> eq(mapModel.put(k, v) == null, map.put(k, v));
            case 2 -> eq(mapModel.remove(k) != null, map.remove(k));
            case 3 -> {
                long wanted = random.nextBoolean() ? mapModel.getOrDefault(k, v) : v;
                boolean hit = mapModel.containsKey(k) && mapModel.get(k) == wanted;
                eq(hit, map.remove(k, wanted)); if (hit) mapModel.remove(k);
            }
            case 4 -> {
                long wanted = random.nextBoolean() ? mapModel.getOrDefault(k, v) : ~v;
                boolean hit = mapModel.containsKey(k) && mapModel.get(k) == wanted;
                eq(hit, map.replace(k, wanted, v)); if (hit) mapModel.put(k, v);
            }
            case 5 -> { long next = mapModel.containsKey(k) ? mapModel.get(k) + v : 7L + v; mapModel.put(k, next); eq(next, map.addTo(k, v, 7L)); }
            case 6 -> { long got = mapModel.computeIfAbsent(k, x -> x ^ v); eq(got, map.computeIfAbsent(k, x -> x ^ v)); }
            case 7 -> {
                if (mapModel.containsKey(k)) eq(mapModel.get(k).longValue(), map.getOrThrow(k));
                else { refuse(IllegalArgumentException.class, () -> map.getOrThrow(k)); missing++; }
            }
            case 8 -> {
                int before = mapModel.size();
                mapModel.entrySet().removeIf(e -> ((e.getKey() ^ e.getValue()) & 7) == (v & 7));
                eq(before - mapModel.size(), map.filterInPlace((x, y) -> ((x ^ y) & 7) != (v & 7)));
            }
            case 9 -> { if (random.nextInt(50) == 0) { map.clear(); mapModel.clear(); } else map.trimToSize(); }
            default -> { eq(mapModel.getOrDefault(k, 97L).longValue(), map.getOrDefault(k, 97L)); eq(mapModel.containsKey(k), map.containsKey(k)); }
        }
        eq(mapModel.size(), map.size());
    }

    private void verifyAll() {
        verify(models[0], bags[0]); verify(models[1], bags[1]);
        Map<Long, Long> seen = new HashMap<>();
        map.forEach((key, value) -> { if (seen.put(key, value) != null) throw new AssertionError("duplicate map key"); });
        eq(mapModel, seen);
        long[] keys = map.keysToArray(), values = map.valuesToArray();
        eq(keys.length, values.length); eq(mapModel.size(), keys.length);
        for (int i = 0; i < keys.length; i++) eq(mapModel.get(keys[i]).longValue(), values[i]);
        int capacity = map.capacity();
        eq(true, capacity >= 2 && Integer.bitCount(capacity) == 1 && capacity >= map.size());
        eq(mapModel.isEmpty(), map.isEmpty());
    }

    private void verify(GuavaStyle expected, M3PackedLongBag actual) {
        Map<Long, Long> seen = new HashMap<>();
        actual.forEachCount((key, count) -> { if (count <= 0 || seen.put(key, count) != null) throw new AssertionError("invalid count lane"); });
        eq(expected.counts, seen);
        eq(expected.total.longValueExact(), actual.size());
        eq(expected.counts.size(), actual.distinctSize());
        eq(expected.counts.isEmpty(), actual.isEmpty());
    }

    private void eq(long expected, long actual) {
        checks++;
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }

    private void eq(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }

    private void refuse(Class<? extends RuntimeException> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (RuntimeException failure) {
            if (failure.getClass() == type) return;
            throw new AssertionError("expected " + type.getSimpleName() + ", got " + failure, failure);
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }
}
