/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.FilteredNavigableSetRangeContract;

final class FilteredNavigableSetRangeTest {
    @Test
    void comparatorNullableBackingAndBoundedNavigation() {
        FilteredNavigableSetRangeContract.run();
    }
}
