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
 * AIConditional - AnOP annotation for conditional activation hints.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Conditional/@Profile → runtime condition evaluation ║
 * ║ AnOP: @AIConditional → compile-time condition declaration ║
 * ║ ║
 * ║ Spring says: "I will evaluate this condition at runtime" ║
 * ║ AnOP says: "This code is relevant under these conditions" ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>The Invariant</h2> <blockquote> Spring @Conditional evaluates conditions
 * at container startup.<br>
 * AnOP @AIConditional declares when code is applicable. </blockquote>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;AIConditional(profile = "production")
 *     public interface IProdDataSource {
 *         // LLM knows: only relevant in production profile
 *     }
 *
 *     &#64;AIConditional(onProperty = "feature.enabled", havingValue = "true")
 *     public interface IFeatureService {
 *         // LLM knows: feature flag controlled
 *     }
 *
 *     &#64;AIConditional(onClass = "com.redis.Redis")
 *     public interface IRedisCacheService {
 *         // LLM knows: only when Redis is on classpath
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Conditional Hints (Spring-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Conditional.html">Spring @Conditional</a>
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Profile.html">Spring @Profile</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIConditional.java}
 * blob {@code e68e62d28b7e3873df5070f9d16c5da11d90caff} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIConditionals.class)
public @interface AIConditional {

    // --- PROFILE CONDITIONS (Spring @Profile) ---

    /**
     * Active profile(s) (Spring: @Profile).
     *
     * @return profile names
     */
    String[] profile() default {};

    /**
     * Negated profile(s) (not in these profiles).
     *
     * @return excluded profiles
     */
    String[] notProfile() default {};

    // --- PROPERTY CONDITIONS (Spring @ConditionalOnProperty) ---

    /**
     * Property name to check.
     *
     * @return property name
     */
    String onProperty() default "";

    /**
     * Expected property value.
     *
     * @return expected value
     */
    String havingValue() default "";

    /**
     * Match if property is missing.
     *
     * @return true if match on missing
     */
    boolean matchIfMissing() default false;

    // --- CLASS CONDITIONS (Spring @ConditionalOnClass) ---

    /**
     * Class must be present on classpath.
     *
     * @return required class names
     */
    String[] onClass() default {};

    /**
     * Class must be missing from classpath.
     *
     * @return excluded class names
     */
    String[] missingClass() default {};

    // --- BEAN CONDITIONS (Spring @ConditionalOnBean) ---

    /**
     * Bean must be present.
     *
     * @return required bean names
     */
    String[] onBean() default {};

    /**
     * Bean must be missing.
     *
     * @return excluded bean names
     */
    String[] missingBean() default {};

    // --- RESOURCE CONDITIONS (Spring @ConditionalOnResource) ---

    /**
     * Resource must be present.
     *
     * @return resource paths
     */
    String[] onResource() default {};

    // --- EXPRESSION CONDITIONS (Spring @ConditionalOnExpression) ---

    /**
     * SpEL-like expression (for documentation, not runtime).
     *
     * @return expression
     */
    String expression() default "";

    // --- ENVIRONMENT CONDITIONS ---

    /**
     * Environment type.
     *
     * @return environment
     */
    Environment environment() default Environment.ANY;

    /**
     * Operating system.
     *
     * @return OS type
     */
    OS os() default OS.ANY;

    /**
     * Java version constraint.
     *
     * @return minimum Java version
     */
    int minJavaVersion() default 0;

    // --- CUSTOM CONDITIONS ---

    /**
     * Custom condition description.
     *
     * @return condition description
     */
    String when() default "";

    /**
     * Reason for condition.
     *
     * @return reason
     */
    String reason() default "";

    // --- ENUMS ---

    /** Environment types. */
    enum Environment {
        /** Any environment. */
        ANY,

        /** Development. */
        DEV,

        /** Testing. */
        TEST,

        /** Staging. */
        STAGING,

        /** Production. */
        PROD,

        /** CI/CD pipeline. */
        CI,

        /** Local development. */
        LOCAL,

        /** Cloud deployment. */
        CLOUD,

        /** Docker container. */
        DOCKER,

        /** Kubernetes. */
        KUBERNETES
    }

    /** Operating systems. */
    enum OS {
        /** Any OS. */
        ANY,

        /** Windows. */
        WINDOWS,

        /** Linux. */
        LINUX,

        /** macOS. */
        MACOS,

        /** Unix-like. */
        UNIX
    }
}
