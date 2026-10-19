/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.M3ItemRange;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.junit.jupiter.api.Test;

/**
 * Contract of getSelectionRanges / setSelectionRanges on JList, JTable and JTree (leaf S1). The oracle is the
 * stock index API: coalesce(getSelectedIndices()) and isSelectedIndex.
 */
public class SelectionRangesTest {

    private static void randomSelection(ListSelectionModel model, Random random, int size) {
        model.clearSelection();
        for (int k = random.nextInt(6); k >= 0; k--) {
            int a = random.nextInt(size), b = Math.min(size - 1, a + random.nextInt(9));
            if (random.nextInt(4) == 0) {
                model.removeSelectionInterval(a, b);
            } else {
                model.addSelectionInterval(a, b);
            }
        }
    }

    @Test
    public void listAndTableRangesAgreeWithTheIndices() {
        Random random = new Random(2);
        JList<String> list = Fixtures.list(200);
        JTable table = Fixtures.table(200);
        for (int round = 0; round < 200; round++) {
            randomSelection(list.getSelectionModel(), random, 200);
            assertArrayEquals(M3ItemRange.coalesce(list.getSelectedIndices()), list.getSelectionRanges(), "list round " + round);
            randomSelection(table.getSelectionModel(), random, 200);
            assertArrayEquals(M3ItemRange.coalesce(table.getSelectedRows()), table.getSelectionRanges(), "table round " + round);
        }
    }

    @Test
    public void customSelectionModelsAreAskedIndexByIndex() {
        Random random = new Random(3);
        JList<String> list = Fixtures.list(100);
        /* a subclass may override the queries: the bit set is not read for it */
        list.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public boolean isSelectedIndex(int index) {
                return super.isSelectedIndex(index) && index % 7 != 0;
            }
        });
        for (int round = 0; round < 50; round++) {
            randomSelection(list.getSelectionModel(), random, 100);
            List<Integer> expected = new ArrayList<>();
            ListSelectionModel model = list.getSelectionModel();
            for (int i = 0; i < 100; i++) {
                if (model.isSelectedIndex(i)) {
                    expected.add(i);
                }
            }
            assertArrayEquals(M3ItemRange.coalesce(expected.stream().mapToInt(Integer::intValue).toArray()), list.getSelectionRanges());
        }
        list.clearSelection();
        assertEquals(0, list.getSelectionRanges().length);
    }

    @Test
    public void setSelectionRangesIsOneChange() {
        JTable table = Fixtures.table(1_000_000 / 100);
        List<ListSelectionEvent> events = new ArrayList<>();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                events.add(e);
            }
        });
        M3ItemRange[] ranges = {M3ItemRange.of(5, 9), M3ItemRange.EMPTY, M3ItemRange.of(100, 9_999)};
        table.setSelectionRanges(ranges);
        assertArrayEquals(new M3ItemRange[] {M3ItemRange.of(5, 9), M3ItemRange.of(100, 9_999)}, table.getSelectionRanges());
        assertEquals(1, events.size(), "one non-adjusting valueChanged (adjusting ones may precede it)");
        assertFalse(events.get(0).getValueIsAdjusting());
        assertEquals(5, events.get(0).getFirstIndex());
        assertEquals(9_999, events.get(0).getLastIndex());
        table.setSelectionRanges(new M3ItemRange[0]);
        assertEquals(0, table.getSelectedRowCount());
        assertThrows(IllegalArgumentException.class, () -> table.setSelectionRanges(new M3ItemRange[] {M3ItemRange.of(0, 10_000)}));
        assertThrows(NullPointerException.class, () -> table.setSelectionRanges(null));
        assertThrows(NullPointerException.class, () -> table.setSelectionRanges(new M3ItemRange[] {null}));
    }

    @Test
    public void selectionModeApplies() {
        JList<String> list = Fixtures.list(50);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectionRanges(new M3ItemRange[] {M3ItemRange.of(3, 6), M3ItemRange.of(10, 12)});
        /* the same as clearSelection then addSelectionInterval per range */
        JList<String> stock = Fixtures.list(50);
        stock.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stock.clearSelection();
        stock.addSelectionInterval(3, 6);
        stock.addSelectionInterval(10, 12);
        assertArrayEquals(stock.getSelectedIndices(), list.getSelectedIndices());
        assertArrayEquals(M3ItemRange.coalesce(stock.getSelectedIndices()), list.getSelectionRanges());
        assertThrows(IllegalArgumentException.class, () -> list.setSelectionRanges(new M3ItemRange[] {M3ItemRange.of(49, 50)}));
    }

    @Test
    public void newBeanPropertiesAreTransientOrReadOnly() throws Exception {
        for (Class<?> type : new Class<?>[] {JList.class, JTable.class, JTree.class}) {
            java.util.Map<String, java.beans.PropertyDescriptor> byName = new java.util.HashMap<>();
            for (java.beans.PropertyDescriptor d : java.beans.Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                byName.put(d.getName(), d);
            }
            /* XMLEncoder skips transient properties: persisted components are written as before */
            assertEquals(Boolean.TRUE, byName.get("selectionRanges").getValue("transient"), type.getName());
            assertNull(byName.get("visibleRange").getWriteMethod(), type.getName());
        }
    }

    @Test
    public void treeRangesAreVisibleRows() {
        JTree tree = Fixtures.tree();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        Random random = new Random(4);
        for (int round = 0; round < 60; round++) {
            int rows = tree.getRowCount();
            int a = random.nextInt(rows), b = Math.min(rows - 1, a + random.nextInt(6));
            int c = random.nextInt(rows);
            tree.setSelectionRanges(new M3ItemRange[] {M3ItemRange.of(a, b), M3ItemRange.of(c, c)});
            int[] selected = tree.getSelectionRows();
            assertArrayEquals(M3ItemRange.coalesce(selected), tree.getSelectionRanges(), "round " + round);
        }
        /* a selected path under a collapsed parent has no row */
        tree.setSelectionRanges(new M3ItemRange[] {M3ItemRange.of(1, 2)});
        tree.collapseRow(0);
        for (M3ItemRange range : tree.getSelectionRanges()) {
            assertTrue(range.end() < tree.getRowCount());
        }
        tree.clearSelection();
        assertEquals(0, tree.getSelectionRanges().length);
        assertThrows(IllegalArgumentException.class, () -> tree.setSelectionRanges(new M3ItemRange[] {M3ItemRange.of(0, tree.getRowCount())}));
        tree.setSelectionRanges(new M3ItemRange[] {M3ItemRange.EMPTY});
        assertEquals(0, tree.getSelectionCount());
    }
}
