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
 * ICamelSpec - Apache Camel route specification for AI code generation.
 *
 * <h2>Camel Route Generation</h2>
 *
 * <pre>
 * From @ICamelSpec annotation, the IOP pipeline generates:
 *   1. RouteBuilder implementation
 *   2. From/to endpoint configuration
 *   3. Processor chain
 *   4. Error handling
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;ISpec(value = "File processing route", library = Library.CAMEL)
 * 	public interface IFileProcessorRoute&lt;Self extends IFileProcessorRoute&lt;Self>> extends IContext&lt;Self> {
 *
 * 		&#64;ICamelSpec(from = "file://input?noop=true", to = "file://output", pattern = EIP.CONTENT_BASED_ROUTER)
 * 		void processFiles();
 *
 * 		&#64;ICamelSpec(from = "kafka:orders", to = "jms:queue:processed", pattern = EIP.SPLITTER, errorHandler = ErrorHandler.DEAD_LETTER)
 * 		void processOrders();
 * 	}
 * }
 * </pre>
 *
 * <h2>Generated RouteBuilder</h2>
 *
 * <pre>{@code
 * public class FileProcessorRouteBuilder extends RouteBuilder {
 *
 * 	&#64;Override
 * 	public void configure() {
 * 		from("file://input?noop=true").routeId("file-processor").log("Processing ${file:name}").to("file://output");
 * 	}
 * }
 * }</pre>
 *
 * @since IOP 1.0 - Camel Integration
 * @see IopParadigm.EIP
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares route endpoints, integration pattern, transformation, error handling, filter/split/aggregation and throttle settings; it starts no Camel route and evaluates no script or expression.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/ICamelSpec.java}
 * blob {@code 9babdb538e0a7f44ff499441ebd8f104b5edf2f8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ICamelSpec {

    // --- ENDPOINTS ---

    /**
     * Source endpoint URI (from).
     *
     * @return from URI
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#from intent: Source endpoint URI (from). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String from();

    /**
     * Destination endpoint URI (to).
     *
     * @return to URI
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#to intent: Destination endpoint URI (to). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String to() default "";

    /**
     * Multiple destination URIs (multicast/recipient list).
     *
     * @return destination URIs
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#destinations intent: Multiple destination URIs (multicast/recipient list). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] destinations() default {};

    /**
     * Route ID for management.
     *
     * @return route ID
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#routeId intent: Route ID for management. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String routeId() default "";

    // --- EIP PATTERNS ---

    /**
     * Enterprise Integration PatternKind to apply.
     *
     * @return EIP pattern
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#pattern intent: Enterprise Integration PatternKind to apply. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code EIP}; default expression {@code EIP.PIPELINE}.
     */
    EIP pattern() default EIP.PIPELINE;

    /** Common EIP patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ICamelSpec.EIP choices described as Common EIP patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 11 constants in the existing source order.
     */
    enum EIP {
        /** Simple pipeline (from -> process -> to).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.EIP#PIPELINE policy choice: Simple pipeline (from -> process -> to). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.EIP; its declared order and identity are preserved.
         */
        PIPELINE,
        /** Content-based router.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.EIP#CONTENT_BASED_ROUTER policy choice: Content-based router. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.EIP; its declared order and identity are preserved.
         */
        CONTENT_BASED_ROUTER,
        /** Message filter.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.EIP#FILTER policy choice: Message filter. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.EIP; its declared order and identity are preserved.
         */
        FILTER,
        /** Splitter (break message into parts).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.EIP#SPLITTER policy choice: Splitter (break message into parts). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.EIP; its declared order and identity are preserved.
         */
        SPLITTER,
        /** Aggregator (combine messages).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.EIP#AGGREGATOR policy choice: Aggregator (combine messages). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.EIP; its declared order and identity are preserved.
         */
        AGGREGATOR,
        /** Multicast (send to multiple).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.EIP#MULTICAST policy choice: Multicast (send to multiple). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.EIP; its declared order and identity are preserved.
         */
        MULTICAST,
        /** Recipient list (dynamic destinations).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.EIP#RECIPIENT_LIST policy choice: Recipient list (dynamic destinations). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.EIP; its declared order and identity are preserved.
         */
        RECIPIENT_LIST,
        /** Wire tap (audit copy).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.EIP#WIRE_TAP policy choice: Wire tap (audit copy). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.EIP; its declared order and identity are preserved.
         */
        WIRE_TAP,
        /** Idempotent consumer.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.EIP#IDEMPOTENT policy choice: Idempotent consumer. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.EIP; its declared order and identity are preserved.
         */
        IDEMPOTENT,
        /** Resequencer (reorder messages).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.EIP#RESEQUENCER policy choice: Resequencer (reorder messages). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.EIP; its declared order and identity are preserved.
         */
        RESEQUENCER,
        /** Saga (distributed transaction).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.EIP#SAGA policy choice: Saga (distributed transaction). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.EIP; its declared order and identity are preserved.
         */
        SAGA
    }

    // --- TRANSFORMATION ---

    /**
     * Message transformation type.
     *
     * @return transform type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#transform intent: Message transformation type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Transform}; default expression {@code Transform.NONE}.
     */
    Transform transform() default Transform.NONE;

    /** Transformation types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ICamelSpec.Transform choices described as Transformation types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum Transform {
        /** No transformation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.Transform#NONE policy choice: No transformation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.Transform; its declared order and identity are preserved.
         */
        NONE,
        /** JSON to object.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.Transform#JSON_TO_OBJECT policy choice: JSON to object. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.Transform; its declared order and identity are preserved.
         */
        JSON_TO_OBJECT,
        /** Object to JSON.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.Transform#OBJECT_TO_JSON policy choice: Object to JSON. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.Transform; its declared order and identity are preserved.
         */
        OBJECT_TO_JSON,
        /** XML to object.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.Transform#XML_TO_OBJECT policy choice: XML to object. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.Transform; its declared order and identity are preserved.
         */
        XML_TO_OBJECT,
        /** Object to XML.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.Transform#OBJECT_TO_XML policy choice: Object to XML. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.Transform; its declared order and identity are preserved.
         */
        OBJECT_TO_XML,
        /** CSV to list.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.Transform#CSV_TO_LIST policy choice: CSV to list. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.Transform; its declared order and identity are preserved.
         */
        CSV_TO_LIST,
        /** Custom transformer.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.Transform#CUSTOM policy choice: Custom transformer. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.Transform; its declared order and identity are preserved.
         */
        CUSTOM
    }

    /**
     * Target class for transformation.
     *
     * @return target type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#transformTo intent: Target class for transformation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> transformTo() default Void.class;

    // --- ERROR HANDLING ---

    /**
     * Error handling strategy.
     *
     * @return error handler
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#errorHandler intent: Error handling strategy. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ErrorHandler}; default expression {@code ErrorHandler.DEFAULT}.
     */
    ErrorHandler errorHandler() default ErrorHandler.DEFAULT;

    /** Error handler types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ICamelSpec.ErrorHandler choices described as Error handler types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum ErrorHandler {
        /** Default Camel error handler.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.ErrorHandler#DEFAULT policy choice: Default Camel error handler. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.ErrorHandler; its declared order and identity are preserved.
         */
        DEFAULT,
        /** Dead letter channel.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.ErrorHandler#DEAD_LETTER policy choice: Dead letter channel. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.ErrorHandler; its declared order and identity are preserved.
         */
        DEAD_LETTER,
        /** Transaction error handler.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.ErrorHandler#TRANSACTED policy choice: Transaction error handler. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.ErrorHandler; its declared order and identity are preserved.
         */
        TRANSACTED,
        /** No error handler.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.ErrorHandler#NONE policy choice: No error handler. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.ErrorHandler; its declared order and identity are preserved.
         */
        NONE
    }

    /**
     * Dead letter endpoint URI.
     *
     * @return DLQ URI
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#deadLetterUri intent: Dead letter endpoint URI. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "log:deadLetter"}.
     */
    String deadLetterUri() default "log:deadLetter";

    /**
     * Maximum redelivery attempts.
     *
     * @return max redeliveries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#maxRedeliveries intent: Maximum redelivery attempts. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3}.
     */
    int maxRedeliveries() default 3;

    /**
     * Redelivery delay in milliseconds.
     *
     * @return delay
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#redeliveryDelayMs intent: Redelivery delay in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1000}.
     */
    int redeliveryDelayMs() default 1000;

    // --- FILTERING ---

    /**
     * Simple expression for filtering.
     *
     * @return filter expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#filter intent: Simple expression for filtering. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String filter() default "";

    /**
     * Expression language for filter.
     *
     * @return expression language
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#filterLang intent: Expression language for filter. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ExpressionLanguage}; default expression {@code ExpressionLanguage.SIMPLE}.
     */
    ExpressionLanguage filterLang() default ExpressionLanguage.SIMPLE;

    /** Expression languages.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ICamelSpec.ExpressionLanguage choices described as Expression languages. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum ExpressionLanguage {
        /** Camel Simple language.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.ExpressionLanguage#SIMPLE policy choice: Camel Simple language. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.ExpressionLanguage; its declared order and identity are preserved.
         */
        SIMPLE,
        /** JSONPath.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.ExpressionLanguage#JSONPATH policy choice: JSONPath. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.ExpressionLanguage; its declared order and identity are preserved.
         */
        JSONPATH,
        /** XPath.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.ExpressionLanguage#XPATH policy choice: XPath. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.ExpressionLanguage; its declared order and identity are preserved.
         */
        XPATH,
        /** Header expression.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.ExpressionLanguage#HEADER policy choice: Header expression. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.ExpressionLanguage; its declared order and identity are preserved.
         */
        HEADER,
        /** Groovy script.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.ExpressionLanguage#GROOVY policy choice: Groovy script. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.ExpressionLanguage; its declared order and identity are preserved.
         */
        GROOVY,
        /** JavaScript.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ICamelSpec.ExpressionLanguage#JAVASCRIPT policy choice: JavaScript. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ICamelSpec.ExpressionLanguage; its declared order and identity are preserved.
         */
        JAVASCRIPT
    }

    // --- SPLITTING ---

    /**
     * Split expression (for SPLITTER pattern).
     *
     * @return split expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#splitExpression intent: Split expression (for SPLITTER pattern). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String splitExpression() default "";

    /**
     * Whether to stream split results.
     *
     * @return true for streaming
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#streaming intent: Whether to stream split results. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean streaming() default false;

    /**
     * Whether to process split parts in parallel.
     *
     * @return true for parallel
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#parallelProcessing intent: Whether to process split parts in parallel. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean parallelProcessing() default false;

    // --- AGGREGATION ---

    /**
     * Correlation expression (for AGGREGATOR pattern).
     *
     * @return correlation expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#correlationExpression intent: Correlation expression (for AGGREGATOR pattern). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String correlationExpression() default "";

    /**
     * Completion size for aggregation.
     *
     * @return completion size
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#completionSize intent: Completion size for aggregation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int completionSize() default 0;

    /**
     * Completion timeout in milliseconds.
     *
     * @return timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#completionTimeoutMs intent: Completion timeout in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int completionTimeoutMs() default 0;

    // --- THROTTLING ---

    /**
     * Maximum requests per time period.
     *
     * @return max requests
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#throttle intent: Maximum requests per time period. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int throttle() default 0;

    /**
     * Throttle time period in milliseconds.
     *
     * @return time period
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#throttlePeriodMs intent: Throttle time period in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1000}.
     */
    int throttlePeriodMs() default 1000;

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ICamelSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
