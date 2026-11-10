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
 * 	&#64;code
 * 	&#64;ISpec(value = "Database connection pool")
 * 	&#64;AILifecycle(scope = Scope.SINGLETON, init = Phase.EAGER, destroy = "close", order = 10)
 * 	public interface IConnectionPool&lt;Self extends IConnectionPool&lt;Self>> extends IContext&lt;Self> {
 *
 * 		Connection acquire();
 *
 * 		void release(Connection conn);
 * 	}
 *
 * 	&#64;AILifecycle(scope = Scope.REQUEST, init = Phase.LAZY)
 * 	public interface IRequestContext&lt;Self extends IRequestContext&lt;Self>> extends IContext&lt;Self> {
 *
 * 		String getRequestId();
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Lifecycle Hints
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares component scope, initialization phase, callback names, order, dependencies and thread-safety hints without creating components or executing callbacks.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code ElementType.TYPE}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAILifecycle.java}
 * blob {@code 317c7b40fce10ee67173d908261df0d40be5dbb4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAILifecycle {

    /**
     * Component scope.
     *
     * @return scope
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares scope metadata for Component scope. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Scope}, with exact default {@code Scope.SINGLETON}.
     */
    Scope scope() default Scope.SINGLETON;

    /**
     * Initialization phase.
     *
     * @return init phase
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares init metadata for Initialization phase. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Phase}, with exact default {@code Phase.LAZY}.
     */
    Phase init() default Phase.LAZY;

    /**
     * Initialization method name.
     *
     * @return init method
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares initMethod metadata for Initialization method name. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String initMethod() default "";

    /**
     * Destruction method name.
     *
     * @return destroy method
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares destroy metadata for Destruction method name. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String destroy() default "";

    /**
     * Initialization order (lower = earlier).
     *
     * @return order
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares order metadata for Initialization order (lower = earlier). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}, with exact default {@code 0}.
     */
    int order() default 0;

    /**
     * Dependencies that must be initialized first.
     *
     * @return dependency names
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares dependsOn metadata for Dependencies that must be initialized first. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}, with exact default {@code {}}.
     */
    String[] dependsOn() default {};

    /**
     * Thread-safe hint.
     *
     * @return true if thread-safe
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares threadSafe metadata for Thread-safe hint. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code true}.
     */
    boolean threadSafe() default true;

    /** Component scopes (aligned with Spring/Jakarta).
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 9 symbolic component lifecycle scope hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAILifecycle.Scope enum with constants in existing declaration order.
     */
    enum Scope {
        // --- STANDARD SCOPES ---

        /** Single instance per container.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SINGLETON metadata choice: Single instance per container. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SINGLETON,

        /** New instance each time.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PROTOTYPE metadata choice: New instance each time. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PROTOTYPE,

        /** One per HTTP request.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the REQUEST metadata choice: One per HTTP request. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        REQUEST,

        /** One per HTTP session.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SESSION metadata choice: One per HTTP session. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SESSION,

        /** One per application.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the APPLICATION metadata choice: One per application. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        APPLICATION,

        // --- EXTENDED SCOPES ---

        /** One per thread.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the THREAD metadata choice: One per thread. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        THREAD,

        /** One per conversation/workflow.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CONVERSATION metadata choice: One per conversation/workflow. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CONVERSATION,

        /** One per transaction.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the TRANSACTION metadata choice: One per transaction. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        TRANSACTION,

        /** Custom scope (use scopeName).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CUSTOM metadata choice: Custom scope (use scopeName). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CUSTOM
    }

    /** Initialization phases.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 4 symbolic component initialization phase hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAILifecycle.Phase enum with constants in existing declaration order.
     */
    enum Phase {
        /** Initialize immediately on container start.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the EAGER metadata choice: Initialize immediately on container start. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        EAGER,

        /** Initialize on first use.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LAZY metadata choice: Initialize on first use. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        LAZY,

        /** Initialize in background thread.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ASYNC metadata choice: Initialize in background thread. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        ASYNC,

        /** Initialize on-demand (manual trigger).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ON_DEMAND metadata choice: Initialize on-demand (manual trigger). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        ON_DEMAND
    }
}
