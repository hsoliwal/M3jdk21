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
 * @summary The literal-absence gate of Matcher consumes the subject's own precompute (A18):
 *          find, matches, lookingAt, hitEnd, requireEnd, regions and reused matchers over M3-backed
 *          Strings (atoms, nested tuples, slices; lengths below, inside and above the trigram
 *          cache band) agree with the flat results for quoted literals and derived patterns, on a
 *          cold and a warm per-owner cache.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3RegexLiteralGateTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3RegexLiteralGateTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.internal.misc.Unsafe;

public class M3RegexLiteralGateTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 3, 7, 40, 255, 256, 257, 900, 2048, 4097, 32767, 32768, 32769, 40000};
    private static final String[] DERIVED = {
        "ab+c", "(ab|cd)+x", "abc.*def", "a.c", "^abc", "abc$", "(?i)ABC", "[abc]{3}", "zzqx", "ba(na)+", "x?y+z", "a{2,}b"};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x4c49544552414cL);
        for (int length : LENGTHS) {
            String text = spell(random, length);
            List<Pattern> patterns = patterns(random, text);
            for (Object storage : shapes(text, random)) {
                String m3 = shaped(text, storage);
                for (Pattern pattern : patterns) {
                    compare(pattern, text, m3, random);
                    compare(pattern, text, m3, random); // warm per-owner cache
                }
                reuse(patterns.get(0), text, m3, spell(random, Math.max(1, length / 2)));
            }
        }
        System.out.println("M3RegexLiteralGateTest checks=" + checks);
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

    private static List<Pattern> patterns(Random random, String text) {
        List<Pattern> out = new ArrayList<>();
        for (String regex : DERIVED) out.add(Pattern.compile(regex));
        for (int needle = 1; needle <= 8; needle++) {
            if (text.length() >= needle) {
                int at = random.nextInt(text.length() - needle + 1);
                out.add(Pattern.compile(Pattern.quote(text.substring(at, at + needle))));
            }
            out.add(Pattern.compile(Pattern.quote(spell(random, needle))));
            out.add(Pattern.compile(Pattern.quote(spell(random, needle)) + "+"));
        }
        out.add(Pattern.compile(Pattern.quote("zzqx") + "|" + Pattern.quote("qqzy")));
        out.add(Pattern.compile("(?m)^" + Pattern.quote(spell(random, 3)) + "$"));
        return out;
    }

    private static void compare(Pattern pattern, String flat, String m3, Random random) {
        String label = pattern.pattern() + " over " + flat.length();
        Matcher a = pattern.matcher(flat);
        Matcher b = pattern.matcher(m3);
        check(findAll(a).equals(findAll(b)), "find sequence " + label);
        check(a.hitEnd() == b.hitEnd(), "hitEnd " + label + " " + a.hitEnd() + " vs " + b.hitEnd());
        check(a.requireEnd() == b.requireEnd(), "requireEnd " + label);
        check(pattern.matcher(flat).matches() == pattern.matcher(m3).matches(), "matches " + label);
        check(pattern.matcher(flat).lookingAt() == pattern.matcher(m3).lookingAt(), "lookingAt " + label);
        check(Pattern.matches(pattern.pattern(), flat) == Pattern.matches(pattern.pattern(), m3), "static matches " + label);
        if (flat.length() > 1) {
            int begin = random.nextInt(flat.length());
            int end = begin + random.nextInt(flat.length() - begin + 1);
            check(findAll(pattern.matcher(flat).region(begin, end)).equals(findAll(pattern.matcher(m3).region(begin, end))),
                    "region " + begin + ".." + end + " " + label);
            check(findAll(pattern.matcher(flat).region(begin, end).useTransparentBounds(true).useAnchoringBounds(false))
                    .equals(findAll(pattern.matcher(m3).region(begin, end).useTransparentBounds(true).useAnchoringBounds(false))),
                    "transparent region " + label);
            int from = random.nextInt(flat.length() + 1);
            check(pattern.matcher(flat).find(from) == pattern.matcher(m3).find(from), "find(from) " + label);
        }
    }

    private static void reuse(Pattern pattern, String flat, String m3, String other) throws Exception {
        Matcher a = pattern.matcher(flat);
        Matcher b = pattern.matcher(m3);
        findAll(a);
        findAll(b);
        check(findAll(a.reset()).equals(findAll(b.reset())), "reset reuse " + flat.length());
        check(findAll(a.reset(other)).equals(findAll(b.reset(other))), "reset(other) " + flat.length());
        String otherM3 = shaped(other, atom(other));
        check(findAll(a.reset(other)).equals(findAll(b.reset(otherM3))), "reset(other M3) " + flat.length());
        check(findAll(a.reset(flat)).equals(findAll(b.reset(m3))), "reset(back) " + flat.length());
    }

    private static List<Integer> findAll(Matcher matcher) {
        List<Integer> out = new ArrayList<>();
        while (matcher.find()) {
            out.add(matcher.start());
            out.add(matcher.end());
            if (out.size() > 20_000) break;
        }
        return out;
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        if (text.isEmpty()) return new Object[0];
        String padded = "pad" + text + "end";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, 3, 3 + text.length())};
    }

    private static String spell(Random random, int length) {
        StringBuilder out = new StringBuilder(length);
        while (out.length() < length) {
            int pick = random.nextInt(16);
            if (pick < 10) out.append((char) ('a' + random.nextInt(3)));
            else if (pick < 13) out.append((char) ('a' + random.nextInt(26)));
            else if (pick < 15) out.append((char) ('0' + random.nextInt(10)));
            else out.append(random.nextBoolean() ? '\n' : (char) (0x100 + random.nextInt(0x400)));
        }
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
