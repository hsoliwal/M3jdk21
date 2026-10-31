// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Frozen donor bytes and independent namespace oracles for the array-backed port. */
final class M3ArrayMetadataContractTest {
  private static M3ContentId id(String value) {
    return M3ContentId.digest(value.getBytes(StandardCharsets.UTF_8));
  }
  @Test void canonicalFormatOneMatchesUntouchedDonorObservations() throws Exception {
    Map<String, byte[]> observed = new HashMap<>();
    try (var stream = Objects.requireNonNull(getClass().getResourceAsStream("metadata-donor-format1.tsv"));
        var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      for (String line : reader.lines().filter(s -> !s.startsWith("#")).toList()) {
        String[] cells = line.split("\t"); byte[] bytes = Base64.getDecoder().decode(cells[2]);
        assertEquals(cells[1], M3ContentId.digest(bytes).hex()); observed.put(cells[0], bytes);
      }
    }
    Map<String, M3ContentId> paths = new LinkedHashMap<>();
    paths.put("/z", id("z")); paths.put("/a/./x", id("x"));
    paths.put("/a-/q", id("q")); paths.put("/a", id("a"));
    paths.put("/é/𐐷", id("unicode")); paths.put("/", id("root"));
    paths.put("/replace/../a/x", id("last"));
    M3GenerationRoot generation = new M3GenerationRoot(42, id("parent"), paths);
    assertArrayEquals(observed.get("generation"), generation.canonicalBytes());
    assertArrayEquals(observed.get("generation-put"), generation.put("/new/child", id("new")).canonicalBytes());
    assertArrayEquals(observed.get("generation-remove"), generation.remove("/a/x").canonicalBytes());
    assertEquals(generation.entries(), M3GenerationRoot.decode(observed.get("generation")).entries());
    M3PrecomputeKey key = new M3PrecomputeKey("regex", "dfa/utf16", 7,
        List.of(id("a"), id("z"), id("a")),
        Map.of("z", "", "é", "𐐷", "a", "(?<name>\\p{L}+)\n"));
    assertArrayEquals(observed.get("key"), key.canonicalBytes());
    assertEquals(key.inputs(), M3PrecomputeKey.decode(observed.get("key")).inputs());
    M3ObjectRef raw = new M3ObjectRef(id("raw"), 12345678901L, M3ObjectKind.RAW_BYTES);
    M3FileManifest manifest = new M3FileManifest(raw,
        Map.of("z", new M3ObjectRef(id("s1"), 9, M3ObjectKind.MINDEX_FILE_IMAGE),
            "é", new M3ObjectRef(id("s2"), 0, M3ObjectKind.MINDEX_SOURCE_FACTS_IMAGE)),
        Map.of("a", new M3ObjectRef(id("relation"), 42, M3ObjectKind.PRECOMPUTE_OUTPUT)),
        Map.of(id("a"), id("descriptor1"), id("z"), id("descriptor2")));
    assertArrayEquals(observed.get("manifest"), manifest.canonicalBytes());
    assertArrayEquals(observed.get("manifest-updated"), manifest.withStructure("a",
        new M3ObjectRef(id("s3"), 27, M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR))
        .withPrecompute(id("a"), id("updated")).canonicalBytes());
    assertEquals(manifest.structures(), M3FileManifest.decode(observed.get("manifest")).structures());
    assertEquals(manifest.precomputes(), M3FileManifest.decode(observed.get("manifest")).precomputes());
    assertEquals(6, observed.size());
  }

  @Test void pagedNamespaceMatchesIndependentChildGroupingWithPunctuationAndUnicode() {
    String[] names = {"a", "a-", "a.", "a0", "z", "é", "𐐷", "\uffff"};
    for (int seed = 0; seed < 64; seed++) {
      Map<String, M3ContentId> entries = new TreeMap<>(); entries.put("/", id("root"));
      Random random = new Random(seed);
      for (int i = 0; i < 80; i++) {
        String path = "/" + names[random.nextInt(names.length)];
        if (random.nextBoolean()) path += "/" + names[random.nextInt(names.length)];
        if (random.nextBoolean()) path += "/child";
        entries.put(path, id("row" + i));
      }
      M3GenerationRoot root = new M3GenerationRoot(seed, null, entries);
      for (String directory : new String[]{"/", "/a", "/a-", "/é", "/absent"}) {
        String prefix = directory.equals("/") ? "/" : directory + "/";
        Map<String, M3ContentId> files = new TreeMap<>(); Set<String> descendants = new TreeSet<>();
        for (var entry : entries.entrySet()) {
          if (!entry.getKey().startsWith(prefix) || entry.getKey().length() == prefix.length()) continue;
          String relative = entry.getKey().substring(prefix.length()); int slash = relative.indexOf('/');
          if (slash < 0) files.put(relative, entry.getValue()); else descendants.add(relative.substring(0, slash));
        }
        Set<String> expectedNames = new TreeSet<>(files.keySet()); expectedNames.addAll(descendants);
        for (int limit : new int[]{1, 2, 3, 9}) {
          List<String> actual = new ArrayList<>(); String cursor = null;
          do {
            M3GenerationRoot.DirectoryPage page = root.directoryPage(directory, cursor, limit);
            assertTrue(page.entries().size() <= limit);
            for (var entry : page.entries()) {
              actual.add(entry.name()); assertEquals(prefix + entry.name(), entry.path());
              assertEquals(Optional.ofNullable(files.get(entry.name())), entry.manifestId());
              assertEquals(descendants.contains(entry.name()), entry.hasDescendants());
            }
            String next = page.nextAfter().orElse(null);
            if (next != null) assertTrue(cursor == null || next.compareTo(cursor) > 0);
            cursor = next;
          } while (cursor != null);
          assertEquals(new ArrayList<>(expectedNames), actual, "seed=" + seed + " directory=" + directory);
        }
      }
    }
  }

  @Test void retainedMetadataUsesColumnsAndPublicProjectionsAreImmutable() throws Exception {
    for (Class<?> owner : List.of(M3IdTable.class, M3IdPairs.class, M3RefTable.class,
        M3GenerationRoot.class, M3PrecomputeKey.class, M3FileManifest.class,
        M3FsPrecomputeCatalog.class, M3FsPrecomputeCatalog.PipelineManifest.class)) {
      for (var field : owner.getDeclaredFields()) {
        if (field.isSynthetic()) continue;
        Class<?> type = field.getType();
        assertFalse(Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type), field.toString());
        assertNotEquals(M3ContentId.class, type, "aggregate identities must also be raw columns: " + field);
        if (type.isArray()) assertFalse(type.getComponentType().isRecord(), field.toString());
      }
    }
    M3GenerationRoot root = new M3GenerationRoot(1, null, Map.of("/a", id("a")));
    assertThrows(UnsupportedOperationException.class, () -> root.entries().put("/b", id("b")));
    assertThrows(UnsupportedOperationException.class, () -> root.entries().entrySet().iterator().next().setValue(id("b")));
    byte[] encoded = root.canonicalBytes(); encoded[0] = 99;
    assertEquals(root.id(), M3GenerationRoot.decode(root.canonicalBytes()).id());
    M3PrecomputeKey key = new M3PrecomputeKey("a", "b", 0, List.of(id("a")), Map.of("a", "b"));
    assertThrows(UnsupportedOperationException.class, () -> key.inputs().clear());
    assertThrows(UnsupportedOperationException.class, () -> key.configuration().clear());
    assertNotSame(key.inputs().get(0), key.inputs().get(0));
  }

  @Test void malformedAndNoncanonicalMetadataFailBeforePublishingRows() throws Exception {
    M3GenerationRoot root = new M3GenerationRoot(0, null, Map.of("/a", id("a")));
    byte[] generation = root.canonicalBytes();
    assertThrows(IOException.class, () -> M3GenerationRoot.decode(Arrays.copyOf(generation, generation.length - 1)));
    assertThrows(IOException.class, () -> M3GenerationRoot.decode(Arrays.copyOf(generation, generation.length + 1)));
    byte[] huge = generation.clone(); java.nio.ByteBuffer.wrap(huge).putInt(13, Integer.MAX_VALUE);
    assertThrows(IOException.class, () -> M3GenerationRoot.decode(huge));
    M3PrecomputeKey key = new M3PrecomputeKey("a", "b", 0, List.of(), Map.of("a", ""));
    byte[] broken = key.canonicalBytes(); broken[8] = (byte) 0xff;
    assertThrows(IOException.class, () -> M3PrecomputeKey.decode(broken));
    M3FileManifest manifest = new M3FileManifest(new M3ObjectRef(id("raw"), 1, M3ObjectKind.RAW_BYTES),
        Map.of(), Map.of(), Map.of());
    byte[] invalidKind = manifest.canonicalBytes(); java.nio.ByteBuffer.wrap(invalidKind).putInt(44, 99);
    assertThrows(IOException.class, () -> M3FileManifest.decode(invalidKind));
  }

  @Test void generationInputValidationPrecedesCounterOverflowAsInTheDonor() {
    assertThrows(IllegalArgumentException.class, () -> new M3GenerationRoot(-1, null, null));
    M3GenerationRoot full = new M3GenerationRoot(Long.MAX_VALUE, null, Map.of());
    assertThrows(IllegalArgumentException.class, () -> full.put("relative", id("a")));
    assertThrows(NullPointerException.class, () -> full.put("/a", null));
    assertThrows(ArithmeticException.class, () -> full.put("/a", id("a")));
    assertSame(full, full.remove("/absent"));
  }
}
