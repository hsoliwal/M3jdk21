/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

/*
 * @test
 * @summary Ordered range slider model, keyboard, pointer, accessibility and painting contracts
 * @run main/othervm -Djava.awt.headless=true RangeSliderTest
 */

import java.awt.ComponentOrientation;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import javax.swing.AbstractAction;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JRangeSlider;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicSliderUI;

public class RangeSliderTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                models();
                keyboard();
                pointer();
                accessibility();
                painting();
                lookAndFeelAndSerialization();
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        });
        System.out.println("RangeSliderTest: " + checks + " checks passed");
    }

    private static void models() {
        JRangeSlider slider = new JRangeSlider();
        interval(slider, 25, 75);
        List<Boolean> adjusting = new ArrayList<>();
        slider.addChangeListener(event -> {
            check(event.getSource() == slider, "component change event source");
            adjusting.add(slider.getValueIsAdjusting());
        });
        slider.setRange(10, 90);
        check(adjusting.size() == 1, "atomic interval change");
        slider.setValue(30);
        interval(slider, 30, 90);
        slider.setUpperValue(60);
        interval(slider, 30, 60);
        slider.setLowerValue(100);
        interval(slider, 60, 60);
        slider.setUpperValue(-1);
        interval(slider, 60, 60);
        slider.setRange(20, 40);
        slider.shiftRange(90);
        interval(slider, 80, 100);
        slider.shiftRange(-200);
        interval(slider, 0, 20);
        reject(() -> slider.setRange(30, 20));
        reject(() -> slider.setRange(-1, 20));
        reject(() -> slider.setRange(0, 101));
        interval(slider, 0, 20);

        JRangeSlider wide = new JRangeSlider(Integer.MIN_VALUE, Integer.MAX_VALUE, -100, 100);
        wide.setLowerValue(Integer.MIN_VALUE);
        interval(wide, 100 - Integer.MAX_VALUE, 100);
        wide.setRange(Integer.MIN_VALUE, -1);
        wide.shiftRange(Integer.MAX_VALUE);
        interval(wide, -1, Integer.MAX_VALUE - 1);
        reject(() -> wide.setRange(Integer.MIN_VALUE, Integer.MAX_VALUE));
        interval(wide, -1, Integer.MAX_VALUE - 1);
        reject(() -> new JRangeSlider(5, 4, 4, 4));
        reject(() -> new JRangeSlider(7, 0, 100, 20, 40));
        reject(() -> new JRangeSlider(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, 0));

        DefaultBoundedRangeModel old = (DefaultBoundedRangeModel) slider.getModel();
        DefaultBoundedRangeModel replacement = new DefaultBoundedRangeModel(5, 15, 0, 50);
        slider.setModel(replacement);
        check(slider.getModel() == replacement, "caller model identity");
        check(old.getChangeListeners().length == 0, "old model listeners detached");
        interval(slider, 5, 20);
        replacement.setRangeProperties(7, 22, 0, 50, true);
        interval(slider, 7, 29);
        check(slider.getValueIsAdjusting(), "external model adjusting flag");
        int count = adjusting.size();
        old.setValue(3);
        check(adjusting.size() == count, "detached model does not notify");

        Random random = new Random(180611L);
        JRangeSlider tested = new JRangeSlider(-500, 500, -100, 100);
        int low = -100;
        int high = 100;
        for (int i = 0; i < 3000; i++) {
            int value = random.nextInt(1601) - 800;
            switch (random.nextInt(3)) {
                case 0 -> { low = Math.max(-500, Math.min(high, value)); tested.setLowerValue(value); }
                case 1 -> { high = Math.max(low, Math.min(500, value)); tested.setUpperValue(value); }
                default -> {
                    int delta = Math.max(-500 - low, Math.min(500 - high, value));
                    low += delta;
                    high += delta;
                    tested.shiftRange(value);
                }
            }
            interval(tested, low, high);
        }
    }

    private static void keyboard() {
        JRangeSlider slider = new JRangeSlider();
        action(slider, "positiveUnitIncrement");
        interval(slider, 26, 75);
        action(slider, "selectOtherThumb");
        action(slider, "positiveUnitIncrement");
        interval(slider, 26, 76);
        action(slider, "negativeBlockIncrement");
        interval(slider, 26, 66);
        action(slider, "maxScroll");
        interval(slider, 26, 100);
        action(slider, "minScroll");
        interval(slider, 26, 26);
        slider.setRange(20, 60);
        slider.setInverted(true);
        action(slider, "positiveUnitIncrement");
        interval(slider, 20, 59);
        action(slider, "maxScroll");
        interval(slider, 20, 20);
        slider.setInverted(false);
        slider.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        action(slider, "selectOtherThumb");
        check(!slider.isUpperThumbSelected(), "thumb action survives RTL change");
        slider.setRange(20, 60);
        slider.setMinorTickSpacing(5);
        slider.setSnapToTicks(true);
        action(slider, "positiveUnitIncrement");
        interval(slider, 25, 60);
        slider.setEnabled(false);
        action(slider, "positiveUnitIncrement");
        action(slider, "maxScroll");
        action(slider, "selectOtherThumb");
        interval(slider, 25, 60);
        check(!slider.isUpperThumbSelected(), "disabled thumb selection unchanged");

        JRangeSlider wide = new JRangeSlider(Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        wide.setUpperThumbSelected(true);
        action(wide, "positiveUnitIncrement");
        interval(wide, Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        action(wide, "positiveBlockIncrement");
        interval(wide, Integer.MAX_VALUE - 1, Integer.MAX_VALUE);

        JRangeSlider coarse = new JRangeSlider(0, 10, 0, 10);
        coarse.setMinorTickSpacing(10);
        coarse.setSnapToTicks(true);
        action(coarse, "positiveBlockIncrement");
        interval(coarse, 10, 10);
    }

    private static void pointer() {
        for (int orientation : new int[] {JSlider.HORIZONTAL, JSlider.VERTICAL}) {
            for (boolean inverted : new boolean[] {false, true}) {
                for (boolean rtl : new boolean[] {false, true}) {
                    JRangeSlider slider = new JRangeSlider(orientation, 0, 100, 25, 75);
                    slider.setInverted(inverted);
                    slider.setComponentOrientation(rtl ? ComponentOrientation.RIGHT_TO_LEFT
                            : ComponentOrientation.LEFT_TO_RIGHT);
                    slider.setSize(orientation == JSlider.HORIZONTAL ? 360 : 48,
                            orientation == JSlider.HORIZONTAL ? 48 : 360);
                    paint(slider, null);
                    List<Boolean> states = new ArrayList<>();
                    slider.addChangeListener(event -> states.add(slider.getValueIsAdjusting()));
                    drag(slider, 25, 40, 0);
                    interval(slider, 40, 75);
                    check(states.contains(true) && !states.get(states.size() - 1),
                            "drag adjusting then final");
                    drag(slider, 75, 60, 0);
                    interval(slider, 40, 60);
                    drag(slider, 50, 70, InputEvent.CTRL_DOWN_MASK);
                    interval(slider, 60, 80);
                    drag(slider, 80, 40, 0);
                    interval(slider, 60, 60);
                    slider.setRange(20, 80);
                    Point start = pointFor(slider, 20);
                    send(slider, MouseEvent.MOUSE_PRESSED, start, InputEvent.BUTTON1_DOWN_MASK,
                            MouseEvent.BUTTON1);
                    check(slider.getValueIsAdjusting(), "press starts adjustment");
                    slider.setEnabled(false);
                    check(!slider.getValueIsAdjusting(), "disable completes adjustment");
                    send(slider, MouseEvent.MOUSE_DRAGGED, pointFor(slider, 40),
                            InputEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON);
                    interval(slider, 20, 80);
                    slider.setEnabled(true);
                    send(slider, MouseEvent.MOUSE_RELEASED, start, 0, MouseEvent.BUTTON1);
                    send(slider, MouseEvent.MOUSE_PRESSED, start, InputEvent.BUTTON3_DOWN_MASK,
                            MouseEvent.BUTTON3);
                    check(!slider.getValueIsAdjusting(), "secondary press ignored");
                }
            }
        }

        JRangeSlider replaced = new JRangeSlider();
        replaced.setSize(360, 48);
        paint(replaced, null);
        DefaultBoundedRangeModel original = (DefaultBoundedRangeModel) replaced.getModel();
        send(replaced, MouseEvent.MOUSE_PRESSED, pointFor(replaced, 25),
                InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        replaced.setModel(new DefaultBoundedRangeModel(10, 10, 0, 100));
        check(!original.getValueIsAdjusting(), "model replacement releases old adjustment");
        send(replaced, MouseEvent.MOUSE_DRAGGED, pointFor(replaced, 80),
                InputEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON);
        interval(replaced, 10, 20);

        JRangeSlider reentrant = new JRangeSlider();
        reentrant.setSize(360, 48);
        paint(reentrant, null);
        reentrant.addChangeListener(event -> {
            if (reentrant.getValueIsAdjusting()) { reentrant.updateUI(); }
        });
        send(reentrant, MouseEvent.MOUSE_PRESSED, pointFor(reentrant, 25),
                InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        check(!reentrant.getValueIsAdjusting(), "UI replacement during callback cancels drag");

        JRangeSlider selectionReentrant = new JRangeSlider();
        selectionReentrant.setSize(360, 48);
        paint(selectionReentrant, null);
        selectionReentrant.addPropertyChangeListener("upperThumbSelected", event -> selectionReentrant.updateUI());
        send(selectionReentrant, MouseEvent.MOUSE_PRESSED, pointFor(selectionReentrant, 75),
                InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        check(!selectionReentrant.getValueIsAdjusting(), "thumb selection UI replacement cancels pending drag");
        interval(selectionReentrant, 25, 75);

        JRangeSlider bounded = new JRangeSlider(0, 100, 20, 80);
        bounded.setSize(360, 48);
        paint(bounded, null);
        send(bounded, MouseEvent.MOUSE_PRESSED, pointFor(bounded, 50),
                InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, MouseEvent.BUTTON1);
        bounded.setMaximum(50);
        int lower = bounded.getLowerValue();
        int upper = bounded.getUpperValue();
        send(bounded, MouseEvent.MOUSE_DRAGGED, new Point(300, 23),
                InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, MouseEvent.NOBUTTON);
        check(!bounded.getValueIsAdjusting(), "changed model bounds cancel interval drag");
        interval(bounded, lower, upper);

        JRangeSlider cancelReentrant = new JRangeSlider();
        cancelReentrant.setSize(360, 48);
        paint(cancelReentrant, null);
        int listenerCount = cancelReentrant.getMouseListeners().length;
        send(cancelReentrant, MouseEvent.MOUSE_PRESSED, pointFor(cancelReentrant, 25),
                InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        cancelReentrant.addChangeListener(event -> {
            if (!cancelReentrant.getValueIsAdjusting()) { cancelReentrant.updateUI(); }
        });
        cancelReentrant.updateUI();
        check(!cancelReentrant.getValueIsAdjusting(), "reentrant UI teardown finishes adjusting");
        check(cancelReentrant.getMouseListeners().length == listenerCount,
                "reentrant UI teardown retains one listener set");
        action(cancelReentrant, "positiveUnitIncrement");
        interval(cancelReentrant, 26, 75);

        JRangeSlider snapped = new JRangeSlider(0, 100, 20, 60);
        snapped.setMinorTickSpacing(10);
        snapped.setSnapToTicks(true);
        snapped.setSize(360, 48);
        paint(snapped, null);
        drag(snapped, 40, 43, InputEvent.CTRL_DOWN_MASK);
        interval(snapped, 20, 60);
        drag(snapped, 40, 48, InputEvent.CTRL_DOWN_MASK);
        interval(snapped, 30, 70);
        drag(snapped, 50, 42, InputEvent.CTRL_DOWN_MASK);
        interval(snapped, 20, 60);
    }

    private static void accessibility() {
        JRangeSlider slider = new JRangeSlider();
        AccessibleContext context = slider.getAccessibleContext();
        check(context.getAccessibleRole() == AccessibleRole.SLIDER, "accessible role");
        check(context.getAccessibleChildrenCount() == 2, "both endpoints accessible");
        check(context.getAccessibleChild(-1) == null && context.getAccessibleChild(2) == null,
                "accessible bounds");
        AccessibleContext low = context.getAccessibleChild(0).getAccessibleContext();
        AccessibleContext high = context.getAccessibleChild(1).getAccessibleContext();
        check(high.getAccessibleParent() == slider, "endpoint accessible parent");
        List<Object> changes = new ArrayList<>();
        high.addPropertyChangeListener(event -> {
            if (AccessibleContext.ACCESSIBLE_VALUE_PROPERTY.equals(event.getPropertyName())) {
                changes.add(event.getNewValue());
            }
        });
        AccessibleValue upper = high.getAccessibleValue();
        check(upper.setCurrentAccessibleValue(90), "upper accessible mutation");
        interval(slider, 25, 90);
        check(changes.size() == 1, "upper accessible change notification");
        check(low.getAccessibleValue().setCurrentAccessibleValue(40), "lower accessible mutation");
        interval(slider, 40, 90);
        check(high.getAccessibleAction().doAccessibleAction(1), "upper accessible decrement");
        interval(slider, 40, 89);
        slider.setFocusable(false);
        check(!low.getAccessibleStateSet().contains(javax.accessibility.AccessibleState.FOCUSABLE),
                "accessible focusability follows component");
        slider.setEnabled(true);
        List<String> lowEvents = new ArrayList<>();
        List<String> highEvents = new ArrayList<>();
        low.addPropertyChangeListener(event -> {
            if (AccessibleContext.ACCESSIBLE_VALUE_PROPERTY.equals(event.getPropertyName())) {
                lowEvents.add(event.getOldValue() + "->" + event.getNewValue());
            }
        });
        high.addPropertyChangeListener(event -> {
            if (AccessibleContext.ACCESSIBLE_VALUE_PROPERTY.equals(event.getPropertyName())) {
                highEvents.add(event.getOldValue() + "->" + event.getNewValue());
            }
        });
        slider.setModel(new DefaultBoundedRangeModel(10, 10, 0, 100));
        check(lowEvents.equals(List.of("40->10")), "model replacement publishes lower change");
        check(highEvents.equals(List.of("89->20")), "model replacement publishes upper change");
        lowEvents.clear();
        highEvents.clear();
        slider.setUpperValue(21);
        check(lowEvents.isEmpty(), "upper change does not publish stale lower value");
        check(highEvents.equals(List.of("20->21")), "upper cache follows new model");
        check(!upper.setCurrentAccessibleValue(null), "null accessible input refused");
        slider.setEnabled(false);
        check(!upper.setCurrentAccessibleValue(99), "disabled accessible input refused");
        check(!high.getAccessibleAction().doAccessibleAction(0), "disabled accessible action refused");
        interval(slider, 10, 21);
    }

    private static void painting() {
        JRangeSlider slider = new JRangeSlider();
        slider.setSize(360, 48);
        BufferedImage full = paint(slider, null);
        Point high = pointFor(slider, 75);
        Rectangle clip = new Rectangle(high.x - 9, 0, 19, slider.getHeight());
        BufferedImage partial = paint(slider, clip);
        for (int y = clip.y; y < clip.y + clip.height; y++) {
            for (int x = clip.x; x < clip.x + clip.width; x++) {
                check(full.getRGB(x, y) == partial.getRGB(x, y), "upper thumb paints with lower excluded");
            }
        }
        slider.setSize(0, 0);
        paint(slider, null);
        slider.setSize(1, 1);
        paint(slider, null);
        JRangeSlider wide = new JRangeSlider(Integer.MIN_VALUE, Integer.MAX_VALUE, -50, 50);
        wide.setSize(360, 48);
        paint(wide, null);
        BasicSliderUI ui = (BasicSliderUI) wide.getUI();
        check(ui.valueForXPosition(0) == Integer.MIN_VALUE, "full-range left projection");
        check(ui.valueForXPosition(359) == Integer.MAX_VALUE, "full-range right projection");
        check(Math.abs((long) ui.valueForXPosition(180)) < 50_000_000L,
                "full-range middle projection does not overflow");
        JRangeSlider singleton = new JRangeSlider(4, 4, 4, 4);
        singleton.setSize(100, 32);
        paint(singleton, null);
        interval(singleton, 4, 4);
    }

    private static void lookAndFeelAndSerialization() throws Exception {
        String initial = UIManager.getLookAndFeel().getClass().getName();
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                try { UIManager.setLookAndFeel(info.getClassName()); }
                catch (javax.swing.UnsupportedLookAndFeelException unavailable) { continue; }
                JRangeSlider slider = new JRangeSlider();
                slider.setSize(360, 48);
                int listeners = slider.getMouseListeners().length;
                for (int i = 0; i < 5; i++) { slider.updateUI(); }
                check(slider.getMouseListeners().length == listeners, "UI reinstall does not leak mouse listeners");
                int[] calls = {0};
                slider.getActionMap().put("applicationAction", new AbstractAction() {
                    @Override public void actionPerformed(ActionEvent event) { calls[0]++; }
                });
                slider.updateUI();
                action(slider, "applicationAction");
                check(calls[0] == 1, "application action retained");
                slider.setPaintTicks(true);
                slider.setMajorTickSpacing(25);
                slider.setPaintLabels(true);
                paint(slider, null);
                action(slider, "selectOtherThumb");
                action(slider, "positiveUnitIncrement");
                interval(slider, 25, 76);
            }
        } finally { UIManager.setLookAndFeel(initial); }

        JRangeSlider slider = new JRangeSlider(-50, 250, 25, 200);
        slider.setUpperThumbSelected(true);
        slider.getAccessibleContext().getAccessibleChild(1).getAccessibleContext().setAccessibleName("Finish");
        javax.swing.Action oldThumbAction = slider.getActionMap().get("selectOtherThumb");
        javax.swing.Action oldEndAction = slider.getActionMap().get("maxScroll");
        slider.updateUI();
        oldThumbAction.actionPerformed(new ActionEvent(slider, 0, "selectOtherThumb"));
        check(!slider.isUpperThumbSelected(), "retained UI action uses event component");
        slider.setUpperThumbSelected(true);
        oldEndAction.actionPerformed(new ActionEvent(slider, 0, "maxScroll"));
        interval(slider, 25, 250);
        slider.setRange(25, 200);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(slider); }
        JRangeSlider restored;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (JRangeSlider) input.readObject();
        }
        interval(restored, 25, 200);
        check(restored.isUpperThumbSelected(), "serialized active thumb");
        restored.setSize(360, 48);
        paint(restored, null);
        action(restored, "positiveUnitIncrement");
        interval(restored, 25, 201);
        check(restored.getAccessibleContext().getAccessibleChild(1).getAccessibleContext()
                .getAccessibleValue().getCurrentAccessibleValue().intValue() == 201,
                "restored accessible upper value");
        check("Finish".equals(restored.getAccessibleContext().getAccessibleChild(1)
                .getAccessibleContext().getAccessibleName()), "restored endpoint accessible name");
        check(restored.getAccessibleContext().getAccessibleChild(1).getAccessibleContext()
                .getAccessibleParent() == restored, "restored endpoint parent identity");
    }

    private static void drag(JRangeSlider slider, int from, int to, int modifiers) {
        paint(slider, null);
        send(slider, MouseEvent.MOUSE_PRESSED, pointFor(slider, from),
                modifiers | InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        send(slider, MouseEvent.MOUSE_DRAGGED, pointFor(slider, to),
                modifiers | InputEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON);
        send(slider, MouseEvent.MOUSE_RELEASED, pointFor(slider, to), modifiers, MouseEvent.BUTTON1);
    }

    private static Point pointFor(JRangeSlider slider, int value) {
        BasicSliderUI ui = (BasicSliderUI) slider.getUI();
        boolean horizontal = slider.getOrientation() == JSlider.HORIZONTAL;
        int length = horizontal ? slider.getWidth() : slider.getHeight();
        int best = 0;
        long distance = Long.MAX_VALUE;
        for (int p = 0; p < length; p++) {
            int candidate = horizontal ? ui.valueForXPosition(p) : ui.valueForYPosition(p);
            long d = Math.abs((long) candidate - value);
            if (d < distance) { best = p; distance = d; }
        }
        return horizontal ? new Point(best, slider.getHeight() / 2 - 1)
                : new Point(slider.getWidth() / 2 - 1, best);
    }

    private static void send(JRangeSlider slider, int id, Point point, int modifiers, int button) {
        slider.dispatchEvent(new MouseEvent(slider, id, 1L, modifiers, point.x, point.y, 1, false, button));
    }

    private static BufferedImage paint(JRangeSlider slider, Rectangle clip) {
        BufferedImage image = new BufferedImage(Math.max(1, slider.getWidth()),
                Math.max(1, slider.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            if (clip != null) { graphics.setClip(clip); }
            slider.paint(graphics);
        } finally { graphics.dispose(); }
        return image;
    }

    private static void action(JRangeSlider slider, String name) {
        check(slider.getActionMap().get(name) != null, "action installed: " + name);
        slider.getActionMap().get(name).actionPerformed(new ActionEvent(slider, 0, name));
    }

    private static void interval(JRangeSlider slider, int lower, int upper) {
        check(slider.getLowerValue() == lower && slider.getUpperValue() == upper,
                "expected [" + lower + "," + upper + "] but got ["
                        + slider.getLowerValue() + "," + slider.getUpperValue() + "]");
        check(slider.getValue() == lower && (long) slider.getExtent() == (long) upper - lower,
                "single model value/extent ownership");
    }

    private static void reject(Runnable action) {
        try { action.run(); throw new AssertionError("expected IllegalArgumentException"); }
        catch (IllegalArgumentException expected) { checks++; }
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) { throw new AssertionError(message); }
    }
}
