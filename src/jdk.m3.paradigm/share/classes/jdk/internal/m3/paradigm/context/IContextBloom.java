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
import jdk.internal.m3.paradigm.context.IContextKernel.IContextNode;
import jdk.internal.m3.paradigm.context.IContextMemento.ContextSummary;

import java.util.List;

/**
 * Context Bloom - Selective Materialization.
 *
 * <h2>Bloom Rule</h2>
 * <blockquote>
 * "Never replay full history unless explicitly requested."
 * </blockquote>
 *
 * <h2>Bloom Strategy</h2>
 * <ul>
 *   <li>Load last snapshot</li>
 *   <li>Load recent N events (bloom window)</li>
 *   <li>Defer older events</li>
 * </ul>
 *
 * <h2>Analogy</h2>
 * <ul>
 *   <li>Working-set principle (OS)</li>
 *   <li>CPU cache logic</li>
 *   <li>Human short-term memory</li>
 * </ul>
 *
 * @since 1.1.0 - ContextOS Integration
 */
@ISpec(
    value = "Selective materialization - working-set principle (load snapshot + recent N events)",
    constraints = {
        "Never replay full history unless requested",
        "Bloom window limits memory usage",
        "Deferred events remain in storage"
    },
    invariants = {
        "bloom() returns non-null BloomedContext",
        "BloomedContext.hotEvents size <= window",
        "shouldCollapse() triggers when totalEvents > threshold"
    },
    examples = {
        "IContextBloom.DEFAULT.bloom(ctx, 100)",
        "bloomed.shouldCollapse(1000)"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
@FunctionalInterface
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/IContextBloom.java}
 * blob {@code 15d476df30c913ef5f0c68140ae91cbcf43ea5ef} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IContextBloom {

    /** Default bloom window size. */
    int DEFAULT_WINDOW = 100;

    /**
     * Bloom a context - materialize recent window.
     *
     * @param context the context node
     * @param window number of recent events to load
     * @return bloomed context with hot events
     */
    BloomedContext bloom(IContextNode context, int window);

    /** Bloomed context - selective materialization result. */
    record BloomedContext(
            IContextNode context,
            ContextSummary snapshot,
            List<String> hotEvents,
            int totalEvents
    ) {
        /** Check if should collapse. */
        public boolean shouldCollapse(int threshold) {
            return totalEvents > threshold;
        }
    }

    /** Default bloom implementation. */
    IContextBloom DEFAULT = (context, window) -> {
        ContextSummary snapshot = context.loadSnapshot().orElse(null); // FM-37: API contract requires nullable — BloomedContext accepts nullable snapshot
        List<String> hotEvents = context.history(window);
        int totalEvents = context.history().size();
        return new BloomedContext(context, snapshot, hotEvents, totalEvents);
    };
}
