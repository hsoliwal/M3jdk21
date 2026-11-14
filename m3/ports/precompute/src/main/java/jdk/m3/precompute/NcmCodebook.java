// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Objects;

/**
 * Checked nearest-code assignment over caller-owned row-major numeric batches.
 *
 * <p>This implements the squared-Euclidean codebook selection used in vector
 * quantization. It is a numerical support operation, not an NCM encoder, trained
 * model, semantic identity, or lossless text representation. Returned indices are
 * local to the supplied codebook; its checkpoint and hierarchy-level identity
 * belong to the existing owner of the derived result.</p>
 *
 * <p>No input array or text payload is retained. Callers must not mutate either
 * input while an operation is in progress. Both inputs may be the same array.
 * The returned array is a fresh caller-owned result. All selected input elements
 * must be finite; elements outside the selected ranges are not inspected.</p>
 */
public final class NcmCodebook {
  private NcmCodebook() {}

  /** Uses the Java oracle; native execution requires the explicit-threshold overload. */
  public static int[] nearest(
      double[] vectors, int vectorOffset, int rows,
      double[] codebook, int codebookOffset, int codeCount, int dimensions) {
    return nearestJava(vectors, vectorOffset, rows, codebook, codebookOffset,
        codeCount, dimensions);
  }

  /**
   * Assigns each vector to its nearest code, with the lowest code index winning ties.
   *
   * <p>Offsets count double elements. Vector and code rows have exactly
   * {@code dimensions} consecutive elements. A zero-row input is permitted,
   * including an empty codebook, after validating dimensions and both declared
   * ranges. Nonempty input requires at least one code. This method uses the Java
   * oracle when the existing library lacks the codebook feature or the batch is
   * below the nonnegative native threshold.</p>
   *
   * <p>Each squared distance is evaluated in dimension order using separate
   * binary64 subtraction, multiplication, and addition. Any nonfinite distance
   * raises {@link ArithmeticException}, even if another code would be nearer.
   * This avoids silently treating overflowed distances as equal. Floating-point
   * underflow follows Java's ordinary binary64 semantics.</p>
   *
   * @throws NullPointerException if either input is null
   * @throws IllegalArgumentException for invalid counts, dimensions, threshold,
   *     or a nonfinite element in a selected input range
   * @throws IndexOutOfBoundsException if either selected range is outside its array
   * @throws ArithmeticException if any squared distance overflows
   */
  public static int[] nearest(
      double[] vectors, int vectorOffset, int rows,
      double[] codebook, int codebookOffset, int codeCount, int dimensions,
      int nativeThreshold) {
    return NativePrecomputeOps.nearestNcmCodes(vectors, vectorOffset, rows,
        codebook, codebookOffset, codeCount, dimensions, nativeThreshold);
  }

  /** Java semantic oracle with the same validation, tie, and overflow contract. */
  public static int[] nearestJava(
      double[] vectors, int vectorOffset, int rows,
      double[] codebook, int codebookOffset, int codeCount, int dimensions) {
    validate(vectors, vectorOffset, rows, codebook, codebookOffset, codeCount, dimensions);
    return nearestValidated(vectors, vectorOffset, rows,
        codebook, codebookOffset, codeCount, dimensions);
  }

  static void validate(
      double[] vectors, int vectorOffset, int rows,
      double[] codebook, int codebookOffset, int codeCount, int dimensions) {
    Objects.requireNonNull(vectors, "vectors");
    Objects.requireNonNull(codebook, "codebook");
    if (rows < 0 || codeCount < 0 || dimensions <= 0 || (rows > 0 && codeCount == 0)) {
      throw new IllegalArgumentException("invalid codebook batch shape");
    }
    int vectorEnd = checkedEnd(vectors.length, vectorOffset, rows, dimensions);
    int codebookEnd = checkedEnd(codebook.length, codebookOffset, codeCount, dimensions);
    finiteRange(vectors, vectorOffset, vectorEnd);
    finiteRange(codebook, codebookOffset, codebookEnd);
  }

  private static int checkedEnd(int length, int offset, int rows, int dimensions) {
    long span = (long) rows * dimensions;
    if (offset < 0 || offset > length || span > length - offset) {
      throw new IndexOutOfBoundsException("codebook batch range outside array");
    }
    return offset + (int) span;
  }

  private static void finiteRange(double[] values, int start, int end) {
    for (int i = start; i < end; i++) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException("nonfinite codebook batch input");
      }
    }
  }

  // Package-private only for the existing NativePrecomputeOps dispatcher after validation.
  static int[] nearestValidated(
      double[] vectors, int vectorOffset, int rows,
      double[] codebook, int codebookOffset, int codeCount, int dimensions) {
    int[] result = new int[rows];
    int vectorBase = vectorOffset;
    for (int row = 0; row < rows; row++, vectorBase += dimensions) {
      int bestCode = 0;
      double bestDistance = Double.POSITIVE_INFINITY;
      int codeBase = codebookOffset;
      for (int code = 0; code < codeCount; code++, codeBase += dimensions) {
        double distance = 0.0;
        for (int dimension = 0; dimension < dimensions; dimension++) {
          double difference = vectors[vectorBase + dimension] - codebook[codeBase + dimension];
          double square = difference * difference;
          distance = distance + square;
        }
        if (!Double.isFinite(distance)) {
          throw new ArithmeticException("squared codebook distance overflow");
        }
        if (distance < bestDistance) {
          bestDistance = distance;
          bestCode = code;
        }
      }
      result[row] = bestCode;
    }
    return result;
  }
}
