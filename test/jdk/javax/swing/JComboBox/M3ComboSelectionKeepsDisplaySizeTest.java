/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary A selection change of a non-editable JComboBox keeps the cached
 *          display size: the next baseline query measures no item, and the
 *          size equals a fresh measurement (every-widget D).
 * @run main/othervm -Djava.awt.headless=true M3ComboSelectionKeepsDisplaySizeTest
 */

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingUtilities;

public class M3ComboSelectionKeepsDisplaySizeTest {

    static final int ITEMS = 20_000;

    /* counts the renderer calls of the display size (index -1; the popup list asks with real indexes) */
    static final class Counting extends DefaultListCellRenderer {
        int calls;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                boolean focus) {
            if (index == -1) {
                calls++;
            }
            return super.getListCellRendererComponent(list, value, index, selected, focus);
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String[] items = new String[ITEMS];
            for (int i = 0; i < ITEMS; i++) {
                items[i] = "item " + i + (i == 4321 ? " is the widest of them all" : "");
            }
            JComboBox<String> combo = new JComboBox<>(items);
            Counting renderer = new Counting();
            combo.setRenderer(renderer);
            combo.setSize(combo.getPreferredSize());
            int baseline = combo.getBaseline(combo.getWidth(), combo.getHeight());
            Dimension before = combo.getPreferredSize();

            renderer.calls = 0;
            combo.setSelectedIndex(17);
            int after = combo.getBaseline(combo.getWidth(), combo.getHeight());
            System.out.println("renderer calls for the baseline after a selection change: " + renderer.calls);
            check(after == baseline, "the baseline is unchanged");
            check(renderer.calls < 10, "a selection change measures no item again, got " + renderer.calls + " calls");

            // the kept size equals a fresh measurement of the same items
            JComboBox<String> fresh = new JComboBox<>(items);
            fresh.setRenderer(new Counting());
            check(fresh.getPreferredSize().equals(combo.getPreferredSize()), "the kept size equals a fresh measurement");
            check(before.equals(combo.getPreferredSize()), "the preferred size is unchanged");

            // an editable combo still measures again (its editor shows the selection)
            combo.setEditable(true);
            combo.getBaseline(combo.getWidth(), combo.getHeight());
            renderer.calls = 0;
            combo.setSelectedIndex(18);
            combo.getBaseline(combo.getWidth(), combo.getHeight());
            check(renderer.calls >= ITEMS, "an editable combo measures its items again, got " + renderer.calls);
        });
        System.out.println("PASSED");
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
