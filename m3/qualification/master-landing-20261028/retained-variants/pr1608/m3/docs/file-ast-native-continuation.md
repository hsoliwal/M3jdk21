<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# File, AST and native stream continuation

M3JDK21 owns the implementation. Reusable transformation datasets are in the
existing Synexia runtime-progress-facade recipe crate, PR12609. This continuation
extends PR1608 additively; it preserves original provider paths and stream APIs.

## File identity and changes

`M3FileScope(maximumFiles)` is an explicit closeable interning lifetime. It uses
one growing array of canonical facades with open addressing and a half-full load
bound. Equality includes FileSystem identity; spellings are not normalized. It
does no file IO and closes no providers. A full scope refuses a new identity,
while existing identities remain retrievable. Returned facades survive close.
`M3File.of` retains its original fresh-facade behavior.

`snapshot(maxBytes)` reuses the complete bounded SHA-256 implementation from
PR1581. It composes with the existing lazy `walk`. It reports creation, deletion,
metadata and content changes. Same-length writes with restored timestamps still
change the content digest. This is not a stable concurrent disk revision.

`observe()` reads attributes without reading payload. `changesFrom` returns
independent type, size, modification/creation/change time, file-key and provider
version bits. Unix ctime is optional and availability is provider-owned. Equal
metadata alone cannot authorize content reuse. Access times are excluded.

An NIO provider may expose `M3FileVersionView` with immutable namespace/generation
tokens. The view must change tokens for every content mutation or replacement and
never recycle tokens within a namespace. A versioned observation brackets fresh
metadata with two equal tokens; a changing version refuses. Unsupported providers
continue through metadata observations and complete snapshots. The view is an
opt-in contract, not an assertion that every existing NIO provider implements it.

## Structured downstream deltas

The existing `M3ASTIndex` now exposes `structureSha256()` and `deltaFrom(previous)`
for each of its domains. The hash has a versioned, length-prefixed UTF-16 encoding
of domain, node kinds and ordered topology. It deliberately excludes source text,
ranges and flags, which have independent observations. It is not a semantic or
class-ABI hash and does not authorize compilation skipping.

The delta compares exact lanes and kind strings. One byte per occurrence records
ADDED, REMOVED, STRUCTURE, RANGE and FLAGS. Source changes are independent; a
fresh primitive changed-row list is projected only on request. Neither AST nor
source payload is retained by the delta. URI/domain mismatches refuse comparison.
Rows are generation-local; insertions can shift later rows. This is not a minimal
tree-edit algorithm or a persistent node-identity scheme.

## Reproduction

- Run the canonical dataset's Maven `-Pcontract verify`, then `receive.py`.
- Run `mvn -f m3/file/pom.xml test`; run `verify_stream_shadow.py` with Java21.
- Run parser `build.py` phases lint, compile, test, runtime, using a fresh output.
  The AST delta contract runs on the host and installed parser image.
- Run `mvn -f m3/fs/pom.xml verify` with CMake, SQLite and Java21 JNI headers.
- Install that M3FS artifact, then run `mvn -f m3/eclipse/filesystem/pom.xml verify`
  with `-Dm3.fs.native.path` pointing to the resulting shared library.
- Compile `M3NativeProgressShadow` against the original Eclipse/M3FS classes and
  run it with the M3JDK21 java.base stream APIs and `-Xcheck:jni`. The class is under
  `m3/eclipse/filesystem/qualification/streams/src/test/java`.
- Package the compiled `com.m3.file` module as a JMOD, link it, and execute
  `M3FileIntegratedImageProbe` from the installed image without module patches.

The complete Java/HotSpot source build (`make images`) and installed-image probes
pass on Linux x86_64. Streams, native M3FS progress/shadows, the installed file
module and all four parser contracts execute without module patches. The file
module's 28 JUnit tests and real Eclipse JDT's six JUnit tests also pass on that
source-built image. The canonical task's `verify_installed.py` retains exact
commands, source seals, JMODs and runtime hashes.

The canonical host validates its retained recipe memory before SDK execution,
replays integrity tests, and binds all retained lesson/evidence bytes into SDK
inputs. Changed or missing memory refuses receiving before any product write.
The catalogue records ten problem categories, alternatives, donor/licensing
choices and actual evidence; subsequent passes can search it by atom/category.
This is enforcement in the existing recipe host, not a repository-wide automatic
writer for every agent correction.

See `file-ast-native-qualification-20261021.json` for current evidence; earlier
stream/JDT receipts are historical and describe their original source hashes.
Full jtreg, other platforms, comparative CPU/heap/native-memory results and
complete five-library collection admission remain open. Receiving phase STRING
is unchanged. No universal all-parser or all-repository completion is asserted.
