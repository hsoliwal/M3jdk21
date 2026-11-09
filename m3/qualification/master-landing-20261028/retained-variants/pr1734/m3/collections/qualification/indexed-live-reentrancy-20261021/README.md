<!-- SPDX-License-Identifier: Apache-2.0 -->
# Integrated M3JDK21 indexed live List preparation repair — candidate receiver

**M3JDK21 is one modified JDK/HotSpot/JNI SDK, not an M3 runtime overlay.**
This modifies the existing parent-backed `M3IndexedLiveView` implementation
and its original Java21 oracle only. The backing dynamic Object[] stays owned
by M3ArrayList; the view owns compact physical int[] positions. Neither the
JDK collection APIs, existing JNI mask selector, native symbols nor the
producer/consumer object identity contract is replaced.

## Reusable recipe FIRST
[Synexia #12818](https://github.com/hsoliwal/com.synexia/pull/12818)
is the canonical Maven/OpenRewrite owner, revision `aa461b00e1b84a4d15013bd66de717cb09fa48b7`.
The exact original Git blobs and SHA-256 source/postimage manifests appear
in `SOURCE_PINS.tsv`. Each source owner is received serially from the
`M3HashPinnedJavaSnapshotRecipe` candidates. Unknown original bytes REFUSE.

## Behavior
- Prevent recursive `refresh()` from a predicate or comparator. Do not publish
  half-built physical indexes and always clear preparation guard in finally.
- Capture explicit view invalidation epoch as well as original M3ArrayList
  structural/value epochs. Invalidating in a callback aborts preparation
  before index publication.
- Preserve existing stable primitive mergesort, JNI byte-state selection,
  read-only List behavior, null/duplicates, listener callback order,
  physical indexes, iteration and original native required/refusal paths.

## Earlier receipt continuity
PR [#1709](https://github.com/hsoliwal/M3jdk21/pull/1709)
retains six historical immutable postimage templates.
`SUCCESSOR_RECEIVERS.tsv` identifies only the two newly admitted Java
postimages by SHA-256. The old verifier's correction is separately
source-sealed by `com.synexia.m3.IndexedLiveVerifierSuccessor20261021`
in the same Synexia review. No earlier receipt is rewritten as if the
original product source never evolved.

## Reproduction, explicit limits
`bash m3/collections/qualification/indexed-live-reentrancy-20261021/verify.sh`
checks the source seals, compiles the existing original Java21 collection
owners and reruns the Java oracle in normal and interpreted modes.
The actual Maven/OpenRewrite SDK, installed M3JDK21/HotSpot/jtreg/JCK,
real JNI required-parity and CPU/GC/native-memory measurements are distinct
gates and not established by this source receipt. This PR does not assert
five-vendor collections complete or a new JNI speedup.
