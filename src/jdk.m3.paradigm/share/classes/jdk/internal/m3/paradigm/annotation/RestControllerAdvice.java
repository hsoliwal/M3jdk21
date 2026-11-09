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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RestControllerAdvice - Combines @ControllerAdvice and @ResponseBody.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;RestControllerAdvice
 * 	public class GlobalExceptionHandler {
 *
 * 		&#64;ExceptionHandler(Exception.class)
 * 		public Error handleAll(Exception e) {
 * 			return new Error(e.getMessage()); // returns JSON
 * 		}
 * 	}
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/RestControllerAdvice.java}
 * blob {@code 9c453cd3c59586ac76c3d84a66d0e2ab6bbdb54a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@ControllerAdvice
@ResponseBody
public @interface RestControllerAdvice {

    /**
     * Advice name.
     *
     * @return advice name
     */
    String value() default "";

    /**
     * Base packages to apply advice to.
     *
     * @return base packages
     */
    String[] basePackages() default {};

    /**
     * Controller classes to apply advice to.
     *
     * @return controller classes
      * @author Synexia
     */
    Class<?>[] assignableTypes() default {};

    /**
     * Annotations to apply advice to.
     *
     * @return annotations
     */
    Class<? extends Annotation>[] annotations() default {};
}
