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
 * IAIScope - IOP annotation declaring the blast radius of changes.
 *
 * <h2>IOP Authority: Scope Control</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Agent must not exceed declared scope.                                    ║
 * ║  Scope violations are immediate rejection.                                ║
 * ║  No "helpful" widening.                                                   ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @ISpec(value = "User service implementation")
 * @IAIScope(Scope.PACKAGE) // Changes limited to this package
 * public interface IUserService<Self extends IUserService<Self>> extends IContext<Self> {
 *
 * 	User getUser(String id);
 * }
 * }</pre>
 *
 * @since IOP 1.0 - Authority Layer
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the maximum allowed extent of source changes: local type, package, module or project. The annotation records metadata and does not itself perform enforcement or generation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}, preserving the declared element and nested-enum schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIScope.java}
 * blob {@code 9e17bdfa35de63ee963eb641cfc52998be46fc4d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIScope {

    /**
     * Maximum allowed scope of changes.
     *
     * @return scope limit
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IAIScope#value metadata for Maximum allowed scope of changes. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Scope}, requiring an explicit value with no default.
     */
    Scope value();

    /** Scope levels for change blast radius.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 4 symbolic choices for allowed source-change extent; interpretation belongs to consuming tooling.
     * @m3.dataStructures Nested IAIScope.Scope enum with the existing 4 constants in declaration order.
     */
    enum Scope {
        /** Same type only - most restrictive.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIScope.Scope#LOCAL choice for allowed source-change extent: Same type only - most restrictive. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIScope.Scope, preserving its declared position and identity.
         */
        LOCAL,

        /** Same package - moderate.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIScope.Scope#PACKAGE choice for allowed source-change extent: Same package - moderate. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIScope.Scope, preserving its declared position and identity.
         */
        PACKAGE,

        /** Same module - broader.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIScope.Scope#MODULE choice for allowed source-change extent: Same module - broader. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIScope.Scope, preserving its declared position and identity.
         */
        MODULE,

        /** Entire project - least restrictive.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIScope.Scope#PROJECT choice for allowed source-change extent: Entire project - least restrictive. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIScope.Scope, preserving its declared position and identity.
         */
        PROJECT
    }
}
