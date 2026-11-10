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
 * IopBoundary - Declares the visibility boundary of a behavioral contract.
 *
 * <h2>BOUNDARY LEVELS</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  LEVEL      │ MEANING                   │ WHO CAN USE                     ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  PUBLIC     │ Part of public API        │ Anyone                          ║
 * ║  MODULE     │ Module-internal           │ Same module only                ║
 * ║  PACKAGE    │ Package-private           │ Same package only               ║
 * ║  INTERNAL   │ Implementation detail     │ Same class/inner only           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Example</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;IopBehavioral
 * 	&#64;IopBoundary(Visibility.PUBLIC)
 * 	public interface IUserService {
 *
 * 		User findById(String id);
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - Visibility Boundary
 * @see IopBehavioral
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the declared visibility boundary of a behavioral contract. The annotation records metadata and does not itself perform enforcement or generation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}, preserving the declared element and nested-enum schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IopBoundary.java}
 * blob {@code 27067d8a38289b023191cd2e27758cf60ecbf10f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IopBoundary {

    /**
     * The visibility boundary.
     *
     * @return visibility level
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IopBoundary#value metadata for The visibility boundary. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Visibility}, with exact default {@code Visibility.PUBLIC}.
     */
    Visibility value() default Visibility.PUBLIC;

    /** Visibility levels.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 4 symbolic choices for contract visibility boundary; interpretation belongs to consuming tooling.
     * @m3.dataStructures Nested IopBoundary.Visibility enum with the existing 4 constants in declaration order.
     */
    enum Visibility {
        /** Public API - anyone can use.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IopBoundary.Visibility#PUBLIC choice for contract visibility boundary: Public API - anyone can use. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IopBoundary.Visibility, preserving its declared position and identity.
         */
        PUBLIC,

        /** Module-internal - same module only.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IopBoundary.Visibility#MODULE choice for contract visibility boundary: Module-internal - same module only. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IopBoundary.Visibility, preserving its declared position and identity.
         */
        MODULE,

        /** Package-private - same package only.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IopBoundary.Visibility#PACKAGE choice for contract visibility boundary: Package-private - same package only. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IopBoundary.Visibility, preserving its declared position and identity.
         */
        PACKAGE,

        /** Internal implementation detail.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IopBoundary.Visibility#INTERNAL choice for contract visibility boundary: Internal implementation detail. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IopBoundary.Visibility, preserving its declared position and identity.
         */
        INTERNAL
    }
}
