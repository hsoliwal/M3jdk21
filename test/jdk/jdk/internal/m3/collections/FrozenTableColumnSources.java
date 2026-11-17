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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/** Source fixtures of FrozenTableColumnIndexDifferentialTest: keys, plans and list maps. */
final class FrozenTableColumnSources {
    static final int FAILURE_KINDS = 6;
    private static final int MAX_ROWS = 12;
    private static final int MAX_COLUMNS = 14;
    private static final int MAX_RUN = 3;
    private static final int EMPTY_ROW_ODDS = 20;
    private static final double[] FILL = {0.2, 0.2, 0.8, 0.8, 0.8, 1.0, 0.0, 0.8, 0.8, 0.0};
    private static final int SINGLE_CELL = 6;
    private static final int SINGLE_ROW = 7;
    private static final int SINGLE_COLUMN = 8;

    private FrozenTableColumnSources() { }

    /** Collision-heavy key: equal by id, constant hash. */
    record Key(int id) {
        @Override public int hashCode() { return 7; }
    }

    /** Well-spread key of the scale probes: equal by id, record hash. */
    record Id(int id) { }

    /** Cell value: one instance per cell, equal by id. */
    record Val(int id) { }

    /** One source row entry in source order: a row id and its column ids. */
    record Run(int row, int[] columns) { }

    /** Callback log of one key universe; both universes hand out the same serials. */
    static final class Trace {
        final List<String> log = new ArrayList<>();
        private int serial;

        LoggedKey key(int id) { return new LoggedKey(id, serial++, log); }
    }

    /** Key that records every hashCode and equals call in its universe's log. */
    static final class LoggedKey {
        private final int id;
        private final int serial;
        private final List<String> log;

        LoggedKey(int id, int serial, List<String> log) {
            this.id = id;
            this.serial = serial;
            this.log = log;
        }

        @Override public int hashCode() {
            log.add("h " + tag());
            return id % 3;
        }

        @Override public boolean equals(Object other) {
            String peer = other instanceof LoggedKey logged ? logged.tag() : String.valueOf(other);
            log.add("e " + tag() + " " + peer);
            return other instanceof LoggedKey key && key.id == id;
        }

        @Override public String toString() { return "L" + id; }

        private String tag() { return id + "#" + serial; }
    }

    /** Shared switch of armed keys; armed callbacks throw the one failure instance. */
    static final class Arm {
        final RuntimeException failure = new IllegalStateException("armed key callback");
        boolean armed;
    }

    /** Key whose hashCode and equals throw the arm's failure while armed. */
    static final class ArmedKey {
        private final int id;
        private final Arm arm;

        ArmedKey(int id, Arm arm) {
            this.id = id;
            this.arm = arm;
        }

        @Override public int hashCode() {
            check();
            return 7;
        }

        @Override public boolean equals(Object other) {
            check();
            return other instanceof ArmedKey key && key.id == id;
        }

        @Override public String toString() { return "A" + id; }

        ArmedKey twin() { return new ArmedKey(id, arm); }

        private void check() {
            if (arm.armed) { throw arm.failure; }
        }
    }

    /** Random shape and fill mode; rows are cut into interleaved runs, so equal rows recur. */
    static List<Run> plan(SplittableRandom random) {
        int mode = random.nextInt(FILL.length);
        int rows = mode == SINGLE_ROW ? 1 : 1 + random.nextInt(MAX_ROWS);
        int columns = mode == SINGLE_COLUMN ? 1 : 1 + random.nextInt(MAX_COLUMNS);
        List<List<Integer>> table = new ArrayList<>();
        IntStream.range(0, rows).forEach(row -> table.add(row(random, columns, FILL[mode])));
        if (mode == SINGLE_CELL) {
            table.get(random.nextInt(rows)).add(random.nextInt(columns));
        }
        return runs(random, table);
    }

    private static List<Integer> row(SplittableRandom random, int columns, double fill) {
        List<Integer> row = new ArrayList<>();
        IntStream.range(0, columns).filter(column -> random.nextDouble() < fill).forEach(row::add);
        Collections.shuffle(row, new Random(random.nextLong()));
        return row;
    }

    private static List<Run> runs(SplittableRandom random, List<List<Integer>> table) {
        List<Run> runs = new ArrayList<>();
        List<Integer> open = new ArrayList<>(IntStream.range(0, table.size())
                .filter(row -> !table.get(row).isEmpty()).boxed().toList());
        while (!open.isEmpty()) {
            int pick = random.nextInt(open.size());
            List<Integer> row = table.get(open.get(pick));
            List<Integer> run = row.subList(0, Math.min(row.size(), 1 + random.nextInt(MAX_RUN)));
            runs.add(new Run(open.get(pick), run.stream().mapToInt(Integer::intValue).toArray()));
            run.clear();
            if (row.isEmpty()) { open.remove(pick); }
            if (random.nextInt(EMPTY_ROW_ODDS) == 0) {
                runs.add(new Run(random.nextInt(table.size()), new int[0]));
            }
        }
        return runs;
    }

    /** Unique random coordinates as single-cell runs in random source order. */
    static List<Run> scalePlan(SplittableRandom random, int rows, int columns, int cells) {
        Set<Long> seen = new HashSet<>();
        List<Run> plan = new ArrayList<>();
        while (plan.size() < cells) {
            int row = random.nextInt(rows);
            int column = random.nextInt(columns);
            if (seen.add((long) row * columns + column)) {
                plan.add(new Run(row, new int[] {column}));
            }
        }
        return plan;
    }

    /** List-backed source; one key factory serves rows and columns in creation order. */
    static Map<Object, Map<Object, Object>> source(List<Run> plan, IntFunction<Object> keys) {
        return entries(items(plan, keys));
    }

    /** Source entries: a fresh row key per run, then fresh column keys and values per cell. */
    static List<Map.Entry<Object, Map<Object, Object>>> items(List<Run> plan,
            IntFunction<Object> keys) {
        List<Map.Entry<Object, Map<Object, Object>>> items = new ArrayList<>();
        int[] serial = {0};
        plan.forEach(run -> {
            Object rowKey = keys.apply(run.row());
            List<Map.Entry<Object, Object>> cells = new ArrayList<>();
            IntStream.of(run.columns())
                    .forEach(column -> cells.add(entry(keys.apply(column), new Val(serial[0]++))));
            items.add(entry(rowKey, entries(cells)));
        });
        return items;
    }

    /** Inserts one failing entry at a random position: duplicate, null part or throwing key. */
    static void inject(SplittableRandom random, List<Map.Entry<Object, Map<Object, Object>>> items,
            int kind, RuntimeException failure) {
        Map<Object, Object> cell = entries(List.of(entry(new Key(0), new Val(-1))));
        Map.Entry<Object, Map<Object, Object>> bad = switch (kind) {
            case 0 -> duplicate(random, items);
            case 1 -> entry(null, cell);
            case 2 -> entry(new Key(0), null);
            case 3 -> entry(new Key(0), entries(List.of(entry(null, new Val(-1)))));
            case 4 -> entry(new Key(0), entries(List.of(entry(new Key(0), null))));
            default -> entry(new ThrowingKey(failure), cell);
        };
        items.add(random.nextInt(items.size() + 1), bad);
    }

    private static Map.Entry<Object, Map<Object, Object>> duplicate(SplittableRandom random,
            List<Map.Entry<Object, Map<Object, Object>>> items) {
        List<Map.Entry<Object, Map<Object, Object>>> filled = items.stream()
                .filter(item -> !item.getValue().isEmpty()).toList();
        if (filled.isEmpty()) { return entry(null, Map.of()); }
        Map.Entry<Object, Map<Object, Object>> item = filled.get(random.nextInt(filled.size()));
        Key column = (Key) item.getValue().keySet().iterator().next();
        Map.Entry<Object, Object> again = entry(new Key(column.id()), new Val(-2));
        return entry(new Key(((Key) item.getKey()).id()), entries(List.of(again)));
    }

    /** Key whose hashCode and equals always throw one shared failure. */
    private record ThrowingKey(RuntimeException failure) {
        @Override public int hashCode() { throw failure; }
        @Override public boolean equals(Object other) { throw failure; }
    }

    static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    /** Map whose entry set iterates the given list as is, duplicates and nulls included. */
    static <K, V> Map<K, V> entries(List<Map.Entry<K, V>> items) {
        return new AbstractMap<>() {
            @Override public Set<Entry<K, V>> entrySet() {
                return new AbstractSet<>() {
                    @Override public int size() { return items.size(); }
                    @Override public Iterator<Entry<K, V>> iterator() { return items.iterator(); }
                };
            }
        };
    }
}
