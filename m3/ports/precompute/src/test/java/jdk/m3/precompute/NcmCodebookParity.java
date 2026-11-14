// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** Executable independent oracle and Java/JNI contract proof for the numerical primitive. */
public final class NcmCodebookParity {
  private static long checks;
  private static Method nativeMethod;

  private NcmCodebookParity() {}

  public static void main(String[] args) throws Exception {
    boolean requireNative = Arrays.asList(args).contains("--require-native");
    boolean forbidNative = Arrays.asList(args).contains("--forbid-native");
    if (requireNative && forbidNative) throw new IllegalArgumentException("conflicting native requirements");
    verify(requireNative, forbidNative);
    if (Arrays.asList(args).contains("--require-legacy-native")) {
      check(NativePrecomputeOps.available(), "legacy precompute ABI must load");
      check(NativePrecomputeOps.tuplesAvailable(), "legacy tuple ABI must load");
      check(!NativePrecomputeOps.ncmCodebookAvailable(), "legacy library must lack NCM feature");
    }
    System.out.println("NCM_CODEBOOK_PARITY_PASS checks=" + checks
        + " native=" + NativePrecomputeOps.ncmCodebookAvailable());
  }

  public static void verify(boolean requireNative, boolean forbidNative) throws Exception {
    checks = 0;
    nativeMethod = null;
    check(!requireNative || NativePrecomputeOps.ncmCodebookAvailable(), "required JNI feature is unavailable");
    check(!forbidNative || !NativePrecomputeOps.ncmCodebookAvailable(), "JNI feature unexpectedly available");
    if (NativePrecomputeOps.ncmCodebookAvailable()) {
      nativeMethod = NativePrecomputeOps.class.getDeclaredMethod("nearestNcmCodesNative",
          double[].class, int.class, int.class, double[].class, int.class, int.class, int.class);
      nativeMethod.setAccessible(true);
    }
    fixedCases();
    roundingWitnesses();
    legacyOperations();
    invalidCases();
    exactOracleProperties();
    binary64ParityProperties();
    nearTieParityProperties();
    concurrentCalls();
  }

  private static void legacyOperations() {
    FrozenLongTupleIndex pairs = FrozenLongTupleIndex.freeze(IndexPrecomputeCapability.TUPLE_ID,
        2, new long[] {1, 2, 3, 4}, new int[] {17, 23});
    check(Arrays.equals(new int[] {17, -1, 23}, NativePrecomputeOps.lookupPairs(pairs,
        new long[] {1, 9, 3}, new long[] {2, 9, 4}, -1, 0)), "legacy pair operation changed");
    for (int arity = 1; arity <= 4; arity++) {
      long[] keys = new long[arity * 2];
      long[] queries = new long[arity * 3];
      for (int d = 0; d < arity; d++) {
        keys[d] = d + 1;
        keys[arity + d] = d + 11;
        queries[d] = d + 1;
        queries[arity + d] = 99;
        queries[2 * arity + d] = d + 11;
      }
      FrozenLongTupleIndex index = FrozenLongTupleIndex.freeze(
          IndexPrecomputeCapability.TUPLE_ID, arity, keys, new int[] {17, 23});
      check(Arrays.equals(new int[] {17, -1, 23}, NativePrecomputeOps.lookupTuples(index,
          queries, -1, 0)), "legacy tuple operation changed");
    }
  }

  private static void fixedCases() {
    compare(new double[] {1, 1, 4, 5}, 0, 2,
        new double[] {0, 0, 2, 2, 4, 4}, 0, 3, 2, new int[] {0, 2});
    compare(new double[] {Double.NaN, 2, 2, Double.NEGATIVE_INFINITY}, 1, 1,
        new double[] {Double.NaN, 0, 0, 2, 2, 2, 2, Double.POSITIVE_INFINITY},
        1, 3, 2, new int[] {1});
    compare(new double[] {-0.0, Double.MIN_VALUE}, 0, 2,
        new double[] {+0.0, -0.0, Double.MIN_VALUE}, 0, 3, 1, new int[] {0, 0});
    compare(new double[0], 0, 0, new double[0], 0, 0, 1, new int[0]);
    compare(new double[] {Double.NaN}, 1, 0,
        new double[] {Double.NaN}, 1, 0, Integer.MAX_VALUE, new int[0]);
    compare(new double[0], 0, 0, new double[] {7}, 0, 1, 1, new int[0]);
    double[] shared = {0, 0, 5, 5, 6, 7};
    compare(shared, 2, 2, shared, 0, 3, 2, new int[] {1, 2});
    int[] changed = NcmCodebook.nearest(new double[] {0}, 0, 1, new double[] {0}, 0, 1, 1);
    changed[0] = 99;
    check(NcmCodebook.nearest(new double[] {0}, 0, 1, new double[] {0}, 0, 1, 1)[0] == 0,
        "result must be fresh caller-owned storage");
  }

  // Both witnesses choose a different code under an illegal arithmetic rewrite.
  private static void roundingWitnesses() {
    double x = 0x1.0000002000001p0;
    double y = 0x1p-27;
    check(Math.fma(x, x, y * y) > x * x + y * y,
        "FMA witness must distinguish contraction");
    compare(new double[2], 0, 1, new double[] {y, x, 0, x}, 0, 2, 2, new int[] {0});
    double z = 0x1.4p-27;
    double small = z * z;
    check(1 + (small + small) > (1 + small) + small,
        "sum witness must distinguish reassociation");
    compare(new double[3], 0, 1, new double[] {1, z, z, 1, 0, 0}, 0, 2, 3, new int[] {0});
  }

  private static void invalidCases() {
    invalid(NullPointerException.class, null, 0, 1, new double[] {0}, 0, 1, 1);
    invalid(NullPointerException.class, new double[] {0}, 0, 1, null, 0, 1, 1);
    invalid(IllegalArgumentException.class, new double[] {0}, 0, -1, new double[] {0}, 0, 1, 1);
    invalid(IllegalArgumentException.class, new double[] {0}, 0, 1, new double[] {0}, 0, -1, 1);
    invalid(IllegalArgumentException.class, new double[] {0}, 0, 1, new double[] {0}, 0, 0, 1);
    invalid(IllegalArgumentException.class, new double[] {0}, 0, 1, new double[] {0}, 0, 1, 0);
    invalid(IllegalArgumentException.class, new double[0], 0, 0, new double[0], 0, 0, -1);
    invalid(IndexOutOfBoundsException.class, new double[] {0}, -1, 1, new double[] {0}, 0, 1, 1);
    invalid(IndexOutOfBoundsException.class, new double[] {0}, 2, 0, new double[] {0}, 0, 1, 1);
    invalid(IndexOutOfBoundsException.class, new double[] {0}, 0, 1, new double[] {0}, -1, 1, 1);
    invalid(IndexOutOfBoundsException.class, new double[] {0}, 0, 2, new double[] {0}, 0, 1, 1);
    invalid(IndexOutOfBoundsException.class, new double[] {0}, 0, 1, new double[] {0}, 1, 1, 1);
    invalid(IndexOutOfBoundsException.class, new double[] {0}, 0, Integer.MAX_VALUE,
        new double[] {0}, 0, 1, Integer.MAX_VALUE);
    invalid(IndexOutOfBoundsException.class, new double[] {0}, 0, 0,
        new double[] {0}, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
      invalid(IllegalArgumentException.class, new double[] {bad}, 0, 1, new double[] {0}, 0, 1, 1);
      invalid(IllegalArgumentException.class, new double[] {0}, 0, 1, new double[] {bad}, 0, 1, 1);
      invalid(IllegalArgumentException.class, new double[0], 0, 0, new double[] {bad}, 0, 1, 1);
    }
    // Validate every selected element before any pair computation can overflow.
    invalid(IllegalArgumentException.class, new double[] {Double.MAX_VALUE, Double.NaN},
        0, 2, new double[] {0}, 0, 1, 1);
    invalid(ArithmeticException.class, new double[] {Double.MAX_VALUE}, 0, 1,
        new double[] {-Double.MAX_VALUE}, 0, 1, 1);
    invalid(ArithmeticException.class, new double[] {Double.MAX_VALUE / 2}, 0, 1,
        new double[] {0}, 0, 1, 1);
    double large = Math.scalb(1.0, 511);
    invalid(ArithmeticException.class, new double[] {large, large, large, large}, 0, 1,
        new double[4], 0, 1, 4);
    // Finding an exact match does not waive overflow checks for other selected codes.
    invalid(ArithmeticException.class, new double[] {0}, 0, 1,
        new double[] {0, Double.MAX_VALUE}, 0, 2, 1);
    // A later row may overflow after native scratch already contains a result.
    invalid(ArithmeticException.class, new double[] {0, Double.MAX_VALUE}, 0, 2,
        new double[] {0}, 0, 1, 1);
    expect(IllegalArgumentException.class, () -> NcmCodebook.nearest(
        new double[] {0}, 0, 1, new double[] {0}, 0, 1, 1, -1));
  }

  private static void exactOracleProperties() {
    Random random = new Random(0x4d334e434dL);
    for (int trial = 0; trial < 1_024; trial++) {
      int rows = random.nextInt(8);
      int dimensions = 1 + random.nextInt(7);
      int codes = rows == 0 ? random.nextInt(11) : 1 + random.nextInt(10);
      int vectorOffset = random.nextInt(4);
      int codeOffset = random.nextInt(4);
      double[] vectors = new double[vectorOffset + rows * dimensions + 2];
      double[] codebook = new double[codeOffset + codes * dimensions + 2];
      Arrays.fill(vectors, Double.NaN);
      Arrays.fill(codebook, Double.NaN);
      for (int i = 0; i < rows * dimensions; i++) vectors[vectorOffset + i] = (random.nextInt(129) - 64) / 8.0;
      for (int i = 0; i < codes * dimensions; i++) codebook[codeOffset + i] = (random.nextInt(129) - 64) / 8.0;
      if (codes > 1 && trial % 3 == 0) {
        System.arraycopy(codebook, codeOffset, codebook, codeOffset + dimensions, dimensions);
      }
      if (rows > 0 && codes > 0 && trial % 5 == 0) {
        System.arraycopy(codebook, codeOffset, vectors, vectorOffset, dimensions);
      }
      int[] expected = exactDyadicOracle(vectors, vectorOffset, rows, codebook, codeOffset, codes, dimensions);
      compare(vectors, vectorOffset, rows, codebook, codeOffset, codes, dimensions, expected);
    }
  }

  // Exact decimal arithmetic is independent of the binary64 implementation for this dyadic corpus.
  private static int[] exactDyadicOracle(double[] vectors, int offset, int rows,
      double[] codebook, int codeOffset, int codes, int dimensions) {
    int[] result = new int[rows];
    for (int row = 0; row < rows; row++) {
      BigDecimal least = null;
      for (int code = 0; code < codes; code++) {
        BigDecimal distance = BigDecimal.ZERO;
        for (int d = 0; d < dimensions; d++) {
          BigDecimal difference = BigDecimal.valueOf(vectors[offset + row * dimensions + d])
              .subtract(BigDecimal.valueOf(codebook[codeOffset + code * dimensions + d]));
          distance = distance.add(difference.multiply(difference));
        }
        if (least == null || distance.compareTo(least) < 0) {
          least = distance;
          result[row] = code;
        }
      }
    }
    return result;
  }

  private static void binary64ParityProperties() {
    Random random = new Random(0x425936344e434dL);
    for (int trial = 0; trial < 1_024; trial++) {
      int rows = 1 + random.nextInt(6);
      int dimensions = 1 + random.nextInt(11);
      int codes = 1 + random.nextInt(13);
      double[] vectors = new double[rows * dimensions];
      double[] codebook = new double[codes * dimensions];
      for (int i = 0; i < vectors.length; i++) vectors[i] = randomDouble(random);
      for (int i = 0; i < codebook.length; i++) codebook[i] = randomDouble(random);
      int[] expected = NcmCodebook.nearestJava(vectors, 0, rows, codebook, 0, codes, dimensions);
      compare(vectors, 0, rows, codebook, 0, codes, dimensions, expected);
    }
  }

  private static void nearTieParityProperties() {
    Random random = new Random(0x5449454e434dL);
    for (int trial = 0; trial < 1_024; trial++) {
      int dimensions = 2 + random.nextInt(32);
      int codes = 2 + random.nextInt(7);
      double[] vectors = new double[dimensions];
      double[] codebook = new double[codes * dimensions];
      for (int d = 0; d < dimensions; d++) {
        double value = Math.scalb(0.5 + random.nextDouble(), random.nextInt(101) - 50);
        for (int code = 0; code < codes; code++) codebook[code * dimensions + d] = value;
      }
      for (int code = 1; code < codes; code++) {
        int coordinate = code * dimensions + random.nextInt(dimensions);
        codebook[coordinate] = random.nextBoolean()
            ? Math.nextUp(codebook[coordinate]) : Math.nextDown(codebook[coordinate]);
      }
      int[] expected = NcmCodebook.nearestJava(vectors, 0, 1, codebook, 0, codes, dimensions);
      compare(vectors, 0, 1, codebook, 0, codes, dimensions, expected);
    }
  }

  private static double randomDouble(Random random) {
    return Math.scalb(random.nextDouble() * 2.0 - 1.0, random.nextInt(1_101) - 600);
  }

  private static void concurrentCalls() throws Exception {
    double[] vectors = {1, 2, 7, 8, -2, -3};
    double[] codebook = {0, 0, 1, 2, 7, 8, -2, -3};
    int[] expected = {1, 2, 3};
    List<Callable<Boolean>> calls = new ArrayList<>();
    for (int i = 0; i < 64; i++) {
      calls.add(() -> Arrays.equals(expected, NcmCodebook.nearest(vectors, 0, 3, codebook, 0, 4, 2, 0)));
    }
    try (var pool = Executors.newFixedThreadPool(4)) {
      for (var result : pool.invokeAll(calls)) check(result.get(), "concurrent call mismatch");
    }
  }

  private static void compare(double[] vectors, int vectorOffset, int rows,
      double[] codebook, int codeOffset, int codes, int dimensions, int[] expected) {
    double[] beforeVectors = vectors.clone();
    double[] beforeCodes = codebook.clone();
    check(Arrays.equals(expected, NcmCodebook.nearestJava(
        vectors, vectorOffset, rows, codebook, codeOffset, codes, dimensions)), "Java oracle mismatch");
    check(Arrays.equals(expected, NcmCodebook.nearest(
        vectors, vectorOffset, rows, codebook, codeOffset, codes, dimensions, 0)), "dispatch mismatch");
    check(Arrays.equals(expected, NcmCodebook.nearest(
        vectors, vectorOffset, rows, codebook, codeOffset, codes, dimensions, Integer.MAX_VALUE)), "threshold fallback mismatch");
    if (nativeMethod != null) {
      check(Arrays.equals(expected, rawNative(vectors, vectorOffset, rows, codebook, codeOffset, codes, dimensions)),
          "direct JNI mismatch");
    }
    check(Arrays.equals(vectors, beforeVectors) && Arrays.equals(codebook, beforeCodes), "input mutated");
  }

  private static void invalid(Class<? extends Throwable> expected, double[] vectors, int vectorOffset, int rows,
      double[] codebook, int codeOffset, int codes, int dimensions) {
    expect(expected, () -> NcmCodebook.nearestJava(vectors, vectorOffset, rows, codebook, codeOffset, codes, dimensions));
    expect(expected, () -> NcmCodebook.nearest(vectors, vectorOffset, rows, codebook, codeOffset, codes, dimensions, 0));
    if (nativeMethod != null) {
      expect(expected, () -> rawNative(vectors, vectorOffset, rows, codebook, codeOffset, codes, dimensions));
    }
  }

  private static int[] rawNative(double[] vectors, int vectorOffset, int rows,
      double[] codebook, int codeOffset, int codes, int dimensions) {
    try {
      return (int[]) nativeMethod.invoke(null, vectors, vectorOffset, rows, codebook, codeOffset, codes, dimensions);
    } catch (InvocationTargetException failure) {
      Throwable cause = failure.getCause();
      if (cause instanceof RuntimeException runtime) throw runtime;
      if (cause instanceof Error error) throw error;
      throw new AssertionError(cause);
    } catch (ReflectiveOperationException failure) {
      throw new AssertionError(failure);
    }
  }

  private static void expect(Class<? extends Throwable> type, Runnable action) {
    try {
      action.run();
    } catch (Throwable error) {
      check(error.getClass() == type, "expected " + type.getSimpleName() + ", got " + error);
      return;
    }
    throw new AssertionError("expected " + type.getSimpleName());
  }

  private static void check(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }
}
