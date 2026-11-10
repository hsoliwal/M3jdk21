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
 *     &#64;code
 *     &#64;AIComponent(modules = {AppModule.class, NetworkModule.class})
 *     &#64;AIInstanceScope(Cardinality.SINGLETON)
 *     public interface AppComponent {
 *
 *         // Provision methods - expose dependencies
 *         UserService userService();
 *
 *         ILogger logger();
 *
 *         // Member injection methods
 *         void inject(MainActivity activity);
 *
 *         // Subcomponent factory
 *         UserComponent.Factory userComponentFactory();
 *     }
 *
 *     &#64;AIComponent(dependencies = {AppComponent.class}, modules = {
 *             UserModule.class})
 *     &#64;AIInstanceScope(Cardinality.REQUEST)
 *     public interface UserComponent {
 *
 *         UserPresenter presenter();
 *
 *         &#64;AIComponent.Factory
 *         interface Factory {
 *
 *             UserComponent create(@AIBindsInstance User user);
 *         }
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Component Hints (Dagger-inspired)
 * @see <a href="https://dagger.dev/dev-guide/">Dagger @Component</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIComponent.java}
 * blob {@code fca230edc3600a94fda301966ec56bf77d83ba1f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIComponent {

    /**
     * Modules that provide bindings for this component.
     *
     * @return module classes
     */
    Class<?>[] modules() default {};

    /**
     * Component dependencies (other components this depends on).
     *
     * @return dependency component classes
     */
    Class<?>[] dependencies() default {};

    /**
     * Component description.
     *
     * @return description
     */
    String description() default "";

    // --- NESTED ANNOTATIONS ---

    /**
     * Marks a factory interface for creating component instances. Similar to
     * Dagger's @Component.Factory.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface Factory {}

    /**
     * Marks a builder interface for creating component instances. Similar to
     * Dagger's @Component.Builder.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface Builder {}
}
