/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32C;
import jdk.internal.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

/**
 * Canonical owner pool for M3String.
 *
 * <p>Scalar payload is either read-only mapped storage or weakly interned VM-local native memory. Java
 * byte[]/char[] values are admission or compatibility shadows only and are never retained here.
 * Tuple nodes retain only child M3 coordinates and form a persistent immutable DAG.</p>
 */
final class M3StringPool {
    private static final long LEXICON_MAGIC = 0x4d334c4558303031L;
    private static final int LEXICON_HEADER = 64;
    private static final int MAX_LEXICON_BYTES = 1 << 30;
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    private static final AtomicLong NEXT_LOCAL_ID = new AtomicLong(1L);
    private static final AtomicLong NEXT_TUPLE_ID = new AtomicLong(1L << 40);
    private static final AtomicLong LOCAL_NATIVE_BYTES = new AtomicLong();

    private static final ConcurrentHashMap<Fingerprint, LocalBucket> LOCAL =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, TupleBucket> TUPLES =
            new ConcurrentHashMap<>();
    private static final ReferenceQueue<M3StringAtom> LOCAL_QUEUE = new ReferenceQueue<>();
    private static final ReferenceQueue<M3StringTuple> TUPLE_QUEUE = new ReferenceQueue<>();

    private static volatile Lexicon lexicon;

    private M3StringPool() {}

    static void initializeLexicon(String file) {
        if (file == null || file.isEmpty() || lexicon != null) return;
        synchronized (M3StringPool.class) {
            if (lexicon != null) return;
            try {
                lexicon = Lexicon.open(Path.of(file));
            } catch (IOException | RuntimeException failure) {
                lexicon = Lexicon.unavailable();
            }
        }
    }

    static M3String internScalar(byte[] value, byte coder) {
        Objects.requireNonNull(value, "value");
        if (coder != String.LATIN1 && coder != String.UTF16) {
            throw new IllegalArgumentException("invalid String coder");
        }
        if ((value.length >> coder << coder) != value.length) {
            throw new IllegalArgumentException("misaligned String payload");
        }
        if (value.length == 0) return M3String.empty();

        Lexicon active = lexicon;
        if (active != null && active.available()) {
            int row = active.find(value, coder);
            if (row >= 0) return M3String.whole(active.atom(row));
        }
        return M3String.whole(internLocal(value, coder));
    }

    static M3String internUnit(char unit) {
        Lexicon active = lexicon;
        if (active != null && active.available()) {
            int row = active.findUnit(unit);
            if (row >= 0) return M3String.whole(active.atom(row));
        }

        byte coder = StringLatin1.canEncode(unit) ? String.LATIN1 : String.UTF16;
        int byteLength = 1 << coder;
        long hash64 = mix64(0x9e3779b97f4a7c15L ^ coder ^ Integer.toUnsignedLong(byteLength));
        hash64 = mix64(hash64 ^ unit);
        Fingerprint fingerprint = new Fingerprint(coder, byteLength, hash64);

        expungeLocals();
        for (;;) {
            LocalBucket bucket = LOCAL.computeIfAbsent(fingerprint, ignored -> new LocalBucket());
            synchronized (bucket) {
                if (bucket.retired) continue;
                for (Iterator<LocalRef> iterator = bucket.values.iterator(); iterator.hasNext();) {
                    LocalRef reference = iterator.next();
                    M3StringAtom existing = reference.get();
                    if (existing == null) {
                        iterator.remove();
                        reference.releaseNative();
                    } else if (existing.length == 1
                            && existing.coder == coder
                            && existing.charAt(0) == unit) {
                        return M3String.whole(existing);
                    }
                }

                long id = nextId(NEXT_LOCAL_ID, "M3 scalar ID");
                M3StringAtom created = M3StringAtom.localUnit(unit, coder, id, hash64);
                long retainedBytes = created.nativePayloadBytes();
                LOCAL_NATIVE_BYTES.addAndGet(retainedBytes);
                bucket.values.add(new LocalRef(created, fingerprint, bucket, retainedBytes));
                return M3String.whole(created);
            }
        }
    }

    private static M3StringAtom internLocal(byte[] value, byte coder) {
        expungeLocals();
        long hash64 = contentHash64(value, coder);
        Fingerprint fingerprint = new Fingerprint(coder, value.length, hash64);
        for (;;) {
            LocalBucket bucket = LOCAL.computeIfAbsent(fingerprint, ignored -> new LocalBucket());
            synchronized (bucket) {
                if (bucket.retired) continue;
                for (Iterator<LocalRef> iterator = bucket.values.iterator(); iterator.hasNext();) {
                    LocalRef reference = iterator.next();
                    M3StringAtom existing = reference.get();
                    if (existing == null) {
                        iterator.remove();
                        reference.releaseNative();
                    } else if (existing.contentEquals(value, coder)) {
                        return existing;
                    }
                }
                long id = nextId(NEXT_LOCAL_ID, "M3 scalar ID");
                M3StringAtom created = M3StringAtom.local(value, coder, id, hash64);
                long retainedBytes = created.nativePayloadBytes();
                LOCAL_NATIVE_BYTES.addAndGet(retainedBytes);
                bucket.values.add(new LocalRef(created, fingerprint, bucket, retainedBytes));
                return created;
            }
        }
    }

    static M3String concat(M3String left, M3String right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length() == 0) return right;
        if (right.length() == 0) return left;
        if (left.owner() == right.owner() && left.end() == right.start()) {
            return M3String.range(left.owner(), left.start(), Math.addExact(left.length(), right.length()));
        }

        expungeTuples();
        long hash = tupleHash64(left, right);
        for (;;) {
            TupleBucket bucket = TUPLES.computeIfAbsent(hash, ignored -> new TupleBucket());
            synchronized (bucket) {
                if (bucket.retired) continue;
                for (Iterator<TupleRef> iterator = bucket.values.iterator(); iterator.hasNext();) {
                    TupleRef reference = iterator.next();
                    M3StringTuple existing = reference.get();
                    if (existing == null) iterator.remove();
                    else if (existing.geometryEquals(left, right)) return M3String.whole(existing);
                }
                long id = nextId(NEXT_TUPLE_ID, "M3 tuple ID");
                M3StringTuple created = new M3StringTuple(left, right, id, hash);
                bucket.values.add(new TupleRef(created, hash, bucket));
                return M3String.whole(created);
            }
        }
    }

    private static void expungeLocals() {
        LocalRef reference;
        while ((reference = (LocalRef) LOCAL_QUEUE.poll()) != null) {
            LocalBucket bucket = reference.bucket;
            synchronized (bucket) {
                bucket.values.remove(reference);
                reference.releaseNative();
                if (bucket.values.isEmpty() && LOCAL.remove(reference.fingerprint, bucket)) {
                    bucket.retired = true;
                }
            }
        }
    }

    private static void expungeTuples() {
        TupleRef reference;
        while ((reference = (TupleRef) TUPLE_QUEUE.poll()) != null) {
            TupleBucket bucket = reference.bucket;
            synchronized (bucket) {
                bucket.values.remove(reference);
                if (bucket.values.isEmpty() && TUPLES.remove(reference.hash64, bucket)) {
                    bucket.retired = true;
                }
            }
        }
    }

    static long localNativeBytes() {
        expungeLocals();
        return LOCAL_NATIVE_BYTES.get();
    }

    private static long nextId(AtomicLong sequence, String label) {
        long id = sequence.getAndIncrement();
        if (id <= 0L) throw new IllegalStateException(label + " exhausted");
        return id;
    }

    static long mix64(long value) {
        long mixed = value;
        mixed ^= mixed >>> 30;
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }

    private static long contentHash64(byte[] value, byte coder) {
        long hash = mix64(0x9e3779b97f4a7c15L ^ coder ^ Integer.toUnsignedLong(value.length));
        int length = value.length >> coder;
        for (int index = 0; index < length; index++) {
            char unit = coder == String.LATIN1
                    ? StringLatin1.charAt(value, index)
                    : StringUTF16.charAt(value, index);
            hash = mix64(hash ^ unit);
        }
        return hash;
    }

    private static long tupleHash64(M3String left, M3String right) {
        long hash = mix64(0x517cc1b727220a95L ^ left.identityHash64());
        hash = mix64(hash ^ right.identityHash64());
        hash = mix64(hash ^ Integer.toUnsignedLong(left.length()));
        return mix64(hash ^ Integer.toUnsignedLong(right.length()));
    }

    private static boolean mappedLatin1(long address, int length, boolean bigEndian) {
        for (int index = 0; index < length; index++) {
            long at = address + ((long) index << 1);
            int first = UNSAFE.getByte(at) & 0xff;
            int second = UNSAFE.getByte(at + 1L) & 0xff;
            char unit = bigEndian ? (char) ((first << 8) | second) : (char) (first | (second << 8));
            if (unit > 0xff) return false;
        }
        return true;
    }

    private record Fingerprint(byte coder, int byteLength, long hash64) {}

    private static final class LocalBucket {
        final ArrayList<LocalRef> values = new ArrayList<>();
        boolean retired;
    }

    private static final class LocalRef extends WeakReference<M3StringAtom> {
        final Fingerprint fingerprint;
        final LocalBucket bucket;
        final long address;
        final long retainedBytes;
        private boolean released;

        LocalRef(
                M3StringAtom value,
                Fingerprint fingerprint,
                LocalBucket bucket,
                long retainedBytes) {
            super(value, LOCAL_QUEUE);
            this.fingerprint = fingerprint;
            this.bucket = bucket;
            this.address = value.address;
            this.retainedBytes = retainedBytes;
        }

        void releaseNative() {
            if (!released) {
                released = true;
                if (address != 0L) UNSAFE.freeMemory(address);
                LOCAL_NATIVE_BYTES.addAndGet(-retainedBytes);
            }
        }
    }

    private static final class TupleBucket {
        final ArrayList<TupleRef> values = new ArrayList<>();
        boolean retired;
    }

    private static final class TupleRef extends WeakReference<M3StringTuple> {
        final long hash64;
        final TupleBucket bucket;
        TupleRef(M3StringTuple value, long hash64, TupleBucket bucket) {
            super(value, TUPLE_QUEUE);
            this.hash64 = hash64;
            this.bucket = bucket;
        }
    }

    private static final class Lexicon {
        private final MappedByteBuffer mapping;
        private final long baseAddress;
        private final int payloadOffset;
        private final int[] offsets;
        private final int[] lengths;
        private final int[] javaHashes;
        private final M3StringAtom[] atoms;
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
            atoms = new M3StringAtom[0];
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
            this.atoms = new M3StringAtom[offsets.length];
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

        int findUnit(char unit) {
            int low = 0;
            int high = offsets.length - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                int length = lengths[middle];
                int comparison;
                if (length == 0) {
                    comparison = 1;
                } else {
                    comparison = unit - mappedUnit(offsets[middle], 0);
                    if (comparison == 0) comparison = 1 - length;
                }
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

        M3StringAtom atom(int row) {
            M3StringAtom result = atoms[row];
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
                            M3StringAtom.mapped(
                                    mapping,
                                    address,
                                    (byte) 2,
                                    bigEndian,
                                    lengths[row],
                                    mappedLatin1(address, lengths[row], bigEndian)
                                            ? String.LATIN1 : String.UTF16,
                                    javaHashes[row],
                                    id,
                                    M3StringPool.mix64(id ^ lengths[row]));
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
