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
 * AIValue - AnOP annotation for configuration value hints.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Value → runtime property injection ║
 * ║ AnOP: @AIValue → compile-time configuration declaration ║
 * ║ ║
 * ║ Spring says: "I will inject this value from properties" ║
 * ║ AnOP says: "This field expects a configuration value" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT inject values. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;AIValue("${app.timeout:30}")
 * 	int timeout;
 * 	// LLM knows: from properties, default 30
 *
 * 	&#64;AIValue(value = "${api.key}", required = true, secret = true)
 * 	String apiKey;
 * 	// LLM knows: required, sensitive value
 *
 * 	&#64;AIValue("${feature.enabled:false}")
 * 	boolean featureEnabled;
 * 	// LLM knows: feature flag with default
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Value Hints (Spring-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/factory/annotation/Value.html">Spring @Value</a>
 * @m3.pattern spring:dependency-injection
 * @m3.algorithm Declares property-value injection and conversion/validation hints; this annotation reads no property and evaluates no expression.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD}; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIValue.java}
 * blob {@code 3cc2065eed585c75a26d9669a40735e53b60a0be} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIValue {

    /**
     * Property expression (SpEL-like, e.g., "${app.name:default}").
     *
     * @return property expression
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Carries the required property/expression text; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; explicit value required, with no default.
     */
    String value();

    /**
     * Property is required (no default allowed).
     *
     * @return true if required
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Declares whether a property must be present without a default; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression true.
     */
    boolean required() default true;

    /**
     * Value is secret/sensitive (don't log).
     *
     * @return true if secret
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Marks a sensitive value that should not be logged; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression false.
     */
    boolean secret() default false;

    /**
     * Property source name hint.
     *
     * @return source name
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Names the property source hint; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String source() default "";

    /**
     * Type conversion hint.
     *
     * @return expected type
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Carries the requested conversion type token; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Class&lt;?> annotation element; default expression Object.class.
     */
    Class<?> type() default Object.class;

    /**
     * Refresh on configuration change.
     *
     * @return true if refreshable
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Declares configuration-refresh participation; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression false.
     */
    boolean refreshable() default false;

    /**
     * Validation pattern (regex).
     *
     * @return pattern
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Carries the validation regular-expression text; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String pattern() default "";

    /**
     * Minimum value (for numeric).
     *
     * @return min value
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Carries the minimum numeric constraint as text; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String min() default "";

    /**
     * Maximum value (for numeric).
     *
     * @return max value
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Carries the maximum numeric constraint as text; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String max() default "";
}
