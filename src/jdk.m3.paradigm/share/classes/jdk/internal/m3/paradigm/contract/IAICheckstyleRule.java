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
 * AICheckstyleRule - IOP annotation for Checkstyle rule enforcement.
 *
 * <h2>IOP Authority: Checkstyle Enforcement</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  "If a value is important enough to name,                                 ║
 * ║   it is important enough to centralize and protect."                      ║
 * ║                                                                           ║
 * ║  Checkstyle rules are AUTHORITY, not suggestions.                         ║
 * ║  Violations are compile-time rejections.                                  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Constant Creation Contract (Non-Negotiable)</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  ALLOWED FORMS (only):                                                  │
 * │  ✅ public static final TYPE NAME = value;                              │
 * │  ✅ Interface constants (for protocols/contracts only)                  │
 * │                                                                         │
 * │  FORBIDDEN FORMS (hard reject):                                         │
 * │  ❌ instance final fields as constants                                  │
 * │  ❌ inline magic numbers                                                │
 * │  ❌ enum abuse for constants                                            │
 * │  ❌ static without final                                                │
 * │  ❌ mutable objects (List, Map, Set, arrays)                            │
 * │  ❌ constant creation inside methods                                    │
 * │  ❌ configuration-as-constants                                          │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @since IOP 1.0 - Checkstyle Enforcement
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares selected source-checking rule tokens and desired violation severity; it configures no Checkstyle module and makes no build succeed or fail by itself.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.PACKAGE, ElementType.TYPE}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAICheckstyleRule.java}
 * blob {@code 4f413e0310280c388b8d938cddb75c679e3e575b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAICheckstyleRule {

    /**
     * Checkstyle rules to enforce.
     *
     * @return rules to enforce
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICheckstyleRule#value intent: Checkstyle rules to enforce. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Rule[]}; an explicit value is required.
     */
    Rule[] value();

    /**
     * Severity level for violations.
     *
     * @return severity
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICheckstyleRule#severity intent: Severity level for violations. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Severity}; default expression {@code Severity.ERROR}.
     */
    Severity severity() default Severity.ERROR;

    /** Checkstyle rules mapped to IOP contracts.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAICheckstyleRule.Rule choices described as Checkstyle rules mapped to IOP contracts. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 31 constants in the existing source order.
     */
    enum Rule {
        // --- CONSTANT RULES (IOP LOCKED) ---

        /**
         * Constants must be UPPER_SNAKE_CASE.
         *
         * <pre>
         * &lt;module name="ConstantName"&gt;
         *     &lt;property name="format" value="^[A-Z][A-Z0-9_]*$"/&gt;
         * &lt;/module&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#CONSTANT_NAME policy choice: Constants must be UPPER_SNAKE_CASE. This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        CONSTANT_NAME,

        /**
         * No magic numbers (except 0, 1).
         *
         * <pre>
         * &lt;module name="MagicNumber"&gt;
         *     &lt;property name="ignoreNumbers" value="0,1"/&gt;
         * &lt;/module&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#NO_MAGIC_NUMBERS policy choice: No magic numbers (except 0, 1). This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        NO_MAGIC_NUMBERS,

        /**
         * Constants must be static final. Custom AST rule: reject static without final,
         * final without static.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#STATIC_FINAL_REQUIRED policy choice: Constants must be static final. Custom AST rule: reject static without final, final without static. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        STATIC_FINAL_REQUIRED,

        /**
         * No mutable constant types (List, Map, Set, arrays). Custom AST rule: reject
         * unless @ImmutableConstant.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#NO_MUTABLE_CONSTANTS policy choice: No mutable constant types (List, Map, Set, arrays). Custom AST rule: reject unless @ImmutableConstant. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        NO_MUTABLE_CONSTANTS,

        /**
         * Utility classes must hide constructor.
         *
         * <pre>
         * &lt;module name="HideUtilityClassConstructor"/&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#HIDE_UTILITY_CONSTRUCTOR policy choice: Utility classes must hide constructor. This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        HIDE_UTILITY_CONSTRUCTOR,

        // --- NAMING RULES (IOP LOCKED) ---

        /**
         * IOP naming: I* prefix for interfaces.
         *
         * <pre>
         * &lt;module name="TypeName"&gt;
         *     &lt;property name="format" value="^I[A-Z][a-zA-Z0-9]*$"/&gt;
         *     &lt;property name="tokens" value="INTERFACE_DEF"/&gt;
         * &lt;/module&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#INTERFACE_PREFIX_I policy choice: IOP naming: I* prefix for interfaces. This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        INTERFACE_PREFIX_I,

        /**
         * IOP naming: Abstract* prefix for abstract classes.
         *
         * <pre>
         * &lt;module name="AbstractClassName"&gt;
         *     &lt;property name="format" value="^Abstract[A-Z][a-zA-Z0-9]*$"/&gt;
         * &lt;/module&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#ABSTRACT_PREFIX policy choice: IOP naming: Abstract* prefix for abstract classes. This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        ABSTRACT_PREFIX,

        /**
         * Method names must be camelCase.
         *
         * <pre>
         * &lt;module name="MethodName"&gt;
         *     &lt;property name="format" value="^[a-z][a-zA-Z0-9]*$"/&gt;
         * &lt;/module&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#METHOD_CAMEL_CASE policy choice: Method names must be camelCase. This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        METHOD_CAMEL_CASE,

        /** Parameter names must be camelCase.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#PARAMETER_CAMEL_CASE policy choice: Parameter names must be camelCase. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        PARAMETER_CAMEL_CASE,

        /** Local variable names must be camelCase.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#LOCAL_CAMEL_CASE policy choice: Local variable names must be camelCase. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        LOCAL_CAMEL_CASE,

        // --- SIZE RULES ---

        /**
         * Maximum method length (default: 20 lines).
         *
         * <pre>
         * &lt;module name="MethodLength"&gt;
         *     &lt;property name="max" value="20"/&gt;
         * &lt;/module&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#MAX_METHOD_LENGTH policy choice: Maximum method length (default: 20 lines). This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        MAX_METHOD_LENGTH,

        /** Maximum class length (default: 500 lines).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#MAX_CLASS_LENGTH policy choice: Maximum class length (default: 500 lines). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        MAX_CLASS_LENGTH,

        /** Maximum line length (default: 120 characters).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#MAX_LINE_LENGTH policy choice: Maximum line length (default: 120 characters). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        MAX_LINE_LENGTH,

        /**
         * Maximum parameters (default: 5).
         *
         * <pre>
         * &lt;module name="ParameterNumber"&gt;
         *     &lt;property name="max" value="5"/&gt;
         * &lt;/module&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#MAX_PARAMETERS policy choice: Maximum parameters (default: 5). This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        MAX_PARAMETERS,

        /** Maximum cyclomatic complexity (default: 10).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#MAX_COMPLEXITY policy choice: Maximum cyclomatic complexity (default: 10). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        MAX_COMPLEXITY,

        // --- MODIFIER RULES ---

        /**
         * Modifier order: public/protected/private, static, final, etc.
         *
         * <pre>
         * &lt;module name="ModifierOrder"/&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#MODIFIER_ORDER policy choice: Modifier order: public/protected/private, static, final, etc. This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        MODIFIER_ORDER,

        /** Redundant modifiers (interface methods are public).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#REDUNDANT_MODIFIER policy choice: Redundant modifiers (interface methods are public). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        REDUNDANT_MODIFIER,

        /** Final class for non-extendable classes.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#FINAL_CLASS policy choice: Final class for non-extendable classes. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        FINAL_CLASS,

        // --- DOCUMENTATION RULES ---

        /**
         * JavaDoc required for public API.
         *
         * <pre>
         * &lt;module name="MissingJavadocMethod"/&gt;
         * &lt;module name="MissingJavadocType"/&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#JAVADOC_REQUIRED policy choice: JavaDoc required for public API. This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        JAVADOC_REQUIRED,

        /**
         * JavaDoc must have @param, @return, @throws.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#JAVADOC_COMPLETE policy choice: JavaDoc must have @param, @return, @throws. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        JAVADOC_COMPLETE,

        // --- CODE STYLE RULES ---

        /**
         * Braces on same line (K&amp;R style).
         *
         * <pre>
         * &lt;module name="LeftCurly"&gt;
         *     &lt;property name="option" value="eol"/&gt;
         * &lt;/module&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#BRACES_SAME_LINE policy choice: Braces on same line (K&amp;R style). This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        BRACES_SAME_LINE,

        /** No empty blocks.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#NO_EMPTY_BLOCKS policy choice: No empty blocks. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        NO_EMPTY_BLOCKS,

        /** One statement per line.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#ONE_STATEMENT_PER_LINE policy choice: One statement per line. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        ONE_STATEMENT_PER_LINE,

        /** No multiple variable declarations per line.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#ONE_VARIABLE_PER_LINE policy choice: No multiple variable declarations per line. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        ONE_VARIABLE_PER_LINE,

        // --- IMPORT RULES ---

        /**
         * No star imports.
         *
         * <pre>
         * &lt;module name="AvoidStarImport"/&gt;
         * </pre>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#NO_STAR_IMPORTS policy choice: No star imports. This token does not install the example checker configuration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        NO_STAR_IMPORTS,

        /** No unused imports.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#NO_UNUSED_IMPORTS policy choice: No unused imports. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        NO_UNUSED_IMPORTS,

        /** Import order (static first, then java, then third-party).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#IMPORT_ORDER policy choice: Import order (static first, then java, then third-party). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        IMPORT_ORDER,

        // --- BEST PRACTICES ---

        /** Equals and hashCode must be paired.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#EQUALS_HASHCODE policy choice: Equals and hashCode must be paired. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        EQUALS_HASHCODE,

        /** No System.out or System.err.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#NO_SYSTEM_OUT policy choice: No System.out or System.err. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        NO_SYSTEM_OUT,

        /** Use StringBuilder for string concatenation in loops.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#STRING_BUILDER_IN_LOOPS policy choice: Use StringBuilder for string concatenation in loops. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        STRING_BUILDER_IN_LOOPS,

        /** No empty catch blocks.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Rule#NO_EMPTY_CATCH policy choice: No empty catch blocks. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Rule; its declared order and identity are preserved.
         */
        NO_EMPTY_CATCH
    }

    /** Violation severity.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAICheckstyleRule.Severity choices described as Violation severity. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Severity {
        /** Informational only.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Severity#INFO policy choice: Informational only. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Severity; its declared order and identity are preserved.
         */
        INFO,

        /** Warning - build continues.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Severity#WARNING policy choice: Warning - build continues. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Severity; its declared order and identity are preserved.
         */
        WARNING,

        /** Error - build fails.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Severity#ERROR policy choice: Error - build fails. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Severity; its declared order and identity are preserved.
         */
        ERROR,

        /** Ignore - rule disabled.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICheckstyleRule.Severity#IGNORE policy choice: Ignore - rule disabled. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICheckstyleRule.Severity; its declared order and identity are preserved.
         */
        IGNORE
    }
}
