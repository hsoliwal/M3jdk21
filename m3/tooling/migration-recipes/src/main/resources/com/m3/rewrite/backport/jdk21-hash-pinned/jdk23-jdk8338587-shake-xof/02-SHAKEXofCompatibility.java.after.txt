/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8338587
 * @summary Java-21 compatibility proof for nested SHAKE XOF and standalone SHAKE256
 * @modules java.base/sun.security.provider
 * @run main SHAKEXofCompatibility
 */

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import sun.security.provider.SHA3;
import sun.security.provider.SHAKE256;

public class SHAKEXofCompatibility {
    private static final HexFormat HEX = HexFormat.of();

    private static final byte[] EMPTY_SHAKE128_32 = HEX.parseHex(
            "7f9c2ba4e88f827d616045507605853e"
            + "d73b8093f6efbc88eb1a6eacfa66ef26");

    private static final byte[] EMPTY_SHAKE256_64 = HEX.parseHex(
            "46b9dd2b0ba88d13233b3feb743eeb24"
            + "3fcd52ea62b81b82b50c27646ed5762f"
            + "d75dc4ddd8c0f200cb05019d67b592f6"
            + "fc821c49479ab48640292eacb3b7c4be");

    public static void main(String[] args) {
        knownAnswer128();
        knownAnswer256();
        chunkedSqueezeMatchesDigest();
        standaloneCompatibilityWrapperMatchesNested();
    }

    private static void knownAnswer128() {
        var shake = new SHA3.SHAKE128(EMPTY_SHAKE128_32.length);
        assertBytes("SHAKE128 empty", EMPTY_SHAKE128_32, shake.digest());
    }

    private static void knownAnswer256() {
        var shake = new SHA3.SHAKE256(EMPTY_SHAKE256_64.length);
        assertBytes("SHAKE256 empty", EMPTY_SHAKE256_64, shake.digest());
    }

    private static void chunkedSqueezeMatchesDigest() {
        byte[] input = "M3JDK21-SHAKE-XOF".getBytes(StandardCharsets.UTF_8);

        var digest = new SHA3.SHAKE256(96);
        digest.update(input);
        byte[] expected = digest.digest();

        var squeeze = new SHA3.SHAKE256();
        squeeze.update(input);
        byte[] actual = new byte[96];
        squeeze.squeeze(actual, 0, 7);
        squeeze.squeeze(actual, 7, 33);
        squeeze.squeeze(actual, 40, 56);

        assertBytes("chunked squeeze", expected, actual);
    }

    private static void standaloneCompatibilityWrapperMatchesNested() {
        byte[] input = "legacy-wrapper".getBytes(StandardCharsets.UTF_8);

        var nested = new SHA3.SHAKE256(64);
        nested.update(input);

        var legacy = new SHAKE256(64);
        legacy.update(input);

        assertBytes("standalone compatibility", nested.digest(), legacy.digest());
    }

    private static void assertBytes(String label, byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    label + " expected=" + HEX.formatHex(expected)
                    + " actual=" + HEX.formatHex(actual));
        }
    }
}
