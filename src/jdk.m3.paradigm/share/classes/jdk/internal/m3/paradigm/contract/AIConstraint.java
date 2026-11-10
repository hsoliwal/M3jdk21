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
 * AIConstraint - AnOP annotation for domain constraint hints.
 *
 * <h2>AnOP Advisory: Constraint Hints</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Constraints describe INVARIANTS that must hold.                          ║
 * ║  Constraints are HINTS for validation code generation.                    ║
 * ║  Actual enforcement is generated, not magical.                            ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;ISpec(value = "Order entity")
 *     &#64;AIConstraint(name = "POSITIVE_TOTAL", expression = "total >= 0", type = Type.INVARIANT)
 *     &#64;AIConstraint(name = "ITEMS_NOT_EMPTY", expression = "items.size() > 0", type = Type.PRECONDITION)
 *     public interface IOrder {
 *
 *         BigDecimal getTotal();
 *
 *         List&lt;OrderItem> getItems();
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Constraint Hints
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIConstraint.java}
 * blob {@code ef58a5b88d95b6bdd26c75828ce9a541eb114c41} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIConstraints.class)
public @interface AIConstraint {

    /**
     * Constraint name/identifier.
     *
     * @return constraint name
     */
    String name() default "";

    /**
     * Constraint expression (pseudo-code or natural language).
     *
     * @return constraint expression
     */
    String expression();

    /**
     * Constraint type (when to check).
     *
     * @return constraint type
     */
    Type type() default Type.INVARIANT;

    /**
     * Error message when constraint is violated.
     *
     * @return error message
     */
    String message() default "";

    /**
     * Severity level.
     *
     * @return severity
     */
    Severity severity() default Severity.ERROR;

    /** Constraint types (Design by Contract). */
    enum Type {
        // --- DESIGN BY CONTRACT ---

        /** Must be true before method execution. */
        PRECONDITION,

        /** Must be true after method execution. */
        POSTCONDITION,

        /** Must always be true (class invariant). */
        INVARIANT,

        // --- VALIDATION ---

        /** Input validation. */
        INPUT,

        /** Output validation. */
        OUTPUT,

        /** State validation. */
        STATE,

        // --- TEMPORAL ---

        /** Must be true during execution. */
        DURING,

        /** Must eventually become true. */
        EVENTUALLY,

        /** Must always remain true. */
        ALWAYS
    }

    /** Constraint violation severity. */
    enum Severity {
        /** Informational only. */
        INFO,

        /** Warning - continue execution. */
        WARNING,

        /** Error - stop execution. */
        ERROR,

        /** Fatal - system cannot recover. */
        FATAL
    }
}
