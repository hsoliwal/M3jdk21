/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary Bounded compiled-node Boolean DP preserves Pattern and String contracts
 * @run main/othervm --add-opens java.base/java.util.regex=ALL-UNNAMED M3PatternDpTest candidate
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3PatternDpTest {
    private static long checks;
    private static final MessageDigest DIGEST;
    static {
        try { DIGEST = MessageDigest.getInstance("SHA-256"); }
        catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
    }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static void pair(Pattern pattern, String text) {
        boolean expected = pattern.matcher(new StringBuilder(text)).matches();
        boolean actual = pattern.asMatchPredicate().test(text);
        check(expected == actual, "predicate: " + pattern + " text=" + text);
        DIGEST.update((byte) (actual ? 1 : 0));
        if (pattern.flags() == 0) {
            check(Pattern.matches(pattern.pattern(), text) == expected, "static matches");
            check(text.matches(pattern.pattern()) == expected, "String.matches");
        }
    }
    private static List<String> words(String[] alphabet, int max) {
        List<String> all = new ArrayList<>();
        List<String> level = List.of("");
        all.addAll(level);
        for (int size = 1; size <= max; size++) {
            List<String> next = new ArrayList<>();
            for (String word : level) for (String unit : alphabet) next.add(word + unit);
            all.addAll(next);
            level = next;
        }
        return all;
    }
    private static Pattern roundTrip(Pattern pattern) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) { out.writeObject(pattern); }
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (Pattern) in.readObject();
        }
    }
    private static Object plan(Pattern pattern) throws Exception {
        Field field = Pattern.class.getDeclaredField("m3BooleanPlan");
        field.setAccessible(true);
        return field.get(pattern);
    }
    public static void main(String[] args) throws Exception {
        boolean candidate = args.length > 0 && args[0].equals("candidate");
        for (String regex : words(new String[]{"a", "b", ".", "a*", "b*", ".*"}, 3)) {
            Pattern pattern = Pattern.compile(regex);
            for (String text : words(new String[]{"a", "b"}, 6)) pair(pattern, text);
        }
        String[] expressions = {
            "a*", ".*", ".a*", "a*.", "a*b*", "c*a*b", "mis*is*p*.", "a*bc.*", "[ab]*c*",
            "[^x]*a*", "\\d*\\s*", "\\p{L}*.*", "(?i)a*b*", "(?s).*a*", "(?d).*a*",
            "(?U)\\w*.*", "\\Q.*\\E.*", "é*.*", "\\x{1f600}*.*", "\\uD800*.*",
            "\\uDC00*.*", "[\\uD800-\\uDFFF]*.*", "a+", "a?", "a{2,4}", "a{0,}b*",
            "a*?a", "a*+a", "(?:a*)b*", "(a*)\\1", "(?=a)a*", "(?<=a)b*",
            "(a|b)*", "^a*$", "\\A.*\\z", "\\R*", "\\X*", "(?<x>a*)", "", "literal"
        };
        String[] texts = {"", "a", "aa", "ab", "aab", "mississippi", "bc", "abc", "\n", "\r\n",
            "\u0085", "\u2028", "\u2029", "é", "É", "e\u0301", "一", "123 ", "\u0000",
            "\uD800", "\uDC00", "\uD83D\uDE00", "a\uD83D\uDE00b", "a\uD800b",
            "class X { String s = \"a*b*\"; }", ".*"};
        int[] flags = {0, Pattern.DOTALL, Pattern.UNIX_LINES, Pattern.CASE_INSENSITIVE,
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE, Pattern.UNICODE_CHARACTER_CLASS,
            Pattern.COMMENTS, Pattern.CANON_EQ, Pattern.LITERAL};
        for (String expression : expressions) for (int flag : flags) {
            Pattern pattern = Pattern.compile(expression, flag);
            for (String text : texts) pair(pattern, text);
        }
        Random random = new Random(0x4d334450);
        String[] atoms = {"a", "a*", "[ab]*", ".*", "\\p{L}*", "é*", "[^x]*", "\\s*"};
        for (int sample = 0; sample < 4000; sample++) {
            StringBuilder expression = new StringBuilder();
            for (int j = 0, n = 1 + random.nextInt(7); j < n; j++) expression.append(atoms[random.nextInt(atoms.length)]);
            StringBuilder text = new StringBuilder();
            for (int j = 0, n = random.nextInt(10); j < n; j++) text.append(texts[random.nextInt(texts.length)]);
            pair(Pattern.compile(expression.toString(), flags[random.nextInt(6)]), text.toString());
        }
        for (String expression : List.of("", "a*b*", "(a*)\\1", "(?s).*a*")) {
            Pattern restored = roundTrip(Pattern.compile(expression));
            for (String text : texts) pair(restored, text);
        }
        try { Pattern.matches("*", null); throw new AssertionError("syntax precedence"); }
        catch (PatternSyntaxException expected) { checks++; }
        try { Pattern.matches("a*", null); throw new AssertionError("null"); }
        catch (NullPointerException expected) { checks++; }
        try { Pattern.compile("a*").asMatchPredicate().test(null); throw new AssertionError("predicate null"); }
        catch (NullPointerException expected) { checks++; }

        Pattern shared = Pattern.compile("[ab]*a*b*");
        var predicate = shared.asMatchPredicate();
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            final int seed = i;
            tasks.add(() -> {
                Random r = new Random(seed);
                for (int j = 0; j < 200; j++) {
                    String text = texts[r.nextInt(texts.length)];
                    if (predicate.test(text) != shared.matcher(text).matches()) return false;
                }
                return true;
            });
        }
        try (var pool = Executors.newFixedThreadPool(4)) {
            for (var result : pool.invokeAll(tasks)) check(result.get(), "thread-confined DP");
        }
        System.out.println("common checks=" + checks + " digest=" + HexFormat.of().formatHex(DIGEST.digest()));
        if (candidate) {
            for (String expression : List.of("a*", "a*b*", "c*a*b", "[ab]*c*", "a*bc.*", "(?s).*a*")) {
                Pattern p = Pattern.compile(expression);
                Object old = plan(p);
                check(old != null, "admitted " + expression);
                p.asMatchPredicate().test("ab");
                check(old == plan(p), "plan reused");
            }
            for (String expression : List.of("a+", "a?", "a*+a", "(a*)", "(?=a)a*", "(a|b)*", "^a*$", "literal")) {
                check(plan(Pattern.compile(expression)) == null, "fallback " + expression);
            }
            check(plan(Pattern.compile("a*", Pattern.CANON_EQ)) == null, "canon eq fallback");
            check(plan(Pattern.compile("a*".repeat(256))) != null, "256 boundary");
            check(plan(Pattern.compile("a*".repeat(257))) == null, "257 fallback");
            Pattern hostile = Pattern.compile("a*".repeat(32) + "b");
            check(!hostile.asMatchPredicate().test("a".repeat(100_000)), "bounded hostile negative");
            check(hostile.asMatchPredicate().test("a".repeat(100_000) + "b"), "bounded hostile positive");
            check(Pattern.matches("a*".repeat(32) + "b", "a".repeat(100_000) + "b"), "static hostile");
            System.out.println("candidate admission and hostile checks passed");
        }
    }
}
