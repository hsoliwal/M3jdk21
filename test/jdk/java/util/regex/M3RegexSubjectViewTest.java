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
 * @summary The regex engine reads an M3-backed String through a block-filled view (A20): find
 *          sequences, hitEnd, requireEnd, matches, lookingAt, regions with transparent bounds,
 *          find(from), reused and reset matchers and usePattern agree with the flat results for
 *          classes, alternations, quantifiers, backreferences, lookaround, word boundaries,
 *          anchors, multiline, case-insensitive, canonical equivalence and dot-all patterns over
 *          atoms, nested tuples and slices of 0..300000 units (block edges at 1024-unit multiples).
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3RegexSubjectViewTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3RegexSubjectViewTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.internal.misc.Unsafe;

public class M3RegexSubjectViewTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 3, 9, 100, 1023, 1024, 1025, 2047, 2049, 5000, 16384, 70000, 300000};
    private static final String[] REGEXES = {
        "[a-c]+d", "(ab|cd|ef)+g", "a.*?b", "a.+b", "(?s).*", "([a-d])\\1", "(?<=ab)c", "a(?=b)", "(?<!a)b",
        "\\bab\\w*\\b", "^ab", "cd$", "(?m)^[a-d]+$", "(?i)AB+C", "x?y+z", "a{2,4}b", "[^a-d]", "\\d+", "(a|ab)(c|bcd)(d*)",
        "a*+b", "(?s)a.{1,3}b", ".{1020,1030}d", "\\p{L}+", "é", "(?<name>ab)c\\k<name>"};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x5355424a454354L);
        List<Pattern> patterns = new ArrayList<>();
        for (String regex : REGEXES) patterns.add(Pattern.compile(regex));
        patterns.add(Pattern.compile("é", Pattern.CANON_EQ));
        patterns.add(Pattern.compile("é", Pattern.CANON_EQ));
        for (int length : LENGTHS) {
            String text = spell(random, length);
            for (Object storage : shapes(text, random)) {
                String m3 = shaped(text, storage);
                for (Pattern pattern : patterns) compare(pattern, text, m3, random);
                reuse(patterns.get(0), patterns.get(1), text, m3, spell(random, Math.max(1, length / 3)));
            }
        }
        System.out.println("M3RegexSubjectViewTest checks=" + checks);
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

    private static void compare(Pattern pattern, String flat, String m3, Random random) {
        String label = pattern.pattern() + " over " + flat.length();
        Matcher a = pattern.matcher(flat);
        Matcher b = pattern.matcher(m3);
        check(findAll(a).equals(findAll(b)), "find sequence " + label);
        check(a.hitEnd() == b.hitEnd(), "hitEnd " + label);
        check(a.requireEnd() == b.requireEnd(), "requireEnd " + label);
        check(pattern.matcher(flat).matches() == pattern.matcher(m3).matches(), "matches " + label);
        check(pattern.matcher(flat).lookingAt() == pattern.matcher(m3).lookingAt(), "lookingAt " + label);
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
            if (flat.length() > 1030) {
                int edge = 1024 * (1 + random.nextInt(flat.length() / 1024));
                check(findAll(pattern.matcher(flat).region(edge - 3, Math.min(flat.length(), edge + 3)))
                        .equals(findAll(pattern.matcher(m3).region(edge - 3, Math.min(flat.length(), edge + 3)))), "block edge " + edge + " " + label);
            }
        }
    }

    private static void reuse(Pattern first, Pattern second, String flat, String m3, String other) throws Exception {
        Matcher a = first.matcher(flat);
        Matcher b = first.matcher(m3);
        findAll(a);
        findAll(b);
        check(findAll(a.reset()).equals(findAll(b.reset())), "reset reuse " + flat.length());
        check(findAll(a.usePattern(second)).equals(findAll(b.usePattern(second))), "usePattern " + flat.length());
        check(findAll(a.reset(other)).equals(findAll(b.reset(other))), "reset(other) " + flat.length());
        String otherM3 = shaped(other, atom(other));
        check(findAll(a.reset(other)).equals(findAll(b.reset(otherM3))), "reset(other M3) " + flat.length());
        check(findAll(a.reset(flat)).equals(findAll(b.reset(m3))), "reset(back) " + flat.length());
        StringBuilder builder = new StringBuilder(flat);
        check(findAll(a.reset(builder)).equals(findAll(b.reset(builder))), "reset(builder) " + flat.length());
    }

    private static List<Integer> findAll(Matcher matcher) {
        List<Integer> out = new ArrayList<>();
        while (matcher.find()) {
            out.add(matcher.start());
            out.add(matcher.end());
            if (out.size() > 40_000) break;
        }
        return out;
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        if (text.isEmpty()) return new Object[0];
        String padded = "padé" + text + "end";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, 4, 4 + text.length())};
    }

    private static String spell(Random random, int length) {
        StringBuilder out = new StringBuilder(length);
        while (out.length() < length) {
            int pick = random.nextInt(20);
            if (pick < 11) out.append((char) ('a' + random.nextInt(4)));
            else if (pick < 14) out.append((char) ('a' + random.nextInt(26)));
            else if (pick < 16) out.append((char) ('0' + random.nextInt(10)));
            else if (pick < 17) out.append(' ');
            else if (pick < 18) out.append('\n');
            else if (pick < 19) out.append(random.nextBoolean() ? "é" : "é");
            else out.append((char) (0x100 + random.nextInt(0x400)));
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
