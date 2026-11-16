// SPDX-FileCopyrightText: 2026 Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.os;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Java21 source-built host and injected platform/native evidence regression. */
public final class OSCapabilitiesTest {
    private static long checks;
    private OSCapabilitiesTest() { }
    private static void yes(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError("test=" + checks + " " + why);
    }
    private static void unsupported(OSCapabilities.Platform platform, String os, String vm) {
        OSCapabilities.KernelProbe failing = new OSCapabilities.KernelProbe() {
            public boolean ready() { return false; }
            public int pageSize() { throw new AssertionError("unloaded must not page probe"); }
            public int probe(int id) { throw new AssertionError("unloaded must not native probe"); }
        };
        var out = OSCapabilities.inspect(os, vm, "", failing);
        yes(out.platform() == platform, "OS family");
        yes(!out.nativeLoaded() && out.pageSizeBytes() == 0, "no claimed page size");
        yes(out.defaultIoBackend().equals("JDK_NIO"), "no backend switch");
        for (var f : OSCapabilities.Feature.values()) {
            yes(!out.supports(f), "no inferred availability");
            yes(out.osError(f) == 0, "no errno without an actual probe");
            yes(out.status(f) == (f.appliesTo(platform)
                    ? OSCapabilities.Status.UNPROBED : OSCapabilities.Status.NOT_APPLICABLE),
                    "unprobed or not applicable");
        }
    }

    public static void main(String[] args) throws Exception {
        unsupported(OSCapabilities.Platform.LINUX, "Linux", "OpenJDK 64-Bit Server VM");
        unsupported(OSCapabilities.Platform.ANDROID, "Linux", "Dalvik");
        unsupported(OSCapabilities.Platform.ANDROID, "Linux", "OpenJDK 64-Bit Server VM Android");
        unsupported(OSCapabilities.Platform.WINDOWS, "Windows 11", "OpenJDK");
        unsupported(OSCapabilities.Platform.MACOS, "Mac OS X", "OpenJDK");
        unsupported(OSCapabilities.Platform.IOS, "iOS", "OpenJDK");
        unsupported(OSCapabilities.Platform.FREEBSD, "FreeBSD", "OpenJDK");
        unsupported(OSCapabilities.Platform.NETBSD, "NetBSD", "OpenJDK");
        unsupported(OSCapabilities.Platform.OPENBSD, "OpenBSD", "OpenJDK");
        unsupported(OSCapabilities.Platform.AIX, "AIX", "OpenJDK");
        unsupported(OSCapabilities.Platform.SOLARIS, "SunOS", "OpenJDK");
        unsupported(OSCapabilities.Platform.ILLUMOS, "illumos", "OpenJDK");
        unsupported(OSCapabilities.Platform.OTHER, "Plan9", "OpenJDK");
        OSCapabilities.KernelProbe mixed = new OSCapabilities.KernelProbe() {
            public boolean ready() { return true; }
            public int pageSize() { return 16 * 1024; }
            public int probe(int id) {
                return switch (id) {
                    case 0 -> 2 | (1 << 8); // denied by seccomp or security policy
                    case 1 -> 0;             // probed successfully
                    case 2 -> 1 | (38 << 8); // ENOSYS
                    case 3 -> 3 | (12 << 8); // ENOMEM
                    default -> -1;
                };
            }
        };
        var out = OSCapabilities.inspect("Linux", "OpenJDK", "", mixed);
        yes(out.nativeLoaded(), "native ABI admission");
        yes(out.pageSizeBytes() == 16384, "no 4 KiB page assumption");
        yes(out.status(OSCapabilities.Feature.LINUX_IO_URING) == OSCapabilities.Status.POLICY_DENIED,
                "policy denied is not unsupported");
        yes(out.osError(OSCapabilities.Feature.LINUX_IO_URING) == 1, "policy errno");
        yes(out.supports(OSCapabilities.Feature.LINUX_PIDFD), "native success only");
        yes(out.status(OSCapabilities.Feature.LINUX_OPENAT2) == OSCapabilities.Status.UNSUPPORTED,
                "kernel absent");
        yes(out.status(OSCapabilities.Feature.LINUX_STATX) == OSCapabilities.Status.RESOURCE_LIMIT,
                "retryable errors");
        yes(!out.supports(OSCapabilities.Feature.WINDOWS_IO_RING), "no cross OS inference");
        var malformed = OSCapabilities.inspect("Linux", "OpenJDK", "", new OSCapabilities.KernelProbe() {
            public boolean ready() { return true; }
            public int pageSize() { return 6000; }
            public int probe(int id) { return id == 0 ? (4 << 8) : 99; }
        });
        yes(malformed.pageSizeBytes() == 0, "malformed page size rejected");
        for (var feature : new OSCapabilities.Feature[]{
                OSCapabilities.Feature.LINUX_IO_URING, OSCapabilities.Feature.LINUX_PIDFD}) {
            yes(malformed.status(feature) == OSCapabilities.Status.PROBE_ERROR,
                    "unexpected ABI values fail closed");
        }
        var host = OSCapabilities.current();
        yes(host == OSCapabilities.current(), "same process snapshot");
        yes(host.defaultIoBackend().equals("JDK_NIO"), "host fallback is standard JDK NIO");
        for (var feature : OSCapabilities.Feature.values()) {
            yes(host.status(feature) != null, "no missing status");
            yes(host.supports(feature) == (host.status(feature) == OSCapabilities.Status.AVAILABLE),
                    "only probed success is supported");
            yes(host.osError(feature) >= 0, "never negative errno");
        }
        // Multiple readers share immutable scalar arrays; no worker touches native code.
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);
        Thread[] workers = new Thread[8];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(() -> {
                try {
                    start.await();
                    for (int round = 0; round < 1000; round++) {
                        for (var feature : OSCapabilities.Feature.values()) {
                            if (host.status(feature) == OSCapabilities.Status.AVAILABLE &&
                                !host.supports(feature)) throw new AssertionError("race");
                        }
                    }
                } catch (Throwable caught) { error.compareAndSet(null, caught); }
            });
            workers[i].start();
        }
        start.countDown();
        for (var t : workers) t.join();
        yes(error.get() == null, "racing readers: " + error.get());
        // The native probes open only ephemeral descriptors. Check that repeated
        // admission does not retain io_uring/pidfd/openat2 handles in the JVM.
        if (NativeProbe.AVAILABLE && java.nio.file.Files.isDirectory(java.nio.file.Path.of("/proc/self/fd"))) {
            long before;
            try (var fds = java.nio.file.Files.list(java.nio.file.Path.of("/proc/self/fd"))) {
                before = fds.count();
            }
            for (int pass = 0; pass < 128; pass++) {
                for (int id = 0; id < 4; id++) {
                    int code = NativeProbe.query(id);
                    yes(code >= 0, "native probe result must encode its own status");
                }
            }
            long after;
            try (var fds = java.nio.file.Files.list(java.nio.file.Path.of("/proc/self/fd"))) {
                after = fds.count();
            }
            yes(after <= before + 1, "native probe fd leak: " + before + " to " + after);
        }
        System.out.println("M3_OS_CAPABILITIES_JAVA21_PASS checks=" + checks +
                " native=" + host.nativeLoaded() + " platform=" + host.platform() +
                " pageBytes=" + host.pageSizeBytes() + " io=" +
                host.status(OSCapabilities.Feature.LINUX_IO_URING) +
                " ioErrno=" + host.osError(OSCapabilities.Feature.LINUX_IO_URING));
    }
}
