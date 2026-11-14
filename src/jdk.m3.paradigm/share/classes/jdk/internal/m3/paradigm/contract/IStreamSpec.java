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
 * IStreamSpec - Stream processing specification for AI code generation.
 *
 * <h2>Stream Patterns</h2>
 *
 * <pre>
 * Streaming systems:
 *   - Kafka Streams
 *   - Apache Flink
 *   - Spark Streaming
 *   - Reactive Streams (Flow)
 *   - Project Reactor
 *   - RxJava
 *
 * &#64;IStreamSpec specifies stream processing in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IEventProcessor {
 *
 *     &#64;IStreamSpec(source = "orders-topic", sink = "processed-orders", operations = {FILTER, MAP,
 * AGGREGATE}, windowSize = 60000)
 * void processOrderStream();
 *
 * &#64;IStreamSpec(option = REACTIVE, backpressure = BUFFER)
 * Flux&lt;Result> processReactive(Flux&lt;Input> input);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Stream Specification
 * @see IEventSpec
 * @see IBatchSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares stream endpoints, operations, windows, pressure handling and parallelism preferences; it connects no stream and provides no unit interpretation for window values.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IStreamSpec.java}
 * blob {@code bdb789d017f19600396064190e9649ca12bad12e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IStreamSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Requested stream-processing backend preset.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#option intent: Requested stream-processing backend preset. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /**
     * Option.
     * @author Synexia
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IStreamSpec.Option choices described as Backend preset choices; selecting a token allocates no processing state. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Option {
        /**
         * Requested no preset choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Option#NONE policy choice: Requested no preset choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Option; its declared order and identity are preserved.
         */
        NONE,
        /**
         * Requested Kafka choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Option#KAFKA policy choice: Requested Kafka choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Option; its declared order and identity are preserved.
         */
        KAFKA,
        /**
         * Requested reactive streams choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Option#REACTIVE policy choice: Requested reactive streams choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Option; its declared order and identity are preserved.
         */
        REACTIVE,
        /**
         * Requested Java streams choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Option#JAVA_STREAM policy choice: Requested Java streams choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Option; its declared order and identity are preserved.
         */
        JAVA_STREAM,
        /**
         * Requested Flink choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Option#FLINK policy choice: Requested Flink choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Option; its declared order and identity are preserved.
         */
        FLINK,
        /**
         * Requested Spark choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Option#SPARK policy choice: Requested Spark choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Option; its declared order and identity are preserved.
         */
        SPARK
    }

    // --- SOURCE & SINK ---

    /**
     * Primary source identifier; the schema defines no URI syntax.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#source intent: Primary source identifier; the schema defines no URI syntax. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String source() default "";

    /**
     * Output sink identifier; the annotation opens no destination.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#sink intent: Output sink identifier; the annotation opens no destination. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String sink() default "";

    /**
     * Additional source identifiers for the requested stream.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#sources intent: Additional source identifiers for the requested stream. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] sources() default {};

    // --- OPERATIONS ---

    /**
     * Ordered operation-category tokens requested for the pipeline.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#operations intent: Ordered operation-category tokens requested for the pipeline. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Operation[]}; default expression {@code {}}.
     */
    Operation[] operations() default {};

    /**
     * Pipeline operation categories; selecting a token allocates no processing state.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IStreamSpec.Operation choices described as Pipeline operation categories; selecting a token allocates no processing state. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 9 constants in the existing source order.
     */
    enum Operation {
        /**
         * Requested filtering choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Operation#FILTER policy choice: Requested filtering choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Operation; its declared order and identity are preserved.
         */
        FILTER,
        /**
         * Requested one-to-one mapping choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Operation#MAP policy choice: Requested one-to-one mapping choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Operation; its declared order and identity are preserved.
         */
        MAP,
        /**
         * Requested flattening mapped results choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Operation#FLAT_MAP policy choice: Requested flattening mapped results choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Operation; its declared order and identity are preserved.
         */
        FLAT_MAP,
        /**
         * Requested reduction choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Operation#REDUCE policy choice: Requested reduction choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Operation; its declared order and identity are preserved.
         */
        REDUCE,
        /**
         * Requested aggregation choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Operation#AGGREGATE policy choice: Requested aggregation choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Operation; its declared order and identity are preserved.
         */
        AGGREGATE,
        /**
         * Requested joining streams choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Operation#JOIN policy choice: Requested joining streams choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Operation; its declared order and identity are preserved.
         */
        JOIN,
        /**
         * Requested grouping by key choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Operation#GROUP_BY policy choice: Requested grouping by key choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Operation; its declared order and identity are preserved.
         */
        GROUP_BY,
        /**
         * Requested window grouping choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Operation#WINDOW policy choice: Requested window grouping choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Operation; its declared order and identity are preserved.
         */
        WINDOW,
        /**
         * Requested duplicate elimination choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Operation#DISTINCT policy choice: Requested duplicate elimination choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Operation; its declared order and identity are preserved.
         */
        DISTINCT
    }

    // --- WINDOWING ---

    /**
     * Requested window-size value; this schema declares no units.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#windowSize intent: Requested window-size value; this schema declares no units. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 0}.
     */
    long windowSize() default 0;

    /**
     * Requested slide-interval value; this schema declares no units.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#slideInterval intent: Requested slide-interval value; this schema declares no units. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 0}.
     */
    long slideInterval() default 0;

    /**
     * Requested grouping-window category.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#windowType intent: Requested grouping-window category. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code WindowType}; default expression {@code WindowType.TUMBLING}.
     */
    WindowType windowType() default WindowType.TUMBLING;

    /**
     * Window grouping categories; selecting a token allocates no processing state.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IStreamSpec.WindowType choices described as Window grouping categories; selecting a token allocates no processing state. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum WindowType {
        /**
         * Requested non-overlapping windows choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.WindowType#TUMBLING policy choice: Requested non-overlapping windows choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.WindowType; its declared order and identity are preserved.
         */
        TUMBLING,
        /**
         * Requested sliding windows choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.WindowType#SLIDING policy choice: Requested sliding windows choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.WindowType; its declared order and identity are preserved.
         */
        SLIDING,
        /**
         * Requested session windows choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.WindowType#SESSION policy choice: Requested session windows choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.WindowType; its declared order and identity are preserved.
         */
        SESSION,
        /**
         * Requested global grouping choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.WindowType#GLOBAL policy choice: Requested global grouping choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.WindowType; its declared order and identity are preserved.
         */
        GLOBAL
    }

    // --- BACKPRESSURE ---

    /**
     * Requested response to downstream demand pressure.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#backpressure intent: Requested response to downstream demand pressure. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Backpressure}; default expression {@code Backpressure.BUFFER}.
     */
    Backpressure backpressure() default Backpressure.BUFFER;

    /**
     * Demand-pressure handling choices; selecting a token allocates no processing state.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IStreamSpec.Backpressure choices described as Demand-pressure handling choices; selecting a token allocates no processing state. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Backpressure {
        /**
         * Requested buffering choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Backpressure#BUFFER policy choice: Requested buffering choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Backpressure; its declared order and identity are preserved.
         */
        BUFFER,
        /**
         * Requested dropping choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Backpressure#DROP policy choice: Requested dropping choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Backpressure; its declared order and identity are preserved.
         */
        DROP,
        /**
         * Requested retaining the latest value choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Backpressure#LATEST policy choice: Requested retaining the latest value choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Backpressure; its declared order and identity are preserved.
         */
        LATEST,
        /**
         * Requested signalling an error choice; its runtime implementation belongs to consuming tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStreamSpec.Backpressure#ERROR policy choice: Requested signalling an error choice; its runtime implementation belongs to consuming tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStreamSpec.Backpressure; its declared order and identity are preserved.
         */
        ERROR
    }

    // --- PARALLELISM ---

    /**
     * Requested parallelism value; the default negative sentinel has no interpretation implemented here.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#parallelism intent: Requested parallelism value; the default negative sentinel has no interpretation implemented here. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code -1}.
     */
    int parallelism() default -1;

    /**
     * Requested buffer capacity value, defaulting to 256.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#bufferSize intent: Requested buffer capacity value, defaulting to 256. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 256}.
     */
    int bufferSize() default 256;

    // --- DESCRIPTION ---

    /**
     * Natural-language stream specification.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#value intent: Natural-language stream specification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    /**
     * Preferred generation-model token for the stream implementation.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStreamSpec#model intent: Preferred generation-model token for the stream implementation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
