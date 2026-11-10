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

import jdk.internal.m3.paradigm.annotation.IFieldSpec;
import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IListenerSpec - Listener generation specification for AI code generation.
 *
 * <h2>Java Listener Patterns</h2>
 *
 * <pre>
 * Java has multiple listener patterns:
 *   - PropertyChangeListener (JavaBeans)
 *   - Flow.Publisher/Subscriber (JDK 9+ Reactive)
 *   - Observer/Observable (classic, deprecated)
 *   - EventListener (custom events)
 *
 * &#64;IListenerSpec unifies these into one annotation for code generation.
 * </pre>
 *
 * <h2>Generated Code Patterns</h2>
 *
 * <h3>Flow.Publisher (Modern, Recommended)</h3>
 *
 * <pre>{@code
 * &#64;IListenerSpec(style = FLOW)
 * public class UserService {
 *
 * 	// Generates:
 * 	private final SubmissionPublisher&lt;UserEvent> publisher = new SubmissionPublisher&lt;>();
 *
 * 	public Flow.Publisher&lt;UserEvent> events() {
 * 		return publisher;
 * 	}
 *
 * 	private void fireEvent(UserEvent event) {
 * 		publisher.submit(event);
 * 	}
 * }
 * }</pre>
 *
 * <h3>PropertyChangeSupport (JavaBeans)</h3>
 *
 * <pre>{@code
 * &#64;IListenerSpec(style = PROPERTY_CHANGE)
 * public class Person {
 *
 * 	// Generates:
 * 	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
 *
 * 	public void addPropertyChangeListener(PropertyChangeListener l) {
 * 		pcs.addPropertyChangeListener(l);
 * 	}
 *
 * 	public void setName(String newName) {
 * 		String old = this.name;
 * 		this.name = newName;
 * 		pcs.firePropertyChange("name", old, newName);
 * 	}
 * }
 * }</pre>
 *
 * <h3>Custom Event Listener</h3>
 *
 * <pre>{@code
 * &#64;IListenerSpec(style = CUSTOM, eventType = OrderEvent.class, listenerInterface = OrderListener.class, addMethod = "addOrderListener", removeMethod = "removeOrderListener", fireMethod = "fireOrderEvent")
 * public class OrderService {
 * 	// Generates full listener management
 * }
 * }</pre>
 *
 * @since IOP 1.0 - Listener Generation
 * @see IFieldSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares listener API style, event types, dispatch, buffering, backpressure and reference preferences; it registers no listener, dispatches no event and proves no thread safety.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.FIELD, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IListenerSpec.java}
 * blob {@code ac773de4f94853d2fee7aa47d0361014c9baa1ab} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IListenerSpec {

    // --- LISTENER STYLE ---

    /**
     * Listener pattern style to generate.
     *
     * @return listener style
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#style intent: Listener pattern style to generate. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Style}; default expression {@code Style.FLOW}.
     */
    Style style() default Style.FLOW;

    /** Listener pattern styles.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IListenerSpec.Style choices described as Listener pattern styles. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Style {
        /** JDK Flow.Publisher/Subscriber (recommended, backpressure).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.Style#FLOW policy choice: JDK Flow.Publisher/Subscriber (recommended, backpressure). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.Style; its declared order and identity are preserved.
         */
        FLOW,

        /** PropertyChangeSupport (JavaBeans, property-level).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.Style#PROPERTY_CHANGE policy choice: PropertyChangeSupport (JavaBeans, property-level). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.Style; its declared order and identity are preserved.
         */
        PROPERTY_CHANGE,

        /** VetoableChangeSupport (constrained properties).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.Style#VETOABLE_CHANGE policy choice: VetoableChangeSupport (constrained properties). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.Style; its declared order and identity are preserved.
         */
        VETOABLE_CHANGE,

        /** Custom event listener interface.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.Style#CUSTOM policy choice: Custom event listener interface. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.Style; its declared order and identity are preserved.
         */
        CUSTOM,

        /** Callback-based (simple Consumer).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.Style#CALLBACK policy choice: Callback-based (simple Consumer). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.Style; its declared order and identity are preserved.
         */
        CALLBACK,

        /** CompletableFuture (async notification).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.Style#FUTURE policy choice: CompletableFuture (async notification). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.Style; its declared order and identity are preserved.
         */
        FUTURE
    }

    // --- EVENT TYPE ---

    /**
     * Event class to publish.
     *
     * @return event class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#eventType intent: Event class to publish. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> eventType() default Void.class;

    /**
     * Event types to generate (for FLOW style). Each becomes a case in a sealed
     * event hierarchy.
     *
     * @return event type names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#eventTypes intent: Event types to generate (for FLOW style). Each becomes a case in a sealed event hierarchy. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] eventTypes() default {};

    // --- CUSTOM LISTENER ---

    /**
     * Listener interface for CUSTOM style.
     *
     * @return listener interface class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#listenerInterface intent: Listener interface for CUSTOM style. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> listenerInterface() default Void.class;

    /**
     * Method name for adding listeners.
     *
     * @return add method name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#addMethod intent: Method name for adding listeners. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "addListener"}.
     */
    String addMethod() default "addListener";

    /**
     * Method name for removing listeners.
     *
     * @return remove method name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#removeMethod intent: Method name for removing listeners. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "removeListener"}.
     */
    String removeMethod() default "removeListener";

    /**
     * Method name for firing events.
     *
     * @return fire method name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#fireMethod intent: Method name for firing events. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "fireEvent"}.
     */
    String fireMethod() default "fireEvent";

    // --- THREAD SAFETY ---

    /**
     * Whether listener management is thread-safe.
     *
     * @return true for thread-safe
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#threadSafe intent: Whether listener management is thread-safe. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean threadSafe() default true;

    /**
     * Execution model for event dispatch.
     *
     * @return execution model
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#execution intent: Execution model for event dispatch. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ExecutionModel}; default expression {@code ExecutionModel.SYNC}.
     */
    ExecutionModel execution() default ExecutionModel.SYNC;

    /** Event dispatch execution models.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IListenerSpec.ExecutionModel choices described as Event dispatch execution models. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum ExecutionModel {
        /** Synchronous dispatch (same thread).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.ExecutionModel#SYNC policy choice: Synchronous dispatch (same thread). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.ExecutionModel; its declared order and identity are preserved.
         */
        SYNC,

        /** Asynchronous dispatch (virtual threads).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.ExecutionModel#ASYNC_VIRTUAL policy choice: Asynchronous dispatch (virtual threads). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.ExecutionModel; its declared order and identity are preserved.
         */
        ASYNC_VIRTUAL,

        /** Asynchronous dispatch (platform threads).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.ExecutionModel#ASYNC_PLATFORM policy choice: Asynchronous dispatch (platform threads). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.ExecutionModel; its declared order and identity are preserved.
         */
        ASYNC_PLATFORM,

        /** Dispatch via executor service.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.ExecutionModel#EXECUTOR policy choice: Dispatch via executor service. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.ExecutionModel; its declared order and identity are preserved.
         */
        EXECUTOR,

        /** Dispatch via event bus.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.ExecutionModel#EVENT_BUS policy choice: Dispatch via event bus. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.ExecutionModel; its declared order and identity are preserved.
         */
        EVENT_BUS
    }

    // --- FLOW OPTIONS ---

    /**
     * Buffer size for Flow.Publisher.
     *
     * @return buffer size
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#bufferSize intent: Buffer size for Flow.Publisher. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 256}.
     */
    int bufferSize() default 256;

    /**
     * Backpressure strategy for Flow.
     *
     * @return backpressure strategy
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#backpressure intent: Backpressure strategy for Flow. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code BackpressureStrategy}; default expression {@code BackpressureStrategy.BUFFER}.
     */
    BackpressureStrategy backpressure() default BackpressureStrategy.BUFFER;

    /** Backpressure strategies.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IListenerSpec.BackpressureStrategy choices described as Backpressure strategies. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum BackpressureStrategy {
        /** Buffer events (may grow unbounded).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.BackpressureStrategy#BUFFER policy choice: Buffer events (may grow unbounded). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.BackpressureStrategy; its declared order and identity are preserved.
         */
        BUFFER,

        /** Drop oldest events when full.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.BackpressureStrategy#DROP_OLDEST policy choice: Drop oldest events when full. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.BackpressureStrategy; its declared order and identity are preserved.
         */
        DROP_OLDEST,

        /** Drop newest events when full.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.BackpressureStrategy#DROP_NEWEST policy choice: Drop newest events when full. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.BackpressureStrategy; its declared order and identity are preserved.
         */
        DROP_NEWEST,

        /** Block producer until consumed.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.BackpressureStrategy#BLOCK policy choice: Block producer until consumed. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.BackpressureStrategy; its declared order and identity are preserved.
         */
        BLOCK,

        /** Error when buffer full.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IListenerSpec.BackpressureStrategy#ERROR policy choice: Error when buffer full. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IListenerSpec.BackpressureStrategy; its declared order and identity are preserved.
         */
        ERROR
    }

    // --- PROPERTY CHANGE OPTIONS ---

    /**
     * Property names to fire events for. Empty = all properties.
     *
     * @return property names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#properties intent: Property names to fire events for. Empty = all properties. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] properties() default {};

    /**
     * Whether to fire events for nested property changes.
     *
     * @return true for nested events
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#nestedEvents intent: Whether to fire events for nested property changes. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean nestedEvents() default false;

    // --- WEAK REFERENCES ---

    /**
     * Whether to use weak references for listeners. Prevents memory leaks from
     * forgotten listeners.
     *
     * @return true for weak references
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#weakListeners intent: Whether to use weak references for listeners. Prevents memory leaks from forgotten listeners. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean weakListeners() default false;

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IListenerSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
