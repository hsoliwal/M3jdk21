// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Owner of one rooted directory tree held in structure-of-arrays lanes ({@code m3/file/DESIGN.md}).
 *
 * <p>Row ids are dense and allocated in pre-order (the scan order of {@code Files.walk}); {@code 0}
 * is the root. Lanes: {@code parent}, {@code depth}, {@code nameId} into the tree's
 * {@link M3FileNames} interner (each distinct segment stored once), {@code typeId} into a second
 * interner of lower-case extensions (id {@code 0} = none or directory), CSR child slices,
 * {@code subtreeEnd}, {@code size}, {@code mtime}, and one {@link BitSet} per {@link M3FileFlag}.
 * Nothing else of the file system is retained (DOOP). {@link M3File} facades are thin
 * {@code (tree, id)} handles from {@link #file(int)}, kept in a bounded {@link M3FileResidency}.</p>
 *
 * <p>Invariants: {@code parent[id] < id}; {@code depth[id] == depth[parent[id]] + 1}; every child
 * slice is sorted by {@link #compareNames}; {@code childIds} holds every non-root id exactly once;
 * the subtree of {@code id} is exactly the positions {@code [rank(id), subtreeEnd(id))}; {@link M3FileFlag#DELETED} rows
 * keep their lanes and are skipped by {@link #ids()} and walks.</p>
 */
public final class M3FileTree implements Closeable {
    static final int DEFAULT_RESIDENCY = 4096;
    private static final int INITIAL_ROWS = 64;
    private static final M3FileTree DETACHED = new M3FileTree(Path.of(""), 1, M3FileSource.FILESYSTEM);

    private final Path root;
    private final M3FileSource source;
    private final String separator;
    private final BitSet[] flags = new BitSet[M3FileFlag.VALUES.length];
    private final M3FileResidency residency;
    private final M3FileNames names = new M3FileNames(INITIAL_ROWS);
    private final M3FileNames types = new M3FileNames(8);
    private final M3FileHashes hashes = new M3FileHashes(INITIAL_ROWS);
    private int[] parent = new int[INITIAL_ROWS];
    private short[] depth = new short[INITIAL_ROWS];
    private int[] nameId = new int[INITIAL_ROWS];
    private int[] typeId = new int[INITIAL_ROWS];
    private int[] childStart = new int[INITIAL_ROWS];
    private int[] childCount = new int[INITIAL_ROWS];
    private int[] childIds = new int[INITIAL_ROWS];
    private int[] subtreeEnd = new int[INITIAL_ROWS];
    private int[] order = new int[INITIAL_ROWS];
    private int[] rank = new int[INITIAL_ROWS];
    private long[] size = new long[INITIAL_ROWS];
    private long[] mtime = new long[INITIAL_ROWS];
    private byte[] error = new byte[INITIAL_ROWS];
    private int rows;
    private int generation;
    private M3FileScanSpec spec = M3FileScanSpec.EAGER;
    private int childTop;

    private M3FileTree(Path root, int residencyCapacity, M3FileSource source) {
        this.root = Objects.requireNonNull(root, "root");
        this.source = Objects.requireNonNull(source, "source");
        this.separator = source.separator(root);
        this.residency = new M3FileResidency(residencyCapacity);
        for (int i = 0; i < flags.length; i++) flags[i] = new BitSet();
        types.intern("");
    }

    /**
     * Eager scan of {@code root}: a directory tree with {@code NOFOLLOW_LINKS} and unbounded depth, or,
     * when {@code root} is a zip/jar (by signature or extension), the archive's central directory with
     * nested archives as sub-trees and no extraction. Close the tree to release an archive.
     */
    public static M3FileTree open(Path root) throws IOException {
        return open(root, DEFAULT_RESIDENCY);
    }

    /** As {@link #open(Path)} with an explicit facade residency capacity (power of two). */
    public static M3FileTree open(Path root, int residencyCapacity) throws IOException {
        return open(root, new M3FileScanSpec(Integer.MAX_VALUE, residencyCapacity));
    }

    /**
     * As {@link #open(Path)} under {@code spec}: a directory scan stops {@code spec.maxDepth()} levels
     * down and {@link #scanChildren(int)} expands further on demand. Archives are always read whole.
     */
    public static M3FileTree open(Path root, M3FileScanSpec spec) throws IOException {
        if (isArchive(root)) return M3FileZipScanner.scan(root, spec.residencyCapacity());
        M3FileTree tree = new M3FileTree(root, spec.residencyCapacity(), M3FileSource.FILESYSTEM);
        tree.spec = spec;
        return M3FileTreeScanner.scan(tree, -1, spec.maxDepth());
    }

    /**
     * Scans the children of an unscanned directory row ({@code spec.maxDepth()} levels) and splices
     * them into pre-order after it. Ids stay stable; positions after the directory shift, and walks or
     * cursors open during the call fail fast. Returns the rows added; {@code 0} for scanned rows.
     */
    public int scanChildren(int id) throws IOException {
        checkId(id);
        if (!flag(M3FileFlag.DIR, id) || flag(M3FileFlag.SCANNED, id)) return 0;
        if (source != M3FileSource.FILESYSTEM) throw new IllegalStateException("only file-system trees scan lazily");
        int first = rows;
        M3FileTreeScanner.scan(this, id, Math.max(1, spec.maxDepth()));
        splice(id, first);
        generation++;
        return rows - first;
    }

    /** Shared empty tree behind {@link M3File#of(Path)}; it has no rows. */
    public static M3FileTree detached() {
        return DETACHED;
    }

    static M3FileTree create(Path root, int residencyCapacity, M3FileSource source) {
        return new M3FileTree(root, residencyCapacity, source);
    }

    /** Regular file starting with a zip signature, or named like an archive. */
    static boolean isArchive(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return false;
        if (M3FileZipScanner.ARCHIVE_TYPES.contains(extension(M3FileTreeScanner.nameOf(path)))) return true;
        try (InputStream in = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            byte[] magic = in.readNBytes(4);
            return magic.length == 4 && magic[0] == 'P' && magic[1] == 'K'
                    && ((magic[2] == 3 && magic[3] == 4) || (magic[2] == 5 && magic[3] == 6));
        }
    }

    public Path root() {
        return root;
    }

    /** Where rows and bytes come from (file system, archive). */
    public M3FileSource source() {
        return source;
    }

    /** Releases the source (an open archive); file-system trees hold nothing. */
    @Override
    public void close() throws IOException {
        source.close();
    }

    public M3File rootFile() {
        return file(0);
    }

    /** Facade for {@code id}; identical instance while resident, equal instance after eviction. */
    public M3File file(int id) {
        checkId(id);
        return residency.file(this, id);
    }

    /** Walk over the whole tree. */
    public M3FileWalk walk() {
        return walk(0);
    }

    /** Walk over the subtree of {@code id} (inclusive). */
    public M3FileWalk walk(int id) {
        checkId(id);
        return new M3FileWalk(this, id);
    }

    /** Rows allocated, tombstones included. */
    public int rows() {
        return rows;
    }

    public int liveCount() {
        return rows - tombstones();
    }

    /** Live ids in pre-order. */
    public IntStream ids() {
        BitSet deleted = flags[M3FileFlag.DELETED.ordinal()];
        return IntStream.range(0, rows).map(position -> order[position]).filter(id -> !deleted.get(id));
    }

    /** Pre-order position of {@code id}; equal to {@code id} until a lazy expansion splices rows. */
    public int rank(int id) {
        checkId(id);
        return rank[id];
    }

    /** Row id at pre-order {@code position}. */
    public int idAt(int position) {
        Objects.checkIndex(position, rows);
        return order[position];
    }

    /** Incremented by every structural change (lazy expansion); traversals fail fast across it. */
    public int generation() {
        return generation;
    }

    public M3FileScanSpec spec() {
        return spec;
    }

    public int residentCount() {
        return residency.residentCount();
    }

    /** Interned name segments of this tree. */
    public M3FileNames names() {
        return names;
    }

    /** Interned lower-case extensions of this tree; id {@code 0} is the empty type. */
    public M3FileNames types() {
        return types;
    }

    public int parent(int id) {
        checkId(id);
        return parent[id];
    }

    public int depth(int id) {
        checkId(id);
        return depth[id];
    }

    /** Exclusive end position of the pre-order subtree of {@code id}: positions {@code [rank(id), subtreeEnd(id))}. */
    public int subtreeEnd(int id) {
        checkId(id);
        return subtreeEnd[id];
    }

    public int childCount(int id) {
        checkId(id);
        return childCount[id];
    }

    /** Allocation-free child access: {@code index} in {@code [0, childCount(id))}. */
    public int childId(int id, int index) {
        checkId(id);
        Objects.checkIndex(index, childCount[id]);
        return childIds[childStart[id] + index];
    }

    public IntStream childIds(int id) {
        checkId(id);
        int start = childStart[id];
        return IntStream.range(start, start + childCount[id]).map(i -> childIds[i]);
    }

    public long size(int id) {
        checkId(id);
        return size[id];
    }

    public long mtime(int id) {
        checkId(id);
        return mtime[id];
    }

    public boolean flag(M3FileFlag flag, int id) {
        checkId(id);
        return flags[flag.ordinal()].get(id);
    }

    public M3FileKind kind(int id) {
        checkId(id);
        return M3FileKind.VALUES[Integer.numberOfTrailingZeros(kindBit(id))];
    }

    /** Interned name id of row {@code id} in {@link #names()}. */
    public int nameId(int id) {
        checkId(id);
        return nameId[id];
    }

    /** Interned type id of row {@code id} in {@link #types()}; {@code 0} for none and for directories. */
    public int typeId(int id) {
        checkId(id);
        return typeId[id];
    }

    /** Cold: lower-case extension of row {@code id}, {@code ""} for none. */
    public String type(int id) {
        return types.name(typeId(id));
    }

    /** Why the last observation of {@code id} failed; {@link M3FileError#NONE} unless {@link M3FileFlag#FAILED}. */
    public M3FileError error(int id) {
        checkId(id);
        return M3FileError.VALUES[error[id]];
    }

    /** CRC32C lane; meaningful when {@link M3FileFlag#HASHED}. */
    public long crc(int id) {
        checkId(id);
        return hashes.crc(id);
    }

    /** SHA-256 word {@code word} (0..3, big-endian, #1567 {@code Fingerprint} order); meaningful when HASHED. */
    public long shaWord(int id, int word) {
        checkId(id);
        Objects.checkIndex(word, 4);
        return hashes.sha(id, word);
    }

    /** Token-stream hash lane; meaningful when {@link M3FileFlag#TOKEN_HASHED}. */
    public long tokenHash(int id) {
        checkId(id);
        return hashes.tokens(id);
    }

    /** Structure hash lane; meaningful when {@link M3FileFlag#STRUCT_HASHED}. */
    public long structHash(int id) {
        checkId(id);
        return hashes.structure(id);
    }

    /** CRC32 recorded by an archive's directory for this entry, {@code -1} when the source has none. */
    public long entryCrc(int id) {
        checkId(id);
        return hashes.entryCrc(id);
    }

    /** Hash of the scan-time {@code BasicFileAttributes.fileKey()}, {@code 0} when the provider has none. */
    public int fileKeyHash(int id) {
        checkId(id);
        return hashes.fileKey(id);
    }

    /**
     * Fills the hash lanes of the regular files in {@code entries} with one observation pass each;
     * progress units are bytes (one {@link M3FileProgress#split} per file). Returns the rows hashed;
     * rows whose observation fails get {@link M3FileFlag#FAILED}. Cancellation propagates.
     */
    public int hash(M3FileTraversal entries, long maxBytes, M3FileProgress progress, M3FileChangeProvider provider)
            throws InterruptedIOException {
        if (entries.tree() != this) throw new IllegalArgumentException("traversal of another tree");
        int[] files = entries.ids().filter(id -> flag(M3FileFlag.REGULAR, id)).toArray();
        long total = 0;
        for (int id : files) total += Math.max(0, size[id]);
        progress.begin("hash " + root, total);
        int hashed = 0;
        for (int id : files) {
            if (hashes.observe(this, id, maxBytes, progress.split(Math.max(0, size[id])), provider)) hashed++;
        }
        progress.done();
        return hashed;
    }

    /**
     * Bounded parallel variant of {@link #hash(M3FileTraversal, long, M3FileProgress, M3FileChangeProvider)}.
     * At most {@code parallelism} observations are outstanding, including completed results waiting
     * for an earlier row. Each worker reads its file once; the caller publishes hash/failure lanes
     * in traversal order. No list proportional to the traversal is retained by this overload.
     *
     * <p>The source must support independent concurrent reads, and {@code provider.structure} must
     * support concurrent calls and return independent shadows. Monitor callbacks are serialized;
     * workers receive private split monitors. The total is unknown ({@code 0}) until complete.
     * With {@code parallelism == 1}, the original serial implementation (including its total) is used.</p>
     *
     * <p>The caller must exclusively own the tree during this call: do not expand, close, hash, or
     * otherwise mutate it from callbacks or other threads. Cancellation/interruption or an unchecked
     * callback failure stops submission and joins all workers before returning; already committed rows
     * remain committed. Blocking sources must cooperate with interruption or eventually return.
     * Ordinary per-file I/O failures mark that row failed and allow later rows to proceed.</p>
     *
     * @throws IllegalArgumentException if the traversal belongs to another tree, the byte limit is
     *         negative, or parallelism is not positive
     */
    public int hash(M3FileTraversal entries, long maxBytes, M3FileProgress progress,
            M3FileChangeProvider provider, int parallelism) throws InterruptedIOException {
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(provider, "provider");
        if (entries.tree() != this) throw new IllegalArgumentException("traversal of another tree");
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        if (parallelism < 1) throw new IllegalArgumentException("nonpositive parallelism");
        progress.checkCanceled();
        return parallelism == 1 ? hash(entries, maxBytes, progress, provider)
                : hashes.observeParallel(this, entries, maxBytes, progress, provider, parallelism);
    }

    public int nameLength(int id) {
        return names.length(nameId(id));
    }

    public char nameCharAt(int id, int index) {
        return names.charAt(nameId(id), index);
    }

    /** Cold: materialises the name of {@code id}. */
    public String name(int id) {
        return names.name(nameId(id));
    }

    /** UTF-16 code unit order, then length; locale-free and allocation-free. */
    public int compareNames(int a, int b) {
        return names.compare(nameId(a), nameId(b));
    }

    /** Cold: {@code root} for id {@code 0}, otherwise {@code root.resolve(names joined by the separator)}. */
    public Path path(int id) {
        checkId(id);
        if (id == 0) return root;
        StringBuilder relative = new StringBuilder();
        appendRelative(0, id, relative);
        return root.resolve(relative.toString());
    }

    /** Lower-case extension after the last dot of a name; {@code ""} for none, dot-names and trailing dots. */
    static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot <= 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Kind bit of {@code id} ({@link M3FileKind#bit()}); a link is never a directory or file. */
    int kindBit(int id) {
        if (flags[M3FileFlag.LINK.ordinal()].get(id)) return M3FileKind.SYMLINK.bit();
        if (flags[M3FileFlag.DIR.ordinal()].get(id)) return M3FileKind.DIRECTORY.bit();
        if (flags[M3FileFlag.REGULAR.ordinal()].get(id)) return M3FileKind.FILE.bit();
        return M3FileKind.OTHER.bit();
    }

    boolean deleted(int id) {
        return flags[M3FileFlag.DELETED.ordinal()].get(id);
    }

    int tombstones() {
        return flags[M3FileFlag.DELETED.ordinal()].cardinality();
    }

    /** Appends the name chars of {@code id}; no intermediate String. */
    void appendName(int id, StringBuilder out) {
        names.appendTo(nameId[id], out);
    }

    /** Appends the path of {@code id} relative to its ancestor-or-self {@code from}; empty when equal. */
    void appendRelative(int from, int id, StringBuilder out) {
        if (id == from) return;
        if (parent[id] != from) {
            appendRelative(from, parent[id], out);
            out.append(separator);
        }
        appendName(id, out);
    }

    M3FileHashes hashes() {
        return hashes;
    }

    /** Observes row {@code id} into the hash lanes; false when not regular or failed. */
    boolean hashRow(int id, long maxBytes, M3FileProgress progress, M3FileChangeProvider provider)
            throws InterruptedIOException {
        checkId(id);
        return hashes.observe(this, id, maxBytes, progress, provider);
    }

    /** Gate closed: row {@code id} inherits the content observation of {@code from}'s row {@code fromId}. */
    void carryHashes(int id, M3FileTree from, int fromId) {
        hashes.carry(this, id, from, fromId);
    }

    /** Records a failed observation of {@code id}: {@link M3FileFlag#FAILED} and the error lane. */
    void fail(int id, IOException failure) {
        setFlag(M3FileFlag.FAILED, id, true);
        error[id] = (byte) M3FileError.of(failure).ordinal();
    }

    void clearFailure(int id) {
        setFlag(M3FileFlag.FAILED, id, false);
        error[id] = 0;
    }

    void setFlag(M3FileFlag flag, int id, boolean value) {
        flags[flag.ordinal()].set(id, value);
    }

    /** Scanner entry: appends one row in pre-order and returns its id. */
    int append(int parentId, String name, M3FileKind kind, boolean hidden, long bytes, long modified) {
        ensureRows(rows + 1);
        int id = rows++;
        parent[id] = parentId;
        depth[id] = (short) (parentId < 0 ? 0 : depth[parentId] + 1);
        subtreeEnd[id] = id + 1;
        order[id] = id;
        rank[id] = id;
        size[id] = bytes;
        mtime[id] = modified;
        nameId[id] = names.intern(name);
        typeId[id] = kind == M3FileKind.DIRECTORY ? 0 : types.intern(extension(name));
        setFlag(M3FileFlag.DIR, id, kind == M3FileKind.DIRECTORY);
        setFlag(M3FileFlag.REGULAR, id, kind == M3FileKind.FILE);
        setFlag(M3FileFlag.LINK, id, kind == M3FileKind.SYMLINK);
        setFlag(M3FileFlag.HIDDEN, id, hidden);
        return id;
    }

    /** Scanner entry: writes the sorted CSR slice {@code ids[from, to)} of {@code dirId} and closes its subtree. */
    void closeDirectory(int dirId, int[] ids, int from, int to) {
        int count = to - from;
        if (childTop + count > childIds.length) {
            childIds = Arrays.copyOf(childIds, Math.max(childIds.length * 2, childTop + count));
        }
        childStart[dirId] = childTop;
        childCount[dirId] = count;
        System.arraycopy(ids, from, childIds, childTop, count);
        childTop += count;
        subtreeEnd[dirId] = rows;
        setFlag(M3FileFlag.SCANNED, dirId, true);
    }

    /**
     * Moves rows {@code [first, rows)}, built in pre-order after every existing position, to the positions
     * right after {@code dirId}, and shifts the subtree ends that cover or follow it. O(rows).
     */
    private void splice(int dirId, int first) {
        int added = rows - first;
        int at = rank[dirId] + 1;
        for (int id = 0; id < first; id++) {
            if (subtreeEnd[id] >= at) subtreeEnd[id] += added;
        }
        subtreeEnd[dirId] = at + added;
        for (int id = first; id < rows; id++) subtreeEnd[id] += at - first;
        int[] moved = Arrays.copyOfRange(order, first, rows);
        System.arraycopy(order, at, order, at + added, first - at);
        System.arraycopy(moved, 0, order, at, added);
        for (int position = at; position < rows; position++) rank[order[position]] = position;
    }

    private void ensureRows(int needed) {
        if (needed <= parent.length) return;
        int capacity = Math.max(parent.length * 2, needed);
        parent = Arrays.copyOf(parent, capacity);
        depth = Arrays.copyOf(depth, capacity);
        nameId = Arrays.copyOf(nameId, capacity);
        typeId = Arrays.copyOf(typeId, capacity);
        childStart = Arrays.copyOf(childStart, capacity);
        childCount = Arrays.copyOf(childCount, capacity);
        subtreeEnd = Arrays.copyOf(subtreeEnd, capacity);
        order = Arrays.copyOf(order, capacity);
        rank = Arrays.copyOf(rank, capacity);
        size = Arrays.copyOf(size, capacity);
        mtime = Arrays.copyOf(mtime, capacity);
        error = Arrays.copyOf(error, capacity);
        hashes.grow(capacity);
    }

    private void checkId(int id) {
        Objects.checkIndex(id, rows);
    }
}
