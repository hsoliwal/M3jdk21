/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Same oracle as the standalone strict-Java-21 execution, under the canonical JUnit lane. */
final class M3MutableFilteredViewsTest {
    @Test void liveMutableNavigableContracts() {
        M3MutableFilteredViewsContract.main(new String[0]);
    }
}
