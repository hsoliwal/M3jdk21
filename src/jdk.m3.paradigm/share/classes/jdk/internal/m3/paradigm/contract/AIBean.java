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
 *     // LLM knows: this creates a DataSource instance
 * }
 *
 * @AIBean(initMethod = "init", destroyMethod = "close", autowireCandidate = true)
 * ConnectionPool createPool() {
 *     // LLM knows: lifecycle methods, injection candidate
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Bean Hints (Spring-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Bean.html">Spring @Bean</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIBean.java}
 * blob {@code 3aa271f6c764b6cdb092fa0b4d70778cf8ea171f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIBean {

    /**
     * Bean name(s) (Spring: value/name).
     *
     * @return bean names
     */
    String[] name() default {};

    /**
     * Initialization method name (Spring: initMethod).
     *
     * @return init method
     */
    String initMethod() default "";

    /**
     * Destruction method name (Spring: destroyMethod).
     *
     * @return destroy method
     */
    String destroyMethod() default "";

    /**
     * Autowire candidate hint (Spring: autowireCandidate).
     *
     * @return true if injection candidate
     */
    boolean autowireCandidate() default true;

    /**
     * Primary bean hint (Spring: @Primary).
     *
     * @return true if primary choice
     */
    boolean primary() default false;

    /**
     * Lazy initialization hint (Spring: @Lazy).
     *
     * @return true if lazy
     */
    boolean lazy() default false;

    /**
     * Description (Spring: @Description).
     *
     * @return description
     */
    String description() default "";

    /**
     * Depends-on beans (Spring: @DependsOn).
     *
     * @return dependency names
     */
    String[] dependsOn() default {};

    /**
     * Factory method bootstrap mode.
     *
     * @return bootstrap mode
     */
    Bootstrap bootstrap() default Bootstrap.DEFAULT;

    /** Bootstrap modes. */
    enum Bootstrap {
        /** Default initialization. */
        DEFAULT,

        /** Background initialization (async). */
        BACKGROUND,

        /** Lazy on first use. */
        LAZY
    }
}
