// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Original boolean[][] oracle for dense 64x64 word-level transposition and JNI parity. */
public final class M3MatBitMatrixTileContract {
    private static long assertions;
    private static long scenarios;

    private M3MatBitMatrixTileContract() { }

    private static void require(boolean ok, String why) {
        assertions++;
        if (!ok) throw new AssertionError(why + " scenario=" + scenarios);
    }

    private static boolean bit(int density, Random random, int row, int col) {
        return switch (density) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> random.nextInt(128) == 0;
            case 3 -> random.nextInt(16) == 0;
            case 4 -> random.nextBoolean();
            case 5 -> ((row + col) & 63) == 0;
            default -> throw new AssertionError("density mode");
        };
    }

    private static void scenario(int rows, int columns, int density, long seed) {
        M3MatBitMatrix.Builder builder = M3MatBitMatrix.builder(rows, columns);
        boolean[][] oracle = new boolean[rows][columns];
        Random random = new Random(seed);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                boolean value = bit(density, random, r, c);
                oracle[r][c] = value;
                if (value) builder.set(r, c);
            }
        }
        M3MatBitMatrix m = builder.build();
        if (rows > 0 && columns > 0) {
            builder.set(0, 0, !oracle[0][0]);
            require(m.get(0, 0) == oracle[0][0], "builder ownership");
        }
        M3MatBitMatrix expected = m.transpose();
        M3MatBitMatrix nativeResult = m.transpose(M3MatBitMatrix.Backend.NATIVE_REQUIRED);
        M3MatBitMatrix twice = nativeResult.transpose();
        require(expected.rows() == columns && nativeResult.rows() == columns, "transpose rows");
        require(expected.columns() == rows && nativeResult.columns() == rows, "transpose cols");
        for (int r = 0; r < rows; r++) {
            int count = 0;
            for (int c = 0; c < columns; c++) {
                require(m.get(r, c) == oracle[r][c], "original coord");
                require(expected.get(c, r) == oracle[r][c], "Java transposed coord");
                require(nativeResult.get(c, r) == oracle[r][c], "JNI transposed coord");
                require(twice.get(r, c) == oracle[r][c], "double transpose coord");
                if (oracle[r][c]) count++;
            }
            require(count == m.rowCardinality(r), "row count");
        }
        for (int r = 0; r < columns; r++) {
            int count = 0;
            int[] javaList = expected.setColumns(r).toArray();
            int[] nativeList = nativeResult.setColumns(r).toArray();
            for (int c = 0; c < rows; c++) if (oracle[c][r]) count++;
            require(nativeResult.rowCardinality(r) == count, "native row count");
            require(expected.rowCardinality(r) == count, "Java row count");
            require(Arrays.equals(javaList, nativeList), "sorted immutable columns");
        }
        scenarios++;
    }

    private static void rejectMalformedNative() throws Exception {
        Method nativeFn = M3MatBitMatrix.class.getDeclaredMethod("transposeNative0",
                long[].class, int.class, int.class, int.class, int.class);
        nativeFn.setAccessible(true);
        require(nativeFn.invoke(null, new long[0], 0, 0, 0, 0) instanceof long[], "valid empty JNI");
        require(nativeFn.invoke(null, new long[0], 1, 1, 0, 1) == null, "wrong input width refused");
        require(nativeFn.invoke(null, new long[0], 1, 1, 1, 1) == null, "wrong length refused");
        require(nativeFn.invoke(null, new long[0], -1, 1, 1, 1) == null, "negative row count refused");
        require(nativeFn.invoke(null, new long[0], 1, -1, 1, 1) == null, "negative column count refused");
        long[] source = {-1L, 0x0102030405060708L};
        long[] before = source.clone();
        Object result = nativeFn.invoke(null, source, 2, 64, 1, 1);
        require(result instanceof long[], "direct JNI returned long array");
        require(Arrays.equals(source, before), "raw JNI must not mutate input");
        require(result != source, "detached output ownership");
    }

    private static void concurrentRead() throws Exception {
        M3MatBitMatrix.Builder b = M3MatBitMatrix.builder(192, 256);
        for (int r = 0; r < 192; r++) for (int c = 0; c < 256; c++) {
            if (((r * 13 + c * 17) & 7) != 0) b.set(r, c);
        }
        M3MatBitMatrix matrix = b.build();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            java.util.List<Future<Integer>> tasks = new java.util.ArrayList<>();
            for (int t = 0; t < 8; t++) {
                tasks.add(pool.submit((Callable<Integer>) () -> {
                    int completed = 0;
                    for (int i = 0; i < 120; i++) {
                        M3MatBitMatrix output = matrix.transpose(M3MatBitMatrix.Backend.NATIVE_REQUIRED);
                        if (output.rows() != 256 || output.columns() != 192 || !output.get(1, 0)) {
                            throw new AssertionError("threaded JNI transpose result");
                        }
                        completed++;
                    }
                    return completed;
                }));
            }
            for (Future<Integer> future : tasks) require(future.get() == 120, "concurrent read count");
        } finally {
            pool.shutdownNow();
        }
    }

    public static void runAll() throws Exception {
        require(M3MatBitMatrix.nativeAvailable(), "explicit native library is mandatory");
        int[] sizes = {0, 1, 31, 63, 64, 65, 95, 127, 128, 129, 191, 192, 193, 256, 257};
        for (int rows : sizes) for (int cols : sizes) for (int density = 0; density < 6; density++) {
            scenario(rows, cols, density, 0x4d3354494c453634L ^ (rows * 131L + cols * 17L + density));
        }
        Random random = new Random(0x4d3354494c454f52L);
        for (int i = 0; i < 400; i++) {
            scenario(random.nextInt(260), random.nextInt(260), i % 6, random.nextLong());
        }
        scenario(512, 512, 1, 123);
        scenario(512, 512, 4, 456);
        scenario(768, 129, 3, 789);
        scenario(129, 768, 4, 987);
        rejectMalformedNative();
        concurrentRead();
        System.out.println("MAT_TILE_SCENARIOS=" + scenarios);
        System.out.println("MAT_TILE_ASSERTIONS=" + assertions);
        System.out.println("MAT_TILE_RESULT=PASS");
    }

    public static void main(String[] ignored) throws Exception { runAll(); }
}
