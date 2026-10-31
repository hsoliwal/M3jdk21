// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Canonical donor observations plus array metadata, mapping identity and malformed-input gates. */
final class M3PrecomputeArrayMetadataTest {
  @TempDir Path temp;

  @Test void canonicalBytesMatchFourUntouchedDonorObservations() throws Exception {
    Map<String,byte[]> actual = canonicalFixtures();
    try (var input = Objects.requireNonNull(getClass().getResourceAsStream("precompute-donor-format1.tsv"));
        var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      int count = 0;
      for (String line : reader.lines().filter(value -> !value.startsWith("#")).toList()) {
        String[] fields = line.split("\t"); byte[] expected = Base64.getDecoder().decode(fields[2]);
        assertEquals(fields[1], M3ContentId.digest(expected).hex());
        assertArrayEquals(expected, actual.get(fields[0]), fields[0]); count++;
      }
      assertEquals(4, count);
    }
    M3FsPrecomputeBundle bundle = M3FsPrecomputeBundle.decode(actual.get("bundle"));
    assertEquals(List.of("alpha", "z", "😀", "\uE000"), bundle.lanes().stream().map(M3FsPrecomputeBundle.Lane::name).toList());
    assertEquals(27, bundle.totalBytes());
    assertNotSame(bundle.lanes().get(0), bundle.lanes().get(0));
    assertNotSame(bundle.lanes().get(0).object(), bundle.lanes().get(0).object());
    assertNotSame(bundle.keyId(), bundle.keyId());
    assertEquals(bundle.lanes(), M3FsPrecomputeBundle.decode(bundle.canonicalBytes()).lanes());
    assertThrows(UnsupportedOperationException.class, () -> bundle.lanes().removeIf(row -> false));
    assertThrows(UnsupportedOperationException.class, () -> bundle.lanes().subList(0,1).clear());
    var pipeline = M3FsPrecomputeCatalog.PipelineManifest.decode(actual.get("pipeline"));
    assertEquals(pipeline.entries(), pipeline.entries());
    assertEquals(pipeline.entries().get(0).hashCode(), pipeline.entries().get(0).hashCode());
    assertNotSame(pipeline.entries().get(0).key(), pipeline.entries().get(0).key());
  }

  @Test void retainedPrecomputeStateContainsArraysWithoutEntryRows() throws Exception {
    for (String name : List.of("M3FsPrecomputeBundle", "M3FsPrecomputeBundle$LaneTable",
        "M3FsPrecomputeBundle$MappedView", "M3FsPrecomputeBundle$MappedLaneList",
        "M3FsPrecomputeWriter", "M3PrecomputeCache$IdIndex", "M3PrecomputeCache$MappedBundleList",
        "M3DirectoryContentStore$PrecomputeLaneTable", "M3DirectoryContentStore$PrecomputeProfile")) {
      Class<?> type = Class.forName("com.m3.fs." + name);
      for (var field : type.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
        Class<?> stored = field.getType();
        assertFalse(Collection.class.isAssignableFrom(stored) || Map.class.isAssignableFrom(stored), field.toString());
        assertFalse(stored == M3ObjectRef.class || stored == M3ContentId.class
            || stored == M3FsPrecomputeBundle.Lane.class || stored == M3DirectoryContentStore.PrecomputeLane.class, field.toString());
        if (stored.isArray()) assertFalse(stored.getComponentType().isRecord(), field.toString());
      }
    }
  }

  @Test void writerDetachesPublishedBundleAndDirectoryRowsAreLazy() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("detach"));
    M3PrecomputeKey key = new M3PrecomputeKey("hash","array",1,List.of(),Map.of());
    M3FsPrecomputeWriter writer = new M3FsPrecomputeWriter(store,key,"hash-signatures-v1")
        .addLongs("first",M3FsPrecomputeBundle.Role.HASH64_I64,new long[]{1});
    M3FsPrecomputeBundle first = writer.bundle(); byte[] frozen = first.canonicalBytes();
    writer.addLongs("second",M3FsPrecomputeBundle.Role.HASH64_I64,new long[]{2});
    assertEquals(1, first.lanes().size()); assertEquals(2,writer.bundle().lanes().size());
    assertArrayEquals(frozen,first.canonicalBytes());
    byte[] directoryBytes=canonicalFixtures().get("directory");
    M3ObjectRef descriptor=store.put(directoryBytes,M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR);
    var directory=store.readPrecomputeBundle(descriptor,4096);
    assertNotSame(directory.lanes().get(0),directory.lanes().get(0));
    assertEquals(directory.lanes(),store.readPrecomputeBundle(descriptor,4096).lanes());
    assertThrows(UnsupportedOperationException.class,()->directory.lanes().clear());
  }

  @Test void rawMappingRehashesDeduplicatesAndPreservesInputOrder() throws Exception {
    M3DirectoryContentStore store=new M3DirectoryContentStore(temp.resolve("mapped"));
    M3PrecomputeCache cache=new M3PrecomputeCache(store); M3PrecomputeKey[] keys=new M3PrecomputeKey[3];
    for(int owner=0;owner<3;owner++) {
      keys[owner]=new M3PrecomputeKey("hash","rehash",1,List.of(),Map.of("owner",Integer.toString(owner)));
      M3FsPrecomputeWriter writer=new M3FsPrecomputeWriter(store,keys[owner],"hash-signatures-v1");
      for(int lane=0;lane<16;lane++) writer.addLongs(String.format(Locale.ROOT,"lane-%02d",lane),M3FsPrecomputeBundle.Role.HASH64_I64,new long[]{owner*8L+lane});
      writer.addLongs("alias",M3FsPrecomputeBundle.Role.HASH64_I64,new long[]{owner*8L}); writer.publish(cache);
    }
    byte[] raw=new byte[96]; keys[2].copyIdTo(raw,0); keys[0].copyIdTo(raw,32); keys[1].copyIdTo(raw,64);
    var set=cache.lookupMappedBundleIds(raw,8,256).orElseThrow();
    assertEquals(256,set.uniqueMappedBytes()); assertEquals(3,set.bundleCount());
    assertEquals(List.of(keys[2].id(),keys[0].id(),keys[1].id()),set.bundles().stream().map(M3PrecomputeCache.MappedBundle::keyId).toList());
    var first=set.bundles().get(0); assertSame(first.requireLane("alias").mapped(),first.requireLane("lane-00").mapped());
    assertSame(set.bundles().get(1).requireLane("lane-08").mapped(),set.bundles().get(2).requireLane("lane-00").mapped());
    assertNotSame(set.bundles().get(0),set.bundles().get(0));
    assertEquals(set.bundles().get(0),set.bundles().get(0));
    assertThrows(IOException.class,()->cache.lookupMappedBundleIds(raw,8,255));
    byte[] duplicate=new byte[64]; keys[0].copyIdTo(duplicate,0); keys[0].copyIdTo(duplicate,32);
    assertThrows(IllegalArgumentException.class,()->cache.lookupMappedBundleIds(duplicate,8,256));
    assertThrows(UnsupportedOperationException.class,()->set.bundles().removeIf(row->false));
  }

  @Test void strictDecodersRejectMalformedTextAndOverflow() throws Exception {
    byte[] bundle=canonicalFixtures().get("bundle"); byte[] invalid=bundle.clone(); invalid[40]=(byte)0xff;
    assertThrows(IOException.class,()->M3FsPrecomputeBundle.decode(invalid));
    byte[] noncanonical=bundle.clone(); noncanonical[40]=' ';
    assertThrows(IOException.class,()->M3FsPrecomputeBundle.decode(noncanonical));
    M3DirectoryContentStore store=new M3DirectoryContentStore(temp.resolve("invalid"));
    byte[] directory=canonicalFixtures().get("directory"); directory[0]=(byte)0xff;
    var invalidUtf8=store.put(directory,M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR);
    assertThrows(IOException.class,()->store.readPrecomputeBundle(invalidUtf8,4096));
    String root=M3ContentId.digest(new byte[0]).hex();
    String overflow="M3PRECOMPUTE1\t"+root+"\t"+root+"\nUTF16_CORPUS\tUTF16_BE\t9223372036854775807\t"+root+"\t0\tPRECOMPUTE_OUTPUT\n";
    var bad=store.put(overflow.getBytes(StandardCharsets.UTF_8),M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR);
    assertThrows(IOException.class,()->store.readPrecomputeBundle(bad,4096));
  }
  private static Map<String,byte[]> canonicalFixtures() throws Exception {
    Map<String,byte[]> outputs = new LinkedHashMap<>();
    M3ContentId a = M3ContentId.digest(new byte[]{1,2,3});
    M3ContentId b = M3ContentId.digest(new byte[]{4,5});
    M3ContentId c = M3ContentId.digest(new byte[]{6});
    M3ContentId empty = M3ContentId.digest(new byte[0]);
    M3PrecomputeKey key = new M3PrecomputeKey("search","algorithm",3,List.of(a,b,a),Map.of("z","last","a","first","empty",""));
    M3FsPrecomputeBundle bundle = new M3FsPrecomputeBundle(key.id(),"gpu-grep-segments-v1",List.of(
      new M3FsPrecomputeBundle.Lane("z",M3FsPrecomputeBundle.Role.CANDIDATE_BYTES,3,new M3ObjectRef(a,3,M3ObjectKind.PRECOMPUTE_OUTPUT)),
      new M3FsPrecomputeBundle.Lane("\uE000",M3FsPrecomputeBundle.Role.RECORD_LENGTHS_I32,2,new M3ObjectRef(c,8,M3ObjectKind.PRECOMPUTE_OUTPUT)),
      new M3FsPrecomputeBundle.Lane("\uD83D\uDE00",M3FsPrecomputeBundle.Role.TOKEN_IDS_I64,2,new M3ObjectRef(b,16,M3ObjectKind.PRECOMPUTE_OUTPUT)),
      new M3FsPrecomputeBundle.Lane("alpha",M3FsPrecomputeBundle.Role.UTF16_CODE_UNITS_I16,0,new M3ObjectRef(empty,0,M3ObjectKind.PRECOMPUTE_OUTPUT))));
    outputs.put("bundle",bundle.canonicalBytes());
    M3PrecomputeKey segments = new M3PrecomputeKey("search","segments",1,List.of(a),Map.of("encoding","utf8"));
    M3PrecomputeKey machine = new M3PrecomputeKey("search","machine",2,List.of(b),Map.of("alphabet","256"));
    M3PrecomputeKey hash = new M3PrecomputeKey("search","hash",1,List.of(c),Map.of());
    var pipeline = new M3FsPrecomputeCatalog.PipelineManifest("gpu-prepared-byte-search-v1",List.of(
      new M3FsPrecomputeCatalog.PipelineEntry("hash-signatures-v1",hash),
      new M3FsPrecomputeCatalog.PipelineEntry("gpu-prepared-byte-machine-v1",machine),
      new M3FsPrecomputeCatalog.PipelineEntry("gpu-prepared-byte-segments-v1",segments)));
    outputs.put("pipeline",pipeline.canonicalBytes());
    var directory = new M3DirectoryContentStore.PrecomputeBundle(a.hex(),b.hex(),List.of(
      new M3DirectoryContentStore.PrecomputeLane(M3DirectoryContentStore.PrecomputeRole.POSTING_ROWS,M3DirectoryContentStore.LaneEncoding.INT32_BE,3,new M3ObjectRef(b,12,M3ObjectKind.PRECOMPUTE_OUTPUT)),
      new M3DirectoryContentStore.PrecomputeLane(M3DirectoryContentStore.PrecomputeRole.POSTING_OFFSETS,M3DirectoryContentStore.LaneEncoding.INT32_LE,2,new M3ObjectRef(a,8,M3ObjectKind.PRECOMPUTE_OUTPUT))));
    var encoder = M3DirectoryContentStore.class.getDeclaredMethod("encodePrecomputeBundle",M3DirectoryContentStore.PrecomputeBundle.class);
    encoder.setAccessible(true);
    outputs.put("directory",((String)encoder.invoke(null,directory)).getBytes(StandardCharsets.UTF_8));
    StringBuilder catalog = new StringBuilder();
    for(var p:M3FsPrecomputeCatalog.profiles()) {
      catalog.append(p.id()).append('|').append(p.retention()).append('|');
      p.requiredRoles().stream().map(Enum::name).sorted().forEach(v -> catalog.append(v).append(','));
      catalog.append('|');p.optionalRoles().stream().map(Enum::name).sorted().forEach(v -> catalog.append(v).append(','));
      catalog.append('|').append(String.join(",",p.acceleratorOperations())).append('\n');
    }
    for(var p:M3FsPrecomputeCatalog.pipelines()) catalog.append(p.id()).append('|').append(String.join(",",p.requiredProfiles())).append('|').append(String.join(",",p.optionalProfiles())).append('|').append(String.join(",",p.runtimeOnlyState())).append('\n');
    outputs.put("catalog",catalog.toString().getBytes(StandardCharsets.UTF_8));
    return outputs;
  }
}
