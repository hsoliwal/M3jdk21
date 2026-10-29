<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Integrated M3JDK21 convergence

This review head combines the String, Pool, fixed lexicon, GVAS, JNI and build
prerequisite results with the array/mask collection branch. The Java runtime and
its native implementation belong to M3JDK21. The public String owner and its
existing M3 representation routes remain integrated in `java.base`.

The parent is [M3JDK21 #1445](https://github.com/hsoliwal/M3JDK21/pull/1445), commit
`494ca2cbbb393088c88b03946a695bd5164611f9`, containing the newer filtered/sorted
views and direct JDK-map owners from #1426 plus the bitmap/histogram refinements.
This head adds thirty-five received String/GVAS/native/build/workflow files and
the complete seventeen-file sorted-recipe repair from #1440. Review documents
are counted separately.

## Runtime changes

The final `java.lang.M3StringPool` retains the #1414 lookup, coder eligibility,
interning and metadata-budget behavior. It adds only the qualified fixed-address
admission atom. `src/java.base/share/native/libjava/M3StringPool.c` supplies its
built-in map/unmap JNI entry points. Warm lookup does not acquire a new JNI
transition. The existing GVAS literal reader receives its bounded exact lookup,
reverse offsets and process-identity checks from the retained canonical recipe.

| Operation | Retained contract and representation |
| --- | --- |
| Frozen exact lookup | Bounded primitive row directory; at most 64 probes, half occupancy and 16 MiB directory payload, with original exact fallback and UTF-16 verification |
| Reverse lookup and coder facts | Validation-time eligibility and cached row-to-atom owners; optional one-byte-per-row sidecar, with mandatory atom storage allocated first |
| Interning metadata | Existing monitor, captured-key/bucket identity checks, stale-capture retries and retirement of empty refused/capped buckets |
| Fixed mapped lexicon | Exact numerical address or refusal, no replacement of an occupied mapping, failed unpublished mappings released, successful mapping retained for VM lifetime |
| Arrays, offsets and masks | Retain #1445's ordered immutable projection search, bounded JNI stride batching and current live-view/map owners |
| Build prerequisites | Exact fleet doclint, scope-lint, native feature guard and Object vtable results, preserving public/protected Java descriptors |

The file's bytes **and length must remain immutable throughout the mapping
lifetime**. Admission verifies content and geometry; it cannot stop subsequent
external writes or truncation. A fixed payload address in one immutable image
does not establish a permanent semantic concept anchor across image revisions.

## Canonical recipes and catalogue

[Synexia PR #12462](https://github.com/hsoliwal/com.synexia/pull/12462) retains the
reusable recipes, validated algorithm decisions, source/engine custody, exact
SDK Results and execution evidence. Its immutable commit is
`540716d1002865188efc14457f51d5f84a9dbaca`.

- [Implementation and qualification guide](https://github.com/hsoliwal/com.synexia/blob/540716d1002865188efc14457f51d5f84a9dbaca/synexia-openrewrite-recipes/recipe-crates/m3jdk21-fixed-lexicon-convergence-20261019/README.md)
- [Seventy algorithm evaluations and search instructions](https://github.com/hsoliwal/com.synexia/blob/540716d1002865188efc14457f51d5f84a9dbaca/synexia-openrewrite-recipes/catalogs/m3jdk21-convergence-20261019.md)
- [Synexia M3 per-file refinement invariant](https://github.com/hsoliwal/com.synexia/blob/540716d1002865188efc14457f51d5f84a9dbaca/docs/M3-SCALE/invariants/SYNEXIA-M3-CATALOGUE-PER-FILE-REFINEMENT-1.md)
- [Exact serial SDK and receiver replay](https://github.com/hsoliwal/com.synexia/blob/540716d1002865188efc14457f51d5f84a9dbaca/synexia-openrewrite-recipes/recipe-crates/m3jdk21-fixed-lexicon-convergence-20261019/qualification/REPLAY.md)
- [Thirty-five final file identities and forty-six actual SDK result records](https://github.com/hsoliwal/com.synexia/blob/540716d1002865188efc14457f51d5f84a9dbaca/synexia-openrewrite-recipes/recipe-crates/m3jdk21-fixed-lexicon-convergence-20261019/root-receiving/TERMINAL_SDK_BINDING.json)

The catalogue preserves its thirty-three historical evaluations verbatim, then
adds thirty-seven current evaluations. Challenge sites supply algorithm discovery;
their content is not treated as automatic permission to copy implementation code.
All existing source notices remain. Each transformation admits its exact input,
transports actual class-backed OpenRewrite Results and reaches its own fixed point.
Later successor recipes can supersede earlier postimages; the whole sequence is
not claimed to be an unconditional replay over its final state.

## Executed evidence and limits

The [qualification index](https://github.com/hsoliwal/com.synexia/blob/540716d1002865188efc14457f51d5f84a9dbaca/synexia-openrewrite-recipes/recipe-crates/m3jdk21-fixed-lexicon-convergence-20261019/qualification/QUALIFICATION_INDEX_V2.json)
binds the individual proofs:

- Seven new recipes pass thirty-five original JUnit groups, twenty-eight partial
  states in both input orders and the unchanged sealing/receiver controls.
- The final String checker passes all 159 targeted refusals. A subsequent full
  source-checker run after CI receiving passes with all thirty-five current inputs
  and nineteen forbidden path states unchanged; the refusal suite was not rerun.
- The phase source gate passes fourteen relations and thirty test groups while
  preserving historical tables and leaving runtime promotion false.
- The native component passes strict compilation, JNI header/API checks, twelve
  child cases, eight-thread admission contention and two simultaneous JVM mappings.
- The sealed diagnostic M3JDK21 image passes four original jtreg fixtures and all
  six annotated main actions, including thirteen fixed-lexicon child cases,
  ordinary String boot, both CompactStrings configurations and intern identity.
- The received GVAS proof passes twenty-five process-identity tests, three JVMs
  with 15,433 checks each and five lookup fixtures totaling 43,015 checks.
- The collection parent's exact composed sources pass 1,941,255 focused assertions.

These are explicit source/component/diagnostic closures. A fresh strict build of
this entire combined checkout remains **NOT_RUN locally because of disk capacity**.
The diagnostic image has an inherited diagnostic build provenance; its original
`libjava` was reproduced byte-for-byte before adding the qualified object, and the
eighteen Java owners were compiled with its own matching M3 compiler/modules.
The first setup and descriptor failures remain in the evidence.

Hosted CI is reported from actual run results, separately from local qualification.
The full-image workflow retains its strict build commands, thirty-one existing
steps and forty-one existing jtreg tests, adding the fixed fixture and three path
filters. Workflow wiring alone is not a passing full build.

Linux was executed. Windows native execution is unrun; this fixed admission atom
explicitly refuses Darwin/iOS. No fresh latency or RAM-speed result is claimed.
Permanent concept anchors, complete hierarchical lexicon/grammar convergence and
the separately documented pre-existing negative-length `regionMatches` finding
remain outside the completed runtime changes.
