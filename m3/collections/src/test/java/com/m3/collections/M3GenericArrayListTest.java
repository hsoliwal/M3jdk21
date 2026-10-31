/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Run the independent Java 21 differential oracle under the owning Maven test gate. */
public final class M3GenericArrayListTest {
    @Test public void objectArrayListAndIndexedSnapshot() throws Exception {
        M3GenericArrayListSelfTest.main(new String[0]);
    }
}
