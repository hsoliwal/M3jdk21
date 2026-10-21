/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.pcollections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit bridge to the same dependency-free executable contract fixtures. */
class M3PCollectionsTest {
    @Test void vectorContracts() throws Exception { M3PVectorContract.main(new String[0]); }
    @Test void hashContracts() throws Exception { M3PHashContract.run(); }
    @Test void derivedContracts() throws Exception { M3PDerivedContract.run(); }
    @Test void viewContracts() throws Exception { M3PViewsContract.run(); }
}
