/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import org.junit.jupiter.api.Test;

/** Executes the real Commons Collections 4.5.0 binary differential oracle. */
final class M3ApacheCollectionsTest {
    @Test void actualApacheListAndSetValuedContracts() {
        M3ApacheCollectionsContract.main(new String[0]);
    }
}
