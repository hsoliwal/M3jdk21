/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @key headful
 * @summary An indeterminate JProgressBar paints no animation frame while it
 *          cannot be seen (an ancestor hidden, or its frame iconified) and
 *          animates again when it is shown (every-widget H).
 * @run main M3IndeterminateHiddenTest
 */

import java.awt.Frame;
import java.awt.Toolkit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class M3IndeterminateHiddenTest {

    static final AtomicInteger frames = new AtomicInteger();
    static JFrame frame;
    static JPanel panel;
    static JProgressBar bar;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame("M3IndeterminateHiddenTest");
                panel = new JPanel();
                bar = new JProgressBar() {
                    @Override
                    public void repaint(long tm, int x, int y, int width, int height) {
                        frames.incrementAndGet();
                        super.repaint(tm, x, y, width, height);
                    }
                };
                bar.setIndeterminate(true);
                panel.add(bar);
                frame.add(panel);
                frame.pack();
                frame.setVisible(true);
            });
            Thread.sleep(500);
            check(frames.get() > 0, "the bar animates while shown");
            SwingUtilities.invokeAndWait(() -> panel.setVisible(false));
            Thread.sleep(300);
            frames.set(0);
            Thread.sleep(500);
            check(frames.get() == 0, "no frame while an ancestor is hidden, got " + frames.get());
            SwingUtilities.invokeAndWait(() -> panel.setVisible(true));
            Thread.sleep(500);
            check(frames.get() > 0, "the bar animates again when shown");
            if (Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.ICONIFIED)) {
                SwingUtilities.invokeAndWait(() -> frame.setExtendedState(Frame.ICONIFIED));
                Thread.sleep(500);
                frames.set(0);
                Thread.sleep(500);
                boolean iconified = (frame.getExtendedState() & Frame.ICONIFIED) != 0;
                if (iconified) {
                    check(frames.get() == 0, "no frame while the frame is iconified, got " + frames.get());
                    SwingUtilities.invokeAndWait(() -> frame.setExtendedState(Frame.NORMAL));
                    Thread.sleep(500);
                    check(frames.get() > 0, "the bar animates again after deiconify");
                    System.out.println("iconify: checked");
                } else {
                    System.out.println("iconify: the frame did not iconify, not checked");
                }
            }
            System.out.println("PASSED");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
