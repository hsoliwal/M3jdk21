/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import javax.swing.SwingUtilities;

/** Bounded-memory stress: 10 million incoming samples, eight channels, 1M retained/channel. */
public final class M3OscilloscopeScale {
    private M3OscilloscopeScale() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(() -> {
            M3Oscilloscope scope = new M3Oscilloscope(8, 1_000_000);
            int[] frame = new int[8];
            for (int tick = 0; tick < 1_250_000; tick++) {
                for (int channel = 0; channel < frame.length; channel++) frame[channel] = tick + channel;
                scope.appendFrame(frame);
            }
            if (scope.getTotalAppended() != 10_000_000L) throw new AssertionError("10M total writes");
            for (int channel = 0; channel < 8; channel++) {
                if (scope.getSampleCount(channel) != 1_000_000) throw new AssertionError("capacity");
                if (scope.sampleAt(channel, 0) != 250_000 + channel) throw new AssertionError("oldest");
                if (scope.sampleAt(channel, 999_999) != 1_249_999 + channel) throw new AssertionError("latest");
            }
            scope.setSamplesPerColumn(4);
            int[] min = new int[1920], max = new int[1920];
            if (scope.copyEnvelope(7, 1920, min, max) != 1920) throw new AssertionError("viewport bins");
            if (min[1919] != 1_250_003 || max[1919] != 1_250_006) throw new AssertionError("final envelope");
        });
        System.out.println("M3_OSCILLOSCOPE_10M_SAMPLES_128M_HEAP_PASS");
    }
}
