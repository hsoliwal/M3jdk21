// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Original M3JDK21 callback/exception-order contract, not an overlay. */
final class M3IndexSortStrictTest {
    @Test void strictOriginalCallbacksAndExceptionState() {
        M3IndexSortStrictContract.contracts();
    }
}
