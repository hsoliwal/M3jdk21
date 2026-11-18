/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Arrays;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * A native Swing, multichannel oscilloscope, ported from the feature requirements
 * of Nebula Oscilloscope without incorporating SWT or EPL-licensed code.
 *
 * <p>Every channel lives in one primitive, fixed-capacity, circular sample lane.
 * No sample objects, widget peers or external scheduling threads are created.
 * Painting aggregates extrema per pixel, retaining narrow spikes that an
 * ordinary decimated polyline would miss. Appending is O(1) per channel;
 * memory is O(channels * capacity + viewportWidth). All calls are EDT-bound.</p>
 *
 * <p>The control is acquisition-driven: it does not install an always-running
 * timer. A producer posts samples onto the Swing event thread when ready.</p>
 */
public final class M3Oscilloscope extends JComponent {
    private static final long serialVersionUID = 1L;
    private final int channels;
    private final int capacity;
    private final int[] samples;
    private final int[] next;
    private final int[] used;
    private final Color[] channelColors;
    private transient Color gridColor;
    private int[] pixelMin = new int[0];
    private int[] pixelMax = new int[0];
    private int samplesPerColumn = 1;
    private int lower = -100;
    private int upper = 100;
    private boolean paused;
    private boolean gridVisible = true;
    private long totalAppended;

    public M3Oscilloscope(int channels, int capacity) {
        if (channels <= 0 || capacity < 2 || (long) channels * capacity > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("invalid channel count or sample capacity");
        }
        this.channels = channels;
        this.capacity = capacity;
        this.samples = new int[channels * capacity];
        this.next = new int[channels];
        this.used = new int[channels];
        this.channelColors = new Color[channels];
        Color foreground = UIManager.getColor("Label.foreground");
        Color background = UIManager.getColor("Panel.background");
        setForeground(foreground == null ? Color.BLACK : foreground);
        setBackground(background == null ? Color.WHITE : background);
        setOpaque(true);
        setFocusable(true);
        getAccessibleContext().setAccessibleName("Multichannel oscilloscope");
        getAccessibleContext().setAccessibleDescription("Time-domain traces for " + channels + " channels");
    }

    @Override public void setBackground(Color background) {
        super.setBackground(background);
        gridColor = null;
    }

    @Override public void setForeground(Color foreground) {
        super.setForeground(foreground);
        gridColor = null;
    }

    public int getChannelCount() { return channels; }
    public int getSampleCapacity() { return capacity; }
    public int getSamplesPerColumn() { return samplesPerColumn; }
    public boolean isPaused() { return paused; }
    public boolean isGridVisible() { return gridVisible; }
    public long getTotalAppended() { return totalAppended; }

    public void setSamplesPerColumn(int samplesPerColumn) {
        requireEdt();
        if (samplesPerColumn <= 0) throw new IllegalArgumentException("samplesPerColumn");
        if (this.samplesPerColumn == samplesPerColumn) return;
        this.samplesPerColumn = samplesPerColumn;
        repaintIfVisible();
    }

    public void setValueRange(int lower, int upper) {
        requireEdt();
        if (lower >= upper) throw new IllegalArgumentException("value range");
        this.lower = lower;
        this.upper = upper;
        repaintIfVisible();
    }

    public void setPaused(boolean paused) {
        requireEdt();
        this.paused = paused;
    }

    public void setGridVisible(boolean visible) {
        requireEdt();
        if (gridVisible != visible) {
            gridVisible = visible;
            repaintIfVisible();
        }
    }

    public void setChannelColor(int channel, Color color) {
        requireEdt();
        checkChannel(channel);
        channelColors[channel] = java.util.Objects.requireNonNull(color, "color");
        repaintIfVisible();
    }

    public int getSampleCount(int channel) {
        requireEdt();
        checkChannel(channel);
        return used[channel];
    }

    /** Returns a channel's sample by age, index 0 being its oldest retained value. */
    public int sampleAt(int channel, int index) {
        requireEdt();
        checkChannel(channel);
        if (index < 0 || index >= used[channel]) throw new IndexOutOfBoundsException("sample index");
        return sample(channel, index);
    }

    public void append(int channel, int value) {
        requireEdt();
        checkChannel(channel);
        if (!paused) {
            store(channel, value);
            repaintIfVisible();
        }
    }

    /** Appends exactly one reading to every channel, with one repaint request. */
    public void appendFrame(int[] values) {
        requireEdt();
        if (values == null || values.length != channels) throw new IllegalArgumentException("frame channels");
        if (paused) return;
        for (int i = 0; i < channels; i++) store(i, values[i]);
        repaintIfVisible();
    }

    /** Clears one channel without disturbing any other channel's logical age. */
    public void clearChannel(int channel) {
        requireEdt();
        checkChannel(channel);
        used[channel] = 0;
        next[channel] = 0;
        repaintIfVisible();
    }

    public void clear() {
        requireEdt();
        Arrays.fill(next, 0);
        Arrays.fill(used, 0);
        repaintIfVisible();
    }

    /**
     * Copies the current visible per-pixel minimum/maximum envelopes into
     * caller-provided arrays of at least {@code columns} entries. Empty columns
     * receive (+MAX_VALUE,-MIN_VALUE). New samples are right-aligned; the most
     * recent bin appears in the final column. No new heap objects are created.
     *
     * @return number of populated columns
     */
    public int copyEnvelope(int channel, int columns, int[] minima, int[] maxima) {
        requireEdt();
        checkChannel(channel);
        if (columns < 0 || minima == null || maxima == null || minima.length < columns || maxima.length < columns) {
            throw new IllegalArgumentException("envelope destinations");
        }
        Arrays.fill(minima, 0, columns, Integer.MAX_VALUE);
        Arrays.fill(maxima, 0, columns, Integer.MIN_VALUE);
        int available = used[channel];
        int visible = (int) Math.min(available, Math.min((long) Integer.MAX_VALUE, (long) columns * samplesPerColumn));
        if (visible == 0) return 0;
        int populated = (int) (((long) visible + samplesPerColumn - 1L) / samplesPerColumn);
        int firstColumn = columns - populated;
        int firstSample = available - visible;
        for (int i = 0; i < visible; i++) {
            int column = firstColumn + i / samplesPerColumn;
            int value = sample(channel, firstSample + i);
            if (value < minima[column]) minima[column] = value;
            if (value > maxima[column]) maxima[column] = value;
        }
        return populated;
    }

    private void store(int channel, int value) {
        samples[channel * capacity + next[channel]] = value;
        next[channel] = next[channel] + 1 == capacity ? 0 : next[channel] + 1;
        if (used[channel] != capacity) used[channel]++;
        totalAppended++;
    }

    private int sample(int channel, int age) {
        int start = next[channel] - used[channel];
        if (start < 0) start += capacity;
        long index = (long) start + age;
        if (index >= capacity) index -= capacity;
        return samples[channel * capacity + (int) index];
    }

    @Override public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {
            private static final long serialVersionUID = 1L;
        };
        return accessibleContext;
    }

    @Override public Dimension getPreferredSize() { return new Dimension(320, 160); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        if (pixelMin.length < width) {
            pixelMin = new int[width];
            pixelMax = new int[width];
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(getBackground());
            g.fillRect(0, 0, width, height);
            if (gridVisible) paintGrid(g, width, height);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int channel = 0; channel < channels; channel++) {
                int plotted = copyEnvelope(channel, width, pixelMin, pixelMax);
                if (plotted == 0) continue;
                Color chosen = channelColors[channel];
                g.setColor(chosen == null ? getForeground() : chosen);
                int previousX = -1, previousY = 0;
                for (int x = width - plotted; x < width; x++) {
                    if (pixelMin[x] == Integer.MAX_VALUE && pixelMax[x] == Integer.MIN_VALUE) continue;
                    int yTop = project(pixelMax[x], height);
                    int yBottom = project(pixelMin[x], height);
                    if (previousX >= 0) {
                        g.drawLine(previousX, previousY, x, (yTop + yBottom) / 2);
                    }
                    g.drawLine(x, yTop, x, yBottom);
                    previousX = x;
                    previousY = (yTop + yBottom) / 2;
                }
            }
        } finally {
            g.dispose();
        }
    }

    private void paintGrid(Graphics2D g, int width, int height) {
        if (gridColor == null) {
            Color base = getBackground(), fg = getForeground();
            gridColor = new Color((base.getRed() * 3 + fg.getRed()) / 4,
                    (base.getGreen() * 3 + fg.getGreen()) / 4,
                    (base.getBlue() * 3 + fg.getBlue()) / 4);
        }
        g.setColor(gridColor);
        for (int division = 1; division < 4; division++) {
            int x = (int) ((long) width * division / 4);
            int y = (int) ((long) height * division / 4);
            g.drawLine(x, 0, x, height - 1);
            g.drawLine(0, y, width - 1, y);
        }
    }

    private int project(int value, int height) {
        long bounded = Math.max((long) lower, Math.min((long) upper, value));
        long denominator = (long) upper - lower;
        return height - 1 - (int) ((bounded - lower) * (height - 1L) / denominator);
    }

    private void repaintIfVisible() {
        if (isShowing()) repaint();
    }

    private void checkChannel(int channel) {
        if (channel < 0 || channel >= channels) throw new IndexOutOfBoundsException("channel");
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Swing EDT required");
    }
}
