/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 */

/*
 * @test
 * @summary M3JDK brief rule 25: JComponent part visibility, one event per real transition, per trigger
 * @key headful
 * @run main/othervm M3PartVisibilityTest
 */

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Frame;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.PartVisibilityEvent;
import javax.swing.event.PartVisibilityListener;

/**
 * Brief rule 25, one scenario per trigger: setVisible on the component and on an ancestor, JTabbedPane selection,
 * CardLayout, iconify and deiconify, setPartHidden (a collapsible container), size 0 and back for the component
 * and for an ancestor, reparenting, window hide, show and dispose. Every scenario checks the exact list of events
 * (listener, direction, cause, origin, in order: ancestors first), and that a repeated or neutral change sends
 * nothing. The cost checks: no tracker and no AWT listener without a part visibility listener, and the tracker
 * and window dispatcher go with the last listener. Serialization: the stock serialVersionUID is kept and a
 * deserialized listener is tracked again. Headful; results go to the file named by -Dout (one line per check)
 * and the exit status.
 */
public final class M3PartVisibilityTest {

    static final List<String> EVENTS = new ArrayList<>();
    static final List<String> RESULTS = new ArrayList<>();
    static int failures;
    static JFrame frame;

    /** Records "name visible|hidden CAUSE origin". */
    static final class Recorder implements PartVisibilityListener, Serializable {
        private static final long serialVersionUID = 1L;
        final String name;

        Recorder(String name) {
            this.name = name;
        }

        @Override
        public void partVisible(PartVisibilityEvent e) {
            EVENTS.add(name + " visible " + cause(e) + " " + origin(e));
        }

        @Override
        public void partHidden(PartVisibilityEvent e) {
            EVENTS.add(name + " hidden " + cause(e) + " " + origin(e));
        }
    }

    static String cause(PartVisibilityEvent e) {
        return new String[] {"", "VISIBILITY", "SIZE", "MINIMIZE", "REPARENT", "CONTAINER"}[e.getCause()];
    }

    static String origin(PartVisibilityEvent e) {
        Component o = e.getOrigin();
        return o == null ? "null" : o.getName() != null ? o.getName() : o.getClass().getSimpleName();
    }

    static <T extends JComponent> T named(T c, String name) {
        c.setName(name);
        return c;
    }

    static Recorder listen(JComponent c) {
        Recorder r = new Recorder(c.getName());
        c.addPartVisibilityListener(r);
        return r;
    }

    /** Runs {@code r} on the EDT, then waits until the event queue is quiet and no event came for 150 ms. */
    static void act(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
        settle();
    }

    static void settle() throws Exception {
        int seen = -1;
        long quietSince = System.nanoTime();
        long end = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < end) {
            SwingUtilities.invokeAndWait(() -> { });
            java.awt.Toolkit.getDefaultToolkit().sync();
            int now;
            synchronized (EVENTS) {
                now = EVENTS.size();
            }
            if (now != seen) {
                seen = now;
                quietSince = System.nanoTime();
            } else if (System.nanoTime() - quietSince > 150_000_000L) {
                return;
            }
            Thread.sleep(20);
        }
    }

    /** Checks that the events since the last check are exactly {@code expected}, then clears them. */
    static void expect(String check, String... expected) {
        List<String> got;
        synchronized (EVENTS) {
            got = new ArrayList<>(EVENTS);
            EVENTS.clear();
        }
        boolean ok = got.equals(List.of(expected));
        record(check, ok, ok ? got.size() + " events" : "expected " + List.of(expected) + " got " + got);
    }

    static void check(String check, boolean ok, String detail) {
        record(check, ok, detail);
    }

    static void record(String check, boolean ok, String detail) {
        if (!ok) {
            failures++;
        }
        RESULTS.add((ok ? "PASS\t" : "FAIL\t") + check + "\t" + detail);
    }

    public static void main(String[] args) throws Exception {
        try {
            run();
        } catch (Throwable t) {
            record("uncaught", false, t.toString());
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(frame::dispose);
            }
            RESULTS.add("SUMMARY\tM3PartVisibilityTest\t" + (RESULTS.size() - failures) + " passed, " + failures
                    + " failed");
            String out = System.getProperty("out");
            if (out != null) {
                Files.write(Path.of(out), RESULTS, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            }
            RESULTS.forEach(System.out::println);
        }
        System.exit(failures == 0 ? 0 : 1);
    }

    static void run() throws Exception {
        JPanel[] root = new JPanel[1];
        JPanel[] panel = new JPanel[1];
        JLabel[] leaf = new JLabel[1];
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame("M3PartVisibilityTest");
            frame.setName("frame");
            root[0] = named(new JPanel(new BorderLayout()), "root");
            panel[0] = named(new JPanel(new BorderLayout()), "panel");
            leaf[0] = named(new JLabel("leaf"), "leaf");
            panel[0].add(leaf[0]);
            root[0].add(panel[0]);
            frame.getContentPane().add(root[0]);
            frame.setSize(400, 300);
            frame.setLocation(20, 20);
        });
        costWithoutListeners(leaf[0]);
        act(() -> frame.setVisible(true));
        act(() -> {
            listen(panel[0]);
            listen(leaf[0]);
        });
        expect("no event when the listeners are added (showing)");
        check("isPartVisible after show", leaf[0].isPartVisible() && panel[0].isPartVisible(), "both true");

        act(() -> leaf[0].setVisible(false));
        expect("setVisible(false) on the component", "leaf hidden VISIBILITY leaf");
        act(() -> leaf[0].setVisible(false));
        expect("setVisible(false) again sends nothing");
        act(() -> leaf[0].setVisible(true));
        expect("setVisible(true) on the component", "leaf visible VISIBILITY leaf");

        act(() -> root[0].setVisible(false));
        expect("setVisible(false) on an ancestor, ancestors first", "panel hidden VISIBILITY root",
                "leaf hidden VISIBILITY root");
        act(() -> root[0].setVisible(true));
        expect("setVisible(true) on an ancestor, ancestors first", "panel visible VISIBILITY root",
                "leaf visible VISIBILITY root");

        act(() -> panel[0].setPartHidden(true));
        expect("setPartHidden(true) on a container", "panel hidden CONTAINER panel", "leaf hidden CONTAINER panel");
        check("isPartVisible under a part-hidden container", !leaf[0].isPartVisible(), "false");
        act(() -> panel[0].setPartHidden(true));
        expect("setPartHidden(true) again sends nothing");
        act(() -> root[0].setVisible(false));
        expect("setVisible(false) above a part-hidden container sends nothing");
        act(() -> root[0].setVisible(true));
        expect("setVisible(true) above a part-hidden container sends nothing");
        act(() -> panel[0].setPartHidden(false));
        expect("setPartHidden(false)", "panel visible CONTAINER panel", "leaf visible CONTAINER panel");

        sizeScenarios(root[0], panel[0], leaf[0]);
        tabbedPane();
        cardLayout();
        reparent(panel[0], leaf[0]);
        iconify();
        windowHideShow();
        serialization();
        removal(panel[0], leaf[0]);
    }

    /** No listener: no tracker, no hierarchy, bounds or component listener added by the API. */
    static void costWithoutListeners(JComponent c) {
        check("no tracker without a listener", sun.swing.M3PartVisibility.trackers() == 0,
                "trackers " + sun.swing.M3PartVisibility.trackers());
        check("no AWT listener without a part visibility listener", c.getHierarchyListeners().length == 0
                && c.getHierarchyBoundsListeners().length == 0 && c.getComponentListeners().length == 0, "none");
        long uid = ObjectStreamClass.lookup(JComponent.class).getSerialVersionUID();
        check("JComponent serialVersionUID is the stock JDK 21 value", uid == 4588530037560142483L, "uid " + uid);
    }

    static void sizeScenarios(JPanel root, JPanel panel, JLabel leaf) throws Exception {
        JPanel[] holder = new JPanel[1];
        JLabel[] box = new JLabel[1];
        act(() -> {
            holder[0] = named(new JPanel(null), "holder");
            box[0] = named(new JLabel("box"), "box");
            box[0].setBounds(10, 10, 100, 40);
            holder[0].add(box[0]);
            root.add(holder[0], BorderLayout.SOUTH);
            holder[0].setPreferredSize(new java.awt.Dimension(300, 80));
            root.revalidate();
        });
        act(() -> listen(box[0]));
        expect("no event when added (box showing)");
        act(() -> box[0].setSize(0, 40));
        expect("size 0 of the component", "box hidden SIZE box");
        act(() -> box[0].setSize(0, 0));
        expect("still size 0 sends nothing");
        act(() -> box[0].setSize(100, 40));
        expect("non-zero size of the component", "box visible SIZE box");
        act(() -> holder[0].setSize(300, 0));
        expect("size 0 of an ancestor", "box hidden SIZE holder");
        act(() -> holder[0].setSize(300, 80));
        expect("non-zero size of an ancestor", "box visible SIZE holder");
        act(() -> {
            root.remove(holder[0]);
            root.revalidate();
        });
        expect("removed from a showing container", "box hidden REPARENT box");
    }

    static void tabbedPane() throws Exception {
        JTabbedPane[] tabs = new JTabbedPane[1];
        JPanel[] second = new JPanel[1];
        JLabel[] inner = new JLabel[1];
        JFrame[] f = new JFrame[1];
        act(() -> {
            f[0] = new JFrame("tabs");
            f[0].setName("tabsFrame");
            tabs[0] = new JTabbedPane();
            tabs[0].addTab("one", named(new JPanel(), "first"));
            second[0] = named(new JPanel(new BorderLayout()), "second");
            inner[0] = named(new JLabel("inner"), "inner");
            second[0].add(inner[0]);
            tabs[0].addTab("two", second[0]);
            f[0].getContentPane().add(tabs[0]);
            f[0].setSize(300, 200);
            f[0].setVisible(true);
            listen(second[0]);
            listen(inner[0]);
        });
        expect("no event when added (hidden tab)");
        check("hidden tab is not part-visible", !inner[0].isPartVisible(), "false");
        act(() -> tabs[0].setSelectedIndex(1));
        expect("JTabbedPane selects the tab", "second visible VISIBILITY second", "inner visible VISIBILITY second");
        act(() -> tabs[0].setSelectedIndex(1));
        expect("selecting the same tab sends nothing");
        act(() -> tabs[0].setSelectedIndex(0));
        expect("JTabbedPane selects another tab", "second hidden VISIBILITY second", "inner hidden VISIBILITY second");
        act(() -> f[0].dispose());
        expect("dispose with the tab hidden sends nothing");
    }

    static void cardLayout() throws Exception {
        JPanel[] deck = new JPanel[1];
        JPanel[] a = new JPanel[1];
        JPanel[] b = new JPanel[1];
        JFrame[] f = new JFrame[1];
        act(() -> {
            f[0] = new JFrame("cards");
            f[0].setName("cardsFrame");
            deck[0] = new JPanel(new CardLayout());
            a[0] = named(new JPanel(), "cardA");
            b[0] = named(new JPanel(), "cardB");
            deck[0].add(a[0], "a");
            deck[0].add(b[0], "b");
            f[0].getContentPane().add(deck[0]);
            f[0].setSize(300, 200);
            f[0].setVisible(true);
            listen(a[0]);
            listen(b[0]);
        });
        expect("no event when added (cards)");
        act(() -> ((CardLayout) deck[0].getLayout()).show(deck[0], "b"));
        expect("CardLayout shows card b", "cardA hidden VISIBILITY cardA", "cardB visible VISIBILITY cardB");
        act(() -> ((CardLayout) deck[0].getLayout()).show(deck[0], "a"));
        expect("CardLayout shows card a", "cardB hidden VISIBILITY cardB", "cardA visible VISIBILITY cardA");
        act(() -> f[0].dispose());
        expect("dispose of the cards window", "cardA hidden VISIBILITY cardsFrame");
    }

    static void reparent(JPanel panel, JLabel leaf) throws Exception {
        JPanel[] other = new JPanel[1];
        JFrame[] f = new JFrame[1];
        act(() -> {
            f[0] = new JFrame("other");
            f[0].setName("otherFrame");
            other[0] = named(new JPanel(new BorderLayout()), "other");
            f[0].getContentPane().add(other[0]);
            f[0].setSize(200, 150);
            f[0].setLocation(450, 20);
            f[0].setVisible(true);
        });
        act(() -> {
            panel.remove(leaf);
            panel.revalidate();
        });
        expect("removed from its parent", "leaf hidden REPARENT leaf");
        act(() -> {
            other[0].add(leaf);
            other[0].revalidate();
        });
        expect("added to a showing parent in another window", "leaf visible REPARENT leaf");
        act(() -> f[0].setExtendedState(Frame.ICONIFIED));
        expect("iconify of the new window reaches the moved component", "leaf hidden MINIMIZE otherFrame");
        act(() -> f[0].setExtendedState(Frame.NORMAL));
        expect("deiconify of the new window", "leaf visible MINIMIZE otherFrame");
        act(() -> {
            other[0].remove(leaf);
            panel.add(leaf);
            panel.revalidate();
            f[0].dispose();
        });
        expect("moved back", "leaf hidden REPARENT leaf", "leaf visible REPARENT leaf");
        check("the window the component left has no dispatcher left", !hasDispatcher(f[0]), "none");
        check("the window it joined has one", hasDispatcher(frame), "one");
    }

    static void iconify() throws Exception {
        act(() -> frame.setExtendedState(Frame.ICONIFIED));
        expect("iconify, ancestors first", "panel hidden MINIMIZE frame", "leaf hidden MINIMIZE frame");
        act(() -> frame.setExtendedState(Frame.NORMAL));
        expect("deiconify, ancestors first", "panel visible MINIMIZE frame", "leaf visible MINIMIZE frame");
    }

    static void windowHideShow() throws Exception {
        act(() -> frame.setVisible(false));
        expect("window hidden", "panel hidden VISIBILITY frame", "leaf hidden VISIBILITY frame");
        act(() -> frame.setVisible(true));
        expect("window shown", "panel visible VISIBILITY frame", "leaf visible VISIBILITY frame");
    }

    static void serialization() throws Exception {
        JPanel[] copy = new JPanel[1];
        int before = sun.swing.M3PartVisibility.trackers();
        act(() -> {
            try {
                JPanel p = named(new JPanel(), "ser");
                listen(p);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (ObjectOutputStream o = new ObjectOutputStream(bytes)) {
                    o.writeObject(p);
                }
                try (ObjectInputStream i = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                    copy[0] = (JPanel) i.readObject();
                }
                p.removePartVisibilityListener(p.getPartVisibilityListeners()[0]);
                frame.getContentPane().add(copy[0], BorderLayout.NORTH);
                copy[0].setPreferredSize(new java.awt.Dimension(100, 30));
                frame.getContentPane().revalidate();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        check("a deserialized listener is tracked again", copy[0].getPartVisibilityListeners().length == 1
                && sun.swing.M3PartVisibility.trackers() == before + 1, "trackers " + sun.swing.M3PartVisibility.trackers());
        expect("the deserialized component added to the showing window, visible once laid out", "ser visible SIZE ser");
        act(() -> {
            frame.getContentPane().remove(copy[0]);
            frame.getContentPane().revalidate();
        });
        expect("and removed", "ser hidden REPARENT ser");
    }

    static void removal(JPanel panel, JLabel leaf) throws Exception {
        act(() -> {
            for (PartVisibilityListener l : panel.getPartVisibilityListeners()) {
                panel.removePartVisibilityListener(l);
            }
            for (PartVisibilityListener l : leaf.getPartVisibilityListeners()) {
                leaf.removePartVisibilityListener(l);
            }
        });
        act(() -> frame.setVisible(false));
        expect("no event after the listeners are removed");
        check("the trackers go with the last listener", panel.getHierarchyListeners().length == 0
                && leaf.getHierarchyListeners().length == 0, "hierarchy listeners 0");
        check("no window dispatcher left for the main window", !hasDispatcher(frame), "none");
    }

    static boolean hasDispatcher(java.awt.Window w) {
        for (java.awt.event.WindowStateListener l : w.getWindowStateListeners()) {
            if (l.getClass().getName().startsWith("sun.swing.M3PartVisibility")) {
                return true;
            }
        }
        return false;
    }
}
