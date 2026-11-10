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
 * &#64;AICodingStandard(style = Style.GOOGLE, naming = NamingConvention.IOP, documentation = Documentation.JAVADOC_REQUIRED, maxMethodLength = 20, maxClassLength = 500)
 * package com.example.service;
 *
 * @AICodingStandard(style = Style.SUN, braces = Braces.SAME_LINE, indent = Indent.SPACES_4)
 * public interface IUserService {
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Coding Standards
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AICodingStandard.java}
 * blob {@code 87c442f945459a02a68c0e9b4d13316a86848be1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AICodingStandard {

    /**
     * Base coding style.
     *
     * @return style
     */
    Style style() default Style.SUN;

    /**
     * Naming convention.
     *
     * @return naming convention
     */
    NamingConvention naming() default NamingConvention.IOP;

    /**
     * Documentation requirements.
     *
     * @return documentation level
     */
    Documentation documentation() default Documentation.JAVADOC_REQUIRED;

    /**
     * Brace style.
     *
     * @return brace style
     */
    Braces braces() default Braces.SAME_LINE;

    /**
     * Indentation style.
     *
     * @return indent style
     */
    Indent indent() default Indent.SPACES_4;

    /**
     * Maximum method length (lines).
     *
     * @return max lines
     */
    int maxMethodLength() default 20;

    /**
     * Maximum class length (lines).
     *
     * @return max lines
     */
    int maxClassLength() default 500;

    /**
     * Maximum line length (characters).
     *
     * @return max chars
     */
    int maxLineLength() default 120;

    /**
     * Maximum parameters per method.
     *
     * @return max params
     */
    int maxParameters() default 5;

    /**
     * Maximum cyclomatic complexity.
     *
     * @return max complexity
     */
    int maxComplexity() default 10;

    /**
     * Require final fields where possible.
     *
     * @return true for final fields
     */
    boolean finalFields() default true;

    /**
     * Require Optional instead of null.
     *
     * @return true for optional
     */
    boolean useOptional() default true;

    /**
     * Require streams over loops.
     *
     * @return true for streams
     */
    boolean preferStreams() default true;

    /**
     * Require Java 21 features.
     *
     * @return true for modern Java
     */
    boolean modernJava() default true;

    /** Coding style presets. */
    enum Style {
        /** Sun/Oracle Java conventions. */
        SUN,

        /** Google Java Style Guide. */
        GOOGLE,

        /** Airbnb style. */
        AIRBNB,

        /** Spring Framework conventions. */
        SPRING,

        /** Apache conventions. */
        APACHE,

        /** Custom (define via other attributes). */
        CUSTOM
    }

    /** Naming conventions (maps to Checkstyle NamingConvention). */
    enum NamingConvention {
        // --- IOP NAMING (LOCKED) ---

        /**
         * IOP naming convention:
         * <ul>
         * <li>Interfaces: I* prefix (IContext, IVerb)</li>
         * <li>Abstract classes: Abstract* prefix</li>
         * <li>Implementations: Descriptive name</li>
         * <li>Factories: *Factory suffix</li>
         * <li>Builders: *Builder suffix</li>
         * <li>Utilities: *Utils or *Ops suffix</li>
         * <li>Constants: SCREAMING_SNAKE_CASE</li>
         * </ul>
         */
        IOP,

        // --- STANDARD NAMING ---

        /** Standard Java naming (no I* prefix). */
        STANDARD,

        /** Sun naming conventions. */
        SUN_NAMING,

        /** Google naming conventions. */
        GOOGLE_NAMING
    }

    /** Documentation requirements. */
    enum Documentation {
        /** No documentation required. */
        NONE,

        /** JavaDoc required for public API. */
        JAVADOC_REQUIRED,

        /** JavaDoc required with @param, @return, @throws. */
        JAVADOC_FULL,

        /** Only inline comments. */
        COMMENTS_ONLY
    }

    /** Brace styles. */
    enum Braces {
        /** Opening brace on same line (K&amp;R style). */
        SAME_LINE,

        /** Opening brace on new line (Allman style). */
        NEW_LINE,

        /** Same line for methods, new line for classes. */
        MIXED
    }

    /** Indentation styles. */
    enum Indent {
        /** 2 spaces. */
        SPACES_2,

        /** 4 spaces (standard). */
        SPACES_4,

        /** 8 spaces. */
        SPACES_8,

        /** Tab character. */
        TAB
    }
}
