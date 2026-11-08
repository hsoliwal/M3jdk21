// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.LaneSlidingMaximumContract;

/** JUnit admission for LC #239 original primitive index lane. */
//FLLDCIM: RELOCATE - donor blob 987dde85f026 at d5ad5090d87e; recipe m3/collections/qualification/census2-t1-20261106/relocate.py v5
final class M3LaneSlidingMaximumTest {
    @Test void sourceAndNativeSlidingMaximumParity() throws Exception {
        LaneSlidingMaximumContract.contracts();
    }
}
