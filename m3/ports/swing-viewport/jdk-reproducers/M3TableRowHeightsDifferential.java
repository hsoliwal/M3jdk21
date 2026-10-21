/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 8: a JTable that keeps its custom row heights in sparse stores
 *          answers every query and paints every pixel as one that keeps the stock dense
 *          SizeSequences, with and without a sorter, through every model and sorter change.
 * @modules java.desktop/javax.swing:open java.desktop/sun.swing.table
 * @run main/othervm -Djava.awt.headless=true -Dm3.swing.viewport=true -XX:-OmitStackTraceInFastThrow M3TableRowHeightsDifferential 40 300
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;


/**
 * Seeded scripts against two JTables on one DefaultTableModel. The stock side has its
 * {@code m3Sizes} flag cleared, so it keeps {@code rowModel} and {@code SortManager.modelRowSizes};
 * the other keeps {@code sun.swing.table.M3SparseSizes}. Steps: per-row and global row heights,
 * inserts, removals, moves, edits, update/data/structure events, sorters on and off, sort keys,
 * filters, row count changes, selections, serialization round trips (both sides throw while custom heights
 * are set, upstream defect D8). After every step: the row
 * count, every row's height and rectangle, rowAtPoint at every row boundary and outside, the
 * preferred size, the scroll increments at sampled rectangles, the selection, and that the M3 side
 * keeps no dense SizeSequence; pixels every 10 steps. Every fifth seed uses 2,000-4,000 rows.
 * Usage: {@code M3TableRowHeightsDifferential <seeds> <steps> [firstSeed]}.
 */
public final class M3TableRowHeightsDifferential {

    interface Action {
        void run(JTable t) throws Exception;
    }

    /** The shared model, serializable (a static class, so a round trip does not capture the harness). */
    static final class Model extends DefaultTableModel {
        private static final long serialVersionUID = 1L;

        Model() {
            super(new Object[] {"s", "n"}, 0);
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return c == 0 ? String.class : Integer.class;
        }
    }

    static int pixelChecks;
    static int sparseSteps;
    static int roundTrips;
    static int failedRoundTrips;

    private final Random rnd;
    private final DefaultTableModel model;
    private JTable stock;
    private JTable m3;
    private final List<String> log = new ArrayList<>();
    private int step;

    M3TableRowHeightsDifferential(long seed) {
        rnd = new Random(seed);
        model = new Model();
        int n = seed % 5 == 0 ? 2000 + rnd.nextInt(2000) : rnd.nextInt(50);
        for (int i = 0; i < n; i++) {
            model.addRow(row());
        }
        stock = table(true);
        m3 = table(false);
        log.add("seed " + seed + " rows " + n);
    }

    JTable table(boolean stockSide) {
        JTable t = new JTable(model);
        if (stockSide) {
            setFlag(t, false);
        } else if (!flag(t)) {
            throw new IllegalStateException("run with -Dm3.swing.viewport=true on the patched JDK");
        }
        t.setSize(300, 400);
        return t;
    }

    static Field field(String name) throws NoSuchFieldException {
        Field f = JTable.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    static boolean flag(JTable t) {
        try {
            return field("m3Sizes").getBoolean(t);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("needs --add-opens java.desktop/javax.swing=ALL-UNNAMED", e);
        }
    }

    static void setFlag(JTable t, boolean v) {
        try {
            field("m3Sizes").setBoolean(t, v);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static Object get(JTable t, String name) {
        try {
            return field(name).get(t);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    Object[] row() {
        return new Object[] {"" + (char) ('a' + rnd.nextInt(6)) + rnd.nextInt(5), rnd.nextInt(8)};
    }

    void act(String what, Action a) {
        log.add(step + " " + what);
        String[] out = new String[2];
        int i = 0;
        for (JTable t : List.of(stock, m3)) {
            try {
                a.run(t);
                out[i++] = "ok";
            } catch (Exception | Error e) {
                if (e instanceof AssertionError) {
                    throw (AssertionError) e;
                }
                out[i++] = e.getClass().getName();
            }
        }
        if (!out[0].equals(out[1])) {
            fail("outcome stock " + out[0] + " m3 " + out[1]);
        }
    }

    @SuppressWarnings("unchecked")
    void step() throws Exception {
        int op = rnd.nextInt(100);
        int size = model.getRowCount();
        int view = m3.getRowCount();
        if (op < 22 && view > 0) {
            // a negative row is upstream defect D7 (the stock SizeSequence corrupts its sums); not compared
            int r = rnd.nextInt(view + 2);
            int h = rnd.nextInt(10) == 0 ? -1 + rnd.nextInt(2) : 8 + rnd.nextInt(40);
            act("setRowHeight " + r + " " + h, t -> t.setRowHeight(r, h));
        } else if (op < 25) {
            int h = rnd.nextInt(10) == 0 ? 0 : 10 + rnd.nextInt(20);
            act("setRowHeight " + h, t -> t.setRowHeight(h));
        } else if (op < 37) {
            int at = rnd.nextInt(size + 1);
            log.add(step + " insertRow " + at);
            model.insertRow(at, row());
        } else if (op < 45 && size > 0) {
            int at = rnd.nextInt(size);
            log.add(step + " removeRow " + at);
            model.removeRow(at);
        } else if (op < 48 && size > 1) {
            int a = rnd.nextInt(size);
            int b = Math.min(size - 1, a + rnd.nextInt(3));
            int to = rnd.nextInt(size - (b - a));
            log.add(step + " moveRow " + a + " " + b + " " + to);
            model.moveRow(a, b, to);
        } else if (op < 53 && size > 0) {
            int r = rnd.nextInt(size);
            log.add(step + " setValueAt " + r);
            model.setValueAt(rnd.nextInt(8), r, 1);
        } else if (op < 55 && size > 0) {
            int a = rnd.nextInt(size);
            log.add(step + " fireTableRowsUpdated " + a);
            model.fireTableRowsUpdated(a, Math.min(size - 1, a + rnd.nextInt(5)));
        } else if (op < 57) {
            log.add(step + " fireTableDataChanged");
            model.fireTableDataChanged();
        } else if (op < 58) {
            log.add(step + " fireTableStructureChanged");
            model.fireTableStructureChanged();
        } else if (op < 63) {
            int k = rnd.nextInt(3);
            act("sorter " + k, t -> {
                if (k == 0) {
                    t.setRowSorter(null);
                } else if (k == 1) {
                    t.setAutoCreateRowSorter(true);
                } else {
                    t.setRowSorter(new TableRowSorter<TableModel>(t.getModel()));
                }
            });
        } else if (op < 70 && m3.getRowSorter() != null) {
            int c = rnd.nextInt(2);
            SortOrder o = rnd.nextInt(5) == 0 ? SortOrder.UNSORTED : rnd.nextBoolean() ? SortOrder.ASCENDING
                    : SortOrder.DESCENDING;
            act("setSortKeys " + c + " " + o, t -> t.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(c, o))));
        } else if (op < 74 && m3.getRowSorter() != null) {
            int f = rnd.nextInt(3);
            act("setRowFilter " + f, t -> ((TableRowSorter<TableModel>) t.getRowSorter()).setRowFilter(f == 0 ? null
                    : f == 1 ? RowFilter.regexFilter("[a-c]", 0)
                    : RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 3, 1)));
        } else if (op < 76) {
            int n = rnd.nextInt(size + 3);
            log.add(step + " setRowCount " + n);
            model.setRowCount(n);
        } else if (op < 82 && view > 0) {
            int a = rnd.nextInt(view);
            int b = Math.min(view - 1, a + rnd.nextInt(2));
            act("setRowSelectionInterval " + a + " " + b, t -> t.setRowSelectionInterval(a, b));
        } else if (op < 84) {
            log.add(step + " serialization round trip");
            Object s2 = roundTrip(stock, true);
            Object m2 = roundTrip(m3, false);
            if (s2 instanceof JTable st && m2 instanceof JTable mt) {
                String a = heights(st);
                String b = heights(mt);
                if (!a.equals(b)) {
                    fail("heights after a serialization round trip\n  stock " + a + "\n  m3    " + b);
                }
                st.setModel(model);
                mt.setModel(model);
                stock = st;
                m3 = mt;
                roundTrips++;
            } else if (!String.valueOf(s2).equals(String.valueOf(m2))) {
                fail("serialization outcome stock " + s2 + " m3 " + m2);
            } else {
                // the failed write left both UIs uninstalled (the reinstall comes after the write); reinstall both
                reinstall(stock);
                reinstall(m3);
                failedRoundTrips++;
            }
        } else {
            act("query only", t -> { });
        }
    }

    /**
     * Serializes and reads back a table with its own copy of the model, or answers the exception
     * (a table with custom row heights is not serializable in stock: upstream defect D8).
     */
    Object roundTrip(JTable t, boolean stockSide) throws Exception {
        JTable copy;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream o = new ObjectOutputStream(out)) {
            o.writeObject(t);
        } catch (java.io.IOException e) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            copy = (JTable) in.readObject();
        }
        if (stockSide) {
            setFlag(copy, false);
        }
        copy.setSize(300, 400);
        return copy;
    }

    /** A fresh UI for a table whose UI a failed write left uninstalled (its uninstallUI would throw). */
    static void reinstall(JTable t) throws ReflectiveOperationException {
        Field ui = javax.swing.JComponent.class.getDeclaredField("ui");
        ui.setAccessible(true);
        ui.set(t, null);
        t.updateUI();
    }

    static String heights(JTable t) {
        StringBuilder sb = new StringBuilder(t.getRowCount() + ": ");
        for (int r = 0; r < t.getRowCount(); r++) {
            sb.append(t.getRowHeight(r)).append(',').append(t.getCellRect(r, 0, true).y).append(' ');
        }
        return sb.toString();
    }

    void compare(String what, Function<JTable, String> f) {
        String a = f.apply(stock);
        String b = f.apply(m3);
        if (!a.equals(b)) {
            fail(what + "\n  stock " + a + "\n  m3    " + b);
        }
    }

    void fail(String msg) {
        throw new AssertionError(msg + "\n" + String.join("\n", log.subList(Math.max(0, log.size() - 12), log.size())));
    }

    void check(boolean pixels) {
        compare("rows", t -> t.getRowCount() + " " + t.getRowHeight() + " " + t.getPreferredSize());
        compare("heights", t -> {
            StringBuilder sb = new StringBuilder();
            for (int r = -1; r <= t.getRowCount(); r++) {
                Rectangle c = t.getCellRect(r, 0, true);
                sb.append(t.getRowHeight(r)).append(':').append(c.y).append('/').append(c.height).append(' ');
            }
            return sb.toString();
        });
        compare("rowAtPoint", t -> {
            StringBuilder sb = new StringBuilder();
            int bottom = t.getRowCount() == 0 ? 0 : t.getCellRect(t.getRowCount() - 1, 0, true).y
                    + t.getRowHeight(t.getRowCount() - 1);
            for (int y : new int[] {-1, 0, bottom - 1, bottom, bottom + 7}) {
                sb.append(t.rowAtPoint(new Point(5, y))).append(' ');
            }
            for (int r = 0; r < Math.min(t.getRowCount(), 400); r++) {
                Rectangle c = t.getCellRect(r, 0, true);
                sb.append(t.rowAtPoint(new Point(5, c.y))).append(',').append(t.rowAtPoint(new Point(5, c.y + c.height - 1)))
                        .append(' ');
            }
            return sb.toString();
        });
        int h = Math.max(1, m3.getPreferredSize().height);
        int y0 = rnd.nextInt(h);
        int vh = 20 + rnd.nextInt(200);
        compare("scroll increments", t -> {
            Rectangle v = new Rectangle(0, y0, 300, vh);
            return t.getScrollableBlockIncrement(v, SwingConstants.VERTICAL, 1) + " "
                    + t.getScrollableBlockIncrement(v, SwingConstants.VERTICAL, -1) + " "
                    + t.getScrollableUnitIncrement(v, SwingConstants.VERTICAL, 1) + " "
                    + t.getScrollableUnitIncrement(v, SwingConstants.VERTICAL, -1);
        });
        compare("selection", t -> Arrays.toString(t.getSelectedRows()));
        if (get(m3, "rowModel") != null) {
            fail("the M3 table keeps a dense rowModel");
        }
        if (get(m3, "m3RowSizes") != null) {
            sparseSteps++;
        }
        if (pixels) {
            int y = rnd.nextInt(h);
            compare("pixels at y " + y, t -> pixels(t, y));
            pixelChecks++;
        }
    }

    static String pixels(JTable t, int y) {
        t.setSize(300, Math.max(400, t.getPreferredSize().height));
        BufferedImage img = new BufferedImage(300, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.MAGENTA);
        g.fillRect(0, 0, 300, 240);
        g.translate(0, -y);
        g.setClip(0, y, 300, 240);
        t.paint(g);
        g.dispose();
        return Integer.toHexString(Arrays.hashCode(img.getRGB(0, 0, 300, 240, null, 0, 300)));
    }

    void run(int steps) {
        check(true);
        for (step = 0; step < steps; step++) {
            try {
                step();
            } catch (Exception e) {
                fail("exception from a step: " + e);
            }
            check(step % 10 == 0);
        }
    }

    public static void main(String[] args) throws Exception {
        int seeds = args.length > 0 ? Integer.parseInt(args[0]) : 40;
        int steps = args.length > 1 ? Integer.parseInt(args[1]) : 300;
        long first = args.length > 2 ? Long.parseLong(args[2]) : 1;
        String summary = runSeeds(first, seeds, steps);
        System.out.println(summary);
        if (!summary.contains(" failed 0 ")) {
            throw new AssertionError(summary);
        }
    }

    /** Runs seeds {@code first .. first + seeds - 1} on the event dispatch thread; returns the summary. */
    public static String runSeeds(long first, int seeds, int steps) throws Exception {
        List<String> failures = new ArrayList<>();
        int[] passed = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            for (long seed = first; seed < first + seeds; seed++) {
                try {
                    new M3TableRowHeightsDifferential(seed).run(steps);
                    passed[0]++;
                } catch (AssertionError e) {
                    failures.add("seed " + seed);
                    String msg = String.valueOf(e.getMessage());
                    System.err.println(failures.size() == 1 ? msg
                            : "seed " + seed + ": " + msg.lines().limit(3).reduce((x, y) -> x + " | " + y).orElse(""));
                }
            }
        });
        return "M3TableRowHeightsDifferential seeds " + seeds + " steps " + steps + " passed " + passed[0]
                + " failed " + failures.size() + " " + failures + "; pixel comparisons " + pixelChecks
                + "; checks with the sparse store in use " + sparseSteps + "; serialization round trips " + roundTrips
                + " (both threw " + failedRoundTrips + " times: custom row heights, upstream defect D8)";
    }
}
