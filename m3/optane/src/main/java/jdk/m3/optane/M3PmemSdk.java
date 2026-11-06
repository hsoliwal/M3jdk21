// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.util.Objects;

/** Low-level, fail-closed, file-backed mapping API. See docs/API_CONTRACT.md. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemSdk.java blob 360b782ed01b; recipe m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
public final class M3PmemSdk {
    private M3PmemSdk() {}

    /** Preserve the actual filesystem traversal, including parent symlinks before '..'. */
    static Path nativeRealPath(Path path) throws IOException {
        Path resolved = path.toRealPath();
        if (!Path.of(resolved.toString()).equals(resolved))
            throw new IOException("PMem native path is not losslessly representable");
        return resolved;
    }

    /** Checks runtime library loadability; does not probe physical PMem. */
    public static M3PmemCapabilities capabilities() {
        try {
            int bitset = M3NativePmem.capabilities();
            return new M3PmemCapabilities(true, (bitset & 1) != 0, M3NativePmem.pageSize(),
                    System.getProperty("os.name"));
        } catch (LinkageError e) {
            return new M3PmemCapabilities(false, false, 0, System.getProperty("os.name"));
        }
    }

    /**
     * Map an existing regular file range with long offsets. The file must not be
     * truncated/replaced until close, including while open acquires the owner.
     * FILE captures provider file keys for alias checks; unavailable keys cause
     * potentially overlapping distinct-owner copies to fail closed. This SDK
     * does not create DAX namespaces.
     */
    public static M3PmemRegion open(Path path, long offset, long length,
                                  M3PmemAccess access, M3PmemBackend backend) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(backend, "backend");
        if (offset < 0 || length <= 0 || length > Long.MAX_VALUE - offset) {
            throw new IllegalArgumentException("invalid mapped file span");
        }
        if (backend == M3PmemBackend.FILE) {
            M3PmemMapping.FileIdentity before = M3PmemMapping.readFileIdentity(path);
            FileChannel channel = access == M3PmemAccess.READ_WRITE
                    ? FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)
                    : FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
            try {
                M3PmemMapping.FileIdentity after = M3PmemMapping.readFileIdentity(path);
                if (!Objects.equals(before, after)) throw new IOException("PMem path identity changed while opening");
                long size = channel.size();
                if (offset > size || length > size - offset)
                    throw new IOException("PMem file span exceeds file size");
                return new M3PmemMapping(path, offset, length, access, channel, after);
            } catch (IOException | RuntimeException | Error failure) {
                try { channel.close(); }
                catch (IOException cleanupError) { failure.addSuppressed(cleanupError); }
                throw failure;
            }
        }
        Path nativePath = nativeRealPath(path);
        byte[] encoded = nativePath.toString()
                .getBytes(StandardCharsets.UTF_8);
        if (encoded.length == 0) throw new IllegalArgumentException("empty path");
        M3PmemMapping.FileIdentity before = M3PmemMapping.optionalFileIdentity(nativePath);
        long handle = M3NativePmem.open(encoded, offset, length, access.ordinal(), backend.ordinal());
        try {
            M3PmemMapping.FileIdentity after = M3PmemMapping.optionalFileIdentity(nativePath);
            M3PmemMapping.FileIdentity identity = Objects.equals(before, after) ? after : null;
            return new M3PmemMapping(path, offset, length, access, handle, identity);
        } catch (RuntimeException | Error failure) {
            try { M3NativePmem.close(handle); }
            catch (IOException cleanupError) { failure.addSuppressed(cleanupError); }
            throw failure;
        }
    }
}
