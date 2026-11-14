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

import jdk.internal.m3.paradigm.annotation.IMethodSpec;
import jdk.internal.m3.paradigm.annotation.IFieldSpec;
import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IValidationSpec - Validation specification for AI code generation.
 *
 * <h2>Validation Patterns</h2>
 *
 * <pre>
 * Validation patterns in Java:
 *   - Jakarta Bean Validation (JSR-380)
 *   - @Valid, @NotNull, @Size, @PatternKind
 *   - Spring @Validated
 *   - Custom validators
 *   - Design by Contract (DbC)
 *
 * &#64;IValidationSpec specifies validation rules in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IUserService {
 *
 *     // Parameter validation
 * &#64;IValidationSpec(params = {&#64;IValidationSpec.Param(name = "email", rules = {"@Email", "@NotBlank"}),
 * &#64;IValidationSpec.Param(name = "age", rules = {"@Min(0)", "@Max(150)"})})
 * User createUser(String email, int age);
 *
 * // Design by Contract
 * &#64;IValidationSpec(preconditions = {"id != null", "id.length() > 0"}, postconditions = {"result != null",
 * "result.id.equals(id)"}, invariants = {"userCount >= 0"})
 * User findById(String id);
 *
 * // Custom validation
 * &#64;IValidationSpec(validator = UserValidator.class, message = "Invalid user data")
 * void updateUser(User user);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Validation Specification
 * @see IMethodSpec
 * @see IFieldSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares parameter and value constraints, contract expressions, validator selection and failure handling; it evaluates no expression and performs no validation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IValidationSpec.java}
 * blob {@code 3e7c14819dfe7755b8b8de25a0887033eef25856} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IValidationSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined validation pattern.
     *
     * @return option
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#option intent: Predefined validation pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common validation patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IValidationSpec.Option choices described as Common validation patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 8 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Not null validation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.Option#NOT_NULL policy choice: Not null validation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.Option; its declared order and identity are preserved.
         */
        NOT_NULL,

        /** Not empty (string/collection).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.Option#NOT_EMPTY policy choice: Not empty (string/collection). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.Option; its declared order and identity are preserved.
         */
        NOT_EMPTY,

        /** Email format.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.Option#EMAIL policy choice: Email format. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.Option; its declared order and identity are preserved.
         */
        EMAIL,

        /** URL format.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.Option#URL policy choice: URL format. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.Option; its declared order and identity are preserved.
         */
        URL,

        /** UUID format.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.Option#UUID policy choice: UUID format. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.Option; its declared order and identity are preserved.
         */
        UUID,

        /** Positive number.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.Option#POSITIVE policy choice: Positive number. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.Option; its declared order and identity are preserved.
         */
        POSITIVE,

        /** Design by Contract.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.Option#CONTRACT policy choice: Design by Contract. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.Option; its declared order and identity are preserved.
         */
        CONTRACT
    }

    // --- PARAMETER VALIDATION ---

    /**
     * Parameter validation rules.
     *
     * @return parameter rules
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#params intent: Parameter validation rules. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Param[]}; default expression {@code {}}.
     */
    Param[] params() default {};

    /** Parameter validation definition.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a parameter name, validation-rule strings and message; it validates no argument and has implicit CLASS retention with no explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Param {

        /** Parameter name.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IValidationSpec.Param#name intent: Parameter name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String name();

        /** Validation rules (Jakarta annotations as strings).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IValidationSpec.Param#rules intent: Validation rules (Jakarta annotations as strings). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
         */
        String[] rules() default {};

        /** Custom message.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IValidationSpec.Param#message intent: Custom message. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String message() default "";
    }

    // --- BUILT-IN RULES ---

    /**
     * Not null constraint.
     *
     * @return true for not null
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#notNull intent: Not null constraint. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean notNull() default false;

    /**
     * Not empty constraint.
     *
     * @return true for not empty
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#notEmpty intent: Not empty constraint. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean notEmpty() default false;

    /**
     * Not blank constraint (string).
     *
     * @return true for not blank
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#notBlank intent: Not blank constraint (string). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean notBlank() default false;

    /**
     * Minimum value/length.
     *
     * @return min value
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#min intent: Minimum value/length. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code Long.MIN_VALUE}.
     */
    long min() default Long.MIN_VALUE;

    /**
     * Maximum value/length.
     *
     * @return max value
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#max intent: Maximum value/length. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code Long.MAX_VALUE}.
     */
    long max() default Long.MAX_VALUE;

    /**
     * Regex pattern.
     *
     * @return pattern
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#pattern intent: Regex pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String pattern() default "";

    /**
     * Email format.
     *
     * @return true for email
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#email intent: Email format. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean email() default false;

    // --- DESIGN BY CONTRACT ---

    /**
     * Preconditions (must be true before execution).
     *
     * @return precondition expressions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#preconditions intent: Preconditions (must be true before execution). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] preconditions() default {};

    /**
     * Postconditions (must be true after execution). Use "result" to reference
     * return value.
     *
     * @return postcondition expressions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#postconditions intent: Postconditions (must be true after execution). Use "result" to reference return value. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] postconditions() default {};

    /**
     * Class invariants (always true).
     *
     * @return invariant expressions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#invariants intent: Class invariants (always true). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] invariants() default {};

    // --- CUSTOM VALIDATOR ---

    /**
     * Custom validator class. Must implement Predicate or ConstraintValidator.
     *
     * @return validator class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#validator intent: Custom validator class. Must implement Predicate or ConstraintValidator. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> validator() default Void.class;

    /**
     * Validation groups.
     *
     * @return groups
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#groups intent: Validation groups. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>[]}; default expression {@code {}}.
     */
    Class<?>[] groups() default {};

    // --- ERROR HANDLING ---

    /**
     * Validation error message.
     *
     * @return message
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#message intent: Validation error message. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String message() default "";

    /**
     * Action on validation failure.
     *
     * @return action
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#onFailure intent: Action on validation failure. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code OnFailure}; default expression {@code OnFailure.EXCEPTION}.
     */
    OnFailure onFailure() default OnFailure.EXCEPTION;

    /** Failure actions.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IValidationSpec.OnFailure choices described as Failure actions. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum OnFailure {
        /** Throw ValidationException.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.OnFailure#EXCEPTION policy choice: Throw ValidationException. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.OnFailure; its declared order and identity are preserved.
         */
        EXCEPTION,

        /** Return null.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.OnFailure#RETURN_NULL policy choice: Return null. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.OnFailure; its declared order and identity are preserved.
         */
        RETURN_NULL,

        /** Return default value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.OnFailure#RETURN_DEFAULT policy choice: Return default value. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.OnFailure; its declared order and identity are preserved.
         */
        RETURN_DEFAULT,

        /** Log and continue.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.OnFailure#LOG_CONTINUE policy choice: Log and continue. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.OnFailure; its declared order and identity are preserved.
         */
        LOG_CONTINUE
    }

    /**
     * Default value on failure.
     *
     * @return default expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#defaultValue intent: Default value on failure. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String defaultValue() default "";

    // --- TIMING ---

    /**
     * When to validate.
     *
     * @return timing
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#when intent: When to validate. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code When}; default expression {@code When.BEFORE}.
     */
    When when() default When.BEFORE;

    /** Validation timing.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IValidationSpec.When choices described as Validation timing. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 3 constants in the existing source order.
     */
    enum When {
        /** Before method execution.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.When#BEFORE policy choice: Before method execution. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.When; its declared order and identity are preserved.
         */
        BEFORE,

        /** After method execution.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.When#AFTER policy choice: After method execution. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.When; its declared order and identity are preserved.
         */
        AFTER,

        /** Both before and after.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IValidationSpec.When#BOTH policy choice: Both before and after. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IValidationSpec.When; its declared order and identity are preserved.
         */
        BOTH
    }

    // --- DESCRIPTION ---

    /**
     * Validation rule description.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#value intent: Validation rule description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IValidationSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.HAIKU}.
     */
    Model.ModelType model() default Model.ModelType.HAIKU;
}
