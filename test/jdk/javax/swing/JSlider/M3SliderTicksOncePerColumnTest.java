/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary A slider whose ticks are denser than its pixels paints each tick
 *          column once, and exactly the columns of all its tick values
 *          (every-widget D).
 * @run main/othervm -Djava.awt.headless=true M3SliderTicksOncePerColumnTest
 */

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

public class M3SliderTicksOncePerColumnTest {

    static final int MAX = 1_000_000;

    /* records the positions the slider paints its ticks at */
    static final class Recording extends BasicSliderUI {
        final List<Integer> minor = new ArrayList<>();
        final List<Integer> major = new ArrayList<>();

        Recording(JSlider slider) {
            super(slider);
        }

        void geometry() {
            calculateGeometry();
        }

        TreeSet<Integer> positions(int spacing) {
            TreeSet<Integer> all = new TreeSet<>();
            boolean horizontal = slider.getOrientation() == JSlider.HORIZONTAL;
            for (int value = slider.getMinimum(); value <= slider.getMaximum(); value += spacing) {
                all.add(horizontal ? xPositionForValue(value) : yPositionForValue(value));
            }
            return all;
        }

        @Override
        protected void paintMinorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
            minor.add(x);
            super.paintMinorTickForHorizSlider(g, tickBounds, x);
        }

        @Override
        protected void paintMajorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
            major.add(x);
            super.paintMajorTickForHorizSlider(g, tickBounds, x);
        }

        @Override
        protected void paintMinorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
            minor.add(y);
            super.paintMinorTickForVertSlider(g, tickBounds, y);
        }

        @Override
        protected void paintMajorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
            major.add(y);
            super.paintMajorTickForVertSlider(g, tickBounds, y);
        }
    }

    public static void main(String[] args) {
        for (int orientation : new int[] { JSlider.HORIZONTAL, JSlider.VERTICAL }) {
            String name = orientation == JSlider.HORIZONTAL ? "horizontal" : "vertical";
            JSlider slider = new JSlider(orientation, 0, MAX, 0);
            slider.setMinorTickSpacing(1);
            slider.setMajorTickSpacing(100_000);
            slider.setPaintTicks(true);
            Recording ui = new Recording(slider);
            slider.setUI(ui);
            if (orientation == JSlider.HORIZONTAL) {
                slider.setSize(400, 60);
            } else {
                slider.setSize(60, 400);
            }
            ui.geometry();

            BufferedImage image = new BufferedImage(slider.getWidth(), slider.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            long start = System.nanoTime();
            slider.paint(g);
            long micros = (System.nanoTime() - start) / 1000;
            g.dispose();

            int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
            System.out.println(name + ": image hash " + java.util.Arrays.hashCode(pixels));
            TreeSet<Integer> minorColumns = ui.positions(1);
            TreeSet<Integer> majorColumns = ui.positions(100_000);
            System.out.println(name + ": " + ui.minor.size() + " minor tick paints for " + minorColumns.size()
                    + " columns, " + ui.major.size() + " major for " + majorColumns.size() + ", paint " + micros + " us");
            check(new TreeSet<>(ui.minor).equals(minorColumns), name + ": the minor ticks cover exactly their columns");
            check(new TreeSet<>(ui.major).equals(majorColumns), name + ": the major ticks cover exactly their columns");
            check(ui.minor.size() == minorColumns.size(), name + ": each minor tick column is painted once, got "
                    + ui.minor.size() + " paints for " + minorColumns.size() + " columns");
            check(ui.major.size() == majorColumns.size(), name + ": each major tick column is painted once");
        }
        System.out.println("PASSED");
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
