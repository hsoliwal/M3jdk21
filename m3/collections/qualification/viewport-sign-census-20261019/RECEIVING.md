# M3JDK21 viewport sign census and empty JNI selection

Product base: adc10e96af417655a7f1fc61b07c0ca8789c83da, after merged #1473.
Canonical recipe: https://github.com/hsoliwal/com.synexia/pull/12516
Source pin: 1fbc3c61f1b9b5e1fb08fe19979cb6b082591319.
Docs-first: 6e3c68836fc6ef9291b6633ccf3b31394a972935.
Task: synexia-openrewrite-recipes/recipes/widget-backing-20261014/viewport-sign-census-20261019/.
Class: com.synexia.rewrite.M3Jdk21ViewportSignCensusRecipe, using unchanged
M3GpuToolJoinRecipe/Manifest. Synexia provides recipe custody, not runtime dependency.

## Existing product owners only

M3RowTree maintains one private scalar negativeWeights using stored sign bits on
add/reweigh/release/clear at existing callback boundaries. Nonnegative stored
weights prove monotone prefixes even with hidden zero rows. Endpoints use cached
counts without another per-row array. Negative custom height+1 retains the original
descent. Existing array lanes, row identities, callbacks and public/protected APIs
remain; the private field addition is intentional, not a claim of identical layout.

Current m3_index_mask_compactor.c receives the first-party m3_verify_empty helper
from donor49e8bac26e4017a262f18f202298350239f1133b. Legacy/int/long empty paths still
allocate a detached empty result and rescan states, but avoid row payload transfers.
All nine current exports, newer required/excluded-position semantics and nonempty
compaction remain. No JNI signature, object callback, retained pointer or loader change.

## Exact qualification

Two production files and three new tests are exact canonical recipe output; six
KEEP dependencies are source sealed. The kernel runs2111 initial/2122 replay checks
and2048 before/post assignments (including redundant KEEP cases). It is not SDK
visitor execution. Source hygiene precedes strict Java21/C17 compilation, then
actual tests and -Xcheck:jni/fatal UBSan runtime. Public/protected javap surfaces match.

Current sources passed run02 and run03: sign/callback716758 checks, fingerprint
6608799614118242221; mask1204704, fingerprint5092694660038735449; inherited tree
269096, fingerprint16622768763742859435; inherited window6758426. Baseline/candidate
observations agree. Four compiled missing-counter variants fail twice. The native
empty-return fault fails isolated allocation-time drift twice. Missing JNI fails.

The separately labelled actual-C JNI-function-table instrumentation (ASan/UBSan)
measures1539->0 row values, while retaining3078->3078 state values across three empty
selectors. It is not actual JVM execution; checked Java/JNI runs are separate.
Fifteen boundary cases include dense output and injected allocation/read/drift errors.
All189 original command-log hashes across five campaigns verified. Counts are finite
corpus assertions, not independent GUI/JCK tests or whole-module coverage.

## Resource HOLD

Three before->candidate CPU ns/op pairs: hidden endpoints71.876->41.911,
71.589->39.701,97.836->8.214. Mixed108.593->74.939,48.555->63.842,50.070->75.754.
Weight updates10.906->12.152,10.746->10.901,11.111->11.530. One interior pair also
regressed174.070->209.294. Retain these adverse outcomes, not a universal speedup.
Measured owner72bytes both, ten direct arrays totalling1212576bytes at32767rows on
this host. Padding absorbs the scalar here; not a portable object-size guarantee.
Warm query allocation zero both. Full profile/RSS samples are canonical README/raw
archive evidence, not a native GUI or installed-SDK memory/performance claim.

## Remaining gates and reproduction

Local Maven is absent (exit127); local SDK/JUnit NOT_RUN. Canonical path-scoped CI
attempts upstream OpenRewrite8.17.1 and bounded emitted-source JUnit; its observed
result is separate from the default custom8.17.1-synexia-source-1 reactor and full
M3JDK21 module. A queued/failed job is never a pass. Whole module/image/jtreg/JCK,
other native platforms and original SWT/Nebula Display/events/pixels remain NOT_RUN.
Live filtered/sorted wrappers, Skija/RWT/GWT/React and token-aware regex are unchanged,
not completed by these row/index tests. String-active admission is unchanged.

Run python3 verify.py reviewer01 in the pinned canonical task or source archive,
using a fresh campaign name. The extra runner explicitly needs its verified
predecessor and retains its historical Maven-absent environment expectation; see
canonical README for Maven-equipped reproduction. This receiver is not a full JDK
checkout. No root/default-branch write, merge, rebase or force push occurred.
