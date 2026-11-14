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
 * Container for multiple {@link IAITestHint} annotations.
 *
 * @since IOP 1.0
 * @see IAITestHint
 * @m3.pattern synexia:aiop
 * @m3.algorithm Carries repeated IAITestHint records for test-generation conventions, coverage targets, frameworks, fixtures, isolation and boundary-case guidance; the container performs no evaluation, planning or execution.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD}; preserves its declared element and nested-token schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAITestHints.java}
 * blob {@code 3838197c2367066a6b2c152be4bd66786f5d42a3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAITestHints {

    /**
     * Array of test hints.
     *
     * @return test hints array
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAITestHints#value advisory metadata for Array of test hints. The element itself neither executes nor enforces that request.
     * @m3.dataStructures IAITestHint[] annotation element; explicit value required, no default.
     */
    IAITestHint[] value();
}
