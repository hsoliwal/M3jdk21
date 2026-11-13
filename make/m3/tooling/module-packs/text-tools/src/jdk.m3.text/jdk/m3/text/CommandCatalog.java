/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Discovery only. A fuzzy hit cannot become an executable binding without host approval. */
public final class CommandCatalog {
  public static final List<String> COMMANDS = List.of("cat", "cd", "cut", "echo", "find", "grep",
      "head", "ls", "sed", "sort", "tail", "uniq", "wc", "xargs");
  private CommandCatalog() {}
  public static List<String> lookup(String query, int limit) {
    if (query == null || query.length() > 256 || limit < 0 || limit > COMMANDS.size()) {
      throw new IllegalArgumentException("invalid lookup");
    }
    String key = query.toLowerCase(Locale.ROOT);
    return COMMANDS.stream().sorted(Comparator.comparingInt((String c) -> distance(key, c))
        .thenComparing(Comparator.naturalOrder())).limit(limit).toList();
  }
  private static int distance(String a, String b) {
    int[] row = new int[b.length() + 1];
    for (int j = 0; j < row.length; j++) row[j] = j;
    for (int i = 1; i <= a.length(); i++) {
      int previous = row[0]; row[0] = i;
      for (int j = 1; j <= b.length(); j++) {
        int old = row[j];
        row[j] = Math.min(Math.min(row[j] + 1, row[j - 1] + 1),
            previous + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
        previous = old;
      }
    }
    return row[b.length()];
  }
}
