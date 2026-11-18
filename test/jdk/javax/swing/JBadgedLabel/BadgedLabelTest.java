/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @summary Badged labels retain JLabel layout/UI/accessibility while adding a clipped RTL notification badge
 * @run main/othervm -Djava.awt.headless=true BadgedLabelTest
 */

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.ImageIcon;
import javax.swing.JBadgedLabel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class BadgedLabelTest {
    private static int checks;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void equal(Object expected, Object actual, String description) {
        check(expected == null ? actual == null : expected.equals(actual),
                description + " expected=" + expected + " actual=" + actual);
    }

    private static BufferedImage render(JLabel label, int width, int height) {
        label.setSize(width, height);
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            label.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static int diff(BufferedImage a, BufferedImage b) {
        int changed = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    changed++;
                }
            }
        }
        return changed;
    }

    private static void ordinaryLabelContract() {
        JLabel baseline = new JLabel("Notifications");
        JBadgedLabel candidate = new JBadgedLabel("Notifications");
        baseline.setHorizontalAlignment(SwingConstants.CENTER);
        candidate.setHorizontalAlignment(SwingConstants.CENTER);
        equal(baseline.getPreferredSize(), candidate.getPreferredSize(),
                "no-badge preferred size");
        equal(baseline.getInsets(), candidate.getInsets(), "no-badge insets");
        check(diff(render(baseline, 210, 60), render(candidate, 210, 60)) == 0,
                "no-badge JLabel pixels identical");
        equal(baseline.getAccessibleContext().getAccessibleRole(),
                candidate.getAccessibleContext().getAccessibleRole(),
                "original accessible role");
        equal(AccessibleRole.LABEL,
                candidate.getAccessibleContext().getAccessibleRole(),
                "label role");
        equal(baseline.isFocusable(), candidate.isFocusable(),
                "inherits ordinary JLabel focusability");
        check(candidate.getComponentCount() == 0, "no child widgets");

        BufferedImage icon = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
        baseline.setIcon(new ImageIcon(icon));
        candidate.setIcon(new ImageIcon(icon));
        equal(baseline.getPreferredSize(), candidate.getPreferredSize(),
                "text-plus-icon preferred size");
        check(diff(render(baseline, 210, 60), render(candidate, 210, 60)) == 0,
                "text-plus-icon UI painting identical");
    }

    private static void badgePropertiesAndGeometry() {
        JBadgedLabel candidate = new JBadgedLabel("Inbox");
        AtomicInteger changes = new AtomicInteger();
        candidate.addPropertyChangeListener("badgeValue", event -> changes.incrementAndGet());
        Dimension base = candidate.getPreferredSize();
        candidate.setBadgeValue("12");
        check(candidate.getPreferredSize().width > base.width,
                "badge reserves horizontal geometry");
        check(candidate.getPreferredSize().height > base.height,
                "badge reserves vertical geometry");
        Insets insets = candidate.getInsets();
        check(insets.right > 0 && insets.top > 0, "top-trailing inset reserve");
        check(candidate.getComponentCount() == 0, "badge adds no native/child widget");
        equal("Inbox", candidate.getText(), "label text preserved");
        equal("12", candidate.getBadgeValue(), "badge value");
        candidate.setBadgeValue("12");
        check(changes.get() == 1, "same value does not fire again");
        candidate.setBadgeForeground(Color.BLACK);
        candidate.setBadgeBackground(Color.YELLOW);
        equal(Color.BLACK, candidate.getBadgeForeground(), "foreground property");
        equal(Color.YELLOW, candidate.getBadgeBackground(), "background property");
        candidate.setBadgeValue("");
        equal(base, candidate.getPreferredSize(), "empty badge restores ordinary size");
        candidate.setBadgeValue(null);
        equal(base, candidate.getPreferredSize(), "null badge restores ordinary size");
        check(changes.get() == 3, "value changes reflected exactly");
    }

    private static void paintPlacementAndClip() {
        JBadgedLabel candidate = new JBadgedLabel("Messages");
        BufferedImage noBadge = render(candidate, 200, 70);
        candidate.setBadgeValue("9");
        BufferedImage ltr = render(candidate, 200, 70);
        check(diff(noBadge, ltr) > 50, "badge visibly painted");
        candidate.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        BufferedImage rtl = render(candidate, 200, 70);
        check(diff(ltr, rtl) > 50, "trailing badge mirrors in RTL");
        candidate.setBadgeVerticalAlignment(SwingConstants.BOTTOM);
        BufferedImage bottom = render(candidate, 200, 70);
        check(diff(rtl, bottom) > 50, "bottom corner changes painted geometry");
        candidate.setBadgeHorizontalAlignment(SwingConstants.RIGHT);
        check(candidate.getInsets().right > 0, "absolute RIGHT ignores RTL");

        BufferedImage clipped = new BufferedImage(200, 70,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = clipped.createGraphics();
        try {
            graphics.setColor(Color.MAGENTA);
            graphics.fillRect(0, 0, 200, 70);
            graphics.setClip(0, 0, 5, 5);
            candidate.paint(graphics);
        } finally {
            graphics.dispose();
        }
        equal(Color.MAGENTA.getRGB(), clipped.getRGB(190, 50),
                "inherited dirty-region clip preserved");
    }

    private static void accessibilityAndValidation() {
        JBadgedLabel candidate = new JBadgedLabel("Unread");
        AccessibleContext context = candidate.getAccessibleContext();
        context.setAccessibleDescription("Inbox status");
        AtomicInteger descriptions = new AtomicInteger();
        context.addPropertyChangeListener(event -> {
            if (AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY
                    .equals(event.getPropertyName())) {
                descriptions.incrementAndGet();
            }
        });
        candidate.setBadgeValue("4");
        equal("Unread", context.getAccessibleName(),
                "ordinary accessible name preserved");
        equal("Inbox status; Badge: 4", context.getAccessibleDescription(),
                "badge description added");
        check(descriptions.get() == 1, "description change announced once");
        candidate.setBadgeValue(null);
        equal("Inbox status", context.getAccessibleDescription(),
                "description restored when hidden");
        check(descriptions.get() == 2, "badge removal announced");

        int before = candidate.getBadgeHorizontalAlignment();
        try {
            candidate.setBadgeHorizontalAlignment(12345);
            throw new AssertionError("invalid horizontal alignment accepted");
        } catch (IllegalArgumentException expected) {
            equal(before, candidate.getBadgeHorizontalAlignment(),
                    "invalid horizontal alignment keeps state");
        }
        try {
            candidate.setBadgeVerticalAlignment(-300);
            throw new AssertionError("invalid vertical alignment accepted");
        } catch (IllegalArgumentException expected) {
            equal(SwingConstants.TOP, candidate.getBadgeVerticalAlignment(),
                    "invalid vertical alignment keeps state");
        }
    }

    private static void serializationAndLongBadge() {
        JBadgedLabel candidate = new JBadgedLabel("Count");
        candidate.setBadgeValue("1".repeat(1024));
        check(candidate.getPreferredSize().width > 0, "long badge size positive");
        candidate.setBadgeValue("25");
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(candidate);
            output.flush();
            try (ObjectInputStream input = new ObjectInputStream(
                    new ByteArrayInputStream(bytes.toByteArray()))) {
                JBadgedLabel restored = (JBadgedLabel) input.readObject();
                equal("25", restored.getBadgeValue(), "serialization badge state");
                equal("Count", restored.getText(), "serialization base label");
                equal(AccessibleRole.LABEL,
                        restored.getAccessibleContext().getAccessibleRole(),
                        "serialized accessibility");
            }
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    public static void main(String[] arguments) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var original = UIManager.getLookAndFeel();
            try {
                int families = 0;
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if (!info.getName().equals("Metal") && !info.getName().equals("Nimbus")) {
                        continue;
                    }
                    UIManager.setLookAndFeel(info.getClassName());
                    families++;
                    ordinaryLabelContract();
                    badgePropertiesAndGeometry();
                    paintPlacementAndClip();
                    accessibilityAndValidation();
                    serializationAndLongBadge();
                }
                check(families == 2, "Metal and Nimbus executed");
            } catch (Exception failure) {
                throw new RuntimeException(failure);
            } finally {
                try {
                    UIManager.setLookAndFeel(original);
                } catch (Exception failure) {
                    throw new RuntimeException(failure);
                }
            }
        });
        System.out.println("BadgedLabelTest: " + checks + " checks passed");
    }
}
