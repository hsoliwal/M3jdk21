// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;

/** A configured byte limit was exceeded while reading or writing; classified as {@link M3FileError#LIMIT}. */
public final class M3FileLimitException extends IOException {
    private static final long serialVersionUID = 1L;

    public M3FileLimitException(String message) {
        super(message);
    }
}
