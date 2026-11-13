// SPDX-License-Identifier: Apache-2.0
package jdk.m3.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.IntSupplier;

/** Parent-owned byte storage: automatically replaces its hot array with a cold file. */
public final class SpoolingCapture extends OutputStream {
  private final IntSupplier budget;
  private ByteArrayOutputStream hot = new ByteArrayOutputStream(4096);
  private OutputStream cold;
  private Path file;
  private long size;
  private boolean finished;
  private boolean closed;

  SpoolingCapture(IntSupplier budget) {
    this.budget = Objects.requireNonNull(budget, "budget");
    if (budget.getAsInt() < 1) throw new IllegalArgumentException("invalid initial budget");
  }

  @Override public void write(int value) throws IOException {
    ensureWritable();
    if (cold == null && (long) hot.size() + 1 > budget.getAsInt()) spill();
    if (cold == null) hot.write(value); else cold.write(value);
    size = Math.addExact(size, 1);
  }

  @Override public void write(byte[] bytes, int offset, int length) throws IOException {
    Objects.checkFromIndexSize(offset, length, bytes.length);
    ensureWritable();
    if (length > Long.MAX_VALUE - size) throw new IOException("capture length overflow");
    if (cold == null && (long) hot.size() + length > budget.getAsInt()) spill();
    if (cold == null) hot.write(bytes, offset, length); else cold.write(bytes, offset, length);
    size += length;
  }

  private void ensureWritable() throws IOException {
    if (closed || finished) throw new IOException("capture no longer writable");
  }

  private void spill() throws IOException {
    file = Files.createTempFile("m3-native-pipeline-", ".spool");
    try {
      cold = Files.newOutputStream(file);
      hot.writeTo(cold);
      hot = null; // release the hot array immediately after cold promotion
    } catch (IOException e) {
      close();
      throw e;
    }
  }

  public void finish() throws IOException {
    if (closed) throw new IOException("capture closed");
    if (!finished) {
      if (cold != null) cold.close();
      finished = true;
    }
  }

  public long size() { return size; }
  public boolean spilled() { return file != null; }
  Path spillPath() { return file; }

  public InputStream openInput() throws IOException {
    if (!finished || closed) throw new IOException("capture must be finished and open");
    return file == null ? new ByteArrayInputStream(hot.toByteArray()) : Files.newInputStream(file);
  }

  /** Explicit opt-in materialization; large outputs are normally read through openInput(). */
  public byte[] bytes() throws IOException {
    if (size > Integer.MAX_VALUE) throw new IOException("capture exceeds byte[] maximum");
    try (InputStream source = openInput()) {
      return source.readAllBytes();
    }
  }

  @Override public void close() throws IOException {
    if (closed) return;
    closed = true;
    IOException error = null;
    if (cold != null && !finished) {
      try { cold.close(); } catch (IOException e) { error = e; }
    }
    if (file != null) {
      try { Files.deleteIfExists(file); }
      catch (IOException e) { if (error == null) error = e; else error.addSuppressed(e); }
    }
    if (error != null) throw error;
  }
}
