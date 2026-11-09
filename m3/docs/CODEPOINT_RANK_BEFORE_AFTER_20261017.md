<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Measured before/after: M3 String code-point offset rank/select — 2026-10-17

This is a concrete product source comparison, not a cross-JDK marketing claim.

## Input identity and scope

- Baseline: M3JDK21 \`master@970a34350ebdf6a3f1c5ff5acc8445d0f3515f55\`
  \`src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java\`
  SHA-256 \`50e1fe55cd5e8a53b72aa8a4772f826f712c7e3edb3239514f9504819a0b0257\`.
- Receiver: canonical [Synexia #11966](https://github.com/hsoliwal/com.synexia/pull/11966)
  Java-snapshot recipe, post SHA-256 \`43f65f6e578dc5b3ad59046a52ea70bffae3bbff855c861bc2ffd24d1f4bf3d2\`.
- Same Java 21 runner (GitHub Actions \`38998405911\`), precomputed geometry objects,
  no extra character copy or M3 runtime state in the timed method. Rerun locally with:
  \`python3 m3/runtime-integration/codepoint-rank/measure_before_after.py\`
  using a checkout that contains the baseline Git commit.
- **Exact source extraction:** test loads baseline bytes via \`git show <master-baseline>:<source>\`,
  extracts the pre-existing \`Geometry\` and \`lowerBound\`, then repeats for receiver
  source, verifies both full-file SHA-256s, compiles them with stock \`javac --release 21\`.
  This avoids using a hand-written or merely simulated baseline algorithm.

## Before: nested searches

\`\`\`java
int low = index, high = utf16Length;
while (low < high) {
    int middle = (low + high + 1) >>> 1;
    if (codePointCount(index, middle) <= target) low = middle;
    else high = middle - 1;
}
return low;
\`\`\`

\`codePointCount()\` binary-searches \`pairedLowOffsets\` to count the paired low
surrogates inside each new range. The outer binary-search does this repeatedly.

## After: direct monotone rank/select

\`\`\`java
int first = lowerBound(pairedLowOffsets, index + 1);
long threshold = (long) index + codePointOffset - first;
int low = first, high = pairedLowOffsets.length;
while (low < high) {
    int middle = (low + high) >>> 1;
    if ((long) pairedLowOffsets[middle] - middle <= threshold)
        low = middle + 1;
    else
        high = middle;
}
return index + codePointOffset + (low - first);
\`\`\`

This is the forward path; negative offsets use the symmetric bound. A paired-low
at the starting boundary must **not** be miscounted: an offset starting inside a
surrogate pair traverses the low/high unit independently. The after-code keeps this
Java \`Character.offsetByCodePoints\` contract.

Complexity improves from \`O(log N × log P)\` to \`O(log P)\`, with N UTF-16
code units and P paired-low offsets. Cache size is unchanged. The sidecar
construction also reuses the existing tuple-aware \`M3String.CodeUnitCursor\`,
which resolves scalar leaves in long runs; that *construction* speed has not
been measured by the following benchmark.

## Measured warmed offset-only A/B, same runner

Values are **median nanoseconds per query** from nine runs, 65,536 mixed
forward/backward valid code-point offsets per measurement and alternating A/B
order. Fresh allocations to construct precompute geometry, cache-miss effects,
VM bootstrap and String routing are *not included*. Raw timing arrays are in
[the completed GitHub Action](https://github.com/hsoliwal/M3jdk21/actions/runs/38998405911).

| Dataset | Before ns/op | After ns/op | Measured speedup |
| --- | ---: | ---: | ---: |
| ASCII, 512 units | 17.380 | 5.730 | 3.033× |
| Dense pairs, 512 units | 582.329 | 47.103 | 12.363× |
| Sparse pairs, 4,096 units | 649.739 | 42.623 | 15.244× |
| Dense pairs, 4,096 units | 1,103.730 | 107.591 | 10.259× |
| Sparse pairs, 32,768 units | 1,225.952 | 91.163 | 13.448× |
| Dense pairs, 32,768 units | 1,695.305 | 167.732 | 10.107× |

Acceptance evidence from this hosted run:
- \`M3_BA_DIFFERENTIAL_PASS checks=6144\` compared both extracted algorithms
  against \`Character.offsetByCodePoints\`.
- \`M3_CODEPOINT_RANK_EXTRACTED_JAVA_PASS checks=1066861\` additional source-extracted
  differential oracle.
- \`M3_CODEPOINT_RANK_CURSOR_PASS checks=963597\` ran new jtreg fixture
  as a stock-Java-21 test harness, not on the modified M3 runtime.
- Workflow completed **SUCCESS** on exact branch head with the A/B measurement.
  Raw timings for short ASCII have more noise and are not substitutes for JMH.

## What is NOT measured or qualified

These are **isolated warmed method results, not a whole-application 3–15× acceleration**.
Still pending: actual M3-enabled \`java.lang.String\` dispatch and cache hit rate,
precompute first-use cost, full modified JDK build, jtreg/JCK, interpreter/C1/C2
comparisons, native/JNI ownership lifetime, GC/CDS and representative multi-run JMH
throughput/allocation/RSS under memory pressure.

Do not promote the product release contract or close the M3 String phase based on
this one bounded proof. Keep PR as draft.
