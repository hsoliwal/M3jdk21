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
 * Pattern Survival Taxonomy - Where Patterns Land (absorbed, not exposed).
 *
 * <h2>CORE PRINCIPLE</h2>
 * <blockquote>
 * "Patterns are not dogma. They are compression artifacts
 * of human reasoning under constraints."
 * <br><br>
 * "Patterns disappear when correctly placed."
 * </blockquote>
 *
 * <h2>GOF PATTERNS (Where They Land)</h2>
 * <pre>
 * Pattern | Implementation | Layer
 * -------------+-------------------------------+--------
 * Proxy | compile-time generated wrapper| AOP
 * Decorator | generated method decoration | AOP
 * Strategy | compile-time selected policy | AnOP
 * Factory | code generation | Compile
 * Template | generated skeleton | Compile
 * Builder | execution plans | COP
 * Singleton | Scoped Authority | COP
 * Chain of R. | IChainHandler interface | IOP
 * Interpreter | IInterpreter interface | IOP
 * Flyweight | IFlyweight interface | OOP
 * Composite | IComponent interface | OOP
 * Memento | IMemento (human-time opt) | OOP
 * </pre>
 *
 * <h2>EIP PATTERNS (Where They Land)</h2>
 * <pre>
 * Pattern | Implementation | Layer
 * ----------------+--------------------------+--------
 * Message Channel | interface boundary | IOP
 * Listener | interface role | IOP
 * Claim Check | memento / lazy load | OOP
 * Aggregator | context composition | COP
 * Router | execution plan | COP
 * Event Store | IEventStore interface | IOP
 * Event Bus | IEventBus interface | IOP
 * </pre>
 *
 * <h2>MICROSERVICE PATTERNS (Where They Land)</h2>
 * <pre>
 * Pattern | Implementation | Layer
 * -----------------+------------------------+--------
 * Service Boundary | interface | IOP
 * Bulkhead | COP isolation | COP
 * Circuit Breaker | policy wrapper | AOP
 * Saga | context choreography | COP
 * Sidecar | generated adapter | Compile
 * </pre>
 *
 * <h2>PATTERN CLASSIFICATION</h2>
 *
 * <h3>Always Hold (Structurally Invariant)</h3>
 * <ul>
 * <li>Factory - Component creation is universal</li>
 * <li>Chain of Responsibility - Processing pipelines</li>
 * <li>Interpreter - Language processing</li>
 * <li>Flyweight - Memory optimization</li>
 * <li>Composite - Tree structures</li>
 * </ul>
 *
 * <h3>Hold, but Transform</h3>
 * <ul>
 * <li>Singleton -> Scoped Authority (context-bound)</li>
 * <li>Builder -> Execution Plans (compile-time)</li>
 * <li>Decorator -> Function composition via ILaw.then()</li>
 * </ul>
 *
 * <h3>Alive and Essential</h3>
 * <ul>
 * <li>Memento - Human-time optimization (essential)</li>
 * </ul>
 *
 * <h3>Absorbed (No Longer Visible)</h3>
 * <ul>
 * <li>Proxy -> compile-time wrappers</li>
 * <li>Observer -> IEventBus + contexts</li>
 * <li>State -> context-managed transitions</li>
 * <li>Command -> execution plans</li>
 * </ul>
 *
 * <h2>WHY PATTERNS SURVIVE</h2>
 * <blockquote>
 * "If an annotation does not mark an execution entry point,
 * it will not survive long-term."
 * <br><br>
 * What SURVIVED:
 * - @Listener -> "When X happens, run Y" (event boundary)
 * - @Job -> "Run this under policy" (time/policy boundary)
 *
 * These map to REAL INVARIANTS.
 * </blockquote>
 *
 * <h2>ANNOTATION SURVIVAL RULE</h2>
 * <pre>
 * Annotations that survive:
 * - @Listener
 * - @Job / @Scheduled
 * - @Cancellable
 * - @Observable
 * - @Bounded
 * - @SingleWriter
 * - @AIGenerated
 *
 * All others are candidates for elimination.
 * </pre>
 *
 * <h2>WHAT DIDN'T SURVIVE (Historical Lessons)</h2>
 * <pre>
 * OSGi was elegant but died:
 * - modules as real classloader isolation
 * - explicit lifecycles
 * - dynamic services
 * - versioning
 * - hard boundaries
 *
 * But cognitive overhead was too high.
 *
 * CDI had the words but lacked force:
 * - contexts were passive, static, non-executing
 *
 * Spring doubled down on runtime reflection:
 * - worked socially, aged badly technically
 * </pre>
 *
 * <h2>THE KEY INSIGHT</h2>
 * <blockquote>
 * "FOP is the spine. Everything else is musculature."
 * <br><br>
 * "LLMs should decide WHAT, tools should do HOW."
 * <br><br>
 * "Patterns are absorbed, not exposed."
 * </blockquote>
 *
 * @since Grand Java Patterns Proof - Pattern Taxonomy Section
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/spec/PatternTaxonomy.java}
 * blob {@code f73f289bf3ae7c0605995f4ad4fdc5976efeb46f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PatternTaxonomy {

 /** Pattern survival categories. */
 public enum Survival {
 /** Always hold - structurally invariant */
 ALWAYS_HOLD,
 /** Hold but transform - adapted form */
 HOLD_TRANSFORM,
 /** Alive and essential - critical pattern */
 ALIVE_ESSENTIAL,
 /** Absorbed - no longer visible */
 ABSORBED
 }

 /** Target layers for pattern placement. */
 public enum TargetLayer {
 /** Interface-Oriented Programming layer */
 IOP,
 /** Annotation-Oriented Programming layer */
 ANOP,
 /** Context-Oriented Programming layer */
 COP,
 /** Aspect-Oriented Programming layer */
 AOP,
 /** Object-Oriented Programming layer */
 OOP,
 /** Compile-time layer */
 COMPILE
 }

 private PatternTaxonomy() {
 // Specification only - not instantiable
 }
}
