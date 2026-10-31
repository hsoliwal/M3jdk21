<!-- SPDX-License-Identifier: Apache-2.0 -->
# Comparator-key entry membership: additive test convergence

Canonical source-time recipe: `com.synexia.m3.FilteredMapEntryMembership20261018`, qualified in [Synexia #12351](https://github.com/hsoliwal/com.synexia/pull/12351).

## Current product owner

The current M3JDK21 `master` already implements the comparator-aware `SortedMapView.entrySet().contains(Object)` contract inside `M3FilteredViews`, including the stored-key representative and nullable-value distinction.

- Current production owner Git blob: `34fca25ba45467567213ed7a170bf4b27d6427e3`
- Older PR #1345 production blob: `f8a0809e55bbe5d78ae29d3552ff113f93971f06`

The older production blob is **not** used to overwrite the newer 32-KiB runtime owner. The canonical behavior is already present; only previously unreceived contract tests are transferred.

## Test source custody

- Existing product test preimage blob: `5dfe9035322a30a99a185d2431f2363065fa7824`
- Older donor test blob from PR #1345: `dacd7a5a52c79878a3f53ab4e404100bdab221f3`
- Additive product test result blob: `cdf8844d6eb9baa4368b57984b346606e032f6c4`

Mechanical insertion includes the preexisting donor's comparator alias, predicate callback, read-only refusal and live mutation oracles plus two required imports and explicit `main` dispatch calls. Every original line of the 272-line product test is retained in order; there are no deleted files, runtime edits, JNI edits, new dependencies, or changes to exported collection interfaces.

## Qualification

This staging branch is only an additive **candidate**. The existing `m3-stored-representative-receiver-20261017.yml` workflow runs the authentic receiver and interpreted-variant checks when its PR is opened. Full M3JDK21 module/reactor/JNI qualification remains separate.

Admission requires actual executed successful tests. Source ancestry alone is not proof of behavior.
