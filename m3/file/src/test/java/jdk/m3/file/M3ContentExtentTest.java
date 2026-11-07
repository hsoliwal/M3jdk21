// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-storage-sdk/src/test/java/com/synexia/storage/ContentExtentTest.java blob 2033fdc54c42; recipe m3/file/recipe/durable-extent-store-20261107/relocate.py v1
class M3ContentExtentTest {
  private static final String SOURCE = "0".repeat(64);

  @Test void canonicalIdentityIsStableAndPlacementIndependent() {
    byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
    M3ContentExtent first = M3ContentExtent.fromBytes("source", SOURCE, 7, data, "text");
    M3ContentExtent second = new M3ContentExtent("source", SOURCE, 7, 10,
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", "text");
    assertEquals(first, second);
    assertEquals(first.extentId(), second.extentId());
    assertEquals(64, first.extentId().length());
    assertEquals(M3ContentExtent.CONTENT_EXTENT_SCHEMA, first.schema());
    assertEquals(first.extentId(), first.receipt().get("extent_id"));
    assertEquals("VERIFIED", M3ContentExtent.validateContent(first, data).get("status"));
    assertNotEquals(first.extentId(), M3ContentExtent.fromBytes("source", SOURCE, 8, data, "text").extentId());
  }

  @Test void manifestIsDeterministicAndRejectsDuplicates() {
    M3ContentExtent a = M3ContentExtent.fromBytes("a", SOURCE, 0, new byte[]{1}, "bytes");
    M3ContentExtent b = M3ContentExtent.fromBytes("b", SOURCE, 1, new byte[]{2}, "bytes");
    var ordered = M3ContentExtent.buildManifest(List.of(a, b));
    var reversed = M3ContentExtent.buildManifest(List.of(b, a));
    assertEquals(ordered.manifestRoot(), reversed.manifestRoot());
    assertEquals(List.of(a.extentId(), b.extentId()), ordered.orderedExtentIds());
    assertThrows(IllegalArgumentException.class, () -> M3ContentExtent.buildManifest(List.of(a, a)));
  }

  @Test void validationFailsClosedForTamperingAndInvalidMetadata() {
    M3ContentExtent extent = M3ContentExtent.fromBytes("src", SOURCE, 0, new byte[]{1, 2}, "raw");
    assertThrows(M3ContentExtent.ContentTamperedException.class,
        () -> M3ContentExtent.validateContent(extent, new byte[]{1}));
    assertThrows(M3ContentExtent.ContentTamperedException.class,
        () -> M3ContentExtent.validateContent(extent, new byte[]{1, 3}));
    assertThrows(IllegalArgumentException.class,
        () -> new M3ContentExtent("", SOURCE, 0, 0, SOURCE, "raw"));
    assertThrows(IllegalArgumentException.class,
        () -> new M3ContentExtent("source", SOURCE.toUpperCase().replace('0', 'A'), 0, 0, SOURCE, "raw"));
    assertThrows(IllegalArgumentException.class,
        () -> new M3ContentExtent("source", SOURCE, -1, 0, SOURCE, "raw"));
    assertThrows(IllegalArgumentException.class,
        () -> new M3ContentExtent("source", SOURCE, 4, 3, SOURCE, "raw"));
    assertThrows(IllegalArgumentException.class,
        () -> M3ContentExtent.fromBytes("source", SOURCE, Long.MAX_VALUE, new byte[]{1}, "raw"));
  }

  @Test void emptyAndControlCharacterPayloadsHaveDeterministicIds() {
    byte[] empty = new byte[0];
    M3ContentExtent extent = M3ContentExtent.fromBytes("with\nquote\"", SOURCE, 0, empty, "zero");
    assertEquals(0L, extent.byteLength());
    assertEquals(M3ContentExtent.sha256(empty), extent.contentSha256());
    assertEquals(extent.extentId(), M3ContentExtent.fromBytes("with\nquote\"", SOURCE, 0, empty, "zero").extentId());
    assertEquals(64, M3ContentExtent.contentSourceId(SOURCE, 0).length());
  }
}
