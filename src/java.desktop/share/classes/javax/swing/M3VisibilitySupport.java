/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package javax.swing;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;

/**
 * The viewport observer of one {@link JList}, {@link JTable} or {@link JTree} (leaf S9): the last announced range
 * and extents, whether the viewport was hidden and dirty when last seen, and the listeners it installed on the
 * component, its viewport, its window and its models. The first visibility listener or the first
 * {@code isViewportDirty()} creates it; it lives as long as the component and follows its viewport, window and
 * models when they change.
 * <p>
 * A show or hide arrives as a {@code SHOWING_CHANGED} hierarchy event, sent synchronously while the component or an
 * ancestor changes its visibility, before Swing validates and paints it; the window delta of a show is therefore
 * announced before the first paint. Model events are coalesced into one check on the event queue, after the
 * component has processed them.
 */
final class M3VisibilitySupport implements HierarchyListener, ComponentListener, ChangeListener,
        PropertyChangeListener, WindowStateListener, TableModelListener, ListDataListener, TreeModelListener,
        TreeExpansionListener, TableColumnModelListener {

    private final JComponent component;
    private int first;
    private int last = -1;
    private int width = -1;
    private int height = -1;
    private int rows = -1;
    private int rowExtent = -1;
    private int contentWidth = -1;
    private boolean hidden;
    private boolean dirty;
    private boolean scheduled;
    private boolean extentsDirty = true;
    private JViewport viewport;
    private Window window;
    private Object model;
    private TableColumnModel columns;

    M3VisibilitySupport(JComponent component) {
        this.component = component;
        component.addHierarchyListener(this);
        component.addComponentListener(this);
        component.addPropertyChangeListener(this);
        if (component instanceof JTree tree) {
            tree.addTreeExpansionListener(this);
        }
        attach();
        hidden = hidden(component);
    }

    /**
     * Whether the viewport of a component is hidden: the component is not showing, its window is iconified, or its
     * visible rectangle is empty.
     */
    static boolean hidden(JComponent c) {
        if (!c.isShowing()) {
            return true;
        }
        if (SwingUtilities.getWindowAncestor(c) instanceof Frame frame
                && (frame.getExtendedState() & Frame.ICONIFIED) != 0) {
            return true;
        }
        Rectangle visible = c.getVisibleRect();
        return visible.width <= 0 || visible.height <= 0;
    }

    boolean isDirty() {
        return dirty;
    }

    /* the first listener hears the current state: HIDDEN, or the visible rows as ENTERED and the extents */
    void add(M3VisibilityListener listener) {
        Objects.requireNonNull(listener, "listener");
        boolean firstListener = !listening();
        component.listenerList.add(M3VisibilityListener.class, listener);
        if (!firstListener) {
            return;
        }
        first = 0;
        last = -1;
        width = height = rows = rowExtent = contentWidth = -1;
        extentsDirty = true;
        boolean wasHidden = hidden;
        hidden = hidden(component);
        if (hidden) {
            fire(M3VisibilityEvent.HIDDEN, 0, -1, 0, 0, 0);
            return;
        }
        if (wasHidden) {
            dirty = false;
        }
        window();
    }

    /* ------------------------------------------------------------------ triggers */

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED)) != 0) {
            attach();
        }
        extentsDirty = true;
        check();
    }

    @Override
    public void componentResized(ComponentEvent e) {
        extentsDirty = true;
        check();
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        check();
    }

    @Override
    public void componentShown(ComponentEvent e) {
        check();
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        check();
    }

    /* the viewport scrolled or changed its extent */
    @Override
    public void stateChanged(ChangeEvent e) {
        Dimension extent = viewport.getExtentSize();
        if (extent.width != width || extent.height != height) {
            extentsDirty = true;
        }
        check();
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        switch (Objects.requireNonNullElse(e.getPropertyName(), "")) {
            case "model", "columnModel" -> {
                attach();
                modelChanged();
            }
            case "rowHeight", "fixedCellHeight", "font", "prototypeCellValue", "layoutOrientation", "rootVisible",
                    "showsRootHandles" -> {
                extentsDirty = true;
                schedule();
            }
            default -> {
                // not an extent of the viewport
            }
        }
    }

    @Override
    public void windowStateChanged(WindowEvent e) {
        check();
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        modelChanged();
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        modelChanged();
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        modelChanged();
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        modelChanged();
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        modelChanged();
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
        modelChanged();
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
        modelChanged();
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
        modelChanged();
    }

    @Override
    public void treeExpanded(TreeExpansionEvent e) {
        modelChanged();
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent e) {
        modelChanged();
    }

    @Override
    public void columnAdded(TableColumnModelEvent e) {
        columnsChanged();
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
        columnsChanged();
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        columnsChanged();
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
        columnsChanged();
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        // the selection is not an extent
    }

    /* ------------------------------------------------------------------ the observer */

    /* the rows changed: a hidden viewport becomes dirty and announces nothing; a visible one is checked once, later */
    private void modelChanged() {
        extentsDirty = true;
        if (visibility()) {
            dirty = true;
            return;
        }
        schedule();
    }

    private void columnsChanged() {
        extentsDirty = true;
        schedule();
    }

    /* one check on the event queue, after the component has processed the change */
    private void schedule() {
        if (scheduled) {
            return;
        }
        scheduled = true;
        SwingUtilities.invokeLater(() -> {
            scheduled = false;
            check();
        });
    }

    private void check() {
        if (!visibility() && listening()) {
            window();
        }
    }

    /*
     * Answers whether the viewport is hidden now, and announces a change: HIDDEN; or, when it shows, SHOWN while
     * still dirty, the window delta and the extents, then clean.
     */
    private boolean visibility() {
        boolean now = hidden(component);
        if (now == hidden) {
            return now;
        }
        hidden = now;
        if (now) {
            fire(M3VisibilityEvent.HIDDEN, 0, -1, 0, 0, 0);
            return true;
        }
        if (listening()) {
            fire(M3VisibilityEvent.SHOWN, 0, -1, 0, 0, 0);
            extentsDirty = true;
            window();
        }
        dirty = false;
        return false;
    }

    /* the window delta of a visible viewport: the rows that left, the rows that entered, then new extents */
    private void window() {
        M3ItemRange visible = visibleRange(component);
        int newFirst = visible.isEmpty() ? 0 : visible.start();
        int newLast = visible.isEmpty() ? -1 : visible.end();
        if (newFirst != first || newLast != last) {
            int oldFirst = first;
            int oldLast = last;
            first = newFirst;
            last = newLast;
            difference(M3VisibilityEvent.LEFT, oldFirst, oldLast, newFirst, newLast);
            difference(M3VisibilityEvent.ENTERED, newFirst, newLast, oldFirst, oldLast);
        }
        if (extentsDirty) {
            extents();
        }
    }

    /* the rows of [from, to] outside [cutFirst, cutLast], as at most two ranges */
    private void difference(int detail, int from, int to, int cutFirst, int cutLast) {
        if (to < from) {
            return;
        }
        if (cutLast < cutFirst || cutLast < from || cutFirst > to) {
            fire(detail, from, to, 0, 0, 0);
            return;
        }
        if (from < cutFirst) {
            fire(detail, from, cutFirst - 1, 0, 0, 0);
        }
        if (cutLast < to) {
            fire(detail, cutLast + 1, to, 0, 0, 0);
        }
    }

    /* EXTENTS when the size of the viewport, the rows, the row height or the content width changed */
    private void extents() {
        extentsDirty = false;
        Rectangle area = viewport != null ? new Rectangle(viewport.getExtentSize()) : component.getVisibleRect();
        int count = rowCount(component);
        int extent = rowExtent(component);
        int content = component.getWidth();
        if (area.width == width && area.height == height && count == rows && extent == rowExtent
                && content == contentWidth) {
            return;
        }
        width = area.width;
        height = area.height;
        rows = count;
        rowExtent = extent;
        contentWidth = content;
        fire(M3VisibilityEvent.EXTENTS, 0, -1, area.width, area.height, count);
    }

    private boolean listening() {
        return component.listenerList.getListenerCount(M3VisibilityListener.class) > 0;
    }

    private void fire(int detail, int start, int end, int w, int h, int count) {
        M3VisibilityListener[] listeners = component.listenerList.getListeners(M3VisibilityListener.class);
        if (listeners.length == 0) {
            return;
        }
        M3VisibilityEvent event = new M3VisibilityEvent(component, detail, start, end, w, h, count);
        for (M3VisibilityListener listener : listeners) {
            listener.visibilityChanged(event);
        }
    }

    /* follows the viewport, the window and the models of the component */
    private void attach() {
        JViewport port = component.getParent() instanceof JViewport v ? v : null;
        if (port != viewport) {
            if (viewport != null) {
                viewport.removeChangeListener(this);
            }
            viewport = port;
            if (port != null) {
                port.addChangeListener(this);
            }
        }
        Window w = SwingUtilities.getWindowAncestor(component);
        if (w != window) {
            if (window != null) {
                window.removeWindowStateListener(this);
            }
            window = w;
            if (w != null) {
                w.addWindowStateListener(this);
            }
        }
        Object m = model(component);
        if (m != model) {
            listen(model, false);
            model = m;
            listen(m, true);
        }
        TableColumnModel c = component instanceof JTable table ? table.getColumnModel() : null;
        if (c != columns) {
            if (columns != null) {
                columns.removeColumnModelListener(this);
            }
            columns = c;
            if (c != null) {
                c.addColumnModelListener(this);
            }
        }
    }

    private void listen(Object m, boolean add) {
        if (m == null) {
            return;
        }
        if (component instanceof JTable) {
            if (add) {
                ((TableModel) m).addTableModelListener(this);
            } else {
                ((TableModel) m).removeTableModelListener(this);
            }
        } else if (component instanceof JList) {
            if (add) {
                ((ListModel<?>) m).addListDataListener(this);
            } else {
                ((ListModel<?>) m).removeListDataListener(this);
            }
        } else if (add) {
            ((TreeModel) m).addTreeModelListener(this);
        } else {
            ((TreeModel) m).removeTreeModelListener(this);
        }
    }

    private static Object model(JComponent c) {
        if (c instanceof JTable table) {
            return table.getModel();
        }
        if (c instanceof JList<?> list) {
            return list.getModel();
        }
        return ((JTree) c).getModel();
    }

    private static M3ItemRange visibleRange(JComponent c) {
        if (c instanceof JTable table) {
            return table.getVisibleRange();
        }
        if (c instanceof JList<?> list) {
            return list.getVisibleRange();
        }
        return ((JTree) c).getVisibleRange();
    }

    private static int rowCount(JComponent c) {
        if (c instanceof JTable table) {
            return table.getRowCount();
        }
        if (c instanceof JList<?> list) {
            return list.getModel().getSize();
        }
        return ((JTree) c).getRowCount();
    }

    private static int rowExtent(JComponent c) {
        if (c instanceof JTable table) {
            return table.getRowHeight();
        }
        if (c instanceof JList<?> list) {
            return list.getFixedCellHeight();
        }
        return ((JTree) c).getRowHeight();
    }
}
