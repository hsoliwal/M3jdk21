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
 * Container for multiple {@link IAIConstant} annotations.
 *
 * @since IOP 1.0
 * @see IAIConstant
 * @m3.pattern synexia:anop
 * @m3.algorithm Declares the array of repeated IAIConstant generation-hint records. The annotation records metadata and does not itself perform enforcement or generation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}, preserving the declared element and nested-enum schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIConstants.java}
 * blob {@code 5e4261477aacd881ede1534cd198dc4d1fc91117} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIConstants {

    /**
     * Array of constant hints.
     *
     * @return constants array
     * @m3.pattern synexia:anop
     * @m3.algorithm Declares IAIConstants#value metadata for Array of constant hints. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code IAIConstant[]}, requiring an explicit value with no default.
     */
    IAIConstant[] value();
}
