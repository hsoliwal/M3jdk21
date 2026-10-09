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
 * @summary Atom bulk I/O (A11): every admission constructor stores units the readers return
 *          exactly (charAt, getChars, getBytes into both coders, javaHash), across the bulk
 *          threshold and the vectorized tail; mismatchUnits agrees with a reference loop for
 *          atoms and tuples with the difference at every position; mixed equals, compareTo,
 *          regionMatches and startsWith agree with the stock flat results.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3StringAtomBulkTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringAtomBulkTest
 */

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3StringAtomBulkTest {

    private static final int[] LENGTHS = {
        0, 1, 2, 3, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 127, 128, 129, 255, 256, 257, 511,
        512, 513, 1023, 1024, 1025, 2048, 4097
    };
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static long checks;
    private static Method admitChars;
    private static Method admitCompact;
    private static Method admitLatin1Bytes;
    private static Method admitCodePoints;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static Method getChars;
    private static Method getBytes;
    private static Method mismatchUnits;
    private static Method hashCodeValue;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        admissionAndReads();
        mismatch();
        consumers();
        System.out.println("M3StringAtomBulkTest checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        admitChars = accessible(m3.getDeclaredMethod("admit", char[].class, int.class, int.class));
        admitCompact = accessible(m3.getDeclaredMethod("admit", byte[].class, byte.class));
        admitLatin1Bytes = accessible(m3.getDeclaredMethod("admitLatin1Bytes", byte[].class, int.class, int.class));
        admitCodePoints = accessible(m3.getDeclaredMethod("admitCodePoints", int[].class, int.class, int.class));
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        getChars = accessible(m3.getDeclaredMethod("getChars", int.class, int.class, char[].class, int.class));
        getBytes = accessible(m3.getDeclaredMethod("getBytes", byte[].class, int.class, int.class, byte.class, int.class));
        mismatchUnits = accessible(m3.getDeclaredMethod("mismatchUnits", int.class, byte[].class, int.class, byte.class, int.class));
        hashCodeValue = accessible(m3.getDeclaredMethod("hashCodeValue"));
        valueOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
        coderOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("coder"));
        m3Offset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("m3"));
    }

    /** Every constructor, both widths, lengths across the bulk threshold: readers return the spelling. */
    private static void admissionAndReads() throws Exception {
        Random random = new Random(0x41544f4d42554c4bL);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 4; kind++) {
                char[] text = spell(random, length, kind);
                String flat = new String(text);
                for (Object storage : admissions(flat, text, random)) {
                    if (storage == null) continue;
                    readsAgree(storage, flat, "admission kind " + kind + " length " + length);
                }
            }
        }
    }

    private static Object[] admissions(String flat, char[] text, Random random) throws Exception {
        boolean latin1 = flat.chars().allMatch(unit -> unit < 0x100);
        byte[] compact = latin1 ? flat.getBytes(StandardCharsets.ISO_8859_1) : utf16(text);
        int[] codePoints = flat.codePoints().toArray();
        Object[] out = new Object[5];
        out[0] = text.length == 0 ? null : admitChars.invoke(null, text, 0, text.length);
        out[1] = text.length == 0 ? null : admitCompact.invoke(null, compact, latin1 ? (byte) 0 : (byte) 1);
        out[2] = latin1 && text.length != 0 ? admitLatin1Bytes.invoke(null, compact, 0, text.length) : null;
        out[3] = text.length == 0 ? null : admitCodePoints.invoke(null, codePoints, 0, codePoints.length);
        out[4] = text.length == 0 ? null : internChars.invoke(null, text, 0, text.length);
        return out;
    }

    private static void readsAgree(Object storage, String flat, String where) throws Exception {
        int length = flat.length();
        check(((CharSequence) storage).length() == length, "length " + where);
        check((int) hashCodeValue.invoke(storage) == flat.hashCode(), "javaHash " + where);
        for (int index = 0; index < length; index += Math.max(1, length / 17)) {
            check(((CharSequence) storage).charAt(index) == flat.charAt(index), "charAt " + where + "@" + index);
        }
        int[] begins = {0, Math.min(1, length), Math.min(7, length), Math.min(63, length), Math.min(64, length), Math.min(65, length), length};
        for (int begin : begins) {
            for (int end : begins) {
                if (end < begin) continue;
                int count = end - begin;
                char[] chars = new char[count + 3];
                getChars.invoke(storage, begin, end, chars, 2);
                check(new String(chars, 2, count).equals(flat.substring(begin, end)), "getChars " + where + " [" + begin + "," + end + ")");
                byte[] utf16 = new byte[(count + 3) << 1];
                getBytes.invoke(storage, utf16, begin, 1, (byte) 1, count);
                check(fromUtf16(utf16, 1, count).equals(flat.substring(begin, end)), "getBytes UTF16 " + where + " [" + begin + "," + end + ")");
                if (flat.chars().allMatch(unit -> unit < 0x100)) {
                    byte[] latin1 = new byte[count + 3];
                    getBytes.invoke(storage, latin1, begin, 2, (byte) 0, count);
                    check(new String(latin1, 2, count, StandardCharsets.ISO_8859_1).equals(flat.substring(begin, end)), "getBytes LATIN1 " + where);
                }
            }
        }
    }

    /** mismatchUnits against a reference loop: equal ranges, and the difference at every position. */
    private static void mismatch() throws Exception {
        Random random = new Random(0x4d49534d41544348L);
        int[] lengths = {1, 7, 8, 9, 16, 31, 32, 33, 64, 65, 255, 256, 257, 1024, 2048};
        for (int length : lengths) {
            for (int kind = 0; kind < 4; kind++) {
                char[] text = spell(random, length, kind);
                String flat = new String(text);
                Object atom = internChars.invoke(null, text, 0, length);
                Object tuple = length < 2 ? atom : concat.invoke(internChars.invoke(null, text, 0, length / 2), internChars.invoke(null, text, length / 2, length - length / 2));
                for (Object storage : new Object[] {atom, tuple}) {
                    for (int flatKind = 0; flatKind < 2; flatKind++) {
                        String other = flatKind == 0 ? new String(text) : widen(text);
                        byte[] value = (byte[]) UNSAFE.getReference(other, valueOffset);
                        byte coder = UNSAFE.getByte(other, coderOffset);
                        check((int) mismatchUnits.invoke(storage, 0, value, 0, coder, length) == -1, "equal " + length + " kind " + kind);
                        int step = Math.max(1, length / 24);
                        for (int position = 0; position < length; position += step) {
                            checkDifference(storage, other, position, length);
                        }
                        for (int position = Math.max(0, length - 9); position < length; position++) {
                            checkDifference(storage, other, position, length);
                        }
                        if (length > 3) {
                            int from = 1 + random.nextInt(length - 2);
                            int count = 1 + random.nextInt(length - from);
                            check((int) mismatchUnits.invoke(storage, from, value, from, coder, count) == -1, "equal range " + length);
                            Object range = slice.invoke(storage, from, from + count);
                            check((int) mismatchUnits.invoke(range, 0, value, from, coder, count) == -1, "equal slice " + length);
                        }
                    }
                }
            }
        }
    }

    private static void checkDifference(Object storage, String other, int position, int length) throws Exception {
        char[] changed = other.toCharArray();
        changed[position] = (char) (changed[position] ^ (position % 3 == 0 ? 1 : position % 3 == 1 ? 0x20 : 0x100));
        String differing = new String(changed);
        byte[] value = (byte[]) UNSAFE.getReference(differing, valueOffset);
        byte coder = UNSAFE.getByte(differing, coderOffset);
        int found = (int) mismatchUnits.invoke(storage, 0, value, 0, coder, length);
        check(found == position, "difference at " + position + " of " + length + " found " + found);
        if (position > 0) {
            int tail = (int) mismatchUnits.invoke(storage, position, value, position, coder, length - position);
            check(tail == 0, "difference at range start " + position);
            int before = (int) mismatchUnits.invoke(storage, 0, value, 0, coder, position);
            check(before == -1, "no difference before " + position);
        }
    }

    /** Mixed consumers over the atom paths agree with the stock flat results. */
    private static void consumers() throws Exception {
        Random random = new Random(0x434f4e53554d4552L);
        for (int round = 0; round < 3_000; round++) {
            int length = round % 7 == 0 ? 60 + random.nextInt(600) : random.nextInt(80);
            char[] text = spell(random, length, round % 4);
            String left = new String(text);
            String right = switch (round % 3) {
                case 0 -> new String(text);
                case 1 -> length == 0 ? "x" : differAt(text, random.nextInt(length));
                default -> new String(spell(random, random.nextInt(80), random.nextInt(4)));
            };
            String leftM3 = shaped(left, internChars.invoke(null, text, 0, length));
            check(leftM3.equals(right) == left.equals(right), "equals " + length);
            check(right.equals(leftM3) == right.equals(left), "equals reversed " + length);
            check(leftM3.compareTo(right) == left.compareTo(right), "compareTo " + length);
            check(right.compareTo(leftM3) == right.compareTo(left), "compareTo reversed " + length);
            check(leftM3.startsWith(right) == left.startsWith(right), "startsWith " + length);
            check(leftM3.endsWith(right) == left.endsWith(right), "endsWith " + length);
            if (length > 0 && !right.isEmpty()) {
                int len = 1 + random.nextInt(Math.min(length, right.length()));
                int toffset = random.nextInt(length - len + 1);
                int ooffset = random.nextInt(right.length() - len + 1);
                check(leftM3.regionMatches(toffset, right, ooffset, len) == left.regionMatches(toffset, right, ooffset, len), "regionMatches " + length);
                check(right.regionMatches(ooffset, leftM3, toffset, len) == right.regionMatches(ooffset, left, toffset, len), "regionMatches reversed " + length);
                check(leftM3.startsWith(right.substring(ooffset, ooffset + len), toffset) == left.startsWith(right.substring(ooffset, ooffset + len), toffset), "startsWith offset " + length);
            }
        }
    }

    private static char[] spell(Random random, int length, int kind) {
        char[] out = new char[length];
        for (int index = 0; index < length; index++) {
            out[index] = switch (kind) {
                case 0 -> (char) ('a' + random.nextInt(26));
                case 1 -> (char) (random.nextInt(0x100));
                case 2 -> (char) (0x100 + random.nextInt(0x700));
                default -> random.nextInt(9) == 0 ? (char) (0xd800 + random.nextInt(0x800)) : (char) random.nextInt(0x10000);
            };
        }
        return out;
    }

    private static String differAt(char[] text, int position) {
        char[] changed = text.clone();
        changed[position] = (char) (changed[position] ^ 1);
        return new String(changed);
    }

    private static String widen(char[] text) {
        char[] wide = new char[text.length + 1];
        System.arraycopy(text, 0, wide, 0, text.length);
        wide[text.length] = 'Ā';
        return new String(wide, 0, text.length);
    }

    private static byte[] utf16(char[] text) {
        String probe = new String(text);
        byte[] value = (byte[]) UNSAFE.getReference(probe, valueOffset);
        check(UNSAFE.getByte(probe, coderOffset) == 1, "utf16 probe coder");
        return value.clone();
    }

    private static String fromUtf16(byte[] value, int offset, int count) {
        char[] out = new char[count];
        for (int index = 0; index < count; index++) {
            int at = (offset + index) << 1;
            out[index] = UNSAFE.isBigEndian()
                    ? (char) (((value[at] & 0xff) << 8) | (value[at + 1] & 0xff))
                    : (char) ((value[at] & 0xff) | ((value[at + 1] & 0xff) << 8));
        }
        return new String(out);
    }

    private static String shaped(String flat, Object storage) {
        String out;
        try {
            out = (String) UNSAFE.allocateInstance(String.class);
        } catch (InstantiationException failure) {
            throw new AssertionError(failure);
        }
        UNSAFE.putReference(out, valueOffset, UNSAFE.getReference(flat, valueOffset));
        UNSAFE.putByte(out, coderOffset, UNSAFE.getByte(flat, coderOffset));
        UNSAFE.putReference(out, m3Offset, storage);
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
