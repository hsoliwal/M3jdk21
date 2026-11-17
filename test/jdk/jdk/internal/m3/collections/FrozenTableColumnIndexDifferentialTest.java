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
package jdk.internal.m3.collections;

import static jdk.internal.m3.collections.FrozenTableColumnSources.entries;
import static jdk.internal.m3.collections.FrozenTableColumnSources.inject;
import static jdk.internal.m3.collections.FrozenTableColumnSources.items;
import static jdk.internal.m3.collections.FrozenTableColumnSources.plan;
import static jdk.internal.m3.collections.FrozenTableColumnSources.scalePlan;
import static jdk.internal.m3.collections.FrozenTableColumnSources.source;
import static jdk.internal.m3.collections.FrozenTableColumnViewChecks.field;
import static jdk.internal.m3.collections.FrozenTableColumnViewChecks.mask;
import static jdk.internal.m3.collections.FrozenTableColumnViewChecks.outcome;
import static jdk.internal.m3.collections.FrozenTableColumnViewChecks.sameElements;
import static jdk.internal.m3.collections.FrozenTableColumnViewChecks.sameLanes;
import static jdk.internal.m3.collections.FrozenTableColumnViewChecks.sameResult;
import static jdk.internal.m3.collections.FrozenTableColumnViewChecks.sameShape;
import static jdk.internal.m3.collections.FrozenTableColumnViewChecks.sameView;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jdk.internal.m3.collections.FrozenTableColumnSources.Arm;
import jdk.internal.m3.collections.FrozenTableColumnSources.ArmedKey;
import jdk.internal.m3.collections.FrozenTableColumnSources.Id;
import jdk.internal.m3.collections.FrozenTableColumnSources.Key;
import jdk.internal.m3.collections.FrozenTableColumnSources.Run;
import jdk.internal.m3.collections.FrozenTableColumnSources.Trace;
import jdk.internal.m3.collections.FrozenTableColumnViewChecks.Outcome;
import org.junit.jupiter.api.Test;

//FLLDCIM: LEETCODIFY frozen-table-column: CSR column slices vs master column scan
/** Differential proof of the frozen-table-column leaf against the pre-leaf master copy. */
final class FrozenTableColumnIndexDifferentialTest {
    private static final long SEED = 0x434F4C554D4E4CL;
    private static final int TRIALS = 3000;
    private static final int SIDE_TRIALS = 400;
    private static final int INJECT_PERCENT = 5;
    private static final int READERS = 8;
    private static final int SAMPLE_STRIDE = 20;
    private static final List<String> EXPOSED = List.of("$2", "$2$1", "$5", "$1", "$3", "$3", "$4");
    private static final RuntimeException INJECTED = new IllegalStateException("injected key");
    private static final System.Logger LOG =
            System.getLogger(FrozenTableColumnIndexDifferentialTest.class.getName());
    private static FrozenTable<Object, Object, Object> shared;

    private interface Table {
        Map<Object, Object> column(Object key);
    }

    @Test void randomTablesMatchMasterColumnScan() {
        SplittableRandom random = new SplittableRandom(SEED);
        int[] counts = new int[2];
        IntStream.range(0, TRIALS).forEach(trial -> trial(random, counts));
        assertTrue(counts[0] > TRIALS / 2, "built tables " + counts[0]);
        assertTrue(counts[1] > 0, "injected failures " + counts[1]);
    }

    private static void trial(SplittableRandom random, int[] counts) {
        List<Map.Entry<Object, Map<Object, Object>>> items =
                items(plan(random), Key::new);
        boolean inject = random.nextInt(100) < INJECT_PERCENT;
        if (inject) {
            int kind = random.nextInt(FrozenTableColumnSources.FAILURE_KINDS);
            inject(random, items, kind, INJECTED);
        }
        int result = sameCopy(entries(items));
        assertEquals(inject ? 1 : 0, result);
        counts[result]++;
    }

    /** Builds both sides from one source; returns 0 when built and compared, 1 on failure. */
    private static int sameCopy(Map<Object, Map<Object, Object>> source) {
        Outcome actual = outcome(() -> FrozenTable.copyOf(source));
        Outcome expected = outcome(() -> FrozenTableColumnScanReference.copyOf(source));
        if (!sameShape(actual, expected)) {
            assertEquals(expected.failure() == INJECTED, actual.failure() == INJECTED);
            return 1;
        }
        sameTable(cast(actual.value()), cast(expected.value()));
        return 0;
    }

    @Test void injectedFailuresMatchMaster() {
        SplittableRandom random = new SplittableRandom(SEED + 1);
        IntStream.range(0, SIDE_TRIALS).forEach(trial -> {
            List<Map.Entry<Object, Map<Object, Object>>> items =
                    items(plan(random), Key::new);
            int kind = trial % FrozenTableColumnSources.FAILURE_KINDS;
            inject(random, items, kind, INJECTED);
            assertEquals(1, sameCopy(entries(items)));
        });
    }

    @Test void emptyTablesMatchMaster() {
        sameTable(new FrozenTable<>(), new FrozenTableColumnScanReference<>());
        List<Run> blank = List.of(new Run(0, new int[0]), new Run(1, new int[0]));
        Stream.of(List.<Run>of(), blank).forEach(plan -> {
            Map<Object, Map<Object, Object>> source = source(plan, Key::new);
            assertEquals(0, sameCopy(source));
        });
    }

    private static void sameTable(FrozenTable<Object, Object, Object> actual,
            FrozenTableColumnScanReference<Object, Object, Object> expected) {
        assertEquals(expected.size(), actual.size());
        sameElements(actual.columnKeySet(), expected.columnKeySet());
        sameElements(actual.rowKeySet(), expected.rowKeySet());
        List<Object> rows = probes(expected.rowKeySet(), new Key(-1), new Key(999), 3, "row");
        List<Object> columns = probes(expected.columnKeySet(), new Key(100), new Key(101),
                new Key(102), 5, null);
        columns.forEach(key -> sameColumn(actual, expected, key, rows));
        sameLanes(actual, expected);
    }

    /** Representative keys, fresh equal twins, then absent, wrong-type and null probes. */
    private static List<Object> probes(Iterable<Object> keys, Object... extra) {
        List<Object> probes = new ArrayList<>();
        keys.forEach(probes::add);
        keys.forEach(key -> probes.add(key instanceof Key known ? new Key(known.id()) : key));
        probes.addAll(Arrays.asList(extra));
        return probes;
    }

    private static void sameColumn(FrozenTable<Object, Object, Object> actual,
            FrozenTableColumnScanReference<Object, Object, Object> expected, Object key,
            List<Object> rows) {
        Outcome mine = outcome(() -> actual.column(key));
        Outcome theirs = outcome(() -> expected.column(key));
        if (sameShape(mine, theirs)) {
            sameView(cast(mine.value()), cast(theirs.value()), rows);
            assertNotSame(actual.column(key), actual.column(key));
            assertNotSame(expected.column(key), expected.column(key));
        }
    }

    @Test void callbackTraceMatchesMaster() {
        SplittableRandom random = new SplittableRandom(SEED + 2);
        IntStream.range(0, SIDE_TRIALS)
                .forEach(trial -> sameTrace(plan(random)));
    }

    private static void sameTrace(List<Run> plan) {
        Trace left = new Trace();
        Trace right = new Trace();
        FrozenTable<Object, Object, Object> actual =
                FrozenTable.copyOf(source(plan, left::key));
        FrozenTableColumnScanReference<Object, Object, Object> expected =
                FrozenTableColumnScanReference.copyOf(source(plan, right::key));
        assertEquals(right.log, left.log);
        left.log.clear();
        right.log.clear();
        List<Object> mine = script(actual::column, left);
        List<Object> theirs = script(expected::column, right);
        assertEquals(right.log, left.log);
        assertEquals(theirs, mine);
    }

    /** Lookups, iteration, JDK map equality and hashing on every probed column view. */
    private static List<Object> script(Table table, Trace keys) {
        List<Object> rows = IntStream.rangeClosed(0, 12).<Object>mapToObj(keys::key).toList();
        List<Object> results = new ArrayList<>();
        IntStream.rangeClosed(0, 14).mapToObj(keys::key).forEach(key -> {
            Map<Object, Object> view = table.column(key);
            rows.forEach(row -> results.add(view.get(row)));
            rows.forEach(row -> results.add(view.containsKey(row)));
            view.entrySet().forEach(entry -> results.add(entry.getValue()));
            Map<Object, Object> copy = new LinkedHashMap<>(view);
            results.addAll(List.of(view.equals(copy), copy.equals(view), view.hashCode()));
        });
        return results;
    }

    @Test void throwingCallbacksMatchMaster() {
        SplittableRandom random = new SplittableRandom(SEED + 3);
        int[] failures = new int[1];
        IntStream.range(0, SIDE_TRIALS)
                .forEach(trial -> sameArmed(plan(random), failures));
        assertTrue(failures[0] > 0, "armed failures " + failures[0]);
    }

    private static void sameArmed(List<Run> plan, int[] failures) {
        Arm arm = new Arm();
        Map<Object, Map<Object, Object>> source =
                source(plan, id -> new ArmedKey(id, arm));
        FrozenTable<Object, Object, Object> actual = FrozenTable.copyOf(source);
        FrozenTableColumnScanReference<Object, Object, Object> expected =
                FrozenTableColumnScanReference.copyOf(source);
        expected.columnKeySet().forEach(
                key -> failures[0] += armedColumn(actual, expected, (ArmedKey) key, arm));
    }

    /** Armed probes: iterating stays callback free, lookups throw the arm's own failure. */
    private static int armedColumn(FrozenTable<Object, Object, Object> actual,
            FrozenTableColumnScanReference<Object, Object, Object> expected, ArmedKey key,
            Arm arm) {
        Map<Object, Object> mine = actual.column(key);
        Map<Object, Object> theirs = expected.column(key);
        ArmedKey probe = key.twin();
        arm.armed = true;
        sameElements(mine.entrySet(), theirs.entrySet());
        int failures = armed(outcome(() -> actual.column(probe)),
                outcome(() -> expected.column(probe)), arm)
                + armed(outcome(() -> mine.get(probe)), outcome(() -> theirs.get(probe)), arm);
        arm.armed = false;
        sameView(mine, theirs, new ArrayList<>(expected.rowKeySet()));
        return failures;
    }

    /** Same armed outcome on both sides; counts 1 when both threw the arm's own failure. */
    private static int armed(Outcome actual, Outcome expected, Arm arm) {
        if (sameShape(actual, expected)) {
            return 0;
        }
        assertSame(arm.failure, expected.failure());
        assertSame(arm.failure, actual.failure());
        return 1;
    }

    @Test void serializationNamesMatchMaster() {
        List<Run> dense = List.of(new Run(0, new int[] {0, 1}), new Run(1, new int[] {1, 0}));
        List<Run> sparse = List.of(new Run(0, new int[] {0}), new Run(1, new int[] {1}),
                new Run(2, new int[] {2}));
        Stream.of(dense, sparse).forEach(plan -> {
            Map<Object, Map<Object, Object>> source = source(plan, Key::new);
            List<String> mine = names(exposed(FrozenTable.copyOf(source)));
            List<String> theirs = names(exposed(FrozenTableColumnScanReference.copyOf(source)));
            assertEquals(EXPOSED, theirs);
            assertEquals(theirs, mine);
        });
    }

    private static List<Object> exposed(FrozenTable<Object, Object, Object> table) {
        return List.of(table.column(new Key(0)), table.column(new Key(0)).entrySet(),
                table.column(new Key(99)), table.row(new Key(0)), table.rowKeySet(),
                table.columnKeySet(), table.rowMap());
    }

    private static List<Object> exposed(
            FrozenTableColumnScanReference<Object, Object, Object> table) {
        return List.of(table.column(new Key(0)), table.column(new Key(0)).entrySet(),
                table.column(new Key(99)), table.row(new Key(0)), table.rowKeySet(),
                table.columnKeySet(), table.rowMap());
    }

    /** Anonymous-class suffix named by the NotSerializableException of each exposed view. */
    private static List<String> names(List<Object> exposed) {
        return exposed.stream().map(view -> {
            try (ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream())) {
                out.writeObject(view);
                return "serializable";
            } catch (NotSerializableException failure) {
                return failure.getMessage().substring(failure.getMessage().indexOf('$'));
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }).toList();
    }

    @Test void frozenMaskMatchesMaster() {
        assertArrayEquals(mask(new FrozenTableColumnScanReference<>()), mask(new FrozenTable<>()));
        SplittableRandom random = new SplittableRandom(SEED + 4);
        IntStream.concat(IntStream.of(0, 1, 63, 64, 65, 127, 128, 129), random.ints(16, 2, 300))
                .forEach(size -> {
                    List<Run> line = List.of(new Run(0, IntStream.range(0, size).toArray()));
                    List<Run> diagonal = IntStream.range(0, size)
                            .mapToObj(at -> new Run(at, new int[] {at})).toList();
                    Stream.of(line, diagonal).forEach(plan -> sameMask(plan, size));
                });
    }

    private static void sameMask(List<Run> plan, int size) {
        Map<Object, Map<Object, Object>> source = source(plan, Id::new);
        FrozenTable<Object, Object, Object> actual = FrozenTable.copyOf(source);
        assertEquals(size, actual.size());
        assertEquals((size + 63) / 64, mask(actual).length);
        assertArrayEquals(mask(FrozenTableColumnScanReference.copyOf(source)), mask(actual));
    }

    @Test void retiredLaneAndFinalFields() {
        assertThrows(NoSuchFieldException.class,
                () -> FrozenTable.class.getDeclaredField("sourceOrderLane"));
        assertTrue(Arrays.stream(FrozenTable.class.getDeclaredFields())
                .allMatch(field -> Modifier.isFinal(field.getModifiers())));
    }

    @Test void racyPublicationIteratesEveryColumn() {
        List<Run> plan = scalePlan(new SplittableRandom(SEED + 5),
                200, 150, 6000);
        Map<Object, Map<Object, Object>> source = source(plan, Id::new);
        FrozenTableColumnScanReference<Object, Object, Object> expected =
                FrozenTableColumnScanReference.copyOf(source);
        List<Object> keys = new ArrayList<>(expected.columnKeySet());
        publishAndRead(source, keys, snapshot(expected::column, keys));
    }

    /** Publishes through the plain static field while the readers spin on it. */
    private static void publishAndRead(Map<Object, Map<Object, Object>> source, List<Object> keys,
            List<List<Object>> want) {
        ExecutorService pool = Executors.newFixedThreadPool(READERS);
        try {
            List<Future<List<List<Object>>>> seen = IntStream.range(0, READERS)
                    .mapToObj(reader -> pool.submit(() -> snapshot(awaitShared()::column, keys)))
                    .toList();
            shared = FrozenTable.copyOf(source);
            seen.forEach(reader -> sameSnapshot(reader, want));
        } finally {
            pool.shutdownNow();
            shared = null;
        }
    }

    private static FrozenTable<Object, Object, Object> awaitShared() {
        FrozenTable<Object, Object, Object> table = shared;
        while (table == null) {
            Thread.yield();
            table = shared;
        }
        return table;
    }

    private static void sameSnapshot(Future<List<List<Object>>> reader, List<List<Object>> want) {
        try {
            List<List<Object>> got = reader.get(60, TimeUnit.SECONDS);
            assertEquals(want.size(), got.size());
            IntStream.range(0, want.size()).forEach(at -> sameElements(got.get(at), want.get(at)));
        } catch (Exception failure) {
            throw new AssertionError("reader", failure);
        }
    }

    /** Per column: row key, value, row key, value ... in view order. */
    private static List<List<Object>> snapshot(Table table, List<Object> keys) {
        return keys.stream().map(key -> table.column(key).entrySet().stream()
                .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue())).toList()).toList();
    }

    @Test void largeSparseTableMatchesModel() {
        List<Run> plan = scalePlan(new SplittableRandom(SEED + 6),
                1000, 2000, 200_000);
        Map<Object, Map<Object, Object>> source = source(plan, Id::new);
        FrozenTable<Object, Object, Object> actual = FrozenTable.copyOf(source);
        assertFalse((boolean) field(actual, "denseOrder"));
        Map<Object, Map<Object, Object>> model = columnModel(source);
        long start = System.nanoTime();
        List<Object> keys = new ArrayList<>(actual.columnKeySet());
        sameElements(keys, model.keySet());
        keys.forEach(key -> sameElements(actual.column(key).entrySet(), model.get(key).entrySet()));
        long patched = System.nanoTime() - start;
        long master = sampleMaster(actual, FrozenTableColumnScanReference.copyOf(source), keys);
        LOG.log(System.Logger.Level.INFO, "scale probe cells={0} columns={1} patchedAllNs={2}"
                + " masterSampleNs={3} sampleStride={4}", 200_000, keys.size(), patched, master,
                SAMPLE_STRIDE);
    }

    /** Column model in source order: first column key instance, then each cell's row key. */
    private static Map<Object, Map<Object, Object>> columnModel(
            Map<Object, Map<Object, Object>> source) {
        Map<Object, Map<Object, Object>> model = new LinkedHashMap<>();
        source.forEach((row, cells) -> cells.forEach((column, value) ->
                model.computeIfAbsent(column, key -> new LinkedHashMap<>()).put(row, value)));
        return model;
    }

    private static long sampleMaster(FrozenTable<Object, Object, Object> actual,
            FrozenTableColumnScanReference<Object, Object, Object> expected, List<Object> keys) {
        long start = System.nanoTime();
        IntStream.range(0, keys.size()).filter(at -> at % SAMPLE_STRIDE == 0)
                .mapToObj(keys::get).forEach(key -> sameElements(actual.column(key).entrySet(),
                        expected.column(key).entrySet()));
        return System.nanoTime() - start;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) { return (T) value; }
}
