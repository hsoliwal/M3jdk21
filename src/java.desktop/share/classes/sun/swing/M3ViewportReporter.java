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

package sun.swing;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ListModel;
import javax.swing.RepaintManager;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.PartVisibilityAdapter;
import javax.swing.event.PartVisibilityEvent;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.ViewportEvent;
import javax.swing.event.ViewportListener;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * The reporter behind {@code JComponent.addViewportListener} (M3JDK, brief rule 25; the Swing counterpart of SWT's
 * {@code org.eclipse.swt.internal.ViewportReporter}). A JList, JTree or JTable gets one reporter when its first
 * {@link ViewportListener} is added and loses it with the last; other components get none, and a view without a
 * listener tracks nothing.
 * <p>
 * The reporter listens to what can move the window of rows: the enclosing JViewport (scroll, resize, view size),
 * the view's size, the model (rows inserted, removed, changed), tree expansion, the row sorter, the layout
 * properties, and part visibility. Each of these only records a cause bit and schedules one flush. The flush runs
 * once per event dispatch turn, after layout and before paint: from the RepaintManager pass, right after it
 * validates, or from an invokeLater when no pass is coming. A flush while the view is not part-visible waits; the
 * first one after the view shows again adds {@link ViewportEvent#VISIBILITY}.
 * <p>
 * A row's identity is its list index or table model index, moved through the inserts and removes the model reports,
 * or its tree node. So the entered rows are those of the new window that were not in the last reported one, and the
 * left rows are those of the last reported window that are gone or out of the new one; a row that a change only
 * moved is in neither.
 */
//FLLDCIM: M3 swing viewport listener - one coalesced event per turn, after layout, never while hidden
public final class M3ViewportReporter extends ComponentAdapter implements ChangeListener, HierarchyListener,
        PropertyChangeListener {

    /** Reporters with a pending flush; event dispatch thread, guarded by itself for model events from elsewhere. */
    private static final List<M3ViewportReporter> PENDING = new ArrayList<>();
    private static volatile boolean anyPending;
    private static boolean flushQueued;
    private static final Object[] NO_KEYS = {};

    /** What the reporter needs from a view kind. */
    private abstract static class Host {
        /** The first row in {@code r}, or -1. */
        abstract int first(Rectangle r);

        /** The last row in {@code r}, or -1; called only when {@link #first} found one. */
        abstract int last(Rectangle r, int first);

        /** The identity of a row in the current window. */
        abstract Object key(int row);

        /** An empty set with the identity semantics of the keys. */
        Set<Object> keys() {
            return new HashSet<>();
        }

        abstract void bind(M3ViewportReporter r);

        abstract void unbind(M3ViewportReporter r);
    }

    private final JComponent view;
    private final Host host;
    private final PartVisibilityAdapter visibility = new PartVisibilityAdapter() {
        @Override
        public void partVisible(PartVisibilityEvent e) {
            hidden = true;
            schedule(ViewportEvent.VISIBILITY);
        }
    };
    private JViewport viewport;
    private int cause;
    private boolean dirty;
    private boolean hidden;
    private int lastFirst = -1;
    private Object[] lastKeys = NO_KEYS;
    private Rectangle lastRect;
    private int lastWidth;
    private int lastHeight;

    private M3ViewportReporter(JComponent view, Host host) {
        this.view = view;
        this.host = host;
    }

    /** {@code c} got its first viewport listener: a JList, JTree or JTable gets a reporter; nothing is fired. */
    public static void listenerAdded(JComponent c) {
        if (reporter(c) != null) {
            return;
        }
        Host host = c instanceof JTable t ? new TableHost(t) : c instanceof JTree t ? new TreeHost(t)
                : c instanceof JList<?> l ? new ListHost(l) : null;
        if (host == null) {
            return;
        }
        M3ViewportReporter r = new M3ViewportReporter(c, host);
        c.addHierarchyListener(r);
        c.addComponentListener(r);
        c.addPropertyChangeListener(r);
        c.addPartVisibilityListener(r.visibility);
        r.bindViewport();
        host.bind(r);
        r.record(c.isPartVisible());
    }

    /** {@code c} lost its last viewport listener: the reporter goes. */
    public static void listenerRemoved(JComponent c) {
        M3ViewportReporter r = reporter(c);
        if (r == null) {
            return;
        }
        c.removeHierarchyListener(r);
        c.removeComponentListener(r);
        c.removePropertyChangeListener(r);
        c.removePartVisibilityListener(r.visibility);
        r.setViewport(null);
        r.host.unbind(r);
        synchronized (PENDING) {
            PENDING.remove(r);
        }
    }

    /** Runs the pending flushes; called by the RepaintManager pass after it validates, and by the queued flush. */
    public static void flushAll() {
        if (!anyPending) {
            return;
        }
        M3ViewportReporter[] due;
        synchronized (PENDING) {
            due = PENDING.toArray(new M3ViewportReporter[0]);
            PENDING.clear();
            anyPending = false;
        }
        for (M3ViewportReporter r : due) {
            r.flush();
        }
    }

    private static M3ViewportReporter reporter(JComponent c) {
        for (HierarchyListener l : c.getHierarchyListeners()) {
            if (l instanceof M3ViewportReporter r) {
                return r;
            }
        }
        return null;
    }

    /** Records {@code bits} and makes sure one flush is coming. */
    void schedule(int bits) {
        boolean queue = false;
        synchronized (PENDING) {
            cause |= bits;
            if (!dirty) {
                dirty = true;
                PENDING.add(this);
                anyPending = true;
            }
            if (!flushQueued) {
                flushQueued = true;
                queue = true;
            }
        }
        if (queue) {
            SwingUtilities.invokeLater(M3ViewportReporter::queuedFlush);
        }
    }

    /** The invokeLater flush: lays out what is pending, as the RepaintManager pass would, then flushes. */
    private static void queuedFlush() {
        synchronized (PENDING) {
            flushQueued = false;
            if (!anyPending) {
                return;
            }
        }
        M3ViewportReporter first;
        synchronized (PENDING) {
            first = PENDING.isEmpty() ? null : PENDING.get(0);
        }
        if (first != null) {
            RepaintManager.currentManager(first.view).validateInvalidComponents();
        }
        flushAll();
    }

    /** Compares the window with the last one reported and fires one event when anything changed. */
    private void flush() {
        int bits;
        synchronized (PENDING) {
            dirty = false;
            bits = cause;
            cause = 0;
        }
        if (!view.isPartVisible()) {
            synchronized (PENDING) {
                cause |= bits;
            }
            return;
        }
        if (hidden) {
            hidden = false;
            bits |= ViewportEvent.VISIBILITY;
        }
        Rectangle r = view.getVisibleRect();
        if (lastRect != null && (r.x != lastRect.x || r.y != lastRect.y)) {
            bits |= ViewportEvent.SCROLL;
        }
        if (lastRect != null && (r.width != lastRect.width || r.height != lastRect.height)) {
            bits |= ViewportEvent.RESIZE;
        }
        boolean extents = view.getWidth() != lastWidth || view.getHeight() != lastHeight;
        int first = host.first(r);
        int last = first < 0 ? -1 : host.last(r, first);
        Object[] keys = keys(first, last);
        Set<Object> now = host.keys();
        Collections.addAll(now, keys);
        Set<Object> before = host.keys();
        for (Object k : lastKeys) {
            if (k != null) {
                before.add(k);
            }
        }
        int[] entered = rows(keys, first, before, false);
        int[] left = rows(lastKeys, lastFirst, now, true);
        if (bits == 0 && (entered.length > 0 || left.length > 0)) {
            bits = ViewportEvent.LAYOUT;
        }
        store(first, keys, r);
        if (bits == 0 && !extents) {
            return;
        }
        ViewportEvent e = new ViewportEvent(view, first, last, entered, left, extents, bits);
        for (ViewportListener l : view.getViewportListeners()) {
            l.viewportChanged(e);
        }
    }

    /** Takes the current window as the last one reported, without an event (at add time, nothing is fired). */
    private void record(boolean visible) {
        if (!visible) {
            hidden = true;
            return;
        }
        Rectangle r = view.getVisibleRect();
        int first = host.first(r);
        store(first, keys(first, first < 0 ? -1 : host.last(r, first)), r);
    }

    private void store(int first, Object[] keys, Rectangle r) {
        lastFirst = first;
        lastKeys = keys;
        lastRect = r;
        lastWidth = view.getWidth();
        lastHeight = view.getHeight();
    }

    private Object[] keys(int first, int last) {
        if (first < 0 || last < first) {
            return NO_KEYS;
        }
        Object[] keys = new Object[last - first + 1];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = host.key(first + i);
        }
        return keys;
    }

    /**
     * The rows {@code base + i} whose key is not in {@code other} (or, when {@code goneToo}, is null), as ascending
     * inclusive pairs.
     */
    private static int[] rows(Object[] keys, int base, Set<Object> other, boolean goneToo) {
        int[] pairs = new int[8];
        int n = 0;
        for (int i = 0; i < keys.length; i++) {
            boolean out = keys[i] == null ? goneToo : !other.contains(keys[i]);
            if (!out) {
                continue;
            }
            int row = base + i;
            if (n > 0 && pairs[n - 1] == row - 1) {
                pairs[n - 1] = row;
            } else {
                if (n + 2 > pairs.length) {
                    pairs = java.util.Arrays.copyOf(pairs, pairs.length * 2);
                }
                pairs[n++] = row;
                pairs[n++] = row;
            }
        }
        return java.util.Arrays.copyOf(pairs, n);
    }

    /** Model rows {@code index0..index0+count-1} were inserted: list and table keys at or past index0 move down. */
    void inserted(int index0, int count) {
        Object[] keys = lastKeys.clone();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] instanceof Integer k && k >= index0) {
                keys[i] = k + count;
            }
        }
        lastKeys = keys;
        schedule(ViewportEvent.MODEL_DELTA);
    }

    /** Model rows {@code index0..index1} were removed: their keys go, later keys move up. */
    void removed(int index0, int index1) {
        Object[] keys = lastKeys.clone();
        int count = index1 - index0 + 1;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] instanceof Integer k && k >= index0) {
                keys[i] = k <= index1 ? null : Integer.valueOf(k - count);
            }
        }
        lastKeys = keys;
        schedule(ViewportEvent.MODEL_DELTA);
    }

    /** Every row may be another one now (data or structure changed, a new model): every key goes. */
    void replaced(int bits) {
        lastKeys = new Object[lastKeys.length];
        schedule(bits);
    }

    private void bindViewport() {
        setViewport(view.getParent() instanceof JViewport v && v.getView() == view ? v : null);
    }

    private void setViewport(JViewport v) {
        if (v == viewport) {
            return;
        }
        if (viewport != null) {
            viewport.removeChangeListener(this);
        }
        viewport = v;
        if (v != null) {
            v.addChangeListener(this);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        schedule(0);
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
            bindViewport();
            schedule(0);
        }
    }

    @Override
    public void componentResized(ComponentEvent e) {
        schedule(0);
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        schedule(0);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        String name = e.getPropertyName();
        if ("model".equals(name) || "sorter".equals(name) || "rowSorter".equals(name)) {
            host.unbind(this);
            host.bind(this);
            replaced("model".equals(name) ? ViewportEvent.MODEL_DELTA : ViewportEvent.LAYOUT);
        } else if ("rowHeight".equals(name) || "font".equals(name) || "fixedCellHeight".equals(name)
                || "fixedCellWidth".equals(name) || "layoutOrientation".equals(name)
                || "visibleRowCount".equals(name) || "rootVisible".equals(name)
                || "showsRootHandles".equals(name) || "cellRenderer".equals(name)
                || "prototypeCellValue".equals(name) || "largeModel".equals(name)) {
            schedule(ViewportEvent.LAYOUT);
        }
    }

    /** JList: the list index, moved through the inserts and removes of the ListModel. */
    private static final class ListHost extends Host implements ListDataListener {
        final JList<?> list;
        ListModel<?> model;
        M3ViewportReporter reporter;

        ListHost(JList<?> list) {
            this.list = list;
        }

        @Override
        int first(Rectangle r) {
            return r.isEmpty() ? -1 : list.getFirstVisibleIndex();
        }

        @Override
        int last(Rectangle r, int first) {
            return Math.max(first, list.getLastVisibleIndex());
        }

        @Override
        Object key(int row) {
            return row;
        }

        @Override
        void bind(M3ViewportReporter r) {
            reporter = r;
            model = list.getModel();
            model.addListDataListener(this);
        }

        @Override
        void unbind(M3ViewportReporter r) {
            if (model != null) {
                model.removeListDataListener(this);
                model = null;
            }
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            reporter.inserted(Math.min(e.getIndex0(), e.getIndex1()), Math.abs(e.getIndex1() - e.getIndex0()) + 1);
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            reporter.removed(Math.min(e.getIndex0(), e.getIndex1()), Math.max(e.getIndex0(), e.getIndex1()));
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            reporter.schedule(ViewportEvent.MODEL_DELTA);
        }
    }

    /** JTable: the model index of a view row, moved through the inserts and removes of the TableModel. */
    private static final class TableHost extends Host implements TableModelListener, RowSorterListener {
        final JTable table;
        TableModel model;
        RowSorter<?> sorter;
        M3ViewportReporter reporter;

        TableHost(JTable table) {
            this.table = table;
        }

        @Override
        int first(Rectangle r) {
            return r.isEmpty() || table.getRowCount() == 0 ? -1 : table.rowAtPoint(new Point(r.x, r.y));
        }

        @Override
        int last(Rectangle r, int first) {
            int last = table.rowAtPoint(new Point(r.x, r.y + r.height - 1));
            return last < 0 ? table.getRowCount() - 1 : last;
        }

        @Override
        Object key(int row) {
            return table.convertRowIndexToModel(row);
        }

        @Override
        void bind(M3ViewportReporter r) {
            reporter = r;
            model = table.getModel();
            model.addTableModelListener(this);
            sorter = table.getRowSorter();
            if (sorter != null) {
                sorter.addRowSorterListener(this);
            }
        }

        @Override
        void unbind(M3ViewportReporter r) {
            if (model != null) {
                model.removeTableModelListener(this);
                model = null;
            }
            if (sorter != null) {
                sorter.removeRowSorterListener(this);
                sorter = null;
            }
        }

        @Override
        public void tableChanged(TableModelEvent e) {
            int first = e.getFirstRow();
            int last = e.getLastRow();
            if (first == TableModelEvent.HEADER_ROW || last == Integer.MAX_VALUE) {
                reporter.replaced(ViewportEvent.MODEL_DELTA);
            } else if (e.getType() == TableModelEvent.INSERT) {
                reporter.inserted(first, last - first + 1);
            } else if (e.getType() == TableModelEvent.DELETE) {
                reporter.removed(first, last);
            } else {
                reporter.schedule(ViewportEvent.MODEL_DELTA);
            }
        }

        @Override
        public void sorterChanged(RowSorterEvent e) {
            reporter.schedule(ViewportEvent.LAYOUT);
        }
    }

    /** JTree: the node of a row (the last component of its path). */
    private static final class TreeHost extends Host implements TreeModelListener, TreeExpansionListener {
        final JTree tree;
        TreeModel model;
        M3ViewportReporter reporter;

        TreeHost(JTree tree) {
            this.tree = tree;
        }

        @Override
        int first(Rectangle r) {
            if (r.isEmpty() || tree.getRowCount() == 0) {
                return -1;
            }
            int row = tree.getClosestRowForLocation(r.x, r.y);
            Rectangle b = row < 0 ? null : tree.getRowBounds(row);
            return b == null || b.y + b.height <= r.y || b.y >= r.y + r.height ? -1 : row;
        }

        @Override
        int last(Rectangle r, int first) {
            return Math.max(first, tree.getClosestRowForLocation(r.x, r.y + r.height - 1));
        }

        @Override
        Object key(int row) {
            TreePath p = tree.getPathForRow(row);
            return p == null ? null : p.getLastPathComponent();
        }

        @Override
        Set<Object> keys() {
            return Collections.newSetFromMap(new IdentityHashMap<>());
        }

        @Override
        void bind(M3ViewportReporter r) {
            reporter = r;
            model = tree.getModel();
            if (model != null) {
                model.addTreeModelListener(this);
            }
            tree.addTreeExpansionListener(this);
        }

        @Override
        void unbind(M3ViewportReporter r) {
            if (model != null) {
                model.removeTreeModelListener(this);
                model = null;
            }
            tree.removeTreeExpansionListener(this);
        }

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            reporter.schedule(ViewportEvent.MODEL_DELTA);
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            reporter.schedule(ViewportEvent.MODEL_DELTA);
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            reporter.schedule(ViewportEvent.MODEL_DELTA);
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            reporter.schedule(ViewportEvent.MODEL_DELTA);
        }

        @Override
        public void treeExpanded(TreeExpansionEvent e) {
            reporter.schedule(ViewportEvent.EXPAND);
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent e) {
            reporter.schedule(ViewportEvent.EXPAND);
        }
    }
}
