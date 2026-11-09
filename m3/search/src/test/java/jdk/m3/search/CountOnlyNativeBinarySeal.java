// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/CountOnlyNativeBinarySeal.java (blob d83c73795307) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/** Fail-closed identity gate for the original JNI binary used by cross-backend tests. */
final class CountOnlyNativeBinarySeal {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final int CHUNK_BYTES = 64 * 1024;

    private CountOnlyNativeBinarySeal() { }

    /** Verify a real, nonsymlink regular binary before allowing JNI test activation. */
    static Path requireExact(Path library, String expectedSha256) {
        Objects.requireNonNull(library, "library");
        if (expectedSha256 == null || !SHA256.matcher(expectedSha256).matches()) {
            throw new IllegalArgumentException("m3.native.library.sha256 must be 64 lowercase hex digits");
        }
        Path path = library.toAbsolutePath().normalize();
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("JNI library is missing, linked or not a regular file: " + path);
        }
        try (InputStream input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] chunk = new byte[CHUNK_BYTES];
            for (int read; (read = input.read(chunk)) != -1; ) {
                if (read > 0) digest.update(chunk, 0, read);
            }
            if (!expectedSha256.equals(HexFormat.of().formatHex(digest.digest()))) {
                throw new IllegalStateException("JNI library SHA-256 mismatch: " + path);
            }
            return path;
        } catch (IOException | NoSuchAlgorithmException failure) {
            throw new IllegalStateException("Cannot verify JNI library SHA-256: " + path, failure);
        }
    }
}
