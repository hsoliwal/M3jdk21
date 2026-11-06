// SPDX-License-Identifier: Apache-2.0
package jdk.m3.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Resolves an executable once from the host PATH, not from a shell interpreter. */
public final class HostExecutable {
  private HostExecutable() { }

  public static Optional<Path> find(String executable) {
    Objects.requireNonNull(executable, "executable");
    if (executable.isBlank() || executable.contains("/") || executable.contains("\\")
        || executable.indexOf('\0') >= 0 || executable.equals(".") || executable.equals("..")) {
      throw new IllegalArgumentException("expected executable basename");
    }
    String path = System.getenv("PATH");
    if (path == null || path.isEmpty()) return Optional.empty();
    boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT)
        .contains("windows");
    List<String> suffixes = windows && !executable.contains(".")
        ? List.of(".exe", ".com") : List.of("");
    for (String directory : path.split(java.util.regex.Pattern.quote(
        java.io.File.pathSeparator), -1)) {
      // Never interpret the empty PATH entry as implicit current-working-directory authority.
      if (directory.isBlank()) continue;
      for (String suffix : suffixes) {
        Path candidate;
        try {
          candidate = Path.of(directory, executable + suffix).toAbsolutePath().normalize();
          if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
            return Optional.of(candidate.toRealPath());
          }
        } catch (IOException | java.nio.file.InvalidPathException ignored) {
          // An unreadable PATH entry is not an admitted executable.
        }
      }
    }
    return Optional.empty();
  }
}
