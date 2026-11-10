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
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.*;

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
 *     &#64;code
 *     &#64;AIStereotype(Role.SERVICE)
 *     public interface IUserService {
 *         // LLM knows: stateless operations, domain logic, no direct DB access
 *     }
 *
 *     &#64;AIStereotype(Role.REPOSITORY)
 *     public interface IUserRepository {
 *         // LLM knows: data access, collection-like API, persistence boundary
 *     }
 *
 *     &#64;AIStereotype(value = Role.CONTROLLER, layer = Layer.PRESENTATION)
 *     public interface IUserController {
 *         // LLM knows: HTTP entry point, validation, no business logic
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Stereotype Hints (Spring-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/package-summary.html">Spring
 *      Stereotypes</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIStereotype.java}
 * blob {@code 22419d70724c9cdb5630cd74545d0d32b7965dd1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIStereotype {

    /**
     * Architectural role (DDD-inspired, Spring-compatible vocabulary).
     *
     * @return role
     */
    Role value();

    /**
     * Architectural layer this component belongs to.
     *
     * @return layer
     */
    Layer layer() default Layer.INFERRED;

    /**
     * Description of purpose (maps to Spring's @Description).
     *
     * @return description
     */
    String description() default "";

    /**
     * Allowed dependencies (role-based).
     *
     * @return allowed dependency roles
     */
    Role[] allowedDependencies() default {};

    /**
     * Forbidden dependencies (role-based).
     *
     * @return forbidden dependency roles
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
      * @author Synexia
     */
    enum Role {
        // --- SPRING CORE STEREOTYPES (exact mapping) ---

        /**
         * Generic component (Spring: @Component). A general-purpose managed
         * component.
         */
        COMPONENT,

        /**
         * Service layer (Spring: @Service). Stateless domain logic,
         * orchestration, business rules.
         * <p>
         * DDD: "An operation offered as an interface that stands alone in the
         * model, with no encapsulated state." (Evans, 2003)
         * </p>
         */
        SERVICE,

        /**
         * Repository layer (Spring: @Repository). Data access, persistence
         * boundary, collection-like API.
         * <p>
         * DDD: "A mechanism for encapsulating storage, retrieval, and search
         * behavior which emulates a collection." (Evans, 2003)
         * </p>
         */
        REPOSITORY,

        /**
         * Controller layer (Spring: @Controller). Presentation boundary,
         * HTTP/REST entry points.
         */
        CONTROLLER,

        /**
         * REST controller (Spring: @RestController). Controller + ResponseBody
         * semantics.
         */
        REST_CONTROLLER,

        // --- DDD TACTICAL PATTERNS ---

        /** Entity (DDD). Has identity, lifecycle, mutable state. */
        ENTITY,

        /** Value Object (DDD). No identity, immutable, equality by value. */
        VALUE_OBJECT,

        /** Aggregate Root (DDD). Consistency boundary, owns child entities. */
        AGGREGATE_ROOT,

        /** Domain Event (DDD). Something that happened in the domain. */
        DOMAIN_EVENT,

        /** Factory (DDD/GoF). Creates complex objects/aggregates. */
        FACTORY,

        // --- ARCHITECTURAL COMPONENTS ---

        /**
         * Configuration holder. Maps to Spring's @Configuration (but no runtime
         * wiring).
         */
        CONFIGURATION,

        /**
         * Infrastructure component. Technical concerns: logging, metrics,
         * security.
         */
        INFRASTRUCTURE,

        /** Gateway (integration). External system boundary. */
        GATEWAY,

        /** Adapter (hexagonal). Port implementation, translation layer. */
        ADAPTER,

        /** Port (hexagonal). Interface defining application boundary. */
        PORT,

        /** Mapper/Translator. Object transformation between layers. */
        MAPPER,

        /** Validator. Input validation, constraint checking. */
        VALIDATOR,

        /** Event Handler. Reacts to domain/integration events. */
        EVENT_HANDLER,

        /** Command Handler (CQRS). Handles write operations. */
        COMMAND_HANDLER,

        /** Query Handler (CQRS). Handles read operations. */
        QUERY_HANDLER
    }

    /** Architectural layers (Clean Architecture / Onion). */
    enum Layer {
        /** Infer from role. */
        INFERRED,

        /** Presentation / UI / API layer. */
        PRESENTATION,

        /** Application / Use Case layer. */
        APPLICATION,

        /** Domain / Business Logic layer. */
        DOMAIN,

        /** Infrastructure / Framework layer. */
        INFRASTRUCTURE,

        /** Integration / Anti-corruption layer. */
        INTEGRATION
    }
}
