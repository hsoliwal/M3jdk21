// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

/**
 * tree-lanes-port-20261107 (m3/file): seeded tree-corpus differential of the five ported tree-lane leaves
 * (M3FileFlag, M3FileKind, M3FileNames, M3FileScanSpec, M3FileWordCount). The same class compiles against the
 * retained PR #1619 module snapshot (donor side) and against today's module with the ported leaves (head side);
 * the two outputs must be byte-identical. Every episode builds one seeded directory tree under the given scratch
 * root, walks it (links never followed), classifies every row with M3FileKind.of, interns every name segment into
 * two M3FileNames tables (walk order and reverse order) and prints every observable; then a pure in-memory batch
 * exercises interning, rehash, find, compare, M3FileScanSpec validation and M3FileWordCount identity.
 *
 * usage: TreeLeafDifferential <seed> <episodes> <out-file> <scratch-root>
 */
public final class TreeLeafDifferential {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.";
    private static final String[] TYPES = {"", ".java", ".txt", ".zip", ".jar", ".JAVA", ".tar.gz", ".c", ".md", "."};
    private static final int[] CAPACITIES = {1, 2, 4, 16, 4096, 65536, 1 << 30, 0, -1, 3, 12, 4095, 4097, Integer.MAX_VALUE,
        Integer.MIN_VALUE};

    private TreeLeafDifferential() { }

    public static void main(String[] args) throws IOException {
        long seed = Long.decode(args[0]);
        int episodes = Integer.parseInt(args[1]);
        Path out = Path.of(args[2]);
        Path scratch = Path.of(args[3]);
        Files.createDirectories(scratch);
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            w.println("flags " + String.join(",", flagNames()) + " kinds " + kindNames() + " ALL=" + M3FileKind.ALL);
            for (int episode = 0; episode < episodes; episode++) {
                SplittableRandom rng = new SplittableRandom(seed * 1_000_003L + episode);
                Path root = scratch.resolve("ep" + episode);
                treeEpisode(w, rng, root, episode);
                memoryEpisode(w, rng, episode);
                specEpisode(w, rng, episode);
                countEpisode(w, rng, episode);
            }
            w.println("F failure=none");
        }
    }

    private static List<String> flagNames() {
        List<String> names = new ArrayList<>();
        for (M3FileFlag flag : M3FileFlag.values()) names.add(flag.name() + "=" + flag.ordinal());
        return names;
    }

    private static String kindNames() {
        StringBuilder sb = new StringBuilder();
        for (M3FileKind kind : M3FileKind.values()) sb.append(kind.name()).append('=').append(kind.bit()).append(' ');
        return sb.toString().trim();
    }

    private static String segment(SplittableRandom rng) {
        int length = 1 + rng.nextInt(12);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(ALPHABET.charAt(rng.nextInt(ALPHABET.length())));
        String name = sb.toString();
        if (name.endsWith(".") || name.endsWith(" ")) name = name + "x";
        return name + TYPES[rng.nextInt(TYPES.length)];
    }

    private static void treeEpisode(PrintWriter w, SplittableRandom rng, Path root, int episode) throws IOException {
        Files.createDirectories(root);
        int dirs = 1 + rng.nextInt(5);
        int files = rng.nextInt(12);
        List<Path> parents = new ArrayList<>();
        parents.add(root);
        for (int i = 0; i < dirs; i++) {
            Path parent = parents.get(rng.nextInt(parents.size()));
            Path dir = parent.resolve(uniqueName(rng, parent));
            Files.createDirectories(dir);
            parents.add(dir);
        }
        for (int i = 0; i < files; i++) {
            Path parent = parents.get(rng.nextInt(parents.size()));
            byte[] body = new byte[rng.nextInt(64)];
            rng.nextBytes(body);
            Files.write(parent.resolve(uniqueName(rng, parent)), body);
        }
        List<Path> rows = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                rows.add(dir); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                rows.add(file); return FileVisitResult.CONTINUE;
            }
        });
        rows.sort(Comparator.comparing(p -> root.relativize(p).toString().replace('\\', '/')));
        M3FileNames forward = new M3FileNames(1 + rng.nextInt(4));
        M3FileNames backward = new M3FileNames(64);
        List<String> names = new ArrayList<>();
        int kindMask = 0;
        for (Path row : rows) {
            BasicFileAttributes attrs = Files.readAttributes(row, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            M3FileKind kind = M3FileKind.of(attrs);
            kindMask |= kind.bit();
            String name = row.getFileName() == null ? "" : row.getFileName().toString();
            int id = forward.intern(name);
            names.add(name);
            String rel = root.relativize(row).toString().replace('\\', '/');
            w.println("T" + episode + " " + rel + " kind=" + kind + " bit=" + kind.bit() + " id=" + id + " find="
                + forward.find(name) + " len=" + forward.length(id) + " name=" + forward.name(id).equals(name)
                + " size=" + (kind == M3FileKind.FILE ? attrs.size() : -1));
        }
        for (int i = names.size() - 1; i >= 0; i--) backward.intern(names.get(i));
        w.println("N" + episode + " rows=" + rows.size() + " count=" + forward.count() + "/" + backward.count()
            + " pool=" + forward.poolChars() + "/" + backward.poolChars() + " kinds=" + kindMask
            + " maskAll=" + M3FileKind.mask(M3FileKind.values()) + " maskNone=" + M3FileKind.mask()
            + " maskFD=" + M3FileKind.mask(M3FileKind.FILE, M3FileKind.DIRECTORY));
        for (int i = 0; i < names.size(); i++) {
            int a = forward.find(names.get(i));
            int b = backward.find(names.get(i));
            int other = forward.find(names.get(rng.nextInt(names.size())));
            w.println("C" + episode + " " + i + " cmp=" + Integer.signum(forward.compare(a, other))
                + " cross=" + Integer.signum(forward.compare(a, backward, b))
                + " crossOther=" + Integer.signum(forward.compare(a, backward, backward.find(names.get(rng.nextInt(names.size())))))
                + " chars=" + charHash(forward, a));
        }
        deleteTree(root);
    }

    private static String uniqueName(SplittableRandom rng, Path parent) {
        String name = segment(rng);
        while (Files.exists(parent.resolve(name), LinkOption.NOFOLLOW_LINKS)) name = segment(rng) + rng.nextInt(1000);
        return name;
    }

    private static void deleteTree(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir); return FileVisitResult.CONTINUE;
            }
        });
    }

    private static long charHash(M3FileNames names, int id) {
        long h = 1469598103934665603L;
        for (int i = 0; i < names.length(id); i++) h = (h ^ names.charAt(id, i)) * 1099511628211L;
        return h;
    }

    private static void memoryEpisode(PrintWriter w, SplittableRandom rng, int episode) {
        int n = 50 + rng.nextInt(1500);
        M3FileNames names = new M3FileNames(1 + rng.nextInt(8));
        List<String> inserted = new ArrayList<>();
        long check = 0;
        for (int i = 0; i < n; i++) {
            String s = rng.nextInt(4) == 0 && !inserted.isEmpty() ? inserted.get(rng.nextInt(inserted.size())) : segment(rng);
            int id = names.intern(s);
            inserted.add(s);
            check = check * 31 + id;
            if (names.find(s) != id || !names.name(id).equals(s) || names.length(id) != s.length()) {
                w.println("M" + episode + " MISMATCH " + s + " " + id); break;
            }
        }
        StringBuilder appended = new StringBuilder();
        for (int i = 0; i < Math.min(32, names.count()); i++) names.appendTo(i, appended);
        int absent = 0;
        for (int i = 0; i < 64; i++) if (names.find(segment(rng) + "é" + i) < 0) absent++;
        int order = 0;
        for (int i = 1; i < names.count(); i++) order += Integer.signum(names.compare(i - 1, i)) + 2 * Integer.signum(names.compare(i, i - 1));
        w.println("M" + episode + " n=" + n + " count=" + names.count() + " pool=" + names.poolChars() + " check=" + check
            + " absent=" + absent + " order=" + order + " appended=" + appended.toString().hashCode()
            + " self=" + names.compare(0, 0) + " cross=" + names.compare(0, names, 0));
    }

    private static void specEpisode(PrintWriter w, SplittableRandom rng, int episode) {
        int capacity = CAPACITIES[rng.nextInt(CAPACITIES.length)];
        int depth = rng.nextInt(6) == 0 ? -1 - rng.nextInt(3) : rng.nextInt(4) == 0 ? Integer.MAX_VALUE : rng.nextInt(40);
        StringBuilder sb = new StringBuilder("S" + episode + " depth=" + depth + " cap=" + capacity);
        try {
            M3FileScanSpec spec = new M3FileScanSpec(depth, capacity);
            sb.append(" spec=").append(spec).append(" arch=").append(spec.withArchives().expandArchives())
              .append(" eq=").append(spec.equals(new M3FileScanSpec(depth, capacity, false)))
              .append(" hash=").append(spec.hashCode() == new M3FileScanSpec(depth, capacity).hashCode());
        } catch (IllegalArgumentException refused) {
            sb.append(" refused=").append(refused.getMessage());
        }
        try {
            M3FileScanSpec lazy = M3FileScanSpec.lazy(depth);
            sb.append(" lazy=").append(lazy.maxDepth()).append('/').append(lazy.residencyCapacity()).append('/').append(lazy.expandArchives());
        } catch (IllegalArgumentException refused) {
            sb.append(" lazyRefused=").append(refused.getMessage());
        }
        sb.append(" eager=").append(M3FileScanSpec.EAGER.maxDepth()).append('/').append(M3FileScanSpec.EAGER.residencyCapacity())
          .append('/').append(M3FileScanSpec.EAGER.expandArchives());
        w.println(sb);
    }

    private static void countEpisode(PrintWriter w, SplittableRandom rng, int episode) {
        long files = rng.nextLong(0, 1L << 40), lines = rng.nextLong(0, 1L << 40), words = rng.nextLong(0, 1L << 40);
        long bytes = rng.nextLong(), failed = rng.nextInt(3);
        M3FileWordCount a = new M3FileWordCount(files, lines, words, bytes, failed);
        M3FileWordCount b = new M3FileWordCount(files, lines, words, bytes, failed);
        M3FileWordCount c = new M3FileWordCount(files, lines, words, bytes, failed + 1);
        w.println("W" + episode + " " + a + " eq=" + a.equals(b) + " ne=" + a.equals(c) + " hash=" + (a.hashCode() == b.hashCode())
            + " sum=" + (a.files() + a.lines() + a.words() + a.bytes() + a.failed()));
    }
}
