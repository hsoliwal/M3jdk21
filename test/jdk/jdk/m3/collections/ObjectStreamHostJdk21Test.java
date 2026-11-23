/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Exact class-backed JUnit qualification for host Java21 stream operators. */
public final class ObjectStreamHostJdk21Test {
    @Test
    void pairwiseAndOptionalMapWhileOnHostJdk21() {
        ObjectStreamHostJdk21SelfTest.main(new String[0]);
    }
}
