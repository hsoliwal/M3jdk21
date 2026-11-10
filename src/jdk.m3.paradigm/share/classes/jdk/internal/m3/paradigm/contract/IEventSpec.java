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

import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IEventSpec - Event specification for AI code generation.
 *
 * <h2>Event Patterns</h2>
 *
 * <pre>
 * Event patterns in Java:
 *   - Spring ApplicationEvent
 *   - CDI Events (@Observes)
 *   - Flow.Publisher (reactive)
 *   - JMS / Kafka / RabbitMQ
 *   - Domain Events (DDD)
 *   - CQRS Event Sourcing
 *
 * &#64;IEventSpec specifies event pub/sub in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IUserService {
 *
 *     // Publish event after action
 *     &#64;IEventSpec(
 *         publish = UserCreatedEvent.class,
 *         channel = "users",
 *         async = true
 *     )
 *     User createUser(CreateUserRequest req);
 *
 *     // Subscribe to events
 *     &#64;IEventSpec(
 *         subscribe = UserDeletedEvent.class,
 *         handler = "handleUserDeleted"
 *     )
 *     void onUserDeleted(UserDeletedEvent event);
 * }
 *
 * // Event class definition
 * &#64;IEventSpec.Event(name = "UserCreated", aggregate = "User", version = 1)
 * public record UserCreatedEvent(String userId, String email, Instant createdAt) {
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Event Specification
 * @see IListenerSpec
 * @see IStateSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares event publication/subscription types, channel, delivery, ordering, filtering and retry hints; it publishes no event and establishes no delivery guarantee.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IEventSpec.java}
 * blob {@code 2a63690a9b3e150fe91803571b2f8bde3f27fe47} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IEventSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined event pattern.
     *
     * @return option
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#option intent: Predefined event pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common event patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IEventSpec.Option choices described as Common event patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IEventSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IEventSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Publish event.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IEventSpec.Option#PUBLISH policy choice: Publish event. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IEventSpec.Option; its declared order and identity are preserved.
         */
        PUBLISH,

        /** Subscribe to event.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IEventSpec.Option#SUBSCRIBE policy choice: Subscribe to event. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IEventSpec.Option; its declared order and identity are preserved.
         */
        SUBSCRIBE,

        /** Domain event (DDD).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IEventSpec.Option#DOMAIN_EVENT policy choice: Domain event (DDD). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IEventSpec.Option; its declared order and identity are preserved.
         */
        DOMAIN_EVENT,

        /** Integration event.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IEventSpec.Option#INTEGRATION_EVENT policy choice: Integration event. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IEventSpec.Option; its declared order and identity are preserved.
         */
        INTEGRATION_EVENT,

        /** Event sourcing.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IEventSpec.Option#EVENT_SOURCING policy choice: Event sourcing. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IEventSpec.Option; its declared order and identity are preserved.
         */
        EVENT_SOURCING
    }

    // --- PUBLISH ---

    /**
     * Event type to publish.
     *
     * @return event class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#publish intent: Event type to publish. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> publish() default Void.class;

    /**
     * Event types to publish (multiple).
     *
     * @return event classes
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#publishes intent: Event types to publish (multiple). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>[]}; default expression {@code {}}.
     */
    Class<?>[] publishes() default {};

    /**
     * Channel/topic to publish to.
     *
     * @return channel name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#channel intent: Channel/topic to publish to. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String channel() default "";

    // --- SUBSCRIBE ---

    /**
     * Event type to subscribe to.
     *
     * @return event class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#subscribe intent: Event type to subscribe to. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> subscribe() default Void.class;

    /**
     * Event types to subscribe to (multiple).
     *
     * @return event classes
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#subscribes intent: Event types to subscribe to (multiple). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>[]}; default expression {@code {}}.
     */
    Class<?>[] subscribes() default {};

    /**
     * Handler method name.
     *
     * @return handler name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#handler intent: Handler method name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String handler() default "";

    // --- DELIVERY ---

    /**
     * Whether to publish/subscribe async.
     *
     * @return true for async
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#async intent: Whether to publish/subscribe async. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean async() default false;

    /**
     * Delivery guarantee.
     *
     * @return delivery mode
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#delivery intent: Delivery guarantee. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code DeliveryMode}; default expression {@code DeliveryMode.AT_LEAST_ONCE}.
     */
    DeliveryMode delivery() default DeliveryMode.AT_LEAST_ONCE;

    /** Delivery modes.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IEventSpec.DeliveryMode choices described as Delivery modes. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum DeliveryMode {
        /** Fire and forget.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IEventSpec.DeliveryMode#FIRE_AND_FORGET policy choice: Fire and forget. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IEventSpec.DeliveryMode; its declared order and identity are preserved.
         */
        FIRE_AND_FORGET,

        /** At most once.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IEventSpec.DeliveryMode#AT_MOST_ONCE policy choice: At most once. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IEventSpec.DeliveryMode; its declared order and identity are preserved.
         */
        AT_MOST_ONCE,

        /** At least once (may duplicate).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IEventSpec.DeliveryMode#AT_LEAST_ONCE policy choice: At least once (may duplicate). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IEventSpec.DeliveryMode; its declared order and identity are preserved.
         */
        AT_LEAST_ONCE,

        /** Exactly once.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IEventSpec.DeliveryMode#EXACTLY_ONCE policy choice: Exactly once. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IEventSpec.DeliveryMode; its declared order and identity are preserved.
         */
        EXACTLY_ONCE
    }

    // --- ORDERING ---

    /**
     * Event ordering key.
     *
     * @return ordering key expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#orderingKey intent: Event ordering key. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String orderingKey() default "";

    /**
     * Subscriber priority (lower = earlier).
     *
     * @return priority
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#priority intent: Subscriber priority (lower = earlier). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 100}.
     */
    int priority() default 100;

    // --- FILTERING ---

    /**
     * Filter condition (SpEL).
     *
     * @return condition
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#filter intent: Filter condition (SpEL). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String filter() default "";

    // --- RETRY ---

    /**
     * Max delivery retries.
     *
     * @return retries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#maxRetries intent: Max delivery retries. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3}.
     */
    int maxRetries() default 3;

    /**
     * Dead letter channel for failed events.
     *
     * @return dead letter channel
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#deadLetterChannel intent: Dead letter channel for failed events. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String deadLetterChannel() default "";

    // --- EVENT DEFINITION (nested) ---

    /** Event definition annotation.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares event name, aggregate, version and description on a type; it creates or publishes no event.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @interface Event {

        /** Event name.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IEventSpec.Event#name intent: Event name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String name();

        /** Aggregate type (DDD).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IEventSpec.Event#aggregate intent: Aggregate type (DDD). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String aggregate() default "";

        /** Event version.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IEventSpec.Event#version intent: Event version. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1}.
         */
        int version() default 1;

        /** Event description.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IEventSpec.Event#description intent: Event description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String description() default "";
    }

    // --- DESCRIPTION ---

    /**
     * Event specification description.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#value intent: Event specification description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IEventSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
