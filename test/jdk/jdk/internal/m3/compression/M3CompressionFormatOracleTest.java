// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import org.junit.jupiter.api.Test;

/** Runs the format oracle (100,000 seeded inputs through both encoders and the strict reference decoders). */
final class M3CompressionFormatOracleTest {
    @Test
    void rawEncodersRoundTripThroughSpecDecoders() throws Throwable {
        System.out.print(M3CompressionFormatOracle.runAll());
    }
}
