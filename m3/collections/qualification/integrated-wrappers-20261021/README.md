<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Integrated M3JDK21 collection refinement: final v3

This change refines existing M3JDK21 Java/JNI owners through the canonical
Synexia recipe. It contains four Java modifications and eight additive
contract/resource fixtures. Public interfaces, JNI declarations, callback
order/count, exceptions, stable ordering, aliases and ownership remain unchanged
within the qualified component scope. The final native compactor and bitmap
snapshot retain their incumbent bytes after the measured alternatives were
rejected.

[SYNEXIA_HANDOFF.json](SYNEXIA_HANDOFF.json) pins the exact canonical commit,
recipe manifest, SDK emission and source-license custody. Synexia owns the
reusable recipes, fixtures, catalogue and correction memory. M3JDK21 owns the
integrated runtime and its receiving qualification. The initial plan and contract
are frozen design history; this README and [QUALIFICATION.json](QUALIFICATION.json)
describe the final selected result.

## Runtime changes

| Owner | Selected refinement |
| --- | --- |
| `jdk.internal.m3.M3IndexedArray` | Copy private unfiltered index ranges under the existing monitor; bulk-copy only unopposed stable-merge tails. Preserve callback, epoch and tie behavior. |
| `jdk.internal.m3.M3IntAlgorithms` | Return a fresh complete lonely-number result directly; retain trimming for partial results and preserve signed-domain behavior and result independence. |
| `java.util.CollectionViews` | Use the same backing iterator directly for filtered `isEmpty`, preserving predicate demand, short circuit and exception behavior. Existing sorted-wrapper contracts remain covered. |
| `com.m3.collections.M3ArrayifiedIndexOps` | Reuse the prior detached rotation, stable merge-tail and owned JNI-result remapping atoms, requalified against the current source closure. |

Every source/test change comes from the actual OpenRewrite SDK emission.
[GENERATED_SOURCES.json](GENERATED_SOURCES.json) records the exact twelve paths,
before/after SHA256 values and Git blobs. Ten other owners remain sealed KEEP
inputs. The 331 original collection tests and protected core tests are unchanged.

## Executed qualification

| Gate | Result |
| --- | --- |
| Canonical OpenRewrite SDK | 43 tests passed, zero failures/errors/skips. All sixteen MODIFY states in both input orders are exercised through the SDK. |
| Fixed point | Twelve first-run changes; zero second-run changes. Separate disk readback has zero changes on both executions. |
| Core component | Sixteen commands passed; twelve behavioral/API transcripts match baseline and the prior qualified candidate, including interpreted runs. |
| Collection SDK | Strict compilation of 282 main Java files and 336 tests; all 578 JUnit tests passed with zero skipped, aborted or failed. |
| API/JNI boundary | Nine SDK direct/API tapes and all twelve generated JNI headers match the protected baseline. Final SDK executions require checked JNI and the incumbent native library closure. |
| Native candidate experiments | Both retained alternatives passed their separate functional, ABI, model, UBSan and real-JNI checks. Performance admission failed, so neither changes the final production C source. |

The installed core mask-window JNI test remains explicitly NOT_RUN: the
historical source-built image lacks the required exports. Its Java path ran.
The separate actual collection JNI qualification does not close that image gate.
See [qualification/final-core-v3/CORE_NATIVE_IMAGE_HOLD.json](qualification/final-core-v3/CORE_NATIVE_IMAGE_HOLD.json).

## Resource decisions

The Java candidates provide measured allocation reductions in the recorded
finite workloads. At 4,096 entries, the dense lonely-result case decreases from
35,904 to 19,504 bytes per operation; native index selection with step one
decreases from 36,912 to 20,512. Filtered first-hit `isEmpty` decreases from 64 to
32 bytes per operation. Sorting allocation is unchanged. Some CPU observations,
including sorted-refresh32 and dense4096, remain mixed; these measurements do not
establish a universal CPU improvement.

Native prefix trimming v1 produced eighteen CPU flags in its 196-case matrix.
The pointer-helper revision v2 produced twenty-five, including six changed
nonempty cases and nineteen controls/empty paths. Control variability limits
causal interpretation and does not waive the frozen rule. The instrumented
transfer reductions belong to these rejected experiments. The final native
source is unchanged. The optional occurrence bitmap also retains a confirmed CPU
veto across two complete cohorts, despite allocation savings; its owner is KEEP.

The [searchable catalogue](../../algorithm-catalog/integrated-wrappers-20261021.tsv)
records problem categories, donor comparisons, exact candidate identities,
contracts, complexity and selection rationale. [source-coverage.tsv](source-coverage.tsv)
enumerates 299 production owners, distinguishing six operation/donor-reviewed
owners, seven sealed KEEP co-input owners and 286 compiled dependencies. It does
not claim individual algorithm review of every dependency or the whole JDK.

## Evidence and remaining receiving gates

Visible receipts retain original task paths under `qualification/`. The complete
raw measurement JSONL is published as ordered checksum-verified parts under
`evidence/`, described in `EVIDENCE_HANDOFF.json`; its exact hash,
record count and canonical reconstruction location are in
[EVIDENCE_HANDOFF.json](EVIDENCE_HANDOFF.json). The canonical packet retains all
original/rejected source images, failed invocations, commands, raw CSV/XML/logs,
assembly observations, independent audits, provenance and replay adapters.
No binary caches or toolchains are embedded. Original notices and donor source
identities are preserved; no challenge/editorial/submission bodies are copied.

The PR remains a draft while the full matching current-source JDK image,
installed core native-window exports, applicable jtreg/JCK/platform/provider
matrix, unchanged owning Maven reactor with source-built dependencies and the
existing `CollectionViews` core integration-license review remain open.
Component success does not advance those receiving gates. The prior index
refinement PR and its exact reusable atoms remain preserved in the handoff.
