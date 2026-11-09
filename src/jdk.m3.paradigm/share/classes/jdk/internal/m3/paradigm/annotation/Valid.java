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

package jdk.internal.m3.paradigm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valid - Triggers cascading validation.
 *
 * <h2>From Jakarta Validation</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     public class Order {
 *
 *         &#64;Valid
 *         &#64;NotNull
 *         private Customer customer; // validates Customer fields too
 *
 *         {@code @Valid}
 *         private List&lt;OrderItem> items; // validates each item
 *     }
 * }
 * </pre>
 *
 * <h2>Cascading</h2>
 * <p>
 * When the Order is validated, the Customer and each OrderItem are also
 * validated recursively.
 *
 * @since ContextOS 1.0 - AnOP Jakarta
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Valid.java}
 * blob {@code d2a4b1673d6a7f5248af63de3c8a22d139d9f6d8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface Valid {
    // Marker annotation - no attributes
}
