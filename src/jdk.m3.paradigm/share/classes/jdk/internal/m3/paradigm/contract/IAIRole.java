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
 * public interface IUserManagementService<Self extends IUserManagementService<Self>> extends IContext<Self> {
 *
 * 	void deleteUser(String userId);
 * }
 *
 * &#64;ISpec(value = "Read user profile")
 * @AIRole(allowed = Role.AUTHENTICATED, denied = Role.GUEST)
 * User getProfile(String userId);
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Role Hints
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares allowed, denied and custom role hints with all-versus-any matching preference; it authenticates no principal, grants no authority and performs no access check.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIRole.java}
 * blob {@code 634e8164c1f7214a6829efd131d8f6a29763093a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAIRoles.class)
public @interface IAIRole {

    /**
     * Roles that are allowed to access this element.
     *
     * @return allowed roles
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIRole#allowed intent: Roles that are allowed to access this element. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Role[]}; default expression {@code {}}.
     */
    Role[] allowed() default {};

    /**
     * Roles that are explicitly denied access.
     *
     * @return denied roles
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIRole#denied intent: Roles that are explicitly denied access. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Role[]}; default expression {@code {}}.
     */
    Role[] denied() default {};

    /**
     * Require ALL listed roles (AND logic). Default is ANY role (OR logic).
     *
     * @return true for AND logic
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIRole#requireAll intent: Require ALL listed roles (AND logic). Default is ANY role (OR logic). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean requireAll() default false;

    /**
     * Custom role names (for domain-specific roles).
     *
     * @return custom role names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIRole#custom intent: Custom role names (for domain-specific roles). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] custom() default {};

    /** Standard role types. */
    /**
     * Role.
     * @author Synexia
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAIRole.Role choices described as Role. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 19 constants in the existing source order.
     */
    enum Role {
        // --- SYSTEM ROLES ---

        /** System administrator - full access.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#SYSTEM policy choice: System administrator - full access. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        SYSTEM,

        /** Application administrator.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#ADMIN policy choice: Application administrator. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        ADMIN,

        /** Operator - runtime operations.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#OPERATOR policy choice: Operator - runtime operations. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        OPERATOR,

        /** Auditor - read-only access for compliance.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#AUDITOR policy choice: Auditor - read-only access for compliance. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        AUDITOR,

        // --- USER ROLES ---

        /** Authenticated user.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#AUTHENTICATED policy choice: Authenticated user. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        AUTHENTICATED,

        /** Guest/anonymous user.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#GUEST policy choice: Guest/anonymous user. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        GUEST,

        /** Premium/paid user.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#PREMIUM policy choice: Premium/paid user. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        PREMIUM,

        /** User manager - can manage other users.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#USER_MANAGER policy choice: User manager - can manage other users. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        USER_MANAGER,

        // --- FUNCTIONAL ROLES ---

        /** Can read data.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#READER policy choice: Can read data. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        READER,

        /** Can write/modify data.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#WRITER policy choice: Can write/modify data. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        WRITER,

        /** Can delete data.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#DELETER policy choice: Can delete data. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        DELETER,

        /** Can approve/reject.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#APPROVER policy choice: Can approve/reject. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        APPROVER,

        /** Can review.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#REVIEWER policy choice: Can review. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        REVIEWER,

        // --- DOMAIN ROLES ---

        /** Domain owner - owns the bounded context.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#OWNER policy choice: Domain owner - owns the bounded context. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        OWNER,

        /** Domain member - participates in domain.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#MEMBER policy choice: Domain member - participates in domain. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        MEMBER,

        /** External - outside the domain boundary.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#EXTERNAL policy choice: External - outside the domain boundary. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        EXTERNAL,

        // --- SERVICE ROLES ---

        /** Internal service (service-to-service).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#SERVICE policy choice: Internal service (service-to-service). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        SERVICE,

        /** Batch job / scheduled task.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#BATCH policy choice: Batch job / scheduled task. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        BATCH,

        /** Event handler / async processor.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIRole.Role#EVENT_HANDLER policy choice: Event handler / async processor. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIRole.Role; its declared order and identity are preserved.
         */
        EVENT_HANDLER
    }
}
