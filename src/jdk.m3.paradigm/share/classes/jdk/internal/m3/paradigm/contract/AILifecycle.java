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

import java.lang.annotation.*;

/**
 * AILifecycle - AnOP annotation for component lifecycle hints.
 *
 * <h2>AnOP Advisory: Lifecycle Hints</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Lifecycle describes WHEN and HOW a component is created/destroyed.       ║
 * ║  Lifecycle hints guide DI container integration code generation.          ║
 * ║  Actual lifecycle management is done by runtime container.                ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;ISpec(value = "Database connection pool")
 *     &#64;AILifecycle(scope = Scope.SINGLETON, init = Phase.EAGER, destroy = "close", order = 10)
 *     public interface IConnectionPool {
 *
 *         Connection acquire();
 *
 *         void release(Connection conn);
 *     }
 *
 *     &#64;AILifecycle(scope = Scope.REQUEST, init = Phase.LAZY)
 *     public interface IRequestContext {
 *
 *         String getRequestId();
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Lifecycle Hints
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AILifecycle.java}
 * blob {@code 78db03bbc3bbea0a20ea5f37b084cc3d4f5c05db} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AILifecycle {

    /**
     * Component scope.
     *
     * @return scope
     */
    Scope scope() default Scope.SINGLETON;

    /**
     * Initialization phase.
     *
     * @return init phase
     */
    Phase init() default Phase.LAZY;

    /**
     * Initialization method name.
     *
     * @return init method
     */
    String initMethod() default "";

    /**
     * Destruction method name.
     *
     * @return destroy method
     */
    String destroy() default "";

    /**
     * Initialization order (lower = earlier).
     *
     * @return order
     */
    int order() default 0;

    /**
     * Dependencies that must be initialized first.
     *
     * @return dependency names
     */
    String[] dependsOn() default {};

    /**
     * Thread-safe hint.
     *
     * @return true if thread-safe
     */
    boolean threadSafe() default true;

    /** Component scopes (aligned with Spring/Jakarta). */
    enum Scope {
        // --- STANDARD SCOPES ---

        /** Single instance per container. */
        SINGLETON,

        /** New instance each time. */
        PROTOTYPE,

        /** One per HTTP request. */
        REQUEST,

        /** One per HTTP session. */
        SESSION,

        /** One per application. */
        APPLICATION,

        // --- EXTENDED SCOPES ---

        /** One per thread. */
        THREAD,

        /** One per conversation/workflow. */
        CONVERSATION,

        /** One per transaction. */
        TRANSACTION,

        /** Custom scope (use scopeName). */
        CUSTOM
    }

    /** Initialization phases. */
    enum Phase {
        /** Initialize immediately on container start. */
        EAGER,

        /** Initialize on first use. */
        LAZY,

        /** Initialize in background thread. */
        ASYNC,

        /** Initialize on-demand (manual trigger). */
        ON_DEMAND
    }
}
