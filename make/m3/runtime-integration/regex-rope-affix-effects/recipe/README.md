<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->

# Rope affixes and prepared composite full matches

This recipe refines the existing `java.lang.M3StringRegexDp` and `jdk.internal.mindex.M3RegexDpPlan` owners. The receiving baseline is `3bbb4712fb20adba1405741db8457164b9f436f5` (#1029), which retains the rolling cursor, immutable Plan preflight, and mandatory n-gram work from earlier passes.

## Behavioral atoms

1. **Mandatory literal boundaries.** Two small Plan fields reference the full mandatory literal prefix/suffix already present in its atom array. Existing facts and edge tests remain early necessary conditions. Longer boundaries use the canonical forward `CodeUnitCursor`, including slices and shared rope fragments. A guard may prove rejection; passing it still requires the exact matcher.
2. **Warm exact effects.** An already-cached exact root effect is consulted before re-reading a long boundary. The same weak owner, source range, Plan identity, and incoming frontier remain the key. UNKNOWN may remain cached as an unresolved sentinel; it is never treated as false or as an exact transition. A failed whole-match affix guard is never cached as a fragment transition.
3. **Prepared fixed context.** The existing internal regex Plan prepares the prefix once into an open frontier plus pending-high-surrogate state. It computes two accepting preimage masks for the suffix, one for each possible pending-high state. A row evaluates only its variable ordered fragments. Relation preimages use linear passes over existing rows; shared Effects are never narrowed.
4. **Bounded Java/JNI execution.** A closeable prepared batch pins immutable Effects, copies CSR tuple coordinates, and records exact UTF-16 lengths. Admission bounds effect/row counts, primitive bytes, tuple references, and state work. Native code validates the complete numeric image and manages opaque handles and leases. Explicit backends report what actually executed; cancellation callbacks run outside lifecycle locks.

The public String/Pattern APIs and the admitted dot/star ASCII-literal shortcut grammar are unchanged. Supplementary pairs, unpaired surrogates, line terminators, fallback, and cache ownership retain their existing contracts. The new internal batch API does not promise Java Matcher captures, `find` iteration, lookarounds, backreferences, or automatic translation of arbitrary callbacks.

The optional CUDA provider is retained only in the private convergence recipe. The environment has no CUDA compiler or physical GPU. This public receiving adaptation supplies the prepared Java/JNI contract and explicit optional-provider seam; it carries no measured GPU acceleration claim.

`catalogue.tsv` records the affix/search categories. `ALGORITHM_CATALOGUE.tsv` records broader candidate comparisons and ownership decisions. `QUALIFICATION.md` separates executed local evidence from matched product/device/performance gates.

## Convergence and recipe invariant

`com.synexia` and the other private repositories are convergence workspaces: develop a concept, compare reusable implementations, repair the transformation recipe, and retain executable evidence before adapting it to a product such as the JDK or SWT. A private location is not itself evidence of qualification. Product repositories receive reviewable, self-contained adaptations with explicit remaining gates.

This task reuses the existing `M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe` owners at Synexia commit `11c94feb18155a53cc213708c353791132edc4a7`. The four exact owner sources are compiled by the standalone Maven POM. A public receiving mirror carries these unchanged sources under `sdk-owner`; it has no Synexia runtime dependency.

Every refinement starts by assigning a problem category and consulting the retained catalogue. Compare the existing owner, first-party reusable atoms, and compatible licensed donors. Record contract differences, complexity, actual measurements, selection rationale, and unexecuted gates. Edit the recipe inputs and tests, run the real OpenRewrite SDK, validate the generated Java/native behavior, and receive those SDK Results serially. If an output is wrong, repair the recipe and regenerate it. Never patch emitted product files to hide a failed transformation.

The five Maven tests execute the named YAML recipe, exact replay, reversed source order, each file against already-converged siblings, fixed point, absent/new-file admission, drift/missing/duplicate/wrong-representation refusal, and post-scan refusal. They retain actual SDK Results and bind the POM, receiver, all `src` and `contract` files, and the four SDK owners in `SDK_INPUTS.tsv`. This proves transformation custody; the separate contract profile proves relevant runtime behavior.

The receiving adapter admits every file before the first write, preserves file paths and modes, and applies only hash-verified SDK Results. A second application writes nothing. It assumes exclusive write access during application; serial atomic file replacement is not a multi-file filesystem transaction under a mid-apply I/O failure. Keep the worktree reviewable and rerun from admitted per-file states after resolving any such failure.

## Repeat the qualification

Use JDK 21, Maven 3.9 or later, Python 3, and a Linux C compiler for native gates. The first run resolves pinned Maven dependencies. Subsequent runs can use `-o`. Configure an approved Maven proxy/trust store in the environment if required; no credentials or machine settings belong in the repository.

From this canonical crate:

```sh
mvn -B -ntp -Pcontract verify -Dm3.contract.target=/absolute/path/to/M3jdk21
python3 receive.py --target /absolute/path/to/M3jdk21 --check
python3 receive.py --target /absolute/path/to/M3jdk21
python3 receive.py --target /absolute/path/to/M3jdk21
```

From the public mirror, add `-Drecipe.owner.root="$PWD/sdk-owner"` to the Maven command. `receive.py` automatically selects that owner directory. `JAVA_HOME` must identify JDK 21. A contract output directory must be new: remove only that generated task output after preserving evidence, run `mvn clean`, or select `-Dm3.contract.output=/new/absolute/path`.

The `target/candidate` files are the executed OpenRewrite Results. `target/contract-proof/evidence` records commands, source identities, and outcomes. Retained review evidence is under `evidence/`; machine paths and timings in historical logs describe the recorded execution, not portable configuration.

## Catalogue reuse

The TSV files are the searchable catalogue, not a new search service. Use `rg -n` with a problem category, owner, donor, or operation. For each later pass, append the new source pin and decision; preserve prior failures and decisions as historical evidence. LeetCode, HackerRank, and GeeksforGeeks identify problem categories and independent test shapes. Their pages are not blanket licenses to copy submitted solutions.

## Licenses and acceptance

Recipe code and new task documentation are Apache-2.0 unless a file retains another notice. Exact target preimages, generated target payloads, and reused donor files retain their original licenses. See `NOTICE.md` and `licenses/`. Hash equality identifies a source; it does not establish semantic equivalence, speedup, or product acceptance.

No workflow is added merely to create a green status. These task-specific Maven profiles can be invoked by the existing control plane. No JDK family completion flag or ordered promotion gate is advanced by this task.
