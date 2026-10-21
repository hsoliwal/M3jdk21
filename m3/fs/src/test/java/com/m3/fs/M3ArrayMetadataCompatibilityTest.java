// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3ArrayMetadataCompatibilityTest {
  @TempDir Path temp;

  @Test void directoryColumnsDecodeWithoutRetainedEntryObjectsAndExposeEqualApiValues() {
    byte[] first = row("a".getBytes(StandardCharsets.UTF_8), 1, 41);
    byte[] second = row("😀".getBytes(StandardCharsets.UTF_8), 2, 42);
    ByteBuffer input = ByteBuffer.allocate(first.length + second.length + 4).putInt(99).put(first).put(second).flip().position(4);
    M3DirectoryTable table = M3DirectoryTable.decode(input);
    assertEquals(4, input.position()); assertEquals(2, table.size());
    assertEquals("a", table.name(0)); assertEquals("😀", table.name(1));
    assertEquals(41, table.inode(0)); assertEquals(3, table.fileSize(0));
    assertEquals(M3FS.Kind.DIRECTORY, table.kind(1));
    assertEquals(table.entry(0), table.entry(0)); assertNotSame(table.entry(0), table.entry(0));
    assertThrows(UnsupportedOperationException.class, () -> table.entries().clear());
    assertEquals(table, M3DirectoryTable.decode(input));
    for (var field : M3DirectoryTable.class.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) assertTrue(field.getType().isArray(), field.toString());
    }
  }

  @Test void strictNativeUtf8ChecksDoNotConstrainApiOnlyUtf16Names() {
    for (byte[] invalid : List.of(new byte[] {(byte) 0xc0, (byte) 0xaf},
        new byte[] {(byte) 0xed, (byte) 0xa0, (byte) 0x80}, new byte[] {0}, new byte[] {'/'})) {
      assertThrows(IllegalStateException.class, () -> M3DirectoryTable.decode(ByteBuffer.wrap(row(invalid, 1, 1))));
    }
    byte[] valid = row(new byte[] {'x'}, 1, 1);
    assertThrows(IllegalStateException.class,
        () -> M3DirectoryTable.decode(ByteBuffer.wrap(Arrays.copyOf(valid, valid.length - 1))));
    String name = "a\ud800" + "b".repeat(300);
    M3FS.DirEntry entry = new M3FS.DirEntry(name, new M3FS.Stat(1, 0, 1, 0, M3FS.Kind.FILE, 0644, 1, 65536));
    M3FS.DirectoryPage page = new M3FS.DirectoryPage(List.of(entry), false);
    assertEquals(name, page.nextAfter()); assertEquals(List.of(entry), page.entries());
    assertEquals(page, new M3FS.DirectoryPage(List.of(entry), false));
    assertThrows(UnsupportedOperationException.class, () -> new M3FS.DirectoryPage(List.of(), false).entries().clear());
  }

  @Test void mappedOwnerRetainsRawIdentityAndProjectsReferencesOnlyAtApiAccess() throws Exception {
    byte[] content = {1, 2, 3, 4, 5}; Path path = temp.resolve("object"); Files.write(path, content);
    M3ContentId id = M3ContentId.digest(content); byte[] ids = new byte[80]; System.arraycopy(id.bytes(), 0, ids, 17, 32);
    M3MappedObject mapped = M3MappedObject.openColumns(path, ids, 17, content.length, M3ObjectKind.RAW_BYTES.ordinal(), 5);
    M3ObjectRef ref = new M3ObjectRef(id, content.length, M3ObjectKind.RAW_BYTES);
    assertEquals(ref, mapped.reference()); assertNotSame(mapped.reference(), mapped.reference());
    assertTrue(mapped.matchesReference(ids, 17, 5, M3ObjectKind.RAW_BYTES.ordinal()));
    ids[17] ^= 1; assertEquals(ref, mapped.reference());
    assertFalse(mapped.matchesReference(ids, 17, 5, M3ObjectKind.RAW_BYTES.ordinal()));
    assertThrows(IndexOutOfBoundsException.class, () -> mapped.matchesReference(ids, 49, 5, 0));
    assertThrows(IllegalArgumentException.class, () -> mapped.matchesReference(ids, 0, -1, 0));
    assertThrows(IOException.class, () -> M3MappedObject.open(path, ref, 4));
    for (var field : M3MappedObject.class.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        assertTrue(field.getType().isPrimitive() || field.getType() == byte[].class
            || field.getType() == ByteBuffer.class, field.toString());
      }
    }
    Files.write(path, new byte[] {9, 2, 3, 4, 5});
    assertThrows(IOException.class, () -> M3MappedObject.open(path, ref, 5));
    Path empty = temp.resolve("empty"); Files.write(empty, new byte[0]);
    M3ObjectRef emptyRef = new M3ObjectRef(M3ContentId.digest(new byte[0]), 0, M3ObjectKind.RAW_BYTES);
    assertEquals(emptyRef, M3MappedObject.open(empty, emptyRef, 0).reference());
  }

  @Test void persistedIndexImageOwnsExactPrimitiveCoordinatesAndUtf16Units() {
    int[] starts = {0, 2, 2}, units = {0, 2, 2};
    long[] ids = {Long.MIN_VALUE | 19, -1L}; char[] text = {'A', '\ud800'};
    M3FsIndexImage image = M3FsIndexImage.of(starts, ids, units, text);
    starts[1] = 0; ids[0] = 0; text[0] = 'Z';
    assertEquals("A\ud800", image.value(0).toString()); assertEquals("", image.value(1).toString());
    ByteBuffer bytes = image.image(); int tokenAt = bytes.getInt(32);
    assertEquals(Long.MIN_VALUE | 19, bytes.getLong(tokenAt)); assertEquals(-1L, bytes.getLong(tokenAt + 8));
    assertTrue(image.image().isReadOnly()); assertTrue(image.image().isDirect());
    bytes.position(bytes.limit()); assertEquals(0, image.image().position());
    for (var field : M3FsIndexImage.class.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) assertTrue(field.getType().isPrimitive()
          || field.getType() == ByteBuffer.class, field.toString());
    }
    assertThrows(IllegalArgumentException.class, () -> M3FsIndexImage.of(new int[] {1}, new long[0], new int[] {0}, new char[0]));
    assertEquals(0, M3FsIndexImage.of().valueCount());
  }

  @Test void snapshotNamespaceAndPrecomputeResultsRetainOnlyIdentityColumns() {
    for (Class<?> type : List.of(M3PrecomputeResult.class, M3SnapshotStore.class)) {
      for (var field : type.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers())) continue;
        assertNotEquals(M3ContentId.class, field.getType(), field.toString());
        assertNotEquals(M3ObjectRef.class, field.getType(), field.toString());
        assertNotEquals(java.util.UUID.class, field.getType(), field.toString());
      }
    }
  }

  private static byte[] row(byte[] name, int kind, long inode) {
    return ByteBuffer.allocate(52 + name.length).order(ByteOrder.LITTLE_ENDIAN)
        .putInt(name.length).put(name).putInt(kind).putInt(0644).putInt(1).putInt(65536)
        .putLong(inode).putLong(3).putLong(7).putLong(11).array();
  }
}
