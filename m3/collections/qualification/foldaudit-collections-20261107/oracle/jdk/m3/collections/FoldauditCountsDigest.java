// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

/**
 * Digest oracle of the restored count atoms of qualification/foldaudit-collections-20261107 (PR #1084 leaves L0025 L0028
 * L0029 L0032): seeded aligned count lanes (lengths across the 1024-element native chunk, aliases, offsets, every
 * operation, negative and overflowing inputs) through M3CountLanes.combine / M3CountTotals.combine, and seeded bags
 * through M3CountTotals.sum/intersection/union/difference against M3LongBag's materialized algebra. With -Dfoldaudit.native
 * =true the native entry points run too and their results (or exception classes) join the digest, so the digest of a
 * native run must equal the digest of a Java run (default JIT and -Xint). CPU only; prints one line.
 * usage: java [-Dfoldaudit.native=true] jdk.m3.collections.FoldauditCountsDigest OPS SEED
 */
public final class FoldauditCountsDigest {
    private FoldauditCountsDigest() { }

    public static void main(String[] args) throws Exception {
        int ops = Integer.parseInt(args[0]);
        long seed = Long.parseLong(args[1]);
        boolean nativeToo = Boolean.getBoolean("foldaudit.native");
        if (nativeToo) System.load(System.getProperty("foldaudit.library"));
        Random random = new Random(seed);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long checks = 0;
        int[] lengths = {0, 1, 2, 7, 63, 64, 65, 1023, 1024, 1025, 2049, 4097};
        for (int op = 0; op < ops; op++) {
            int length = lengths[random.nextInt(lengths.length)];
            long[] a = lane(random, length + 5);
            long[] b = random.nextInt(4) == 0 ? a : lane(random, length + 9);
            int ao = random.nextInt(a.length - length + 1);
            int bo = random.nextInt(b.length - length + 1);
            int operation = random.nextInt(6) - 1;
            String java = combine(a, ao, b, bo, length, operation, false);
            digest.update(java.getBytes());
            checks++;
            if (nativeToo) {
                String nat = combine(a, ao, b, bo, length, operation, true);
                if (!nat.equals(java)) throw new AssertionError("native != java at op " + op + ": " + nat + " vs " + java);
                checks++;
            }
            M3LongBag left = bag(random), right = bag(random);
            String totals = bagTotals(left, right);
            digest.update(totals.getBytes());
            checks++;
        }
        System.out.println("FOLDAUDIT_COUNTS ops=" + ops + " seed=" + seed + " native=" + nativeToo + " checks=" + checks
                + " sha256=" + HexFormat.of(digest.digest()));
    }

    private static long[] lane(Random random, int length) {
        long[] lane = new long[length];
        for (int i = 0; i < length; i++) {
            lane[i] = switch (random.nextInt(16)) {
                case 0 -> Long.MAX_VALUE;
                case 1 -> random.nextInt(3) == 0 ? -1L - random.nextInt(5) : Long.MAX_VALUE - random.nextInt(3);
                default -> random.nextInt(1 << 20);
            };
        }
        return lane;
    }

    private static String combine(long[] a, int ao, long[] b, int bo, int length, int op, boolean nat) {
        long[] before = a.clone(), beforeB = b.clone();
        String lanes, total;
        try {
            long[] result = nat ? M3CountLanes.combineNative(a, ao, b, bo, length, op) : M3CountLanes.combine(a, ao, b, bo, length, op);
            lanes = Arrays.hashCode(result) + ":" + result.length;
        } catch (RuntimeException failure) {
            lanes = failure.getClass().getSimpleName();
        }
        try {
            total = Long.toString(nat ? M3CountTotals.combineNative(a, ao, b, bo, length, op) : M3CountTotals.combine(a, ao, b, bo, length, op));
        } catch (RuntimeException failure) {
            total = failure.getClass().getSimpleName();
        }
        if (!Arrays.equals(before, a) || !Arrays.equals(beforeB, b)) throw new AssertionError("input modified");
        return lanes + "/" + total + ";";
    }

    private static M3LongBag bag(Random random) {
        M3LongBag bag = new M3LongBag(random.nextInt(64));
        int distinct = random.nextInt(40);
        for (int i = 0; i < distinct; i++) {
            long key = random.nextInt(6) == 0 ? (random.nextBoolean() ? Long.MIN_VALUE : Long.MAX_VALUE) : random.nextInt(50) - 25;
            long amount = random.nextInt(20) == 0 ? Long.MAX_VALUE / 4 : random.nextInt(100);
            if (amount != 0 && bag.totalCount() <= Long.MAX_VALUE - amount) bag.setCount(key, amount);
        }
        return bag;
    }

    private static String bagTotals(M3LongBag left, M3LongBag right) {
        StringBuilder line = new StringBuilder();
        line.append(scalar(() -> M3CountTotals.sum(left, right))).append(',').append(scalar(() -> left.sum(right).totalCount()))
                .append(',').append(scalar(() -> M3CountTotals.intersection(left, right)))
                .append(',').append(scalar(() -> left.intersection(right).totalCount()))
                .append(',').append(scalar(() -> M3CountTotals.union(left, right))).append(',').append(scalar(() -> left.union(right).totalCount()))
                .append(',').append(scalar(() -> M3CountTotals.difference(left, right)))
                .append(',').append(scalar(() -> left.difference(right).totalCount()));
        String[] cells = line.toString().split(",");
        for (int i = 0; i < cells.length; i += 2) {
            if (!cells[i].equals(cells[i + 1])) throw new AssertionError("scalar total != materialized algebra: " + line);
        }
        return line.append(';').toString();
    }

    private interface Scalar { long get(); }

    private static String scalar(Scalar scalar) {
        try {
            return Long.toString(scalar.get());
        } catch (RuntimeException failure) {
            return failure.getClass().getSimpleName();
        }
    }

    private static final class HexFormat {
        private HexFormat() { }
        static String of(byte[] bytes) {
            StringBuilder text = new StringBuilder();
            for (byte value : bytes) text.append(String.format("%02x", value));
            return text.substring(0, 16);
        }
    }
}
