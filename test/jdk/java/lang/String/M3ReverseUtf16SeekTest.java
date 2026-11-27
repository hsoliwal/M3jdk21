/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only
 */

/*
 * @test
 * @summary Preserve exact bounded reverse seek for native-order UTF-16 bytes
 * @modules java.base/java.lang:+open
 * @run main M3ReverseUtf16SeekTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3ReverseUtf16SeekTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3ReverseUtf16SeekTest
 */

import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

/** Independent primitive oracle for the existing reverse seek atom. */
public class M3ReverseUtf16SeekTest {
    private static final byte LATIN1 = 0;
    private static final byte UTF16 = 1;
    private static final boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    private static Method seek;
    private static Method read;
    private static long checks;

    public static void main(String[] args) throws Exception {
        seek = Class.forName("java.lang.M3StringSearchPrecompute").getDeclaredMethod(
                "seekBackwards", byte[].class, byte.class, int.class, int.class, char.class);
        seek.setAccessible(true);
        read = seek.getDeclaringClass().getDeclaredMethod(
                "rawUnit", byte[].class, byte.class, int.class);
        read.setAccessible(true);
        for (int value = 0; value <= Character.MAX_VALUE; value++) {
            char unit = (char) value;
            char swapped = (char) ((unit << 8) | (unit >>> 8));
            char[] units = {unit, (char) (unit ^ 1), unit, swapped};
            byte[] bytes = encode(units, UTF16);
            compareRead(bytes, UTF16, 0, unit);
            compareRead(bytes, UTF16, 3, swapped);
            compare(units, bytes, UTF16, 3, 0, unit);
            compare(units, bytes, UTF16, 1, 1, unit);
            compare(units, bytes, UTF16, 0, 1, unit);
            compare(units, bytes, UTF16, 3, 2, swapped);
        }
        Random random = new Random(0x5345454b55544631L);
        char[] alphabet = {0, 1, 255, 256, 257, 0x0102, 0x0201, 0xd800, 0xdc00, 0xffff};
        for (byte coder : new byte[] {LATIN1, UTF16}) {
            for (int trial = 0; trial < 32; trial++) {
                char[] units = new char[random.nextInt(33)];
                for (int index = 0; index < units.length; index++) {
                    units[index] = coder == LATIN1 ? (char) random.nextInt(256)
                            : alphabet[random.nextInt(alphabet.length)];
                }
                byte[] bytes = encode(units, coder);
                byte[] original = bytes.clone();
                for (int index = 0; index < units.length; index++) {
                    compareRead(bytes, coder, index, units[index]);
                }
                for (int floor = 0; floor <= units.length; floor++) {
                    for (int top = floor - 1; top < units.length; top++) {
                        for (char wanted : alphabet) {
                            compare(units, bytes, coder, top, floor, wanted);
                        }
                        if (top >= floor) {
                            compare(units, bytes, coder, top, floor, units[top]);
                            compare(units, bytes, coder, top, floor, units[floor]);
                        }
                    }
                }
                if (!Arrays.equals(original, bytes)) throw new AssertionError("seek mutated source");
            }
        }
        System.out.println("M3_REVERSE_UTF16_SEEK_PASS checks=" + checks);
    }

    private static byte[] encode(char[] units, byte coder) {
        byte[] bytes = new byte[units.length << coder];
        for (int index = 0; index < units.length; index++) {
            char unit = units[index];
            if (coder == LATIN1) {
                bytes[index] = (byte) unit;
            } else {
                bytes[index * 2] = (byte) (BIG_ENDIAN ? unit >>> 8 : unit);
                bytes[index * 2 + 1] = (byte) (BIG_ENDIAN ? unit : unit >>> 8);
            }
        }
        return bytes;
    }

    private static void compareRead(byte[] bytes, byte coder, int index, char expected) throws Exception {
        char actual = (char) read.invoke(null, bytes, coder, index);
        checks++;
        if (actual != expected) throw new AssertionError("raw unit mismatch at " + checks);
    }

    private static void compare(char[] units, byte[] bytes, byte coder,
            int top, int floor, char wanted) throws Exception {
        int expected = -1;
        for (int index = top; index >= floor; index--) {
            if (units[index] == wanted) {
                expected = index;
                break;
            }
        }
        int actual = (int) seek.invoke(null, bytes, coder, top, floor, wanted);
        checks++;
        if (actual != expected) {
            throw new AssertionError("seek #" + checks + " coder=" + coder + " top=" + top
                    + " floor=" + floor + " unit=" + (int) wanted + " expected=" + expected + " actual=" + actual);
        }
    }
}
