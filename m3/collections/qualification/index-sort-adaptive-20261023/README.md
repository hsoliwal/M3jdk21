<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 product receipt: adaptive physical-index sorting (2026-10-23)

This is an original M3JDK21 product-source refinement, not a compatibility overlay. The original package-private owner is m3/collections/src/main/java/com/m3/collections/M3IndexSort.java. This pass neither changes the JDK java.util public surface nor claims an installed JDK runtime.

## Exact custody
- Master source commit (before): b032048eece51c897d8623fd7a69f7f16eac5a57
- Existing source Git blob (before): 30a3bcb1d3a58935c548c40b99862b5643484e06
- Result source Git blob: f786685aa7c7cf0e4e6a1b975f98227449fb637a
- Result standalone contract Git blob: b6f558d55df9715b6536dcc7fbcecb4dd05aeba7
- Result JUnit wrapper Git blob: 18862e0b96dfa096fc76c27605e88f19b8265937
- Canonical source-sealed Synexia recipe: hsoliwal/com.synexia PR #13224, commit ca5c5cc0668187bf85562dbe1fd1653b95ef8480, recipe com.synexia.m3.Jdk21IndexSortAdaptive20261023. This product branch contains only admitted Java result sources/tests and this data-only receipt, not recipe code.

## Contract and storage
Use existing parent-owned dynamic Object[] and primitive int[] physical positions. The original M3IndexSort.stable signature is retained; no new public method, type, JNI export, callback-to-native ABI, per-row Entry object or payload copy. Already-ordered indexes take a linear no-scratch path; disordered <=32 indexes sort stably in-place; larger disordered indexes use the original stable bottom-up merge kernel. Neither source payload nor caller comparison implementation is transferred into native memory.

## Executed local bounds
PASS: strict javac --release 21 -Xlint:all -Werror, executable Java21 oracle with 6,576 deterministic assertions, including duplicate and signed physical IDs, ties, natural/reversed/absolute-value comparators, exceptions, and the 31/32/33 boundary. Source-seal input/output git blobs match; local replay refused source drift and missing product owner, and passed second fixed point.

NOT_RUN: full Maven reactor, actual OpenRewrite Recipe.run, source-built JDK21, jtreg/JCK, JNI provider parity (native source untouched), Windows/macOS, installed image, CPU throughput and heap allocations. Comparator call transcript can differ; admission relies on pure deterministic comparator ordering. No green claim for queued, unrun or skipped CI.

## Product boundary
m3/collections is an existing M3JDK21-owned Java module, but this PR alone does not prove it is included in the installed JDK image or that java.util implementation is replaced. Preserve String -> arrays -> collections promotion gates. Keep this receiver as DRAFT/HOLD until actual full qualification.
