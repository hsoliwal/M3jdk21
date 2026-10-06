/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

/** Immutable code-unit view. Conversion to String is an explicit allocating boundary. */
public sealed interface M3StringPiece extends CharSequence
        permits LocalM3StringPiece, JoinedM3StringPiece {
    @Override M3StringPiece subSequence(int start, int end);

    default String flatten() {
        char[] result = new char[length()];
        for (int i = 0; i < result.length; i++) result[i] = charAt(i);
        return new String(result);
    }

    /** Joins immutable leaf ranges without copying their character bytes. */
    static M3StringPiece join(M3StringPiece... pieces) {
        return new JoinedM3StringPiece(pieces);
    }
}
