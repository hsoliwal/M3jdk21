// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package jdk.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Fixed wire vectors, independent of the reader's fingerprint implementation. */
class M3ColdImageFingerprintTest {
    @TempDir Path temp;

    @Test void acceptsOriginalDomainWireVector() throws Exception {
        long fingerprint = 0xe32cdf7f5da0133bL;
        assertArrayEquals(new long[] {fingerprint, 0, 0},
                M3ColdFileImage.validate(mapped(fingerprint), language -> 0));
    }

    @Test void acceptsCurrentM3FileImageDomainWireVector() throws Exception {
        long fingerprint = 0x17bade2ab5e2ce69L;
        assertArrayEquals(new long[] {fingerprint, 0, 0},
                M3ColdFileImage.validate(mapped(fingerprint), language -> 0));
    }

    @Test void rejectsAlteredFingerprintInBothDomains() {
        assertThrows(IOException.class, () -> M3ColdFileImage.validate(
                mapped(0xe32cdf7f5da0133bL ^ 1), language -> 0));
        assertThrows(IOException.class, () -> M3ColdFileImage.validate(
                mapped(0x17bade2ab5e2ce69L ^ 1), language -> 0));
    }

    private M3MappedObject mapped(long fingerprint) throws Exception {
        // Empty SMFILE01: width=1, one line start, all CSR/offset sentinels zero.
        byte[] bytes = ByteBuffer.allocate(68).putLong(0x534d46494c453031L)
                .putInt(1).putInt(0).putInt(0).putInt(1).putInt(1)
                .putInt(0).putInt(0).putInt(0).putLong(fingerprint).array();
        Path path = Files.write(temp.resolve(Long.toHexString(fingerprint)), bytes);
        M3ObjectRef reference = new M3ObjectRef(
                M3ContentId.of(MessageDigest.getInstance("SHA-256").digest(bytes)),
                bytes.length, M3ObjectKind.MINDEX_FILE_IMAGE);
        return M3MappedObject.open(path, reference, bytes.length);
    }
}
