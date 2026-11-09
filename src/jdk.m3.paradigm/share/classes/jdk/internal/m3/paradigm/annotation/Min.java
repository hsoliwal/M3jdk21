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
 * Min - Validates minimum numeric value.
 *
 * <h2>From Jakarta Validation</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	public class Product {
 *
 * 		&#64;Min(0)
 * 		private int quantity;
 *
 * 		&#64;Min(value = 1, message = "Price must be positive")
 * 		private BigDecimal price;
 * 	}
 * }
 * </pre>
 *
 * <h2>Supported Types</h2>
 * <ul>
 * <li>BigDecimal, BigInteger</li>
 * <li>byte, short, int, long and wrappers</li>
 * <li>CharSequence (parsed as number)</li>
 * </ul>
 *
 * @since ContextOS 1.0 - AnOP Jakarta
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
/**
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Min.java}
 * blob {@code bd29a362bd0456de411bdf752997c4a2cac6c19c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public @interface Min {

    /**
     * Minimum value (inclusive).
     *
     * @return min value
     */
    long value();

    /**
     * Error message.
     *
     * @return message template
     */
    String message() default "must be greater than or equal to {value}";

    /**
     * Validation groups.
     *
     * @return groups
     */
    Class<?>[] groups() default {};
}
