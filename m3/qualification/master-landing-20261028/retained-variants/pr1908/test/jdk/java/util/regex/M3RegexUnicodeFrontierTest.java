/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Preserve complete Unicode frontiers, surrogate alternatives and shared Pattern reuse
 * @modules java.base/java.util.regex:open
 * @run main/othervm M3RegexUnicodeFrontierTest
 */
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public final class M3RegexUnicodeFrontierTest {
    private static final AtomicLong CHECKS = new AtomicLong();
    private static void check(boolean value, String why) {
        CHECKS.incrementAndGet();
        if (!value) throw new AssertionError(why);
    }
    private static Pattern admitted(String expression, int flags) throws Exception {
        Pattern p = Pattern.compile(expression, flags);
        Field f = Pattern.class.getDeclaredField("m3BooleanPlan");
        f.setAccessible(true);
        check(f.get(p) != null, "missing admitted plan: " + expression);
        return p;
    }
    private static void compare(Pattern p, String s) {
        boolean expected = p.matcher(new StringBuilder(s)).matches();
        check(p.asMatchPredicate().test(s) == expected,
                "differential pattern=" + p + " flags=" + p.flags() + " units=" + s.length());
    }
    private static void literal(String expression, String input, boolean expected) throws Exception {
        Pattern p = admitted(expression, 0);
        check(p.matcher(new StringBuilder(input)).matches() == expected, "literal reference");
        check(p.asMatchPredicate().test(input) == expected, "literal frontier: " + expression);
    }
    public static void main(String[] args) throws Exception {
        literal("a*\u03b2*", "\u03b2\u03b2", true);
        literal("a*\u03b2*", "\u03b2\u00e9", false);
        literal("a*\u03b2*", "", true);
        literal("a*\u03b2*", "aaa\u03b2", true);
        literal("[a-z]*", "\ud83d\ude00aaaa", false);
        literal(".*z", "\ud83d\ude00z", true);
        literal(".*z", "\ud83d\ude00q", false);
        literal("a*.*", "\ud83d\ude00", true);
        literal("[a-z]*.*z", "\ud800z", true);
        literal("[a-z]*.*z", "\udc00z", true);

        List<String> samples = List.of("", "a", "b", "z", "\u03b2", "\u00e9", "\u0085",
                "\u2028", "\u2029", "\ud800", "\udc00", "\ud83d\ude00",
                "a\ud83d\ude00z", "\ud800\ud800\udc00z", "\udc00\ud800z",
                "a\u03b2\u00e9z", "\u03b2".repeat(64), "a".repeat(128),
                "\ud83d\ude00".repeat(128), "\u00e9".repeat(128), "\n", "\r\n");
        int[] flags = {0, Pattern.DOTALL, Pattern.UNIX_LINES,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE, Pattern.UNICODE_CHARACTER_CLASS};
        String[] expressions = {"a*\u03b2*", "[a-z]*.*z", "[^a]*b", "\\p{L}*z", "\\w*",
                "[\\u0000-\\uffff]*z", "[\\ud800-\\udbff]*[\\udc00-\\udfff]*",
                "[a-z]*[\\x{1f600}]*z", ".*z", "a*.*", "[a-z]*"};
        for (String expression : expressions) for (int flag : flags) {
            Pattern p = Pattern.compile(expression, flag);
            for (String input : samples) compare(p, input);
        }
        for (int n : new int[] {1, 2, 62, 63, 64, 65, 127, 128, 255, 256}) {
            Pattern p = admitted("[a-z]*".repeat(n), 0);
            for (String input : samples) compare(p, input);
            Pattern alternative = admitted("a*".repeat(n - 1) + "\u03b2*", 0);
            for (String input : List.of("", "\u03b2", "\u03b2\u03b2", "aaa\u03b2", "\u00e9", "\ud800")) compare(alternative, input);
        }
        Random random = new Random(0x4d33554e49434f44L);
        String[] atoms = {"a", "\u03b2", ".", "a*", "\u03b2*", ".*", "[a-z]*", "\\p{L}*"};
        String[] units = {"a", "b", "z", "\u03b2", "\u00e9", "\ud800", "\udc00", "\ud83d\ude00", "\n"};
        for (int trial = 0; trial < 8000; trial++) {
            StringBuilder expression = new StringBuilder(), input = new StringBuilder();
            for (int n = 1 + random.nextInt(8); n > 0; n--) expression.append(atoms[random.nextInt(atoms.length)]);
            for (int n = random.nextInt(8); n > 0; n--) input.append(units[random.nextInt(units.length)]);
            compare(Pattern.compile(expression.toString(), flags[random.nextInt(flags.length)]), input.toString());
        }
        Pattern shared = admitted("a*\u03b2*", 0);
        CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int worker = 0; worker < 2; worker++) tasks.add(pool.submit(() -> {
                ready.countDown();
                try { if (!go.await(30, TimeUnit.SECONDS)) throw new AssertionError("start timeout"); }
                catch (InterruptedException ex) { throw new AssertionError(ex); }
                for (int n = 0; n < 1000; n++) {
                    check(!shared.asMatchPredicate().test("\u00e9\ud83d\ude00"), "shared dead");
                    check(shared.asMatchPredicate().test("aaa\u03b2"), "shared reuse");
                }
            }));
            check(ready.await(30, TimeUnit.SECONDS), "workers ready"); go.countDown();
            for (var future : tasks) future.get(30, TimeUnit.SECONDS);
        }
        System.out.println("UNICODE_FRONTIER_PASS checks=" + CHECKS.get());
    }
}
