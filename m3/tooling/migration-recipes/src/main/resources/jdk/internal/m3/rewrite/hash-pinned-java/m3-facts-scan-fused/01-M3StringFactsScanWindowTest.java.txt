// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary The fused single-pass M3StringFacts scan over bulk-read windows equals two independent
 *          oracles on every field: (1) the fold of single-unit atom facts through compose, which
 *          never scans more than one unit, and (2) public String/Character definitions for the
 *          geometry, hash, case-hash, prefix/suffix, trim and strip fields; exercised on atoms and
 *          atom ranges that straddle the window boundaries
 * @modules java.base/java.lang:+open
 * @run main M3StringFactsScanWindowTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringFactsScanWindowTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class M3StringFactsScanWindowTest {
    private static final String[] ATOMS = {
        "", "a", "bc", "XYZ", " ", "\t", "\n", " ", " ", "é", "ÿ", "Ā",
        "\ud83d", "\ude42", "🙂", "\ud800", "\udc00", "\u0000", "alpha", "  pad  ",
        "\u001f", "\u007f", "Title Case", "🙂\ud83d", "\ude42 \ud83d", "x\u0085y"
    };
    private static long checks;
    private static Method internChars;
    private static Method slice;
    private static Method facts;
    private static Method compose;
    private static List<Field> fields;
    private static Object emptyFacts;

    public static void main(String[] args) throws Exception {
        bind();
        deterministic();
        randomized();
        System.out.println("M3StringFactsScanWindowTest checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> factsClass = Class.forName("java.lang.M3StringFacts");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        facts = accessible(m3.getDeclaredMethod("facts"));
        compose = accessible(factsClass.getDeclaredMethod("compose", factsClass, factsClass));
        fields = new ArrayList<>();
        for (Field field : factsClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                check(field.getType().isPrimitive(), "facts stay primitive: " + field);
                field.setAccessible(true);
                fields.add(field);
            }
        }
        Field window = factsClass.getDeclaredField("SCAN_WINDOW");
        window.setAccessible(true);
        check(window.getInt(null) == 1024, "declared window");
        emptyFacts = facts.invoke(atom(""));
    }

    private static void deterministic() throws Exception {
        for (String spelling : ATOMS) verify(spelling, "atom");
        // Window boundaries: lengths around 1024 and 2048 with a surrogate pair or a lone high
        // surrogate placed exactly on the boundary, plus whitespace-only and blank-tail shapes.
        for (int length : new int[] {1023, 1024, 1025, 2047, 2048, 2049, 3071}) {
            verify(repeat("ab é", length), "boundary-mixed-" + length);
            verify(repeat(" ", length), "boundary-blank-" + length);
            verify(repeat("x", length - 1) + " ", "boundary-tail-" + length);
            if (length < 1026) continue;
            String pairOnSeam = repeat("q", 1023) + "🙂" + repeat("r", length - 1025);
            verify(pairOnSeam, "boundary-pair-" + length);
            String loneOnSeam = repeat("q", 1023) + "\ud83d" + repeat("r", length - 1024);
            verify(loneOnSeam, "boundary-lone-high-" + length);
            String loneLowOnSeam = repeat("q", 1024) + "\ude42" + repeat("r", length - 1025);
            verify(loneLowOnSeam, "boundary-lone-low-" + length);
            String pairSplitBySeam = repeat("q", 1024) + "\ud83d" + "\ude42" + repeat("r", length - 1026);
            verify(pairSplitBySeam, "boundary-pair-split-" + length);
            String highAtSeamThenHigh = repeat("q", 1023) + "\ud83d\ud83d" + repeat("r", length - 1025);
            verify(highAtSeamThenHigh, "boundary-high-high-" + length);
        }
        verify(repeat("🙂", 1500), "pairs-only-3000");
        verify(repeat("\ud83d", 2100), "lone-high-only-2100");
        verify(repeat(" ", 2100) + "z", "whitespace-head-2101");
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d33464143545353L);
        for (int trial = 0; trial < 4_000; trial++) {
            String spelling = spell(random, 1 + random.nextInt(6));
            if (trial % 40 == 0) {
                int target = 1000 + random.nextInt(2200);
                StringBuilder big = new StringBuilder(target + 8);
                while (big.length() < target) big.append(ATOMS[random.nextInt(ATOMS.length)]);
                spelling = big.toString();
            }
            Object atom = verify(spelling, "random " + trial);
            if (!spelling.isEmpty()) {
                int begin = random.nextInt(spelling.length());
                int end = begin + random.nextInt(spelling.length() - begin + 1);
                Object range = slice.invoke(atom, begin, end);
                sameFacts(facts.invoke(range), spelling.substring(begin, end), "random range " + trial);
            }
        }
    }

    private static Object verify(String spelling, String label) throws Exception {
        Object atom = atom(spelling);
        sameFacts(facts.invoke(atom), spelling, label);
        return atom;
    }

    /** Scanned facts equal the compose fold of single-unit facts and the public definitions. */
    private static void sameFacts(Object scanned, String spelling, String label) throws Exception {
        Object folded = emptyFacts;
        for (int index = 0; index < spelling.length(); index++) {
            folded = compose.invoke(null, folded, facts.invoke(atom(spelling.substring(index, index + 1))));
        }
        for (Field field : fields) {
            Object left = field.get(scanned);
            Object right = field.get(folded);
            check(left.equals(right), label + " field=" + field.getName() + " scanned=" + left + " folded=" + right);
        }
        int length = spelling.length();
        check(get(scanned, "utf16Length").equals(length), label + " utf16Length");
        check(get(scanned, "javaHash").equals(spelling.hashCode()), label + " javaHash");
        check(get(scanned, "codePointCount").equals(spelling.codePointCount(0, length)), label + " codePointCount");
        check(get(scanned, "utf8Length").equals(spelling.getBytes(StandardCharsets.UTF_8).length), label + " utf8Length");
        check(get(scanned, "ascii").equals(spelling.chars().allMatch(c -> c <= 0x7f)), label + " ascii");
        check(get(scanned, "latin1").equals(spelling.chars().allMatch(c -> c <= 0xff)), label + " latin1");
        check(get(scanned, "firstUtf16Unit").equals(length == 0 ? (char) 0 : spelling.charAt(0)), label + " first");
        check(get(scanned, "lastUtf16Unit").equals(length == 0 ? (char) 0 : spelling.charAt(length - 1)), label + " last");
        check(get(scanned, "hash31Power").equals(pow31(length)), label + " hash31Power");
        check(get(scanned, "asciiUpperHash").equals(asciiHash(spelling, true, false)), label + " asciiUpperHash");
        check(get(scanned, "asciiLowerHash").equals(asciiHash(spelling, false, false)), label + " asciiLowerHash");
        check(get(scanned, "asciiTitleHash").equals(asciiHash(spelling, false, true)), label + " asciiTitleHash");
        check(get(scanned, "prefix4").equals(prefix4(spelling)), label + " prefix4");
        check(get(scanned, "suffix4").equals(suffix4(spelling)), label + " suffix4");
        check(get(scanned, "unpairedSurrogateCount").equals(unpaired(spelling)), label + " unpaired");
        int stripStart = length - spelling.stripLeading().length();
        int stripEnd = spelling.isBlank() ? 0 : spelling.stripTrailing().length();
        check(get(scanned, "stripStart").equals(stripStart), label + " stripStart");
        check(get(scanned, "stripEnd").equals(stripEnd), label + " stripEnd");
        int trimStart = 0;
        while (trimStart < length && spelling.charAt(trimStart) <= ' ') trimStart++;
        int trimEnd = trimStart == length ? 0 : length;
        while (trimEnd > trimStart && spelling.charAt(trimEnd - 1) <= ' ') trimEnd--;
        check(get(scanned, "trimStart").equals(trimStart), label + " trimStart");
        check(get(scanned, "trimEnd").equals(trimEnd), label + " trimEnd");
    }

    private static Object get(Object prepared, String name) throws Exception {
        for (Field field : fields) {
            if (field.getName().equals(name)) return field.get(prepared);
        }
        throw new AssertionError("no field " + name);
    }

    private static int pow31(int length) {
        int result = 1;
        for (int index = 0; index < length; index++) result *= 31;
        return result;
    }

    private static int asciiHash(String value, boolean upper, boolean title) {
        int hash = 0;
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            boolean up = title ? index == 0 : upper;
            char mapped = up
                    ? (unit >= 'a' && unit <= 'z' ? (char) (unit - ('a' - 'A')) : unit)
                    : (unit >= 'A' && unit <= 'Z' ? (char) (unit + ('a' - 'A')) : unit);
            hash = 31 * hash + mapped;
        }
        return hash;
    }

    private static long prefix4(String value) {
        long packed = 0L;
        for (int index = 0; index < Math.min(4, value.length()); index++) {
            packed |= (long) value.charAt(index) << (48 - (index << 4));
        }
        return packed;
    }

    private static long suffix4(String value) {
        long packed = 0L;
        for (int index = Math.max(0, value.length() - 4); index < value.length(); index++) {
            packed = (packed << 16) | value.charAt(index);
        }
        return packed;
    }

    private static int unpaired(String value) {
        int count = 0;
        int index = 0;
        while (index < value.length()) {
            char unit = value.charAt(index);
            if (Character.isHighSurrogate(unit) && index + 1 < value.length()
                    && Character.isLowSurrogate(value.charAt(index + 1))) {
                index += 2;
                continue;
            }
            if (Character.isSurrogate(unit)) count++;
            index++;
        }
        return count;
    }

    private static Object atom(String spelling) throws Exception {
        char[] chars = spelling.toCharArray();
        return internChars.invoke(null, chars, 0, chars.length);
    }

    private static String repeat(String unit, int length) {
        StringBuilder out = new StringBuilder(length + unit.length());
        while (out.length() < length) out.append(unit);
        out.setLength(length);
        return out.toString();
    }

    private static String spell(Random random, int parts) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < parts; index++) out.append(ATOMS[random.nextInt(ATOMS.length)]);
        return out.toString();
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
