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
 * @run main/native M3PatternAsciiFrontierTest require-wide
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
    private static boolean requireWide;
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
    private static native int wideOracle(long[] masks, boolean[] repeats, int atoms, String input);

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
    private record Prepared(Pattern pattern, Object plan, long[] masks, long stars,
                            int atoms, boolean[] repeats) { }
    private static Prepared prepare(String expression, int flags) throws Exception {
        Pattern pattern = Pattern.compile(expression, flags);
        Object plan = derive(pattern);
        if (plan == null) return null;
        plans++;
        Object[] atoms = (Object[]) ATOMS.get(plan);
        boolean[] repeated = (boolean[]) REPEATED.get(plan);
        boolean[] empty = (boolean[]) EMPTY.get(plan);
        int words = (atoms.length >>> 6) + 1;
        long stars = 0;
        long[] masks = new long[136 * words];
        for (int atom = 0; atom < atoms.length; atom++) {
            int state = atom + 1;
            if (repeated[atom] && state < 64) stars |= 1L << state;
            for (int unit = 0; unit < 128; unit++) {
                if ((Boolean) PREDICATE.invoke(atoms[atom], unit)) {
                    masks[(state >>> 6) * 136 + unit] |= 1L << (state & 63);
                }
            }
        }
        // Independent contiguous-run enumeration, not the production doubling recurrence.
        for (int word = 0; word < words; word++) for (int level = 0; level < 6; level++) {
            int width = 1 << level;
            for (int end = Math.max(1, word * 64); end <= Math.min(atoms.length, word * 64 + 63); end++) {
                if (level != 0 && (end & 63) < width - 1) continue;
                if (end < width) continue;
                boolean all = true;
                for (int j = end - width; j < end; j++) all &= repeated[j];
                if (all) masks[word * 136 + 128 + level] |= 1L << (end & 63);
            }
        }
        for (int state = 0; state < empty.length; state++) {
            if (empty[state]) masks[(state >>> 6) * 136 + 134] |= 1L << (state & 63);
        }
        masks[(atoms.length >>> 6) * 136 + 135] = 1L << (atoms.length & 63);
        if (requireAscii && (atoms.length <= 63 || requireWide)) {
            check(ASCII != null && ASCII.get(plan) != null, "MISSING_ASCII_METADATA");
        }
        if (ASCII != null && ASCII.get(plan) != null) {
            check(Arrays.equals(masks, (long[]) ASCII.get(plan)), "PRECOMPUTED_MASK_MISMATCH:" + atoms.length);
        }
        return new Prepared(pattern, plan, masks, stars, atoms.length, repeated.clone());
    }

    private static void verify(Prepared prepared, String input) throws Exception {
        verify(prepared, input, null);
    }
    private static void verify(Prepared prepared, String input, Integer unaryExpected) throws Exception {
        if (prepared == null) return;
        int actual = match(prepared.plan, input);
        boolean surrogate = false, ascii = true;
        for (int i = 0; i < input.length(); i++) {
            surrogate |= Character.isSurrogate(input.charAt(i));
            ascii &= input.charAt(i) < 128;
        }
        int expected = surrogate ? -1 : unaryExpected != null ? unaryExpected
                : prepared.pattern.matcher(input).matches() ? 1 : 0;
        if (actual != expected) {
            check(false, "SEMANTIC_DIVERGENCE pattern=" + prepared.pattern
                    + " input=" + escape(input) + " expected=" + expected + " actual=" + actual);
        } else {
            check(true, "exact semantic parity");
        }
        if (prepared.masks != null) {
            long[] before = prepared.masks.clone();
            boolean[] repeatsBefore = prepared.repeats.clone();
            int nativeValue = prepared.atoms > 63
                    ? wideOracle(prepared.masks, prepared.repeats, prepared.atoms, input)
                    : scalarOracle(prepared.masks, prepared.stars, prepared.atoms, input);
            check(Arrays.equals(repeatsBefore, prepared.repeats), "JNI_MUTATED_REPEATS");
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
    private static void wideBoundaries() throws Exception {
        for (int atoms = 64; atoms <= 256; atoms++) {
            Prepared allStars = prepare("a*".repeat(atoms), 0);
            Prepared closure = prepare("b" + "a*".repeat(atoms - 1), 0);
            Prepared consume = prepare("a".repeat(atoms - 1) + "b*", 0);
            for (String input : List.of("", "a", "aa", "b", "z\ud800", "\u00e9")) verify(allStars, input);
            for (String input : List.of("b", "ba", "b\ud800", "", "a")) verify(closure, input);
            String prefix = "a".repeat(atoms - 1);
            for (String input : List.of(prefix, prefix + "b", prefix + "c", prefix.substring(1),
                    prefix + "\u00e9", "z\ud800")) verify(consume, input);
        }
        for (int atoms : new int[] {64, 65, 127, 128, 129, 191, 192, 193, 255, 256}) {
            for (int flags : new int[] {0, Pattern.DOTALL, Pattern.UNIX_LINES}) {
                Prepared mixed = prepare("b" + ".*".repeat(atoms - 1), flags);
                for (String input : List.of("b\u00e9a", "ba\u2028", "ba\u0085b", "z\ud800", "b\ud83d\ude00")) {
                    verify(mixed, input);
                }
            }
            for (int cohort = 0; cohort < 8; cohort++) {
                StringBuilder expression = new StringBuilder();
                int required = 0;
                for (int atom = 0; atom < atoms; atom++) {
                    expression.append('a');
                    if (((atom + cohort) & 7) < 4) expression.append('*');
                    else required++;
                }
                Prepared mixed = prepare(expression.toString(), 0);
                // This generated chain contains only a/a* and at least one star. Its exact
                // language is a^{n >= required}. Do not invoke an exponentially searching
                // oracle for this family; predecessor/candidate and scalar JNI still run.
                for (String input : List.of("a".repeat(required), "a".repeat(atoms), "c", "z\ud800")) {
                    int expected = input.length() >= required && input.chars().allMatch(c -> c == 'a') ? 1 : 0;
                    verify(mixed, input, expected);
                }
            }
        }
        Prepared p = prepare(".*".repeat(256), 0);
        try { wideOracle(p.masks, p.repeats, 257, ""); throw new AssertionError("WIDE_LIMIT_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "wide limit"); }
        try { wideOracle(new long[1], p.repeats, 256, ""); throw new AssertionError("WIDE_SHAPE_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "wide shape"); }
        try { wideOracle(p.masks, new boolean[1], 256, ""); throw new AssertionError("WIDE_REPEAT_SHAPE_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "wide repeats"); }
        long[] corrupt = p.masks.clone(); corrupt[0] |= 1;
        try { wideOracle(corrupt, p.repeats, 256, ""); throw new AssertionError("WIDE_PREDICATE_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "wide predicates"); }
        try { wideOracle(null, p.repeats, 256, ""); throw new AssertionError("WIDE_NULL_ACCEPTED"); }
        catch (NullPointerException expected) { check(true, "wide null"); }
    }

    private static void concurrency() throws Exception {
        Prepared p = prepare("a*b*c*", 0);
        Prepared wide = prepare("a*b*c*" + "c*".repeat(253), 0);
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Void>> jobs = new ArrayList<>();
            for (int job = 0; job < 64; job++) jobs.add(() -> {
                for (int i = 0; i < 256; i++) {
                    String input = (i & 1) == 0 ? "aaaabbbc" : "aaaabbbcz";
                    check(match(p.plan, input) == ((i & 1) == 0 ? 1 : 0), "CONCURRENT_PLAN");
                    check(scalarOracle(p.masks, p.stars, p.atoms, input) == ((i & 1) == 0 ? 1 : 0), "CONCURRENT_JNI");
                    check(match(wide.plan, input) == ((i & 1) == 0 ? 1 : 0), "CONCURRENT_WIDE");
                    check(wideOracle(wide.masks, wide.repeats, wide.atoms, input) == ((i & 1) == 0 ? 1 : 0), "CONCURRENT_WIDE_JNI");
                }
                return null;
            });
            for (var future : executor.invokeAll(jobs)) future.get();
        }
    }
    public static void main(String[] args) throws Exception {
        System.loadLibrary("M3PatternAsciiFrontierTest");
        requireWide = args.length > 0 && args[0].equals("require-wide");
        requireAscii = requireWide || args.length > 0 && args[0].equals("require-ascii");
        if (args.length > 0 && args[0].equals("jdk-unary-timeout")) {
            Pattern pattern = Pattern.compile("a*a*a*a*aaaa".repeat(8));
            System.out.println("JDK_UNARY_ORACLE pattern=(a*a*a*a*aaaa)^8 input=a^32");
            System.out.flush();
            check(pattern.matcher("a".repeat(32)).matches(), "JDK_UNARY_ORACLE_RESULT");
            return;
        }
        if (args.length > 0 && args[0].equals("mutant-consume")) {
            Prepared p = prepare("a".repeat(64) + "b*", 0);
            check(match(p.plan, "a".repeat(64)) == 1, "CONSUME_CARRY_64_MISSING");
            return;
        }
        if (args.length > 0 && args[0].equals("mutant-epsilon")) {
            Prepared p = prepare("b" + "a*".repeat(255), 0);
            check(match(p.plan, "b") == 1, "EPSILON_CARRY_64_MISSING");
            return;
        }
        if (args.length > 0 && args[0].equals("mutant-case")) {
            Prepared p = prepare("b" + "a*".repeat(62), 0);
            int value = match(p.plan, "b");
            check(value == 1, "EPSILON_32_MISSING:b+(a*)62:b");
            return;
        }
        corpus(); allBmpAndMixedPrefixes(); boundaries(); wideBoundaries(); concurrency();
        System.out.println("M3_ASCII_PROOF cases=" + cases + " nativeCases=" + nativeCases
                + " plans=" + plans + " assertions=" + ASSERTIONS.get()
                + " digest=" + HexFormat.of().formatHex(DIGEST.digest())
                 + " implementation=" + (requireWide ? "WIDE_CANDIDATE" : "PREDECESSOR"));
    }
}
