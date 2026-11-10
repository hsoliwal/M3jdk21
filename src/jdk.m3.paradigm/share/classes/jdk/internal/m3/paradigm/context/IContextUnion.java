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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Context Union - Root Sealed Type for All Context Variants.
 *
 * <h2>Union of Union Pattern</h2>
 * <pre>
 * IContextUnion (root)
 *   ├── IStructuralContext (tree structure)
 *   │     ├── NodeContext (has children)
 *   │     └── LeafContext (terminal)
 *   ├── IExecutionContext (runtime state)
 *   │     ├── ActiveContext (running)
 *   │     ├── CancelledContext (stopped)
 *   │     └── CompletedContext (done)
 *   └── IMemoryContext (persistence)
 *         ├── LoadedContext (materialized)
 *         ├── EvictedContext (flushed to disk)
 *         └── LazyContext (not yet loaded)
 * </pre>
 *
 * <h2>Why Union of Union?</h2>
 * <ul>
 *   <li>Exhaustive pattern matching with switch</li>
 *   <li>Compile-time type safety</li>
 *   <li>No instanceof chains</li>
 *   <li>Self-documenting hierarchies</li>
 *   <li>JIT-friendly (no reflection)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * String describe(IContextUnion ctx) {
 *     return switch (ctx) {
 *         // Structural
 *         case NodeContext n -> "Node with " + n.children().size() + " children";
 *         case LeafContext l -> "Leaf: " + l.id();
 *         // Execution
 *         case ActiveContext a -> "Running since " + a.startedAt();
 *         case CancelledContext c -> "Cancelled: " + c.reason();
 *         case CompletedContext d -> "Completed at " + d.completedAt();
 *         // Memory
 *         case LoadedContext m -> "Loaded: " + m.eventCount() + " events";
 *         case EvictedContext e -> "Evicted to " + e.dbPath();
 *         case LazyContext z -> "Lazy: " + z.contextId();
 *     };
 * }
 * }</pre>
 *
 * @since Context Mandelbrot - Union of Union
 */
@ISpec(
    value = "Union of Union - root sealed type for exhaustive context pattern matching",
    constraints = {
        "Sealed interface permits only declared subtypes",
        "Structural, Execution, and Memory context families",
        "Pattern matching with switch is exhaustive"
    },
    invariants = {
        "id() returns non-null UUID",
        "createdAt() returns non-null Instant",
        "metadata() returns non-null Map"
    },
    examples = {
        "switch (ctx) { case NodeContext n -> ... }",
        "IContextUnion.createRoot()",
        "IContextUnion.createActive()"
    },
    model = Model.ModelType.OPUS,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/mandelbrot/IContextUnion.java}
 * blob {@code 4baecdff92a1bb596c191669a4ea2c2d59f58055} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IContextUnion
        permits IContextUnion.IStructuralContext,
                IContextUnion.IExecutionContext,
                IContextUnion.IMemoryContext {

    /**
     * Unique identifier for this context.
     *
     * @return context UUID
     */
    UUID id();

    /**
     * Context creation timestamp.
     *
     * @return when this context was created
     */
    Instant createdAt();

    /**
     * Context metadata.
     *
     * @return metadata map
     */
    Map<String, Object> metadata();

    // --- STRUCTURAL CONTEXT UNION ---

    /** Structural Context - Tree structure with parent/children. */
    sealed interface IStructuralContext extends IContextUnion
            permits NodeContext, LeafContext {

        /**
         * Get parent context.
         *
         * @return optional parent
         */
        Optional<IStructuralContext> parent();

        /**
         * Get depth in tree.
         *
         * @return depth (0 for root)
         */
        default int depth() {
            return parent().map(p -> p.depth() + 1).orElse(0);
        }

        /**
         * Check if this is a root.
         *
         * @return true if no parent
         */
        default boolean isRoot() {
            return parent().isEmpty();
        }
    }

    /**
     * Node Context - Has children.
     *
     * @param id context UUID
     * @param createdAt creation time
     * @param metadata context metadata
     * @param parentRef parent context reference (nullable)
     * @param children child contexts
     */
    record NodeContext(
            UUID id,
            Instant createdAt,
            Map<String, Object> metadata,
            IStructuralContext parentRef,
            List<IStructuralContext> children
    ) implements IStructuralContext {
        @Override
        public Optional<IStructuralContext> parent() {
            return Optional.ofNullable(parentRef);
        }
    }

    /**
     * Leaf Context - Terminal node, no children.
     *
     * @param id context UUID
     * @param createdAt creation time
     * @param metadata context metadata
     * @param parentRef parent context reference (nullable)
     * @param value leaf value
     */
    record LeafContext(
            UUID id,
            Instant createdAt,
            Map<String, Object> metadata,
            IStructuralContext parentRef,
            Object value
    ) implements IStructuralContext {
        @Override
        public Optional<IStructuralContext> parent() {
            return Optional.ofNullable(parentRef);
        }
    }

    // --- EXECUTION CONTEXT UNION ---

    /** Execution Context - Runtime state for COP. */
    sealed interface IExecutionContext extends IContextUnion
            permits ActiveContext, CancelledContext, CompletedContext {

        /**
         * When execution started.
         *
         * @return start timestamp
         */
        Instant startedAt();

        /**
         * Check if execution is finished.
         *
         * @return true if done
         */
        boolean isDone();
    }

    /**
     * Active Context - Currently executing.
     *
     * @param id context UUID
     * @param createdAt creation time
     * @param metadata context metadata
     * @param startedAt when started
     * @param deadline optional deadline
     */
    record ActiveContext(
            UUID id,
            Instant createdAt,
            Map<String, Object> metadata,
            Instant startedAt,
            Instant deadline
    ) implements IExecutionContext {
        @Override
        public boolean isDone() {
            return false;
        }

        /**
         * Check if deadline is exceeded.
         *
         * @return true if past deadline
         */
        public boolean isDeadlineExceeded() {
            return deadline != null && Instant.now().isAfter(deadline);
        }
    }

    /**
     * Cancelled Context - Execution was cancelled.
     *
     * @param id context UUID
     * @param createdAt creation time
     * @param metadata context metadata
     * @param startedAt when started
     * @param cancelledAt when cancelled
     * @param reason cancellation reason
     */
    record CancelledContext(
            UUID id,
            Instant createdAt,
            Map<String, Object> metadata,
            Instant startedAt,
            Instant cancelledAt,
            String reason
    ) implements IExecutionContext {
        @Override
        public boolean isDone() {
            return true;
        }
    }

    /**
     * Completed Context - Execution finished successfully.
     *
     * @param id context UUID
     * @param createdAt creation time
     * @param metadata context metadata
     * @param startedAt when started
     * @param completedAt when completed
     * @param result execution result
     */
    record CompletedContext(
            UUID id,
            Instant createdAt,
            Map<String, Object> metadata,
            Instant startedAt,
            Instant completedAt,
            Object result
    ) implements IExecutionContext {
        @Override
        public boolean isDone() {
            return true;
        }
    }

    // --- MEMORY CONTEXT UNION ---

    /** Memory Context - Persistence state for ContextOS. */
    sealed interface IMemoryContext extends IContextUnion
            permits LoadedContext, EvictedContext, LazyContext {

        /**
         * Database path for this context.
         *
         * @return path to UUID.db file
         */
        java.nio.file.Path dbPath();
    }

    /**
     * Loaded Context - Materialized in memory.
     *
     * @param id context UUID
     * @param createdAt creation time
     * @param metadata context metadata
     * @param dbPath database file path
     * @param eventCount number of events loaded
     * @param lastAccess last access time
     */
    record LoadedContext(
            UUID id,
            Instant createdAt,
            Map<String, Object> metadata,
            java.nio.file.Path dbPath,
            int eventCount,
            Instant lastAccess
    ) implements IMemoryContext {}

    /**
     * Evicted Context - Flushed to disk, not in memory.
     *
     * @param id context UUID
     * @param createdAt creation time
     * @param metadata context metadata
     * @param dbPath database file path
     * @param evictedAt when evicted
     * @param snapshotVersion version at eviction
     */
    record EvictedContext(
            UUID id,
            Instant createdAt,
            Map<String, Object> metadata,
            java.nio.file.Path dbPath,
            Instant evictedAt,
            int snapshotVersion
    ) implements IMemoryContext {}

    /**
     * Lazy Context - Not yet loaded (proxy).
     *
     * @param id context UUID
     * @param createdAt creation time
     * @param metadata context metadata
     * @param dbPath database file path
     */
    record LazyContext(
            UUID id,
            Instant createdAt,
            Map<String, Object> metadata,
            java.nio.file.Path dbPath
    ) implements IMemoryContext {
        /**
         * Get context ID as string.
         *
         * @return UUID string
         */
        public String contextId() {
            return id.toString();
        }
    }

    // --- FACTORY METHODS ---

    /**
     * Create a root node context.
     *
     * @return new root node
     */
    static NodeContext createRoot() {
        return new NodeContext(
                UUID.randomUUID(),
                Instant.now(),
                Map.of(),
                null,
                List.of()
        );
    }

    /**
     * Create an active execution context.
     *
     * @return new active context
     */
    static ActiveContext createActive() {
        Instant now = Instant.now();
        return new ActiveContext(
                UUID.randomUUID(),
                now,
                Map.of(),
                now,
                null
        );
    }

    /**
     * Create a lazy memory context.
     *
     * @param dbPath path to database file
     * @return new lazy context
     */
    static LazyContext createLazy(java.nio.file.Path dbPath) {
        return new LazyContext(
                UUID.randomUUID(),
                Instant.now(),
                Map.of(),
                dbPath
        );
    }
}
