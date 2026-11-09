# M3JDK21 — original M3FS and M3File enhanced by qualified use cases

<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

**M3JDK21 is the integrated Java21/JDK/HotSpot/JNI SDK product.**
M3FS, M3File, Apache Commons VFS providers and the original
SQLite JNI are its existing implementation owners, not an
"M3 + JDK" wrapper or a new runtime layer. The canonical
Synexia repositories hold qualified donor source/recipes,
catalogue decisions and proof memory.

## Qualified already-implemented M3FS use cases

All 19 original source/test/legal/naming postimages were
received from [M3JDK21 #1747](https://github.com/hsoliwal/M3jdk21/pull/1747),
canonical [Synexia #12844](https://github.com/hsoliwal/com.synexia/pull/12844),
after verifying exact preimages/ABSENT inputs.

| Practical use case | Original implementation | Preservation |
| --- | --- | --- |
| Index ZIP/JRT and other non-default `java.nio.file.Path` providers | `MFiles` plus `NioFileProvider` | Borrow caller FileSystem, never close/remount it; identity bound to live provider instance |
| Read/write/append/transfer streams with byte progress and shadow facts | `MFileContent` default methods | Bounded size and EOF, borrowed stream close/flush/error identity; original `MFileCapture` after successful close only |
| Browse/store content under SQLite | `DatabaseFileName/Object/System/Provider` + `m3db:` registration | Reuses M3FS original native SQLite checked inode, WAL, transaction/lock and manifests, not JDBC or another database |
| Select files using archive traversal, predicates or bounded parallel tasks | Original `MFiles` `Plan` | Keep DFS/BFS, owned roots, traversal close and original executor lifecycle |
| Package in a JDK21 linked image | Original VFS module/test and `M3VfsImageProbe` | Preserve unchanged Maven/JMOD/JNI and provider registration contract |

Donor #1747 recorded real combined **372 Java tests / 12 CTest**
and native JDK21 provider probes, but **these results were on its own
qualified branch**. The source receiver did not re-run them.

## New M3File source/observation use case

The prior original `M3File.readAllBytes(maxBytes)` gives caller-owned
bytes without an attached fingerprint. A compiler, IDE/JDT parser or
static analyzer calling `M3File.fingerprint(maxBytes)` immediately
afterwards rereads the file. For consumers that specifically need
whole raw bytes, the **additive** `captureBytes(maxBytes)` and
`captureBytes(maxBytes, M3StreamProgress)` instead consume the
original checked `M3File.observe` stream **once** and return
`M3File.CapturedBytes`:

- `observation()` returns the original raw SHA256, byte count and
  last-modified evidence, subject to original file-key/metadata
  bracketing, EOF and size-limit checks.
- `readOnlyBuffer()` is a fresh read-only ByteBuffer over the
  private dynamic byte-array owner. No mutable backing array
  escapes; it can be passed to parsers needing bytes.
- `copyBytes()` is an explicit defensive copy for a legacy API
  requiring mutable `byte[]`.
- Internally a bounded primitive `byte[]` grows geometrically.
  The final existing allocation can be reused when full; the
  fallback trim may copy one array. This is deliberate whole-file
  materialization and **not** a claim of zero-copy source access
  or universal lower heap usage.
- Clients that only need a SHA-256 or streaming token/AST
  precomputation must keep using the existing `M3File.observe`
  and borrowed synchronous byte observers rather than
  materializing a complete copy.

The original public `M3File` APIs, JNI exports and calling
contracts remain unchanged. File mutation during a read can evade
generic metadata checks; freeze or provider-version a source for a
stronger atomic-generation guarantee. A byte-hash match never
alone proves equal Java semantics or successful Maven/Gradle work.

The existing native M3FS WAL and
`M3BuildStateStore.observe/promoteObserved` remain the one
project checkpoint/commit authority. No new SQL schema,
Node/Entry object graph, native heap pins, or second persistent
content store is added.

## Qualified Synexia use-case crosswalk

Original donor concepts inspected for this review:

- `synexia-fileindex/CONTENT-TRUTH.md`: separate mutable
  path/location from content digest and stable handle. Reuse
  established M3FS content IDs/manifests, not its separate
  FileIndex backing store or unverified latency numbers.
- `synexia-fileindex/COMMIT_PROTOCOL_WAL.md`: visible version
  fences after complete input and projection. Existing M3FS
  native SQLite/observed-successful publication already
  supplies this authority; do not copy a second semantic WAL.
- `synexia-build-inventory/README.md`: project POM/dependency
  role scans, SHA seals and serial source inventories.
  Reuse M3BuildState V2 and M3File fact emitters. Text-level
  heuristics do not assert attributed javac ABI.
- `synexia-index-precompute/README.md`: compile stable
  indexed facts once and amortize JNI on large primitive
  batches; existing M3FS precompute and index images remain
  the native/JVM owners.
- `synexia-m3-residency/README.md`: bounded immutable
  page leases and spill. It is a reference for future
  optional large-file residency, **not** authority to page
  arbitrary mutable `Object[]` or change GC identity now.
- `synexia-dfs/README.md`: Hadoop/HDFS/Spring resource API
  ideas are separate provider candidates with their own
  licensing, credentials and network failure contracts;
  no untested HDFS bridge is copied here.

## Recipe-first and exact source custody

This receiver has 22 serial, source-exact changes:
**19 M3FS original qualified donor files + M3File.java + new
M3FileCapturedBytesContract.java + `m3/file/verify.py`**.

For the M3File change, canonical
[Synexia recipe](https://github.com/hsoliwal/com.synexia/tree/chatgpt/m3jdk21-m3fs-m3file-usecases-20261022/synexia-openrewrite-recipes/crates/m3file-observed-content-capture-20261022)
contains Java21 and Python hash-pinned Maven/OpenRewrite recipes,
the actual pre/post SHA256, ABSENT original test source, a fixed
point/source-drift test definition, the problem-category donor
catalogue, and append-only MOP recipe memory.

`RECEIVED_DONORS.tsv` plus `verify_received_blobs.py` enforce
exact original Git content identities without writing source.
The qualified first-party Apache-2.0 source and attributed
Commons VFS licenses/notices remain intact. Competitive
LeetCode/HackerRank/GeeksforGeeks bodies were not copied.

The original `m3/file/verify.py` strict contract set now admits
`M3FileCapturedBytesContract`: host + linked JDK21 image
execution is mandatory (it will fail if the new test is
omitted). No relaxation of the old module tests, source byte
seals or packaging verification.

## Test status and remaining qualification

- **PASSED, source only:** all 22 receiving postimages checked
  against exact Git blob identities; prior original source hashes
  or ABSENT status checked before each mutation.
- **PASSED, scoped local Java21:** accumulator/read-only buffer
  algorithm compiled under `javac --release 21 -Xlint:all
  -Werror`; `M3_CAPTURE_KERNEL_SCOPED_PASS checks=87270`.
  This tests exact accumulator behavior in an isolated class,
  **not** complete M3File owner/JNI/VFS build.
- **ORIGINAL DONOR HISTORY:** provider #1747 had 372 Java
  and 12 native CTest checks. Those are not refreshed here.
- **NOT EXECUTED:** actual canonical OpenRewrite engine replay,
  full `m3/file/verify.py` host+linked image, original
  M3FS Maven/CMake/JUnit/native JNI/VFS providers on this head,
  source-built M3JDK21 full HotSpot/jtreg/JCK, supported
  Windows/macOS/Android/network providers, and measured
  CPU/heap/native throughput.

No claim of a complete release, universal file-content
stability or blanket speedup. **Keep this pull request DRAFT**
until original owning tests and full SDK verification pass.
