/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections.sketch;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/** Baseline/candidate reference comparison without JNI or runtime dependencies. */
final class HllEstimateCacheOracle {
    private static final Field REGISTERS = privateRegisterField();
    private static final int[] PRECISIONS = {4,5,6,7,8,9,10,11,12,18};
    private static final int LARGE = 18;
    private static final long SEED = 0x4d33484c4c433231L;
    /** Upper bounds of the action ranges of one step, out of 100 draws. */
    private static final int[] BOUNDS = {40, 64, 77, 80, 86, 88, 95};
    private static long assertions;
    private static long operations;
    private static long estimates;
    private static long registerChanges;
    private static long unchangedAdds;
    private static MessageDigest digest;
    private static final byte[] BYTES = new byte[Long.BYTES];

    private HllEstimateCacheOracle() { }

    /** One precision: the sketch under test, one to merge from, and both reference lanes. */
    private record Lanes(int p, LongHyperLogLog source, LongHyperLogLog other, byte[] expected,
            byte[] otherExpected, Random rand) {
        Lanes(int p) {
            this(p, new LongHyperLogLog(p), new LongHyperLogLog(p), new byte[1 << p],
                    new byte[1 << p], new Random(SEED + p));
        }

        /** The smaller count for the largest precision, whose registers are costly to hash. */
        int count(int large, int other) {
            return p == LARGE ? large : other;
        }
    }

    private static Field privateRegisterField() {
        try {
            Field field = LongHyperLogLog.class.getDeclaredField("registers");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static byte[] actualRegisters(LongHyperLogLog hll) {
        try { return (byte[]) REGISTERS.get(hll); }
        catch (IllegalAccessException error) { throw new AssertionError(error); }
    }

    private static void record(long value) {
        for (int i = 0; i < BYTES.length; i++) BYTES[i] = (byte) (value >>> (8 * i));
        digest.update(BYTES);
    }

    private static void eq(long expected, long actual, String where) {
        assertions++;
        if (expected != actual) {
            throw new AssertionError("check " + assertions + " " + where
                    + " expected=" + expected + " actual=" + actual);
        }
        record(actual);
    }

    private static void exact(boolean value, String where) {
        assertions++;
        if (!value) throw new AssertionError("check " + assertions + " " + where);
    }

    private static void throwsType(Class<? extends Throwable> kind, Runnable code) {
        assertions++;
        try { code.run(); }
        catch (Throwable ex) {
            if (kind.isInstance(ex)) { record(kind.getName().hashCode()); return; }
            throw new AssertionError("wrong refusal " + ex, ex);
        }
        throw new AssertionError("missing refusal " + kind.getName());
    }

    /** Independent register-update model, using the mix64 of LongBloomFilter. */
    private static boolean addReference(byte[] registers, int p, long value) {
        long hash = LongBloomFilter.mix64(value);
        int index = (int) (hash >>> (64 - p)) & (registers.length - 1);
        long remaining = (hash << p) | (1L << (p - 1));
        int newRank = Long.numberOfLeadingZeros(remaining) + 1;
        int previous = Byte.toUnsignedInt(registers[index]);
        if (newRank <= previous) return false;
        registers[index] = (byte) newRank;
        return true;
    }

    private static double alpha(int m) {
        return switch (m) {
            case 16 -> 0.673d;
            case 32 -> 0.697d;
            case 64 -> 0.709d;
            default -> 0.7213d / (1d + 1.079d / m);
        };
    }

    private static long estimateReference(byte[] registers) {
        int m = registers.length;
        double sum = 0d;
        int zeros = 0;
        for (byte register : registers) {
            int rank = Byte.toUnsignedInt(register);
            sum += Math.scalb(1d, -rank);
            if (rank == 0) zeros++;
        }
        double estimate = alpha(m) * m * (double) m / sum;
        if (estimate <= 2.5d * m && zeros != 0) estimate = m * Math.log(m / (double) zeros);
        if (estimate >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(0L, Math.round(estimate));
    }

    private static void checkState(LongHyperLogLog hll, byte[] expected) {
        byte[] actual = actualRegisters(hll);
        eq(expected.length, actual.length, "register count");
        exact(Arrays.equals(expected, actual), "every register matches reference");
        for (int i = 0; i < expected.length; i++) record(Byte.toUnsignedInt(actual[i]));
    }

    private static void repeated(LongHyperLogLog hll, byte[] registers, int count) {
        long answer = estimateReference(registers);
        for (int i = 0; i < count; i++) {
            eq(answer, hll.estimate(), "repeated estimate must remain stable");
            estimates++;
        }
    }

    /** The reference of a merge: every register of {@code into} becomes the larger of the two. */
    private static void foldMax(byte[] into, byte[] from) {
        for (int i = 0; i < into.length; i++) {
            if (Byte.toUnsignedInt(from[i]) > Byte.toUnsignedInt(into[i])) into[i] = from[i];
        }
    }

    private static void clear(LongHyperLogLog hll, byte[] registers) {
        hll.clear();
        Arrays.fill(registers, (byte) 0);
    }

    private static void opening(Lanes lanes) {
        eq(lanes.p(), lanes.source().precision(), "precision");
        eq(1 << lanes.p(), lanes.source().registerCount(), "registerCount");
        eq(1 << lanes.p(), lanes.source().payloadBytes(), "payload bytes");
        repeated(lanes.source(), lanes.expected(), lanes.count(2, 8));
        checkState(lanes.source(), lanes.expected());
    }

    private static void addToSource(Lanes lanes, long value) {
        boolean expectedChange = addReference(lanes.expected(), lanes.p(), value);
        boolean actual = lanes.source().add(value);
        eq(expectedChange ? 1 : 0, actual ? 1 : 0, "source add changed");
        if (expectedChange) registerChanges++; else unchangedAdds++;
        // A second insertion is guaranteed unchanged and must not
        // invalidate a cached estimate or any register.
        eq(0, lanes.source().add(value) ? 1 : 0, "repeated add");
        unchangedAdds++;
    }

    private static void addToOther(Lanes lanes, long value) {
        boolean expectedChange = addReference(lanes.otherExpected(), lanes.p(), value);
        eq(expectedChange ? 1 : 0, lanes.other().add(value) ? 1 : 0, "other add");
    }

    private static void mergeOther(Lanes lanes) {
        foldMax(lanes.expected(), lanes.otherExpected());
        lanes.source().merge(lanes.other());
    }

    private static void oneEstimate(Lanes lanes) {
        eq(estimateReference(lanes.expected()), lanes.source().estimate(), "one estimate");
        estimates++;
    }

    /** The index of the action range that holds {@code action}. */
    private static int range(int action) {
        int range = 0;
        while (range < BOUNDS.length && action >= BOUNDS[range]) range++;
        return range;
    }

    private static void step(Lanes lanes) {
        int action = lanes.rand().nextInt(100);
        long value = lanes.rand().nextInt(12) == 0 ? 0L : lanes.rand().nextLong();
        switch (range(action)) {
            case 0 -> addToSource(lanes, value);
            case 1 -> addToOther(lanes, value);
            case 2 -> mergeOther(lanes);
            case 3 -> lanes.source().merge(lanes.source()); // a self merge changes nothing
            case 4 -> clear(lanes.source(), lanes.expected());
            case 5 -> clear(lanes.other(), lanes.otherExpected());
            case 6 -> repeated(lanes.source(), lanes.expected(), lanes.count(1, 3));
            default -> oneEstimate(lanes);
        }
    }

    /** The complete lanes are hashed periodically, not after every operation. */
    private static void checkpoint(Lanes lanes) {
        repeated(lanes.source(), lanes.expected(), lanes.count(1, 4));
        checkState(lanes.source(), lanes.expected());
        checkState(lanes.other(), lanes.otherExpected());
    }

    private static void closing(Lanes lanes) {
        lanes.source().merge(lanes.other());
        foldMax(lanes.expected(), lanes.otherExpected());
        repeated(lanes.source(), lanes.expected(), lanes.count(3, 128));
        checkState(lanes.source(), lanes.expected());
        clear(lanes.source(), lanes.expected());
        repeated(lanes.source(), lanes.expected(), lanes.count(2, 32));
        checkState(lanes.source(), lanes.expected());
    }

    private static void refusals(Lanes lanes) {
        LongHyperLogLog source = lanes.source();
        throwsType(IllegalArgumentException.class, () -> new LongHyperLogLog(3));
        throwsType(IllegalArgumentException.class, () -> new LongHyperLogLog(19));
        throwsType(NullPointerException.class, () -> source.merge(null));
        int different = lanes.p() == 4 ? 5 : 4;
        throwsType(IllegalArgumentException.class,
                () -> source.merge(new LongHyperLogLog(different)));
        checkState(source, lanes.expected());
    }

    private static void trial(int p) {
        Lanes lanes = new Lanes(p);
        opening(lanes);
        int rounds = lanes.count(48, 1800);
        int period = lanes.count(40, 80);
        for (int i = 0; i < rounds; i++) {
            step(lanes);
            operations++;
            if (i % period == 0 || i == rounds - 1) checkpoint(lanes);
        }
        closing(lanes);
        refusals(lanes);
    }

    static String run() {
        assertions = operations = estimates = registerChanges = unchangedAdds = 0;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        for (int p : PRECISIONS) trial(p);
        return "assertions=" + assertions + ",operations=" + operations
                + ",estimates=" + estimates + ",changed=" + registerChanges
                + ",unchangedAdds=" + unchangedAdds + ",sha256="
                + HexFormat.of().formatHex(digest.digest());
    }
}
