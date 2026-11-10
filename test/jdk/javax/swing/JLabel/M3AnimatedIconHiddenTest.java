/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @key headful
 * @summary Animated icons of a JLabel and a JButton take no frame while their
 *          frame is iconified or an ancestor is hidden, and animate again when
 *          shown (every-widget H).
 * @run main M3AnimatedIconHiddenTest
 */

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class M3AnimatedIconHiddenTest {

    static final int SIDE = 16;
    static final AtomicInteger labelFrames = new AtomicInteger(), buttonFrames = new AtomicInteger();
    static volatile boolean running = true;
    static JFrame frame;
    static JPanel panel;

    public static void main(String[] args) throws Exception {
        int[] pixels = new int[SIDE * SIDE];
        MemoryImageSource source = new MemoryImageSource(SIDE, SIDE, pixels, 0, SIDE);
        source.setAnimated(true);
        Image image = Toolkit.getDefaultToolkit().createImage(source);
        Thread animator = new Thread(() -> {
            int tick = 0;
            while (running) {
                java.util.Arrays.fill(pixels, 0xff000000 | (tick++ * 0x10101));
                source.newPixels();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "animated-icon");
        animator.setDaemon(true);
        animator.start();
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame("M3AnimatedIconHiddenTest");
                panel = new JPanel();
                panel.add(new JLabel(new ImageIcon(image)) {
                    @Override
                    public boolean imageUpdate(Image img, int flags, int x, int y, int w, int h) {
                        boolean more = super.imageUpdate(img, flags, x, y, w, h);
                        if (more) {
                            labelFrames.incrementAndGet();
                        }
                        return more;
                    }
                });
                panel.add(new JButton(new ImageIcon(image)) {
                    @Override
                    public boolean imageUpdate(Image img, int flags, int x, int y, int w, int h) {
                        boolean more = super.imageUpdate(img, flags, x, y, w, h);
                        if (more) {
                            buttonFrames.incrementAndGet();
                        }
                        return more;
                    }
                });
                frame.add(panel);
                frame.pack();
                frame.setVisible(true);
            });
            Thread.sleep(600);
            check(labelFrames.get() > 0 && buttonFrames.get() > 0, "the icons animate while shown");
            SwingUtilities.invokeAndWait(() -> panel.setVisible(false));
            Thread.sleep(300);
            reset();
            Thread.sleep(400);
            check(labelFrames.get() == 0, "label: no frame while an ancestor is hidden, got " + labelFrames.get());
            check(buttonFrames.get() == 0, "button: no frame while an ancestor is hidden, got " + buttonFrames.get());
            SwingUtilities.invokeAndWait(() -> panel.setVisible(true));
            Thread.sleep(600);
            check(labelFrames.get() > 0 && buttonFrames.get() > 0, "the icons animate again when shown");
            if (Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.ICONIFIED)) {
                SwingUtilities.invokeAndWait(() -> frame.setExtendedState(Frame.ICONIFIED));
                Thread.sleep(500);
                reset();
                Thread.sleep(400);
                if ((frame.getExtendedState() & Frame.ICONIFIED) != 0) {
                    check(labelFrames.get() == 0, "label: no frame while iconified, got " + labelFrames.get());
                    check(buttonFrames.get() == 0, "button: no frame while iconified, got " + buttonFrames.get());
                    SwingUtilities.invokeAndWait(() -> frame.setExtendedState(Frame.NORMAL));
                    Thread.sleep(600);
                    check(labelFrames.get() > 0 && buttonFrames.get() > 0, "the icons animate again after deiconify");
                    System.out.println("iconify: checked");
                } else {
                    System.out.println("iconify: the frame did not iconify, not checked");
                }
            }
            System.out.println("PASSED");
        } finally {
            running = false;
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }
    }

    static void reset() {
        labelFrames.set(0);
        buttonFrames.set(0);
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
