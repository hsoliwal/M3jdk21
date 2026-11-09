/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.PatchApplyRunsContract;

/** Source-owned JUnit execution of the primitive packed-patch run oracle. */
public final class PatchApplyRunsTest {
    @Test
    void stableSingleDestinationRunComposition() {
        PatchApplyRunsContract.main(new String[0]);
    }
}
