/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.nio;

import java.util.Objects;
import java.util.function.IntUnaryOperator;
import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;

// ## If the sequence is a string, use reflection to share its array

final class StringCharBuffer                                  // package-private
    extends CharBuffer
{
    CharSequence str;
    private final IntUnaryOperator codeUnits;

    private static IntUnaryOperator cursorFor(CharSequence source) {
        if (!(source instanceof String text)) return null;
        JavaLangAccess access = SharedSecrets.getJavaLangAccess();
        return access == null ? null : access.stringCodeUnitCursor(text);
    }

    private char unitAt(int index) {
        return codeUnits == null ? str.charAt(index) : (char) codeUnits.applyAsInt(index);
    }

    StringCharBuffer(CharSequence s, int start, int end) { // package-private
        super(-1, start, end, s.length(), null);
        int n = s.length();
        Objects.checkFromToIndex(start, end, n);
        str = s;
        codeUnits = cursorFor(s);
        this.isReadOnly = true;
    }

    public CharBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        int rem = (pos <= lim ? lim - pos : 0);
        return new StringCharBuffer(str,
                                    -1,
                                    0,
                                    rem,
                                    rem,
                                    offset + pos);
    }

    @Override
    public CharBuffer slice(int index, int length) {
        Objects.checkFromIndexSize(index, length, limit());
        return new StringCharBuffer(str,
                                    -1,
                                    0,
                                    length,
                                    length,
                                    offset + index);
    }

    private StringCharBuffer(CharSequence s,
                             int mark,
                             int pos,
                             int limit,
                             int cap,
                             int offset) {
        super(mark, pos, limit, cap, null, offset, null);
        str = s;
        codeUnits = cursorFor(s);
        this.isReadOnly = true;
    }

    public CharBuffer duplicate() {
        return new StringCharBuffer(str, markValue(),
                                    position(), limit(), capacity(), offset);
    }

    public CharBuffer asReadOnlyBuffer() {
        return duplicate();
    }

    public final char get() {
        return unitAt(nextGetIndex() + offset);
    }

    public final char get(int index) {
        return unitAt(checkIndex(index) + offset);
    }

    char getUnchecked(int index) {
        return unitAt(index + offset);
    }

    @Override
    public CharBuffer get(char[] dst, int off, int len) {
        if (!(str instanceof String text)) return super.get(dst, off, len);
        Objects.checkFromIndexSize(off, len, dst.length);
        int pos = position();
        if (len > limit() - pos) throw new BufferUnderflowException();
        text.getChars(pos + offset, pos + offset + len, dst, off);
        position(pos + len);
        return this;
    }

    @Override
    public CharBuffer get(int index, char[] dst, int off, int len) {
        if (!(str instanceof String text)) return super.get(index, dst, off, len);
        Objects.checkFromIndexSize(index, len, limit());
        Objects.checkFromIndexSize(off, len, dst.length);
        text.getChars(index + offset, index + offset + len, dst, off);
        return this;
    }

    public final CharBuffer put(char c) {
        throw new ReadOnlyBufferException();
    }

    public final CharBuffer put(int index, char c) {
        throw new ReadOnlyBufferException();
    }

    public final CharBuffer compact() {
        throw new ReadOnlyBufferException();
    }

    public final boolean isReadOnly() {
        return true;
    }

    final String toString(int start, int end) {
        return str.subSequence(start + offset, end + offset).toString();
    }

    public final CharBuffer subSequence(int start, int end) {
        try {
            int pos = position();
            return new StringCharBuffer(str,
                                        -1,
                                        pos + checkIndex(start, pos),
                                        pos + checkIndex(end, pos),
                                        capacity(),
                                        offset);
        } catch (IllegalArgumentException x) {
            throw new IndexOutOfBoundsException();
        }
    }

    public boolean isDirect() {
        return false;
    }

    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }

    ByteOrder charRegionOrder() {
        return null;
    }

    boolean isAddressable() {
        return false;
    }

    public boolean equals(Object ob) {
        if (this == ob)
            return true;
        if (!(ob instanceof CharBuffer that))
            return false;
        int thisPos = this.position();
        int thisRem = this.limit() - thisPos;
        int thatPos = that.position();
        int thatRem = that.limit() - thatPos;
        if (thisRem < 0 || thisRem != thatRem)
            return false;
        return BufferMismatch.mismatch(this, thisPos,
                                       that, thatPos,
                                       thisRem) < 0;
    }

    public int compareTo(CharBuffer that) {
        int thisPos = this.position();
        int thisRem = this.limit() - thisPos;
        int thatPos = that.position();
        int thatRem = that.limit() - thatPos;
        int length = Math.min(thisRem, thatRem);
        if (length < 0)
            return -1;
        int i = BufferMismatch.mismatch(this, thisPos,
                                        that, thatPos,
                                        length);
        if (i >= 0) {
            return Character.compare(this.get(thisPos + i), that.get(thatPos + i));
        }
        return thisRem - thatRem;
    }
}
