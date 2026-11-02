// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Exact UTF-16 and JNI contracts; run in separate native and Java-fallback JVMs. */
public final class M3TextKernelRegression {
    private static final Class<?> LITERAL = type("jdk.m3.text.NativeUtf16Literal");
    private static final Method COMPILE = method(LITERAL, "compile", CharSequence.class);
    private static final Method CONTAINS = method(type("jdk.m3.text.NativeUtf16Literal$Compiled"),
            "contains", CharSequence.class);
    private static int literalCases;
    private static int lineCases;

    private M3TextKernelRegression() { }

    private static Class<?> type(String name) {
        try { return Class.forName(name); }
        catch (ClassNotFoundException failure) { throw new AssertionError(failure); }
    }

    private static Method method(Class<?> owner, String name, Class<?>... arguments) {
        try {
            Method result = owner.getDeclaredMethod(name, arguments);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private static Object call(Method method, Object receiver, Object... arguments) {
        try { return method.invoke(receiver, arguments); }
        catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private static void same(Object expected, Object actual) {
        if (!Objects.equals(expected, actual))
            throw new AssertionError("expected " + expected + ", got " + actual);
    }

    private static void fails(Class<? extends RuntimeException> expected, Runnable operation) {
        try { operation.run(); }
        catch (RuntimeException failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("missing " + expected.getName());
    }

    private static void literal(String text, String needle) {
        Object plan = call(COMPILE, null, needle);
        boolean expected = text.contains(needle);
        same(expected, call(CONTAINS, plan, text));
        same(expected, call(CONTAINS, plan, new StringBuilder(text)));
        same(expected, call(CONTAINS, plan, new Segments(text, text.length() / 2)));
        same(expected, call(CONTAINS, plan, text)); // Reusing a plan must reset feed state.
        literalCases++;
    }

    private static String randomUnits(Random random, int length) {
        char[] units = new char[length];
        String alphabet = "aab\u0000\u00ff\u0100\ud800\udc00\uffff";
        for (int i = 0; i < length; i++) units[i] = random.nextInt(4) == 0
                ? (char) random.nextInt(65536) : alphabet.charAt(random.nextInt(alphabet.length()));
        return new String(units);
    }

    private static void literals() {
        String[] samples = {"", "a", "b", "aaab", "\u0000", "x\u0000y", "\ud800", "\udc00",
                "\ud800\udc00", "\uffff\u0000\ud800", "abababac", "\u0100\u00ff"};
        for (String text : samples) for (String needle : samples) literal(text, needle);
        for (int boundary : new int[] {8191, 8192, 8193, 16383, 16384, 16385}) {
            String text = "a".repeat(boundary) + "\u0000\ud800\udc00z";
            literal(text, "aa\u0000\ud800\udc00");
            literal(text, "aa\u0000\ud800\udc01");
            literal(text, "a".repeat(8193) + "\u0000");
        }
        literal("ab".repeat(9000) + "c", "ab".repeat(5000) + "c");
        literal("a".repeat(20000), "a".repeat(9000) + "b");
        Random random = new Random(0x4D335554463136L);
        for (int run = 0; run < 8000; run++) {
            String text = randomUnits(random, random.nextInt(257));
            String needle;
            if (!text.isEmpty() && random.nextBoolean()) {
                int from = random.nextInt(text.length() + 1);
                needle = text.substring(from, from + random.nextInt(text.length() - from + 1));
            } else needle = randomUnits(random, random.nextInt(33));
            literal(text, needle);
        }
        StringBuilder mutablePattern = new StringBuilder("ab");
        Object snapshot = call(COMPILE, null, mutablePattern);
        mutablePattern.setCharAt(0, 'z');
        same(true, call(CONTAINS, snapshot, "ab"));
        same(false, call(CONTAINS, snapshot, "zb"));
        fails(NullPointerException.class, () -> call(COMPILE, null, (Object) null));
        fails(NullPointerException.class, () -> call(CONTAINS, snapshot, (Object) null));
    }

    private static void observations() {
        Object plan = call(COMPILE, null, "a");
        Observed text = new Observed("abx", -1);
        same(true, call(CONTAINS, plan, text));
        same(List.of("length", "length", "length", "length", "char0", "char1", "char2"), text.calls);
        Observed throwing = new Observed("abx", 2);
        fails(IllegalStateException.class, () -> call(CONTAINS, plan, throwing));
        same(text.calls, throwing.calls);
        Observed empty = new Observed("abx", -1);
        same(true, call(CONTAINS, call(COMPILE, null, ""), empty));
        same(List.of(), empty.calls);
        Observed shortText = new Observed("a", -1);
        same(false, call(CONTAINS, call(COMPILE, null, "ab"), shortText));
        same(List.of("length"), shortText.calls);
    }

    private static void nativeLines() {
        Class<?> owner = type("jdk.m3.text.NativeLineOps");
        same(true, call(method(owner, "available"), null));
        Method sort = method(owner, "sort0", String[].class, int[].class, boolean.class);
        Method uniq = method(owner, "uniq0", String[].class, long[].class);
        String[] samples = {"", "\u0000", "\u0000".repeat(8), "\u0000".repeat(9),
                "1234567", "12345678", "12345678a", "12345678b", "12345678\u0000",
                "12345678\u0000a", "12345678\ud800", "12345678\udc00", "12345678\uffff",
                "a".repeat(255), "a".repeat(256), "a".repeat(257),
                "a".repeat(520) + "X", "a".repeat(520) + "Y", "\ud800", "\udc00"};
        Random random = new Random(0x4D334B45524EL);
        for (int size : new int[] {0, 1, 2, 3, 63, 64, 65, 127, 128, 129, 255, 256, 257}) {
            for (int run = 0; run < 20; run++) {
                String[] lines = new String[size];
                for (int i = 0; i < size; i++) lines[i] = random.nextInt(4) == 0
                        ? "12345678" + randomUnits(random, random.nextInt(300))
                        : samples[random.nextInt(samples.length)];
                String[] snapshot = lines.clone();
                for (boolean reverse : new boolean[] {false, true}) {
                    Integer[] expected = new Integer[size];
                    for (int i = 0; i < size; i++) expected[i] = i;
                    Arrays.sort(expected, (a, b) -> reverse
                            ? lines[b].compareTo(lines[a]) : lines[a].compareTo(lines[b]));
                    int[] actual = new int[size];
                    call(sort, null, lines, actual, reverse);
                    for (int i = 0; i < size; i++) same(expected[i], actual[i]);
                }
                long[] expected = new long[(size + 63) >>> 6];
                for (int i = 0; i < size; i++)
                    if (i == 0 || !lines[i].equals(lines[i - 1])) expected[i >>> 6] |= 1L << (i & 63);
                long[] actual = new long[expected.length];
                call(uniq, null, lines, actual);
                if (!Arrays.equals(expected, actual)) throw new AssertionError("adjacent keep mask");
                if (!Arrays.equals(snapshot, lines)) throw new AssertionError("input descriptor mutation");
                lineCases++;
            }
        }
        fails(NullPointerException.class, () -> call(sort, null, null, new int[0], false));
        fails(NullPointerException.class, () -> call(sort, null, new String[] {null}, new int[1], false));
        fails(IllegalArgumentException.class, () -> call(sort, null, new String[1], new int[0], false));
        fails(IllegalArgumentException.class, () -> call(uniq, null, new String[] {"a"}, new long[0]));
    }

    private static void nativeFeeds() {
        Method feed = method(LITERAL, "feed0", char[].class, int.class, char[].class, int[].class, int.class);
        char[] page = new char[8192];
        Arrays.fill(page, 'a');
        char[] needle = "aaaaab".toCharArray();
        int[] failure = {0, 1, 2, 3, 4, 0};
        same(5, call(feed, null, page, page.length, needle, failure, 0));
        same(-1, call(feed, null, new char[] {'b'}, 1, needle, failure, 5));
        same(5, call(feed, null, new char[0], 0, needle, failure, 5));
        fails(NullPointerException.class, () -> call(feed, null, null, 0, needle, failure, 0));
        fails(IllegalArgumentException.class, () -> call(feed, null, page, -1, needle, failure, 0));
        fails(IllegalArgumentException.class, () -> call(feed, null, page, 8193, needle, failure, 0));
        fails(IllegalArgumentException.class, () -> call(feed, null, new char[8193], 1, needle, failure, 0));
        fails(IllegalArgumentException.class, () -> call(feed, null, page, 1, new char[0], new int[0], 0));
        fails(IllegalArgumentException.class, () -> call(feed, null, page, 1, needle, new int[1], 0));
        fails(IllegalArgumentException.class, () -> call(feed, null, page, 1, needle, failure, -1));
        fails(IllegalArgumentException.class, () -> call(feed, null, page, 1, needle, failure, needle.length));
        fails(IllegalArgumentException.class, () -> call(feed, null, new char[] {'x'}, 1,
                new char[] {'a', 'b'}, new int[] {1, 0}, 1));
    }

    public static void main(String[] arguments) {
        if (arguments.length != 1 || !(arguments[0].equals("native") || arguments[0].equals("java")))
            throw new IllegalArgumentException("native or java mode required");
        boolean nativeMode = arguments[0].equals("native");
        same(nativeMode, call(method(LITERAL, "nativeAvailable"), null));
        literals();
        observations();
        if (nativeMode) { nativeLines(); nativeFeeds(); }
        System.out.println("M3_TEXT_KERNEL_REGRESSION_PASS mode=" + arguments[0]
                + " literalCases=" + literalCases + " lineCases=" + lineCases);
    }

    private static final class Segments implements CharSequence {
        private final String left;
        private final String right;
        Segments(String text, int split) { left = text.substring(0, split); right = text.substring(split); }
        @Override public int length() { return left.length() + right.length(); }
        @Override public char charAt(int index) {
            Objects.checkIndex(index, length());
            return index < left.length() ? left.charAt(index) : right.charAt(index - left.length());
        }
        @Override public CharSequence subSequence(int from, int to) { throw new AssertionError("flatten"); }
        @Override public String toString() { throw new AssertionError("flatten"); }
    }

    private static final class Observed implements CharSequence {
        private final String text;
        private final int failAt;
        private final List<String> calls = new ArrayList<>();
        Observed(String text, int failAt) { this.text = text; this.failAt = failAt; }
        @Override public int length() { calls.add("length"); return text.length(); }
        @Override public char charAt(int index) {
            calls.add("char" + index);
            if (index == failAt) throw new IllegalStateException("observed read ahead");
            return text.charAt(index);
        }
        @Override public CharSequence subSequence(int from, int to) { throw new AssertionError("flatten"); }
        @Override public String toString() { throw new AssertionError("flatten"); }
    }
}
