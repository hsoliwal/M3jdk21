/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import org.junit.jupiter.api.Test;

/** Existing Swing viewport module's JUnit launcher discovers this gate. */
class M3NebulaComboParityTest {
    @Test
    void tableAndTreeSelectionAndLazyMillionRow() throws Exception {
        M3NebulaComboParitySmoke.main(new String[0]);
    }
    @Test
    void passwordRevealHoldAndToggle() throws Exception {
        M3PasswordRevealerSmoke.main(new String[0]);
    }
}
