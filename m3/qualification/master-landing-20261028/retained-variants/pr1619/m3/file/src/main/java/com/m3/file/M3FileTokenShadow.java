// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/**
 * Token layer: words ({@code letterOrDigit}, {@code _}, {@code $}) and single non-whitespace symbols,
 * hashed as a token stream with FNV-1a 64 and a boundary marker outside the char range.
 *
 * <p>Whitespace only separates tokens, so re-indentation or re-wrapping keeps the hash while a moved
 * token boundary changes it: the structural hash lane for text, beneath an M3AST hash.</p>
 */
public final class M3FileTokenShadow implements M3FileCharShadow {
    private static final long OFFSET = 0xcbf29ce484222325L;
    private static final long PRIME = 0x100000001b3L;
    private static final long BOUNDARY = 0x10000L;

    private long hash = OFFSET;
    private long tokens;
    private boolean inWord;
    private boolean finished;

    M3FileTokenShadow() {
    }

    @Override
    public void update(char[] chars, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            char c = chars[i];
            if (Character.isLetterOrDigit(c) || c == '_' || c == '$') {
                if (!inWord) {
                    tokens++;
                    inWord = true;
                }
                mix(c);
            } else {
                endWord();
                if (!Character.isWhitespace(c)) {
                    tokens++;
                    mix(c);
                    mix(BOUNDARY);
                }
            }
        }
    }

    @Override
    public void finish() {
        endWord();
        finished = true;
    }

    /** Token-stream hash; complete after {@link #finish()}. */
    public long hash() {
        if (!finished) throw new IllegalStateException("main stream has not ended");
        return hash;
    }

    public long tokens() {
        return tokens;
    }

    private void endWord() {
        if (inWord) {
            mix(BOUNDARY);
            inWord = false;
        }
    }

    private void mix(long value) {
        hash = (hash ^ value) * PRIME;
    }
}
