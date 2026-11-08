/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.UuidBinarySelfTest;
/** Owning-module JUnit test replaying the second donor Uuid128 variant's round-trip oracle on M3Uuid128 (recipe immutable-pools-20261106). */
final class UuidBinaryTest {
    @Test void canonicalRoundTripOfTheOtherVariant() {
        UuidBinarySelfTest.main(new String[0]);
    }
}
