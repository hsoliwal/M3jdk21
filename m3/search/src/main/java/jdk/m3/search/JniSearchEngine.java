// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/JniSearchEngine.java (blob e859500e294c) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import jdk.m3.collections.DefaultProgressMonitor;
import jdk.m3.collections.ProgressMonitor;
import jdk.m3.file.M3JniLibraryLoader;
import jdk.m3.file.M3NativeLibrarySpec;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** Optional JNI provider with shared loader ownership and a Java correctness fallback. */
public final class JniSearchEngine implements SearchEngine {
    private static final String LIBRARY_PROPERTY = "m3.native.library";
    // Native methods are static: their binary identity belongs to this JVM class.
    private static final Object NATIVE_BIND_LOCK = new Object();
    private static M3NativeLibrarySpec boundLibrary;
    private static boolean boundCompatible;

    @Override
    public String name() { return "JNI"; }

    @Override
    public int priority() { return 100; }

    @Override
    public boolean available() {
        if (!Boolean.getBoolean("m3.native.enabled")) return false;
        M3NativeLibrarySpec library =
                M3NativeLibrarySpec.fromProperties(LIBRARY_PROPERTY, "m3search");
        synchronized (NATIVE_BIND_LOCK) {
            if (boundLibrary != null) {
                return boundCompatible && boundLibrary.cacheKey().equals(library.cacheKey());
            }
            try {
                M3JniLibraryLoader.load(library);
            } catch (LinkageError | SecurityException unavailable) {
                // Failed loads establish no JNI provider identity and may be retried.
                return false;
            }
            // A successful load may bind static JNI methods even when ABI validation
            // fails. Seal that binary identity for every instance of this class.
            // Successful System.load can bind static JNI methods even when
            // an unchecked exception escapes ABI verification. Pin first.
            boundLibrary = library;
            boolean compatible;
            try {
                compatible = nativeAbi() == 1;
            } catch (LinkageError | SecurityException incompatible) {
                compatible = false;
            }
            boundCompatible = compatible;
            return compatible;
        }
    }

    @Override
    public CompiledSearch compile(byte[] needle) { return compile(needle, null); }

    @Override
    public CompiledSearch compile(byte[] needle, ProgressMonitor monitor) {
        ProgressMonitor checked = monitor == null ? new DefaultProgressMonitor() : monitor;
        checked.checkCanceled();
        if (!available()) throw new IllegalStateException("JNI search provider is unavailable");
        if (needle == null || needle.length == 0) {
            throw new IllegalArgumentException("needle must not be empty");
        }
        byte[] copy = Arrays.copyOf(needle, needle.length);
        checked.checkCanceled();
        long handle = compileNative(copy);
        if (handle == 0L) throw new IllegalStateException("native pattern compilation returned null handle");
        try {
            checked.checkCanceled();
            return new NativePattern(handle, copy.length);
        } catch (RuntimeException | Error failure) {
            try {
                releaseNative(handle);
            } catch (RuntimeException | Error releaseFailure) {
                if (releaseFailure != failure) failure.addSuppressed(releaseFailure);
            }
            throw failure;
        }
    }

    private static native int nativeAbi();
    private static native long compileNative(byte[] needle);
    private static native int indexOfNative(long handle, byte[] haystack, int fromIndex);
    /** Optional ABI-1 extension; an older library falls back to the interface implementation. */
    private static native int lastIndexOfNative(long handle, byte[] haystack, int fromIndex);
    /** Optional zero-copy ABI-1 extension for direct/mapped buffers. */
    private static native int indexOfDirectNative(
            long handle, ByteBuffer haystack, int offset, int length, int fromIndex);
    /** Optional zero-copy ABI-1 extension for reverse direct/mapped-buffer lookup. */
    private static native int lastIndexOfDirectNative(
            long handle, ByteBuffer haystack, int offset, int length, int fromIndex);
    /** Optional ABI-1 extension; an older library falls back to repeated indexOf calls. */
    private static native int countOverlappingNative(long handle, byte[] haystack, int fromIndex);
    /** Optional ABI-1 extension; bounded positions are copied once across JNI. */
    private static native int[] positionsOverlappingNative(
            long handle, byte[] haystack, int fromIndex, int maxMatches);
    /** Callback-free probe: missing optional symbols are detected before a monitor can run. */
    private static native int monitoredBulkNativeAbi();
    /** Optional ABI-1 extension with cooperative checkpoints in prefix and scan work. */
    private static native int countOverlappingMonitoredNative(
            long handle, byte[] haystack, int fromIndex, Object callback);
    /** Optional ABI-1 extension; callback failures propagate without invoking a fallback. */
    private static native int[] positionsOverlappingMonitoredNative(
            long handle, byte[] haystack, int fromIndex, int maxMatches, Object callback);
    private static native void releaseNative(long handle);

    static int countIfNative(CompiledSearch compiled, byte[] haystack, int fromIndex,
            ProgressMonitor monitor) {
        if (!(compiled instanceof NativePattern pattern)) return -1;
        return pattern.countOverlapping(haystack, fromIndex, monitor);
    }

    /** Null means the provider or additive native symbol is unavailable. */
    static int[] positionsIfNative(CompiledSearch compiled, byte[] haystack, int fromIndex,
            int maxMatches, ProgressMonitor monitor) {
        if (!(compiled instanceof NativePattern pattern)) return null;
        return pattern.positionsOverlapping(haystack, fromIndex, maxMatches, monitor);
    }

    private static final class NativePattern implements CompiledSearch {
        private final int needleLength;
        private long handle;
        private long borrowedHandle;
        private int monitoredBulkAvailability;

        private NativePattern(long handle, int needleLength) {
            this.handle = handle;
            this.needleLength = needleLength;
        }

        @Override
        public synchronized int indexOf(byte[] haystack, int fromIndex) {
            if (handle == 0L) throw new IllegalStateException("native search pattern is closed");
            if (haystack == null) throw new NullPointerException("haystack");
            return indexOfNative(handle, haystack, Math.max(0, fromIndex));
        }

        @Override
        public synchronized int indexOf(byte[] haystack, int fromIndex, ProgressMonitor monitor) {
            ProgressMonitor checked = monitor == null ? new DefaultProgressMonitor() : monitor;
            checked.checkCanceled();
            int result = indexOf(haystack, fromIndex);
            checked.checkCanceled();
            return result;
        }

        @Override
        public synchronized int lastIndexOf(byte[] haystack, int fromIndex) {
            if (handle == 0L) throw new IllegalStateException("native search pattern is closed");
            if (haystack == null) throw new NullPointerException("haystack");
            try {
                return lastIndexOfNative(handle, haystack, fromIndex);
            } catch (UnsatisfiedLinkError oldLibrary) {
                return CompiledSearch.super.lastIndexOf(haystack, fromIndex);
            }
        }

        @Override
        public synchronized int indexOf(ByteBuffer haystack, int fromIndex) {
            if (handle == 0L) throw new IllegalStateException("native search pattern is closed");
            if (haystack == null) throw new NullPointerException("haystack");
            if (!haystack.isDirect()) {
                return CompiledSearch.super.indexOf(haystack, fromIndex);
            }
            ByteBuffer view = haystack.duplicate();
            try {
                int result = indexOfDirectNative(
                        handle, view, view.position(), view.remaining(), fromIndex);
                return result == Integer.MIN_VALUE
                        ? CompiledSearch.super.indexOf(haystack, fromIndex)
                        : result;
            } catch (UnsatisfiedLinkError oldLibrary) {
                return CompiledSearch.super.indexOf(haystack, fromIndex);
            }
        }

        @Override
        public synchronized int lastIndexOf(ByteBuffer haystack, int fromIndex) {
            if (handle == 0L) throw new IllegalStateException("native search pattern is closed");
            if (haystack == null) throw new NullPointerException("haystack");
            if (!haystack.isDirect()) {
                return CompiledSearch.super.lastIndexOf(haystack, fromIndex);
            }
            ByteBuffer view = haystack.duplicate();
            try {
                int result = lastIndexOfDirectNative(
                        handle, view, view.position(), view.remaining(), fromIndex);
                return result == Integer.MIN_VALUE
                        ? CompiledSearch.super.lastIndexOf(haystack, fromIndex)
                        : result;
            } catch (UnsatisfiedLinkError oldLibrary) {
                return CompiledSearch.super.lastIndexOf(haystack, fromIndex);
            }
        }

        @Override
        public int needleLength() { return needleLength; }

        private synchronized int countOverlapping(byte[] haystack, int fromIndex,
                ProgressMonitor monitor) {
            requireBulkEntry();
            monitor.checkCanceled();
            requireOpen();
            // Older ABI-1 libraries can have bulk symbols but no monitored entrypoint.
            // Never run their unmonitored bulk scan for a caller-supplied monitor.
            boolean monitored = monitor != new DefaultProgressMonitor();
            if (monitored && !supportsMonitoredBulk()) return -1;
            int count;
            if (monitored) {
                long borrowed = borrow();
                try {
                    count = countOverlappingMonitoredNative(
                            borrowed, haystack, Math.max(0, fromIndex), new NativePoll(monitor));
                } finally {
                    endBorrow();
                }
            } else {
                try {
                    count = countOverlappingNative(handle, haystack, Math.max(0, fromIndex));
                } catch (UnsatisfiedLinkError oldLibrary) {
                    return -1;
                }
            }
            monitor.checkCanceled();
            return count;
        }

        private synchronized int[] positionsOverlapping(byte[] haystack, int fromIndex,
                int maxMatches, ProgressMonitor monitor) {
            requireBulkEntry();
            monitor.checkCanceled();
            requireOpen();
            // Preserve the Java fallback when the optional monitored JNI ABI is absent.
            boolean monitored = monitor != new DefaultProgressMonitor();
            if (monitored && !supportsMonitoredBulk()) return null;
            int[] positions;
            if (monitored) {
                long borrowed = borrow();
                try {
                    positions = positionsOverlappingMonitoredNative(
                            borrowed, haystack, Math.max(0, fromIndex), maxMatches,
                            new NativePoll(monitor));
                } finally {
                    endBorrow();
                }
            } else {
                try {
                    positions = positionsOverlappingNative(
                            handle, haystack, Math.max(0, fromIndex), maxMatches);
                } catch (UnsatisfiedLinkError oldLibrary) {
                    return null;
                }
            }
            monitor.checkCanceled();
            if (positions == null || positions.length > maxMatches) {
                throw new IllegalStateException("native positions returned an invalid batch");
            }
            int previous = Math.max(0, fromIndex) - 1;
            int lastStart = haystack.length - needleLength;
            for (int position : positions) {
                if (position <= previous || position > lastStart) {
                    throw new IllegalStateException("native positions returned an invalid byte offset");
                }
                previous = position;
            }
            return positions;
        }

        private void requireOpen() {
            if (handle == 0L) throw new IllegalStateException("native search pattern is closed");
        }

        private void requireBulkEntry() {
            requireOpen();
            if (borrowedHandle != 0L) {
                throw new IllegalStateException("native bulk search is already active");
            }
        }

        private boolean supportsMonitoredBulk() {
            if (monitoredBulkAvailability == 0) {
                try {
                    monitoredBulkAvailability = monitoredBulkNativeAbi() == 1 ? 1 : -1;
                } catch (UnsatisfiedLinkError oldLibrary) {
                    monitoredBulkAvailability = -1;
                }
            }
            return monitoredBulkAvailability > 0;
        }

        private long borrow() {
            requireBulkEntry();
            borrowedHandle = handle;
            return borrowedHandle;
        }

        private void endBorrow() {
            long borrowed = borrowedHandle;
            borrowedHandle = 0L;
            if (handle == 0L) releaseNative(borrowed);
        }

        /** Called only by a monitored JNI operation while its native handle is borrowed. */
        private final class NativePoll {
            private final ProgressMonitor monitor;

            private NativePoll(ProgressMonitor monitor) {
                this.monitor = monitor;
            }

            public void checkCanceled() {
                requireOpen();
                monitor.checkCanceled();
                requireOpen();
            }
        }

        @Override
        public String strategy() { return "JNI_PRECOMPUTED_BMH"; }

        @Override
        public synchronized void close() {
            if (handle != 0L) {
                long closing = handle;
                handle = 0L;
                if (borrowedHandle == 0L) releaseNative(closing);
            }
        }
    }
}
