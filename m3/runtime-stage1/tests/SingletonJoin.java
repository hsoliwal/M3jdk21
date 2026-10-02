/*
 * Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Verify singleton join backing reuse with normal String/VM semantics
 * @modules java.base/java.lang:open
 * @run main/othervm -ea SingletonJoin
 * @run main/othervm -ea -Xint SingletonJoin
 * @run main/othervm -ea -XX:-CompactStrings SingletonJoin
 * @run main/othervm -ea -Xbatch -XX:-TieredCompilation SingletonJoin
 */
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SingletonJoin {
    static final boolean PATCHED = Boolean.parseBoolean(System.getProperty("m3.patched", "true"));
    static final boolean CHECK_BACKING = Boolean.parseBoolean(System.getProperty("m3.checkBacking", "true"));
    static final Field VALUE;
    static int checks;
    static {
        try { VALUE = String.class.getDeclaredField("value"); VALUE.setAccessible(true); }
        catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    static void exact(String source, String result) throws Exception {
        check(source != result);
        check(source.equals(result) && result.equals(source));
        check(source.hashCode() == result.hashCode());
        check(source.compareTo(result) == 0 && source.contentEquals(result));
        check(Arrays.equals(source.toCharArray(), result.toCharArray()));
        if (CHECK_BACKING) check((VALUE.get(source) == VALUE.get(result)) == PATCHED);
        check(source.intern() == result.intern());
    }
    static void allRoutes(String source) throws Exception {
        exact(source, String.join("unused\u0100", source));
        exact(source, String.join("unused\u0100", List.of(source)));
        exact(source, new StringJoiner("unused\u0100").add(source).toString());
        exact(source, Stream.of(source).collect(Collectors.joining("unused\u0100")));
        check(String.join("-", source, "tail").equals(source + "-tail"));
        check(new StringJoiner("-", "[", "]").add(source).toString().equals("[" + source + "]"));
    }
    static void expectNpe(Runnable action) {
        try { action.run(); throw new AssertionError("NPE required"); }
        catch (NullPointerException expected) { checks++; }
    }
    static final class Sequence implements CharSequence {
        final String text; final String name; final StringBuilder calls;
        Sequence(String text, String name, StringBuilder calls) {
            this.text = text; this.name = name; this.calls = calls;
        }
        public int length() { throw new AssertionError("unexpected length callback"); }
        public char charAt(int i) { throw new AssertionError("unexpected charAt callback"); }
        public CharSequence subSequence(int a, int b) { throw new AssertionError("unexpected slice"); }
        public String toString() { calls.append(name); return text; }
    }
    public static void main(String[] args) throws Exception {
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            allRoutes(new String(new char[] {(char) unit}));
        }
        for (String source : List.of("", "Aa", "BB", "\0", "\0\0", "\ud800", "\udc00",
                "\ud83d\ude00", "a\ud800z\udc00", "日本語", "x".repeat(4096))) {
            allRoutes(source);
            String result = String.join("", source);
            for (Charset charset : List.of(StandardCharsets.UTF_8, StandardCharsets.UTF_16,
                    StandardCharsets.UTF_16LE, StandardCharsets.ISO_8859_1, Charset.forName("ISO-2022-JP"))) {
                check(Arrays.equals(source.getBytes(charset), result.getBytes(charset)));
            }
            check(result.substring(0).equals(source));
            check(result.matches(java.util.regex.Pattern.quote(source)));
            check((result + "suffix").equals(source + "suffix"));
        }
        check(String.join("", "\ud83d", "\ude00").equals("\ud83d\ude00"));
        check(String.join("", (CharSequence) null).equals("null"));
        check(String.join("", List.of()).isEmpty());
        expectNpe(() -> String.join(null, "x"));
        expectNpe(() -> String.join("", (CharSequence[]) null));
        expectNpe(() -> String.join("", (Iterable<CharSequence>) null));
        StringBuilder calls = new StringBuilder();
        check(String.join(new Sequence("", "d", calls), new Sequence("x", "e", calls)).equals("x"));
        check(calls.toString().equals("de"));
        StringBuilder mutable = new StringBuilder("before");
        String snapshot = String.join("", mutable); mutable.setCharAt(0, 'X');
        check(snapshot.equals("before"));
        char[] ingress = {'a', '\ud800', 'z'};
        String source = new String(ingress); String result = String.join("", source);
        Arrays.fill(ingress, 'X'); Arrays.fill(result.toCharArray(), 'Y'); Arrays.fill(result.getBytes(), (byte) 0);
        check(result.equals("a\ud800z") && source.equals(result));
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] threads = new Thread[4];
        for (int t = 0; t < threads.length; t++) {
            threads[t] = new Thread(() -> {
                try {
                    for (int i = 0; i < 20000; i++) {
                        String joined = String.join("", source);
                        if (!joined.equals(source) || joined.hashCode() != source.hashCode()
                                || joined.intern() != source.intern()) throw new AssertionError("concurrent join");
                    }
                } catch (Throwable ex) { failure.compareAndSet(null, ex); }
            }); threads[t].start();
        }
        for (Thread thread : threads) thread.join();
        if (failure.get() != null) throw new AssertionError(failure.get());
        System.gc(); check(result.equals(source));
        System.out.println("SINGLETON_JOIN_PASS checks=" + checks + " concurrent_joins=80000 patched=" + PATCHED);
    }
}
