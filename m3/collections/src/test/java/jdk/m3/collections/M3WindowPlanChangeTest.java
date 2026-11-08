/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.WindowPlanChangeContract;

/** Discovery hook: run the complete strict Java21 model-to-viewport delta oracle. */
public final class M3WindowPlanChangeTest {
    @Test
    public void windowChangeProjectionParity() {
        WindowPlanChangeContract.main(new String[0]);
    }
}
