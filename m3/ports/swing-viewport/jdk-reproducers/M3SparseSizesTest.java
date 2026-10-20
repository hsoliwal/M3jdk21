/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 8: JUnit suite of sun.swing.table.M3SparseSizes against
 *          javax.swing.SizeSequence: every query at every index and position, inside and outside
 *          the range, through random setSize, insertEntries, removeEntries and setSizes scripts;
 *          the view-order mapping through a sorter; the JTable differential.
 * @modules java.desktop/sun.swing.table java.desktop/javax.swing:open
 * @build M3TableRowHeightsDifferential
 * @run junit/othervm -Djava.awt.headless=true -Dm3.swing.viewport=true -XX:-OmitStackTraceInFastThrow M3SparseSizesTest
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SizeSequence;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.junit.jupiter.api.Test;

import sun.swing.table.M3SparseSizes;

public class M3SparseSizesTest {

    /** Every query of both, at every index from -2 to n + 2 and at positions around every boundary. */
    static void same(SizeSequence stock, M3SparseSizes m3, String what) {
        int n = stock.getSizes().length;
        assertEquals(n, m3.length(), what);
        assertArrayEquals(stock.getSizes(), m3.getSizes(), what);
        for (int i = -2; i <= n + 2; i++) {
            assertEquals(stock.getSize(i), m3.getSize(i), what + " getSize " + i);
            assertEquals(stock.getPosition(i), m3.getPosition(i), what + " getPosition " + i);
        }
        int total = stock.getPosition(n);
        for (int i = 0; i <= n; i++) {
            int p = stock.getPosition(i);
            for (int d = -1; d <= 1; d++) {
                assertEquals(stock.getIndex(p + d), m3.getIndex(p + d), what + " getIndex " + (p + d));
            }
        }
        for (int p : new int[] {-1, 0, total - 1, total, total + 1, total + 1000, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            assertEquals(stock.getIndex(p), m3.getIndex(p), what + " getIndex " + p);
        }
    }

    /** 50 scripts x 300 steps of the four mutators, compared after every step. */
    @Test
    void matchesSizeSequence() {
        Random rnd = new Random(8);
        int entries = 0;
        for (int run = 0; run < 50; run++) {
            int n = rnd.nextInt(40);
            int d = 1 + rnd.nextInt(30);
            SizeSequence stock = new SizeSequence(n, d);
            M3SparseSizes m3 = new M3SparseSizes(n, d);
            same(stock, m3, "run " + run + " start");
            for (int step = 0; step < 300; step++) {
                int op = rnd.nextInt(10);
                int len = stock.getSizes().length;
                String what = "run " + run + " step " + step + " op " + op;
                if (op < 6) {
                    int i = rnd.nextInt(len + 2);
                    int s = rnd.nextInt(4) == 0 ? d : rnd.nextInt(60);
                    stock.setSize(i, s);
                    m3.setSize(i, s);
                } else if (op < 8) {
                    int at = rnd.nextInt(len + 1);
                    int k = rnd.nextInt(4);
                    int v = rnd.nextInt(3) == 0 ? rnd.nextInt(50) : d;
                    stock.insertEntries(at, k, v);
                    m3.insertEntries(at, k, v);
                } else if (op < 9 && len > 0) {
                    int at = rnd.nextInt(len);
                    int k = rnd.nextInt(len - at + 1);
                    stock.removeEntries(at, k);
                    m3.removeEntries(at, k);
                } else {
                    int k = rnd.nextInt(20);
                    int v = 1 + rnd.nextInt(30);
                    stock = new SizeSequence(k, v);
                    m3.setSizes(k, v);
                    d = v;
                }
                same(stock, m3, what);
                entries += m3.count();
            }
        }
        assertTrue(entries > 10_000, "stored entries over the scripts: " + entries);
    }

    /** Above LIMIT custom sizes the store is the stock dense tree: same answers, and back to sparse on setSizes. */
    @Test
    void denseAboveTheLimit() {
        Random rnd = new Random(4);
        int n = 20_000;
        SizeSequence stock = new SizeSequence(n, 16);
        M3SparseSizes m3 = new M3SparseSizes(n, 16);
        for (int k = 0; k < 6000; k++) {
            int i = rnd.nextInt(n);
            int s = 1 + rnd.nextInt(60);
            stock.setSize(i, s);
            m3.setSize(i, s);
        }
        assertEquals(-1, m3.count(), "dense above the limit");
        same(stock, m3, "after 6,000 sets");
        for (int step = 0; step < 40; step++) {
            int len = stock.getSizes().length;
            int op = rnd.nextInt(3);
            if (op == 0) {
                int at = rnd.nextInt(len + 1);
                stock.insertEntries(at, 3, 16);
                m3.insertEntries(at, 3, 16);
            } else if (op == 1) {
                int at = rnd.nextInt(len - 3);
                stock.removeEntries(at, 3);
                m3.removeEntries(at, 3);
            } else {
                int i = rnd.nextInt(len);
                stock.setSize(i, 7);
                m3.setSize(i, 7);
            }
            m3.setSize(-1, 99);
            if (step % 10 == 0) {
                same(stock, m3, "dense step " + step);
            }
        }
        same(stock, m3, "dense end");
        m3.setSizes(10, 3);
        assertEquals(0, m3.count(), "setSizes returns to the sparse form");
        SizeSequence bulkStock = new SizeSequence(10, 5);
        M3SparseSizes bulk = new M3SparseSizes(10, 5);
        bulkStock.insertEntries(3, M3SparseSizes.LIMIT + 1, 9);
        bulk.insertEntries(3, M3SparseSizes.LIMIT + 1, 9);
        assertEquals(-1, bulk.count(), "a bulk insert over the limit goes dense");
        same(bulkStock, bulk, "bulk insert");
        DefaultTableModel model = new DefaultTableModel(new Object[] {"n"}, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return Integer.class;
            }
        };
        for (int i = 0; i < 6000; i++) {
            model.addRow(new Object[] {i % 17});
        }
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.NOT_EQUAL, 3, 0));
        M3SparseSizes modelSizes = new M3SparseSizes(6000, 16);
        for (int i = 0; i < 6000; i++) {
            modelSizes.setSize(i, 17 + i % 5);
        }
        assertEquals(-1, modelSizes.count());
        M3SparseSizes view = modelSizes.inViewOrder(sorter.getViewRowCount(), 16, sorter);
        for (int v = 0; v < sorter.getViewRowCount(); v++) {
            assertEquals(modelSizes.getSize(sorter.convertRowIndexToModel(v)), view.getSize(v), "view row " + v);
        }
    }

    /** Zero sizes among the entries: getIndex skips them as the stock tree does. */
    @Test
    void zeroSizes() {
        int[] sizes = {5, 0, 0, 7, 0, 3};
        SizeSequence stock = new SizeSequence(sizes);
        M3SparseSizes m3 = new M3SparseSizes(sizes.length, 4);
        for (int i = 0; i < sizes.length; i++) {
            m3.setSize(i, sizes[i]);
        }
        same(stock, m3, "zeros");
        m3.setSize(3, 0);
        stock.setSize(3, 0);
        same(stock, m3, "more zeros");
        m3.setSize(0, 0);
        stock.setSize(0, 0);
        same(stock, m3, "leading zero");
    }

    /** The checks and the ignored negative setSize. */
    @Test
    void boundsAndArguments() {
        assertThrows(IllegalArgumentException.class, () -> new M3SparseSizes(-1, 5));
        assertThrows(IllegalArgumentException.class, () -> new M3SparseSizes(3, 0));
        M3SparseSizes s = new M3SparseSizes(5, 10);
        s.setSize(-1, 7);
        assertEquals(0, s.count(), "negative index ignored");
        s.setSize(5, 7);
        assertEquals(0, s.count(), "index at the length: no change, as stock");
        assertThrows(IndexOutOfBoundsException.class, () -> s.insertEntries(6, 1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> s.insertEntries(-1, 1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> s.removeEntries(4, 2));
        assertThrows(IllegalArgumentException.class, () -> s.setSizes(2, 0));
        assertThrows(IllegalArgumentException.class, () -> s.setSizes(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> s.insertEntries(0, -1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> s.removeEntries(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> s.removeEntries(-1, 1));
        s.setSize(2, 30);
        assertEquals(1, s.count());
        assertEquals(10, s.defaultSize());
        assertEquals(20 + 30, s.getPosition(3));
        assertEquals(2, s.getIndex(25));
        assertEquals(3, s.getIndex(50));
        assertEquals(5, s.getIndex(70));
        assertEquals(4, s.getIndex(69));
        assertArrayEquals(new int[] {10, 10, 30, 10, 10}, s.getSizes());
        assertEquals(0, new M3SparseSizes(0, 3).getIndex(5), "empty: always 0");
        assertEquals(0, new M3SparseSizes(0, 3).getPosition(2));
    }

    /** Model order to view order through a sorter: hidden rows left out, visible rows mapped. */
    @Test
    void viewOrderThroughASorter() {
        DefaultTableModel model = new DefaultTableModel(new Object[] {"n"}, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return Integer.class;
            }
        };
        for (int i = 0; i < 100; i++) {
            model.addRow(new Object[] {i % 13});
        }
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
        sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.NOT_EQUAL, 5, 0));
        M3SparseSizes modelSizes = new M3SparseSizes(100, 16);
        Random rnd = new Random(1);
        for (int k = 0; k < 20; k++) {
            modelSizes.setSize(rnd.nextInt(100), 20 + rnd.nextInt(30));
        }
        M3SparseSizes view = modelSizes.inViewOrder(sorter.getViewRowCount(), 16, sorter);
        assertEquals(sorter.getViewRowCount(), view.length());
        for (int v = 0; v < sorter.getViewRowCount(); v++) {
            assertEquals(modelSizes.getSize(sorter.convertRowIndexToModel(v)), view.getSize(v), "view row " + v);
        }
        int hidden = 0;
        for (int m = 0; m < 100; m++) {
            if (sorter.convertRowIndexToView(m) == -1 && modelSizes.getSize(m) != 16) {
                hidden++;
            }
        }
        assertEquals(modelSizes.count() - hidden, view.count());
    }

    /** The JTable-level differential. */
    @Test
    void jtableDifferential() throws Exception {
        String summary = M3TableRowHeightsDifferential.runSeeds(100, 20, 200);
        assertTrue(summary.contains(" failed 0 "), summary);
    }
}
