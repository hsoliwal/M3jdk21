// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.zip.CRC32C;

/** Running CRC32C shadow (JDK intrinsic): the fast per-entry hash. */
public final class M3FileCrcShadow implements M3FileShadow {
    private final CRC32C crc = new CRC32C();

    M3FileCrcShadow() {
    }

    @Override
    public void update(byte[] bytes, int offset, int length) {
        crc.update(bytes, offset, length);
    }

    /** CRC32C of the bytes seen so far. */
    public long value() {
        return crc.getValue();
    }
}
