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
import jdk.internal.m3.paradigm.annotation.Stability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ISessionDigestSpec - Session digest specification for LLM memory enhancement.
 *
 * <h2>THE SESSION DIGEST SPEC</h2>
 *
 * <pre>
 * "LLM memory will be so clean"
 * "Imagine no more context problems or troubles"
 * "LLMs lack context momentum"
 *
 * THE PROBLEM: LLMs can't sense where a conversation is HEADING,
 * only where it IS. They lose trajectory between turns.
 *
 * Session digest solves the LLM context window problem:
 *   1. CPU-first annotation (heat, intent, emotion, direction)
 *   2. Ollama for ambiguous cases (saves Claude tokens)
 *   3. SQLite caching for instant recall
 *   4. High-heat turns preserved when context fills
 *   5. Low-heat turns collapsed or discarded
 *   6. MOMENTUM TRACKING - is conversation accelerating or drifting?
 *   7. TRAJECTORY INFERENCE - where is this heading?
 *
 * Continuity columns:
 *   - heat (1-10): Importance score
 *   - intent (?/!//@/#): Question/Insight/Plan/Fix
 *   - emotion (+/=/-): Positive/Neutral/Negative
 *   - direction (->/&lt;-/&lt;>): Forward/Backward/Bidirectional
 *   - correctness (0.0-1.0): Confidence score
 *   - sequence: ZWJ turn linking
 *   - momentum (0.0-1.0): Progress velocity toward goal
 *   - trajectory: Inferred destination (goal/topic/completion)
 *   - drift (0.0-1.0): How far off-track from original intent
 *
 * &#64;ISessionDigestSpec marks classes that consume session digests.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Agent that uses session digest for memory
 * &#64;ISessionDigestSpec(
 *     heatThreshold = 7,
 *     maxTurns = 50,
 *     collapseStrategy = CollapseStrategy.SUMMARIZE,
 *     cacheEnabled = true
 * )
 * public interface IMemoryAwareAgent&lt;Self extends IMemoryAwareAgent&lt;Self>> extends IContext&lt;Self> {
 *
 *     &#64;ISessionDigestSpec.HighHeat(minHeat = 8)
 *     List&lt;Turn> getCriticalContext();
 *
 *     &#64;ISessionDigestSpec.Filter(intent = Intent.FIX)
 *     List&lt;Turn> getFixRelatedTurns();
 *
 *     &#64;ISessionDigestSpec.Consolidate(sessions = 5)
 *     ConsolidatedSession getRecentSessions();
 * }
 *
 * // Session-aware method
 * &#64;ISessionDigestSpec.Inject
 * public void processWithContext(SessionDigest digest, String input) {
 * 	// digest contains annotated turns with continuity columns
 * }
 * }
 * </pre>
 *
 * <h2>Anti-Drift Benefits</h2>
 *
 * <pre>
 * - Heat scoring keeps agent focused on critical turns
 * - Intent tracking maintains FIX/PLAN focus
 * - Sequence linking preserves conversation flow
 * - SQLite cache provides instant session recall
 * - Collapse strategy manages context overflow gracefully
 * - MOMENTUM tracking detects when progress stalls
 * - TRAJECTORY inference predicts where conversation is heading
 * - DRIFT alerts catch when agent goes off-rails
 * - GOAL anchoring keeps agent focused on original objective
 * </pre>
 *
 * <h2>Context Momentum (The Missing Piece)</h2>
 *
 * <pre>
 * LLMs see: [turn1] [turn2] [turn3] ... (discrete snapshots)
 * Humans see: turn1 → turn2 → turn3 ... (continuous trajectory)
 *
 * The session digest adds:
 *   - momentum: 0.0 (stalled) → 1.0 (accelerating toward goal)
 *   - trajectory: Where is this heading? (goal/topic/completion)
 *   - drift: How far off-track? (0.0 = on-track, 1.0 = lost)
 *
 * This gives LLMs the "sense of direction" they naturally lack.
 * </pre>
 *
 * <h2>Net Sum Momentum (Physics of Conversation)</h2>
 *
 * <pre>
 * "Every context has net sum moment of its inner context"
 *
 * Like physics: p_total = Σ p_i  (momentum is conserved)
 * Context:      M_total = Σ m_i  (net context momentum)
 *
 * Each turn contributes a momentum vector:
 *   +m : Accelerating (progress toward goal, answers, solutions)
 *   -m : Decelerating (confusion, tangents, errors, drift)
 *   0m : Neutral (acknowledgments, fillers, pleasantries)
 *
 * The NET SUM reveals conversation health:
 *   M_total > 0 : Progressing (keep going)
 *   M_total = 0 : Stalled (intervention needed)
 *   M_total &lt; 0 : Regressing (course correction required)
 *
 * Implementation:
 *   m_i = heat_i × direction_i × intent_weight_i
 *
 *   where:
 *     heat_i = importance (1-10 normalized to 0-1)
 *     direction_i = +1 (forward), -1 (backward), 0 (neutral)
 *     intent_weight_i = FIX(1.0), PLAN(0.8), QUESTION(0.5), STATEMENT(0.3)
 * </pre>
 *
 * <h2>Mandelbrot Context (Fractal Self-Similarity)</h2>
 *
 * <pre>
 * "This is all Mandelbrot stuff"
 *
 * Context momentum is FRACTAL - self-similar at every scale:
 *
 *   Project Level:      M_project = Σ M_session
 *        │
 *        └── Session Level:      M_session = Σ M_conversation
 *                 │
 *                 └── Conversation Level:  M_conv = Σ M_turn
 *                          │
 *                          └── Turn Level:       M_turn = Σ M_sentence
 *                                   │
 *                                   └── Sentence Level:   M_sent = Σ M_token
 *
 * The SAME momentum formula applies at EVERY level.
 * Zoom in or out - the pattern repeats infinitely.
 *
 * Why this matters:
 *   - Summarize a session → preserve its net momentum
 *   - Collapse low-heat turns → maintain Mandelbrot invariant
 *   - Expand context → unfold nested momentum vectors
 *
 * Context is fractal. Momentum is conserved across scales.
 * This is the mathematical foundation of LLM memory.
 * </pre>
 *
 * <h2>Information Theory of Context</h2>
 *
 * <pre>
 * "Every context has SNR"
 *
 * Three unified theories:
 *   Physics:    M = Σ m_i            (momentum conservation)
 *   Fractals:   M_n = Σ M_(n-1)      (Mandelbrot self-similarity)
 *   InfoTheory: SNR = Signal / Noise (Shannon channel capacity)
 *
 * Context SNR (Signal-to-Noise Ratio):
 *   Signal = Σ (high-heat × on-trajectory turns)
 *   Noise  = Σ (low-heat × drift × filler turns)
 *   SNR    = Signal / Noise
 *
 *   High SNR → clean, focused conversation (preserve)
 *   Low SNR  → noisy, unfocused conversation (collapse)
 *
 * Shannon Entropy of Context:
 *   H(C) = -Σ p(turn_i) × log₂(p(turn_i))
 *
 *   High entropy → diverse, exploratory conversation
 *   Low entropy  → focused, convergent conversation
 *
 * Mutual Information between Turns:
 *   I(T₁;T₂) = H(T₁) + H(T₂) - H(T₁,T₂)
 *
 *   High I → turns are related (keep both)
 *   Low I  → turns are independent (can collapse one)
 *
 * Channel Capacity (max info per turn):
 *   C = max I(input; output)
 *
 *   This determines optimal context compression ratio.
 *
 * THE UNIFIED FORMULA:
 *   ContextQuality = M_net × SNR × (1 - H_normalized)
 *
 *   Momentum × Signal clarity × Focus
 * </pre>
 *
 * @since IOP 1.1 - Session Digest Specification
 * @see IAgentSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares session selection, retention, scoring, drift, scale, cache and model preferences; it reads no transcript, computes no metric and exports no context.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/ISessionDigestSpec.java}
 * blob {@code f3f0b9b626fef52c9a33cbeda45db7f0f568c93c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ISessionDigestSpec {

    // --- HEAT CONFIGURATION ---

    /**
     * Minimum heat score to include in context. Turns below this threshold may be
     * collapsed or discarded.
     *
     * @return heat threshold (1-10)
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#heatThreshold intent: Minimum heat score to include in context. Turns below this threshold may be collapsed or discarded. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 5}.
     */
    int heatThreshold() default 5;

    /**
     * Maximum turns to retain in working memory.
     *
     * @return max turns
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#maxTurns intent: Maximum turns to retain in working memory. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 100}.
     */
    int maxTurns() default 100;

    // --- COLLAPSE STRATEGY ---

    /**
     * Strategy for handling context overflow.
     *
     * @return collapse strategy
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#collapseStrategy intent: Strategy for handling context overflow. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code CollapseStrategy}; default expression {@code CollapseStrategy.DISCARD_LOW_HEAT}.
     */
    CollapseStrategy collapseStrategy() default CollapseStrategy.DISCARD_LOW_HEAT;

    /** Collapse strategies.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISessionDigestSpec.CollapseStrategy choices described as Collapse strategies. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Enum containing 6 constants in the existing source order.
     */
    enum CollapseStrategy {
        /** Discard turns below heat threshold.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.CollapseStrategy#DISCARD_LOW_HEAT policy choice: Discard turns below heat threshold. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.CollapseStrategy; its declared order and identity are preserved.
         */
        DISCARD_LOW_HEAT,

        /** Summarize low-heat turns into single entry.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.CollapseStrategy#SUMMARIZE policy choice: Summarize low-heat turns into single entry. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.CollapseStrategy; its declared order and identity are preserved.
         */
        SUMMARIZE,

        /** Keep only most recent turns.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.CollapseStrategy#RECENT_ONLY policy choice: Keep only most recent turns. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.CollapseStrategy; its declared order and identity are preserved.
         */
        RECENT_ONLY,

        /** Keep only high-heat + recent turns.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.CollapseStrategy#HEAT_PLUS_RECENT policy choice: Keep only high-heat + recent turns. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.CollapseStrategy; its declared order and identity are preserved.
         */
        HEAT_PLUS_RECENT,

        /** Keep turns matching current intent.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.CollapseStrategy#INTENT_FOCUSED policy choice: Keep turns matching current intent. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.CollapseStrategy; its declared order and identity are preserved.
         */
        INTENT_FOCUSED,

        /** Custom collapse via bean.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.CollapseStrategy#CUSTOM policy choice: Custom collapse via bean. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.CollapseStrategy; its declared order and identity are preserved.
         */
        CUSTOM
    }

    /**
     * Custom collapse bean name.
     *
     * @return bean name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#collapseBean intent: Custom collapse bean name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String collapseBean() default "";

    // --- CACHING ---

    /**
     * Enable SQLite caching for session digests.
     *
     * @return true if caching enabled
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#cacheEnabled intent: Enable SQLite caching for session digests. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean cacheEnabled() default true;

    /**
     * Cache directory (default: CWD/.session-digest.db).
     *
     * @return cache path
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#cachePath intent: Cache directory (default: CWD/.session-digest.db). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String cachePath() default "";

    /**
     * Cache TTL in hours (0 = infinite).
     *
     * @return TTL hours
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#cacheTtlHours intent: Cache TTL in hours (0 = infinite). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 72}.
     */
    int cacheTtlHours() default 72;

    // --- INTENT FILTERING ---

    /**
     * Intents to prioritize.
     *
     * @return priority intents
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#priorityIntents intent: Intents to prioritize. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Intent[]}; default expression {@code {}}.
     */
    Intent[] priorityIntents() default {};

    /** Session intents (mirrors SessionDigestAnnotator.Intent).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISessionDigestSpec.Intent choices described as Session intents (mirrors SessionDigestAnnotator.Intent). Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Enum containing 5 constants in the existing source order.
     */
    enum Intent {
        /** Question turn.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.Intent#QUESTION policy choice: Question turn. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.Intent; its declared order and identity are preserved.
         */
        QUESTION,

        /** Insight/idea turn.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.Intent#INSIGHT policy choice: Insight/idea turn. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.Intent; its declared order and identity are preserved.
         */
        INSIGHT,

        /** Fix/error turn.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.Intent#FIX policy choice: Fix/error turn. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.Intent; its declared order and identity are preserved.
         */
        FIX,

        /** Plan/design turn.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.Intent#PLAN policy choice: Plan/design turn. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.Intent; its declared order and identity are preserved.
         */
        PLAN,

        /** Statement turn.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.Intent#STATEMENT policy choice: Statement turn. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.Intent; its declared order and identity are preserved.
         */
        STATEMENT
    }

    // --- MOMENTUM & TRAJECTORY (Context Momentum Tracking) ---

    /**
     * Enable momentum tracking. Momentum measures progress velocity toward the
     * stated goal.
     *
     * @return true to track momentum
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#trackMomentum intent: Enable momentum tracking. Momentum measures progress velocity toward the stated goal. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean trackMomentum() default true;

    /**
     * Minimum momentum before triggering drift warning. Below this threshold, agent
     * may be going off-track.
     *
     * @return momentum threshold (0.0-1.0)
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#momentumThreshold intent: Minimum momentum before triggering drift warning. Below this threshold, agent may be going off-track. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.3}.
     */
    double momentumThreshold() default 0.3;

    /**
     * Maximum drift before triggering course correction. Drift measures how far
     * conversation has strayed from original intent.
     *
     * @return drift threshold (0.0-1.0)
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#driftThreshold intent: Maximum drift before triggering course correction. Drift measures how far conversation has strayed from original intent. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.5}.
     */
    double driftThreshold() default 0.5;

    /**
     * Enable trajectory inference. Trajectory predicts where conversation is
     * heading based on patterns.
     *
     * @return true to infer trajectory
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#inferTrajectory intent: Enable trajectory inference. Trajectory predicts where conversation is heading based on patterns. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean inferTrajectory() default true;

    /**
     * Trajectory inference mode.
     *
     * @return inference mode
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#trajectoryMode intent: Trajectory inference mode. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code TrajectoryMode}; default expression {@code TrajectoryMode.GOAL_ORIENTED}.
     */
    TrajectoryMode trajectoryMode() default TrajectoryMode.GOAL_ORIENTED;

    /** Trajectory inference modes.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISessionDigestSpec.TrajectoryMode choices described as Trajectory inference modes. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Enum containing 5 constants in the existing source order.
     */
    enum TrajectoryMode {
        /** Track progress toward stated goal.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.TrajectoryMode#GOAL_ORIENTED policy choice: Track progress toward stated goal. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.TrajectoryMode; its declared order and identity are preserved.
         */
        GOAL_ORIENTED,

        /** Track topic evolution/drift.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.TrajectoryMode#TOPIC_TRACKING policy choice: Track topic evolution/drift. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.TrajectoryMode; its declared order and identity are preserved.
         */
        TOPIC_TRACKING,

        /** Track toward task completion.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.TrajectoryMode#COMPLETION_TRACKING policy choice: Track toward task completion. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.TrajectoryMode; its declared order and identity are preserved.
         */
        COMPLETION_TRACKING,

        /** Track user satisfaction signals.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.TrajectoryMode#SATISFACTION_TRACKING policy choice: Track user satisfaction signals. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.TrajectoryMode; its declared order and identity are preserved.
         */
        SATISFACTION_TRACKING,

        /** Hybrid of all modes.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISessionDigestSpec.TrajectoryMode#HYBRID policy choice: Hybrid of all modes. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISessionDigestSpec.TrajectoryMode; its declared order and identity are preserved.
         */
        HYBRID
    }

    // --- OLLAMA INTEGRATION ---

    /**
     * Use Ollama for ambiguous annotation.
     *
     * @return true to use Ollama
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#ollamaEnabled intent: Use Ollama for ambiguous annotation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean ollamaEnabled() default true;

    /**
     * Ollama model for ambiguous cases.
     *
     * @return model name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#ollamaModel intent: Ollama model for ambiguous cases. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "qwen2.5-coder:3b"}.
     */
    String ollamaModel() default "qwen2.5-coder:3b";

    /**
     * Ollama host.
     *
     * @return host
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#ollamaHost intent: Ollama host. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "localhost:11434"}.
     */
    String ollamaHost() default "localhost:11434";

    // --- NESTED ANNOTATIONS ---

    /** Filter for high-heat turns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a heat threshold and result limit for selecting turns; it reads or filters no conversation.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface HighHeat {

        /** Minimum heat score.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.HighHeat#minHeat intent: Minimum heat score. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 7}.
         */
        int minHeat() default 7;

        /** Maximum turns to return.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.HighHeat#limit intent: Maximum turns to return. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 50}.
         */
        int limit() default 50;
    }

    /** Filter by intent.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares intent, heat and speaker filters; it evaluates no session data and resolves no conflicting speaker flags.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Filter {

        /** Filter by intent.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Filter#intent intent: Filter by intent. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Intent}; default expression {@code Intent.STATEMENT}.
         */
        Intent intent() default Intent.STATEMENT;

        /** Filter by minimum heat.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Filter#minHeat intent: Filter by minimum heat. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1}.
         */
        int minHeat() default 1;

        /** Include only user turns.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Filter#userOnly intent: Include only user turns. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean userOnly() default false;

        /** Include only assistant turns.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Filter#assistantOnly intent: Include only assistant turns. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean assistantOnly() default false;
    }

    /** Consolidate multiple sessions.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares recent-session count and heat preference; it loads or combines no session.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Consolidate {

        /** Number of recent sessions to consolidate.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Consolidate#sessions intent: Number of recent sessions to consolidate. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 5}.
         */
        int sessions() default 5;

        /** Only include high-heat turns.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Consolidate#highHeatOnly intent: Only include high-heat turns. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean highHeatOnly() default true;
    }

    /** Inject session digest into method.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares session-pattern and heat selection for digest injection; it injects no data into a method or parameter.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.METHOD, ElementType.PARAMETER}}; existing element and enum schemas are preserved.
     */
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.SOURCE)
    @interface Inject {

        /** Session ID pattern (regex).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Inject#sessionPattern intent: Session ID pattern (regex). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ".*"}.
         */
        String sessionPattern() default ".*";

        /** Pre-filter by heat threshold.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Inject#heatThreshold intent: Pre-filter by heat threshold. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
         */
        int heatThreshold() default 0;
    }

    /** Export session digest to file.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares export format and destination path; it reads no transcript and writes or uploads no file.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Export {

        /** Export format.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Export#format intent: Export format. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code ExportFormat}; default expression {@code ExportFormat.CSV}.
         */
        ExportFormat format() default ExportFormat.CSV;

        /** Output path (relative to CWD).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Export#path intent: Output path (relative to CWD). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "session-digest.csv"}.
         */
        String path() default "session-digest.csv";

        /**
         * Serialization format choices for requested digest export.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Enumerates the ISessionDigestSpec.Export.ExportFormat choices described as Serialization format choices for requested digest export. Selection records an annotation value; execution remains the consumer responsibility.
         * @m3.dataStructures Enum containing 4 constants in the existing source order.
         */
        enum ExportFormat {
            /**
             * Requested comma-separated values export representation; no file is written.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Export.ExportFormat#CSV policy choice: Requested comma-separated values export representation; no file is written. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Export.ExportFormat; its declared order and identity are preserved.
             */
            CSV,
            /**
             * Requested JSON export representation; no file is written.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Export.ExportFormat#JSON policy choice: Requested JSON export representation; no file is written. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Export.ExportFormat; its declared order and identity are preserved.
             */
            JSON,
            /**
             * Requested JSON Lines export representation; no file is written.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Export.ExportFormat#JSONL policy choice: Requested JSON Lines export representation; no file is written. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Export.ExportFormat; its declared order and identity are preserved.
             */
            JSONL,
            /**
             * Requested Markdown export representation; no file is written.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Export.ExportFormat#MARKDOWN policy choice: Requested Markdown export representation; no file is written. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Export.ExportFormat; its declared order and identity are preserved.
             */
            MARKDOWN
        }
    }

    /**
     * Momentum - Track conversation progress velocity.
     *
     * <p>
     * Momentum captures what LLMs lack: sense of where conversation is HEADING, not
     * just where it IS. High momentum = accelerating toward goal. Low momentum =
     * drifting or stalled.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares momentum filtering and delta reporting preferences; it computes no progress score.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Momentum {

        /** Minimum momentum to include.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Momentum#minMomentum intent: Minimum momentum to include. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.0}.
         */
        double minMomentum() default 0.0;

        /** Only include turns that increased momentum.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Momentum#acceleratingOnly intent: Only include turns that increased momentum. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean acceleratingOnly() default false;

        /** Include momentum delta (change from previous).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Momentum#includeDelta intent: Include momentum delta (change from previous). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean includeDelta() default true;
    }

    /**
     * Trajectory - Infer conversation destination.
     *
     * <p>
     * Trajectory predicts where conversation is heading based on: - Stated goals
     * from early turns - Intent patterns (FIX→FIX→FIX = still fixing) - Topic
     * clustering - Completion signals
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares inference mode, confidence threshold and history preference; it infers no conversational direction.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Trajectory {

        /** Trajectory mode.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Trajectory#mode intent: Trajectory mode. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code TrajectoryMode}; default expression {@code TrajectoryMode.GOAL_ORIENTED}.
         */
        TrajectoryMode mode() default TrajectoryMode.GOAL_ORIENTED;

        /** Confidence threshold for trajectory.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Trajectory#confidence intent: Confidence threshold for trajectory. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.5}.
         */
        double confidence() default 0.5;

        /** Include trajectory history (how it changed).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Trajectory#includeHistory intent: Include trajectory history (how it changed). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean includeHistory() default false;
    }

    /**
     * DriftAlert - Trigger when conversation drifts off-track.
     *
     * <p>
     * Drift detection helps catch when agent is going off-rails: - Topic drift
     * (talking about unrelated things) - Goal drift (working on wrong objective) -
     * Scope drift (expanding beyond original request)
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares drift thresholds, categories and response preference; it detects no drift and triggers no correction or alert.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface DriftAlert {

        /** Drift threshold to trigger alert.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.DriftAlert#threshold intent: Drift threshold to trigger alert. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.5}.
         */
        double threshold() default 0.5;

        /** Drift types to monitor.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.DriftAlert#types intent: Drift types to monitor. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code DriftType[]}; default expression {@code {DriftType.TOPIC, DriftType.GOAL}}.
         */
        DriftType[] types() default {DriftType.TOPIC, DriftType.GOAL};

        /** Action when drift detected.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.DriftAlert#action intent: Action when drift detected. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code DriftAction}; default expression {@code DriftAction.WARN}.
         */
        DriftAction action() default DriftAction.WARN;

        /**
         * Categories of requested drift classification.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Enumerates the ISessionDigestSpec.DriftAlert.DriftType choices described as Categories of requested drift classification. Selection records an annotation value; execution remains the consumer responsibility.
         * @m3.dataStructures Enum containing 4 constants in the existing source order.
         */
        enum DriftType {
            /** Off-topic drift.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.DriftAlert.DriftType#TOPIC policy choice: Off-topic drift. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.DriftAlert.DriftType; its declared order and identity are preserved.
             */
            TOPIC,
            /** Wrong goal drift.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.DriftAlert.DriftType#GOAL policy choice: Wrong goal drift. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.DriftAlert.DriftType; its declared order and identity are preserved.
             */
            GOAL,
            /** Scope expansion drift.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.DriftAlert.DriftType#SCOPE policy choice: Scope expansion drift. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.DriftAlert.DriftType; its declared order and identity are preserved.
             */
            SCOPE,
            /** Repetition (going in circles).
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.DriftAlert.DriftType#REPETITION policy choice: Repetition (going in circles). This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.DriftAlert.DriftType; its declared order and identity are preserved.
             */
            REPETITION
        }

        /**
         * Requested responses to classified drift.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Enumerates the ISessionDigestSpec.DriftAlert.DriftAction choices described as Requested responses to classified drift. Selection records an annotation value; execution remains the consumer responsibility.
         * @m3.dataStructures Enum containing 4 constants in the existing source order.
         */
        enum DriftAction {
            /** Log warning.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.DriftAlert.DriftAction#WARN policy choice: Log warning. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.DriftAlert.DriftAction; its declared order and identity are preserved.
             */
            WARN,
            /** Emit AgentTip.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.DriftAlert.DriftAction#TIP policy choice: Emit AgentTip. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.DriftAlert.DriftAction; its declared order and identity are preserved.
             */
            TIP,
            /** Throw exception.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.DriftAlert.DriftAction#EXCEPTION policy choice: Throw exception. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.DriftAlert.DriftAction; its declared order and identity are preserved.
             */
            EXCEPTION,
            /** Auto-correct via Ollama.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.DriftAlert.DriftAction#AUTO_CORRECT policy choice: Auto-correct via Ollama. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.DriftAlert.DriftAction; its declared order and identity are preserved.
             */
            AUTO_CORRECT
        }
    }

    /**
     * GoalAnchor - Anchor conversation to stated goal.
     *
     * <p>
     * Extracts and preserves the original goal from early turns. All subsequent
     * turns are measured against this anchor.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares goal source index, extraction pattern and model fallback preference; it extracts no goal and invokes no model.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface GoalAnchor {

        /** Turn index to extract goal from (default: first user turn).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.GoalAnchor#turnIndex intent: Turn index to extract goal from (default: first user turn). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
         */
        int turnIndex() default 0;

        /** Goal extraction pattern (regex).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.GoalAnchor#pattern intent: Goal extraction pattern (regex). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String pattern() default "";

        /** Use Ollama to extract goal if pattern fails.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.GoalAnchor#ollamaFallback intent: Use Ollama to extract goal if pattern fails. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean ollamaFallback() default true;
    }

    /**
     * Scale - Mandelbrot fractal scale operations.
     *
     * <p>
     * Context momentum is fractal - self-similar at every scale. This annotation
     * enables operations across scales while preserving the Mandelbrot invariant
     * (net momentum is conserved).
     *
     * <pre>
     * M_project = Σ M_session = Σ Σ M_conv = Σ Σ Σ M_turn = ...
     * </pre>
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares digest aggregation level, operation and momentum-preservation preference; it transforms no context or score.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Scale {

        /** Scale level to operate at.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Scale#level intent: Scale level to operate at. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code ScaleLevel}; default expression {@code ScaleLevel.TURN}.
         */
        ScaleLevel level() default ScaleLevel.TURN;

        /** Operation to perform.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Scale#operation intent: Operation to perform. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code ScaleOperation}; default expression {@code ScaleOperation.AGGREGATE}.
         */
        ScaleOperation operation() default ScaleOperation.AGGREGATE;

        /** Preserve momentum when collapsing.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Scale#preserveMomentum intent: Preserve momentum when collapsing. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean preserveMomentum() default true;

        /**
         * Context aggregation levels for requested scale operations.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Enumerates the ISessionDigestSpec.Scale.ScaleLevel choices described as Context aggregation levels for requested scale operations. Selection records an annotation value; execution remains the consumer responsibility.
         * @m3.dataStructures Enum containing 6 constants in the existing source order.
         */
        enum ScaleLevel {
            /** Individual token level.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Scale.ScaleLevel#TOKEN policy choice: Individual token level. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Scale.ScaleLevel; its declared order and identity are preserved.
             */
            TOKEN,
            /** Sentence level.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Scale.ScaleLevel#SENTENCE policy choice: Sentence level. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Scale.ScaleLevel; its declared order and identity are preserved.
             */
            SENTENCE,
            /** Turn level (default).
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Scale.ScaleLevel#TURN policy choice: Turn level (default). This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Scale.ScaleLevel; its declared order and identity are preserved.
             */
            TURN,
            /** Conversation level.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Scale.ScaleLevel#CONVERSATION policy choice: Conversation level. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Scale.ScaleLevel; its declared order and identity are preserved.
             */
            CONVERSATION,
            /** Session level.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Scale.ScaleLevel#SESSION policy choice: Session level. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Scale.ScaleLevel; its declared order and identity are preserved.
             */
            SESSION,
            /** Project level.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Scale.ScaleLevel#PROJECT policy choice: Project level. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Scale.ScaleLevel; its declared order and identity are preserved.
             */
            PROJECT
        }

        /**
         * Requested context-scale transformations.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Enumerates the ISessionDigestSpec.Scale.ScaleOperation choices described as Requested context-scale transformations. Selection records an annotation value; execution remains the consumer responsibility.
         * @m3.dataStructures Enum containing 4 constants in the existing source order.
         */
        enum ScaleOperation {
            /** Aggregate (zoom out): combine children into parent.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Scale.ScaleOperation#AGGREGATE policy choice: Aggregate (zoom out): combine children into parent. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Scale.ScaleOperation; its declared order and identity are preserved.
             */
            AGGREGATE,
            /** Expand (zoom in): unfold parent into children.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Scale.ScaleOperation#EXPAND policy choice: Expand (zoom in): unfold parent into children. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Scale.ScaleOperation; its declared order and identity are preserved.
             */
            EXPAND,
            /** Transfer: move momentum between scales.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Scale.ScaleOperation#TRANSFER policy choice: Transfer: move momentum between scales. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Scale.ScaleOperation; its declared order and identity are preserved.
             */
            TRANSFER,
            /** Normalize: rebalance momentum at scale.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Scale.ScaleOperation#NORMALIZE policy choice: Normalize: rebalance momentum at scale. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Scale.ScaleOperation; its declared order and identity are preserved.
             */
            NORMALIZE
        }
    }

    /**
     * NetSum - Calculate net momentum sum.
     *
     * <p>
     * "Every context has net sum moment of its inner context" This annotation
     * triggers net sum calculation at specified scale.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares momentum aggregation level, history and stall/regression thresholds; it computes no sum or trend.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface NetSum {

        /** Scale level for calculation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.NetSum#level intent: Scale level for calculation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Scale.ScaleLevel}; default expression {@code Scale.ScaleLevel.CONVERSATION}.
         */
        Scale.ScaleLevel level() default Scale.ScaleLevel.CONVERSATION;

        /** Include momentum history (how it changed over time).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.NetSum#includeHistory intent: Include momentum history (how it changed over time). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean includeHistory() default false;

        /** Threshold for stall detection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.NetSum#stallThreshold intent: Threshold for stall detection. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.1}.
         */
        double stallThreshold() default 0.1;

        /** Threshold for regression detection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.NetSum#regressionThreshold intent: Threshold for regression detection. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code -0.2}.
         */
        double regressionThreshold() default -0.2;
    }

    /**
     * ContextQuality - Unified quality metric.
     *
     * <p>
     * "The beauty of making everything a context and a Mandelbrot"
     *
     * <p>
     * Everything is a context. Same laws apply at every scale:
     *
     * <pre>
     *   Token, Sentence, Turn, Session, Project, LLM Memory
     *   → All are contexts with M, SNR, H
     *   → Same formulas, same operations
     *   → The universe is self-similar
     * </pre>
     *
     * <p>
     * Unified formula:
     *
     * <pre>
     *   Q = M_net × SNR × (1 - H_normalized)
     *   where:
     *     M_net = net momentum (progress velocity)
     *     SNR   = signal-to-noise ratio (clarity)
     *     H     = Shannon entropy (focus)
     * </pre>
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares quality scope, component reporting, threshold and response; it computes no quality metric and pauses no workflow.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface ContextQuality {

        /** Scale level to measure.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.ContextQuality#level intent: Scale level to measure. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Scale.ScaleLevel}; default expression {@code Scale.ScaleLevel.CONVERSATION}.
         */
        Scale.ScaleLevel level() default Scale.ScaleLevel.CONVERSATION;

        /** Include all component metrics (M, SNR, H).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.ContextQuality#includeComponents intent: Include all component metrics (M, SNR, H). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean includeComponents() default true;

        /** Quality threshold (0.0-1.0) - below this triggers alert.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.ContextQuality#threshold intent: Quality threshold (0.0-1.0) - below this triggers alert. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.5}.
         */
        double threshold() default 0.5;

        /** Action when quality drops below threshold.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.ContextQuality#action intent: Action when quality drops below threshold. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code QualityAction}; default expression {@code QualityAction.LOG}.
         */
        QualityAction action() default QualityAction.LOG;

        /**
         * Requested responses to a context-quality threshold crossing.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Enumerates the ISessionDigestSpec.ContextQuality.QualityAction choices described as Requested responses to a context-quality threshold crossing. Selection records an annotation value; execution remains the consumer responsibility.
         * @m3.dataStructures Enum containing 5 constants in the existing source order.
         */
        enum QualityAction {
            /** Log quality metric.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.ContextQuality.QualityAction#LOG policy choice: Log quality metric. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.ContextQuality.QualityAction; its declared order and identity are preserved.
             */
            LOG,
            /** Emit agent tip.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.ContextQuality.QualityAction#TIP policy choice: Emit agent tip. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.ContextQuality.QualityAction; its declared order and identity are preserved.
             */
            TIP,
            /** Trigger context collapse.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.ContextQuality.QualityAction#COLLAPSE policy choice: Trigger context collapse. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.ContextQuality.QualityAction; its declared order and identity are preserved.
             */
            COLLAPSE,
            /** Request course correction.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.ContextQuality.QualityAction#CORRECT policy choice: Request course correction. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.ContextQuality.QualityAction; its declared order and identity are preserved.
             */
            CORRECT,
            /** Pause for human review.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.ContextQuality.QualityAction#PAUSE policy choice: Pause for human review. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.ContextQuality.QualityAction; its declared order and identity are preserved.
             */
            PAUSE
        }
    }

    /**
     * SNR - Signal-to-Noise Ratio calculation.
     *
     * <p>
     * Every context has SNR. This determines what to keep vs collapse.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares signal/noise threshold, heat classification and history preference; it measures no signal or noise.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface SNR {

        /** Minimum SNR to consider "clean".
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.SNR#minSNR intent: Minimum SNR to consider "clean". Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 2.0}.
         */
        double minSNR() default 2.0;

        /** Heat threshold for "signal" classification.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.SNR#signalHeatThreshold intent: Heat threshold for "signal" classification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 7}.
         */
        int signalHeatThreshold() default 7;

        /** Include SNR history.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.SNR#includeHistory intent: Include SNR history. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean includeHistory() default false;
    }

    /**
     * Entropy - Shannon entropy calculation.
     *
     * <p>
     * Measures conversation diversity/focus.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares entropy dimension and normalization preference; it calculates no distribution or entropy.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Entropy {

        /** Calculate over which dimension.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Entropy#dimension intent: Calculate over which dimension. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code EntropyDimension}; default expression {@code EntropyDimension.INTENT}.
         */
        EntropyDimension dimension() default EntropyDimension.INTENT;

        /** Normalize to 0-1 range.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares ISessionDigestSpec.Entropy#normalize intent: Normalize to 0-1 range. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean normalize() default true;

        /**
         * Distributions over which entropy calculation is requested.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Enumerates the ISessionDigestSpec.Entropy.EntropyDimension choices described as Distributions over which entropy calculation is requested. Selection records an annotation value; execution remains the consumer responsibility.
         * @m3.dataStructures Enum containing 4 constants in the existing source order.
         */
        enum EntropyDimension {
            /** Entropy over intent distribution.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Entropy.EntropyDimension#INTENT policy choice: Entropy over intent distribution. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Entropy.EntropyDimension; its declared order and identity are preserved.
             */
            INTENT,
            /** Entropy over topic distribution.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Entropy.EntropyDimension#TOPIC policy choice: Entropy over topic distribution. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Entropy.EntropyDimension; its declared order and identity are preserved.
             */
            TOPIC,
            /** Entropy over heat distribution.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Entropy.EntropyDimension#HEAT policy choice: Entropy over heat distribution. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Entropy.EntropyDimension; its declared order and identity are preserved.
             */
            HEAT,
            /** Combined entropy.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the ISessionDigestSpec.Entropy.EntropyDimension#COMBINED policy choice: Combined entropy. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of ISessionDigestSpec.Entropy.EntropyDimension; its declared order and identity are preserved.
             */
            COMBINED
        }
    }

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;

    /**
     * Stability level.
     *
     * @return stability
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#stability intent: Stability level. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Stability.Level}; default expression {@code Stability.Level.STABLE}.
     */
    Stability.Level stability() default Stability.Level.STABLE;

    /**
     * Specification value (description).
     *
     * @return value
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISessionDigestSpec#value intent: Specification value (description). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";
}
