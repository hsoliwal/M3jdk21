// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/**
 * Aggregate {@code wc} over the regular files of a traversal.
 *
 * @param files regular files counted
 * @param lines newline bytes
 * @param words POSIX C-locale words
 * @param bytes bytes read
 * @param failed files whose observation failed ({@link M3FileFlag#FAILED} set on their rows)
 */
public record M3FileWordCount(long files, long lines, long words, long bytes, long failed) {
}
