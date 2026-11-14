/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary A selection change of a JComboBox does not make its popup list
 *          measure every cell again; the list keeps the same cell bounds
 *          (every-widget D).
 * @run main/othervm -Djava.awt.headless=true M3ComboPopupListSelectionTest
 */

import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.ComboPopup;

public class M3ComboPopupListSelectionTest {

    static final int ITEMS = 20_000;

    /* counts the renderer calls of the popup list (cell indexes; the combo's display size asks with -1) */
    static final class Counting extends DefaultListCellRenderer {
        int calls;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                boolean focus) {
            if (index >= 0) {
                calls++;
            }
            return super.getListCellRendererComponent(list, value, index, selected, focus);
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String[] items = new String[ITEMS];
            for (int i = 0; i < ITEMS; i++) {
                items[i] = "item " + i;
            }
            JComboBox<String> combo = new JComboBox<>(items);
            Counting renderer = new Counting();
            combo.setRenderer(renderer);
            JList<?> list = ((ComboPopup) combo.getAccessibleContext().getAccessibleChild(0)).getList();
            combo.setSelectedIndex(3);
            Rectangle before = list.getCellBounds(17, 17);

            renderer.calls = 0;
            combo.setSelectedIndex(17);
            Rectangle after = list.getCellBounds(17, 17);
            System.out.println("popup list renderer calls for a selection change: " + renderer.calls);
            check(list.getSelectedIndex() == 17, "the popup list follows the selection");
            check(before.equals(after), "the cell bounds are unchanged: " + before + " / " + after);
            check(renderer.calls < 100, "a selection change measures no list cell again, got " + renderer.calls);

            // a real content change still measures again
            renderer.calls = 0;
            combo.insertItemAt("a new first item that is much wider than the others", 0);
            list.getCellBounds(0, 0);
            check(renderer.calls >= ITEMS, "an inserted item still measures the list, got " + renderer.calls);
        });
        System.out.println("PASSED");
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
