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
 * CachePut - Updates cache without skipping method execution.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;Service
 * 	public class UserService {
 *
 * 		&#64;CachePut(value = "users", key = "#user.id")
 * 		public User updateUser(User user) {
 * 			// Always executes and updates cache
 * 			return repository.save(user);
 * 		}
 * 	}
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring Cache
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/CachePut.java}
 * blob {@code 75a748b82094a5b062a49a4469c2e8910a3d0f85} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface CachePut {

    /**
     * Cache names.
     *
     * @return cache names
     */
    String[] value() default {};

    /**
     * Cache names (alias).
     *
     * @return cache names
     */
    String[] cacheNames() default {};

    /**
     * SpEL expression for cache key.
     *
     * @return key expression
     */
    String key() default "";

    /**
     * Condition for caching.
     *
     * @return SpEL condition
     */
    String condition() default "";

    /**
     * Condition to veto caching.
     *
     * @return SpEL condition
     */
    String unless() default "";
}
