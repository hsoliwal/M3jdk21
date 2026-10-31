/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** One Maven/JUnit entrypoint for the independent Java/JNI differential corpus. */
public class M3SpanIndexTest {
    @Test public void compareAgainstSparseWidgetSpanOracle() {
        assertTrue(M3SpanIndexSelfTest.runAll() >= 128_000);
    }
}
