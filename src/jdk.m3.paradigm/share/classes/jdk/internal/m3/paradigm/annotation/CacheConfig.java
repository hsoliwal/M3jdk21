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
 * CacheConfig - Class-level cache configuration.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 * {@code
 * &#64;Service
 * &#64;CacheConfig(cacheNames = "users", cacheManager = "primaryCacheManager")
 * public class UserService {
 *
 *     &#64;Cacheable
 *     public User getUser(Long id) { ... }
 *
 *     &#64;CachePut(key = "#user.id")
 *     public User updateUser(User user) { ... }
 * }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring Cache
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/CacheConfig.java}
 * blob {@code e591ad889e35237f79c061332aea1b7a46df44a8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface CacheConfig {

    /**
     * Default cache names.
     *
     * @return cache names
     */
    String[] cacheNames() default {};

    /**
     * Default key generator bean name.
     *
     * @return key generator name
     */
    String keyGenerator() default "";

    /**
     * Default cache manager bean name.
     *
     * @return cache manager name
     */
    String cacheManager() default "";

    /**
     * Default cache resolver bean name.
     *
     * @return cache resolver name
     */
    String cacheResolver() default "";
}
