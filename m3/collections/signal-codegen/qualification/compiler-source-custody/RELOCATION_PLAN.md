<!-- SPDX-License-Identifier: Apache-2.0 -->
# Compiler signal qualification: public-name-safe source relocation

This is a source-path-only qualification change on draft M3JDK21 PR #2308, not a Java/JNI/HotSpot modification. Original product Java public APIs, compiler parser, `M3CompilerFacts`, `SortableProcessor`, generated companion and JNI exports remain unchanged.

## Observed executable failure

[GitHub Actions run 41415682260](https://github.com/hsoliwal/M3jdk21/actions/runs/41415682260), at PR head `087b4769022aea0564eb3bfdaaf2ee67b2c3ecbf`, completed **failure** in `Check exact PR changes against public JDK naming rules`. The original 34 Python naming-policy tests passed, but `check_public_names.py --base ce005d25b29fb6246e013bffb462afd5ed070362 --head 087b4769022aea0564eb3bfdaaf2ee67b2c3ecbf` rejected exactly six newly added qualification file paths for their date-stamped path segments.

The policy intentionally rejects new dated paths regardless of whether files are Markdown, TSV or shell scripts. **Do not weaken or add exceptions to the policy.** Existing historical paths in the base commit are grandfathered by diff semantics. New product source paths remain unchanged.

## Explicit reviewed six-path relocation

Move the following *qualification data and script only* within `m3/collections/signal-codegen/qualification/`. Copy the exact old Git bytes before deleting/renaming the old branch paths. No silent file move: each old/new path is recorded here, and the PR diff must show the relocation.

| Old directory path prefix | New stable path prefix | File(s) |
|---|---|---|
| `m3-machine-compiler-hints-20261102/` | `compiler-machine-hints/` | `COMPILER_INTEGRATION_PLAN.md`, `README.md`, `SOURCE_MANIFEST.tsv`, `verify.sh` |
| `m3-record-component-accessor-20261102/` | `record-component-accessor/` | `README.md`, `SOURCE_MANIFEST.tsv` |

The five prose/manifest files must have the **same Git blob IDs before and after relocation**. The shell verifier must change exactly two literal path references: its own `SOURCE_MANIFEST.tsv` path and the record successor `SOURCE_MANIFEST.tsv` path; all other bytes, historical counts, source digests and fail-closed logic remain byte-identical. Keep its original Git blob as an ancestor. Do not rewrite the historical `m3-compiler-signal-facts-20261022/SOURCE_MANIFEST.tsv` or the record/component source receipts.

In the **existing** `.github/workflows/m3-compiler-signal-facts-20261022.yml`, change only the successor-path trigger and the invoked successor verifier path. Preserve its original Java21 javac test, original Maven entrypoint and original JNI job. No other workflow or product source change.

## Verification and admission order

1. Inventory branch head, exact old/new paths, SHA-1 Git blob IDs, and history. Record original target absence. Refuse new-path conflicts.
2. Publish this plan before code.
3. Create stable-path files and verify five original Git blob matches; verify `verify.sh` differs only by the two planned path strings, and workflow only by two references. **Only after verified receiving files exist**, remove the six old paths on this isolated PR branch.
4. Execute `check_public_names.py` original 34 tests, then compare diff-based path naming from the exact base/head pair. A pass is only valid if the underlying executable Python test exits 0.
5. Execute the new stable `verify.sh`, then strict module `verify.sh`, Maven, and the original JNI/CI stages. Source receipts and compile/contract results must remain unchanged. Log every stage; never claim GitHub-hosted green while queued or failing.
6. Keep draft/HOLD until the real hosted terminal outcome. No direct `master` write or automatic PR merge.

## Non-goals and unresolved independent failures

This path repair does **not** fix the unrelated Apache multivalue source hash mismatch for `M3ArrayList.java`, or the Guava MinMaxPriorityQueue workflow source hash mismatch. Those are separate source owner/history convergence tasks, not permission to repin an existing historical receipt. It also does not implement JIT lowering or any LeetCode/HackerRank/GeeksforGeeks challenge source.

Donor classification remains owned by Synexia PR #15039; LeetCode/HackerRank/GfG are category/adversarial-test references, not copied source. API, JNI ABI and semantics locked.
