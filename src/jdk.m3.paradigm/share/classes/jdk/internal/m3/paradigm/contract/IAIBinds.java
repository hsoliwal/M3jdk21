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
 * AIBinds - AnOP annotation for interface-to-implementation binding hints.
 *
 * <h2>Dagger / Guice Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ Dagger: @Binds → abstract method binds interface to impl ║
 * ║ Guice: bind(I).to(Impl) → programmatic binding ║
 * ║ AnOP: @AIBinds → compile-time binding declaration ║
 * ║ ║
 * ║ Frameworks say: "Bind this interface to this implementation" ║
 * ║ AnOP says: "This implementation fulfills this interface" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT create bindings. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;AIModule
 * 	public abstract class RepositoryModule {
 *
 * 		&#64;AIBinds
 * 		abstract IUserRepository bindUserRepo(JpaUserRepository impl);
 * 		// LLM knows: IUserRepository is implemented by JpaUserRepository
 *
 * 		&#64;AIBinds
 * 		&#64;AIQualifier("cached")
 * 		abstract IUserRepository bindCachedRepo(CachedUserRepository impl);
 * 		// LLM knows: qualified binding
 * 	}
 * }
 * </pre>
 *
 * <p>
 * This is more efficient than @AIProvides when simply delegating to an existing
 * implementation (no object creation logic needed).
 * </p>
 *
 * @since IOP 1.0 - AnOP Binding Hints (Dagger-inspired)
 * @see <a href="https://dagger.dev/dev-guide/">Dagger @Binds</a>
 * @m3.pattern spring:dependency-injection
 * @m3.algorithm Declares an abstract implementation binding and optional multibinding destination; this annotation performs no binding or dispatch.
 * @m3.dataStructures SOURCE-retained annotation targeting ElementType.METHOD; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIBinds.java}
 * blob {@code 91221657574b43c1f3b295585205c174eeec9e08} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIBinds {

    /**
     * Into collection hint (multibinding).
     *
     * @return collection type
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Selects the implementation binding collection destination; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures IntoCollection annotation element; default expression IntoCollection.NONE.
     */
    IntoCollection into() default IntoCollection.NONE;

    /**
     * Map key for multibinding.
     *
     * @return map key
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Names the map key of the implementation binding; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String mapKey() default "";

    /** Collection binding types.
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Enumerates 4 declared abstract-binding multibinding destinations; selecting a token performs no corresponding runtime operation.
     * @m3.dataStructures Nested IAIBinds.IntoCollection enum with 4 source-declared constants in existing order.
     */
    enum IntoCollection {
        /** Not a collection binding.
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIBinds.IntoCollection#NONE for abstract-binding multibinding destinations: Not a collection binding. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIBinds.IntoCollection enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        NONE,

        /** Contribute to a Set&lt;T>.
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIBinds.IntoCollection#SET for abstract-binding multibinding destinations: Contribute to a Set&lt;T>. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIBinds.IntoCollection enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        SET,

        /** Contribute to a Map&lt;K, V>.
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIBinds.IntoCollection#MAP for abstract-binding multibinding destinations: Contribute to a Map&lt;K, V>. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIBinds.IntoCollection enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        MAP,

        /** Contribute elements to Set&lt;T>.
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIBinds.IntoCollection#ELEMENTS_INTO_SET for abstract-binding multibinding destinations: Contribute elements to Set&lt;T>. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIBinds.IntoCollection enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        ELEMENTS_INTO_SET
    }
}
