/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Actual owner behavior against JDK 21 sorted/navigable interfaces. */
final class M3BoundedFilteredNavigationTest {
    @Test void boundedSetAndMapDifferential() {
        M3BoundedFilteredNavigationContract.run();
    }
}
