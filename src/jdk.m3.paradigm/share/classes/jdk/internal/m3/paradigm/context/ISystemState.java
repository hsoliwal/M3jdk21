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

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.time.Instant;
import java.util.UUID;

/**
 * ISystemState - Mathematical bedrock for state representation.
 *
 * <p>Scientific justification from Grand Java Patterns Proof:
 * <pre>
 *   State → Transformation → State
 *
 *   This is pure mathematics:
 *   - Set: all possible states
 *   - Element: one snapshot
 *   - Invariant: identity + time
 * </pre>
 *
 * <p>Isomorphic to:
 * <ul>
 *   <li>Physics: state evolution operators</li>
 *   <li>Math: endofunctions over a set</li>
 *   <li>Category theory: objects in a category</li>
 * </ul>
 *
 * @param <Self> Self-type for fluent operations
 * @since Grand Java Patterns Proof - Section 1
 */
@ISpec(
    value = "Mathematical bedrock for state - pure set theory (identity + time invariant)",
    constraints = {
        "States are immutable snapshots",
        "Identity (UUID) is invariant across transformations",
        "Time is explicit, not hidden"
    },
    invariants = {
        "id() returns non-null UUID",
        "timestamp() returns non-null Instant",
        "withTimestamp() returns new instance, not mutation"
    },
    examples = {
        "state.id()",
        "state.withTimestamp(Instant.now())",
        "state.self()"
    },
    model = Model.ModelType.HAIKU,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ISystemState.java}
 * blob {@code 1647360bea5d88165306c70eababb3a736e929dc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ISystemState<Self extends ISystemState<Self>> {

    /**
     * Unique identifier for this state snapshot.
     * Identity is invariant across transformations.
     *
     * @return unique identifier
     */
    UUID id();

    /**
     * Timestamp when this state was captured.
     * Time is explicit, not hidden (FOP principle).
     *
     * @return state timestamp
     */
    Instant timestamp();

    /**
     * Cast to self-type for fluent chaining.
     *
     * @return this as Self type
     */
    @SuppressWarnings("unchecked")
    default Self self() {
        return (Self) this;
    }

    /**
     * Create a new state with updated timestamp.
     * States are immutable - this returns a new instance.
     *
     * @param newTimestamp the new timestamp
     * @return new state with updated timestamp
     */
    Self withTimestamp(Instant newTimestamp);
}
