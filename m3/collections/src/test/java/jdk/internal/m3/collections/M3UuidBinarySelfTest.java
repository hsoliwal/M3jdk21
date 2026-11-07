// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.UUID;

/** Dependency-free verification for binary UUID storage and pooling. */
//FLLDCIM: RELOCATE - donor blob 932c34ba9007 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public final class M3UuidBinarySelfTest {
    private static int checks;

    private M3UuidBinarySelfTest() { }

    public static void main(String[] args) {
        checks = 0;
        canonicalRoundTrip();
        System.out.println("PASS: " + checks + " UUID binary checks");
    }

    private static void canonicalRoundTrip() {
        String text = "123e4567-e89b-12d3-a456-426614174000";
        UUID uuid = UUID.fromString(text);
        M3Uuid128 value = M3Uuid128.fromUuid(uuid);

        check(value.toCanonicalString().equals(text), "canonical text boundary");
        check(value.toBigEndianBytes().length == M3Uuid128.BINARY_BYTES, "binary width");
        check(value.toCanonicalString().length() == M3Uuid128.TEXT_CHARS, "text width");
        check(M3Uuid128.fromBigEndian(value.toBigEndianBytes()).equals(value), "binary round trip");
        check(value.toUuid().equals(uuid), "JDK UUID round trip");

        boolean rejected = false;
        try {
            M3Uuid128.parse("123e4567-e89b-12d3-a456-42661417400");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "non-canonical UUID text rejected");

        byte[] target = new byte[32];
        value.writeBigEndian(target, 7);
        check(M3Uuid128.fromBigEndian(target, 7).equals(value), "offset binary round trip");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
