/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/Surefire entrypoint for the same standalone Java 21 contract corpus. */
final class M3ReadOnlyMapDefaultsTest {
    @Test void defaultMethodsAndDerivedRanges() {
        M3ReadOnlyMapDefaultsContract.main(new String[0]);
    }
}
