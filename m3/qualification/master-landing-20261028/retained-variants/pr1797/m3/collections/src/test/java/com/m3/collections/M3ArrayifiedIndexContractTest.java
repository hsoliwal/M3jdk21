// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Default Java contract suite; native mode is an explicit, non-skipping runtime gate. */
public final class M3ArrayifiedIndexContractTest {
    @Test void allFourOperationContracts() { M3ArrayifiedIndexContract.run(false); }
}
