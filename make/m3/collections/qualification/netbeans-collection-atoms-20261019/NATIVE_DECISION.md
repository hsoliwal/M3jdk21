<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
## Native zero-mask review: retain the existing owner

The existing JNI mask compactor remains unchanged. Two fixture-only proposals reused
its existing window-selector identity-copy atom: a zero-mask branch around the
shared emission loop, and an early zero-mask copy before the exact original body.
No NetBeans C implementation or additional native ABI was adopted.

Both proposals passed the existing **26,152 JNI contract checks** with fatal UBSan,
real generated headers and `-Xcheck:jni`. The independent early-branch review also
confirmed that removing its guard restores the byte-exact incumbent and that all
nine exported JNI symbols remain identical.

Correctness did not establish resource fitness. Three interleaved release `-O2`
forks per variant used the unchanged diagnostic, 150 warmups and 400 measured calls
for each of 24 whole/range, length and mask cases. At 65,536 rows, the loop proposal
measured CPU ratios of **1.211** for whole-array mask 128 and **1.189** for range mask
255; the early-copy proposal measured **1.216** and **1.168** for range masks 1 and
128. Ratios are candidate divided by incumbent; values above 1 are slower in this
experiment. Zero-mask improvements occurred in several cases, with equal measured
Java allocation across paired rows, but did not establish a neutral nonzero path.

The short, unpinned shared-host probe showed substantial fork variation, including
about 198,000–809,000 CPU ns/call for one unchanged baseline case. The earlier broad
UBSan cost pattern did not reproduce consistently without instrumentation. These
results support **refusing the optimization until its cost gate is resolved**, not
a universal performance ranking. Full source, command, raw measurement and refusal
receipts remain in the canonical evaluation evidence; no further tuning was admitted.
