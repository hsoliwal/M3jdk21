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
 * AIComponent - AnOP annotation for dependency graph root hints.
 *
 * <h2>Dagger Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ Dagger: @Component → interface defining the dependency graph root ║
 * ║ AnOP: @AIComponent → compile-time component declaration ║
 * ║ ║
 * ║ Dagger says: "Generate code to build this dependency graph" ║
 * ║ AnOP says: "This is a dependency graph boundary" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT generate graphs. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;AIComponent(modules = {AppModule.class, NetworkModule.class})
 * 	&#64;AIInstanceScope(Cardinality.SINGLETON)
 * 	public interface AppComponent {
 *
 * 		// Provision methods - expose dependencies
 * 		UserService userService();
 *
 * 		ILogger logger();
 *
 * 		// Member injection methods
 * 		void inject(MainActivity activity);
 *
 * 		// Subcomponent factory
 * 		UserComponent.Factory userComponentFactory();
 * 	}
 *
 * 	&#64;AIComponent(dependencies = {AppComponent.class}, modules = {UserModule.class})
 * 	&#64;AIInstanceScope(Cardinality.REQUEST)
 * 	public interface UserComponent {
 *
 * 		UserPresenter presenter();
 *
 * 		&#64;AIComponent.Factory
 * 		interface Factory {
 *
 * 			UserComponent create(@AIBindsInstance User user);
 * 		}
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Component Hints (Dagger-inspired)
 * @see <a href="https://dagger.dev/dev-guide/">Dagger @Component</a>
 * @m3.pattern spring:dependency-injection
 * @m3.algorithm Declares the root of a Dagger-style binding graph and its module/dependency type tokens; this annotation builds no graph or component.
 * @m3.dataStructures SOURCE-retained annotation targeting ElementType.TYPE; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIComponent.java}
 * blob {@code eef9a4913d45d77db5b3f16a263a08b763dd170a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIComponent {

    /**
     * Modules that provide bindings for this component.
     *
     * @return module classes
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Lists the module type tokens that provide root component bindings; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Class&lt;?>[] annotation element; default expression {}.
     */
    Class<?>[] modules() default {};

    /**
     * Component dependencies (other components this depends on).
     *
     * @return dependency component classes
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Lists the external component dependency type tokens; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Class&lt;?>[] annotation element; default expression {}.
     */
    Class<?>[] dependencies() default {};

    /**
     * Component description.
     *
     * @return description
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Carries the root component purpose description; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String description() default "";

    // --- NESTED ANNOTATIONS ---

    /**
     * Marks a factory interface for creating component instances. Similar to
     * Dagger's @Component.Factory.
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Marks a component factory interface for construction intent; this annotation allocates or assembles no component.
     * @m3.dataStructures Memberless nested SOURCE-retained TYPE annotation for the component factory role.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface Factory {}

    /**
     * Marks a builder interface for creating component instances. Similar to
     * Dagger's @Component.Builder.
     * @m3.pattern gof:builder
     * @m3.algorithm Marks a component builder interface for construction intent; this annotation allocates or assembles no component.
     * @m3.dataStructures Memberless nested SOURCE-retained TYPE annotation for the component builder role.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface Builder {}
}
