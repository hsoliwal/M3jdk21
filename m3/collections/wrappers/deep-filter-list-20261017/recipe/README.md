<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->

# Ordered deep filtering across the existing wrappers

This recipe reuses the exact first-party binary-carry predicate atom from M3JDK21 #1018 and routes the existing filtered List, Map, and counted sorted-multiset conjunction sites through it. The receiving source baseline is `37eba37dcbdf0c0222b494d07ba2fca401ce248f` (#1031 including #1056). Original List/Map preimages and thirty-four required parent bytes remain exactly as sealed at `10ce8abaa279292c73e1c118628344cf0851d14a`; the new counted owner and its twenty-seven additional dependencies are pinned at the current baseline. Writable Map, sorted-range/AVL, representative-key, counted-storage and native refinements are retained.

The twenty-four-command contract lane constructs an isolated overlay from sixty-one sealed parent inputs and thirteen actual SDK outputs. The three retained Java/native runners enumerate their compilation inputs explicitly. The counted gate adds the existing `M3CollectionViews` source as the exact new dependency of the one-line conjunction adaptation. Original historical manifests and payloads remain frozen, and each active gate admits only its proven current successor.

## Behavioral atoms and contracts

`M3CollectionViews` receives the exact donor atom. `M3FilteredLists`, `M3FilteredMaps`, and `M3FilteredSortedMultisets.View` each change their existing conjunction site, preserving the newer writable-map representative-key and AVL receiver work. Wrapper-created predicate evaluation has logarithmic stack depth. Predicate occurrence order, duplicates, short circuit, thrown exception identity, and previously created views retain their original behavior. Arbitrary recursion inside a caller's predicate is outside this bound.

The existing filtered Collection, Set, List, SortedSet, NavigableSet, Map, SortedMap, and NavigableMap owners remain authoritative. Live backed ranges retain comparator and mutation semantics. Detached stable List snapshots retain duplicates and stable identity for comparator ties. Writable bidirectional List cursors continue to operate on stored backing elements. Counted sorted-multiset views retain live ranges, descending order, count groups, stored representatives, domain/epoch checks, and read-only no-op refusal. The recipe adds no alternate collection hierarchy or global `java.util` replacement.

The catalogue compares exact Guava and Eclipse Collections sources. These are useful algorithm/contract comparisons, but their generic filtered iterators and eager selected sets do not replace the complete existing writable List and live sorted-range contracts. Their source code was reviewed without copying. The actual implementation reuse is the already-qualified first-party atom and existing Synexia-derived owners.

`CATEGORY_REVIEW.tsv` is the category index, `CATEGORY_IMPLEMENTATION_DONORS.tsv` pins immutable implementation sources and licenses, `CATEGORY_TAXONOMY.tsv` records challenge/tutorial categories, and `DONORS.tsv` records actual first-party source reuse. `QUALIFICATION.md` identifies exact generated-source evidence and the inherited broader inventory hold.

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
