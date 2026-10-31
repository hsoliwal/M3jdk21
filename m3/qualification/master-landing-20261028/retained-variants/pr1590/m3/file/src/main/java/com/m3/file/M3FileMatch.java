// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/**
 * One {@code grep} hit: the entry, the 1-based line number (LF, CR, CRLF terminators) and the line text.
 *
 * @param file entry containing the line
 * @param line 1-based line number
 * @param text the line without its terminator
 */
public record M3FileMatch(M3File file, long line, String text) {
}
