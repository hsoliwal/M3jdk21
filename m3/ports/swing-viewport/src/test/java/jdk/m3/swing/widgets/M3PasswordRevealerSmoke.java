/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.util.Arrays;
import javax.swing.ButtonModel;
import javax.swing.SwingUtilities;

/** Headless selected-behavior smoke, independent of SwingX and SWT jars. */
public final class M3PasswordRevealerSmoke {
    private M3PasswordRevealerSmoke() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3PasswordRevealerSmoke::exercise);
        System.out.println("M3_PASSWORD_REVEAL_PARITY_PASS");
    }

    private static void exercise() {
        M3PasswordRevealer p = new M3PasswordRevealer();
        p.getPasswordField().setText("never-printed");
        char mask = p.getPasswordField().getEchoChar();
        check(mask != '\0', "password starts masked");
        ButtonModel button = p.getRevealButton().getModel();
        button.setArmed(true);
        button.setPressed(true);
        check(p.isRevealed() && p.getPasswordField().getEchoChar() == '\0', "hold reveal");
        button.setArmed(false);
        check(!p.isRevealed() && p.getPasswordField().getEchoChar() == mask, "disarm hides");
        button.setPressed(false);
        p.setMode(M3PasswordRevealer.Mode.TOGGLE);
        p.getRevealButton().doClick();
        check(p.isRevealed(), "toggle reveals");
        p.getRevealButton().doClick();
        check(!p.isRevealed(), "toggle hides");
        p.setRevealed(true);
        p.setEnabled(false);
        check(!p.isRevealed() && p.getPasswordField().getEchoChar() == mask, "disable hides");
        p.setEnabled(true);
        char[] secret = p.getPasswordField().getPassword();
        try { check(Arrays.equals(secret, "never-printed".toCharArray()), "echo changes do not alter password"); }
        finally { Arrays.fill(secret, '\0'); }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
