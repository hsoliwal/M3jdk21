// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Independent public List/record oracles; also compiles against the sealed before sources. */
final class M3DirectoryPageIdentityTest {
  @TempDir Path temporary;

  @Test void emptyPagesAndOtherTypesKeepTheirValueContracts() {
    M3FS.DirectoryPage page = checkedPage(List.of(), false);
    assertNull(page.nextAfter());
    assertFalse(page.equals(null));
    assertFalse(page.equals(List.of()));
    assertFalse(page.equals(page.table()));
    assertEquals(page, page);
    assertThrows(IllegalArgumentException.class, () -> new M3FS.DirectoryPage(List.of(), true));
  }

  @Test void kindsNamesAndScalarExtremaMatchActualRecordHashes() {
    List<String> names = List.of("a", "Aa", "BB", "\u00e9", "e\u0301", "\u4e2d",
        "\ud83d\ude00", "\ud800", "\udc00", "x\ud800y\udc00z", "\uffff",
        "a\ud800" + "b".repeat(300), "wide-" + "\uffff".repeat(2048));
    for (M3FS.Kind kind : M3FS.Kind.values()) {
      List<M3FS.Stat> stats = List.of(
          new M3FS.Stat(1, 0, 1, 0, kind, 0, 0, 1),
          new M3FS.Stat(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
              kind, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE),
          new M3FS.Stat(0x100000000L, 0x7fffffff00000001L, 0x100000001L, 0x200000003L,
              kind, 0x40000000, 0x70000001, 0x40000001));
      for (String name : names) {
        for (M3FS.Stat stat : stats) {
          List<M3FS.DirEntry> entries = List.of(new M3FS.DirEntry(name, stat));
          M3FS.DirectoryPage complete = checkedPage(entries, false);
          M3FS.DirectoryPage continued = checkedPage(entries, true);
          assertNotEquals(complete, continued);
        }
      }
    }
  }

  @Test void everyAdmissibleUtf16UnitParticipatesWithoutNativeNameLimits() {
    StringBuilder name = new StringBuilder(65_534);
    for (int unit = 1; unit <= Character.MAX_VALUE; unit++) {
      if (unit != '/') name.append((char) unit);
    }
    assertEquals(65_534, name.length());
    for (M3FS.Kind kind : M3FS.Kind.values()) {
      checkedPage(List.of(new M3FS.DirEntry(name.toString(),
          new M3FS.Stat(7, 9, 11, 13, kind, 15, 17, 19))), false);
    }
  }

  @Test void everyStatComponentAndRowBoundaryParticipatesInEquality() {
    M3FS.Stat base = new M3FS.Stat(11, 12, 13, 14, M3FS.Kind.FILE, 15, 16, 17);
    List<M3FS.Stat> variants = List.of(
        new M3FS.Stat(111, 12, 13, 14, M3FS.Kind.FILE, 15, 16, 17),
        new M3FS.Stat(11, 112, 13, 14, M3FS.Kind.FILE, 15, 16, 17),
        new M3FS.Stat(11, 12, 113, 14, M3FS.Kind.FILE, 15, 16, 17),
        new M3FS.Stat(11, 12, 13, 114, M3FS.Kind.FILE, 15, 16, 17),
        new M3FS.Stat(11, 12, 13, 14, M3FS.Kind.DIRECTORY, 15, 16, 17),
        new M3FS.Stat(11, 12, 13, 14, M3FS.Kind.FILE, 115, 16, 17),
        new M3FS.Stat(11, 12, 13, 14, M3FS.Kind.FILE, 15, 116, 17),
        new M3FS.Stat(11, 12, 13, 14, M3FS.Kind.FILE, 15, 16, 117));
    M3FS.DirectoryPage original = checkedPage(List.of(new M3FS.DirEntry("base", base)), false);
    for (M3FS.Stat variant : variants) {
      M3FS.DirectoryPage changed = checkedPage(List.of(new M3FS.DirEntry("base", variant)), false);
      assertNotEquals(original, changed);
      assertNotEquals(changed, original);
    }
    M3FS.DirectoryPage left = checkedPage(List.of(
        new M3FS.DirEntry("ab", base), new M3FS.DirEntry("c", base)), false);
    M3FS.DirectoryPage right = checkedPage(List.of(
        new M3FS.DirEntry("a", base), new M3FS.DirEntry("bc", base)), false);
    assertNotEquals(left, right, "equal concatenated characters have different row boundaries");
  }

  @Test void collisionsOrderingAndDuplicateOccurrencesKeepListSemantics() {
    M3FS.Stat stat = new M3FS.Stat(1, 2, 3, 4, M3FS.Kind.SYMLINK, 5, 6, 7);
    M3FS.DirEntry aa = new M3FS.DirEntry("Aa", stat);
    M3FS.DirEntry bb = new M3FS.DirEntry("BB", stat);
    assertEquals(aa.hashCode(), bb.hashCode(), "known String hash collision");
    M3FS.DirectoryPage a = checkedPage(List.of(aa), false);
    M3FS.DirectoryPage b = checkedPage(List.of(bb), false);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, b);
    M3FS.DirectoryPage repeated = checkedPage(List.of(aa, bb, aa), true);
    M3FS.DirectoryPage reordered = checkedPage(List.of(aa, aa, bb), true);
    assertNotEquals(repeated, reordered);
    assertNotEquals(repeated, checkedPage(List.of(aa, bb), true));
    M3FS.DirEntry copy = new M3FS.DirEntry(new String("Aa"),
        new M3FS.Stat(1, 2, 3, 4, M3FS.Kind.SYMLINK, 5, 6, 7));
    assertEquals(a, checkedPage(List.of(copy), false));
  }

  @Test void admittedSnapshotsStayDetachedAndApiRowsStayImmutable() {
    M3FS.DirEntry first = new M3FS.DirEntry("first",
        new M3FS.Stat(1, 2, 3, 4, M3FS.Kind.FILE, 5, 6, 7));
    M3FS.DirEntry second = new M3FS.DirEntry("second",
        new M3FS.Stat(11, 12, 13, 14, M3FS.Kind.DIRECTORY, 15, 16, 17));
    List<M3FS.DirEntry> input = new ArrayList<>(List.of(first, second));
    M3FS.DirectoryPage page = checkedPage(input, false);
    int initialHash = page.hashCode();
    input.set(0, second);
    input.clear();
    assertValues(page, List.of(first, second), false);
    assertEquals(initialHash, page.hashCode());
    List<M3FS.DirEntry> view = page.entries();
    assertThrows(UnsupportedOperationException.class, view::clear);
    assertThrows(UnsupportedOperationException.class, () -> view.set(0, second));
    assertNotSame(view.get(0), view.get(0));
    assertNotSame(page.table().stat(0), page.table().stat(0));
  }

  @Test void nativeCodecAndJavaAdmissionHaveTheSamePageIdentity() {
    List<M3FS.DirEntry> entries = List.of(
        new M3FS.DirEntry("a", new M3FS.Stat(1, 0, 1, 0, M3FS.Kind.FILE, 0, 0, 1)),
        new M3FS.DirEntry("\u00e9\ud83d\ude00", new M3FS.Stat(Long.MAX_VALUE, 0x100000001L,
            Long.MAX_VALUE, 0x200000003L, M3FS.Kind.DIRECTORY, 07777, 13, 65536)),
        new M3FS.DirEntry("\uffff", new M3FS.Stat(3, 4, 5, 6, M3FS.Kind.SYMLINK,
            Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)));
    ByteBuffer encoded = encode(entries).asReadOnlyBuffer();
    int position = encoded.position();
    int limit = encoded.limit();
    M3DirectoryTable decoded = M3DirectoryTable.decode(encoded);
    assertEquals(position, encoded.position());
    assertEquals(limit, encoded.limit());
    for (boolean more : new boolean[] {false, true}) {
      M3FS.DirectoryPage nativePage = new M3FS.DirectoryPage(decoded, more);
      M3FS.DirectoryPage javaPage = checkedPage(entries, more);
      assertValues(nativePage, entries, more);
      assertEquals(javaPage, nativePage);
      assertEquals(nativePage, javaPage);
      assertEquals(javaPage.table(), nativePage.table());
      assertEquals(javaPage.table().hashCode(), nativePage.table().hashCode());
    }
  }

  @Test void deterministicPagesMatchIndependentOrderedLists() {
    Random random = new Random(0x4d33465320261020L);
    for (int sample = 0; sample < 1024; sample++) {
      int count = random.nextInt(65);
      List<M3FS.DirEntry> entries = new ArrayList<>(count);
      for (int row = 0; row < count; row++) {
        if (row > 0 && random.nextInt(4) == 0) {
          entries.add(entries.get(random.nextInt(row)));
        } else {
          char[] name = new char[1 + random.nextInt(40)];
          for (int i = 0; i < name.length; i++) {
            char unit = (char) random.nextInt(65_536);
            name[i] = unit == 0 || unit == '/' ? '_' : unit;
          }
          M3FS.Stat stat = new M3FS.Stat(
              (random.nextLong() & Long.MAX_VALUE) | 1L,
              random.nextLong() & Long.MAX_VALUE,
              (random.nextLong() & Long.MAX_VALUE) | 1L,
              random.nextLong() & Long.MAX_VALUE,
              M3FS.Kind.values()[random.nextInt(3)],
              random.nextInt() & Integer.MAX_VALUE,
              random.nextInt() & Integer.MAX_VALUE,
              (random.nextInt() & Integer.MAX_VALUE) | 1);
          entries.add(new M3FS.DirEntry(new String(name), stat));
        }
      }
      boolean more = count > 0 && random.nextBoolean();
      M3FS.DirectoryPage page = checkedPage(entries, more);
      List<M3FS.DirEntry> rotated = new ArrayList<>(entries);
      Collections.rotate(rotated, 1);
      M3FS.DirectoryPage other = checkedPage(rotated, more);
      assertEquals(entries.equals(rotated), page.equals(other), "sample=" + sample);
      assertEquals(rotated.equals(entries), other.equals(page), "sample=" + sample);
    }
  }

  @Test void nativePagesAndPinnedSnapshotsKeepValueIdentity() throws Exception {
    assertTrue(M3FS.nativeAvailable(), "the owning FS gate requires actual JNI");
    assertTrue(M3FS.supportsDirectoryPaging());
    assertTrue(M3FS.supportsReadSnapshots());
    Path database = temporary.resolve("directory-identity.sqlite");
    try (M3FS writer = M3FS.create(database)) {
      writer.mkdir("/dir", 0755);
      writer.createFile("/dir/a", 0644);
      writer.mkdir("/dir/d", 0755);
      writer.symlink("a", "/dir/\u00e9\ud83d\ude00");
      try (M3FS snapshot = M3FS.openSnapshot(database)) {
        M3FS.DirectoryPage before = snapshot.listPage("/dir", null, 2);
        assertTrue(before.hasMore());
        List<M3FS.DirEntry> beforeValues = new ArrayList<>(before.entries());
        assertValues(before, beforeValues, true);
        assertEquals(new M3FS.DirectoryPage(beforeValues, true), before);
        int beforeHash = before.hashCode();
        M3FS.DirectoryPage last = snapshot.listPage("/dir", before.nextAfter(), 2);
        assertFalse(last.hasMore());
        assertValues(last, new ArrayList<>(last.entries()), false);
        List<M3FS.DirEntry> complete = new ArrayList<>(beforeValues);
        complete.addAll(last.entries());
        assertEquals(snapshot.list("/dir"), complete);

        writer.setMetadata("/dir/a", 0750, 1_000_000L, true, true);
        M3FS.DirectoryPage changed = writer.listPage("/dir", null, 2);
        assertNotEquals(before, changed);
        assertValues(changed, new ArrayList<>(changed.entries()), true);
        assertEquals(before, snapshot.listPage("/dir", null, 2));
        assertEquals(beforeHash, before.hashCode());
        assertValues(before, beforeValues, true);
      }
    }
  }

  private static M3FS.DirectoryPage checkedPage(List<M3FS.DirEntry> values, boolean more) {
    List<M3FS.DirEntry> oracle = new ArrayList<>(values);
    M3FS.DirectoryPage page = new M3FS.DirectoryPage(values, more);
    assertValues(page, oracle, more);
    M3FS.DirectoryPage equal = new M3FS.DirectoryPage(oracle, more);
    assertEquals(page, equal);
    assertEquals(equal, page);
    assertEquals(page.hashCode(), equal.hashCode());
    return page;
  }

  private static void assertValues(M3FS.DirectoryPage page, List<M3FS.DirEntry> oracle, boolean more) {
    assertEquals(oracle, page.entries());
    assertEquals(more, page.hasMore());
    assertEquals(31 * oracle.hashCode() + Boolean.hashCode(more), page.hashCode());
    assertEquals(oracle.isEmpty() ? null : oracle.getLast().name(), page.nextAfter());
    assertEquals("DirectoryPage[entries=" + oracle + ", hasMore=" + more + "]", page.toString());
  }

  /** Independent construction of the existing native directory wire format. */
  private static ByteBuffer encode(List<M3FS.DirEntry> entries) {
    int bytes = 8;
    for (M3FS.DirEntry entry : entries) bytes += 52 + entry.name().getBytes(StandardCharsets.UTF_8).length;
    ByteBuffer encoded = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
    encoded.putLong(0x0123456789abcdefL);
    for (M3FS.DirEntry entry : entries) {
      byte[] name = entry.name().getBytes(StandardCharsets.UTF_8);
      M3FS.Stat stat = entry.stat();
      int kind = switch (stat.kind()) {
        case FILE -> 1;
        case DIRECTORY -> 2;
        case SYMLINK -> 3;
      };
      encoded.putInt(name.length).put(name).putInt(kind).putInt(stat.mode())
          .putInt(stat.linkCount()).putInt(stat.blockSize()).putLong(stat.inode())
          .putLong(stat.size()).putLong(stat.generation()).putLong(stat.modifiedTimeNanos());
    }
    return encoded.flip().position(8);
  }
}
