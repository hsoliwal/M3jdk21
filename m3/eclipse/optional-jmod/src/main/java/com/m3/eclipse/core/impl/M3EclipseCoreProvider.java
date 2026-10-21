// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.core.impl;

import com.m3.arrays.M3Arrays;
import com.m3.arrays.M3Utf16ArrayView;
import com.m3.ast.M3ASTHierarchy;
import com.m3.collections.M3LongIndexProjection;
import com.m3.eclipse.core.M3EclipseCore;
import com.m3.file.M3File;
import com.m3.text.LocalM3Arena;
import com.m3.text.M3StringPiece;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

/** No parallel index, text, file, AST or native operator implementation. */
public final class M3EclipseCoreProvider implements M3EclipseCore {
    private final LocalM3Arena copyArena = new LocalM3Arena();

    public M3EclipseCoreProvider() { }

    @Override public M3StringPiece retainText(M3StringPiece existing) {
        return Objects.requireNonNull(existing, "existing");
    }

    @Override public M3StringPiece copyUtf16(CharSequence source) {
        CharSequence checked = Objects.requireNonNull(source, "source");
        char[] codeUnits = new char[checked.length()];
        for (int i = 0; i < codeUnits.length; i++) codeUnits[i] = checked.charAt(i);
        return copyArena.copyUtf16(codeUnits);
    }

    @Override public M3Utf16ArrayView utf16(CharSequence source) {
        return M3Arrays.chars(Objects.requireNonNull(source, "source"));
    }

    @Override public M3LongIndexProjection index(long[] source) {
        return M3LongIndexProjection.snapshot(Objects.requireNonNull(source, "source"));
    }

    @Override public M3ASTHierarchy hierarchy(
            String repository, Collection<M3ASTHierarchy.FileFact> facts) {
        return M3ASTHierarchy.build(repository, facts);
    }

    @Override public M3File file(Path path) {
        return M3File.of(path);
    }
}
