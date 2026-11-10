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
 * IVectorSpec - Vector/Embedding specification for AI code generation.
 *
 * <h2>Vector Patterns</h2>
 *
 * <pre>
 * Vector databases:
 *   - Pinecone
 *   - Weaviate
 *   - Milvus
 *   - Chroma
 *   - Qdrant
 *   - pgvector (PostgreSQL)
 *
 * &#64;IVectorSpec specifies vector operations for RAG and semantic search.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IDocumentStore {
 *
 *     &#64;IVectorSpec(collection = "documents", embeddingModel = "text-embedding-3-small", dimensions = 1536)
 *     void storeDocument(String id, String content);
 *
 * &#64;IVectorSpec(collection = "documents", operation = SIMILARITY_SEARCH, topK = 5, threshold = 0.7)
 * List&lt;Document> searchSimilar(String query);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Vector Specification
 * @see IPromptSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares vector storage/search operation, model, dimensions, filters and comparison/backend tokens; it contacts no service and modifies no collection.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IVectorSpec.java}
 * blob {@code 82d4b5d3bcfdc2529a5f73241dc1003fc1d1c9ce} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IVectorSpec {

    // --- COLLECTION ---

    /**
     * Vector collection identifier.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#collection intent: Vector collection identifier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String collection() default "";

    /**
     * Namespace selector within a vector store.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#namespace intent: Namespace selector within a vector store. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String namespace() default "";

    // --- EMBEDDING ---

    /**
     * Requested embedding-model identifier; the annotation establishes no deployed availability.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#embeddingModel intent: Requested embedding-model identifier; the annotation establishes no deployed availability. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "text-embedding-3-small"}.
     */
    String embeddingModel() default "text-embedding-3-small";

    /**
     * Requested vector dimensionality; compatibility with the model is not validated.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#dimensions intent: Requested vector dimensionality; compatibility with the model is not validated. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1536}.
     */
    int dimensions() default 1536;

    // --- OPERATION ---

    /**
     * Requested vector-store operation category.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#operation intent: Requested vector-store operation category. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Operation}; default expression {@code Operation.UPSERT}.
     */
    Operation operation() default Operation.UPSERT;

    /**
     * Vector-store operations; selection performs no operation or dependency resolution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IVectorSpec.Operation choices described as Vector-store operations; selection performs no operation or dependency resolution. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum Operation {
        /**
         * Requested insert or update category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Operation#UPSERT policy choice: Requested insert or update category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Operation; its declared order and identity are preserved.
         */
        UPSERT,
        /**
         * Requested deletion category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Operation#DELETE policy choice: Requested deletion category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Operation; its declared order and identity are preserved.
         */
        DELETE,
        /**
         * Requested similarity search category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Operation#SIMILARITY_SEARCH policy choice: Requested similarity search category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Operation; its declared order and identity are preserved.
         */
        SIMILARITY_SEARCH,
        /**
         * Requested hybrid search category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Operation#HYBRID_SEARCH policy choice: Requested hybrid search category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Operation; its declared order and identity are preserved.
         */
        HYBRID_SEARCH,
        /**
         * Requested maximum-marginal-relevance search category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Operation#MMR_SEARCH policy choice: Requested maximum-marginal-relevance search category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Operation; its declared order and identity are preserved.
         */
        MMR_SEARCH
    }

    // --- SEARCH PARAMETERS ---

    /**
     * Requested search result count.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#topK intent: Requested search result count. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 10}.
     */
    int topK() default 10;

    /**
     * Requested score cutoff; direction and normalization depend on consuming tooling.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#threshold intent: Requested score cutoff; direction and normalization depend on consuming tooling. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.0}.
     */
    double threshold() default 0.0;

    /**
     * Requested metadata-filter expression; this schema declares no filter language.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#filter intent: Requested metadata-filter expression; this schema declares no filter language. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String filter() default "";

    // --- DISTANCE METRIC ---

    /**
     * Requested vector similarity or distance category.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#metric intent: Requested vector similarity or distance category. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Metric}; default expression {@code Metric.COSINE}.
     */
    Metric metric() default Metric.COSINE;

    /**
     * Vector comparison categories; selection performs no operation or dependency resolution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IVectorSpec.Metric choices described as Vector comparison categories; selection performs no operation or dependency resolution. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 3 constants in the existing source order.
     */
    enum Metric {
        /**
         * Requested cosine comparison category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Metric#COSINE policy choice: Requested cosine comparison category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Metric; its declared order and identity are preserved.
         */
        COSINE,
        /**
         * Requested Euclidean distance category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Metric#EUCLIDEAN policy choice: Requested Euclidean distance category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Metric; its declared order and identity are preserved.
         */
        EUCLIDEAN,
        /**
         * Requested dot-product comparison category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Metric#DOT_PRODUCT policy choice: Requested dot-product comparison category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Metric; its declared order and identity are preserved.
         */
        DOT_PRODUCT
    }

    // --- PROVIDER ---

    /**
     * Requested vector-store backend token.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#provider intent: Requested vector-store backend token. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Provider}; default expression {@code Provider.PINECONE}.
     */
    Provider provider() default Provider.PINECONE;

    /**
     * Vector backend identifiers; selection performs no operation or dependency resolution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IVectorSpec.Provider choices described as Vector backend identifiers; selection performs no operation or dependency resolution. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Provider {
        /**
         * Requested Pinecone category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Provider#PINECONE policy choice: Requested Pinecone category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Provider; its declared order and identity are preserved.
         */
        PINECONE,
        /**
         * Requested Weaviate category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Provider#WEAVIATE policy choice: Requested Weaviate category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Provider; its declared order and identity are preserved.
         */
        WEAVIATE,
        /**
         * Requested Milvus category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Provider#MILVUS policy choice: Requested Milvus category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Provider; its declared order and identity are preserved.
         */
        MILVUS,
        /**
         * Requested Chroma category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Provider#CHROMA policy choice: Requested Chroma category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Provider; its declared order and identity are preserved.
         */
        CHROMA,
        /**
         * Requested Qdrant category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Provider#QDRANT policy choice: Requested Qdrant category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Provider; its declared order and identity are preserved.
         */
        QDRANT,
        /**
         * Requested pgvector category; execution remains the consumer responsibility.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IVectorSpec.Provider#PGVECTOR policy choice: Requested pgvector category; execution remains the consumer responsibility. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IVectorSpec.Provider; its declared order and identity are preserved.
         */
        PGVECTOR
    }

    // --- DESCRIPTION ---

    /**
     * Natural-language vector-operation specification.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#value intent: Natural-language vector-operation specification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    /**
     * Preferred code-generation model token.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IVectorSpec#model intent: Preferred code-generation model token. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
