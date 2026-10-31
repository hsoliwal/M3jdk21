# Threshold-aware removal-run counting: REJECT_COST_GATE

The existing `jdk.internal.mindex.M3CollectionMaskCompactor` remains the runtime authority.
The proposed runtime postimage is rejected evidence, not a receiving payload.
No receiving source, public API, JNI symbol, native reference transfer, commit or phase status changed.

## Proposal and contract argument

The existing caller only compares the private run count to `max(4, length >>> 7)`.
The evaluated atom returns early once a monotone partial count exceeds that threshold.
Below or at the threshold it still counts the complete valid mask. Thus it chooses the
same scalar/span algorithm as baseline. All validation, survivor writes, run copying,
reference order and discarded-tail behavior remain unchanged. The exact GPLv2/Classpath
owner header is retained. No external algorithm implementation was copied.

The actual current ArrayList.removeIf and synchronized Vector.bulkRemove source call
this owner only for deletion windows at least 512 elements, after callback evaluation
and the original modCount check. Those callers and their clearing/shifting logic were
not modified. The preexisting NetBeans catalogue's rejection of span-copy replacements
remains in force; this experiment changes only strategy-decision work.

## Functional evidence

Strict Java 21 `-Xlint:all -Werror` compilation succeeded for the baseline/candidate owner
and the unchanged receiving ArrayList/Vector sources via `--patch-module java.base`.
Both variants passed:

- New independent stable-filter oracle: 199,294 cases. Exhaustive masks of lengths 0–16;
  all 65,536 sixteen-bit patterns embedded across a word seam in 513-element windows;
  threshold-1/threshold/threshold+1 cases;511/512/513 and64-bit boundary lengths;
  large random masks; nulls and unique reference identities; entire-array comparison
  including discarded tail and outside-window state; mask immutability; validation order.
- Existing M3MaskCompactionDifferential: 13,493,171 checks.
- Existing M3JdkRemoveIfMaskContracts: 3,019,650 checks, covering real patched ArrayList,
  nested backed sublists, Vector, predicate effects/throwing callbacks, reentrant mutation,
  exception behavior and iterator modification detection.

Class-load logs explicitly show ArrayList, Vector and M3CollectionMaskCompactor loading
from the corresponding compiled patch directory. These are direct test-main runs on
stock Temurin 21.0.12.1 with exact receiving source patches, not a jtreg harness or a
source-built complete M3JDK21 image pass.

## Cost evidence and refusal

`benchmark/SUMMARY.json` and SUMMARY.tsv retain all per-workload results. Five included
paired JVM forks per variant cover lengths 512, 8,192 and 131,072 with eight mask shapes and
five approximately 50 ms samples per shape. Variant order alternates. Each timed operation
restores its Object[] using arraycopy, compacts it and consumes the end/last survivor
identity. Inputs are restored inside the timing in both variants; these are component
costs including restoration, not a complete removeIf/IDE benchmark. JVM flags fix 512 MiB
heap and G1. This is an explicit local diagnostic microbenchmark, not JMH.

Exploratory fork 0 overlapped a functional test process and is excluded, retained and
replaced with fork 5. Included forks are 1–5. Other host activity and CPU affinity are
uncontrolled. Fork ranges are broad and often cross 1, so these measurements do not
establish universal speedups or regressions.

The geometric mean of 24 median paired baseline/candidate ratios is 1.00677: approximately
0.7 percent, insufficient for a material overall improvement. The six alternating/random
workload medians have a median ratio 1.03979, but gains are inconsistent across lengths
and forks. Four nonfragmented control medians exceed 5 percent slowdown: sparse at 8,192,
no deletions at 131,072, cluster at 131,072 and threshold at 131,072. Their ranges are noisy; that
uncertainty itself leaves the required no-regression cost gate unresolved.

Disposition: REJECT_COST_GATE. Preserve the existing runtime implementation. Keep the
stronger oracle as an independently useful test-only candidate and return this rejection
to the existing convergence/category catalogue. Do not reinterpret a finite semantic
pass as permission to admit an unproven performance change.

## Files and remaining gates

REJECTION_RECEIPT.json binds source hashes and scope. The rejected implementation is
`evidence/rejected-threshold-exit/M3CollectionMaskCompactor.java.txt`.
The sole candidate receiving payload is test-only:
`payloads/M3MaskThresholdContracts.java.txt` ->
`test/jdk/java/util/ArrayList/M3MaskThresholdContracts.java`.

Canonical recipe application, refusal/fixed-point verification, actual jtreg execution,
whole-runtime image qualification, hosted checks and any applicable full-image gates
remain unexecuted. No JNI or performance admission follows from this packet.
