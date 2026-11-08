// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RegexShapeTest {
    @Test
    void classifiesOnlyProvableLiteralShapes() {
        assertShape("abc", RegexShape.Kind.LITERAL, "abc");
        assertShape("\\Aabc", RegexShape.Kind.STRICT_PREFIX, "abc");
        assertShape("abc\\z", RegexShape.Kind.STRICT_SUFFIX, "abc");
        assertShape("\\Aabc\\z", RegexShape.Kind.STRICT_EXACT, "abc");
        assertShape("a\\.b", RegexShape.Kind.LITERAL, "a.b");
        assertShape("\\Qa.*[b]\\E", RegexShape.Kind.LITERAL, "a.*[b]");
        assertShape("\ud83d\ude42", RegexShape.Kind.LITERAL, "\ud83d\ude42");
    }

    @Test
    void refusesRegexSyntaxControlEscapesAndBrokenSurrogates() {
        for (String expression :
                new String[] {
                    "", "\\A\\z", "\\Q\\E",
                    ".", "a+", "(abc)", "[abc]", "^abc$", "\\d+", "\\s",
                    "\\u0061", "\\x61", "\\Qabc", "\ud83d", "\ude42"
                }) {
            RegexShape shape = RegexShape.analyze(expression);
            assertEquals(RegexShape.Kind.GENERAL, shape.kind(), expression);
            assertFalse(shape.specialized(), expression);
            assertThrows(IllegalStateException.class, shape::literal);
        }
    }

    private static void assertShape(
            String expression,
            RegexShape.Kind expectedKind,
            String expectedLiteral) {
        RegexShape shape = RegexShape.analyze(expression);
        assertEquals(expectedKind, shape.kind(), expression);
        assertTrue(shape.specialized(), expression);
        assertEquals(expectedLiteral, shape.literal(), expression);
        assertEquals(expression, shape.expression());
    }
}
