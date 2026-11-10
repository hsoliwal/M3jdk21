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
 * 	// LLM knows: result can be cached, same id = same result
 * }
 *
 * @AICacheable(cacheNames = "products", key = "#category + ':' + #page", ttl = 3600, condition = "#page > 0")
 * List<Product> findByCategory(String category, int page) {
 * 	// LLM knows: composite key, 1 hour TTL, conditional
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Cache Hints (Spring-inspired)
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares cache names, key and provider hints, conditions, time-to-live, synchronization and eviction choices. SOURCE retention supplies no cache interception or eviction implementation.
 * @m3.dataStructures SOURCE-retained annotation targeting TYPE and METHOD, with its existing element defaults and nested enum.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAICacheable.java}
 * blob {@code b2b04f2503dd05d85ab05dfc98e0b16480bd8012} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAICacheable {

    /**
     * Cache name(s).
     *
     * @return cache names
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares cacheNames metadata for Cache name(s). The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code String[]} with exact default {@code {}}.
     */
    String[] cacheNames() default {};

    /**
     * Cache key expression (SpEL-like, for documentation).
     *
     * @return key expression
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares key metadata for Cache key expression (SpEL-like, for documentation). The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code String} with exact default {@code ""}.
     */
    String key() default "";

    /**
     * Key generator bean name hint.
     *
     * @return key generator
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares keyGenerator metadata for Key generator bean name hint. The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code String} with exact default {@code ""}.
     */
    String keyGenerator() default "";

    /**
     * Cache manager bean name hint.
     *
     * @return cache manager
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares cacheManager metadata for Cache manager bean name hint. The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code String} with exact default {@code ""}.
     */
    String cacheManager() default "";

    /**
     * Condition for caching (SpEL-like, for documentation).
     *
     * @return condition
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares condition metadata for Condition for caching (SpEL-like, for documentation). The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code String} with exact default {@code ""}.
     */
    String condition() default "";

    /**
     * Unless condition (veto caching).
     *
     * @return unless condition
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares unless metadata for Unless condition (veto caching). The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code String} with exact default {@code ""}.
     */
    String unless() default "";

    /**
     * TTL in seconds (extension).
     *
     * @return time-to-live
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares ttl metadata for TTL in seconds (extension). The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code long} with exact default {@code -1}.
     */
    long ttl() default -1;

    /**
     * Sync mode (prevent cache stampede).
     *
     * @return true for synchronized
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares sync metadata for Sync mode (prevent cache stampede). The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code boolean} with exact default {@code false}.
     */
    boolean sync() default false;

    /**
     * Cache eviction policy hint.
     *
     * @return eviction policy
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares eviction metadata for Cache eviction policy hint. The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code Eviction} with exact default {@code Eviction.LRU}.
     */
    Eviction eviction() default Eviction.LRU;

    /** Eviction policies.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 5 symbolic eviction policies for annotation metadata; consumers interpret the choice.
     * @m3.dataStructures Nested IAICacheable.Eviction enum preserving constant declaration order and identities.
     */
    enum Eviction {
        /** Least Recently Used.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LRU metadata choice: Least Recently Used. This token does not schedule execution, intercept calls or implement caching.
         * @m3.dataStructures Existing nested enum constant preserving its identity and declaration position.
         */
        LRU,

        /** Least Frequently Used.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LFU metadata choice: Least Frequently Used. This token does not schedule execution, intercept calls or implement caching.
         * @m3.dataStructures Existing nested enum constant preserving its identity and declaration position.
         */
        LFU,

        /** First In First Out.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FIFO metadata choice: First In First Out. This token does not schedule execution, intercept calls or implement caching.
         * @m3.dataStructures Existing nested enum constant preserving its identity and declaration position.
         */
        FIFO,

        /** Time-based expiration only.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the TTL metadata choice: Time-based expiration only. This token does not schedule execution, intercept calls or implement caching.
         * @m3.dataStructures Existing nested enum constant preserving its identity and declaration position.
         */
        TTL,

        /** No eviction (bounded by size).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the NONE metadata choice: No eviction (bounded by size). This token does not schedule execution, intercept calls or implement caching.
         * @m3.dataStructures Existing nested enum constant preserving its identity and declaration position.
         */
        NONE
    }
}
