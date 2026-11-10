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
 * IAIGenMethod - Marks a method for AI-assisted code generation.
 *
 * <p>
 * From IOP (Interface-Oriented Programming):
 *
 * <pre>
 *
 * &#64;AIGenMethod("""
 * 		Validate input.
 * 		Normalize text.
 * 		Dispatch to intent extractor.
 * 		Idempotent. Non-blocking.
 * 		""")
 * void ingest(TextInput input);
 * </pre>
 *
 * <p>
 * This annotation provides method-level context for AI generation:
 * <ul>
 * <li>Pre-conditions (what must be true before)</li>
 * <li>Post-conditions (what must be true after)</li>
 * <li>Behavior constraints (idempotent, non-blocking, etc.)</li>
 * </ul>
 *
 * <h2>Key Insight</h2> <blockquote> When code is the instruction and code is
 * the document, AI does not need memory - it needs literacy. </blockquote>
 *
 * @since IOP 1.0 - AI Generation
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares a method generation specification with idempotence, non-blocking and determinism requirements; the annotation supplies context and proves none of those properties.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIGenMethod.java}
 * blob {@code 9ffbd268b254b5f1e2de728b48343fb74326bdcf} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIGenMethod {

    /**
     * Method specification for AI generation.
     *
     * <p>
     * Should include:
     * <ul>
     * <li>What the method does</li>
     * <li>Behavior constraints</li>
     * <li>Edge cases</li>
     * </ul>
     *
     * @return specification text
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenMethod#value intent: Method specification for AI generation. <p> Should include: <ul> <li>What the method does</li> <li>Behavior constraints</li> <li>Edge cases</li> </ul> Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String value();

    /**
     * Whether this method must be idempotent.
     *
     * @return true if idempotent
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenMethod#idempotent intent: Whether this method must be idempotent. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean idempotent() default false;

    /**
     * Whether this method must be non-blocking.
     *
     * @return true if non-blocking
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenMethod#nonBlocking intent: Whether this method must be non-blocking. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean nonBlocking() default false;

    /**
     * Whether this method is deterministic.
     *
     * @return true if deterministic
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenMethod#deterministic intent: Whether this method is deterministic. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean deterministic() default true;
}
