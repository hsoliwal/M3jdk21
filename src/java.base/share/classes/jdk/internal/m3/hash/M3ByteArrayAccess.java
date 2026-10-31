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
import jdk.internal.misc.Unsafe;
import jdk.internal.util.ByteArrayLittleEndian;

import static jdk.internal.m3.hash.M3Primitives.*;

public class M3ByteArrayAccess extends M3Access<byte[]> {
    static final M3ByteArrayAccess INSTANCE;
    private static final M3Access<byte[]> INSTANCE_NON_NATIVE;

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    static final long BYTE_BASE = 0L;

    static {
        INSTANCE = new M3ByteArrayAccess();
        INSTANCE_NON_NATIVE = M3Access.newDefaultReverseAccess(INSTANCE);
    }

    private M3ByteArrayAccess() {}

    //FLLDCIM: HPCPOP - UNSAFE_TO_BYTE_ARRAY v2: the read atom's address (index base 0, no check)
    private static long address(final long offset) {
        return Unsafe.ARRAY_BYTE_BASE_OFFSET + offset;
    }

    //FLLDCIM: HPCPOP - UNSAFE_TO_BYTE_ARRAY v2: unchecked i64; the caller checked [off, off + len)
    @Override
    public long i64(final byte[] input, final long offset) {
        return UNSAFE.getLongUnaligned(input, address(offset), false);
    }

    //FLLDCIM: HPCPOP - UNSAFE_TO_BYTE_ARRAY v2: unchecked u32; the caller checked [off, off + len)
    @Override
    public long u32(final byte[] input, final long offset) {
        return unsignedInt(UNSAFE.getIntUnaligned(input, address(offset), false));
    }

    //FLLDCIM: HPCPOP - UNSAFE_TO_BYTE_ARRAY v2: unchecked i32; the caller checked [off, off + len)
    @Override
    public int i32(final byte[] input, final long offset) {
        return UNSAFE.getIntUnaligned(input, address(offset), false);
    }

    //FLLDCIM: HPCPOP - UNSAFE_TO_BYTE_ARRAY v2: unchecked u16; the caller checked [off, off + len)
    @Override
    public int u16(final byte[] input, final long offset) {
        return unsignedShort(UNSAFE.getShortUnaligned(input, address(offset), false));
    }

    //FLLDCIM: HPCPOP - UNSAFE_TO_BYTE_ARRAY v2: unchecked i16; the caller checked [off, off + len)
    @Override
    public int i16(final byte[] input, final long offset) {
        return UNSAFE.getShortUnaligned(input, address(offset), false);
    }

    @Override
    public long getLong(byte[] input, long offset) {
        return ByteArrayLittleEndian.getLong(input, (int) offset);
    }

    @Override
    public long getUnsignedInt(byte[] input, long offset) {
        return unsignedInt(getInt(input, offset));
    }

    @Override
    public int getInt(byte[] input, long offset) {
        return ByteArrayLittleEndian.getInt(input, (int) offset);
    }

    @Override
    public int getUnsignedShort(byte[] input, long offset) {
        return unsignedShort(getShort(input, offset));
    }

    @Override
    public int getShort(byte[] input, long offset) {
        return ByteArrayLittleEndian.getShort(input, (int) offset);
    }

    @Override
    public int getUnsignedByte(byte[] input, long offset) {
        return unsignedByte(getByte(input, offset));
    }

    @Override
    public int getByte(byte[] input, long offset) {
        return input[(int) offset];
    }

    @Override
    public ByteOrder byteOrder(byte[] input) {
        return ByteOrder.LITTLE_ENDIAN;
    }

    @Override
    protected M3Access<byte[]> reverseAccess() {
        return INSTANCE_NON_NATIVE;
    }
}
