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
 * IScheduleSpec - Scheduling specification for AI code generation.
 *
 * <h2>Scheduling Patterns</h2>
 *
 * <pre>
 * Scheduling patterns in Java:
 *   - Spring @Scheduled
 *   - Quartz Scheduler
 *   - ScheduledExecutorService
 *   - Timer/TimerTask
 *   - Cron expressions
 *   - Virtual thread scheduled tasks
 *
 * &#64;IScheduleSpec specifies scheduled tasks in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IMaintenanceService {
 *
 *     // Cron schedule
 * &#64;IScheduleSpec(cron = "0 0 2 * * ?", description = "Daily cleanup at 2 AM")
 * void dailyCleanup();
 *
 * // Fixed rate
 * &#64;IScheduleSpec(fixedRate = 60000, initialDelay = 10000, description = "Health check every minute")
 * void healthCheck();
 *
 * // Fixed delay
 * &#64;IScheduleSpec(fixedDelay = 30000, description = "Process queue with 30s pause between")
 * void processQueue();
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Schedule Specification
 * @see IExecSpec
 * @see IHookSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares cron or interval timing, execution, concurrency, lock, retry and timeout preferences; it schedules no task and acquires no lock.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IScheduleSpec.java}
 * blob {@code 8ee7dcc66386f6e16b20726344bd1728fc228f9f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IScheduleSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined schedule pattern.
     *
     * @return option
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#option intent: Predefined schedule pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common schedule patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IScheduleSpec.Option choices described as Common schedule patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Every minute.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.Option#EVERY_MINUTE policy choice: Every minute. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.Option; its declared order and identity are preserved.
         */
        EVERY_MINUTE,

        /** Every hour.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.Option#HOURLY policy choice: Every hour. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.Option; its declared order and identity are preserved.
         */
        HOURLY,

        /** Daily at midnight.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.Option#DAILY policy choice: Daily at midnight. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.Option; its declared order and identity are preserved.
         */
        DAILY,

        /** Weekly on Sunday.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.Option#WEEKLY policy choice: Weekly on Sunday. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.Option; its declared order and identity are preserved.
         */
        WEEKLY,

        /** Monthly on 1st.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.Option#MONTHLY policy choice: Monthly on 1st. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.Option; its declared order and identity are preserved.
         */
        MONTHLY
    }

    // --- CRON ---

    /**
     * Cron expression. Format: "second minute hour day month weekday"
     *
     * @return cron expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#cron intent: Cron expression. Format: "second minute hour day month weekday" Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String cron() default "";

    /**
     * Timezone for cron.
     *
     * @return timezone ID
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#zone intent: Timezone for cron. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String zone() default "";

    // --- FIXED RATE / DELAY ---

    /**
     * Fixed rate in milliseconds. Execute at fixed intervals regardless of
     * completion.
     *
     * @return rate ms
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#fixedRate intent: Fixed rate in milliseconds. Execute at fixed intervals regardless of completion. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code -1}.
     */
    long fixedRate() default -1;

    /**
     * Fixed delay in milliseconds. Execute with fixed delay after completion.
     *
     * @return delay ms
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#fixedDelay intent: Fixed delay in milliseconds. Execute with fixed delay after completion. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code -1}.
     */
    long fixedDelay() default -1;

    /**
     * Initial delay before first execution.
     *
     * @return initial delay ms
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#initialDelay intent: Initial delay before first execution. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 0}.
     */
    long initialDelay() default 0;

    // --- EXECUTION ---

    /**
     * Execution mode.
     *
     * @return mode
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#mode intent: Execution mode. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ExecutionMode}; default expression {@code ExecutionMode.VIRTUAL}.
     */
    ExecutionMode mode() default ExecutionMode.VIRTUAL;

    /** Execution modes.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IScheduleSpec.ExecutionMode choices described as Execution modes. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 3 constants in the existing source order.
     */
    enum ExecutionMode {
        /** Virtual thread (recommended).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.ExecutionMode#VIRTUAL policy choice: Virtual thread (recommended). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.ExecutionMode; its declared order and identity are preserved.
         */
        VIRTUAL,

        /** Platform thread.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.ExecutionMode#PLATFORM policy choice: Platform thread. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.ExecutionMode; its declared order and identity are preserved.
         */
        PLATFORM,

        /** Custom executor.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.ExecutionMode#CUSTOM policy choice: Custom executor. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.ExecutionMode; its declared order and identity are preserved.
         */
        CUSTOM
    }

    /**
     * Custom executor bean name.
     *
     * @return executor name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#executor intent: Custom executor bean name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String executor() default "";

    // --- CONCURRENCY ---

    /**
     * Whether to allow concurrent executions.
     *
     * @return true for concurrent
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#concurrent intent: Whether to allow concurrent executions. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean concurrent() default false;

    /**
     * Lock name for distributed locking.
     *
     * @return lock name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#lockName intent: Lock name for distributed locking. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String lockName() default "";

    /**
     * Lock timeout in milliseconds.
     *
     * @return lock timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#lockTimeout intent: Lock timeout in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 30000}.
     */
    long lockTimeout() default 30000;

    // --- RETRY ---

    /**
     * Max retries on failure.
     *
     * @return max retries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#maxRetries intent: Max retries on failure. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int maxRetries() default 0;

    /**
     * Retry delay in milliseconds.
     *
     * @return retry delay
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#retryDelay intent: Retry delay in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 1000}.
     */
    long retryDelay() default 1000;

    // --- TIMEOUT ---

    /**
     * Execution timeout in milliseconds. 0 = no timeout.
     *
     * @return timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#timeout intent: Execution timeout in milliseconds. 0 = no timeout. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 0}.
     */
    long timeout() default 0;

    /**
     * Action on timeout.
     *
     * @return timeout action
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#onTimeout intent: Action on timeout. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code TimeoutAction}; default expression {@code TimeoutAction.LOG}.
     */
    TimeoutAction onTimeout() default TimeoutAction.LOG;

    /** Timeout actions.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IScheduleSpec.TimeoutAction choices described as Timeout actions. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 3 constants in the existing source order.
     */
    enum TimeoutAction {
        /** Log warning.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.TimeoutAction#LOG policy choice: Log warning. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.TimeoutAction; its declared order and identity are preserved.
         */
        LOG,

        /** Interrupt execution.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.TimeoutAction#INTERRUPT policy choice: Interrupt execution. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.TimeoutAction; its declared order and identity are preserved.
         */
        INTERRUPT,

        /** Skip this run.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IScheduleSpec.TimeoutAction#SKIP policy choice: Skip this run. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IScheduleSpec.TimeoutAction; its declared order and identity are preserved.
         */
        SKIP
    }

    // --- LIFECYCLE ---

    /**
     * Whether to run on startup.
     *
     * @return true to run on startup
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#runOnStartup intent: Whether to run on startup. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean runOnStartup() default false;

    /**
     * Whether schedule is enabled.
     *
     * @return true if enabled
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#enabled intent: Whether schedule is enabled. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean enabled() default true;

    /**
     * Enable/disable property key.
     *
     * @return property key
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#enabledProperty intent: Enable/disable property key. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String enabledProperty() default "";

    // --- DESCRIPTION ---

    /**
     * Schedule description.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#value intent: Schedule description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    /**
     * Human-readable description.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#description intent: Human-readable description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String description() default "";

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IScheduleSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.HAIKU}.
     */
    Model.ModelType model() default Model.ModelType.HAIKU;
}
