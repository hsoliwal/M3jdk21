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
 * @summary Case-insensitive comparisons with an M3 side read in place (A41): compareToIgnoreCase
 *          and CASE_INSENSITIVE_ORDER with an M3 side, a flat side or both M3 (both orders),
 *          equalsIgnoreCase and regionMatches(ignoreCase), agree with the flat Strings for
 *          identical, case-swapped, prefix-sharing and single-unit-different texts (the difference
 *          at random places, at the first and the last unit, at the 64/256/1024/4096/8192/16384
 *          window edges), every coder pair, Latin-1 ranges of wide owners, case rules with no
 *          round trip (Kelvin sign, long s, dotless i, Georgian), Deseret surrogate pairs whose
 *          low unit alone differs (equal ignoring case or not), pairs at window edges and
 *          straddling the end of the shorter side, lone surrogates, 0..20000 units.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3IgnoreCaseInPlaceTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3IgnoreCaseInPlaceTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3IgnoreCaseInPlaceTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 15, 16, 17, 63, 64, 65, 255, 256, 257, 1023, 1024, 1025, 4095, 4096, 4097, 5000, 8192, 8193, 16384, 20000};
    private static final int[] EDGES = {64, 256, 1024, 4096, 8192, 16384};
    private static final String SPECIALS = "kKKKſsSıiIİßµΜμაႠᲐаАéÉ";
    private static final String PAIRS = "𐐀𐐨𐐁𐐩𐐂𐐪𐐃𐐫";
    private static long checks;
    private static String context = "";
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x494e504c414345L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 5; kind++) {
                String text = spell(random, length, kind);
                List<String> others = others(text, random, kind);
                for (int o = 0; o < others.size(); o++) {
                    String other = others.get(o);
                    Object[] shapes = shapes(text, random);
                    for (int s = 0; s < shapes.length; s++) {
                        context = "length " + length + " kind " + kind + " other " + o + " shape " + s + " ";
                        String m3 = shaped(text, shapes[s]);
                        Object otherShape = random.nextBoolean() ? atom(other) : tupleOf(other, random, 1 + random.nextInt(2));
                        compare(text, m3, other, shaped(other, otherShape), random);
                    }
                }
            }
        }
        System.out.println("M3IgnoreCaseInPlaceTest checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        valueOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
        coderOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("coder"));
        m3Offset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("m3"));
    }

    private static void compare(String flat, String m3, String other, String otherM3, Random random) {
        String label = context + flat.length() + "/" + other.length() + " units";
        int order = flat.compareToIgnoreCase(other);
        int left = m3.compareToIgnoreCase(other);
        check(left == order, label + " compareToIgnoreCase m3 left " + left + " vs " + order);
        int right = flat.compareToIgnoreCase(otherM3);
        check(right == order, label + " compareToIgnoreCase m3 right " + right + " vs " + order);
        int both = m3.compareToIgnoreCase(otherM3);
        check(both == order, label + " compareToIgnoreCase m3 both " + both + " vs " + order);
        int reversed = other.compareToIgnoreCase(flat);
        check(String.CASE_INSENSITIVE_ORDER.compare(otherM3, m3) == reversed, label + " CASE_INSENSITIVE_ORDER reversed both");
        check(String.CASE_INSENSITIVE_ORDER.compare(other, m3) == reversed, label + " CASE_INSENSITIVE_ORDER reversed m3 right");
        check(String.CASE_INSENSITIVE_ORDER.compare(otherM3, flat) == reversed, label + " CASE_INSENSITIVE_ORDER reversed m3 left");
        check(Integer.signum(reversed) == -Integer.signum(order), label + " antisymmetry of the flat oracle");
        boolean equal = flat.equalsIgnoreCase(other);
        check((order == 0) == equal, label + " flat oracle consistency");
        check(m3.equalsIgnoreCase(other) == equal, label + " equalsIgnoreCase m3 left");
        check(flat.equalsIgnoreCase(otherM3) == equal, label + " equalsIgnoreCase m3 right");
        check(m3.equalsIgnoreCase(otherM3) == equal, label + " equalsIgnoreCase m3 both");
        int length = Math.min(flat.length(), other.length());
        for (int round = 0; round < 3; round++) {
            int len = length == 0 ? 0 : random.nextInt(length + 1);
            int toffset = flat.length() - len == 0 ? 0 : random.nextInt(flat.length() - len + 1);
            int ooffset = other.length() - len == 0 ? 0 : random.nextInt(other.length() - len + 1);
            region(flat, m3, other, otherM3, toffset, ooffset, len, label);
        }
        region(flat, m3, other, otherM3, 0, 0, length, label);
        region(flat, m3, other, otherM3, 1, 1, length - 2, label);
        region(flat, m3, other, otherM3, 0, 0, length + 1, label);
        for (int edge : EDGES) {
            if (edge < length) {
                region(flat, m3, other, otherM3, edge - 1, edge - 1, length - edge + 1, label);
                region(flat, m3, other, otherM3, 0, 1, length - 1, label);
                region(flat, m3, other, otherM3, 1, 0, length - 1, label);
            }
        }
    }

    private static void region(String flat, String m3, String other, String otherM3, int toffset, int ooffset, int len, String label) {
        boolean expected = flat.regionMatches(true, toffset, other, ooffset, len);
        String where = label + " regionMatches(true, " + toffset + ", " + ooffset + ", " + len + ")";
        check(m3.regionMatches(true, toffset, other, ooffset, len) == expected, where + " m3 left");
        check(flat.regionMatches(true, toffset, otherM3, ooffset, len) == expected, where + " m3 right");
        check(m3.regionMatches(true, toffset, otherM3, ooffset, len) == expected, where + " m3 both");
    }

    private static List<String> others(String text, Random random, int kind) {
        List<String> out = new ArrayList<>();
        out.add(text);
        out.add(swapCase(text));
        int length = text.length();
        if (length == 0) {
            out.add("a");
            out.add("K");
            out.add("𐐀");
            return out;
        }
        out.add(mutate(text, random.nextInt(length), random));
        out.add(mutate(text, length - 1, random));
        out.add(mutate(text, 0, random));
        out.add(text.substring(0, length - 1));
        out.add(text + 'x');
        out.add(text + "𐐀");
        out.add(text + "\ud801");
        out.add(text.substring(0, length - 1) + "𐐀");
        out.add(text.substring(0, length - 1) + "𐐨");
        out.add(text.substring(0, length - 1) + "\udc00");
        out.add(text.substring(0, length - 1) + "");
        int cut = random.nextInt(length + 1);
        out.add(text.substring(0, cut) + spell(random, length - cut, kind));
        out.add(swapCase(text.substring(0, cut)) + spell(random, length - cut, kind));
        out.add(text.substring(0, cut) + swapCase(text.substring(cut)) + spell(random, 1 + random.nextInt(3), kind));
        out.add(text.substring(0, length / 2) + 'K' + text.substring(Math.min(length, length / 2 + 1)));
        for (int edge : EDGES) {
            for (int at = edge - 2; at <= edge + 1 && at < length; at++) {
                if (at >= 0) out.add(mutate(text, at, random));
            }
            if (edge + 1 < length) {
                out.add(text.substring(0, edge - 1) + "𐐀" + text.substring(edge + 1));
                out.add(text.substring(0, edge - 1) + "𐐨" + text.substring(edge + 1));
                out.add(text.substring(0, edge - 1) + "\ud801" + text.substring(edge));
                out.add(text.substring(0, edge) + "𐐀" + text.substring(edge + 2));
                out.add(text.substring(0, edge) + "𐐨" + text.substring(edge + 2));
            }
        }
        if (kind == 4) {
            for (int round = 0; round < 4; round++) {
                int at = 1 + random.nextInt(length);
                if (at < length && Character.isHighSurrogate(text.charAt(at - 1)) && Character.isLowSurrogate(text.charAt(at))) {
                    out.add(text.substring(0, at) + (char) (text.charAt(at) ^ 0x28) + text.substring(at + 1));
                    out.add(text.substring(0, at) + (char) (text.charAt(at) ^ 0x01) + text.substring(at + 1));
                    out.add(text.substring(0, at - 1) + (char) (text.charAt(at - 1) ^ 0x01) + text.substring(at));
                    out.add(text.substring(0, at - 1) + text.substring(at));
                }
            }
        }
        return out;
    }

    private static String mutate(String text, int at, Random random) {
        char unit = text.charAt(at);
        char replacement = switch (random.nextInt(5)) {
            case 0 -> Character.isUpperCase(unit) ? Character.toLowerCase(unit) : Character.toUpperCase(unit);
            case 1 -> SPECIALS.charAt(random.nextInt(SPECIALS.length()));
            case 2 -> (char) ('a' + random.nextInt(26));
            case 3 -> (char) (random.nextInt(0x100));
            default -> '\udc00';
        };
        return text.substring(0, at) + replacement + text.substring(at + 1);
    }

    private static String swapCase(String text) {
        char[] units = text.toCharArray();
        for (int i = 0; i < units.length; i++) {
            char unit = units[i];
            units[i] = Character.isUpperCase(unit) ? Character.toLowerCase(unit) : Character.isLowerCase(unit) ? Character.toUpperCase(unit) : unit;
        }
        return new String(units);
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        if (text.isEmpty()) return new Object[] {slice.invoke(atom("padend"), 3, 3)};
        String padded = (random.nextBoolean() ? "一pad" : "pad") + text + "endé";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        int begin = padded.length() - 4 - text.length();
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, begin, begin + text.length())};
    }

    private static String spell(Random random, int length, int kind) {
        StringBuilder out = new StringBuilder(length + 1);
        while (out.length() < length) {
            switch (kind) {
                case 0 -> out.append((char) ((random.nextBoolean() ? 'A' : 'a') + random.nextInt(26)));
                case 1 -> out.append((char) random.nextInt(0x100));
                case 2 -> out.append((char) switch (random.nextInt(6)) {
                    case 0 -> 0x391 + random.nextInt(25);
                    case 1 -> 0x3b1 + random.nextInt(25);
                    case 2 -> 0x410 + random.nextInt(64);
                    case 3 -> SPECIALS.charAt(random.nextInt(SPECIALS.length()));
                    default -> (random.nextBoolean() ? 'A' : 'a') + random.nextInt(26);
                });
                case 3 -> out.append((char) (random.nextInt(8) == 0 ? (random.nextBoolean() ? 0xd801 : 0xdc00 + random.nextInt(0x50)) : 'a' + random.nextInt(26)));
                default -> {
                    if (random.nextInt(3) == 0) {
                        int pair = random.nextInt(PAIRS.length() / 2) * 2;
                        out.append(PAIRS, pair, pair + 2);
                    } else {
                        out.append((char) ((random.nextBoolean() ? 'A' : 'a') + random.nextInt(26)));
                    }
                }
            }
        }
        out.setLength(length);
        return out.toString();
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

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
