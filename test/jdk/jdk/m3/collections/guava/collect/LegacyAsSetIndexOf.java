/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.guava.collect;

import static java.util.Objects.requireNonNull;
import static jdk.m3.collections.guava.collect.SortedLists.KeyAbsentBehavior.NEXT_LOWER;
import static jdk.m3.collections.guava.collect.SortedLists.KeyPresentBehavior.ANY_PRESENT;

import java.util.Optional;
import java.util.function.Supplier;
import jdk.m3.collections.guava.primitives.Ints;

/** Oracle: the pre-patch AsSet.indexOf body, copied verbatim and renamed. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class LegacyAsSetIndexOf {

  private LegacyAsSetIndexOf() {}

  /** A returned value, or the thrown exception class and message. */
  record Outcome(Object value, Class<?> thrown, String message) {

    static Outcome of(Supplier<?> call) {
      try {
        return new Outcome(call.get(), Outcome.class, "");
      } catch (RuntimeException | AssertionError e) {
        return new Outcome(Outcome.class, e.getClass(), String.valueOf(e.getMessage()));
      }
    }

    Outcome withoutMessage() {
      return new Outcome(value, thrown, "");
    }
  }

  /**
   * Pre-patch body with {@code ranges} -> the reference range list, {@code contains(t)} ->
   * {@code asSet.contains(t)}.
   */
  static <C extends Comparable> int indexOf(
      Iterable<Range<C>> ranges, DiscreteDomain<C> domain, ImmutableSortedSet<C> asSet,
      Object target) {
    if (asSet.contains(target)) {
      @SuppressWarnings("unchecked") // if it's contained, it's definitely a C
      C c = (C) requireNonNull(target);
      long total = 0;
      for (Range<C> range : ranges) {
        if (range.contains(c)) {
          return Ints.saturatedCast(total + ContiguousSet.create(range, domain).indexOf(c));
        } else {
          total += ContiguousSet.create(range, domain).size();
        }
      }
      throw new AssertionError("impossible");
    }
    return -1;
  }

  /** Pre-patch rangeContaining body with {@code ranges} -> the reference range list. */
  static Optional<Range> rangeContaining(ImmutableRangeSet rs, Object target) {
    ImmutableList<Range> ranges = ((ImmutableSet<Range>) rs.asRanges()).asList();
    Comparable value = (Comparable) target;
    int index =
        SortedLists.binarySearch(
            ranges,
            Range::lowerBound,
            Cut.belowValue(value),
            Ordering.natural(),
            ANY_PRESENT,
            NEXT_LOWER);
    if (index != -1) {
      Range range = ranges.get(index);
      return range.contains(value) ? Optional.of(range) : Optional.empty();
    }
    return Optional.empty();
  }

  /** Independent model: linear scan for the range holding a non-null value. */
  static Optional<Range> linearRangeContaining(ImmutableRangeSet rs, Comparable value) {
    return ((ImmutableSet<Range>) rs.asRanges()).stream()
        .filter(r -> r.contains(value))
        .findFirst();
  }

  /** The patched rangeContaining, called raw so wrong-type values reach it. */
  static Optional<Range> patchedRangeContaining(ImmutableRangeSet rs, Object value) {
    return Optional.ofNullable(rs.rangeContaining((Comparable) value));
  }
}
