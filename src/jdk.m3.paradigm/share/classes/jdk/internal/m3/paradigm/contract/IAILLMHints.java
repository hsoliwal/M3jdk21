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
 * Container for multiple {@link IAILLMHint} annotations.
 *
 * @since IOP 1.0
 * @see IAILLMHint
 * @m3.pattern synexia:aiop
 * @m3.algorithm Carries repeated IAILLMHint records for LLM attention, trigger, context and prompting preferences in the separate IAI-prefixed API; the container performs no evaluation, planning or execution.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PACKAGE}; preserves its declared element and nested-token schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAILLMHints.java}
 * blob {@code bc2137b9f7ac421df55ad4975d0a5cebc0d24099} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAILLMHints {

    /**
     * Array of LLM hint annotations.
     *
     * @return hints array
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILLMHints#value advisory metadata for Array of LLM hint annotations. The element itself neither executes nor enforces that request.
     * @m3.dataStructures IAILLMHint[] annotation element; explicit value required, no default.
     */
    IAILLMHint[] value();
}
