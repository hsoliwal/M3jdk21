/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  The copyright holders designate
 * this particular file as subject to the "Classpath" exception as provided
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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.Serial;
import java.util.Objects;
import javax.accessibility.AccessibleContext;

/**
 * A Swing label with a notification badge at one of its four corners.
 *
 * <p>The ordinary {@link JLabel} look-and-feel delegate still paints the label,
 * icon, mnemonic and HTML text. The badge is painted by this same lightweight
 * component, with no child widget, native peer, raster cache or second viewport.
 * A {@code null} or empty badge value hides it and restores the original label
 * insets. The inherited component orientation resolves leading/trailing badge
 * placement. Badge text is included in the accessible description.
 *
 * <p>This is an M3JDK21 additive Swing widget, not a Java SE 21 API. Callers
 * use the ordinary Swing event-dispatch-thread policy.
 */
@SuppressWarnings("serial")
public class JBadgedLabel extends JLabel {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final int MIN_BADGE_SIZE = 18;
    private static final int BADGE_PADDING = 6;
    private static final int BADGE_GAP = 4;
    private static final Font FALLBACK_FONT = new Font(Font.DIALOG, Font.BOLD, 12);

    private String badgeValue;
    private Color badgeForeground = Color.WHITE;
    private Color badgeBackground = new Color(0, 123, 255);
    private int badgeHorizontalAlignment = TRAILING;
    private int badgeVerticalAlignment = TOP;

    /** Creates an empty label without a badge. */
    public JBadgedLabel() {
        super();
    }

    /** Creates a label with ordinary JLabel text and no badge. */
    public JBadgedLabel(String text) {
        super(text);
    }

    /** Creates a label with an ordinary JLabel icon and no badge. */
    public JBadgedLabel(Icon icon) {
        super(icon);
    }

    /** Creates a label with text and an icon, using the ordinary JLabel layout. */
    public JBadgedLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
    }

    /** Returns the badge value, or {@code null} if it is absent. */
    public String getBadgeValue() {
        return badgeValue;
    }

    /**
     * Sets the badge's text; {@code null} or empty hides the badge.
     * Does not modify {@link #getText()} or the label's icon.
     */
    public void setBadgeValue(String value) {
        String old = badgeValue;
        if (Objects.equals(old, value)) {
            return;
        }
        String oldDescription = accessibleContext == null
                ? null : accessibleContext.getAccessibleDescription();
        badgeValue = value;
        firePropertyChange("badgeValue", old, value);
        if (accessibleContext != null) {
            accessibleContext.firePropertyChange(
                    AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY,
                    oldDescription, accessibleContext.getAccessibleDescription());
        }
        revalidate();
        repaint();
    }

    /** Returns the badge's foreground color. */
    public Color getBadgeForeground() {
        return badgeForeground;
    }

    /** Sets its foreground color, independently of the label foreground. */
    public void setBadgeForeground(Color color) {
        Color next = Objects.requireNonNull(color, "color");
        Color old = badgeForeground;
        badgeForeground = next;
        firePropertyChange("badgeForeground", old, next);
        repaint();
    }

    /** Returns the badge's background color. */
    public Color getBadgeBackground() {
        return badgeBackground;
    }

    /** Sets its background color, independently of the label background. */
    public void setBadgeBackground(Color color) {
        Color next = Objects.requireNonNull(color, "color");
        Color old = badgeBackground;
        badgeBackground = next;
        firePropertyChange("badgeBackground", old, next);
        repaint();
    }

    /** Returns LEADING, TRAILING, LEFT or RIGHT for badge placement. */
    public int getBadgeHorizontalAlignment() {
        return badgeHorizontalAlignment;
    }

    /** Sets the badge side; LEADING/TRAILING follow component orientation. */
    public void setBadgeHorizontalAlignment(int alignment) {
        if (alignment != LEADING && alignment != TRAILING
                && alignment != LEFT && alignment != RIGHT) {
            throw new IllegalArgumentException("badge horizontal alignment: " + alignment);
        }
        int old = badgeHorizontalAlignment;
        badgeHorizontalAlignment = alignment;
        firePropertyChange("badgeHorizontalAlignment", old, alignment);
        repaint();
    }

    /** Returns TOP or BOTTOM for badge placement. */
    public int getBadgeVerticalAlignment() {
        return badgeVerticalAlignment;
    }

    /** Sets TOP or BOTTOM for badge placement. */
    public void setBadgeVerticalAlignment(int alignment) {
        if (alignment != TOP && alignment != BOTTOM) {
            throw new IllegalArgumentException("badge vertical alignment: " + alignment);
        }
        int old = badgeVerticalAlignment;
        badgeVerticalAlignment = alignment;
        firePropertyChange("badgeVerticalAlignment", old, alignment);
        repaint();
    }

    @Override
    public Insets getInsets() {
        return getInsets(new Insets(0, 0, 0, 0));
    }

    @Override
    public Insets getInsets(Insets insets) {
        Insets result = super.getInsets(insets);
        if (hasBadge()) {
            int width = badgeWidth() + BADGE_GAP;
            int height = badgeHeight() + BADGE_GAP;
            if (badgeAtRight()) {
                result.right = saturatingAdd(result.right, width);
            } else {
                result.left = saturatingAdd(result.left, width);
            }
            if (badgeVerticalAlignment == TOP) {
                result.top = saturatingAdd(result.top, height);
            } else {
                result.bottom = saturatingAdd(result.bottom, height);
            }
        }
        return result;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (!hasBadge() || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Graphics painting = graphics.create();
        try {
            if (painting instanceof Graphics2D graphics2D) {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
            }
            Font font = badgeFont();
            painting.setFont(font);
            FontMetrics metrics = painting.getFontMetrics(font);
            int width = badgeWidth(metrics);
            int height = badgeHeight(metrics);
            Insets border = super.getInsets(new Insets(0, 0, 0, 0));
            int x = badgeAtRight()
                    ? getWidth() - border.right - width : border.left;
            int y = badgeVerticalAlignment == TOP
                    ? border.top : getHeight() - border.bottom - height;
            painting.setColor(badgeBackground);
            painting.fillRoundRect(x, y, width, height, height, height);
            painting.setColor(isEnabled() ? badgeForeground : getForeground());
            int textWidth = Math.max(0, metrics.stringWidth(badgeValue));
            int baseline = y + (height - metrics.getHeight()) / 2
                    + metrics.getAscent();
            painting.clipRect(x, y, width, height);
            painting.drawString(badgeValue, x + (width - textWidth) / 2, baseline);
        } finally {
            painting.dispose();
        }
    }

    /** Exposes the badge value without replacing JLabel's accessible name. */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJBadgedLabel();
        }
        return accessibleContext;
    }

    protected class AccessibleJBadgedLabel extends AccessibleJLabel {
        @Serial
        private static final long serialVersionUID = 1L;

        /** Creates the accessible context for this label. */
        protected AccessibleJBadgedLabel() {
            super();
        }

        @Override
        public String getAccessibleDescription() {
            String ordinary = super.getAccessibleDescription();
            if (!hasBadge()) {
                return ordinary;
            }
            String detail = "Badge: " + badgeValue;
            return ordinary == null || ordinary.isEmpty()
                    ? detail : ordinary + "; " + detail;
        }
    }

    private boolean hasBadge() {
        return badgeValue != null && !badgeValue.isEmpty();
    }

    private boolean badgeAtRight() {
        return switch (badgeHorizontalAlignment) {
            case LEFT -> false;
            case RIGHT -> true;
            case LEADING -> !getComponentOrientation().isLeftToRight();
            default -> getComponentOrientation().isLeftToRight();
        };
    }

    private Font badgeFont() {
        Font font = getFont() == null ? FALLBACK_FONT : getFont();
        return font.deriveFont(Font.BOLD, Math.max(8.0f, font.getSize2D() * 0.75f));
    }

    private int badgeWidth() {
        return badgeWidth(getFontMetrics(badgeFont()));
    }

    private int badgeWidth(FontMetrics metrics) {
        return Math.max(MIN_BADGE_SIZE,
                saturatingAdd(Math.max(0, metrics.stringWidth(badgeValue)),
                        2 * BADGE_PADDING));
    }

    private int badgeHeight() {
        return badgeHeight(getFontMetrics(badgeFont()));
    }

    private static int badgeHeight(FontMetrics metrics) {
        return Math.max(MIN_BADGE_SIZE,
                saturatingAdd(Math.max(0, metrics.getHeight()), BADGE_PADDING));
    }

    private static int saturatingAdd(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }
}
