/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 7: JUnit suite of sun.swing.table.M3SorterLanes: the lane sort
 *          against Arrays.sort, every change path against a brute-force reference with a scripted
 *          host, the sorter gate and the absence of Row objects, plus the JTable differential.
 * @modules java.desktop/sun.swing.table java.desktop/javax.swing:open
 * @build M3RowSorterLanesDifferential
 * @run junit/othervm -Djava.awt.headless=true -Dm3.swing.viewport=true -XX:-OmitStackTraceInFastThrow M3SorterLanesTest
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.junit.jupiter.api.Test;

import sun.swing.table.M3SorterLanes;

public class M3SorterLanesTest {

    /** A scripted host: integer keys per model row, a visibility rule, flags and an event log. */
    static final class Script implements M3SorterLanes.Host {
        final List<Integer> keys = new ArrayList<>();
        boolean unsorted;
        boolean cachedUnsorted;
        boolean filtered;
        boolean sorted;
        int hideModulo;
        int compares;
        int prepared;
        int resorts;
        final List<int[]> changes = new ArrayList<>();
        M3SorterLanes lanes;

        @Override
        public int compare(int model1, int model2) {
            compares++;
            if (cachedUnsorted) {
                // stock: no cached sort keys, so the fallback to model order decides
                return model1 - model2;
            }
            int c = Integer.compare(keys.get(model1), keys.get(model2));
            return c != 0 ? c : model1 - model2;
        }

        @Override
        public boolean include(int modelRow) {
            return !filtered || keys.get(modelRow) % hideModulo != 0;
        }

        @Override
        public int modelRowCount() {
            return keys.size();
        }

        @Override
        public boolean unsorted() {
            return unsorted;
        }

        @Override
        public boolean filtered() {
            return filtered;
        }

        @Override
        public boolean sorted() {
            return sorted;
        }

        @Override
        public void sorted(boolean value) {
            sorted = value;
        }

        @Override
        public void prepareSort() {
            prepared++;
            cachedUnsorted = unsorted;
        }

        @Override
        public void prepareResort() {
            resorts++;
            cachedUnsorted = unsorted;
        }

        @Override
        public void sort() {
            lanes.sort();
        }

        @Override
        public void changed(int[] lastViewToModel) {
            changes.add(lastViewToModel);
        }

        /** The visible rows in the sorter's order, by brute force; null when nothing is transformed. */
        int[] expected() {
            if (unsorted && !filtered) {
                return null;
            }
            List<Integer> rows = new ArrayList<>();
            for (int i = 0; i < keys.size(); i++) {
                if (include(i)) {
                    rows.add(i);
                }
            }
            if (!unsorted) {
                rows.sort((a, b) -> {
                    int c = Integer.compare(keys.get(a), keys.get(b));
                    return c != 0 ? c : a - b;
                });
            }
            return rows.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    static Script script(int n, Random rnd) {
        Script s = new Script();
        s.lanes = new M3SorterLanes();
        s.lanes.bind(s);
        s.hideModulo = 2 + rnd.nextInt(4);
        for (int i = 0; i < n; i++) {
            s.keys.add(rnd.nextInt(20));
        }
        return s;
    }

    /** The lane sort gives Arrays.sort's permutation; an ordered lane costs n - 1 comparisons. */
    @Test
    void laneSortMatchesArraysSort() {
        Random rnd = new Random(3);
        for (int n : new int[] {0, 1, 2, 3, 15, 16, 17, 33, 100, 1000, 5000}) {
            for (int shape = 0; shape < 4; shape++) {
                Script s = script(n, rnd);
                for (int i = 0; i < n; i++) {
                    s.keys.set(i, shape == 0 ? rnd.nextInt(n / 3 + 1) : shape == 1 ? i : shape == 2 ? n - i : 7);
                }
                int[] lane = new int[n];
                Integer[] boxed = new Integer[n];
                for (int i = 0; i < n; i++) {
                    lane[i] = n - 1 - i;
                    boxed[i] = n - 1 - i;
                }
                Arrays.sort(boxed, (a, b) -> s.compare(a, b));
                s.compares = 0;
                s.lanes.sortLane(lane, n);
                assertArrayEquals(Arrays.stream(boxed).mapToInt(Integer::intValue).toArray(), lane, "n " + n + " shape " + shape);
                s.compares = 0;
                s.lanes.sortLane(lane, n);
                assertEquals(Math.max(0, n - 1), s.compares, "sorted lane, n " + n);
                int[] sorted = lane.clone();
                for (int i = 0, j = n - 1; i < j; i++, j--) {
                    int t = lane[i];
                    lane[i] = lane[j];
                    lane[j] = t;
                }
                s.compares = 0;
                s.lanes.sortLane(lane, n);
                assertArrayEquals(sorted, lane, "reversed lane, n " + n);
                assertEquals(Math.max(0, n - 1), s.compares, "reversed lane is one run, n " + n);
            }
        }
    }

    /** Every change path against the brute-force order: 60 scripts x 300 steps. */
    @Test
    void changePathsMatchTheReference() {
        Random rnd = new Random(11);
        int inserted = 0;
        int deleted = 0;
        int updated = 0;
        for (int run = 0; run < 60; run++) {
            Script s = script(rnd.nextInt(40), rnd);
            s.unsorted = rnd.nextBoolean();
            s.filtered = rnd.nextBoolean();
            s.lanes.sort();
            for (int step = 0; step < 300; step++) {
                int[] before = s.lanes.snapshot();
                int events = s.changes.size();
                int op = rnd.nextInt(10);
                int n = s.keys.size();
                if (op < 2) {
                    s.unsorted = rnd.nextInt(3) == 0;
                    if (rnd.nextBoolean()) {
                        // setRowFilter or sort(): the filter may change too
                        s.filtered = rnd.nextBoolean();
                        s.hideModulo = 2 + rnd.nextInt(4);
                        s.lanes.sort();
                    } else {
                        // setSortKeys: only the keys change (stock sortExistingData keeps the filtered rows)
                        s.lanes.sortKeysChanged();
                    }
                } else if (op < 5) {
                    int at = rnd.nextInt(n + 1);
                    int k = 1 + rnd.nextInt(rnd.nextInt(8) == 0 ? 12 : 3);
                    for (int i = 0; i < k; i++) {
                        s.keys.add(at, rnd.nextInt(20));
                    }
                    s.lanes.rowsInserted(at, at + k - 1);
                    inserted++;
                } else if (op < 7 && n > 0) {
                    int at = rnd.nextInt(n);
                    int k = Math.min(n - at, 1 + rnd.nextInt(3));
                    for (int i = 0; i < k; i++) {
                        s.keys.remove(at);
                    }
                    s.lanes.rowsDeleted(at, at + k - 1);
                    deleted++;
                } else if (op < 9 && n > 0) {
                    int at = rnd.nextInt(n);
                    int k = Math.min(n - at, 1 + rnd.nextInt(3));
                    for (int i = at; i < at + k; i++) {
                        s.keys.set(i, rnd.nextInt(20));
                    }
                    boolean sortsOnUpdates = rnd.nextInt(4) > 0;
                    s.lanes.rowsUpdated(at, at + k - 1, sortsOnUpdates);
                    if (!sortsOnUpdates) {
                        assertFalse(s.sorted);
                        s.lanes.sort();
                    }
                    updated++;
                } else {
                    s.lanes.clear();
                    before = s.lanes.snapshot();
                    assertFalse(s.lanes.transformed());
                    s.lanes.sort();
                }
                int[] want = s.expected();
                if (want == null) {
                    assertFalse(s.lanes.transformed(), "run " + run + " step " + step);
                } else {
                    assertTrue(s.lanes.transformed(), "run " + run + " step " + step);
                    assertArrayEquals(want, s.lanes.snapshot(), "run " + run + " step " + step + " op " + op);
                    assertEquals(want.length, s.lanes.viewRowCount());
                    int[] toView = new int[s.keys.size()];
                    Arrays.fill(toView, -1);
                    for (int v = 0; v < want.length; v++) {
                        assertEquals(want[v], s.lanes.modelIndex(v));
                        toView[want[v]] = v;
                    }
                    for (int m = 0; m < toView.length; m++) {
                        assertEquals(toView[m], s.lanes.viewIndex(m), "model row " + m);
                    }
                }
                if (s.changes.size() > events) {
                    assertArrayEquals(before, s.changes.get(events), "the event carries the previous order");
                }
            }
        }
        assertTrue(inserted > 1000 && deleted > 500 && updated > 500, inserted + " " + deleted + " " + updated);
    }

    /** The stock corner cases: nothing to do, all inserted rows hidden, no visible row deleted, hidden count unchanged. */
    @Test
    void cornerCases() {
        Random rnd = new Random(5);
        Script s = script(10, rnd);
        s.unsorted = true;
        s.lanes.sort();
        assertEquals(0, s.changes.size(), "unsorted and unfiltered, nothing was transformed: no event");
        s.lanes.sortKeysChanged();
        assertEquals(0, s.changes.size());
        s.unsorted = false;
        s.lanes.sortKeysChanged();
        assertEquals(1, s.changes.size());
        assertEquals(0, s.changes.get(0).length, "previous order empty");
        s.unsorted = true;
        s.lanes.sortKeysChanged();
        assertFalse(s.lanes.transformed(), "sortExistingData back to unsorted drops the lanes");
        assertEquals(1, s.resorts);
        s.filtered = true;
        s.hideModulo = 2;
        s.unsorted = false;
        s.lanes.sort();
        int[] order = s.lanes.snapshot();
        s.keys.add(3, 4);
        s.keys.add(3, 8);
        s.lanes.rowsInserted(3, 4);
        assertEquals(order.length, s.lanes.viewRowCount(), "both inserted rows hidden");
        assertArrayEquals(s.expected(), s.lanes.snapshot());
        int hiddenRow = -1;
        for (int i = 0; i < s.keys.size(); i++) {
            if (!s.include(i)) {
                hiddenRow = i;
                break;
            }
        }
        s.keys.remove(hiddenRow);
        s.lanes.rowsDeleted(hiddenRow, hiddenRow);
        assertArrayEquals(s.expected(), s.lanes.snapshot(), "deleting a hidden row keeps the lane length");
        int visible = s.lanes.modelIndex(0);
        s.keys.set(visible, s.keys.get(visible) + 1);
        hiddenRow = -1;
        for (int i = 0; i < s.keys.size(); i++) {
            if (!s.include(i) && i != visible) {
                hiddenRow = i;
                break;
            }
        }
        s.keys.set(hiddenRow, 1);
        int a = Math.min(visible, hiddenRow);
        int b = Math.max(visible, hiddenRow);
        s.lanes.rowsUpdated(a, b, true);
        assertArrayEquals(s.expected(), s.lanes.snapshot(), "one row newly hidden, one newly visible");
        s.unsorted = true;
        s.lanes.sortKeysChanged();
        assertArrayEquals(s.expected(), s.lanes.snapshot(), "unsorted but filtered keeps model order");
        s.keys.add(0, 2);
        s.sorted = false;
        s.lanes.rowsInserted(0, 0);
        assertArrayEquals(s.expected(), s.lanes.snapshot(), "not sorted: a full sort");
        Script empty = script(0, rnd);
        empty.filtered = true;
        empty.lanes.sort();
        assertEquals(0, empty.lanes.viewRowCount());
        empty.keys.add(1);
        empty.lanes.rowsInserted(0, 0);
        assertArrayEquals(new int[] {0}, empty.lanes.snapshot(), "an empty lane sorts in full");
    }

    static Object field(Object o, String name) throws Exception {
        Field f = javax.swing.DefaultRowSorter.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    /** With the property set a TableRowSorter keeps no Row objects; with the field cleared it keeps them. */
    @Test
    void sorterGate() throws Exception {
        assertTrue(M3SorterLanes.ENABLED);
        DefaultTableModel model = new DefaultTableModel(new Object[] {"n"}, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return Integer.class;
            }
        };
        for (int i = 0; i < 500; i++) {
            model.addRow(new Object[] {i % 37});
        }
        TableRowSorter<DefaultTableModel> m3 = new TableRowSorter<>(model);
        TableRowSorter<DefaultTableModel> stock = new TableRowSorter<>(model);
        Field f = javax.swing.DefaultRowSorter.class.getDeclaredField("m3Lanes");
        f.setAccessible(true);
        f.set(stock, null);
        assertNotNull(field(m3, "m3Lanes"));
        assertFalse(((M3SorterLanes) field(m3, "m3Lanes")).bound(), "bound on first use");
        assertEquals(500, m3.getViewRowCount());
        assertEquals(7, m3.convertRowIndexToModel(7));
        assertNull(field(m3, "viewToModel"));
        for (TableRowSorter<DefaultTableModel> s : List.of(m3, stock)) {
            s.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
            s.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.NOT_EQUAL, 5, 0));
        }
        assertNull(field(m3, "viewToModel"), "no Row objects");
        assertNotNull(field(stock, "viewToModel"), "the cleared field keeps the stock Row objects");
        assertTrue(((M3SorterLanes) field(m3, "m3Lanes")).transformed());
        assertEquals(stock.getViewRowCount(), m3.getViewRowCount());
        for (int v = 0; v < stock.getViewRowCount(); v++) {
            assertEquals(stock.convertRowIndexToModel(v), m3.convertRowIndexToModel(v));
        }
        JTable table = new JTable(model);
        table.setRowSorter(m3);
        assertSame(m3, table.getRowSorter());
        assertEquals(m3.getViewRowCount(), table.getRowCount());
    }

    /** The JTable-level differential. */
    @Test
    void differential() throws Exception {
        String summary = M3RowSorterLanesDifferential.runSeeds(100, 20, 200);
        assertTrue(summary.contains(" failed 0 "), summary);
    }
}
