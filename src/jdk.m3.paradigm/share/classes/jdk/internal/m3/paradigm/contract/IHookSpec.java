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
 * IHookSpec - Lifecycle hook specification for AI code generation.
 *
 * <h2>The Universal Entry Point</h2>
 *
 * <pre>
 * The final hook that starts from any point:
 *   - Cold boot (fresh start)
 *   - Warm boot (session timeout recovery)
 *   - Crash recovery (checkpoint restoration)
 *   - Context switch (state handoff)
 *
 * "No matter the boot or system session timeout,
 *  the hook ensures continuity."
 * </pre>
 *
 * <h2>Lifecycle Hook Patterns</h2>
 *
 * <pre>
 * Hook patterns in systems:
 *   - @PostConstruct / @PreDestroy (Jakarta)
 *   - ApplicationRunner / CommandLineRunner (Spring Boot)
 *   - ContextRefreshedEvent (Spring)
 *   - Servlet init() / destroy()
 *   - JVM shutdown hooks
 *   - OS signal handlers (SIGTERM, SIGHUP)
 *   - Kubernetes lifecycle hooks
 *   - systemd service lifecycle
 *
 * &#64;IHookSpec unifies these into one annotation.
 * </pre>
 *
 * <h2>Generated Code PatternKind</h2>
 *
 * <pre>{@code
 * // From: &#64;IHookSpec(phase = BOOT, priority = 0)
 * // Generates:
 *
 * &#64;PostConstruct
 * &#64;Order(0)
 * public void onBoot() {
 * 	// Restore state from last checkpoint
 * 	var checkpoint = checkpointStore.loadLatest();
 * 	if (checkpoint.isPresent()) {
 * 		restoreState(checkpoint.get());
 * 		log.info("Resumed from checkpoint: {}", checkpoint.get().id());
 * 	} else {
 * 		initializeFresh();
 * 		log.info("Cold boot - fresh initialization");
 * 	}
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface ISessionManager {
 *
 * 	// Boot hook - first thing that runs
 * 	&#64;IHookSpec(phase = BOOT, priority = 0, checkpoint = true, description = "Restore session state from any boot scenario")
 * 	void onBoot();
 *
 * 	// Shutdown hook - graceful cleanup
 * 	&#64;IHookSpec(phase = SHUTDOWN, priority = 100, checkpoint = true, description = "Save state for next session")
 * 	void onShutdown();
 *
 * 	// Timeout recovery hook
 * 	&#64;IHookSpec(phase = RECOVERY, trigger = SESSION_TIMEOUT, description = "Handle session timeout gracefully")
 * 	void onSessionTimeout();
 *
 * 	// Crash recovery hook
 * 	@IHookSpec(phase = RECOVERY, trigger = CRASH, checkpoint = true, description = "Recover from unexpected termination")
 * 	void onCrashRecovery();
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Lifecycle Hooks
 * @see IExecSpec
 * @see IStateSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares lifecycle phase, trigger, dependencies, checkpoint, timeout, signal and deployment-hook preferences; it installs no signal handler and performs no lifecycle transition or shutdown.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IHookSpec.java}
 * blob {@code fd33db73316fa4e3ac5ea64b537fb106e65009fc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IHookSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined hook pattern.
     *
     * @return option
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#option intent: Predefined hook pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common hook patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHookSpec.Option choices described as Common hook patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 9 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Application boot hook.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Option#BOOT policy choice: Application boot hook. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Option; its declared order and identity are preserved.
         */
        BOOT,

        /** Application shutdown hook.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Option#SHUTDOWN policy choice: Application shutdown hook. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Option; its declared order and identity are preserved.
         */
        SHUTDOWN,

        /** Session start hook.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Option#SESSION_START policy choice: Session start hook. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Option; its declared order and identity are preserved.
         */
        SESSION_START,

        /** Session end hook.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Option#SESSION_END policy choice: Session end hook. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Option; its declared order and identity are preserved.
         */
        SESSION_END,

        /** Crash recovery hook.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Option#CRASH_RECOVERY policy choice: Crash recovery hook. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Option; its declared order and identity are preserved.
         */
        CRASH_RECOVERY,

        /** Timeout recovery hook.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Option#TIMEOUT_RECOVERY policy choice: Timeout recovery hook. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Option; its declared order and identity are preserved.
         */
        TIMEOUT_RECOVERY,

        /** Health check hook.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Option#HEALTH_CHECK policy choice: Health check hook. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Option; its declared order and identity are preserved.
         */
        HEALTH_CHECK,

        /** Configuration reload hook.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Option#CONFIG_RELOAD policy choice: Configuration reload hook. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Option; its declared order and identity are preserved.
         */
        CONFIG_RELOAD
    }

    // --- LIFECYCLE PHASE ---

    /**
     * Lifecycle phase when hook executes.
     *
     * @return phase
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#phase intent: Lifecycle phase when hook executes. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Phase}; default expression {@code Phase.BOOT}.
     */
    Phase phase() default Phase.BOOT;

    /** Lifecycle phases.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHookSpec.Phase choices described as Lifecycle phases. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 10 constants in the existing source order.
     */
    enum Phase {
        /** Pre-initialization (before any beans).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Phase#PRE_INIT policy choice: Pre-initialization (before any beans). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Phase; its declared order and identity are preserved.
         */
        PRE_INIT,

        /** Boot/startup (after context ready).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Phase#BOOT policy choice: Boot/startup (after context ready). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Phase; its declared order and identity are preserved.
         */
        BOOT,

        /** Post-boot (after all boot hooks).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Phase#POST_BOOT policy choice: Post-boot (after all boot hooks). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Phase; its declared order and identity are preserved.
         */
        POST_BOOT,

        /** Running state (normal operation).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Phase#RUNNING policy choice: Running state (normal operation). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Phase; its declared order and identity are preserved.
         */
        RUNNING,

        /** Pre-shutdown (before cleanup begins).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Phase#PRE_SHUTDOWN policy choice: Pre-shutdown (before cleanup begins). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Phase; its declared order and identity are preserved.
         */
        PRE_SHUTDOWN,

        /** Shutdown (graceful termination).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Phase#SHUTDOWN policy choice: Shutdown (graceful termination). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Phase; its declared order and identity are preserved.
         */
        SHUTDOWN,

        /** Post-shutdown (final cleanup).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Phase#POST_SHUTDOWN policy choice: Post-shutdown (final cleanup). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Phase; its declared order and identity are preserved.
         */
        POST_SHUTDOWN,

        /** Recovery (from failure/timeout).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Phase#RECOVERY policy choice: Recovery (from failure/timeout). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Phase; its declared order and identity are preserved.
         */
        RECOVERY,

        /** Suspend (pause operation).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Phase#SUSPEND policy choice: Suspend (pause operation). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Phase; its declared order and identity are preserved.
         */
        SUSPEND,

        /** Resume (continue operation).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Phase#RESUME policy choice: Resume (continue operation). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Phase; its declared order and identity are preserved.
         */
        RESUME
    }

    // --- TRIGGER ---

    /**
     * Event that triggers this hook.
     *
     * @return trigger
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#trigger intent: Event that triggers this hook. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Trigger}; default expression {@code Trigger.LIFECYCLE}.
     */
    Trigger trigger() default Trigger.LIFECYCLE;

    /** Hook triggers.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHookSpec.Trigger choices described as Hook triggers. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 11 constants in the existing source order.
     */
    enum Trigger {
        /** Normal lifecycle event.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Trigger#LIFECYCLE policy choice: Normal lifecycle event. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Trigger; its declared order and identity are preserved.
         */
        LIFECYCLE,

        /** Application cold start.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Trigger#COLD_BOOT policy choice: Application cold start. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Trigger; its declared order and identity are preserved.
         */
        COLD_BOOT,

        /** Application warm restart.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Trigger#WARM_BOOT policy choice: Application warm restart. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Trigger; its declared order and identity are preserved.
         */
        WARM_BOOT,

        /** Session timeout.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Trigger#SESSION_TIMEOUT policy choice: Session timeout. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Trigger; its declared order and identity are preserved.
         */
        SESSION_TIMEOUT,

        /** Unexpected crash.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Trigger#CRASH policy choice: Unexpected crash. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Trigger; its declared order and identity are preserved.
         */
        CRASH,

        /** Signal received (SIGTERM, etc).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Trigger#SIGNAL policy choice: Signal received (SIGTERM, etc). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Trigger; its declared order and identity are preserved.
         */
        SIGNAL,

        /** Configuration change.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Trigger#CONFIG_CHANGE policy choice: Configuration change. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Trigger; its declared order and identity are preserved.
         */
        CONFIG_CHANGE,

        /** Health check request.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Trigger#HEALTH_CHECK policy choice: Health check request. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Trigger; its declared order and identity are preserved.
         */
        HEALTH_CHECK,

        /** Manual trigger.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Trigger#MANUAL policy choice: Manual trigger. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Trigger; its declared order and identity are preserved.
         */
        MANUAL,

        /** Scheduled trigger.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Trigger#SCHEDULED policy choice: Scheduled trigger. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Trigger; its declared order and identity are preserved.
         */
        SCHEDULED,

        /** Context switch (handoff).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Trigger#CONTEXT_SWITCH policy choice: Context switch (handoff). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Trigger; its declared order and identity are preserved.
         */
        CONTEXT_SWITCH
    }

    // --- PRIORITY ---

    /**
     * Execution priority (lower = earlier). 0 = highest priority (runs first).
     *
     * @return priority
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#priority intent: Execution priority (lower = earlier). 0 = highest priority (runs first). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 100}.
     */
    int priority() default 100;

    /**
     * Named dependency - run after this hook.
     *
     * @return hook names to wait for
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#after intent: Named dependency - run after this hook. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] after() default {};

    /**
     * Named dependency - run before this hook.
     *
     * @return hook names that wait for this
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#before intent: Named dependency - run before this hook. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] before() default {};

    // --- CHECKPOINT ---

    /**
     * Whether to checkpoint state at this hook.
     *
     * @return true to checkpoint
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#checkpoint intent: Whether to checkpoint state at this hook. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean checkpoint() default false;

    /**
     * Checkpoint store identifier.
     *
     * @return store name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#checkpointStore intent: Checkpoint store identifier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "default"}.
     */
    String checkpointStore() default "default";

    /**
     * Whether to restore from checkpoint if available.
     *
     * @return true to restore
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#restoreFromCheckpoint intent: Whether to restore from checkpoint if available. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean restoreFromCheckpoint() default true;

    /**
     * Maximum checkpoint age in seconds (0 = no limit).
     *
     * @return max age
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#maxCheckpointAgeSeconds intent: Maximum checkpoint age in seconds (0 = no limit). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int maxCheckpointAgeSeconds() default 0;

    // --- TIMEOUT ---

    /**
     * Maximum execution time in milliseconds. 0 = no timeout.
     *
     * @return timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#timeoutMs intent: Maximum execution time in milliseconds. 0 = no timeout. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
     */
    int timeoutMs() default 30000;

    /**
     * Action on timeout.
     *
     * @return timeout action
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#onTimeout intent: Action on timeout. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code TimeoutAction}; default expression {@code TimeoutAction.LOG_CONTINUE}.
     */
    TimeoutAction onTimeout() default TimeoutAction.LOG_CONTINUE;

    /** Timeout actions.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHookSpec.TimeoutAction choices described as Timeout actions. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum TimeoutAction {
        /** Log warning and continue.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.TimeoutAction#LOG_CONTINUE policy choice: Log warning and continue. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.TimeoutAction; its declared order and identity are preserved.
         */
        LOG_CONTINUE,

        /** Throw exception.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.TimeoutAction#EXCEPTION policy choice: Throw exception. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.TimeoutAction; its declared order and identity are preserved.
         */
        EXCEPTION,

        /** Force shutdown.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.TimeoutAction#FORCE_SHUTDOWN policy choice: Force shutdown. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.TimeoutAction; its declared order and identity are preserved.
         */
        FORCE_SHUTDOWN,

        /** Skip and proceed.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.TimeoutAction#SKIP policy choice: Skip and proceed. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.TimeoutAction; its declared order and identity are preserved.
         */
        SKIP
    }

    // --- RETRY ---

    /**
     * Maximum retry attempts on failure.
     *
     * @return max retries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#maxRetries intent: Maximum retry attempts on failure. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3}.
     */
    int maxRetries() default 3;

    /**
     * Delay between retries in milliseconds.
     *
     * @return retry delay
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#retryDelayMs intent: Delay between retries in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1000}.
     */
    int retryDelayMs() default 1000;

    /**
     * Whether failure is critical (blocks boot).
     *
     * @return true if critical
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#critical intent: Whether failure is critical (blocks boot). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean critical() default false;

    // --- ASYNC ---

    /**
     * Whether hook runs asynchronously.
     *
     * @return true for async
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#async intent: Whether hook runs asynchronously. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean async() default false;

    /**
     * Whether to block until async hook completes.
     *
     * @return true to await
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#awaitCompletion intent: Whether to block until async hook completes. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean awaitCompletion() default true;

    // --- SIGNAL HANDLING ---

    /**
     * OS signals that trigger this hook.
     *
     * @return signals
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#signals intent: OS signals that trigger this hook. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Signal[]}; default expression {@code {}}.
     */
    Signal[] signals() default {};

    /** OS signals.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHookSpec.Signal choices described as OS signals. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum Signal {
        /** SIGTERM - termination request.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Signal#TERM policy choice: SIGTERM - termination request. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Signal; its declared order and identity are preserved.
         */
        TERM,

        /** SIGINT - interrupt (Ctrl+C).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Signal#INT policy choice: SIGINT - interrupt (Ctrl+C). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Signal; its declared order and identity are preserved.
         */
        INT,

        /** SIGHUP - hangup (reload config).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Signal#HUP policy choice: SIGHUP - hangup (reload config). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Signal; its declared order and identity are preserved.
         */
        HUP,

        /** SIGUSR1 - user-defined 1.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Signal#USR1 policy choice: SIGUSR1 - user-defined 1. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Signal; its declared order and identity are preserved.
         */
        USR1,

        /** SIGUSR2 - user-defined 2.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.Signal#USR2 policy choice: SIGUSR2 - user-defined 2. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.Signal; its declared order and identity are preserved.
         */
        USR2
    }

    // --- KUBERNETES LIFECYCLE ---

    /**
     * Kubernetes lifecycle hook type.
     *
     * @return k8s hook
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#k8sHook intent: Kubernetes lifecycle hook type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code K8sHook}; default expression {@code K8sHook.NONE}.
     */
    K8sHook k8sHook() default K8sHook.NONE;

    /** Kubernetes lifecycle hooks.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHookSpec.K8sHook choices described as Kubernetes lifecycle hooks. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum K8sHook {
        /** Not a K8s hook.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.K8sHook#NONE policy choice: Not a K8s hook. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.K8sHook; its declared order and identity are preserved.
         */
        NONE,

        /** PostStart - after container starts.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.K8sHook#POST_START policy choice: PostStart - after container starts. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.K8sHook; its declared order and identity are preserved.
         */
        POST_START,

        /** PreStop - before container stops.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.K8sHook#PRE_STOP policy choice: PreStop - before container stops. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.K8sHook; its declared order and identity are preserved.
         */
        PRE_STOP,

        /** Liveness probe handler.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.K8sHook#LIVENESS policy choice: Liveness probe handler. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.K8sHook; its declared order and identity are preserved.
         */
        LIVENESS,

        /** Readiness probe handler.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.K8sHook#READINESS policy choice: Readiness probe handler. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.K8sHook; its declared order and identity are preserved.
         */
        READINESS,

        /** Startup probe handler.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.K8sHook#STARTUP policy choice: Startup probe handler. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.K8sHook; its declared order and identity are preserved.
         */
        STARTUP
    }

    // --- IDENTIFICATION ---

    /**
     * Hook name (for dependencies).
     *
     * @return name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#name intent: Hook name (for dependencies). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String name() default "";

    /**
     * Description of what this hook does.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#value intent: Description of what this hook does. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- LOGGING ---

    /**
     * Whether to log hook execution.
     *
     * @return true to log
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#logged intent: Whether to log hook execution. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean logged() default true;

    /**
     * Log level for hook events.
     *
     * @return log level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#logLevel intent: Log level for hook events. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code LogLevel}; default expression {@code LogLevel.INFO}.
     */
    LogLevel logLevel() default LogLevel.INFO;

    /** Log levels.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHookSpec.LogLevel choices described as Log levels. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum LogLevel {
        /** Trace.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.LogLevel#TRACE policy choice: Trace. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.LogLevel; its declared order and identity are preserved.
         */
        TRACE,
        /** Debug.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.LogLevel#DEBUG policy choice: Debug. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.LogLevel; its declared order and identity are preserved.
         */
        DEBUG,
        /** Info.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.LogLevel#INFO policy choice: Info. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.LogLevel; its declared order and identity are preserved.
         */
        INFO,
        /** Warning.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.LogLevel#WARN policy choice: Warning. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.LogLevel; its declared order and identity are preserved.
         */
        WARN,
        /** Error.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHookSpec.LogLevel#ERROR policy choice: Error. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHookSpec.LogLevel; its declared order and identity are preserved.
         */
        ERROR
    }

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHookSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
