<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Restore bounded runtime repairs on the current receiving tree

Current commit `07ba6e383343d32c83cfdaf641387bbe72bf325d` has lost the
delivered bytes from `86528a90079b89a50b51ef13da08e7e70d468cb4`. The admission
record compares actual Git blobs and original recipe manifests. A merge or an
ancestor relationship is not evidence that a source change remains delivered.

Of the 27 targets controlled by the seven original runtime recipes, 26 are
exact original preimages, including absent additions. This pass reuses their
exact qualified postimage atoms. The current global algorithm catalogue has
one later numeric-copy row; every current byte is retained and the five missing
runtime rows are appended. The old name map is itself an exact original
preimage, so its restored postimage preserves all current entries.

The fourteen production source files after restoration are byte-identical to
the earlier delivered files: HotSpot's matching Object vtable count, the JNI
feature-macro guard, and the progress/function/Strings strict-build repairs.
There are no changes to allocator error handling, public signatures, or global
build gates. Object itself remains unchanged and its universal compatibility
claim remains HOLD. The ordered STRING phase is unchanged. Runtime facade and
ProgressJob API additions are separate feature datasets and reviews.

The source authorer prepares inputs and invokes the original Maven sealer.
The transformation is `com.synexia.m3jdk.CollectionsContractRefinement20261017`
with dataset `m3jdk21-runtime-restore-07ba`. The original Java and text owners,
five SDK tests, eight sealing cases, nine receiver cases, and capped POM are
reused. No target is copied into a receiving tree before actual SDK
qualification. Current receiving and fixed-point receipts must bind these
inputs; arbitrary source drift is refused.

Earlier build/test receipts retain their earlier source identity. They are not
relabeled as a build of this current commit. A later verification may establish
that the complete current OpenJDK build input set equals the exact earlier
feature-build input set; such a receipt must identify both source sets and the
actual image build workspace. Otherwise a new image build is required. This
preparation record alone claims neither outcome.
