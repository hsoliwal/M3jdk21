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
 * @summary The exact trigram facts built by the counting sort (A19) are bit-identical to the
 *          comparison-sorted reference: keys, length, prefix and suffix behaviour, test and
 *          containsAll, over alphabets of 1..65536 symbols (surrogates, U+FFFF, NUL included),
 *          lengths across the 256 and 8192 key thresholds, all-equal and strictly ascending
 *          texts, every precompute overload and the exact-literal query gate.
 * @modules java.base/jdk.internal.mindex
 * @run main M3TQFactsBuildTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3TQFactsBuildTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import jdk.internal.mindex.M3TQ;

public class M3TQFactsBuildTest {

    private static final int[] LENGTHS = {0, 1, 2, 3, 4, 100, 257, 258, 259, 300, 1000, 2048, 8193, 8194, 8195, 9000, 20000, 40000};
    private static final int[] ALPHABETS = {1, 2, 3, 8, 9, 26, 52, 200, 256, 257, 1000, 65536};
    private static long checks;

    public static void main(String[] args) {
        Random random = new Random(0x5241444958L);
        for (int length : LENGTHS) {
            for (int alphabet : ALPHABETS) {
                compare(spell(random, length, alphabet), random);
            }
            compare(ascending(length), random);
            compare(descending(length), random);
            compare(repeated(length, "￿\u0000𐀀"), random);
        }
        System.out.println("M3TQFactsBuildTest checks=" + checks);
    }

    private static void compare(String text, Random random) {
        int length = text.length();
        long[] reference = referenceKeys(text, 0, length);
        M3TQ.Facts whole = M3TQ.precompute(text, Integer.MAX_VALUE);
        check(Arrays.equals(whole.keys(), reference), "keys " + length);
        check(whole.keyCount() == reference.length, "keyCount " + length);
        check(whole.utf16Length() == length, "utf16Length " + length);
        check(Arrays.equals(M3TQ.precompute(text, 0, length, Integer.MAX_VALUE).keys(), reference), "keys from..to " + length);
        if (length > 3) {
            int from = random.nextInt(length / 2);
            int to = from + random.nextInt(length - from + 1);
            M3TQ.Facts range = M3TQ.precompute(text, from, to, Integer.MAX_VALUE);
            check(Arrays.equals(range.keys(), referenceKeys(text, from, to)), "range keys " + from + ".." + to + " of " + length);
            check(range.utf16Length() == to - from, "range length " + length);
        }
        for (int probe = 0; probe < 64; probe++) {
            long key = probe < reference.length ? reference[random.nextInt(reference.length)] : random.nextLong() & 0xffffffffffffL;
            check(whole.test(key) == (Arrays.binarySearch(reference, key) >= 0), "test " + key + " in " + length);
        }
        for (int needle = 1; needle <= 6 && length > 0; needle++) {
            int at = random.nextInt(length);
            String present = text.substring(at, Math.min(length, at + needle));
            M3TQ.Facts needleFacts = M3TQ.precompute(present, Integer.MAX_VALUE);
            check(whole.containsAll(needleFacts), "containsAll present " + needle + " in " + length);
            check(M3TQ.fromExact(List.of(present)).testPrecomputed(whole), "fromExact present " + needle + " in " + length);
            String absent = present + "☃";
            check(M3TQ.fromExact(List.of(absent)).testPrecomputed(whole) == mayContain(reference, absent), "fromExact absent " + needle + " in " + length);
        }
        List<String> terms = new ArrayList<>();
        for (int term = 0; term < 3 && length > 2; term++) {
            int at = random.nextInt(length - 2);
            terms.add(text.substring(at, at + 3));
        }
        if (!terms.isEmpty()) check(M3TQ.fromExact(terms).testPrecomputed(whole), "fromExact terms " + length);
    }

    private static boolean mayContain(long[] reference, String needle) {
        for (int i = 0; i + 2 < needle.length(); i++) {
            if (Arrays.binarySearch(reference, M3TQ.trigram(needle.charAt(i), needle.charAt(i + 1), needle.charAt(i + 2))) < 0) return false;
        }
        return true;
    }

    private static long[] referenceKeys(String text, int from, int to) {
        int count = Math.max(0, to - from - 2);
        long[] keys = new long[count];
        for (int i = 0; i < count; i++) {
            keys[i] = M3TQ.trigram(text.charAt(from + i), text.charAt(from + i + 1), text.charAt(from + i + 2));
        }
        Arrays.sort(keys);
        int unique = 0;
        for (int i = 0; i < count; i++) {
            if (unique == 0 || keys[i] != keys[unique - 1]) keys[unique++] = keys[i];
        }
        return Arrays.copyOf(keys, unique);
    }

    private static String spell(Random random, int length, int alphabet) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = (char) (alphabet == 65536 ? random.nextInt(0x10000) : alphabet > 256 ? 0x100 + random.nextInt(alphabet) : 'a' + random.nextInt(alphabet));
        }
        return new String(out);
    }

    private static String ascending(int length) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) out[i] = (char) i;
        return new String(out);
    }

    private static String descending(int length) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) out[i] = (char) (0xffff - i);
        return new String(out);
    }

    private static String repeated(int length, String unit) {
        StringBuilder out = new StringBuilder(length + unit.length());
        while (out.length() < length) out.append(unit);
        return out.substring(0, length);
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
