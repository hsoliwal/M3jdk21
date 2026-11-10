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
 * AIPattern - AnOP annotation for design pattern hints.
 *
 * <h2>AnOP Advisory: PatternKind Hints</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ Patterns are HINTS, not requirements. ║
 * ║ Agent uses pattern knowledge to guide implementation. ║
 * ║ Patterns absorbed: GoF (23) + EIP (83) + Microservice (41) + DAG (40) ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	// Singleton pattern
 * 	&#64;ISpec(value = "Application configuration")
 * 	&#64;AIPattern(gof = GoF.SINGLETON, scope = Scope.APPLICATION)
 * 	public interface IAppConfig {
 *
 * 		String get(String key);
 * 	}
 *
 * 	// Factory + Repository patterns
 * 	&#64;ISpec(value = "User repository")
 * 	&#64;AIPattern(gof = {GoF.FACTORY_METHOD, GoF.REPOSITORY})
 * 	public interface IUserRepository {
 *
 * 		User findById(String id);
 * 	}
 *
 * 	// EIP Content-Based Router
 * 	&#64;ISpec(value = "Message router")
 * 	&#64;AIPattern(eip = EIP.CONTENT_BASED_ROUTER)
 * 	public interface IMessageRouter {
 *
 * 		void route(Message message);
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Advisory Layer
 * @see IopParadigm
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares design-pattern and lifecycle-scope implementation preferences in the separate AI-prefixed API; this annotation records guidance rather than implementing or verifying the requested behavior.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD}; preserves its declared element and nested-token schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIPattern.java}
 * blob {@code f805e471c9096992c6fff8172000e811724d29f3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIPatterns.class)
public @interface AIPattern {

    // --- GOF PATTERNS (Gang of Four - 23) ---

    /**
     * GoF design patterns to apply.
     *
     * @return GoF patterns
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries AIPattern#gof advisory metadata for GoF design patterns to apply. The element itself neither executes nor enforces that request.
     * @m3.dataStructures GoF[] annotation element; exact default expression {}.
     */
    GoF[] gof() default {};

    // --- EIP PATTERNS (Enterprise Integration - 83) ---

    /**
     * Enterprise Integration Patterns to apply.
     *
     * @return EIP patterns
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries AIPattern#eip advisory metadata for Enterprise Integration Patterns to apply. The element itself neither executes nor enforces that request.
     * @m3.dataStructures EIP[] annotation element; exact default expression {}.
     */
    EIP[] eip() default {};

    // --- MICROSERVICE PATTERNS (41) ---

    /**
     * Microservice patterns to apply.
     *
     * @return Microservice patterns
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries AIPattern#microservice advisory metadata for Microservice patterns to apply. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Microservice[] annotation element; exact default expression {}.
     */
    Microservice[] microservice() default {};

    // --- DAG PATTERNS (40) ---

    /**
     * DAG flow patterns to apply.
     *
     * @return DAG patterns
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries AIPattern#dag advisory metadata for DAG flow patterns to apply. The element itself neither executes nor enforces that request.
     * @m3.dataStructures DAG[] annotation element; exact default expression {}.
     */
    DAG[] dag() default {};

    // --- SCOPE (Lifecycle/DI Scopes) ---

    /**
     * Instance scope hint.
     *
     * @return scope
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries AIPattern#scope advisory metadata for Instance scope hint. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Scope annotation element; exact default expression Scope.DEFAULT.
     */
    Scope scope() default Scope.DEFAULT;

    // --- GOF ENUM (23 patterns) ---

    /** Gang of Four design patterns. */
    /**
     * GoF.
     * @author Synexia
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 25 symbolic choices for GoF. @author Synexia within AIPattern advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested AIPattern.GoF enum with 25 source-declared constants in existing order.
     */
    enum GoF {
        // Creational (5)
        /** Single instance per scope.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#SINGLETON token within GoF. @author Synexia: Single instance per scope. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SINGLETON,
        /** Create object without specifying exact class.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#FACTORY_METHOD token within GoF. @author Synexia: Create object without specifying exact class. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FACTORY_METHOD,
        /** Create families of related objects.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#ABSTRACT_FACTORY token within GoF. @author Synexia: Create families of related objects. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ABSTRACT_FACTORY,
        /** Construct complex objects step by step.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#BUILDER token within GoF. @author Synexia: Construct complex objects step by step. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BUILDER,
        /** Clone existing objects.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#PROTOTYPE token within GoF. @author Synexia: Clone existing objects. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PROTOTYPE,

        // Structural (7)
        /** Convert interface to another interface.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#ADAPTER token within GoF. @author Synexia: Convert interface to another interface. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ADAPTER,
        /** Separate abstraction from implementation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#BRIDGE token within GoF. @author Synexia: Separate abstraction from implementation. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BRIDGE,
        /** Tree structure of objects.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#COMPOSITE token within GoF. @author Synexia: Tree structure of objects. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        COMPOSITE,
        /** Add responsibilities dynamically.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#DECORATOR token within GoF. @author Synexia: Add responsibilities dynamically. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DECORATOR,
        /** Simplified interface to subsystem.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#FACADE token within GoF. @author Synexia: Simplified interface to subsystem. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FACADE,
        /** Share fine-grained objects efficiently.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#FLYWEIGHT token within GoF. @author Synexia: Share fine-grained objects efficiently. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FLYWEIGHT,
        /** Placeholder for another object.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#PROXY token within GoF. @author Synexia: Placeholder for another object. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PROXY,

        // Behavioral (11)
        /** Pass request along chain of handlers.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#CHAIN_OF_RESPONSIBILITY token within GoF. @author Synexia: Pass request along chain of handlers. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CHAIN_OF_RESPONSIBILITY,
        /** Encapsulate request as object.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#COMMAND token within GoF. @author Synexia: Encapsulate request as object. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        COMMAND,
        /** Access elements sequentially.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#ITERATOR token within GoF. @author Synexia: Access elements sequentially. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ITERATOR,
        /** Centralize complex communications.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#MEDIATOR token within GoF. @author Synexia: Centralize complex communications. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MEDIATOR,
        /** Capture and restore object state.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#MEMENTO token within GoF. @author Synexia: Capture and restore object state. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MEMENTO,
        /** Notify dependents of state changes.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#OBSERVER token within GoF. @author Synexia: Notify dependents of state changes. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        OBSERVER,
        /** Alter behavior when state changes.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#STATE token within GoF. @author Synexia: Alter behavior when state changes. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        STATE,
        /** Encapsulate interchangeable algorithms.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#STRATEGY token within GoF. @author Synexia: Encapsulate interchangeable algorithms. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        STRATEGY,
        /** Define skeleton of algorithm.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#TEMPLATE_METHOD token within GoF. @author Synexia: Define skeleton of algorithm. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TEMPLATE_METHOD,
        /** Separate algorithm from object structure.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#VISITOR token within GoF. @author Synexia: Separate algorithm from object structure. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        VISITOR,
        /** Grammar interpretation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#INTERPRETER token within GoF. @author Synexia: Grammar interpretation. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        INTERPRETER,

        // Data Access (extended)
        /** Mediates between domain and data mapping.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#REPOSITORY token within GoF. @author Synexia: Mediates between domain and data mapping. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REPOSITORY,
        /** Maintains list of affected objects.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.GoF#UNIT_OF_WORK token within GoF. @author Synexia: Maintains list of affected objects. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.GoF enum constant, retaining declared order and identity; it stores no implementation object.
         */
        UNIT_OF_WORK
    }

    // --- EIP ENUM (Key patterns from 83) ---

    /** Enterprise Integration Patterns (key subset).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 27 symbolic choices for Enterprise Integration Patterns (key subset) within AIPattern advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested AIPattern.EIP enum with 27 source-declared constants in existing order.
     */
    enum EIP {
        // Messaging
        /** Named channel for message delivery.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#MESSAGE_CHANNEL token within Enterprise Integration Patterns (key subset): Named channel for message delivery. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MESSAGE_CHANNEL,
        /** Single sender to single receiver.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#POINT_TO_POINT token within Enterprise Integration Patterns (key subset): Single sender to single receiver. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        POINT_TO_POINT,
        /** One-to-many distribution.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#PUBLISH_SUBSCRIBE token within Enterprise Integration Patterns (key subset): One-to-many distribution. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PUBLISH_SUBSCRIBE,
        /** Request and wait for reply.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#REQUEST_REPLY token within Enterprise Integration Patterns (key subset): Request and wait for reply. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REQUEST_REPLY,

        // Routing
        /** Route based on content.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#CONTENT_BASED_ROUTER token within Enterprise Integration Patterns (key subset): Route based on content. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONTENT_BASED_ROUTER,
        /** Filter messages by criteria.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#MESSAGE_FILTER token within Enterprise Integration Patterns (key subset): Filter messages by criteria. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MESSAGE_FILTER,
        /** Send to multiple recipients.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#RECIPIENT_LIST token within Enterprise Integration Patterns (key subset): Send to multiple recipients. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        RECIPIENT_LIST,
        /** Split message into parts.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#SPLITTER token within Enterprise Integration Patterns (key subset): Split message into parts. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SPLITTER,
        /** Combine multiple messages.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#AGGREGATOR token within Enterprise Integration Patterns (key subset): Combine multiple messages. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        AGGREGATOR,
        /** Reorder messages by sequence.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#RESEQUENCER token within Enterprise Integration Patterns (key subset): Reorder messages by sequence. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        RESEQUENCER,
        /** Route to one of several outputs.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#DYNAMIC_ROUTER token within Enterprise Integration Patterns (key subset): Route to one of several outputs. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DYNAMIC_ROUTER,

        // Transformation
        /** Transform message content.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#MESSAGE_TRANSLATOR token within Enterprise Integration Patterns (key subset): Transform message content. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MESSAGE_TRANSLATOR,
        /** Add data to message.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#CONTENT_ENRICHER token within Enterprise Integration Patterns (key subset): Add data to message. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONTENT_ENRICHER,
        /** Remove data from message.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#CONTENT_FILTER token within Enterprise Integration Patterns (key subset): Remove data from message. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONTENT_FILTER,
        /** Normalize different formats.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#NORMALIZER token within Enterprise Integration Patterns (key subset): Normalize different formats. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        NORMALIZER,

        // Endpoints
        /** Consume messages.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#MESSAGE_ENDPOINT token within Enterprise Integration Patterns (key subset): Consume messages. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MESSAGE_ENDPOINT,
        /** Poll for messages.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#POLLING_CONSUMER token within Enterprise Integration Patterns (key subset): Poll for messages. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        POLLING_CONSUMER,
        /** Receive messages asynchronously.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#EVENT_DRIVEN_CONSUMER token within Enterprise Integration Patterns (key subset): Receive messages asynchronously. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        EVENT_DRIVEN_CONSUMER,
        /** Compete for messages.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#COMPETING_CONSUMERS token within Enterprise Integration Patterns (key subset): Compete for messages. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        COMPETING_CONSUMERS,
        /** Dispatch to workers.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#MESSAGE_DISPATCHER token within Enterprise Integration Patterns (key subset): Dispatch to workers. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MESSAGE_DISPATCHER,

        // System Management
        /** Wrap with standard metadata.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#ENVELOPE_WRAPPER token within Enterprise Integration Patterns (key subset): Wrap with standard metadata. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ENVELOPE_WRAPPER,
        /** Track message flow.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#MESSAGE_HISTORY token within Enterprise Integration Patterns (key subset): Track message flow. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MESSAGE_HISTORY,
        /** Store and forward.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#MESSAGE_STORE token within Enterprise Integration Patterns (key subset): Store and forward. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MESSAGE_STORE,
        /** Eliminate duplicates.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#IDEMPOTENT_RECEIVER token within Enterprise Integration Patterns (key subset): Eliminate duplicates. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        IDEMPOTENT_RECEIVER,

        // Error Handling
        /** Handle failed messages.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#DEAD_LETTER_CHANNEL token within Enterprise Integration Patterns (key subset): Handle failed messages. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DEAD_LETTER_CHANNEL,
        /** Detect invalid messages.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#INVALID_MESSAGE_CHANNEL token within Enterprise Integration Patterns (key subset): Detect invalid messages. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        INVALID_MESSAGE_CHANNEL,
        /** Route to error handler.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.EIP#ERROR_CHANNEL token within Enterprise Integration Patterns (key subset): Route to error handler. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.EIP enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ERROR_CHANNEL
    }

    // --- MICROSERVICE ENUM (Key patterns from 41) ---

    /** Microservice patterns (key subset).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 23 symbolic choices for Microservice patterns (key subset) within AIPattern advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested AIPattern.Microservice enum with 23 source-declared constants in existing order.
     */
    enum Microservice {
        // Resilience
        /** Prevent cascade failures.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#CIRCUIT_BREAKER token within Microservice patterns (key subset): Prevent cascade failures. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CIRCUIT_BREAKER,
        /** Retry failed operations.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#RETRY token within Microservice patterns (key subset): Retry failed operations. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        RETRY,
        /** Limit concurrent requests.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#BULKHEAD token within Microservice patterns (key subset): Limit concurrent requests. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BULKHEAD,
        /** Limit request rate.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#RATE_LIMITER token within Microservice patterns (key subset): Limit request rate. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        RATE_LIMITER,
        /** Fail within time limit.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#TIMEOUT token within Microservice patterns (key subset): Fail within time limit. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TIMEOUT,
        /** Fallback on failure.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#FALLBACK token within Microservice patterns (key subset): Fallback on failure. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FALLBACK,

        // Discovery
        /** Register and discover services.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#SERVICE_REGISTRY token within Microservice patterns (key subset): Register and discover services. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SERVICE_REGISTRY,
        /** Client-side discovery.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#CLIENT_SIDE_DISCOVERY token within Microservice patterns (key subset): Client-side discovery. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CLIENT_SIDE_DISCOVERY,
        /** Server-side discovery.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#SERVER_SIDE_DISCOVERY token within Microservice patterns (key subset): Server-side discovery. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SERVER_SIDE_DISCOVERY,

        // Communication
        /** Single entry point.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#API_GATEWAY token within Microservice patterns (key subset): Single entry point. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        API_GATEWAY,
        /** Backend for specific frontend.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#BFF token within Microservice patterns (key subset): Backend for specific frontend. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BFF,
        /** Proxy for service communication.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#SERVICE_MESH token within Microservice patterns (key subset): Proxy for service communication. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SERVICE_MESH,

        // Data
        /** Database per service.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#DATABASE_PER_SERVICE token within Microservice patterns (key subset): Database per service. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DATABASE_PER_SERVICE,
        /** Distributed transactions.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#SAGA token within Microservice patterns (key subset): Distributed transactions. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SAGA,
        /** Separate read/write models.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#CQRS token within Microservice patterns (key subset): Separate read/write models. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CQRS,
        /** Store state as event sequence.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#EVENT_SOURCING token within Microservice patterns (key subset): Store state as event sequence. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        EVENT_SOURCING,
        /** Change data capture.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#TRANSACTIONAL_OUTBOX token within Microservice patterns (key subset): Change data capture. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TRANSACTIONAL_OUTBOX,

        // Deployment
        /** Blue-green deployment.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#BLUE_GREEN token within Microservice patterns (key subset): Blue-green deployment. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BLUE_GREEN,
        /** Gradual rollout.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#CANARY token within Microservice patterns (key subset): Gradual rollout. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CANARY,
        /** Route by feature.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#FEATURE_FLAG token within Microservice patterns (key subset): Route by feature. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FEATURE_FLAG,

        // Observability
        /** Distributed tracing.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#DISTRIBUTED_TRACING token within Microservice patterns (key subset): Distributed tracing. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DISTRIBUTED_TRACING,
        /** Health endpoint.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#HEALTH_CHECK token within Microservice patterns (key subset): Health endpoint. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        HEALTH_CHECK,
        /** Structured logging.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Microservice#LOG_AGGREGATION token within Microservice patterns (key subset): Structured logging. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Microservice enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LOG_AGGREGATION
    }

    // --- DAG ENUM (Key patterns from 40) ---

    /** DAG flow patterns (key subset).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 21 symbolic choices for DAG flow patterns (key subset) within AIPattern advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested AIPattern.DAG enum with 21 source-declared constants in existing order.
     */
    enum DAG {
        // Flow Control
        /** Linear step sequence.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#PIPELINE token within DAG flow patterns (key subset): Linear step sequence. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PIPELINE,
        /** Parallel branching.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#FORK token within DAG flow patterns (key subset): Parallel branching. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FORK,
        /** Wait for branches.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#JOIN token within DAG flow patterns (key subset): Wait for branches. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        JOIN,
        /** Fork and join.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#FORK_JOIN token within DAG flow patterns (key subset): Fork and join. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FORK_JOIN,
        /** Conditional branching.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#CONDITIONAL token within DAG flow patterns (key subset): Conditional branching. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONDITIONAL,
        /** Iterative loop.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#LOOP token within DAG flow patterns (key subset): Iterative loop. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LOOP,

        // Data Flow
        /** Apply to each element.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#MAP token within DAG flow patterns (key subset): Apply to each element. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MAP,
        /** Combine elements.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#REDUCE token within DAG flow patterns (key subset): Combine elements. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REDUCE,
        /** Filter elements.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#FILTER token within DAG flow patterns (key subset): Filter elements. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FILTER,
        /** Flatten nested structures.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#FLATMAP token within DAG flow patterns (key subset): Flatten nested structures. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FLATMAP,
        /** Group by key.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#GROUP_BY token within DAG flow patterns (key subset): Group by key. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GROUP_BY,

        // Orchestration
        /** Multi-step transaction with compensation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#SAGA token within DAG flow patterns (key subset): Multi-step transaction with compensation. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SAGA,
        /** Coordinate parallel work.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#SCATTER_GATHER token within DAG flow patterns (key subset): Coordinate parallel work. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SCATTER_GATHER,
        /** Process sequentially.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#SEQUENTIAL token within DAG flow patterns (key subset): Process sequentially. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SEQUENTIAL,
        /** Process in parallel.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#PARALLEL token within DAG flow patterns (key subset): Process in parallel. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PARALLEL,

        // Agent Patterns
        /** Reason-Act loop.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#REACT token within DAG flow patterns (key subset): Reason-Act loop. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REACT,
        /** Reflect on output.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#REFLEXION token within DAG flow patterns (key subset): Reflect on output. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REFLEXION,
        /** Plan then execute.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#PLAN_AND_EXECUTE token within DAG flow patterns (key subset): Plan then execute. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PLAN_AND_EXECUTE,
        /** Multiple agents collaborate.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#MULTI_AGENT token within DAG flow patterns (key subset): Multiple agents collaborate. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MULTI_AGENT,
        /** Supervise sub-agents.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#SUPERVISOR token within DAG flow patterns (key subset): Supervise sub-agents. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SUPERVISOR,
        /** Tool-using agent.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.DAG#TOOL_AGENT token within DAG flow patterns (key subset): Tool-using agent. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.DAG enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TOOL_AGENT
    }

    // --- SCOPE ENUM (DI/Lifecycle Scopes) ---

    /** Instance scope hints.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 8 symbolic choices for Instance scope hints within AIPattern advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested AIPattern.Scope enum with 8 source-declared constants in existing order.
     */
    enum Scope {
        /** Use default scope.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Scope#DEFAULT token within Instance scope hints: Use default scope. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Scope enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DEFAULT,

        /** Single instance for entire application.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Scope#APPLICATION token within Instance scope hints: Single instance for entire application. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Scope enum constant, retaining declared order and identity; it stores no implementation object.
         */
        APPLICATION,

        /** Single instance per session.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Scope#SESSION token within Instance scope hints: Single instance per session. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Scope enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SESSION,

        /** Single instance per request.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Scope#REQUEST token within Instance scope hints: Single instance per request. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Scope enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REQUEST,

        /** New instance each time (prototype).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Scope#PROTOTYPE token within Instance scope hints: New instance each time (prototype). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Scope enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PROTOTYPE,

        /** Single instance per thread.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Scope#THREAD token within Instance scope hints: Single instance per thread. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Scope enum constant, retaining declared order and identity; it stores no implementation object.
         */
        THREAD,

        /** Single instance per conversation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Scope#CONVERSATION token within Instance scope hints: Single instance per conversation. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Scope enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONVERSATION,

        /** Lazily initialized singleton.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the AIPattern.Scope#LAZY_SINGLETON token within Instance scope hints: Lazily initialized singleton. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing AIPattern.Scope enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LAZY_SINGLETON
    }
}
