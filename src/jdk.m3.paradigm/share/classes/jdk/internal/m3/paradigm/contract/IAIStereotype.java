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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIStereotype - AnOP annotation for architectural role hints.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Component/@Service/@Repository → runtime container wiring ║
 * ║ AnOP: @AIStereotype → compile-time role declaration ║
 * ║ ║
 * ║ Spring says: "I will wire this for you at runtime" ║
 * ║ AnOP says: "This code plays this architectural role" ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>The Invariant</h2> <blockquote> Spring annotations describe how a
 * container should behave.<br>
 * AnOP annotations describe how code is allowed to change. </blockquote>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;AIStereotype(Role.SERVICE)
 * 	public interface IUserService&lt;Self extends IUserService&lt;Self>> extends IContext&lt;Self> {
 * 		// LLM knows: stateless operations, domain logic, no direct DB access
 * 	}
 *
 * 	&#64;AIStereotype(Role.REPOSITORY)
 * 	public interface IUserRepository&lt;Self extends IUserRepository&lt;Self>> extends IContext&lt;Self> {
 * 		// LLM knows: data access, collection-like API, persistence boundary
 * 	}
 *
 * 	&#64;AIStereotype(value = Role.CONTROLLER, layer = Layer.PRESENTATION)
 * 	public interface IUserController&lt;Self extends IUserController&lt;Self>> extends IContext&lt;Self> {
 * 		// LLM knows: HTTP entry point, validation, no business logic
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Stereotype Hints (Spring-inspired)
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares architectural role, layer and allowed/forbidden dependency roles; it creates no managed bean and enforces no dependency boundary.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIStereotype.java}
 * blob {@code bb18cc13273e7dbfcaac8c51b3f21bdf4217ccb4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIStereotype {

    /**
     * Architectural role (DDD-inspired, Spring-compatible vocabulary).
     *
     * @return role
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIStereotype#value intent: Architectural role (DDD-inspired, Spring-compatible vocabulary). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Role}; an explicit value is required.
     */
    Role value();

    /**
     * Architectural layer this component belongs to.
     *
     * @return layer
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIStereotype#layer intent: Architectural layer this component belongs to. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Layer}; default expression {@code Layer.INFERRED}.
     */
    Layer layer() default Layer.INFERRED;

    /**
     * Description of purpose (maps to Spring's @Description).
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIStereotype#description intent: Description of purpose (maps to Spring's @Description). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String description() default "";

    /**
     * Allowed dependencies (role-based).
     *
     * @return allowed dependency roles
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIStereotype#allowedDependencies intent: Allowed dependencies (role-based). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Role[]}; default expression {@code {}}.
     */
    Role[] allowedDependencies() default {};

    /**
     * Forbidden dependencies (role-based).
     *
     * @return forbidden dependency roles
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIStereotype#forbiddenDependencies intent: Forbidden dependencies (role-based). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Role[]}; default expression {@code {}}.
     */
    Role[] forbiddenDependencies() default {};

    // --- ENUMS - Spring-inspired vocabulary, no runtime authority ---

    /**
     * Architectural roles (DDD + Spring stereotypes).
     *
     * <p>
     * These map conceptually to Spring's @Component hierarchy:
     * </p>
     * <ul>
     * <li>COMPONENT → @Component (generic)</li>
     * <li>SERVICE → @Service (domain logic)</li>
     * <li>REPOSITORY → @Repository (data access)</li>
     * <li>CONTROLLER → @Controller (presentation)</li>
     * </ul>
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAIStereotype.Role choices described as Architectural-role vocabulary used by the stereotype hint. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Enum containing 20 constants in the existing source order.
     */
    enum Role {
        // --- SPRING CORE STEREOTYPES (exact mapping) ---

        /**
         * Generic component (Spring: @Component). A general-purpose managed component.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#COMPONENT policy choice: Generic component (Spring: @Component). A general-purpose managed component. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        COMPONENT,

        /**
         * Service layer (Spring: @Service). Stateless domain logic, orchestration,
         * business rules.
         * <p>
         * DDD: "An operation offered as an interface that stands alone in the model,
         * with no encapsulated state." (Evans, 2003)
         * </p>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#SERVICE policy choice: Requested stateless domain-service or orchestration role. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        SERVICE,

        /**
         * Repository layer (Spring: @Repository). Data access, persistence boundary,
         * collection-like API.
         * <p>
         * DDD: "A mechanism for encapsulating storage, retrieval, and search behavior
         * which emulates a collection." (Evans, 2003)
         * </p>
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#REPOSITORY policy choice: Requested data-access and persistence-boundary role. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        REPOSITORY,

        /**
         * Controller layer (Spring: @Controller). Presentation boundary, HTTP/REST
         * entry points.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#CONTROLLER policy choice: Controller layer (Spring: @Controller). Presentation boundary, HTTP/REST entry points. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        CONTROLLER,

        /**
         * REST controller (Spring: @RestController). Controller + ResponseBody
         * semantics.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#REST_CONTROLLER policy choice: REST controller (Spring: @RestController). Controller + ResponseBody semantics. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        REST_CONTROLLER,

        // --- DDD TACTICAL PATTERNS ---

        /** Entity (DDD). Has identity, lifecycle, mutable state.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#ENTITY policy choice: Entity (DDD). Has identity, lifecycle, mutable state. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        ENTITY,

        /** Value Object (DDD). No identity, immutable, equality by value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#VALUE_OBJECT policy choice: Value Object (DDD). No identity, immutable, equality by value. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        VALUE_OBJECT,

        /** Aggregate Root (DDD). Consistency boundary, owns child entities.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#AGGREGATE_ROOT policy choice: Aggregate Root (DDD). Consistency boundary, owns child entities. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        AGGREGATE_ROOT,

        /** Domain Event (DDD). Something that happened in the domain.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#DOMAIN_EVENT policy choice: Domain Event (DDD). Something that happened in the domain. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        DOMAIN_EVENT,

        /** Factory (DDD/GoF). Creates complex objects/aggregates.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#FACTORY policy choice: Factory (DDD/GoF). Creates complex objects/aggregates. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        FACTORY,

        // --- ARCHITECTURAL COMPONENTS ---

        /**
         * Configuration holder. Maps to Spring's @Configuration (but no runtime
         * wiring).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#CONFIGURATION policy choice: Configuration holder. Maps to Spring's @Configuration (but no runtime wiring). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        CONFIGURATION,

        /** Infrastructure component. Technical concerns: logging, metrics, security.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#INFRASTRUCTURE policy choice: Infrastructure component. Technical concerns: logging, metrics, security. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        INFRASTRUCTURE,

        /** Gateway (integration). External system boundary.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#GATEWAY policy choice: Gateway (integration). External system boundary. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        GATEWAY,

        /** Adapter (hexagonal). Port implementation, translation layer.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#ADAPTER policy choice: Adapter (hexagonal). Port implementation, translation layer. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        ADAPTER,

        /** Port (hexagonal). Interface defining application boundary.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#PORT policy choice: Port (hexagonal). Interface defining application boundary. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        PORT,

        /** Mapper/Translator. Object transformation between layers.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#MAPPER policy choice: Mapper/Translator. Object transformation between layers. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        MAPPER,

        /** Validator. Input validation, constraint checking.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#VALIDATOR policy choice: Validator. Input validation, constraint checking. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        VALIDATOR,

        /** Event Handler. Reacts to domain/integration events.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#EVENT_HANDLER policy choice: Event Handler. Reacts to domain/integration events. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        EVENT_HANDLER,

        /** Command Handler (CQRS). Handles write operations.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#COMMAND_HANDLER policy choice: Command Handler (CQRS). Handles write operations. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        COMMAND_HANDLER,

        /** Query Handler (CQRS). Handles read operations.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Role#QUERY_HANDLER policy choice: Query Handler (CQRS). Handles read operations. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Role; its declared order and identity are preserved.
         */
        QUERY_HANDLER
    }

    /** Architectural layers (Clean Architecture / Onion).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAIStereotype.Layer choices described as Architectural layers (Clean Architecture / Onion). Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Enum containing 6 constants in the existing source order.
     */
    enum Layer {
        /** Infer from role.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Layer#INFERRED policy choice: Infer from role. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Layer; its declared order and identity are preserved.
         */
        INFERRED,

        /** Presentation / UI / API layer.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Layer#PRESENTATION policy choice: Presentation / UI / API layer. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Layer; its declared order and identity are preserved.
         */
        PRESENTATION,

        /** Application / Use Case layer.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Layer#APPLICATION policy choice: Application / Use Case layer. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Layer; its declared order and identity are preserved.
         */
        APPLICATION,

        /** Domain / Business Logic layer.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Layer#DOMAIN policy choice: Domain / Business Logic layer. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Layer; its declared order and identity are preserved.
         */
        DOMAIN,

        /** Infrastructure / Framework layer.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Layer#INFRASTRUCTURE policy choice: Infrastructure / Framework layer. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Layer; its declared order and identity are preserved.
         */
        INFRASTRUCTURE,

        /** Integration / Anti-corruption layer.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIStereotype.Layer#INTEGRATION policy choice: Integration / Anti-corruption layer. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIStereotype.Layer; its declared order and identity are preserved.
         */
        INTEGRATION
    }
}
