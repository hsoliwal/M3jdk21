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
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AICheckstyleRule.java}
 * blob {@code ba241f569cfbe77c9eea203f5aea34a4bf803137} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AICheckstyleRule {

    /**
     * Checkstyle rules to enforce.
     *
     * @return rules to enforce
     */
    Rule[] value();

    /**
     * Severity level for violations.
     *
     * @return severity
     */
    Severity severity() default Severity.ERROR;

    /** Checkstyle rules mapped to IOP contracts. */
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
         */
        NO_MAGIC_NUMBERS,

        /**
         * Constants must be static final. Custom AST rule: reject static
         * without final, final without static.
         */
        STATIC_FINAL_REQUIRED,

        /**
         * No mutable constant types (List, Map, Set, arrays). Custom AST rule:
         * reject unless @ImmutableConstant.
         */
        NO_MUTABLE_CONSTANTS,

        /**
         * Utility classes must hide constructor.
         *
         * <pre>
         * &lt;module name="HideUtilityClassConstructor"/&gt;
         * </pre>
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
         */
        METHOD_CAMEL_CASE,

        /** Parameter names must be camelCase. */
        PARAMETER_CAMEL_CASE,

        /** Local variable names must be camelCase. */
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
         */
        MAX_METHOD_LENGTH,

        /** Maximum class length (default: 500 lines). */
        MAX_CLASS_LENGTH,

        /** Maximum line length (default: 120 characters). */
        MAX_LINE_LENGTH,

        /**
         * Maximum parameters (default: 5).
         *
         * <pre>
         * &lt;module name="ParameterNumber"&gt;
         *     &lt;property name="max" value="5"/&gt;
         * &lt;/module&gt;
         * </pre>
         */
        MAX_PARAMETERS,

        /** Maximum cyclomatic complexity (default: 10). */
        MAX_COMPLEXITY,

        // --- MODIFIER RULES ---

        /**
         * Modifier order: public/protected/private, static, final, etc.
         *
         * <pre>
         * &lt;module name="ModifierOrder"/&gt;
         * </pre>
         */
        MODIFIER_ORDER,

        /** Redundant modifiers (interface methods are public). */
        REDUNDANT_MODIFIER,

        /** Final class for non-extendable classes. */
        FINAL_CLASS,

        // --- DOCUMENTATION RULES ---

        /**
         * JavaDoc required for public API.
         *
         * <pre>
         * &lt;module name="MissingJavadocMethod"/&gt;
         * &lt;module name="MissingJavadocType"/&gt;
         * </pre>
         */
        JAVADOC_REQUIRED,

        /**
         * JavaDoc must have @param, @return, @throws.
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
         */
        BRACES_SAME_LINE,

        /** No empty blocks. */
        NO_EMPTY_BLOCKS,

        /** One statement per line. */
        ONE_STATEMENT_PER_LINE,

        /** No multiple variable declarations per line. */
        ONE_VARIABLE_PER_LINE,

        // --- IMPORT RULES ---

        /**
         * No star imports.
         *
         * <pre>
         * &lt;module name="AvoidStarImport"/&gt;
         * </pre>
         */
        NO_STAR_IMPORTS,

        /** No unused imports. */
        NO_UNUSED_IMPORTS,

        /** Import order (static first, then java, then third-party). */
        IMPORT_ORDER,

        // --- BEST PRACTICES ---

        /** Equals and hashCode must be paired. */
        EQUALS_HASHCODE,

        /** No System.out or System.err. */
        NO_SYSTEM_OUT,

        /** Use StringBuilder for string concatenation in loops. */
        STRING_BUILDER_IN_LOOPS,

        /** No empty catch blocks. */
        NO_EMPTY_CATCH
    }

    /** Violation severity. */
    enum Severity {
        /** Informational only. */
        INFO,

        /** Warning - build continues. */
        WARNING,

        /** Error - build fails. */
        ERROR,

        /** Ignore - rule disabled. */
        IGNORE
    }
}
