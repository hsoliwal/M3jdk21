/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @key headful
 * @summary Highlight changes of a text component that is not showing, or whose
 *          frame is iconified, run no damageRange (modelToView and repaint per
 *          range); a showing component is damaged as before (every-widget H).
 * @run main M3HighlightDamageHiddenTest
 */

import java.awt.Frame;
import java.awt.Toolkit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;

public class M3HighlightDamageHiddenTest {

    static final AtomicInteger damaged = new AtomicInteger();
    static JFrame frame;
    static JPanel panel;
    static JTextArea text;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                text = new JTextArea("alpha beta gamma delta epsilon zeta eta theta", 4, 30);
                text.setUI(new BasicTextAreaUI() {
                    @Override
                    public void damageRange(JTextComponent t, int p0, int p1) {
                        damaged.incrementAndGet();
                        super.damageRange(t, p0, p1);
                    }
                });
                frame = new JFrame("M3HighlightDamageHiddenTest");
                panel = new JPanel();
                panel.add(text);
                frame.add(panel);
                frame.pack();
                frame.setVisible(true);
            });
            Thread.sleep(500);

            check(highlight() > 0, "a showing text component is damaged for a new highlight");

            SwingUtilities.invokeAndWait(() -> panel.setVisible(false));
            check(highlight() == 0, "a hidden text component takes no highlight damage, got " + damaged.get());
            SwingUtilities.invokeAndWait(() -> panel.setVisible(true));
            check(highlight() > 0, "the shown text component is damaged again");

            if (Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.ICONIFIED)) {
                SwingUtilities.invokeAndWait(() -> frame.setExtendedState(Frame.ICONIFIED));
                Thread.sleep(500);
                if ((frame.getExtendedState() & Frame.ICONIFIED) != 0) {
                    check(highlight() == 0, "an iconified text component takes no highlight damage, got " + damaged.get());
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

    /* adds three highlights on the EDT, lets the queued damage run, and returns the damageRange calls it made */
    static int highlight() throws Exception {
        damaged.set(0);
        SwingUtilities.invokeAndWait(() -> {
            try {
                DefaultHighlighter.DefaultHighlightPainter painter =
                        new DefaultHighlighter.DefaultHighlightPainter(java.awt.Color.YELLOW);
                text.getHighlighter().addHighlight(0, 5, painter);
                text.getHighlighter().addHighlight(6, 10, painter);
                text.getHighlighter().addHighlight(11, 16, painter);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
        return damaged.get();
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
