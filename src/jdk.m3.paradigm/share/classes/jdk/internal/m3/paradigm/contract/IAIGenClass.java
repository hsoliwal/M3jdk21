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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IAIGenClass - Marks an interface for AI-assisted code generation.
 *
 * <p>
 * From IOP (Interface-Oriented Programming):
 *
 * <pre>
 *   In IOP, the program IS the contract.
 *   Code becomes executable documentation.
 *   AI no longer needs long conversational memory.
 *
 *   The annotation is the PROMPT, frozen in code.
 * </pre>
 *
 * <p>
 * This annotation does three things simultaneously:
 * <ul>
 * <li>Human-readable spec</li>
 * <li>Compile-time authority</li>
 * <li>AI-local context</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @AIGenClass("""
 * 		Ingest user text.
 * 		Extract intent and emotion.
 * 		Persist memory.
 * 		Emit events.
 * 		Must be deterministic and cancellable.
 * 		""")
 * public interface MemoryIngestor {
 * 	// methods...
 * }
 * }</pre>
 *
 * <h2>Why this works</h2>
 * <ul>
 * <li>AI reads the current contract</li>
 * <li>AI reads the current generated code</li>
 * <li>AI reads the failing test or diff</li>
 * <li>AI produces a localized fix</li>
 * </ul>
 *
 * <p>
 * This turns AI from "conversational partner" into "stateless document editor
 * with rules".
 *
 * @since IOP 1.0 - AI Generation
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares a type-level generation specification, category tags and regeneration preference; it invokes no model, enforces no contract and overwrites no source.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIGenClass.java}
 * blob {@code b88ae45238361a4f2378f69f47f74e93d468e49f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIGenClass {

    /**
     * JavaDoc-style specification for the class. This becomes the AI's instruction
     * context.
     *
     * <p>
     * Should include:
     * <ul>
     * <li>Responsibilities (what it does)</li>
     * <li>Constraints (what it must not do)</li>
     * <li>Invariants (what must always hold)</li>
     * </ul>
     *
     * @return specification text
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenClass#value intent: JavaDoc-style specification for the class. This becomes the AI's instruction context. <p> Should include: <ul> <li>Responsibilities (what it does)</li> <li>Constraints (what it must not do)</li> <li>Invariants (what must always hold)</li> </ul> Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String value();

    /**
     * Optional tags for categorization.
     *
     * @return category tags
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenClass#tags intent: Optional tags for categorization. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] tags() default {};

    /**
     * Whether generated code can be overwritten. Default true - generated code is
     * cache, not treasure.
     *
     * @return true if regeneration is allowed
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenClass#regenerable intent: Whether generated code can be overwritten. Default true - generated code is cache, not treasure. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean regenerable() default true;
}
