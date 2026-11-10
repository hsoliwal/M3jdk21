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

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Context Memento - The Memory Backbone.
 *
 * <h2>Core Principle</h2>
 * <blockquote>
 * "The backbone is just a time-ordered list of snapshots + deltas."
 * </blockquote>
 *
 * <h2>What This Stores</h2>
 * <pre>
 * Context = Origin + History
 * History = [State₀, Δ₁, Δ₂, Δ₃, …]
 * </pre>
 *
 * @since 1.1.0 - ContextOS Integration
 */
@ISpec(
    value = "Context Memento - The Memory Backbone for ContextOS persistence",
    constraints = {
        "Backbone is time-ordered list of snapshots + deltas",
        "Context = Origin + History = [State₀, Δ₁, Δ₂, ...]",
        "Parent is UUID pointer (lazy) to prevent circular loading",
        "One UUID = one .db file (SQLite)"
    },
    invariants = {
        "ContextId.id() returns non-null UUID",
        "ContextState.apply(delta) returns new immutable state",
        "Memento.contextId() returns non-null identity"
    },
    examples = {
        "ContextId.root(\"chat\")",
        "ContextId.child(parentId, \"tool\")",
        "state.apply(delta)"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/IContextMemento.java}
 * blob {@code 9840c6791a11f52ba9c155d40c401ea3d776b2ae} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IContextMemento {

    // --- CONTEXT IDENTITY ---

    /**
     * Context identity - UUID + parent + component.
     *
     * <p>Parent is a UUID pointer (lazy) - prevents circular loading.</p>
     *
     * @param id the context UUID (becomes filename: id.db)
     * @param parent parent context UUID (null for root) - LAZY POINTER
     * @param component subsystem (chat, tool, agent, planner)
     */
    record ContextId(
            UUID id,
            UUID parent,
            String component
    ) {
        /** Create root context ID. */
        public static ContextId root(String component) {
            return new ContextId(UUID.randomUUID(), null, component);
        }

        /** Create child context ID. */
        public static ContextId child(ContextId parent, String component) {
            return new ContextId(UUID.randomUUID(), parent.id(), component);
        }
    }

    // --- MEMENTO STATE ---

    /** Context state - immutable, functional. */
    record ContextState(
            String intent,
            String emotion,
            String direction,
            int focus,
            Instant updatedAt
    ) {
        /** Apply delta to produce new state. */
        public ContextState apply(ContextDelta delta) {
            return new ContextState(
                    delta.intent() != null ? delta.intent() : intent,
                    delta.emotion() != null ? delta.emotion() : emotion,
                    delta.direction() != null ? delta.direction() : direction,
                    focus + delta.focusShift(),
                    Instant.now()
            );
        }

        /** Create empty initial state. */
        public static ContextState initial() {
            return new ContextState("UNKNOWN", "NEUTRAL", "FORWARD", 0, Instant.now());
        }
    }

    /** Context delta - change, not full copy. */
    record ContextDelta(
            String intent,
            String emotion,
            String direction,
            int focusShift
    ) {}

    // --- THE MEMENTO ---

    /** The memento - captures context state for persistence/restore. */
    record Memento(
            ContextId contextId,
            ContextState state,
            Map<String, Object> compressedState
    ) {
        public static Memento of(ContextId contextId, ContextState state) {
            return new Memento(contextId, state, new java.util.HashMap<>());
        }
    }

    // --- CONTEXT SUMMARY (COLLAPSE ARTIFACT) ---

    /**
     * Context summary - produced when collapsing many events.
     *
     * <p>Collapse rule: Replace many events with a semantic summary.</p>
     */
    record ContextSummary(
            String dominantIntent,
            String dominantEmotion,
            String direction,
            Instant lastActive
    ) {}
}
