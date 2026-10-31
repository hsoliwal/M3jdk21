package org.m3.runtime.lazy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Parent-owned, logical handles for transient state. All operations are serialized by
 * the parent monitor. Handles, rather than raw objects, must escape the parent.
 * This is an opt-in research adapter, not a replacement for JVM object identity.
 */
public final class LazyObjectParent implements AutoCloseable {
    public static final int LAZY = 1;
    public static final int RECREATE = 2;
    public static final int SWAP = 4;

    public interface Codec<T> {
        byte[] encode(T value) throws IOException;
        T decode(byte[] data) throws IOException;
    }

    private final Path directory;
    private final Map<String, Handle<?>> children = new HashMap<>();
    private long clock;
    private boolean closed;

    public LazyObjectParent(Path directory) throws IOException {
        this.directory = Objects.requireNonNull(directory);
        Files.createDirectories(directory);
    }

    public synchronized <T> Handle<T> child(String key, int flags, Supplier<? extends T> factory,
                                               Codec<T> codec) {
        checkOpen();
        Objects.requireNonNull(key);
        Objects.requireNonNull(factory);
        if ((flags & ~(LAZY | RECREATE | SWAP)) != 0) throw new IllegalArgumentException("unknown flags");
        if ((flags & SWAP) != 0 && codec == null) throw new IllegalArgumentException("SWAP requires codec");
        if ((flags & (RECREATE | SWAP)) == 0) throw new IllegalArgumentException("no eviction strategy");
        if (children.containsKey(key)) throw new IllegalArgumentException("duplicate key: " + key);
        Handle<T> h = new Handle<>(key, flags, factory, codec);
        children.put(key, h);
        if ((flags & LAZY) == 0) h.get();
        return h;
    }

    /** Evicts at most one unpinned resident handle, oldest first. */
    public synchronized boolean evictOne() throws IOException {
        checkOpen();
        Handle<?> oldest = null;
        for (Handle<?> h : children.values())
            if (h.resident && h.pins == 0 && (oldest == null || h.lastAccess < oldest.lastAccess))
                oldest = h;
        if (oldest == null) return false;
        oldest.evict();
        return true;
    }

    public synchronized int evictUpTo(int maximum) throws IOException {
        if (maximum < 0) throw new IllegalArgumentException("maximum");
        int count = 0;
        while (count < maximum && evictOne()) count++;
        return count;
    }

    public synchronized int residentCount() {
        int n = 0;
        for (Handle<?> h : children.values()) if (h.resident) n++;
        return n;
    }

    private void checkOpen() {
        if (closed) throw new IllegalStateException("parent closed");
    }

    private static void closeValue(Object value) throws IOException {
        if (value instanceof AutoCloseable c) {
            try { c.close(); }
            catch (Exception e) { throw new IOException("closing evicted resource", e); }
        }
    }

    @Override public synchronized void close() throws IOException {
        if (closed) return;
        for (Handle<?> h : children.values())
            if (h.pins != 0) throw new IllegalStateException("pinned child: " + h.key);
        IOException failure = null;
        for (Handle<?> h : children.values()) {
            try {
                if (h.resident) closeValue(h.value);
                if (h.swapFile != null) Files.deleteIfExists(h.swapFile);
                h.value = null;
                h.resident = false;
            } catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
        }
        closed = true;
        if (failure != null) throw failure;
    }

    public final class Handle<T> {
        private final String key;
        private final int flags;
        private final Supplier<? extends T> factory;
        private final Codec<T> codec;
        private T value;
        private boolean resident;
        private int pins;
        private long lastAccess;
        private Path swapFile;

        private Handle(String key, int flags, Supplier<? extends T> factory, Codec<T> codec) {
            this.key = key; this.flags = flags; this.factory = factory; this.codec = codec;
        }

        public T get() {
            synchronized (LazyObjectParent.this) {
                checkOpen();
                if (!resident) {
                    try {
                        if (swapFile != null) {
                            value = Objects.requireNonNull(codec.decode(Files.readAllBytes(swapFile)));
                            Files.delete(swapFile);
                            swapFile = null;
                        } else value = Objects.requireNonNull(factory.get());
                    } catch (IOException e) { throw new IllegalStateException("swap-in failed: " + key, e); }
                    resident = true;
                }
                lastAccess = ++clock;
                return value;
            }
        }

        /** Pin while externally using a value; release the lease to permit eviction. */
        public Lease pin() {
            synchronized (LazyObjectParent.this) {
                T v = get();
                pins++;
                return new Lease(v);
            }
        }

        private void evict() throws IOException {
            if (!resident || pins != 0) throw new IllegalStateException("not evictable");
            if ((flags & SWAP) != 0) {
                byte[] bytes = Objects.requireNonNull(codec.encode(value));
                Path tmp = Files.createTempFile(directory, "m3-lazy-", ".tmp");
                try {
                    Files.write(tmp, bytes);
                    Path target = Files.createTempFile(directory, "m3-lazy-", ".page");
                    try {
                        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                        // Commit the swap before releasing the resident value.
                        closeValue(value);
                        if (swapFile != null) Files.deleteIfExists(swapFile);
                        swapFile = target;
                    } catch (IOException | RuntimeException e) {
                        Files.deleteIfExists(target);
                        throw e;
                    }
                } finally { Files.deleteIfExists(tmp); }
            } else {
                closeValue(value);
            }
            value = null;
            resident = false;
        }

        public synchronized String key() { return key; }
        public int flags() { return flags; }

        public final class Lease implements AutoCloseable {
            private final T leased;
            private boolean released;
            private Lease(T leased) { this.leased = leased; }
            public T value() {
                synchronized (LazyObjectParent.this) {
                    if (released) throw new IllegalStateException("lease released");
                    return leased;
                }
            }
            @Override public void close() {
                synchronized (LazyObjectParent.this) {
                    if (!released) { pins--; released = true; }
                }
            }
        }
    }
}
