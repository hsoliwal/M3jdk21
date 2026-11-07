// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compression;

import org.junit.jupiter.api.Test;

/** Runs the four-way encoding probe (encoder twin over both indexes; four-way wires through every decoder). */
final class M3LzFourWayEncodingProbeTest {
    @Test
    void fourWayIndexOutputRoundTrips() throws Throwable {
        System.out.print(M3LzFourWayEncodingProbe.runAll());
    }
}
