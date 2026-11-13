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
public final class PmemSdk {
    private PmemSdk() {}

    /** Resolve parents with filesystem semantics, retaining the final component for O_NOFOLLOW. */
    static Path nativeRealPath(Path path) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        Path resolved = parent == null ? absolute
                : parent.toRealPath().resolve(absolute.getFileName());
        if (!Path.of(resolved.toString()).equals(resolved))
            throw new IOException("PMem native path is not losslessly representable");
        return resolved;
    }

    /** Checks runtime library loadability; does not probe physical PMem. */
    public static PmemCapabilities capabilities() {
        try {
            int bitset = NativePmem.capabilities();
            return new PmemCapabilities(true, (bitset & 1) != 0, NativePmem.pageSize(),
                    System.getProperty("os.name"));
        } catch (LinkageError e) {
            return new PmemCapabilities(false, false, 0, System.getProperty("os.name"));
        }
    }

    /**
     * Map an existing regular file range with long offsets. The file must not be
     * truncated/replaced until close, including while open acquires the owner.
     * FILE captures provider file keys for alias checks; unavailable keys cause
     * potentially overlapping distinct-owner copies to fail closed. This SDK
     * does not create DAX namespaces.
     */
    public static PmemRegion open(Path path, long offset, long length,
                                  PmemAccess access, PmemBackend backend) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(backend, "backend");
        if (offset < 0 || length <= 0 || length > Long.MAX_VALUE - offset) {
            throw new IllegalArgumentException("invalid mapped file span");
        }
        if (backend == PmemBackend.FILE) {
            PmemMapping.FileIdentity before = PmemMapping.readFileIdentity(path);
            FileChannel channel = access == PmemAccess.READ_WRITE
                    ? FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)
                    : FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
            try {
                PmemMapping.FileIdentity after = PmemMapping.readFileIdentity(path);
                if (!Objects.equals(before, after)) throw new IOException("PMem path identity changed while opening");
                long size = channel.size();
                if (offset > size || length > size - offset)
                    throw new IOException("PMem file span exceeds file size");
                return new PmemMapping(path, offset, length, access, channel, after);
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
        PmemMapping.FileIdentity before = PmemMapping.optionalFileIdentity(nativePath);
        long handle = NativePmem.open(encoded, offset, length, access.ordinal(), backend.ordinal());
        try {
            PmemMapping.FileIdentity after = PmemMapping.optionalFileIdentity(nativePath);
            PmemMapping.FileIdentity identity = Objects.equals(before, after) ? after : null;
            return new PmemMapping(path, offset, length, access, handle, identity);
        } catch (RuntimeException | Error failure) {
            try { NativePmem.close(handle); }
            catch (IOException cleanupError) { failure.addSuppressed(cleanupError); }
            throw failure;
        }
    }
}
