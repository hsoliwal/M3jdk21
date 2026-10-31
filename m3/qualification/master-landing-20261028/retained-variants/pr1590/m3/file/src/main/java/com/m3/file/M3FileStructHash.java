// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/**
 * Char layer that yields a hash of a structured view of the file (an M3AST, a JSON shape, a manifest)
 * once the main stream ends. Supplied per file by {@link M3FileChangeProvider#structure(M3File)};
 * {@code m3/file} has no parser dependency.
 */
public interface M3FileStructHash extends M3FileCharShadow {
    /** Hash of the structured view; valid after {@link #finish()}. */
    long structHash();
}
