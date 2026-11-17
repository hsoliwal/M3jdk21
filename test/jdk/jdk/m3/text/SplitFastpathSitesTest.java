// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors

/*
 * @test
 * @summary The tab and line-feed splits of the jdk.m3 TSV parsers take the String.split
 *          fastpath and give the results of the regex arguments they replaced
 * @modules java.base/java.lang:open java.base/jdk.m3.precompute java.base/jdk.m3.text:open
 *          java.base/jdk.m3.regexcursor
 * @run main/othervm SplitFastpathSitesTest
 */

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.regex.Pattern;

import jdk.m3.precompute.CpuToolImageCompiler;
import jdk.m3.regexcursor.GrammarImageTool;
import jdk.m3.text.M3LanguageCorpusManifest;
import jdk.m3.text.SharedLexiconFamilySidecarCatalog;

/**
 * Six splits of the jdk.m3 TSV parsers passed the regex arguments backslash-t and
 * backslash-n. A backslash followed by a letter is not on the String.split fastpath, so every
 * call went to a compiled Pattern. They now pass the tab and line-feed characters, which are.
 *
 * <p>Differential: for every text and limit, the split around the character equals the split
 * around the regex it replaced, with Pattern as the oracle. Discriminator: String keeps the
 * Pattern of each regex argument it compiles in its RegexMemo table. After each parser has
 * read a row, the table holds no Pattern for backslash-t or backslash-n; before the change,
 * each parser leaves one there.
 */
public class SplitFastpathSitesTest {
    private static final String TAB_REGEX = "\\t";
    private static final String LINE_REGEX = "\\n";
    private static final List<String> TEXTS = List.of("", "\t", "\n", "a", "a\tb", "\ta\t",
            "a\t\tb\t\t", "a\nb\n", "\n\na\n", "a\r\nb", "x\t\ny\n\t", "\u00e9\t\ud83d\ude00\n",
            "a\u000bb\fc d", "\t\t\t", "\n\n", "a\\tb\\nc");
    private static final int[] LIMITS = {-1, 0, 1, 2, 5};
    private static AtomicReferenceArray<SoftReference<Pattern>> memo;
    private static int checks;

    /** A parser call that is expected to refuse its input after it has split it. */
    private interface Call {
        void run() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        memo = memo();
        differential();
        observation();
        Path dir = Files.createTempDirectory("split-fastpath");
        parse("CpuToolImageCompiler.compileTsv", "invalid tool row 1",
                () -> CpuToolImageCompiler.compileTsv(CpuToolImageCompiler.HEADER
                        + "\nrow\twith\tthree\n"));
        parse("M3LanguageCorpusManifest.read", "invalid corpus manifest line 2",
                () -> M3LanguageCorpusManifest.read(write(dir.resolve("corpus.tsv"),
                        M3LanguageCorpusManifest.HEADER + "\nartifact\tx\n")));
        parse("SharedLexiconFamilySidecarCatalog.rows", "field count mismatch",
                () -> invoke(SharedLexiconFamilySidecarCatalog.class, "rows",
                        new Class<?>[] {byte[].class, String[].class, int.class},
                        "h1\th2\nx\n".getBytes(StandardCharsets.UTF_8),
                        new String[] {"h1", "h2"}, 1));
        String lexicon = write(dir.resolve("lexicon.tsv"), "row-without-tab\n").toString();
        String grammar = write(dir.resolve("grammar.ebnf"), "").toString();
        String image = dir.resolve("syntax.m3grammar").toString();
        parse("GrammarImageTool.main", "malformed lexicon row",
                () -> GrammarImageTool.main(new String[] {lexicon, grammar, "start", image}));
        System.out.println("PASS checks=" + checks);
    }

    /** The character split equals the split around the regex it replaced. */
    private static void differential() {
        for (String text : TEXTS) {
            for (int limit : LIMITS) {
                same(text, limit, TAB_REGEX, "\t");
                same(text, limit, LINE_REGEX, "\n");
            }
        }
    }

    private static void same(String text, int limit, String regex, String character) {
        String[] oracle = Pattern.compile(regex).split(text, limit);
        check(Arrays.equals(oracle, text.split(regex, limit)), "regex split of " + show(text));
        check(Arrays.equals(oracle, text.split(character, limit)),
                "character split of " + show(text) + " limit " + limit);
    }

    /** The memo shows a compiled regex; the characters meet the fastpath condition. */
    private static void observation() {
        clear();
        check("a\tb".split(TAB_REGEX, -1).length == 2, "regex split result");
        check(compiled(TAB_REGEX), "the regex backslash-t is compiled and kept in the memo");
        clear();
        check("a\tb\nc".split("\t", -1).length == 2 && "a\nb".split("\n", -1).length == 2,
                "character split result");
        check(!compiled("\t") && !compiled("\n"), "a character split compiles no Pattern");
        check(fastpath("\t") && fastpath("\n"), "tab and line feed meet the fastpath condition");
        check(!fastpath(TAB_REGEX) && !fastpath(LINE_REGEX),
                "backslash-t and backslash-n do not meet the fastpath condition");
    }

    /** Runs one parser on a refused row and checks that it compiled neither regex. */
    private static void parse(String name, String refusal, Call call) {
        clear();
        try {
            call.run();
            throw new AssertionError(name + " accepted a malformed row");
        } catch (Exception expected) {
            check(String.valueOf(expected.getMessage()).contains(refusal),
                    name + " refused the row after splitting it: " + expected);
        }
        check(!compiled(TAB_REGEX), name + " compiled backslash-t: its split left the fastpath");
        check(!compiled(LINE_REGEX), name + " compiled backslash-n: its split left the fastpath");
    }

    /** The fastpath condition of String.split, as its comment states it. */
    private static boolean fastpath(String regex) {
        char ch = regex.charAt(regex.length() - 1);
        boolean one = regex.length() == 1 && ".$|()[{^?*+\\".indexOf(ch) < 0;
        boolean two = regex.length() == 2 && regex.charAt(0) == '\\'
                && !(ch < 0x80 && Character.isLetterOrDigit(ch));
        return (one || two) && !Character.isSurrogate(ch);
    }

    @SuppressWarnings("unchecked")
    private static AtomicReferenceArray<SoftReference<Pattern>> memo() throws Exception {
        Class<?> type = Class.forName("java.lang.String$RegexMemo");
        return (AtomicReferenceArray<SoftReference<Pattern>>) field(type, "TABLE");
    }

    private static void clear() {
        for (int i = 0; i < memo.length(); i++) memo.set(i, null);
    }

    private static boolean compiled(String regex) {
        for (int i = 0; i < memo.length(); i++) {
            SoftReference<Pattern> kept = memo.get(i);
            Pattern pattern = kept == null ? null : kept.get();
            if (pattern != null && pattern.pattern().equals(regex)) return true;
        }
        return false;
    }

    private static Object field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object invoke(Class<?> type, String name, Class<?>[] parameters,
            Object... arguments) throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException thrown) {
            throw (Exception) thrown.getCause();
        }
    }

    private static Path write(Path file, String text) throws Exception {
        return Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static String show(String text) {
        return text.chars().mapToObj(ch -> ch < 0x20 || ch > 0x7e
                ? String.format("\\u%04x", ch) : String.valueOf((char) ch))
                .reduce("\"", String::concat) + "\"";
    }

    private static void check(boolean success, String what) {
        checks++;
        if (!success) throw new AssertionError(what);
    }
}
