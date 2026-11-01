/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 4: JUnit suite of the JList measurement path (sun.swing.list):
 *          the piece table against a reference list, which rows a layout measures, the probe, the
 *          inputs that make every row measured again, the cases the path declines, untrackable
 *          events, the recorded event-less mutation divergence, plus the JList differential.
 * @modules java.desktop/sun.swing.list java.desktop/javax.swing.plaf.basic:open java.desktop/sun.swing.list:open
 * @build M3JListViewportDifferential
 * @run junit/othervm -Djava.awt.headless=true -Dm3.swing.viewport=true -XX:-OmitStackTraceInFastThrow M3ListMeasureTest
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Insets;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import javax.swing.AbstractListModel;
import javax.swing.CellRendererPane;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.plaf.basic.BasicListUI;

import org.junit.jupiter.api.Test;

import sun.swing.list.M3ListMeasure;
import sun.swing.list.M3ListRows;
import sun.swing.list.M3RowPieces;

public class M3ListMeasureTest {

    /** A value whose text changes without a list data event (a ListModel contract violation). */
    static final class Mutable {
        String text;

        Mutable(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** A model whose events are fired by the test, whatever its contents. */
    @SuppressWarnings("serial")
    static final class Model extends AbstractListModel<Object> {
        final List<Object> rows = new ArrayList<>();

        @Override
        public int getSize() {
            return rows.size();
        }

        @Override
        public Object getElementAt(int index) {
            return rows.get(index);
        }

        void added(int a, int b) {
            fireIntervalAdded(this, a, b);
        }

        void removed(int a, int b) {
            fireIntervalRemoved(this, a, b);
        }

        void changed(int a, int b) {
            fireContentsChanged(this, a, b);
        }
    }

    /** Two lists on one model: [0] on the stock loop (UI outside java.desktop), [1] on the M3 path. */
    static List<JList<Object>> pair(ListModel<Object> model) {
        JList<Object> stock = new JList<>(model);
        stock.setUI(new M3JListViewportDifferential.StockListUI());
        JList<Object> m3 = new JList<>(model);
        m3.setUI(new BasicListUI());
        for (JList<Object> l : List.of(stock, m3)) {
            l.setSize(300, 400);
        }
        return List.of(stock, m3);
    }

    static Object field(JList<Object> l, String name) throws Exception {
        Field f = BasicListUI.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(l.getUI());
    }

    static M3ListMeasure measure(JList<Object> l) throws Exception {
        return (M3ListMeasure) field(l, "m3Measure");
    }

    /** Runs one layout (updateLayoutState) on both lists and checks they agree. */
    static void layout(List<JList<Object>> p) throws Exception {
        Method m = BasicListUI.class.getDeclaredMethod("updateLayoutState");
        m.setAccessible(true);
        Field needed = BasicListUI.class.getDeclaredField("updateLayoutStateNeeded");
        needed.setAccessible(true);
        for (JList<Object> l : p) {
            m.invoke(l.getUI());
            needed.setInt(l.getUI(), 0);
        }
        same(p);
    }

    static void same(List<JList<Object>> p) throws Exception {
        assertArrayEquals((int[]) field(p.get(0), "cellHeights"), (int[]) field(p.get(1), "cellHeights"));
        assertEquals(field(p.get(0), "cellWidth"), field(p.get(1), "cellWidth"));
        assertEquals(field(p.get(0), "cellHeight"), field(p.get(1), "cellHeight"));
        assertEquals(p.get(0).getPreferredSize(), p.get(1).getPreferredSize());
    }

    static int measured(List<JList<Object>> p) throws Exception {
        return measure(p.get(1)).remeasured();
    }

    static DefaultListModel<Object> strings(int n) {
        DefaultListModel<Object> m = new DefaultListModel<>();
        for (int i = 0; i < n; i++) {
            m.addElement(i % 7 == 0 ? "<html>r" + i + "<br>two</html>" : "row " + i + "x".repeat(i % 13));
        }
        return m;
    }

    /** The piece table maps rows exactly as a list of old row indices edited the same way. */
    @Test
    void piecesMatchAReferenceList() {
        Random rnd = new Random(7);
        M3RowPieces pieces = new M3RowPieces();
        List<Integer> ref = new ArrayList<>();
        for (int round = 0; round < 40; round++) {
            int n = rnd.nextInt(30);
            pieces.reset(n);
            ref.clear();
            for (int i = 0; i < n; i++) {
                ref.add(i);
            }
            for (int op = 0; op < 60; op++) {
                int size = ref.size();
                int kind = rnd.nextInt(3);
                if (kind == 0 || size == 0) {
                    int at = rnd.nextInt(size + 1);
                    int k = 1 + rnd.nextInt(4);
                    pieces.insert(at, k);
                    for (int i = 0; i < k; i++) {
                        ref.add(at, M3RowPieces.NEW);
                    }
                } else {
                    int a = rnd.nextInt(size);
                    int b = Math.min(size - 1, a + rnd.nextInt(5));
                    if (kind == 1) {
                        pieces.remove(a, b);
                        ref.subList(a, b + 1).clear();
                    } else {
                        pieces.change(a, b);
                        for (int i = a; i <= b; i++) {
                            ref.set(i, M3RowPieces.NEW);
                        }
                    }
                }
                assertEquals(ref, expand(pieces), "round " + round + " op " + op);
            }
        }
    }

    static List<Integer> expand(M3RowPieces pieces) {
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < pieces.count(); i++) {
            assertTrue(pieces.length(i) > 0 || pieces.count() == 0, "empty piece");
            for (int j = 0; j < pieces.length(i); j++) {
                rows.add(pieces.start(i) == M3RowPieces.NEW ? M3RowPieces.NEW : pieces.start(i) + j);
            }
        }
        assertEquals(rows.size(), pieces.size());
        return rows;
    }

    /** A layout after list data events measures the touched rows and one probe, with stock answers. */
    @Test
    void measuresOnlyTouchedRows() throws Exception {
        if (!M3ListRows.ENABLED) {
            return;
        }
        DefaultListModel<Object> model = strings(1000);
        List<JList<Object>> p = pair(model);
        layout(p);
        assertEquals(1000, measured(p), "first layout measures every row");
        model.add(500, "inserted");
        layout(p);
        assertEquals(2, measured(p), "inserted row and the probe");
        model.set(10, "<html>a<br>b<br>c</html>");
        layout(p);
        assertEquals(2, measured(p));
        model.removeRange(100, 199);
        layout(p);
        assertEquals(1, measured(p), "probe only");
        model.addElement("<html>last<br>row</html>");
        layout(p);
        assertEquals(2, measured(p), "probe first, then the new last row");
        layout(p);
        assertEquals(1, measured(p), "no events: the probe");
        model.add(3, "a");
        model.add(3, "b");
        model.remove(4);
        model.set(0, "zero");
        layout(p);
        assertEquals(3, measured(p));
        p.get(0).getPreferredSize();
        same(p);
    }

    /** A change that resizes every row but no event names is caught by the probe. */
    @Test
    void probeCatchesAGlobalChange() throws Exception {
        if (!M3ListRows.ENABLED) {
            return;
        }
        Object old = UIManager.get("List.cellNoFocusBorder");
        try {
            for (int kind = 0; kind < 3; kind++) {
                boolean atEnd = kind == 1;
                DefaultListModel<Object> model = strings(50);
                List<JList<Object>> p = pair(model);
                layout(p);
                Insets in = ((JLabel) p.get(1).getCellRenderer()).getInsets();
                Border b = kind == 2 ? new EmptyBorder(in.top + 4, in.left, in.bottom, in.right)
                        : new EmptyBorder(atEnd ? 6 : 3, 2, 5, 2);
                UIManager.put("List.cellNoFocusBorder", b);
                if (atEnd) {
                    model.addElement("end");
                } else {
                    model.add(0, "front");
                }
                layout(p);
                assertEquals(51, measured(p), "probe mismatch measures every row");
                UIManager.put("List.cellNoFocusBorder", old);
            }
        } finally {
            UIManager.put("List.cellNoFocusBorder", old);
        }
    }

    /** Inputs besides the values that set row sizes make the next layout measure every row. */
    @Test
    void inputChangesMeasureEveryRow() throws Exception {
        if (!M3ListRows.ENABLED) {
            return;
        }
        DefaultListModel<Object> other = strings(80);
        List<Consumer<JList<Object>>> changes = List.of(
                l -> ((JLabel) l.getCellRenderer()).putClientProperty("html.disable", Boolean.TRUE),
                l -> ((JLabel) l.getCellRenderer()).setIconTextGap(9),
                l -> ((JLabel) l.getCellRenderer()).setHorizontalTextPosition(JLabel.LEADING),
                l -> ((JLabel) l.getCellRenderer()).setVerticalTextPosition(JLabel.TOP),
                l -> ((JLabel) l.getCellRenderer()).setHorizontalAlignment(JLabel.RIGHT),
                l -> ((JLabel) l.getCellRenderer()).setVerticalAlignment(JLabel.BOTTOM),
                l -> ((JLabel) l.getCellRenderer()).setPreferredSize(new Dimension(40, 11)),
                l -> ((JLabel) l.getCellRenderer()).setUI(new BasicLabelUI()),
                l -> l.setFont(l.getFont().deriveFont(23f)),
                l -> l.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT),
                l -> l.setCellRenderer(new DefaultListCellRenderer()),
                l -> l.setModel(other));
        for (int i = 0; i < changes.size(); i++) {
            DefaultListModel<Object> model = strings(80);
            List<JList<Object>> p = pair(model);
            layout(p);
            for (JList<Object> l : p) {
                changes.get(i).accept(l);
            }
            ((DefaultListModel<Object>) p.get(0).getModel()).add(1, "<html>x<br>y</html>");
            layout(p);
            assertEquals(81, measured(p), "change " + i);
        }
    }

    /** The cases the path declines keep the stock loop; the next accepted layout measures every row. */
    @Test
    void declinesOutsideItsCases() throws Exception {
        if (!M3ListRows.ENABLED) {
            return;
        }
        JList<Object> list = new JList<>(strings(5));
        CellRendererPane pane = new CellRendererPane();
        M3ListMeasure m = new M3ListMeasure();
        BasicListUI ui = new BasicListUI();
        assertTrue(m.measure(ui, list, pane));
        assertEquals(5, m.cellHeights().length);
        assertFalse(m.measure(new M3JListViewportDifferential.StockListUI(), list, pane), "UI outside java.desktop");
        list.setFixedCellHeight(12);
        assertFalse(m.measure(ui, list, pane), "fixed cell height");
        list.setFixedCellHeight(-1);
        list.setEnabled(false);
        assertFalse(m.measure(ui, list, pane), "disabled list");
        list.setEnabled(true);
        list.setCellRenderer(null);
        assertFalse(m.measure(ui, list, pane), "no renderer");
        list.setCellRenderer(new M3JListViewportDifferential.OtherRenderer());
        assertFalse(m.measure(ui, list, pane), "DefaultListCellRenderer subclass");
        list.setCellRenderer(new M3JListViewportDifferential.Renderer());
        assertFalse(m.measure(ui, list, pane), "other renderer");
        list.setCellRenderer(new DefaultListCellRenderer.UIResource());
        m.added(0, 0);
        assertTrue(m.measure(ui, list, pane));
        assertEquals(5, m.remeasured(), "declined layouts drop the kept sizes");
        List<JList<Object>> p = pair(strings(30));
        p.get(1).setFixedCellHeight(10);
        p.get(0).setFixedCellHeight(10);
        layout(p);
        p.get(1).setFixedCellHeight(-1);
        p.get(0).setFixedCellHeight(-1);
        layout(p);
        assertEquals(30, measured(p));
    }

    /** Events that the piece table cannot follow make the next layout measure every row. */
    @Test
    void untrackableEventsMeasureEveryRow() throws Exception {
        if (!M3ListRows.ENABLED) {
            return;
        }
        List<Consumer<Model>> events = List.of(
                m -> m.changed(-1, -1),
                m -> m.rows.add(0, "silent"),
                m -> {
                    for (int i = 0; i < 600; i++) {
                        m.rows.add(3 * i, "e" + i);
                        m.added(3 * i, 3 * i);
                    }
                },
                m -> {
                    for (int i = 0; i < 1100; i++) {
                        m.rows.remove(i);
                        m.removed(i, i);
                    }
                },
                m -> {
                    for (int i = 0; i < 600; i++) {
                        m.rows.set(2 * i + 1, "c" + i);
                        m.changed(2 * i + 1, 2 * i + 1);
                    }
                });
        for (int i = 0; i < events.size(); i++) {
            Model model = new Model();
            for (int r = 0; r < 3000; r++) {
                model.rows.add("row " + r);
            }
            List<JList<Object>> p = pair(model);
            layout(p);
            events.get(i).accept(model);
            layout(p);
            assertEquals(model.getSize(), measured(p), "event " + i);
        }
        List<Consumer<M3ListMeasure>> calls = List.of(
                m -> m.changed(0, 40),
                m -> m.removed(5, 40),
                m -> m.removed(-1, 2),
                m -> m.added(-1, 0),
                m -> m.added(31, 31),
                m -> m.added(0, Integer.MAX_VALUE));
        JList<Object> list = new JList<>(strings(30));
        CellRendererPane pane = new CellRendererPane();
        BasicListUI ui = new BasicListUI();
        for (int i = 0; i < calls.size(); i++) {
            M3ListMeasure m = new M3ListMeasure();
            assertTrue(m.measure(ui, list, pane));
            calls.get(i).accept(m);
            m.added(0, 0);
            assertTrue(m.measure(ui, list, pane));
            assertEquals(30, m.remeasured(), "call " + i);
        }
        Model model = new Model();
        for (int r = 0; r < 30; r++) {
            model.rows.add("row " + r);
        }
        List<JList<Object>> p = pair(model);
        layout(p);
        model.rows.add(4, "<html>a<br>b</html>");
        model.rows.add(4, "c");
        model.added(5, 4);
        layout(p);
        assertEquals(3, measured(p), "reversed indices are an interval");
    }

    /**
     * The one known difference (DESIGN.md section 10): a value changed without a list data event.
     * Stock measures every row on every layout, so it picks the change up at the next layout that
     * any other event or property change causes; the M3 path keeps the row's recorded size until
     * the row gets an event or every row is measured again (here a font change).
     */
    @Test
    void eventlessMutationIsTheOneKnownDivergence() throws Exception {
        if (!M3ListRows.ENABLED) {
            return;
        }
        DefaultListModel<Object> model = strings(20);
        Mutable value = new Mutable("plain");
        model.set(3, value);
        List<JList<Object>> p = pair(model);
        layout(p);
        int before = ((int[]) field(p.get(1), "cellHeights"))[3];
        value.text = "<html>now<br>three<br>lines</html>";
        model.addElement("unrelated");
        Method m = BasicListUI.class.getDeclaredMethod("updateLayoutState");
        m.setAccessible(true);
        m.invoke(p.get(0).getUI());
        m.invoke(p.get(1).getUI());
        int stock = ((int[]) field(p.get(0), "cellHeights"))[3];
        int m3 = ((int[]) field(p.get(1), "cellHeights"))[3];
        assertNotEquals(before, stock, "stock re-measured the mutated row");
        assertEquals(before, m3, "the M3 path kept its recorded size");
        for (JList<Object> l : p) {
            l.setFont(l.getFont().deriveFont(l.getFont().getSize2D() + 1));
        }
        layout(p);
        model.set(3, value);
        layout(p);
    }

    /** Empty lists, clear and refill, and fixed cell widths give the stock cellWidth and heights. */
    @Test
    void emptyClearAndFixedWidth() throws Exception {
        if (!M3ListRows.ENABLED) {
            return;
        }
        DefaultListModel<Object> model = new DefaultListModel<>();
        List<JList<Object>> p = pair(model);
        layout(p);
        assertEquals(-1, field(p.get(1), "cellWidth"));
        for (int i = 0; i < 12; i++) {
            model.addElement("r" + i);
        }
        layout(p);
        model.clear();
        layout(p);
        model.addElement("again");
        layout(p);
        assertEquals(1, measured(p), "every row new: measured as a full layout");
        for (JList<Object> l : p) {
            l.setFixedCellWidth(57);
        }
        model.addElement("wide wide wide wide wide wide wide");
        layout(p);
        assertEquals(57, field(p.get(1), "cellWidth"));
        for (JList<Object> l : p) {
            l.setFixedCellWidth(-1);
        }
        layout(p);
    }

    /** The JList-level differential, DefaultListCellRenderer seeds included (every third seed). */
    @Test
    void jlistDifferential() throws Exception {
        if (!M3ListRows.ENABLED) {
            return;
        }
        String summary = M3JListViewportDifferential.runSeeds(100, 30, 200);
        assertTrue(summary.contains(" failed 0 "), summary);
    }
}
