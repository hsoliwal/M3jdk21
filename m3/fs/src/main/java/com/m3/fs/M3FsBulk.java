/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.util.Arrays;
import java.util.Objects;

import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystem;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.Selectors;

/**
 * Ordered, columnar metadata/namespace requests for an M3FS filesystem provider.
 * The default execution is deliberately sequential and nontransactional; an
 * atomic-aware provider may override the filesystem's execution template.
 * Neither successful operation nor a failed/unknown outcome implies durable
 * publication on every backend.
 */
public final class M3FsBulk {
    public static final byte STAT = 1;
    public static final byte MKDIR = 2;
    public static final byte DELETE = 3;
    public static final byte DELETE_TREE = 4;
    public static final byte COPY = 5;
    public static final byte MOVE = 6;

    public static final byte OK = 1;
    public static final byte NO_EFFECT = 2;
    public static final byte FAILED = 3;
    public static final byte SKIPPED = 4;

    /** Selection flags address status values by {@code 1 << status}, not opcode. */
    public static final int STATUS_FLAG_OK = 1 << OK;
    public static final int STATUS_FLAG_NO_EFFECT = 1 << NO_EFFECT;
    public static final int STATUS_FLAG_FAILED = 1 << FAILED;
    public static final int STATUS_FLAG_SKIPPED = 1 << SKIPPED;
    public static final int STATUS_FLAGS_ALL = STATUS_FLAG_OK | STATUS_FLAG_NO_EFFECT
            | STATUS_FLAG_FAILED | STATUS_FLAG_SKIPPED;

    public static final int META_CHECKED = 1;
    public static final int META_EXISTS = 2;
    public static final int META_FILE = 4;
    public static final int META_DIRECTORY = 8;

    private M3FsBulk() { }

    public static Builder builder(int initialCapacity) {
        return new Builder(initialCapacity);
    }

    /** An immutable slice-free request packet backed by parallel primitive/reference arrays. */
    public static final class Batch {
        private final byte[] operations;
        private final String[] sources;
        private final String[] targets;
        private final boolean failFast;
        private final boolean atomic;
        private final M3FsProgress progress;

        private Batch(Builder builder) {
            operations = Arrays.copyOf(builder.operations, builder.size);
            sources = Arrays.copyOf(builder.sources, builder.size);
            targets = Arrays.copyOf(builder.targets, builder.size);
            failFast = builder.failFast;
            atomic = builder.atomic;
            progress = builder.progress;
        }

        public int size() { return operations.length; }
        public byte operation(int index) { return operations[Objects.checkIndex(index, size())]; }
        public String source(int index) { return sources[Objects.checkIndex(index, size())]; }
        public String target(int index) { return targets[Objects.checkIndex(index, size())]; }
        public boolean failFast() { return failFast; }
        public boolean atomic() { return atomic; }
        public M3FsProgress progress() { return progress; }
    }

    /** Mutable construction only; {@link #build()} freezes independent arrays. */
    public static final class Builder {
        private byte[] operations;
        private String[] sources;
        private String[] targets;
        private int size;
        private boolean failFast;
        private boolean atomic;
        private M3FsProgress progress = M3FsProgress.noop();

        private Builder(int capacity) {
            if (capacity < 0) throw new IllegalArgumentException("negative capacity");
            operations = new byte[capacity];
            sources = new String[capacity];
            targets = new String[capacity];
        }

        public Builder failFast(boolean enabled) { failFast = enabled; return this; }
        public Builder atomic(boolean enabled) { atomic = enabled; return this; }
        public Builder progress(M3FsProgress monitor) {
            progress = Objects.requireNonNull(monitor, "monitor");
            return this;
        }
        public Builder stat(String path) { return add(STAT, path, null); }
        public Builder mkdir(String path) { return add(MKDIR, path, null); }
        public Builder delete(String path) { return add(DELETE, path, null); }
        public Builder deleteTree(String path) { return add(DELETE_TREE, path, null); }
        public Builder copy(String from, String to) { return add(COPY, from, to); }
        public Builder move(String from, String to) { return add(MOVE, from, to); }

        private Builder add(byte operation, String source, String target) {
            Objects.requireNonNull(source, "source");
            if (source.isEmpty()) throw new IllegalArgumentException("empty source path");
            if (operation == COPY || operation == MOVE) {
                Objects.requireNonNull(target, "target");
                if (target.isEmpty()) throw new IllegalArgumentException("empty target path");
            }
            if (size == operations.length) {
                int capacity = size == 0 ? 4 : Math.addExact(size, Math.max(1, size >>> 1));
                operations = Arrays.copyOf(operations, capacity);
                sources = Arrays.copyOf(sources, capacity);
                targets = Arrays.copyOf(targets, capacity);
            }
            operations[size] = operation;
            sources[size] = source;
            targets[size] = target;
            size++;
            return this;
        }
        public Batch build() { return new Batch(this); }
    }

    /** Status columns; only failures allocate exception objects from a provider. */
    public static final class Result {
        private final byte[] statuses;
        private final int[] metadata;
        private final FileSystemException[] errors;

        public Result(byte[] statuses, int[] metadata, FileSystemException[] errors) {
            Objects.requireNonNull(statuses, "statuses");
            Objects.requireNonNull(metadata, "metadata");
            Objects.requireNonNull(errors, "errors");
            if (statuses.length != metadata.length || statuses.length != errors.length) {
                throw new IllegalArgumentException("batch columns differ in length");
            }
            this.statuses = statuses.clone();
            this.metadata = metadata.clone();
            this.errors = errors.clone();
        }
        public int size() { return statuses.length; }
        public byte status(int index) { return statuses[Objects.checkIndex(index, size())]; }
        public int metadataMask(int index) { return metadata[Objects.checkIndex(index, size())]; }
        public FileSystemException error(int index) { return errors[Objects.checkIndex(index, size())]; }
        public long successfulCount() {
            long n = 0;
            for (byte status : statuses) if (status == OK) n++;
            return n;
        }

        /**
         * Count rows satisfying a status flag-set and metadata-required/excluded masks.
         * Status flags use {@code 1 << OK}, {@code 1 << FAILED}, etc.
         * The original detached result columns remain authoritative.
         */
        public int selectedCount(int allowedStatuses, int requiredMetadata, int excludedMetadata) {
            return selectedCount(0, size(), allowedStatuses, requiredMetadata, excludedMetadata);
        }

        /** Count in a half-open physical row window; no selection array is allocated. */
        public int selectedCount(int first, int length, int allowedStatuses,
                                 int requiredMetadata, int excludedMetadata) {
            validateSelection(first, length, allowedStatuses);
            if ((requiredMetadata & excludedMetadata) != 0 || allowedStatuses == 0) return 0;
            int count = 0;
            for (int i = first, end = first + length; i < end; i++) {
                if (matches(i, allowedStatuses, requiredMetadata, excludedMetadata)) count++;
            }
            return count;
        }

        /**
         * Detached selected physical ordinals in increasing original encounter order.
         * Two passes allocate exactly one int[k], with no retained per-row Entry/Node.
         * Unknown provider status bytes do not match any of the four known flags.
         */
        public int[] selectedOrdinals(int allowedStatuses, int requiredMetadata,
                                      int excludedMetadata) {
            return selectedOrdinals(0, size(), allowedStatuses, requiredMetadata, excludedMetadata);
        }

        /** Physical ordinals stay absolute even when scanning a sub-window. */
        public int[] selectedOrdinals(int first, int length, int allowedStatuses,
                                      int requiredMetadata, int excludedMetadata) {
            int count = selectedCount(first, length, allowedStatuses,
                    requiredMetadata, excludedMetadata);
            int[] ordinals = new int[count];
            int written = 0;
            for (int i = first, end = first + length; i < end; i++) {
                if (matches(i, allowedStatuses, requiredMetadata, excludedMetadata)) {
                    ordinals[written++] = i;
                }
            }
            return ordinals;
        }

        /**
         * Return one packed bit per original row, including skipped and error rows.
         * The result is sized for the whole parent batch. Unselected rows outside
         * the supplied window stay zero, so it can be used by an indexed bulk caller.
         * This is a detached snapshot, not a live filtered/sorted collection view.
         */
        public long[] selectedMaskWords(int allowedStatuses, int requiredMetadata,
                                        int excludedMetadata) {
            return selectedMaskWords(0, size(), allowedStatuses, requiredMetadata, excludedMetadata);
        }

        /** Windowed variant with absolute physical bit positions. */
        public long[] selectedMaskWords(int first, int length, int allowedStatuses,
                                        int requiredMetadata, int excludedMetadata) {
            validateSelection(first, length, allowedStatuses);
            long[] words = new long[(size() >>> 6) + ((size() & 63) == 0 ? 0 : 1)];
            if ((requiredMetadata & excludedMetadata) != 0 || allowedStatuses == 0) return words;
            for (int i = first, end = first + length; i < end; i++) {
                if (matches(i, allowedStatuses, requiredMetadata, excludedMetadata)) {
                    words[i >>> 6] |= 1L << (i & 63);
                }
            }
            return words;
        }

        private void validateSelection(int first, int length, int allowedStatuses) {
            Objects.checkFromIndexSize(first, length, size());
            if ((allowedStatuses & ~STATUS_FLAGS_ALL) != 0) {
                throw new IllegalArgumentException("unknown M3FS bulk status flag");
            }
        }

        private boolean matches(int index, int allowedStatuses, int requiredMetadata,
                                int excludedMetadata) {
            int state = statuses[index];
            if (state < OK || state > SKIPPED || (allowedStatuses & (1 << state)) == 0) {
                return false;
            }
            int metadataBits = metadata[index];
            return (metadataBits & requiredMetadata) == requiredMetadata
                    && (metadataBits & excludedMetadata) == 0;
        }
    }

    /**
     * Safe fallback for any existing VFS provider: ordered and best-effort.
     * Atomic batches are refused before the first provider operation. A DBFS or
     * other transactional backend must implement its own atomic execution hook.
     * Individual checked failures are retained rather than reported as success.
     */
    public static Result fallback(FileSystem fileSystem, Batch batch) {
        Objects.requireNonNull(fileSystem, "fileSystem");
        Objects.requireNonNull(batch, "batch");
        if (batch.atomic()) {
            throw new UnsupportedOperationException("atomic bulk operation requires a provider implementation");
        }
        int length = batch.size();
        byte[] statuses = new byte[length];
        Arrays.fill(statuses, SKIPPED);
        int[] metadata = new int[length];
        FileSystemException[] errors = new FileSystemException[length];
        for (int i = 0; i < length; i++) {
            batch.progress().checkCanceled();
            try {
                statuses[i] = perform(fileSystem, batch, i, metadata);
            } catch (FileSystemException failure) {
                errors[i] = failure;
                statuses[i] = FAILED;
                if (batch.failFast()) {
                    batch.progress().worked(1);
                    break;
                }
            }
            batch.progress().worked(1);
        }
        return new Result(statuses, metadata, errors);
    }

    private static byte perform(FileSystem fileSystem, Batch batch, int index, int[] metadata)
            throws FileSystemException {
        FileObject source = fileSystem.resolveFile(batch.source(index));
        return switch (batch.operation(index)) {
            case STAT -> {
                int bits = META_CHECKED;
                if (source.exists()) {
                    bits |= META_EXISTS;
                    if (source.isFile()) bits |= META_FILE;
                    if (source.isFolder()) bits |= META_DIRECTORY;
                }
                metadata[index] = bits;
                yield OK;
            }
            case MKDIR -> {
                source.createFolder();
                yield OK;
            }
            case DELETE -> source.delete() ? OK : NO_EFFECT;
            case DELETE_TREE -> source.deleteAll() > 0 ? OK : NO_EFFECT;
            case COPY -> {
                fileSystem.resolveFile(batch.target(index)).copyFrom(source, Selectors.SELECT_ALL);
                yield OK;
            }
            case MOVE -> {
                source.moveTo(fileSystem.resolveFile(batch.target(index)));
                yield OK;
            }
            default -> throw new IllegalArgumentException("unknown batch opcode");
        };
    }
}
