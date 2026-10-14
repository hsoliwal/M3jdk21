/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary Qualify existing M3PatternQuery BooleanPlan ASCII precompute and unchanged fallbacks
 * @modules java.base/java.util.regex:open
 * @run main/native M3PatternAsciiFrontierTest require-ascii
 */

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/** Target-native test oracle, not a production JNI dispatch policy or new regex parser. */
public final class M3PatternAsciiFrontierTest {
    private static final AtomicLong ASSERTIONS = new AtomicLong();
    private static final MessageDigest DIGEST;
    private static final Field ROOT;
    private static final Method DERIVE;
    private static final Method MATCHES;
    private static final Field ATOMS;
    private static final Field REPEATED;
    private static final Field EMPTY;
    private static final Method PREDICATE;
    private static final Field ASCII;
    private static long cases;
    private static long nativeCases;
    private static long plans;
    private static boolean requireAscii;
    static {
        try {
            DIGEST = MessageDigest.getInstance("SHA-256");
            ROOT = Pattern.class.getDeclaredField("matchRoot"); ROOT.setAccessible(true);
            Class<?> node = Class.forName("java.util.regex.Pattern$Node");
            Class<?> query = Class.forName("java.util.regex.M3PatternQuery");
            DERIVE = query.getDeclaredMethod("deriveBoolean", node); DERIVE.setAccessible(true);
            Class<?> plan = Class.forName("java.util.regex.M3PatternQuery$BooleanPlan");
            MATCHES = plan.getDeclaredMethod("matches", String.class); MATCHES.setAccessible(true);
            ATOMS = plan.getDeclaredField("atoms"); ATOMS.setAccessible(true);
            REPEATED = plan.getDeclaredField("repeated"); REPEATED.setAccessible(true);
            EMPTY = plan.getDeclaredField("empty"); EMPTY.setAccessible(true);
            PREDICATE = Class.forName("java.util.regex.Pattern$CharPredicate").getDeclaredMethod("is", int.class);
            PREDICATE.setAccessible(true);
            Field field;
            try { field = plan.getDeclaredField("ascii"); field.setAccessible(true); }
            catch (NoSuchFieldException baseline) { field = null; }
            ASCII = field;
        } catch (ReflectiveOperationException | java.security.NoSuchAlgorithmException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
    private M3PatternAsciiFrontierTest() { }
    private static native int scalarOracle(long[] masks, long stars, int atoms, String input);

    private static void check(boolean condition, String message) {
        ASSERTIONS.incrementAndGet();
        if (!condition) throw new AssertionError(message);
    }
    private static Object derive(Pattern pattern) throws Exception {
        return DERIVE.invoke(null, ROOT.get(pattern));
    }
    private static int match(Object plan, String input) throws Exception {
        try { return (Integer) MATCHES.invoke(plan, input); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException cause) throw cause;
            throw failure;
        }
    }
    private record Prepared(Pattern pattern, Object plan, long[] masks, long stars, int atoms) { }
    private static Prepared prepare(String expression, int flags) throws Exception {
        Pattern pattern = Pattern.compile(expression, flags);
        Object plan = derive(pattern);
        if (plan == null) return null;
        plans++;
        Object[] atoms = (Object[]) ATOMS.get(plan);
        boolean[] repeated = (boolean[]) REPEATED.get(plan);
        boolean[] empty = (boolean[]) EMPTY.get(plan);
        if (atoms.length > 63) {
            if (ASCII != null) check(ASCII.get(plan) == null, "PROGRAM_LIMIT: " + atoms.length);
            return new Prepared(pattern, plan, null, 0, atoms.length);
        }
        long stars = 0;
        long[] masks = new long[136];
        for (int atom = 0; atom < atoms.length; atom++) {
            if (repeated[atom]) stars |= 1L << (atom + 1);
            for (int unit = 0; unit < 128; unit++) {
                if ((Boolean) PREDICATE.invoke(atoms[atom], unit)) masks[unit] |= 1L << (atom + 1);
            }
        }
        // Independent per-destination contiguous-run enumeration, not production doubling.
        for (int level = 0; level < 6; level++) {
            int width = 1 << level;
            for (int end = width; end <= atoms.length; end++) {
                boolean all = true;
                for (int j = end - width; j < end; j++) all &= repeated[j];
                if (all) masks[128 + level] |= 1L << end;
            }
        }
        for (int state = 0; state < empty.length; state++) if (empty[state]) masks[134] |= 1L << state;
        masks[135] = 1L << atoms.length;
        if (requireAscii) check(ASCII != null && ASCII.get(plan) != null, "MISSING_ASCII_METADATA");
        if (ASCII != null && ASCII.get(plan) != null) {
            check(Arrays.equals(masks, (long[]) ASCII.get(plan)), "PRECOMPUTED_MASK_MISMATCH");
        }
        return new Prepared(pattern, plan, masks, stars, atoms.length);
    }
    private static void verify(Prepared prepared, String input) throws Exception {
        if (prepared == null) return;
        int actual = match(prepared.plan, input);
        boolean surrogate = false, ascii = true;
        for (int i = 0; i < input.length(); i++) {
            surrogate |= Character.isSurrogate(input.charAt(i));
            ascii &= input.charAt(i) < 128;
        }
        int expected = surrogate ? -1 : prepared.pattern.matcher(input).matches() ? 1 : 0;
        check(actual == expected, "SEMANTIC_DIVERGENCE pattern=" + prepared.pattern
                + " input=" + escape(input) + " expected=" + expected + " actual=" + actual);
        if (prepared.masks != null) {
            long[] before = prepared.masks.clone();
            int nativeValue = scalarOracle(prepared.masks, prepared.stars, prepared.atoms, input);
            nativeCases++;
            check(nativeValue == (ascii ? expected : -1), "JNI_ORACLE_DIVERGENCE");
            check(Arrays.equals(before, prepared.masks), "JNI_MUTATED_METADATA");
        }
        cases++;
        DIGEST.update((byte) actual);
    }
    private static String escape(String input) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < input.length(); i++) b.append(String.format("%04x", (int) input.charAt(i)));
        return b.toString();
    }
    private static List<String> words(String[] alphabet, int maxLength) {
        List<String> all = new ArrayList<>(); all.add("");
        List<String> level = List.of("");
        for (int n = 1; n <= maxLength; n++) {
            List<String> next = new ArrayList<>();
            for (String prefix : level) for (String unit : alphabet) next.add(prefix + unit);
            all.addAll(next); level = next;
        }
        return all;
    }
    private static void corpus() throws Exception {
        List<String> inputs = words(new String[] {"a", "b", "*", "\n", "\u007f"}, 3);
        for (String expression : words(new String[] {"a", "a*", "b", "b*", ".", ".*"}, 4)) {
            Prepared plan = prepare(expression, 0);
            for (String input : inputs) verify(plan, input);
        }
        String[] patterns = {"a*", ".*", "[a-z]*b", "[^a]*", "\\d*a*", "\\w*", "\\s*",
                             "a*b*", "a*bc", "\\p{Lower}*", "[a-z&&[^b]]*", "a*\\Q.*\\E"};
        int[] flags = {0, Pattern.DOTALL, Pattern.UNIX_LINES, Pattern.CASE_INSENSITIVE,
                       Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE, Pattern.UNICODE_CHARACTER_CLASS};
        for (String expression : patterns) for (int flag : flags) {
            Prepared plan = prepare(expression, flag);
            for (int unit = 0; unit < 128; unit++) {
                String value = Character.toString((char) unit);
                verify(plan, value); verify(plan, "a" + value + "b");
            }
            for (String value : List.of("", "*", "\u0085", "\u00e9", "\u0100", "\u2028", "\u2029",
                    "\ud800", "\udc00", "\ud83d\ude00", "z\ud800", "aaa\ud800", "a\u0085b")) verify(plan, value);
        }
    }
    private static void allBmpAndMixedPrefixes() throws Exception {
        for (int flags : new int[] {0, Pattern.DOTALL, Pattern.UNIX_LINES}) {
            Prepared plan = prepare(".*", flags);
            for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
                verify(plan, Character.toString((char) unit));
            }
            Prepared boundary = prepare("b" + ".*".repeat(62), flags);
            for (String prefix : words(new String[] {"a", "b"}, 4)) {
                for (String middle : List.of("\u00e9", "\u0085", "\u0100", "\u2028", "\u2029", "\uffff", "\ud800")) {
                    verify(boundary, "b" + prefix + middle + "ab");
                }
            }
        }
    }

    private static void boundaries() throws Exception {
        for (int atoms : new int[] {1, 2, 31, 32, 33, 62, 63, 64, 65, 255, 256}) {
            Prepared nullable = prepare("a*".repeat(atoms), 0);
            check(nullable != null, "EXPECTED_DP_PLAN");
            for (String input : List.of("", "a", "aa", "b", "z\ud800", "\u0085")) verify(nullable, input);
            if (atoms > 1) {
                Prepared tail = prepare("b" + "a*".repeat(atoms - 1), 0);
                for (String input : List.of("b", "ba", "baaa", "a", "", "b\udc00")) verify(tail, input);
            }
        }
        check(prepare("a*".repeat(257), 0) == null, "ORIGINAL_256_ATOM_BUDGET");
        for (String unsupported : List.of("a", "a+", "a*?", "a*+", "(a*)", "a|b", "(?=a)a*", "(a)\\1", "a*$", "^a*")) {
            check(prepare(unsupported, 0) == null, "ADMISSION_WIDENED: " + unsupported);
        }
        Prepared p = prepare(".*", 0);
        try { match(p.plan, null); throw new AssertionError("NULL_ACCEPTED"); }
        catch (NullPointerException expected) { check(true, "null contract"); }
        try { scalarOracle(new long[1], 0, 1, "a"); throw new AssertionError("BAD_SHAPE_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "native shape"); }
        try { scalarOracle(p.masks, 0, 64, "a"); throw new AssertionError("BAD_COUNT_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "native count"); }
        try { scalarOracle(p.masks, 1, 1, "a"); throw new AssertionError("BAD_STARS_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "native stars"); }
        verify(prepare("a*b*", 0), "a".repeat(100_000) + "b");
        verify(prepare("a*b*", 0), "a".repeat(100_000) + "c");
    }
    private static void concurrency() throws Exception {
        Prepared p = prepare("a*b*c*", 0);
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Void>> jobs = new ArrayList<>();
            for (int job = 0; job < 64; job++) jobs.add(() -> {
                for (int i = 0; i < 256; i++) {
                    String input = (i & 1) == 0 ? "aaaabbbc" : "aaaabbbcz";
                    check(match(p.plan, input) == ((i & 1) == 0 ? 1 : 0), "CONCURRENT_PLAN");
                    check(scalarOracle(p.masks, p.stars, p.atoms, input) == ((i & 1) == 0 ? 1 : 0), "CONCURRENT_JNI");
                }
                return null;
            });
            for (var future : executor.invokeAll(jobs)) future.get();
        }
    }
    public static void main(String[] args) throws Exception {
        System.loadLibrary("M3PatternAsciiFrontierTest");
        requireAscii = args.length > 0 && args[0].equals("require-ascii");
        if (args.length > 0 && args[0].equals("mutant-case")) {
            Prepared p = prepare("b" + "a*".repeat(62), 0);
            int value = match(p.plan, "b");
            check(value == 1, "EPSILON_32_MISSING:b+(a*)62:b");
            return;
        }
        corpus(); allBmpAndMixedPrefixes(); boundaries(); concurrency();
        System.out.println("M3_ASCII_PROOF cases=" + cases + " nativeCases=" + nativeCases
                + " plans=" + plans + " assertions=" + ASSERTIONS.get()
                + " digest=" + HexFormat.of().formatHex(DIGEST.digest())
                + " implementation=" + (ASCII == null ? "ROLLING_BASELINE" : "ASCII_CANDIDATE"));
    }
}
