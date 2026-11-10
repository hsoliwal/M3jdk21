/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.contract;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for StringFormatUtils.
 *
 * <p>
 * Tests string padding, spacing, normalization, and truncation.
 */
@DisplayName("StringFormatUtils Tests")
class StringFormatUtilsTest {

    @Nested
    @DisplayName("Padding Tests")
    class PaddingTests {

        @Test
        @DisplayName("Pad right with shorter string")
        void testPadRightShorter() {
            assertEquals("hello     ", StringFormatUtils.padRight("hello", 10));
        }

        @Test
        @DisplayName("Pad right with equal length")
        void testPadRightEqual() {
            assertEquals("hello", StringFormatUtils.padRight("hello", 5));
        }

        @Test
        @DisplayName("Pad right with longer string")
        void testPadRightLonger() {
            assertEquals("helloworld", StringFormatUtils.padRight("helloworld", 5));
        }

        @Test
        @DisplayName("Pad right with null")
        void testPadRightNull() {
            assertEquals("     ", StringFormatUtils.padRight(null, 5));
        }

        @Test
        @DisplayName("Pad left with shorter string")
        void testPadLeftShorter() {
            assertEquals("     hello", StringFormatUtils.padLeft("hello", 10));
        }

        @Test
        @DisplayName("Pad left with equal length")
        void testPadLeftEqual() {
            assertEquals("hello", StringFormatUtils.padLeft("hello", 5));
        }

        @Test
        @DisplayName("Pad left with null")
        void testPadLeftNull() {
            assertEquals("     ", StringFormatUtils.padLeft(null, 5));
        }

        @Test
        @DisplayName("Pad center with shorter string")
        void testPadCenter() {
            final String result = StringFormatUtils.padCenter("hi", 6);
            assertEquals(6, result.length());
            assertTrue(result.contains("hi"));
        }

        @Test
        @DisplayName("Pad center with null")
        void testPadCenterNull() {
            assertEquals("     ", StringFormatUtils.padCenter(null, 5));
        }
    }

    @Nested
    @DisplayName("Space Utilities Tests")
    class SpaceUtilitiesTests {

        @ParameterizedTest
        @CsvSource({"0, ''", "1, ' '", "5, '     '", "10, '          '"})
        @DisplayName("Generate spaces")
        void testSpaces(final int count, final String expected) {
            assertEquals(expected, StringFormatUtils.spaces(count));
        }

        @Test
        @DisplayName("Spaces with negative count returns empty")
        void testSpacesNegative() {
            assertEquals("", StringFormatUtils.spaces(-5));
        }

        @ParameterizedTest
        @CsvSource({"a, 0, ''", "a, 1, a", "a, 5, aaaaa", "-, 3, ---"})
        @DisplayName("Repeat character")
        void testRepeat(final char character, final int count, final String expected) {
            assertEquals(expected, StringFormatUtils.repeat(character, count));
        }

        @Test
        @DisplayName("Repeat with negative count returns empty")
        void testRepeatNegative() {
            assertEquals("", StringFormatUtils.repeat('a', -5));
        }

        @Test
        @DisplayName("Line with default character")
        void testLine() {
            final String line = StringFormatUtils.line(10);
            assertEquals(10, line.length());
        }

        @ParameterizedTest
        @CsvSource({"5, -, -----", "3, =, ===", "8, *, ********"})
        @DisplayName("Line with custom character")
        void testLineWithCharacter(final int width, final char character, final String expected) {
            assertEquals(expected, StringFormatUtils.line(width, character));
        }
    }

    @Nested
    @DisplayName("Normalization Tests")
    class NormalizationTests {

        @Test
        @DisplayName("Normalize whitespace")
        void testNormalizeWhitespace() {
            assertEquals(Optional.of("hello world test"), StringFormatUtils.normalizeWhitespace("hello   world\n\ttest"));
        }

        @Test
        @DisplayName("Normalize whitespace trims")
        void testNormalizeWhitespaceTrim() {
            assertEquals(Optional.of("hello world"), StringFormatUtils.normalizeWhitespace("  hello   world  "));
        }

        @Test
        @DisplayName("Normalize null returns null")
        void testNormalizeWhitespaceNull() {
            assertEquals(Optional.empty(), StringFormatUtils.normalizeWhitespace(null));
        }

        @Test
        @DisplayName("Remove whitespace")
        void testRemoveWhitespace() {
            assertEquals(Optional.of("helloworld"), StringFormatUtils.removeWhitespace("hello   world\n\t"));
        }

        @Test
        @DisplayName("Remove whitespace from null returns null")
        void testRemoveWhitespaceNull() {
            assertEquals(Optional.empty(), StringFormatUtils.removeWhitespace(null));
        }
    }

    @Nested
    @DisplayName("Truncation Tests")
    class TruncationTests {

        @Test
        @DisplayName("Truncate longer string with ellipsis")
        void testTruncate() {
            assertEquals("hello...", StringFormatUtils.truncate("helloworld", 8));
        }

        @Test
        @DisplayName("Truncate shorter string returns original")
        void testTruncateShorter() {
            assertEquals("hello", StringFormatUtils.truncate("hello", 10));
        }

        @Test
        @DisplayName("Truncate null returns null")
        void testTruncateNull() {
            assertNull(StringFormatUtils.truncate(null, 10));
        }

        @Test
        @DisplayName("Truncate with very small max length")
        void testTruncateSmallLength() {
            assertEquals("...", StringFormatUtils.truncate("hello", 3));
        }

        @Test
        @DisplayName("Truncate plain longer string")
        void testTruncatePlain() {
            assertEquals("hello", StringFormatUtils.truncatePlain("helloworld", 5));
        }

        @Test
        @DisplayName("Truncate plain shorter string returns original")
        void testTruncatePlainShorter() {
            assertEquals("hello", StringFormatUtils.truncatePlain("hello", 10));
        }

        @Test
        @DisplayName("Truncate plain null returns null")
        void testTruncatePlainNull() {
            assertNull(StringFormatUtils.truncatePlain(null, 10));
        }
    }

    @Nested
    @DisplayName("Box Drawing Tests")
    class BoxDrawingTests {

        @Test
        @DisplayName("Box line with minimum width")
        void testBoxLineMinimum() {
            assertEquals("++", StringFormatUtils.boxLine(1, '+', '-', '+'));
        }

        @Test
        @DisplayName("Box line with normal width")
        void testBoxLineNormal() {
            assertEquals("+-----+", StringFormatUtils.boxLine(7, '+', '-', '+'));
        }

        @Test
        @DisplayName("Box line with different characters")
        void testBoxLineCustom() {
            assertEquals("[===]", StringFormatUtils.boxLine(5, '[', '=', ']'));
        }

        @Test
        @DisplayName("Box cell with content")
        void testBoxCell() {
            final String cell = StringFormatUtils.boxCell("hello", 10, '|', '|');
            assertTrue(cell.startsWith("|"));
            assertTrue(cell.endsWith("|"));
            assertTrue(cell.contains("hello"));
            assertEquals(10, cell.length());
        }

        @Test
        @DisplayName("Box cell pads content")
        void testBoxCellPadding() {
            final String cell = StringFormatUtils.boxCell("hi", 8, '|', '|');
            assertEquals(8, cell.length());
            assertTrue(cell.contains("hi"));
        }
    }
}
