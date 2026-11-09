<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Program escape precomputation

This canonical Synexia task recipe prepares the existing M3 compile-time escape
classifier for repeated obtaining-site queries within a method. It reuses the method's ASM source frames, builds a
bounded primitive dependency graph, and joins escape masks backwards to answer
each obtaining-site query with one array lookup. It also repairs a demonstrated
`SWAP` provenance omission and the local-variable collision that blocked the
complete compile-time port from compiling.

Recipe identity: `com.synexia.m3jdk.ProgramEscapePrecompute20261017`.
The five receiving paths are pinned to public M3JDK21 revision
`d47a4b4fdb8e3794ec56768b06aa9e23730c6b8e`. The four reused Maven/OpenRewrite
owners and private donor review are pinned to Synexia revision
`81c4f19a5ce5a68da316ff6f2802f2041a684f31`. Exact preimages, postimages, parent
inputs and SDK owner hashes are recorded in the manifests and
`contract/input-seals.json`.

## Existing owners and receiving changes

| Receiving path within `m3/` | Admitted change |
| --- | --- |
| `ports/compiletime/src/main/java/com/m3/compiletime/M3EscapeFlow.java` | Prepare one primitive CSR/worklist result; retain the original forward classifier as the budget fallback; correct its missing `SWAP` copy case. |
| `ports/compiletime/src/main/java/com/m3/compiletime/M3ItemReferenceScan.java` | Retain the corrected forward query for one obtaining site; for repeated sites, prepare once after the existing frame analysis and share the result. |
| `ports/compiletime/src/main/java/com/m3/compiletime/M3GvasLiteralHarvester.java` | Rename exactly three uses of the input `String` binding to resolve the duplicate `destination` declaration. The later `Path` binding and no-clobber behavior remain intact. |
| `ports/compiletime/src/test/java/com/m3/compiletime/M3EscapePreparedTest.java` | Add the absent-before differential, bytecode, policy, concurrency and budget regression corpus. |
| `docs/name-mapping.d/m3-program-escape-precompute.json` | Add the exact provenance shard with `PROVING_GROUND_CANDIDATE` disposition and no receiving-phase advancement. |

The public scanner API, site order, bytecode offsets, FNV identity, descriptor
policy, null/dead-frame handling and existing frame-analysis budget remain
unchanged. The complete forward implementation from `escape` onwards is
mechanically compared with its frozen preimage, permitting only the single
`SWAP` case addition. The harvester repair is mechanically restricted to its
three bound identifier occurrences.

`SWAP` is an intentional observable correction: the original classifier
reported no escape for the retained field-store witness, while the corrected
classifier reports `FIELD`. Equivalence of preparation is established against
that explicitly corrected forward oracle, not against the original wrong
answer on this witness.

This is program-level reuse of method topology and consumer facts. It does not
infer dynamic execution counts, rewrite bytecode, replace ASM, or grant general
ownership, lifetime or GC permission. There is no retained cross-method cache
or Synexia runtime dependency.

The existing scanner already skips methods without obtaining sites. The
single-site admission choice follows the retained v3 CPU/allocation result:
the optional graph cost more than the forward query for that fixture, while
sharing preparation across 8 and 32 sites reduced both costs. The source
uses the existing exact `sites.size()` value; no speculative runtime
counter, global threshold tuner or second analysis is added.

## Bounded preparation

For every copy instruction `q` with source predecessor `p`, the original
classifier propagates taint from `p` to `q`. The prepared solution seeds each
directly consumed source with the unchanged use mask and solves
`mask[p] |= mask[q]`. The monotone least fixed point includes loops and merged
`SourceValue` producers. All eleven escape bits are retained.

The implementation adapts the existing Synexia CSR count/prefix construction
and finite worklist pattern inside `M3EscapeFlow`. It does not import the
donors' Horn rules, labels, weights, graph objects or another plan owner.
`ALGORITHM_CATALOGUE.tsv` records exact donor paths, revisions, licenses and
the LeetCode, HackerRank and GeeksforGeeks taxonomy decisions. No challenge,
editorial or donor implementation body was copied.

Preparation admits at most 1,048,576 dependency entries, 4,000,000 charged
source visits and 32 MiB of declared primitive array payload. That payload
accounting is `13*N + 4 + 4*E` for instruction count `N` and dependency-entry
count `E`; it excludes VM array headers, existing ASM frames and transient
descriptor/key allocations. The only retained prepared result is `int[N]`.
Budget refusal returns to the corrected forward classifier. The queue holds
at most one entry per instruction, and each escape bit is added at most once
per instruction. The closure is bounded by `O(11*(N+E))`; a site lookup is
`O(1)` after preparation.

## Reproduce the source qualification

Use Java 21 and the approved offline Maven cache. From this crate:

```sh
export JAVA_HOME=/absolute/path/to/jdk-21
export M3_MAVEN=/absolute/path/to/apache-maven-3.9.11/bin/mvn
export M3_MAVEN_REPOSITORY=/absolute/path/to/approved-maven-repository
export JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=2
"$M3_MAVEN" -o -B -ntp \
  -Dmaven.repo.local="$M3_MAVEN_REPOSITORY" \
  -Pcontract \
  -Dm3.contract.target=/absolute/path/to/pinned-m3jdk21 \
  -Dm3.contract.output=/absolute/path/to/new-proof-directory \
  verify
```

The default SDK owner root is the enclosing canonical
`synexia-openrewrite-recipes` module. A qualified authoring mirror may supply
`-Drecipe.owner.root=/absolute/path/to/exact-sdk-owner-root`. The five actual
OpenRewrite tests execute the named descriptor, verify fixed point and input
order, mixed pre/postimages, drift/missing/duplicate/wrong-representation
refusal, post-scan drift refusal, and source-bound emitted Results.

The contract gate consumes those actual Results, validates all 141 frozen
parent inputs, and stages isolated baseline/candidate sources. It compiles
the exact vendored ASM sources and the complete compile-time port with its
precompute main-source dependencies. The existing Surefire/JUnit stack then
runs the entire port test suite. It retains the original failure witnesses,
public API comparison, CPU/allocation probe and actual VM retained-array
measurement. `QUALIFICATION.md` states the executed results and limitations.

For a read-only receiving check after qualification:

```sh
python3 receive.py --target /absolute/path/to/pinned-m3jdk21 --check
```

The existing thin receiver admits every file before serial replacement and
accepts only exact preimages or postimages bound to successful SDK Results.
It does not execute templates or provide a second rewrite engine. Target
application and target-specific promotion remain the receiving owner's work.

## Qualification boundary

The historical whole-port custody checker already refuses its stale source
inventory. Its exact diagnostic is preserved; no old manifest, historical
template or validator is weakened. This crate qualifies its explicitly
bounded successor inputs. Full product-reactor and JDK image qualification,
general escape-analysis soundness and ordered full-family promotion remain
outside this task's pass.

Cold graph preparation and warm lookup are measured separately for one-site
and repeated-site methods. The diagnostic uses shared, already-computed ASM
frames and finite synthetic methods; it is not an end-to-end compiler
benchmark or evidence that graph preparation wins for every workload. No
blanket CPU, heap or application speedup is claimed.
