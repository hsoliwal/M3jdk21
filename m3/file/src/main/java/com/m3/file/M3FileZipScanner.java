// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Fills an {@link M3FileTree} from a zip/jar: the archive file is the root row, its central directory
 * ({@link ZipFile#entries()}) the rows below, nothing extracted.
 *
 * <p>Entry names split on {@code /} (empty segments dropped); missing parent directories are implied
 * rows with unknown time; duplicates keep the first entry. Entries typed {@code zip/jar/war/ear} are
 * listed once by streaming them through {@link ZipInputStream} (their own central directory is not
 * reachable without extraction) and become sub-trees flagged {@link M3FileFlag#ARCHIVE}, up to
 * {@link #MAX_NESTING} levels and {@link #NESTED_LIMIT} inflated bytes per nested stream; a nested
 * entry that does not list as an archive stays a plain file. Archive-directory CRC32s go to the
 * {@code entryCrc} lane for change gating. Scan-time item records are discarded once rows exist.</p>
 */
final class M3FileZipScanner {
    static final Set<String> ARCHIVE_TYPES = Set.of("zip", "jar", "war", "ear");
    static final int MAX_NESTING = 4;
    static final long NESTED_LIMIT = 1L << 30;

    private static final Comparator<Item> SEGMENT_ORDER = (a, b) -> {
        for (int i = 0; i < Math.min(a.segments.length, b.segments.length); i++) {
            int order = a.segments[i].compareTo(b.segments[i]);
            if (order != 0) return order;
        }
        return a.segments.length - b.segments.length;
    };

    private record Item(String[] segments, boolean directory, long size, long mtime, long crc, List<Item> nested) {
    }

    private M3FileZipScanner() {
    }

    static M3FileTree scan(Path archive, int residencyCapacity) throws IOException {
        ZipFile zip = new ZipFile(archive.toFile());
        try {
            M3FileTree tree = M3FileTree.create(archive, residencyCapacity, new M3FileZipSource(zip));
            BasicFileAttributes attrs = Files.readAttributes(archive, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            M3FileTreeBuilder builder = new M3FileTreeBuilder(tree);
            String name = M3FileTreeScanner.nameOf(archive);
            int root = builder.append(name, M3FileKind.FILE, M3FileTreeScanner.dotName(name), attrs.size(),
                    attrs.lastModifiedTime().toMillis());
            tree.setFlag(M3FileFlag.ARCHIVE, root, true);
            List<Item> items = new ArrayList<>();
            for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = entries.nextElement();
                items.add(item(entry, entry.getSize(), nestedOf(zip, entry)));
            }
            builder.openFrame(root);
            emit(tree, builder, items);
            builder.closeFrame();
            return tree;
        } catch (IOException | RuntimeException failure) {
            try {
                zip.close();
            } catch (IOException suppressed) {
                failure.addSuppressed(suppressed);
            }
            throw failure;
        }
    }

    /** Normalised entry name: segments joined by {@code /}. */
    static String normalize(String name) {
        return String.join("/", segments(name));
    }

    static boolean nestable(String name) {
        return ARCHIVE_TYPES.contains(M3FileTree.extension(name.endsWith("/") ? "" : lastSegment(name)));
    }

    private static List<Item> nestedOf(ZipFile zip, ZipEntry entry) {
        if (entry.isDirectory() || !nestable(entry.getName()) || MAX_NESTING < 2) return List.of();
        try (InputStream in = zip.getInputStream(entry)) {
            return list(new Counting(in, NESTED_LIMIT), 2);
        } catch (IOException notAnArchive) {
            return List.of();
        }
    }

    private static List<Item> list(InputStream in, int depth) throws IOException {
        List<Item> items = new ArrayList<>();
        ZipInputStream archive = new ZipInputStream(in);
        for (ZipEntry entry = archive.getNextEntry(); entry != null; entry = archive.getNextEntry()) {
            Counting body = new Counting(archive, NESTED_LIMIT);
            List<Item> nested = List.of();
            if (!entry.isDirectory() && nestable(entry.getName()) && depth < MAX_NESTING) {
                try {
                    nested = list(body, depth + 1);
                } catch (IOException notAnArchive) {
                    nested = List.of();
                }
            }
            body.transferTo(OutputStream.nullOutputStream());
            items.add(item(entry, body.count, nested));
        }
        return items;
    }

    private static Item item(ZipEntry entry, long size, List<Item> nested) {
        FileTime time = entry.getLastModifiedTime();
        return new Item(segments(entry.getName()), entry.isDirectory(), size,
                time == null ? Long.MIN_VALUE : time.toMillis(), entry.getCrc(), nested);
    }

    private static void emit(M3FileTree tree, M3FileTreeBuilder builder, List<Item> items) {
        List<Item> sorted = new ArrayList<>(items);
        sorted.sort(SEGMENT_ORDER);
        String[] stack = new String[8];
        int open = 0;
        String[] previous = null;
        for (Item item : sorted) {
            String[] segments = item.segments;
            if (segments.length == 0 || Arrays.equals(segments, previous)) continue;
            previous = segments;
            int parents = segments.length - 1;
            int common = 0;
            while (common < open && common < parents && stack[common].equals(segments[common])) common++;
            for (; open > common; open--) builder.closeFrame();
            for (int k = common; k < parents; k++) {
                builder.openFrame(builder.append(segments[k], M3FileKind.DIRECTORY,
                        M3FileTreeScanner.dotName(segments[k]), 0, Long.MIN_VALUE));
                stack = push(stack, open++, segments[k]);
            }
            String name = segments[parents];
            boolean hidden = M3FileTreeScanner.dotName(name);
            if (item.directory) {
                builder.openFrame(builder.append(name, M3FileKind.DIRECTORY, hidden, 0, item.mtime));
                stack = push(stack, open++, name);
                continue;
            }
            int file = builder.append(name, M3FileKind.FILE, hidden, item.size, item.mtime);
            tree.hashes().entryCrc(file, item.crc);
            if (!item.nested.isEmpty()) {
                tree.setFlag(M3FileFlag.ARCHIVE, file, true);
                builder.openFrame(file);
                emit(tree, builder, item.nested);
                builder.closeFrame();
            }
        }
        for (; open > 0; open--) builder.closeFrame();
    }

    private static String[] push(String[] stack, int at, String segment) {
        String[] grown = at == stack.length ? Arrays.copyOf(stack, stack.length * 2) : stack;
        grown[at] = segment;
        return grown;
    }

    private static String[] segments(String name) {
        return Arrays.stream(name.split("/")).filter(segment -> !segment.isEmpty()).toArray(String[]::new);
    }

    private static String lastSegment(String name) {
        return name.substring(name.lastIndexOf('/') + 1);
    }

    /** Counts bytes, optionally caps them, and never closes the stream it wraps. */
    private static final class Counting extends FilterInputStream {
        private final long limit;
        private long count;

        Counting(InputStream in, long limit) {
            super(in);
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int b = in.read();
            if (b >= 0) counted(1);
            return b;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            int n = in.read(bytes, offset, length);
            if (n > 0) counted(n);
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = in.skip(n);
            if (skipped > 0) counted(skipped);
            return skipped;
        }

        @Override
        public void close() {
        }

        private void counted(long n) throws IOException {
            count += n;
            if (count > limit) throw new IOException("nested archive exceeds " + limit + " inflated bytes");
        }
    }
}
