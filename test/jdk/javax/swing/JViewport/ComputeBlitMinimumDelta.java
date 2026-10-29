/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary JViewport rejects unrepresentable blit magnitudes without changing outputs
 * @run main/othervm -Djava.awt.headless=true ComputeBlitMinimumDelta
 */

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

/**
 * Product-owned main-form regression for the canonical SwingViewportTest oracle.
 * Exercises the actual protected JViewport operation through a subclass on the EDT.
 */
public final class ComputeBlitMinimumDelta {
    private static int geometries;
    private static int observations;

    private static final class Viewport extends JViewport {
        private static final long serialVersionUID = 1L;
        private int extentReads;
        private RuntimeException extentFailure;

        @Override
        public Dimension getExtentSize() {
            extentReads++;
            if (extentFailure != null) {
                throw extentFailure;
            }
            return super.getExtentSize();
        }

        boolean compute(int dx, int dy, Point from, Point to,
                        Dimension size, Rectangle paint) {
            return computeBlit(dx, dy, from, to, size, paint);
        }
    }

    private ComputeBlitMinimumDelta() { }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "all" : args[0];
        SwingUtilities.invokeAndWait(() -> {
            switch (mode) {
                case "horizontal" -> geometry(80, 60, Integer.MIN_VALUE, 0);
                case "vertical" -> geometry(80, 60, 0, Integer.MIN_VALUE);
                case "callbacks" -> callbacks();
                case "matrix" -> matrix();
                case "all" -> {
                    geometry(80, 60, Integer.MIN_VALUE, 0);
                    geometry(80, 60, 0, Integer.MIN_VALUE);
                    matrix();
                    callbacks();
                }
                default -> throw new IllegalArgumentException(mode);
            }
        });
        System.out.println("ComputeBlitMinimumDelta PASS: " + geometries
                + " geometries, " + observations + " observations, mode=" + mode);
    }

    private static void matrix() {
        int[] extents = {0, 1, 2, 17, 80, Integer.MAX_VALUE};
        int[] deltas = {Integer.MIN_VALUE, -Integer.MAX_VALUE, -81, -17, -1,
                        0, 1, 17, 81, Integer.MAX_VALUE};
        for (int width : extents) {
            for (int height : extents) {
                for (int dx : deltas) {
                    for (int dy : deltas) {
                        geometry(width, height, dx, dy);
                    }
                }
            }
        }
    }

    private static void geometry(int width, int height, int dx, int dy) {
        Viewport viewport = new Viewport();
        viewport.setSize(width, height);
        viewport.extentReads = 0;
        Point from = new Point(101, 102);
        Point to = new Point(103, 104);
        Dimension size = new Dimension(105, 106);
        Rectangle paint = new Rectangle(107, 108, 109, 110);
        long magnitudeX = Math.abs((long) dx);
        long magnitudeY = Math.abs((long) dy);
        boolean vertical = dx == 0 && dy != 0 && magnitudeY < height;
        boolean horizontal = dy == 0 && dx != 0 && magnitudeX < width;
        boolean expected = vertical || horizontal;
        boolean actual = viewport.compute(dx, dy, from, to, size, paint);
        equal(expected, actual, "extent=" + width + "x" + height
                + ", delta=" + dx + "," + dy);
        equal(1, viewport.extentReads, "extent callback count");
        if (!expected) {
            equal(new Point(101, 102), from, "rejected source");
            equal(new Point(103, 104), to, "rejected destination");
            equal(new Dimension(105, 106), size, "rejected size");
            equal(new Rectangle(107, 108, 109, 110), paint, "rejected paint");
        } else {
            int ax = (int) magnitudeX;
            int ay = (int) magnitudeY;
            equal(new Point(dx < 0 ? ax : 0, dy < 0 ? ay : 0), from, "source");
            equal(new Point(dx > 0 ? ax : 0, dy > 0 ? ay : 0), to, "destination");
            equal(new Dimension(width - ax, height - ay), size, "size");
            Rectangle expectedPaint = vertical
                    ? new Rectangle(0, dy < 0 ? height - ay : 0, width, ay)
                    : new Rectangle(dx < 0 ? width - ax : 0, 0, ax, height);
            equal(expectedPaint, paint, "paint");
        }
        geometries++;
    }

    private static void callbacks() {
        Viewport viewport = new Viewport();
        viewport.setSize(80, 60);
        viewport.extentReads = 0;
        equal(false, viewport.compute(80, 0, null, null, null, null), "horizontal edge");
        equal(false, viewport.compute(0, 60, null, null, null, null), "vertical edge");
        equal(false, viewport.compute(1, 1, null, null, null, null), "diagonal");
        equal(false, viewport.compute(0, 0, null, null, null, null), "stationary");
        equal(4, viewport.extentReads, "rejected extent callbacks");
        try {
            viewport.compute(1, 0, null, null, null, null);
            throw new AssertionError("accepted blit must access output arguments");
        } catch (NullPointerException expected) {
            equal(5, viewport.extentReads, "extent callback before null output");
        }

        RuntimeException marker = new IllegalStateException("extent callback marker");
        viewport.extentFailure = marker;
        Point from = new Point(21, 22);
        Point to = new Point(23, 24);
        Dimension size = new Dimension(25, 26);
        Rectangle paint = new Rectangle(27, 28, 29, 30);
        try {
            viewport.compute(Integer.MIN_VALUE, 0, from, to, size, paint);
            throw new AssertionError("extent callback must still run for minimum delta");
        } catch (RuntimeException expected) {
            if (expected != marker) {
                throw new AssertionError("extent exception identity changed", expected);
            }
            observations++;
        }
        equal(6, viewport.extentReads, "throwing extent callback count");
        equal(new Point(21, 22), from, "exception source");
        equal(new Point(23, 24), to, "exception destination");
        equal(new Dimension(25, 26), size, "exception size");
        equal(new Rectangle(27, 28, 29, 30), paint, "exception paint");
    }

    private static void equal(Object expected, Object actual, String description) {
        observations++;
        if (!expected.equals(actual)) {
            throw new AssertionError(description + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
