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

import java.nio.ByteOrder;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

public abstract class M3CharSequenceAccess extends M3Access<CharSequence> {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Bit position of the second char of a read at an even offset. */
    private static final int CHAR_1_SHIFT = 16;
    /** Bit position of the third char of a read at an even offset. */
    private static final int CHAR_2_SHIFT = 32;
    /** Bit position of the fourth char of a read at an even offset. */
    private static final int CHAR_3_SHIFT = 48;
    /** Bits of a byte: a read at an odd offset starts at the high byte of a char, which moves down by a byte; the next char moves up by one. */
    private static final int BYTE_BITS = 8;
    /** Bit position of the third char of a read at an odd offset. */
    private static final int ODD_CHAR_2_SHIFT = 24;
    /** Bit position of the fourth char of a read at an odd offset. */
    private static final int ODD_CHAR_3_SHIFT = 40;
    /** Bit position of the fifth char (its low byte) of a read at an odd offset. */
    private static final int ODD_CHAR_4_SHIFT = 56;

    static M3CharSequenceAccess charSequenceAccess(ByteOrder order) {
        return order == LITTLE_ENDIAN ?
                LittleEndianCharSequenceAccess.INSTANCE :
                BigEndianCharSequenceAccess.INSTANCE;
    }

    static M3CharSequenceAccess nativeCharSequenceAccess() {
        return charSequenceAccess(ByteOrder.nativeOrder());
    }

    private static int ix(long offset) {
        return (int) (offset >> 1);
    }

    protected static long getLong(CharSequence input, long offset,
                                  int char0Off, int char1Off, int char2Off, int char3Off,
                                  int char4Off, int delta) {
        final int base = ix(offset);
        if (0 == ((int)offset & 1)) {
            final long char0 = input.charAt(base + char0Off);
            final long char1 = input.charAt(base + char1Off);
            final long char2 = input.charAt(base + char2Off);
            final long char3 = input.charAt(base + char3Off);
            return char0 | (char1 << CHAR_1_SHIFT) | (char2 << CHAR_2_SHIFT) | (char3 << CHAR_3_SHIFT);
        } else {
            final long char0 = input.charAt(base + char0Off + delta) >>> BYTE_BITS;
            final long char1 = input.charAt(base + char1Off + delta);
            final long char2 = input.charAt(base + char2Off + delta);
            final long char3 = input.charAt(base + char3Off + delta);
            final long char4 = input.charAt(base + char4Off);
            return char0 | (char1 << BYTE_BITS) | (char2 << ODD_CHAR_2_SHIFT) | (char3 << ODD_CHAR_3_SHIFT) | (char4 << ODD_CHAR_4_SHIFT);
        }
    }

    protected static long getUnsignedInt(CharSequence input, long offset,
                                         int char0Off, int char1Off, int char2Off, int delta) {
        final int base = ix(offset);
        if (0 == ((int)offset & 1)) {
            final long char0 = input.charAt(base + char0Off);
            final long char1 = input.charAt(base + char1Off);
            return char0 | (char1 << CHAR_1_SHIFT);
        } else {
            final long char0 = input.charAt(base + char0Off + delta) >>> BYTE_BITS;
            final long char1 = input.charAt(base + char1Off + delta);
            final long char2 = M3Primitives.unsignedByte(input.charAt(base + char2Off));
            return char0 | (char1 << BYTE_BITS) | (char2 << ODD_CHAR_2_SHIFT);
        }
    }

    protected static char getUnsignedShort(CharSequence input,
                                           long offset, int char1Off, int delta) {
        if (0 == ((int)offset & 1)) {
            return input.charAt(ix(offset));
        } else {
            final int base = ix(offset);
            final int char0 = input.charAt(base + delta) >>> BYTE_BITS;
            final int char1 = input.charAt(base + char1Off);
            return (char)(char0 | (char1 << BYTE_BITS));
        }
    }

    protected static int getUnsignedByte(CharSequence input, long offset, int shift) {
        return M3Primitives.unsignedByte(input.charAt(ix(offset)) >> shift);
    }

    private M3CharSequenceAccess() {}

    @Override
    public int getInt(CharSequence input, long offset) {
        return (int) getUnsignedInt(input, offset);
    }

    @Override
    public int getShort(CharSequence input, long offset) {
        return (int)(short)getUnsignedShort(input, offset);
    }

    @Override
    public int getByte(CharSequence input, long offset) {
        return (int) (byte) getUnsignedByte(input, offset);
    }

    private static class LittleEndianCharSequenceAccess extends M3CharSequenceAccess {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** Index of the third char of a little-endian read. */
        private static final int CHAR_2_INDEX = 2;
        /** Index of the fourth char of a little-endian read. */
        private static final int CHAR_3_INDEX = 3;
        /** Index of the fifth char of a little-endian read at an odd offset. */
        private static final int CHAR_4_INDEX = 4;
        /** log2 of the bits of a byte: (offset & 1) << 3 is the bit position of the addressed byte in its char. */
        private static final int BYTE_BITS_LOG2 = 3;

        private static final M3CharSequenceAccess INSTANCE = new LittleEndianCharSequenceAccess();
        private static final M3Access<CharSequence> INSTANCE_REVERSE = M3Access.newDefaultReverseAccess(INSTANCE);

        private LittleEndianCharSequenceAccess() {}

        @Override
        public long getLong(CharSequence input, long offset) {
            return getLong(input, offset, 0, 1, CHAR_2_INDEX, CHAR_3_INDEX, CHAR_4_INDEX, 0);
        }

        @Override
        public long getUnsignedInt(CharSequence input, long offset) {
            return getUnsignedInt(input, offset, 0, 1, CHAR_2_INDEX, 0);
        }

        @Override
        public int getUnsignedShort(CharSequence input, long offset) {
            return getUnsignedShort(input, offset, 1, 0);
        }

        @Override
        public int getUnsignedByte(CharSequence input, long offset) {
            return getUnsignedByte(input, offset, ((int) offset & 1) << BYTE_BITS_LOG2);
        }

        @Override
        public ByteOrder byteOrder(CharSequence input) {
            return LITTLE_ENDIAN;
        }

        @Override
        protected M3Access<CharSequence> reverseAccess() {
            return INSTANCE_REVERSE;
        }
    }

    private static class BigEndianCharSequenceAccess extends M3CharSequenceAccess {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** Index of the char that holds the lowest bytes of a big-endian read. */
        private static final int CHAR_0_INDEX = 3;
        /** Index of the char that holds the next bytes of a big-endian read. */
        private static final int CHAR_1_INDEX = 2;
        /** log2 of the bits of a byte: (offset & 1) << 3 is the bit position of the addressed byte in its char. */
        private static final int BYTE_BITS_LOG2 = 3;

        private static final M3CharSequenceAccess INSTANCE = new BigEndianCharSequenceAccess();
        private static final M3Access<CharSequence> INSTANCE_REVERSE = M3Access.newDefaultReverseAccess(INSTANCE);

        private BigEndianCharSequenceAccess() {}

        @Override
        public long getLong(CharSequence input, long offset) {
            return getLong(input, offset, CHAR_0_INDEX, CHAR_1_INDEX, 1, 0, 0, 1);
        }

        @Override
        public long getUnsignedInt(CharSequence input, long offset) {
            return getUnsignedInt(input, offset, 1, 0, 0, 1);
        }

        @Override
        public int getUnsignedShort(CharSequence input, long offset) {
            return getUnsignedShort(input, offset, 0, 1);
        }

        @Override
        public int getUnsignedByte(CharSequence input, long offset) {
            return getUnsignedByte(input, offset, (((int) offset & 1) ^ 1) << BYTE_BITS_LOG2);
        }

        @Override
        public ByteOrder byteOrder(CharSequence input) {
            return BIG_ENDIAN;
        }

        @Override
        protected M3Access<CharSequence> reverseAccess() {
            return INSTANCE_REVERSE;
        }
    }
}
