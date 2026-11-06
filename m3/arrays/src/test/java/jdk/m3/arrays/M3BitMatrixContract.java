// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.util.Arrays;
import java.util.Random;

/** Independent Boolean-array oracle for the canonical matrix Java/JNI source. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d4985c2a46d713724b3872c85633655e53d76662/synexia-mat-collections/src/test/java/com/synexia/matcollections/MatBitMatrixConvergenceContract.java blob f65323e7c40c; recipe m3/arrays/recipe/bitmatrix-20261106/relocate.py v3
public final class M3BitMatrixContract {
    private static long checks;
    private static long scenarios;
    private static final long SEED = 0x3128b7d05eedL;

    private M3BitMatrixContract() { }

    public static void main(String[] args) {
        if (Boolean.getBoolean("m3.bitmatrix.expectMutant")) {
            requireNativeParityRefusal();
            System.out.println("MAT_BIT_MATRIX_NATIVE_MUTANT_REFUSED=PASS");
            return;
        }
        boolean requireNative = Boolean.getBoolean("m3.bitmatrix.requireNative");
        runAll(requireNative);
        System.out.println("MAT_BIT_MATRIX_SCENARIOS=" + scenarios);
        System.out.println("MAT_BIT_MATRIX_ASSERTIONS=" + checks);
        System.out.println("MAT_BIT_MATRIX_BACKEND=" + (requireNative ? "JNI_PARITY" : "JAVA"));
        System.out.println("MAT_BIT_MATRIX_RESULT=PASS");
    }

    private static void requireNativeParityRefusal() {
        if (!M3BitMatrix.nativeAvailable()) throw new AssertionError("mutant library was not loaded");
        M3BitMatrix matrix = M3BitMatrix.builder(2, 3).set(0, 1).set(1, 2).build();
        try {
            matrix.transpose(M3BitMatrix.Backend.NATIVE_REQUIRED);
        } catch (IllegalStateException expected) {
            if ("MAT_BIT_MATRIX_NATIVE_PARITY_FAILURE".equals(expected.getMessage())) return;
            throw new AssertionError("unexpected native failure", expected);
        }
        throw new AssertionError("a corrupted native result was not rejected");
    }

    public static void runAll(boolean requireNative) {
        if (requireNative && !M3BitMatrix.nativeAvailable()) {
            throw new AssertionError("native test was requested but no real JNI library loaded");
        }
        dimensionalBoundaryCases();
        int[] boundaries = {0, 1, 2, 7, 31, 63, 64, 65, 95, 127};
        for (int r : boundaries) for (int c : boundaries) {
            runMatrix(r, c, 0, requireNative, r * 1024L + c);
            runMatrix(r, c, 1, requireNative, r * 2048L + c);
            runMatrix(r, c, 2, requireNative, r * 4096L + c);
        }
        Random random = new Random(SEED);
        for (int round = 0; round < 2048; round++) {
            int rows = random.nextInt(129);
            int columns = random.nextInt(129);
            runMatrix(rows, columns, round % 4, requireNative, random.nextLong());
        }
    }

    private static void dimensionalBoundaryCases() {
        M3BitMatrix zeroRows = M3BitMatrix.builder(0, Integer.MAX_VALUE).build();
        eq(0L, zeroRows.primitivePayloadBytes(), "zero-row size must not overflow");
        eq(0, zeroRows.rows(), "zero rows");
        eq(Integer.MAX_VALUE, zeroRows.columns(), "large declared column universe");
        M3BitMatrix reversed = zeroRows.transpose();
        eq(Integer.MAX_VALUE, reversed.rows(), "large transpose row universe");
        eq(0, reversed.columns(), "large transpose column universe");
        eq(0L, reversed.primitivePayloadBytes(), "zero-column transpose payload");
        M3BitMatrix zeroColumns = M3BitMatrix.builder(Integer.MAX_VALUE, 0).build();
        eq(0L, zeroColumns.primitivePayloadBytes(), "zero-column size must not overflow");
        eq(0L, zeroColumns.transpose().primitivePayloadBytes(), "zero-width reverse transpose");
        if (M3BitMatrix.nativeAvailable()) {
            eq(0L, zeroRows.transpose(M3BitMatrix.Backend.NATIVE_REQUIRED)
                    .primitivePayloadBytes(), "large zero-row JNI transpose");
            eq(0L, zeroColumns.transpose(M3BitMatrix.Backend.NATIVE_REQUIRED)
                    .primitivePayloadBytes(), "large zero-column JNI transpose");
        }
        expect(IllegalArgumentException.class, () -> M3BitMatrix.builder(-1, 10));
        expect(IllegalArgumentException.class, () -> M3BitMatrix.builder(1, -1));
        expect(IndexOutOfBoundsException.class, () -> M3BitMatrix.builder(1, 1).set(1, 0));
        expect(IndexOutOfBoundsException.class, () -> M3BitMatrix.builder(1, 1).set(0, 1));
        expect(IndexOutOfBoundsException.class, () -> M3BitMatrix.builder(1, 1).build().get(-1, 0));
        expect(IndexOutOfBoundsException.class, () -> M3BitMatrix.builder(1, 1).build().rowCardinality(1));
        // Deliberate NATIVE_REQUIRED without configured library must fail closed.
        if (!M3BitMatrix.nativeAvailable()) {
            expect(UnsatisfiedLinkError.class, () -> M3BitMatrix.builder(1, 1).build()
                    .transpose(M3BitMatrix.Backend.NATIVE_REQUIRED));
        }
    }

    private static void runMatrix(int rowCount, int columnCount, int density,
            boolean requireNative, long seed) {
        Random random = new Random(seed);
        boolean[][] oracle = new boolean[rowCount][columnCount];
        M3BitMatrix.Builder builder = M3BitMatrix.builder(rowCount, columnCount);
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                boolean value = switch (density) {
                    case 0 -> false;
                    case 1 -> true;
                    case 2 -> random.nextInt(16) == 0;
                    default -> random.nextBoolean();
                };
                oracle[row][col] = value;
                if (value) builder.set(row, col);
            }
        }
        M3BitMatrix original = builder.build();
        // Check published immutability against later builder mutation.
        if (rowCount != 0 && columnCount != 0) {
            builder.set(0, 0, !oracle[0][0]);
            yes(original.get(0, 0) == oracle[0][0], "defensive builder ownership");
        }
        checkMatrix(original, oracle, "original");
        M3BitMatrix transposed = original.transpose();
        boolean[][] oracleTranspose = transposeBoolean(oracle, columnCount);
        checkMatrix(transposed, oracleTranspose, "transpose");
        checkMatrix(original.transpose(M3BitMatrix.Backend.JAVA), oracleTranspose, "backend Java");
        checkMatrix(transposed.transpose(), oracle, "involution");
        if (requireNative) {
            checkMatrix(original.transpose(M3BitMatrix.Backend.NATIVE_REQUIRED),
                    oracleTranspose, "native transpose");
        }
        scenarios++;
    }

    private static boolean[][] transposeBoolean(boolean[][] value, int columns) {
        boolean[][] result = new boolean[columns][value.length];
        for (int row = 0; row < value.length; row++) {
            for (int col = 0; col < columns; col++) result[col][row] = value[row][col];
        }
        return result;
    }

    private static void checkMatrix(M3BitMatrix actual, boolean[][] expected, String label) {
        eq(expected.length, actual.rows(), label + " row shape");
        int columns = expected.length == 0 ? actual.columns() : expected[0].length;
        if (expected.length > 0) eq(columns, actual.columns(), label + " column shape");
        long payload = 8L * actual.rows() * ((actual.columns() >>> 6)
                + ((actual.columns() & 63) == 0 ? 0 : 1));
        eq(payload, actual.primitivePayloadBytes(), label + " payload shape");
        for (int row = 0; row < expected.length; row++) {
            int cardinality = 0;
            int[] selected = new int[columns];
            for (int col = 0; col < columns; col++) {
                yes(expected[row][col] == actual.get(row, col), label + " coordinate");
                if (expected[row][col]) selected[cardinality++] = col;
            }
            eq(cardinality, actual.rowCardinality(row), label + " row cardinality");
            int[] list = actual.setColumns(row).toArray();
            yes(Arrays.equals(Arrays.copyOf(selected, cardinality), list),
                    label + " selected columns");
        }
    }

    private static void yes(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message + " scenario=" + scenarios);
    }

    private static void eq(long expected, long actual, String message) {
        yes(expected == actual, message + " expected=" + expected + " actual=" + actual);
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong exception type", failure);
        }
        throw new AssertionError("expected " + type);
    }
}
