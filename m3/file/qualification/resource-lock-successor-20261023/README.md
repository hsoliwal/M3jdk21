<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 resource-lock source: immutable historic handoff + sealed successor

**Status: source-only review; canonical SDK execution PENDING; full JDK HOLD.**
M3JDK21 is one integrated JDK, not Synexia + JDK.

This product PR is stacked on [M3JDK21 #1887](https://github.com/hsoliwal/M3jdk21/pull/1887)
head `0881a7c17467a48da351d6cce71fead76ff94669`. The actual
Java21 host + linked-image M3File runs passed on that parent head,
including all 13 contract mains and 140,034 original resource-lock
checks. Product code does not change here.

The canonical private source recipe and memory remain solely in
[Synexia #13236](https://github.com/hsoliwal/com.synexia/pull/13236),
`com.synexia.m3.M3Jdk21ResourceLocksTimedCtorSuccessor20261023`.
M3JDK21 receives **only** the executed-source candidate identities,
one passive successor TSV and an original-source preservation workflow
update; no OpenRewrite source, template, runner or duplicate lock owner.

## Exact source genealogy (do not overwrite)

| Historical atom | Source identity |
| --- | --- |
| Frozen first handoff | `m3/docs/synexia-m3jdk21-eclipse-recipe-handoff-20261022.tsv`, Git blob `4dfffd4cae11edf342b801ca3c007c43a6137979`, 5 rows |
| Explicit public constructor | Source recipe Synexia #12951, historical expected SHA-256 `18d05ae54945d4129c46d62e26551cbc4fd200b31c185e020b9ee6240fdc9599` |
| Timed acquisition | Synexia #12842 and M3JDK21 #1745, pre-union SHA-256 `7d0e50075f26400bfce311919457996468321a987d86cf0145737da9fde024c5` |
| Existing combined original class | SHA-256 `eb0bf36625effaffa147be1f650590dfa08627a5f6f52d105b8fbb201d6b5718`, Git blob `f493038c3829def666664da3c2297fa7a0e9d877` |

The existing `com.m3.file.M3ResourceLocks` is **exactly** the older
timed-acquisition source plus the original accessible public
no-argument constructor declaration and documentation. No other source
bytes differ. It keeps path/provider lexical identity, nested/scope
exclusion, bounded timed wait, interruption/cancellation, active-lease
reclamation and LIFO rules. Do not revert it or introduce another
FileSystem, scheduling service, retained per-scope Node/Entry or JNI ABI.

## Unchanged original historical provenance

The five historical rows are unchanged, and four product sources still
match their original SHA-256 bytes. For this one changed implementation,
the existing merged-preservation workflow accepts **only** the one
fully pinned canonical successor, exact SHA-256 and Git blob; all
other changes and unknown successors still fail closed. It checks that
the old lock row still contains its old recipe ID and old SHA.
No generic `skip`, `allowAll`, source-identity bypass, or changed
licence/pull/state is admitted.

The product source remains untouched; this is a source-handoff
integrity repair. New passive TSV and workflow outputs are sealed in
private Synexia; target Git readback is required.

## Verification and promotion

- Existing #1887 Java21 M3File host + jlink contract proof: PASS
  (13 contract classes including `M3ResourceLocksContract` and
  `M3ResourceLocksTimedContract`).
- New combined history successor source script and 12-row preservation
  workflow: **NOT RUN** at receipt creation.
- New canonical Synexia Java/text OpenRewrite SDK fixed-point, drift/
  missing/duplicate refusals: **QUEUED** at receipt creation.
- Installed source-built **M3JDK21** module image, JTReg/JCK,
  HotSpot/JNI/GC/CDS/JVMTI, platforms and CPU/heap/native-process
  measurements: **NOT RUN/HOLD**.

No automatic merge, develop rebase, public recipe duplicate or
String→arrays→collections phase advancement.
