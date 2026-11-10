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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for ParsingUtils.
 *
 * <p>
 * Tests course code parsing, string splitting, and numeric parsing.
 */
@DisplayName("ParsingUtils Tests")
class ParsingUtilsTest {

    @Nested
    @DisplayName("Course Number Parsing Tests")
    class CourseNumberParsingTests {

        @ParameterizedTest
        @CsvSource({"18.01, 18", "8, 8", "6.042, 6", "18.01.5, 18", "24.900, 24"})
        @DisplayName("Parse valid course codes")
        void testParseValidCourseNumber(final String courseCode, final int expected) {
            assertEquals(expected, ParsingUtils.parseCourseNumber(courseCode));
        }

        @Test
        @DisplayName("Parse course code with null throws NPE")
        void testParseNullCourseCodeThrows() {
            assertThrows(NullPointerException.class, () -> ParsingUtils.parseCourseNumber(null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "abc", "MIT-18"})
        @DisplayName("Parse invalid course code throws NumberFormatException")
        void testParseInvalidCourseCodeThrows(final String courseCode) {
            assertThrows(NumberFormatException.class, () -> ParsingUtils.parseCourseNumber(courseCode));
        }

        @ParameterizedTest
        @CsvSource({"18.01, 18", "8, 8", "6.042, 6"})
        @DisplayName("Safe parse valid course codes")
        void testParseCourseNumberSafe(final String courseCode, final int expected) {
            final Optional<Integer> result = ParsingUtils.parseCourseNumberSafe(courseCode);
            assertTrue(result.isPresent());
            assertEquals(expected, result.get());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"abc", "MIT-18"})
        @DisplayName("Safe parse invalid course codes returns empty")
        void testParseCourseNumberSafeInvalid(final String courseCode) {
            final Optional<Integer> result = ParsingUtils.parseCourseNumberSafe(courseCode);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Generic Parsing Tests")
    class GenericParsingTests {

        @Test
        @DisplayName("Get first part with valid string")
        void testGetFirstPartValid() {
            assertEquals("hello", ParsingUtils.getFirstPart("hello.world", "\\.", "default"));
        }

        @Test
        @DisplayName("Get first part with no delimiter")
        void testGetFirstPartNoDelimiter() {
            assertEquals("hello", ParsingUtils.getFirstPart("hello", "\\.", "default"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Get first part with null or empty returns default")
        void testGetFirstPartNullOrEmpty(final String str) {
            assertEquals("default", ParsingUtils.getFirstPart(str, "\\.", "default"));
        }

        @ParameterizedTest
        @CsvSource({"a.b.c, \\., 0, a", "a.b.c, \\., 1, b", "a.b.c, \\., 2, c"})
        @DisplayName("Get part by index")
        void testGetPart(final String str, final String delimiter, final int index, final String expected) {
            final Optional<String> result = ParsingUtils.getPart(str, delimiter, index);
            assertTrue(result.isPresent());
            assertEquals(expected, result.get());
        }

        @Test
        @DisplayName("Get part with out of bounds index")
        void testGetPartOutOfBounds() {
            final Optional<String> result = ParsingUtils.getPart("a.b", "\\.", 5);
            assertTrue(result.isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Get part with null or empty returns empty")
        void testGetPartNullOrEmpty(final String str) {
            final Optional<String> result = ParsingUtils.getPart(str, "\\.", 0);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Extract Between Tests")
    class ExtractBetweenTests {

        @Test
        @DisplayName("Extract text between delimiters")
        void testExtractBetween() {
            final Optional<String> result = ParsingUtils.extractBetween("hello [world] test", "[", "]");
            assertTrue(result.isPresent());
            assertEquals("world", result.get());
        }

        @Test
        @DisplayName("Extract with missing start delimiter")
        void testExtractBetweenMissingStart() {
            final Optional<String> result = ParsingUtils.extractBetween("hello world test", "[", "]");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Extract with missing end delimiter")
        void testExtractBetweenMissingEnd() {
            final Optional<String> result = ParsingUtils.extractBetween("hello [world test", "[", "]");
            assertTrue(result.isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Extract from null or empty returns empty")
        void testExtractBetweenNullOrEmpty(final String str) {
            final Optional<String> result = ParsingUtils.extractBetween(str, "[", "]");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Integer Parsing Tests")
    class IntegerParsingTests {

        @ParameterizedTest
        @CsvSource({"123, 123", "-456, -456", "0, 0"})
        @DisplayName("Parse valid integers with default")
        void testParseIntValid(final String str, final int expected) {
            assertEquals(expected, ParsingUtils.parseInt(str, 999));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"abc", "12.34", ""})
        @DisplayName("Parse invalid integers returns default")
        void testParseIntInvalid(final String str) {
            assertEquals(999, ParsingUtils.parseInt(str, 999));
        }

        @ParameterizedTest
        @CsvSource({"123, 123", "-456, -456"})
        @DisplayName("Safe parse valid integers")
        void testParseIntSafeValid(final String str, final int expected) {
            final Optional<Integer> result = ParsingUtils.parseIntSafe(str);
            assertTrue(result.isPresent());
            assertEquals(expected, result.get());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"abc", "12.34"})
        @DisplayName("Safe parse invalid integers returns empty")
        void testParseIntSafeInvalid(final String str) {
            final Optional<Integer> result = ParsingUtils.parseIntSafe(str);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Double Parsing Tests")
    class DoubleParsingTests {

        @ParameterizedTest
        @CsvSource({"123.45, 123.45", "-456.78, -456.78", "0.0, 0.0", "123, 123.0"})
        @DisplayName("Parse valid doubles with default")
        void testParseDoubleValid(final String str, final double expected) {
            assertEquals(expected, ParsingUtils.parseDouble(str, 999.0), 0.001);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"abc", ""})
        @DisplayName("Parse invalid doubles returns default")
        void testParseDoubleInvalid(final String str) {
            assertEquals(999.0, ParsingUtils.parseDouble(str, 999.0), 0.001);
        }

        @ParameterizedTest
        @CsvSource({"123.45, 123.45", "-456.78, -456.78"})
        @DisplayName("Safe parse valid doubles")
        void testParseDoubleSafeValid(final String str, final double expected) {
            final Optional<Double> result = ParsingUtils.parseDoubleSafe(str);
            assertTrue(result.isPresent());
            assertEquals(expected, result.get(), 0.001);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"abc"})
        @DisplayName("Safe parse invalid doubles returns empty")
        void testParseDoubleSafeInvalid(final String str) {
            final Optional<Double> result = ParsingUtils.parseDoubleSafe(str);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Boolean Parsing Tests")
    class BooleanParsingTests {

        @ParameterizedTest
        @ValueSource(strings = {"true", "TRUE", "True", "yes", "YES", "Yes", "1", "on", "ON", "On"})
        @DisplayName("Parse truthy strings")
        void testParseBooleanTrue(final String str) {
            assertTrue(ParsingUtils.parseBoolean(str));
        }

        @ParameterizedTest
        @ValueSource(strings = {"false", "no", "0", "off", "abc", ""})
        @DisplayName("Parse falsy strings")
        void testParseBooleanFalse(final String str) {
            assertFalse(ParsingUtils.parseBoolean(str));
        }

        @Test
        @DisplayName("Parse null boolean returns false")
        void testParseBooleanNull() {
            assertFalse(ParsingUtils.parseBoolean(null));
        }

        @Test
        @DisplayName("Parse empty boolean returns false")
        void testParseBooleanEmpty() {
            assertFalse(ParsingUtils.parseBoolean(""));
        }
    }
}
