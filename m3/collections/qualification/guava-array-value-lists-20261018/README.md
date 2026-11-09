<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Guava array value lists

One existing production file now supplies existing `M3ArrayList` value lists to
Guava's `ListMultimap` wrapper, preserving random access alongside nulls,
duplicates, order and live-view bookkeeping. One regression method is added.

The qualified Synexia recipe and its immutable source/fixture/proof packet are at
https://github.com/hsoliwal/com.synexia/pull/12292 (commit `83cbf7399396afafa42ec75ecd81e6d2131166da`).
This target retains its actual emitted implementation, tests and receiving proof;
the reusable recipe remains in Synexia. No runtime dependency on Synexia is added.

Java 21 compilation with all lint warnings as errors passed. All 23 interop tests
pass, including the unchanged differential trace; the new random-access test fails
on the original factory. Public/protected descriptors are unchanged. Nine recipe
tests pass on the exact publication copies with fixed-point and refusal coverage.

The existing `m3-sdk-families-20261017/FAMILY_CATALOG.tsv` ArrayListMultimap row
remains PENDING. Concrete-class/serialized-form compatibility, the full collection
superset, hosted CI and product promotion remain open. No speedup or subsystem
phase advancement is claimed. Bundled autoreview refused required `--no-tools`
before authentication/reviewer invocation; the PR remains draft pending review.
