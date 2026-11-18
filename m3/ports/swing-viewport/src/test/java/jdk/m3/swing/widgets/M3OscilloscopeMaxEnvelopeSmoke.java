/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

/** A true MAX_VALUE reading must not disappear as an empty-bin sentinel. */
public final class M3OscilloscopeMaxEnvelopeSmoke {
    private M3OscilloscopeMaxEnvelopeSmoke() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3OscilloscopeMaxEnvelopeSmoke::exercise);
        System.out.println("M3_OSCILLOSCOPE_MAX_ENVELOPE_PASS");
    }

    private static void exercise() {
        M3Oscilloscope scope = new M3Oscilloscope(1, 8);
        scope.setGridVisible(false);
        scope.setForeground(Color.RED);
        scope.setBackground(Color.WHITE);
        scope.setValueRange(Integer.MAX_VALUE - 100, Integer.MAX_VALUE);
        scope.setSize(60, 40);
        scope.append(0, Integer.MAX_VALUE);
        int[] lows = new int[1], highs = new int[1];
        if (scope.copyEnvelope(0, 1, lows, highs) != 1
                || lows[0] != Integer.MAX_VALUE || highs[0] != Integer.MAX_VALUE) {
            throw new AssertionError("populated bin must retain exact MAX_VALUE extrema");
        }
        BufferedImage image = new BufferedImage(60,40,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { scope.paint(g); } finally { g.dispose(); }
        int hit = 0;
        for (int y = 0; y < 5; y++) for (int x = 57; x < 60; x++) {
            int rgb = image.getRGB(x, y);
            int red = (rgb >>> 16) & 255;
            int green = (rgb >>> 8) & 255;
            if (red > green + 40) hit++;
        }
        if (hit == 0) throw new AssertionError("all-MAX_VALUE envelope was skipped during paint");
        scope.clear();
        BufferedImage empty = new BufferedImage(60,40,BufferedImage.TYPE_INT_ARGB);
        g = empty.createGraphics();
        try { scope.paint(g); } finally { g.dispose(); }
        for (int y = 0; y < 5; y++) for (int x = 57; x < 60; x++) {
            int rgb = empty.getRGB(x,y);
            if (((rgb >>> 16) & 255) > ((rgb >>> 8) & 255) + 40) {
                throw new AssertionError("empty bin incorrectly drawn as MAX_VALUE");
            }
        }
    }
}
