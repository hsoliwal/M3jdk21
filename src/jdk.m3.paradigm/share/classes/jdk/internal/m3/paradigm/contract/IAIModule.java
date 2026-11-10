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
 * AIModule - AnOP annotation for dependency module hints.
 *
 * <h2>Guice / Dagger Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ Guice: Module (interface) → configure() defines bindings ║
 * ║ Dagger: @Module → contains @Provides methods ║
 * ║ AnOP: @AIModule → compile-time module declaration ║
 * ║ ║
 * ║ Frameworks say: "This class defines dependency bindings" ║
 * ║ AnOP says: "This class is a dependency configuration unit" ║
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
 * 	&#64;AIModule(includes = {DatabaseModule.class, CacheModule.class})
 * 	public class AppModule {
 *
 * 		&#64;AIProvides
 * 		UserService provideUserService(IUserRepository repo) {
 * 			return new UserServiceImpl(repo);
 * 		}
 * 	}
 *
 * 	&#64;AIModule(subcomponents = {UserComponent.class})
 * 	public abstract class CoreModule {
 *
 * 		&#64;AIBinds
 * 		abstract ILogger bindLogger(ConsoleLogger impl);
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Module Hints (Guice/Dagger-inspired)
 * @see <a href="https://dagger.dev/dev-guide/">Dagger @Module</a>
 * @see <a href="https://www.baeldung.com/guice">Guice Guide</a>
 * @m3.pattern spring:configuration
 * @m3.algorithm Declares a Dagger-style group of bindings, module inclusions and subcomponents; this annotation installs or validates no module.
 * @m3.dataStructures SOURCE-retained annotation targeting ElementType.TYPE; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIModule.java}
 * blob {@code 2b5eafba5d1bf706bfd12380c48f3f8f9dd005e7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIModule {

    /**
     * Other modules to include. Similar to Dagger's @Module(includes = {...}).
     *
     * @return included module classes
     * @m3.pattern spring:configuration
     * @m3.algorithm Lists included module type tokens; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Class&lt;?>[] annotation element; default expression {}.
     */
    Class<?>[] includes() default {};

    /**
     * Subcomponents this module installs. Similar to Dagger's @Module(subcomponents
     * = {...}).
     *
     * @return subcomponent classes
     * @m3.pattern spring:configuration
     * @m3.algorithm Lists installed subcomponent type tokens; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Class&lt;?>[] annotation element; default expression {}.
     */
    Class<?>[] subcomponents() default {};

    /**
     * Module is complete (all bindings satisfied internally). Similar to Dagger's
     * complete validation.
     *
     * @return true if complete
     * @m3.pattern spring:configuration
     * @m3.algorithm Declares whether all bindings are intended to be satisfied internally; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression true.
     */
    boolean complete() default true;

    /**
     * Module description.
     *
     * @return description
     * @m3.pattern spring:configuration
     * @m3.algorithm Carries the binding module purpose description; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String description() default "";

    /**
     * Module category hint.
     *
     * @return category
     * @m3.pattern spring:configuration
     * @m3.algorithm Selects the binding module category hint; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Category annotation element; default expression Category.GENERAL.
     */
    Category category() default Category.GENERAL;

    /** Module categories.
     * @m3.pattern spring:configuration
     * @m3.algorithm Enumerates 7 declared binding module purposes; selecting a token performs no corresponding runtime operation.
     * @m3.dataStructures Nested IAIModule.Category enum with 7 source-declared constants in existing order.
     */
    enum Category {
        /** General-purpose module.
         * @m3.pattern spring:configuration
         * @m3.algorithm Declares IAIModule.Category#GENERAL for binding module purposes: General-purpose module. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIModule.Category enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        GENERAL,

        /** Application-level bindings.
         * @m3.pattern spring:configuration
         * @m3.algorithm Declares IAIModule.Category#APPLICATION for binding module purposes: Application-level bindings. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIModule.Category enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        APPLICATION,

        /** Activity/request-level bindings (Android/web).
         * @m3.pattern spring:configuration
         * @m3.algorithm Declares IAIModule.Category#ACTIVITY for binding module purposes: Activity/request-level bindings (Android/web). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIModule.Category enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        ACTIVITY,

        /** Feature-specific module.
         * @m3.pattern spring:configuration
         * @m3.algorithm Declares IAIModule.Category#FEATURE for binding module purposes: Feature-specific module. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIModule.Category enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        FEATURE,

        /** Infrastructure/technical concerns.
         * @m3.pattern spring:configuration
         * @m3.algorithm Declares IAIModule.Category#INFRASTRUCTURE for binding module purposes: Infrastructure/technical concerns. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIModule.Category enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        INFRASTRUCTURE,

        /** Testing/mock module.
         * @m3.pattern spring:configuration
         * @m3.algorithm Declares IAIModule.Category#TEST for binding module purposes: Testing/mock module. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIModule.Category enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        TEST,

        /** Development-only module.
         * @m3.pattern spring:configuration
         * @m3.algorithm Declares IAIModule.Category#DEV for binding module purposes: Development-only module. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIModule.Category enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        DEV
    }
}
