/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.lucene.analysis;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/**
 * Frozen caller observations for the original and generated reusable reader in separate JVMs.
 * Includes inherited Reader APIs and records raw UTF-16 units without replacement encoding.
 */
public final class M3ReaderCompatibilityProbe {
  private M3ReaderCompatibilityProbe() {}

  private static void record(MessageDigest digest, String value) {
    int count = value.length();
    digest.update((byte) (count >>> 24));
    digest.update((byte) (count >>> 16));
    digest.update((byte) (count >>> 8));
    digest.update((byte) count);
    for (int i = 0; i < count; i++) {
      char unit = value.charAt(i);
      digest.update((byte) (unit >>> 8));
      digest.update((byte) unit);
    }
  }

  public static void main(String[] args) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    Random random = new Random(0x4d33524541444552L);
    ReusableStringReader reader = new ReusableStringReader();
    int observations = 0;
    for (int trial = 0; trial < 4000; trial++) {
      char[] units = new char[trial % 137];
      for (int i = 0; i < units.length; i++) units[i] = (char) random.nextInt(65536);
      String input = new String(units);
      reader.setValue(input);
      for (int step = 0; step < 32; step++) {
        char[] output = new char[19];
        Arrays.fill(output, '\u6f22');
        StringWriter sink = new StringWriter();
        int operation = random.nextInt(16);
        Object result;
        try {
          switch (operation) {
            case 0 -> result = reader.read();
            case 1 -> result = reader.read(output, random.nextInt(27) - 4, random.nextInt(27) - 4);
            case 2 -> result = reader.read(null, random.nextInt(3) - 1, random.nextInt(3) - 1);
            case 3 -> {
              reader.close();
              result = reader.read(output, 0, 0);
            }
            case 4 -> {
              reader.setValue((String) null);
              result = "unexpected-null-acceptance";
            }
            case 5 -> {
              reader.setValue(input);
              result = reader.read(output, 1, Math.min(output.length - 1, input.length()));
            }
            case 6 -> result = reader.markSupported();
            case 7 -> {
              reader.mark(random.nextInt(10) - 3);
              result = "mark-returned";
            }
            case 8 -> {
              reader.reset();
              result = "reset-returned";
            }
            case 9 -> result = reader.skip(random.nextInt(15) - 2);
            case 10 -> result = reader.ready();
            case 11 -> {
              CharBuffer buffer = CharBuffer.wrap(output);
              buffer.position(random.nextInt(5));
              buffer.limit(5 + random.nextInt(15));
              int count = reader.read(buffer);
              result = count + ":" + buffer.position() + ":" + buffer.limit();
            }
            case 12 -> result = reader.read(CharBuffer.allocate(7).asReadOnlyBuffer());
            case 13 -> result = reader.transferTo(sink);
            case 14 -> {
              Writer rejecting = new Writer() {
                @Override public void write(char[] text, int start, int length) throws IOException {
                  sink.write(text, start, Math.min(3, length));
                  throw new IOException("frozen downstream writer rejection");
                }
                @Override public void flush() {}
                @Override public void close() {}
              };
              result = reader.transferTo(rejecting);
            }
            default -> result = reader.read(output);
          }
        } catch (Exception failure) {
          result = failure.getClass().getName() + ":" + failure.getMessage();
        }
        record(digest, operation + ":" + result);
        record(digest, new String(output));
        record(digest, sink.toString());
        observations++;
      }
    }
    System.out.println("LUCENE_READER_COMPAT observations=" + observations
        + " sha256=" + HexFormat.of().formatHex(digest.digest()));
  }
}
