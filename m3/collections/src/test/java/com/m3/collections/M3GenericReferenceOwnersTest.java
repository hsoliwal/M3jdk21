/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Actual Maven/JUnit contract on M3 array-backed generic Queue/Deque owners. */
final class M3GenericReferenceOwnersTest {
    @Test void jdk21ReferenceOwnerDifferential() {
        M3GenericReferenceOwnersProof.main(new String[0]);
    }
}
