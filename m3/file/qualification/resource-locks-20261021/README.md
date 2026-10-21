<!-- SPDX-License-Identifier: Apache-2.0 -->
# Java resource scopes — M3JDK21 original owner

## Direct Java21 product, not an adapter
M3JDK21 is the integrated Java21/JDK/HotSpot/JNI SDK. This changes
the existing `m3/file` product domain by adding
`com.m3.file.M3ResourceLocks`, not a second Eclipse JobManager or
Synexia runtime dependency. `M3File` retains original NIO Path/provider,
external refresh, NOFOLLOW, stream and native M3FS authority.
Existing public JDK, M3File, Eclipse and JNI declarations are unchanged.

Canonical Maven/OpenRewrite named source recipe:
[Synexia #12814](https://github.com/hsoliwal/com.synexia/pull/12814)
(`7664c4129887eaffd0f57550fb1aa494ce646160`). Reuses existing unchanged
`com.synexia.m3jdk.AstBuildReverse20261017`, exact two new ABSENT-before
Java source templates, sorted SHA-256 manifest and current engine pins.
Both product Java Git blobs must match canonical candidate templates exactly:
- `M3ResourceLocks.java`: `210e3099f21bfbc5881aa3f53c03d8b04a439714`, SHA256 `99a90eca92fea1e45604e29c0f1c1295e9e4e3c05dc3bbb3b27c48890f007575`.
- `M3ResourceLocksContract.java`: `b204f79fc5c0973c52cd8d755597f60536ad0a8d`, SHA256 `a20c5e004fd23a83f05e02bc895c62f11ec8db02d86f1c0dae5448a4b911d292`.

## Semantics and limits
`Scope.exact(Path)` excludes only a single lexical path.
`Scope.subtree(Path)` excludes that path and true component descendants.
Normalized paths are compared within their original NIO `FileSystem`;
path-like textual siblings never collide.
Exclusive in-process ownership is held by the acquiring Java thread,
nested only into contained scopes, released in strict LIFO order.
Competing threads block interruptibly via a fair `ReentrantLock`/
`Condition`; unrelated scopes can execute concurrently.
No background worker is started by the lock.
`run(Runnable)` and `call(Callable)` preserve Java result/exception
and release in try-with-resources. No JNI callbacks or GPU operations.

**Not a native file lock:** symlink aliases, provider case folding, external
filesystem writes, cross-process exclusive access, M3FS inode transactions,
Eclipse ISchedulingRule/UI thread semantics and distributed lease recovery
must be handled at their established owning boundaries.

## Actual finite standalone validation
Local original candidate sources compiled with
`javac --release 21 -Xlint:all -Werror`; the actual test source returned
`M3_RESOURCE_LOCKS_PASS checks=140034`, including 70,000 randomized pairs,
thread ownership, nested LIFO, interrupted waits, disjoint parallel scopes,
refusal paths and Runnable/Callable callback identity.

Reproduction from the product checkout (Java21 only):
```sh
mkdir -p /tmp/m3-resource-locks
javac --release 21 -Xlint:all -Werror -d /tmp/m3-resource-locks \
  m3/file/src/main/java/com/m3/file/M3ResourceLocks.java \
  m3/file/src/test/java/com/m3/file/M3ResourceLocksContract.java
java -cp /tmp/m3-resource-locks com.m3.file.M3ResourceLocksContract
```

## Required HOLD
The exact-head CI workflow compiles/tests source and refuses hash drift.
The canonical OpenRewrite Maven SDK five-test named Recipe.run and exact
SDK receiving fixed-point, entire current m3/file Maven owner reactor,
full rebuilt M3JDK21 image/jtreg/JCK/HotSpot, original Eclipse scheduling
and Resources/Equinox workspace suites, platform-specific file behavior,
JNI image/native modules and comparative CPU/heap measurements remain
NOT_RUN in this authoring environment. Source SHA equality and strict
isolated Java21 checks are **not** full acceptance. Keep PR DRAFT until
the actual authoring/receiving/hosted release gates pass. Fix the
canonical recipe and replay on failures rather than hand-patch output.
