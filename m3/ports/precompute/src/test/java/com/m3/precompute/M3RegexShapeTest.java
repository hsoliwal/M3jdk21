// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class M3RegexShapeTest {
    @Test
    void classifiesOnlyProvableLiteralShapes() {
        assertShape("abc", M3RegexShape.Kind.LITERAL, "abc");
        assertShape("\\Aabc", M3RegexShape.Kind.STRICT_PREFIX, "abc");
        assertShape("abc\\z", M3RegexShape.Kind.STRICT_SUFFIX, "abc");
        assertShape("\\Aabc\\z", M3RegexShape.Kind.STRICT_EXACT, "abc");
        assertShape("a\\.b", M3RegexShape.Kind.LITERAL, "a.b");
        assertShape("\\Qa.*[b]\\E", M3RegexShape.Kind.LITERAL, "a.*[b]");
        assertShape("\ud83d\ude42", M3RegexShape.Kind.LITERAL, "\ud83d\ude42");
    }

    @Test
    void refusesRegexSyntaxControlEscapesAndBrokenSurrogates() {
        for (String expression :
                new String[] {
                    ".", "a+", "(abc)", "[abc]", "^abc$", "\\d+", "\\s",
                    "\\u0061", "\\x61", "\\Qabc", "\ud83d", "\ude42"
                }) {
            M3RegexShape shape = M3RegexShape.analyze(expression);
            assertEquals(M3RegexShape.Kind.GENERAL, shape.kind(), expression);
            assertFalse(shape.specialized(), expression);
            assertThrows(IllegalStateException.class, shape::literal);
        }
    }

    private static void assertShape(
            String expression,
            M3RegexShape.Kind expectedKind,
            String expectedLiteral) {
        M3RegexShape shape = M3RegexShape.analyze(expression);
        assertEquals(expectedKind, shape.kind(), expression);
        assertTrue(shape.specialized(), expression);
        assertEquals(expectedLiteral, shape.literal(), expression);
        assertEquals(expression, shape.expression());
    }
}
