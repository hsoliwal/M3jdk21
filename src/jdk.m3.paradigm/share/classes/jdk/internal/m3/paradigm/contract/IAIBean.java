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
 * AIBean - AnOP annotation for factory method hints.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Bean → runtime container creates and manages bean ║
 * ║ AnOP: @AIBean → compile-time declaration of factory method ║
 * ║ ║
 * ║ Spring says: "I will call this method and manage the result" ║
 * ║ AnOP says: "This method creates instances of this type" ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>The Invariant</h2> <blockquote> Spring @Bean causes the container to
 * invoke the method.<br>
 * AnOP @AIBean declares that this method is a factory. </blockquote>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;AIBean(name = "dataSource")
 * DataSource createDataSource() {
 * 	// LLM knows: this creates a DataSource instance
 * }
 *
 * @AIBean(initMethod = "init", destroyMethod = "close", autowireCandidate = true)
 * ConnectionPool createPool() {
 * 	// LLM knows: lifecycle methods, injection candidate
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Bean Hints (Spring-inspired)
 * @m3.pattern spring:ioc-container
 * @m3.algorithm Declares a bean factory and lifecycle, candidate-selection and bootstrap hints; this annotation creates or manages no bean.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.METHOD, ElementType.ANNOTATION_TYPE}; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIBean.java}
 * blob {@code 57d3d445689b5d575181ba73bc79b81ab61ef894} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIBean {

    /**
     * Bean name(s) (Spring: value/name).
     *
     * @return bean names
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Lists the bean factory product names; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String[] annotation element; default expression {}.
     */
    String[] name() default {};

    /**
     * Initialization method name (Spring: initMethod).
     *
     * @return init method
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Names the initialization callback; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String initMethod() default "";

    /**
     * Destruction method name (Spring: destroyMethod).
     *
     * @return destroy method
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Names the destruction callback; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String destroyMethod() default "";

    /**
     * Autowire candidate hint (Spring: autowireCandidate).
     *
     * @return true if injection candidate
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Declares eligibility for injection candidate selection; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression true.
     */
    boolean autowireCandidate() default true;

    /**
     * Primary bean hint (Spring: @Primary).
     *
     * @return true if primary choice
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Declares preferred-candidate intent; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression false.
     */
    boolean primary() default false;

    /**
     * Lazy initialization hint (Spring: @Lazy).
     *
     * @return true if lazy
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Declares deferred initialization intent; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression false.
     */
    boolean lazy() default false;

    /**
     * Description (Spring: @Description).
     *
     * @return description
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Carries the bean purpose description; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String description() default "";

    /**
     * Depends-on beans (Spring: @DependsOn).
     *
     * @return dependency names
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Lists named bean initialization dependencies; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String[] annotation element; default expression {}.
     */
    String[] dependsOn() default {};

    /**
     * Factory method bootstrap mode.
     *
     * @return bootstrap mode
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Selects default, background or lazy bootstrap intent; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Bootstrap annotation element; default expression Bootstrap.DEFAULT.
     */
    Bootstrap bootstrap() default Bootstrap.DEFAULT;

    /** Bootstrap modes.
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Enumerates 3 declared bean initialization timing choices; selecting a token performs no corresponding runtime operation.
     * @m3.dataStructures Nested IAIBean.Bootstrap enum with 3 source-declared constants in existing order.
     */
    enum Bootstrap {
        /** Default initialization.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIBean.Bootstrap#DEFAULT for bean initialization timing choices: Default initialization. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIBean.Bootstrap enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        DEFAULT,

        /** Background initialization (async).
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIBean.Bootstrap#BACKGROUND for bean initialization timing choices: Background initialization (async). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIBean.Bootstrap enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        BACKGROUND,

        /** Lazy on first use.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIBean.Bootstrap#LAZY for bean initialization timing choices: Lazy on first use. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIBean.Bootstrap enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        LAZY
    }
}
