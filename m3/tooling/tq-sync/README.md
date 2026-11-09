<!-- SPDX-License-Identifier: Apache-2.0 -->
# TQ reconciliation

Synexia is the convergence workspace and contributor. M3JDK21 is the independent
runtime target. This successor packet repairs the target's recipe custody while
retaining its current runtime owner byte for byte. The repository-wide port stream is
documented in [`m3-porting.md`](../../docs/m3-porting.md), following the established
M3String names and ownership model. Maven/OpenRewrite and the
native TQ oracle remain build/test tooling; the runtime patch depends only on
`java.base`.

## Reconciled history

The initial TQ port is `b68391863561ce80169663cd2b49c2845e084b74`. Commit
`bb0461ea8adc5b93771596a0fc8e8981a1e9e328` renamed the backing owners to M3 names;
`1ba43c787d44cf46324634efee7bceacd9fcec4e` added `Facts.containsAll`. The live
owner included both changes, but dependency pins still named removed `MIndex*`
paths and the recipe template omitted containment. Baseline Maven execution
failed on the removed `MIndexStringBacking.java` dependency.

| Contribution / contract | Target owner / decision |
| --- | --- |
| `com.synexia.indexstring.MIndexRegexTrigramQuery` | `jdk.internal.mindex.M3TQ` |
| Current canonical payload backing | `M3StringBacking` / `M3MappedStringBacking` |
| Target `Facts.containsAll` | Retain exact sorted-set inclusion; qualify independently |
| Shared mapping | Advance TQ only; retain the other 51 records and all 20 gates |
| M3 String compiler boundary | Retain `UseM3StringStorage` interpreter guard |

The mapping retains the earlier target in `lineage.previous_targets`; the
successor's `.before` resources retain the previous plans, manifests, workflows,
pins and mapping snapshots. The original TQ and Lane28 receipts remain historical
evidence. The TQ reconciliation limitation described in Lane28's initial
`RESTORATION.md` is resolved by this packet's focused gates, not by rewriting that
receipt or claiming a current full-image pass.

## Recipe and gates

The existing `M3Jdk21HashPinnedTextSnapshotRecipe` executes `m3-tq-sync`, producing
24 exact outputs. `plan.json` seals those outputs and six dependencies, including
the unchanged runtime TQ source, HotSpot argument guard and compiler checker.
No recipe engine is modified. The inner Java recipe now reproduces the current
TQ owner and enhanced tests; its original fixed-point and refusal gates remain.

The standalone harness stages exactly three product sources before compiling a
`java.base` patch, preventing accidental discovery of unrelated fork sources.
Both target workflows run the successor's Maven and custody checks before their
existing gates. Image qualification requires normal C1/C2 compiled TQ and
containment methods; M3-enabled compiler requests must instead prove interpreted
mode and zero nmethods. The image script uses the existing strict XML checker.

Run from this repository with Java 21 and Maven 3.9.9:

```sh
mvn -B -ntp -f m3/tooling/tq-sync/pom.xml verify
python3 m3/tooling/tq-sync/verify-plan.py
mvn -B -ntp -f m3/tooling/tq/pom.xml verify
mvn -B -ntp -f m3/tooling/tq/pom.xml -Dtq.sanitize=true verify
python3 m3/tooling/tq/verify-plan.py
mvn -B -ntp -f m3/tooling/lane28/pom.xml verify
python3 m3/tooling/lane28/verify-plan.py
python3 m3/migration/migration.py validate .
```

The successor passes exact output/fixed-point, missing/duplicate/drift and
unrelated-file tests. Its sealed installer additionally passes rollback and
three refusal cases without writes. Mapping checks preserve every unrelated
record and top-level field, with no admission/status promotion.

TQ passes 13 JUnit checks in release and nonrecovering UBSan configurations.
Each interpreter/mixed/C1/C2 execution performs 308,649 checks, including 201,601
ordered containment comparisons over 449 fact sets and 4,458 JNI oracle calls.
An independent `TreeSet` oracle covers empty/short values, duplicate trigrams,
NUL, non-Latin UTF-16, surrogate units and seeded random strings. Sealed readers
reject warm payload access. Tests explicitly preserve false-positive text
matches, discarded ordering/multiplicity and seam composition. Normal C1 and C2
logs must contain actual `containsAll` nmethods. An always-true containment mutant
is rejected alongside the original native-Unicode and seam mutants.

The clean Lane28 suite also passes all 20 checks, including the sparse-fill
baseline/candidate comparisons and compiler-evidence fixtures. Counts, hashes,
compiler excerpts and custody results are retained in `evidence/receipt.json`.

## Admission and feedback

The mapping remains **INCOMPLETE** and the TQ record remains
`implemented-unverified`. Local runtime tests use stock Java 21.0.8+9 with exact
target classes patched into `java.base`; they are not current fork-image or jtreg
proof. Hosted CI was queued at the predecessor head. Full fastdebug image,
image-local JNI, jtreg, full reactor, cross-platform/workload qualification and
broader regex integration remain separate gates. This packet
does not add a production JNI TQ accelerator or claim an end-to-end speedup.

Feedback to Synexia: preserve the distinction between necessary trigram-set
containment and an exact text/regex match; bind facts to an immutable payload
generation and range; require zero warm payload reads. Future source recipes
must rebase against both target owner history and its current backing names.
Target adaptations flow back as evidence and requirements; the JDK retains its
own namespace, implementation, licences, build and admission decisions.
