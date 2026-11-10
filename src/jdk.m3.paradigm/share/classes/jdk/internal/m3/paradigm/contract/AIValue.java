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
 *     &#64;code
 *     &#64;AIValue("${app.timeout:30}")
 *     int timeout;
 *     // LLM knows: from properties, default 30
 *
 *     &#64;AIValue(value = "${api.key}", required = true, secret = true)
 *     String apiKey;
 *     // LLM knows: required, sensitive value
 *
 *     &#64;AIValue("${feature.enabled:false}")
 *     boolean featureEnabled;
 *     // LLM knows: feature flag with default
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Value Hints (Spring-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/factory/annotation/Value.html">Spring @Value</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIValue.java}
 * blob {@code 7ad54e79095c48acf81400a77be52ec6c0efc590} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIValue {

    /**
     * Property expression (SpEL-like, e.g., "${app.name:default}").
     *
     * @return property expression
     */
    String value();

    /**
     * Property is required (no default allowed).
     *
     * @return true if required
     */
    boolean required() default true;

    /**
     * Value is secret/sensitive (don't log).
     *
     * @return true if secret
     */
    boolean secret() default false;

    /**
     * Property source name hint.
     *
     * @return source name
     */
    String source() default "";

    /**
     * Type conversion hint.
     *
     * @return expected type
     */
    Class<?> type() default Object.class;

    /**
     * Refresh on configuration change.
     *
     * @return true if refreshable
     */
    boolean refreshable() default false;

    /**
     * Validation pattern (regex).
     *
     * @return pattern
     */
    String pattern() default "";

    /**
     * Minimum value (for numeric).
     *
     * @return min value
     */
    String min() default "";

    /**
     * Maximum value (for numeric).
     *
     * @return max value
     */
    String max() default "";
}
