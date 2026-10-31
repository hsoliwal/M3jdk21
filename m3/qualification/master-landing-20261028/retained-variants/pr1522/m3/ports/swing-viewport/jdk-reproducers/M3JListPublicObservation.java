/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Public JList observations for paired stock, receiving, and opt-in images.
 * @run main/othervm -Djava.awt.headless=true M3JListPublicObservation
 */

import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicListUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * No internal classes, counters, or reflection: run the same compiled fixture in
 * each image and compare its complete output. Every model change delivers its
 * normal DefaultListModel event. Eventless mutation remains the separate donor
 * regression, whose deliberate divergence is not included in this equivalence claim.
 */
public final class M3JListPublicObservation {

    static final class ExternalListUI extends BasicListUI { }

    private M3JListPublicObservation() { }

    static void observe(String label, JList<Object> list) {
        System.out.println(label + " preferred=" + list.getPreferredSize()
                + " scrollable=" + list.getPreferredScrollableViewportSize()
                + " selected=" + Arrays.toString(list.getSelectedIndices())
                + " visible=" + list.getFirstVisibleIndex() + "," + list.getLastVisibleIndex());
        int count = list.getModel().getSize();
        for (int row : new int[] {-1, 0, 1, 3, count / 2, count - 1, count}) {
            System.out.println("row=" + row + " at=" + list.indexToLocation(row)
                    + " bounds=" + list.getCellBounds(row, row));
        }
        for (int y : new int[] {-1, 0, 1, 19, 100, 200, 399, 1000, Integer.MAX_VALUE}) {
            System.out.println("y=" + y + " index=" + list.locationToIndex(new Point(31, y)));
        }
        Rectangle visible = new Rectangle(0, 19, 160, 120);
        for (int direction : new int[] {-1, 1}) {
            System.out.println("scroll=" + direction + " unit="
                    + list.getScrollableUnitIncrement(visible, SwingConstants.VERTICAL, direction)
                    + " block="
                    + list.getScrollableBlockIncrement(visible, SwingConstants.VERTICAL, direction));
        }
        BufferedImage image = new BufferedImage(240, 180, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            list.paint(graphics);
        } finally {
            graphics.dispose();
        }
        System.out.println("pixels=" + Arrays.hashCode(image.getRGB(0, 0, 240, 180, null, 0, 240)));
    }

    static void script() {
        DefaultListModel<Object> model = new DefaultListModel<>();
        JList<Object> list = new JList<>(model);
        list.setUI(new BasicListUI());
        list.setCellRenderer(new DefaultListCellRenderer());
        list.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        list.setSize(240, 180);
        list.setVisibleRowCount(7);
        list.setBorder(new EmptyBorder(3, 5, 7, 11));
        observe("empty", list);
        for (int row = 0; row < 37; row++) {
            model.addElement(row % 5 == 0 ? "<html>row " + row + "<br>second</html>"
                    : row % 7 == 0 ? "" : "row " + row);
        }
        list.setSelectionInterval(2, 6);
        observe("populated", list);
        model.add(3, "<html>inserted<br>second<br>third</html>");
        observe("inserted", list);
        model.set(5, "<html>changed<br>second</html>");
        model.remove(0);
        model.addElement(null);
        observe("changed-removed-null", list);
        list.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        observe("font", list);
        list.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        model.add(1, "right to left");
        observe("orientation", list);
        list.setFixedCellWidth(90);
        observe("fixed-width", list);
        list.setFixedCellHeight(23);
        observe("fixed-height", list);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        observe("horizontal-wrap", list);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setFixedCellHeight(-1);
        list.setFixedCellWidth(-1);
        list.setEnabled(false);
        model.addElement("disabled insertion");
        observe("disabled", list);
        list.setEnabled(true);
        ((DefaultListCellRenderer) list.getCellRenderer()).putClientProperty("html.disable", true);
        model.set(2, "<html>text<br>only</html>");
        observe("renderer-property", list);
        list.setCellRenderer(new DefaultListCellRenderer.UIResource());
        observe("renderer-replaced", list);
        list.setUI(new ExternalListUI());
        model.add(4, "external UI");
        observe("external-ui", list);
        list.setUI(new BasicListUI());
        model.clear();
        observe("clear", list);
        model.addElement("refill");
        observe("refill", list);
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        SwingUtilities.invokeAndWait(M3JListPublicObservation::script);
    }
}
