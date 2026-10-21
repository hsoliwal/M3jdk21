// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import com.m3.fs.M3FS;
import com.m3.fs.M3FsIndexImage;
import com.m3.fs.M3FsRegexImage;
import java.nio.file.Path;

/** Executes the automatic RE2 fallback from the owned module in an actual linked image. */
public final class M3FsRegexFallbackProbe {
  private M3FsRegexFallbackProbe() {}
  private static void require(boolean test, String message) {
    if (!test) throw new AssertionError(message);
  }
  public static void main(String[] args) throws Exception {
    require(args.length == 1, "one fresh database path is required");
    Module owner = M3FS.class.getModule();
    require("jdk.m3fs".equals(owner.getName()), "M3FS must be in jdk.m3fs");
    require(System.getProperty("m3.fs.native.path") == null, "no native library override permitted");
    Class<?> backend = Class.forName("com.m3.fs.internal.re2j.Pattern", false, M3FS.class.getClassLoader());
    require(backend.getModule() == owner, "RE2 fallback must have the M3FS module owner");
    require(!owner.isExported("com.m3.fs.internal.re2j"), "RE2 implementation must not become a public module export");
    String origin = String.valueOf(backend.getResource("Pattern.class"));
    require("jrt:/jdk.m3fs/com/m3/fs/internal/re2j/Pattern.class".equals(origin), "RE2 source not present in the owned image");
    var image = M3FsIndexImage.of("\ud83d\ude00");
    var program = M3FsRegexImage.fromImage(new M3FsRegexImage.Image(".",
        M3FsRegexImage.Semantics.RE2, new byte[] {1, 6}, new int[] {1, -1},
        new int[] {-1, -1}, new int[] {0, -1}, new long[] {~(1L << '\n')},
        new long[] {-1L}, new int[] {0}, new int[] {1}, new char[] {128},
        new char[] {65535}, 0, 1, 1, 1, 0, 0, 0, true));
    try (M3FS fs = M3FS.create(Path.of(args[0]))) {
      fs.createMIndexFile("/values", 0644, image);
      for (M3FS.RegexMode mode : M3FS.RegexMode.values()) {
        require(fs.nativeRegex("/values", program, mode).fallbackCount() == 1, "native fallback boundary not reached");
        var result = fs.nativeRegex("/values", program, mode, image);
        require(result.matches(0) && result.fallbackCount() == 0, "automatic fallback failed in " + mode);
      }
    }
    System.out.println("M3FS_RE2_MODULE_RESOURCE=" + origin);
    System.out.println("M3FS_RE2_FALLBACK_PROBE=PASS");
  }
}
