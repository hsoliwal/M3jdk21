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
package jdk.internal.m3.paradigm.context;

import jdk.internal.m3.collections.TokenInterner;
import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toUnmodifiableSet;

/**
 * IContextRole - Role-based context access control.
 *
 * <h2>Design Principle</h2>
 * <blockquote>
 * "A context knows what roles can access it.
 *  Roles define what operations are permitted.
 *  Context + Role = Permission."
 * </blockquote>
 *
 * <h2>Role-Based Access</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  ROLE-BASED CONTEXT ACCESS                                              │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  Context                                                                │
 * │    ├── Role: ADMIN                                                      │
 * │    │     ├── Permission: READ                                           │
 * │    │     ├── Permission: WRITE                                          │
 * │    │     └── Permission: DELETE                                         │
 * │    ├── Role: USER                                                       │
 * │    │     ├── Permission: READ                                           │
 * │    │     └── Permission: WRITE                                          │
 * │    └── Role: GUEST                                                      │
 * │          └── Permission: READ                                           │
 * │                                                                         │
 * │  hasPermission(role, permission) → boolean                              │
 * │  getPermissions(role) → Set&lt;Permission>                                 │
 * │  getRoles() → Set&lt;Role>                                                 │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * IContextRole role = IContextRole.of("ADMIN", Permission.READ, Permission.WRITE);
 * if (role.hasPermission(Permission.WRITE)) {
 *     // proceed with write
 * }
 * }</pre>
 *
 * @since ContextOS 1.0 - Layer Package
 */
@ISpec(
    value = "Role-based context access control - defines what operations are permitted",
    constraints = {
        "Role name is interned for identity comparison",
        "Permissions are immutable after creation",
        "Roles compose via union (additive)"
    },
    invariants = {
        "name() returns non-null interned string",
        "permissions() returns non-null immutable set",
        "hasPermission() is O(1) via hash lookup"
    },
    examples = {
        "IContextRole.of(\"ADMIN\", Permission.READ, Permission.WRITE)",
        "role.hasPermission(Permission.DELETE)",
        "role.union(otherRole)"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/layer/IContextRole.java}
 * blob {@code 550f1b3f97206b31022645ef6b33ec20fd385350} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IContextRole {

    // --- CORE OPERATIONS ---

    /** Get the role name (interned for == comparison). */
    String name();

    /** Get all permissions for this role. */
    Set<Permission> permissions();

    /** Check if this role has a specific permission. */
    default boolean hasPermission(Permission permission) {
        return permissions().contains(permission);
    }

    /** Check if this role has all specified permissions. */
    default boolean hasAllPermissions(Permission... permissions) {
        return Stream.of(permissions).allMatch(this::hasPermission);
    }

    /** Check if this role has any of the specified permissions. */
    default boolean hasAnyPermission(Permission... permissions) {
        return Stream.of(permissions).anyMatch(this::hasPermission);
    }

    // --- COMPOSITION ---

    /** Union with another role (additive). */
    default IContextRole union(IContextRole other) {
        return of(
            name() + "+" + other.name(),
            Stream.concat(permissions().stream(), other.permissions().stream())
                  .collect(toUnmodifiableSet())
        );
    }

    /** Intersect with another role. */
    default IContextRole intersect(IContextRole other) {
        return of(
            name() + "&" + other.name(),
            permissions().stream()
                         .filter(other.permissions()::contains)
                         .collect(toUnmodifiableSet())
        );
    }

    // --- PERMISSION ENUM ---

    /** Standard context permissions. */
    enum Permission {
        READ, WRITE, DELETE, EXECUTE, CREATE, ADMIN
    }

    // --- FACTORY METHODS ---

    /** Create a role with specified permissions. */
    static IContextRole of(String name, Permission... permissions) {
        return of(name, Set.of(permissions));
    }

    /** Create a role with permission set. */
    static IContextRole of(String name, Set<Permission> permissions) {
        return new SimpleRole(
            TokenInterner.getInstance().intern(name),
            Set.copyOf(permissions)
        );
    }

    /** Create a role with all permissions. */
    static IContextRole all(String name) {
        return of(name, EnumSet.allOf(Permission.class));
    }

    /** Create an empty role (no permissions). */
    static IContextRole none(String name) {
        return of(name, Set.of());
    }

    // --- SIMPLE IMPLEMENTATION ---

    /** Simple role record implementation. */
    record SimpleRole(String name, Set<Permission> permissions) implements IContextRole {
        @Override
        public boolean equals(Object o) {
            return o instanceof IContextRole that && name == that.name();
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(name);
        }

        @Override
        public String toString() {
            return "Role[" + name + ", " + permissions + "]";
        }
    }

    // --- STANDARD ROLES ---

    /** Admin role - all permissions. */
    IContextRole ADMIN = all("ADMIN");

    /** User role - read, write, execute. */
    IContextRole USER = of("USER", Permission.READ, Permission.WRITE, Permission.EXECUTE);

    /** Guest role - read only. */
    IContextRole GUEST = of("GUEST", Permission.READ);

    /** System role - all permissions. */
    IContextRole SYSTEM = all("SYSTEM");
}
