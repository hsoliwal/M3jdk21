/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3LongAddressBoundedTest {
    @Test void preservesOffsetsDuplicatesMasksAndPartialFailure() {
        M3LongAddressBoundedSelfTest.run();
    }
}
