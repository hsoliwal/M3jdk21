// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Each test is an independent contract atom; JNI parity is exercised by verify.sh. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/recipe-crates/m3-utf16-wildcard-glob-20261105/src/test/java/com/synexia/m3/proof/Utf16WildcardOracleTest.java blob e1fa77b626b5; recipe m3/ports/precompute/recipe/utf16-atoms-20261106/relocate.py v1
final class M3Utf16GlobTest {
    @Test void leetcodeAndGfgFullMatchExamples() {
        assertEquals(false, M3Utf16Glob.match("aa".toCharArray(), "a".toCharArray()));
        assertTrue(M3Utf16Glob.match("aa".toCharArray(), "*".toCharArray()));
        assertEquals(false, M3Utf16Glob.match("cb".toCharArray(), "?a".toCharArray()));
        assertTrue(M3Utf16Glob.match("adceb".toCharArray(), "*a*b".toCharArray()));
        assertEquals(false, M3Utf16Glob.match("acdcb".toCharArray(), "a*c?b".toCharArray()));
        assertTrue(M3Utf16Glob.match("baaabab".toCharArray(), "ba*a?".toCharArray()));
    }

    @Test void emptyAndUtf16UnitSemantics() {
        assertTrue(M3Utf16Glob.match(new char[0], new char[0]));
        assertTrue(M3Utf16Glob.match(new char[0], "**".toCharArray()));
        assertEquals(false, M3Utf16Glob.match(new char[0], "?".toCharArray()));
        assertTrue(M3Utf16Glob.match(new char[]{'\ud800'}, new char[]{'?'}));
        assertEquals(false, M3Utf16Glob.match(new char[]{'\ud800', '\udc00'}, new char[]{'?'}));
        assertTrue(M3Utf16Glob.match(new char[]{'\ud800', '\udc00'}, new char[]{'?', '?'}));
        assertTrue(M3Utf16Glob.match(new char[]{0, '*'}, new char[]{'?', '?'}));
        assertTrue(M3Utf16Glob.match(new char[]{'*'}, new char[]{'*'}));
    }

    @Test void boundRefusalsAndNoInputMutation() {
        assertThrows(NullPointerException.class, () -> M3Utf16Glob.match(null, new char[0]));
        assertThrows(NullPointerException.class, () -> M3Utf16Glob.match(new char[0], null));
        assertThrows(IllegalArgumentException.class,
                () -> M3Utf16Glob.match(new char[M3Utf16Glob.MAX_TEXT + 1], new char[0]));
        assertThrows(IllegalArgumentException.class,
                () -> M3Utf16Glob.match(new char[0], new char[M3Utf16Glob.MAX_PATTERN + 1]));
        char[] text = new char[]{0, '\ud800', '*', '?', '\uffff'};
        char[] pattern = new char[]{'*', '?', '*'};
        char[] textCopy = text.clone(), patternCopy = pattern.clone();
        M3Utf16Glob.match(text, pattern);
        assertTrue(Arrays.equals(textCopy, text));
        assertTrue(Arrays.equals(patternCopy, pattern));
    }

    @Test void independentDpDifferentialAcrossShortInputs() {
        char[] sourceSymbols = new char[]{0, 'a', '\ud800'};
        char[] patternSymbols = new char[]{0, 'a', '?', '*'};
        for (int length = 0; length <= 3; length++) {
            int sourceCount = (int) Math.pow(sourceSymbols.length, length);
            for (int sourceId = 0; sourceId < sourceCount; sourceId++) {
                char[] source = decode(sourceId, length, sourceSymbols);
                for (int pLen = 0; pLen <= 4; pLen++) {
                    int patternCount = (int) Math.pow(patternSymbols.length, pLen);
                    for (int patternId = 0; patternId < patternCount; patternId++) {
                        char[] pattern = decode(patternId, pLen, patternSymbols);
                        assertEquals(M3Utf16GlobOracle.reference(source, pattern),
                                M3Utf16Glob.match(source, pattern));
                    }
                }
            }
        }
    }

    private static char[] decode(int id, int size, char[] alphabet) {
        char[] result = new char[size];
        for (int i = 0; i < size; i++) {
            result[i] = alphabet[id % alphabet.length];
            id /= alphabet.length;
        }
        return result;
    }
}
