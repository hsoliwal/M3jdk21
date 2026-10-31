/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;
import org.junit.jupiter.api.Test;

/** JUnit delegates to the exact Java21 standalone immutable indexed-view oracle. */
final class M3IndexedReadOnlyNoopTest {
    @Test void strictlyReadOnlySnapshots() {
        M3IndexedReadOnlyNoopProof.main(new String[0]);
    }
}
