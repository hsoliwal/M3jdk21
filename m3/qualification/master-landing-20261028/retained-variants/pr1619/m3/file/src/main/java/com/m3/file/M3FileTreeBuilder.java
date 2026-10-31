// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;

/**
 * Pre-order row builder shared by the scanners (file system, archives).
 *
 * <p>Each open directory (or archive) is a frame on an {@code int} stack; its direct child ids are
 * collected there, heap-sorted by name without allocation and written as one CSR slice when the frame
 * closes, which also closes the pre-order subtree range of the frame's row.</p>
 */
final class M3FileTreeBuilder {
    private final M3FileTree tree;
    private int[] pending = new int[256];
    private int[] frameStart = new int[32];
    private int[] frameDir = new int[32];
    private int pendingTop;
    private int frames;

    M3FileTreeBuilder(M3FileTree tree) {
        this.tree = tree;
    }

    /** Row of the innermost open frame, {@code -1} before the root. */
    int parentId() {
        return frames == 0 ? -1 : frameDir[frames - 1];
    }

    int frames() {
        return frames;
    }

    /** Appends a row under the innermost open frame and lists it as that frame's child. */
    int append(String name, M3FileKind kind, boolean hidden, long size, long mtime) {
        int id = tree.append(parentId(), name, kind, hidden, size, mtime);
        if (frames > 0) {
            if (pendingTop == pending.length) pending = Arrays.copyOf(pending, pendingTop * 2);
            pending[pendingTop++] = id;
        }
        return id;
    }

    void openFrame(int dirId) {
        if (frames == frameStart.length) {
            frameStart = Arrays.copyOf(frameStart, frames * 2);
            frameDir = Arrays.copyOf(frameDir, frames * 2);
        }
        frameStart[frames] = pendingTop;
        frameDir[frames] = dirId;
        frames++;
    }

    void closeFrame() {
        frames--;
        int from = frameStart[frames];
        heapSort(pending, from, pendingTop);
        tree.closeDirectory(frameDir[frames], pending, from, pendingTop);
        pendingTop = from;
    }

    /** In-place heap sort of {@code a[from, to)} by {@link M3FileTree#compareNames}. */
    private void heapSort(int[] a, int from, int to) {
        int n = to - from;
        for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, from, i, n);
        for (int end = n - 1; end > 0; end--) {
            swap(a, from, from + end);
            siftDown(a, from, 0, end);
        }
    }

    private void siftDown(int[] a, int base, int root, int n) {
        int i = root;
        while (true) {
            int left = 2 * i + 1;
            if (left >= n) return;
            int child = left + 1 < n && tree.compareNames(a[base + left + 1], a[base + left]) > 0 ? left + 1 : left;
            if (tree.compareNames(a[base + child], a[base + i]) <= 0) return;
            swap(a, base + i, base + child);
            i = child;
        }
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
