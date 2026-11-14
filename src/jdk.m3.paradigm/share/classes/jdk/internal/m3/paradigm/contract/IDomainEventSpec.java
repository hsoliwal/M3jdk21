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

import jdk.internal.m3.paradigm.annotation.LLM;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IDomainEventSpec - Domain event specification for AI code generation.
 *
 * <h2>THE DOMAIN EVENT SPEC</h2>
 *
 * <pre>
 * Domain events in the IOP context:
 *   - Immutable records with validation
 *   - Links to prior events via eventId references
 *   - Attribution tracking (which component/model caused it)
 *   - Confidence scoring for AI-suggested events
 *   - Schema versioning for evolution
 *
 * Key principle: "LLMs are editors, not authors"
 *   - CPU pipeline creates initial truth (e.g., GlyphAssigned)
 *   - LLMs can only SUGGEST refinements (e.g., GlyphRefined)
 *   - Never overwrite, always append with new event
 *
 * Event patterns:
 *   - Sourced: CPU pipeline produces (truth)
 *   - Refined: LLM suggests refinement (suggestion)
 *   - Derived: Computed from other events (derived)
 *   - Consolidated: Aggregated from multiple events
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	// CPU-sourced event (authoritative)
 * 	&#64;IDomainEventSpec(name = "GlyphAssigned", source = EventSource.CPU, aggregate = "TextProcessing", description = "CPU pipeline assigns glyph via rule-based decision")
 * 	public record GlyphAssigned(UUID eventId, Instant occurredAt, UUID lineEventId, String glyphSequence,
 * 			String rulePath, String schemaVersion) implements IDomainEvent {
 * 	}
 *
 * 	// LLM-suggested refinement (non-authoritative)
 * 	&#64;IDomainEventSpec(name = "GlyphRefined", source = EventSource.LLM, aggregate = "TextProcessing", priorEvent = GlyphAssigned.class, description = "LLM suggests glyph refinement - editors not authors")
 * 	public record GlyphRefined(UUID eventId, Instant occurredAt, UUID priorGlyphEventId,
 * 			String suggestedGlyphSequence, String modelId, double confidence,
 * 			String rationale) implements IDomainEvent {
 * 	}
 *
 * 	// Derived event (computed)
 * 	&#64;IDomainEventSpec(name = "SemanticDerived", source = EventSource.DERIVED, aggregate = "TextProcessing", priorEvent = LineIngested.class)
 * 	public record SemanticDerived(UUID eventId, UUID lineEventId, float[] vector,
 * 			String vectorSource) implements IDomainEvent {
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.1 - Domain Event Specification
 * @see IEventSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares event identity, reported origin, causal/schema fields, validation, storage and factory hints; it authenticates no event origin and grants no approval.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IDomainEventSpec.java}
 * blob {@code 6ecdb1ba1361d910d23c14e2cb76eaf873115eb0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IDomainEventSpec {

    // --- IDENTITY ---

    /**
     * Event name (unique within aggregate).
     *
     * @return event name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#name intent: Event name (unique within aggregate). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String name();

    /**
     * Event description.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#description intent: Event description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String description() default "";

    /**
     * Specification value (alternative to description).
     *
     * @return value
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#value intent: Specification value (alternative to description). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- AGGREGATE ---

    /**
     * Aggregate root this event belongs to (DDD).
     *
     * @return aggregate name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#aggregate intent: Aggregate root this event belongs to (DDD). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String aggregate() default "";

    /**
     * Bounded context.
     *
     * @return context name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#boundedContext intent: Bounded context. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String boundedContext() default "";

    // --- EVENT SOURCE ---

    /**
     * Source of this event.
     *
     * @return source
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#source intent: Source of this event. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code EventSource}; default expression {@code EventSource.CPU}.
     */
    EventSource source() default EventSource.CPU;

    /** Event source types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IDomainEventSpec.EventSource choices described as Event source types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum EventSource {
        /** CPU pipeline produced (authoritative truth).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDomainEventSpec.EventSource#CPU policy choice: Reported CPU-pipeline event origin; a token does not prove authoritative truth. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDomainEventSpec.EventSource; its declared order and identity are preserved.
         */
        CPU,

        /** LLM suggested (requires human/system approval).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDomainEventSpec.EventSource#LLM policy choice: Reported model-suggested event origin; this token supplies no approval. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDomainEventSpec.EventSource; its declared order and identity are preserved.
         */
        LLM,

        /** Derived/computed from other events.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDomainEventSpec.EventSource#DERIVED policy choice: Derived/computed from other events. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDomainEventSpec.EventSource; its declared order and identity are preserved.
         */
        DERIVED,

        /** External system produced.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDomainEventSpec.EventSource#EXTERNAL policy choice: External system produced. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDomainEventSpec.EventSource; its declared order and identity are preserved.
         */
        EXTERNAL,

        /** Human action produced.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDomainEventSpec.EventSource#HUMAN policy choice: Human action produced. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDomainEventSpec.EventSource; its declared order and identity are preserved.
         */
        HUMAN
    }

    // --- LINKING ---

    /**
     * Prior event this event links to (causation).
     *
     * @return prior event class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#priorEvent intent: Prior event this event links to (causation). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> priorEvent() default Void.class;

    /**
     * Correlation ID field name.
     *
     * @return field name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#correlationIdField intent: Correlation ID field name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String correlationIdField() default "";

    /**
     * Causation ID field name.
     *
     * @return field name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#causationIdField intent: Causation ID field name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String causationIdField() default "";

    // --- LLM ATTRIBUTION (for LLM-sourced events) ---

    /**
     * Model ID field name (for LLM events).
     *
     * @return field name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#modelIdField intent: Model ID field name (for LLM events). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "modelId"}.
     */
    String modelIdField() default "modelId";

    /**
     * Confidence score field name (for LLM events).
     *
     * @return field name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#confidenceField intent: Confidence score field name (for LLM events). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "confidence"}.
     */
    String confidenceField() default "confidence";

    /**
     * Minimum confidence threshold for auto-approval.
     *
     * @return threshold
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#autoApproveThreshold intent: Requested confidence threshold for a consuming approval policy; the element itself approves no action. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.95}.
     */
    double autoApproveThreshold() default 0.95;

    // --- SCHEMA ---

    /**
     * Schema version.
     *
     * @return version string
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#schemaVersion intent: Schema version. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "1.0.0"}.
     */
    String schemaVersion() default "1.0.0";

    /**
     * Schema version field name.
     *
     * @return field name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#schemaVersionField intent: Schema version field name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "schemaVersion"}.
     */
    String schemaVersionField() default "schemaVersion";

    /**
     * Upcaster class for schema evolution.
     *
     * @return upcaster class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#upcaster intent: Upcaster class for schema evolution. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> upcaster() default Void.class;

    // --- VALIDATION ---

    /**
     * Required fields (must be non-null).
     *
     * @return field names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#requiredFields intent: Required fields (must be non-null). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] requiredFields() default {};

    /**
     * Validator class.
     *
     * @return validator class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#validator intent: Validator class. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> validator() default Void.class;

    // --- STORAGE ---

    /**
     * Event store topic/channel.
     *
     * @return topic name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#topic intent: Event store topic/channel. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String topic() default "";

    /**
     * Partition key expression.
     *
     * @return partition key
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#partitionKey intent: Partition key expression. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String partitionKey() default "";

    /**
     * Time-to-live in days (0 = infinite).
     *
     * @return TTL days
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#ttlDays intent: Time-to-live in days (0 = infinite). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int ttlDays() default 0;

    // --- NESTED ANNOTATIONS ---

    /** Field-level validation.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares requiredness, numeric bounds, pattern and validator-name hints for a field; it validates no value.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.FIELD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Field {

        /** Field is required (non-null).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IDomainEventSpec.Field#required intent: Field is required (non-null). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean required() default true;

        /** Minimum value (for numbers).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IDomainEventSpec.Field#min intent: Requested minimum numeric value; the preserved Double.MIN_VALUE default is the smallest positive nonzero double, not a negative lower bound. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code Double.MIN_VALUE}.
         */
        double min() default Double.MIN_VALUE;

        /** Maximum value (for numbers).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IDomainEventSpec.Field#max intent: Maximum value (for numbers). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code Double.MAX_VALUE}.
         */
        double max() default Double.MAX_VALUE;

        /** PatternKind for strings.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IDomainEventSpec.Field#pattern intent: PatternKind for strings. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String pattern() default "";

        /** Custom validator method.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IDomainEventSpec.Field#validator intent: Custom validator method. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String validator() default "";
    }

    /** Event handler subscription.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares handled event types, ordering and asynchronous preference for a method; it creates no subscription.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Handler {

        /** Event types to handle.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IDomainEventSpec.Handler#events intent: Event types to handle. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Class<?>[]}; an explicit value is required.
         */
        Class<?>[] events();

        /** Ordering (lower = earlier).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IDomainEventSpec.Handler#order intent: Ordering (lower = earlier). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 100}.
         */
        int order() default 100;

        /** Async handling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IDomainEventSpec.Handler#async intent: Async handling. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean async() default false;
    }

    /** Factory method for event creation.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares event factory naming, identifier and timestamp-generation hints; it invokes no factory and generates no identifier.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Factory {

        /** Factory method name pattern.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IDomainEventSpec.Factory#name intent: Factory method name pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "from*"}.
         */
        String name() default "from*";

        /** Auto-generate eventId.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IDomainEventSpec.Factory#autoEventId intent: Auto-generate eventId. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean autoEventId() default true;

        /** Auto-set occurredAt to now.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IDomainEventSpec.Factory#autoTimestamp intent: Auto-set occurredAt to now. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean autoTimestamp() default true;
    }

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.HAIKU}.
     */
    Model.ModelType model() default Model.ModelType.HAIKU;

    /**
     * Stability level.
     *
     * @return stability
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDomainEventSpec#stability intent: Stability level. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Stability.Level}; default expression {@code Stability.Level.STABLE}.
     */
    Stability.Level stability() default Stability.Level.STABLE;
}
