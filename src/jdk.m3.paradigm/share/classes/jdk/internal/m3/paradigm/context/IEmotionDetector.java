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
import java.util.Map;
import java.util.Optional;

/**
 * Emotion Detector - Detects emotional tone from text using Ollama.
 *
 * <p>Following SOLID principles:
 * <ul>
 *   <li>S - Single responsibility: only detects emotions</li>
 *   <li>O - Open for extension via custom classifiers</li>
 *   <li>L - Liskov substitution: any implementation works</li>
 *   <li>I - Interface segregation: focused interface</li>
 *   <li>D - Depends on abstractions (this interface)</li>
 * </ul>
 *
 * <h2>Emotion Model</h2>
 * <pre>
 * Primary emotions (Ekman model):
 * - HAPPY, SAD, ANGRY, FEARFUL, SURPRISED, DISGUSTED
 *
 * Session-specific emotions:
 * - FRUSTRATED, SATISFIED, CONFUSED, CONFIDENT, CURIOUS
 *
 * Intensity levels:
 * - LOW (subtle), MEDIUM (clear), HIGH (strong)
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * IEmotionDetector detector = EmotionDetector.create();
 *
 * // Single emotion detection
 * EmotionResult result = detector.detect(
 *     "This is taking forever! Nothing works!"
 * );
 * log.info(result.primary());     // FRUSTRATED
 * log.info(result.intensity());   // HIGH
 * log.info(result.confidence());  // 0.92
 *
 * // Multi-emotion analysis
 * List<EmotionResult> emotions = detector.detectAll(
 *     "I'm happy it finally works, but worried about performance"
 * );
 * // Returns: [HAPPY, WORRIED]
 *
 * // Session analysis
 * SessionEmotion session = detector.analyzeSession(messages);
 * log.info(session.dominantEmotion());
 * log.info(session.emotionalArc());
 * }</pre>
 *
 * <h2>Implementation Notes</h2>
 * <pre>
 * Uses Ollama with models:
 * - mistral for emotion detection (best at nuance)
 * - llama3.2 for session-level analysis
 *
 * Processing is LOCAL - no cloud API calls.
 * CPU cycles over tokens.
 * </pre>
 *
 * @since Grand Java Patterns Proof - Skill Section
 */
@ISpec(
    value = "Emotion detector - Ekman model + session emotions using Ollama (mistral)",
    constraints = {
        "SOLID compliant: S - single responsibility",
        "Extensible via registerClassifier()",
        "Local processing via Ollama (mistral model)"
    },
    invariants = {
        "detect() returns non-null EmotionResult",
        "detectAll() returns list of detected emotions",
        "analyzeSession() produces SessionEmotion"
    },
    examples = {
        "detector.detect(text)",
        "detector.analyzeSession(messages)",
        "detector.detectTransition(before, after)"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.BETA
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/skill/IEmotionDetector.java}
 * blob {@code aaa4a24f41566025b9a5dd92d9aa8f5384d65ae3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IEmotionDetector {
    PlaceholderLogger log = PlaceholderLogger.of(IEmotionDetector.class);

    /**
     * Detect primary emotion from text.
     *
     * @param text input text to analyze
     * @return emotion result
     */
    EmotionResult detect(String text);

    /**
     * Detect all emotions from text.
     *
     * @param text input text
     * @return list of detected emotions with confidence
     */
    List<EmotionResult> detectAll(String text);

    /**
     * Analyze emotional tone of a conversation/session.
     *
     * @param messages list of messages to analyze
     * @return session emotion analysis
     */
    SessionEmotion analyzeSession(List<String> messages);

    /**
     * Detect emotion change between two messages.
     *
     * @param before previous message
     * @param after current message
     * @return emotion transition
     */
    EmotionTransition detectTransition(String before, String after);

    /**
     * Register custom emotion classifier.
     *
     * @param classifier custom classifier
     */
    void registerClassifier(EmotionClassifier classifier);

    // --- Emotion Types ---

    /** Primary emotion categories. */
    enum Emotion {
        // Ekman primary emotions
        /** Happiness, joy, contentment. */
        HAPPY,
        /** Sadness, disappointment. */
        SAD,
        /** Anger, annoyance. */
        ANGRY,
        /** Fear, anxiety. */
        FEARFUL,
        /** Surprise, unexpected. */
        SURPRISED,
        /** Disgust, rejection. */
        DISGUSTED,

        // Session-specific emotions
        /** Frustration, stuck. */
        FRUSTRATED,
        /** Satisfaction, accomplished. */
        SATISFIED,
        /** Confusion, unclear. */
        CONFUSED,
        /** Confidence, certainty. */
        CONFIDENT,
        /** Curiosity, exploration. */
        CURIOUS,
        /** Impatience, hurried. */
        IMPATIENT,
        /** Relief, resolved. */
        RELIEVED,
        /** Worried, concerned. */
        WORRIED,

        /** Neutral - no strong emotion. */
        NEUTRAL,
        /** Unknown - cannot classify. */
        UNKNOWN
    }

    /** Emotion intensity levels. */
    enum Intensity {
        /** Subtle, barely present. */
        LOW,
        /** Clear, noticeable. */
        MEDIUM,
        /** Strong, dominant. */
        HIGH
    }

    /** Emotion valence (positive/negative). */
    enum Valence {
        /** Positive emotion. */
        POSITIVE,
        /** Negative emotion. */
        NEGATIVE,
        /** Neutral. */
        NEUTRAL
    }

    // --- Result Types ---

    /**
     * Single emotion detection result.
     *
     * @param emotion detected emotion
     * @param intensity emotion intensity
     * @param valence positive/negative
     * @param confidence confidence score (0-1)
     * @param triggers words/phrases that triggered detection
     * @param original original text
     */
    record EmotionResult(
            Emotion emotion,
            Intensity intensity,
            Valence valence,
            double confidence,
            List<String> triggers,
            String original
    ) {
        /**
         * Check if emotion is negative.
         *
         * @return true if negative valence
         */
        public boolean isNegative() {
            return valence == Valence.NEGATIVE;
        }

        /**
         * Check if emotion is strong.
         *
         * @return true if high intensity
         */
        public boolean isStrong() {
            return intensity == Intensity.HIGH;
        }

        /**
         * Check if high confidence.
         *
         * @return true if confidence > 0.8
         */
        public boolean isConfident() {
            return confidence > 0.8;
        }
    }

    /**
     * Session-level emotion analysis.
     *
     * @param dominantEmotion most frequent/strong emotion
     * @param emotionalArc progression of emotions
     * @param averageValence overall sentiment
     * @param volatility how much emotions changed
     * @param emotionDistribution frequency of each emotion
     * @param highlights key emotional moments
     */
    record SessionEmotion(
            Emotion dominantEmotion,
            List<Emotion> emotionalArc,
            Valence averageValence,
            double volatility,
            Map<Emotion, Integer> emotionDistribution,
            List<EmotionalHighlight> highlights
    ) {
        /**
         * Check if session was frustrating.
         *
         * @return true if dominant emotion is FRUSTRATED
         */
        public boolean wasFrustrating() {
            return dominantEmotion == Emotion.FRUSTRATED;
        }

        /**
         * Check if session ended positively.
         *
         * @return true if arc ends with positive emotion
         */
        public boolean endedPositively() {
            if (emotionalArc.isEmpty()) {
                return false;
            }
            Emotion last = emotionalArc.get(emotionalArc.size() - 1);
            return last == Emotion.HAPPY || last == Emotion.SATISFIED ||
                   last == Emotion.RELIEVED || last == Emotion.CONFIDENT;
        }
    }

    /**
     * Emotional highlight - key moment in session.
     *
     * @param messageIndex which message
     * @param emotion detected emotion
     * @param intensity emotion intensity
     * @param snippet text snippet
     */
    record EmotionalHighlight(
            int messageIndex,
            Emotion emotion,
            Intensity intensity,
            String snippet
    ) {}

    /**
     * Emotion transition between messages.
     *
     * @param fromEmotion previous emotion
     * @param toEmotion current emotion
     * @param direction transition direction
     * @param trigger what caused the transition
     */
    record EmotionTransition(
            Emotion fromEmotion,
            Emotion toEmotion,
            Direction direction,
            String trigger
    ) {
        /** Transition direction. */
        public enum Direction {
            /** Improving - negative to positive. */
            IMPROVING,
            /** Declining - positive to negative. */
            DECLINING,
            /** Stable - same valence. */
            STABLE,
            /** Volatile - rapid changes. */
            VOLATILE
        }

        /**
         * Check if emotions improved.
         *
         * @return true if improving
         */
        public boolean isImproving() {
            return direction == Direction.IMPROVING;
        }
    }

    // --- Extension Interface ---

    /**
     * Custom emotion classifier interface.
     * Open for extension (SOLID - O).
     */
    @FunctionalInterface
    interface EmotionClassifier {
        /**
         * Classify text emotion.
         *
         * @param text input text
         * @return emotion if classified, empty if not applicable
         */
        Optional<EmotionResult> classify(String text);
    }
}
