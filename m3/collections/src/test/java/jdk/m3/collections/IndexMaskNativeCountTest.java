/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Module-level Java21 contract check; JNI parity has its own explicit test mode. */
final class IndexMaskNativeCountTest {
    @Test void cardinalityParityWithExistingJdkShapedSelection() {
        IndexMaskNativeCountSelfTest.main(new String[0]);
    }
}
