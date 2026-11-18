/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.SwingUtilities;

/** JDK-only EDT/headless behavioral proof; no Nebula or SwingX dependency. */
public final class M3OscilloscopeSmoke {
    private M3OscilloscopeSmoke() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3OscilloscopeSmoke::exercise);
        System.out.println("M3_NATIVE_OSCILLOSCOPE_PASS");
    }

    private static void exercise() {
        reject(() -> new M3Oscilloscope(0, 8), IllegalArgumentException.class);
        reject(() -> new M3Oscilloscope(1, 1), IllegalArgumentException.class);
        reject(() -> new M3Oscilloscope(Integer.MAX_VALUE, Integer.MAX_VALUE), IllegalArgumentException.class);
        M3Oscilloscope scope = new M3Oscilloscope(2, 8);
        check(scope.getChannelCount() == 2 && scope.getSampleCapacity() == 8, "fixed primitive model");
        for (int i = 0; i < 12; i++) scope.appendFrame(new int[]{i, 100 + i});
        check(scope.getSampleCount(0) == 8 && scope.getSampleCount(1) == 8, "bounded channels");
        check(scope.sampleAt(0, 0) == 4 && scope.sampleAt(0, 7) == 11, "wrap preserves newest 8");
        check(scope.sampleAt(1, 0) == 104 && scope.sampleAt(1, 7) == 111, "independent lanes");
        check(scope.getTotalAppended() == 24, "two channel counts per frame");
        scope.setSamplesPerColumn(2);
        int[] min = new int[4], max = new int[4];
        int columns = scope.copyEnvelope(0, 4, min, max);
        check(columns == 4, "all columns populated");
        check(Arrays.equals(min, new int[]{4, 6, 8, 10}), "left-to-right min aggregation");
        check(Arrays.equals(max, new int[]{5, 7, 9, 11}), "left-to-right max aggregation");
        scope.clearChannel(0);
        check(scope.getSampleCount(0) == 0 && scope.getSampleCount(1) == 8, "clear one channel");
        scope.append(0, 12);
        scope.append(0, -100);
        scope.append(0, 13);
        scope.append(0, 100);
        scope.setSamplesPerColumn(4);
        check(scope.copyEnvelope(0, 3, min, max) == 1, "right aligned sparse window");
        check(min[0] == Integer.MAX_VALUE && max[0] == Integer.MIN_VALUE, "empty pixel sentinel");
        check(min[2] == -100 && max[2] == 100, "spikes survive decimation");
        scope.setPaused(true);
        long before = scope.getTotalAppended();
        scope.appendFrame(new int[]{999, 999});
        check(scope.getTotalAppended() == before, "paused acquisition is stable");
        scope.setPaused(false);
        scope.append(0, 14);
        check(scope.sampleAt(0, 4) == 14, "resumed acquisition");
        scope.setChannelColor(0, Color.RED);
        scope.setChannelColor(1, Color.BLUE);
        scope.setValueRange(-100, 100);
        scope.setGridVisible(false);
        scope.setSize(120, 80);
        BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try { scope.paint(graphics); } finally { graphics.dispose(); }
        int redPixels = 0, bluePixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y) & 0x00ffffff;
                if (rgb == (Color.RED.getRGB() & 0x00ffffff)) redPixels++;
                if (rgb == (Color.BLUE.getRGB() & 0x00ffffff)) bluePixels++;
            }
        }
        check(redPixels > 0 && bluePixels > 0, "both channels visibly render");
        scope.clear();
        check(scope.getSampleCount(0) == 0 && scope.getSampleCount(1) == 0, "clear all");
        scope.copyEnvelope(0, 3, min, max);
        check(Arrays.equals(Arrays.copyOf(min, 3), new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}),
                "no points after clear");
        reject(() -> scope.setSamplesPerColumn(0), IllegalArgumentException.class);
        reject(() -> scope.setValueRange(1, 1), IllegalArgumentException.class);
        reject(() -> scope.append(2, 0), IndexOutOfBoundsException.class);
        reject(() -> scope.sampleAt(0, 0), IndexOutOfBoundsException.class);
        reject(() -> scope.copyEnvelope(0, 4, new int[1], new int[4]), IllegalArgumentException.class);
    }

    private static void reject(Runnable action, Class<? extends Throwable> type) {
        try { action.run(); } catch (Throwable e) { if (type.isInstance(e)) return; throw e; }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
