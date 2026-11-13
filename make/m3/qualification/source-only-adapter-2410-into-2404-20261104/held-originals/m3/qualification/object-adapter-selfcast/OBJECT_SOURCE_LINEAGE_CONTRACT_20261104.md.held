<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

# Object.toAdapter exact source lineage: October 17 → November 2

## Scope and authority

This file precedes an **additive, read-only** lineage verifier stacked on
[M3JDK21 #2395](https://github.com/hsoliwal/M3jdk21/pull/2395), which is
itself stacked on the production draft
[#2394](https://github.com/hsoliwal/M3jdk21/pull/2394).
No existing source, historic manifest, historic verifier, native code,
JDK public API, HotSpot, or release-admission checks may be modified.

The exact existing API is `public <T> T Object.toAdapter(Class<T> clazz)`.
Its erased JNI descriptor `(Ljava/lang/Class;)Ljava/lang/Object;`,
virtual dispatch, null-class NPE, assignable identity, exact
`M3Object.class` capability and unsupported-type null remain unchanged.
No `toAdpater` typo alias or seventh Object vtable entry is admitted.

## Complete immutable source transitions

The canonical original Java source in Synexia was verified by GitHub
connector readback. Both adjoining source images are **exactly** shared
between the two authoritative original Maven/OpenRewrite recipe snapshots:

| Stage | Complete `Object.java` SHA-256 | Git blob SHA-1 |
| --- | --- | --- |
| 2026-10-17 adapter/progress recipe | `829fb37093cf3e8d11b3c1e3d42f898f0c86f3b697e7a66cefe09934be3bb259` | `2bfef3101224112c40957c954592a100dc59a3fc` |
| 2026-10-18 M3Object DOOP/events recipe | `014671527cd647d90f8cab6d6dbd0ca127d918c5a8f38d1ec50be6140eb63735` | `a9899e1c2970f6d5e9172ffb12f5b7399d052b68` |
| 2026-11-02 guarded self-cast recipe | `bd21f3da2ff9f8365576946cfe2d5b5557acaace76de2146277e9d1baa436bd4` | `c703c72bea6e792a378bb6d3babc96f22dbf6cb4` |

Canonical recipe manifests:

- Synexia `m3-object-adapter-progress-20261017/manifest.tsv`
  (included in M3JDK21's original, unmodified historical manifest).
- Synexia `m3-object-doop-events-20261018/manifest.tsv`
  (Git blob `4e15c478161c81aee6538dbe4b183e653172eca2`).
- Synexia `m3-object-toadapter-selfcast-20261102/manifest.tsv`
  (Git blob `080b16eaad789cab14e3303338aed260c56a385d`).

## Mechanical reconstruction procedure

The next proposed first-party JSON source receipt contains **exact,
bounded before/after fragments**, not another full JDK class or a new
adapter implementation. Its two steps cover the original JDK header
annotation, Javadoc, lazy capability branch and later guarded cast.

Start with the **complete current** `src/java.base/share/classes/java/lang/Object.java`.
For each step in reverse chronological order:

1. Check exact **current** full-source SHA-256.
2. Validate the recorded unique replacement region at its stated Unicode
   prefix and suffix boundaries. Reject any fragment drift, overlap,
   missing stage, unexpected count, non-UTF-8 file or symlink.
3. Reconstruct the **previous** complete source by replacing only that
   exact bounded region; verify its full-source SHA-256 and computed Git blob.
4. Reconstruct both older stages. The last must match the original
   historical progress manifest's `Object.java` postimage.
5. Independently verify the JSON ledger's own checksum/format.
   The verifier is read-only and deterministic; no source rewrite is
   permitted.

The verifier must show that exact full-source reverse synthesis produces
the original GitHub snapshots byte for byte and preserves their SHA-256.
Unexpected inputs **HARD FAIL**. Fixed-point and wrong fragment mutation
tests must be executed.

## Significance and non-equivalence

This proves **source lineage/custody for one historical `Object.java`
entry**. It does **not** by itself prove semantic equivalence (the October
18 capability intentionally extends behavior), JVM binary compatibility,
JNI execution or release qualification. The existing
`check_object_adapter_progress_receiver_20261017.py` must remain
unchanged and will still reject the newer source until the complete,
authorized history protocol is composed. The seven other divergent
progress owners retain independent **HOLD** status. No prior PR is merged
or retargeted, no donor code is copied, and no performance claim follows.

## Donor-category decision

The proper reuse authority here is the *existing Synexia source-sealed
Maven/OpenRewrite recipes*, OpenJDK `Object.java`, SHA-256 and Git blob
identity. LeetCode hash-table/cache problems, HackerRank hash query
problems, and GeeksforGeeks probing are **algorithm taxonomy only** and
cannot prove source identity. Importing challenge solutions would be
irrelevant and unsafe for this source-custody leaf.

Strict order: inventory → documentation → immutable fragment ledger →
source validator → negative tests → byte-hash proof → hosted CI →
matched real M3JDK21 Java/JNI tests → release admission.
