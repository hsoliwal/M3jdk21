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
 * 	&#64;code
 * 	&#64;ISpec(value = "Legacy system adapter")
 * 	&#64;AIGlue(type = GlueType.ADAPTER, from = "LegacyOrderSystem", to = "ModernOrderService", transform = Transform.MAP)
 * 	public interface ILegacyOrderAdapter&lt;Self extends ILegacyOrderAdapter&lt;Self>> extends IContext&lt;Self> {
 *
 * 		ModernOrder convert(LegacyOrder legacy);
 * 	}
 *
 * 	&#64;AIGlue(type = GlueType.FACADE, aggregates = {"IUserService", "IProfileService", "IPreferenceService"})
 * 	public interface IUserFacade&lt;Self extends IUserFacade&lt;Self>> extends IContext&lt;Self> {
 *
 * 		UserProfile getCompleteProfile(String userId);
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Glue Hints
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares an integration role, source and target names, aggregation, transformation, direction and failure-handling hints; it connects no component and performs no conversion or error recovery.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIGlue.java}
 * blob {@code 23e9745466a523574776a87c4abe1a1c8cd9cad4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAIGlues.class)
public @interface IAIGlue {

    /**
     * Type of glue code.
     *
     * @return glue type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGlue#type intent: Type of glue code. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code GlueType}; an explicit value is required.
     */
    GlueType type();

    /**
     * Source component/system (for adapters/bridges).
     *
     * @return source name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGlue#from intent: Source component/system (for adapters/bridges). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String from() default "";

    /**
     * Target component/system (for adapters/bridges).
     *
     * @return target name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGlue#to intent: Target component/system (for adapters/bridges). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String to() default "";

    /**
     * Components being aggregated (for facades/composites).
     *
     * @return component names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGlue#aggregates intent: Components being aggregated (for facades/composites). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] aggregates() default {};

    /**
     * Transformation style.
     *
     * @return transform style
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGlue#transform intent: Transformation style. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Transform}; default expression {@code Transform.NONE}.
     */
    Transform transform() default Transform.NONE;

    /**
     * Bidirectional glue (can convert both ways).
     *
     * @return true for bidirectional
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGlue#bidirectional intent: Bidirectional glue (can convert both ways). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean bidirectional() default false;

    /**
     * Error handling strategy for glue.
     *
     * @return error handling
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGlue#errorHandling intent: Error handling strategy for glue. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ErrorHandling}; default expression {@code ErrorHandling.PROPAGATE}.
     */
    ErrorHandling errorHandling() default ErrorHandling.PROPAGATE;

    /** Glue code types. */
    /**
     * GlueType.
     * @author Synexia
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAIGlue.GlueType choices described as GlueType. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Enum containing 17 constants in the existing source order.
     */
    enum GlueType {
        // --- STRUCTURAL GLUE ---

        /** Converts one interface to another (GoF Adapter).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#ADAPTER policy choice: Converts one interface to another (GoF Adapter). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        ADAPTER,

        /** Decouples abstraction from implementation (GoF Bridge).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#BRIDGE policy choice: Decouples abstraction from implementation (GoF Bridge). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        BRIDGE,

        /** Simplified interface to subsystem (GoF Facade).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#FACADE policy choice: Simplified interface to subsystem (GoF Facade). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        FACADE,

        /** Placeholder/surrogate (GoF Proxy).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#PROXY policy choice: Placeholder/surrogate (GoF Proxy). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        PROXY,

        /** Adds behavior without modifying (GoF Decorator).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#DECORATOR policy choice: Adds behavior without modifying (GoF Decorator). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        DECORATOR,

        // --- DATA GLUE ---

        /** Maps between data models (DTO ↔ Entity).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#MAPPER policy choice: Maps between data models (DTO ↔ Entity). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        MAPPER,

        /** Transforms data format (JSON ↔ XML).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#TRANSFORMER policy choice: Transforms data format (JSON ↔ XML). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        TRANSFORMER,

        /** Serializes/deserializes.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#SERIALIZER policy choice: Serializes/deserializes. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        SERIALIZER,

        /** Validates and sanitizes.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#VALIDATOR policy choice: Validates and sanitizes. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        VALIDATOR,

        // --- INTEGRATION GLUE ---

        /** Anti-corruption layer (DDD).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#ACL policy choice: Anti-corruption layer (DDD). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        ACL,

        /** Gateway to external system.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#GATEWAY policy choice: Gateway to external system. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        GATEWAY,

        /** Client for remote service.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#CLIENT policy choice: Client for remote service. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        CLIENT,

        /** Message translator (EIP).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#TRANSLATOR policy choice: Message translator (EIP). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        TRANSLATOR,

        // --- COMPOSITION GLUE ---

        /** Aggregates multiple services.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#AGGREGATOR policy choice: Aggregates multiple services. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        AGGREGATOR,

        /** Orchestrates workflow.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#ORCHESTRATOR policy choice: Orchestrates workflow. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        ORCHESTRATOR,

        /** Routes to appropriate handler.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#ROUTER policy choice: Routes to appropriate handler. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        ROUTER,

        /** Dispatches events/commands.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.GlueType#DISPATCHER policy choice: Dispatches events/commands. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.GlueType; its declared order and identity are preserved.
         */
        DISPATCHER
    }

    /** Transformation styles.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAIGlue.Transform choices described as Transformation styles. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Enum containing 8 constants in the existing source order.
     */
    enum Transform {
        /** No transformation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.Transform#NONE policy choice: No transformation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.Transform; its declared order and identity are preserved.
         */
        NONE,

        /** Simple field mapping.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.Transform#MAP policy choice: Simple field mapping. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.Transform; its declared order and identity are preserved.
         */
        MAP,

        /** Complex transformation with logic.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.Transform#TRANSFORM policy choice: Complex transformation with logic. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.Transform; its declared order and identity are preserved.
         */
        TRANSFORM,

        /** Enrichment (add data).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.Transform#ENRICH policy choice: Enrichment (add data). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.Transform; its declared order and identity are preserved.
         */
        ENRICH,

        /** Filtering (remove data).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.Transform#FILTER policy choice: Filtering (remove data). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.Transform; its declared order and identity are preserved.
         */
        FILTER,

        /** Aggregation (combine data).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.Transform#AGGREGATE policy choice: Aggregation (combine data). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.Transform; its declared order and identity are preserved.
         */
        AGGREGATE,

        /** Splitting (break apart).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.Transform#SPLIT policy choice: Splitting (break apart). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.Transform; its declared order and identity are preserved.
         */
        SPLIT,

        /** Normalization (standardize).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.Transform#NORMALIZE policy choice: Normalization (standardize). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.Transform; its declared order and identity are preserved.
         */
        NORMALIZE
    }

    /** Error handling strategies for glue.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAIGlue.ErrorHandling choices described as Error handling strategies for glue. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Enum containing 7 constants in the existing source order.
     */
    enum ErrorHandling {
        /** Propagate errors as-is.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.ErrorHandling#PROPAGATE policy choice: Propagate errors as-is. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.ErrorHandling; its declared order and identity are preserved.
         */
        PROPAGATE,

        /** Wrap in glue-specific exception.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.ErrorHandling#WRAP policy choice: Wrap in glue-specific exception. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.ErrorHandling; its declared order and identity are preserved.
         */
        WRAP,

        /** Return null/empty on error.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.ErrorHandling#NULL_ON_ERROR policy choice: Return null/empty on error. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.ErrorHandling; its declared order and identity are preserved.
         */
        NULL_ON_ERROR,

        /** Return default value on error.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.ErrorHandling#DEFAULT_ON_ERROR policy choice: Return default value on error. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.ErrorHandling; its declared order and identity are preserved.
         */
        DEFAULT_ON_ERROR,

        /** Log and continue.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.ErrorHandling#LOG_AND_CONTINUE policy choice: Log and continue. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.ErrorHandling; its declared order and identity are preserved.
         */
        LOG_AND_CONTINUE,

        /** Retry with backoff.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.ErrorHandling#RETRY policy choice: Retry with backoff. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.ErrorHandling; its declared order and identity are preserved.
         */
        RETRY,

        /** Circuit breaker.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIGlue.ErrorHandling#CIRCUIT_BREAKER policy choice: Circuit breaker. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIGlue.ErrorHandling; its declared order and identity are preserved.
         */
        CIRCUIT_BREAKER
    }
}
