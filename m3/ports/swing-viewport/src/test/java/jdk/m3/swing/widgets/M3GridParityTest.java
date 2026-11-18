/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import org.junit.jupiter.api.Test;

/** Headless native Grid gate for the existing Swing viewport reactor. */
class M3GridParityTest {
    @Test
    void frozenColumnsHeaderSelectionAndMillionRows() throws Exception {
        M3GridContractSmoke.main(new String[0]);
    }
    @Test
    void frozenTopBottomRowsAndMillionRowWindow() throws Exception {
        M3GridPinnedRowsSmoke.main(new String[0]);
    }
}
