<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 product receipt: adaptive physical-index sorting (2026-10-23)

This is an original M3JDK21 product-source refinement, not a compatibility overlay. The original package-private owner is m3/collections/src/main/java/com/m3/collections/M3IndexSort.java. This pass neither changes the JDK java.util public surface nor claims an installed JDK runtime.

## Exact custody
- Master source commit (before): b032048eece51c897d8623fd7a69f7f16eac5a57
- Existing source Git blob (before): 30a3bcb1d3a58935c548c40b99862b5643484e06
- Result source Git blob: a62fc49228ac09213bf44c3a9075098f1d5c65ef
- Result standalone contract Git blob: 49a913ba2427451e12ae4016d2b529af18f3fbef
- Result JUnit wrapper Git blob: 15d06bd2740ea7c9ebc320b697a2ac347b446d1d
- Canonical source-sealed Synexia recipe: hsoliwal/com.synexia PR #13224, commit 9524650e3f4743bf22f79945526560277eb41d7c, recipe com.synexia.m3.Jdk21IndexSortAdaptive20261023. This product branch contains only admitted Java result sources/tests and this data-only receipt, not recipe code.

## Contract and storage
Use existing parent-owned dynamic Object[] and primitive int[] physical positions. The original M3IndexSort.stable signature is retained; no new public method, type, JNI export, callback-to-native ABI, per-row Entry object or payload copy. The default stable(...) retains the original bottom-up merge kernel and its comparator callback/exception transcript. A separate explicit stablePrepared(...) route permits a linear no-scratch path for already-ordered indexes and bounded <=32 in-place stable insertion, only when callbacks are proven pure/deterministic with no observable invocation count/order. Neither source payload nor caller comparison implementation is transferred into native memory.

## Executed local bounds
PASS: strict javac --release 21 -Xlint:all -Werror, executable Java21 oracle with 6,576 deterministic assertions, including duplicate and signed physical IDs, ties, natural/reversed/absolute-value comparators, exceptions, and the 31/32/33 boundary. Source-seal input/output git blobs match; local replay refused source drift and missing product owner, and passed second fixed point.

NOT_RUN: full Maven reactor, actual OpenRewrite Recipe.run, source-built JDK21, jtreg/JCK, JNI provider parity (native source untouched), Windows/macOS, installed image, CPU throughput and heap allocations. Default callback/exception transcript is preserved; only explicit stablePrepared is allowed a different callback schedule. No green claim for queued, unrun or skipped CI.

## Product boundary
m3/collections is an existing M3JDK21-owned Java module, but this PR alone does not prove it is included in the installed JDK image or that java.util implementation is replaced. Preserve String -> arrays -> collections promotion gates. Keep this receiver as DRAFT/HOLD until actual full qualification.

## Corrective source-receiving pass
The implicit optimization of default stable(...) was rejected after a RED comparison
showed changed user callback order and exception-time physical indexes. The
obsolete intermediate Git blob f786685aa7c7cf0e4e6a1b975f98227449fb637a
remains solely in Synexia correction memory. The default merge algorithm is
restored; no existing collection caller silently opts into stablePrepared.

New strict Java21 source contract blob: 7a61f99ac3fb5703385b61ea99b0e763a53ae82f
New strict JUnit wrapper blob: ea757248616e6ba01fe78625ea0696b6ff3969cb
Original adaptive JUnit wrapper remains 15d06bd2740ea7c9ebc320b697a2ac347b446d1d.
Corrected Synexia recipe source commit: 9524650e3f4743bf22f79945526560277eb41d7c.

Executable focused evidence: local normal/JVM interpreter adaptive oracle
6,576 assertions, strict callback/exception oracle 9 assertions; sealed
differential with original baseline 48,752 normal cases and 2,432 interpreter
cases. These do not certify full owning module Maven/JUnit, JDK image/jtreg,
JNI mask parity or runtime performance. Explicit prepared lane has no
production caller in this bounded pass; therefore no production speedup is
claimed. Keep draft/HOLD until remaining qualification runs.
