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

package jdk.internal.m3.paradigm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IParamSpec - Parameter-level specification for LLM code generation.
 *
 * <h2>Purpose</h2>
 *
 * <p>
 * Annotates method parameters to specify their validation rules, constraints,
 * and expected values for LLM-based code generation. Similar to {@link ISpec}
 * but at parameter granularity.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	public class UserService {
 *
 * 		public User createUser(@IParamSpec(value = "User's email address - must be unique in system", constraints = {
 * 				"RFC 5322 compliant email format", "Lowercase normalized", "Maximum 255 characters"}, invariants = {
 * 						"Must not be null or empty", "Must contain exactly one '@'",
 * 						"Domain must have valid TLD"}, examples = {"user@example.com → Valid",
 * 								"ADMIN@COMPANY.COM → admin@company.com (normalized)", "null → NullPointerException",
 * 								"invalid.email → IllegalArgumentException"}, nullable = false, pattern = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$") String email,
 *
 * 				&#64;IParamSpec(value = "User's age - must be adult", min = "18", max = "120", invariants = {
 * 						"Must be >= 18", "Must be &lt;= 120"}) int age,
 *
 * 				&#64;IParamSpec(value = "Optional middle name", nullable = true, defaultValue = "null") String middleName) {
 * 			// Implementation with parameter validation
 * 		}
 * 	}
 * }
 * </pre>
 *
 * <h2>Parameter Categories</h2>
 *
 * <ul>
 * <li><strong>Required parameters</strong> - nullable=false, must be validated
 * <li><strong>Optional parameters</strong> - nullable=true, default values
 * <li><strong>Bounded parameters</strong> - min/max range constraints
 * <li><strong>Patterned parameters</strong> - regex validation for strings
 * <li><strong>Enum parameters</strong> - limited set of valid values
 * </ul>
 *
 * <h2>Validation Generation</h2>
 *
 * <pre>
 * 1. Scan parameters annotated with @IParamSpec
 * 2. Generate null checks for non-nullable parameters
 * 3. Generate range checks for min/max constraints
 * 4. Generate regex validation for pattern constraints
 * 5. Generate custom validation for invariants
 * 6. Generate test cases for all validation paths
 * </pre>
 *
 * <h2>Integration with Method Specs</h2>
 *
 * <p>
 * When used with {@link IMethodSpec}, parameter specifications contribute to:
 *
 * <ul>
 * <li>Method preconditions - parameter validation becomes preconditions
 * <li>Test case generation - parameter examples become test inputs
 * <li>Documentation - parameter descriptions enhance method JavaDoc
 * </ul>
 *
 * @see ISpec
 * @see IFieldSpec
 * @see IMethodSpec
 * @see Stability
 * @since Phase 1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/IParamSpec.java}
 * blob {@code 46c8b6de89d7d5a74734f3c4531abe756e1686a2} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP LazyInit batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IParamSpec {

    // --- SPECIFICATION ---

    /**
     * Natural language description of the parameter purpose and expected values.
     *
     * @return parameter description
     */
    String value();

    /**
     * Constraints that guide validation (soft requirements). These influence
     * validation logic.
     *
     * @return constraint descriptions
     */
    String[] constraints() default {};

    /**
     * Invariants that MUST hold (hard requirements). These become validation
     * assertions.
     *
     * @return invariant expressions
     */
    String[] invariants() default {};

    /**
     * Example values and their expected behavior. Format: "value → expected_result"
     *
     * @return example pairs
     */
    String[] examples() default {};

    // --- VALIDATION RULES ---

    /**
     * Whether null values are allowed for this parameter.
     *
     * @return true if null is allowed (default: true)
     */
    boolean nullable() default true;

    /**
     * Default value if parameter is not provided (for optional parameters).
     *
     * @return default value expression (empty = no default)
     */
    String defaultValue() default "";

    /**
     * Minimum value for numeric parameters (inclusive).
     *
     * @return minimum value (empty = no minimum)
     */
    String min() default "";

    /**
     * Maximum value for numeric parameters (inclusive).
     *
     * @return maximum value (empty = no maximum)
     */
    String max() default "";

    /**
     * Regular expression pattern for string parameters.
     *
     * @return regex pattern (empty = no pattern)
     */
    String pattern() default "";

    /**
     * Valid values for enum-like parameters.
     *
     * @return array of valid values (empty = no restriction)
     */
    String[] validValues() default {};

    /**
     * Minimum length for string/array parameters.
     *
     * @return minimum length (-1 = no minimum)
     */
    int minLength() default -1;

    /**
     * Maximum length for string/array parameters.
     *
     * @return maximum length (-1 = no maximum)
     */
    int maxLength() default -1;

    // --- MODEL SELECTION ---

    /**
     * Preferred model for generating validation code.
     *
     * @return model type (default: SONNET)
     */
    Model.ModelType model() default Model.ModelType.SONNET;

    /**
     * Fallback model if preferred is unavailable.
     *
     * @return fallback model type
     */
    Model.ModelType fallback() default Model.ModelType.OLLAMA;

    // --- STABILITY & QUALITY ---

    /**
     * Target stability level for generated validation code.
     *
     * @return stability level
     */
    Stability.Level stability() default Stability.Level.ALPHA;

    /**
     * Test class to verify generated validation logic.
     *
     * @return test class (Void.class = no test)
     */
    Class<?> test() default Void.class;
}
