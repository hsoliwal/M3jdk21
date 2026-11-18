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
 * @summary Toggle switches retain Swing toggle, action and model contracts and render both orientations
 * @run main/othervm -Djava.awt.headless=true ToggleSwitchTest
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DebugGraphics;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToggleSwitch;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ToggleSwitchTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LookAndFeel original = UIManager.getLookAndFeel();
            try {
                int lookAndFeels = 0;
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if (!info.getName().equals("Metal") && !info.getName().equals("Nimbus")) {
                        continue;
                    }
                    UIManager.setLookAndFeel(info.getClassName());
                    lookAndFeels++;
                    modelAndEventParity();
                    keyboardAndMouse();
                    actionAndIconOwnership();
                    propertiesAndPainting();
                    accessibilityAndSerialization();
                }
                check(lookAndFeels == 2, "Metal and Nimbus both executed");
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            } finally {
                try {
                    UIManager.setLookAndFeel(original);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        });
        System.out.println("ToggleSwitchTest: " + checks + " checks passed");
    }

    private static void modelAndEventParity() {
        JToggleButton baseline = new JToggleButton("Power");
        JToggleSwitch candidate = new JToggleSwitch("Power");
        List<String> expected = recordEvents(baseline);
        List<String> actual = recordEvents(candidate);
        baseline.doClick(0);
        candidate.doClick(0);
        equal(expected, actual, "completed activation event sequence");
        check(candidate.isSelected(), "click selects");

        expected.clear();
        actual.clear();
        baseline.setSelected(false);
        candidate.setSelected(false);
        equal(expected, actual, "programmatic selection event sequence");
        check(actual.stream().noneMatch(value -> value.startsWith("action:")),
              "setSelected emits no action");

        expected.clear();
        actual.clear();
        baseline.getModel().setSelected(true);
        candidate.getModel().setSelected(true);
        equal(expected, actual, "direct model selection");

        baseline.setEnabled(false);
        candidate.setEnabled(false);
        expected.clear();
        actual.clear();
        baseline.doClick(0);
        candidate.doClick(0);
        equal(expected, actual, "disabled doClick");
        check(candidate.isSelected(), "disabled click keeps selection");

        candidate.setEnabled(true);
        ButtonModel previous = candidate.getModel();
        JToggleButton.ToggleButtonModel replacement = new JToggleButton.ToggleButtonModel();
        candidate.setModel(replacement);
        check(candidate.getModel() == replacement, "caller model identity retained");
        previous.setSelected(false);
        previous.setSelected(true);
        check(!candidate.isSelected(), "detached model cannot change selection");
        candidate.doClick(0);
        check(replacement.isSelected(), "click updates replacement model");

        ButtonGroup group = new ButtonGroup();
        JToggleButton peer = new JToggleButton("Peer");
        group.add(candidate);
        group.add(peer);
        peer.setSelected(true);
        check(peer.isSelected() && !candidate.isSelected(), "group selects peer exclusively");
        candidate.doClick(0);
        check(candidate.isSelected() && !peer.isSelected(), "group selects switch exclusively");
        candidate.doClick(0);
        check(candidate.isSelected(), "group keeps selected member on repeated click");
    }

    private static List<String> recordEvents(JToggleButton button) {
        List<String> events = new ArrayList<>();
        button.addChangeListener(event -> events.add("change:" + state(button)));
        button.addItemListener(event -> events.add("item:" + event.getStateChange() + ':' + state(button)));
        button.addActionListener(event -> events.add("action:" + event.getActionCommand() + ':' + state(button)));
        return events;
    }

    private static String state(JToggleButton button) {
        ButtonModel model = button.getModel();
        return model.isSelected() + ":" + model.isArmed() + ":" + model.isPressed();
    }

    private static void keyboardAndMouse() {
        JToggleSwitch button = new JToggleSwitch("Activate");
        int[] actions = {0};
        button.addActionListener(event -> actions[0]++);
        key(button, "pressed SPACE");
        check(button.getModel().isPressed() && button.getModel().isArmed(), "space arms and presses");
        check(!button.isSelected(), "space press alone does not select");
        key(button, "released SPACE");
        check(button.isSelected() && actions[0] == 1, "space release selects once");

        button.setSize(button.getPreferredSize());
        mouse(button, MouseEvent.MOUSE_PRESSED, button.getWidth() / 2, button.getHeight() / 2);
        mouse(button, MouseEvent.MOUSE_RELEASED, button.getWidth() / 2, button.getHeight() / 2);
        check(!button.isSelected() && actions[0] == 2, "mouse activation selects once");

        mouse(button, MouseEvent.MOUSE_PRESSED, button.getWidth() / 2, button.getHeight() / 2);
        mouse(button, MouseEvent.MOUSE_EXITED, -3, -3);
        mouse(button, MouseEvent.MOUSE_RELEASED, -3, -3);
        check(!button.isSelected() && actions[0] == 2, "drag outside cancels activation");

        button.setEnabled(false);
        key(button, "pressed SPACE");
        key(button, "released SPACE");
        mouse(button, MouseEvent.MOUSE_PRESSED, button.getWidth() / 2, button.getHeight() / 2);
        mouse(button, MouseEvent.MOUSE_RELEASED, button.getWidth() / 2, button.getHeight() / 2);
        check(!button.isSelected() && actions[0] == 2, "disabled mouse and keyboard do not activate");

        button.setEnabled(true);
        int listeners = button.getMouseListeners().length;
        for (int iteration = 0; iteration < 8; iteration++) {
            button.updateUI();
        }
        equal(listeners, button.getMouseListeners().length, "UI refresh does not accumulate mouse listeners");
        key(button, "pressed SPACE");
        key(button, "released SPACE");
        check(button.isSelected() && actions[0] == 3, "UI refresh retains one activation");
    }

    private static void key(JToggleButton button, String stroke) {
        Object binding = button.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(stroke));
        check(binding != null, "installed key binding: " + stroke);
        Action action = button.getActionMap().get(binding);
        check(action != null, "installed key action: " + stroke);
        action.actionPerformed(new ActionEvent(button, ActionEvent.ACTION_PERFORMED, binding.toString()));
    }

    private static void mouse(JToggleButton button, int id, int x, int y) {
        int which = id == MouseEvent.MOUSE_EXITED ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1;
        button.dispatchEvent(new MouseEvent(button, id, System.currentTimeMillis(), 0,
                                            x, y, 1, false, which));
    }

    private static void actionAndIconOwnership() {
        int[] invocations = {0};
        Action action = new AbstractAction("Connected") {
            @Override
            public void actionPerformed(ActionEvent event) {
                equal("toggle-command", event.getActionCommand(), "action command retained");
                invocations[0]++;
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, "toggle-command");
        action.putValue(Action.SELECTED_KEY, Boolean.TRUE);
        action.putValue(Action.SHORT_DESCRIPTION, "Connection state");
        JToggleSwitch button = new JToggleSwitch(action);
        check(button.isSelected(), "Action.SELECTED_KEY initializes state");
        equal("Connected", button.getText(), "action supplies label");
        equal("Connection state", button.getToolTipText(), "action supplies tooltip");
        check(button.getIcon() != null, "action without icon retains switch");
        button.doClick(0);
        check(!button.isSelected() && invocations[0] == 1, "action activation changes state once");
        equal(Boolean.FALSE, action.getValue(Action.SELECTED_KEY), "selection writes back to action");
        action.putValue(Action.SELECTED_KEY, Boolean.TRUE);
        check(button.isSelected(), "action selected update reaches model");
        action.setEnabled(false);
        check(!button.isEnabled(), "action enabled state propagated");
        action.setEnabled(true);

        Icon custom = new Icon() {
            @Override public int getIconWidth() { return 13; }
            @Override public int getIconHeight() { return 11; }
            @Override public void paintIcon(Component component, Graphics graphics, int x, int y) { }
        };
        action.putValue(Action.SMALL_ICON, custom);
        check(button.getIcon() == custom, "action icon identity retained");
        button.updateUI();
        check(button.getIcon() == custom, "application icon survives look-and-feel refresh");
        button.setOnText("Available");
        button.setFont(button.getFont().deriveFont(18f));
        check(button.getIcon() == custom, "state label and font changes preserve application icon");
        action.putValue(Action.SMALL_ICON, null);
        check(button.getIcon() != null && button.getIcon() != custom, "removing action icon restores switch");
        button.setIcon(custom);
        button.setIcon(null);
        check(button.getIcon() != null && button.getIcon() != custom, "null icon restores switch");
        button.setDisabledIcon(custom);
        check(button.getDisabledIcon() == custom, "application disabled icon retained");
        button.setAction(null);
        check(button.getAction() == null && button.getIcon() != null, "detaching action restores switch");
    }

    private static void propertiesAndPainting() {
        JToggleSwitch button = new JToggleSwitch("State");
        check(!button.isSelected(), "default is off");
        equal("On", button.getOnText(), "default on text");
        equal("Off", button.getOffText(), "default off text");
        check(button.isStateTextVisible(), "state text enabled by default");
        check(button.isFocusPainted(), "standard focus painting enabled");
        List<String> properties = new ArrayList<>();
        button.addPropertyChangeListener(event -> properties.add(event.getPropertyName()));
        int originalWidth = button.getIcon().getIconWidth();
        button.setOnText("Connection available");
        check(button.getIcon().getIconWidth() > originalWidth, "long state text increases preferred width");
        button.setOffText("Unavailable");
        button.setOnColor(Color.RED);
        button.setOffColor(Color.BLUE);
        button.setThumbColor(Color.GREEN);
        button.setStateTextVisible(false);
        for (String property : List.of("onText", "offText", "onColor", "offColor",
                                       "thumbColor", "stateTextVisible")) {
            check(properties.contains(property), "bound property: " + property);
        }
        properties.clear();
        button.setOnColor(Color.RED);
        button.setStateTextVisible(false);
        check(properties.isEmpty(), "same property values do not fire events");
        expectNull(() -> button.setOnText(null));
        expectNull(() -> button.setOffText(null));
        equal("Connection available", button.getOnText(), "null failure preserves property");

        for (boolean leftToRight : new boolean[] {true, false}) {
            button.setComponentOrientation(leftToRight
                    ? ComponentOrientation.LEFT_TO_RIGHT : ComponentOrientation.RIGHT_TO_LEFT);
            for (boolean selected : new boolean[] {false, true}) {
                button.setSelected(selected);
                BufferedImage image = iconImage(button);
                int height = image.getHeight();
                int left = height / 2;
                int right = image.getWidth() - height / 2;
                int thumbX = selected == leftToRight ? right : left;
                int trackX = selected == leftToRight ? left : right;
                equal(Color.GREEN.getRGB(), image.getRGB(thumbX, height / 2), "thumb follows state and orientation");
                equal((selected ? Color.RED : Color.BLUE).getRGB(), image.getRGB(trackX, height / 2),
                      "track follows selected color");
                check((image.getRGB(0, 0) >>> 24) == 0, "capsule corner stays transparent");
            }
        }
        button.setEnabled(false);
        BufferedImage disabled = iconImage(button);
        check(disabled.getRGB(disabled.getHeight() / 2, disabled.getHeight() / 2) != Color.GREEN.getRGB(),
              "disabled thumb has distinct presentation");
        check(button.isSelected(), "disabled presentation retains selected model");
        button.setEnabled(true);
        button.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        button.setStateTextVisible(true);
        button.setFont(new Font(Font.DIALOG, Font.PLAIN, 24));
        check(button.getIcon().getIconHeight() >= button.getFontMetrics(button.getFont()).getHeight(),
              "state icon accommodates larger font");
        button.setSize(button.getPreferredSize());
        BufferedImage complete = new BufferedImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = complete.createGraphics();
        try {
            button.paint(graphics);
        } finally {
            graphics.dispose();
        }
        check(nonTransparentPixels(complete) > 0, "full UI delegate paints component");
        BufferedImage debugImage = new BufferedImage(button.getIcon().getIconWidth(),
                button.getIcon().getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D debugDelegate = debugImage.createGraphics();
        DebugGraphics debug = new DebugGraphics(debugDelegate);
        try {
            button.getIcon().paintIcon(button, debug, 0, 0);
        } finally {
            debug.dispose();
        }
        check(nonTransparentPixels(debugImage) > 0, "non-Graphics2D debug wrapper paints icon");
        button.updateUI();
        equal("Connection available", button.getOnText(), "state text survives UI update");
        equal(Color.RED, button.getOnColor(), "custom state color survives UI update");
        check(button.isSelected(), "selection survives UI update");
        button.setOnColor(null);
        button.setOffColor(null);
        button.setThumbColor(null);
        check(button.getOnColor() == null && button.getOffColor() == null && button.getThumbColor() == null,
              "null colors restore UI defaults");
        check(nonTransparentPixels(iconImage(button)) > 0, "UI default colors paint");
    }

    private static BufferedImage iconImage(JToggleSwitch button) {
        Icon icon = button.getIcon();
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.MAGENTA);
            AffineTransform before = graphics.getTransform();
            icon.paintIcon(button, graphics, 0, 0);
            equal(before, graphics.getTransform(), "icon preserves graphics transform");
            equal(Color.MAGENTA, graphics.getColor(), "icon preserves graphics color");
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static int nonTransparentPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void accessibilityAndSerialization() throws Exception {
        JToggleSwitch button = new JToggleSwitch("Airplane", true);
        equal(AccessibleRole.TOGGLE_BUTTON, button.getAccessibleContext().getAccessibleRole(),
              "standard accessible toggle role");
        check(button.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.CHECKED),
              "selected accessibility state");
        check(button.getAccessibleContext().getAccessibleAction().doAccessibleAction(0),
              "accessible activation supported");
        check(!button.isSelected(), "accessible action updates ordinary model");

        button.setSelected(true);
        button.setOnText("Enabled");
        button.setOffText("Disabled");
        button.setOnColor(Color.RED);
        button.setOffColor(Color.BLUE);
        button.setThumbColor(Color.WHITE);
        button.setStateTextVisible(false);
        button.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(button);
        }
        JToggleSwitch restored;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (JToggleSwitch) input.readObject();
        }
        check(restored.isSelected(), "serialization retains selection");
        equal("Airplane", restored.getText(), "serialization retains button label");
        equal("Enabled", restored.getOnText(), "serialization retains on text");
        equal("Disabled", restored.getOffText(), "serialization retains off text");
        equal(Color.RED, restored.getOnColor(), "serialization retains color");
        check(!restored.isStateTextVisible(), "serialization retains state text flag");
        check(!restored.getComponentOrientation().isLeftToRight(), "serialization retains RTL");
        check(nonTransparentPixels(iconImage(restored)) > 0, "deserialized icon paints");
        restored.doClick(0);
        check(!restored.isSelected(), "deserialized button activates");
        button.doClick(0);
        check(!button.isSelected(), "original button remains usable after serialization");
    }

    private static void expectNull(Runnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("expected NullPointerException");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual), message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message + " [" + UIManager.getLookAndFeel().getName() + "]");
        }
    }
}
