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

import jdk.internal.m3.collections.PlaceholderLogger;

import java.util.stream.Stream;

/**
 * Template Method base for {@link IGlyphifiable}.
 *
 * <p>
 * Delegates glyph production to {@code doGlyphify()} with lifecycle hooks.
 *
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/AbstractGlyphifiable.java}
 * blob {@code d4f12bdae098ea81e2a0aac3c39dc0a07e837608} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractGlyphifiable implements IGlyphifiable {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractGlyphifiable.class);

    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public AbstractGlyphifiable() {
    }


    // --- TEMPLATE METHODS ---

    @Override
    public Stream<IGlyph.GlyphTuple> glyphs() {
        onBeforeGlyphs();
        Stream<IGlyph.GlyphTuple> result = doGlyphs();
        onAfterGlyphs(result);
        return result;
    }

    @Override
    public IGlyph.GlyphTuple glyphify() {
        onBeforeGlyphify();
        IGlyph.GlyphTuple result = doGlyphify();
        onAfterGlyphify(result);
        return result;
    }

    // --- ABSTRACT doXxx() - SUBCLASS PROVIDES ---

    protected abstract Stream<IGlyph.GlyphTuple> doGlyphs();

    protected abstract IGlyph.GlyphTuple doGlyphify();

    // --- CALLBACK HOOKS - onXxx() ---

    protected void onBeforeGlyphs() {}

    protected void onAfterGlyphs(Stream<IGlyph.GlyphTuple> result) {}

    protected void onBeforeGlyphify() {}

    protected void onAfterGlyphify(IGlyph.GlyphTuple result) {}
}
