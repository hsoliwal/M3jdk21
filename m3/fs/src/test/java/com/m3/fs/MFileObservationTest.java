/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import org.junit.jupiter.api.Test;

/** Runs the same standalone evidence contract in the module's existing test suite. */
class MFileObservationTest {
    @Test
    void observationAndDeltaEvidenceContract() throws Exception {
        MFileObservationSelfTest.runAll();
    }
}
