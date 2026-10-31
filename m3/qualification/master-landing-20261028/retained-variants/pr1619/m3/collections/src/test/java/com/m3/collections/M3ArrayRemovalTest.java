// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the unchanged prior-API removal contract through the owning module's test provider. */
final class M3ArrayRemovalTest {
    @Test void preservedRemovalContract() throws Exception {
        M3ArrayRemovalContract.main(new String[0]);
    }
}
