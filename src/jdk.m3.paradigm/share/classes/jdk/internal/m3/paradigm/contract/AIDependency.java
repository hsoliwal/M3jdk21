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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIDependency - AnOP annotation for dependency injection hints.
 *
 * <h2>AnOP Advisory: Dependency Hints</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Dependencies describe WHAT a component needs.                            ║
 * ║  Dependency hints guide DI wiring code generation.                        ║
 * ║  Actual injection is done by runtime container.                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;ISpec(value = "User service implementation")
 *     &#64;AIDependency(requires = {IUserRepository.class, IPasswordEncoder.class})
 *     &#64;AIDependency(optional = IEmailService.class)
 *     public interface IUserService {
 *
 *         User createUser(UserRequest request);
 *     }
 *
 *     &#64;AIDependency(requires = ILogger.class, injection = Injection.CONSTRUCTOR)
 *     public interface IAuditService {
 *
 *         void logAction(String action);
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Dependency Hints
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIDependency.java}
 * blob {@code 1413a77d4b14367fa5ea7c4267357b44dff2f86c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIDependencies.class)
public @interface AIDependency {

    /**
     * Required dependencies (must be present).
     *
     * @return required types
     */
    Class<?>[] requires() default {};

    /**
     * Optional dependencies (may be null).
     *
     * @return optional types
     */
    Class<?>[] optional() default {};

    /**
     * Dependency names (for qualifier hints).
     *
     * @return dependency names
     */
    String[] named() default {};

    /**
     * Injection style hint.
     *
     * @return injection style
     */
    Injection injection() default Injection.CONSTRUCTOR;

    /**
     * Lazy loading hint.
     *
     * @return true for lazy
     */
    boolean lazy() default false;

    /**
     * Provider/Factory hint (for scoped dependencies).
     *
     * @return true for provider pattern
     */
    boolean provider() default false;

    /** Injection styles. */
    enum Injection {
        // --- STANDARD INJECTION ---

        /** Inject via constructor (preferred). */
        CONSTRUCTOR,

        /** Inject via setter method. */
        SETTER,

        /** Inject directly into field. */
        FIELD,

        // --- LOOKUP PATTERNS ---

        /** Lookup from container/registry. */
        LOOKUP,

        /** Provider/Factory pattern. */
        PROVIDER,

        /** Method injection (parameter). */
        METHOD
    }
}
