// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/*
 * @test id=file
 * @summary Existing-file admission preserves traversal and rejects final symlinks
 * @requires (os.family == "linux") & (vm.bits == "64")
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm test.PmemNativeOpenContractTest FILE
 */
/*
 * @test id=posix
 * @summary Native no-follow admission and access modes match the FILE contract
 * @requires (os.family == "linux") & (vm.bits == "64")
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xcheck:jni test.PmemNativeOpenContractTest POSIX
 * @run main/othervm -Xint -Xcheck:jni test.PmemNativeOpenContractTest POSIX
 */
/*
 * @test id=auto
 * @summary AUTO preserves no-follow and read-only admission with the installed backend
 * @requires (os.family == "linux") & (vm.bits == "64")
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xcheck:jni test.PmemNativeOpenContractTest AUTO
 */
package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import jdk.m3.optane.PmemAccess;
import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemCapabilities;
import jdk.m3.optane.PmemRegion;
import jdk.m3.optane.PmemSdk;

/** Real regular-file tests; successful PMDK execution is not physical PMem evidence. */
public final class PmemNativeOpenContractTest {
    private static int checks;

    private PmemNativeOpenContractTest() {}

    @FunctionalInterface private interface Action { void run() throws Exception; }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void refused(Class<? extends Throwable> type, Action action,
                                String message) throws Exception {
        Throwable caught = null;
        try { action.run(); }
        catch (Exception failure) { caught = failure; }
        check(caught != null && type.isInstance(caught), message + ": " + caught);
    }

    private static void selected(PmemRegion region, PmemBackend requested,
                                 PmemCapabilities capabilities) {
        if (requested == PmemBackend.AUTO) {
            check(region.backend() == PmemBackend.POSIX || region.backend() == PmemBackend.PMDK,
                    "AUTO reports the backend actually selected");
            if (!capabilities.libpmem2Compiled())
                check(region.backend() == PmemBackend.POSIX, "AUTO uses available POSIX backend");
        } else {
            check(region.backend() == requested,
                    "explicit backend: expected " + requested + ", got " + region.backend());
        }
    }

    private static void exactRead(Path path, byte[] expected, PmemBackend backend,
                                  PmemCapabilities capabilities) throws Exception {
        for (PmemAccess access : PmemAccess.values()) {
            try (PmemRegion region = PmemSdk.open(path, 0, expected.length, access, backend)) {
                selected(region, backend, capabilities);
                byte[] actual = new byte[expected.length];
                region.read(0, actual, 0, actual.length);
                check(Arrays.equals(actual, expected), "filesystem traversal bytes for " + path);
                check(region.access() == access, "requested access retained");
            }
        }
    }

    private static void refuseFinalLink(Path link, int length, PmemBackend backend)
            throws Exception {
        for (PmemAccess access : PmemAccess.values()) {
            refused(IOException.class, () -> {
                try (PmemRegion region = PmemSdk.open(link, 0, length, access, backend)) {
                    check(region.isOpen(), "unexpected accepted symlink owner");
                }
            }, "final symlink must be refused: " + backend + "/" + access + "/" + link);
        }
    }

    private static void pathAdmission(Path root, byte[] bytes, PmemBackend backend,
                                       PmemCapabilities capabilities) throws Exception {
        Path actual = Files.createDirectory(root.resolve("actual"));
        Path child = Files.createDirectory(actual.resolve("child"));
        Path payload = Files.write(actual.resolve("payload.bin"), bytes);
        byte[] decoy = bytes.clone();
        decoy[0] ^= 0x5a;
        Files.write(root.resolve("payload.bin"), decoy);
        Files.write(child.resolve("child.bin"), bytes);
        Path parentLink = Files.createSymbolicLink(root.resolve("parent-link"), child);
        exactRead(parentLink.resolve("child.bin"), bytes, backend, capabilities);
        // Lexically normalizing this path would select the decoy, not the real payload.
        exactRead(parentLink.resolve("..").resolve("payload.bin"), bytes, backend, capabilities);
        Path finalLink = Files.createSymbolicLink(actual.resolve("final-link"),
                Path.of("payload.bin"));
        refuseFinalLink(finalLink, bytes.length, backend);
        refuseFinalLink(parentLink.resolve("..").resolve("final-link"), bytes.length, backend);
        Path chain = Files.createSymbolicLink(actual.resolve("link-chain"), finalLink);
        refuseFinalLink(chain, bytes.length, backend);
        Path dangling = Files.createSymbolicLink(actual.resolve("dangling"), Path.of("missing"));
        refuseFinalLink(dangling, bytes.length, backend);
        exactRead(Files.write(actual.resolve("payload-\u00e9-\u03bb.bin"), bytes),
                bytes, backend, capabilities);
        refused(IOException.class, () -> {
            try (PmemRegion region = PmemSdk.open(actual, 0, bytes.length,
                    PmemAccess.READ_ONLY, backend)) {
                check(region.isOpen(), "unexpected accepted directory owner");
            }
        }, "directories remain outside the regular-file contract");
        check(Arrays.equals(bytes, Files.readAllBytes(payload)),
                "refusal leaves target bytes unchanged");
        check(Arrays.equals(decoy, Files.readAllBytes(root.resolve("payload.bin"))),
                "parent traversal leaves the lexical decoy unchanged");
    }

    private static void accessModes(Path root, byte[] bytes, PmemBackend backend,
                                     PmemCapabilities capabilities) throws Exception {
        Path file = Files.write(root.resolve("access.bin"), bytes);
        exactRead(file, bytes, backend, capabilities);
        try (PmemRegion readOnly = PmemSdk.open(file, 0, bytes.length,
                PmemAccess.READ_ONLY, backend)) {
            selected(readOnly, backend, capabilities);
            refused(IllegalStateException.class, () -> readOnly.write(0, new byte[0], 0, 0),
                    "read-only zero-byte write refused");
            refused(IllegalStateException.class, () -> readOnly.persist(0, 0),
                    "read-only zero-byte persist refused");
            refused(IllegalStateException.class, () -> readOnly.write(0, new byte[] {77}, 0, 1),
                    "read-only write refused");
            refused(IllegalStateException.class, () -> readOnly.persist(0, 1),
                    "read-only persist refused");
        }
        check(Arrays.equals(bytes, Files.readAllBytes(file)),
                "read-only refusals leave bytes unchanged");
        byte[] expected = bytes.clone();
        expected[7] ^= 0x35;
        try (PmemRegion writable = PmemSdk.open(file, 0, bytes.length,
                PmemAccess.READ_WRITE, backend)) {
            selected(writable, backend, capabilities);
            writable.write(7, expected, 7, 1);
            writable.persist(7, 1);
        }
        exactRead(file, expected, backend, capabilities);
        check(Arrays.equals(expected, Files.readAllBytes(file)),
                "persisted write observed after close");
    }

    public static void main(String[] args) throws Exception {
        checks = 0;
        PmemBackend backend = args.length == 0 ? PmemBackend.FILE : PmemBackend.valueOf(args[0]);
        String phase = args.length < 2 ? "ALL" : args[1];
        if (!phase.equals("ALL") && !phase.equals("PATHS") && !phase.equals("ACCESS"))
            throw new IllegalArgumentException("phase must be ALL, PATHS or ACCESS");
        PmemCapabilities capabilities = PmemSdk.capabilities();
        if (backend != PmemBackend.FILE)
            check(capabilities.nativeAvailable(), "requested native library must be available");
        if (backend == PmemBackend.PMDK)
            check(capabilities.libpmem2Compiled(), "requested PMDK build must be present");
        int page = capabilities.osPageSize() > 0 ? capabilities.osPageSize() : 4096;
        byte[] bytes = new byte[Math.multiplyExact(page, 2)];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (17 * i + 3);
        Path root = Files.createTempDirectory("pmem-open-contract-");
        try {
            if (!phase.equals("ACCESS")) pathAdmission(root, bytes, backend, capabilities);
            if (!phase.equals("PATHS")) accessModes(root, bytes, backend, capabilities);
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                    Files.delete(path);
            }
        }
        System.out.println("PMEM_NATIVE_OPEN_PASS backend=" + backend + " phase=" + phase
                + " checks=" + checks + " libpmem2=" + capabilities.libpmem2Compiled()
                + " evidence=regular-file-contract-only");
    }
}
