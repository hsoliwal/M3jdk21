/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Fail-closed admission contract of the exact-address JNI mapper.
 *
 * <p>One mode runs per JVM because the mapper binds one JNI library and keeps every
 * admitted mapping for the process lifetime:</p>
 * <ul>
 * <li>{@code refusals <mapper> <other-library> <dir>}: argument, file, overflow, ownership
 * and library-binding refusals against the shipped mapper;</li>
 * <li>{@code fault <fault-double> <dir>}: the test-only native double answers with no
 * buffer, a heap buffer, a wrong capacity or a foreign address and every answer is refused;</li>
 * <li>{@code nosha <mapper> <dir>}: started with a provider-free {@code java.security}
 * file, admission refuses to publish an image it cannot hash.</li>
 * </ul>
 */
public final class M3FixedAddressMappingRefusalContractTest {
    private static final long STRIDE = 0x10_0000L;
    private static final long BASE = M3FixedAddressMapping.DEFAULT_BASE;
    private static final String ALIGNMENT = "fixed base must be positive and 64-KiB aligned";
    private static final String HEX = "SHA-256 must contain exactly 64 hexadecimal digits";
    private static final String OWNED = "different canonical image already owns fixed base";
    private static final String BOUND = "M3 fixed mapper JNI library already bound";
    private static final String NOT_EXACT = "JNI mapping did not return exact requested address";
    private static long checks;

    private M3FixedAddressMappingRefusalContractTest() { }

    private interface Action { void run() throws Exception; }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    private static void refused(Class<? extends Throwable> type, String reason, Action action) {
        try {
            action.run();
        } catch (Throwable thrown) {
            check(type.isInstance(thrown) && (reason == null || reason.equals(thrown.getMessage())),
                    "wrong refusal for <" + reason + ">: " + thrown);
            return;
        }
        throw new AssertionError("accepted: " + reason);
    }

    private static Path image(Path dir, String name, int length, int seed) throws IOException {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) bytes[i] = (byte) (seed + i * 7);
        return Files.write(dir.resolve(name), bytes);
    }

    private static String sha256(Path file) throws IOException, NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private static void refusals(Path library, Path other, Path dir) throws Exception {
        Path a = image(dir, "refusal-a.bin", 256, 1);
        Path b = image(dir, "refusal-b.bin", 256, 2);
        Path empty = Files.write(dir.resolve("refusal-empty.bin"), new byte[0]);
        Path wide = image(dir, "refusal-wide.bin", 0x1_0001, 3);
        String shaA = sha256(a);
        String shaB = sha256(b);
        refused(NullPointerException.class, "path", () -> M3FixedAddressMapping.open(null, BASE, shaA, library));
        refused(NullPointerException.class, "requiredSha256", () -> M3FixedAddressMapping.open(a, BASE, null, library));
        refused(NullPointerException.class, "nativeLibrary", () -> M3FixedAddressMapping.open(a, BASE, shaA, null));
        refused(IllegalArgumentException.class, ALIGNMENT, () -> M3FixedAddressMapping.open(a, 0, shaA, library));
        refused(IllegalArgumentException.class, ALIGNMENT, () -> M3FixedAddressMapping.open(a, -STRIDE, shaA, library));
        refused(IllegalArgumentException.class, ALIGNMENT, () -> M3FixedAddressMapping.open(a, BASE + 4096, shaA, library));
        refused(IllegalArgumentException.class, HEX, () -> M3FixedAddressMapping.open(a, BASE, "abc", library));
        refused(IllegalArgumentException.class, HEX, () -> M3FixedAddressMapping.open(a, BASE, "g".repeat(64), library));
        refused(IOException.class, "M3 shared image must be a regular file",
                () -> M3FixedAddressMapping.open(dir, BASE, shaA, library));
        refused(IOException.class, "M3 fixed mapping requires 1..Integer.MAX_VALUE bytes",
                () -> M3FixedAddressMapping.open(empty, BASE, shaA, library));
        refused(IllegalArgumentException.class, "fixed region overflows signed address range",
                () -> M3FixedAddressMapping.open(wide, Long.MAX_VALUE & ~0xffffL, sha256(wide), library));
        // Upper-case digests are admitted and published in canonical lower case.
        M3FixedAddressMapping.Image owner = M3FixedAddressMapping.open(a, BASE, shaA.toUpperCase(), library);
        check(owner.file().equals(a.toRealPath()), "canonical real file");
        check(owner.sha256().equals(shaA) && owner.byteLength() == 256 && owner.address() == BASE, "admitted facts");
        check(owner.readOnlyBuffer().get(255) == (byte) (1 + 255 * 7), "mapped bytes");
        check(M3FixedAddressMapping.open(a, BASE, shaA, library) == owner, "same owner is idempotent");
        refused(IllegalStateException.class, OWNED, () -> M3FixedAddressMapping.open(b, BASE, shaB, library));
        refused(IllegalStateException.class, OWNED, () -> M3FixedAddressMapping.open(a, BASE, shaB, library));
        refused(IllegalStateException.class, BOUND,
                () -> M3FixedAddressMapping.open(b, BASE + STRIDE, shaB, other));
        refused(IllegalStateException.class, BOUND,
                () -> M3FixedAddressMapping.reserveCollisionForProof(BASE + 2 * STRIDE, 256, other));
        // Re-using the bound library reserves the range; mapping there is then refused, never relocated.
        M3FixedAddressMapping.reserveCollisionForProof(BASE + 2 * STRIDE, STRIDE, library);
        refused(IOException.class, null, () -> M3FixedAddressMapping.open(b, BASE + 2 * STRIDE, shaB, library));
        M3FixedAddressMapping.Image second = M3FixedAddressMapping.open(b, BASE + 3 * STRIDE, shaB, library);
        check(second != owner && second.addressOfOffset(17) == BASE + 3 * STRIDE + 17, "independent second owner");
        System.out.println("M3_FIXED_ADDRESS_REFUSALS_PASS checks=" + checks);
    }

    private static void faults(Path library, Path dir) throws Exception {
        // Each length selects one broken answer of the fault double; none may be published.
        for (int length = 1; length <= 4; length++) {
            Path file = image(dir, "fault-" + length + ".bin", length, length);
            String sha = sha256(file);
            long base = BASE + length * STRIDE;
            refused(IllegalStateException.class, NOT_EXACT,
                    () -> M3FixedAddressMapping.open(file, base, sha, library));
            refused(IllegalStateException.class, NOT_EXACT,
                    () -> M3FixedAddressMapping.open(file, base, sha, library));
        }
        System.out.println("M3_FIXED_ADDRESS_FAULTS_PASS checks=" + checks);
    }

    private static void noSha(Path library, Path dir) throws Exception {
        check(Security.getProviders().length == 0, "provider-free platform was injected");
        byte[] bytes = new byte[128];
        Arrays.fill(bytes, (byte) 0x5a);
        Path file = Files.write(dir.resolve("nosha.bin"), bytes);
        try {
            M3FixedAddressMapping.open(file, BASE, "a".repeat(64), library);
            throw new IllegalStateException("admitted an image without SHA-256");
        } catch (AssertionError expected) {
            check(expected.getCause() instanceof NoSuchAlgorithmException, "missing SHA-256 is fatal: " + expected);
        }
        System.out.println("M3_FIXED_ADDRESS_NO_SHA256_PASS checks=" + checks);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("mode library [other-library] dir");
        Path dir = Files.createDirectories(Path.of(args[args.length - 1]));
        switch (args[0]) {
            case "refusals" -> refusals(Path.of(args[1]), Path.of(args[2]), dir);
            case "fault" -> faults(Path.of(args[1]), dir);
            case "nosha" -> noSha(Path.of(args[1]), dir);
            default -> throw new IllegalArgumentException("unknown mode " + args[0]);
        }
    }
}
