/*
 * Copyright (c) 2026, Synexia contributors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

/*
 * @test
 * @bug 8338587
 * @summary Java 21 compatibility proof for nested SHAKE XOFs
 * @modules java.base/sun.security.provider
 * @run main SHAKEXofCompatibility
 */

import java.security.ProviderException;
import java.util.Arrays;
import java.util.HexFormat;

import sun.security.provider.SHA3;
import sun.security.provider.SHAKE256;

public class SHAKEXofCompatibility {
    private static final HexFormat HEX = HexFormat.of();

    public static void main(String[] args) {
        kat128(new byte[0],
                "7f9c2ba4e88f827d616045507605853ed73b8093f6efbc88eb1a6eacfa66ef263cb1eea988004b93103cfb0aeefd2a686e01fa4a58e8a3639ca8a1e3f9ae57e2");
        kat128("abc".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                "5881092dd818bf5cf8a3ddb793fbcba74097d5c526a6d35f97b83351940f2cc844c50af32acd3f2cdd066568706f509bc1bdde58295dae3f891a9a0fca578378");
        kat256(new byte[0],
                "46b9dd2b0ba88d13233b3feb743eeb243fcd52ea62b81b82b50c27646ed5762fd75dc4ddd8c0f200cb05019d67b592f6fc821c49479ab48640292eacb3b7c4be");
        kat256("abc".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                "483366601360a8771c6863080cc4114d8db44530f8f1e1ee4f94ea37e78b5739d5a15bef186a5386c75744c0527e1faa9f8726e462a12a4feb06bd8801e751e4");
        chunkedAndLegacyParity();
        resetAndPhaseContracts();
    }

    private static void kat128(byte[] input, String expected) {
        var xof = new SHA3.SHAKE128();
        xof.update(input);
        assertBytes(expected, xof.squeeze(64), "SHAKE128");
    }

    private static void kat256(byte[] input, String expected) {
        var xof = new SHA3.SHAKE256();
        xof.update(input);
        assertBytes(expected, xof.squeeze(64), "SHAKE256");
    }

    private static void chunkedAndLegacyParity() {
        byte[] input = new byte[32];
        for (int index = 0; index < input.length; index++) {
            input[index] = (byte) index;
        }

        var streaming = new SHA3.SHAKE256();
        streaming.update(input, 0, 7);
        streaming.update(input, 7, input.length - 7);
        byte[] first = streaming.squeeze(13);
        byte[] second = streaming.squeeze(51);
        byte[] combined = new byte[64];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);

        assertBytes(
                "69f07c8840ce80024db30939882c3d5bbc9c98b3e31e4513ebd2ca9b4503cdd3c9c90742452c7173d4a75ac49163e14ee0cc24ef7035b272d19a7af1099b333f",
                combined,
                "chunked SHAKE256");

        var fixed = new SHA3.SHAKE256(64);
        fixed.update(input);
        byte[] fixedDigest = fixed.digest();
        if (!Arrays.equals(combined, fixedDigest)) {
            throw new AssertionError("streaming/fixed SHAKE256 mismatch");
        }

        var legacy = new SHAKE256(64);
        legacy.update(input, 0, input.length);
        byte[] legacyDigest = legacy.digest();
        if (!Arrays.equals(combined, legacyDigest)) {
            throw new AssertionError("nested/legacy SHAKE256 mismatch");
        }
    }

    private static void resetAndPhaseContracts() {
        var xof = new SHA3.SHAKE128();
        xof.update((byte) 1);
        xof.squeeze(1);
        expectProviderException(() -> xof.update((byte) 2), "update after squeeze");

        xof.reset();
        xof.update((byte) 2);
        if (xof.squeeze(8).length != 8) {
            throw new AssertionError("reset did not restore absorbing state");
        }

        var fixed = new SHA3.SHAKE128(32);
        fixed.update(new byte[] {1, 2, 3});
        expectProviderException(() -> fixed.squeeze(1), "squeeze in fixed digest mode");
        if (fixed.digest().length != 32) {
            throw new AssertionError("fixed SHAKE128 digest length");
        }
    }

    private static void assertBytes(String expectedHex, byte[] actual, String label) {
        byte[] expected = HEX.parseHex(expectedHex);
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    label + " mismatch expected=" + expectedHex
                            + " actual=" + HEX.formatHex(actual));
        }
    }

    private static void expectProviderException(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError("missing ProviderException: " + label);
        } catch (ProviderException expected) {
            // expected
        }
    }
}
