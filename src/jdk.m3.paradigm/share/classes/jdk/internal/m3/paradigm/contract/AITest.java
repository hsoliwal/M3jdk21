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

import java.lang.annotation.*;

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
 *     &#64;code
 *     &#64;AIGenerated(model = "OPUS", specHash = "sha256:abc...", testsPassed = true)
 *     &#64;AITest(passed = true, testCount = 15, passedCount = 15, failedCount = 0, coverage = 0.87, testClass = "UserServiceTest", executionMs = 1234)
 *     public class UserServiceImpl implements IUserService {
 *         // Implementation verified by tests
 *     }
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
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AITest.java}
 * blob {@code 300dbb8f2b5414143f9be6c862e47ce8fbf49f2f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface AITest {

    /**
     * Whether all tests passed.
     *
     * @return true if all passed
     */
    boolean passed();

    /**
     * Total number of test methods run.
     *
     * @return test count
     */
    int testCount() default 0;

    /**
     * Number of tests that passed.
     *
     * @return passed count
     */
    int passedCount() default 0;

    /**
     * Number of tests that failed.
     *
     * @return failed count
     */
    int failedCount() default 0;

    /**
     * Number of tests that were skipped.
     *
     * @return skipped count
     */
    int skippedCount() default 0;

    /**
     * Code coverage percentage (0.0 - 1.0).
     *
     * @return coverage ratio
     */
    double coverage() default 0.0;

    /**
     * Line coverage percentage.
     *
     * @return line coverage
     */
    double lineCoverage() default 0.0;

    /**
     * Branch coverage percentage.
     *
     * @return branch coverage
     */
    double branchCoverage() default 0.0;

    /**
     * Test class used for verification.
     *
     * @return test class name
     */
    String testClass() default "";

    /**
     * Failed test method names.
     *
     * @return failed method names
     */
    String[] failedMethods() default {};

    /**
     * Failure messages from tests. Used as feedback for regeneration.
     *
     * @return failure messages
     */
    String[] failureMessages() default {};

    /**
     * Test execution time in milliseconds.
     *
     * @return execution time
     */
    int executionMs() default 0;

    /**
     * Test framework used.
     *
     * @return framework name
     */
    String framework() default "JUnit";

    /**
     * Number of regeneration attempts due to test failures.
     *
     * @return attempt count
     */
    int regenerationAttempts() default 0;

    /**
     * Maximum retries allowed before giving up.
     *
     * @return max retries
     */
    int maxRetries() default 3;

    /**
     * Whether mutation testing was run.
     *
     * @return true if mutation testing ran
     */
    boolean mutationTested() default false;

    /**
     * Mutation score (0.0 - 1.0). Percentage of mutants killed.
     *
     * @return mutation score
     */
    double mutationScore() default 0.0;
}
