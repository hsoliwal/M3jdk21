// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Explicit native-required Atom for the existing Unix4j/M3 DAG. Only these
 * four fixed-contract variants may use the JNI kernel; upstream Unix4j
 * Command<?> and its regex/options/side effects retain their original owner.
 */
public final class NativeTextAtom implements Atom {
    private enum Operation { GREP_LITERAL, UNIQ, HEAD, TAIL }

    private final Operation operation;
    private final String literal;
    private final int count;
    private final Limits limits;

    private NativeTextAtom(Operation operation, String literal, int count, Limits limits) {
        this.operation = operation;
        this.literal = literal;
        this.count = count;
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public static NativeTextAtom grepLiteral(String literal, Limits limits) {
        Canonical.validUnicode(literal);
        return new NativeTextAtom(Operation.GREP_LITERAL, literal, 0, limits);
    }

    public static NativeTextAtom uniq(Limits limits) {
        return new NativeTextAtom(Operation.UNIQ, "", 0, limits);
    }

    public static NativeTextAtom head(int count, Limits limits) {
        if (count < 0) throw new IllegalArgumentException("negative head count");
        return new NativeTextAtom(Operation.HEAD, "", count, limits);
    }

    public static NativeTextAtom tail(int count, Limits limits) {
        if (count < 0) throw new IllegalArgumentException("negative tail count");
        return new NativeTextAtom(Operation.TAIL, "", count, limits);
    }

    @Override
    public void execute(TextFrame input, Consumer<TextLine> output) {
        Objects.requireNonNull(output, "output");
        M3NativeTextWindow nativeWindow = M3NativeTextWindow.from(input, limits);
        M3NativeTextWindow selected = switch (operation) {
            case GREP_LITERAL -> nativeWindow.grepLiteral(literal);
            case UNIQ -> nativeWindow.uniqAdjacent();
            case HEAD -> nativeWindow.head(count);
            case TAIL -> nativeWindow.tail(count);
        };
        selected.frame().lines().forEach(output);
    }
}
