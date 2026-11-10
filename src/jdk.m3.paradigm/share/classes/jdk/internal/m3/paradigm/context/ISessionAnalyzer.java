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

import java.nio.file.Path;
import java.util.List;

/**
 * Session Analyzer - Ollama-based session transcript analyzer.
 *
 * <p>Digests and reads session transcripts/logs and annotates them with:
 * <ul>
 *   <li>Intent - what was the user trying to do</li>
 *   <li>Emotion - frustration, satisfaction, confusion, clarity</li>
 *   <li>Heat - urgency level (cold/warm/hot)</li>
 *   <li>Direction - productive/tangent/blocked</li>
 *   <li>Correctness - accurate/hallucinated/partial</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ISessionAnalyzer analyzer = SessionAnalyzer.create();
 * SessionAnalysis analysis = analyzer.analyze(transcriptPath);
 *
 * log.info("Intent: {}", analysis.intent());
 * log.info("Emotion: {}", analysis.emotion());
 * log.info("Heat: {}", analysis.heat());
 * log.info("Direction: {}", analysis.direction());
 * log.info("Correctness: {}", analysis.correctness());
 * }</pre>
 *
 * <h2>Implementation Notes</h2>
 * <pre>
 * Uses Ollama with models:
 * - llama3.2 for general analysis
 * - mistral for emotion detection
 * - codellama for code-specific correctness
 *
 * Processing is LOCAL - no cloud API calls.
 * CPU cycles over tokens.
 * </pre>
 *
 * @since Grand Java Patterns Proof - Skill Section
 */
@ISpec(
    value = "Ollama-based session transcript analyzer - extracts intent, emotion, heat, direction",
    constraints = {
        "Processing is LOCAL - no cloud API calls",
        "CPU cycles over tokens",
        "Uses llama3.2, mistral, codellama models"
    },
    invariants = {
        "analyze() returns non-null SessionAnalysis",
        "analyzeText() processes raw text content",
        "analyzeBatch() processes multiple files in sequence"
    },
    examples = {
        "analyzer.analyze(transcriptPath)",
        "analyzer.analyzeText(text)",
        "analyzer.analyzeBatch(paths)"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.BETA
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/skill/ISessionAnalyzer.java}
 * blob {@code eba8078aa6a2ca1334e59a6f9c50e974ba18c861} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ISessionAnalyzer {
    PlaceholderLogger log = PlaceholderLogger.of(ISessionAnalyzer.class);

    /**
     * Analyze a session transcript file.
     *
     * @param transcriptPath path to the transcript file (JSONL format)
     * @return analysis result
     */
    SessionAnalysis analyze(Path transcriptPath);

    /**
     * Analyze raw transcript text.
     *
     * @param transcriptText raw transcript content
     * @return analysis result
     */
    SessionAnalysis analyzeText(String transcriptText);

    /**
     * Batch analyze multiple transcripts.
     *
     * @param transcriptPaths list of transcript paths
     * @return list of analysis results
     */
    List<SessionAnalysis> analyzeBatch(List<Path> transcriptPaths);

    /**
     * Session analysis result.
     *
     * @param intent what the user was trying to do
     * @param emotion detected emotion (frustration, satisfaction, confusion, clarity)
     * @param heat urgency level (cold, warm, hot)
     * @param direction session direction (productive, tangent, blocked)
     * @param correctness accuracy assessment (accurate, hallucinated, partial)
     * @param summary brief summary of the session
     * @param keyTopics extracted key topics
     * @param actionItems identified action items
     */
    record SessionAnalysis(
            String intent,
            Emotion emotion,
            Heat heat,
            Direction direction,
            Correctness correctness,
            String summary,
            List<String> keyTopics,
            List<String> actionItems
    ) {}

    /** Emotion classification. */
    enum Emotion {
        /** User expressed frustration */
        FRUSTRATION,
        /** User expressed satisfaction */
        SATISFACTION,
        /** User expressed confusion */
        CONFUSION,
        /** User expressed clarity */
        CLARITY,
        /** Neutral emotion */
        NEUTRAL
    }

    /** Heat (urgency) level. */
    enum Heat {
        /** Low urgency, exploratory */
        COLD,
        /** Moderate urgency, working */
        WARM,
        /** High urgency, deadline-driven */
        HOT
    }

    /** Session direction classification. */
    enum Direction {
        /** Making progress toward goal */
        PRODUCTIVE,
        /** Went off on a tangent */
        TANGENT,
        /** Stuck, not making progress */
        BLOCKED
    }

    /** Correctness assessment. */
    enum Correctness {
        /** AI responses were accurate */
        ACCURATE,
        /** AI hallucinated information */
        HALLUCINATED,
        /** Partially correct responses */
        PARTIAL
    }
}
