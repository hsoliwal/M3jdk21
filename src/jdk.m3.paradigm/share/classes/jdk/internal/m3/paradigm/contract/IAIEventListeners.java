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
 * Container for multiple {@link IAIEventListener} annotations.
 *
 * @since IOP 1.0
 * @see IAIEventListener
 * @m3.pattern synexia:iop
 * @m3.algorithm Stores the required annotation array used as the repeatable IAIEventListener container for method declarations.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code ElementType.METHOD}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIEventListeners.java}
 * blob {@code b955c816c8e9798dc7d444e774b88107c73bbd7a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIEventListeners {

    /**
     * Array of event listeners.
     *
     * @return listeners array
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares value metadata for Array of event listeners. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code IAIEventListener[]}, required explicitly with no default value.
     */
    IAIEventListener[] value();
}
