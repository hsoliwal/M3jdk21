/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK 21 from cpullm-java/src/main/java/jdk/internal/m3/cpullm/facets/FacetKernels.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.facets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Optional batched bit arithmetic. Trusted native library identity is not process sandboxing. */
public final class FacetKernels {
    private static String installedPath, installedSha;
    private static boolean verifiedNative;
    private final boolean nativeExecution;
    private final AtomicLong calls = new AtomicLong();
    private FacetKernels(boolean nativeExecution) { this.nativeExecution = nativeExecution; }
    public static FacetKernels javaOnly() { return new FacetKernels(false); }
    public static synchronized FacetKernels pinnedJni(Path library, String sha256) throws IOException {
        Objects.requireNonNull(library, "library");
        if (!library.isAbsolute() || sha256 == null || !sha256.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("absolute library and SHA-256 required");
        Path p = library.normalize();
        for (Path at = p; at != null; at = at.getParent()) if (Files.isSymbolicLink(at)) throw new IOException("library symlink rejected");
        if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS) || Files.size(p) > 64L * 1024 * 1024) throw new IOException("library file/budget rejected");
        if (!fileHash(p).equals(sha256)) throw new IOException("library identity differs");
        if (installedPath != null && (!installedPath.equals(p.toString()) || !installedSha.equals(sha256))) throw new IOException("one pinned library per process");
        if (installedPath == null) {
            installedPath = p.toString(); installedSha = sha256;
            System.load(p.toString());
            if (abiNative() != 1 || !fileHash(p).equals(sha256)) throw new IOException("native ABI or post-load identity differs");
            verifiedNative = true;
        }
        if (!verifiedNative) throw new IOException("earlier native admission failed; use a fresh process");
        return new FacetKernels(true);
    }
    public int[] gains(long[] masks, long required) {
        Objects.requireNonNull(masks, "masks");
        if (masks.length > FacetCorpus.MAX_ATOMS) throw new IllegalArgumentException("gain batch limit");
        FacetCorpus.cancelled(); calls.incrementAndGet();
        if (nativeExecution) return gainsNative(masks, required);
        int[] output = new int[masks.length];
        for (int i = 0; i < masks.length; i++) output[i] = Long.bitCount(masks[i] & required);
        return output;
    }
    public String implementation() { return nativeExecution ? "JNI_BATCH_POPCOUNT_V1" : "JAVA_LONG_BITCOUNT"; }
    public long calls() { return calls.get(); }
    private static String fileHash(Path path) throws IOException {
        var hash = FacetCorpus.sha();
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[65_536]; int n; long total = 0;
            while ((n = input.read(buffer)) != -1) {
                FacetCorpus.cancelled(); total += n;
                if (total > 64L * 1024 * 1024) throw new IOException("library grew beyond hash budget");
                hash.update(buffer, 0, n);
            }
        }
        return HexFormat.of().formatHex(hash.digest());
    }
    private static native int abiNative();
    private static native int[] gainsNative(long[] masks, long required);
}
