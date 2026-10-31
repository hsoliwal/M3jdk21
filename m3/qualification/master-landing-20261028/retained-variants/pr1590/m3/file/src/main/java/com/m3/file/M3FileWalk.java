// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable walk over the subtree of one {@link M3FileTree} row ({@code m3/file/DESIGN.md} section 5).
 *
 * <p>Encounter order is pre-order scan order, the order of {@code Files.walk} over the same directory
 * state. Each configuration method returns a new walk; filters compose with AND, prunes with OR.
 * Depths are relative to the walk root (root = 0). Kind and type filters are lane tests. {@code prune} stops descent below a matching
 * directory while the directory itself still meets the filters ({@code find -prune}); pruned and
 * depth-capped subtrees are skipped in one jump through the {@code subtreeEnd} lane. Lane tests (depth,
 * kind, {@code filterIds}, name and path patterns) run before facade predicates, so most rejected
 * rows never become an {@link M3File}. Predicates must be pure: a split may evaluate {@code prune}
 * on an ancestor more than once.</p>
 */
public final class M3FileWalk implements M3FileTraversal {
    private static final Pattern[] NO_PATTERNS = new Pattern[0];

    private final M3FileTree tree;
    private final int root;
    private final int rootDepth;
    private final int minDepth;
    private final int maxDepth;
    private final int kindMask;
    private final BitSet types;
    private final PathMatcher glob;
    private final Pattern[] pathRegex;
    private final Pattern[] nameRegex;
    private final IntPredicate pruneIds;
    private final Predicate<M3File> prune;
    private final IntPredicate filterIds;
    private final Predicate<M3File> filter;

    M3FileWalk(M3FileTree tree, int root) {
        this(tree, root, 0, Integer.MAX_VALUE, M3FileKind.ALL, null, null, NO_PATTERNS, NO_PATTERNS,
                null, null, null, null);
    }

    private M3FileWalk(M3FileTree tree, int root, int minDepth, int maxDepth, int kindMask, BitSet types,
            PathMatcher glob, Pattern[] pathRegex, Pattern[] nameRegex, IntPredicate pruneIds,
            Predicate<M3File> prune, IntPredicate filterIds, Predicate<M3File> filter) {
        this.tree = tree;
        this.root = root;
        this.rootDepth = tree.depth(root);
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
        this.kindMask = kindMask;
        this.types = types;
        this.glob = glob;
        this.pathRegex = pathRegex;
        this.nameRegex = nameRegex;
        this.pruneIds = pruneIds;
        this.prune = prune;
        this.filterIds = filterIds;
        this.filter = filter;
    }

    @Override
    public M3FileTree tree() {
        return tree;
    }

    public int rootId() {
        return root;
    }

    /** Emits only entries at relative depth {@code >= depth}. */
    public M3FileWalk minDepth(int depth) {
        return new M3FileWalk(tree, root, requireDepth(depth), maxDepth, kindMask, types, glob, pathRegex,
                nameRegex, pruneIds, prune, filterIds, filter);
    }

    /** Does not descend below relative depth {@code depth}, like {@code Files.walk(start, depth)}. */
    public M3FileWalk maxDepth(int depth) {
        return new M3FileWalk(tree, root, minDepth, requireDepth(depth), kindMask, types, glob, pathRegex,
                nameRegex, pruneIds, prune, filterIds, filter);
    }

    public M3FileWalk kinds(M3FileKind... kinds) {
        return kindMask(M3FileKind.mask(kinds));
    }

    /** Emits only entries whose {@link M3FileKind#bit()} is in {@code mask}. */
    public M3FileWalk kindMask(int mask) {
        if ((mask & ~M3FileKind.ALL) != 0) throw new IllegalArgumentException("unknown kind bits: " + mask);
        return new M3FileWalk(tree, root, minDepth, maxDepth, mask, types, glob, pathRegex, nameRegex,
                pruneIds, prune, filterIds, filter);
    }

    /**
     * Emits only entries whose interned type (lower-case extension, leading dot optional, {@code ""} for
     * none and for directories) is one of {@code extensions}; a lane test on {@code typeId}.
     */
    public M3FileWalk types(String... extensions) {
        BitSet wanted = new BitSet();
        for (String extension : extensions) {
            String key = extension.startsWith(".") ? extension.substring(1) : extension;
            int id = tree.types().find(key.toLowerCase(Locale.ROOT));
            if (id >= 0) wanted.set(id);
        }
        if (types != null) wanted.and(types);
        return new M3FileWalk(tree, root, minDepth, maxDepth, kindMask, wanted, glob, pathRegex, nameRegex,
                pruneIds, prune, filterIds, filter);
    }

    /** JDK {@code glob:} syntax over the path relative to the walk root (the root itself is the empty path). */
    public M3FileWalk glob(String pattern) {
        PathMatcher next = tree.root().getFileSystem().getPathMatcher("glob:" + pattern);
        PathMatcher previous = glob;
        PathMatcher composed = previous == null ? next : p -> previous.matches(p) && next.matches(p);
        return new M3FileWalk(tree, root, minDepth, maxDepth, kindMask, types, composed, pathRegex, nameRegex,
                pruneIds, prune, filterIds, filter);
    }

    public M3FileWalk regex(String regex) {
        return regex(Pattern.compile(regex));
    }

    /** Full match over the relative path string (platform separator, as {@code root.relativize(p).toString()}). */
    public M3FileWalk regex(Pattern pattern) {
        return new M3FileWalk(tree, root, minDepth, maxDepth, kindMask, types, glob, plus(pathRegex, pattern),
                nameRegex, pruneIds, prune, filterIds, filter);
    }

    public M3FileWalk nameRegex(String regex) {
        return nameRegex(Pattern.compile(regex));
    }

    /** Full match over the entry name; matched in place over the name lanes. */
    public M3FileWalk nameRegex(Pattern pattern) {
        return new M3FileWalk(tree, root, minDepth, maxDepth, kindMask, types, glob, pathRegex,
                plus(nameRegex, pattern), pruneIds, prune, filterIds, filter);
    }

    /** Does not descend into directories matching {@code predicate}. */
    public M3FileWalk prune(Predicate<? super M3File> predicate) {
        Predicate<M3File> next = Objects.requireNonNull(predicate, "predicate")::test;
        return new M3FileWalk(tree, root, minDepth, maxDepth, kindMask, types, glob, pathRegex, nameRegex,
                pruneIds, prune == null ? next : prune.or(next), filterIds, filter);
    }

    /** Lane-level {@link #prune}: tested on row ids, no facade. */
    public M3FileWalk pruneIds(IntPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return new M3FileWalk(tree, root, minDepth, maxDepth, kindMask, types, glob, pathRegex, nameRegex,
                pruneIds == null ? predicate : pruneIds.or(predicate), prune, filterIds, filter);
    }

    public M3FileWalk filter(Predicate<? super M3File> predicate) {
        Predicate<M3File> next = Objects.requireNonNull(predicate, "predicate")::test;
        return new M3FileWalk(tree, root, minDepth, maxDepth, kindMask, types, glob, pathRegex, nameRegex,
                pruneIds, prune, filterIds, filter == null ? next : filter.and(next));
    }

    /** Lane-level {@link #filter}: tested on row ids, no facade. */
    public M3FileWalk filterIds(IntPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return new M3FileWalk(tree, root, minDepth, maxDepth, kindMask, types, glob, pathRegex, nameRegex,
                pruneIds, prune, filterIds == null ? predicate : filterIds.and(predicate), filter);
    }

    @Override
    public Spliterator.OfInt idSpliterator() {
        return new M3FileIdSpliterator(this, tree.rank(root), rangeEnd(), plain());
    }

    /** Pull cursor over the same entries in the same order. */
    public M3FileCursor cursor() {
        return new M3FileCursor(this, tree.rank(root), rangeEnd());
    }

    /**
     * {@code tree}-style rendering in name order (CSR slices): an entry is shown when the walk accepts it
     * or when it leads to an accepted descendant; pruned and depth-capped subtrees are not entered.
     */
    public String render() {
        StringBuilder out = new StringBuilder();
        for (String line : lines(root, probe())) out.append(line).append('\n');
        return out.toString();
    }

    /**
     * Writes this walk's directories and regular files into a zip on {@code target} (not closed), names
     * relative to the walk root; content streams through each entry's source with {@code maxBytes} and a
     * split of {@code progress} per file. Returns the entries written.
     */
    public long zipTo(OutputStream target, long maxBytes, M3FileProgress progress) throws IOException {
        return M3FileZipWriter.write(this, Objects.requireNonNull(target, "target"), maxBytes, progress);
    }

    /** {@link #zipTo(OutputStream, long, M3FileProgress)} into a new or truncated file. */
    public long zipTo(Path target, long maxBytes, M3FileProgress progress) throws IOException {
        try (OutputStream out = Files.newOutputStream(target)) {
            return zipTo(out, maxBytes, progress);
        }
    }

    private List<String> lines(int id, Probe probe) {
        List<List<String>> children = new ArrayList<>();
        if (!closed(id)) {
            for (int i = 0; i < tree.childCount(id); i++) {
                List<String> child = lines(tree.childId(id, i), probe);
                if (!child.isEmpty()) children.add(child);
            }
        }
        if (children.isEmpty() && !probe.accepts(id)) return List.of();
        List<String> out = new ArrayList<>();
        out.add(tree.name(id));
        for (int i = 0; i < children.size(); i++) {
            boolean last = i == children.size() - 1;
            List<String> child = children.get(i);
            out.add((last ? "└── " : "├── ") + child.get(0));
            for (int j = 1; j < child.size(); j++) out.add((last ? "    " : "│   ") + child.get(j));
        }
        return out;
    }

    /** Exclusive end position of this walk. */
    int rangeEnd() {
        return closed(root) ? tree.rank(root) + 1 : tree.subtreeEnd(root);
    }

    /** Unfiltered walk over a tree without tombstones: every id in range is emitted. */
    boolean plain() {
        return minDepth == 0 && maxDepth == Integer.MAX_VALUE && kindMask == M3FileKind.ALL
                && types == null && glob == null && pathRegex.length == 0 && nameRegex.length == 0 && pruneIds == null
                && prune == null && filterIds == null && filter == null && tree.tombstones() == 0;
    }

    /** True when the walk does not descend below {@code id}. */
    boolean closed(int id) {
        if (tree.subtreeEnd(id) == tree.rank(id) + 1) return false;
        if (tree.deleted(id) || tree.depth(id) - rootDepth >= maxDepth) return true;
        if (pruneIds != null && pruneIds.test(id)) return true;
        return prune != null && prune.test(tree.file(id));
    }

    /** Next position after visiting {@code position}: the first child, or past a closed subtree. */
    int advance(int position) {
        int id = tree.idAt(position);
        int end = tree.subtreeEnd(id);
        return end != position + 1 && closed(id) ? end : position + 1;
    }

    /** Moves {@code position} past the highest closed proper ancestor, so it never lies in a skipped subtree. */
    int normalize(int position) {
        int rootPosition = tree.rank(root);
        int target = position;
        for (int ancestor = tree.parent(tree.idAt(position)); ancestor >= 0 && tree.rank(ancestor) >= rootPosition;
                ancestor = tree.parent(ancestor)) {
            if (closed(ancestor)) target = tree.subtreeEnd(ancestor);
        }
        return target;
    }

    Probe probe() {
        return new Probe();
    }

    private static int requireDepth(int depth) {
        if (depth < 0) throw new IllegalArgumentException("negative depth: " + depth);
        return depth;
    }

    private static Pattern[] plus(Pattern[] patterns, Pattern next) {
        Pattern[] grown = Arrays.copyOf(patterns, patterns.length + 1);
        grown[patterns.length] = Objects.requireNonNull(next, "pattern");
        return grown;
    }

    /** Per-traversal scratch: one text buffer and one matcher per pattern, reused for every row. */
    final class Probe {
        private final StringBuilder text = new StringBuilder(64);
        private final Matcher[] path = matchers(pathRegex);
        private final Matcher[] name = matchers(nameRegex);

        boolean accepts(int id) {
            if (tree.deleted(id) || tree.depth(id) - rootDepth < minDepth) return false;
            if ((kindMask & tree.kindBit(id)) == 0 || (types != null && !types.get(tree.typeId(id)))) return false;
            if (filterIds != null && !filterIds.test(id)) return false;
            if (name.length > 0 && !matchesName(id)) return false;
            if ((path.length > 0 || glob != null) && !matchesPath(id)) return false;
            return filter == null || filter.test(tree.file(id));
        }

        private boolean matchesName(int id) {
            text.setLength(0);
            tree.appendName(id, text);
            return all(name);
        }

        private boolean matchesPath(int id) {
            text.setLength(0);
            tree.appendRelative(root, id, text);
            return all(path) && (glob == null || glob.matches(tree.root().getFileSystem().getPath(text.toString())));
        }

        private boolean all(Matcher[] matchers) {
            for (Matcher matcher : matchers) {
                if (!matcher.reset().matches()) return false;
            }
            return true;
        }

        private Matcher[] matchers(Pattern[] patterns) {
            Matcher[] matchers = new Matcher[patterns.length];
            for (int i = 0; i < patterns.length; i++) matchers[i] = patterns[i].matcher(text);
            return matchers;
        }
    }
}
