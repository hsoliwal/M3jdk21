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

import jdk.internal.m3.paradigm.context.IContextKernel.IContextNode;
import jdk.internal.m3.paradigm.context.IContextMemento.ContextSummary;
import java.util.List;

/**
 * AbstractContextBloom - Base implementation for selective materialization.
 *
 * <p>Implements the Bloom Rule: "Never replay full history unless explicitly requested."
 *
 * <p>Bloom Strategy:
 * <ul>
 *   <li>Load last snapshot</li>
 *   <li>Load recent N events (bloom window)</li>
 *   <li>Defer older events</li>
 * </ul>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses can override:
 * <ul>
 *   <li>{@link #loadSnapshot(IContextNode)} - Custom snapshot loading</li>
 *   <li>{@link #loadHotEvents(IContextNode, int)} - Custom event loading</li>
 *   <li>{@link #countTotalEvents(IContextNode)} - Custom event counting</li>
 * </ul>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/AbstractContextBloom.java}
 * blob {@code 3119e9005470dad0b067e36eab81ce1f84af9554} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractContextBloom implements IContextBloom {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractContextBloom.class);


    /** Create a context bloom. */
    protected AbstractContextBloom() {
    }

    @Override
    public BloomedContext bloom(IContextNode context, int window) {
        ContextSummary snapshot = loadSnapshot(context);
        List<String> hotEvents = loadHotEvents(context, window);
        int totalEvents = countTotalEvents(context);
        return new BloomedContext(context, snapshot, hotEvents, totalEvents);
    }

    // --- Template Methods (Override for Custom Behavior) ---

    /**
     * Load snapshot from context.
     *
     * @param context the context node
     * @return context summary or null if none
     */
    protected ContextSummary loadSnapshot(IContextNode context) {
        return context.loadSnapshot().orElse(null); // FM-37: API contract requires nullable — Javadoc says "or null if none"
    }

    /**
     * Load hot events from context.
     *
     * @param context the context node
     * @param window number of recent events to load
     * @return list of recent events
     */
    protected List<String> loadHotEvents(IContextNode context, int window) {
        return context.history(window);
    }

    /**
     * Count total events in context.
     *
     * @param context the context node
     * @return total event count
     */
    protected int countTotalEvents(IContextNode context) {
        return context.history().size();
    }
}
