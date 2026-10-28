// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Executes the standalone source-compatible superset oracle in the Maven reactor. */
final class M3ArrayListSupersetTest {
    @Test void preserveExplicitTrimAndPrimitiveRemovalInOneOriginalOwner() {
        M3ArrayListSupersetContract.main(new String[0]);
    }
}
