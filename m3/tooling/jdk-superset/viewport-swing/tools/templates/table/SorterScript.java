/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
// Template of gen_atom_tests.py / gen_atom_perf.py: the generated copy replaces this header.

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.IntPredicate;

import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import sun.swing.table.M3SorterLanes;

/**
 * Patterns SORT_ORDER / MODEL_EVENT / INDEX_MAPPING / INTERFACE_CONTRACT: {@code M3SorterLanes} driven through a
 * {@link TableHost} (the {@code DefaultRowSorter} members the lanes call, written over a {@code DefaultTableModel})
 * against a stock {@code TableRowSorter} on the same model, keys and filter. Both receive the same sorter calls
 * ({@code setSortKeys}, {@code setRowFilter}, {@code rowsInserted}, {@code rowsDeleted}, {@code rowsUpdated},
 * {@code allRowsChanged}, {@code modelStructureChanged}). After every step: transformed or not, the view order, both
 * conversions, the {@code sorted} flag and the previous-order payload of every SORTED event.
 */
final class SorterScript {

    static final int MAX_ROWS = 40;
    static final int STEPS = 160;
    static final int KEY_RANGE = 9;
    static final int MAX_RUN = 3;
    static final int KEY = 0;
    static final int ID = 1;
    /** The stock cutoff: more than a tenth of the view's rows changed sorts everything. */
    static final int CUTOFF_DIVISOR = 10;
    static final int[] LANE_SIZES = {0, 1, 2, 3, 31, 32, 33, 64, 65, 100, 257};
    static final int SHAPES = 6;
    static final int TRIALS = 40;

    enum Mix {
        KEYS(6, 1, 1, 1, 1, 0, 0), FILTER(2, 6, 1, 1, 1, 0, 0), INSERT(1, 1, 7, 0, 0, 0, 0), DELETE(1, 1, 0, 7, 0, 0, 0),
        UPDATE(1, 1, 0, 0, 7, 0, 0), BULK(1, 1, 3, 3, 3, 0, 0), STRUCTURE(2, 2, 1, 1, 1, 3, 0),
        MIXED(2, 2, 2, 2, 2, 1, 1), INSERT_ONLY(0, 0, 1, 0, 0, 0, 0);

        final int[] weights;

        Mix(int... weights) {
            this.weights = weights;
        }
    }

    /** {@code DefaultRowSorter}'s members as the lanes see them, over the script's model, keys and filter. */
    static final class TableHost implements M3SorterLanes.Host {
        final DefaultTableModel model;
        final M3SorterLanes lanes = new M3SorterLanes();
        List<RowSorter.SortKey> keys = Collections.emptyList();
        IntPredicate filter;
        boolean sorted;
        final List<String> calls = new ArrayList<>();
        final List<int[]> changes = new ArrayList<>();
        /** How often the lanes called each member: the reach check of the Host atoms (an interface has no code). */
        final java.util.Map<String, Integer> called = new java.util.HashMap<>();

        TableHost(DefaultTableModel model) {
            this.model = model;
            lanes.bind(this);
        }

        int key(int row) {
            return (Integer) model.getValueAt(row, KEY);
        }

        /** The stock {@code DefaultRowSorter.compare}: keys in order (UNSORTED by model index), ties by model index. */
        @Override
        public int compare(int model1, int model2) {
            called.merge("compare", 1, Integer::sum);
            for (RowSorter.SortKey k : keys) {
                int result;
                if (k.getSortOrder() == SortOrder.UNSORTED) {
                    result = model1 - model2;
                } else {
                    Integer v1 = (Integer) model.getValueAt(model1, k.getColumn());
                    Integer v2 = (Integer) model.getValueAt(model2, k.getColumn());
                    result = v1.compareTo(v2);
                    if (k.getSortOrder() == SortOrder.DESCENDING) {
                        result *= -1;
                    }
                }
                if (result != 0) {
                    return result;
                }
            }
            return model1 - model2;
        }

        @Override
        public boolean include(int modelRow) {
            called.merge("include", 1, Integer::sum);
            return filter == null || filter.test(key(modelRow));
        }

        @Override
        public int modelRowCount() {
            called.merge("modelRowCount", 1, Integer::sum);
            return model.getRowCount();
        }

        @Override
        public boolean unsorted() {
            called.merge("unsorted", 1, Integer::sum);
            return keys.isEmpty() || keys.get(0).getSortOrder() == SortOrder.UNSORTED;
        }

        @Override
        public boolean filtered() {
            called.merge("filtered", 1, Integer::sum);
            return filter != null;
        }

        @Override
        public boolean sorted() {
            called.merge("sorted()", 1, Integer::sum);
            return sorted;
        }

        @Override
        public void sorted(boolean sorted) {
            called.merge("sorted(boolean)", 1, Integer::sum);
            this.sorted = sorted;
        }

        @Override
        public void prepareSort() {
            called.merge("prepareSort", 1, Integer::sum);
            calls.add("prepareSort");
        }

        @Override
        public void prepareResort() {
            called.merge("prepareResort", 1, Integer::sum);
            calls.add("prepareResort");
        }

        @Override
        public void sort() {
            called.merge("sort", 1, Integer::sum);
            calls.add("sort");
            lanes.sort();
        }

        @Override
        public void changed(int[] lastViewToModel) {
            called.merge("changed", 1, Integer::sum);
            calls.add("changed");
            changes.add(lastViewToModel.clone());
        }

        /** The stock {@code setSortKeys} tail: normalized keys, and a sort only when they changed. */
        void setKeys(List<RowSorter.SortKey> next) {
            List<RowSorter.SortKey> normalized = next == null || next.isEmpty() ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(next));
            boolean changed = !normalized.equals(keys);
            keys = normalized;
            if (changed) {
                lanes.sortKeysChanged();
            }
        }
    }

    static final VarHandle VIEW_TO_MODEL;
    static final VarHandle MODEL_TO_VIEW;
    static final VarHandle SORTED;
    static final VarHandle ROW_MODEL_INDEX;

    static {
        try {
            Class<?> sorter = Class.forName("javax.swing.DefaultRowSorter");
            Class<?> row = Class.forName("javax.swing.DefaultRowSorter$Row");
            VIEW_TO_MODEL = AtomSupport.field(sorter, "viewToModel", row.arrayType());
            MODEL_TO_VIEW = AtomSupport.field(sorter, "modelToView", int[].class);
            SORTED = AtomSupport.field(sorter, "sorted", boolean.class);
            ROW_MODEL_INDEX = AtomSupport.field(row, "modelIndex", int.class);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    final Random rnd;
    final DefaultTableModel model = new DefaultTableModel(new Object[] {"key", "id"}, 0) {
        @Override
        public Class<?> getColumnClass(int column) {
            return Integer.class;
        }
    };
    final TableRowSorter<DefaultTableModel> stock;
    final TableHost host;
    final List<int[]> stockChanges = new ArrayList<>();
    int ids;
    int checks;

    SorterScript(String atom) {
        rnd = AtomSupport.random(atom);
        for (int i = rnd.nextInt(MAX_ROWS); i > 0; i--) {
            model.addRow(newRow());
        }
        stock = new TableRowSorter<>(model);
        host = new TableHost(model);
        stock.addRowSorterListener(e -> {
            if (e.getType() == RowSorterEvent.Type.SORTED) {
                int[] previous = new int[e.getPreviousRowCount()];
                for (int i = 0; i < previous.length; i++) {
                    previous[i] = e.convertPreviousRowIndexToModel(i);
                }
                stockChanges.add(previous);
            }
        });
    }

    Object[] newRow() {
        return new Object[] {rnd.nextInt(KEY_RANGE), ids++};
    }

    // ---- the comparison ----------------------------------------------------------------------------------

    int[] stockViewToModel() {
        Object[] rows = (Object[]) VIEW_TO_MODEL.get(stock);
        if (rows == null) {
            return null;
        }
        int[] v2m = new int[rows.length];
        for (int i = 0; i < rows.length; i++) {
            v2m[i] = (int) ROW_MODEL_INDEX.get(rows[i]);
        }
        return v2m;
    }

    void compare(String what) {
        checks++;
        M3SorterLanes lanes = host.lanes;
        int[] v2m = stockViewToModel();
        assertEquals(v2m != null, lanes.transformed(), what + ": transformed");
        if (v2m != null) {
            assertEquals(v2m.length, lanes.viewRowCount(), what + ": view rows");
            assertArrayEquals(v2m, lanes.snapshot(), what + ": view order");
            for (int v = 0; v < v2m.length; v++) {
                assertEquals(v2m[v], lanes.modelIndex(v), what + ": modelIndex " + v);
                assertEquals(stock.convertRowIndexToModel(v), lanes.modelIndex(v));
            }
            int[] m2v = (int[]) MODEL_TO_VIEW.get(stock);
            for (int m = 0; m < model.getRowCount(); m++) {
                assertEquals(m2v[m], lanes.viewIndex(m), what + ": viewIndex " + m);
                assertEquals(stock.convertRowIndexToView(m), lanes.viewIndex(m));
            }
        } else {
            assertEquals(0, lanes.snapshot().length, what + ": no order while untransformed");
            assertEquals(model.getRowCount(), stock.getViewRowCount());
        }
        assertEquals((boolean) SORTED.get(stock), host.sorted, what + ": sorted flag");
        assertEquals(stockChanges.size(), host.changes.size(), what + ": SORTED events");
        for (int i = 0; i < stockChanges.size(); i++) {
            assertArrayEquals(stockChanges.get(i), host.changes.get(i), what + ": previous order of event " + i);
        }
        stockChanges.clear();
        host.changes.clear();
    }

    // ---- script ---------------------------------------------------------------------------------------------

    List<RowSorter.SortKey> randomKeys() {
        switch (rnd.nextInt(7)) {
            case 0:
                return List.of();
            case 1:
                return List.of(new RowSorter.SortKey(KEY, SortOrder.ASCENDING));
            case 2:
                return List.of(new RowSorter.SortKey(KEY, SortOrder.DESCENDING));
            case 3:
                return List.of(new RowSorter.SortKey(KEY, SortOrder.UNSORTED));
            case 4:
                return List.of(new RowSorter.SortKey(KEY, SortOrder.ASCENDING), new RowSorter.SortKey(ID, SortOrder.DESCENDING));
            case 5:
                return List.of(new RowSorter.SortKey(ID, SortOrder.DESCENDING));
            default:
                return List.of(new RowSorter.SortKey(KEY, SortOrder.DESCENDING), new RowSorter.SortKey(ID, SortOrder.UNSORTED));
        }
    }

    IntPredicate randomFilter() {
        int pick = rnd.nextInt(6);
        int pivot = rnd.nextInt(KEY_RANGE);
        switch (pick) {
            case 0:
                return null;
            case 1:
                return k -> k % 2 == 0;
            case 2:
                return k -> k < pivot;
            case 3:
                return k -> k != pivot;
            case 4:
                return k -> false;
            default:
                return k -> true;
        }
    }

    static RowFilter<DefaultTableModel, Integer> stockFilter(IntPredicate p) {
        return p == null ? null : new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                return p.test((Integer) e.getValue(KEY));
            }
        };
    }

    void keys() {
        List<RowSorter.SortKey> k = randomKeys();
        stock.setSortKeys(k);
        host.setKeys(k);
        compare("setSortKeys " + k);
    }

    void filter() {
        IntPredicate p = randomFilter();
        stock.setRowFilter(stockFilter(p));
        host.filter = p;
        host.sort();
        compare("setRowFilter");
    }

    int run(boolean bulk) {
        int n = model.getRowCount();
        return bulk ? 2 + n / CUTOFF_DIVISOR + rnd.nextInt(MAX_RUN) : 1 + rnd.nextInt(MAX_RUN);
    }

    void insert(boolean bulk) {
        int n = model.getRowCount();
        int first = rnd.nextInt(n + 1);
        int count = run(bulk);
        for (int j = 0; j < count; j++) {
            model.insertRow(first + j, newRow());
        }
        stock.rowsInserted(first, first + count - 1);
        host.lanes.rowsInserted(first, first + count - 1);
        compare("rowsInserted " + first + ".." + (first + count - 1));
    }

    void delete(boolean bulk) {
        int n = model.getRowCount();
        if (n == 0) {
            insert(bulk);
            return;
        }
        int first = rnd.nextInt(n);
        int last = Math.min(n - 1, first + run(bulk) - 1);
        for (int r = last; r >= first; r--) {
            model.removeRow(r);
        }
        stock.rowsDeleted(first, last);
        host.lanes.rowsDeleted(first, last);
        compare("rowsDeleted " + first + ".." + last);
    }

    void update(boolean bulk) {
        int n = model.getRowCount();
        if (n == 0) {
            insert(bulk);
            return;
        }
        if (rnd.nextInt(MAX_RUN * MAX_RUN) == 0) {
            boolean on = !stock.getSortsOnUpdates();
            stock.setSortsOnUpdates(on);
        }
        int first = rnd.nextInt(n);
        updateRange(first, Math.min(n - 1, first + run(bulk) - 1));
    }

    /** New keys for rows first..last, then rowsUpdated on both sides (sortsOnUpdates as the stock sorter has it). */
    void updateRange(int first, int last) {
        for (int r = first; r <= last; r++) {
            model.setValueAt(rnd.nextInt(KEY_RANGE), r, KEY);
        }
        stock.rowsUpdated(first, last);
        host.lanes.rowsUpdated(first, last, stock.getSortsOnUpdates());
        compare("rowsUpdated " + first + ".." + last + " sortsOnUpdates " + stock.getSortsOnUpdates());
    }

    void ensureRows(int min) {
        while (model.getRowCount() < min) {
            model.addRow(newRow());
            stock.rowsInserted(model.getRowCount() - 1, model.getRowCount() - 1);
            host.lanes.rowsInserted(model.getRowCount() - 1, model.getRowCount() - 1);
        }
        compare("rows added");
    }

    void structure() {
        if (rnd.nextBoolean()) {
            for (int r = 0; r < model.getRowCount(); r++) {
                model.setValueAt(rnd.nextInt(KEY_RANGE), r, KEY);
            }
            stock.allRowsChanged();
            host.sort();
            compare("allRowsChanged");
        } else {
            stock.modelStructureChanged();
            host.lanes.clear();
            if (host.unsorted()) {
                host.sort();
            } else {
                host.setKeys(null);
            }
            compare("modelStructureChanged");
        }
    }

    void query() {
        M3SorterLanes lanes = host.lanes;
        if (lanes.transformed()) {
            int[] snap = lanes.snapshot();
            assertNotSame(snap, lanes.snapshot(), "snapshot hands out a copy");
            if (snap.length > 0) {
                snap[0] = -1;
                assertTrue(lanes.modelIndex(0) >= 0, "a snapshot written by the caller leaves the lanes alone");
            }
        }
        compare("query");
    }

    void run(Mix mix, int steps) {
        compare("start");
        int total = Arrays.stream(mix.weights).sum();
        for (int step = 0; step < steps; step++) {
            int op = rnd.nextInt(total);
            int k = 0;
            while (op >= mix.weights[k]) {
                op -= mix.weights[k++];
            }
            boolean bulk = mix == Mix.BULK && rnd.nextBoolean();
            switch (k) {
                case 0 -> keys();
                case 1 -> filter();
                case 2 -> insert(bulk);
                case 3 -> delete(bulk);
                case 4 -> update(bulk);
                case 5 -> structure();
                default -> query();
            }
        }
    }

    /** Sorted keys first so the delta paths (not the full sorts) carry the edits. */
    void runSorted(Mix mix, int steps, boolean filtered) {
        List<RowSorter.SortKey> k = List.of(new RowSorter.SortKey(KEY, SortOrder.ASCENDING));
        stock.setSortKeys(k);
        host.setKeys(k);
        if (filtered) {
            IntPredicate p = key -> key % 3 != 0;
            stock.setRowFilter(stockFilter(p));
            host.filter = p;
            host.sort();
        }
        compare("sorted start");
        run(mix, steps);
    }

    // ---- sorting an int lane with the host's order ---------------------------------------------------

    /** A lane of model indices in one of the shapes TimSort-like sorts treat differently. */
    int[] laneShape(int n, int shape) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        switch (shape) {
            case 0 -> {
            }
            case 1 -> reverse(a, 0, n);
            case 2 -> shuffle(a);
            case 3 -> {
                for (int lo = 0; lo < n; lo += MAX_RUN * MAX_RUN) {
                    reverse(a, lo, Math.min(n, lo + MAX_RUN * MAX_RUN));
                }
            }
            case 4 -> {
                shuffle(a);
                Arrays.sort(a, 0, n / 2);
            }
            default -> {
                for (int i = 0; i + 1 < n; i += 2) {
                    int t = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = t;
                }
            }
        }
        return a;
    }

    void shuffle(int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    static void reverse(int[] a, int lo, int hi) {
        for (int i = lo, j = hi - 1; i < j; i++, j--) {
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    /** A host over {@code n} rows of random keys sorted ascending (or descending), ties by model index. */
    TableHost laneHost(int n, boolean descending) {
        DefaultTableModel m = new DefaultTableModel(new Object[] {"key", "id"}, 0);
        for (int i = 0; i < n; i++) {
            m.addRow(new Object[] {rnd.nextInt(KEY_RANGE), i});
        }
        TableHost h = new TableHost(m);
        h.keys = List.of(new RowSorter.SortKey(KEY, descending ? SortOrder.DESCENDING : SortOrder.ASCENDING));
        return h;
    }

    static Integer[] boxed(int[] a, int n) {
        Integer[] b = new Integer[n];
        for (int i = 0; i < n; i++) {
            b[i] = a[i];
        }
        return b;
    }

    static int[] unboxed(Integer[] b) {
        int[] a = new int[b.length];
        for (int i = 0; i < b.length; i++) {
            a[i] = b[i];
        }
        return a;
    }

    void sortLaneContract() {
        for (int n : LANE_SIZES) {
            for (int shape = 0; shape < SHAPES; shape++) {
                TableHost h = laneHost(n, shape % 2 == 1);
                int extra = MAX_RUN;
                int[] a = Arrays.copyOf(laneShape(n, shape), n + extra);
                for (int i = n; i < n + extra; i++) {
                    a[i] = -1 - i;
                }
                Integer[] expected = boxed(a, n);
                Comparator<Integer> order = h::compare;
                Arrays.sort(expected, order);
                h.lanes.sortLane(a, n);
                assertArrayEquals(unboxed(expected), Arrays.copyOf(a, n), "n " + n + " shape " + shape);
                for (int i = n; i < n + extra; i++) {
                    assertEquals(-1 - i, a[i], "sortLane sorts a[0 .. n) only");
                }
            }
        }
    }

    void runEndContract() {
        MethodHandle runEnd = AtomSupport.virtual(M3SorterLanes.class, "runEnd", int.class, int[].class, int.class, int.class);
        for (int n : LANE_SIZES) {
            for (int shape = 0; shape < SHAPES && n > 0; shape++) {
                TableHost h = laneHost(n, false);
                int[] a = laneShape(n, shape);
                int lo = rnd.nextInt(n);
                int[] before = a.clone();
                int hi = (int) AtomSupport.call(runEnd, h.lanes, a, lo, n);
                int expectedHi = lo + 1;
                boolean descending = lo + 1 < n && h.compare(before[lo + 1], before[lo]) < 0;
                if (lo + 1 < n) {
                    expectedHi = lo + 2;
                    while (expectedHi < n && (descending ? h.compare(before[expectedHi], before[expectedHi - 1]) < 0
                            : h.compare(before[expectedHi], before[expectedHi - 1]) >= 0)) {
                        expectedHi++;
                    }
                }
                assertEquals(expectedHi, hi, "end of the run at " + lo);
                int[] expected = before.clone();
                if (descending) {
                    reverse(expected, lo, hi);
                }
                assertArrayEquals(expected, a, "a strictly descending run is reversed in place, nothing else moves");
            }
        }
    }

    void binaryInsertionContract() {
        MethodHandle bi = AtomSupport.virtual(M3SorterLanes.class, "binaryInsertion", void.class, int[].class, int.class,
                int.class, int.class);
        for (int trial = 0; trial < TRIALS; trial++) {
            int n = 2 + rnd.nextInt(MAX_ROWS);
            TableHost h = laneHost(n, rnd.nextBoolean());
            int[] a = laneShape(n, 2);
            int lo = rnd.nextInt(n - 1);
            int hi = lo + 1 + rnd.nextInt(n - lo - 1) + 1;
            hi = Math.min(hi, n);
            int start = lo + 1 + rnd.nextInt(hi - lo);
            start = Math.min(start, hi);
            Integer[] head = boxed(Arrays.copyOfRange(a, lo, start), start - lo);
            Arrays.sort(head, (Comparator<Integer>) h::compare);
            System.arraycopy(unboxed(head), 0, a, lo, head.length);
            int[] before = a.clone();
            AtomSupport.call(bi, h.lanes, a, lo, hi, start);
            Integer[] expected = boxed(Arrays.copyOfRange(before, lo, hi), hi - lo);
            Arrays.sort(expected, (Comparator<Integer>) h::compare);
            assertArrayEquals(unboxed(expected), Arrays.copyOfRange(a, lo, hi), "a[lo .. hi) sorted");
            assertArrayEquals(Arrays.copyOfRange(before, 0, lo), Arrays.copyOfRange(a, 0, lo));
            assertArrayEquals(Arrays.copyOfRange(before, hi, n), Arrays.copyOfRange(a, hi, n));
        }
    }

    void mergeContract() {
        MethodHandle merge = AtomSupport.virtual(M3SorterLanes.class, "merge", void.class, int[].class, int[].class,
                int.class, int.class, int.class);
        for (int trial = 0; trial < TRIALS; trial++) {
            int n = 2 + rnd.nextInt(MAX_ROWS);
            TableHost h = laneHost(n, rnd.nextBoolean());
            int[] a = laneShape(n, 2);
            int lo = rnd.nextInt(n - 1);
            int hi = lo + 2 + rnd.nextInt(n - lo - 1);
            hi = Math.min(hi, n);
            int mid = lo + 1 + rnd.nextInt(hi - lo - 1);
            Comparator<Integer> order = h::compare;
            for (int[] range : new int[][] {{lo, mid}, {mid, hi}}) {
                Integer[] part = boxed(Arrays.copyOfRange(a, range[0], range[1]), range[1] - range[0]);
                Arrays.sort(part, order);
                System.arraycopy(unboxed(part), 0, a, range[0], part.length);
            }
            int[] before = a.clone();
            AtomSupport.call(merge, h.lanes, a, new int[n], lo, mid, hi);
            Integer[] expected = boxed(Arrays.copyOfRange(before, lo, hi), hi - lo);
            Arrays.sort(expected, order);
            assertArrayEquals(unboxed(expected), Arrays.copyOfRange(a, lo, hi), "merged a[lo .. hi)");
            assertArrayEquals(Arrays.copyOfRange(before, 0, lo), Arrays.copyOfRange(a, 0, lo));
            assertArrayEquals(Arrays.copyOfRange(before, hi, n), Arrays.copyOfRange(a, hi, n));
        }
    }

    void binarySearchContract() {
        MethodHandle bs = AtomSupport.virtual(M3SorterLanes.class, "binarySearch", int.class, int[].class, int.class);
        for (int trial = 0; trial < TRIALS; trial++) {
            int n = 1 + rnd.nextInt(MAX_ROWS);
            TableHost h = laneHost(n, rnd.nextBoolean());
            int key = rnd.nextInt(n);
            Comparator<Integer> order = h::compare;
            Integer[] others = new Integer[n - 1];
            for (int i = 0, j = 0; i < n; i++) {
                if (i != key) {
                    others[j++] = i;
                }
            }
            Arrays.sort(others, order);
            int expected = Arrays.binarySearch(others, key, order);
            assertTrue(expected < 0, "the key is never in the lane");
            assertEquals(expected, (int) AtomSupport.call(bs, h.lanes, unboxed(others), key), "insertion point of " + key);
        }
    }

    // ---- the Host members, each through what the lanes do with it -----------------------------------------

    void hostContract(String member) {
        TableHost h = host;
        M3SorterLanes lanes = h.lanes;
        switch (member) {
            case "compare" -> {
                runSorted(Mix.INSERT, STEPS / 2, false);
                for (int v = 0; v + 1 < (lanes.transformed() ? lanes.viewRowCount() : 0); v++) {
                    assertTrue(h.compare(lanes.modelIndex(v), lanes.modelIndex(v + 1)) < 0, "view order follows compare");
                }
            }
            case "include" -> {
                runSorted(Mix.INSERT, STEPS / 2, true);
                for (int m = 0; m < model.getRowCount(); m++) {
                    assertEquals(!h.include(m), lanes.viewIndex(m) == -1, "a row is in the view iff include");
                }
            }
            case "modelRowCount" -> {
                runSorted(Mix.INSERT_ONLY, STEPS / 2, false);
                assertTrue(lanes.transformed());
                assertEquals(h.modelRowCount(), lanes.viewRowCount(), "unfiltered: one view row per model row");
            }
            case "unsorted" -> {
                stock.setSortKeys(List.of());
                h.setKeys(List.of());
                stock.sort();
                h.sort();
                assertTrue(h.unsorted());
                assertFalse(lanes.transformed(), "unsorted and unfiltered: no lanes");
                compare("unsorted");
            }
            case "filtered" -> {
                IntPredicate p = k -> k % 2 == 1;
                stock.setRowFilter(stockFilter(p));
                h.filter = p;
                h.sort();
                assertTrue(h.filtered() && h.unsorted());
                assertTrue(lanes.transformed(), "a filter alone transforms the view");
                for (int v = 0; v + 1 < lanes.viewRowCount(); v++) {
                    assertTrue(lanes.modelIndex(v) < lanes.modelIndex(v + 1), "unsorted filter keeps model order");
                }
                compare("filtered");
            }
            case "sorted()" -> {
                ensureRows(MAX_RUN);
                runSorted(Mix.UPDATE, 0, false);
                stock.setSortsOnUpdates(false);
                updateRange(0, 0);
                assertFalse(h.sorted(), "an update without sortsOnUpdates clears sorted");
                int before = lanes.fullSortsOnDeltas();
                insert(false);
                assertEquals(before + 1, lanes.fullSortsOnDeltas(), "not sorted: the next delta sorts everything");
                assertTrue(h.sorted());
            }
            case "sorted(boolean)" -> {
                h.sorted(false);
                h.setKeys(List.of(new RowSorter.SortKey(KEY, SortOrder.DESCENDING)));
                stock.setSortKeys(h.keys);
                assertTrue(h.sorted(), "sort() sets sorted");
                compare("sorted(true)");
                ensureRows(MAX_RUN);
                stock.setSortsOnUpdates(false);
                updateRange(0, 0);
                assertFalse(h.sorted(), "rowsUpdated without sortsOnUpdates sets sorted(false)");
            }
            case "prepareSort" -> {
                h.calls.clear();
                keys();
                filter();
                int prepares = Collections.frequency(h.calls, "prepareSort");
                assertTrue(prepares >= 1, "every sort starts with prepareSort");
                int first = h.calls.indexOf("prepareSort");
                assertTrue(h.calls.indexOf("changed") < 0 || h.calls.indexOf("changed") > first);
            }
            case "prepareResort" -> {
                runSorted(Mix.MIXED, 0, false);
                h.calls.clear();
                List<RowSorter.SortKey> k = List.of(new RowSorter.SortKey(KEY, SortOrder.DESCENDING));
                stock.setSortKeys(k);
                h.setKeys(k);
                assertEquals(List.of("prepareResort", "changed"), h.calls, "new keys over existing lanes: a re-sort");
                compare("prepareResort");
            }
            case "sort" -> {
                h.calls.clear();
                List<RowSorter.SortKey> k = List.of(new RowSorter.SortKey(KEY, SortOrder.ASCENDING));
                stock.setSortKeys(k);
                h.setKeys(k);
                assertEquals("sort", h.calls.get(0), "first keys: the public sort() through the host");
                compare("first sort");
            }
            case "changed" -> {
                runSorted(Mix.MIXED, STEPS / 2, true);
                int[] before = lanes.snapshot();
                List<RowSorter.SortKey> desc = List.of(new RowSorter.SortKey(ID, SortOrder.DESCENDING));
                List<RowSorter.SortKey> k = h.keys.equals(desc) ? List.of(new RowSorter.SortKey(ID, SortOrder.ASCENDING)) : desc;
                stock.setSortKeys(k);
                h.setKeys(k);
                assertArrayEquals(before, h.changes.get(h.changes.size() - 1), "changed gets the order before the sort");
                compare("changed");
            }
            default -> fail("no check for Host#" + member);
        }
        assertTrue(h.called.getOrDefault(member, 0) > 0, "the lanes called Host." + member + " (reach of an interface atom)");
    }

    // ---- per-atom checks ----------------------------------------------------------------------------------

    static void check(String atom) {
        SorterScript s = new SorterScript(atom);
        M3SorterLanes lanes = s.host.lanes;
        switch (atom) {
            case "Host.compare", "Host.include", "Host.modelRowCount", "Host.unsorted", "Host.filtered", "Host.sorted()",
                    "Host.sorted(boolean)", "Host.prepareSort", "Host.prepareResort", "Host.sort", "Host.changed" ->
                    s.hostContract(atom.substring("Host.".length()));
            case "M3SorterLanes" -> {
                M3SorterLanes fresh = new M3SorterLanes();
                assertFalse(fresh.bound(), "unbound until the sorter binds its host");
                assertFalse(fresh.transformed(), "no order before the first sort");
                assertEquals(0, fresh.fullSortsOnDeltas());
                assertEquals(0, fresh.snapshot().length);
                s.run(Mix.MIXED, STEPS);
            }
            case "fullSortsOnDeltas" -> {
                s.runSorted(Mix.BULK, STEPS, false);
                assertTrue(lanes.fullSortsOnDeltas() > 0, "bulk edits beyond the stock cutoff sort everything");
            }
            case "bound" -> {
                assertTrue(lanes.bound());
                assertFalse(new M3SorterLanes().bound());
            }
            case "bind" -> {
                M3SorterLanes other = new M3SorterLanes();
                other.bind(s.host);
                assertTrue(other.bound());
                s.run(Mix.KEYS, STEPS);
            }
            case "transformed", "viewRowCount", "modelIndex", "viewIndex" -> s.run(Mix.MIXED, STEPS);
            case "clear" -> {
                s.runSorted(Mix.STRUCTURE, STEPS, false);
                lanes.clear();
                assertFalse(lanes.transformed(), "clear forgets the order");
                assertEquals(0, lanes.snapshot().length);
            }
            case "snapshot" -> {
                s.runSorted(Mix.MIXED, STEPS, true);
                s.query();
            }
            case "sortKeysChanged", "sort", "createModelToView", "createViewToModel" -> s.run(Mix.KEYS, STEPS);
            case "sortExistingData" -> {
                s.runSorted(Mix.KEYS, STEPS, true);
                s.run(Mix.KEYS, STEPS);
            }
            case "initializeFilteredMapping" -> s.run(Mix.FILTER, STEPS);
            case "setModelToViewFromViewToModel" -> s.runSorted(Mix.MIXED, STEPS, true);
            case "rowsInserted", "rowsInserted0" -> s.runSorted(Mix.INSERT, STEPS, atom.endsWith("0"));
            case "rowsDeleted", "rowsDeleted0" -> s.runSorted(Mix.DELETE, STEPS, atom.endsWith("0"));
            case "rowsUpdated" -> s.runSorted(Mix.UPDATE, STEPS, false);
            case "rowsUpdated0" -> {
                s.runSorted(Mix.UPDATE, STEPS, false);
                new SorterScript(atom + "/filtered").runSorted(Mix.UPDATE, STEPS, true);
            }
            case "shouldOptimizeChange" -> {
                s.runSorted(Mix.BULK, STEPS, true);
                assertTrue(lanes.fullSortsOnDeltas() > 0);
            }
            case "insertInOrder" -> {
                s.runSorted(Mix.INSERT, STEPS, false);
                new SorterScript(atom + "/update").runSorted(Mix.UPDATE, STEPS, true);
            }
            case "binarySearch" -> s.binarySearchContract();
            case "sortLane" -> s.sortLaneContract();
            case "runEnd" -> s.runEndContract();
            case "binaryInsertion" -> s.binaryInsertionContract();
            case "merge" -> s.mergeContract();
            default -> fail("no check for M3SorterLanes#" + atom);
        }
    }
}
