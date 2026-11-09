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
 * Autowired - Marks dependency for automatic injection.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Service
 *     public class UserService {
 *
 *         &#64;Autowired
 *         private UserRepository repository;
 *
 *         // Or constructor injection (preferred)
 *         {@code @Autowired}
 *         public UserService(UserRepository repository) {
 *             this.repository = repository;
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>Optional Dependencies</h2>
 *
 * <pre>{@code
 *
 * @Autowired(required = false)
 * private CacheManager cache; // null if not available
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 */
@Target({
    ElementType.CONSTRUCTOR,
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.PARAMETER,
    ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only, no reflection
@Documented
/**
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Autowired.java}
 * blob {@code eb83e6d6af17d420ddc27a75db8e5877f480865e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public @interface Autowired {

    /**
     * Whether dependency is required.
     *
     * @return true if required
     */
    boolean required() default true;
}
