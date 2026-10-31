<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# M3FS provider-native bulk operations: first bounded Java 21 slice

M3JDK21 already owns `jdk.m3fs`, `com.m3.fs.M3FS`, `MFiles`, the relocated
`com.m3.fs.vfs.FileSystem` / `AbstractFileSystem` SPI and SQLite JNI. This change
**adds bulk methods to those existing owners**. It does not create another
filesystem, global data registry or storage implementation.

## Contract

`M3FsBulk.Batch` is an immutable ordered command packet. Its builder uses
parallel `byte[]` operation codes, `String[]` source paths and `String[]`
target paths, copying active ranges once at freeze. There are no per-operation
command/entry objects. Provider handles may still allocate when an operation
needs one. `M3FsBulk.Result` retains status, stat metadata bit masks and error
references in parallel arrays. Status `OK` indicates the provider method
completed normally, **not atomicity, durability or byte-for-byte equivalence**.
`NO_EFFECT` reports a provider returning false or zero for delete; `FAILED`
retains the actual checked exception; `SKIPPED` means not attempted.

`FileSystem` gains additive default methods `executeM3Batch`, `statAll`,
`createFolders`, and `deletePaths`. Its existing `AbstractFileSystem` gains a
single protected `doExecuteM3Batch` template hook. The default is ordered,
sequential, best-effort, works on all existing VFS providers and makes no
transactional promise. Each operation is resolved through the owning provider;
`COPY` uses its recursive `FileObject.copyFrom(SELECT_ALL)` semantics and
`MOVE` uses its provider `moveTo` semantics. This is **not** an atomic
cross-provider mover. `DELETE` is nonrecursive; `DELETE_TREE` is explicit.

```java
M3FsBulk.Batch batch = M3FsBulk.builder(4)
        .stat("/users/a")
        .mkdir("/archive")
        .copy("/users/a", "/archive/a")
        .delete("/stale")
        .build();
M3FsBulk.Result result = fileSystem.executeM3Batch(batch);
for (int i = 0; i < result.size(); ++i) {
    if (result.status(i) == M3FsBulk.FAILED) {
        report(result.error(i));
    }
}
```

## DBFS / transactional provider template

A DBFS provider should build upon the already-implemented `AbstractFileSystem`
for `FileObject` identity and resolution, and override `doExecuteM3Batch`:

1. Validate all paths and capabilities before opening a SQL transaction.
2. In one connection / transaction, resolve owner paths through indexed
   `(parent_id,name)` keys and execute prepared statements in page-sized batches.
3. Use provider-specific savepoints or fail the complete atomic unit. Do not
   report `OK` before commit. Roll back on errors/cancellation.
4. Publish one array-column `Result` only after commit; retain exception causes
   and clearly distinguish unknown outcomes from confirmed changes.
5. For content, use the existing streaming / native scatter-gather owners.
   Do not materialize all BLOBs in memory merely to perform a metadata batch.

**Atomic requests fail before any default fallback I/O.** Only an explicitly
implemented provider hook may accept them. S3/HTTP/HDFS/WebDAV transfers cannot
silently acquire SQL transaction semantics; destination verification and
idempotent retries require separate documented policies.

## Follow-on integration and qualifying providers

- Local NIO: classify filesystem capabilities; check symlink, read-only,
  POSIX/Windows and rename/copy contracts; accept both Java NIO and VFS adapters.
- SQLite-native M3FS: add a native JNI bulk entry point consuming opcode,
  offset/index and mask arrays, with one WAL transaction when requested.
- DBFS (H2/PostgreSQL/SQLite): batch prepared statements and indexed path
  resolution, with explicit isolation and transaction boundaries.
- Archive/RAM: batch lookup against precomputed directory indexes without
  repeatedly constructing `FileObject` facades where possible.
- Remote/HDFS/object storage: preserve native multipart, list-prefix paging,
  continuation tokens, conditional writes and rate limits; no fake atomicity.
- Eclipse EFS: adapt progress/cancellation from the existing M3 monitor and
  preserve provider-specific checked failures/status aggregation.

The current PR does **not** implement new remote transport clients, a SQL DBFS
connector or JNI batch acceleration. Those require separate donor and contract
admission. Scoped local qualification uses a synthetic VFS provider and Java 21
strict compiler with `-Xlint:all -Werror`; full Maven/JMOD/native/Eclipse gates
remain independent.
