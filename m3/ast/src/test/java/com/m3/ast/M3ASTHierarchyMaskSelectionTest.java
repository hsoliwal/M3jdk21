// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import org.junit.jupiter.api.Test;

/** Maven/JUnit bridge to the exhaustive standalone Java21 differential. */
final class M3ASTHierarchyMaskSelectionTest {
    @Test void allFamilyAndStateMasksRemainStable() {
        M3ASTHierarchyMaskSelectionContract.main(new String[0]);
    }
}
