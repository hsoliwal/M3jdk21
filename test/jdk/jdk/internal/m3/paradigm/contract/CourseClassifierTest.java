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
 * Unit tests for CourseClassifier.
 *
 * <p>
 * Tests MIT course classification, text-based classification, and universal law
 * enumeration.
 */
@DisplayName("CourseClassifier Tests")
class CourseClassifierTest {

    @Nested
    @DisplayName("MIT Course Classification Tests")
    class MITCourseTests {

        @ParameterizedTest
        @CsvSource({
            "18, MATHEMATICS",
            "8, PHYSICS",
            "5, CHEMISTRY",
            "7, BIOLOGY",
            "6, COMPUTER_SCIENCE",
            "14, ECONOMICS",
            "24, PHILOSOPHY"
        })
        @DisplayName("Classify MIT courses by number")
        void testClassifyMITByNumber(final int courseNumber, final String expectedDomain) {
            final Optional<OrdinalSpine.Domain> result = CourseClassifier.classifyMIT(courseNumber);
            assertTrue(result.isPresent());
            assertEquals(expectedDomain, result.get().name());
        }

        @Test
        @DisplayName("Classify unknown MIT course number returns empty")
        void testClassifyMITUnknownNumber() {
            final Optional<OrdinalSpine.Domain> result = CourseClassifier.classifyMIT(99);
            assertTrue(result.isEmpty());
        }

        @ParameterizedTest
        @CsvSource({
            "18.01, MATHEMATICS",
            "8.01, PHYSICS",
            "5.12, CHEMISTRY",
            "7.012, BIOLOGY",
            "6.042, COMPUTER_SCIENCE"
        })
        @DisplayName("Classify MIT courses by code string")
        void testClassifyMITByCode(final String courseCode, final String expectedDomain) {
            final Optional<OrdinalSpine.Domain> result = CourseClassifier.classifyMIT(courseCode);
            assertTrue(result.isPresent());
            assertEquals(expectedDomain, result.get().name());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Classify empty or null MIT course code returns empty")
        void testClassifyMITEmptyCode(final String courseCode) {
            final Optional<OrdinalSpine.Domain> result = CourseClassifier.classifyMIT(courseCode);
            assertTrue(result.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "MIT-18", "xyz.123"})
        @DisplayName("Classify invalid MIT course code returns empty")
        void testClassifyMITInvalidCode(final String courseCode) {
            final Optional<OrdinalSpine.Domain> result = CourseClassifier.classifyMIT(courseCode);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Crystallinity for MIT course 18 is 1.0")
        void testCrystallinityMIT() {
            final double crystallinity = CourseClassifier.crystallinityMIT(18);
            assertEquals(1.0, crystallinity, 0.001);
        }

        @Test
        @DisplayName("Crystallinity for unknown MIT course is -1.0")
        void testCrystallinityMITUnknown() {
            final double crystallinity = CourseClassifier.crystallinityMIT(99);
            assertEquals(-1.0, crystallinity, 0.001);
        }
    }

    @Nested
    @DisplayName("Text-Based Classification Tests")
    class TextClassificationTests {

        @ParameterizedTest
        @CsvSource({
            "Calculus and Differential Equations, MATHEMATICS",
            "Quantum Mechanics, PHYSICS",
            "Organic Chemistry, CHEMISTRY",
            "Molecular Biology, BIOLOGY",
            "Algorithms and Data Structures, COMPUTER_SCIENCE",
            "Microeconomics, ECONOMICS",
            "Ethics and Morality, PHILOSOPHY"
        })
        @DisplayName("Classify courses by text")
        void testClassifyByText(final String text, final String expectedDomain) {
            final Optional<OrdinalSpine.Domain> result = CourseClassifier.classifyByText(text);
            assertTrue(result.isPresent());
            assertEquals(expectedDomain, result.get().name());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Classify null or empty text returns empty")
        void testClassifyByTextEmpty(final String text) {
            final Optional<OrdinalSpine.Domain> result = CourseClassifier.classifyByText(text);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Classify unrecognized text returns empty")
        void testClassifyByTextUnrecognized() {
            final Optional<OrdinalSpine.Domain> result = CourseClassifier.classifyByText("Basket Weaving 101");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Crystallinity by text for math course")
        void testCrystallinityByText() {
            final double crystallinity = CourseClassifier.crystallinityByText("Linear Algebra");
            assertTrue(crystallinity >= 0.9);
        }

        @Test
        @DisplayName("Crystallinity by unrecognized text is -1.0")
        void testCrystallinityByTextUnrecognized() {
            final double crystallinity = CourseClassifier.crystallinityByText("Unknown Subject");
            assertEquals(-1.0, crystallinity, 0.001);
        }
    }

    @Nested
    @DisplayName("ClassifiedCourse Record Tests")
    class ClassifiedCourseTests {

        @Test
        @DisplayName("Create classified course")
        void testCreateClassifiedCourse() {
            final CourseClassifier.ClassifiedCourse course =
                    CourseClassifier.ClassifiedCourse.of("MIT", "18.01", "Calculus", OrdinalSpine.Domain.MATHEMATICS);

            assertEquals("MIT", course.source());
            assertEquals("18.01", course.code());
            assertEquals("Calculus", course.title());
            assertEquals(OrdinalSpine.Domain.MATHEMATICS, course.domain());
            assertEquals(1.0, course.crystallinity(), 0.001);
        }

        @Test
        @DisplayName("Compare crystallinity of two courses")
        void testIsMoreCrystallineThan() {
            final CourseClassifier.ClassifiedCourse math =
                    CourseClassifier.ClassifiedCourse.of("MIT", "18.01", "Calculus", OrdinalSpine.Domain.MATHEMATICS);

            final CourseClassifier.ClassifiedCourse physics =
                    CourseClassifier.ClassifiedCourse.of("MIT", "8.01", "Physics", OrdinalSpine.Domain.PHYSICS);

            assertTrue(math.isMoreCrystallineThan(physics));
            assertFalse(physics.isMoreCrystallineThan(math));
        }

        @Test
        @DisplayName("Check prerequisite relationship")
        void testIsPrerequisiteFor() {
            final CourseClassifier.ClassifiedCourse math =
                    CourseClassifier.ClassifiedCourse.of("MIT", "18.01", "Calculus", OrdinalSpine.Domain.MATHEMATICS);

            final CourseClassifier.ClassifiedCourse physics =
                    CourseClassifier.ClassifiedCourse.of("MIT", "8.01", "Physics", OrdinalSpine.Domain.PHYSICS);

            assertTrue(math.isPrerequisiteFor(physics));
            assertFalse(physics.isPrerequisiteFor(math));
        }
    }

    @Nested
    @DisplayName("Classify Method Tests")
    class ClassifyMethodTests {

        @Test
        @DisplayName("Classify MIT course by source")
        void testClassifyMITSource() {
            final Optional<CourseClassifier.ClassifiedCourse> result =
                    CourseClassifier.classify("MIT", "18.01", "Calculus");

            assertTrue(result.isPresent());
            assertEquals(OrdinalSpine.Domain.MATHEMATICS, result.get().domain());
        }

        @Test
        @DisplayName("Classify non-MIT course by text")
        void testClassifyOtherSource() {
            final Optional<CourseClassifier.ClassifiedCourse> result =
                    CourseClassifier.classify("Harvard", "MATH-101", "Linear Algebra and Calculus");

            assertTrue(result.isPresent());
            assertEquals(OrdinalSpine.Domain.MATHEMATICS, result.get().domain());
        }

        @Test
        @DisplayName("Classify unrecognizable course returns empty")
        void testClassifyUnrecognizable() {
            final Optional<CourseClassifier.ClassifiedCourse> result =
                    CourseClassifier.classify("Other", "XYZ-123", "Unknown Subject");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Universal Law Tests")
    class UniversalLawTests {

        @Test
        @DisplayName("All universal laws have formulas")
        void testUniversalLawsHaveFormulas() {
            for (final CourseClassifier.UniversalLaw law : CourseClassifier.UniversalLaw.values()) {
                assertNotNull(law.formula);
                assertFalse(law.formula.isEmpty());
            }
        }

        @Test
        @DisplayName("All universal laws have domains")
        void testUniversalLawsHaveDomains() {
            for (final CourseClassifier.UniversalLaw law : CourseClassifier.UniversalLaw.values()) {
                assertNotNull(law.domain);
            }
        }

        @Test
        @DisplayName("All universal laws have descriptions")
        void testUniversalLawsHaveDescriptions() {
            for (final CourseClassifier.UniversalLaw law : CourseClassifier.UniversalLaw.values()) {
                assertNotNull(law.description);
                assertFalse(law.description.isEmpty());
            }
        }

        @Test
        @DisplayName("Universal law crystallinity matches domain")
        void testUniversalLawCrystallinity() {
            final CourseClassifier.UniversalLaw arithmetic = CourseClassifier.UniversalLaw.ARITHMETIC;
            assertEquals(arithmetic.domain.crystallinity, arithmetic.crystallinity(), 0.001);
        }

        @Test
        @DisplayName("Mathematical laws are crystalline")
        void testMathematicalLawsCrystalline() {
            assertTrue(CourseClassifier.UniversalLaw.ARITHMETIC.isCrystalline());
            assertTrue(CourseClassifier.UniversalLaw.PYTHAGOREAN.isCrystalline());
            assertTrue(CourseClassifier.UniversalLaw.EULER_IDENTITY.isCrystalline());
        }

        @Test
        @DisplayName("Physical laws are crystalline")
        void testPhysicalLawsCrystalline() {
            assertTrue(CourseClassifier.UniversalLaw.NEWTON_SECOND.isCrystalline());
            assertTrue(CourseClassifier.UniversalLaw.MASS_ENERGY.isCrystalline());
            assertTrue(CourseClassifier.UniversalLaw.GRAVITATION.isCrystalline());
        }

        @Test
        @DisplayName("Chemical laws are crystalline")
        void testChemicalLawsCrystalline() {
            assertTrue(CourseClassifier.UniversalLaw.IDEAL_GAS.isCrystalline());
            assertTrue(CourseClassifier.UniversalLaw.GIBBS_ENERGY.isCrystalline());
        }

        @Test
        @DisplayName("Information laws are crystalline")
        void testInformationLawsCrystalline() {
            assertTrue(CourseClassifier.UniversalLaw.SHANNON_ENTROPY.isCrystalline());
            assertTrue(CourseClassifier.UniversalLaw.CHANNEL_CAPACITY.isCrystalline());
        }
    }
}
