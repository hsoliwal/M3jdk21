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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AILLMHint - AnOP annotation for LLM-specific attention and triggers.
 *
 * <h2>AnOP Advisory: LLM Attention Control</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  CRITICAL: Different LLMs have different attention patterns.             ║
 * ║                                                                           ║
 * ║  Claude:  Responds to structured headers, XML-like tags, @mentions       ║
 * ║  GPT:     Responds to markdown headers, explicit instructions            ║
 * ║  Gemini:  Responds to structured prompts, clear delimiters               ║
 * ║  Llama:   Responds to clear formatting, explicit context                 ║
 * ║                                                                           ║
 * ║  This annotation ensures code is TAGGED for LLM attention.               ║
 * ║  When an LLM processes this file, these tags GRAB ATTENTION.             ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Universal Attention Triggers</h2>
 * <ul>
 * <li>CRITICAL, IMPORTANT, WARNING, NOTE - Universal attention words</li>
 * <li>TODO, FIXME, HACK, XXX - Universal code markers</li>
 * <li>MUST, SHALL, SHOULD, MAY - RFC-2119 keywords</li>
 * <li>BEGIN/END blocks - Explicit boundaries</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	// Tag for high attention
 * 	&#64;AILLMHint(attention = Attention.CRITICAL, trigger = "MUST implement exactly as specified", models = {
 * 			Model.CLAUDE, Model.GPT})
 * 	public interface ISecurityService&lt;Self extends ISecurityService&lt;Self>> extends IContext&lt;Self> {
 * 	}
 *
 * 	// Tag with context for the LLM
 * 	&#64;AILLMHint(attention = Attention.IMPORTANT, context = "This is the main entry point for all user operations", constraints = {
 * 			"No null returns", "Always validate input"})
 * 	public interface IUserService&lt;Self extends IUserService&lt;Self&gt;&gt; extends IContext&lt;Self&gt; {
 * 	}
 *
 * 	// Model-specific behavior
 * 	&#64;AILLMHint(models = {
 * 			Model.CLAUDE}, claudeTag = "claude-attention", systemPrompt = "You are implementing a security-critical component")
 * 	public interface IAuthService&lt;Self extends IAuthService&lt;Self&gt;&gt; extends IContext&lt;Self&gt; {
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP LLM Attention Hints
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares LLM attention, trigger, context and prompting preferences in the separate IAI-prefixed API; this annotation records guidance rather than implementing or verifying the requested behavior.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PACKAGE}; preserves its declared element and nested-token schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAILLMHint.java}
 * blob {@code 9d13058e7af7526334b90999534e69e4a7c414b7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAILLMHints.class)
public @interface IAILLMHint {

    // --- ATTENTION CONTROL ---

    /**
     * Attention level for this element.
     *
     * @return attention level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#attention advisory metadata for Attention level for this element. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Attention annotation element; exact default expression Attention.NORMAL.
     */
    Attention attention() default Attention.NORMAL;

    /**
     * Explicit trigger phrase for LLM attention. Use RFC-2119 words: MUST, SHALL,
     * SHOULD, MAY, etc.
     *
     * @return trigger phrase
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#trigger advisory metadata for Explicit trigger phrase for LLM attention. Use RFC-2119 words: MUST, SHALL, SHOULD, MAY, etc. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String trigger() default "";

    /**
     * Context to provide the LLM for understanding.
     *
     * @return context description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#context advisory metadata for Context to provide the LLM for understanding. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String context() default "";

    /**
     * Constraints the LLM MUST follow.
     *
     * @return constraints
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#constraints advisory metadata for Constraints the LLM MUST follow. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] constraints() default {};

    /**
     * Things the LLM MUST NOT do.
     *
     * @return prohibitions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#prohibitions advisory metadata for Things the LLM MUST NOT do. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] prohibitions() default {};

    // --- MODEL TARGETING ---

    /**
     * Target LLM models.
     *
     * @return target models
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#models advisory metadata for Target LLM models. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Model[] annotation element; exact default expression {Model.ALL}.
     */
    Model[] models() default {Model.ALL};

    /**
     * Minimum capability level required.
     *
     * @return capability
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#capability advisory metadata for Minimum capability level required. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Capability annotation element; exact default expression Capability.STANDARD.
     */
    Capability capability() default Capability.STANDARD;

    // --- MODEL-SPECIFIC TAGS ---

    /**
     * Claude-specific attention tag. Claude responds well to: &lt;tag&gt; blocks,
     * Human/Assistant format.
     *
     * @return Claude tag
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#claudeTag advisory metadata for Claude-specific attention tag. Claude responds well to: &lt;tag&gt; blocks, Human/Assistant format. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String claudeTag() default "";

    /**
     * GPT-specific system prompt hint.
     *
     * @return GPT system hint
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#gptSystemHint advisory metadata for GPT-specific system prompt hint. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String gptSystemHint() default "";

    /**
     * System prompt to inject for this element.
     *
     * @return system prompt
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#systemPrompt advisory metadata for System prompt to inject for this element. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String systemPrompt() default "";

    // --- PROCESSING HINTS ---

    /**
     * Processing priority (higher = process first).
     *
     * @return priority
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#priority advisory metadata for Processing priority (higher = process first). The element itself neither executes nor enforces that request.
     * @m3.dataStructures int annotation element; exact default expression 0.
     */
    int priority() default 0;

    /**
     * This element requires careful review before changes.
     *
     * @return true if careful review needed
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#reviewRequired advisory metadata for This element requires careful review before changes. The element itself neither executes nor enforces that request.
     * @m3.dataStructures boolean annotation element; exact default expression false.
     */
    boolean reviewRequired() default false;

    /**
     * Changes to this element are high-risk.
     *
     * @return true if high risk
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#highRisk advisory metadata for Changes to this element are high-risk. The element itself neither executes nor enforces that request.
     * @m3.dataStructures boolean annotation element; exact default expression false.
     */
    boolean highRisk() default false;

    /**
     * This element has security implications.
     *
     * @return true if security-sensitive
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#securitySensitive advisory metadata for This element has security implications. The element itself neither executes nor enforces that request.
     * @m3.dataStructures boolean annotation element; exact default expression false.
     */
    boolean securitySensitive() default false;

    /**
     * This element affects data integrity.
     *
     * @return true if data-critical
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#dataCritical advisory metadata for This element affects data integrity. The element itself neither executes nor enforces that request.
     * @m3.dataStructures boolean annotation element; exact default expression false.
     */
    boolean dataCritical() default false;

    // --- GENERATION CONTROL ---

    /**
     * Generation temperature hint (0.0 = deterministic, 1.0 = creative).
     *
     * @return temperature
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#temperature advisory metadata for Generation temperature hint (0.0 = deterministic, 1.0 = creative). The element itself neither executes nor enforces that request.
     * @m3.dataStructures float annotation element; exact default expression 0.3F.
     */
    float temperature() default 0.3f;

    /**
     * Preferred output style.
     *
     * @return style
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#outputStyle advisory metadata for Preferred output style. The element itself neither executes nor enforces that request.
     * @m3.dataStructures OutputStyle annotation element; exact default expression OutputStyle.CONCISE.
     */
    OutputStyle outputStyle() default OutputStyle.CONCISE;

    /**
     * Include reasoning/explanation in output.
     *
     * @return true for reasoning
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHint#includeReasoning advisory metadata for Include reasoning/explanation in output. The element itself neither executes nor enforces that request.
     * @m3.dataStructures boolean annotation element; exact default expression false.
     */
    boolean includeReasoning() default false;

    // --- ENUMS ---

    /** Attention levels - universal triggers that work across LLMs.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 7 symbolic choices for Attention levels - universal triggers that work across LLMs within IAILLMHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAILLMHint.Attention enum with 7 source-declared constants in existing order.
     */
    enum Attention {
        /** Normal attention.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Attention#NORMAL token within Attention levels - universal triggers that work across LLMs: Normal attention. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Attention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        NORMAL,

        /** Important - should not miss.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Attention#IMPORTANT token within Attention levels - universal triggers that work across LLMs: Important - should not miss. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Attention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        IMPORTANT,

        /** Critical - MUST not miss.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Attention#CRITICAL token within Attention levels - universal triggers that work across LLMs: Critical - MUST not miss. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Attention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CRITICAL,

        /** Warning - potential issues.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Attention#WARNING token within Attention levels - universal triggers that work across LLMs: Warning - potential issues. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Attention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        WARNING,

        /** Security - security-sensitive.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Attention#SECURITY token within Attention levels - universal triggers that work across LLMs: Security - security-sensitive. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Attention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SECURITY,

        /** Performance - performance-critical.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Attention#PERFORMANCE token within Attention levels - universal triggers that work across LLMs: Performance - performance-critical. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Attention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PERFORMANCE,

        /** Breaking - breaking changes.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Attention#BREAKING token within Attention levels - universal triggers that work across LLMs: Breaking - breaking changes. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Attention enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BREAKING
    }

    /** Target LLM models.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 20 symbolic choices for Target LLM models within IAILLMHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAILLMHint.Model enum with 20 source-declared constants in existing order.
     */
    enum Model {
        /** All models (universal).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#ALL token within Target LLM models: All models (universal). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ALL,

        // --- ANTHROPIC ---

        /** Claude (all versions).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#CLAUDE token within Target LLM models: Claude (all versions). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CLAUDE,

        /** Claude Opus (highest capability).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#CLAUDE_OPUS token within Target LLM models: Claude Opus (highest capability). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CLAUDE_OPUS,

        /** Claude Sonnet (balanced).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#CLAUDE_SONNET token within Target LLM models: Claude Sonnet (balanced). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CLAUDE_SONNET,

        /** Claude Haiku (fast).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#CLAUDE_HAIKU token within Target LLM models: Claude Haiku (fast). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CLAUDE_HAIKU,

        // --- OPENAI ---

        /** GPT (all versions).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#GPT token within Target LLM models: GPT (all versions). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GPT,

        /** GPT-4 family.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#GPT_4 token within Target LLM models: GPT-4 family. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GPT_4,

        /** GPT-4 Turbo.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#GPT_4_TURBO token within Target LLM models: GPT-4 Turbo. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GPT_4_TURBO,

        /** GPT-4o (optimized).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#GPT_4O token within Target LLM models: GPT-4o (optimized). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GPT_4O,

        /** o1 (reasoning).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#O1 token within Target LLM models: o1 (reasoning). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        O1,

        // --- GOOGLE ---

        /** Gemini (all versions).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#GEMINI token within Target LLM models: Gemini (all versions). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GEMINI,

        /** Gemini Pro.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#GEMINI_PRO token within Target LLM models: Gemini Pro. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GEMINI_PRO,

        /** Gemini Ultra.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#GEMINI_ULTRA token within Target LLM models: Gemini Ultra. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GEMINI_ULTRA,

        // --- META ---

        /** Llama (all versions).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#LLAMA token within Target LLM models: Llama (all versions). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LLAMA,

        /** Llama 3.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#LLAMA_3 token within Target LLM models: Llama 3. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LLAMA_3,

        // --- LOCAL/OPEN ---

        /** Ollama (local models).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#OLLAMA token within Target LLM models: Ollama (local models). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        OLLAMA,

        /** Mistral.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#MISTRAL token within Target LLM models: Mistral. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MISTRAL,

        /** Mixtral.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#MIXTRAL token within Target LLM models: Mixtral. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MIXTRAL,

        /** CodeLlama.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#CODE_LLAMA token within Target LLM models: CodeLlama. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CODE_LLAMA,

        /** DeepSeek Coder.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Model#DEEPSEEK_CODER token within Target LLM models: DeepSeek Coder. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Model enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DEEPSEEK_CODER
    }

    /** Capability levels required.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 4 symbolic choices for Capability levels required within IAILLMHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAILLMHint.Capability enum with 4 source-declared constants in existing order.
     */
    enum Capability {
        /** Basic - simple tasks.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Capability#BASIC token within Capability levels required: Basic - simple tasks. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Capability enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BASIC,

        /** Standard - typical coding tasks.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Capability#STANDARD token within Capability levels required: Standard - typical coding tasks. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Capability enum constant, retaining declared order and identity; it stores no implementation object.
         */
        STANDARD,

        /** Advanced - complex reasoning.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Capability#ADVANCED token within Capability levels required: Advanced - complex reasoning. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Capability enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ADVANCED,

        /** Expert - architecture, security review.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.Capability#EXPERT token within Capability levels required: Expert - architecture, security review. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.Capability enum constant, retaining declared order and identity; it stores no implementation object.
         */
        EXPERT
    }

    /** Output style preferences.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 5 symbolic choices for Output style preferences within IAILLMHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAILLMHint.OutputStyle enum with 5 source-declared constants in existing order.
     */
    enum OutputStyle {
        /** Concise - minimal output.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.OutputStyle#CONCISE token within Output style preferences: Concise - minimal output. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.OutputStyle enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONCISE,

        /** Verbose - detailed output.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.OutputStyle#VERBOSE token within Output style preferences: Verbose - detailed output. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.OutputStyle enum constant, retaining declared order and identity; it stores no implementation object.
         */
        VERBOSE,

        /** Documented - with comments/javadoc.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.OutputStyle#DOCUMENTED token within Output style preferences: Documented - with comments/javadoc. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.OutputStyle enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DOCUMENTED,

        /** Explained - with reasoning.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.OutputStyle#EXPLAINED token within Output style preferences: Explained - with reasoning. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.OutputStyle enum constant, retaining declared order and identity; it stores no implementation object.
         */
        EXPLAINED,

        /** Minimal - just the code.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILLMHint.OutputStyle#MINIMAL token within Output style preferences: Minimal - just the code. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILLMHint.OutputStyle enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MINIMAL
    }
}
