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
 * AIInstanceScope - AnOP annotation for instance cardinality hints.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Scope("singleton") → runtime container manages instances ║
 * ║ AnOP: @AIInstanceScope → compile-time declaration of intent ║
 * ║ ║
 * ║ Spring says: "I will create and manage instances for you" ║
 * ║ AnOP says: "This code expects this instance cardinality" ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>The Invariant</h2> <blockquote> Spring @Scope controls how the container
 * creates instances.<br>
 * AnOP @AIInstanceScope declares what cardinality the code expects.
 * </blockquote>
 *
 * <h2>Difference from AIScope</h2>
 * <ul>
 * <li>{@link IAIScope} - change blast radius (LOCAL, PACKAGE, MODULE,
 * PROJECT)</li>
 * <li>{@link IAIInstanceScope} - instance cardinality (SINGLETON, PROTOTYPE,
 * REQUEST...)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;AIInstanceScope(Cardinality.SINGLETON)
 * 	public interface IConnectionPool&lt;Self extends IConnectionPool&lt;Self>> extends IContext&lt;Self> {
 * 		// LLM knows: one instance, thread-safe, shared state OK
 * 	}
 *
 * 	&#64;AIInstanceScope(Cardinality.PROTOTYPE)
 * 	public interface IRequestContext&lt;Self extends IRequestContext&lt;Self>> extends IContext&lt;Self> {
 * 		// LLM knows: new instance each time, no shared state
 * 	}
 *
 * 	&#64;AIInstanceScope(value = Cardinality.REQUEST, proxyMode = ProxyMode.INTERFACES)
 * 	public interface IUserSession&lt;Self extends IUserSession&lt;Self>> extends IContext&lt;Self> {
 * 		// LLM knows: one per HTTP request, scoped proxy
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Instance Scope Hints (Spring-inspired)
 * @see IAIScope for change blast radius (different concept)
 * @m3.pattern spring:ioc-container
 * @m3.algorithm Declares instance cardinality, scoped proxy and thread-safety hints; this annotation allocates no instances or proxies and enforces no synchronization.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD}; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIInstanceScope.java}
 * blob {@code 7818c66f05b1166c67be212a7eb8c7c3fdba296b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIInstanceScope {

    /**
     * Instance cardinality.
     *
     * @return cardinality
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Selects the intended lifetime/cardinality of managed instances; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Cardinality annotation element; default expression Cardinality.SINGLETON.
     */
    Cardinality value() default Cardinality.SINGLETON;

    /**
     * Proxy mode hint (for scoped proxies).
     *
     * @return proxy mode
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Selects the intended scoped proxy strategy; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures ProxyMode annotation element; default expression ProxyMode.DEFAULT.
     */
    ProxyMode proxyMode() default ProxyMode.DEFAULT;

    /**
     * Custom scope name (if using custom scopes).
     *
     * @return custom scope name
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Names a custom instance scope; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String scopeName() default "";

    /**
     * Thread-safety required hint.
     *
     * @return true if thread-safety expected
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Declares a thread-safety expectation for scoped instances; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression true.
     */
    boolean threadSafe() default true;

    // --- ENUMS - Spring-compatible vocabulary ---

    /** Instance cardinality (Spring Scope-compatible names).
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Enumerates 11 declared managed instance lifetime and cardinality choices; selecting a token performs no corresponding runtime operation.
     * @m3.dataStructures Nested IAIInstanceScope.Cardinality enum with 11 source-declared constants in existing order.
     */
    enum Cardinality {
        // --- STANDARD SCOPES (Spring Core) ---

        /**
         * Singleton (Spring: "singleton"). One instance per container/application.
         * Thread-safety required.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.Cardinality#SINGLETON for managed instance lifetime and cardinality choices: Singleton (Spring: "singleton"). One instance per container/application. Thread-safety required. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.Cardinality enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        SINGLETON,

        /**
         * Prototype (Spring: "prototype"). New instance for each injection/lookup. No
         * shared state.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.Cardinality#PROTOTYPE for managed instance lifetime and cardinality choices: Prototype (Spring: "prototype"). New instance for each injection/lookup. No shared state. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.Cardinality enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        PROTOTYPE,

        // --- WEB SCOPES (Spring Web) ---

        /** Request scope (Spring: "request"). One instance per HTTP request.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.Cardinality#REQUEST for managed instance lifetime and cardinality choices: Request scope (Spring: "request"). One instance per HTTP request. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.Cardinality enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        REQUEST,

        /** Session scope (Spring: "session"). One instance per HTTP session.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.Cardinality#SESSION for managed instance lifetime and cardinality choices: Session scope (Spring: "session"). One instance per HTTP session. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.Cardinality enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        SESSION,

        /**
         * Application scope (Spring: "application"). One instance per ServletContext.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.Cardinality#APPLICATION for managed instance lifetime and cardinality choices: Application scope (Spring: "application"). One instance per ServletContext. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.Cardinality enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        APPLICATION,

        /**
         * WebSocket scope (Spring: "websocket"). One instance per WebSocket session.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.Cardinality#WEBSOCKET for managed instance lifetime and cardinality choices: WebSocket scope (Spring: "websocket"). One instance per WebSocket session. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.Cardinality enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        WEBSOCKET,

        // --- CUSTOM/ADVANCED SCOPES ---

        /** Thread scope. One instance per thread (ThreadLocal).
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.Cardinality#THREAD for managed instance lifetime and cardinality choices: Thread scope. One instance per thread (ThreadLocal). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.Cardinality enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        THREAD,

        /** Refresh scope (Spring Cloud). Re-created on configuration refresh.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.Cardinality#REFRESH for managed instance lifetime and cardinality choices: Refresh scope (Spring Cloud). Re-created on configuration refresh. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.Cardinality enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        REFRESH,

        /** Transaction scope. One instance per transaction.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.Cardinality#TRANSACTION for managed instance lifetime and cardinality choices: Transaction scope. One instance per transaction. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.Cardinality enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        TRANSACTION,

        /** Conversation scope (CDI). Spans multiple requests in a conversation.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.Cardinality#CONVERSATION for managed instance lifetime and cardinality choices: Conversation scope (CDI). Spans multiple requests in a conversation. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.Cardinality enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        CONVERSATION,

        /** Custom scope (use scopeName()).
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.Cardinality#CUSTOM for managed instance lifetime and cardinality choices: Custom scope (use scopeName()). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.Cardinality enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        CUSTOM
    }

    /** Proxy modes (Spring ScopedProxyMode equivalent).
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Enumerates 4 declared scoped proxy mechanisms; selecting a token performs no corresponding runtime operation.
     * @m3.dataStructures Nested IAIInstanceScope.ProxyMode enum with 4 source-declared constants in existing order.
     */
    enum ProxyMode {
        /** Default: no proxy unless required by scope.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.ProxyMode#DEFAULT for scoped proxy mechanisms: Default: no proxy unless required by scope. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.ProxyMode enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        DEFAULT,

        /** No scoped proxy.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.ProxyMode#NO for scoped proxy mechanisms: No scoped proxy. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.ProxyMode enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        NO,

        /** JDK interface-based proxy.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.ProxyMode#INTERFACES for scoped proxy mechanisms: JDK interface-based proxy. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.ProxyMode enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        INTERFACES,

        /** CGLIB class-based proxy.
         * @m3.pattern spring:ioc-container
         * @m3.algorithm Declares IAIInstanceScope.ProxyMode#TARGET_CLASS for scoped proxy mechanisms: CGLIB class-based proxy. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInstanceScope.ProxyMode enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        TARGET_CLASS
    }
}
