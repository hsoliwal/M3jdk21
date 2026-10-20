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

import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import javax.swing.JComponent;
import javax.swing.event.PartVisibilityEvent;
import javax.swing.event.PartVisibilityListener;

/**
 * The tracker behind {@code JComponent.addPartVisibilityListener} (brief rule 25). A component gets one
 * tracker when its first {@link PartVisibilityListener} is added and loses it with the last, so the cost
 * is proportional to the listening components. The tracker keeps the last part-visible state and
 * recomputes it on the triggers that can change it:
 * <ul>
 * <li>{@code HierarchyEvent.SHOWING_CHANGED} (setVisible of the component or an ancestor, a JTabbedPane
 * tab or a CardLayout card selected, the window shown or hidden): cause {@code VISIBILITY};</li>
 * <li>{@code PARENT_CHANGED} and {@code DISPLAYABILITY_CHANGED}: cause {@code REPARENT};</li>
 * <li>a resize of the component ({@code componentResized}) or of an ancestor ({@code ancestorResized}):
 * cause {@code SIZE};</li>
 * <li>the window iconified or deiconified ({@code WindowEvent.WINDOW_STATE_CHANGED}): cause
 * {@code MINIMIZE}, one dispatcher per window, ancestors first;</li>
 * <li>{@code JComponent.setPartHidden} on the component or an ancestor: cause {@code CONTAINER},
 * ancestors first.</li>
 * </ul>
 * An event goes out only when the recomputed state differs from the kept one, so a trigger that
 * changes nothing sends nothing; adding the first listener only records the state.
 */
//FLLDCIM: M3 swing part visibility - JComponent.isPartVisible and its listener, cost O(listening components)
public final class M3PartVisibility extends ComponentAdapter
        implements HierarchyListener, HierarchyBoundsListener {

    /** Every live tracker, weakly (the component holds its tracker as a listener). */
    private static final List<WeakReference<M3PartVisibility>> TRACKERS = new ArrayList<>();

    /** One window-state dispatcher per window with trackers, so iconify reaches them ancestors first. */
    private static final WeakHashMap<Window, Dispatcher> WINDOWS = new WeakHashMap<>();

    /** Dispatches a window's state changes to its trackers, ancestors first. */
    private static final class Dispatcher implements WindowStateListener {
        final List<WeakReference<M3PartVisibility>> trackers = new ArrayList<>();

        @Override
        public void windowStateChanged(WindowEvent e) {
            if (((e.getOldState() ^ e.getNewState()) & Frame.ICONIFIED) != 0) {
                for (M3PartVisibility t : byDepth(trackers)) {
                    t.recheck(PartVisibilityEvent.MINIMIZE, e.getWindow());
                }
            }
        }
    }

    private final JComponent component;
    private boolean visible;
    private Window window;
    private final WeakReference<M3PartVisibility> self = new WeakReference<>(this);

    private M3PartVisibility(JComponent component) {
        this.component = component;
    }

    /** {@code JComponent.isPartVisible}: showing, non-zero size up to the window, no part-hidden ancestor, not iconified. */
    public static boolean isPartVisible(JComponent c) {
        if (!c.isShowing()) {
            return false;
        }
        for (Component p = c; p != null; p = p.getParent()) {
            if (p.getWidth() <= 0 || p.getHeight() <= 0) {
                return false;
            }
            if (p instanceof JComponent j && j.getPartHidden()) {
                return false;
            }
            if (p instanceof Window w) {
                return !(w instanceof Frame f) || (f.getExtendedState() & Frame.ICONIFIED) == 0;
            }
        }
        return true;
    }

    /** Called when {@code c} got a listener: the first one installs the tracker and records the state. */
    public static void listenerAdded(JComponent c) {
        if (tracker(c) != null) {
            return;
        }
        M3PartVisibility t = new M3PartVisibility(c);
        t.visible = isPartVisible(c);
        c.addHierarchyListener(t);
        c.addHierarchyBoundsListener(t);
        c.addComponentListener(t);
        t.attachWindow();
        TRACKERS.add(t.self);
    }

    /** Called when {@code c} lost its last listener: removes the tracker. */
    public static void listenerRemoved(JComponent c) {
        M3PartVisibility t = tracker(c);
        if (t == null) {
            return;
        }
        c.removeHierarchyListener(t);
        c.removeHierarchyBoundsListener(t);
        c.removeComponentListener(t);
        t.detachWindow();
        TRACKERS.remove(t.self);
    }

    /** Called when {@code c} was marked part-hidden or unmarked: rechecks the trackers in its subtree, ancestors first. */
    public static void partHiddenChanged(JComponent c) {
        List<WeakReference<M3PartVisibility>> below = new ArrayList<>();
        for (WeakReference<M3PartVisibility> r : TRACKERS) {
            M3PartVisibility t = r.get();
            if (t != null && (t.component == c || c.isAncestorOf(t.component))) {
                below.add(r);
            }
        }
        for (M3PartVisibility t : byDepth(below)) {
            t.recheck(PartVisibilityEvent.CONTAINER, c);
        }
    }

    /** The number of live trackers (for tests). */
    public static int trackers() {
        TRACKERS.removeIf(r -> r.get() == null);
        return TRACKERS.size();
    }

    private static M3PartVisibility tracker(JComponent c) {
        for (var l : c.getHierarchyListeners()) {
            if (l instanceof M3PartVisibility t) {
                return t;
            }
        }
        return null;
    }

    /** The live trackers of {@code refs} sorted by depth (ancestors first); cleared references are dropped. */
    private static List<M3PartVisibility> byDepth(List<WeakReference<M3PartVisibility>> refs) {
        refs.removeIf(r -> r.get() == null);
        List<M3PartVisibility> live = new ArrayList<>(refs.size());
        for (WeakReference<M3PartVisibility> r : refs) {
            live.add(r.get());
        }
        live.sort((a, b) -> Integer.compare(depth(a.component), depth(b.component)));
        return live;
    }

    private static int depth(Component c) {
        int d = 0;
        for (Component p = c.getParent(); p != null; p = p.getParent()) {
            d++;
        }
        return d;
    }

    private void attachWindow() {
        Window w = javax.swing.SwingUtilities.getWindowAncestor(component);
        if (w == window) {
            return;
        }
        detachWindow();
        window = w;
        if (w != null) {
            Dispatcher d = WINDOWS.get(w);
            if (d == null) {
                d = new Dispatcher();
                WINDOWS.put(w, d);
                w.addWindowStateListener(d);
            }
            d.trackers.add(self);
        }
    }

    private void detachWindow() {
        if (window != null) {
            Dispatcher d = WINDOWS.get(window);
            if (d != null) {
                d.trackers.remove(self);
                d.trackers.removeIf(r -> r.get() == null);
                if (d.trackers.isEmpty()) {
                    window.removeWindowStateListener(d);
                    WINDOWS.remove(window);
                }
            }
            window = null;
        }
    }

    /** Recomputes the state; sends one event when it changed. */
    void recheck(int cause, Component origin) {
        boolean now = isPartVisible(component);
        if (now == visible) {
            return;
        }
        visible = now;
        PartVisibilityEvent e = new PartVisibilityEvent(component, cause, origin);
        for (PartVisibilityListener l : component.getPartVisibilityListeners()) {
            if (now) {
                l.partVisible(e);
            } else {
                l.partHidden(e);
            }
        }
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        long flags = e.getChangeFlags();
        if ((flags & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED)) != 0) {
            attachWindow();
            recheck(PartVisibilityEvent.REPARENT, e.getChanged());
        }
        if ((flags & HierarchyEvent.SHOWING_CHANGED) != 0) {
            recheck(PartVisibilityEvent.VISIBILITY, e.getChanged());
        }
    }

    @Override
    public void ancestorResized(HierarchyEvent e) {
        recheck(PartVisibilityEvent.SIZE, e.getChanged());
    }

    @Override
    public void ancestorMoved(HierarchyEvent e) {
        // a move never changes part visibility
    }

    @Override
    public void componentResized(ComponentEvent e) {
        recheck(PartVisibilityEvent.SIZE, component);
    }
}
