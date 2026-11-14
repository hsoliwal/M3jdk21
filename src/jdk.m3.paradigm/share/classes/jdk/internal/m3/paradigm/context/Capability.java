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

import java.util.Objects;

/**
 * Shared capability bits; enum instances are JVM singletons, never per-context state.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/capability/ContextCapability.java}
 * blob {@code 2ad6f6511f2aa5792da26312e5427df819bf3e37} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum Capability {
    PROMPT(0),
    SUMMARIZE(1),
    WRITE(2),
    REWRITE(3),
    PROOFREAD(4),
    TRANSLATE(5),
    LANGUAGE_DETECT(6),
    TEXT_INPUT(7),
    IMAGE_INPUT(8),
    AUDIO_INPUT(9),
    STREAM(10),
    TEMPERATURE(11),
    TOP_K(12),
    CONTEXT_TOKENS(13),
    MAX_OUTPUT_TOKENS(14),
    STATELESS_MESSAGES(15),
    LOCAL_MEDIA_ONLY(16),
    ON_DEVICE(17),
    SSE(18),
    MODEL_DISCOVERY(19),
    TOOL_CALLS(20),
    STRUCTURED_OUTPUT(21),
    EMBEDDINGS(22);

    private final int mask;

    Capability(int bit) {
        mask = 1 << bit;
    }

    public int mask() {
        return mask;
    }

    public static int mask(Capability... capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        int result = 0;
        for (Capability capability : capabilities) {
            result |= Objects.requireNonNull(capability, "capability").mask;
        }
        return result;
    }

    public static boolean containsAll(int available, int required) {
        return (available & required) == required;
    }
}
