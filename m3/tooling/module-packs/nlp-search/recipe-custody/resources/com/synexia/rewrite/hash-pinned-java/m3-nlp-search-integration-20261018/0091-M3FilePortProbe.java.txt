// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/** Independent String, naive search and physical-I/O oracles for the complete file receiver. */
public final class M3FilePortProbe {
  private static long checks;
  private static final long SEED = 0x4d3346494c453031L;
  private M3FilePortProbe() { }

  public static void main(String[] args) throws Exception {
    boolean nativeRequired = args.length == 1 && args[0].equals("native");
    check(M3FileNative.available() == nativeRequired, "native mode must be explicit");
    dictionary();
    text();
    nativeSearch(nativeRequired);
    Path directory = Files.createTempDirectory("m3-file-port-");
    try {
      images(directory);
      io(directory);
      retention();
    } finally {
      try (var paths = Files.walk(directory)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
      }
    }
    System.out.println("M3_FILE_PORT_PASS|mode=" + (nativeRequired ? "native" : "java")
        + "|seed=" + Long.toUnsignedString(SEED) + "|checks=" + checks
        + "|stringOwner=1|descriptorPayloadArrays=0|packedIds=1|frozenGeneration=1"
        + "|randomUtf16=1|streamUtf8=1|diskReopen=1|malformedImages=1|snapshot=1");
  }

  private static void dictionary() throws Exception {
    M3FileDictionary resolver = new M3FileDictionary();
    List<Long> admitted = new ArrayList<>();
    for (int row = 0; row < 6000; row++) {
      int language = (row * 173) & 65535;
      String value = "word" + row;
      CharSequence input = opaque(value);
      check(resolver.lookup(language, input).isEmpty(), "lookup must not admit a miss");
      long id = resolver.intern(language, input);
      admitted.add(id);
      check(M3FileId.isOverlay(id), "live ID must be overlay");
      check(M3FileId.language(id) == language, "packed language");
      check(resolver.lookup(language, input).orElseThrow() == id, "looked-up ID");
      check(resolver.resolve(id).equals(value), "reverse lookup exact");
      check(resolver.resolve(id) == value.intern(), "one canonical String reference");
    }
    for (int row = admitted.size() - 1; row >= 0; row--) {
      check(resolver.resolve(admitted.get(row)).equals("word" + row), "interleaved reverse directory");
    }
    check(resolver.size() == admitted.size(), "no lookup-side growth");
    check(resolver.coordinateFingerprint(0).isEmpty(), "live coordinates have no durable identity");
    check(M3FileId.base(65535, M3FileId.MAX_LOCAL_ID) == 0xffff7fffffffffffL, "packed base width");
    check(M3FileId.overlay(65535, M3FileId.MAX_LOCAL_ID) == -1L, "packed overlay width");
    expect(IllegalArgumentException.class, () -> resolver.intern(-1, "x"));
    expect(IllegalArgumentException.class, () -> resolver.resolve(M3FileId.base(0, 0)));
    expect(IllegalArgumentException.class, () -> resolver.resolve(M3FileId.overlay(0, 500000)));
    expect(NullPointerException.class, () -> resolver.lookup(0, null));
    expect(NullPointerException.class, () -> resolver.intern(0, null));

    List<M3FileDictionary.Language> generations = List.of(
        new M3FileDictionary.Language(9, "el", "v1", List.of("β", "alpha", "")),
        new M3FileDictionary.Language(0, "en", "v1", List.of("", "alpha", " ")));
    M3FileDictionary frozen = M3FileDictionary.frozen(generations);
    M3FileDictionary same = M3FileDictionary.frozen(generations);
    for (M3FileDictionary.Language generation : generations) {
      check(frozen.coordinateFingerprint(generation.id()).equals(same.coordinateFingerprint(generation.id())), "stable fingerprint");
      for (int i = 0; i < generation.words().size(); i++) {
        long expected = M3FileId.base(generation.id(), i);
        check(frozen.intern(generation.id(), generation.words().get(i)) == expected, "ordered frozen base ID");
        check(frozen.resolve(expected).equals(generation.words().get(i)), "frozen reverse lookup");
      }
    }
    check(frozen.isFrozen(), "frozen marker");
    expect(IllegalArgumentException.class, () -> frozen.intern(0, "unlisted"));
    expect(IllegalArgumentException.class, () -> frozen.resolve(M3FileId.overlay(0, 0)));
    expect(IllegalArgumentException.class, () -> M3FileDictionary.frozen(0, "en", "v1", "x", "x"));
    expect(IllegalArgumentException.class, () -> M3FileDictionary.frozen(0, "en", "v1", "\ud800"));
    expect(IllegalArgumentException.class, () -> M3FileDictionary.frozen(0, "\ud800", "v1", "?"));
    expect(IllegalArgumentException.class, () -> M3FileDictionary.frozen(0, "en", "\udc00", "?"));
    expect(IllegalArgumentException.class, () -> M3FileDictionary.frozen(List.of(generations.get(0), generations.get(0))));
    M3FileDictionary changed = M3FileDictionary.frozen(0, "en", "v2", "", "alpha", " ");
    check(!frozen.coordinateFingerprint(0).equals(changed.coordinateFingerprint(0)), "generation version affects identity");
    M3FileText first = M3FileText.intern(frozen, 0, "alpha");
    expect(IllegalArgumentException.class, () -> first.concat(M3FileText.intern(same, 0, "alpha")));
    expect(IllegalArgumentException.class, () -> first.substring(1, 3));
  }

  private static void text() throws Exception {
    M3FileDictionary resolver = new M3FileDictionary();
    M3FileText partition = new M3FileTextBuilder(resolver)
        .appendTokenizedText(3, "a.b  \r\ncan't$tail_\u0301 😀12.34").build();
    String[] expectedTokens = {"a", ".", "b", "  \r\n", "can't", "$", "tail_\u0301", " ", "😀", "12", ".", "34"};
    check(partition.tokenCount() == expectedTokens.length, "donor builder convenience token count");
    for (int i = 0; i < expectedTokens.length; i++) {
      check(resolver.resolve(partition.tokenIdAt(i)).equals(expectedTokens[i]), "donor builder convenience partition");
    }
    Random random = new Random(SEED);
    String[] atoms = {"", "alpha", " ", "\n", "\r", "β", "中", "😀", "\ud83d", "\ude00", "\u0000", "é", "𐐀"};
    List<Charset> charsets = List.of(StandardCharsets.UTF_8, StandardCharsets.UTF_16,
        StandardCharsets.UTF_16BE, StandardCharsets.UTF_16LE, StandardCharsets.ISO_8859_1,
        StandardCharsets.US_ASCII);
    for (int trial = 0; trial < 180; trial++) {
      M3FileTextBuilder builder = new M3FileTextBuilder(resolver);
      M3FileText left = new M3FileText(resolver);
      M3FileText right = new M3FileText(resolver);
      StringBuilder oracle = new StringBuilder();
      int count = random.nextInt(700);
      long[] ids = new long[count];
      for (int i = 0; i < count; i++) {
        String value = atoms[random.nextInt(atoms.length)];
        ids[i] = resolver.intern(3, value);
        builder.appendId(ids[i]);
        oracle.append(value);
        left = left.concat(new M3FileText(resolver, ids[i]));
      }
      for (int i = count - 1; i >= 0; i--) right = new M3FileText(resolver, ids[i]).concat(right);
      String expected = oracle.toString();
      M3FileText flat = builder.build();
      check(flat.equals(left) && left.equals(right), "composition tree text equality");
      check(flat.hashCode() == expected.hashCode(), "composed Java hash");
      check(flat.metrics().equals(M3TextMetrics.of(expected)), "composed UTF16/UTF8 metrics");
      check(left.precomputedFacts().equals(M3TextFacts.of(expected)), "composed candidate facts");
      check(flat.materialize().equals(expected), "explicit String export");
      check(Arrays.equals(left.tokenIds(), ids), "left balanced ID sequence");
      check(Arrays.equals(right.tokenIds(), ids), "right balanced ID sequence");
      if (count > 0) { long saved = ids[0]; ids[0] = -1L; check(flat.tokenIdAt(0) == saved, "defensive metadata"); }
      for (int i = 0; i < expected.length(); i++) check(left.charAt(i) == expected.charAt(i), "charAt exact UTF16");
      for (Charset charset : charsets) {
        byte[] bytes = expected.getBytes(charset);
        check(Arrays.equals(flat.getBytes(charset), bytes), "bounded charset encoding parity");
        check(left.sha256(charset).equals(hash(bytes)), "bounded charset digest parity");
      }
      for (int range = 0; range < 20; range++) {
        int start = random.nextInt(expected.length() + 1);
        int end = start + random.nextInt(expected.length() - start + 1);
        String slice = expected.substring(start, end);
        M3FileText actual = right.substring(start, end);
        check(actual.materialize().equals(slice), "cut-boundary substring parity");
        check(actual.sha256(StandardCharsets.UTF_8).equals(hash(slice.getBytes(StandardCharsets.UTF_8))), "cut surrogate digest");
      }
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      long written = right.writeUtf8To(Channels.newChannel(bytes));
      check(written == expected.getBytes(StandardCharsets.UTF_8).length, "channel byte count");
      check(Arrays.equals(bytes.toByteArray(), expected.getBytes(StandardCharsets.UTF_8)), "channel exact bytes");
      expect(IndexOutOfBoundsException.class, () -> flat.charAt(-1));
      expect(IndexOutOfBoundsException.class, () -> flat.charAt(flat.length()));
      expect(IndexOutOfBoundsException.class, () -> flat.substring(1, 0));
    }
    // Exercise encoder buffer seams and a surrogate pair whose two halves have different IDs.
    String expected = "x".repeat(8191) + "😀" + "é".repeat(8193);
    M3FileText text = M3FileText.intern(resolver, 1, "x".repeat(8191) + "\ud83d")
        .concat(M3FileText.intern(resolver, 1, "\ude00" + "é".repeat(8193)));
    check(text.sha256(StandardCharsets.UTF_8).equals(hash(expected.getBytes(StandardCharsets.UTF_8))), "encoder refill surrogate seam");
  }

  private static void nativeSearch(boolean nativeRequired) throws Exception {
    Random random = new Random(SEED ^ 0x4b4d50L);
    Method nativeMethod = M3FileNative.class.getDeclaredMethod("nativeMappedFileTokenIndexOf",
        ByteBuffer.class, int.class, int.class, int.class, int[].class, int.class);
    if (nativeRequired) nativeMethod.setAccessible(true);
    for (int width : new int[] {1, 2, 4}) {
      for (int trial = 0; trial < 250; trial++) {
        int count = 1 + random.nextInt(900), base = random.nextInt(12);
        int[] tokens = new int[count];
        ByteBuffer input = ByteBuffer.allocateDirect(base + count * width + 11).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < count; i++) {
          tokens[i] = random.nextInt(9);
          int offset = base + i * width;
          if (width == 1) input.put(offset, (byte) tokens[i]);
          else if (width == 2) input.putShort(offset, (short) tokens[i]);
          else input.putInt(offset, tokens[i]);
        }
        input.limit(base + count * width);
        input.position(random.nextInt(input.limit() + 1));
        ByteBuffer borrowed = input.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        int patternLength = random.nextInt(Math.min(count, 12) + 1);
        int start = random.nextInt(count - patternLength + 1);
        int[] pattern = Arrays.copyOfRange(tokens, start, start + patternLength);
        if (pattern.length > 0 && (trial & 1) == 0) pattern[0] = 10;
        int from = random.nextInt(count + 1);
        int expected = naive(tokens, pattern, from);
        int position = borrowed.position(), limit = borrowed.limit();
        check(M3FileNative.mappedFileTokenIndexOf(borrowed, base, width, count, pattern, from) == expected, "mapped KMP versus naive");
        check(borrowed.position() == position && borrowed.limit() == limit && borrowed.order() == ByteOrder.LITTLE_ENDIAN, "borrowed buffer state");
        if (nativeRequired && pattern.length > 0 && pattern.length <= count - from) {
          int actual = (int) nativeMethod.invoke(null, borrowed, base, width, count, pattern, from);
          check(actual == expected, "required native symbol result versus naive");
        }
        ByteBuffer heap = ByteBuffer.allocate(input.limit());
        heap.put(input.duplicate().position(0));
        check(M3FileNative.mappedFileTokenIndexOf(heap, base, width, count, pattern, from) == expected, "heap fallback versus naive");
      }
    }
    ByteBuffer data = ByteBuffer.allocateDirect(5);
    expect(NullPointerException.class, () -> M3FileNative.mappedFileTokenIndexOf(null, -1, 3, -1, new int[0], -1));
    expect(NullPointerException.class, () -> M3FileNative.mappedFileTokenIndexOf(data, -1, 3, -1, null, -1));
    expect(IllegalArgumentException.class, () -> M3FileNative.mappedFileTokenIndexOf(data, 0, 3, 1, new int[0], 0));
    expect(IllegalArgumentException.class, () -> M3FileNative.mappedFileTokenIndexOf(data, 0, 1, 1, new int[] {-1}, 0));
    expect(IndexOutOfBoundsException.class, () -> M3FileNative.mappedFileTokenIndexOf(data, 1, 4, 2, new int[0], 0));
  }

  private static void images(Path directory) throws Exception {
    String[] vocabulary = {"", "alpha", "β", " ", "\r", "\n", "😀", "+", "12", "tail"};
    M3FileDictionary frozen = M3FileDictionary.frozen(7, "test", "v1", vocabulary);
    M3FileDictionary same = M3FileDictionary.frozen(7, "test", "v1", vocabulary);
    M3FileDictionary different = M3FileDictionary.frozen(7, "test", "v2", vocabulary);
    Random random = new Random(SEED ^ 0x494d414745L);
    Path image = directory.resolve("mapped.midxf");
    for (int trial = 0; trial < 24; trial++) {
      M3FileBuilder builder = new M3FileBuilder(frozen).enableRollingIndex();
      StringBuilder expected = new StringBuilder();
      int count = 1 + random.nextInt(300);
      for (int i = 0; i < count; i++) {
        String word = vocabulary[1 + random.nextInt(vocabulary.length - 1)];
        builder.add(7, word, word.isBlank() ? M3FileTokenKind.WHITESPACE : M3FileTokenKind.WORD);
        expected.append(word);
      }
      M3File file = builder.build();
      M3FileImage.write(file, image);
      byte[] stable = Files.readAllBytes(image);
      try (MappedM3File mapped = MappedM3File.open(image, same)) {
        check(mapped.materialize().contentEquals(expected), "disk reopen with equivalent immutable generation");
        check(mapped.sha256(StandardCharsets.UTF_8).equals(hash(expected.toString().getBytes(StandardCharsets.UTF_8))), "mapped digest");
        snapshot(file.snapshot(), mapped.snapshot());
        check(mapped.mappedBytes() == stable.length, "mapped byte length");
        int[] handles = file.primitiveSnapshot().localHandles();
        for (int k = 0; k < 20; k++) {
          int start = random.nextInt(count);
          int end = Math.min(count, start + random.nextInt(8));
          int[] needle = Arrays.copyOfRange(handles, start, end);
          int from = random.nextInt(count + 1);
          check(mapped.indexOf(needle, from) == naive(handles, needle, from), "mapped file search");
        }
      }
      check(Arrays.equals(stable, Files.readAllBytes(image)), "reopen and close preserve image bytes");
      expect(IOException.class, () -> { try (MappedM3File ignored = MappedM3File.open(image, different)) { check(ignored.tokenCount() < 0, "wrong generation"); } });
    }
    M3File live = M3FileTokenizer.plainText(new M3FileDictionary(), 7, "alpha");
    expect(IllegalArgumentException.class, () -> M3FileImage.write(live, directory.resolve("overlay.midxf")));
    check(!Files.exists(directory.resolve("overlay.midxf")), "overlay refusal before publication");

    byte[] valid = Files.readAllBytes(image);
    Path malformed = directory.resolve("malformed.midxf");
    for (int length : new int[] {0, 1, 7, 16, 47, valid.length - 1}) {
      Files.write(malformed, Arrays.copyOf(valid, length));
      expect(IOException.class, () -> { try (MappedM3File ignored = MappedM3File.open(malformed, same)) { check(ignored.tokenCount() < 0, "truncated image"); } });
    }
    for (int offset : new int[] {0, 8, 12, 16, 20, 24, 28, 32, 40, 48, valid.length - 1}) {
      byte[] corrupted = valid.clone(); corrupted[offset] ^= (byte) 0x80;
      Files.write(malformed, corrupted);
      expect(IOException.class, () -> { try (MappedM3File ignored = MappedM3File.open(malformed, same)) { check(ignored.tokenCount() < 0, "corrupt image"); } });
    }
  }

  private static void io(Path directory) throws Exception {
    String source = ("class β { String text = \"😀\"; // comment\r\n int x=42; /* block */ }\n").repeat(350);
    byte[] raw = source.getBytes(StandardCharsets.UTF_8);
    Path path = directory.resolve("source.java");
    Files.write(path, raw);
    M3FileDictionary dictionary = new M3FileDictionary();
    ByteArrayOutputStream eventBytes = new ByteArrayOutputStream();
    List<String> order = new ArrayList<>();
    M3File physical;
    try (M3FileEventWriter writer = new M3FileEventWriter(eventBytes)) {
      physical = M3FileStreamIndexer.codeLike(path, dictionary, 7, event -> {
        if (event instanceof M3FileStreamEvents.Finished) order.add("finished");
        writer.accept(event);
      }, true, snapshot -> order.add("snapshot"));
    }
    check(order.equals(List.of("snapshot", "finished")), "snapshot precedes terminal event");
    check(physical.hasSourceFacts() && physical.sourceFacts().stableDuringRead(), "physical facts stability");
    check(physical.materialize().equals(source), "strict UTF8 source text");
    check(physical.sourceFacts().contentSha256().equals(hash(raw)), "physical raw-byte digest");
    check(physical.sourceFacts().utf8Length() == raw.length, "physical raw-byte length");
    M3File memory = M3FileTokenizer.codeLike(dictionary, 7, opaque(source), true);
    snapshot(memory.snapshot(), physical.snapshot());
    M3File replay = M3FileEventReader.readM3File(new ByteArrayInputStream(eventBytes.toByteArray()), dictionary, true);
    snapshot(physical.snapshot(), replay.snapshot());
    check(replay.sourceFacts().contentSha256().equals(hash(raw)), "event replay byte digest");
    ByteArrayOutputStream snapshotBytes = new ByteArrayOutputStream();
    M3FileSnapshotCodec.write(physical.snapshot(), snapshotBytes);
    M3FileSnapshot detached = M3FileSnapshotCodec.read(new ByteArrayInputStream(snapshotBytes.toByteArray()));
    snapshot(physical.snapshot(), detached);
    check(detached.tokenRangeSha256(dictionary, 0, physical.tokenCount()).sha256().equals(hash(raw)), "snapshot whole-token digest");
    M3ByteFile bytes = M3ByteFileStreamIndexer.index(path);
    check(bytes.stableDuringRead() && bytes.byteFacts().sha256().equals(hash(raw)), "raw-byte file lane");
    long[] histogram = new long[256]; for (byte value : raw) histogram[value & 255]++;
    check(Arrays.equals(histogram, bytes.byteFactsSnapshot().byteHistogram()), "physical byte histogram");

    Path second = directory.resolve("second.txt"); Files.writeString(second, "tail β\n");
    M3FileUniversePlan plan = M3FileUniversePlan.capture(List.of(second, path));
    check(plan.size() == 2, "universe captured plan");
    ByteArrayOutputStream universeBytes = new ByteArrayOutputStream();
    M3FileUniverseEventWriter.Summary summary = M3FileUniverseEventWriter.codeLike(plan, dictionary, 7, universeBytes);
    M3FileUniverseSnapshot universe = M3FileUniverseEventReader.readSnapshot(new ByteArrayInputStream(universeBytes.toByteArray()), dictionary);
    StringBuilder combined = new StringBuilder();
    for (Path planned : plan.paths()) combined.append(Files.readString(planned));
    check(universe.indexedText().materialize().contentEquals(combined), "universe ordering and text composition");
    check(universe.tokenCount() == summary.tokenCount(), "universe summary tokens");
    check(universe.byteCount() == summary.byteCount(), "universe summary bytes");
    check(universe.sha256(StandardCharsets.UTF_8).equals(hash(combined.toString().getBytes(StandardCharsets.UTF_8))), "universe aggregate digest");
    long beta = dictionary.lookup(7, "β").orElseThrow();
    int occurrences = 0;
    for (int i = 0; i < universe.tokenCount(); i++) {
      M3FileText token = universe.tokenAt(i);
      for (int atom = 0; atom < token.tokenCount(); atom++) if (token.tokenIdAt(atom) == beta) occurrences++;
    }
    check(universe.tokenIndex().occurrenceCount(beta) == occurrences, "universe numeric postings");

    Files.delete(path); Files.delete(second);
    check(detached.indexedText(dictionary).materialize().equals(source), "detached snapshot survives source close/delete");
    check(replay.asM3String().sha256(StandardCharsets.UTF_8).equals(hash(raw)), "replay has no source reopen dependency");

    Path malformed = directory.resolve("invalid-utf8.txt"); Files.write(malformed, new byte[] {(byte) 0xf0, (byte) 0x80, (byte) 0x80});
    AtomicBoolean finished = new AtomicBoolean();
    expect(IOException.class, () -> M3FileStreamIndexer.plainText(malformed, dictionary, 7,
        event -> { if (event instanceof M3FileStreamEvents.Finished) finished.set(true); }));
    check(!finished.get(), "strict malformed UTF8 refusal has no terminal success");
    expect(IOException.class, () -> M3FileStreamIndexer.plainText(directory, dictionary, 7));
  }

  private static void retention() throws Exception {
    M3FileDictionary dictionary = new M3FileDictionary();
    String atom = "canonical String atom ".repeat(32);
    M3FileBuilder builder = new M3FileBuilder(dictionary);
    for (int i = 0; i < 8192; i++) builder.add(7, atom, M3FileTokenKind.WORD);
    M3File file = builder.build();
    check(dictionary.size() == 1 && file.uniqueTokenCount() == 1, "repetition preserves one String atom owner");
    for (Field field : M3FileText.class.getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers())) continue;
      check(field.getType() != String.class && field.getType() != char[].class && field.getType() != byte[].class,
          "descriptor has no text payload field");
    }
    Field root = M3FileText.class.getDeclaredField("root"); root.setAccessible(true);
    long metadataBytes = descriptorPrimitiveBytes(root.get(file.asM3String()));
    check(metadataBytes < file.utf16Length(), "metadata growth independent of repeated atom spelling width");
    System.out.println("M3_FILE_RETAINED_METADATA|logicalUtf16=" + file.utf16Length()
        + "|canonicalStringAtoms=" + dictionary.size() + "|filePrimitiveBytes=" + file.retainedPayloadBytes()
        + "|descriptorPrimitiveArrayBytes=" + metadataBytes
        + "|excludesObjectHeadersAndResolverHeap=1");
  }

  private static long descriptorPrimitiveBytes(Object node) throws Exception {
    long bytes = 0;
    for (Field field : node.getClass().getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers())) continue;
      field.setAccessible(true); Object value = field.get(node);
      check(!(value instanceof String) && !(value instanceof char[]) && !(value instanceof byte[]), "node contains only ID/coordinate metadata");
      if (value instanceof long[] ids) bytes += 8L * ids.length;
      else if (value instanceof int[] starts) bytes += 4L * starts.length;
      else if (value != null && value.getClass().getEnclosingClass() == M3FileText.class) bytes += descriptorPrimitiveBytes(value);
    }
    return bytes;
  }

  private static void snapshot(M3FileSnapshot expected, M3FileSnapshot actual) {
    check(Arrays.equals(expected.lexiconSnapshot().tokenIds(), actual.lexiconSnapshot().tokenIds()), "snapshot lexicon IDs");
    check(Arrays.equals(expected.lexiconSnapshot().tokenStarts(), actual.lexiconSnapshot().tokenStarts()), "snapshot lexicon rows");
    M3FilePrimitiveSnapshot a = expected.primitiveSnapshot(), b = actual.primitiveSnapshot();
    check(Arrays.equals(a.localHandles(), b.localHandles()), "snapshot local handles");
    check(Arrays.equals(a.tokenKinds(), b.tokenKinds()), "snapshot kinds");
    check(Arrays.equals(a.utf16Offsets(), b.utf16Offsets()), "snapshot UTF16 offsets");
    check(Arrays.equals(a.lineStarts(), b.lineStarts()), "snapshot line starts");
    check(Arrays.equals(a.frequencies(), b.frequencies()), "snapshot frequencies");
    check(Arrays.equals(a.firstOccurrences(), b.firstOccurrences()), "snapshot first occurrences");
    check(Arrays.equals(a.lastOccurrences(), b.lastOccurrences()), "snapshot last occurrences");
    check(Arrays.equals(a.postingStarts(), b.postingStarts()), "snapshot CSR boundaries");
    check(Arrays.equals(a.postings(), b.postings()), "snapshot CSR postings");
  }

  private static int naive(int[] text, int[] pattern, int from) {
    for (int at = from; at <= text.length - pattern.length; at++) {
      int matched = 0;
      while (matched < pattern.length && text[at + matched] == pattern[matched]) matched++;
      if (matched == pattern.length) return at;
    }
    return -1;
  }

  private static CharSequence opaque(String value) {
    return new CharSequence() {
      @Override public int length() { return value.length(); }
      @Override public char charAt(int index) { return value.charAt(index); }
      @Override public CharSequence subSequence(int start, int end) { throw new AssertionError("unexpected source slice"); }
      @Override public String toString() { throw new AssertionError("unexpected source flatten"); }
    };
  }

  private static M3Hash256 hash(byte[] bytes) throws Exception {
    return M3Hash256.of(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
  private static void check(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }
  @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
  private static void expect(Class<? extends Throwable> type, CheckedAction action) throws Exception {
    try { action.run(); }
    catch (Throwable failure) {
      if (type.isInstance(failure)) { checks++; return; }
      throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
    }
    throw new AssertionError("expected " + type.getName());
  }
}
