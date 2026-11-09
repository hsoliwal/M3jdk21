/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.Uuid128SelfTest;
/** Owning-module JUnit test of the relocated 128-bit UUID record, vector and hash index (recipe immutable-pools-20261106). */
final class Uuid128Test {
    @Test void parseBytesOrderVectorIndexAndFootprint() throws Exception {
        Uuid128SelfTest.main(new String[0]);
    }
}
