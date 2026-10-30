// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning JUnit5 binding for indexed original-array JNI differential proof. */
final class M3StateMaskHistogramIndexedNativeTest {
    @Test void indexedNativeAndJavaParity() throws Exception {
        M3StateMaskHistogramIndexedNativeContract.main(new String[0]);
    }
}
