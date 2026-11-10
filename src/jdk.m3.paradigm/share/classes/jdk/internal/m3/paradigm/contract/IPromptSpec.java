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
 * IPromptSpec - LLM Prompt specification for AI code generation.
 *
 * <h2>THE META SPEC</h2>
 *
 * <pre>
 * This is IOP's most unique annotation - a specification for LLM prompts.
 * AI generating prompts for AI. Meta-programming for the LLM age.
 *
 * Prompt patterns:
 *   - Zero-shot
 *   - Few-shot (with examples)
 *   - Chain-of-thought
 *   - ReAct (Reasoning + Acting)
 *   - Tool-use prompts
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	interface ICodeAssistant {
 *
 * 		&#64;IPromptSpec(template = "Analyze this code for bugs:\n\n{code}\n\nProvide a list of issues.", model = OPUS, temperature = 0.3, maxTokens = 2000)
 * 		String analyzeCode(String code);
 *
 * 		&#64;IPromptSpec(system = "You are an expert code reviewer.", template = "Review this PR:\n{diff}", examples = {
 * 				&#64;IPromptSpec.Example(input = "...", output = "...")})
 * 		String reviewPullRequest(String diff);
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - Prompt Specification (Meta)
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares prompt text, examples, model settings, tool preferences and output/cache configuration; it invokes no model and grants no permission.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IPromptSpec.java}
 * blob {@code bc246741b4b6b88d03711489c6d4565514a6ca7f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IPromptSpec {

    // --- PROMPT TEMPLATE ---

    /**
     * Prompt template text; placeholder expansion is not implemented here.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#template intent: Prompt template text; placeholder expansion is not implemented here. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String template() default "";

    /**
     * Requested system-context text; this source value does not override host instructions.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#system intent: Requested system-context text; this source value does not override host instructions. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String system() default "";

    /**
     * Declared prompt-variable names.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#variables intent: Declared prompt-variable names. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] variables() default {};

    // --- FEW-SHOT EXAMPLES ---

    /**
     * Prompt examples in supplied order.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#examples intent: Prompt examples in supplied order. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Example[]}; default expression {@code {}}.
     */
    Example[] examples() default {};

    /**
     * Declares an input/output example and explanation; it validates no example and retains implicit CLASS retention without explicit Target restriction.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares an input/output example and explanation; it validates no example and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Example {

        /**
         * Example input text.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IPromptSpec.Example#input intent: Example input text. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String input();

        /**
         * Example expected output text; no correctness check occurs.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IPromptSpec.Example#output intent: Example expected output text; no correctness check occurs. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String output();

        /**
         * Optional explanation of the supplied example.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IPromptSpec.Example#explanation intent: Optional explanation of the supplied example. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String explanation() default "";
    }

    // --- MODEL PARAMETERS ---

    /**
     * Requested generation-model token.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#model intent: Requested generation-model token. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;

    /**
     * Requested sampling temperature; no range check is performed.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#temperature intent: Requested sampling temperature; no range check is performed. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.7}.
     */
    double temperature() default 0.7;

    /**
     * Requested output token budget.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#maxTokens intent: Requested output token budget. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 4096}.
     */
    int maxTokens() default 4096;

    /**
     * Requested nucleus-sampling setting; no range check is performed.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#topP intent: Requested nucleus-sampling setting; no range check is performed. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 1.0}.
     */
    double topP() default 1.0;

    /**
     * Requested generation stopping strings.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#stopSequences intent: Requested generation stopping strings. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] stopSequences() default {};

    // --- PROMPT PATTERN ---

    /**
     * Requested prompt-pattern category.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#pattern intent: Requested prompt-pattern category. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Pattern}; default expression {@code Pattern.DIRECT}.
     */
    Pattern pattern() default Pattern.DIRECT;

    /**
     * Prompt-pattern categories; tokens supply schema values without execution or authorization.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IPromptSpec.Pattern choices described as Prompt-pattern categories; tokens supply schema values without execution or authorization. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum Pattern {
        /**
         * Requested direct prompting category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.Pattern#DIRECT policy choice: Requested direct prompting category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.Pattern; its declared order and identity are preserved.
         */
        DIRECT,
        /**
         * Requested zero-example prompting category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.Pattern#ZERO_SHOT policy choice: Requested zero-example prompting category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.Pattern; its declared order and identity are preserved.
         */
        ZERO_SHOT,
        /**
         * Requested example-guided prompting category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.Pattern#FEW_SHOT policy choice: Requested example-guided prompting category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.Pattern; its declared order and identity are preserved.
         */
        FEW_SHOT,
        /**
         * Requested reasoning-oriented prompting category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.Pattern#CHAIN_OF_THOUGHT policy choice: Requested reasoning-oriented prompting category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.Pattern; its declared order and identity are preserved.
         */
        CHAIN_OF_THOUGHT,
        /**
         * Requested reasoning/action prompting category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.Pattern#REACT policy choice: Requested reasoning/action prompting category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.Pattern; its declared order and identity are preserved.
         */
        REACT,
        /**
         * Requested branching reasoning prompting category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.Pattern#TREE_OF_THOUGHT policy choice: Requested branching reasoning prompting category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.Pattern; its declared order and identity are preserved.
         */
        TREE_OF_THOUGHT,
        /**
         * Requested consistency-oriented sampling category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.Pattern#SELF_CONSISTENCY policy choice: Requested consistency-oriented sampling category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.Pattern; its declared order and identity are preserved.
         */
        SELF_CONSISTENCY
    }

    // --- TOOL USE ---

    /**
     * Requested tool class references; no tool permission is granted.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#tools intent: Requested tool class references; no tool permission is granted. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>[]}; default expression {@code {}}.
     */
    Class<?>[] tools() default {};

    /**
     * Requested tool-use preference; this flag is not external authorization.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#allowToolUse intent: Requested tool-use preference; this flag is not external authorization. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean allowToolUse() default false;

    // --- OUTPUT ---

    /**
     * Requested output representation.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#output intent: Requested output representation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code OutputFormat}; default expression {@code OutputFormat.TEXT}.
     */
    OutputFormat output() default OutputFormat.TEXT;

    /**
     * Prompt output representations; tokens supply schema values without execution or authorization.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IPromptSpec.OutputFormat choices described as Prompt output representations; tokens supply schema values without execution or authorization. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum OutputFormat {
        /**
         * Requested plain text category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.OutputFormat#TEXT policy choice: Requested plain text category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.OutputFormat; its declared order and identity are preserved.
         */
        TEXT,
        /**
         * Requested JSON category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.OutputFormat#JSON policy choice: Requested JSON category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.OutputFormat; its declared order and identity are preserved.
         */
        JSON,
        /**
         * Requested Markdown category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.OutputFormat#MARKDOWN policy choice: Requested Markdown category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.OutputFormat; its declared order and identity are preserved.
         */
        MARKDOWN,
        /**
         * Requested source code category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.OutputFormat#CODE policy choice: Requested source code category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.OutputFormat; its declared order and identity are preserved.
         */
        CODE,
        /**
         * Requested structured output category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IPromptSpec.OutputFormat#STRUCTURED policy choice: Requested structured output category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IPromptSpec.OutputFormat; its declared order and identity are preserved.
         */
        STRUCTURED
    }

    /**
     * Requested structured response class; Void.class remains the unspecified sentinel.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#responseType intent: Requested structured response class; Void.class remains the unspecified sentinel. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> responseType() default Void.class;

    // --- CACHING ---

    /**
     * Whether prompt-result caching is requested.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#cacheable intent: Whether prompt-result caching is requested. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean cacheable() default false;

    /**
     * Requested cache lifetime in seconds.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#cacheTtlSeconds intent: Requested cache lifetime in seconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3600}.
     */
    int cacheTtlSeconds() default 3600;

    // --- DESCRIPTION ---

    /**
     * Natural-language prompt specification.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IPromptSpec#value intent: Natural-language prompt specification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";
}
