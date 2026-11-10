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
 * 	&#64;code
 * 	&#64;AITestHint(type = Type.UNIT, coverage = Coverage.BRANCH, minCoverage = 80, framework = Framework.JUNIT5, mocking = Mocking.MOCKITO)
 * 	public interface IUserService&lt;Self extends IUserService&lt;Self>> extends IContext&lt;Self> {
 *
 * 		User findById(String id);
 * 	}
 *
 * 	&#64;AITestHint(type = Type.INTEGRATION, requires = {"database", "redis"}, timeout = 30000, retries = 3)
 * 	public interface IOrderRepository&lt;Self extends IOrderRepository&lt;Self>> extends IContext&lt;Self> {
 * 	}
 *
 * 	&#64;AITestHint(type = Type.PROPERTY_BASED, framework = Framework.JQWIK, generators = {"validEmail",
 * 			"positiveAmount"})
 * 	public interface IValidator&lt;Self extends IValidator&lt;Self>> extends IContext&lt;Self> {
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Testing Hints
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares test-generation conventions, coverage targets, frameworks, fixtures, isolation and boundary-case guidance; this annotation records guidance rather than implementing or verifying the requested behavior.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD}; preserves its declared element and nested-token schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAITestHint.java}
 * blob {@code 115262724be88e3a65a12bfb70806217058b2bde} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAITestHints.class)
public @interface IAITestHint {

    /**
     * Test type.
     *
     * @return test type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#type advisory metadata for Test type. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Type annotation element; exact default expression Type.UNIT.
     */
    Type type() default Type.UNIT;

    /**
     * Coverage metric type.
     *
     * @return coverage type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#coverage advisory metadata for Coverage metric type. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Coverage annotation element; exact default expression Coverage.LINE.
     */
    Coverage coverage() default Coverage.LINE;

    /**
     * Minimum coverage percentage (0-100).
     *
     * @return min coverage
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#minCoverage advisory metadata for Minimum coverage percentage (0-100). The element itself neither executes nor enforces that request.
     * @m3.dataStructures int annotation element; exact default expression 80.
     */
    int minCoverage() default 80;

    /**
     * Test framework.
     *
     * @return framework
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#framework advisory metadata for Test framework. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Framework annotation element; exact default expression Framework.JUNIT5.
     */
    Framework framework() default Framework.JUNIT5;

    /**
     * Mocking framework.
     *
     * @return mocking framework
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#mocking advisory metadata for Mocking framework. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Mocking annotation element; exact default expression Mocking.MOCKITO.
     */
    Mocking mocking() default Mocking.MOCKITO;

    /**
     * Required test dependencies/resources.
     *
     * @return required resources
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#requires advisory metadata for Required test dependencies/resources. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] requires() default {};

    /**
     * Test timeout in milliseconds.
     *
     * @return timeout ms
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#timeout advisory metadata for Test timeout in milliseconds. The element itself neither executes nor enforces that request.
     * @m3.dataStructures long annotation element; exact default expression 5000.
     */
    long timeout() default 5000;

    /**
     * Number of retries for flaky tests.
     *
     * @return retry count
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#retries advisory metadata for Number of retries for flaky tests. The element itself neither executes nor enforces that request.
     * @m3.dataStructures int annotation element; exact default expression 0.
     */
    int retries() default 0;

    /**
     * Test data generators (for property-based testing).
     *
     * @return generator names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#generators advisory metadata for Test data generators (for property-based testing). The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] generators() default {};

    /**
     * Test categories/tags.
     *
     * @return tags
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#tags advisory metadata for Test categories/tags. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] tags() default {};

    /**
     * Edge cases that MUST be tested.
     *
     * @return edge cases
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#edgeCases advisory metadata for Edge cases that MUST be tested. The element itself neither executes nor enforces that request.
     * @m3.dataStructures EdgeCase[] annotation element; exact default expression {}.
     */
    EdgeCase[] edgeCases() default {};

    /**
     * Boundary values to test.
     *
     * @return boundaries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#boundaries advisory metadata for Boundary values to test. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] boundaries() default {};

    /**
     * Expected exceptions for negative tests.
     *
     * @return exception classes
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#expectedException advisory metadata for Expected exceptions for negative tests. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Class&lt;? extends Throwable>[] annotation element; exact default expression {}.
     */
    Class<? extends Throwable>[] expectedException() default {};

    /**
     * Test isolation level.
     *
     * @return isolation
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#isolation advisory metadata for Test isolation level. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Isolation annotation element; exact default expression Isolation.METHOD.
     */
    Isolation isolation() default Isolation.METHOD;

    /**
     * Parallel execution allowed.
     *
     * @return true if parallelizable
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#parallel advisory metadata for Parallel execution allowed. The element itself neither executes nor enforces that request.
     * @m3.dataStructures boolean annotation element; exact default expression false.
     */
    boolean parallel() default false;

    /**
     * Test data strategy.
     *
     * @return data strategy
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#dataStrategy advisory metadata for Test data strategy. The element itself neither executes nor enforces that request.
     * @m3.dataStructures DataStrategy annotation element; exact default expression DataStrategy.BUILDER.
     */
    DataStrategy dataStrategy() default DataStrategy.BUILDER;

    /**
     * Assertion style.
     *
     * @return assertion style
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#assertions advisory metadata for Assertion style. The element itself neither executes nor enforces that request.
     * @m3.dataStructures AssertionStyle annotation element; exact default expression AssertionStyle.ASSERTJ.
     */
    AssertionStyle assertions() default AssertionStyle.ASSERTJ;

    /**
     * Test naming convention.
     *
     * @return naming convention
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHint#naming advisory metadata for Test naming convention. The element itself neither executes nor enforces that request.
     * @m3.dataStructures NamingConvention annotation element; exact default expression NamingConvention.GIVEN_WHEN_THEN.
     */
    NamingConvention naming() default NamingConvention.GIVEN_WHEN_THEN;

    // --- ENUMS ---

    /** Test types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 15 symbolic choices for Test types within IAITestHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAITestHint.Type enum with 15 source-declared constants in existing order.
     */
    enum Type {
        // --- FUNCTIONAL TESTS ---

        /** Unit test - single class/method in isolation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#UNIT token within Test types: Unit test - single class/method in isolation. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        UNIT,

        /** Integration test - multiple components together.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#INTEGRATION token within Test types: Integration test - multiple components together. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        INTEGRATION,

        /** End-to-end test - full system flow.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#E2E token within Test types: End-to-end test - full system flow. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        E2E,

        /** Contract test - API contract verification.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#CONTRACT token within Test types: Contract test - API contract verification. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONTRACT,

        /** Component test - single component with real dependencies.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#COMPONENT token within Test types: Component test - single component with real dependencies. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        COMPONENT,

        // --- SPECIALIZED TESTS ---

        /** Property-based test - generative testing.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#PROPERTY_BASED token within Test types: Property-based test - generative testing. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PROPERTY_BASED,

        /** Mutation test - test quality verification.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#MUTATION token within Test types: Mutation test - test quality verification. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MUTATION,

        /** Fuzz test - random input testing.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#FUZZ token within Test types: Fuzz test - random input testing. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FUZZ,

        /** Snapshot test - output comparison.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#SNAPSHOT token within Test types: Snapshot test - output comparison. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SNAPSHOT,

        /** Approval test - human-verified output.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#APPROVAL token within Test types: Approval test - human-verified output. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        APPROVAL,

        // --- NON-FUNCTIONAL TESTS ---

        /** Performance test - speed/throughput.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#PERFORMANCE token within Test types: Performance test - speed/throughput. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PERFORMANCE,

        /** Load test - behavior under load.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#LOAD token within Test types: Load test - behavior under load. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LOAD,

        /** Stress test - behavior at limits.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#STRESS token within Test types: Stress test - behavior at limits. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        STRESS,

        /** Security test - vulnerability checks.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#SECURITY token within Test types: Security test - vulnerability checks. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SECURITY,

        /** Chaos test - failure injection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Type#CHAOS token within Test types: Chaos test - failure injection. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Type enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CHAOS
    }

    /** Coverage metrics.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 6 symbolic choices for Coverage metrics within IAITestHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAITestHint.Coverage enum with 6 source-declared constants in existing order.
     */
    enum Coverage {
        /** Line coverage.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Coverage#LINE token within Coverage metrics: Line coverage. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Coverage enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LINE,

        /** Branch coverage (if/else paths).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Coverage#BRANCH token within Coverage metrics: Branch coverage (if/else paths). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Coverage enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BRANCH,

        /** Method coverage.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Coverage#METHOD token within Coverage metrics: Method coverage. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Coverage enum constant, retaining declared order and identity; it stores no implementation object.
         */
        METHOD,

        /** Instruction coverage.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Coverage#INSTRUCTION token within Coverage metrics: Instruction coverage. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Coverage enum constant, retaining declared order and identity; it stores no implementation object.
         */
        INSTRUCTION,

        /** Mutation coverage (PIT).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Coverage#MUTATION token within Coverage metrics: Mutation coverage (PIT). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Coverage enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MUTATION,

        /** Path coverage (all execution paths).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Coverage#PATH token within Coverage metrics: Path coverage (all execution paths). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Coverage enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PATH
    }

    /** Test frameworks.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 8 symbolic choices for Test frameworks within IAITestHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAITestHint.Framework enum with 8 source-declared constants in existing order.
     */
    enum Framework {
        /** JUnit 5 (Jupiter).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Framework#JUNIT5 token within Test frameworks: JUnit 5 (Jupiter). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Framework enum constant, retaining declared order and identity; it stores no implementation object.
         */
        JUNIT5,

        /** JUnit 4.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Framework#JUNIT4 token within Test frameworks: JUnit 4. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Framework enum constant, retaining declared order and identity; it stores no implementation object.
         */
        JUNIT4,

        /** TestNG.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Framework#TESTNG token within Test frameworks: TestNG. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Framework enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TESTNG,

        /** Spock (Groovy).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Framework#SPOCK token within Test frameworks: Spock (Groovy). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Framework enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SPOCK,

        /** jqwik (property-based).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Framework#JQWIK token within Test frameworks: jqwik (property-based). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Framework enum constant, retaining declared order and identity; it stores no implementation object.
         */
        JQWIK,

        /** Arquillian (integration).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Framework#ARQUILLIAN token within Test frameworks: Arquillian (integration). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Framework enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ARQUILLIAN,

        /** Cucumber (BDD).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Framework#CUCUMBER token within Test frameworks: Cucumber (BDD). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Framework enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CUCUMBER,

        /** JBehave (BDD).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Framework#JBEHAVE token within Test frameworks: JBehave (BDD). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Framework enum constant, retaining declared order and identity; it stores no implementation object.
         */
        JBEHAVE
    }

    /** Mocking frameworks.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 7 symbolic choices for Mocking frameworks within IAITestHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAITestHint.Mocking enum with 7 source-declared constants in existing order.
     */
    enum Mocking {
        /** No mocking.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Mocking#NONE token within Mocking frameworks: No mocking. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Mocking enum constant, retaining declared order and identity; it stores no implementation object.
         */
        NONE,

        /** Mockito.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Mocking#MOCKITO token within Mocking frameworks: Mockito. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Mocking enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MOCKITO,

        /** EasyMock.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Mocking#EASYMOCK token within Mocking frameworks: EasyMock. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Mocking enum constant, retaining declared order and identity; it stores no implementation object.
         */
        EASYMOCK,

        /** PowerMock (for static/final).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Mocking#POWERMOCK token within Mocking frameworks: PowerMock (for static/final). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Mocking enum constant, retaining declared order and identity; it stores no implementation object.
         */
        POWERMOCK,

        /** WireMock (HTTP).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Mocking#WIREMOCK token within Mocking frameworks: WireMock (HTTP). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Mocking enum constant, retaining declared order and identity; it stores no implementation object.
         */
        WIREMOCK,

        /** Testcontainers (Docker).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Mocking#TESTCONTAINERS token within Mocking frameworks: Testcontainers (Docker). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Mocking enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TESTCONTAINERS,

        /** MockServer.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Mocking#MOCKSERVER token within Mocking frameworks: MockServer. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Mocking enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MOCKSERVER
    }

    /** Edge cases that MUST be covered.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 23 symbolic choices for Edge cases that MUST be covered within IAITestHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAITestHint.EdgeCase enum with 23 source-declared constants in existing order.
     */
    enum EdgeCase {
        // --- NULL/EMPTY ---

        /** Null input.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#NULL_INPUT token within Edge cases that MUST be covered: Null input. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        NULL_INPUT,

        /** Empty string.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#EMPTY_STRING token within Edge cases that MUST be covered: Empty string. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        EMPTY_STRING,

        /** Empty collection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#EMPTY_COLLECTION token within Edge cases that MUST be covered: Empty collection. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        EMPTY_COLLECTION,

        /** Whitespace only.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#WHITESPACE_ONLY token within Edge cases that MUST be covered: Whitespace only. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        WHITESPACE_ONLY,

        // --- BOUNDARIES ---

        /** Zero value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#ZERO token within Edge cases that MUST be covered: Zero value. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ZERO,

        /** Negative values.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#NEGATIVE token within Edge cases that MUST be covered: Negative values. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        NEGATIVE,

        /** Maximum value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#MAX_VALUE token within Edge cases that MUST be covered: Maximum value. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MAX_VALUE,

        /** Minimum value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#MIN_VALUE token within Edge cases that MUST be covered: Minimum value. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MIN_VALUE,

        /** Overflow.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#OVERFLOW token within Edge cases that MUST be covered: Overflow. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        OVERFLOW,

        /** Underflow.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#UNDERFLOW token within Edge cases that MUST be covered: Underflow. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        UNDERFLOW,

        // --- SPECIAL CASES ---

        /** Single element.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#SINGLE_ELEMENT token within Edge cases that MUST be covered: Single element. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SINGLE_ELEMENT,

        /** Duplicate elements.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#DUPLICATES token within Edge cases that MUST be covered: Duplicate elements. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DUPLICATES,

        /** Already sorted.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#ALREADY_SORTED token within Edge cases that MUST be covered: Already sorted. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ALREADY_SORTED,

        /** Reverse sorted.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#REVERSE_SORTED token within Edge cases that MUST be covered: Reverse sorted. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REVERSE_SORTED,

        /** Unicode/special characters.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#UNICODE token within Edge cases that MUST be covered: Unicode/special characters. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        UNICODE,

        /** Very large input.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#LARGE_INPUT token within Edge cases that MUST be covered: Very large input. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LARGE_INPUT,

        // --- CONCURRENCY ---

        /** Concurrent access.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#CONCURRENT token within Edge cases that MUST be covered: Concurrent access. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONCURRENT,

        /** Race condition.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#RACE_CONDITION token within Edge cases that MUST be covered: Race condition. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        RACE_CONDITION,

        /** Deadlock scenario.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#DEADLOCK token within Edge cases that MUST be covered: Deadlock scenario. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DEADLOCK,

        // --- ERROR CONDITIONS ---

        /** Timeout scenario.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#TIMEOUT token within Edge cases that MUST be covered: Timeout scenario. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TIMEOUT,

        /** Connection failure.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#CONNECTION_FAILURE token within Edge cases that MUST be covered: Connection failure. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONNECTION_FAILURE,

        /** Invalid format.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#INVALID_FORMAT token within Edge cases that MUST be covered: Invalid format. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        INVALID_FORMAT,

        /** Permission denied.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.EdgeCase#PERMISSION_DENIED token within Edge cases that MUST be covered: Permission denied. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.EdgeCase enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PERMISSION_DENIED
    }

    /** Test isolation levels.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 4 symbolic choices for Test isolation levels within IAITestHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAITestHint.Isolation enum with 4 source-declared constants in existing order.
     */
    enum Isolation {
        /** Each test method is isolated.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Isolation#METHOD token within Test isolation levels: Each test method is isolated. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Isolation enum constant, retaining declared order and identity; it stores no implementation object.
         */
        METHOD,

        /** Each test class is isolated.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Isolation#CLASS token within Test isolation levels: Each test class is isolated. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Isolation enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CLASS,

        /** No isolation (shared state).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Isolation#NONE token within Test isolation levels: No isolation (shared state). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Isolation enum constant, retaining declared order and identity; it stores no implementation object.
         */
        NONE,

        /** Fork per test (JVM isolation).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.Isolation#FORK token within Test isolation levels: Fork per test (JVM isolation). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.Isolation enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FORK
    }

    /** Test data creation strategies.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 7 symbolic choices for Test data creation strategies within IAITestHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAITestHint.DataStrategy enum with 7 source-declared constants in existing order.
     */
    enum DataStrategy {
        /** Builder pattern.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.DataStrategy#BUILDER token within Test data creation strategies: Builder pattern. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.DataStrategy enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BUILDER,

        /** Object Mother pattern.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.DataStrategy#OBJECT_MOTHER token within Test data creation strategies: Object Mother pattern. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.DataStrategy enum constant, retaining declared order and identity; it stores no implementation object.
         */
        OBJECT_MOTHER,

        /** Test Data Builder.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.DataStrategy#TEST_DATA_BUILDER token within Test data creation strategies: Test Data Builder. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.DataStrategy enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TEST_DATA_BUILDER,

        /** Factory methods.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.DataStrategy#FACTORY token within Test data creation strategies: Factory methods. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.DataStrategy enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FACTORY,

        /** Fixture files (JSON/YAML).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.DataStrategy#FIXTURE token within Test data creation strategies: Fixture files (JSON/YAML). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.DataStrategy enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FIXTURE,

        /** Random generation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.DataStrategy#RANDOM token within Test data creation strategies: Random generation. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.DataStrategy enum constant, retaining declared order and identity; it stores no implementation object.
         */
        RANDOM,

        /** Database seeding.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.DataStrategy#DATABASE_SEED token within Test data creation strategies: Database seeding. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.DataStrategy enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DATABASE_SEED
    }

    /** Assertion library styles.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 5 symbolic choices for Assertion library styles within IAITestHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAITestHint.AssertionStyle enum with 5 source-declared constants in existing order.
     */
    enum AssertionStyle {
        /** JUnit assertions.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.AssertionStyle#JUNIT token within Assertion library styles: JUnit assertions. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.AssertionStyle enum constant, retaining declared order and identity; it stores no implementation object.
         */
        JUNIT,

        /** AssertJ fluent assertions.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.AssertionStyle#ASSERTJ token within Assertion library styles: AssertJ fluent assertions. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.AssertionStyle enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ASSERTJ,

        /** Hamcrest matchers.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.AssertionStyle#HAMCREST token within Assertion library styles: Hamcrest matchers. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.AssertionStyle enum constant, retaining declared order and identity; it stores no implementation object.
         */
        HAMCREST,

        /** Truth (Google).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.AssertionStyle#TRUTH token within Assertion library styles: Truth (Google). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.AssertionStyle enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TRUTH,

        /** Custom assertions.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.AssertionStyle#CUSTOM token within Assertion library styles: Custom assertions. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.AssertionStyle enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CUSTOM
    }

    /** Test naming conventions.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 5 symbolic choices for Test naming conventions within IAITestHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAITestHint.NamingConvention enum with 5 source-declared constants in existing order.
     */
    enum NamingConvention {
        /** given_when_then (BDD style).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.NamingConvention#GIVEN_WHEN_THEN token within Test naming conventions: given_when_then (BDD style). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.NamingConvention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GIVEN_WHEN_THEN,

        /** should_expectedBehavior_when_condition.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.NamingConvention#SHOULD_WHEN token within Test naming conventions: should_expectedBehavior_when_condition. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.NamingConvention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SHOULD_WHEN,

        /** methodName_stateUnderTest_expectedBehavior.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.NamingConvention#METHOD_STATE_EXPECTED token within Test naming conventions: methodName_stateUnderTest_expectedBehavior. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.NamingConvention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        METHOD_STATE_EXPECTED,

        /** test + method name (legacy).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.NamingConvention#TEST_PREFIX token within Test naming conventions: test + method name (legacy). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.NamingConvention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TEST_PREFIX,

        /** Descriptive sentence.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAITestHint.NamingConvention#DESCRIPTIVE token within Test naming conventions: Descriptive sentence. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAITestHint.NamingConvention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DESCRIPTIVE
    }
}
