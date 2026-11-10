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
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIInterceptorBinding - AnOP meta-annotation for interceptor bindings.
 *
 * <h2>Jakarta CDI Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ CDI: @InterceptorBinding → meta-annotation to define binding types ║
 * ║ AnOP: @AIInterceptorBinding → compile-time binding declaration ║
 * ║ ║
 * ║ CDI says: "This annotation binds interceptors to targets" ║
 * ║ AnOP says: "This is an interceptor binding annotation" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT apply bindings. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * // Define a binding annotation
 * &#64;AIInterceptorBinding
 * &#64;Target({ElementType.TYPE, ElementType.METHOD})
 * &#64;Retention(RetentionPolicy.SOURCE)
 * public @interface Secured {
 * Role[] requiredRoles() default {};
 * }
 *
 * // Interceptor uses the binding
 * &#64;AIInterceptor(bindings = {Secured.class})
 * public class SecurityInterceptor {
 * &#64;AIInterceptor.AroundInvoke
 * public Object checkSecurity(InvocationContext ctx) throws Exception {
 * // Check roles...
 * return ctx.proceed();
 * }
 * }
 *
 * // Apply to target
 * &#64;Secured(requiredRoles = {Role.ADMIN})
 * public class AdminService {
 * public void deleteUser(String id) { ... }
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Interceptor Binding Hints (CDI-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 * @see IAIInterceptor
 * @m3.pattern synexia:iop
 * @m3.algorithm Marks annotation types as interceptor-binding metadata without registering interceptors or intercepting calls.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code ElementType.ANNOTATION_TYPE}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIInterceptorBinding.java}
 * blob {@code de68e9209fa3d199e7e52de19d6be59466271556} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIInterceptorBinding {}
