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

import java.util.Collections;
import java.lang.annotation.*;

/**
 * AIImmutableConstant - IOP annotation marking an immutable constant.
 *
 * <h2>IOP Authority: Immutable Constant Exception</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  By default, constants with collection types (List, Set, Map) are REJECTED║
 * ║  because collections are mutable.                                         ║
 * ║                                                                           ║
 * ║  This annotation EXPLICITLY marks a constant as immutable, allowing:      ║
 * ║    • List.of(...)                                                         ║
 * ║    • Set.of(...)                                                          ║
 * ║    • Map.of(...)                                                          ║
 * ║    • Collections.unmodifiable*(...)                                       ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     public final class AllowedRoles {
 *
 *         &#64;AIImmutableConstant
 *         public static final List&lt;String> ADMIN_ROLES = List.of("ADMIN",
 *                 "SUPER_ADMIN");
 *
 *         &#64;AIImmutableConstant
 *         public static final Set&lt;Integer> VALID_CODES = Set.of(200, 201, 204);
 *
 *         &#64;AIImmutableConstant
 *         public static final Map&lt;String, String> ERROR_MESSAGES = Map
 *                 .of("E001", "Invalid input", "E002", "Timeout");
 *
 *         private AllowedRoles() {
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>Checkstyle Integration</h2>
 * <p>
 * Custom AST rule checks:
 * <ul>
 * <li>Field must be static final</li>
 * <li>Type must be List, Set, or Map</li>
 * <li>Initializer must use immutable factory (List.of, Set.of, Map.of)</li>
 * <li>Without this annotation, collection constants are REJECTED</li>
 * </ul>
 *
 * @since IOP 1.0 - Immutable Constants
 * @see AIConstant
 * @see AICheckstyleRule
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIImmutableConstant.java}
 * blob {@code 002033962e51d62a4b493005248c80b95ebc7755} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIImmutableConstant {

    /**
     * Reason for using collection type (documentation).
     *
     * @return reason
     */
    String reason() default "";

    /**
     * Expected factory method used.
     *
     * @return factory type
     */
    Factory factory() default Factory.INFERRED;

    /** Immutable collection factory methods. */
    enum Factory {
        /** Infer from code. */
        INFERRED,

        /** List.of(...). */
        LIST_OF,

        /** Set.of(...). */
        SET_OF,

        /** Map.of(...) or Map.ofEntries(...). */
        MAP_OF,

        /** Collections.unmodifiableList(...). */
        UNMODIFIABLE_LIST,

        /** Collections.unmodifiableSet(...). */
        UNMODIFIABLE_SET,

        /** Collections.unmodifiableMap(...). */
        UNMODIFIABLE_MAP,

        /** Guava ImmutableList.of(...). */
        GUAVA_IMMUTABLE_LIST,

        /** Guava ImmutableSet.of(...). */
        GUAVA_IMMUTABLE_SET,

        /** Guava ImmutableMap.of(...). */
        GUAVA_IMMUTABLE_MAP
    }
}
