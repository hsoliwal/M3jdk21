/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.ImmutablePoolSelfTest;
/** Owning-module JUnit test of the relocated strong/weak canonical pools (recipe immutable-pools-20261106). */
final class ImmutablePoolTest {
    @Test void strongAndWeakLivePooling() throws Exception {
        ImmutablePoolSelfTest.main(new String[0]);
    }
}
