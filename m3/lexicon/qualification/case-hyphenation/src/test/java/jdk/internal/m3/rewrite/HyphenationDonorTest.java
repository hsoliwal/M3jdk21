/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Test-only qualification of Apache Lucene 9.10.0, revision
 * 695c0ac84508438302cd346a812cfa2fdc5a10df. Donor notices: README.md.
 */
package jdk.internal.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.compound.hyphenation.Hyphenation;
import org.apache.lucene.analysis.compound.hyphenation.HyphenationTree;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterIterator;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

/** Explicit donor examples, not a language corpus or a project implementation oracle. */
class HyphenationDonorTest {
    private static final String CLASSES =
            "aA bB cC dD eE fF gG hH iI jJ kK lL mM nN oO pP qQ rR sS tT uU vV wW xX yY zZ";
    private static final Pattern TEX_BLOCKS = Pattern.compile(
            "\\A\\s*\\\\patterns\\s*\\{([^{}]*)}\\s*\\\\hyphenation\\s*\\{([^{}]*)}\\s*\\z");

    @Test
    void xmlLoadingCallsActualAddPatternWithParsedWeights() throws IOException {
        RecordingTree tree = new RecordingTree();
        tree.loadPatterns(new InputSource(new StringReader(xml("c1d bc4d e1f", ""))));

        // super.addPattern runs inside loadPatterns, while its temporary value table is live.
        assertEquals(List.of("cd=010", "bcd=0040", "ef=010"), tree.added);
        assertEquals("010", tree.findPattern("cd"));
        assertEquals("0040", tree.findPattern("bcd"));
        assertEquals("", tree.findPattern("absent"));
        assertPoints(tree.hyphenate("abcdef", 1, 1), 0, 5, 6);
    }

    @Test
    void maximumWeightDeterminesParityAtEachOverlappingPosition() throws IOException {
        assertPoints(load("c3d", "").hyphenate("abcdef", 1, 1), 0, 3, 6);
        assertNull(load("c3d bc4d", "").hyphenate("abcdef", 1, 1));
        assertPoints(load("c5d bc4d", "").hyphenate("abcdef", 1, 1), 0, 3, 6);
        // A high even weight suppresses only its own position, not another odd position.
        assertPoints(load("c3d bc4d e1f", "").hyphenate("abcdef", 1, 1), 0, 5, 6);
    }

    @Test
    void allSixInsertionOrdersOfDistinctOverlappingKeysHaveTheExplicitSameCuts()
            throws IOException {
        for (String patterns : List.of(
                "c5d bc4d e1f", "c5d e1f bc4d", "bc4d c5d e1f",
                "bc4d e1f c5d", "e1f c5d bc4d", "e1f bc4d c5d")) {
            Hyphenation result = load(patterns, "").hyphenate("abcdef", 1, 1);
            assertNotNull(result, patterns);
            assertArrayEquals(new int[] {0, 3, 5, 6}, result.getHyphenationPoints(), patterns);
        }
    }

    @Test
    void duplicatePatternKeysReplaceRatherThanOverlayTheirStoredWeights() throws IOException {
        HyphenationTree evenLast = load("c1d c2d", "");
        assertEquals("020", evenLast.findPattern("cd"));
        assertNull(evenLast.hyphenate("abcdef", 1, 1));
        HyphenationTree oddLast = load("c2d c1d", "");
        assertEquals("010", oddLast.findPattern("cd"));
        assertPoints(oddLast.hyphenate("abcdef", 1, 1), 0, 3, 6);
    }

    @Test
    void patternMinimaIncludeTheExactLeftAndRightBoundaries() throws IOException {
        HyphenationTree tree = load("b1c c1d d1e", "");
        assertPoints(tree.hyphenate("abcdef", 2, 2), 0, 2, 3, 4, 6);
        assertPoints(tree.hyphenate("abcdef", 3, 3), 0, 3, 6);
        assertPoints(tree.hyphenate("abcdef", 4, 2), 0, 4, 6);
        assertPoints(tree.hyphenate("abcdef", 2, 4), 0, 2, 6);
        assertNull(tree.hyphenate("abcdef", 4, 3));
    }

    @Test
    void dotPatternsAreAnchoredToWordBoundaries() throws IOException {
        HyphenationTree left = load(".ab1c", "");
        assertPoints(left.hyphenate("abcdef", 1, 1), 0, 2, 6);
        assertNull(left.hyphenate("zabcdef", 1, 1));
        HyphenationTree right = load("d1ef.", "");
        assertPoints(right.hyphenate("abcdef", 1, 1), 0, 4, 6);
        assertNull(right.hyphenate("abcdefz", 1, 1));
    }

    @Test
    void exceptionsReplacePatternCutsAndCanExplicitlyForbidAllBreaks() throws IOException {
        assertPoints(load("c1d e1f", "").hyphenate("abcdef", 1, 1), 0, 3, 5, 6);
        assertPoints(load("c1d e1f", "ab-cdef").hyphenate("abcdef", 1, 1), 0, 2, 6);
        assertNull(load("c1d e1f", "abcdef").hyphenate("abcdef", 1, 1));
    }

    @Test
    void simpleExceptionsHaveAnInclusiveLeftButStrictRightEndpoint() throws IOException {
        HyphenationTree pattern = load("c1d", "");
        HyphenationTree exception = load("c1d", "abc-def");
        assertPoints(pattern.hyphenate("abcdef", 3, 3), 0, 3, 6);
        assertNull(exception.hyphenate("abcdef", 3, 3));
        assertPoints(exception.hyphenate("abcdef", 3, 2), 0, 3, 6);
        assertNull(exception.hyphenate("abcdef", 4, 1));
        assertNull(load("", "abcde-f").hyphenate("abcdef", 1, 1));
        assertPoints(load("", "abcde-f").hyphenate("abcdef", 1, 0), 0, 5, 6);
    }

    @Test
    void noBreakIsNullIncludingEmptyTextAndProjectionUsesAnEmptyArray() throws IOException {
        HyphenationTree tree = load("c2d", "");
        assertNull(tree.hyphenate("abcdef", 1, 1));
        assertNull(tree.hyphenate("", 1, 1));
        assertNull(tree.hyphenate("", 0, 0));
        assertNull(tree.hyphenate("a", 1, 1));
        assertNull(load("c1d", "").hyphenate("ab#cdef", 1, 1));
        assertThrows(NullPointerException.class, () -> tree.hyphenate((String) null, 1, 1));
        assertArrayEquals(new int[0], projectInternal(tree.hyphenate("abcdef", 1, 1), 4, 10));
    }

    @Test
    void manualTokenSpanProjectsRelativeCutsToOriginalUtf16Offsets() throws IOException {
        String original = "\uD83D\uDE80::abcdef!TAIL";
        int tokenStart = 4;
        int tokenEnd = 10;
        assertEquals("abcdef", original.substring(tokenStart, tokenEnd));
        assertEquals(3, original.codePointCount(0, tokenStart));
        assertTrue(Character.isSurrogatePair(original.charAt(0), original.charAt(1)));

        HyphenationTree tree = load("b1c d1e", "");
        Hyphenation sliced = tree.hyphenate(
                original.toCharArray(), tokenStart, tokenEnd - tokenStart, 1, 1);
        assertPoints(sliced, 0, 2, 4, 6);
        assertPoints(tree.hyphenate("abcdef", 1, 1), 0, 2, 4, 6);
        assertArrayEquals(new int[] {6, 8}, projectInternal(sliced, tokenStart, tokenEnd));
        assertEquals("ab", original.substring(4, 6));
        assertEquals("cd", original.substring(6, 8));
        assertEquals("ef", original.substring(8, 10));
    }

    @Test
    void syntheticEndpointsAndZeroMinimumBoundaryCandidatesAreExcludedFromInternalCuts()
            throws IOException {
        Hyphenation result = load("c1d", "").hyphenate("abcdef", 1, 1);
        assertPoints(result, 0, 3, 6);
        assertArrayEquals(new int[] {7}, projectInternal(result, 4, 10));
        // With zero left minimum, the donor can also emit a genuine candidate at zero.
        Hyphenation zero = load("1a b1c", "").hyphenate("abcdef", 0, 1);
        assertPoints(zero, 0, 0, 2, 6);
        assertArrayEquals(new int[] {6}, projectInternal(zero, 4, 10));
    }

    @Test
    void upstream8124SinglePatternRegressionUsesAnExplicitlyReducedFixture() throws IOException {
        // Adapted from hyphenation-LUCENE-8124.xml: only its d7f pattern is reused.
        // Original fixture credits Frank Jensen and FOP adapter Carlos Villegas.
        // Our ASCII classes omit its Danish additions; this is not a Danish corpus test.
        HyphenationTree tree = load("d7f", "");
        assertPoints(tree.hyphenate("Rindfleisch", 2, 2), 0, 4, 11);
        assertPoints(tree.hyphenate("rindfleisch", 2, 2), 0, 4, 11);
        assertNull(tree.hyphenate("Rindfleisch", 5, 2));
    }

    @Test
    void actualIdentifierDonorSplitsCamelDigitsAndDelimitersButRetainsAcronymPrefix()
            throws IOException {
        assertIdentifier("PowerShot", List.of("Power", "Shot"),
                new int[] {0, 5}, new int[] {5, 9});
        // Lucene keeps XMLParser together. The project policy remains XML | Parser.
        assertIdentifier("XMLParser42_testCase", List.of("XMLParser", "42", "test", "Case"),
                new int[] {0, 9, 12, 16}, new int[] {9, 11, 16, 20});
    }

    @Test
    void pinnedEnglishExceptionsHaveExplicitCutsAtMinimaTwoAndThree() throws Exception {
        EnglishFixture fixture = englishFixture();
        assertEnglishException(fixture, "as-so-ciate", 0, 2, 4, 9);
        assertEnglishException(fixture, "as-so-ciates", 0, 2, 4, 10);
        assertEnglishException(fixture, "dec-li-na-tion", 0, 3, 5, 7, 11);
        assertEnglishException(fixture, "oblig-a-tory", 0, 5, 6, 10);
        assertEnglishException(fixture, "phil-an-thropic", 0, 4, 6, 13);
        assertEnglishException(fixture, "reci-procity", 0, 4, 11);
        assertTrue(fixture.exceptions().contains("ta-ble"));
        // The donor exception branch excludes equality at the right minimum: 5 - 3 == 2.
        assertNull(fixture.tree().hyphenate("table", 2, 3));
        for (String noBreak : List.of("present", "presents", "project", "projects")) {
            assertTrue(fixture.exceptions().contains(noBreak));
            assertNull(fixture.tree().hyphenate(noBreak, 2, 3), noBreak);
        }
    }

    @Test
    void pinnedEnglishExceptionMinimaCanRemoveOrRestoreSpecifiedCuts() throws Exception {
        HyphenationTree tree = englishFixture().tree();
        assertPoints(tree.hyphenate("associate", 3, 3), 0, 4, 9);
        assertNull(tree.hyphenate("associate", 5, 3));
        assertPoints(tree.hyphenate("associate", 2, 5), 0, 2, 9);
        assertNull(tree.hyphenate("associate", 2, 7));
        assertPoints(tree.hyphenate("declination", 2, 4), 0, 3, 5, 11);
        assertPoints(tree.hyphenate("table", 2, 2), 0, 2, 5);
    }

    @Test
    void pinnedEnglishExceptionsProjectFromManualUtf16SpanIncludingSurrogatePrefix()
            throws Exception {
        String original = "\uD83D\uDE80::associate/TAIL";
        int tokenStart = 4;
        int tokenEnd = 13;
        assertEquals("associate", original.substring(tokenStart, tokenEnd));
        assertEquals(3, original.codePointCount(0, tokenStart));
        Hyphenation result = englishFixture().tree().hyphenate(
                original.toCharArray(), tokenStart, tokenEnd - tokenStart, 2, 3);
        assertPoints(result, 0, 2, 4, 9);
        assertArrayEquals(new int[] {6, 8}, projectInternal(result, tokenStart, tokenEnd));
        assertEquals("as", original.substring(4, 6));
        assertEquals("so", original.substring(6, 8));
        assertEquals("ciate", original.substring(8, 13));
    }

    @Test
    void restrictedTexLoaderRejectsExtraBlocksMacrosAndMalformedTokens() throws IOException {
        EnglishFixture example = parseEnglishFixture(
                "% ignored \\patterns{invalid}\n\\patterns{c1d%comment\n e1f}\n"
                        + "\\hyphenation{ab-cdef present}");
        assertEquals(2, example.patternCount());
        assertEquals(List.of("ab-cdef", "present"), example.exceptions());
        assertPoints(example.tree().hyphenate("abcdef", 1, 1), 0, 2, 6);
        for (String bad : List.of(
                "\\patterns{c1d}",
                "\\patterns{c1d}\\patterns{e1f}\\hyphenation{ab-cdef}",
                "\\patterns{{c1d}}\\hyphenation{ab-cdef}",
                "\\patterns{c1d}\\hyphenation{ab-cdef}\\input{other}",
                "\\patterns{c12d}\\hyphenation{ab-cdef}",
                "\\patterns{c1d}\\hyphenation{ab--cdef}")) {
            assertThrows(IllegalArgumentException.class, () -> parseEnglishFixture(bad), bad);
        }
    }

    private static EnglishFixture englishFixture() throws Exception {
        Path caseRoot = Path.of(System.getProperty("m3.case.root",
                "F:/codex-work/lexicon-migration-20261031/case-precompute"));
        Path source = caseRoot.resolve("hyphenation-tests/donor-input/hyph-en-us.tex.txt");
        assertEquals(34128L, Files.size(source));
        byte[] bytes = Files.readAllBytes(source);
        assertEquals(34128, bytes.length);
        MessageDigest gitBlob = MessageDigest.getInstance("SHA-1");
        gitBlob.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.US_ASCII));
        assertEquals("bd9b757d89074e3e11b590311ce13ba6667544aa",
                HexFormat.of().formatHex(gitBlob.digest(bytes)));
        assertEquals("f4ffcd96c5cbc886bdad23f95dcae8edc3cd3620eae62f7946eceda97c4e68f8",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        EnglishFixture fixture = parseEnglishFixture(new String(bytes, StandardCharsets.US_ASCII));
        assertEquals(4938, fixture.patternCount());
        assertEquals(14, fixture.exceptions().size());
        return fixture;
    }

    private static EnglishFixture parseEnglishFixture(String tex) throws IOException {
        // Restricted grammar for this pinned ASCII file, not a TeX interpreter.
        // TeX percent comments also suppress their terminating line break.
        String uncommented = tex.replaceAll("%[^\\r\\n]*(?:\\r\\n|\\r|\\n|\\z)", "");
        Matcher blocks = TEX_BLOCKS.matcher(uncommented);
        if (!blocks.matches()) {
            throw new IllegalArgumentException("Expected exactly one patterns then one hyphenation block");
        }
        String[] patterns = blocks.group(1).strip().split("\\s+");
        String[] exceptions = blocks.group(2).strip().split("\\s+");
        for (String item : patterns) {
            if (!item.matches("[0-9]?(?:[a-z.][0-9]?)+")) {
                throw new IllegalArgumentException("Unsupported pattern item: " + item);
            }
        }
        for (String item : exceptions) {
            if (!item.matches("[a-z]+(?:-[a-z]+)*")) {
                throw new IllegalArgumentException("Unsupported exception item: " + item);
            }
        }
        return new EnglishFixture(load(String.join(" ", patterns), String.join(" ", exceptions)),
                List.of(exceptions), patterns.length);
    }

    private static void assertEnglishException(EnglishFixture fixture, String item, int... points) {
        assertTrue(fixture.exceptions().contains(item), item);
        assertPoints(fixture.tree().hyphenate(item.replace("-", ""), 2, 3), points);
    }

    private record EnglishFixture(HyphenationTree tree, List<String> exceptions, int patternCount) { }

    private static HyphenationTree load(String patterns, String exceptions) throws IOException {
        HyphenationTree tree = new HyphenationTree();
        tree.loadPatterns(new InputSource(new StringReader(xml(patterns, exceptions))));
        return tree;
    }

    private static String xml(String patterns, String exceptions) {
        // Constants supplied by these tests only; no DTD, external entity, file, or network I/O.
        return "<hyphenation-info><hyphen-char value=\"-\"/><classes>" + CLASSES
                + "</classes><patterns>" + patterns + "</patterns><exceptions>"
                + exceptions + "</exceptions></hyphenation-info>";
    }

    private static void assertPoints(Hyphenation result, int... expected) {
        assertNotNull(result);
        assertEquals(expected.length, result.length());
        assertArrayEquals(expected, result.getHyphenationPoints());
    }

    /** Test-local coordinate adapter for a manually supplied, unmodified letter-only item span. */
    private static int[] projectInternal(Hyphenation result, int tokenStart, int tokenEnd) {
        assertTrue(tokenStart >= 0 && tokenEnd >= tokenStart);
        if (result == null) {
            return new int[0];
        }
        int[] raw = result.getHyphenationPoints();
        assertTrue(raw.length >= 2);
        return Arrays.stream(raw, 1, raw.length - 1)
                .filter(point -> point > 0 && point < tokenEnd - tokenStart)
                .map(point -> tokenStart + point)
                .toArray();
    }

    private static void assertIdentifier(String text, List<String> expectedTerms,
            int[] expectedStarts, int[] expectedEnds) throws IOException {
        int flags = WordDelimiterGraphFilter.GENERATE_WORD_PARTS
                | WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS
                | WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE
                | WordDelimiterGraphFilter.SPLIT_ON_NUMERICS;
        KeywordTokenizer input = new KeywordTokenizer();
        input.setReader(new StringReader(text));
        try (WordDelimiterGraphFilter stream = new WordDelimiterGraphFilter(
                input, true, WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE, flags, null)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            OffsetAttribute offset = stream.addAttribute(OffsetAttribute.class);
            List<String> terms = new ArrayList<>();
            List<Integer> starts = new ArrayList<>();
            List<Integer> ends = new ArrayList<>();
            stream.reset();
            while (stream.incrementToken()) {
                terms.add(term.toString());
                starts.add(offset.startOffset());
                ends.add(offset.endOffset());
            }
            assertFalse(stream.incrementToken());
            stream.end();
            assertEquals(expectedTerms, terms);
            assertArrayEquals(expectedStarts, starts.stream().mapToInt(Integer::intValue).toArray());
            assertArrayEquals(expectedEnds, ends.stream().mapToInt(Integer::intValue).toArray());
            assertEquals(text.length(), offset.endOffset());
        }
    }

    @SuppressWarnings("serial")
    private static final class RecordingTree extends HyphenationTree {
        private final List<String> added = new ArrayList<>();

        @Override
        public void addPattern(String pattern, String weights) {
            added.add(pattern + "=" + weights);
            super.addPattern(pattern, weights);
        }
    }
}
