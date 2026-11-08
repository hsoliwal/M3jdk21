/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

class PathMaskAliasTest {
    @Test void maskInputsRemainBorrowedAndWindowsRemainStable() {
        PathMaskAliasContract.run(IndexPathMasks.Backend.JAVA);
    }
}
