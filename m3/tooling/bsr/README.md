<!-- SPDX-License-Identifier: Apache-2.0 -->
# BSR: bitmap summary reuse

Synexia's existing bitmap rank/select summaries were exact and maintained on
mutation, but forward/reverse scans ignored them. BSR implements the missing
reuse in the source project, then transfers the two qualified scan atoms to the
existing JDK owner. Synexia remains the convergence workspace; M3JDK21 owns its
self-contained runtime, names, JNI linkage, build and admission decisions.

| Source contribution | Independent JDK target |
| --- | --- |
| `com.synexia.common.collections.SegmentedBitLane28` | `jdk.internal.mindex.M3BitLane28` |
| `nextSetBit`, `previousSetBit` | Same method descriptors and semantics |
| `M3HashPinnedJavaSnapshotRecipe("bsr")` | Existing Lane28 Java recipe plus `M3Jdk21HashPinnedTextSnapshotRecipe("m3-bsr")` |
| Synexia PR [9448](https://github.com/hsoliwal/com.synexia/pull/9448) | Source revision `a8e8b3ade47d04229aff2a280e67bad5741ed863` |

The scans validate bounds before returning from an empty bitmap. Prepared exact
zero counts skip empty regions/pages. Cold scans retain their traversal and do
not prepare counts. No new cache, public API, per-entry object, native ABI or
runtime dependency is introduced. Stable primitive pages, lazy metadata,
allocation counts, rank/select and single-threaded mutation semantics remain.
The original `libjava` range-count peer is unchanged and still independently
compiled and exercised through JNI. This does not replace public `java.util`
classes or change the Java SE compatibility baseline.

## Reproduce and install

Use Java 21.0.8+9, Maven 3.9.9, Linux GCC and the JDK JNI headers:

```sh
mvn -B -ntp -f m3/tooling/bsr/pom.xml verify
mvn -B -ntp -f m3/tooling/bsr/pom.xml -Dbsr.sanitize=true verify
python3 m3/tooling/bsr/verify-plan.py
mvn -B -ntp -f m3/tooling/tq-sync/pom.xml verify
python3 m3/tooling/tq-sync/verify-plan.py
mvn -B -ntp -f m3/tooling/lane28/pom.xml verify
python3 m3/tooling/lane28/verify-plan.py
mvn -B -ntp -f m3/tooling/tq/pom.xml verify
python3 m3/tooling/tq/verify-plan.py
python3 m3/migration/migration.py validate .
```

The existing Python validator requires `m3/migration/requirements.txt`. Local
Maven evidence used an offline, prepopulated dependency cache. It does not prove
repository-local dependency-mirror completeness.

The executed outer recipe produces 17 exact outputs and checks nine existing
dependencies. It carries the runtime owner, jtreg probe, existing Java/text
recipe resources, mapping, image test and both existing workflows. The inner
Lane28 Java recipe separately reproduces the adapted owner as a Java LST.
No recipe engine is replaced. Author changes in recipes, qualify the generated
postimages, then materialize through the existing sealed installer:

```sh
python3 m3/migration/recipe.py check --root . --plan m3/tooling/bsr/plan.json
python3 m3/migration/recipe.py apply --root . --plan m3/tooling/bsr/plan.json
python3 m3/migration/recipe.py rollback --root . --plan m3/tooling/bsr/plan.json
```

Use mutations only in an exclusively owned matching worktree. They refuse
foreign or mixed bytes and dependency drift. Reapplication has zero writes.
The custody verifier exercises apply/fixed-point/rollback and seven no-write
refusals, including occupied output and broken predecessor/successor links.

The preceding TQsync packet's 24 historical outputs and sealed plan stay
immutable. Its live-tree check accepts BSR only after validating the complete
sealed successor and proving each overlapping `TQsync.after == BSR.before`.
Outputs outside that successor must still equal their historical postimages.
The predecessor's original generated-byte, mapping, replay and refusal checks
remain. BSR tests the actual amended predecessor check against preimage drift,
a missing link, a foreign successor and current-file drift. It cannot silently
accept an arbitrary newer tree.

## Evidence and limits

The BSR source has 10 passing tests in release and nonrecovering UBSan modes.
The target has 11 in each mode, adding byte-exact source-atom adaptation.
Each baseline/candidate interpreter/mixed/C1/C2 execution makes 20,636 checks,
4,000 seeded mutations and 126 JNI comparisons. Independent `TreeSet`/`BitSet`
oracles cover bounds, word/page/region transitions, maximum domain, clear and
repopulation, lazy metadata and stable arrays. Public/protected descriptors are
identical. Normal C1/C2 must actually compile both directional methods. Inverted
region/page-count and premature-empty-return mutants must fail.

Test-only counters measure the frozen 512-page cleared-storage fixture:

| Bitmap-word reads | Before | After |
| --- | ---: | ---: |
| Empty lane | 65,536 | 0 |
| Cold scans with live endpoints | 65,664 | 65,664 |
| Scans after existing rank preparation | 65,664 | 256 |

Preparation is explicit `rank(1)` outside the measured warm scans. These are
bounded work counts, not elapsed-time measurements or a universal speedup.
Dense workloads may pay additional metadata checks and require measurement.
Production code has no counters. Existing Lane28, TQ and TQsync suites and
custody gates remain mandatory; exact local results are in `evidence/receipt.json`.

Local runtime tests isolate the exact product classes in stock Java 21's
`java.base`, with a test-only bootstrap loader for the unchanged JNI peer.
That is not a rebuilt fork-image pass. Existing image CI now includes the new
probe, image-local `libjava`, actual normal C1/C2 scan methods and the retained
M3 String interpreter boundary. `UseM3StringStorage` still forces interpretation;
M3-enabled C1/C2 requests must show interpreted mode and zero compiled methods.
Both existing workflows run BSR release/UBSan and custody before older gates.

Full current-image, complete jtreg/TCK, repository-wide reactor/admission,
cross-platform, memory/performance, remaining collections and automatic JDK
consumer gates remain open. The map keeps 52 records and 20 gates with no status
promotion. Four collection records only advance their shared recipe identity;
the bitmap record gains source/target lineage and this receipt. All other 47
records remain byte-for-byte equivalent as parsed data. Completion remains
`INCOMPLETE`.

## Provenance and feedback

`donor/` retains the exact qualified Synexia owner, current source LICENSE and
NOTICE. `pins.json` records their immutable Git/SHA identities, target preimage
and permitted adaptations. Existing distribution notices and other licenses
retain their scope. The earlier Lane28 donor and recipes remain historical
evidence. The source catalogue reviewed the existing challenge categories and
pinned RoaringBitmap comparison; no challenge solution or external body was
copied for this change.

Feedback to Synexia: reuse maintained exact facts before adding a hierarchy;
check bounds before empty shortcuts; qualify cold and warm states separately;
include clearing, repopulation and new-region allocation after preparation.
Future ports must compare current target history and keep JDK-specific JNI and
compiler constraints. Preserve owner/generation binding for other precomputed
facts; this bitmap's single-threaded mutable-owner proof does not establish a
classloader, versioned AST or general regex optimization.
