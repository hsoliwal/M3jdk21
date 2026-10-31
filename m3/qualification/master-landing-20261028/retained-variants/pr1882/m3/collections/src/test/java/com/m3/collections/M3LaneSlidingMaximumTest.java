// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit admission for LC #239 original primitive index lane. */
final class M3LaneSlidingMaximumTest {
    @Test void sourceAndNativeSlidingMaximumParity() throws Exception {
        M3LaneSlidingMaximumContract.contracts();
    }
}
