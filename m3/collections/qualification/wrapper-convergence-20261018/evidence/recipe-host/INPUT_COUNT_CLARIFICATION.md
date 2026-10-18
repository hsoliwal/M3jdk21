<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Input count clarification

This additive note corrects ambiguous count labels in the earlier qualification summaries and progress messages. The 343, 42 and 339 figures reported for `stable-order-groups`, `native-row-suffix` and `index-contract` count the `source_sha256` entries in their orchestration input snapshots. Their exact `SDK_INPUTS.tsv` binding counts are **316, 15 and 315** respectively. The earlier figures must not be read as SDK binding counts.

| Pass | Orchestration snapshot entries | SDK binding rows | Snapshot-only entries |
| --- | ---: | ---: | ---: |
| readonly-iterator | 341 | 316 | 25 |
| stable-order-groups | 343 | 316 | 27 |
| native-row-suffix | 42 | 15 | 27 |
| index-contract | 339 | 315 | 24 |
| index-resource-probe | 339 | 315 | 24 |
| index-junit | 346 | 322 | 24 |
| native-region-proof | 40 | 15 | 25 |

## What each count covers

The orchestration snapshot freezes the SDK inputs plus surrounding qualification material: original receiver files, frozen candidate/oracle copies, plan and packet metadata, host construction/review documents, and unrelated retained fixture inputs. These wider snapshots support the ordered gates' drift checks.

The SDK manifest binds the current pass's host files and resource copies, the parent contract, the four canonical engine/closure sources, and the actual parser classpath when used. The 301-row baseline classpath includes compiled classes and retained resources; it is not a count of 301 Java source owners. The index JUnit lane binds 308 classpath files, including its additional real compiled verification owners and source-owned JUnit dependency jars.

A reconciliation maps every SDK key to its corresponding workspace-relative snapshot key and compares the retained SHA-256 values. For all seven passes, every SDK entry is present with the same hash, with no duplicate SDK keys. The difference consists entirely of snapshot-only entries. Thus the counts describe different inventories; they do not indicate a missing SDK input.

[INPUT_COUNT_RECONCILIATION.tsv](INPUT_COUNT_RECONCILIATION.tsv) records exact counts by SDK prefix, source paths and both manifest SHA-256 values. Counts exclude empty and `#` comment lines; `SDK_INPUTS.tsv` has no header row. Paths in that table are relative to the shared task workspace root.

This note does not rerun or change qualification. All prior input snapshots, SDK outputs, test reports and qualification summaries retain their original bytes. The receiving process should use the exact SDK manifest key set and hashes when verifying SDK custody, and name the orchestration snapshot explicitly when reporting its broader count.
