/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Run the independent Java 21 differential oracle under the owning Maven test gate. */
public final class GenericArrayListTest {
    @Test public void objectArrayListAndIndexedSnapshot() throws Exception {
        GenericArrayListSelfTest.main(new String[0]);
    }
}
