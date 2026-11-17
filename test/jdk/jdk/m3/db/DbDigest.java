// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

/** Runs only in an isolated fault JVM with its own startup security configuration. */
final class DbDigest {
    private DbDigest() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("private probe directory required");
        missing(IllegalStateException.class,
                () -> Artifact.create("key", "data", 1, new byte[] {1}));
        try (M3DB db = M3DB.open(Path.of(args[0]))) {
            missing(AssertionError.class, () -> db.containsArtifact("key"));
        }
        missing(AssertionError.class, () -> SemanticFingerprint.utf16Sha256("key"));
        System.out.println("DB_DIGEST_FAULT_PASS");
    }

    private static void missing(Class<? extends Throwable> kind, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable failure) {
            if (kind.isInstance(failure) && failure.getCause() instanceof NoSuchAlgorithmException) {
                return;
            }
            throw new AssertionError("wrong missing-digest failure", failure);
        }
        throw new AssertionError("digest failure was silently accepted");
    }
}
