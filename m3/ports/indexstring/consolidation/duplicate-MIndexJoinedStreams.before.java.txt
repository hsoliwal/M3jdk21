// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Objects;

/** Ordinary Java I/O cursors over the existing immutable segment payloads. */
final class MIndexJoinedStreams {
  private MIndexJoinedStreams() {}

  static InputStream bytes(ByteBuffer[] segments, int length) {
    return new ByteInput(new Cursor(segments, length));
  }

  static Reader chars(CharBuffer[] segments, int length) {
    return new CharInput(new Cursor(segments, length));
  }

  /** One reader-owned metadata cursor; closing it releases only its buffer attachments. */
  private static final class Cursor {
    private Buffer[] segments;
    private int index;
    private int remaining;
    private int markedIndex;
    private int markedPosition;
    private int markedRemaining;

    Cursor(Buffer[] segments, int length) {
      this.segments = segments;
      remaining = length;
      markedRemaining = length;
    }

    void requireOpen() throws IOException {
      if (segments == null) throw new IOException("joined cursor is closed");
    }

    Buffer current() {
      while (index < segments.length && !segments[index].hasRemaining()) index++;
      return index == segments.length ? null : segments[index];
    }

    long skip(long requested) {
      int count = (int) Math.min(Math.max(0L, requested), remaining);
      int pending = count;
      while (pending > 0) {
        Buffer part = current();
        int take = Math.min(pending, part.remaining());
        part.position(part.position() + take);
        pending -= take;
      }
      remaining -= count;
      return count;
    }

    void mark() {
      if (segments == null) return;
      Buffer part = current();
      markedIndex = index;
      markedPosition = part == null ? 0 : part.position();
      markedRemaining = remaining;
    }

    void reset() {
      for (int i = 0; i < segments.length; i++) {
        segments[i].position(i < markedIndex ? segments[i].limit()
            : i == markedIndex ? markedPosition : 0);
      }
      index = markedIndex;
      remaining = markedRemaining;
    }

    void close() { segments = null; remaining = 0; }
  }

  private static final class ByteInput extends InputStream {
    private final Cursor cursor;
    ByteInput(Cursor cursor) { this.cursor = cursor; }

    @Override public synchronized int read() throws IOException {
      cursor.requireOpen();
      ByteBuffer part = (ByteBuffer) cursor.current();
      if (part == null) return -1;
      cursor.remaining--;
      return part.get() & 255;
    }

    @Override public synchronized int read(byte[] target, int offset, int count) throws IOException {
      Objects.checkFromIndexSize(offset, count, Objects.requireNonNull(target, "target").length);
      cursor.requireOpen();
      if (count == 0) return 0;
      int wanted = Math.min(count, cursor.remaining);
      if (wanted == 0) return -1;
      int written = 0;
      while (written < wanted) {
        ByteBuffer part = (ByteBuffer) cursor.current();
        int take = Math.min(wanted - written, part.remaining());
        part.get(target, offset + written, take);
        written += take;
      }
      cursor.remaining -= written;
      return written;
    }

    @Override public synchronized long skip(long count) throws IOException {
      cursor.requireOpen(); return cursor.skip(count);
    }
    @Override public synchronized int available() throws IOException {
      cursor.requireOpen(); return cursor.remaining;
    }
    @Override public boolean markSupported() { return true; }
    /** Immutable backing supports an unlimited mark without allocating a replay buffer. */
    @Override public synchronized void mark(int readLimit) { cursor.mark(); }
    @Override public synchronized void reset() throws IOException {
      cursor.requireOpen(); cursor.reset();
    }
    @Override public synchronized void close() { cursor.close(); }
  }

  private static final class CharInput extends Reader {
    private final Cursor cursor;
    CharInput(Cursor cursor) { this.cursor = cursor; }

    @Override public synchronized int read() throws IOException {
      cursor.requireOpen();
      CharBuffer part = (CharBuffer) cursor.current();
      if (part == null) return -1;
      cursor.remaining--;
      return part.get();
    }

    @Override public synchronized int read(char[] target, int offset, int count) throws IOException {
      Objects.checkFromIndexSize(offset, count, Objects.requireNonNull(target, "target").length);
      cursor.requireOpen();
      if (count == 0) return 0;
      int wanted = Math.min(count, cursor.remaining);
      if (wanted == 0) return -1;
      int written = 0;
      while (written < wanted) {
        CharBuffer part = (CharBuffer) cursor.current();
        int take = Math.min(wanted - written, part.remaining());
        part.get(target, offset + written, take);
        written += take;
      }
      cursor.remaining -= written;
      return written;
    }

    @Override public synchronized long skip(long count) throws IOException {
      if (count < 0) throw new IllegalArgumentException("negative character skip");
      cursor.requireOpen(); return cursor.skip(count);
    }
    @Override public synchronized boolean ready() throws IOException {
      cursor.requireOpen(); return true;
    }
    @Override public boolean markSupported() { return true; }
    @Override public synchronized void mark(int readLimit) throws IOException {
      if (readLimit < 0) throw new IllegalArgumentException("negative read-ahead limit");
      cursor.requireOpen(); cursor.mark();
    }
    @Override public synchronized void reset() throws IOException {
      cursor.requireOpen(); cursor.reset();
    }
    @Override public synchronized void close() { cursor.close(); }
  }
}
