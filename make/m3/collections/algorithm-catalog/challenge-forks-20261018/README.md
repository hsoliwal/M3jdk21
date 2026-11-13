<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Pinned challenge donors and public fork workspaces

This packet extends the existing M3JDK problem-category catalogue with a complete
path inventory of **two specific public donor trees**, five source-reviewed
comparisons, pending owner-refinement decisions, and public fork workspace
identities. It adds no Java/JNI implementation, search engine, runtime dependency,
or new recipe owner. Every source-inventory row is `NOT_EVALUATED`; the five
reviewed examples remain `REFERENCE_ONLY`.

## Exact corpus coverage

| Public donor | Pinned commit | Blob paths | Java paths | README paths |
| --- | --- | ---: | ---: | ---: |
| [javadev/LeetCode-in-Java](https://github.com/javadev/LeetCode-in-Java/tree/bed5d330e2b145e24e6fc1048ca28bf156b3d639) | `bed5d330e2b145e24e6fc1048ca28bf156b3d639` | 9,665 | 6,156 | 3,176 |
| [RodneyShag/HackerRank_solutions](https://github.com/RodneyShag/HackerRank_solutions/tree/a98773d3a0c4f11b9cd90d6a1f37b994c170996a) | `a98773d3a0c4f11b9cd90d6a1f37b994c170996a` | 331 | 267 | 10 |
| **Combined** | Two pinned complete trees | **9,996** | **6,423** | **3,186** |

The LeetCode donor has 3,027 Java files under the literal `src/main/java/` root
and 3,129 under `src/test/java/`. Within its main root, 3,020 files occupy 3,014
numbered challenge directories; seven files occupy helper/type directories.
The HackerRank donor has 267 Java paths outside those literal Maven-style roots.
Those directory roles do not establish algorithm fitness or whether tests ran.
README counts describe paths; they do not claim all README bodies were read or
that every included problem statement has independently cleared source rights.

`DONOR_SOURCE_INVENTORY.tsv` preserves every blob occurrence, exact path, Git
blob/mode/size, native directory name, root-license reference and disposition.
Identical Git blobs at different paths remain separate occurrences. No row is
dropped because it looks like a helper, fixture, document or non-Java file.
`DONOR_DIRECTORY_TAXONOMY.tsv` retains the donor's own top-level directory labels;
these labels are not invented platform topic tags.

Completeness is limited to the two recursively listed, nontruncated Git trees.
The complete source bodies, all LeetCode/HackerRank problems, all algorithms,
platform submissions, correctness, performance and target compatibility have
**not** been established. GeeksforGeeks remains an existing taxonomy/discovery
source in the parent catalogue; this packet adds no copied website editorial,
submission, or third challenge corpus.

## Reviewed category comparisons and existing owners

`REVIEWED_CANDIDATES.tsv` preserves the existing
`category/candidate/source/revision/rights/contract/complexity/evidence/decision`
schema with stable donor IDs. Its category origin explicitly says curated static
source review. The five entries retain exact public source URLs and Git blobs:

| Donor comparison | Contract difference that matters to M3 | Recorded decision |
| --- | --- | --- |
| LeetCode 189: three-reversal rotation | Whole nonempty array and nonnegative distance differ from aligned offset windows and signed distances. | Retain the existing `M3StateMaskArrays` rotation owner; reference only. |
| HackerRank array left rotation | Scanner-oriented program and inclusive helper bounds differ from an M3 API; empty modulo and invalid-bound behavior need explicit treatment. | Retain the existing aligned-window owner; reference only. |
| LeetCode 307: Fenwick update/prefix sum | Restricted integer sums and retained caller-array aliasing differ from M3 numeric and ownership contracts. | Keep existing bit-lane/Fenwick owners; adaptation deferred. |
| LeetCode 57: interval insertion | Closed `int[][]` intervals, row allocation and suffix aliasing differ from packed half-open signed-long lanes. | Refine the existing `M3LongRangeSet` only after its boundary contract is proved. |
| LeetCode 31: next permutation | Mutating payload permutation has no comparator or paired state-plane contract. | No new payload-permutation API admitted; a reversal atom is only a possible component of a separate existing-owner refinement. |

No candidate code was copied into an M3 production owner in this packet. Root
MIT license bodies are preserved exactly in `licenses/` and bound to their Git
blob identities. This verifies the repository-root declaration; later source
reuse still requires exact file-body, copyright, dependency and target-contract
review. Root metadata does not license every embedded problem statement or
third-party payload automatically.

`ALGORITHM_DECISIONS.tsv` joins these comparisons to the catalogue's existing
problem-category/owner/contract/proof-gate convention. Two concrete refinements
remain **deferred source-review proposals**:

1. For `M3LongIndexProjection`, convert a proven opposite sorted index order by
   whole reversal followed by reversal of each equal-key run. The proposed
   composition preserves encounter order within ties and touches only indexes;
   the general encounter-order path retains its existing stable sort. Expected
   linear index work is a structural proposal, not a benchmark result.
2. For `M3LongRangeSet`, use the existing binary-boundary pattern to locate
   affected interval endpoints, while retaining packed lanes, half-open bounds,
   adjacency rules, shifts, capacity and revision mechanics. The shifting part
   remains linear; no measured speedup is claimed.

`OWNER_REFINEMENT_PROPOSALS.md` retains the original source-review discussion.
`source-receipts/LOCAL_OWNER_OBSERVATIONS.json` pins the relevant **public M3JDK**
whole-file observations. Neither proposal is applied by the catalogue PR. A later
change must recheck the current preimage and complete its own canonical recipe,
unchanged-interface proof and independent target qualification.

## Forks are convergence workspaces

The user explicitly requested that **forks can also be convergence workspaces**.
A fork can hold experiments, donor comparisons and reusable transformations under
the same serial, contract-preserving discipline as an original repository. Its
fork flag does not make it less eligible for exploration, and it does not grant
algorithm admission automatically. Existing canonical M3 recipe ownership is
retained; `com.synexia` is referenced here only by its canonical-workspace role.

`FORK_WORKSPACE_INVENTORY.tsv` contains nine branch rows from five repositories
whose retained metadata explicitly confirms `private: false` and
`visibility: public`. Each row is still `READ_ONLY_CANDIDATE`:

| Public workspace | Fork source | Evidence relevant to this pass |
| --- | --- | --- |
| [hsoliwal/guava](https://github.com/hsoliwal/guava) | `google/guava` | Default head matches the separately reviewed Guava donor pin. |
| [hsoliwal/commons-collections](https://github.com/hsoliwal/commons-collections) | `apache/commons-collections` | Default head matches the separately reviewed Commons donor pin. |
| [hsoliwal/eclipse-collections](https://github.com/hsoliwal/eclipse-collections) | `eclipse-collections/eclipse-collections` | Default head and two M3 11.1 baseline/relocation-named branches are pinned. Both M3 branches point to the same upstream release commit, so no relocation implementation is inferred. |
| [hsoliwal/rewrite](https://github.com/hsoliwal/rewrite) | `openrewrite/rewrite` | Default head and separate SDK-receiving-test/runtime-application work branches are pinned. Their recorded qualification is not re-executed by this packet. |
| [hsoliwal/M3jdk21](https://github.com/hsoliwal/M3jdk21) | `openjdk/jdk21` | Receiving product fork at this catalogue packet's base. Root GPL metadata remains distinct from independent Apache-2.0 M3 components. |

The original fork research sampled repository searches, selected branch lists
and recent history on 2026-10-18. This public packet preserves only the relevant
public fields in `PUBLIC_FORK_EVIDENCE.json`, with source digests and explicit
projection rules. It omits private repositories, private commit bodies, private
source inventories, restricted URLs and mixed-visibility search/list responses.
The retained public rows are not an exhaustive census of all owner repositories
or branches. A branch name alone does not prove an implemented feature.

The retained HackerRank commit message also contains the historical repository
name `rshaghoulian/HackerRank_solutions`. `PUBLIC_DONOR_ALIASES.json` preserves the
public API evidence that this name redirects to repository ID `80899820`, the
same public repository currently named `RodneyShag/HackerRank_solutions`. This
corroborates the historical URL without changing any original commit-response
bytes, source pin, inventory row or algorithm-admission status.

## Search and review through the existing catalogue

From this directory, use the existing text tooling rather than a new index:

```sh
rg -n 'INDEX_ROTATION|FENWICK_PREFIX_SUM|INTERVAL_COALESCING' REVIEWED_CANDIDATES.tsv
rg -n 's0031_next_permutation|s0057_insert_interval|Left Rotation' DONOR_SOURCE_INVENTORY.tsv
rg -n 'sorted-or-ordered-index|physical-index-offset-bitmask' ALGORITHM_DECISIONS.tsv
rg -n 'guava|eclipse-collections|rewrite' FORK_WORKSPACE_INVENTORY.tsv
```

Rows can be joined by immutable repository/revision/path/blob identity and stable
donor IDs. No platform topic is inferred merely from a filename. New source
comparisons append evidence and rationale; a matching category does not bypass
license, callback, ordering, null, alias, state-plane or exception contracts.
The existing catalogue/search and serial-recipe components remain the owners of
any later integration. Static TSV publication does not silently update their
packaged donor manifests or assert that those APIs ingest this directory.

## Reproduce the metadata checks

The four files under `metadata/` contain gzip-compressed **exact public GitHub API
response bytes** for the two pinned commits and complete recursive trees. Gzip
timestamps are zero and filenames are omitted. Decompressed sizes and SHA-256
digests must match the unchanged `source-receipts/API_SOURCE_RECEIPTS.json`.
The inventory TSV remains uncompressed and searchable.

Run the standard-library-only verifier from this directory:

```sh
python3 -I -S -B verify_metadata.py
```

It reads the directory containing the script and writes no files. It checks the
sealed package files; raw response/commit/root bindings; all Git directory hashes;
exact unique repository/path/blob occurrences and inventory order; counts and
structural classifications; taxonomy; root-license bytes; five reference-only
comparisons; and the public fork fields and candidate status. A successful result
is **metadata verification**, not algorithm correctness, Java/JNI execution,
performance evidence or source-reuse admission.

`source-receipts/DATA_PROJECTION_VERIFY.json` and
`source-receipts/FINAL_VERIFY_REPORT.json` preserve the original donor research's
results as source receipts. `PACKET_SOURCE_RECEIPTS.json` records exact copied,
compressed and public-projection lineage for this packet. `SHA256SUMS` seals the
final package files and excludes itself. The parent PR receipt records the
manifest hash and the verifier's actual stdout/exit result after review.
