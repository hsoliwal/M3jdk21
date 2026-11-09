// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package org.jetbrains.kotlin.psi;

/** API-shape fixture; text and ranges are supplied explicitly, never parsed. */
public final class KtFile {
    private final String text;
    private final Object[] children;
    public KtFile(String text, Object... children) {
        this.text = text;
        this.children = children;
    }
    public String getText() { return text; }
    public Range getTextRange() { return new Range(0, text.length()); }
    public Object[] getChildren() { return children; }
    public record Range(int startOffset, int endOffset) {
        public int getStartOffset() { return startOffset; }
        public int getEndOffset() { return endOffset; }
    }
}
