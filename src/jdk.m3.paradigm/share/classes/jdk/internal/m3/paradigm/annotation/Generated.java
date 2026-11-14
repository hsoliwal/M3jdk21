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
 * Generated - Marks code as machine-generated.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Standard marker for generated code provenance. Distinguishes hand-written
 * from generated artifacts for code review and tooling.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;Generated("anop-llm")
 * 	final class MemoryIngestion_Impl implements MemoryIngestion {
 *
 * 		&#64;Override
 * 		public void ingest(TextInput input) {
 * 			// generated deterministic code
 * 			// no AI here
 * 		}
 * 	}
 *
 * 	&#64;Generated("anop-llm")
 * 	final class MemoryIngestion_ExecutionPlan {
 *
 * 		static final ExecutionPlan PLAN = ExecutionPlan.builder().singleWriter().observable().cancellable()
 * 				.listener().build();
 * 	}
 * }
 * </pre>
 *
 * <h2>Relationship to @AIGenerated</h2>
 * <ul>
 * <li>@Generated - Standard marker for any generated code</li>
 * <li>@AIGenerated - Specific marker for AI-generated code with intent</li>
 * </ul>
 *
 * <h2>Hard Rule</h2> <blockquote> Any file marked @Generated is disposable. Any
 * file not marked is authoritative. </blockquote>
 *
 * @since ContextOS 1.0 - AnOP Provenance
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Generated.java}
 * blob {@code 720f379bc08634a95776da4901e0445babfdec8f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Generated {

    /**
     * Generator tool name.
     *
     * @return tool identifier
     */
    String value();

    /**
     * Generation timestamp (ISO 8601).
     *
     * @return when generated
     */
    String date() default "";

    /**
     * Optional comments about generation.
     *
     * @return additional info
     */
    String comments() default "";
}
