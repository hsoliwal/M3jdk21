/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

class M3PathMaskAliasTest {
    @Test void maskInputsRemainBorrowedAndWindowsRemainStable() {
        M3PathMaskAliasContract.run(M3IndexPathMasks.Backend.JAVA);
    }
}
