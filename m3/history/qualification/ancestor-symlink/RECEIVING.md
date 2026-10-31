<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 source-handoff: intermediate symlink refusal

**M3JDK21 is the integrated JDK.** This is an exact two-file original
Python product verification result, not a Synexia runtime layer, a
source-changing recipe or a modified public Java/JNI/HotSpot API.

Canonical PRIVATE source recipe:
[Synexia #13593](https://github.com/hsoliwal/com.synexia/pull/13593),
`com.synexia.m3.M3Jdk21HandoffAncestorSymlink20261027`.

The current-master source predecessor was
`05e947be6fe9a25d3ca0b4803777beebfb52876a`.
The original source-control handoff already passes its strict 5-row
history / one-successor verification in executed product
[run #40444848305](https://github.com/hsoliwal/M3jdk21/actions/runs/40444848305).
The new atom addresses only an untested alias: the original verifier
accepted an in-root **parent directory symlink** whose target remained
within checkout. The final Python path was not itself a symlink,
but the lexical original physical source owner was absent.

Exact source identities:
| Original owner | Base Git blob | Reviewed canonical postimage |
| --- | --- | --- |
| `m3/history/verify_synexia_handoff.py` | `ffe6d2a477a1b051e3320cf4ba2f7aa45426638a` | `c49347c76257b84dbf68d9f8a54a70634d0ee263` |
| `m3/history/test_synexia_handoff.py` | `597062f39cbd159a11d3f6e5b5820ecf8939014c` | `09ae2f61bfc7fccbeb3b6316d642289dbc9cd982` |

Product outputs were written only after canonical Synexia templates, and
both product Git blobs match the templates exactly by remote readback.
Original test methods remain; one negative parent-alias case added.
No original release source, source SHA expectation, historical 5-row
receipt, one-successor receipt, 16 M3File contract registry or protected
workflow changes. No private recipe Java/YAML/templates copied to JDK.

## Gates (fail closed)

- Original nested-parent negative reproduced separately using the
  exact former `checked_path` predicate: **accepted alias (failure)**.
- Original full Python unittest + source history gate at THIS receiving
  head: require actual GitHub Actions run with runner allocation and
  successful steps. Do not infer from source readback alone.
- Private Synexia Java21 Maven/OpenRewrite recipe replay, fixed point,
  missing/duplicate/drift refusal: **not executed** while Synexia
  account runners fail before step 1; see
  [issue #1205](https://github.com/hsoliwal/com.synexia/issues/1205).
- Built installed M3JDK21/JTReg/JCK/JNI/GC/CDS/all-provider
  semantics, cross-platform and CPU/heap/native performance:
  **not qualified by this verifier-only PR**.

Draft and HOLD until source-recipe + product tests execute and pass;
no auto-merge, rebase, protected workflow bypass or unrelated API change.
