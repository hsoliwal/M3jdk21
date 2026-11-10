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
 * Container for multiple {@link IExceptionSpec} annotations.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;IExceptionSpec(handle = IOException.class, action = LOG_FALLBACK, fallback = "[]")
 * @IExceptionSpec(handle = SQLException.class, action = TRANSLATE, translate = @IExceptionSpec.Translation(from = SQLException.class, to = DataAccessException.class))
 * List<User> loadUsers();
 * }
 * </pre>
 *
 * @since IOP 1.0 - Exception Handling
 * @see IExceptionSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Groups repeated exception-handling specifications at a method; it establishes no ordering or conflict-resolution semantics beyond the supplied array.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IExceptionSpecs.java}
 * blob {@code 5e2efdd02def4f199aee31af2f67ee5c57015373} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IExceptionSpecs {

    /**
     * Array of exception specifications.
     *
     * @return exception specs
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpecs#value intent: Array of exception specifications. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code IExceptionSpec[]}; an explicit value is required.
     */
    IExceptionSpec[] value();
}
