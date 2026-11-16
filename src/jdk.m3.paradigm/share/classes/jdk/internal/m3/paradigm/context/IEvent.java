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
 * Event Union - Domain Event algebraic types.
 *
 * <p>A sealed hierarchy representing all domain events.
 * Events are immutable facts about things that happened.
 *
 * <h2>Event Categories</h2>
 * <ul>
 *   <li>{@link IDomainEvent} - Business domain events</li>
 *   <li>{@link ISystemEvent} - System/infrastructure events</li>
 *   <li>{@link IIntegrationEvent} - Cross-boundary events</li>
 * </ul>
 *
 * <h2>Union of Union Structure</h2>
 * <pre>
 * IEvent (root)
 *   ├── IDomainEvent
 *   │     ├── EntityCreated
 *   │     ├── EntityUpdated
 *   │     ├── EntityDeleted
 *   │     └── StateTransitioned
 *   ├── ISystemEvent
 *   │     ├── ServiceStarted
 *   │     ├── ServiceStopped
 *   │     ├── HealthCheck
 *   │     └── ErrorOccurred
 *   └── IIntegrationEvent
 *         ├── MessageReceived
 *         ├── MessageSent
 *         └── IntegrationFailed
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * void handle(IEvent event) {
 *     switch (event) {
 *         case EntityCreated e -> repository.add(e.entityId());
 *         case EntityDeleted e -> repository.remove(e.entityId());
 *         case ErrorOccurred e -> alerting.notify(e.error());
 *         // ... exhaustive
 *     }
 * }
 * }</pre>
 *
 * @since Grand Java Patterns Proof - Union of Union
 */
@ISpec(
    value = "Event Union - Domain events as sealed algebraic types",
    constraints = {
        "Events are immutable facts about things that happened",
        "Union of Union: IEvent → IDomainEvent|ISystemEvent|IIntegrationEvent",
        "Each sub-union has specific variants (EntityCreated, ServiceStarted, etc.)",
        "Exhaustive pattern matching in switch expressions"
    },
    invariants = {
        "eventId() returns non-null unique identifier",
        "timestamp() returns non-null Instant",
        "eventType() returns non-null type name for routing"
    },
    examples = {
        "switch(event) { case EntityCreated e -> ...; }",
        "new EntityCreated(\"User\", \"123\", Map.of())"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/union/IEvent.java}
 * blob {@code 1f32dc6049d35a3bd89f3a9ee3e608938fdb203b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IEvent
        permits IEvent.IDomainEvent, IEvent.ISystemEvent, IEvent.IIntegrationEvent {

    /**
     * Get event ID.
     *
     * @return unique event identifier
     */
    String eventId();

    /**
     * Get event timestamp.
     *
     * @return when event occurred
     */
    Instant timestamp();

    /**
     * Get event type name.
     *
     * @return type name for routing/serialization
     */
    String eventType();

    /**
     * Get correlation ID for tracing.
     *
     * @return correlation ID
     */
    String correlationId();

    // --- Domain Event Union ---

    /** Domain Event - Business domain events. */
    sealed interface IDomainEvent extends IEvent
            permits EntityCreated, EntityUpdated, EntityDeleted, StateTransitioned {

        /**
         * Get aggregate ID.
         *
         * @return aggregate root ID
         */
        String aggregateId();

        /**
         * Get aggregate type.
         *
         * @return aggregate type name
         */
        String aggregateType();
    }

    /**
     * Entity created event.
     *
     * @param eventId event identifier
     * @param timestamp event time
     * @param correlationId correlation ID
     * @param aggregateId aggregate ID
     * @param aggregateType aggregate type
     * @param entityId created entity ID
     * @param initialState initial state data
     */
    record EntityCreated(
            String eventId,
            Instant timestamp,
            String correlationId,
            String aggregateId,
            String aggregateType,
            String entityId,
            Map<String, Object> initialState
    ) implements IDomainEvent {

        /**
         * Create with auto-generated IDs.
         *
         * @param aggregateType aggregate type
         * @param entityId entity ID
         * @param initialState initial state
         */
        public EntityCreated(String aggregateType, String entityId, Map<String, Object> initialState) {
            this(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    UUID.randomUUID().toString(),
                    entityId,
                    aggregateType,
                    entityId,
                    initialState
            );
        }

        @Override
        public String eventType() {
            return aggregateType + ".Created";
        }
    }

    /**
     * Entity updated event.
     *
     * @param eventId event identifier
     * @param timestamp event time
     * @param correlationId correlation ID
     * @param aggregateId aggregate ID
     * @param aggregateType aggregate type
     * @param entityId updated entity ID
     * @param changes changed fields
     * @param version new version
     */
    record EntityUpdated(
            String eventId,
            Instant timestamp,
            String correlationId,
            String aggregateId,
            String aggregateType,
            String entityId,
            Map<String, Object> changes,
            long version
    ) implements IDomainEvent {
        @Override
        public String eventType() {
            return aggregateType + ".Updated";
        }
    }

    /**
     * Entity deleted event.
     *
     * @param eventId event identifier
     * @param timestamp event time
     * @param correlationId correlation ID
     * @param aggregateId aggregate ID
     * @param aggregateType aggregate type
     * @param entityId deleted entity ID
     * @param reason deletion reason
     */
    record EntityDeleted(
            String eventId,
            Instant timestamp,
            String correlationId,
            String aggregateId,
            String aggregateType,
            String entityId,
            String reason
    ) implements IDomainEvent {
        @Override
        public String eventType() {
            return aggregateType + ".Deleted";
        }
    }

    /**
     * State transition event.
     *
     * @param eventId event identifier
     * @param timestamp event time
     * @param correlationId correlation ID
     * @param aggregateId aggregate ID
     * @param aggregateType aggregate type
     * @param fromState previous state
     * @param toState new state
     * @param trigger what triggered transition
     */
    record StateTransitioned(
            String eventId,
            Instant timestamp,
            String correlationId,
            String aggregateId,
            String aggregateType,
            String fromState,
            String toState,
            String trigger
    ) implements IDomainEvent {
        @Override
        public String eventType() {
            return aggregateType + ".StateTransitioned";
        }
    }

    // --- System Event Union ---

    /** System Event - Infrastructure events. */
    sealed interface ISystemEvent extends IEvent
            permits ServiceStarted, ServiceStopped, HealthCheck, ErrorOccurred {

        /**
         * Get service name.
         *
         * @return service identifier
         */
        String serviceName();
    }

    /**
     * Service started event.
     *
     * @param eventId event identifier
     * @param timestamp event time
     * @param correlationId correlation ID
     * @param serviceName service name
     * @param version service version
     * @param config startup configuration
     */
    record ServiceStarted(
            String eventId,
            Instant timestamp,
            String correlationId,
            String serviceName,
            String version,
            Map<String, String> config
    ) implements ISystemEvent {
        @Override
        public String eventType() {
            return "System.ServiceStarted";
        }
    }

    /**
     * Service stopped event.
     *
     * @param eventId event identifier
     * @param timestamp event time
     * @param correlationId correlation ID
     * @param serviceName service name
     * @param reason stop reason
     * @param graceful whether stopped gracefully
     */
    record ServiceStopped(
            String eventId,
            Instant timestamp,
            String correlationId,
            String serviceName,
            String reason,
            boolean graceful
    ) implements ISystemEvent {
        @Override
        public String eventType() {
            return "System.ServiceStopped";
        }
    }

    /**
     * Health check event.
     *
     * @param eventId event identifier
     * @param timestamp event time
     * @param correlationId correlation ID
     * @param serviceName service name
     * @param status health status
     * @param checks individual check results
     */
    record HealthCheck(
            String eventId,
            Instant timestamp,
            String correlationId,
            String serviceName,
            Status status,
            Map<String, Status> checks
    ) implements ISystemEvent {

        /** Health status. */
        public enum Status {
            /** Service is healthy. */
            HEALTHY,
            /** Service is degraded. */
            DEGRADED,
            /** Service is unhealthy. */
            UNHEALTHY
        }

        @Override
        public String eventType() {
            return "System.HealthCheck";
        }
    }

    /**
     * Error occurred event.
     *
     * @param eventId event identifier
     * @param timestamp event time
     * @param correlationId correlation ID
     * @param serviceName service name
     * @param error error that occurred
     * @param severity error severity
     * @param context error context
     */
    record ErrorOccurred(
            String eventId,
            Instant timestamp,
            String correlationId,
            String serviceName,
            Throwable error,
            Severity severity,
            Map<String, Object> context
    ) implements ISystemEvent {

        /** Error severity. */
        public enum Severity {
            /** Warning - can continue. */
            WARNING,
            /** Error - operation failed. */
            ERROR,
            /** Critical - service impacted. */
            CRITICAL,
            /** Fatal - service must restart. */
            FATAL
        }

        @Override
        public String eventType() {
            return "System.ErrorOccurred";
        }
    }

    // --- Integration Event Union ---

    /** Integration Event - Cross-boundary events. */
    sealed interface IIntegrationEvent extends IEvent
            permits MessageReceived, MessageSent, IntegrationFailed {

        /**
         * Get source system.
         *
         * @return source system identifier
         */
        String source();

        /**
         * Get target system.
         *
         * @return target system identifier
         */
        String target();
    }

    /**
     * Message received event.
     *
     * @param eventId event identifier
     * @param timestamp event time
     * @param correlationId correlation ID
     * @param source source system
     * @param target target system
     * @param messageType message type
     * @param payload message payload
     */
    record MessageReceived(
            String eventId,
            Instant timestamp,
            String correlationId,
            String source,
            String target,
            String messageType,
            Object payload
    ) implements IIntegrationEvent {
        @Override
        public String eventType() {
            return "Integration.MessageReceived";
        }
    }

    /**
     * Message sent event.
     *
     * @param eventId event identifier
     * @param timestamp event time
     * @param correlationId correlation ID
     * @param source source system
     * @param target target system
     * @param messageType message type
     * @param messageId sent message ID
     */
    record MessageSent(
            String eventId,
            Instant timestamp,
            String correlationId,
            String source,
            String target,
            String messageType,
            String messageId
    ) implements IIntegrationEvent {
        @Override
        public String eventType() {
            return "Integration.MessageSent";
        }
    }

    /**
     * Integration failed event.
     *
     * @param eventId event identifier
     * @param timestamp event time
     * @param correlationId correlation ID
     * @param source source system
     * @param target target system
     * @param error failure error
     * @param retryable whether operation can be retried
     * @param attempts number of attempts made
     */
    record IntegrationFailed(
            String eventId,
            Instant timestamp,
            String correlationId,
            String source,
            String target,
            Throwable error,
            boolean retryable,
            int attempts
    ) implements IIntegrationEvent {
        @Override
        public String eventType() {
            return "Integration.Failed";
        }
    }
}
