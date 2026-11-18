/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

/**
 * Swing counterpart of Nebula PasswordRevealer's HOLD and PUSH behaviors.
 * One ordinary JPasswordField retains ownership of text, input, caret,
 * document, validators, look-and-feel, and accessibility. The revealer does
 * not obtain or copy the password into a String or an extra char array.
 */
public final class M3PasswordRevealer extends JPanel {
    private static final long serialVersionUID = 1L;

    /** HOLD reveals only while the button is pressed; TOGGLE persists until the next action. */
    public enum Mode { HOLD, TOGGLE }

    private final JPasswordField field = new JPasswordField();
    private final JButton eye = new JButton("\u25C9");
    private final char maskedEcho;
    private Mode mode;
    private boolean revealed;

    public M3PasswordRevealer() { this(Mode.HOLD); }

    public M3PasswordRevealer(Mode mode) {
        super(new BorderLayout(0, 0));
        this.mode = java.util.Objects.requireNonNull(mode, "mode");
        maskedEcho = field.getEchoChar();
        eye.setFocusable(true);
        eye.getAccessibleContext().setAccessibleName("Reveal password");
        add(field, BorderLayout.CENTER);
        add(eye, BorderLayout.EAST);
        // A ButtonModel supports mouse and keyboard without global event hooks.
        eye.getModel().addChangeListener(e -> {
            if (this.mode == Mode.HOLD) {
                boolean hold = eye.getModel().isArmed() && eye.getModel().isPressed();
                setRevealed(hold);
            }
        });
        eye.addActionListener(e -> {
            if (this.mode == Mode.TOGGLE) setRevealed(!revealed);
        });
    }

    public JPasswordField getPasswordField() { return field; }
    public JButton getRevealButton() { return eye; }
    public Mode getMode() { return mode; }
    public boolean isRevealed() { return revealed; }

    public void setMode(Mode mode) {
        requireEdt();
        Mode next = java.util.Objects.requireNonNull(mode, "mode");
        if (this.mode != next) {
            this.mode = next;
            setRevealed(false);
        }
    }

    /** Controls presentation only. Password contents are not read or copied. */
    public void setRevealed(boolean show) {
        requireEdt();
        boolean actual = show && isEnabled();
        if (revealed != actual) {
            revealed = actual;
            field.setEchoChar(actual ? '\0' : maskedEcho);
            eye.getAccessibleContext().setAccessibleDescription(actual ? "Password visible" : "Password hidden");
        }
    }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (field != null) field.setEnabled(enabled);
        if (eye != null) eye.setEnabled(enabled);
        if (!enabled && revealed) setRevealed(false);
    }

    @Override public void removeNotify() {
        if (revealed) setRevealed(false);
        super.removeNotify();
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Swing EDT required");
    }
}
