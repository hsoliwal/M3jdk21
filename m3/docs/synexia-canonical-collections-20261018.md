<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 source-sealed collections receiver — 2026-10-18

Canonical reusable recipe owner: [hsoliwal/com.synexia](https://github.com/hsoliwal/com.synexia),
Synexia review [#12231](https://github.com/hsoliwal/com.synexia/pull/12231).
This receiving branch starts at master commit `0efc1719ca5766aead09f4f7c49f5067975bd946`.

Three existing Java21 first-party Maven installers have their exact source and
six source templates centralized in Synexia. See
`m3/compatibility/synexia-m3-collections-receiver-20261018.tsv`
for the immutable output-to-template Git blob comparison.

**Four outputs are identical and two are evolved**. The index projection
and state mask production owners have different bytes from their old
template and remain **EVOLVED_PRESERVED**. No older template will replace
them. The bitmap snapshot and its selftest are exactly the template
outputs. Existing target-local recipe shims and all production code
remain unchanged pending exact-head proof and target build reconciliation.

The new workflow compiles the existing Java source-sealed installers,
executes their selftests and performs a bounded real receiving replay:
`--check`, idempotent `--apply`, then `--check` on the existing
bitmap family (no expected product diff). For the two evolved production
owners, it executes `--apply` and **requires** drift refusal.
The workflow then asserts `git diff --exit-code` on Collections.

This qualifies only this narrow source-sealed Java receipt if CI
actually passes. It is not a claim that OpenRewrite SDK, HotSpot, JNI,
jtreg, other M3 recipes, or all public forks were executed.
Do not delete target mirrors or merge the draft until the complete
canonical and target receiver gates are reviewed.
