// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Owner-scoped, append-only interning of immutable bytes, UTF-16 atoms, structural
 * tuples and acyclic graphs. A handle is meaningful ONLY in its originating store.
 * No Java object, mutable collection, class, class loader or JNI pointer is interned.
 *
 * <p>All entries are held in primitive arrays and segmented primitive arenas. Insertion
 * order is retained by the portable archive; handles are not global content IDs.
 * Freeze before publishing or archiving a space. This is an explicit Java core,
 * not transparent HotSpot/JDK object interning or an OS-wide dictionary.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/main/java/com/synexia/common/intern/M3InternSpace.java blob 0c06c4dfab78; recipe m3/intern/recipe/intern-space-20261106/relocate.py v4
public final class M3InternSpace {
    /** Explicit constructor: the exported module package lint (missing-explicit-ctor) refuses the default one. */
    public M3InternSpace() { }
    private static final int MAGIC = 0x4d334953; // M3IS
    private static final int VERSION = 1;
    private static final int TYPE = 0x4d335459; // M3TY
    private static final int PATH = 0x4d335054; // M3PT
    private static final int INTS = 0x4d33494e; // M3IN
    private static final int MAX_READ_ENTRY = 1 << 26; // bounded untrusted archive read
    private static final int MAX_READ_COUNT = 1 << 24;

    private final ByteStore atoms = new ByteStore();
    private final ByteStore blobs = new ByteStore();
    private final TupleStore shapes = new TupleStore();
    private final GraphStore graphs = new GraphStore();
    private volatile boolean frozen;

    public ByteStore atoms() { return atoms; }
    public ByteStore blobs() { return blobs; }
    public TupleStore shapes() { return shapes; }
    public GraphStore graphs() { return graphs; }

    /** Store Java UTF-16 code units exactly, including unpaired surrogates. */
    public long internString(String value) {
        Objects.requireNonNull(value, "value");
        byte[] units = new byte[Math.multiplyExact(value.length(), 2)];
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            units[2 * i] = (byte) (c >>> 8);
            units[2 * i + 1] = (byte) c;
        }
        return atoms.intern(units);
    }

    public String string(long atomId) {
        byte[] units = atoms.copy(atomId);
        if ((units.length & 1) != 0) {
            throw new IllegalStateException("not a UTF-16 atom");
        }
        char[] text = new char[units.length / 2];
        for (int i = 0; i < text.length; i++) {
            text[i] = (char) (((units[2 * i] & 255) << 8) | (units[2 * i + 1] & 255));
        }
        return new String(text);
    }

    /**
     * Class-loader scope must identify the actual resolution authority, not a name
     * or class-loader hashCode. A scope of zero is a caller-defined boot context.
     * Type arguments must be previously interned TYPE handles in this space.
     */
    public long internType(long loaderScope, String descriptor, long... typeArguments) {
        if (loaderScope < 0) throw new IllegalArgumentException("negative loader scope");
        Objects.requireNonNull(descriptor, "descriptor");
        long[] args = Objects.requireNonNull(typeArguments, "typeArguments").clone();
        for (long type : args) {
            if (shapes.kind(type) != TYPE) throw new IllegalArgumentException("not a type handle");
        }
        long[] members = new long[args.length + 2];
        members[0] = loaderScope;
        members[1] = internString(descriptor);
        System.arraycopy(args, 0, members, 2, args.length);
        return shapes.intern(TYPE, members);
    }

    /** Lexical path only: no filesystem normalization, I/O, symlink or case folding. */
    public long internPath(long filesystemScope, String... segments) {
        if (filesystemScope < 0) throw new IllegalArgumentException("negative filesystem scope");
        String[] copy = Objects.requireNonNull(segments, "segments").clone();
        long[] members = new long[copy.length + 1];
        members[0] = filesystemScope;
        for (int i = 0; i < copy.length; i++) {
            members[i + 1] = internString(Objects.requireNonNull(copy[i], "segment"));
        }
        return shapes.intern(PATH, members);
    }

    /** Snapshot a mutable int[] into deduplicated frozen backing bytes. */
    public long internFrozenInts(int[] values) {
        int[] ints = Objects.requireNonNull(values, "values").clone();
        byte[] bytes = new byte[Math.multiplyExact(ints.length, 4)];
        for (int i = 0; i < ints.length; i++) {
            int v = ints[i];
            int at = i * 4;
            bytes[at] = (byte) (v >>> 24);
            bytes[at + 1] = (byte) (v >>> 16);
            bytes[at + 2] = (byte) (v >>> 8);
            bytes[at + 3] = (byte) v;
        }
        long blob = blobs.intern(bytes);
        return shapes.intern(INTS, blob, ints.length);
    }

    /** Returns an independent writable copy, never the shared backing. */
    public int[] frozenInts(long shapeId) {
        if (shapes.kind(shapeId) != INTS) throw new IllegalArgumentException("not frozen ints");
        long[] members = shapes.members(shapeId);
        if (members.length != 2 || members[1] < 0 || members[1] > Integer.MAX_VALUE / 4) {
            throw new IllegalStateException("malformed int shape");
        }
        byte[] bytes = blobs.copy(members[0]);
        if (bytes.length != members[1] * 4) throw new IllegalStateException("malformed int blob");
        int[] result = new int[(int) members[1]];
        for (int i = 0; i < result.length; i++) {
            int at = i * 4;
            result[i] = ((bytes[at] & 255) << 24) | ((bytes[at + 1] & 255) << 16)
                    | ((bytes[at + 2] & 255) << 8) | (bytes[at + 3] & 255);
        }
        return result;
    }

    /** Freezes all four stores. Immutable existing handles remain readable. */
    public synchronized void freeze() {
        graphs.freeze();
        shapes.freeze();
        atoms.freeze();
        blobs.freeze();
        frozen = true;
    }

    public boolean isFrozen() { return frozen; }

    /** Deterministic insertion-order archive; requires a frozen, stable snapshot. */
    public synchronized void writeTo(DataOutput output) throws IOException {
        Objects.requireNonNull(output, "output");
        if (!frozen) throw new IllegalStateException("freeze before archiving");
        output.writeInt(MAGIC);
        output.writeInt(VERSION);
        atoms.writeTo(output);
        blobs.writeTo(output);
        shapes.writeTo(output);
        graphs.writeTo(output);
    }

    /** Strict replay; rejects truncation, duplicates, out-of-order DAG edges and bad sizes. */
    public static M3InternSpace readFrom(DataInput input) throws IOException {
        Objects.requireNonNull(input, "input");
        if (input.readInt() != MAGIC || input.readInt() != VERSION) {
            throw new IOException("invalid M3InternSpace header");
        }
        M3InternSpace space = new M3InternSpace();
        space.atoms.readFrom(input);
        space.blobs.readFrom(input);
        space.shapes.readFrom(input);
        space.graphs.readFrom(input);
        space.freeze();
        return space;
    }

    private static long hash(long seed, long value) {
        long h = seed ^ value;
        h *= 0x100000001b3L;
        h ^= h >>> 32;
        return h;
    }

    private static int bucket(long h, int capacity) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (int) h & (capacity - 1);
    }

    private static int grow(int old) {
        if (old > (1 << 29)) throw new IllegalStateException("intern table capacity exhausted");
        return old << 1;
    }

    /** One primitive byte arena per store; segmented into 64-KiB pages. */
    private static final class ByteArena {
        private static final int SHIFT = 16;
        private static final int SIZE = 1 << SHIFT;
        private static final int MASK = SIZE - 1;
        private byte[][] pages = new byte[4][];
        private long length;

        long add(byte[] bytes) {
            long start = length;
            long end = Math.addExact(start, bytes.length);
            if ((end >>> SHIFT) > Integer.MAX_VALUE - 1L) {
                throw new IllegalStateException("byte arena page limit");
            }
            int from = 0;
            while (from < bytes.length) {
                int page = (int) (length >>> SHIFT);
                if (page >= pages.length) pages = Arrays.copyOf(pages, grow(pages.length));
                if (pages[page] == null) pages[page] = new byte[SIZE];
                int inPage = (int) (length & MASK);
                int n = Math.min(bytes.length - from, SIZE - inPage);
                System.arraycopy(bytes, from, pages[page], inPage, n);
                length += n;
                from += n;
            }
            return start;
        }

        boolean matches(long start, byte[] value) {
            int checked = 0;
            while (checked < value.length) {
                long position = start + checked;
                int page = (int) (position >>> SHIFT);
                int from = (int) (position & MASK);
                int count = Math.min(value.length - checked, SIZE - from);
                if (Arrays.mismatch(pages[page], from, from + count,
                        value, checked, checked + count) >= 0) return false;
                checked += count;
            }
            return true;
        }

        byte[] copy(long start, int len) {
            byte[] result = new byte[len];
            int copied = 0;
            while (copied < len) {
                long pos = start + copied;
                int inPage = (int) (pos & MASK);
                int n = Math.min(len - copied, SIZE - inPage);
                System.arraycopy(pages[(int) (pos >>> SHIFT)], inPage, result, copied, n);
                copied += n;
            }
            return result;
        }
        long bytes() { return length; }
    }

    /** Collision-verified bytes with primitive slots and one contiguous logical arena. */
    public static final class ByteStore {
        /** Explicit constructor: the exported module package lint (missing-explicit-ctor) refuses the default one. */
        public ByteStore() { }
        private final ByteArena arena = new ByteArena();
        private long[] starts = new long[16];
        private long[] hashes = new long[16];
        private int[] lengths = new int[16];
        private int[] links = new int[16];
        private int[] buckets = new int[32];
        private int count;
        private boolean frozen;

        public synchronized long intern(byte[] bytes) {
            // Freeze the caller-owned bytes before hashing and materializing.
            // Otherwise a concurrent caller write can associate a hash with
            // different stored bytes and create duplicate canonical handles.
            byte[] input = Objects.requireNonNull(bytes, "bytes").clone();
            if (input.length > MAX_READ_ENTRY) throw new IllegalArgumentException("atom too large");
            long h = hash(0xcbf29ce484222325L, input.length);
            for (byte b : input) h = hash(h, b & 255L);
            int bucket = bucket(h, buckets.length);
            for (int p = buckets[bucket]; p != 0; p = links[p]) {
                if (hashes[p] == h && lengths[p] == input.length && arena.matches(starts[p], input)) {
                    return p;
                }
            }
            if (frozen) throw new IllegalStateException("store frozen");
            if (count == MAX_READ_COUNT) throw new IllegalStateException("too many atoms");
            if (count + 1 >= starts.length) resize();
            int id = ++count;
            starts[id] = arena.add(input);
            lengths[id] = input.length;
            hashes[id] = h;
            links[id] = buckets[bucket];
            buckets[bucket] = id;
            if (count > buckets.length * 3 / 4) rehash(grow(buckets.length));
            return id;
        }

        public synchronized byte[] copy(long id) {
            int i = index(id, count);
            return arena.copy(starts[i], lengths[i]);
        }
        public synchronized int length(long id) { return lengths[index(id, count)]; }
        public synchronized int size() { return count; }
        public synchronized long storedBytes() { return arena.bytes(); }
        public synchronized void freeze() { frozen = true; }
        private void resize() {
            int n = grow(starts.length);
            starts = Arrays.copyOf(starts, n);
            lengths = Arrays.copyOf(lengths, n);
            hashes = Arrays.copyOf(hashes, n);
            links = Arrays.copyOf(links, n);
        }
        private void rehash(int n) {
            buckets = new int[n];
            for (int i = 1; i <= count; i++) {
                int b = bucket(hashes[i], n);
                links[i] = buckets[b];
                buckets[b] = i;
            }
        }
        private synchronized void writeTo(DataOutput output) throws IOException {
            output.writeInt(count);
            for (int i = 1; i <= count; i++) {
                output.writeInt(lengths[i]);
                output.write(arena.copy(starts[i], lengths[i]));
            }
        }
        private void readFrom(DataInput input) throws IOException {
            int n = readCount(input);
            for (int i = 1; i <= n; i++) {
                int size = input.readInt();
                if (size < 0 || size > MAX_READ_ENTRY) throw new IOException("invalid byte length");
                byte[] bytes = new byte[size];
                input.readFully(bytes);
                if (intern(bytes) != i) throw new IOException("duplicate byte entry");
            }
        }
    }

    private static final class LongArena {
        private static final int SHIFT = 13;
        private static final int SIZE = 1 << SHIFT;
        private static final int MASK = SIZE - 1;
        private long[][] pages = new long[4][];
        private long length;
        long add(long[] members) {
            long start = length;
            long end = Math.addExact(start, members.length);
            if ((end >>> SHIFT) > Integer.MAX_VALUE - 1L) throw new IllegalStateException("tuple arena limit");
            int from = 0;
            while (from < members.length) {
                int page = (int) (length >>> SHIFT);
                if (page >= pages.length) pages = Arrays.copyOf(pages, grow(pages.length));
                if (pages[page] == null) pages[page] = new long[SIZE];
                int inPage = (int) (length & MASK);
                int n = Math.min(members.length - from, SIZE - inPage);
                System.arraycopy(members, from, pages[page], inPage, n);
                length += n;
                from += n;
            }
            return start;
        }
        boolean matches(long start, long[] value) {
            int checked = 0;
            while (checked < value.length) {
                long position = start + checked;
                int page = (int) (position >>> SHIFT);
                int from = (int) (position & MASK);
                int count = Math.min(value.length - checked, SIZE - from);
                if (Arrays.mismatch(pages[page], from, from + count,
                        value, checked, checked + count) >= 0) return false;
                checked += count;
            }
            return true;
        }
        // Read a single primitive cell without allocating a tuple snapshot.
        // The owning TupleStore holds its monitor while invoking this.
        long get(long position) {
            return pages[(int) (position >>> SHIFT)][(int) (position & MASK)];
        }
        long[] copy(long start, int size) {
            long[] result = new long[size];
            int copied = 0;
            while (copied < size) {
                long pos = start + copied;
                int at = (int) (pos & MASK);
                int n = Math.min(size - copied, SIZE - at);
                System.arraycopy(pages[(int) (pos >>> SHIFT)], at, result, copied, n);
                copied += n;
            }
            return result;
        }
    }

    /** Kind-separated, collision-verified hash-consed tuples of primitive long cells. */
    public static final class TupleStore {
        /** Explicit constructor: the exported module package lint (missing-explicit-ctor) refuses the default one. */
        public TupleStore() { }
        private final LongArena arena = new LongArena();
        private long[] starts = new long[16];
        private long[] hashes = new long[16];
        private int[] lengths = new int[16];
        private int[] kinds = new int[16];
        private int[] links = new int[16];
        private int[] buckets = new int[32];
        private int count;
        private boolean frozen;

        public synchronized long intern(int kind, long... members) {
            // Keep hashing, collision comparison and storage on one snapshot.
            long[] seq = Objects.requireNonNull(members, "members").clone();
            if (seq.length > MAX_READ_ENTRY / 8) throw new IllegalArgumentException("tuple too large");
            long h = hash(hash(0xcbf29ce484222325L, kind), seq.length);
            for (long v : seq) h = hash(h, v);
            int b = bucket(h, buckets.length);
            for (int p = buckets[b]; p != 0; p = links[p]) {
                if (hashes[p] == h && kinds[p] == kind && lengths[p] == seq.length
                        && arena.matches(starts[p], seq)) return p;
            }
            if (frozen) throw new IllegalStateException("store frozen");
            if (count == MAX_READ_COUNT) throw new IllegalStateException("too many tuples");
            if (count + 1 >= starts.length) resize();
            int id = ++count;
            starts[id] = arena.add(seq);
            hashes[id] = h;
            kinds[id] = kind;
            lengths[id] = seq.length;
            links[id] = buckets[b];
            buckets[b] = id;
            if (count > buckets.length * 3 / 4) rehash(grow(buckets.length));
            return id;
        }
        public synchronized long[] members(long id) {
            int i = index(id, count);
            return arena.copy(starts[i], lengths[i]);
        }
        /**
         * Internal scalar view for a known immutable shape. Validate the exact
         * kind, width and owner before returning a primitive field. Unlike the
         * public members() API, this never creates a per-entry array; the
         * synchronized store guards growth and publication of the arena.
         */
        synchronized long scopedMember(long id, int kind, int width, long owner, int at) {
            int i = index(id, count);
            if (kinds[i] != kind || lengths[i] != width
                    || arena.get(starts[i]) != owner) {
                throw new IllegalArgumentException("shape belongs to another scope");
            }
            Objects.checkIndex(at, width);
            return arena.get(starts[i] + at);
        }
        public synchronized int kind(long id) { return kinds[index(id, count)]; }
        public synchronized int size() { return count; }
        public synchronized void freeze() { frozen = true; }
        private void resize() {
            int n = grow(starts.length);
            starts = Arrays.copyOf(starts, n);
            hashes = Arrays.copyOf(hashes, n);
            lengths = Arrays.copyOf(lengths, n);
            kinds = Arrays.copyOf(kinds, n);
            links = Arrays.copyOf(links, n);
        }
        private void rehash(int n) {
            buckets = new int[n];
            for (int i = 1; i <= count; i++) {
                int b = bucket(hashes[i], n);
                links[i] = buckets[b];
                buckets[b] = i;
            }
        }
        private synchronized void writeTo(DataOutput output) throws IOException {
            output.writeInt(count);
            for (int i = 1; i <= count; i++) {
                output.writeInt(kinds[i]);
                output.writeInt(lengths[i]);
                for (long v : arena.copy(starts[i], lengths[i])) output.writeLong(v);
            }
        }
        private void readFrom(DataInput input) throws IOException {
            int count = readCount(input);
            for (int i = 1; i <= count; i++) {
                int kind = input.readInt();
                int len = input.readInt();
                if (len < 0 || len > MAX_READ_ENTRY / 8) throw new IOException("invalid tuple size");
                long[] seq = new long[len];
                for (int j = 0; j < len; j++) seq[j] = input.readLong();
                if (intern(kind, seq) != i) throw new IOException("duplicate tuple entry");
            }
        }
    }

    /** Strict acyclic node factory: every child must be an existing local node. */
    public static final class GraphStore {
        /** Explicit constructor: the exported module package lint (missing-explicit-ctor) refuses the default one. */
        public GraphStore() { }
        private final TupleStore nodes = new TupleStore();
        public synchronized long intern(int opcode, long payload, long... children) {
            long[] copy = Objects.requireNonNull(children, "children").clone();
            for (long id : copy) {
                if (id <= 0 || id > nodes.size()) throw new IllegalArgumentException("unknown DAG child");
            }
            long[] record = new long[copy.length + 1];
            record[0] = payload;
            System.arraycopy(copy, 0, record, 1, copy.length);
            return nodes.intern(opcode, record);
        }
        public synchronized int opcode(long id) { return nodes.kind(id); }
        public synchronized long payload(long id) { return nodes.members(id)[0]; }
        public synchronized long[] children(long id) {
            long[] record = nodes.members(id);
            return Arrays.copyOfRange(record, 1, record.length);
        }
        public synchronized int size() { return nodes.size(); }
        public synchronized void freeze() { nodes.freeze(); }
        private synchronized void writeTo(DataOutput output) throws IOException { nodes.writeTo(output); }
        private void readFrom(DataInput input) throws IOException {
            int n = readCount(input);
            for (int i = 1; i <= n; i++) {
                int opcode = input.readInt();
                int length = input.readInt();
                if (length < 1 || length > MAX_READ_ENTRY / 8) {
                    throw new IOException("invalid graph node length");
                }
                long payload = input.readLong();
                long[] children = new long[length - 1];
                for (int j = 0; j < children.length; j++) children[j] = input.readLong();
                try {
                    if (intern(opcode, payload, children) != i) throw new IOException("duplicate graph node");
                } catch (IllegalArgumentException invalid) {
                    throw new IOException("invalid graph child", invalid);
                }
            }
        }
    }

    private static int readCount(DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > MAX_READ_COUNT) throw new IOException("invalid intern entry count");
        return n;
    }

    private static int index(long id, int count) {
        if (id <= 0 || id > count) throw new IndexOutOfBoundsException("intern handle " + id);
        return (int) id;
    }
}
