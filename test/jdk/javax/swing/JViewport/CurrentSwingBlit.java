// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Reject unrepresentable scroll magnitudes without corrupting reusable blit geometry.
 * @run main/othervm -Djava.awt.headless=true CurrentSwingBlit
 */
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

/** Current-source regression; executes on the EDT and needs no native display. */
public final class CurrentSwingBlit {
    private static final class Viewport extends JViewport {
        private static final long serialVersionUID = 1L;
        int extentReads;
        @Override public Dimension getExtentSize() {
            extentReads++;
            return super.getExtentSize();
        }
        boolean blit(int dx, int dy, Point from, Point to, Dimension size, Rectangle paint) {
            return computeBlit(dx, dy, from, to, size, paint);
        }
    }

    private CurrentSwingBlit() { }

    public static void main(String[] args) throws Exception {
        String selected = args.length == 0 ? "all" : args[0];
        if (!java.util.Set.of("all", "horizontal", "vertical", "cartesian", "callbacks").contains(selected)) {
            throw new IllegalArgumentException("unknown case: " + selected);
        }
        SwingUtilities.invokeAndWait(() -> {
            if ("all".equals(selected) || "horizontal".equals(selected)) {
                verify(80, 60, Integer.MIN_VALUE, 0);
            }
            if ("all".equals(selected) || "vertical".equals(selected)) {
                verify(80, 60, 0, Integer.MIN_VALUE);
            }
            if ("all".equals(selected) || "cartesian".equals(selected)) {
                int[] sizes = {0, 1, 2, 17, 80, Integer.MAX_VALUE};
                int[] deltas = {Integer.MIN_VALUE, -Integer.MAX_VALUE, -81, -17, -1,
                        0, 1, 17, 81, Integer.MAX_VALUE};
                for (int width : sizes) for (int height : sizes)
                    for (int dx : deltas) for (int dy : deltas) verify(width, height, dx, dy);
            }
            if ("all".equals(selected) || "callbacks".equals(selected)) {
                Viewport v = new Viewport();
                v.setSize(80, 60);
                v.extentReads = 0;
                require(!v.blit(80, 0, null, null, null, null), "boundary x must reject");
                require(!v.blit(0, 60, null, null, null, null), "boundary y must reject");
                require(!v.blit(1, 1, null, null, null, null), "diagonal must reject");
                require(v.extentReads == 3, "rejections must retain the extent callback");
                try {
                    v.blit(1, 0, null, null, null, null);
                    throw new AssertionError("accepted geometry requires output objects");
                } catch (NullPointerException expected) {
                    require(v.extentReads == 4, "accepted geometry reads extent first");
                }
            }
        });
        System.out.println("PASS CurrentSwingBlit " + selected);
    }

    private static void verify(int width, int height, int dx, int dy) {
        Viewport v = new Viewport();
        v.setSize(width, height);
        v.extentReads = 0;
        Point from = new Point(101, 102), to = new Point(103, 104);
        Dimension size = new Dimension(105, 106);
        Rectangle paint = new Rectangle(107, 108, 109, 110);
        boolean vertical = dx == 0 && dy != 0 && Math.abs((long) dy) < height;
        boolean horizontal = dy == 0 && dx != 0 && Math.abs((long) dx) < width;
        require(v.blit(dx, dy, from, to, size, paint) == (vertical || horizontal),
                "extent=" + width + "x" + height + ", delta=" + dx + "," + dy);
        require(v.extentReads == 1, "one extent read");
        if (!vertical && !horizontal) {
            require(from.equals(new Point(101, 102)), "rejection changed source point");
            require(to.equals(new Point(103, 104)), "rejection changed destination point");
            require(size.equals(new Dimension(105, 106)), "rejection changed blit size");
            require(paint.equals(new Rectangle(107, 108, 109, 110)), "rejection changed paint bounds");
            return;
        }
        int ax = (int) Math.abs((long) dx), ay = (int) Math.abs((long) dy);
        require(from.equals(new Point(Math.max(0, -dx), Math.max(0, -dy))), "source point");
        require(to.equals(new Point(Math.max(0, dx), Math.max(0, dy))), "destination point");
        require(size.equals(new Dimension(width - ax, height - ay)), "blit size");
        Rectangle expectedPaint = vertical
                ? new Rectangle(0, dy < 0 ? height - ay : 0, width, ay)
                : new Rectangle(dx < 0 ? width - ax : 0, 0, ax, height);
        require(paint.equals(expectedPaint), "paint bounds");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
