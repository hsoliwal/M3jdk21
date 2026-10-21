/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.desktop.jfx;

import com.m3.swing.viewport.M3ViewportIndexWindow;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeView;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Bounds;

/**
 * Optional JavaFX adapter over the existing M3 primitive viewport projection.
 * JavaFX controls and skins retain full ownership of cells, sorting, events and painting.
 * A VirtualFlow skin must report actual [first, end) visible row coordinates explicitly:
 * public ListView/TableView/TreeView APIs do not expose this range reliably.
 */
public final class M3JavaFxViewport {
    private M3JavaFxViewport() { }

    /** Source-provided visible row geometry, in JavaFX view (not model) coordinates. */
    public static void fromList(ListView<?> list, int first, int end, int overscan,
                                int[] output, int offset) {
        if (list == null) throw new NullPointerException("list");
        checkFxThread();
        int count = list.getItems() == null ? 0 : list.getItems().size();
        measured(list, count, first, end, overscan, output, offset);
    }

    /** SortedList/FilteredList TableView indices remain visible-view coordinates. */
    public static void fromTable(TableView<?> table, int first, int end, int overscan,
                                 int[] output, int offset) {
        if (table == null) throw new NullPointerException("table");
        checkFxThread();
        int count = table.getItems() == null ? 0 : table.getItems().size();
        measured(table, count, first, end, overscan, output, offset);
    }

    /** Count is expanded/visible tree rows, not the full logical node universe. */
    public static void fromTree(TreeView<?> tree, int first, int end, int overscan,
                                int[] output, int offset) {
        if (tree == null) throw new NullPointerException("tree");
        checkFxThread();
        measured(tree, tree.getExpandedItemCount(), first, end, overscan, output, offset);
    }

    /**
     * A JavaFX ScrollPane with uniform row-height content; actual public viewport bounds and
     * normalized scrolling position provide the visible window. Not for a ListView VirtualFlow.
     */
    public static void fromUniformScrollPane(ScrollPane pane, int logicalCount, double rowHeight,
                                            int overscan, int[] output, int offset) {
        if (pane == null) throw new NullPointerException("pane");
        checkFxThread();
        if (!(rowHeight > 0.0) || !Double.isFinite(rowHeight)) {
            throw new IllegalArgumentException("rowHeight must be positive and finite");
        }
        Node content = pane.getContent();
        if (pane.getScene() == null || !pane.isVisible() || content == null || !content.isVisible()) {
            M3ViewportIndexWindow.project(logicalCount, 0, 0, overscan, output, offset);
            return;
        }
        Bounds viewport = pane.getViewportBounds();
        double height = viewport.getHeight();
        double travel = Math.max(0.0, content.getLayoutBounds().getHeight() - height);
        double scale = pane.getVmax() - pane.getVmin();
        double fraction = scale <= 0.0 ? 0.0 : Math.max(0.0,
                Math.min(1.0, (pane.getVvalue() - pane.getVmin()) / scale));
        int first = clampToInt(Math.floor(travel * fraction / rowHeight));
        int visible = height <= 0.0 ? 0 : clampToInt(Math.ceil(height / rowHeight) + 1.0);
        M3ViewportIndexWindow.project(logicalCount, first, visible, overscan, output, offset);
    }

    private static void measured(Node view, int count, int first, int end, int overscan,
                                 int[] output, int offset) {
        if (end < first) throw new IllegalArgumentException("reversed cells");
        if (view.getScene() == null || !view.isVisible() || view.getLayoutBounds().isEmpty()) {
            M3ViewportIndexWindow.project(count, 0, 0, overscan, output, offset);
            return;
        }
        M3ViewportIndexWindow.project(count, first, end - first, overscan, output, offset);
    }

    private static int clampToInt(double value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE :
                value <= 0.0 ? 0 : (int) value;
    }

    private static void checkFxThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("JavaFX application thread required");
        }
    }
}
