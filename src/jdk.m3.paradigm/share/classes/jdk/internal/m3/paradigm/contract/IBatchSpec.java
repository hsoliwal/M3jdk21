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
 * IBatchSpec - Batch processing specification for AI code generation.
 *
 * <h2>Batch Patterns</h2>
 *
 * <pre>
 * Batch frameworks:
 *   - Spring Batch
 *   - JSR-352 (JBatch)
 *   - Parallel streams
 *   - Fork/Join
 *   - Virtual thread batching
 *
 * &#64;IBatchSpec specifies batch processing in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IDataProcessor {
 *
 *     &#64;IBatchSpec(chunkSize = 100, parallelism = 4, reader = "fileReader", processor = "transformProcessor", writer = "dbWriter")
 *     void processLargeFile(Path input);
 *
 * &#64;IBatchSpec(option = PARALLEL_STREAM, chunkSize = 1000)
 * List&lt;Result> processBatch(List&lt;Item> items);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Batch Specification
 * @see IExecSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares batch execution, chunking, processing identifiers, failure handling and restart preferences; it reads or writes no item and starts no job.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IBatchSpec.java}
 * blob {@code a51f9ca3343ce28df7e04a30e828babc8c5abec8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IBatchSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Requested batch execution preset.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#option intent: Requested batch execution preset. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /**
     * Batch execution choices; selection performs no operation or dependency resolution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IBatchSpec.Option choices described as Batch execution choices; selection performs no operation or dependency resolution. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Option {
        /**
         * Requested no preset category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBatchSpec.Option#NONE policy choice: Requested no preset category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBatchSpec.Option; its declared order and identity are preserved.
         */
        NONE,
        /**
         * Requested sequential processing category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBatchSpec.Option#SEQUENTIAL policy choice: Requested sequential processing category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBatchSpec.Option; its declared order and identity are preserved.
         */
        SEQUENTIAL,
        /**
         * Requested parallel Java streams category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBatchSpec.Option#PARALLEL_STREAM policy choice: Requested parallel Java streams category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBatchSpec.Option; its declared order and identity are preserved.
         */
        PARALLEL_STREAM,
        /**
         * Requested fork/join processing category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBatchSpec.Option#FORK_JOIN policy choice: Requested fork/join processing category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBatchSpec.Option; its declared order and identity are preserved.
         */
        FORK_JOIN,
        /**
         * Requested virtual-thread processing category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBatchSpec.Option#VIRTUAL_THREADS policy choice: Requested virtual-thread processing category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBatchSpec.Option; its declared order and identity are preserved.
         */
        VIRTUAL_THREADS,
        /**
         * Requested Spring Batch processing category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBatchSpec.Option#SPRING_BATCH policy choice: Requested Spring Batch processing category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBatchSpec.Option; its declared order and identity are preserved.
         */
        SPRING_BATCH
    }

    // --- CHUNK PROCESSING ---

    /**
     * Requested processing chunk size.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#chunkSize intent: Requested processing chunk size. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 100}.
     */
    int chunkSize() default 100;

    /**
     * Requested parallelism; the default negative sentinel is not interpreted here.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#parallelism intent: Requested parallelism; the default negative sentinel is not interpreted here. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code -1}.
     */
    int parallelism() default -1;

    // --- READER/PROCESSOR/WRITER ---

    /**
     * Requested reader identifier.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#reader intent: Requested reader identifier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String reader() default "";

    /**
     * Requested item-processor identifier.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#processor intent: Requested item-processor identifier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String processor() default "";

    /**
     * Requested output-writer identifier.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#writer intent: Requested output-writer identifier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String writer() default "";

    // --- ERROR HANDLING ---

    /**
     * Requested maximum skipped failures.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#skipLimit intent: Requested maximum skipped failures. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int skipLimit() default 0;

    /**
     * Requested retry limit.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#retryLimit intent: Requested retry limit. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int retryLimit() default 0;

    /**
     * Throwable classes eligible for skipping.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#skippableExceptions intent: Throwable classes eligible for skipping. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}; default expression {@code {}}.
     */
    Class<? extends Throwable>[] skippableExceptions() default {};

    /**
     * Throwable classes eligible for retries.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#retryableExceptions intent: Throwable classes eligible for retries. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}; default expression {@code {}}.
     */
    Class<? extends Throwable>[] retryableExceptions() default {};

    // --- RESTART ---

    /**
     * Whether restart support is requested.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#restartable intent: Whether restart support is requested. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean restartable() default true;

    /**
     * Requested checkpoint-policy identifier.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#checkpointPolicy intent: Requested checkpoint-policy identifier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "item"}.
     */
    String checkpointPolicy() default "item";

    // --- DESCRIPTION ---

    /**
     * Natural-language batch-processing specification.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#value intent: Natural-language batch-processing specification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    /**
     * Preferred generation-model token.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBatchSpec#model intent: Preferred generation-model token. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
