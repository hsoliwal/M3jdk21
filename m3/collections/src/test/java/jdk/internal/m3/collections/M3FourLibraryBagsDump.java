// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Dump driver of the differential gate of recipe four-library-bags-20261106 (not a test, no Test suffix).
 *
 * <p>Replays seeded random operations through three {@link M3PackedLongBag} instances and two
 * {@link M3PackedLongLongHashMap} instances and prints every observable result (return values, thrown exception
 * classes and messages, totals, slot-order digests of traversals and exported arrays) as one text line per
 * operation, so two runs can be compared byte for byte. No line prints a type name or an identity hash.</p>
 *
 * <p>The donor side of the gate compiles this same source after differential.sh maps the type names back through
 * the TYPE rows of RELOCATION_MAP.tsv (and the package line through the PACKAGE row), so donor and relocated
 * classes replay one program. Traversal order is part of the dump: both sides run one algorithm.</p>
 *
 * <p>usage: M3FourLibraryBagsDump [ops] [seed]</p>
 */
public final class M3FourLibraryBagsDump {
    private static final long[] EXTREMES = {Long.MIN_VALUE, Long.MAX_VALUE, 0L, -1L, 1L, Long.MIN_VALUE + 1};
    private final Random random;
    private final PrintStream out;
    private final M3PackedLongBag[] bags = new M3PackedLongBag[3];
    private final M3PackedLongLongHashMap[] maps = new M3PackedLongLongHashMap[2];
    private long step;

    private M3FourLibraryBagsDump(long seed, PrintStream out) {
        this.random = new Random(seed);
        this.out = out;
        for (int i = 0; i < bags.length; i++) bags[i] = new M3PackedLongBag(i * 7);
        for (int i = 0; i < maps.length; i++) maps[i] = new M3PackedLongLongHashMap(i * 5);
    }

    /**
     * Runs the dump.
     * @param args optional operation count (default 200000) and seed (default 20261106)
     */
    public static void main(String[] args) {
        long ops = args.length > 0 ? Long.parseLong(args[0]) : 200_000L;
        long seed = args.length > 1 ? Long.parseLong(args[1]) : 20261106L;
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        M3FourLibraryBagsDump dump = new M3FourLibraryBagsDump(seed, out);
        for (long i = 0; i < ops; i++) dump.next();
        dump.summary(ops, seed);
        out.flush();
    }

    private long key() {
        return switch (random.nextInt(10)) {
            case 0 -> EXTREMES[random.nextInt(EXTREMES.length)];
            case 1 -> random.nextLong();
            case 2 -> (long) random.nextInt(4096) << 32;
            default -> random.nextInt(97) - 48;
        };
    }

    private long amount() {
        return switch (random.nextInt(16)) {
            case 0 -> 0L;
            case 1 -> -1L - random.nextInt(3);
            case 2 -> Long.MAX_VALUE - random.nextInt(3);
            case 3 -> (long) Integer.MAX_VALUE + random.nextInt(5);
            default -> 1L + random.nextInt(20);
        };
    }

    private void next() {
        step++;
        if (random.nextInt(5) < 3) bagOp(random.nextInt(bags.length));
        else mapOp(random.nextInt(maps.length));
    }

    private void bagOp(int which) {
        M3PackedLongBag bag = bags[which];
        M3PackedLongBag other = bags[(which + 1 + random.nextInt(bags.length - 1)) % bags.length];
        long v = key(), n = amount(), e = random.nextBoolean() ? bag.count(v) : amount();
        int op = random.nextInt(24);
        String head = step + " b" + which + " " + op + " " + v + " " + n;
        try {
            String result = switch (op) {
                case 0, 1, 2, 3 -> Long.toString(bag.add(v, n));
                case 4 -> Long.toString(bag.add(v));
                case 5, 6, 7 -> Long.toString(bag.remove(v, n));
                case 8 -> Long.toString(bag.removeAll(v));
                case 9, 10 -> Long.toString(bag.setCount(v, Math.max(-2L, n % 50)));
                case 11 -> e + " " + bag.setCount(v, e, Math.max(-1L, n % 40));
                case 12 -> bag.count(v) + " " + bag.contains(v);
                case 13 -> { bag.trimToSize(); yield "trim"; }
                case 14 -> { if (random.nextInt(16) == 0) bag.clear(); yield "clear?"; }
                case 15 -> describe(bag.copy());
                case 16 -> Boolean.toString(bag.containsOccurrences(random.nextInt(40) == 0 ? null : other));
                case 17 -> describe(bag.union(random.nextInt(40) == 0 ? null : other));
                case 18 -> describe(bag.intersection(random.nextInt(40) == 0 ? null : other));
                case 19 -> describe(bag.sum(random.nextInt(40) == 0 ? null : other));
                case 20 -> describe(bag.difference(random.nextInt(40) == 0 ? null : other));
                case 21 -> describe(bag.union(bag)) + " " + describe(bag.intersection(bag)) + " " + describe(bag.difference(bag));
                case 22 -> describe(bag.sum(bag));
                default -> sweep(bag, v);
            };
            out.println(head + " -> " + result + " | " + describe(bag));
        } catch (RuntimeException failure) {
            out.println(head + " -> !" + failure.getClass().getSimpleName() + ":" + failure.getMessage() + " | " + describe(bag));
        }
    }

    // Distinct-key sweeps across the 1023/1024/1025/2048 lane edges of the crate's chunk proof, then one algebra step.
    private String sweep(M3PackedLongBag bag, long base) {
        int[] edges = {1023, 1024, 1025, 2048};
        int edge = edges[random.nextInt(edges.length)];
        M3PackedLongBag wide = new M3PackedLongBag(random.nextInt(3) == 0 ? edge : 0);
        for (int i = 0; i < edge; i++) wide.add(base + i, 1 + (i % 7));
        return edge + " " + describe(wide) + " " + describe(wide.sum(bag)) + " " + describe(wide.difference(bag))
                + " " + wide.containsOccurrences(bag);
    }

    private void mapOp(int which) {
        M3PackedLongLongHashMap map = maps[which];
        long k = key(), v = random.nextInt(4) == 0 ? random.nextLong() : random.nextInt(1000) - 500;
        int op = random.nextInt(18);
        String head = step + " m" + which + " " + op + " " + k + " " + v;
        try {
            String result = switch (op) {
                case 0, 1, 2 -> Boolean.toString(map.put(k, v));
                case 3 -> map.containsKey(k) + " " + map.getOrDefault(k, 97L);
                case 4 -> Long.toString(map.getOrThrow(k));
                case 5 -> Boolean.toString(map.replace(k, random.nextBoolean() ? map.getOrDefault(k, v) : v, v ^ 5));
                case 6 -> Long.toString(map.computeIfAbsent(k, x -> x ^ v));
                case 7, 8 -> Long.toString(map.addTo(k, v, random.nextInt(3) - 1));
                case 9 -> Boolean.toString(map.remove(k));
                case 10 -> Boolean.toString(map.remove(k, random.nextBoolean() ? map.getOrDefault(k, v) : v));
                case 11 -> Integer.toString(map.filterInPlace((x, y) -> ((x ^ y) & 3) != (v & 3)));
                case 12 -> { map.trimToSize(); yield "trim " + map.capacity(); }
                case 13 -> { if (random.nextInt(20) == 0) map.clear(); yield "clear? " + map.capacity(); }
                case 14 -> arrays(map);
                case 15 -> Long.toString(map.computeIfAbsent(k, null));
                case 16 -> { M3PackedLongLongHashMap fresh = new M3PackedLongLongHashMap(random.nextInt(6) - 1); yield fresh.capacity() + " " + fresh.size(); }
                default -> Integer.toString(map.filterInPlace(null));
            };
            out.println(head + " -> " + result + " | " + describe(map));
        } catch (RuntimeException failure) {
            out.println(head + " -> !" + failure.getClass().getSimpleName() + ":" + failure.getMessage() + " | " + describe(map));
        }
    }

    private static String arrays(M3PackedLongLongHashMap map) {
        long[] keys = map.keysToArray(), values = map.valuesToArray();
        long hash = keys.length;
        for (int i = 0; i < keys.length; i++) hash = hash * 31 + keys[i] * 17 + values[i];
        return keys.length + "/" + values.length + " " + hash;
    }

    private static String describe(M3PackedLongBag bag) {
        long[] digest = {0L, 0L};
        bag.forEachCount((key, count) -> { digest[0] = digest[0] * 1_000_003L + key * 31 + count; digest[1]++; });
        return bag.size() + "/" + bag.distinctSize() + "/" + bag.isEmpty() + "/" + digest[1] + "/" + digest[0];
    }

    private static String describe(M3PackedLongLongHashMap map) {
        long[] digest = {0L, 0L};
        map.forEach((key, value) -> { digest[0] = digest[0] * 1_000_003L + key * 31 + value; digest[1]++; });
        return map.size() + "/" + map.isEmpty() + "/" + map.capacity() + "/" + digest[1] + "/" + digest[0];
    }

    private void summary(long ops, long seed) {
        StringBuilder line = new StringBuilder("TOTAL ops=").append(ops).append(" seed=").append(seed);
        for (int i = 0; i < bags.length; i++) line.append(" b").append(i).append('=').append(describe(bags[i]));
        for (int i = 0; i < maps.length; i++) line.append(" m").append(i).append('=').append(describe(maps[i]));
        out.println(line);
    }
}
