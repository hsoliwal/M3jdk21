// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/**
 * Shared JVM-heap admission atom for M3 resident metadata growth.
 *
 * <p>This class is deliberately small: it observes effective {@code -Xmx}, current committed/used
 * heap and a caller-supplied full allocation peak. It never calls {@code System.gc()}, never owns a
 * cache, never allocates native memory and never changes logical collection semantics. Under
 * pressure it only removes speculative spare capacity; the requested minimum remains authoritative.
 * Native/JNI payload owners keep their own exact byte-budget checks.</p>
 */
public final class MIndexHeapAdmission {
  public enum Level {
    NORMAL,
    SOFT,
    HARD
  }

  public record Policy(int warmPercent, int highPercent, long reserveBytes) {
    public static final Policy DEFAULT = new Policy(70, 85, 0L);

    public Policy {
      if (warmPercent < 1 || warmPercent >= highPercent || highPercent > 100
          || reserveBytes < 0L) {
        throw new IllegalArgumentException("invalid heap admission policy");
      }
    }

    long effectiveReserve(long maximumBytes) {
      if (maximumBytes <= 0L) return reserveBytes;
      return Math.max(reserveBytes, Math.min(8L << 20, maximumBytes >>> 5));
    }
  }

  /** Detached observation; values are estimates, never an allocation or GC guarantee. */
  public record Snapshot(
      long maximumBytes,
      long committedBytes,
      long usedBytes,
      long availableToMaximumBytes,
      long additionalPeakBytes,
      long reserveBytes,
      long projectedUsedBytes,
      Level level) {
    public Snapshot {
      if (maximumBytes <= 0L || committedBytes < 0L || usedBytes < 0L
          || usedBytes > committedBytes || additionalPeakBytes < 0L || reserveBytes < 0L
          || projectedUsedBytes < usedBytes || projectedUsedBytes > maximumBytes
          || availableToMaximumBytes != maximumBytes - usedBytes) {
        throw new IllegalArgumentException("invalid heap admission snapshot");
      }
      Objects.requireNonNull(level, "level");
    }

    public boolean pressure() {
      return level != Level.NORMAL;
    }
  }

  private MIndexHeapAdmission() {}

  public static Snapshot current(long additionalPeakBytes) {
    return current(additionalPeakBytes, Policy.DEFAULT);
  }

  public static Snapshot current(long additionalPeakBytes, Policy policy) {
    if (additionalPeakBytes < 0L) {
      throw new IllegalArgumentException("negative allocation estimate");
    }
    Objects.requireNonNull(policy, "policy");
    Runtime runtime = Runtime.getRuntime();
    long maximum = runtime.maxMemory();
    long committed = runtime.totalMemory();
    long used = Math.max(0L, committed - runtime.freeMemory());
    if (maximum <= 0L) {
      maximum = committed > 0L ? committed : Long.MAX_VALUE;
    }
    if (committed > maximum) committed = maximum;
    if (used > committed) used = committed;
    return snapshot(used, committed, maximum, additionalPeakBytes, policy);
  }

  /**
   * Choose resident-array capacity without caller-visible capacity choreography.
   *
   * <p>{@code geometricCapacity} is the existing healthy-heap policy. Under pressure the result
   * approaches {@code minimumCapacity}; it never falls below that minimum and never exceeds the
   * existing geometric/max bounds.</p>
   */
  public static int growthCapacity(
      int currentCapacity,
      int minimumCapacity,
      int geometricCapacity,
      int maximumCapacity,
      int bytesPerSlot,
      long headerBytes) {
    Snapshot snapshot = current(0L);
    return growthCapacity(
        currentCapacity,
        minimumCapacity,
        geometricCapacity,
        maximumCapacity,
        bytesPerSlot,
        headerBytes,
        snapshot.usedBytes(),
        snapshot.maximumBytes(),
        Policy.DEFAULT);
  }

  /** Pure deterministic form for tests and compiler/recipe proofs. */
  public static int growthCapacity(
      int currentCapacity,
      int minimumCapacity,
      int geometricCapacity,
      int maximumCapacity,
      int bytesPerSlot,
      long headerBytes,
      long usedBytes,
      long maximumBytes,
      Policy policy) {
    Objects.requireNonNull(policy, "policy");
    if (currentCapacity < 0 || minimumCapacity < 0 || geometricCapacity < minimumCapacity
        || maximumCapacity < minimumCapacity || geometricCapacity > maximumCapacity
        || bytesPerSlot < 1 || headerBytes < 0L || usedBytes < 0L || maximumBytes <= 0L
        || usedBytes > maximumBytes) {
      throw new IllegalArgumentException("invalid array growth admission");
    }
    if (minimumCapacity <= currentCapacity) return currentCapacity;

    long minimumPeak = arrayBytes(minimumCapacity, bytesPerSlot, headerBytes);
    long geometricPeak = arrayBytes(geometricCapacity, bytesPerSlot, headerBytes);
    long reserve = policy.effectiveReserve(maximumBytes);
    long available = maximumBytes - usedBytes;
    if (available <= reserve || minimumPeak > available - reserve) {
      return minimumCapacity;
    }

    long projectedGeometric = addWithin(maximumBytes, usedBytes, geometricPeak);
    if (projectedGeometric < fraction(maximumBytes, policy.warmPercent())
        && geometricPeak <= available - reserve) {
      return geometricCapacity;
    }

    int shift =
        projectedGeometric >= fraction(maximumBytes, policy.highPercent()) ? 5 : 3;
    long increment = Math.max(8L, (long) currentCapacity >>> shift);
    long boundedLong =
        Math.min(
            (long) geometricCapacity,
            Math.max((long) minimumCapacity, (long) currentCapacity + increment));
    int bounded = (int) Math.min(maximumCapacity, boundedLong);
    return arrayBytes(bounded, bytesPerSlot, headerBytes) <= available - reserve
        ? bounded
        : minimumCapacity;
  }

  public static long arrayBytes(int capacity, int bytesPerSlot, long headerBytes) {
    if (capacity < 0 || bytesPerSlot < 1 || headerBytes < 0L) {
      throw new IllegalArgumentException("invalid array byte estimate");
    }
    return addSaturated(headerBytes, multiplySaturated(capacity, bytesPerSlot));
  }

  /** Pure snapshot form for deterministic tests and JNI/telemetry bridges. */
  public static Snapshot snapshot(
      long usedBytes,
      long committedBytes,
      long maximumBytes,
      long additionalPeakBytes,
      Policy policy) {
    Objects.requireNonNull(policy, "policy");
    if (usedBytes < 0L || committedBytes < 0L || maximumBytes <= 0L
        || usedBytes > committedBytes || committedBytes > maximumBytes
        || additionalPeakBytes < 0L) {
      throw new IllegalArgumentException("invalid heap observation");
    }
    long reserve = policy.effectiveReserve(maximumBytes);
    long projected =
        Math.min(
            maximumBytes,
            addSaturated(
                addSaturated(usedBytes, additionalPeakBytes),
                reserve));
    Level level =
        projected >= fraction(maximumBytes, policy.highPercent())
            ? Level.HARD
            : projected >= fraction(maximumBytes, policy.warmPercent())
                ? Level.SOFT
                : Level.NORMAL;
    return new Snapshot(
        maximumBytes,
        committedBytes,
        usedBytes,
        maximumBytes - usedBytes,
        additionalPeakBytes,
        reserve,
        projected,
        level);
  }

  private static long fraction(long amount, int percent) {
    return (amount / 100L) * percent + ((amount % 100L) * percent) / 100L;
  }

  private static long addWithin(long limit, long left, long right) {
    if (left >= limit || right >= limit - left) return limit;
    return left + right;
  }

  private static long addSaturated(long left, long right) {
    return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
  }

  private static long multiplySaturated(long left, long right) {
    if (left == 0L || right == 0L) return 0L;
    return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
  }
}
