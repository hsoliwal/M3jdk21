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
 * 	&#64;code
 * 	&#64;AIConditional(profile = "production")
 * 	public interface IProdDataSource&lt;Self extends IProdDataSource&lt;Self>> extends IContext&lt;Self> {
 * 		// LLM knows: only relevant in production profile
 * 	}
 *
 * 	&#64;AIConditional(onProperty = "feature.enabled", havingValue = "true")
 * 	public interface IFeatureService&lt;Self extends IFeatureService&lt;Self>> extends IContext&lt;Self> {
 * 		// LLM knows: feature flag controlled
 * 	}
 *
 * 	&#64;AIConditional(onClass = "com.redis.Redis")
 * 	public interface IRedisCacheService&lt;Self extends IRedisCacheService&lt;Self>> extends IContext&lt;Self> {
 * 		// LLM knows: only when Redis is on classpath
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Conditional Hints (Spring-inspired)
 * @m3.pattern spring:conditional
 * @m3.algorithm Declares activation conditions over profiles, properties, classes, beans, resources and environment; this annotation evaluates none of those conditions.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD}; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIConditional.java}
 * blob {@code 021cebfafea300a7129645dc5ccc15ecaa38c5fa} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAIConditionals.class)
public @interface IAIConditional {

    // --- PROFILE CONDITIONS (Spring @Profile) ---

    /**
     * Active profile(s) (Spring: @Profile).
     *
     * @return profile names
     * @m3.pattern spring:conditional
     * @m3.algorithm Lists profiles whose activation is requested; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String[] annotation element; default expression {}.
     */
    String[] profile() default {};

    /**
     * Negated profile(s) (not in these profiles).
     *
     * @return excluded profiles
     * @m3.pattern spring:conditional
     * @m3.algorithm Lists profiles excluded from activation; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String[] annotation element; default expression {}.
     */
    String[] notProfile() default {};

    // --- PROPERTY CONDITIONS (Spring @ConditionalOnProperty) ---

    /**
     * Property name to check.
     *
     * @return property name
     * @m3.pattern spring:conditional
     * @m3.algorithm Names the property required by the condition; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String onProperty() default "";

    /**
     * Expected property value.
     *
     * @return expected value
     * @m3.pattern spring:conditional
     * @m3.algorithm Carries the expected property value text; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String havingValue() default "";

    /**
     * Match if property is missing.
     *
     * @return true if match on missing
     * @m3.pattern spring:conditional
     * @m3.algorithm Declares acceptance when the property is absent; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression false.
     */
    boolean matchIfMissing() default false;

    // --- CLASS CONDITIONS (Spring @ConditionalOnClass) ---

    /**
     * Class must be present on classpath.
     *
     * @return required class names
     * @m3.pattern spring:conditional
     * @m3.algorithm Lists class names required on the classpath; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String[] annotation element; default expression {}.
     */
    String[] onClass() default {};

    /**
     * Class must be missing from classpath.
     *
     * @return excluded class names
     * @m3.pattern spring:conditional
     * @m3.algorithm Lists class names required to be absent; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String[] annotation element; default expression {}.
     */
    String[] missingClass() default {};

    // --- BEAN CONDITIONS (Spring @ConditionalOnBean) ---

    /**
     * Bean must be present.
     *
     * @return required bean names
     * @m3.pattern spring:conditional
     * @m3.algorithm Lists bean names required to exist; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String[] annotation element; default expression {}.
     */
    String[] onBean() default {};

    /**
     * Bean must be missing.
     *
     * @return excluded bean names
     * @m3.pattern spring:conditional
     * @m3.algorithm Lists bean names required to be absent; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String[] annotation element; default expression {}.
     */
    String[] missingBean() default {};

    // --- RESOURCE CONDITIONS (Spring @ConditionalOnResource) ---

    /**
     * Resource must be present.
     *
     * @return resource paths
     * @m3.pattern spring:conditional
     * @m3.algorithm Lists required resource paths; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String[] annotation element; default expression {}.
     */
    String[] onResource() default {};

    // --- EXPRESSION CONDITIONS (Spring @ConditionalOnExpression) ---

    /**
     * SpEL-like expression (for documentation, not runtime).
     *
     * @return expression
     * @m3.pattern spring:conditional
     * @m3.algorithm Carries the documented condition expression text; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String expression() default "";

    // --- ENVIRONMENT CONDITIONS ---

    /**
     * Environment type.
     *
     * @return environment
     * @m3.pattern spring:conditional
     * @m3.algorithm Selects the environment constraint token; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Environment annotation element; default expression Environment.ANY.
     */
    Environment environment() default Environment.ANY;

    /**
     * Operating system.
     *
     * @return OS type
     * @m3.pattern spring:conditional
     * @m3.algorithm Selects the operating-system constraint token; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures OS annotation element; default expression OS.ANY.
     */
    OS os() default OS.ANY;

    /**
     * Java version constraint.
     *
     * @return minimum Java version
     * @m3.pattern spring:conditional
     * @m3.algorithm Declares a minimum Java version constraint; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures int annotation element; default expression 0.
     */
    int minJavaVersion() default 0;

    // --- CUSTOM CONDITIONS ---

    /**
     * Custom condition description.
     *
     * @return condition description
     * @m3.pattern spring:conditional
     * @m3.algorithm Carries a custom activation condition description; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String when() default "";

    /**
     * Reason for condition.
     *
     * @return reason
     * @m3.pattern spring:conditional
     * @m3.algorithm Carries the rationale for the condition; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String reason() default "";

    // --- ENUMS ---

    /** Environment types.
     * @m3.pattern spring:conditional
     * @m3.algorithm Enumerates 10 declared deployment environment constraints; selecting a token performs no corresponding runtime operation.
     * @m3.dataStructures Nested IAIConditional.Environment enum with 10 source-declared constants in existing order.
     */
    enum Environment {
        /** Any environment.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.Environment#ANY for deployment environment constraints: Any environment. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.Environment enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        ANY,

        /** Development.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.Environment#DEV for deployment environment constraints: Development. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.Environment enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        DEV,

        /** Testing.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.Environment#TEST for deployment environment constraints: Testing. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.Environment enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        TEST,

        /** Staging.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.Environment#STAGING for deployment environment constraints: Staging. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.Environment enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        STAGING,

        /** Production.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.Environment#PROD for deployment environment constraints: Production. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.Environment enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        PROD,

        /** CI/CD pipeline.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.Environment#CI for deployment environment constraints: CI/CD pipeline. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.Environment enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        CI,

        /** Local development.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.Environment#LOCAL for deployment environment constraints: Local development. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.Environment enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        LOCAL,

        /** Cloud deployment.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.Environment#CLOUD for deployment environment constraints: Cloud deployment. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.Environment enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        CLOUD,

        /** Docker container.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.Environment#DOCKER for deployment environment constraints: Docker container. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.Environment enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        DOCKER,

        /** Kubernetes.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.Environment#KUBERNETES for deployment environment constraints: Kubernetes. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.Environment enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        KUBERNETES
    }

    /** Operating systems.
     * @m3.pattern spring:conditional
     * @m3.algorithm Enumerates 5 declared operating-system constraints; selecting a token performs no corresponding runtime operation.
     * @m3.dataStructures Nested IAIConditional.OS enum with 5 source-declared constants in existing order.
     */
    enum OS {
        /** Any OS.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.OS#ANY for operating-system constraints: Any OS. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.OS enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        ANY,

        /** Windows.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.OS#WINDOWS for operating-system constraints: Windows. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.OS enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        WINDOWS,

        /** Linux.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.OS#LINUX for operating-system constraints: Linux. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.OS enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        LINUX,

        /** macOS.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.OS#MACOS for operating-system constraints: macOS. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.OS enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        MACOS,

        /** Unix-like.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIConditional.OS#UNIX for operating-system constraints: Unix-like. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIConditional.OS enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        UNIX
    }
}
