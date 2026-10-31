/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs.vfs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.AccessMode;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Differential JDK oracle for the M3-native byte and access mode masks. */
final class M3FsDonorFreePrimitivesTest {
    @Test void byteMasksMatchBitSetAlgebra() {
        Random random = new Random(0x4D335552494DL);
        for (int trial = 0; trial < 1024; trial++) {
            M3UriMask actual = new M3UriMask();
            BitSet expected = new BitSet(256);
            for (int step = 0; step < 24; step++) {
                int a = random.nextInt(256), b = random.nextInt(256);
                int low = Math.min(a, b), high = Math.max(a, b);
                switch (random.nextInt(4)) {
                    case 0 -> { actual.set(a); expected.set(a); }
                    case 1 -> { actual.clear(a); expected.clear(a); }
                    case 2 -> { actual.setInclusive(low, high); expected.set(low, high + 1); }
                    case 3 -> {
                        M3UriMask rhs = new M3UriMask().setInclusive(low, high);
                        BitSet reference = new BitSet(256);
                        reference.set(low, high + 1);
                        if (random.nextBoolean()) {
                            actual.or(rhs);
                            expected.or(reference);
                        } else {
                            actual.andNot(rhs);
                            expected.andNot(reference);
                        }
                    }
                    default -> throw new AssertionError();
                }
            }
            for (int i = 0; i < 256; i++) {
                assertEquals(expected.get(i), actual.get(i), "trial=" + trial + " byte=" + i);
            }
        }
        assertFalse(new M3UriMask().get(256));
        assertThrows(IndexOutOfBoundsException.class, () -> new M3UriMask().get(-1));
    }

    private static String oracle(AccessMode[] input) {
        try {
            AccessMode[] sorted = input.clone();
            if (sorted.length == 0) throw new IllegalArgumentException("Empty AccessMode[].");
            Arrays.sort(sorted);
            if (Arrays.binarySearch(sorted, AccessMode.WRITE) >= 0) return "READWRITE";
            if (Arrays.binarySearch(sorted, AccessMode.READ) >= 0) return "READ";
            throw new IllegalArgumentException(Arrays.toString(input));
        } catch (NullPointerException failure) {
            return "NullPointerException";
        } catch (IllegalArgumentException failure) {
            return "IllegalArgumentException:" + failure.getMessage();
        }
    }

    private static String actual(AccessMode[] input) {
        try {
            return RandomAccessMode.from(input).name();
        } catch (NullPointerException failure) {
            return "NullPointerException";
        } catch (IllegalArgumentException failure) {
            return "IllegalArgumentException:" + failure.getMessage();
        }
    }

    @Test void accessModesMatchLegacySortedSelection() {
        AccessMode[] values = {AccessMode.READ, AccessMode.WRITE, AccessMode.EXECUTE, null};
        for (int len = 0; len <= 6; len++) {
            for (int bits = 0; bits < (1 << (len * 2)); bits++) {
                AccessMode[] input = new AccessMode[len];
                int cursor = bits;
                for (int i = 0; i < len; i++) {
                    input[i] = values[cursor & 3];
                    cursor >>>= 2;
                }
                AccessMode[] baseline = input.clone();
                assertEquals(oracle(input), actual(input), Arrays.toString(input));
                assertTrue(Arrays.equals(baseline, input), "input must remain untouched");
            }
        }
        assertEquals("rw", RandomAccessMode.from(AccessMode.WRITE).getModeString());
        assertEquals("r", RandomAccessMode.from(AccessMode.READ).getModeString());
    }

    @Test void uriEscapesAndCharsetEdgesStayStable() {
        assertEquals("a%20b%2Bc/%25", URIUtils.encodePath("a b+c/%"));
        assertEquals("%C3%BCber", URIUtils.encodePath("über"));
        assertEquals("", URIUtils.encodePath("", "UTF-8"));
        assertThrows(IllegalArgumentException.class, () -> URIUtils.encodePath(null));
        assertThrows(IllegalArgumentException.class, () -> URIUtils.encodePath("x", ""));
        assertEquals(Messages.getString("m3fs.nonexistent.message.code", new Object[0]),
                Messages.getString("m3fs.nonexistent.message.code"));
    }
}
