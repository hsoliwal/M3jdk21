/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary M3 canonical String regex candidate signal bitmaps preserve JDK Pattern whole-match semantics
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexRowPrecomputeTest
 */
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class M3StringRegexRowPrecomputeTest {
    private static long checks;
    private static Class<?> m3Class;
    private static Class<?> indexClass;
    private static Method compile;
    private static Method prepare;
    private static Method candidates;
    private static Method matches;
    private static Field stringM3;
    private static Constructor<?> fallbackCtor;

    private static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("M3 regex precompute oracle mismatch #" + checks);
    }

    private static Object canonical(String text) throws Exception {
        if (stringM3 != null) {
            String s = new String(text.toCharArray());
            s.length();
            Object existing = stringM3.get(s);
            check(existing != null);
            return existing;
        }
        return fallbackCtor.newInstance(text);
    }

    private static void init() throws Exception {
        m3Class = Class.forName("java.lang.M3String");
        indexClass = Class.forName("java.lang.M3StringRegexRowPrecompute");
        Class<?> arrayClass = Array.newInstance(m3Class, 0).getClass();
        compile = indexClass.getDeclaredMethod("compile", arrayClass);
        compile.setAccessible(true);
        prepare = indexClass.getDeclaredMethod("prepare", String.class, int.class);
        prepare.setAccessible(true);
        try {
            stringM3 = String.class.getDeclaredField("m3");
            stringM3.setAccessible(true);
        } catch (NoSuchFieldException isolatedFixture) {
            fallbackCtor = m3Class.getDeclaredConstructor(String.class);
            fallbackCtor.setAccessible(true);
        }
    }

    private static String[] subjects() {
        ArrayList<String> corpus = new ArrayList<>(List.of("", "a", "ab", "abc", "ABC", "aBc", "abcx",
                "xabc", "xabcx", "a\n", "abc\r\n", "\uD83D\uDE03", "\uD83D", "\uDE03",
                "a\u0301", "a\u0000a", "a_b", "123", "nasty.*text", "verylongvalue"));
        Random random = new Random(20261016L);
        String alphabet = "abcXYZ012_\r\n\uD83D\uDE03";
        for (int i = 0; i < 512; i++) {
            int size = random.nextInt(32);
            StringBuilder builder = new StringBuilder(size);
            for (int j = 0; j < size; j++) builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
            corpus.add(builder.toString());
        }
        return corpus.toArray(String[]::new);
    }

    private static String randomLiteral(Random r) {
        String chars = "aBcXY012_";
        int size = 1 + r.nextInt(10);
        StringBuilder out = new StringBuilder(size);
        for (int i = 0; i < size; i++) out.append(chars.charAt(r.nextInt(chars.length())));
        return out.toString();
    }

    private static Object compiled(String[] text) throws Exception {
        Object m3 = Array.newInstance(m3Class, text.length);
        for (int i = 0; i < text.length; i++) Array.set(m3, i, canonical(text[i]));
        return compile.invoke(null, m3);
    }

    private static Object plan(Object index, String regex, int flags) throws Exception {
        Object result = prepare.invoke(index, regex, flags);
        if (candidates == null) {
            candidates = result.getClass().getDeclaredMethod("candidateRows");
            candidates.setAccessible(true);
            matches = result.getClass().getDeclaredMethod("matchingRows");
            matches.setAccessible(true);
        }
        return result;
    }

    private static void verify(Object index, String[] corpus, String regex, int flags) throws Exception {
        Pattern authoritative = Pattern.compile(regex, flags);
        Object prepared = plan(index, regex, flags);
        int[] candidateRows = (int[]) candidates.invoke(prepared);
        int[] matched = (int[]) matches.invoke(prepared);
        boolean[] admitted = new boolean[corpus.length];
        for (int row : candidateRows) {
            check(row >= 0 && row < corpus.length);
            check(!admitted[row]);
            admitted[row] = true;
        }
        int expectedCount = 0;
        int previous = -1;
        for (int row = 0; row < corpus.length; row++) {
            if (authoritative.matcher(corpus[row]).matches()) {
                check(admitted[row]);
                expectedCount++;
            }
        }
        check(matched.length == expectedCount);
        for (int row : matched) {
            check(row > previous);
            check(authoritative.matcher(corpus[row]).matches());
            previous = row;
        }
    }

    public static void main(String[] args) throws Exception {
        init();
        String[] corpus = subjects();
        Object index = compiled(corpus);
        String[] patterns = {"", "a", "abc", "ABC", "a.*", ".*a", ".*abc.*", "abc.*", "a+",
                "(a)\\1", "\\p{L}+", "[a-z]+", "\\d+", "(?s).*abc.*", ".*", "a|b", "(a)?"};
        for (String pattern : patterns) {
            verify(index, corpus, pattern, 0);
            verify(index, corpus, pattern, Pattern.CASE_INSENSITIVE);
        }
        Random random = new Random(20261016L);
        for (int trial = 0; trial < 10_000; trial++) {
            String literal = randomLiteral(random);
            String pattern = switch (trial & 3) {
                case 0 -> literal;
                case 1 -> literal + ".*";
                case 2 -> ".*" + literal;
                default -> ".*" + literal + ".*";
            };
            verify(index, corpus, pattern, trial % 29 == 0 ? Pattern.CASE_INSENSITIVE : 0);
        }
        Object shared = plan(index, ".*abc.*", 0);
        try (var workers = Executors.newFixedThreadPool(4)) {
            List<Future<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                tasks.add(workers.submit(() -> {
                    int sum = 0;
                    for (int j = 0; j < 30; j++) {
                        try { sum += ((int[]) matches.invoke(shared)).length; }
                        catch (ReflectiveOperationException problem) { throw new AssertionError(problem); }
                    }
                    return sum;
                }));
            }
            for (Future<Integer> task : tasks) check(task.get().equals(tasks.get(0).get()));
        }
        System.out.println("M3_JDK_REGEX_ROW_PRECOMPUTE_PASS checks=" + checks
                + " regexes=" + (patterns.length * 2 + 10_000) + " rows=" + corpus.length);
    }
}
