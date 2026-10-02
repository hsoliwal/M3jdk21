/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.LocalM3Arena;
import com.m3.text.M3StringPiece;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Focused stock-JVM interoperability, not an exhaustive regex conformance suite. */
public final class ViewInteropContractTest {
    private static int checks;
    private static void same(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }
    private static void matches(Matcher expected, Matcher actual) {
        for (;;) {
            boolean found = expected.find();
            same(found, actual.find());
            if (!found) break;
            same(expected.groupCount(), actual.groupCount());
            for (int group = 0; group <= expected.groupCount(); group++) {
                same(expected.start(group), actual.start(group));
                same(expected.end(group), actual.end(group));
                same(expected.group(group), actual.group(group));
            }
            same(expected.hitEnd(), actual.hitEnd());
            same(expected.requireEnd(), actual.requireEnd());
        }
    }
    public static void main(String[] args) {
        String[] inputs = {"", "ababa aa", "a\0b\0a", "a\r\nb\na", "\ud83d\ude00a\ud83d\ude00",
                "\ud800a\udc00", "\u00c4\u00e4\u03a3\u03c3", "a$\\b"};
        String[] expressions = {"a+", "(a)\\1", "(?<=a)b", "(?<word>\\w+)", "^.*$",
                "\\X", ".", "\\b\\w+\\b", "\ud83d\ude00", "^|$", "[\\s\\S]", "(a)|(b)"};
        int[] flags = {0, Pattern.DOTALL | Pattern.MULTILINE,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS};
        for (String input : inputs) {
            M3StringPiece atom = new LocalM3Arena().copyUtf16(input.toCharArray());
            for (int cut = 0; cut <= input.length(); cut++) {
                M3StringPiece view = M3StringPiece.join(atom.subSequence(0, cut), atom.subSequence(cut, input.length()));
                same(true, Arrays.equals(input.chars().toArray(), view.chars().toArray()));
                same(true, Arrays.equals(input.codePoints().toArray(), view.codePoints().toArray()));
                for (String expression : expressions) for (int flag : flags) {
                    Pattern pattern = Pattern.compile(expression, flag);
                    matches(pattern.matcher(input), pattern.matcher(view));
                    same(pattern.matcher(input).matches(), pattern.matcher(view).matches());
                    same(pattern.matcher(input).lookingAt(), pattern.matcher(view).lookingAt());
                    same(pattern.matcher(input).replaceAll("[$0]"), pattern.matcher(view).replaceAll("[$0]"));
                    same(pattern.matcher(input).replaceFirst("[$0]"), pattern.matcher(view).replaceFirst("[$0]"));
                    same(true, Arrays.equals(pattern.split(input, -1), pattern.split(view, -1)));
                    for (boolean transparent : new boolean[]{false, true}) {
                        Matcher expected = pattern.matcher(input).region(cut, input.length());
                        Matcher actual = pattern.matcher(view).region(cut, input.length());
                        expected.useTransparentBounds(transparent).useAnchoringBounds(!transparent);
                        actual.useTransparentBounds(transparent).useAnchoringBounds(!transparent);
                        matches(expected, actual);
                    }
                }
            }
        }
        System.out.println("VIEW_INTEROP_PASS checks=" + checks + " oracle=stock-Java21 scope=focused-regex-codepoints");
    }
}
