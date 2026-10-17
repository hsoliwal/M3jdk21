/* Copyright (c) 2026, Hitesh Soliwal. GPLv2 with Classpath exception. */
/* @test
 * @summary M3JDK21 Strings utility family, Unicode and donor behavior
 * @compile StringsContractTest.java
 * @run main StringsContractTest
 */
import java.util.Objects;
import java.util.Random;
import java.util.Strings;

public final class StringsContractTest {
    private static long checks;
    private static void eq(Object actual, Object expected) {
        checks++;
        if (!Objects.equals(actual, expected))
            throw new AssertionError("expected=" + expected + " actual=" + actual);
    }
    private static void eq(boolean actual, boolean expected) { eq(Boolean.valueOf(actual), Boolean.valueOf(expected)); }
    private static void refusal(Class<? extends Throwable> type, Runnable call) {
        checks++;
        try { call.run(); }
        catch (Throwable result) {
            if (type.isInstance(result)) return;
            throw new AssertionError("wrong exception", result);
        }
        throw new AssertionError("missing exception " + type);
    }
    public static void main(String... args) {
        eq(Strings.isNullOrEmpty(null), true);
        eq(Strings.isNullOrEmpty(""), true);
        eq(Strings.isNullOrEmpty("x"), false);
        eq(Strings.nullToEmpty(null), "");
        eq(Strings.emptyToNull(""), null);
        eq(Strings.defaultIfEmpty("", "fallback"), "fallback");
        eq(Strings.defaultIfBlank(" \t", "fallback"), "fallback");
        eq(Strings.isNullOrBlank("\u2003\t"), true);
        eq(Strings.isNullOrBlank("\u00A0"), false);
        eq(Strings.hasText("\u00A0"), true);
        eq(Strings.padStart("ab", 5, '0'), "000ab");
        eq(Strings.padEnd("ab", 4, '0'), "ab00");
        eq(Strings.repeat("ab", 3), "ababab");
        eq(Strings.commonPrefix("abc", "abd"), "ab");
        eq(Strings.commonSuffix("zzabc", "xxabc"), "abc");
        eq(Strings.commonPrefix("x\ud83d\ude00A", "x\ud83d\ude02B"), "x");
        eq(Strings.commonSuffix("x\ud83d\ude00", "y\ud83d\ude00"), "\ud83d\ude00");
        eq(Strings.removePrefix("abc", "a"), "bc");
        eq(Strings.removeSuffix("abc", "bc"), "a");
        eq(Strings.substringBefore("abc:def", ":"), "abc");
        eq(Strings.substringAfter("abc:def", ":"), "def");
        eq(Strings.substringBeforeLast("a:b:c", ":"), "a:b");
        eq(Strings.substringAfterLast("a:b:c", ":"), "c");
        eq(Strings.substringAfter("abc", ":"), "");
        eq(Strings.countOccurrences("aaaaa", "aa"), 2);
        eq(Strings.countOccurrences("aba", ""), 0);
        eq(Strings.containsIgnoreCase("ABc", "bc"), true);
        eq(Strings.startsWithIgnoreCase("Abc", "aB"), true);
        eq(Strings.endsWithIgnoreCase("aBc", "BC"), true);
        eq(Strings.indexOfIgnoreCase("AbCaBc", "bc", 2), 4);
        eq(Strings.indexOfIgnoreCase("abc", "", 99), 3);
        eq(Strings.reverse("x\ud83d\ude00y"), "y\ud83d\ude00x");
        eq(Strings.stripToNull("  "), null);
        eq(Strings.stripToEmpty(null), "");
        refusal(NullPointerException.class, () -> Strings.commonPrefix(null, "x"));
        refusal(IllegalArgumentException.class, () -> Strings.padStart("x", -1, 'a'));
        refusal(IllegalArgumentException.class, () -> Strings.repeat("a", -1));
        Random random = new Random(20261017L);
        String alphabet = "abAB01\u0000\u2003\ud83d\ude00\uffff";
        for (int trial = 0; trial < 20000; trial++) {
            StringBuilder left = new StringBuilder(), right = new StringBuilder();
            for (int i = random.nextInt(18); i >= 0; i--) left.append(alphabet.charAt(random.nextInt(alphabet.length())));
            for (int i = random.nextInt(18); i >= 0; i--) right.append(alphabet.charAt(random.nextInt(alphabet.length())));
            String x = left.toString(), y = right.toString();
            String prefix = Strings.commonPrefix(x, y), suffix = Strings.commonSuffix(x, y);
            eq(x.startsWith(prefix) && y.startsWith(prefix), true);
            eq(x.endsWith(suffix) && y.endsWith(suffix), true);
            eq(Strings.reverse(x), new StringBuilder(x).reverse().toString());
            eq(Strings.isNullOrBlank(x), x.isBlank());
            eq(Strings.startsWithIgnoreCase(x, y), x.regionMatches(true, 0, y, 0, y.length()));
            eq(Strings.indexOfIgnoreCase(x, "a", 0) >= 0, 
               x.toLowerCase(java.util.Locale.ROOT).contains("a"));
        }
        System.out.println("M3_STRINGS_UTILITY_PASS checks=" + checks);
    }
}
