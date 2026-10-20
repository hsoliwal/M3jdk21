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
import java.awt.Container;
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
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.PartVisibilityEvent;
import javax.swing.event.PartVisibilityListener;

/**
 * The tracker behind {@code JComponent.addPartVisibilityListener} (M3JDK, brief rule 25). A component gets one
 * tracker when its first {@link PartVisibilityListener} is added and loses it with the last, so the cost is
 * proportional to the listening components; with no listener nothing is tracked. The tracker keeps the last
 * part-visible state and recomputes it on the triggers that can change it:
 * <ul>
 * <li>{@code HierarchyEvent.SHOWING_CHANGED}: setVisible on the component or an ancestor, a JTabbedPane tab or a
 * CardLayout card selected, the window shown or hidden. Cause {@code VISIBILITY}.</li>
 * <li>{@code PARENT_CHANGED} and {@code DISPLAYABILITY_CHANGED}: cause {@code REPARENT}.</li>
 * <li>A resize of the component ({@code componentResized}) or of an ancestor ({@code ancestorResized}): cause
 * {@code SIZE}.</li>
 * <li>The window iconified or deiconified ({@code WINDOW_STATE_CHANGED}, one dispatcher per window): cause
 * {@code MINIMIZE}.</li>
 * <li>{@code JComponent.setPartHidden} on the component or an ancestor: cause {@code CONTAINER}.</li>
 * </ul>
 * AWT delivers hierarchy events to descendants before ancestors. So the first tracker whose state a change flips
 * rechecks every tracker below the changed component, ancestors first, and the trackers reached later find their
 * state already current. An event goes out only when the state differs from the kept one: a trigger that changes
 * nothing sends nothing, and adding the first listener only records the state. Listeners run on the thread that
 * made the change, the event dispatch thread for code that follows the Swing threading rule.
 */
//FLLDCIM: M3 swing part visibility - JComponent.isPartVisible and its listener, cost O(listening components)
public final class M3PartVisibility extends ComponentAdapter
        implements HierarchyListener, HierarchyBoundsListener {

    /** Guards {@link #TRACKERS}, {@link #WINDOWS} and the dispatchers' lists; never held while a listener runs. */
    private static final Object LOCK = new Object();

    /** Every live tracker, weakly (the component holds its tracker as a listener). */
    private static final List<WeakReference<M3PartVisibility>> TRACKERS = new ArrayList<>();

    /** One window-state dispatcher per window with trackers. */
    private static final WeakHashMap<Window, Dispatcher> WINDOWS = new WeakHashMap<>();

    private static final Comparator<M3PartVisibility> ANCESTORS_FIRST =
            Comparator.comparingInt(t -> depth(t.component));

    /** Sends a window's iconify and deiconify to its trackers, ancestors first. */
    private static final class Dispatcher implements WindowStateListener {
        final List<WeakReference<M3PartVisibility>> trackers = new ArrayList<>();

        @Override
        public void windowStateChanged(WindowEvent e) {
            if (((e.getOldState() ^ e.getNewState()) & Frame.ICONIFIED) == 0) {
                return;
            }
            List<M3PartVisibility> all;
            synchronized (LOCK) {
                all = live(trackers, null);
            }
            for (M3PartVisibility t : all) {
                t.recheck(PartVisibilityEvent.MINIMIZE, e.getWindow());
            }
        }
    }

    private final JComponent component;
    private final WeakReference<M3PartVisibility> self = new WeakReference<>(this);
    private boolean visible;
    private Window window;

    private M3PartVisibility(JComponent component) {
        this.component = component;
    }

    /**
     * {@code JComponent.isPartVisible}: showing, a non-zero size for the component and every ancestor up to its
     * window, no ancestor marked part-hidden, and the window not iconified.
     */
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

    /** {@code c} got a listener: the first one installs the tracker and records the state, without an event. */
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
        synchronized (LOCK) {
            TRACKERS.add(t.self);
        }
    }

    /** {@code c} lost its last listener: the tracker goes. */
    public static void listenerRemoved(JComponent c) {
        M3PartVisibility t = tracker(c);
        if (t == null) {
            return;
        }
        c.removeHierarchyListener(t);
        c.removeHierarchyBoundsListener(t);
        c.removeComponentListener(t);
        t.attachTo(null);
        synchronized (LOCK) {
            TRACKERS.remove(t.self);
        }
    }

    /** {@code c} was marked part-hidden or unmarked: rechecks the trackers at and below it, ancestors first. */
    public static void partHiddenChanged(JComponent c) {
        List<M3PartVisibility> below;
        synchronized (LOCK) {
            below = live(TRACKERS, c);
        }
        for (M3PartVisibility t : below) {
            t.recheck(PartVisibilityEvent.CONTAINER, c);
        }
    }

    /** The number of live trackers, for tests. */
    public static int trackers() {
        synchronized (LOCK) {
            return live(TRACKERS, null).size();
        }
    }

    /** The number of windows with a window-state dispatcher, for tests. */
    public static int windows() {
        synchronized (LOCK) {
            return WINDOWS.size();
        }
    }

    private static M3PartVisibility tracker(JComponent c) {
        for (HierarchyListener l : c.getHierarchyListeners()) {
            if (l instanceof M3PartVisibility t) {
                return t;
            }
        }
        return null;
    }

    /**
     * The live trackers of {@code refs} at or below {@code root} (all of them when null), ancestors first; cleared
     * references are dropped. Callers hold {@link #LOCK}.
     */
    private static List<M3PartVisibility> live(List<WeakReference<M3PartVisibility>> refs, Component root) {
        refs.removeIf(r -> r.get() == null);
        List<M3PartVisibility> out = new ArrayList<>();
        for (WeakReference<M3PartVisibility> r : refs) {
            M3PartVisibility t = r.get();
            if (t != null && (root == null || t.component == root
                    || root instanceof Container k && k.isAncestorOf(t.component))) {
                out.add(t);
            }
        }
        out.sort(ANCESTORS_FIRST);
        return out;
    }

    private static int depth(Component c) {
        int d = 0;
        for (Component p = c.getParent(); p != null; p = p.getParent()) {
            d++;
        }
        return d;
    }

    private void attachWindow() {
        attachTo(SwingUtilities.getWindowAncestor(component));
    }

    /** Moves this tracker to the dispatcher of {@code w} (none when null). */
    private void attachTo(Window w) {
        if (w == window) {
            return;
        }
        Window old = window;
        window = w;
        Dispatcher added = null;
        Dispatcher emptied = null;
        synchronized (LOCK) {
            Dispatcher d = old == null ? null : WINDOWS.get(old);
            if (d != null) {
                d.trackers.removeIf(r -> r.get() == null || r == self);
                if (d.trackers.isEmpty()) {
                    WINDOWS.remove(old);
                    emptied = d;
                }
            }
            if (w != null) {
                d = WINDOWS.get(w);
                if (d == null) {
                    d = new Dispatcher();
                    WINDOWS.put(w, d);
                    added = d;
                }
                d.trackers.add(self);
            }
        }
        if (emptied != null) {
            old.removeWindowStateListener(emptied);
        }
        if (added != null) {
            w.addWindowStateListener(added);
        }
    }

    /** Recomputes the state; sends one event when it changed. */
    private void recheck(int cause, Component origin) {
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

    /** A change at {@code changed}: when it flips this tracker, every tracker at or below it is rechecked. */
    private void changed(int cause, Component changed) {
        if (isPartVisible(component) == visible) {
            return;
        }
        List<M3PartVisibility> below;
        synchronized (LOCK) {
            below = live(TRACKERS, changed);
        }
        if (!below.contains(this)) {
            below.add(this);
        }
        for (M3PartVisibility t : below) {
            t.recheck(cause, changed);
        }
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        long flags = e.getChangeFlags();
        boolean moved = (flags & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED)) != 0;
        if (moved) {
            attachWindow();
        }
        if (moved || (flags & HierarchyEvent.SHOWING_CHANGED) != 0) {
            changed(moved ? PartVisibilityEvent.REPARENT : PartVisibilityEvent.VISIBILITY, e.getChanged());
        }
    }

    @Override
    public void ancestorResized(HierarchyEvent e) {
        changed(PartVisibilityEvent.SIZE, e.getChanged());
    }

    @Override
    public void ancestorMoved(HierarchyEvent e) {
        // a move never changes part visibility
    }

    @Override
    public void componentResized(ComponentEvent e) {
        changed(PartVisibilityEvent.SIZE, component);
    }
}
