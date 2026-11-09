/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DigestOpsTest {

    private static final String ABC_SHA256 =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Test
    void hashesKnownUtf8Vector() {
        assertEquals(ABC_SHA256, DigestOps.sha256HexUtf8("abc"));
        assertArrayEquals(DigestOps.sha256Utf8("abc"), DigestOps.sha256("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void streamsWithoutClosingCallerOwnedInput() throws IOException {
        TrackingInputStream input = new TrackingInputStream("abc".getBytes(StandardCharsets.UTF_8));

        assertEquals(ABC_SHA256, DigestOps.sha256Hex(input));
        assertFalse(input.closed);
    }

    @Test
    void hashesExactFileBytes(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("payload.bin");
        Files.write(file, "abc".getBytes(StandardCharsets.UTF_8));

        assertEquals(ABC_SHA256, DigestOps.sha256Hex(file));
    }

    @Test
    void validatesOnlySha256HexShape() {
        assertTrue(DigestOps.isSha256Hex(ABC_SHA256));
        assertTrue(DigestOps.isSha256Hex(ABC_SHA256.toUpperCase()));
        assertFalse(DigestOps.isSha256Hex(null));
        assertFalse(DigestOps.isSha256Hex(""));
        assertFalse(DigestOps.isSha256Hex(ABC_SHA256.substring(1)));
        assertFalse(DigestOps.isSha256Hex(ABC_SHA256.substring(0, 63) + "g"));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> DigestOps.sha256((byte[]) null));
        assertThrows(NullPointerException.class, () -> DigestOps.sha256Utf8(null));
        assertThrows(NullPointerException.class, () -> DigestOps.sha256((InputStream) null));
        assertThrows(NullPointerException.class, () -> DigestOps.sha256((Path) null));
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        private TrackingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
