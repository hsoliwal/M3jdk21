/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Real allocation failure against the uninstrumented production primitive matrix. */
public final class PrimitiveMatrixAllocationTest {
    private static final int BLOCK = 4_096;
    private static final int RELEASE = 160;
    private static int checks;

    private PrimitiveMatrixAllocationTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            for (String mode : new String[]{"zero", "nonzero"}) {
                Process child = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-XX:-UsePerfData", "-Xms16m", "-Xmx16m", "-XX:+UseSerialGC",
                        "-cp", System.getProperty("java.class.path"),
                        PrimitiveMatrixAllocationTest.class.getName(), mode).inheritIO().start();
                if (!child.waitFor(30, TimeUnit.SECONDS)) {
                    child.destroyForcibly();
                    throw new AssertionError("allocation child timed out");
                }
                check(child.exitValue() == 0, "allocation child " + mode + " failed");
            }
            return;
        }
        boolean zero = switch (args[0]) {
            case "zero" -> true;
            case "nonzero" -> false;
            default -> throw new IllegalArgumentException("zero or nonzero");
        };
        PrimitivePrimitiveHashMap warm = new PrimitivePrimitiveHashMap(PrimitiveKind.LONG, PrimitiveKind.LONG, 0, .65f);
        for (int i = 0; i < 100; i++) warm.putBits(i, i, -1);
        PrimitivePrimitiveHashMap map = new PrimitivePrimitiveHashMap(PrimitiveKind.LONG, PrimitiveKind.LONG, 20_000, .65f);
        int threshold = Math.min(map.capacity() - 1, (int) (map.capacity() * .65f));
        for (int i = 1; i <= threshold; i++) map.putBits(i, i * 31L, -1);
        long pending = zero ? 0L : Long.MIN_VALUE;
        long[] keys = (long[]) map.keyArray();
        long[] values = (long[]) map.valueArray();
        long[] keyCopy = keys.clone();
        long[] valueCopy = values.clone();
        int size = map.size();
        int capacity = map.capacity();
        check(capacity == 32_768 && size == threshold, "growth precondition");
        byte[][] ballast = new byte[8_192][];
        int count = 0;
        boolean filled = false;
        try {
            while (count < ballast.length) ballast[count++] = new byte[BLOCK];
        } catch (OutOfMemoryError expected) {
            // Assignment increments count before allocation; release tolerates
            // the possible final null slot and reserves a bounded interval.
            filled = true;
        }
        boolean enough = count > RELEASE;
        if (enough) for (int i = 0; i < RELEASE; i++) ballast[--count] = null;
        boolean failed = false;
        try {
            if (filled && enough) map.putBits(pending, 47, -1);
        } catch (OutOfMemoryError expected) {
            failed = true;
        } finally {
            Arrays.fill(ballast, null);
            ballast = null;
        }
        System.gc();
        check(filled && enough && failed, "actual map growth OOME required");
        System.out.println("MATRIX_OOME_OBSERVED mode=" + args[0]);
        check(map.keyArray() == keys, "failed allocation changed key lane identity");
        check(map.valueArray() == values, "failed allocation changed value lane identity");
        check(Arrays.equals(keys, keyCopy), "failed allocation changed old keys");
        check(Arrays.equals(values, valueCopy), "failed allocation changed old values");
        check(map.size() == size && map.capacity() == capacity, "failed allocation changed map shape");
        check(!map.containsKeyBits(pending), "failed insertion became visible");
        for (int i = 1; i <= threshold; i++) check(map.getOrDefaultBits(i, -1) == i * 31L, "old entry lost");
        check(map.putBits(pending, 47, -1) == -1, "retry return");
        check(map.capacity() == 65_536 && map.size() == size + 1, "retry shape");
        check(map.getOrDefaultBits(pending, -1) == 47, "retry value");
        System.out.println("MATRIX_OOME_PASS mode=" + args[0] + " checks=" + checks);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
