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
 * Compile-Time Generation Specification - The Authoritative Pipeline.
 *
 * <h2>CORE PRINCIPLE</h2>
 * <blockquote>
 * "In Java, anything you can decide at compile time
 *  you SHOULD decide at compile time -
 *  because runtime will never be cheaper."
 * <br><br>
 * Not faster. Not simpler. Not clearer.
 * You're not optimizing prematurely.
 * You're ALIGNING WITH THE LANGUAGE.
 * </blockquote>
 *
 * <h2>THE COMPILE-TIME PIPELINE (Authoritative)</h2>
 * <pre>
 * Interface (IOP)
 *    |
 * Annotations (AnOP)
 *    |
 * Aspect logic (AOP - authored)
 *    |
 * Wrapper generation (compile-time)
 *    |
 * Plain Java bytecode
 *    |
 * Runtime (COP enforcement only)
 *
 * No reflection. No proxies. No surprises.
 * </pre>
 *
 * <h2>REPOSITORY LAYOUT (non-negotiable)</h2>
 * <pre>
 * anop/
 *  +-- anop-api/          &lt;- annotations, interfaces (SOURCE only)
 *  +-- anop-compiler/     &lt;- maven plugin + LLM + processors
 *  +-- anop-runtime/      &lt;- IoC, proxies, contexts, concurrency
 *  +-- app/               &lt;- user application (interfaces only)
 *
 * If these blur, the model collapses.
 * </pre>
 *
 * <h2>ANOP-API: The Language Surface</h2>
 * <pre>
 * Core annotations (SOURCE only):
 * - @LLM         - natural language intent
 * - @Listener    - event listener role
 * - @AIGenerated - generated code marker
 * - @Observable  - observability decoration
 * - @Cancellable - cancellation support
 * - @SingleWriter - concurrency constraint
 * - @Retry       - retry policy
 * - @Timeout     - timeout policy
 *
 * No behavior. No defaults. No runtime presence.
 * </pre>
 *
 * <h2>ANOP-COMPILER: Compile-Time Authority</h2>
 * <pre>
 * Maven plugin entry point:
 * &#64;Mojo(name = "generate", defaultPhase = GENERATE_SOURCES)
 *
 * Pipeline:
 * 1. Scan interfaces
 * 2. Extract intent (InterfaceIntent, MethodIntent records)
 * 3. Call LLM (Ollama, temperature=0, timeout, retries&lt;=1)
 * 4. Normalize + Validate
 * 5. Generate sources (JavaPoet / AST)
 *
 * Generated artifacts:
 * - Implementation class (Foo_Impl)
 * - Execution plan (Foo_ExecutionPlan)
 *
 * Rule: "If LLM unavailable, build may fail - runtime must never care"
 * </pre>
 *
 * <h2>ANOP-RUNTIME: Boring by Design</h2>
 * <pre>
 * ExecutionPlan (immutable):
 * - listener: boolean
 * - singleWriter: boolean
 * - cancellable: boolean
 * - observable: boolean
 *
 * ExecutionContext (COP):
 * - id: UUID
 * - cancelled: AtomicBoolean
 * - startedAt: Instant
 *
 * Contexts are:
 * - created per invocation
 * - owned by runtime
 * - never user-visible
 * </pre>
 *
 * <h2>GENERATED WRAPPER PATTERN</h2>
 * <pre>{@code
 * final class Foo_Proxy implements Foo {
 *     private final Foo target;
 *
 *     Foo_Proxy(Foo target) {
 *         this.target = target;
 *     }
 *
 *     &#64;Override
 *     public Result method(Args args) {
 *         before();
 *         try {
 *             return target.method(args);
 *         } finally {
 *             after();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>WHY COMPILE-TIME IS BETTER</h2>
 * <pre>
 * Compile-time gives you:
 * - Determinism
 * - Visibility
 * - Reviewability
 * - Fail-fast errors
 * - Tooling leverage (IDE, static analysis, tests)
 * - Zero runtime magic
 *
 * Runtime magic gives you:
 * - Opacity you don't want
 * - Decisions you already made earlier
 * - Flexibility you don't need
 * </pre>
 *
 * <h2>THE CORRECT SPLIT</h2>
 * <pre>
 * Compile-time decides:
 * - roles
 * - execution topology
 * - threading model
 * - queues
 * - listeners
 * - jobs
 * - generated classes
 * - static policies
 *
 * Runtime may handle (thin):
 * - context enter/exit
 * - cancellation checks
 * - metrics hooks
 * - tracing
 * - security context
 * - per-invocation state
 *
 * But NEVER topology. Never "this is now async". Never "this is now a listener".
 * </pre>
 *
 * <h2>FINAL RULES</h2>
 * <blockquote>
 * "If an annotation changes behavior,
 *  that behavior must be visible in generated source code
 *  before the program ever runs."
 * <br><br>
 * "A JDK proxy is just a late-bound wrapper.
 *  If the wrapper can be generated early, it should be."
 * <br><br>
 * "Once wrappers are generated, AOP is finished.
 *  Everything else is just Java calling Java."
 * </blockquote>
 *
 * @since Grand Java Patterns Proof - Compile-Time Section
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/spec/CompileTimeSpec.java}
 * blob {@code 9e1efafa5421266925fb7463ae9a1bd501eefe39} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CompileTimeSpec {

    /** Default phase for generation. */
    public static final String GENERATE_PHASE = "GENERATE_SOURCES";

    /** LLM call constraints. */
    public static final int LLM_TEMPERATURE = 0;
    public static final int LLM_MAX_RETRIES = 1;

    private CompileTimeSpec() {
        // Specification only - not instantiable
    }
}
