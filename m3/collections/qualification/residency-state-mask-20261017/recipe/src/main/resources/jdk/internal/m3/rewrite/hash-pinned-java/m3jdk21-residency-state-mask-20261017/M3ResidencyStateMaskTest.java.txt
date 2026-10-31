// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Existing M3JDK21 JUnit contract lane; native parity has its own real JNI CI job. */
final class M3ResidencyStateMaskTest {
    @Test void deterministicJavaStateMaskAndIndexContract() {
        M3ResidencyStateMaskProof.main(new String[0]);
    }
}
