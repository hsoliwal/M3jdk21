// M3JDK21 adaptation: package/type/progress mapping only; Synexia remains the source owner.
// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package jdk.internal.mindex;

import java.util.Objects;

/**
 * Bounded Boolean full-match effects for already compiled dot/star atoms. This is not a parser.
 * Reuses M3StateRelation's ordered algebra. The access adapter borrows an existing immutable
 * owner/range DAG; this class never interns, flattens, persists, or retains text after evaluate.
 * Captures, flags, assertions, searching and surrogate input require the caller's exact fallback.
 */
public final class M3DotStarRelations {
  private static final int STAR = 1 << 16;
  public static final int UNKNOWN = -1;

  private M3DotStarRelations() { }

  /** Identity/coordinate pairs must name immutable content, including range and mask semantics. */
  public interface Access<T> {
    Object owner(T value);
    long coordinate(T value);
    int length(T value);
    int codeUnitAt(T value, int index);
    /** Null for a leaf, otherwise the first visible child. */
    T first(T value);
    /** Null for a leaf/unary view, otherwise the second visible child, in occurrence order. */
    T second(T value);
  }

  /** Optional explicitly selected numeric provider. No source or native pointer crosses this API. */
  @FunctionalInterface
  public interface Join {
    long[] compose(long[] left, long[] right);
  }

  /** Counters measure work in this invocation, not elapsed time, RSS or universal speedup. */
  public record Result(int decision, long codeUnitReads, int summaries, int compositions,
                       int reusedSummaries) { }

  /**
   * No lifecycle is owned for checkpoint. With null, even an interrupted thread keeps ordinary
   * String matching behavior. Infrastructure supplies budgets; ordinary String callers do not.
   * Compiled atoms use low 16 bits for ASCII alphanumeric/dot and bit 16 for star.
   * Refusal returns UNKNOWN, never a negative regex answer. All inputs must remain stable.
   */
  public static <T> Result evaluate(T source, Access<T> access, int[] atoms, int atomCount,
      int maxNodes, long maxWork, Join nativeJoin, Runnable checkpoint) {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(access, "access");
    Objects.requireNonNull(atoms, "atoms");
    if (atomCount < 0 || atomCount > atoms.length || maxNodes < 1 || maxNodes > 4096
        || maxWork < 0) throw new IllegalArgumentException("invalid relation admission bounds");
    if (atomCount >= Long.SIZE) return new Result(UNKNOWN, 0, 0, 0, 0);
    for (int i = 0; i < atomCount; i++) {
      int a = atoms[i];
      int unit = a & 0xffff;
      if ((a & ~(STAR | 0xffff)) != 0 || !(unit == '.' || unit >= 'a' && unit <= 'z'
          || unit >= 'A' && unit <= 'Z' || unit >= '0' && unit <= '9')) {
        throw new IllegalArgumentException("unadmitted compiled regex atom");
      }
    }
    Engine<T> engine = new Engine<>(access, atoms, atomCount, maxNodes, maxWork,
        nativeJoin, checkpoint);
    long[] effect = engine.fold(source, 0);
    int decision = effect == null ? UNKNOWN : (effect[0] & (1L << atomCount)) != 0 ? 1 : 0;
    engine.check();
    return new Result(decision, engine.reads, engine.used, engine.joins, engine.hits);
  }

  private static final class Engine<T> {
    private final Access<T> access;
    private final int[] atoms;
    private final int count;
    private final int states;
    private final long starMask;
    private final long[] closure;
    private final Object[] owners;
    private final long[] coordinates;
    private final int[] lengths;
    private final long[][] effects;
    private final Join nativeJoin;
    private final Runnable checkpoint;
    private long remaining;
    private long reads;
    private int used;
    private int joins;
    private int hits;

    Engine(Access<T> access, int[] atoms, int count, int nodes, long work,
        Join nativeJoin, Runnable checkpoint) {
      this.access = access;
      this.atoms = atoms;
      this.count = count;
      states = count + 1;
      owners = new Object[nodes];
      coordinates = new long[nodes];
      lengths = new int[nodes];
      effects = new long[nodes][];
      this.nativeJoin = nativeJoin;
      this.checkpoint = checkpoint;
      remaining = work;
      closure = new long[states];
      long stars = 0;
      closure[count] = 1L << count;
      for (int i = count - 1; i >= 0; i--) {
        closure[i] = 1L << i;
        if ((atoms[i] & STAR) != 0) {
          stars |= 1L << i;
          closure[i] |= closure[i + 1];
        }
      }
      starMask = stars;
    }

    private void check() { if (checkpoint != null) checkpoint.run(); }

    private boolean charge(long work) {
      if (work > remaining) return false;
      remaining -= work;
      return true;
    }

    private long[] fold(T value, int depth) {
      check();
      if (depth > 64) return null;
      int length = access.length(value);
      if (length < 0) throw new IllegalArgumentException("negative visible length");
      Object owner = Objects.requireNonNull(access.owner(value), "owner");
      long coordinate = access.coordinate(value);
      for (int i = 0; i < used; i++) {
        if (owners[i] == owner && coordinates[i] == coordinate) {
          if (lengths[i] != length) throw new IllegalArgumentException("inconsistent owner coordinate");
          hits++;
          return effects[i];
        }
      }
      if (used == owners.length) return null;
      T first = access.first(value);
      T second = access.second(value);
      long[] result;
      if (first == null) {
        if (second != null) throw new IllegalArgumentException("second child without first");
        result = leaf(value, length);
      } else {
        int firstLength = access.length(first);
        int secondLength = second == null ? 0 : access.length(second);
        if (firstLength < 0 || secondLength < 0
            || (long) firstLength + secondLength != length) {
          throw new IllegalArgumentException("visible child geometry mismatch");
        }
        long[] left = fold(first, depth + 1);
        if (left == null) return null;
        if (second == null) result = left;
        else {
          long[] right = fold(second, depth + 1);
          if (right == null || !charge((long) states * states)) return null;
          if (nativeJoin == null) {
            result = new long[states];
            M3StateRelation.compose(left, 0, right, 0, result, 0, states);
          } else {
            // A provider must not be able to mutate cached child effects through borrowed arrays.
            result = nativeJoin.compose(left.clone(), right.clone());
            check();
            long mask = states == 64 ? -1L : (1L << states) - 1;
            if (result == null || result.length != states) {
              throw new IllegalStateException("invalid native relation shape");
            }
            result = result.clone();
            for (long row : result) {
              if ((row & ~mask) != 0) throw new IllegalStateException("foreign native state bits");
            }
          }
          joins++;
        }
      }
      if (result == null || used == owners.length) return null;
      owners[used] = owner;
      coordinates[used] = coordinate;
      lengths[used] = length;
      effects[used++] = result;
      return result;
    }

    private long[] leaf(T value, int length) {
      // q^2+q bounds frontier-bit visits and atom comparisons per source unit.
      if (!charge((long) length * ((long) states * states + states))) return null;
      long[] rows = closure.clone();
      for (int index = 0; index < length; index++) {
        check();
        int unit = access.codeUnitAt(value, index);
        reads++;
        if (unit < 0 || unit > 0xffff) throw new IllegalArgumentException("invalid UTF-16 unit");
        if (Character.isSurrogate((char) unit)) return null;
        long matching = 0;
        for (int i = 0; i < count; i++) {
          int required = atoms[i] & 0xffff;
          boolean accepts = required == '.' ? !lineTerminator(unit) : required == unit;
          if (accepts) matching |= 1L << i;
        }
        for (int entry = 0; entry < states; entry++) {
          long matched = rows[entry] & matching;
          long moved = (matched & starMask) | ((matched & ~starMask) << 1);
          long next = 0;
          while (moved != 0) {
            next |= closure[Long.numberOfTrailingZeros(moved)];
            moved &= moved - 1;
          }
          rows[entry] = next;
        }
      }
      return rows;
    }
  }

  private static boolean lineTerminator(int unit) {
    return unit == '\n' || unit == '\r' || unit == 0x85 || unit == 0x2028 || unit == 0x2029;
  }
}
