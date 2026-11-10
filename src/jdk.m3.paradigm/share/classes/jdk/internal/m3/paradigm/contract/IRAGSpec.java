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
 * IRAGSpec - Retrieval-Augmented Generation specification.
 *
 * <h2>RAG Patterns</h2>
 *
 * <pre>
 * RAG components:
 *   - Document chunking
 *   - Embedding generation
 *   - Vector retrieval
 *   - Context augmentation
 *   - Response generation
 *
 * &#64;IRAGSpec specifies RAG pipelines in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * &#64;IRAGSpec(
 *     collection = "knowledge-base",
 *     chunkSize = 500,
 *     chunkOverlap = 50,
 *     topK = 5,
 *     model = SONNET
 * )
 * public interface IKnowledgeBase&lt;Self extends IKnowledgeBase&lt;Self>> extends IContext&lt;Self> {
 *
 * 	&#64;IRAGSpec.Ingest(source = "documents/", fileTypes = {"pdf", "md", "txt"})
 * 	void ingestDocuments(Path directory);
 *
 * 	&#64;IRAGSpec.Query(systemPrompt = "Answer based on the context provided.", includeSourceLinks = true)
 * 	Answer askQuestion(String question);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - RAG Specification
 * @see IVectorSpec
 * @see IPromptSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares retrieval collection, chunking, ranking and generation requirements; it ingests no document, computes no embedding and invokes no model.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IRAGSpec.java}
 * blob {@code 6f29eb4098127d65d323c3fc9b78e3c56c594fd5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IRAGSpec {

    // --- VECTOR STORE ---

    /**
     * Retrieval collection identifier.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRAGSpec#collection intent: Retrieval collection identifier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String collection();

    /**
     * Requested vector-backend token from IVectorSpec.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRAGSpec#vectorProvider intent: Requested vector-backend token from IVectorSpec. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code IVectorSpec.Provider}; default expression {@code IVectorSpec.Provider.PINECONE}.
     */
    IVectorSpec.Provider vectorProvider() default IVectorSpec.Provider.PINECONE;

    // --- CHUNKING ---

    /**
     * Requested document chunk size; the schema does not define units.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRAGSpec#chunkSize intent: Requested document chunk size; the schema does not define units. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 500}.
     */
    int chunkSize() default 500;

    /**
     * Requested overlap between chunks; the schema does not define units or validate its relationship to chunkSize.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRAGSpec#chunkOverlap intent: Requested overlap between chunks; the schema does not define units or validate its relationship to chunkSize. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 50}.
     */
    int chunkOverlap() default 50;

    /**
     * Requested document partitioning strategy.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRAGSpec#chunkStrategy intent: Requested document partitioning strategy. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ChunkStrategy}; default expression {@code ChunkStrategy.RECURSIVE}.
     */
    ChunkStrategy chunkStrategy() default ChunkStrategy.RECURSIVE;

    /**
     * Document partitioning choices; selection performs no operation or dependency resolution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IRAGSpec.ChunkStrategy choices described as Document partitioning choices; selection performs no operation or dependency resolution. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum ChunkStrategy {
        /**
         * Requested fixed-size chunks category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRAGSpec.ChunkStrategy#FIXED_SIZE policy choice: Requested fixed-size chunks category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRAGSpec.ChunkStrategy; its declared order and identity are preserved.
         */
        FIXED_SIZE,
        /**
         * Requested recursive splitting category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRAGSpec.ChunkStrategy#RECURSIVE policy choice: Requested recursive splitting category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRAGSpec.ChunkStrategy; its declared order and identity are preserved.
         */
        RECURSIVE,
        /**
         * Requested semantic grouping category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRAGSpec.ChunkStrategy#SEMANTIC policy choice: Requested semantic grouping category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRAGSpec.ChunkStrategy; its declared order and identity are preserved.
         */
        SEMANTIC,
        /**
         * Requested sentence splitting category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRAGSpec.ChunkStrategy#SENTENCE policy choice: Requested sentence splitting category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRAGSpec.ChunkStrategy; its declared order and identity are preserved.
         */
        SENTENCE,
        /**
         * Requested paragraph splitting category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRAGSpec.ChunkStrategy#PARAGRAPH policy choice: Requested paragraph splitting category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRAGSpec.ChunkStrategy; its declared order and identity are preserved.
         */
        PARAGRAPH
    }

    // --- RETRIEVAL ---

    /**
     * Requested retrieval result count.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRAGSpec#topK intent: Requested retrieval result count. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 5}.
     */
    int topK() default 5;

    /**
     * Requested similarity cutoff; score normalization is not specified here.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRAGSpec#similarityThreshold intent: Requested similarity cutoff; score normalization is not specified here. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.7}.
     */
    double similarityThreshold() default 0.7;

    /**
     * Requested retrieval strategy.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRAGSpec#retrieval intent: Requested retrieval strategy. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code RetrievalStrategy}; default expression {@code RetrievalStrategy.SIMILARITY}.
     */
    RetrievalStrategy retrieval() default RetrievalStrategy.SIMILARITY;

    /**
     * Retrieval choices; selection performs no operation or dependency resolution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IRAGSpec.RetrievalStrategy choices described as Retrieval choices; selection performs no operation or dependency resolution. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum RetrievalStrategy {
        /**
         * Requested similarity ranking category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRAGSpec.RetrievalStrategy#SIMILARITY policy choice: Requested similarity ranking category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRAGSpec.RetrievalStrategy; its declared order and identity are preserved.
         */
        SIMILARITY,
        /**
         * Requested maximum marginal relevance category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRAGSpec.RetrievalStrategy#MMR policy choice: Requested maximum marginal relevance category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRAGSpec.RetrievalStrategy; its declared order and identity are preserved.
         */
        MMR,
        /**
         * Requested combined retrieval category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRAGSpec.RetrievalStrategy#HYBRID policy choice: Requested combined retrieval category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRAGSpec.RetrievalStrategy; its declared order and identity are preserved.
         */
        HYBRID,
        /**
         * Requested reranking category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IRAGSpec.RetrievalStrategy#RERANK policy choice: Requested reranking category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IRAGSpec.RetrievalStrategy; its declared order and identity are preserved.
         */
        RERANK
    }

    // --- GENERATION ---

    /**
     * Preferred generation-model token.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRAGSpec#model intent: Preferred generation-model token. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;

    /**
     * Requested generation context text; it is not a host instruction.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRAGSpec#systemPrompt intent: Requested generation context text; it is not a host instruction. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String systemPrompt() default "";

    // --- INGEST (nested) ---

    /**
     * Declares method-level ingestion source, file-type and traversal preferences; it reads no document.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares method-level ingestion source, file-type and traversal preferences; it reads no document.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Ingest {

        /**
         * Requested ingestion source identifier.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IRAGSpec.Ingest#source intent: Requested ingestion source identifier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String source() default "";

        /**
         * Requested source-file type labels.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IRAGSpec.Ingest#fileTypes intent: Requested source-file type labels. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {"txt", "md", "pdf"}}.
         */
        String[] fileTypes() default {"txt", "md", "pdf"};

        /**
         * Whether recursive source traversal is requested.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IRAGSpec.Ingest#recursive intent: Whether recursive source traversal is requested. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean recursive() default true;
    }

    // --- QUERY (nested) ---

    /**
     * Declares query generation context and response-source preferences; it performs no retrieval.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares query generation context and response-source preferences; it performs no retrieval.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Query {

        /**
         * Query-specific generation context text.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IRAGSpec.Query#systemPrompt intent: Query-specific generation context text. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String systemPrompt() default "";

        /**
         * Whether source references should be included in the response.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IRAGSpec.Query#includeSourceLinks intent: Whether source references should be included in the response. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean includeSourceLinks() default true;

        /**
         * Requested context-window size; the schema defines no unit.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IRAGSpec.Query#contextWindow intent: Requested context-window size; the schema defines no unit. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 4000}.
         */
        int contextWindow() default 4000;
    }

    // --- DESCRIPTION ---

    /**
     * Natural-language retrieval pipeline specification.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IRAGSpec#value intent: Natural-language retrieval pipeline specification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";
}
