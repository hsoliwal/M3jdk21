// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Owns the focused Java21 source/JNI boundary contract test. */
final class M3IndexedLiveViewTest {
    @Test void indexedLiveViews() { M3IndexedLiveViewContract.contracts(); }
}
