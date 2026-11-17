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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jdk.m3.collections.guava.collect.LegacyAsSetIndexOf.Outcome;

/** Random lockstep driver: patched AsSet.indexOf against the legacy body and a brute model. */
final class IndexOfLockstep {

  static final DiscreteDomain<Integer> INTS = DiscreteDomain.integers();
  private static final int BRUTE_LIMIT = 10_000;
  private static final int SMALL_SPAN = 4_000;
  private static final int MAX_RANGES = 40;
  private static final int RANDOM_TARGETS = 64;
  private static final Object[] ODD_TARGETS = {"x", 5L, new Object()};

  private final Random random;
  private final boolean small;
  private int checks;

  IndexOfLockstep(long seed, boolean small) {
    this.random = new Random(seed);
    this.small = small;
  }

  int checks() {
    return checks;
  }

  /** 1..40 disjoint ranges; built by builder or unionOf at random. */
  ImmutableRangeSet<Integer> randomRangeSet() {
    int k = 1 + random.nextInt(MAX_RANGES);
    int[] cuts = randomInts().distinct().limit(2L * k).sorted().toArray();
    List<Range<Integer>> ranges =
        IntStream.range(0, k).mapToObj(i -> range(cuts, i, k)).toList();
    return random.nextBoolean()
        ? ImmutableRangeSet.unionOf(ranges)
        : ImmutableRangeSet.<Integer>builder().addAll(ranges).build();
  }

  private Range<Integer> range(int[] cuts, int i, int k) {
    BoundType lower = bound();
    BoundType upper = bound();
    if (!small && i == 0 && random.nextInt(4) == 0) {
      return Range.upTo(cuts[1], upper);
    }
    if (!small && i == k - 1 && random.nextInt(4) == 0) {
      return Range.downTo(cuts[2 * i], lower);
    }
    return Range.range(cuts[2 * i], lower, cuts[2 * i + 1], upper);
  }

  private BoundType bound() {
    return random.nextBoolean() ? BoundType.OPEN : BoundType.CLOSED;
  }

  private IntStream randomInts() {
    return small ? random.ints(-SMALL_SPAN, SMALL_SPAN + 1) : random.ints();
  }

  private int pick() {
    return randomInts().findFirst().orElseThrow();
  }

  /** Endpoints and neighbours, random ints, extremes and wrong types, shuffled. */
  List<Object> targets(ImmutableRangeSet<Integer> rs) {
    Stream<Integer> ends = rs.asRanges().stream()
        .flatMap(IndexOfLockstep::endpoints)
        .flatMap(e -> Stream.of(e - 1, e, e + 1));
    Stream<Integer> rnd = randomInts().limit(RANDOM_TARGETS).boxed();
    Stream<Object> fixed = Stream.of(Integer.MIN_VALUE, Integer.MAX_VALUE, null);
    List<Object> all = new ArrayList<>(
        Stream.of(ends, rnd, fixed, Stream.of(ODD_TARGETS)).flatMap(s -> s).toList());
    Collections.shuffle(all, random);
    return all;
  }

  private static Stream<Integer> endpoints(Range<Integer> r) {
    Stream<Integer> lower = r.hasLowerBound() ? Stream.of(r.lowerEndpoint()) : Stream.empty();
    Stream<Integer> upper = r.hasUpperBound() ? Stream.of(r.upperEndpoint()) : Stream.empty();
    return Stream.concat(lower, upper);
  }

  /** Drives the base set, its complement, a sub range set and the three AsSet views. */
  void drive(ImmutableRangeSet<Integer> rs) {
    List<Object> targets = targets(rs);
    ImmutableSortedSet<Integer> set = rs.asSet(INTS);
    check(set, rs, targets);
    check(rs.complement().asSet(INTS), rs.complement(), targets);
    Range<Integer> sub = randomRange();
    check(rs.subRangeSet(sub).asSet(INTS), rs.subRangeSet(sub), targets);
    int x = pick();
    boolean b = random.nextBoolean();
    check(set.headSet(x, b), rs.subRangeSet(Range.upTo(x, BoundType.forBoolean(b))), targets);
    check(set.tailSet(x, b), rs.subRangeSet(Range.downTo(x, BoundType.forBoolean(b))), targets);
    Range<Integer> r = randomRange();
    check(set.subSet(r.lowerEndpoint(), r.lowerBoundType() == BoundType.CLOSED,
        r.upperEndpoint(), r.upperBoundType() == BoundType.CLOSED), rs.subRangeSet(r), targets);
  }

  private Range<Integer> randomRange() {
    int a = pick();
    int b = pick();
    if (a == b) {
      return Range.closed(a, b);
    }
    return Range.range(Math.min(a, b), bound(), Math.max(a, b), bound());
  }

  /** Every target twice: the first pass builds the lane, the second hits the cached path. */
  void check(ImmutableSortedSet<Integer> set, ImmutableRangeSet<Integer> ref, List<Object> ts) {
    Optional<int[]> brute = brute(set, ref);
    brute.ifPresent(elems -> surface(set, ref, elems));
    Stream.concat(ts.stream(), ts.stream()).forEach(t -> checkTarget(set, ref, brute, t));
  }

  private void checkTarget(
      ImmutableSortedSet<Integer> set, ImmutableRangeSet<Integer> ref, Optional<int[]> brute,
      Object t) {
    Outcome legacy =
        Outcome.of(() -> LegacyAsSetIndexOf.indexOf(ref.asRanges(), INTS, set, t));
    Outcome patched = Outcome.of(() -> set.indexOf(t));
    assertEquals(legacy, patched, () -> "indexOf(" + t + ") on " + ref);
    brute.ifPresent(e -> assertEquals(bruteIndex(e, t), patched.value(), () -> "brute " + t));
    Outcome down = Outcome.of(() -> set.descendingSet().indexOf(t));
    assertEquals(descending(legacy, set), down, () -> "descending indexOf(" + t + ")");
    if (t == null || t instanceof Comparable) {
      checkRangeContaining(ref, t);
    }
    checks++;
  }

  /** Verbatim pre-patch rangeContaining (exception class only) and, for ints, a linear scan. */
  private static void checkRangeContaining(ImmutableRangeSet<Integer> ref, Object t) {
    Outcome before = Outcome.of(() -> LegacyAsSetIndexOf.rangeContaining(ref, t));
    Outcome after = Outcome.of(() -> LegacyAsSetIndexOf.patchedRangeContaining(ref, t));
    assertEquals(before.withoutMessage(), after.withoutMessage(), () -> "range " + t);
    if (t instanceof Integer i) {
      assertEquals(Outcome.of(() -> LegacyAsSetIndexOf.linearRangeContaining(ref, i)), after,
          () -> "linear range " + i);
    }
  }

  private static Outcome descending(Outcome forward, ImmutableSortedSet<Integer> set) {
    if (forward.thrown() != Outcome.class) {
      return forward;
    }
    int index = (Integer) forward.value();
    return Outcome.of(() -> index == -1 ? -1 : set.size() - 1 - index);
  }

  private static int bruteIndex(int[] elems, Object t) {
    if (!(t instanceof Integer)) {
      return -1;
    }
    int at = Arrays.binarySearch(elems, (Integer) t);
    return at >= 0 ? at : -1;
  }

  /** Elements from ContiguousSet per range only; never through the AsSet under test. */
  private static Optional<int[]> brute(
      ImmutableSortedSet<Integer> set, ImmutableRangeSet<Integer> ref) {
    if (set.size() > BRUTE_LIMIT) {
      return Optional.empty();
    }
    return Optional.of(ref.asRanges().stream()
        .map(r -> ContiguousSet.create(r, INTS))
        .filter(cs -> !cs.isEmpty())
        .flatMapToInt(cs -> IntStream.rangeClosed(cs.first(), cs.last()))
        .toArray());
  }

  /** Unchanged surface: size, toString, equals/hashCode, both iteration orders. */
  private void surface(ImmutableSortedSet<Integer> set, ImmutableRangeSet<Integer> ref, int[] e) {
    assertEquals(e.length, set.size());
    assertEquals(ref.asRanges().toString(), set.toString());
    assertArrayEquals(e, set.stream().mapToInt(Integer::intValue).toArray());
    int[] down = set.descendingSet().stream().mapToInt(Integer::intValue).toArray();
    assertArrayEquals(e, IntStream.range(0, e.length).map(i -> down[e.length - 1 - i]).toArray());
    ImmutableSortedSet<Integer> copy = ImmutableSortedSet.copyOf(IntStream.of(e).boxed().toList());
    assertTrue(set.equals(copy) && copy.equals(set), "equals");
    assertEquals(IntStream.of(e).sum(), set.hashCode());
    checks++;
  }
}
