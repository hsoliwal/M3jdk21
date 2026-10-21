// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Pinned MIndexStringCharsetEncoding state machine; output generalized to a bounded sink.
package com.m3.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.function.IntSupplier;

/** Bounded streaming encoding state, owned by one call and never retained by a file descriptor. */
final class M3FileCharsetEncoding {
  @FunctionalInterface interface Sink { void accept(ByteBuffer bytes) throws IOException; }
  private final IntSupplier units;
  private final CharsetEncoder encoder;
  private final Sink sink;
  private final CharBuffer input = CharBuffer.allocate(4096);
  private ByteBuffer output = ByteBuffer.allocate(8192);
  private boolean sourceExhausted;
  private long written;

  private M3FileCharsetEncoding(IntSupplier units, Charset charset, Sink sink) {
    this.units = units;
    encoder = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
    this.sink = sink;
    input.limit(0);
  }

  static long encode(IntSupplier units, Charset charset, Sink sink) throws IOException {
    return new M3FileCharsetEncoding(units, charset, sink).encode();
  }

  private long encode() throws IOException {
    do {
      refill();
      while (encodeBuffer().isOverflow()) { /* Continue the same encoder state. */ }
    } while (!sourceExhausted);
    while (flush().isOverflow()) { /* Flush state may require a larger atomic output. */ }
    return written;
  }

  /** Preserve every unconsumed unit, including malformed or multi-unit lookahead tails. */
  private void refill() {
    input.compact();
    if (!input.hasRemaining()) {
      requireEndOfSource();
    } else {
      while (input.hasRemaining()) {
        int unit = units.getAsInt();
        if (!hasUnit(unit)) break;
        input.put((char) unit);
      }
    }
    input.flip();
  }

  private boolean hasUnit(int unit) {
    sourceExhausted = unit < 0;
    return !sourceExhausted;
  }

  /** Permit end-of-input processing even when an encoder retained the complete buffer. */
  private void requireEndOfSource() {
    if (!hasUnit(units.getAsInt())) return;
    throw new IllegalStateException("encoder requires more than 4096 input units for " + encoder.charset());
  }

  private CoderResult encodeBuffer() throws IOException {
    int before = input.position();
    CoderResult result = encoder.encode(input, output, sourceExhausted);
    boolean progressed = input.position() != before || output.position() != 0;
    drain();
    checkResult(result, progressed, "encoding");
    if (sourceExhausted && result.isUnderflow() && input.hasRemaining()) {
      throw new IllegalStateException("encoder retained final input for " + encoder.charset());
    }
    return result;
  }

  private CoderResult flush() throws IOException {
    CoderResult result = encoder.flush(output);
    boolean progressed = output.position() != 0;
    drain();
    checkResult(result, progressed, "flushing");
    return result;
  }

  private void checkResult(CoderResult result, boolean progressed, String phase) {
    if (result.isError()) {
      throw new IllegalStateException("indexed text failed while " + phase + " for " + encoder.charset());
    }
    if (result.isOverflow() && !progressed) growOutput(phase);
  }

  /** A valid encoder may require an atomic output larger than the initial byte buffer. */
  private void growOutput(String phase) {
    double declared = Math.ceil(encoder.maxBytesPerChar() * (double) input.capacity());
    int limit = (int) Math.max(8192, Math.min(Integer.MAX_VALUE - 8L, declared));
    if (output.capacity() >= limit) {
      throw new IllegalStateException("encoder made no progress while " + phase + " for " + encoder.charset());
    }
    int capacity = (int) Math.min(limit, output.capacity() * 2L);
    output = ByteBuffer.allocate(capacity);
  }

  private void drain() throws IOException {
    output.flip();
    int count = output.remaining();
    if (count != 0) sink.accept(output);
    if (output.hasRemaining()) throw new IOException("encoding sink did not consume buffer");
    written = Math.addExact(written, count);
    output.clear();
  }
}
