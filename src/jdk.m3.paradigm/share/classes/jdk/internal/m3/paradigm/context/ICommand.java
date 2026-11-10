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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Command Union - Command pattern as algebraic type.
 *
 * <p>Commands represent intentions to change state.
 * Unlike events (facts), commands may be rejected.
 *
 * <h2>Command vs Event</h2>
 * <pre>
 * Command (imperative): "Create user John"
 * Event (past tense):   "User John was created"
 *
 * Commands MAY fail.
 * Events HAVE happened.
 * </pre>
 *
 * <h2>Command Categories</h2>
 * <ul>
 *   <li>{@link IWriteCommand} - State mutation commands</li>
 *   <li>{@link IQueryCommand} - Read operations (CQRS)</li>
 *   <li>{@link IBatchCommand} - Composite commands</li>
 *   <li>{@link ICompensatingCommand} - Saga rollback</li>
 * </ul>
 *
 * <h2>Union of Union Structure</h2>
 * <pre>
 * ICommand (root)
 *   ├── IWriteCommand
 *   │     ├── Create
 *   │     ├── Update
 *   │     ├── Delete
 *   │     └── Execute
 *   ├── IQueryCommand
 *   │     ├── FindById
 *   │     ├── FindByQuery
 *   │     └── Count
 *   ├── IBatchCommand
 *   │     ├── Batch
 *   │     └── Transactional
 *   └── ICompensatingCommand
 *         ├── Undo
 *         └── Compensate
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * IOutcome<?> handle(ICommand cmd) {
 *     return switch (cmd) {
 *         case Create c -> createEntity(c.entityType(), c.data());
 *         case Update u -> updateEntity(u.entityId(), u.changes());
 *         case Delete d -> deleteEntity(d.entityId());
 *         case FindById q -> findById(q.entityType(), q.id());
 *         case Batch b -> executeBatch(b.commands());
 *         case Undo u -> undoCommand(u.originalCommand());
 *         // ... exhaustive
 *     };
 * }
 * }</pre>
 *
 * @since Grand Java Patterns Proof - Union of Union
 */
@ISpec(
    value = "Command Union - CQRS commands as sealed algebraic types",
    constraints = {
        "Commands represent intentions to change state (may be rejected)",
        "Commands MAY fail, Events HAVE happened",
        "Union of Union: ICommand → IWriteCommand|IQueryCommand|IBatchCommand|ICompensatingCommand",
        "Supports Saga pattern via ICompensatingCommand"
    },
    invariants = {
        "commandId() returns non-null unique identifier",
        "timestamp() returns non-null Instant",
        "commandType() returns non-null type name for routing"
    },
    examples = {
        "new Create(\"User\", Map.of(\"name\", \"John\"))",
        "switch(cmd) { case Create c -> createEntity(...); }"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/union/ICommand.java}
 * blob {@code 353de6ac7cef486dce6895d696b9555438ee93c5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface ICommand
        permits ICommand.IWriteCommand, ICommand.IQueryCommand,
                ICommand.IBatchCommand, ICommand.ICompensatingCommand {

    /**
     * Get command ID.
     *
     * @return unique command identifier
     */
    String commandId();

    /**
     * Get command timestamp.
     *
     * @return when command was created
     */
    Instant timestamp();

    /**
     * Get command type name.
     *
     * @return type for routing
     */
    String commandType();

    /**
     * Get correlation ID for tracing.
     *
     * @return correlation ID
     */
    String correlationId();

    /**
     * Get causation ID (what triggered this).
     *
     * @return causation ID or null
     */
    String causationId();

    // --- Write Command Union ---

    /** Write Command - State mutation commands. */
    sealed interface IWriteCommand extends ICommand
            permits Create, Update, Delete, Execute {

        /**
         * Get target entity type.
         *
         * @return entity type name
         */
        String entityType();
    }

    /**
     * Create command - create new entity.
     *
     * @param commandId command identifier
     * @param timestamp command time
     * @param correlationId correlation ID
     * @param causationId causation ID
     * @param entityType entity type to create
     * @param data initial data
     * @param expectedId expected entity ID (optional)
      * @author Synexia
     */
    record Create(
            String commandId,
            Instant timestamp,
            String correlationId,
            String causationId,
            String entityType,
            Map<String, Object> data,
            String expectedId
    ) implements IWriteCommand {

        /**
         * Create command with auto-generated IDs.
         *
         * @param entityType entity type
         * @param data initial data
         */
        public Create(String entityType, Map<String, Object> data) {
            this(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    UUID.randomUUID().toString(),
                    null,
                    entityType,
                    data,
                    null
            );
        }

        @Override
        public String commandType() {
            return entityType + ".Create";
        }
    }

    /**
     * Update command - modify existing entity.
     *
     * @param commandId command identifier
     * @param timestamp command time
     * @param correlationId correlation ID
     * @param causationId causation ID
     * @param entityType entity type
     * @param entityId entity to update
     * @param changes fields to change
     * @param expectedVersion optimistic lock version
     */
    record Update(
            String commandId,
            Instant timestamp,
            String correlationId,
            String causationId,
            String entityType,
            String entityId,
            Map<String, Object> changes,
            Long expectedVersion
    ) implements IWriteCommand {

        /**
         * Create update command with auto-generated IDs.
         *
         * @param entityType entity type
         * @param entityId entity ID
         * @param changes changes to apply
         */
        public Update(String entityType, String entityId, Map<String, Object> changes) {
            this(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    UUID.randomUUID().toString(),
                    null,
                    entityType,
                    entityId,
                    changes,
                    null
            );
        }

        @Override
        public String commandType() {
            return entityType + ".Update";
        }
    }

    /**
     * Delete command - remove entity.
     *
     * @param commandId command identifier
     * @param timestamp command time
     * @param correlationId correlation ID
     * @param causationId causation ID
     * @param entityType entity type
     * @param entityId entity to delete
     * @param soft whether to soft delete
     * @param reason deletion reason
     */
    record Delete(
            String commandId,
            Instant timestamp,
            String correlationId,
            String causationId,
            String entityType,
            String entityId,
            boolean soft,
            String reason
    ) implements IWriteCommand {

        /**
         * Create delete command with auto-generated IDs.
         *
         * @param entityType entity type
         * @param entityId entity ID
         */
        public Delete(String entityType, String entityId) {
            this(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    UUID.randomUUID().toString(),
                    null,
                    entityType,
                    entityId,
                    false,
                    null
            );
        }

        @Override
        public String commandType() {
            return entityType + ".Delete";
        }
    }

    /**
     * Execute command - run an operation.
     *
     * @param commandId command identifier
     * @param timestamp command time
     * @param correlationId correlation ID
     * @param causationId causation ID
     * @param entityType entity type
     * @param operation operation name
     * @param parameters operation parameters
     */
    record Execute(
            String commandId,
            Instant timestamp,
            String correlationId,
            String causationId,
            String entityType,
            String operation,
            Map<String, Object> parameters
    ) implements IWriteCommand {
        @Override
        public String commandType() {
            return entityType + "." + operation;
        }
    }

    // --- Query Command Union ---

    /** Query Command - Read operations (CQRS queries). */
    sealed interface IQueryCommand extends ICommand
            permits FindById, FindByQuery, Count {

        /**
         * Get target entity type.
         *
         * @return entity type name
         */
        String entityType();
    }

    /**
     * Find by ID query.
     *
     * @param commandId command identifier
     * @param timestamp command time
     * @param correlationId correlation ID
     * @param causationId causation ID
     * @param entityType entity type
     * @param id entity ID to find
     */
    record FindById(
            String commandId,
            Instant timestamp,
            String correlationId,
            String causationId,
            String entityType,
            String id
    ) implements IQueryCommand {
        @Override
        public String commandType() {
            return entityType + ".FindById";
        }
    }

    /**
     * Find by query.
     *
     * @param commandId command identifier
     * @param timestamp command time
     * @param correlationId correlation ID
     * @param causationId causation ID
     * @param entityType entity type
     * @param query query criteria
     * @param offset pagination offset
     * @param limit pagination limit
     * @param orderBy sort order
     */
    record FindByQuery(
            String commandId,
            Instant timestamp,
            String correlationId,
            String causationId,
            String entityType,
            Map<String, Object> query,
            int offset,
            int limit,
            String orderBy
    ) implements IQueryCommand {
        @Override
        public String commandType() {
            return entityType + ".FindByQuery";
        }
    }

    /**
     * Count query.
     *
     * @param commandId command identifier
     * @param timestamp command time
     * @param correlationId correlation ID
     * @param causationId causation ID
     * @param entityType entity type
     * @param query count criteria
     */
    record Count(
            String commandId,
            Instant timestamp,
            String correlationId,
            String causationId,
            String entityType,
            Map<String, Object> query
    ) implements IQueryCommand {
        @Override
        public String commandType() {
            return entityType + ".Count";
        }
    }

    // --- Batch Command Union ---

    /** Batch Command - Composite commands. */
    sealed interface IBatchCommand extends ICommand
            permits Batch, Transactional {
    }

    /**
     * Batch of commands (may partially succeed).
     *
     * @param commandId command identifier
     * @param timestamp command time
     * @param correlationId correlation ID
     * @param causationId causation ID
     * @param commands commands to execute
     * @param stopOnError whether to stop on first error
     */
    record Batch(
            String commandId,
            Instant timestamp,
            String correlationId,
            String causationId,
            List<ICommand> commands,
            boolean stopOnError
    ) implements IBatchCommand {
        @Override
        public String commandType() {
            return "Batch";
        }
    }

    /**
     * Transactional batch (all-or-nothing).
     *
     * @param commandId command identifier
     * @param timestamp command time
     * @param correlationId correlation ID
     * @param causationId causation ID
     * @param commands commands to execute atomically
     */
    record Transactional(
            String commandId,
            Instant timestamp,
            String correlationId,
            String causationId,
            List<ICommand> commands
    ) implements IBatchCommand {
        @Override
        public String commandType() {
            return "Transactional";
        }
    }

    // --- Compensating Command Union ---

    /** Compensating Command - Saga rollback. */
    sealed interface ICompensatingCommand extends ICommand
            permits Undo, Compensate {
    }

    /**
     * Undo command - reverse a command.
     *
     * @param commandId command identifier
     * @param timestamp command time
     * @param correlationId correlation ID
     * @param causationId causation ID
     * @param originalCommand command to undo
     */
    record Undo(
            String commandId,
            Instant timestamp,
            String correlationId,
            String causationId,
            ICommand originalCommand
    ) implements ICompensatingCommand {
        @Override
        public String commandType() {
            return "Undo." + originalCommand.commandType();
        }
    }

    /**
     * Compensate command - apply compensation action.
     *
     * @param commandId command identifier
     * @param timestamp command time
     * @param correlationId correlation ID
     * @param causationId causation ID
     * @param sagaId saga identifier
     * @param stepId step to compensate
     * @param compensationData data for compensation
     */
    record Compensate(
            String commandId,
            Instant timestamp,
            String correlationId,
            String causationId,
            String sagaId,
            String stepId,
            Map<String, Object> compensationData
    ) implements ICompensatingCommand {
        @Override
        public String commandType() {
            return "Compensate." + stepId;
        }
    }
}
