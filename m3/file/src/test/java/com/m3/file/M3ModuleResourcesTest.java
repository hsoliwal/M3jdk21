/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.file;

import org.junit.jupiter.api.Test;

/** Maven entry point for the actual-owner resource and module-layer oracle. */
final class M3ModuleResourcesTest {
    @Test
    void resourceReaderAndModuleLayerContracts() throws Exception {
        M3ModuleResourcesSelfTest.main(Boolean.getBoolean("m3.test.native")
                ? new String[] {"--native"} : new String[0]);
    }
}
