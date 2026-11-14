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

package jdk.internal.m3.paradigm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuration - Marks class as a configuration source.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Configuration
 *     public class AppConfig {
 *
 *         &#64;Bean
 *         public DataSource dataSource() {
 *             return new HikariDataSource();
 *         }
 *
 *         {@code @Bean}
 *         public UserService userService(UserRepository repo) {
 *             return new UserServiceImpl(repo);
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>Semantic Meaning</h2>
 * <p>
 * Classes annotated with @Configuration contain @Bean definitions that are
 * processed to generate bean definitions.
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Configuration.java}
 * blob {@code 1ccfa536acc237cb55a6a35a56ebaa224c9972ed} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@Component
public @interface Configuration {

    /**
     * Configuration name.
     *
     * @return configuration name
     */
    String value() default "";

    /**
     * Whether to proxy @Bean methods.
     *
     * @return true to proxy
     */
    boolean proxyBeanMethods() default true;
}
