// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.reference;

import jdk.m3.collections.M3ProgressMonitor;
import java.util.Objects;

/** Counted int-loop arithmetic. Facts describe the header, never effects of the body. */
public final class M3LoopFacts {
  private M3LoopFacts() {}

  public enum Relation { LT, LE, GT, GE }
  public enum Kind { EXACT_CONSTANT, EXACT_SYMBOLIC, UPPER_BOUND, CONDITIONAL, UNKNOWN }

  /** A negative count means unresolved, not zero. No fact authorizes object reclamation. */
  public record Count(Kind kind, long count, String reason) {
    public Count {
      Objects.requireNonNull(kind, "kind");
      Objects.requireNonNull(reason, "reason");
      if (count < -1 || (kind == Kind.EXACT_CONSTANT && count < 0)) {
        throw new IllegalArgumentException("invalid count");
      }
    }
  }

  /** Includes the final induction update: it too must not wrap a Java int. */
  public static Count count(int start, int limit, int step, Relation relation,
                            M3ProgressMonitor monitor) {
    Objects.requireNonNull(relation, "relation");
    if (monitor != null) monitor.checkCanceled();
    boolean enters = switch (relation) {
      case LT -> start < limit;
      case LE -> start <= limit;
      case GT -> start > limit;
      case GE -> start >= limit;
    };
    if (!enters) return new Count(Kind.EXACT_CONSTANT, 0, "initial condition is false");
    boolean ascending = relation == Relation.LT || relation == Relation.LE;
    if (step == 0 || ascending != (step > 0)) return unknown("non-progressing induction");
    long distance = ascending ? (long) limit - start : (long) start - limit;
    long stride = Math.abs((long) step);
    boolean inclusive = relation == Relation.LE || relation == Relation.GE;
    long iterations = inclusive ? distance / stride + 1 : (distance + stride - 1) / stride;
    // All inputs are int: this multiplication/addition fits in a signed long.
    long exit = start + iterations * step;
    if (exit < Integer.MIN_VALUE || exit > Integer.MAX_VALUE) {
      return unknown("final induction update may wrap");
    }
    return new Count(Kind.EXACT_CONSTANT, iterations, "monotone int induction without wrap");
  }

  private static Count unknown(String reason) { return new Count(Kind.UNKNOWN, -1, reason); }

  /** Four int lanes per loop: start, limit, step, Relation.ordinal(). Java is the default. */
  public static long[] batch(int[] encoded, M3ProgressMonitor monitor) {
    Objects.requireNonNull(encoded, "encoded");
    if (monitor != null) monitor.checkCanceled();
    if (encoded.length % 4 != 0) throw new IllegalArgumentException("four lanes per loop required");
    long[] result = new long[encoded.length / 4];
    Relation[] relations = Relation.values();
    for (int i = 0; i < result.length; i++) {
      int p = i * 4;
      int ordinal = encoded[p + 3];
      if (ordinal < 0 || ordinal >= relations.length) throw new IllegalArgumentException("relation");
      result[i] = count(encoded[p], encoded[p + 1], encoded[p + 2], relations[ordinal], monitor).count();
    }
    return result;
  }

  /** Explicit, opt-in JNI. The caller loads its reviewed library; there is no silent fallback. */
  public static long[] nativeBatch(int[] encoded, M3ProgressMonitor monitor) {
    Objects.requireNonNull(encoded, "encoded");
    if (monitor != null) monitor.checkCanceled();
    if (encoded.length % 4 != 0) throw new IllegalArgumentException("four lanes per loop required");
    if (monitor != null) monitor.checkCanceled();
    if (abi0() != 1) throw new LinkageError("M3 loop facts JNI ABI mismatch");
    long[] result = new long[encoded.length / 4];
    // Bound JNI residency and cancellation latency instead of retaining pinned Java arrays.
    final int chunk = 4096;
    for (int p = 0; p < encoded.length; p += Math.min(chunk, encoded.length - p)) {
      if (monitor != null) monitor.checkCanceled();
      int size = Math.min(chunk, encoded.length - p);
      batch0(encoded, p, size, result, p / 4);
    }
    if (monitor != null) monitor.checkCanceled();
    return result;
  }

  private static native int abi0();
  private static native void batch0(int[] encoded, int offset, int length, long[] output, int outputOffset);
}
