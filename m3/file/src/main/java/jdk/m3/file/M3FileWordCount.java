// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;
//FLLDCIM: RESTORE - source m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java/jdk/m3/file/M3FileWordCount.java blob 6ef808b65b6323320a5f6c25699c5c17c4c573d7 (retained PR #1619 head; parent 0cd0944136c1 blob faf766173e7754056ebb09a846c0d03f1ccbb07a https://github.com/hsoliwal/M3jdk21/blob/0cd0944136c1/m3/file/src/main/java/com/m3/file/M3FileWordCount.java); recipe m3/file/qualification/tree-lanes-port-20261107 RESTORE_EXACT

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
