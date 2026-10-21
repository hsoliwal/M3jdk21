// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Host/JNI/second-JVM proof without a second dictionary, source rewrite or JDK contract claim. */
public final class M3FmtLexiconFrameContract {
  private static final String ROOT = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
  private static final long[] IDS = {0L, 1L, 3L, 0x7000000000001L, Long.MIN_VALUE, -1L};
  private static final String[] SPELLINGS = {"apple", " ", "orange", "class X { String p = \"a.*\"; }", "😀", "λ"};
  private static long checks;
  private M3FmtLexiconFrameContract() {}

  private static M3FmtLexiconFrame.Lexicon lexicon(String root) {
    return new M3FmtLexiconFrame.Lexicon() {
      @Override public String catalogRoot() { return root; }
      @Override public void writeFrozenUtf8(long id, OutputStream destination) throws IOException {
        for (int i = 0; i < IDS.length; i++) {
          if (IDS[i] == id) {
            destination.write(SPELLINGS[i].getBytes(StandardCharsets.UTF_8));
            return;
          }
        }
        throw new IOException("unknown or overlay ID");
      }
    };
  }

  private static void check(boolean value, String description) {
    checks++;
    if (!value) throw new AssertionError(description);
  }

  private static void refuses(Throwing operation) {
    checks++;
    try { operation.run(); }
    catch (IOException | IllegalArgumentException expected) { return; }
    throw new AssertionError("expected fail-closed refusal");
  }

  private interface Throwing { void run() throws IOException; }

  private static void varints() {
    Random random = new Random(0x4d33464d544cL);
    for (int length : new int[] {0, 1, 2, 63, 64, 127, 128, 1000}) {
      long[] values = new long[length];
      for (int i = 0; i < length; i++) {
        values[i] = switch (i % 9) {
          case 0 -> 0L;
          case 1 -> 127L;
          case 2 -> 128L;
          case 3 -> 0xffffffffL;
          case 4 -> Long.MIN_VALUE;
          case 5 -> -1L;
          default -> random.nextLong();
        };
      }
      for (int repeat = 0; repeat < 50; repeat++) {
        long[] result = M3FmtVarints.unpack(M3FmtVarints.pack(values), length);
        check(Arrays.equals(values, result), "varint roundtrip " + length);
      }
    }
    for (byte[] invalid : new byte[][] {{(byte) 0x80}, {(byte) 0x80, 0},
        {(byte) 0x80, (byte) 0x80, 0},
        {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
          (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 2},
        {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
          (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x81, 0}}) {
      refuses(() -> M3FmtVarints.unpack(invalid, 1));
    }
  }

  private static M3FmtLexiconFrame.Frame sample() throws IOException {
    byte[] raw = {(byte) 0xff, (byte) 0xc0, 0, '\r', '\n'};
    byte[] kinds = {0, 0, 0, 1, 0, 0, 0, 0};
    long[] ids = {IDS[0], IDS[1], IDS[3], 0L, IDS[1], IDS[4], IDS[1], IDS[5]};
    int[] offsets = {0, 0, 0, 0, 5, 5, 5, 5, 5};
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("mode", "raw-source");
    meta.put("filename", "demo.java");
    return M3FmtLexiconFrame.seal(lexicon(ROOT), meta, kinds, ids, offsets, raw);
  }

  private static void regexCorpus(byte[] raw) {
    String s = new String(raw, StandardCharsets.ISO_8859_1);
    for (String expression : new String[] {
        "apple", ".*?", "^apple", "apple\\s", "(a)(b)\\2", "\\bclass\\b",
        "(?m)^class", "(?:class|interface)", "a.*\\}", "[A-Z]+", "(?<=apple) ",
        "(?s).*λ", "\\p{L}+", "(?:a|b){2,5}", "(a+)+$", "[invalid"
    }) {
      try {
        Pattern compiled = Pattern.compile(expression);
        boolean beforeFind = compiled.matcher(s).find();
        boolean afterFind = compiled.matcher(new String(raw, StandardCharsets.ISO_8859_1)).find();
        check(beforeFind == afterFind, "regex oracle reproducibility " + expression);
      } catch (PatternSyntaxException invalid) {
        check("[invalid".equals(expression), "unexpected invalid regex");
      }
    }
  }

  private static void transport() throws Exception {
    var frame = sample();
    byte[] encoded = M3FmtLexiconFrame.write(frame);
    byte[] encodedAgain = M3FmtLexiconFrame.write(frame);
    check(Arrays.equals(encoded, encodedAgain), "canonical wire deterministic");
    var restored = M3FmtLexiconFrame.read(encoded, lexicon(ROOT));
    check(frame.contentRoot().equals(restored.contentRoot()), "digest identity");
    check(Arrays.equals(M3FmtLexiconFrame.materialize(frame, lexicon(ROOT)),
                        M3FmtLexiconFrame.materialize(restored, lexicon(ROOT))),
        "lossless source bytes");
    check(restored.segmentCount() == 8 && restored.literalByteCount() == 5, "segmented lanes");
    regexCorpus(M3FmtLexiconFrame.materialize(restored, lexicon(ROOT)));
    refuses(() -> M3FmtLexiconFrame.read(encoded, lexicon("f".repeat(64))));
    refuses(() -> M3FmtLexiconFrame.materialize(restored, lexicon("f".repeat(64))));
    refuses(() -> M3FmtLexiconFrame.seal(lexicon(ROOT), Map.of(),
        new byte[] {0}, new long[] {99}, new int[] {0, 0}, new byte[0]));
    refuses(() -> M3FmtLexiconFrame.seal(lexicon(ROOT), Map.of(),
        new byte[] {1}, new long[] {1}, new int[] {0, 1}, new byte[] {1}));
    refuses(() -> M3FmtLexiconFrame.seal(lexicon(ROOT), Map.of(),
        new byte[] {0}, new long[] {0}, new int[] {0, 1}, new byte[] {1}));
    refuses(() -> M3FmtLexiconFrame.seal(lexicon(ROOT), Map.of(),
        new byte[] {1}, new long[] {0}, new int[] {0, 0}, new byte[0]));
    refuses(() -> M3FmtLexiconFrame.seal(lexicon(ROOT), Map.of(),
        new byte[] {3}, new long[] {0}, new int[] {0, 0}, new byte[0]));
    for (int where : new int[] {0, 8, 16, 65, encoded.length / 2, encoded.length - 1}) {
      byte[] corrupt = encoded.clone(); corrupt[where] ^= 1;
      refuses(() -> M3FmtLexiconFrame.read(corrupt, lexicon(ROOT)));
    }
    var one = M3FmtLexiconFrame.seal(lexicon(ROOT), Map.of(), new byte[0],
        new long[0], new int[] {0}, new byte[0]);
    check(M3FmtLexiconFrame.read(M3FmtLexiconFrame.write(one), lexicon(ROOT)).segmentCount() == 0,
        "empty frame");

    Path tmp = Files.createTempFile("m3fmt-shared-lexicon-", ".frame");
    try {
      Files.write(tmp, encoded);
      Path java = Path.of(System.getProperty("java.home"), "bin", "java");
      Process child = new ProcessBuilder(java.toString(), "-cp", System.getProperty("java.class.path"),
          M3FmtLexiconFrameContract.class.getName(), "--child", tmp.toString(), frame.contentRoot())
          .redirectErrorStream(true).start();
      boolean exited = child.waitFor(20, TimeUnit.SECONDS);
      if (!exited) child.destroyForcibly();
      check(exited, "cross-JVM read completed");
      String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      check(child.exitValue() == 0 && output.contains("CROSS_JVM_PASS"), "cross-JVM: " + output);
    } finally { Files.deleteIfExists(tmp); }
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 3 && "--child".equals(args[0])) {
      var frame = M3FmtLexiconFrame.read(Files.readAllBytes(Path.of(args[1])), lexicon(ROOT));
      check(frame.contentRoot().equals(args[2]), "second JVM content fingerprint");
      check(M3FmtLexiconFrame.materialize(frame, lexicon(ROOT)).length > 0, "second JVM bytes");
      System.out.println("CROSS_JVM_PASS " + ProcessHandle.current().pid());
      return;
    }
    varints();
    transport();
    System.out.println("M3FMT_JAVA_CONTRACT_PASS checks=" + checks
        + " native=" + M3FmtVarints.nativeAvailable());
  }
}
