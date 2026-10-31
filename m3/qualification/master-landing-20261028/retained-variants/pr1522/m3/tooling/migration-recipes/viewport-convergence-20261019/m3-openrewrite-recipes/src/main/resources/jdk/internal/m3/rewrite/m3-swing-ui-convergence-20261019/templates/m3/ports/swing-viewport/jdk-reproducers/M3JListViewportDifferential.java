/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaves 3 and 4: a JList whose BasicListUI reads row offsets from a
 *          prefix lane and measures only the rows that list data events touched answers every
 *          public query and paints every pixel as the stock loops do.
 * @modules java.desktop/javax.swing.plaf.basic:open java.desktop/sun.swing.list:open
 * @run main/othervm -Djava.awt.headless=true -Dm3.swing.viewport=true -XX:-OmitStackTraceInFastThrow M3JListViewportDifferential 40 300
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicListUI;

/**
 * Seeded scripts of public JList operations and model edits run against two JLists on one
 * DefaultListModel. One has {@link StockListUI}, a BasicListUI subclass outside java.desktop, so
 * {@code sun.swing.list.M3ListRows} leaves it on the stock loops; the other has a plain
 * BasicListUI, which with {@code -Dm3.swing.viewport=true} takes its row offsets from the prefix
 * lane. Row heights come from the values (0, negative and very large heights included, so the
 * wrapped int sums and the hand-back to the stock loop are exercised). After every step the public
 * answers are compared; every 10 steps both lists are painted at the same scroll offset and
 * compared pixel by pixel. Every third seed ({@code seed % 3 == 2}) keeps the look and feel's
 * {@code DefaultListCellRenderer} and fills the model with plain, HTML (one to four lines), empty
 * and null strings and icons, so that {@code sun.swing.list.M3ListMeasure} measures only the rows
 * that list data events touched; those seeds also toggle {@code html.disable}, the icon-text gap,
 * the enabled state and the orientation, and swap the renderer. Usage:
 * {@code M3JListViewportDifferential <seeds> <steps> [firstSeed]}.
 */
public final class M3JListViewportDifferential {

    /** The stock side: declared outside java.desktop, so the M3 row path stays off for it. */
    static final class StockListUI extends BasicListUI {
    }

    /** A list value: its renderer component is {@code width x height}. */
    record Item(int id, int width, int height) {
        @Override
        public String toString() {
            return "i" + id + ":" + height;
        }
    }

    /** A list value painted by DefaultListCellRenderer as an icon of {@code w x h}. */
    record Swatch(int id, int w, int h) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(new Color(0x203040 + id * 7919 & 0xffffff));
            g.fillRect(x, y, w, h);
        }

        @Override
        public int getIconWidth() {
            return w;
        }

        @Override
        public int getIconHeight() {
            return h;
        }
    }

    /** A DefaultListCellRenderer subclass: outside the classes M3ListMeasure accepts. */
    @SuppressWarnings("serial")
    static final class OtherRenderer extends DefaultListCellRenderer {
    }

    static final class Renderer implements ListCellRenderer<Object> {
        private final JLabel label = new JLabel();

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                boolean focused) {
            label.setText(String.valueOf(value));
            label.setOpaque(true);
            label.setBackground(selected ? Color.ORANGE : (index % 2 == 0 ? Color.WHITE : Color.LIGHT_GRAY));
            if (value instanceof Item it) {
                label.setPreferredSize(new Dimension(it.width(), it.height()));
            } else {
                label.setPreferredSize(null);
            }
            return label;
        }
    }

    private final Random rnd;
    private final DefaultListModel<Object> model = new DefaultListModel<>();
    private final JList<Object> stock = new JList<>(model);
    private final JList<Object> m3 = new JList<>(model);
    private final List<String> log = new ArrayList<>();
    private final boolean wide;
    private final boolean labels;
    private int ids;
    private int step;
    static int pixelChecks;
    static int m3Steps;
    static int partialLayouts;

    M3JListViewportDifferential(long seed) {
        rnd = new Random(seed);
        wide = seed % 7 == 0;
        labels = seed % 3 == 2;
        stock.setUI(new StockListUI());
        m3.setUI(new BasicListUI());
        for (JList<Object> l : lists()) {
            if (!labels) {
                l.setCellRenderer(new Renderer());
            }
            l.setSize(320, 400);
        }
        int n = wide ? 3000 + rnd.nextInt(3000) : rnd.nextInt(60);
        for (int i = 0; i < n; i++) {
            model.addElement(item());
        }
        log.add("seed " + seed + " size " + n);
    }

    List<JList<Object>> lists() {
        return List.of(stock, m3);
    }

    Object item() {
        return labels ? label() : sized();
    }

    /** A value for DefaultListCellRenderer: plain, HTML, empty or null text, or an icon. */
    Object label() {
        int r = rnd.nextInt(100);
        int id = ids++;
        if (r < 45) {
            return "v" + id + "x".repeat(rnd.nextInt(40));
        } else if (r < 62) {
            return "<html>h" + id + "<br>y".repeat(rnd.nextInt(4)) + "</html>";
        } else if (r < 72) {
            return "";
        } else if (r < 90) {
            return new Swatch(id, 1 + rnd.nextInt(60), rnd.nextInt(50));
        }
        return null;
    }

    Item sized() {
        int r = rnd.nextInt(100);
        int h = r < 80 ? 12 + rnd.nextInt(30) : r < 90 ? 0 : r < 95 ? 1 + rnd.nextInt(3)
                : r < 98 ? (1 << 24) + rnd.nextInt(1 << 25) : -1 - rnd.nextInt(20);
        return new Item(ids++, 20 + rnd.nextInt(200), h);
    }

    interface Action {
        void run(JList<Object> l) throws Exception;
    }

    interface Query {
        Object ask(JList<Object> l) throws Exception;
    }

    /** Runs a list operation on both lists; both must succeed or throw the same class. */
    void act(String what, Action a) {
        log.add(step + " " + what);
        String r1 = attempt(stock, a);
        String r2 = attempt(m3, a);
        if (!r1.equals(r2)) {
            fail(what, r1, r2);
        }
    }

    static String attempt(JList<Object> l, Action a) {
        try {
            a.run(l);
            return "ok";
        } catch (Exception | Error e) {
            return e.getClass().getName();
        }
    }

    void compare(String what, Query q) {
        String a = answer(stock, q);
        String b = answer(m3, q);
        if (!a.equals(b)) {
            fail(what, a, b);
        }
    }

    static String answer(JList<Object> l, Query q) {
        try {
            Object v = q.ask(l);
            return v instanceof int[] arr ? Arrays.toString(arr) : String.valueOf(v);
        } catch (Exception | Error e) {
            return "throws " + e.getClass().getName();
        }
    }

    void fail(String what, String a, String b) {
        StringBuilder s = new StringBuilder("DIFFERENCE at step " + step + " in " + what + "\n stock: " + a
                + "\n    m3: " + b + "\nop log:\n");
        int from = Math.max(0, log.size() - 60);
        for (String l : log.subList(from, log.size())) {
            s.append("  ").append(l).append('\n');
        }
        throw new AssertionError(s.toString());
    }

    void step() {
        int size = model.getSize();
        int op = rnd.nextInt(100);
        if (op < 18) {
            int at = rnd.nextInt(size + 1);
            int k = 1 + rnd.nextInt(wide ? 40 : 4);
            act("add " + k + " at " + at, l -> { });
            for (int i = 0; i < k; i++) {
                model.add(at, item());
            }
        } else if (op < 28 && size > 0) {
            int from = rnd.nextInt(size);
            int to = Math.min(size - 1, from + rnd.nextInt(wide ? 30 : 3));
            log.add(step + " removeRange " + from + " " + to);
            model.removeRange(from, to);
        } else if (op < 38 && size > 0) {
            int at = rnd.nextInt(size);
            Object it = item();
            log.add(step + " set " + at + " " + it);
            model.set(at, it);
        } else if (op < 41) {
            int h = rnd.nextInt(3) == 0 ? -1 : 8 + rnd.nextInt(20);
            act("setFixedCellHeight " + h, l -> l.setFixedCellHeight(h));
        } else if (op < 43) {
            int w = rnd.nextInt(3) == 0 ? -1 : 30 + rnd.nextInt(100);
            act("setFixedCellWidth " + w, l -> l.setFixedCellWidth(w));
        } else if (op < 46) {
            Object p = rnd.nextBoolean() ? null : item();
            act("setPrototypeCellValue " + p, l -> l.setPrototypeCellValue(p));
        } else if (op < 49) {
            int o = rnd.nextInt(6) < 4 ? JList.VERTICAL : rnd.nextBoolean() ? JList.VERTICAL_WRAP
                    : JList.HORIZONTAL_WRAP;
            act("setLayoutOrientation " + o, l -> l.setLayoutOrientation(o));
        } else if (op < 53) {
            int top = rnd.nextInt(5) == 0 ? -7 : rnd.nextInt(12);
            boolean none = rnd.nextInt(4) == 0;
            act("setBorder " + (none ? "null" : "top " + top), l -> l.setBorder(none ? null
                    : new EmptyBorder(top, 3, 2, 1)));
        } else if (op < 57) {
            int w = 50 + rnd.nextInt(400);
            int h = 50 + rnd.nextInt(800);
            act("setSize " + w + "x" + h, l -> l.setSize(w, h));
        } else if (op < 59) {
            int v = rnd.nextInt(12) - 1;
            act("setVisibleRowCount " + v, l -> l.setVisibleRowCount(v));
        } else if (op < 61) {
            float f = 9 + rnd.nextInt(10);
            act("setFont " + f, l -> l.setFont(l.getFont().deriveFont(f)));
        } else if (op < 66 && size > 0) {
            int a = rnd.nextInt(size);
            int b = rnd.nextInt(size);
            act("setSelectionInterval " + a + " " + b, l -> l.setSelectionInterval(a, b));
        } else if (op < 68) {
            log.add(step + " clear");
            model.clear();
        } else if (labels && op < 81) {
            labelStep(op);
        } else {
            act("query only", l -> { });
        }
    }

    /** Changes that set row sizes without a list data event, for DefaultListCellRenderer seeds. */
    void labelStep(int op) {
        if (op < 70) {
            Boolean b = rnd.nextBoolean() ? Boolean.TRUE : null;
            act("html.disable " + b, l -> ((JComponent) l.getCellRenderer()).putClientProperty("html.disable", b));
        } else if (op < 72) {
            int g = rnd.nextInt(12);
            act("iconTextGap " + g, l -> ((JLabel) l.getCellRenderer()).setIconTextGap(g));
        } else if (op < 74) {
            int p = rnd.nextBoolean() ? JLabel.LEADING : JLabel.CENTER;
            act("horizontalTextPosition " + p, l -> ((JLabel) l.getCellRenderer()).setHorizontalTextPosition(p));
        } else if (op < 76) {
            boolean e = rnd.nextInt(3) > 0;
            act("setEnabled " + e, l -> l.setEnabled(e));
        } else if (op < 78) {
            ComponentOrientation o = rnd.nextBoolean() ? ComponentOrientation.RIGHT_TO_LEFT
                    : ComponentOrientation.LEFT_TO_RIGHT;
            act("setComponentOrientation " + o.isLeftToRight(), l -> l.setComponentOrientation(o));
        } else {
            int k = rnd.nextInt(3);
            act("setCellRenderer " + k, l -> l.setCellRenderer(k == 0 ? new DefaultListCellRenderer.UIResource()
                    : k == 1 ? new DefaultListCellRenderer() : new OtherRenderer()));
        }
    }

    void check(boolean pixels) {
        compare("preferred size", JList::getPreferredSize);
        compare("scrollable size", JList::getPreferredScrollableViewportSize);
        compare("first/last visible", l -> l.getFirstVisibleIndex() + " " + l.getLastVisibleIndex());
        int size = model.getSize();
        List<Integer> idx = new ArrayList<>();
        if (size <= 120) {
            for (int i = -1; i <= size; i++) {
                idx.add(i);
            }
        } else {
            for (int i = 0; i < 100; i++) {
                idx.add(rnd.nextInt(size));
            }
            idx.addAll(List.of(-1, 0, 1, size - 2, size - 1, size));
        }
        for (int i : idx) {
            compare("indexToLocation " + i, l -> l.indexToLocation(i));
            compare("getCellBounds " + i, l -> l.getCellBounds(i, i));
        }
        if (size > 0) {
            int a = rnd.nextInt(size);
            int b = rnd.nextInt(size);
            compare("getCellBounds " + a + ".." + b, l -> l.getCellBounds(a, b));
        }
        int height = stock.getPreferredSize().height;
        List<Integer> ys = new ArrayList<>(List.of(-20, -1, 0, 1, height - 1, height, height + 50,
                Integer.MAX_VALUE, Integer.MIN_VALUE));
        for (int i : idx) {
            Rectangle r = stock.getCellBounds(i, i);
            if (r != null) {
                ys.addAll(List.of(r.y - 1, r.y, r.y + r.height - 1, r.y + r.height));
            }
        }
        for (int i = 0; i < 40; i++) {
            ys.add(height <= 0 ? rnd.nextInt(100) : (int) (rnd.nextDouble() * height));
        }
        for (int y : ys) {
            compare("locationToIndex y " + y, l -> l.locationToIndex(new Point(5, y)));
        }
        Rectangle vis = new Rectangle(0, Math.max(0, height / 2), 300, 200);
        compare("unit increment", l -> l.getScrollableUnitIncrement(vis, javax.swing.SwingConstants.VERTICAL, 1)
                + " " + l.getScrollableUnitIncrement(vis, javax.swing.SwingConstants.VERTICAL, -1));
        compare("block increment", l -> l.getScrollableBlockIncrement(vis, javax.swing.SwingConstants.VERTICAL, 1)
                + " " + l.getScrollableBlockIncrement(vis, javax.swing.SwingConstants.VERTICAL, -1));
        if (pixels) {
            int y = height <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE - 1000L, (long) (rnd.nextDouble() * height));
            compare("pixels at y " + y, l -> pixels(l, y));
            pixelChecks++;
        }
        if (offsetsBuilt()) {
            m3Steps++;
        }
        countPartialLayout();
    }

    private Object lastHeights;

    /** Counts layouts of the M3 list that measured fewer rows than it has (evidence for leaf 4). */
    void countPartialLayout() {
        try {
            Field h = BasicListUI.class.getDeclaredField("cellHeights");
            h.setAccessible(true);
            Object heights = h.get(m3.getUI());
            Field f = BasicListUI.class.getDeclaredField("m3Measure");
            f.setAccessible(true);
            Object measure = f.get(m3.getUI());
            int measured = (int) measure.getClass().getMethod("remeasured").invoke(measure);
            if (heights != null && heights != lastHeights && measured < model.getSize()) {
                partialLayouts++;
            }
            lastHeights = heights;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("M3 measure path not installed", e);
        }
    }

    /** A hash of the list painted into a 320 x 240 image whose top is {@code y}. */
    static String pixels(JList<Object> l, int y) {
        BufferedImage img = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.MAGENTA);
        g.fillRect(0, 0, 320, 240);
        g.translate(0, -y);
        g.setClip(0, y, 320, 240);
        l.paint(g);
        g.dispose();
        return Integer.toHexString(Arrays.hashCode(img.getRGB(0, 0, 320, 240, null, 0, 320)));
    }

    /** Whether the M3 list's UI has built offsets (evidence that the M3 row path ran). */
    boolean offsetsBuilt() {
        try {
            Field f = BasicListUI.class.getDeclaredField("m3Rows");
            f.setAccessible(true);
            Object rows = f.get(m3.getUI());
            Field s = rows.getClass().getDeclaredField("source");
            s.setAccessible(true);
            return s.get(rows) != null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("M3 row path not installed", e);
        }
    }

    void run(int steps) {
        check(true);
        for (step = 0; step < steps; step++) {
            step();
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
                    new M3JListViewportDifferential(seed).run(steps);
                    passed[0]++;
                } catch (AssertionError e) {
                    failures.add("seed " + seed);
                    String msg = String.valueOf(e.getMessage());
                    System.err.println(failures.size() == 1 ? msg
                            : "seed " + seed + ": " + msg.lines().limit(3).reduce((x, y) -> x + " | " + y).orElse(""));
                }
            }
        });
        return "M3JListViewportDifferential seeds " + seeds + " steps " + steps + " passed " + passed[0]
                + " failed " + failures.size() + " " + failures + "; pixel comparisons " + pixelChecks
                + "; checks on M3 offsets " + m3Steps + "; partial M3 layouts " + partialLayouts;
    }
}
