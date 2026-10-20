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
import java.util.EnumSet;

/**
 * Fills an {@link M3FileTree} from {@link Files#walkFileTree} in pre-order.
 *
 * <p>Links are never followed. Frames, child collection and CSR slices come from
 * {@link M3FileTreeBuilder}. Hidden state comes from the attributes the walk already read. An entry
 * whose attributes cannot be read is still recorded, with unknown size and time and kind
 * {@link M3FileKind#OTHER}.</p>
 */
final class M3FileTreeScanner extends SimpleFileVisitor<Path> {
    private final M3FileTree tree;
    private final M3FileTreeBuilder builder;
    private final int startId;
    private boolean started;

    private M3FileTreeScanner(M3FileTree tree, int startId) {
        this.tree = tree;
        this.builder = new M3FileTreeBuilder(tree);
        this.startId = startId;
    }

    /**
     * Scans {@code maxDepth} levels: from the root of an empty tree ({@code startId < 0}), or below the
     * existing unscanned directory row {@code startId}, whose new descendants are appended in pre-order.
     * Directories at {@code maxDepth} become rows without children.
     */
    static M3FileTree scan(M3FileTree tree, int startId, int maxDepth) throws IOException {
        if (startId >= 0 && maxDepth < 1) throw new IllegalArgumentException("expansion needs maxDepth >= 1");
        M3FileTreeScanner scanner = new M3FileTreeScanner(tree, startId);
        Path start = startId < 0 ? tree.root() : tree.path(startId);
        Files.walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), maxDepth, scanner);
        if (tree.rows() == 0) throw new IOException("root was not visited: " + tree.root());
        return tree;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        boolean expanding = !started && startId >= 0;
        started = true;
        builder.openFrame(expanding ? startId : append(dir, fresh(dir, attrs)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        started = true;
        append(file, attrs.isDirectory() ? fresh(file, attrs) : attrs);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        if (!started && startId >= 0) throw exc;
        started = true;
        String name = nameOf(file);
        builder.append(name, M3FileKind.OTHER, dotName(name), -1L, Long.MIN_VALUE);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        builder.closeFrame();
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
        boolean hidden = dotName(name) || (attrs instanceof DosFileAttributes dos && dos.isHidden());
        int id = builder.append(name, M3FileKind.of(attrs), hidden, attrs.size(), attrs.lastModifiedTime().toMillis());
        tree.hashes().fileKey(id, attrs.fileKey());
        return id;
    }

    static String nameOf(Path path) {
        Path name = path.getFileName();
        return name == null ? path.toString() : name.toString();
    }

    static boolean dotName(String name) {
        return name.length() > 1 && name.charAt(0) == '.' && !".".equals(name) && !"..".equals(name);
    }
}
