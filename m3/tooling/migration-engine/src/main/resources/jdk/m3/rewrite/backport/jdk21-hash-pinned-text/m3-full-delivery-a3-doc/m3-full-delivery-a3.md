# Synexia full delivery -> A3 receiver contract

## Purpose

M3JDK21 may reuse Synexia-owned Apache-2.0 code, JNI/native atoms, OpenRewrite recipes,
tests, resources and proof tooling through the existing sealed Synexia delivery manifest.

This document extends the existing seed-import model to current/full delivery manifests.
It does not change OpenJDK licensing and does not turn the vendor tree into JDK product source.

The direction remains:

```text
public donors / problem evidence
          |
          v
     com.synexia
inventory -> atomize -> patternize -> compare -> implement -> verify -> seal
          |
          v
Apache-only delivery manifest
source revision + category + source path + target path + SHA-256 + mode
          |
          v
M3JDK21 receiver
verify -> plan delta -> stage candidates -> A3/tooling review -> serial promotion
          |
          v
configure -> make -> jtreg -> runtime
```

## Why a delta/staging plane is required

The checked-in seed import is intentionally small and immutable. A newer Synexia export may:

- add files;
- replace an earlier exported recipe/convergence file with a newer canonical postimage;
- retain unchanged files;
- stop exporting an obsolete target.

Directly overwriting or deleting files under `m3/vendor/synexia` would bypass M3 recipe-first
promotion. Therefore the current/full receiver computes a deterministic delta:

- `ADD`
- `REPLACE`
- `KEEP`
- `STALE`

`STALE` is evidence only. Automatic deletion is not allowed.

Only ADD/REPLACE candidates may be copied, and only below `m3/build/**`.

## A3 lanes

Every exported target is assigned one deterministic target-tool lane:

- `OPENREWRITE_RECIPE`
- `OPENREWRITE_TEST`
- `OPENREWRITE_RESOURCE`
- `CONVERGENCE_JAVA`
- `CONVERGENCE_TEST`
- `CONVERGENCE_NATIVE`
- `M3_CLONER`
- `M3INDEX`
- `M3_RECIPE`
- `OTHER_APACHE`

These lanes do not grant JDK source-write authority.

They answer only: what kind of canonical Synexia asset was delivered and which review/tooling
surface should consume it.

OpenRewrite recipe code remains authoring/proof tooling. Native convergence atoms remain
Java-oracle/parity evidence until a target-specific JNI/HotSpot packet admits them.

## Challenge/problem evidence

LeetCode, HackerRank and GeeksforGeeks remain category/edge-case/complexity evidence only.
Their solution bodies are not copied through the Apache Synexia delivery lane.

GitHub donor code requires its own pinned repository/revision/path/license decision before
Synexia can independently adapt or reuse it. Only Synexia-owned Apache-2.0 output may enter the
automatic delivery manifest.

## Staging contract

For a pinned manifest and exact Synexia checkout:

1. verify every source file is regular, non-symlink and SHA-256 exact;
2. inspect the current `m3/vendor/synexia` snapshot;
3. compute ADD/REPLACE/KEEP/STALE rows in target-path order;
4. content-address the plan;
5. write ADD/REPLACE candidate bytes only to
   `m3/build/synexia-import/<run>/candidate/<target-path>`;
6. write the plan and root receipt under the same build directory;
7. leave `m3/vendor/synexia` unchanged.

A later source-sealed recipe/promoter may materialize reviewed target changes.

## Contract preservation

This plane changes no:

- `java.*` or `jdk.*` API;
- HotSpot/JNI ABI;
- OpenJDK source license;
- Synexia source bytes;
- existing seed manifest semantics;
- existing `SynexiaImporter.materialize` behavior.

It adds a safer path for newer manifests.

## OpenRewrite relationship

A3 continues to use Maven only as the authoring/proof control plane.
OpenJDK remains built by its native build.

The current OpenRewrite model supports imperative, declarative and scanning recipes; scanning
recipes are the correct shape when a transformation needs repository-wide inventory before
generation. M3 keeps the same inventory-first rule and keeps actual JDK product acceptance behind
the OpenJDK build/test oracle.

## Completion boundary

A staged full Synexia delivery is not an absorbed JEP or JDK enhancement.

Absorption still requires the true scope:

```text
FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API
```

followed by compiler, jtreg, runtime/JNI and benchmark gates where applicable.
