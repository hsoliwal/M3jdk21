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
 * IBootSpec - The entry point that triggers AI into IOP flow.
 *
 * <h2>THE TRIGGER</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  @IBootSpec is the TRIGGER that activates IOP mode in the AI.             ║
 * ║                                                                           ║
 * ║  When Claude sees @IBootSpec on a package, class, or interface:           ║
 * ║    1. READ all @I*Spec annotations (contracts)                            ║
 * ║    2. VERIFY JavaDoc completeness                                         ║
 * ║    3. GENERATE implementations following IOP workflow                     ║
 * ║    4. RECORD manifestation with @AI* annotations                          ║
 * ║                                                                           ║
 * ║  "The boot hook that starts IOP flow from any point -                     ║
 * ║   cold boot, session timeout, or context switch."                         ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Why IBootSpec Exists</h2>
 *
 * <pre>
 * Problem: AI sessions are stateless. Each conversation starts fresh.
 * Solution: Code IS the memory. @IBootSpec tells AI to enter IOP mode.
 *
 * When AI reads @IBootSpec:
 *   - It knows to follow IOP workflow (READ → VERIFY → GENERATE → RECORD)
 *   - It knows to respect interface contracts as LAW
 *   - It knows to delegate to appropriate models (OPUS/SONNET/HAIKU/OLLAMA)
 *   - It knows to checkpoint progress for session survival
 *
 * "No matter the boot or system session timeout,
 *  &#64;IBootSpec ensures the AI knows what to do."
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Package-level boot (activates IOP for entire package)
 * &#64;IBootSpec(
 *     scope = PACKAGE,
 *     mode = FULL,
 *     checkpoint = true,
 *     description = "CogniX context system - IOP enabled"
 * )
 * package jdk.internal.m3.context;
 *
 * // Interface-level boot (activates IOP for this interface)
 * &#64;IBootSpec(mode = GENERATE)
 * &#64;ISpec(
 *     value = "Context factory creates and manages contexts",
 *     model = OPUS
 * )
 * public interface IContextFactory&lt;Self extends IContextFactory&lt;Self>> extends IContext&lt;Self> {
 *     IContext create(String name);
 * }
 *
 * // Class-level boot (for implementation generation)
 * &#64;IBootSpec(mode = IMPLEMENT)
 * &#64;IClassSpec(value = "SQLite-backed context registry", model = SONNET)
 * public class SqliteContextRegistry implements IContextRegistry {
 * 	// AI generates implementation here
 * }
 * }
 * </pre>
 *
 * <h2>Boot Sequence</h2>
 *
 * <pre>
 * 1. AI encounters @IBootSpec
 * 2. AI loads axis position (X=concepts, Y=chains, Z=surface, T=session)
 * 3. AI enters IOP workflow mode
 * 4. AI reads all contracts in scope
 * 5. AI validates JavaDoc completeness
 * 6. AI generates/updates implementations
 * 7. AI records manifestation (@IAIGenerated, @AITest, etc.)
 * 8. AI checkpoints progress (for session survival)
 * </pre>
 *
 * @since IOP 1.0 - Boot Trigger
 * @see IHookSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares generation-workflow scope, phases, checkpoints, model choices, registry, limits and filters; it activates no workflow, reads no checkpoint and modifies no file itself.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.PACKAGE, ElementType.TYPE}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IBootSpec.java}
 * blob {@code 58c79199dbc5faac8baea5f68fd4132ba4b7545e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IBootSpec {

    // --- SCOPE ---

    /**
     * Scope of IOP activation.
     *
     * @return scope
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#scope intent: Scope of IOP activation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Scope}; default expression {@code Scope.TYPE}.
     */
    Scope scope() default Scope.TYPE;

    /** Boot scopes.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IBootSpec.Scope choices described as Boot scopes. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum Scope {
        /** This type only.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Scope#TYPE policy choice: This type only. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Scope; its declared order and identity are preserved.
         */
        TYPE,

        /** This package.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Scope#PACKAGE policy choice: This package. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Scope; its declared order and identity are preserved.
         */
        PACKAGE,

        /** This package and subpackages.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Scope#PACKAGE_RECURSIVE policy choice: This package and subpackages. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Scope; its declared order and identity are preserved.
         */
        PACKAGE_RECURSIVE,

        /** Entire module.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Scope#MODULE policy choice: Entire module. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Scope; its declared order and identity are preserved.
         */
        MODULE,

        /** Entire project.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Scope#PROJECT policy choice: Entire project. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Scope; its declared order and identity are preserved.
         */
        PROJECT
    }

    // --- MODE ---

    /**
     * IOP mode to activate.
     *
     * @return mode
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#mode intent: IOP mode to activate. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Mode}; default expression {@code Mode.FULL}.
     */
    Mode mode() default Mode.FULL;

    /** IOP modes.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IBootSpec.Mode choices described as IOP modes. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 8 constants in the existing source order.
     */
    enum Mode {
        /** Full IOP workflow (READ → VERIFY → GENERATE → RECORD).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Mode#FULL policy choice: Full IOP workflow (READ → VERIFY → GENERATE → RECORD). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Mode; its declared order and identity are preserved.
         */
        FULL,

        /** Read and analyze only (no generation).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Mode#ANALYZE policy choice: Read and analyze only (no generation). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Mode; its declared order and identity are preserved.
         */
        ANALYZE,

        /** Validate contracts and JavaDoc only.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Mode#VALIDATE policy choice: Validate contracts and JavaDoc only. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Mode; its declared order and identity are preserved.
         */
        VALIDATE,

        /** Generate implementations from specs.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Mode#GENERATE policy choice: Generate implementations from specs. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Mode; its declared order and identity are preserved.
         */
        GENERATE,

        /** Implement a specific interface.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Mode#IMPLEMENT policy choice: Implement a specific interface. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Mode; its declared order and identity are preserved.
         */
        IMPLEMENT,

        /** Document/fix JavaDoc only.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Mode#DOCUMENT policy choice: Document/fix JavaDoc only. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Mode; its declared order and identity are preserved.
         */
        DOCUMENT,

        /** Test generated code.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Mode#TEST policy choice: Test generated code. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Mode; its declared order and identity are preserved.
         */
        TEST,

        /** Review existing code.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Mode#REVIEW policy choice: Review existing code. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Mode; its declared order and identity are preserved.
         */
        REVIEW
    }

    // --- WORKFLOW PHASES ---

    /**
     * Phases to execute.
     *
     * @return phases
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#phases intent: Phases to execute. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Phase[]}; default expression {@code {Phase.READ, Phase.VERIFY, Phase.GENERATE, Phase.RECORD}}.
     */
    Phase[] phases() default {Phase.READ, Phase.VERIFY, Phase.GENERATE, Phase.RECORD};

    /** IOP workflow phases.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IBootSpec.Phase choices described as IOP workflow phases. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum Phase {
        /** Read contracts (interfaces, JavaDoc).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Phase#READ policy choice: Read contracts (interfaces, JavaDoc). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Phase; its declared order and identity are preserved.
         */
        READ,

        /** Verify completeness and consistency.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Phase#VERIFY policy choice: Verify completeness and consistency. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Phase; its declared order and identity are preserved.
         */
        VERIFY,

        /** Repair/improve documentation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Phase#DOCUMENT policy choice: Repair/improve documentation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Phase; its declared order and identity are preserved.
         */
        DOCUMENT,

        /** Generate implementations.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Phase#GENERATE policy choice: Generate implementations. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Phase; its declared order and identity are preserved.
         */
        GENERATE,

        /** Test generated code.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Phase#TEST policy choice: Test generated code. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Phase; its declared order and identity are preserved.
         */
        TEST,

        /** Record manifestation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Phase#RECORD policy choice: Record manifestation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Phase; its declared order and identity are preserved.
         */
        RECORD,

        /** Checkpoint progress.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Phase#CHECKPOINT policy choice: Checkpoint progress. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Phase; its declared order and identity are preserved.
         */
        CHECKPOINT
    }

    // --- CHECKPOINT ---

    /**
     * Whether to checkpoint progress.
     *
     * @return true to checkpoint
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#checkpoint intent: Whether to checkpoint progress. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean checkpoint() default true;

    /**
     * Checkpoint frequency (after N operations).
     *
     * @return frequency
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#checkpointFrequency intent: Checkpoint frequency (after N operations). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 5}.
     */
    int checkpointFrequency() default 5;

    /**
     * Checkpoint store path.
     *
     * @return store path
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#checkpointStore intent: Checkpoint store path. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ".claude/checkpoints"}.
     */
    String checkpointStore() default ".claude/checkpoints";

    // --- AXIS POSITION ---

    /**
     * Expected X-axis (concepts loaded). -1 = any.
     *
     * @return x position
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#axisX intent: Expected X-axis (concepts loaded). -1 = any. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code -1}.
     */
    int axisX() default -1;

    /**
     * Expected Y-axis (active chains). -1 = any.
     *
     * @return y position
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#axisY intent: Expected Y-axis (active chains). -1 = any. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code -1}.
     */
    int axisY() default -1;

    /**
     * Expected Z-axis (surface architecture).
     *
     * @return z position
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#axisZ intent: Expected Z-axis (surface architecture). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String axisZ() default "";

    /**
     * Minimum session count (T-axis). -1 = any.
     *
     * @return min sessions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#minSessions intent: Minimum session count (T-axis). -1 = any. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code -1}.
     */
    int minSessions() default -1;

    // --- MODEL DEFAULTS ---

    /**
     * Default model for this scope.
     *
     * @return default model
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#defaultModel intent: Default model for this scope. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType defaultModel() default Model.ModelType.SONNET;

    /**
     * Fallback model if default unavailable.
     *
     * @return fallback model
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#fallbackModel intent: Fallback model if default unavailable. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.OLLAMA}.
     */
    Model.ModelType fallbackModel() default Model.ModelType.OLLAMA;

    /**
     * Model for security-critical code.
     *
     * @return security model
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#securityModel intent: Model for security-critical code. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.OPUS}.
     */
    Model.ModelType securityModel() default Model.ModelType.OPUS;

    /**
     * Model for simple/boilerplate code.
     *
     * @return simple model
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#simpleModel intent: Model for simple/boilerplate code. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.HAIKU}.
     */
    Model.ModelType simpleModel() default Model.ModelType.HAIKU;

    // --- REGISTRY ---

    /**
     * Registry type for tracking generations.
     *
     * @return registry type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#registry intent: Registry type for tracking generations. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code RegistryType}; default expression {@code RegistryType.SQLITE}.
     */
    RegistryType registry() default RegistryType.SQLITE;

    /** Registry types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IBootSpec.RegistryType choices described as Registry types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum RegistryType {
        /** No registry.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.RegistryType#NONE policy choice: No registry. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.RegistryType; its declared order and identity are preserved.
         */
        NONE,

        /** In-memory (session only).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.RegistryType#MEMORY policy choice: In-memory (session only). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.RegistryType; its declared order and identity are preserved.
         */
        MEMORY,

        /** SQLite database.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.RegistryType#SQLITE policy choice: SQLite database. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.RegistryType; its declared order and identity are preserved.
         */
        SQLITE,

        /** JSON files.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.RegistryType#JSON policy choice: JSON files. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.RegistryType; its declared order and identity are preserved.
         */
        JSON,

        /** Custom implementation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.RegistryType#CUSTOM policy choice: Custom implementation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.RegistryType; its declared order and identity are preserved.
         */
        CUSTOM
    }

    /**
     * Registry path.
     *
     * @return path
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#registryPath intent: Registry path. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ".claude/iop-registry.db"}.
     */
    String registryPath() default ".claude/iop-registry.db";

    // --- CONSTRAINTS ---

    /**
     * Maximum tokens to use in this session. 0 = unlimited.
     *
     * @return max tokens
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#maxTokens intent: Maximum tokens to use in this session. 0 = unlimited. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int maxTokens() default 0;

    /**
     * Maximum files to modify. 0 = unlimited.
     *
     * @return max files
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#maxFiles intent: Maximum files to modify. 0 = unlimited. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int maxFiles() default 0;

    /**
     * Read-only mode (analyze only, no changes).
     *
     * @return true for read-only
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#readOnly intent: Read-only mode (analyze only, no changes). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean readOnly() default false;

    // --- FILTERS ---

    /**
     * Include only these packages (glob patterns).
     *
     * @return include patterns
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#includePackages intent: Include only these packages (glob patterns). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] includePackages() default {};

    /**
     * Exclude these packages (glob patterns).
     *
     * @return exclude patterns
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#excludePackages intent: Exclude these packages (glob patterns). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] excludePackages() default {};

    /**
     * Include only interfaces matching these patterns.
     *
     * @return include patterns
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#includeInterfaces intent: Include only interfaces matching these patterns. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] includeInterfaces() default {};

    // --- IDENTIFICATION ---

    /**
     * Boot identifier (for logging/tracking).
     *
     * @return boot ID
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#id intent: Boot identifier (for logging/tracking). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String id() default "";

    /**
     * Description of this boot configuration.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#value intent: Description of this boot configuration. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    /**
     * Version of IOP protocol.
     *
     * @return version
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#iopVersion intent: Version of IOP protocol. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "1.0"}.
     */
    String iopVersion() default "1.0";

    // --- LOGGING ---

    /**
     * Whether to log boot sequence.
     *
     * @return true to log
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#logged intent: Whether to log boot sequence. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean logged() default true;

    /**
     * Verbosity level.
     *
     * @return verbosity
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IBootSpec#verbosity intent: Verbosity level. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Verbosity}; default expression {@code Verbosity.NORMAL}.
     */
    Verbosity verbosity() default Verbosity.NORMAL;

    /** Verbosity levels.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IBootSpec.Verbosity choices described as Verbosity levels. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Verbosity {
        /** Minimal output.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Verbosity#QUIET policy choice: Minimal output. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Verbosity; its declared order and identity are preserved.
         */
        QUIET,

        /** Normal output.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Verbosity#NORMAL policy choice: Normal output. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Verbosity; its declared order and identity are preserved.
         */
        NORMAL,

        /** Verbose output.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Verbosity#VERBOSE policy choice: Verbose output. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Verbosity; its declared order and identity are preserved.
         */
        VERBOSE,

        /** Debug output.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IBootSpec.Verbosity#DEBUG policy choice: Debug output. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IBootSpec.Verbosity; its declared order and identity are preserved.
         */
        DEBUG
    }
}
