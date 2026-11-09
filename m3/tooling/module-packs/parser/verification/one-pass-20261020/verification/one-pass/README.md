# Generated one-pass filesystem/compiler/facts proof

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

This is an opt-in qualification harness outside product APIs. It composes the
already-qualified generated `jdk.m3fs` and `jdk.m3.parser` JMODs with the original
SQLite JDBC 3.45.1.0 JNI distribution and the explicit SLF4J API 2.0.13 module.
The existing `m3/tooling/jmod-recipes/tools/jmod_pack.py` packages the two original
dependencies and this consumer; the JDK's `jlink` links the combined image.
There is no new native engine, descriptor replacement for SQLite/SLF4J, or
production source/class overlay. Original license resources and the actual Linux
x86_64 SQLite JNI resource are checked byte-for-byte in the JMODs and image.

## What the proof executes

Three finite local roots contain six selected Java files, nested subdirectories,
non-Java files and a pruned directory. `MFiles.mapParallel(..., executor, 3, task)`
admits independent worker-owned tasks and returns completion-order detached
indexes. A first-wave latch exercises three concurrent callbacks. Each task:

1. Creates file-local progress, shadow and capture state. A counting adapter
   delegates metadata/open to the real `MFile`; it performs no independent read.
2. Transfers the one monitored primary stream to
   `M3File.read(URI, InputStream, Charset, int)`, which owns and closes it. The
   harness does not add a second stream-close owner or reopen the `Path`.
3. Checks raw bytes, progress, shadow bytes, completed capture/manifest and the
   source's raw SHA-256 against the finite fixture. UTF-16 identity is checked
   separately, including non-ASCII and supplementary characters.
4. Creates a fresh Java compiler/file-manager/context on that worker, parses the
   retained `String` once, and projects the existing tree through
   `M3ASTIngress.javac`. A counted `JavaFileObject` proves one source request and
   forbids a primary-byte reopen. The file manager closes before the detached
   index returns. Runtime parsing uses `-proc:none -source 21`; this is syntax
   projection, with no attribution, processors, code generation or dependency
   analysis. It does not need a source-built JDK or an image-external `ct.sym`.
5. Feeds the owning `Stream<M3ASTIndex>` directly to `M3FactsWriter.write`. The
   real SQLite connection publishes only after exhaustion and successful stream
   cleanup. A fresh unused audit provider observes each real VFS manager's
   component cleanup without replacing the local `file:` provider.

Independent SQL queries check file/node totals, raw and UTF-16 file identities,
all stored exact/structural node hashes, and retained source-uncertainty flags.
An external SQLite connection checks that the generation remains invisible in
the stream-close callback, after all task/input/compiler/provider cleanup.

Three cases run as separate operations: successful publication; an injected
checked task failure after at least one earlier index was written inside the
transaction; and an injected stream-close failure after all indexes were
written. Both failures must roll back their generation and all its rows while
preserving the successful generation. JNI executes under `-Xcheck:jni` and must
report SQLite 3.45.1 with its original native loader active.

## Exact invocation

Use fresh output; previous qualification evidence is never overwritten.

```sh
python3 "$D/verification/one-pass/run_proof.py" \
  --jdk "$B/toolchain/jdk21" \
  --receiving-root "$D/receiver" \
  --fs-qualification "$D/qualification/fs" \
  --fs-sha256 abc5879b71d6161e926970b637bb158a820af805cc175c17a4156184aa976b60 \
  --parser-qualification "$D/qualification/parser-final" \
  --parser-sha256 28cb413a3c3b9e5777651e255b1b2f9b26c6910c04edcbe564dbcd06874e6289 \
  --sqlite-jar "$B/toolchain/test-dependencies/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar" \
  --slf4j-jar "$B/m2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar" \
  --out "$D/qualification/one-pass-final"
```

The runner refuses missing/drifted pins, authored inputs, an existing output
directory, failed prior module qualification, and generated source drift. It
seals all receiving/build inputs before and after, checks product JAR/JMOD class
parity, compiles only this proof with strict Java 21 lint, records exact commands,
and preserves the actual database, native/image resources and runtime results.

## Observed result

The final command exited zero. `qualification/one-pass-final/RESULTS.json` is
`PASS`, SHA-256
`9903f6f97515baf46705a83fdda9b55ce5dd5206a727aeb961f08c93fca8d7a0`.
The linked image contains 26 modules. It executed **1,179 checks in total across
the three cases and setup/final checks**, not 1,179 per case. All 566 sealed
inputs remained unchanged. Product JAR/JMOD byte comparison covered 426 M3FS and
64 parser non-descriptor classes. The image retained all 110 input-JMOD legal
entries and the original SQLite/SLF4J license resources.

The successful generation contains six files (411 consumed bytes), 87 nodes,
six raw-byte file hashes, six UTF-16 file hashes, 72 exact-range node hashes and
87 structural node hashes. Every selected file records one primary open, one
close, one metadata sample, one capture, one javac source request and one parse.
Each of the three operations reached a peak of three active callbacks. Both
injected failures left no generation/file/node rows and preserved the successful
generation. Cleanup completed before external visibility in all three cases.

SQLite reported version 3.45.1, JDBC reported 3.45.1.0, and the original native
loader was active. Its Linux x86_64 JNI resource SHA-256 is
`8991ba66c5c95a6d2a8bc395e874c5550b5acde267c618db1049cc1d801c34f1`,
unchanged in the JMOD and image. `-Xcheck:jni` emitted no JNI diagnostics. SLF4J
reported the absence of a logging provider and used its documented NOP fallback;
this is recorded in the unedited runtime log.

The first attempt is retained in `qualification/one-pass`. Strict javac rejected
the qualification-only module name `m3.onepass.proof` because `m3` ends with a
digit. The sole correction renamed that consumer to `m3fs.onepass.proof`; it did
not change product module names, module requirements or lint flags. Exact
before/after seals and the original diagnostic are in `CORRECTION.json`. Do not
rerun into either retained output directory; choose a fresh name.

## Boundaries

One primary content read and one javac parse are demonstrated **per selected file
per case**. The three separate success/failure cases read their fixtures anew.
Hash validation deliberately recomputes reference hashes; no claim is made that
every hashing or analysis algorithm visits bytes/nodes only once.

The generic parallel API cannot force a callback to read once, finish EOF, parse
once, stay within a heap budget, or cooperate with interruption. Its admission
and unconsumed-result accounting is bounded by `K`; provider arrays, caches,
archive replication and task-owned state are outside that bound. This harness
uses local files and finite fixture/evidence maps. It does not establish bounded
whole-repository memory, archive single-physical-read behavior, universal speed,
or safe sharing of live compiler/provider objects.

Capture metadata is sampled before input. Its timestamp is distinct from provider
modification time. Complete raw evidence can be published by the input-close
observer before a later strict decode fails; such a task still fails and must
not publish a facts generation. Known sampled-size conflict prevents capture.
None of this proves an atomic source snapshot. The supplied-tree public ingress
keeps `SOURCE_UNVERIFIED`; persisted facts are syntactic, not a semantic cache or
an E48 compatibility assertion. Exact range hashes use source UTF-16 units;
structural hashes retain their existing algorithm/input dependencies and are
not claimed whitespace invariant. Source-built OpenJDK/M3JDK21, a default
Synexia Maven switch and the separate M3FS native engine gate remain independent.
