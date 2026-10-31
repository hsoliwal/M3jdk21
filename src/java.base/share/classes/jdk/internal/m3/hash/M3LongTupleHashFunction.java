/*
 * Changed by Synexia (2026-10-31): zero-allocation-hashing 0.16 (Apache License 2.0) absorbed into java.base
 * as jdk.internal.m3.hash; the changes are listed in src/java.base/share/legal/zero-allocation-hashing.md.
 */
package jdk.internal.m3.hash;


import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static jdk.internal.m3.hash.M3CharSequenceAccess.nativeCharSequenceAccess;
import static jdk.internal.m3.hash.M3ByteArrayAccess.*;
import static jdk.internal.m3.hash.M3Util.*;

/**
 * Tuple hash function producing more than 64-bit hash code into a result array of type
 * {@code long[]} from any byte sequences. See {@link LongHashFunction} for the definition of byte
 * sequence semantics and {@link ByteOrder} requirements for implementations.
 *
 * <p>Every {@link LongHashFunction} hash method has two corresponding ones in this class for different
 * allocation strategies:
 * <ul>
 *     <li><strong>{@code void hash(input..., long[] result)}</strong>
 *
 *     <p>will store the hash results in array {@code result[0 .. newResultArray().length-1]}, and
 *     throws exceptions when {@code result == null} or the length of the array is less than
 *     {@code newResultArray().length}. {@link #newResultArray} method should always be used to
 *     create resuable result arrays to avoid exceptions. See {@link #hashLong(long, long[])}.
 *
 *     <p><b>Warning:</b> A single allocation occurs only at the begining of some runtime scope, so
 *     it could be called <strong>Almost-Zero-Allocation-Hashing</strong>.</li>
 *
 *     <li><b>{@code long[] hash(input...)}</b>
 *
 *     <p>will allocate and return an array containing the results.
 *
 *     <p><b>Warning:</b> Methods with this form
 *     <strong>always perform exactly one allocation</strong> for the result array in each
 *     invocation, that is <strong>One-Allocation-Hashing</strong>. So prefer the first form as much
 *     as possible.</li>
 * </ul>
 *
 * <h2>Subclassing</h2>
 * To implement a specific hash function algorithm resulting more than 64 bits results, this class
 * should be subclassed. Only methods with resued result array that accept single primitives,
 * {@link #hashVoid(long[])} and {@link #hash(Object, Access, long, long, long[])} should be
 * implemented; other have default implementations which in the end delegate to
 * {@link #hash(Object, Access, long, long, long[])} abstract method.
 *
 * <p>The {@link #bitsLength} method should also be implemented, returning the actual number of bits
 * in the result array. The bits length must be greater than 64, otherwise just use the
 * {@link LongHashFunction} interface. And the length should also be a positive multiple of 8.
 *
 * <p>Also see {@link LongHashFunction} for additional information about subclassing andd access.
 *
 * @see LongHashFunction LongHashFunction
 */
public abstract class M3LongTupleHashFunction implements Serializable {
    private static final long serialVersionUID = 0L;

    // Implementations
    //

    /**
     * Returns a 128-bit hash function implementing
     * <a href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">MurmurHash3
     * algorithm</a> without seed values. This implementation produces equal results for equal input
     * on platforms with different {@link ByteOrder}, but is slower on big-endian platforms than on
     * little-endian.
     *
     * @see #murmur_3(long)
     */
    public static M3LongTupleHashFunction murmur_3() {
        return M3MurmurHash_3.asLongTupleHashFunctionWithoutSeed();
    }

    /**
     * Returns a 128-bit hash function implementing
     * <a href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">MurmurHash3
     * algorithm</a> with the given seed value. This implementation produces equal results for equal
     * input on platforms with different {@link ByteOrder}, but is slower on big-endian platforms
     * than on little-endian.
     *
     * @see #murmur_3()
     */
    public static M3LongTupleHashFunction murmur_3(final long seed) {
        return M3MurmurHash_3.asLongTupleHashFunctionWithSeed(seed);
    }

    /**
     * Returns a 128-bit hash function implementing
     * <a href="https://github.com/Cyan4973/xxHash">XXH3 128bit algorithm</a> without seed values
     * (0 is used as default seed value). This implementation produces equal results for equal input
     * on platforms with different {@link ByteOrder}, but is slower on big-endian platforms than on
     * little-endian.
     *
     * @see #xx128(long)
     */
    public static M3LongTupleHashFunction xx128() {
        return M3XXH3.asLongTupleHashFunctionWithoutSeed();
    }

    /**
     * Returns a hash function implementing
     * <a href="https://github.com/Cyan4973/xxHash">XXH3 128bit algorithm</a> with the given seed
     * value. This implementation produces equal results for equal input on platforms with different
     * {@link ByteOrder}, but is slower on big-endian platforms than on little-endian.
     *
     * @see #xx128()
     */
    public static M3LongTupleHashFunction xx128(final long seed) {
        return M3XXH3.asLongTupleHashFunctionWithSeed(seed);
    }

    /**
     * Constructor for use in subclasses.
     */
    protected M3LongTupleHashFunction() {}

    // Public API
    //

    /**
     * Returns the actual number of bits in a result array; a positive multiple of 8.
     */
    public abstract int bitsLength();

    /**
     * Returns a new-allocated result array.
     *
     * If {@code bitsLength()} returns non-multiple of 64, the implementation of this method should
     * round-up the length to a multiple of 64 for allocating the {@code long} array.
     */
    public long[] newResultArray() {
        return new long[(bitsLength() + 63) / 64];
    }

    /**
     * Computes the hash code for the given {@code long} value, and store the results in the
     * {@code result} array; this method is consistent with {@code LongTupleHashFunction} methods
     * that accept sequences of bytes, assuming the {@code input} value is interpreted in
     * {@linkplain ByteOrder#nativeOrder() native} byte order. For example, the result of
     * {@code hashLong(v, result)} call is identical to the result of
     * {@code hashLongs(new long[] {v}, result)} call for any {@code long} value.
     *
     * <p>The {@code result} array should be always created by {@link #newResultArray} method. When
     * storing, the {@code result[0 .. newResultArray().length-1]} will be accessed, the rest
     * elements of the array will not be touched when
     * {@code result.length > newResultArray().length]}.
     *
     * @throws NullPointerException if {@code result == null}
     * @throws IllegalArgumentException if {@code result.length < newResultArray().length}
     */
    public abstract void hashLong(long input, long[] result);

    /**
     * The result array is allocated on the fly, and no exceptions will be thrown.
     *
     * @see #hashLong(long, long[])
     */
    public long[] hashLong(final long input) {
        final long[] result = newResultArray();
        hashLong(input, result);
        return result;
    }

    /**
     * Computes the hash code for the given {@code int} value, and store the results in the
     * {@code result} array; this method is consistent with {@code LongTupleHashFunction} methods
     * that accept sequences of bytes, assuming the {@code input} value is interpreted in
     * {@linkplain ByteOrder#nativeOrder() native} byte order. For example, the result of
     * {@code hashInt(v, result)} call is identical to the result of
     * {@code hashInts(new int[] {v}, result)} call for any {@code int} value.
     *
     * <p>The {@code result} array should be always created by {@link #newResultArray} method. When
     * storing, the {@code result[0 .. newResultArray().length-1]} will be accessed, the rest
     * elements of the array will not be touched when
     * {@code result.length > newResultArray().length]}.
     *
     * @throws NullPointerException if {@code result == null}
     * @throws IllegalArgumentException if {@code result.length < newResultArray().length}
     */
    public abstract void hashInt(int input, long[] result);

    /**
     * The result array is allocated on the fly, and no exceptions will be thrown.
     *
     * @see #hashInt(int, long[])
     */
    public long[] hashInt(final int input) {
        final long[] result = newResultArray();
        hashInt(input, result);
        return result;
    }

    /**
     * Computes the hash code for the given {@code short} value, and store the results in the
     * {@code result} array; this method is consistent with {@code LongTupleHashFunction} methods
     * that accept sequences of bytes, assuming the {@code input} value is interpreted in
     * {@linkplain ByteOrder#nativeOrder() native} byte order. For example, the result of
     * {@code hashShort(v, result)} call is identical to the result of
     * {@code hashShorts(new short[] {v}, result)} call for any {@code short} value.
     *
     * <p>The {@code result} array should be always created by {@link #newResultArray} method. When
     * storing, the {@code result[0 .. newResultArray().length-1]} will be accessed, the rest
     * elements of the array will not be touched when
     * {@code result.length > newResultArray().length]}.
     *
     * @throws NullPointerException if {@code result == null}
     * @throws IllegalArgumentException if {@code result.length < newResultArray().length}
     */
    public abstract void hashShort(short input, long[] result);

    /**
     * The result array is allocated on the fly, and no exceptions will be thrown.
     *
     * @see #hashShort(short, long[])
     */
    public long[] hashShort(final short input) {
        final long[] result = newResultArray();
        hashShort(input, result);
        return result;
    }

    /**
     * Computes the hash code for the given {@code char} value, and store the results in the
     * {@code result} array; this method is consistent with {@code LongTupleHashFunction} methods
     * that accept sequences of bytes, assuming the {@code input} value is interpreted in
     * {@linkplain ByteOrder#nativeOrder() native} byte order. For example, the result of
     * {@code hashChar(v, result)} call is identical to the result of
     * {@code hashChars(new char[] {v}, result)} call for any {@code char} value.
     *
     * <p>The {@code result} array should be always created by {@link #newResultArray} method. When
     * storing, the {@code result[0 .. newResultArray().length-1]} will be accessed, the rest
     * elements of the array will not be touched when
     * {@code result.length > newResultArray().length]}.
     *
     * @throws NullPointerException if {@code result == null}
     * @throws IllegalArgumentException if {@code result.length < newResultArray().length}
     */
    public abstract void hashChar(char input, long[] result);

    /**
     * The result array is allocated on the fly, and no exceptions will be thrown.
     *
     * @see #hashChar(char, long[])
     */
    public long[] hashChar(final char input) {
        final long[] result = newResultArray();
        hashChar(input, result);
        return result;
    }

    /**
     * Computes the hash code for the given {@code byte} value, and store the results in the
     * {@code result} array; this method is consistent with {@code LongTupleHashFunction} methods
     * that accept sequences of bytes, assuming the {@code input} value is the first and only byte.
     * For example, the result of {@code hashByte(v, result)} call is identical to the result of
     * {@code hashBytes(new byte[] {v}, result)} call for any {@code byte} value.
     *
     * <p>The {@code result} array should be always created by {@link #newResultArray} method. When
     * storing, the {@code result[0 .. newResultArray().length-1]} will be accessed, the rest
     * elements of the array will not be touched when
     * {@code result.length > newResultArray().length]}.
     *
     * @throws NullPointerException if {@code result == null}
     * @throws IllegalArgumentException if {@code result.length < newResultArray().length}
     */
    public abstract void hashByte(byte input, long[] result);

    /**
     * The result array is allocated on the fly, and no exceptions will be thrown.
     *
     * @see #hashByte(byte, long[])
     */
    public long[] hashByte(final byte input) {
        final long[] result = newResultArray();
        hashByte(input, result);
        return result;
    }

    /**
     * Computes the hash code for the empty (zero-length) bytes sequence, and store the results in
     * the {@code result} array. For example, the result of {@code hashVoid(result)} call is
     * identical to the result of {@code hashBytes(new byte[0], result)} call.
     *
     * <p>The {@code result} array should be always created by {@link #newResultArray} method. When
     * storing, the {@code result[0 .. newResultArray().length-1]} will be accessed, the rest
     * elements of the array will not be touched when
     * {@code result.length > newResultArray().length]}.
     *
     * @throws NullPointerException if {@code result == null}
     * @throws IllegalArgumentException if {@code result.length < newResultArray().length}
     */
    public abstract void hashVoid(long[] result);

    /**
     * The result array is allocated on the fly, and no exceptions will be thrown.
     *
     * @see #hashVoid(long[])
     */
    public long[] hashVoid() {
        final long[] result = newResultArray();
        hashVoid(result);
        return result;
    }

    /**
     * Computes the hash code for the given {@code input} object starting from the given offset, and
     * store the results in the {@code result} array. The abstraction of input as ordered byte
     * sequence and "offset within the input" is defined by the given {@code access} strategy.
     *
     * <p>This method doesn't promise to throw a {@code RuntimeException} if
     * {@code [off, off + len - 1]} subsequence exceeds the bounds of the bytes sequence, defined by
     * {@code access} strategy for the given {@code input}, so use this method with caution.
     *
     * <p>The {@code result} array should be always created by {@link #newResultArray} method. When
     * storing, the {@code result[0 .. newResultArray().length-1]} will be accessed, the rest
     * elements of the array will not be touched when
     * {@code result.length > newResultArray().length]}.
     *
     * @param input the object to read bytes from
     * @param access access which defines the abstraction of the given input
     *               as ordered byte sequence
     * @param off offset to the first byte of the subsequence to hash
     * @param len length of the subsequence to hash
     * @param result the container array for storing the hash results,
     *               should be alloced by {@link #newResultArray}
     * @param <T> the type of the input
     * @throws NullPointerException if {@code result == null}
     * @throws IllegalArgumentException if {@code result.length < newResultArray().length}
     */
    public abstract <T> void hash(T input, M3Access<T> access,
                                  long off, long len, long[] result);

    /**
     * The result array is allocated on the fly, and no exceptions will be thrown.
     *
     * @see #hash(Object, Access, long, long, long[])
     */
    public <T> long[] hash(final T input, final M3Access<T> access,
                           final long off, final long len) {
        final long[] result = newResultArray();
        hash(input, access, off, len, result);
        return result;
    }

    /**
     * Shortcut for {@link #hashBytes(byte[], int, int, long[]) hashBytes(input, 0, input.length, result)}.
     */
    public void hashBytes(final byte[] input, final long[] result) {
        unsafeHash(this, input, BYTE_BASE, input.length, result);
    }

    /**
     * The result array is allocated on the fly, and no exceptions will be thrown.
     *
     * @see #hashBytes(byte[], long[])
     */
    public long[] hashBytes(final byte[] input) {
        final long[] result = newResultArray();
        unsafeHash(this, input, BYTE_BASE, input.length, result);
        return result;
    }

    /**
     * Computes the hash code for the specified subsequence of the given {@code byte} array, and
     * store the results in the {@code result} array.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long, long[])}
     * method using {@linkplain Access#unsafe() unsafe} {@code Access}.
     *
     * <p>The {@code result} array should be always created by {@link #newResultArray} method. When
     * storing, the {@code result[0 .. newResultArray().length-1]} will be accessed, the rest
     * elements of the array will not be touched when
     * {@code result.length > newResultArray().length]}.
     *
     * @param input the array to read data from
     * @param off index of the first {@code byte} in the subsequence to hash
     * @param len length of the subsequence to hash
     * @param result the container array for storing the hash results,
     *               should be alloced by {@link #newResultArray}
     * @throws NullPointerException if {@code result == null}
     * @throws IllegalArgumentException if {@code result.length < newResultArray().length}
     * @throws IllegalArgumentException if {@code off < 0} or {@code off + len > input.length}
     *                                  or {@code len < 0}
     */
    public void hashBytes(final byte[] input, final int off, final int len, final long[] result) {
        checkArrayOffs(input.length, off, len);
        unsafeHash(this, input, BYTE_BASE + off, len, result);
    }

    /**
     * The result array is allocated on the fly.
     *
     * @see #hashBytes(byte[], int, int, long[])
     */
    public long[] hashBytes(final byte[] input, final int off, final int len) {
        checkArrayOffs(input.length, off, len);
        final long[] result = newResultArray();
        unsafeHash(this, input, BYTE_BASE + off, len, result);
        return result;
    }

    /**
     * Shortcut for {@link #hashBytes(ByteBuffer, int, int, long[])
     * hashBytes(input, input.position(), input.remaining(), result)}.
     */
    public void hashBytes(final ByteBuffer input, final long[] result) {
        hashByteBuffer(this, input, input.position(), input.remaining(), result);
    }

    /**
     * The result array is allocated on the fly, and no exceptions will be thrown.
     *
     * @see #hashBytes(ByteBuffer, long[])
     */
    public long[] hashBytes(final ByteBuffer input) {
        final long[] result = newResultArray();
        hashByteBuffer(this, input, input.position(), input.remaining(), result);
        return result;
    }

    /**
     * Computes the hash code for the specified subsequence of the given {@code ByteBuffer}, and
     * store the results in the {@code result} array.
     *
     * <p>This method doesn't alter the state (mark, position, limit or order) of the given
     * {@code ByteBuffer}.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long, long[])}
     * method using {@linkplain Access#unsafe() unsafe} {@code Access}.
     *
     * <p>The {@code result} array should be always created by {@link #newResultArray} method. When
     * storing, the {@code result[0 .. newResultArray().length-1]} will be accessed, the rest
     * elements of the array will not be touched when
     * {@code result.length > newResultArray().length]}.
     *
     * @param input the buffer to read bytes from
     * @param off index of the first {@code byte} in the subsequence to hash
     * @param len length of the subsequence to hash
     * @param result the container array for storing the hash results,
     *               should be alloced by {@link #newResultArray}
     * @throws NullPointerException if {@code result == null}
     * @throws IllegalArgumentException if {@code result.length < newResultArray().length}
     * @throws IllegalArgumentException if {@code off < 0} or {@code off + len > input.length}
     *                                  or {@code len < 0}
     */
    public void hashBytes(final ByteBuffer input,
                          final int off, final int len, final long[] result) {
        checkArrayOffs(input.capacity(), off, len);
        hashByteBuffer(this, input, off, len, result);
    }

    /**
     * The result array is allocated on the fly.
     *
     * @see #hashBytes(ByteBuffer, int, int, long[])
     */
    public long[] hashBytes(final ByteBuffer input, final int off, final int len) {
        checkArrayOffs(input.capacity(), off, len);
        final long[] result = newResultArray();
        hashByteBuffer(this, input, off, len, result);
        return result;
    }

    /**
     * Shortcut for
     * {@link #hashChars(String, int, int, long[]) hashChars(input, 0, input.length(), result)}.
     */
    public void hashChars(final String input, final long[] result) {
        hashNativeChars(this, input, 0, input.length(), result);
    }

    /**
     * The result array is allocated on the fly, and no exceptions will be thrown.
     *
     * @see #hashChars(String, long[])
     */
    public long[] hashChars(final String input) {
        final long[] result = newResultArray();
        hashNativeChars(this, input, 0, input.length(), result);
        return result;
    }

    /**
     * Computes the hash code for bytes of the specified subsequence of the given {@code String}'s
     * underlying {@code char} array or {@code byte} array.
     *
     * <p>Default implementation could either delegate to
     * {@link #hash(Object, Access, long, long, long[])} using
     * {@link Access#toNativeCharSequence()}, or to {@link #hashChars(char[], int, int, long[])}.
     *
     * <p>The {@code result} array should be always created by {@link #newResultArray} method. When
     * storing, the {@code result[0 .. newResultArray().length-1]} will be accessed, the rest
     * elements of the array will not be touched when
     * {@code result.length > newResultArray().length]}.
     *
     * @param input the string which bytes to hash
     * @param off index of the first {@code char} in the subsequence to hash
     * @param len length of the subsequence to hash, in chars (i.e. the length of the bytes sequence
     *            to hash is {@code len * 2L})
     * @param result the container array for storing the hash results,
     *               should be alloced by {@link #newResultArray}
     * @throws NullPointerException if {@code result == null}
     * @throws IllegalArgumentException if {@code result.length < newResultArray().length}
     * @throws IllegalArgumentException if {@code off < 0} or {@code off + len > input.length}
     *                                  or {@code len < 0}
     */
    public void hashChars(final String input, final int off, final int len, final long[] result) {
        checkArrayOffs(input.length(), off, len);
        hashNativeChars(this, input, off, len, result);
    }

    /**
     * The result array is allocated on the fly.
     *
     * @see #hashChars(String, int, int, long[])
     */
    public long[] hashChars(final String input, final int off, final int len) {
        checkArrayOffs(input.length(), off, len);
        final long[] result = newResultArray();
        hashNativeChars(this, input, off, len, result);
        return result;
    }

    /**
     * Shortcut for
     * {@link #hashChars(CharSequence, int, int, long[]) hashChars(input, 0, input.length(), result)}.
     */
    public <T extends CharSequence> void hashChars(final T input, final long[] result) {
        hashNativeChars(this, input, 0, input.length(), result);
    }

    /**
     * The result array is allocated on the fly, and no exceptions will be thrown.
     *
     * @see #hashChars(CharSequence, long[])
     */
    public <T extends CharSequence> long[] hashChars(final T input) {
        final long[] result = newResultArray();
        hashNativeChars(this, input, 0, input.length(), result);
        return result;
    }

    /**
     * Computes the hash code for bytes of the specified subsequence of the given
     * {@code CharSequence}'s underlying {@code char} array.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long, long[])}
     * method using {@link Access#toNativeCharSequence()}.
     *
     * <p>The {@code result} array should be always created by {@link #newResultArray} method. When
     * storing, the {@code result[0 .. newResultArray().length-1]} will be accessed, the rest
     * elements of the array will not be touched when
     * {@code result.length > newResultArray().length]}.
     *
     * @param input the char sequence which bytes to hash
     * @param off index of the first {@code char} in the subsequence to hash
     * @param len length of the subsequence to hash, in chars (i.e. the length of the bytes sequence
     *            to hash is {@code len * 2L})
     * @param result the container array for storing the hash results,
     *               should be alloced by {@link #newResultArray}
     * @throws NullPointerException if {@code result == null}
     * @throws IllegalArgumentException if {@code result.length < newResultArray().length}
     * @throws IllegalArgumentException if {@code off < 0} or {@code off + len > input.length}
     *                                  or {@code len < 0}
     */
    public <T extends CharSequence> void hashChars(final T input, final int off, final int len,
                                                   final long[] result) {
        checkArrayOffs(input.length(), off, len);
        hashNativeChars(this, input, off, len, result);
    }

    /**
     * The result array is allocated on the fly.
     *
     * @see #hashChars(CharSequence, int, int, long[])
     */
    public <T extends CharSequence> long[] hashChars(final T input, final int off, final int len) {
        checkArrayOffs(input.length(), off, len);
        final long[] result = newResultArray();
        hashNativeChars(this, input, off, len, result);
        return result;
    }

    // Internal helper
    //
    private static final M3Access<byte[]> OBJECT_ACCESS = M3ByteArrayAccess.INSTANCE;
    private static final M3Access<CharSequence> CHAR_SEQ_ACCESS = nativeCharSequenceAccess();
    private static final M3Access<ByteBuffer> BYTE_BUF_ACCESS = M3ByteBufferAccess.INSTANCE;

    //FLLDCIM: HPCPOP - FACADE_ENTRY_CHECK v1: every byte[] reaches the kernels here; checked before the unchecked short-name reads
    private static void unsafeHash(final M3LongTupleHashFunction f, final byte[] input,
                                   final long off, final long len, final long[] result) {
        java.util.Objects.checkFromIndexSize(off, len, input.length);
        f.hash(input, OBJECT_ACCESS, off, len, result);
    }

    private static void hashByteBuffer(final M3LongTupleHashFunction f, final ByteBuffer input,
                                       final int off, final int len, final long[] result) {
        if (input.hasArray()) {
            unsafeHash(f, input.array(), BYTE_BASE + input.arrayOffset() + off, len, result);
        } else {
            f.hash(input, BYTE_BUF_ACCESS, off, len, result);
        }
    }

    static void hashNativeChars(final M3LongTupleHashFunction f, final CharSequence input,
                                final int off, final int len, final long[] result) {
        f.hash(input, CHAR_SEQ_ACCESS, off * 2L, len * 2L, result);
    }
}
