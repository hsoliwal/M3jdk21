<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Independent review of prepared escape masks

**Result: no correctness blocker found in the reviewed candidate.** This is a source and evidence review performed separately from the authoring and runtime qualification. No target source, recipe payload, public API, or remote branch was changed by this review.

## Exact source binding

The five files below were read from the actual SDK output at `target/candidate`, matched against `SDK_RESULTS.tsv`, and checked for byte identity with their sealed recipe resources. The two core hashes remained unchanged across the preceding pilot and final five-target SDK run.

| Emitted target | SHA-256 |
| --- | --- |
| `m3/ports/compiletime/src/main/java/com/m3/compiletime/M3EscapeFlow.java` | `180488506ddfd0709530a7f164217d684389b4d5e77ce56b204bb0df4ad153de` |
| `m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ItemReferenceScan.java` | `39881510663e11b27b59fc5f1ff8e1ff86e793a2f5d7ef36cef42023aebbccbf` |
| `m3/ports/compiletime/src/main/java/com/m3/compiletime/M3GvasLiteralHarvester.java` | `31dd73ae6b8621eb326af013560fb1fca09650248fa67dba7daf526f7c385dd6` |
| `m3/ports/compiletime/src/test/java/com/m3/compiletime/M3EscapePreparedTest.java` | `ea8bed1a238df1964d11b49e3a5742f44f27ff262c0fe1741bbaa0bcf5a23dcb` |
| `m3/docs/name-mapping.d/m3-program-escape-precompute.json` | `469cfac93eae278aeb5914dc301a68efaf732baf3216b96009ca68308b8f0be0` |

The source-binding receipt hashes are:

| Receipt | SHA-256 |
| --- | --- |
| `target/candidate/SDK_INPUTS.tsv` | `9720eb424b1872dfa0f22b93f1bbfbbfd00f1ff2458a3c1c58e056aaaea1b194` |
| `target/candidate/SDK_RESULTS.tsv` | `ed2fde639cb9f4e13cc82cf75965e24c592e502b3c4e0c5882f9617d1e6f68fc` |
| `target/surefire-reports/TEST-com.synexia.rewrite.M3TaskRecipeTest.xml` | `a055e885d201d8c27d482f9b7d0bc6c73eff0cd8be508916a3182ab32d6b3f86` |

The inspected final SDK report records five tests, zero failures, zero errors, and zero skips. This establishes the named recipe execution and source binding; it is not a runtime or performance result.

## Properties inspected

The prepared equation is the distributive reverse of the retained forward classifier. If a copy instruction `q` receives provenance from `p`, the forward classifier propagates a selected site's taint from `p` to `q`. Seeding each direct consumer's operand provenance and propagating `mask[p] |= mask[q]` therefore computes the same least fixed point for every possible site. The result retains every escape-kind bit; it does not stop after the first retaining category.

The source review checked the following obligations:

- The copy-input table matches the corrected forward classifier for `ALOAD`, `ASTORE`, `CHECKCAST`, every supported `DUP` form, `SWAP`, `AALOAD`, and the three exact `Objects.requireNonNull` descriptors. Category-two values follow ASM's frame coordinates. Conservative duplication of edges is harmless because joins are idempotent.
- Direct-consumer masks preserve field, array, return, identity comparison, identity hash, monitor, callee-argument and invokedynamic classifications. Receiver and argument positions use the same ASM frame convention and exact callee policy as the retained implementation. String-concatenation bootstrap handling remains unchanged.
- The primitive ring queue has at most one queued entry per instruction. Dequeue clears membership before propagation, allowing a later gained bit to requeue a node. Empty methods, self edges, cycles and merges do not require a special unbounded queue or recursive traversal.
- All eleven bits propagate monotonically. An instruction can acquire each bit only once, bounding closure by the stated `O(11 * (N + E))` work. The returned array gives constant-time site lookup without an all-sites reachability matrix.
- Edge, primitive-payload-byte and source-visit limits use wide arithmetic before narrowing or allocation. Budget refusal returns `null`; the caller then uses the corrected forward classifier. A partially populated answer is never published. The documented byte budget excludes ASM state, VM array headers and temporary descriptor or policy objects; it is not a bound on total JVM allocation.
- Null analysis frames contribute no edges or consumers. Existing analysis failure and frame-budget failure still produce `ANALYSIS_FAILED` through the caller. No claim is made that this package-private seam validates arbitrary fabricated or concurrently mutated ASM frames.
- Method instructions, frames and the callee table are fixed for a preparation call. Only primitive answers escape it. There is no persistent cache, no new canonical graph owner, no new public plan API, and no change to the scanner's site order, BCI calculation or record format.

The preimage omitted `SWAP` propagation. Adding it to both implementations is an intentional correctness repair, not a claim of byte-for-byte output equivalence on that counterexample. The retained tests construct legal stack examples and execute generated witnesses showing that the reference can reach a field through either swapped position.

The `M3GvasLiteralHarvester` delta is limited to renaming the first local declaration and its two uses from `destination` to `requestedDestination`. This repairs the baseline duplicate-local compilation failure while preserving the CLI argument, guard and output path expression. No other harvester logic was changed in the inspected diff.

## Test and qualification scope

The test source was reviewed for all-instruction comparison against the retained forward classifier, legal stack shapes, loops and merges, wide invocation descriptors, full mask preservation, exact callee-policy changes, null and unreachable frames, budget boundary/fallback behavior, and independent parallel/serial preparation calls. The executable `SWAP` witnesses provide an additional oracle for the intentional correction.

The preceding pilot's JUnit XML was inspected independently: 27 available port tests passed, comprising nine prepared tests, seven item-reference tests, and eleven retained regex/format tests. That count belongs to the preceding pilot; it does not include the later parallel test or the restored harvester tests.

**Full final runtime gate: pending at the time this review was sealed.** The author was running the final source contract under `/tmp/m3-program-escape-qualified-v2`. This review does not assert that unfinished result or any throughput improvement. Its independent conclusion is limited to the exact source, SDK binding, and properties recorded above; the final qualification receipt remains the authority for runtime results.

## Final single-site selection and V4 receipt addendum

The preceding section remains the exact V2 review history. Subsequent qualification retained a real process-deadline failure, a successful V3 run, and its measured single-site preparation cost. The final V4 caller now selects the corrected forward atom when there is exactly one obtaining site. That choice is an ordinary existing-implementation selection, not a change to the graph equation or escape categories.

The tiny caller delta was inspected independently. The existing outer loop skips methods with an empty obtaining list before invoking `method`. Within `method`, `frames == null || sites.size() == 1` leaves `prepared` null. The unchanged result selection still emits `ANALYSIS_FAILED` for absent analysis, otherwise executes the corrected forward classifier for the one site. Multiple-site methods prepare once, and a preparation-budget refusal uses the same fallback. Frame construction, site coordinates, BCI/record order and public API are unchanged. No correctness blocker was found in that selection.

The final caller SHA-256 is `eadaa78df40c19062ac56119fb123e31faf2dcc8e9b302e29a415bc7798e35f0`; the updated provenance shard is `d293b891ed743279037f4cf4417bf8efaae9fab186dee4a3cc6bb69dc78a0e8f`. The graph owner, GVAS local-name repair and test source retain the hashes in the original table.

| Final V4 receipt | SHA-256 |
| --- | --- |
| `target/candidate/SDK_INPUTS.tsv` | `e3c442a0973fe61a69c1768682023d1f1970b27bf24c2f945a4a5ba8aba0c4e9` |
| `target/candidate/SDK_RESULTS.tsv` | `69b360a1288ec8ecf5fdb9d67867e1b6f941e9e12f47cd538a1cf37343ab3c9d` |
| `target/surefire-reports/TEST-com.synexia.rewrite.M3TaskRecipeTest.xml` | `065c2b1dcf7ad3400dcb5b07f9969b20d202efb7e446a1f9f0dd6b63baa2c6d9` |
| `/tmp/m3-program-escape-qualified-v4/receipt.json` | `4daa23b7a980ad7d0d500bd053f8a8a0ba52f6498aeac5a83c9afc29a2e32ec6` |

The final SDK files and runtime receipt were read and rehashed independently. They record five passing SDK tests, 15 completed source-contract commands, 18 passing available-baseline JUnit tests and 30 passing complete-candidate JUnit tests, with zero failures/errors/skips in both suites. The receipt reports unchanged source inputs and public API, the exact forward-oracle restriction to the admitted SWAP correction, and only the three bound GVAS identifier changes. Its status remains `PASS_WITH_EXPLICIT_HISTORICAL_CUSTODY_HOLD`; historical stale custody is not converted into a pass.

The CPU/allocation and retained-array rows describe finite synthetic methods with precomputed ASM frames. The one-site diagnostic can still explicitly measure a forced preparation for comparison even though the production scanner now chooses forward execution for that shape. Neither those figures nor this review asserts an end-to-end compiler speedup, general ownership/GC proof, full reactor pass or rebuilt JDK qualification.
