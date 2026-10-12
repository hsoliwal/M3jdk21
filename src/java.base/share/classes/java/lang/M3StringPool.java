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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32C;
import jdk.internal.access.JavaLangRefAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import sun.nio.ch.DirectBuffer;

/**
 * Canonical owner pool for M3String.
 *
 * <p>Scalar payload is either read-only mapped storage or weakly interned VM-local native memory. Java
 * byte[]/char[] values are admission or compatibility shadows only and are never retained here.
 * Tuple nodes retain only child M3 coordinates and form a persistent immutable DAG.</p>
 *
 * <p>Bounds. The native bytes retained by VM-local scalar owners are capped by
 * {@code -Dm3.string.pool.maxBytes} (default {@code Runtime.maxMemory()}, the MaxDirectMemorySize
 * default); every multi-unit admission reserves its bytes before allocating and a reclaimed block
 * returns them through the local reference queue. On exhaustion the admission takes the
 * {@code java.nio.Bits.reserveMemory} route (drain the queue, wait for reference processing, one
 * {@code System.gc()}, bounded back-off retries) and, if the budget is still short, it is refused:
 * the intern methods return {@code null} and the String keeps its flat spelling (lineage invariant
 * 8: precompute absence never changes semantics; no OutOfMemoryError is introduced). A refused
 * cycle holds further GC-assisted cycles off for one second so sustained pressure stays cheap. The
 * single-unit lane is bounded by construction (at most 65,536 atoms, 128 KiB) and never refuses.
 * Canonical tuple retention is capped by {@code -Dm3.string.pool.maxTuples} (default 1,048,576
 * registered entries); beyond the cap a composition still forms its tuple, only without canonical
 * reuse. A lexicon that fails to open is reported once on {@code System.err}, never per call.</p>
 */
final class M3StringPool {
    private static final long LEXICON_MAGIC = 0x4d334c4558303031L;
    private static final int LEXICON_HEADER = 64;
    private static final int MAX_LEXICON_BYTES = 1 << 30;
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    static final String MAX_LOCAL_BYTES_PROPERTY = "m3.string.pool.maxBytes";
    static final String MAX_RETAINED_TUPLES_PROPERTY = "m3.string.pool.maxTuples";
    static final long DEFAULT_MAX_RETAINED_TUPLES = 1L << 20;
    /** Native byte budget of the VM-local scalar lane; {@code 0} admits no multi-unit local atom. */
    static final long MAX_LOCAL_BYTES =
            bound(MAX_LOCAL_BYTES_PROPERTY, Runtime.getRuntime().maxMemory());
    /** Registered canonical tuple entries (live or awaiting expunge) the pool keeps at most. */
    static final long MAX_RETAINED_TUPLES =
            bound(MAX_RETAINED_TUPLES_PROPERTY, DEFAULT_MAX_RETAINED_TUPLES);
    /** Bits.reserveMemory uses 9 (about 0.5 s); String admission refuses after 1+2+4+8 ms. */
    private static final int MAX_SLEEPS = 4;
    private static final long SLOW_RESERVE_HOLDOFF_NANOS = 1_000_000_000L;

    private static final AtomicLong NEXT_LOCAL_ID = new AtomicLong(1L);
    private static final AtomicLong NEXT_TUPLE_ID = new AtomicLong(1L << 40);
    private static final AtomicLong LOCAL_NATIVE_BYTES = new AtomicLong();
    private static final AtomicLong RETAINED_TUPLES = new AtomicLong();

    private static final ConcurrentHashMap<Fingerprint, LocalBucket> LOCAL =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, TupleBucket> TUPLES =
            new ConcurrentHashMap<>();
    private static final ReferenceQueue<M3StringAtom> LOCAL_QUEUE = new ReferenceQueue<>();
    private static final ReferenceQueue<M3StringTuple> TUPLE_QUEUE = new ReferenceQueue<>();

    private static volatile Lexicon lexicon;
    /** Guarded by the class lock: the lexicon failure report is emitted once per VM. */
    private static boolean lexiconFailureReported;
    /** Deadline before which a budget miss is refused without another GC-assisted cycle. */
    private static volatile long nextSlowReserveNanos = System.nanoTime();

    private M3StringPool() {}

    static void initializeLexicon(String file) {
        if (file == null || file.isEmpty() || lexicon != null) return;
        synchronized (M3StringPool.class) {
            if (lexicon != null) return;
            try {
                lexicon = Lexicon.open(Path.of(file));
            } catch (IOException | RuntimeException failure) {
                lexicon = Lexicon.unavailable();
                reportLexiconFailure(file, failure);
            }
        }
    }

    /** Once per VM (class lock held): a missing or corrupt lexicon is a degraded mode, not an error. */
    private static void reportLexiconFailure(String file, Exception failure) {
        if (lexiconFailureReported) return;
        lexiconFailureReported = true;
        java.io.PrintStream err = System.err;
        if (err != null) {
            err.println("M3StringPool: lexicon " + file + " unavailable, continuing without it: "
                    + failure);
        }
    }

    /** Saved-property bound; absent, negative or unparsable values fall back (as MaxDirectMemorySize). */
    static long bound(String property, long fallback) {
        return parseBound(VM.initLevel() >= 1 ? VM.getSavedProperty(property) : null, fallback);
    }

    static long parseBound(String value, long fallback) {
        if (value == null) return fallback;
        try {
            long parsed = Long.parseLong(value);
            return parsed < 0L ? fallback : parsed;
        } catch (NumberFormatException invalid) {
            return fallback;
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
        return internCompactBytes(value, 0, value.length >> coder, coder);
    }

    static M3String internCodePoints(int[] source, int offset, int count) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, count, source.length);
        if (count == 0) return M3String.empty();

        long utf16LengthLong = 0L;
        boolean latin1 = String.COMPACT_STRINGS;
        for (int index = 0; index < count; index++) {
            int cp = source[offset + index];
            if (Character.isBmpCodePoint(cp)) {
                utf16LengthLong++;
                if (cp > 0xff) latin1 = false;
            } else if (Character.isValidCodePoint(cp)) {
                utf16LengthLong += 2L;
                latin1 = false;
            } else {
                throw new IllegalArgumentException(Integer.toString(cp));
            }
        }

        if (utf16LengthLong > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("UTF16 String size is " + utf16LengthLong);
        }
        int utf16Length = (int) utf16LengthLong;
        byte coder = latin1 ? String.LATIN1 : String.UTF16;
        if (coder == String.UTF16 && utf16Length > StringUTF16.MAX_LENGTH) {
            throw new OutOfMemoryError(
                    "UTF16 String size is " + utf16Length
                            + ", should be less than " + StringUTF16.MAX_LENGTH);
        }

        Lexicon active = lexicon;
        if (active != null && active.available()) {
            int row = active.findCodePoints(source, offset, count, utf16Length);
            if (row >= 0) return M3String.whole(active.atom(row));
        }

        int byteLength = utf16Length << coder;
        long hash64 =
                mix64(0x9e3779b97f4a7c15L ^ coder ^ Integer.toUnsignedLong(byteLength));
        for (int index = 0; index < count; index++) {
            int cp = source[offset + index];
            if (Character.isBmpCodePoint(cp)) {
                hash64 = mix64(hash64 ^ (char) cp);
            } else {
                hash64 = mix64(hash64 ^ Character.highSurrogate(cp));
                hash64 = mix64(hash64 ^ Character.lowSurrogate(cp));
            }
        }
        Fingerprint fingerprint = new Fingerprint(coder, byteLength, hash64);

        expungeLocals();
        long reserved = 0L;
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
                    } else if (existing.contentEqualsCodePoints(
                            source, offset, count, utf16Length, coder)) {
                        return found(existing, reserved);
                    }
                }

                if (reserved != 0L || tryReserve(byteLength)) {
                    M3StringAtom created = null;
                    try {
                        long id = nextId(NEXT_LOCAL_ID, "M3 scalar ID");
                        created = M3StringAtom.localCodePoints(
                                source, offset, count, utf16Length, coder, id, hash64);
                    } finally {
                        if (created == null) release(byteLength);
                    }
                    return retain(bucket, fingerprint, created, byteLength);
                }
            }
            reserved = reserveSlow(byteLength);
            if (reserved == 0L) return null;
        }
    }

    static M3String internCompactBytes(
            byte[] source, int sourceOffset, int length, byte sourceCoder) {
        Objects.requireNonNull(source, "source");
        if (sourceCoder != String.LATIN1 && sourceCoder != String.UTF16) {
            throw new IllegalArgumentException("invalid source String coder");
        }
        Objects.checkFromIndexSize(sourceOffset, length, source.length >> sourceCoder);
        if (length == 0) return M3String.empty();

        boolean latin1 = String.COMPACT_STRINGS;
        if (latin1 && sourceCoder == String.UTF16) {
            for (int index = 0; index < length; index++) {
                if (StringUTF16.charAt(source, sourceOffset + index) > 0xff) {
                    latin1 = false;
                    break;
                }
            }
        }
        byte targetCoder = latin1 ? String.LATIN1 : String.UTF16;
        if (targetCoder == String.UTF16 && length > StringUTF16.MAX_LENGTH) {
            throw new OutOfMemoryError(
                    "UTF16 String size is " + length
                            + ", should be less than " + StringUTF16.MAX_LENGTH);
        }

        Lexicon active = lexicon;
        if (active != null && active.available()) {
            int row = active.findCompactBytes(source, sourceOffset, length, sourceCoder);
            if (row >= 0) return M3String.whole(active.atom(row));
        }

        int byteLength = length << targetCoder;
        long hash64 =
                mix64(0x9e3779b97f4a7c15L ^ targetCoder ^ Integer.toUnsignedLong(byteLength));
        for (int index = 0; index < length; index++) {
            char unit =
                    sourceCoder == String.LATIN1
                            ? (char) (source[sourceOffset + index] & 0xff)
                            : StringUTF16.charAt(source, sourceOffset + index);
            hash64 = mix64(hash64 ^ unit);
        }
        Fingerprint fingerprint = new Fingerprint(targetCoder, byteLength, hash64);

        expungeLocals();
        long reserved = 0L;
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
                    } else if (existing.contentEqualsCompactBytes(
                            source, sourceOffset, length, sourceCoder, targetCoder)) {
                        return found(existing, reserved);
                    }
                }

                if (reserved != 0L || tryReserve(byteLength)) {
                    M3StringAtom created = null;
                    try {
                        long id = nextId(NEXT_LOCAL_ID, "M3 scalar ID");
                        created = M3StringAtom.localCompactBytes(
                                source, sourceOffset, length, sourceCoder, targetCoder, id, hash64);
                    } finally {
                        if (created == null) release(byteLength);
                    }
                    return retain(bucket, fingerprint, created, byteLength);
                }
            }
            reserved = reserveSlow(byteLength);
            if (reserved == 0L) return null;
        }
    }

    static M3String internLatin1Bytes(byte[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (length == 0) return M3String.empty();

        byte coder = String.COMPACT_STRINGS ? String.LATIN1 : String.UTF16;
        if (coder == String.UTF16 && length > StringUTF16.MAX_LENGTH) {
            throw new OutOfMemoryError(
                    "UTF16 String size is " + length
                            + ", should be less than " + StringUTF16.MAX_LENGTH);
        }

        Lexicon active = lexicon;
        if (active != null && active.available()) {
            int row = active.findLatin1Bytes(source, offset, length);
            if (row >= 0) return M3String.whole(active.atom(row));
        }

        int byteLength = length << coder;
        long hash64 =
                mix64(0x9e3779b97f4a7c15L ^ coder ^ Integer.toUnsignedLong(byteLength));
        for (int index = 0; index < length; index++) {
            hash64 = mix64(hash64 ^ (source[offset + index] & 0xffL));
        }
        Fingerprint fingerprint = new Fingerprint(coder, byteLength, hash64);

        expungeLocals();
        long reserved = 0L;
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
                    } else if (existing.contentEqualsLatin1Bytes(
                            source, offset, length, coder)) {
                        return found(existing, reserved);
                    }
                }

                if (reserved != 0L || tryReserve(byteLength)) {
                    M3StringAtom created = null;
                    try {
                        long id = nextId(NEXT_LOCAL_ID, "M3 scalar ID");
                        created = M3StringAtom.localLatin1Bytes(
                                source, offset, length, coder, id, hash64);
                    } finally {
                        if (created == null) release(byteLength);
                    }
                    return retain(bucket, fingerprint, created, byteLength);
                }
            }
            reserved = reserveSlow(byteLength);
            if (reserved == 0L) return null;
        }
    }

    static M3String internChars(char[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (length == 0) return M3String.empty();

        boolean latin1 = String.COMPACT_STRINGS;
        if (latin1) {
            for (int index = 0; index < length; index++) {
                if (source[offset + index] > 0xff) {
                    latin1 = false;
                    break;
                }
            }
        }
        byte coder = latin1 ? String.LATIN1 : String.UTF16;
        if (coder == String.UTF16 && length > StringUTF16.MAX_LENGTH) {
            throw new OutOfMemoryError(
                    "UTF16 String size is " + length
                            + ", should be less than " + StringUTF16.MAX_LENGTH);
        }

        Lexicon active = lexicon;
        if (active != null && active.available()) {
            int row = active.find(source, offset, length);
            if (row >= 0) return M3String.whole(active.atom(row));
        }

        int byteLength = length << coder;
        long hash64 =
                mix64(0x9e3779b97f4a7c15L ^ coder ^ Integer.toUnsignedLong(byteLength));
        for (int index = 0; index < length; index++) {
            hash64 = mix64(hash64 ^ source[offset + index]);
        }
        Fingerprint fingerprint = new Fingerprint(coder, byteLength, hash64);

        expungeLocals();
        long reserved = 0L;
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
                    } else if (existing.contentEquals(source, offset, length, coder)) {
                        return found(existing, reserved);
                    }
                }

                if (reserved != 0L || tryReserve(byteLength)) {
                    M3StringAtom created = null;
                    try {
                        long id = nextId(NEXT_LOCAL_ID, "M3 scalar ID");
                        created = M3StringAtom.localChars(source, offset, length, coder, id, hash64);
                    } finally {
                        if (created == null) release(byteLength);
                    }
                    return retain(bucket, fingerprint, created, byteLength);
                }
            }
            reserved = reserveSlow(byteLength);
            if (reserved == 0L) return null;
        }
    }

    /**
     * Single-unit lane: bounded by construction (at most 65,536 distinct atoms, 128 KiB), so it is
     * accounted but never refused; the callers inside M3String compositions rely on a non-null result.
     */
    static M3String internUnit(char unit) {
        Lexicon active = lexicon;
        if (active != null && active.available()) {
            int row = active.findUnit(unit);
            if (row >= 0) return M3String.whole(active.atom(row));
        }

        byte coder =
                String.COMPACT_STRINGS && StringLatin1.canEncode(unit)
                        ? String.LATIN1
                        : String.UTF16;
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

    static M3String concat(M3String left, M3String right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length() == 0) return right;
        if (right.length() == 0) return left;
        // An M3 DAG may represent the largest legal logical String without a flat byte array.
        // Reject an unrepresentable combined length with the JDK-compatible size failure
        // before either adjacent-range coalescing or tuple Math.addExact can overflow.
        if ((long) left.length() + right.length() > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        if (left.owner() == right.owner() && left.end() == right.start()) {
            return M3String.range(
                    left.owner(), left.start(), Math.addExact(left.length(), right.length()));
        }
        return concatBalanced(left, right);
    }

    private static M3String concatBalanced(M3String left, M3String right) {
        int leftHeight = height(left);
        int rightHeight = height(right);
        if (leftHeight > rightHeight + 1) {
            M3StringTuple branch = (M3StringTuple) left.owner();
            return balance(branch.left, concatBalanced(branch.right, right));
        }
        if (rightHeight > leftHeight + 1) {
            M3StringTuple branch = (M3StringTuple) right.owner();
            return balance(concatBalanced(left, branch.left), branch.right);
        }
        return internTuple(left, right);
    }

    private static M3String balance(M3String left, M3String right) {
        int leftHeight = height(left);
        int rightHeight = height(right);
        if (leftHeight > rightHeight + 1) {
            M3StringTuple branch = (M3StringTuple) left.owner();
            if (height(branch.left) >= height(branch.right)) {
                return internTuple(branch.left, internTuple(branch.right, right));
            }
            M3StringTuple middle = (M3StringTuple) branch.right.owner();
            return internTuple(
                    internTuple(branch.left, middle.left),
                    internTuple(middle.right, right));
        }
        if (rightHeight > leftHeight + 1) {
            M3StringTuple branch = (M3StringTuple) right.owner();
            if (height(branch.right) >= height(branch.left)) {
                return internTuple(internTuple(left, branch.left), branch.right);
            }
            M3StringTuple middle = (M3StringTuple) branch.left.owner();
            return internTuple(
                    internTuple(left, middle.left),
                    internTuple(middle.right, branch.right));
        }
        return internTuple(left, right);
    }

    private static int height(M3String value) {
        M3StringOwner owner = value.owner();
        return value.start() == 0
                        && value.length() == owner.length
                        && owner instanceof M3StringTuple tuple
                ? tuple.height
                : 0;
    }

    /**
     * Parenthesization-independent tuple interning.
     *
     * <p>The O(1) route key is candidate-only. Exact normalized terminal M3 atom owner/range
     * coordinates decide canonical reuse, so route collisions affect lookup cost only.</p>
     */
    private static M3String internTuple(M3String left, M3String right) {
        expungeTuples();
        int totalLength = Math.addExact(left.length(), right.length());
        int javaHash =
                left.hashCodeValue() * M3String.pow31(right.length()) + right.hashCodeValue();
        byte coder = (byte) (left.coder() | right.coder());
        long routeKey = tupleRouteKey(left, right, javaHash, totalLength, coder);

        for (;;) {
            TupleBucket bucket = TUPLES.computeIfAbsent(routeKey, ignored -> new TupleBucket());
            synchronized (bucket) {
                if (bucket.retired) continue;
                for (Iterator<TupleRef> iterator = bucket.values.iterator(); iterator.hasNext();) {
                    TupleRef reference = iterator.next();
                    M3StringTuple existing = reference.get();
                    if (existing == null) {
                        iterator.remove();
                        RETAINED_TUPLES.decrementAndGet();
                    } else if (existing.length == totalLength
                            && existing.coder == coder
                            && existing.javaHash == javaHash
                            && sameLeafSequence(M3String.whole(existing), left, right)) {
                        return M3String.whole(existing);
                    }
                }
                long id = nextId(NEXT_TUPLE_ID, "M3 tuple ID");
                M3StringTuple created = new M3StringTuple(left, right, id, routeKey);
                // Retention cap: past it the tuple composes and lives with its String, uncanonical.
                if (RETAINED_TUPLES.incrementAndGet() <= MAX_RETAINED_TUPLES) {
                    bucket.values.add(new TupleRef(created, routeKey, bucket));
                } else {
                    RETAINED_TUPLES.decrementAndGet();
                }
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
                if (bucket.values.remove(reference)) RETAINED_TUPLES.decrementAndGet();
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

    static long retainedTuples() {
        expungeTuples();
        return RETAINED_TUPLES.get();
    }

    /** Fast budget reservation (CAS); the caller allocates only after it succeeds. */
    private static boolean tryReserve(long bytes) {
        for (;;) {
            long current = LOCAL_NATIVE_BYTES.get();
            if (bytes > MAX_LOCAL_BYTES - current) return false;
            if (LOCAL_NATIVE_BYTES.compareAndSet(current, current + bytes)) return true;
        }
    }

    private static void release(long bytes) {
        LOCAL_NATIVE_BYTES.addAndGet(-bytes);
    }

    /** A bucket hit while a slow-path reservation is held hands the reservation back. */
    private static M3String found(M3StringAtom existing, long reserved) {
        if (reserved != 0L) release(reserved);
        return M3String.whole(existing);
    }

    /** Publishes a freshly allocated local atom whose bytes are already reserved (bucket lock held). */
    private static M3String retain(
            LocalBucket bucket, Fingerprint fingerprint, M3StringAtom created, long bytes) {
        bucket.values.add(new LocalRef(created, fingerprint, bucket, bytes));
        return M3String.whole(created);
    }

    private static boolean reserveAfterDrain(long bytes) {
        expungeLocals();
        return tryReserve(bytes);
    }

    /**
     * Back-pressure in the shape of {@code java.nio.Bits.reserveMemory} (adapted: no bucket lock is
     * held, and the terminal step refuses with {@code 0} instead of throwing OutOfMemoryError).
     * Drains the local queue, waits for pending reference processing, triggers one collection and
     * retries with bounded exponential back-off. A failed cycle arms a one-second hold-off during
     * which further misses are refused without another collection. Interrupts are deferred.
     *
     * @return the reserved byte count, or {@code 0} when admission is refused
     */
    private static long reserveSlow(long bytes) {
        if (System.nanoTime() - nextSlowReserveNanos < 0L) return 0L;
        JavaLangRefAccess references = SharedSecrets.getJavaLangRefAccess();
        boolean interrupted = false;
        try {
            for (boolean active = true; active;) {
                try {
                    active = references.waitForReferenceProcessing();
                } catch (InterruptedException deferred) {
                    interrupted = true;
                }
                if (reserveAfterDrain(bytes)) return bytes;
            }
            System.gc();
            for (int sleeps = 0, sleep = 1;;) {
                if (reserveAfterDrain(bytes)) return bytes;
                if (sleeps >= MAX_SLEEPS) break;
                try {
                    if (!references.waitForReferenceProcessing()) {
                        Thread.sleep(sleep);
                        sleep <<= 1;
                        sleeps++;
                    }
                } catch (InterruptedException deferred) {
                    interrupted = true;
                }
            }
            nextSlowReserveNanos = System.nanoTime() + SLOW_RESERVE_HOLDOFF_NANOS;
            return 0L;
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
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

    private static long tupleRouteKey(
            M3String left, M3String right, int javaHash, int totalLength, byte coder) {
        long route = mix64(Integer.toUnsignedLong(javaHash));
        route = mix64(route ^ Long.rotateLeft(Integer.toUnsignedLong(totalLength), 17));
        route = mix64(route ^ coder);

        int edge = Math.min(4, totalLength);
        for (int index = 0; index < edge; index++) {
            route = mix64(route ^ joinedCharAt(left, right, index));
        }
        for (int index = Math.max(edge, totalLength - 4); index < totalLength; index++) {
            route = mix64(route ^ Long.rotateLeft(joinedCharAt(left, right, index), 23));
        }
        return route;
    }

    private static char joinedCharAt(M3String left, M3String right, int index) {
        int leftLength = left.length();
        return index < leftLength
                ? left.charAt(index)
                : right.charAt(index - leftLength);
    }

    private static boolean sameLeafSequence(
            M3String existing, M3String left, M3String right) {
        LeafCursor candidate = new LeafCursor(existing, null);
        LeafCursor requested = new LeafCursor(left, right);
        for (;;) {
            boolean candidateNext = candidate.next();
            boolean requestedNext = requested.next();
            if (candidateNext != requestedNext) return false;
            if (!candidateNext) return true;
            if (candidate.atom() != requested.atom()
                    || candidate.start() != requested.start()
                    || candidate.length() != requested.length()) {
                return false;
            }
        }
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
                release(retainedBytes);
            }
        }
    }

    private static final class LeafCursor {
        private final ArrayDeque<M3String> pending = new ArrayDeque<>();

        private M3StringAtom atom;
        private int start;
        private int length;

        private boolean buffered;
        private M3StringAtom bufferedAtom;
        private int bufferedStart;
        private int bufferedLength;

        private M3StringAtom rawAtom;
        private int rawStart;
        private int rawLength;

        LeafCursor(M3String first, M3String second) {
            if (second != null && second.length() != 0) pending.push(second);
            if (first != null && first.length() != 0) pending.push(first);
        }

        boolean next() {
            if (buffered) {
                atom = bufferedAtom;
                start = bufferedStart;
                length = bufferedLength;
                buffered = false;
            } else {
                if (!pullRaw()) return false;
                atom = rawAtom;
                start = rawStart;
                length = rawLength;
            }

            while (pullRaw()) {
                if (rawAtom == atom && start + length == rawStart) {
                    length = Math.addExact(length, rawLength);
                } else {
                    buffered = true;
                    bufferedAtom = rawAtom;
                    bufferedStart = rawStart;
                    bufferedLength = rawLength;
                    break;
                }
            }
            return true;
        }

        M3StringAtom atom() { return atom; }
        int start() { return start; }
        int length() { return length; }

        private boolean pullRaw() {
            while (!pending.isEmpty()) {
                M3String value = pending.pop();
                if (value.length() == 0) continue;
                if (value.owner() instanceof M3StringAtom scalar) {
                    rawAtom = scalar;
                    rawStart = value.start();
                    rawLength = value.length();
                    return true;
                }

                M3StringTuple tuple = (M3StringTuple) value.owner();
                int rangeStart = value.start();
                int rangeEnd = value.end();
                int leftLength = tuple.left.length();

                if (rangeEnd > leftLength) {
                    int rightStart = Math.max(0, rangeStart - leftLength);
                    int rightEnd = rangeEnd - leftLength;
                    pending.push(
                            M3String.range(
                                    tuple.right.owner(),
                                    Math.addExact(tuple.right.start(), rightStart),
                                    rightEnd - rightStart));
                }
                if (rangeStart < leftLength) {
                    int leftStart = rangeStart;
                    int leftEnd = Math.min(rangeEnd, leftLength);
                    pending.push(
                            M3String.range(
                                    tuple.left.owner(),
                                    Math.addExact(tuple.left.start(), leftStart),
                                    leftEnd - leftStart));
                }
            }
            return false;
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

        int findCodePoints(
                int[] value, int offset, int count, int utf16Length) {
            int low = 0;
            int high = offsets.length - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                int comparison =
                        compareCodePoints(
                                value,
                                offset,
                                count,
                                utf16Length,
                                offsets[middle],
                                lengths[middle]);
                if (comparison == 0) return middle;
                if (comparison < 0) high = middle - 1;
                else low = middle + 1;
            }
            return -1;
        }

        private int compareCodePoints(
                int[] value,
                int offset,
                int count,
                int utf16Length,
                int mappedOffset,
                int mappedLength) {
            int common = Math.min(utf16Length, mappedLength);
            int cpIndex = 0;
            int sourceUnit = 0;
            char pendingLow = 0;
            boolean hasPendingLow = false;

            for (int index = 0; index < common; index++) {
                char left;
                if (hasPendingLow) {
                    left = pendingLow;
                    hasPendingLow = false;
                } else {
                    int cp = value[offset + cpIndex++];
                    if (Character.isBmpCodePoint(cp)) {
                        left = (char) cp;
                    } else {
                        left = Character.highSurrogate(cp);
                        pendingLow = Character.lowSurrogate(cp);
                        hasPendingLow = true;
                    }
                }
                char right = mappedUnit(mappedOffset, index);
                if (left != right) return left - right;
                sourceUnit++;
            }
            return utf16Length - mappedLength;
        }

        int findCompactBytes(
                byte[] value, int sourceOffset, int logicalLength, byte sourceCoder) {
            int low = 0;
            int high = offsets.length - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                int common = Math.min(logicalLength, lengths[middle]);
                int comparison = 0;
                for (int index = 0; index < common; index++) {
                    char left =
                            sourceCoder == String.LATIN1
                                    ? (char) (value[sourceOffset + index] & 0xff)
                                    : StringUTF16.charAt(value, sourceOffset + index);
                    comparison = left - mappedUnit(offsets[middle], index);
                    if (comparison != 0) break;
                }
                if (comparison == 0) comparison = logicalLength - lengths[middle];
                if (comparison == 0) return middle;
                if (comparison < 0) high = middle - 1;
                else low = middle + 1;
            }
            return -1;
        }

        int findLatin1Bytes(byte[] value, int offset, int logicalLength) {
            int low = 0;
            int high = offsets.length - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                int common = Math.min(logicalLength, lengths[middle]);
                int comparison = 0;
                for (int index = 0; index < common; index++) {
                    comparison =
                            (value[offset + index] & 0xff)
                                    - mappedUnit(offsets[middle], index);
                    if (comparison != 0) break;
                }
                if (comparison == 0) comparison = logicalLength - lengths[middle];
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

        int find(char[] value, int offset, int logicalLength) {
            int low = 0;
            int high = offsets.length - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                int common = Math.min(logicalLength, lengths[middle]);
                int comparison = 0;
                for (int index = 0; index < common; index++) {
                    comparison = value[offset + index] - mappedUnit(offsets[middle], index);
                    if (comparison != 0) break;
                }
                if (comparison == 0) comparison = logicalLength - lengths[middle];
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
                                    String.COMPACT_STRINGS
                                                    && mappedLatin1(address, lengths[row], bigEndian)
                                            ? String.LATIN1
                                            : String.UTF16,
                                    javaHashes[row],
                                    id,
                                    M3StringPool.mix64(id ^ lengths[row]));
                    atoms[row] = result;
                }
                return result;
            }
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
