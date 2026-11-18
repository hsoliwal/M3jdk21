/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. The copyright holders
 * designate this particular file as subject to the "Classpath" exception
 * as provided in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package javax.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.beans.BeanProperty;
import java.io.Serial;
import javax.swing.plaf.basic.BasicProgressBarUI;

/**
 * A progress bar that displays its value as a circular ring.
 * The inherited {@link BoundedRangeModel}, change listeners, accessible value,
 * and progress string have the same ownership as in {@link JProgressBar}.
 * No second progress model is created. The default range is 0 to 100.
 *
 * <p>The ring starts at twelve o'clock and advances clockwise by default.
 * {@link #setClockwise(boolean)} changes its direction, and
 * {@link #setCountdown(boolean)} displays the remaining fraction without
 * changing the model's value or progress string. The inherited orientation
 * property does not change the circular presentation.
 *
 * <p>The default delegate uses the current look and feel's progress-bar colors
 * and font. The foreground paints progress and the background paints the
 * remaining track; the center is transparent unless the component is opaque.
 * An optional {@code ProgressCircle.border} UI default supplies its border.
 * Indeterminate animation uses {@link BasicProgressBarUI}'s existing timer and
 * component lifecycle, including the target look and feel's animation timing.
 * Applications may install another progress-bar delegate using {@code setUI}.
 * Calling {@link #updateUI()} restores the default circular delegate.
 *
 * <p>Like other Swing components, this component and its model should be
 * accessed on the event dispatch thread. Serialization is intended only for
 * short-term storage between applications running the same Swing version.
 *
 * @see JProgressBar
 * @see BoundedRangeModel
 * @see javax.accessibility.AccessibleValue
 */
public class JProgressCircle extends JProgressBar {
    @Serial
    private static final long serialVersionUID = 1L;

    private float ringThickness = 6.0f;
    private boolean clockwise = true;
    private boolean countdown;

    /** Creates an empty progress circle with a range of 0 to 100. */
    public JProgressCircle() {
        super();
    }

    /**
     * Creates a progress circle with the specified range.
     *
     * @param minimum the minimum value
     * @param maximum the maximum value
     * @throws IllegalArgumentException if {@code minimum > maximum}
     */
    public JProgressCircle(int minimum, int maximum) {
        super(minimum, maximum);
    }

    /**
     * Creates a progress circle using the supplied model.
     *
     * @param model the progress model; {@code null} leaves the model unset,
     *              as in {@link JProgressBar#JProgressBar(BoundedRangeModel)}
     */
    public JProgressCircle(BoundedRangeModel model) {
        super(model);
    }

    /**
     * Installs the circular delegate using the current progress-bar defaults.
     * The inherited {@code ProgressBarUI} identity and serialization lifecycle
     * remain owned by {@code JProgressBar}.
     */
    @Override
    public void updateUI() {
        setUI(new CircleProgressBarUI());
    }

    /**
     * Returns the ring thickness in logical pixels.
     *
     * @return the positive thickness, initially 6
     */
    public float getRingThickness() {
        return ringThickness;
    }

    /**
     * Sets the ring thickness in logical pixels. Painting limits the thickness
     * to half the available diameter when the component is smaller than the
     * requested ring. The value is independent of the display scale.
     *
     * @param thickness the positive, finite thickness
     * @throws IllegalArgumentException if the value is not positive and finite
     */
    @BeanProperty(description = "The progress ring thickness in logical pixels.")
    public void setRingThickness(float thickness) {
        if (!(thickness > 0.0f) || !Float.isFinite(thickness)) {
            throw new IllegalArgumentException("thickness must be positive and finite");
        }
        float previous = ringThickness;
        if (previous != thickness) {
            ringThickness = thickness;
            firePropertyChange("ringThickness", previous, thickness);
            revalidate();
            repaint();
        }
    }

    /**
     * Returns the direction in which the ring advances.
     *
     * @return {@code true} for clockwise, initially {@code true}
     */
    public boolean isClockwise() {
        return clockwise;
    }

    /**
     * Sets the direction of determinate and indeterminate progress.
     *
     * @param clockwise {@code true} for clockwise; {@code false} for counterclockwise
     */
    @BeanProperty(description = "Whether the ring advances clockwise.")
    public void setClockwise(boolean clockwise) {
        boolean previous = this.clockwise;
        if (previous != clockwise) {
            this.clockwise = clockwise;
            firePropertyChange("clockwise", previous, clockwise);
            repaint();
        }
    }

    /**
     * Returns whether the ring shows remaining progress.
     *
     * @return {@code true} for a remaining-progress ring, initially {@code false}
     */
    public boolean isCountdown() {
        return countdown;
    }

    /**
     * Selects remaining-progress painting. This setting affects the determinate
     * ring only; model values, strings, accessibility, and animation are unchanged.
     *
     * @param countdown {@code true} to paint one minus the completed fraction
     */
    @BeanProperty(description = "Whether the ring displays remaining progress.")
    public void setCountdown(boolean countdown) {
        boolean previous = this.countdown;
        if (previous != countdown) {
            this.countdown = countdown;
            firePropertyChange("countdown", previous, countdown);
            repaint();
        }
    }

    /**
     * Returns the completed fraction using the full signed integer model range.
     * An empty numeric range or an unset model returns 0 rather than a
     * non-finite value.
     *
     * @return the completed fraction from 0 to 1
     */
    @Override
    public double getPercentComplete() {
        BoundedRangeModel model = getModel();
        if (model == null) {
            return 0.0;
        }
        long minimum = model.getMinimum();
        long range = (long) model.getMaximum() - minimum;
        return range <= 0 ? 0.0 : ((long) model.getValue() - minimum) / (double) range;
    }

    /** Notifies inherited listeners and invalidates the circular presentation. */
    @Override
    protected void fireStateChanged() {
        super.fireStateChanged();
        // BasicProgressBarUI's integer percentage cache is not ring geometry.
        repaint();
    }

    private static final class CircleProgressBarUI extends BasicProgressBarUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            LookAndFeel.installBorder(progressBar, "ProgressCircle.border");
            LookAndFeel.installProperty(progressBar, "opaque", Boolean.FALSE);
        }

        @Override
        protected void paintDeterminate(Graphics graphics, JComponent component) {
            paintRing(graphics, false);
        }

        @Override
        protected void paintIndeterminate(Graphics graphics, JComponent component) {
            paintRing(graphics, true);
        }

        private void paintRing(Graphics graphics, boolean indeterminate) {
            if (!(graphics instanceof Graphics2D)) {
                return;
            }
            JProgressCircle circle = (JProgressCircle) progressBar;
            Insets insets = circle.getInsets();
            double width = (double) circle.getWidth() - insets.left - insets.right;
            double height = (double) circle.getHeight() - insets.top - insets.bottom;
            double diameter = Math.min(width, height);
            if (diameter <= 0.0) {
                return;
            }
            float thickness = (float) Math.min(circle.getRingThickness(), diameter / 2.0);
            double x = insets.left + (width - diameter + thickness) / 2.0;
            double y = insets.top + (height - diameter + thickness) / 2.0;
            double size = diameter - thickness;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.clip(new Rectangle2D.Double(insets.left, insets.top, width, height));
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT,
                                            BasicStroke.JOIN_ROUND));
                Color foreground = indicatorColor(circle);
                Color background = circle.getBackground();
                g.setColor(background == null ? Color.LIGHT_GRAY : background);
                g.draw(new Ellipse2D.Double(x, y, size, size));
                g.setColor(foreground);
                double direction = circle.isClockwise() ? -1.0 : 1.0;
                if (indeterminate) {
                    int frames = Math.max(1, getFrameCount());
                    double start = 90.0 + direction * 360.0 * getAnimationIndex() / frames;
                    g.draw(new Arc2D.Double(x, y, size, size,
                                           start, direction * 90.0, Arc2D.OPEN));
                } else {
                    double fraction = circle.getPercentComplete();
                    if (circle.isCountdown()) {
                        fraction = 1.0 - fraction;
                    }
                    if (fraction >= 1.0) {
                        g.draw(new Ellipse2D.Double(x, y, size, size));
                    } else if (fraction > 0.0) {
                        g.draw(new Arc2D.Double(x, y, size, size,
                                               90.0, direction * 360.0 * fraction, Arc2D.OPEN));
                    }
                }
                Font font = circle.getFont();
                if (circle.isStringPainted() && font != null) {
                    String text = circle.getString();
                    g.setFont(font);
                    FontMetrics metrics = g.getFontMetrics();
                    float textX = (float) (insets.left + (width - metrics.stringWidth(text)) / 2.0);
                    float textY = (float) (insets.top + (height - metrics.getHeight()) / 2.0
                                          + metrics.getAscent());
                    g.drawString(text, textX, textY);
                }
            } finally {
                g.dispose();
            }
        }

        private Color indicatorColor(JProgressCircle circle) {
            Color foreground = circle.getForeground();
            if (!circle.isEnabled()) {
                Color disabled = UIManager.getColor("ProgressBar.disabledForeground");
                if (disabled == null) {
                    disabled = UIManager.getColor("Label.disabledForeground");
                }
                if (disabled != null) {
                    return disabled;
                }
                return foreground == null ? Color.GRAY : foreground.darker();
            }
            return foreground == null ? Color.DARK_GRAY : foreground;
        }

        @Override
        protected Rectangle getBox(Rectangle rectangle) {
            // Each frame moves an arc around the ring, not a horizontal box.
            if (rectangle == null) {
                rectangle = new Rectangle();
            }
            return SwingUtilities.calculateInnerArea(progressBar, rectangle);
        }

        @Override
        public Dimension getPreferredSize(JComponent component) {
            JProgressCircle circle = (JProgressCircle) component;
            double side = Math.max(64.0, 2.0 * circle.getRingThickness() + 12.0);
            if (circle.isStringPainted() && circle.getFont() != null) {
                FontMetrics metrics = circle.getFontMetrics(circle.getFont());
                side = Math.max(side, Math.max(metrics.stringWidth(circle.getString()),
                                               metrics.getHeight())
                                     + 2.0 * circle.getRingThickness() + 12.0);
            }
            return withInsets(component, side);
        }

        @Override
        public Dimension getMinimumSize(JComponent component) {
            JProgressCircle circle = (JProgressCircle) component;
            return withInsets(component, Math.max(16.0, 2.0 * circle.getRingThickness() + 2.0));
        }

        @Override
        public Dimension getMaximumSize(JComponent component) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        private Dimension withInsets(JComponent component, double side) {
            Insets insets = component.getInsets();
            int width = (int) Math.max(0.0, Math.ceil(side + insets.left + insets.right));
            int height = (int) Math.max(0.0, Math.ceil(side + insets.top + insets.bottom));
            return new Dimension(width, height);
        }

        @Override
        public int getBaseline(JComponent component, int width, int height) {
            if (component == null) {
                throw new NullPointerException("component");
            }
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("negative component size");
            }
            if (!progressBar.isStringPainted() || progressBar.getFont() == null) {
                return -1;
            }
            Insets insets = progressBar.getInsets();
            FontMetrics metrics = progressBar.getFontMetrics(progressBar.getFont());
            long innerHeight = (long) height - insets.top - insets.bottom;
            long baseline = insets.top + (innerHeight - metrics.getHeight()) / 2
                            + metrics.getAscent();
            return baseline >= 0 && baseline < height ? (int) baseline : -1;
        }

        @Override
        public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent component) {
            if (component == null) {
                throw new NullPointerException("component");
            }
            return progressBar.isStringPainted()
                    ? Component.BaselineResizeBehavior.CENTER_OFFSET
                    : Component.BaselineResizeBehavior.OTHER;
        }
    }
}
