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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class M3ByteBufferAccess extends M3Access<ByteBuffer> {
    public static final M3ByteBufferAccess INSTANCE = new M3ByteBufferAccess();
    private static final M3Access<ByteBuffer> INSTANCE_REVERSE = M3Access.newDefaultReverseAccess(INSTANCE);

    private M3ByteBufferAccess() {}

    @Override
    public long getLong(ByteBuffer input, long offset) {
        return input.getLong((int) offset);
    }

    @Override
    public long getUnsignedInt(ByteBuffer input, long offset) {
        return M3Primitives.unsignedInt(getInt(input, offset));
    }

    @Override
    public int getInt(ByteBuffer input, long offset) {
        return input.getInt((int) offset);
    }

    @Override
    public int getUnsignedShort(ByteBuffer input, long offset) {
        return M3Primitives.unsignedShort(getShort(input, offset));
    }

    @Override
    public int getShort(ByteBuffer input, long offset) {
        return input.getShort((int) offset);
    }

    @Override
    public int getUnsignedByte(ByteBuffer input, long offset) {
        return M3Primitives.unsignedByte(getByte(input, offset));
    }

    @Override
    public int getByte(ByteBuffer input, long offset) {
        return input.get((int) offset);
    }

    @Override
    public ByteOrder byteOrder(ByteBuffer input) {
        return input.order();
    }

    @Override
    protected M3Access<ByteBuffer> reverseAccess() {
        return INSTANCE_REVERSE;
    }
}
