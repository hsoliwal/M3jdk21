/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Runs the real scalar-view contract when the actual arrays JNI library is configured. */
@EnabledIfSystemProperty(named = "m3.arrays.native.path", matches = ".+")
final class ListMultiValueMapScalarContainsValueTest {
    @Test
    void scalarMembershipPreservesCallbacksAndBackedViews() {
        ListMultiValueMapScalarContainsValueContract.main(new String[0]);
    }
}
