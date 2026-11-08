<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 array and collection contract refinement

This canonical Synexia task crate reuses the existing Java and text snapshot recipes, with unchanged engine sources. The first published checkpoint contains the qualified array refinement and the machinery fixture. Later source-sealed collection passes belong to this same crate.

The array dataset repairs scalar indexing across empty joined segments in `M3Arrays`, restores its two required snapshot segment-count methods, and keeps the existing native comparator test executable in Release builds. Four generated files include the existing Java owner, the native test, and two focused Java regression tests. Public interfaces and production JNI symbols remain unchanged.

## Reused owners and source epochs

`ENGINE_SOURCES.tsv` pins all four exact current engine sources to Synexia `10e8628b2479e56f6f841f160233d3a8bf04a6db`. `HARNESS_LINEAGE.json` identifies the previously qualified AST/build harness at `1d890f4c332bf2b548ddd4a1ef14dd599b4238bc`. This crate adapts its task identities, display strings, Maven coordinate and receipt label; it preserves the Java SDK qualification class, sealing/receiving algorithms, and refusal gates.

The array receiving baseline is `hsoliwal/M3JDK21@e913ab89056657eca4009678cfe9a657c020e79b`. Dataset resources retain exact preimages and postimages. Unknown source bytes are refused. The receiver applies actual SDK Results serially after validating every bound input and target; replay at the fixed point changes no files or timestamps.

## Reproduce the finite recipes

Use Java 21, Maven 3.9 or newer, and Python 3. From this crate in a canonical Synexia checkout:

```sh
mvn -B -ntp -Dm3.dataset=fixture -Pcontract verify
mvn -B -ntp -Dm3.dataset=m3jdk21-arrays -Pcontract verify
```

The POM pins OpenRewrite 8.17.1. Supply `-Drecipe.owner.root=/absolute/path/to/synexia-openrewrite-recipes` when the existing owner is outside its canonical location. Add `-o` when dependencies are cached. Each dataset writes its own `target/<dataset>` output. For a fresh repeated contract execution, use a new `-Dm3.contract.output=/absolute/new/evidence` directory.

Each dataset passed five actual SDK tests, eight input-sealing tests, and nine isolated receiver cases. These exercise Java LST and native/text lanes, exact replay, source order, mixed pre/postimages, fixed point, missing/duplicate/drift inputs, wrong representation, post-scan changes, recipe/result tampering, symlink refusal, and file modes/timestamps. `evidence/<dataset>/SDK_QUALIFICATION.json`, source/result bindings and raw reports retain the executed scope.

After qualification, receive into an isolated M3 checkout:

```sh
python3 receive.py --dataset m3jdk21-arrays --target /absolute/m3-checkout --check
python3 receive.py --dataset m3jdk21-arrays --target /absolute/m3-checkout
python3 receive.py --dataset m3jdk21-arrays --target /absolute/m3-checkout
```

A source-sealed successor is required for a changed preimage. Correct reviewed recipe inputs, execute the SDK gates, then receive their retained results. Per-file atomic replacement does not imply a transaction across multiple concurrently edited files.

## Algorithm comparison and receiving evidence

`CATALOGUE.tsv` uses the established `M3_COLLECTION_VIEW_ALGORITHM_DECISIONS_20261017.tsv` vocabulary. `catalogue/arrays-evaluation.json` records the authoring comparison: an existing first-party upper-bound atom was selected because it finds the first cumulative end strictly greater than the ordinal in O(log s) time and O(1) scratch while retaining the existing descriptors. An arbitrary duplicate match is incorrect; linear rewind loses the logarithmic bound. Existing Synexia and M3 owners are pinned. Guava and the three challenge platforms provide contract and adversarial-shape evidence; no challenge/editorial/submission source is copied and no Guava runtime dependency is introduced.

The frozen authoring evaluation retains its original pending reception markers. `catalogue/arrays-receiving-status.json` resolves those gates against the actual SDK-generated receiver and links the separate runtime receipts. On that receiver, Java, `-Xcheck:jni`, and interpreter JNI runs each passed 417,595 checks across 912 cases. The bounded resource probe used nine fresh JVMs: the unique-boundary comparison retained identical cold allocation and owner geometry, and two million warm scalar reads allocated zero bytes. These observations establish the measured scope; no throughput improvement is claimed.

The fixture qualifies transformation machinery. Native ABI, product behavior, allocation, hosted CI, non-Linux platforms, and full JDK acceptance have their own gates. String remains the active ordered receiving family; these separately licensed proving-owner repairs do not promote arrays or collections into the global JDK family sequence.

`NOTICE.md`, `LICENSE.txt`, `FOSS_REUSE_DECISION.tsv` and retained per-file headers preserve first-party Apache-2.0 attribution and donor boundaries. M3 runtime classes gain no Synexia runtime dependency.
