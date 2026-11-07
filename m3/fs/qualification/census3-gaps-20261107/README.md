<!-- SPDX-License-Identifier: Apache-2.0 -->
# census3-gaps-20261107 (m3/fs)

Census pass 3 member-gap leaf of lane com-synexia-0e on `m3/fs`: **P3-001** (`M3FsNative` vs develop
`com.synexia.indexstring.fs.MIndexFsNative`, `M3SnapshotStore` vs `MIndexSnapshotStore`). Census pins: com.synexia
develop `78cf2ad4c7e5`, M3jdk21 master `2274e18709b8` (`S:/m3-scratch/eb-conv/census3`); donor re-read at
develop-live head `78cf2ad4c7e5`, owner at master `0581643118d7`.

**Record-only leaf: no member needs an edit.** All 9 unexplained MEMBER_GAPS rows are PRESENT_EVOLVED
(`DECISION.tsv`):

| member | owner shape |
|---|---|
| `M3FsNative.LIBRARY` (NativeLibrarySpec) | `available()` loads the library itself: `m3.fs.native.path`, `m3.fs.native.pathFile`, else `System.loadLibrary("m3fs")`; the donor's `com.synexia.nativeinterop` spec/loader has no M3 owner and is not needed (owner since 23edf362f68 "Port MIndexFS capabilities into M3FS") |
| `M3SnapshotStore.namespace` (UUID) | DOOP scalar lanes `namespaceMost`/`namespaceLeast`; `namespace()` rebuilds the UUID; CAS admission decodes scalars (owner javadoc) |
| `M3SnapshotStore.WRITERS` (Set&lt;Path&gt;) | lock object + `registerWriter`/`unregisterWriter` over primitive lanes; same one-writer-per-directory-per-JVM admission |
| `put`, `map`, `publish`, `cloneSnapshot`, `monitor`, `verify` (IProgressMonitor) | `IProgressMonitor` -> `M3FsProgress`, the module's progress contract; same bodies otherwise |

Owner history (blobless clone, `git log --no-renames`): `M3FsNative`/`M3SnapshotStore` created by 23edf362f68
(2026-10-19), JNI column entrypoint 39ea5da6c7e, renamed by fe35ea98841/e2e9e0d3e6e (JDK naming); no later commit
removed a donor member (nothing to cite as SUPERSEDED).

No source change: the build, differential, mutant and javap gates do not apply (`GATES.tsv` records the scope and
naming gates).
