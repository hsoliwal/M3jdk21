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
 * AIInject - AnOP annotation for injection point hints.
 *
 * <h2>JSR-330 / Guice / CDI / Dagger Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ JSR-330: @Inject → runtime injection by container ║
 * ║ Guice: @Inject → Guice injector wires dependencies ║
 * ║ CDI: @Inject → CDI container injects beans ║
 * ║ Dagger: @Inject → compile-time generated injection ║
 * ║ AnOP: @AIInject → compile-time injection point declaration ║
 * ║ ║
 * ║ Frameworks say: "I will inject the dependency" ║
 * ║ AnOP says: "This is an injection point" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT inject. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * // Constructor injection (preferred)
 * &#64;AIInject
 * public UserService(IUserRepository repository) {
 * 	// LLM knows: repository is injected
 * }
 *
 * // Field injection
 * &#64;AIInject
 * private ILogger logger;
 *
 * // Method injection
 * &#64;AIInject
 * void setConfig(IConfig config) {
 * 	// LLM knows: config is injected after construction
 * }
 *
 * // With qualifier
 * &#64;AIInject
 * &#64;AIQualifier("primary")
 * private DataSource dataSource;
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Injection Hints (JSR-330/Guice/CDI/Dagger-inspired)
 * @see <a href="https://github.com/google/guice/wiki/Injections">Guice
 *      Injections</a>
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 * @see <a href="https://dagger.dev/dev-guide/">Dagger 2</a>
 * @m3.pattern spring:dependency-injection
 * @m3.algorithm Declares dependency injection sites and optional, lazy, named and style hints; this annotation resolves or injects no dependency.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER}; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIInject.java}
 * blob {@code 846574682fd5a6a65fdcb5f3fed3e50940eb19ab} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIInject {

    /**
     * Injection is optional (don't fail if no binding). Similar to
     * Guice's @Nullable or CDI's Instance.isUnsatisfied().
     *
     * @return true if optional
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Marks absence of a binding as allowed; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression false.
     */
    boolean optional() default false;

    /**
     * Lazy injection hint (inject Provider/Supplier instead of instance). Similar
     * to Guice's Provider&lt;T> or CDI's Instance&lt;T>.
     *
     * @return true if lazy
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Requests deferred dependency access through a provider/supplier; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression false.
     */
    boolean lazy() default false;

    /**
     * Named qualifier (shorthand for @AIQualifier). Similar to Guice/CDI @Named.
     *
     * @return qualifier name
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Names the candidate qualifier; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String named() default "";

    /**
     * Injection style hint.
     *
     * @return style
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Selects the declared injection site style; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Style annotation element; default expression Style.CONSTRUCTOR.
     */
    Style style() default Style.CONSTRUCTOR;

    /** Injection styles.
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Enumerates 4 declared dependency injection sites; selecting a token performs no corresponding runtime operation.
     * @m3.dataStructures Nested IAIInject.Style enum with 4 source-declared constants in existing order.
     */
    enum Style {
        /** Constructor injection (preferred, immutable).
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIInject.Style#CONSTRUCTOR for dependency injection sites: Constructor injection (preferred, immutable). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInject.Style enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        CONSTRUCTOR,

        /** Field injection (simpler, but harder to test).
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIInject.Style#FIELD for dependency injection sites: Field injection (simpler, but harder to test). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInject.Style enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        FIELD,

        /** Method/setter injection (for optional deps).
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIInject.Style#METHOD for dependency injection sites: Method/setter injection (for optional deps). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInject.Style enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        METHOD,

        /** Parameter injection (in @Provides methods).
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIInject.Style#PARAMETER for dependency injection sites: Parameter injection (in @Provides methods). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIInject.Style enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        PARAMETER
    }
}
