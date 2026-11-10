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
 * AIInject - AnOP annotation for injection point hints.
 *
 * <h2>JSR-330 / Guice / CDI / Dagger Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ JSR-330: @Inject → runtime injection by container ║
 * ║ Guice: @Inject → Guice injector wires dependencies ║
 * ║ CDI: @Inject → CDI container injects beans ║
 * ║ Dagger: @Inject → compile-time generated injection ║
 * ║ AnOP: @AIInject → compile-time injection point declaration ║
 * ║ ║
 * ║ Frameworks say: "I will inject the dependency" ║
 * ║ AnOP says: "This is an injection point" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT inject. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * // Constructor injection (preferred)
 * &#64;AIInject
 * public UserService(IUserRepository repository) {
 *     // LLM knows: repository is injected
 * }
 *
 * // Field injection
 * &#64;AIInject
 * private ILogger logger;
 *
 * // Method injection
 * &#64;AIInject
 * void setConfig(IConfig config) {
 *     // LLM knows: config is injected after construction
 * }
 *
 * // With qualifier
 * &#64;AIInject
 * &#64;AIQualifier("primary")
 * private DataSource dataSource;
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Injection Hints (JSR-330/Guice/CDI/Dagger-inspired)
 * @see <a href="https://github.com/google/guice/wiki/Injections">Guice
 *      Injections</a>
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 * @see <a href="https://dagger.dev/dev-guide/">Dagger 2</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIInject.java}
 * blob {@code 147964eb4159e23beeb369ec37f5ca5c54446e38} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIInject {

    /**
     * Injection is optional (don't fail if no binding). Similar to
     * Guice's @Nullable or CDI's Instance.isUnsatisfied().
     *
     * @return true if optional
     */
    boolean optional() default false;

    /**
     * Lazy injection hint (inject Provider/Supplier instead of instance).
     * Similar to Guice's Provider&lt;T> or CDI's Instance&lt;T>.
     *
     * @return true if lazy
     */
    boolean lazy() default false;

    /**
     * Named qualifier (shorthand for @AIQualifier). Similar to
     * Guice/CDI @Named.
     *
     * @return qualifier name
     */
    String named() default "";

    /**
     * Injection style hint.
     *
     * @return style
     */
    Style style() default Style.CONSTRUCTOR;

    /** Injection styles. */
    enum Style {
        /** Constructor injection (preferred, immutable). */
        CONSTRUCTOR,

        /** Field injection (simpler, but harder to test). */
        FIELD,

        /** Method/setter injection (for optional deps). */
        METHOD,

        /** Parameter injection (in @Provides methods). */
        PARAMETER
    }
}
