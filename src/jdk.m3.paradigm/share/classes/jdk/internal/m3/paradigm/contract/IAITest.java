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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AITest - Records test results for AI-generated code.
 *
 * <h2>Test-Driven Generation</h2>
 *
 * <pre>
 * IOP supports test-driven code generation:
 *
 * 1. Human writes tests (the spec)
 * 2. AI generates implementation
 * 3. Tests run automatically
 * 4. @AITest captures results
 * 5. If failed: AI regenerates with test feedback
 * 6. Cycle until tests pass (max retries)
 *
 * "Tests ARE the specification. Tests ARE the contract."
 * </pre>
 *
 * <h2>Usage (Auto-Applied)</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;IAIGenerated(model = "OPUS", specHash = "sha256:abc...", testsPassed = true)
 * 	&#64;AITest(passed = true, testCount = 15, passedCount = 15, failedCount = 0, coverage = 0.87, testClass = "UserServiceTest", executionMs = 1234)
 * 	public class UserServiceImpl implements IUserService {
 * 		// Implementation verified by tests
 * 	}
 * }
 * </pre>
 *
 * <h2>Test Feedback Loop</h2>
 *
 * <pre>
 * If tests fail:
 *   &#64;AITest(
 *       passed = false,
 *       failedCount = 2,
 *       failedMethods = {"testNullInput", "testEdgeCase"},
 *       failureMessages = {
 *           "Expected: Optional.empty(), Got: NullPointerException",
 *           "Expected: 0, Got: -1"
 *       }
 *   )
 *
 * AI reads failure messages → regenerates with context → tests again
 * </pre>
 *
 * @since IOP 1.0 - Test Tracking
 * @see IAIGenerated
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares a required passed value and sixteen constant defaults for reported test counters, coverage, failures, duration, framework and retries. Java compilation preserves explicit values in CLASS-retained type or method metadata. The annotation runs no tests, calculates no coverage and authenticates no reported result.
 * @m3.dataStructures Documented annotation targeting TYPE and METHOD with CLASS retention, one required boolean element and sixteen default-valued elements. It has no Repeatable annotation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAITest.java}
 * blob {@code 0f8721b9b9398a2b3b93e975e246f03f44982ad0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface IAITest {

    /**
     * Whether all tests passed.
     *
     * @return true if all passed
     * @m3.pattern synexia:aiop
     * @m3.applicability algorithm=not-applicable; reason=This passed annotation element declares a boolean result with neither an executable body nor an annotation default expression.; review=iop-annotation-contracts-declarations/reviewed-declarations.json#IAITest-2440
     * @m3.dataStructures Bodyless annotation element returning boolean. An explicit passed value is required and Java performs no result validation.
     */
    boolean passed();

    /**
     * Total number of test methods run.
     *
     * @return test count
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>0</code> for testCount. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning int, with the original exact default expression <code>0</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    int testCount() default 0;

    /**
     * Number of tests that passed.
     *
     * @return passed count
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>0</code> for passedCount. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning int, with the original exact default expression <code>0</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    int passedCount() default 0;

    /**
     * Number of tests that failed.
     *
     * @return failed count
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>0</code> for failedCount. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning int, with the original exact default expression <code>0</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    int failedCount() default 0;

    /**
     * Number of tests that were skipped.
     *
     * @return skipped count
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>0</code> for skippedCount. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning int, with the original exact default expression <code>0</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    int skippedCount() default 0;

    /**
     * Code coverage percentage (0.0 - 1.0).
     *
     * @return coverage ratio
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>0.0</code> for coverage. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning double, with the original exact default expression <code>0.0</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    double coverage() default 0.0;

    /**
     * Line coverage percentage.
     *
     * @return line coverage
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>0.0</code> for lineCoverage. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning double, with the original exact default expression <code>0.0</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    double lineCoverage() default 0.0;

    /**
     * Branch coverage percentage.
     *
     * @return branch coverage
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>0.0</code> for branchCoverage. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning double, with the original exact default expression <code>0.0</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    double branchCoverage() default 0.0;

    /**
     * Test class used for verification.
     *
     * @return test class name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>""</code> for testClass. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning String, with the original exact default expression <code>""</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    String testClass() default "";

    /**
     * Failed test method names.
     *
     * @return failed method names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>{}</code> for failedMethods. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning String[], with the original exact default expression <code>{}</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    String[] failedMethods() default {};

    /**
     * Failure messages from tests. Used as feedback for regeneration.
     *
     * @return failure messages
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>{}</code> for failureMessages. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning String[], with the original exact default expression <code>{}</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    String[] failureMessages() default {};

    /**
     * Test execution time in milliseconds.
     *
     * @return execution time
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>0</code> for executionMs. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning int, with the original exact default expression <code>0</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    int executionMs() default 0;

    /**
     * Test framework used.
     *
     * @return framework name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>"JUnit"</code> for framework. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning String, with the original exact default expression <code>"JUnit"</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    String framework() default "JUnit";

    /**
     * Number of regeneration attempts due to test failures.
     *
     * @return attempt count
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>0</code> for regenerationAttempts. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning int, with the original exact default expression <code>0</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    int regenerationAttempts() default 0;

    /**
     * Maximum retries allowed before giving up.
     *
     * @return max retries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>3</code> for maxRetries. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning int, with the original exact default expression <code>3</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    int maxRetries() default 3;

    /**
     * Whether mutation testing was run.
     *
     * @return true if mutation testing ran
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>false</code> for mutationTested. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning boolean, with the original exact default expression <code>false</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    boolean mutationTested() default false;

    /**
     * Mutation score (0.0 - 1.0). Percentage of mutants killed.
     *
     * @return mutation score
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares the constant annotation-element default <code>0.0</code> for mutationScore. Java exposes that default through annotation method metadata when the value is omitted. The declaration performs no test execution, numeric validation or result authentication.
     * @m3.dataStructures Annotation element returning double, with the original exact default expression <code>0.0</code>. Array defaults remain annotation-element arrays under the JDK contract.
     */
    double mutationScore() default 0.0;
}
