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
 * EIP - Enterprise Integration Patterns (Apache Camel Naming).
 *
 * <p>
 * Patterns from Gregor Hohpe and Bobby Woolf's "Enterprise Integration
 * Patterns" book, using Apache Camel naming conventions.
 *
 * @see <a href=
 *      "https://camel.apache.org/components/latest/eips/enterprise-integration-patterns.html">Apache
 *      Camel EIP</a>
 * @m3.pattern synexia:iop
 * @m3.algorithm Defines 82 singleton tokens in declaration order, with generated name, ordinal, values and valueOf behavior. The declared descriptor constructor retains its supplied references and getters return them directly. Naming a pattern or annotation does not implement that named behavior.
 * @m3.dataStructures Enum values in source order: aggregate, bean, choice, circuitBreaker, claimCheck, contentBasedRouter, convertBodyTo, delay, dynamicRouter, enrich, filter, from, idempotentConsumer, loadBalance, log, loop, marshal, multicast, onCompletion, onException, otherwise, pipeline, pollEnrich, process, recipientList, removeHeader, removeHeaders, removeProperty, resequence, routingSlip, saga, sample, script, serviceCall, setBody, setHeader, setProperty, sort, split, step, stop, threads, throttle, throwException, to, toD, transform, unmarshal, validate, when, wireTap, messageChannel, messageBus, deadLetterChannel, guaranteedDelivery, messageTranslator, messageEndpoint, pollingConsumer, eventDrivenConsumer, competingConsumers, messageDispatcher, selectiveConsumer, durableSubscriber, messagingGateway, messagingMapper, transactionalClient, serviceActivator, scatterGather, composedMessageProcessor, processManager, normalizer, canonicalDataModel, contentEnricher, contentFilter, envelopeWrapper, controlBus, detour, messageHistory, messageStore, smartProxy, testMessage, channelPurger.
 * @m3.member {@code FIELD:aggregate} pattern=synexia:iop; algorithm=Initializes aggregate using the existing expression <code>new EIP("Routing", "Aggregates many messages into a single message")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:bean} pattern=synexia:iop; algorithm=Initializes bean using the existing expression <code>new EIP("Endpoint", "Invokes a method on a bean")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:choice} pattern=synexia:iop; algorithm=Initializes choice using the existing expression <code>new EIP("Routing", "Routes messages based on content using when/otherwise")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:circuitBreaker} pattern=synexia:iop; algorithm=Initializes circuitBreaker using the existing expression <code>new EIP("Resilience", "Circuit Breaker pattern for fault tolerance")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:claimCheck} pattern=synexia:iop; algorithm=Initializes claimCheck using the existing expression <code>new EIP("Transformation", "Store large data, pass reference, retrieve later")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:contentBasedRouter} pattern=synexia:iop; algorithm=Initializes contentBasedRouter using the existing expression <code>new EIP("Routing", "Route based on message content")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:convertBodyTo} pattern=synexia:iop; algorithm=Initializes convertBodyTo using the existing expression <code>new EIP("Transformation", "Converts the message body to another type")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:delay} pattern=synexia:iop; algorithm=Initializes delay using the existing expression <code>new EIP("Routing", "Delays the processing of a message")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:dynamicRouter} pattern=synexia:iop; algorithm=Initializes dynamicRouter using the existing expression <code>new EIP("Routing", "Route to dynamically determined destinations")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:enrich} pattern=synexia:iop; algorithm=Initializes enrich using the existing expression <code>new EIP("Transformation", "Enriches a message with additional data from a resource")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:filter} pattern=synexia:iop; algorithm=Initializes filter using the existing expression <code>new EIP("Routing", "Filters messages based on a predicate")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:from} pattern=synexia:iop; algorithm=Initializes from using the existing expression <code>new EIP("Endpoint", "Start of a route - message consumer")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:idempotentConsumer} pattern=synexia:iop; algorithm=Initializes idempotentConsumer using the existing expression <code>new EIP("Routing", "Filters out duplicate messages")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:loadBalance} pattern=synexia:iop; algorithm=Initializes loadBalance using the existing expression <code>new EIP("Routing", "Load balances messages across multiple endpoints")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:log} pattern=synexia:iop; algorithm=Initializes log using the existing expression <code>new EIP("Management", "Logs messages to a logger")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:loop} pattern=synexia:iop; algorithm=Initializes loop using the existing expression <code>new EIP("Routing", "Processes messages N times in a loop")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:marshal} pattern=synexia:iop; algorithm=Initializes marshal using the existing expression <code>new EIP("Transformation", "Marshals the message body to a specific data format")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:multicast} pattern=synexia:iop; algorithm=Initializes multicast using the existing expression <code>new EIP("Routing", "Sends message to multiple endpoints simultaneously")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:onCompletion} pattern=synexia:iop; algorithm=Initializes onCompletion using the existing expression <code>new EIP("Management", "Callback executed on route completion")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:onException} pattern=synexia:iop; algorithm=Initializes onException using the existing expression <code>new EIP("ErrorHandling", "Defines exception handling policy")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:otherwise} pattern=synexia:iop; algorithm=Initializes otherwise using the existing expression <code>new EIP("Routing", "Default branch in choice routing")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:pipeline} pattern=synexia:iop; algorithm=Initializes pipeline using the existing expression <code>new EIP("Routing", "Chains processors in sequence (default)")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:pollEnrich} pattern=synexia:iop; algorithm=Initializes pollEnrich using the existing expression <code>new EIP("Transformation", "Polls a resource to enrich the message")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:process} pattern=synexia:iop; algorithm=Initializes process using the existing expression <code>new EIP("Endpoint", "Invokes a custom Processor")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:recipientList} pattern=synexia:iop; algorithm=Initializes recipientList using the existing expression <code>new EIP("Routing", "Routes to a list of dynamically determined recipients")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:removeHeader} pattern=synexia:iop; algorithm=Initializes removeHeader using the existing expression <code>new EIP("Transformation", "Removes a header from the message")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:removeHeaders} pattern=synexia:iop; algorithm=Initializes removeHeaders using the existing expression <code>new EIP("Transformation", "Removes headers matching a pattern")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:removeProperty} pattern=synexia:iop; algorithm=Initializes removeProperty using the existing expression <code>new EIP("Transformation", "Removes an exchange property")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:resequence} pattern=synexia:iop; algorithm=Initializes resequence using the existing expression <code>new EIP("Routing", "Reorders messages based on an expression")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:routingSlip} pattern=synexia:iop; algorithm=Initializes routingSlip using the existing expression <code>new EIP("Routing", "Routes message through a dynamic sequence of endpoints")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:saga} pattern=synexia:iop; algorithm=Initializes saga using the existing expression <code>new EIP("Orchestration", "Saga pattern for long-running distributed transactions")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:sample} pattern=synexia:iop; algorithm=Initializes sample using the existing expression <code>new EIP("Routing", "Samples messages at specified intervals")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:script} pattern=synexia:iop; algorithm=Initializes script using the existing expression <code>new EIP("Transformation", "Executes a script language")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:serviceCall} pattern=synexia:iop; algorithm=Initializes serviceCall using the existing expression <code>new EIP("Endpoint", "Calls a remote service with service discovery")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:setBody} pattern=synexia:iop; algorithm=Initializes setBody using the existing expression <code>new EIP("Transformation", "Sets the message body")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:setHeader} pattern=synexia:iop; algorithm=Initializes setHeader using the existing expression <code>new EIP("Transformation", "Sets a message header")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:setProperty} pattern=synexia:iop; algorithm=Initializes setProperty using the existing expression <code>new EIP("Transformation", "Sets an exchange property")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:sort} pattern=synexia:iop; algorithm=Initializes sort using the existing expression <code>new EIP("Transformation", "Sorts messages based on an expression")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:split} pattern=synexia:iop; algorithm=Initializes split using the existing expression <code>new EIP("Routing", "Splits a message into multiple parts")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:step} pattern=synexia:iop; algorithm=Initializes step using the existing expression <code>new EIP("Orchestration", "Defines a step in a saga")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:stop} pattern=synexia:iop; algorithm=Initializes stop using the existing expression <code>new EIP("Routing", "Stops routing the current message")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:threads} pattern=synexia:iop; algorithm=Initializes threads using the existing expression <code>new EIP("Concurrency", "Processes messages using a thread pool")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:throttle} pattern=synexia:iop; algorithm=Initializes throttle using the existing expression <code>new EIP("Resilience", "Throttles the rate of messages")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:throwException} pattern=synexia:iop; algorithm=Initializes throwException using the existing expression <code>new EIP("ErrorHandling", "Throws an exception")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:to} pattern=synexia:iop; algorithm=Initializes to using the existing expression <code>new EIP("Endpoint", "Sends message to an endpoint")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:toD} pattern=synexia:iop; algorithm=Initializes toD using the existing expression <code>new EIP("Endpoint", "Sends message to a dynamically computed endpoint")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:transform} pattern=synexia:iop; algorithm=Initializes transform using the existing expression <code>new EIP("Transformation", "Transforms the message body")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:unmarshal} pattern=synexia:iop; algorithm=Initializes unmarshal using the existing expression <code>new EIP("Transformation", "Unmarshals message body from a data format")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:validate} pattern=synexia:iop; algorithm=Initializes validate using the existing expression <code>new EIP("Transformation", "Validates message content")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:when} pattern=synexia:iop; algorithm=Initializes when using the existing expression <code>new EIP("Routing", "Conditional branch in choice routing")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:wireTap} pattern=synexia:iop; algorithm=Initializes wireTap using the existing expression <code>new EIP("Management", "Sends a copy of the message to another endpoint")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:messageChannel} pattern=synexia:iop; algorithm=Initializes messageChannel using the existing expression <code>new EIP("Channel", "Connects sender and receiver via channel")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:messageBus} pattern=synexia:iop; algorithm=Initializes messageBus using the existing expression <code>new EIP("Channel", "Backbone that connects all applications")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:deadLetterChannel} pattern=synexia:iop; algorithm=Initializes deadLetterChannel using the existing expression <code>new EIP("ErrorHandling", "Handles messages that cannot be delivered")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:guaranteedDelivery} pattern=synexia:iop; algorithm=Initializes guaranteedDelivery using the existing expression <code>new EIP("Channel", "Ensures message is delivered despite failures")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:messageTranslator} pattern=synexia:iop; algorithm=Initializes messageTranslator using the existing expression <code>new EIP("Transformation", "Translates message from one format to another")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:messageEndpoint} pattern=synexia:iop; algorithm=Initializes messageEndpoint using the existing expression <code>new EIP("Endpoint", "Application\'s connection to the messaging system")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:pollingConsumer} pattern=synexia:iop; algorithm=Initializes pollingConsumer using the existing expression <code>new EIP("Endpoint", "Consumer that polls for messages")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:eventDrivenConsumer} pattern=synexia:iop; algorithm=Initializes eventDrivenConsumer using the existing expression <code>new EIP("Endpoint", "Consumer that receives pushed messages")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:competingConsumers} pattern=synexia:iop; algorithm=Initializes competingConsumers using the existing expression <code>new EIP("Endpoint", "Multiple consumers compete for messages")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:messageDispatcher} pattern=synexia:iop; algorithm=Initializes messageDispatcher using the existing expression <code>new EIP("Endpoint", "Coordinates multiple consumers")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:selectiveConsumer} pattern=synexia:iop; algorithm=Initializes selectiveConsumer using the existing expression <code>new EIP("Endpoint", "Selects which messages to receive")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:durableSubscriber} pattern=synexia:iop; algorithm=Initializes durableSubscriber using the existing expression <code>new EIP("Endpoint", "Subscription that persists across restarts")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:messagingGateway} pattern=synexia:iop; algorithm=Initializes messagingGateway using the existing expression <code>new EIP("Endpoint", "Encapsulates access to the messaging system")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:messagingMapper} pattern=synexia:iop; algorithm=Initializes messagingMapper using the existing expression <code>new EIP("Transformation", "Maps domain objects to/from messages")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:transactionalClient} pattern=synexia:iop; algorithm=Initializes transactionalClient using the existing expression <code>new EIP("Endpoint", "Controls transactions with messaging")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:serviceActivator} pattern=synexia:iop; algorithm=Initializes serviceActivator using the existing expression <code>new EIP("Endpoint", "Connects application service to messaging system")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:scatterGather} pattern=synexia:iop; algorithm=Initializes scatterGather using the existing expression <code>new EIP("Routing", "Broadcasts to recipients and aggregates responses")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:composedMessageProcessor} pattern=synexia:iop; algorithm=Initializes composedMessageProcessor using the existing expression <code>new EIP("Routing", "Processes a composed message")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:processManager} pattern=synexia:iop; algorithm=Initializes processManager using the existing expression <code>new EIP("Routing", "Maintains state during multi-step routing")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:normalizer} pattern=synexia:iop; algorithm=Initializes normalizer using the existing expression <code>new EIP("Transformation", "Converts different formats to a common format")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:canonicalDataModel} pattern=synexia:iop; algorithm=Initializes canonicalDataModel using the existing expression <code>new EIP("Transformation", "Application-independent data model")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:contentEnricher} pattern=synexia:iop; algorithm=Initializes contentEnricher using the existing expression <code>new EIP("Transformation", "Adds missing data to a message")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:contentFilter} pattern=synexia:iop; algorithm=Initializes contentFilter using the existing expression <code>new EIP("Transformation", "Removes unwanted data from a message")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:envelopeWrapper} pattern=synexia:iop; algorithm=Initializes envelopeWrapper using the existing expression <code>new EIP("Transformation", "Wraps message data in an envelope")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:controlBus} pattern=synexia:iop; algorithm=Initializes controlBus using the existing expression <code>new EIP("Management", "Administers the messaging system")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:detour} pattern=synexia:iop; algorithm=Initializes detour using the existing expression <code>new EIP("Management", "Routes messages through additional steps for testing")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:messageHistory} pattern=synexia:iop; algorithm=Initializes messageHistory using the existing expression <code>new EIP("Management", "Records the path message has traveled")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:messageStore} pattern=synexia:iop; algorithm=Initializes messageStore using the existing expression <code>new EIP("Management", "Stores messages for audit or replay")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:smartProxy} pattern=synexia:iop; algorithm=Initializes smartProxy using the existing expression <code>new EIP("Management", "Tracks requests and matches with responses")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:testMessage} pattern=synexia:iop; algorithm=Initializes testMessage using the existing expression <code>new EIP("Management", "Verifies messaging system is working correctly")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:channelPurger} pattern=synexia:iop; algorithm=Initializes channelPurger using the existing expression <code>new EIP("Management", "Removes pending messages from a channel")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One EIP enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:category} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The category declaration is a stored String field or record component with no initializer and no executable algorithm body.; algorithm.review=iop-annotation-contracts-declarations/reviewed-declarations.json#EIP-11578; data.structures=One stored String field or component. The enum or record constructor retains the supplied reference directly, without validation or defensive copying.; description=Stored category descriptor in EIP.
 * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration is a stored String field or record component with no initializer and no executable algorithm body.; algorithm.review=iop-annotation-contracts-declarations/reviewed-declarations.json#EIP-11613; data.structures=One stored String field or component. The enum or record constructor retains the supplied reference directly, without validation or defensive copying.; description=Stored description descriptor in EIP.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/EIP.java}
 * blob {@code 6a91ae73cd2b437fc1c780e3c579e6cd26741830} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum EIP {
    // --- MESSAGE ROUTING (camel.apache.org/components/latest/eips/) ---

    /** aggregate - Aggregates many messages into a single message. */
    aggregate("Routing", "Aggregates many messages into a single message"),

    /** bean - Invokes a method on a bean. */
    bean("Endpoint", "Invokes a method on a bean"),

    /** choice - Routes messages based on content (when/otherwise). */
    choice("Routing", "Routes messages based on content using when/otherwise"),

    /** circuitBreaker - Circuit Breaker pattern for fault tolerance. */
    circuitBreaker("Resilience", "Circuit Breaker pattern for fault tolerance"),

    /** claimCheck - Store data temporarily and retrieve later. */
    claimCheck("Transformation", "Store large data, pass reference, retrieve later"),

    /** contentBasedRouter - Route based on message content. */
    contentBasedRouter("Routing", "Route based on message content"),

    /** convertBodyTo - Converts message body to another type. */
    convertBodyTo("Transformation", "Converts the message body to another type"),

    /** delay - Delays message processing. */
    delay("Routing", "Delays the processing of a message"),

    /** dynamicRouter - Route to dynamically determined destination. */
    dynamicRouter("Routing", "Route to dynamically determined destinations"),

    /** enrich - Enriches a message with additional data. */
    enrich("Transformation", "Enriches a message with additional data from a resource"),

    /** filter - Filters messages based on a predicate. */
    filter("Routing", "Filters messages based on a predicate"),

    /** from - Start of a route (consumer endpoint). */
    from("Endpoint", "Start of a route - message consumer"),

    /** idempotentConsumer - Filters duplicate messages. */
    idempotentConsumer("Routing", "Filters out duplicate messages"),

    /** loadBalance - Distributes load across endpoints. */
    loadBalance("Routing", "Load balances messages across multiple endpoints"),

    /** log - Logs messages to a logger. */
    log("Management", "Logs messages to a logger"),

    /** loop - Loops through processing N times. */
    loop("Routing", "Processes messages N times in a loop"),

    /** marshal - Marshals data to a specific format. */
    marshal("Transformation", "Marshals the message body to a specific data format"),

    /** multicast - Multicasts to multiple endpoints. */
    multicast("Routing", "Sends message to multiple endpoints simultaneously"),

    /** onCompletion - Callback on route completion. */
    onCompletion("Management", "Callback executed on route completion"),

    /** onException - Exception handler. */
    onException("ErrorHandling", "Defines exception handling policy"),

    /** otherwise - Default branch in choice. */
    otherwise("Routing", "Default branch in choice routing"),

    /** pipeline - Chains processors in sequence. */
    pipeline("Routing", "Chains processors in sequence (default)"),

    /** pollEnrich - Poll a resource to enrich message. */
    pollEnrich("Transformation", "Polls a resource to enrich the message"),

    /** process - Invokes a Processor. */
    process("Endpoint", "Invokes a custom Processor"),

    /** recipientList - Route to a list of recipients. */
    recipientList("Routing", "Routes to a list of dynamically determined recipients"),

    /** removeHeader - Removes a header. */
    removeHeader("Transformation", "Removes a header from the message"),

    /** removeHeaders - Removes headers by pattern. */
    removeHeaders("Transformation", "Removes headers matching a pattern"),

    /** removeProperty - Removes an exchange property. */
    removeProperty("Transformation", "Removes an exchange property"),

    /** resequence - Reorders messages by sequence. */
    resequence("Routing", "Reorders messages based on an expression"),

    /** routingSlip - Route through a dynamic sequence. */
    routingSlip("Routing", "Routes message through a dynamic sequence of endpoints"),

    /** saga - Saga pattern for long-running transactions. */
    saga("Orchestration", "Saga pattern for long-running distributed transactions"),

    /** sample - Sample messages at intervals. */
    sample("Routing", "Samples messages at specified intervals"),

    /** script - Executes a script. */
    script("Transformation", "Executes a script language"),

    /** serviceCall - Calls a remote service. */
    serviceCall("Endpoint", "Calls a remote service with service discovery"),

    /** setBody - Sets the message body. */
    setBody("Transformation", "Sets the message body"),

    /** setHeader - Sets a header. */
    setHeader("Transformation", "Sets a message header"),

    /** setProperty - Sets an exchange property. */
    setProperty("Transformation", "Sets an exchange property"),

    /** sort - Sorts messages. */
    sort("Transformation", "Sorts messages based on an expression"),

    /** split - Splits a message into parts. */
    split("Routing", "Splits a message into multiple parts"),

    /** step - Step for saga transaction. */
    step("Orchestration", "Defines a step in a saga"),

    /** stop - Stops routing. */
    stop("Routing", "Stops routing the current message"),

    /** threads - Uses thread pool for async processing. */
    threads("Concurrency", "Processes messages using a thread pool"),

    /** throttle - Throttles message rate. */
    throttle("Resilience", "Throttles the rate of messages"),

    /** throwException - Throws an exception. */
    throwException("ErrorHandling", "Throws an exception"),

    /** to - Send to an endpoint. */
    to("Endpoint", "Sends message to an endpoint"),

    /** toD - Send to a dynamic endpoint. */
    toD("Endpoint", "Sends message to a dynamically computed endpoint"),

    /** transform - Transforms the message. */
    transform("Transformation", "Transforms the message body"),

    /** unmarshal - Unmarshals data from a format. */
    unmarshal("Transformation", "Unmarshals message body from a data format"),

    /** validate - Validates message content. */
    validate("Transformation", "Validates message content"),

    /** when - Conditional branch in choice. */
    when("Routing", "Conditional branch in choice routing"),

    /** wireTap - Taps a copy of the message. */
    wireTap("Management", "Sends a copy of the message to another endpoint"),

    // --- CLASSIC EIP PATTERNS (Book Terminology) ---

    /** messageChannel - Point-to-point or pub-sub channel. */
    messageChannel("Channel", "Connects sender and receiver via channel"),

    /** messageBus - Backbone connecting applications. */
    messageBus("Channel", "Backbone that connects all applications"),

    /** deadLetterChannel - Handle undeliverable messages. */
    deadLetterChannel("ErrorHandling", "Handles messages that cannot be delivered"),

    /** guaranteedDelivery - Ensure message is delivered. */
    guaranteedDelivery("Channel", "Ensures message is delivered despite failures"),

    /** messageTranslator - Translates message format. */
    messageTranslator("Transformation", "Translates message from one format to another"),

    /** messageEndpoint - Application connection point. */
    messageEndpoint("Endpoint", "Application's connection to the messaging system"),

    /** pollingConsumer - Polls for messages. */
    pollingConsumer("Endpoint", "Consumer that polls for messages"),

    /** eventDrivenConsumer - Pushed messages. */
    eventDrivenConsumer("Endpoint", "Consumer that receives pushed messages"),

    /** competingConsumers - Concurrent consumers. */
    competingConsumers("Endpoint", "Multiple consumers compete for messages"),

    /** messageDispatcher - Coordinates consumers. */
    messageDispatcher("Endpoint", "Coordinates multiple consumers"),

    /** selectiveConsumer - Selects messages to receive. */
    selectiveConsumer("Endpoint", "Selects which messages to receive"),

    /** durableSubscriber - Persists subscription. */
    durableSubscriber("Endpoint", "Subscription that persists across restarts"),

    /** messagingGateway - Encapsulates messaging access. */
    messagingGateway("Endpoint", "Encapsulates access to the messaging system"),

    /** messagingMapper - Maps domain to messages. */
    messagingMapper("Transformation", "Maps domain objects to/from messages"),

    /** transactionalClient - Transaction control. */
    transactionalClient("Endpoint", "Controls transactions with messaging"),

    /** serviceActivator - Connects service to messaging. */
    serviceActivator("Endpoint", "Connects application service to messaging system"),

    /** scatterGather - Broadcast and aggregate. */
    scatterGather("Routing", "Broadcasts to recipients and aggregates responses"),

    /** composedMessageProcessor - Process composed message. */
    composedMessageProcessor("Routing", "Processes a composed message"),

    /** processManager - Maintains routing state. */
    processManager("Routing", "Maintains state during multi-step routing"),

    /** normalizer - Converts to common format. */
    normalizer("Transformation", "Converts different formats to a common format"),

    /** canonicalDataModel - Independent data model. */
    canonicalDataModel("Transformation", "Application-independent data model"),

    /** contentEnricher - Adds data to message. */
    contentEnricher("Transformation", "Adds missing data to a message"),

    /** contentFilter - Removes data from message. */
    contentFilter("Transformation", "Removes unwanted data from a message"),

    /** envelopeWrapper - Wraps message in envelope. */
    envelopeWrapper("Transformation", "Wraps message data in an envelope"),

    /** controlBus - Administers messaging system. */
    controlBus("Management", "Administers the messaging system"),

    /** detour - Routes through extra steps. */
    detour("Management", "Routes messages through additional steps for testing"),

    /** messageHistory - Records processing path. */
    messageHistory("Management", "Records the path message has traveled"),

    /** messageStore - Stores for audit/replay. */
    messageStore("Management", "Stores messages for audit or replay"),

    /** smartProxy - Tracks request/response. */
    smartProxy("Management", "Tracks requests and matches with responses"),

    /** testMessage - Tests system health. */
    testMessage("Management", "Verifies messaging system is working correctly"),

    /** channelPurger - Removes pending messages. */
    channelPurger("Management", "Removes pending messages from a channel");

    private final String category;
    private final String description;

    /**
     * EIP descriptor reference constructor.
     * @m3.pattern synexia:iop
     * @m3.algorithm Assigns the supplied category, description references directly to category, description fields in source order, without validation or defensive copying.
     * @m3.dataStructures Constructor parameters and corresponding final descriptor fields: category, description.
     */
    EIP(String category, String description) {
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
