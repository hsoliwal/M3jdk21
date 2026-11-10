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

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.util.stream.Stream;

/**
 * Contract for types that can produce text streams.
 *
 * <p>From ContextOS: "Everything can express itself as text."
 *
 * <h2>Unix Model</h2>
 * <ul>
 *   <li>lines() - stream of lines (like cat)</li>
 *   <li>stringify() - single string output</li>
 *   <li>pipe-compatible via stream output</li>
 * </ul>
 *
 * @since ContextOS 1.0
 */
@ISpec(
    value = "Contract for text streams - Unix pipe compatible",
    constraints = {
        "lines() produces stream of text lines",
        "stringify() produces single structured string",
        "Different from toString() which is for debugging"
    },
    invariants = {
        "lines() returns non-null stream",
        "stringify() returns non-null string"
    },
    examples = {
        "context.lines().forEach(System.out::println)",
        "context.stringify()"
    },
    model = Model.ModelType.HAIKU,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/IStringifiable.java}
 * blob {@code 773a6d35661b3434ff1f523042832899f4b4faaa} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IStringifiable {

    /**
     * Produce a stream of text lines.
     *
     * <p>Like Unix cat - outputs text as stream of lines.
     *
     * @return Stream of lines
     */
    default Stream<String> lines() {
        return Stream.of(stringify());
    }

    /**
     * Produce a structured string representation.
     *
     * <p>Unlike {@code toString()} which is for debugging,
     * this produces a semantic, structured representation.
     *
     * @return structured string representation
     */
    default String stringify() {
        return toString();
    }
}
