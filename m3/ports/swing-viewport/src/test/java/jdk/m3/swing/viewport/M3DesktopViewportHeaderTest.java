/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.ComponentOrientation;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import org.junit.jupiter.api.Test;

/** Horizontal header geometry from real scroll-pane layout, including a zero-height table body. */
final class M3DesktopViewportHeaderTest {
    private static final class ShowingTable extends JTable {
        private static final long serialVersionUID = 1L;
        ShowingTable(int rows, int columns) { super(new DefaultTableModel(rows, columns)); }
        @Override public boolean isShowing() { return true; }
    }

    private record Scene(ShowingTable table, JScrollPane pane) { }

    private static Scene scene(int rows, int... widths) {
        ShowingTable table = new ShowingTable(rows, widths.length);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < widths.length; ++i) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setMinWidth(0);
            column.setPreferredWidth(widths[i]);
            column.setWidth(widths[i]);
        }
        JScrollPane pane = new JScrollPane(table);
        // Displayable JTable configures this header itself. Install that same
        // component explicitly while exercising layout without a native peer.
        pane.setColumnHeaderView(table.getTableHeader());
        pane.setSize(180, 160);
        layout(pane);
        return new Scene(table, pane);
    }

    private static void layout(JScrollPane pane) {
        pane.doLayout();
        pane.getViewport().doLayout();
        pane.getColumnHeader().doLayout();
    }

    private static int[] window(JTable table) {
        int[] actual = {-9, -9, -9, -9};
        M3DesktopViewportAdapters.fromTableColumns(table, 0, actual, 0);
        return actual;
    }

    private static void matchesHorizontalPixels(Scene scene) {
        Rectangle clip = scene.table().getVisibleRect();
        assertTrue(clip.width > 0, "the real horizontal clip is nonempty");
        int first = Integer.MAX_VALUE;
        int end = 0;
        for (int x = clip.x; x < clip.x + clip.width; ++x) {
            int hit = scene.table().columnAtPoint(new Point(x, 0));
            if (hit >= 0) {
                first = Math.min(first, hit);
                end = Math.max(end, hit + 1);
            }
        }
        assertTrue(end > 0, "at least one header column intersects the horizontal clip");
        assertArrayEquals(new int[]{first, end, first, end}, window(scene.table()));
    }

    @Test void emptyTableHasVisibleHeaderColumnsAfterNormalLayout() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(0, 40, 60, 90, 110);
            assertEquals(0, scene.table().getHeight());
            assertEquals(0, scene.table().getVisibleRect().height);
            assertTrue(scene.table().getTableHeader().getVisibleRect().height > 0);
            matchesHorizontalPixels(scene);
        });
    }

    @Test void nonemptyTableWithZeroHeightBodyClipStillProjectsColumns() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(3, 40, 60, 90, 110);
            scene.pane().getViewport().setSize(scene.pane().getViewport().getWidth(), 0);
            assertTrue(scene.table().getHeight() > 0);
            assertEquals(0, scene.table().getVisibleRect().height);
            matchesHorizontalPixels(scene);
        });
    }

    @Test void emptyRtlTableUsesItsMeasuredRightEdge() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(0, 40, 60, 90, 110);
            scene.pane().applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            layout(scene.pane());
            for (int x : new int[]{0, 30, 90}) {
                scene.pane().getViewport().setViewPosition(new Point(x, 0));
                matchesHorizontalPixels(scene);
            }
        });
    }

    @Test void reorderedEmptyHeaderKeepsItsViewToModelPermutation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(0, 40, 60, 90, 110);
            scene.table().moveColumn(3, 0);
            scene.pane().getViewport().setViewPosition(new Point(70, 0));
            matchesHorizontalPixels(scene);
            int[] measured = window(scene.table());
            int[] actual = new int[measured[1] - measured[0]];
            assertEquals(actual.length, M3DesktopViewportAdapters.copyTableModelColumns(
                    scene.table(), measured, 0, actual, 0));
            for (int i = 0; i < actual.length; ++i) {
                assertEquals(scene.table().convertColumnIndexToModel(measured[0] + i), actual[i]);
            }
        });
    }

    @Test void zeroWidthBodyClipCannotRequestHeaderColumns() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(0, 40, 60, 90);
            scene.table().setSize(0, 0);
            assertTrue(scene.table().getVisibleRect().width <= 0);
            assertArrayEquals(new int[4], window(scene.table()));
        });
    }

    @Test void negativeWidthBodyClipCannotRequestHeaderColumns() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(0, 40, 60, 90);
            scene.table().setSize(-7, 0);
            assertTrue(scene.table().getVisibleRect().width <= 0);
            assertArrayEquals(new int[4], window(scene.table()));
        });
    }

    @Test void verticalExtentDoesNotChangeAnExistingHorizontalWindow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(3, 40, 60, 90, 110);
            scene.pane().getViewport().setViewPosition(new Point(40, 0));
            int[] expected = window(scene.table());
            assertTrue(expected[1] > expected[0]);
            scene.pane().getViewport().setSize(scene.pane().getViewport().getWidth(), 0);
            assertEquals(0, scene.table().getVisibleRect().height);
            assertArrayEquals(expected, window(scene.table()));
        });
    }
}
