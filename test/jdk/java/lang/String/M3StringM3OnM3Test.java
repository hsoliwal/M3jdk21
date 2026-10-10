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
 * @summary M3-on-M3 comparisons in place (A12): two M3 ranges (atoms, nested tuples of mixed
 *          widths, slices) are compared leaf atom by leaf atom natively; equals, compareTo,
 *          startsWith, endsWith and regionMatches with storage on both sides agree with the stock
 *          flat results; mismatchStorages and mismatchAtom find the difference at every position;
 *          the pool's compact-value and char-array lookups still find their atoms and refuse
 *          every differing candidate.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3StringM3OnM3Test
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringM3OnM3Test
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3StringM3OnM3Test {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static Method mismatchStorages;
    private static Method mismatchAtom;
    private static Method contentEqualsBytes;
    private static Method contentEqualsChars;
    private static Method contentEqualsCompact;
    private static Field ownerField;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        consumers();
        mismatchEveryPosition();
        poolLookups();
        System.out.println("M3StringM3OnM3Test checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> atom = Class.forName("java.lang.M3StringAtom");
        Class<?> compare = Class.forName("java.lang.M3StringMixedCompare");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        mismatchStorages = accessible(compare.getDeclaredMethod("mismatchStorages", m3, int.class, m3, int.class, int.class));
        mismatchAtom = accessible(atom.getDeclaredMethod("mismatchAtom", int.class, atom, int.class, int.class));
        contentEqualsBytes = accessible(atom.getDeclaredMethod("contentEquals", byte[].class, byte.class));
        contentEqualsChars = accessible(atom.getDeclaredMethod("contentEquals", char[].class, int.class, int.class, byte.class));
        contentEqualsCompact = accessible(atom.getDeclaredMethod("contentEqualsCompactBytes", byte[].class, int.class, int.class, byte.class, byte.class));
        ownerField = m3.getDeclaredField("owner");
        ownerField.setAccessible(true);
        valueOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
        coderOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("coder"));
        m3Offset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("m3"));
    }

    /** Both sides carry storage of random shapes: every consumer agrees with the stock result. */
    private static void consumers() throws Exception {
        Random random = new Random(0x4d334f4e4d335f31L);
        for (int round = 0; round < 4_000; round++) {
            int length = round % 9 == 0 ? 100 + random.nextInt(2_000) : random.nextInt(70);
            String left = spell(random, length);
            String right = switch (round % 4) {
                case 0 -> new String(left.toCharArray());
                case 1 -> length == 0 ? "y" : differAt(left, random.nextInt(length), random);
                case 2 -> length < 2 ? left + "z" : left.substring(0, random.nextInt(length));
                default -> spell(random, random.nextInt(70));
            };
            Object leftStorage = shape(left, random);
            Object rightStorage = shape(right, random);
            String a = shaped(left, leftStorage);
            String b = shaped(right, rightStorage);
            String where = " round " + round + " length " + length;
            check(a.equals(b) == left.equals(right), "equals" + where);
            check(b.equals(a) == right.equals(left), "equals reversed" + where);
            check(a.contentEquals(b) == left.contentEquals(right), "contentEquals" + where);
            check(a.compareTo(b) == left.compareTo(right), "compareTo" + where);
            check(b.compareTo(a) == right.compareTo(left), "compareTo reversed" + where);
            check(a.startsWith(b) == left.startsWith(right), "startsWith" + where);
            check(a.endsWith(b) == left.endsWith(right), "endsWith" + where);
            check(a.compareToIgnoreCase(b) == left.compareToIgnoreCase(right), "compareToIgnoreCase" + where);
            check(a.equalsIgnoreCase(b) == left.equalsIgnoreCase(right), "equalsIgnoreCase" + where);
            if (length > 0 && !right.isEmpty()) {
                int len = 1 + random.nextInt(Math.min(length, right.length()));
                int toffset = random.nextInt(length - len + 1);
                int ooffset = random.nextInt(right.length() - len + 1);
                check(a.regionMatches(toffset, b, ooffset, len) == left.regionMatches(toffset, right, ooffset, len), "regionMatches" + where);
                check(a.regionMatches(true, toffset, b, ooffset, len) == left.regionMatches(true, toffset, right, ooffset, len), "regionMatches(true)" + where);
                check(a.startsWith(right.substring(ooffset, ooffset + len), toffset) == left.startsWith(right.substring(ooffset, ooffset + len), toffset), "startsWith offset" + where);
                String sub = shaped(right.substring(ooffset, ooffset + len), slice.invoke(rightStorage, ooffset, ooffset + len));
                check(a.startsWith(sub, toffset) == left.startsWith(right.substring(ooffset, ooffset + len), toffset), "startsWith M3 slice" + where);
            }
        }
    }

    /** mismatchStorages over every shape pair finds the difference at every position. */
    private static void mismatchEveryPosition() throws Exception {
        Random random = new Random(0x4d49534d41544348L);
        int[] lengths = {1, 2, 7, 8, 9, 15, 16, 17, 63, 64, 65, 255, 256, 257, 600, 2048};
        for (int length : lengths) {
            for (int kind = 0; kind < 3; kind++) {
                String text = kind == 0 ? latin(random, length) : kind == 1 ? wide(random, length) : spell(random, length);
                Object[] shapes = {atom(text), tupleOf(text, random, 1), tupleOf(text, random, 3), slicedShape(text, random)};
                for (Object leftStorage : shapes) {
                    for (Object rightStorage : shapes) {
                        check((int) mismatchStorages.invoke(null, leftStorage, 0, rightStorage, 0, length) == -1, "equal shapes " + length + " kind " + kind);
                        int step = Math.max(1, length / 16);
                        for (int position = 0; position < length; position += step) {
                            checkDifference(leftStorage, text, position, random);
                        }
                        for (int position = Math.max(0, length - 9); position < length; position++) {
                            checkDifference(leftStorage, text, position, random);
                        }
                    }
                }
                Object a = atom(text);
                Object b = atom(differAt(text, length - 1, random));
                int found = (int) mismatchAtom.invoke(ownerField.get(a), 0, ownerField.get(b), 0, length);
                check(found == length - 1, "mismatchAtom last unit " + length + " kind " + kind + " found " + found);
                check((int) mismatchAtom.invoke(ownerField.get(a), 0, ownerField.get(a), 0, length) == -1, "mismatchAtom self " + length);
            }
        }
    }

    private static void checkDifference(Object leftStorage, String text, int position, Random random) throws Exception {
        String differing = differAt(text, position, random);
        Object rightStorage = position % 2 == 0 ? atom(differing) : tupleOf(differing, random, 1 + position % 3);
        int length = text.length();
        int found = (int) mismatchStorages.invoke(null, leftStorage, 0, rightStorage, 0, length);
        check(found == position, "difference at " + position + " of " + length + " found " + found);
        if (position > 0) {
            check((int) mismatchStorages.invoke(null, leftStorage, position, rightStorage, position, length - position) == 0, "difference at range start " + position);
            check((int) mismatchStorages.invoke(null, leftStorage, 0, rightStorage, 0, position) == -1, "no difference before " + position);
        }
    }

    /** The pool finds the same atom for an equal spelling and refuses every differing candidate. */
    private static void poolLookups() throws Exception {
        Random random = new Random(0x504f4f4c4c4f4f4bL);
        for (int round = 0; round < 1_500; round++) {
            int length = 1 + (round % 5 == 0 ? 64 + random.nextInt(600) : random.nextInt(70));
            String text = round % 3 == 0 ? latin(random, length) : round % 3 == 1 ? wide(random, length) : spell(random, length);
            char[] chars = text.toCharArray();
            Object first = internChars.invoke(null, chars, 0, length);
            Object again = internChars.invoke(null, chars.clone(), 0, length);
            check(ownerField.get(first) == ownerField.get(again), "pool hit returns the same atom " + round);
            Object owner = ownerField.get(first);
            byte coder = UNSAFE.getByte(text, coderOffset);
            byte[] value = (byte[]) UNSAFE.getReference(text, valueOffset);
            check((boolean) contentEqualsBytes.invoke(owner, value, coder), "contentEquals(byte[]) equal " + round);
            check((boolean) contentEqualsChars.invoke(owner, chars, 0, length, coder), "contentEquals(char[]) equal " + round);
            check((boolean) contentEqualsCompact.invoke(owner, value, 0, length, coder, coder), "contentEqualsCompactBytes equal " + round);
            int position = random.nextInt(length);
            String differing = differAt(text, position, random);
            if (UNSAFE.getByte(differing, coderOffset) == coder) {
                byte[] otherValue = (byte[]) UNSAFE.getReference(differing, valueOffset);
                check(!(boolean) contentEqualsBytes.invoke(owner, otherValue, coder), "contentEquals(byte[]) differs " + round);
                check(!(boolean) contentEqualsChars.invoke(owner, differing.toCharArray(), 0, length, coder), "contentEquals(char[]) differs " + round);
                check(!(boolean) contentEqualsCompact.invoke(owner, otherValue, 0, length, coder, coder), "contentEqualsCompactBytes differs " + round);
            }
            if (coder == 0 && length > 0) {
                char[] widened = text.toCharArray();
                byte[] utf16 = utf16Value(widened);
                check((boolean) contentEqualsCompact.invoke(owner, utf16, 0, length, (byte) 1, coder), "contentEqualsCompactBytes UTF-16 source " + round);
            }
        }
    }

    private static Object shape(String text, Random random) throws Exception {
        if (text.isEmpty()) return atom("x");
        return switch (random.nextInt(4)) {
            case 0 -> atom(text);
            case 1 -> tupleOf(text, random, 1);
            case 2 -> tupleOf(text, random, 2 + random.nextInt(3));
            default -> slicedShape(text, random);
        };
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
        String padded = "pre" + text + "post";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        return slice.invoke(whole, 3, 3 + text.length());
    }

    private static String shaped(String flat, Object storage) throws Exception {
        if (flat.isEmpty()) return flat;
        String out = (String) UNSAFE.allocateInstance(String.class);
        UNSAFE.putReference(out, valueOffset, UNSAFE.getReference(flat, valueOffset));
        UNSAFE.putByte(out, coderOffset, UNSAFE.getByte(flat, coderOffset));
        UNSAFE.putReference(out, m3Offset, storage);
        check(out.length() == flat.length(), "shaped length");
        return out;
    }

    private static String spell(Random random, int length) {
        StringBuilder out = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            out.append(switch (random.nextInt(6)) {
                case 0, 1, 2 -> (char) ('a' + random.nextInt(26));
                case 3 -> (char) random.nextInt(0x100);
                case 4 -> (char) (0x100 + random.nextInt(0x700));
                default -> (char) (0xd800 + random.nextInt(0x800));
            });
        }
        return out.toString();
    }

    private static String latin(Random random, int length) {
        StringBuilder out = new StringBuilder(length);
        for (int index = 0; index < length; index++) out.append((char) random.nextInt(0x100));
        return out.toString();
    }

    private static String wide(Random random, int length) {
        StringBuilder out = new StringBuilder(length);
        for (int index = 0; index < length; index++) out.append((char) (0x100 + random.nextInt(0xf000)));
        return out.toString();
    }

    private static String differAt(String text, int position, Random random) {
        char[] changed = text.toCharArray();
        changed[position] = (char) (changed[position] ^ (1 + random.nextInt(0x1ff)));
        return new String(changed);
    }

    private static byte[] utf16Value(char[] units) {
        char[] wide = new char[units.length + 1];
        System.arraycopy(units, 0, wide, 0, units.length);
        wide[units.length] = 'Ā';
        String probe = new String(wide);
        byte[] value = (byte[]) UNSAFE.getReference(probe, valueOffset);
        check(UNSAFE.getByte(probe, coderOffset) == 1, "utf16 probe coder");
        byte[] out = new byte[units.length << 1];
        System.arraycopy(value, 0, out, 0, out.length);
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
