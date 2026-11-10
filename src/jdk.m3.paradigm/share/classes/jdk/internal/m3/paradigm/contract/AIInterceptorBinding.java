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
 * Role[] requiredRoles() default { };
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
 * @see AIInterceptor
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIInterceptorBinding.java}
 * blob {@code 8df684d0da602df20677532c59e8286c5091f54a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIInterceptorBinding {}
