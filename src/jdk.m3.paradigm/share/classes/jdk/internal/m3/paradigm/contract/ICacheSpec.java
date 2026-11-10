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

import jdk.internal.m3.paradigm.annotation.IMethodSpec;
import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ICacheSpec - Caching specification for AI code generation.
 *
 * <h2>Caching Patterns</h2>
 *
 * <pre>
 * Caching patterns in Java:
 *   - Spring @Cacheable / @CacheEvict / @CachePut
 *   - JCache (JSR-107)
 *   - Guava Cache
 *   - Caffeine
 *   - Redis / Memcached
 *   - Ehcache
 *
 * &#64;ICacheSpec specifies caching behavior in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IUserService {
 *
 *     // Simple caching
 * &#64;ICacheSpec(name = "users", key = "#id", ttl = 3600)
 * User findById(String id);
 *
 * // Cache eviction
 * &#64;ICacheSpec(name = "users", evict = true, key = "#user.id")
 * void updateUser(User user);
 *
 * // Conditional caching
 * &#64;ICacheSpec(name = "users", condition = "#result != null", unless = "#result.inactive")
 * User findByEmail(String email);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Cache Specification
 * @see IMethodSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares method caching operations, expressions, expiry, size, eviction and provider hints; it creates no cache and evaluates no key or condition expression.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/ICacheSpec.java}
 * blob {@code 157d3ba592690bc8d004ffa0f52f0d19ba347345} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ICacheSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined caching pattern.
     *
     * @return option
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#option intent: Predefined caching pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common caching patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ICacheSpec.Option choices described as Common caching patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Simple read-through cache.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Option#READ_THROUGH policy choice: Simple read-through cache. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Option; its declared order and identity are preserved.
         */
        READ_THROUGH,

        /** Write-through cache.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Option#WRITE_THROUGH policy choice: Write-through cache. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Option; its declared order and identity are preserved.
         */
        WRITE_THROUGH,

        /** Write-behind cache.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Option#WRITE_BEHIND policy choice: Write-behind cache. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Option; its declared order and identity are preserved.
         */
        WRITE_BEHIND,

        /** Cache aside pattern.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Option#CACHE_ASIDE policy choice: Cache aside pattern. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Option; its declared order and identity are preserved.
         */
        CACHE_ASIDE,

        /** Refresh ahead.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Option#REFRESH_AHEAD policy choice: Refresh ahead. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Option; its declared order and identity are preserved.
         */
        REFRESH_AHEAD
    }

    // --- CACHE IDENTITY ---

    /**
     * Cache name/region.
     *
     * @return cache name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#name intent: Cache name/region. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "default"}.
     */
    String name() default "default";

    /**
     * Cache key expression (SpEL).
     *
     * @return key expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#key intent: Cache key expression (SpEL). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String key() default "";

    /**
     * Key generator bean name.
     *
     * @return key generator
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#keyGenerator intent: Key generator bean name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String keyGenerator() default "";

    // --- OPERATION TYPE ---

    /**
     * Cache operation type.
     *
     * @return operation
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#operation intent: Cache operation type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Operation}; default expression {@code Operation.GET}.
     */
    Operation operation() default Operation.GET;

    /** Cache operations.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ICacheSpec.Operation choices described as Cache operations. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Operation {
        /** Get from cache (read-through).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Operation#GET policy choice: Get from cache (read-through). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Operation; its declared order and identity are preserved.
         */
        GET,

        /** Put into cache.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Operation#PUT policy choice: Put into cache. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Operation; its declared order and identity are preserved.
         */
        PUT,

        /** Evict from cache.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Operation#EVICT policy choice: Evict from cache. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Operation; its declared order and identity are preserved.
         */
        EVICT,

        /** Evict all from cache.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Operation#EVICT_ALL policy choice: Evict all from cache. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Operation; its declared order and identity are preserved.
         */
        EVICT_ALL
    }

    /**
     * Whether to evict (shorthand for operation=EVICT).
     *
     * @return true to evict
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#evict intent: Whether to evict (shorthand for operation=EVICT). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean evict() default false;

    /**
     * Whether to put result (shorthand for operation=PUT).
     *
     * @return true to put
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#put intent: Whether to put result (shorthand for operation=PUT). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean put() default false;

    // --- TTL / EXPIRY ---

    /**
     * Time-to-live in seconds. 0 = no expiry.
     *
     * @return TTL seconds
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#ttl intent: Time-to-live in seconds. 0 = no expiry. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int ttl() default 0;

    /**
     * Time-to-idle in seconds (expire if not accessed).
     *
     * @return TTI seconds
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#tti intent: Time-to-idle in seconds (expire if not accessed). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int tti() default 0;

    /**
     * Refresh time (refresh before expiry).
     *
     * @return refresh seconds
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#refresh intent: Refresh time (refresh before expiry). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int refresh() default 0;

    // --- CONDITIONS ---

    /**
     * Condition for caching (SpEL). Cache only if true.
     *
     * @return condition
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#condition intent: Condition for caching (SpEL). Cache only if true. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String condition() default "";

    /**
     * Unless condition (SpEL). Don't cache if true.
     *
     * @return unless condition
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#unless intent: Unless condition (SpEL). Don't cache if true. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String unless() default "";

    // --- CACHE CONFIGURATION ---

    /**
     * Maximum cache size.
     *
     * @return max size
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#maxSize intent: Maximum cache size. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int maxSize() default 0;

    /**
     * Eviction policy when full.
     *
     * @return eviction policy
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#evictionPolicy intent: Eviction policy when full. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code EvictionPolicy}; default expression {@code EvictionPolicy.LRU}.
     */
    EvictionPolicy evictionPolicy() default EvictionPolicy.LRU;

    /** Eviction policies.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ICacheSpec.EvictionPolicy choices described as Eviction policies. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum EvictionPolicy {
        /** Least Recently Used.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.EvictionPolicy#LRU policy choice: Least Recently Used. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.EvictionPolicy; its declared order and identity are preserved.
         */
        LRU,

        /** Least Frequently Used.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.EvictionPolicy#LFU policy choice: Least Frequently Used. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.EvictionPolicy; its declared order and identity are preserved.
         */
        LFU,

        /** First In First Out.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.EvictionPolicy#FIFO policy choice: First In First Out. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.EvictionPolicy; its declared order and identity are preserved.
         */
        FIFO,

        /** Random.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.EvictionPolicy#RANDOM policy choice: Random. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.EvictionPolicy; its declared order and identity are preserved.
         */
        RANDOM
    }

    // --- DISTRIBUTED CACHE ---

    /**
     * Cache provider.
     *
     * @return provider
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#provider intent: Cache provider. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Provider}; default expression {@code Provider.LOCAL}.
     */
    Provider provider() default Provider.LOCAL;

    /** Cache providers.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ICacheSpec.Provider choices described as Cache providers. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum Provider {
        /** In-memory (Caffeine/Guava).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Provider#LOCAL policy choice: In-memory (Caffeine/Guava). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Provider; its declared order and identity are preserved.
         */
        LOCAL,

        /** Redis.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Provider#REDIS policy choice: Redis. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Provider; its declared order and identity are preserved.
         */
        REDIS,

        /** Memcached.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Provider#MEMCACHED policy choice: Memcached. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Provider; its declared order and identity are preserved.
         */
        MEMCACHED,

        /** Hazelcast.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Provider#HAZELCAST policy choice: Hazelcast. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Provider; its declared order and identity are preserved.
         */
        HAZELCAST,

        /** Ehcache.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICacheSpec.Provider#EHCACHE policy choice: Ehcache. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICacheSpec.Provider; its declared order and identity are preserved.
         */
        EHCACHE
    }

    // --- SYNC ---

    /**
     * Whether to synchronize cache access.
     *
     * @return true to sync
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#sync intent: Whether to synchronize cache access. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean sync() default false;

    // --- DESCRIPTION ---

    /**
     * Cache behavior description.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#value intent: Cache behavior description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICacheSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.HAIKU}.
     */
    Model.ModelType model() default Model.ModelType.HAIKU;
}
