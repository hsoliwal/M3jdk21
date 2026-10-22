/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit integration for the existing M3JDK21 compiler signal/image owners. */
final class M3SourceMethodImageTest {
    @Test
    void methodEvidenceReaderAndLegacyV1Refusal() throws Exception {
        M3SourceMethodImageSelfTest.main(new String[0]);
    }
}
