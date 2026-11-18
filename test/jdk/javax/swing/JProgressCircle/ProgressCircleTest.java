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
 * @summary Circular progress reuses Swing models, painting and animation lifecycle.
 * @modules java.desktop/javax.swing.plaf.basic:open
 * @run main/othervm -Djava.awt.headless=true ProgressCircleTest
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import javax.swing.BorderFactory;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JProgressBar;
import javax.swing.JProgressCircle;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.ProgressBarUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

public class ProgressCircleTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                defaultsAndModel();
                rangesAndEvents();
                properties();
                painting();
                dimensionsAndInsets();
                uiLifecycle();
                serialization();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        System.out.println("ProgressCircleTest: " + checks + " checks passed");
    }

    private static void defaultsAndModel() {
        JProgressCircle circle = new JProgressCircle();
        check(circle instanceof JProgressBar, "original Swing progress owner");
        check(circle.getMinimum() == 0 && circle.getMaximum() == 100, "default range");
        check(circle.getValue() == 0 && circle.getPercentComplete() == 0, "empty default");
        check(circle.getRingThickness() == 6.0f, "default thickness");
        check(circle.isClockwise() && !circle.isCountdown(), "default direction");
        check(!circle.isOpaque(), "transparent center by default");
        check("ProgressBarUI".equals(circle.getUIClassID()), "inherited UI identity");
        check(circle.getAccessibleContext().getAccessibleRole() == AccessibleRole.PROGRESS_BAR,
              "inherited accessible role");

        DefaultBoundedRangeModel model = new DefaultBoundedRangeModel(25, 0, -100, 100);
        JProgressCircle supplied = new JProgressCircle(model);
        check(supplied.getModel() == model, "caller model identity");
        close(supplied.getPercentComplete(), 0.625, "supplied model fraction");
        AccessibleValue accessible = supplied.getAccessibleContext().getAccessibleValue();
        check(accessible.getCurrentAccessibleValue().intValue() == 25, "accessible value");
        check(accessible.getMinimumAccessibleValue().intValue() == -100, "accessible minimum");
        check(accessible.getMaximumAccessibleValue().intValue() == 100, "accessible maximum");
        check(accessible.setCurrentAccessibleValue(50), "accessible update accepted");
        check(model.getValue() == 50, "accessible update uses original model");
        JProgressCircle unset = new JProgressCircle(null);
        check(unset.getModel() == null && unset.getPercentComplete() == 0,
              "null constructor model retains original ownership");
        expectIllegal(() -> new JProgressCircle(10, 0));
    }

    private static void rangesAndEvents() {
        CountingCircle circle = new CountingCircle(Integer.MIN_VALUE, Integer.MAX_VALUE);
        check(circle.getPercentComplete() == 0, "full range start");
        AtomicInteger events = new AtomicInteger();
        circle.addChangeListener(event -> {
            check(event.getSource() == circle, "Swing event source");
            events.incrementAndGet();
        });
        circle.setValue(0);
        close(circle.getPercentComplete(), 2147483648.0 / 4294967295.0, "full range middle");
        check(events.get() == 1, "one model notification");
        circle.repaints = 0;
        circle.setValue(1);
        check(circle.repaints > 0, "small full-range model change repaints");
        check(events.get() == 2, "one more model notification");
        circle.setValue(Integer.MAX_VALUE);
        check(circle.getPercentComplete() == 1, "full range end");
        circle.setStringPainted(true);
        check(circle.getString().contains("100"), "inherited default string");

        DefaultBoundedRangeModel empty = new DefaultBoundedRangeModel(7, 0, 7, 7);
        circle.setModel(empty);
        check(circle.getModel() == empty, "model replacement identity");
        check(circle.getPercentComplete() == 0, "empty range is finite");
        JProgressCircle ordinary = new JProgressCircle(-20, 80);
        ordinary.setValue(30);
        close(ordinary.getPercentComplete(), 0.5, "negative minimum");
        ordinary.setCountdown(true);
        close(ordinary.getPercentComplete(), 0.5, "countdown preserves completed fraction");
    }

    private static void properties() {
        JProgressCircle circle = new JProgressCircle();
        AtomicInteger properties = new AtomicInteger();
        circle.addPropertyChangeListener(event -> {
            if (event.getPropertyName().equals("ringThickness")
                    || event.getPropertyName().equals("clockwise")
                    || event.getPropertyName().equals("countdown")) {
                properties.incrementAndGet();
            }
        });
        circle.setRingThickness(10);
        circle.setClockwise(false);
        circle.setCountdown(true);
        check(properties.get() == 3, "bound property changes");
        circle.setRingThickness(10);
        circle.setClockwise(false);
        circle.setCountdown(true);
        check(properties.get() == 3, "identical property values are quiet");
        for (float invalid : new float[] {0, -1, Float.NaN,
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}) {
            expectIllegal(() -> circle.setRingThickness(invalid));
        }
        check(circle.getRingThickness() == 10 && properties.get() == 3,
              "invalid thickness is failure atomic");
        circle.setRingThickness(Float.MAX_VALUE);
        paint(circle, 1);
        check(circle.getPreferredSize().width > 0 && circle.getMinimumSize().height > 0,
              "large thickness sizes do not wrap");
    }

    private static void painting() {
        JProgressCircle circle = styledCircle();
        circle.setValue(0);
        BufferedImage image = paint(circle, 1);
        pixel(image, 82, 18, Color.LIGHT_GRAY, "zero value track");
        check(image.getRGB(50, 50) == 0, "transparent center");

        circle.setValue(25);
        image = paint(circle, 1);
        pixel(image, 82, 18, Color.RED, "clockwise upper right");
        pixel(image, 18, 18, Color.LIGHT_GRAY, "unfilled upper left");
        circle.setClockwise(false);
        image = paint(circle, 1);
        pixel(image, 18, 18, Color.RED, "counterclockwise upper left");
        pixel(image, 82, 18, Color.LIGHT_GRAY, "counterclockwise upper right track");

        circle.setClockwise(true);
        circle.setCountdown(true);
        image = paint(circle, 1);
        pixel(image, 18, 82, Color.RED, "remaining fraction lower left");
        pixel(image, 18, 18, Color.LIGHT_GRAY, "remaining fraction upper left track");
        circle.setCountdown(false);
        circle.setValue(100);
        image = paint(circle, 1);
        pixel(image, 18, 18, Color.RED, "complete circle");
        pixel(image, 82, 82, Color.RED, "complete circle opposite side");

        circle.setValue(50);
        circle.setString("READY");
        circle.setStringPainted(true);
        image = paint(circle, 1);
        check(nontransparent(image, 25, 35, 75, 65) > 0, "inherited progress string painted");
        circle.setStringPainted(false);
        image = paint(circle, 2);
        pixel(image, 164, 36, Color.RED, "scaled logical-pixel ring");
        check(image.getRGB(100, 100) == 0, "scaled center");

        Object previous = UIManager.get("ProgressBar.disabledForeground");
        try {
            UIManager.put("ProgressBar.disabledForeground", Color.BLUE);
            circle.setEnabled(false);
            image = paint(circle, 1);
            pixel(image, 82, 18, Color.BLUE, "disabled indicator color");
        } finally {
            UIManager.put("ProgressBar.disabledForeground", previous);
        }
        circle.setEnabled(true);
        circle.setOrientation(JProgressBar.VERTICAL);
        image = paint(circle, 1);
        pixel(image, 82, 18, Color.RED, "orientation does not turn ring into bar");

        BufferedImage canvas = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.translate(3, 4);
            g.scale(1.25, 1.25);
            g.setClip(0, 0, 90, 90);
            g.setColor(Color.GREEN);
            g.setStroke(new BasicStroke(3));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_OFF);
            AffineTransform transform = g.getTransform();
            Rectangle clip = g.getClipBounds();
            circle.getUI().paint(g, circle);
            check(transform.equals(g.getTransform()), "caller transform preserved");
            check(clip.equals(g.getClipBounds()), "caller clipping preserved");
            check(Color.GREEN.equals(g.getColor()), "caller color preserved");
            check(new BasicStroke(3).equals(g.getStroke()), "caller stroke preserved");
            check(g.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
                    == RenderingHints.VALUE_ANTIALIAS_OFF, "caller hints preserved");
        } finally {
            g.dispose();
        }
    }

    private static void dimensionsAndInsets() {
        JProgressCircle circle = styledCircle();
        circle.setBorder(BorderFactory.createEmptyBorder(7, 11, 13, 17));
        circle.setSize(120, 90);
        circle.setValue(100);
        BufferedImage image = paint(circle, 1);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != 0) {
                    check(x >= 11 && x < 103 && y >= 7 && y < 77,
                          "insets bound painting at " + x + "," + y);
                }
            }
        }
        Dimension preferred = circle.getPreferredSize();
        check(preferred.width - 28 == preferred.height - 20, "square preferred content");
        Dimension minimum = circle.getMinimumSize();
        check(minimum.width > 28 && minimum.height > 20, "positive minimum content");
        check(circle.getMaximumSize().width >= preferred.width, "resizable maximum");
        circle.setStringPainted(true);
        circle.setString("A longer progress status");
        check(circle.getPreferredSize().width > preferred.width, "string affects preferred size");
        check(circle.getBaseline(200, 100) > 0, "text baseline");
        circle.setSize(1, 1);
        paint(circle, 1);
        circle.setSize(0, 0);
        paint(circle, 1);
        circle.setBorder(BorderFactory.createEmptyBorder());
        circle.setSize(2, 3);
        circle.setStringPainted(false);
        paint(circle, 1);
        check(true, "tiny and empty layouts paint safely");
    }

    private static void uiLifecycle() throws Exception {
        JProgressCircle circle = styledCircle();
        DefaultBoundedRangeModel model = (DefaultBoundedRangeModel) circle.getModel();
        int modelListeners = model.getChangeListeners().length;
        int changeListeners = circle.getChangeListeners().length;
        int propertyListeners = circle.getPropertyChangeListeners().length;
        for (int iteration = 0; iteration < 20; iteration++) {
            circle.updateUI();
        }
        check(model.getChangeListeners().length == modelListeners, "no model listener leak");
        check(circle.getChangeListeners().length == changeListeners, "no change listener leak");
        check(circle.getPropertyChangeListeners().length == propertyListeners,
              "no property listener leak");
        check(circle.getModel() == model, "UI replacement retains model");
        check(circle.getForeground().equals(Color.RED), "UI preserves user color");
        check(circle.getInsets().equals(new Insets(0, 0, 0, 0)), "UI preserves user border");
        circle.setOpaque(true);
        circle.updateUI();
        check(circle.isOpaque(), "UI preserves explicit user opacity");
        circle.setOpaque(false);

        circle.setIndeterminate(true);
        check(timer(circle.getUI()) == null, "no animation timer before displayability");
        circle.addNotify();
        try {
            ProgressBarUI oldUI = circle.getUI();
            Timer oldTimer = timer(oldUI);
            check(oldTimer != null && oldTimer.isRunning(), "original animation starts");
            circle.updateUI();
            check(!oldTimer.isRunning(), "old UI timer stopped");
            Timer replacement = timer(circle.getUI());
            check(replacement != null && replacement.isRunning(), "replacement UI timer starts");
            Method getBox = BasicProgressBarUI.class.getDeclaredMethod("getBox", Rectangle.class);
            getBox.setAccessible(true);
            Rectangle dirty = (Rectangle) getBox.invoke(circle.getUI(), new Rectangle());
            check(dirty.equals(new Rectangle(0, 0, 100, 100)), "whole ring animation damage");
            BufferedImage image = paint(circle, 1);
            check(nontransparent(image, 0, 0, 100, 100) > 0, "indeterminate arc paints");
            circle.setIndeterminate(false);
            check(!replacement.isRunning(), "determinate state stops animation");
            circle.setIndeterminate(true);
            check(timer(circle.getUI()).isRunning(), "animation can restart");
            Timer running = timer(circle.getUI());
            circle.removeNotify();
            check(!running.isRunning(), "removeNotify stops original animation");
        } finally {
            if (circle.isDisplayable()) {
                circle.removeNotify();
            }
            circle.setIndeterminate(false);
        }
    }

    private static void serialization() throws Exception {
        JProgressCircle original = styledCircle();
        original.setValue(63);
        original.setClockwise(false);
        original.setCountdown(true);
        original.setRingThickness(9);
        original.setString("working");
        original.setStringPainted(true);
        int listeners = original.getChangeListeners().length;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(original);
        }
        check(original.getChangeListeners().length == listeners,
              "inherited serialization reinstalls one delegate");
        JProgressCircle restored;
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (JProgressCircle) input.readObject();
        }
        check(restored.getValue() == 63 && restored.getRingThickness() == 9,
              "serialized value and thickness");
        check(!restored.isClockwise() && restored.isCountdown(), "serialized direction");
        check(restored.isStringPainted() && restored.getString().equals("working"),
              "serialized string");
        check(restored.getUI() != null && restored.getChangeListeners().length == listeners,
              "deserialization installs circular delegate once");
        restored.setStringPainted(false);
        restored.setCountdown(false);
        restored.setClockwise(true);
        restored.setValue(25);
        pixel(paint(restored, 1), 82, 18, Color.RED, "deserialized circular paint");
    }

    private static JProgressCircle styledCircle() {
        JProgressCircle circle = new JProgressCircle();
        circle.setBorder(BorderFactory.createEmptyBorder());
        circle.setForeground(Color.RED);
        circle.setBackground(Color.LIGHT_GRAY);
        circle.setRingThickness(10);
        circle.setSize(100, 100);
        return circle;
    }

    private static BufferedImage paint(JProgressCircle circle, int scale) {
        BufferedImage image = new BufferedImage(Math.max(1, circle.getWidth() * scale),
                Math.max(1, circle.getHeight() * scale), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.scale(scale, scale);
            circle.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static Timer timer(ProgressBarUI ui) throws Exception {
        Field animatorField = BasicProgressBarUI.class.getDeclaredField("animator");
        animatorField.setAccessible(true);
        Object animator = animatorField.get(ui);
        if (animator == null) {
            return null;
        }
        Field timerField = animator.getClass().getDeclaredField("timer");
        timerField.setAccessible(true);
        return (Timer) timerField.get(animator);
    }

    private static int nontransparent(BufferedImage image,
                                      int left, int top, int right, int bottom) {
        int count = 0;
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void pixel(BufferedImage image, int x, int y, Color expected, String message) {
        check(image.getRGB(x, y) == expected.getRGB(), message + ": "
                + Integer.toHexString(image.getRGB(x, y)));
    }

    private static void close(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0e-12, message + ": " + actual);
    }

    private static void expectIllegal(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class CountingCircle extends JProgressCircle {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        int repaints;

        CountingCircle(int minimum, int maximum) {
            super(minimum, maximum);
        }

        @Override
        public void repaint() {
            repaints++;
        }
    }
}
