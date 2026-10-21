// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors

/*
 * @test
 * @summary R6 differential: jdk.internal.mindex M3RegExpCompiler/M3RunTable (Lucene automaton core port) against
 *          java.util.regex.Pattern over the regular subset of the frozen M3 regex oracle corpus (10,000 x 64), chunked
 *          transfer-element composition, and String.split on separator classes via presence masks.
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm -Xmx256m M3AutomatonDifferentialTest
 */

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import jdk.internal.mindex.M3RegExpCompiler;
import jdk.internal.mindex.M3RunTable;

/**
 * Corpus generator and roots are reproduced from com.m3.precompute.M3RegexOracleCorpus (m3/ports/precompute, Synexia
 * Regex10kMatrixMasteryCorpus lineage); the test refuses to run on root drift, so the corpus is byte-identical.
 * java.util.regex.Pattern of the running JDK is the authority; M3 results are candidates (UNSUPPORTED is counted, never
 * compared). Any disagreement on a decided candidate fails the test.
 */
public final class M3AutomatonDifferentialTest {
    private static final String REGEX_ROOT = "215d0e2d2fdf7b8491cd749fdae29ef254c5bec467e47d898e3158d26617b8a6";
    private static final String STRING_ROOT = "4785a7e1ab13050473f5b42f4a31fb241ff1adba6f60dd1fae1d3a5e140ef069";
    private static final List<String> BASES = List.of("a", "b", "[a-c]", "[0-3]", "\\d", "\\w", "\\s", ".",
            "(?:ab|ba)", "\\Qa.b\\E");
    private static final List<String> REPEATS = List.of("%s", "(?:%s)?", "(?:%s)*", "(?:%s)+", "(?:%s){1}",
            "(?:%s){1,2}", "(?:%s){0,2}", "(?:%s)??", "(?:%s)*?", "(?:%s)+?");
    private static final List<String> WRAPPERS = List.of("%s", "^(?:%s)$", "^(?:%s)", "(?:%s)$", "(?:%s)",
            "(?:%s|z)", "(?:z|%s)", "(?:%s)(?:x)?", "(?:x)?(?:%s)", "(?:%s)(?:y|z)?");
    private static final List<String> TAILS = List.of("%s", "(?:%s)a?", "(?:%s)b*", "(?:%s)c+", "(?:%s)d{0,1}",
            "(?:%s)[0-2]?", "(?:%s)\\d?", "(?:%s)\\w?", "(?:%s)\\s?", "(?:%s)(?:m|n)?");
    private static final List<String> PREFIXES = List.of("", "a", "x", "class ");
    private static final List<String> CORES = List.of("", "b", "123", "\u00e9");
    private static final List<String> SUFFIXES = List.of("", "\n", "\0", " while(true){} Pattern.compile(\"[\");");
    private static final List<String> SEPARATORS = List.of(",", "[,;]", "\\s+", "\\p{Punct}");
    private static final List<String> SPLIT_INPUTS = List.of("", ",", ",,", "a,b;;c", "  lead  trail  ", "x,,",
            ",x", "a.b!c?d", "\t\n x \u000B y\f", "camelCaseString", "HTTPServerName", "a;b,c;", "no separators",
            "\u00e9,\u00e9 \u00e9", "Pattern.compile(\"[\");");

    private static long compared;
    private static long unsupported;
    private static long composed;
    private static long splits;
    private static long lexicalValid;
    private static long lexicalInvalid;
    private static long lexicalFallback;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        List<String> regexes = expressions();
        List<String> inputs = strings();
        require(REGEX_ROOT.equals(root("M3_REGEX_10K_CORPUS_V1\n", regexes)), "REGEX_10K_ROOT_DRIFT");
        require(STRING_ROOT.equals(root("M3_REGEX_64_STRINGS_V1\n", inputs)), "STRING_64_ROOT_DRIFT");
        lexicalDifferential();
        Map<String, Integer> refusals = new TreeMap<>();
        int admitted = 0;
        int javaInvalid = 0;
        for (int r = 0; r < regexes.size(); r++) {
            String regex = regexes.get(r);
            Pattern pattern;
            try {
                pattern = Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                javaInvalid++;
                require(!M3RegExpCompiler.compile(regex, 0).admitted(),
                        "INVALID_JAVA_PATTERN_ADMITTED: " + printable(regex));
                continue;
            }
            M3RegExpCompiler.Compiled compiled = M3RegExpCompiler.compile(regex, 0);
            if (!compiled.admitted()) {
                refusals.merge(compiled.refusal(), 1, Integer::sum);
                continue;
            }
            admitted++;
            for (String input : inputs) {
                Matcher m = pattern.matcher(input);
                check(regex, input, "matches", m.matches(), compiled.matches(input));
                check(regex, input, "lookingAt", pattern.matcher(input).lookingAt(), compiled.lookingAt(input));
                check(regex, input, "find", pattern.matcher(input).find(), compiled.find(input));
                if (r % 13 == 0) {
                    composition(regex, pattern, compiled, input);
                }
            }
        }
        splitDifferential();
        System.out.println("regexes=" + regexes.size() + " javaInvalid=" + javaInvalid + " admitted=" + admitted
                + " refused=" + refusals.values().stream().mapToInt(Integer::intValue).sum() + " " + refusals);
        System.out.println("decided=" + compared + " unsupported=" + unsupported + " composedChecks=" + composed
                + " splitChecks=" + splits + " failures=" + failures.size());
        for (int i = 0; i < Math.min(20, failures.size()); i++) {
            System.out.println("FAIL " + failures.get(i));
        }
        require(failures.isEmpty(), failures.size() + " differential failures");
        require(admitted > 0 && compared > 0 && composed > 0 && splits > 0, "empty differential");
        require(lexicalValid == 16 && lexicalInvalid == 32 && lexicalFallback == 11,
                "empty or incomplete lexical differential");
        System.out.println("lexicalValid=" + lexicalValid + " lexicalInvalid=" + lexicalInvalid
                + " lexicalFallback=" + lexicalFallback);
        System.out.println("PASS M3AutomatonDifferentialTest");
    }

    /** Numeric lexemes are ASCII syntax; Unicode text and unsupported grammar retain their prior fallback. */
    private static void lexicalDifferential() {
        String[][] valid = {
            {"\\x30", "0"}, {"\\x41", "A"}, {"\\x4a", "J"}, {"\\x4A", "J"},
            {"\\u0030", "0"}, {"\\u0041", "A"}, {"\\u00e9", "\u00e9"},
            {"\\x{41}", "A"}, {"\\x{00000041}", "A"}, {"\\x{1F600}", "\uD83D\uDE00"},
            {"\\x{10FFFF}", "\uDBFF\uDFFF"}, {"\\uD83D\\uDE00", "\uD83D\uDE00"},
            {"\\uD800\\uDC00", "\uD800\uDC00"}, {"\\uDBFF\\uDFFF", "\uDBFF\uDFFF"},
            {"[\\x30-\\x39]+", "123"}, {"[\\u0030-\\u0039]+", "123"}
        };
        for (String[] row : valid) {
            Pattern oracle = Pattern.compile(row[0]);
            M3RegExpCompiler.Compiled candidate = M3RegExpCompiler.compile(row[0], 0);
            require(candidate.admitted(), "ASCII_ESCAPE_REFUSED: " + row[0] + " " + candidate.refusal());
            require(oracle.matcher(row[1]).matches(), "ASCII_ESCAPE_ORACLE: " + row[0]);
            for (String text : List.of(row[1], "", "prefix" + row[1], row[1] + "suffix")) {
                check(row[0], text, "lexical.matches", oracle.matcher(text).matches(), candidate.matches(text));
                check(row[0], text, "lexical.lookingAt", oracle.matcher(text).lookingAt(), candidate.lookingAt(text));
                check(row[0], text, "lexical.find", oracle.matcher(text).find(), candidate.find(text));
            }
            lexicalValid++;
        }
        String[] invalid = {
            "\\x\uff14\uff11", "\\x\u0664\u0661", "\\x4\uff11", "\\x\uff144",
            "\\x4\u0661", "\\x\u06644", "\\u\uff10\uff10\uff14\uff11", "\\u\u0660\u0660\u0664\u0661",
            "\\u00\uff14\uff11", "\\u00\u0664\u0661", "\\u00\uff26\uff26", "\\u00\u0664F",
            "\\x{\uff14\uff11}", "\\x{\u0664\u0661}", "\\x{4\uff11}", "\\x{4\u0661}",
            "\\x", "\\x4", "\\xGG", "\\x{}", "\\x{41", "\\x{GG}",
            "\\u", "\\u123", "\\u00GG", "\\x{110000}", "\\x{7FFFFFFF}", "\\x{80000000}",
            "\\x{FFFFFFFF}", "\\x{100000000}", "a{\uff11}", "a{1,\u0662}"
        };
        for (String regex : invalid) {
            boolean rejected = false;
            try {
                Pattern.compile(regex);
            } catch (PatternSyntaxException expected) {
                rejected = true;
            }
            require(rejected, "INVALID_SYNTAX_ORACLE_ACCEPTED: " + printable(regex));
            require(!M3RegExpCompiler.compile(regex, 0).admitted(),
                    "INVALID_SYNTAX_ADMITTED: " + printable(regex));
            lexicalInvalid++;
        }
        for (String regex : List.of("\\d+", "\\D", "[\\d]")) {
            Pattern oracle = Pattern.compile(regex);
            M3RegExpCompiler.Compiled candidate = M3RegExpCompiler.compile(regex, 0);
            require(candidate.admitted(), "DEFAULT_DIGIT_CLASS_REFUSED: " + regex);
            for (String text : List.of("123", "\u0661", "\uff11", "A")) {
                check(regex, text, "default-digit.matches", oracle.matcher(text).matches(), candidate.matches(text));
                check(regex, text, "default-digit.find", oracle.matcher(text).find(), candidate.find(text));
            }
        }
        int[] flags = {Pattern.CASE_INSENSITIVE, Pattern.UNICODE_CASE, Pattern.UNICODE_CHARACTER_CLASS,
                Pattern.LITERAL};
        for (int flag : flags) {
            Pattern.compile("\\d+", flag);
            M3RegExpCompiler.Compiled candidate = M3RegExpCompiler.compile("\\d+", flag);
            require(!candidate.admitted() && "FLAGS".equals(candidate.refusal()), "FLAG_FALLBACK_CHANGED: " + flag);
            lexicalFallback++;
        }
        String[][] fallback = {
            {"(?U)\\d+", "GROUP_KIND"}, {"(?u)\\d+", "GROUP_KIND"}, {"(?i)a", "GROUP_KIND"},
            {"(a)\\1", "ESCAPE"}, {"\\p{Nd}", "UNICODE_CATEGORY"},
            {"\\uD800", "SURROGATE_LITERAL"}, {"\\uDC00", "SURROGATE_LITERAL"}
        };
        for (String[] row : fallback) {
            Pattern.compile(row[0]);
            M3RegExpCompiler.Compiled candidate = M3RegExpCompiler.compile(row[0], 0);
            require(!candidate.admitted() && row[1].equals(candidate.refusal()),
                    "GRAMMAR_OR_SURROGATE_FALLBACK_CHANGED: " + row[0] + " " + candidate.refusal());
            lexicalFallback++;
        }
        require(Pattern.compile("\\d").matcher("\u0661").matches() == false,
                "DEFAULT_DIGIT_ORACLE_CHANGED");
        require(Pattern.compile("\\d", Pattern.UNICODE_CHARACTER_CLASS).matcher("\u0661").matches(),
                "UNICODE_DIGIT_ORACLE_CHANGED");
    }

    /** Whole-text verdict from per-chunk transfer elements at every cut, for matches and find existence. */
    private static void composition(String regex, Pattern pattern, M3RegExpCompiler.Compiled compiled, String input) {
        M3RunTable table = compiled.matchTable();
        for (int cut = 0; cut <= input.length(); cut++) {
            for (int cut2 : new int[] {(cut + input.length()) / 2}) {
                M3RunTable.Element e = table.compose(table.compose(table.transfer(input, 0, cut),
                        table.transfer(input, cut, cut2)), table.transfer(input, cut2, input.length()));
                check(regex, input + "|cut=" + cut + "," + cut2, "composed.matches", pattern.matcher(input).matches(),
                        table.verdict(e));
                composed++;
                M3RunTable find = compiled.findTable();
                if (find != null && !compiled.anchorEnd() && !hasSurrogate(input)) {
                    M3RunTable.Element f = find.compose(find.transfer(input, 0, cut), find.transfer(input, cut,
                            input.length()));
                    int verdict = f.firstAccept(0) >= 0 ? M3RunTable.TRUE : M3RunTable.FALSE;
                    check(regex, input + "|cut=" + cut, "composed.find", pattern.matcher(input).find(), verdict);
                    composed++;
                }
            }
        }
    }

    /** String.split on separator classes from presence masks composed over chunks (no scan of a joined text). */
    private static void splitDifferential() {
        List<String> inputs = new ArrayList<>(SPLIT_INPUTS);
        inputs.addAll(strings());
        for (String sep : SEPARATORS) {
            M3RunTable t = M3RegExpCompiler.compile(sep, 0).matchTable();
            require(t != null, "separator not admitted: " + sep);
            int expectedShape = sep.endsWith("+") ? M3RunTable.SHAPE_UNIT_CLASS_RUN : M3RunTable.SHAPE_UNIT_CLASS;
            require(t.shape() == expectedShape, "shape of " + sep + " = " + t.shape());
            for (String input : inputs) {
                boolean[] mask = chunkedMask(t, input);
                List<int[]> matches = new ArrayList<>();
                for (int i = 0; i < input.length(); i++) {
                    if (!mask[i]) {
                        continue;
                    }
                    int j = i;
                    if (expectedShape == M3RunTable.SHAPE_UNIT_CLASS_RUN) {
                        while (j + 1 < input.length() && mask[j + 1]) {
                            j++;
                        }
                    }
                    matches.add(new int[] {i, j + 1});
                    i = j;
                }
                splitCheck(sep, input, matches);
            }
        }
        M3RunTable lower = M3RegExpCompiler.compile("[a-z]", 0).matchTable();
        M3RunTable upper = M3RegExpCompiler.compile("[A-Z]", 0).matchTable();
        String camel = "(?<=[a-z])(?=[A-Z])";
        for (String input : inputs) {
            boolean[] lo = chunkedMask(lower, input);
            boolean[] up = chunkedMask(upper, input);
            List<int[]> matches = new ArrayList<>();
            for (int i = 1; i < input.length(); i++) {
                if (lo[i - 1] && up[i]) {
                    matches.add(new int[] {i, i});
                }
            }
            splitCheck(camel, input, matches);
        }
    }

    /** Presence mask of the whole input as the OR of per-chunk masks shifted by chunk offsets (3 chunks). */
    private static boolean[] chunkedMask(M3RunTable t, String input) {
        boolean[] mask = new boolean[input.length()];
        int[] cuts = {0, input.length() / 3, (2 * input.length()) / 3, input.length()};
        for (int c = 0; c + 1 < cuts.length; c++) {
            long[] words = t.presence(input, cuts[c], cuts[c + 1]);
            for (int k = 0; k < cuts[c + 1] - cuts[c]; k++) {
                if ((words[k >>> 6] & (1L << k)) != 0) {
                    mask[cuts[c] + k] = true;
                }
            }
        }
        return mask;
    }

    private static void splitCheck(String regex, String input, List<int[]> matches) {
        List<String> pieces = new ArrayList<>();
        int last = 0;
        for (int[] m : matches) {
            if (m[1] == 0) {
                continue;
            }
            pieces.add(input.substring(last, m[0]));
            last = m[1];
        }
        if (matches.isEmpty()) {
            pieces = List.of(input);
        } else {
            pieces.add(input.substring(last));
            int end = pieces.size();
            while (end > 0 && pieces.get(end - 1).isEmpty()) {
                end--;
            }
            pieces = pieces.subList(0, end);
        }
        String[] expected = input.split(regex);
        splits++;
        if (!Arrays.equals(expected, pieces.toArray(new String[0]))) {
            failures.add("split " + regex + " on '" + printable(input) + "' java=" + Arrays.toString(expected)
                    + " m3=" + pieces);
        }
    }

    private static void check(String regex, String input, String op, boolean expected, int candidate) {
        if (candidate == M3RunTable.UNSUPPORTED) {
            unsupported++;
            return;
        }
        compared++;
        if (expected != (candidate == M3RunTable.TRUE)) {
            failures.add(op + " /" + regex + "/ on '" + printable(input) + "' java=" + expected + " m3=" + candidate);
        }
    }

    private static boolean hasSurrogate(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isSurrogate(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s.replace("\n", "\\n").replace("\0", "\\0").replace("\t", "\\t").replace("\r", "\\r");
    }

    private static List<String> expressions() {
        List<String> out = new ArrayList<>();
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
        return out;
    }

    private static List<String> strings() {
        List<String> out = new ArrayList<>();
        for (String prefix : PREFIXES) {
            for (String core : CORES) {
                for (String suffix : SUFFIXES) {
                    out.add(prefix + core + suffix);
                }
            }
        }
        return out;
    }

    private static String root(String header, List<String> values) throws Exception {
        StringBuilder material = new StringBuilder(header);
        for (String v : values) {
            material.append(v.length()).append(':').append(v).append('\n');
        }
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(material.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
