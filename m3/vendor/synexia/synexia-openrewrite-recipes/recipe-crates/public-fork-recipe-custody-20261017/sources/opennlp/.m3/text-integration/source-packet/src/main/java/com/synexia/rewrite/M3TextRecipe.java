// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Filesystem adapter for the existing M3GpuToolJoinManifest; no second manifest parser. */
public final class M3TextRecipe {
  private M3TextRecipe() { }

  public static void main(String[] args) throws Exception {
    if (args.length < 2) throw new IllegalArgumentException("apply|verify|catalogue REPO_ROOT [query]");
    Path root = Path.of(args[1]).toRealPath();
    if (args[0].equals("catalogue")) {
      String query = args.length == 3 ? args[2].toLowerCase(Locale.ROOT) : "";
      for (String row : Files.readAllLines(root.resolve(".m3/text-integration/catalogue.jsonl"))) {
        if (row.toLowerCase(Locale.ROOT).contains(query)) System.out.println(row);
      }
      return;
    }
    if (!args[0].equals("apply") && !args[0].equals("verify")) throw new IllegalArgumentException("unknown mode");
    int changes = run(root, root.resolve(".m3/text-integration/dataset"), args[0].equals("apply"), null);
    System.out.println(args[0] + " changes=" + changes);
  }

  /**
   * Use an exclusive checkout. Validate the complete batch before writing, recheck each source,
   * then install serially with a per-file atomic move. Not a multi-file filesystem transaction.
   * Deletions are deliberately unsupported. Null checkpoint selects the existing no-op monitor.
   */
  public static int run(Path root, Path data, boolean apply, Runnable checkpoint) throws IOException {
    Path base = root.toRealPath();
    Runnable check = checkpoint == null ? M3GpuToolJoinManifest.NOOP : checkpoint;
    M3GpuToolJoinManifest manifest = M3GpuToolJoinManifest.load("/dataset/", name ->
        Files.newInputStream(safe(data.toRealPath(), name.substring("/dataset/".length()))), check);
    var scan = manifest.scan();
    for (var t : manifest.targets()) {
      manifest.checkpoint();
      if (t.removal()) throw new IllegalStateException("this additive adapter does not delete sources");
      Path target = safe(base, t.path());
      if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) scan.observe(t.path(), hash(target));
    }
    scan.requireAdmissible();
    // Verification must reject preimages, not merely regard them as admissible.
    if (!apply) {
      for (var t : manifest.targets()) {
        if (!Files.exists(safe(base, t.path())) || !hash(safe(base, t.path())).equals(t.after()))
          throw new IllegalStateException("not generated: " + t.path());
      }
      return 0;
    }
    int changed = 0;
    for (var t : manifest.targets()) {
      manifest.checkpoint();
      Path target = safe(base, t.path());
      boolean exists = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
      String actual = exists ? hash(target) : M3GpuToolJoinManifest.ABSENT;
      if (actual.equals(t.after())) continue;
      if (t.addition()) {
        if (exists || !scan.missing(t.path())) throw new IllegalStateException("addition drift: " + t.path());
      } else {
        if (!exists) throw new IllegalStateException("source disappeared: " + t.path());
        scan.editing(t.path(), actual);
      }
      Files.createDirectories(target.getParent());
      Path tmp = Files.createTempFile(target.getParent(), ".m3-recipe-", ".tmp");
      try {
        Files.writeString(tmp, t.text());
        if (exists) {
          try { Files.setPosixFilePermissions(tmp, Files.getPosixFilePermissions(target)); }
          catch (UnsupportedOperationException unsupported) { /* Windows has no POSIX permissions. */ }
        }
        manifest.checkpoint();
        safe(base, t.path());
        if (!Objects.equals(actual, Files.exists(target) ? hash(target) : M3GpuToolJoinManifest.ABSENT))
          throw new IllegalStateException("source changed before move: " + t.path());
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        changed++;
        System.out.println(t.path() + " " + t.before() + " -> " + t.after());
      } finally { Files.deleteIfExists(tmp); }
    }
    return changed;
  }

  static String hash(Path p) throws IOException { return M3GpuToolJoinManifest.sha256(Files.readString(p)); }

  static Path safe(Path root, String relative) throws IOException {
    Path p = root.resolve(relative).normalize();
    if (Path.of(relative).isAbsolute() || !p.startsWith(root) || p.equals(root))
      throw new IllegalStateException("path escapes root");
    Path cursor = root;
    for (Path part : root.relativize(p)) {
      cursor = cursor.resolve(part);
      if (Files.isSymbolicLink(cursor)) throw new IllegalStateException("symlink refused: " + cursor);
    }
    return p;
  }
}
