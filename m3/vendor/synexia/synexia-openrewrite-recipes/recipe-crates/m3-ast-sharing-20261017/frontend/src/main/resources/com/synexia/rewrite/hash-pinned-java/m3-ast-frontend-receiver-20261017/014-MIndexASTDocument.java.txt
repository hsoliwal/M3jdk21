// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.compiler.ast;

import com.synexia.mindex.ast.MIndexAST;
import com.synexia.mindex.experiment.Digests;
import java.util.List;
import java.util.Objects;

/** Lossless source custody and occurrence index; neither participates in immutable atom identity. */
public record MIndexASTDocument(String sourceName, String source, String sourceUtf16Sha256,
        MIndexAST root, List<MIndexASTOccurrence> occurrences) {
    public MIndexASTDocument {
        Objects.requireNonNull(sourceName, "sourceName");
        Objects.requireNonNull(source, "source");
        Digests.requireSha256(sourceUtf16Sha256);
        Objects.requireNonNull(root, "root");
        occurrences = List.copyOf(occurrences);
        for (MIndexASTOccurrence occurrence : occurrences) {
            if (occurrence.end() > source.length()) { throw new IllegalArgumentException("span exceeds source"); }
        }
    }
    public String reconstruct() { return source; }
    public String text(MIndexASTOccurrence occurrence) {
        if (!occurrences.contains(occurrence)) { throw new IllegalArgumentException("foreign occurrence"); }
        if (!occurrence.hasSourceSpan()) { throw new IllegalStateException("compiler supplied no source span"); }
        return source.substring(occurrence.start(), occurrence.end());
    }
}
