/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
// Template of gen_atom_tests.py / gen_atom_perf.py: the generated copy replaces this header.

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandle;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractListModel;
import javax.swing.CellRendererPane;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.plaf.basic.BasicListUI;
import javax.swing.plaf.metal.MetalLabelUI;

import sun.swing.list.M3ListMeasure;
import sun.swing.list.M3ListRows;

/**
 * Pattern LIFECYCLE / MODEL_EVENT: {@code M3ListMeasure} on a {@code JList} over a {@code DefaultListModel} against
 * the stock {@code BasicListUI.updateLayoutState} of a second {@code JList} on the same model (a {@code BasicListUI}
 * subclass of the unnamed module, so the stock loop measures every row). The model's list data events are forwarded
 * to {@code added}, {@code removed} and {@code changed} as {@code BasicListUI}'s handler does. After every layout the
 * row heights and the cell width must be the stock ones, and the number of rows measured must be the incremental
 * count the Javadoc promises (the new rows plus one probe) whenever only trackable events of stable values happened,
 * else every row.
 */
final class MeasureScript {

    static final int MAX_ROWS = 24;
    static final int STEPS = 90;
    static final int MAX_RUN = 3;
    static final int MAX_TEXT = 12;
    static final int[] FONT_SIZES = {11, 12, 14, 17};
    static final int ICON_SIZE = 9;
    static final int GAP = 7;
    static final int FIXED_WIDTH = 55;
    static final int FIXED_HEIGHT = 21;
    static final int CAP_ROWS = 1400;
    static final int CAP_STRIDE = 2;
    static final int DELTA_ROWS = 400;
    /** {@code M3ListMeasure.MAX_PIECES}: more pieces make the next layout measure every row. */
    static final int MAX_PIECES = 1024;
    static final String HTML = "<html>two<br>lines</html>";

    enum Mix {
        EVENTS(10, 0, 0), INSERTS(10, 0, 0), REMOVALS(10, 0, 0), CHANGES(10, 0, 0), INPUTS(6, 4, 0), VALUES(8, 0, 2),
        MIXED(7, 2, 1);

        final int events;
        final int inputs;
        final int unstable;

        Mix(int events, int inputs, int unstable) {
            this.events = events;
            this.inputs = inputs;
            this.unstable = unstable;
        }
    }

    /** The stock measuring loop, reachable through the protected {@code updateLayoutState}. */
    static final class StockUI extends BasicListUI {
        int[] heights() {
            updateLayoutState();
            return cellHeights == null ? new int[0] : cellHeights.clone();
        }

        int width() {
            return cellWidth;
        }
    }

    final DefaultListModel<Object> model = new DefaultListModel<>();
    final JList<Object> list = new JList<>(model);
    final JList<Object> stockList = new JList<>(model);
    final StockUI stockUI = new StockUI();
    final M3ListMeasure measure = new M3ListMeasure();
    final CellRendererPane pane = new CellRendererPane();
    final Random rnd;
    /** Per row: whether it was added or changed since the last layout (the NEW pieces). */
    final List<Boolean> fresh = new ArrayList<>();
    boolean usable;
    boolean stableAtLayout;
    int fullMeasures;
    /** The measuring inputs at the last layout, compared by identity as the Javadoc's "any change" means. */
    Object[] inputsAtLayout = new Object[0];

    MeasureScript(String atom) {
        rnd = AtomSupport.random(atom);
        stockList.setUI(stockUI);
        list.setUI(new BasicListUI());
        model.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                measure.added(e.getIndex0(), e.getIndex1());
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    fresh.add(i, true);
                }
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                measure.removed(e.getIndex0(), e.getIndex1());
                fresh.subList(e.getIndex0(), e.getIndex1() + 1).clear();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                measure.changed(e.getIndex0(), e.getIndex1());
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    fresh.set(i, true);
                }
            }
        });
    }

    static boolean stable(Object v) {
        return v == null || v instanceof String || v instanceof Boolean || v instanceof Character || v instanceof Byte
                || v instanceof Short || v instanceof Integer || v instanceof Long || v instanceof Float
                || v instanceof Double;
    }

    Object value(boolean allowUnstable) {
        int kind = rnd.nextInt(allowUnstable ? 9 : 7);
        switch (kind) {
            case 0:
                return "v" + "w".repeat(rnd.nextInt(MAX_TEXT));
            case 1:
                return HTML;
            case 2:
                return rnd.nextInt();
            case 3:
                return rnd.nextBoolean() ? "" : null;
            case 4:
                return (char) ('a' + rnd.nextInt(MAX_TEXT));
            case 5:
                return rnd.nextDouble();
            case 6:
                return "x".repeat(1 + rnd.nextInt(MAX_TEXT));
            case 7:
                return new StringBuilder("sb").append("z".repeat(rnd.nextInt(MAX_TEXT)));
            default:
                return new ImageIcon(new BufferedImage(1 + rnd.nextInt(ICON_SIZE), 1 + rnd.nextInt(ICON_SIZE),
                        BufferedImage.TYPE_INT_ARGB));
        }
    }

    void fill(int n, boolean allowUnstable) {
        for (int i = 0; i < n; i++) {
            model.addElement(value(allowUnstable));
        }
    }

    /** One layout of both lists; returns the rows M3 measured. */
    int layout() {
        int n = model.getSize();
        int newRows = (int) fresh.stream().filter(b -> b).count();
        boolean took = measure.measure(list.getUI(), list, pane);
        int[] stockHeights = stockUI.heights();
        boolean variable = list.getFixedCellHeight() == -1 && list.isEnabled();
        assertEquals(variable && M3ListRows.ENABLED, took, "measure takes the M3 path exactly when its gates hold");
        if (!took) {
            usable = false;
            return -1;
        }
        assertArrayEquals(stockHeights, measure.cellHeights(), "row heights of the stock loop");
        assertEquals(stockUI.width(), measure.cellWidth(), "cell width of the stock loop");
        boolean keptRows = newRows < n;
        boolean newStable = true;
        for (int i = 0; i < n; i++) {
            newStable &= !fresh.get(i) || stable(model.getElementAt(i));
        }
        boolean sameInputs = sameAsLayout(inputs());
        int expected = usable && sameInputs && stableAtLayout && keptRows && newStable ? newRows + 1 : n;
        assertEquals(expected, measure.remeasured(), "rows measured: new rows plus one probe, or every row");
        if (expected == n) {
            fullMeasures++;
        }
        boolean allStable = true;
        for (int i = 0; i < n; i++) {
            allStable &= stable(model.getElementAt(i));
            fresh.set(i, false);
        }
        stableAtLayout = expected == n ? allStable : stableAtLayout;
        usable = true;
        inputsAtLayout = inputs();
        return measure.remeasured();
    }

    /** What sets a row's size besides its value: model, renderer, font, orientation and the renderer properties. */
    Object[] inputs() {
        DefaultListCellRenderer r = (DefaultListCellRenderer) list.getCellRenderer();
        return new Object[] {list.getModel(), r, list.getFont(), list.getComponentOrientation(),
            r.getClientProperty("html.disable"), r.getUI(), r.getIconTextGap(), r.getHorizontalTextPosition(),
            r.getVerticalTextPosition(), r.getHorizontalAlignment(), r.getVerticalAlignment(),
            r.isPreferredSizeSet() ? r.getPreferredSize() : null};
    }

    boolean sameAsLayout(Object[] now) {
        if (now.length != inputsAtLayout.length) {
            return false;
        }
        for (int i = 0; i < now.length; i++) {
            Object a = now[i];
            Object b = inputsAtLayout[i];
            boolean same = a instanceof Integer || a instanceof Dimension ? java.util.Objects.equals(a, b) : a == b;
            if (!same) {
                return false;
            }
        }
        return true;
    }

    void event(Mix mix) {
        int n = model.getSize();
        boolean unstableOk = mix.unstable > 0;
        int kind = mix == Mix.INSERTS ? 0 : mix == Mix.REMOVALS ? 1 : mix == Mix.CHANGES ? 2 : rnd.nextInt(5);
        if (n == 0 || kind == 0) {
            model.add(rnd.nextInt(n + 1), value(unstableOk));
        } else if (kind == 1) {
            int from = rnd.nextInt(n);
            int to = Math.min(n - 1, from + rnd.nextInt(MAX_RUN));
            if (to > from) {
                model.removeRange(from, to);
            } else {
                model.remove(from);
            }
        } else if (kind == 2) {
            model.set(rnd.nextInt(n), value(unstableOk));
        } else if (kind == 3) {
            List<Object> batch = new ArrayList<>();
            for (int k = 1 + rnd.nextInt(MAX_RUN); k > 0; k--) {
                batch.add(value(unstableOk));
            }
            model.addAll(rnd.nextInt(n + 1), batch);
        } else {
            model.addElement(value(unstableOk));
        }
    }

    /**
     * A change of the list or renderer setup. When it changes a measuring input ({@link #inputs}) the next layout
     * measures every row; a fixed cell width is not one, and two toggles between layouts cancel out.
     */
    void input() {
        DefaultListCellRenderer m3r = (DefaultListCellRenderer) list.getCellRenderer();
        DefaultListCellRenderer sr = (DefaultListCellRenderer) stockList.getCellRenderer();
        switch (rnd.nextInt(7)) {
            case 0 -> {
                Font f = new Font(Font.DIALOG, Font.PLAIN, FONT_SIZES[rnd.nextInt(FONT_SIZES.length)]);
                list.setFont(f);
                stockList.setFont(f);
            }
            case 1 -> {
                list.setCellRenderer(new DefaultListCellRenderer());
                stockList.setCellRenderer(new DefaultListCellRenderer());
            }
            case 2 -> {
                m3r.setIconTextGap(m3r.getIconTextGap() == GAP ? GAP + 1 : GAP);
                sr.setIconTextGap(m3r.getIconTextGap());
            }
            case 3 -> {
                Object v = Boolean.TRUE.equals(m3r.getClientProperty("html.disable")) ? null : Boolean.TRUE;
                m3r.putClientProperty("html.disable", v);
                sr.putClientProperty("html.disable", v);
            }
            case 4 -> {
                ComponentOrientation o = list.getComponentOrientation().isLeftToRight()
                        ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT;
                list.setComponentOrientation(o);
                stockList.setComponentOrientation(o);
            }
            case 5 -> {
                int w = list.getFixedCellWidth() == -1 ? FIXED_WIDTH : -1;
                list.setFixedCellWidth(w);
                stockList.setFixedCellWidth(w);
            }
            default -> {
                int h = list.getFixedCellHeight() == -1 ? FIXED_HEIGHT : -1;
                list.setFixedCellHeight(h);
                stockList.setFixedCellHeight(h);
            }
        }
    }

    void fontChange() {
        Font f = list.getFont();
        Font next = new Font(Font.DIALOG, Font.PLAIN, f.getSize() == FONT_SIZES[0] ? FONT_SIZES[1] : FONT_SIZES[0]);
        list.setFont(next);
        stockList.setFont(next);
    }

    void run(Mix mix, int steps) {
        fill(1 + rnd.nextInt(MAX_ROWS), mix.unstable > 0);
        layout();
        int total = mix.events + mix.inputs + mix.unstable;
        for (int step = 0; step < steps; step++) {
            int op = rnd.nextInt(total);
            if (op < mix.events + mix.unstable) {
                event(mix);
            } else {
                input();
            }
            if (rnd.nextBoolean()) {
                layout();
            }
        }
        layout();
    }

    // ---- scenarios of single atoms ---------------------------------------------------------------------

    /** Large list, a few edits: the delta layout measures the new rows and one probe, not every row. */
    void delta() {
        fill(DELTA_ROWS, false);
        assertEquals(DELTA_ROWS, layout(), "the first layout measures every row");
        model.add(DELTA_ROWS / 2, "inserted");
        model.set(1, "changed");
        assertEquals(3, layout(), "two new rows plus the probe");
        model.remove(0);
        assertEquals(1, layout(), "a removal leaves only the probe to measure");
    }

    /** Untrackable indices: {@code changed(-1, -1)} and out-of-range removals make the next layout full. */
    void untrackable() {
        fill(MAX_ROWS, false);
        layout();
        int before = measure.fullMeasuresOnDeltas();
        measure.changed(-1, -1);
        usable = false;
        assertEquals(before + 1, measure.fullMeasuresOnDeltas(), "an index below 0 means every row");
        assertEquals(model.getSize(), layout());
        measure.removed(0, model.getSize());
        usable = false;
        assertEquals(before + 2, measure.fullMeasuresOnDeltas(), "a removal beyond the rows cannot be tracked");
        measure.removed(0, model.getSize());
        assertEquals(before + 2, measure.fullMeasuresOnDeltas(), "already unusable: not counted twice");
        assertEquals(model.getSize(), layout());
        measure.added(model.getSize() + 1, model.getSize() + 1);
        usable = false;
        assertEquals(before + 3, measure.fullMeasuresOnDeltas(), "an insertion beyond the end cannot be tracked");
        layout();
    }

    /** More than MAX_PIECES pieces: the next layout measures every row and counts one full measure. */
    void pieceCap() {
        fill(CAP_ROWS, false);
        layout();
        int before = measure.fullMeasuresOnDeltas();
        for (int i = 0; i < CAP_ROWS; i += CAP_STRIDE) {
            model.set(i, "c" + i);
        }
        assertEquals(before + 1, measure.fullMeasuresOnDeltas(), "the piece cap ends the incremental path once");
        usable = false;
        assertEquals(CAP_ROWS, layout());
    }

    /** {@code same}: a probe that measures differently (a value changed without an event) forces a full layout. */
    static void probeMismatch(String atom) {
        for (boolean atEnd : new boolean[] {false, true}) {
            SilentModel m = new SilentModel();
            JList<Object> l = new JList<>(m);
            JList<Object> s = new JList<>(m);
            StockUI ui = new StockUI();
            s.setUI(ui);
            l.setUI(new BasicListUI());
            M3ListMeasure lm = new M3ListMeasure();
            m.addListDataListener(forward(lm));
            for (int i = 0; i < MAX_ROWS; i++) {
                m.add(i, "row" + i);
            }
            CellRendererPane p = new CellRendererPane();
            assertTrue(lm.measure(l.getUI(), l, p));
            m.setSilently(MAX_ROWS - 1, "a much longer text than before: " + atom);
            m.add(atEnd ? MAX_ROWS : 0, "new");
            int before = lm.fullMeasuresOnDeltas();
            assertTrue(lm.measure(l.getUI(), l, p));
            assertEquals(before + 1, lm.fullMeasuresOnDeltas(), "the probe mismatch is counted");
            assertEquals(m.getSize(), lm.remeasured(), "and every row is measured");
            assertArrayEquals(ui.heights(), lm.cellHeights());
            assertEquals(ui.width(), lm.cellWidth(), "the silent change reaches the width (probe at the end: " + atEnd + ")");
        }
    }

    static ListDataListener forward(M3ListMeasure lm) {
        return new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                lm.added(e.getIndex0(), e.getIndex1());
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                lm.removed(e.getIndex0(), e.getIndex1());
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                lm.changed(e.getIndex0(), e.getIndex1());
            }
        };
    }

    /** A list model that can change a value without telling its listeners (a contract breach the probe detects). */
    static final class SilentModel extends AbstractListModel<Object> {
        private final List<Object> rows = new ArrayList<>();

        @Override
        public int getSize() {
            return rows.size();
        }

        @Override
        public Object getElementAt(int i) {
            return rows.get(i);
        }

        void add(int i, Object v) {
            rows.add(i, v);
            fireIntervalAdded(this, i, i);
        }

        void setSilently(int i, Object v) {
            rows.set(i, v);
        }
    }

    // ---- white-box checks of private atoms ---------------------------------------------------------------

    static void isStableValueContract() {
        MethodHandle h = AtomSupport.statik(M3ListMeasure.class, "isStableValue", boolean.class, Object.class);
        Object[] stableValues = {null, "s", Boolean.TRUE, 'c', (byte) 1, (short) 2, 3, 4L, 5f, 6d};
        Object[] unstableValues = {new StringBuilder("sb"), new Object(), BigInteger.ONE, new AtomicInteger(),
            new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)), new String[] {"a"}};
        for (Object v : stableValues) {
            assertTrue((boolean) AtomSupport.call(h, v), "stable: " + v);
        }
        for (Object v : unstableValues) {
            assertFalse((boolean) AtomSupport.call(h, v), "text or size may change without an event: " + v);
        }
    }

    static void supportsIncrementalSizingContract() {
        MethodHandle h = AtomSupport.statik(M3ListMeasure.class, "supportsIncrementalSizing", boolean.class,
                DefaultListCellRenderer.class);
        DefaultListCellRenderer r = new DefaultListCellRenderer();
        r.setUI(new BasicLabelUI());
        r.setBorder(null);
        assertTrue((boolean) AtomSupport.call(h, r), "basic delegate, no border");
        r.setUI(new MetalLabelUI());
        r.setBorder(new EmptyBorder(1, 1, 1, 1));
        assertTrue((boolean) AtomSupport.call(h, r), "Metal delegate, empty border");
        r.setBorder(new BorderUIResource.EmptyBorderUIResource(1, 1, 1, 1));
        assertTrue((boolean) AtomSupport.call(h, r), "empty border resource");
        r.setBorder(new LineBorder(Color.BLACK));
        assertFalse((boolean) AtomSupport.call(h, r), "a line border may size from elsewhere");
        r.setBorder(null);
        r.setUI(new BasicLabelUI() {
        });
        assertFalse((boolean) AtomSupport.call(h, r), "an application delegate");
        r.setUI(null);
        assertFalse((boolean) AtomSupport.call(h, r), "no delegate");
    }

    void sameInputsContract() {
        MethodHandle h = AtomSupport.virtual(M3ListMeasure.class, "sameInputs", boolean.class, JList.class,
                DefaultListCellRenderer.class);
        fill(MAX_ROWS, false);
        layout();
        DefaultListCellRenderer r = (DefaultListCellRenderer) list.getCellRenderer();
        assertTrue((boolean) AtomSupport.call(h, measure, list, r), "nothing changed since the layout");
        list.setFont(new Font(Font.DIALOG, Font.BOLD, FONT_SIZES[FONT_SIZES.length - 1]));
        stockList.setFont(list.getFont());
        assertFalse((boolean) AtomSupport.call(h, measure, list, r), "a new font");
        layout();
        assertTrue((boolean) AtomSupport.call(h, measure, list, (DefaultListCellRenderer) list.getCellRenderer()));
        r = (DefaultListCellRenderer) list.getCellRenderer();
        r.setHorizontalTextPosition(DefaultListCellRenderer.LEADING);
        assertFalse((boolean) AtomSupport.call(h, measure, list, r), "a new text position");
        assertFalse((boolean) AtomSupport.call(h, measure, list, new DefaultListCellRenderer()), "another renderer");
    }

    void trackContract() {
        MethodHandle h = AtomSupport.virtual(M3ListMeasure.class, "track", boolean.class, boolean.class);
        M3ListMeasure fresh = new M3ListMeasure();
        assertFalse((boolean) AtomSupport.call(h, fresh, true), "nothing kept yet");
        assertEquals(0, fresh.fullMeasuresOnDeltas(), "and nothing counted");
        fill(MAX_ROWS, false);
        layout();
        assertTrue((boolean) AtomSupport.call(h, measure, true), "kept sizes stay usable while ok");
        assertFalse((boolean) AtomSupport.call(h, measure, false));
        assertEquals(1, measure.fullMeasuresOnDeltas(), "the event that made them unusable is counted");
        assertFalse((boolean) AtomSupport.call(h, measure, false));
        assertEquals(1, measure.fullMeasuresOnDeltas(), "once");
    }

    void sizeContract() {
        MethodHandle h = AtomSupport.virtual(M3ListMeasure.class, "size", Dimension.class, JList.class, ListModel.class,
                DefaultListCellRenderer.class, CellRendererPane.class, int.class, boolean.class);
        fill(MAX_ROWS, true);
        model.add(0, new StringBuilder("unstable"));
        DefaultListCellRenderer r = (DefaultListCellRenderer) list.getCellRenderer();
        ListCellRenderer<Object> sr = stockList.getCellRenderer();
        for (int row = 0; row < model.getSize(); row++) {
            Object v = model.getElementAt(row);
            Dimension full = (Dimension) AtomSupport.call(h, measure, list, model, r, pane, row, false);
            Dimension expected = sr.getListCellRendererComponent(stockList, v, row, false, false).getPreferredSize();
            assertEquals(expected, full, "row " + row + " measured as the stock loop does");
            Dimension incremental = (Dimension) AtomSupport.call(h, measure, list, model, r, pane, row, true);
            if (stable(v)) {
                assertEquals(expected, incremental);
            } else {
                assertNull(incremental, "an unknown value is declined before the renderer runs");
            }
        }
    }

    void cellHeightsContract() {
        fill(MAX_ROWS, false);
        layout();
        int[] first = measure.cellHeights();
        int[] copy = first.clone();
        model.add(0, HTML);
        layout();
        assertNotSame(first, measure.cellHeights(), "each layout hands out a new array");
        assertArrayEquals(copy, first, "an array handed out is never written afterwards");
        run(Mix.MIXED, STEPS);
    }

    // ---- per-atom checks ----------------------------------------------------------------------------------

    static void check(String atom) {
        MeasureScript s = new MeasureScript(atom);
        switch (atom) {
            case "M3ListMeasure" -> {
                M3ListMeasure fresh = new M3ListMeasure();
                assertEquals(0, fresh.fullMeasuresOnDeltas(), "a new measure counted nothing");
                assertEquals(0, fresh.remeasured());
                assertEquals(0, fresh.cellHeights().length, "no rows before the first layout");
                s.fill(MAX_ROWS, false);
                assertEquals(s.model.getSize(), s.layout(), "the first layout measures every row");
            }
            case "track" -> s.trackContract();
            case "fullMeasuresOnDeltas" -> {
                s.untrackable();
                new MeasureScript(atom + "/cap").pieceCap();
            }
            case "added" -> s.run(Mix.INSERTS, STEPS);
            case "removed" -> s.run(Mix.REMOVALS, STEPS);
            case "changed" -> s.run(Mix.CHANGES, STEPS);
            case "measure" -> s.run(Mix.MIXED, STEPS);
            case "cellWidth" -> s.run(Mix.INPUTS, STEPS);
            case "cellHeights" -> s.cellHeightsContract();
            case "remeasured" -> {
                s.delta();
                new MeasureScript(atom + "/events").run(Mix.EVENTS, STEPS);
            }
            case "measureAll" -> {
                s.run(Mix.INPUTS, STEPS);
                assertTrue(s.fullMeasures > 1, "input changes led to full layouts");
            }
            case "remeasure" -> {
                s.delta();
                new MeasureScript(atom + "/values").run(Mix.VALUES, STEPS);
            }
            case "same" -> probeMismatch(atom);
            case "size" -> s.sizeContract();
            case "keep" -> {
                s.run(Mix.EVENTS, STEPS);
                s.fontChange();
                assertEquals(s.model.getSize(), s.layout(), "a new font: every row");
                s.model.add(0, "after keep");
                assertEquals(2, s.layout(), "keep recorded the new font: the next delta measures one row and the probe");
            }
            case "isStableValue" -> {
                isStableValueContract();
                s.run(Mix.VALUES, STEPS);
            }
            case "supportsIncrementalSizing" -> supportsIncrementalSizingContract();
            case "sameInputs" -> s.sameInputsContract();
            default -> fail("no check for M3ListMeasure#" + atom);
        }
    }
}
