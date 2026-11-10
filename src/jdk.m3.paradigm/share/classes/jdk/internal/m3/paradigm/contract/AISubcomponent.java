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
 *     &#64;code
 *     &#64;AISubcomponent(modules = {ActivityModule.class})
 *     &#64;AIInstanceScope(Cardinality.REQUEST)
 *     public interface ActivityComponent {
 *
 *         ActivityPresenter presenter();
 *
 *         &#64;AISubcomponent.Factory
 *         interface Factory {
 *
 *             ActivityComponent create(@AIBindsInstance Activity activity);
 *         }
 *     }
 *
 *     // Parent component exposes the factory
 *     &#64;AIComponent(modules = {AppModule.class})
 *     public interface AppComponent {
 *
 *         ActivityComponent.Factory activityComponentFactory();
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Subcomponent Hints (Dagger-inspired)
 * @see <a href="https://dagger.dev/dev-guide/">Dagger @Subcomponent</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AISubcomponent.java}
 * blob {@code c509bff226ec4dfd024ee2d6ce2d63f59f1568da} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AISubcomponent {

    /**
     * Modules that provide bindings for this subcomponent.
     *
     * @return module classes
     */
    Class<?>[] modules() default {};

    /**
     * Subcomponent description.
     *
     * @return description
     */
    String description() default "";

    // --- NESTED ANNOTATIONS ---

    /** Marks a factory interface for creating subcomponent instances. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface Factory {}

    /** Marks a builder interface for creating subcomponent instances. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface Builder {}
}
