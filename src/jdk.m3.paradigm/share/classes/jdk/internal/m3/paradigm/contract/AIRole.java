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
 * AIRole - AnOP annotation for role-based access hints.
 *
 * <h2>AnOP Advisory: Role Hints</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Roles describe WHO may interact with this element.                       ║
 * ║  Roles are HINTS for authorization patterns, not enforcement.             ║
 * ║  Actual enforcement is done by security frameworks (Spring Security, etc.)║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;ISpec(value = "User management service")
 * &#64;AIRole(allowed = {Role.ADMIN, Role.USER_MANAGER})
 * public interface IUserManagementService {
 *
 *     void deleteUser(String userId);
 * }
 *
 * &#64;ISpec(value = "Read user profile")
 * @AIRole(allowed = Role.AUTHENTICATED, denied = Role.GUEST)
 * User getProfile(String userId);
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Role Hints
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIRole.java}
 * blob {@code 0af1ee1fd178a0586ca3666f5c44a0ef0aef784b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIRoles.class)
public @interface AIRole {

    /**
     * Roles that are allowed to access this element.
     *
     * @return allowed roles
     */
    Role[] allowed() default {};

    /**
     * Roles that are explicitly denied access.
     *
     * @return denied roles
     */
    Role[] denied() default {};

    /**
     * Require ALL listed roles (AND logic). Default is ANY role (OR logic).
     *
     * @return true for AND logic
     */
    boolean requireAll() default false;

    /**
     * Custom role names (for domain-specific roles).
     *
     * @return custom role names
     */
    String[] custom() default {};

    /** Standard role types. */
    enum Role {
        // --- SYSTEM ROLES ---

        /** System administrator - full access. */
        SYSTEM,

        /** Application administrator. */
        ADMIN,

        /** Operator - runtime operations. */
        OPERATOR,

        /** Auditor - read-only access for compliance. */
        AUDITOR,

        // --- USER ROLES ---

        /** Authenticated user. */
        AUTHENTICATED,

        /** Guest/anonymous user. */
        GUEST,

        /** Premium/paid user. */
        PREMIUM,

        /** User manager - can manage other users. */
        USER_MANAGER,

        // --- FUNCTIONAL ROLES ---

        /** Can read data. */
        READER,

        /** Can write/modify data. */
        WRITER,

        /** Can delete data. */
        DELETER,

        /** Can approve/reject. */
        APPROVER,

        /** Can review. */
        REVIEWER,

        // --- DOMAIN ROLES ---

        /** Domain owner - owns the bounded context. */
        OWNER,

        /** Domain member - participates in domain. */
        MEMBER,

        /** External - outside the domain boundary. */
        EXTERNAL,

        // --- SERVICE ROLES ---

        /** Internal service (service-to-service). */
        SERVICE,

        /** Batch job / scheduled task. */
        BATCH,

        /** Event handler / async processor. */
        EVENT_HANDLER
    }
}
