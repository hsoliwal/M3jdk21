// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * Fills an {@link M3FileTree} from {@link Files#walkFileTree} in pre-order.
 *
 * <p>Links are never followed. Each directory opens a frame on an {@code int} stack; its direct
 * child ids are collected there, heap-sorted by name (no allocation) and written as one CSR slice
 * when the directory closes. Hidden state comes from the attributes the walk already read, so no
 * second stat is issued. An entry whose attributes cannot be read is still recorded, with unknown
 * size and time and no flags.</p>
 */
final class M3FileTreeScanner extends SimpleFileVisitor<Path> {
    private final M3FileTree tree;
    private int[] pending = new int[256];
    private int[] frameStart = new int[32];
    private int[] frameDir = new int[32];
    private int pendingTop;
    private int frames;

    private M3FileTreeScanner(M3FileTree tree) {
        this.tree = tree;
    }

    static M3FileTree scan(M3FileTree tree) throws IOException {
        M3FileTreeScanner scanner = new M3FileTreeScanner(tree);
        Files.walkFileTree(tree.root(), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, scanner);
        if (tree.rows() == 0) throw new IOException("root was not visited: " + tree.root());
        return tree;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        openFrame(append(dir, fresh(dir, attrs)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        append(file, attrs);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        String name = nameOf(file);
        enlist(tree.append(parentId(), name, M3FileKind.OTHER, dotName(name), -1L, Long.MIN_VALUE));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        closeFrame();
        return FileVisitResult.CONTINUE;
    }

    /**
     * Directory attributes from a Windows listing come from the parent's NTFS index entry, which is
     * updated lazily when children change; a directory's own record is current, so it is read again.
     */
    private static BasicFileAttributes fresh(Path dir, BasicFileAttributes attrs) {
        if (!(attrs instanceof DosFileAttributes)) return attrs;
        try {
            return Files.readAttributes(dir, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException unreadable) {
            return attrs;
        }
    }

    private int append(Path path, BasicFileAttributes attrs) {
        String name = nameOf(path);
        M3FileKind kind = M3FileKind.of(attrs);
        boolean hidden = dotName(name) || (attrs instanceof DosFileAttributes dos && dos.isHidden());
        int id = tree.append(parentId(), name, kind, hidden,
                attrs.size(), attrs.lastModifiedTime().toMillis());
        tree.hashes().fileKey(id, attrs.fileKey());
        enlist(id);
        return id;
    }

    private static String nameOf(Path path) {
        Path name = path.getFileName();
        return name == null ? path.toString() : name.toString();
    }

    private static boolean dotName(String name) {
        return name.length() > 1 && name.charAt(0) == '.' && !".".equals(name) && !"..".equals(name);
    }

    private int parentId() {
        return frames == 0 ? -1 : frameDir[frames - 1];
    }

    private void enlist(int id) {
        if (frames == 0) return;
        if (pendingTop == pending.length) pending = Arrays.copyOf(pending, pendingTop * 2);
        pending[pendingTop++] = id;
    }

    private void openFrame(int dirId) {
        if (frames == frameStart.length) {
            frameStart = Arrays.copyOf(frameStart, frames * 2);
            frameDir = Arrays.copyOf(frameDir, frames * 2);
        }
        frameStart[frames] = pendingTop;
        frameDir[frames] = dirId;
        frames++;
    }

    private void closeFrame() {
        frames--;
        int from = frameStart[frames];
        heapSort(pending, from, pendingTop);
        tree.closeDirectory(frameDir[frames], pending, from, pendingTop);
        pendingTop = from;
    }

    /** In-place heap sort of {@code a[from, to)} by {@link M3FileTree#compareNames}; names in one directory are unique. */
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
