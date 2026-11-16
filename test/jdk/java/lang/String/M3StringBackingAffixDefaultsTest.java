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
 * @summary M3 backing prefix/suffix defaults preserve legacy binary and UTF-16 contracts
 * @modules java.base/jdk.internal.mindex
 * @run main M3StringBackingAffixDefaultsTest
 */

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import jdk.internal.mindex.M3StringBacking;

public class M3StringBackingAffixDefaultsTest {
    private static long checks;

    public static void main(String[] ignored) {
        String value = "\u0000ab\uD83D\uDE00z\uFFFF";
        SpyBacking spy = new SpyBacking(value);
        check(spy.startsWith(1, ""), "empty prefix");
        check(spy.endsWith(1, ""), "empty suffix");
        check(spy.startsWith(1, "\u0000ab\uD83D"), "prefix splits surrogate");
        check(spy.endsWith(1, "\uDE00z\uFFFF"), "suffix splits surrogate");
        check(spy.startsWith(1, new StringBuilder("\u0000ab")), "StringBuilder prefix");
        check(spy.endsWith(1, new StringBuilder("z\uFFFF")), "StringBuilder suffix");
        check(spy.startsWith(1, value), "full prefix");
        check(spy.endsWith(1, value), "full suffix");
        check(!spy.startsWith(1, "\u0000aX"), "different prefix");
        check(!spy.endsWith(1, "Y\uFFFF"), "different suffix");
        int before = spy.rangeCalls;
        check(!spy.startsWith(1, value + "x"), "long prefix");
        check(!spy.endsWith(1, "x" + value), "long suffix");
        check(!spy.startsWith(1, null), "null prefix");
        check(!spy.endsWith(1, null), "null suffix");
        check(spy.rangeCalls == before, "rejected affixes skip range scan");
        check(spy.contentEquals(1, value), "legacy whole equality");
        check(spy.compare(1, 1) == 0, "legacy comparison");
        check(spy.rangeCalls >= 10, "range override remains polymorphic");
        check(spy.lastStart == 0 || spy.lastEnd == value.length(), "range coordinates");
        check(spy.materializations == 0, "must not materialize");
        spy.close();

        Random random = new Random(0xA7715L);
        String alphabet = "aB0\u0000\uD83D\uDE00\uFFFF\u0001";
        for (int trial = 0; trial < 2_048; trial++) {
            StringBuilder builder = new StringBuilder();
            for (int i = random.nextInt(25); i > 0; i--) {
                builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String text = builder.toString();
            Backing b = new Backing(text);
            int length = random.nextInt(text.length() + 1);
            String prefix = trial % 4 == 0 ? text.substring(0, length)
                    : randomString(random, alphabet, length);
            String suffix = trial % 4 == 1 ? text.substring(text.length() - length)
                    : randomString(random, alphabet, length);
            check(b.startsWith(7, new StringBuilder(prefix)) == text.startsWith(prefix),
                    "differential prefix " + trial);
            check(b.endsWith(7, new StringBuilder(suffix)) == text.endsWith(suffix),
                    "differential suffix " + trial);
            check(b.materializations == 0, "no materialize " + trial);
        }
        System.out.println("M3_STRING_BACKING_AFFIX_DEFAULTS_PASS checks=" + checks);
    }

    private static String randomString(Random random, String alphabet, int size) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < size; i++) {
            value.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return value.toString();
    }

    private static void check(boolean actual, String why) {
        if (!actual) throw new AssertionError(why + " check=" + checks);
        checks++;
    }

    private static class Backing implements M3StringBacking {
        final String value;
        int materializations;

        Backing(String value) { this.value = value; }
        @Override public void close() { }
        @Override public int length(long id) { return value.length(); }
        @Override public int utf8Length(long id) {
            return value.getBytes(StandardCharsets.UTF_8).length;
        }
        @Override public int codePointCount(long id) {
            return value.codePointCount(0, value.length());
        }
        @Override public int unpairedSurrogateCount(long id) {
            int unpaired = 0;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (Character.isHighSurrogate(c) && i + 1 < value.length()
                        && Character.isLowSurrogate(value.charAt(i + 1))) {
                    i++;
                } else if (Character.isSurrogate(c)) {
                    unpaired++;
                }
            }
            return unpaired;
        }
        @Override public int hashCode(long id) { return value.hashCode(); }
        @Override public char charAt(long id, int index) { return value.charAt(index); }
        @Override public CharBuffer utf16View(long id) {
            return CharBuffer.wrap(value).asReadOnlyBuffer();
        }
        @Override public ByteBuffer utf8View(long id) {
            return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
        }
        @Override public String materialize(long id) {
            materializations++;
            return value;
        }
    }

    private static final class SpyBacking extends Backing {
        int rangeCalls;
        int lastStart;
        int lastEnd;

        SpyBacking(String value) { super(value); }
        @Override public boolean contentEquals(long id, int start, int end, CharSequence other) {
            rangeCalls++;
            lastStart = start;
            lastEnd = end;
            return super.contentEquals(id, start, end, other);
        }
    }
}
