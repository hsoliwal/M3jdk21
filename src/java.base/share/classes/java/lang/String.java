/*
 * Copyright (c) 1994, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

import java.io.ObjectStreamField;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Native;
import java.lang.invoke.MethodHandles;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jdk.internal.util.ArraysSupport;
import jdk.internal.util.Preconditions;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.IntrinsicCandidate;
import jdk.internal.vm.annotation.Stable;
import sun.nio.cs.ArrayDecoder;
import sun.nio.cs.ArrayEncoder;

import sun.nio.cs.ISO_8859_1;
import sun.nio.cs.US_ASCII;
import sun.nio.cs.UTF_8;

/**
 * The {@code String} class represents character strings. All
 * string literals in Java programs, such as {@code "abc"}, are
 * implemented as instances of this class.
 * <p>
 * Strings are constant; their values cannot be changed after they
 * are created. String buffers support mutable strings.
 * Because String objects are immutable they can be shared. For example:
 * <blockquote><pre>
 *     String str = "abc";
 * </pre></blockquote><p>
 * is equivalent to:
 * <blockquote><pre>
 *     char data[] = {'a', 'b', 'c'};
 *     String str = new String(data);
 * </pre></blockquote><p>
 * Here are some more examples of how strings can be used:
 * <blockquote><pre>
 *     System.out.println("abc");
 *     String cde = "cde";
 *     System.out.println("abc" + cde);
 *     String c = "abc".substring(2, 3);
 *     String d = cde.substring(1, 2);
 * </pre></blockquote>
 * <p>
 * The class {@code String} includes methods for examining
 * individual characters of the sequence, for comparing strings, for
 * searching strings, for extracting substrings, and for creating a
 * copy of a string with all characters translated to uppercase or to
 * lowercase. Case mapping is based on the Unicode Standard version
 * specified by the {@link java.lang.Character Character} class.
 * <p>
 * The Java language provides special support for the string
 * concatenation operator (&nbsp;+&nbsp;), and for conversion of
 * other objects to strings. For additional information on string
 * concatenation and conversion, see <i>The Java Language Specification</i>.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * <p>A {@code String} represents a string in the UTF-16 format
 * in which <em>supplementary characters</em> are represented by <em>surrogate
 * pairs</em> (see the section <a href="Character.html#unicode">Unicode
 * Character Representations</a> in the {@code Character} class for
 * more information).
 * Index values refer to {@code char} code units, so a supplementary
 * character uses two positions in a {@code String}.
 * <p>The {@code String} class provides methods for dealing with
 * Unicode code points (i.e., characters), in addition to those for
 * dealing with Unicode code units (i.e., {@code char} values).
 *
 * <p>Unless otherwise noted, methods for comparing Strings do not take locale
 * into account.  The {@link java.text.Collator} class provides methods for
 * finer-grain, locale-sensitive String comparison.
 *
 * @implNote The implementation of the string concatenation operator is left to
 * the discretion of a Java compiler, as long as the compiler ultimately conforms
 * to <i>The Java Language Specification</i>. For example, the {@code javac} compiler
 * may implement the operator with {@code StringBuffer}, {@code StringBuilder},
 * or {@code java.lang.invoke.StringConcatFactory} depending on the JDK version. The
 * implementation of string conversion is typically through the method {@code toString},
 * defined by {@code Object} and inherited by all classes in Java.
 *
 * @author  Lee Boynton
 * @author  Arthur van Hoff
 * @author  Martin Buchholz
 * @author  Ulf Zibis
 * @see     java.lang.Object#toString()
 * @see     java.lang.StringBuffer
 * @see     java.lang.StringBuilder
 * @see     java.nio.charset.Charset
 * @since   1.0
 * @jls     15.18.1 String Concatenation Operator +
 */

public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence,
               Constable, ConstantDesc {

    /**
     * The bytes of this string: Java 21 compatibility is the interned bytes.
     *
     * <p>M3JDK layout invariant (doc/m3/string-lane/STRING_LAYOUT_INVARIANT.md): every String of
     * the JDK holds its text twice in Java 21 form, as these bytes (Latin-1 or UTF-16 by
     * {@code coder}, exactly as Java 21 lays them out) and as the chars of {@link #chValue}. Both
     * arrays are filled when the String is made, by every constructor and by every VM site that
     * makes a String, and neither is ever written again. An M3-backed String keeps its canonical
     * M3 owner and coordinate in {@code m3} as well; these bytes are then the JNI byte shadow of
     * that text, never a sentinel.</p>
     *
     * @implNote This field is trusted by the VM and is a subject to constant folding if the
     * String instance is constant. Overwriting it after construction will cause problems.
     */
    @Stable
    private final byte[] value;

    /**
     * The chars of this string: Java 21 compatibility is the interned chars.
     *
     * <p>A JNI-ready {@code char[]} of {@link #length()} UTF-16 units, filled lazily (owner
     * decision 51): it is {@code null} until the first whole-string char demand ({@link
     * #toCharArray()}, {@link #getChars(int, int, char[], int)} of the whole string, JNI), then
     * written once with the chars of {@link #value} and never again: a const array, published by
     * one compare-and-set from {@code null} in {@link #constChars()}. It is protected
     * internally: never handed to a caller, which only ever receives a copy.</p>
     */
    @Stable
    private char[] chValue;

    /**
     * Canonical M3JDK text value. Synexia MIndexString is the donor/reference structure; M3JDK
     * deliberately names its counterpart M3String. M3String itself is only owner + coordinate.
     */
    private volatile M3String m3;

    /**
     * The identifier of the encoding used to encode the bytes in
     * {@code value}. The supported values in this implementation are
     *
     * LATIN1
     * UTF16
     *
     * @implNote This field is trusted by the VM, and is a subject to
     * constant folding if String instance is constant. Overwriting this
     * field after construction will cause problems.
     */
    private final byte coder;

    /** Cache the hash code for the string */
    private int hash; // Default to 0

    /**
     * Cache if the hash has been calculated as actually being zero, enabling
     * us to avoid recalculating this.
     */
    private boolean hashIsZero; // Default to false;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    @java.io.Serial
    private static final long serialVersionUID = -6849794470754667710L;

    /**
     * If String compaction is disabled, the bytes in {@code value} are
     * always encoded in UTF16.
     *
     * For methods with several possible implementation paths, when String
     * compaction is disabled, only one code path is taken.
     *
     * The instance field value is generally opaque to optimizing JIT
     * compilers. Therefore, in performance-sensitive place, an explicit
     * check of the static boolean {@code COMPACT_STRINGS} is done first
     * before checking the {@code coder} field since the static boolean
     * {@code COMPACT_STRINGS} would be constant folded away by an
     * optimizing JIT compiler. The idioms for these cases are as follows.
     *
     * For code such as:
     *
     *    if (coder == LATIN1) { ... }
     *
     * can be written more optimally as
     *
     *    if (coder() == LATIN1) { ... }
     *
     * or:
     *
     *    if (COMPACT_STRINGS && coder == LATIN1) { ... }
     *
     * An optimizing JIT compiler can fold the above conditional as:
     *
     *    COMPACT_STRINGS == true  => if (coder == LATIN1) { ... }
     *    COMPACT_STRINGS == false => if (false)           { ... }
     *
     * @implNote
     * The actual value for this field is injected by JVM. The static
     * initialization block is used to set the value here to communicate
     * that this static final field is not statically foldable, and to
     * avoid any possible circular dependency during vm initialization.
     */
    static final boolean COMPACT_STRINGS;

    /**
     * Enables M3 segmented String production. The actual value is injected by
     * HotSpot after String class initialization, exactly like COMPACT_STRINGS.
     */
    static final boolean M3_JOINED_STRINGS;

    static {
        COMPACT_STRINGS = true;
        M3_JOINED_STRINGS = false;
    }

    /**
     * Class String is special cased within the Serialization Stream Protocol.
     *
     * A String instance is written into an ObjectOutputStream according to
     * <a href="{@docRoot}/../specs/serialization/protocol.html#stream-elements">
     * <cite>Java Object Serialization Specification</cite>, Section 6.2, "Stream Elements"</a>
     */
    @java.io.Serial
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[0];

    /**
     * Initializes a newly created {@code String} object so that it represents
     * an empty character sequence.  Note that use of this constructor is
     * unnecessary since Strings are immutable.
     */
    public String() {
        this.value = "".value;
        this.coder = "".coder;
        this.m3 = null;
    }

    /**
     * Initializes a newly created {@code String} object so that it represents
     * the same sequence of characters as the argument; in other words, the
     * newly created string is a copy of the argument string. Unless an
     * explicit copy of {@code original} is needed, use of this constructor is
     * unnecessary since Strings are immutable.
     *
     * @param  original
     *         A {@code String}
     */
    @IntrinsicCandidate
    public String(String original) {
        M3String storage = original.m3();
        this.value = original.value;
        this.coder = original.coder;
        this.m3 = storage;
        this.chValue = original.chValue;
        this.hash = original.hash;
        this.hashIsZero = original.hashIsZero;
    }

    /**
     * Allocates a new {@code String} so that it represents the sequence of
     * characters currently contained in the character array argument. The
     * contents of the character array are copied; subsequent modification of
     * the character array does not affect the newly created string.
     *
     * @param  value
     *         The initial value of the string
     */
    public String(char[] value) {
        this(value, 0, value.length, null);
    }

    /**
     * Allocates a new {@code String} that contains characters from a subarray
     * of the character array argument. The {@code offset} argument is the
     * index of the first character of the subarray and the {@code count}
     * argument specifies the length of the subarray. The contents of the
     * subarray are copied; subsequent modification of the character array does
     * not affect the newly created string.
     *
     * @param  value
     *         Array that is the source of characters
     *
     * @param  offset
     *         The initial offset
     *
     * @param  count
     *         The length
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code offset} is negative, {@code count} is negative, or
     *          {@code offset} is greater than {@code value.length - count}
     */
    public String(char[] value, int offset, int count) {
        this(value, offset, count, rangeCheck(value, offset, count));
    }

    private static Void rangeCheck(char[] value, int offset, int count) {
        checkBoundsOffCount(offset, count, value.length);
        return null;
    }

    /**
     * Allocates a new {@code String} that contains characters from a subarray
     * of the <a href="Character.html#unicode">Unicode code point</a> array
     * argument.  The {@code offset} argument is the index of the first code
     * point of the subarray and the {@code count} argument specifies the
     * length of the subarray.  The contents of the subarray are converted to
     * {@code char}s; subsequent modification of the {@code int} array does not
     * affect the newly created string.
     *
     * @param  codePoints
     *         Array that is the source of Unicode code points
     *
     * @param  offset
     *         The initial offset
     *
     * @param  count
     *         The length
     *
     * @throws  IllegalArgumentException
     *          If any invalid Unicode code point is found in {@code
     *          codePoints}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code offset} is negative, {@code count} is negative, or
     *          {@code offset} is greater than {@code codePoints.length - count}
     *
     * @since  1.5
     */
    public String(int[] codePoints, int offset, int count) {
        checkBoundsOffCount(offset, count, codePoints.length);
        if (count == 0) {
            this.value = "".value;
            this.coder = "".coder;
            this.m3 = null;
            return;
        }

        M3String direct = maybeAdmitCodePoints(codePoints, offset, count);
        if (direct != null) {
            this.value = direct.materialize();
            this.coder = shadowCoder(this.value, direct.length());
            this.m3 = direct;
            return;
        }

        //FLLDCIM: MERGE-21U - 8311906: compress returns the result array (never null); its length gives the coder
        if (COMPACT_STRINGS) {
            byte[] val = StringUTF16.compress(codePoints, offset, count);
            this.value = val;
            this.coder = StringUTF16.coderFromArrayLen(val, count);
            this.m3 = null;
            return;
        }
        this.value = StringUTF16.toBytes(codePoints, offset, count);
        this.coder = UTF16;
        this.m3 = null;
    }

    /**
     * Allocates a new {@code String} constructed from a subarray of an array
     * of 8-bit integer values.
     *
     * <p> The {@code offset} argument is the index of the first byte of the
     * subarray, and the {@code count} argument specifies the length of the
     * subarray.
     *
     * <p> Each {@code byte} in the subarray is converted to a {@code char} as
     * specified in the {@link #String(byte[],int) String(byte[],int)} constructor.
     *
     * @deprecated This method does not properly convert bytes into characters.
     * As of JDK&nbsp;1.1, the preferred way to do this is via the
     * {@code String} constructors that take a {@link Charset}, charset name,
     * or that use the {@link Charset#defaultCharset() default charset}.
     *
     * @param  ascii
     *         The bytes to be converted to characters
     *
     * @param  hibyte
     *         The top 8 bits of each 16-bit Unicode code unit
     *
     * @param  offset
     *         The initial offset
     * @param  count
     *         The length
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code offset} is negative, {@code count} is negative, or
     *          {@code offset} is greater than {@code ascii.length - count}
     *
     * @see  #String(byte[], int)
     * @see  #String(byte[], int, int, java.lang.String)
     * @see  #String(byte[], int, int, java.nio.charset.Charset)
     * @see  #String(byte[], int, int)
     * @see  #String(byte[], java.lang.String)
     * @see  #String(byte[], java.nio.charset.Charset)
     * @see  #String(byte[])
     */
    @Deprecated(since="1.1")
    public String(byte[] ascii, int hibyte, int offset, int count) {
        checkBoundsOffCount(offset, count, ascii.length);
        if (count == 0) {
            this.value = "".value;
            this.coder = "".coder;
            this.m3 = null;
            return;
        }
        byte[] candidate;
        byte candidateCoder;
        if (COMPACT_STRINGS && (byte)hibyte == 0) {
            candidate = Arrays.copyOfRange(ascii, offset, offset + count);
            candidateCoder = LATIN1;
        } else {
            hibyte <<= 8;
            candidate = StringUTF16.newBytesFor(count);
            for (int i = 0; i < count; i++) {
                StringUTF16.putChar(candidate, i, hibyte | (ascii[offset++] & 0xff));
            }
            candidateCoder = UTF16;
        }
        // The candidate is canonical: Latin-1 for a zero high byte, else every unit is above
        // 0xff (or Compact Strings are off), so its coder is the coder the pool chose.
        M3String storage = maybeAdmit(candidate, candidateCoder);
        this.value = candidate;
        this.coder = candidateCoder;
        this.m3 = storage;
    }

    /**
     * Allocates a new {@code String} containing characters constructed from
     * an array of 8-bit integer values. Each character <i>c</i> in the
     * resulting string is constructed from the corresponding component
     * <i>b</i> in the byte array such that:
     *
     * <blockquote><pre>
     *     <b><i>c</i></b> == (char)(((hibyte &amp; 0xff) &lt;&lt; 8)
     *                         | (<b><i>b</i></b> &amp; 0xff))
     * </pre></blockquote>
     *
     * @deprecated  This method does not properly convert bytes into
     * characters.  As of JDK&nbsp;1.1, the preferred way to do this is via the
     * {@code String} constructors that take a {@link Charset}, charset name,
     * or that use the {@link Charset#defaultCharset() default charset}.
     *
     * @param  ascii
     *         The bytes to be converted to characters
     *
     * @param  hibyte
     *         The top 8 bits of each 16-bit Unicode code unit
     *
     * @see  #String(byte[], int, int, java.lang.String)
     * @see  #String(byte[], int, int, java.nio.charset.Charset)
     * @see  #String(byte[], int, int)
     * @see  #String(byte[], java.lang.String)
     * @see  #String(byte[], java.nio.charset.Charset)
     * @see  #String(byte[])
     */
    @Deprecated(since="1.1")
    public String(byte[] ascii, int hibyte) {
        this(ascii, hibyte, 0, ascii.length);
    }

    /**
     * Constructs a new {@code String} by decoding the specified subarray of
     * bytes using the specified charset.  The length of the new {@code String}
     * is a function of the charset, and hence may not be equal to the length
     * of the subarray.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the given charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  offset
     *         The index of the first byte to decode
     *
     * @param  length
     *         The number of bytes to decode
     *
     * @param  charsetName
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code offset} is negative, {@code length} is negative, or
     *          {@code offset} is greater than {@code bytes.length - length}
     *
     * @since  1.1
     */
    public String(byte[] bytes, int offset, int length, String charsetName)
            throws UnsupportedEncodingException {
        this(lookupCharset(charsetName), bytes, checkBoundsOffCount(offset, length, bytes.length), length);
    }

    /**
     * Constructs a new {@code String} by decoding the specified subarray of
     * bytes using the specified {@linkplain java.nio.charset.Charset charset}.
     * The length of the new {@code String} is a function of the charset, and
     * hence may not be equal to the length of the subarray.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  offset
     *         The index of the first byte to decode
     *
     * @param  length
     *         The number of bytes to decode
     *
     * @param  charset
     *         The {@linkplain java.nio.charset.Charset charset} to be used to
     *         decode the {@code bytes}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code offset} is negative, {@code length} is negative, or
     *          {@code offset} is greater than {@code bytes.length - length}
     *
     * @since  1.6
     */
    public String(byte[] bytes, int offset, int length, Charset charset) {
        this(Objects.requireNonNull(charset), bytes, checkBoundsOffCount(offset, length, bytes.length), length);
    }

    /**
     * This method does not do any precondition checks on its arguments.
     * <p>
     * Important: parameter order of this method is deliberately changed in order to
     * disambiguate it against other similar methods of this class.
     */
    @SuppressWarnings("removal")
    private String(Charset charset, byte[] bytes, int offset, int length) {
        this(decodeForM3Construction(charset, bytes, offset, length));
    }

    private String(M3Construction construction) {
        if (construction.storage != null) {
            this.value = construction.storage.materialize();
            this.coder = shadowCoder(this.value, construction.storage.length());
            this.m3 = construction.storage;
            return;
        }
        M3String storage = maybeAdmit(construction.value, construction.coder);
        this.value = admittedBytes(construction.value, construction.coder, storage);
        this.coder = this.value == construction.value
                ? construction.coder
                : shadowCoder(this.value, storage.length());
        this.m3 = storage;
    }

    private static final class M3Construction {
        final byte[] value;
        final byte coder;
        final M3String storage;

        M3Construction(byte[] value, byte coder) {
            this.value = value;
            this.coder = coder;
            this.storage = null;
        }

        M3Construction(M3String storage) {
            this.value = null;
            this.coder = storage.coder();
            this.storage = Objects.requireNonNull(storage, "storage");
        }
    }

    private static M3String maybeAdmitDecodedSingleByte(
            Charset charset, byte[] bytes, int offset, int length) {
        if (!M3_JOINED_STRINGS
                || !jdk.internal.misc.VM.isBooted()
                || !M3String.admissionEnabled()
                || length == 0) {
            return null;
        }

        if (charset == ISO_8859_1.INSTANCE) {
            return M3String.admitLatin1Bytes(bytes, offset, length);
        }
        if ((charset == UTF_8.INSTANCE || charset == US_ASCII.INSTANCE)
                && StringCoding.countPositives(bytes, offset, length) == length) {
            return M3String.admitLatin1Bytes(bytes, offset, length);
        }
        return null;
    }

    @SuppressWarnings("removal")
    private static M3Construction decodeForM3Construction(
            Charset charset, byte[] bytes, int offset, int length) {
        if (length == 0) {
            return new M3Construction("".value, "".coder);
        }

        M3String direct = maybeAdmitDecodedSingleByte(charset, bytes, offset, length);
        if (direct != null) {
            return new M3Construction(direct);
        }
        //FLLDCIM: MERGE-21U - 8311906/8321180 ported onto the M3Construction helper: decode from a stable copy,
        //FLLDCIM: MERGE-21U - newBytesFor checks the UTF16 size, compress results carry their coder by length
        if (charset == UTF_8.INSTANCE) {
            if (COMPACT_STRINGS) {
                int dp = StringCoding.countPositives(bytes, offset, length);
                if (dp == length) {
                    return new M3Construction(
                            Arrays.copyOfRange(bytes, offset, offset + length), LATIN1);
                }
                // Decode with a stable copy, to be the result if the decoded length is the same
                byte[] latin1 = Arrays.copyOfRange(bytes, offset, offset + length);
                int sp = dp;            // first dp bytes are already in the copy
                while (sp < length) {
                    int b1 = latin1[sp++];
                    if (b1 >= 0) {
                        latin1[dp++] = (byte)b1;
                        continue;
                    }
                    if ((b1 & 0xfe) == 0xc2 && sp < length) { // b1 either 0xc2 or 0xc3
                        int b2 = latin1[sp];
                        if (b2 < -64) { // continuation bytes are always negative values in the range -128 to -65
                            latin1[dp++] = (byte)decode2(b1, b2);
                            sp++;
                            continue;
                        }
                    }
                    // anything not a latin1, including the REPL
                    // we have to go with the utf16
                    sp--;
                    break;
                }
                if (sp == length) {
                    if (dp != latin1.length) {
                        latin1 = Arrays.copyOf(latin1, dp);
                    }
                    return new M3Construction(latin1, LATIN1);
                }
                byte[] utf16 = StringUTF16.newBytesFor(length);
                StringLatin1.inflate(latin1, 0, utf16, 0, dp);
                dp = decodeUTF8_UTF16(latin1, sp, length, utf16, dp, true);
                if (dp != length) {
                    utf16 = Arrays.copyOf(utf16, dp << 1);
                }
                return new M3Construction(utf16, UTF16);
            }
            byte[] dst = StringUTF16.newBytesFor(length);
            int dp = decodeUTF8_UTF16(bytes, offset, offset + length, dst, 0, true);
            if (dp != length) {
                dst = Arrays.copyOf(dst, dp << 1);
            }
            return new M3Construction(dst, UTF16);
        }

        if (charset == ISO_8859_1.INSTANCE) {
            return COMPACT_STRINGS
                    ? new M3Construction(
                            Arrays.copyOfRange(bytes, offset, offset + length), LATIN1)
                    : new M3Construction(
                            StringLatin1.inflate(bytes, offset, length), UTF16);
        }

        if (charset == US_ASCII.INSTANCE) {
            if (COMPACT_STRINGS && !StringCoding.hasNegatives(bytes, offset, length)) {
                return new M3Construction(
                        Arrays.copyOfRange(bytes, offset, offset + length), LATIN1);
            }
            byte[] dst = StringUTF16.newBytesFor(length);
            int dp = 0;
            while (dp < length) {
                int b = bytes[offset++];
                StringUTF16.putChar(dst, dp++, (b >= 0) ? (char)b : REPL);
            }
            return new M3Construction(dst, UTF16);
        }

        CharsetDecoder cd = charset.newDecoder();
        if (cd instanceof ArrayDecoder ad) {
            if (ad.isASCIICompatible() && !StringCoding.hasNegatives(bytes, offset, length)) {
                return COMPACT_STRINGS
                        ? new M3Construction(
                                Arrays.copyOfRange(bytes, offset, offset + length), LATIN1)
                        : new M3Construction(
                                StringLatin1.inflate(bytes, offset, length), UTF16);
            }

            if (COMPACT_STRINGS && ad.isLatin1Decodable()) {
                byte[] dst = new byte[length];
                ad.decodeToLatin1(bytes, offset, length, dst);
                return new M3Construction(dst, LATIN1);
            }

            int en = scale(length, cd.maxCharsPerByte());
            cd.onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
            char[] ca = new char[en];
            int clen = ad.decode(bytes, offset, length, ca);
            if (COMPACT_STRINGS) {
                byte[] val = StringUTF16.compress(ca, 0, clen);
                return new M3Construction(val, StringUTF16.coderFromArrayLen(val, clen));
            }
            return new M3Construction(StringUTF16.toBytes(ca, 0, clen), UTF16);
        }

        int en = scale(length, cd.maxCharsPerByte());
        cd.onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        char[] ca = new char[en];
        if (charset.getClass().getClassLoader0() != null
                && System.getSecurityManager() != null) {
            bytes = Arrays.copyOfRange(bytes, offset, offset + length);
            offset = 0;
        }

        int caLen;
        try {
            caLen = decodeWithDecoder(cd, ca, bytes, offset, length);
        } catch (CharacterCodingException failure) {
            throw new Error(failure);
        }
        if (COMPACT_STRINGS) {
            byte[] val = StringUTF16.compress(ca, 0, caLen);
            return new M3Construction(val, StringUTF16.coderFromArrayLen(val, caLen));
        }
        return new M3Construction(StringUTF16.toBytes(ca, 0, caLen), UTF16);
    }

    /*
     * Throws iae, instead of replacing, if malformed or unmappable.
     *
     * @param  noShare
     *         {@code true} if the resulting string MUST NOT share the byte array,
     *         {@code false} if the byte array can be exclusively used to construct
     *         the string and is not modified or used for any other purpose.
     */
    static String newStringUTF8NoRepl(byte[] bytes, int offset, int length, boolean noShare) {
        checkBoundsOffCount(offset, length, bytes.length);
        if (length == 0) {
            return "";
        }
        int dp;
        byte[] dst;
        if (COMPACT_STRINGS) {
            dp = StringCoding.countPositives(bytes, offset, length);
            int sl = offset + length;
            if (dp == length) {
                if (noShare || length != bytes.length) {
                    return new String(Arrays.copyOfRange(bytes, offset, offset + length), LATIN1);
                } else {
                    return new String(bytes, LATIN1);
                }
            }
            dst = new byte[length];
            System.arraycopy(bytes, offset, dst, 0, dp);
            offset += dp;
            while (offset < sl) {
                int b1 = bytes[offset++];
                if (b1 >= 0) {
                    dst[dp++] = (byte)b1;
                    continue;
                }
                if ((b1 & 0xfe) == 0xc2 && offset < sl) { // b1 either 0xc2 or 0xc3
                    int b2 = bytes[offset];
                    if (b2 < -64) { // continuation bytes are always negative values in the range -128 to -65
                        dst[dp++] = (byte)decode2(b1, b2);
                        offset++;
                        continue;
                    }
                }
                // anything not a latin1, including the REPL
                // we have to go with the utf16
                offset--;
                break;
            }
            if (offset == sl) {
                if (dp != dst.length) {
                    dst = Arrays.copyOf(dst, dp);
                }
                return new String(dst, LATIN1);
            }
            if (dp == 0) {
                dst = StringUTF16.newBytesFor(length);
            } else {
                byte[] buf = StringUTF16.newBytesFor(length);
                StringLatin1.inflate(dst, 0, buf, 0, dp);
                dst = buf;
            }
            dp = decodeUTF8_UTF16(bytes, offset, sl, dst, dp, false);
        } else { // !COMPACT_STRINGS
            dst = StringUTF16.newBytesFor(length);
            dp = decodeUTF8_UTF16(bytes, offset, offset + length, dst, 0, false);
        }
        if (dp != length) {
            dst = Arrays.copyOf(dst, dp << 1);
        }
        return new String(dst, UTF16);
    }

    static String newStringNoRepl(byte[] src, Charset cs) throws CharacterCodingException {
        try {
            return newStringNoRepl1(src, cs);
        } catch (IllegalArgumentException e) {
            //newStringNoRepl1 throws IAE with MalformedInputException or CCE as the cause
            Throwable cause = e.getCause();
            if (cause instanceof MalformedInputException mie) {
                throw mie;
            }
            throw (CharacterCodingException)cause;
        }
    }

    @SuppressWarnings("removal")
    private static String newStringNoRepl1(byte[] src, Charset cs) {
        int len = src.length;
        if (len == 0) {
            return "";
        }
        if (cs == UTF_8.INSTANCE) {
            return newStringUTF8NoRepl(src, 0, src.length, false);
        }
        if (cs == ISO_8859_1.INSTANCE) {
            if (COMPACT_STRINGS)
                return new String(src, LATIN1);
            return new String(StringLatin1.inflate(src, 0, src.length), UTF16);
        }
        if (cs == US_ASCII.INSTANCE) {
            if (!StringCoding.hasNegatives(src, 0, src.length)) {
                if (COMPACT_STRINGS)
                    return new String(src, LATIN1);
                return new String(StringLatin1.inflate(src, 0, src.length), UTF16);
            } else {
                throwMalformed(src);
            }
        }

        CharsetDecoder cd = cs.newDecoder();
        // ascii fastpath
        if (cd instanceof ArrayDecoder ad &&
                ad.isASCIICompatible() &&
                !StringCoding.hasNegatives(src, 0, src.length)) {
            if (COMPACT_STRINGS)
                return new String(src, LATIN1);
            return new String(src, 0, src.length, ISO_8859_1.INSTANCE);
        }
        int en = scale(len, cd.maxCharsPerByte());
        char[] ca = new char[en];
        if (cs.getClass().getClassLoader0() != null &&
                System.getSecurityManager() != null) {
            src = Arrays.copyOf(src, len);
        }
        int caLen;
        try {
            caLen = decodeWithDecoder(cd, ca, src, 0, src.length);
        } catch (CharacterCodingException x) {
            // throw via IAE
            throw new IllegalArgumentException(x);
        }
        if (COMPACT_STRINGS) {
            byte[] val = StringUTF16.compress(ca, 0, caLen);
            byte coder = StringUTF16.coderFromArrayLen(val, caLen);
            return new String(val, coder);
        }
        return new String(StringUTF16.toBytes(ca, 0, caLen), UTF16);
    }

    private static final char REPL = '\ufffd';

    // Trim the given byte array to the given length
    @SuppressWarnings("removal")
    private static byte[] safeTrim(byte[] ba, int len, boolean isTrusted) {
        if (len == ba.length && (isTrusted || System.getSecurityManager() == null)) {
            return ba;
        } else {
            return Arrays.copyOf(ba, len);
        }
    }

    private static int scale(int len, float expansionFactor) {
        // We need to perform double, not float, arithmetic; otherwise
        // we lose low order bits when len is larger than 2**24.
        return (int)(len * (double)expansionFactor);
    }

    private static Charset lookupCharset(String csn) throws UnsupportedEncodingException {
        Objects.requireNonNull(csn);
        try {
            return Charset.forName(csn);
        } catch (UnsupportedCharsetException | IllegalCharsetNameException x) {
            throw new UnsupportedEncodingException(csn);
        }
    }

    private static byte[] encode(Charset cs, byte coder, byte[] val) {
        if (cs == UTF_8.INSTANCE) {
            return encodeUTF8(coder, val, true);
        }
        if (cs == ISO_8859_1.INSTANCE) {
            return encode8859_1(coder, val);
        }
        if (cs == US_ASCII.INSTANCE) {
            return encodeASCII(coder, val);
        }
        return encodeWithEncoder(cs, coder, val, true);
    }

    private static byte[] encodeWithEncoder(Charset cs, byte coder, byte[] val, boolean doReplace) {
        CharsetEncoder ce = cs.newEncoder();
        int len = val.length >> coder;  // assume LATIN1=0/UTF16=1;
        int en = scale(len, ce.maxBytesPerChar());
        // fastpath with ArrayEncoder implies `doReplace`.
        if (doReplace && ce instanceof ArrayEncoder ae) {
            // fastpath for ascii compatible
            if (coder == LATIN1 &&
                    ae.isASCIICompatible() &&
                    !StringCoding.hasNegatives(val, 0, val.length)) {
                return val.clone();
            }
            byte[] ba = new byte[en];
            if (len == 0) {
                return ba;
            }

            int blen = (coder == LATIN1) ? ae.encodeFromLatin1(val, 0, len, ba)
                    : ae.encodeFromUTF16(val, 0, len, ba);
            if (blen != -1) {
                return safeTrim(ba, blen, true);
            }
        }

        byte[] ba = new byte[en];
        if (len == 0) {
            return ba;
        }
        if (doReplace) {
            ce.onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
        }
        char[] ca = (coder == LATIN1 ) ? StringLatin1.toChars(val)
                : StringUTF16.toChars(val);
        ByteBuffer bb = ByteBuffer.wrap(ba);
        CharBuffer cb = CharBuffer.wrap(ca, 0, len);
        try {
            CoderResult cr = ce.encode(cb, bb, true);
            if (!cr.isUnderflow())
                cr.throwException();
            cr = ce.flush(bb);
            if (!cr.isUnderflow())
                cr.throwException();
        } catch (CharacterCodingException x) {
            if (!doReplace) {
                throw new IllegalArgumentException(x);
            } else {
                throw new Error(x);
            }
        }
        return safeTrim(ba, bb.position(), cs.getClass().getClassLoader0() == null);
    }

    /*
     * Throws iae, instead of replacing, if unmappable.
     */
    static byte[] getBytesUTF8NoRepl(String s) {
        M3String storage = s.m3();
        return storage != null ? storage.encodeUtf8NoRepl()
                               : encodeUTF8(s.coder(), s.value(), false);
    }

    private static boolean isASCII(byte[] src) {
        return !StringCoding.hasNegatives(src, 0, src.length);
    }

    /*
     * Throws CCE, instead of replacing, if unmappable.
     */
    static byte[] getBytesNoRepl(String s, Charset cs) throws CharacterCodingException {
        try {
            return getBytesNoRepl1(s, cs);
        } catch (IllegalArgumentException e) {
            //getBytesNoRepl1 throws IAE with UnmappableCharacterException or CCE as the cause
            Throwable cause = e.getCause();
            if (cause instanceof UnmappableCharacterException) {
                throw (UnmappableCharacterException)cause;
            }
            throw (CharacterCodingException)cause;
        }
    }

    private static byte[] getBytesNoRepl1(String s, Charset cs) {
        M3String storage = s.m3();
        if (storage != null) {
            return storage.encodeNoRepl(cs);
        }
        byte[] val = s.value();
        byte coder = s.coder();
        if (cs == UTF_8.INSTANCE) {
            if (coder == LATIN1 && isASCII(val)) {
                return val;
            }
            return encodeUTF8(coder, val, false);
        }
        if (cs == ISO_8859_1.INSTANCE) {
            if (coder == LATIN1) {
                return val;
            }
            return encode8859_1(coder, val, false);
        }
        if (cs == US_ASCII.INSTANCE) {
            if (coder == LATIN1) {
                if (isASCII(val)) {
                    return val;
                } else {
                    throwUnmappable(val);
                }
            }
        }
        return encodeWithEncoder(cs, coder, val, false);
    }

    private static byte[] encodeASCII(byte coder, byte[] val) {
        if (coder == LATIN1) {
            int positives = StringCoding.countPositives(val, 0, val.length);
            byte[] dst = val.clone();
            if (positives < dst.length) {
                replaceNegatives(dst, positives);
            }
            return dst;
        }
        int len = val.length >> 1;
        byte[] dst = new byte[len];
        int dp = 0;
        for (int i = 0; i < len; i++) {
            char c = StringUTF16.getChar(val, i);
            if (c < 0x80) {
                dst[dp++] = (byte)c;
                continue;
            }
            if (Character.isHighSurrogate(c) && i + 1 < len &&
                    Character.isLowSurrogate(StringUTF16.getChar(val, i + 1))) {
                i++;
            }
            dst[dp++] = '?';
        }
        if (len == dp) {
            return dst;
        }
        return Arrays.copyOf(dst, dp);
    }

    private static void replaceNegatives(byte[] val, int fromIndex) {
        for (int i = fromIndex; i < val.length; i++) {
            if (val[i] < 0) {
                val[i] = '?';
            }
        }
    }

    private static byte[] encode8859_1(byte coder, byte[] val) {
        return encode8859_1(coder, val, true);
    }

    private static byte[] encode8859_1(byte coder, byte[] val, boolean doReplace) {
        if (coder == LATIN1) {
            return val.clone();
        }
        int len = val.length >> 1;
        byte[] dst = new byte[len];
        int dp = 0;
        int sp = 0;
        int sl = len;
        while (sp < sl) {
            int ret = StringCoding.implEncodeISOArray(val, sp, dst, dp, len);
            sp = sp + ret;
            dp = dp + ret;
            if (ret != len) {
                if (!doReplace) {
                    throwUnmappable(sp);
                }
                char c = StringUTF16.getChar(val, sp++);
                if (Character.isHighSurrogate(c) && sp < sl &&
                        Character.isLowSurrogate(StringUTF16.getChar(val, sp))) {
                    sp++;
                }
                dst[dp++] = '?';
                len = sl - sp;
            }
        }
        if (dp == dst.length) {
            return dst;
        }
        return Arrays.copyOf(dst, dp);
    }

    //////////////////////////////// utf8 ////////////////////////////////////

    /**
     * Decodes ASCII from the source byte array into the destination
     * char array. Used via JavaLangAccess from UTF_8 and other charset
     * decoders.
     *
     * @return the number of bytes successfully decoded, at most len
     */
    /* package-private */
    static int decodeASCII(byte[] sa, int sp, char[] da, int dp, int len) {
        int count = StringCoding.countPositives(sa, sp, len);
        while (count < len) {
            if (sa[sp + count] < 0) {
                break;
            }
            count++;
        }
        StringLatin1.inflate(sa, sp, da, dp, count);
        return count;
    }

    private static boolean isNotContinuation(int b) {
        return (b & 0xc0) != 0x80;
    }

    private static boolean isMalformed3(int b1, int b2, int b3) {
        return (b1 == (byte)0xe0 && (b2 & 0xe0) == 0x80) ||
                (b2 & 0xc0) != 0x80 || (b3 & 0xc0) != 0x80;
    }

    private static boolean isMalformed3_2(int b1, int b2) {
        return (b1 == (byte)0xe0 && (b2 & 0xe0) == 0x80) ||
                (b2 & 0xc0) != 0x80;
    }

    private static boolean isMalformed4(int b2, int b3, int b4) {
        return (b2 & 0xc0) != 0x80 || (b3 & 0xc0) != 0x80 ||
                (b4 & 0xc0) != 0x80;
    }

    private static boolean isMalformed4_2(int b1, int b2) {
        return (b1 == 0xf0 && (b2 < 0x90 || b2 > 0xbf)) ||
                (b1 == 0xf4 && (b2 & 0xf0) != 0x80) ||
                (b2 & 0xc0) != 0x80;
    }

    private static boolean isMalformed4_3(int b3) {
        return (b3 & 0xc0) != 0x80;
    }

    private static char decode2(int b1, int b2) {
        return (char)(((b1 << 6) ^ b2) ^
                (((byte) 0xC0 << 6) ^
                        ((byte) 0x80 << 0)));
    }

    private static char decode3(int b1, int b2, int b3) {
        return (char)((b1 << 12) ^
                (b2 <<  6) ^
                (b3 ^
                        (((byte) 0xE0 << 12) ^
                                ((byte) 0x80 <<  6) ^
                                ((byte) 0x80 <<  0))));
    }

    private static int decode4(int b1, int b2, int b3, int b4) {
        return ((b1 << 18) ^
                (b2 << 12) ^
                (b3 <<  6) ^
                (b4 ^
                        (((byte) 0xF0 << 18) ^
                                ((byte) 0x80 << 12) ^
                                ((byte) 0x80 <<  6) ^
                                ((byte) 0x80 <<  0))));
    }

    private static int decodeUTF8_UTF16(byte[] src, int sp, int sl, byte[] dst, int dp, boolean doReplace) {
        while (sp < sl) {
            int b1 = src[sp++];
            if (b1 >= 0) {
                StringUTF16.putChar(dst, dp++, (char) b1);
            } else if ((b1 >> 5) == -2 && (b1 & 0x1e) != 0) {
                if (sp < sl) {
                    int b2 = src[sp++];
                    if (isNotContinuation(b2)) {
                        if (!doReplace) {
                            throwMalformed(sp - 1, 1);
                        }
                        StringUTF16.putChar(dst, dp++, REPL);
                        sp--;
                    } else {
                        StringUTF16.putChar(dst, dp++, decode2(b1, b2));
                    }
                    continue;
                }
                if (!doReplace) {
                    throwMalformed(sp, 1);  // underflow()
                }
                StringUTF16.putChar(dst, dp++, REPL);
                break;
            } else if ((b1 >> 4) == -2) {
                if (sp + 1 < sl) {
                    int b2 = src[sp++];
                    int b3 = src[sp++];
                    if (isMalformed3(b1, b2, b3)) {
                        if (!doReplace) {
                            throwMalformed(sp - 3, 3);
                        }
                        StringUTF16.putChar(dst, dp++, REPL);
                        sp -= 3;
                        sp += malformed3(src, sp);
                    } else {
                        char c = decode3(b1, b2, b3);
                        if (Character.isSurrogate(c)) {
                            if (!doReplace) {
                                throwMalformed(sp - 3, 3);
                            }
                            StringUTF16.putChar(dst, dp++, REPL);
                        } else {
                            StringUTF16.putChar(dst, dp++, c);
                        }
                    }
                    continue;
                }
                if (sp < sl && isMalformed3_2(b1, src[sp])) {
                    if (!doReplace) {
                        throwMalformed(sp - 1, 2);
                    }
                    StringUTF16.putChar(dst, dp++, REPL);
                    continue;
                }
                if (!doReplace) {
                    throwMalformed(sp, 1);
                }
                StringUTF16.putChar(dst, dp++, REPL);
                break;
            } else if ((b1 >> 3) == -2) {
                if (sp + 2 < sl) {
                    int b2 = src[sp++];
                    int b3 = src[sp++];
                    int b4 = src[sp++];
                    int uc = decode4(b1, b2, b3, b4);
                    if (isMalformed4(b2, b3, b4) ||
                            !Character.isSupplementaryCodePoint(uc)) { // shortest form check
                        if (!doReplace) {
                            throwMalformed(sp - 4, 4);
                        }
                        StringUTF16.putChar(dst, dp++, REPL);
                        sp -= 4;
                        sp += malformed4(src, sp);
                    } else {
                        StringUTF16.putChar(dst, dp++, Character.highSurrogate(uc));
                        StringUTF16.putChar(dst, dp++, Character.lowSurrogate(uc));
                    }
                    continue;
                }
                b1 &= 0xff;
                if (b1 > 0xf4 || sp < sl && isMalformed4_2(b1, src[sp] & 0xff)) {
                    if (!doReplace) {
                        throwMalformed(sp - 1, 1);  // or 2
                    }
                    StringUTF16.putChar(dst, dp++, REPL);
                    continue;
                }
                if (!doReplace) {
                    throwMalformed(sp - 1, 1);
                }
                sp++;
                StringUTF16.putChar(dst, dp++, REPL);
                if (sp < sl && isMalformed4_3(src[sp])) {
                    continue;
                }
                break;
            } else {
                if (!doReplace) {
                    throwMalformed(sp - 1, 1);
                }
                StringUTF16.putChar(dst, dp++, REPL);
            }
        }
        return dp;
    }

    private static int decodeWithDecoder(CharsetDecoder cd, char[] dst, byte[] src, int offset, int length)
                                            throws CharacterCodingException {
        ByteBuffer bb = ByteBuffer.wrap(src, offset, length);
        CharBuffer cb = CharBuffer.wrap(dst, 0, dst.length);
        CoderResult cr = cd.decode(bb, cb, true);
        if (!cr.isUnderflow())
            cr.throwException();
        cr = cd.flush(cb);
        if (!cr.isUnderflow())
            cr.throwException();
        return cb.position();
    }

    private static int malformed3(byte[] src, int sp) {
        int b1 = src[sp++];
        int b2 = src[sp];    // no need to lookup b3
        return ((b1 == (byte)0xe0 && (b2 & 0xe0) == 0x80) ||
                isNotContinuation(b2)) ? 1 : 2;
    }

    private static int malformed4(byte[] src, int sp) {
        // we don't care the speed here
        int b1 = src[sp++] & 0xff;
        int b2 = src[sp++] & 0xff;
        if (b1 > 0xf4 ||
                (b1 == 0xf0 && (b2 < 0x90 || b2 > 0xbf)) ||
                (b1 == 0xf4 && (b2 & 0xf0) != 0x80) ||
                isNotContinuation(b2))
            return 1;
        if (isNotContinuation(src[sp]))
            return 2;
        return 3;
    }

    private static void throwMalformed(int off, int nb) {
        String msg = "malformed input off : " + off + ", length : " + nb;
        throw new IllegalArgumentException(msg, new MalformedInputException(nb));
    }

    private static void throwMalformed(byte[] val) {
        int dp = StringCoding.countPositives(val, 0, val.length);
        throwMalformed(dp, 1);
    }

    private static void throwUnmappable(int off) {
        String msg = "malformed input off : " + off + ", length : 1";
        throw new IllegalArgumentException(msg, new UnmappableCharacterException(1));
    }

    private static void throwUnmappable(byte[] val) {
        int dp = StringCoding.countPositives(val, 0, val.length);
        throwUnmappable(dp);
    }

    private static byte[] encodeUTF8(byte coder, byte[] val, boolean doReplace) {
        if (coder == UTF16) {
            return encodeUTF8_UTF16(val, doReplace);
        }

        if (!StringCoding.hasNegatives(val, 0, val.length)) {
            return val.clone();
        }

        int dp = 0;
        byte[] dst = StringUTF16.newBytesFor(val.length);
        for (byte c : val) {
            if (c < 0) {
                dst[dp++] = (byte) (0xc0 | ((c & 0xff) >> 6));
                dst[dp++] = (byte) (0x80 | (c & 0x3f));
            } else {
                dst[dp++] = c;
            }
        }
        if (dp == dst.length) {
            return dst;
        }
        return Arrays.copyOf(dst, dp);
    }

    private static byte[] encodeUTF8_UTF16(byte[] val, boolean doReplace) {
        int dp = 0;
        int sp = 0;
        int sl = val.length >> 1;
        byte[] dst = new byte[sl * 3];
        while (sp < sl) {
            // ascii fast loop;
            char c = StringUTF16.getChar(val, sp);
            if (c >= '\u0080') {
                break;
            }
            dst[dp++] = (byte)c;
            sp++;
        }
        while (sp < sl) {
            char c = StringUTF16.getChar(val, sp++);
            if (c < 0x80) {
                dst[dp++] = (byte)c;
            } else if (c < 0x800) {
                dst[dp++] = (byte)(0xc0 | (c >> 6));
                dst[dp++] = (byte)(0x80 | (c & 0x3f));
            } else if (Character.isSurrogate(c)) {
                int uc = -1;
                char c2;
                if (Character.isHighSurrogate(c) && sp < sl &&
                        Character.isLowSurrogate(c2 = StringUTF16.getChar(val, sp))) {
                    uc = Character.toCodePoint(c, c2);
                }
                if (uc < 0) {
                    if (doReplace) {
                        dst[dp++] = '?';
                    } else {
                        throwUnmappable(sp - 1);
                    }
                } else {
                    dst[dp++] = (byte)(0xf0 | ((uc >> 18)));
                    dst[dp++] = (byte)(0x80 | ((uc >> 12) & 0x3f));
                    dst[dp++] = (byte)(0x80 | ((uc >>  6) & 0x3f));
                    dst[dp++] = (byte)(0x80 | (uc & 0x3f));
                    sp++;  // 2 chars
                }
            } else {
                // 3 bytes, 16 bits
                dst[dp++] = (byte)(0xe0 | ((c >> 12)));
                dst[dp++] = (byte)(0x80 | ((c >>  6) & 0x3f));
                dst[dp++] = (byte)(0x80 | (c & 0x3f));
            }
        }
        if (dp == dst.length) {
            return dst;
        }
        return Arrays.copyOf(dst, dp);
    }

    /**
     * Constructs a new {@code String} by decoding the specified array of bytes
     * using the specified {@linkplain java.nio.charset.Charset charset}.  The
     * length of the new {@code String} is a function of the charset, and hence
     * may not be equal to the length of the byte array.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the given charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  charsetName
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     *
     * @since  1.1
     */
    public String(byte[] bytes, String charsetName)
            throws UnsupportedEncodingException {
        this(lookupCharset(charsetName), bytes, 0, bytes.length);
    }

    /**
     * Constructs a new {@code String} by decoding the specified array of
     * bytes using the specified {@linkplain java.nio.charset.Charset charset}.
     * The length of the new {@code String} is a function of the charset, and
     * hence may not be equal to the length of the byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  charset
     *         The {@linkplain java.nio.charset.Charset charset} to be used to
     *         decode the {@code bytes}
     *
     * @since  1.6
     */
    public String(byte[] bytes, Charset charset) {
        this(Objects.requireNonNull(charset), bytes, 0, bytes.length);
    }

    /**
     * Constructs a new {@code String} by decoding the specified subarray of
     * bytes using the {@link Charset#defaultCharset() default charset}.
     * The length of the new {@code String} is a function of the charset,
     * and hence may not be equal to the length of the subarray.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the default charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  offset
     *         The index of the first byte to decode
     *
     * @param  length
     *         The number of bytes to decode
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code offset} is negative, {@code length} is negative, or
     *          {@code offset} is greater than {@code bytes.length - length}
     *
     * @since  1.1
     */
    public String(byte[] bytes, int offset, int length) {
        this(Charset.defaultCharset(), bytes, checkBoundsOffCount(offset, length, bytes.length), length);
    }

    /**
     * Constructs a new {@code String} by decoding the specified array of bytes
     * using the {@link Charset#defaultCharset() default charset}. The length
     * of the new {@code String} is a function of the charset, and hence may not
     * be equal to the length of the byte array.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the default charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @since  1.1
     */
    public String(byte[] bytes) {
        this(Charset.defaultCharset(), bytes, 0, bytes.length);
    }

    /**
     * Allocates a new string that contains the sequence of characters
     * currently contained in the string buffer argument. The contents of the
     * string buffer are copied; subsequent modification of the string buffer
     * does not affect the newly created string.
     *
     * @param  buffer
     *         A {@code StringBuffer}
     */
    public String(StringBuffer buffer) {
        this(buffer.toString());
    }

    /**
     * Allocates a new string that contains the sequence of characters
     * currently contained in the string builder argument. The contents of the
     * string builder are copied; subsequent modification of the string builder
     * does not affect the newly created string.
     *
     * <p> This constructor is provided to ease migration to {@code
     * StringBuilder}. Obtaining a string from a string builder via the {@code
     * toString} method is likely to run faster and is generally preferred.
     *
     * @param   builder
     *          A {@code StringBuilder}
     *
     * @since  1.5
     */
    public String(StringBuilder builder) {
        this(builder, null);
    }

    /**
     * Returns the length of this string.
     * The length is equal to the number of <a href="Character.html#unicode">Unicode
     * code units</a> in the string.
     *
     * @return  the length of the sequence of characters represented by this
     *          object.
     */
    public int length() {
        M3String storage = m3();
        return storage == null ? value.length >> coder() : storage.length();
    }

    /**
     * Returns {@code true} if, and only if, {@link #length()} is {@code 0}.
     *
     * @return {@code true} if {@link #length()} is {@code 0}, otherwise
     * {@code false}
     *
     * @since 1.6
     */
    @Override
    public boolean isEmpty() {
        M3String storage = m3();
        return storage == null ? value.length == 0 : storage.length() == 0;
    }

    /**
     * Returns the {@code char} value at the
     * specified index. An index ranges from {@code 0} to
     * {@code length() - 1}. The first {@code char} value of the sequence
     * is at index {@code 0}, the next at index {@code 1},
     * and so on, as for array indexing.
     *
     * <p>If the {@code char} value specified by the index is a
     * <a href="Character.html#unicode">surrogate</a>, the surrogate
     * value is returned.
     *
     * @param      index   the index of the {@code char} value.
     * @return     the {@code char} value at the specified index of this string.
     *             The first {@code char} value is at index {@code 0}.
     * @throws     IndexOutOfBoundsException  if the {@code index}
     *             argument is negative or not less than the length of this
     *             string.
     */
    public char charAt(int index) {
        //FLLDCIM: LAYOUT - decision 50: an M3-backed String reads value with its canonical coder
        if (isLatin1()) {
            return StringLatin1.charAt(value, index);
        } else {
            return StringUTF16.charAt(value, index);
        }
    }

    /**
     * Returns the character (Unicode code point) at the specified
     * index. The index refers to {@code char} values
     * (Unicode code units) and ranges from {@code 0} to
     * {@link #length()}{@code  - 1}.
     *
     * <p> If the {@code char} value specified at the given index
     * is in the high-surrogate range, the following index is less
     * than the length of this {@code String}, and the
     * {@code char} value at the following index is in the
     * low-surrogate range, then the supplementary code point
     * corresponding to this surrogate pair is returned. Otherwise,
     * the {@code char} value at the given index is returned.
     *
     * @param      index the index to the {@code char} values
     * @return     the code point value of the character at the
     *             {@code index}
     * @throws     IndexOutOfBoundsException  if the {@code index}
     *             argument is negative or not less than the length of this
     *             string.
     * @since      1.5
     */
    public int codePointAt(int index) {
        if (isLatin1()) {
            checkIndex(index, value.length);
            return value[index] & 0xff;
        }
        int length = value.length >> 1;
        checkIndex(index, length);
        return StringUTF16.codePointAt(value, index, length);
    }

    /**
     * Returns the character (Unicode code point) before the specified
     * index. The index refers to {@code char} values
     * (Unicode code units) and ranges from {@code 1} to {@link
     * CharSequence#length() length}.
     *
     * <p> If the {@code char} value at {@code (index - 1)}
     * is in the low-surrogate range, {@code (index - 2)} is not
     * negative, and the {@code char} value at {@code (index -
     * 2)} is in the high-surrogate range, then the
     * supplementary code point value of the surrogate pair is
     * returned. If the {@code char} value at {@code index -
     * 1} is an unpaired low-surrogate or a high-surrogate, the
     * surrogate value is returned.
     *
     * @param     index the index following the code point that should be returned
     * @return    the Unicode code point value before the given index.
     * @throws    IndexOutOfBoundsException if the {@code index}
     *            argument is less than 1 or greater than the length
     *            of this string.
     * @since     1.5
     */
    public int codePointBefore(int index) {
        int i = index - 1;
        checkIndex(i, length());
        if (isLatin1()) {
            return (value[i] & 0xff);
        }
        return StringUTF16.codePointBefore(value, index);
    }

    /**
     * Returns the number of Unicode code points in the specified text
     * range of this {@code String}. The text range begins at the
     * specified {@code beginIndex} and extends to the
     * {@code char} at index {@code endIndex - 1}. Thus the
     * length (in {@code char}s) of the text range is
     * {@code endIndex-beginIndex}. Unpaired surrogates within
     * the text range count as one code point each.
     *
     * @param beginIndex the index to the first {@code char} of
     * the text range.
     * @param endIndex the index after the last {@code char} of
     * the text range.
     * @return the number of Unicode code points in the specified text
     * range
     * @throws    IndexOutOfBoundsException if the
     * {@code beginIndex} is negative, or {@code endIndex}
     * is larger than the length of this {@code String}, or
     * {@code beginIndex} is larger than {@code endIndex}.
     * @since  1.5
     */
    public int codePointCount(int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, length());
        if (isLatin1()) {
            return endIndex - beginIndex;
        }
        M3String storage = m3();
        if (storage != null) {
            // Same exact gate as offsetByCodePoints: without a surrogate pair in the value,
            // every range holds one code point per unit. The geometry answers repeated queries.
            M3StringFacts prepared = storage.factsIfPrepared();
            if (prepared != null && prepared.codePointCount == storage.length()) {
                return endIndex - beginIndex;
            }
            M3StringCodePointPrecompute.Geometry geometry =
                    M3StringCodePointPrecompute.prepare(storage);
            if (geometry != null) {
                return geometry.codePointCount(beginIndex, endIndex);
            }
        }
        //FLLDCIM: LAYOUT - decision 50: counted over value, no facts scan of the range
        return StringUTF16.codePointCount(value, beginIndex, endIndex);
    }

    /**
     * Returns the index within this {@code String} that is
     * offset from the given {@code index} by
     * {@code codePointOffset} code points. Unpaired surrogates
     * within the text range given by {@code index} and
     * {@code codePointOffset} count as one code point each.
     *
     * @param index the index to be offset
     * @param codePointOffset the offset in code points
     * @return the index within this {@code String}
     * @throws    IndexOutOfBoundsException if {@code index}
     *   is negative or larger than the length of this
     *   {@code String}, or if {@code codePointOffset} is positive
     *   and the substring starting with {@code index} has fewer
     *   than {@code codePointOffset} code points,
     *   or if {@code codePointOffset} is negative and the substring
     *   before {@code index} has fewer than the absolute value
     *   of {@code codePointOffset} code points.
     * @since 1.5
     */
    public int offsetByCodePoints(int index, int codePointOffset) {
        M3String storage = m3();
        if (storage != null && index >= 0 && index <= storage.length()) {
            M3StringFacts prepared = storage.factsIfPrepared();
            boolean oneUnitPerCodePoint =
                    isLatin1()
                            || (prepared != null
                                    && prepared.codePointCount == storage.length());
            if (oneUnitPerCodePoint) {
                long result = (long) index + codePointOffset;
                if (result >= 0L && result <= storage.length()) {
                    return (int) result;
                }
            }
            M3StringCodePointPrecompute.Geometry geometry =
                    M3StringCodePointPrecompute.prepare(storage);
            if (geometry != null) {
                return geometry.offsetByCodePoints(index, codePointOffset);
            }
        }
        return Character.offsetByCodePoints(this, index, codePointOffset);
    }

    /**
     * Copies characters from this string into the destination character
     * array.
     * <p>
     * The first character to be copied is at index {@code srcBegin};
     * the last character to be copied is at index {@code srcEnd-1}
     * (thus the total number of characters to be copied is
     * {@code srcEnd-srcBegin}). The characters are copied into the
     * subarray of {@code dst} starting at index {@code dstBegin}
     * and ending at index:
     * <blockquote><pre>
     *     dstBegin + (srcEnd-srcBegin) - 1
     * </pre></blockquote>
     *
     * @param      srcBegin   index of the first character in the string
     *                        to copy.
     * @param      srcEnd     index after the last character in the string
     *                        to copy.
     * @param      dst        the destination array.
     * @param      dstBegin   the start offset in the destination array.
     * @throws    IndexOutOfBoundsException If any of the following
     *            is true:
     *            <ul><li>{@code srcBegin} is negative.
     *            <li>{@code srcBegin} is greater than {@code srcEnd}
     *            <li>{@code srcEnd} is greater than the length of this
     *                string
     *            <li>{@code dstBegin} is negative
     *            <li>{@code dstBegin+(srcEnd-srcBegin)} is larger than
     *                {@code dst.length}</ul>
     */
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        checkBoundsBeginEnd(srcBegin, srcEnd, length());
        checkBoundsOffCount(dstBegin, srcEnd - srcBegin, dst.length);
        char[] chars = srcBegin == 0 && srcEnd == length() ? constChars() : chValue;
        if (chars != null) {
            System.arraycopy(chars, srcBegin, dst, dstBegin, srcEnd - srcBegin);
            return;
        }
        if (isLatin1()) {
            StringLatin1.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        } else {
            StringUTF16.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        }
    }

    /**
     * Copies characters from this string into the destination byte array. Each
     * byte receives the 8 low-order bits of the corresponding character. The
     * eight high-order bits of each character are not copied and do not
     * participate in the transfer in any way.
     *
     * <p> The first character to be copied is at index {@code srcBegin}; the
     * last character to be copied is at index {@code srcEnd-1}.  The total
     * number of characters to be copied is {@code srcEnd-srcBegin}. The
     * characters, converted to bytes, are copied into the subarray of {@code
     * dst} starting at index {@code dstBegin} and ending at index:
     *
     * <blockquote><pre>
     *     dstBegin + (srcEnd-srcBegin) - 1
     * </pre></blockquote>
     *
     * @deprecated  This method does not properly convert characters into
     * bytes.  As of JDK&nbsp;1.1, the preferred way to do this is via the
     * {@link #getBytes()} method, which uses the {@link Charset#defaultCharset()
     * default charset}.
     *
     * @param  srcBegin
     *         Index of the first character in the string to copy
     *
     * @param  srcEnd
     *         Index after the last character in the string to copy
     *
     * @param  dst
     *         The destination array
     *
     * @param  dstBegin
     *         The start offset in the destination array
     *
     * @throws  IndexOutOfBoundsException
     *          If any of the following is true:
     *          <ul>
     *            <li> {@code srcBegin} is negative
     *            <li> {@code srcBegin} is greater than {@code srcEnd}
     *            <li> {@code srcEnd} is greater than the length of this String
     *            <li> {@code dstBegin} is negative
     *            <li> {@code dstBegin+(srcEnd-srcBegin)} is larger than {@code
     *                 dst.length}
     *          </ul>
     */
    @Deprecated(since="1.1")
    public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
        checkBoundsBeginEnd(srcBegin, srcEnd, length());
        Objects.requireNonNull(dst);
        checkBoundsOffCount(dstBegin, srcEnd - srcBegin, dst.length);
        M3String storage = m3();
        if (storage != null) {
            storage.getBytes(dst, srcBegin, dstBegin, LATIN1, srcEnd - srcBegin);
        } else if (isLatin1()) {
            StringLatin1.getBytes(value, srcBegin, srcEnd, dst, dstBegin);
        } else {
            StringUTF16.getBytes(value, srcBegin, srcEnd, dst, dstBegin);
        }
    }

    /**
     * Encodes this {@code String} into a sequence of bytes using the named
     * charset, storing the result into a new byte array.
     *
     * <p> The behavior of this method when this string cannot be encoded in
     * the given charset is unspecified.  The {@link
     * java.nio.charset.CharsetEncoder} class should be used when more control
     * over the encoding process is required.
     *
     * @param  charsetName
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @return  The resultant byte array
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     *
     * @since  1.1
     */
    public byte[] getBytes(String charsetName)
            throws UnsupportedEncodingException {
        Charset charset = lookupCharset(charsetName);
        M3String storage = m3();
        return storage != null ? storage.encode(charset) : encode(charset, coder(), value());
    }

    /**
     * Encodes this {@code String} into a sequence of bytes using the given
     * {@linkplain java.nio.charset.Charset charset}, storing the result into a
     * new byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement byte array.  The
     * {@link java.nio.charset.CharsetEncoder} class should be used when more
     * control over the encoding process is required.
     *
     * @param  charset
     *         The {@linkplain java.nio.charset.Charset} to be used to encode
     *         the {@code String}
     *
     * @return  The resultant byte array
     *
     * @since  1.6
     */
    public byte[] getBytes(Charset charset) {
        if (charset == null) throw new NullPointerException();
        M3String storage = m3();
        return storage != null ? storage.encode(charset) : encode(charset, coder(), value());
     }

    /**
     * Encodes this {@code String} into a sequence of bytes using the
     * {@link Charset#defaultCharset() default charset}, storing the result
     * into a new byte array.
     *
     * <p> The behavior of this method when this string cannot be encoded in
     * the default charset is unspecified.  The {@link
     * java.nio.charset.CharsetEncoder} class should be used when more control
     * over the encoding process is required.
     *
     * @return  The resultant byte array
     *
     * @since      1.1
     */
    public byte[] getBytes() {
        Charset charset = Charset.defaultCharset();
        M3String storage = m3();
        return storage != null ? storage.encode(charset) : encode(charset, coder(), value());
    }

    /**
     * Compares this string to the specified object.  The result is {@code
     * true} if and only if the argument is not {@code null} and is a {@code
     * String} object that represents the same sequence of characters as this
     * object.
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param  anObject
     *         The object to compare this {@code String} against
     *
     * @return  {@code true} if the given object represents a {@code String}
     *          equivalent to this string, {@code false} otherwise
     *
     * @see  #compareTo(String)
     * @see  #equalsIgnoreCase(String)
     */
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (!(anObject instanceof String aString)) {
            return false;
        }
        M3String leftStorage = m3();
        M3String rightStorage = aString.m3();
        if (leftStorage != null || rightStorage != null) {
            //FLLDCIM: LAYOUT - decision 50: O(1) gates, then the stock compare of the values
            int known = M3String.equalityGate(this, leftStorage, aString, rightStorage);
            if (known != M3String.GATE_UNKNOWN) {
                return known == M3String.GATE_EQUAL;
            }
        }
        return (!COMPACT_STRINGS || this.coder == aString.coder)
                && StringLatin1.equals(value, aString.value);
    }

    /**
     * Mixed-side equality gate: this flat String's cached hash (only when {@link #hashCode()} has
     * already run) against the M3 String's canonical hash when that is already known. Equal content
     * implies equal hashes, so a difference proves inequality without reading native storage; an
     * unknown hash on either side proves nothing. Reads of {@code hash}/{@code hashIsZero} are the
     * same benign race as in {@link #hashCode()}.
     */
    boolean cachedHashDiffers(M3String storage) {
        int h = hash;
        if (h == 0 && !hashIsZero) return false;
        return storage.hashKnownToDiffer(h);
    }

    /**
     * Compares this string to the specified {@code StringBuffer}.  The result
     * is {@code true} if and only if this {@code String} represents the same
     * sequence of characters as the specified {@code StringBuffer}. This method
     * synchronizes on the {@code StringBuffer}.
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param  sb
     *         The {@code StringBuffer} to compare this {@code String} against
     *
     * @return  {@code true} if this {@code String} represents the same
     *          sequence of characters as the specified {@code StringBuffer},
     *          {@code false} otherwise
     *
     * @since  1.4
     */
    public boolean contentEquals(StringBuffer sb) {
        return contentEquals((CharSequence)sb);
    }

    private boolean nonSyncContentEquals(AbstractStringBuilder sb) {
        int len = length();
        if (len != sb.length()) {
            return false;
        }
        byte[] v1 = value();
        byte[] v2 = sb.getValue();
        byte coder = coder();
        if (coder == sb.getCoder()) {
            return v1.length <= v2.length && ArraysSupport.mismatch(v1, v2, v1.length) < 0;
        } else {
            if (coder != LATIN1) {  // utf16 str and latin1 abs can never be "equal"
                return false;
            }
            return StringUTF16.contentEquals(v1, v2, len);
        }
    }

    /**
     * Compares this string to the specified {@code CharSequence}.  The
     * result is {@code true} if and only if this {@code String} represents the
     * same sequence of char values as the specified sequence. Note that if the
     * {@code CharSequence} is a {@code StringBuffer} then the method
     * synchronizes on it.
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param  cs
     *         The sequence to compare this {@code String} against
     *
     * @return  {@code true} if this {@code String} represents the same
     *          sequence of char values as the specified sequence, {@code
     *          false} otherwise
     *
     * @since  1.5
     */
    public boolean contentEquals(CharSequence cs) {
        // Argument is a StringBuffer, StringBuilder
        if (cs instanceof AbstractStringBuilder) {
            if (cs instanceof StringBuffer) {
                synchronized(cs) {
                   return nonSyncContentEquals((AbstractStringBuilder)cs);
                }
            } else {
                return nonSyncContentEquals((AbstractStringBuilder)cs);
            }
        }
        // Argument is a String
        if (cs instanceof String) {
            return equals(cs);
        }
        // Argument is a generic CharSequence
        int n = cs.length();
        if (n != length()) {
            return false;
        }
        byte[] val = value();
        if (isLatin1()) {
            for (int i = 0; i < n; i++) {
                if ((val[i] & 0xff) != cs.charAt(i)) {
                    return false;
                }
            }
        } else {
            if (!StringUTF16.contentEquals(val, cs, n)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Mixed-side ignore-case gate (one M3 side, one flat side, as in {@code equalsIgnoreCase} with a
     * literal): when the M3 side covers its whole String, its prepared facts are ASCII and the flat
     * region is ASCII, regions that are equal ignoring case have equal ASCII-lower hashes. The flat
     * region's hash is folded without allocation; the M3 side is not read. Never prepares facts.
     */
    private static boolean mixedAsciiCaseHashDiffers(M3String storage, int storageOffset,
            String flat, int flatOffset, int len) {
        if (storageOffset != 0 || len != storage.length()) return false;
        M3StringFacts prepared = storage.factsIfPrepared();
        if (prepared == null || !prepared.ascii) return false;
        long folded = M3StringFacts.asciiLowerHashOrNegative(flat, flatOffset, flatOffset + len);
        return folded >= 0 && (int) folded != prepared.asciiLowerHash;
    }

    /**
     * Compares this {@code String} to another {@code String}, ignoring case
     * considerations.  Two strings are considered equal ignoring case if they
     * are of the same length and corresponding Unicode code points in the two
     * strings are equal ignoring case.
     *
     * <p> Two Unicode code points are considered the same
     * ignoring case if at least one of the following is true:
     * <ul>
     *   <li> The two Unicode code points are the same (as compared by the
     *        {@code ==} operator)
     *   <li> Calling {@code Character.toLowerCase(Character.toUpperCase(int))}
     *        on each Unicode code point produces the same result
     * </ul>
     *
     * <p>Note that this method does <em>not</em> take locale into account, and
     * will result in unsatisfactory results for certain locales.  The
     * {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @param  anotherString
     *         The {@code String} to compare this {@code String} against
     *
     * @return  {@code true} if the argument is not {@code null} and it
     *          represents an equivalent {@code String} ignoring case; {@code
     *          false} otherwise
     *
     * @see  #equals(Object)
     * @see  #codePoints()
     */
    public boolean equalsIgnoreCase(String anotherString) {
        return (this == anotherString) ? true
                : (anotherString != null)
                && (anotherString.length() == length())
                && regionMatches(true, 0, anotherString, 0, length());
    }

    /**
     * Compares two strings lexicographically.
     * The comparison is based on the Unicode value of each character in
     * the strings. The character sequence represented by this
     * {@code String} object is compared lexicographically to the
     * character sequence represented by the argument string. The result is
     * a negative integer if this {@code String} object
     * lexicographically precedes the argument string. The result is a
     * positive integer if this {@code String} object lexicographically
     * follows the argument string. The result is zero if the strings
     * are equal; {@code compareTo} returns {@code 0} exactly when
     * the {@link #equals(Object)} method would return {@code true}.
     * <p>
     * This is the definition of lexicographic ordering. If two strings are
     * different, then either they have different characters at some index
     * that is a valid index for both strings, or their lengths are different,
     * or both. If they have different characters at one or more index
     * positions, let <i>k</i> be the smallest such index; then the string
     * whose character at position <i>k</i> has the smaller value, as
     * determined by using the {@code <} operator, lexicographically precedes the
     * other string. In this case, {@code compareTo} returns the
     * difference of the two character values at position {@code k} in
     * the two string -- that is, the value:
     * <blockquote><pre>
     * this.charAt(k)-anotherString.charAt(k)
     * </pre></blockquote>
     * If there is no index position at which they differ, then the shorter
     * string lexicographically precedes the longer string. In this case,
     * {@code compareTo} returns the difference of the lengths of the
     * strings -- that is, the value:
     * <blockquote><pre>
     * this.length()-anotherString.length()
     * </pre></blockquote>
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param   anotherString   the {@code String} to be compared.
     * @return  the value {@code 0} if the argument string is equal to
     *          this string; a value less than {@code 0} if this string
     *          is lexicographically less than the string argument; and a
     *          value greater than {@code 0} if this string is
     *          lexicographically greater than the string argument.
     */
    public int compareTo(String anotherString) {
        M3String leftM3 = m3();
        if (leftM3 != null && leftM3.sameCoordinate(anotherString.m3())) {
            return 0;
        }
        //FLLDCIM: LAYOUT - decision 50: M3-backed Strings compare their values like flat ones
        byte[] v1 = value();
        byte[] v2 = anotherString.value();
        byte coder = coder();
        if (coder == anotherString.coder()) {
            return coder == LATIN1 ? StringLatin1.compareTo(v1, v2)
                                   : StringUTF16.compareTo(v1, v2);
        }
        return coder == LATIN1 ? StringLatin1.compareToUTF16(v1, v2)
                               : StringUTF16.compareToLatin1(v1, v2);
     }

    /**
     * A Comparator that orders {@code String} objects as by
     * {@link #compareToIgnoreCase(String) compareToIgnoreCase}.
     * This comparator is serializable.
     * <p>
     * Note that this Comparator does <em>not</em> take locale into account,
     * and will result in an unsatisfactory ordering for certain locales.
     * The {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @see     java.text.Collator
     * @since   1.2
     */
    public static final Comparator<String> CASE_INSENSITIVE_ORDER
                                         = new CaseInsensitiveComparator();

    /**
     * CaseInsensitiveComparator for Strings.
     */
    private static class CaseInsensitiveComparator
            implements Comparator<String>, java.io.Serializable {
        // use serialVersionUID from JDK 1.2.2 for interoperability
        @java.io.Serial
        private static final long serialVersionUID = 8575799808933029326L;

        public int compare(String s1, String s2) {
            byte[] v1 = s1.value();
            byte[] v2 = s2.value();
            byte coder = s1.coder();
            if (coder == s2.coder()) {
                return coder == LATIN1 ? StringLatin1.compareToCI(v1, v2)
                                       : StringUTF16.compareToCI(v1, v2);
            }
            return coder == LATIN1 ? StringLatin1.compareToCI_UTF16(v1, v2)
                                   : StringUTF16.compareToCI_Latin1(v1, v2);
        }

        /** Replaces the de-serialized object. */
        @java.io.Serial
        private Object readResolve() { return CASE_INSENSITIVE_ORDER; }
    }

    /**
     * Compares two strings lexicographically, ignoring case
     * differences. This method returns an integer whose sign is that of
     * calling {@code compareTo} with case folded versions of the strings
     * where case differences have been eliminated by calling
     * {@code Character.toLowerCase(Character.toUpperCase(int))} on
     * each Unicode code point.
     * <p>
     * Note that this method does <em>not</em> take locale into account,
     * and will result in an unsatisfactory ordering for certain locales.
     * The {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @param   str   the {@code String} to be compared.
     * @return  a negative integer, zero, or a positive integer as the
     *          specified String is greater than, equal to, or less
     *          than this String, ignoring case considerations.
     * @see     java.text.Collator
     * @see     #codePoints()
     * @since   1.2
     */
    public int compareToIgnoreCase(String str) {
        return CASE_INSENSITIVE_ORDER.compare(this, str);
    }

    /**
     * Tests if two string regions are equal.
     * <p>
     * A substring of this {@code String} object is compared to a substring
     * of the argument other. The result is true if these substrings
     * represent identical character sequences. The substring of this
     * {@code String} object to be compared begins at index {@code toffset}
     * and has length {@code len}. The substring of other to be compared
     * begins at index {@code ooffset} and has length {@code len}. The
     * result is {@code false} if and only if at least one of the following
     * is true:
     * <ul><li>{@code toffset} is negative.
     * <li>{@code ooffset} is negative.
     * <li>{@code toffset+len} is greater than the length of this
     * {@code String} object.
     * <li>{@code ooffset+len} is greater than the length of the other
     * argument.
     * <li>There is some nonnegative integer <i>k</i> less than {@code len}
     * such that:
     * {@code this.charAt(toffset + }<i>k</i>{@code ) != other.charAt(ooffset + }
     * <i>k</i>{@code )}
     * </ul>
     *
     * <p>Note that this method does <em>not</em> take locale into account.  The
     * {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @param   toffset   the starting offset of the subregion in this string.
     * @param   other     the string argument.
     * @param   ooffset   the starting offset of the subregion in the string
     *                    argument.
     * @param   len       the number of characters to compare.
     * @return  {@code true} if the specified subregion of this string
     *          exactly matches the specified subregion of the string argument;
     *          {@code false} otherwise.
     */
    public boolean regionMatches(int toffset, String other, int ooffset, int len) {
        // Note: toffset, ooffset, or len might be near -1>>>1.
        if ((ooffset < 0) || (toffset < 0) ||
             (toffset > (long)length() - len) ||
             (ooffset > (long)other.length() - len)) {
            return false;
        }
        // Any strings match if len <= 0
        if (len <= 0) { //FLLDCIM: MERGE-21U - upstream len <= 0 rule ahead of the M3 slice path
           return true;
        }
        M3String leftM3 = m3();
        M3String rightM3 = other.m3();
        if (leftM3 != null && rightM3 != null) {
            M3String leftRange = leftM3.slice(toffset, toffset + len);
            M3String rightRange = rightM3.slice(ooffset, ooffset + len);
            if (leftRange.sameCoordinate(rightRange)) return true;
            M3StringFacts leftFacts = leftRange.factsIfPrepared();
            M3StringFacts rightFacts = rightRange.factsIfPrepared();
            if (leftFacts != null && rightFacts != null
                    && leftFacts.javaHash != rightFacts.javaHash) {
                return false;
            }
        }
        //FLLDCIM: LAYOUT - decision 50: after the O(1) gates, the stock compare of the values
        byte[] tv = value();
        byte[] ov = other.value();
        byte coder = coder();
        if (coder == other.coder()) {
            if (coder == UTF16) {
                toffset <<= UTF16;
                ooffset <<= UTF16;
                len <<= UTF16;
            }
            return ArraysSupport.mismatch(tv, toffset, ov, ooffset, len) < 0;
        }
        if (coder == LATIN1) {
            while (len-- > 0) {
                if (StringLatin1.getChar(tv, toffset++) != StringUTF16.getChar(ov, ooffset++)) return false;
            }
        } else {
            while (len-- > 0) {
                if (StringUTF16.getChar(tv, toffset++) != StringLatin1.getChar(ov, ooffset++)) return false;
            }
        }
        return true;
    }

    /**
     * Tests if two string regions are equal.
     * <p>
     * A substring of this {@code String} object is compared to a substring
     * of the argument {@code other}. The result is {@code true} if these
     * substrings represent Unicode code point sequences that are the same,
     * ignoring case if and only if {@code ignoreCase} is true.
     * The sequences {@code tsequence} and {@code osequence} are compared,
     * where {@code tsequence} is the sequence produced as if by calling
     * {@code this.substring(toffset, toffset + len).codePoints()} and
     * {@code osequence} is the sequence produced as if by calling
     * {@code other.substring(ooffset, ooffset + len).codePoints()}.
     * The result is {@code true} if and only if all of the following
     * are true:
     * <ul><li>{@code toffset} is non-negative.
     * <li>{@code ooffset} is non-negative.
     * <li>{@code toffset+len} is less than or equal to the length of this
     * {@code String} object.
     * <li>{@code ooffset+len} is less than or equal to the length of the other
     * argument.
     * <li>if {@code ignoreCase} is {@code false}, all pairs of corresponding Unicode
     * code points are equal integer values; or if {@code ignoreCase} is {@code true},
     * {@link Character#toLowerCase(int) Character.toLowerCase(}
     * {@link Character#toUpperCase(int)}{@code )} on all pairs of Unicode code points
     * results in equal integer values.
     * </ul>
     *
     * <p>Note that this method does <em>not</em> take locale into account,
     * and will result in unsatisfactory results for certain locales when
     * {@code ignoreCase} is {@code true}.  The {@link java.text.Collator} class
     * provides locale-sensitive comparison.
     *
     * @param   ignoreCase   if {@code true}, ignore case when comparing
     *                       characters.
     * @param   toffset      the starting offset of the subregion in this
     *                       string.
     * @param   other        the string argument.
     * @param   ooffset      the starting offset of the subregion in the string
     *                       argument.
     * @param   len          the number of characters (Unicode code units -
     *                       16bit {@code char} value) to compare.
     * @return  {@code true} if the specified subregion of this string
     *          matches the specified subregion of the string argument;
     *          {@code false} otherwise. Whether the matching is exact
     *          or case insensitive depends on the {@code ignoreCase}
     *          argument.
     * @see     #codePoints()
     */
    public boolean regionMatches(boolean ignoreCase, int toffset,
            String other, int ooffset, int len) {
        if (!ignoreCase) {
            return regionMatches(toffset, other, ooffset, len);
        }
        if ((ooffset < 0) || (toffset < 0)
                || (toffset > (long)length() - len)
                || (ooffset > (long)other.length() - len)) {
            return false;
        }
        M3String leftM3 = m3();
        M3String rightM3 = other.m3();
        if (len > 0 && (leftM3 != null || rightM3 != null)) {
            if (leftM3 != null && rightM3 != null) {
                M3StringFacts leftFacts =
                        leftM3.slice(toffset, toffset + len).factsIfPrepared();
                M3StringFacts rightFacts =
                        rightM3.slice(ooffset, ooffset + len).factsIfPrepared();
                if (leftFacts != null && rightFacts != null
                        && leftFacts.ascii && rightFacts.ascii
                        && (leftFacts.asciiLowerHash != rightFacts.asciiLowerHash
                                || leftFacts.asciiUpperHash != rightFacts.asciiUpperHash
                                || leftFacts.asciiTitleHash != rightFacts.asciiTitleHash)) {
                    return false;
                }
            } else if (leftM3 != null
                    ? mixedAsciiCaseHashDiffers(leftM3, toffset, other, ooffset, len)
                    : mixedAsciiCaseHashDiffers(rightM3, ooffset, this, toffset, len)) {
                return false;
            }
        }
        //FLLDCIM: LAYOUT - decision 50: after the O(1) gates, the stock compare of the values
        byte[] tv = value();
        byte[] ov = other.value();
        byte coder = coder();
        if (coder == other.coder()) {
            return coder == LATIN1
              ? StringLatin1.regionMatchesCI(tv, toffset, ov, ooffset, len)
              : StringUTF16.regionMatchesCI(tv, toffset, ov, ooffset, len);
        }
        return coder == LATIN1
              ? StringLatin1.regionMatchesCI_UTF16(tv, toffset, ov, ooffset, len)
              : StringUTF16.regionMatchesCI_Latin1(tv, toffset, ov, ooffset, len);
    }

    /**
     * Tests if the substring of this string beginning at the
     * specified index starts with the specified prefix.
     *
     * @param   prefix    the prefix.
     * @param   toffset   where to begin looking in this string.
     * @return  {@code true} if the character sequence represented by the
     *          argument is a prefix of the substring of this object starting
     *          at index {@code toffset}; {@code false} otherwise.
     *          The result is {@code false} if {@code toffset} is
     *          negative or greater than the length of this
     *          {@code String} object; otherwise the result is the same
     *          as the result of the expression
     *          <pre>
     *          this.substring(toffset).startsWith(prefix)
     *          </pre>
     */
    public boolean startsWith(String prefix, int toffset) {
        // Note: toffset might be near -1>>>1.
        if (toffset < 0 || toffset > length() - prefix.length()) {
            return false;
        }
        M3String sourceM3 = m3();
        if (sourceM3 != null) {
            return M3StringPrecomputeBridge.startsWith(sourceM3, prefix, toffset);
        }
        M3String prefixM3 = prefix.m3();
        if (prefixM3 != null) {
            // A flat receiver compares an M3-backed prefix by content. The prefix is not
            // flattened, and its coder is not tested: a range reports the coder of its
            // owner, so Latin-1 content can sit behind a UTF-16 coder.
            int count = prefix.length();
            return toffset == 0
                    ? prefixM3.compareFlatPrefix(this, count) == 0
                    : regionMatches(toffset, prefix, 0, count);
        }
        byte[] ta = value();
        byte[] pa = prefix.value();
        int po = 0;
        int pc = pa.length;
        byte coder = coder();
        if (coder == prefix.coder()) {
            if (coder == UTF16) {
                toffset <<= UTF16;
            }
            return ArraysSupport.mismatch(ta, toffset,
                    pa, 0, pc) < 0;
        } else {
            if (coder == LATIN1) {  // && pcoder == UTF16
                return false;
            }
            // coder == UTF16 && pcoder == LATIN1)
            while (po < pc) {
                if (StringUTF16.getChar(ta, toffset++) != (pa[po++] & 0xff)) {
                    return false;
               }
            }
        }
        return true;
    }

    /**
     * Tests if this string starts with the specified prefix.
     *
     * @param   prefix   the prefix.
     * @return  {@code true} if the character sequence represented by the
     *          argument is a prefix of the character sequence represented by
     *          this string; {@code false} otherwise.
     *          Note also that {@code true} will be returned if the
     *          argument is an empty string or is equal to this
     *          {@code String} object as determined by the
     *          {@link #equals(Object)} method.
     * @since   1.0
     */
    public boolean startsWith(String prefix) {
        return startsWith(prefix, 0);
    }

    /**
     * Tests if this string ends with the specified suffix.
     *
     * @param   suffix   the suffix.
     * @return  {@code true} if the character sequence represented by the
     *          argument is a suffix of the character sequence represented by
     *          this object; {@code false} otherwise. Note that the
     *          result will be {@code true} if the argument is the
     *          empty string or is equal to this {@code String} object
     *          as determined by the {@link #equals(Object)} method.
     */
    public boolean endsWith(String suffix) {
        return startsWith(suffix, length() - suffix.length());
    }

    /**
     * Returns a hash code for this string. The hash code for a
     * {@code String} object is computed as
     * <blockquote><pre>
     * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
     * </pre></blockquote>
     * using {@code int} arithmetic, where {@code s[i]} is the
     * <i>i</i>th character of the string, {@code n} is the length of
     * the string, and {@code ^} indicates exponentiation.
     * (The hash value of the empty string is zero.)
     *
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        // The hash or hashIsZero fields are subject to a benign data race,
        // making it crucial to ensure that any observable result of the
        // calculation in this method stays correct under any possible read of
        // these fields. Necessary restrictions to allow this to be correct
        // without explicit memory fences or similar concurrency primitives is
        // that we can ever only write to one of these two fields for a given
        // String instance, and that the computation is idempotent and derived
        // from immutable state
        int h = hash;
        if (h == 0 && !hashIsZero) {
            M3String storage = m3();
            //FLLDCIM: LAYOUT - decision 50: an unknown M3 range hash hashes value (vectorized)
            h = storage != null && storage.hashKnown()
                    ? M3StringPrecomputeBridge.hashCodeValue(storage)
                    : (isLatin1() ? StringLatin1.hashCode(value)
                                  : StringUTF16.hashCode(value));
            if (h == 0) {
                hashIsZero = true;
            } else {
                hash = h;
            }
        }
        return h;
    }

    /**
     * Returns the index within this string of the first occurrence of
     * the specified character. If a character with value
     * {@code ch} occurs in the character sequence represented by
     * this {@code String} object, then the index (in Unicode
     * code units) of the first such occurrence is returned. For
     * values of {@code ch} in the range from 0 to 0xFFFF
     * (inclusive), this is the smallest value <i>k</i> such that:
     * <blockquote><pre>
     * this.charAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the
     * smallest value <i>k</i> such that:
     * <blockquote><pre>
     * this.codePointAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true. In either case, if no such character occurs in this
     * string, then {@code -1} is returned.
     *
     * @param   ch   a character (Unicode code point).
     * @return  the index of the first occurrence of the character in the
     *          character sequence represented by this object, or
     *          {@code -1} if the character does not occur.
     */
    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified character, starting the search at the specified index.
     * <p>
     * If a character with value {@code ch} occurs in the
     * character sequence represented by this {@code String}
     * object at an index no smaller than {@code fromIndex}, then
     * the index of the first such occurrence is returned. For values
     * of {@code ch} in the range from 0 to 0xFFFF (inclusive),
     * this is the smallest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.charAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &gt;= fromIndex)
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the
     * smallest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.codePointAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &gt;= fromIndex)
     * </pre></blockquote>
     * is true. In either case, if no such character occurs in this
     * string at or after position {@code fromIndex}, then
     * {@code -1} is returned.
     *
     * <p>
     * There is no restriction on the value of {@code fromIndex}. If it
     * is negative, it has the same effect as if it were zero: this entire
     * string may be searched. If it is greater than the length of this
     * string, it has the same effect as if it were equal to the length of
     * this string: {@code -1} is returned.
     *
     * <p>All indices are specified in {@code char} values
     * (Unicode code units).
     *
     * @param   ch          a character (Unicode code point).
     * @param   fromIndex   the index to start the search from.
     * @return  the index of the first occurrence of the character in the
     *          character sequence represented by this object that is greater
     *          than or equal to {@code fromIndex}, or {@code -1}
     *          if the character does not occur.
     *
     * @apiNote
     * Unlike {@link #substring(int)}, for example, this method does not throw
     * an exception when {@code fromIndex} is outside the valid range.
     * Rather, it returns -1 when {@code fromIndex} is larger than the length of
     * the string.
     * This result is, by itself, indistinguishable from a genuine absence of
     * {@code ch} in the string.
     * If stricter behavior is needed, {@link #indexOf(int, int, int)}
     * should be considered instead.
     * On a {@link String} {@code s}, for example,
     * {@code s.indexOf(ch, fromIndex, s.length())} would throw if
     * {@code fromIndex} were larger than the string length, or were negative.
     */
    public int indexOf(int ch, int fromIndex) {
        M3String storage = m3();
        if (storage != null) {
            int from = Math.max(fromIndex, 0);
            if (from >= storage.length()) return -1;
            if (Character.isBmpCodePoint(ch)) {
                return storage.indexOf((char) ch, from, storage.length());
            }
            return storage.indexOfCodePoint(ch, from, storage.length());
        }
        return isLatin1() ? StringLatin1.indexOf(value(), ch, fromIndex, length())
                : StringUTF16.indexOf(value(), ch, fromIndex, length());
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified character, starting the search at {@code beginIndex} and
     * stopping before {@code endIndex}.
     *
     * <p>If a character with value {@code ch} occurs in the
     * character sequence represented by this {@code String}
     * object at an index no smaller than {@code beginIndex} but smaller than
     * {@code endIndex}, then
     * the index of the first such occurrence is returned. For values
     * of {@code ch} in the range from 0 to 0xFFFF (inclusive),
     * this is the smallest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.charAt(<i>k</i>) == ch) &amp;&amp; (beginIndex &lt;= <i>k</i> &lt; endIndex)
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the
     * smallest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.codePointAt(<i>k</i>) == ch) &amp;&amp; (beginIndex &lt;= <i>k</i> &lt; endIndex)
     * </pre></blockquote>
     * is true. In either case, if no such character occurs in this
     * string at or after position {@code beginIndex} and before position
     * {@code endIndex}, then {@code -1} is returned.
     *
     * <p>All indices are specified in {@code char} values
     * (Unicode code units).
     *
     * @param   ch          a character (Unicode code point).
     * @param   beginIndex  the index to start the search from (included).
     * @param   endIndex    the index to stop the search at (excluded).
     * @return  the index of the first occurrence of the character in the
     *          character sequence represented by this object that is greater
     *          than or equal to {@code beginIndex} and less than {@code endIndex},
     *          or {@code -1} if the character does not occur.
     * @throws  StringIndexOutOfBoundsException if {@code beginIndex}
     *          is negative, or {@code endIndex} is larger than the length of
     *          this {@code String} object, or {@code beginIndex} is larger than
     *          {@code endIndex}.
     * @since   21
     */
    public int indexOf(int ch, int beginIndex, int endIndex) {
        checkBoundsBeginEnd(beginIndex, endIndex, length());
        M3String storage = m3();
        if (storage != null) {
            if (Character.isBmpCodePoint(ch)) {
                return storage.indexOf((char) ch, beginIndex, endIndex);
            }
            return storage.indexOfCodePoint(ch, beginIndex, endIndex);
        }
        return isLatin1() ? StringLatin1.indexOf(value(), ch, beginIndex, endIndex)
                : StringUTF16.indexOf(value(), ch, beginIndex, endIndex);
    }

    /**
     * Returns the index within this string of the last occurrence of
     * the specified character. For values of {@code ch} in the
     * range from 0 to 0xFFFF (inclusive), the index (in Unicode code
     * units) returned is the largest value <i>k</i> such that:
     * <blockquote><pre>
     * this.charAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the
     * largest value <i>k</i> such that:
     * <blockquote><pre>
     * this.codePointAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true.  In either case, if no such character occurs in this
     * string, then {@code -1} is returned.  The
     * {@code String} is searched backwards starting at the last
     * character.
     *
     * @param   ch   a character (Unicode code point).
     * @return  the index of the last occurrence of the character in the
     *          character sequence represented by this object, or
     *          {@code -1} if the character does not occur.
     */
    public int lastIndexOf(int ch) {
        return lastIndexOf(ch, length() - 1);
    }

    /**
     * Returns the index within this string of the last occurrence of
     * the specified character, searching backward starting at the
     * specified index. For values of {@code ch} in the range
     * from 0 to 0xFFFF (inclusive), the index returned is the largest
     * value <i>k</i> such that:
     * <blockquote><pre>
     * (this.charAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &lt;= fromIndex)
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the
     * largest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.codePointAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &lt;= fromIndex)
     * </pre></blockquote>
     * is true. In either case, if no such character occurs in this
     * string at or before position {@code fromIndex}, then
     * {@code -1} is returned.
     *
     * <p>All indices are specified in {@code char} values
     * (Unicode code units).
     *
     * @param   ch          a character (Unicode code point).
     * @param   fromIndex   the index to start the search from. There is no
     *          restriction on the value of {@code fromIndex}. If it is
     *          greater than or equal to the length of this string, it has
     *          the same effect as if it were equal to one less than the
     *          length of this string: this entire string may be searched.
     *          If it is negative, it has the same effect as if it were -1:
     *          -1 is returned.
     * @return  the index of the last occurrence of the character in the
     *          character sequence represented by this object that is less
     *          than or equal to {@code fromIndex}, or {@code -1}
     *          if the character does not occur before that point.
     */
    public int lastIndexOf(int ch, int fromIndex) {
        M3String storage = m3();
        if (storage != null) {
            int from = Math.min(fromIndex, storage.length() - 1);
            if (from < 0) return -1;
            if (Character.isBmpCodePoint(ch)) {
                return storage.lastIndexOf((char) ch, from);
            }
            return storage.lastIndexOfCodePoint(ch, from);
        }
        return isLatin1() ? StringLatin1.lastIndexOf(value(), ch, fromIndex)
                          : StringUTF16.lastIndexOf(value(), ch, fromIndex);
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring.
     *
     * <p>The returned index is the smallest value {@code k} for which:
     * <pre>{@code
     * this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param   str   the substring to search for.
     * @return  the index of the first occurrence of the specified substring,
     *          or {@code -1} if there is no such occurrence.
     */
    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring, starting at the specified index.
     *
     * <p>The returned index is the smallest value {@code k} for which:
     * <pre>{@code
     *     k >= Math.min(fromIndex, this.length()) &&
     *                   this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @apiNote
     * Unlike {@link #substring(int)}, for example, this method does not throw
     * an exception when {@code fromIndex} is outside the valid range.
     * Rather, it returns -1 when {@code fromIndex} is larger than the length of
     * the string.
     * This result is, by itself, indistinguishable from a genuine absence of
     * {@code str} in the string.
     * If stricter behavior is needed, {@link #indexOf(String, int, int)}
     * should be considered instead.
     * On {@link String} {@code s} and a non-empty {@code str}, for example,
     * {@code s.indexOf(str, fromIndex, s.length())} would throw if
     * {@code fromIndex} were larger than the string length, or were negative.
     *
     * @param   str         the substring to search for.
     * @param   fromIndex   the index from which to start the search.
     * @return  the index of the first occurrence of the specified substring,
     *          starting at the specified index,
     *          or {@code -1} if there is no such occurrence.
     */
    public int indexOf(String str, int fromIndex) {
        Objects.requireNonNull(str, "str");
        M3String sourceM3 = m3();
        if (sourceM3 != null) {
            return M3StringPrecomputeBridge.indexOf(
                    sourceM3, str, fromIndex, sourceM3.length());
        }
        return indexOf(value(), coder(), length(), str, fromIndex);
    }

    /**
     * Returns the index of the first occurrence of the specified substring
     * within the specified index range of {@code this} string.
     *
     * <p>This method returns the same result as the one of the invocation
     * <pre>{@code
     *     s.substring(beginIndex, endIndex).indexOf(str) + beginIndex
     * }</pre>
     * if the index returned by {@link #indexOf(String)} is non-negative,
     * and returns -1 otherwise.
     * (No substring is instantiated, though.)
     *
     * @param   str         the substring to search for.
     * @param   beginIndex  the index to start the search from (included).
     * @param   endIndex    the index to stop the search at (excluded).
     * @return  the index of the first occurrence of the specified substring
     *          within the specified index range,
     *          or {@code -1} if there is no such occurrence.
     * @throws  StringIndexOutOfBoundsException if {@code beginIndex}
     *          is negative, or {@code endIndex} is larger than the length of
     *          this {@code String} object, or {@code beginIndex} is larger than
     *          {@code endIndex}.
     * @since   21
     */
    public int indexOf(String str, int beginIndex, int endIndex) {
        if (str.length() == 1) {
            /* Simple optimization, can be omitted without behavioral impact */
            return indexOf(str.charAt(0), beginIndex, endIndex);
        }
        checkBoundsBeginEnd(beginIndex, endIndex, length());
        M3String sourceM3 = m3();
        if (sourceM3 != null) {
            return M3StringPrecomputeBridge.indexOf(
                    sourceM3, str, beginIndex, endIndex);
        }
        return indexOf(value(), coder(), endIndex, str, beginIndex);
    }

    /**
     * Code shared by String and AbstractStringBuilder to do searches. The
     * source is the character array being searched, and the target
     * is the string being searched for.
     *
     * @param   src       the characters being searched.
     * @param   srcCoder  the coder of the source string.
     * @param   srcCount  last index (exclusive) in the source string.
     * @param   tgtStr    the characters being searched for.
     * @param   fromIndex the index to begin searching from.
     */
    static int indexOf(byte[] src, byte srcCoder, int srcCount,
                       String tgtStr, int fromIndex) {
        fromIndex = Math.clamp(fromIndex, 0, srcCount);
        int tgtCount = tgtStr.length();
        if (tgtCount > srcCount - fromIndex) {
            return -1;
        }
        if (tgtCount == 0) {
            return fromIndex;
        }

        M3String target = tgtStr.m3();
        if (target != null) {
            return M3StringSearchPrecompute.indexOf(
                    src, srcCoder, srcCount, target, fromIndex);
        }

        byte[] tgt = tgtStr.value();
        byte tgtCoder = tgtStr.coder();
        if (srcCoder != tgtCoder && srcCoder == LATIN1) {
            return -1;
        }
        // Recipe-pinned to the existing DENSE_CANDIDATES work bound (8).
        // Short needles or at most eight starts retain the stock intrinsic lane.
        if (tgtCount > 8 && srcCount - tgtCount - fromIndex >= 8
                && jdk.internal.misc.VM.isBooted()) {
            return M3StringSearchPrecompute.twoWay(
                    src, srcCoder, srcCount, null, tgtStr, fromIndex, false);
        }
        if (srcCoder == tgtCoder) {
            return srcCoder == LATIN1
                ? StringLatin1.indexOf(src, srcCount, tgt, tgtCount, fromIndex)
                : StringUTF16.indexOf(src, srcCount, tgt, tgtCount, fromIndex);
        }
        // srcCoder == UTF16 && tgtCoder == LATIN1) {
        return StringUTF16.indexOfLatin1(src, srcCount, tgt, tgtCount, fromIndex);
    }

    /**
     * Returns the index within this string of the last occurrence of the
     * specified substring.  The last occurrence of the empty string ""
     * is considered to occur at the index value {@code this.length()}.
     *
     * <p>The returned index is the largest value {@code k} for which:
     * <pre>{@code
     * this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param   str   the substring to search for.
     * @return  the index of the last occurrence of the specified substring,
     *          or {@code -1} if there is no such occurrence.
     */
    public int lastIndexOf(String str) {
        return lastIndexOf(str, length());
    }

    /**
     * Returns the index within this string of the last occurrence of the
     * specified substring, searching backward starting at the specified index.
     *
     * <p>The returned index is the largest value {@code k} for which:
     * <pre>{@code
     *     k <= Math.min(fromIndex, this.length()) &&
     *                   this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param   str         the substring to search for.
     * @param   fromIndex   the index to start the search from.
     * @return  the index of the last occurrence of the specified substring,
     *          searching backward from the specified index,
     *          or {@code -1} if there is no such occurrence.
     */
    public int lastIndexOf(String str, int fromIndex) {
        Objects.requireNonNull(str, "str");
        M3String storage = m3();
        if (storage != null) {
            return M3StringPrecomputeBridge.lastIndexOf(storage, str, fromIndex);
        }
        return lastIndexOf(value(), coder(), length(), str, fromIndex);
    }

    /**
     * Code shared by String and AbstractStringBuilder to do searches. The
     * source is the character array being searched, and the target
     * is the string being searched for.
     *
     * @param   src         the characters being searched.
     * @param   srcCoder    coder handles the mapping between bytes/chars
     * @param   srcCount    count of the source string.
     * @param   tgtStr      the characters being searched for.
     * @param   fromIndex   the index to begin searching from.
     */
    static int lastIndexOf(byte[] src, byte srcCoder, int srcCount,
                           String tgtStr, int fromIndex) {
        int tgtCount = tgtStr.length();
        /*
         * Check arguments; return immediately where possible. For
         * consistency, don't check for null str.
         */
        int rightIndex = srcCount - tgtCount;
        if (fromIndex > rightIndex) {
            fromIndex = rightIndex;
        }
        if (fromIndex < 0) {
            return -1;
        }
        /* Empty string always matches. */
        if (tgtCount == 0) {
            return fromIndex;
        }

        M3String target = tgtStr.m3();
        if (target != null) {
            return M3StringSearchPrecompute.lastIndexOf(
                    src, srcCoder, srcCount, target, fromIndex);
        }

        byte[] tgt = tgtStr.value();
        byte tgtCoder = tgtStr.coder();
        if (srcCoder != tgtCoder && srcCoder == LATIN1) {
            return -1;
        }
        // Recipe-pinned to the existing DENSE_CANDIDATES work bound (8).
        // Short needles or at most eight starts retain the stock intrinsic lane.
        if (tgtCount > 8 && fromIndex >= 8 && jdk.internal.misc.VM.isBooted()) {
            int first = srcCount - tgtCount - fromIndex;
            int found = M3StringSearchPrecompute.twoWay(
                    src, srcCoder, srcCount, null, tgtStr, first, true);
            return found < 0 ? -1 : srcCount - tgtCount - found;
        }
        if (srcCoder == tgtCoder) {
            return srcCoder == LATIN1
                ? StringLatin1.lastIndexOf(src, srcCount, tgt, tgtCount, fromIndex)
                : StringUTF16.lastIndexOf(src, srcCount, tgt, tgtCount, fromIndex);
        }
        // srcCoder == UTF16 && tgtCoder == LATIN1
        return StringUTF16.lastIndexOfLatin1(src, srcCount, tgt, tgtCount, fromIndex);
    }

    /**
     * Returns a string that is a substring of this string. The
     * substring begins with the character at the specified index and
     * extends to the end of this string. <p>
     * Examples:
     * <blockquote><pre>
     * "unhappy".substring(2) returns "happy"
     * "Harbison".substring(3) returns "bison"
     * "emptiness".substring(9) returns "" (an empty string)
     * </pre></blockquote>
     *
     * @param      beginIndex   the beginning index, inclusive.
     * @return     the specified substring.
     * @throws     IndexOutOfBoundsException  if
     *             {@code beginIndex} is negative or larger than the
     *             length of this {@code String} object.
     */
    public String substring(int beginIndex) {
        return substring(beginIndex, length());
    }

    /**
     * Returns a string that is a substring of this string. The
     * substring begins at the specified {@code beginIndex} and
     * extends to the character at index {@code endIndex - 1}.
     * Thus the length of the substring is {@code endIndex-beginIndex}.
     * <p>
     * Examples:
     * <blockquote><pre>
     * "hamburger".substring(4, 8) returns "urge"
     * "smiles".substring(1, 5) returns "mile"
     * </pre></blockquote>
     *
     * @param      beginIndex   the beginning index, inclusive.
     * @param      endIndex     the ending index, exclusive.
     * @return     the specified substring.
     * @throws     IndexOutOfBoundsException  if the
     *             {@code beginIndex} is negative, or
     *             {@code endIndex} is larger than the length of
     *             this {@code String} object, or
     *             {@code beginIndex} is larger than
     *             {@code endIndex}.
     */
    public String substring(int beginIndex, int endIndex) {
        int length = length();
        checkBoundsBeginEnd(beginIndex, endIndex, length);
        if (beginIndex == 0 && endIndex == length) {
            return this;
        }
        int subLen = endIndex - beginIndex;
        if (subLen == 0) {
            return "";
        }
        M3String storage = m3();
        if (storage != null) {
            return M3StringPrecomputeBridge.substring(storage, beginIndex, endIndex);
        }
        byte[] currentValue = value();
        return isLatin1() ? StringLatin1.newString(currentValue, beginIndex, subLen)
                          : StringUTF16.newString(currentValue, beginIndex, subLen);
    }

    /**
     * Returns a character sequence that is a subsequence of this sequence.
     *
     * <p> An invocation of this method of the form
     *
     * <blockquote><pre>
     * str.subSequence(begin,&nbsp;end)</pre></blockquote>
     *
     * behaves in exactly the same way as the invocation
     *
     * <blockquote><pre>
     * str.substring(begin,&nbsp;end)</pre></blockquote>
     *
     * @apiNote
     * This method is defined so that the {@code String} class can implement
     * the {@link CharSequence} interface.
     *
     * @param   beginIndex   the begin index, inclusive.
     * @param   endIndex     the end index, exclusive.
     * @return  the specified subsequence.
     *
     * @throws  IndexOutOfBoundsException
     *          if {@code beginIndex} or {@code endIndex} is negative,
     *          if {@code endIndex} is greater than {@code length()},
     *          or if {@code beginIndex} is greater than {@code endIndex}
     *
     * @since 1.4
     */
    public CharSequence subSequence(int beginIndex, int endIndex) {
        return this.substring(beginIndex, endIndex);
    }

    /**
     * Concatenates the specified string to the end of this string.
     * <p>
     * If the length of the argument string is {@code 0}, then this
     * {@code String} object is returned. Otherwise, a
     * {@code String} object is returned that represents a character
     * sequence that is the concatenation of the character sequence
     * represented by this {@code String} object and the character
     * sequence represented by the argument string.<p>
     * Examples:
     * <blockquote><pre>
     * "cares".concat("s") returns "caress"
     * "to".concat("get").concat("her") returns "together"
     * </pre></blockquote>
     *
     * @param   str   the {@code String} that is concatenated to the end
     *                of this {@code String}.
     * @return  a string that represents the concatenation of this object's
     *          characters followed by the string argument's characters.
     */
    public String concat(String str) {
        if (str.isEmpty()) {
            return this;
        }
        if (M3_JOINED_STRINGS) {
            String joined = m3Concat(this, str);
            if (joined != null) {
                return joined;
            }
        }
        return StringConcatHelper.simpleConcat(this, str);
    }

    /**
     * Returns a string resulting from replacing all occurrences of
     * {@code oldChar} in this string with {@code newChar}.
     * <p>
     * If the character {@code oldChar} does not occur in the
     * character sequence represented by this {@code String} object,
     * then a reference to this {@code String} object is returned.
     * Otherwise, a {@code String} object is returned that
     * represents a character sequence identical to the character sequence
     * represented by this {@code String} object, except that every
     * occurrence of {@code oldChar} is replaced by an occurrence
     * of {@code newChar}.
     * <p>
     * Examples:
     * <blockquote><pre>
     * "mesquite in your cellar".replace('e', 'o')
     *         returns "mosquito in your collar"
     * "the war of baronets".replace('r', 'y')
     *         returns "the way of bayonets"
     * "sparring with a purple porpoise".replace('p', 't')
     *         returns "starring with a turtle tortoise"
     * "JonL".replace('q', 'x') returns "JonL" (no change)
     * </pre></blockquote>
     *
     * @param   oldChar   the old character.
     * @param   newChar   the new character.
     * @return  a string derived from this string by replacing every
     *          occurrence of {@code oldChar} with {@code newChar}.
     */
    public String replace(char oldChar, char newChar) {
        if (oldChar != newChar) {
            M3String storage = m3();
            if (storage != null) {
                M3String replaced = storage.replace(oldChar, newChar);
                return replaced == storage ? this : new String(replaced);
            }
            byte[] currentValue = value();
            String ret = isLatin1() ? StringLatin1.replace(currentValue, oldChar, newChar)
                                    : StringUTF16.replace(currentValue, oldChar, newChar);
            if (ret != null) {
                return ret;
            }
        }
        return this;
    }

    /**
     * The compiled form of the regex argument of {@link #matches}, {@link #replaceFirst},
     * {@link #replaceAll} and {@link #split(String, int)}. A regex is compiled once, stored,
     * and read back by later calls with an equal regex, together with everything the
     * compilation derived. A {@code null} regex and a very long one go to
     * {@link Pattern#compile(String)} as before; so does an invalid one, on every call,
     * with its exception.
     */
    private static Pattern compiledRegex(String regex) {
        if (regex == null || regex.length() > RegexMemo.MAX_REGEX_UNITS) {
            return Pattern.compile(regex);
        }
        return RegexMemo.compiled(regex);
    }

    /** Recently compiled regex arguments: softly held, one per slot of a direct-mapped table. */
    private static final class RegexMemo {
        static final int MAX_REGEX_UNITS = 256;
        private static final int SLOTS = 256;
        private static final java.util.concurrent.atomic.AtomicReferenceArray<
                java.lang.ref.SoftReference<Pattern>> TABLE =
                new java.util.concurrent.atomic.AtomicReferenceArray<>(SLOTS);

        private RegexMemo() {}

        static Pattern compiled(String regex) {
            int slot = regex.hashCode() & (SLOTS - 1);
            java.lang.ref.SoftReference<Pattern> stored = TABLE.get(slot);
            Pattern known = stored == null ? null : stored.get();
            if (known != null && known.pattern().equals(regex)) return known;
            Pattern compiled = Pattern.compile(regex);
            TABLE.set(slot, new java.lang.ref.SoftReference<>(compiled));
            return compiled;
        }
    }

    /**
     * Tells whether or not this string matches the given <a
     * href="../util/regex/Pattern.html#sum">regular expression</a>.
     *
     * <p> An invocation of this method of the form
     * <i>str</i>{@code .matches(}<i>regex</i>{@code )} yields exactly the
     * same result as the expression
     *
     * <blockquote>
     * {@link java.util.regex.Pattern}.{@link java.util.regex.Pattern#matches(String,CharSequence)
     * matches(<i>regex</i>, <i>str</i>)}
     * </blockquote>
     *
     * @param   regex
     *          the regular expression to which this string is to be matched
     *
     * @return  {@code true} if, and only if, this string matches the
     *          given regular expression
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     */
    public boolean matches(String regex) {
        Objects.requireNonNull(regex);
        M3String storage = m3();
        // The one literal-regex owner (M3String.literalRegex, rule LITERAL_MATCHES): a literal
        // matches exactly the String equal to it; equals keeps its O(1) M3 gates.
        if (storage != null && M3String.isLiteralMatchesRegex(regex)) {
            return equals(regex);
        }
        if (storage != null) {
            int decision = M3StringRegexDp.tryMatch(storage, regex);
            if (decision != M3StringRegexDp.UNKNOWN) {
                return decision == M3StringRegexDp.MATCH;
            }
        }
        return compiledRegex(regex).asMatchPredicate().test(this);
    }

    /**
     * Returns the number of times the specified character occurs in this string.
     * <p>
     * The result is the number of indexes that successive calls of
     * {@link #indexOf(int, int)} find when each search starts after the previous
     * occurrence. A character in the range from 0 to 0xFFFF (inclusive) is counted
     * wherever a {@code char} of that value occurs, a surrogate value included. A
     * supplementary code point is counted wherever its surrogate pair occurs. A value
     * that is not a valid Unicode code point does not occur in any string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. It is the common
     * entry point for the counting operation that utility libraries provide.
     *
     * @param   ch   a character (Unicode code point).
     * @return  the number of occurrences of the character in this string, or
     *          {@code 0} if it does not occur.
     * @see     #indexOf(int, int)
     * @since   21
     */
    public int countMatches(int ch) {
        if (m3() == null && Character.isBmpCodePoint(ch)) {
            return isLatin1() ? countLatin1(value, (char) ch) : countUtf16(value, (char) ch);
        }
        return countByIndexOf(ch);
    }

    /** Occurrences found by {@code indexOf} hops: any storage and any code point. */
    private int countByIndexOf(int ch) {
        int step = Character.isSupplementaryCodePoint(ch) ? 2 : 1;
        int count = 0;
        for (int index = indexOf(ch); index >= 0; index = indexOf(ch, index + step)) {
            count++;
        }
        return count;
    }

    /** Units equal to {@code unit} in flat Latin-1 storage; a wider unit never occurs. */
    private static int countLatin1(byte[] value, char unit) {
        if (!StringLatin1.canEncode(unit)) return 0;
        byte wanted = (byte) unit;
        int count = 0;
        for (byte stored : value) {
            if (stored == wanted) count++;
        }
        return count;
    }

    /** Units equal to {@code unit} in flat UTF-16 storage. */
    private static int countUtf16(byte[] value, char unit) {
        int length = value.length >> UTF16;
        int count = 0;
        for (int index = 0; index < length; index++) {
            if (StringUTF16.getChar(value, index) == unit) count++;
        }
        return count;
    }

    /**
     * Returns the index at which this string and the specified string begin to differ.
     * <p>
     * The result is the smallest index {@code k} at which the two strings have different
     * {@code char} values, with one exception that keeps a surrogate pair whole: when the
     * {@code char} before {@code k} is a high surrogate and either string has a low surrogate
     * at {@code k}, the result is {@code k - 1}, the index of the high surrogate. When no
     * {@code char} differs within the shorter string, the result is the length of the shorter
     * string, or {@code -1} when the two strings have the same length and so the same content.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. It is the common
     * entry point for the first-difference operation that utility libraries provide.
     *
     * @param   other   the string to compare with this string.
     * @return  the index of the first difference, or {@code -1} if the strings are equal.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @see     #regionMatches(int, String, int, int)
     * @since   21
     */
    public int indexOfDifference(String other) {
        if (this == other) return -1;
        int length = length();
        int otherLength = other.length();
        int limit = Math.min(length, otherLength);
        boolean flatSameCoder = m3() == null && other.m3() == null && coder == other.coder;
        int same = flatSameCoder ? commonFlatUnits(value, other.value, limit, coder)
                : commonUnits(other, limit);
        if (same == limit) return length == otherLength ? -1 : limit;
        boolean insidePair = same > 0 && Character.isHighSurrogate(charAt(same - 1))
                && (Character.isLowSurrogate(charAt(same))
                        || Character.isLowSurrogate(other.charAt(same)));
        return insidePair ? same - 1 : same;
    }

    /** Leading units two flat arrays of one coder share, up to {@code limit} units. */
    private static int commonFlatUnits(byte[] left, byte[] right, int limit, byte coder) {
        int at = ArraysSupport.mismatch(left, right, limit << coder);
        return at < 0 ? limit : at >> coder;
    }

    /** Leading units this string and {@code other} share, up to {@code limit}: any storage. */
    private int commonUnits(String other, int limit) {
        int index = 0;
        while (index < limit && charAt(index) == other.charAt(index)) {
            index++;
        }
        return index;
    }

    /**
     * Returns {@code true} if this string contains any of the given characters. A high surrogate in
     * {@code chars} matches only where this string holds it followed by the character that follows
     * it in {@code chars}, or alone when it ends {@code chars}; a low surrogate that follows a high
     * surrogate in {@code chars} belongs to that pair and does not match alone.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API, the common entry point for the
     * character-set search that utility libraries provide.
     *
     * @param   chars   the characters to search for.
     * @return  {@code true} if any of the characters occurs; {@code false} if this string or
     *          {@code chars} is empty.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public boolean containsAny(char... chars) {
        return indexOfAny(chars) >= 0;
    }

    /**
     * Returns {@code true} if this string contains any of the characters of {@code chars}, with the
     * surrogate rule of {@link #containsAny(char...)}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   chars   the characters to search for, as a sequence.
     * @return  {@code true} if any of the characters occurs.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public boolean containsAny(CharSequence chars) {
        return indexOfAny(unitsOf(chars)) >= 0;
    }

    /**
     * Returns {@code true} if this string contains any of the given sequences. {@code null}
     * elements are skipped; an empty sequence is contained in any non-empty string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   searches   the sequences to search for.
     * @return  {@code true} if any sequence occurs; {@code false} if this string or
     *          {@code searches} is empty.
     * @throws  NullPointerException if {@code searches} is {@code null}.
     * @since   21
     */
    public boolean containsAny(CharSequence... searches) {
        if (searches.length == 0 || isEmpty()) return false;
        for (CharSequence search : searches) {
            if (search != null && indexOf(search.toString()) >= 0) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this string contains any of the given sequences, ignoring case as
     * {@link #regionMatches(boolean, int, String, int, int)} does. {@code null} elements are
     * skipped.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   searches   the sequences to search for.
     * @return  {@code true} if any sequence occurs; {@code false} if this string or
     *          {@code searches} is empty.
     * @throws  NullPointerException if {@code searches} is {@code null}.
     * @since   21
     */
    public boolean containsAnyIgnoreCase(CharSequence... searches) {
        if (searches.length == 0 || isEmpty()) return false;
        for (CharSequence search : searches) {
            if (search != null && containsIgnoreCase(search)) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this string contains {@code search}, ignoring case as
     * {@link #regionMatches(boolean, int, String, int, int)} does.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search   the sequence to search for.
     * @return  {@code true} if {@code search} occurs, ignoring case.
     * @throws  NullPointerException if {@code search} is {@code null}.
     * @since   21
     */
    public boolean containsIgnoreCase(CharSequence search) {
        return indexOfIgnoreCase(search, 0) >= 0;
    }

    /**
     * Returns {@code true} if this string contains none of the given characters: the negation of
     * {@link #containsAny(char...)}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   chars   the characters that must not occur.
     * @return  {@code true} if none of the characters occurs.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public boolean containsNone(char... chars) {
        return indexOfAny(chars) < 0;
    }

    /**
     * Returns {@code true} if this string contains none of the characters of {@code chars}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   chars   the characters that must not occur, as a string.
     * @return  {@code true} if none of the characters occurs.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public boolean containsNone(String chars) {
        return indexOfAny(chars.toCharArray()) < 0;
    }

    /**
     * Returns {@code true} if every character of this string is one of the given characters, read
     * as code points as {@link #indexOfAnyBut(char...)} does. An empty string contains only valid
     * characters; no character is valid when {@code chars} is empty.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   chars   the valid characters.
     * @return  {@code true} if this string is empty or holds only valid characters.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public boolean containsOnly(char... chars) {
        java.util.Objects.requireNonNull(chars, "chars");
        if (isEmpty()) return true;
        return chars.length > 0 && indexOfAnyBut(chars) < 0;
    }

    /**
     * Returns {@code true} if every character of this string is one of the characters of
     * {@code chars}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   chars   the valid characters, as a string.
     * @return  {@code true} if this string is empty or holds only valid characters.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public boolean containsOnly(String chars) {
        return containsOnly(chars.toCharArray());
    }

    /**
     * Returns {@code true} if this string contains a whitespace character, as
     * {@link Character#isWhitespace(char)} defines it.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if any character is whitespace.
     * @since   21
     */
    public boolean containsWhitespace() {
        int length = length();
        M3String storage = m3();
        if (storage != null) {
            M3StringFacts facts = storage.factsIfPrepared();
            if (facts != null && (facts.regexAnyClasses & M3StringFacts.REGEX_SPACE) != 0) {
                return true;
            }
            return windowContainsWhitespace(length);
        }
        if (isLatin1()) {
            byte[] bytes = value;
            for (int index = 0; index < length; index++) {
                if (Character.isWhitespace((char) (bytes[index] & 0xFF))) return true;
            }
            return false;
        }
        for (int index = 0; index < length; index++) {
            if (Character.isWhitespace(charAt(index))) return true;
        }
        return false;
    }

    /**
     * Returns the index of the first character of this string that is one of the given characters,
     * with the surrogate rule of {@link #containsAny(char...)}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   chars   the characters to search for.
     * @return  the index of the first match, or {@code -1} if there is none or this string or
     *          {@code chars} is empty.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public int indexOfAny(char... chars) {
        int length = length();
        if (chars.length == 0 || length == 0) return -1;
        long low = asciiMask(chars, 0);
        long high = asciiMask(chars, 64);
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts != null && noUnitMayOccur(facts, chars)) return -1;
        byte[] bytes = value;
        if (storage == null && isLatin1()) {
            long upper = asciiMask(chars, 128);
            long top = asciiMask(chars, 192);
            for (int index = 0; index < length; index++) {
                int unit = bytes[index] & 0xFF;
                if (unit < 128 ? inMask((char) unit, low, high)
                        : inMask((char) (unit - 128), upper, top)) {
                    return index;
                }
            }
            return -1;
        }
        boolean wide = hasWideUnit(chars);
        UnitSet set = wide && chars.length > UnitSet.FEW ? new UnitSet(chars, UnitSet.ALONE) : null;
        if (storage != null) return windowIndexOfAny(chars, low, high, wide, set);
        for (int index = 0; index < length; index++) {
            char unit = StringUTF16.getChar(bytes, index);
            if (unit < 128) {
                if (inMask(unit, low, high)) return index;
            } else if (wide) {
                int next = index + 1 < length ? StringUTF16.getChar(bytes, index + 1) : -1;
                if (set == null ? anyMatches(chars, unit, next) : set.matches(unit, next)) {
                    return index;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first character of this string that is one of the characters of
     * {@code chars}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   chars   the characters to search for, as a string.
     * @return  the index of the first match, or {@code -1}.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public int indexOfAny(String chars) {
        return indexOfAny(chars.toCharArray());
    }

    /**
     * Returns the smallest index at which any of the given sequences occurs in this string.
     * {@code null} elements are skipped; an empty sequence occurs at index 0.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   searches   the sequences to search for.
     * @return  the smallest index of an occurrence, or {@code -1}.
     * @throws  NullPointerException if {@code searches} is {@code null}.
     * @since   21
     */
    public int indexOfAny(CharSequence... searches) {
        int best = Integer.MAX_VALUE;
        for (CharSequence search : searches) {
            if (search == null) continue;
            int at = indexOf(search.toString());
            if (at >= 0 && at < best) best = at;
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    /**
     * Returns the index of the first character of this string that is not one of the given
     * characters. Both this string and {@code chars} are read as code points: a surrogate pair is
     * one character, a lone surrogate is a character of its own.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   chars   the characters to skip.
     * @return  the index of the first other character, or {@code -1} if there is none or this
     *          string or {@code chars} is empty.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public int indexOfAnyBut(char... chars) {
        int length = length();
        if (chars.length == 0 || length == 0) return -1;
        long low = asciiMask(chars, 0);
        long high = asciiMask(chars, 64);
        boolean flat = m3() == null;
        byte[] bytes = value;
        if (flat && isLatin1()) {
            long upper = asciiMask(chars, 128);
            long top = asciiMask(chars, 192);
            for (int index = 0; index < length; index++) {
                int point = bytes[index] & 0xFF;
                if (point < 128 ? !inMask((char) point, low, high)
                        : !inMask((char) (point - 128), upper, top)) {
                    return index;
                }
            }
            return -1;
        }
        boolean wide = hasWideUnit(chars);
        UnitSet set = wide && chars.length > UnitSet.FEW
                ? new UnitSet(chars, UnitSet.POINTS) : null;
        if (!flat) return windowIndexOfAnyBut(chars, low, high, wide, set);
        for (int index = 0; index < length; ) {
            int point = StringUTF16.codePointAt(bytes, index, length);
            boolean skipped = point < 128 ? inMask((char) point, low, high)
                    : wide && (set == null ? pointIn(chars, point) : set.has(point));
            if (!skipped) return index;
            index += Character.charCount(point);
        }
        return -1;
    }

    /**
     * Returns the index of the first character of this string that does not occur in
     * {@code chars}, read as code points as {@link #indexOfAnyBut(char...)} does.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   chars   the characters to skip, as a sequence.
     * @return  the index of the first other character, or {@code -1}.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public int indexOfAnyBut(CharSequence chars) {
        return indexOfAnyBut(unitsOf(chars));
    }

    /**
     * Returns the largest index at which any of the given sequences occurs in this string.
     * {@code null} elements are skipped; an empty sequence occurs at the end of this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   searches   the sequences to search for.
     * @return  the largest index of an occurrence, or {@code -1}.
     * @throws  NullPointerException if {@code searches} is {@code null}.
     * @since   21
     */
    public int lastIndexOfAny(CharSequence... searches) {
        int best = -1;
        for (CharSequence search : searches) {
            if (search == null) continue;
            int at = lastIndexOf(search.toString());
            if (at > best) best = at;
        }
        return best;
    }

    /**
     * Returns the index of the first occurrence of {@code search} in this string, ignoring case as
     * {@link #regionMatches(boolean, int, String, int, int)} does.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search   the sequence to search for.
     * @return  the index of the first occurrence, or {@code -1}.
     * @throws  NullPointerException if {@code search} is {@code null}.
     * @since   21
     */
    public int indexOfIgnoreCase(CharSequence search) {
        return indexOfIgnoreCase(search, 0);
    }

    /**
     * Returns the index of the first occurrence of {@code search} at or after {@code fromIndex},
     * ignoring case. A negative {@code fromIndex} counts as 0. An empty {@code search} occurs at
     * {@code fromIndex} when {@code fromIndex} is at most the length of this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search      the sequence to search for.
     * @param   fromIndex   the index to start at.
     * @return  the index of the first occurrence, or {@code -1}.
     * @throws  NullPointerException if {@code search} is {@code null}.
     * @since   21
     */
    public int indexOfIgnoreCase(CharSequence search, int fromIndex) {
        String target = search.toString();
        int count = target.length();
        int start = Math.max(fromIndex, 0);
        if (start > length() - count) return -1;
        if (count == 0) return start;
        int end = length() - count + 1;
        char first = target.charAt(0);
        boolean ascii = first < 128;
        if (ascii && noCaseCandidate(first)) return -1;
        int index = ascii ? nextCaseCandidate(start, end, 1, first) : start;
        while (index < end) {
            if (regionMatches(true, index, target, 0, count)) return index;
            index = ascii ? nextCaseCandidate(index + 1, end, 1, first) : index + 1;
        }
        return -1;
    }

    /**
     * Returns the index of the last occurrence of {@code search} in this string, ignoring case.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search   the sequence to search for.
     * @return  the index of the last occurrence, or {@code -1}.
     * @throws  NullPointerException if {@code search} is {@code null}.
     * @since   21
     */
    public int lastIndexOfIgnoreCase(CharSequence search) {
        return lastIndexOfIgnoreCase(search, length());
    }

    /**
     * Returns the index of the last occurrence of {@code search} that starts at or before
     * {@code fromIndex}, ignoring case. An empty {@code search} occurs at {@code fromIndex}, capped
     * at the length of this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search      the sequence to search for.
     * @param   fromIndex   the largest index to consider.
     * @return  the index of the last occurrence, or {@code -1}.
     * @throws  NullPointerException if {@code search} is {@code null}.
     * @since   21
     */
    public int lastIndexOfIgnoreCase(CharSequence search, int fromIndex) {
        String target = search.toString();
        int count = target.length();
        int start = Math.min(fromIndex, length() - count);
        if (start < 0) return -1;
        if (count == 0) return start;
        char first = target.charAt(0);
        boolean ascii = first < 128;
        if (ascii && noCaseCandidate(first)) return -1;
        int index = ascii ? nextCaseCandidate(start, -1, -1, first) : start;
        while (index >= 0) {
            if (regionMatches(true, index, target, 0, count)) return index;
            index = ascii ? nextCaseCandidate(index - 1, -1, -1, first) : index - 1;
        }
        return -1;
    }

    /**
     * The first index from {@code from} towards {@code stop} (exclusive), moving by {@code step},
     * whose unit can start an ignoring-case region match of the ASCII unit {@code first}, else
     * {@code stop}. An ASCII unit can only by ASCII case; any other unit is judged by
     * {@link #sameUnitIgnoreCase(char, char)}.
     */
    private int nextCaseCandidate(int from, int stop, int step, char first) {
        char fold = asciiLower(first);
        if (m3() != null) return windowCaseCandidate(from, stop, step, first, fold);
        if (isLatin1()) {
            byte[] bytes = value;
            for (int index = from; index != stop; index += step) {
                char unit = (char) (bytes[index] & 0xFF);
                if (unit < 128 ? asciiLower(unit) == fold : sameUnitIgnoreCase(unit, first)) {
                    return index;
                }
            }
            return stop;
        }
        for (int index = from; index != stop; index += step) {
            char unit = charAt(index);
            if (unit < 128 ? asciiLower(unit) == fold : sameUnitIgnoreCase(unit, first)) {
                return index;
            }
        }
        return stop;
    }

    /**
     * {@link #nextCaseCandidate(int, int, int, char)} over M3 storage: the units from
     * {@code from} towards {@code stop} are read in bulk windows in the direction of the scan.
     */
    private int windowCaseCandidate(int from, int stop, int step, char first, char fold) {
        char[] window = new char[Math.min(Math.abs(stop - from), SCAN_WINDOW)];
        for (int index = from; index != stop; ) {
            int count = Math.min(window.length, Math.abs(stop - index));
            int begin = step > 0 ? index : index - count + 1;
            getChars(begin, begin + count, window, 0);
            for (int k = 0; k < count; k++) {
                int at = step > 0 ? k : count - 1 - k;
                char unit = window[at];
                if (unit < 128 ? asciiLower(unit) == fold : sameUnitIgnoreCase(unit, first)) {
                    return begin + at;
                }
            }
            index += step * count;
        }
        return stop;
    }

    /**
     * Whether prepared M3 facts rule out every start of an ignoring-case match of the ASCII unit
     * {@code first}: in Latin-1 text only its ASCII lower- and upper-case forms can match it.
     */
    private boolean noCaseCandidate(char first) {
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts == null || !facts.latin1) return false;
        char lower = asciiLower(first);
        char upper = lower >= 'a' && lower <= 'z' ? (char) (lower - 'a' + 'A') : lower;
        return !facts.mayContainCodeUnit(lower) && !facts.mayContainCodeUnit(upper);
    }

    /** {@code unit} with an ASCII upper-case letter turned into lower case. */
    private static char asciiLower(char unit) {
        return unit >= 'A' && unit <= 'Z' ? (char) (unit - 'A' + 'a') : unit;
    }

    /**
     * Whether two units can start an ignoring-case region match: {@code false} only when
     * {@link #regionMatches(boolean, int, String, int, int)} with {@code ignoreCase} refuses them
     * whatever units follow. A surrogate always passes; its comparison belongs to the region.
     */
    private static boolean sameUnitIgnoreCase(char a, char b) {
        if (a == b || Character.isSurrogate(a) || Character.isSurrogate(b)) return true;
        char upperA = Character.toUpperCase(a);
        char upperB = Character.toUpperCase(b);
        return upperA == upperB || Character.toLowerCase(upperA) == Character.toLowerCase(upperB);
    }

    /** Whether prepared M3 facts rule out every unit of {@code units}. */
    private static boolean noUnitMayOccur(M3StringFacts facts, char[] units) {
        for (char unit : units) {
            if (facts.mayContainCodeUnit(unit)) return false;
        }
        return true;
    }

    /** {@link #indexOfAny(char...)} over M3 storage, read in bulk windows. */
    private int windowIndexOfAny(char[] chars, long low, long high, boolean wide, UnitSet set) {
        int length = length();
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; from += window.length) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            for (int at = 0; at < to - from; at++) {
                char unit = window[at];
                if (unit < 128) {
                    if (inMask(unit, low, high)) return from + at;
                } else if (wide) {
                    int after = from + at + 1;
                    int next = after == length ? -1 : after < to ? window[at + 1] : charAt(after);
                    if (set == null ? anyMatches(chars, unit, next) : set.matches(unit, next)) {
                        return from + at;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * {@link #indexOfAnyBut(char...)} over M3 storage: code points read in bulk windows, a
     * surrogate pair never split between two windows.
     */
    private int windowIndexOfAnyBut(char[] chars, long low, long high, boolean wide, UnitSet set) {
        int length = length();
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; ) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            int count = to - from;
            if (to < length && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;
            for (int at = 0; at < count; ) {
                int point = Character.codePointAt(window, at, count);
                boolean skipped = point < 128 ? inMask((char) point, low, high)
                        : wide && (set == null ? pointIn(chars, point) : set.has(point));
                if (!skipped) return from + at;
                at += Character.charCount(point);
            }
            from += count;
        }
        return -1;
    }

    /** {@link #containsWhitespace()} over M3 storage, read in bulk windows. */
    private boolean windowContainsWhitespace(int length) {
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; from += window.length) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            for (int at = 0; at < to - from; at++) {
                if (Character.isWhitespace(window[at])) return true;
            }
        }
        return false;
    }

    /**
     * Returns the index of the {@code ordinal}-th occurrence of {@code search} in this string,
     * counting occurrences that overlap. An empty {@code search} gives 0.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search    the sequence to search for.
     * @param   ordinal   which occurrence, from 1.
     * @return  the index of that occurrence, or {@code -1} if there are fewer or {@code ordinal} is
     *          not positive.
     * @throws  NullPointerException if {@code search} is {@code null}.
     * @since   21
     */
    public int ordinalIndexOf(CharSequence search, int ordinal) {
        String target = search.toString();
        if (ordinal <= 0) return -1;
        if (target.isEmpty()) return 0;
        int index = -1;
        for (int found = 0; found < ordinal; found++) {
            index = indexOf(target, index + 1);
            if (index < 0) return -1;
        }
        return index;
    }

    /**
     * Returns the index of the {@code ordinal}-th occurrence of {@code search} in this string,
     * counting from the end and counting occurrences that overlap. An empty {@code search} gives
     * the length of this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search    the sequence to search for.
     * @param   ordinal   which occurrence from the end, from 1.
     * @return  the index of that occurrence, or {@code -1}.
     * @throws  NullPointerException if {@code search} is {@code null}.
     * @since   21
     */
    public int lastOrdinalIndexOf(CharSequence search, int ordinal) {
        String target = search.toString();
        if (ordinal <= 0) return -1;
        if (target.isEmpty()) return length();
        int index = length();
        for (int found = 0; found < ordinal; found++) {
            index = lastIndexOf(target, index - 1);
            if (index < 0) return -1;
        }
        return index;
    }

    /**
     * Returns {@code true} if this string starts with any of the given prefixes. {@code null}
     * elements are skipped; an empty string starts with none.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   prefixes   the prefixes.
     * @return  {@code true} if any prefix matches.
     * @throws  NullPointerException if {@code prefixes} is {@code null}.
     * @since   21
     */
    public boolean startsWithAny(CharSequence... prefixes) {
        if (prefixes.length == 0 || isEmpty()) return false;
        for (CharSequence prefix : prefixes) {
            if (prefix != null && startsWith(prefix.toString())) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this string ends with any of the given suffixes. {@code null}
     * elements are skipped; an empty string ends with none.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   suffixes   the suffixes.
     * @return  {@code true} if any suffix matches.
     * @throws  NullPointerException if {@code suffixes} is {@code null}.
     * @since   21
     */
    public boolean endsWithAny(CharSequence... suffixes) {
        if (suffixes.length == 0 || isEmpty()) return false;
        for (CharSequence suffix : suffixes) {
            if (suffix != null && endsWith(suffix.toString())) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this string starts with {@code prefix}, ignoring case as
     * {@link #regionMatches(boolean, int, String, int, int)} does.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   prefix   the prefix.
     * @return  {@code true} if this string starts with {@code prefix}, ignoring case.
     * @throws  NullPointerException if {@code prefix} is {@code null}.
     * @since   21
     */
    public boolean startsWithIgnoreCase(CharSequence prefix) {
        String target = prefix.toString();
        return target.length() <= length() && regionMatches(true, 0, target, 0, target.length());
    }

    /**
     * Returns {@code true} if this string ends with {@code suffix}, ignoring case as
     * {@link #regionMatches(boolean, int, String, int, int)} does.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   suffix   the suffix.
     * @return  {@code true} if this string ends with {@code suffix}, ignoring case.
     * @throws  NullPointerException if {@code suffix} is {@code null}.
     * @since   21
     */
    public boolean endsWithIgnoreCase(CharSequence suffix) {
        String target = suffix.toString();
        int count = target.length();
        return count <= length() && regionMatches(true, length() - count, target, 0, count);
    }

    /**
     * Returns {@code true} if every given word occurs in this string as a whole word: between word
     * boundaries in the sense of {@code \b} of {@link java.util.regex.Pattern}, on one line. An
     * empty string, no words, or a blank word gives {@code false}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   words   the words.
     * @return  {@code true} if every word occurs as a whole word.
     * @throws  NullPointerException if {@code words} or one of its elements is {@code null}.
     * @since   21
     */
    public boolean containsAllWords(CharSequence... words) {
        if (isEmpty() || words.length == 0) return false;
        for (CharSequence word : words) {
            if (word.toString().isBlank()) return false;
            String regex = ".*\\b" + java.util.regex.Pattern.quote(word.toString()) + "\\b.*";
            if (!java.util.regex.Pattern.compile(regex).matcher(this).matches()) return false;
        }
        return true;
    }

    /** The code units of {@code chars} as an array, for the character-set operations. */
    private static char[] unitsOf(CharSequence chars) {
        int count = chars.length();
        char[] units = new char[count];
        for (int index = 0; index < count; index++) {
            units[index] = chars.charAt(index);
        }
        return units;
    }

    /** Bits {@code base .. base + 63} of the set of units below 256 in {@code chars}. */
    private static long asciiMask(char[] chars, int base) {
        long mask = 0L;
        for (char unit : chars) {
            if (unit >= base && unit < base + 64) mask |= 1L << (unit - base);
        }
        return mask;
    }

    /** Whether the ASCII unit {@code unit} is in the set given by its two mask words. */
    private static boolean inMask(char unit, long low, long high) {
        return unit < 64 ? (low & (1L << unit)) != 0 : (high & (1L << (unit - 64))) != 0;
    }

    /** Whether {@code chars} holds a unit outside ASCII. */
    private static boolean hasWideUnit(char[] chars) {
        for (char unit : chars) {
            if (unit >= 128) return true;
        }
        return false;
    }

    /**
     * Whether the non-ASCII unit {@code unit}, followed in the string by the unit {@code next}
     * ({@code -1} at its end), matches {@code chars} for indexOfAny: a unit that is not a high
     * surrogate matches when it is in {@code chars}, except a low surrogate that follows a high
     * surrogate there; a high surrogate matches at a position of {@code chars} that ends the
     * array or is followed there by {@code next}. A linear scan, for a set of at most
     * {@link UnitSet#FEW} units.
     */
    private static boolean anyMatches(char[] chars, char unit, int next) {
        boolean pair = Character.isHighSurrogate(unit);
        int last = chars.length - 1;
        for (int j = 0; j <= last; j++) {
            char candidate = chars[j];
            if (candidate == unit && (!pair || j == last || chars[j + 1] == next)) return true;
            if (j < last && Character.isHighSurrogate(candidate)
                    && Character.isLowSurrogate(chars[j + 1])) {
                j++;   // the low surrogate of a pair in chars does not match alone
            }
        }
        return false;
    }

    /**
     * Whether {@code point} is one of the code points of {@code chars}, a surrogate pair read as
     * one. A linear scan, for a set of at most {@link UnitSet#FEW} units.
     */
    private static boolean pointIn(char[] chars, int point) {
        for (int i = 0; i < chars.length; ) {
            int candidate = Character.codePointAt(chars, i);
            if (candidate == point) return true;
            i += Character.charCount(candidate);
        }
        return false;
    }

    /**
     * A character set of the search methods, built once per call so that a unit or code point
     * is looked up in constant time below 256 and above it in constant time or by halving: a bit
     * mask of the values below 256, and of the others a bit mask over their range when that range
     * spans at most 64 times their number of values (or 512), else their sorted array.
     * {@link #UNITS} holds every unit of the array and each surrogate pair in it also as its code
     * point, the characters {@link String#indexOf(int)} finds there; {@link #POINTS} holds its
     * code points, a surrogate pair as one; {@link #ALONE} reads it as
     * {@link String#indexOfAny(char...)} does: a low surrogate that follows a high surrogate
     * belongs to that pair, and a high surrogate is kept apart, matching where the array ends
     * with it or where the string holds it followed by the unit that follows it in the array.
     */
    private static final class UnitSet {
        /** Every unit, and every surrogate pair also as its code point. */
        static final int UNITS = 0;
        /** The code points, a surrogate pair as one. */
        static final int POINTS = 1;
        /** The units as indexOfAny matches them. */
        static final int ALONE = 2;
        /** The largest set the search methods match by a linear scan, without building one. */
        static final int FEW = 16;

        private static final int[] NONE = {};

        private final long below64;
        private final long below128;
        private final long below192;
        private final long below256;
        private final int[] values;
        private final int count;
        private final int base;
        private final long[] dense;
        private final int[] pairs;
        private final int pairCount;
        private final int lastHigh;

        UnitSet(char[] chars, int read) {
            below64 = asciiMask(chars, 0);
            below128 = asciiMask(chars, 64);
            below192 = asciiMask(chars, 128);
            below256 = asciiMask(chars, 192);
            int last = chars.length - 1;
            int[] found = NONE;
            int[] starts = NONE;
            int n = 0;
            int p = 0;
            int high = -1;
            int least = Integer.MAX_VALUE;
            int most = 0;
            for (int i = 0; i <= last; i++) {
                char unit = chars[i];
                if (unit < 256) continue;
                if (found == NONE) found = new int[2 * (last + 1 - i)];
                boolean pair = i < last && Character.isHighSurrogate(unit)
                        && Character.isLowSurrogate(chars[i + 1]);
                if (read == ALONE && Character.isHighSurrogate(unit)) {
                    if (i == last) {
                        high = unit;
                    } else {
                        if (starts == NONE) starts = new int[last - i];
                        starts[p++] = unit << 16 | chars[i + 1];
                    }
                } else if (read != POINTS || !pair) {
                    found[n++] = unit;
                }
                if (pair && read != ALONE) found[n++] = Character.toCodePoint(unit, chars[i + 1]);
                if (pair && read != UNITS) i++;   // the low surrogate belongs to the pair
            }
            for (int i = 0; i < n; i++) {
                least = Math.min(least, found[i]);
                most = Math.max(most, found[i]);
            }
            long[] bits = null;
            if (n > 0 && (most - least) >>> 6 < Math.max(8, n)) {
                bits = new long[((most - least) >>> 6) + 1];
                for (int i = 0; i < n; i++) {
                    int offset = found[i] - least;
                    bits[offset >>> 6] |= 1L << offset;
                }
            } else if (n > 1) {
                java.util.Arrays.sort(found, 0, n);
            }
            if (p > 1) java.util.Arrays.sort(starts, 0, p);
            values = found;
            count = n;
            base = least;
            dense = bits;
            pairs = starts;
            pairCount = p;
            lastHigh = high;
        }

        /** Whether {@code value}, a unit or a code point as the set reads them, is in the set. */
        boolean has(int value) {
            if (value < 256) {
                long word = value < 128 ? (value < 64 ? below64 : below128)
                        : value < 192 ? below192 : below256;
                return (word & 1L << value) != 0;
            }
            if (dense != null) {
                int offset = value - base;
                return offset >= 0 && offset >>> 6 < dense.length
                        && (dense[offset >>> 6] & 1L << offset) != 0;
            }
            return count > 0 && java.util.Arrays.binarySearch(values, 0, count, value) >= 0;
        }

        /**
         * Whether the unit {@code unit}, followed in the string by the unit {@code next}
         * ({@code -1} at its end), matches an {@link #ALONE} set.
         */
        boolean matches(char unit, int next) {
            if (!Character.isHighSurrogate(unit)) return has(unit);
            return unit == lastHigh || next >= 0 && pairCount > 0
                    && java.util.Arrays.binarySearch(pairs, 0, pairCount, unit << 16 | next) >= 0;
        }
    }

    /**
     * Returns this string without one line terminator at its end: {@code "\r\n"}, {@code "\n"} or
     * {@code "\r"}. A string with no terminator at its end is returned unchanged.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string without its final line terminator.
     * @since   21
     */
    public String chomp() {
        int length = length();
        if (length == 0) return this;
        char last = charAt(length - 1);
        if (last == '\n') {
            return substring(0, length > 1 && charAt(length - 2) == '\r' ? length - 2 : length - 1);
        }
        return last == '\r' ? substring(0, length - 1) : this;
    }

    /**
     * Returns this string without its last character; a final {@code "\r\n"} is removed as one,
     * and so is a final surrogate pair.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string without its last character, or the empty string.
     * @since   21
     */
    public String chop() {
        int length = length();
        if (length < 2) return "";
        boolean pair = Character.isLowSurrogate(charAt(length - 1))
                && Character.isHighSurrogate(charAt(length - 2));
        boolean crlf = charAt(length - 1) == '\n' && charAt(length - 2) == '\r';
        return substring(0, pair || crlf ? length - 2 : length - 1);
    }

    /**
     * Returns this string without its whitespace characters, as
     * {@link Character#isWhitespace(char)} defines them.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string without whitespace.
     * @since   21
     */
    public String deleteWhitespace() {
        int length = length();
        int first = 0;
        while (first < length && !Character.isWhitespace(charAt(first))) {
            first++;
        }
        if (first == length) return this;
        StringBuilder kept = new StringBuilder(length - 1).append(this, 0, first);
        for (int index = first + 1; index < length; index++) {
            char unit = charAt(index);
            if (!Character.isWhitespace(unit)) kept.append(unit);
        }
        return kept.toString();
    }

    /**
     * Returns this string with its whitespace normalized: each run of whitespace characters (as
     * {@link Character#isWhitespace(char)} defines them) becomes one space, a no-break space
     * ({@code U+00A0}) becomes a space, and the result is {@linkplain #trim() trimmed}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string with normalized whitespace.
     * @since   21
     */
    public String normalizeSpace() {
        int length = length();
        if (spaceNormalized(length)) return this;
        StringBuilder out = new StringBuilder(length);
        boolean leading = true;
        boolean pending = false;
        for (int index = 0; index < length; index++) {
            char unit = charAt(index);
            if (Character.isWhitespace(unit)) {
                pending = !leading;
            } else {
                if (pending) out.append(' ');
                out.append(unit == (char) 0x00A0 ? ' ' : unit);
                leading = false;
                pending = false;
            }
        }
        return leading ? "" : out.toString().trim();
    }

    /**
     * Whether normalizeSpace leaves this string as it is: no no-break space, whitespace only as
     * single spaces between other characters, and no character at most U+0020 at either end.
     */
    private boolean spaceNormalized(int length) {
        if (length == 0) return true;
        if (charAt(0) <= ' ' || charAt(length - 1) <= ' ') return false;
        M3String storage = m3();
        if (storage != null) {
            M3StringFacts facts = storage.factsIfPrepared();
            if (facts != null && (facts.stripStart != 0 || facts.stripEnd != length)) return false;
            return windowSpaceNormalized(length);
        }
        int previous = 1;
        for (int index = 0; index < length && previous >= 0; index++) {
            previous = spaceStep(charAt(index), previous);
        }
        return previous == 0;
    }

    /** {@link #spaceNormalized(int)} over M3 storage, read in bulk windows. */
    private boolean windowSpaceNormalized(int length) {
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        int previous = 1;
        for (int from = 0; from < length && previous >= 0; from += window.length) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            for (int at = 0; at < to - from && previous >= 0; at++) {
                previous = spaceStep(window[at], previous);
            }
        }
        return previous == 0;
    }

    /**
     * One unit of {@link #spaceNormalized(int)}: {@code 1} after a space, {@code 0} after another
     * unit, {@code -1} when the string is not normalized (a no-break space, other whitespace, or
     * a space after whitespace). {@code previous} is {@code 1} at the start.
     */
    private static int spaceStep(char unit, int previous) {
        if (unit == (char) 0x00A0) return -1;
        boolean space = Character.isWhitespace(unit);
        if (space && (unit != ' ' || previous == 1)) return -1;
        return space ? 1 : 0;
    }

    /**
     * Returns this string without diacritical marks: the compatibility decomposition
     * ({@link java.text.Normalizer.Form#NFKD}) without the combining diacritical marks of the block
     * {@code U+0300..U+036F}, composed again ({@link java.text.Normalizer.Form#NFC}), with the
     * letters that carry a stroke or bar mapped to their base letter: {@code U+0110 U+0111 U+0141
     * U+0142 U+0166 U+0167 U+0197 U+0244 U+0268 U+0289 U+1D7B U+1D7E} become
     * {@code D d L l T t I U i u I U}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string without diacritical marks.
     * @since   21
     */
    public String stripAccents() {
        int length = length();
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts != null && facts.ascii) return this;
        int index = 0;
        while (index < length && charAt(index) < 0x80) {
            index++;
        }
        if (index == length) return this;
        if (facts != null ? facts.latin1 : latin1Units(index, length)) {
            String[] table = Latin1Accents.TABLE;
            StringBuilder out = new StringBuilder(length).append(this, 0, index);
            if (storage == null) {
                for (int at = index; at < length; at++) {
                    out.append(table[charAt(at)]);
                }
                return out.toString();
            }
            char[] window = new char[Math.min(length - index, SCAN_WINDOW)];
            for (int from = index; from < length; from += window.length) {
                int to = Math.min(length, from + window.length);
                getChars(from, to, window, 0);
                for (int at = 0; at < to - from; at++) {
                    out.append(table[window[at]]);
                }
            }
            return out.toString();
        }
        return stripAccentsFull();
    }

    /** Whether every unit from {@code from} up to {@code length} is at most U+00FF. */
    private boolean latin1Units(int from, int length) {
        if (m3() == null) {
            if (isLatin1()) return true;
            for (int index = from; index < length; index++) {
                if (charAt(index) > 0xFF) return false;
            }
            return true;
        }
        char[] window = new char[Math.min(length - from, SCAN_WINDOW)];
        for (int start = from; start < length; start += window.length) {
            int to = Math.min(length, start + window.length);
            getChars(start, to, window, 0);
            for (int at = 0; at < to - start; at++) {
                if (window[at] > 0xFF) return false;
            }
        }
        return true;
    }

    /**
     * stripAccents of each Latin-1 unit alone, built once from the full algorithm. A string of
     * Latin-1 units decomposes unit by unit into base characters and marks of U+0300..U+036F, and
     * no two of the remaining base characters compose, so its result is the units' results joined.
     */
    private static final class Latin1Accents {
        static final String[] TABLE = new String[256];
        static {
            for (int unit = 0; unit < 256; unit++) {
                TABLE[unit] = String.valueOf((char) unit).stripAccentsFull();
            }
        }
    }

    /** stripAccents of any string: NFKD, marks U+0300..U+036F removed, NFC, stroke letters. */
    private String stripAccentsFull() {
        String decomposed = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFKD);
        StringBuilder kept = new StringBuilder(decomposed.length());
        for (int at = 0; at < decomposed.length(); at++) {
            char unit = decomposed.charAt(at);
            if (unit < 0x0300 || unit > 0x036F) kept.append(unit);
        }
        String composed = java.text.Normalizer.normalize(kept, java.text.Normalizer.Form.NFC);
        StringBuilder out = new StringBuilder(composed.length());
        for (int at = 0; at < composed.length(); at++) {
            out.append(baseOfStroked(composed.charAt(at)));
        }
        return out.toString();
    }

    /** The base letter of a letter with stroke or bar that stripAccents maps, else {@code unit}. */
    private static char baseOfStroked(char unit) {
        return switch (unit) {
            case (char) 0x0110 -> 'D';
            case (char) 0x0111 -> 'd';
            case (char) 0x0141 -> 'L';
            case (char) 0x0142 -> 'l';
            case (char) 0x0166 -> 'T';
            case (char) 0x0167 -> 't';
            case (char) 0x0197, (char) 0x1D7B -> 'I';
            case (char) 0x0244, (char) 0x1D7E -> 'U';
            case (char) 0x0268 -> 'i';
            case (char) 0x0289 -> 'u';
            default -> unit;
        };
    }

    /**
     * Returns this string without the given characters at its start and its end.
     * This string is read as code points, and a code point is removed when {@code chars}
     * contains it as {@link #indexOf(int)} finds it: a surrogate pair goes only when
     * {@code chars} holds that pair.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; {@link #strip()} removes
     * whitespace.
     *
     * @param   chars   the characters to remove.
     * @return  this string without those characters at either end.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public String strip(String chars) {
        return stripStart(chars).stripEnd(chars);
    }

    /**
     * Returns this string without the given characters at its start.
     * This string is read as code points, and a code point is removed when {@code chars}
     * contains it as {@link #indexOf(int)} finds it: a surrogate pair goes only when
     * {@code chars} holds that pair.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; {@link #stripLeading()} removes
     * whitespace.
     *
     * @param   chars   the characters to remove.
     * @return  this string without those characters at its start.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public String stripStart(String chars) {
        int length = length();
        int start = 0;
        if (!chars.isEmpty()) {
            while (start < length) {
                int point = codePointAt(start);
                if (chars.indexOf(point) < 0) break;
                start += Character.charCount(point);
            }
        }
        return substring(start);
    }

    /**
     * Returns this string without the given characters at its end.
     * This string is read as code points, and a code point is removed when {@code chars}
     * contains it as {@link #indexOf(int)} finds it: a surrogate pair goes only when
     * {@code chars} holds that pair.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; {@link #stripTrailing()} removes
     * whitespace.
     *
     * @param   chars   the characters to remove.
     * @return  this string without those characters at its end.
     * @throws  NullPointerException if {@code chars} is {@code null}.
     * @since   21
     */
    public String stripEnd(String chars) {
        int end = length();
        if (!chars.isEmpty()) {
            while (end > 0) {
                int point = codePointBefore(end);
                if (chars.indexOf(point) < 0) break;
                end -= Character.charCount(point);
            }
        }
        return substring(0, end);
    }

    /**
     * Returns {@code true} if this string is not {@linkplain #isBlank() blank}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if this string holds a character that is not whitespace.
     * @since   21
     */
    public boolean isNotBlank() {
        return !isBlank();
    }

    /**
     * Returns {@code true} if this string is not {@linkplain #isEmpty() empty}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if {@link #length()} is greater than 0.
     * @since   21
     */
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * Returns {@code true} if {@code s} is {@code null} or empty.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   s   the string, or {@code null}.
     * @return  {@code true} if {@code s} is {@code null} or empty.
     * @since   21
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Returns {@code true} if any of the given sequences is blank: {@code null}, empty, or only
     * whitespace as {@link Character#isWhitespace(char)} defines it. A {@code null} or empty array
     * gives {@code false}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   sequences   the sequences, or {@code null}.
     * @return  {@code true} if any sequence is blank.
     * @since   21
     */
    public static boolean isAnyBlank(CharSequence... sequences) {
        if (sequences == null) return false;
        for (CharSequence sequence : sequences) {
            if (blank(sequence)) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if every given sequence is blank. A {@code null} or empty array gives
     * {@code true}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   sequences   the sequences, or {@code null}.
     * @return  {@code true} if every sequence is blank.
     * @since   21
     */
    public static boolean isAllBlank(CharSequence... sequences) {
        if (sequences == null) return true;
        for (CharSequence sequence : sequences) {
            if (!blank(sequence)) return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if none of the given sequences is blank: the negation of
     * {@link #isAnyBlank(CharSequence...)}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   sequences   the sequences, or {@code null}.
     * @return  {@code true} if no sequence is blank.
     * @since   21
     */
    public static boolean isNoneBlank(CharSequence... sequences) {
        return !isAnyBlank(sequences);
    }

    /**
     * Returns {@code true} if any of the given sequences is {@code null} or empty. A {@code null}
     * or empty array gives {@code false}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   sequences   the sequences, or {@code null}.
     * @return  {@code true} if any sequence is empty.
     * @since   21
     */
    public static boolean isAnyEmpty(CharSequence... sequences) {
        if (sequences == null) return false;
        for (CharSequence sequence : sequences) {
            if (sequence == null || sequence.length() == 0) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if every given sequence is {@code null} or empty. A {@code null} or
     * empty array gives {@code true}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   sequences   the sequences, or {@code null}.
     * @return  {@code true} if every sequence is empty.
     * @since   21
     */
    public static boolean isAllEmpty(CharSequence... sequences) {
        if (sequences == null) return true;
        for (CharSequence sequence : sequences) {
            if (sequence != null && sequence.length() > 0) return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if none of the given sequences is {@code null} or empty: the negation of
     * {@link #isAnyEmpty(CharSequence...)}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   sequences   the sequences, or {@code null}.
     * @return  {@code true} if no sequence is empty.
     * @since   21
     */
    public static boolean isNoneEmpty(CharSequence... sequences) {
        return !isAnyEmpty(sequences);
    }

    /**
     * Returns {@code value}, or {@code defaultValue} if {@code value} is blank.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   <T>            the type of the sequences.
     * @param   value          the sequence, or {@code null}.
     * @param   defaultValue   the sequence for a blank {@code value}, or {@code null}.
     * @return  {@code value} unless it is blank, else {@code defaultValue}.
     * @since   21
     */
    public static <T extends CharSequence> T defaultIfBlank(T value, T defaultValue) {
        return blank(value) ? defaultValue : value;
    }

    /**
     * Returns {@code value}, or {@code defaultValue} if {@code value} is {@code null} or empty.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   <T>            the type of the sequences.
     * @param   value          the sequence, or {@code null}.
     * @param   defaultValue   the sequence for an empty {@code value}, or {@code null}.
     * @return  {@code value} unless it is empty, else {@code defaultValue}.
     * @since   21
     */
    public static <T extends CharSequence> T defaultIfEmpty(T value, T defaultValue) {
        return value == null || value.length() == 0 ? defaultValue : value;
    }

    /**
     * Returns {@code value}, or the empty string if {@code value} is {@code null}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   value   the string, or {@code null}.
     * @return  {@code value}, or {@code ""}.
     * @since   21
     */
    public static String defaultString(String value) {
        return value == null ? "" : value;
    }

    /**
     * Returns {@code value}, or {@code nullDefault} if {@code value} is {@code null}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   value         the string, or {@code null}.
     * @param   nullDefault   the string for a {@code null} {@code value}.
     * @return  {@code value}, or {@code nullDefault}.
     * @since   21
     */
    public static String defaultString(String value, String nullDefault) {
        return value == null ? nullDefault : value;
    }

    /**
     * Returns the first of the given values that is not blank, or {@code null} if there is none or
     * the array is {@code null}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   <T>      the type of the values.
     * @param   values   the values, or {@code null}.
     * @return  the first value that is not blank, or {@code null}.
     * @since   21
     */
    @SafeVarargs
    public static <T extends CharSequence> T firstNonBlank(T... values) {
        if (values == null) return null;
        for (T value : values) {
            if (!blank(value)) return value;
        }
        return null;
    }

    /**
     * Returns the first of the given values that is neither {@code null} nor empty, or
     * {@code null} if there is none or the array is {@code null}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   <T>      the type of the values.
     * @param   values   the values, or {@code null}.
     * @return  the first value that is not empty, or {@code null}.
     * @since   21
     */
    @SafeVarargs
    public static <T extends CharSequence> T firstNonEmpty(T... values) {
        if (values == null) return null;
        for (T value : values) {
            if (value != null && value.length() > 0) return value;
        }
        return null;
    }

    /**
     * Returns {@code value}, or the value of {@code defaultSupplier} if {@code value} is blank;
     * {@code null} if then {@code defaultSupplier} is {@code null}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   <T>               the type of the values.
     * @param   value             the sequence, or {@code null}.
     * @param   defaultSupplier   supplies the value for a blank {@code value}, or {@code null}.
     * @return  {@code value} unless it is blank, else the supplied value.
     * @since   21
     */
    public static <T extends CharSequence> T getIfBlank(T value,
            java.util.function.Supplier<T> defaultSupplier) {
        if (!blank(value)) return value;
        return defaultSupplier == null ? null : defaultSupplier.get();
    }

    /**
     * Returns {@code value}, or the value of {@code defaultSupplier} if {@code value} is
     * {@code null} or empty; {@code null} if then {@code defaultSupplier} is {@code null}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   <T>               the type of the values.
     * @param   value             the sequence, or {@code null}.
     * @param   defaultSupplier   supplies the value for an empty {@code value}, or {@code null}.
     * @return  {@code value} unless it is empty, else the supplied value.
     * @since   21
     */
    public static <T extends CharSequence> T getIfEmpty(T value,
            java.util.function.Supplier<T> defaultSupplier) {
        if (value != null && value.length() > 0) return value;
        return defaultSupplier == null ? null : defaultSupplier.get();
    }

    /**
     * Returns a new array of the given strings {@linkplain #strip() stripped} of whitespace;
     * {@code null} elements stay {@code null}. A {@code null} array gives {@code null}, an empty
     * array is returned as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   strings   the strings, or {@code null}.
     * @return  the stripped strings.
     * @since   21
     */
    public static String[] stripAll(String... strings) {
        return stripAll(strings, null);
    }

    /**
     * Returns a new array of the given strings without the given characters at either end, or
     * without whitespace when {@code chars} is {@code null}; {@code null} elements stay
     * {@code null}. A {@code null} array gives {@code null}, an empty array is returned as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   strings   the strings, or {@code null}.
     * @param   chars     the characters to remove, or {@code null} for whitespace.
     * @return  the stripped strings.
     * @since   21
     */
    public static String[] stripAll(String[] strings, String chars) {
        if (strings == null || strings.length == 0) return strings;
        String[] out = new String[strings.length];
        for (int index = 0; index < strings.length; index++) {
            String value = strings[index];
            out[index] = value == null ? null : chars == null ? stripWhitespace(value)
                    : value.strip(chars);
        }
        return out;
    }

    /**
     * Returns {@code value} {@linkplain #strip() stripped} of whitespace, or the empty string if
     * {@code value} is {@code null}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   value   the string, or {@code null}.
     * @return  the stripped string, never {@code null}.
     * @since   21
     */
    public static String stripToEmpty(String value) {
        return value == null ? "" : stripWhitespace(value);
    }

    /**
     * Returns {@code value} stripped of whitespace, or {@code null} if that leaves it empty or
     * {@code value} is {@code null}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   value   the string, or {@code null}.
     * @return  the stripped string, or {@code null}.
     * @since   21
     */
    public static String stripToNull(String value) {
        if (value == null) return null;
        String stripped = stripWhitespace(value);
        return stripped.isEmpty() ? null : stripped;
    }

    /**
     * Returns {@code value} {@linkplain #trim() trimmed}, or the empty string if {@code value} is
     * {@code null}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   value   the string, or {@code null}.
     * @return  the trimmed string, never {@code null}.
     * @since   21
     */
    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Returns {@code value} {@linkplain #trim() trimmed}, or {@code null} if that leaves it empty
     * or {@code value} is {@code null}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   value   the string, or {@code null}.
     * @return  the trimmed string, or {@code null}.
     * @since   21
     */
    public static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Whether {@code sequence} is {@code null}, empty, or only whitespace by code unit. */
    private static boolean blank(CharSequence sequence) {
        if (sequence == null) return true;
        int length = sequence.length();
        for (int index = 0; index < length; index++) {
            if (!Character.isWhitespace(sequence.charAt(index))) return false;
        }
        return true;
    }

    /** {@code value} without whitespace (by code unit) at either end. */
    private static String stripWhitespace(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && Character.isWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    /**
     * Returns {@code true} if this string is not empty and every character is a letter, as
     * {@link Character#isLetter(int)} defines it. The string is read as code points: a surrogate
     * pair is one character, a lone surrogate is a character of its own.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if this string is a non-empty run of letters.
     * @since   21
     */
    public boolean isAlpha() {
        return !isEmpty() && allInClass(CLASS_LETTER);
    }

    /**
     * Returns {@code true} if every character of this string is a letter or the space
     * {@code U+0020}, read as code points; an empty string answers {@code true}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if this string holds only letters and spaces.
     * @since   21
     */
    public boolean isAlphaSpace() {
        return allInClass(CLASS_LETTER | CLASS_SPACE);
    }

    /**
     * Returns {@code true} if this string is not empty and every character is a letter or a
     * digit, as {@link Character#isLetterOrDigit(int)} defines them, read as code points.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if this string is a non-empty run of letters and digits.
     * @since   21
     */
    public boolean isAlphanumeric() {
        return !isEmpty() && allInClass(CLASS_LETTER | CLASS_DIGIT);
    }

    /**
     * Returns {@code true} if every character of this string is a letter, a digit or the space
     * {@code U+0020}, read as code points; an empty string answers {@code true}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if this string holds only letters, digits and spaces.
     * @since   21
     */
    public boolean isAlphanumericSpace() {
        return allInClass(CLASS_LETTER | CLASS_DIGIT | CLASS_SPACE);
    }

    /**
     * Returns {@code true} if every character of this string is a printable ASCII character,
     * {@code U+0020} to {@code U+007E}; an empty string answers {@code true}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if this string holds only printable ASCII characters.
     * @since   21
     */
    public boolean isAsciiPrintable() {
        return allInClass(CLASS_PRINTABLE);
    }

    /**
     * Returns {@code true} if this string is not empty and every character is a digit, as
     * {@link Character#isDigit(int)} defines it (any Unicode decimal digit), read as code points.
     * A sign or a decimal point is not a digit.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if this string is a non-empty run of digits.
     * @since   21
     */
    public boolean isNumeric() {
        return !isEmpty() && allInClass(CLASS_DIGIT);
    }

    /**
     * Returns {@code true} if every character of this string is a digit or the space
     * {@code U+0020}, read as code points; an empty string answers {@code true}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if this string holds only digits and spaces.
     * @since   21
     */
    public boolean isNumericSpace() {
        return allInClass(CLASS_DIGIT | CLASS_SPACE);
    }

    /**
     * Returns {@code true} if this string is not empty and every character is lower case, as
     * {@link Character#isLowerCase(int)} defines it, read as code points.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if this string is a non-empty run of lower-case characters.
     * @since   21
     */
    public boolean isAllLowerCase() {
        return !isEmpty() && allInClass(CLASS_LOWER);
    }

    /**
     * Returns {@code true} if this string is not empty and every character is upper case, as
     * {@link Character#isUpperCase(int)} defines it, read as code points.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if this string is a non-empty run of upper-case characters.
     * @since   21
     */
    public boolean isAllUpperCase() {
        return !isEmpty() && allInClass(CLASS_UPPER);
    }


    /**
     * Returns {@code true} if this string holds at least one upper-case and at least one
     * lower-case character, as {@link Character#isUpperCase(int)} and
     * {@link Character#isLowerCase(int)} define them, read as code points; other characters do
     * not count.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  {@code true} if this string mixes upper and lower case.
     * @since   21
     */
    public boolean isMixedCase() {
        return foldClasses(CLASS_LOWER | CLASS_UPPER, false) == (CLASS_LOWER | CLASS_UPPER);
    }

    /** Character classes of the class-test methods, as bits. */
    private static final int CLASS_LETTER = 1;
    private static final int CLASS_DIGIT = 2;
    private static final int CLASS_SPACE = 4;
    private static final int CLASS_PRINTABLE = 8;
    private static final int CLASS_LOWER = 16;
    private static final int CLASS_UPPER = 32;
    /** {@link #foldClasses(int, boolean)}: a code point is in none of the classes. */
    private static final int CLASS_MISSING = -1;
    /** {@link #classesFromFacts}: the facts do not decide. */
    private static final int CLASS_UNKNOWN = -2;
    /** Units copied per bulk read of UTF-16 or M3 storage by the scanning methods. */
    private static final int SCAN_WINDOW = 512;

    /** Whether every code point of this string is in one of the given classes. */
    private boolean allInClass(int classes) {
        return foldClasses(classes, true) != CLASS_MISSING;
    }

    /**
     * The union of {@code classes} over the classes of this string's code points. With
     * {@code each}, {@link #CLASS_MISSING} as soon as a code point is in none of them; without,
     * the scan stops once the union is complete. Prepared M3 facts answer without a scan when
     * they decide; a flat Latin-1 string is read on its bytes; UTF-16 and M3 storage are read
     * in bulk windows, a surrogate pair never split between two windows.
     */
    private int foldClasses(int classes, boolean each) {
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        int known = facts == null ? CLASS_UNKNOWN : classesFromFacts(facts, classes, each);
        if (known != CLASS_UNKNOWN) return known;
        int length = length();
        int seen = 0;
        if (storage == null && isLatin1()) {
            byte[] bits = Latin1Classes.BITS;
            byte[] bytes = value;
            for (int index = 0; index < length && (each || seen != classes); index++) {
                int found = bits[bytes[index] & 0xFF] & classes;
                if (each && found == 0) return CLASS_MISSING;
                seen |= found;
            }
            return seen;
        }
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length && (each || seen != classes); ) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            int count = to - from;
            if (to < length && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;
            for (int at = 0; at < count && (each || seen != classes); ) {
                char unit = window[at];
                int point = Character.isHighSurrogate(unit) && at + 1 < count
                        && Character.isLowSurrogate(window[at + 1])
                        ? Character.toCodePoint(unit, window[at + 1]) : unit;
                int found = classesIn(point, classes);
                if (each && found == 0) return CLASS_MISSING;
                seen |= found;
                at += Character.charCount(point);
            }
            from += count;
        }
        return seen;
    }

    /**
     * {@link #foldClasses(int, boolean)} answered from prepared M3 facts, or
     * {@link #CLASS_UNKNOWN}. Facts carry exact ASCII regex classes: on ASCII text a letter is
     * ALPHA, a digit DIGIT, a letter or digit ALNUM, lower and upper case LOWER and UPPER.
     */
    private static int classesFromFacts(M3StringFacts facts, int classes, boolean each) {
        if (each) {
            int regex = switch (classes) {
                case CLASS_LETTER -> M3StringFacts.REGEX_ALPHA;
                case CLASS_DIGIT -> M3StringFacts.REGEX_DIGIT;
                case CLASS_LETTER | CLASS_DIGIT -> M3StringFacts.REGEX_ALNUM;
                case CLASS_LOWER -> M3StringFacts.REGEX_LOWER;
                case CLASS_UPPER -> M3StringFacts.REGEX_UPPER;
                default -> 0;
            };
            if (regex == 0 || !facts.ascii) return CLASS_UNKNOWN;
            return (facts.regexAllClasses & regex) != 0 ? classes : CLASS_MISSING;
        }
        if (classes != (CLASS_LOWER | CLASS_UPPER)) return CLASS_UNKNOWN;
        int seen = ((facts.regexAnyClasses & M3StringFacts.REGEX_LOWER) != 0 ? CLASS_LOWER : 0)
                | ((facts.regexAnyClasses & M3StringFacts.REGEX_UPPER) != 0 ? CLASS_UPPER : 0);
        return seen == classes || facts.ascii ? seen : CLASS_UNKNOWN;
    }

    /** The classes among {@code classes} that one code point is in, testing only those. */
    private static int classesIn(int point, int classes) {
        if (point < Latin1Classes.BITS.length) return Latin1Classes.BITS[point] & classes;
        int found = (classes & CLASS_LETTER) != 0 && Character.isLetter(point) ? CLASS_LETTER : 0;
        found |= (classes & CLASS_DIGIT) != 0 && Character.isDigit(point) ? CLASS_DIGIT : 0;
        found |= (classes & CLASS_LOWER) != 0 && Character.isLowerCase(point) ? CLASS_LOWER : 0;
        return found | ((classes & CLASS_UPPER) != 0 && Character.isUpperCase(point)
                ? CLASS_UPPER : 0);
    }

    /** The classes of one code point. */
    private static int classesOf(int point) {
        int classes = Character.isLetter(point) ? CLASS_LETTER : 0;
        classes |= Character.isDigit(point) ? CLASS_DIGIT : 0;
        classes |= point == ' ' ? CLASS_SPACE : 0;
        classes |= point >= ' ' && point <= '~' ? CLASS_PRINTABLE : 0;
        classes |= Character.isLowerCase(point) ? CLASS_LOWER : 0;
        return classes | (Character.isUpperCase(point) ? CLASS_UPPER : 0);
    }

    /** The classes of each Latin-1 unit, built once from {@link #classesOf(int)}. */
    private static final class Latin1Classes {
        static final byte[] BITS = new byte[256];
        static {
            for (int unit = 0; unit < BITS.length; unit++) {
                BITS[unit] = (byte) classesOf(unit);
            }
        }
    }

    /**
     * Compares two strings lexicographically, as {@link #compareTo(String)} does, ordering
     * {@code null} before every string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   a   a string, or {@code null}.
     * @param   b   a string, or {@code null}.
     * @return  {@code 0} for two nulls, {@code -1} or {@code 1} when one is {@code null}, and
     *          {@code a.compareTo(b)} otherwise.
     * @since   21
     */
    public static int compare(String a, String b) {
        return compare(a, b, true);
    }

    /**
     * Compares two strings lexicographically, as {@link #compareTo(String)} does, ordering
     * {@code null} before every string when {@code nullIsLess}, after every string otherwise.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   a            a string, or {@code null}.
     * @param   b            a string, or {@code null}.
     * @param   nullIsLess   whether {@code null} sorts first.
     * @return  {@code 0} for two nulls, {@code -1} or {@code 1} when one is {@code null}, and
     *          {@code a.compareTo(b)} otherwise.
     * @since   21
     */
    public static int compare(String a, String b, boolean nullIsLess) {
        if (a == b) return 0;
        if (a == null) return nullIsLess ? -1 : 1;
        if (b == null) return nullIsLess ? 1 : -1;
        return a.compareTo(b);
    }

    /**
     * Compares two strings ignoring case, as {@link #compareToIgnoreCase(String)} does, ordering
     * {@code null} before every string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   a   a string, or {@code null}.
     * @param   b   a string, or {@code null}.
     * @return  {@code 0} for two nulls, {@code -1} or {@code 1} when one is {@code null}, and
     *          {@code a.compareToIgnoreCase(b)} otherwise.
     * @since   21
     */
    public static int compareIgnoreCase(String a, String b) {
        return compareIgnoreCase(a, b, true);
    }

    /**
     * Compares two strings ignoring case, as {@link #compareToIgnoreCase(String)} does, ordering
     * {@code null} before every string when {@code nullIsLess}, after every string otherwise.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   a            a string, or {@code null}.
     * @param   b            a string, or {@code null}.
     * @param   nullIsLess   whether {@code null} sorts first.
     * @return  {@code 0} for two nulls, {@code -1} or {@code 1} when one is {@code null}, and
     *          {@code a.compareToIgnoreCase(b)} otherwise.
     * @since   21
     */
    public static int compareIgnoreCase(String a, String b, boolean nullIsLess) {
        if (a == b) return 0;
        if (a == null) return nullIsLess ? -1 : 1;
        if (b == null) return nullIsLess ? 1 : -1;
        return a.compareToIgnoreCase(b);
    }

    /**
     * Returns {@code true} if both strings are {@code null}, or both are not and they are equal,
     * ignoring case as {@link #equalsIgnoreCase(String)} does when {@code ignoreCase}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   a            a string, or {@code null}.
     * @param   b            a string, or {@code null}.
     * @param   ignoreCase   whether case is ignored.
     * @return  whether the strings are equal.
     * @since   21
     */
    public static boolean equals(String a, String b, boolean ignoreCase) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return ignoreCase ? a.equalsIgnoreCase(b) : a.equals(b);
    }

    /**
     * Returns the longest prefix common to all the given strings. The prefix never ends between
     * the two halves of a surrogate pair that all the strings share up to its high half. A
     * {@code null} or empty array, or any {@code null} element, gives the empty string; a single
     * string gives itself.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   strings   the strings, or {@code null}.
     * @return  the common prefix, a range of the first string.
     * @since   21
     */
    public static String getCommonPrefix(String... strings) {
        if (strings == null || strings.length == 0) return "";
        int end = differenceOfAll(strings);
        if (end < 0) return strings[0] == null ? "" : strings[0];
        return end == 0 ? "" : strings[0].substring(0, end);
    }

    /**
     * The index of the first difference among {@code strings} as {@link #getCommonPrefix} needs
     * it: {@code -1} for fewer than two strings, for all {@code null} and for all empty;
     * {@code 0} when one is {@code null} or empty; the shortest length when all agree up to it;
     * moved back onto a shared high surrogate.
     */
    private static int differenceOfAll(String[] strings) {
        if (strings.length <= 1) return -1;
        boolean anyNull = false;
        boolean allNull = true;
        int shortest = Integer.MAX_VALUE;
        int longest = 0;
        for (String s : strings) {
            if (s == null) {
                anyNull = true;
                shortest = 0;
            } else {
                allNull = false;
                shortest = Math.min(shortest, s.length());
                longest = Math.max(longest, s.length());
            }
        }
        if (allNull || longest == 0 && !anyNull) return -1;
        if (shortest == 0) return 0;
        int first = commonPrefixLength(strings, shortest);
        if (first > 0 && first < shortest && Character.isLowSurrogate(strings[0].charAt(first))
                && Character.isHighSurrogate(strings[0].charAt(first - 1))) {
            first--;
        }
        return first == shortest ? (shortest != longest ? shortest : -1) : first;
    }

    /** The number of leading units all {@code strings} share, at most {@code limit}. */
    private static int commonPrefixLength(String[] strings, int limit) {
        String first = strings[0];
        for (int index = 0; index < limit; index++) {
            char unit = first.charAt(index);
            for (int k = 1; k < strings.length; k++) {
                if (strings[k].charAt(index) != unit) return index;
            }
        }
        return limit;
    }

    /**
     * Returns the part of {@code other} that follows the longest prefix it shares with this
     * string, as {@link #indexOfDifference(String)} defines it; the empty string when the two are
     * equal.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   other   the string to take the remainder of.
     * @return  a range of {@code other}.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @since   21
     */
    public String difference(String other) {
        int at = indexOfDifference(other);
        return at < 0 ? "" : other.substring(at);
    }

    /**
     * Returns {@code true} if this string is equal to any of the given sequences, compared as
     * {@link #contentEquals(CharSequence)} does; {@code null} elements equal nothing.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   searches   the sequences to compare with.
     * @return  whether one of them equals this string.
     * @throws  NullPointerException if {@code searches} is {@code null}.
     * @since   21
     */
    public boolean equalsAny(CharSequence... searches) {
        for (CharSequence search : searches) {
            if (search != null && contentEquals(search)) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this string is equal to any of the given sequences ignoring case,
     * as {@link #equalsIgnoreCase(String)} does; {@code null} elements equal nothing.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   searches   the sequences to compare with.
     * @return  whether one of them equals this string ignoring case.
     * @throws  NullPointerException if {@code searches} is {@code null}.
     * @since   21
     */
    public boolean equalsAnyIgnoreCase(CharSequence... searches) {
        for (CharSequence search : searches) {
            if (search != null && contentEqualsIgnoreCase(search)) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this string and {@code cs} have the same length and are equal
     * ignoring case, unit by unit as {@link #equalsIgnoreCase(String)} compares.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   cs   the sequence to compare with.
     * @return  whether they are equal ignoring case.
     * @throws  NullPointerException if {@code cs} is {@code null}.
     * @since   21
     */
    public boolean contentEqualsIgnoreCase(CharSequence cs) {
        return equalsIgnoreCase(cs.toString());
    }

    /**
     * Compares this string with {@code other} by code point, the order of Unicode scalar values:
     * unlike {@link #compareTo(String)}, a supplementary character sorts after every character of
     * the Basic Multilingual Plane. A lone surrogate counts as the code point of its unit.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   other   the string to compare with.
     * @return  a negative, zero or positive value as this string sorts before, with or after
     *          {@code other}: the difference of the first differing code points, or of the lengths
     *          in code points when one is a prefix of the other.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @since   21
     */
    public int compareCodePoints(String other) {
        int common = indexOfDifference(other);
        if (common < 0) return 0;
        int start = common;
        if (start > 0 && Character.isHighSurrogate(charAt(start - 1))) start--;
        int a = codePointOrEnd(this, start);
        int b = codePointOrEnd(other, start);
        if (a == b && start < common) {
            start = common;
            a = codePointOrEnd(this, start);
            b = codePointOrEnd(other, start);
        }
        if (a >= 0 && b >= 0 && a != b) return a - b;
        return codePointCount(start, length()) - other.codePointCount(start, other.length());
    }

    /** The code point at {@code index} of {@code s}, or {@code -1} at its end. */
    private static int codePointOrEnd(String s, int index) {
        return index < s.length() ? s.codePointAt(index) : -1;
    }

    /**
     * Returns {@code true} if this string and {@code other} are equal when ASCII letters are
     * compared without case; every other unit must be identical.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   other   the string to compare with.
     * @return  whether they are equal ignoring ASCII case.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @since   21
     */
    public boolean equalsIgnoreAsciiCase(String other) {
        if (length() != other.length()) return false;
        M3StringFacts mine = m3() == null ? null : m3().factsIfPrepared();
        M3StringFacts theirs = other.m3() == null ? null : other.m3().factsIfPrepared();
        if (mine != null && theirs != null && mine.ascii && theirs.ascii
                && mine.asciiLowerHash != theirs.asciiLowerHash) {
            return false;
        }
        return compareToIgnoreAsciiCase(other) == 0;
    }

    /**
     * Compares this string with {@code other} lexicographically after mapping the ASCII letters
     * {@code A..Z} to {@code a..z}; every other unit compares by its value.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   other   the string to compare with.
     * @return  the difference of the first differing mapped units, or of the lengths.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @since   21
     */
    public int compareToIgnoreAsciiCase(String other) {
        int count = Math.min(length(), other.length());
        char[] mine = new char[Math.min(count, SCAN_WINDOW)];
        char[] theirs = new char[mine.length];
        for (int from = 0; from < count; from += mine.length) {
            int n = Math.min(mine.length, count - from);
            getChars(from, from + n, mine, 0);
            other.getChars(from, from + n, theirs, 0);
            for (int k = 0; k < n; k++) {
                char a = asciiLower(mine[k]);
                char b = asciiLower(theirs[k]);
                if (a != b) return a - b;
            }
        }
        return length() - other.length();
    }

    /**
     * Returns the longest prefix of this string that {@code other} starts with, never ending
     * between the two halves of a surrogate pair of either sequence.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   other   the sequence to compare with.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @since   21
     */
    public String commonPrefix(CharSequence other) {
        return commonPrefix(other, false);
    }

    /**
     * Returns the longest prefix of this string that {@code other} starts with, comparing units
     * ignoring case as {@link #regionMatches(boolean, int, String, int, int)} does when
     * {@code ignoreCase}; it never ends between the two halves of a surrogate pair of either
     * sequence.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   other        the sequence to compare with.
     * @param   ignoreCase   whether case is ignored.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @since   21
     */
    public String commonPrefix(CharSequence other, boolean ignoreCase) {
        int i = matchingUnits(other, Math.min(length(), other.length()), ignoreCase, true);
        if (i > 0 && (pairAt(this, i - 1) || pairAt(other, i - 1))) i--;
        return substring(0, i);
    }

    /**
     * Returns the longest suffix of this string that {@code other} ends with, never starting
     * between the two halves of a surrogate pair of either sequence.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   other   the sequence to compare with.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @since   21
     */
    public String commonSuffix(CharSequence other) {
        return commonSuffix(other, false);
    }

    /**
     * Returns the longest suffix of this string that {@code other} ends with, comparing units
     * ignoring case as {@link #regionMatches(boolean, int, String, int, int)} does when
     * {@code ignoreCase}; it never starts between the two halves of a surrogate pair of either
     * sequence.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   other        the sequence to compare with.
     * @param   ignoreCase   whether case is ignored.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @since   21
     */
    public String commonSuffix(CharSequence other, boolean ignoreCase) {
        int length = length();
        int otherLength = other.length();
        int i = matchingUnits(other, Math.min(length, otherLength), ignoreCase, false);
        if (i > 0 && (pairAt(this, length - i - 1) || pairAt(other, otherLength - i - 1))) i--;
        return substring(length - i);
    }

    /**
     * The number of units this string and {@code other} share from the start ({@code forward})
     * or from the end, at most {@code limit}, compared by {@link #sameUnit}. This string, and
     * {@code other} when it is a String, are read in bulk windows.
     */
    private int matchingUnits(CharSequence other, int limit, boolean ignoreCase, boolean forward) {
        int length = length();
        int otherLength = other.length();
        char[] mine = new char[Math.min(limit, SCAN_WINDOW)];
        String flat = other instanceof String s ? s : null;
        char[] theirs = flat != null ? new char[mine.length] : null;
        for (int i = 0; i < limit; ) {
            int count = Math.min(mine.length, limit - i);
            int begin = forward ? i : length - i - count;
            int otherBegin = forward ? i : otherLength - i - count;
            getChars(begin, begin + count, mine, 0);
            if (flat != null) flat.getChars(otherBegin, otherBegin + count, theirs, 0);
            for (int k = 0; k < count; k++) {
                int at = forward ? k : count - 1 - k;
                char unit = flat != null ? theirs[at] : other.charAt(otherBegin + at);
                if (!sameUnit(mine[at], unit, ignoreCase)) return i + k;
            }
            i += count;
        }
        return limit;
    }

    /** Whether two units are equal, or equal ignoring case by regionMatches' rule. */
    private static boolean sameUnit(char a, char b, boolean ignoreCase) {
        if (a == b) return true;
        if (!ignoreCase) return false;
        char upperA = Character.toUpperCase(a);
        char upperB = Character.toUpperCase(b);
        return upperA == upperB || Character.toLowerCase(upperA) == Character.toLowerCase(upperB);
    }

    /** Whether a surrogate pair starts at {@code index} of {@code cs}. */
    private static boolean pairAt(CharSequence cs, int index) {
        return index >= 0 && index + 1 < cs.length() && Character.isHighSurrogate(cs.charAt(index))
                && Character.isLowSurrogate(cs.charAt(index + 1));
    }

    /**
     * Returns {@code true} if this string and {@code other} are canonically equivalent: equal
     * after both are put into Unicode Normalization Form C.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   other   the string to compare with.
     * @return  whether they are canonically equivalent.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @since   21
     */
    public boolean canonicalEquals(String other) {
        if (equals(other)) return true;
        java.text.Normalizer.Form nfc = java.text.Normalizer.Form.NFC;
        return java.text.Normalizer.normalize(this, nfc)
                .equals(java.text.Normalizer.normalize(other, nfc));
    }

    /**
     * Returns the leftmost {@code length} characters of this string: this string when it is not
     * longer, the empty string for a negative {@code length}. The cut never splits a surrogate
     * pair; it moves left onto the high half.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   length   the number of characters wanted.
     * @return  a range of this string.
     * @since   21
     */
    public String left(int length) {
        if (length < 0) return "";
        if (length() <= length) return this;
        int cut = length;
        if (splitsPair(cut)) cut--;
        return substring(0, cut);
    }

    /**
     * Returns the rightmost {@code length} characters of this string: this string when it is not
     * longer, the empty string for a negative {@code length}. The cut never splits a surrogate
     * pair; it moves right past the low half.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   length   the number of characters wanted.
     * @return  a range of this string.
     * @since   21
     */
    public String right(int length) {
        if (length < 0) return "";
        if (length() <= length) return this;
        int start = length() - length;
        if (splitsPair(start)) start++;
        return substring(start);
    }

    /**
     * Returns {@code length} characters of this string from {@code position}: a negative position
     * counts as 0, a position past the end or a negative length gives the empty string. Neither cut
     * splits a surrogate pair: the start moves right past a low half, the end left onto a high one.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   position   the index of the first character.
     * @param   length     the number of characters wanted.
     * @return  a range of this string.
     * @since   21
     */
    public String mid(int position, int length) {
        if (length < 0 || position > length()) return "";
        int pos = Math.max(position, 0);
        int start = splitsPair(pos) ? pos + 1 : pos;
        if (length() - pos <= length) return substring(start);
        int end = pos + length;
        if (splitsPair(end)) end--;
        return substring(start, Math.max(start, end));
    }

    /**
     * Returns this string truncated to at most {@code maxWidth} characters, never splitting a
     * surrogate pair.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   maxWidth   the most characters kept.
     * @return  a range of this string.
     * @throws  IllegalArgumentException if {@code maxWidth} is negative.
     * @since   21
     */
    public String truncate(int maxWidth) {
        return truncate(0, maxWidth);
    }

    /**
     * Returns at most {@code maxWidth} characters of this string from {@code offset}; an offset
     * past the end gives the empty string. Neither cut splits a surrogate pair.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   offset     the index of the first character kept.
     * @param   maxWidth   the most characters kept.
     * @return  a range of this string.
     * @throws  IllegalArgumentException if {@code offset} or {@code maxWidth} is negative.
     * @since   21
     */
    public String truncate(int offset, int maxWidth) {
        if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
        if (maxWidth < 0) throw new IllegalArgumentException("maxWidth cannot be negative");
        int length = length();
        int start = Math.min(offset, length);
        int end = Math.min(offset > length - maxWidth ? length : offset + maxWidth, length);
        if (splitsPair(start)) start++;
        if (splitsPair(end)) end--;
        return substring(start, Math.max(start, end));
    }

    /**
     * Returns this string without its first {@code count} characters: the empty string when it is
     * not longer.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   count   the number of characters dropped.
     * @return  a range of this string.
     * @throws  IllegalArgumentException if {@code count} is negative.
     * @since   21
     */
    public String drop(int count) {
        if (count < 0) throw new IllegalArgumentException("Requested character count " + count
                + " is less than zero.");
        return substring(Math.min(count, length()));
    }

    /**
     * Returns this string without its last {@code count} characters: the empty string when it is
     * not longer.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   count   the number of characters dropped.
     * @return  a range of this string.
     * @throws  IllegalArgumentException if {@code count} is negative.
     * @since   21
     */
    public String dropLast(int count) {
        if (count < 0) throw new IllegalArgumentException("Requested character count " + count
                + " is less than zero.");
        return substring(0, Math.max(length() - count, 0));
    }

    /**
     * Returns the longest prefix of this string whose characters ({@code char} values) all pass
     * {@code test}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   test   the test of each character.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code test} is {@code null}.
     * @since   21
     */
    public String takeWhile(java.util.function.IntPredicate test) {
        return substring(0, passingUnits(test, true));
    }

    /**
     * Returns this string without its longest prefix whose characters all pass {@code test}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   test   the test of each character.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code test} is {@code null}.
     * @since   21
     */
    public String dropWhile(java.util.function.IntPredicate test) {
        return substring(passingUnits(test, true));
    }

    /**
     * Returns the longest suffix of this string whose characters all pass {@code test}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   test   the test of each character.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code test} is {@code null}.
     * @since   21
     */
    public String takeLastWhile(java.util.function.IntPredicate test) {
        return substring(length() - passingUnits(test, false));
    }

    /**
     * Returns this string without its longest suffix whose characters all pass {@code test}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   test   the test of each character.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code test} is {@code null}.
     * @since   21
     */
    public String dropLastWhile(java.util.function.IntPredicate test) {
        return substring(0, length() - passingUnits(test, false));
    }

    /**
     * The number of units from the start ({@code forward}) or from the end that pass
     * {@code test}, read in bulk windows.
     */
    private int passingUnits(java.util.function.IntPredicate test, boolean forward) {
        java.util.Objects.requireNonNull(test, "test");
        int length = length();
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int done = 0; done < length; ) {
            int count = Math.min(window.length, length - done);
            int begin = forward ? done : length - done - count;
            getChars(begin, begin + count, window, 0);
            for (int k = 0; k < count; k++) {
                if (!test.test(window[forward ? k : count - 1 - k])) return done + k;
            }
            done += count;
        }
        return length;
    }

    /**
     * Returns the part of this string before the first occurrence of {@code separator}: this
     * string when it does not occur, the empty string for an empty separator.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separator   the separator.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @since   21
     */
    public String substringBefore(String separator) {
        java.util.Objects.requireNonNull(separator, "separator");
        if (isEmpty()) return this;
        int at = indexOf(separator);
        return at < 0 ? this : substring(0, at);
    }

    /**
     * Returns the part of this string before the first occurrence of the character
     * {@code codePoint}, or this string when it does not occur.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   codePoint   the separator character.
     * @return  a range of this string.
     * @since   21
     */
    public String substringBefore(int codePoint) {
        int at = indexOf(codePoint);
        return at < 0 ? this : substring(0, at);
    }

    /**
     * Returns the part of this string after the first occurrence of {@code separator}: the empty
     * string when it does not occur, this string for an empty separator.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separator   the separator.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @since   21
     */
    public String substringAfter(String separator) {
        java.util.Objects.requireNonNull(separator, "separator");
        if (isEmpty()) return this;
        int at = indexOf(separator);
        return at < 0 ? "" : substring(at + separator.length());
    }

    /**
     * Returns the part of this string after the first occurrence of the character
     * {@code codePoint}, or the empty string when it does not occur.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   codePoint   the separator character.
     * @return  a range of this string.
     * @since   21
     */
    public String substringAfter(int codePoint) {
        if (isEmpty()) return this;
        int at = indexOf(codePoint);
        return at < 0 ? "" : substring(at + Character.charCount(codePoint));
    }

    /**
     * Returns the part of this string before the last occurrence of {@code separator}: this
     * string when it does not occur or the separator is empty.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separator   the separator.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @since   21
     */
    public String substringBeforeLast(String separator) {
        java.util.Objects.requireNonNull(separator, "separator");
        if (isEmpty() || separator.isEmpty()) return this;
        int at = lastIndexOf(separator);
        return at < 0 ? this : substring(0, at);
    }

    /**
     * Returns the part of this string after the last occurrence of {@code separator}: the empty
     * string when it does not occur, ends this string, or is empty.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separator   the separator.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @since   21
     */
    public String substringAfterLast(String separator) {
        java.util.Objects.requireNonNull(separator, "separator");
        if (isEmpty()) return this;
        if (separator.isEmpty()) return "";
        int at = lastIndexOf(separator);
        int end = at + separator.length();
        return at < 0 || end == length() ? "" : substring(end);
    }

    /**
     * Returns the part of this string after the last occurrence of the character
     * {@code codePoint}: the empty string when it does not occur or ends this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   codePoint   the separator character.
     * @return  a range of this string.
     * @since   21
     */
    public String substringAfterLast(int codePoint) {
        if (isEmpty()) return this;
        int at = lastIndexOf(codePoint);
        int width = Character.charCount(codePoint);
        return at < 0 || at == length() - width ? "" : substring(at + width);
    }

    /**
     * Returns the part of this string before the first occurrence of {@code delimiter}, or
     * {@code ifAbsent} when it does not occur.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter   the delimiter.
     * @param   ifAbsent    the answer when the delimiter does not occur.
     * @return  a range of this string, or {@code ifAbsent}.
     * @throws  NullPointerException if {@code delimiter} is {@code null}.
     * @since   21
     */
    public String substringBefore(String delimiter, String ifAbsent) {
        int at = indexOf(delimiter);
        return at < 0 ? ifAbsent : substring(0, at);
    }

    /**
     * Returns the part of this string after the first occurrence of {@code delimiter}, or
     * {@code ifAbsent} when it does not occur.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter   the delimiter.
     * @param   ifAbsent    the answer when the delimiter does not occur.
     * @return  a range of this string, or {@code ifAbsent}.
     * @throws  NullPointerException if {@code delimiter} is {@code null}.
     * @since   21
     */
    public String substringAfter(String delimiter, String ifAbsent) {
        int at = indexOf(delimiter);
        return at < 0 ? ifAbsent : substring(at + delimiter.length());
    }

    /**
     * Returns the part of this string before the last occurrence of {@code delimiter} that starts
     * before the last character, or {@code ifAbsent} when there is none (so an empty delimiter is
     * found before the last character, and not at all in an empty string).
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter   the delimiter.
     * @param   ifAbsent    the answer when the delimiter does not occur.
     * @return  a range of this string, or {@code ifAbsent}.
     * @throws  NullPointerException if {@code delimiter} is {@code null}.
     * @since   21
     */
    public String substringBeforeLast(String delimiter, String ifAbsent) {
        int at = lastIndexOf(delimiter, length() - 1);
        return at < 0 ? ifAbsent : substring(0, at);
    }

    /**
     * Returns the part of this string after the last occurrence of {@code delimiter} that starts
     * before the last character, or {@code ifAbsent} when there is none (so an empty delimiter is
     * found before the last character, and not at all in an empty string).
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter   the delimiter.
     * @param   ifAbsent    the answer when the delimiter does not occur.
     * @return  a range of this string, or {@code ifAbsent}.
     * @throws  NullPointerException if {@code delimiter} is {@code null}.
     * @since   21
     */
    public String substringAfterLast(String delimiter, String ifAbsent) {
        int at = lastIndexOf(delimiter, length() - 1);
        return at < 0 ? ifAbsent : substring(at + delimiter.length());
    }

    /**
     * Returns the part of this string between the first two occurrences of {@code tag}, or
     * {@code null} when there are not two.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   tag   the tag before and after the part.
     * @return  a range of this string, or {@code null}.
     * @throws  NullPointerException if {@code tag} is {@code null}.
     * @since   21
     */
    public String substringBetween(String tag) {
        return substringBetween(tag, tag);
    }

    /**
     * Returns the part of this string between the first occurrence of {@code open} and the first
     * occurrence of {@code close} after it, or {@code null} when either is missing.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   open    the text before the part.
     * @param   close   the text after the part.
     * @return  a range of this string, or {@code null}.
     * @throws  NullPointerException if {@code open} or {@code close} is {@code null}.
     * @since   21
     */
    public String substringBetween(String open, String close) {
        java.util.Objects.requireNonNull(close, "close");
        int start = indexOf(open);
        if (start < 0) return null;
        int end = indexOf(close, start + open.length());
        return end < 0 ? null : substring(start + open.length(), end);
    }

    /**
     * Returns this string without {@code prefix} when it starts with it, else this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code str.removeprefix} has the same result.
     *
     * @param   prefix   the prefix to remove.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code prefix} is {@code null}.
     * @since   21
     */
    public String removeStart(String prefix) {
        return startsWith(prefix) ? substring(prefix.length()) : this;
    }

    /**
     * Returns this string without its first character when that is {@code c}, else this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   c   the character to remove.
     * @return  a range of this string.
     * @since   21
     */
    public String removeStart(char c) {
        return !isEmpty() && charAt(0) == c ? substring(1) : this;
    }

    /**
     * Returns this string without {@code suffix} when it ends with it and both are non-empty,
     * else this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code str.removesuffix} has the same result.
     *
     * @param   suffix   the suffix to remove.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code suffix} is {@code null}.
     * @since   21
     */
    public String removeEnd(String suffix) {
        java.util.Objects.requireNonNull(suffix, "suffix");
        if (isEmpty() || suffix.isEmpty()) return this;
        return endsWith(suffix) ? substring(0, length() - suffix.length()) : this;
    }

    /**
     * Returns this string without {@code prefix} when it starts with it ignoring case, as
     * {@link #regionMatches(boolean, int, String, int, int)} compares, else this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   prefix   the prefix to remove.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code prefix} is {@code null}.
     * @since   21
     */
    public String removeStartIgnoreCase(String prefix) {
        java.util.Objects.requireNonNull(prefix, "prefix");
        return startsWithIgnoreCase(prefix) ? substring(prefix.length()) : this;
    }

    /**
     * Returns this string without {@code suffix} when it ends with it ignoring case and both are
     * non-empty, else this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   suffix   the suffix to remove.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code suffix} is {@code null}.
     * @since   21
     */
    public String removeEndIgnoreCase(String suffix) {
        java.util.Objects.requireNonNull(suffix, "suffix");
        if (isEmpty() || suffix.isEmpty()) return this;
        return endsWithIgnoreCase(suffix) ? substring(0, length() - suffix.length()) : this;
    }

    /**
     * Returns this string without {@code prefix} and {@code suffix} when it starts with the one,
     * ends with the other and is at least as long as both; else this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   prefix   the prefix to remove.
     * @param   suffix   the suffix to remove.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code prefix} or {@code suffix} is {@code null}.
     * @since   21
     */
    public String removeSurrounding(CharSequence prefix, CharSequence suffix) {
        String p = prefix.toString();
        String s = suffix.toString();
        if (length() >= p.length() + s.length() && startsWith(p) && endsWith(s)) {
            return substring(p.length(), length() - s.length());
        }
        return this;
    }

    /**
     * Returns this string without {@code delimiter} at both ends, as
     * {@link #removeSurrounding(CharSequence, CharSequence)} with the delimiter on both sides.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter   the text to remove at both ends.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code delimiter} is {@code null}.
     * @since   21
     */
    public String removeSurrounding(CharSequence delimiter) {
        return removeSurrounding(delimiter, delimiter);
    }

    /**
     * Returns this string without {@code wrapToken} at both ends when it starts and ends with it
     * and is at least twice as long; else this string. The part kept ends at the last occurrence
     * of the token.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   wrapToken   the token to remove at both ends.
     * @return  a range of this string.
     * @throws  NullPointerException if {@code wrapToken} is {@code null}.
     * @since   21
     */
    public String unwrap(String wrapToken) {
        java.util.Objects.requireNonNull(wrapToken, "wrapToken");
        if (isEmpty() || wrapToken.isEmpty() || wrapToken.length() > length() / 2) return this;
        if (startsWith(wrapToken) && endsWith(wrapToken)) {
            return substring(wrapToken.length(), lastIndexOf(wrapToken));
        }
        return this;
    }

    /**
     * Returns this string without {@code wrapChar} at both ends when it starts and ends with it
     * and is longer than one character; else this string. The character {@code U+0000} never
     * wraps.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   wrapChar   the character to remove at both ends.
     * @return  a range of this string.
     * @since   21
     */
    public String unwrap(char wrapChar) {
        int length = length();
        if (length <= 1 || wrapChar == 0) return this;
        if (charAt(0) == wrapChar && charAt(length - 1) == wrapChar) {
            return substring(1, length - 1);
        }
        return this;
    }

    /** Whether {@code index} falls between the two halves of a surrogate pair of this string. */
    private boolean splitsPair(int index) {
        return index > 0 && index < length() && Character.isHighSurrogate(charAt(index - 1))
                && Character.isLowSurrogate(charAt(index));
    }

    /**
     * Returns this string padded on the left with spaces to {@code size} characters, or this
     * string when it is at least that long.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   size   the length wanted.
     * @return  the padded string, or this string.
     * @since   21
     */
    public String leftPad(int size) {
        return leftPad(size, ' ');
    }

    /**
     * Returns this string padded on the left with {@code padChar} to {@code size} characters, or
     * this string when it is at least that long.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   size      the length wanted.
     * @param   padChar   the character to pad with.
     * @return  the padded string, or this string.
     * @since   21
     */
    public String leftPad(int size, char padChar) {
        if (size <= length()) return this;
        return padded(String.valueOf(padChar), size - length(), 0);
    }

    /**
     * Returns this string padded on the left to {@code size} characters with {@code padStr},
     * repeated from its start and cut to fit; an empty {@code padStr} pads with spaces. When the
     * cut would split a surrogate pair of {@code padStr}, the padding is one character shorter.
     * Returns this string when it is at least {@code size} characters long or no padding is left.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   size     the length wanted.
     * @param   padStr   the text to pad with.
     * @return  the padded string, or this string.
     * @throws  NullPointerException if {@code padStr} is {@code null}.
     * @since   21
     */
    public String leftPad(int size, String padStr) {
        java.util.Objects.requireNonNull(padStr, "padStr");
        if (size <= length()) return this;
        String pad = padStr.isEmpty() ? " " : padStr;
        return padded(pad, padUnits(pad, size - length()), 0);
    }

    /**
     * Returns this string padded on the right with spaces to {@code size} characters, or this
     * string when it is at least that long.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   size   the length wanted.
     * @return  the padded string, or this string.
     * @since   21
     */
    public String rightPad(int size) {
        return rightPad(size, ' ');
    }

    /**
     * Returns this string padded on the right with {@code padChar} to {@code size} characters,
     * or this string when it is at least that long.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   size      the length wanted.
     * @param   padChar   the character to pad with.
     * @return  the padded string, or this string.
     * @since   21
     */
    public String rightPad(int size, char padChar) {
        if (size <= length()) return this;
        return padded(String.valueOf(padChar), 0, size - length());
    }

    /**
     * Returns this string padded on the right to {@code size} characters with {@code padStr},
     * repeated from its start and cut to fit; an empty {@code padStr} pads with spaces. When the
     * cut would split a surrogate pair of {@code padStr}, the padding is one character shorter.
     * Returns this string when it is at least {@code size} characters long or no padding is left.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   size     the length wanted.
     * @param   padStr   the text to pad with.
     * @return  the padded string, or this string.
     * @throws  NullPointerException if {@code padStr} is {@code null}.
     * @since   21
     */
    public String rightPad(int size, String padStr) {
        java.util.Objects.requireNonNull(padStr, "padStr");
        if (size <= length()) return this;
        String pad = padStr.isEmpty() ? " " : padStr;
        return padded(pad, 0, padUnits(pad, size - length()));
    }

    /**
     * Returns this string centered in {@code size} characters of spaces: the left side gets half
     * of the padding, rounded down, the right side the rest. Returns this string when it is at
     * least {@code size} characters long.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   size   the length wanted.
     * @return  the centered string, or this string.
     * @since   21
     */
    public String center(int size) {
        return center(size, ' ');
    }

    /**
     * Returns this string centered in {@code size} characters of {@code padChar}: the left side
     * gets half of the padding, rounded down, the right side the rest. Returns this string when
     * it is at least {@code size} characters long.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   size      the length wanted.
     * @param   padChar   the character to pad with.
     * @return  the centered string, or this string.
     * @since   21
     */
    public String center(int size, char padChar) {
        if (size <= length()) return this;
        int pads = size - length();
        return padded(String.valueOf(padChar), pads / 2, pads - pads / 2);
    }

    /**
     * Returns this string centered in {@code size} characters of {@code padStr}: first padded on
     * the left with half of the padding, rounded down, as {@link #leftPad(int, String)} pads,
     * then on the right up to {@code size} as {@link #rightPad(int, String)} pads. An empty
     * {@code padStr} pads with spaces. Returns this string when it is at least {@code size}
     * characters long.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   size     the length wanted.
     * @param   padStr   the text to pad with.
     * @return  the centered string, or this string.
     * @throws  NullPointerException if {@code padStr} is {@code null}.
     * @since   21
     */
    public String center(int size, String padStr) {
        java.util.Objects.requireNonNull(padStr, "padStr");
        int length = length();
        if (size <= length) return this;
        String pad = padStr.isEmpty() ? " " : padStr;
        int left = padUnits(pad, (size - length) / 2);
        return padded(pad, left, padUnits(pad, size - length - left));
    }

    /**
     * Returns this string padded at the start to {@code targetLength} characters with
     * {@code padString}, repeated from its start and cut to fit, as JavaScript's
     * {@code String.prototype.padStart} pads; this string when it is at least
     * {@code targetLength} characters long or {@code padString} is empty. Unlike
     * {@link #leftPad(int, String)}, an empty pad string adds nothing and the cut may split a
     * surrogate pair of {@code padString}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   targetLength   the length wanted.
     * @param   padString      the text to pad with.
     * @return  the padded string, or this string.
     * @throws  NullPointerException if {@code padString} is {@code null}.
     * @since   21
     */
    public String padStart(int targetLength, String padString) {
        java.util.Objects.requireNonNull(padString, "padString");
        if (targetLength <= length() || padString.isEmpty()) return this;
        return padded(padString, targetLength - length(), 0);
    }

    /**
     * Returns this string padded at the end to {@code targetLength} characters with
     * {@code padString}, repeated from its start and cut to fit, as JavaScript's
     * {@code String.prototype.padEnd} pads; this string when it is at least
     * {@code targetLength} characters long or {@code padString} is empty. Unlike
     * {@link #rightPad(int, String)}, an empty pad string adds nothing and the cut may split a
     * surrogate pair of {@code padString}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   targetLength   the length wanted.
     * @param   padString      the text to pad with.
     * @return  the padded string, or this string.
     * @throws  NullPointerException if {@code padString} is {@code null}.
     * @since   21
     */
    public String padEnd(int targetLength, String padString) {
        java.util.Objects.requireNonNull(padString, "padString");
        if (targetLength <= length() || padString.isEmpty()) return this;
        return padded(padString, 0, targetLength - length());
    }

    /**
     * Returns this string filled on the left with {@code '0'} digits to {@code width} code
     * points, as Python's {@code str.zfill} fills: a leading {@code '+'} or {@code '-'} stays in
     * front of the digits. The width counts code points, as Python's {@code len} does: a
     * surrogate pair counts once, as does a lone surrogate. Returns this string when it has at
     * least {@code width} code points.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   width   the number of code points wanted.
     * @return  the filled string, or this string.
     * @since   21
     */
    public String zeroFill(int width) {
        int length = length();
        if (width <= length - (length >>> 1)) return this;
        int fill = width - codePointCount(0, length);
        if (fill <= 0) return this;
        String zeros = "0".repeat(fill);
        if (length > 0 && (charAt(0) == '+' || charAt(0) == '-')) {
            return substring(0, 1).concat(zeros).concat(substring(1));
        }
        return zeros.concat(this);
    }

    /**
     * Returns this string repeated {@code count} times with {@code separator} between the
     * repetitions: the empty string for a count that is not positive, this string for a count of
     * one.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separator   the text between two repetitions.
     * @param   count       the number of repetitions.
     * @return  the repeated string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @throws  IllegalArgumentException if {@code count} is greater than one and the result,
     *          {@code count} times this string and {@code count - 1} times the separator, would be
     *          longer than {@link Integer#MAX_VALUE}.
     * @since   21
     */
    public String repeat(String separator, int count) {
        java.util.Objects.requireNonNull(separator, "separator");
        if (count <= 0) return "";
        if (count == 1) return this;
        if ((long) length() * count + (long) separator.length() * (count - 1)
                > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("The requested result is too large for a String.");
        }
        if (separator.isEmpty()) return repeat(count);
        return concat(separator.concat(this).repeat(count - 1));
    }

    /**
     * Returns this string with its characters in reverse order, as
     * {@link StringBuilder#reverse()} reverses them: the {@code char} values are reversed, then
     * every low surrogate that a high surrogate follows is swapped with it, from the start, so a
     * surrogate pair keeps its order.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  the reversed string, or this string when it has fewer than two characters.
     * @since   21
     */
    public String reverse() {
        int length = length();
        if (length <= 1) return this;
        M3String storage = m3();
        if (storage == null && isLatin1()) {
            byte[] bytes = value;
            byte[] reversed = new byte[length];
            for (int index = 0; index < length; index++) {
                reversed[index] = bytes[length - 1 - index];
            }
            return new String(reversed, LATIN1);
        }
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        char[] reversed = new char[length];
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; from += window.length) {
            int count = Math.min(window.length, length - from);
            getChars(from, from + count, window, 0);
            for (int k = 0; k < count; k++) {
                reversed[length - 1 - from - k] = window[k];
            }
        }
        if (facts == null || !facts.latin1) keepPairOrder(reversed);
        return new String(reversed);
    }

    /**
     * Swaps every low surrogate of {@code units} that a high surrogate follows with it, from the
     * start, as {@link StringBuilder#reverse()} does after reversing the units.
     */
    private static void keepPairOrder(char[] units) {
        for (int k = 0; k + 1 < units.length; k++) {
            if (Character.isLowSurrogate(units[k]) && Character.isHighSurrogate(units[k + 1])) {
                char low = units[k];
                units[k] = units[k + 1];
                units[++k] = low;
            }
        }
    }

    /**
     * Returns the parts of this string between occurrences of {@code separator} in reverse
     * order, joined by single separators. Empty parts, between two adjacent separators or before
     * or after one at either end, are dropped. Returns this string when it has no separator.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separator   the character between the parts.
     * @return  the parts in reverse order.
     * @since   21
     */
    public String reverseDelimited(char separator) {
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts != null && !facts.mayContainCodeUnit(separator)) return this;
        StringBuilder out = null;
        int lastStart = 0;
        int lastEnd = 0;
        for (int end = length(); end > 0; ) {
            int at = lastIndexOf(separator, end - 1);
            if (at + 1 < end) {
                if (lastEnd == 0) {
                    lastStart = at + 1;
                    lastEnd = end;
                } else {
                    if (out == null) {
                        out = new StringBuilder(length()).append(this, lastStart, lastEnd);
                    }
                    out.append(separator).append(this, at + 1, end);
                }
            }
            end = at;
        }
        return out != null ? out.toString() : substring(lastStart, lastEnd);
    }

    /**
     * Returns this string rotated right by {@code shift} characters, left for a negative shift:
     * the last {@code shift % length()} characters move to the front. Returns this string when
     * it is empty or {@code shift} is a multiple of its length. A rotation may split a surrogate
     * pair.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   shift   the number of characters to rotate right by.
     * @return  the rotated string, or this string.
     * @since   21
     */
    public String rotate(int shift) {
        int length = length();
        if (length == 0 || shift % length == 0) return this;
        int offset = -(shift % length);
        int cut = offset < 0 ? length + offset : offset;
        return substring(cut).concat(substring(0, cut));
    }

    /**
     * Returns this string with {@code suffix} appended, unless {@code suffix} is empty or this
     * string ends with it or with an element of {@code suffixes}; {@code null} elements are
     * skipped and an empty element always matches.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   suffix     the suffix to append.
     * @param   suffixes   more suffixes that count as present.
     * @return  this string, or this string followed by {@code suffix}.
     * @throws  NullPointerException if {@code suffix} or {@code suffixes} is {@code null}.
     * @since   21
     */
    public String appendIfMissing(CharSequence suffix, CharSequence... suffixes) {
        return hasAnyAffix(suffix, suffixes, false, false) ? this : concat(suffix.toString());
    }

    /**
     * Returns this string with {@code suffix} appended, unless {@code suffix} is empty or this
     * string ends with it or with an element of {@code suffixes}, ignoring case as
     * {@link #regionMatches(boolean, int, String, int, int)} does; {@code null} elements are
     * skipped and an empty element always matches.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   suffix     the suffix to append.
     * @param   suffixes   more suffixes that count as present.
     * @return  this string, or this string followed by {@code suffix}.
     * @throws  NullPointerException if {@code suffix} or {@code suffixes} is {@code null}.
     * @since   21
     */
    public String appendIfMissingIgnoreCase(CharSequence suffix, CharSequence... suffixes) {
        return hasAnyAffix(suffix, suffixes, false, true) ? this : concat(suffix.toString());
    }

    /**
     * Returns this string with {@code prefix} in front, unless {@code prefix} is empty or this
     * string starts with it or with an element of {@code prefixes}; {@code null} elements are
     * skipped and an empty element always matches.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   prefix     the prefix to put in front.
     * @param   prefixes   more prefixes that count as present.
     * @return  this string, or {@code prefix} followed by this string.
     * @throws  NullPointerException if {@code prefix} or {@code prefixes} is {@code null}.
     * @since   21
     */
    public String prependIfMissing(CharSequence prefix, CharSequence... prefixes) {
        return hasAnyAffix(prefix, prefixes, true, false) ? this : prefix.toString().concat(this);
    }

    /**
     * Returns this string with {@code prefix} in front, unless {@code prefix} is empty or this
     * string starts with it or with an element of {@code prefixes}, ignoring case as
     * {@link #regionMatches(boolean, int, String, int, int)} does; {@code null} elements are
     * skipped and an empty element always matches.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   prefix     the prefix to put in front.
     * @param   prefixes   more prefixes that count as present.
     * @return  this string, or {@code prefix} followed by this string.
     * @throws  NullPointerException if {@code prefix} or {@code prefixes} is {@code null}.
     * @since   21
     */
    public String prependIfMissingIgnoreCase(CharSequence prefix, CharSequence... prefixes) {
        return hasAnyAffix(prefix, prefixes, true, true) ? this : prefix.toString().concat(this);
    }

    /**
     * Whether this string starts ({@code atStart}) or ends with {@code affix}, or {@code affix}
     * is empty, or it starts or ends with a non-null element of {@code more}; ignoring case as
     * {@link #regionMatches(boolean, int, String, int, int)} does when {@code ignoreCase}.
     */
    private boolean hasAnyAffix(CharSequence affix, CharSequence[] more, boolean atStart,
            boolean ignoreCase) {
        java.util.Objects.requireNonNull(affix, atStart ? "prefix" : "suffix");
        java.util.Objects.requireNonNull(more, atStart ? "prefixes" : "suffixes");
        if (affix.isEmpty() || hasAffix(affix, atStart, ignoreCase)) return true;
        for (CharSequence other : more) {
            if (other != null && hasAffix(other, atStart, ignoreCase)) return true;
        }
        return false;
    }

    /** Whether this string starts ({@code atStart}) or ends with {@code affix}. */
    private boolean hasAffix(CharSequence affix, boolean atStart, boolean ignoreCase) {
        if (ignoreCase) return atStart ? startsWithIgnoreCase(affix) : endsWithIgnoreCase(affix);
        return atStart ? startsWith(affix.toString()) : endsWith(affix.toString());
    }

    /**
     * Returns this string with {@code wrapWith} before and after it; this string when it is
     * empty or {@code wrapWith} is {@code U+0000}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   wrapWith   the character to wrap with.
     * @return  the wrapped string, or this string.
     * @since   21
     */
    public String wrap(char wrapWith) {
        if (isEmpty() || wrapWith == 0) return this;
        String with = String.valueOf(wrapWith);
        return with.concat(this).concat(with);
    }

    /**
     * Returns this string with {@code wrapWith} before and after it; this string when it or
     * {@code wrapWith} is empty.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   wrapWith   the text to wrap with.
     * @return  the wrapped string, or this string.
     * @throws  NullPointerException if {@code wrapWith} is {@code null}.
     * @since   21
     */
    public String wrap(String wrapWith) {
        java.util.Objects.requireNonNull(wrapWith, "wrapWith");
        if (isEmpty() || wrapWith.isEmpty()) return this;
        return wrapWith.concat(this).concat(wrapWith);
    }

    /**
     * Returns this string with {@code wrapWith} added in front unless it starts with it, and at
     * the end unless it ends with it; this string when it is empty or {@code wrapWith} is
     * {@code U+0000}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   wrapWith   the character to wrap with.
     * @return  the wrapped string, or this string.
     * @since   21
     */
    public String wrapIfMissing(char wrapWith) {
        if (isEmpty() || wrapWith == 0) return this;
        String with = String.valueOf(wrapWith);
        String start = charAt(0) == wrapWith ? this : with.concat(this);
        return charAt(length() - 1) == wrapWith ? start : start.concat(with);
    }

    /**
     * Returns this string with {@code wrapWith} added in front unless it starts with it, and at
     * the end unless it ends with it; this string when it or {@code wrapWith} is empty.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   wrapWith   the text to wrap with.
     * @return  the wrapped string, or this string.
     * @throws  NullPointerException if {@code wrapWith} is {@code null}.
     * @since   21
     */
    public String wrapIfMissing(String wrapWith) {
        java.util.Objects.requireNonNull(wrapWith, "wrapWith");
        if (isEmpty() || wrapWith.isEmpty()) return this;
        String start = startsWith(wrapWith) ? this : wrapWith.concat(this);
        return endsWith(wrapWith) ? start : start.concat(wrapWith);
    }

    /**
     * Returns this string with the characters from {@code start} to {@code end} replaced by
     * {@code overlay}. Both indexes are clamped to {@code 0..length()} and swapped when
     * {@code start} is the greater; a cut that would split a surrogate pair moves outward, so the
     * whole pair is replaced.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   overlay   the text put in place of the range.
     * @param   start     one end of the range.
     * @param   end       the other end of the range.
     * @return  the overlaid string, or this string when nothing changes.
     * @throws  NullPointerException if {@code overlay} is {@code null}.
     * @since   21
     */
    public String overlay(String overlay, int start, int end) {
        java.util.Objects.requireNonNull(overlay, "overlay");
        int length = length();
        int from = Math.min(Math.max(start, 0), length);
        int to = Math.min(Math.max(end, 0), length);
        if (from > to) {
            int swap = from;
            from = to;
            to = swap;
        }
        if (splitsPair(from)) from--;
        if (splitsPair(to)) to++;
        if (from == to && overlay.isEmpty()) return this;
        return substring(0, from).concat(overlay).concat(substring(to));
    }

    /**
     * The number of units of {@code pad} that pad {@code pads} characters: one fewer when cutting
     * the repeated pad there would split one of its surrogate pairs.
     */
    private static int padUnits(String pad, int pads) {
        return pad.splitsPair(pads % pad.length()) ? pads - 1 : pads;
    }

    /**
     * This string between {@code left} and {@code right} units of {@code pad}, each repeated
     * from the start of {@code pad}; this string when neither is positive.
     */
    private String padded(String pad, int left, int right) {
        String start = left > 0 ? tiled(pad, left).concat(this) : this;
        return right > 0 ? start.concat(tiled(pad, right)) : start;
    }

    /** The first {@code count} units of {@code pad} repeated without end. */
    private static String tiled(String pad, int count) {
        int whole = count / pad.length();
        String rest = pad.substring(0, count - whole * pad.length());
        return whole == 0 ? rest : pad.repeat(whole).concat(rest);
    }

    /**
     * Splits this string around runs of whitespace, as {@link Character#isWhitespace(char)}
     * defines it, unit by unit. Whitespace at the ends gives no empty part, and a run of
     * whitespace separates once. An empty string gives an empty array.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. Unlike
     * {@link #split(String)}, it takes no regular expression.
     *
     * @return  the parts, each a range of this string.
     * @since   21
     */
    public String[] split() {
        return splitOnUnits(null, -1, false);
    }

    /**
     * Splits this string around occurrences of {@code separatorChar}. Separators at the ends give
     * no empty part, and adjacent separators separate once. An empty string gives an empty array.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. Unlike
     * {@link #split(String)}, it takes no regular expression.
     *
     * @param   separatorChar   the separator.
     * @return  the parts, each a range of this string.
     * @since   21
     */
    public String[] split(char separatorChar) {
        return splitOnUnit(separatorChar, -1, false);
    }

    /**
     * Splits this string at every whitespace unit, as {@link Character#isWhitespace(char)}
     * defines it, keeping the empty parts between adjacent separators and at the ends. An empty
     * string gives an empty array.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  the parts, each a range of this string.
     * @since   21
     */
    public String[] splitPreserveAllTokens() {
        return splitOnUnits(null, -1, true);
    }

    /**
     * Splits this string at every occurrence of {@code separatorChar}, keeping the empty parts
     * between adjacent separators and at the ends. An empty string gives an empty array.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separatorChar   the separator.
     * @return  the parts, each a range of this string.
     * @since   21
     */
    public String[] splitPreserveAllTokens(char separatorChar) {
        return splitOnUnit(separatorChar, -1, true);
    }

    /**
     * Splits this string at every unit that occurs in {@code separatorChars}, keeping the empty
     * parts between adjacent separators and at the ends. An empty string gives an empty array;
     * empty {@code separatorChars} give this string as the only part.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separatorChars   the separator units.
     * @return  the parts, each a range of this string.
     * @throws  NullPointerException if {@code separatorChars} is {@code null}.
     * @since   21
     */
    public String[] splitPreserveAllTokens(String separatorChars) {
        return splitPreserveAllTokens(separatorChars, -1);
    }

    /**
     * Splits this string at every unit that occurs in {@code separatorChars} into at most
     * {@code max} parts, keeping the empty parts between adjacent separators and at the ends.
     * The last part is the rest of this string from where it starts, separators included; a
     * {@code max} of zero or less sets no limit. An empty string gives an empty array.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separatorChars   the separator units.
     * @param   max              the most parts, or zero or less for no limit.
     * @return  the parts, each a range of this string.
     * @throws  NullPointerException if {@code separatorChars} is {@code null}.
     * @since   21
     */
    public String[] splitPreserveAllTokens(String separatorChars, int max) {
        java.util.Objects.requireNonNull(separatorChars, "separatorChars");
        return splitOnUnits(separatorChars, max, true);
    }

    /**
     * Splits this string around occurrences of {@code separator}, found left to right without
     * overlap. Separators at the ends give no empty part, and adjacent separators separate once.
     * An empty separator splits around runs of whitespace, as {@link #split()} does. An empty
     * string gives an empty array.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separator   the separator.
     * @return  the parts, each a range of this string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @since   21
     */
    public String[] splitByWholeSeparator(String separator) {
        return splitByWholeSeparator(separator, -1);
    }

    /**
     * Splits this string around occurrences of {@code separator} into at most {@code max} parts,
     * as {@link #splitByWholeSeparator(String)} does. The last part is the rest of this string
     * from where it starts, separators included; a {@code max} of zero or less sets no limit.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separator   the separator.
     * @param   max         the most parts, or zero or less for no limit.
     * @return  the parts, each a range of this string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @since   21
     */
    public String[] splitByWholeSeparator(String separator, int max) {
        return wholeSeparatorParts(separator, max, false);
    }

    /**
     * Splits this string at every occurrence of {@code separator}, found left to right without
     * overlap, keeping the empty parts between adjacent separators and at the ends. An empty
     * separator splits at every whitespace unit, as {@link #splitPreserveAllTokens()} does. An
     * empty string gives an empty array.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separator   the separator.
     * @return  the parts, each a range of this string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @since   21
     */
    public String[] splitByWholeSeparatorPreserveAllTokens(String separator) {
        return splitByWholeSeparatorPreserveAllTokens(separator, -1);
    }

    /**
     * Splits this string at every occurrence of {@code separator} into at most {@code max}
     * parts, as {@link #splitByWholeSeparatorPreserveAllTokens(String)} does. The last part is
     * the rest of this string from where it starts, separators included; a {@code max} of zero
     * or less sets no limit.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   separator   the separator.
     * @param   max         the most parts, or zero or less for no limit.
     * @return  the parts, each a range of this string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @since   21
     */
    public String[] splitByWholeSeparatorPreserveAllTokens(String separator, int max) {
        return wholeSeparatorParts(separator, max, true);
    }

    /**
     * Splits this string into runs of characters of one type, as {@link Character#getType(int)}
     * gives it. The string is read as code points: a surrogate pair is one character, a lone
     * surrogate is a character of its own. An empty string gives an empty array.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  the runs, each a range of this string.
     * @since   21
     */
    public String[] splitByCharacterType() {
        return characterTypeRuns(false);
    }

    /**
     * Splits this string into runs of characters of one type, as
     * {@link #splitByCharacterType()} does, except that an upper-case letter directly before a
     * run of lower-case letters starts that run: {@code "ASFRules"} gives {@code "ASF"} and
     * {@code "Rules"}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  the runs, each a range of this string.
     * @since   21
     */
    public String[] splitByCharacterTypeCamelCase() {
        return characterTypeRuns(true);
    }

    /**
     * Splits this string at the first occurrence of {@code separator} into three parts: the part
     * before it, the separator and the part after it; when it does not occur, this string and two
     * empty strings. As Python reads strings as code points, an occurrence that starts or ends
     * between the two halves of a surrogate pair does not count.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code str.partition} has the same result.
     *
     * @param   separator   the separator.
     * @return  an array of three ranges of this string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @throws  IllegalArgumentException if {@code separator} is empty.
     * @since   21
     */
    public String[] partition(String separator) {
        int at = indexOfWhole(pythonSeparator(separator), 0);
        if (at < 0) return new String[] {this, "", ""};
        int end = at + separator.length();
        return new String[] {substring(0, at), substring(at, end), substring(end)};
    }

    /**
     * Splits this string at the last occurrence of {@code separator} into three parts: the part
     * before it, the separator and the part after it; when it does not occur, two empty strings
     * and this string. An occurrence that starts or ends between the two halves of a surrogate
     * pair does not count.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code str.rpartition} has the same result.
     *
     * @param   separator   the separator.
     * @return  an array of three ranges of this string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @throws  IllegalArgumentException if {@code separator} is empty.
     * @since   21
     */
    public String[] rpartition(String separator) {
        int at = lastIndexOfWhole(pythonSeparator(separator), length());
        if (at < 0) return new String[] {"", "", this};
        int end = at + separator.length();
        return new String[] {substring(0, at), substring(at, end), substring(end)};
    }

    /**
     * Splits this string around occurrences of {@code separator}, found right to left without
     * overlap, making at most {@code maxSplit} splits, the rightmost ones; a negative
     * {@code maxSplit} sets no limit. Every separator separates, so empty parts are kept, and an
     * empty string gives one empty part. An occurrence that starts or ends between the two
     * halves of a surrogate pair does not count.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code str.rsplit(sep, maxsplit)} has the same result.
     *
     * @param   separator   the separator.
     * @param   maxSplit    the most splits, or a negative value for no limit.
     * @return  the parts, left to right, each a range of this string.
     * @throws  NullPointerException if {@code separator} is {@code null}.
     * @throws  IllegalArgumentException if {@code separator} is empty.
     * @since   21
     */
    public String[] rsplit(String separator, int maxSplit) {
        int width = pythonSeparator(separator).length();
        ArrayList<String> parts = new ArrayList<>();
        int end = length();
        for (int splits = 0; splits != maxSplit; splits++) {
            int at = lastIndexOfWhole(separator, end);
            if (at < 0) break;
            parts.add(substring(at + width, end));
            end = at;
        }
        parts.add(substring(0, end));
        java.util.Collections.reverse(parts);
        return parts.toArray(new String[0]);
    }

    /**
     * Splits this string into lines at the line boundaries of Python's {@code str.splitlines}:
     * {@code \n}, {@code \r}, {@code \r\n}, {@code U+000B}, {@code U+000C}, {@code U+001C},
     * {@code U+001D}, {@code U+001E}, {@code U+0085}, {@code U+2028} and {@code U+2029}. A
     * boundary at the end gives no empty last line; an empty string gives an empty array.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code str.splitlines(keepends)} has the same result. Unlike {@link #lines()}, it knows
     * every boundary above and can keep them.
     *
     * @param   keepEnds   whether each line keeps its boundary.
     * @return  the lines, each a range of this string.
     * @since   21
     */
    public String[] splitLines(boolean keepEnds) {
        int length = length();
        if (length == 0) return new String[0];
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts != null && (facts.regexAnyClasses & M3StringFacts.REGEX_VERTICAL) == 0
                && noUnitBetween(facts, (char) 0x1C, (char) 0x1E)) {
            return new String[] {this};
        }
        ArrayList<String> lines = new ArrayList<>();
        int begin = 0;
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; from += window.length) {
            int count = Math.min(window.length, length - from);
            getChars(from, from + count, window, 0);
            for (int k = 0; k < count; k++) {
                int at = from + k;
                if (at < begin || !isPythonLineBoundary(window[k])) continue;
                int next = at + 1;
                if (window[k] == '\r' && next < length
                        && (k + 1 < count ? window[k + 1] : charAt(next)) == '\n') {
                    next++;
                }
                lines.add(substring(begin, keepEnds ? next : at));
                begin = next;
            }
        }
        if (begin < length) lines.add(substring(begin));
        return lines.toArray(new String[0]);
    }

    /**
     * Splits this string into chunks of {@code size} characters ({@code char} values), the last
     * one shorter when the length is not a multiple of {@code size}. An empty string gives an
     * empty array.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Kotlin's
     * {@code CharSequence.chunked(size)} has the same result.
     *
     * @param   size   the length of each chunk.
     * @return  the chunks, each a range of this string.
     * @throws  IllegalArgumentException if {@code size} is not positive.
     * @since   21
     */
    public String[] chunked(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size " + size + " must be greater than zero.");
        }
        int length = length();
        String[] chunks = new String[length / size + (length % size == 0 ? 0 : 1)];
        for (int k = 0; k < chunks.length; k++) {
            int begin = k * size;
            chunks[k] = substring(begin, begin + Math.min(size, length - begin));
        }
        return chunks;
    }

    /**
     * The parts of this string around separator units: whitespace units when {@code chars} is
     * {@code null}, else the units of {@code chars}. Every separator ends a part when
     * {@code all}; otherwise only a non-empty part counts and no empty part ends the string. The
     * {@code max}-th part takes the rest of this string. Prepared M3 facts that rule out every
     * separator give this string at once; else the text is read in bulk windows.
     */
    private String[] splitOnUnits(String chars, int max, boolean all) {
        if (chars != null && chars.length() == 1) return splitOnUnit(chars.charAt(0), max, all);
        int length = length();
        if (length == 0) return new String[0];
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts != null && noSeparatorUnit(facts, chars)) return new String[] {this};
        ArrayList<String> parts = new ArrayList<>();
        int begin = 0;
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; from += window.length) {
            int count = Math.min(window.length, length - from);
            getChars(from, from + count, window, 0);
            for (int k = 0; k < count; k++) {
                char unit = window[k];
                boolean separates = chars == null ? Character.isWhitespace(unit)
                        : chars.indexOf(unit) >= 0;
                if (!separates) continue;
                if (addPart(parts, begin, from + k, max, all)) return parts.toArray(new String[0]);
                begin = from + k + 1;
            }
        }
        return lastParts(parts, begin, all);
    }

    /**
     * The parts of this string around the unit {@code separator}, by the rule of
     * {@link #splitOnUnits}; the separators are found by {@link #indexOf(int, int)}.
     */
    private String[] splitOnUnit(char separator, int max, boolean all) {
        if (isEmpty()) return new String[0];
        ArrayList<String> parts = new ArrayList<>();
        int begin = 0;
        for (int at = indexOf(separator); at >= 0; at = indexOf(separator, at + 1)) {
            if (addPart(parts, begin, at, max, all)) return parts.toArray(new String[0]);
            begin = at + 1;
        }
        return lastParts(parts, begin, all);
    }

    /**
     * The parts of this string around the non-overlapping occurrences of {@code separator}, by
     * the rule of {@link #splitOnUnits}; an empty separator splits at whitespace units.
     */
    private String[] wholeSeparatorParts(String separator, int max, boolean all) {
        java.util.Objects.requireNonNull(separator, "separator");
        if (isEmpty()) return new String[0];
        if (separator.isEmpty()) return splitOnUnits(null, max, all);
        ArrayList<String> parts = new ArrayList<>();
        int begin = 0;
        for (int at = indexOf(separator); at >= 0; at = indexOf(separator, begin)) {
            if (addPart(parts, begin, at, max, all)) return parts.toArray(new String[0]);
            begin = at + separator.length();
        }
        return lastParts(parts, begin, all);
    }

    /**
     * Adds the part from {@code begin} to the separator at {@code at} when it counts: always
     * when {@code all}, else when it is not empty. Returns {@code true} when it was the
     * {@code max}-th part, which then takes the rest of this string.
     */
    private boolean addPart(ArrayList<String> parts, int begin, int at, int max, boolean all) {
        if (!all && at == begin) return false;
        boolean last = parts.size() + 1 == max;
        parts.add(last ? substring(begin) : substring(begin, at));
        return last;
    }

    /** The parts with the rest of this string from {@code begin} when it counts, as an array. */
    private String[] lastParts(ArrayList<String> parts, int begin, boolean all) {
        if (all || begin < length()) parts.add(substring(begin));
        return parts.toArray(new String[0]);
    }

    /**
     * Whether prepared M3 facts rule out every separator unit: each unit of {@code chars}, or
     * each whitespace unit when it is {@code null}. Whitespace outside ASCII is in the regex
     * class HORIZONTAL or VERTICAL; in ASCII it is in SPACE or one of {@code U+001C..U+001F}.
     */
    private static boolean noSeparatorUnit(M3StringFacts facts, String chars) {
        if (chars != null) return noUnitMayOccur(facts, chars.toCharArray());
        int space = M3StringFacts.REGEX_SPACE | M3StringFacts.REGEX_HORIZONTAL
                | M3StringFacts.REGEX_VERTICAL;
        return (facts.regexAnyClasses & space) == 0
                && noUnitBetween(facts, (char) 0x1C, (char) 0x1F);
    }

    /** Whether prepared M3 facts rule out every unit from {@code low} to {@code high}. */
    private static boolean noUnitBetween(M3StringFacts facts, char low, char high) {
        for (char unit = low; unit <= high; unit++) {
            if (facts.mayContainCodeUnit(unit)) return false;
        }
        return true;
    }

    /**
     * The runs of {@link #splitByCharacterType()}, or with {@code camelCase} of
     * {@link #splitByCharacterTypeCamelCase()}. The text is read in bulk windows, a surrogate
     * pair never split between two windows.
     */
    private String[] characterTypeRuns(boolean camelCase) {
        int length = length();
        if (length == 0) return new String[0];
        ArrayList<String> runs = new ArrayList<>();
        int begin = 0;
        int previous = 0;
        int runType = -1;
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; ) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            int count = to - from;
            if (to < length && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;
            for (int at = 0; at < count; ) {
                char unit = window[at];
                int point = Character.isHighSurrogate(unit) && at + 1 < count
                        && Character.isLowSurrogate(window[at + 1])
                        ? Character.toCodePoint(unit, window[at + 1]) : unit;
                int type = Character.getType(point);
                int index = from + at;
                if (index > 0 && type != runType) {
                    if (!camelCase || type != Character.LOWERCASE_LETTER
                            || runType != Character.UPPERCASE_LETTER) {
                        runs.add(substring(begin, index));
                        begin = index;
                    } else if (previous != begin) {
                        runs.add(substring(begin, previous));
                        begin = previous;
                    }
                }
                runType = type;
                previous = index;
                at += Character.charCount(point);
            }
            from += count;
        }
        runs.add(substring(begin));
        return runs.toArray(new String[0]);
    }

    /** {@code separator}, checked as Python checks one: not {@code null}, not empty. */
    private static String pythonSeparator(String separator) {
        java.util.Objects.requireNonNull(separator, "separator");
        if (separator.isEmpty()) throw new IllegalArgumentException("empty separator");
        return separator;
    }

    /**
     * The index of the first occurrence of {@code separator} at or after {@code from} that
     * neither starts nor ends between the two halves of a surrogate pair, as Python finds a
     * string among code points; {@code -1} when there is none.
     */
    private int indexOfWhole(String separator, int from) {
        int width = separator.length();
        for (int at = indexOf(separator, from); at >= 0; at = indexOf(separator, at + 1)) {
            if (!splitsPair(at) && !splitsPair(at + width)) return at;
        }
        return -1;
    }

    /**
     * The index of the last occurrence of {@code separator} that ends at or before {@code end}
     * and neither starts nor ends between the two halves of a surrogate pair; {@code -1} when
     * there is none.
     */
    private int lastIndexOfWhole(String separator, int end) {
        int width = separator.length();
        for (int at = lastIndexOf(separator, end - width); at >= 0;
                at = lastIndexOf(separator, at - 1)) {
            if (!splitsPair(at) && !splitsPair(at + width)) return at;
        }
        return -1;
    }

    /** Whether {@code unit} is a line boundary of Python's {@code str.splitlines}. */
    private static boolean isPythonLineBoundary(char unit) {
        return (unit >= '\n' && unit <= '\r') || (unit >= (char) 0x1C && unit <= (char) 0x1E)
                || unit == (char) 0x85 || unit == (char) 0x2028 || unit == (char) 0x2029;
    }

    /**
     * Returns this string with the first {@code max} occurrences of {@code search} replaced by
     * {@code replacement}, found from left to right without overlap; a negative {@code max}
     * replaces every occurrence. Unlike {@link #replace(CharSequence, CharSequence)}, an empty
     * {@code search} occurs nowhere, so it gives this string, as do a {@code max} of 0 and an
     * empty string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search        the text to replace.
     * @param   replacement   the text put in its place.
     * @param   max           the most occurrences replaced, or a negative value for all.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code search} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replace(String search, String replacement, int max) {
        return replaceOccurrences(search, replacement, max, false);
    }

    /**
     * Returns this string with the first occurrence of {@code search} replaced by
     * {@code replacement}; an empty {@code search} occurs nowhere.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search        the text to replace.
     * @param   replacement   the text put in its place.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code search} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceOnce(String search, String replacement) {
        return replaceOccurrences(search, replacement, 1, false);
    }

    /**
     * Returns this string with every occurrence of {@code search} replaced by
     * {@code replacement}, found from left to right without overlap and ignoring case as
     * {@link #regionMatches(boolean, int, String, int, int)} compares; an empty {@code search}
     * occurs nowhere.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search        the text to replace, in any case.
     * @param   replacement   the text put in its place.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code search} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceIgnoreCase(String search, String replacement) {
        return replaceOccurrences(search, replacement, -1, true);
    }

    /**
     * Returns this string with the first {@code max} occurrences of {@code search} replaced by
     * {@code replacement}, found from left to right without overlap and ignoring case as
     * {@link #regionMatches(boolean, int, String, int, int)} compares; a negative {@code max}
     * replaces every occurrence, and an empty {@code search} occurs nowhere.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search        the text to replace, in any case.
     * @param   replacement   the text put in its place.
     * @param   max           the most occurrences replaced, or a negative value for all.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code search} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceIgnoreCase(String search, String replacement, int max) {
        return replaceOccurrences(search, replacement, max, true);
    }

    /**
     * Returns this string with the first occurrence of {@code search}, ignoring case as
     * {@link #regionMatches(boolean, int, String, int, int)} compares, replaced by
     * {@code replacement}; an empty {@code search} occurs nowhere.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   search        the text to replace, in any case.
     * @param   replacement   the text put in its place.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code search} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceOnceIgnoreCase(String search, String replacement) {
        return replaceOccurrences(search, replacement, 1, true);
    }

    /**
     * Returns this string without every occurrence of {@code remove}, found from left to right
     * without overlap; an empty {@code remove} occurs nowhere.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   remove   the text to remove.
     * @return  the remaining text, or this string when nothing is removed.
     * @throws  NullPointerException if {@code remove} is {@code null}.
     * @since   21
     */
    public String remove(String remove) {
        return replaceOccurrences(remove, "", -1, false);
    }

    /**
     * Returns this string without every occurrence of {@code remove}, found from left to right
     * without overlap and ignoring case as {@link #regionMatches(boolean, int, String, int, int)}
     * compares; an empty {@code remove} occurs nowhere.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   remove   the text to remove, in any case.
     * @return  the remaining text, or this string when nothing is removed.
     * @throws  NullPointerException if {@code remove} is {@code null}.
     * @since   21
     */
    public String removeIgnoreCase(String remove) {
        return replaceOccurrences(remove, "", -1, true);
    }

    /**
     * Returns this string without every occurrence of the character {@code remove}, a
     * {@code char} value: a surrogate value removes that unit wherever it occurs.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   remove   the character to remove.
     * @return  the remaining text, or this string when nothing is removed.
     * @since   21
     */
    public String remove(char remove) {
        return replaceOccurrences(String.valueOf(remove), "", -1, false);
    }

    /**
     * The replace and remove methods: the first {@code max} (all when negative) occurrences of
     * {@code search}, from left to right without overlap, found by {@link #indexOf(String, int)}
     * or, when {@code ignoreCase}, by {@link #indexOfIgnoreCase(CharSequence, int)}, are replaced
     * by {@code replacement}. An occurrence equal to the replacement is left as it is.
     */
    private String replaceOccurrences(String search, String replacement, int max,
            boolean ignoreCase) {
        java.util.Objects.requireNonNull(search, "search");
        java.util.Objects.requireNonNull(replacement, "replacement");
        if (isEmpty() || search.isEmpty() || max == 0) return this;
        if (!ignoreCase && (search.equals(replacement) || !mayContainUnit(search.charAt(0)))) {
            return this;
        }
        int count = search.length();
        boolean sameLength = count == replacement.length();
        Splicer edits = new Splicer(this);
        int at = ignoreCase ? indexOfIgnoreCase(search, 0) : indexOf(search);
        for (int left = max; at >= 0; ) {
            if (!ignoreCase || !sameLength || !regionMatches(at, replacement, 0, count)) {
                edits.edit(at, at + count, replacement);
            }
            if (--left == 0) break;
            at = ignoreCase ? indexOfIgnoreCase(search, at + count) : indexOf(search, at + count);
        }
        return edits.result();
    }

    /** Whether prepared M3 facts leave open that {@code unit} occurs in this string. */
    private boolean mayContainUnit(char unit) {
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        return facts == null || facts.mayContainCodeUnit(unit);
    }

    /**
     * Returns this string with every occurrence of each {@code searchList[i]} replaced by
     * {@code replacementList[i]} in one pass from left to right: at each point the earliest
     * occurrence of any pair is replaced (the lowest index wins a tie) and the search goes on after
     * it, so a replacement is never searched again. A pair whose search is {@code null} or empty,
     * or whose replacement is {@code null}, is left out. An empty string or an empty array gives
     * this string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   searchList        the texts to replace.
     * @param   replacementList   the texts put in their places, by index.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code searchList} or {@code replacementList} is
     *          {@code null}.
     * @throws  IllegalArgumentException if this string and both arrays are not empty and the
     *          arrays differ in length.
     * @since   21
     */
    public String replaceEach(String[] searchList, String[] replacementList) {
        String result = replacedEach(searchList, replacementList);
        if (result == null || result.length() == length() && result.equals(this)) return this;
        return result;
    }

    /**
     * Returns this string with {@link #replaceEach(String[], String[])} applied again to its own
     * result until a pass replaces nothing, for at most six passes.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   searchList        the texts to replace.
     * @param   replacementList   the texts put in their places, by index.
     * @return  the text after the last pass, or this string when the first pass replaces nothing.
     * @throws  NullPointerException if {@code searchList} or {@code replacementList} is
     *          {@code null}.
     * @throws  IllegalArgumentException if this string and both arrays are not empty and the
     *          arrays differ in length.
     * @throws  IllegalStateException if the sixth pass still replaces an occurrence, as a cycle of
     *          replacements or a text replaced by itself does.
     * @since   21
     */
    public String replaceEachRepeatedly(String[] searchList, String[] replacementList) {
        String result = this;
        for (int pass = 0; pass < EACH_PASSES; pass++) {
            String next = result.replacedEach(searchList, replacementList);
            if (next == result) return result;
            if (next != null) result = next;
        }
        throw new IllegalStateException("Replacements still occur after " + EACH_PASSES
                + " passes");
    }

    /** The passes {@link #replaceEachRepeatedly(String[], String[])} makes before it gives up. */
    private static final int EACH_PASSES = 6;

    /**
     * One pass of {@link #replaceEach(String[], String[])}: this string when no pair occurs,
     * {@code null} when each occurrence replaced equals its replacement, else the new text. The
     * next occurrence of each pair is kept, and searched again only once the pass is past it.
     */
    private String replacedEach(String[] searchList, String[] replacementList) {
        java.util.Objects.requireNonNull(searchList, "searchList");
        java.util.Objects.requireNonNull(replacementList, "replacementList");
        int pairs = searchList.length;
        if (isEmpty() || pairs == 0 || replacementList.length == 0) return this;
        if (pairs != replacementList.length) {
            throw new IllegalArgumentException("Search and replacement arrays differ in length: "
                    + pairs + " vs " + replacementList.length);
        }
        int[] next = new int[pairs];
        for (int i = 0; i < pairs; i++) {
            String search = searchList[i];
            boolean live = search != null && !search.isEmpty() && replacementList[i] != null
                    && mayContainUnit(search.charAt(0));
            next[i] = live ? indexOf(search) : -1;
        }
        Splicer edits = new Splicer(this);
        boolean replaced = false;
        for (int pair = earliest(next); pair >= 0; pair = earliest(next)) {
            String search = searchList[pair];
            int end = next[pair] + search.length();
            if (!search.equals(replacementList[pair])) {
                edits.edit(next[pair], end, replacementList[pair]);
            }
            replaced = true;
            for (int i = 0; i < pairs; i++) {
                if (next[i] >= 0 && next[i] < end) next[i] = indexOf(searchList[i], end);
            }
        }
        String result = edits.result();
        return result == this && replaced ? null : result;
    }

    /** The index of the least non-negative value of {@code next}, the first on a tie, or -1. */
    private static int earliest(int[] next) {
        int best = -1;
        for (int i = 0; i < next.length; i++) {
            if (next[i] >= 0 && (best < 0 || next[i] < next[best])) best = i;
        }
        return best;
    }

    /**
     * Returns this string with each character ({@code char} value) that occurs in
     * {@code searchChars} replaced by the character at the same index of {@code replaceChars},
     * the index of its first occurrence in {@code searchChars}; it is removed when
     * {@code replaceChars} is not that long.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. For one character,
     * {@link #replace(char, char)} is the same operation.
     *
     * @param   searchChars    the characters to replace.
     * @param   replaceChars   the characters put in their places, by index.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code searchChars} or {@code replaceChars} is
     *          {@code null}.
     * @since   21
     */
    public String replaceChars(String searchChars, String replaceChars) {
        java.util.Objects.requireNonNull(searchChars, "searchChars");
        java.util.Objects.requireNonNull(replaceChars, "replaceChars");
        if (isEmpty() || searchChars.isEmpty()) return this;
        char[] set = searchChars.toCharArray();
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts != null && noUnitMayOccur(facts, set)) return this;
        long low = asciiMask(set, 0);
        long high = asciiMask(set, 64);
        boolean wide = hasWideUnit(set);
        int length = length();
        Splicer edits = new Splicer(this);
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; from += window.length) {
            int count = Math.min(window.length, length - from);
            getChars(from, from + count, window, 0);
            for (int k = 0; k < count; k++) {
                char unit = window[k];
                int at = (unit < 128 ? inMask(unit, low, high) : wide)
                        ? searchChars.indexOf(unit) : -1;
                if (at >= replaceChars.length()) {
                    edits.edit(from + k, from + k + 1, "");
                } else if (at >= 0 && replaceChars.charAt(at) != unit) {
                    edits.edit(from + k, replaceChars.charAt(at));
                }
            }
        }
        return edits.result();
    }

    /**
     * Returns this string with its first character ({@code char} value) replaced by the value of
     * {@code transform} for it, narrowed to a {@code char} as by a cast; an empty string gives
     * itself.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   transform   the function of the first character.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code transform} is {@code null}.
     * @since   21
     */
    public String replaceFirstChar(java.util.function.IntUnaryOperator transform) {
        java.util.Objects.requireNonNull(transform, "transform");
        if (isEmpty()) return this;
        char first = charAt(0);
        char mapped = (char) transform.applyAsInt(first);
        return mapped == first ? this : spliced(0, 1, String.valueOf(mapped));
    }

    /**
     * Returns this string with the part before the first occurrence of {@code delimiter} replaced
     * by {@code replacement}, or this string when the delimiter does not occur.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter     the delimiter.
     * @param   replacement   the text put in place of the part.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code delimiter} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceBefore(String delimiter, String replacement) {
        return replaceBefore(delimiter, replacement, this);
    }

    /**
     * Returns this string with the part before the first occurrence of {@code delimiter} replaced
     * by {@code replacement}, or {@code missingDelimiterValue} when the delimiter does not occur.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter               the delimiter.
     * @param   replacement             the text put in place of the part.
     * @param   missingDelimiterValue   the answer when the delimiter does not occur.
     * @return  the replaced text (this string when nothing changes), or
     *          {@code missingDelimiterValue}.
     * @throws  NullPointerException if {@code delimiter} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceBefore(String delimiter, String replacement,
            String missingDelimiterValue) {
        java.util.Objects.requireNonNull(replacement, "replacement");
        int at = indexOf(delimiter);
        return at < 0 ? missingDelimiterValue : spliced(0, at, replacement);
    }

    /**
     * Returns this string with the part after the first occurrence of {@code delimiter} replaced
     * by {@code replacement}, or this string when the delimiter does not occur.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter     the delimiter.
     * @param   replacement   the text put in place of the part.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code delimiter} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceAfter(String delimiter, String replacement) {
        return replaceAfter(delimiter, replacement, this);
    }

    /**
     * Returns this string with the part after the first occurrence of {@code delimiter} replaced
     * by {@code replacement}, or {@code missingDelimiterValue} when the delimiter does not occur.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter               the delimiter.
     * @param   replacement             the text put in place of the part.
     * @param   missingDelimiterValue   the answer when the delimiter does not occur.
     * @return  the replaced text (this string when nothing changes), or
     *          {@code missingDelimiterValue}.
     * @throws  NullPointerException if {@code delimiter} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceAfter(String delimiter, String replacement,
            String missingDelimiterValue) {
        java.util.Objects.requireNonNull(replacement, "replacement");
        int at = indexOf(delimiter);
        return at < 0 ? missingDelimiterValue
                : spliced(at + delimiter.length(), length(), replacement);
    }

    /**
     * Returns this string with the part before the last occurrence of {@code delimiter} that
     * starts no later than the last character replaced by {@code replacement}, or this string
     * when there is none (so an empty delimiter is found at the last character, and not at all in
     * an empty string).
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter     the delimiter.
     * @param   replacement   the text put in place of the part.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code delimiter} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceBeforeLast(String delimiter, String replacement) {
        return replaceBeforeLast(delimiter, replacement, this);
    }

    /**
     * Returns this string with the part before the last occurrence of {@code delimiter} that
     * starts no later than the last character replaced by {@code replacement}, or
     * {@code missingDelimiterValue} when there is none (so an empty delimiter is found at the
     * last character, and not at all in an empty string).
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter               the delimiter.
     * @param   replacement             the text put in place of the part.
     * @param   missingDelimiterValue   the answer when the delimiter does not occur.
     * @return  the replaced text (this string when nothing changes), or
     *          {@code missingDelimiterValue}.
     * @throws  NullPointerException if {@code delimiter} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceBeforeLast(String delimiter, String replacement,
            String missingDelimiterValue) {
        java.util.Objects.requireNonNull(replacement, "replacement");
        int at = lastIndexOf(delimiter, length() - 1);
        return at < 0 ? missingDelimiterValue : spliced(0, at, replacement);
    }

    /**
     * Returns this string with the part after the last occurrence of {@code delimiter} that
     * starts no later than the last character replaced by {@code replacement}, or this string
     * when there is none (so an empty delimiter is found at the last character, and not at all in
     * an empty string).
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter     the delimiter.
     * @param   replacement   the text put in place of the part.
     * @return  the replaced text, or this string when nothing changes.
     * @throws  NullPointerException if {@code delimiter} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceAfterLast(String delimiter, String replacement) {
        return replaceAfterLast(delimiter, replacement, this);
    }

    /**
     * Returns this string with the part after the last occurrence of {@code delimiter} that
     * starts no later than the last character replaced by {@code replacement}, or
     * {@code missingDelimiterValue} when there is none (so an empty delimiter is found at the
     * last character, and not at all in an empty string).
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiter               the delimiter.
     * @param   replacement             the text put in place of the part.
     * @param   missingDelimiterValue   the answer when the delimiter does not occur.
     * @return  the replaced text (this string when nothing changes), or
     *          {@code missingDelimiterValue}.
     * @throws  NullPointerException if {@code delimiter} or {@code replacement} is {@code null}.
     * @since   21
     */
    public String replaceAfterLast(String delimiter, String replacement,
            String missingDelimiterValue) {
        java.util.Objects.requireNonNull(replacement, "replacement");
        int at = lastIndexOf(delimiter, length() - 1);
        return at < 0 ? missingDelimiterValue
                : spliced(at + delimiter.length(), length(), replacement);
    }

    /**
     * Returns this string with each character (Unicode code point) mapped through {@code table},
     * as Python's {@code str.translate} maps it: a code point that is not a key is kept, one whose
     * key maps to {@code null} is removed, and any other is replaced by the string it maps to. A
     * surrogate pair is one code point; a lone surrogate is a code point of its own.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   table   the mapping from code points to their replacements.
     * @return  the translated text, or this string when nothing changes.
     * @throws  NullPointerException if {@code table} is {@code null}.
     * @since   21
     */
    public String translate(java.util.Map<Integer, String> table) {
        java.util.Objects.requireNonNull(table, "table");
        int length = length();
        Splicer edits = new Splicer(this);
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; ) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            int count = to - from;
            if (to < length && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;
            for (int at = 0; at < count; ) {
                int point = Character.codePointAt(window, at, count);
                int width = Character.charCount(point);
                String mapped = table.get(point);
                boolean kept = mapped == null ? !table.containsKey(point)
                        : mapped.length() == width && mapped.charAt(0) == window[at]
                                && (width == 1 || mapped.charAt(1) == window[at + 1]);
                if (!kept) edits.edit(from + at, from + at + width, mapped == null ? "" : mapped);
                at += width;
            }
            from += count;
        }
        String result = edits.result();
        return result.length() == length && result.equals(this) ? this : result;
    }

    /**
     * This string with the units from {@code start} to {@code end} replaced by
     * {@code replacement}: this string when that changes nothing, a range of it when the
     * replacement is empty and the part touches an end, else the replacement between the ranges
     * before and after the part.
     */
    private String spliced(int start, int end, String replacement) {
        int length = length();
        int count = replacement.length();
        if (end - start == count && regionMatches(start, replacement, 0, count)) return this;
        if (start == 0 && end == length) return replacement;
        if (count == 0 && start == 0) return substring(end);
        if (count == 0 && end == length) return substring(0, start);
        if (m3() != null) return substring(0, start).concat(replacement).concat(substring(end));
        return new StringBuilder(length - (end - start) + count).append(this, 0, start)
                .append(replacement).append(this, end, length).toString();
    }

    /**
     * The edits of a builder method, given from left to right: one edit makes a
     * {@link #spliced(int, int, String)} answer; more are written into one builder, with the
     * ranges of the text between them.
     */
    private static final class Splicer {
        private final String text;
        private StringBuilder out;
        private int copied;
        private int start = -1;
        private int end;
        private String replacement;

        Splicer(String text) {
            this.text = text;
        }

        /** Replaces the units from {@code from} to {@code to}, after every earlier edit. */
        void edit(int from, int to, String with) {
            if (out == null && start < 0) {
                start = from;
                end = to;
                replacement = with;
                return;
            }
            if (out == null) {
                out = new StringBuilder(text.length()).append(text, 0, start).append(replacement);
                copied = end;
            }
            out.append(text, copied, from).append(with);
            copied = to;
        }

        /** Replaces the unit at {@code index} by {@code unit}, after every earlier edit. */
        void edit(int index, char unit) {
            if (out == null) {
                edit(index, index + 1, String.valueOf(unit));
                return;
            }
            out.append(text, copied, index).append(unit);
            copied = index + 1;
        }

        /** The text with every edit made. */
        String result() {
            if (out != null) return out.append(text, copied, text.length()).toString();
            return start < 0 ? text : text.spliced(start, end, replacement);
        }
    }

    /**
     * Returns this string with its first character changed to title case, as
     * {@link Character#toTitleCase(int)} maps it; the rest is unchanged. The string is read as
     * code points: a surrogate pair is one character, a lone surrogate is a character of its own.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string with a title-case first character; this string when it is empty or
     *          already starts with one.
     * @since   21
     */
    public String capitalize() {
        if (isEmpty()) return this;
        int first = codePointAt(0);
        return withFirst(first, Character.toTitleCase(first));
    }

    /**
     * Returns this string with its first character changed to lower case, as
     * {@link Character#toLowerCase(int)} maps it; the rest is unchanged. The string is read as
     * code points.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string with a lower-case first character; this string when it is empty or
     *          already starts with one.
     * @since   21
     */
    public String uncapitalize() {
        if (isEmpty()) return this;
        int first = codePointAt(0);
        return withFirst(first, Character.toLowerCase(first));
    }

    /** This string with its first code point {@code first} replaced by {@code point}. */
    private String withFirst(int first, int point) {
        if (point == first) return this;
        return new StringBuilder(length()).appendCodePoint(point)
                .append(this, Character.charCount(first), length()).toString();
    }

    /**
     * Returns this string with the case of each character swapped: an upper-case or title-case
     * character ({@link Character#isUpperCase(int)}, {@link Character#isTitleCase(int)}) becomes
     * lower case ({@link Character#toLowerCase(int)}), a lower-case character
     * ({@link Character#isLowerCase(int)}) becomes upper case ({@link Character#toUpperCase(int)});
     * every other character is unchanged. The string is read as code points, each mapped to one
     * code point, so swapping twice need not give this string back.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string with the case of its characters swapped.
     * @since   21
     */
    public String swapCase() {
        return mapCodePoints(MAP_SWAP, null, false);
    }

    /**
     * Returns this string with the case of each character swapped as {@link #swapCase()} does,
     * except that a lower-case character at the start of this string or right after a whitespace
     * character ({@link Character#isWhitespace(int)}) becomes title case
     * ({@link Character#toTitleCase(int)}).
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string with the case of its characters swapped, word starts in title case.
     * @since   21
     */
    public String swapCaseWords() {
        return mapCodePoints(MAP_SWAP_WORDS, null, false);
    }

    /**
     * Returns this string with the first character of each word changed to title case, as
     * {@link Character#toTitleCase(int)} maps it; words are separated by whitespace
     * ({@link Character#isWhitespace(int)}) and the rest of each word is unchanged. The string is
     * read as code points.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string with title-case word starts.
     * @since   21
     */
    public String capitalizeWords() {
        return mapCodePoints(MAP_CAPITALIZE, null, false);
    }

    /**
     * Returns this string with the first character of each word changed to title case; words
     * are separated by the given delimiters, and the rest of each word is unchanged. The string
     * is read as code points; the delimiters are the code points
     * {@link Character#codePointAt(char[], int)} reads at each index of {@code delimiters}, so an
     * empty array leaves only the first character of this string to change.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiters   the characters that separate words.
     * @return  this string with title-case word starts.
     * @throws  NullPointerException if {@code delimiters} is {@code null}.
     * @since   21
     */
    public String capitalizeWords(char... delimiters) {
        java.util.Objects.requireNonNull(delimiters, "delimiters");
        return mapCodePoints(MAP_CAPITALIZE, delimiters, false);
    }

    /**
     * Returns this string converted to lower case in {@link java.util.Locale#ROOT} (as
     * {@link #toLowerCase(java.util.Locale)} does), then with the first character of each word
     * changed to title case as
     * {@link #capitalizeWords()} does.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string in lower case with title-case word starts.
     * @since   21
     */
    public String capitalizeFully() {
        return toLowerCase(java.util.Locale.ROOT).mapCodePoints(MAP_CAPITALIZE, null, false);
    }

    /**
     * Returns this string converted to lower case in {@link java.util.Locale#ROOT} (as
     * {@link #toLowerCase(java.util.Locale)} does), then with the first character of each word
     * changed to title case as
     * {@link #capitalizeWords(char...)} does with the given delimiters.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiters   the characters that separate words.
     * @return  this string in lower case with title-case word starts.
     * @throws  NullPointerException if {@code delimiters} is {@code null}.
     * @since   21
     */
    public String capitalizeFully(char... delimiters) {
        java.util.Objects.requireNonNull(delimiters, "delimiters");
        return toLowerCase(java.util.Locale.ROOT).mapCodePoints(MAP_CAPITALIZE, delimiters, false);
    }

    /**
     * Returns this string with the first character of each word changed to lower case, as
     * {@link Character#toLowerCase(int)} maps it; words are separated by whitespace
     * ({@link Character#isWhitespace(int)}) and the rest of each word is unchanged. The string is
     * read as code points.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string with lower-case word starts.
     * @since   21
     */
    public String uncapitalizeWords() {
        return mapCodePoints(MAP_UNCAPITALIZE, null, false);
    }

    /**
     * Returns this string with the first character of each word changed to lower case; words
     * are separated by the given delimiters, read as {@link #capitalizeWords(char...)} reads
     * them, and the rest of each word is unchanged.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiters   the characters that separate words.
     * @return  this string with lower-case word starts.
     * @throws  NullPointerException if {@code delimiters} is {@code null}.
     * @since   21
     */
    public String uncapitalizeWords(char... delimiters) {
        java.util.Objects.requireNonNull(delimiters, "delimiters");
        return mapCodePoints(MAP_UNCAPITALIZE, delimiters, false);
    }

    /**
     * Returns the first character of each word of this string, unchanged; words are separated
     * by whitespace ({@link Character#isWhitespace(int)}). The string is read as code points.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  the initials of the words of this string.
     * @since   21
     */
    public String initials() {
        return mapCodePoints(MAP_INITIALS, null, false);
    }

    /**
     * Returns the first character of each word of this string, unchanged; words are separated
     * by the given delimiters, read as {@link #capitalizeWords(char...)} reads them. An empty
     * array gives the empty string.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   delimiters   the characters that separate words.
     * @return  the initials of the words of this string.
     * @throws  NullPointerException if {@code delimiters} is {@code null}.
     * @since   21
     */
    public String initials(char... delimiters) {
        java.util.Objects.requireNonNull(delimiters, "delimiters");
        return delimiters.length == 0 ? "" : mapCodePoints(MAP_INITIALS, delimiters, false);
    }

    /**
     * Returns this string in camel case: converted to lower case in
     * {@link java.util.Locale#ROOT}, with the space and the given delimiters removed, and the first
     * character after each removed run changed to title case ({@link Character#toTitleCase(int)}).
     * The first character of the result is in title case only when
     * {@code capitalizeFirstLetter}. Delimiters are read as {@link #capitalizeWords(char...)}
     * reads them and matched against the lower-case text.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   capitalizeFirstLetter   whether the first character is in title case.
     * @param   delimiters              the characters that separate words besides the space.
     * @return  this string in camel case.
     * @throws  NullPointerException if {@code delimiters} is {@code null}.
     * @since   21
     */
    public String toCamelCase(boolean capitalizeFirstLetter, char... delimiters) {
        java.util.Objects.requireNonNull(delimiters, "delimiters");
        return toLowerCase(java.util.Locale.ROOT).mapCodePoints(MAP_CAMEL, delimiters,
                capitalizeFirstLetter);
    }

    /**
     * Returns this string with full Unicode case folding: each character replaced by its fold of
     * status C or F in {@code CaseFolding.txt} of Unicode 15.0.0, so {@code "\u00DF"} folds to
     * {@code "ss"}. The folding is the same in every locale (the Turkic folds of status T are not
     * applied) and the result is not normalized. The string is read as code points; a lone
     * surrogate is unchanged.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's {@code str.casefold}
     * has the same result.
     *
     * @return  this string case folded; this string when folding leaves it unchanged.
     * @since   21
     */
    public String caseFold() {
        return mapCodePoints(MAP_FOLD, null, false);
    }

    /**
     * Returns {@code true} if this string and {@code other} are equal after full Unicode case
     * folding, as {@code caseFold().equals(other.caseFold())} compares them, so {@code "\u00DF"}
     * equals {@code "SS"}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   other   the string to compare with.
     * @return  whether they are equal after case folding.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @since   21
     */
    public boolean equalsFoldCase(String other) {
        java.util.Objects.requireNonNull(other, "other");
        if (equals(other)) return true;
        if (bothAscii(other)) return equalsIgnoreAsciiCase(other);
        return caseFold().equals(other.caseFold());
    }

    /**
     * Returns {@code true} if this string and {@code other} have the same number of code points
     * and each two at the same position are equal under Unicode simple case folding (status C
     * and S in {@code CaseFolding.txt} of Unicode 15.0.0): {@code "k"}, {@code "K"} and the
     * Kelvin sign {@code "\u212A"} are equal, {@code "\u00DF"} and {@code "ss"} are not. A lone
     * surrogate equals only itself.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Go's
     * {@code strings.EqualFold} has the same result.
     *
     * @param   other   the string to compare with.
     * @return  whether they are equal under simple case folding.
     * @throws  NullPointerException if {@code other} is {@code null}.
     * @since   21
     */
    public boolean equalsSimpleFold(String other) {
        int length = length();
        // a simple fold never leaves the plane of its code point, so equal strings are as long
        if (length != other.length()) return false;
        if (bothAscii(other)) return equalsIgnoreAsciiCase(other);
        char[] mine = new char[Math.min(length, SCAN_WINDOW)];
        char[] theirs = new char[mine.length];
        for (int from = 0; from < length; ) {
            int to = Math.min(length, from + mine.length);
            getChars(from, to, mine, 0);
            other.getChars(from, to, theirs, 0);
            int count = to - from;
            if (to < length && count > 1 && (Character.isHighSurrogate(mine[count - 1])
                    || Character.isHighSurrogate(theirs[count - 1]))) {
                count--;
            }
            for (int at = 0; at < count; ) {
                int a = Character.codePointAt(mine, at, count);
                int b = Character.codePointAt(theirs, at, count);
                if (a != b && CaseFolding.simpleFold(a) != CaseFolding.simpleFold(b)) return false;
                at += Character.charCount(a);
            }
            from += count;
        }
        return true;
    }

    /** Code point maps of {@link #mapCodePoints(int, char[], boolean)}. */
    private static final int MAP_SWAP = 0;
    private static final int MAP_SWAP_WORDS = 1;
    private static final int MAP_CAPITALIZE = 2;
    private static final int MAP_UNCAPITALIZE = 3;
    private static final int MAP_INITIALS = 4;
    private static final int MAP_CAMEL = 5;
    private static final int MAP_FOLD = 6;
    /** {@link #mapCodePoints(int, char[], boolean)}: the code point is dropped. */
    private static final int MAPPED_DROP = -1;
    /** {@link #mapCodePoints(int, char[], boolean)}: the code point becomes its full fold. */
    private static final int MAPPED_EXPAND = -2;

    /**
     * This string with each code point mapped by {@code mode}, the map of one case method:
     * words are split at {@code delimiters} (whitespace when {@code null}) and {@code first}
     * says whether camel case starts in title case. Prepared M3 facts answer when they show an
     * ASCII string without a character the map changes; otherwise the string is read in bulk
     * windows, a surrogate pair never split between two windows. Unchanged runs are copied as
     * ranges, and an unchanged string is returned as it is.
     */
    private String mapCodePoints(int mode, char[] delimiters, boolean first) {
        int gate = switch (mode) {
            case MAP_SWAP, MAP_SWAP_WORDS -> M3StringFacts.REGEX_ALPHA;
            case MAP_CAPITALIZE -> M3StringFacts.REGEX_LOWER;
            case MAP_UNCAPITALIZE, MAP_FOLD -> M3StringFacts.REGEX_UPPER;
            default -> 0;
        };
        if (gate != 0 && asciiWithout(gate)) return this;
        int length = length();
        StringBuilder out = null;
        int copied = 0;
        boolean start = mode != MAP_CAMEL;
        boolean emitted = false;
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; ) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            int count = to - from;
            if (to < length && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;
            for (int at = 0; at < count; ) {
                int point = Character.codePointAt(window, at, count);
                int mapped = point;
                switch (mode) {
                    case MAP_SWAP -> mapped = swappedCase(point, false);
                    case MAP_SWAP_WORDS -> {
                        mapped = swappedCase(point, start);
                        start = Character.isWhitespace(point);
                    }
                    case MAP_CAPITALIZE, MAP_UNCAPITALIZE -> {
                        if (isDelimiter(point, delimiters)) {
                            start = true;
                        } else if (start) {
                            mapped = mode == MAP_CAPITALIZE ? Character.toTitleCase(point)
                                    : Character.toLowerCase(point);
                            start = false;
                        }
                    }
                    case MAP_INITIALS -> {
                        boolean gap = isDelimiter(point, delimiters);
                        if (gap || !start) mapped = MAPPED_DROP;
                        start = gap;
                    }
                    case MAP_CAMEL -> {
                        if (point == ' ' || isDelimiter(point, delimiters)) {
                            mapped = MAPPED_DROP;
                            start = emitted;
                        } else {
                            if (start || !emitted && first) mapped = Character.toTitleCase(point);
                            start = false;
                            emitted = true;
                        }
                    }
                    default -> {
                        int folded = CaseFolding.fullFold(point);
                        mapped = folded < 0 ? MAPPED_EXPAND : folded;
                    }
                }
                int width = Character.charCount(point);
                if (mapped != point) {
                    if (out == null) out = new StringBuilder(length + 16);
                    out.append(this, copied, from + at);
                    if (mapped == MAPPED_EXPAND) {
                        CaseFolding.appendFullFold(out, point);
                    } else if (mapped != MAPPED_DROP) {
                        out.appendCodePoint(mapped);
                    }
                    copied = from + at + width;
                }
                at += width;
            }
            from += count;
        }
        return out == null ? this : out.append(this, copied, length).toString();
    }

    /**
     * {@code point} with its case swapped as {@link #swapCase()} swaps it; a lower-case one
     * becomes title case instead of upper case when {@code title}.
     */
    private static int swappedCase(int point, boolean title) {
        if (Character.isUpperCase(point) || Character.isTitleCase(point)) {
            return Character.toLowerCase(point);
        }
        if (Character.isLowerCase(point)) {
            return title ? Character.toTitleCase(point) : Character.toUpperCase(point);
        }
        return point;
    }

    /**
     * Whether {@code point} is one of the code points {@link Character#codePointAt(char[], int)}
     * reads at each index of {@code delimiters}, or a whitespace character
     * ({@link Character#isWhitespace(int)}) when they are {@code null}.
     */
    private static boolean isDelimiter(int point, char[] delimiters) {
        if (delimiters == null) return Character.isWhitespace(point);
        for (int index = 0; index < delimiters.length; index++) {
            if (Character.codePointAt(delimiters, index) == point) return true;
        }
        return false;
    }

    /** Whether prepared M3 facts show this string is ASCII without a unit of {@code regexClass}. */
    private boolean asciiWithout(int regexClass) {
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        return facts != null && facts.ascii && (facts.regexAnyClasses & regexClass) == 0;
    }

    /** Whether prepared M3 facts show this string and {@code other} are both ASCII. */
    private boolean bothAscii(String other) {
        M3String mine = m3();
        M3String theirs = other.m3();
        M3StringFacts a = mine == null ? null : mine.factsIfPrepared();
        M3StringFacts b = theirs == null ? null : theirs.factsIfPrepared();
        return a != null && b != null && a.ascii && b.ascii;
    }

    /**
     * Unicode 15.0.0 case folding as {@code CaseFolding.txt} gives it: the folds of status C and
     * S (simple folding) and of status F (full folding), built once. Generated from that file;
     * the Turkic folds (status T) are left out.
     */
    private static final class CaseFolding {
        /**
         * Runs of simple folds: first code point, count, stride, and the fold minus the code
         * point. A difference of 0 marks a code point whose only fold is a full one.
         */
        private static final int[] RUNS = {
            0x0041, 26, 1, 32, 0x00B5, 1, 1, 775, 0x00C0, 23, 1, 32, 0x00D8, 7, 1, 32,
            0x00DF, 1, 1, 0, 0x0100, 24, 2, 1, 0x0130, 1, 1, 0, 0x0132, 3, 2, 1,
            0x0139, 8, 2, 1, 0x0149, 1, 1, 0, 0x014A, 23, 2, 1, 0x0178, 1, 1, -121,
            0x0179, 3, 2, 1, 0x017F, 1, 1, -268, 0x0181, 1, 1, 210, 0x0182, 2, 2, 1,
            0x0186, 1, 1, 206, 0x0187, 1, 1, 1, 0x0189, 2, 1, 205, 0x018B, 1, 1, 1,
            0x018E, 1, 1, 79, 0x018F, 1, 1, 202, 0x0190, 1, 1, 203, 0x0191, 1, 1, 1,
            0x0193, 1, 1, 205, 0x0194, 1, 1, 207, 0x0196, 1, 1, 211, 0x0197, 1, 1, 209,
            0x0198, 1, 1, 1, 0x019C, 1, 1, 211, 0x019D, 1, 1, 213, 0x019F, 1, 1, 214,
            0x01A0, 3, 2, 1, 0x01A6, 1, 1, 218, 0x01A7, 1, 1, 1, 0x01A9, 1, 1, 218,
            0x01AC, 1, 1, 1, 0x01AE, 1, 1, 218, 0x01AF, 1, 1, 1, 0x01B1, 2, 1, 217,
            0x01B3, 2, 2, 1, 0x01B7, 1, 1, 219, 0x01B8, 1, 1, 1, 0x01BC, 1, 1, 1,
            0x01C4, 1, 1, 2, 0x01C5, 1, 1, 1, 0x01C7, 1, 1, 2, 0x01C8, 1, 1, 1,
            0x01CA, 1, 1, 2, 0x01CB, 9, 2, 1, 0x01DE, 9, 2, 1, 0x01F0, 1, 1, 0,
            0x01F1, 1, 1, 2, 0x01F2, 2, 2, 1, 0x01F6, 1, 1, -97, 0x01F7, 1, 1, -56,
            0x01F8, 20, 2, 1, 0x0220, 1, 1, -130, 0x0222, 9, 2, 1, 0x023A, 1, 1, 10795,
            0x023B, 1, 1, 1, 0x023D, 1, 1, -163, 0x023E, 1, 1, 10792, 0x0241, 1, 1, 1,
            0x0243, 1, 1, -195, 0x0244, 1, 1, 69, 0x0245, 1, 1, 71, 0x0246, 5, 2, 1,
            0x0345, 1, 1, 116, 0x0370, 2, 2, 1, 0x0376, 1, 1, 1, 0x037F, 1, 1, 116,
            0x0386, 1, 1, 38, 0x0388, 3, 1, 37, 0x038C, 1, 1, 64, 0x038E, 2, 1, 63,
            0x0390, 1, 1, 0, 0x0391, 17, 1, 32, 0x03A3, 9, 1, 32, 0x03B0, 1, 1, 0,
            0x03C2, 1, 1, 1, 0x03CF, 1, 1, 8, 0x03D0, 1, 1, -30, 0x03D1, 1, 1, -25,
            0x03D5, 1, 1, -15, 0x03D6, 1, 1, -22, 0x03D8, 12, 2, 1, 0x03F0, 1, 1, -54,
            0x03F1, 1, 1, -48, 0x03F4, 1, 1, -60, 0x03F5, 1, 1, -64, 0x03F7, 1, 1, 1,
            0x03F9, 1, 1, -7, 0x03FA, 1, 1, 1, 0x03FD, 3, 1, -130, 0x0400, 16, 1, 80,
            0x0410, 32, 1, 32, 0x0460, 17, 2, 1, 0x048A, 27, 2, 1, 0x04C0, 1, 1, 15,
            0x04C1, 7, 2, 1, 0x04D0, 48, 2, 1, 0x0531, 38, 1, 48, 0x0587, 1, 1, 0,
            0x10A0, 38, 1, 7264, 0x10C7, 1, 1, 7264, 0x10CD, 1, 1, 7264, 0x13F8, 6, 1, -8,
            0x1C80, 1, 1, -6222, 0x1C81, 1, 1, -6221, 0x1C82, 1, 1, -6212, 0x1C83, 2, 1, -6210,
            0x1C85, 1, 1, -6211, 0x1C86, 1, 1, -6204, 0x1C87, 1, 1, -6180, 0x1C88, 1, 1, 35267,
            0x1C90, 43, 1, -3008, 0x1CBD, 3, 1, -3008, 0x1E00, 75, 2, 1, 0x1E96, 5, 1, 0,
            0x1E9B, 1, 1, -58, 0x1E9E, 1, 1, -7615, 0x1EA0, 48, 2, 1, 0x1F08, 8, 1, -8,
            0x1F18, 6, 1, -8, 0x1F28, 8, 1, -8, 0x1F38, 8, 1, -8, 0x1F48, 6, 1, -8,
            0x1F50, 4, 2, 0, 0x1F59, 4, 2, -8, 0x1F68, 8, 1, -8, 0x1F80, 8, 1, 0,
            0x1F88, 8, 1, -8, 0x1F90, 8, 1, 0, 0x1F98, 8, 1, -8, 0x1FA0, 8, 1, 0,
            0x1FA8, 8, 1, -8, 0x1FB2, 3, 1, 0, 0x1FB6, 2, 1, 0, 0x1FB8, 2, 1, -8,
            0x1FBA, 2, 1, -74, 0x1FBC, 1, 1, -9, 0x1FBE, 1, 1, -7173, 0x1FC2, 3, 1, 0,
            0x1FC6, 2, 1, 0, 0x1FC8, 4, 1, -86, 0x1FCC, 1, 1, -9, 0x1FD2, 2, 1, 0,
            0x1FD6, 2, 1, 0, 0x1FD8, 2, 1, -8, 0x1FDA, 2, 1, -100, 0x1FE2, 3, 1, 0,
            0x1FE6, 2, 1, 0, 0x1FE8, 2, 1, -8, 0x1FEA, 2, 1, -112, 0x1FEC, 1, 1, -7,
            0x1FF2, 3, 1, 0, 0x1FF6, 2, 1, 0, 0x1FF8, 2, 1, -128, 0x1FFA, 2, 1, -126,
            0x1FFC, 1, 1, -9, 0x2126, 1, 1, -7517, 0x212A, 1, 1, -8383, 0x212B, 1, 1, -8262,
            0x2132, 1, 1, 28, 0x2160, 16, 1, 16, 0x2183, 1, 1, 1, 0x24B6, 26, 1, 26,
            0x2C00, 48, 1, 48, 0x2C60, 1, 1, 1, 0x2C62, 1, 1, -10743, 0x2C63, 1, 1, -3814,
            0x2C64, 1, 1, -10727, 0x2C67, 3, 2, 1, 0x2C6D, 1, 1, -10780, 0x2C6E, 1, 1, -10749,
            0x2C6F, 1, 1, -10783, 0x2C70, 1, 1, -10782, 0x2C72, 1, 1, 1, 0x2C75, 1, 1, 1,
            0x2C7E, 2, 1, -10815, 0x2C80, 50, 2, 1, 0x2CEB, 2, 2, 1, 0x2CF2, 1, 1, 1,
            0xA640, 23, 2, 1, 0xA680, 14, 2, 1, 0xA722, 7, 2, 1, 0xA732, 31, 2, 1,
            0xA779, 2, 2, 1, 0xA77D, 1, 1, -35332, 0xA77E, 5, 2, 1, 0xA78B, 1, 1, 1,
            0xA78D, 1, 1, -42280, 0xA790, 2, 2, 1, 0xA796, 10, 2, 1, 0xA7AA, 1, 1, -42308,
            0xA7AB, 1, 1, -42319, 0xA7AC, 1, 1, -42315, 0xA7AD, 1, 1, -42305, 0xA7AE, 1, 1, -42308,
            0xA7B0, 1, 1, -42258, 0xA7B1, 1, 1, -42282, 0xA7B2, 1, 1, -42261, 0xA7B3, 1, 1, 928,
            0xA7B4, 8, 2, 1, 0xA7C4, 1, 1, -48, 0xA7C5, 1, 1, -42307, 0xA7C6, 1, 1, -35384,
            0xA7C7, 2, 2, 1, 0xA7D0, 1, 1, 1, 0xA7D6, 2, 2, 1, 0xA7F5, 1, 1, 1,
            0xAB70, 80, 1, -38864, 0xFB00, 7, 1, 0, 0xFB13, 5, 1, 0, 0xFF21, 26, 1, 32,
            0x10400, 40, 1, 40, 0x104B0, 36, 1, 40, 0x10570, 19, 2, 39, 0x10571, 5, 2, 39,
            0x1057D, 7, 2, 39, 0x1058D, 3, 2, 39, 0x10595, 1, 1, 39, 0x10C80, 51, 1, 64,
            0x118A0, 32, 1, 32, 0x16E40, 32, 1, 32, 0x1E900, 34, 1, 34
        };
        /** Full folds: a code point and the two or three code points of its fold, 0 padded. */
        private static final int[] FULL_FOLDS = {
            0x00DF, 0x0073, 0x0073, 0x0000, 0x0130, 0x0069, 0x0307, 0x0000,
            0x0149, 0x02BC, 0x006E, 0x0000, 0x01F0, 0x006A, 0x030C, 0x0000,
            0x0390, 0x03B9, 0x0308, 0x0301, 0x03B0, 0x03C5, 0x0308, 0x0301,
            0x0587, 0x0565, 0x0582, 0x0000, 0x1E96, 0x0068, 0x0331, 0x0000,
            0x1E97, 0x0074, 0x0308, 0x0000, 0x1E98, 0x0077, 0x030A, 0x0000,
            0x1E99, 0x0079, 0x030A, 0x0000, 0x1E9A, 0x0061, 0x02BE, 0x0000,
            0x1E9E, 0x0073, 0x0073, 0x0000, 0x1F50, 0x03C5, 0x0313, 0x0000,
            0x1F52, 0x03C5, 0x0313, 0x0300, 0x1F54, 0x03C5, 0x0313, 0x0301,
            0x1F56, 0x03C5, 0x0313, 0x0342, 0x1F80, 0x1F00, 0x03B9, 0x0000,
            0x1F81, 0x1F01, 0x03B9, 0x0000, 0x1F82, 0x1F02, 0x03B9, 0x0000,
            0x1F83, 0x1F03, 0x03B9, 0x0000, 0x1F84, 0x1F04, 0x03B9, 0x0000,
            0x1F85, 0x1F05, 0x03B9, 0x0000, 0x1F86, 0x1F06, 0x03B9, 0x0000,
            0x1F87, 0x1F07, 0x03B9, 0x0000, 0x1F88, 0x1F00, 0x03B9, 0x0000,
            0x1F89, 0x1F01, 0x03B9, 0x0000, 0x1F8A, 0x1F02, 0x03B9, 0x0000,
            0x1F8B, 0x1F03, 0x03B9, 0x0000, 0x1F8C, 0x1F04, 0x03B9, 0x0000,
            0x1F8D, 0x1F05, 0x03B9, 0x0000, 0x1F8E, 0x1F06, 0x03B9, 0x0000,
            0x1F8F, 0x1F07, 0x03B9, 0x0000, 0x1F90, 0x1F20, 0x03B9, 0x0000,
            0x1F91, 0x1F21, 0x03B9, 0x0000, 0x1F92, 0x1F22, 0x03B9, 0x0000,
            0x1F93, 0x1F23, 0x03B9, 0x0000, 0x1F94, 0x1F24, 0x03B9, 0x0000,
            0x1F95, 0x1F25, 0x03B9, 0x0000, 0x1F96, 0x1F26, 0x03B9, 0x0000,
            0x1F97, 0x1F27, 0x03B9, 0x0000, 0x1F98, 0x1F20, 0x03B9, 0x0000,
            0x1F99, 0x1F21, 0x03B9, 0x0000, 0x1F9A, 0x1F22, 0x03B9, 0x0000,
            0x1F9B, 0x1F23, 0x03B9, 0x0000, 0x1F9C, 0x1F24, 0x03B9, 0x0000,
            0x1F9D, 0x1F25, 0x03B9, 0x0000, 0x1F9E, 0x1F26, 0x03B9, 0x0000,
            0x1F9F, 0x1F27, 0x03B9, 0x0000, 0x1FA0, 0x1F60, 0x03B9, 0x0000,
            0x1FA1, 0x1F61, 0x03B9, 0x0000, 0x1FA2, 0x1F62, 0x03B9, 0x0000,
            0x1FA3, 0x1F63, 0x03B9, 0x0000, 0x1FA4, 0x1F64, 0x03B9, 0x0000,
            0x1FA5, 0x1F65, 0x03B9, 0x0000, 0x1FA6, 0x1F66, 0x03B9, 0x0000,
            0x1FA7, 0x1F67, 0x03B9, 0x0000, 0x1FA8, 0x1F60, 0x03B9, 0x0000,
            0x1FA9, 0x1F61, 0x03B9, 0x0000, 0x1FAA, 0x1F62, 0x03B9, 0x0000,
            0x1FAB, 0x1F63, 0x03B9, 0x0000, 0x1FAC, 0x1F64, 0x03B9, 0x0000,
            0x1FAD, 0x1F65, 0x03B9, 0x0000, 0x1FAE, 0x1F66, 0x03B9, 0x0000,
            0x1FAF, 0x1F67, 0x03B9, 0x0000, 0x1FB2, 0x1F70, 0x03B9, 0x0000,
            0x1FB3, 0x03B1, 0x03B9, 0x0000, 0x1FB4, 0x03AC, 0x03B9, 0x0000,
            0x1FB6, 0x03B1, 0x0342, 0x0000, 0x1FB7, 0x03B1, 0x0342, 0x03B9,
            0x1FBC, 0x03B1, 0x03B9, 0x0000, 0x1FC2, 0x1F74, 0x03B9, 0x0000,
            0x1FC3, 0x03B7, 0x03B9, 0x0000, 0x1FC4, 0x03AE, 0x03B9, 0x0000,
            0x1FC6, 0x03B7, 0x0342, 0x0000, 0x1FC7, 0x03B7, 0x0342, 0x03B9,
            0x1FCC, 0x03B7, 0x03B9, 0x0000, 0x1FD2, 0x03B9, 0x0308, 0x0300,
            0x1FD3, 0x03B9, 0x0308, 0x0301, 0x1FD6, 0x03B9, 0x0342, 0x0000,
            0x1FD7, 0x03B9, 0x0308, 0x0342, 0x1FE2, 0x03C5, 0x0308, 0x0300,
            0x1FE3, 0x03C5, 0x0308, 0x0301, 0x1FE4, 0x03C1, 0x0313, 0x0000,
            0x1FE6, 0x03C5, 0x0342, 0x0000, 0x1FE7, 0x03C5, 0x0308, 0x0342,
            0x1FF2, 0x1F7C, 0x03B9, 0x0000, 0x1FF3, 0x03C9, 0x03B9, 0x0000,
            0x1FF4, 0x03CE, 0x03B9, 0x0000, 0x1FF6, 0x03C9, 0x0342, 0x0000,
            0x1FF7, 0x03C9, 0x0342, 0x03B9, 0x1FFC, 0x03C9, 0x03B9, 0x0000,
            0xFB00, 0x0066, 0x0066, 0x0000, 0xFB01, 0x0066, 0x0069, 0x0000,
            0xFB02, 0x0066, 0x006C, 0x0000, 0xFB03, 0x0066, 0x0066, 0x0069,
            0xFB04, 0x0066, 0x0066, 0x006C, 0xFB05, 0x0073, 0x0074, 0x0000,
            0xFB06, 0x0073, 0x0074, 0x0000, 0xFB13, 0x0574, 0x0576, 0x0000,
            0xFB14, 0x0574, 0x0565, 0x0000, 0xFB15, 0x0574, 0x056B, 0x0000,
            0xFB16, 0x057E, 0x0576, 0x0000, 0xFB17, 0x0574, 0x056D, 0x0000
        };
        /** {@link #LATIN1}: the fold of the unit is more than one code point. */
        static final char EXPANDS = (char) 0xFFFF;
        /** The code points with a fold, ascending. */
        static final int[] KEYS;
        /** The simple fold of each key. */
        static final int[] SIMPLE;
        /** The full fold of each key as {@code offset << 2 | length} in EXPANSIONS, or -1. */
        static final int[] FULL;
        /** The units of the full folds, all in the Basic Multilingual Plane. */
        static final char[] EXPANSIONS;
        /** The full fold of each Latin-1 unit, or {@link #EXPANDS}. */
        static final char[] LATIN1 = new char[256];

        static {
            int count = 0;
            for (int r = 1; r < RUNS.length; r += 4) {
                count += RUNS[r];
            }
            long[] entries = new long[count];
            int n = 0;
            for (int r = 0; r < RUNS.length; r += 4) {
                for (int k = 0; k < RUNS[r + 1]; k++) {
                    int point = RUNS[r] + k * RUNS[r + 2];
                    entries[n++] = (long) point << 32 | (point + RUNS[r + 3]);
                }
            }
            java.util.Arrays.sort(entries);
            KEYS = new int[count];
            SIMPLE = new int[count];
            FULL = new int[count];
            for (int i = 0; i < count; i++) {
                KEYS[i] = (int) (entries[i] >>> 32);
                SIMPLE[i] = (int) entries[i];
                FULL[i] = -1;
            }
            EXPANSIONS = new char[FULL_FOLDS.length / 4 * 3];
            int used = 0;
            for (int f = 0; f < FULL_FOLDS.length; f += 4) {
                int length = FULL_FOLDS[f + 3] == 0 ? 2 : 3;
                for (int k = 0; k < length; k++) {
                    EXPANSIONS[used + k] = (char) FULL_FOLDS[f + 1 + k];
                }
                FULL[find(FULL_FOLDS[f])] = used << 2 | length;
                used += length;
            }
            for (int unit = 0; unit < LATIN1.length; unit++) {
                int i = find(unit);
                LATIN1[unit] = i < 0 ? (char) unit : FULL[i] >= 0 ? EXPANDS : (char) SIMPLE[i];
            }
        }

        /** The index of {@code point} among the keys, or a negative value. */
        static int find(int point) {
            return java.util.Arrays.binarySearch(KEYS, point);
        }

        /** The simple fold of {@code point}. */
        static int simpleFold(int point) {
            int i = find(point);
            return i < 0 ? point : SIMPLE[i];
        }

        /** The full fold of {@code point} when it is one code point, else -1. */
        static int fullFold(int point) {
            if (point < LATIN1.length && LATIN1[point] != EXPANDS) return LATIN1[point];
            int i = find(point);
            return i < 0 ? point : FULL[i] < 0 ? SIMPLE[i] : -1;
        }

        /** Appends the full fold of {@code point}, which is more than one code point. */
        static void appendFullFold(StringBuilder out, int point) {
            int code = FULL[find(point)];
            out.append(EXPANSIONS, code >>> 2, code & 3);
        }
    }

    /**
     * Returns this string with the characters of a Java string literal escaped, as commons-text
     * 1.15.0 {@code StringEscapeUtils.escapeJava} escapes them: a quotation mark and a backslash
     * get a backslash; backspace, tab, line feed, form feed and carriage return become
     * {@code \b}, {@code \t}, {@code \n}, {@code \f} and {@code \r}; every other unit below
     * {@code U+0020} or above {@code U+007F} becomes {@code \u005Cu} and four upper-case hex
     * digits, so a surrogate pair becomes two such escapes. {@code U+007F} is not escaped.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string escaped; this string when nothing in it needs escaping.
     * @since   21
     */
    public String escapeJava() {
        return escapedUnits(ESC_JAVA);
    }

    /**
     * Returns this string with Java escapes translated, as commons-text 1.15.0
     * {@code StringEscapeUtils.unescapeJava} translates them, from left to right: a backslash and
     * one to three octal digits give the unit of that value (three digits only when the first is
     * {@code 0} to {@code 3}); a backslash, one or more {@code u}, an optional {@code +} and four
     * characters give the unit {@link Integer#parseInt(CharSequence, int, int, int)} reads from
     * those four characters in radix 16, as a {@code char}; {@code \b}, {@code \t}, {@code \n},
     * {@code \f} and {@code \r} give backspace, tab, line feed, form feed and carriage return; a
     * backslash before a backslash, a quotation mark or an apostrophe gives that character; any
     * other backslash is removed.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. Unlike
     * {@link #translateEscapes()}, it reads unicode escapes and removes a backslash it does not
     * know instead of rejecting it.
     *
     * @return  this string with its escapes translated; this string when it has no backslash.
     * @throws  IllegalArgumentException if fewer than four characters follow the {@code u} (and
     *          {@code +}) of a unicode escape, or they are not a number in radix 16.
     * @since   21
     */
    public String unescapeJava() {
        int at = firstBackslash();
        if (at < 0) return this;
        int length = length();
        StringBuilder out = new StringBuilder(length);
        int copied = 0;
        while (at >= 0) {
            out.append(this, copied, at);
            copied = at + appendJavaEscape(out, at, length);
            at = indexOf('\\', copied);
        }
        return out.append(this, copied, length).toString();
    }

    /**
     * Returns this string with the characters of an ECMAScript string literal escaped, as
     * commons-text 1.15.0 {@code StringEscapeUtils.escapeEcmaScript} escapes them: as
     * {@link #escapeJava()} does, and an apostrophe and a slash also get a backslash.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string escaped; this string when nothing in it needs escaping.
     * @since   21
     */
    public String escapeEcmaScript() {
        return escapedUnits(ESC_ECMASCRIPT);
    }

    /**
     * Returns this string with ECMAScript escapes translated, as commons-text 1.15.0
     * {@code StringEscapeUtils.unescapeEcmaScript} translates them: by the rules of
     * {@link #unescapeJava()}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string with its escapes translated; this string when it has no backslash.
     * @throws  IllegalArgumentException as {@link #unescapeJava()} throws it.
     * @since   21
     */
    public String unescapeEcmaScript() {
        return unescapeJava();
    }

    /**
     * Returns this string with the characters of a JSON string escaped, as commons-text 1.15.0
     * {@code StringEscapeUtils.escapeJson} escapes them: a quotation mark, a backslash and a slash
     * get a backslash; backspace, tab, line feed, form feed and carriage return become
     * {@code \b}, {@code \t}, {@code \n}, {@code \f} and {@code \r}; every other unit below
     * {@code U+0020} or above {@code U+007E} becomes {@code \u005Cu} and four upper-case hex
     * digits.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string escaped; this string when nothing in it needs escaping.
     * @since   21
     */
    public String escapeJson() {
        return escapedUnits(ESC_JSON);
    }

    /**
     * Returns this string with JSON escapes translated, as commons-text 1.15.0
     * {@code StringEscapeUtils.unescapeJson} translates them: by the rules of
     * {@link #unescapeJava()}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string with its escapes translated; this string when it has no backslash.
     * @throws  IllegalArgumentException as {@link #unescapeJava()} throws it.
     * @since   21
     */
    public String unescapeJson() {
        return unescapeJava();
    }

    /**
     * Returns this string as one CSV field, as commons-text 1.15.0
     * {@code StringEscapeUtils.escapeCsv} writes it: when it holds a comma, a quotation mark, a
     * carriage return or a line feed, it is enclosed in quotation marks and each quotation mark
     * in it is doubled; otherwise it is unchanged.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string as a CSV field; this string when it needs no quotes.
     * @since   21
     */
    public String escapeCsv() {
        return escapedUnits(ESC_CSV);
    }

    /**
     * Returns the value of this string read as one CSV field, as commons-text 1.15.0
     * {@code StringEscapeUtils.unescapeCsv} reads it: when it is at least two characters long and
     * starts and ends with a quotation mark, those two are removed, and two quotation marks in a
     * row in the rest become one when the rest holds a comma, a quotation mark, a carriage return
     * or a line feed; otherwise it is unchanged.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. The donor fails with an
     * exception on a lone quotation mark; this method returns it unchanged.
     *
     * @return  the field value; this string when it is not enclosed in quotation marks.
     * @since   21
     */
    public String unescapeCsv() {
        int length = length();
        if (length < 2 || charAt(0) != '"' || charAt(length - 1) != '"') return this;
        String inner = substring(1, length - 1);
        return inner.indexOfAny(',', '"', '\r', '\n') < 0 ? inner : inner.replace("\"\"", "\"");
    }

    /**
     * Returns this string escaped for an XSI (POSIX) shell word, as commons-text 1.15.0
     * {@code StringEscapeUtils.escapeXSI} escapes it: each of the characters {@code |&;<>()$`},
     * backslash, quotation mark, apostrophe, space, tab and {@code *?[#~=%} gets a backslash; a
     * line feed and a carriage return followed by a line feed are removed. A carriage return
     * before any other character is unchanged.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string escaped; this string when nothing in it needs escaping.
     * @since   21
     */
    public String escapeXSI() {
        return escapedUnits(ESC_XSI);
    }

    /**
     * Returns this string with XSI (POSIX) shell escapes translated, as commons-text 1.15.0
     * {@code StringEscapeUtils.unescapeXSI} translates them: each backslash that is not itself
     * escaped is removed, and the character after it is kept as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string with its escapes translated; this string when it has no backslash.
     * @since   21
     */
    public String unescapeXSI() {
        int at = firstBackslash();
        if (at < 0) return this;
        int length = length();
        StringBuilder out = new StringBuilder(length);
        int copied = 0;
        while (at >= 0) {
            out.append(this, copied, at);
            copied = at + 1;
            at = indexOf('\\', at + 2);
        }
        return out.append(this, copied, length).toString();
    }

    /**
     * Returns this string escaped for a CSS string literal, as unbescape 1.1.8
     * {@code CssEscape.escapeCssString} escapes it at its default level (backslash escapes, else
     * compact hex; level 2: basic escape set and all non-ASCII characters): a quotation mark,
     * an apostrophe, a backslash, a slash and a semicolon get a backslash; {@code &}, {@code <},
     * {@code >}, the units below {@code U+0020}, {@code U+007F} and every character above it
     * become a backslash and the upper-case hex digits of the code point without leading zeros,
     * followed by a space when the next unit is an ASCII hex digit or a space. The string is read
     * as code points; a lone surrogate is a character of its own.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string escaped; this string when nothing in it needs escaping.
     * @since   21
     */
    public String escapeCssString() {
        return escapedUnits(ESC_CSS_STRING);
    }

    /**
     * Returns this string escaped as a CSS identifier, as unbescape 1.1.8
     * {@code CssEscape.escapeCssIdentifier} escapes it at its default level (backslash escapes,
     * else compact hex; level 2: basic escape set and all non-ASCII characters): the space and
     * ASCII punctuation get a backslash, except {@code &}, {@code :}, {@code <} and {@code >},
     * which become a backslash and the upper-case hex digits of the code point without leading
     * zeros, followed by a space when the next unit is an ASCII hex digit, as do the units below
     * {@code U+0020}, {@code U+007F} and every character above it. A hyphen-minus is escaped
     * only as the first character, when it is the only one or a hyphen-minus or a digit follows
     * it; a low line only as the first character; a digit as the first character is escaped in
     * hex. The string is read as code points.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string escaped; this string when nothing in it needs escaping.
     * @since   21
     */
    public String escapeCssIdentifier() {
        return escapedUnits(ESC_CSS_IDENTIFIER);
    }

    /**
     * Returns this string with CSS escapes translated, as unbescape 1.1.8
     * {@code CssEscape.unescapeCss} translates them, from left to right: a backslash before a
     * line feed, a form feed, a carriage return or a carriage return and line feed is removed
     * with them; a backslash and one to six ASCII hex digits, with one space, tab, line feed,
     * form feed, carriage return or carriage return and line feed after them removed, give the
     * code point of that value (a high surrogate value followed by such an escape of a low
     * surrogate value gives their pair; zero, a surrogate value or a value above
     * {@code U+10FFFF} gives {@code U+FFFD}); a backslash before any other unit gives that unit.
     * A backslash at the end is kept.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string with its escapes translated; this string when it has no escape.
     * @since   21
     */
    public String unescapeCss() {
        int at = firstBackslash();
        int length = length();
        StringBuilder out = null;
        int copied = 0;
        while (at >= 0 && at + 1 < length) {
            if (out == null) out = new StringBuilder(length);
            out.append(this, copied, at);
            char next = charAt(at + 1);
            if (next == '\n' || next == '\f') {
                copied = at + 2;
            } else if (next == '\r') {
                copied = at + 2 < length && charAt(at + 2) == '\n' ? at + 3 : at + 2;
            } else if (isAsciiHexDigit(next)) {
                copied = appendCssHexEscape(out, at + 1, length);
            } else {
                out.append(next);
                copied = at + 2;
            }
            at = indexOf('\\', copied);
        }
        return out == null ? this : out.append(this, copied, length).toString();
    }

    /**
     * Returns this string escaped as the value of a {@code .properties} entry, as unbescape 1.1.8
     * {@code PropertiesEscape.escapePropertiesValue} escapes it at its default level (level 2:
     * basic escape set and all non-ASCII characters): a backslash gets a backslash; tab, line
     * feed, form feed and carriage return become {@code \t}, {@code \n}, {@code \f} and
     * {@code \r}; every other unit below {@code U+0020}, {@code U+007F} and every unit above it
     * become {@code \u005Cu} and four upper-case hex digits; a space, a colon or an equals sign
     * that starts this string gets a backslash.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string escaped; this string when nothing in it needs escaping.
     * @since   21
     */
    public String escapePropertiesValue() {
        return escapedUnits(ESC_PROPERTIES_VALUE);
    }

    /**
     * Returns this string escaped as the key of a {@code .properties} entry, as unbescape 1.1.8
     * {@code PropertiesEscape.escapePropertiesKey} escapes it at its default level (level 2:
     * basic escape set and all non-ASCII characters): a space, {@code !}, {@code #}, {@code :},
     * {@code =} and a backslash get a backslash; tab, line feed, form feed and carriage return
     * become {@code \t}, {@code \n}, {@code \f} and {@code \r}; every other unit below
     * {@code U+0020}, {@code U+007F} and every unit above it become {@code \u005Cu} and four
     * upper-case hex digits.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @return  this string escaped; this string when nothing in it needs escaping.
     * @since   21
     */
    public String escapePropertiesKey() {
        return escapedUnits(ESC_PROPERTIES_KEY);
    }

    /**
     * Returns this string with {@code .properties} escapes translated, as unbescape 1.1.8
     * {@code PropertiesEscape.unescapeProperties} translates them, from left to right:
     * {@code \t}, {@code \n}, {@code \f} and {@code \r} give tab, line feed, form feed and
     * carriage return; {@code \u005Cu} and four ASCII hex digits give the unit of that value; a
     * {@code \u005Cu} with fewer hex digits is kept as it is; a backslash before any other unit
     * gives that unit. A backslash at the end is kept.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. Unlike
     * {@link java.util.Properties#load(java.io.Reader)}, it never rejects a malformed escape.
     *
     * @return  this string with its escapes translated; this string when it has no escape.
     * @since   21
     */
    public String unescapeProperties() {
        int at = firstBackslash();
        int length = length();
        StringBuilder out = null;
        int copied = 0;
        while (at >= 0 && at + 1 < length) {
            char next = charAt(at + 1);
            int unit = switch (next) {
                case 't' -> '\t';
                case 'n' -> '\n';
                case 'f' -> '\f';
                case 'r' -> '\r';
                default -> next;
            };
            int end = at + 2;
            if (next == 'u') {
                if (asciiHexRunEnd(at + 2, Math.min(length, at + 6)) < at + 6) {
                    at = indexOf('\\', at + 2);
                    continue;
                }
                unit = Integer.parseInt(this, at + 2, at + 6, 16);
                end = at + 6;
            }
            if (out == null) out = new StringBuilder(length);
            out.append(this, copied, at).append((char) unit);
            copied = end;
            at = indexOf('\\', end);
        }
        return out == null ? this : out.append(this, copied, length).toString();
    }

    /**
     * Returns this string, the body of a string literal of a template language, with the escapes
     * of that language translated, as the language's own lexer translates them.
     * {@link EscapeSyntax#FTL}: as FreeMarker 2.3.35 {@code StringUtil.FTLStringLiteralDec};
     * {@link EscapeSyntax#ST}: as StringTemplate 4.3.4 {@code STLexer} reads a string in an
     * expression. The body is read from left to right; {@link #translateEscapes()} reads Java's
     * own escapes.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API.
     *
     * @param   syntax   the language whose escapes are translated.
     * @return  this string with its escapes translated; this string when it has no backslash.
     * @throws  IllegalArgumentException if the body ends in a backslash that escapes nothing, or
     *          (FTL) an escape is not one of the language's.
     * @throws  NullPointerException if {@code syntax} is {@code null}.
     * @since   21
     */
    public String translateEscapes(EscapeSyntax syntax) {
        java.util.Objects.requireNonNull(syntax, "syntax");
        int at = firstBackslash();
        if (at < 0) return this;
        int length = length();
        StringBuilder out = new StringBuilder(length);
        int copied = 0;
        while (at >= 0) {
            out.append(this, copied, at);
            if (at + 1 == length) {
                throw new IllegalArgumentException("The literal ends in a backslash");
            }
            char next = charAt(at + 1);
            copied = syntax == EscapeSyntax.FTL ? appendFtlEscape(out, next, at, length) : at + 2;
            if (syntax == EscapeSyntax.ST) {
                out.append(next == 'n' ? '\n' : next == 'r' ? '\r' : next == 't' ? '\t' : next);
            }
            at = indexOf('\\', copied);
        }
        return out.append(this, copied, length).toString();
    }

    /**
     * The string-literal escape syntaxes of template languages that
     * {@link String#translateEscapes(EscapeSyntax)} translates.
     *
     * @apiNote
     * This type is an M3JDK addition to the {@code String} API.
     *
     * @since   21
     */
    public enum EscapeSyntax {
        /**
         * FreeMarker (FTL) string literals: a backslash before a quotation mark, an apostrophe, a
         * backslash, a left curly bracket or {@code =} gives that character; {@code \n},
         * {@code \r}, {@code \t}, {@code \f} and {@code \b} give line feed, carriage return, tab,
         * form feed and backspace; {@code \l}, {@code \g} and {@code \a} give {@code <},
         * {@code >} and {@code &}; {@code \x} and one to four ASCII hex digits give the unit of
         * that value; any other escape is rejected.
         */
        FTL,
        /**
         * StringTemplate 4 string literals in expressions: {@code \n}, {@code \r} and {@code \t}
         * give line feed, carriage return and tab; a backslash before any other unit gives that
         * unit.
         */
        ST
    }

    /**
     * Appends the translation of the FTL escape at {@code at}, a backslash followed by
     * {@code next}, as {@link #translateEscapes(EscapeSyntax)} reads it; returns the index after
     * it.
     */
    private int appendFtlEscape(StringBuilder out, char next, int at, int length) {
        int unit = switch (next) {
            case '"', '\'', '\\', '{', '=' -> next;
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'f' -> '\f';
            case 'b' -> '\b';
            case 'g' -> '>';
            case 'l' -> '<';
            case 'a' -> '&';
            default -> -1;
        };
        if (unit >= 0) {
            out.append((char) unit);
            return at + 2;
        }
        if (next != 'x') {
            throw new IllegalArgumentException("Invalid escape sequence (\\" + next
                    + ") in a string literal");
        }
        int end = asciiHexRunEnd(at + 2, Math.min(length, at + 6));
        if (end == at + 2) {
            throw new IllegalArgumentException("Invalid \\x escape in a string literal");
        }
        out.append((char) Integer.parseInt(this, at + 2, end, 16));
        return end;
    }

    /** Escape rules of {@link #escapedUnits(int)}, the rows of {@link EscapeRules}. */
    private static final int ESC_JAVA = 0;
    private static final int ESC_ECMASCRIPT = 1;
    private static final int ESC_JSON = 2;
    private static final int ESC_XSI = 3;
    private static final int ESC_CSS_STRING = 4;
    private static final int ESC_CSS_IDENTIFIER = 5;
    private static final int ESC_PROPERTIES_VALUE = 6;
    private static final int ESC_PROPERTIES_KEY = 7;
    private static final int ESC_CSV = 8;
    /** Units after the last one each window of {@link #escapedUnits(int)} decides. */
    private static final int ESC_LOOKAHEAD = 2;

    /**
     * This string escaped by {@code rule}. Prepared M3 facts answer when they show a string of
     * ASCII letters, digits and low lines that the rule leaves alone; otherwise the string is
     * read in bulk windows that keep {@link #ESC_LOOKAHEAD} units for the next window, so a
     * surrogate pair and the unit after it are never split from the unit that decides. The clean
     * runs between escapes are copied as ranges, and a string with nothing to escape is returned
     * as it is.
     */
    private String escapedUnits(int rule) {
        if (wordUnitsOnly() && (rule != ESC_CSS_IDENTIFIER || isEmpty()
                || Character.isLetter(charAt(0)))) {
            return this;
        }
        int length = length();
        StringBuilder out = null;
        int copied = 0;
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; ) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            int read = to - from;
            int count = to < length ? read - ESC_LOOKAHEAD : read;
            int at = 0;
            while (at < count) {
                char unit = window[at];
                int next = at + 1 < read ? window[at + 1] : -1;
                if (!escapesAt(rule, from + at, unit, next)) {
                    at++;
                    continue;
                }
                if (out == null) {
                    out = new StringBuilder(length + 16);
                    if (rule == ESC_CSV) out.append('"');
                }
                out.append(this, copied, from + at);
                int after = at + 2 < read ? window[at + 2] : -1;
                at += appendEscape(out, rule, unit, next, after);
                copied = from + at;
            }
            from += at;
        }
        if (out == null) return this;
        out.append(this, copied, length);
        return (rule == ESC_CSV ? out.append('"') : out).toString();
    }

    /**
     * Whether {@code rule} escapes {@code unit} at {@code index}, followed by the unit
     * {@code next} (-1 at the end).
     */
    private static boolean escapesAt(int rule, int index, char unit, int next) {
        if (unit >= 128) return (EscapeRules.FLAGS[rule] & EscapeRules.NON_ASCII) != 0;
        if (index == 0 && rule == ESC_CSS_IDENTIFIER) {
            if (unit == '-') return next < 0 || next == '-' || next >= '0' && next <= '9';
            if (unit == '_' || unit >= '0' && unit <= '9') return true;
        }
        if (index == 0 && rule == ESC_PROPERTIES_VALUE
                && (unit == ' ' || unit == ':' || unit == '=')) {
            return true;
        }
        if (rule == ESC_XSI && unit == '\r') return next == '\n';
        return inMask(unit, EscapeRules.ESCAPED_LOW[rule], EscapeRules.ESCAPED_HIGH[rule]);
    }

    /**
     * Appends the escape of {@code unit} under {@code rule}, the unit followed by {@code next}
     * and {@code after} (-1 past the end); returns the number of units it stands for.
     */
    private static int appendEscape(StringBuilder out, int rule, char unit, int next,
            int after) {
        if (rule == ESC_CSV) {
            out.append(unit);
            if (unit == '"') out.append('"');
            return 1;
        }
        if (rule == ESC_XSI && (unit == '\n' || unit == '\r')) return unit == '\r' ? 2 : 1;
        if (unit < 128 && inMask(unit, EscapeRules.BACKSLASHED_LOW[rule],
                EscapeRules.BACKSLASHED_HIGH[rule])) {
            out.append('\\').append(unit);
            return 1;
        }
        int flags = EscapeRules.FLAGS[rule];
        int letter = switch (unit) {
            case '\b' -> (flags & EscapeRules.SHORT_B) != 0 ? 'b' : 0;
            case '\t' -> (flags & EscapeRules.SHORT_TNFR) != 0 ? 't' : 0;
            case '\n' -> (flags & EscapeRules.SHORT_TNFR) != 0 ? 'n' : 0;
            case '\f' -> (flags & EscapeRules.SHORT_TNFR) != 0 ? 'f' : 0;
            case '\r' -> (flags & EscapeRules.SHORT_TNFR) != 0 ? 'r' : 0;
            default -> 0;
        };
        out.append('\\');
        if (letter != 0) {
            out.append((char) letter);
            return 1;
        }
        if ((flags & EscapeRules.CSS_HEX) == 0) {
            out.append('u');
            for (int shift = 12; shift >= 0; shift -= 4) out.append(upperHexDigit(unit >>> shift));
            return 1;
        }
        int point = unit;
        int width = 1;
        if (Character.isHighSurrogate(unit) && next >= 0 && Character.isLowSurrogate((char) next)) {
            point = Character.toCodePoint(unit, (char) next);
            width = 2;
            next = after;
        }
        int shift = 20;
        while (shift > 0 && point >>> shift == 0) shift -= 4;
        for (; shift >= 0; shift -= 4) out.append(upperHexDigit(point >>> shift));
        if (next >= 0 && (isAsciiHexDigit(next)
                || next == ' ' && (flags & EscapeRules.CSS_HEX_SPACE) != 0)) {
            out.append(' ');
        }
        return width;
    }

    /** The upper-case hex digit of the low four bits of {@code value}. */
    private static char upperHexDigit(int value) {
        int digit = value & 15;
        return (char) (digit < 10 ? '0' + digit : 'A' - 10 + digit);
    }

    /** Whether {@code unit} is {@code 0-9}, {@code A-F} or {@code a-f}. */
    private static boolean isAsciiHexDigit(int unit) {
        return unit >= '0' && unit <= '9' || (unit | 0x20) >= 'a' && (unit | 0x20) <= 'f';
    }

    /** The index after the run of ASCII hex digits from {@code from}, at most {@code max}. */
    private int asciiHexRunEnd(int from, int max) {
        int end = from;
        while (end < max && isAsciiHexDigit(charAt(end))) end++;
        return end;
    }

    /** Whether prepared M3 facts show this string is ASCII letters, digits and low lines only. */
    private boolean wordUnitsOnly() {
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        return facts != null && facts.ascii
                && (facts.regexAllClasses & M3StringFacts.REGEX_WORD) != 0;
    }

    /**
     * The index of the first backslash in this string, or -1; prepared M3 facts answer when they
     * show the string has none.
     */
    private int firstBackslash() {
        if (wordUnitsOnly()) return -1;
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        return facts != null && !facts.mayContainCodeUnit('\\') ? -1 : indexOf('\\');
    }

    /**
     * Appends the translation of the Java escape at {@code at}, a backslash, as
     * {@link #unescapeJava()} reads it; returns its length in units.
     */
    private int appendJavaEscape(StringBuilder out, int at, int length) {
        char next = at + 1 < length ? charAt(at + 1) : 0;
        if (next >= '0' && next <= '7') {
            int end = at + 2;
            if (end < length && charAt(end) >= '0' && charAt(end) <= '7') {
                end++;
                if (end < length && next <= '3' && charAt(end) >= '0' && charAt(end) <= '7') end++;
            }
            out.append((char) Integer.parseInt(this, at + 1, end, 8));
            return end - at;
        }
        if (next == 'u') {
            int start = at + 2;
            while (start < length && charAt(start) == 'u') start++;
            if (start < length && charAt(start) == '+') start++;
            if (start + 4 > length) {
                throw new IllegalArgumentException("Less than 4 hex digits in unicode value: '"
                        + substring(at) + "' due to end of CharSequence");
            }
            try {
                out.append((char) Integer.parseInt(this, start, start + 4, 16));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse unicode value: "
                        + substring(start, start + 4), e);
            }
            return start + 4 - at;
        }
        int unit = switch (next) {
            case 'b' -> '\b';
            case 't' -> '\t';
            case 'n' -> '\n';
            case 'f' -> '\f';
            case 'r' -> '\r';
            case '\\', '"', '\'' -> next;
            default -> -1;
        };
        if (unit < 0) return 1;
        out.append((char) unit);
        return 2;
    }

    /**
     * Appends the translation of the CSS hex escape whose digits start at {@code from}, as
     * {@link #unescapeCss()} reads it; returns the index after it.
     */
    private int appendCssHexEscape(StringBuilder out, int from, int length) {
        int end = asciiHexRunEnd(from, Math.min(length, from + 6));
        int point = Integer.parseInt(this, from, end, 16);
        end += cssWhitespaceAt(end, length);
        if (point >= 0xD800 && point <= 0xDBFF && end + 1 < length && charAt(end) == '\\'
                && isAsciiHexDigit(charAt(end + 1))) {
            int lowEnd = asciiHexRunEnd(end + 1, Math.min(length, end + 7));
            int low = Integer.parseInt(this, end + 1, lowEnd, 16);
            if (low >= 0xDC00 && low <= 0xDFFF) {
                point = Character.toCodePoint((char) point, (char) low);
                end = lowEnd + cssWhitespaceAt(lowEnd, length);
            }
        }
        if (point == 0 || point >= 0xD800 && point <= 0xDFFF || point > Character.MAX_CODE_POINT) {
            point = 0xFFFD;
        }
        out.appendCodePoint(point);
        return end;
    }

    /** The number of units of the CSS whitespace that ends a hex escape at {@code index}. */
    private int cssWhitespaceAt(int index, int length) {
        if (index >= length) return 0;
        char unit = charAt(index);
        if (unit == '\r') return index + 1 < length && charAt(index + 1) == '\n' ? 2 : 1;
        return unit == ' ' || unit == '\t' || unit == '\n' || unit == '\f' ? 1 : 0;
    }

    /**
     * The unit sets of the escape rules of {@link #escapedUnits(int)}, from the donor tables:
     * per rule its flags, the ASCII units it escapes besides those of its flags, and the ASCII
     * units it writes as a backslash and the unit; built once into two mask words each.
     */
    private static final class EscapeRules {
        /** The rule escapes the units {@code U+0000} to {@code U+001F}. */
        static final int CONTROLS = 1;
        /** The rule escapes {@code U+007F}. */
        static final int DELETE = 2;
        /** The rule escapes every unit above {@code U+007F}. */
        static final int NON_ASCII = 4;
        /** The rule writes a backspace as {@code \b}. */
        static final int SHORT_B = 8;
        /** The rule writes tab, line feed, form feed, carriage return as {@code \t \n \f \r}. */
        static final int SHORT_TNFR = 16;
        /** The rule writes compact CSS hex escapes of code points, not unicode escapes. */
        static final int CSS_HEX = 32;
        /** A CSS hex escape before a space is ended by a space too. */
        static final int CSS_HEX_SPACE = 64;
        /** The flags of each rule, by rule number. */
        static final int[] FLAGS = {
            CONTROLS | NON_ASCII | SHORT_B | SHORT_TNFR,
            CONTROLS | NON_ASCII | SHORT_B | SHORT_TNFR,
            CONTROLS | DELETE | NON_ASCII | SHORT_B | SHORT_TNFR,
            0,
            CONTROLS | DELETE | NON_ASCII | CSS_HEX | CSS_HEX_SPACE,
            CONTROLS | DELETE | NON_ASCII | CSS_HEX,
            CONTROLS | DELETE | NON_ASCII | SHORT_TNFR,
            CONTROLS | DELETE | NON_ASCII | SHORT_TNFR,
            0
        };
        /** The ASCII units each rule escapes besides those of its flags. */
        private static final String[] ESCAPED = {
            "\"\\",
            "\"\\'/",
            "\"\\/",
            "|&;<>()$`\\\"' \t\n*?[#~=%",
            "\"'\\/&;<>",
            " !\"#$%&'()*+,./:;<=>?@[\\]^`{|}~",
            "\\",
            " !#:=\\",
            ",\"\r\n"
        };
        /** The ASCII units each rule writes as a backslash and the unit. */
        private static final String[] BACKSLASHED = {
            "\"\\",
            "\"\\'/",
            "\"\\/",
            "|&;<>()$`\\\"' \t*?[#~=%",
            "\"'\\/;",
            " !\"#$%'()*+,-./;=?@[\\]^_`{|}~",
            "\\ :=",
            " !#:=\\",
            ""
        };
        static final long[] ESCAPED_LOW = new long[FLAGS.length];
        static final long[] ESCAPED_HIGH = new long[FLAGS.length];
        static final long[] BACKSLASHED_LOW = new long[FLAGS.length];
        static final long[] BACKSLASHED_HIGH = new long[FLAGS.length];

        static {
            for (int rule = 0; rule < FLAGS.length; rule++) {
                char[] escaped = ESCAPED[rule].toCharArray();
                char[] backslashed = BACKSLASHED[rule].toCharArray();
                ESCAPED_LOW[rule] = asciiMask(escaped, 0)
                        | ((FLAGS[rule] & CONTROLS) != 0 ? 0xFFFFFFFFL : 0L);
                ESCAPED_HIGH[rule] = asciiMask(escaped, 64)
                        | ((FLAGS[rule] & DELETE) != 0 ? 1L << 63 : 0L);
                BACKSLASHED_LOW[rule] = asciiMask(backslashed, 0);
                BACKSLASHED_HIGH[rule] = asciiMask(backslashed, 64);
            }
        }
    }

    /**
     * Returns this string percent-encoded as RFC 3986 section 2 defines it for data: each
     * unreserved character ({@code A-Z a-z 0-9 - . _ ~}) is kept, and every other character is
     * written as the {@code %HH} escapes of its UTF-8 bytes, with upper-case hex digits. The
     * string is read as code points: a surrogate pair is one character.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code urllib.parse.quote(s, safe='')} has the same result.
     * {@link java.net.URLEncoder} encodes HTML form data instead.
     *
     * @return  this string percent-encoded; this string when every character is unreserved.
     * @throws  IllegalArgumentException if this string holds a lone surrogate.
     * @since   21
     */
    public String encodePercent() {
        return percentEncoded(UNRESERVED_LOW, UNRESERVED_HIGH, false);
    }

    /**
     * Returns this string percent-encoded as {@link #encodePercent()} encodes it, except that
     * each ASCII character of {@code safe} is kept as well; a character of {@code safe} outside
     * ASCII is ignored.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code urllib.parse.quote(s, safe)} has the same result. With {@code safe} equal to
     * {@code "$&+:=@"} it is Go's {@code url.PathEscape}; with {@code "!$&'()*+,;=:@"} (the
     * characters of an RFC 3986 path segment) it is Guava's {@code urlPathSegmentEscaper()}, and
     * with {@code "!$&'()*+,;=:@/?"} its {@code urlFragmentEscaper()}.
     *
     * @param   safe   the characters to keep besides the unreserved ones.
     * @return  this string percent-encoded; this string when every character is kept.
     * @throws  NullPointerException if {@code safe} is {@code null}.
     * @throws  IllegalArgumentException if this string holds a lone surrogate.
     * @since   21
     */
    public String encodePercent(String safe) {
        char[] kept = java.util.Objects.requireNonNull(safe, "safe").toCharArray();
        return percentEncoded(UNRESERVED_LOW | asciiMask(kept, 0),
                UNRESERVED_HIGH | asciiMask(kept, 64), false);
    }

    /**
     * Returns this string percent-encoded as {@link #encodePercent()} encodes it, except that
     * each space {@code U+0020} becomes a plus sign {@code '+'}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code urllib.parse.quote_plus(s)} and Go's {@code url.QueryEscape} have the same result.
     * {@link java.net.URLEncoder} differs: it keeps {@code '*'} and encodes {@code '~'}.
     *
     * @return  this string percent-encoded with spaces as plus signs; this string when every
     *          character is unreserved.
     * @throws  IllegalArgumentException if this string holds a lone surrogate.
     * @since   21
     */
    public String encodePercentPlus() {
        return percentEncoded(UNRESERVED_LOW, UNRESERVED_HIGH, true);
    }

    /**
     * Returns this string percent-encoded as {@link #encodePercent(String)} encodes it with
     * {@code safe}, except that each space {@code U+0020} becomes a plus sign {@code '+'}, also
     * when {@code safe} holds a space.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code urllib.parse.quote_plus(s, safe)} has the same result.
     *
     * @param   safe   the characters to keep besides the unreserved ones.
     * @return  this string percent-encoded with spaces as plus signs; this string when every
     *          character is kept.
     * @throws  NullPointerException if {@code safe} is {@code null}.
     * @throws  IllegalArgumentException if this string holds a lone surrogate.
     * @since   21
     */
    public String encodePercentPlus(String safe) {
        char[] kept = java.util.Objects.requireNonNull(safe, "safe").toCharArray();
        return percentEncoded(UNRESERVED_LOW | asciiMask(kept, 0),
                UNRESERVED_HIGH | asciiMask(kept, 64), true);
    }

    /**
     * Returns this string with its percent escapes decoded: each {@code '%'} followed by two hex
     * digits of either case gives the byte they spell, and the bytes of each run of adjacent
     * escapes are decoded as UTF-8, each maximal ill-formed subpart (Unicode section 3.9)
     * replaced by one {@code U+FFFD}. A {@code '%'} not followed by two hex digits, and every
     * other character, is kept as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code urllib.parse.unquote(s)} has the same result. It never fails;
     * {@link #decodeUriComponent()} rejects a malformed escape instead.
     *
     * @return  this string with its escapes decoded; this string when it has none.
     * @since   21
     */
    public String decodePercent() {
        return percentDecoded(false);
    }

    /**
     * Returns this string with each plus sign {@code '+'} changed to a space {@code U+0020} and
     * its percent escapes then decoded as {@link #decodePercent()} decodes them, so an escape
     * {@code %2B} gives a plus sign.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code urllib.parse.unquote_plus(s)} has the same result. {@link java.net.URLDecoder}
     * differs: it rejects a {@code '%'} not followed by two hex digits.
     *
     * @return  this string with plus signs as spaces and its escapes decoded; this string when it
     *          has neither.
     * @since   21
     */
    public String decodePercentPlus() {
        return percentDecoded(true);
    }

    /**
     * Returns this string encoded as ECMAScript's {@code encodeURIComponent} encodes it: each
     * character of {@code A-Z a-z 0-9 - _ . ! ~ * ' ( )} is kept, and every other character is
     * written as the {@code %HH} escapes of its UTF-8 bytes, with upper-case hex digits. The
     * string is read as code points.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. Where ECMAScript throws a
     * {@code URIError} for a lone surrogate, this method throws an
     * {@code IllegalArgumentException}.
     *
     * @return  this string encoded as a URI component; this string when every character is kept.
     * @throws  IllegalArgumentException if this string holds a lone surrogate.
     * @since   21
     */
    public String encodeUriComponent() {
        return percentEncoded(URI_COMPONENT_LOW, URI_COMPONENT_HIGH, false);
    }

    /**
     * Returns this string encoded as ECMAScript's {@code encodeURI} encodes it: as
     * {@link #encodeUriComponent()} encodes it, except that the reserved characters
     * {@code ; / ? : @ & = + $ ,} and {@code #} are kept as well.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. Where ECMAScript throws a
     * {@code URIError} for a lone surrogate, this method throws an
     * {@code IllegalArgumentException}.
     *
     * @return  this string encoded as a URI; this string when every character is kept.
     * @throws  IllegalArgumentException if this string holds a lone surrogate.
     * @since   21
     */
    public String encodeUri() {
        return percentEncoded(URI_COMPONENT_LOW | URI_RESERVED_LOW,
                URI_COMPONENT_HIGH | URI_RESERVED_HIGH, false);
    }

    /**
     * Returns this string decoded as ECMAScript's {@code decodeURIComponent} decodes it: each
     * {@code '%'} must start a run of escapes {@code %HH} (two hex digits of either case) that
     * spells one character in UTF-8, which replaces the run; every other character is kept as
     * it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. Where ECMAScript throws a
     * {@code URIError}, this method throws an {@code IllegalArgumentException}.
     *
     * @return  this string with its escapes decoded; this string when it has none.
     * @throws  IllegalArgumentException if a {@code '%'} is not followed by two hex digits, or
     *          the escapes starting there do not spell a character in UTF-8 (a lone or
     *          overlong sequence, a surrogate, or a value above {@code U+10FFFF}).
     * @since   21
     */
    public String decodeUriComponent() {
        return uriDecoded(false);
    }

    /**
     * Returns this string decoded as ECMAScript's {@code decodeURI} decodes it: as
     * {@link #decodeUriComponent()} decodes it, except that an escape spelling one of the
     * reserved characters {@code ; / ? : @ & = + $ ,} or {@code #} is kept as it is written.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API. Where ECMAScript throws a
     * {@code URIError}, this method throws an {@code IllegalArgumentException}.
     *
     * @return  this string with its escapes decoded; this string when nothing is decoded.
     * @throws  IllegalArgumentException if a {@code '%'} is not followed by two hex digits, or
     *          the escapes starting there do not spell a character in UTF-8.
     * @since   21
     */
    public String decodeUri() {
        return uriDecoded(true);
    }

    /**
     * Returns this string with each lone surrogate replaced by the replacement character
     * {@code U+FFFD}; a surrogate pair is kept.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; ECMAScript's
     * {@code String.prototype.toWellFormed} has the same result.
     *
     * @return  this string without lone surrogates; this string when it has none.
     * @since   21
     */
    public String toWellFormed() {
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts != null && facts.latin1 || storage == null && isLatin1()) return this;
        int length = length();
        StringBuilder out = null;
        int copied = 0;
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; ) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            int count = to - from;
            if (to < length && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;
            for (int at = 0; at < count; at++) {
                char unit = window[at];
                if (!Character.isSurrogate(unit)) continue;
                if (Character.isHighSurrogate(unit) && at + 1 < count
                        && Character.isLowSurrogate(window[at + 1])) {
                    at++;
                    continue;
                }
                if (out == null) out = new StringBuilder(length);
                out.append(this, copied, from + at).append((char) 0xFFFD);
                copied = from + at + 1;
            }
            from += count;
        }
        return out == null ? this : out.append(this, copied, length).toString();
    }

    /**
     * Returns the number of bytes of this string in UTF-8: one for each character below
     * {@code U+0080}, two below {@code U+0800}, three for the rest of the Basic Multilingual
     * Plane and four for a surrogate pair.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Guava's
     * {@code Utf8.encodedLength} has the same result, and so has Python's
     * {@code len(s.encode('utf-8'))}, which also rejects a lone surrogate.
     *
     * @return  the length of this string in UTF-8.
     * @throws  IllegalArgumentException if this string holds a lone surrogate, or its length in
     *          UTF-8 is above {@link Integer#MAX_VALUE}.
     * @since   21
     */
    public int utf8Length() {
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        int length = length();
        if (facts != null && facts.ascii) return length;
        long bytes = length;
        if (storage == null && isLatin1()) {
            byte[] units = value;
            for (int index = 0; index < length; index++) {
                if (units[index] < 0) bytes++;
            }
            return utf8LengthOf(bytes);
        }
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; ) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            int count = to - from;
            if (to < length && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;
            for (int at = 0; at < count; at++) {
                char unit = window[at];
                if (unit < 0x80) continue;
                if (unit < 0x800 || !Character.isSurrogate(unit)) {
                    bytes += unit < 0x800 ? 1 : 2;
                } else if (Character.isHighSurrogate(unit) && at + 1 < count
                        && Character.isLowSurrogate(window[at + 1])) {
                    bytes += 2;
                    at++;
                } else {
                    throw unpairedSurrogate(from + at);
                }
            }
            from += count;
        }
        return utf8LengthOf(bytes);
    }

    /**
     * Returns this string with each ASCII letter moved 13 places along the alphabet, keeping its
     * case ({@code 'a'} and {@code 'n'} swap, as do {@code 'A'} and {@code 'N'}); every other
     * character is kept. Applying it twice gives this string back.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code codecs.encode(s, 'rot13')} has the same result.
     *
     * @return  this string in ROT13; this string when it has no ASCII letter.
     * @since   21
     */
    public String rot13() {
        if (asciiWithout(M3StringFacts.REGEX_ALPHA)) return this;
        int first = firstUnitIn(0L, ASCII_LETTERS_HIGH, false);
        if (first < 0) return this;
        char[] units = toCharArray();
        for (int index = first; index < units.length; index++) {
            int lower = units[index] | 0x20;
            if (lower >= 'a' && lower <= 'z') {
                units[index] = (char) (units[index] + (lower <= 'm' ? 13 : -13));
            }
        }
        return new String(units);
    }

    /** The RFC 3986 unreserved characters {@code A-Z a-z 0-9 - . _ ~}, units 0 to 63. */
    private static final long UNRESERVED_LOW = 0x03FF600000000000L;
    /** The RFC 3986 unreserved characters, units 64 to 127. */
    private static final long UNRESERVED_HIGH = 0x47FFFFFE87FFFFFEL;
    /** The characters {@code encodeURIComponent} keeps, {@code A-Z a-z 0-9 - _ . ! ~ * ' ( )}. */
    private static final long URI_COMPONENT_LOW = 0x03FF678200000000L;
    /** The characters {@code encodeURIComponent} keeps, units 64 to 127. */
    private static final long URI_COMPONENT_HIGH = 0x47FFFFFE87FFFFFEL;
    /** The reserved characters {@code ; / ? : @ & = + $ , #} of {@code encodeURI}. */
    private static final long URI_RESERVED_LOW = 0xAC00985800000000L;
    /** The reserved characters of {@code encodeURI}, units 64 to 127. */
    private static final long URI_RESERVED_HIGH = 0x0000000000000001L;
    /** The ASCII letters, units 64 to 127 (none is below 64). */
    private static final long ASCII_LETTERS_HIGH = 0x07FFFFFE07FFFFFEL;

    /**
     * The percent encoders: a unit in the ASCII set given by {@code kept} (units 0 to 63) and
     * {@code high} (units 64 to 127) is kept, except that a space becomes {@code '+'} when
     * {@code plus}; every other code point is written
     * as the upper-case {@code %HH} escapes of its UTF-8 bytes. Prepared M3 facts answer when
     * they show an ASCII string of word characters, which every set keeps. Otherwise the first
     * unit to encode is found on the bytes of a flat Latin-1 string or in bulk windows; the
     * clean prefix is copied as one range, and the rest is read in bulk windows, a surrogate pair
     * never split between two windows, kept runs copied as ranges.
     */
    private String percentEncoded(long kept, long high, boolean plus) {
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts != null && facts.ascii
                && (facts.regexAllClasses & M3StringFacts.REGEX_WORD) != 0) {
            return this;
        }
        long low = plus ? kept & ~(1L << ' ') : kept;
        int first = firstUnitIn(~low, ~high, true);
        if (first < 0) return this;
        int length = length();
        StringBuilder out = new StringBuilder(length + 16).append(this, 0, first);
        int copied = first;
        char[] window = new char[Math.min(length - first, SCAN_WINDOW)];
        for (int from = first; from < length; ) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            int count = to - from;
            if (to < length && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;
            for (int at = 0; at < count; at++) {
                char unit = window[at];
                if (unit < 0x80 && inMask(unit, low, high)) continue;
                out.append(this, copied, from + at);
                copied = from + at + 1;
                if (unit == ' ' && plus) {
                    out.append('+');
                } else if (!Character.isSurrogate(unit)) {
                    appendPercentUtf8(out, unit);
                } else if (Character.isHighSurrogate(unit) && at + 1 < count
                        && Character.isLowSurrogate(window[at + 1])) {
                    appendPercentUtf8(out, Character.toCodePoint(unit, window[++at]));
                    copied++;
                } else {
                    throw unpairedSurrogate(from + at);
                }
            }
            from += count;
        }
        return out.append(this, copied, length).toString();
    }

    /**
     * The index of the first unit of this string that is in the ASCII set given by {@code low}
     * and {@code high}, or outside ASCII when {@code wide}; -1 when there is none. A flat
     * Latin-1 string is read on its bytes, UTF-16 and M3 storage in bulk windows.
     */
    private int firstUnitIn(long low, long high, boolean wide) {
        int length = length();
        if (m3() == null && isLatin1()) {
            byte[] units = value;
            for (int index = 0; index < length; index++) {
                int unit = units[index] & 0xFF;
                if (unit < 0x80 ? inMask((char) unit, low, high) : wide) return index;
            }
            return -1;
        }
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; from += window.length) {
            int count = Math.min(window.length, length - from);
            getChars(from, from + count, window, 0);
            for (int at = 0; at < count; at++) {
                char unit = window[at];
                if (unit < 0x80 ? inMask(unit, low, high) : wide) return from + at;
            }
        }
        return -1;
    }

    /** Appends the upper-case {@code %HH} escapes of the UTF-8 bytes of {@code point}. */
    private static void appendPercentUtf8(StringBuilder out, int point) {
        if (point < 0x80) {
            appendPercentByte(out, point);
        } else if (point < 0x800) {
            appendPercentByte(out, 0xC0 | (point >> 6));
            appendPercentByte(out, 0x80 | (point & 0x3F));
        } else if (point < 0x10000) {
            appendPercentByte(out, 0xE0 | (point >> 12));
            appendPercentByte(out, 0x80 | ((point >> 6) & 0x3F));
            appendPercentByte(out, 0x80 | (point & 0x3F));
        } else {
            appendPercentByte(out, 0xF0 | (point >> 18));
            appendPercentByte(out, 0x80 | ((point >> 12) & 0x3F));
            appendPercentByte(out, 0x80 | ((point >> 6) & 0x3F));
            appendPercentByte(out, 0x80 | (point & 0x3F));
        }
    }

    /** Appends the escape {@code %HH} of the byte {@code value}, in upper-case hex. */
    private static void appendPercentByte(StringBuilder out, int value) {
        out.append('%').append(uppercaseHex(value >> 4)).append(uppercaseHex(value & 0xF));
    }

    /** The upper-case hex digit of {@code digit}, 0 to 15. */
    private static char uppercaseHex(int digit) {
        return (char) (digit < 10 ? '0' + digit : 'A' - 10 + digit);
    }

    /** The value of the ASCII hex digit {@code unit} of either case, or -1. */
    private static int hexDigitValue(char unit) {
        if (unit >= '0' && unit <= '9') return unit - '0';
        int lower = unit | 0x20;
        return lower >= 'a' && lower <= 'f' ? lower - 'a' + 10 : -1;
    }

    /** The byte of the escape {@code %HH} at {@code index}, or -1 when no escape starts there. */
    private int escapedByteAt(int index) {
        if (index + 2 >= length() || charAt(index) != '%') return -1;
        int high = hexDigitValue(charAt(index + 1));
        int low = hexDigitValue(charAt(index + 2));
        return high < 0 || low < 0 ? -1 : high << 4 | low;
    }

    /**
     * {@link #decodePercent()}, and {@link #decodePercentPlus()} when {@code plus}. Prepared M3
     * facts answer when they show no {@code '%'} (nor {@code '+'}); otherwise the next of each
     * is found with {@link #indexOf(int, int)}, the text between copied as one range. The bytes
     * of adjacent escapes are held and decoded together once another character or the end
     * follows; an ASCII character can never continue a UTF-8 sequence.
     */
    private String percentDecoded(boolean plus) {
        boolean percent = mayContainUnit('%');
        if (!percent && !(plus && mayContainUnit('+'))) return this;
        int length = length();
        int nextPercent = percent ? indexOf('%') : -1;
        int nextPlus = plus ? indexOf('+') : -1;
        StringBuilder out = null;
        byte[] held = null;
        int count = 0;
        int copied = 0;
        while (nextPercent >= 0 || nextPlus >= 0) {
            boolean space = nextPercent < 0 || nextPlus >= 0 && nextPlus < nextPercent;
            int at = space ? nextPlus : nextPercent;
            int value = space ? ' ' : escapedByteAt(at);
            int end = space ? at + 1 : at + 3;
            if (space) {
                nextPlus = indexOf('+', end);
            } else if (value < 0) {
                nextPercent = indexOf('%', at + 1);
                continue;
            } else {
                nextPercent = indexOf('%', end);
            }
            if (out == null) out = new StringBuilder(length);
            if (at > copied || value < 0x80) {
                count = heldBytesDecoded(out, held, count);
                out.append(this, copied, at);
            }
            if (value < 0x80) {
                out.append((char) value);
            } else {
                if (held == null) held = new byte[(length - at) / 3];
                held[count++] = (byte) value;
            }
            copied = end;
        }
        if (out == null) return this;
        heldBytesDecoded(out, held, count);
        return out.append(this, copied, length).toString();
    }

    /**
     * Appends the first {@code count} bytes of {@code held}, all at least {@code 0x80}, decoded
     * as UTF-8 with each maximal ill-formed subpart replaced by {@code U+FFFD}; returns 0.
     */
    private static int heldBytesDecoded(StringBuilder out, byte[] held, int count) {
        for (int at = 0; at < count; ) {
            int lead = held[at] & 0xFF;
            int need = lead < 0xC2 ? -1 : lead < 0xE0 ? 1 : lead < 0xF0 ? 2 : lead < 0xF5 ? 3 : -1;
            int next = at + 1;
            int point = lead & (0x3F >> Math.max(need, 0));
            int least = lead == 0xE0 ? 0xA0 : lead == 0xF0 ? 0x90 : 0x80;
            int most = lead == 0xED ? 0x9F : lead == 0xF4 ? 0x8F : 0xBF;
            boolean whole = need > 0;
            for (int k = 0; k < need; k++, next++) {
                int unit = next < count ? held[next] & 0xFF : -1;
                if (unit < (k == 0 ? least : 0x80) || unit > (k == 0 ? most : 0xBF)) {
                    whole = false;
                    break;
                }
                point = point << 6 | (unit & 0x3F);
            }
            if (whole) {
                out.appendCodePoint(point);
            } else {
                out.append((char) 0xFFFD);
            }
            at = next;
        }
        return 0;
    }

    /**
     * {@link #decodeUriComponent()}, and {@link #decodeUri()} when {@code keepReserved}: the
     * ECMAScript Decode operation. Prepared M3 facts answer when they show no {@code '%'};
     * otherwise each {@code '%'} is found with {@link #indexOf(int, int)} and the text between
     * escapes is copied as one range.
     */
    private String uriDecoded(boolean keepReserved) {
        if (!mayContainUnit('%')) return this;
        int length = length();
        StringBuilder out = null;
        int copied = 0;
        for (int at = indexOf('%'); at >= 0; ) {
            int lead = escapedByteAt(at);
            if (lead < 0) throw malformedEscape(at);
            int end = at + 3;
            int point = lead;
            if (lead < 0x80) {
                if (keepReserved && inMask((char) lead, URI_RESERVED_LOW, URI_RESERVED_HIGH)) {
                    at = indexOf('%', end);
                    continue;
                }
            } else {
                int bytes = Integer.numberOfLeadingZeros(~lead & 0xFF) - 24;
                if (bytes == 1 || bytes > 4) throw malformedEscape(at);
                point = lead & (0x7F >> bytes);
                for (int k = 1; k < bytes; k++, end += 3) {
                    int next = escapedByteAt(end);
                    if ((next & 0xC0) != 0x80) throw malformedEscape(at);
                    point = point << 6 | (next & 0x3F);
                }
                int least = bytes == 2 ? 0x80 : bytes == 3 ? 0x800 : 0x10000;
                if (point < least || point > Character.MAX_CODE_POINT
                        || bytes == 3 && Character.isSurrogate((char) point)) {
                    throw malformedEscape(at);
                }
            }
            if (out == null) out = new StringBuilder(length);
            out.append(this, copied, at).appendCodePoint(point);
            copied = end;
            at = indexOf('%', end);
        }
        return out == null ? this : out.append(this, copied, length).toString();
    }

    /** The exception for a lone surrogate at {@code index}. */
    private static IllegalArgumentException unpairedSurrogate(int index) {
        return new IllegalArgumentException("Unpaired surrogate at index " + index);
    }

    /** The exception for an escape at {@code index} that does not decode. */
    private static IllegalArgumentException malformedEscape(int index) {
        return new IllegalArgumentException("Malformed percent escape at index " + index);
    }

    /** {@code bytes} as the length of a string in UTF-8. */
    private static int utf8LengthOf(long bytes) {
        if (bytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("UTF-8 length does not fit in int: " + bytes);
        }
        return (int) bytes;
    }

    /**
     * Returns this string with each character that has a named character reference in HTML 3.2
     * replaced by that reference: {@code "} by {@code &quot;}, {@code &} by {@code &amp;},
     * {@code <} by {@code &lt;}, {@code >} by {@code &gt;} and each of the 96 characters
     * U+00A0..U+00FF by its ISO 8859-1 name (U+00E9 by {@code &eacute;}). Every other character,
     * the apostrophe included, is left as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Apache Commons Text's
     * {@code StringEscapeUtils.escapeHtml3} has the same result.
     *
     * @return  this string with HTML 3.2 named references; this string when it has no character
     *          to escape.
     * @since   21
     */
    public String escapeHtml3() {
        return escapeMarkup(MARKUP_HTML3);
    }

    /**
     * Returns this string with each character that has a named character reference in HTML 4.01
     * replaced by that reference: the characters {@link #escapeHtml3()} escapes and each of the
     * 152 symbols, Greek letters and special characters of HTML 4.01 (U+20AC by
     * {@code &euro;}, U+2329 by {@code &lang;}). Every other character, the apostrophe included,
     * is left as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Apache Commons Text's
     * {@code StringEscapeUtils.escapeHtml4} has the same result.
     *
     * @return  this string with HTML 4.01 named references; this string when it has no character
     *          to escape.
     * @since   21
     */
    public String escapeHtml4() {
        return escapeMarkup(MARKUP_HTML4);
    }

    /**
     * Returns this string with each HTML 3.2 character reference replaced by the character it
     * stands for. A named reference is {@code &}, one of the 100 names {@link #escapeHtml3()}
     * writes, and {@code ;}. A numeric reference is read as {@link #unescapeHtml4()} reads it.
     * Each reference is read once, from left to right; all other text is left as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Apache Commons Text's
     * {@code StringEscapeUtils.unescapeHtml3} has the same result.
     *
     * @return  this string with its references replaced; this string when it has none.
     * @throws  IllegalArgumentException if a numeric reference has a value above U+10FFFF that
     *          is not above {@link Integer#MAX_VALUE}.
     * @since   21
     */
    public String unescapeHtml3() {
        return unescapeMarkup(UNMARKUP_HTML3);
    }

    /**
     * Returns this string with each HTML 4.01 character reference replaced by the character it
     * stands for. A named reference is {@code &}, one of the 252 names {@link #escapeHtml4()}
     * writes, and {@code ;}. A numeric reference is {@code &#}, then decimal digits or
     * {@code x} or {@code X} and hexadecimal digits, then {@code ;}: a value up to U+FFFF gives
     * that {@code char}, a surrogate included, a larger one a surrogate pair, and a value above
     * {@link Integer#MAX_VALUE} leaves the reference as it is. Each reference is read once, from
     * left to right; all other text is left as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Apache Commons Text's
     * {@code StringEscapeUtils.unescapeHtml4} has the same result.
     *
     * @return  this string with its references replaced; this string when it has none.
     * @throws  IllegalArgumentException if a numeric reference has a value above U+10FFFF that
     *          is not above {@link Integer#MAX_VALUE}.
     * @since   21
     */
    public String unescapeHtml4() {
        return unescapeMarkup(UNMARKUP_HTML4);
    }

    /**
     * Returns this string escaped for XML 1.0: {@code "}, {@code &}, {@code '}, {@code <} and
     * {@code >} become {@code &quot;}, {@code &amp;}, {@code &apos;}, {@code &lt;} and
     * {@code &gt;}; U+007F..U+0084 and U+0086..U+009F become decimal references
     * ({@code &#127;}); the characters XML 1.0 does not allow are removed: U+0000..U+0008,
     * U+000B, U+000C, U+000E..U+001F, U+FFFE, U+FFFF and every unpaired surrogate. Every other
     * character is left as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Apache Commons Text's
     * {@code StringEscapeUtils.escapeXml10} has the same result.
     *
     * @return  this string escaped for XML 1.0; this string when it has no character to escape
     *          or remove.
     * @since   21
     */
    public String escapeXml10() {
        return escapeMarkup(MARKUP_XML | MARKUP_NAMED | 1 << MARKUP_LEVEL);
    }

    /**
     * Returns this string escaped for XML 1.1: {@code "}, {@code &}, {@code '}, {@code <} and
     * {@code >} become {@code &quot;}, {@code &amp;}, {@code &apos;}, {@code &lt;} and
     * {@code &gt;}; U+0001..U+0008, U+000B, U+000C, U+000E..U+001F, U+007F..U+0084 and
     * U+0086..U+009F become decimal references ({@code &#11;}); the characters XML 1.1 does not
     * allow are removed: U+0000, U+FFFE, U+FFFF and every unpaired surrogate. Every other
     * character is left as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Apache Commons Text's
     * {@code StringEscapeUtils.escapeXml11} has the same result.
     *
     * @return  this string escaped for XML 1.1; this string when it has no character to escape
     *          or remove.
     * @since   21
     */
    public String escapeXml11() {
        return escapeMarkup(MARKUP_XML | MARKUP_NAMED | MARKUP_XML_1_1 | 1 << MARKUP_LEVEL);
    }

    /**
     * Returns this string with each XML character reference replaced by the character it stands
     * for: {@code &quot;}, {@code &amp;}, {@code &apos;}, {@code &lt;} and {@code &gt;}, and
     * the numeric references {@link #unescapeHtml4()} reads. Each reference is read once, from
     * left to right; all other text is left as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Apache Commons Text's
     * {@code StringEscapeUtils.unescapeXml} has the same result.
     *
     * @return  this string with its references replaced; this string when it has none.
     * @throws  IllegalArgumentException if a numeric reference has a value above U+10FFFF that
     *          is not above {@link Integer#MAX_VALUE}.
     * @since   21
     */
    public String unescapeXml() {
        return unescapeMarkup(UNMARKUP_XML);
    }

    /**
     * Returns this string with its markup-significant characters escaped by HTML 4.01 named
     * references: {@code &}, {@code <}, {@code >} and {@code "} become {@code &amp;},
     * {@code &lt;}, {@code &gt;} and {@code &quot;}, and {@code '} becomes {@code &#39;}; so
     * does U+0000 ({@code &#0;}) and every unpaired surrogate ({@code &#55349;}). This is
     * {@code escapeHtml(HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
     * HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT)}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; unbescape's
     * {@code HtmlEscape.escapeHtml4Xml} has the same result.
     *
     * @return  this string escaped; this string when it has no character to escape.
     * @see     #escapeHtml(HtmlEscapeType, HtmlEscapeLevel)
     * @since   21
     */
    public String escapeHtml4Xml() {
        return escapeHtml(HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }

    /**
     * Returns this string with its markup-significant characters escaped by HTML5 named
     * references: {@code &}, {@code <}, {@code >}, {@code "} and {@code '} become
     * {@code &amp;}, {@code &lt;}, {@code &gt;}, {@code &quot;} and {@code &apos;}; U+0000
     * becomes {@code &#0;} and every unpaired surrogate a decimal reference. This is
     * {@code escapeHtml(HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
     * HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT)}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; unbescape's
     * {@code HtmlEscape.escapeHtml5Xml} has the same result.
     *
     * @return  this string escaped; this string when it has no character to escape.
     * @see     #escapeHtml(HtmlEscapeType, HtmlEscapeLevel)
     * @since   21
     */
    public String escapeHtml5Xml() {
        return escapeHtml(HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }

    /**
     * Returns this string with its markup-significant characters and every character above
     * U+007F escaped, by an HTML5 named reference when there is one (U+00E9 by
     * {@code &eacute;}, {@code '} by {@code &apos;}) and by a decimal reference otherwise
     * (U+4E2D by {@code &#20013;}). The result is ASCII. This is
     * {@code escapeHtml(HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
     * HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT)}.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; unbescape's
     * {@code HtmlEscape.escapeHtml5} has the same result.
     *
     * @return  this string escaped; this string when it has no character to escape.
     * @see     #escapeHtml(HtmlEscapeType, HtmlEscapeLevel)
     * @since   21
     */
    public String escapeHtml5() {
        return escapeHtml(HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }

    /**
     * Returns this string with the characters that {@code level} selects escaped by the
     * references that {@code type} selects. The string is read as code points. U+0000 and every
     * unpaired surrogate are escaped at every level. An escaped character becomes a named
     * reference when {@code type} uses names and its table (HTML 4.01: 252 names, HTML5: one
     * name for each of 1446 characters, among the names of the WHATWG table) has one, and a
     * decimal ({@code &#233;}) or lower-case hexadecimal ({@code &#xe9;}) reference otherwise.
     * Where HTML5 has several names for a character, the one unbescape 1.1.8 writes is written.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; unbescape's
     * {@code HtmlEscape.escapeHtml(text, type, level)} has the same result for non-null
     * arguments.
     *
     * @param   type    the references to write.
     * @param   level   the characters to escape.
     * @return  this string escaped; this string when it has no character to escape.
     * @throws  NullPointerException if {@code type} or {@code level} is {@code null}.
     * @since   21
     */
    public String escapeHtml(HtmlEscapeType type, HtmlEscapeLevel level) {
        java.util.Objects.requireNonNull(type, "type");
        java.util.Objects.requireNonNull(level, "level");
        return escapeMarkup(MARKUP_HTML | type.rule | level.rule);
    }

    /**
     * Returns this string with each HTML5 character reference replaced by the character it
     * stands for, as unbescape 1.1.8 reads them. An {@code &} followed by a space, LF, TAB, FF,
     * {@code <} or {@code &} starts no reference. A numeric reference is {@code &#}, then
     * decimal digits or {@code x} or {@code X} and hexadecimal digits, then an optional
     * {@code ;}; a value of 0, of a surrogate or above U+10FFFF gives U+FFFD, and a value in
     * U+0080..U+009F the Windows-1252 character (U+0080 gives U+20AC). A named reference is
     * {@code &}, a run of ASCII letters and digits and an optional {@code ;}, looked up among
     * the 2231 names of the WHATWG table; when it is not a name, one of the 106 names without
     * {@code ;} that starts the run is taken where unbescape's binary search over the table, in
     * its order, meets it, and the rest of the run is left as it is. All other text is left as it
     * is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; unbescape's
     * {@code HtmlEscape.unescapeHtml} has the same result.
     *
     * @return  this string with its references replaced; this string when it has none.
     * @since   21
     */
    public String unescapeHtml() {
        return unescapeMarkup(UNMARKUP_HTML);
    }

    /**
     * Returns this string with the characters that {@code level} selects escaped for XML 1.0 or
     * XML 1.1, as {@code version} says, by the references that {@code type} selects. The string
     * is read as code points. The characters the version does not allow are removed at every
     * level: for XML 1.0 U+0000..U+0008, U+000B, U+000C and U+000E..U+001F, for XML 1.1
     * U+0000, for both U+FFFE, U+FFFF and every unpaired surrogate. Level 1 escapes
     * {@code &}, {@code <}, {@code >}, {@code "}, {@code '}, U+007F..U+0084 and U+0086..U+009F,
     * for XML 1.1 also U+0001..U+0008, U+000B, U+000C and U+000E..U+001F, and for an attribute
     * value also TAB, LF and CR. An escaped character becomes {@code &amp;}, {@code &lt;},
     * {@code &gt;}, {@code &quot;} or {@code &apos;} when {@code type} uses those and it is one
     * of the five, and a decimal ({@code &#233;}) or lower-case hexadecimal ({@code &#xe9;})
     * reference otherwise.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; unbescape's
     * {@code XmlEscape.escapeXml10}, {@code escapeXml11}, {@code escapeXml10Attribute} and
     * {@code escapeXml11Attribute} with a type and a level have the same result for non-null
     * arguments, and so, for each of their fixed type and level, do its other {@code escapeXml}
     * methods ({@code escapeXml10Minimal} is {@code escapeXml(XmlVersion.XML_1_0,
     * XmlEscapeType.CHARACTER_ENTITY_REFERENCES_DEFAULT_TO_HEXA,
     * XmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT, false)}).
     *
     * @param   version          the XML version.
     * @param   type             the references to write.
     * @param   level            the characters to escape.
     * @param   attributeValue   whether TAB, LF and CR are escaped from level 1, for an
     *                           attribute value.
     * @return  this string escaped; this string when it has no character to escape or remove.
     * @throws  NullPointerException if {@code version}, {@code type} or {@code level} is
     *          {@code null}.
     * @since   21
     */
    public String escapeXml(XmlVersion version, XmlEscapeType type, XmlEscapeLevel level,
            boolean attributeValue) {
        java.util.Objects.requireNonNull(version, "version");
        java.util.Objects.requireNonNull(type, "type");
        java.util.Objects.requireNonNull(level, "level");
        return escapeMarkup(MARKUP_XML | version.rule | type.rule | level.rule
                | (attributeValue ? MARKUP_ATTRIBUTE : 0));
    }

    /**
     * Returns this string with each XML character reference replaced by the character it stands
     * for, as the XML specification writes references: {@code &quot;}, {@code &amp;},
     * {@code &apos;}, {@code &lt;} and {@code &gt;}, and the numeric references {@code &#},
     * decimal digits, {@code ;} and {@code &#x} (lower-case {@code x}), hexadecimal digits,
     * {@code ;}. Each reference is read once, from left to right; all other text is left as it
     * is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; unbescape's
     * {@code XmlEscape.unescapeXml} has the same result. Unlike {@link #unescapeXml()}, it
     * rejects a reference to a character XML 1.1 does not allow.
     *
     * @return  this string with its references replaced; this string when it has none.
     * @throws  IllegalArgumentException if a numeric reference has the value 0, the value of a
     *          surrogate, U+FFFE or U+FFFF, or a value above U+10FFFF.
     * @since   21
     */
    public String unescapeXmlStrict() {
        return unescapeMarkup(UNMARKUP_XML_STRICT);
    }

    /**
     * Returns this string with {@code &}, {@code <} and {@code >} replaced by {@code &amp;},
     * {@code &lt;} and {@code &gt;}, and, when {@code quote} is {@code true}, {@code "} and
     * {@code '} by {@code &quot;} and {@code &#x27;}. Every other character is left as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code html.escape(s, quote)} has the same result.
     *
     * @param   quote   whether the quotation marks are escaped too.
     * @return  this string escaped; this string when it has no character to escape.
     * @since   21
     */
    public String escapeHtml(boolean quote) {
        return escapeMarkup(MARKUP_PYTHON | (quote ? MARKUP_QUOTE : 0));
    }

    /**
     * Returns this string with each HTML5 character reference replaced by the character it
     * stands for, as CPython 3.12 reads them. A numeric reference is {@code &#}, then decimal
     * digits or {@code x} or {@code X} and hexadecimal digits, then an optional {@code ;}; a
     * value of 0, of a surrogate or above U+10FFFF gives U+FFFD, a value in U+0080..U+009F the
     * Windows-1252 character, and the references to U+0001..U+0008, U+000B, U+000E..U+001F,
     * U+007F, U+FDD0..U+FDEF and the last two code points of each plane are removed. A named
     * reference is {@code &}, a run of 1 to 32 code points other than TAB, LF, FF, space,
     * {@code <}, {@code &}, {@code #} and {@code ;}, and an optional {@code ;}: when that is one
     * of the 2231 names of the WHATWG table it is replaced, otherwise its longest prefix of two
     * or more characters that is a name (a name without {@code ;}) is replaced and the rest is
     * left as it is. All other text is left as it is.
     *
     * @apiNote
     * This method is an M3JDK addition to the {@code String} API; Python's
     * {@code html.unescape} (CPython 3.12) has the same result.
     *
     * @return  this string with its references replaced; this string when it has none.
     * @since   21
     */
    public String unescapeHtml5Python() {
        return unescapeMarkup(UNMARKUP_PYTHON);
    }

    /**
     * The references {@link #escapeHtml(HtmlEscapeType, HtmlEscapeLevel)} writes for the
     * characters it escapes; the constants are those of unbescape's {@code HtmlEscapeType}.
     *
     * @apiNote
     * This type is an M3JDK addition to the {@code String} API.
     *
     * @since   21
     */
    public enum HtmlEscapeType {
        /** HTML 4.01 named references, else decimal references. */
        HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL(MARKUP_NAMED),
        /** HTML 4.01 named references, else hexadecimal references. */
        HTML4_NAMED_REFERENCES_DEFAULT_TO_HEXA(MARKUP_NAMED | MARKUP_HEX),
        /** HTML5 named references, else decimal references. */
        HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL(MARKUP_NAMED | MARKUP_HTML5),
        /** HTML5 named references, else hexadecimal references. */
        HTML5_NAMED_REFERENCES_DEFAULT_TO_HEXA(MARKUP_NAMED | MARKUP_HTML5 | MARKUP_HEX),
        /** Decimal references only. */
        DECIMAL_REFERENCES(0),
        /** Hexadecimal references only. */
        HEXADECIMAL_REFERENCES(MARKUP_HEX);

        /** The bits of this type in an escape rule of {@link String#escapeMarkup(int)}. */
        private final int rule;

        HtmlEscapeType(int rule) {
            this.rule = rule;
        }
    }

    /**
     * The characters {@link #escapeHtml(HtmlEscapeType, HtmlEscapeLevel)} escapes; each level
     * escapes those of the levels below it too. The constants are those of unbescape's
     * {@code HtmlEscapeLevel}.
     *
     * @apiNote
     * This type is an M3JDK addition to the {@code String} API.
     *
     * @since   21
     */
    public enum HtmlEscapeLevel {
        /** {@code &}, {@code <}, {@code >} and {@code "}. */
        LEVEL_0_ONLY_MARKUP_SIGNIFICANT_EXCEPT_APOS(0),
        /** Level 0 and {@code '}. */
        LEVEL_1_ONLY_MARKUP_SIGNIFICANT(1),
        /** Level 1 and every character above U+007F. */
        LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT(2),
        /** Level 2 and every ASCII character but the letters and digits. */
        LEVEL_3_ALL_NON_ALPHANUMERIC(3),
        /** Every character. */
        LEVEL_4_ALL_CHARACTERS(4);

        /** The bits of this level in an escape rule of {@link String#escapeMarkup(int)}. */
        private final int rule;

        HtmlEscapeLevel(int level) {
            this.rule = level << MARKUP_LEVEL;
        }
    }

    /**
     * The XML version {@link #escapeXml(XmlVersion, XmlEscapeType, XmlEscapeLevel, boolean)}
     * escapes for.
     *
     * @apiNote
     * This type is an M3JDK addition to the {@code String} API.
     *
     * @since   21
     */
    public enum XmlVersion {
        /** XML 1.0. */
        XML_1_0(0),
        /** XML 1.1. */
        XML_1_1(MARKUP_XML_1_1);

        /** The bits of this version in an escape rule of {@link String#escapeMarkup(int)}. */
        private final int rule;

        XmlVersion(int rule) {
            this.rule = rule;
        }
    }

    /**
     * The references {@link #escapeXml(XmlVersion, XmlEscapeType, XmlEscapeLevel, boolean)}
     * writes for the characters it escapes; the constants are those of unbescape's
     * {@code XmlEscapeType}.
     *
     * @apiNote
     * This type is an M3JDK addition to the {@code String} API.
     *
     * @since   21
     */
    public enum XmlEscapeType {
        /** The five predefined entity references, else decimal references. */
        CHARACTER_ENTITY_REFERENCES_DEFAULT_TO_DECIMAL(MARKUP_NAMED),
        /** The five predefined entity references, else hexadecimal references. */
        CHARACTER_ENTITY_REFERENCES_DEFAULT_TO_HEXA(MARKUP_NAMED | MARKUP_HEX),
        /** Decimal references only. */
        DECIMAL_REFERENCES(0),
        /** Hexadecimal references only. */
        HEXADECIMAL_REFERENCES(MARKUP_HEX);

        /** The bits of this type in an escape rule of {@link String#escapeMarkup(int)}. */
        private final int rule;

        XmlEscapeType(int rule) {
            this.rule = rule;
        }
    }

    /**
     * The characters {@link #escapeXml(XmlVersion, XmlEscapeType, XmlEscapeLevel, boolean)}
     * escapes besides those it removes; each level escapes those of the levels below it too. The
     * constants are those of unbescape's {@code XmlEscapeLevel}.
     *
     * @apiNote
     * This type is an M3JDK addition to the {@code String} API.
     *
     * @since   21
     */
    public enum XmlEscapeLevel {
        /** The markup-significant characters and the controls the version escapes. */
        LEVEL_1_ONLY_MARKUP_SIGNIFICANT(1),
        /** Level 1 and every character above U+007F. */
        LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT(2),
        /** Level 2 and every ASCII character but the letters and digits. */
        LEVEL_3_ALL_NON_ALPHANUMERIC(3),
        /** Every character. */
        LEVEL_4_ALL_CHARACTERS(4);

        /** The bits of this level in an escape rule of {@link String#escapeMarkup(int)}. */
        private final int rule;

        XmlEscapeLevel(int level) {
            this.rule = level << MARKUP_LEVEL;
        }
    }

    /** Escape rules of {@link #escapeMarkup(int)}: a kind (the low three bits) and options. */
    private static final int MARKUP_KIND = 7;
    /** Commons Text {@code escapeHtml3}. */
    private static final int MARKUP_HTML3 = 0;
    /** Commons Text {@code escapeHtml4}. */
    private static final int MARKUP_HTML4 = 1;
    /** Python {@code html.escape}. */
    private static final int MARKUP_PYTHON = 2;
    /** unbescape {@code HtmlEscape.escapeHtml}. */
    private static final int MARKUP_HTML = 3;
    /** unbescape {@code XmlEscape.escapeXml}, of which Commons Text's XML rules are points. */
    private static final int MARKUP_XML = 4;
    /** The shift of the level (0 to 4) in a rule. */
    private static final int MARKUP_LEVEL = 3;
    /** Option: hexadecimal, not decimal, references. */
    private static final int MARKUP_HEX = 1 << 6;
    /** Option: named references where the table has one. */
    private static final int MARKUP_NAMED = 1 << 7;
    /** Option: the HTML5 table, not the HTML 4.01 one. */
    private static final int MARKUP_HTML5 = 1 << 8;
    /** Option: XML 1.1, not XML 1.0. */
    private static final int MARKUP_XML_1_1 = 1 << 9;
    /** Option: an XML attribute value (TAB, LF and CR from level 1). */
    private static final int MARKUP_ATTRIBUTE = 1 << 10;
    /** Option: Python's {@code quote}. */
    private static final int MARKUP_QUOTE = 1 << 11;
    /** Unescape rules of {@link #unescapeMarkup(int)}. */
    private static final int UNMARKUP_HTML3 = 0;
    private static final int UNMARKUP_HTML4 = 1;
    private static final int UNMARKUP_XML = 2;
    private static final int UNMARKUP_HTML = 3;
    private static final int UNMARKUP_XML_STRICT = 4;
    private static final int UNMARKUP_PYTHON = 5;
    /**
     * The longest run of letters and digits a name lookup reads: longer than every name (31
     * letters at most), so the lookup of a longer run is decided by its first units.
     */
    private static final int MARKUP_NAME_MAX = 40;
    /** The characters Windows-1252 gives to the bytes 0x80..0x9F, as HTML5 maps references. */
    private static final String MARKUP_C1 =
            "\u20AC\u0081\u201A\u0192\u201E\u2026\u2020\u2021\u02C6\u2030\u0160\u2039\u0152" +
            "\u008D\u017D\u008F\u0090\u2018\u2019\u201C\u201D\u2022\u2013\u2014\u02DC\u2122" +
            "\u0161\u203A\u0153\u009D\u017E\u0178";

    /**
     * This string escaped by {@code rule}: each code point (an unpaired surrogate is one of its
     * own) that the rule changes is replaced by its reference or removed. Prepared M3 facts
     * answer when they show an ASCII string without a unit the rule changes; otherwise the
     * string is read in bulk windows, a surrogate pair never split between two windows, and the
     * first change is found before anything is copied. Unchanged runs are copied as ranges, and
     * an unchanged string is returned as it is.
     */
    private String escapeMarkup(int rule) {
        if (markupUntouched(rule)) return this;
        int length = length();
        StringBuilder out = null;
        int copied = 0;
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; ) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            int count = to - from;
            if (to < length && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;
            for (int at = 0; at < count; ) {
                int point = Character.codePointAt(window, at, count);
                int width = Character.charCount(point);
                if (!markupKeeps(rule, point)) {
                    if (out == null) out = new StringBuilder(length + 16);
                    out.append(this, copied, from + at);
                    appendMarkup(out, rule, point);
                    copied = from + at + width;
                }
                at += width;
            }
            from += count;
        }
        return out == null ? this : out.append(this, copied, length).toString();
    }

    /** Whether prepared M3 facts show this string is ASCII without a unit {@code rule} changes. */
    private boolean markupUntouched(int rule) {
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts == null || !facts.ascii) return false;
        for (int unit = 0; unit < 0x80; unit++) {
            if (!markupKeeps(rule, unit) && facts.mayContainCodeUnit((char) unit)) return false;
        }
        return true;
    }

    /** Whether escape rule {@code rule} leaves {@code point} as it is. */
    private static boolean markupKeeps(int rule, int point) {
        boolean markup = point == '"' || point == '&' || point == '<' || point == '>';
        int level = rule >>> MARKUP_LEVEL & 7;
        return switch (rule & MARKUP_KIND) {
            case MARKUP_HTML3 -> point < 0xA0 ? !markup : point > 0xFF;
            case MARKUP_HTML4 -> point < 0xA0 ? !markup
                    : point > 0xFF && Html4Names.TABLE.escapeName(point) < 0;
            case MARKUP_PYTHON -> !(point == '&' || point == '<' || point == '>'
                    || (rule & MARKUP_QUOTE) != 0 && (point == '"' || point == '\''));
            case MARKUP_HTML -> point < 0x80 ? point != 0 && level < markupHtmlLevel(point)
                    : (point < 0xD800 || point > 0xDFFF) && level < 2;
            default -> markupXmlAllowed(rule, point) && level < markupXmlLevel(rule, point);
        };
    }

    /** The lowest unbescape HTML level that escapes ASCII character {@code point}. */
    private static int markupHtmlLevel(int point) {
        if (markupAlphanumeric(point)) return 4;
        if (point == '\'') return 1;
        return point == '"' || point == '&' || point == '<' || point == '>' ? 0 : 3;
    }

    /** The lowest unbescape XML level of {@code rule} that escapes {@code point}. */
    private static int markupXmlLevel(int rule, int point) {
        if (point >= 0x7F) return point >= 0xA0 || point == 0x85 ? 2 : 1;
        if (markupAlphanumeric(point)) return 4;
        if (markupEntity(point) != null) return 1;
        if (point == '\t' || point == '\n' || point == '\r') {
            return (rule & MARKUP_ATTRIBUTE) != 0 ? 1 : 3;
        }
        return point < 0x20 && (rule & MARKUP_XML_1_1) != 0 ? 1 : 3;
    }

    /** Whether the XML version of {@code rule} allows {@code point} (a lone surrogate never). */
    private static boolean markupXmlAllowed(int rule, int point) {
        if (point < 0x20) {
            return point == '\t' || point == '\n' || point == '\r'
                    || point != 0 && (rule & MARKUP_XML_1_1) != 0;
        }
        return (point < 0xD800 || point > 0xDFFF) && point != 0xFFFE && point != 0xFFFF;
    }

    /** Whether {@code point} is an ASCII letter or digit. */
    private static boolean markupAlphanumeric(int point) {
        return point >= '0' && point <= '9' || point >= 'A' && point <= 'Z'
                || point >= 'a' && point <= 'z';
    }

    /** The predefined XML entity reference of {@code point}, or {@code null}. */
    private static String markupEntity(int point) {
        return switch (point) {
            case '"' -> "&quot;";
            case '&' -> "&amp;";
            case '\'' -> "&apos;";
            case '<' -> "&lt;";
            case '>' -> "&gt;";
            default -> null;
        };
    }

    /** Appends what escape rule {@code rule} writes for {@code point}, which it changes. */
    private static void appendMarkup(StringBuilder out, int rule, int point) {
        int kind = rule & MARKUP_KIND;
        if (kind == MARKUP_HTML3 || kind == MARKUP_HTML4) {
            Html4Names.TABLE.appendName(out, Html4Names.TABLE.escapeName(point));
        } else if (kind == MARKUP_PYTHON) {
            out.append(point == '\'' ? "&#x27;" : markupEntity(point));
        } else if (kind == MARKUP_XML) {
            if (!markupXmlAllowed(rule, point)) return;
            String entity = (rule & MARKUP_NAMED) != 0 ? markupEntity(point) : null;
            if (entity != null) {
                out.append(entity);
            } else {
                appendMarkupNumber(out, rule, point);
            }
        } else {
            MarkupNames names = (rule & MARKUP_NAMED) == 0 ? null
                    : (rule & MARKUP_HTML5) != 0 ? Html5Names.TABLE : Html4Names.TABLE;
            int name = names == null ? -1 : names.escapeName(point);
            if (name >= 0) {
                names.appendName(out, name);
            } else {
                appendMarkupNumber(out, rule, point);
            }
        }
    }

    /** Appends the decimal or (with {@link #MARKUP_HEX}) hexadecimal reference to {@code point}. */
    private static void appendMarkupNumber(StringBuilder out, int rule, int point) {
        if ((rule & MARKUP_HEX) != 0) {
            out.append("&#x").append(Integer.toHexString(point)).append(';');
        } else {
            out.append("&#").append(point).append(';');
        }
    }

    /**
     * This string unescaped by {@code rule}: each reference the rule reads is replaced by its
     * characters (none when the rule removes it). Prepared M3 facts answer when they show no
     * {@code &}; otherwise the references are found by {@link #indexOf(int, int)}, each read on
     * its own units, and the text between them is copied as ranges. A string without a
     * reference is returned as it is.
     */
    private String unescapeMarkup(int rule) {
        if (!mayContainUnit('&')) return this;
        int length = length();
        StringBuilder out = null;
        int copied = 0;
        int[] value = new int[2];
        for (int at = indexOf('&'); at >= 0; ) {
            value[1] = -1;
            int end = switch (rule) {
                case UNMARKUP_HTML -> markupLenient(at, value);
                case UNMARKUP_XML_STRICT -> markupStrict(at, value);
                case UNMARKUP_PYTHON -> markupPython(at, value);
                default -> markupCommons(rule, at, value);
            };
            if (end < 0) {
                at = indexOf('&', at + 1);
            } else {
                if (out == null) out = new StringBuilder(length);
                out.append(this, copied, at);
                if (value[0] >= 0) out.appendCodePoint(value[0]);
                if (value[1] >= 0) out.appendCodePoint(value[1]);
                copied = end;
                at = indexOf('&', end);
            }
        }
        return out == null ? this : out.append(this, copied, length).toString();
    }

    /**
     * The end of the reference at {@code at} as Commons Text reads it for {@code rule}, its
     * character in {@code value[0]}; -1 when there is none. A name is one of the rule's names
     * followed by {@code ;}; a number is read by {@link #markupCommonsNumber(int, int[])}.
     */
    private int markupCommons(int rule, int at, int[] value) {
        int length = length();
        if (at + 2 >= length) return -1;
        if (charAt(at + 1) == '#') return markupCommonsNumber(at, value);
        int end = at + 1;
        while (end < length && end - at <= MARKUP_NAME_MAX && markupAlphanumeric(charAt(end))) {
            end++;
        }
        if (end == at + 1 || end >= length || charAt(end) != ';') return -1;
        int point;
        if (rule == UNMARKUP_XML) {
            point = markupEntityPoint(at + 1, end + 1);
        } else {
            int name = Html4Names.TABLE.find(this, at + 1, end + 1);
            point = name < 0 ? -1 : Html4Names.TABLE.first(name);
            if (rule == UNMARKUP_HTML3 && point > 0xFF) point = -1;
        }
        value[0] = point;
        return point < 0 ? -1 : end + 1;
    }

    /**
     * The end of the numeric reference at {@code at} as Commons Text reads it, its character in
     * {@code value[0]}; -1 when there is none: {@code &#}, then ASCII hexadecimal digits (after
     * {@code x} or {@code X}) or decimal ones, then {@code ;}, with a value that fits an int.
     */
    private int markupCommonsNumber(int at, int[] value) {
        int length = length();
        int start = at + 2;
        boolean hex = charAt(start) == 'x' || charAt(start) == 'X';
        if (hex && ++start == length) return -1;
        int end = markupDigits(start, 16);
        if (start == end || end == length || charAt(end) != ';') return -1;
        int radix = hex ? 16 : 10;
        long number = 0;
        for (int index = start; index < end; index++) {
            int digit = markupDigit(charAt(index), radix);
            number = number * radix + digit;
            if (digit < 0 || number > Integer.MAX_VALUE) return -1;
        }
        if (number > Character.MAX_CODE_POINT) {
            throw new IllegalArgumentException("Not a valid Unicode code point: 0x"
                    + Integer.toHexString((int) number).toUpperCase(java.util.Locale.ROOT));
        }
        value[0] = (int) number;
        return end + 1;
    }

    /**
     * The end of the reference at {@code at} as unbescape's {@code HtmlEscape.unescapeHtml}
     * reads it, its characters in {@code value}; -1 when there is none.
     */
    private int markupLenient(int at, int[] value) {
        int length = length();
        if (at + 1 >= length) return -1;
        char next = charAt(at + 1);
        if (next == ' ' || next == '\n' || next == '\t' || next == '\f' || next == '<'
                || next == '&') {
            return -1;
        }
        if (next == '#') {
            if (at + 2 >= length) return -1;
            char kind = charAt(at + 2);
            boolean hex = (kind == 'x' || kind == 'X') && at + 3 < length;
            int start = hex ? at + 3 : at + 2;
            int end = markupDigits(start, hex ? 16 : 10);
            if (end == start) return -1;
            value[0] = markupFixedPoint(markupNumber(start, end, hex ? 16 : 10));
            return end < length && charAt(end) == ';' ? end + 1 : end;
        }
        int end = at + 1;
        while (end < length && end - at <= MARKUP_NAME_MAX && markupAlphanumeric(charAt(end))) {
            end++;
        }
        if (end == at + 1) return -1;
        if (end < length && charAt(end) == ';') end++;
        MarkupNames names = Html5Names.TABLE;
        int found = names.findLenient(this, at + 1, end);
        if (found == -1) return -1;
        int name = found >= 0 ? found : -2 - found;
        value[0] = names.first(name);
        value[1] = names.second(name);
        return found >= 0 ? end : at + 1 + names.length(name);
    }

    /**
     * The end of the reference at {@code at} as unbescape's {@code XmlEscape.unescapeXml} reads
     * it, its character in {@code value[0]}; -1 when there is none.
     */
    private int markupStrict(int at, int[] value) {
        int length = length();
        if (at + 2 >= length) return -1;
        if (charAt(at + 1) == '#') {
            boolean hex = charAt(at + 2) == 'x' && at + 3 < length;
            int start = hex ? at + 3 : at + 2;
            int end = markupDigits(start, hex ? 16 : 10);
            if (end == start || end == length || charAt(end) != ';') return -1;
            int point = markupNumber(start, end, hex ? 16 : 10);
            if (point < 1 || point >= 0xD800 && point <= 0xDFFF || point == 0xFFFE
                    || point == 0xFFFF || point > Character.MAX_CODE_POINT) {
                throw new IllegalArgumentException("Not a valid XML char in character reference: 0x"
                        + Integer.toHexString(point).toUpperCase(java.util.Locale.ROOT));
            }
            value[0] = point;
            return end + 1;
        }
        int end = at + 1;
        while (end < length && end - at <= MARKUP_NAME_MAX && markupAlphanumeric(charAt(end))) {
            end++;
        }
        if (end == at + 1 || end == length || charAt(end) != ';') return -1;
        value[0] = markupEntityPoint(at + 1, end + 1);
        return value[0] < 0 ? -1 : end + 1;
    }

    /**
     * The end of the reference at {@code at} as CPython 3.12's {@code html.unescape} reads it,
     * its characters in {@code value} ({@code value[0]} is -1 when it is removed); -1 when there
     * is none.
     */
    private int markupPython(int at, int[] value) {
        int length = length();
        if (at + 1 >= length) return -1;
        if (charAt(at + 1) == '#') {
            if (at + 2 >= length) return -1;
            boolean hex = charAt(at + 2) == 'x' || charAt(at + 2) == 'X';
            int start = hex ? at + 3 : at + 2;
            int end = markupDigits(start, hex ? 16 : 10);
            if (end == start) return -1;
            int point = markupFixedPoint(markupNumber(start, end, hex ? 16 : 10));
            boolean removed = point >= 1 && point <= 8 || point == 0x0B
                    || point >= 0x0E && point <= 0x1F || point == 0x7F
                    || point >= 0xFDD0 && point <= 0xFDEF || (point & 0xFFFE) == 0xFFFE;
            value[0] = removed ? -1 : point;
            return end < length && charAt(end) == ';' ? end + 1 : end;
        }
        int end = at + 1;
        int points = 0;
        int alphanumeric = -1;
        while (end < length && points < 32) {
            char unit = charAt(end);
            if (unit == '\t' || unit == '\n' || unit == '\f' || unit == ' ' || unit == '<'
                    || unit == '&' || unit == '#' || unit == ';') {
                break;
            }
            if (alphanumeric < 0 && !markupAlphanumeric(unit)) alphanumeric = points;
            end += Character.isHighSurrogate(unit) && end + 1 < length
                    && Character.isLowSurrogate(charAt(end + 1)) ? 2 : 1;
            points++;
        }
        if (points == 0) return -1;
        boolean semicolon = end < length && charAt(end) == ';';
        int longest = Math.min(alphanumeric < 0 ? points : alphanumeric,
                semicolon ? points : points - 1);
        MarkupNames names = Html5Names.TABLE;
        int name = names.find(this, at + 1, semicolon ? end + 1 : end);
        if (name >= 0) {
            end = semicolon ? end + 1 : end;
        } else {
            for (end = at + 1 + longest; end >= at + 3; end--) {
                name = names.find(this, at + 1, end);
                if (name >= 0) break;
            }
            if (name < 0) return -1;
        }
        value[0] = names.first(name);
        value[1] = names.second(name);
        return end;
    }

    /** {@code point} as HTML5 fixes a numeric reference: U+FFFD for 0, a surrogate or above. */
    private static int markupFixedPoint(int point) {
        if (point == 0 || point >= 0xD800 && point <= 0xDFFF || point > Character.MAX_CODE_POINT) {
            return 0xFFFD;
        }
        return point >= 0x80 && point <= 0x9F ? MARKUP_C1.charAt(point - 0x80) : point;
    }

    /** The predefined XML entity whose name and {@code ;} are at {@code [start, end)}, or -1. */
    private int markupEntityPoint(int start, int end) {
        return switch (end - start) {
            case 3 -> regionMatches(start, "lt;", 0, 3) ? '<'
                    : regionMatches(start, "gt;", 0, 3) ? '>' : -1;
            case 4 -> regionMatches(start, "amp;", 0, 4) ? '&' : -1;
            case 5 -> regionMatches(start, "quot;", 0, 5) ? '"'
                    : regionMatches(start, "apos;", 0, 5) ? '\'' : -1;
            default -> -1;
        };
    }

    /** The end of the run of ASCII digits of {@code radix} (10 or 16) from {@code start}. */
    private int markupDigits(int start, int radix) {
        int end = start;
        while (end < length() && markupDigit(charAt(end), radix) >= 0) end++;
        return end;
    }

    /** The value of the ASCII digits at {@code [start, end)}, or U+10FFFF + 1 when larger. */
    private int markupNumber(int start, int end, int radix) {
        int number = 0;
        for (int index = start; index < end && number <= Character.MAX_CODE_POINT; index++) {
            number = number * radix + markupDigit(charAt(index), radix);
        }
        return Math.min(number, Character.MAX_CODE_POINT + 1);
    }

    /** The value of {@code unit} as an ASCII digit of {@code radix} (10 or 16), or -1. */
    private static int markupDigit(char unit, int radix) {
        int digit = unit >= '0' && unit <= '9' ? unit - '0'
                : unit >= 'a' && unit <= 'f' ? unit - 'a' + 10
                : unit >= 'A' && unit <= 'F' ? unit - 'A' + 10 : -1;
        return digit < radix ? digit : -1;
    }

    /**
     * A table of named character references, built once from a table text: the names in the
     * order of unbescape's {@code HtmlEscapeSymbols} ({@code ;} sorts below the end of a name,
     * the end below every other character), each with its one or two code points, and for each
     * code point the name its escapers write. Table text: entries separated by a space, each a
     * base-36 digit (the characters shared with the previous name), the rest of the name, a
     * marker ({@code =} plain, {@code :} the escape name of its code point, {@code -} also a
     * name without {@code ;}, {@code +} both), the code point in hexadecimal and optionally
     * {@code /} and a second one. Every name of the text ends with {@code ;}, which it leaves
     * out.
     */
    private static final class MarkupNames {
        /** The characters of the names, each without its {@code &} and with its {@code ;}. */
        private final char[] chars;
        /** Where each name starts in {@link #chars}. */
        private final int[] starts;
        /** The length of each name. */
        private final int[] lengths;
        /** The first code point of each name. */
        private final int[] firsts;
        /** The second code point of each name, or -1. */
        private final int[] seconds;
        /** The code points with an escape name, ascending. */
        private final int[] escapePoints;
        /** The escape name of each of {@link #escapePoints}. */
        private final int[] escapeNames;

        MarkupNames(String text) {
            int count = 0;
            int escapes = 0;
            int size = 0;
            for (int at = 0; at < text.length(); ) {
                int end = at + 1;
                while (markupAlphanumeric(text.charAt(end))) end++;
                char marker = text.charAt(end);
                size += Character.digit(text.charAt(at), 36) + end - at;
                count += marker == '-' || marker == '+' ? 2 : 1;
                escapes += marker == ':' || marker == '+' ? 1 : 0;
                int space = text.indexOf(' ', end);
                at = space < 0 ? text.length() : space + 1;
            }
            chars = new char[size];
            starts = new int[count];
            lengths = new int[count];
            firsts = new int[count];
            seconds = new int[count];
            long[] escape = new long[escapes];
            int n = 0;
            int e = 0;
            int used = 0;
            int previous = 0;
            for (int at = 0; at < text.length(); ) {
                int start = used;
                int keep = Character.digit(text.charAt(at), 36);
                System.arraycopy(chars, previous, chars, used, keep);
                used += keep;
                int end = at + 1;
                while (markupAlphanumeric(text.charAt(end))) chars[used++] = text.charAt(end++);
                chars[used++] = ';';
                char marker = text.charAt(end);
                boolean legacy = marker == '-' || marker == '+';
                int first = 0;
                int second = -1;
                for (end++; end < text.length() && text.charAt(end) != ' '; end++) {
                    char c = text.charAt(end);
                    if (c == '/') {
                        second = 0;
                    } else if (second < 0) {
                        first = first << 4 | Character.digit(c, 16);
                    } else {
                        second = second << 4 | Character.digit(c, 16);
                    }
                }
                for (int twin = 0; twin <= (legacy ? 1 : 0); twin++) {
                    starts[n] = start;
                    lengths[n] = used - start - twin;
                    firsts[n] = first;
                    seconds[n++] = second;
                }
                if (marker == ':' || marker == '+') {
                    escape[e++] = (long) first << 32 | (n - (legacy ? 2 : 1));
                }
                previous = start;
                at = end + 1;
            }
            java.util.Arrays.sort(escape);
            escapePoints = new int[escapes];
            escapeNames = new int[escapes];
            for (int i = 0; i < escapes; i++) {
                escapePoints[i] = (int) (escape[i] >>> 32);
                escapeNames[i] = (int) escape[i];
            }
        }

        /** The first code point of name {@code name}. */
        int first(int name) {
            return firsts[name];
        }

        /** The second code point of name {@code name}, or -1. */
        int second(int name) {
            return seconds[name];
        }

        /** The length of name {@code name} with its {@code ;}, without its {@code &}. */
        int length(int name) {
            return lengths[name];
        }

        /** The escape name of {@code point}, or -1. */
        int escapeName(int point) {
            int i = java.util.Arrays.binarySearch(escapePoints, point);
            return i < 0 ? -1 : escapeNames[i];
        }

        /** Appends {@code &} and name {@code name}. */
        void appendName(StringBuilder out, int name) {
            out.append('&').append(chars, starts[name], lengths[name]);
        }

        /** The name that is {@code text[start, end)}, or -1. */
        int find(String text, int start, int end) {
            int low = 0;
            int high = starts.length - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int cmp = compare(mid, text, start, end);
                if (cmp == 0) return mid;
                if (cmp > 0) {
                    high = mid - 1;
                } else {
                    low = mid + 1;
                }
            }
            return -1;
        }

        /**
         * unbescape's {@code HtmlEscapeSymbols.binarySearch} of {@code text[start, end)}: the
         * name it is, else {@code -2 - name} for the longest name without {@code ;} that the
         * search meets as a prefix of it, else -1.
         */
        int findLenient(String text, int start, int end) {
            int low = 0;
            int high = starts.length - 1;
            int partial = -1;
            int rank = Integer.MIN_VALUE;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int cmp = compare(mid, text, start, end);
                if (cmp == 0) return mid;
                if (cmp == 1) {
                    high = mid - 1;
                } else {
                    low = mid + 1;
                    if (cmp < -10 && (partial < 0 || rank < cmp)) {
                        partial = mid;
                        rank = cmp;
                    }
                }
            }
            return partial < 0 ? -1 : -2 - partial;
        }

        /**
         * unbescape's {@code HtmlEscapeSymbols.compare} of name {@code name} with
         * {@code text[start, end)}: -1 or 1 as the name sorts before or after it, 0 when equal,
         * and {@code -(10 + rest)} when the name is a prefix of it, not followed by {@code ;},
         * with {@code rest} characters of the text left over.
         */
        private int compare(int name, String text, int start, int end) {
            int length = lengths[name];
            int textLength = end - start;
            int common = Math.min(length, textLength);
            int i = 0;
            for (; i < common; i++) {
                char n = chars[starts[name] + i];
                char t = text.charAt(start + i);
                if (n < t) return t == ';' ? 1 : -1;
                if (n > t) return n == ';' ? -1 : 1;
            }
            if (length > i) return chars[starts[name] + i] == ';' ? -1 : 1;
            if (textLength > i) return text.charAt(start + i) == ';' ? 1 : -(textLength - i + 10);
            return 0;
        }
    }

    /** The HTML 4.01 names (252), as unbescape 1.1.8 and Commons Text 1.15.0 have them. */
    private static final class Html4Names {
        /**
         * Generated by tools/gen_markup_tables.py from the escape maps of unbescape 1.1.8 and
         * Commons Text 1.15.0 (EntityArrays), which agree.
         */
        static final MarkupNames TABLE = new MarkupNames(
                "0AElig:c6 1acute:c1 1circ:c2 1grave:c0 1lpha:391 1ring:c5 1tilde:c3 1uml:c4 " +
                "0Beta:392 0Ccedil:c7 1hi:3a7 0Dagger:2021 1elta:394 0ETH:d0 1acute:c9 1circ:ca " +
                "1grave:c8 1psilon:395 1ta:397 1uml:cb 0Gamma:393 0Iacute:cd 1circ:ce 1grave:cc " +
                "1ota:399 1uml:cf 0Kappa:39a 0Lambda:39b 0Mu:39c 0Ntilde:d1 1u:39d 0OElig:152 " +
                "1acute:d3 1circ:d4 1grave:d2 1mega:3a9 2icron:39f 1slash:d8 1tilde:d5 1uml:d6 " +
                "0Phi:3a6 1i:3a0 1rime:2033 1si:3a8 0Rho:3a1 0Scaron:160 1igma:3a3 0THORN:de " +
                "1au:3a4 1heta:398 0Uacute:da 1circ:db 1grave:d9 1psilon:3a5 1uml:dc 0Xi:39e " +
                "0Yacute:dd 1uml:178 0Zeta:396 0aacute:e1 1circ:e2 2ute:b4 1elig:e6 1grave:e0 " +
                "1lefsym:2135 2pha:3b1 1mp:26 1nd:2227 2g:2220 1ring:e5 1symp:2248 1tilde:e3 " +
                "1uml:e4 0bdquo:201e 1eta:3b2 1rvbar:a6 1ull:2022 0cap:2229 1cedil:e7 1edil:b8 " +
                "2nt:a2 1hi:3c7 1irc:2c6 1lubs:2663 1ong:2245 2py:a9 1rarr:21b5 1up:222a " +
                "2rren:a4 0dArr:21d3 1agger:2020 2rr:2193 1eg:b0 2lta:3b4 1iams:2666 2vide:f7 " +
                "0eacute:e9 1circ:ea 1grave:e8 1mpty:2205 2sp:2003 1nsp:2002 1psilon:3b5 " +
                "1quiv:2261 1ta:3b7 2h:f0 1uml:eb 2ro:20ac 1xist:2203 0fnof:192 1orall:2200 " +
                "1rac12:bd 54:bc 434:be 3sl:2044 0gamma:3b3 1e:2265 1t:3e 0hArr:21d4 1arr:2194 " +
                "1earts:2665 2llip:2026 0iacute:ed 1circ:ee 1excl:a1 1grave:ec 1mage:2111 " +
                "1nfin:221e 2t:222b 1ota:3b9 1quest:bf 1sin:2208 1uml:ef 0kappa:3ba 0lArr:21d0 " +
                "1ambda:3bb 2ng:2329 2quo:ab 2rr:2190 1ceil:2308 1dquo:201c 1e:2264 1floor:230a " +
                "1owast:2217 2z:25ca 1rm:200e 1saquo:2039 2quo:2018 1t:3c 0macr:af 1dash:2014 " +
                "1icro:b5 2ddot:b7 2nus:2212 1u:3bc 0nabla:2207 1bsp:a0 1dash:2013 1e:2260 " +
                "1i:220b 1ot:ac 3in:2209 1sub:2284 1tilde:f1 1u:3bd 0oacute:f3 1circ:f4 " +
                "1elig:153 1grave:f2 1line:203e 1mega:3c9 2icron:3bf 1plus:2295 1r:2228 2df:aa " +
                "3m:ba 1slash:f8 1tilde:f5 3mes:2297 1uml:f6 0para:b6 3t:2202 1ermil:2030 " +
                "3p:22a5 1hi:3c6 1i:3c0 2v:3d6 1lusmn:b1 1ound:a3 1rime:2032 2od:220f 3p:221d " +
                "1si:3c8 0quot:22 0rArr:21d2 1adic:221a 2ng:232a 2quo:bb 2rr:2192 1ceil:2309 " +
                "1dquo:201d 1eal:211c 2g:ae 1floor:230b 1ho:3c1 1lm:200f 1saquo:203a 2quo:2019 " +
                "0sbquo:201a 1caron:161 1dot:22c5 1ect:a7 1hy:ad 1igma:3c3 5f:3c2 2m:223c " +
                "1pades:2660 1ub:2282 3e:2286 2m:2211 2p:2283 31:b9 32:b2 33:b3 3e:2287 1zlig:df " +
                "0tau:3c4 1here4:2234 3ta:3b8 5sym:3d1 2insp:2009 2orn:fe 1ilde:2dc 2mes:d7 " +
                "1rade:2122 0uArr:21d1 1acute:fa 2rr:2191 1circ:fb 1grave:f9 1ml:a8 1psih:3d2 " +
                "4lon:3c5 1uml:fc 0weierp:2118 0xi:3be 0yacute:fd 1en:a5 1uml:ff 0zeta:3b6 " +
                "1wj:200d 2nj:200c");
    }

    /** The HTML5 names (2231) of the WHATWG table, with the escape names of unbescape 1.1.8. */
    private static final class Html5Names {
        /**
         * Generated by tools/gen_markup_tables.py from the WHATWG entities.json of SHA-256
         * d741d877ac77c4194c4ad526b5b4a19aef8dfe411ab840a466891cdbb9f362e6, checked against
         * unbescape 1.1.8 and CPython 3.12.6.
         */
        static final MarkupNames TABLE = new MarkupNames(
                "0AElig+c6 1MP-26 1acute+c1 1breve:102 1circ+c2 2y:410 1fr:1d504 1grave+c0 " +
                "1lpha:391 1macr:100 1nd:2a53 1ogon:104 2pf:1d538 1pplyFunction=2061 1ring+c5 " +
                "1scr:1d49c 2sign=2254 1tilde+c3 1uml+c4 0Backslash=2216 2rv:2ae7 3wed=2306 " +
                "1cy:411 1ecause=2235 2rnoullis=212c 2ta:392 1fr:1d505 1opf:1d539 1reve=2d8 " +
                "1scr=212c 1umpeq=224e 0CHcy:427 1OPY-a9 1acute:106 2p:22d2 " +
                "3italDifferentialD:2145 2yleys:212d 1caron:10c 2edil+c7 2irc:108 2onint:2230 " +
                "1dot:10a 1edilla=b8 2nterDot=b7 1fr=212d 1hi:3a7 1ircleDot=2299 6Minus=2296 " +
                "6Plus=2295 6Times=2297 1lockwiseContourIntegral=2232 3seCurlyDoubleQuote=201d " +
                "aQuote=2019 1olon:2237 5e:2a74 2ngruent=2261 3int:222f 3tourIntegral=222e " +
                "2pf=2102 3roduct=2210 2unterClockwiseContourIntegral=2233 1ross:2a2f 1scr:1d49e " +
                "1up:22d3 3Cap=224d 0DD=2145 2otrahd:2911 1Jcy:402 1Scy:405 1Zcy:40f 1agger:2021 " +
                "2rr:21a1 2shv:2ae4 1caron:10e 2y:414 1el=2207 3ta:394 1fr:1d507 " +
                "1iacriticalAcute=b4 bDot=2d9 dubleAcute=2dd bGrave=60 bTilde=2dc 3mond=22c4 " +
                "2fferentialD=2146 1opf:1d53b 2t=a8 3Dot:20dc 3Equal=2250 " +
                "2ubleContourIntegral=222f 6Dot=a8 8wnArrow=21d3 6LeftArrow=21d0 " +
                "aRightArrow=21d4 aTee=2ae4 7ongLeftArrow=27f8 eRightArrow=27fa aRightArrow=27f9 " +
                "6RightArrow=21d2 bTee=22a8 6UpArrow=21d1 8DownArrow=21d5 6VerticalBar=2225 " +
                "2wnArrow=2193 9Bar:2913 9UpArrow=21f5 4Breve:311 4LeftRightVector:2950 " +
                "8TeeVector:295e 8Vector=21bd eBar:2956 4RightTeeVector:295f 9Vector=21c1 " +
                "fBar:2957 4Tee=22a4 7Arrow=21a7 4arrow=21d3 1scr:1d49f 2trok:110 0ENG:14a " +
                "1TH+d0 1acute+c9 1caron:11a 2irc+ca 2y:42d 1dot:116 1fr:1d508 1grave+c8 " +
                "1lement=2208 1macr:112 2ptySmallSquare:25fb 5VerySmallSquare:25ab 1ogon:118 " +
                "2pf:1d53c 1psilon:395 1qual:2a75 5Tilde=2242 3ilibrium=21cc 1scr=2130 2im:2a73 " +
                "1ta:397 1uml+cb 1xists=2203 2ponentialE=2147 0Fcy:424 1fr:1d509 " +
                "1illedSmallSquare:25fc 6VerySmallSquare=25aa 1opf:1d53d 2rAll=2200 " +
                "2uriertrf:2131 1scr=2131 0GJcy:403 1T-3e 1amma:393 5d:3dc 1breve:11e 1cedil:122 " +
                "2irc:11c 2y:413 1dot:120 1fr:1d50a 1g=22d9 1opf:1d53e 1reaterEqual=2265 " +
                "cLess=22db 7FullEqual=2267 7Greater:2aa2 7Less=2277 7SlantEqual=2a7e " +
                "7Tilde=2273 1scr:1d4a2 1t=226b 0HARDcy:42a 1acek=2c7 2t:5e 1circ:124 1fr:210c " +
                "1ilbertSpace=210b 1opf=210d 2rizontalLine=2500 1scr=210b 2trok:126 " +
                "1umpDownHump=224e 4Equal=224f 0IEcy:415 1Jlig:132 1Ocy:401 1acute+cd 1circ+ce " +
                "2y:418 1dot:130 1fr=2111 1grave+cc 1m=2111 2acr:12a 3ginaryI=2148 2plies=21d2 " +
                "1nt:222c 3egral=222b 4rsection=22c2 2visibleComma=2063 9Times=2062 1ogon:12e " +
                "2pf:1d540 2ta:399 1scr=2110 1tilde:128 1ukcy:406 2ml+cf 0Jcirc:134 2y:419 " +
                "1fr:1d50d 1opf:1d541 1scr:1d4a5 2ercy:408 1ukcy:404 0KHcy:425 1Jcy:40c " +
                "1appa:39a 1cedil:136 2y:41a 1fr:1d50e 1opf:1d542 1scr:1d4a6 0LJcy:409 1T-3c " +
                "1acute:139 2mbda:39b 2ng:27ea 2placetrf=2112 2rr=219e 1caron:13d 2edil:13b " +
                "2y:41b 1eftAngleBracket=27e8 5rrow=2190 9Bar=21e4 9RightArrow=21c6 " +
                "4Ceiling=2308 4DoubleBracket=27e6 6wnTeeVector:2961 8Vector=21c3 eBar:2959 " +
                "4Floor=230a 4RightArrow=2194 9Vector:294e 4Tee=22a3 7Arrow=21a4 7Vector:295a " +
                "5riangle=22b2 cBar:29cf cEqual=22b4 4UpDownVector:2951 6TeeVector:2960 " +
                "6Vector=21bf cBar:2958 4Vector=21bc aBar:2952 4arrow=21d0 4rightarrow=21d4 " +
                "2ssEqualGreater=22da 4FullEqual=2266 4Greater=2276 4Less:2aa1 4SlantEqual=2a7d " +
                "4Tilde=2272 1fr:1d50f 1l:22d8 2eftarrow=21da 1midot:13f 1ongLeftArrow=27f5 " +
                "8RightArrow=27f7 4RightArrow=27f6 4leftarrow=27f8 8rightarrow=27fa " +
                "4rightarrow=27f9 2pf:1d543 2werLeftArrow=2199 5RightArrow=2198 1scr=2112 " +
                "2h=21b0 2trok:141 1t=226a 0Map:2905 1cy:41c 1ediumSpace:205f 2llintrf=2133 " +
                "1fr:1d510 1inusPlus=2213 1opf:1d544 1scr=2133 1u:39c 0NJcy:40a 1acute:143 " +
                "1caron:147 2edil:145 2y:41d 1egativeMediumSpace:200b 8ThickSpace=200b " +
                "bnSpace=200b 8VeryThinSpace=200b 2stedGreaterGreater=226b 6LessLess=226a " +
                "2wLine:a 1fr:1d511 1oBreak:2060 2nBreakingSpace=a0 2pf=2115 2t:2aec " +
                "3Congruent=2262 4upCap:226d 3DoubleVerticalBar=2226 3Element=2209 4qual=2260 " +
                "8Tilde=2242/338 4xists=2204 3Greater=226f aEqual=2271 aFullEqual=2267/338 " +
                "aGreater=226b/338 aLess=2279 aSlantEqual=2a7e/338 aTilde=2275 " +
                "3HumpDownHump=224e/338 7Equal=224f/338 3LeftTriangle=22ea fBar=29cf/338 " +
                "fEqual=22ec 5ss=226e 7Equal=2270 7Greater=2278 7Less=226a/338 " +
                "7SlantEqual=2a7d/338 7Tilde=2274 3NestedGreaterGreater=2aa2/338 " +
                "9LessLess=2aa1/338 3Precedes=2280 bEqual=2aaf/338 bSlantEqual=22e0 " +
                "3ReverseElement=220c 4ightTriangle=22eb gBar=29d0/338 gEqual=22ed " +
                "3SquareSubset=228f/338 fEqual=22e2 bperset=2290/338 hEqual=22e3 " +
                "4ubset=2282/20d2 9Equal=2288 5cceeds=2281 bEqual=2ab0/338 bSlantEqual=22e1 " +
                "bTilde=227f/338 5perset=2283/20d2 bEqual=2289 3Tilde=2241 8Equal=2244 " +
                "8FullEqual=2247 8Tilde=2249 3VerticalBar=2224 1scr:1d4a9 1tilde+d1 1u:39d " +
                "0OElig:152 1acute+d3 1circ+d4 2y:41e 1dblac:150 1fr:1d512 1grave+d2 1macr:14c " +
                "2ega:3a9 2icron:39f 1opf:1d546 1penCurlyDoubleQuote=201c 9Quote=2018 1r:2a54 " +
                "1scr:1d4aa 2lash+d8 1tilde+d5 3mes:2a37 1uml+d6 1verBar=203e 5race:23de " +
                "8ket=23b4 4Parenthesis:23dc 0PartialD=2202 1cy:41f 1fr:1d513 1hi:3a6 1i:3a0 " +
                "1lusMinus=b1 1oincareplane=210c 2pf=2119 1r:2abb 2ecedes=227a 8Equal=2aaf " +
                "8SlantEqual=227c 8Tilde=227e 2ime:2033 2oduct=220f 3portion=2237 aal=221d " +
                "1scr:1d4ab 2i:3a8 0QUOT-22 1fr:1d514 1opf=211a 1scr:1d4ac 0RBarr=2910 1EG-ae " +
                "1acute:154 2ng:27eb 2rr=21a0 4tl:2916 1caron:158 2edil:156 2y:420 1e=211c " +
                "2verseElement=220b 8quilibrium=21cb 7UpEquilibrium=296f 1fr=211c 1ho:3a1 " +
                "1ightAngleBracket=27e9 6rrow=2192 aBar=21e5 aLeftArrow=21c4 5Ceiling=2309 " +
                "5DoubleBracket=27e7 7wnTeeVector:295d 9Vector=21c2 fBar:2955 5Floor=230b " +
                "5Tee=22a2 8Arrow=21a6 8Vector:295b 6riangle=22b3 dBar:29d0 dEqual=22b5 " +
                "5UpDownVector:294f 7TeeVector:295c 7Vector=21be dBar:2954 5Vector=21c0 " +
                "bBar:2953 5arrow=21d2 1opf=211d 2undImplies:2970 1rightarrow=21db 1scr=211b " +
                "2h=21b1 1uleDelayed:29f4 0SHCHcy:429 2cy:428 1OFTcy:42c 1acute:15a 1c:2abc " +
                "2aron:160 2edil:15e 2irc:15c 2y:421 1fr:1d516 1hortDownArrow=2193 " +
                "5LeftArrow=2190 5RightArrow=2192 5UpArrow=2191 1igma:3a3 1mallCircle=2218 " +
                "1opf:1d54a 1qrt=221a 2uare=25a1 6Intersection=2293 6Subset=228f cEqual=2291 " +
                "8perset=2290 eEqual=2292 6Union=2294 1scr:1d4ae 1tar=22c6 1ub:22d0 3set=22d0 " +
                "6Equal=2286 2cceeds=227b 8Equal=2ab0 8SlantEqual=227d 8Tilde=227f 3hThat=220b " +
                "2m=2211 2p:22d1 3erset=2283 8Equal=2287 3set=22d1 0THORN+de 1RADE=2122 " +
                "1SHcy:40b 2cy:426 1ab:9 2u:3a4 1caron:164 2edil:162 2y:422 1fr:1d517 " +
                "1herefore=2234 3ta:398 2ickSpace=205f/200a 3nSpace=2009 1ilde=223c 5Equal=2243 " +
                "5FullEqual=2245 5Tilde=2248 1opf:1d54b 1ripleDot=20db 1scr:1d4af 2trok:166 " +
                "0Uacute+da 2rr:219f 4ocir:2949 1brcy:40e 3eve:16c 1circ+db 2y:423 1dblac:170 " +
                "1fr:1d518 1grave+d9 1macr:16a 1nderBar=5f 6race:23df 9ket=23b5 " +
                "5Parenthesis:23dd 2ion=22c3 5Plus=228e 1ogon:172 2pf:1d54c 1pArrow=2191 " +
                "7Bar:2912 7DownArrow=21c5 2DownArrow=2195 2Equilibrium=296e 2Tee=22a5 " +
                "5Arrow=21a5 2arrow=21d1 2downarrow=21d5 2perLeftArrow=2196 5RightArrow=2197 " +
                "2si=3d2 4lon:3a5 1ring:16e 1scr:1d4b0 1tilde:168 1uml+dc 0VDash:22ab 1bar:2aeb " +
                "1cy:412 1dash:22a9 5l:2ae6 1ee=22c1 2rbar:2016 3t=2016 4icalBar=2223 8Line=7c " +
                "8Separator:2758 8Tilde=2240 3yThinSpace=200a 1fr:1d519 1opf:1d54d 1scr:1d4b1 " +
                "1vdash:22aa 0Wcirc:174 1edge=22c0 1fr:1d51a 1opf:1d54e 1scr:1d4b2 0Xfr:1d51b " +
                "1i:39e 1opf:1d54f 1scr:1d4b3 0YAcy:42f 1Icy:407 1Ucy:42e 1acute+dd 1circ:176 " +
                "2y:42b 1fr:1d51c 1opf:1d550 1scr:1d4b4 1uml:178 0ZHcy:416 1acute:179 1caron:17d " +
                "2y:417 1dot:17b 1eroWidthSpace=200b 2ta:396 1fr=2128 1opf=2124 1scr:1d4b5 " +
                "0aacute+e1 1breve:103 1c:223e 2E=223e/333 2d:223f 2irc+e2 2ute+b4 2y:430 " +
                "1elig+e6 1f:2061 2r:1d51e 1grave+e0 1lefsym:2135 3ph=2135 2pha:3b1 1macr:101 " +
                "3lg:2a3f 2p+26 1nd:2227 3and:2a55 3d:2a5c 3slope:2a58 3v:2a5a 2g:2220 3e:29a4 " +
                "3le=2220 3msd:2221 6aa:29a8 7b:29a9 7c:29aa 7d:29ab 7e:29ac 7f:29ad 7g:29ae " +
                "7h:29af 3rt:221f 5vb:22be 7d:299d 3sph:2222 4t=c5 3zarr:237c 1ogon:105 " +
                "2pf:1d552 1p=2248 2E:2a70 2acir:2a6f 2e:224a 2id:224b 2os:27 2prox=2248 " +
                "6eq=224a 1ring+e5 1scr:1d4b6 2t:2a 2ymp:2248 5eq:224d 1tilde+e3 1uml+e4 " +
                "1wconint:2233 2int:2a11 0bNot:2aed 1ackcong:224c 4epsilon:3f6 4prime:2035 " +
                "4sim:223d 7eq:22cd 2rvee:22bd 3wed:2305 6ge=2305 1brk:23b5 4tbrk:23b6 " +
                "1cong=224c 2y:431 1dquo:201e 1ecaus:2235 6e=2235 2mptyv:29b0 2psi=3f6 " +
                "2rnou:212c 2ta:3b2 3h:2136 3ween:226c 1fr:1d51f 1igcap:22c2 4irc:25ef 4up:22c3 " +
                "3odot:2a00 4plus:2a01 4times:2a02 3sqcup:2a06 4tar:2605 3triangledown:25bd " +
                "bup:25b3 3uplus:2a04 3vee:22c1 3wedge:22c0 1karow:290d 1lacklozenge:29eb " +
                "5square:25aa 5triangle:25b4 ddown:25be dleft:25c2 dright:25b8 3nk:2423 " +
                "2k12:2592 44:2591 334:2593 2ock:2588 1ne=3d/20e5 3quiv=2261/20e5 2ot:2310 " +
                "1opf:1d553 2t=22a5 3tom=22a5 2wtie:22c8 2xDL:2557 4R:2554 4l:2556 4r:2553 " +
                "3H:2550 4D:2566 4U:2569 4d:2564 4u:2567 3UL:255d 4R:255a 4l:255c 4r:2559 " +
                "3V:2551 4H:256c 4L:2563 4R:2560 4h:256b 4l:2562 4r:255f 3box:29c9 3dL:2555 " +
                "4R:2552 4l:2510 4r:250c 3h:2500 4D:2565 4U:2568 4d:252c 4u:2534 3minus:229f " +
                "3plus:229e 3times:22a0 3uL:255b 4R:2558 4l:2518 4r:2514 3v:2502 4H:256a 4L:2561 " +
                "4R:255e 4h:253c 4l:2524 4r:251c 1prime=2035 1reve:2d8 2vbar+a6 1scr:1d4b7 " +
                "2emi:204f 2im=223d 4e=22cd 2ol:5c 4b:29c5 4hsub:27c8 1ull:2022 4et=2022 " +
                "2mp:224e 4E:2aae 4e:224f 5q=224f 0cacute:107 2p:2229 3and:2a44 3brcup:2a49 " +
                "3cap:2a4b 4up:2a47 3dot:2a40 3s=2229/fe00 2ret:2041 3on:2c7 1caps:2a4d 3ron:10d " +
                "2edil+e7 2irc:109 2ups:2a4c 5sm:2a50 1dot:10b 1edil+b8 2mptyv:29b2 2nt+a2 " +
                "4erdot=b7 1fr:1d520 1hcy:447 2eck:2713 5mark=2713 2i:3c7 1ir:25cb 3E:29c3 " +
                "3c:2c6 4eq:2257 4learrowleft:21ba bright:21bb 6dR=ae 7S:24c8 7ast:229b " +
                "7circ:229a 7dash:229d 3e=2257 3fnint:2a10 3mid:2aef 3scir:29c2 1lubs:2663 " +
                "5uit=2663 1olon:3a 5e:2254 6q=2254 2mma:2c 5t:40 3p:2201 4fn:2218 4lement=2201 " +
                "6xes:2102 2ng:2245 4dot:2a6d 3int:222e 2pf:1d554 3rod:2210 3y+a9 4sr:2117 " +
                "1rarr:21b5 2oss:2717 1scr:1d4b8 2ub:2acf 4e:2ad1 3p:2ad0 4e:2ad2 1tdot:22ef " +
                "1udarrl:2938 6r:2935 2epr:22de 3sc:22df 2larr:21b6 6p:293d 2p:222a 3brcap:2a48 " +
                "3cap:2a46 4up:2a4a 3dot:228d 3or:2a45 3s=222a/fe00 2rarr:21b7 6m:293c " +
                "3lyeqprec=22de 7succ=22df 5vee:22ce 5wedge:22cf 3ren+a4 3vearrowleft=21b6 " +
                "aright=21b7 2vee=22ce 2wed=22cf 1wconint:2232 2int:2231 1ylcty:232d 0dArr:21d3 " +
                "1Har:2965 1agger:2020 2leth:2138 2rr:2193 2sh:2010 4v:22a3 1bkarow:290f " +
                "2lac:2dd 1caron:10f 2y:434 1d:2146 2agger=2021 3rr:21ca 2otseq:2a77 1eg+b0 " +
                "2lta:3b4 2mptyv:29b1 1fisht:297f 2r:1d521 1harl:21c3 4r:21c2 1iam:22c4 " +
                "4ond=22c4 7suit=2666 4s:2666 2e=a8 2gamma:3dd 2sin:22f2 2v=f7 3ide+f7 " +
                "6ontimes:22c7 3onx=22c7 1jcy:452 1lcorn:231e 3rop:230d 1ollar:24 2pf:1d555 " +
                "2t:2d9 3eq:2250 5dot:2251 3minus:2238 3plus:2214 3square:22a1 " +
                "2ublebarwedge:2306 2wnarrow=2193 4downarrows=21ca 4harpoonleft=21c3 bright=21c2 " +
                "1rbkarow:2910 2corn:231f 3rop:230c 1scr:1d4b9 3y:455 2ol:29f6 2trok:111 " +
                "1tdot:22f1 2ri:25bf 4f=25be 1uarr:21f5 2har:296f 1wangle:29a6 1zcy:45f " +
                "2igrarr:27ff 0eDDot=2a77 2ot=2251 1acute+e9 2ster:2a6e 1caron:11b 2ir:2256 " +
                "4c+ea 2olon:2255 2y:44d 1dot:117 1e:2147 1fDot:2252 2r:1d522 1g:2a9a 2rave+e8 " +
                "2s:2a96 3dot:2a98 1l:2a99 2inters:23e7 2l:2113 2s:2a95 3dot:2a97 1macr:113 " +
                "2pty:2205 5set=2205 5v=2205 2sp:2003 413:2004 54:2005 1ng:14b 2sp:2002 " +
                "1ogon:119 2pf:1d556 1par:22d5 4sl:29e3 2lus:2a71 2si=3b5 4lon:3b5 4v:3f5 " +
                "1qcirc=2256 3olon=2255 2sim:2242 3lantgtr=2a96 7less=2a95 2uals:3d 3est:225f " +
                "3iv:2261 5DD:2a78 2vparsl:29e5 1rDot:2253 2arr:2971 1scr:212f 2dot=2250 " +
                "2im=2242 1ta:3b7 2h+f0 1uml+eb 2ro:20ac 1xcl:21 2ist:2203 2pectation:2130 " +
                "3onentiale=2147 0fallingdotseq=2252 1cy:444 1emale:2640 1filig:fb03 2lig:fb00 " +
                "3lig:fb04 2r:1d523 1ilig:fb01 1jlig=66/6a 1lat:266d 2lig:fb02 2tns:25b1 " +
                "1nof:192 1opf:1d557 2rall:2200 3k:22d4 4v:2ad9 1partint:2a0d 1rac12+bd 53:2153 " +
                "54+bc 55:2155 56:2159 58:215b 423:2154 55:2156 434+be 55:2157 58:215c 445:2158 " +
                "456:215a 58:215d 478:215e 3sl:2044 2own:2322 1scr:1d4bb 0gE:2267 2l:2a8c " +
                "1acute:1f5 2mma:3b3 5d=3dd 2p:2a86 1breve:11f 1circ:11d 2y:433 1dot:121 1e:2265 " +
                "2l:22db 2q=2265 3q=2267 3slant:2a7e 2s=2a7e 3cc:2aa9 3dot:2a80 6o:2a82 7l:2a84 " +
                "3l=22db/fe00 4es:2a94 1fr:1d524 1g:226b 2g:22d9 1imel:2137 1jcy:453 1l:2277 " +
                "2E:2a92 2a:2aa5 2j:2aa4 1nE:2269 2ap:2a8a 4prox=2a8a 2e:2a88 3q=2a88 4q=2269 " +
                "2sim:22e7 1opf:1d558 1rave:60 1scr:210a 2im:2273 4e:2a8e 4l:2a90 1t+3e 2cc:2aa7 " +
                "3ir:2a7a 2dot:22d7 2lPar:2995 2quest:2a7c 2rapprox=2a86 4rr:2978 3dot=22d7 " +
                "3eqless=22db 5qless=2a8c 3less=2277 3sim=2273 1vertneqq=2269/fe00 2nE=2269/fe00 " +
                "0hArr:21d4 1airsp:200a 2lf=bd 2milt:210b 2rdcy:44a 3r:2194 4cir:2948 4w:21ad " +
                "1bar:210f 1circ:125 1earts:2665 6uit=2665 2llip:2026 2rcon:22b9 1fr:1d525 " +
                "1ksearow:2925 3warow:2926 1oarr:21ff 2mtht:223b 2okleftarrow:21a9 " +
                "4rightarrow:21aa 2pf:1d559 2rbar:2015 1scr:1d4bd 2lash=210f 2trok:127 " +
                "1ybull:2043 2phen=2010 0iacute+ed 1c:2063 2irc+ee 2y:438 1ecy:435 2xcl+a1 " +
                "1ff=21d4 2r:1d526 1grave+ec 1i:2148 2iint:2a0c 3nt:222d 2nfin:29dc 2ota:2129 " +
                "1jlig:133 1macr:12b 3ge:2111 4line:2110 4part=2111 3th:131 2of:22b7 2ped:1b5 " +
                "1n=2208 2care:2105 2fin:221e 5tie:29dd 2odot=131 2t:222b 3cal:22ba 3egers:2124 " +
                "4rcal=22ba 3larhk:2a17 3prod:2a3c 1ocy:451 2gon:12f 2pf:1d55a 2ta:3b9 " +
                "1prod=2a3c 1quest+bf 1scr:1d4be 2in:2208 4E:22f9 4dot:22f5 4s:22f4 5v:22f3 " +
                "4v=2208 1t:2062 2ilde:129 1ukcy:456 2ml+ef 0jcirc:135 2y:439 1fr:1d527 " +
                "1math:237 1opf:1d55b 1scr:1d4bf 2ercy:458 1ukcy:454 0kappa:3ba 5v:3f0 " +
                "1cedil:137 2y:43a 1fr:1d528 1green:138 1hcy:445 1jcy:45c 1opf:1d55c 1scr:1d4c0 " +
                "0lAarr:21da 2rr:21d0 2tail:291b 1Barr:290e 1E:2266 2g:2a8b 1Har:2962 1acute:13a " +
                "2emptyv:29b4 2gran:2112 2mbda:3bb 2ng:27e8 4d:2991 4le=27e8 2p:2a85 2quo+ab " +
                "2rr:2190 4b:21e4 5fs:291f 4fs:291d 4hk=21a9 4lp:21ab 4pl:2939 4sim:2973 " +
                "4tl:21a2 2t:2aab 3ail:2919 3e:2aad 4s=2aad/fe00 1barr:290c 2brk:2772 2race:7b " +
                "5k:5b 3ke:298b 4sld:298f 6u:298d 1caron:13e 2edil:13c 3il:2308 2ub=7b 2y:43b " +
                "1dca:2936 2quo:201c 5r=201e 2rdhar:2967 3ushar:294b 2sh:21b2 1e:2264 " +
                "2ftarrow=2190 9tail=21a2 4harpoondown:21bd bup:21bc 4leftarrows:21c7 " +
                "4rightarrow=2194 es:21c6 9harpoons:21cb 9squigarrow=21ad 4threetimes:22cb " +
                "2g:22da 2q=2264 3q=2266 3slant:2a7d 2s=2a7d 3cc:2aa8 3dot:2a7f 6o:2a81 7r:2a83 " +
                "3g=22da/fe00 4es:2a93 3sapprox=2a85 4dot:22d6 4eqgtr=22da 6qgtr=2a8b 4gtr:2276 " +
                "4sim:2272 1fisht:297c 2loor:230a 2r:1d529 1g=2276 2E:2a91 1hard=21bd 4u=21bc " +
                "5l:296a 2blk:2584 1jcy:459 1l:226a 2arr=21c7 2corner=231e 2hard:296b 2tri:25fa " +
                "1midot:140 2oust:23b0 6ache=23b0 1nE:2268 2ap:2a89 4prox=2a89 2e:2a87 3q=2a87 " +
                "4q=2268 2sim:22e6 1oang:27ec 3rr:21fd 2brk:27e6 2ngleftarrow:27f5 " +
                "8rightarrow:27f7 4mapsto:27fc 4rightarrow:27f6 2oparrowleft=21ab 9right:21ac " +
                "2par:2985 3f:1d55d 3lus:2a2d 2times:2a34 2wast:2217 3bar:5f 2z:25ca 3enge=25ca " +
                "3f=29eb 1par:28 4lt:2993 1rarr=21c6 2corner=231f 2har=21cb 5d:296d 2m:200e " +
                "2tri:22bf 1saquo:2039 2cr:1d4c1 2h:21b0 2im=2272 4e:2a8d 4g:2a8f 2qb=5b " +
                "3uo:2018 5r=201a 2trok:142 1t+3c 2cc:2aa6 3ir:2a79 2dot=22d6 2hree=22cb " +
                "2imes:22c9 2larr:2976 2quest:2a7b 2rPar:2996 3i:25c3 4e:22b4 4f=25c2 " +
                "1urdshar:294a 3uhar:2966 1vertneqq=2268/fe00 2nE=2268/fe00 0mDDot:223a 1acr+af " +
                "2le:2642 3t:2720 4ese=2720 2p:21a6 3sto=21a6 6down:21a7 6left:21a4 6up:21a5 " +
                "2rker:25ae 1comma:2a29 2y:43c 1dash:2014 1easuredangle=2221 1fr:1d52a 1ho:2127 " +
                "1icro+b5 2d:2223 3ast=2a 3cir:2af0 3dot+b7 2nus:2212 5b=229f 5d=2238 6u:2a2a " +
                "1lcp:2adb 2dr=2026 1nplus:2213 1odels:22a7 2pf:1d55e 1p=2213 1scr:1d4c2 " +
                "2tpos=223e 1u:3bc 2ltimap:22b8 2map=22b8 0nGg=22d9/338 2t=226b/20d2 3v=226b/338 " +
                "1Leftarrow:21cd 5rightarrow:21ce 2l=22d8/338 2t=226a/20d2 3v=226a/338 " +
                "1Rightarrow:21cf 1VDash:22af 2dash:22ae 1abla:2207 2cute:144 2ng=2220/20d2 " +
                "2p:2249 3E=2a70/338 3id=224b/338 3os:149 3prox=2249 2tur:266e 5al=266e 7s:2115 " +
                "1bsp+a0 2ump=224e/338 5e=224f/338 1cap:2a43 3ron:148 2edil:146 2ong:2247 " +
                "5dot=2a6d/338 2up:2a42 2y:43d 1dash:2013 1e:2260 2Arr:21d7 2arhk:2924 4r:2197 " +
                "5ow=2197 2dot=2250/338 2quiv:2262 2sear:2928 3im=2242/338 2xist:2204 6s=2204 " +
                "1fr:1d52b 1gE=2267/338 2e:2271 3q=2271 4q=2267/338 4slant=2a7e/338 3s=2a7e/338 " +
                "2sim:2275 2t:226f 3r=226f 1hArr=21ce 2arr:21ae 2par:2af2 1i:220b 2s:22fc " +
                "3d:22fa 2v=220b 1jcy:45a 1lArr=21cd 2E=2266/338 2arr:219a 2dr:2025 2e:2270 " +
                "3ftarrow=219a 5rightarrow=21ae 3q=2270 4q=2266/338 4slant=2a7d/338 3s=2a7d/338 " +
                "4s:226e 2sim:2274 2t=226e 3ri:22ea 5e:22ec 1mid:2224 1opf:1d55f 2t+ac 3in:2209 " +
                "5E=22f9/338 5dot=22f5/338 5va=2209 6b:22f7 6c:22f6 3ni:220c 5va=220c 6b:22fe " +
                "6c:22fd 1par:2226 4allel=2226 4sl=2afd/20e5 4t=2202/338 2olint:2a14 2r:2280 " +
                "3cue:22e0 3e=2aaf/338 4c=2280 5eq=2aaf/338 1rArr=21cf 2arr:219b 5c=2933/338 " +
                "5w=219d/338 2ightarrow=219b 2tri:22eb 5e:22ed 1sc:2281 3cue:22e1 3e=2ab0/338 " +
                "3r:1d4c3 2hortmid=2224 6parallel=2226 2im:2241 4e:2244 5q=2244 2mid=2224 " +
                "2par=2226 2qsube:22e2 5pe:22e3 2ub:2284 4E=2ac5/338 4e:2288 4set=2282/20d2 " +
                "7eq=2288 9q=2ac5/338 3cc=2281 5eq=2ab0/338 3p:2285 4E=2ac6/338 4e:2289 " +
                "4set=2283/20d2 7eq=2289 9q=2ac6/338 1tgl:2279 2ilde+f1 2lg:2278 " +
                "2riangleleft=22ea deq=22ec 9right=22eb eeq=22ed 1u:3bd 2m:23 3ero:2116 3sp:2007 " +
                "1vDash:22ad 2Harr:2904 2ap=224d/20d2 2dash:22ac 2ge=2265/20d2 3t=3e/20d2 " +
                "2infin:29de 2lArr:2902 3e=2264/20d2 3t=3c/20d2 4rie=22b4/20d2 2rArr:2903 " +
                "3trie=22b5/20d2 2sim=223c/20d2 1wArr:21d6 2arhk:2923 4r:2196 5ow=2196 " +
                "2near:2927 0oS=24c8 1acute+f3 2st=229b 1cir=229a 4c+f4 2y:43e 1dash=229d " +
                "2blac:151 2iv:2a38 2ot:2299 2sold:29bc 1elig:153 1fcir:29bf 2r:1d52c 1gon:2db " +
                "2rave+f2 2t:29c1 1hbar:29b5 2m=3a9 1int=222e 1larr=21ba 2cir:29be 3ross:29bb " +
                "2ine:203e 2t:29c0 1macr:14d 2ega:3c9 2icron:3bf 3d:29b6 3nus:2296 1opf:1d560 " +
                "1par:29b7 2erp:29b9 2lus:2295 1r:2228 2arr=21bb 2d:2a5d 3er:2134 5of=2134 3f+aa " +
                "3m+ba 2igof:22b6 2or:2a56 2slope:2a57 2v:2a5b 1scr=2134 2lash+f8 2ol:2298 " +
                "1tilde+f5 3mes:2297 6as:2a36 1uml+f6 1vbar:233d 0par:2225 3a+b6 4llel=2225 " +
                "3sim:2af3 4l:2afd 3t:2202 1cy:43f 1ercnt:25 3iod:2e 3mil:2030 3p:22a5 " +
                "3tenk:2031 1fr:1d52d 1hi:3c6 3v:3d5 2mmat:2133 2one:260e 1i:3c0 2tchfork=22d4 " +
                "2v:3d6 1lanck=210f 6h:210e 4kv=210f 2us:2b 4acir:2a23 4b=229e 4cir:2a22 " +
                "4do=2214 5u:2a25 4e:2a72 4mn+b1 4sim:2a26 4two:2a27 1m=b1 1ointint:2a15 " +
                "2pf:1d561 2und+a3 1r:227a 2E:2ab3 2ap:2ab7 2cue:227c 2e:2aaf 3c=227a " +
                "4approx=2ab7 4curlyeq=227c 4eq=2aaf 4napprox:2ab9 5eqq:2ab5 5sim:22e8 4sim:227e " +
                "2ime:2032 5s:2119 2nE=2ab5 3ap=2ab9 3sim=22e8 2od:220f 3falar:232e 4line:2312 " +
                "4surf:2313 3p:221d 4to=221d 2sim=227e 2urel:22b0 1scr:1d4c5 2i:3c8 1uncsp:2008 " +
                "0qfr:1d52e 1int=2a0c 1opf:1d562 1prime:2057 1scr:1d4c6 1uaternions:210d " +
                "4int:2a16 2est:3f 5eq=225f 2ot+22 0rAarr:21db 2rr:21d2 2tail:291c 1Barr=290f " +
                "1Har:2964 1ace=223d/331 3ute:155 2dic:221a 2emptyv:29b3 2ng:27e9 4d:2992 " +
                "4e:29a5 4le=27e9 2quo+bb 2rr:2192 4ap:2975 4b:21e5 5fs:2920 4c:2933 4fs:291e " +
                "4hk=21aa 4lp=21ac 4pl:2945 4sim:2974 4tl:21a3 4w:219d 2tail:291a 3io:2236 " +
                "5nals:211a 1barr=290d 2brk:2773 2race:7d 5k:5d 3ke:298c 4sld:298e 6u:2990 " +
                "1caron:159 2edil:157 3il:2309 2ub=7d 2y:440 1dca:2937 2ldhar:2969 2quo:201d " +
                "5r=201d 2sh:21b3 1eal:211c 4ine:211b 4part=211c 4s:211d 2ct:25ad 2g+ae " +
                "1fisht:297d 2loor:230b 2r:1d52f 1hard:21c1 4u:21c0 5l:296c 2o:3c1 3v:3f1 " +
                "1ightarrow=2192 atail=21a3 5harpoondown=21c1 cup=21c0 5leftarrows:21c4 " +
                "9harpoons:21cc 5rightarrows:21c9 5squigarrow=219d 5threetimes:22cc 2ng:2da " +
                "2singdotseq=2253 1larr=21c4 2har=21cc 2m:200f 1moust:23b1 6ache=23b1 1nmid:2aee " +
                "1oang:27ed 3rr:21fe 2brk:27e7 2par:2986 3f:1d563 3lus:2a2e 2times:2a35 1par:29 " +
                "4gt:2994 2polint:2a12 1rarr=21c9 1saquo:203a 2cr:1d4c7 2h:21b1 2qb=5d 3uo:2019 " +
                "5r=2019 1three=22cc 2imes:22ca 2ri:25b9 4e:22b5 4f=25b8 4ltri:29ce 1uluhar:2968 " +
                "1x:211e 0sacute:15b 1bquo:201a 1c:227b 2E:2ab4 2ap:2ab8 3ron:161 2cue:227d " +
                "2e:2ab0 3dil:15f 2irc:15d 2nE:2ab6 3ap:2aba 3sim:22e9 2polint:2a13 2sim:227f " +
                "2y:441 1dot:22c5 4b=22a1 4e:2a66 1eArr:21d8 2arhk=2925 4r:2198 5ow=2198 2ct+a7 " +
                "2mi:3b 2swar:2929 2tminus:2216 4n=2216 2xt:2736 1fr:1d530 3own=2322 1harp:266f " +
                "2chcy:449 3y:448 2ortmid=2223 5parallel=2225 2y+ad 1igma:3c3 5f:3c2 5v=3c2 " +
                "2m:223c 3dot:2a6a 3e:2243 4q=2243 3g:2a9e 4E:2aa0 3l:2a9d 4E:2a9f 3ne:2246 " +
                "3plus:2a24 3rarr:2972 1larr=2190 1mallsetminus=2216 3shp:2a33 2eparsl:29e4 " +
                "2id=2223 3le:2323 2t:2aaa 3e:2aac 4s=2aac/fe00 1oftcy:44c 2l:2f 3b:29c4 " +
                "4ar:233f 2pf:1d564 1pades:2660 6uit=2660 3r=2225 1qcap:2293 5s=2293/fe00 " +
                "3up:2294 5s=2294/fe00 2sub:228f 5e:2291 5set=228f 8eq=2291 4p:2290 5e:2292 " +
                "5set=2290 8eq=2292 2u:25a1 3are=25a1 5f=25aa 3f=25aa 1rarr=2192 1scr:1d4c8 " +
                "2etmn=2216 2mile=2323 2tarf:22c6 1tar:2606 4f=2605 2raightepsilon=3f5 8phi=3d5 " +
                "3ns=af 1ub:2282 3E:2ac5 3dot:2abd 3e:2286 4dot:2ac3 3mult:2ac1 3nE:2acb 4e:228a " +
                "3plus:2abf 3rarr:2979 3set=2282 6eq=2286 8q=2ac5 6neq=228a 9q=2acb 4im:2ac7 " +
                "4ub:2ad5 5p:2ad3 2cc=227b 4approx=2ab8 4curlyeq=227d 4eq=2ab0 4napprox=2aba " +
                "5eqq=2ab6 5sim=22e9 4sim=227f 2m:2211 2ng:266a 2p:2283 31+b9 32+b2 33+b3 " +
                "3E:2ac6 3dot:2abe 4sub:2ad8 3e:2287 4dot:2ac4 3hsol:27c9 5ub:2ad7 3larr:297b " +
                "3mult:2ac2 3nE:2acc 4e:228b 3plus:2ac0 3set=2283 6eq=2287 8q=2ac6 6neq=228b " +
                "9q=2acc 4im:2ac8 4ub:2ad4 5p:2ad6 1wArr:21d9 2arhk=2926 4r:2199 5ow=2199 " +
                "2nwar:292a 1zlig+df 0target:2316 2u:3c4 1brk:23b4 1caron:165 2edil:163 2y:442 " +
                "1dot:20db 1elrec:2315 1fr:1d531 1here4:2234 5fore=2234 3ta:3b8 5sym:3d1 5v=3d1 " +
                "2ickapprox=2248 5sim=223c 3nsp:2009 2kap=2248 3sim=223c 2orn+fe 1ilde:2dc " +
                "2mes+d7 5b=22a0 6ar:2a31 5d:2a30 2nt=222d 1oea=2928 2p:22a4 3bot:2336 3cir:2af1 " +
                "3f:1d565 4ork:2ada 2sa=2929 1prime:2034 1rade:2122 2iangle:25b5 8down=25bf " +
                "8left=25c3 ceq=22b4 8q:225c 8right=25b9 deq=22b5 3dot:25ec 3e=225c 3minus:2a3a " +
                "3plus:2a39 3sb:29cd 3time:2a3b 2pezium:23e2 1scr:1d4c9 3y:446 2hcy:45b " +
                "2trok:167 1wixt=226c 2oheadleftarrow:219e 7rightarrow:21a0 0uArr:21d1 1Har:2963 " +
                "1acute+fa 2rr:2191 1brcy:45e 3eve:16d 1circ+fb 2y:443 1darr:21c5 2blac:171 " +
                "2har:296e 1fisht:297e 2r:1d532 1grave+f9 1harl:21bf 4r:21be 2blk:2580 " +
                "1lcorn:231c 6er=231c 3rop:230f 2tri:25f8 1macr:16b 2l+a8 1ogon:173 2pf:1d566 " +
                "1parrow=2191 2downarrow:2195 2harpoonleft=21bf 9right=21be 2lus:228e 2si=3c5 " +
                "4h:3d2 4lon:3c5 2uparrows:21c8 1rcorn:231d 6er=231d 3rop:230e 2ing:16f " +
                "2tri:25f9 1scr:1d4ca 1tdot:22f0 2ilde:169 2ri=25b5 4f=25b4 1uarr=21c8 2ml+fc " +
                "1wangle:29a7 0vArr:21d5 1Bar:2ae8 4v:2ae9 1Dash:22a8 1angrt:299c 2repsilon=3f5 " +
                "3kappa=3f0 3nothing=2205 3phi=3d5 4i=3d6 4ropto=221d 3r=2195 4ho=3f1 3sigma=3c2 " +
                "4ubsetneq=228a/fe00 cq=2acb/fe00 5psetneq=228b/fe00 cq=2acc/fe00 3theta=3d1 " +
                "4riangleleft:22b2 bright:22b3 1cy:432 1dash:22a2 1ee=2228 3bar:22bb 3eq:225a " +
                "2llip:22ee 2rbar:7c 3t=7c 1fr:1d533 1ltri=22b2 1nsub=2282/20d2 4p=2283/20d2 " +
                "1opf:1d567 1prop=221d 1rtri=22b3 1scr:1d4cb 2ubnE=2acb/fe00 5e=228a/fe00 " +
                "3pnE=2acc/fe00 5e=228b/fe00 1zigzag:299a 0wcirc:175 1edbar:2a5f 3ge=2227 " +
                "5q:2259 2ierp:2118 1fr:1d534 1opf:1d568 1p=2118 1r:2240 2eath=2240 1scr:1d4cc " +
                "0xcap=22c2 2irc=25ef 2up=22c3 1dtri=25bd 1fr:1d535 1hArr:27fa 2arr=27f7 1i:3be " +
                "1lArr:27f8 2arr=27f5 1map=27fc 1nis:22fb 1odot=2a00 2pf:1d569 3lus=2a01 " +
                "2time=2a02 1rArr:27f9 2arr=27f6 1scr:1d4cd 2qcup=2a06 1uplus=2a04 2tri=25b3 " +
                "1vee=22c1 1wedge=22c0 0yacute+fd 3y:44f 1circ:177 2y:44b 1en+a5 1fr:1d536 " +
                "1icy:457 1opf:1d56a 1scr:1d4ce 1ucy:44e 2ml+ff 0zacute:17a 1caron:17e 2y:437 " +
                "1dot:17c 1eetrf:2128 2ta:3b6 1fr:1d537 1hcy:436 1igrarr:21dd 1opf:1d56b " +
                "1scr:1d4cf 1wj:200d 2nj:200c");
    }

    /**
     * Returns true if and only if this string contains the specified
     * sequence of char values.
     *
     * @param s the sequence to search for
     * @return true if this string contains {@code s}, false otherwise
     * @since 1.5
     */
    public boolean contains(CharSequence s) {
        return indexOf(s.toString()) >= 0;
    }

    /**
     * Replaces the first substring of this string that matches the given <a
     * href="../util/regex/Pattern.html#sum">regular expression</a> with the
     * given replacement.
     *
     * <p> An invocation of this method of the form
     * <i>str</i>{@code .replaceFirst(}<i>regex</i>{@code ,} <i>repl</i>{@code )}
     * yields exactly the same result as the expression
     *
     * <blockquote>
     * <code>
     * {@link java.util.regex.Pattern}.{@link
     * java.util.regex.Pattern#compile(String) compile}(<i>regex</i>).{@link
     * java.util.regex.Pattern#matcher(java.lang.CharSequence) matcher}(<i>str</i>).{@link
     * java.util.regex.Matcher#replaceFirst(String) replaceFirst}(<i>repl</i>)
     * </code>
     * </blockquote>
     *
     *<p>
     * Note that backslashes ({@code \}) and dollar signs ({@code $}) in the
     * replacement string may cause the results to be different than if it were
     * being treated as a literal replacement string; see
     * {@link java.util.regex.Matcher#replaceFirst}.
     * Use {@link java.util.regex.Matcher#quoteReplacement} to suppress the special
     * meaning of these characters, if desired.
     *
     * @param   regex
     *          the regular expression to which this string is to be matched
     * @param   replacement
     *          the string to be substituted for the first match
     *
     * @return  The resulting {@code String}
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     */
    public String replaceFirst(String regex, String replacement) {
        M3String storage = m3();
        if (storage != null && M3String.isLiteralRegexReplacement(regex, replacement)) {
            String replaced = storage.replaceLiteralRegex(this, regex, replacement, true);
            if (replaced != null) return replaced;
        }
        return compiledRegex(regex).matcher(this).replaceFirst(replacement);
    }

    /**
     * Replaces each substring of this string that matches the given <a
     * href="../util/regex/Pattern.html#sum">regular expression</a> with the
     * given replacement.
     *
     * <p> An invocation of this method of the form
     * <i>str</i>{@code .replaceAll(}<i>regex</i>{@code ,} <i>repl</i>{@code )}
     * yields exactly the same result as the expression
     *
     * <blockquote>
     * <code>
     * {@link java.util.regex.Pattern}.{@link
     * java.util.regex.Pattern#compile(String) compile}(<i>regex</i>).{@link
     * java.util.regex.Pattern#matcher(java.lang.CharSequence) matcher}(<i>str</i>).{@link
     * java.util.regex.Matcher#replaceAll(String) replaceAll}(<i>repl</i>)
     * </code>
     * </blockquote>
     *
     *<p>
     * Note that backslashes ({@code \}) and dollar signs ({@code $}) in the
     * replacement string may cause the results to be different than if it were
     * being treated as a literal replacement string; see
     * {@link java.util.regex.Matcher#replaceAll Matcher.replaceAll}.
     * Use {@link java.util.regex.Matcher#quoteReplacement} to suppress the special
     * meaning of these characters, if desired.
     *
     * @param   regex
     *          the regular expression to which this string is to be matched
     * @param   replacement
     *          the string to be substituted for each match
     *
     * @return  The resulting {@code String}
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     */
    public String replaceAll(String regex, String replacement) {
        M3String storage = m3();
        if (storage != null && M3String.isLiteralRegexReplacement(regex, replacement)) {
            String replaced = storage.replaceLiteralRegex(this, regex, replacement, false);
            if (replaced != null) return replaced;
        }
        return compiledRegex(regex).matcher(this).replaceAll(replacement);
    }

    /**
     * Replaces each substring of this string that matches the literal target
     * sequence with the specified literal replacement sequence. The
     * replacement proceeds from the beginning of the string to the end, for
     * example, replacing "aa" with "b" in the string "aaa" will result in
     * "ba" rather than "ab".
     *
     * @param  target The sequence of char values to be replaced
     * @param  replacement The replacement sequence of char values
     * @return  The resulting string
     * @since 1.5
     */
    public String replace(CharSequence target, CharSequence replacement) {
        String trgtStr = target.toString();
        String replStr = replacement.toString();
        int thisLen = length();
        int trgtLen = trgtStr.length();
        int replLen = replStr.length();

        if (trgtLen > 0) {
            if (trgtLen == 1 && replLen == 1) {
                return replace(trgtStr.charAt(0), replStr.charAt(0));
            }

            M3String storage = m3();
            M3String targetM3 = trgtStr.m3();
            M3String replacementM3 = replStr.m3();
            if (storage != null || targetM3 != null || replacementM3 != null) {
                String replaced = m3Replace(storage, trgtStr, targetM3, replStr, replacementM3);
                if (replaced != null) return replaced;
            }

            boolean thisIsLatin1 = this.isLatin1();
            boolean trgtIsLatin1 = trgtStr.isLatin1();
            boolean replIsLatin1 = replStr.isLatin1();
            String ret = (thisIsLatin1 && trgtIsLatin1 && replIsLatin1)
                    ? StringLatin1.replace(value(), thisLen,
                                           trgtStr.value(), trgtLen,
                                           replStr.value(), replLen)
                    : StringUTF16.replace(value(), thisLen, thisIsLatin1,
                                          trgtStr.value(), trgtLen, trgtIsLatin1,
                                          replStr.value(), replLen, replIsLatin1);
            if (ret != null) {
                return ret;
            }
            return this;

        } else { // trgtLen == 0
            int resultLen;
            try {
                resultLen = Math.addExact(thisLen, Math.multiplyExact(
                        Math.addExact(thisLen, 1), replLen));
            } catch (ArithmeticException ignored) {
                throw new OutOfMemoryError("Required length exceeds implementation limit");
            }

            M3String storage = m3();
            M3String replacementM3 = replStr.m3();
            if (storage != null || replacementM3 != null) {
                M3String sourceM3 = storage != null ? storage : M3String.canonicalize(this);
                if (replacementM3 == null) replacementM3 = M3String.canonicalize(replStr);
                // A refused admission (pool budget) leaves the flat path below to build the result.
                if (sourceM3 != null && replacementM3 != null) {
                    return new String(sourceM3.replaceEmptyTarget(replacementM3));
                }
            }

            StringBuilder sb = new StringBuilder(resultLen);
            sb.append(replStr);
            for (int i = 0; i < thisLen; ++i) {
                sb.append(charAt(i)).append(replStr);
            }
            return sb.toString();
        }
    }

    /**
     * M3 lane of {@link #replace(CharSequence, CharSequence)} for a non-empty target. The match
     * is located through the mixed-side search before anything is admitted, so an absent target
     * keeps this String's identity without admitting the target or the replacement; a flat
     * target is never admitted (transient: it only drives the search); the replacement joins the
     * result. Returns {@code null} when the pool refuses an admission (budget), in which case the
     * flat compatibility path computes the same result.
     */
    private String m3Replace(M3String storage, String trgtStr, M3String targetM3,
                             String replStr, M3String replacementM3) {
        int found = indexOf(trgtStr);
        if (found < 0) return this;
        M3String sourceM3 = storage != null ? storage : M3String.canonicalize(this);
        if (sourceM3 == null) return null;
        M3String replacement = replacementM3 != null ? replacementM3 : M3String.canonicalize(replStr);
        if (replacement == null) return null;
        return new String(targetM3 != null
                ? sourceM3.replaceAt(targetM3, replacement, found)
                : sourceM3.replaceFlatTarget(this, trgtStr, replacement, found));
    }

    /**
     * Splits this string around matches of the given
     * <a href="../util/regex/Pattern.html#sum">regular expression</a>.
     *
     * <p> The array returned by this method contains each substring of this
     * string that is terminated by another substring that matches the given
     * expression or is terminated by the end of the string.  The substrings in
     * the array are in the order in which they occur in this string.  If the
     * expression does not match any part of the input then the resulting array
     * has just one element, namely this string.
     *
     * <p> When there is a positive-width match at the beginning of this
     * string then an empty leading substring is included at the beginning
     * of the resulting array. A zero-width match at the beginning however
     * never produces such empty leading substring.
     *
     * <p> The {@code limit} parameter controls the number of times the
     * pattern is applied and therefore affects the length of the resulting
     * array.
     * <ul>
     *    <li><p>
     *    If the <i>limit</i> is positive then the pattern will be applied
     *    at most <i>limit</i>&nbsp;-&nbsp;1 times, the array's length will be
     *    no greater than <i>limit</i>, and the array's last entry will contain
     *    all input beyond the last matched delimiter.</p></li>
     *
     *    <li><p>
     *    If the <i>limit</i> is zero then the pattern will be applied as
     *    many times as possible, the array can have any length, and trailing
     *    empty strings will be discarded.</p></li>
     *
     *    <li><p>
     *    If the <i>limit</i> is negative then the pattern will be applied
     *    as many times as possible and the array can have any length.</p></li>
     * </ul>
     *
     * <p> The string {@code "boo:and:foo"}, for example, yields the
     * following results with these parameters:
     *
     * <blockquote><table class="plain">
     * <caption style="display:none">Split example showing regex, limit, and result</caption>
     * <thead>
     * <tr>
     *     <th scope="col">Regex</th>
     *     <th scope="col">Limit</th>
     *     <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">:</th>
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">2</th>
     *     <td>{@code { "boo", "and:foo" }}</td></tr>
     * <tr><!-- : -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><!-- : -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-2</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">o</th>
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     *     <td>{@code { "b", "", ":and:f", "", "" }}</td></tr>
     * <tr><!-- o -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-2</th>
     *     <td>{@code { "b", "", ":and:f", "", "" }}</td></tr>
     * <tr><!-- o -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">0</th>
     *     <td>{@code { "b", "", ":and:f" }}</td></tr>
     * </tbody>
     * </table></blockquote>
     *
     * <p> An invocation of this method of the form
     * <i>str.</i>{@code split(}<i>regex</i>{@code ,}&nbsp;<i>n</i>{@code )}
     * yields the same result as the expression
     *
     * <blockquote>
     * <code>
     * {@link java.util.regex.Pattern}.{@link
     * java.util.regex.Pattern#compile(String) compile}(<i>regex</i>).{@link
     * java.util.regex.Pattern#split(java.lang.CharSequence,int) split}(<i>str</i>,&nbsp;<i>n</i>)
     * </code>
     * </blockquote>
     *
     *
     * @param  regex
     *         the delimiting regular expression
     *
     * @param  limit
     *         the result threshold, as described above
     *
     * @return  the array of strings computed by splitting this string
     *          around matches of the given regular expression
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     */
    public String[] split(String regex, int limit) {
        return split(regex, limit, false);
    }

    /**
     * Splits this string around matches of the given regular expression and
     * returns both the strings and the matching delimiters.
     *
     * <p> The array returned by this method contains each substring of this
     * string that is terminated by another substring that matches the given
     * expression or is terminated by the end of the string.
     * Each substring is immediately followed by the subsequence (the delimiter)
     * that matches the given expression, <em>except</em> for the last
     * substring, which is not followed by anything.
     * The substrings in the array and the delimiters are in the order in which
     * they occur in the input.
     * If the expression does not match any part of the input then the resulting
     * array has just one element, namely this string.
     *
     * <p> When there is a positive-width match at the beginning of this
     * string then an empty leading substring is included at the beginning
     * of the resulting array. A zero-width match at the beginning however
     * never produces such empty leading substring nor the empty delimiter.
     *
     * <p> The {@code limit} parameter controls the number of times the
     * pattern is applied and therefore affects the length of the resulting
     * array.
     * <ul>
     *    <li> If the <i>limit</i> is positive then the pattern will be applied
     *    at most <i>limit</i>&nbsp;-&nbsp;1 times, the array's length will be
     *    no greater than 2 &times; <i>limit</i> - 1, and the array's last
     *    entry will contain all input beyond the last matched delimiter.</li>
     *
     *    <li> If the <i>limit</i> is zero then the pattern will be applied as
     *    many times as possible, the array can have any length, and trailing
     *    empty strings will be discarded.</li>
     *
     *    <li> If the <i>limit</i> is negative then the pattern will be applied
     *    as many times as possible and the array can have any length.</li>
     * </ul>
     *
     * <p> The input {@code "boo:::and::foo"}, for example, yields the following
     * results with these parameters:
     *
     * <table class="plain" style="margin-left:2em;">
     * <caption style="display:none">Split example showing regex, limit, and result</caption>
     * <thead>
     * <tr>
     *     <th scope="col">Regex</th>
     *     <th scope="col">Limit</th>
     *     <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">:+</th>
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">2</th>
     *     <td>{@code { "boo", ":::", "and::foo" }}</td></tr>
     * <tr><!-- : -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     *     <td>{@code { "boo", ":::", "and", "::", "foo" }}</td></tr>
     * <tr><!-- : -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-1</th>
     *     <td>{@code { "boo", ":::", "and", "::", "foo" }}</td></tr>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">o</th>
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     *     <td>{@code { "b", "o", "", "o", ":::and::f", "o", "", "o", "" }}</td></tr>
     * <tr><!-- o -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-1</th>
     *     <td>{@code { "b", "o", "", "o", ":::and::f", "o", "", "o", "" }}</td></tr>
     * <tr><!-- o -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">0</th>
     *     <td>{@code { "b", "o", "", "o", ":::and::f", "o", "", "o" }}</td></tr>
     * </tbody>
     * </table>
     *
     * @apiNote An invocation of this method of the form
     * <i>str.</i>{@code splitWithDelimiters(}<i>regex</i>{@code ,}&nbsp;<i>n</i>{@code )}
     * yields the same result as the expression
     *
     * <blockquote>
     * <code>
     * {@link java.util.regex.Pattern}.{@link
     * java.util.regex.Pattern#compile(String) compile}(<i>regex</i>).{@link
     * java.util.regex.Pattern#splitWithDelimiters(CharSequence,int) splitWithDelimiters}(<i>str</i>,&nbsp;<i>n</i>)
     * </code>
     * </blockquote>
     *
     * @param  regex
     *         the delimiting regular expression
     *
     * @param  limit
     *         the result threshold, as described above
     *
     * @return  the array of strings computed by splitting this string
     *          around matches of the given regular expression, alternating
     *          substrings and matching delimiters
     *
     * @since   21
     */
    public String[] splitWithDelimiters(String regex, int limit) {
        return split(regex, limit, true);
    }

    private String[] split(String regex, int limit, boolean withDelimiters) {
        /* fastpath if the regex is a
         * (1) one-char String and this character is not one of the
         *     RegEx's meta characters ".$|()[{^?*+\\", or
         * (2) two-char String and the first char is the backslash and
         *     the second is not the ascii digit or ascii letter.
         */
        char ch = 0;
        if (((regex.length() == 1 &&
                ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1) ||
                (regex.length() == 2 &&
                        regex.charAt(0) == '\\' &&
                        (((ch = regex.charAt(1))-'0')|('9'-ch)) < 0 &&
                        ((ch-'a')|('z'-ch)) < 0 &&
                        ((ch-'A')|('Z'-ch)) < 0)) &&
                (ch < Character.MIN_HIGH_SURROGATE ||
                        ch > Character.MAX_LOW_SURROGATE))
        {
            // All the checks above can potentially be constant folded by
            // a JIT/AOT compiler when the regex is a constant string.
            // That requires method inlining of the checks, which is only
            // possible when the actual split logic is in a separate method
            // because the large split loop can usually not be inlined.
            return split(ch, limit, withDelimiters);
        }
        // M3 literal lane: a regex with no metacharacter, escape or surrogate is the literal
        // itself, so the exact engine would only ever report its substring occurrences.
        if (M3String.isLiteralRegex(regex)) {
            return splitLiteral(regex, limit, withDelimiters);
        }
        Pattern pattern = compiledRegex(regex);
        return withDelimiters
                ? pattern.splitWithDelimiters(this, limit)
                : pattern.split(this, limit);
    }

    /**
     * Splits around occurrences of a non-empty literal, with exactly the semantics of
     * {@code Pattern.compile(literal).split(this, limit)}: occurrences are found left to right
     * without overlap, {@code limit} bounds the number of pieces, and a limit of zero drops
     * trailing empty strings. Only reached for literals admitted by
     * {@link M3String#isLiteralRegex(String)}.
     */
    private String[] splitLiteral(String literal, int limit, boolean withDelimiters) {
        int matchCount = 0;
        int off = 0;
        int next;
        int step = literal.length();
        boolean limited = limit > 0;
        ArrayList<String> list = new ArrayList<>();
        while ((next = indexOf(literal, off)) != -1) {
            if (!limited || matchCount < limit - 1) {
                list.add(substring(off, next));
                if (withDelimiters) {
                    list.add(literal);
                }
                off = next + step;
                ++matchCount;
            } else {    // last one
                int last = length();
                list.add(substring(off, last));
                off = last;
                ++matchCount;
                break;
            }
        }
        // If no match was found, return this
        if (off == 0)
            return new String[] {this};
        // Add remaining segment
        if (!limited || matchCount < limit)
            list.add(substring(off, length()));
        // Construct result
        int resultSize = list.size();
        if (limit == 0) {
            while (resultSize > 0 && list.get(resultSize - 1).isEmpty()) {
                resultSize--;
            }
        }
        String[] result = new String[resultSize];
        return list.subList(0, resultSize).toArray(result);
    }

    private String[] split(char ch, int limit, boolean withDelimiters) {
        int matchCount = 0;
        int off = 0;
        int next;
        boolean limited = limit > 0;
        ArrayList<String> list = new ArrayList<>();
        String del = withDelimiters ? String.valueOf(ch) : null;
        while ((next = indexOf(ch, off)) != -1) {
            if (!limited || matchCount < limit - 1) {
                list.add(substring(off, next));
                if (withDelimiters) {
                    list.add(del);
                }
                off = next + 1;
                ++matchCount;
            } else {    // last one
                int last = length();
                list.add(substring(off, last));
                off = last;
                ++matchCount;
                break;
            }
        }
        // If no match was found, return this
        if (off == 0)
            return new String[] {this};

        // Add remaining segment
        if (!limited || matchCount < limit)
            list.add(substring(off, length()));

        // Construct result
        int resultSize = list.size();
        if (limit == 0) {
            while (resultSize > 0 && list.get(resultSize - 1).isEmpty()) {
                resultSize--;
            }
        }
        String[] result = new String[resultSize];
        return list.subList(0, resultSize).toArray(result);
    }

    /**
     * Splits this string around matches of the given <a
     * href="../util/regex/Pattern.html#sum">regular expression</a>.
     *
     * <p> This method works as if by invoking the two-argument {@link
     * #split(String, int) split} method with the given expression and a limit
     * argument of zero.  Trailing empty strings are therefore not included in
     * the resulting array.
     *
     * <p> The string {@code "boo:and:foo"}, for example, yields the following
     * results with these expressions:
     *
     * <blockquote><table class="plain">
     * <caption style="display:none">Split examples showing regex and result</caption>
     * <thead>
     * <tr>
     *  <th scope="col">Regex</th>
     *  <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><th scope="row" style="text-weight:normal">:</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><th scope="row" style="text-weight:normal">o</th>
     *     <td>{@code { "b", "", ":and:f" }}</td></tr>
     * </tbody>
     * </table></blockquote>
     *
     *
     * @param  regex
     *         the delimiting regular expression
     *
     * @return  the array of strings computed by splitting this string
     *          around matches of the given regular expression
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     */
    public String[] split(String regex) {
        return split(regex, 0, false);
    }

    /**
     * Returns a new String composed of copies of the
     * {@code CharSequence elements} joined together with a copy of
     * the specified {@code delimiter}.
     *
     * <blockquote>For example,
     * <pre>{@code
     *     String message = String.join("-", "Java", "is", "cool");
     *     // message returned is: "Java-is-cool"
     * }</pre></blockquote>
     *
     * Note that if an element is null, then {@code "null"} is added.
     *
     * @param  delimiter the delimiter that separates each element
     * @param  elements the elements to join together.
     *
     * @return a new {@code String} that is composed of the {@code elements}
     *         separated by the {@code delimiter}
     *
     * @throws NullPointerException If {@code delimiter} or {@code elements}
     *         is {@code null}
     *
     * @see java.util.StringJoiner
     * @since 1.8
     */
    public static String join(CharSequence delimiter, CharSequence... elements) {
        var delim = delimiter.toString();
        var elems = new String[elements.length];
        for (int i = 0; i < elements.length; i++) {
            elems[i] = String.valueOf(elements[i]);
        }
        return join("", "", delim, elems, elems.length);
    }

    /**
     * Designated join routine.
     *
     * @param prefix the non-null prefix
     * @param suffix the non-null suffix
     * @param delimiter the non-null delimiter
     * @param elements the non-null array of non-null elements
     * @param size the number of elements in the array (<= elements.length)
     * @return the joined string
     */
    @ForceInline
    static String join(String prefix, String suffix, String delimiter, String[] elements, int size) {
        // A singleton with no affixes already owns immutable contiguous backing.
        // Preserve a fresh result object and its default hash/dedup state.
        if (size == 1 && prefix.isEmpty() && suffix.isEmpty()) {
            return new String(elements[0]);
        }
        int icoder = prefix.coder() | suffix.coder();
        long len = (long) prefix.length() + suffix.length();
        if (size > 1) { // when there are more than one element, size - 1 delimiters will be emitted
            len += (long) (size - 1) * delimiter.length();
            icoder |= delimiter.coder();
        }
        // assert len > 0L; // max: (long) Integer.MAX_VALUE << 32
        // following loop will add max: (long) Integer.MAX_VALUE * Integer.MAX_VALUE to len
        // so len can overflow at most once
        for (int i = 0; i < size; i++) {
            var el = elements[i];
            len += el.length();
            icoder |= el.coder();
        }
        byte coder = (byte) icoder;
        if (m3JoinedStringsEnabled()) {
            // Modified 2026-11-15: bound M3 joins by logical UTF-16 units before flat conversion.
            if (len < 0L || len > Integer.MAX_VALUE) {
                throw new OutOfMemoryError("Required length exceeds implementation limit");
            }
            M3String storage = M3String.joinDesignated(prefix, suffix, delimiter, elements, size);
            // null: the pool refused a flat piece (budget); the flat path below builds the result.
            if (storage != null) return new String(storage);
        }
        // long len overflow check, char -> byte length, int len overflow check for flat storage
        if (len < 0L || (len <<= coder) != (int) len) {
            throw new OutOfMemoryError("Requested string length exceeds VM limit");
        }
        byte[] value = StringConcatHelper.newArray(len);

        int off = 0;
        prefix.getBytes(value, off, coder); off += prefix.length();
        if (size > 0) {
            var el = elements[0];
            el.getBytes(value, off, coder); off += el.length();
            for (int i = 1; i < size; i++) {
                delimiter.getBytes(value, off, coder); off += delimiter.length();
                el = elements[i];
                el.getBytes(value, off, coder); off += el.length();
            }
        }
        suffix.getBytes(value, off, coder);
        // assert off + suffix.length() == value.length >> coder;

        return new String(value, coder);
    }

    /**
     * Returns a new {@code String} composed of copies of the
     * {@code CharSequence elements} joined together with a copy of the
     * specified {@code delimiter}.
     *
     * <blockquote>For example,
     * <pre>{@code
     *     List<String> strings = List.of("Java", "is", "cool");
     *     String message = String.join(" ", strings);
     *     // message returned is: "Java is cool"
     *
     *     Set<String> strings =
     *         new LinkedHashSet<>(List.of("Java", "is", "very", "cool"));
     *     String message = String.join("-", strings);
     *     // message returned is: "Java-is-very-cool"
     * }</pre></blockquote>
     *
     * Note that if an individual element is {@code null}, then {@code "null"} is added.
     *
     * @param  delimiter a sequence of characters that is used to separate each
     *         of the {@code elements} in the resulting {@code String}
     * @param  elements an {@code Iterable} that will have its {@code elements}
     *         joined together.
     *
     * @return a new {@code String} that is composed from the {@code elements}
     *         argument
     *
     * @throws NullPointerException If {@code delimiter} or {@code elements}
     *         is {@code null}
     *
     * @see    #join(CharSequence,CharSequence...)
     * @see    java.util.StringJoiner
     * @since 1.8
     */
    public static String join(CharSequence delimiter,
            Iterable<? extends CharSequence> elements) {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(elements);
        var delim = delimiter.toString();
        var elems = new String[8];
        int size = 0;
        for (CharSequence cs: elements) {
            if (size >= elems.length) {
                elems = Arrays.copyOf(elems, elems.length << 1);
            }
            elems[size++] = String.valueOf(cs);
        }
        return join("", "", delim, elems, size);
    }

    /**
     * Converts all of the characters in this {@code String} to lower
     * case using the rules of the given {@code Locale}.  Case mapping is based
     * on the Unicode Standard version specified by the {@link java.lang.Character Character}
     * class. Since case mappings are not always 1:1 char mappings, the resulting {@code String}
     * and this {@code String} may differ in length.
     * <p>
     * Examples of lowercase mappings are in the following table:
     * <table class="plain">
     * <caption style="display:none">Lowercase mapping examples showing language code of locale, upper case, lower case, and description</caption>
     * <thead>
     * <tr>
     *   <th scope="col">Language Code of Locale</th>
     *   <th scope="col">Upper Case</th>
     *   <th scope="col">Lower Case</th>
     *   <th scope="col">Description</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <td>tr (Turkish)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">&#92;u0130</th>
     *   <td>&#92;u0069</td>
     *   <td>capital letter I with dot above -&gt; small letter i</td>
     * </tr>
     * <tr>
     *   <td>tr (Turkish)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">&#92;u0049</th>
     *   <td>&#92;u0131</td>
     *   <td>capital letter I -&gt; small letter dotless i </td>
     * </tr>
     * <tr>
     *   <td>(all)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">French Fries</th>
     *   <td>french fries</td>
     *   <td>lowercased all chars in String</td>
     * </tr>
     * <tr>
     *   <td>(all)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">
     *       &Iota;&Chi;&Theta;&Upsilon;&Sigma;</th>
     *   <td>&iota;&chi;&theta;&upsilon;&sigma;</td>
     *   <td>lowercased all chars in String</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param locale use the case transformation rules for this locale
     * @return the {@code String}, converted to lowercase.
     * @see     java.lang.String#toLowerCase()
     * @see     java.lang.String#toUpperCase()
     * @see     java.lang.String#toUpperCase(Locale)
     * @since   1.1
     */
    public String toLowerCase(Locale locale) {
        Objects.requireNonNull(locale);
        M3String storage = m3();
        if (storage != null) {
            M3String mapped = storage.caseMap(this, locale, false);
            return mapped == storage ? this : new String(mapped);
        }
        byte[] currentValue = value();
        return isLatin1() ? StringLatin1.toLowerCase(this, currentValue, locale)
                          : StringUTF16.toLowerCase(this, currentValue, locale);
    }

    /**
     * Converts all of the characters in this {@code String} to lower
     * case using the rules of the default locale. This method is equivalent to
     * {@code toLowerCase(Locale.getDefault())}.
     *
     * @apiNote This method is locale sensitive, and may produce unexpected
     * results if used for strings that are intended to be interpreted locale
     * independently.
     * Examples are programming language identifiers, protocol keys, and HTML
     * tags.
     * For instance, {@code "TITLE".toLowerCase()} in a Turkish locale
     * returns {@code "t\u005Cu0131tle"}, where '\u005Cu0131' is the
     * LATIN SMALL LETTER DOTLESS I character.
     * To obtain correct results for locale insensitive strings, use
     * {@code toLowerCase(Locale.ROOT)}.
     *
     * @return  the {@code String}, converted to lowercase.
     * @see     java.lang.String#toLowerCase(Locale)
     */
    public String toLowerCase() {
        return toLowerCase(Locale.getDefault());
    }

    /**
     * Converts all of the characters in this {@code String} to upper
     * case using the rules of the given {@code Locale}. Case mapping is based
     * on the Unicode Standard version specified by the {@link java.lang.Character Character}
     * class. Since case mappings are not always 1:1 char mappings, the resulting {@code String}
     * and this {@code String} may differ in length.
     * <p>
     * Examples of locale-sensitive and 1:M case mappings are in the following table:
     * <table class="plain">
     * <caption style="display:none">Examples of locale-sensitive and 1:M case mappings. Shows Language code of locale, lower case, upper case, and description.</caption>
     * <thead>
     * <tr>
     *   <th scope="col">Language Code of Locale</th>
     *   <th scope="col">Lower Case</th>
     *   <th scope="col">Upper Case</th>
     *   <th scope="col">Description</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <td>tr (Turkish)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">&#92;u0069</th>
     *   <td>&#92;u0130</td>
     *   <td>small letter i -&gt; capital letter I with dot above</td>
     * </tr>
     * <tr>
     *   <td>tr (Turkish)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">&#92;u0131</th>
     *   <td>&#92;u0049</td>
     *   <td>small letter dotless i -&gt; capital letter I</td>
     * </tr>
     * <tr>
     *   <td>(all)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">&#92;u00df</th>
     *   <td>&#92;u0053 &#92;u0053</td>
     *   <td>small letter sharp s -&gt; two letters: SS</td>
     * </tr>
     * <tr>
     *   <td>(all)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">Fahrvergn&uuml;gen</th>
     *   <td>FAHRVERGN&Uuml;GEN</td>
     *   <td></td>
     * </tr>
     * </tbody>
     * </table>
     * @param locale use the case transformation rules for this locale
     * @return the {@code String}, converted to uppercase.
     * @see     java.lang.String#toUpperCase()
     * @see     java.lang.String#toLowerCase()
     * @see     java.lang.String#toLowerCase(Locale)
     * @since   1.1
     */
    public String toUpperCase(Locale locale) {
        Objects.requireNonNull(locale);
        M3String storage = m3();
        if (storage != null) {
            M3String mapped = storage.caseMap(this, locale, true);
            return mapped == storage ? this : new String(mapped);
        }
        byte[] currentValue = value();
        return isLatin1() ? StringLatin1.toUpperCase(this, currentValue, locale)
                          : StringUTF16.toUpperCase(this, currentValue, locale);
    }

    /**
     * Converts all of the characters in this {@code String} to upper
     * case using the rules of the default locale. This method is equivalent to
     * {@code toUpperCase(Locale.getDefault())}.
     *
     * @apiNote This method is locale sensitive, and may produce unexpected
     * results if used for strings that are intended to be interpreted locale
     * independently.
     * Examples are programming language identifiers, protocol keys, and HTML
     * tags.
     * For instance, {@code "title".toUpperCase()} in a Turkish locale
     * returns {@code "T\u005Cu0130TLE"}, where '\u005Cu0130' is the
     * LATIN CAPITAL LETTER I WITH DOT ABOVE character.
     * To obtain correct results for locale insensitive strings, use
     * {@code toUpperCase(Locale.ROOT)}.
     *
     * @return  the {@code String}, converted to uppercase.
     * @see     java.lang.String#toUpperCase(Locale)
     */
    public String toUpperCase() {
        return toUpperCase(Locale.getDefault());
    }

    /**
     * Returns a string whose value is this string, with all leading
     * and trailing space removed, where space is defined
     * as any character whose codepoint is less than or equal to
     * {@code 'U+0020'} (the space character).
     * <p>
     * If this {@code String} object represents an empty character
     * sequence, or the first and last characters of character sequence
     * represented by this {@code String} object both have codes
     * that are not space (as defined above), then a
     * reference to this {@code String} object is returned.
     * <p>
     * Otherwise, if all characters in this string are space (as
     * defined above), then a  {@code String} object representing an
     * empty string is returned.
     * <p>
     * Otherwise, let <i>k</i> be the index of the first character in the
     * string whose code is not a space (as defined above) and let
     * <i>m</i> be the index of the last character in the string whose code
     * is not a space (as defined above). A {@code String}
     * object is returned, representing the substring of this string that
     * begins with the character at index <i>k</i> and ends with the
     * character at index <i>m</i>-that is, the result of
     * {@code this.substring(k, m + 1)}.
     * <p>
     * This method may be used to trim space (as defined above) from
     * the beginning and end of a string.
     *
     * @return  a string whose value is this string, with all leading
     *          and trailing space removed, or this string if it
     *          has no leading or trailing space.
     */
    public String trim() {
        M3String storage = m3();
        if (storage != null) {
            return M3StringPrecomputeBridge.trim(this, storage);
        }
        byte[] currentValue = value();
        String ret = isLatin1() ? StringLatin1.trim(currentValue)
                                : StringUTF16.trim(currentValue);
        return ret == null ? this : ret;
    }

    /**
     * Returns a string whose value is this string, with all leading
     * and trailing {@linkplain Character#isWhitespace(int) white space}
     * removed.
     * <p>
     * If this {@code String} object represents an empty string,
     * or if all code points in this string are
     * {@linkplain Character#isWhitespace(int) white space}, then an empty string
     * is returned.
     * <p>
     * Otherwise, returns a substring of this string beginning with the first
     * code point that is not a {@linkplain Character#isWhitespace(int) white space}
     * up to and including the last code point that is not a
     * {@linkplain Character#isWhitespace(int) white space}.
     * <p>
     * This method may be used to strip
     * {@linkplain Character#isWhitespace(int) white space} from
     * the beginning and end of a string.
     *
     * @return  a string whose value is this string, with all leading
     *          and trailing white space removed
     *
     * @see Character#isWhitespace(int)
     *
     * @since 11
     */
    public String strip() {
        M3String storage = m3();
        if (storage != null) {
            return M3StringPrecomputeBridge.strip(this, storage);
        }
        byte[] currentValue = value();
        String ret = isLatin1() ? StringLatin1.strip(currentValue)
                                : StringUTF16.strip(currentValue);
        return ret == null ? this : ret;
    }

    /**
     * Returns a string whose value is this string, with all leading
     * {@linkplain Character#isWhitespace(int) white space} removed.
     * <p>
     * If this {@code String} object represents an empty string,
     * or if all code points in this string are
     * {@linkplain Character#isWhitespace(int) white space}, then an empty string
     * is returned.
     * <p>
     * Otherwise, returns a substring of this string beginning with the first
     * code point that is not a {@linkplain Character#isWhitespace(int) white space}
     * up to and including the last code point of this string.
     * <p>
     * This method may be used to trim
     * {@linkplain Character#isWhitespace(int) white space} from
     * the beginning of a string.
     *
     * @return  a string whose value is this string, with all leading white
     *          space removed
     *
     * @see Character#isWhitespace(int)
     *
     * @since 11
     */
    public String stripLeading() {
        M3String storage = m3();
        if (storage != null) {
            return M3StringPrecomputeBridge.stripLeading(this, storage);
        }
        byte[] currentValue = value();
        String ret = isLatin1() ? StringLatin1.stripLeading(currentValue)
                                : StringUTF16.stripLeading(currentValue);
        return ret == null ? this : ret;
    }

    /**
     * Returns a string whose value is this string, with all trailing
     * {@linkplain Character#isWhitespace(int) white space} removed.
     * <p>
     * If this {@code String} object represents an empty string,
     * or if all characters in this string are
     * {@linkplain Character#isWhitespace(int) white space}, then an empty string
     * is returned.
     * <p>
     * Otherwise, returns a substring of this string beginning with the first
     * code point of this string up to and including the last code point
     * that is not a {@linkplain Character#isWhitespace(int) white space}.
     * <p>
     * This method may be used to trim
     * {@linkplain Character#isWhitespace(int) white space} from
     * the end of a string.
     *
     * @return  a string whose value is this string, with all trailing white
     *          space removed
     *
     * @see Character#isWhitespace(int)
     *
     * @since 11
     */
    public String stripTrailing() {
        M3String storage = m3();
        if (storage != null) {
            return M3StringPrecomputeBridge.stripTrailing(this, storage);
        }
        byte[] currentValue = value();
        String ret = isLatin1() ? StringLatin1.stripTrailing(currentValue)
                                : StringUTF16.stripTrailing(currentValue);
        return ret == null ? this : ret;
    }

    /**
     * Returns {@code true} if the string is empty or contains only
     * {@linkplain Character#isWhitespace(int) white space} codepoints,
     * otherwise {@code false}.
     *
     * @return {@code true} if the string is empty or contains only
     *         {@linkplain Character#isWhitespace(int) white space} codepoints,
     *         otherwise {@code false}
     *
     * @see Character#isWhitespace(int)
     *
     * @since 11
     */
    public boolean isBlank() {
        return indexOfNonWhitespace() == length();
    }

    /**
     * Returns a stream of lines extracted from this string,
     * separated by line terminators.
     * <p>
     * A <i>line terminator</i> is one of the following:
     * a line feed character {@code "\n"} (U+000A),
     * a carriage return character {@code "\r"} (U+000D),
     * or a carriage return followed immediately by a line feed
     * {@code "\r\n"} (U+000D U+000A).
     * <p>
     * A <i>line</i> is either a sequence of zero or more characters
     * followed by a line terminator, or it is a sequence of one or
     * more characters followed by the end of the string. A
     * line does not include the line terminator.
     * <p>
     * The stream returned by this method contains the lines from
     * this string in the order in which they occur.
     *
     * @apiNote This definition of <i>line</i> implies that an empty
     *          string has zero lines and that there is no empty line
     *          following a line terminator at the end of a string.
     *
     * @implNote This method provides better performance than
     *           split("\R") by supplying elements lazily and
     *           by faster search of new line terminators.
     *
     * @return  the stream of lines extracted from this string
     *
     * @since 11
     */
    public Stream<String> lines() {
        if (m3() != null) {
            return StreamSupport.stream(new M3LinesSpliterator(this), false);
        }
        byte[] currentValue = value();
        return isLatin1() ? StringLatin1.lines(currentValue) : StringUTF16.lines(currentValue);
    }

    /**
     * Lazy line traversal for M3-backed Strings. It preserves String.lines() CR/LF/CRLF
     * semantics and emits M3 substring coordinates rather than flattening the source.
     */
    private static final class M3LinesSpliterator
            extends Spliterators.AbstractSpliterator<String> {
        private final String source;
        /** True when prepared facts prove the source holds no LF and no CR. */
        private final boolean terminatorFree;
        private int index;

        M3LinesSpliterator(String source) {
            super(source.length(), Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL);
            this.source = source;
            M3String storage = source.m3();
            M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
            this.terminatorFree = facts != null && facts.lineTerminatorCount == 0;
        }

        @Override
        public boolean tryAdvance(Consumer<? super String> action) {
            Objects.requireNonNull(action, "action");
            int length = source.length();
            if (index >= length) return false;

            int start = index;
            int end = terminatorFree ? length : start;
            while (end < length) {
                char unit = source.charAt(end);
                if (unit == '\n' || unit == '\r') break;
                end++;
            }

            action.accept(source.substring(start, end));
            if (end == length) {
                index = length;
            } else if (source.charAt(end) == '\r'
                    && end + 1 < length
                    && source.charAt(end + 1) == '\n') {
                index = end + 2;
            } else {
                index = end + 1;
            }
            return true;
        }
    }

    /**
     * Expands tab characters into spaces at tab stops eight columns apart.
     *
     * @return the expanded string, or this string if no tabs are present
     * @see #expandTabs(int)
     */
    public String expandTabs() {
        return expandTabs(8);
    }

    /**
     * Expands each horizontal tab into spaces up to the next tab stop.
     * The column starts at zero and resets after a carriage return or line feed.
     * Every Unicode code point other than a tab or line terminator occupies one
     * column, independently of its display width. A nonpositive tab size removes
     * tabs. Other characters and line terminators are preserved exactly.
     *
     * <p>This opt-in operation follows the column rules of Python's
     * {@code str.expandtabs}. M3-backed inputs retain their canonical range
     * composition, including ill-formed UTF-16, without flattening the source.
     *
     * @param tabSize the number of columns between tab stops
     * @return the expanded string, or this string if no tabs are present
     * @throws OutOfMemoryError if the expanded result cannot fit a String
     */
    public String expandTabs(int tabSize) {
        if (indexOf('\t') < 0) {
            return this;
        }
        M3String storage = M3String.canonicalize(this);
        if (storage != null) {
            return new String(storage.expandTabs(tabSize));
        }
        return expandTabsFlat(tabSize);
    }

    /** Budget-refused flat compatibility path; the M3 path composes ranges. */
    private String expandTabsFlat(int tabSize) {
        int sourceLength = length();
        int width = Math.max(0, tabSize);
        StringBuilder expanded = new StringBuilder(sourceLength);
        long resultLength = sourceLength;
        long column = 0;
        int start = 0;
        for (int index = 0; index < sourceLength;) {
            int codePoint = codePointAt(index);
            if (codePoint == '\t') {
                expanded.append(this, start, index);
                long spaces = width == 0 ? 0 : (long) width - column % width;
                resultLength += spaces - 1;
                if (resultLength > Integer.MAX_VALUE) {
                    throw new OutOfMemoryError("Expanded String exceeds implementation limit");
                }
                if (spaces != 0) {
                    expanded.append(" ".repeat((int) spaces));
                }
                column += spaces;
                index++;
                start = index;
            } else {
                column = codePoint == '\r' || codePoint == '\n' ? 0 : column + 1;
                index += Character.charCount(codePoint);
            }
        }
        expanded.append(this, start, sourceLength);
        return expanded.toString();
    }

    /**
     * Adjusts the indentation of each line of this string based on the value of
     * {@code n}, and normalizes line termination characters.
     * <p>
     * This string is conceptually separated into lines using
     * {@link String#lines()}. Each line is then adjusted as described below
     * and then suffixed with a line feed {@code "\n"} (U+000A). The resulting
     * lines are then concatenated and returned.
     * <p>
     * If {@code n > 0} then {@code n} spaces (U+0020) are inserted at the
     * beginning of each line.
     * <p>
     * If {@code n < 0} then up to {@code n}
     * {@linkplain Character#isWhitespace(int) white space characters} are removed
     * from the beginning of each line. If a given line does not contain
     * sufficient white space then all leading
     * {@linkplain Character#isWhitespace(int) white space characters} are removed.
     * Each white space character is treated as a single character. In
     * particular, the tab character {@code "\t"} (U+0009) is considered a
     * single character; it is not expanded.
     * <p>
     * If {@code n == 0} then the line remains unchanged. However, line
     * terminators are still normalized.
     *
     * @param n  number of leading
     *           {@linkplain Character#isWhitespace(int) white space characters}
     *           to add or remove
     *
     * @return string with indentation adjusted and line endings normalized
     *
     * @see String#lines()
     * @see String#isBlank()
     * @see Character#isWhitespace(int)
     *
     * @since 12
     */
    public String indent(int n) {
        if (isEmpty()) {
            return "";
        }
        M3String storage = m3();
        if (storage != null) {
            return new String(storage.indent(n));
        }
        Stream<String> stream = lines();
        if (n > 0) {
            final String spaces = " ".repeat(n);
            stream = stream.map(s -> spaces + s);
        } else if (n == Integer.MIN_VALUE) {
            stream = stream.map(s -> s.stripLeading());
        } else if (n < 0) {
            stream = stream.map(s -> s.substring(Math.min(-n, s.indexOfNonWhitespace())));
        }
        return stream.collect(Collectors.joining("\n", "", "\n"));
    }

    private int indexOfNonWhitespace() {
        M3String storage = m3();
        if (storage != null) return M3StringPrecomputeBridge.edgeStart(storage, true);
        byte[] currentValue = value();
        return isLatin1() ? StringLatin1.indexOfNonWhitespace(currentValue)
                          : StringUTF16.indexOfNonWhitespace(currentValue);
    }

    private int lastIndexOfNonWhitespace() {
        M3String storage = m3();
        if (storage != null) return M3StringPrecomputeBridge.edgeEnd(storage, true);
        byte[] currentValue = value();
        return isLatin1() ? StringLatin1.lastIndexOfNonWhitespace(currentValue)
                          : StringUTF16.lastIndexOfNonWhitespace(currentValue);
    }

    /**
     * Returns a string whose value is this string, with incidental
     * {@linkplain Character#isWhitespace(int) white space} removed from
     * the beginning and end of every line.
     * <p>
     * Incidental {@linkplain Character#isWhitespace(int) white space}
     * is often present in a text block to align the content with the opening
     * delimiter. For example, in the following code, dots represent incidental
     * {@linkplain Character#isWhitespace(int) white space}:
     * <blockquote><pre>
     * String html = """
     * ..............&lt;html&gt;
     * ..............    &lt;body&gt;
     * ..............        &lt;p&gt;Hello, world&lt;/p&gt;
     * ..............    &lt;/body&gt;
     * ..............&lt;/html&gt;
     * ..............""";
     * </pre></blockquote>
     * This method treats the incidental
     * {@linkplain Character#isWhitespace(int) white space} as indentation to be
     * stripped, producing a string that preserves the relative indentation of
     * the content. Using | to visualize the start of each line of the string:
     * <blockquote><pre>
     * |&lt;html&gt;
     * |    &lt;body&gt;
     * |        &lt;p&gt;Hello, world&lt;/p&gt;
     * |    &lt;/body&gt;
     * |&lt;/html&gt;
     * </pre></blockquote>
     * First, the individual lines of this string are extracted. A <i>line</i>
     * is a sequence of zero or more characters followed by either a line
     * terminator or the end of the string.
     * If the string has at least one line terminator, the last line consists
     * of the characters between the last terminator and the end of the string.
     * Otherwise, if the string has no terminators, the last line is the start
     * of the string to the end of the string, in other words, the entire
     * string.
     * A line does not include the line terminator.
     * <p>
     * Then, the <i>minimum indentation</i> (min) is determined as follows:
     * <ul>
     *   <li><p>For each non-blank line (as defined by {@link String#isBlank()}),
     *   the leading {@linkplain Character#isWhitespace(int) white space}
     *   characters are counted.</p>
     *   </li>
     *   <li><p>The leading {@linkplain Character#isWhitespace(int) white space}
     *   characters on the last line are also counted even if
     *   {@linkplain String#isBlank() blank}.</p>
     *   </li>
     * </ul>
     * <p>The <i>min</i> value is the smallest of these counts.
     * <p>
     * For each {@linkplain String#isBlank() non-blank} line, <i>min</i> leading
     * {@linkplain Character#isWhitespace(int) white space} characters are
     * removed, and any trailing {@linkplain Character#isWhitespace(int) white
     * space} characters are removed. {@linkplain String#isBlank() Blank} lines
     * are replaced with the empty string.
     *
     * <p>
     * Finally, the lines are joined into a new string, using the LF character
     * {@code "\n"} (U+000A) to separate lines.
     *
     * @apiNote
     * This method's primary purpose is to shift a block of lines as far as
     * possible to the left, while preserving relative indentation. Lines
     * that were indented the least will thus have no leading
     * {@linkplain Character#isWhitespace(int) white space}.
     * The result will have the same number of line terminators as this string.
     * If this string ends with a line terminator then the result will end
     * with a line terminator.
     *
     * @implSpec
     * This method treats all {@linkplain Character#isWhitespace(int) white space}
     * characters as having equal width. As long as the indentation on every
     * line is consistently composed of the same character sequences, then the
     * result will be as described above.
     *
     * @return string with incidental indentation removed and line
     *         terminators normalized
     *
     * @see String#lines()
     * @see String#isBlank()
     * @see String#indent(int)
     * @see Character#isWhitespace(int)
     *
     * @since 15
     *
     */
    public String stripIndent() {
        int length = length();
        if (length == 0) {
            return "";
        }
        M3String storage = m3();
        if (storage != null) {
            return new String(storage.stripIndent());
        }
        char lastChar = charAt(length - 1);
        boolean optOut = lastChar == '\n' || lastChar == '\r';
        List<String> lines = lines().toList();
        final int outdent = optOut ? 0 : outdent(lines);
        return lines.stream()
            .map(line -> {
                int firstNonWhitespace = line.indexOfNonWhitespace();
                int lastNonWhitespace = line.lastIndexOfNonWhitespace();
                int incidentalWhitespace = Math.min(outdent, firstNonWhitespace);
                return firstNonWhitespace > lastNonWhitespace
                    ? "" : line.substring(incidentalWhitespace, lastNonWhitespace);
            })
            .collect(Collectors.joining("\n", "", optOut ? "\n" : ""));
    }

    private static int outdent(List<String> lines) {
        // Note: outdent is guaranteed to be zero or positive number.
        // If there isn't a non-blank line then the last must be blank
        int outdent = Integer.MAX_VALUE;
        for (String line : lines) {
            int leadingWhitespace = line.indexOfNonWhitespace();
            if (leadingWhitespace != line.length()) {
                outdent = Integer.min(outdent, leadingWhitespace);
            }
        }
        String lastLine = lines.get(lines.size() - 1);
        if (lastLine.isBlank()) {
            outdent = Integer.min(outdent, lastLine.length());
        }
        return outdent;
    }

    /**
     * Returns a string whose value is this string, with escape sequences
     * translated as if in a string literal.
     * <p>
     * Escape sequences are translated as follows;
     * <table class="striped">
     *   <caption style="display:none">Translation</caption>
     *   <thead>
     *   <tr>
     *     <th scope="col">Escape</th>
     *     <th scope="col">Name</th>
     *     <th scope="col">Translation</th>
     *   </tr>
     *   </thead>
     *   <tbody>
     *   <tr>
     *     <th scope="row">{@code \u005Cb}</th>
     *     <td>backspace</td>
     *     <td>{@code U+0008}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Ct}</th>
     *     <td>horizontal tab</td>
     *     <td>{@code U+0009}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Cn}</th>
     *     <td>line feed</td>
     *     <td>{@code U+000A}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Cf}</th>
     *     <td>form feed</td>
     *     <td>{@code U+000C}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Cr}</th>
     *     <td>carriage return</td>
     *     <td>{@code U+000D}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Cs}</th>
     *     <td>space</td>
     *     <td>{@code U+0020}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C"}</th>
     *     <td>double quote</td>
     *     <td>{@code U+0022}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C'}</th>
     *     <td>single quote</td>
     *     <td>{@code U+0027}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C\u005C}</th>
     *     <td>backslash</td>
     *     <td>{@code U+005C}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C0 - \u005C377}</th>
     *     <td>octal escape</td>
     *     <td>code point equivalents</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C<line-terminator>}</th>
     *     <td>continuation</td>
     *     <td>discard</td>
     *   </tr>
     *   </tbody>
     * </table>
     *
     * @implNote
     * This method does <em>not</em> translate Unicode escapes such as "{@code \u005cu2022}".
     * Unicode escapes are translated by the Java compiler when reading input characters and
     * are not part of the string literal specification.
     *
     * @throws IllegalArgumentException when an escape sequence is malformed.
     *
     * @return String with escape sequences translated.
     *
     * @jls 3.10.7 Escape Sequences
     *
     * @since 15
     */
    public String translateEscapes() {
        if (isEmpty()) {
            return "";
        }
        M3String storage = m3();
        if (storage != null) {
            return new String(storage.translateEscapes());
        }
        char[] chars = toCharArray();
        int length = chars.length;
        int from = 0;
        int to = 0;
        while (from < length) {
            char ch = chars[from++];
            if (ch == '\\') {
                ch = from < length ? chars[from++] : '\0';
                switch (ch) {
                case 'b':
                    ch = '\b';
                    break;
                case 'f':
                    ch = '\f';
                    break;
                case 'n':
                    ch = '\n';
                    break;
                case 'r':
                    ch = '\r';
                    break;
                case 's':
                    ch = ' ';
                    break;
                case 't':
                    ch = '\t';
                    break;
                case '\'':
                case '\"':
                case '\\':
                    // as is
                    break;
                case '0': case '1': case '2': case '3':
                case '4': case '5': case '6': case '7':
                    int limit = Integer.min(from + (ch <= '3' ? 2 : 1), length);
                    int code = ch - '0';
                    while (from < limit) {
                        ch = chars[from];
                        if (ch < '0' || '7' < ch) {
                            break;
                        }
                        from++;
                        code = (code << 3) | (ch - '0');
                    }
                    ch = (char)code;
                    break;
                case '\n':
                    continue;
                case '\r':
                    if (from < length && chars[from] == '\n') {
                        from++;
                    }
                    continue;
                default: {
                    String msg = String.format(
                        "Invalid escape sequence: \\%c \\\\u%04X",
                        ch, (int)ch);
                    throw new IllegalArgumentException(msg);
                }
                }
            }

            chars[to++] = ch;
        }

        return new String(chars, 0, to);
    }

    /**
     * This method allows the application of a function to {@code this}
     * string. The function should expect a single String argument
     * and produce an {@code R} result.
     * <p>
     * Any exception thrown by {@code f.apply()} will be propagated to the
     * caller.
     *
     * @param f    a function to apply
     *
     * @param <R>  the type of the result
     *
     * @return     the result of applying the function to this string
     *
     * @see java.util.function.Function
     *
     * @since 12
     */
    public <R> R transform(Function<? super String, ? extends R> f) {
        return f.apply(this);
    }

    /**
     * This object (which is already a string!) is itself returned.
     *
     * @return  the string itself.
     */
    public String toString() {
        return this;
    }

    /**
     * Returns a stream of {@code int} zero-extending the {@code char} values
     * from this sequence.  Any char which maps to a {@linkplain
     * Character##unicode surrogate code point} is passed through
     * uninterpreted.
     *
     * @return an IntStream of char values from this sequence
     * @since 9
     */
    @Override
    public IntStream chars() {
        M3String storage = m3();
        if (storage != null) {
            return storage.charsStream();
        }
        return StreamSupport.intStream(
            isLatin1() ? new StringLatin1.CharsSpliterator(value(), Spliterator.IMMUTABLE)
                       : new StringUTF16.CharsSpliterator(value(), Spliterator.IMMUTABLE),
            false);
    }


    /**
     * Returns a stream of code point values from this sequence.  Any surrogate
     * pairs encountered in the sequence are combined as if by {@linkplain
     * Character#toCodePoint Character.toCodePoint} and the result is passed
     * to the stream. Any other code units, including ordinary BMP characters,
     * unpaired surrogates, and undefined code units, are zero-extended to
     * {@code int} values which are then passed to the stream.
     *
     * @return an IntStream of Unicode code points from this sequence
     * @since 9
     */
    @Override
    public IntStream codePoints() {
        M3String storage = m3();
        if (storage != null) {
            return storage.codePointsStream();
        }
        return StreamSupport.intStream(
            isLatin1() ? new StringLatin1.CharsSpliterator(value(), Spliterator.IMMUTABLE)
                       : new StringUTF16.CodePointsSpliterator(value(), Spliterator.IMMUTABLE),
            false);
    }

    /**
     * Converts this string to a new character array.
     *
     * @return  a newly allocated character array whose length is the length
     *          of this string and whose contents are initialized to contain
     *          the character sequence represented by this string.
     */
    public char[] toCharArray() {
        return constChars().clone();
    }

    /**
     * Returns a formatted string using the specified format string and
     * arguments.
     *
     * <p> The locale always used is the one returned by {@link
     * java.util.Locale#getDefault(java.util.Locale.Category)
     * Locale.getDefault(Locale.Category)} with
     * {@link java.util.Locale.Category#FORMAT FORMAT} category specified.
     *
     * @param  format
     *         A <a href="../util/Formatter.html#syntax">format string</a>
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The number of arguments is
     *         variable and may be zero.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *         The behaviour on a
     *         {@code null} argument depends on the <a
     *         href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @throws  java.util.IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the <a
     *          href="../util/Formatter.html#detail">Details</a> section of the
     *          formatter class specification.
     *
     * @return  A formatted string
     *
     * @see  java.util.Formatter
     * @since  1.5
     */
    public static String format(String format, Object... args) {
        return new Formatter().format(format, args).toString();
    }

    /**
     * Returns a formatted string using the specified locale, format string,
     * and arguments.
     *
     * @param  l
     *         The {@linkplain java.util.Locale locale} to apply during
     *         formatting.  If {@code l} is {@code null} then no localization
     *         is applied.
     *
     * @param  format
     *         A <a href="../util/Formatter.html#syntax">format string</a>
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The number of arguments is
     *         variable and may be zero.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *         The behaviour on a
     *         {@code null} argument depends on the
     *         <a href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @throws  java.util.IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the <a
     *          href="../util/Formatter.html#detail">Details</a> section of the
     *          formatter class specification
     *
     * @return  A formatted string
     *
     * @see  java.util.Formatter
     * @since  1.5
     */
    public static String format(Locale l, String format, Object... args) {
        return new Formatter(l).format(format, args).toString();
    }

    /**
     * Formats using this string as the format string, and the supplied
     * arguments.
     *
     * @implSpec This method is equivalent to {@code String.format(this, args)}.
     *
     * @param  args
     *         Arguments referenced by the format specifiers in this string.
     *
     * @return  A formatted string
     *
     * @see  java.lang.String#format(String,Object...)
     * @see  java.util.Formatter
     *
     * @since 15
     *
     */
    public String formatted(Object... args) {
        return new Formatter().format(this, args).toString();
    }

    /**
     * Returns the string representation of the {@code Object} argument.
     *
     * @param   obj   an {@code Object}.
     * @return  if the argument is {@code null}, then a string equal to
     *          {@code "null"}; otherwise, the value of
     *          {@code obj.toString()} is returned.
     * @see     java.lang.Object#toString()
     */
    public static String valueOf(Object obj) {
        return (obj == null) ? "null" : obj.toString();
    }

    /**
     * Returns the string representation of the {@code char} array
     * argument. The contents of the character array are copied; subsequent
     * modification of the character array does not affect the returned
     * string.
     *
     * @param   data     the character array.
     * @return  a {@code String} that contains the characters of the
     *          character array.
     */
    public static String valueOf(char[] data) {
        return new String(data);
    }

    /**
     * Returns the string representation of a specific subarray of the
     * {@code char} array argument.
     * <p>
     * The {@code offset} argument is the index of the first
     * character of the subarray. The {@code count} argument
     * specifies the length of the subarray. The contents of the subarray
     * are copied; subsequent modification of the character array does not
     * affect the returned string.
     *
     * @param   data     the character array.
     * @param   offset   initial offset of the subarray.
     * @param   count    length of the subarray.
     * @return  a {@code String} that contains the characters of the
     *          specified subarray of the character array.
     * @throws    IndexOutOfBoundsException if {@code offset} is
     *          negative, or {@code count} is negative, or
     *          {@code offset+count} is larger than
     *          {@code data.length}.
     */
    public static String valueOf(char[] data, int offset, int count) {
        return new String(data, offset, count);
    }

    /**
     * Equivalent to {@link #valueOf(char[], int, int)}.
     *
     * @param   data     the character array.
     * @param   offset   initial offset of the subarray.
     * @param   count    length of the subarray.
     * @return  a {@code String} that contains the characters of the
     *          specified subarray of the character array.
     * @throws    IndexOutOfBoundsException if {@code offset} is
     *          negative, or {@code count} is negative, or
     *          {@code offset+count} is larger than
     *          {@code data.length}.
     */
    public static String copyValueOf(char[] data, int offset, int count) {
        return new String(data, offset, count);
    }

    /**
     * Equivalent to {@link #valueOf(char[])}.
     *
     * @param   data   the character array.
     * @return  a {@code String} that contains the characters of the
     *          character array.
     */
    public static String copyValueOf(char[] data) {
        return new String(data);
    }

    /**
     * Returns the string representation of the {@code boolean} argument.
     *
     * @param   b   a {@code boolean}.
     * @return  if the argument is {@code true}, a string equal to
     *          {@code "true"} is returned; otherwise, a string equal to
     *          {@code "false"} is returned.
     */
    public static String valueOf(boolean b) {
        return b ? "true" : "false";
    }

    /**
     * Returns the string representation of the {@code char}
     * argument.
     *
     * @param   c   a {@code char}.
     * @return  a string of length {@code 1} containing
     *          as its single character the argument {@code c}.
     */
    public static String valueOf(char c) {
        if (M3String.admissionEnabled()) {
            return new String(M3StringPool.internUnit(c));
        }
        if (COMPACT_STRINGS && StringLatin1.canEncode(c)) {
            return new String(StringLatin1.toBytes(c), LATIN1);
        }
        return new String(StringUTF16.toBytes(c), UTF16);
    }

    /**
     * Returns the string representation of the {@code int} argument.
     * <p>
     * The representation is exactly the one returned by the
     * {@code Integer.toString} method of one argument.
     *
     * @param   i   an {@code int}.
     * @return  a string representation of the {@code int} argument.
     * @see     java.lang.Integer#toString(int, int)
     */
    public static String valueOf(int i) {
        return Integer.toString(i);
    }

    /**
     * Returns the string representation of the {@code long} argument.
     * <p>
     * The representation is exactly the one returned by the
     * {@code Long.toString} method of one argument.
     *
     * @param   l   a {@code long}.
     * @return  a string representation of the {@code long} argument.
     * @see     java.lang.Long#toString(long)
     */
    public static String valueOf(long l) {
        return Long.toString(l);
    }

    /**
     * Returns the string representation of the {@code float} argument.
     * <p>
     * The representation is exactly the one returned by the
     * {@code Float.toString} method of one argument.
     *
     * @param   f   a {@code float}.
     * @return  a string representation of the {@code float} argument.
     * @see     java.lang.Float#toString(float)
     */
    public static String valueOf(float f) {
        return Float.toString(f);
    }

    /**
     * Returns the string representation of the {@code double} argument.
     * <p>
     * The representation is exactly the one returned by the
     * {@code Double.toString} method of one argument.
     *
     * @param   d   a {@code double}.
     * @return  a  string representation of the {@code double} argument.
     * @see     java.lang.Double#toString(double)
     */
    public static String valueOf(double d) {
        return Double.toString(d);
    }

    /**
     * Returns a canonical representation for the string object.
     * <p>
     * A pool of strings, initially empty, is maintained privately by the
     * class {@code String}.
     * <p>
     * When the intern method is invoked, if the pool already contains a
     * string equal to this {@code String} object as determined by
     * the {@link #equals(Object)} method, then the string from the pool is
     * returned. Otherwise, this {@code String} object is added to the
     * pool and a reference to this {@code String} object is returned.
     * <p>
     * It follows that for any two strings {@code s} and {@code t},
     * {@code s.intern() == t.intern()} is {@code true}
     * if and only if {@code s.equals(t)} is {@code true}.
     * <p>
     * All literal strings and string-valued constant expressions are
     * interned. String literals are defined in section {@jls 3.10.5} of the
     * <cite>The Java Language Specification</cite>.
     *
     * @return  a string that has the same contents as this string, but is
     *          guaranteed to be from a pool of unique strings.
     */
    public native String intern();

    /**
     * Returns a string whose value is the concatenation of this
     * string repeated {@code count} times.
     * <p>
     * If this string is empty or count is zero then the empty
     * string is returned.
     *
     * @param   count number of times to repeat
     *
     * @return  A string composed of this string repeated
     *          {@code count} times or the empty string if this
     *          string is empty or count is zero
     *
     * @throws  IllegalArgumentException if the {@code count} is
     *          negative.
     *
     * @since 11
     */
    public String repeat(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count is negative: " + count);
        }
        if (count == 1) {
            return this;
        }
        final int charLength = length();
        if (charLength == 0 || count == 0) {
            return "";
        }
        if (Integer.MAX_VALUE / count < charLength) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        M3String storage = m3();
        if (storage != null) {
            return new String(storage.repeat(count));
        }
        byte[] currentValue = value();
        final int len = currentValue.length;
        if (Integer.MAX_VALUE / count < len) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        if (len == 1) {
            final byte[] single = new byte[count];
            Arrays.fill(single, currentValue[0]);
            return new String(single, coder);
        }
        final int limit = len * count;
        final byte[] multiple = new byte[limit];
        System.arraycopy(currentValue, 0, multiple, 0, len);
        repeatCopyRest(multiple, 0, limit, len);
        return new String(multiple, coder);
    }

    /**
     * Used to perform copying after the initial insertion. Copying is optimized
     * by using power of two duplication. First pass duplicates original copy,
     * second pass then duplicates the original and the copy yielding four copies,
     * third pass duplicates four copies yielding eight copies, and so on.
     * Finally, the remainder is filled in with prior copies.
     *
     * @implNote The technique used here is significantly faster than hand-rolled
     * loops or special casing small numbers due to the intensive optimization
     * done by intrinsic {@code System.arraycopy}.
     *
     * @param buffer    destination buffer
     * @param offset    offset in the destination buffer
     * @param limit     total replicated including what is already in the buffer
     * @param copied    number of bytes that have already in the buffer
     */
    static void repeatCopyRest(byte[] buffer, int offset, int limit, int copied) {
        // Initial copy is in the buffer.
        for (; copied < limit - copied; copied <<= 1) {
            // Power of two duplicate.
            System.arraycopy(buffer, offset, buffer, offset + copied, copied);
        }
        // Duplicate remainder.
        System.arraycopy(buffer, offset, buffer, offset + copied, limit - copied);
    }

    ////////////////////////////////////////////////////////////////

    /**
     * Copy character bytes from this string into dst starting at dstBegin.
     * This method doesn't perform any range checking.
     *
     * Invoker guarantees: dst is in UTF16 (inflate itself for asb), if two
     * coders are different, and dst is big enough (range check)
     *
     * @param dstBegin  the char index, not offset of byte[]
     * @param coder     the coder of dst[]
     */
    void getBytes(byte[] dst, int dstBegin, byte coder) {
        M3String storage = m3();
        if (storage != null) {
            storage.getBytes(dst, 0, dstBegin, coder, length());
        } else if (coder() == coder) {
            System.arraycopy(value, 0, dst, dstBegin << coder, value.length);
        } else {    // this.coder == LATIN && coder == UTF16
            StringLatin1.inflate(value, 0, dst, dstBegin, value.length);
        }
    }

    /**
     * Copy character bytes from this string into dst starting at dstBegin.
     * This method doesn't perform any range checking.
     *
     * Invoker guarantees: dst is in UTF16 (inflate itself for asb), if two
     * coders are different, and dst is big enough (range check)
     *
     * @param srcPos    the char index, not offset of byte[]
     * @param dstBegin  the char index to start from
     * @param coder     the coder of dst[]
     * @param length    the amount of copied chars
     */
    void getBytes(byte[] dst, int srcPos, int dstBegin, byte coder, int length) {
        M3String storage = m3();
        if (storage != null) {
            storage.getBytes(dst, srcPos, dstBegin, coder, length);
        } else if (coder() == coder) {
            System.arraycopy(value, srcPos << coder, dst, dstBegin << coder, length << coder);
        } else {    // this.coder == LATIN && coder == UTF16
            StringLatin1.inflate(value, srcPos, dst, dstBegin, length);
        }
    }

    /*
     * Private constructor. Trailing Void argument is there for
     * disambiguating it against other (public) constructors.
     *
     * Stores the char[] value into a byte[] that each byte represents
     * the8 low-order bits of the corresponding character, if the char[]
     * contains only latin1 character. Or a byte[] that stores all
     * characters in their byte sequences defined by the {@code StringUTF16}.
     *
     * <p> The contents of the string are unspecified if the character array
     * is modified during string construction.
     */
    private String(char[] value, int off, int len, Void sig) {
        if (len == 0) {
            this.value = "".value;
            this.coder = "".coder;
            this.m3 = null;
            return;
        }

        M3String direct = maybeAdmit(value, off, len);
        if (direct != null) {
            this.value = direct.materialize();
            this.coder = shadowCoder(this.value, direct.length());
            this.m3 = direct;
            return;
        }

        //FLLDCIM: MERGE-21U - 8311906: compress returns the result array (never null); its length gives the coder
        if (COMPACT_STRINGS) {
            byte[] val = StringUTF16.compress(value, off, len);
            this.value = val;
            this.coder = StringUTF16.coderFromArrayLen(val, len);
            this.m3 = null;
            return;
        }
        this.value = StringUTF16.toBytes(value, off, len);
        this.coder = UTF16;
        this.m3 = null;
    }

    /*
     * Package private constructor. Trailing Void argument is there for
     * disambiguating it against other (public) constructors.
     *
     * <p> The contents of the string are unspecified if the {@code StringBuilder}
     * is modified during string construction.
     */
    String(AbstractStringBuilder asb, Void sig) {
        byte[] val = asb.getValue();
        int length = asb.length();

        M3String direct =
                maybeAdmitCompact(
                        val,
                        0,
                        length,
                        asb.isLatin1() ? LATIN1 : UTF16);
        if (direct != null) {
            this.value = direct.materialize();
            this.coder = shadowCoder(this.value, direct.length());
            this.m3 = direct;
            return;
        }

        byte[] candidate;
        byte candidateCoder;
        if (asb.isLatin1()) {
            candidate = Arrays.copyOfRange(val, 0, length);
            candidateCoder = LATIN1;
        } else {
            // only try to compress val if some characters were deleted.
            //FLLDCIM: MERGE-21U - 8311906: compress returns the result array (never null); its length gives the coder
            if (COMPACT_STRINGS && asb.maybeLatin1) {
                candidate = StringUTF16.compress(val, 0, length);
                candidateCoder = StringUTF16.coderFromArrayLen(candidate, length);
            } else {
                candidate = Arrays.copyOfRange(val, 0, length << 1);
                candidateCoder = UTF16;
            }
        }
        this.value = candidate;
        this.coder = candidateCoder;
        this.m3 = null;
    }

   /*
    * Package private constructor which shares value array for speed.
    */
    String(byte[] value, byte coder) {
        M3String storage = maybeAdmit(value, coder);
        this.value = admittedBytes(value, coder, storage);
        this.coder = this.value == value ? coder : shadowCoder(this.value, storage.length());
        this.m3 = storage;
    }

    /**
     * The coder of M3 shadow bytes of {@code length} units, which {@link M3String#materialize()}
     * lays out in the canonical coder (decision 50): Latin-1 exactly when one byte per unit.
     */
    private static byte shadowCoder(byte[] shadow, int length) {
        return COMPACT_STRINGS ? StringUTF16.coderFromArrayLen(shadow, length) : UTF16;
    }

    /**
     * The bytes an admitted String keeps: the caller's bytes, unless they are UTF-16 and the
     * pool found every unit Latin-1 (the pool canonicalises), in which case they are laid out
     * again in Latin-1, so that value and coder agree with the canonical coder (decision 50).
     */
    private static byte[] admittedBytes(byte[] bytes, byte coder, M3String storage) {
        return COMPACT_STRINGS && storage != null && coder == UTF16
                        && storage.coder() == LATIN1 && storage.length() != 0
                ? StringUTF16.compress(bytes, 0, storage.length())
                : bytes;
    }

    private static M3String maybeAdmitCodePoints(
            int[] value, int offset, int count) {
        return M3_JOINED_STRINGS
                && jdk.internal.misc.VM.isBooted()
                && M3String.admissionEnabled()
                && count != 0
                ? M3String.admitCodePoints(Arrays.copyOfRange(value, offset, offset + count), 0, count)
                : null;
    }

    private static M3String maybeAdmitCompact(
            byte[] value, int sourceOffset, int length, byte sourceCoder) {
        return M3_JOINED_STRINGS
                && jdk.internal.misc.VM.isBooted()
                && M3String.admissionEnabled()
                && length != 0
                ? M3String.admitCompactBytes(value, sourceOffset, length, sourceCoder)
                : null;
    }

    // The caller's array may be modified during construction (JDK-8311906): the M3 pool decides the coder
    // and copies the units in separate passes, so it admits a private snapshot read exactly once.
    private static M3String maybeAdmit(char[] value, int offset, int length) {
        return M3_JOINED_STRINGS
                && jdk.internal.misc.VM.isBooted()
                && M3String.admissionEnabled()
                && length != 0
                ? M3String.admit(Arrays.copyOfRange(value, offset, offset + length), 0, length)
                : null;
    }

    private static M3String maybeAdmit(byte[] value, byte coder) {
        return M3_JOINED_STRINGS && jdk.internal.misc.VM.isBooted() && M3String.admissionEnabled() && value.length != 0
                ? M3String.admit(value, coder)
                : null;
    }

    /** Trusted constructor for one canonical M3String storage body. */
    String(M3String storage) {
        M3String checked = Objects.requireNonNull(storage, "storage");
        this.value = checked.materialize();
        this.coder = shadowCoder(this.value, checked.length());
        this.m3 = checked;
    }

    M3String m3() {
        return m3;
    }

    /**
     * The const chars of this string (layout invariant, decision 51): fills {@link #chValue} once
     * with the chars of {@link #value} on the first call and returns it. The fill is published
     * by one compare-and-set from {@code null}, so no thread sees the reference before the
     * filled contents and racing first callers install one array: a caller that loses the race
     * returns the winner's. Callers copy out of it; it never leaves this class.
     */
    private char[] constChars() {
        char[] chars = chValue;
        if (chars == null) {
            //FLLDCIM: LAYOUT - decision 51: chValue is filled once; a lost race takes the winner
            chars = layoutChars(value, coder);
            if (!ConstChars.UNSAFE.compareAndSetReference(this, ConstChars.OFFSET, null, chars)) {
                chars = (char[]) ConstChars.UNSAFE.getReferenceAcquire(this, ConstChars.OFFSET);
            }
        }
        return chars;
    }

    /**
     * The offset of {@link #chValue} for the publishing compare-and-set of {@link #constChars()},
     * computed once. A holder initialized on the first fill: the VM initializes String before
     * jdk.internal.misc.UnsafeConstants, so the class initializer of String must not touch
     * Unsafe.
     */
    private static final class ConstChars {
        static final jdk.internal.misc.Unsafe UNSAFE = jdk.internal.misc.Unsafe.getUnsafe();
        static final long OFFSET = UNSAFE.objectFieldOffset(String.class, "chValue");

        private ConstChars() {}
    }

    /**
     * The chars of the layout invariant: the bytes of {@link #value} inflated to UTF-16 units.
     * An M3-backed String inflates them too: its value and coder agree (decision 50).
     */
    private static char[] layoutChars(byte[] bytes, byte coder) {
        //FLLDCIM: LAYOUT - decision 51: called once per String by chars(), never at construction
        //FLLDCIM: LAYOUT - decision 50: M3-backed Strings inflate value; no owner walk
        return COMPACT_STRINGS && coder == LATIN1 ? StringLatin1.toChars(bytes)
                                                 : StringUTF16.toChars(bytes);
    }

    private M3String ensureM3() {
        M3String storage = m3;
        if (storage == null
                && M3_JOINED_STRINGS
                && jdk.internal.misc.VM.isBooted()
                && M3String.admissionEnabled()
                && value.length != 0) {
            storage = M3String.admit(value, coder());
            m3 = storage;
        }
        return storage;
    }

    /**
     * VM-private JNI ingress hook. The VM calls this only after module initialization; the normal
     * M3 admission gates still decide whether the supplied String should attach canonical
     * storage. Bootstrap-created Strings therefore retain the stock representation.
     */
    static byte[] m3AdmitNative(String value) {
        String checked = Objects.requireNonNull(value, "value");
        checked.ensureM3();
        // Layout invariant: the bytes the VM made stay this String's bytes after M3 admission.
        return checked.value;
    }

    static boolean m3StorageRequested() {
        return M3_JOINED_STRINGS;
    }

    static boolean m3JoinedStringsEnabled() {
        return M3_JOINED_STRINGS && jdk.internal.misc.VM.isBooted() && M3String.admissionEnabled();
    }

    static String m3Concat(String first, String second) {
        if (!m3JoinedStringsEnabled()) {
            return null;
        }
        M3String storage = M3String.join(first, second);
        return storage == null ? null : new String(storage);
    }

    /**
     * JNI-callable internal ingress for selected UTF-16 ranges. Native callers can resolve this
     * static method with signature {@code ([[C[I[I)Ljava/lang/String;} and pass Java-owned
     * {@code char[][]}, start, and length arrays without acquiring or retaining element pointers.
     * The method interns each piece in the canonical M3 pool and returns an immutable tuple-backed
     * String, or {@code null} when M3 admission is disabled or the bounded pool declines; callers
     * then retain their existing flat fallback.
     */
    static String m3JoinCharRanges(char[][] sources, int[] starts, int[] lengths) {
        if (!m3JoinedStringsEnabled()) return null;
        M3String storage = M3String.joinCharRanges(sources, starts, lengths);
        return storage == null ? null : new String(storage);
    }

    /**
     * The coder of {@link #value}, for every String the canonical coder of its text (decision
     * 50): an M3-backed String reports its bytes' coder, never its owner's.
     */
    byte coder() {
        return COMPACT_STRINGS ? coder : UTF16;
    }

    byte[] value() {
        return value;
    }

    boolean isLatin1() {
        return COMPACT_STRINGS && coder() == LATIN1;
    }

    @Native static final byte LATIN1 = 0;
    @Native static final byte UTF16  = 1;

    /*
     * StringIndexOutOfBoundsException  if {@code index} is
     * negative or greater than or equal to {@code length}.
     */
    static void checkIndex(int index, int length) {
        Preconditions.checkIndex(index, length, Preconditions.SIOOBE_FORMATTER);
    }

    /*
     * StringIndexOutOfBoundsException  if {@code offset}
     * is negative or greater than {@code length}.
     */
    static void checkOffset(int offset, int length) {
        Preconditions.checkFromToIndex(offset, length, length, Preconditions.SIOOBE_FORMATTER);
    }

    /*
     * Check {@code offset}, {@code count} against {@code 0} and {@code length}
     * bounds.
     *
     * @return  {@code offset} if the sub-range within bounds of the range
     * @throws  StringIndexOutOfBoundsException
     *          If {@code offset} is negative, {@code count} is negative,
     *          or {@code offset} is greater than {@code length - count}
     */
    static int checkBoundsOffCount(int offset, int count, int length) {
        return Preconditions.checkFromIndexSize(offset, count, length, Preconditions.SIOOBE_FORMATTER);
    }

    /*
     * Check {@code begin}, {@code end} against {@code 0} and {@code length}
     * bounds.
     *
     * @throws  StringIndexOutOfBoundsException
     *          If {@code begin} is negative, {@code begin} is greater than
     *          {@code end}, or {@code end} is greater than {@code length}.
     */
    static void checkBoundsBeginEnd(int begin, int end, int length) {
        Preconditions.checkFromToIndex(begin, end, length, Preconditions.SIOOBE_FORMATTER);
    }

    /**
     * Returns the string representation of the {@code codePoint}
     * argument.
     *
     * @param   codePoint a {@code codePoint}.
     * @return  a string of length {@code 1} or {@code 2} containing
     *          as its single character the argument {@code codePoint}.
     * @throws IllegalArgumentException if the specified
     *          {@code codePoint} is not a {@linkplain Character#isValidCodePoint
     *          valid Unicode code point}.
     */
    static String valueOfCodePoint(int codePoint) {
        if (COMPACT_STRINGS && StringLatin1.canEncode(codePoint)) {
            return new String(StringLatin1.toBytes((char)codePoint), LATIN1);
        } else if (Character.isBmpCodePoint(codePoint)) {
            return new String(StringUTF16.toBytes((char)codePoint), UTF16);
        } else if (Character.isSupplementaryCodePoint(codePoint)) {
            return new String(StringUTF16.toBytesSupplementary(codePoint), UTF16);
        }

        throw new IllegalArgumentException(
            format("Not a valid Unicode code point: 0x%X", codePoint));
    }

    /**
     * Returns an {@link Optional} containing the nominal descriptor for this
     * instance, which is the instance itself.
     *
     * @return an {@link Optional} describing the {@linkplain String} instance
     * @since 12
     */
    @Override
    public Optional<String> describeConstable() {
        return Optional.of(this);
    }

    /**
     * Resolves this instance as a {@link ConstantDesc}, the result of which is
     * the instance itself.
     *
     * @param lookup ignored
     * @return the {@linkplain String} instance
     * @since 12
     */
    @Override
    public String resolveConstantDesc(MethodHandles.Lookup lookup) {
        return this;
    }

}
