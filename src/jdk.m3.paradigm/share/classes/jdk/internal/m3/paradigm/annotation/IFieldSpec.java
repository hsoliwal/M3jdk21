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
 * IFieldSpec - Field-level specification for LLM code generation.
 *
 * <h2>Purpose</h2>
 *
 * <p>
 * Annotates fields to specify their behavior, constraints, and invariants for
 * LLM-based code generation. Similar to {@link ISpec} but at field granularity.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	public class UserAccount {
 *
 * 		&#64;IFieldSpec(value = "User's email address - must be valid and unique", constraints = {
 * 				"Format: RFC 5322 compliant email", "Lowercase normalized", "Maximum 255 characters"}, invariants = {
 * 						"Must not be null or empty", "Must contain exactly one '@' character",
 * 						"Must end with valid TLD"}, examples = {"user@example.com → Valid",
 * 								"ADMIN@COMPANY.COM → admin@company.com (normalized)",
 * 								"invalid.email → ValidationException"})
 * 		private String email;
 *
 * 		&#64;IFieldSpec(value = "Account creation timestamp - immutable after initialization", constraints = {
 * 				"Must be in UTC",
 * 				"ISO 8601 format"}, invariants = {"Cannot be in the future", "Cannot be modified after creation"})
 * 		private final Instant createdAt = Instant.now();
 * 	}
 * }
 * </pre>
 *
 * <h2>Field Types</h2>
 *
 * <ul>
 * <li><strong>Primitive fields</strong> - Specify valid ranges, default values
 * <li><strong>Object fields</strong> - Specify null-safety, validation rules
 * <li><strong>Collection fields</strong> - Specify element constraints, size
 * limits
 * <li><strong>Static fields</strong> - Specify constants, immutability
 * <li><strong>Final fields</strong> - Specify initialization contract
 * </ul>
 *
 * <h2>Generation Process</h2>
 *
 * <pre>
 * 1. Scan fields annotated with @IFieldSpec
 * 2. Generate validation logic for constraints
 * 3. Generate getter/setter with validation
 * 4. Generate builder methods if needed
 * 5. Generate test cases for invariants
 * </pre>
 *
 * @see ISpec
 * @see IMethodSpec
 * @see IParamSpec
 * @see Stability
 * @since Phase 1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/IFieldSpec.java}
 * blob {@code 77ea843b3f1b0cb99249dc7913f18708a13166be} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP LazyInit batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IFieldSpec {

    // --- SPECIFICATION ---

    /**
     * Natural language description of the field purpose and behavior.
     *
     * @return field description
     */
    String value();

    /**
     * Constraints that guide generation (soft requirements). These influence
     * validation logic.
     *
     * @return constraint descriptions
     */
    String[] constraints() default {};

    /**
     * Invariants that MUST hold (hard requirements). These become assertions in
     * getters/setters.
     *
     * @return invariant expressions
     */
    String[] invariants() default {};

    /**
     * Example values and their expected behavior. Format: "value ->
     * expected_result"
     *
     * @return example pairs
     */
    String[] examples() default {};

    // --- VALIDATION ---

    /**
     * Default value for the field if unspecified.
     *
     * @return default value expression (empty = no default)
     */
    String defaultValue() default "";

    /**
     * Whether null values are allowed for object fields.
     *
     * @return true if null is allowed
     */
    boolean nullable() default true;

    /**
     * Minimum value for numeric fields (inclusive).
     *
     * @return minimum value (empty = no minimum)
     */
    String min() default "";

    /**
     * Maximum value for numeric fields (inclusive).
     *
     * @return maximum value (empty = no maximum)
     */
    String max() default "";

    /**
     * Regular expression pattern for string fields.
     *
     * @return regex pattern (empty = no pattern)
     */
    String pattern() default "";

    // --- MODEL SELECTION ---

    /**
     * Preferred model for generation.
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

    /**
     * Maximum generation attempts before failing.
     *
     * @return max retries (default: 3)
     */
    int maxRetries() default 3;
}
