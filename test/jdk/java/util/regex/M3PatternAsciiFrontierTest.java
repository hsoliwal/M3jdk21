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
 * @run main/native M3PatternAsciiFrontierTest require-bmp
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
    private static native int wideScalarOracle(long[] masks, int atoms, String input);
    private static native int mixedScalarOracle(long[] masks, char[] alphabet, int atoms, String input);

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
            long[] masks = wideMasks(atoms, repeated, empty);
            long[] actual = ASCII == null ? null : (long[]) ASCII.get(plan);
            if (requireWide) check(actual != null, "MISSING_WIDE_METADATA");
            if (actual != null) check(Arrays.equals(masks, actual), "WIDE_MASK_MISMATCH");
            return new Prepared(pattern, plan, masks, 0, atoms.length);
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
            int nativeValue = nativeMatch(prepared, input);
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
    private static int nativeMatch(Prepared p, String input) {
        return p.atoms <= 63 ? scalarOracle(p.masks, p.stars, p.atoms, input)
                : wideScalarOracle(p.masks, p.atoms, input);
    }

    /** Independent per-bit enumeration of word-local epsilon runs, not production doubling. */
    private static long[] wideMasks(Object[] atoms, boolean[] repeated, boolean[] empty) throws Exception {
        int words = (atoms.length >>> 6) + 1;
        long[] masks = new long[137 * words];
        for (int atom = 0; atom < atoms.length; atom++) {
            int state = atom + 1, word = state >>> 6;
            long bit = 1L << (state & 63);
            if (repeated[atom]) masks[136 * words + word] |= bit;
            for (int unit = 0; unit < 128; unit++) {
                if ((Boolean) PREDICATE.invoke(atoms[atom], unit)) masks[unit * words + word] |= bit;
            }
        }
        for (int word = 0; word < words; word++) for (int level = 0; level < 6; level++) {
            int width = 1 << level;
            for (int bit = width; bit < 64 && 64 * word + bit <= atoms.length; bit++) {
                boolean all = true;
                for (int j = 64 * word + bit - width; j < 64 * word + bit; j++) all &= repeated[j];
                if (all) masks[(128 + level) * words + word] |= 1L << bit;
            }
        }
        for (int state = 0; state < empty.length; state++) {
            if (empty[state]) masks[134 * words + (state >>> 6)] |= 1L << (state & 63);
        }
        masks[135 * words + (atoms.length >>> 6)] = 1L << (atoms.length & 63);
        return masks;
    }

    /** Complete scalar recurrence, independent of word transitions and carry propagation. */
    private static int wideJavaOracle(Prepared p, String input) throws Exception {
        Object[] atoms = (Object[]) ATOMS.get(p.plan);
        boolean[] repeated = (boolean[]) REPEATED.get(p.plan);
        boolean[] row = ((boolean[]) EMPTY.get(p.plan)).clone();
        int words = (p.atoms >>> 6) + 1;
        for (int i = 0; i < input.length(); i++) {
            char unit = input.charAt(i);
            if (Character.isSurrogate(unit)) return -1;
            boolean diagonal = row[0]; row[0] = false;
            for (int j = 1; j <= p.atoms; j++) {
                boolean above = row[j];
                boolean accepts = unit < 128
                        ? (p.masks[unit * words + (j >>> 6)] & (1L << (j & 63))) != 0
                        : (Boolean) PREDICATE.invoke(atoms[j - 1], (int) unit);
                row[j] = repeated[j - 1] ? row[j - 1] || (above && accepts) : diagonal && accepts;
                diagonal = above;
            }
        }
        return row[p.atoms] ? 1 : 0;
    }

    private static void wideCorpus() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int tested = 0;
        for (int count = 64; count <= 256; count++) for (int family = 0; family < 4; family++) {
            String expression = switch (family) {
                case 0 -> "a*".repeat(count);
                case 1 -> "b" + "a*".repeat(count - 1);
                case 2 -> "a".repeat(count - 1) + "b*";
                default -> "[ab]*".repeat(count - 1) + ".";
            };
            Prepared p = prepare(expression, family == 3 ? Pattern.DOTALL : 0);
            check(p != null && p.atoms == count, "WIDE_COMPILED_GEOMETRY");
            long[] saved = p.masks.clone();
            for (String input : List.of("", "a", "b", "ab", "a".repeat(count - 1),
                    "a".repeat(count), "a".repeat(63) + "b", "ab".repeat(65),
                    "a".repeat(65) + "\u00e9", "b\u0085ab", "z\ud800", "b" + "a".repeat(193) + "\udc00")) {
                int expected = wideJavaOracle(p, input);
                check(match(p.plan, input) == expected, "WIDE_PARITY:" + count + ":" + family);
                boolean ascii = input.chars().allMatch(c -> c < 128);
                check(nativeMatch(p, input) == (ascii ? expected : -1), "WIDE_JNI_PARITY");
                digest.update((byte) expected); tested++;
            }
            check(Arrays.equals(saved, p.masks), "WIDE_JNI_IMMUTABILITY");
        }
        Prepared p = prepare("b" + "a*".repeat(255), 0);
        long[] corrupt = p.masks.clone(); corrupt[136 * 5 + 4] |= 2L;
        try { wideScalarOracle(corrupt, 256, "b"); throw new AssertionError("WIDE_TAIL_STARS_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "wide tail stars"); }
        corrupt = p.masks.clone(); corrupt[127 * 5 + 4] |= 2L;
        try { wideScalarOracle(corrupt, 256, "b"); throw new AssertionError("WIDE_UNREAD_PREDICATE_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "validate every predicate plane"); }
        corrupt = p.masks.clone(); corrupt[0] |= 1L;
        try { wideScalarOracle(corrupt, 256, "b"); throw new AssertionError("WIDE_STATE_ZERO_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "state zero"); }
        try { wideScalarOracle(new long[1], 256, "b"); throw new AssertionError("WIDE_SHAPE_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "wide shape"); }
        try { wideScalarOracle(p.masks, 257, "b"); throw new AssertionError("WIDE_COUNT_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(true, "wide count"); }
        try { wideScalarOracle(null, 256, "b"); throw new AssertionError("WIDE_NULL_ACCEPTED"); }
        catch (NullPointerException expected) { check(true, "wide null"); }
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Void>> jobs = new ArrayList<>();
            for (int n = 0; n < 64; n++) jobs.add(() -> {
                check(match(p.plan, "baaa") == 1, "WIDE_CONCURRENT_JAVA");
                check(nativeMatch(p, "baaa") == 1, "WIDE_CONCURRENT_JNI");
                return null;
            });
            for (var future : executor.invokeAll(jobs)) future.get();
        }
        System.out.println("M3_WIDE_PROOF cases=" + tested + " atomCounts=193 concurrentTasks=64 digest="
                + HexFormat.of().formatHex(digest.digest()));
    }


    /** An independent alphabet export; no production cache/closure state is read. */
    private static long[] mixedImage(Prepared p, char[] keys) throws Exception {
        Object[] atoms = (Object[]) ATOMS.get(p.plan);
        boolean[] repeated = (boolean[]) REPEATED.get(p.plan);
        int words = (p.atoms >>> 6) + 1;
        long[] image = new long[(keys.length + 1) * words];
        for (int atom = 0; atom < atoms.length; atom++) {
            int state = atom + 1, word = state >>> 6;
            long bit = 1L << (state & 63);
            for (int symbol = 0; symbol < keys.length; symbol++) {
                if ((Boolean) PREDICATE.invoke(atoms[atom], (int) keys[symbol])) {
                    image[symbol * words + word] |= bit;
                }
            }
            if (repeated[atom]) image[keys.length * words + word] |= bit;
        }
        return image;
    }

    private static void mixedCase(Prepared p, String text, long[] image, char[] keys,
                                  MessageDigest digest) throws Exception {
        int expected = wideJavaOracle(p, text);
        check(match(p.plan, text) == expected, "BMP_FRONTIER_DIVERGENCE:" + p.atoms + ":" + escape(text));
        check(mixedScalarOracle(image, keys, p.atoms, text) == expected, "BMP_NATIVE_DIVERGENCE");
        digest.update((byte) expected);
    }

    private static void bmpCorpus() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int tested = 0;
        // All code units, not an ASCII-only corpus. Oracle alphabet excludes surrogates;
        // the input-side refusal still has to occur even when its frontier has died.
        for (int flags : new int[] {0, Pattern.DOTALL, Pattern.UNIX_LINES}) {
            Prepared p = prepare(".*", flags);
            for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
                char c = (char) unit;
                char[] keys = Character.isSurrogate(c) ? new char[0] : new char[] {c};
                long[] image = mixedImage(p, keys);
                String text = Character.toString(c);
                int expected = Character.isSurrogate(c) ? -1 : p.pattern.matcher(text).matches() ? 1 : 0;
                check(match(p.plan, text) == expected, "BMP_COMPLETE_JAVA");
                check(mixedScalarOracle(image, keys, p.atoms, text) == expected, "BMP_COMPLETE_JNI");
                digest.update((byte) expected); tested++;
            }
        }
        char[] keys = {0, 'a', 'b', 'z', '\u0085', '\u00e9', '\u03b2', '\u0966', '\u2028', '\uffff'};
        List<String> inputs = new ArrayList<>(words(new String[] {"a", "\u00e9", "\u03b2"}, 3));
        inputs.addAll(List.of("\u03b2a\u00e9", "\u00e9a\u03b2", "\u00e9a\u00e9", "z\ud800",
                "\u00e9\udc00", "\u0085", "\u0966", "\u2028", "\uffff", "\u0000",
                "\u00e9\u03b2\u0966", "\u00e9\u03b2\u0966a", "\u00e9\u03b2\u0966\ud800"));
        // Every atom count, with required ordering and changing non-ASCII acceptance sets.
        for (int count = 1; count <= 256; count++) for (int family = 0; family < 2; family++) {
            String expression = family == 0 ? "[a\u00e9\u03b2]*".repeat(count)
                    : count == 1 ? "\u00e9*" : "\u00e9*" + "a*".repeat(count - 2) + "\u03b2*";
            Prepared p = prepare(expression, 0);
            check(p != null && p.atoms == count, "BMP_COMPILED_GEOMETRY");
            long[] image = mixedImage(p, keys), saved = image.clone();
            char[] savedKeys = keys.clone();
            for (String input : inputs) { mixedCase(p, input, image, keys, digest); tested++; }
            check(Arrays.equals(saved, image) && Arrays.equals(savedKeys, keys), "BMP_NATIVE_MUTATED_IMAGE");
        }
        for (String expression : List.of("\\w*", "\\d*", "[a-z]*", "\\p{Lower}*", "[^a]*")) {
            for (int flags : new int[] {0, Pattern.UNICODE_CHARACTER_CLASS,
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE}) {
                Prepared p = prepare(expression, flags);
                check(p != null, "BMP_FLAG_PLAN");
                long[] image = mixedImage(p, keys);
                for (String input : inputs) {
                    mixedCase(p, input, image, keys, digest);
                    if (input.indexOf('\ud800') < 0 && input.indexOf('\udc00') < 0) {
                        check(match(p.plan, input) == (p.pattern.matcher(input).matches() ? 1 : 0), "BMP_JDK_FLAGS");
                    }
                    tested++;
                }
            }
        }
        for (int count : new int[] {1, 63, 64, 128, 256}) {
            Prepared p = prepare(".*".repeat(count), Pattern.DOTALL);
            long[] image = mixedImage(p, keys);
            for (String text : List.of("\u00e9\u03b2\u0966a", "\u03b2\u00e9\u0966",
                    "\u00e9\u03b2\u0966\ud800", "\u00e9a\u03b2a\u00e9a\u03b2")) {
                mixedCase(p, text, image, keys, digest); tested++;
            }
        }
        // Invocation-local cache state cannot leak across threads or plan instances.
        Prepared shared = prepare("\u00e9*" + "a*".repeat(254) + "\u03b2*", 0);
        long[] image = mixedImage(shared, keys);
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Void>> jobs = new ArrayList<>();
            for (int job = 0; job < 64; job++) jobs.add(() -> {
                for (int i = 0; i < 64; i++) {
                    String text = (i & 1) == 0 ? "\u00e9a\u03b2" : "\u03b2a\u00e9";
                    int expected = (i & 1) == 0 ? 1 : 0;
                    check(match(shared.plan, text) == expected, "BMP_CONCURRENT_JAVA");
                    check(mixedScalarOracle(image, keys, 256, text) == expected, "BMP_CONCURRENT_JNI");
                }
                return null;
            });
            for (var future : executor.invokeAll(jobs)) future.get();
        }
        if (ASCII != null) {
            Prepared fallback = prepare("\u00e9*\u03b2*", 0);
            ASCII.set(fallback.plan, null); // Isolated test plan: force optional-metadata failure path.
            check(match(fallback.plan, "\u00e9\u03b2") == 1, "BMP_OPTIONAL_DP");
            check(match(fallback.plan, "\u03b2\u00e9") == 0, "BMP_OPTIONAL_DP_ORDER");
            check(match(fallback.plan, "z\ud800") == -1, "BMP_OPTIONAL_DP_SURROGATE");
        }
        mixedRefusals();
        System.out.println("M3_BMP_PROOF cases=" + tested + " atomCounts=256 concurrentTasks=64 digest="
                + HexFormat.of().formatHex(digest.digest()));
    }

    private static void mixedReject(long[] image, char[] alphabet, int atoms, String text, String message) {
        try { mixedScalarOracle(image, alphabet, atoms, text); throw new AssertionError("BMP_INVALID_ACCEPTED"); }
        catch (IllegalArgumentException expected) { check(expected.getMessage().contains(message), "BMP_WRONG_REFUSAL"); }
    }

    private static void mixedRefusals() throws Exception {
        Prepared p = prepare("\u00e9*", 0);
        char[] keys = {'\u00e9'};
        long[] image = mixedImage(p, keys);
        mixedReject(image, keys, 257, "", "count");
        mixedReject(image, keys, -1, "", "count");
        mixedReject(image, new char[33], 1, "", "count");
        mixedReject(new long[1], keys, 1, "", "shape");
        mixedReject(new long[3], new char[] {'b', 'a'}, 1, "", "alphabet");
        mixedReject(new long[3], new char[] {'a', 'a'}, 1, "", "alphabet");
        mixedReject(image, new char[] {'\ud800'}, 1, "", "alphabet");
        mixedReject(image, keys, 1, "b", "missing symbol");
        long[] bad = image.clone(); bad[0] |= 1L;
        mixedReject(bad, keys, 1, "", "foreign bits");
        bad = image.clone(); bad[1] |= 4L;
        mixedReject(bad, keys, 1, "\ud800", "foreign bits");
        Prepared wide = prepare("a*".repeat(256), 0);
        bad = mixedImage(wide, new char[] {'a', 'z'}); bad[5 + 4] |= 2L;
        mixedReject(bad, new char[] {'a', 'z'}, 256, "a", "foreign bits");
        for (int kind = 0; kind < 3; kind++) {
            try {
                mixedScalarOracle(kind == 0 ? null : image, kind == 1 ? null : keys, 1, kind == 2 ? null : "");
                throw new AssertionError("BMP_NULL_ACCEPTED");
            } catch (NullPointerException expected) { check(true, "native null"); }
        }
        check(mixedScalarOracle(new long[1], new char[0], 0, "") == 1, "BMP_NATIVE_EMPTY_IDENTITY");
    }

    public static void main(String[] args) throws Exception {
        System.loadLibrary("M3PatternAsciiFrontierTest");
        boolean requireBmp = args.length > 0 && args[0].equals("require-bmp");
        requireWide = requireBmp || (args.length > 0 && args[0].equals("require-wide"));
        if (requireBmp) {
            Class.forName("java.util.regex.M3PatternQuery$BooleanPlan")
                    .getDeclaredMethod("predicateBits", char.class, int.class);
        }
        requireAscii = requireWide || (args.length > 0 && args[0].equals("require-ascii"));
        if (args.length > 0 && args[0].equals("mutant-case")) {
            Prepared p = prepare("b" + "a*".repeat(62), 0);
            int value = match(p.plan, "b");
            check(value == 1, "EPSILON_32_MISSING:b+(a*)62:b");
            return;
        }
        if (args.length > 0 && args[0].equals("consume-carry-mutant")) {
            Prepared p = prepare("a".repeat(64) + "b*", 0);
            check(match(p.plan, "a".repeat(64)) == 1, "CONSUME_CARRY_64");
            return;
        }
        if (args.length > 0 && args[0].equals("epsilon-carry-mutant")) {
            Prepared p = prepare("b" + "a*".repeat(63), 0);
            check(match(p.plan, "b") == 1, "EPSILON_CARRY_64");
            return;
        }
        if (args.length > 0 && args[0].equals("bmp-cache-mutant")) {
            Prepared p = prepare("\u00e9*\u03b2*", 0);
            check(match(p.plan, "\u03b2\u00e9") == 0, "BMP_CACHE_KEY");
            return;
        }
        if (args.length > 0 && args[0].equals("bmp-replace-mutant")) {
            Prepared p = prepare("\u00e9*" + "a*".repeat(254) + "\u03b2*", 0);
            check(match(p.plan, "\u03b2\u00e9") == 0, "BMP_MASK_REPLACE");
            return;
        }
        corpus(); allBmpAndMixedPrefixes(); boundaries(); concurrency();
        wideCorpus(); bmpCorpus();
        System.out.println("M3_ASCII_PROOF cases=" + cases + " nativeCases=" + nativeCases
                + " plans=" + plans + " assertions=" + ASSERTIONS.get()
                + " digest=" + HexFormat.of().formatHex(DIGEST.digest())
                + " implementation=" + (ASCII == null ? "ROLLING_BASELINE" : "ASCII_CANDIDATE"));
    }
}
