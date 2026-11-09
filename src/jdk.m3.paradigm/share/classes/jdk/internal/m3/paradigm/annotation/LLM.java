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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * LLM - Intent Specification for Interface-Oriented Programming.
 *
 * <h2>From Grand Java Patterns Proof - IOP Section</h2> <blockquote> "Perfect
 * IOP is when intent is authored once, correctness is compiled, and execution
 * is enforced - without the programmer ever thinking about how." </blockquote>
 *
 * <h2>Core Principle</h2>
 *
 * <pre>
 * Interfaces define intent and execution boundaries.
 * Implementations are generated or supplied.
 * Runtime enforces invariants invisibly.
 *
 * {@code @LLM} annotation on interface = Intent Contract
 * Generated implementation = Compiled consequence
 * </pre>
 *
 * <h2>The IOP Workflow</h2>
 *
 * <pre>
 * 1. Human provides high-level intent via @LLM
 * 2. LLM proposes interface (methods, names, contracts)
 * 3. Human reviews and approves interface
 * 4. System generates implementation
 * 5. Tests verify behavior
 * 6. Runtime enforces execution
 * </pre>
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * &#64;LLM("""
 *     Listen for user text,
 *     extract meaning and intent,
 *     persist to memory,
 *     emit events,
 *     support cancellation and observation.
 *     """)
 * interface MemoryIngestor {
 *
 *     {@code @Listener}
 *     void ingest(TextInput input);
 * }
 * }
 * </pre>
 *
 * <h2>Compile-Time Only</h2>
 * <p>
 * This annotation has SOURCE retention:
 * <ul>
 * <li>Processed at compile time by Maven plugin</li>
 * <li>Uses Ollama (local LLM) to generate implementation</li>
 * <li>Produces @AIGenerated marked classes</li>
 * <li>Never reaches runtime</li>
 * </ul>
 *
 * <h2>Paradigm Constraints</h2>
 * <p>
 * Generated code must adhere to:
 * <ul>
 * <li>Interface-Oriented Programming</li>
 * <li>AnOP semantics (listeners, jobs)</li>
 * <li>Determinism</li>
 * <li>Cancellation support</li>
 * <li>Observability</li>
 * <li>Non-blocking guarantees</li>
 * </ul>
 *
 * <h2>Hard Rule</h2> <blockquote> "If the LLM is unavailable, the build may
 * fail - but the runtime must never care." </blockquote>
 *
 * @since Grand Java Patterns Proof - IOP Section
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/LLM.java}
 * blob {@code 0062315954b6b1587d2b5d2316d5c0e6e9d3f933} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface LLM {

    /**
     * Natural language description of the desired behavior. This is the primary
     * intent specification.
     *
     * @return intent description
     */
    String value();

    /**
     * Constraints that the generated code must satisfy. These are non-negotiable
     * requirements.
     *
     * @return array of constraints
     */
    String[] constraints() default {};

    /**
     * Context annotations to apply to generated code. These declare execution
     * contexts.
     *
     * @return array of context annotation types
     */
    Class<? extends Annotation>[] contexts() default {};

    /**
     * Whether to generate test cases.
     *
     * @return true to generate tests
     */
    boolean generateTests() default true;

    /**
     * Model to use for generation. Default uses local Ollama.
     *
     * @return model identifier
     */
    String model() default "ollama:qwen2.5-coder:latest";

    /**
     * Output package for generated classes. Defaults to same package +
     * ".generated".
     *
     * @return output package name
     */
    String outputPackage() default "";
}
