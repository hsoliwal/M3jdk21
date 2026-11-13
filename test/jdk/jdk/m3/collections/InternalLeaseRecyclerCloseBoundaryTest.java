// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit bridge for the source-sealed independent Java21 lifecycle oracle. */
//FLLDCIM: RELOCATE - donor blob 734504ab2201 at fd9a233680b4; recipe make/m3/collections/recipe/transient-facade-20261106/relocate.py v4
class InternalLeaseRecyclerCloseBoundaryTest {
    @Test
    void correctReentrantOwnerCloseAndPreserveReusableLeases() throws Exception {
        InternalLeaseRecyclerCloseBoundarySelfTest.main(new String[0]);
    }
}
