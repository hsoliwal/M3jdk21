/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package java.lang;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.zip.CRC32C;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import sun.nio.ch.DirectBuffer;

/**
 * Canonical atom/tuple owner for the experimental MIndex-backed String runtime.
 *
 * <p>There is one content plane with two admission sources:</p>
 * <ul>
 *   <li>pre-materialized UTF-16 lexicon records in one immutable read-only mapped image; and</li>
 *   <li>VM-local immutable Compact-String payloads for content absent from that image.</li>
 * </ul>
 *
 * <p>Both sources produce canonical {@link MIndexString} scalar atoms. Joined values are weakly
 * interned tuples of scalar atom/range coordinates. A Java String wrapper is never the identity of
 * payload storage. String identity therefore remains ordinary Java object identity while equal live
 * payload atoms and equal live tuple compositions share the same storage objects.</p>
 */
final class MIndexStringPool {
    private static final long LEXICON_MAGIC = 0x4d334c4558303031L; // M3LEX001
    private static final int LEXICON_HEADER = 64;
    private static final int MAX_LEXICON_BYTES = 1 << 30;
    private static final AtomicLong NEXT_LOCAL_ID = new AtomicLong(1L);
    private static final AtomicLong NEXT_JOIN_ID = new AtomicLong(1L << 40);

    private static final ConcurrentHashMap<Fingerprint, LocalBucket> LOCAL =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, JoinBucket> JOINS =
            new ConcurrentHashMap<>();
    private static final ReferenceQueue<MIndexString> LOCAL_QUEUE = new ReferenceQueue<>();
    private static final ReferenceQueue<MIndexString> JOIN_QUEUE = new ReferenceQueue<>();

    private static volatile Lexicon lexicon;

    private MIndexStringPool() {}

    static void initializeLexicon(String file) {
        if (file == null || file.isEmpty() || lexicon != null) {
            return;
        }
        synchronized (MIndexStringPool.class) {
            if (lexicon != null) {
                return;
            }
            try {
                lexicon = Lexicon.open(Path.of(file));
            } catch (IOException | RuntimeException failure) {
                // Experimental storage fails closed to the VM-local interner. String semantics
                // must not depend on an optional external lexicon being present.
                lexicon = Lexicon.unavailable();
            }
        }
    }

    static MIndexString internScalar(byte[] value, byte coder) {
        Objects.requireNonNull(value, "value");
        if (value.length == 0) {
            return MIndexString.emptyStorage();
        }

        Lexicon active = lexicon;
        if (active != null && active.available()) {
            int row = active.find(value, coder);
            if (row >= 0) {
                return active.atom(row);
            }
        }
        return internLocal(value, coder);
    }

    private static MIndexString internLocal(byte[] value, byte coder) {
        expungeLocal();
        long hash64 = contentHash64(value, coder);
        Fingerprint fingerprint = new Fingerprint(coder, value.length, hash64);
        for (;;) {
            LocalBucket bucket = LOCAL.computeIfAbsent(fingerprint, ignored -> new LocalBucket());
            synchronized (bucket) {
                if (bucket.retired) {
                    continue;
                }
                for (Iterator<LocalRef> iterator = bucket.values.iterator(); iterator.hasNext();) {
                    LocalRef reference = iterator.next();
                    MIndexString existing = reference.get();
                    if (existing == null) {
                        iterator.remove();
                    } else if (existing.localContentEquals(value, coder)) {
                        return existing;
                    }
                }
                long id = nextId(NEXT_LOCAL_ID, "local MIndex atom ID");
                MIndexString created =
                        MIndexString.localScalar(value, coder, id, hash64);
                bucket.values.add(new LocalRef(created, fingerprint, bucket));
                return created;
            }
        }
    }

    static MIndexString internJoin(
            MIndexString[] atoms,
            int[] offsets,
            int[] lengths,
            byte coder,
            int logicalLength) {
        MIndexString.enterAdmission();
        try {
            return internJoinGuarded(atoms, offsets, lengths, coder, logicalLength);
        } finally {
            MIndexString.leaveAdmission();
        }
    }

    private static MIndexString internJoinGuarded(MIndexString[] atoms, int[] offsets,
            int[] lengths, byte coder, int logicalLength) {
        expungeJoins();
        long hash64 = tupleHash64(atoms, offsets, lengths, coder, logicalLength);
        for (;;) {
            JoinBucket bucket = JOINS.computeIfAbsent(hash64, ignored -> new JoinBucket());
            synchronized (bucket) {
                if (bucket.retired) {
                    continue;
                }
                for (Iterator<JoinRef> iterator = bucket.values.iterator(); iterator.hasNext();) {
                    JoinRef reference = iterator.next();
                    MIndexString existing = reference.get();
                    if (existing == null) {
                        iterator.remove();
                    } else if (existing.joinGeometryEquals(atoms, offsets, lengths, coder)) {
                        return existing;
                    }
                }
                long id = nextId(NEXT_JOIN_ID, "joined MIndex tuple ID");
                MIndexString created =
                        MIndexString.joinedCanonical(
                                atoms, offsets, lengths, coder, logicalLength, id, hash64);
                bucket.values.add(new JoinRef(created, hash64, bucket));
                return created;
            }
        }
    }

    private static void expungeLocal() {
        LocalRef reference;
        while ((reference = (LocalRef) LOCAL_QUEUE.poll()) != null) {
            LocalBucket bucket = reference.bucket;
            synchronized (bucket) {
                bucket.values.remove(reference);
                if (bucket.values.isEmpty()
                        && LOCAL.remove(reference.fingerprint, bucket)) {
                    bucket.retired = true;
                }
            }
        }
    }

    private static void expungeJoins() {
        JoinRef reference;
        while ((reference = (JoinRef) JOIN_QUEUE.poll()) != null) {
            JoinBucket bucket = reference.bucket;
            synchronized (bucket) {
                bucket.values.remove(reference);
                if (bucket.values.isEmpty()
                        && JOINS.remove(reference.hash64, bucket)) {
                    bucket.retired = true;
                }
            }
        }
    }

    private static long nextId(AtomicLong sequence, String label) {
        long id = sequence.getAndIncrement();
        if (id <= 0L) {
            throw new IllegalStateException(label + " exhausted");
        }
        return id;
    }

    private static long contentHash64(byte[] value, byte coder) {
        long hash = mix64(0x9e3779b97f4a7c15L ^ coder ^ Integer.toUnsignedLong(value.length));
        for (byte element : value) {
            hash = mix64(hash ^ (element & 0xffL));
        }
        return hash;
    }

    private static long tupleHash64(
            MIndexString[] atoms,
            int[] offsets,
            int[] lengths,
            byte coder,
            int logicalLength) {
        long hash =
                mix64(0x517cc1b727220a95L
                        ^ coder
                        ^ Integer.toUnsignedLong(logicalLength)
                        ^ Integer.toUnsignedLong(atoms.length));
        for (int index = 0; index < atoms.length; index++) {
            hash = mix64(hash ^ atoms[index].canonicalId());
            hash = mix64(hash ^ Integer.toUnsignedLong(offsets[index]));
            hash = mix64(hash ^ Integer.toUnsignedLong(lengths[index]));
        }
        return hash;
    }

    static long mix64(long value) {
        long mixed = value;
        mixed ^= mixed >>> 30;
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }

    private static final class Fingerprint {
        final byte coder;
        final int byteLength;
        final long hash64;
        Fingerprint(byte coder, int byteLength, long hash64) {
            this.coder = coder; this.byteLength = byteLength; this.hash64 = hash64;
        }
        public int hashCode() { return (int)(hash64 ^ (hash64 >>> 32)) ^ byteLength ^ coder; }
        public boolean equals(Object other) {
            return other instanceof Fingerprint f && coder == f.coder
                && byteLength == f.byteLength && hash64 == f.hash64;
        }
    }

    private static final class LocalBucket {
        final ArrayList<LocalRef> values = new ArrayList<>();
        boolean retired;
    }

    private static final class JoinBucket {
        final ArrayList<JoinRef> values = new ArrayList<>();
        boolean retired;
    }

    private static final class LocalRef extends WeakReference<MIndexString> {
        final Fingerprint fingerprint;
        final LocalBucket bucket;

        LocalRef(MIndexString value, Fingerprint fingerprint, LocalBucket bucket) {
            super(value, LOCAL_QUEUE);
            this.fingerprint = fingerprint;
            this.bucket = bucket;
        }
    }

    private static final class JoinRef extends WeakReference<MIndexString> {
        final long hash64;
        final JoinBucket bucket;

        JoinRef(MIndexString value, long hash64, JoinBucket bucket) {
            super(value, JOIN_QUEUE);
            this.hash64 = hash64;
            this.bucket = bucket;
        }
    }

    /**
     * Read-only lexicon image. Records are UTF-16LE and must be sorted by Java UTF-16 lexical
     * order. The mapping itself owns payload lifetime; scalar atoms retain the mapping object.
     */
    private static final class Lexicon {
        private final MappedByteBuffer mapping;
        private final long baseAddress;
        private final int payloadOffset;
        private final int[] offsets;
        private final int[] lengths;
        private final int[] javaHashes;
        private final MIndexString[] atoms;
        private final long generation64;
        private final boolean available;
        private boolean bigEndian;
        private long namespaceHigh, namespaceLow;
        private int[] recordOffsets;

        private Lexicon() {
            mapping = null;
            baseAddress = 0L;
            payloadOffset = 0;
            offsets = lengths = javaHashes = new int[0];
            atoms = new MIndexString[0];
            generation64 = 0L;
            available = false;
        }

        private Lexicon(
                MappedByteBuffer mapping,
                int payloadOffset,
                int[] offsets,
                int[] lengths,
                int[] javaHashes,
                long generation64) {
            this.mapping = mapping;
            this.baseAddress = ((DirectBuffer) mapping).address();
            this.payloadOffset = payloadOffset;
            this.offsets = offsets;
            this.lengths = lengths;
            this.javaHashes = javaHashes;
            this.atoms = new MIndexString[offsets.length];
            this.generation64 = generation64;
            this.available = true;
        }

        static Lexicon unavailable() {
            return new Lexicon();
        }

        boolean available() {
            return available;
        }

        static Lexicon open(Path path) throws IOException {
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                long size = channel.size();
                if (size < LEXICON_HEADER || size > MAX_LEXICON_BYTES) {
                    throw new IOException("MIndex lexicon image size out of bounds");
                }
                MappedByteBuffer mapped =
                        channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
                mapped.order(ByteOrder.BIG_ENDIAN);
                if (mapped.getLong(0) == 0x53594e4152523031L) {
                    return openSharedArena(mapped);
                }
                if (mapped.getLong(0) != LEXICON_MAGIC) {
                    throw new IOException("unsupported MIndex lexicon image");
                }
                int version = mapped.getInt(8);
                if (version != 1 && version != 2) {
                    throw new IOException("unsupported MIndex lexicon version");
                }
                int count = mapped.getInt(12);
                int directoryBytes = version == 1 ? 8 : 12;
                long payload = mapped.getLong(16);
                long units = mapped.getLong(24);
                if (count < 0
                        || payload != LEXICON_HEADER + (long) directoryBytes * count
                        || units < 0
                        || payload + 2L * units != size) {
                    throw new IOException("invalid MIndex lexicon dimensions");
                }

                byte[] expected = new byte[32];
                mapped.get(32, expected);
                byte[] actual = digestImage(mapped);
                if (!MessageDigest.isEqual(expected, actual)) {
                    throw new IOException("MIndex lexicon checksum mismatch");
                }

                int[] offsets = new int[count];
                int[] lengths = new int[count];
                int[] javaHashes = new int[count];
                for (int row = 0; row < count; row++) {
                    int entry = LEXICON_HEADER + row * directoryBytes;
                    int offset = mapped.getInt(entry);
                    int length = mapped.getInt(entry + 4);
                    if (offset < 0 || length < 0 || (long) offset + length > units) {
                        throw new IOException("invalid MIndex lexicon record");
                    }
                    offsets[row] = offset;
                    lengths[row] = length;
                    javaHashes[row] =
                            version == 2
                                    ? mapped.getInt(entry + 8)
                                    : javaHash(mapped, (int) payload, offset, length);
                    if (javaHashes[row] != javaHash(mapped, (int) payload, offset, length)) {
                        throw new IOException("MIndex lexicon Java hash mismatch");
                    }
                    if (row > 0
                            && compareRecords(
                                            mapped,
                                            (int) payload,
                                            offsets[row - 1],
                                            lengths[row - 1],
                                            offset,
                                            length)
                                    >= 0) {
                        throw new IOException("MIndex lexicon must be UTF-16 sorted");
                    }
                }
                long generation64 = ByteBuffer.wrap(actual).getLong();
                return new Lexicon(
                        mapped, (int) payload, offsets, lengths, javaHashes, generation64);
            }
        }

        int find(byte[] value, byte coder) {
            int logicalLength = value.length >> coder;
            int low = 0;
            int high = offsets.length - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                int comparison =
                        compareInput(value, coder, logicalLength, offsets[middle], lengths[middle]);
                if (comparison == 0) {
                    return middle;
                }
                if (comparison < 0) {
                    high = middle - 1;
                } else {
                    low = middle + 1;
                }
            }
            return -1;
        }

        MIndexString atom(int row) {
            MIndexString result = atoms[row];
            if (result != null) {
                return result;
            }
            synchronized (this) {
                result = atoms[row];
                if (result == null) {
                    long address = baseAddress + payloadOffset + (bigEndian ? offsets[row] : ((long) offsets[row] << 1));
                    long id = Long.MIN_VALUE
                            | (generation64 & 0x3fff_ffff_0000_0000L)
                            | Integer.toUnsignedLong(row + 1);
                    result =
                            MIndexString.lexiconScalar(
                                    mapping,
                                    address,
                                    lengths[row],
                                    javaHashes[row],
                                    id,
                                    MIndexStringPool.mix64(id ^ lengths[row]), bigEndian);
                    atoms[row] = result;
                }
                return result;
            }
        }

        private int compareInput(
                byte[] value, byte coder, int logicalLength, int offset, int length) {
            int common = Math.min(logicalLength, length);
            for (int index = 0; index < common; index++) {
                char left =
                        coder == String.LATIN1
                                ? (char) (value[index] & 0xff)
                                : StringUTF16.charAt(value, index);
                char right = mappedUnit(offset, index);
                if (left != right) {
                    return left - right;
                }
            }
            return logicalLength - length;
        }

        private char mappedUnit(int offset, int index) {
            int at = bigEndian ? offset + (index << 1) : payloadOffset + ((offset + index) << 1);
            int a = mapping.get(at) & 255, b = mapping.get(at + 1) & 255;
            return bigEndian ? (char)((a << 8) | b) : (char)(a | (b << 8));
        }

        /** Read the existing owner format without creating another arena or copying payloads.
         * Publication contract matches SharedArrayPool: trusted directory, immutable committed
         * records, no external truncation or mutation. This is a fixed snapshot, not a writer.
         * Only language-0 UTF16 scalar records enter ordinary String's language-neutral plane.
         */
        private static Lexicon openSharedArena(MappedByteBuffer map) throws IOException {
            CRC32C crc = new CRC32C();
            crc.update(map.asReadOnlyBuffer().slice(0, 56));
            if (map.getInt(8) != 1 || map.getLong(56) != crc.getValue()
                    || map.getLong(32) < map.limit() || map.getInt(40) < 0) {
                throw new IOException("invalid SYNARR01 header");
            }
            ArrayList<int[]> records = new ArrayList<>();
            java.util.HashMap<Long, int[]> prior = new java.util.HashMap<>();
            int position = 64, count = 0;
            while (position < map.limit()) {
                if (map.limit() - position < 88) throw new IOException("incomplete SYNARR01 record");
                int total = map.getInt(position+4), width = map.getInt(position+8);
                int language = map.getInt(position+12), units = map.getInt(position+16);
                int parts = map.getInt(position+20), payload = map.getInt(position+28);
                long expected = parts < 0 ? (long)width * units : (long)parts * 16;
                if (map.getInt(position) != 0x41525231 || (width != 1 && width != 2)
                        || units < 0 || parts < -1 || payload < 0 || payload > 64*1024*1024
                        || expected != payload || 88L+payload != total || total > map.limit()-position
                        || map.getLong(position+72) != 0 || (width == 1 && language != 0)
                        || ++count > map.getInt(40)
                        || map.getLong(position+total-8) != (0x434f4d4d49543031L ^ position ^ total)) {
                    throw new IOException("invalid SYNARR01 record");
                }
                crc.reset();crc.update(map.asReadOnlyBuffer().slice(position,64));
                crc.update(map.asReadOnlyBuffer().slice(position+80,payload));
                if (crc.getValue() != map.getLong(position+64)) throw new IOException("SYNARR01 CRC mismatch");
                if (parts >= 0) {
                    long length = 0;
                    for (int i=0;i<parts;i++) {
                        int at=position+80+i*16;int[] atom=prior.get(map.getLong(at));
                        int start=map.getInt(at+8), size=map.getInt(at+12);
                        if(atom==null || atom[0]!=width || atom[2]!=-1 || start<0 || size<=0
                                || (long)start+size>atom[1]) throw new IOException("invalid SYNARR01 tuple");
                        length += size;
                    }
                    if(length!=units)throw new IOException("invalid SYNARR01 tuple length");
                } else if (width==2 && language==0 && units>0) {
                    int hash=0;for(int i=0;i<units;i++)hash=31*hash+(map.getChar(position+80+i*2)&65535);
                    if(hash!=map.getInt(position+24))throw new IOException("SYNARR01 hash mismatch");
                    records.add(new int[]{position+80,units,hash,position});
                }
                prior.put((long)position,new int[]{width,units,parts});position+=total;
            }
            records.sort((a,b)->{
                int common=Math.min(a[1],b[1]);
                for(int i=0;i<common;i++){int diff=map.getChar(a[0]+i*2)-map.getChar(b[0]+i*2);if(diff!=0)return diff;}
                return Integer.compare(a[1],b[1]);
            });
            int n=records.size();int[] offsets=new int[n],lengths=new int[n],hashes=new int[n],ids=new int[n];
            for(int i=0;i<n;i++){int[] r=records.get(i);offsets[i]=r[0];lengths[i]=r[1];hashes[i]=r[2];ids[i]=r[3];}
            Lexicon result=new Lexicon(map,0,offsets,lengths,hashes,map.getLong(16)^map.getLong(24));
            result.bigEndian=true;result.namespaceHigh=map.getLong(16);result.namespaceLow=map.getLong(24);
            result.recordOffsets=ids;return result;
        }

        private static int compareRecords(
                ByteBuffer mapped,
                int payloadOffset,
                int leftOffset,
                int leftLength,
                int rightOffset,
                int rightLength) {
            int common = Math.min(leftLength, rightLength);
            for (int index = 0; index < common; index++) {
                char left = mappedChar(mapped, payloadOffset, leftOffset + index);
                char right = mappedChar(mapped, payloadOffset, rightOffset + index);
                if (left != right) {
                    return left - right;
                }
            }
            return leftLength - rightLength;
        }

        private static int javaHash(
                ByteBuffer mapped, int payloadOffset, int offset, int length) {
            int hash = 0;
            for (int index = 0; index < length; index++) {
                hash = 31 * hash + mappedChar(mapped, payloadOffset, offset + index);
            }
            return hash;
        }

        private static char mappedChar(ByteBuffer mapped, int payloadOffset, int unit) {
            int at = payloadOffset + (unit << 1);
            int low = mapped.get(at) & 0xff;
            int high = mapped.get(at + 1) & 0xff;
            return (char) (low | (high << 8));
        }

        private static byte[] digestImage(ByteBuffer bytes) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(bytes.asReadOnlyBuffer().slice(0, 32));
                digest.update(
                        bytes.asReadOnlyBuffer()
                                .slice(LEXICON_HEADER, bytes.limit() - LEXICON_HEADER));
                return digest.digest();
            } catch (NoSuchAlgorithmException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @SuppressWarnings("unused")
        String generation() {
            return HexFormat.of().toHexDigits(generation64);
        }
    }
}
