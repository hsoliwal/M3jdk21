// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;
//FLLDCIM: RESTORE - source m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java/jdk/m3/file/M3FileStructHash.java blob 0c85ce2c64eef826d33cb6fa3994349757af4e4e (retained PR #1619 head; parent 0cd0944136c1 blob 3cc1caa47fa2aed979adb3053f072b5b8d40745c https://github.com/hsoliwal/M3jdk21/blob/0cd0944136c1/m3/file/src/main/java/com/m3/file/M3FileStructHash.java); recipe m3/file/qualification/retained-port-20261107 RESTORE_EXACT

/**
 * Char layer that yields a hash of a structured view of the file (an M3AST, a JSON shape, a manifest)
 * once the main stream ends. Supplied per file by {@link M3FileChangeProvider#structure(M3File)};
 * {@code m3/file} has no parser dependency.
 */
public interface M3FileStructHash extends M3FileCharShadow {
    /** Hash of the structured view; valid after {@link #finish()}. */
    long structHash();
}
