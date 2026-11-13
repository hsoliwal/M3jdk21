// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/** Deterministic real JNI vs independent Pattern oracle; no fake native backend. */
public final class M3RegexRequiredSegmentsSelfTest {
    private static long checks;
    private static final long SEED = 0x5eed12345789abcdL;
    private static final String[] ATOMS = {
            "", "a", "ab", "abc", "aab", "b", "de", "漢", "🙂", "α", "β", "\u0000",
            "public class A {}", "(?i)ab.*c", "record R(int x) {}",
            "// JNIEXPORT", "\"\"\"", "\\Q.*\\E", "[A-Z]+", "\r\n", "\n",
            "\\uD800", "e\u0301", "aababa"
    };
    private static final String[] PROVEN = {
            "a.*b", "ab.*c", "^ab.*c$", "^.*abc.*$", "a.+b", "α.*β",
            "🙂.*漢", "public.*class", "JNIEXPORT.*int", "a.*a.*b", ".*ab.*",
            "^.*漢.+a$", "a.*\\uD800", "ab.*cd.*ef.*zz", "ab.*ab",
            "ab.*", "^.*b.*", "a.*(?i)b"  // final two unsafe forms are refused below
    };
    private static final String[] NOT_PROVEN = {
            "a|b", "ab.*c|d", "a?b", "^a+b$", "a\\db", "a[bc]d",
            "(?i)a.*b", "a.*(?i)b", "a.*?b", "[a-z].*b",
            "\\Qa\\E.*b", "(a).*b", "abc", "^abc$", "^.*$", "",
            "a.*\\x{61}", "(?<=a)b.*", "a.*(?!b)c", "a.*\\1"
    };
    private M3RegexRequiredSegmentsSelfTest() { }

    private static void equal(int[] actual, int[] expected, String reason) {
        checks++;
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError(reason + " actual=" + Arrays.toString(actual)
                    + " expected=" + Arrays.toString(expected));
        }
    }
    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }
    private static void rejected(Runnable action, Class<? extends Throwable> kind) {
        checks++;
        try { action.run(); }
        catch (Throwable caught) {
            if (kind.isInstance(caught)) return;
            throw new AssertionError("wrong exception " + caught, caught);
        }
        throw new AssertionError("missing " + kind.getSimpleName());
    }

    private static int[] reference(Pattern pattern, List<String> rows) {
        int[] output = new int[rows.size()];
        int used = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (pattern.matcher(rows.get(i)).find()) output[used++] = i;
        }
        return Arrays.copyOf(output, used);
    }

    private static List<String> corpus(Random random, int size) {
        List<String> lines = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int tokens = random.nextInt(9);
            StringBuilder text = new StringBuilder();
            for (int j = 0; j < tokens; j++) {
                text.append(ATOMS[random.nextInt(ATOMS.length)]);
            }
            lines.add(text.toString());
        }
        return List.copyOf(lines);
    }

    public static long run() {
        check(M3RegexRequiredSegments.compile("^ab.*c$", 0).necessarySegments()
                .equals(List.of("ab", "c")), "anchored segment proof");
        check(M3RegexRequiredSegments.compile("ab.*ab", 0).necessarySegments()
                .equals(List.of("ab", "ab")), "duplicate mandatory fragments are retained");
        check(M3RegexRequiredSegments.compile(".*abc.*", 0).necessarySegments()
                .equals(List.of("abc")), "leading and trailing wildcard");
        for (String unsupported : NOT_PROVEN) {
            M3RegexRequiredSegments plan = M3RegexRequiredSegments.compile(unsupported, 0);
            check(!plan.facts().proven(), "must refuse grammar: " + unsupported);
            rejected(() -> plan.select(List.of("abc", "abde", ""),
                    M3RegexRequiredSegments.Backend.NATIVE_REQUIRED),
                    UnsupportedOperationException.class);
        }
        check(!M3RegexRequiredSegments.compile("ab.*c", Pattern.CASE_INSENSITIVE)
                .facts().proven(), "flags must be refused");
        check(!M3RegexRequiredSegments.compile("ab.*c", Pattern.CANON_EQ)
                .facts().proven(), "canonical equivalence cannot prefilter with bytes");
        Random random = new Random(SEED);
        List<String> all = corpus(random, 513);
        for (String regex : PROVEN) {
            M3RegexRequiredSegments plan = M3RegexRequiredSegments.compile(regex, 0);
            if (!plan.facts().proven()) continue;
            int[] expected = reference(plan.pattern(), all);
            equal(plan.select(all, M3RegexRequiredSegments.Backend.JAVA), expected, "java " + regex);
            equal(plan.select(all, M3RegexRequiredSegments.Backend.AUTO), expected, "auto " + regex);
            equal(plan.select(all, M3RegexRequiredSegments.Backend.NATIVE_REQUIRED), expected, "jni " + regex);
        }
        String[] literals = {"a", "b", "abc", "🙂", "漢", "α", "β", "ab", "de", "public", "class"};
        for (int iteration = 0; iteration < 320; iteration++) {
            String left = literals[random.nextInt(literals.length)];
            String middle = literals[random.nextInt(literals.length)];
            String right = literals[random.nextInt(literals.length)];
            String regex = switch (iteration & 3) {
                case 0 -> left + ".*" + right;
                case 1 -> "^.*" + left + ".+" + right + "$";
                case 2 -> left + ".*" + middle + ".*" + right;
                default -> ".*" + left + ".*";
            };
            M3RegexRequiredSegments plan = M3RegexRequiredSegments.compile(regex, 0);
            check(plan.facts().proven(), "constructed proof " + regex);
            List<String> lines = corpus(random, 48);
            int[] expected = reference(plan.pattern(), lines);
            equal(plan.select(lines, M3RegexRequiredSegments.Backend.JAVA), expected, "seed-java " + iteration);
            equal(plan.select(lines, M3RegexRequiredSegments.Backend.AUTO), expected, "seed-auto " + iteration);
            equal(plan.select(lines, M3RegexRequiredSegments.Backend.NATIVE_REQUIRED), expected, "seed-jni " + iteration);
        }
        M3RegexRequiredSegments valid = M3RegexRequiredSegments.compile("a.*b", 0);
        List<String> malformed = List.of("a\uD800b", "ab", "\uDC00");
        equal(valid.select(malformed, M3RegexRequiredSegments.Backend.AUTO),
                reference(valid.pattern(), malformed), "malformed UTF-16 uses Java");
        rejected(() -> valid.select(malformed, M3RegexRequiredSegments.Backend.NATIVE_REQUIRED),
                IllegalArgumentException.class);
        rejected(() -> valid.select(null, M3RegexRequiredSegments.Backend.AUTO),
                NullPointerException.class);
        rejected(() -> valid.select(Arrays.asList("a", null), M3RegexRequiredSegments.Backend.JAVA),
                NullPointerException.class);
        rejected(() -> valid.select(List.of("ab"), null), NullPointerException.class);
        M3RegexRequiredSegments text = M3RegexRequiredSegments.compile("a.*b", 0);
        equal(text.select(List.of("", "a\nb", "a\u0000b", "a\rb", "ab"),
                M3RegexRequiredSegments.Backend.NATIVE_REQUIRED),
                reference(text.pattern(), List.of("", "a\nb", "a\u0000b", "a\rb", "ab")),
                "newlines and embedded NULs");
        System.out.println("M3_REQUIRED_SEGMENTS_PASS checks=" + checks + " seed=" + Long.toHexString(SEED));
        return checks;
    }

    public static void main(String[] args) { run(); }
}
