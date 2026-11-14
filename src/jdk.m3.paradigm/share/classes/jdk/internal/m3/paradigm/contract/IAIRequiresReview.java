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
 * AIRequiresReview - IOP annotation requiring manual review before applying
 * changes.
 *
 * <h2>IOP Authority: Review Gate</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Agent may generate a plan.                                               ║
 * ║  Agent may generate a JDT Change.                                         ║
 * ║  Agent may NOT apply it automatically.                                    ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Use Cases</h2>
 * <ul>
 * <li>Public APIs - external contract changes need review</li>
 * <li>Security-sensitive code - auth, crypto, permissions</li>
 * <li>Core infrastructure - foundational code</li>
 * <li>Database schemas - migration impact</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;ISpec(value = "Authentication service")
 * 	&#64;AIRequiresReview // No auto-apply, human must approve
 * 	public interface IAuthService&lt;Self extends IAuthService&lt;Self>> extends IContext&lt;Self> {
 *
 * 		AuthResult authenticate(String user, String password);
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - Authority Layer
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares a manual-review requirement and its rationale before applying changes. The annotation records metadata and does not itself perform enforcement or generation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}, preserving the declared element and nested-enum schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIRequiresReview.java}
 * blob {@code 6d7b6eee786323af98fb61e12c9a553701564dfb} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIRequiresReview {

    /**
     * Reason for requiring review (documentation).
     *
     * @return reason string
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IAIRequiresReview#reason metadata for Reason for requiring review (documentation). Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String reason() default "";
}
