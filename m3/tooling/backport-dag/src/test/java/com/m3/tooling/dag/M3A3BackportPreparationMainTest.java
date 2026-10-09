// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class M3A3BackportPreparationMainTest {
    @Test
    void parsesOnlyCanonicalRootFile() {
        String root = "a".repeat(64);
        assertEquals(
                root,
                M3A3BackportPreparationMain.masteryRoot(
                        "ROOT  " + root + System.lineSeparator()));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3A3BackportPreparationMain.masteryRoot(root));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3A3BackportPreparationMain.masteryRoot("ROOT  " + "g".repeat(64)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3A3BackportPreparationMain.masteryRoot(""));
    }
}
