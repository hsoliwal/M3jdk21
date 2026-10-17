<!-- SPDX-License-Identifier: Apache-2.0 -->
# Pinned Java recipe provider compatibility repair — 17 October 2026

**Reproduced red CI:** M3JDK21 filtered/sorted wrappers workflow
[run 39016934427](https://github.com/hsoliwal/M3jdk21/actions/runs/39016934427)
now passes its original source-tree identity step but fails at Maven `testCompile`:
the retained M3JDK21 `M3HashPinnedJavaSnapshotRecipe` (Git blob
`d58ca50dfaeb60f581b64f43545e688aca5c6530`) does NOT expose
`targetPaths()` or `preimageManifest()`. Two recently introduced
tests mistakenly use these later Synexia helper APIs. The older
`M3HashPinnedTextSnapshotRecipe` already does expose both.

**Selected contract-preserving repair**: rather than changing the pinned
provider's parser/source boundary or copying its newer version, adapt
ONLY the two recipe test implementations to enumerate the same exact
path/preimage information from their **own existing SHA-pinned manifests**.
All product LST run, second-pass fixed point, drift/occupied/missing
admission, JNI and public API gates remain unchanged. No product source
or public API is edited by this task.

Canonical named recipe:
`com.synexia.m3.M3Jdk21PinnedProviderTestCompat20261017`, using the
existing `M3HashPinnedJavaSnapshotRecipe` with exactly two before/after
Java test files in `m3/collections/wrappers/recipe/src/test/java`.

Process: canonical Synexia recipe+test FIRST; target-side copy of its
source-sealed resources in the isolated workflow-pin Maven module SECOND;
two exact target test postimages THIRD. The target wrapper recipe tree
SHA necessarily changes when those original test files are replaced,
so a separate exact-tree workflow-pin successor must be source-sealed
and replayed after receipt; do not disable the workflow check.

The original source manifest and JDK21 Map/List outputs remain byte
identical. Real Maven/OpenRewrite JUnit and original JNI/collection
runtime acceptance remain required before promoting this draft.
