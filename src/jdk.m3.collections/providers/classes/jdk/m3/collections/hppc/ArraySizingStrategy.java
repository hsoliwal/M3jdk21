/*
 * HPPC
 *
 * Copyright (C) 2010-2026 Carrot Search s.c. and contributors
 * All rights reserved.
 *
 * Refer to the full license file "LICENSE.txt":
 * https://github.com/carrotsearch/hppc/blob/master/LICENSE.txt
 *
 * Modified 2026 by Synexia contributors: @FunctionalInterface, Accountable defaults (grow is the
 * only abstract method) and factories for the array-list growth policies of jdk.m3.collections.
 */
package jdk.m3.collections.hppc;

/**
 * Resizing (growth) strategy for array-backed buffers.
 *
 * <p>The growth-policy owner of the collection libraries carried in {@code jdk.m3.collections}.
 * {@link #grow} is the only abstract method, so a lambda is a strategy, and the static factories
 * are a superset of the array-list growth policies of the module's libraries: {@link #hppc()},
 * {@link #arrayList()}, {@link #fastList()}, {@link #immutableCollectionBuilder()} and {@link
 * #gapList()}. Each factory reproduces its library's arithmetic for a buffer of length {@code
 * currentBufferLength} that must hold {@code elementsCount + expectedAdditions} elements, as a
 * container created with that explicit capacity grows, {@code currentBufferLength == 0} included.
 *
 * <p>Every factory throws {@link BufferAllocationException} when {@code elementsCount +
 * expectedAdditions} exceeds {@code Integer.MAX_VALUE}; {@link #hppc()} throws it already above
 * {@link BoundedProportionalArraySizingStrategy#MAX_ARRAY_LENGTH}. The other four return lengths
 * above the JDK's soft maximum array length, {@code Integer.MAX_VALUE - 8}, as their library
 * computes them; allocating such a length may fail with {@link OutOfMemoryError}.
 */
@FunctionalInterface
public interface ArraySizingStrategy extends Accountable {
  /**
   * @param currentBufferLength Current size of the array (buffer). This number should comply with
   *     the strategy's policies (it is a result of initial rounding or further growCalls). It can
   *     also be zero, indicating the growth from an empty buffer.
   * @param elementsCount Number of elements stored in the buffer.
   * @param expectedAdditions Expected number of additions (resize hint).
   * @return Must return a new size at least as big as to hold <code>
   *     elementsCount + expectedAdditions</code>.
   * @throws BufferAllocationException If the sizing strategy cannot grow the buffer (for example
   *     due to constraints or memory limits).
   */
  int grow(int currentBufferLength, int elementsCount, int expectedAdditions)
      throws BufferAllocationException;

  /**
   * Allocated memory of the strategy: the object header; a strategy that holds state (including a
   * capturing lambda) is under-counted unless written as a class that overrides this method.
   *
   * @return Ram allocated in bytes
   */
  @Override
  default long ramBytesAllocated() {
    return RamUsageEstimator.NUM_BYTES_OBJECT_HEADER;
  }

  /**
   * Memory used by the strategy: what {@link #ramBytesAllocated()} returns.
   *
   * @return Ram used in bytes
   */
  @Override
  default long ramBytesUsed() {
    return ramBytesAllocated();
  }

  /**
   * HPPC's policy, {@link BoundedProportionalArraySizingStrategy#DEFAULT_INSTANCE}, the default of
   * HPPC's containers: the current length plus half of it (computed in {@code float}), at least
   * 10 more and at most {@link BoundedProportionalArraySizingStrategy#MAX_ARRAY_LENGTH}, or what is
   * needed if that is more; refused when what is needed exceeds {@code MAX_ARRAY_LENGTH}.
   *
   * @return the strategy
   */
  static ArraySizingStrategy hppc() {
    return BoundedProportionalArraySizingStrategy.DEFAULT_INSTANCE;
  }

  /**
   * The policy of a {@code java.util.ArrayList} created with an explicit capacity, as in {@code
   * new ArrayList<>(currentBufferLength)}: {@code jdk.internal.util.ArraysSupport.newLength(
   * currentBufferLength, needed - currentBufferLength, currentBufferLength >> 1)}, that is the
   * current length plus the larger of half of it and what is missing; when that overflows or
   * exceeds {@code Integer.MAX_VALUE - 8}, the larger of {@code Integer.MAX_VALUE - 8} and what is
   * needed. From an empty buffer it is what is needed. {@code new ArrayList<>()} allocates 10 on
   * its first add instead and grows by this policy from there.
   *
   * @return the strategy
   */
  static ArraySizingStrategy arrayList() {
    return ArraySizingStrategies.ARRAY_LIST;
  }

  /**
   * The policy of an Eclipse Collections {@code FastList} created with an explicit capacity, as in
   * {@code new FastList<>(currentBufferLength)}, from {@code FastList.ensureCapacity} and {@code
   * sizePlusFiftyPercent}: the larger of the current length plus half of it plus one and what is
   * needed. When that sum overflows, {@code Integer.MAX_VALUE - 8} replaces it; it is not a cap,
   * a sum above it that does not overflow is used as it is. {@code new FastList<>()} allocates 10
   * on its first add instead and grows by this policy from there.
   *
   * @return the strategy
   */
  static ArraySizingStrategy fastList() {
    return ArraySizingStrategies.FAST_LIST;
  }

  /**
   * The policy of a Guava {@code ImmutableList.Builder} created with an explicit capacity, as in
   * {@code ImmutableList.builderWithExpectedSize(currentBufferLength)}, from {@code
   * ImmutableCollection.Builder.expandedCapacity}: the current length plus half of it plus one,
   * or, if that is not enough, the power of two at or above what is needed; {@code
   * Integer.MAX_VALUE} when that overflows. {@code ImmutableList.builder()} allocates 4 when it is
   * created instead and grows by this policy from there.
   *
   * @return the strategy
   */
  static ArraySizingStrategy immutableCollectionBuilder() {
    return ArraySizingStrategies.IMMUTABLE_COLLECTION_BUILDER;
  }

  /**
   * The policy of a Brownies Collections {@code GapList}, one created with an explicit capacity,
   * as in {@code new GapList<>(currentBufferLength)}, included, from {@code
   * CapacityHelper.calculateCapacity(needed, currentBufferLength, GapList.DEFAULT_CAPACITY)}: the
   * current length plus half of it, or what is needed, but at least 10, if that is more or the sum
   * overflows. {@code GapList} applies its default capacity 10 always, as {@code CapacityHelper}
   * does, so {@code new GapList<>()} and {@code new GapList<>(0)} both grow to 10 on their first
   * add.
   *
   * @return the strategy
   */
  static ArraySizingStrategy gapList() {
    return ArraySizingStrategies.GAP_LIST;
  }
}
