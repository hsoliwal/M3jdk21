// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** S2 differential contract: {@link M3FileWalk} against {@code Files.walk}, {@code walkFileTree}, {@code PathMatcher}, {@code Pattern}. */
class M3FileWalkTest {
    private static final int ALL = Integer.MAX_VALUE;
    private static final List<String> FILES = List.of(
            ".git/HEAD", ".git/objects/aa/obj1", "a/a1.txt", "a/a2.json", "a/deep/d1.txt", "a/deep/d2.bin",
            "a/deep/deeper/x.txt", "b/b1.txt", "b/b2.json", "z.txt", "m.json", ".hidden");

    @TempDir
    Path root;

    @BeforeEach
    void fixture() throws IOException {
        Files.createDirectories(root.resolve("empty"));
        for (int i = 0; i < FILES.size(); i++) write(root.resolve(FILES.get(i)), i + 1);
    }

    @Test
    void orderEqualsFilesWalkAtEveryDepth() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        for (int depth = 0; depth <= 6; depth++) {
            assertEquals(filesWalk(root, depth), tree.walk().maxDepth(depth).paths().toList(), "maxDepth " + depth);
        }
        assertEquals(filesWalk(root, ALL), tree.walk().paths().toList());
        for (String sub : List.of("a", "a/deep", ".git", "empty", "z.txt")) {
            int id = idOf(tree, sub);
            assertEquals(filesWalk(root.resolve(sub), ALL), tree.walk(id).paths().toList(), sub);
            assertEquals(filesWalk(root.resolve(sub), 1), tree.file(id).walk().maxDepth(1).paths().toList(), sub);
        }
    }

    @Test
    void minDepthEqualsDepthFilter() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        for (int min = 0; min <= 5; min++) {
            int bound = min;
            assertEquals(filesWalk(root, ALL).stream().filter(p -> depth(root, p) >= bound).toList(),
                    tree.walk().minDepth(min).paths().toList(), "minDepth " + min);
        }
        assertEquals(filesWalk(root, 3).stream().filter(p -> depth(root, p) >= 2).toList(),
                tree.walk().minDepth(2).maxDepth(3).paths().toList());
    }

    @Test
    void globEqualsPathMatcher() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        for (String glob : List.of("**/*.txt", "*.txt", "a/**", "**/d?.*", "{a,b}/*", "**", "*", "**/.*")) {
            PathMatcher matcher = root.getFileSystem().getPathMatcher("glob:" + glob);
            assertEquals(filesWalk(root, ALL).stream().filter(p -> matcher.matches(root.relativize(p))).toList(),
                    tree.walk().glob(glob).paths().toList(), glob);
        }
        Path a = root.resolve("a");
        PathMatcher txt = root.getFileSystem().getPathMatcher("glob:**.txt");
        assertEquals(filesWalk(a, ALL).stream().filter(p -> txt.matches(a.relativize(p))).toList(),
                tree.walk(idOf(tree, "a")).glob("**.txt").paths().toList());
    }

    @Test
    void regexEqualsPattern() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        for (String regex : List.of(".*\\.txt", "a.*", ".*deep.*", "[^.].*", "")) {
            Pattern pattern = Pattern.compile(regex);
            assertEquals(filesWalk(root, ALL).stream()
                            .filter(p -> pattern.matcher(root.relativize(p).toString()).matches()).toList(),
                    tree.walk().regex(regex).paths().toList(), regex);
        }
        for (String regex : List.of("d\\d\\..*", "\\..*", ".*\\.json", "deep")) {
            Pattern pattern = Pattern.compile(regex);
            assertEquals(filesWalk(root, ALL).stream().filter(p -> pattern.matcher(name(p)).matches()).toList(),
                    tree.walk().nameRegex(regex).paths().toList(), regex);
        }
        Pattern txt = Pattern.compile(".*\\.txt");
        assertEquals(filesWalk(root, ALL).stream().filter(p -> txt.matcher(name(p)).matches())
                        .filter(p -> root.relativize(p).toString().startsWith("a")).toList(),
                tree.walk().nameRegex(txt).regex("a.*").paths().toList());
    }

    @Test
    void pruneEqualsSkipSubtree() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        for (String pruned : List.of(".git", "deep", "a", "empty", "z.txt")) {
            List<Path> expected = skipSubtree(root, ALL, p -> name(p).equals(pruned));
            assertEquals(expected, tree.walk().prune(f -> f.name().equals(pruned)).paths().toList(), pruned);
            assertEquals(expected, tree.walk().pruneIds(id -> tree.name(id).equals(pruned)).paths().toList(), pruned);
        }
        assertEquals(skipSubtree(root, 2, p -> name(p).startsWith(".")),
                tree.walk().maxDepth(2).prune(M3File::isHidden).paths().toList());
        assertEquals(List.of(root), tree.walk().prune(f -> true).paths().toList());
        assertEquals(skipSubtree(root, ALL, p -> name(p).equals(".git") || name(p).equals("deeper")),
                tree.walk().prune(f -> f.name().equals(".git")).prune(f -> f.name().equals("deeper")).paths().toList());
    }

    @Test
    void kindsEqualFilesPredicates() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        assertEquals(filesWalk(root, ALL).stream().filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList(),
                tree.walk().kinds(M3FileKind.FILE).paths().toList());
        assertEquals(filesWalk(root, ALL).stream().filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).toList(),
                tree.walk().kinds(M3FileKind.DIRECTORY).paths().toList());
        assertEquals(List.of(), tree.walk().kinds().paths().toList());
        assertEquals(filesWalk(root, ALL), tree.walk().kinds(M3FileKind.values()).paths().toList());
        assertThrows(IllegalArgumentException.class, () -> tree.walk().kindMask(1 << 4));
    }

    @Test
    void symlinksAreLeavesWithKindSymlink() throws IOException {
        try {
            Files.createSymbolicLink(root.resolve("link-z"), root.resolve("z.txt"));
            Files.createSymbolicLink(root.resolve("link-a"), root.resolve("a"));
        } catch (IOException | UnsupportedOperationException unavailable) {
            Assumptions.abort("symbolic links unavailable: " + unavailable);
        }
        M3FileTree tree = M3FileTree.open(root);
        assertEquals(filesWalk(root, ALL), tree.walk().paths().toList());
        assertEquals(filesWalk(root, ALL).stream().filter(Files::isSymbolicLink).toList(),
                tree.walk().kinds(M3FileKind.SYMLINK).paths().toList());
        assertEquals(0, tree.childCount(idOf(tree, "link-a")));
        assertEquals(M3FileKind.SYMLINK, tree.file(idOf(tree, "link-a")).kind());
    }

    @Test
    void facadeDefaultsAgreeWithFilesWalk() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        List<Path> all = filesWalk(root, ALL);
        assertEquals(all.stream().map(M3FileWalkTest::name).toList(), tree.walk().map(M3File::name).toList());
        List<M3File> each = new ArrayList<>();
        tree.walk().forEach(each::add);
        assertEquals(tree.walk().toList(), each);
        assertEquals(all.size(), tree.walk().count());
        assertEquals(Optional.of(tree.rootFile()), tree.walk().first());
        assertEquals(IntStream.range(0, tree.rows()).boxed().toList(), tree.walk().ids().boxed().toList());
        Map<M3FileKind, Long> kinds = new EnumMap<>(M3FileKind.class);
        for (Path p : all) kinds.merge(Files.isDirectory(p) ? M3FileKind.DIRECTORY : M3FileKind.FILE, 1L, Long::sum);
        assertEquals(kinds, tree.walk().collect(Collectors.groupingBy(M3File::kind,
                () -> new EnumMap<>(M3FileKind.class), Collectors.counting())));
        assertEquals(all.stream().filter(p -> Files.isRegularFile(p) && size(p) > 4).toList(),
                tree.walk().kinds(M3FileKind.FILE).filter(f -> f.size() > 4).paths().toList());
        assertEquals(all.stream().filter(p -> Files.isRegularFile(p) && size(p) > 4).toList(),
                tree.walk().filterIds(id -> tree.size(id) > 4).filter(M3File::isRegularFile).paths().toList());
        assertThrows(IllegalStateException.class, () -> M3File.of(root).walk());
    }

    @Test
    void parallelEqualsSequentialAndSplitsAreDisjoint() throws IOException {
        for (int d = 0; d < 12; d++) {
            for (int s = 0; s < 4; s++) {
                for (int f = 0; f < 9; f++) write(root.resolve("big/d" + d + "/s" + s + "/f" + f + (f % 2 == 0 ? ".txt" : ".bin")), f);
            }
        }
        M3FileTree tree = M3FileTree.open(root);
        assertEquals(filesWalk(root, ALL), tree.walk().parallelStream().map(M3File::toPath).toList());
        List<M3FileWalk> walks = List.of(tree.walk(), tree.walk().glob("**/*.txt"), tree.walk().maxDepth(3),
                tree.walk().minDepth(3), tree.walk().prune(f -> f.name().startsWith("s1")),
                tree.walk().pruneIds(id -> tree.name(id).equals("d3")).kinds(M3FileKind.DIRECTORY),
                tree.walk().filter(f -> f.size() % 2 == 0), tree.walk(idOf(tree, "big/d5")));
        for (M3FileWalk walk : walks) {
            List<Integer> sequential = walk.ids().boxed().toList();
            assertEquals(sequential, walk.ids().parallel().boxed().toList());
            assertEquals(walk.paths().toList(), walk.parallelStream().map(M3File::toPath).toList());
            assertEquals(sequential.size(), walk.parallelStream().count());
            for (int depth = 1; depth <= 8; depth++) {
                List<Integer> drained = new ArrayList<>();
                Spliterator.OfInt spliterator = walk.idSpliterator();
                for (int i = 0; i < depth && spliterator.tryAdvance((IntConsumer) drained::add); i++) {
                    // advance a few rows before splitting
                }
                drain(spliterator, depth, drained);
                assertEquals(sequential, drained, "split depth " + depth);
            }
        }
        assertNotNull(tree.walk().idSpliterator().trySplit());
    }

    @Test
    void sizedOnlyWhenPlain() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        Spliterator<M3File> plain = tree.walk().spliterator();
        assertTrue(plain.hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED));
        assertEquals(filesWalk(root, ALL).size(), plain.getExactSizeIfKnown());
        Spliterator<M3File> prefix = plain.trySplit();
        assertNotNull(prefix);
        assertEquals(filesWalk(root, ALL).size(), prefix.getExactSizeIfKnown() + plain.getExactSizeIfKnown());
        Spliterator<M3File> filtered = tree.walk().glob("**").spliterator();
        assertFalse(filtered.hasCharacteristics(Spliterator.SIZED));
        assertTrue(filtered.hasCharacteristics(Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.NONNULL));
        assertEquals(-1, filtered.getExactSizeIfKnown());
    }

    @Test
    void cursorMatchesStreamAndSkipsSubtrees() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        M3FileCursor plain = tree.walk().cursor();
        List<Integer> ids = new ArrayList<>();
        for (int id = plain.nextId(); id >= 0; id = plain.nextId()) ids.add(id);
        assertEquals(tree.walk().ids().boxed().toList(), ids);
        assertFalse(plain.hasNext());
        assertEquals(-1, plain.nextId());
        M3FileCursor cursor = tree.walk().cursor();
        List<Path> seen = new ArrayList<>();
        for (Optional<M3File> entry = cursor.nextEntry(); entry.isPresent(); entry = cursor.nextEntry()) {
            seen.add(entry.get().toPath());
            if (entry.get().name().equals("deep")) {
                assertTrue(cursor.hasNext());
                cursor.skipSubtree();
            }
            if (entry.get().name().equals(".git")) cursor.skipSubtree();
        }
        assertEquals(skipSubtree(root, ALL, p -> name(p).equals("deep") || name(p).equals(".git")), seen);
        assertThrows(IllegalStateException.class, () -> tree.walk().cursor().skipSubtree());
    }

    private static void drain(Spliterator.OfInt spliterator, int depth, List<Integer> out) {
        Spliterator.OfInt prefix = depth > 0 ? spliterator.trySplit() : null;
        if (prefix == null) {
            spliterator.forEachRemaining((IntConsumer) out::add);
            return;
        }
        drain(prefix, depth - 1, out);
        drain(spliterator, depth - 1, out);
    }

    private static List<Path> filesWalk(Path start, int maxDepth) throws IOException {
        try (Stream<Path> paths = Files.walk(start, maxDepth)) {
            return paths.toList();
        }
    }

    private static List<Path> skipSubtree(Path start, int maxDepth, Predicate<Path> prune) throws IOException {
        List<Path> out = new ArrayList<>();
        Files.walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), maxDepth, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                out.add(dir);
                return prune.test(dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                out.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return out;
    }

    private static int depth(Path start, Path p) {
        return p.equals(start) ? 0 : start.relativize(p).getNameCount();
    }

    private static String name(Path p) {
        return p.getFileName().toString();
    }

    private static long size(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(Path file, int bytes) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "x".repeat(bytes), StandardCharsets.US_ASCII);
    }

    private int idOf(M3FileTree tree, String relative) {
        Path target = root.resolve(relative);
        return tree.ids().filter(id -> tree.path(id).equals(target)).findFirst().orElseThrow();
    }
}
