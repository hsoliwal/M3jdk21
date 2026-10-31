/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Repeated canonical UTF-16 fragments receive definite dot/star decisions
 * @run main/othervm -Xint -Xshare:off --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexPreparedUtf16Test enabled
 * @run main/othervm -Xint -Xshare:off --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexPreparedUtf16Test enabled
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringRegexPreparedUtf16Test disabled
 */
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Removing the prepared caller must fail the first strict surrogate decision. */
public final class M3StringRegexPreparedUtf16Test {
    private static final AtomicInteger checks = new AtomicInteger();
    private static Field storage;
    private static Method decision;
    private static Method owner;
    private static Method coordinate;
    private static Method start;
    private static Method length;
    private static Class<?> tupleClass;
    private static Field left;
    private static Field right;

    private static void check(boolean value, String label) {
        int index = checks.incrementAndGet();
        if (!value) throw new AssertionError(label + " at check " + index);
    }

    private static Method method(Class<?> type, String name, Class<?>... arguments)
            throws ReflectiveOperationException {
        Method result = type.getDeclaredMethod(name, arguments);
        result.setAccessible(true);
        return result;
    }

    private static void initialize() throws ReflectiveOperationException {
        storage = String.class.getDeclaredField("m3");
        storage.setAccessible(true);
        Class<?> type = Class.forName("java.lang.M3String");
        decision = method(Class.forName("java.lang.M3StringRegexDp"), "tryMatch", type, String.class);
        owner = method(type, "owner");
        coordinate = method(type, "coordinate");
        start = method(type, "start");
        length = method(type, "length");
        tupleClass = Class.forName("java.lang.M3StringTuple");
        left = tupleClass.getDeclaredField("left");
        right = tupleClass.getDeclaredField("right");
        left.setAccessible(true);
        right.setAccessible(true);
    }

    private static Object canonical(String text) throws IllegalAccessException {
        Object value = storage.get(text);
        check(value != null, "actual M3 storage required");
        return value;
    }

    private static int direct(String text, String regex) throws ReflectiveOperationException {
        try {
            return (int) decision.invoke(null, canonical(text), regex);
        } catch (InvocationTargetException failure) {
            throw new AssertionError("unexpected direct receiver failure", failure.getCause());
        }
    }

    private static String repeated(String unit, int levels) {
        String value = new String(unit.toCharArray());
        for (int i = 0; i < levels; i++) value = value.concat(value);
        return value;
    }

    private static void requireRepeatedGeometry(String text, int levels) throws ReflectiveOperationException {
        Object value = canonical(text);
        for (int i = 0; i < levels; i++) {
            Object identity = owner.invoke(value);
            check(tupleClass.isInstance(identity), "real repeated tuple required");
            Object first = left.get(identity);
            Object second = right.get(identity);
            check(owner.invoke(first) == owner.invoke(second), "repeated child owner identity");
            check(coordinate.invoke(first).equals(coordinate.invoke(second)), "repeated child range identity");
            check((int) start.invoke(value) == 0, "whole repeated owner range");
            check((int) length.invoke(value) == 2 * (int) length.invoke(first), "repeated length geometry");
            value = first;
        }
    }

    private static boolean oracle(String text, String regex) {
        return Pattern.compile(regex).matcher(new StringBuilder(text)).matches();
    }

    private static void parity(String text, String regex) {
        boolean expected = oracle(text, regex);
        check(text.matches(regex) == expected, "public Pattern parity");
    }

    private static void strict(String text, String regex, boolean expected) throws ReflectiveOperationException {
        check(oracle(text, regex) == expected, "independent literal expectation");
        check(direct(text, regex) == (expected ? 1 : 0), "definite prepared UTF16 decision");
        check(text.matches(regex) == expected, "public prepared result");
    }

    private static void strictCorpus() throws ReflectiveOperationException {
        // Boundary-free supplementary effects can use repeat0; a disabled caller yields UNKNOWN.
        String nativeText = repeated("a\uD83D\uDE00b", 8);
        requireRepeatedGeometry(nativeText, 8);
        strict(nativeText, ".*".repeat(16), true);
        strict(nativeText, ".*".repeat(63), true);
        strict(nativeText, "a*", false);
        // A fragment-only result would match: applying its power must reject this length.
        strict(nativeText, "a*...", false);
        strict(repeated("\uD800", 8), "a*.", false);
        check(direct(nativeText, ".*".repeat(64)) == -1, "64-atom prepared bound retains fallback");
        parity(nativeText, ".*".repeat(64));
        String smaller = repeated("a\uD83D\uDE00b", 7);
        check(direct(smaller, ".*".repeat(16)) == -1, "128-copy cohort retains fallback");
        parity(smaller, ".*".repeat(16));
        String[] units = {"\uD800", "\uDC00", "\uD800\uDC00", "\uDC00\uD800",
                "a\uD800\uDC00b", "\uDC00\n\uD800", "\uD800a\uDC00", "\u0000\uD800"};
        for (String unit : units) {
            String text = repeated(unit, 8);
            requireRepeatedGeometry(text, 8);
            strict(text, unit.indexOf('\n') < 0 ? ".*".repeat(16) : ".*", unit.indexOf('\n') < 0);
            strict(text, "a*", false);
            if (unit.equals("\uDC00\uD800")) strict(text, "a*..", false);
            // Ranges and broken repeat roots retain the exact ordinary fallback.
            for (int begin = 0; begin < Math.min(3, text.length()); begin++) {
                String range = text.substring(begin, text.length() - 1);
                for (String regex : new String[]{".*", "a*..", "a*.*b*", "(?s).*", "[\\s\\S]*"}) {
                    parity(range, regex);
                }
            }
        }
        for (String text : new String[]{"", "a", "\uD800", "\uD800\uDC00", "a".repeat(8193)}) {
            for (String regex : new String[]{"a*", ".*", ".*".repeat(16)}) parity(text, regex);
        }
        for (String terminator : new String[]{"\n", "\r", "\u0085", "\u2028", "\u2029"}) {
            strict(repeated("\uD800" + terminator + "\uDC00", 8), ".*", false);
        }
    }

    private static void fallbackCorpus() {
        Random random = new Random(0x4d335546313650L);
        String alphabet = "aB0\u0000\n\r\u0085\u2028\u2029\uD800\uDC00\uD83D\uDE00";
        for (int trial = 0; trial < 128; trial++) {
            StringBuilder source = new StringBuilder();
            for (int i = 0, n = 1 + random.nextInt(7); i < n; i++) {
                source.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String text = repeated(source.toString(), 8);
            for (String regex : new String[]{".*", "a*.*B*", "(?s).*", "[\\s\\S]*"}) {
                parity(text, regex);
            }
        }
        for (String regex : new String[]{"*a", "a**", "[", "(?", "\\"}) {
            PatternSyntaxException wanted;
            try {
                Pattern.compile(regex);
                throw new AssertionError("invalid oracle expression admitted");
            } catch (PatternSyntaxException expected) { wanted = expected; }
            try {
                repeated("\uDC00\uD800", 8).matches(regex);
                throw new AssertionError("invalid expression admitted");
            } catch (PatternSyntaxException actual) {
                check(actual.getIndex() == wanted.getIndex()
                        && actual.getPattern().equals(wanted.getPattern()), "syntax failure contract");
            }
        }
        try {
            repeated("\uD800", 8).matches(null);
            throw new AssertionError("null expression admitted");
        } catch (NullPointerException expected) { checks.incrementAndGet(); }
    }

    private static void interruptedAndConcurrent() throws Exception {
        String text = repeated("\uDC00\uD800", 8);
        Thread.currentThread().interrupt();
        try {
            strict(text, ".*".repeat(16), true);
            check(Thread.currentThread().isInterrupted(), "interrupt status preserved");
        } finally {
            Thread.interrupted();
        }
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Boolean>> work = new ArrayList<>();
            for (int thread = 0; thread < 4; thread++) {
                work.add(() -> {
                    for (int i = 0; i < 32; i++) {
                        if (direct(text, ".*".repeat(16)) != 1 || !text.matches(".*".repeat(16))) return false;
                    }
                    return true;
                });
            }
            for (var result : executor.invokeAll(work)) check(result.get(), "concurrent canonical effect decision");
        }
        System.gc();
        strict(text, ".*".repeat(16), true);
    }

    public static void main(String[] arguments) throws Exception {
        boolean enabled = arguments.length == 1 && arguments[0].equals("enabled");
        if (enabled) {
            initialize();
            strictCorpus();
            interruptedAndConcurrent();
        }
        fallbackCorpus();
        System.out.println("M3_PREPARED_UTF16 checks=" + checks.get() + " enabled=" + enabled);
    }
}
