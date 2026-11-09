/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.TreeWindowShiftContract;

/** Maven/JUnit discovery of the original M3 tree coordinate-shift oracle. */
public final class TreeWindowShiftTest {
    @Test
    void primitiveIndexPreflightAndResidentHandleParity() {
        TreeWindowShiftContract.main(new String[0]);
    }
}
