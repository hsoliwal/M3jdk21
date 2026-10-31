/*
 * Copyright 2014 Higher Frequency Trading http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Changed by Synexia (2026-10-31): zero-allocation-hashing 0.16 (Apache License 2.0) absorbed into java.base
 * as jdk.internal.m3.hash; the changes are listed in src/java.base/share/legal/zero-allocation-hashing.md.
 */
package jdk.internal.m3.hash;


import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.nio.ByteOrder.nativeOrder;
import static jdk.internal.m3.hash.M3CharSequenceAccess.nativeCharSequenceAccess;
import static jdk.internal.m3.hash.M3ByteArrayAccess.*;
import static jdk.internal.m3.hash.M3Util.*;

/**
 * Hash function producing {@code long}-valued result from byte sequences of any length and
 * a plenty of different sources which "feels like byte sequences". Except {@link
 * #hashBytes(byte[])}, {@link #hashBytes(ByteBuffer)} (with their "sliced" versions) and
 * {@link #hashMemory(long, long)} methods, which actually accept byte sequences, notion of byte
 * sequence is defined as follows:
 * <ul>
 *     <li>For methods accepting arrays of Java primitives, {@code String}s and
 *     {@code StringBuilder}s, byte sequence is how the input's bytes are actually lay in memory.
 *     </li>
 *     <li>For methods accepting single primitive values, byte sequence is how this primitive
 *     would be put into memory with {@link ByteOrder#nativeOrder() native} byte order, or
 *     equivalently, {@code hashXxx(primitive)} has always the same result as {@code
 *     hashXxxs(new xxx[] {primitive})}, where "xxx" is any Java primitive type name.</li>
 *     <li>For {@link #hash(Object, Access, long, long)} method byte sequence abstraction
 *     is defined by the given {@link Access} strategy to the given object.</li>
 * </ul>
 *
 * <p>Hash function implementation could either produce equal results for equal input on platforms
 * with different {@link ByteOrder}, favoring one byte order in terms of performance, or different
 * results, but performing equally good. This choice should be explicitly documented for all
 * {@code LongHashFunction} implementations.
 *
 * <h2>Subclassing</h2>
 * To implement a specific hash function algorithm, this class should be subclassed. Only methods
 * that accept single primitives, {@link #hashVoid()} and {@link #hash(Object, Access, long, long)}
 * should be implemented; other have default implementations which in the end delegate to
 * {@link #hash(Object, Access, long, long)} abstract method.
 *
 * <p>Notes about how exactly methods with default implementations are implemented in doc comments
 * are given for information and could be changed at any moment. However, it could hardly cause
 * any issues with subclassing, except probably little performance degradation. Methods documented
 * as "shortcuts" could either delegate to the referenced method or delegate directly to the method
 * to which the referenced method delegates.
 *
 *<p>{@code LongHashFunction} implementations shouldn't assume that {@code Access} strategies
 * do defensive checks, and access only bytes within the requested range.
 */
public abstract class M3LongHashFunction implements Serializable {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Bytes of a char: char offsets and lengths in bytes. */
    private static final long CHAR_BYTES = 2L;

    private static final long serialVersionUID = 0L;

    /**
     * Returns a hash function implementing
     * <a href="https://github.com/google/cityhash/blob/8af9b8c2b889d80c22d6bc26ba0df1afb79a30db/src/city.cc">
     * CityHash64 algorithm, version 1.1</a> without seed values. This implementation produce
     * equal results for equal input on platforms with different {@link ByteOrder}, but is slower
     * on big-endian platforms than on little-endian.
     *
     * @see #city_1_1(long)
     * @see #city_1_1(long, long)
     */
    public static M3LongHashFunction city_1_1() {
        return M3CityAndFarmHash_1_1.asLongHashFunctionWithoutSeed();
    }

    /**
     * Returns a hash function implementing
     * <a href="https://github.com/google/cityhash/blob/8af9b8c2b889d80c22d6bc26ba0df1afb79a30db/src/city.cc">
     * CityHash64 algorithm, version 1.1</a> using the given seed value. This implementation produce
     * equal results for equal input on platforms with different {@link ByteOrder}, but is slower
     * on big-endian platforms than on little-endian.
     *
     * @see #city_1_1()
     * @see #city_1_1(long, long)
     */
    public static M3LongHashFunction city_1_1(long seed) {
        return M3CityAndFarmHash_1_1.asLongHashFunctionWithSeed(seed);
    }

    /**
     * Returns a hash function implementing
     * <a href="https://github.com/google/cityhash/blob/8af9b8c2b889d80c22d6bc26ba0df1afb79a30db/src/city.cc">
     * CityHash64 algorithm, version 1.1</a> using the two given seed values. This implementation
     * produce equal results for equal input on platforms with different {@link ByteOrder}, but
     * is slower on big-endian platforms than on little-endian.
     *
     * @see #city_1_1()
     * @see #city_1_1(long)
     */
    public static M3LongHashFunction city_1_1(long seed0, long seed1) {
        return M3CityAndFarmHash_1_1.asLongHashFunctionWithTwoSeeds(seed0, seed1);
    }

    /**
     * Returns a hash function implementing so-called
     * <a href="https://github.com/google/farmhash/blob/a371645d2caa1685541d9963b94751c23b235c72/dev/farmhashna.cc">
     * farmhashna algorithm</a>, without seed values. This implementation produces equal results for
     * equal input on platforms with different {@link ByteOrder}, but is slower on big-endian
     * platforms than on little-endian.
     *
     * <p>{@code farmhashna} was introduced in FarmHash 1.0. For inputs shorter than 32 bytes it's
     * output is equivalent to {@link #city_1_1()} output.
     *
     * @see #farmNa(long)
     * @see #farmNa(long, long)
     */
    public static M3LongHashFunction farmNa() {
        return M3CityAndFarmHash_1_1.naWithoutSeeds();
    }

    /**
     * Returns a hash function implementing so-called
     * <a href="https://github.com/google/farmhash/blob/a371645d2caa1685541d9963b94751c23b235c72/dev/farmhashna.cc">
     * farmhashna algorithm</a>, using the given seed value. This implementation produces equal
     * results for equal input on platforms with different {@link ByteOrder}, but is slower on
     * big-endian platforms than on little-endian.
     *
     * <p>{@code farmhashna} was introduced in FarmHash 1.0. For inputs shorter than 32 bytes it's
     * output is equivalent to {@link #city_1_1(long)} output.
     *
     * @see #farmNa()
     * @see #farmNa(long, long)
     */
    public static M3LongHashFunction farmNa(long seed) {
        return M3CityAndFarmHash_1_1.naWithSeed(seed);
    }

    /**
     * Returns a hash function implementing so-called
     * <a href="https://github.com/google/farmhash/blob/a371645d2caa1685541d9963b94751c23b235c72/dev/farmhashna.cc">
     * farmhashna algorithm</a>, using the two given seed values. This implementation produces equal
     * results for equal input on platforms with different {@link ByteOrder}, but is slower on
     * big-endian platforms than on little-endian.
     *
     * <p>{@code farmhashna} was introduced in FarmHash 1.0. For inputs shorter than 32 bytes it's
     * output is equivalent to {@link #city_1_1(long, long)} output.
     *
     * @see #farmNa()
     * @see #farmNa(long)
     */
    public static M3LongHashFunction farmNa(long seed0, long seed1) {
        return M3CityAndFarmHash_1_1.naWithSeeds(seed0, seed1);
    }

    /**
     * Returns a hash function implementing so-called
     * <a href="https://github.com/google/farmhash/blob/34c13ddfab0e35422f4c3979f360635a8c050260/dev/farmhashuo.cc">
     * farmhashuo algorithm</a> without seed values. This implementation produces equal results for
     * equal input on platforms with different {@link ByteOrder}, but is slower on big-endian
     * platforms than on little-endian.
     *
     * <p>{@code farmhashuo} was introduced in FarmHash 1.1.
     *
     * @see #farmUo(long)
     * @see #farmUo(long, long)
     */
    public static M3LongHashFunction farmUo() {
        return M3CityAndFarmHash_1_1.uoWithoutSeeds();
    }

    /**
     * Returns a hash function implementing so-called
     * <a href="https://github.com/google/farmhash/blob/34c13ddfab0e35422f4c3979f360635a8c050260/dev/farmhashuo.cc">
     * farmhashuo algorithm</a> with the given seed value. This implementation produces equal results
     * for equal input on platforms with different {@link ByteOrder}, but is slower on big-endian
     * platforms than on little-endian.
     *
     * <p>{@code farmhashuo} was introduced in FarmHash 1.1.
     *
     * @see #farmUo()
     * @see #farmUo(long, long)
     */
    public static M3LongHashFunction farmUo(long seed) {
        return M3CityAndFarmHash_1_1.uoWithSeed(seed);
    }

    /**
     * Returns a hash function implementing so-called
     * <a href="https://github.com/google/farmhash/blob/34c13ddfab0e35422f4c3979f360635a8c050260/dev/farmhashuo.cc">
     * farmhashuo algorithm</a> with the two given seed values. This implementation produces equal
     * results for equal input on platforms with different {@link ByteOrder}, but is slower on
     * big-endian platforms than on little-endian.
     *
     * <p>{@code farmhashuo} was introduced in FarmHash 1.1.
     *
     * @see #farmUo()
     * @see #farmUo(long)
     */
    public static M3LongHashFunction farmUo(long seed0, long seed1) {
        return M3CityAndFarmHash_1_1.uoWithSeeds(seed0, seed1);
    }

    /**
     * Returns a 64-bit hash function implementing
     * <a href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">MurmurHash3
     * algorithm</a> without seed values. This implementation produces equal results for equal input
     * on platforms with different {@link ByteOrder}, but is slower on big-endian platforms than on
     * little-endian.
     *
     * @see #murmur_3(long)
     */
    public static M3LongHashFunction murmur_3() {
        return M3MurmurHash_3.asLongHashFunctionWithoutSeed();
    }

    /**
     * Returns a 64-bit hash function implementing
     * <a href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">MurmurHash3
     * algorithm</a> with the given seed value. This implementation produces equal results for equal
     * input on platforms with different {@link ByteOrder}, but is slower on big-endian platforms
     * than on little-endian.
     *
     * @see #murmur_3()
     */
    public static M3LongHashFunction murmur_3(long seed) {
        return M3MurmurHash_3.asLongHashFunctionWithSeed(seed);
    }

    /**
     * Returns a hash function implementing <a href="https://github.com/Cyan4973/xxHash">xxHash
     * algorithm</a> without a seed value (0 is used as default seed value). This implementation
     * produces equal results for equal input on platforms with different {@link
     * ByteOrder}, but is slower on big-endian platforms than on little-endian.
     *
     * @see #xx(long)
     */
    public static M3LongHashFunction xx() {
        return M3XxHash.asLongHashFunctionWithoutSeed();
    }

    /**
     * Returns a hash function implementing <a href="https://github.com/Cyan4973/xxHash">xxHash
     * algorithm</a> with the given seed value. This implementation produces equal results for equal
     * input on platforms with different {@link ByteOrder}, but is slower on big-endian platforms
     * than on little-endian.
     *
     * @see #xx()
     */
    public static M3LongHashFunction xx(long seed) {
        return M3XxHash.asLongHashFunctionWithSeed(seed);
    }

    /**
     * Returns a hash function implementing <a href="https://github.com/Cyan4973/xxHash">XXH3 64bit
     * algorithm</a> without a seed value (0 is used as default seed value). This implementation
     * produces equal results for equal input on platforms with different {@link
     * ByteOrder}, but is slower on big-endian platforms than on little-endian.
     *
     * @see #xx3(long)
     */
    public static M3LongHashFunction xx3() {
        return M3XXH3.asLongHashFunctionWithoutSeed();
    }

    /**
     * Returns a hash function implementing <a href="https://github.com/Cyan4973/xxHash">XXH3 64bit
     * algorithm</a> with the given seed value. This implementation produces equal results for equal
     * input on platforms with different {@link ByteOrder}, but is slower on big-endian platforms
     * than on little-endian.
     *
     * @see #xx3()
     */
    public static M3LongHashFunction xx3(final long seed) {
        return M3XXH3.asLongHashFunctionWithSeed(seed);
    }

    /**
     * Returns a hash function implementing <a href="https://github.com/Cyan4973/xxHash">XXH128 low
     * 64bit algorithm</a> without a seed value (0 is used as default seed value). This
     * implementation produces equal results for equal input on platforms with different {@link
     * ByteOrder}, but is slower on big-endian platforms than on little-endian.
     *
     * @see #xx128low(long)
     */
    public static M3LongHashFunction xx128low() {
        return M3XXH3.asLongTupleLowHashFunctionWithoutSeed();
    }

    /**
     * Returns a hash function implementing <a href="https://github.com/Cyan4973/xxHash">XXH128 low
     * 64bit algorithm</a> with the given seed value. This implementation produces equal results for
     * equal input on platforms with different {@link ByteOrder}, but is slower on big-endian
     * platforms than on little-endian.
     *
     * @see #xx128low()
     */
    public static M3LongHashFunction xx128low(final long seed) {
        return M3XXH3.asLongTupleLowHashFunctionWithSeed(seed);
    }

    /**
     * Returns a hash function implementing
     * <a href="https://github.com/wangyi-fudan/wyhash/blob/9f68c1b10166a54c17f55b284c21bd455fd0f7e2/wyhash.h">
     * wyhash algorithm, version 3</a> without a seed value (0 is used as default seed value). This
     * implementation produces equal results for equal input on platforms with different {@link
     * ByteOrder}, but is slower on big-endian platforms than on little-endian.
     *
     * @see #wy_3(long)
     */
    public static M3LongHashFunction wy_3() {
        return M3WyHash.asLongHashFunctionWithoutSeed();
    }

    /**
     * Returns a hash function implementing
     * <a href="https://github.com/wangyi-fudan/wyhash/blob/9f68c1b10166a54c17f55b284c21bd455fd0f7e2/wyhash.h">
     * wyhash algorithm, version 3</a> with the given seed value. This implementation produces equal
     * results for equal input on platforms with different {@link ByteOrder}, but is slower on
     * big-endian platforms than on little-endian.
     *
     * @see #wy_3()
     */
    public static M3LongHashFunction wy_3(long seed) {
        return M3WyHash.asLongHashFunctionWithSeed(seed);
    }

    /**
     * Returns a hash function implementing the 64 bit version of
     * <a href="https://github.com/jandrewrogers/MetroHash">metrohash algorithm</a> without
     * a seed value (0 is used as default seed value), with the initialization vector for
     * metrohash64_2. This implementation produces equal results for equal input on platforms with
     * different {@link ByteOrder}, but is slower on big-endian platforms than on little-endian.
     *
     * @see #metro(long)
     */
    public static M3LongHashFunction metro() {
        return M3MetroHash.asLongHashFunctionWithoutSeed();
    }

    /**
     * Returns a hash function implementing the 64 bit version of
     * <a href="https://github.com/jandrewrogers/MetroHash">metrohash algorithm</a> with the given
     * seed value, with the initialization vector for metrohash64_2. This implementation produces
     * equal results for equal input on platforms with different {@link ByteOrder}, but is slower on
     * big-endian platforms than on little-endian.
     *
     * @see #metro()
     */
    public static M3LongHashFunction metro(long seed) {
        return M3MetroHash.asLongHashFunctionWithSeed(seed);
    }

    /**
     * Constructor for use in subclasses.
     */
    protected M3LongHashFunction() {}

    /**
     * Returns the hash code for the given {@code long} value; this method is consistent with
     * {@code LongHashFunction} methods that accept sequences of bytes, assuming the {@code input}
     * value is interpreted in {@linkplain ByteOrder#nativeOrder() native} byte order. For example,
     * the result of {@code hashLong(v)} call is identical to the result of
     * {@code hashLongs(new long[] {v})} call for any {@code long} value.
     */
    public abstract long hashLong(long input);

    /**
     * Returns the hash code for the given {@code int} value; this method is consistent with
     * {@code LongHashFunction} methods that accept sequences of bytes, assuming the {@code input}
     * value is interpreted in {@linkplain ByteOrder#nativeOrder() native} byte order. For example,
     * the result of {@code hashInt(v)} call is identical to the result of
     * {@code hashInts(new int[] {v})} call for any {@code int} value.
     */
    public abstract long hashInt(int input);

    /**
     * Returns the hash code for the given {@code short} value; this method is consistent with
     * {@code LongHashFunction} methods that accept sequences of bytes, assuming the {@code input}
     * value is interpreted in {@linkplain ByteOrder#nativeOrder() native} byte order. For example,
     * the result of {@code hashShort(v)} call is identical to the result of
     * {@code hashShorts(new short[] {v})} call for any {@code short} value.
     * As a consequence, {@code hashShort(v)} call produce always the same result as {@code
     * hashChar((char) v)}.
     */
    public abstract long hashShort(short input);

    /**
     * Returns the hash code for the given {@code char} value; this method is consistent with
     * {@code LongHashFunction} methods that accept sequences of bytes, assuming the {@code input}
     * value is interpreted in {@linkplain ByteOrder#nativeOrder() native} byte order. For example,
     * the result of {@code hashChar(v)} call is identical to the result of
     * {@code hashChars(new char[] {v})} call for any {@code char} value.
     * As a consequence, {@code hashChar(v)} call produce always the same result as {@code
     * hashShort((short) v)}.
     */
    public abstract long hashChar(char input);

    /**
     * Returns the hash code for the given {@code byte} value. This method is consistent with
     * {@code LongHashFunction} methods that accept sequences of bytes. For example, the result of
     * {@code hashByte(v)} call is identical to the result of
     * {@code hashBytes(new byte[] {v})} call for any {@code byte} value.
     */
    public abstract long hashByte(byte input);

    /**
     * Returns the hash code for the empty (zero-length) bytes sequence,
     * for example {@code hashBytes(new byte[0])}.
     */
    public abstract long hashVoid();

    /**
     * Returns the hash code for {@code len} continuous bytes of the given {@code input} object,
     * starting from the given offset. The abstraction of input as ordered byte sequence and
     * "offset within the input" is defined by the given {@code access} strategy.
     *
     * <p>This method doesn't promise to throw a {@code RuntimeException} if {@code
     * [off, off + len - 1]} subsequence exceeds the bounds of the bytes sequence, defined by {@code
     * access} strategy for the given {@code input}, so use this method with caution.
     *
     * @param input the object to read bytes from
     * @param access access which defines the abstraction of the given input
     *               as ordered byte sequence
     * @param off offset to the first byte of the subsequence to hash
     * @param len length of the subsequence to hash
     * @param <T> the type of the input
     * @return hash code for the specified bytes subsequence
     */
    public abstract <T> long hash(T input, M3Access<T> access, long off, long len);

    //FLLDCIM: HPCPOP - FACADE_ENTRY_CHECK v1: every byte[] reaches the kernels here; checked before the unchecked short-name reads
    private long unsafeHash(byte[] input, long off, long len) {
        java.util.Objects.checkFromIndexSize(off, len, input.length);
        return hash(input, M3ByteArrayAccess.INSTANCE, off, len);
    }

    /**
     * Shortcut for {@link #hashBytes(byte[], int, int) hashBytes(input, 0, input.length)}.
     */
    public long hashBytes(byte[] input) {
        return unsafeHash(input, BYTE_BASE, input.length);
    }

    /**
     * Returns the hash code for the specified subsequence of the given {@code byte} array.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long)} method
     * using {@linkplain Access#unsafe() unsafe} {@code Access}.
     *
     * @param input the array to read bytes from
     * @param off index of the first {@code byte} in the subsequence to hash
     * @param len length of the subsequence to hash
     * @return hash code for the specified subsequence
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length}
     * or {@code len < 0}
     */
    public long hashBytes(byte[] input, int off, int len) {
        checkArrayOffs(input.length, off, len);
        return unsafeHash(input, BYTE_BASE + off, len);
    }

    /**
     * Shortcut for {@link #hashBytes(ByteBuffer, int, int)
     * hashBytes(input, input.position(), input.remaining())}.
     */
    public long hashBytes(ByteBuffer input) {
        return hashByteBuffer(input, input.position(), input.remaining());
    }

    /**
     * Returns the hash code for the specified subsequence of the given {@code ByteBuffer}.
     *
     * <p>This method doesn't alter the state (mark, position, limit or order) of the given
     * {@code ByteBuffer}.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long)} method
     * using {@link Access#toByteBuffer()}.
     *
     * @param input the buffer to read bytes from
     * @param off index of the first {@code byte} in the subsequence to hash
     * @param len length of the subsequence to hash
     * @return hash code for the specified subsequence
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.capacity()}
     * or {@code len < 0}
     */
    public long hashBytes(ByteBuffer input, int off, int len) {
        checkArrayOffs(input.capacity(), off, len);
        return hashByteBuffer(input, off, len);
    }

    private long hashByteBuffer(ByteBuffer input, int off, int len) {
        if (input.hasArray()) {
            return unsafeHash(input.array(), BYTE_BASE + input.arrayOffset() + off, len);
        } else if (input.isDirect() && off + len > input.limit()) {
            // read up to the capacity, as the donor's address read did (the limit does not bound it)
            return hash(input.duplicate().order(input.order()).limit(input.capacity()),
                    M3ByteBufferAccess.INSTANCE, off, len);
        } else {
            return hash(input, M3ByteBufferAccess.INSTANCE, off, len);
        }
    }

    /**
     * Shortcut for {@link #hashChars(String, int, int) hashChars(input, 0, input.length())}.
     */
    public long hashChars(String input) {
        return hashNativeChars(input, 0, input.length());
    }

    /**
     * Returns the hash code for bytes of the specified subsequence of the given {@code String}'s
     * underlying {@code char} array.
     *
     * <p>Default implementation could either delegate to {@link #hash(Object, Access, long, long)}
     * using {@link Access#toNativeCharSequence()}, or to {@link #hashChars(char[], int, int)}.
     *
     * @param input the string which bytes to hash
     * @param off index of the first {@code char} in the subsequence to hash
     * @param len length of the subsequence to hash, in chars (i. e. the length of the bytes
     *            sequence to hash is {@code len * 2L})
     * @return the hash code of the given {@code String}'s bytes
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length()}
     * or {@code len < 0}
     */
    public long hashChars(String input, int off, int len) {
        checkArrayOffs(input.length(), off, len);
        return hashNativeChars(input, off, len);
    }

    /**
     * Shortcut for {@link #hashChars(StringBuilder, int, int) hashChars(input, 0, input.length())}.
     */
    public long hashChars(StringBuilder input) {
        return hashNativeChars(input);
    }

    /**
     * Returns the hash code for bytes of the specified subsequence of the given
     * {@code StringBuilder}'s underlying {@code char} array.
     *
     * <p>Default implementation could either delegate to {@link #hash(Object, Access, long, long)}
     * using {@link Access#toNativeCharSequence()}, or to {@link #hashChars(char[], int, int)}.
     *
     * @param input the string builder which bytes to hash
     * @param off index of the first {@code char} in the subsequence to hash
     * @param len length of the subsequence to hash, in chars (i. e. the length of the bytes
     *            sequence to hash is {@code len * 2L})
     * @return the hash code of the given {@code String}'s bytes
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length()}
     * or {@code len < 0}
     */
    public long hashChars(StringBuilder input, int off, int len) {
        checkArrayOffs(input.length(), off, len);
        return hashNativeChars(input, off, len);
    }

    long hashNativeChars(CharSequence input) {
        return hashNativeChars(input, 0, input.length());
    }

    long hashNativeChars(CharSequence input, int off, int len) {
        return hash(input, nativeCharSequenceAccess(), off * CHAR_BYTES, len * CHAR_BYTES);
    }
}
