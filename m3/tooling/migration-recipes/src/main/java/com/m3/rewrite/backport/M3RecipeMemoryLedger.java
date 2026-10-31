// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Machine-readable self-improvement memory for one M3 recipe crate.
 *
 * <p>The ledger is append-only by contract. Git history is the durable publication log; this class
 * validates the current file and can prove that a candidate retains an older validated ledger as
 * an exact prefix.</p>
 */
public final class M3RecipeMemoryLedger {
  public static final String HEADER =
      "seq\trecipe\ttrigger\tobserved\tcorrection\tguard\tlesson\tcategory\tdonor\tstatus";
  private static final int MAX_BYTES = 1024 * 1024;
  private static final Set<String> STATUSES =
      Set.of("RECORDED", "VALIDATED", "ADMITTED", "REJECTED", "SUPERSEDED", "REVOKED");

  public record Entry(
      int sequence,
      String recipe,
      String trigger,
      String observed,
      String correction,
      String guard,
      String lesson,
      String category,
      String donor,
      String status) {}

  private M3RecipeMemoryLedger() {}

  public static List<Entry> read(Path file) throws IOException {
    Path path = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
    if (Files.isSymbolicLink(path)
        || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
        || Files.size(path) <= 0L
        || Files.size(path) > MAX_BYTES) {
      throw new IOException("invalid M3 recipe memory file: " + path);
    }
    byte[] bytes = Files.readAllBytes(path);
    String text =
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString();
    return parse(text);
  }

  public static List<Entry> parse(String text) {
    List<String> lines = Objects.requireNonNull(text, "text").lines().toList();
    if (lines.isEmpty() || !HEADER.equals(lines.getFirst())) {
      throw new IllegalArgumentException("invalid M3 recipe memory header");
    }
    List<Entry> result = new ArrayList<>();
    int expected = 1;
    for (int line = 1; line < lines.size(); line++) {
      String value = lines.get(line);
      if (value.isBlank()) continue;
      String[] cells = value.split("\t", -1);
      if (cells.length != 10) {
        throw new IllegalArgumentException("invalid M3 recipe memory row " + (line + 1));
      }
      int sequence;
      try {
        sequence = Integer.parseInt(cells[0]);
      } catch (NumberFormatException invalid) {
        throw new IllegalArgumentException("invalid M3 recipe memory sequence", invalid);
      }
      if (sequence != expected++) {
        throw new IllegalArgumentException("M3 recipe memory sequence is not contiguous");
      }
      for (int index = 1; index < cells.length; index++) {
        if (cells[index].isBlank() || cells[index].indexOf('\0') >= 0) {
          throw new IllegalArgumentException("blank M3 recipe memory field");
        }
      }
      if (!cells[1].matches("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+")) {
        throw new IllegalArgumentException("invalid recipe identity");
      }
      if (!STATUSES.contains(cells[9])) {
        throw new IllegalArgumentException("invalid M3 recipe memory status");
      }
      if (!"PENDING".equals(cells[4]) && !cells[4].matches("[0-9a-f]{40,64}")) {
        throw new IllegalArgumentException("invalid correction revision");
      }
      if ("PENDING".equals(cells[4]) && !"RECORDED".equals(cells[9])) {
        throw new IllegalArgumentException("only RECORDED memory may be pending");
      }
      result.add(
          new Entry(
              sequence,
              cells[1],
              cells[2],
              cells[3],
              cells[4],
              cells[5],
              cells[6],
              cells[7],
              cells[8],
              cells[9]));
    }
    return List.copyOf(result);
  }

  public static void verifyAppendOnly(List<Entry> previous, List<Entry> candidate) {
    List<Entry> oldEntries = List.copyOf(Objects.requireNonNull(previous, "previous"));
    List<Entry> newEntries = List.copyOf(Objects.requireNonNull(candidate, "candidate"));
    if (newEntries.size() < oldEntries.size()) {
      throw new IllegalArgumentException("M3 recipe memory removed prior rows");
    }
    for (int index = 0; index < oldEntries.size(); index++) {
      if (!oldEntries.get(index).equals(newEntries.get(index))) {
        throw new IllegalArgumentException("M3 recipe memory rewrote prior row " + (index + 1));
      }
    }
  }
}
