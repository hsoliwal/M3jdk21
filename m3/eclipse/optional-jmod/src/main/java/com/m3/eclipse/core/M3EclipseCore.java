// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.core;

import com.m3.arrays.M3Utf16ArrayView;
import com.m3.ast.M3ASTHierarchy;
import com.m3.collections.M3LongIndexProjection;
import com.m3.file.M3File;
import com.m3.text.M3StringPiece;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Explicit capability bridge for Eclipse integrations that retain their public
 * org.eclipse.* interfaces. No workbench, native widget, file owner, canonical
 * text interner or AST parser is reimplemented here. This JPMS service is
 * optional; the core JDK and Eclipse do not require it for startup.
 */
public interface M3EclipseCore {
    /** Keep original M3 immutable text descriptor and its backing identity. */
    M3StringPiece retainText(M3StringPiece existing);

    /** Explicit stock-String-to-M3 copied edge, NOT a VM-global string intern. */
    M3StringPiece copyUtf16(CharSequence source);

    /** Snapshot mutable caller text through the established M3 arrays owner. */
    M3Utf16ArrayView utf16(CharSequence source);

    /** Own only immutable primitive index coordinates in the existing M3 class. */
    M3LongIndexProjection index(long[] source);

    /** Use the established M3 L0-L5 hierarchy, not another Eclipse AST store. */
    M3ASTHierarchy hierarchy(String repository, Collection<M3ASTHierarchy.FileFact> facts);

    /** Wrap original provider-owned Path, with no new resource-tree state. */
    M3File file(Path path);
}
