// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Real Surefire/JUnit entrypoint for the same bounded pure-Java oracle. */
//FLLDCIM: RELOCATE - donor blob ec0e3d9697a1 at fd9a233680b4; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
class InternalLeaseRecyclerTest {
    @Test
    void scopedReuseAndPressureSafety() throws Exception {
        InternalLeaseRecyclerSelfTest.main(new String[0]);
    }
}
