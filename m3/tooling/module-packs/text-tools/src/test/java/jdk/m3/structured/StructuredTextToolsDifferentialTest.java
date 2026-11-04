// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.structured;

import org.junit.jupiter.api.Test;

/** Every UTF-16 code unit plus retained CSV/INI/environment contracts. */
final class StructuredTextToolsDifferentialTest {
    @Test void originalBehaviorAndEnvironmentClassifier() {
        int count = StructuredTextToolsContract.runAll();
        if (count != 131293) throw new AssertionError("contract corpus count drift: " + count);
    }
}
