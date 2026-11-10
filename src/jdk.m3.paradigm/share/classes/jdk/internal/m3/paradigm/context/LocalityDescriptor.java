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

import java.util.Map;

/**
 * Stable declaration of one runtime locality.
 *
 * <p>A locality is a source of runtime state. It is not proof of a historical
 * referent or a hidden mental state.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/LocalityDescriptor.java}
 * blob {@code c2097900d9a06ab50cb4c6f80251d8a764e161fa} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record LocalityDescriptor(String id, Kind kind, String modeledReferentId) {
    /** Runtime locality kind. */
    public enum Kind {
        PARTICIPANT,
        SIMULATION,
        SYSTEM
    }

    public LocalityDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind");
        }
        id = id.trim();
        modeledReferentId = modeledReferentId == null ? "" : modeledReferentId.trim();
    }

    public static LocalityDescriptor participant(String id) {
        return new LocalityDescriptor(id, Kind.PARTICIPANT, "");
    }

    public static LocalityDescriptor simulation(String id, String modeledReferentId) {
        return new LocalityDescriptor(id, Kind.SIMULATION, modeledReferentId);
    }

    public static LocalityDescriptor system(String id) {
        return new LocalityDescriptor(id, Kind.SYSTEM, "");
    }

    public boolean simulated() {
        return kind == Kind.SIMULATION;
    }

    /**
     * A declared locality never acquires historical-person authority merely by
     * existing in the runtime.
     *
     * @return always false
     */
    public boolean historicalReferentAuthority() {
        return false;
    }

    Map<String, Object> canonicalForm() {
        return Map.of(
                "id", id,
                "kind", kind.name(),
                "modeledReferentId", modeledReferentId,
                "historicalReferentAuthority", false);
    }
}
