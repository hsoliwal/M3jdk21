/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Incremental list measurement preserves application LabelUI sizing.
 * @run main/othervm -Djava.awt.headless=true -Dm3.swing.viewport=true M3ListMeasureCustomLabelUITest
 */

import java.awt.Dimension;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.Icon;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.plaf.basic.BasicListUI;
import javax.swing.plaf.synth.ColorType;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthGraphicsUtils;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.synth.SynthStyle;
import javax.swing.plaf.synth.SynthStyleFactory;

public final class M3ListMeasureCustomLabelUITest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<String> failures = new ArrayList<>();
            runCase("insert", M3ListMeasureCustomLabelUITest::checkInsert, failures);
            runCase("remove", M3ListMeasureCustomLabelUITest::checkRemove, failures);
            runCase("sibling change", M3ListMeasureCustomLabelUITest::checkChangedSibling, failures);
            runCase("delegate transition", M3ListMeasureCustomLabelUITest::checkDefaultDelegateTransition, failures);
            runCase("custom border", () -> checkCustomBorder(false), failures);
            runCase("border replacement", () -> checkCustomBorder(true), failures);
            runCase("border transition", M3ListMeasureCustomLabelUITest::checkBorderTransition, failures);
            runCase("null LabelUI", M3ListMeasureCustomLabelUITest::checkNullLabelUI, failures);
            runCase("Synth style", M3ListMeasureCustomLabelUITest::checkSynthStyle, failures);
            runCase("model-dependent icon", M3ListMeasureCustomLabelUITest::checkModelDependentIcon, failures);
            runCase("model-dependent text", M3ListMeasureCustomLabelUITest::checkModelDependentText, failures);
            runCase("inserted value changes UI", () -> checkValueChangesUI(false), failures);
            runCase("appended value changes UI", () -> checkValueChangesUI(true), failures);
            if (!failures.isEmpty()) {
                throw new AssertionError(failures.toString());
            }
            System.out.println("PASS custom sizing: 13 cases");
        });
    }

    private static void runCase(String name, Runnable action, List<String> failures) {
        try {
            action.run();
            System.out.println("PASS " + name);
        } catch (AssertionError failure) {
            failures.add(name + ": " + failure.getMessage());
            System.out.println("FAIL " + name + ": " + failure.getMessage());
        }
    }

    private static void checkInsert() {
        DefaultListModel<String> model = model("first", "middle", "last");
        JList<String> list = list(model, true);
        checkWidth(list, 30, "before insert");
        model.addElement("added");
        checkWidth(list, 40, "after insert");
    }

    private static void checkRemove() {
        DefaultListModel<String> model = model("first", "middle", "last", "removed");
        JList<String> list = list(model, true);
        checkWidth(list, 40, "before remove");
        model.removeElementAt(3);
        checkWidth(list, 30, "after remove");
    }

    private static void checkChangedSibling() {
        DefaultListModel<String> model = model("first", "xx", "last");
        JList<String> list = list(model, false);
        checkWidth(list, 20, "before sibling change");
        model.set(1, "xxxx");
        checkWidth(list, 40, "after sibling change");
    }

    private static void checkDefaultDelegateTransition() {
        DefaultListModel<String> model = model("first", "middle", "last");
        JList<String> list = list(model, true);
        checkWidth(list, 30, "custom before transition");
        DefaultListCellRenderer renderer = (DefaultListCellRenderer) list.getCellRenderer();
        renderer.setUI(new BasicLabelUI());
        model.addElement("ordinary");
        int width = stockWidth(list, renderer);
        checkWidth(list, width, "standard after custom transition");
        renderer.setUI(new ModelAwareLabelUI(model, true));
        model.addElement("custom");
        checkWidth(list, 50, "custom after standard transition");
    }

    private static void checkCustomBorder(boolean replaceAfterFirstLayout) {
        Object previousBorder = UIManager.get("List.cellNoFocusBorder");
        try {
            DefaultListModel<String> model = model("first", "x", "x");
            Insets baseInsets = new DefaultListCellRenderer().getInsets();
            AbstractBorder border = new AbstractBorder() {
                @Override
                public Insets getBorderInsets(Component component) {
                    int left = "first".equals(((JLabel) component).getText())
                            ? model.getSize() * 10 : 0;
                    return new Insets(baseInsets.top, baseInsets.left + left,
                            baseInsets.bottom, baseInsets.right);
                }
                @Override
                public Insets getBorderInsets(Component component, Insets destination) {
                    Insets value = getBorderInsets(component);
                    destination.set(value.top, value.left, value.bottom, value.right);
                    return destination;
                }
            };
            if (!replaceAfterFirstLayout) {
                UIManager.put("List.cellNoFocusBorder", border);
            }
            JList<String> stock = new JList<>(model);
            stock.setUI(new BasicListUI() { });
            JList<String> candidate = new JList<>(model);
            candidate.setUI(new BasicListUI());
            stock.setCellRenderer(new DefaultListCellRenderer());
            candidate.setCellRenderer(new DefaultListCellRenderer());
            checkWidth(candidate, stock.getPreferredSize().width, "before custom border change");
            if (replaceAfterFirstLayout) {
                UIManager.put("List.cellNoFocusBorder", border);
            }
            model.addElement("x");
            checkWidth(candidate, stock.getPreferredSize().width,
                    replaceAfterFirstLayout ? "after border replacement" : "after custom border model change");
        } finally {
            UIManager.put("List.cellNoFocusBorder", previousBorder);
        }
    }

    private static void checkBorderTransition() {
        Object previousBorder = UIManager.get("List.cellNoFocusBorder");
        try {
            DefaultListModel<String> model = model("first", "x", "x");
            AbstractBorder custom = new AbstractBorder() {
                @Override
                public Insets getBorderInsets(Component component) {
                    return new Insets(0, "first".equals(((JLabel) component).getText())
                            ? model.getSize() * 10 : 0, 0, 0);
                }
                @Override
                public Insets getBorderInsets(Component component, Insets destination) {
                    Insets value = getBorderInsets(component);
                    destination.set(value.top, value.left, value.bottom, value.right);
                    return destination;
                }
            };
            UIManager.put("List.cellNoFocusBorder", custom);
            JList<String> stock = new JList<>(model);
            stock.setUI(new BasicListUI() { });
            JList<String> candidate = new JList<>(model);
            candidate.setUI(new BasicListUI());
            stock.setCellRenderer(new DefaultListCellRenderer());
            candidate.setCellRenderer(new DefaultListCellRenderer());
            checkWidth(candidate, stock.getPreferredSize().width, "initial custom border");
            UIManager.put("List.cellNoFocusBorder", new EmptyBorder(0, 0, 0, 0));
            model.addElement("x");
            checkWidth(candidate, stock.getPreferredSize().width, "custom to empty border");
            model.addElement("x");
            checkWidth(candidate, stock.getPreferredSize().width, "known border delta");
            UIManager.put("List.cellNoFocusBorder", custom);
            model.addElement("x");
            checkWidth(candidate, stock.getPreferredSize().width, "empty to custom border");
        } finally {
            UIManager.put("List.cellNoFocusBorder", previousBorder);
        }
    }

    private static void checkNullLabelUI() {
        DefaultListModel<String> model = model("first", "middle", "last");
        JList<String> stock = new JList<>(model);
        stock.setUI(new BasicListUI() { });
        JList<String> candidate = new JList<>(model);
        candidate.setUI(new BasicListUI());
        DefaultListCellRenderer stockRenderer = new DefaultListCellRenderer();
        DefaultListCellRenderer candidateRenderer = new DefaultListCellRenderer();
        stockRenderer.setUI(null);
        candidateRenderer.setUI(null);
        stock.setCellRenderer(stockRenderer);
        candidate.setCellRenderer(candidateRenderer);
        checkWidth(candidate, stock.getPreferredSize().width, "initial null LabelUI");
        model.addElement("added");
        checkWidth(candidate, stock.getPreferredSize().width, "null LabelUI delta");
        stockRenderer.setUI(new BasicLabelUI());
        candidateRenderer.setUI(new BasicLabelUI());
        model.addElement("ordinary");
        checkWidth(candidate, stock.getPreferredSize().width, "null to standard LabelUI");
    }

    private static void checkSynthStyle() {
        LookAndFeel previousLookAndFeel = UIManager.getLookAndFeel();
        SynthStyleFactory previousFactory = SynthLookAndFeel.getStyleFactory();
        try {
            DefaultListModel<String> model = model("first", "middle", "last");
            SynthGraphicsUtils graphics = new SynthGraphicsUtils() {
                @Override
                public Dimension getPreferredSize(SynthContext context, Font font, String text,
                        Icon icon, int horizontalAlignment, int verticalAlignment,
                        int horizontalTextPosition, int verticalTextPosition, int iconTextGap,
                        int mnemonicIndex) {
                    return new Dimension("first".equals(text) ? 10 * model.getSize() : 10, 20);
                }
            };
            SynthStyle style = new SynthStyle() {
                @Override
                protected Color getColorForState(SynthContext context, ColorType type) {
                    return Color.BLACK;
                }
                @Override
                protected Font getFontForState(SynthContext context) {
                    return new Font(Font.DIALOG, Font.PLAIN, 12);
                }
                @Override
                public SynthGraphicsUtils getGraphicsUtils(SynthContext context) {
                    return graphics;
                }
            };
            UIManager.setLookAndFeel(new SynthLookAndFeel());
            SynthLookAndFeel.setStyleFactory(new SynthStyleFactory() {
                @Override
                public SynthStyle getStyle(JComponent component, Region region) {
                    return style;
                }
            });
            JList<String> list = new JList<>(model);
            list.setUI(new BasicListUI());
            list.setCellRenderer(new DefaultListCellRenderer());
            checkWidth(list, 30, "initial Synth style");
            model.addElement("added");
            checkWidth(list, 40, "Synth style delta");
        } catch (javax.swing.UnsupportedLookAndFeelException failure) {
            throw new AssertionError("Synth test setup failed", failure);
        } finally {
            try {
                UIManager.setLookAndFeel(previousLookAndFeel);
            } catch (javax.swing.UnsupportedLookAndFeelException failure) {
                throw new AssertionError("Look and feel restore failed", failure);
            }
            SynthLookAndFeel.setStyleFactory(previousFactory);
        }
    }

    private static void checkModelDependentIcon() {
        DefaultListModel<Object> model = new DefaultListModel<>();
        Icon icon = new Icon() {
            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) { }
            @Override
            public int getIconWidth() {
                return model.getSize() * 100;
            }
            @Override
            public int getIconHeight() {
                return 20;
            }
        };
        model.addElement(icon);
        model.addElement("x");
        model.addElement("x");
        checkUnknownValueChanges(model, "model-dependent icon");
    }

    private static void checkModelDependentText() {
        DefaultListModel<Object> model = new DefaultListModel<>();
        Object value = new Object() {
            @Override
            public String toString() {
                return "wide".repeat(model.getSize());
            }
        };
        model.addElement(value);
        model.addElement("x");
        model.addElement("x");
        checkUnknownValueChanges(model, "model-dependent text");
    }

    private static void checkUnknownValueChanges(DefaultListModel<Object> model, String context) {
        Object dynamicValue = model.getElementAt(0);
        JList<Object> stock = new JList<>(model);
        stock.setUI(new BasicListUI() { });
        JList<Object> candidate = new JList<>(model);
        candidate.setUI(new BasicListUI());
        stock.setCellRenderer(new DefaultListCellRenderer());
        candidate.setCellRenderer(new DefaultListCellRenderer());
        checkWidth(candidate, stock.getPreferredSize().width, "initial " + context);
        model.addElement("x");
        checkWidth(candidate, stock.getPreferredSize().width, "insert " + context);
        model.removeElementAt(model.getSize() - 1);
        checkWidth(candidate, stock.getPreferredSize().width, "remove " + context);
        model.set(0, "fixed");
        checkWidth(candidate, stock.getPreferredSize().width, "unknown to string " + context);
        model.addElement("ordinary");
        checkWidth(candidate, stock.getPreferredSize().width, "known value delta " + context);
        model.add(0, dynamicValue);
        checkWidth(candidate, stock.getPreferredSize().width, "insert dynamic value " + context);
        model.addElement("next delta");
        checkWidth(candidate, stock.getPreferredSize().width, "delta after dynamic insertion " + context);
    }

    private static void checkValueChangesUI(boolean appendOnly) {
        DefaultListModel<Object> model = new DefaultListModel<>();
        model.addElement("first");
        model.addElement("last");
        JList<Object> list = new JList<>(model);
        list.setUI(new BasicListUI());
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setUI(new BasicLabelUI());
        list.setCellRenderer(renderer);
        int originalWidth = list.getPreferredSize().width;
        BasicLabelUI changedUI = new BasicLabelUI() {
            @Override
            public Dimension getPreferredSize(JComponent component) {
                return new Dimension("first".equals(((JLabel) component).getText()) ? 100 : 10, 20);
            }
        };
        Object configuringValue = new Object() {
            @Override
            public String toString() {
                renderer.setUI(changedUI);
                return "inserted";
            }
        };
        if (appendOnly) {
            // Stock measures earlier rows before the new value changes the delegate.
            model.addElement(configuringValue);
        } else {
            // Both edits precede layout. Stock visits the configuring value first;
            // incremental measurement probes the last retained row before new values.
            model.add(0, configuringValue);
            model.addElement("appended");
        }
        checkWidth(list, appendOnly ? originalWidth : 100,
                appendOnly ? "appended value preserves earlier measurement order"
                        : "new value reconfigures retained row sizing");
    }

    private static DefaultListModel<String> model(String... values) {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String value : values) {
            model.addElement(value);
        }
        return model;
    }

    private static JList<String> list(DefaultListModel<String> model, boolean countBased) {
        JList<String> list = new JList<>(model);
        list.setUI(new BasicListUI());
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setUI(new ModelAwareLabelUI(model, countBased));
        list.setCellRenderer(renderer);
        return list;
    }

    private static int stockWidth(JList<String> list, DefaultListCellRenderer renderer) {
        int width = 0;
        for (int i = 0; i < list.getModel().getSize(); i++) {
            width = Math.max(width, renderer.getListCellRendererComponent(list,
                    list.getModel().getElementAt(i), i, false, false).getPreferredSize().width);
        }
        return width;
    }

    private static void checkWidth(JList<?> list, int expected, String context) {
        int actual = list.getPreferredSize().width;
        if (actual != expected) {
            throw new AssertionError(context + ": expected width " + expected + ", actual " + actual);
        }
    }

    private static final class ModelAwareLabelUI extends BasicLabelUI {
        private final DefaultListModel<String> model;
        private final boolean countBased;

        ModelAwareLabelUI(DefaultListModel<String> model, boolean countBased) {
            this.model = model;
            this.countBased = countBased;
        }

        @Override
        public Dimension getPreferredSize(JComponent component) {
            int width = 10;
            if ("first".equals(((JLabel) component).getText())) {
                width = 10 * (countBased ? model.getSize() : model.getElementAt(1).length());
            }
            return new Dimension(width, 20);
        }
    }
}
