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
 * AISubcomponent - AnOP annotation for child component hints.
 *
 * <h2>Dagger Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ Dagger: @Subcomponent → child component with inherited bindings ║
 * ║ AnOP: @AISubcomponent → compile-time subcomponent declaration ║
 * ║ ║
 * ║ Dagger says: "This is a child graph inheriting from parent" ║
 * ║ AnOP says: "This component is scoped under a parent" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT create hierarchies. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;AISubcomponent(modules = {ActivityModule.class})
 * 	&#64;AIInstanceScope(Cardinality.REQUEST)
 * 	public interface ActivityComponent {
 *
 * 		ActivityPresenter presenter();
 *
 * 		&#64;AISubcomponent.Factory
 * 		interface Factory {
 *
 * 			ActivityComponent create(@AIBindsInstance Activity activity);
 * 		}
 * 	}
 *
 * 	// Parent component exposes the factory
 * 	&#64;AIComponent(modules = {AppModule.class})
 * 	public interface AppComponent {
 *
 * 		ActivityComponent.Factory activityComponentFactory();
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Subcomponent Hints (Dagger-inspired)
 * @see <a href="https://dagger.dev/dev-guide/">Dagger @Subcomponent</a>
 * @m3.pattern spring:dependency-injection
 * @m3.algorithm Declares a child binding graph with inherited parent bindings and additional modules; this annotation creates no subcomponent.
 * @m3.dataStructures SOURCE-retained annotation targeting ElementType.TYPE; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAISubcomponent.java}
 * blob {@code 361ebfe68db1d77e3f1eafd103f28b85066fa2c7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAISubcomponent {

    /**
     * Modules that provide bindings for this subcomponent.
     *
     * @return module classes
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Lists the additional module type tokens for the child binding graph; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Class&lt;?>[] annotation element; default expression {}.
     */
    Class<?>[] modules() default {};

    /**
     * Subcomponent description.
     *
     * @return description
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Carries the subcomponent purpose description; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String description() default "";

    // --- NESTED ANNOTATIONS ---

    /** Marks a factory interface for creating subcomponent instances.
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Marks a subcomponent factory interface for construction intent; this annotation allocates or assembles no subcomponent.
     * @m3.dataStructures Memberless nested SOURCE-retained TYPE annotation for the subcomponent factory role.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface Factory {}

    /** Marks a builder interface for creating subcomponent instances.
     * @m3.pattern gof:builder
     * @m3.algorithm Marks a subcomponent builder interface for construction intent; this annotation allocates or assembles no subcomponent.
     * @m3.dataStructures Memberless nested SOURCE-retained TYPE annotation for the subcomponent builder role.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface Builder {}
}
