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
 *     &#64;code
 *     &#64;AIModule
 *     public class AppModule {
 *
 *         &#64;AIProvides
 *         &#64;AIInstanceScope(Cardinality.SINGLETON)
 *         DataSource provideDataSource(Config config) {
 *             // LLM knows: this creates DataSource, singleton scope
 *             return new HikariDataSource(config.getDbUrl());
 *         }
 *
 *         &#64;AIProvides
 *         &#64;AIQualifier("cached")
 *         IUserRepository provideCachedRepo(DataSource ds) {
 *             // LLM knows: qualified provider, depends on DataSource
 *             return new CachedUserRepository(ds);
 *         }
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Provider Hints (Guice/Dagger/CDI-inspired)
 * @see <a href=
 *      "https://www.tutorialspoint.com/guice/guice_provides_annotation.htm">Guice @Provides</a>
 * @see <a href="https://dagger.dev/dev-guide/">Dagger @Provides</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIProvides.java}
 * blob {@code c20df4c097dcdd28d39bbe68158a48053dd27676} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIProvides {

    /**
     * Provider type hint.
     *
     * @return type
     */
    Type type() default Type.INSTANCE;

    /**
     * Nullable return hint (can return null).
     *
     * @return true if nullable
     */
    boolean nullable() default false;

    /**
     * Static provider (no instance needed). Similar to Dagger's
     * static @Provides.
     *
     * @return true if static
     */
    boolean isStatic() default false;

    /**
     * Into collection hint (Dagger @IntoSet, @IntoMap).
     *
     * @return collection type
     */
    IntoCollection into() default IntoCollection.NONE;

    /**
     * Map key (for @IntoMap).
     *
     * @return map key
     */
    String mapKey() default "";

    /** Provider types. */
    enum Type {
        /** Provides a single instance. */
        INSTANCE,

        /** Provides a Provider&lt;T> (lazy). */
        PROVIDER,

        /** Provides a Lazy&lt;T> (cached lazy). */
        LAZY,

        /** Provides Optional&lt;T>. */
        OPTIONAL
    }

    /** Collection binding types (Dagger multibindings). */
    enum IntoCollection {
        /** Not a collection binding. */
        NONE,

        /** Contribute to a Set&lt;T>. */
        SET,

        /** Contribute to a Map&lt;K, V>. */
        MAP,

        /** Contribute elements to Set&lt;T>. */
        ELEMENTS_INTO_SET
    }
}
