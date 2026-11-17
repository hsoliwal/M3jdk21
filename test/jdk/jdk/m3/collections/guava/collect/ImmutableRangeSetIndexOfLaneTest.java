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

import static jdk.m3.collections.guava.collect.IndexOfLockstep.INTS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import jdk.m3.collections.guava.collect.LegacyAsSetIndexOf.Outcome;
import org.junit.jupiter.api.Test;

//FLLDCIM: LEETCODIFY — AsSet.indexOf prefix-offset lane vs the verbatim legacy loop
@SuppressWarnings({"rawtypes", "unchecked"})
class ImmutableRangeSetIndexOfLaneTest {

  private static final long SEED = 20261117L;
  private static final int RANDOM_SETS = 2_000;
  private static final int SERIAL_SETS = 100;
  private static final int RACE_SETS = 200;
  private static final int THREADS = 8;
  private static final int COST_RANGES = 32;
  private static final int MAX = Integer.MAX_VALUE;
  private static final long E9 = 1_000_000_000L;
  private static final long TWO_31 = 1L << 31;

  @Test
  void randomLockstepMatchesLegacy() {
    int checks = IntStream.range(0, RANDOM_SETS).map(n -> driveOne(n)).sum();
    assertTrue(checks > RANDOM_SETS * 100, () -> "checks " + checks);
  }

  private static int driveOne(int n) {
    IndexOfLockstep lockstep = new IndexOfLockstep(SEED + n, n % 2 == 0);
    lockstep.drive(lockstep.randomRangeSet());
    return lockstep.checks();
  }

  @Test
  void longsTruncatedDistancesMatchLiterals() {
    DiscreteDomain<Long> d = DiscreteDomain.longs();
    ImmutableRangeSet<Long> rs = ImmutableRangeSet.unionOf(List.of(Range.closed(0L, 3 * E9),
        Range.closed(4 * E9, 7 * E9), Range.closed(8 * E9, 11 * E9)));
    ImmutableSortedSet<Long> set = rs.asSet(d);
    assertIndex(rs, d, set, 3 * E9 + 1, -1);
    assertIndex(rs, d, set, TWO_31, Integer.MIN_VALUE);
    assertIndex(rs, d, set, 4 * E9 + TWO_31, -1);
    assertIndex(rs, d, set, 8 * E9 + TWO_31, MAX - 1);
    assertIndex(rs, d, set, 8 * E9 + 5, MAX);
  }

  @Test
  void bigIntegerSaturatedDistances() {
    DiscreteDomain<BigInteger> d = DiscreteDomain.bigIntegers();
    BigInteger w = BigInteger.ONE.shiftLeft(70);
    BigInteger s1 = BigInteger.ONE.shiftLeft(71);
    BigInteger s2 = BigInteger.ONE.shiftLeft(72);
    BigInteger far = BigInteger.ONE.shiftLeft(65);
    ImmutableRangeSet<BigInteger> rs = ImmutableRangeSet.unionOf(List.of(
        Range.closed(BigInteger.ZERO, w), Range.closed(s1, s1.add(w)),
        Range.closed(s2, s2.add(w))));
    ImmutableSortedSet<BigInteger> set = rs.asSet(d);
    assertIndex(rs, d, set, s2.add(far), MAX);
    assertIndex(rs, d, set, far, -1);
    assertIndex(rs, d, set, s1.add(far), MAX - 1);
    assertIndex(rs, d, set, s1.add(BigInteger.valueOf(TWO_31)), -1);
    assertIndex(rs, d, set, s1.add(BigInteger.valueOf(7)), MAX);
  }

  @Test
  void clippedDomainSliceMinusOne() {
    ImmutableRangeSet<Integer> rs = halves();
    ClippedDomain d = new ClippedDomain();
    ImmutableSortedSet<Integer> set = rs.asSet(d);
    assertIndex(rs, d, set, 2000, 5);
    assertIndex(rs, d, set, -3, -1);
    assertIndex(rs, d, set, 7, -1);
    assertIndex(rs, d, set, 12, 8);
  }

  @Test
  void laneTiedToOneDomain() {
    ImmutableRangeSet<Integer> rs = halves();
    assertIndex(rs, new ClippedDomain(), rs.asSet(new ClippedDomain()), 12, 8);
    assertIndex(rs, INTS, rs.asSet(INTS), 12, MAX);
    assertIndex(rs, INTS, rs.asSet(INTS), -3, MAX - 2);
    assertIndex(rs, new ClippedDomain(), rs.asSet(new ClippedDomain()), -3, -1);
  }

  private static ImmutableRangeSet<Integer> halves() {
    return ImmutableRangeSet.unionOf(List.of(Range.atMost(5), Range.atLeast(10)));
  }

  private static <C extends Comparable> void assertIndex(ImmutableRangeSet<C> rs,
      DiscreteDomain<C> d, ImmutableSortedSet<C> set, Object target, int expected) {
    assertEquals(expected, LegacyAsSetIndexOf.indexOf(rs.asRanges(), d, set, target), "legacy");
    assertEquals(expected, set.indexOf(target), () -> "first indexOf(" + target + ")");
    assertEquals(expected, set.indexOf(target), () -> "cached indexOf(" + target + ")");
    int down = expected == -1 ? -1 : set.size() - 1 - expected;
    assertEquals(down, set.descendingSet().indexOf(target), "descending");
  }

  @Test
  void serializedCopyMatchesLegacy() {
    IntStream.range(0, SERIAL_SETS).forEach(n -> roundTrip(n));
  }

  private static void roundTrip(int n) {
    IndexOfLockstep lockstep = new IndexOfLockstep(SEED - n, n % 2 == 0);
    ImmutableRangeSet<Integer> rs = lockstep.randomRangeSet();
    ImmutableSortedSet<Integer> set = rs.asSet(INTS);
    List<Object> targets = lockstep.targets(rs);
    lockstep.check(set, rs, targets);
    ImmutableSortedSet<Integer> copy = (ImmutableSortedSet<Integer>) deserialize(serialize(set));
    assertEquals(set.getClass(), copy.getClass());
    assertEquals(set.toString(), copy.toString());
    lockstep.check(copy, rs, targets);
  }

  private static byte[] serialize(Object o) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(o);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return bytes.toByteArray();
  }

  private static Object deserialize(byte[] bytes) {
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return in.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void concurrentFirstCallsAgree() {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    try {
      IntStream.range(0, RACE_SETS).forEach(n -> race(pool, n));
    } finally {
      pool.shutdownNow();
    }
  }

  private static void race(ExecutorService pool, int n) {
    IndexOfLockstep lockstep = new IndexOfLockstep(SEED * 31 + n, n % 2 == 0);
    ImmutableRangeSet<Integer> rs = lockstep.randomRangeSet();
    ImmutableSortedSet<Integer> set = rs.asSet(INTS);
    List<Object> targets = lockstep.targets(rs);
    CountDownLatch gate = new CountDownLatch(1);
    List<Future<?>> runs = IntStream.range(0, THREADS)
        .<Future<?>>mapToObj(i -> pool.submit(() -> agree(gate, rs, set, shuffled(targets, i))))
        .toList();
    gate.countDown();
    runs.forEach(run -> assertDoesNotThrow(() -> run.get(), "racing thread"));
  }

  private static List<Object> shuffled(List<Object> targets, int seed) {
    List<Object> copy = new ArrayList<>(targets);
    Collections.shuffle(copy, new Random(seed));
    return copy;
  }

  private static void agree(CountDownLatch gate, ImmutableRangeSet<Integer> rs,
      ImmutableSortedSet<Integer> set, List<Object> targets) {
    try {
      gate.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
    targets.forEach(t -> assertEquals(
        Outcome.of(() -> LegacyAsSetIndexOf.indexOf(rs.asRanges(), INTS, set, t)),
        Outcome.of(() -> set.indexOf(t)), () -> "raced indexOf(" + t + ")"));
  }

  @Test
  void secondCallMakesOneDistanceCall() {
    CountingDomain d = new CountingDomain();
    ImmutableRangeSet<Integer> rs = ImmutableRangeSet.unionOf(IntStream.range(0, COST_RANGES)
        .mapToObj(i -> Range.closed(10 * i, 10 * i + 4)).toList());
    ImmutableSortedSet<Integer> set = rs.asSet(d);
    int target = 10 * (COST_RANGES - 1) + 2;
    int first = d.distanceCalls(() -> set.indexOf(target));
    assertTrue(first <= COST_RANGES + 1, () -> "first call " + first);
    assertEquals(1, d.distanceCalls(() -> set.indexOf(target)), "cached call");
    assertEquals(COST_RANGES,
        d.distanceCalls(() -> LegacyAsSetIndexOf.indexOf(rs.asRanges(), d, set, target)));
    assertEquals(5 * (COST_RANGES - 1) + 2, set.indexOf(target));
  }

  /** integers() with every distance() call counted. */
  private static final class CountingDomain extends DiscreteDomain<Integer> {
    private final AtomicInteger distances = new AtomicInteger();

    int distanceCalls(Runnable call) {
      distances.set(0);
      call.run();
      return distances.get();
    }

    @Override
    public Integer next(Integer value) {
      return INTS.next(value);
    }

    @Override
    public Integer previous(Integer value) {
      return INTS.previous(value);
    }

    @Override
    public long distance(Integer start, Integer end) {
      distances.incrementAndGet();
      return INTS.distance(start, end);
    }

    @Override
    public Integer minValue() {
      return INTS.minValue();
    }

    @Override
    public Integer maxValue() {
      return INTS.maxValue();
    }
  }

  /** Integers clipped to [0..1000]; slices of unbounded ranges miss values outside it. */
  private static final class ClippedDomain extends DiscreteDomain<Integer> {
    private static final int LOW = 0;
    private static final int HIGH = 1000;

    @Override
    public Integer next(Integer value) {
      return value >= HIGH ? null : value + 1;
    }

    @Override
    public Integer previous(Integer value) {
      return value <= LOW ? null : value - 1;
    }

    @Override
    public long distance(Integer start, Integer end) {
      return (long) end - start;
    }

    @Override
    public Integer minValue() {
      return LOW;
    }

    @Override
    public Integer maxValue() {
      return HIGH;
    }
  }
}
