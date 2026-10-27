/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * Modified 2026-10-27 by Hitesh Soliwal and contributors:
 * verify eager immutable scalar and wide ASCII masks and M3 String storage behavior.
 */
/*
 * @test
 * @summary Scalar and wide regex masks preserve compiled JDK predicate and UTF-16 semantics
 * @run main/othervm --add-opens java.base/java.util.regex=ALL-UNNAMED M3PatternStateMaskTest candidate
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage --add-opens java.base/java.util.regex=ALL-UNNAMED M3PatternStateMaskTest candidate
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class M3PatternStateMaskTest {
    private static long observations;
    private static final MessageDigest DIGEST;
    static {
        try { DIGEST = MessageDigest.getInstance("SHA-256"); }
        catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
    private static Object read(Object value, String name) throws Exception {
        Field field = value.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(value);
    }
    // The current owner eagerly prepares final ASCII metadata; the superseded lazy
    // WordPlan lifecycle is not the contract of this scalar/wide implementation.
    private static long states(int word, int first, int last, int localMinimum) {
        long mask = 0L;
        for (int bit = localMinimum; bit < Long.SIZE; bit++) {
            int state = word * Long.SIZE + bit;
            if (state >= first && state <= last) mask |= 1L << bit;
        }
        return mask;
    }
    private static long[] masks(Object plan, int count, boolean allRepeated) throws Exception {
        Field field = plan.getClass().getDeclaredField("ascii");
        check(Modifier.isFinal(field.getModifiers()), "final ASCII metadata " + count);
        long[] actual = (long[]) read(plan, "ascii");
        check(actual != null, "eager ASCII metadata " + count);
        int words = count <= 63 ? 1 : (count >>> 6) + 1;
        check(actual.length == (count <= 63 ? 136 : 137 * words), "mask layout " + count);
        int firstRepeat = allRepeated ? 1 : count;
        long scalarStars = count <= 63 ? states(0, firstRepeat, count, 0) : 0L;
        check((long) read(plan, "repeatedStates") == scalarStars, "scalar stars " + count);
        for (int word = 0; word < words; word++) {
            for (int unit = 0; unit < 128; unit++) {
                long expected = allRepeated
                        ? (unit == 'a' ? states(word, 1, count, 0) : 0L)
                        : (unit == 'a' ? states(word, 1, count - 1, 0)
                           : unit == 'b' ? states(word, count, count, 0) : 0L);
                check(actual[unit * words + word] == expected,
                        "character plane " + count + "/" + word + "/" + unit);
            }
            for (int level = 0; level < 6; level++) {
                int width = 1 << level;
                // Enumerate each allowed destination. Wide planes close only within a
                // word; the matcher handles the separate cross-word epsilon carry.
                long expected = allRepeated ? states(word, width, count, count <= 63 ? 0 : width)
                        : width == 1 ? states(word, count, count, count <= 63 ? 0 : 1) : 0L;
                check(actual[(128 + level) * words + word] == expected,
                        "epsilon plane " + count + "/" + word + "/" + width);
            }
            int lastEmpty = allRepeated || count == 1 ? count : 0;
            check(actual[134 * words + word] == states(word, 0, lastEmpty, 0),
                    "initial frontier " + count + "/" + word);
            check(actual[135 * words + word] == states(word, count, count, 0),
                    "accepting frontier " + count + "/" + word);
            if (count > 63) {
                check(actual[136 * words + word] == states(word, firstRepeat, count, 0),
                        "wide stars " + count + "/" + word);
            }
        }
        return actual;
    }
    private static void pair(Pattern pattern, String text) {
        boolean expected = pattern.matcher(new StringBuilder(text)).matches();
        boolean actual = pattern.asMatchPredicate().test(text);
        check(expected == actual, "predicate " + pattern + " length=" + text.length());
        DIGEST.update((byte) (actual ? 1 : 0));
        observations++;
    }
    private static List<String> words(String[] atoms, int limit) {
        var all = new ArrayList<String>();
        List<String> level = List.of("");
        all.addAll(level);
        for (int i = 0; i < limit; i++) {
            var next = new ArrayList<String>();
            for (String prefix : level) for (String atom : atoms) next.add(prefix + atom);
            all.addAll(next);
            level = next;
        }
        return all;
    }
    public static void main(String[] args) throws Exception {
        boolean candidate = args.length > 0 && args[0].equals("candidate");
        for (String regex : words(new String[]{"a", "b", ".", "a*", "b*", ".*"}, 3)) {
            Pattern pattern = Pattern.compile(regex);
            for (String text : words(new String[]{"a", "b", "\n"}, 4)) pair(pattern, text);
        }
        String[] regexes = {"a*", "[ab]*c*", "[^x]*a*", ".*a*", "\\p{L}*\\d*", "(?i)a*", "é*.*", "[\\x00-\\x7f]*", "[\\u0080-\\uffff]*", "a*bc.*"};
        int[] flags = {0, Pattern.DOTALL, Pattern.UNIX_LINES, Pattern.CASE_INSENSITIVE,
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE, Pattern.UNICODE_CHARACTER_CLASS,
            Pattern.COMMENTS, Pattern.CANON_EQ, Pattern.LITERAL, Pattern.MULTILINE};
        for (String regex : regexes) for (int flag : flags) {
            Pattern pattern = Pattern.compile(regex, flag);
            for (int unit = 0; unit < 128; unit++) {
                String text = String.valueOf((char) unit);
                pair(pattern, text);
                pair(pattern, text + "a");
            }
        }
        for (String regex : List.of(".*", "\\p{L}*", "[^x]*a*")) {
            Pattern pattern = Pattern.compile(regex);
            for (int unit = 0; unit <= Character.MAX_VALUE; unit++) pair(pattern, String.valueOf((char) unit));
        }
        Pattern concurrent = Pattern.compile("a*b*");
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<java.util.concurrent.Callable<Boolean>>();
            for (int i = 0; i < 32; i++) tasks.add(() -> {
                for (int j = 0; j < 100; j++) {
                    if (!concurrent.asMatchPredicate().test("aab") || concurrent.asMatchPredicate().test("ba")) return false;
                }
                return true;
            });
            for (var future : pool.invokeAll(tasks)) check(future.get(), "concurrent immutable plan use");
        }
        String[] samples = {"", "a", "aa", "b", "ab", "aab", "\n", "\r\n", "é", "一", "\ud800", "\udc00", "\ud83d\ude00", "a\ud800", "\u0085", "\u2028"};
        for (int count : new int[]{1, 2, 31, 32, 62, 63, 64, 65, 255, 256, 257}) {
            Pattern repeated = Pattern.compile("a*".repeat(count));
            Object plan = read(repeated, "m3BooleanPlan");
            check((plan != null) == (count <= 256), "DP admission " + count);
            if (candidate && plan != null) {
                long[] prepared = masks(plan, count, true);
                long[] beforeUses = prepared.clone();
                for (int use = 0; use < 40; use++) repeated.asMatchPredicate().test("a");
                check(read(repeated, "m3BooleanPlan") == plan, "same compiled plan " + count);
                check(read(plan, "ascii") == prepared, "same prepared masks " + count);
                check(Arrays.equals(prepared, beforeUses), "immutable prepared masks " + count);
            }
            for (String text : samples) pair(repeated, text);
            Pattern tail = Pattern.compile("a".repeat(count - 1) + "b*");
            Object tailPlan = read(tail, "m3BooleanPlan");
            check((tailPlan != null) == (count <= 256), "tail DP admission " + count);
            if (candidate && tailPlan != null) masks(tailPlan, count, false);
            for (String text : List.of("a".repeat(count - 1), "a".repeat(count - 1) + "bbb", "a".repeat(count) + "b")) pair(tail, text);
        }
        Object shortPlan = read(Pattern.compile("a*b*"), "m3BooleanPlan");
        Pattern warm = Pattern.compile("a*b*");
        for (int i = 0; i < 40; i++) warm.asMatchPredicate().test("a");
        if (candidate) shortPlan = read(warm, "m3BooleanPlan");
        Method matches = shortPlan.getClass().getDeclaredMethod("matches", String.class);
        matches.setAccessible(true);
        for (String text : List.of("\ud800", "\udc00", "\ud83d\ude00", "z\ud800")) {
            int expected = warm.matcher(new StringBuilder(text)).matches() ? 1 : 0;
            check((int) matches.invoke(shortPlan, text) == expected,
                    "surrogate decision even after dead frontier");
        }
        var random = new Random(0x4d334d41534bL);
        String[] atoms = {"a", "b", ".", "a*", "b*", ".*", "[ab]*", "\\d*", "é*", "[^x]*"};
        for (int sample = 0; sample < 3000; sample++) {
            var regex = new StringBuilder();
            for (int i = 0, n = 1 + random.nextInt(9); i < n; i++) regex.append(atoms[random.nextInt(atoms.length)]);
            pair(Pattern.compile(regex.toString(), flags[random.nextInt(flags.length)]), samples[random.nextInt(samples.length)]);
        }
        // Known oracle for a hostile repeat chain; no recursive Matcher invocation is needed.
        String hostile = "a".repeat(30000);
        var predicate = Pattern.compile("a*".repeat(30) + "b").asMatchPredicate();
        check(!predicate.test(hostile), "long rejection");
        check(predicate.test(hostile + "b"), "long acceptance");
        System.out.println("observations=" + observations + " sha256=" + HexFormat.of().formatHex(DIGEST.digest()));
    }
}
