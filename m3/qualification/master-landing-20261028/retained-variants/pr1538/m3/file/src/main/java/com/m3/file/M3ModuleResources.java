/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Generated from the source-sealed Synexia M3 runtime-convergence recipe.
 */
package com.m3.file;

import com.m3.collections.M3ArrayList;
import com.m3.collections.M3IndexedListSnapshot;
import com.m3.collections.M3IndexMaskCompactor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * One explicitly closed resource scope over the caller's original module reader.
 *
 * <p>The immutable directory is a {@code String[]} in reader encounter order,
 * aligned state bytes, and one sorted primitive name permutation. Resource bytes,
 * URIs, module identity and class definition remain with the original JDK owners.
 * No class is loaded or initialized and no resource-row object is retained.
 * The existing M3 indexed collection prepares the name permutation; the existing
 * Java/optional-JNI compactor selects physical rows without object references in JNI.
 *
 * <p>Metadata describes capture time. An externally mutable or exploded module
 * can subsequently change its payload; this class is not a filesystem snapshot.
 * Open a separate owner for a new revision. There is no global module cache.
 * The caller must close this owner, which also closes outstanding input streams.
 * The default {@link ModuleReader#read(String)} returns a detached heap buffer
 * through these tracked streams, so no reader-owned native buffer is retained.
 */
public final class M3ModuleResources implements ModuleReader {
    /** Every enumerated resource, including directory entries. */
    public static final int RESOURCE = 1;
    /** A name ending in {@code .class}; this is not bytecode validation. */
    public static final int CLASS = 2;
    /** Exactly {@code module-info.class}, also classified as RESOURCE and CLASS. */
    public static final int MODULE_DESCRIPTOR = 4;
    /** Default maximum captured names; the overloads may choose another budget. */
    public static final int DEFAULT_MAX_ENTRIES = 1_000_000;
    /** Default aggregate name budget measured in UTF-16 code units. */
    public static final long DEFAULT_MAX_NAME_CHARS = 64L * 1024 * 1024;

    private static final ManagedInputStream[] NO_STREAMS = new ManagedInputStream[0];

    private final ModuleReference reference;
    private final ModuleReader reader;
    private final String[] names;
    private final byte[] states;
    private final int[] nameOrder;
    // Active resource handles only, removed on close; not a per-directory-row plane.
    private ManagedInputStream[] streams = NO_STREAMS;
    private int streamCount;
    private boolean closed;

    private M3ModuleResources(ModuleReference reference, ModuleReader reader,
            String[] names, byte[] states, int[] nameOrder) {
        this.reference = reference;
        this.reader = reader;
        this.names = names;
        this.states = states;
        this.nameOrder = nameOrder;
    }

    /** Capture with the documented default entry and aggregate name budgets. */
    public static M3ModuleResources open(ModuleReference reference) throws IOException {
        return open(reference, DEFAULT_MAX_ENTRIES, DEFAULT_MAX_NAME_CHARS);
    }

    /** Capture with a caller entry budget and the default aggregate name budget. */
    public static M3ModuleResources open(ModuleReference reference, int maxEntries)
            throws IOException {
        return open(reference, maxEntries, DEFAULT_MAX_NAME_CHARS);
    }

    /**
     * Open one original reader and capture its complete resource-name directory.
     *
     * <p>Negative budgets refuse before reader I/O. Exhaustion, duplicate names
     * or malformed enumerated paths throw {@code IOException}, never a partial
     * directory. Lazy enumeration exceptions keep their original type. Every
     * capture failure closes the enumeration stream and original reader, with
     * close failures suppressed on the original failure.
     */
    public static M3ModuleResources open(ModuleReference reference, int maxEntries,
            long maxNameChars) throws IOException {
        Objects.requireNonNull(reference, "reference");
        if (maxEntries < 0 || maxNameChars < 0) {
            throw new IllegalArgumentException("negative module resource capture budget");
        }
        ModuleReader reader = Objects.requireNonNull(reference.open(), "module reader");
        try {
            String[] names = captureNames(reader, maxEntries, maxNameChars);
            int[] order = M3IndexedListSnapshot.copyOf(names).sorted().physicalIndices();
            for (int i = 1; i < order.length; i++) {
                if (names[order[i - 1]].equals(names[order[i]])) {
                    throw new IOException("duplicate module resource name at row " + order[i]);
                }
            }
            byte[] states = new byte[names.length];
            for (int i = 0; i < names.length; i++) states[i] = classifyName(names[i]);
            return new M3ModuleResources(reference, reader, names, states, order);
        } catch (Throwable failure) {
            closeAfterFailure(reader, failure);
            throw failure;
        }
    }

    private static String[] captureNames(ModuleReader reader, int maxEntries,
            long maxNameChars) throws IOException {
        M3ArrayList<String> captured = new M3ArrayList<>();
        long nameChars = 0;
        try (Stream<String> source = reader.list()) {
            Iterator<String> iterator = source.iterator();
            while (iterator.hasNext()) {
                if (captured.size() == maxEntries) {
                    throw new IOException("module resource entry budget exceeded");
                }
                String name = iterator.next();
                if (!validName(name)) {
                    throw new IOException("invalid module resource name at row " + captured.size());
                }
                if (name.length() > maxNameChars - nameChars) {
                    throw new IOException("module resource name-character budget exceeded");
                }
                nameChars += name.length();
                captured.add(name);
            }
        }
        return captured.toArray(String[]::new);
    }

    private static byte classifyName(String name) {
        int bits = RESOURCE;
        if (name.endsWith(".class")) bits |= CLASS;
        if (name.equals("module-info.class")) bits |= MODULE_DESCRIPTOR;
        return (byte) bits;
    }

    /** Original reference; no name-based or location-based re-resolution. */
    public ModuleReference reference() { return reference; }

    /** Exact original immutable descriptor object. */
    public ModuleDescriptor descriptor() { return reference.descriptor(); }

    /** Captured row count, usable after close. */
    public int size() { return names.length; }

    /** Original encounter-order name at a physical row, usable after close. */
    public String nameAt(int row) { return names[Objects.checkIndex(row, names.length)]; }

    /** Defensive snapshot in original reader encounter order. */
    public String[] names() { return names.clone(); }

    /**
     * Captured name membership, usable after close. A slash-terminated captured
     * directory also admits its alias without the slash, as ModuleReader permits.
     * Malformed paths are absent; null is rejected.
     */
    public boolean contains(String name) {
        Objects.requireNonNull(name, "name");
        return resourceRow(name) >= 0;
    }

    /**
     * Original captured name for an exact name or permitted directory alias.
     * An exact file wins when both {@code name} and {@code name + "/"} exist.
     * Uses the existing sorted index, performs no I/O, and remains usable after close.
     */
    public Optional<String> canonicalName(String name) {
        Objects.requireNonNull(name, "name");
        int row = resourceRow(name);
        return row < 0 ? Optional.empty() : Optional.of(names[row]);
    }

    /** Whether this explicit reader scope still admits I/O. */
    public synchronized boolean isOpen() { return !closed; }

    /**
     * Literal-prefix and state-mask selection in original encounter order.
     *
     * <p>Coordinates index {@link #nameAt(int)}. Masks fit an unsigned byte;
     * overlapping required/excluded bits select nothing. Empty prefix selects
     * directly from the physical state plane. Other prefixes bound the sorted
     * name window, compact its aligned states with the existing Java/JNI owner,
     * then restore physical order. No String or callback crosses JNI. A required
     * native backend never silently falls back, including empty windows.
     *
     * <p>For n names, w prefix candidates and k results, cost is
     * O(log n + w + k log k), plus UTF-16 comparisons, and O(w+k) temporary
     * primitive storage. The returned array is detached; no selection is cached.
     */
    public int[] select(String prefix, int requiredMask, int excludedMask,
            M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(backend, "backend");
        requireMask(requiredMask);
        requireMask(excludedMask);
        if (prefix.isEmpty()) {
            return M3IndexMaskCompactor.selectPositions(states, requiredMask, excludedMask, backend);
        }
        int first = lowerBound(prefix);
        int end = prefixEnd(prefix, first);
        byte[] windowStates = new byte[end - first];
        for (int i = 0; i < windowStates.length; i++) windowStates[i] = states[nameOrder[first + i]];
        int[] selected = M3IndexMaskCompactor.selectPositions(
                windowStates, requiredMask, excludedMask, backend);
        for (int i = 0; i < selected.length; i++) selected[i] = nameOrder[first + selected[i]];
        Arrays.sort(selected);
        return selected;
    }

    private static void requireMask(int mask) {
        if ((mask & ~0xff) != 0) throw new IllegalArgumentException("mask must fit an unsigned byte");
    }

    /** First name >= value in the sorted primitive directory. */
    private int lowerBound(String value) {
        int low = 0, high = nameOrder.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (names[nameOrder[mid]].compareTo(value) < 0) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** All names at/after first are >= prefix; prefix membership is contiguous. */
    private int prefixEnd(String prefix, int first) {
        int low = first, high = nameOrder.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (names[nameOrder[mid]].startsWith(prefix)) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    private int exactRow(String name) {
        int index = lowerBound(name);
        return index < nameOrder.length && names[nameOrder[index]].equals(name) ? nameOrder[index] : -1;
    }

    private int resourceRow(String name) {
        if (!validName(name)) return -1;
        int row = exactRow(name);
        if (row < 0 && !name.endsWith("/")) row = exactRow(name + "/");
        return row;
    }

    /** Abstract path validation; preserves literal Unicode and optional directory slash. */
    private static boolean validName(String name) {
        if (name == null || name.isEmpty() || name.charAt(0) == '/') return false;
        int start = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '\\' || c == 0) return false;
            if (c == '/') {
                if (i == start || dotSegment(name, start, i)) return false;
                start = i + 1;
            }
        }
        return start == name.length() || !dotSegment(name, start, name.length());
    }

    private static boolean dotSegment(String name, int start, int end) {
        int length = end - start;
        return (length == 1 || length == 2) && name.charAt(start) == '.'
                && (length == 1 || name.charAt(start + 1) == '.');
    }

    /** Delegates URI construction to the original reader for captured names only. */
    @Override public synchronized Optional<URI> find(String name) throws IOException {
        requireOpen();
        Objects.requireNonNull(name, "name");
        return resourceRow(name) < 0 ? Optional.empty() : reader.find(name);
    }

    /** Opens a tracked, caller-closeable stream from the original reader. */
    @Override public synchronized Optional<InputStream> open(String name) throws IOException {
        requireOpen();
        Objects.requireNonNull(name, "name");
        if (resourceRow(name) < 0) return Optional.empty();
        Optional<InputStream> opened = reader.open(name);
        if (opened.isEmpty()) {
            requireOpen();
            return Optional.empty();
        }
        InputStream input = opened.orElseThrow();
        try {
            // A provider can synchronously re-enter close() while opening its raw handle.
            // Keep this check inside the cleanup region so that handle cannot escape or leak.
            requireOpen();
            ManagedInputStream managed = new ManagedInputStream(input);
            Optional<InputStream> result = Optional.of(managed);
            if (streamCount == streams.length) {
                int length = streams.length == 0 ? 4 : Math.addExact(streams.length,
                        Math.max(1, streams.length >>> 1));
                streams = Arrays.copyOf(streams, length);
            }
            streams[streamCount++] = managed;
            return result;
        } catch (Throwable failure) {
            closeAfterFailure(input, failure);
            throw failure;
        }
    }

    /** Convenience open that reports an absent name with FileNotFoundException. */
    public InputStream openStream(String name) throws IOException {
        return open(name).orElseThrow(() -> new FileNotFoundException(name));
    }

    /**
     * Returns immutable captured metadata after checking this scope is open.
     * The returned stream holds no original-reader handle and may be closed
     * independently; already obtained metadata remains valid after owner close.
     */
    @Override public synchronized Stream<String> list() throws IOException {
        requireOpen();
        return Arrays.stream(names);
    }

    private void requireOpen() throws ClosedChannelException {
        if (closed) throw new ClosedChannelException();
    }

    /** Invalidates I/O, closes every outstanding stream, then the original reader. */
    @Override public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        Throwable failure = null;
        for (int i = 0; i < streamCount; i++) {
            ManagedInputStream stream = streams[i];
            streams[i] = null;
            stream.streamClosed = true;
            try { stream.input.close(); }
            catch (Throwable closeFailure) { failure = accumulate(failure, closeFailure); }
        }
        streamCount = 0;
        streams = NO_STREAMS;
        try { reader.close(); }
        catch (Throwable closeFailure) { failure = accumulate(failure, closeFailure); }
        if (failure instanceof IOException io) throw io;
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
        if (failure != null) throw new IOException(failure);
    }

    private static Throwable accumulate(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }

    private static void closeAfterFailure(java.io.Closeable resource, Throwable failure) {
        try { resource.close(); }
        catch (Throwable closeFailure) { accumulate(failure, closeFailure); }
    }

    private void forget(ManagedInputStream stream) {
        for (int i = 0; i < streamCount; i++) {
            if (streams[i] == stream) {
                System.arraycopy(streams, i + 1, streams, i, streamCount - i - 1);
                streams[--streamCount] = null;
                return;
            }
        }
    }

    /** Active handle projection only; no instance is created by directory enumeration. */
    private final class ManagedInputStream extends InputStream {
        private final InputStream input;
        private boolean streamClosed;

        private ManagedInputStream(InputStream input) { this.input = input; }

        private void requireStreamOpen() throws ClosedChannelException {
            requireOpen();
            if (streamClosed) throw new ClosedChannelException();
        }

        @Override public int read() throws IOException {
            synchronized (M3ModuleResources.this) { requireStreamOpen(); return input.read(); }
        }
        @Override public int read(byte[] bytes, int offset, int length) throws IOException {
            synchronized (M3ModuleResources.this) {
                requireStreamOpen();
                return input.read(bytes, offset, length);
            }
        }
        @Override public long skip(long count) throws IOException {
            synchronized (M3ModuleResources.this) { requireStreamOpen(); return input.skip(count); }
        }
        @Override public int available() throws IOException {
            synchronized (M3ModuleResources.this) { requireStreamOpen(); return input.available(); }
        }
        @Override public void mark(int readLimit) {
            synchronized (M3ModuleResources.this) {
                if (!closed && !streamClosed) input.mark(readLimit);
            }
        }
        @Override public boolean markSupported() {
            synchronized (M3ModuleResources.this) {
                return !closed && !streamClosed && input.markSupported();
            }
        }
        @Override public void reset() throws IOException {
            synchronized (M3ModuleResources.this) { requireStreamOpen(); input.reset(); }
        }
        @Override public void close() throws IOException {
            synchronized (M3ModuleResources.this) {
                if (streamClosed) return;
                streamClosed = true;
                forget(this);
                input.close();
            }
        }
    }
}
