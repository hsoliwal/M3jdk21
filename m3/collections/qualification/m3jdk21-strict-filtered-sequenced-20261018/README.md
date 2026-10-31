<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 strict filtered SortedSet / NavigableSet Java21 receiver

**M3JDK is the product runtime**. Synexia is the source of reusable transformation
recipes and donor/category evidence; **no Synexia runtime dependency**.

Canonical recipe-first [Synexia PR #12281](https://github.com/hsoliwal/com.synexia/pull/12281)
source commit `4eb4bd87fe8c9526e377efc1afd2401ba4f1e1a2`.
Original M3JDK21 master source blob: `6ec7a88dafeca87760faee0c808dbd9a93ba1424`, strict preimage SHA256:
`1061f6a0c789082c7e86078c7ea3c48b97b804150a7ab96c2ff8e28ff6786369`. Generated owner post SHA256:
`dd76093f07091d7224d9f4ec9f7d3a84271c3eef858186ff5d7de9b25158fe82`.

This receiver does **not** import OpenJDK, Guava, Apache, Eclipse, Spring,
their adapters, or source from LeetCode/HackerRank/GfG.
No public M3 interface, JNI ABI, payload owner, array/index/mask algorithm,
sorted comparator or predicate meaning is changed.

## Exact behavioral atom

Java21 default `SortedSet.removeFirst/removeLast` searches for a member
before removal, reporting `NoSuchElementException` for empty filtered views.
M3JDK's explicit strict read-only invariant instead refuses attempted
mutation with `UnsupportedOperationException`, even on empty ranges.
The stock reverse SortedSet may likewise re-expose inherited defaults.
The existing `SortedSetView` now uses its own backing-indexed reversed
wrapper (no materialized element array); `NavigableSetView` delegates to
its established descending view with the precise covariant return type.

The strict refusal atom executes no user predicates, and all non-mutating
sorted/navigable operations remain live and comparator-aware.

## Reproduction and verification

```sh
python3 m3/collections/qualification/m3jdk21-strict-filtered-sequenced-20261018/verify.py
mvn -B -ntp -f m3/collections/pom.xml -Dtest=M3StrictFilteredSequencedTest,M3FilteredViewsTest test
```

`manifest.tsv` and `templates` are byte-identical to exact Synexia source
resources, not a second independent mutating recipe engine. `verify.py`
refuses product/template drift and checks the historical source preimage.
The source crate uses the canonical Maven/OpenRewrite Java LST engine,
drift/occupied-path refusals and fixed-point JUnit replay.

**DRAFT/HOLD**: a Git commit or queued Actions workflow is not proof.
Only completed source and receiver job logs can promote this change. The
Java/JNI installed-JDK/JCK/JTReg and performance families remain separately
gated; they are not claimed complete in this Java wrapper pass.
