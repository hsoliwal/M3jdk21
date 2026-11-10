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

import jdk.internal.m3.collections.PlaceholderLogger;

import jdk.internal.m3.paradigm.context.IContextMemento.Memento;

/**
 * AbstractContextComponent - Base implementation for context components.
 *
 * <p>Core Principle: "Components are derived, not stored live.
 * SQLite stores only mementos, not live objects."
 *
 * <p>Lifecycle:
 * <pre>
 * load(memento)  → extract state from memento
 * [use]          → work with component
 * flush(memento) → write state back to memento
 * [evict]        → component = null, memory freed
 * </pre>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses must implement:
 * <ul>
 *   <li>{@link #doLoad(Memento)} - Extract state from memento</li>
 * </ul>
 *
 * <p>Optional overrides:
 * <ul>
 *   <li>{@link #doFlush(Memento)} - Persist state back to memento</li>
 *   <li>{@link #onLoad(Memento)} - Callback after load</li>
 *   <li>{@link #onFlush(Memento)} - Callback before flush</li>
 * </ul>
 *
 * @since ContextOS 1.1
 */
/**
 * AbstractContextComponent.
 * @author Synexia
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/AbstractContextComponent.java}
 * blob {@code 2e8f7cf48d9c2a2b7bb9ea8fa2b7974effd1e920} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractContextComponent implements IContextComponent {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractContextComponent.class);


    private boolean loaded;

    /** Create a context component. */
    protected AbstractContextComponent() {
        this.loaded = false;
    }

    @Override
    public void load(Memento memento) {
        doLoad(memento);
        this.loaded = true;
        onLoad(memento);
    }

    @Override
    public void flush(Memento memento) {
        if (loaded) {
            onFlush(memento);
            doFlush(memento);
        }
    }

    /**
     * Check if component is loaded.
     *
     * @return true if loaded
     */
    public boolean isLoaded() {
        return loaded;
    }

    /** Mark component as not loaded (after eviction). */
    protected void markUnloaded() {
        this.loaded = false;
    }

    // --- Abstract Template Methods (Must Implement) ---

    /**
     * Extract state from memento.
     *
     * @param memento the context memento
     */
    protected abstract void doLoad(Memento memento);

    // --- Optional Template Methods (Override for Custom Behavior) ---

    /**
     * Persist state back to memento.
     *
     * @param memento the memento to update
     */
    protected void doFlush(Memento memento) {
        // Default: no-op (mementos are often immutable)
    }

    /**
     * Called after component is loaded.
     *
     * @param memento the memento that was loaded from
     */
    protected void onLoad(Memento memento) {
        // Default: no-op
    }

    /**
     * Called before component is flushed.
     *
     * @param memento the memento that will be flushed to
     */
    protected void onFlush(Memento memento) {
        // Default: no-op
    }
}
