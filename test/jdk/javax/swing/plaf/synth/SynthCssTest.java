/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary Opt-in Synth CSS preserves XML, ownership, cascade, lifecycle and lazy models
 * @run main/othervm -Djava.awt.headless=true SynthCssTest
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Objects;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.synth.ColorType;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthConstants;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.synth.SynthPainter;
import javax.swing.plaf.synth.SynthStyle;
import javax.swing.plaf.synth.SynthStyleFactory;
import javax.swing.plaf.synth.SynthUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.html.StyleSheet;

public class SynthCssTest {
    private static final Color XML_COLOR = new Color(0x123456);
    private static int checks;
    private static int painted;
    private static SynthLookAndFeel installed;

    private static final String XML = """
        <synth>
          <object id="marker" class="SynthCssTest$MarkerPainter"/>
          <style id="base">
            <font name="Dialog" style="PLAIN" size="12"/>
            <insets top="1" left="2" bottom="3" right="4"/>
            <state>
              <color value="#123456" type="FOREGROUND"/>
              <color value="#f0f0f0" type="BACKGROUND"/>
              <color value="#123456" type="TEXT_FOREGROUND"/>
            </state>
            <state value="DISABLED">
              <color value="#555555" type="FOREGROUND"/>
            </state>
            <property key="css.marker" type="integer" value="27"/>
            <painter idref="marker" method="buttonBackground"/>
          </style>
          <bind style="base" type="region" key=".*"/>
        </synth>
        """;

    public static class MarkerPainter extends SynthPainter {
        public void paintButtonBackground(SynthContext context, Graphics g,
                                          int x, int y, int w, int h) {
            painted++;
        }
    }

    public static void main(String[] args) throws Exception {
        LookAndFeel previous = UIManager.getLookAndFeel();
        SynthLookAndFeel beforeInstall = new SynthLookAndFeel();
        TrackingReader source = new TrackingReader("Button { color: red; }");
        beforeInstall.loadCss(source);
        check(!source.closed, "caller retains reader ownership before installation");
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    xmlAndOwnership();
                    cascadeAndStates();
                    valuesAndSelectors();
                    failuresAreAtomic();
                    lifecycleAndFactories();
                    lazyViewports();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            TrackingReader offEdt = new TrackingReader("Button { color: red; }");
            expect(IllegalStateException.class, () -> installed.loadCss(offEdt));
            equal(0, offEdt.reads, "installed load rejects off-EDT before reading");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(previous);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
        System.out.println("Synth CSS: " + checks + " checks passed");
    }

    private static SynthLookAndFeel theme(String css) throws Exception {
        SynthLookAndFeel laf = new SynthLookAndFeel();
        laf.load(new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)),
                 SynthCssTest.class);
        if (css != null) {
            laf.loadCss(new StringReader(css));
        }
        UIManager.setLookAndFeel(laf);
        installed = laf;
        return laf;
    }

    private static JButton button(String name, String classes) {
        JButton button = new JButton("CSS");
        button.setName(name);
        button.putClientProperty("styleClass", classes);
        return button;
    }

    private static SynthStyle style(JComponent component) {
        return ((SynthUI) component.getUI()).getContext(component).getStyle();
    }

    private static SynthContext context(JComponent component, int state) {
        return new SynthContext(component, SynthLookAndFeel.getRegion(component),
                                style(component), state);
    }

    private static Color color(JComponent component, int state) {
        return style(component).getColor(context(component, state), ColorType.FOREGROUND);
    }

    private static void xmlAndOwnership() throws Exception {
        SynthLookAndFeel laf = theme(null);
        JButton plain = button(null, null);
        equal(XML_COLOR, plain.getForeground(), "XML-only foreground unchanged");
        equal(new Color(0x555555), color(plain, SynthConstants.DISABLED),
              "XML disabled state unchanged");
        SynthStyle original = style(plain);
        equal(original, style(button(null, null)), "XML cache identity retained");

        JButton owned = button("owned", null);
        Color foreground = new Color(0x246813);
        Color background = new Color(0x987654);
        Font font = new Font(Font.MONOSPACED, Font.BOLD, 19);
        Border border = new LineBorder(Color.PINK, 3);
        owned.setForeground(foreground);
        owned.setBackground(background);
        owned.setFont(font);
        owned.setBorder(border);
        owned.setOpaque(false);
        laf.loadCss(new StringReader("""
            Button { color: red; background-color: #abcdef; font-size: 22px;
                     padding: 9px; -synth-opaque: true; }
            """));
        SynthLookAndFeel.updateStyles(plain);
        SynthLookAndFeel.updateStyles(owned);
        equal(Color.RED, plain.getForeground(), "CSS installs theme foreground");
        check(plain.getForeground() instanceof UIResource, "theme color remains UIResource");
        check(plain.getFont() instanceof UIResource, "theme font remains UIResource");
        equal(foreground, owned.getForeground(), "application foreground retained");
        equal(background, owned.getBackground(), "application background retained");
        check(owned.getFont() == font, "application font identity retained");
        check(owned.getBorder() == border, "application border identity retained");
        check(!owned.isOpaque(), "application opaque setting retained");
        equal(27, style(plain).get(context(plain, SynthConstants.ENABLED), "css.marker"),
              "XML property retained with CSS");
        int oldPainted = painted;
        BufferedImage image = new BufferedImage(30, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        try {
            style(plain).getPainter(context(plain, SynthConstants.ENABLED))
                .paintButtonBackground(context(plain, SynthConstants.ENABLED), graphics,
                                       0, 0, 30, 20);
        } finally {
            graphics.dispose();
        }
        equal(oldPainted + 1, painted, "original XML painter delegates through CSS style");
        plain.setSize(30, 20);
        equal(0xffabcdef, paint(plain).getRGB(0, 0), "CSS background reaches original Swing painting");
        equal(XML_COLOR, new JLabel("label").getForeground(), "unmatched region unchanged");

        // Existing XML API continues to work after CSS was loaded.
        laf.load(new ByteArrayInputStream("""
            <synth><style id="next"><property key="css.marker" type="integer" value="42"/>
            </style><bind style="next" type="region" key="Button"/></synth>
            """.getBytes(StandardCharsets.UTF_8)), SynthCssTest.class);
        SynthLookAndFeel.updateStyles(plain);
        equal(42, style(plain).get(context(plain, SynthConstants.ENABLED), "css.marker"),
              "XML can load after CSS without a factory cast failure");
        equal(Color.RED, plain.getForeground(), "CSS remains layered over new XML");
    }

    private static void cascadeAndStates() throws Exception {
        SynthLookAndFeel laf = theme("""
            Button { color: red; background-color: #fff; font-size: 14px; }
            .accent { color: blue; font-weight: bold; }
            #primary { color: #010203; }
            Button:hover { color: lime; font-style: italic; }
            .accent:disabled { color: gray; }
            .forced { color: purple !important; color: red; }
            #primary:pressed { background-color: #aabbcc; }
            Button:enabled { padding-left: 7px; }
            """);
        JButton primary = button("primary", "accent");
        equal(new Color(0x010203), primary.getForeground(), "id beats class and region");
        equal(new Color(0x010203), color(primary, SynthConstants.MOUSE_OVER),
              "id default beats less-specific hover declaration");
        Font hover = style(primary).getFont(context(primary, SynthConstants.MOUSE_OVER));
        equal(Font.BOLD | Font.ITALIC, hover.getStyle(), "font longhands cascade independently");
        equal(14, hover.getSize(), "state font inherits matching non-state size");
        equal(new Color(0xaabbcc), style(primary).getColor(
              context(primary, SynthConstants.PRESSED), ColorType.BACKGROUND),
              "pressed state background independent of foreground");
        equal(7, style(primary).getInsets(context(primary, SynthConstants.PRESSED), null).left,
              "enabled pseudo-class includes pressed primary state");
        equal(2, style(primary).getInsets(context(primary, SynthConstants.DISABLED), null).left,
              "enabled pseudo-class excludes disabled state");
        JButton forced = button("primary", "accent forced");
        equal(new Color(0x800080), forced.getForeground(), "important beats id specificity");
        equal(new Color(0x800080), color(forced, SynthConstants.DISABLED),
              "normal declaration cannot replace important state winner");
        JButton accent = button(null, "accent");
        equal(Color.BLUE, accent.getForeground(), "class beats region");
        equal(Color.GREEN, color(accent, SynthConstants.MOUSE_OVER),
              "state specificity compares per property");
        equal(Color.GRAY, color(accent, SynthConstants.DISABLED), "disabled state selector");
        laf.loadCss(new StringReader(".accent { color: orange; }"));
        SynthLookAndFeel.updateStyles(accent);
        equal(new Color(0xffa500), accent.getForeground(), "later load wins tied specificity");
        laf.loadCss(new StringReader(".forced { color: teal ! important; }"));
        SynthLookAndFeel.updateStyles(forced);
        equal(new Color(0x008080), forced.getForeground(), "later important declaration wins");

        laf.loadCss(new StringReader("""
            #state:focus:selected:default { color: #224466; }
            #state:active { background-color: #6688aa; }
            #state:checked { padding-top: 6px; }
            """));
        JButton state = button("state", null);
        int combined = SynthConstants.ENABLED | SynthConstants.FOCUSED |
                       SynthConstants.SELECTED | SynthConstants.DEFAULT;
        equal(new Color(0x224466), color(state, combined), "combined state selector");
        equal(6, style(state).getInsets(context(state, combined), null).top,
              "checked aliases selected");
        equal(new Color(0x6688aa), style(state).getColor(context(state, SynthConstants.PRESSED),
              ColorType.BACKGROUND), "active aliases pressed");
    }

    private static void valuesAndSelectors() throws Exception {
        SynthLookAndFeel laf = theme("""
            bUtToN.values, Label.values {
                color: rgb(17, 34, 51); background-color: rgba(4,5,6,0.5);
                font-family: monospace; font-size: 12pt;
                font-weight: 700; font-style: oblique;
                padding: 1px 2px 3px 4px; padding-left: 8px;
            }
            #Case { color: #abcd; }
            #case { color: #11223344; }
            Button[attr], #wrong { color: yellow; }
            "Button" { color: yellow; }
            Button:not(:disabled) { color: yellow; }
            Panel Button { color: yellow; }
            Button > Label { color: yellow; }
            .unknown:unknown { color: yellow; }
            """);
        JButton values = button(null, "values");
        equal(new Color(0x112233), values.getForeground(), "rgb integer channels");
        equal(new Color(4, 5, 6, 128), values.getBackground(), "rgba alpha preserved");
        equal(Font.MONOSPACED, values.getFont().getFamily(), "generic family mapping");
        equal(16, values.getFont().getSize(), "point to logical pixel conversion");
        equal(Font.BOLD | Font.ITALIC, values.getFont().getStyle(), "font weight and style");
        equal(new Insets(1, 8, 3, 2), style(values).getInsets(context(values, 1), null),
              "padding expands by side and respects later longhand");
        JLabel label = new JLabel("grouped");
        label.putClientProperty("styleClass", "values");
        equal(values.getForeground(), label.getForeground(), "comma-separated selectors");
        equal(new Color(0xaa, 0xbb, 0xcc, 0xdd), button("Case", null).getForeground(),
              "short alpha hex and case-sensitive id");
        equal(new Color(0x11, 0x22, 0x33, 0x44), button("case", null).getForeground(),
              "long alpha hex order");
        equal(XML_COLOR, button("wrong", "unknown").getForeground(),
              "unsupported selector invalidates grouped rule without broadening");
        equal(XML_COLOR, button(null, "VALUES").getForeground(), "class names remain case-sensitive");
        Region subclass = new Region("Button", null, true) {
            public String getName() {
                throw new AssertionError("style matching must not call overridden Region.getName");
            }
        };
        SynthStyle subregionStyle = SynthLookAndFeel.getStyle(values, subclass);
        SynthContext subregionContext = new SynthContext(values, subclass, subregionStyle, 1);
        equal(new Color(0x112233), subregionStyle.getColor(subregionContext, ColorType.FOREGROUND),
              "CSS uses the existing Region identity without new subclass callbacks");

        laf.loadCss(new StringReader("""
            .values { color: #xyz; background-color: rgba(1,2,3,2);
                font-size: 1e30px; font-weight: 101; font-style: sideways;
                padding: 3px -4px; padding-left: NaNpx; -synth-opaque: perhaps;
                font-family: url(http://invalid.example/font); }
            @import url(http://invalid.example/never-load.css);
            @media screen { Button { color: yellow; } }
            """));
        SynthLookAndFeel.updateStyles(values);
        equal(new Color(0x112233), values.getForeground(), "invalid color does not erase prior rule");
        equal(new Color(4, 5, 6, 128), values.getBackground(), "out-of-range alpha ignored");
        equal(16, values.getFont().getSize(), "unsafe font size ignored");
        equal(new Insets(1, 8, 3, 2), style(values).getInsets(context(values, 1), null),
              "invalid shorthand is atomic and NaN ignored");
        laf.loadCss(new StringReader("#transparent { background-color: transparent; }"));
        JButton transparent = button("transparent", null);
        equal(0, transparent.getBackground().getAlpha(), "transparent color alpha");
        check(!transparent.isOpaque(), "transparent background defaults to nonopaque");
    }

    private static void failuresAreAtomic() throws Exception {
        SynthLookAndFeel laf = theme("Button { color: blue; }");
        JButton button = button(null, null);
        SynthStyle oldStyle = style(button);
        expect(ParseException.class,
               () -> laf.loadCss(new StringReader("Button { color: red; } }")));
        SynthLookAndFeel.updateStyles(button);
        equal(oldStyle, style(button), "parse failure retains exact cached style");
        equal(Color.BLUE, button.getForeground(), "parse failure publishes no partial rule");
        TrackingReader failing = new TrackingReader("Button { color: red; }") {
            public int read() throws IOException {
                int result = super.read();
                if (result < 0) {
                    throw new IOException("deliberate end-of-stream failure");
                }
                return result;
            }
        };
        expect(IOException.class, () -> laf.loadCss(failing));
        SynthLookAndFeel.updateStyles(button);
        equal(Color.BLUE, button.getForeground(), "I/O failure publishes no partial rule");
        check(!failing.closed, "failed reader remains caller-owned");
        expect(IllegalArgumentException.class, () -> laf.loadCss(null));
        TrackingReader valid = new TrackingReader("Button { color: green; }");
        laf.loadCss(valid);
        check(!valid.closed, "successful reader remains caller-owned");
        SynthLookAndFeel.updateStyles(button);
        equal(new Color(0x008000), button.getForeground(), "load after failure recovers");
    }

    private static void lifecycleAndFactories() throws Exception {
        SynthLookAndFeel laf = theme(".hot { color: red; } #renamed { color: blue; }");
        JButton button = button(null, null);
        button.putClientProperty(new String("styleClass"), "hot");
        equal(Color.RED, button.getForeground(), "styleClass event refreshes UI using value equality");
        button.setName("renamed");
        equal(Color.BLUE, button.getForeground(), "name event refreshes CSS style");
        button.setName(null);
        button.putClientProperty("styleClass", null);
        equal(XML_COLOR, button.getForeground(), "removing classes restores original XML");

        SynthStyleFactory originalFactory = SynthLookAndFeel.getStyleFactory();
        SynthStyleFactory custom = new SynthStyleFactory() {
            public SynthStyle getStyle(JComponent c, Region region) {
                return originalFactory.getStyle(c, region);
            }
        };
        SynthLookAndFeel.setStyleFactory(custom);
        laf.loadCss(new StringReader("Button { color: orange; }"));
        check(SynthLookAndFeel.getStyleFactory() == custom, "custom global factory is never replaced");
        SynthLookAndFeel.setStyleFactory(originalFactory);
        theme(null);
        equal(XML_COLOR, button(null, "hot").getForeground(), "CSS belongs to its LAF instance");

        StyleSheet html = new StyleSheet();
        html.addRule("p { color: #123456; font-size: 12pt; }");
        equal(XML_COLOR, html.getForeground(html.getRule("p")),
              "original HTML StyleSheet still uses its parser normally");
    }

    private static void lazyViewports() throws Exception {
        SynthLookAndFeel laf = theme("Table, List { color: #112233; font-size: 12px; }");
        final int rows = 1_000_000_000;
        final int[] reads = {0};
        AbstractTableModel tableModel = new AbstractTableModel() {
            public int getRowCount() { return rows; }
            public int getColumnCount() { return 3; }
            public Object getValueAt(int row, int column) {
                if (++reads[0] > 500) {
                    throw new AssertionError("table read outside bounded viewport work");
                }
                return row + ":" + column;
            }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(1);
        table.setPreferredScrollableViewportSize(new Dimension(180, 40));
        table.setSize(180, rows);
        JViewport viewport = new JViewport();
        viewport.setSize(180, 40);
        viewport.setView(table);
        viewport.setViewPosition(new Point(0, 800_000_000));
        paint(viewport);
        check(reads[0] > 0 && reads[0] <= 150, "billion-row table paint reads only visible cells");
        int afterPaint = reads[0];
        laf.loadCss(new StringReader("Table { color: blue; }"));
        SynthLookAndFeel.updateStyles(viewport);
        equal(afterPaint, reads[0], "theme update does not visit table model rows");
        check(table.getModel() == tableModel, "table model identity retained");
        equal(new Point(0, 800_000_000), viewport.getViewPosition(), "table viewport position retained");

        final int[] listReads = {0};
        AbstractListModel<String> listModel = new AbstractListModel<>() {
            public int getSize() { return rows; }
            public String getElementAt(int row) {
                if (++listReads[0] > 200) {
                    throw new AssertionError("list read outside bounded viewport work");
                }
                return Integer.toString(row);
            }
        };
        JList<String> list = new JList<>(listModel);
        list.setFixedCellHeight(1);
        list.setFixedCellWidth(180);
        list.setSize(180, rows);
        JViewport listViewport = new JViewport();
        listViewport.setSize(180, 40);
        listViewport.setView(list);
        listViewport.setViewPosition(new Point(0, 800_000_000));
        paint(listViewport);
        check(listReads[0] > 0 && listReads[0] <= 50, "billion-row list paint reads only visible cells");
        int afterListPaint = listReads[0];
        laf.loadCss(new StringReader("List { color: blue; }"));
        SynthLookAndFeel.updateStyles(listViewport);
        equal(afterListPaint, listReads[0], "theme update does not visit list model rows");
        check(list.getModel() == listModel, "list model identity retained");
        equal(new Point(0, 800_000_000), listViewport.getViewPosition(), "list viewport position retained");
    }

    private static BufferedImage paint(JComponent component) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(),
                                               BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        try {
            component.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static class TrackingReader extends StringReader {
        boolean closed;
        int reads;
        TrackingReader(String source) { super(source); }
        public int read() throws IOException {
            reads++;
            return super.read();
        }
        public void close() {
            closed = true;
            super.close();
        }
    }

    private interface Checked { void run() throws Exception; }

    private static void expect(Class<? extends Exception> type, Checked action) throws Exception {
        try {
            action.run();
        } catch (Exception ex) {
            check(type.isInstance(ex), "expected " + type + ", received " + ex);
            return;
        }
        throw new AssertionError("Expected " + type);
    }

    private static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual), message + ": expected=" + expected + ", actual=" + actual);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
