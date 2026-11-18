/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  The copyright holders designate this
 * particular file as subject to the "Classpath" exception as provided
 * in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import javax.swing.plaf.UIResource;

/**
 * A two-state switch backed by the standard toggle-button model.
 * The selected state is the on state. The normal toggle-button UI delegate
 * owns mouse and keyboard input, actions, focus, button groups and
 * accessibility. A switch icon provides the capsule, thumb and optional
 * on/off text; the inherited button text is a separate label.
 *
 * <p>The thumb is at the trailing end when selected and at the leading end
 * otherwise. Both the thumb and state text follow the component orientation.
 * The default icon follows the component font and look and feel. Applications
 * may replace it with {@link #setIcon(Icon)}; passing {@code null} restores
 * the switch presentation. An application icon that is not a
 * {@link UIResource} survives {@link #updateUI()}.
 *
 * <p>Selection, model and {@link Action} behavior is inherited from
 * {@link JToggleButton}. In particular, {@code setSelected} does not fire
 * an action event, while a completed user activation does. There is no
 * separate selection state or animation timer in this component.
 * Like other Swing components, this component should be accessed on the
 * event dispatch thread.
 *
 * @see JToggleButton
 * @see ButtonModel
 */
@SuppressWarnings("serial")
public class JToggleSwitch extends JToggleButton {
    @Serial
    private static final long serialVersionUID = 1L;

    private String onText = "On";
    private String offText = "Off";
    private boolean stateTextVisible = true;
    private Color onColor;
    private Color offColor;
    private Color thumbColor;

    /** Creates an unselected switch without a label. */
    public JToggleSwitch() {
        this(null, false);
    }

    /**
     * Creates a switch without a label.
     * @param selected the initial selection
     */
    public JToggleSwitch(boolean selected) {
        this(null, selected);
    }

    /**
     * Creates an unselected switch with a label.
     * @param text the label, or {@code null}
     */
    public JToggleSwitch(String text) {
        this(text, false);
    }

    /**
     * Creates a switch with a label and selection.
     * @param text the label, or {@code null}
     * @param selected the initial selection
     */
    @SuppressWarnings("this-escape") // Swing constructors initialize overridable UI properties.
    public JToggleSwitch(String text, boolean selected) {
        super(text, selected);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setRolloverEnabled(true);
        refreshSwitchIcon();
    }

    /**
     * Creates a switch configured by an action.
     * @param action the action, or {@code null}
     */
    @SuppressWarnings("this-escape") // Preserve the standard action-configuration hooks.
    public JToggleSwitch(Action action) {
        this();
        setAction(action);
    }

    /**
     * Installs the current look and feel's ordinary toggle-button delegate.
     * Application-owned icons are retained.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        if (getIcon() == null || getIcon() instanceof UIResource) {
            super.setIcon(createSwitchIcon());
        }
    }

    /**
     * Sets an application icon, or restores the switch icon when {@code null}.
     * This also supplies the switch icon when an action has no small icon.
     * @param icon the application icon, or {@code null}
     */
    @Override
    public void setIcon(Icon icon) {
        super.setIcon(icon == null ? createSwitchIcon() : icon);
    }

    /**
     * Sets the font and updates the default switch icon's preferred size.
     * @param font the font, or {@code null}
     */
    @Override
    public void setFont(Font font) {
        super.setFont(font);
        refreshSwitchIcon();
    }

    /** @return the text displayed inside the selected switch */
    public String getOnText() {
        return onText;
    }

    /**
     * Sets the selected state's text. Empty text is permitted.
     * @param text the non-null text
     * @throws NullPointerException if {@code text} is null
     */
    public void setOnText(String text) {
        Objects.requireNonNull(text, "text");
        String old = onText;
        if (!text.equals(old)) {
            onText = text;
            refreshSwitchIcon();
            firePropertyChange("onText", old, text);
        }
    }

    /** @return the text displayed inside the unselected switch */
    public String getOffText() {
        return offText;
    }

    /**
     * Sets the unselected state's text. Empty text is permitted.
     * @param text the non-null text
     * @throws NullPointerException if {@code text} is null
     */
    public void setOffText(String text) {
        Objects.requireNonNull(text, "text");
        String old = offText;
        if (!text.equals(old)) {
            offText = text;
            refreshSwitchIcon();
            firePropertyChange("offText", old, text);
        }
    }

    /** @return whether the state text is displayed inside the switch */
    public boolean isStateTextVisible() {
        return stateTextVisible;
    }

    /**
     * Controls state text without changing the separate button label.
     * @param visible whether state text is visible
     */
    public void setStateTextVisible(boolean visible) {
        boolean old = stateTextVisible;
        if (old != visible) {
            stateTextVisible = visible;
            refreshSwitchIcon();
            firePropertyChange("stateTextVisible", old, visible);
        }
    }

    /** @return the selected track color, or {@code null} for a UI default */
    public Color getOnColor() {
        return onColor;
    }

    /**
     * Sets the selected track color. Disabled rendering blends it with the
     * component background.
     * @param color the color, or {@code null} for a UI default
     */
    public void setOnColor(Color color) {
        Color old = onColor;
        if (!Objects.equals(old, color)) {
            onColor = color;
            repaint();
            firePropertyChange("onColor", old, color);
        }
    }

    /** @return the unselected track color, or {@code null} for a UI default */
    public Color getOffColor() {
        return offColor;
    }

    /**
     * Sets the unselected track color.
     * @param color the color, or {@code null} for a UI default
     */
    public void setOffColor(Color color) {
        Color old = offColor;
        if (!Objects.equals(old, color)) {
            offColor = color;
            repaint();
            firePropertyChange("offColor", old, color);
        }
    }

    /** @return the thumb color, or {@code null} for a UI default */
    public Color getThumbColor() {
        return thumbColor;
    }

    /**
     * Sets the thumb color.
     * @param color the color, or {@code null} for a UI default
     */
    public void setThumbColor(Color color) {
        Color old = thumbColor;
        if (!Objects.equals(old, color)) {
            thumbColor = color;
            repaint();
            firePropertyChange("thumbColor", old, color);
        }
    }

    private void refreshSwitchIcon() {
        if (getIcon() == null || getIcon() instanceof SwitchIcon) {
            super.setIcon(createSwitchIcon());
        }
    }

    private Icon createSwitchIcon() {
        Font font = getFont();
        FontMetrics metrics = font == null ? null : getFontMetrics(font);
        long height = metrics == null ? 24 : Math.max(24L, (long) metrics.getHeight() + 8);
        long textWidth = 0;
        if (stateTextVisible && metrics != null) {
            textWidth = Math.max(metrics.stringWidth(onText == null ? "" : onText),
                                 metrics.stringWidth(offText == null ? "" : offText));
        }
        int h = (int) Math.min(Integer.MAX_VALUE, height);
        int w = (int) Math.min(Integer.MAX_VALUE, Math.max(height * 2, height + textWidth + 12));
        return new SwitchIcon(w, h);
    }

    private static Color uiColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color == null ? fallback : color;
    }

    private static Color blend(Color color, Color background) {
        return new Color((color.getRed() + background.getRed()) / 2,
                         (color.getGreen() + background.getGreen()) / 2,
                         (color.getBlue() + background.getBlue()) / 2,
                         color.getAlpha());
    }

    private static final class SwitchIcon implements Icon, UIResource, Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private final int width;
        private final int height;

        SwitchIcon(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            if (!(component instanceof JToggleSwitch button)) {
                return;
            }
            ButtonModel model = button.getModel();
            boolean selected = model.isSelected();
            boolean thumbAtRight = selected == button.getComponentOrientation().isLeftToRight();
            Color background = button.getBackground();
            if (background == null) {
                background = uiColor("control", Color.LIGHT_GRAY);
            }
            Color track = selected ? button.onColor : button.offColor;
            if (track == null) {
                track = selected
                        ? uiColor("ToggleSwitch.onBackground",
                                uiColor("Component.accentColor", new Color(0x2563EB)))
                        : uiColor("ToggleSwitch.offBackground",
                                uiColor("controlShadow", Color.GRAY));
            }
            Color thumb = button.thumbColor;
            if (thumb == null) {
                thumb = uiColor("ToggleSwitch.thumb", Color.WHITE);
            }
            if (!model.isEnabled()) {
                track = blend(track, background);
                thumb = blend(thumb, background);
            } else if (model.isArmed() && model.isPressed()) {
                track = track.darker();
            }

            Graphics g = graphics.create();
            try {
                g.translate(x, y);
                g.clipRect(0, 0, width, height);
                if (g instanceof Graphics2D g2) {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                       RenderingHints.VALUE_ANTIALIAS_ON);
                }
                g.setColor(track);
                g.fillRoundRect(0, 0, width, height, height, height);
                int inset = Math.max(2, height / 8);
                int diameter = height - inset * 2;
                int thumbX = thumbAtRight ? width - inset - diameter : inset;
                g.setColor(thumb);
                g.fillOval(thumbX, inset, diameter, diameter);
                if (button.stateTextVisible) {
                    paintStateText(g, button, selected, thumbAtRight, track, inset, diameter);
                }
            } finally {
                g.dispose();
            }
        }

        private void paintStateText(Graphics g, JToggleSwitch button,
                                    boolean selected, boolean thumbAtRight,
                                    Color track, int inset, int diameter) {
            String text = selected ? button.onText : button.offText;
            if (text == null || text.isEmpty()) {
                return;
            }
            if (button.getFont() != null) {
                g.setFont(button.getFont());
            }
            FontMetrics metrics = g.getFontMetrics();
            int start = thumbAtRight ? inset : inset * 2 + diameter;
            int end = thumbAtRight ? width - inset * 2 - diameter : width - inset;
            int luminance = track.getRed() * 299 + track.getGreen() * 587 + track.getBlue() * 114;
            g.setColor(luminance >= 140000 ? Color.BLACK : Color.WHITE);
            g.clipRect(start, 0, Math.max(0, end - start), height);
            int textX = start + (end - start - metrics.stringWidth(text)) / 2;
            int textY = (height - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(text, textX, textY);
        }
    }
}
