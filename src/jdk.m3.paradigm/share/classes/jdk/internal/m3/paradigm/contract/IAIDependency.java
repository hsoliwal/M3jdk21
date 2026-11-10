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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIDependency - AnOP annotation for dependency injection hints.
 *
 * <h2>AnOP Advisory: Dependency Hints</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Dependencies describe WHAT a component needs.                            ║
 * ║  Dependency hints guide DI wiring code generation.                        ║
 * ║  Actual injection is done by runtime container.                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;ISpec(value = "User service implementation")
 * 	&#64;AIDependency(requires = {IUserRepository.class, IPasswordEncoder.class})
 * 	&#64;AIDependency(optional = IEmailService.class)
 * 	public interface IUserService&lt;Self extends IUserService&lt;Self>> extends IContext&lt;Self> {
 *
 * 		User createUser(UserRequest request);
 * 	}
 *
 * 	&#64;AIDependency(requires = ILogger.class, injection = Injection.CONSTRUCTOR)
 * 	public interface IAuditService&lt;Self extends IAuditService&lt;Self>> extends IContext&lt;Self> {
 *
 * 		void logAction(String action);
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Dependency Hints
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares required and optional dependency types, qualifier names, injection style and lazy/provider hints; it resolves no dependency and performs no injection.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIDependency.java}
 * blob {@code 70be6f058be97ee5d8c3df86c6fc89ebc908e3f6} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAIDependencies.class)
public @interface IAIDependency {

    /**
     * Required dependencies (must be present).
     *
     * @return required types
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIDependency#requires intent: Required dependencies (must be present). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>[]}; default expression {@code {}}.
     */
    Class<?>[] requires() default {};

    /**
     * Optional dependencies (may be null).
     *
     * @return optional types
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIDependency#optional intent: Optional dependencies (may be null). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>[]}; default expression {@code {}}.
     */
    Class<?>[] optional() default {};

    /**
     * Dependency names (for qualifier hints).
     *
     * @return dependency names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIDependency#named intent: Dependency names (for qualifier hints). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] named() default {};

    /**
     * Injection style hint.
     *
     * @return injection style
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIDependency#injection intent: Injection style hint. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Injection}; default expression {@code Injection.CONSTRUCTOR}.
     */
    Injection injection() default Injection.CONSTRUCTOR;

    /**
     * Lazy loading hint.
     *
     * @return true for lazy
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIDependency#lazy intent: Lazy loading hint. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean lazy() default false;

    /**
     * Provider/Factory hint (for scoped dependencies).
     *
     * @return true for provider pattern
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIDependency#provider intent: Provider/Factory hint (for scoped dependencies). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean provider() default false;

    /** Injection styles.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAIDependency.Injection choices described as Injection styles. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Injection {
        // --- STANDARD INJECTION ---

        /** Inject via constructor (preferred).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIDependency.Injection#CONSTRUCTOR policy choice: Inject via constructor (preferred). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIDependency.Injection; its declared order and identity are preserved.
         */
        CONSTRUCTOR,

        /** Inject via setter method.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIDependency.Injection#SETTER policy choice: Inject via setter method. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIDependency.Injection; its declared order and identity are preserved.
         */
        SETTER,

        /** Inject directly into field.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIDependency.Injection#FIELD policy choice: Inject directly into field. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIDependency.Injection; its declared order and identity are preserved.
         */
        FIELD,

        // --- LOOKUP PATTERNS ---

        /** Lookup from container/registry.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIDependency.Injection#LOOKUP policy choice: Lookup from container/registry. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIDependency.Injection; its declared order and identity are preserved.
         */
        LOOKUP,

        /** Provider/Factory pattern.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIDependency.Injection#PROVIDER policy choice: Provider/Factory pattern. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIDependency.Injection; its declared order and identity are preserved.
         */
        PROVIDER,

        /** Method injection (parameter).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIDependency.Injection#METHOD policy choice: Method injection (parameter). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIDependency.Injection; its declared order and identity are preserved.
         */
        METHOD
    }
}
