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
 *     &#64;code
 *     &#64;AIModule
 *     public abstract class RepositoryModule {
 *
 *         &#64;AIBinds
 *         abstract IUserRepository bindUserRepo(JpaUserRepository impl);
 *         // LLM knows: IUserRepository is implemented by JpaUserRepository
 *
 *         &#64;AIBinds
 *         &#64;AIQualifier("cached")
 *         abstract IUserRepository bindCachedRepo(CachedUserRepository impl);
 *         // LLM knows: qualified binding
 *     }
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
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIBinds.java}
 * blob {@code c3a39888234af4af4410ce033c2fe29f32b4040c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIBinds {

    /**
     * Into collection hint (multibinding).
     *
     * @return collection type
     */
    IntoCollection into() default IntoCollection.NONE;

    /**
     * Map key for multibinding.
     *
     * @return map key
     */
    String mapKey() default "";

    /** Collection binding types. */
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
