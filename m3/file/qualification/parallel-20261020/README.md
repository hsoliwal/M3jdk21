<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original DOOP file/index lanes: bounded parallel selected work

**M3JDK21 is the product—not an M3 + JDK adapter or a second filesystem.**
This is a strictly stacked refinement on [M3FileTree #1590](https://github.com/hsoliwal/M3jdk21/pull/1590);
the original `M3File`, `M3FileTree`, `M3FileWalk`, `M3FileShadowInputStream`,
`M3FileShadowOutputStream`, `M3FileProgress`, `M3FileChangeProvider`, hash lanes,
NIO/ZIP providers and native/JNI storage owners remain in charge.

## Execution law

There is only one authoritative M3FileTree per admitted root. Its interned
name/type strings, physical row IDs, CSR child offsets, `BitSet` object flags,
subtree indexes and optional content hashes are never reproduced by a scheduler.

- `M3FileTraversal.parallelFiles(p, monitor, task, sink)`: stream only
  selected **regular-file row IDs** from the existing ID spliterator. An
  original `M3File` facade is requested only in the worker. At most p
  virtual-thread tasks/futures are in flight; p is capped at 256. No
  `ids().toArray()`, whole-file arrays, directory copies, per-file `Entry`
  nodes or second file descriptors are introduced.
- `M3FileTraversal.parallelDirectories(spec, p, monitor, task, sink)`:
  a default method deriving a lazy stream of original directory-row paths
  from the selected traversal. Caller selects disjoint directories to avoid
  duplicate processing of overlapping subtrees.
- `M3FileParallel.roots(rootStream, spec, p, monitor, task, sink)`: one
  original `M3FileTree.open(root, spec)` per admitted root/drive/directory,
  opened in its own bounded worker and closed before that worker returns.
  `M3FileScanSpec.lazy(0)` starts with **only that root's first row**.
  `tree.scanChildren` expands independently inside its owning worker
  when needed. Only up to p root trees exist concurrently; a stream of
  drive roots does not materialize all drives or traverse every file.
- All results reach their sink **in source encounter/root input order**,
  even if workers finish out of order. Work callbacks may have side
  effects in any scheduling order; no implicit transactional rollback
  is promised.
- Existing per-file `M3FileShadow` observers may compute SHA-256, CRC32C,
  UTF-8/token facts, line breaks and M3AST structure from the **same
  primary input stream read**. For unchanged source/class generations,
  original M3File+compiler ABI/environment facts govern invalidation;
  metadata size, timestamp and file key are only provider-specific
  change hints, never universal proof that bytes are unchanged.

Virtual threads are **not pooled**. The original input producer admits
no more than p tasks into a private bounded `ArrayDeque`, then awaits
the oldest result. Workers check cancellation through a synchronized
delegate of the original `M3FileProgress` defaults; `worked` callbacks
are serialized, but byte processing and content reads are not. The
producer checks cancellation during timed Future waits. On refusal the
scheduler interrupts every admitted pending task and does **not** send
`done()`. External I/O may still have committed side effects.

The source row generation is sealed during indexed file processing: the
original M3FileTree must not be structurally mutated by another thread.
Lazy directory expansions are supported safely by processing each
selected directory as an **independent** root/owner rather than writing
multiple concurrent appends into one shared lane array.

## In-place API example

```java
try (var root = M3FileTree.open(drive, M3FileScanSpec.lazy(1))) {
    // Iterate only selected file types; the original M3 tree remains authoritative.
    var files = root.walk().types("txt", "json", "bin").kinds(M3FileKind.FILE);
    files.parallelFiles(8, M3FileProgress.NONE, (row, file, monitor) -> {
        var digest = M3FileShadow.sha256();
        return file.observe(128L << 20, monitor, digest);
    }, (row, observedBytes) -> {
        // Original row ID and deterministic encounter order; store downstream
        // deltas using existing M3FS/ABI index owners, never a second file table.
    });
}
```

Multiple drives are explicitly selected with `M3FileParallel.roots(
streamOfRoots, M3FileScanSpec.lazy(0), ...)`, not guessed from global OS
state. A valid filesystem provider must obey the original NOFOLLOW and
generation/size constraints. Symlinks are never silently traversed.

## Qualification and MOP recipe memory

This PR is **recipe first**, then exact generated output on one unchanged
original source `M3FileTraversal.java` and two additive original-module
files: `M3FileParallel.java` and `M3FileParallelTest.java`.
Each file is a distinct, hash-pinned OpenRewrite Java 21 pass.
The named recipe uses the incumbent
`com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe` for all three,
with exact before/after SHA-256 and source-mode seals. The original
Maven/OpenRewrite SDK executes real Java LST replay and refuses source
drift, absent inputs, wrong source kind and non-fixed-point results.

`CATEGORY_CATALOG.tsv` stores comparisons by problem category, including
JDK 21 virtual threads, bounded producer/consumer queues, direct
primitive row streaming, DOOP ownership, lazy root expansion, deterministic
reduction and conservative ABI delta invalidation. LeetCode 622,
GeeksforGeeks producer/consumer and HackerRank are **category/discovery**
references; no solution code is copied. Existing M3File and original
JDK/third-party copyright/license notices remain intact.

**Machine Oriented Programming (MOP) — Hitesh Soliwal / Synexia**:
Atomize -> Patternize -> Annotate -> Document, then improve the recipe
itself on an observed failure. `RECIPE_MEMORY.tsv` is an append-only
per-crate memory ledger with the schema of merged MOP PR #1592.
`RECIPE_MEMORY_CANDIDATES.tsv` captures unqualified candidate lessons
without falsely promoting them to proven corrections. A future verified
recipe repair appends a true row in the **same repair commit**, including
the exact failing gate, guard fixture and recipe diff seal. Consumers
must read those records before the next pass.

Reproduce after checking out the stacked branch:

```sh
mvn -B -ntp -f m3/file/pom.xml -Dtest=M3FileParallelTest test

root="$PWD/m3/tooling/shared-frontend/recipe-custody"
mvn -B -ntp -f "$root/pom.xml" -Pcontract \
  -Dm3.dataset=m3-file-parallel-20261020 \
  "-Drecipe.owner.root=$root/sdk-owner" verify
python3 "$root/receive.py" --dataset m3-file-parallel-20261020 \
  --recipe-owner "$root/sdk-owner" --target "$PWD" --check
```

**This is deliberately not yet a full parallel directory *mutation*
engine or whole filesystem index**. It parallelizes work over already
selected indexed rows and independently rooted lazy owner trees, not
unsafe concurrent appends into one shared M3FileTree. Full Maven/Gradle
plugin adoption, native/JNI/HotSpot, Windows/network provider cases,
SQLite persistence, all-file source/class ABI reuse and full
runtime/heap/speed qualification remain explicit gates. Do not merge
or promote without observing the actual full original-owner tests.
