// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Always-enabled seeded JDK/real-JNI differential oracle for conservative regex precompute. */
public final class M3RegexLiteralPlanSelfTest {
    private static long checks;
    private static final String[] TOKENS = {
        "", "a", "aa", "aaa", "abc", "b", "B", "-", ".*", "^foo$", "\\Qx\\E",
        "class", "public", "record", "JNIEXPORT", "String", "<T>", "{x}", "// c",
        "/* a */", "\"{code}\"", "\\d+", "(?s)", "[]", "ä", "\u0301", "☃", "中",
        "😀", "αβ", "𐐷", "\u0000", "\n", "\t", "\\", "(?=foo)", "::", "|"
    };

    private M3RegexLiteralPlanSelfTest() { }

    private static void check(boolean fact, String where) {
        checks++;
        if (!fact) throw new AssertionError("check=" + checks + " " + where);
    }

    private static void equal(int[] observed, int[] reference, String where) {
        checks++;
        if (!Arrays.equals(observed, reference)) {
            throw new AssertionError("check=" + checks + " " + where
                    + " actual=" + Arrays.toString(observed)
                    + " expected=" + Arrays.toString(reference));
        }
    }

    private static int[] jdk(Pattern pattern, List<String> lines) {
        int[] indices = new int[lines.size()];
        int used = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (pattern.matcher(lines.get(i)).find()) indices[used++] = i;
        }
        return Arrays.copyOf(indices, used);
    }

    private static List<String> corpus(long seed, int size) {
        Random random = new Random(seed);
        List<String> lines = new ArrayList<>(size + 20);
        lines.addAll(List.of(
            "public class Host { String text = \"a.*\\\\Q\"; }",
            "// class Dummy { void fake() { if(true) return; } }",
            "/* JNIEXPORT jint JNICALL Java_fake(JNIEnv *env) */",
            "record R(String s) { R { if(s == null) throw new Error(); } }",
            "String regex = \"(?s).*\\\\Qclass\\\\E.*\";",
            "class Fake { /* String code = \"x\"; */ }",
            "\"\"\" text block { class Fake {} } \"\"\"",
            "@interface A { String value() default \"(?=\\\\w)\"; }",
            "a\u0000b", "a\nb", "", "äe\u0301中😀𐐷"
        ));
        for (int i = 0; i < size; i++) {
            StringBuilder line = new StringBuilder();
            int words = random.nextInt(10);
            for (int j = 0; j < words; j++) line.append(TOKENS[random.nextInt(TOKENS.length)]);
            lines.add(line.toString());
        }
        return List.copyOf(lines);
    }

    private static void oracle(String pattern, int flags, List<String> corpus) {
        M3RegexLiteralPlan compiled = M3RegexLiteralPlan.compile(pattern, flags);
        Pattern jdkPattern = Pattern.compile(pattern, flags);
        int[] reference = jdk(jdkPattern, corpus);
        equal(compiled.select(corpus, M3RegexLiteralPlan.Backend.JAVA), reference,
                "JAVA " + pattern + " " + flags);
        equal(compiled.select(corpus, M3RegexLiteralPlan.Backend.AUTO), reference,
                "AUTO " + pattern + " " + flags);
        check(compiled.signals().equals(M3RegexLiteralPlan.compile(pattern, flags).signals()),
                "deterministic static signals");
        if (compiled.signals().exactLiteral()) {
            equal(compiled.select(corpus, M3RegexLiteralPlan.Backend.NATIVE_REQUIRED), reference,
                    "JNI " + pattern + " " + flags);
            check(compiled.signals().utf8Bytes() >= 0, "prepared native literal size");
        } else {
            checks++;
            try {
                compiled.select(corpus, M3RegexLiteralPlan.Backend.NATIVE_REQUIRED);
                throw new AssertionError("unsupported regex admitted to native: " + pattern);
            } catch (UnsupportedOperationException expected) { /* fail-closed */ }
        }
    }

    private static void reject(Runnable action, Class<? extends Throwable> type) {
        checks++;
        try {
            action.run();
            throw new AssertionError("expected refusal " + type.getName());
        } catch (RuntimeException expected) {
            if (!type.isInstance(expected)) throw expected;
        }
    }

    public static void main(String[] args) {
        check(NativeLineBatch.nativeAbi() == 0x00010002, "exact JNI baseline ABI");
        List<String> examples = List.of(
            "", "a", "a+b", "public class", "String s = \"x\";", "// fake source",
            "(?s)", "äe\u0301", "中", "😀", "\u0000", "x\\y", "a\nb", "\t", "^foo$",
            "\\Qclass\\E", "\\Qx\\E", "[]", "<T>", "(?=x)", "\\d+",
            "a{2}", "αβ", "𐐷"
        );
        List<String> regexes = List.of(
            "", "a", "aa", "public class", "JNIEXPORT", "中", "😀", "\u0000",
            "a.*", "^a", "a$", "a+b", "[a-z]", "(?:a|b)", "\\p{L}+", "\\d+",
            "(?=class)", "(?<=a)b", "(a)\\1", "(?s)class.*", "(?i)string", ".*",
            "\\Qpublic class\\E", "\\Q(?s).*\\E", "\\Q\\E", "\\Qa\\Qb\\E"
        );
        int[] flags = {0, Pattern.LITERAL, Pattern.CASE_INSENSITIVE,
                Pattern.MULTILINE, Pattern.DOTALL, Pattern.UNICODE_CASE,
                Pattern.CANON_EQ, Pattern.COMMENTS,
                Pattern.LITERAL | Pattern.CASE_INSENSITIVE};
        // Cross the pattern/flag lattice with the code-like and Unicode corpus.
        for (String regex : regexes) for (int flag : flags) {
            oracle(regex, flag, examples);
        }
        for (long seed = 0; seed < 30; seed++) {
            List<String> sample = corpus(seed * 67L + 18L, 60);
            for (String regex : regexes) oracle(regex, 0, sample);
            for (String raw : List.of("class", "a+b", "中", "😀", "(?s)", "\\Qx\\E")) {
                oracle(raw, Pattern.LITERAL, sample);
            }
        }
        // Deliberately invalid/unknown precompute hypotheses must refuse without execution.
        reject(() -> M3RegexLiteralPlan.compile("[", 0), PatternSyntaxException.class);
        reject(() -> M3RegexLiteralPlan.compile("a", -1), IllegalArgumentException.class);
        reject(() -> M3RegexLiteralPlan.compile(null, 0), NullPointerException.class);
        reject(() -> M3RegexLiteralPlan.compile("a", 0).select(null,
                M3RegexLiteralPlan.Backend.JAVA), NullPointerException.class);
        reject(() -> M3RegexLiteralPlan.compile("a", 0).select(List.of("a"), null),
                NullPointerException.class);
        // Unpaired surrogates are legal java.util.regex inputs but cannot be UTF-8 snapshots.
        List<String> malformed = List.of("a\uD800b", "z\uDC00", "a");
        oracle("a", 0, List.of("a", "z"));
        equal(M3RegexLiteralPlan.compile("a", 0).select(malformed, M3RegexLiteralPlan.Backend.AUTO),
                jdk(Pattern.compile("a"), malformed), "AUTO malformed UTF-16 fallback");
        reject(() -> M3RegexLiteralPlan.compile("a", 0).select(malformed,
                M3RegexLiteralPlan.Backend.NATIVE_REQUIRED), IllegalArgumentException.class);
        // The native prefilter never becomes an authority for captures, Java Pattern handles them.
        check(!M3RegexLiteralPlan.compile("(a)\\1", 0).signals().exactLiteral(), "backreference held");
        check(!M3RegexLiteralPlan.compile("(?<=a)b", 0).signals().exactLiteral(), "lookbehind held");
        // Immutable compiled signal reused across virtual-thread fan-out; every result
        // is joined and compared with the same serial JDK oracle before acceptance.
        M3RegexLiteralPlan shared = M3RegexLiteralPlan.compile("\\Qclass\\E", 0);
        List<String> concurrentText = corpus(0x83adf10eL, 120);
        int[] concurrentExpected = jdk(Pattern.compile("class"), concurrentText);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            List<java.util.concurrent.Future<int[]>> futures = new ArrayList<>();
            for (int worker = 0; worker < 8; worker++) {
                futures.add(executor.submit(() -> {
                    for (int repetition = 0; repetition < 40; repetition++) {
                        int[] actual = shared.select(concurrentText,
                                M3RegexLiteralPlan.Backend.NATIVE_REQUIRED);
                        if (!Arrays.equals(actual, concurrentExpected)) {
                            throw new AssertionError("virtual-thread JNI result drift");
                        }
                    }
                    return shared.select(concurrentText,
                            M3RegexLiteralPlan.Backend.NATIVE_REQUIRED);
                }));
            }
            for (var future : futures) {
                try { equal(future.get(), concurrentExpected, "fanout/fanin JNI precompute"); }
                catch (java.util.concurrent.ExecutionException ex) {
                    throw new AssertionError("fanout JNI failure", ex.getCause());
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("concurrent native test interrupted", interrupted);
        }
        System.out.println("M3_UNIX_REGEX_PRECOMPUTE_PASS checks=" + checks + " patterns="
                + regexes.size() + " flags=" + flags.length + " seeds=30");
    }
}
