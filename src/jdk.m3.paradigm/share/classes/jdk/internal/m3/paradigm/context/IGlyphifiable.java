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
 * Contract for types that can produce glyph streams.
 *
 * <p>From ContextOS: "Everything can compress to a symbol."
 *
 * <h2>Unix Model</h2>
 * <ul>
 *   <li>glyphs() - stream of glyphs</li>
 *   <li>glyphify() - single glyph tuple</li>
 *   <li>pipe-compatible via stream output</li>
 * </ul>
 *
 * @since ContextOS 1.0
 */
@ISpec(
    value = "Contract for glyph streams - Unix pipe compatible",
    constraints = {
        "glyphs() produces stream of glyph tuples",
        "glyphify() produces single glyph tuple",
        "Minimal token footprint for LLM reasoning"
    },
    invariants = {
        "glyphs() returns non-null stream",
        "glyphify() returns non-null GlyphTuple"
    },
    examples = {
        "context.glyphs().map(IGlyph.GlyphTuple::toSymbols)",
        "context.glyphify().toSymbols()"
    },
    model = Model.ModelType.HAIKU,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/IGlyphifiable.java}
 * blob {@code 13100dcb9e53b6c8070befe58bd4341a0848d8f1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IGlyphifiable {

    /**
     * Produce a stream of glyph tuples.
     *
     * @return Stream of glyphs
     */
    default Stream<IGlyph.GlyphTuple> glyphs() {
        return Stream.of(glyphify());
    }

    /**
     * Produce a glyph tuple representing this context's semantic state.
     *
     * @return glyph tuple (defaults to neutral)
     */
    default IGlyph.GlyphTuple glyphify() {
        return IGlyph.GlyphTuple.neutral();
    }
}
