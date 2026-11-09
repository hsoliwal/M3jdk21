<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 array/mask owner continuation — 2026-10-20

M3JDK21 is the product. This changes the existing Java and C17 selection owners,
not an M3-over-JDK layer, a second collection hierarchy, or a runtime Synexia dependency.
Canonical reusable recipe custody remains in Synexia; the product copy is an exact
qualification/receiving packet, not a new canonical recipe owner.

## Exact source and reuse

Receiving parent: M3JDK21 #1515, `a7b83b8dff260cf415016fd7ed5617cc01f028cb`.
Reuse the reviewed private atoms from M3JDK21 #1280 at
`05a37a87a10fb7583770fdccecea4af54751bf39` / Synexia #12260 at
`a0f22da4f2bd953a116f124adbe7784f9e5e419d`. This is the versioned continuation
of that existing mask-window recipe crate, not another transformation framework.

- `M3IndexMaskCompactor`: checked full-array/window paths share a private stable
  emission atom for each primitive width. Preserve required/excluded position
  masks, unsigned byte semantics, validation precedence and native-required behavior.
- `m3_index_mask_compactor.c`: int full/range/window exports share the offset-aware
  existing kernel. Entirely rejected chunks skip row-payload Region reads; the
  second state scan and count-drift refusal remain. Long windows reuse the same
  first-match atom. Scratch remains bounded to 256 primitive elements per lane.
- `M3LongIndexProjection` and `M3LongAddressKernel` remain byte-exact KEEP inputs.
  The parent's stable equal-value-run reversal, shared immutable payload and
  logical/physical offsets survive. Its live filtered wrapper owners are untouched.

No retained Entry/Node per row, copied object payload, Java callback through JNI,
critical-array pin, escaping native pointer, public API addition, or JNI symbol
change. All nine existing JNI exports and generated selector header are unchanged.

## Contract and lifecycle

Inputs must remain stable for a call. Output is detached, exact-sized and preserves
encounter order and duplicate occurrences. Zero-mask semantics and selected signed
extremes are preserved. Count drift is a diagnostic, not an atomic concurrent
snapshot promise. Caller predicates/comparators remain in Java; this primitive
selector is not a replacement for a live filtered or sorted collection contract.
No new cancellation promise is made for native calls. Source-seal cancellation
proof applies only to the existing manifest kernel.

## Recipe first and serial source refinement

Unchanged `M3GpuToolJoinManifest` and `SealReplay` validate the complete hash-pinned
batch before serial output. Templates were authored first; the existing Java driver
emitted two production files and one original synthetic proof. Failed transformations
must be corrected in the recipe, not patched in receiving source. The five manifest
rows include two KEEP dependencies. A second application writes nothing.

The included Maven POM invokes this same exact-source recipe and qualification
entrypoint. Actual Java kernel replay was executed. **Maven returned 127 because
mvn is unavailable. OpenRewrite's attributed SDK/scheduler was NOT executed.**
Do not conflate Java manifest replay with either of those gates.

## Fresh executed evidence

Java 21 strict compilation, generated JNI headers, C17 warnings-as-errors,
nonrecovering UBSan, and actual JVM `-Xcheck:jni` were run on complete selected owners.
No runtime dependency stubs replace those owners.

- Java corpus: 236,639 assertions per baseline/candidate, identical digest.
- Native corpus: 320,006 assertions per run in all four baseline/candidate Java/C
  pairings, identical digest. Candidate interpreted mode also matches.
- All 65,536 required/excluded-mask pairs; chunk seams; independent offsets;
  null/invalid/overflow refusals; duplicates/extremes; 400 sorted/sliced/filter
  scenarios with 12 operations each; concurrent independent calls.
- 21 missing-native refusals. A separately labelled legacy-ABI **test fixture**
  removes the excluded-mask exports: required-only paths work, stronger requests
  refuse. This is not a retrieved historical native binary.
- Four public JVM descriptor comparisons, one generated selector JNI header and
  nine exports match. This is not whole-JDK ABI certification.
- Eight partial source states converge; seven pre-write refusals leave source bytes
  unchanged; two manifest-kernel cancellation checks pass. A deliberately corrupted
  native row emission is rejected by the real-JVM corpus.

For a 1,024-row/four-chunk function-table probe, payload Region calls change from
4 to 1 with one matching chunk and from 4 to 0 with none. Both scans still read
8 state chunks. All-selected payload reads remain 4. The probe includes actual C
source and has 54 checks per variant; its fake JNI function table is a work counter,
not a substitute for the separately executed JVM tests. These are operation counts,
not wall-time, RSS, JMH, or universal speedup measurements.

## Reproduce selected owner qualification

Run only on an owned disposable checkout/bounded source copy matching SOURCE_PINS:

```sh
bash PATH_TO_THIS_PACKET/verify.sh /absolute/disposable/M3jdk21
mvn -B -ntp -f PATH_TO_THIS_PACKET/pom.xml \
  -Dtarget.root=/absolute/disposable/M3jdk21 verify
```

The shell entrypoint was executed successfully. It is Linux/JDK21/GCC specific.
The portable companion bundle includes exact original owners, extra cross-version
and refusal drivers and raw logs. `evidence/EXECUTION.json` records the boundary.

## Catalogue and remaining gates

`CATALOGUE.tsv` retains the existing schema and records selected/rejected atoms,
complexity, compatibility and rights. Prior LeetCode/HackerRank/GeeksForGeeks
category evidence is carried forward separately; no new page-level review or
challenge solution copy is asserted. Reused first-party GitHub code remains
Apache-2.0 with its notices. No marketplace or client-private data enters fixtures.

Full owning-module/JUnit acceptance, installed M3JDK21 image/jtreg/JCK,
OpenRewrite SDK, native OS/architecture portability, live-wrapper whole-module
requalification and representative latency/CPU/RSS benchmarks remain HOLD.
The parent #1515's earlier results are provenance, not newly executed results here.
Other strings, AST/compiler and marketplace programme items are not completed by
this patch. No cache/proof version may silently reuse older source receipts.
No merge, rebase, force push, default-branch mutation or automatic promotion.
