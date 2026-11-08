// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import org.junit.jupiter.api.Test;

/** Runs the decoder agreement oracle (donor decoder pair vs the strict reference decoders: valid, malformed, fuzz). */
final class CompressionDecoderAgreementTest {
    @Test
    void donorDecodersAgreeWithReferenceDecoders() throws Throwable {
        System.out.print(CompressionDecoderAgreement.runAll());
    }
}
