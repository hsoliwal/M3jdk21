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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIConstant - AnOP annotation for constant generation hints.
 *
 * <h2>AnOP Advisory: Constant Generation</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Constants should be:                                                     ║
 * ║    • SCREAMING_SNAKE_CASE                                                 ║
 * ║    • static final                                                         ║
 * ║    • Grouped by category                                                  ║
 * ║    • Documented with JavaDoc                                              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;AIConstant(category = Category.CONFIG, prefix = "DEFAULT_", immutable = true)
 *     public interface IAppConstants {
 *         // LLM generates:
 *         // /** Default connection timeout in milliseconds. *\/
 *         // int DEFAULT_TIMEOUT_MS = 30000;
 *     }
 *
 *     &#64;AIConstant(category = Category.ERROR_CODE, format = Format.STRING, prefix = "ERR_")
 *     public interface IErrorCodes {
 *         // LLM generates error code constants
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Constant Hints
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIConstant.java}
 * blob {@code c48964a3dae9ae7a4280bc3d9acdd934e4ded911} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIConstants.class)
public @interface AIConstant {

    /**
     * Constant category.
     *
     * @return category
     */
    Category category() default Category.GENERAL;

    /**
     * Constant name prefix.
     *
     * @return prefix
     */
    String prefix() default "";

    /**
     * Constant name suffix.
     *
     * @return suffix
     */
    String suffix() default "";

    /**
     * Constant value format.
     *
     * @return format
     */
    Format format() default Format.INFERRED;

    /**
     * Generate immutable collections for array constants.
     *
     * @return true for immutable
     */
    boolean immutable() default true;

    /**
     * Generate JavaDoc for each constant.
     *
     * @return true for javadoc
     */
    boolean documented() default true;

    /**
     * Naming style.
     *
     * @return naming style
     */
    Naming naming() default Naming.SCREAMING_SNAKE_CASE;

    /** Constant categories. */
    enum Category {
        // --- CONFIGURATION ---

        /** General constants. */
        GENERAL,

        /** Configuration defaults. */
        CONFIG,

        /** Environment variables. */
        ENV,

        /** System properties. */
        PROPERTY,

        // --- IDENTIFIERS ---

        /** Error codes. */
        ERROR_CODE,

        /** Status codes. */
        STATUS_CODE,

        /** HTTP status. */
        HTTP_STATUS,

        /** Event types. */
        EVENT_TYPE,

        /** Message keys. */
        MESSAGE_KEY,

        // --- LIMITS ---

        /** Size limits. */
        LIMIT,

        /** Timeout values. */
        TIMEOUT,

        /** Threshold values. */
        THRESHOLD,

        // --- PATTERNS ---

        /** Regex patterns. */
        REGEX,

        /** Format strings. */
        FORMAT,

        /** Date/time formats. */
        DATE_FORMAT,

        // --- DOMAIN ---

        /** Domain-specific. */
        DOMAIN,

        /** Business rules. */
        BUSINESS,

        /** Feature flags. */
        FEATURE_FLAG
    }

    /** Constant value formats. */
    enum Format {
        /** Infer from context. */
        INFERRED,

        /** String value. */
        STRING,

        /** Integer value. */
        INT,

        /** Long value. */
        LONG,

        /** Double value. */
        DOUBLE,

        /** Boolean value. */
        BOOLEAN,

        /** Duration (ISO-8601). */
        DURATION,

        /** PatternKind/Regex. */
        PATTERN,

        /** Enum constant. */
        ENUM
    }

    /** Naming styles. */
    enum Naming {
        /** SCREAMING_SNAKE_CASE (standard for constants). */
        SCREAMING_SNAKE_CASE,

        /** camelCase. */
        CAMEL_CASE,

        /** PascalCase. */
        PASCAL_CASE,

        /** kebab-case (for string values). */
        KEBAB_CASE
    }
}
