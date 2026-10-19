// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import com.m3.fs.M3FS;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ReadOnlyFileSystemException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** Runs from the linked image with only this probe on the class path. */
public final class M3FsRuntimeProbe {
  private M3FsRuntimeProbe() {}
  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
  public static void main(String[] arguments) throws Exception {
    require(arguments.length == 3, "database, image and SQLite SONAME arguments required");
    Path database = Path.of(arguments[0]).toAbsolutePath();
    Path image = Path.of(arguments[1]).toRealPath();
    require(!Files.exists(database), "probe requires a fresh database");
    require(Path.of(System.getProperty("java.home")).toRealPath().equals(image), "probe is not using the linked image");
    require("jdk.m3fs".equals(M3FS.class.getModule().getName()), "M3FS is not in the owned module");
    String resource = String.valueOf(M3FS.class.getResource("M3FS.class"));
    require("jrt:/jdk.m3fs/com/m3/fs/M3FS.class".equals(resource), "M3FS did not load from jrt: " + resource);
    require(System.getProperty("m3.fs.native.path") == null && System.getProperty("m3.fs.native.pathFile") == null,
        "probe may not use an explicit native library override");
    for (String argument : java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()) {
      require(!argument.startsWith("-Djava.library.path="), "probe may not override java.library.path");
    }
    require(M3FS.nativeAvailable(), "image native library unavailable");
    require((M3FS.featureBits() & 2_097_151L) == 2_097_151L, "incomplete semantic feature plane");
    byte[] original = new byte[140_123];
    for (int index = 0; index < original.length; index++) original[index] = (byte) (index * 31 + 7);
    byte[] live = original.clone(); live[0] = 44;
    try (M3FS fs = M3FS.create(database)) {
      fs.mkdir("/project", 0755); fs.createFile("/project/direct", 0644);
      ByteBuffer source = ByteBuffer.allocateDirect(original.length + 8);
      source.position(4); source.put(original); source.flip(); source.position(4);
      require(fs.write("/project/direct", 0, source) == original.length, "direct write size");
      require(source.position() == source.limit(), "direct write cursor");
      ByteBuffer target = ByteBuffer.allocateDirect(original.length + 8);
      target.position(3); target.limit(original.length + 3);
      require(fs.read("/project/direct", 0, target) == original.length, "direct read size");
      byte[] read = new byte[original.length]; target.flip(); target.position(3); target.get(read);
      require(Arrays.equals(original, read), "direct byte parity");
      long sourceInode = fs.stat("/project/direct").inode();
      try (M3FS snapshot = M3FS.openSnapshot(database)) {
        long beforeBlocks = fs.stats().uniqueBlocks();
        long clone = fs.cloneFile("/project/direct", "/project/clone");
        require(clone != sourceInode && fs.stats().uniqueBlocks() == beforeBlocks, "clone must share blocks with a distinct inode");
        fs.write("/project/clone", 0, new byte[] {99});
        require(fs.read("/project/direct", 0, 1)[0] == original[0], "clone write changed the original");
        fs.write("/project/direct", 0, new byte[] {live[0]});
        require(Arrays.equals(original, snapshot.read("/project/direct", 0, original.length)), "retained snapshot changed");
        require(fs.read("/project/direct", 0, 1)[0] == live[0], "live read failed to observe current bytes");
      }
      require(fs.scrub().cleanAndComplete(), "integrity scrub is not complete and clean");
      fs.verify(); fs.checkpoint();
    }
    try (M3FS readonly = M3FS.openReadOnly(database)) {
      require(Arrays.equals(live, readonly.read("/project/direct", 0, original.length)), "reopened read-only content differs");
      boolean refused = false;
      try { readonly.write("/project/direct", 0, new byte[] {0}); }
      catch (IOException | ReadOnlyFileSystemException expected) { refused = true; }
      require(refused, "read-only handle accepted a write");
      require(readonly.scrub().cleanAndComplete(), "reopened integrity coverage");
    }
    Path nativeLibrary = image.resolve("lib/libm3fs.so").toRealPath();
    Path sqliteLibrary = image.resolve("lib").resolve(arguments[2]).toRealPath();
    Set<Path> natives = new LinkedHashSet<>(), sqlites = new LinkedHashSet<>();
    for (String line : Files.readAllLines(Path.of("/proc/self/maps"))) {
      String[] fields = line.split("\\s+", 6);
      if (fields.length != 6 || !fields[5].startsWith("/")) continue;
      Path path = Path.of(fields[5]); String name = path.getFileName().toString();
      if (name.equals("libm3fs.so")) natives.add(path.toRealPath());
      if (name.startsWith("libsqlite3.so")) sqlites.add(path.toRealPath());
    }
    require(natives.equals(Set.of(nativeLibrary)), "JNI came from an unexpected path: " + natives);
    require(sqlites.equals(Set.of(sqliteLibrary)), "SQLite came from an unexpected path: " + sqlites);
    System.out.println("M3FS_MODULE_RESOURCE=" + resource);
    System.out.println("M3FS_NATIVE_ORIGIN=" + nativeLibrary);
    System.out.println("M3FS_SQLITE_ORIGIN=" + sqliteLibrary);
    System.out.println("M3FS_FEATURE_BITS=" + M3FS.featureBits());
    System.out.println("M3FS_RUNTIME_PROBE=PASS");
  }
}
