/*
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.hppc;

/**
 * The array-list growth policies of the module's collection libraries, returned by the factories
 * of {@link ArraySizingStrategy}. Each constant re-expresses its library's arithmetic for a buffer
 * of length {@code currentBufferLength} that must hold {@code elementsCount + expectedAdditions}
 * elements; as enum constants they are serializable singletons.
 */
enum ArraySizingStrategies implements ArraySizingStrategy {

  /**
   * {@code ArrayList.grow(minCapacity)} with {@code ArraysSupport.newLength}, for every current
   * length.
   */
  ARRAY_LIST {
    @Override
    public int grow(int currentBufferLength, int elementsCount, int expectedAdditions) {
      int target = target(currentBufferLength, elementsCount, expectedAdditions);
      int half = currentBufferLength >> 1;
      int grown = currentBufferLength + Math.max(target - currentBufferLength, half);
      if (0 < grown && grown <= SOFT_MAX_ARRAY_LENGTH) {
        return grown;
      }
      return Math.max(target, SOFT_MAX_ARRAY_LENGTH);
    }

    @Override
    public String toString() {
      return "ArraySizingStrategy.arrayList()";
    }
  },

  /** {@code FastList.ensureCapacity(minCapacity)} with {@code FastList.sizePlusFiftyPercent}. */
  FAST_LIST {
    @Override
    public int grow(int currentBufferLength, int elementsCount, int expectedAdditions) {
      int target = target(currentBufferLength, elementsCount, expectedAdditions);
      int grown = currentBufferLength + (currentBufferLength >> 1) + 1;
      if (grown < currentBufferLength) {
        grown = SOFT_MAX_ARRAY_LENGTH;
      }
      return Math.max(grown, target);
    }

    @Override
    public String toString() {
      return "ArraySizingStrategy.fastList()";
    }
  },

  /** {@code ImmutableCollection.Builder.expandedCapacity(oldCapacity, minCapacity)}. */
  IMMUTABLE_COLLECTION_BUILDER {
    @Override
    public int grow(int currentBufferLength, int elementsCount, int expectedAdditions) {
      int target = target(currentBufferLength, elementsCount, expectedAdditions);
      if (target <= currentBufferLength) {
        return currentBufferLength;
      }
      int grown = currentBufferLength + (currentBufferLength >> 1) + 1;
      if (grown < target) {
        grown = Integer.highestOneBit(target - 1) << 1;
      }
      return grown < 0 ? Integer.MAX_VALUE : grown;
    }

    @Override
    public String toString() {
      return "ArraySizingStrategy.immutableCollectionBuilder()";
    }
  },

  /** {@code CapacityHelper.calculateCapacity(minCapacity, oldCapacity, 10)} of {@code GapList}. */
  GAP_LIST {
    @Override
    public int grow(int currentBufferLength, int elementsCount, int expectedAdditions) {
      int target = target(currentBufferLength, elementsCount, expectedAdditions);
      if (target <= currentBufferLength) {
        return currentBufferLength;
      }
      int floor = Math.max(target, GAP_LIST_DEFAULT_CAPACITY);
      int grown = currentBufferLength + (currentBufferLength >> 1);
      return grown < floor ? floor : grown;
    }

    @Override
    public String toString() {
      return "ArraySizingStrategy.gapList()";
    }
  };

  /**
   * The JDK's soft maximum array length, {@code Integer.MAX_VALUE - 8}, which is also {@code
   * FastList.MAXIMUM_ARRAY_SIZE}.
   */
  private static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

  /** {@code GapList.DEFAULT_CAPACITY} of Brownies Collections. */
  private static final int GAP_LIST_DEFAULT_CAPACITY = 10;

  /**
   * The object header and the fields every enum constant holds: {@code java.lang.Enum}'s {@code
   * name}, {@code ordinal} and {@code hash}.
   *
   * @return Ram allocated in bytes
   */
  @Override
  public long ramBytesAllocated() {
    return RamUsageEstimator.NUM_BYTES_OBJECT_HEADER
        + RamUsageEstimator.NUM_BYTES_OBJECT_REF
        + Integer.BYTES * 2;
  }

  /** {@code elementsCount + expectedAdditions}, refused when it is not an array length. */
  private static int target(int currentBufferLength, int elementsCount, int expectedAdditions) {
    long sum = (long) elementsCount + expectedAdditions;
    if (sum > Integer.MAX_VALUE) {
      throw new BufferAllocationException(
          "Java array size exceeded (current length: %d, elements: %d, expected additions: %d)",
          currentBufferLength, elementsCount, expectedAdditions);
    }
    return (int) sum;
  }
}
