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
 * @summary The Latin-1 facts lane (A15) is bit-identical to the unit scan: every M3StringFacts
 *          field agrees between scanLatin1 and scanUnits over random Latin-1 atoms, nested tuples
 *          and slices (whitespace-heavy, all-whitespace, letters of both cases, 0x80..0xFF,
 *          lengths across the bulk and window boundaries); facts() routes Latin-1 owners through
 *          the lane and UTF-16 owners through the unit scan; the public consumers that read facts
 *          (isBlank, hashCode, codePointCount, getBytes(UTF-8) length, equalsIgnoreCase, contains,
 *          startsWith) agree with flat Strings; the trim and strip bounds are covered by the field dump.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3StringFactsLatin1LaneTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringFactsLatin1LaneTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3StringFactsLatin1LaneTest {

    private static final int[] LENGTHS = {
        1, 2, 3, 4, 5, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 127, 128, 129, 255, 256, 257,
        511, 512, 513, 1023, 1024, 1025, 1500, 2047, 2048, 2049, 4100
    };
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static Method facts;
    private static Method scanLatin1;
    private static Method scanUnits;
    private static Method coder;
    private static List<Field> fields;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        laneAgreesWithUnitScan();
        routing();
        consumers();
        System.out.println("M3StringFactsLatin1LaneTest checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> factsClass = Class.forName("java.lang.M3StringFacts");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        facts = accessible(m3.getDeclaredMethod("facts"));
        coder = accessible(m3.getDeclaredMethod("coder"));
        scanLatin1 = accessible(factsClass.getDeclaredMethod("scanLatin1", m3));
        scanUnits = accessible(factsClass.getDeclaredMethod("scanUnits", m3));
        fields = new ArrayList<>();
        for (Field field : factsClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            fields.add(field);
        }
        check(fields.size() >= 22, "facts fields " + fields.size());
        valueOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
        coderOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("coder"));
        m3Offset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("m3"));
    }

    /** Every field of every Latin-1 shape: lane == unit scan. */
    private static void laneAgreesWithUnitScan() throws Exception {
        Random random = new Random(0x4c4154494e31L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 7; kind++) {
                String text = spell(random, length, kind);
                for (Object value : shapes(text, random)) {
                    check((byte) coder.invoke(value) == 0, "Latin-1 shape coder");
                    Object lane = scanLatin1.invoke(null, value);
                    Object reference = scanUnits.invoke(null, value);
                    for (Field field : fields) {
                        Object a = field.get(lane);
                        Object b = field.get(reference);
                        check(a.equals(b), field.getName() + " differs: lane " + a + " unit " + b + " for " + show(text) + " length " + length + " kind " + kind);
                    }
                }
            }
        }
    }

    /** facts() takes the lane for Latin-1 owners and the unit scan for UTF-16 owners. */
    private static void routing() throws Exception {
        Random random = new Random(0x524f5554494e47L);
        for (int round = 0; round < 600; round++) {
            int length = 1 + random.nextInt(round % 5 == 0 ? 3000 : 90);
            boolean wide = round % 3 == 0;
            String text = wide ? widen(spell(random, length, random.nextInt(7)), random) : spell(random, length, random.nextInt(7));
            for (Object value : shapes(text, random)) {
                Object cached = facts.invoke(value);
                Object reference = scanUnits.invoke(null, value);
                for (Field field : fields) {
                    check(field.get(cached).equals(field.get(reference)), "facts() " + field.getName() + " for " + show(text));
                }
            }
        }
    }

    /** Public consumers that read facts agree with flat Strings. */
    private static void consumers() throws Exception {
        Random random = new Random(0x434f4e53554d45L);
        for (int round = 0; round < 1_500; round++) {
            int length = random.nextInt(round % 7 == 0 ? 1200 : 60);
            String text = spell(random, length, random.nextInt(7));
            if (text.isEmpty()) continue;
            Object storage = shapes(text, random).get(random.nextInt(4));
            String m3 = shaped(text, storage);
            check(m3.hashCode() == text.hashCode(), "hashCode " + show(text));
            check(m3.isBlank() == text.isBlank(), "isBlank " + show(text));
            check(m3.getBytes(StandardCharsets.UTF_8).length == text.getBytes(StandardCharsets.UTF_8).length, "utf8 length " + show(text));
            check(m3.codePointCount(0, length) == text.codePointCount(0, length), "codePointCount " + show(text));
            check(m3.equalsIgnoreCase(text.toUpperCase(java.util.Locale.ROOT)) == text.equalsIgnoreCase(text.toUpperCase(java.util.Locale.ROOT)), "equalsIgnoreCase upper " + show(text));
            check(m3.contains(text.substring(length / 2)) == text.contains(text.substring(length / 2)), "contains " + show(text));
            check(m3.startsWith(text.substring(0, length / 2)) == text.startsWith(text.substring(0, length / 2)), "startsWith " + show(text));
        }
    }

    private static List<Object> shapes(String text, Random random) throws Exception {
        List<Object> out = new ArrayList<>();
        out.add(atom(text));
        out.add(tupleOf(text, random, 1));
        out.add(tupleOf(text, random, 3));
        String padded = "  pad" + text + "end \t";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        out.add(slice.invoke(whole, 5, 5 + text.length()));
        return out;
    }

    private static String spell(Random random, int length, int kind) {
        StringBuilder out = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            out.append(switch (kind) {
                case 0 -> (char) ('a' + random.nextInt(26));
                case 1 -> (char) (random.nextBoolean() ? 'A' + random.nextInt(26) : 'a' + random.nextInt(26));
                case 2 -> (char) random.nextInt(0x100);
                case 3 -> random.nextInt(3) == 0 ? (char) (random.nextInt(2) == 0 ? ' ' : '\t') : (char) ('a' + random.nextInt(26));
                case 4 -> (char) (random.nextInt(5) == 0 ? 0x80 + random.nextInt(0x80) : 'a' + random.nextInt(26));
                case 5 -> (char) (new char[] {' ', '\t', '\n', '\r', '\u001c', '\u001f', ' ', '\u0085', '\u000b', '\f'}[random.nextInt(10)]);
                default -> (char) random.nextInt(0x21);
            });
        }
        return out.toString();
    }

    private static String widen(String text, Random random) {
        char[] units = text.toCharArray();
        if (units.length == 0) return "Ā";
        units[random.nextInt(units.length)] = (char) (0x100 + random.nextInt(0x400));
        return new String(units);
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

    private static String shaped(String flat, Object storage) throws Exception {
        String out = (String) UNSAFE.allocateInstance(String.class);
        UNSAFE.putReference(out, valueOffset, UNSAFE.getReference(flat, valueOffset));
        UNSAFE.putByte(out, coderOffset, UNSAFE.getByte(flat, coderOffset));
        UNSAFE.putReference(out, m3Offset, storage);
        return out;
    }

    private static String show(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int index = 0; index < Math.min(value.length(), 40); index++) {
            char unit = value.charAt(index);
            out.append(unit >= 0x20 && unit <= 0x7e ? String.valueOf(unit) : String.format("\\u%04x", (int) unit));
        }
        return out.append(value.length() > 40 ? "...\"" : "\"").toString();
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
