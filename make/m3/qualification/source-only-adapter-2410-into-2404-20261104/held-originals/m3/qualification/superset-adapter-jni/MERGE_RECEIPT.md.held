<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

# Verified two-parent source-history fold: adapter/lineage and concurrent JNI

## Actual GitHub operation — 2026-11-04

GitHub **merged transport PR [#2409](https://github.com/hsoliwal/M3jdk21/pull/2409)**
into the isolated branch
`m3/additive-adapter-jni-lineage-superset-20261104`.
This is **not** the protected default `master`, not a release, and not a
claim to have merged the entire `hsoliwal/*` PR fleet.

| Git identity | Exact SHA |
| --- | --- |
| New two-parent merge commit | `96d60fa8a00e3deb0dc689e800d9c3d35fc1d11d` |
| First parent — documented staging from #2394/#2395 | `ad7071704e6d235254b64b61d18bfbe43532776d` |
| Second parent — original #2393/#2405/#2406 donor | `6cfc59b98f7d9609feee847a720bcb851f36ef04` |
| Resulting Git tree | `e44b3b236dafdddea196ade90e6031e7a1e61409` |
| Common original master ancestor | `172e39d5e69a8f7775c25ad1ce2bfaf2bf5d1246` |

The exact GitHub commit response had **two** parents in this order.
The receiving first-parent diff to the new merge showed **11 added paths,
zero modifications, zero deletions or renames**, and zero behind.
Before transport, the original receiving chain changed 16 paths and
the donor chain added 11 disjoint paths; their intersection was empty.

## Byte-for-byte and mode custody

`EXACT_SOURCE_BLOBS.tsv` records all **11 original donor paths**,
original Git blob SHA-1, merged Git blob SHA-1, original Git tree mode,
and merged mode. Every original and merged source file was independently
fetched from GitHub and compared **byte for byte**, giving
**11/11 exact source content matches**. Original and resulting Git
trees were traversed by subdirectory rather than accepting the
truncated Git recursive-tree result. All **11/11** mode values
are `100644` and their blob IDs match exactly.

The receiving `src/java.base/share/classes/java/lang/Object.java`
Git blob remains `c703c72bea6e792a378bb6d3babc96f22dbf6cb4`,
unchanged from the #2395 receiver. The donor's concurrent Java/C
test postimages remain
`e5755b524ca9b7608480462b29dc8d11388a52e4` and
`5cb3533c9ea53e59fe9e56cf40c4c7500c739040`.
Neither original branch was rewritten, moved, squashed or deleted.

## Current boundary of qualification

The donor's Java21/C17 JNI model tests are previously recorded,
but they do **not** execute the real modified JDK virtual
`Object.toAdapter(Class<T>)` JNI path or weak owner GC.
The receiving historical Object/progress gate retains **eight source
mismatches**, only one of which has full Oct17→Oct18→Nov2
source-lineage reconstruction. A read-only census passing does **not**
mean the historical progress release gate passed.

**NOT RUN on this merged commit**: combined Maven/OpenRewrite source
recipe SDK, JDK21/full `java.base`, JNI/native libjava, jtreg,
HotSpot/vtable compatibility, classloader/GC/JCK/ABI, complete
hosted CI, performance and cross-repository release admission.
It is a **source-exact staging merge, not production integration**.

Required action before release: run all specified gates against the
final exact source tree, reconcile historical sources serially,
retain negative tests, and then separately approve any default-branch
merge. Keep the review PR DRAFT/HOLD.

Google Drive continuity source (older snapshot, not authority):
https://docs.google.com/document/d/14POJyi5FgXiD_GpE_snZQI5pyraL3CdgTCZFPUr1GXQ/edit
