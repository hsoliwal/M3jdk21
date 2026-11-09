/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Actual owner behavior against JDK 21 sorted/navigable interfaces. */
final class BoundedFilteredNavigationTest {
    @Test void boundedSetAndMapDifferential() {
        BoundedFilteredNavigationContract.run();
    }
}
