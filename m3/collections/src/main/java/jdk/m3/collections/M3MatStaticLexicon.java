/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable finite lexicon stored as one UTF-8 arena plus primitive metadata and lookup lanes.
 *
 * <p>IDs are unsigned-UTF-8 lexical ranks. Input Strings are not retained per lexeme.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatStaticLexicon.java}
 * blob {@code 58e162aa9f12bacecc90d28ded7cf0700f6936b1} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatStaticLexicon {
    private static final int FNV_OFFSET = 0x811c9dc5;
    private static final int FNV_PRIME = 0x01000193;

    private final String namespace;
    private final byte[] arena;
    private final int[] offsets;
    private final long[] semanticIds;
    private final long[] flags;
    private final int[] slots;
    private final long fingerprint64;

    private M3MatStaticLexicon(
            String namespace,
            byte[] arena,
            int[] offsets,
            long[] semanticIds,
            long[] flags,
            int[] slots,
            long fingerprint64) {
        this.namespace = namespace;
        this.arena = arena;
        this.offsets = offsets;
        this.semanticIds = semanticIds;
        this.flags = flags;
        this.slots = slots;
        this.fingerprint64 = fingerprint64;
    }

    public static M3MatStaticLexicon compile(String namespace, Collection<Entry> entries) {
        String canonicalNamespace = requireNamespace(namespace);
        Objects.requireNonNull(entries, "entries");

        List<EncodedEntry> encoded = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            Objects.requireNonNull(entry, "entry");
            requireScalarText(entry.token());
            encoded.add(new EncodedEntry(
                    entry.token().getBytes(StandardCharsets.UTF_8),
                    entry.semanticId(),
                    entry.flags()));
        }
        encoded.sort(Comparator.comparing(
                EncodedEntry::bytes,
                M3MatStaticLexicon::compareUnsigned));

        List<EncodedEntry> unique = new ArrayList<>(encoded.size());
        EncodedEntry previous = null;
        for (EncodedEntry entry : encoded) {
            if (previous != null && compareUnsigned(previous.bytes(), entry.bytes()) == 0) {
                if (previous.semanticId() != entry.semanticId() || previous.flags() != entry.flags()) {
                    throw new IllegalArgumentException(
                            "Conflicting metadata for duplicate lexicon token");
                }
                continue;
            }
            unique.add(entry);
            previous = entry;
        }

        long totalBytes = 0L;
        for (EncodedEntry entry : unique) {
            totalBytes = Math.addExact(totalBytes, entry.bytes().length);
            if (totalBytes > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Lexicon UTF-8 arena exceeds JVM array limit");
            }
        }

        byte[] arena = new byte[(int) totalBytes];
        int[] offsets = new int[unique.size() + 1];
        long[] semanticIds = new long[unique.size()];
        long[] flags = new long[unique.size()];
        int cursor = 0;
        for (int id = 0; id < unique.size(); id++) {
            EncodedEntry entry = unique.get(id);
            offsets[id] = cursor;
            System.arraycopy(entry.bytes(), 0, arena, cursor, entry.bytes().length);
            cursor += entry.bytes().length;
            semanticIds[id] = entry.semanticId();
            flags[id] = entry.flags();
        }
        offsets[unique.size()] = cursor;

        int[] slots = new int[tableCapacity(unique.size())];
        for (int id = 0; id < unique.size(); id++) {
            int start = offsets[id];
            int length = offsets[id + 1] - start;
            int slot = hash(arena, start, length) & (slots.length - 1);
            while (slots[slot] != 0) {
                slot = (slot + 1) & (slots.length - 1);
            }
            slots[slot] = id + 1;
        }

        return new M3MatStaticLexicon(
                canonicalNamespace,
                arena,
                offsets,
                semanticIds,
                flags,
                slots,
                fingerprint(canonicalNamespace, arena, offsets, semanticIds, flags));
    }

    public String namespace() {
        return namespace;
    }

    public int size() {
        return semanticIds.length;
    }

    public long fingerprint64() {
        return fingerprint64;
    }

    public int idOf(String token) {
        Objects.requireNonNull(token, "token");
        requireScalarText(token);
        byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
        int slot = hash(bytes, 0, bytes.length) & (slots.length - 1);
        for (int probes = 0; probes < slots.length; probes++) {
            int encoded = slots[slot];
            if (encoded == 0) {
                return -1;
            }
            int id = encoded - 1;
            if (matches(id, bytes)) {
                return id;
            }
            slot = (slot + 1) & (slots.length - 1);
        }
        return -1;
    }

    public int requireId(String token) {
        int id = idOf(token);
        if (id < 0) {
            throw new IllegalArgumentException(
                    "Unknown " + namespace + " lexeme: " + token);
        }
        return id;
    }

    public String token(int id) {
        int checked = checkedId(id);
        int start = offsets[checked];
        return new String(arena, start, offsets[checked + 1] - start, StandardCharsets.UTF_8);
    }

    public ByteBuffer utf8(int id) {
        int checked = checkedId(id);
        int start = offsets[checked];
        return ByteBuffer.wrap(arena, start, offsets[checked + 1] - start)
                .slice()
                .asReadOnlyBuffer();
    }

    public long semanticId(int id) {
        return semanticIds[checkedId(id)];
    }

    public long flags(int id) {
        return flags[checkedId(id)];
    }

    public int compareIds(int left, int right) {
        return Integer.compare(checkedId(left), checkedId(right));
    }

    private boolean matches(int id, byte[] query) {
        int start = offsets[id];
        int length = offsets[id + 1] - start;
        if (length != query.length) {
            return false;
        }
        for (int index = 0; index < length; index++) {
            if (arena[start + index] != query[index]) {
                return false;
            }
        }
        return true;
    }

    private int checkedId(int id) {
        return Objects.checkIndex(id, semanticIds.length);
    }

    private static void requireScalarText(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (++index >= value.length() || !Character.isLowSurrogate(value.charAt(index))) {
                    throw new IllegalArgumentException("Lexicon token contains an unpaired surrogate");
                }
            } else if (Character.isLowSurrogate(current)) {
                throw new IllegalArgumentException("Lexicon token contains an unpaired surrogate");
            }
        }
    }

    private static String requireNamespace(String value) {
        String namespace = Objects.requireNonNull(value, "namespace").strip();
        if (namespace.isEmpty() || namespace.length() > 128) {
            throw new IllegalArgumentException("namespace must contain 1..128 characters");
        }
        return namespace;
    }

    private static int tableCapacity(int size) {
        int target = Math.max(2, Math.multiplyExact(Math.max(1, size), 2));
        int capacity = 1;
        while (capacity < target) {
            capacity = Math.multiplyExact(capacity, 2);
        }
        return capacity;
    }

    private static int compareUnsigned(byte[] left, byte[] right) {
        int common = Math.min(left.length, right.length);
        for (int index = 0; index < common; index++) {
            int compared = Integer.compare(left[index] & 0xff, right[index] & 0xff);
            if (compared != 0) {
                return compared;
            }
        }
        return Integer.compare(left.length, right.length);
    }

    private static int hash(byte[] bytes, int offset, int length) {
        int hash = FNV_OFFSET;
        int end = offset + length;
        for (int index = offset; index < end; index++) {
            hash ^= bytes[index] & 0xff;
            hash *= FNV_PRIME;
        }
        return hash;
    }

    private static long fingerprint(
            String namespace,
            byte[] arena,
            int[] offsets,
            long[] semanticIds,
            long[] flags) {
        long hash = 0xcbf29ce484222325L;
        for (byte value : namespace.getBytes(StandardCharsets.UTF_8)) {
            hash = mix(hash, value & 0xffL);
        }
        hash = mix(hash, 0xffL);
        for (int id = 0; id < semanticIds.length; id++) {
            int start = offsets[id];
            int end = offsets[id + 1];
            for (int index = start; index < end; index++) {
                hash = mix(hash, arena[index] & 0xffL);
            }
            hash = mix(hash, semanticIds[id]);
            hash = mix(hash, flags[id]);
        }
        return hash;
    }

    private static long mix(long hash, long value) {
        return (hash ^ value) * 0x100000001b3L;
    }

    public record Entry(String token, long semanticId, long flags) {
        public Entry {
            token = Objects.requireNonNull(token, "token");
            if (token.isEmpty()) {
                throw new IllegalArgumentException("Empty lexicon token");
            }
        }
    }

    private record EncodedEntry(byte[] bytes, long semanticId, long flags) { }
}
