# M3JDK21 original M3File offline project-pass receiver

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

**M3JDK21 is the Java21/JDK/HotSpot/JNI SDK itself**, not M3 wrapped
around a JDK. This reviewed receiver stacks on the original
[combined M3File/JNI/AST PR #1681](https://github.com/hsoliwal/M3jdk21/pull/1681)
source head `e691f1f96f6a4ff42113f3d6ac5a2d1723fe3915`.
PR #1681 was merged into its feature branch, not assumed merged into master.

## Existing authoritative owners, no duplicate implementation

- `M3FileTree.open(...).walk().stream()` is the one native M3File
  directory/archive traversal, with interned path segments, physical row
  indices, offsets, primitive flags, original byte sources and bounded
  `M3File` facades. No second walker or WatchService is introduced.
- `M3File.observe(..., M3FileShadow.sha256(), ...)` consumes the original
  selected file **once** and delivers bytes synchronously to optional
  additional shadow consumers (including M3Grammar/M3AST fact producers).
- `M3BuildState` is received **exactly** from the newer first-party
  master Git blob `3b933d3ba3de41b8acc0a7013049de84338069d6`.
  It owns SHA-256 file fingerprints, compiler-attributed ABI masks,
  dependency edge lanes and their safe reverse closure; the offline
  scan emits **unknown ABI and zero compiler edges**, not fake facts.
- `M3BuildStateStore` is source-qualified from current master donor blob
  `1c178381a5275b68cedea9973ee430a1b7793818`, then refined by
  its canonical source-sealed Synexia recipe to store `offline-scan`
  separately from `state` **inside the same existing M3FS native
  SQLite database**. There is no sqlite-jdbc production runtime
  dependency, new SQL schema/storage engine, native JNI export or
  independent filesystem.
- The existing M3FS SQLite WAL/atomic rename/OS lock/CAS source remains
  the publication authority. Publishing is explicit: discovery succeeds
  before caller decides to advance the offline checkpoint. The
  `/m3-build/state` compiler ABI/dependency image is never erased
  by an offline discovery.
- `m3/fs/pom.xml` and `jdk.m3fs` module descriptor add a direct,
  one-way in-product dependency on the original `com.m3.file` module;
  `M3File` remains the canonical entry name, no new `MFile` alias.

## One offline pass for Maven, analyzers and downstream native SQLite

```java
Path root = Path.of("/my/project");
Path db = root.resolve(".m3/project.sqlite");
Files.createDirectories(db.getParent());
try (M3FS fs = M3FS.create(db)) {
    var limits = new M3OfflineProjectScan.Limits(1_000_000, 128_000_000, 268_435_456);
    var candidate = M3OfflineProjectScan.scan(root, fs,
            contextSha256, "maven-context-and-file-policy-v1",
            name -> true, file -> new M3FileShadow[0],
            limits, M3FileProgress.NONE);
    candidate.changedRows().forEach(row -> use(candidate.state().path(row)));
    for (String removed : candidate.removedPaths()) invalidate(removed);
    // Content changes are only discovery: Maven/javac still prove ABI and
    // dependent task impact. Commit when this consumer accepts the scan.
    M3OfflineProjectScan.publish(fs, candidate, limits);
}
```

No active watcher is required. The scan compares current complete SHA-256
against the persisted previous generation; even an in-place write that
preserves size and modification timestamp changes the content hash.
`contextSha256` binds compiler/toolchain/configuration choices; the
separate stable `selectionId` binds the caller's selected input policy.
Policy changes require changing that ID or context. Project URI is
part of the source generation identity. In-memory rows use
`String[]/byte[]/long[]/byte[]` lanes and a primitive stable merge order
over `int[]`; no retained Java `FileEntry`/Node per file.

Delete/rename is conservative: `M3BuildState.changedFrom` marks a
removed input as a full-inventory invalidation because a vanished class or
resource may alter dependency resolution. The scan does **not** assume
hash equality proves semantic/ABI equivalence or that arbitrary Maven
plugin outputs are reusable. Old compiler-attributed facts remain in
their independent state slot; the compiler can explicitly update them.

Limits are explicit (row count, per-input bytes, image bytes); unknown
content or I/O/metadata errors, symlink inputs, duplicate/out-of-project
identities, canceled work, stale concurrent project publications or
corrupt native SQLite checkpoints cannot be treated as unchanged. The
stream is consumed once and closed. Metadata bracketing still cannot
guarantee an atomic filesystem revision against undetectable external
racing writes: freeze sources or use an admitted provider generation.

Project internal `.m3/`, `.git/` and the selected database and its
WAL/SHM/lock files are excluded by default so a checkpoint cannot
invalidate itself. Individual build tools may choose additional
selection predicates; they must bind their selection identity in context.

## Recipe and verification custody

Canonical first-party Synexia
[PR #12807](https://github.com/hsoliwal/com.synexia/pull/12807):
`com.synexia.m3.OfflineProjectScanJava20261021` (Java/module)
and `com.synexia.m3.OfflineProjectScanPom20261021` (Maven/XML).
The existing OpenRewrite source-sealed recipe engines emit the product
Java/module/POM sources, original master build-state contracts and the
new offline SQLite JUnit test. Original source and output SHA-256
manifests, licence attributions and category-first donor evaluation
are in that recipe crate.

The receiver was applied serially file-by-file, with the exact source
fingerprint and Git postimage readback required. No reviewed source is
replaced by copying an entire unrelated master tree.

For a checked-out target branch:
```sh
mvn -B -f m3/file/pom.xml install
mvn -B -f m3/fs/pom.xml verify
# then full M3JDK21 module-pack image, JNI/JTreg/GC and Maven/Gradle
# source-built, classpath/module path and multi-platform smoke
```

**Actual execution has NOT been demonstrated in this receiver**.
The JUnit tests and source-recipe fixed-point/drift-refusal tests are
committed, not asserted passing. The installed-image build, original
native SQLite JNI, parallel/watcher/Windows providers, and
CPU/heap/native-memory benchmarks remain required. No measured
speedup or automatic safe plugin-task skipping is claimed.
