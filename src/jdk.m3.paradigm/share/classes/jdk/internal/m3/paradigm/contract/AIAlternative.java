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
 * AIAlternative - AnOP annotation for alternative implementation hints.
 *
 * <h2>Jakarta CDI Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ CDI: @Alternative → alternative implementation (disabled by default) ║
 * ║ AnOP: @AIAlternative → compile-time alternative declaration ║
 * ║ ║
 * ║ CDI says: "This is an alternative, enable it in beans.xml" ║
 * ║ AnOP says: "This is an alternative implementation" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT select alternatives. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     // Default implementation
 *     &#64;AIStereotype(Role.REPOSITORY)
 *     public class JpaUserRepository implements IUserRepository {
 *     }
 *
 *     // Alternative for testing
 *     &#64;AIAlternative(priority = 100)
 *     &#64;AIStereotype(Role.REPOSITORY)
 *     public class MockUserRepository implements IUserRepository {
 *     }
 *
 *     // Alternative for production with specific config
 *     &#64;AIAlternative(priority = 200, enabledWhen = "profile == 'production'")
 *     public class CachedUserRepository implements IUserRepository {
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Alternative Hints (CDI-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIAlternative.java}
 * blob {@code 7f8c70936352ef3e5352d5d3bf2850290bf477f7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIAlternative {

    /**
     * Priority for alternative selection. Higher priority wins when multiple
     * alternatives are enabled. Similar to CDI's @Priority.
     *
     * @return priority value
     */
    int priority() default 0;

    /**
     * Condition when this alternative is enabled. Expression-like (for
     * documentation, not runtime evaluation).
     *
     * @return condition expression
     */
    String enabledWhen() default "";

    /**
     * Description of why this alternative exists.
     *
     * @return description
     */
    String description() default "";

    /**
     * Alternative use case.
     *
     * @return use case
     */
    UseCase useCase() default UseCase.GENERAL;

    /** Alternative use cases. */
    enum UseCase {
        /** General-purpose alternative. */
        GENERAL,

        /** Testing alternative (mocks, stubs). */
        TESTING,

        /** Development alternative (logging, debugging). */
        DEVELOPMENT,

        /** Production alternative (optimized, secured). */
        PRODUCTION,

        /** Performance alternative (caching, pooling). */
        PERFORMANCE,

        /** Fallback alternative (when primary fails). */
        FALLBACK
    }
}
