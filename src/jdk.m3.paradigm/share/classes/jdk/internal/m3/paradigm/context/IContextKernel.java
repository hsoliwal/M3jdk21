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
import jdk.internal.m3.paradigm.context.IContextMemento.ContextId;
import jdk.internal.m3.paradigm.context.IContextMemento.ContextSummary;


import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Context Kernel - Authority, Not Storage.
 *
 * <h2>Key Invariants</h2>
 * <ul>
 *   <li>Kernel does routing only</li>
 *   <li>Context DBs stay independent (one UUID = one .db file)</li>
 *   <li>Lazy load by UUID</li>
 *   <li><b>NO LOOPS</b> - parent chain must terminate at root</li>
 * </ul>
 *
 * <h2>Loop Prevention</h2>
 * <pre>
 * A → B → C → A  ← FORBIDDEN (cycle detected)
 * A → B → C      ← OK (terminates at root)
 *
 * wouldCreateLoop(childId, parentId):
 *   Walk up parent chain from parentId
 *   If we hit childId → loop would be created → REJECT
 * </pre>
 *
 * @since 1.1.0 - ContextOS Integration
 */
@ISpec(
    value = "Context Kernel - Authority and Routing (NOT Storage)",
    constraints = {
        "Kernel does routing only, not storage",
        "Context DBs stay independent (one UUID = one .db file)",
        "Lazy load by UUID",
        "NO LOOPS - parent chain MUST terminate at root",
        "wouldCreateLoop() MUST prevent cycles"
    },
    invariants = {
        "create() returns non-null IContextNode",
        "fork() returns non-null child node",
        "wouldCreateLoop() prevents all cycles in parent chain"
    },
    examples = {
        "kernel.create(\"chat\")",
        "kernel.fork(parentId, \"tool\")",
        "kernel.wouldCreateLoop(childId, parentId)"
    },
    model = Model.ModelType.OPUS,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/IContextKernel.java}
 * blob {@code f3fc9b238a39def01b94e24230ea6652996d4e28} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IContextKernel {

    // --- CONTEXT LIFECYCLE ---

    /**
     * Create a new root context.
     *
     * @param component the component name (chat, tool, agent, planner)
     * @return new context node
     */
    IContextNode create(String component);

    /**
     * Create a child context (fork).
     *
     * @param parentId parent context UUID
     * @param component the component name
     * @return new child context node
     */
    IContextNode fork(UUID parentId, String component);

    /**
     * Load existing context by ID.
     *
     * @param contextId the context UUID
     * @return context node if exists
     */
    Optional<IContextNode> load(UUID contextId);

    /** Get or create context. */
    IContextNode getOrCreate(UUID contextId, String component);

    // --- LOOP PREVENTION ---

    /**
     * Link a context to a new parent (re-parent).
     *
     * <p><b>Loop Prevention:</b> This method checks that setting the new parent
     * would not create a cycle in the context tree.</p>
     *
     * @param childId the context to re-parent
     * @param newParentId the new parent (null to make root)
     * @throws IllegalArgumentException if this would create a loop
     */
    void link(UUID childId, UUID newParentId);

    /**
     * Check if linking would create a loop.
     *
     * <p>Algorithm: Walk up parent chain from parentId.
     * If we encounter childId, that means childId is an ancestor of parentId,
     * so making parentId the parent of childId would create a cycle.</p>
     *
     * @param childId the context that would become a child
     * @param parentId the proposed parent
     * @return true if this would create a cycle
     */
    boolean wouldCreateLoop(UUID childId, UUID parentId);

    // --- CONTEXT TREE OPERATIONS ---

    /** Get all children of a context (lazy - just UUIDs until loaded). */
    List<IContextNode> children(UUID parentId);

    /** Get parent of a context. */
    Optional<IContextNode> parent(UUID contextId);

    // --- CONTEXT GC ---

    /** Delete a context and its DB file. */
    boolean delete(UUID contextId);

    /** Delete context tree (recursive). */
    int deleteTree(UUID contextId);

    // --- LIFECYCLE ---

    void close();
    Path root();

    // --- CONTEXT NODE ---

    /** Context Node - The actual context object. */
    interface IContextNode {
        ContextId id();
        ContextMeta meta();
        void accept(String eventType, String payload);
        List<String> history();
        List<String> history(int limit);
        void snapshot(ContextSummary summary);
        Optional<ContextSummary> loadSnapshot();
        void flush();
        boolean isLoaded();
    }

    /** Context metadata. */
    record ContextMeta(
            ContextId id,
            Path dbPath,
            java.time.Instant createdAt
    ) {}
}
