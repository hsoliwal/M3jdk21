// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Exact byte-delta facts for source files, generated class files and build inputs.
 * This is one M3JDK21-owned file identity plane, not another parser or compiler.
 * A caller must supply the complete tracked input set for each generation.
 * Semantic/ABI equivalence is intentionally not inferred from byte equality.
 */
public final class M3FileDeltaLedger {
    public static final long LIVE = 1L;
    public static final long CREATED = 1L << 1;
    public static final long MODIFIED = 1L << 2;
    public static final long REMOVED = 1L << 3;
    public static final long DELTA = CREATED | MODIFIED | REMOVED;
    private static final int MAGIC = 0x4d33444c; // M3DL
    private static final int VERSION = 1;
    private static final int HASH_BYTES = 32;
    private static final int MAX_SLOTS = 1_000_000;
    private static final Kind[] KINDS = Kind.values();

    public enum Kind { SOURCE, CLASS, BUILD_INPUT }

    private M3FileDeltaLedger() {}

    /** Immutable physical-slot view backed only by parent-owned columns. */
    public static final class Snapshot {
        private final Path root;
        private final String[] paths;
        private final byte[] kinds;
        private final long[] sizes;
        private final long[] state;
        private final byte[] hashes;
        private final int[] slotsByPath;

        private Snapshot(Path root, String[] paths, byte[] kinds, long[] sizes,
                         long[] state, byte[] hashes, int[] slotsByPath) {
            this.root = root;
            this.paths = paths;
            this.kinds = kinds;
            this.sizes = sizes;
            this.state = state;
            this.hashes = hashes;
            this.slotsByPath = slotsByPath;
        }

        public Path root() { return root; }
        public int slots() { return paths.length; }
        public String pathAt(int slot) { return paths[checked(slot)]; }
        public Kind kindAt(int slot) { return KINDS[Byte.toUnsignedInt(kinds[checked(slot)])]; }
        public int physicalSlot(Path path) {
            int at = bucket(relativeKey(root, Objects.requireNonNull(path, "path")), paths, slotsByPath);
            return slotsByPath[at] - 1;
        }
        public long sizeAt(int slot) { return sizes[checked(slot)]; }
        public long stateAt(int slot) { return state[checked(slot)]; }
        public boolean changedAt(int slot) { return (stateAt(slot) & DELTA) != 0; }
        public boolean liveAt(int slot) { return (stateAt(slot) & LIVE) != 0; }

        public byte[] digestAt(int slot) {
            int start = Math.multiplyExact(checked(slot), HASH_BYTES);
            return Arrays.copyOfRange(hashes, start, start + HASH_BYTES);
        }

        /** Physical slots, not copies of the payload or a sorted logical view. */
        public int[] changedSlots(Kind kind) {
            Objects.requireNonNull(kind, "kind");
            int count = 0;
            for (int i = 0; i < paths.length; i++) {
                if (kinds[i] == (byte) kind.ordinal() && (state[i] & DELTA) != 0) count++;
            }
            int[] slots = new int[count];
            for (int i = 0, at = 0; i < paths.length; i++) {
                if (kinds[i] == (byte) kind.ordinal() && (state[i] & DELTA) != 0) slots[at++] = i;
            }
            return slots;
        }

        private int checked(int slot) { return Objects.checkIndex(slot, paths.length); }
    }

    /**
     * Compare exact file bytes and preserve old physical slots across generations.
     * Missing tracked paths become tombstones; a re-created path reuses its slot.
     * No timestamp shortcut is allowed to certify unchanged content.
     */
    public static Snapshot scan(Path directory, Snapshot old, Collection<Path> tracked)
            throws IOException {
        Path root = checkedRoot(directory);
        Objects.requireNonNull(tracked, "tracked");
        if (old != null && !old.root.equals(root)) {
            throw new IllegalArgumentException("M3File delta root mismatch");
        }
        TreeSet<String> supplied = new TreeSet<>();
        for (Path path : tracked) supplied.add(relativeKey(root, Objects.requireNonNull(path, "path")));
        int original = old == null ? 0 : old.slots();
        int capacity = (int) Math.min(MAX_SLOTS, (long) original + supplied.size());
        String[] paths = old == null ? new String[capacity] : Arrays.copyOf(old.paths, capacity);
        byte[] kinds = old == null ? new byte[capacity] : Arrays.copyOf(old.kinds, capacity);
        long[] sizes = old == null ? new long[capacity] : Arrays.copyOf(old.sizes, capacity);
        long[] states = new long[capacity];
        byte[] hashes = old == null ? new byte[Math.multiplyExact(capacity, HASH_BYTES)]
                : Arrays.copyOf(old.hashes, Math.multiplyExact(capacity, HASH_BYTES));
        int[] lookup = newTable(capacity);
        for (int i = 0; i < original; i++) {
            lookup[bucket(paths[i], paths, lookup)] = i + 1;
            if ((old.state[i] & LIVE) != 0) states[i] = REMOVED;
        }
        int used = original;
        ByteBuffer scratch = supplied.isEmpty() ? null : ByteBuffer.allocate(64 * 1024);
        for (String key : supplied) {
            int bucket = bucket(key, paths, lookup);
            int slot = lookup[bucket] - 1;
            Path target = root.resolve(key);
            Fingerprint actual = fingerprint(root, target, scratch);
            if (actual == null) continue;
            if (slot < 0 && used == capacity) {
                throw new IOException("M3File delta slot limit exceeded");
            }
            int at = slot < 0 ? used++ : slot;
            if (slot < 0) {
                paths[at] = key;
                lookup[bucket] = at + 1;
            }
            byte kind = (byte) kindOf(key).ordinal();
            boolean matched = slot >= 0 && (old.state[at] & LIVE) != 0
                    && old.kinds[at] == kind && old.sizes[at] == actual.size
                    && hashEquals(old.hashes, at * HASH_BYTES, actual.hash);
            kinds[at] = kind;
            sizes[at] = actual.size;
            System.arraycopy(actual.hash, 0, hashes, at * HASH_BYTES, HASH_BYTES);
            states[at] = LIVE | (matched ? 0 : slot < 0 || (old.state[at] & LIVE) == 0
                    ? CREATED : MODIFIED);
        }
        return new Snapshot(root, Arrays.copyOf(paths, used), Arrays.copyOf(kinds, used),
                Arrays.copyOf(sizes, used), Arrays.copyOf(states, used),
                Arrays.copyOf(hashes, Math.multiplyExact(used, HASH_BYTES)), lookup);
    }

    public static Kind kindOf(String path) {
        Objects.requireNonNull(path, "path");
        return path.endsWith(".java") ? Kind.SOURCE
                : path.endsWith(".class") ? Kind.CLASS : Kind.BUILD_INPUT;
    }

    /** Atomically publish the next immutable build-fact generation. */
    public static void save(Path image, Snapshot snapshot) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot");
        Path output = Objects.requireNonNull(image, "image").toAbsolutePath().normalize();
        Path directory = Objects.requireNonNull(output.getParent(), "image parent");
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("image parent is not a directory");
        }
        Path temporary = Files.createTempFile(directory, ".m3file-delta-", ".tmp");
        boolean published = false;
        try {
            MessageDigest digest = sha();
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
                 DigestOutputStream signed = new DigestOutputStream(
                         java.nio.channels.Channels.newOutputStream(channel), digest);
                 DataOutputStream out = new DataOutputStream(signed)) {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.writeUTF(snapshot.root.toString());
                out.writeInt(snapshot.slots());
                for (int i = 0; i < snapshot.slots(); i++) {
                    out.writeUTF(snapshot.paths[i]);
                    out.writeByte(snapshot.kinds[i]);
                    out.writeLong(snapshot.sizes[i]);
                    out.writeLong(snapshot.state[i] & LIVE);
                    out.write(snapshot.hashes, i * HASH_BYTES, HASH_BYTES);
                }
                out.flush();
                signed.on(false);
                out.write(digest.digest());
                out.flush();
                channel.force(true);
            }
            try {
                Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                throw new IOException("atomic M3File state publication required", unsupported);
            }
            published = true;
        } finally {
            if (!published) Files.deleteIfExists(temporary);
        }
    }

    /** Verify format, exact input root, and complete SHA-256 seal before reuse. */
    public static Snapshot load(Path image, Path expectedDirectory) throws IOException {
        Path expectedRoot = checkedRoot(expectedDirectory);
        MessageDigest digest = sha();
        try (InputStream source = Files.newInputStream(image);
             DigestInputStream signed = new DigestInputStream(source, digest);
             DataInputStream in = new DataInputStream(signed)) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) {
                throw new IOException("unknown M3File delta image format");
            }
            Path root = Path.of(in.readUTF()).toAbsolutePath().normalize();
            if (!expectedRoot.equals(root)) throw new IOException("M3File delta image root mismatch");
            int count = in.readInt();
            if (count < 0 || count > MAX_SLOTS) throw new IOException("invalid M3File slot count");
            String[] paths = new String[count];
            byte[] kinds = new byte[count];
            long[] sizes = new long[count];
            long[] states = new long[count];
            byte[] hashes = new byte[Math.multiplyExact(count, HASH_BYTES)];
            int[] lookup = newTable(count);
            for (int i = 0; i < count; i++) {
                String key = in.readUTF();
                if (!key.equals(relativeKey(root, root.resolve(key)))) {
                    throw new IOException("invalid M3File delta path");
                }
                int at = bucket(key, paths, lookup);
                if (lookup[at] != 0) throw new IOException("duplicate M3File delta path");
                paths[i] = key;
                lookup[at] = i + 1;
                byte kind = in.readByte();
                if (Byte.toUnsignedInt(kind) >= KINDS.length
                        || kind != (byte) kindOf(key).ordinal()) {
                    throw new IOException("invalid M3File delta kind");
                }
                kinds[i] = kind;
                sizes[i] = in.readLong();
                states[i] = in.readLong();
                if (sizes[i] < 0 || (states[i] & ~LIVE) != 0) {
                    throw new IOException("invalid M3File delta row");
                }
                in.readFully(hashes, i * HASH_BYTES, HASH_BYTES);
            }
            signed.on(false);
            byte[] seal = new byte[HASH_BYTES];
            in.readFully(seal);
            if (in.read() != -1 || !MessageDigest.isEqual(seal, digest.digest())) {
                throw new IOException("M3File delta image checksum mismatch");
            }
            return new Snapshot(root, paths, kinds, sizes, states, hashes, lookup);
        }
    }

    /** Compare the fixed 32-byte digest in its parent array without a temporary row copy. */
    private static boolean hashEquals(byte[] lane, int offset, byte[] candidate) {
        for (int i = 0; i < HASH_BYTES; i++) {
            if (lane[offset + i] != candidate[i]) return false;
        }
        return true;
    }

    /** Open addressing over original physical row indexes; no per-file retained Entry nodes. */
    private static int[] newTable(int count) {
        int length = 16;
        while (length <= count * 2) length <<= 1;
        return new int[length];
    }

    private static int bucket(String key, String[] paths, int[] table) {
        int hash = key.hashCode();
        int at = (hash ^ (hash >>> 16)) & (table.length - 1);
        while (table[at] != 0 && !paths[table[at] - 1].equals(key)) {
            at = (at + 1) & (table.length - 1);
        }
        return at;
    }

    private record Fingerprint(long size, byte[] hash) {}

    private static Fingerprint fingerprint(Path root, Path file, ByteBuffer buffer) throws IOException {
        Path cursor = root;
        for (Path element : root.relativize(file)) {
            cursor = cursor.resolve(element);
            if (Files.isSymbolicLink(cursor)) throw new IOException("symlink in tracked M3File path");
        }
        BasicFileAttributes before;
        try {
            before = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException missing) {
            return null;
        }
        if (!before.isRegularFile()) throw new IOException("tracked input is not a regular file");
        MessageDigest digest = sha();
        long total = 0;
        buffer.clear();
        int consecutiveZeroReads = 0;
        try (SeekableByteChannel channel = M3File.of(file).openReadChannel()) {
            while (true) {
                int read = channel.read(buffer);
                if (read < 0) break;
                if (read == 0) {
                    if (++consecutiveZeroReads > 16) throw new IOException("M3File read made no progress");
                    continue;
                }
                consecutiveZeroReads = 0;
                total = Math.addExact(total, read);
                digest.update(buffer.flip());
                buffer.clear();
            }
        }
        BasicFileAttributes after = Files.readAttributes(
                file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile() || before.size() != total || after.size() != total
                || !before.lastModifiedTime().equals(after.lastModifiedTime())
                || !Objects.equals(before.fileKey(), after.fileKey())) {
            throw new IOException("tracked M3File changed while being hashed");
        }
        return new Fingerprint(total, digest.digest());
    }

    private static Path checkedRoot(Path directory) throws IOException {
        Path root = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(root)) throw new IOException("invalid M3File root directory");
        return root;
    }

    private static String relativeKey(Path root, Path requested) {
        Path path = requested.isAbsolute() ? requested.normalize() : root.resolve(requested).normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new IllegalArgumentException("tracked M3File escapes its root");
        }
        String relative = root.relativize(path).toString().replace('\\', '/');
        if (relative.isEmpty() || relative.indexOf('\0') >= 0 || relative.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("invalid M3File tracked path");
        }
        return relative;
    }

    private static MessageDigest sha() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
