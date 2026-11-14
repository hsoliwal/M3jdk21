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
package jdk.internal.m3.paradigm.contract;

/**
 * Microservice Patterns - Resilience and Architecture.
 *
 * <p>
 * Patterns for building resilient, scalable microservices.
 * @m3.pattern synexia:iop
 * @m3.algorithm Defines 42 singleton tokens in declaration order, with generated name, ordinal, values and valueOf behavior. The declared descriptor constructor retains its supplied references and getters return them directly. Naming a pattern or annotation does not implement that named behavior.
 * @m3.dataStructures Enum values in source order: RETRY, CIRCUIT_BREAKER, TIMEOUT, BULKHEAD, RATE_LIMITER, FALLBACK, HEDGING, HEALTH_CHECK, SERVICE_REGISTRY, CLIENT_SIDE_DISCOVERY, SERVER_SIDE_DISCOVERY, SELF_REGISTRATION, THIRD_PARTY_REGISTRATION, DATABASE_PER_SERVICE, SHARED_DATABASE, SAGA, CQRS, EVENT_SOURCING, API_COMPOSITION, API_GATEWAY, BACKEND_FOR_FRONTEND, SERVICE_MESH, SIDECAR, AMBASSADOR, DECOMPOSE_BY_BUSINESS_CAPABILITY, DECOMPOSE_BY_SUBDOMAIN, STRANGLER_FIG, ANTI_CORRUPTION_LAYER, DISTRIBUTED_TRACING, LOG_AGGREGATION, EXCEPTION_TRACKING, APPLICATION_METRICS, AUDIT_LOGGING, MULTIPLE_INSTANCES_PER_HOST, SINGLE_INSTANCE_PER_HOST, SERVICE_PER_VM, SERVICE_PER_CONTAINER, SERVERLESS, BLUE_GREEN_DEPLOYMENT, CANARY_DEPLOYMENT, ACCESS_TOKEN, EXTERNALIZED_CONFIGURATION.
 * @m3.member {@code FIELD:RETRY} pattern=synexia:iop; algorithm=Initializes RETRY using the existing expression <code>new Microservice("Resilience", "Retry failed operations with backoff")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:CIRCUIT_BREAKER} pattern=synexia:iop; algorithm=Initializes CIRCUIT_BREAKER using the existing expression <code>new Microservice("Resilience", "Prevent cascade failures")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:TIMEOUT} pattern=synexia:iop; algorithm=Initializes TIMEOUT using the existing expression <code>new Microservice("Resilience", "Limit operation duration")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:BULKHEAD} pattern=synexia:iop; algorithm=Initializes BULKHEAD using the existing expression <code>new Microservice("Resilience", "Isolate failures in compartments")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:RATE_LIMITER} pattern=synexia:iop; algorithm=Initializes RATE_LIMITER using the existing expression <code>new Microservice("Resilience", "Control request rate")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:FALLBACK} pattern=synexia:iop; algorithm=Initializes FALLBACK using the existing expression <code>new Microservice("Resilience", "Provide alternative when operation fails")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:HEDGING} pattern=synexia:iop; algorithm=Initializes HEDGING using the existing expression <code>new Microservice("Resilience", "Send parallel requests, use fastest")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:HEALTH_CHECK} pattern=synexia:iop; algorithm=Initializes HEALTH_CHECK using the existing expression <code>new Microservice("Resilience", "Monitor service health")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SERVICE_REGISTRY} pattern=synexia:iop; algorithm=Initializes SERVICE_REGISTRY using the existing expression <code>new Microservice("Discovery", "Register and discover services")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:CLIENT_SIDE_DISCOVERY} pattern=synexia:iop; algorithm=Initializes CLIENT_SIDE_DISCOVERY using the existing expression <code>new Microservice("Discovery", "Client queries registry directly")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SERVER_SIDE_DISCOVERY} pattern=synexia:iop; algorithm=Initializes SERVER_SIDE_DISCOVERY using the existing expression <code>new Microservice("Discovery", "Router queries registry")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SELF_REGISTRATION} pattern=synexia:iop; algorithm=Initializes SELF_REGISTRATION using the existing expression <code>new Microservice("Discovery", "Service registers itself")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:THIRD_PARTY_REGISTRATION} pattern=synexia:iop; algorithm=Initializes THIRD_PARTY_REGISTRATION using the existing expression <code>new Microservice("Discovery", "External registrar registers services")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:DATABASE_PER_SERVICE} pattern=synexia:iop; algorithm=Initializes DATABASE_PER_SERVICE using the existing expression <code>new Microservice("Data", "Each service owns its database")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SHARED_DATABASE} pattern=synexia:iop; algorithm=Initializes SHARED_DATABASE using the existing expression <code>new Microservice("Data", "Services share database")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SAGA} pattern=synexia:iop; algorithm=Initializes SAGA using the existing expression <code>new Microservice("Data", "Manage distributed transactions")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:CQRS} pattern=synexia:iop; algorithm=Initializes CQRS using the existing expression <code>new Microservice("Data", "Separate read and write models")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:EVENT_SOURCING} pattern=synexia:iop; algorithm=Initializes EVENT_SOURCING using the existing expression <code>new Microservice("Data", "Store state as event sequence")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:API_COMPOSITION} pattern=synexia:iop; algorithm=Initializes API_COMPOSITION using the existing expression <code>new Microservice("Data", "Aggregate data from multiple services")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:API_GATEWAY} pattern=synexia:iop; algorithm=Initializes API_GATEWAY using the existing expression <code>new Microservice("Communication", "Single entry point for clients")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:BACKEND_FOR_FRONTEND} pattern=synexia:iop; algorithm=Initializes BACKEND_FOR_FRONTEND using the existing expression <code>new Microservice("Communication", "API gateway per client type")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SERVICE_MESH} pattern=synexia:iop; algorithm=Initializes SERVICE_MESH using the existing expression <code>new Microservice("Communication", "Infrastructure layer for service communication")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SIDECAR} pattern=synexia:iop; algorithm=Initializes SIDECAR using the existing expression <code>new Microservice("Communication", "Deploy helper alongside service")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:AMBASSADOR} pattern=synexia:iop; algorithm=Initializes AMBASSADOR using the existing expression <code>new Microservice("Communication", "Helper for connecting to external services")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:DECOMPOSE_BY_BUSINESS_CAPABILITY} pattern=synexia:iop; algorithm=Initializes DECOMPOSE_BY_BUSINESS_CAPABILITY using the existing expression <code>new Microservice("Decomposition", "Services align with business capabilities")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:DECOMPOSE_BY_SUBDOMAIN} pattern=synexia:iop; algorithm=Initializes DECOMPOSE_BY_SUBDOMAIN using the existing expression <code>new Microservice("Decomposition", "Services align with DDD subdomains")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:STRANGLER_FIG} pattern=synexia:iop; algorithm=Initializes STRANGLER_FIG using the existing expression <code>new Microservice("Decomposition", "Gradually replace legacy system")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:ANTI_CORRUPTION_LAYER} pattern=synexia:iop; algorithm=Initializes ANTI_CORRUPTION_LAYER using the existing expression <code>new Microservice("Decomposition", "Isolate from legacy/external systems")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:DISTRIBUTED_TRACING} pattern=synexia:iop; algorithm=Initializes DISTRIBUTED_TRACING using the existing expression <code>new Microservice("Observability", "Trace requests across services")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:LOG_AGGREGATION} pattern=synexia:iop; algorithm=Initializes LOG_AGGREGATION using the existing expression <code>new Microservice("Observability", "Centralize logs from all services")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:EXCEPTION_TRACKING} pattern=synexia:iop; algorithm=Initializes EXCEPTION_TRACKING using the existing expression <code>new Microservice("Observability", "Track and aggregate exceptions")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:APPLICATION_METRICS} pattern=synexia:iop; algorithm=Initializes APPLICATION_METRICS using the existing expression <code>new Microservice("Observability", "Collect and expose metrics")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:AUDIT_LOGGING} pattern=synexia:iop; algorithm=Initializes AUDIT_LOGGING using the existing expression <code>new Microservice("Observability", "Log significant events")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:MULTIPLE_INSTANCES_PER_HOST} pattern=synexia:iop; algorithm=Initializes MULTIPLE_INSTANCES_PER_HOST using the existing expression <code>new Microservice("Deployment", "Run multiple services per host")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SINGLE_INSTANCE_PER_HOST} pattern=synexia:iop; algorithm=Initializes SINGLE_INSTANCE_PER_HOST using the existing expression <code>new Microservice("Deployment", "One service per host")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SERVICE_PER_VM} pattern=synexia:iop; algorithm=Initializes SERVICE_PER_VM using the existing expression <code>new Microservice("Deployment", "One service per virtual machine")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SERVICE_PER_CONTAINER} pattern=synexia:iop; algorithm=Initializes SERVICE_PER_CONTAINER using the existing expression <code>new Microservice("Deployment", "One service per container")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SERVERLESS} pattern=synexia:iop; algorithm=Initializes SERVERLESS using the existing expression <code>new Microservice("Deployment", "Deploy as serverless functions")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:BLUE_GREEN_DEPLOYMENT} pattern=synexia:iop; algorithm=Initializes BLUE_GREEN_DEPLOYMENT using the existing expression <code>new Microservice("Deployment", "Zero-downtime deployment")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:CANARY_DEPLOYMENT} pattern=synexia:iop; algorithm=Initializes CANARY_DEPLOYMENT using the existing expression <code>new Microservice("Deployment", "Gradual rollout to subset")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:ACCESS_TOKEN} pattern=synexia:iop; algorithm=Initializes ACCESS_TOKEN using the existing expression <code>new Microservice("Security", "Token-based authentication")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:EXTERNALIZED_CONFIGURATION} pattern=synexia:iop; algorithm=Initializes EXTERNALIZED_CONFIGURATION using the existing expression <code>new Microservice("Security", "Store config outside service")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One Microservice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:category} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The category declaration is a stored String field or record component with no initializer and no executable algorithm body.; algorithm.review=iop-annotation-contracts-declarations/reviewed-declarations.json#Microservice-5209; data.structures=One stored String field or component. The enum or record constructor retains the supplied reference directly, without validation or defensive copying.; description=Stored category descriptor in Microservice.
 * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration is a stored String field or record component with no initializer and no executable algorithm body.; algorithm.review=iop-annotation-contracts-declarations/reviewed-declarations.json#Microservice-5244; data.structures=One stored String field or component. The enum or record constructor retains the supplied reference directly, without validation or defensive copying.; description=Stored description descriptor in Microservice.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/Microservice.java}
 * blob {@code fb3612ef598fd70b89ea5f58acfb42b2b9ed90be} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum Microservice {

    // --- RESILIENCE PATTERNS ---
    /** Retry with backoff. */
    RETRY("Resilience", "Retry failed operations with backoff"),
    /** Circuit breaker. */
    CIRCUIT_BREAKER("Resilience", "Prevent cascade failures"),
    /** Timeout. */
    TIMEOUT("Resilience", "Limit operation duration"),
    /** Bulkhead. */
    BULKHEAD("Resilience", "Isolate failures in compartments"),
    /** Rate limiter. */
    RATE_LIMITER("Resilience", "Control request rate"),
    /** Fallback. */
    FALLBACK("Resilience", "Provide alternative when operation fails"),
    /** Hedging. */
    HEDGING("Resilience", "Send parallel requests, use fastest"),
    /** Health check. */
    HEALTH_CHECK("Resilience", "Monitor service health"),

    // --- SERVICE DISCOVERY ---
    /** Service registry. */
    SERVICE_REGISTRY("Discovery", "Register and discover services"),
    /** Client-side discovery. */
    CLIENT_SIDE_DISCOVERY("Discovery", "Client queries registry directly"),
    /** Server-side discovery. */
    SERVER_SIDE_DISCOVERY("Discovery", "Router queries registry"),
    /** Self-registration. */
    SELF_REGISTRATION("Discovery", "Service registers itself"),
    /** Third-party registration. */
    THIRD_PARTY_REGISTRATION("Discovery", "External registrar registers services"),

    // --- DATA MANAGEMENT ---
    /** Database per service. */
    DATABASE_PER_SERVICE("Data", "Each service owns its database"),
    /** Shared database. */
    SHARED_DATABASE("Data", "Services share database"),
    /** Saga. */
    SAGA("Data", "Manage distributed transactions"),
    /** CQRS. */
    CQRS("Data", "Separate read and write models"),
    /** Event sourcing. */
    EVENT_SOURCING("Data", "Store state as event sequence"),
    /** API composition. */
    API_COMPOSITION("Data", "Aggregate data from multiple services"),

    // --- COMMUNICATION ---
    /** API gateway. */
    API_GATEWAY("Communication", "Single entry point for clients"),
    /** Backend for frontend. */
    BACKEND_FOR_FRONTEND("Communication", "API gateway per client type"),
    /** Service mesh. */
    SERVICE_MESH("Communication", "Infrastructure layer for service communication"),
    /** Sidecar. */
    SIDECAR("Communication", "Deploy helper alongside service"),
    /** Ambassador. */
    AMBASSADOR("Communication", "Helper for connecting to external services"),

    // --- DECOMPOSITION ---
    /** Decompose by business capability. */
    DECOMPOSE_BY_BUSINESS_CAPABILITY("Decomposition", "Services align with business capabilities"),
    /** Decompose by subdomain. */
    DECOMPOSE_BY_SUBDOMAIN("Decomposition", "Services align with DDD subdomains"),
    /** Strangler fig. */
    STRANGLER_FIG("Decomposition", "Gradually replace legacy system"),
    /** Anti-corruption layer. */
    ANTI_CORRUPTION_LAYER("Decomposition", "Isolate from legacy/external systems"),

    // --- OBSERVABILITY ---
    /** Distributed tracing. */
    DISTRIBUTED_TRACING("Observability", "Trace requests across services"),
    /** Log aggregation. */
    LOG_AGGREGATION("Observability", "Centralize logs from all services"),
    /** Exception tracking. */
    EXCEPTION_TRACKING("Observability", "Track and aggregate exceptions"),
    /** Application metrics. */
    APPLICATION_METRICS("Observability", "Collect and expose metrics"),
    /** Audit logging. */
    AUDIT_LOGGING("Observability", "Log significant events"),

    // --- DEPLOYMENT ---
    /** Multiple service instances per host. */
    MULTIPLE_INSTANCES_PER_HOST("Deployment", "Run multiple services per host"),
    /** Single service instance per host. */
    SINGLE_INSTANCE_PER_HOST("Deployment", "One service per host"),
    /** Service instance per VM. */
    SERVICE_PER_VM("Deployment", "One service per virtual machine"),
    /** Service instance per container. */
    SERVICE_PER_CONTAINER("Deployment", "One service per container"),
    /** Serverless deployment. */
    SERVERLESS("Deployment", "Deploy as serverless functions"),
    /** Blue-green deployment. */
    BLUE_GREEN_DEPLOYMENT("Deployment", "Zero-downtime deployment"),
    /** Canary deployment. */
    CANARY_DEPLOYMENT("Deployment", "Gradual rollout to subset"),

    // --- SECURITY ---
    /** Access token. */
    ACCESS_TOKEN("Security", "Token-based authentication"),
    /** External configuration. */
    EXTERNALIZED_CONFIGURATION("Security", "Store config outside service");

    private final String category;
    private final String description;

    /**
     * Microservice descriptor reference constructor.
     * @m3.pattern synexia:iop
     * @m3.algorithm Assigns the supplied category, description references directly to category, description fields in source order, without validation or defensive copying.
     * @m3.dataStructures Constructor parameters and corresponding final descriptor fields: category, description.
     */
    Microservice(String category, String description) {
        this.category = category;
        this.description = description;
    }

    /** PatternKind category.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the stored category reference directly, without copying, validating or performing the named pattern or annotation behavior.
     * @m3.dataStructures One final String reference. ElementType arrays remain shared mutable aliases where present.
     */
    public String category() {
        return category;
    }

    /** PatternKind description.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the stored description reference directly, without copying, validating or performing the named pattern or annotation behavior.
     * @m3.dataStructures One final String reference. ElementType arrays remain shared mutable aliases where present.
     */
    public String description() {
        return description;
    }
}
