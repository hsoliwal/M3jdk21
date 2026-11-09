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
 * NotNull - Validates that value is not null.
 *
 * <h2>From Jakarta Validation</h2>
 *
 * <pre>{@code
 * public class User {
 *
 *     {@code @NotNull}
 *     private String name; // must not be null
 * }
 * }</pre>
 *
 * <h2>With Message</h2>
 *
 * <pre>{@code
 *
 * @NotNull(message = "Name is required")
 * private String name;
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Jakarta
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
/**
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/NotNull.java}
 * blob {@code 58ced953eeb978ef164bbf434341fa5619725416} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public @interface NotNull {

    /**
     * Error message.
     *
     * @return message template
     */
    String message() default "must not be null";

    /**
     * Validation groups.
     *
     * @return groups
     */
    Class<?>[] groups() default {};
}
