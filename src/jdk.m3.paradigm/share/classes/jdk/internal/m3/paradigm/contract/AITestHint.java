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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AITestHint - AnOP annotation for testing guidance.
 *
 * <h2>AnOP Advisory: Testing Strategy</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Testing hints guide LLM test generation to match project conventions.    ║
 * ║  These are ADVISORY - actual test execution is by JUnit/TestNG/etc.       ║
 * ║  Tests outrank AI. Tests outrank annotations. Fix BEHAVIOR, not tests.    ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>IOP Authority Hierarchy</h2>
 *
 * <pre>
 * Interface + JavaDoc     (highest - the contract)
 *        ↓
 * Compiler                (enforcement)
 *        ↓
 * Tests                   (verification - OUTRANKS AI)
 *        ↓
 * AI Output               (lowest - disposable)
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;AITestHint(type = Type.UNIT, coverage = Coverage.BRANCH, minCoverage = 80, framework = Framework.JUNIT5, mocking = Mocking.MOCKITO)
 *     public interface IUserService {
 *
 *         User findById(String id);
 *     }
 *
 *     &#64;AITestHint(type = Type.INTEGRATION, requires = {"database",
 *             "redis"}, timeout = 30000, retries = 3)
 *     public interface IOrderRepository {
 *     }
 *
 *     &#64;AITestHint(type = Type.PROPERTY_BASED, framework = Framework.JQWIK, generators = {
 *             "validEmail", "positiveAmount"})
 *     public interface IValidator {
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Testing Hints
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AITestHint.java}
 * blob {@code 23600f42f8f33983b2697934b768f5dab9b2b048} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AITestHints.class)
public @interface AITestHint {

    /**
     * Test type.
     *
     * @return test type
     */
    Type type() default Type.UNIT;

    /**
     * Coverage metric type.
     *
     * @return coverage type
     */
    Coverage coverage() default Coverage.LINE;

    /**
     * Minimum coverage percentage (0-100).
     *
     * @return min coverage
     */
    int minCoverage() default 80;

    /**
     * Test framework.
     *
     * @return framework
     */
    Framework framework() default Framework.JUNIT5;

    /**
     * Mocking framework.
     *
     * @return mocking framework
     */
    Mocking mocking() default Mocking.MOCKITO;

    /**
     * Required test dependencies/resources.
     *
     * @return required resources
     */
    String[] requires() default {};

    /**
     * Test timeout in milliseconds.
     *
     * @return timeout ms
     */
    long timeout() default 5000;

    /**
     * Number of retries for flaky tests.
     *
     * @return retry count
     */
    int retries() default 0;

    /**
     * Test data generators (for property-based testing).
     *
     * @return generator names
     */
    String[] generators() default {};

    /**
     * Test categories/tags.
     *
     * @return tags
     */
    String[] tags() default {};

    /**
     * Edge cases that MUST be tested.
     *
     * @return edge cases
     */
    EdgeCase[] edgeCases() default {};

    /**
     * Boundary values to test.
     *
     * @return boundaries
     */
    String[] boundaries() default {};

    /**
     * Expected exceptions for negative tests.
     *
     * @return exception classes
     */
    Class<? extends Throwable>[] expectedException() default {};

    /**
     * Test isolation level.
     *
     * @return isolation
     */
    Isolation isolation() default Isolation.METHOD;

    /**
     * Parallel execution allowed.
     *
     * @return true if parallelizable
     */
    boolean parallel() default false;

    /**
     * Test data strategy.
     *
     * @return data strategy
     */
    DataStrategy dataStrategy() default DataStrategy.BUILDER;

    /**
     * Assertion style.
     *
     * @return assertion style
     */
    AssertionStyle assertions() default AssertionStyle.ASSERTJ;

    /**
     * Test naming convention.
     *
     * @return naming convention
     */
    NamingConvention naming() default NamingConvention.GIVEN_WHEN_THEN;

    // --- ENUMS ---

    /** Test types. */
    enum Type {
        // --- FUNCTIONAL TESTS ---

        /** Unit test - single class/method in isolation. */
        UNIT,

        /** Integration test - multiple components together. */
        INTEGRATION,

        /** End-to-end test - full system flow. */
        E2E,

        /** Contract test - API contract verification. */
        CONTRACT,

        /** Component test - single component with real dependencies. */
        COMPONENT,

        // --- SPECIALIZED TESTS ---

        /** Property-based test - generative testing. */
        PROPERTY_BASED,

        /** Mutation test - test quality verification. */
        MUTATION,

        /** Fuzz test - random input testing. */
        FUZZ,

        /** Snapshot test - output comparison. */
        SNAPSHOT,

        /** Approval test - human-verified output. */
        APPROVAL,

        // --- NON-FUNCTIONAL TESTS ---

        /** Performance test - speed/throughput. */
        PERFORMANCE,

        /** Load test - behavior under load. */
        LOAD,

        /** Stress test - behavior at limits. */
        STRESS,

        /** Security test - vulnerability checks. */
        SECURITY,

        /** Chaos test - failure injection. */
        CHAOS
    }

    /** Coverage metrics. */
    enum Coverage {
        /** Line coverage. */
        LINE,

        /** Branch coverage (if/else paths). */
        BRANCH,

        /** Method coverage. */
        METHOD,

        /** Instruction coverage. */
        INSTRUCTION,

        /** Mutation coverage (PIT). */
        MUTATION,

        /** Path coverage (all execution paths). */
        PATH
    }

    /** Test frameworks. */
    enum Framework {
        /** JUnit 5 (Jupiter). */
        JUNIT5,

        /** JUnit 4. */
        JUNIT4,

        /** TestNG. */
        TESTNG,

        /** Spock (Groovy). */
        SPOCK,

        /** jqwik (property-based). */
        JQWIK,

        /** Arquillian (integration). */
        ARQUILLIAN,

        /** Cucumber (BDD). */
        CUCUMBER,

        /** JBehave (BDD). */
        JBEHAVE
    }

    /** Mocking frameworks. */
    enum Mocking {
        /** No mocking. */
        NONE,

        /** Mockito. */
        MOCKITO,

        /** EasyMock. */
        EASYMOCK,

        /** PowerMock (for static/final). */
        POWERMOCK,

        /** WireMock (HTTP). */
        WIREMOCK,

        /** Testcontainers (Docker). */
        TESTCONTAINERS,

        /** MockServer. */
        MOCKSERVER
    }

    /** Edge cases that MUST be covered. */
    enum EdgeCase {
        // --- NULL/EMPTY ---

        /** Null input. */
        NULL_INPUT,

        /** Empty string. */
        EMPTY_STRING,

        /** Empty collection. */
        EMPTY_COLLECTION,

        /** Whitespace only. */
        WHITESPACE_ONLY,

        // --- BOUNDARIES ---

        /** Zero value. */
        ZERO,

        /** Negative values. */
        NEGATIVE,

        /** Maximum value. */
        MAX_VALUE,

        /** Minimum value. */
        MIN_VALUE,

        /** Overflow. */
        OVERFLOW,

        /** Underflow. */
        UNDERFLOW,

        // --- SPECIAL CASES ---

        /** Single element. */
        SINGLE_ELEMENT,

        /** Duplicate elements. */
        DUPLICATES,

        /** Already sorted. */
        ALREADY_SORTED,

        /** Reverse sorted. */
        REVERSE_SORTED,

        /** Unicode/special characters. */
        UNICODE,

        /** Very large input. */
        LARGE_INPUT,

        // --- CONCURRENCY ---

        /** Concurrent access. */
        CONCURRENT,

        /** Race condition. */
        RACE_CONDITION,

        /** Deadlock scenario. */
        DEADLOCK,

        // --- ERROR CONDITIONS ---

        /** Timeout scenario. */
        TIMEOUT,

        /** Connection failure. */
        CONNECTION_FAILURE,

        /** Invalid format. */
        INVALID_FORMAT,

        /** Permission denied. */
        PERMISSION_DENIED
    }

    /** Test isolation levels. */
    enum Isolation {
        /** Each test method is isolated. */
        METHOD,

        /** Each test class is isolated. */
        CLASS,

        /** No isolation (shared state). */
        NONE,

        /** Fork per test (JVM isolation). */
        FORK
    }

    /** Test data creation strategies. */
    enum DataStrategy {
        /** Builder pattern. */
        BUILDER,

        /** Object Mother pattern. */
        OBJECT_MOTHER,

        /** Test Data Builder. */
        TEST_DATA_BUILDER,

        /** Factory methods. */
        FACTORY,

        /** Fixture files (JSON/YAML). */
        FIXTURE,

        /** Random generation. */
        RANDOM,

        /** Database seeding. */
        DATABASE_SEED
    }

    /** Assertion library styles. */
    enum AssertionStyle {
        /** JUnit assertions. */
        JUNIT,

        /** AssertJ fluent assertions. */
        ASSERTJ,

        /** Hamcrest matchers. */
        HAMCREST,

        /** Truth (Google). */
        TRUTH,

        /** Custom assertions. */
        CUSTOM
    }

    /** Test naming conventions. */
    enum NamingConvention {
        /** given_when_then (BDD style). */
        GIVEN_WHEN_THEN,

        /** should_expectedBehavior_when_condition. */
        SHOULD_WHEN,

        /** methodName_stateUnderTest_expectedBehavior. */
        METHOD_STATE_EXPECTED,

        /** test + method name (legacy). */
        TEST_PREFIX,

        /** Descriptive sentence. */
        DESCRIPTIVE
    }
}
