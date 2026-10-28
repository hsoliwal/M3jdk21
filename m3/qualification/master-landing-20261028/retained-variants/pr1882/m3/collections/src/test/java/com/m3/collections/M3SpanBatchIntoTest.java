// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** JUnit entry for the original java.util-independent M3 native span owner. */
final class M3SpanBatchIntoTest {
    @Test void sourceAndNativeBatchWindowContracts() {
        assertTrue(M3SpanBatchIntoContract.runAll() > 10_000);
    }
}
