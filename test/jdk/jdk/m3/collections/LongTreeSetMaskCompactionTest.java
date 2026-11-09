// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entry point for the exact standalone Java 21 oracle. */
final class LongTreeSetMaskCompactionTest {
    @Test
    void preserveSortedPagesAndCallbackExceptions() {
        LongTreeSetMaskCompactionContract.run();
    }
}