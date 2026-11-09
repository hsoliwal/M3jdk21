// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary M3TQ absence gate for general patterns: sound against the ungated engine over the frozen
 *          10k x 64 corpus and targeted constructs, with Matcher state parity on every search
 * @modules java.base/java.util.regex:+open
 *          java.base/jdk.internal.mindex
 * @run main/othervm -Xmx512m M3PatternQueryTest
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3TQ;

public class M3PatternQueryTest {
    private static final List<String> BASES =
            List.of("a", "b", "[a-c]", "[0-3]", "\\d", "\\w", "\\s", ".", "(?:ab|ba)", "\\Qa.b\\E");
    private static final List<String> REPEATS =
            List.of("%s", "(?:%s)?", "(?:%s)*", "(?:%s)+", "(?:%s){1}", "(?:%s){1,2}", "(?:%s){0,2}",
                    "(?:%s)??", "(?:%s)*?", "(?:%s)+?");
    private static final List<String> WRAPPERS =
            List.of("%s", "^(?:%s)$", "^(?:%s)", "(?:%s)$", "(?:%s)", "(?:%s|z)", "(?:z|%s)",
                    "(?:%s)(?:x)?", "(?:x)?(?:%s)", "(?:%s)(?:y|z)?");
    private static final List<String> TAILS =
            List.of("%s", "(?:%s)a?", "(?:%s)b*", "(?:%s)c+", "(?:%s)d{0,1}", "(?:%s)[0-2]?",
                    "(?:%s)\\d?", "(?:%s)\\w?", "(?:%s)\\s?", "(?:%s)(?:m|n)?");
    private static final List<String> PREFIXES = List.of("", "a", "x", "class ");
    private static final List<String> CORES = List.of("", "b", "123", "é");
    private static final List<String> SUFFIXES =
            List.of("", "\n", "\0", " while(true){} Pattern.compile(\"[\");");

    private static final List<String> TARGETED_REGEXES = List.of(
            "abcd", "ab(cd)ef", "(?:abc|abd)efg", "(abc)+d", "(abc)*d", "(abc){2,}d", "(abc)+?d",
            "(abc)*+d", "(abc)?def", "abc(?:x)?def", "x{0,2}abc", "(abc){0}xyz", "((ab)+cd){2}",
            "a(?=bcd)", "(?<=abc)def", "(?<!abc)def", "(abc)\\1", "[a-c]bcd", "abc\\d+def",
            "(?i)abcd", "abc(?i)def", "\\Qa.b.c\\E", "abc|", "|abc", "(|abc)def", "😀abc",
            "abc$", "abc\\b", "\\Aabc", "\\Gabc", "abc(?!x)def", "^abc", "abc\\z", "(?m)^abc$",
            "abcdefghijklmnopqrstuvwxyz", "a", "ab", "abc", ".*abc.*", "(a|b|c)bcd");
    private static final List<String> TARGETED_INPUTS = List.of(
            "", "abc", "abcd", "abcdef", "xabcdefy", "abdefg", "ababcdcd", "abcabc", "ABCD",
            "a.b.c", "😀abc", "zzz", "abc\nabc", "def", "cbcd", "abc1def", "abcxdef",
            "x" + "abcd".repeat(50));

    private static Field M3TQ_FIELD;
    private static long checks;
    private static long constrained;
    private static long pruned;

    public static void main(String[] args) throws Exception {
        M3TQ_FIELD = Pattern.class.getDeclaredField("m3Tq");
        M3TQ_FIELD.setAccessible(true);
        designPins();
        List<String> inputs = strings();
        List<String> expressions = expressions();
        for (String regex : expressions) {
            differential(Pattern.compile(regex), inputs);
        }
        for (String regex : TARGETED_REGEXES) {
            differential(Pattern.compile(regex), TARGETED_INPUTS);
        }
        differential(Pattern.compile("a.b.c", Pattern.LITERAL), TARGETED_INPUTS);
        differential(Pattern.compile("abcd", Pattern.CASE_INSENSITIVE), TARGETED_INPUTS);
        differential(Pattern.compile("abcd", Pattern.CANON_EQ), TARGETED_INPUTS);
        check(pruned > 0, "the gate must prune at least once over the corpus");
        System.out.println("M3PatternQueryTest checks=" + checks + " constrainedPatterns=" + constrained
                + " prunedSearches=" + pruned);
    }

    /** Design pins: which patterns are admitted; not a JDK contract. */
    private static void designPins() throws Exception {
        check(query("abcd") != null, "plain literal regex is constrained");
        check(query("ab(cd)ef") != null, "literal runs across a group are constrained");
        check(query("abc(?:x)?def") != null, "optional atom between runs keeps both runs");
        check(query("(?:abc|abd)efg") != null, "alternation of literals is constrained");
        check(query("(abc)+d") != null, "group repeated at least once is constrained");
        check(query("(abc)*d") == null, "group repeated zero or more times has no requirement");
        check(query("abc$") == null, "Dollar is refused for requireEnd parity");
        check(query("abc\\b") == null, "Bound is refused for requireEnd parity");
        check(query("abc(?!x)def") == null, "Neg is refused for requireEnd parity");
        check(query("\\Aabc") == null, "Begin-rooted pattern is refused for hitEnd parity");
        check(query("\\Gabc") != null, "LastMatch anchor compiles under a Start root and keeps the literal");
        check(query("(?i)abcd") == null, "case-insensitive slice has no requirement");
        check(query("ab") == null, "runs shorter than a trigram have no requirement");
        check(queryOf(Pattern.compile("abcd", Pattern.CANON_EQ)) == null, "CANON_EQ is refused");
        check(queryOf(Pattern.compile("a.b.c", Pattern.LITERAL)) != null, "LITERAL gate is unchanged");
    }

    private static M3TQ query(String regex) throws Exception {
        return queryOf(Pattern.compile(regex));
    }

    private static M3TQ queryOf(Pattern pattern) throws Exception {
        return (M3TQ) M3TQ_FIELD.get(pattern);
    }

    private static void differential(Pattern pattern, List<String> inputs) throws Exception {
        M3TQ query = queryOf(pattern);
        if (query != null && query.hasConstraints()) {
            constrained++;
        }
        for (String input : inputs) {
            Matcher gated = pattern.matcher(input);
            Matcher free = pattern.matcher(new Ungated(input));
            if (query != null && query.hasConstraints()
                    && !query.testPrecomputed(M3TQ.precompute(input, input.length()))) {
                pruned++;
            }
            check(gated.matches() == free.matches(), pattern + " matches " + show(input));
            check(gated.lookingAt() == free.lookingAt(), pattern + " lookingAt " + show(input));
            gated.reset();
            free.reset();
            for (int step = 0; step < 64; step++) {
                boolean g = gated.find();
                boolean f = free.find();
                check(g == f, pattern + " find#" + step + " " + show(input));
                check(gated.hitEnd() == free.hitEnd(), pattern + " hitEnd#" + step + " " + show(input));
                check(gated.requireEnd() == free.requireEnd(), pattern + " requireEnd#" + step + " " + show(input));
                if (!g) {
                    break;
                }
                check(gated.start() == free.start() && gated.end() == free.end(),
                        pattern + " span#" + step + " " + show(input));
                check(gated.groupCount() == free.groupCount(), pattern + " groups");
                for (int group = 0; group <= gated.groupCount(); group++) {
                    check(gated.start(group) == free.start(group) && gated.end(group) == free.end(group),
                            pattern + " group" + group + "#" + step + " " + show(input));
                }
                if (gated.end() == gated.start() && gated.end() >= input.length()) {
                    break;
                }
            }
        }
    }

    private static String show(String input) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c < 32 || c > 126) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static List<String> expressions() {
        List<String> out = new ArrayList<>(10_000);
        for (String base : BASES) {
            for (String repeat : REPEATS) {
                String repeated = repeat.formatted(base);
                for (String wrapper : WRAPPERS) {
                    String wrapped = wrapper.formatted(repeated);
                    for (String tail : TAILS) {
                        out.add(tail.formatted(wrapped));
                    }
                }
            }
        }
        check(out.size() == 10_000 && out.stream().distinct().count() == 10_000, "corpus cardinality");
        return out;
    }

    static List<String> strings() {
        List<String> out = new ArrayList<>(64);
        for (String prefix : PREFIXES) {
            for (String core : CORES) {
                for (String suffix : SUFFIXES) {
                    out.add(prefix + core + suffix);
                }
            }
        }
        check(out.size() == 64, "input cardinality");
        return out;
    }

    /** A CharSequence that is not a String, so Matcher never consults the gate. */
    private static final class Ungated implements CharSequence {
        private final String text;

        Ungated(String text) {
            this.text = text;
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            return text.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new Ungated(text.substring(start, end));
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
