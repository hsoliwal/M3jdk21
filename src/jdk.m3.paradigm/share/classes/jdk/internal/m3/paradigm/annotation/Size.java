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
 * Size - Validates size/length constraints.
 *
 * <h2>From Jakarta Validation</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	public class User {
 *
 * 		&#64;Size(min = 2, max = 100)
 * 		private String name;
 *
 * 		&#64;Size(max = 10)
 * 		private List&lt;Role> roles;
 * 	}
 * }
 * </pre>
 *
 * <h2>Supported Types</h2>
 * <ul>
 * <li>CharSequence (length)</li>
 * <li>Collection (size)</li>
 * <li>Map (size)</li>
 * <li>Array (length)</li>
 * </ul>
 *
 * @since ContextOS 1.0 - AnOP Jakarta
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
/**
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Size.java}
 * blob {@code 28a54428e9c018f46c815043645c1153f74eb432} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public @interface Size {

    /**
     * Minimum size (inclusive).
     *
     * @return min size
     */
    int min() default 0;

    /**
     * Maximum size (inclusive).
     *
     * @return max size
     */
    int max() default Integer.MAX_VALUE;

    /**
     * Error message.
     *
     * @return message template
     */
    String message() default "size must be between {min} and {max}";

    /**
     * Validation groups.
     *
     * @return groups
      * @author Synexia
     */
    Class<?>[] groups() default {};
}
