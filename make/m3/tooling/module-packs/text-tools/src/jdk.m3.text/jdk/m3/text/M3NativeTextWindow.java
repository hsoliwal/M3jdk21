// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import jdk.m3.text.NativeLineBatch;
import java.util.AbstractList;
import java.util.List;
import java.util.Objects;

/**
 * Contract-preserving integration of the native packed-line selector with the
 * existing M3 TextFrame. Original M3 String owners and CR/LF/CRLF endings stay
 * in TextFrame; the JNI selector handles only UTF-8 content plus primitive masks.
 */
public final class M3NativeTextWindow {
    private final TextFrame source;
    private final Limits limits;
    private final NativeLineBatch rows;

    private M3NativeTextWindow(TextFrame source, Limits limits, NativeLineBatch rows) {
        this.source = source;
        this.limits = limits;
        this.rows = rows;
    }

    public static M3NativeTextWindow from(TextFrame source, Limits limits) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(limits, "limits").check(source);
        // One immutable indexed view; no O(rows) intermediate List<String> copy.
        List<TextLine> sourceLines = source.lines();
        List<String> contents = new AbstractList<>() {
            @Override public int size() { return sourceLines.size(); }
            @Override public String get(int index) { return sourceLines.get(index).content(); }
        };
        return new M3NativeTextWindow(source, limits, NativeLineBatch.fromUtf8Lines(contents));
    }

    public M3NativeTextWindow grepLiteral(String pattern) {
        Canonical.validUnicode(pattern);
        return new M3NativeTextWindow(source, limits, rows.grepLiteral(pattern));
    }

    public M3NativeTextWindow uniqAdjacent() {
        return new M3NativeTextWindow(source, limits, rows.uniqAdjacent());
    }

    public M3NativeTextWindow head(int count) {
        return new M3NativeTextWindow(source, limits, rows.head(count));
    }

    public M3NativeTextWindow tail(int count) {
        return new M3NativeTextWindow(source, limits, rows.tail(count));
    }

    public int selectedCount() { return rows.selectedCount(); }

    public TextFrame frame() {
        FrameCollector result = new FrameCollector(limits);
        rows.forEachSelectedIndex(i -> result.accept(source.lines().get(i)));
        return result.frame();
    }
}
