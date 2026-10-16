/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary Identical typed primitive addresses in separately exec-started JVMs
 * @requires (os.family == "linux") & (sun.arch.data.model == "64")
 * @modules java.base/jdk.internal.mindex
 * @run main/native PrimitiveIdentityFixedTest
 */
import jdk.internal.mindex.M3PrimitiveIdentity;
import jdk.internal.mindex.M3PrimitiveImage;
import static jdk.internal.mindex.M3PrimitiveIdentity.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PrimitiveIdentityFixedTest {
    private static native void registerMapping();
    private static native long readKey(long address);
    private static int checks;
    private static final int[] KINDS = {BOOLEAN, BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE};
    private static final long[] KEYS = {1L, -128L, -32768L, 65535L, 42L,
            Long.MIN_VALUE, 0x7f800001L, 0x7ff0000000000001L};
    private static final UUID NS = UUID.fromString("66684406-8739-53ee-7ffa-8ba9cd505833");
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("fixed check " + checks); }
    interface Action { void run() throws Exception; }
    private static void fails(Class<? extends Throwable> expected, Action action) throws Exception {
        try { action.run(); } catch (Throwable failure) {
            if (expected.isInstance(failure)) { checks++; return; }
            throw new AssertionError("wrong failure", failure);
        }
        throw new AssertionError("missing " + expected.getSimpleName());
    }
    private static void verify(M3PrimitiveImage image, long base) {
        check(image.size() == 8);
        for (int kind = 0; kind < 8; kind++) {
            final int k = kind;
            int slot = image.find(k, KEYS[k]);
            check(slot == k && image.kind(slot) == k && image.key(slot) == KEYS[k]);
            image.withAddress(slot, address -> {
                check(address == base + 96L + 16L * k + 8L);
                check(readKey(address) == KEYS[k]);
            });
        }
    }
    private static List<String> childCommand(String library, Path image, long base) {
        var args = new ArrayList<String>();
        args.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        String patch = System.getProperty("m3.test.patch");
        if (patch != null) { args.add("--patch-module"); args.add("java.base=" + patch); }
        args.addAll(List.of("--add-exports", "java.base/jdk.internal.mindex=ALL-UNNAMED", "-cp",
                System.getProperty("java.class.path"), "PrimitiveIdentityFixedTest", library,
                "child", image.toString(), Long.toString(base)));
        return args;
    }
    public static void main(String[] args) throws Exception {
        String library = args.length == 0
                ? Path.of(System.getProperty("test.nativepath"), System.mapLibraryName("PrimitiveIdentityTest")).toString()
                : Path.of(args[0]).toAbsolutePath().toString();
        System.load(library);
        registerMapping(); // only the test adapter; a matched libjava contains the product exports.
        if (args.length > 1 && args[1].equals("child")) {
            long base = Long.parseLong(args[3]);
            try (var image = M3PrimitiveImage.openFixed(Path.of(args[2]), base)) {
                verify(image, base);
                System.out.println("pid=" + ProcessHandle.current().pid() + " fixedBase=0x"
                        + Long.toHexString(base) + " int42Address=0x" + Long.toHexString(base + 168)
                        + " slots=8 checks=" + checks + " PASS");
            }
            return;
        }
        Path dir = Files.createTempDirectory("m3-fixed-identity-");
        Path imagePath = dir.resolve("image.bin");
        try {
            check(M3PrimitiveImage.publish(imagePath, NS, 1, KINDS, KEYS) == 8);
            long base = 0;
            M3PrimitiveImage selected = null;
            IOException last = null;
            // Candidate VAs for this 64-bit Linux proof, not a universal product address ABI.
            for (long candidate : new long[]{0x200000000000L, 0x300000000000L, 0x400000000000L}) {
                try { selected = M3PrimitiveImage.openFixed(imagePath, candidate); base = candidate; break; }
                catch (IOException unavailable) { last = unavailable; }
            }
            if (selected == null) throw new IOException("no common proof arena available", last);
            final long chosenBase = base;
            try (var image = selected) {
                verify(image, base);
                System.out.println("pid=" + ProcessHandle.current().pid() + " fixedBase=0x"
                        + Long.toHexString(base) + " int42Address=0x" + Long.toHexString(base + 168));
                fails(IOException.class, () -> M3PrimitiveImage.openFixed(imagePath, chosenBase));
                verify(image, base); // collision must not overwrite the live mapping.
                System.out.println("collisionRefusedOriginalMappingIntact=PASS");
                fails(IOException.class, () -> M3PrimitiveImage.openFixed(imagePath, chosenBase + 1));
                fails(IllegalArgumentException.class, () -> M3PrimitiveImage.openFixed(imagePath, -1));
                fails(IllegalStateException.class, () -> image.withAddress(0, address -> image.close()));
                fails(IllegalStateException.class, () -> image.withAddress(0, address -> { throw new IllegalStateException(); }));
                verify(image, base);
                List<Process> children = new ArrayList<>();
                try {
                    for (int i = 0; i < 2; i++) children.add(new ProcessBuilder(childCommand(library, imagePath, base))
                            .redirectErrorStream(true).start());
                    for (Process child : children) {
                        check(child.waitFor(20, TimeUnit.SECONDS));
                        String output = new String(child.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        check(child.exitValue() == 0 && output.contains("fixedBase=0x" + Long.toHexString(base))
                                && output.contains("int42Address=0x" + Long.toHexString(base + 168))
                                && output.contains("slots=8") && output.contains("PASS"));
                        System.out.print(output);
                    }
                } finally { for (Process child : children) if (child.isAlive()) child.destroyForcibly(); }
                verify(image, base);
            }
            // Failed validation must release the reservation; no slot/image ABA is allowed while live.
            byte[] corrupt = Files.readAllBytes(imagePath); corrupt[corrupt.length - 1] ^= 1;
            Path bad = dir.resolve("bad.bin"); Files.write(bad, corrupt);
            fails(IOException.class, () -> M3PrimitiveImage.openFixed(bad, chosenBase));
            try (var reopened = M3PrimitiveImage.openFixed(imagePath, base)) { verify(reopened, base); }
            System.out.println("fixedValidationCleanupAndReopen=PASS");
            System.out.println("PrimitiveIdentityFixedTest checks=" + checks + " independentJVMs=3 PASS");
        } finally {
            try (var files = Files.list(dir)) { for (Path path : files.toList()) Files.deleteIfExists(path); }
            Files.deleteIfExists(dir);
        }
    }
}
