/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Set;
import java.util.TreeSet;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.tree.TreeSelectionModel;

/**
 * Differential behaviour probe (not a JUnit test): drives JList, JTable and JTree through stock API only and prints
 * a deterministic trace (selection, geometry, events, painted pixels, bean descriptors). run-tests.sh diff runs it
 * against master's classes and against this branch's; equal output shows the new API changes nothing when unused.
 */
public final class M3SwingBehaviourProbe {

    private static final StringBuilder OUT = new StringBuilder();

    private M3SwingBehaviourProbe() {
    }

    /** @param args the output file of the trace */
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        Random random = new Random(42);
        probeList(random);
        probeTable(random);
        probeTree(random);
        java.nio.file.Files.writeString(java.nio.file.Path.of(args[0]), OUT);
    }

    private static void line(String key, Object value) {
        OUT.append(key).append(' ').append(value).append('\n');
    }

    private static JViewport show(JComponent view, int width, int height) {
        JViewport viewport = new JViewport();
        viewport.setView(view);
        viewport.setSize(width, height);
        view.setSize(Math.max(width, view.getPreferredSize().width), view.getPreferredSize().height);
        view.doLayout();
        return viewport;
    }

    private static long pixels(JComponent view) {
        BufferedImage image = new BufferedImage(Math.max(1, view.getWidth()), Math.min(600, Math.max(1, view.getHeight())), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        view.paint(g);
        g.dispose();
        CRC32 crc = new CRC32();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                crc.update(image.getRGB(x, y));
            }
        }
        return crc.getValue();
    }

    /* properties added by the enhanced API; every other property must keep its descriptor */
    private static final Set<String> NEW_PROPERTIES = Set.of("selectionRanges", "visibleRange", "scrollInfo");

    private static String beans(Class<?> type) throws Exception {
        TreeSet<String> properties = new TreeSet<>();
        for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
            if (NEW_PROPERTIES.contains(descriptor.getName())) {
                continue;
            }
            properties.add(descriptor.getName() + ":" + descriptor.getPropertyType() + ":" + descriptor.isBound()
                    + ":" + descriptor.getValue("transient") + ":" + (descriptor.getWriteMethod() != null));
        }
        CRC32 crc = new CRC32();
        crc.update(String.join("\n", properties).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return properties.size() + ":" + Long.toHexString(crc.getValue());
    }

    private static void probeList(Random random) throws Exception {
        JList<String> list = Fixtures.list(300);
        list.addListSelectionListener(e -> line("list-event", e.getFirstIndex() + "," + e.getLastIndex() + "," + e.getValueIsAdjusting()));
        JViewport viewport = show(list, 180, 150);
        for (int step = 0; step < 40; step++) {
            int a = random.nextInt(300), b = Math.min(299, a + random.nextInt(12));
            switch (random.nextInt(4)) {
                case 0 -> list.setSelectionInterval(a, b);
                case 1 -> list.addSelectionInterval(a, b);
                case 2 -> list.removeSelectionInterval(a, b);
                default -> list.setSelectionMode(random.nextBoolean() ? ListSelectionModel.SINGLE_SELECTION : ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            }
            viewport.setViewPosition(new Point(0, random.nextInt(Math.max(1, list.getHeight() - 150))));
            line("list-sel", Arrays.toString(list.getSelectedIndices()));
            line("list-visible", list.getFirstVisibleIndex() + ".." + list.getLastVisibleIndex());
        }
        line("list-pixels", pixels(list));
        line("list-beans", beans(JList.class));
    }

    private static void probeTable(Random random) throws Exception {
        JTable table = Fixtures.table(400);
        table.getSelectionModel().addListSelectionListener(e -> line("table-event", e.getFirstIndex() + "," + e.getLastIndex() + "," + e.getValueIsAdjusting()));
        JViewport viewport = show(table, 300, 200);
        for (int step = 0; step < 40; step++) {
            int a = random.nextInt(400), b = Math.min(399, a + random.nextInt(20));
            switch (random.nextInt(5)) {
                case 0 -> table.setRowSelectionInterval(a, b);
                case 1 -> table.addRowSelectionInterval(a, b);
                case 2 -> table.removeRowSelectionInterval(a, b);
                case 3 -> table.setRowHeight(a, 10 + random.nextInt(30));
                default -> table.changeSelection(a, random.nextInt(2), random.nextBoolean(), random.nextBoolean());
            }
            viewport.setViewPosition(new Point(0, random.nextInt(Math.max(1, table.getHeight() - 200))));
            line("table-sel", Arrays.toString(table.getSelectedRows()) + " cols " + Arrays.toString(table.getSelectedColumns()));
            line("table-rows", table.rowAtPoint(new Point(0, table.getVisibleRect().y)) + " h " + table.getHeight());
        }
        line("table-pixels", pixels(table));
        line("table-beans", beans(JTable.class));
    }

    private static void probeTree(Random random) throws Exception {
        JTree tree = Fixtures.tree();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.addTreeSelectionListener(e -> line("tree-event", e.getPaths().length + "," + e.getNewLeadSelectionPath()));
        JViewport viewport = show(tree, 200, 150);
        for (int step = 0; step < 40; step++) {
            int rows = tree.getRowCount();
            int a = random.nextInt(rows), b = Math.min(rows - 1, a + random.nextInt(5));
            switch (random.nextInt(5)) {
                case 0 -> tree.setSelectionInterval(a, b);
                case 1 -> tree.addSelectionInterval(a, b);
                case 2 -> tree.expandRow(a);
                case 3 -> tree.collapseRow(a);
                default -> tree.scrollRowToVisible(a);
            }
            tree.setSize(Math.max(200, tree.getPreferredSize().width), tree.getPreferredSize().height);
            viewport.setViewPosition(new Point(0, random.nextInt(Math.max(1, tree.getHeight() - 150))));
            line("tree-sel", Arrays.toString(tree.getSelectionRows()) + " rows " + tree.getRowCount());
        }
        line("tree-pixels", pixels(tree));
        line("tree-beans", beans(JTree.class));
    }
}
