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

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Stable actor identity plus named roles used by ContextAuthority.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/context/ContextActor.java}
 * blob {@code 3c8b35f63d3a9ff6eeef67f0aad6b9e252403bda} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record Actor(String id, Set<String> roles) {
    public static final Actor ANONYMOUS = new Actor("anonymous", Set.of("ANONYMOUS"));
    public static final Actor SYSTEM = new Actor("system", Set.of("SYSTEM"));

    public Actor {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id");
        Objects.requireNonNull(roles, "roles");
        TreeSet<String> copy = new TreeSet<>();
        for (String role : roles) {
            if (role == null || role.isBlank()) throw new IllegalArgumentException("role");
            copy.add(role.trim().toUpperCase(java.util.Locale.ROOT));
        }
        roles = Collections.unmodifiableSet(copy);
    }

    public static Actor of(String id, String... roles) {
        return new Actor(id, Set.of(roles));
    }

    public boolean hasRole(String role) {
        return roles.contains(role.toUpperCase(java.util.Locale.ROOT));
    }
}
