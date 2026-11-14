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

package jdk.internal.m3.paradigm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Contextual - Context-Aware Execution Annotation.
 *
 * <h2>From Grand Java Patterns Proof - COP Section</h2> <blockquote> "State
 * changes meaning based on context (observer frame). COP = Context-Oriented
 * Programming, not a Java gimmick." </blockquote>
 *
 * <h2>Semantic Meaning</h2>
 * <p>
 * A method or type annotated with @Contextual declares:
 * <ul>
 * <li>Execution depends on current context</li>
 * <li>Behavior may adapt based on context</li>
 * <li>Context is passed, never owned</li>
 * </ul>
 *
 * <h2>Context Projection</h2>
 *
 * <pre>{@code
 *
 * {@code @Contextual}
 * State process(State s, Context ctx) {
 *     return s.adapt(ctx);
 * }
 *
 * // Context changes meaning, not state:
 * Context observer = new Context("observer-A", 0.5);
 * State projected = process(state, observer);
 * }</pre>
 *
 * <h2>Layer Integration</h2>
 * <p>
 *
 * {@code @Contextual} integrates with the COP layer system:
 * <ul>
 * <li>Layer activation scoping</li>
 * <li>Partial method dispatch</li>
 * <li>Context-dependent behavior</li>
 * </ul>
 *
 * <h2>Generated Code Pattern</h2>
 *
 * <pre>{@code
 *
 * void process_contextual() {
 * 	IContext<?> ctx = IContext.current();
 * 	for (ILayer<?> layer : ILayerActivation.activeLayers()) {
 * 		if (layer.appliesTo(ctx)) {
 * 			// apply layer partial methods
 * 		}
 * 	}
 * 	process_impl();
 * }
 * }</pre>
 *
 * @since Grand Java Patterns Proof - COP Section
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Contextual.java}
 * blob {@code 3f7929a3eafedca24e5b50d7ee81b8c1cd034f07} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Contextual {

    /**
     * Name of the context dimension to use.
     *
     * @return context dimension name
     */
    String dimension() default "";

    /**
     * Whether to propagate context to nested calls.
     *
     * @return true to propagate
     */
    boolean propagate() default true;

    /**
     * Required context type.
     *
     * @return context class
     */
    Class<?> contextType() default Object.class;

    /**
     * Layer classes that must be active.
     *
     * @return array of layer classes
     */
    Class<?>[] requiredLayers() default {};
}
