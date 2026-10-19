/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 7: a DefaultRowSorter that keeps its order in int lanes answers
 *          every query, fires every event and restores every selection as one that keeps Row objects.
 * @modules java.desktop/javax.swing:open java.desktop/sun.swing.table
 * @run main/othervm -Djava.awt.headless=true -Dm3.swing.viewport=true -XX:-OmitStackTraceInFastThrow M3RowSorterLanesDifferential 40 300
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import sun.swing.table.M3SorterLanes;

/**
 * Seeded scripts of sorter, table and model operations run against two JTables on one
 * DefaultTableModel, each with its own TableRowSorter. The stock side's sorter has its
 * {@code m3Lanes} field cleared by reflection, so it keeps the stock Row objects; the other keeps
 * {@code sun.swing.table.M3SorterLanes}. Steps: sort keys (up to three, any order, UNSORTED
 * included), toggles, filters (none, regex, number), sortsOnUpdates, comparators, maxSortKeys,
 * sortable columns, row inserts, removals, cell edits, update events, full data and structure
 * events, direct sorter calls with invalid ranges, selections. After every step the view row
 * count, the whole permutation both ways, the exceptions for out-of-range indices, the sort keys,
 * the selection and the RowSorterEvents (type, previous count, previous permutation) are compared;
 * every 10 steps the tables are painted and compared pixel by pixel. Every fifth seed uses
 * 2,000-4,000 rows. Usage: {@code M3RowSorterLanesDifferential <seeds> <steps> [firstSeed]}.
 */
public final class M3RowSorterLanesDifferential {

    /** One side: a table, its sorter and the events the sorter fired since the last check. */
    static final class Side {
        final JTable table;
        final TableRowSorter<TableModel> sorter;
        final List<String> events = new ArrayList<>();

        Side(DefaultTableModel model, boolean stock) {
            table = new JTable(model);
            sorter = new TableRowSorter<>(model);
            if (stock) {
                set(sorter, "m3Lanes", null);
            }
            sorter.addRowSorterListener(e -> {
                StringBuilder sb = new StringBuilder(e.getType() + " prev " + e.getPreviousRowCount() + ":");
                for (int i = 0; i < e.getPreviousRowCount(); i++) {
                    sb.append(' ').append(e.convertPreviousRowIndexToModel(i));
                }
                events.add(sb.toString());
            });
            table.setRowSorter(sorter);
            table.setSize(300, 400);
        }
    }

    interface Action {
        void run(Side s) throws Exception;
    }

    static int pixelChecks;
    static int lanesSteps;
    static int checks;

    private final Random rnd;
    private final DefaultTableModel model;
    private final Side stock;
    private final Side m3;
    private final List<String> log = new ArrayList<>();
    private int step;

    M3RowSorterLanesDifferential(long seed) {
        rnd = new Random(seed);
        boolean wide = seed % 5 == 0;
        model = new DefaultTableModel(new Object[] {"s", "n", "b"}, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 0 ? String.class : c == 1 ? Integer.class : Boolean.class;
            }
        };
        int n = wide ? 2000 + rnd.nextInt(2000) : rnd.nextInt(60);
        for (int i = 0; i < n; i++) {
            model.addRow(row());
        }
        stock = new Side(model, true);
        m3 = new Side(model, false);
        if (get(m3.sorter, "m3Lanes") == null || get(stock.sorter, "m3Lanes") != null) {
            throw new IllegalStateException("run with -Dm3.swing.viewport=true on the patched JDK");
        }
        log.add("seed " + seed + " rows " + n);
    }

    Object[] row() {
        String s = rnd.nextInt(8) == 0 ? null : "" + (char) ('a' + rnd.nextInt(6)) + rnd.nextInt(4);
        Integer v = rnd.nextInt(10) == 0 ? null : rnd.nextInt(10);
        return new Object[] {s, v, rnd.nextBoolean()};
    }

    static Object get(Object o, String field) {
        try {
            Field f = javax.swing.DefaultRowSorter.class.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(o);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("needs --add-opens java.desktop/javax.swing=ALL-UNNAMED", e);
        }
    }

    static void set(Object o, String field, Object value) {
        try {
            Field f = javax.swing.DefaultRowSorter.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(o, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("needs --add-opens java.desktop/javax.swing=ALL-UNNAMED", e);
        }
    }

    List<Side> sides() {
        return List.of(stock, m3);
    }

    void act(String what, Action a) {
        log.add(step + " " + what);
        String[] out = new String[2];
        int i = 0;
        for (Side s : sides()) {
            try {
                a.run(s);
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

    List<RowSorter.SortKey> keys() {
        int k = rnd.nextInt(4);
        List<RowSorter.SortKey> keys = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            SortOrder o = rnd.nextInt(6) == 0 ? SortOrder.UNSORTED : rnd.nextBoolean() ? SortOrder.ASCENDING
                    : SortOrder.DESCENDING;
            keys.add(new RowSorter.SortKey(rnd.nextInt(3), o));
        }
        return keys;
    }

    void step() {
        int op = rnd.nextInt(100);
        int size = model.getRowCount();
        if (op < 14) {
            List<RowSorter.SortKey> k = keys();
            act("setSortKeys " + k, s -> s.sorter.setSortKeys(k));
        } else if (op < 22) {
            int c = rnd.nextInt(3);
            act("toggleSortOrder " + c, s -> s.sorter.toggleSortOrder(c));
        } else if (op < 30) {
            int f = rnd.nextInt(4);
            RowFilter<Object, Object> filter = f == 0 ? null : f == 1 ? RowFilter.regexFilter("[a-c]", 0)
                    : f == 2 ? RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 4, 1)
                    : RowFilter.orFilter(List.of(RowFilter.regexFilter("^d"), RowFilter.numberFilter(
                            RowFilter.ComparisonType.EQUAL, 3, 1)));
            act("setRowFilter " + f, s -> s.sorter.setRowFilter(filter));
        } else if (op < 34) {
            boolean v = rnd.nextBoolean();
            act("setSortsOnUpdates " + v, s -> s.sorter.setSortsOnUpdates(v));
        } else if (op < 38) {
            int c = rnd.nextInt(3);
            int kind = rnd.nextInt(3);
            Comparator<Object> cmp = kind == 0 ? null : kind == 1
                    ? Comparator.comparing(o -> String.valueOf(o).length())
                    : Comparator.comparing(o -> String.valueOf(o), Comparator.reverseOrder());
            act("setComparator " + c + " " + kind, s -> s.sorter.setComparator(c, cmp));
        } else if (op < 40) {
            int m = 1 + rnd.nextInt(3);
            act("setMaxSortKeys " + m, s -> s.sorter.setMaxSortKeys(m));
        } else if (op < 42) {
            int c = rnd.nextInt(3);
            boolean v = rnd.nextInt(3) > 0;
            act("setSortable " + c + " " + v, s -> s.sorter.setSortable(c, v));
        } else if (op < 52) {
            int at = rnd.nextInt(size + 1);
            Object[] r = row();
            log.add(step + " insertRow " + at + " " + Arrays.toString(r));
            model.insertRow(at, r);
        } else if (op < 58 && size > 0) {
            int at = rnd.nextInt(size);
            log.add(step + " removeRow " + at);
            model.removeRow(at);
        } else if (op < 66 && size > 0) {
            int r = rnd.nextInt(size);
            int c = rnd.nextInt(3);
            Object v = row()[c];
            log.add(step + " setValueAt " + v + " " + r + " " + c);
            model.setValueAt(v, r, c);
        } else if (op < 70 && size > 0) {
            int a = rnd.nextInt(size);
            int b = Math.min(size - 1, a + rnd.nextInt(wideRange()));
            for (int i = a; i <= b; i++) {
                if (rnd.nextBoolean()) {
                    model.getDataVector().get(i).set(1, rnd.nextInt(10));
                }
            }
            log.add(step + " fireTableRowsUpdated " + a + " " + b);
            model.fireTableRowsUpdated(a, b);
        } else if (op < 72) {
            log.add(step + " fireTableDataChanged");
            model.fireTableDataChanged();
        } else if (op < 73) {
            log.add(step + " fireTableStructureChanged");
            model.fireTableStructureChanged();
        } else if (op < 75) {
            int k = rnd.nextInt(3);
            act("direct " + k, s -> {
                switch (k) {
                    case 0 -> s.sorter.sort();
                    case 1 -> s.sorter.allRowsChanged();
                    default -> s.sorter.modelStructureChanged();
                }
            });
        } else if (op < 78) {
            int k = rnd.nextInt(3);
            int bad = size + 1 + rnd.nextInt(3);
            act("invalid range " + k + " " + bad, s -> {
                switch (k) {
                    case 0 -> s.sorter.rowsInserted(bad, bad);
                    case 1 -> s.sorter.rowsDeleted(bad, bad);
                    default -> s.sorter.rowsUpdated(-1, 0);
                }
            });
        } else if (op < 80) {
            int n = rnd.nextInt(size + 3);
            log.add(step + " setRowCount " + n);
            model.setRowCount(n);
        } else if (op < 90 && m3.table.getRowCount() > 0) {
            int rows = m3.table.getRowCount();
            int a = rnd.nextInt(rows);
            int b = Math.min(rows - 1, a + rnd.nextInt(3));
            boolean add = rnd.nextBoolean();
            act((add ? "addRowSelectionInterval " : "setRowSelectionInterval ") + a + " " + b, s -> {
                if (add) {
                    s.table.addRowSelectionInterval(a, b);
                } else {
                    s.table.setRowSelectionInterval(a, b);
                }
            });
        } else if (op < 92) {
            act("clearSelection", s -> s.table.clearSelection());
        } else {
            act("query only", s -> { });
        }
    }

    int wideRange() {
        return model.getRowCount() > 500 ? 300 : 6;
    }

    void compare(String what, Function<Side, String> f) {
        String a = f.apply(stock);
        String b = f.apply(m3);
        if (!a.equals(b)) {
            fail(what + "\n  stock " + a + "\n  m3    " + b);
        }
    }

    void fail(String msg) {
        throw new AssertionError(msg + "\n" + String.join("\n", log.subList(Math.max(0, log.size() - 12), log.size())));
    }

    static String outcome(Runnable r) {
        try {
            r.run();
            return "ok";
        } catch (RuntimeException e) {
            return e.getClass().getName();
        }
    }

    void check(boolean pixels) {
        compare("view rows", s -> s.sorter.getViewRowCount() + " " + s.sorter.getModelRowCount() + " " + s.table.getRowCount());
        compare("permutation", s -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.sorter.getViewRowCount(); i++) {
                sb.append(s.sorter.convertRowIndexToModel(i)).append(' ');
            }
            sb.append("| ");
            for (int i = 0; i < model.getRowCount(); i++) {
                sb.append(s.sorter.convertRowIndexToView(i)).append(' ');
            }
            return sb.toString();
        });
        compare("out of range", s -> outcome(() -> s.sorter.convertRowIndexToModel(-1)) + " "
                + outcome(() -> s.sorter.convertRowIndexToModel(s.sorter.getViewRowCount())) + " "
                + outcome(() -> s.sorter.convertRowIndexToView(-1)) + " "
                + outcome(() -> s.sorter.convertRowIndexToView(model.getRowCount())));
        compare("keys", s -> s.sorter.getSortKeys() + " " + s.sorter.getSortsOnUpdates() + " " + s.sorter.getMaxSortKeys());
        compare("selection", s -> Arrays.toString(s.table.getSelectedRows()) + " lead "
                + s.table.getSelectionModel().getLeadSelectionIndex() + " anchor "
                + s.table.getSelectionModel().getAnchorSelectionIndex());
        compare("events", s -> {
            String e = s.events.toString();
            s.events.clear();
            return e;
        });
        if (get(m3.sorter, "viewToModel") != null) {
            fail("the M3 sorter created stock Row objects");
        }
        M3SorterLanes lanes = (M3SorterLanes) get(m3.sorter, "m3Lanes");
        if (lanes.transformed()) {
            lanesSteps++;
            if (get(stock.sorter, "viewToModel") == null) {
                fail("stock not transformed while the lanes are");
            }
        }
        checks++;
        if (pixels) {
            int h = Math.max(1, m3.table.getPreferredSize().height);
            int y = rnd.nextInt(h);
            compare("pixels at y " + y, s -> pixels(s.table, y));
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
            } catch (RuntimeException e) {
                fail("exception from a model edit: " + e);
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
                    new M3RowSorterLanesDifferential(seed).run(steps);
                    passed[0]++;
                } catch (AssertionError e) {
                    failures.add("seed " + seed);
                    String msg = String.valueOf(e.getMessage());
                    System.err.println(failures.size() == 1 ? msg
                            : "seed " + seed + ": " + msg.lines().limit(3).reduce((x, y) -> x + " | " + y).orElse(""));
                }
            }
        });
        return "M3RowSorterLanesDifferential seeds " + seeds + " steps " + steps + " passed " + passed[0]
                + " failed " + failures.size() + " " + failures + "; pixel comparisons " + pixelChecks
                + "; checks " + checks + ", with the lanes transformed " + lanesSteps;
    }
}
