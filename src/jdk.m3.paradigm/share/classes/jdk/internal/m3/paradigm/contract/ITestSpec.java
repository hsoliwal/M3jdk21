/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ITestSpec - Test specification for AI code generation.
 *
 * <h2>Testing Patterns</h2>
 *
 * <pre>
 * Testing frameworks:
 *   - JUnit 5
 *   - TestNG
 *   - Mockito
 *   - AssertJ
 *   - Property-based testing
 *   - Contract testing
 *
 * &#64;ITestSpec tells AI what tests to generate.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * &#64;ITestSpec(
 *     target = UserService.class,
 *     coverage = 0.80,
 *     tests = {
 *         &#64;ITestSpec.Case(method = "createUser", scenario = "valid input", expected = "success"),
 *         &#64;ITestSpec.Case(method = "createUser", scenario = "null email", expected = "ValidationException"),
 * &#64;ITestSpec.Case(method = "findById", scenario = "not found", expected = "Optional.empty()")})
 * public interface IUserServiceTest&lt;Self extends IUserServiceTest&lt;Self>> extends IContext&lt;Self> {
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Test Specification
 * @see AITest
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares testing targets, coverage goals, cases, doubles and framework preferences; it runs no test and verifies no coverage or assertion.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/ITestSpec.java}
 * blob {@code 156abf2f026acdbfc0a48c80bb1e9a4d31de85fc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ITestSpec {

    // --- TARGET ---

    /**
     * Class under test; Void.class remains the unspecified sentinel.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#target intent: Class under test; Void.class remains the unspecified sentinel. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> target() default Void.class;

    /**
     * Requested target method selector.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#targetMethod intent: Requested target method selector. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String targetMethod() default "";

    // --- COVERAGE ---

    /**
     * Requested coverage target, defaulting to 0.8; no measurement occurs.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#coverage intent: Requested coverage target, defaulting to 0.8; no measurement occurs. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.8}.
     */
    double coverage() default 0.80;

    /**
     * Requested branch-coverage target, defaulting to 0.7.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#branchCoverage intent: Requested branch-coverage target, defaulting to 0.7. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.7}.
     */
    double branchCoverage() default 0.70;

    /**
     * Requested mutation-score target.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#mutationScore intent: Requested mutation-score target. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.0}.
     */
    double mutationScore() default 0.0;

    // --- TEST CASES ---

    /**
     * Declared test cases in supplied order.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#tests intent: Declared test cases in supplied order. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Case[]}; default expression {@code {}}.
     */
    Case[] tests() default {};

    /**
     * Declares a test scenario, target method and expected behavior; it runs no test and retains implicit CLASS retention without explicit Target restriction.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a test scenario, target method and expected behavior; it runs no test and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Case {

        /**
         * Target method identifier for the test case.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ITestSpec.Case#method intent: Target method identifier for the test case. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String method();

        /**
         * Description of the test scenario.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ITestSpec.Case#scenario intent: Description of the test scenario. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String scenario();

        /**
         * Expected result text; the annotation performs no assertion.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ITestSpec.Case#expected intent: Expected result text; the annotation performs no assertion. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String expected();

        /**
         * Precondition or fixture descriptions in supplied order.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ITestSpec.Case#given intent: Precondition or fixture descriptions in supplied order. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
         */
        String[] given() default {};

        /**
         * Action descriptions in supplied order.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ITestSpec.Case#when intent: Action descriptions in supplied order. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
         */
        String[] when() default {};

        /**
         * Expected-outcome descriptions in supplied order.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ITestSpec.Case#then intent: Expected-outcome descriptions in supplied order. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
         */
        String[] then() default {};
    }

    // --- MOCKING ---

    /**
     * Types requested as test doubles.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#mocks intent: Types requested as test doubles. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>[]}; default expression {@code {}}.
     */
    Class<?>[] mocks() default {};

    /**
     * Types requested as observing test doubles.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#spies intent: Types requested as observing test doubles. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>[]}; default expression {@code {}}.
     */
    Class<?>[] spies() default {};

    // --- TEST TYPE ---

    /**
     * Requested test category.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#type intent: Requested test category. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Type}; default expression {@code Type.UNIT}.
     */
    Type type() default Type.UNIT;

    /**
     * Test categories; selection performs no operation or dependency resolution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ITestSpec.Type choices described as Test categories; selection performs no operation or dependency resolution. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum Type {
        /**
         * Requested unit tests category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITestSpec.Type#UNIT policy choice: Requested unit tests category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITestSpec.Type; its declared order and identity are preserved.
         */
        UNIT,
        /**
         * Requested integration tests category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITestSpec.Type#INTEGRATION policy choice: Requested integration tests category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITestSpec.Type; its declared order and identity are preserved.
         */
        INTEGRATION,
        /**
         * Requested contract tests category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITestSpec.Type#CONTRACT policy choice: Requested contract tests category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITestSpec.Type; its declared order and identity are preserved.
         */
        CONTRACT,
        /**
         * Requested property-based tests category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITestSpec.Type#PROPERTY_BASED policy choice: Requested property-based tests category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITestSpec.Type; its declared order and identity are preserved.
         */
        PROPERTY_BASED,
        /**
         * Requested end-to-end tests category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITestSpec.Type#E2E policy choice: Requested end-to-end tests category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITestSpec.Type; its declared order and identity are preserved.
         */
        E2E
    }

    // --- FRAMEWORK ---

    /**
     * Requested test framework token.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#framework intent: Requested test framework token. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Framework}; default expression {@code Framework.JUNIT5}.
     */
    Framework framework() default Framework.JUNIT5;

    /**
     * Test framework choices; selection performs no operation or dependency resolution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ITestSpec.Framework choices described as Test framework choices; selection performs no operation or dependency resolution. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 3 constants in the existing source order.
     */
    enum Framework {
        /**
         * Requested JUnit 5 category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITestSpec.Framework#JUNIT5 policy choice: Requested JUnit 5 category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITestSpec.Framework; its declared order and identity are preserved.
         */
        JUNIT5,
        /**
         * Requested TestNG category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITestSpec.Framework#TESTNG policy choice: Requested TestNG category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITestSpec.Framework; its declared order and identity are preserved.
         */
        TESTNG,
        /**
         * Requested Spock category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITestSpec.Framework#SPOCK policy choice: Requested Spock category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITestSpec.Framework; its declared order and identity are preserved.
         */
        SPOCK
    }

    // --- DESCRIPTION ---

    /**
     * Natural-language testing specification.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#value intent: Natural-language testing specification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    /**
     * Preferred generation-model token.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITestSpec#model intent: Preferred generation-model token. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
