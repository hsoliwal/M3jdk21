// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atoms;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Content-addressed, read-only class-file evidence for an existing companion
 * candidate. This does not produce an API, modify a JDK class, or establish
 * binary/source/semantic compatibility. The JDK class-file bytes, not the
 * runtime-version string, are the identity of this particular evidence.
 */
public final class ClassfileSeal {
    public static final String SCHEMA = "M3_CLASSFILE_SHA256_V1";
    private static final int BUFFER_SIZE = 8192;

    private ClassfileSeal() { }

    public record Seal(String moduleName, String binaryName, long bytes, String sha256) {
        public Seal {
            Objects.requireNonNull(moduleName, "moduleName");
            Objects.requireNonNull(binaryName, "binaryName");
            Objects.requireNonNull(sha256, "sha256");
            if (bytes <= 0 || !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("invalid class-file evidence");
            }
        }
    }

    /**
     * Seal only a REVIEW candidate, after the existing JPMS/visibility checks;
     * reserved-owner and capability mapping decisions are still independent.
     * The bounded stream is consumed without materializing the class file.
     * A too-small budget fails closed, never returning a truncated digest.
     */
    public static Seal snapshot(CompanionPlan.Request request, long maxBytes)
            throws IOException {
        Objects.requireNonNull(request, "request");
        if (maxBytes < 1L) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        CompanionPlan.Row review = CompanionPlan.inspect(
                List.of(request), Set.of()).getFirst();
        if (review.decision() != CompanionPlan.Decision.REVIEW) {
            throw new IllegalArgumentException(
                    "class is not a public companion candidate: " + review.decision());
        }
        String name = request.binaryName();
        if (!name.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+")) {
            throw new IllegalArgumentException("invalid binary class name");
        }
        ModuleReference module = ModuleFinder.ofSystem().find(request.moduleName())
                .orElseThrow(() -> new IllegalArgumentException("system module not installed"));
        String resource = name.replace('.', '/') + ".class";
        MessageDigest digest = newDigest();
        long size = 0L;
        try (var reader = module.open()) {
            try (InputStream bytes = reader.open(resource).orElseThrow(
                    () -> new IOException("missing class resource: " + resource))) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = bytes.read(buffer)) != -1) {
                    if (read > maxBytes - size) {
                        throw new IllegalStateException("class resource exceeds byte budget: " + name);
                    }
                    digest.update(buffer, 0, read);
                    size += read;
                }
            }
        }
        if (size == 0L) {
            throw new IllegalStateException("empty class resource: " + name);
        }
        return new Seal(request.moduleName(), name, size,
                HexFormat.of().formatHex(digest.digest()));
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException unexpected) {
            throw new IllegalStateException("JDK SHA-256 is unavailable", unexpected);
        }
    }
}
