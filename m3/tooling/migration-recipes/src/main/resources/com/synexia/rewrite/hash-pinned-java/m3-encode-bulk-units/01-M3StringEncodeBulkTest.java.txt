/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
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
 */

/*
 * @test
 * @summary Encoding M3-backed Strings from one bulk-read unit array (A13): getBytes for UTF-8,
 *          ISO-8859-1, US-ASCII, UTF-16BE/LE, UTF-16, windows-1252 and the no-replacement
 *          encoders agree byte for byte with the stock flat results over atoms, nested tuples and
 *          slices holding ASCII, Latin-1, BMP, surrogate pairs and lone surrogates; the
 *          no-replacement entry (String.getBytesNoRepl, M3 dispatch inside) returns the same
 *          bytes and refuses with the same exception class as for a flat String.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3StringEncodeBulkTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringEncodeBulkTest
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3StringEncodeBulkTest {

    private static final Charset[] CHARSETS = {
        StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, StandardCharsets.US_ASCII,
        StandardCharsets.UTF_16BE, StandardCharsets.UTF_16LE, StandardCharsets.UTF_16,
        Charset.forName("windows-1252")
    };
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static Method stockEncodeNoRepl;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        charsets();
        noReplacement();
        System.out.println("M3StringEncodeBulkTest checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        stockEncodeNoRepl = accessible(String.class.getDeclaredMethod("getBytesNoRepl", String.class, Charset.class));
        valueOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
        coderOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("coder"));
        m3Offset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("m3"));
    }

    /** Every charset over every shape and content kind: bytes identical to the flat result. */
    private static void charsets() throws Exception {
        Random random = new Random(0x454e434f44455f31L);
        int[] lengths = {0, 1, 2, 3, 7, 8, 9, 31, 32, 33, 63, 64, 65, 255, 256, 257, 1000, 2048, 4099};
        for (int length : lengths) {
            for (int kind = 0; kind < 5; kind++) {
                String text = spell(random, length, kind);
                Object[] shapes = text.isEmpty() ? new Object[0] : new Object[] {atom(text), tupleOf(text, random, 1), tupleOf(text, random, 3), slicedShape(text, random)};
                for (Object storage : shapes) {
                    String m3 = shaped(text, storage);
                    for (Charset charset : CHARSETS) {
                        byte[] expected = text.getBytes(charset);
                        check(Arrays.equals(expected, m3.getBytes(charset)), "getBytes(" + charset + ") length " + length + " kind " + kind);
                    }
                    check(Arrays.equals(text.getBytes(), m3.getBytes()), "getBytes() length " + length + " kind " + kind);
                    check(Arrays.equals(text.getBytes("UTF-8"), m3.getBytes("UTF-8")), "getBytes(name) length " + length + " kind " + kind);
                }
            }
        }
    }

    /** The no-replacement encoders: same bytes, and refusal exactly where the stock one refuses. */
    private static void noReplacement() throws Exception {
        Random random = new Random(0x4e4f5245504c4143L);
        for (int round = 0; round < 1_500; round++) {
            int length = 1 + (round % 7 == 0 ? 64 + random.nextInt(900) : random.nextInt(60));
            String text = spell(random, length, random.nextInt(5));
            Object storage = round % 3 == 0 ? atom(text) : round % 3 == 1 ? tupleOf(text, random, 2) : slicedShape(text, random);
            for (Charset charset : new Charset[] {StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, StandardCharsets.US_ASCII, StandardCharsets.UTF_16BE}) {
                byte[] expected;
                Exception expectedFailure = null;
                try {
                    expected = (byte[]) stockEncodeNoRepl.invoke(null, text, charset);
                } catch (InvocationTargetException failure) {
                    expected = null;
                    expectedFailure = (Exception) failure.getCause();
                }
                byte[] actual;
                Exception actualFailure = null;
                try {
                    actual = (byte[]) stockEncodeNoRepl.invoke(null, shaped(text, storage), charset);
                } catch (InvocationTargetException failure) {
                    actual = null;
                    actualFailure = (Exception) failure.getCause();
                }
                String where = "encodeNoRepl(" + charset + ") round " + round + " length " + length;
                if (expectedFailure == null) {
                    check(actualFailure == null && Arrays.equals(expected, actual), where + (actualFailure == null ? " bytes differ" : " refused: " + actualFailure));
                } else {
                    check(actualFailure != null, where + " expected refusal " + expectedFailure);
                    check(actualFailure != null && actualFailure.getClass() == expectedFailure.getClass(), where + " refusal kind " + actualFailure);
                }
            }
        }
    }

    private static String spell(Random random, int length, int kind) {
        StringBuilder out = new StringBuilder(length);
        while (out.length() < length) {
            switch (kind) {
                case 0 -> out.append((char) ('a' + random.nextInt(26)));
                case 1 -> out.append((char) (random.nextInt(5) == 0 ? 0x80 + random.nextInt(0x80) : 'a' + random.nextInt(26)));
                case 2 -> out.append((char) (random.nextInt(3) == 0 ? 0x100 + random.nextInt(0xd700) : 'a' + random.nextInt(26)));
                case 3 -> {
                    if (random.nextInt(4) == 0 && out.length() + 2 <= length) out.appendCodePoint(0x10000 + random.nextInt(0x10000));
                    else out.append((char) (random.nextInt(2) == 0 ? 0x100 + random.nextInt(0x700) : 'a' + random.nextInt(26)));
                }
                default -> out.append(switch (random.nextInt(6)) {
                    case 0 -> (char) (0xd800 + random.nextInt(0x400));
                    case 1 -> (char) (0xdc00 + random.nextInt(0x400));
                    case 2 -> (char) random.nextInt(0x100);
                    default -> (char) random.nextInt(0x10000);
                });
            }
        }
        return out.substring(0, length);
    }

    private static Object atom(String text) throws Exception {
        char[] chars = text.toCharArray();
        return internChars.invoke(null, chars, 0, chars.length);
    }

    private static Object tupleOf(String text, Random random, int depth) throws Exception {
        if (depth == 0 || text.length() < 2) return atom(text);
        int cut = 1 + random.nextInt(text.length() - 1);
        return concat.invoke(tupleOf(text.substring(0, cut), random, depth - 1), tupleOf(text.substring(cut), random, depth - 1));
    }

    private static Object slicedShape(String text, Random random) throws Exception {
        String padded = "épre" + text + "post\ud83d";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        return slice.invoke(whole, 4, 4 + text.length());
    }

    private static String shaped(String flat, Object storage) throws Exception {
        String out = (String) UNSAFE.allocateInstance(String.class);
        UNSAFE.putReference(out, valueOffset, UNSAFE.getReference(flat, valueOffset));
        UNSAFE.putByte(out, coderOffset, UNSAFE.getByte(flat, coderOffset));
        UNSAFE.putReference(out, m3Offset, storage);
        check(out.length() == flat.length(), "shaped length");
        return out;
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
