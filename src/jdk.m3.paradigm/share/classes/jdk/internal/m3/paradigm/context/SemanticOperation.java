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
package jdk.internal.m3.paradigm.context;

/**
 * Semantic operations can be native or honestly marked synthesized from PROMPT.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/capability/ContextSemanticOperation.java}
 * blob {@code 6d5d905322f611600b8dbb533853976a08de64bf} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum SemanticOperation {
    PROMPT(Capability.PROMPT),
    SUMMARIZE(Capability.SUMMARIZE),
    WRITE(Capability.WRITE),
    REWRITE(Capability.REWRITE),
    PROOFREAD(Capability.PROOFREAD),
    TRANSLATE(Capability.TRANSLATE),
    LANGUAGE_DETECT(Capability.LANGUAGE_DETECT);

    private final Capability capability;

    SemanticOperation(Capability capability) {
        this.capability = capability;
    }

    public Capability capability() {
        return capability;
    }
}
