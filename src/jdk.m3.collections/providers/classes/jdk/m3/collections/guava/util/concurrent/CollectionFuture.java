/*
 * Copyright (C) 2006 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package jdk.m3.collections.guava.util.concurrent;

import static jdk.m3.collections.guava.collect.Lists.newArrayListWithCapacity;
import static java.util.Collections.unmodifiableList;

import jdk.m3.collections.guava.annotations.GwtCompatible;
import jdk.m3.collections.guava.collect.ImmutableCollection;
import java.util.Arrays;
import java.util.List;

/** Aggregate future that collects (stores) results of each future. */
@GwtCompatible
abstract class CollectionFuture<V extends Object, C extends Object>
    extends AggregateFuture<V, C> {
  /*
   * We access this field racily but safely. For discussion of a similar situation, see the comments
   * on the fields of TimeoutFuture. This field is slightly different from the fields discussed
   * there: cancel() never reads this field, only writes to it. That makes the race here completely
   * harmless, rather than just 99.99% harmless.
   */
  private Object[] values;

  /** An input that has not delivered a value; a delivered null is stored as null. Never escapes. */
  //FLLDCIM: LEETCODIFY - EMPTY sentinel slots replace the Present wrapper per collected value
  private static final Object EMPTY = new Object();

  CollectionFuture(
      ImmutableCollection<? extends ListenableFuture<? extends V>> futures,
      boolean allMustSucceed) {
    super(futures, allMustSucceed, true);

    Object[] values = new Object[futures.size()];
    Arrays.fill(values, EMPTY);
    this.values = values;
  }

  @Override
  final void collectOneValue(int index, @ParametricNullness V returnValue) {
    Object[] localValues = values;
    if (localValues != null) {
      localValues[index] = returnValue;
    }
  }

  @Override
  final void handleAllCompleted() {
    Object[] localValues = values;
    if (localValues != null) {
      set(combine(localValues));
    }
  }

  @Override
  void releaseResources(ReleaseResourcesReason reason) {
    super.releaseResources(reason);
    this.values = null;
  }

  /** Combines the slots; a slot still holding {@link #isEmptySlot EMPTY} had no value. */
  abstract C combine(Object[] values);

  static boolean isEmptySlot(Object slot) {
    return slot == EMPTY;
  }

  /** Used for {@link Futures#allAsList} and {@link Futures#successfulAsList}. */
  static final class ListFuture<V extends Object>
      extends CollectionFuture<V, List<V>> {
    ListFuture(
        ImmutableCollection<? extends ListenableFuture<? extends V>> futures,
        boolean allMustSucceed) {
      super(futures, allMustSucceed);
      init();
    }

    @Override
    @SuppressWarnings("unchecked") // each non-EMPTY slot holds the V its future delivered
    List<V> combine(Object[] values) {
      List<V> result = newArrayListWithCapacity(values.length);
      for (Object slot : values) {
        result.add(isEmptySlot(slot) ? null : (V) slot);
      }
      return unmodifiableList(result);
    }
  }
}
