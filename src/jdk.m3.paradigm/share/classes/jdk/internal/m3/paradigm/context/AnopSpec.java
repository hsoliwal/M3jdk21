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
package jdk.internal.m3.paradigm.context;

/**
 * AnOP Specification - Annotation-Oriented Programming via Lombok + Jakarta.
 *
 * <h2>THE ANOP PRINCIPLE</h2>
 * <blockquote>
 * AnOP is UX, not semantics.
 * Annotations must behave like @Buffered.
 * They may: decorate, constrain, optimize.
 * They must NEVER: introduce queues, change execution domain,
 *                  spawn threads, bind brokers.
 * <br><br>
 * "AnOP is Jakarta for meaning + Lombok for mechanics,
 *  executed entirely at compile time,
 *  leaving nothing behind but honest Java code."
 * </blockquote>
 *
 * <h2>THE TWO-LAYER ANOP MODEL</h2>
 * <pre>
 * +---------------------------------------------------------------+
 * |  LAYER A: Jakarta-style (Intent)                              |
 * |  - Defines SEMANTIC MEANING                                   |
 * |  - Policy, constraint, role refinement                        |
 * |  - Jakarta NEVER rewrites code                                |
 * +---------------------------------------------------------------+
 * |  LAYER B: Lombok-style (Mechanics)                            |
 * |  - Performs CODE TRANSFORMATION                               |
 * |  - Generates wrappers, inlines aspects                        |
 * |  - Lombok NEVER decides meaning                               |
 * +---------------------------------------------------------------+
 * </pre>
 *
 * <h2>THE COMPILATION PIPELINE</h2>
 * <pre>
 * SOURCE annotations
 *    |
 * Annotation processor / compiler plugin
 *    |
 * Generated wrappers / decorators
 *    |
 * Plain Java bytecode
 *    |
 * Annotations GONE
 *    |
 * JIT sees REAL control flow
 * </pre>
 *
 * <h2>NON-NEGOTIABLE CONSTRAINTS</h2>
 *
 * <h3>ALLOWED</h3>
 * <ul>
 *   <li>SOURCE retention only</li>
 *   <li>Consumed by annotation processor</li>
 *   <li>Translated into generated code</li>
 *   <li>Overwritten on regeneration</li>
 *   <li>Deterministic output</li>
 *   <li>JIT-visible logic</li>
 * </ul>
 *
 * <h3>FORBIDDEN</h3>
 * <ul>
 *   <li>RUNTIME retention</li>
 *   <li>Reflection</li>
 *   <li>Classpath scanning</li>
 *   <li>Method interception</li>
 *   <li>Dynamic dispatch decisions</li>
 *   <li>Proxy chains for semantics</li>
 * </ul>
 *
 * <h2>ALLOWED VS FORBIDDEN INTENT</h2>
 *
 * <h3>Allowed Intent</h3>
 * <ul>
 *   <li>decoration (like @Buffered)</li>
 *   <li>policy (@Retry, @Timeout)</li>
 *   <li>constraints (@SingleWriter)</li>
 *   <li>observability (@Observable)</li>
 *   <li>safety (@Cancellable)</li>
 * </ul>
 *
 * <h3>Forbidden Intent</h3>
 * <ul>
 *   <li>threading</li>
 *   <li>async execution</li>
 *   <li>queue selection</li>
 *   <li>broker binding</li>
 *   <li>retries with runtime policies</li>
 *   <li>dynamic routing</li>
 * </ul>
 *
 * <h2>THE DECORATION RULE</h2>
 * <blockquote>
 * "Annotations must behave like language modifiers, not frameworks."
 * The mental model must always be equivalent to:
 * {@code @Buffered InputStream in; -> new BufferedInputStream(original);}
 * If the annotation cannot be explained as SIMPLE DECORATION, it is invalid.
 * </blockquote>
 *
 * <h2>ASPECT BEHAVIOR (compile-time only)</h2>
 * <pre>
 * - Aspect logic is AUTHORED ONCE (normal Java classes)
 * - Wrappers are GENERATED
 * - Before / around / after is INLINED
 * - Regeneration overwrites everything
 * - Runtime does nothing special
 *
 * AOP becomes CODE GENERATION, not runtime machinery.
 * </pre>
 *
 * <h2>TWO-PHASE COMPILATION</h2>
 * <pre>
 * Phase 1 - Aspect Definition Phase (authoritative):
 *   - Define aspect behavior as code (normal Java classes)
 *   - Authored, reviewed, tested
 *
 * Phase 2 - Wrapper Generation Phase (mechanical):
 *   - Reads annotations / intent
 *   - Resolves which aspects apply
 *   - Orders them deterministically
 *   - Inlines aspect logic into generated wrappers
 * </pre>
 *
 * <h2>WHY THIS BEATS RUNTIME AOP</h2>
 * <pre>
 * | Runtime AOP           | Compile-time AnOP      |
 * |-----------------------|------------------------|
 * | Aspect logic hidden   | Aspect logic explicit  |
 * | Ordering implicit     | Ordering fixed         |
 * | Debugging painful     | Debugging normal       |
 * | Stack traces opaque   | Stack traces honest    |
 * | Late failures         | Early failures         |
 * | Reflection heavy      | Plain Java             |
 * </pre>
 *
 * <h2>FINAL LOCK</h2>
 * <blockquote>
 * "SOLID compile-time annotations give you the simplicity people love
 *  without the runtime lies people fear."
 * </blockquote>
 *
 * @since Grand Java Patterns Proof - AnOP Section
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/spec/AnopSpec.java}
 * blob {@code d2fbf99908f3eb4554b4a5773b53b310ca652554} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class AnopSpec {

    /**
     * Annotation retention policy for AnOP.
     * All AnOP annotations MUST use SOURCE retention.
     */
    public static final String REQUIRED_RETENTION = "SOURCE";

    /**
     * Maximum allowed annotation parameters.
     * Annotations with runtime tuning knobs are forbidden.
     */
    public static final int MAX_ANNOTATION_PARAMS = 3;

    private AnopSpec() {
        // Specification only - not instantiable
    }
}
