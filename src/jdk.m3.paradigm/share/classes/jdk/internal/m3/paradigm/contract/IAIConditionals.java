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
 * Container for multiple {@link IAIConditional} annotations.
 *
 * @since IOP 1.0
 * @see IAIConditional
 * @m3.pattern spring:conditional
 * @m3.algorithm Holds repeated IAIConditional declarations at one source site; this container combines no predicates and evaluates no activation condition.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD}; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIConditionals.java}
 * blob {@code 4fdad05692bd50bdf4c0e46d1b916b74161ee4c7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIConditionals {

    /**
     * Array of conditionals.
     *
     * @return conditionals array
     * @m3.pattern spring:conditional
     * @m3.algorithm Carries the required array of repeated activation-condition annotations; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures IAIConditional[] annotation element; explicit value required, with no default.
     */
    IAIConditional[] value();
}
