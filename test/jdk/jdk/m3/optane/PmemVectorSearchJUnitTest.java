// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import org.junit.jupiter.api.Test;

/** Full-sort oracle, input refusal, large offset and close-lease checks. */
public final class PmemVectorSearchJUnitTest {
    @Test void fileVectors() throws Exception { PmemVectorSearchSmokeTest.run(PmemBackend.FILE); }
    @Test void nativeVectors() throws Exception {
        if (Boolean.getBoolean("pmem.streaming.native")) PmemVectorSearchSmokeTest.run(PmemBackend.POSIX);
    }
}
