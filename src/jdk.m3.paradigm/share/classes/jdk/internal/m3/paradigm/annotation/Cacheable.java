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
 * Cacheable - Marks method for caching.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;Service
 * 	public class ProductService {
 *
 * 		&#64;Cacheable("products")
 * 		public Product findById(Long id) {
 * 			// result is cached
 * 		}
 *
 * 		&#64;Cacheable(value = "products", key = "#name")
 * 		public Product findByName(String name) {
 * 			// custom cache key
 * 		}
 *
 * 		&#64;Cacheable(value = "products", condition = "#id > 10")
 * 		public Product findConditional(Long id) {
 * 			// only cache if condition is true
 * 		}
 * 	}
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Cacheable.java}
 * blob {@code c4012fc67d3add20f45cba27caf76e38a6346c51} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface Cacheable {

    /**
     * Cache names.
     *
     * @return cache names
     */
    String[] value() default {};

    /**
     * Cache names (alias for value).
     *
     * @return cache names
     */
    String[] cacheNames() default {};

    /**
     * Cache key SpEL expression.
     *
     * @return key expression
     */
    String key() default "";

    /**
     * Condition SpEL expression.
     *
     * @return condition expression
     */
    String condition() default "";

    /**
     * Unless SpEL expression (skip caching if true).
     *
     * @return unless expression
     */
    String unless() default "";

    /**
     * Cache manager name.
     *
     * @return cache manager
     */
    String cacheManager() default "";

    /**
     * Sync cache access.
     *
     * @return true to sync
     */
    boolean sync() default false;
}
