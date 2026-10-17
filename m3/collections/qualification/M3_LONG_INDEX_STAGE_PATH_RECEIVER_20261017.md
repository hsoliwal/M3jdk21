<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 sorted index-path full-width mask receiving packet (2026-10-17)

**Disposition: ADDITIVE_COLLECTION_CANDIDATE / HOLD_UNQUALIFIED.**
No `java.base` files changed; no M3JDK21 receiving phase advanced from STRING.

## Canonical recipe and exact custody

Reusable recipe owner: `hsoliwal/com.synexia` draft
[PR #12106](https://github.com/hsoliwal/com.synexia/pull/12106),
`com.synexia.m3.Jdk21LongIndexStagePath20261017`.
It composes the existing `M3HashPinnedJavaSnapshotRecipe` and
`M3HashPinnedTextSnapshotRecipe`. Templates and both manifests live in
Synexia; these target files are candidate postimages, not a second
canonical recipe owner.

| Target | Exact preimage SHA-256 | Candidate postimage SHA-256 |
| --- | --- | --- |
| `M3LongIndexProjection.java` | `a69694a2ce22ca9599cb665e4845f5dee75d527b7c144fa3ea7fb4518ec2d3fa` | `b923b9857f6302ed529fa4b3a4415b8f3da0f1b42701ff2b4a3d8f89854fd5f0` |
| `M3LongIndexProjectionPathMasksTest.java` | ABSENT | `239c6662ba7f91c786df60a9faa19633844b20408c972f88f381a7a241a49847` |
| `verify_donation.py` | `f357639d9550477da9bfdab10d5619a8e71b1642941686ef19ce3c9b11717a7f` | `301e13cc694c20dd78123987f3395610be0451e4ec22a449b1bfa311d3a01d0e` |

Receiver manifest SHA-256:
`37686ec2db3a8570de182768bc166384322e045a0419a0bf4e38f415ce8bf1e2`.

`M3IndexPathMasks` Java/C17 JNI code is **unchanged**, not copied or
forked. The projection's private immutable physical `int[]` directory
serves as both ordered rows and physical offsets. The selected result
owns only its new primitive index directory, retaining the original
immutable value backing.

## Mechanical contracts

- Full signed 32-bit required and forbidden masks, left-to-right stages,
  including sign bit, zero and conflicting masks.
- A path is relative to the current sorted/sliced/previously-filtered
  logical projection; invalid geometry refuses before JNI.
- Negative stride reverses ascending/descending order metadata, without
  claiming stable tie order under reversal.
- Repeated values retain separate physical positions.
- No retained caller mask array, Java object per row, native pointer,
  callback into JNI or additional JVM-wide state.
- JAVA remains the default; explicit NATIVE_REQUIRED refuses if the
  existing library cannot load. No silent fallback or speedup claim.
- Live filtered `Collection/List/Set/Map` wrappers remain distinct and
  retain predicate side effects, mutability and comparator semantics.

## Run the gates

```sh
python3 -m unittest discover -s m3/collections -p 'test_verify_*.py' -v
mvn -B -ntp -f m3/collections/pom.xml -Dtest=M3LongIndexProjectionPathMasksTest test
# Linux, pinned JDK21/JUnit, GCC, existing JNI region library:
python3 m3/collections/verify_donation.py --cost-probe
```

The installed-source verifier now admits **only** the exact successor:
one preimage-to-postimage replacement and one ABSENT new test. All other
previous source hashes and source-set completeness checks remain
unchanged. Its standalone `test_verify_long_stage_path.py` proves
matching bytes plus corrupt-manifest and corrupt-source refusal.

**Evidence disposition at PR publication:**
source/template equality: VERIFIED_BLOB_BYTES; source-seal named recipe
authored: YES; OpenRewrite SDK execution: NOT_RUN; Java21/JUnit target
module proof: NOT_RUN; loaded JNI `-Xcheck:jni` staged-path parity:
NOT_RUN; full OpenJDK jtreg/HotSpot: NOT_RUN; cold/warm CPU and retained
heap: NOT_RUN.

No unsupported JDK compatibility, memory or performance claim is made.
Only actual tool outputs may change a gate from NOT_RUN to PASS.
