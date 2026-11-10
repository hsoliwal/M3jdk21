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
 * 	&#64;code
 * 	&#64;AIConstant(category = Category.CONFIG, prefix = "DEFAULT_", immutable = true)
 * 	public interface IAppConstants&lt;Self extends IAppConstants&lt;Self>> extends IContext&lt;Self> {
 * 		// LLM generates:
 * 		// /** Default connection timeout in milliseconds. *\/
 * 		// int DEFAULT_TIMEOUT_MS = 30000;
 * 	}
 *
 * 	&#64;AIConstant(category = Category.ERROR_CODE, format = Format.STRING, prefix = "ERR_")
 * 	public interface IErrorCodes&lt;Self extends IErrorCodes&lt;Self>> extends IContext&lt;Self> {
 * 		// LLM generates error code constants
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Constant Hints
 * @m3.pattern synexia:anop
 * @m3.algorithm Declares constant-generation hints for category, naming, value representation, immutability and documentation. The annotation records metadata and does not itself perform enforcement or generation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}, preserving the declared element and nested-enum schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIConstant.java}
 * blob {@code e76d99239ac7fa2c71bb39974b18a4df91f9fda9} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAIConstants.class)
public @interface IAIConstant {

    /**
     * Constant category.
     *
     * @return category
     * @m3.pattern synexia:anop
     * @m3.algorithm Declares IAIConstant#category metadata for Constant category. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Category}, with exact default {@code Category.GENERAL}.
     */
    Category category() default Category.GENERAL;

    /**
     * Constant name prefix.
     *
     * @return prefix
     * @m3.pattern synexia:anop
     * @m3.algorithm Declares IAIConstant#prefix metadata for Constant name prefix. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String prefix() default "";

    /**
     * Constant name suffix.
     *
     * @return suffix
     * @m3.pattern synexia:anop
     * @m3.algorithm Declares IAIConstant#suffix metadata for Constant name suffix. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String suffix() default "";

    /**
     * Constant value format.
     *
     * @return format
     * @m3.pattern synexia:anop
     * @m3.algorithm Declares IAIConstant#format metadata for Constant value format. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Format}, with exact default {@code Format.INFERRED}.
     */
    Format format() default Format.INFERRED;

    /**
     * Generate immutable collections for array constants.
     *
     * @return true for immutable
     * @m3.pattern synexia:anop
     * @m3.algorithm Declares IAIConstant#immutable metadata for Generate immutable collections for array constants. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code true}.
     */
    boolean immutable() default true;

    /**
     * Generate JavaDoc for each constant.
     *
     * @return true for javadoc
     * @m3.pattern synexia:anop
     * @m3.algorithm Declares IAIConstant#documented metadata for Generate JavaDoc for each constant. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code true}.
     */
    boolean documented() default true;

    /**
     * Naming style.
     *
     * @return naming style
     * @m3.pattern synexia:anop
     * @m3.algorithm Declares IAIConstant#naming metadata for Naming style. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Naming}, with exact default {@code Naming.SCREAMING_SNAKE_CASE}.
     */
    Naming naming() default Naming.SCREAMING_SNAKE_CASE;

    /** Constant categories.
     * @m3.pattern synexia:anop
     * @m3.algorithm Enumerates 18 symbolic choices for constant generation category; interpretation belongs to consuming tooling.
     * @m3.dataStructures Nested IAIConstant.Category enum with the existing 18 constants in declaration order.
     */
    enum Category {
        // --- CONFIGURATION ---

        /** General constants.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#GENERAL choice for constant generation category: General constants. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        GENERAL,

        /** Configuration defaults.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#CONFIG choice for constant generation category: Configuration defaults. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        CONFIG,

        /** Environment variables.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#ENV choice for constant generation category: Environment variables. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        ENV,

        /** System properties.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#PROPERTY choice for constant generation category: System properties. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        PROPERTY,

        // --- IDENTIFIERS ---

        /** Error codes.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#ERROR_CODE choice for constant generation category: Error codes. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        ERROR_CODE,

        /** Status codes.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#STATUS_CODE choice for constant generation category: Status codes. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        STATUS_CODE,

        /** HTTP status.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#HTTP_STATUS choice for constant generation category: HTTP status. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        HTTP_STATUS,

        /** Event types.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#EVENT_TYPE choice for constant generation category: Event types. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        EVENT_TYPE,

        /** Message keys.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#MESSAGE_KEY choice for constant generation category: Message keys. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        MESSAGE_KEY,

        // --- LIMITS ---

        /** Size limits.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#LIMIT choice for constant generation category: Size limits. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        LIMIT,

        /** Timeout values.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#TIMEOUT choice for constant generation category: Timeout values. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        TIMEOUT,

        /** Threshold values.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#THRESHOLD choice for constant generation category: Threshold values. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        THRESHOLD,

        // --- PATTERNS ---

        /** Regex patterns.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#REGEX choice for constant generation category: Regex patterns. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        REGEX,

        /** Format strings.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#FORMAT choice for constant generation category: Format strings. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        FORMAT,

        /** Date/time formats.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#DATE_FORMAT choice for constant generation category: Date/time formats. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        DATE_FORMAT,

        // --- DOMAIN ---

        /** Domain-specific.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#DOMAIN choice for constant generation category: Domain-specific. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        DOMAIN,

        /** Business rules.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#BUSINESS choice for constant generation category: Business rules. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        BUSINESS,

        /** Feature flags.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Category#FEATURE_FLAG choice for constant generation category: Feature flags. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Category, preserving its declared position and identity.
         */
        FEATURE_FLAG
    }

    /** Constant value formats.
     * @m3.pattern synexia:anop
     * @m3.algorithm Enumerates 9 symbolic choices for requested constant value representation; interpretation belongs to consuming tooling.
     * @m3.dataStructures Nested IAIConstant.Format enum with the existing 9 constants in declaration order.
     */
    enum Format {
        /** Infer from context.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Format#INFERRED choice for requested constant value representation: Infer from context. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Format, preserving its declared position and identity.
         */
        INFERRED,

        /** String value.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Format#STRING choice for requested constant value representation: String value. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Format, preserving its declared position and identity.
         */
        STRING,

        /** Integer value.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Format#INT choice for requested constant value representation: Integer value. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Format, preserving its declared position and identity.
         */
        INT,

        /** Long value.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Format#LONG choice for requested constant value representation: Long value. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Format, preserving its declared position and identity.
         */
        LONG,

        /** Double value.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Format#DOUBLE choice for requested constant value representation: Double value. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Format, preserving its declared position and identity.
         */
        DOUBLE,

        /** Boolean value.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Format#BOOLEAN choice for requested constant value representation: Boolean value. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Format, preserving its declared position and identity.
         */
        BOOLEAN,

        /** Duration (ISO-8601).
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Format#DURATION choice for requested constant value representation: Duration (ISO-8601). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Format, preserving its declared position and identity.
         */
        DURATION,

        /** PatternKind/Regex.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Format#PATTERN choice for requested constant value representation: PatternKind/Regex. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Format, preserving its declared position and identity.
         */
        PATTERN,

        /** Enum constant.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Format#ENUM choice for requested constant value representation: Enum constant. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Format, preserving its declared position and identity.
         */
        ENUM
    }

    /** Naming styles.
     * @m3.pattern synexia:anop
     * @m3.algorithm Enumerates 4 symbolic choices for requested constant naming style; interpretation belongs to consuming tooling.
     * @m3.dataStructures Nested IAIConstant.Naming enum with the existing 4 constants in declaration order.
     */
    enum Naming {
        /** SCREAMING_SNAKE_CASE (standard for constants).
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Naming#SCREAMING_SNAKE_CASE choice for requested constant naming style: SCREAMING_SNAKE_CASE (standard for constants). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Naming, preserving its declared position and identity.
         */
        SCREAMING_SNAKE_CASE,

        /** camelCase.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Naming#CAMEL_CASE choice for requested constant naming style: camelCase. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Naming, preserving its declared position and identity.
         */
        CAMEL_CASE,

        /** PascalCase.
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Naming#PASCAL_CASE choice for requested constant naming style: PascalCase. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Naming, preserving its declared position and identity.
         */
        PASCAL_CASE,

        /** kebab-case (for string values).
         * @m3.pattern synexia:anop
         * @m3.algorithm Names the IAIConstant.Naming#KEBAB_CASE choice for requested constant naming style: kebab-case (for string values). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstant.Naming, preserving its declared position and identity.
         */
        KEBAB_CASE
    }
}
