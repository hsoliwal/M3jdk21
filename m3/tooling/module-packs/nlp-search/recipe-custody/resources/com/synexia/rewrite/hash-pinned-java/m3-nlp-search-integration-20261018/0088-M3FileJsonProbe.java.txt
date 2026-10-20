// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.file;

import com.m3.file.json.M3JsonStructuralIndex;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Standalone source and JNI contract proof; run patched into the actual named module. */
public final class M3FileJsonProbe {
  private static long checks;
  private static boolean requireNative;
  private static Path temporary;

  private M3FileJsonProbe() {}

  public static void main(String[] args) throws Exception {
    requireNative = args.length != 0 && args[0].equals("native");
    temporary = Files.createTempDirectory("m3-json-proof-");
    try {
      equal(requireNative, M3JsonStructuralNative.available(), "selected native availability");
      libraries();
      flags();
      progress();
      trees();
      json();
      metrics();
      System.out.println("M3_FILE_JSON_PASS checks=" + checks + " native=" + requireNative
          + " allByteValues=256 allUtf16Units=" + (requireNative ? 65536 : 0) + " randomFlagPages=10000"
          + " randomForests=200 nativeMetricsRequired=" + requireNative);
    } finally {
      try (var entries = Files.list(temporary)) {
        for (Path entry : entries.toList()) Files.deleteIfExists(entry);
      }
      Files.deleteIfExists(temporary);
    }
  }

  private static void libraries() throws IOException {
    M3NativeLibrarySpec named = M3NativeLibrarySpec.named("m3file");
    equal(false, named.explicit(), "logical library");
    equal("m3file", named.loadTarget(), "logical load target");
    equal("name:m3file", named.cacheKey(), "logical cache key");
    String prefix = "m3.json.probe.library";
    Path pathFile = temporary.resolve("library-path.txt");
    Files.writeString(pathFile, "relative-native.so\r\n", StandardCharsets.UTF_8);
    System.setProperty(prefix + ".pathFile", pathFile.toString());
    try {
      M3NativeLibrarySpec relative = M3NativeLibrarySpec.fromProperties(prefix, "m3file");
      equal(temporary.resolve("relative-native.so"), relative.explicitPath(), "relative path-file resolution");
      System.setProperty(prefix + ".path", temporary.resolve("explicit.so").toString());
      equal(temporary.resolve("explicit.so"), M3NativeLibrarySpec.fromProperties(prefix, "m3file").explicitPath(),
          "explicit path precedence");
      System.clearProperty(prefix + ".path");
      Files.writeString(pathFile, "one\ntwo\n", StandardCharsets.UTF_8);
      throwsType(IllegalArgumentException.class, () -> M3NativeLibrarySpec.fromProperties(prefix, "m3file"));
      Files.write(pathFile, new byte[] {(byte) 0xc0, (byte) 0xaf});
      throwsType(IllegalArgumentException.class, () -> M3NativeLibrarySpec.fromProperties(prefix, "m3file"));
      Files.writeString(pathFile, "x".repeat(65537), StandardCharsets.UTF_8);
      throwsType(IllegalArgumentException.class, () -> M3NativeLibrarySpec.fromProperties(prefix, "m3file"));
    } finally {
      System.clearProperty(prefix + ".path");
      System.clearProperty(prefix + ".pathFile");
    }
    throwsType(IllegalArgumentException.class, () -> new M3NativeLibrarySpec("", null));
    if (requireNative) {
      M3NativeLibrarySpec active = M3NativeLibrarySpec.fromProperties("m3.file.native", "m3file");
      equal(true, M3JniLibraryLoader.isLoaded(active), "shared native loader recorded load");
      M3JniLibraryLoader.load(active);
      equal(true, M3JniLibraryLoader.isLoaded(active), "shared native loader is idempotent");
    }
  }

  private static void flags() {
    ByteBuffer all = ByteBuffer.allocateDirect(272);
    for (int i = 0; i < 256; i++) all.put(i + 7, (byte) i);
    all.position(3).limit(270);
    ByteBuffer out = ByteBuffer.allocateDirect(274);
    out.position(4).limit(272);
    equal(requireNative ? M3JsonStructuralNative.Backend.JNI : M3JsonStructuralNative.Backend.JAVA,
        M3JsonStructuralNative.classify(all.asReadOnlyBuffer(), 7, 256, out, 9), "all flags backend");
    for (int i = 0; i < 256; i++) equal(flag(i), Byte.toUnsignedInt(out.get(i + 9)), "all byte flags");
    equal(3, all.position(), "input position");
    equal(270, all.limit(), "input limit");
    equal(4, out.position(), "output position");
    equal(272, out.limit(), "output limit");

    ByteBuffer readOnlyBacking = ByteBuffer.allocateDirect(8);
    readOnlyBacking.put(0, (byte) 99);
    throwsType(ReadOnlyBufferException.class,
        () -> M3JsonStructuralNative.classify(all, 7, 1, readOnlyBacking.asReadOnlyBuffer(), 0));
    equal(99, Byte.toUnsignedInt(readOnlyBacking.get(0)), "read-only bytes unchanged");
    M3JsonStructuralNative.classify(all, 0, 0, readOnlyBacking.asReadOnlyBuffer(), 0);
    checks++;

    Random random = new Random(0x4d334a534f4eL);
    for (int c = 0; c < 10000; c++) {
      int length = random.nextInt(257);
      byte[] bytes = new byte[length];
      random.nextBytes(bytes);
      ByteBuffer source = ByteBuffer.allocateDirect(length + 16);
      ByteBuffer target = ByteBuffer.allocateDirect(length + 16);
      source.position(5);
      source.put(bytes);
      source.position(2);
      target.position(1);
      equal(requireNative ? M3JsonStructuralNative.Backend.JNI : M3JsonStructuralNative.Backend.JAVA,
          M3JsonStructuralNative.classify(source, 5, length, target, 3), "random backend");
      for (int i = 0; i < length; i++) {
        equal(flag(Byte.toUnsignedInt(bytes[i])), Byte.toUnsignedInt(target.get(i + 3)), "random flags");
      }
      equal(2, source.position(), "random source position");
      equal(1, target.position(), "random target position");
    }

    ByteBuffer alias = ByteBuffer.allocate(16);
    byte[] expected = {'{', '"', 'x', '"', '}', '\n'};
    alias.put(expected);
    equal(M3JsonStructuralNative.Backend.JAVA,
        M3JsonStructuralNative.classify(alias.duplicate(), 0, 6, alias.duplicate(), 2), "heap alias backend");
    for (int i = 0; i < expected.length; i++) {
      equal(flag(Byte.toUnsignedInt(expected[i])), Byte.toUnsignedInt(alias.get(i + 2)), "heap alias snapshot");
    }

    ByteBuffer directAlias = ByteBuffer.allocateDirect(16);
    directAlias.put(expected);
    if (requireNative) {
      throwsType(IllegalArgumentException.class,
          () -> M3JsonStructuralNative.classify(directAlias.duplicate(), 0, 6, directAlias.duplicate(), 2));
      for (int i = 0; i < expected.length; i++) equal(expected[i], directAlias.get(i), "JNI overlap no write");
    } else {
      M3JsonStructuralNative.classify(directAlias.duplicate(), 0, 6, directAlias.duplicate(), 2);
      for (int i = 0; i < expected.length; i++) {
        equal(flag(Byte.toUnsignedInt(expected[i])), Byte.toUnsignedInt(directAlias.get(i + 2)),
            "direct Java alias snapshot");
      }
    }
    throwsType(IllegalArgumentException.class,
        () -> M3JsonStructuralNative.classify(directAlias, 0, 8, directAlias, 1));
    throwsType(IndexOutOfBoundsException.class,
        () -> M3JsonStructuralNative.classify(all, Integer.MAX_VALUE, 1, out, 0));
    throwsType(IndexOutOfBoundsException.class,
        () -> M3JsonStructuralNative.classify(all, 0, -1, out, 0));
    throwsType(IndexOutOfBoundsException.class,
        () -> M3JsonStructuralNative.classify(all, 0, 3, ByteBuffer.allocate(10).limit(2), 0));
    throwsType(NullPointerException.class,
        () -> M3JsonStructuralNative.classify(null, -1, -1, out, 0));
  }

  private static int flag(int value) {
    int bits = "{}[]:,".indexOf((char) value) >= 0 ? 1 : 0;
    if (value == 34) bits += 2;
    if (value == 92) bits += 4;
    if (value == 9 || value == 10 || value == 13 || value == 32) bits += 8;
    if (value == 10) bits += 16;
    if (value >= 128) bits += 32;
    if (value <= 31) bits += 64;
    return bits;
  }

  private static void progress() {
    M3ProgressMonitor noop = M3ProgressMonitor.noop();
    equal(noop, noop.split(Long.MAX_VALUE), "no-op split identity");
    noop.setCanceled(true);
    equal(false, noop.isCanceled(), "donor no-op cancellation boundary");
    equal(M3Progress.UNKNOWN, noop.snapshot().totalWork(), "unknown total");
    equal(0L, noop.state().startedAtMs(), "no-op epoch");
    throwsType(IllegalArgumentException.class, () -> noop.split(-1));
    Recording parent = new Recording();
    parent.beginTask("parent", 100);
    M3ProgressMonitor child = parent.split(30);
    child.beginTask("child", 12);
    child.worked(3);
    equal(7L, parent.snapshot().worked(), "weighted floor");
    child.worked(3);
    equal(15L, parent.snapshot().worked(), "weighted increment");
    child.setProgress(1, 12);
    equal(15L, parent.snapshot().worked(), "weighted monotonic parent");
    child.done();
    child.done();
    child.worked(Long.MAX_VALUE);
    equal(30L, parent.snapshot().worked(), "weighted done exactly once");
    throwsType(IllegalStateException.class, () -> child.beginTask("again", 4));
    throwsType(IllegalArgumentException.class, () -> child.worked(-1));
    M3ProgressMonitor canceled = parent.split(4);
    canceled.setCanceled(true);
    equal(true, parent.isCanceled(), "child cancellation forwarded");
    throwsType(CancellationException.class, canceled::checkCanceled);
    throwsType(CancellationException.class, parent::checkCanceled);

    Recording large = new Recording();
    large.beginTask("large", Long.MAX_VALUE);
    M3ProgressMonitor largeChild = large.split(Long.MAX_VALUE);
    largeChild.beginTask("large child", Long.MAX_VALUE);
    largeChild.worked(Long.MAX_VALUE - 1);
    equal(Long.MAX_VALUE - 1, large.snapshot().worked(), "overflow-safe weighted multiplication");
    largeChild.done();
    equal(Long.MAX_VALUE, large.snapshot().worked(), "large remainder");

    StateMonitor state = new StateMonitor();
    state.beginTask("state", 100);
    M3Progress weighted = state.split(20);
    weighted.beginTask("first generation", 2);
    weighted.worked(1);
    equal(10L, state.state().worked(), "common weighted progress");
    state.beginTask("new generation", 100);
    weighted.done();
    equal(0L, state.state().worked(), "stale common child cannot credit new generation");
    throwsType(IllegalArgumentException.class, () -> state.split(-1));
    throwsType(IllegalArgumentException.class, () -> new M3ProgressState("", "", -1, 3, false, false, 0, 0));
    equal(25.0, M3ProgressCalculations.calculatePercentage(1, 4), "percentage");
    equal(2.0, M3ProgressCalculations.calculateRate(4, 2000), "rate");
    equal(3000L, M3ProgressCalculations.calculateEta(4, 10, 2.0), "eta");
  }

  private static void trees() {
    Random random = new Random(0x5041474544L);
    for (int c = 0; c < 200; c++) {
      int count = 1 + random.nextInt(100);
      int[] parents = new int[count];
      for (int node = 0; node < count; node++) {
        parents[node] = node == 0 || random.nextInt(7) == 0 ? -1 : random.nextInt(node);
      }
      M3PageableTree paging = M3PageableTree.freezePaging(parents);
      M3PageableTree algorithms = M3PageableTree.freeze(parents);
      equal(false, paging.binaryLiftingEnabled(), "paging storage profile");
      equal(paging.nodeCount(), algorithms.nodeCount(), "tree size");
      for (int node = 0; node < count; node++) {
        equal(parents[node], paging.parent(node), "parent coordinate");
        equal(parents[node], algorithms.parent(node), "algorithm parent coordinate");
        int depth = 0;
        for (int at = parents[node]; at >= 0; at = parents[at]) depth++;
        equal(depth, paging.depth(node), "depth oracle");
        int at = node;
        for (int k = 0; k < depth + 2; k++) {
          equal(at, paging.kthAncestor(node, k), "paging ancestor oracle");
          equal(at, algorithms.kthAncestor(node, k), "algorithm ancestor oracle");
          if (at >= 0) at = parents[at];
        }
        for (int pair = 0; pair < 10; pair++) {
          int right = random.nextInt(count);
          int ancestor = -1;
          outer: for (int leftAt = node; leftAt >= 0; leftAt = parents[leftAt]) {
            for (int rightAt = right; rightAt >= 0; rightAt = parents[rightAt]) {
              if (leftAt == rightAt) { ancestor = leftAt; break outer; }
            }
          }
          equal(ancestor, paging.lowestCommonAncestor(node, right), "paging LCA oracle");
          equal(ancestor, algorithms.lowestCommonAncestor(node, right), "algorithm LCA oracle");
        }
      }
      int saved = parents[0];
      parents[0] = 99999;
      equal(saved, paging.parent(0), "tree snapshot owner");
      M3PageableTreeCursor cursor = paging.rootsCursor(0);
      equal(cursor, M3PageableTreeCursor.decode(cursor.encode()), "cursor bytes");
      equal(cursor, M3PageableTreeCursor.parse(cursor.token()), "cursor token");
      byte[] corrupt = cursor.encode();
      corrupt[11] ^= 1;
      throwsType(IllegalArgumentException.class, () -> M3PageableTreeCursor.decode(corrupt));
      M3PageableTree other = M3PageableTree.freezePaging(new int[] {-1, 0, 0, 1});
      if (other.fingerprint64() != cursor.snapshotFingerprint64()) {
        throwsType(IllegalArgumentException.class, () -> other.rootsPage(cursor, 5));
      }
    }
    int[] family = {-1, 0, 0, 1, 1, 2};
    M3PageableTree tree = M3PageableTree.freezePaging(family);
    equal(new int[] {3, 1, 0, 2, 5}, tree.path(3, 5), "unique path");
    long[] expanded = {1};
    M3PageableTree.VisibleCursor cursor = tree.firstVisibleCursor(expanded);
    equal(new int[] {0, 1, 2}, tree.visiblePage(expanded, cursor, 10).nodes(), "collapsed visible rows");
    expanded[0] = 3;
    throwsType(IllegalArgumentException.class, () -> tree.visiblePage(expanded, cursor, 10));
    throwsType(IllegalArgumentException.class, () -> M3PageableTree.freeze(new int[] {1, 0}));
    throwsType(IllegalArgumentException.class, () -> M3PageableTree.freeze(new int[] {-1, 1}));
    Recording canceled = new Recording();
    canceled.cancelAt = 3;
    throwsType(CancellationException.class, () -> M3PageableTree.freeze(family, canceled));
    equal(1, canceled.finished, "tree cancellation done");
  }

  private static void json() throws Exception {
    Path nested = temporary.resolve("nested.json");
    String text = "{\"name\":\"α\\nβ\",\"items\":[1,true,null,{\"k\":\"v\"}],\"n\":-12.5e+2}\n";
    Files.writeString(nested, text, StandardCharsets.UTF_8);
    Recording monitor = new Recording();
    try (M3JsonStructuralIndex index = M3JsonStructuralIndex.open(nested, monitor)) {
      equal(10, index.nodeCount(), "JSON node count");
      equal(1, monitor.finished, "JSON indexing done");
      equal("α\nβ", index.preview(2, 32), "escaped UTF-8 scalar");
      equal("items", index.displayName(3, 32), "property name");
      equal(4, index.childCount(3), "array child count");
      equal(5, index.childAt(3, 1), "array ordinal");
      equal("\"α\\nβ\"", index.rawValue(2, 32), "exact source lexeme");
      byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
      for (int node = 1; node < index.nodeCount(); node++) {
        String raw = new String(Arrays.copyOfRange(bytes, Math.toIntExact(index.startByte(node)),
            Math.toIntExact(index.endByte(node))), StandardCharsets.UTF_8);
        equal(raw, index.rawValue(node, bytes.length), "byte coordinates refer to exact donor");
      }
      M3JsonStructuralIndex.ViewportPage page = index.viewportChildren(3, 1, 2, 16);
      equal(2, page.size(), "bounded viewport size");
      equal("[1]", page.nameAt(0), "viewport name");
      equal("true", page.previewAt(0), "viewport preview");
      int[] childPage = new int[2];
      index.source().readChildren(3, 1, 2, childPage, 0);
      equal(new int[] {5, 6}, childPage, "JSON pageable Source");
      equal(index.nodeCount(), index.freezePagingTree().nodeCount(), "JSON frozen topology");
      M3JsonStructuralIndex.SignalPage signals = index.signalPage(0, bytes.length);
      equal(requireNative ? M3JsonStructuralNative.Backend.JNI : M3JsonStructuralNative.Backend.JAVA,
          signals.backend(), "JSON signal backend");
      for (int i = 0; i < bytes.length; i++) {
        equal(flag(Byte.toUnsignedInt(bytes[i])), signals.flagsAt(i), "JSON signal page");
      }
      throwsType(ReadOnlyBufferException.class, () -> signals.flags().put(0, (byte) 0));
      byte[] query = "α".getBytes(StandardCharsets.UTF_8);
      M3JsonStructuralIndex.RawSearch search = index.prepareRawSearch(query);
      Arrays.fill(query, (byte) 'X');
      equal(9L, search.indexOf(0), "prepared query snapshot and byte offset");
      throwsType(IndexOutOfBoundsException.class, () -> index.signalPage(0, 1024 * 1024 + 1));
    }
    Recording canceled = new Recording();
    canceled.cancelAt = 1;
    throwsType(CancellationException.class, () -> M3JsonStructuralIndex.open(nested, canceled));
    equal(1, canceled.finished, "JSON cancellation done");

    Path stream = temporary.resolve("stream.json");
    Files.writeString(stream, "{\"a\":1}\n[2,3]\ntrue\n", StandardCharsets.UTF_8);
    try (M3JsonStructuralIndex index = M3JsonStructuralIndex.open(stream)) {
      equal(true, index.multipleRoots(), "NDJSON roots");
      equal(3, index.childCount(0), "NDJSON root count");
      equal("$[1]", index.displayName(index.childAt(0, 1), 32), "NDJSON root name");
    }

    Path large = temporary.resolve("large.json");
    String content = "{\"prefix\":\"" + "x".repeat(65522) + "ABABAC" + "x".repeat(512 * 1024) + "ABABAC\"}";
    Files.writeString(large, content, StandardCharsets.UTF_8);
    try (M3JsonStructuralIndex index = M3JsonStructuralIndex.open(large)) {
      equal(3, index.nodeCount(), "large scalar structural-only node count");
      check(index.retainedPrimitivePayloadBytes() < 512, "large scalar primitive payload bound");
      equal("x".repeat(16) + "…", index.preview(2, 16), "bounded scalar preview");
      M3JsonStructuralIndex.RawSearch search = index.prepareRawSearch("ABABAC");
      long first = search.indexOf(0);
      equal(65533L, first, "KMP crosses 64KiB window");
      equal((long) content.lastIndexOf("ABABAC"), search.indexOf(first + 1), "KMP subsequent hit");
      equal(-1L, search.indexOf(content.length()), "KMP EOF");
      throwsType(IllegalArgumentException.class, () -> index.rawValue(2, 4096));
      Recording searchCanceled = new Recording();
      searchCanceled.cancelAt = 1;
      throwsType(CancellationException.class, () -> search.indexOf(0, searchCanceled));
      equal(0, searchCanceled.finished, "search does not own supplied monitor lifecycle");
    }
    int invalidId = 0;
    for (String invalid : new String[] {"{\"a\":01}", "[1,]", "{\"a\":true false}", "\"unterminated", "{}[]", ""}) {
      Path bad = temporary.resolve("bad-" + invalidId++ + ".json");
      Files.writeString(bad, invalid, StandardCharsets.UTF_8);
      throwsType(IllegalArgumentException.class, () -> M3JsonStructuralIndex.open(bad));
    }
    Path badUtf8 = temporary.resolve("bad-utf8.json");
    Files.write(badUtf8, new byte[] {'"', (byte) 0xc0, (byte) 0xaf, '"'});
    throwsType(IllegalArgumentException.class, () -> M3JsonStructuralIndex.open(badUtf8));

    Path stale = temporary.resolve("stale.json");
    Files.writeString(stale, "{\"a\":\"value\"}", StandardCharsets.UTF_8);
    M3JsonStructuralIndex index = M3JsonStructuralIndex.open(stale);
    M3JsonStructuralIndex.RawSearch search = index.prepareRawSearch("value");
    Files.writeString(stale, "{\"a\":\"other\"}", StandardCharsets.UTF_8);
    Files.setLastModifiedTime(stale, FileTime.fromMillis(System.currentTimeMillis() + 2000));
    throwsType(IllegalStateException.class, () -> index.preview(2, 32));
    throwsType(IllegalStateException.class, () -> search.indexOf(0));
    throwsType(IllegalStateException.class, () -> index.signalPage(0, 4));
    index.close();
    index.close();
    equal(M3JsonStructuralIndex.Kind.STRING, index.kind(2), "closed primitive geometry retained");
    throwsType(IllegalStateException.class, () -> index.preview(2, 32));
  }

  private static void metrics() throws Exception {
    if (!requireNative) return;
    // Directly invoke the actual required JNI symbol: no silent Java fallback can satisfy this lane.
    Class<?> worker = Class.forName("com.m3.file.M3TextMetrics$Native");
    Method nativeMetrics = worker.getDeclaredMethod("directMetrics0",
        CharBuffer.class, int.class, int.class, boolean.class, int.class);
    nativeMetrics.setAccessible(true);
    int replacement = StandardCharsets.UTF_8.newEncoder().replacement().length;
    for (ByteOrder order : new ByteOrder[] {ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN}) {
      CharBuffer buffer = ByteBuffer.allocateDirect(24).order(order).asCharBuffer();
      for (int unit = 0; unit <= 65535; unit++) {
        String value = "A" + (char) unit + "Z";
        buffer.position(2);
        buffer.put(value);
        buffer.limit(5).position(2);
        CharBuffer readOnly = buffer.asReadOnlyBuffer();
        int[] actual = (int[]) nativeMetrics.invoke(null, readOnly, 2, 3,
            order == ByteOrder.LITTLE_ENDIAN, replacement);
        M3TextMetrics expected = M3TextMetrics.of(value);
        int[] facts = {expected.utf16Length(), expected.utf8Length(), expected.codePointCount(),
            expected.unpairedSurrogateCount(), expected.javaHashCode(), expected.hash31Power(),
            expected.firstUtf16Unit(), expected.lastUtf16Unit()};
        equal(facts, actual, "required JNI exact UTF-16 metrics");
        equal(value.hashCode(), actual[4], "JDK hash oracle");
        equal(value.getBytes(StandardCharsets.UTF_8).length, actual[1], "JDK UTF-8 oracle");
        equal(value.codePointCount(0, value.length()), actual[2], "JDK code point oracle");
        equal(2, readOnly.position(), "native metrics source position");
        buffer.clear();
      }
      for (String value : new String[] {"", "😀", "\ud800\ud801\udc00", "\udc00\ud800", "\ud800Z"}) {
        buffer.clear().position(2);
        buffer.put(value);
        buffer.limit(2 + value.length()).position(2);
        int[] actual = (int[]) nativeMetrics.invoke(null, buffer, 2, value.length(),
            order == ByteOrder.LITTLE_ENDIAN, replacement);
        equal(value.getBytes(StandardCharsets.UTF_8).length, actual[1], "surrogate native UTF-8");
        equal(value.codePointCount(0, value.length()), actual[2], "surrogate native points");
      }
    }
  }

  private static void check(boolean condition, String label) {
    checks++;
    if (!condition) throw new AssertionError(label);
  }

  private static void equal(Object expected, Object actual, String label) {
    checks++;
    if (expected instanceof int[] left && actual instanceof int[] right) {
      if (Arrays.equals(left, right)) return;
      throw new AssertionError(label + ": expected " + Arrays.toString(left) + ", got " + Arrays.toString(right));
    }
    if (!Objects.equals(expected, actual)) {
      throw new AssertionError(label + ": expected " + expected + ", got " + actual);
    }
  }

  private static void throwsType(Class<? extends Throwable> type, Checked operation) {
    checks++;
    try { operation.run(); }
    catch (Throwable failure) {
      if (type.isInstance(failure)) return;
      throw new AssertionError("Expected " + type.getName() + ", got " + failure, failure);
    }
    throw new AssertionError("Expected " + type.getName());
  }

  @FunctionalInterface
  private interface Checked { void run() throws Exception; }

  private static final class Recording implements M3ProgressMonitor {
    private String task = "";
    private String sub = "";
    private long doneWork;
    private long total = UNKNOWN;
    private boolean canceled;
    private boolean done;
    private int finished;
    private int cancelAt = Integer.MAX_VALUE;
    private int canceledChecks;

    @Override public synchronized void beginTask(String name, long totalWork) {
      task = name; total = totalWork; doneWork = 0; done = false;
    }
    @Override public synchronized void done() { finished++; done = true; }
    @Override public synchronized void worked(long work) { doneWork = Math.addExact(doneWork, work); }
    @Override public synchronized void setProgress(long work, long totalWork) { doneWork = work; total = totalWork; }
    @Override public synchronized void setTaskName(String name) { task = name; }
    @Override public synchronized void subTask(String name) { sub = name; }
    @Override public synchronized boolean isCanceled() { return canceled; }
    @Override public synchronized void setCanceled(boolean value) { canceled = value; }
    @Override public synchronized M3ProgressSnapshot snapshot() {
      return new M3ProgressSnapshot(task, sub, doneWork, total, -1, 0, 0, -1, canceled, done, Instant.EPOCH);
    }
    @Override public synchronized void checkCanceled() {
      if (++canceledChecks >= cancelAt) canceled = true;
      M3ProgressMonitor.super.checkCanceled();
    }
  }

  private static final class StateMonitor implements M3Progress {
    private long generation;
    private long completed;
    private long total = UNKNOWN;
    private String task = "";
    private boolean canceled;
    private boolean done;

    @Override public void beginTask(String name, long totalWork) {
      generation++; completed = 0; total = totalWork; task = name; done = false;
    }
    @Override public void done() { done = true; }
    @Override public void worked(long work) { completed = Math.addExact(completed, work); }
    @Override public void setProgress(long work, long totalWork) { completed = work; total = totalWork; }
    @Override public void setTaskName(String name) { task = name; }
    @Override public void subTask(String name) {}
    @Override public boolean isCanceled() { return canceled; }
    @Override public void setCanceled(boolean value) { canceled = value; }
    @Override public M3ProgressState state() {
      return new M3ProgressState(task, "", completed, total, canceled, done, generation, 0);
    }
  }
}
