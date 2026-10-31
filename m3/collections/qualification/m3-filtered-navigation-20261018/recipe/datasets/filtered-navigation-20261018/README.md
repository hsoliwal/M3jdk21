<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered navigation and mask-alias reconciliation

This finite successor uses the existing `m3-collections-contract-refinement-20261017` task crate and its unchanged Java and text snapshot owners. The target is `hsoliwal/M3JDK21@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. It refines five named behavioral atoms in the existing `M3FilteredViews` and `M3IndexPathMasks` public owners and adds focused regression/qualification inputs. Public callable interfaces and production native ABI stay the same.

The wrapper repairs distinguish a stored comparator-middle null from an absent neighbor, use the actual stored map key for entry membership, enforce unconditional read-only sequenced removal and reversed-range guards, and reuse historical explicit continuations for known deep generic filter chains. The mask repair rejects output aliases with both required and forbidden mask vectors before either Java or JNI can write.

## Review and source custody

`CATALOGUE.tsv` supplements the existing shared catalogue by problem category. `DONOR_SOURCES.tsv` pins selected first-party Apache-2.0 atoms and the existing reviewed catalogues; `EVALUATION.json` records rejected alternatives and contract/cost limits. Existing LeetCode, HackerRank and GeeksforGeeks discovery categories inform adversarial shapes only. No challenge, editorial, JDK or Guava implementation is copied.

`SOURCE_INVENTORY.tsv` seals the 203 locally present baseline support files: 134 production Java files, 58 Java test files, ten native sources and one native test. Only the named operations in two production files are reviewed. Every other operation remains pending, including other operations in those same files. Unfetched repository files are outside that inventory; this is not an exhaustive collection or JDK review.

The manifests retain exact preimages and postimages. `ENGINE_EFFECTIVE.tsv` must report no changes to the four canonical engine sources. New regression classes are test-only; this pass requires no new public owner, convergence namespace or naming-map entry.

## Reproduce with the existing shared protocol

From this crate, using Java 21, Maven and Python 3:

```sh
mvn -B -ntp -Dm3.dataset=filtered-navigation-20261018 -Pcontract verify
python3 receive.py --dataset filtered-navigation-20261018 --target /absolute/M3JDK21 --check
python3 receive.py --dataset filtered-navigation-20261018 --target /absolute/M3JDK21
python3 receive.py --dataset filtered-navigation-20261018 --target /absolute/M3JDK21
```

Use a fresh `-Dm3.contract.output=/absolute/new/path` for another receiver-contract execution. The existing five actual SDK tests, eight preparation tests and nine receiver cases remain the qualification gate. The receiver validates every bound recipe and target input before its first per-file replacement, applies actual retained SDK Results, and writes nothing at the fixed point.

Do not edit dataset metadata, templates or manifests after SDK qualification: the existing receiver binds those bytes in `SDK_INPUTS.tsv`. A changed preimage requires a newly sealed successor and qualification. Historical datasets and receipts are preserved.

## Evidence and limits

`AUTHORING_WITNESSES/` retains failed baseline cases and candidate comparisons. These are authoring evidence; they do not stand in for checks on received product files. Current wrapper candidate evidence is 154,239 checks per tiered and interpreter run of the same corpus. The baseline has nullable-navigation, comparator-entry, read-only and deep-stack failures. The unchanged ordinary-read tape has SHA-256 `0a279ab0c033308d4ec3777b2d1328f4ff9dd4909e6a79e447418ad800f283ac`. Corrected entry-membership callbacks and null-sentinel comparator demand are explicit behavior repairs and are excluded from this tape's equivalence claim. When both stored extremes are nonnull, the null repair adds one opposite stored-boundary comparison; the first/last lookup order is retained, but universal comparator callback identity is not claimed.

The alias contract passes 2,302 checks with native libraries absent and 2,332 with actual JNI present. Both modes require pre-write rejection for every output/input alias, including empty windows, while preserving safe input/input sharing and earlier geometry-validation failures. The baseline witness records corruption in both Java and actual JNI; the generated JNI headers are byte-identical before and after.

Post-qualification receipts are kept outside this sealed dataset at `../../evidence/filtered-navigation-20261018/`. Runtime proofs must identify the received sources and actual native availability separately. Repeated execution modes are not added together as independent coverage.

Known generic continuation chains use O(d) temporary behavioral references and constant M3 call stack. Membership deliberately preserves repeated callback demand, including d(d+1)/2 evaluations for one always-visible element; it has no claimed linear-time improvement. Live owner, stored identity and encounter order are retained; no element snapshot, new native algorithm or throughput improvement is claimed. Opaque foreign decorators, deep sorted range construction, concurrent mutation, non-Linux native behavior and whole-JDK acceptance remain outside this finite pass.

The historical PR #1204 contract also promised strict empty sequenced endpoints for Map-derived views. Current outer JDK map wrappers remain unchanged and can still inherit `NoSuchElementException` for these endpoints. That stronger historical guarantee is unreconciled. Resolving it requires separate admission for concrete wrapper, marker and delegation behavior; this Set-focused repair is not universal read-only endpoint closure.
