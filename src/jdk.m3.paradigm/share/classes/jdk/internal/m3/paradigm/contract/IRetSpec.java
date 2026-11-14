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
 * IRetSpec - Return value specification for AI code generation.
 *
 * <h2>Return Type Patterns</h2>
 *
 * <pre>
 * Return patterns in modern Java:
 *   - Optional&lt;T&gt; (nullable)
 *   - Result&lt;T, E&gt; (success/failure)
 *   - Either&lt;L, R&gt; (discriminated union)
 *   - Try&lt;T&gt; (exception capture)
 *   - CompletableFuture&lt;T&gt; (async)
 *   - Flow.Publisher&lt;T&gt; (reactive stream)
 *   - Mono&lt;T&gt; / Flux&lt;T&gt; (Project Reactor)
 *
 * &#64;IRetSpec specifies return semantics in IOP style.
 * </pre>
 *
 * <h2>Generated Code PatternKind</h2>
 *
 * <pre>{@code
 * // From: @IRetSpec(wrapper = OPTIONAL, nullMessage = "User not found")
 * // Generates:
 *
 * public Optional&lt;User> findUser(String id) {
 *     User user = repository.findById(id);
 *     return Optional.ofNullable(user);  // Or empty() if null
 * }
 *
 * // From: @IRetSpec(wrapper = RESULT)
 * // Generates:
 *
 * public Result&lt;User, UserError> createUser(CreateUserRequest req) {
 * 	try {
 * 		User user = userService.create(req);
 * 		return Result.success(user);
 * 	} catch (ValidationException e) {
 * 		return Result.failure(new UserError(e.getMessage()));
 * 	}
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * interface IUserService {
 *
 *     // Nullable → Optional @IRetSpec(wrapper = OPTIONAL) Optional&lt;User> findById(String id);
 *
 * // Success/Failure → Result @IRetSpec( wrapper = RESULT, errorType = ValidationError.class )
 *     Result&lt;User, ValidationError> createUser(CreateUserRequest req);
 *
 *     // Async operation
 *     &#64;IRetSpec(wrapper = FUTURE)
 *     CompletableFuture&lt;List&lt;User>> searchUsersAsync(String query);
 *
 *     // Reactive stream
 *     &#64;IRetSpec(wrapper = PUBLISHER)
 *     Flow.Publisher&lt;UserEvent> userEvents();
 *
 *     // Paginated result
 * &#64;IRetSpec(wrapper = PAGE)
 * Page&lt;User> listUsers(Pageable pageable);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Return Specification
 * @see IMethodSpec
 * @see IExceptionSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares return wrapping, failure conversion, asynchronous/reactive, collection, pagination and caching preferences; it creates no wrapper, starts no task and validates no postcondition.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IRetSpec.java}
 * blob {@code 4cb26fda093cc050ec58b3d2d291616e2be4925a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IRetSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined return pattern.
     *
     * @return option
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#option intent: Predefined return pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common return patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IRetSpec.Option choices described as Common return patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 9 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Return raw value (default Java).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Option#RAW policy choice: Return raw value (default Java). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Option; its declared order and identity are preserved.
         */
        RAW,

        /** Wrap in Optional (nullable).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Option#OPTIONAL policy choice: Wrap in Optional (nullable). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Option; its declared order and identity are preserved.
         */
        OPTIONAL,

        /** Wrap in Result (success/failure).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Option#RESULT policy choice: Wrap in Result (success/failure). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Option; its declared order and identity are preserved.
         */
        RESULT,

        /** Return CompletableFuture (async).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Option#FUTURE policy choice: Return CompletableFuture (async). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Option; its declared order and identity are preserved.
         */
        FUTURE,

        /** Return Flow.Publisher (reactive).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Option#PUBLISHER policy choice: Return Flow.Publisher (reactive). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Option; its declared order and identity are preserved.
         */
        PUBLISHER,

        /** Return Page (paginated).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Option#PAGE policy choice: Return Page (paginated). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Option; its declared order and identity are preserved.
         */
        PAGE,

        /** Return Stream (lazy collection).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Option#STREAM policy choice: Return Stream (lazy collection). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Option; its declared order and identity are preserved.
         */
        STREAM,

        /** Return Either (discriminated union).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Option#EITHER policy choice: Return Either (discriminated union). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Option; its declared order and identity are preserved.
         */
        EITHER
    }

    // --- WRAPPER TYPE ---

    /**
     * Wrapper type for return value.
     *
     * @return wrapper
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#wrapper intent: Wrapper type for return value. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Wrapper}; default expression {@code Wrapper.RAW}.
     */
    Wrapper wrapper() default Wrapper.RAW;

    /** Return value wrappers.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IRetSpec.Wrapper choices described as Return value wrappers. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 15 constants in the existing source order.
     */
    enum Wrapper {
        /** No wrapper - direct return.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#RAW policy choice: No wrapper - direct return. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        RAW,

        /** Optional&lt;T&gt; for nullable.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#OPTIONAL policy choice: Optional&lt;T&gt; for nullable. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        OPTIONAL,

        /** Result&lt;T, E&gt; for success/failure.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#RESULT policy choice: Result&lt;T, E&gt; for success/failure. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        RESULT,

        /** Try&lt;T&gt; for exception capture.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#TRY policy choice: Try&lt;T&gt; for exception capture. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        TRY,

        /** Either&lt;L, R&gt; for discriminated union.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#EITHER policy choice: Either&lt;L, R&gt; for discriminated union. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        EITHER,

        /** CompletableFuture&lt;T&gt; for async.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#FUTURE policy choice: CompletableFuture&lt;T&gt; for async. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        FUTURE,

        /** Flow.Publisher&lt;T&gt; for reactive.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#PUBLISHER policy choice: Flow.Publisher&lt;T&gt; for reactive. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        PUBLISHER,

        /** Mono&lt;T&gt; for single reactive.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#MONO policy choice: Mono&lt;T&gt; for single reactive. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        MONO,

        /** Flux&lt;T&gt; for multi reactive.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#FLUX policy choice: Flux&lt;T&gt; for multi reactive. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        FLUX,

        /** Stream&lt;T&gt; for lazy collection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#STREAM policy choice: Stream&lt;T&gt; for lazy collection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        STREAM,

        /** List&lt;T&gt; for collection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#LIST policy choice: List&lt;T&gt; for collection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        LIST,

        /** Set&lt;T&gt; for unique collection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#SET policy choice: Set&lt;T&gt; for unique collection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        SET,

        /** Map&lt;K, V&gt; for key-value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#MAP policy choice: Map&lt;K, V&gt; for key-value. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        MAP,

        /** Page&lt;T&gt; for paginated.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#PAGE policy choice: Page&lt;T&gt; for paginated. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        PAGE,

        /** Slice&lt;T&gt; for cursor-based.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.Wrapper#SLICE policy choice: Slice&lt;T&gt; for cursor-based. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.Wrapper; its declared order and identity are preserved.
         */
        SLICE
    }

    // --- OPTIONAL CONFIGURATION ---

    /**
     * Message when Optional is empty.
     *
     * @return empty message
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#nullMessage intent: Message when Optional is empty. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String nullMessage() default "";

    /**
     * Whether to throw if null (vs return empty Optional).
     *
     * @return true to throw
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#throwIfNull intent: Whether to throw if null (vs return empty Optional). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean throwIfNull() default false;

    // --- RESULT CONFIGURATION ---

    /**
     * Error type for Result wrapper.
     *
     * @return error class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#errorType intent: Error type for Result wrapper. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> errorType() default Void.class;

    /**
     * Exceptions to convert to Result.failure().
     *
     * @return exception classes
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#catchExceptions intent: Exceptions to convert to Result.failure(). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Exception>[]}; default expression {@code {}}.
     */
    Class<? extends Exception>[] catchExceptions() default {};

    // --- EITHER CONFIGURATION ---

    /**
     * Left type for Either wrapper.
     *
     * @return left class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#leftType intent: Left type for Either wrapper. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> leftType() default Void.class;

    /**
     * Right type for Either wrapper.
     *
     * @return right class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#rightType intent: Right type for Either wrapper. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> rightType() default Void.class;

    // --- ASYNC CONFIGURATION ---

    /**
     * Executor for async operations.
     *
     * @return executor bean name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#executor intent: Executor for async operations. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String executor() default "";

    /**
     * Timeout for async operations in milliseconds.
     *
     * @return timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#timeoutMs intent: Timeout for async operations in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int timeoutMs() default 0;

    /**
     * Default value if async times out.
     *
     * @return default value expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#timeoutDefault intent: Default value if async times out. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String timeoutDefault() default "";

    // --- REACTIVE CONFIGURATION ---

    /**
     * Buffer size for reactive streams.
     *
     * @return buffer size
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#bufferSize intent: Buffer size for reactive streams. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 256}.
     */
    int bufferSize() default 256;

    /**
     * Backpressure strategy.
     *
     * @return strategy
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#backpressure intent: Backpressure strategy. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code BackpressureStrategy}; default expression {@code BackpressureStrategy.BUFFER}.
     */
    BackpressureStrategy backpressure() default BackpressureStrategy.BUFFER;

    /** Backpressure strategies.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IRetSpec.BackpressureStrategy choices described as Backpressure strategies. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum BackpressureStrategy {
        /** Buffer all items.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.BackpressureStrategy#BUFFER policy choice: Buffer all items. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.BackpressureStrategy; its declared order and identity are preserved.
         */
        BUFFER,
        /** Drop oldest when full.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.BackpressureStrategy#DROP_OLDEST policy choice: Drop oldest when full. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.BackpressureStrategy; its declared order and identity are preserved.
         */
        DROP_OLDEST,
        /** Drop newest when full.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.BackpressureStrategy#DROP_NEWEST policy choice: Drop newest when full. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.BackpressureStrategy; its declared order and identity are preserved.
         */
        DROP_NEWEST,
        /** Signal error when full.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.BackpressureStrategy#ERROR policy choice: Signal error when full. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.BackpressureStrategy; its declared order and identity are preserved.
         */
        ERROR,
        /** Block producer.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.BackpressureStrategy#BLOCK policy choice: Block producer. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.BackpressureStrategy; its declared order and identity are preserved.
         */
        BLOCK
    }

    // --- COLLECTION CONFIGURATION ---

    /**
     * Element type for collections.
     *
     * @return element class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#elementType intent: Element type for collections. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> elementType() default Void.class;

    /**
     * Key type for Map.
     *
     * @return key class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#keyType intent: Key type for Map. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> keyType() default Void.class;

    /**
     * Value type for Map.
     *
     * @return value class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#valueType intent: Value type for Map. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> valueType() default Void.class;

    /**
     * Whether to return immutable collection.
     *
     * @return true for immutable
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#immutable intent: Whether to return immutable collection. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean immutable() default true;

    // --- PAGINATION CONFIGURATION ---

    /**
     * Default page size.
     *
     * @return page size
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#pageSize intent: Default page size. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 20}.
     */
    int pageSize() default 20;

    /**
     * Maximum page size.
     *
     * @return max size
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#maxPageSize intent: Maximum page size. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 100}.
     */
    int maxPageSize() default 100;

    /**
     * Sort field.
     *
     * @return sort field
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#sortBy intent: Sort field. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String sortBy() default "";

    /**
     * Sort direction.
     *
     * @return direction
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#sortDirection intent: Sort direction. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code SortDirection}; default expression {@code SortDirection.ASC}.
     */
    SortDirection sortDirection() default SortDirection.ASC;

    /** Sort directions.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IRetSpec.SortDirection choices described as Sort directions. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 2 constants in the existing source order.
     */
    enum SortDirection {
        /** Ascending.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.SortDirection#ASC policy choice: Ascending. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.SortDirection; its declared order and identity are preserved.
         */
        ASC,
        /** Descending.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRetSpec.SortDirection#DESC policy choice: Descending. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRetSpec.SortDirection; its declared order and identity are preserved.
         */
        DESC
    }

    // --- CACHING ---

    /**
     * Whether return value is cacheable.
     *
     * @return true to cache
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#cacheable intent: Whether return value is cacheable. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean cacheable() default false;

    /**
     * Cache key expression.
     *
     * @return cache key
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#cacheKey intent: Cache key expression. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String cacheKey() default "";

    /**
     * Cache TTL in seconds.
     *
     * @return TTL
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#cacheTtlSeconds intent: Cache TTL in seconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 300}.
     */
    int cacheTtlSeconds() default 300;

    // --- VALIDATION ---

    /**
     * Post-conditions on return value.
     *
     * @return post-conditions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#postconditions intent: Post-conditions on return value. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] postconditions() default {};

    /**
     * Whether return value can be null.
     *
     * @return true if nullable
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#nullable intent: Whether return value can be null. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean nullable() default false;

    // --- DOCUMENTATION ---

    /**
     * Description of return value.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#value intent: Description of return value. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    /**
     * Example return values.
     *
     * @return examples
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#examples intent: Example return values. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] examples() default {};

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRetSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.HAIKU}.
     */
    Model.ModelType model() default Model.ModelType.HAIKU;
}
