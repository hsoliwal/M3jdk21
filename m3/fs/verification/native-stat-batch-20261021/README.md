<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 M3FS native metadata batch — bounded receiver

M3JDK21 **is** the JDK product and owns `jdk.m3fs` directly. This is an additive
refinement of the existing SQLite/JNI filesystem, not an M3+JDK integration layer,
second storage engine, or separate file-object registry.

## Product atoms (serial per-file changes)

1. `com.m3.fs.M3FsStatBatch`: immutable detached metadata/status columns,
   `long[9 * n]` plus `int[n]` masks; API `M3FS.Stat` is materialized only on
   explicit `stat(row)`. A successful JNI allocation is exclusively adopted
   without a redundant full-lane clone. Package-local construction used by tests
   defensively copies input.
2. `M3FS.statBatch(String...)`: one JNI downcall for at most 4096 paths;
   validates the input array before provider work and preserves path order.
3. `M3FsNative.nativeStatBatch`: optional additive ABI-1 JNI method. An older
   native library missing that entrypoint causes explicit
   `UnsupportedOperationException`, **not** an unnoticed fallback.
4. `m3fs_jni.c`: one native handle lease for the call; 32-row bounded stack slab
   for JNI publication; native status plus eight scalar/stat lanes for each row.
   No Java row objects, C heap result array, retained Java string pins, or
   JNI pointer retained beyond the call. Existing `m3fs_stat_path`, SQLite
   schema, native transaction owner, and all existing JNI entrypoints stay intact.
5. `M3FsStatBatchTest`: packed-column, missing/error, bounded-input, stable-sort,
   live native, and retained snapshot regression witnesses.

## Precise semantics

The row layout is `[nativeStatus, inode, size, generation, modifiedTimeNanos,
kind, mode, linkCount, blockSize]`. Status zero denotes success, one denotes
absence; other defined native statuses remain distinguishable. Unsuccessful
rows have zero scalar lanes; `stat(row)` refuses them.

Mask selection requires all `requiredBits` and none of `forbiddenBits` and
returns detached `int[]` logical positions in original request order. Its time
cost is O(n), memory O(k). `fileRowsBySize` sorts only existing regular-file
positions with stable, signed-`long` comparisons and O(k log k) time/O(k)
scratch; identical sizes retain original path order. No `Map.Entry`, boxed
position, inode wrapper, or full file-content read occurs in these atoms.

On an ordinary live M3FS handle, each path calls the existing native
`m3fs_stat_path` read transaction independently. One JNI crossing is **not**
one SQLite transaction or an atomic multi-file snapshot. For consistent rows,
open the existing retained `M3FS.openSnapshot(database)` first. This operation
has no write/DBFS transaction semantics and makes no IOPS/latency speedup claim.

The separate `FileSystem.executeM3Batch` and `AbstractFileSystem.doExecuteM3Batch`
provider templates live in target PR #1728; Synexia recipe #12815 owns that
source packet. This independent receiver does **not** replace those templates or
claim that every Commons VFS/remote provider now performs native batching.
A later SQLite-backed provider can call the present stat-batch implementation,
while future DBFS/SFTP/HDFS/object providers can specialize the existing
provider template, preserving each provider's capability and failure contracts.

The existing M3JDK21 `M3FilteredViews`, `M3MutableFilteredViews`,
`M3LongSortedViews`, `M3IndexMaskCompactor`, and optional collection JNI masks
are separate canonical owners. They were inventoried for reuse; this filesystem
result needs only its primitive subset, so there is no new runtime dependency on
an external Synexia module or duplicate general collection wrapper framework.

## Reproducible bounded qualification

Local Java 21 stand-alone source compile:
`javac -Xlint:all -Werror` **PASS**.

318 assertions in seeded synthetic `StatBatchContractTest`, including
stable ascending/descending index order, missing/error flags, bounds,
defensive copy, high long sizes, and malformed native lanes: **PASS**.

JNI extraction in isolation:
`gcc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic`
strict syntax: **PASS**.

A synthetic library exporting the exact JNI function was built with those
warnings, then called by Java 21 `-Xcheck:jni`: **PASS** for successful/missing/
error rows, primitive indices, empty batch, bounded input and invalid handle.
That is a mock **JNI geometry and lifetime** probe, not the real SQLite backend.

**NOT VERIFIED:** full product Maven/reactor, native SQLite CMake/CTest,
real JNI regression tests in `M3FsStatBatchTest`, full modified OpenJDK build,
Eclipse Resources/OSGi suite, other OS targets, benchmark versus per-row JNI,
and complete Synexia SDK/OpenRewrite recipe execution. Neither compile/test
skips nor CI failures should be presented as passes.

## Next admission passes

- Validate actual `m3/fs/pom.xml` with native SQLite and `-Xcheck:jni`;
  check all original native header/export contracts plus one new JNI symbol.
- Qualify the real provider specialization and DBFS SQL transaction hook in
  the existing `AbstractFileSystem`, not a second filesystem API.
- Benchmark Java-per-path JNI versus JNI-packed live/snapshot read modes
  with identical operations and metadata expectations. Admit only measured
  improvements, not conjectured throughput.
- Re-run filtered/sorted wrapper live/range/reversed contracts and reuse
  existing collection-native mask kernels only where composition and ABI allow.
- Extend category catalogue before adapting optional remote provider donors;
  preserve original Apache/EPL/BSD notices and license custody.

See [CANDIDATES.tsv](CANDIDATES.tsv) for selection rationale and rejected donor shapes.
