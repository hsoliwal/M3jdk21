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
 * IStateSpec - State machine specification for AI code generation.
 *
 * <h2>Inspired By State Machine DSLs</h2>
 *
 * <pre>
 * Humans created many state machine specifications:
 *   - SCXML (State Chart XML)
 *   - XState (JavaScript state machines)
 *   - UML State Diagrams
 *   - Finite Automata notation
 *   - Ragel (state machine compiler)
 *
 * &#64;IStateSpec unifies state machine definition into IOP.
 * </pre>
 *
 * <h2>Generated Code PatternKind</h2>
 *
 * <pre>{@code
 * // From: @IStateSpec(states = {"IDLE","RUNNING","PAUSED","STOPPED"}, initial
 * // = "IDLE")
 * // Generates:
 *
 * public sealed interface State permits Idle, Running, Paused, Stopped {
 *
 * 	State transition(Event event);
 * }
 *
 * public record Idle() implements State {
 *
 * 	public State transition(Event event) {
 * 		return switch (event) {
 * 			case Start start -> new Running();
 * 			default -> this;
 * 		};
 * 	}
 * }
 * // ... similar for other states
 *
 * public sealed interface Event permits Start, Pause, Resume, Stop {
 * }
 * public record Start() implements Event {
 * }
 * // ... similar for other events
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * &#64;IStateSpec(
 *     value = "Order lifecycle state machine",states = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED",
 * "CANCELLED"}, initial = "PENDING", terminal = {"DELIVERED", "CANCELLED"}, transitions = {
 * &#64;Transition(from = "PENDING", to = "CONFIRMED", on = "confirm"),
 * &#64;Transition(from = "PENDING", to = "CANCELLED", on = "cancel"),
 * &#64;Transition(from = "CONFIRMED", to = "SHIPPED", on = "ship"),
 * &#64;Transition(from = "CONFIRMED", to = "CANCELLED", on = "cancel"),
 * &#64;Transition(from = "SHIPPED", to = "DELIVERED", on = "deliver")})
 * public interface IOrderStateMachine&lt;Self extends IOrderStateMachine&lt;Self>> extends IContext&lt;Self> {
 *
 * 	State current();
 *
 * 	State transition(Event event);
 *
 * 	boolean canTransition(Event event);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - State Machine Specification
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares state names, transitions, guards, hooks, generation style and validation preferences; it executes no transition and proves no reachability or completeness property.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IStateSpec.java}
 * blob {@code 8b9b889e80849a328d23f315ca1f942aad245dea} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IStateSpec {

    // --- SPECIFICATION ---

    /**
     * Description of the state machine.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#value intent: Description of the state machine. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- STATES ---

    /**
     * All possible states.
     *
     * @return state names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#states intent: All possible states. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; an explicit value is required.
     */
    String[] states();

    /**
     * Initial state.
     *
     * @return initial state name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#initial intent: Initial state. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String initial();

    /**
     * Terminal/final states (no outgoing transitions).
     *
     * @return terminal state names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#terminal intent: Terminal/final states (no outgoing transitions). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] terminal() default {};

    // --- TRANSITIONS ---

    /**
     * State transitions.
     *
     * @return transition definitions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#transitions intent: State transitions. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Transition[]}; default expression {@code {}}.
     */
    Transition[] transitions() default {};

    /** A state transition definition.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares source and target states, triggering event, guard and action strings; it evaluates no guard or action and has implicit CLASS retention with no explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Transition {

        /** Source state.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IStateSpec.Transition#from intent: Source state. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String from();

        /** Target state.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IStateSpec.Transition#to intent: Target state. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String to();

        /** Event/action that triggers transition.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IStateSpec.Transition#on intent: Event/action that triggers transition. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String on();

        /** Guard condition (Java expression).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IStateSpec.Transition#guard intent: Guard condition (Java expression). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String guard() default "";

        /** Action to execute during transition.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IStateSpec.Transition#action intent: Action to execute during transition. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String action() default "";
    }

    // --- STATE DATA ---

    /**
     * Context/data class that accompanies state.
     *
     * @return context class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#context intent: Context/data class that accompanies state. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> context() default Void.class;

    /**
     * Whether states carry data (extended state machines).
     *
     * @return true for extended state machine
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#extended intent: Whether states carry data (extended state machines). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean extended() default false;

    // --- GENERATION OPTIONS ---

    /**
     * Generation style for state machine.
     *
     * @return style
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#style intent: Generation style for state machine. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Style}; default expression {@code Style.SEALED}.
     */
    Style style() default Style.SEALED;

    /** State machine generation styles.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IStateSpec.Style choices described as State machine generation styles. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Style {
        /** Sealed interface hierarchy (recommended).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStateSpec.Style#SEALED policy choice: Sealed interface hierarchy (recommended). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStateSpec.Style; its declared order and identity are preserved.
         */
        SEALED,

        /** Enum-based states.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStateSpec.Style#ENUM policy choice: Enum-based states. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStateSpec.Style; its declared order and identity are preserved.
         */
        ENUM,

        /** Class-based with State pattern.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStateSpec.Style#CLASS policy choice: Class-based with State pattern. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStateSpec.Style; its declared order and identity are preserved.
         */
        CLASS,

        /** Table-driven state machine.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IStateSpec.Style#TABLE policy choice: Table-driven state machine. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IStateSpec.Style; its declared order and identity are preserved.
         */
        TABLE
    }

    /**
     * Whether to generate immutable state transitions. If true, transition returns
     * new state. If false, mutates current.
     *
     * @return true for immutable
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#immutable intent: Whether to generate immutable state transitions. If true, transition returns new state. If false, mutates current. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean immutable() default true;

    /**
     * Whether to generate event sourcing support.
     *
     * @return true for event sourcing
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#eventSourced intent: Whether to generate event sourcing support. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean eventSourced() default false;

    /**
     * Whether to generate state history tracking.
     *
     * @return true to track history
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#trackHistory intent: Whether to generate state history tracking. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean trackHistory() default false;

    // --- VALIDATION ---

    /**
     * Whether to validate transitions at compile time.
     *
     * @return true to validate
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#validateTransitions intent: Whether to validate transitions at compile time. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean validateTransitions() default true;

    /**
     * Whether all states must be reachable from initial.
     *
     * @return true to check reachability
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#checkReachability intent: Whether all states must be reachable from initial. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean checkReachability() default true;

    /**
     * Whether all non-terminal states must have outgoing transitions.
     *
     * @return true to check completeness
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#checkCompleteness intent: Whether all non-terminal states must have outgoing transitions. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean checkCompleteness() default false;

    // --- HOOKS ---

    /**
     * Method to call on any state entry.
     *
     * @return entry hook method name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#onEntry intent: Method to call on any state entry. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String onEntry() default "";

    /**
     * Method to call on any state exit.
     *
     * @return exit hook method name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#onExit intent: Method to call on any state exit. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String onExit() default "";

    /**
     * Method to call on any transition.
     *
     * @return transition hook method name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#onTransition intent: Method to call on any transition. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String onTransition() default "";

    /**
     * Method to call on invalid transition attempt.
     *
     * @return error hook method name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#onInvalidTransition intent: Method to call on invalid transition attempt. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String onInvalidTransition() default "";

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IStateSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
