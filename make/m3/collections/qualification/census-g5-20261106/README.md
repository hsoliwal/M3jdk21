<!-- SPDX-License-Identifier: Apache-2.0 -->
# Census group G5: circular FIFO ring shift (L079)

Census leaf L079, crate `m3-fork-ring-shapes-20261019` (com.synexia develop `ca1aafc4d94f`), owner `CircularFifoQueue`
in the commons-collections 4.5.0 fork (`implementations/commons-collections-4.5.0`).

| row | verdict | evidence |
|---|---|---|
| `CircularFifoQueue` bulk-shift iterator removal (census PARTIAL, 82 lines) | REJECTED_BY_DONOR, not applied | the crate rejects its own candidate: `verification-receipt.json` `candidateDisposition: REJECTED_PERFORMANCE_REGRESSION`, and the postimage header reads "bulk-shift experiment for iterator removal; rejected, not applied"; the owner has since received its own bounded contiguous-span compaction |
| `M3CircularFifoShiftProof` | PORTED (fork test oracle) + WRAP `M3CircularFifoShiftProofTest` | the oracle was written to run unchanged against baseline and candidate; donor baseline = donor candidate = relocated-on-owner outputs IDENTICAL, `PASS M3_CIRCULAR_SHIFT checks=20939053` |
| `BoundedCollection` | parse context (not a postimage) | crate `parse-context.tsv`; census APPLIED_SUPERSET |

Recipe `relocate.py` (RELOCATE_CENSUS v4; targets relative to m3/collections), `RELOCATION_MAP.tsv`, `PROVENANCE.tsv`;
proof `differential.sh`, `GATES.tsv`, `CENSUS_RERUN.tsv`, `RECIPE_MEMORY.tsv`.
