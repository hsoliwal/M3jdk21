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
 * <li>{@link AIScope} - change blast radius (LOCAL, PACKAGE, MODULE,
 * PROJECT)</li>
 * <li>{@link AIInstanceScope} - instance cardinality (SINGLETON, PROTOTYPE,
 * REQUEST...)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;AIInstanceScope(Cardinality.SINGLETON)
 *     public interface IConnectionPool {
 *         // LLM knows: one instance, thread-safe, shared state OK
 *     }
 *
 *     &#64;AIInstanceScope(Cardinality.PROTOTYPE)
 *     public interface IRequestContext {
 *         // LLM knows: new instance each time, no shared state
 *     }
 *
 *     &#64;AIInstanceScope(value = Cardinality.REQUEST, proxyMode = ProxyMode.INTERFACES)
 *     public interface IUserSession {
 *         // LLM knows: one per HTTP request, scoped proxy
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Instance Scope Hints (Spring-inspired)
 * @see AIScope for change blast radius (different concept)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Scope.html">Spring @Scope</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIInstanceScope.java}
 * blob {@code 6db340cbd34a240c7050334b907610026f044359} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIInstanceScope {

    /**
     * Instance cardinality.
     *
     * @return cardinality
     */
    Cardinality value() default Cardinality.SINGLETON;

    /**
     * Proxy mode hint (for scoped proxies).
     *
     * @return proxy mode
     */
    ProxyMode proxyMode() default ProxyMode.DEFAULT;

    /**
     * Custom scope name (if using custom scopes).
     *
     * @return custom scope name
     */
    String scopeName() default "";

    /**
     * Thread-safety required hint.
     *
     * @return true if thread-safety expected
     */
    boolean threadSafe() default true;

    // --- ENUMS - Spring-compatible vocabulary ---

    /** Instance cardinality (Spring Scope-compatible names). */
    enum Cardinality {
        // --- STANDARD SCOPES (Spring Core) ---

        /**
         * Singleton (Spring: "singleton"). One instance per
         * container/application. Thread-safety required.
         */
        SINGLETON,

        /**
         * Prototype (Spring: "prototype"). New instance for each
         * injection/lookup. No shared state.
         */
        PROTOTYPE,

        // --- WEB SCOPES (Spring Web) ---

        /** Request scope (Spring: "request"). One instance per HTTP request. */
        REQUEST,

        /** Session scope (Spring: "session"). One instance per HTTP session. */
        SESSION,

        /**
         * Application scope (Spring: "application"). One instance per
         * ServletContext.
         */
        APPLICATION,

        /**
         * WebSocket scope (Spring: "websocket"). One instance per WebSocket
         * session.
         */
        WEBSOCKET,

        // --- CUSTOM/ADVANCED SCOPES ---

        /** Thread scope. One instance per thread (ThreadLocal). */
        THREAD,

        /** Refresh scope (Spring Cloud). Re-created on configuration refresh. */
        REFRESH,

        /** Transaction scope. One instance per transaction. */
        TRANSACTION,

        /** Conversation scope (CDI). Spans multiple requests in a conversation. */
        CONVERSATION,

        /** Custom scope (use scopeName()). */
        CUSTOM
    }

    /** Proxy modes (Spring ScopedProxyMode equivalent). */
    enum ProxyMode {
        /** Default: no proxy unless required by scope. */
        DEFAULT,

        /** No scoped proxy. */
        NO,

        /** JDK interface-based proxy. */
        INTERFACES,

        /** CGLIB class-based proxy. */
        TARGET_CLASS
    }
}
