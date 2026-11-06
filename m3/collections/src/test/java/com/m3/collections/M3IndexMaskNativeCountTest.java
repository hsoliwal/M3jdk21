/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Module-level Java21 contract check; JNI parity has its own explicit test mode. */
final class M3IndexMaskNativeCountTest {
    @Test void cardinalityParityWithExistingJdkShapedSelection() {
        M3IndexMaskNativeCountSelfTest.main(new String[0]);
    }
}
