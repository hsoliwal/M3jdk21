<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original owner: atomic seven-array growth + fused views

**Status: candidate-only; SOURCE_GIT_RECEIPT_PENDING; SDK_EXECUTION_NOT_RUN;
FULL_INSTALLED_M3JDK21_HOLD.**

This work belongs inside **M3JDK21 itself**, not to an external M3 collection
layer. It builds upon the original JDK owner in [#1872](https://github.com/hsoliwal/M3jdk21/pull/1872)
and receives two already qualified internal fused state-mask methods from
[#1804](https://github.com/hsoliwal/M3jdk21/pull/1804). The sole canonical
source-changing recipe and test fixtures are in private Synexia
[#13220](https://github.com/hsoliwal/com.synexia/pull/13220).

## Exact provenance and intended product outputs

| Target | Before SHA-256 | Reviewed after SHA-256 |
| --- | --- | --- |
| `src/java.base/share/classes/jdk/internal/m3/M3IndexedArray.java` | `90f9c573c48319d1811fd5a663d6690b075c45ab257c8a80256f9beccdf6b791` | `0c79f59b111bc1669a626c4a45598b7205319522b0c00c56ef009a8b9ea660e5` |
| `test/jdk/jdk/internal/m3/M3FusedMaskedFilteredSortedViewTest.java` | ABSENT | `1c599c9f7a8dd98dd1c6b067c7703105923914fa9a86082899a133993ed1bf97` |
| `.github/workflows/m3-indexed-array-atomic-growth-20261023.yml` | `a7a34d1f18a6a93252d8024139e0795f303df71d05b2b2c1a2189daf377fa5d0` | `119063f62e62cc966dc8aae6de83b50e6d15d694e7b8140b809b2127a355f36e` |

The original Java owner retains all seven `Object[]/int[]` array planes,
generation-qualified handles, lease/state masks, logical/physical offsets,
and failure-atomic allocation publication. The master stable index
`System.arraycopy` strategy is retained byte-exact: **do not overwrite
it with the separate older threshold=16 variant**. Only
`filteredMaskedStates` and `filteredMaskedSortedStates` are inserted
before the existing private `checkMasks` atom. No standalone runtime,
JNI symbol, per-element Entry/Node store or new exported `java.*` type exists.

## Exact contract coverage

The existing `M3IndexedArrayGrowthAgent`/fault/owner tests are retained:
seven `Arrays.copyOf` failure sites at each of three capacities, original
JDK Java21 `-Xint` and `-Xmixed`, stale handles, leases and recovery.
The existing fused selector test is reused **unchanged**, including
predicate/state-mask short circuit, stable comparator ties, repeated state
changes, overlapping-mask and null refusal, and reentrant callback rejection.

The same original `jdk.internal.m3.M3IndexedArray`/`M3IndexedView`
classes are used throughout; Java owns Object references and callbacks,
JNI remains behind existing bounded primitive-mask methods elsewhere.

## Proof gates

- Existing independent #1872 host Java21 21-site allocation fault CI: PASS
  on #1872 exact head, **not** this combined head.
- Existing independent #1878 original Java/C JNI and filtered/sorted CI:
  PASS on #1878 exact head, **not** this combined head.
- Source-scoped combined exact SHA Java snapshot:
  AWAIT real Synexia #13220 SDK execution and readback.
- Combined Java21 patched `java.base` class + regression CI:
  NOT RUN at receiving-document creation.
- M3JDK21 source-built image/jtreg/JCK/HotSpot/GC/CDS/JNI-platform,
  ABI/JMM, five-provider contract matrix and comparable CPU/heap/native
  resource costs: NOT RUN.

The target's canonical `m3/docs/name-mapping.json` remains the naming
authority. Its legacy owner mapping must be reconciled to a stable
`migration.records[].id` before final promotion; no second mapping
registry is created here. Keep this PR a draft while any gate is HOLD.
