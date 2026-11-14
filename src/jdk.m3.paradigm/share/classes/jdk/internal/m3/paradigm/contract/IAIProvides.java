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
 * AIProvides - AnOP annotation for provider method hints.
 *
 * <h2>Guice / Dagger / CDI Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ Guice: @Provides → method in Module provides binding ║
 * ║ Dagger: @Provides → compile-time provider method ║
 * ║ CDI: @Produces → producer method for injection ║
 * ║ AnOP: @AIProvides → compile-time provider declaration ║
 * ║ ║
 * ║ Frameworks say: "This method provides the dependency" ║
 * ║ AnOP says: "This method is a provider" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT wire providers. It declares INTENT. ║
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
 * 	public class AppModule {
 *
 * 		&#64;AIProvides
 * 		&#64;AIInstanceScope(Cardinality.SINGLETON)
 * 		DataSource provideDataSource(Config config) {
 * 			// LLM knows: this creates DataSource, singleton scope
 * 			return new HikariDataSource(config.getDbUrl());
 * 		}
 *
 * 		&#64;AIProvides
 * 		&#64;AIQualifier("cached")
 * 		IUserRepository provideCachedRepo(DataSource ds) {
 * 			// LLM knows: qualified provider, depends on DataSource
 * 			return new CachedUserRepository(ds);
 * 		}
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Provider Hints (Guice/Dagger/CDI-inspired)
 * @see <a href=
 *      "https://www.tutorialspoint.com/guice/guice_provides_annotation.htm">Guice @Provides</a>
 * @see <a href="https://dagger.dev/dev-guide/">Dagger @Provides</a>
 * @m3.pattern spring:dependency-injection
 * @m3.algorithm Declares a provider method and its return/collection binding hints; this annotation invokes no provider and creates no collection.
 * @m3.dataStructures SOURCE-retained annotation targeting ElementType.METHOD; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIProvides.java}
 * blob {@code 82cb9d9478abc1bc517d9d33fd25d10c66822b87} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIProvides {

    /**
     * Provider type hint.
     *
     * @return type
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Selects instance, provider, lazy or optional return intent; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures Type annotation element; default expression Type.INSTANCE.
     */
    Type type() default Type.INSTANCE;

    /**
     * Nullable return hint (can return null).
     *
     * @return true if nullable
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Declares whether the provider may return null; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression false.
     */
    boolean nullable() default false;

    /**
     * Static provider (no instance needed). Similar to Dagger's static @Provides.
     *
     * @return true if static
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Marks a provider callable without an owning instance; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression false.
     */
    boolean isStatic() default false;

    /**
     * Into collection hint (Dagger @IntoSet, @IntoMap).
     *
     * @return collection type
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Selects the set/map multibinding contribution mode; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures IntoCollection annotation element; default expression IntoCollection.NONE.
     */
    IntoCollection into() default IntoCollection.NONE;

    /**
     * Map key (for @IntoMap).
     *
     * @return map key
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Names the key for a map contribution; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String mapKey() default "";

    /** Provider types.
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Enumerates 4 declared provider return forms; selecting a token performs no corresponding runtime operation.
     * @m3.dataStructures Nested IAIProvides.Type enum with 4 source-declared constants in existing order.
     */
    enum Type {
        /** Provides a single instance.
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIProvides.Type#INSTANCE for provider return forms: Provides a single instance. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIProvides.Type enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        INSTANCE,

        /** Provides a Provider&lt;T> (lazy).
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIProvides.Type#PROVIDER for provider return forms: Provides a Provider&lt;T> (lazy). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIProvides.Type enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        PROVIDER,

        /** Provides a Lazy&lt;T> (cached lazy).
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIProvides.Type#LAZY for provider return forms: Provides a Lazy&lt;T> (cached lazy). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIProvides.Type enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        LAZY,

        /** Provides Optional&lt;T>.
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIProvides.Type#OPTIONAL for provider return forms: Provides Optional&lt;T>. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIProvides.Type enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        OPTIONAL
    }

    /** Collection binding types (Dagger multibindings).
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Enumerates 4 declared provider multibinding destinations; selecting a token performs no corresponding runtime operation.
     * @m3.dataStructures Nested IAIProvides.IntoCollection enum with 4 source-declared constants in existing order.
     */
    enum IntoCollection {
        /** Not a collection binding.
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIProvides.IntoCollection#NONE for provider multibinding destinations: Not a collection binding. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIProvides.IntoCollection enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        NONE,

        /** Contribute to a Set&lt;T>.
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIProvides.IntoCollection#SET for provider multibinding destinations: Contribute to a Set&lt;T>. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIProvides.IntoCollection enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        SET,

        /** Contribute to a Map&lt;K, V>.
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIProvides.IntoCollection#MAP for provider multibinding destinations: Contribute to a Map&lt;K, V>. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIProvides.IntoCollection enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        MAP,

        /** Contribute elements to Set&lt;T>.
         * @m3.pattern spring:dependency-injection
         * @m3.algorithm Declares IAIProvides.IntoCollection#ELEMENTS_INTO_SET for provider multibinding destinations: Contribute elements to Set&lt;T>. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIProvides.IntoCollection enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        ELEMENTS_INTO_SET
    }
}
