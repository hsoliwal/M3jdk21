// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;
//FLLDCIM: RESTORE - source m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java/jdk/m3/file/M3FileCrcShadow.java blob fd9a1af1da24310d68c04ab57a16ba42857581b1 (retained PR #1619 head; parent 0cd0944136c1 blob 0c433352dfe3212311021a4c7e63ccbd98f0f74c https://github.com/hsoliwal/M3jdk21/blob/0cd0944136c1/m3/file/src/main/java/com/m3/file/M3FileCrcShadow.java); recipe m3/file/qualification/retained-port-20261107 RESTORE_EXACT

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
