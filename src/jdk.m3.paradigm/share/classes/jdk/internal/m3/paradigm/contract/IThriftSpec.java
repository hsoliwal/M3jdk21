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

import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IThriftSpec - Apache Thrift RPC specification for AI code generation.
 *
 * <h2>Thrift Code Generation</h2>
 *
 * <pre>
 * From @IThriftSpec annotation, the IOP pipeline generates:
 *   1. Thrift IDL (.thrift file)
 *   2. Server implementation (TProcessor)
 *   3. Client stub (TServiceClient)
 *   4. Transport/protocol configuration
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * &#64;ISpec(value = "User profile service", library = Library.THRIFT)
 * public interface IUserService<Self extends IUserService<Self>> extends IContext<Self> {
 *
 *     &#64;IThriftSpec(
 *         serviceMethod = "getUser",
 *         throws = {UserNotFoundException.class}
 *     )
 *     UserProfile getUser(String userId);
 *
 *     &#64;IThriftSpec(
 *         serviceMethod = "updateUser",
 *         oneway = false
 *     )
 *     boolean updateUser(UserProfile profile);
 * }
 * }</pre>
 *
 * <h2>Generated Thrift IDL</h2>
 *
 * <pre>
 * namespace java com.example.thrift
 *
 * struct UserProfile {
 *   1: required string userId;
 *   2: required string name;
 *   3: optional string email;
 * }
 *
 * exception UserNotFoundException {
 *   1: string message;
 * }
 *
 * service UserService {
 *   UserProfile getUser(1: string userId) throws (1: UserNotFoundException e);
 *   bool updateUser(1: UserProfile profile);
 * }
 * </pre>
 *
 * @since IOP 1.0 - Thrift Integration
 * @see IHttpSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares Thrift method, transport, protocol, server, pooling and retry preferences; it generates no IDL, starts no server and opens no connection.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IThriftSpec.java}
 * blob {@code 9fd217b398c08808780811e799513a3e837fbbe1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IThriftSpec {

    // --- SERVICE DEFINITION ---

    /**
     * Thrift service method name. Default: Java method name.
     *
     * @return method name in Thrift IDL
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#serviceMethod intent: Thrift service method name. Default: Java method name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String serviceMethod() default "";

    /**
     * Whether this is a oneway method (fire-and-forget).
     *
     * @return true for oneway
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#oneway intent: Whether this is a oneway method (fire-and-forget). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean oneway() default false;

    /**
     * Exception types that can be thrown.
     *
     * @return exception classes
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#exceptions intent: Exception types that can be thrown. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Exception>[]}; default expression {@code {}}.
     */
    Class<? extends Exception>[] exceptions() default {};

    // --- TRANSPORT ---

    /**
     * Thrift transport type.
     *
     * @return transport
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#transport intent: Thrift transport type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Transport}; default expression {@code Transport.FRAMED}.
     */
    Transport transport() default Transport.FRAMED;

    /** Transport types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IThriftSpec.Transport choices described as Transport types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Transport {
        /** Simple blocking transport.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.Transport#BLOCKING policy choice: Simple blocking transport. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.Transport; its declared order and identity are preserved.
         */
        BLOCKING,
        /** Framed transport (recommended).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.Transport#FRAMED policy choice: Framed transport (recommended). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.Transport; its declared order and identity are preserved.
         */
        FRAMED,
        /** HTTP transport.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.Transport#HTTP policy choice: HTTP transport. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.Transport; its declared order and identity are preserved.
         */
        HTTP,
        /** Non-blocking (NIO).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.Transport#NON_BLOCKING policy choice: Non-blocking (NIO). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.Transport; its declared order and identity are preserved.
         */
        NON_BLOCKING
    }

    // --- PROTOCOL ---

    /**
     * Thrift protocol type.
     *
     * @return protocol
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#protocol intent: Thrift protocol type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Protocol}; default expression {@code Protocol.BINARY}.
     */
    Protocol protocol() default Protocol.BINARY;

    /** Protocol types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IThriftSpec.Protocol choices described as Protocol types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Protocol {
        /** Binary protocol (fastest).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.Protocol#BINARY policy choice: Requested Thrift binary protocol; the token establishes no comparative performance guarantee. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.Protocol; its declared order and identity are preserved.
         */
        BINARY,
        /** Compact binary protocol.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.Protocol#COMPACT policy choice: Compact binary protocol. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.Protocol; its declared order and identity are preserved.
         */
        COMPACT,
        /** JSON protocol (human-readable).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.Protocol#JSON policy choice: JSON protocol (human-readable). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.Protocol; its declared order and identity are preserved.
         */
        JSON,
        /** Simple JSON protocol.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.Protocol#SIMPLE_JSON policy choice: Simple JSON protocol. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.Protocol; its declared order and identity are preserved.
         */
        SIMPLE_JSON
    }

    // --- SERVER OPTIONS ---

    /**
     * Server type for generated server.
     *
     * @return server type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#serverType intent: Server type for generated server. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ServerType}; default expression {@code ServerType.THREADED_SELECTOR}.
     */
    ServerType serverType() default ServerType.THREADED_SELECTOR;

    /** Server types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IThriftSpec.ServerType choices described as Server types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum ServerType {
        /** Simple single-threaded server.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.ServerType#SIMPLE policy choice: Simple single-threaded server. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.ServerType; its declared order and identity are preserved.
         */
        SIMPLE,
        /** Thread pool server.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.ServerType#THREAD_POOL policy choice: Thread pool server. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.ServerType; its declared order and identity are preserved.
         */
        THREAD_POOL,
        /** Threaded selector (NIO).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.ServerType#THREADED_SELECTOR policy choice: Threaded selector (NIO). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.ServerType; its declared order and identity are preserved.
         */
        THREADED_SELECTOR,
        /** Half-sync/half-async.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IThriftSpec.ServerType#HSSHA policy choice: Half-sync/half-async. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IThriftSpec.ServerType; its declared order and identity are preserved.
         */
        HSSHA
    }

    /**
     * Server port (0 = dynamic).
     *
     * @return port number
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#port intent: Server port (0 = dynamic). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int port() default 0;

    /**
     * Maximum number of worker threads.
     *
     * @return max threads
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#maxWorkers intent: Maximum number of worker threads. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 64}.
     */
    int maxWorkers() default 64;

    // --- CLIENT OPTIONS ---

    /**
     * Connection timeout in milliseconds.
     *
     * @return timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#connectTimeoutMs intent: Connection timeout in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 5000}.
     */
    int connectTimeoutMs() default 5000;

    /**
     * Socket timeout in milliseconds.
     *
     * @return timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#socketTimeoutMs intent: Socket timeout in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
     */
    int socketTimeoutMs() default 30000;

    /**
     * Whether to use connection pooling.
     *
     * @return true for pooling
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#pooled intent: Whether to use connection pooling. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean pooled() default true;

    /**
     * Maximum pool size.
     *
     * @return pool size
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#maxPoolSize intent: Maximum pool size. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 10}.
     */
    int maxPoolSize() default 10;

    // --- RETRY ---

    /**
     * Maximum retry attempts.
     *
     * @return retries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#maxRetries intent: Maximum retry attempts. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3}.
     */
    int maxRetries() default 3;

    /**
     * Retry backoff in milliseconds.
     *
     * @return backoff
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#retryBackoffMs intent: Retry backoff in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1000}.
     */
    int retryBackoffMs() default 1000;

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IThriftSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
