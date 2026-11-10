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
 *     &#64;code
 *     &#64;AIModule(includes = {DatabaseModule.class, CacheModule.class})
 *     public class AppModule {
 *
 *         &#64;AIProvides
 *         UserService provideUserService(IUserRepository repo) {
 *             return new UserServiceImpl(repo);
 *         }
 *     }
 *
 *     &#64;AIModule(subcomponents = {UserComponent.class})
 *     public abstract class CoreModule {
 *
 *         &#64;AIBinds
 *         abstract ILogger bindLogger(ConsoleLogger impl);
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Module Hints (Guice/Dagger-inspired)
 * @see <a href="https://dagger.dev/dev-guide/">Dagger @Module</a>
 * @see <a href="https://www.baeldung.com/guice">Guice Guide</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIModule.java}
 * blob {@code 80052131142167ba13c6149b506805a15edf6b30} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIModule {

    /**
     * Other modules to include. Similar to Dagger's @Module(includes = {...}).
     *
     * @return included module classes
      * @author Synexia
     */
    Class<?>[] includes() default {};

    /**
     * Subcomponents this module installs. Similar to
     * Dagger's @Module(subcomponents = {...}).
     *
     * @return subcomponent classes
     */
    Class<?>[] subcomponents() default {};

    /**
     * Module is complete (all bindings satisfied internally). Similar to
     * Dagger's complete validation.
     *
     * @return true if complete
     */
    boolean complete() default true;

    /**
     * Module description.
     *
     * @return description
     */
    String description() default "";

    /**
     * Module category hint.
     *
     * @return category
     */
    Category category() default Category.GENERAL;

    /** Module categories. */
    enum Category {
        /** General-purpose module. */
        GENERAL,

        /** Application-level bindings. */
        APPLICATION,

        /** Activity/request-level bindings (Android/web). */
        ACTIVITY,

        /** Feature-specific module. */
        FEATURE,

        /** Infrastructure/technical concerns. */
        INFRASTRUCTURE,

        /** Testing/mock module. */
        TEST,

        /** Development-only module. */
        DEV
    }
}
