/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3JoinedStringRegex {
    private static final Field STORAGE = field(String.class, "m3Storage");
    private static final Field MATERIALIZED;

    static {
        try {
            Class<?> storageClass = Class.forName("java.lang.M3StringStorage");
            MATERIALIZED = field(storageClass, "materialized");
        } catch (ClassNotFoundException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        boolean enabled = Boolean.getBoolean("m3.enabled");

        String input = String.join("",
                new String("prefix-ab"),
                new String("12"),
                new String("_"),
                new String(new char[] {'\ud83d'}),
                new String(new char[] {'\ude00'}),
                new String("-word"),
                new String("-suffix"));

        eq("prefix-ab12_😀-word-suffix", input, "input");

        Matcher main = Pattern.compile("ab(?<digits>\\d+)_\\x{1F600}-(word)").matcher(input);
        check(main.find(), "main find");
        eq("12", main.group("digits"), "named group");
        eq("word", main.group(2), "numeric group");

        Matcher look = Pattern.compile("(?<=ab)\\d+(?=_\\x{1F600})").matcher(input);
        check(look.find(), "lookaround");
        eq("12", look.group(), "lookaround group");

        Matcher boundary = Pattern.compile("\\bword\\b").matcher(input);
        check(boundary.find(), "word boundary");
        eq("word", boundary.group(), "word boundary group");

        Matcher literal = Pattern.compile("ab12_😀", Pattern.LITERAL).matcher(input);
        check(literal.find(), "literal seam");
        eq("ab12_😀", literal.group(), "literal group");

        Matcher ci = Pattern.compile("PREFIX-AB", Pattern.LITERAL | Pattern.CASE_INSENSITIVE)
                .matcher(input);
        check(ci.find(), "ASCII case-insensitive literal");

        Matcher region = Pattern.compile("\\d+_😀").matcher(input);
        region.region("prefix-ab".length(), input.length() - "-word-suffix".length());
        check(region.matches(), "region matches");

        MatchResult snapshot = main.toMatchResult();
        eq("12", snapshot.group(1), "MatchResult capture");

        String[] split = Pattern.compile("[-_]").split(input);
        check(Arrays.equals(
                new String[] {"prefix", "ab12", "😀", "word", "suffix"}, split),
                "split across segmented input");

        String replaced = Pattern.compile("(ab)(\\d+)").matcher(input).replaceAll("$1[$2]");
        eq("prefix-ab[12]_😀-word-suffix", replaced, "replaceAll");

        String odd = String.join("",
                new String(new char[] {'\ud800'}),
                new String("x"),
                new String(new char[] {'\udc00'}));
        String oddLiteral = new String(new char[] {'\ud800', 'x', '\udc00'});
        check(Pattern.compile(Pattern.quote(oddLiteral)).matcher(odd).matches(),
                "unpaired surrogate units");

        String supplementaryAcrossSeam = String.join("",
                new String(new char[] {'a', '\ud83d'}),
                new String(new char[] {'\ude00', 'b'}));
        Matcher supplementary = Pattern.compile("a.\\x{1F600}?b|a\\x{1F600}b")
                .matcher(supplementaryAcrossSeam);
        check(supplementary.matches(), "supplementary seam");

        if (enabled) {
            Object storage = STORAGE.get(input);
            check(storage != null, "regex input should be segmented");
            check(MATERIALIZED.get(storage) == null,
                    "Pattern/Matcher matching must not flatten segmented input");
            check(STORAGE.get(main.group()) != null,
                    "captured group should preserve String slice storage");
        }

        System.out.println("M3_STRING_REGEX_PASS enabled=" + enabled);
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void eq(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
