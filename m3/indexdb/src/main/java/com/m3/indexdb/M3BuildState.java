// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable compiler-supplied build facts stored by the original M3IndexDB.
 * Physical rows, SHA-256 lanes, state flags and dependency edges are primitive
 * arrays; no per-file Node/Entry objects or second parser are retained.
 *
 * <p>The consumer must seal the complete build context, supply trustworthy
 * compiler ABI facts, verify all outputs and classify opaque plugin effects
 * before permitting any build step to be skipped. A plan alone never authorizes
 * a Maven goal to be cached or skipped.</p>
 */
public final class M3BuildState {
    public static final String ARTIFACT_KIND = "m3.build-state";
    public static final int VERSION = 1;
    public static final byte SOURCE = 1, CLASS = 2, INPUT = 3;
    public static final int DIRECT = 1, DEPENDENT = 2, CONTEXT = 4, REMOVAL = 8, INPUT_CHANGED = 16;
    private static final int MAGIC = 0x4d334253; // M3BS
    private static final int DIGEST_BYTES = 32;
    private static final int MAX_ROWS = 250_000, MAX_EDGES = 2_000_000;
    private static final int MAX_BYTES = 96 * 1024 * 1024;

    private final byte[] context;
    private final String[] names;
    private final byte[] kinds, content, abi, known;
    private final int[] consumers, providers;

    private M3BuildState(byte[] context, String[] names, byte[] kinds, byte[] content,
            byte[] abi, byte[] known, int[] consumers, int[] providers) {
        this.context = context;
        this.names = names;
        this.kinds = kinds;
        this.content = content;
        this.abi = abi;
        this.known = known;
        this.consumers = consumers;
        this.providers = providers;
    }

    /** Indexed dependency edge: consumers[i] depends on providers[i]. */
    public static M3BuildState of(byte[] contextSha256, String[] identities, byte[] kinds,
            byte[] contentSha256, byte[] abiSha256, byte[] abiKnown,
            int[] consumers, int[] providers) {
        byte[] ctx = digest(contextSha256);
        String[] names = Objects.requireNonNull(identities, "identities").clone();
        byte[] type = Objects.requireNonNull(kinds, "kinds").clone();
        byte[] content = Objects.requireNonNull(contentSha256, "content").clone();
        byte[] abi = Objects.requireNonNull(abiSha256, "abi").clone();
        byte[] known = Objects.requireNonNull(abiKnown, "abiKnown").clone();
        int[] from = Objects.requireNonNull(consumers, "consumers").clone();
        int[] to = Objects.requireNonNull(providers, "providers").clone();
        if (names.length > MAX_ROWS || from.length > MAX_EDGES
                || type.length != names.length || known.length != names.length
                || content.length != (long) names.length * DIGEST_BYTES
                || abi.length != (long) names.length * DIGEST_BYTES
                || from.length != to.length) {
            throw new IllegalArgumentException("M3 build column geometry");
        }
        String previous = null;
        long maximumEncodedBytes = 48L + 68L * names.length + 8L * from.length;
        for (int i = 0; i < names.length; i++) {
            String name = Objects.requireNonNull(names[i], "identity");
            if (name.isBlank() || name.indexOf('\0') >= 0 || name.length() > 2048
                    || (previous != null && previous.compareTo(name) >= 0)) {
                throw new IllegalArgumentException("noncanonical M3 build identity order");
            }
            maximumEncodedBytes += 3L * name.length();
            if (maximumEncodedBytes > MAX_BYTES) throw new IllegalArgumentException("build index byte budget");
            previous = name;
            if (type[i] < SOURCE || type[i] > INPUT || (known[i] != 0 && known[i] != 1)) {
                throw new IllegalArgumentException("invalid M3 build kind or ABI certainty");
            }
            if (known[i] == 0) {
                for (int j = i * DIGEST_BYTES; j < (i + 1) * DIGEST_BYTES; j++) {
                    if (abi[j] != 0) throw new IllegalArgumentException("unknown ABI must be empty");
                }
            }
        }
        int lastFrom = -1, lastTo = -1;
        for (int i = 0; i < from.length; i++) {
            if (from[i] < 0 || from[i] >= names.length
                    || to[i] < 0 || to[i] >= names.length
                    || from[i] < lastFrom || (from[i] == lastFrom && to[i] <= lastTo)) {
                throw new IllegalArgumentException("noncanonical M3 dependency edges");
            }
            lastFrom = from[i];
            lastTo = to[i];
        }
        return new M3BuildState(ctx, names, type, content, abi, known, from, to);
    }

    public int rows() { return names.length; }
    public int edges() { return consumers.length; }
    public String identityAt(int row) { return names[Objects.checkIndex(row, names.length)]; }
    public byte kindAt(int row) { return kinds[Objects.checkIndex(row, names.length)]; }
    public byte[] contentAt(int row) { return Arrays.copyOfRange(content,
            Objects.checkIndex(row, names.length) * DIGEST_BYTES, (row + 1) * DIGEST_BYTES); }

    /**
     * Plan only Java source recompilation. Consumers must verify output files,
     * processors, declared task inputs and side effects independently.
     * Unknown/changed ABI propagates conservatively; equal, known ABI does not.
     */
    public Plan planAfter(M3BuildState previous) {
        int[] flags = new int[rows()];
        if (previous == null || !Arrays.equals(context, previous.context)) {
            for (int i = 0; i < rows(); i++) if (kinds[i] == SOURCE) flags[i] = DIRECT | CONTEXT;
            return new Plan(flags, kinds);
        }
        boolean[] abiChanged = new boolean[rows()];
        int broadReason = 0;
        for (int i = 0; i < rows(); i++) {
            int old = Arrays.binarySearch(previous.names, names[i]);
            boolean changed = old < 0 || previous.kinds[old] != kinds[i]
                    || !same(content, i, previous.content, old);
            boolean abiDifference = old >= 0 && previous.kinds[old] == kinds[i]
                    && known[i] == 1 && previous.known[old] == 1
                    && !same(abi, i, previous.abi, old);
            if ((changed || abiDifference) && kinds[i] == SOURCE) flags[i] |= DIRECT;
            if (changed && kinds[i] == INPUT) broadReason |= INPUT_CHANGED;
            if (kinds[i] != INPUT && (old < 0 || previous.kinds[old] != kinds[i]
                    || known[i] == 0 || previous.known[old] == 0
                    || !same(abi, i, previous.abi, old))) {
                // Unknown but byte-identical facts do not by themselves invalidate dependents.
                abiChanged[i] = changed || abiDifference;
            }
        }
        for (String name : previous.names) {
            if (Arrays.binarySearch(names, name) < 0) { broadReason |= REMOVAL; break; }
        }
        if (broadReason != 0) {
            for (int i = 0; i < rows(); i++) if (kinds[i] == SOURCE) flags[i] |= DIRECT | broadReason;
            return new Plan(flags, kinds);
        }
        // Reverse CSR: for provider p, [offset[p], offset[p+1]) contains its consumers.
        int[] offset = new int[rows() + 1];
        for (int provider : providers) offset[provider + 1]++;
        for (int i = 1; i < offset.length; i++) offset[i] += offset[i - 1];
        int[] cursor = Arrays.copyOf(offset, rows());
        int[] reverse = new int[edges()];
        for (int i = 0; i < edges(); i++) reverse[cursor[providers[i]]++] = consumers[i];
        int[] queue = new int[rows()];
        boolean[] seen = new boolean[rows()];
        int head = 0, tail = 0;
        for (int i = 0; i < rows(); i++) {
            if (abiChanged[i]) { seen[i] = true; queue[tail++] = i; }
        }
        while (head < tail) {
            int provider = queue[head++];
            for (int edge = offset[provider]; edge < offset[provider + 1]; edge++) {
                int consumer = reverse[edge];
                if (kinds[consumer] == SOURCE) flags[consumer] |= DEPENDENT;
                if (!seen[consumer]) { seen[consumer] = true; queue[tail++] = consumer; }
            }
        }
        return new Plan(flags, kinds);
    }

    /** No false reuse claim: marks source rows, never executes or skips Maven tasks. */
    public static final class Plan {
        private final int[] flags;
        private final byte[] kinds;
        private Plan(int[] flags, byte[] kinds) { this.flags = flags; this.kinds = kinds.clone(); }
        public int reasonAt(int row) { return flags[Objects.checkIndex(row, flags.length)]; }
        public int[] recompileRows() {
            int count = 0;
            for (int i = 0; i < flags.length; i++) if (kinds[i] == SOURCE && flags[i] != 0) count++;
            int[] result = new int[count];
            for (int i = 0, j = 0; i < flags.length; i++) {
                if (kinds[i] == SOURCE && flags[i] != 0) result[j++] = i;
            }
            return result;
        }
    }

    /** Persist in the already-owning M3IndexDB blob/ref store, not a second database. */
    public M3IndexDbArtifact store(M3IndexDB database, String name) throws IOException {
        return Objects.requireNonNull(database, "database")
                .putArtifact(name, ARTIFACT_KIND, VERSION, encode());
    }

    public static M3BuildState load(M3IndexDB database, String name) throws IOException {
        M3IndexDbArtifact artifact = Objects.requireNonNull(database, "database").requireArtifact(name);
        if (!ARTIFACT_KIND.equals(artifact.kind()) || artifact.formatVersion() != VERSION) {
            throw new IOException("wrong M3 build artifact kind/version");
        }
        return decode(artifact.payload());
    }

    public byte[] encode() throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC); out.writeInt(VERSION);
            out.write(context); out.writeInt(rows()); out.writeInt(edges());
            for (int i = 0; i < rows(); i++) {
                out.writeUTF(names[i]); out.writeByte(kinds[i]); out.writeByte(known[i]);
                out.write(content, i * DIGEST_BYTES, DIGEST_BYTES);
                out.write(abi, i * DIGEST_BYTES, DIGEST_BYTES);
            }
            for (int i = 0; i < edges(); i++) {
                out.writeInt(consumers[i]); out.writeInt(providers[i]);
            }
            out.flush();
            if (bytes.size() > MAX_BYTES) throw new IOException("M3 build state exceeds bound");
            return bytes.toByteArray();
        }
    }

    public static M3BuildState decode(byte[] encoded) throws IOException {
        Objects.requireNonNull(encoded, "encoded");
        if (encoded.length > MAX_BYTES) throw new IOException("M3 build state exceeds bound");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(encoded))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) throw new IOException("M3 build state version");
            byte[] context = in.readNBytes(DIGEST_BYTES);
            if (context.length != DIGEST_BYTES) throw new IOException("truncated M3 build context");
            int count = in.readInt(), links = in.readInt();
            if (count < 0 || count > MAX_ROWS || links < 0 || links > MAX_EDGES
                    || 48L + 68L * count + 8L * links > encoded.length) {
                throw new IOException("M3 build state row budget");
            }
            String[] names = new String[count];
            byte[] kinds = new byte[count], known = new byte[count];
            byte[] content = new byte[Math.multiplyExact(count, DIGEST_BYTES)];
            byte[] abi = new byte[Math.multiplyExact(count, DIGEST_BYTES)];
            int[] from = new int[links], to = new int[links];
            for (int i = 0; i < count; i++) {
                names[i] = in.readUTF(); kinds[i] = in.readByte(); known[i] = in.readByte();
                in.readFully(content, i * DIGEST_BYTES, DIGEST_BYTES);
                in.readFully(abi, i * DIGEST_BYTES, DIGEST_BYTES);
            }
            for (int i = 0; i < links; i++) { from[i] = in.readInt(); to[i] = in.readInt(); }
            if (in.read() != -1) throw new IOException("trailing M3 build bytes");
            try { return of(context, names, kinds, content, abi, known, from, to); }
            catch (IllegalArgumentException malformed) { throw new IOException("invalid M3 build state", malformed); }
        }
    }

    private static byte[] digest(byte[] value) {
        if (Objects.requireNonNull(value, "sha256").length != DIGEST_BYTES) {
            throw new IllegalArgumentException("SHA-256 must be 32 bytes");
        }
        return value.clone();
    }
    private static boolean same(byte[] left, int li, byte[] right, int ri) {
        int a = li * DIGEST_BYTES, b = ri * DIGEST_BYTES;
        for (int j = 0; j < DIGEST_BYTES; j++) if (left[a + j] != right[b + j]) return false;
        return true;
    }
}
