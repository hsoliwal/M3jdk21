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
 * ExceptionHandler - Marks method as exception handler.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;RestController
 * 	public class UserController {
 *
 * 		&#64;ExceptionHandler(UserNotFoundException.class)
 * 		public ResponseEntity&lt;Error> handleNotFound(UserNotFoundException e) {
 * 			return ResponseEntity.notFound().build();
 * 		}
 *
 * 		&#64;ExceptionHandler({ValidationException.class, IllegalArgumentException.class})
 * 		public ResponseEntity&lt;Error> handleBadRequest(Exception e) {
 * 			return ResponseEntity.badRequest().body(new Error(e.getMessage()));
 * 		}
 * 	}
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/ExceptionHandler.java}
 * blob {@code 4386dc91ce56f86babfcf54b57287903fb954f19} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface ExceptionHandler {

    /**
     * Exception types to handle.
     *
     * @return exception classes
     */
    Class<? extends Throwable>[] value() default {};
}
