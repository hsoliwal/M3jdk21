// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

/**
 * Differential dump driver of the intern-space relocation (recipe intern-space-20261106, gate 3). It is compiled once
 * against the relocated package and once, by differential.sh, against the donor classes (only its package line
 * differs), replays the same seeded workload through the public API of the family and prints one line per
 * observable result: byte chunks and long tuples interned with duplicates, near-duplicates and page-boundary lengths
 * (0, 1, page - 1, page, page + 1, three pages; 64-KiB byte pages and 8,192-long pages, chunks straddling page
 * boundaries inside the contiguous arena), graph nodes and their edges, typed shapes, path-trie inserts and lookups
 * with shared prefixes, the core archive (sha256, read-back, re-archive) and the guard container with its refusal
 * verdicts (bad magic, wrong version, truncation, digest mismatch, budgets, forward DAG edge, trailing bytes). The
 * dumps of both sides must be byte-identical, under the default JIT and under -Xint. No line names a class of the
 * family, so the TYPE map needs no output normalization.
 *
 * <p>usage: java InternSpaceDump [operations, default 100000]</p>
 */
public final class InternSpaceDump {
    /** Seed of every workload (named so a rerun replays the same inputs). */
    static final long SEED = 0x2026_1106_494E_544EL;
    static final int BYTE_PAGE = 1 << 16;
    static final int LONG_PAGE = 1 << 13;
    static final int[] BYTE_BOUNDARIES = {0, 1, BYTE_PAGE - 1, BYTE_PAGE, BYTE_PAGE + 1, 3 * BYTE_PAGE};
    static final int[] LONG_BOUNDARIES = {0, 1, LONG_PAGE - 1, LONG_PAGE, LONG_PAGE + 1, 3 * LONG_PAGE};
    static final int BOUNDARY_EVERY = 4_000;
    static final int GRAPH_NODES = 20_000;
    static final int SHAPE_ROUNDS = 2_000;
    static final int LOOKUPS = 10_000;
    static final int GUARD_BUDGET = 1 << 27;
    static final int GUARD_RECORDS = 1 << 24;
    static final int FRAME_MAGIC = 0x4d334947;
    static final int CORE_MAGIC = 0x4d334953;
    static final int FRAME_HEADER = 44;

    private final PrintStream out;
    private final Random random = new Random(SEED);
    private final InternSpace space = new InternSpace();
    private final int operations;

    private InternSpaceDump(PrintStream out, int operations) {
        this.out = out;
        this.operations = operations;
    }

    public static void main(String[] args) throws Exception {
        int operations = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.out), 1 << 16),
                false, StandardCharsets.US_ASCII);
        new InternSpaceDump(out, operations).run();
        out.flush();
    }

    private void run() throws Exception {
        long bytes = byteChunks();
        long tuples = longTuples();
        long nodes = graph();
        long shapes = shapes();
        InternPathTrie trie = new InternPathTrie(space, 7);
        List<String[]> paths = new ArrayList<>();
        long[] handles = paths(trie, paths);
        space.freeze();
        long found = lookups(trie, paths, handles);
        String core = coreArchive();
        String guard = guardArchive();
        refusals();
        out.println("TOTAL ops=" + operations + " bytes=" + bytes + " tuples=" + tuples + " nodes=" + nodes
                + " shapes=" + shapes + " paths=" + handles.length + " found=" + found + " core=" + core
                + " guard=" + guard);
    }

    private long byteChunks() {
        InternSpace.ByteStore store = space.blobs();
        List<byte[]> seen = new ArrayList<>();
        byte[] lastBoundary = new byte[0];
        for (int i = 0; i < operations; i++) {
            byte[] value;
            int mode = random.nextInt(100);
            if (i % BOUNDARY_EVERY == 0) {
                value = new byte[BYTE_BOUNDARIES[(i / BOUNDARY_EVERY) % BYTE_BOUNDARIES.length]];
                random.nextBytes(value);
                lastBoundary = value;
            } else if (i % BOUNDARY_EVERY == BOUNDARY_EVERY / 2) {
                value = lastBoundary.clone();
                if (value.length > 0 && (i / BOUNDARY_EVERY) % 2 == 1) value[value.length - 1] ^= 1;
            } else if (mode < 25 && !seen.isEmpty()) {
                value = seen.get(random.nextInt(seen.size())).clone();
            } else if (mode < 30 && !seen.isEmpty()) {
                value = seen.get(random.nextInt(seen.size())).clone();
                if (value.length > 0) value[random.nextInt(value.length)] ^= (byte) (1 + random.nextInt(255));
            } else {
                value = new byte[random.nextInt(97)];
                random.nextBytes(value);
            }
            long before = store.storedBytes();
            int size = store.size();
            long id = store.intern(value);
            byte[] copy = store.copy(id);
            if (id > size) seen.add(value);
            out.println("B " + i + " " + id + " " + value.length + " " + (id <= size ? "dup" : "new") + " "
                    + Arrays.equals(copy, value) + " " + Arrays.hashCode(copy) + " " + before % BYTE_PAGE);
        }
        out.println("BYTES size=" + store.size() + " stored=" + store.storedBytes());
        return store.size();
    }

    private long longTuples() {
        InternSpace.TupleStore store = space.shapes();
        List<long[]> seen = new ArrayList<>();
        List<Integer> kinds = new ArrayList<>();
        long[] lastBoundary = new long[0];
        for (int i = 0; i < operations; i++) {
            long[] value;
            int kind = 0x100 + random.nextInt(4);
            int mode = random.nextInt(100);
            if (i % BOUNDARY_EVERY == 0) {
                value = randomLongs(LONG_BOUNDARIES[(i / BOUNDARY_EVERY) % LONG_BOUNDARIES.length]);
                lastBoundary = value;
            } else if (i % BOUNDARY_EVERY == BOUNDARY_EVERY / 2) {
                value = lastBoundary.clone();
                if (value.length > 0 && (i / BOUNDARY_EVERY) % 2 == 1) value[value.length - 1] ^= 1L;
            } else if (mode < 25 && !seen.isEmpty()) {
                int pick = random.nextInt(seen.size());
                value = seen.get(pick).clone();
                kind = mode < 5 ? kind : kinds.get(pick);
            } else if (mode < 30 && !seen.isEmpty()) {
                int pick = random.nextInt(seen.size());
                value = seen.get(pick).clone();
                kind = kinds.get(pick);
                if (value.length > 0) value[random.nextInt(value.length)] += 1L + random.nextInt(1000);
            } else {
                value = randomLongs(random.nextInt(17));
            }
            int size = store.size();
            long id = store.intern(kind, value);
            long[] copy = store.members(id);
            if (id > size) {
                seen.add(value);
                kinds.add(kind);
            }
            out.println("T " + i + " " + id + " " + store.kind(id) + " " + value.length + " "
                    + (id <= size ? "dup" : "new") + " " + Arrays.equals(copy, value) + " " + Arrays.hashCode(copy));
        }
        out.println("TUPLES size=" + store.size());
        return store.size();
    }

    private long[] randomLongs(int length) {
        long[] value = new long[length];
        for (int j = 0; j < length; j++) value[j] = random.nextInt(4) == 0 ? random.nextInt(16) : random.nextLong();
        return value;
    }

    private long graph() {
        InternSpace.GraphStore graph = space.graphs();
        List<long[]> records = new ArrayList<>();
        for (int i = 0; i < GRAPH_NODES; i++) {
            int size = graph.size();
            int opcode;
            long payload;
            long[] children;
            if (random.nextInt(5) == 0 && !records.isEmpty()) {
                long[] record = records.get(random.nextInt(records.size()));
                opcode = (int) record[0];
                payload = record[1];
                children = Arrays.copyOfRange(record, 2, record.length);
            } else {
                opcode = random.nextInt(8);
                payload = random.nextInt(3) == 0 ? random.nextInt(50) : random.nextLong();
                children = new long[size == 0 ? 0 : random.nextInt(4)];
                for (int c = 0; c < children.length; c++) children[c] = 1 + random.nextInt(size);
            }
            final long[] edges = i % 997 == 0 ? Arrays.copyOf(children, children.length + 1) : children;
            if (i % 997 == 0) edges[edges.length - 1] = i % 2 == 0 ? 0 : size + 1L;
            String verdict = verdict(() -> {
                long id = graph.intern(opcode, payload, edges);
                return id + " " + graph.opcode(id) + " " + graph.payload(id) + " "
                        + Arrays.equals(graph.children(id), edges);
            });
            if (verdict.charAt(0) != '!' && graph.size() > size) {
                long[] record = new long[edges.length + 2];
                record[0] = opcode;
                record[1] = payload;
                System.arraycopy(edges, 0, record, 2, edges.length);
                records.add(record);
            }
            out.println("G " + i + " " + edges.length + " " + verdict);
        }
        out.println("GRAPH size=" + graph.size());
        return graph.size();
    }

    private long shapes() {
        InternShapes shapes = new InternShapes(space);
        String[] names = {"a", "b", "value", "", "\uD800", "été", "x/y", "."};
        for (int i = 0; i < SHAPE_ROUNDS; i++) {
            long scope = random.nextInt(4);
            long string = space.internString(names[random.nextInt(names.length)] + random.nextInt(50));
            long type = space.internType(scope, "L" + names[random.nextInt(names.length)] + random.nextInt(20) + ";");
            long generic = space.internType(scope, "Ljava/util/List;", type);
            long method = shapes.internMethod(scope, "m" + random.nextInt(30), type, generic, type);
            long record = shapes.internRecord(scope, type, new String[] {"k", "v"}, new long[] {type, generic});
            long[] longs = randomLongs(random.nextInt(12));
            long frozen = shapes.internFrozenLongs(longs);
            BitSet mask = new BitSet();
            for (int b = random.nextInt(8); b > 0; b--) mask.set(random.nextInt(300));
            long maskId = shapes.internBitMask(mask);
            long tokens = shapes.internTokens(scope, names[random.nextInt(names.length)], "t" + random.nextInt(9));
            int[] ints = new int[random.nextInt(9)];
            for (int k = 0; k < ints.length; k++) ints[k] = random.nextInt();
            long intsId = space.internFrozenInts(ints);
            long path = space.internPath(scope, names[random.nextInt(names.length)], "p" + random.nextInt(9));
            String foreign = verdict(() -> String.valueOf(shapes.internMethod(scope + 1, "f", type)));
            out.println("S " + i + " " + string + " " + type + " " + generic + " " + method + " " + record + " "
                    + frozen + " " + Arrays.equals(shapes.copyFrozenLongs(frozen), longs) + " " + maskId + " "
                    + mask.equals(shapes.copyBitMask(maskId)) + " " + tokens + " "
                    + String.join("|", shapes.copyTokens(tokens)).hashCode() + " " + intsId + " "
                    + Arrays.equals(space.frozenInts(intsId), ints) + " " + path + " " + foreign);
        }
        out.println("SHAPES tuples=" + space.shapes().size() + " atoms=" + space.atoms().size());
        return space.shapes().size();
    }

    private long[] paths(InternPathTrie trie, List<String[]> paths) {
        String[] vocabulary = new String[400];
        String[] special = {"", "/", ".", "..", "a/b", "\uD800", "Case", "case", "é", "src"};
        for (int v = 0; v < vocabulary.length; v++) {
            vocabulary[v] = v < special.length ? special[v] : "seg" + v;
        }
        long[] handles = new long[operations];
        for (int i = 0; i < operations; i++) {
            String[] base = paths.isEmpty() ? new String[0] : paths.get(random.nextInt(paths.size()));
            int keep = base.length == 0 ? 0 : random.nextInt(base.length + 1);
            int extra = keep >= 12 ? 0 : 1 + random.nextInt(3);
            String[] segments = Arrays.copyOf(base, keep + extra);
            for (int e = keep; e < segments.length; e++) segments[e] = vocabulary[random.nextInt(vocabulary.length)];
            long handle = trie.intern(segments);
            handles[i] = handle;
            paths.add(segments);
            out.println("P " + i + " " + handle + " " + trie.depth(handle) + " " + trie.parent(handle) + " "
                    + Arrays.equals(trie.segments(handle), segments) + " " + trie.isPrefix(trie.root(), handle));
        }
        out.println("PATHS tuples=" + space.shapes().size() + " atoms=" + space.atoms().size());
        return handles;
    }

    private long lookups(InternPathTrie trie, List<String[]> paths, long[] handles) {
        long found = 0;
        for (int i = 0; i < operations; i++) {
            int a = random.nextInt(handles.length);
            int b = random.nextInt(handles.length);
            String last = trie.depth(handles[a]) == 0 ? "-" : String.valueOf(trie.lastSegment(handles[a]).hashCode());
            out.println("L " + i + " " + trie.isPrefix(handles[a], handles[b]) + " "
                    + trie.commonPrefixLength(handles[a], handles[b]) + " " + last);
        }
        InternPathTrie foreign = new InternPathTrie(space, 7);
        for (int i = 0; i < LOOKUPS; i++) {
            String[] segments = paths.get(random.nextInt(paths.size())).clone();
            if (i % 2 == 1 && segments.length > 0) segments[random.nextInt(segments.length)] = "absent" + i;
            long hit = foreign.findExisting(segments);
            found += hit == 0 ? 0 : 1;
            out.println("F " + i + " " + hit);
        }
        out.println("FOREIGN " + verdict(() -> String.valueOf(new InternPathTrie(space, 9).depth(handles[0]))));
        out.println("FROZEN " + verdict(() -> String.valueOf(trie.intern("never", "interned"))));
        return found;
    }

    private String coreArchive() throws IOException {
        byte[] archive = archive(space);
        InternSpace back = InternSpace.readFrom(new DataInputStream(new ByteArrayInputStream(archive)));
        boolean same = Arrays.equals(archive, archive(back));
        String digest = sha256(archive);
        out.println("A core bytes=" + archive.length + " sha256=" + digest + " readback=" + same
                + " atoms=" + back.atoms().size() + " blobs=" + back.blobs().size() + " shapes=" + back.shapes().size()
                + " graphs=" + back.graphs().size() + " frozen=" + back.isFrozen());
        return digest;
    }

    private String guardArchive() throws IOException {
        byte[] framed = InternArchiveGuard.write(space, GUARD_BUDGET);
        InternSpace back = InternArchiveGuard.read(framed, GUARD_BUDGET, GUARD_RECORDS);
        // the read-back space re-archives to the framed payload (the frame header is checked by the refusal lane)
        boolean same = Arrays.equals(archive(back), Arrays.copyOfRange(framed, FRAME_HEADER, framed.length));
        String digest = sha256(framed);
        out.println("A guard bytes=" + framed.length + " sha256=" + digest + " readback=" + same);
        return digest;
    }

    private void refusals() throws IOException {
        InternSpace small = new InternSpace();
        long leaf = small.graphs().intern(1, small.blobs().intern(new byte[] {1, 2, 3}));
        small.graphs().intern(2, small.internString("root"), leaf);
        small.shapes().intern(0x100, 5L, 6L);
        refuse("guard-unfrozen", () -> InternArchiveGuard.write(small, 1 << 20).length);
        refuse("core-unfrozen", () -> archive(small).length);
        small.freeze();
        byte[] core = archive(small);
        byte[] framed = InternArchiveGuard.write(small, 1 << 20);
        out.println("A small core=" + sha256(core) + " guard=" + sha256(framed));
        refuse("core-ok", () -> archive(readCore(core)).length);
        refuse("core-bad-magic", () -> readCore(withInt(core, 0, CORE_MAGIC ^ 1)).atoms().size());
        refuse("core-wrong-version", () -> readCore(withInt(core, 4, 2)).atoms().size());
        refuse("core-truncated", () -> readCore(Arrays.copyOf(core, core.length - 5)).atoms().size());
        refuse("core-duplicate", () -> readCore(duplicateAtom()).atoms().size());
        refuse("guard-ok", () -> InternArchiveGuard.read(framed, 1 << 20, 100).blobs().size());
        refuse("guard-bad-magic", () -> InternArchiveGuard.read(withInt(framed, 0, FRAME_MAGIC ^ 1), 1 << 20, 100).blobs().size());
        refuse("guard-wrong-version", () -> InternArchiveGuard.read(withInt(framed, 4, 2), 1 << 20, 100).blobs().size());
        refuse("guard-truncated-header", () -> InternArchiveGuard.read(Arrays.copyOf(framed, 20), 1 << 20, 100).blobs().size());
        refuse("guard-truncated-payload", () -> InternArchiveGuard.read(Arrays.copyOf(framed, framed.length - 1), 1 << 20, 100).blobs().size());
        refuse("guard-digest", () -> InternArchiveGuard.read(flipLast(framed), 1 << 20, 100).blobs().size());
        refuse("guard-inner-bad-magic", () -> InternArchiveGuard.read(frame(withInt(core, 0, CORE_MAGIC ^ 1)), 1 << 20, 100).blobs().size());
        refuse("guard-inner-wrong-version", () -> InternArchiveGuard.read(frame(withInt(core, 4, 2)), 1 << 20, 100).blobs().size());
        refuse("guard-inner-truncated", () -> InternArchiveGuard.read(frame(Arrays.copyOf(core, core.length - 5)), 1 << 20, 100).blobs().size());
        refuse("guard-inner-trailing", () -> InternArchiveGuard.read(frame(Arrays.copyOf(core, core.length + 1)), 1 << 20, 100).blobs().size());
        refuse("guard-record-budget", () -> InternArchiveGuard.read(framed, 1 << 20, 1).blobs().size());
        refuse("guard-read-byte-budget", () -> InternArchiveGuard.read(framed, 16, 100).blobs().size());
        refuse("guard-write-byte-budget", () -> InternArchiveGuard.write(small, 16).length);
        refuse("guard-budget-argument", () -> InternArchiveGuard.read(framed, 15, 100).blobs().size());
        refuse("guard-records-argument", () -> InternArchiveGuard.read(framed, 1 << 20, -1).blobs().size());
        refuse("guard-forward-edge", () -> InternArchiveGuard.read(frame(forwardEdge()), 1 << 20, 100).blobs().size());
    }

    private static InternSpace readCore(byte[] archive) throws IOException {
        return InternSpace.readFrom(new DataInputStream(new ByteArrayInputStream(archive)));
    }

    private static byte[] archive(InternSpace target) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        target.writeTo(new DataOutputStream(bytes));
        return bytes.toByteArray();
    }

    private static byte[] withInt(byte[] data, int at, int value) {
        byte[] copy = data.clone();
        ByteBuffer.wrap(copy).putInt(at, value);
        return copy;
    }

    private static byte[] flipLast(byte[] data) {
        byte[] copy = data.clone();
        copy[copy.length - 1] ^= 1;
        return copy;
    }

    private static byte[] frame(byte[] payload) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(FRAME_MAGIC);
        out.writeInt(1);
        out.writeInt(payload.length);
        out.write(digest(payload));
        out.write(payload);
        out.flush();
        if (bytes.size() != FRAME_HEADER + payload.length) throw new IllegalStateException("frame size");
        return bytes.toByteArray();
    }

    private static byte[] duplicateAtom() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(CORE_MAGIC);
        out.writeInt(1);
        out.writeInt(2);
        out.writeInt(1);
        out.writeByte(9);
        out.writeInt(1);
        out.writeByte(9);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        return bytes.toByteArray();
    }

    private static byte[] forwardEdge() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(CORE_MAGIC);
        out.writeInt(1);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(2);
        out.writeInt(4);
        out.writeInt(1);
        out.writeLong(11);
        out.writeInt(5);
        out.writeInt(2);
        out.writeLong(12);
        out.writeLong(2);
        return bytes.toByteArray();
    }

    private interface Action {
        Object call() throws Exception;
    }

    private static String verdict(Action action) {
        try {
            return String.valueOf(action.call());
        } catch (Exception | AssertionError error) {
            Throwable cause = error.getCause();
            return "!" + error.getClass().getSimpleName() + ": " + error.getMessage()
                    + (cause == null ? "" : " <- " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }

    private void refuse(String name, Action action) {
        out.println("R " + name + " " + verdict(action));
    }

    private static byte[] digest(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String sha256(byte[] data) {
        return HexFormat.of().formatHex(digest(data));
    }
}
