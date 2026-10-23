/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Original owning M3JDK21 collections JUnit contract entrypoint. */
final class M3SignalGraphComponentsTest {
    @Test
    void attributedSourceTypeSccAndGraphColumnsAgreeWithIndependentReachability() {
        M3SignalGraphComponentsSelfTest.main(new String[0]);
    }
}
