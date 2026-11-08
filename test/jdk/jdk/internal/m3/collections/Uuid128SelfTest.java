// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Dependency-free deterministic verification for 128-bit UUID representation. */
//FLLDCIM: RELOCATE - donor blob cc59afb69969 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public final class Uuid128SelfTest {
    private static int checks;

    private Uuid128SelfTest() { }

    public static void main(String[] args) throws Exception {
        checks = 0;
        parseFormatAndBytes();
        byteOrderIndependence();
        ordering();
        vectorAndHashIndex();
        storageFootprint();
        System.out.println("PASS: " + checks + " uuid128 checks");
    }

    private static void parseFormatAndBytes() {
        String text = "123e4567-e89b-42d3-a456-426614174000";
        Uuid128 value = Uuid128.parse(text);
        check(text.equals(value.toCanonicalString()), "canonical text round trip");
        check(value.toBigEndianBytes().length == 16, "binary UUID is exactly 16 bytes");
        check(value.version() == 4, "version nibble");
        check(value.variant() == 2, "RFC variant");
        check(value.toUuid().equals(UUID.fromString(text)), "java UUID parity");
        check(Uuid128.fromUuid(UUID.fromString(text)).equals(value), "from java UUID parity");
        check(Uuid128.parse(text.toUpperCase(java.util.Locale.ROOT)).equals(value), "uppercase input accepted");
        check(Uuid128.fromBigEndian(value.toBigEndianBytes()).equals(value), "byte round trip");
    }

    private static void byteOrderIndependence() {
        Uuid128 value = Uuid128.parse("00112233-4455-6677-8899-aabbccddeeff");
        byte[] expected = {
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
            (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb,
            (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff
        };

        ByteBuffer little = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        value.writeBigEndian(little);
        check(Arrays.equals(expected, little.array()), "binary bytes ignore target byte order");

        little.flip();
        little.order(ByteOrder.LITTLE_ENDIAN);
        check(Uuid128.fromBigEndian(little).equals(value), "binary read ignores source byte order");
    }

    private static void ordering() {
        Uuid128 a = Uuid128.parse("0fffffff-ffff-ffff-ffff-ffffffffffff");
        Uuid128 b = Uuid128.parse("10000000-0000-0000-0000-000000000000");
        Uuid128 c = Uuid128.parse("ffffffff-ffff-ffff-ffff-ffffffffffff");

        check(a.compareTo(b) < 0, "unsigned 128-bit order across high sign boundary");
        check(b.compareTo(c) < 0, "unsigned 128-bit max order");
        check(a.toCanonicalString().compareTo(b.toCanonicalString()) < 0, "text order parity");
        check(compareUnsigned(a.toBigEndianBytes(), b.toBigEndianBytes()) < 0, "binary order parity");
    }

    private static void vectorAndHashIndex() {
        Uuid128 a = Uuid128.parse("00000000-0000-0000-0000-000000000001");
        Uuid128 b = Uuid128.parse("00000000-0000-0000-0000-000000000002");
        Uuid128 c = Uuid128.parse("ffffffff-ffff-ffff-ffff-ffffffffffff");

        Uuid128Vector vector = Uuid128Vector.builder(2)
            .add(a).add(b).add(a).add(c)
            .build();
        check(vector.size() == 4, "vector size");
        check(vector.payloadBytes() == 64L, "vector payload exactly 16 bytes per occurrence");
        check(vector.equalsAt(2, a), "vector primitive equality");
        check(vector.indexOf(b) == 1, "vector indexOf");

        Uuid128HashIndex index = Uuid128HashIndex.build(vector, PoolProgressMonitor.none());
        check(index.size() == 3, "hash index deduplicates keys");
        check(index.firstPosition(a) == 0, "hash index keeps first position");
        check(index.firstPosition(c) == 3, "hash index lookup");
        check(index.firstPosition(Uuid128.parse("00000000-0000-0000-0000-000000000099")) == -1,
            "hash index miss");
    }

    private static void storageFootprint() {
        UuidStorageFootprint tenMillion = UuidStorageFootprint.forOccurrences(10_000_000L);
        check(tenMillion.binaryBytes() == 160_000_000L, "10M binary UUID payload");
        check(tenMillion.canonicalAsciiBytes() == 360_000_000L, "10M canonical text payload");
        check(tenMillion.savedBytes() == 200_000_000L, "10M raw payload saving");
        check(tenMillion.textToBinaryRatio() == 2.25d, "36/16 payload ratio");
    }

    private static int compareUnsigned(byte[] left, byte[] right) {
        for (int i = 0; i < left.length; i++) {
            int a = left[i] & 0xff;
            int b = right[i] & 0xff;
            if (a != b) return Integer.compare(a, b);
        }
        return Integer.compare(left.length, right.length);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
