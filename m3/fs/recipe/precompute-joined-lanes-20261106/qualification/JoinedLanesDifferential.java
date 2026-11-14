// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Differential driver of recipe PRECOMPUTE_JOINED_LANES v1. Runs against whichever jdk.m3.fs.M3FsPrecomputeBundle
// variant is first on the class path and prints digests that differential.sh compares between stage builds.
// presence: the input stream of the Synexia crate m3fs-memchr-bitmap-20261102 proof M3BytePresenceFastSearchProof
// (same seed, same 16 segment counts, same sparse forcing), applied to the real JoinedBytePresence.candidateRows; the
// proof's bitmap-read model runs beside it (unoptimized and early-refusal) and must agree with the real rows.
// raw: JoinedContentSegments.openRawSegments over a real M3DirectoryContentStore with repeated content IDs, budgets
// at, below and above the distinct payload total, and object-size limits; every result or refusal enters the trace.
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import jdk.m3.fs.M3ContentId;
import jdk.m3.fs.M3DirectoryContentStore;
import jdk.m3.fs.M3FsPrecomputeBundle;
import jdk.m3.fs.M3ObjectKind;
import jdk.m3.fs.M3ObjectRef;

public final class JoinedLanesDifferential {
  private static final int[] DIMENSIONS = {0, 1, 2, 31, 63, 64, 65, 66, 127, 128, 129, 255, 256, 257, 512, 1024};

  public static void main(String[] args) throws Exception {
    if (args.length == 2 && args[0].equals("presence")) {
      presence(Integer.parseInt(args[1]));
    } else if (args.length == 3 && args[0].equals("raw")) {
      raw(Integer.parseInt(args[1]), Path.of(args[2]));
    } else {
      throw new IllegalArgumentException("usage: presence <trials> | raw <scenarios> <store-dir>");
    }
  }

  private static int[] model(LongBuffer bitmap, int segments, int words, byte[] needle, boolean early, long[] reads) {
    int[] output = new int[segments];
    int count = 0;
    for (int word = 0; word < words; word++) {
      long candidates = -1L;
      for (byte value : needle) {
        candidates &= bitmap.get(Byte.toUnsignedInt(value) * words + word);
        reads[0]++;
        if (early && candidates == 0L) break;
      }
      if (word == words - 1 && (segments & 63) != 0) candidates &= (1L << (segments & 63)) - 1L;
      while (candidates != 0L) {
        output[count++] = (word << 6) + Long.numberOfTrailingZeros(candidates);
        candidates &= candidates - 1L;
      }
    }
    return Arrays.copyOf(output, count);
  }

  private static void presence(int trials) throws Exception {
    Random rng = new Random(0x4d334a4156413231L);
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    long cases = 0, oldReads = 0, newReads = 0, agree = 0;
    for (int segments : DIMENSIONS) {
      int words = (segments + 63) >>> 6;
      for (int trial = 0; trial < trials; trial++) {
        long[] bits = new long[256 * words];
        for (int i = 0; i < bits.length; i++) bits[i] = rng.nextLong();
        byte[] needle = new byte[1 + rng.nextInt(32)];
        rng.nextBytes(needle);
        if (trial % 3 == 0 && words > 0) {
          for (int w = 0; w < words; w++) bits[Byte.toUnsignedInt(needle[0]) * words + w] = 0L;
        }
        var index = new M3FsPrecomputeBundle.JoinedBytePresence(
            LongBuffer.wrap(bits), IntBuffer.wrap(new int[] {segments, words}));
        int[] output = new int[segments];
        int count = index.candidateRows(needle, output);
        int[] real = Arrays.copyOf(output, count);
        long[] a = {0}, b = {0};
        int[] unoptimized = model(LongBuffer.wrap(bits), segments, words, needle, false, a);
        int[] early = model(LongBuffer.wrap(bits), segments, words, needle, true, b);
        if (!Arrays.equals(unoptimized, early)) throw new AssertionError("model mismatch " + segments + "/" + trial);
        if (Arrays.equals(real, unoptimized)) agree++;
        ByteBuffer row = ByteBuffer.allocate(12 + 4 * count).putInt(segments).putInt(trial).putInt(count);
        for (int value : real) row.putInt(value);
        digest.update(row.array());
        oldReads += a[0];
        newReads += b[0];
        cases++;
      }
    }
    System.out.println("PRESENCE cases=" + cases + " rows_sha256=" + HexFormat.of().formatHex(digest.digest())
        + " model_old_reads=" + oldReads + " model_new_reads=" + newReads + " model_equals_real=" + agree);
  }

  private static void raw(int scenarios, Path root) throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(Files.createDirectories(root));
    Random rng = new Random(0x4d3352415753L);
    int poolSize = 420;
    M3ObjectRef[] pool = new M3ObjectRef[poolSize];
    for (int p = 0; p < poolSize; p++) {
      byte[] bytes = new byte[1 + rng.nextInt(96)];
      rng.nextBytes(bytes);
      bytes[0] = (byte) p;
      bytes[bytes.length - 1] = (byte) (p >>> 8);
      pool[p] = store.put(bytes, M3ObjectKind.RAW_BYTES);
    }
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    long calls = 0, ok = 0;
    for (int s = 0; s < scenarios; s++) {
      int distinct = 1 + rng.nextInt(s % 4 == 0 ? poolSize : 40);
      int segments = rng.nextInt(1500);
      int[] pick = new int[segments], starts = new int[segments], lengths = new int[segments];
      ByteBuffer ids = ByteBuffer.allocate(segments * M3ContentId.BYTES);
      long[] logical = new long[segments + 1];
      for (int i = 0; i < segments; i++) {
        pick[i] = rng.nextInt(distinct);
        int size = Math.toIntExact(pool[pick[i]].size());
        starts[i] = rng.nextInt(size + 1);
        lengths[i] = rng.nextInt(size - starts[i] + 1);
        if (s % 9 == 4 && i == segments - 1) lengths[i] = size - starts[i] + 1; // payload overrun
        ids.put(pool[pick[i]].id().bytes());
        logical[i + 1] = logical[i] + lengths[i];
      }
      ids.flip();
      var directory = new M3FsPrecomputeBundle.JoinedContentSegments(
          ids, IntBuffer.wrap(starts), LongBuffer.wrap(logical), IntBuffer.wrap(lengths));
      long needed = 0;
      boolean[] seen = new boolean[poolSize];
      for (int i = 0; i < segments; i++) {
        if (!seen[pick[i]]) { seen[pick[i]] = true; needed += pool[pick[i]].size(); }
      }
      long[] budgets = {needed, Math.max(0, needed - 1), needed / 2, needed + 7, 0};
      int[] objectLimits = {1 << 20, 64, 1 << 20, 32, 1 << 20};
      for (int b = 0; b < budgets.length; b++) {
        String line = trace(directory, store, objectLimits[b], budgets[b]);
        digest.update(line.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ok += line.contains(":OK ") ? 1 : 0;
        calls++;
      }
    }
    System.out.println("RAW scenarios=" + scenarios + " calls=" + calls + " mapped=" + ok + " refused=" + (calls - ok) + " trace_sha256="
        + HexFormat.of().formatHex(digest.digest()));
  }

  private static String trace(M3FsPrecomputeBundle.JoinedContentSegments directory, M3DirectoryContentStore store,
      int maxObjectBytes, long budget) throws Exception {
    StringBuilder out = new StringBuilder().append(maxObjectBytes).append('/').append(budget).append(':');
    try {
      var mapped = directory.openRawSegments(store, maxObjectBytes, budget);
      out.append("OK u=").append(mapped.uniquePayloadCount()).append(" b=").append(mapped.mappedPayloadBytes())
          .append(" n=").append(mapped.segmentCount()).append(" L=").append(mapped.logicalLength()).append(" h=");
      MessageDigest bytes = MessageDigest.getInstance("SHA-256");
      for (int i = 0; i < mapped.segmentCount(); i++) {
        ByteBuffer segment = mapped.segment(i);
        bytes.update(ByteBuffer.allocate(4).putInt(segment.remaining()).array());
        bytes.update(segment);
      }
      out.append(HexFormat.of().formatHex(bytes.digest()));
    } catch (IOException | RuntimeException refusal) {
      out.append(refusal.getClass().getName()).append(':').append(refusal.getMessage());
    }
    return out.append('\n').toString();
  }
}
