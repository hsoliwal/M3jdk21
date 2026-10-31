// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class M3RegexStringTrialImageTest {
    @Test
    void finiteMatrixMatchesJdkAndRanksHardCasesDeterministically() {
        List<M3RegexStringTrialImage.RegexSpec> regexes = regexes();
        List<String> strings = strings();
        var image = M3RegexStringTrialImage.compile(regexes, strings, null);

        assertEquals(regexes.size(), image.regexCount());
        assertEquals(strings.size(), image.stringCount());
        assertEquals(regexes.size() * strings.size(), image.pairCount());
        assertTrue(image.rootHash().matches("[0-9a-f]{64}"));
        assertTrue(image.primitivePayloadBytes() > 0);

        for (int regexRow = 0; regexRow < regexes.size(); regexRow++) {
            Pattern pattern = null;
            try {
                pattern = Pattern.compile(
                        regexes.get(regexRow).expression(),
                        regexes.get(regexRow).flags());
                assertTrue(image.regexValid(regexRow));
            } catch (java.util.regex.PatternSyntaxException invalid) {
                assertFalse(image.regexValid(regexRow));
            }

            for (int stringRow = 0; stringRow < strings.size(); stringRow++) {
                assertTrue(image.simHashDistance(regexRow, stringRow) <= 64);
                assertTrue(image.estimatedJaccard(regexRow, stringRow) >= 0.0);
                assertTrue(image.estimatedJaccard(regexRow, stringRow) <= 1.0);
                assertTrue(image.difficultyScore(regexRow, stringRow) >= 0);

                if (pattern == null) {
                    int r = regexRow;
                    int s = stringRow;
                    assertThrows(IllegalStateException.class, () -> image.find(r, s));
                    continue;
                }

                Matcher matcher = pattern.matcher(strings.get(stringRow));
                boolean matches = matcher.matches();
                matcher.reset();
                boolean lookingAt = matcher.lookingAt();
                matcher.reset();
                boolean find = matcher.find();

                assertEquals(matches, image.matches(regexRow, stringRow));
                assertEquals(lookingAt, image.lookingAt(regexRow, stringRow));
                assertEquals(find, image.find(regexRow, stringRow));
            }
        }

        int[] topRegexes = image.topRegexRows(image.regexCount());
        assertEquals(image.regexCount(), topRegexes.length);
        for (int index = 1; index < topRegexes.length; index++) {
            int previous = topRegexes[index - 1];
            int current = topRegexes[index];
            assertTrue(image.regexDifficultyScore(previous) >= image.regexDifficultyScore(current));
            if (image.regexDifficultyScore(previous) == image.regexDifficultyScore(current)) {
                assertTrue(previous < current);
            }
        }

        int[] topPairs = image.topPairs(24);
        for (int index = 1; index < topPairs.length; index++) {
            int previous = topPairs[index - 1];
            int current = topPairs[index];
            int previousScore = image.difficultyScore(
                    image.regexRowOfPair(previous), image.stringRowOfPair(previous));
            int currentScore = image.difficultyScore(
                    image.regexRowOfPair(current), image.stringRowOfPair(current));
            assertTrue(previousScore >= currentScore);
            if (previousScore == currentScore) assertTrue(previous < current);
        }
    }

    @Test
    void inputOrderAndProviderArePartOfStableTrialSemantics() {
        List<M3RegexStringTrialImage.RegexSpec> regexes = regexes();
        List<String> strings = strings();
        var first = M3RegexStringTrialImage.compile(regexes, strings, null);
        var second = M3RegexStringTrialImage.compile(regexes, strings, null);
        assertEquals(first.rootHash(), second.rootHash());
        assertArrayEquals(first.topPairs(32), second.topPairs(32));

        ArrayList<String> reversed = new ArrayList<>(strings);
        java.util.Collections.reverse(reversed);
        var reordered = M3RegexStringTrialImage.compile(regexes, reversed, null);
        assertNotEquals(first.rootHash(), reordered.rootHash());
    }

    @Test
    void existingJniCodeTextProviderBuildsTheSameTrialImageWhenEnabled() {
        String path = System.getProperty("m3.precompute.native.path");
        String pathFile = System.getProperty("m3.precompute.native.pathFile");
        if ((path == null || path.isBlank()) && (pathFile == null || pathFile.isBlank())) return;

        List<M3RegexStringTrialImage.RegexSpec> regexes = regexes();
        List<String> strings = strings();
        var javaImage =
                M3RegexStringTrialImage.compile(
                        regexes,
                        strings,
                        M3CodeTextSignalBatchJava.INSTANCE,
                        M3RegexStringTrialImage.Limits.DEFAULT,
                        null);
        var nativeImage =
                M3RegexStringTrialImage.compile(
                        regexes,
                        strings,
                        M3CodeTextSignalBatchNative.loadRequired(),
                        M3RegexStringTrialImage.Limits.DEFAULT,
                        null);

        assertEquals(javaImage.rootHash(), nativeImage.rootHash());
        assertArrayEquals(javaImage.topPairs(64), nativeImage.topPairs(64));
    }

    @Test
    void hardBudgetsFailBeforeCartesianAllocation() {
        List<M3RegexStringTrialImage.RegexSpec> regexes =
                List.of(
                        new M3RegexStringTrialImage.RegexSpec("a"),
                        new M3RegexStringTrialImage.RegexSpec("b"));
        List<String> strings = List.of("a", "b", "c", "d", "e", "f");
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3RegexStringTrialImage.compile(
                                regexes,
                                strings,
                                M3CodeTextSignalBatchJava.INSTANCE,
                                new M3RegexStringTrialImage.Limits(2, 6, 11, 100, 10_000),
                                null));
    }

    private static List<M3RegexStringTrialImage.RegexSpec> regexes() {
        return List.of(
                new M3RegexStringTrialImage.RegexSpec(""),
                new M3RegexStringTrialImage.RegexSpec("while"),
                new M3RegexStringTrialImage.RegexSpec("\\Awhile"),
                new M3RegexStringTrialImage.RegexSpec("while\\z"),
                new M3RegexStringTrialImage.RegexSpec("\\Awhile\\z"),
                new M3RegexStringTrialImage.RegexSpec("\\Qwhile(value!=0)\\E"),
                new M3RegexStringTrialImage.RegexSpec("[a-z]+\\d{2}"),
                new M3RegexStringTrialImage.RegexSpec("(while|for)"),
                new M3RegexStringTrialImage.RegexSpec("(?=while)while"),
                new M3RegexStringTrialImage.RegexSpec("\\bwhile\\b"),
                new M3RegexStringTrialImage.RegexSpec(".*"),
                new M3RegexStringTrialImage.RegexSpec("["),
                new M3RegexStringTrialImage.RegexSpec("while", Pattern.CASE_INSENSITIVE),
                new M3RegexStringTrialImage.RegexSpec("^while$", Pattern.MULTILINE),
                new M3RegexStringTrialImage.RegexSpec("a.b", Pattern.DOTALL),
                new M3RegexStringTrialImage.RegexSpec("while\\(value!=0\\).*count\\+\\+"));
    }

    private static List<String> strings() {
        return List.of(
                "",
                "while",
                "WHILE",
                "xwhile",
                "whilex",
                "for",
                "foo42",
                "abc99",
                "while(value!=0)",
                "while(value!=0){value&=value-1;count++;}",
                "if (x > 0) { return x; }",
                "^[a-z]+\\d{2}$",
                "a\nb",
                "while\nwhile",
                "λwhile",
                "class Fake { int count(int value) { return value; } }");
    }
}
