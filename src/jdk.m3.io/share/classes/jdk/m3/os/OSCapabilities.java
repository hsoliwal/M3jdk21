// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.os;

import java.util.Objects;

/**
 * Read-only native OS admission for optional M3 I/O backends.
 *
 * <p>A kernel version or OS name never makes a feature available. A feature is
 * AVAILABLE only following an ABI-matched, successful runtime native probe.
 * Policy denial, resource exhaustion and missing native libraries are separate
 * outcomes. This class does not replace NIO, enable an accelerated backend,
 * allocate persistent kernel rings or change any Java 21 contract.</p>
 */
public final class OSCapabilities {
    private OSCapabilities() { }
    private static final Status[] STATUS_LUT = Status.values();

    public enum Platform {
        LINUX, ANDROID, WINDOWS, MACOS, IOS, FREEBSD, NETBSD, OPENBSD,
        AIX, SOLARIS, ILLUMOS, OTHER
    }

    /** Append-only native feature IDs; never interpret the enum ordinal as an ABI. */
    public enum Feature {
        LINUX_IO_URING(0), LINUX_PIDFD(1), LINUX_OPENAT2(2), LINUX_STATX(3),
        WINDOWS_IO_RING(-1), WINDOWS_IOCP(-1), BSD_KQUEUE(-1), FREEBSD_INOTIFY(-1);
        private final int nativeId;
        Feature(int nativeId) { this.nativeId = nativeId; }
        int nativeId() { return nativeId; }

        boolean appliesTo(Platform platform) {
            return switch (this) {
                case LINUX_IO_URING, LINUX_PIDFD, LINUX_OPENAT2, LINUX_STATX ->
                        platform == Platform.LINUX || platform == Platform.ANDROID;
                case WINDOWS_IO_RING, WINDOWS_IOCP -> platform == Platform.WINDOWS;
                case BSD_KQUEUE -> platform == Platform.MACOS || platform == Platform.IOS
                        || platform == Platform.FREEBSD || platform == Platform.OPENBSD
                        || platform == Platform.NETBSD;
                case FREEBSD_INOTIFY -> platform == Platform.FREEBSD;
            };
        }
    }

    public enum Status {
        /** An actual native probe succeeded in the current process. */
        AVAILABLE,
        /** Native syscall/API was reported absent or unsupported. */
        UNSUPPORTED,
        /** Kernel, OS policy or sandbox denied the actual probe. */
        POLICY_DENIED,
        /** Transient resource condition; do not cache as permanently absent. */
        RESOURCE_LIMIT,
        /** Probe failed with a different OS error. */
        PROBE_ERROR,
        /** No qualified, ABI-matched native probe has been executed. */
        UNPROBED,
        /** The feature is for a different OS family. */
        NOT_APPLICABLE
    }

    /**
     * Immutable, array-backed process snapshot. Each lookup is O(1) without
     * a per-feature entry object. Do not persist it across process launches.
     */
    public static final class Snapshot {
        private final Platform platform;
        private final boolean nativeLoaded;
        private final int pageSizeBytes;
        private final byte[] statuses;
        private final int[] errors;

        private Snapshot(Platform platform, boolean nativeLoaded, int pageSizeBytes,
                         byte[] statuses, int[] errors) {
            this.platform = platform;
            this.nativeLoaded = nativeLoaded;
            this.pageSizeBytes = pageSizeBytes;
            this.statuses = statuses.clone();
            this.errors = errors.clone();
        }

        public Platform platform() { return platform; }
        public boolean nativeLoaded() { return nativeLoaded; }
        /** Zero means unknown; never assume 4096-byte pages. */
        public int pageSizeBytes() { return pageSizeBytes; }
        public Status status(Feature feature) {
            return STATUS_LUT[Byte.toUnsignedInt(statuses[Objects.requireNonNull(feature).ordinal()])];
        }
        public int osError(Feature feature) {
            return errors[Objects.requireNonNull(feature).ordinal()];
        }
        public boolean supports(Feature feature) { return status(feature) == Status.AVAILABLE; }
        /** No accelerated I/O path is enabled merely by detecting a syscall. */
        public String defaultIoBackend() { return "JDK_NIO"; }
    }

    /** Actual native driver; the package-private seam is only for deterministic tests. */
    interface KernelProbe {
        boolean ready();
        int pageSize();
        /** result = (positive errno << 8) | status; 0 means AVAILABLE. */
        int probe(int nativeFeatureId);
    }

    private static final class HostHolder {
        static final Snapshot CURRENT = inspect(safeProperty("os.name"),
                safeProperty("java.vm.name"), safeProperty("java.runtime.name"),
                new KernelProbe() {
                    public boolean ready() { return NativeProbe.AVAILABLE; }
                    public int pageSize() { return NativeProbe.pageSize(); }
                    public int probe(int id) { return NativeProbe.query(id); }
                });
    }

    public static Snapshot current() { return HostHolder.CURRENT; }

    /** Explicit fresh query after resource-policy changes; does not alter the cached snapshot. */
    public static Snapshot recheck() {
        return inspect(safeProperty("os.name"), safeProperty("java.vm.name"),
                safeProperty("java.runtime.name"), new KernelProbe() {
                    public boolean ready() { return NativeProbe.AVAILABLE; }
                    public int pageSize() { return NativeProbe.pageSize(); }
                    public int probe(int id) { return NativeProbe.query(id); }
                });
    }

    static Platform platformFor(String osName, String vmName, String runtimeName) {
        String os = osName == null ? "" : osName.toLowerCase(java.util.Locale.ROOT);
        String vm = vmName == null ? "" : vmName.toLowerCase(java.util.Locale.ROOT);
        String runtime = runtimeName == null ? "" : runtimeName.toLowerCase(java.util.Locale.ROOT);
        if (os.contains("android") || vm.contains("dalvik") || vm.contains("android")
                || runtime.contains("android"))
            return Platform.ANDROID;
        if (os.equals("ios") || os.contains("iphone") || os.contains("ipad"))
            return Platform.IOS;
        if (os.contains("linux")) return Platform.LINUX;
        if (os.contains("windows")) return Platform.WINDOWS;
        if (os.contains("mac") || os.contains("darwin")) return Platform.MACOS;
        if (os.contains("freebsd")) return Platform.FREEBSD;
        if (os.contains("netbsd")) return Platform.NETBSD;
        if (os.contains("openbsd")) return Platform.OPENBSD;
        if (os.contains("aix")) return Platform.AIX;
        if (os.contains("illumos")) return Platform.ILLUMOS;
        if (os.contains("sunos") || os.contains("solaris")) return Platform.SOLARIS;
        return Platform.OTHER;
    }

    private static String safeProperty(String key) {
        try { return System.getProperty(key, ""); }
        catch (SecurityException denied) { return ""; }
    }

    static Snapshot inspect(String osName, String vmName, String runtimeName, KernelProbe probe) {
        Objects.requireNonNull(probe, "probe");
        Platform platform = platformFor(osName, vmName, runtimeName);
        // Linux x86/ARM native admission is qualified separately. Android is
        // a Linux kernel but is NOT a Linux desktop ABI or unrestricted sandbox.
        boolean admitted = platform == Platform.LINUX && probe.ready();
        int pageSize = admitted ? probe.pageSize() : 0;
        if (pageSize < 512 || pageSize > 1024 * 1024 ||
                (pageSize & (pageSize - 1)) != 0) pageSize = 0;
        Feature[] features = Feature.values();
        byte[] status = new byte[features.length];
        int[] osErrors = new int[features.length];
        for (Feature feature : features) {
            Status state;
            int osError = 0;
            if (!feature.appliesTo(platform)) {
                state = Status.NOT_APPLICABLE;
            } else if (!admitted || feature.nativeId() < 0) {
                state = Status.UNPROBED;
            } else {
                int result;
                try { result = probe.probe(feature.nativeId()); }
                catch (UnsatisfiedLinkError | SecurityException failure) { result = -1; }
                int kind = result & 0xff;
                osError = result < 0 ? 0 : result >>> 8;
                state = switch (kind) {
                    case 0 -> osError == 0 ? Status.AVAILABLE : Status.PROBE_ERROR;
                    case 1 -> Status.UNSUPPORTED;
                    case 2 -> Status.POLICY_DENIED;
                    case 3 -> Status.RESOURCE_LIMIT;
                    case 4 -> Status.PROBE_ERROR;
                    default -> Status.PROBE_ERROR;
                };
                if (result < 0 || osError > 4095) {
                    state = Status.PROBE_ERROR;
                    osError = 0;
                }
            }
            status[feature.ordinal()] = (byte) state.ordinal();
            osErrors[feature.ordinal()] = osError;
        }
        return new Snapshot(platform, admitted, pageSize, status, osErrors);
    }
}
