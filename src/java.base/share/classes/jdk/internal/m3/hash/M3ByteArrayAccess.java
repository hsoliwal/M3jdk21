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
import jdk.internal.util.ByteArrayLittleEndian;

import static jdk.internal.m3.hash.M3Primitives.*;

public class M3ByteArrayAccess extends M3Access<byte[]> {
    static final M3ByteArrayAccess INSTANCE;
    private static final M3Access<byte[]> INSTANCE_NON_NATIVE;

    static final long BYTE_BASE = 0L;

    static {
        INSTANCE = new M3ByteArrayAccess();
        INSTANCE_NON_NATIVE = M3Access.newDefaultReverseAccess(INSTANCE);
    }

    private M3ByteArrayAccess() {}

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
