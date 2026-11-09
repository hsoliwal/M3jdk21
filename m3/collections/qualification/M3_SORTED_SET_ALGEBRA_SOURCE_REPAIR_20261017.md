<!-- SPDX-License-Identifier: Apache-2.0 -->
# Sorted algebra Java21 compiler red -> source recipe corrected

Initial [CI run 38989400393](https://github.com/hsoliwal/M3jdk21/actions/runs/38989400393)
passed original/inherited source-seal gates and failed at source generation
of the `M3LongCompositeViews.validate(T[])` method. Its old template
duplicated the declaration and adjacent Javadoc anchor, making Java21
compilation fail. This is a recipe authoring bug, not a genuine failure
of primitive set algebra algorithms.

Canonical source correction was committed FIRST to
[Synexia #11937](https://github.com/hsoliwal/com.synexia/pull/11937)
at `90d5722019173baef55c90860cd21b7d11b8facc`, postimage SHA-256 `759ba7905d6190f42eb691d25307dc5807af7289948fc5c45b94568d679c11b8`.
This commit installs *only* that reviewed Java source template byte-for-byte
into the existing product owner, updates its exact SHA receipt, and retains
the red-run provenance. All existing concat/sortedUnion functions and
JDK/JNI contracts are preserved. Do not mark this corrected source
qualified until the new hosted Java21 regression genuinely passes.
