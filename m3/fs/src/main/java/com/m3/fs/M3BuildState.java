// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Versioned, immutable, array/offset/mask build facts owned by M3JDK21's M3FS.
 * The producer (M3File and M3AST/javac) supplies verified facts; this class does not
 * read source files, parse code or independently infer ABI compatibility.
 * Dependency offsets contain the row indices on which each row depends.
 */
public final class M3BuildState {
    public static final byte SOURCE = 1, CLASS = 2, BUILD_INPUT = 3, DEPENDENCY = 4, RESOURCE = 5;
    public static final int HASH_BYTES = 32;
    private static final int MAGIC = 0x4d334253; // M3BS
    private static final int VERSION = 1;
    private static final int MAX_KEY_BYTES = 65_535;

    private final String[] paths;
    private final byte[] roles;
    private final long[] lengths;
    private final byte[] contentHashes;
    private final byte[] abiHashes;
    private final long[] abiKnown;
    private final int[] offsets;
    private final int[] dependencies;
    private final byte[] contextHash;

    /** All input arrays are defensively copied. A zero ABI-known bit means NOT PROVEN. */
    public M3BuildState(String[] paths, byte[] roles, long[] lengths, byte[] contentHashes,
            byte[] abiHashes, long[] abiKnown, int[] offsets, int[] dependencies, byte[] contextHash) {
        this.paths = Objects.requireNonNull(paths, "paths").clone();
        this.roles = Objects.requireNonNull(roles, "roles").clone();
        this.lengths = Objects.requireNonNull(lengths, "lengths").clone();
        this.contentHashes = Objects.requireNonNull(contentHashes, "contentHashes").clone();
        this.abiHashes = Objects.requireNonNull(abiHashes, "abiHashes").clone();
        this.abiKnown = Objects.requireNonNull(abiKnown, "abiKnown").clone();
        this.offsets = Objects.requireNonNull(offsets, "offsets").clone();
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies").clone();
        this.contextHash = Objects.requireNonNull(contextHash, "contextHash").clone();
        validate();
    }

    private void validate() {
        int n = paths.length;
        if (roles.length != n || lengths.length != n ||
                contentHashes.length != Math.multiplyExact(n, HASH_BYTES) ||
                abiHashes.length != Math.multiplyExact(n, HASH_BYTES) ||
                abiKnown.length != (n + 63L) / 64L || offsets.length != n + 1 ||
                contextHash.length != HASH_BYTES || offsets[0] != 0 || offsets[n] != dependencies.length) {
            throw new IllegalArgumentException("build state column geometry");
        }
        if (n % 64 != 0 && n != 0 && (abiKnown[abiKnown.length - 1] >>> (n & 63)) != 0) {
            throw new IllegalArgumentException("noncanonical ABI mask tail");
        }
        for (int i = 0; i < n; i++) {
            String key = paths[i];
            if (key == null || key.isEmpty() || key.indexOf('\0') >= 0 ||
                    key.startsWith("/") || key.indexOf('\\') >= 0 ||
                    Arrays.asList(key.split("/", -1)).contains("..") ||
                    encodedKey(key).length > MAX_KEY_BYTES) {
                throw new IllegalArgumentException("invalid workspace-relative path");
            }
            if (roles[i] < SOURCE || roles[i] > RESOURCE || lengths[i] < 0L ||
                    (i > 0 && compare(paths[i - 1], roles[i - 1], key, roles[i]) >= 0)) {
                throw new IllegalArgumentException("unsorted or invalid build identity");
            }
            if (offsets[i] < 0 || offsets[i] > offsets[i + 1] || offsets[i + 1] > dependencies.length) {
                throw new IllegalArgumentException("invalid dependency offsets");
            }
            if (!abiAt(i) && !zeroHash(abiHashes, i)) {
                throw new IllegalArgumentException("unknown ABI must have zero lane");
            }
            for (int p = offsets[i]; p < offsets[i + 1]; p++) {
                int d = dependencies[p];
                if (d < 0 || d >= n || (p > offsets[i] && d <= dependencies[p - 1])) {
                    throw new IllegalArgumentException("unsorted or invalid dependency edge");
                }
            }
        }
    }

    private static byte[] encodedKey(String key) {
        try {
            ByteBuffer buffer = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(key));
            byte[] result = new byte[buffer.remaining()]; buffer.get(result); return result;
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("non-UTF8 build path", invalid);
        }
    }

    private static int compare(String a, byte roleA, String b, byte roleB) {
        int difference = a.compareTo(b);
        return difference != 0 ? difference : Byte.compare(roleA, roleB);
    }

    public int size() { return paths.length; }
    public String path(int row) { return paths[Objects.checkIndex(row, size())]; }
    public byte role(int row) { return roles[Objects.checkIndex(row, size())]; }
    public long length(int row) { return lengths[Objects.checkIndex(row, size())]; }
    public boolean hasAbi(int row) { Objects.checkIndex(row, size()); return abiAt(row); }
    private boolean abiAt(int row) { return (abiKnown[row >>> 6] & (1L << (row & 63))) != 0L; }
    public byte[] contextSha256() { return contextHash.clone(); }
    public int dependencyCount() { return dependencies.length; }
    public int dependency(int row, int ordinal) {
        Objects.checkIndex(row, size());
        return dependencies[Objects.checkIndex(offsets[row] + ordinal, offsets[row + 1])];
    }
    public int dependencyCount(int row) { Objects.checkIndex(row, size()); return offsets[row + 1] - offsets[row]; }
    public int find(String path, byte role) {
        int low = 0, high = size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1, diff = compare(paths[mid], roles[mid], path, role);
            if (diff == 0) return mid;
            if (diff < 0) low = mid + 1; else high = mid - 1;
        }
        return -1;
    }

    private static boolean zeroHash(byte[] values, int row) {
        int base = row * HASH_BYTES;
        for (int i = 0; i < HASH_BYTES; i++) if (values[base + i] != 0) return false;
        return true;
    }

    private static boolean sameHash(byte[] first, int a, byte[] second, int b) {
        return Arrays.equals(first, a * HASH_BYTES, (a + 1) * HASH_BYTES,
                second, b * HASH_BYTES, (b + 1) * HASH_BYTES);
    }

    private boolean sameDependencies(int row, M3BuildState prior, int earlier) {
        int count = dependencyCount(row);
        if (count != prior.dependencyCount(earlier)) return false;
        for (int i = 0; i < count; i++) {
            int current = dependencies[offsets[row] + i];
            int old = prior.dependencies[prior.offsets[earlier] + i];
            if (compare(paths[current], roles[current], prior.paths[old], prior.roles[old]) != 0) return false;
        }
        return true;
    }

    /**
     * Conservative reverse-dependency closure. Equal, *compiler-proven* ABI may
     * suppress downstream invalidation, but never skips the edited node itself.
     * A removed input or changed build context invalidates the entire inventory:
     * a removal can alter resolution even if a current graph edge has disappeared.
     * Results are candidates only; Maven/Gradle still own task execution decisions.
     */
    public Plan changedFrom(M3BuildState previous) {
        int n = size(), words = (int) ((n + 63L) >>> 6);
        long[] changed = new long[words], affected = new long[words];
        if (previous == null || !Arrays.equals(contextHash, previous.contextHash) || removedFrom(previous)) {
            Arrays.fill(changed, -1L);
            if (words != 0 && (n & 63) != 0) changed[words - 1] = (1L << (n & 63)) - 1L;
            return new Plan(changed, changed.clone(), true, n);
        }
        int[] reverseCounts = new int[n];
        for (int d : dependencies) reverseCounts[d]++;
        int[] reverseOffsets = new int[n + 1];
        for (int i = 0; i < n; i++) reverseOffsets[i + 1] = Math.addExact(reverseOffsets[i], reverseCounts[i]);
        int[] reverse = new int[dependencies.length], cursor = reverseOffsets.clone();
        for (int owner = 0; owner < n; owner++) {
            for (int p = offsets[owner]; p < offsets[owner + 1]; p++) {
                reverse[cursor[dependencies[p]]++] = owner;
            }
        }
        int[] queue = new int[n]; int head = 0, tail = 0;
        for (int row = 0; row < n; row++) {
            int old = previous.find(paths[row], roles[row]);
            boolean bytesChanged = old < 0 || lengths[row] != previous.lengths[old] ||
                    !sameHash(contentHashes, row, previous.contentHashes, old);
            boolean abiChanged = old < 0 || abiAt(row) != previous.abiAt(old) ||
                    (abiAt(row) && !sameHash(abiHashes, row, previous.abiHashes, old));
            boolean edgesChanged = old < 0 || !sameDependencies(row, previous, old);
            if (bytesChanged || abiChanged || edgesChanged) {
                set(changed, row); set(affected, row);
                // ABI evidence only applies to source/class changes with the same
                // proven ABI, and an unchanged dependency graph.
                boolean canStop = old >= 0 && !edgesChanged && !abiChanged &&
                        (roles[row] == SOURCE || roles[row] == CLASS) && abiAt(row);
                if (!canStop) queue[tail++] = row;
            }
        }
        while (head < tail) {
            int current = queue[head++];
            for (int p = reverseOffsets[current]; p < reverseOffsets[current + 1]; p++) {
                int dependent = reverse[p];
                if (!bit(affected, dependent)) {
                    set(affected, dependent);
                    queue[tail++] = dependent;
                }
            }
        }
        return new Plan(changed, affected, false, n);
    }

    private boolean removedFrom(M3BuildState earlier) {
        int current = 0;
        for (int i = 0; i < earlier.size(); i++) {
            while (current < size() && compare(paths[current], roles[current],
                    earlier.paths[i], earlier.roles[i]) < 0) current++;
            if (current == size() || compare(paths[current], roles[current],
                    earlier.paths[i], earlier.roles[i]) != 0) return true;
        }
        return false;
    }

    private static byte[] digest(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static void set(long[] words, int row) { words[row >>> 6] |= 1L << (row & 63); }
    private static boolean bit(long[] words, int row) { return (words[row >>> 6] & (1L << (row & 63))) != 0L; }

    public record Plan(long[] changedMask, long[] affectedMask, boolean fullRebuild, int size) {
        public Plan {
            changedMask = changedMask.clone(); affectedMask = affectedMask.clone();
            if (size < 0 || changedMask.length != (size + 63L) / 64L ||
                    affectedMask.length != changedMask.length) throw new IllegalArgumentException("plan mask geometry");
        }
        @Override public long[] changedMask() { return changedMask.clone(); }
        @Override public long[] affectedMask() { return affectedMask.clone(); }
        public boolean changed(int row) { Objects.checkIndex(row, size); return bit(changedMask, row); }
        public boolean affected(int row) { Objects.checkIndex(row, size); return bit(affectedMask, row); }
        public long affectedCount() { long count = 0L; for (long word : affectedMask) count += Long.bitCount(word); return count; }
    }

    /** Checksum-sealed, versioned serialization; the caller supplies an allocation budget. */
    public byte[] encode(int maxBytes) throws IOException {
        if (maxBytes < HASH_BYTES + 48) throw new IllegalArgumentException("maxBytes");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC); out.writeInt(VERSION);
            out.writeInt(size()); out.writeInt(dependencies.length);
            out.write(contextHash);
            for (int i = 0; i < size(); i++) {
                byte[] name = encodedKey(paths[i]);
                out.writeInt(name.length); out.write(name);
                out.writeByte(roles[i]); out.writeLong(lengths[i]);
                out.write(contentHashes, i * HASH_BYTES, HASH_BYTES);
                out.write(abiHashes, i * HASH_BYTES, HASH_BYTES);
            }
            for (long word : abiKnown) out.writeLong(word);
            for (int offset : offsets) out.writeInt(offset);
            for (int dependency : dependencies) out.writeInt(dependency);
        }
        byte[] payload = bytes.toByteArray();
        if (payload.length > maxBytes - HASH_BYTES) throw new IOException("build state image budget exceeded");
        byte[] image = Arrays.copyOf(payload, payload.length + HASH_BYTES);
        byte[] digest = digest(payload);
        System.arraycopy(digest, 0, image, payload.length, HASH_BYTES);
        return image;
    }

    public static M3BuildState decode(byte[] encoded, int maxBytes, int maxRows, int maxEdges) throws IOException {
        Objects.requireNonNull(encoded, "encoded");
        if (maxBytes < 0 || maxRows < 0 || maxEdges < 0 || encoded.length > maxBytes || encoded.length < 52 + HASH_BYTES)
            throw new IOException("invalid or oversized build state image");
        int payloadEnd = encoded.length - HASH_BYTES;
        if (!MessageDigest.isEqual(digest(Arrays.copyOf(encoded, payloadEnd)),
                Arrays.copyOfRange(encoded, payloadEnd, encoded.length))) {
            throw new IOException("build state checksum mismatch");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(encoded, 0, payloadEnd))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) throw new IOException("unknown build state schema");
            int n = in.readInt(), m = in.readInt();
            if (n < 0 || n > maxRows || m < 0 || m > maxEdges) throw new IOException("invalid image counts");
            byte[] context = new byte[HASH_BYTES]; in.readFully(context);
            String[] paths = new String[n]; byte[] roles = new byte[n]; long[] lengths = new long[n];
            byte[] content = new byte[Math.multiplyExact(n, HASH_BYTES)];
            byte[] abi = new byte[content.length];
            for (int i = 0; i < n; i++) {
                int len = in.readInt();
                if (len < 1 || len > MAX_KEY_BYTES) throw new IOException("invalid path size");
                byte[] pathBytes = new byte[len]; in.readFully(pathBytes);
                try {
                    paths[i] = StandardCharsets.UTF_8.newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(pathBytes)).toString();
                } catch (CharacterCodingException invalid) { throw new IOException("malformed UTF-8 path", invalid); }
                roles[i] = in.readByte(); lengths[i] = in.readLong();
                in.readFully(content, i * HASH_BYTES, HASH_BYTES);
                in.readFully(abi, i * HASH_BYTES, HASH_BYTES);
            }
            long[] abiKnown = new long[(int) ((n + 63L) >>> 6)];
            for (int i = 0; i < abiKnown.length; i++) abiKnown[i] = in.readLong();
            int[] offsets = new int[n + 1], dependencies = new int[m];
            for (int i = 0; i < offsets.length; i++) offsets[i] = in.readInt();
            for (int i = 0; i < m; i++) dependencies[i] = in.readInt();
            if (in.read() != -1) throw new IOException("trailing build state payload");
            return new M3BuildState(paths, roles, lengths, content, abi, abiKnown, offsets, dependencies, context);
        } catch (IllegalArgumentException | ArithmeticException invalid) {
            throw new IOException("invalid build state lanes", invalid);
        }
    }
}
