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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIGlue - AnOP annotation for integration/glue code hints.
 *
 * <h2>AnOP Advisory: Glue Code Hints</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Glue code connects components that don't know about each other.          ║
 * ║  This annotation hints HOW components should be wired.                    ║
 * ║  Glue is ADAPTER logic, not business logic.                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;ISpec(value = "Legacy system adapter")
 *     &#64;AIGlue(type = GlueType.ADAPTER, from = "LegacyOrderSystem", to = "ModernOrderService", transform = Transform.MAP)
 *     public interface ILegacyOrderAdapter {
 *
 *         ModernOrder convert(LegacyOrder legacy);
 *     }
 *
 *     &#64;AIGlue(type = GlueType.FACADE, aggregates = {"IUserService",
 *             "IProfileService", "IPreferenceService"})
 *     public interface IUserFacade {
 *
 *         UserProfile getCompleteProfile(String userId);
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Glue Hints
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIGlue.java}
 * blob {@code e89b66e7317c4629c00c036d99f6b5a5031d6675} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIGlues.class)
public @interface AIGlue {

    /**
     * Type of glue code.
     *
     * @return glue type
     */
    GlueType type();

    /**
     * Source component/system (for adapters/bridges).
     *
     * @return source name
     */
    String from() default "";

    /**
     * Target component/system (for adapters/bridges).
     *
     * @return target name
     */
    String to() default "";

    /**
     * Components being aggregated (for facades/composites).
     *
     * @return component names
     */
    String[] aggregates() default {};

    /**
     * Transformation style.
     *
     * @return transform style
     */
    Transform transform() default Transform.NONE;

    /**
     * Bidirectional glue (can convert both ways).
     *
     * @return true for bidirectional
     */
    boolean bidirectional() default false;

    /**
     * Error handling strategy for glue.
     *
     * @return error handling
     */
    ErrorHandling errorHandling() default ErrorHandling.PROPAGATE;

    /** Glue code types. */
    enum GlueType {
        // --- STRUCTURAL GLUE ---

        /** Converts one interface to another (GoF Adapter). */
        ADAPTER,

        /** Decouples abstraction from implementation (GoF Bridge). */
        BRIDGE,

        /** Simplified interface to subsystem (GoF Facade). */
        FACADE,

        /** Placeholder/surrogate (GoF Proxy). */
        PROXY,

        /** Adds behavior without modifying (GoF Decorator). */
        DECORATOR,

        // --- DATA GLUE ---

        /** Maps between data models (DTO ↔ Entity). */
        MAPPER,

        /** Transforms data format (JSON ↔ XML). */
        TRANSFORMER,

        /** Serializes/deserializes. */
        SERIALIZER,

        /** Validates and sanitizes. */
        VALIDATOR,

        // --- INTEGRATION GLUE ---

        /** Anti-corruption layer (DDD). */
        ACL,

        /** Gateway to external system. */
        GATEWAY,

        /** Client for remote service. */
        CLIENT,

        /** Message translator (EIP). */
        TRANSLATOR,

        // --- COMPOSITION GLUE ---

        /** Aggregates multiple services. */
        AGGREGATOR,

        /** Orchestrates workflow. */
        ORCHESTRATOR,

        /** Routes to appropriate handler. */
        ROUTER,

        /** Dispatches events/commands. */
        DISPATCHER
    }

    /** Transformation styles. */
    enum Transform {
        /** No transformation. */
        NONE,

        /** Simple field mapping. */
        MAP,

        /** Complex transformation with logic. */
        TRANSFORM,

        /** Enrichment (add data). */
        ENRICH,

        /** Filtering (remove data). */
        FILTER,

        /** Aggregation (combine data). */
        AGGREGATE,

        /** Splitting (break apart). */
        SPLIT,

        /** Normalization (standardize). */
        NORMALIZE
    }

    /** Error handling strategies for glue. */
    enum ErrorHandling {
        /** Propagate errors as-is. */
        PROPAGATE,

        /** Wrap in glue-specific exception. */
        WRAP,

        /** Return null/empty on error. */
        NULL_ON_ERROR,

        /** Return default value on error. */
        DEFAULT_ON_ERROR,

        /** Log and continue. */
        LOG_AND_CONTINUE,

        /** Retry with backoff. */
        RETRY,

        /** Circuit breaker. */
        CIRCUIT_BREAKER
    }
}
