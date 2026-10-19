/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
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
 * a deterministic trace (selection, geometry, events, painted pixels, XMLEncoder output). run-tests.sh diff runs it
 * against master's classes and against this branch's; equal output shows the new API changes nothing when unused.
 */
public final class M3SwingBehaviourProbe {

    private static final StringBuilder OUT = new StringBuilder();

    private M3SwingBehaviourProbe() {
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        Random random = new Random(42);
        probeList(random);
        probeTable(random);
        probeTree(random);
        System.out.print(OUT);
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

    private static String xml(Object bean) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (XMLEncoder encoder = new XMLEncoder(bytes)) {
            encoder.setExceptionListener(e -> line("xml-exception", e.getClass().getName()));
            encoder.writeObject(bean);
        }
        CRC32 crc = new CRC32();
        crc.update(bytes.toByteArray());
        return bytes.size() + ":" + Long.toHexString(crc.getValue());
    }

    private static void probeList(Random random) {
        JList<String> list = SelectionRangesTest.list(300);
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
        line("list-xml", xml(list));
    }

    private static void probeTable(Random random) {
        JTable table = SelectionRangesTest.table(400);
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
        line("table-xml", xml(table));
    }

    private static void probeTree(Random random) {
        JTree tree = SelectionRangesTest.tree();
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
        line("tree-xml", xml(tree));
    }
}
