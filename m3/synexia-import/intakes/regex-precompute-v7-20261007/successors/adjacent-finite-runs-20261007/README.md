<!-- SPDX-License-Identifier: Apache-2.0 -->
Copyright 2026 Hitesh Soliwal and contributors.

This prepared successor replaces only the existing vendor MIndexRegexProgram with exact qualified Synexia source 4e217b9a and adds the unchanged original MIndexRegexTrigramQuery 428cb5cd. Both come from merged Synexia PR9797 at source commit 1e4743f6661a9149f327bf7171b4de2df68add54. Its 30 source/recipe files merged separately from the 63 executed Maven evidence files in draft PR9807.

The two-target recipe reuses the existing pinned Replay/Manifest engine without copying or changing it. Its four-column manifest replaces the exact 9999e8af Program preimage and requires the query path to be absent, or already at the recorded postimage. The base-revision file retains the historical planned receiver epoch; Replay checks source hashes. PR394 has merged, so this successor requires a new PR from a freshly checked master rather than updating the closed PR. The observed master bac0108267c93ecdd702f06609828f9953278eef retains all 177 prior postimages; the publisher must recheck current master and exact target preimages immediately before publication. Actual isolated receiver Maven apply changed both targets successfully; default check, second apply and both drift refusals remain NOT_RUN.

The owning Maven entry point is the already published Synexia POM, pinned with its unchanged neighboring Replay/Manifest sources at commit 1e4743f6661a9149f327bf7171b4de2df68add54. From a checkout at that revision, run:

```powershell
mvn -f "<Synexia checkout>/synexia-openrewrite-recipes/tasks/synexia-m3jdk-regex-adjacent-finite-runs-20261007/pom.xml" "-Dm3.target.repo=<isolated M3 snapshot>" "-Dm3.templates=<M3 snapshot>/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/recipe/templates" -Dm3.mode=--plan verify
```

Use `-Dm3.mode=--apply` to apply the exact two-target recipe, then `-Dm3.mode=--check` to verify the fixed point. The POM defaults to `--check` when mode is omitted. `m3.replay.sources` can retain its existing neighboring source default. No new POM, compiler or engine is introduced. Root executed the isolated M3 two-target manifest through the existing Maven Exec goal: apply changed two targets and returned EXIT0. Subsequent resource refusals occurred before any check JVM launched. The remaining default check, second apply and two drift-refusal outcomes are NOT_RUN; earlier donor Maven qualification does not close these receiver gates. Root's bounded execution packet may use the already qualified Maven Exec 3.5.0 goal and eight compiled engine classes without repeating compilation.

Existing intake provenance, export/source custody, importer recipe, dependency frontier and owner/name map remain historical and byte-exact. After actual recipe execution and publication, this ledger supersedes only the missing query source-custody edge. The original donor type stays in the tooling vendor namespace; jdk.internal.mindex.M3TQ and canonical M3StringBacking stay unchanged.

Regression owners are evidence .txt files, outside M3 test sources. The donor qualified 40 selected methods: Trigram five retained plus three new, Program 28 retained, gate four retained. Actual donor compile/test/recipe receipts and original failures keep their bytes and scope. These outcomes do not qualify this receiver. Published owning recipe and review evidence are pinned in DONOR_EVIDENCE_INDEX.json. The first seven-file metadata review reported missing template resources because its diff scope was mistaken for the full package. Both resources are present in the complete 21-file publication inventory and were loaded by actual apply2. The original finding and corrected CPU disposition are retained; receiver metadata review admission remains OPEN pending a review with complete package context.

Precompute is core and P0. Receiver source custody must be retained and must not be mistaken for runtime qualification.
Canonical M3StringBacking ownership, owner/generation/range coordinates and exact source/query/image/raw-UTF16 key binding remain required.
Unknown syntax, unsupported modes, exhausted budgets and unavailable accelerators must remain conservative or refuse/fail open; a query positive is never a regex match.
Warm parser/compiler and materialization paths must remain unavailable under the recorded warm contract; persistence restore must preserve exact behavior and key/image parity.
Exact receiver recipe apply, check, second-apply fixed point and no-mutation drift refusal require root execution on isolated receiver snapshots.
Complete dependency/module source closure, JNI/ABI/provider parity, actual rebuilt M3 runtime, JDK/jtreg/platform and full String qualification precede phase promotion.

The cardinality 64 and UTF-16 length 256 limits are correctness budgets, not measured optimal settings. Existing 21 deferred hypotheses remain unchanged. Distribution, allocation, cold/warm and JNI measurements follow reconciliation; no tuning or speedup claim is made here. P0 and full String qualification remain OPEN.
