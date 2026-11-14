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

package jdk.internal.m3.paradigm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Model - Specifies which AI model should generate the implementation.
 *
 * <h2>IOP Principle: Model Selection for Code Generation</h2>
 *
 * <pre>
 * +-------------------------------------------------------------------------+
 * |  MODEL SPECIFICATION                                                    |
 * +-------------------------------------------------------------------------+
 * |                                                                         |
 * |  "Different models excel at different tasks.                            |
 * |   The spec author can specify the preferred model."                     |
 * |                                                                         |
 * |  Model Hierarchy:                                                       |
 * |    OPUS   - Complex architecture, critical code, security               |
 * |    SONNET - General implementation, refactoring                         |
 * |    HAIKU  - Simple tasks, boilerplate, formatting                       |
 * |    OLLAMA - Local processing, bulk operations, scaffolding              |
 * |                                                                         |
 * |  Selection Criteria:                                                    |
 * |    - Complexity of the task                                             |
 * |    - Security sensitivity                                               |
 * |    - Cost/token budget                                                  |
 * |    - Latency requirements                                               |
 * |                                                                         |
 * +-------------------------------------------------------------------------+
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	// Specify model on interface (for implementation generation)
 * 	&#64;Model(ModelType.OPUS)
 * 	public interface ISecurityService {
 * 		// Complex security logic - needs Opus
 * 	}
 *
 * 	&#64;Model(ModelType.HAIKU)
 * 	public interface IDataTransferObject {
 * 		// Simple DTO - Haiku is sufficient
 * 	}
 *
 * 	// Method-level override
 * 	public interface IMixedService {
 *
 * 		&#64;Model(ModelType.OPUS)
 * 		Result criticalOperation(Request req); // Needs Opus
 *
 * 		&#64;Model(ModelType.HAIKU)
 * 		String formatOutput(Data data); // Haiku is enough
 * 	}
 *
 * 	// With constraints
 * 	&#64;Model(value = ModelType.SONNET, fallback = ModelType.OLLAMA)
 * 	public interface IUserService {
 * 		// Use Sonnet, fall back to Ollama if unavailable
 * 	}
 * }
 * </pre>
 *
 * <h2>Model Selection Matrix</h2>
 *
 * <pre>
 * | Task Type              | Recommended Model |
 * |------------------------|-------------------|
 * | Security-critical      | OPUS              |
 * | Architecture decisions | OPUS              |
 * | Complex algorithms     | OPUS              |
 * | General implementation | SONNET            |
 * | Refactoring            | SONNET            |
 * | Code review            | SONNET            |
 * | Boilerplate/scaffolds  | HAIKU             |
 * | Simple transformations | HAIKU             |
 * | Bulk processing        | OLLAMA            |
 * | Local-only tasks       | OLLAMA            |
 * </pre>
 *
 * @since ContextOS 1.0 - IOP Model Selection
 * @see SpecHash
 * @see AIGenClass
 * @see AIGenMethod
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Model.java}
 * blob {@code 9475dbf00f81ba9d47a8c8845bf65327343b4bd2} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Model {

    /**
     * The preferred model for generation.
     *
     * @return model type
     */
    ModelType value();

    /**
     * Fallback model if preferred is unavailable.
     *
     * @return fallback model type
     */
    ModelType fallback() default ModelType.DEFAULT;

    /**
     * Reason for model selection (documentation).
     *
     * @return reason string
     */
    String reason() default "";

    /**
     * Maximum tokens to use for generation. 0 means use model default.
     *
     * @return max tokens
     */
    int maxTokens() default 0;

    /**
     * Temperature for generation (0.0-1.0). -1 means use model default.
     *
     * @return temperature
     */
    double temperature() default -1.0;

    /** Model types available for code generation. */
    enum ModelType {
        /** Use the default model (typically Sonnet). */
        DEFAULT,

        /**
         * Claude Opus - Most capable, highest quality. Use for: complex architecture,
         * security-critical code, nuanced decisions, critical implementations.
         */
        OPUS,

        /**
         * Claude Sonnet - Balanced capability and cost. Use for: general
         * implementation, refactoring, code review, standard tasks.
         */
        SONNET,

        /**
         * Claude Haiku - Fast and efficient. Use for: simple tasks, boilerplate,
         * formatting, straightforward transformations.
         */
        HAIKU,

        /**
         * Ollama (local) - No cloud, no cost. Use for: bulk processing, scaffolding,
         * local-only operations, token conservation.
         */
        OLLAMA,

        /**
         * OpenNLP - CPU-based NLP (not LLM). Use for: tokenization, POS tagging, NER,
         * sentence detection, basic NLP tasks.
         */
        OPENNLP
    }
}
