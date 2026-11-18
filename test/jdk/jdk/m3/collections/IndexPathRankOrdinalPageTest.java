/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entrypoint for source-ordinal bitmap paging regressions. */
public final class IndexPathRankOrdinalPageTest {
    @Test
    void randomizedSparseAndStridedBitmapPages() {
        IndexPathRankOrdinalPageSelfTest.main(new String[0]);
    }
}
