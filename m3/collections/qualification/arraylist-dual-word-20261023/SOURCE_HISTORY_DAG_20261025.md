<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source-history DAG audit — 25 October 2026

This is a read-only evidence record, **not** an instruction to merge, replace
JDK public APIs, or waive source/runtime gates. It documents GitHub source
snapshots inspected from `hsoliwal/M3jdk21`, including earlier receipt drift
that preceded the current array-mask recipe.

## M3ArrayList: verified serial source chronology

| Source owner revision | Git blob SHA-1 | SHA-256 | Evidence |
| --- | --- | --- | --- |
| 2026-10-21 indexed live view, commit `8e78b3ae84d34e8e6ab93a056a337c939be2a4b8` | `9cbd15f140ab92dea5d9a83b05b9edddeec2354a` | `4f38bc4801a488c9b3cf1c2ec848a3293ec6b89c91bf01e6f6a9334197bbc0fa` | Immutable historical postimage |
| Collection conflict union, commit `6a2bdd65bd75c2045bc4dfd9cb8027d800451eb4` | `eaada4ef3f198e780dd2a792ec76dddabb8d3616` | `9833b3d8d36287fe38025d9b7c38fb09e495f94f27c611af85433130f1e72ac2` | Commit's *parent* path blob is precisely the previous `9cbd15...`. Existing source-sealed receipt independently records `4f38bc... -> 9833b3...` |
| Canonical dual-license header, commit `104f2f46ced9c6d4389e1ac01cbcaf7a203ec623` | `c43ca5578521b8f0a2ab1305f5d4becdeabc772e` | `884c9078022f64eb3c1f4aa8c306d852521be6089164c7e99b08e17c318d8282` | Commit's parent path blob is `eaada4...`. Every byte after `package com.m3.collections;` is unchanged |
| Current-master mask refinement, Synexia #13319 / M3JDK21 #1881 | `b1135dc23d04d4654cedab2e8b62684e1e7d4471` | `2168db105fe700ea1a8022ad362ee41d8f181180deba8404ea87d9c70f46d134` | Exact current-master dual-license header plus the previously reviewed two-word-mask method body |

The older PR #13222 created an alternate 2026-10-23 branch candidate
`b1315351aafdad389a065f6d5776923b535224cc` from pre-license
`eaada4...`; it is **not** the current-master receiving choice. That
branch remains in Git history, not an automatic license rollback.

## M3ResourceLocks: proven merge/DAG, not a linear successor

| Revision | Git blob SHA-1 | SHA-256 | Distinction |
| --- | --- | --- | --- |
| Original pre-constructor source, commit `c90c531ef5fb24b16eaab5aa9b6c9af298efdee5` | `210e3099f21bfbc5881aa3f53c03d8b04a439714` | Not claimed | Base shared before divergent changes |
| Constructor branch, commit `052b570729f6b0d1b9f0bcb5ef7648c52274302c` | `58bf5ea2d632c71a025460f43d8410fc67df5459` | `18d05ae54945d4129c46d62e26551cbc4fd200b31c185e020b9ee6240fdc9599` | Explicit `public M3ResourceLocks() { }` |
| Deadline scope branch, commit `d2223b9da6a55a8097ea71f856bd86562b9dce83` | `d95147323f3ca5e59073b19a269178d12fe39bc3` | `7d0e50075f26400bfce311919457996468321a987d86cf0145737da9fde024c5` | Commit parent path was **original `210e3099...`**, *not* the constructor branch |
| Restored merged source, commit `2f933b34fec44456c50bd7c7d04486d7f591263e` | `f493038c3829def666664da3c2297fa7e7d877` | `eb0bf36625effaffa147be1f650590dfa08627a5f6f52d105b8fbb201d6b5718` | Commit parent path blob is exactly `d951473...`; the complete source equals the deadline-scope source plus the original **identical 114-byte constructor insertion** |

The constructor restoration insertion, byte-for-byte in both branches, is:

```java
 Preserve the accessible no-argument construction contract explicitly. */
    public M3ResourceLocks() { }
    /**
```

This mechanically verifies **no deletion of the deadline-scope source bytes**
in the restored file and survival of the constructor atom. It does **not**
replace a full Java ABI, JUnit, JNI, GC/JMM or behavioral proof. The original
2026-10-22 handoff receipt remains historical and must not be silently
rewritten to the merged hash.

## Outstanding historical receivers

The six-row pinned `m3-indexed-live-views-20261021` manifest still has
**three** source hashes that differ from current product source:

- `M3ArrayList.java`: verified evolution above, now new source postimage
  `2168db105fe700ea1a8022ad362ee41d8f181180deba8404ea87d9c70f46d134`
- `M3IndexedListSnapshot.java`: old `c59c2aaa40ef12712b7a5c3ab8af0173a63ae6b995fd4b71695bd61e312d7bff`;
  current `43e9eb81a6828137668e597accbe347a315146d278f423e91fab5754af7f1431`
- `M3IndexedLiveView.java`: old `84e9437c27f59060543cd4728a5222b489fe98f7e39068e66f4431b7ee00867f`;
  current `2e5c00296e26186c071fc47d2e3bcd16315825c8404bf271eafa8cd597c268af`

Source-history qualification of the other two paths is now complete at the
Git blob/first-parent level. This establishes **serial source custody**, not
algorithmic equivalence, behavior, installed-JDK acceptance or a waiver of
strict historical pin checks.

### M3IndexedListSnapshot: two checked successor transitions

| Step | Commit | File Git blob | File SHA-256 | Parent proof |
| --- | --- | --- | --- | --- |
| Original indexed-view postimage | `e6fb590b4e5734f3a2878b3325c18b19e5f79850` | `27c56be2bd960ea9de139766ab40960bdbe91b0a` | `c59c2aaa40ef12712b7a5c3ab8af0173a63ae6b995fd4b71695bd61e312d7bff` | Exactly matches original pinned receiver manifest |
| Comparator-preserving groupmask sort | `4a22d79e92819cd8553a62535688d51a375c7e87` | `6a7cbf805ca8aba1c121eab31fce9c682ca62c51` | `09594e9b2116889f9ca4065fabadf4d7dc4e2d9bae8a506d277f49fdc5ea362f` | Parent `517cc01280aa82bc4e2fbdbe32c9a46399050cb4` contains exact prior `27c56be2...` blob |
| Constant-word predecessor groupmask | `db09d4871bd9db9121dde75b0de95464a01b5b74` | `d12e15f41fe17069739a1cdbca1728ee8286d9c4` | `43e9eb81a6828137668e597accbe347a315146d278f423e91fab5754af7f1431` | Parent `183f90adfe65ec6bd11c229038cc1522d6d8f6c2` contains exact prior `6a7cbf80...` blob |

### M3IndexedLiveView: checked callback-epoch successor

| Step | Commit | File Git blob | File SHA-256 | Parent proof |
| --- | --- | --- | --- | --- |
| Original indexed live-view postimage | `96ed69208e05829e2a0c082aab0231a678fe04ec` | `d710423f42f656b6ab5008076b6f3831118b5b12` | `84e9437c27f59060543cd4728a5222b489fe98f7e39068e66f4431b7ee00867f` | Exactly matches original pinned receiver manifest |
| Callback epoch and recursive-view guard | `64f0eddbf64f3d92f42721e629c9a274ea1d0ce4` | `fbaa5cd8b1603c1fbc75e2a6dc4e484bd4c43f0f` | `2e5c00296e26186c071fc47d2e3bcd16315825c8404bf271eafa8cd597c268af` | Parent `fee49d6f6197dc62c35ee741f564a8898cd2c5ba` contains exact prior `d710423...` blob |

**Acceptance boundary:** All three mismatching indexed-view rows now have
explained source-identity successions, including the `M3ArrayList` chain
above. The old six-row manifest must remain immutable and its original
postimage checks must continue to reject nonmatching bytes until an
independent lineage-aware verifier validates the exact full chain, unchanged
public signatures and applicable runtime behavior. The same applies to the
historical `M3ResourceLocks` diamond. No current hash alone proves code
preservation; no JDK or JNI gate is waived.

The newer M3JDK21 #1908 runtime-integrated `java.base` collection/JNI pass
and #1881 standalone collections receiving pass have disjoint current
changed-file sets. This supports separate review but does not authorize
merging or substitute for an installed-image/runtime compatibility gate.
