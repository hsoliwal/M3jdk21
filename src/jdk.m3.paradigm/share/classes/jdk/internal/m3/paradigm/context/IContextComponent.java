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
import jdk.internal.m3.paradigm.context.IContextMemento.Memento;

/**
 * Context Component - A View Over the Memento.
 *
 * <h2>Core Principle</h2>
 * <blockquote>
 * "Components are derived, not stored live.
 *  SQLite stores only mementos, not live objects."
 * </blockquote>
 *
 * <h2>Component Types</h2>
 * <ul>
 *   <li>IntentComponent - dominant intent</li>
 *   <li>EmotionComponent - emotional state</li>
 *   <li>VectorComponent - embedding cache</li>
 *   <li>ToolComponent - tool execution state</li>
 *   <li>PlannerComponent - planner state</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <pre>
 * load(memento)  → extract state from memento
 * [use]          → work with component
 * flush(memento) → write state back to memento
 * [evict]        → component = null, memory freed
 * </pre>
 *
 * @since 1.1.0 - ContextOS Integration
 */
@ISpec(
    value = "Derived view over memento - components are derived, not stored live",
    constraints = {
        "SQLite stores only mementos, not live objects",
        "load() extracts state from memento",
        "flush() writes state back before eviction"
    },
    invariants = {
        "load() populates component from memento",
        "flush() preserves component state to memento",
        "Lifecycle: load → use → flush → evict"
    },
    examples = {
        "component.load(memento)",
        "component.flush(memento)",
        "new IntentComponent()"
    },
    model = Model.ModelType.HAIKU,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/IContextComponent.java}
 * blob {@code 425cf3b37692d68c225207cce5dd74d7bd05b5f6} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IContextComponent {

    /**
     * Load component state from memento.
     *
     * @param memento the context memento
     */
    void load(Memento memento);

    /**
     * Flush component state back to memento.
     *
     * @param memento the memento to update
     */
    void flush(Memento memento);

    // --- EXAMPLE COMPONENTS ---

    /** Intent component - extracts and holds intent state. */
    final class IntentComponent implements IContextComponent {
        private String intent;

        @Override
        public void load(Memento m) {
            intent = m.state().intent();
        }

        @Override
        public void flush(Memento m) {
            // Memento is immutable, state already captured
        }

        public String intent() { return intent; }
    }

    /** Emotion component - extracts and holds emotion state. */
    final class EmotionComponent implements IContextComponent {
        private String emotion;

        @Override
        public void load(Memento m) {
            emotion = m.state().emotion();
        }

        @Override
        public void flush(Memento m) {}

        public String emotion() { return emotion; }
    }

    /** Direction component - extracts and holds direction state. */
    final class DirectionComponent implements IContextComponent {
        private String direction;

        @Override
        public void load(Memento m) {
            direction = m.state().direction();
        }

        @Override
        public void flush(Memento m) {}

        public String direction() { return direction; }
    }
}
