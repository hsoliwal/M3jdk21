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
 * AICacheable - AnOP annotation for caching hints.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Cacheable → runtime cache interception ║
 * ║ AnOP: @AICacheable → compile-time cache declaration (NO cache impl) ║
 * ║ ║
 * ║ Spring says: "I will cache results of this method" ║
 * ║ AnOP says: "This method's results are cacheable" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT implement caching. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;AICacheable(cacheNames = "users")
 * User findById(String id) {
 *     // LLM knows: result can be cached, same id = same result
 * }
 *
 * @AICacheable(cacheNames = "products", key = "#category + ':' + #page", ttl = 3600, condition = "#page > 0")
 * List<Product> findByCategory(String category, int page) {
 *     // LLM knows: composite key, 1 hour TTL, conditional
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Cache Hints (Spring-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cache/annotation/Cacheable.html">Spring @Cacheable</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AICacheable.java}
 * blob {@code f5df19a2ee8ba2d413100b0317e6928f50161bce} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AICacheable {

    /**
     * Cache name(s).
     *
     * @return cache names
     */
    String[] cacheNames() default {};

    /**
     * Cache key expression (SpEL-like, for documentation).
     *
     * @return key expression
     */
    String key() default "";

    /**
     * Key generator bean name hint.
     *
     * @return key generator
     */
    String keyGenerator() default "";

    /**
     * Cache manager bean name hint.
     *
     * @return cache manager
     */
    String cacheManager() default "";

    /**
     * Condition for caching (SpEL-like, for documentation).
     *
     * @return condition
     */
    String condition() default "";

    /**
     * Unless condition (veto caching).
     *
     * @return unless condition
     */
    String unless() default "";

    /**
     * TTL in seconds (extension).
     *
     * @return time-to-live
     */
    long ttl() default -1;

    /**
     * Sync mode (prevent cache stampede).
     *
     * @return true for synchronized
     */
    boolean sync() default false;

    /**
     * Cache eviction policy hint.
     *
     * @return eviction policy
     */
    Eviction eviction() default Eviction.LRU;

    /** Eviction policies. */
    enum Eviction {
        /** Least Recently Used. */
        LRU,

        /** Least Frequently Used. */
        LFU,

        /** First In First Out. */
        FIFO,

        /** Time-based expiration only. */
        TTL,

        /** No eviction (bounded by size). */
        NONE
    }
}
