/*******************************************************************************
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

import com.m3.collections.M3LongIterator;
import com.m3.collections.M3LongLongTreeMap;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Bounded actual-owner qualification using sequential JDK TreeMap updates as the oracle.
 *
 * <p>No arguments checks semantics on either receiver baseline or candidate. The optional
 * --expect-reuse mode additionally checks private lane identity when the frozen capacity rule
 * permits reuse. The separate --allocation-probe mode measures a fixed workload through the
 * supported thread-allocation counter; it makes no timing or universal performance claim.
 * Private lane references are observed only, never passed into the map or modified.
 * Near-maximum array allocation and unsynchronized concurrent mutation are outside this corpus.
 */
public final class LongLongSortedBatchOracle {
    private static final long SEED = 0x4d334c4c42415443L;
    private static final long[] DOMAIN = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE};
    private static final long[] QUERIES = {
        Long.MIN_VALUE, -2L, -1L, 0L, 1L, 2L, Long.MAX_VALUE
    };
    private static final Field KEY_LANE = field("keys");
    private static final Field VALUE_LANE = field("values");
    private static final Field MOD_COUNT = field("modCount");
    private static final int MIXED_STEPS = 5_000;
    private static volatile Object allocationSink;

    private static boolean expectReuse;
    private static long assertions;
    private static long batchCalls;
    private static long exhaustiveCases;
    private static long invalidCalls;
    private static long callbackCases;
    private static long reuseChecks;
    private static long growthChecks;

    private LongLongSortedBatchOracle() {}

    public static void main(String[] args) {
        if (args.length == 1 && "--allocation-probe".equals(args[0])) {
            allocationProbe();
            return;
        }
        if (args.length > 1
                || (args.length == 1 && !"--expect-reuse".equals(args[0]))) {
            throw new IllegalArgumentException(
                    "usage: LongLongSortedBatchOracle [--expect-reuse | --allocation-probe]");
        }
        expectReuse = args.length == 1;
        fixedBatches();
        invalidBatches();
        callbackBehavior();
        exhaustiveBatches();
        mixedOperations();
        System.out.println("{\"schema\":\"m3.sorted-batch-oracle.v1\",\"status\":\"PASS\""
                + ",\"mode\":\"" + (expectReuse ? "expect-reuse" : "semantic") + "\""
                + ",\"seed\":\"0x4d334c4c42415443\""
                + ",\"batch_calls\":" + batchCalls
                + ",\"exhaustive_cases\":" + exhaustiveCases
                + ",\"mixed_steps\":" + MIXED_STEPS
                + ",\"invalid_calls\":" + invalidCalls
                + ",\"callback_cases\":" + callbackCases
                + ",\"reuse_checks\":" + reuseChecks
                + ",\"growth_checks\":" + growthChecks
                + ",\"assertions\":" + assertions + "}");
    }

    private record Fixture(M3LongLongTreeMap actual, TreeMap<Long, Long> expected) {}

    private record Snapshot(
            int size, int capacity, int modifications, long[] keys, long[] values,
            long[] keyLane, long[] valueLane) {}

    private static Fixture fixture(int capacity, long... initialKeys) {
        M3LongLongTreeMap actual = new M3LongLongTreeMap(capacity);
        TreeMap<Long, Long> expected = new TreeMap<>();
        for (int i = 0; i < initialKeys.length; i++) {
            long value = 100L + i;
            boolean added = expected.put(initialKeys[i], value) == null;
            check(actual.put(initialKeys[i], value) == added, "fixture put result");
        }
        return new Fixture(actual, expected);
    }

    /** Count distinct keys independently; use sequential put to decide all mapping results. */
    private static void batch(
            Fixture fixture, long[] keys, long[] values, String label) {
        long[] sourceKeysBefore = keys.clone();
        long[] sourceValuesBefore = values.clone();
        Snapshot before = snapshot(fixture.actual());
        M3LongIterator previousIterator = fixture.actual().keyIterator();
        TreeSet<Long> distinct = new TreeSet<>();
        for (int i = 0; i < keys.length; i++) {
            distinct.add(keys[i]);
            fixture.expected().put(keys[i], values[i]);
        }
        int expectedAdded = fixture.expected().size() - before.size();
        int expectedCapacity = Math.max(before.capacity(), before.size() + distinct.size());
        int actualAdded = fixture.actual().putAllSorted(keys, values);
        batchCalls++;
        equal(expectedAdded, actualAdded, label + ": new-key result");
        equal(expectedCapacity, fixture.actual().capacity(), label + ": exact capacity");
        equal(before.modifications() + (expectedAdded == 0 ? 0 : 1),
                modifications(fixture.actual()), label + ": structural epoch");
        arrayEqual(sourceKeysBefore, keys, label + ": source keys changed");
        arrayEqual(sourceValuesBefore, values, label + ": source values changed");
        if (expectedAdded == 0) {
            iteratorEquals(previousIterator, fixture.expected(), label + ": existing iterator");
        } else {
            throwsType(ConcurrentModificationException.class, previousIterator::hasNext,
                    label + ": stale iterator hasNext");
            throwsType(ConcurrentModificationException.class, previousIterator::nextLong,
                    label + ": stale iterator nextLong");
        }
        if (expectReuse) {
            long[] keyLaneAfter = lane(KEY_LANE, fixture.actual());
            long[] valueLaneAfter = lane(VALUE_LANE, fixture.actual());
            if (expectedCapacity == before.capacity()) {
                check(keyLaneAfter == before.keyLane(), label + ": keys lane was not reused");
                check(valueLaneAfter == before.valueLane(), label + ": values lane was not reused");
                reuseChecks++;
            } else {
                check(keyLaneAfter != before.keyLane(), label + ": growth retained keys lane");
                check(valueLaneAfter != before.valueLane(), label + ": growth retained values lane");
                growthChecks++;
            }
            equal(expectedCapacity, keyLaneAfter.length, label + ": keys lane length");
            equal(expectedCapacity, valueLaneAfter.length, label + ": values lane length");
            check(keyLaneAfter != valueLaneAfter, label + ": internal lanes alias");
        }
        stateEquals(fixture, label);
    }

    private static void fixedBatches() {
        Fixture overwriteGrowth = fixture(8, 1, 2, 3, 4, 5, 6);
        batch(overwriteGrowth, new long[] {1, 2, 3, 4, 5, 6},
                new long[] {11, 22, 33, 44, 55, 66}, "overwrite-only growth");
        equal(12, overwriteGrowth.actual().capacity(), "overwrite-only grows to twelve");

        Fixture unionSmaller = fixture(8, 1, 2, 3, 4, 5, 6);
        batch(unionSmaller, new long[] {2, 3, 4, 7},
                new long[] {20, 30, 40, 70}, "capacity exceeds union size");
        equal(7, unionSmaller.actual().size(), "union has seven entries");
        equal(10, unionSmaller.actual().capacity(), "capacity follows old distinct-count rule");

        batch(fixture(32, -3, 0, 3), new long[] {-4, -3, -3, 0, 2, 3, 3, 4},
                new long[] {1, 2, 3, 4, 5, 6, 7, 8}, "spare-capacity interleaving");
        batch(fixture(8, 10, 20), new long[] {-4, -4, -2, -1},
                new long[] {1, 2, 3, 4}, "all incoming before existing");
        batch(fixture(8, -20, -10), new long[] {1, 2, 2, 4},
                new long[] {1, 2, 3, 4}, "all incoming after existing");
        batch(fixture(8), new long[] {Long.MIN_VALUE, Long.MIN_VALUE, 0, Long.MAX_VALUE},
                new long[] {0, Long.MAX_VALUE, Long.MIN_VALUE, -1}, "empty and extrema");
        batch(fixture(8, Long.MIN_VALUE, 0, Long.MAX_VALUE),
                new long[] {Long.MIN_VALUE, Long.MIN_VALUE, 0, 0, Long.MAX_VALUE, Long.MAX_VALUE},
                new long[] {1, 2, 3, 4, 5, 6}, "duplicate extrema overwrite");
        long[] sameSource = {Long.MIN_VALUE, Long.MIN_VALUE, -1, 0, 0, Long.MAX_VALUE};
        batch(fixture(16, -2, 0, 2), sameSource, sameSource, "same array for both sources");
        long[] repeatedKeys = new long[257];
        long[] repeatedValues = new long[257];
        Arrays.fill(repeatedKeys, 7L);
        for (int i = 0; i < repeatedValues.length; i++) {
            repeatedValues[i] = i;
        }
        Fixture duplicates = fixture(8);
        batch(duplicates, repeatedKeys, repeatedValues, "batch length is not distinct count");
        equal(8, duplicates.actual().capacity(), "duplicate-only capacity");
        equal(256L, duplicates.actual().getOrThrow(7), "last duplicate wins");
        batch(duplicates, new long[0], new long[0], "empty input");
    }

    private static void invalidBatches() {
        for (int capacity : new int[] {8, 32}) {
            Fixture fixture = fixture(capacity, -1, 1, 3, 5);
            reject(fixture, null, null, NullPointerException.class, "sourceKeys", "both null");
            reject(fixture, null, new long[0], NullPointerException.class,
                    "sourceKeys", "null keys");
            reject(fixture, new long[0], null, NullPointerException.class,
                    "sourceValues", "null values before empty return");
            reject(fixture, new long[] {3, 1}, null, NullPointerException.class,
                    "sourceValues", "null values before sortedness");
            reject(fixture, new long[] {3, 1}, new long[1], IllegalArgumentException.class,
                    "key/value length mismatch", "length before sortedness");
            reject(fixture, new long[0], new long[1], IllegalArgumentException.class,
                    "key/value length mismatch", "length before empty return");
            reject(fixture, new long[] {1, 1, 3, 7, 9, 8}, new long[] {9, 8, 7, 6, 5, 4},
                    IllegalArgumentException.class, "source keys must be sorted", "late descent");
            reject(fixture, new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE, Long.MAX_VALUE - 1},
                    new long[] {1, 2, 3, 4}, IllegalArgumentException.class,
                    "source keys must be sorted", "late extreme descent");
        }
    }

    /** Invalid input must leave contents, ownership, capacity and existing iterators intact. */
    private static void reject(
            Fixture fixture, long[] keys, long[] values,
            Class<? extends Throwable> type, String message, String label) {
        Snapshot before = snapshot(fixture.actual());
        long[] keysBefore = keys == null ? null : keys.clone();
        long[] valuesBefore = values == null ? null : values.clone();
        M3LongIterator iterator = fixture.actual().keyIterator();
        Throwable failure = throwsType(type, () -> fixture.actual().putAllSorted(keys, values), label);
        invalidCalls++;
        check(message.equals(failure.getMessage()), label + ": exception message");
        equal(before.size(), fixture.actual().size(), label + ": size changed");
        equal(before.capacity(), fixture.actual().capacity(), label + ": capacity changed");
        equal(before.modifications(), modifications(fixture.actual()), label + ": epoch changed");
        arrayEqual(before.keys(), fixture.actual().keysToArray(), label + ": keys changed");
        arrayEqual(before.values(), fixture.actual().valuesToArray(), label + ": values changed");
        check(before.keyLane() == lane(KEY_LANE, fixture.actual()), label + ": keys owner changed");
        check(before.valueLane() == lane(VALUE_LANE, fixture.actual()), label + ": values owner changed");
        if (keys != null) {
            arrayEqual(keysBefore, keys, label + ": rejected source keys changed");
        }
        if (values != null) {
            arrayEqual(valuesBefore, values, label + ": rejected source values changed");
        }
        iteratorEquals(iterator, fixture.expected(), label + ": rejected iterator");
        stateEquals(fixture, label);
    }

    private static void callbackBehavior() {
        Fixture overwrite = fixture(8, 1, 2, 3, 4, 5, 6);
        List<Long> visitedKeys = new ArrayList<>();
        List<Long> visitedValues = new ArrayList<>();
        overwrite.actual().forEach((key, value) -> {
            visitedKeys.add(key);
            visitedValues.add(value);
            if (key == 1) {
                batch(overwrite, new long[] {1, 2, 3, 4, 5, 6},
                        new long[] {11, 22, 33, 44, 55, 66}, "callback overwrite and growth");
            }
        });
        check(visitedKeys.equals(List.of(1L, 2L, 3L, 4L, 5L, 6L)), "callback key order");
        check(visitedValues.equals(List.of(100L, 22L, 33L, 44L, 55L, 66L)),
                "callback sees subsequent replacements");
        callbackCases++;

        Fixture ranged = fixture(8, 1, 2, 3, 4, 5, 6);
        List<Long> rangeValues = new ArrayList<>();
        ranged.actual().forEachInRange(2, 6, (key, value) -> {
            rangeValues.add(value);
            if (key == 2) {
                batch(ranged, new long[] {1, 2, 3, 4, 5, 6},
                        new long[] {11, 22, 33, 44, 55, 66}, "range callback overwrite");
            }
        });
        check(rangeValues.equals(List.of(101L, 33L, 44L, 55L)), "range callback values");
        callbackCases++;

        Fixture structural = fixture(8, 1, 2, 3);
        int[] visits = {0};
        throwsType(ConcurrentModificationException.class, () -> structural.actual().forEach((key, value) -> {
            visits[0]++;
            batch(structural, new long[] {99, 99}, new long[] {9, 10}, "structural callback");
        }), "structural callback fails immediately");
        equal(1, visits[0], "structural callback visit count");
        stateEquals(structural, "structural callback committed batch");
        callbackCases++;

        Fixture rejected = fixture(8, 1, 2);
        int[] rejectedVisits = {0};
        rejected.actual().forEach((key, value) -> {
            rejectedVisits[0]++;
            if (key == 1) {
                reject(rejected, new long[] {1, 3, 2}, new long[] {7, 8, 9},
                        IllegalArgumentException.class, "source keys must be sorted",
                        "caught callback rejection");
            }
        });
        equal(2, rejectedVisits[0], "rejection leaves outer walk usable");
        callbackCases++;

        Fixture throwing = fixture(8, 1, 2);
        RuntimeException sentinel = new RuntimeException("callback sentinel");
        Throwable observed = throwsType(RuntimeException.class, () -> throwing.actual().forEach((key, value) -> {
            batch(throwing, new long[] {99}, new long[] {10}, "throwing callback mutation");
            throw sentinel;
        }), "callback exception takes precedence");
        check(observed == sentinel, "callback exception identity");
        stateEquals(throwing, "throwing callback retains committed mutation");
        callbackCases++;
    }

    /** Enumerate all existing subsets and all nondecreasing batches of lengths zero through five. */
    private static void exhaustiveBatches() {
        List<long[]> batches = new ArrayList<>();
        enumerate(new long[5], 0, 0, batches);
        for (int mask = 0; mask < (1 << DOMAIN.length); mask++) {
            long[] existing = new long[Integer.bitCount(mask)];
            int count = 0;
            for (int bit = 0; bit < DOMAIN.length; bit++) {
                if ((mask & (1 << bit)) != 0) {
                    existing[count++] = DOMAIN[bit];
                }
            }
            for (int capacity : new int[] {8, 32}) {
                for (long[] keys : batches) {
                    long[] values = new long[keys.length];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = 1000L + 17L * i;
                    }
                    String label = "exhaustive mask=" + mask + " capacity=" + capacity
                            + " keys=" + Arrays.toString(keys);
                    batch(fixture(capacity, existing), keys, values, label);
                    exhaustiveCases++;
                }
            }
        }
        equal(252, batches.size(), "exhaustive nondecreasing sequence count");
        equal(16_128L, exhaustiveCases, "exhaustive case count");
    }

    private static void enumerate(long[] working, int length, int minimumIndex, List<long[]> output) {
        output.add(Arrays.copyOf(working, length));
        if (length == working.length) {
            return;
        }
        for (int i = minimumIndex; i < DOMAIN.length; i++) {
            working[length] = DOMAIN[i];
            enumerate(working, length + 1, i, output);
        }
    }

    private static void mixedOperations() {
        Random random = new Random(SEED);
        Fixture fixture = fixture(8);
        for (int step = 0; step < MIXED_STEPS; step++) {
            long key = mixedKey(random);
            long value = mixedValue(random, step);
            String label = "mixed step=" + step;
            switch (random.nextInt(12)) {
                case 0, 1 -> {
                    boolean added = fixture.expected().put(key, value) == null;
                    check(fixture.actual().put(key, value) == added, label + ": put result");
                }
                case 2 -> {
                    boolean removed = fixture.expected().remove(key) != null;
                    check(fixture.actual().remove(key) == removed, label + ": remove result");
                }
                case 3, 4, 5 -> {
                    long[] keys = new long[random.nextInt(21)];
                    long[] values = new long[keys.length];
                    for (int i = 0; i < keys.length; i++) {
                        keys[i] = mixedKey(random);
                        values[i] = mixedValue(random, step + i);
                    }
                    Arrays.sort(keys);
                    if (random.nextInt(7) == 0) {
                        values = keys;
                    }
                    batch(fixture, keys, values, label);
                }
                case 6 -> {
                    M3LongIterator iterator = fixture.actual().keyIterator();
                    fixture.actual().trimToSize();
                    equal(Math.max(8, fixture.expected().size()), fixture.actual().capacity(),
                            label + ": trim capacity");
                    iteratorEquals(iterator, fixture.expected(), label + ": trim iterator");
                }
                case 7 -> {
                    if (step % 19 == 0) {
                        fixture.actual().clear();
                        fixture.expected().clear();
                    }
                }
                case 8 -> {
                    if (fixture.expected().isEmpty()) {
                        throwsType(NoSuchElementException.class, fixture.actual()::pollFirstKey,
                                label + ": empty poll");
                    } else if (random.nextBoolean()) {
                        equal(fixture.expected().pollFirstEntry().getKey(),
                                fixture.actual().pollFirstKey(), label + ": poll first");
                    } else {
                        equal(fixture.expected().pollLastEntry().getKey(),
                                fixture.actual().pollLastKey(), label + ": poll last");
                    }
                }
                case 9 -> reject(fixture, new long[] {Long.MIN_VALUE, 0, -1},
                        new long[] {3, 2, 1}, IllegalArgumentException.class,
                        "source keys must be sorted", label + ": rejection");
                default -> {
                    equal(fixture.expected().getOrDefault(key, value),
                            fixture.actual().getOrDefault(key, value), label + ": get default");
                    check(fixture.expected().containsKey(key) == fixture.actual().containsKey(key),
                            label + ": contains");
                }
            }
            stateEquals(fixture, label);
        }
    }

    private static long mixedKey(Random random) {
        return switch (random.nextInt(7)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            default -> random.nextInt(65) - 32L;
        };
    }

    private static long mixedValue(Random random, int step) {
        return switch (step % 7) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            default -> random.nextLong();
        };
    }

    private static void stateEquals(Fixture fixture, String label) {
        TreeMap<Long, Long> expected = fixture.expected();
        M3LongLongTreeMap actual = fixture.actual();
        long[] expectedKeys = new long[expected.size()];
        long[] expectedValues = new long[expected.size()];
        int index = 0;
        for (Map.Entry<Long, Long> entry : expected.entrySet()) {
            expectedKeys[index] = entry.getKey();
            expectedValues[index++] = entry.getValue();
        }
        equal(expected.size(), actual.size(), label + ": size");
        check(expected.isEmpty() == actual.isEmpty(), label + ": empty");
        check(actual.capacity() >= actual.size(), label + ": capacity below size");
        arrayEqual(expectedKeys, actual.keysToArray(), label + ": key snapshot");
        arrayEqual(expectedValues, actual.valuesToArray(), label + ": value snapshot");
        if (!expected.isEmpty()) {
            equal(expected.firstKey(), actual.firstKey(), label + ": first");
            equal(expected.lastKey(), actual.lastKey(), label + ": last");
        }
        for (long query : QUERIES) {
            navigation(expected.lowerKey(query), actual.lowerKey(query, 0), actual.lowerKey(query, 1),
                    label + ": lower");
            navigation(expected.floorKey(query), actual.floorKey(query, 0), actual.floorKey(query, 1),
                    label + ": floor");
            navigation(expected.ceilingKey(query), actual.ceilingKey(query, 0), actual.ceilingKey(query, 1),
                    label + ": ceiling");
            navigation(expected.higherKey(query), actual.higherKey(query, 0), actual.higherKey(query, 1),
                    label + ": higher");
        }
    }

    private static void navigation(Long expected, long zeroDefault, long oneDefault, String label) {
        equal(expected == null ? 0L : expected, zeroDefault, label + ": zero default");
        equal(expected == null ? 1L : expected, oneDefault, label + ": one default");
    }

    private static void iteratorEquals(
            M3LongIterator iterator, TreeMap<Long, Long> expected, String label) {
        for (long key : expected.keySet()) {
            check(iterator.hasNext(), label + ": early end");
            equal(key, iterator.nextLong(), label + ": key");
        }
        check(!iterator.hasNext(), label + ": trailing entry");
        throwsType(NoSuchElementException.class, iterator::nextLong, label + ": exhausted next");
    }

    private static Snapshot snapshot(M3LongLongTreeMap map) {
        return new Snapshot(map.size(), map.capacity(), modifications(map),
                map.keysToArray(), map.valuesToArray(), lane(KEY_LANE, map), lane(VALUE_LANE, map));
    }

    private static Field field(String name) {
        try {
            Field field = M3LongLongTreeMap.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static long[] lane(Field field, M3LongLongTreeMap map) {
        try {
            return (long[]) field.get(map);
        } catch (IllegalAccessException failure) {
            throw new AssertionError("cannot observe private lane", failure);
        }
    }

    private static int modifications(M3LongLongTreeMap map) {
        try {
            return MOD_COUNT.getInt(map);
        } catch (IllegalAccessException failure) {
            throw new AssertionError("cannot observe structural epoch", failure);
        }
    }

    private static Throwable throwsType(
            Class<? extends Throwable> expected, Runnable operation, String label) {
        try {
            operation.run();
        } catch (Throwable failure) {
            check(failure.getClass() == expected,
                    label + ": expected " + expected.getName() + " but received " + failure);
            return failure;
        }
        throw new AssertionError(label + ": expected " + expected.getName());
    }

    private static void arrayEqual(long[] expected, long[] actual, String label) {
        check(Arrays.equals(expected, actual),
                label + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
    }

    private static void equal(long expected, long actual, String label) {
        check(expected == actual, label + " expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    /** Resource-only mode: counter deltas cover putAllSorted, excluding fresh-map setup. */
    private static void allocationProbe() {
        java.lang.management.ThreadMXBean standardBean = ManagementFactory.getThreadMXBean();
        if (!(standardBean instanceof com.sun.management.ThreadMXBean bean)
                || !bean.isThreadAllocatedMemorySupported()) {
            unavailableAllocation("thread_allocation_counter_unsupported");
            return;
        }
        try {
            if (!bean.isThreadAllocatedMemoryEnabled()) {
                bean.setThreadAllocatedMemoryEnabled(true);
            }
        } catch (UnsupportedOperationException | SecurityException failure) {
            unavailableAllocation("thread_allocation_counter_not_enabled");
            return;
        }
        final int warmup = 512;
        final int samples = 1_024;
        long[] keys = new long[512];
        long[] values = new long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = 2L * i + 1;
            values[i] = -i;
        }
        for (int i = 0; i < warmup; i++) {
            M3LongLongTreeMap map = allocationFixture();
            equal(512, map.putAllSorted(keys, values), "allocation warmup new-key count");
            allocationSink = map;
        }
        long thread = Thread.currentThread().threadId();
        long total = 0;
        long minimum = Long.MAX_VALUE;
        long maximum = Long.MIN_VALUE;
        for (int i = 0; i < samples; i++) {
            M3LongLongTreeMap map = allocationFixture();
            long before = bean.getThreadAllocatedBytes(thread);
            int added = map.putAllSorted(keys, values);
            long after = bean.getThreadAllocatedBytes(thread);
            allocationSink = map;
            check(before >= 0 && after >= before, "allocation counter unavailable or decreased");
            equal(512, added, "allocation sample new-key count");
            equal(1_536, map.size(), "allocation sample size");
            equal(4_096, map.capacity(), "allocation sample capacity");
            long allocated = after - before;
            total += allocated;
            minimum = Math.min(minimum, allocated);
            maximum = Math.max(maximum, allocated);
        }
        System.out.println("{\"schema\":\"m3.sorted-batch-allocation.v1\",\"status\":\"MEASURED\""
                + ",\"warmup\":" + warmup + ",\"samples\":" + samples
                + ",\"initial_size\":1024,\"capacity\":4096,\"incoming_length\":512"
                + ",\"allocated_bytes_total\":" + total
                + ",\"allocated_bytes_minimum\":" + minimum
                + ",\"allocated_bytes_maximum\":" + maximum
                + ",\"setup_excluded\":true,\"timing_measured\":false}");
    }

    private static M3LongLongTreeMap allocationFixture() {
        M3LongLongTreeMap map = new M3LongLongTreeMap(4_096);
        for (int i = 0; i < 1_024; i++) {
            map.put(2L * i, i);
        }
        return map;
    }

    private static void unavailableAllocation(String reason) {
        System.out.println("{\"schema\":\"m3.sorted-batch-allocation.v1\""
                + ",\"status\":\"UNAVAILABLE\",\"reason\":\"" + reason + "\"}");
    }
}
