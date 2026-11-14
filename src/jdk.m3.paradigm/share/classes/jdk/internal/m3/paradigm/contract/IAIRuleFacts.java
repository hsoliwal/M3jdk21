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
 * Container for multiple {@link IAIRuleFact} annotations.
 *
 * @since IOP 1.0
 * @see IAIRuleFact
 * @m3.pattern synexia:iop
 * @m3.algorithm Stores the required annotation array used as the repeatable IAIRuleFact container for type, parameter and field declarations.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code {ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD}}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIRuleFacts.java}
 * blob {@code 380364e5c9135d72986ab4c6fcee93e316520ec3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIRuleFacts {

    /**
     * Array of rule fact hints.
     *
     * @return facts array
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares value metadata for Array of rule fact hints. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code IAIRuleFact[]}, required explicitly with no default value.
     */
    IAIRuleFact[] value();
}
