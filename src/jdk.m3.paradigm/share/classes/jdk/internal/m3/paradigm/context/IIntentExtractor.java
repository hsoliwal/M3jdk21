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
package jdk.internal.m3.paradigm.context;

import jdk.internal.m3.collections.PlaceholderLogger;

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.util.List;
import java.util.Optional;

/**
 * Intent Extractor - Extracts user intent from text using Ollama.
 *
 * <p>Following SOLID principles:
 * <ul>
 *   <li>S - Single responsibility: only extracts intent</li>
 *   <li>O - Open for extension via IntentClassifier</li>
 *   <li>L - Liskov substitution: any implementation works</li>
 *   <li>I - Interface segregation: focused interface</li>
 *   <li>D - Depends on abstractions (this interface)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * IIntentExtractor extractor = IntentExtractor.create();
 *
 * ExtractedIntent intent = extractor.extract(
 *     "Help me implement a retry mechanism for the API calls"
 * );
 *
 * log.info("Action: {}", intent.action());    // IMPLEMENT
 * log.info("Target: {}", intent.target());    // retry mechanism
 * log.info("Context: {}", intent.context());  // API calls
 * }</pre>
 *
 * @since Grand Java Patterns Proof - Skill Section
 */
@ISpec(
    value = "Intent extractor - extracts action/target/context from text using Ollama",
    constraints = {
        "SOLID compliant: S - single responsibility",
        "Extensible via registerClassifier()",
        "Local processing via Ollama"
    },
    invariants = {
        "extract() returns non-null ExtractedIntent",
        "extractMultiple() handles longer text",
        "classify() returns IntentCategory"
    },
    examples = {
        "extractor.extract(text)",
        "extractor.classify(text)",
        "extractor.registerClassifier(classifier)"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.BETA
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/skill/IIntentExtractor.java}
 * blob {@code 9e444840db37abc8f5ba254fdc397005a870c3da} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IIntentExtractor {
    PlaceholderLogger log = PlaceholderLogger.of(IIntentExtractor.class);

    /**
     * Extract intent from text.
     *
     * @param text input text to analyze
     * @return extracted intent
     */
    ExtractedIntent extract(String text);

    /**
     * Extract multiple intents from longer text.
     *
     * @param text input text
     * @return list of extracted intents
     */
    List<ExtractedIntent> extractMultiple(String text);

    /**
     * Classify intent into a category.
     *
     * @param text input text
     * @return intent category
     */
    IntentCategory classify(String text);

    /**
     * Extracted intent structure.
     *
     * @param action the action being requested (verb)
     * @param target what the action is applied to (noun)
     * @param context surrounding context
     * @param category intent category
     * @param confidence confidence score (0-1)
     * @param original original text
     */
    record ExtractedIntent(
            String action,
            String target,
            String context,
            IntentCategory category,
            double confidence,
            String original
    ) {
        /**
         * Get formatted intent string.
         *
         * @return formatted intent
         */
        public String formatted() {
            return String.format("%s %s (%s)", action, target, context);
        }
    }

    /** Intent categories for classification. */
    enum IntentCategory {
        /** User wants to create something new */
        CREATE,
        /** User wants to modify existing code */
        MODIFY,
        /** User wants to fix a bug */
        FIX,
        /** User wants to understand something */
        UNDERSTAND,
        /** User wants to search/find something */
        SEARCH,
        /** User wants to refactor code */
        REFACTOR,
        /** User wants to test something */
        TEST,
        /** User wants to deploy/release */
        DEPLOY,
        /** User wants documentation */
        DOCUMENT,
        /** User wants to review code */
        REVIEW,
        /** User is asking a question */
        QUESTION,
        /** User is providing feedback */
        FEEDBACK,
        /** Unknown intent */
        UNKNOWN
    }

    /**
     * Register a custom intent classifier.
     *
     * @param classifier custom classifier
     */
    void registerClassifier(IntentClassifier classifier);

    /**
     * Custom intent classifier interface.
     * Open for extension (SOLID - O).
     */
    @FunctionalInterface
    interface IntentClassifier {
        /**
         * Classify text into category.
         *
         * @param text input text
         * @return category if classified, empty if not applicable
         */
        Optional<IntentCategory> classify(String text);
    }
}
