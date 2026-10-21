// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem;

import com.m3.fs.M3FS;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/** API stream facades; the native transaction checks the opened inode on every payload operation. */
final class M3Streams {
    private M3Streams() { }

    static final class Input extends InputStream {
        private final M3Mounts mounts;
        private final int slot;
        private final long generation;
        private final String path;
        private final long inode;
        private final byte[] one = new byte[1];
        private long position;
        private boolean closed;

        Input(M3Mounts mounts, int slot, long generation, String path, long inode) {
            this.mounts = mounts;
            this.slot = slot;
            this.generation = generation;
            this.path = path;
            this.inode = inode;
        }

        private M3FS owner() throws IOException {
            if (closed) throw new IOException("M3FS input stream is closed");
            return mounts.image(slot, generation);
        }

        @Override public synchronized int read() throws IOException {
            int count = read(one, 0, 1);
            return count < 0 ? -1 : one[0] & 255;
        }

        @Override public synchronized int read(byte[] bytes, int at, int length) throws IOException {
            Objects.checkFromIndexSize(at, length, bytes.length);
            synchronized (mounts) {
                M3FS image = owner();
                if (length == 0) return 0;
                int count = image.readIfInode(path, inode, position, bytes, at, length);
                if (count == 0) return -1;
                if (count < 0 || count > length) throw new IOException("Invalid M3FS read result");
                position = Math.addExact(position, count);
                return count;
            }
        }

        @Override public synchronized long skip(long length) throws IOException {
            synchronized (mounts) {
                M3FS image = owner();
                if (length <= 0) return 0;
                M3FS.Stat stat = image.stat(path);
                if (stat.inode() != inode) throw new IOException("M3FS stale inode: path was replaced");
                long skipped = Math.min(length, Math.max(0, stat.size() - position));
                position = Math.addExact(position, skipped);
                return skipped;
            }
        }

        @Override public synchronized int available() throws IOException {
            synchronized (mounts) {
                M3FS.Stat stat = owner().stat(path);
                if (stat.inode() != inode) throw new IOException("M3FS stale inode: path was replaced");
                return (int) Math.min(Integer.MAX_VALUE, Math.max(0, stat.size() - position));
            }
        }

        @Override public synchronized void close() { closed = true; }
    }

    static final class Output extends OutputStream {
        private final M3Mounts mounts;
        private final int slot;
        private final long generation;
        private final String path;
        private final long inode;
        private final boolean append;
        private final byte[] one = new byte[1];
        private long position;
        private boolean closed;

        Output(M3Mounts mounts, int slot, long generation, String path, long inode, boolean append) {
            this.mounts = mounts;
            this.slot = slot;
            this.generation = generation;
            this.path = path;
            this.inode = inode;
            this.append = append;
        }

        private M3FS owner() throws IOException {
            if (closed) throw new IOException("M3FS output stream is closed");
            return mounts.image(slot, generation);
        }

        @Override public synchronized void write(int value) throws IOException {
            one[0] = (byte) value;
            write(one, 0, 1);
        }

        @Override public synchronized void write(byte[] bytes, int at, int length) throws IOException {
            Objects.checkFromIndexSize(at, length, bytes.length);
            synchronized (mounts) {
                M3FS image = owner();
                if (length == 0) return;
                long next;
                try { next = Math.addExact(position, length); }
                catch (ArithmeticException overflow) { throw new IOException("M3FS stream offset overflow", overflow); }
                long size = image.writeIfInode(path, inode, append ? 0 : position, bytes, at, length,
                        append ? M3FS.WRITE_APPEND : 0);
                if (size < 0 || (!append && size < next)) throw new IOException("Invalid M3FS write size");
                position = append ? size : next;
            }
        }

        @Override public synchronized void flush() throws IOException {
            synchronized (mounts) { owner(); }
        }

        @Override public synchronized void close() { closed = true; }
    }
}
