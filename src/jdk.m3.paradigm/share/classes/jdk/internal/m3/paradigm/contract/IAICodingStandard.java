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
 * AICodingStandard - AnOP annotation for coding standard hints.
 *
 * <h2>AnOP Advisory: Coding Standards</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Coding standards guide LLM code generation to match project conventions. ║
 * ║  These map to Checkstyle rules for enforcement.                           ║
 * ║  Standards are HINTS - actual enforcement is by Checkstyle/PMD/SpotBugs.  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * // Package-level coding standard
 * &#64;AICodingStandard(style = Style.GOOGLE, naming = NamingConvention.IOP, documentation = Documentation.JAVADOC_REQUIRED, maxMethodLength = 20, maxClassLength = 500)
 * package com.example.service;
 *
 * // Type-level coding standard
 * &#64;AICodingStandard(style = Style.SUN, braces = Braces.SAME_LINE, indent = Indent.SPACES_4)
 * public interface IUserService&lt;Self extends IUserService&lt;Self&gt;&gt; {
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Coding Standards
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares coding-style, naming, documentation, layout and complexity preferences at package or type scope; it invokes no formatter and enforces no source rule.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.PACKAGE, ElementType.TYPE}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAICodingStandard.java}
 * blob {@code 605354cab55717e6a9969bf165873e62a2b13605} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAICodingStandard {

    /**
     * Base coding style.
     *
     * @return style
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#style intent: Base coding style. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Style}; default expression {@code Style.SUN}.
     */
    Style style() default Style.SUN;

    /**
     * Naming convention.
     *
     * @return naming convention
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#naming intent: Naming convention. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code NamingConvention}; default expression {@code NamingConvention.IOP}.
     */
    NamingConvention naming() default NamingConvention.IOP;

    /**
     * Documentation requirements.
     *
     * @return documentation level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#documentation intent: Documentation requirements. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Documentation}; default expression {@code Documentation.JAVADOC_REQUIRED}.
     */
    Documentation documentation() default Documentation.JAVADOC_REQUIRED;

    /**
     * Brace style.
     *
     * @return brace style
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#braces intent: Brace style. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Braces}; default expression {@code Braces.SAME_LINE}.
     */
    Braces braces() default Braces.SAME_LINE;

    /**
     * Indentation style.
     *
     * @return indent style
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#indent intent: Indentation style. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Indent}; default expression {@code Indent.SPACES_4}.
     */
    Indent indent() default Indent.SPACES_4;

    /**
     * Maximum method length (lines).
     *
     * @return max lines
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#maxMethodLength intent: Maximum method length (lines). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 20}.
     */
    int maxMethodLength() default 20;

    /**
     * Maximum class length (lines).
     *
     * @return max lines
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#maxClassLength intent: Maximum class length (lines). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 500}.
     */
    int maxClassLength() default 500;

    /**
     * Maximum line length (characters).
     *
     * @return max chars
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#maxLineLength intent: Maximum line length (characters). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 120}.
     */
    int maxLineLength() default 120;

    /**
     * Maximum parameters per method.
     *
     * @return max params
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#maxParameters intent: Maximum parameters per method. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 5}.
     */
    int maxParameters() default 5;

    /**
     * Maximum cyclomatic complexity.
     *
     * @return max complexity
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#maxComplexity intent: Maximum cyclomatic complexity. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 10}.
     */
    int maxComplexity() default 10;

    /**
     * Require final fields where possible.
     *
     * @return true for final fields
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#finalFields intent: Require final fields where possible. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean finalFields() default true;

    /**
     * Require Optional instead of null.
     *
     * @return true for optional
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#useOptional intent: Require Optional instead of null. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean useOptional() default true;

    /**
     * Require streams over loops.
     *
     * @return true for streams
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#preferStreams intent: Require streams over loops. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean preferStreams() default true;

    /**
     * Require Java 21 features.
     *
     * @return true for modern Java
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICodingStandard#modernJava intent: Require Java 21 features. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean modernJava() default true;

    /** Coding style presets.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAICodingStandard.Style choices described as Coding style presets. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Style {
        /** Sun/Oracle Java conventions.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Style#SUN policy choice: Sun/Oracle Java conventions. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Style; its declared order and identity are preserved.
         */
        SUN,

        /** Google Java Style Guide.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Style#GOOGLE policy choice: Google Java Style Guide. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Style; its declared order and identity are preserved.
         */
        GOOGLE,

        /** Airbnb style.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Style#AIRBNB policy choice: Airbnb style. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Style; its declared order and identity are preserved.
         */
        AIRBNB,

        /** Spring Framework conventions.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Style#SPRING policy choice: Spring Framework conventions. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Style; its declared order and identity are preserved.
         */
        SPRING,

        /** Apache conventions.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Style#APACHE policy choice: Apache conventions. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Style; its declared order and identity are preserved.
         */
        APACHE,

        /** Custom (define via other attributes).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Style#CUSTOM policy choice: Custom (define via other attributes). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Style; its declared order and identity are preserved.
         */
        CUSTOM
    }

    /** Naming conventions (maps to Checkstyle NamingConvention).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAICodingStandard.NamingConvention choices described as Naming conventions (maps to Checkstyle NamingConvention). Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum NamingConvention {
        // --- IOP NAMING (LOCKED) ---

        /**
         * IOP naming convention follows these rules.
         * <ul>
         * <li>Interfaces: I* prefix (IContext, IVerb)</li>
         * <li>Abstract classes: Abstract* prefix</li>
         * <li>Implementations: Descriptive name</li>
         * <li>Factories: *Factory suffix</li>
         * <li>Builders: *Builder suffix</li>
         * <li>Utilities: *Utils or *Ops suffix</li>
         * <li>Constants: SCREAMING_SNAKE_CASE</li>
         * </ul>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.NamingConvention#IOP policy choice: IOP naming convention follows these rules. <ul> <li>Interfaces: I* prefix (IContext, IVerb)</li> <li>Abstract classes: Abstract* prefix</li> <li>Implementations: Descriptive name</li> <li>Factories: *Factory suffix</li> <li>Builders: *Builder suffix</li> <li>Utilities: *Utils or *Ops suffix</li> <li>Constants: SCREAMING_SNAKE_CASE</li> </ul> This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.NamingConvention; its declared order and identity are preserved.
         */
        IOP,

        // --- STANDARD NAMING ---

        /** Standard Java naming (no I* prefix).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.NamingConvention#STANDARD policy choice: Standard Java naming (no I* prefix). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.NamingConvention; its declared order and identity are preserved.
         */
        STANDARD,

        /** Sun naming conventions.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.NamingConvention#SUN_NAMING policy choice: Sun naming conventions. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.NamingConvention; its declared order and identity are preserved.
         */
        SUN_NAMING,

        /** Google naming conventions.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.NamingConvention#GOOGLE_NAMING policy choice: Google naming conventions. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.NamingConvention; its declared order and identity are preserved.
         */
        GOOGLE_NAMING
    }

    /** Documentation requirements.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAICodingStandard.Documentation choices described as Documentation requirements. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Documentation {
        /** No documentation required.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Documentation#NONE policy choice: No documentation required. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Documentation; its declared order and identity are preserved.
         */
        NONE,

        /** JavaDoc required for public API.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Documentation#JAVADOC_REQUIRED policy choice: JavaDoc required for public API. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Documentation; its declared order and identity are preserved.
         */
        JAVADOC_REQUIRED,

        /** JavaDoc required with @param, @return, @throws.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Documentation#JAVADOC_FULL policy choice: JavaDoc required with @param, @return, @throws. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Documentation; its declared order and identity are preserved.
         */
        JAVADOC_FULL,

        /** Only inline comments.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Documentation#COMMENTS_ONLY policy choice: Only inline comments. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Documentation; its declared order and identity are preserved.
         */
        COMMENTS_ONLY
    }

    /** Brace styles.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAICodingStandard.Braces choices described as Brace styles. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 3 constants in the existing source order.
     */
    enum Braces {
        /** Opening brace on same line (K&amp;R style).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Braces#SAME_LINE policy choice: Opening brace on same line (K&amp;R style). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Braces; its declared order and identity are preserved.
         */
        SAME_LINE,

        /** Opening brace on new line (Allman style).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Braces#NEW_LINE policy choice: Opening brace on new line (Allman style). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Braces; its declared order and identity are preserved.
         */
        NEW_LINE,

        /** Same line for methods, new line for classes.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Braces#MIXED policy choice: Same line for methods, new line for classes. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Braces; its declared order and identity are preserved.
         */
        MIXED
    }

    /** Indentation styles.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAICodingStandard.Indent choices described as Indentation styles. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Indent {
        /** 2 spaces.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Indent#SPACES_2 policy choice: 2 spaces. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Indent; its declared order and identity are preserved.
         */
        SPACES_2,

        /** 4 spaces (standard).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Indent#SPACES_4 policy choice: 4 spaces (standard). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Indent; its declared order and identity are preserved.
         */
        SPACES_4,

        /** 8 spaces.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Indent#SPACES_8 policy choice: 8 spaces. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Indent; its declared order and identity are preserved.
         */
        SPACES_8,

        /** Tab character.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAICodingStandard.Indent#TAB policy choice: Tab character. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAICodingStandard.Indent; its declared order and identity are preserved.
         */
        TAB
    }
}
