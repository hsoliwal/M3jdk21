// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/** Collects decoded chars; for small files and tests. */
public final class M3FileTextShadow implements M3FileCharShadow {
    private final StringBuilder text = new StringBuilder();

    M3FileTextShadow() {
    }

    @Override
    public void update(char[] chars, int offset, int length) {
        text.append(chars, offset, length);
    }

    public String text() {
        return text.toString();
    }
}
