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
 * SessionAttributes - Declares session attributes for a controller.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>{@code
 * {@code @Controller}
 * &#64;SessionAttributes({"user", "cart"})
 * public class ShoppingController {
 *
 *     &#64;ModelAttribute("cart")
 *     public ShoppingCart createCart() {
 *         return new ShoppingCart();
 *     }
 *
 *     &#64;PostMapping("/checkout")
 *     public String checkout(SessionStatus status) {
 *         // Process checkout
 *         status.setComplete(); // Clear session attributes
 *         return "redirect:/order-complete";
 *     }
 * }
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Spring Web
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/SessionAttributes.java}
 * blob {@code b4faffa3b020ed4cb5be0da819dd26ed067c44b8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface SessionAttributes {

    /**
     * Attribute names to store in session.
     *
     * @return attribute names
     */
    String[] value() default {};

    /**
     * Attribute names (alias).
     *
     * @return attribute names
     */
    String[] names() default {};

    /**
     * Types of attributes to store in session.
     *
     * @return attribute types
     */
    Class<?>[] types() default {};
}
