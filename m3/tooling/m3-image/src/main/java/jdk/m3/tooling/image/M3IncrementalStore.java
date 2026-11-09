/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Verified content-addressed image cache. */
public final class M3IncrementalStore {
    private M3IncrementalStore() {
    }

    /** Computes a cache key from semantic inputs. */
    public static String key(M3ImagePlan plan, M3BackendKind backend, M3JdkTools tools,
                             M3BuildOptions options) {
        String material = "schema=1\n"
                + "plan=" + plan.planSha256() + "\n"
                + "backend=" + backend.name() + "\n"
                + "jdk=" + tools.runtimeIdentity() + "\n"
                + "imageName=" + options.imageName() + "\n"
                + "backendArgs=" + String.join("\u0000", options.backendArguments()) + "\n";
        return M3Hashing.sha256(material);
    }

    /** Restores a verified cache entry. */
    public static M3ImageBuildResult restore(Path cacheRoot, String key, Path output,
                                             M3BackendKind backend) throws IOException {
        Path entry = cacheRoot.resolve(key);
        if (!Files.isDirectory(entry)) {
            return null;
        }
        try {
            String hash = M3ImageManifest.verify(entry);
            Path complete = entry.resolve("CACHE_COMPLETE");
            if (!Files.isRegularFile(complete)
                    || !Files.readString(complete, StandardCharsets.UTF_8).trim().equals(hash)) {
                return null;
            }
            M3Files.freshDirectory(output);
            M3Files.deleteTree(output);
            M3Files.copyTree(entry, output);
            Files.deleteIfExists(output.resolve("CACHE_COMPLETE"));
            String restoredHash = M3ImageManifest.verify(output);
            return new M3ImageBuildResult(output, restoredHash, backend, "PASS_CACHE_RESTORE", true);
        } catch (IOException invalid) {
            M3Files.deleteTree(entry);
            return null;
        }
    }

    /** Publishes a verified image into the cache. */
    public static void publish(Path cacheRoot, String key, Path image) throws IOException {
        Files.createDirectories(cacheRoot);
        Path entry = cacheRoot.resolve(key);
        if (Files.exists(entry)) {
            M3Files.deleteTree(entry);
        }
        Path temporary = Files.createTempDirectory(cacheRoot, key + ".tmp-");
        try {
            M3Files.copyTree(image, temporary);
            String hash = M3ImageManifest.verify(temporary);
            M3Files.writeUtf8(temporary.resolve("CACHE_COMPLETE"), hash + "\n");
            try {
                Files.move(temporary, entry, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, entry);
            }
        } finally {
            if (Files.exists(temporary)) {
                M3Files.deleteTree(temporary);
            }
        }
    }
}
