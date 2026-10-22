// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Nebula application facade for the Synexia M3 verbatim pre-parser source-custody contract.
 *
 * <p>This atom verifies exact repository bytes before OpenRewrite owns the structured Java replay.
 * It never mutates source and never grants replacement or promotion authority.</p>
 */
public final class NebulaM3VerbatimSourceSeal {
    /** Authority-free raw-source observation. */
    public record Receipt(
            String logicalPath,
            String sha256,
            long bytes,
            boolean sourceMutationAuthority,
            boolean replacementAuthority,
            boolean promotionAuthority,
            String root) {
        public Receipt {
            logicalPath = relativePath(logicalPath);
            sha256 = digest(sha256, "sha256");
            if (bytes < 0L) {
                throw new IllegalArgumentException("bytes");
            }
            if (sourceMutationAuthority || replacementAuthority || promotionAuthority) {
                throw new IllegalArgumentException("verbatim source seal is authority-free");
            }
            String expected =
                    NebulaM3VerbatimSourceSeal.sha256(
                            ("schema=nebula-m3-verbatim-source-seal/v1\n"
                                            + "path="
                                            + logicalPath
                                            + "\nsha256="
                                            + sha256
                                            + "\nbytes="
                                            + bytes
                                            + "\nsourceMutationAuthority=false"
                                            + "\nreplacementAuthority=false"
                                            + "\npromotionAuthority=false\n")
                                    .getBytes(StandardCharsets.UTF_8));
            root = root == null || root.isBlank() ? expected : digest(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("verbatim source-seal receipt root mismatch");
            }
        }
    }

    private NebulaM3VerbatimSourceSeal() {}

    /**
     * Verify one exact repository-relative raw file preimage.
     *
     * @param repositoryRoot explicit Nebula checkout root
     * @param logicalPath repository-relative target path
     * @param expectedSha256 reviewed raw-byte SHA-256
     * @return authority-free content-addressed receipt
     */
    public static Receipt verify(
            Path repositoryRoot, String logicalPath, String expectedSha256) throws IOException {
        String path = relativePath(logicalPath);
        String expected = digest(expectedSha256, "expectedSha256");
        Path target = target(repositoryRoot, path);
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("regular source required: " + path);
        }

        long bytes = Files.size(target);
        String actual = sha256(target);
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    "M3 verbatim source preimage drift: "
                            + path
                            + " expected="
                            + expected
                            + " actual="
                            + actual);
        }
        return new Receipt(path, actual, bytes, false, false, false, "");
    }

    /** Streamed SHA-256 over exact file bytes after path custody is separately established. */
    static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(Objects.requireNonNull(bytes, "bytes")));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static Path target(Path repositoryRoot, String logicalPath) throws IOException {
        Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot")
                .toAbsolutePath()
                .normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(root)) {
            throw new IllegalArgumentException(
                    "repository root must be a non-symbolic-link directory");
        }

        Path target = root.resolve(logicalPath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("source target escapes repository root");
        }

        Path cursor = root;
        for (String part : logicalPath.split("/")) {
            cursor = cursor.resolve(part);
            if (Files.isSymbolicLink(cursor)) {
                throw new IllegalArgumentException(
                        "source path component may not be a symbolic link: " + part);
            }
            if (!Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) {
                break;
            }
        }
        return target;
    }

    private static String relativePath(String value) {
        String checked = Objects.toString(value, "").strip().replace('\\', '/');
        if (checked.isEmpty()
                || checked.startsWith("/")
                || checked.indexOf(':') >= 0
                || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("logicalPath");
        }
        for (String part : checked.split("/", -1)) {
            if (part.isEmpty()
                    || part.equals(".")
                    || part.equals("..")
                    || !part.equals(part.strip())) {
                throw new IllegalArgumentException("logicalPath");
            }
        }
        return checked;
    }

    private static String digest(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }

    /**
     * CLI used by the Nebula proving workflow.
     *
     * <p>Arguments: checkout-root, repository-relative path, expected raw-byte SHA-256.</p>
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "checkout root, logical path and expected SHA-256 required");
        }
        Receipt receipt = verify(Path.of(args[0]), args[1], args[2]);
        System.out.println("SOURCE_SEAL_PATH=" + receipt.logicalPath());
        System.out.println("SOURCE_SEAL_SHA256=" + receipt.sha256());
        System.out.println("SOURCE_SEAL_BYTES=" + receipt.bytes());
        System.out.println("SOURCE_SEAL_ROOT=" + receipt.root());
        System.out.println("SOURCE_SEAL_MUTATION_AUTHORITY=false");
        System.out.println("SOURCE_SEAL_REPLACEMENT_AUTHORITY=false");
        System.out.println("SOURCE_SEAL_PROMOTION_AUTHORITY=false");
    }
}
