/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @key headful
 * @summary An animated <img> of a JEditorPane stops taking frames while an
 *          ancestor is hidden or its frame is iconified, and animates again
 *          when shown (every-widget H).
 * @run main M3AnimatedImageViewHiddenTest
 */

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageConsumer;
import java.awt.image.MemoryImageSource;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class M3AnimatedImageViewHiddenTest {

    static final int SIDE = 16;
    static volatile boolean running = true;
    static JFrame frame;
    static JPanel panel;

    /** An animated source that counts the frames it hands to its consumers. */
    static final class CountingSource extends MemoryImageSource {
        final Set<ImageConsumer> consumers = ConcurrentHashMap.newKeySet();
        final AtomicInteger frames = new AtomicInteger();

        CountingSource(int[] pixels) {
            super(SIDE, SIDE, pixels, 0, SIDE);
        }

        @Override
        public synchronized void addConsumer(ImageConsumer ic) {
            consumers.add(ic);
            super.addConsumer(ic);
        }

        @Override
        public synchronized void removeConsumer(ImageConsumer ic) {
            consumers.remove(ic);
            super.removeConsumer(ic);
        }

        @Override
        public synchronized void newPixels() {
            frames.addAndGet(consumers.size());
            super.newPixels();
        }
    }

    public static void main(String[] args) throws Exception {
        int[] pixels = new int[SIDE * SIDE];
        CountingSource source = new CountingSource(pixels);
        source.setAnimated(true);
        Image image = Toolkit.getDefaultToolkit().createImage(source);
        URL url = new URI("http://m3.invalid/animated.png").toURL();
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
        }, "animated-img");
        animator.setDaemon(true);
        animator.start();
        try {
            SwingUtilities.invokeAndWait(() -> {
                HTMLEditorKit kit = new HTMLEditorKit();
                HTMLDocument document = (HTMLDocument) kit.createDefaultDocument();
                Hashtable<URL, Image> cache = new Hashtable<>();
                cache.put(url, image);
                document.putProperty("imageCache", cache);
                try {
                    kit.read(new StringReader("<html><body><img src=\"" + url
                            + "\" width=16 height=16></body></html>"), document, 0);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                JEditorPane pane = new JEditorPane();
                pane.setEditorKit(kit);
                pane.setDocument(document);
                pane.setEditable(false);
                frame = new JFrame("M3AnimatedImageViewHiddenTest");
                panel = new JPanel();
                panel.add(pane);
                frame.add(panel);
                frame.setSize(200, 120);
                frame.setVisible(true);
            });
            Thread.sleep(800);
            check(source.frames.get() > 0, "the image animates while shown");

            SwingUtilities.invokeAndWait(() -> panel.setVisible(false));
            Thread.sleep(300);
            source.frames.set(0);
            Thread.sleep(400);
            check(source.frames.get() == 0, "no frame while an ancestor is hidden, got " + source.frames.get());

            SwingUtilities.invokeAndWait(() -> panel.setVisible(true));
            Thread.sleep(600);
            check(source.frames.get() > 0, "the image animates again when shown");

            if (Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.ICONIFIED)) {
                SwingUtilities.invokeAndWait(() -> frame.setExtendedState(Frame.ICONIFIED));
                Thread.sleep(500);
                source.frames.set(0);
                Thread.sleep(400);
                if ((frame.getExtendedState() & Frame.ICONIFIED) != 0) {
                    check(source.frames.get() == 0, "no frame while iconified, got " + source.frames.get());
                    SwingUtilities.invokeAndWait(() -> frame.setExtendedState(Frame.NORMAL));
                    Thread.sleep(600);
                    check(source.frames.get() > 0, "the image animates again after deiconify");
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

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
