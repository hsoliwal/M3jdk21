<!-- SPDX-License-Identifier: Apache-2.0 -->
# CPU mask-run tool — source-sealed Maven/OpenRewrite candidate

This is a bounded CPU operation for M3JDK21, not an implementation of every CPUTOOLS category.
Canonical receiver: `m3/collections`. The pre-existing `M3IndexPathMasks` owns selection
geometry, Java paths, and optional NATIVE_REQUIRED C17/JNI. This recipe adds only
`M3CpuMaskRuns` and an adversarial JDK21 self-test as two ABSENT-before candidates.
The source paths and postimages are recorded with full SHA-256 in `manifest.tsv`.

## Contract
- Caller supplies dynamic-array row IDs, physical offsets, per-plane full-int state masks.
- Runs group consecutive *selected row IDs*, including duplicates and negative stride.
- One caller-selected `maxRuns` budget; scratch is a 4096-element primitive array.
- Empty paths and budgets still validate geometry/JNI backend admission.
- No per-element node, String, re-entrant Java predicate or native buffer identity retained.
- A later failure may occur after earlier callbacks; no transactionality promised.

The Java source and immutable recipe postimage share exactly the same Git blob.
JNI reuses the existing `M3IndexPathMasks` backend; there are **no new native symbols**.
No donor implementation is copied. Existing M3JDK21 code is Apache-2.0 when marked;
OpenJDK and its licenses are not modified by this independent module.

## Verification
Run existing Maven/OpenRewrite recipe host from the collections module and inspect
generated diffs; run `M3CpuMaskRunsSelfTest` using the existing JDK21 collections classes.
Head GitHub Actions result, complete Maven reactor, real JNI parity and JDK image
qualification are required before promotion. A source snapshot/manifest alone is not PASS.
