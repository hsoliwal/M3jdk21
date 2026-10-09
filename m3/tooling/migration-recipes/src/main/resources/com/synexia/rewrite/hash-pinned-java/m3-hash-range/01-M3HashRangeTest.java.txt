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
 * @summary The hash of an M3 range folded from bulk-read units (A23) equals the flat String's
 *          hashCode, the range facts' javaHash and the whole owner's hash: atoms, nested tuples
 *          and slices of padded owners (Latin-1 spellings inside UTF-16 owners, Latin-1 bytes
 *          above 0x7f, surrogates) of 0..20000 units across the 4096-unit window; hashes before
 *          and after the facts are prepared; HashMap lookups with M3 keys against flat keys.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3HashRangeTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3HashRangeTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3HashRangeTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 3, 16, 63, 64, 65, 300, 1000, 4095, 4096, 4097, 5000, 9000, 20000};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static Method facts;
    private static Field javaHash;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x48415348L);
        Map<String, Integer> map = new HashMap<>();
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 4; kind++) {
                String text = spell(random, length, kind);
                map.put(text, map.size());
                for (Object storage : shapes(text, random)) {
                    String fresh = shaped(text, storage);
                    check(fresh.hashCode() == text.hashCode(), "hash before facts " + length + " kind " + kind);
                    Object prepared = facts.invoke(storage);
                    check(javaHash.getInt(prepared) == text.hashCode(), "facts javaHash " + length + " kind " + kind);
                    String again = shaped(text, storage);
                    check(again.hashCode() == text.hashCode(), "hash after facts " + length + " kind " + kind);
                    check(map.get(again) != null && map.get(again) == map.get(text), "HashMap lookup " + length + " kind " + kind);
                    check(fresh.equals(text) && text.equals(fresh), "equals " + length + " kind " + kind);
                }
            }
        }
        System.out.println("M3HashRangeTest checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> factsClass = Class.forName("java.lang.M3StringFacts");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        facts = accessible(m3.getDeclaredMethod("facts"));
        javaHash = factsClass.getDeclaredField("javaHash");
        javaHash.setAccessible(true);
        valueOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
        coderOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("coder"));
        m3Offset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("m3"));
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        if (text.isEmpty()) return new Object[] {slice.invoke(atom("padend"), 3, 3)};
        String padded = (random.nextBoolean() ? "一pad" : "pad") + text + "endé";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        int begin = padded.length() - 4 - text.length();
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, begin, begin + text.length())};
    }

    private static String spell(Random random, int length, int kind) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = (char) switch (kind) {
                case 0 -> 'a' + random.nextInt(26);
                case 1 -> 0x80 + random.nextInt(0x80);
                case 2 -> random.nextInt(4) == 0 ? 0x100 + random.nextInt(0xd700) : 'a' + random.nextInt(26);
                default -> random.nextInt(3) == 0 ? 0xd800 + random.nextInt(0x800) : random.nextInt(0x10000);
            };
        }
        return new String(out);
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
