/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import org.junit.jupiter.api.Test;

/** Java 21 source-sealed CDateTime text/spinner parity leaf. */
class M3DateTimeParityTest {
    @Test
    void datetimeSpinnerContract() throws Exception {
        M3DateTimeSmoke.main(new String[0]);
    }
}
