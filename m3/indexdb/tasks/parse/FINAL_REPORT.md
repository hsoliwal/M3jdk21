# PR88: DB source recovery and bounded snapshot parsing

## Files changed and exact delta

This candidate restores the existing `m3/indexdb` module from retained commit
`46112be5b68d8d3816b9d3100fc31d8b2e9de736` onto master
`ef39c669ee9ac77bee073f5b95479ec1613cdf3a`. The latter preserved merged-PR ancestry
but did not contain the module. Restoration includes its original Maven coordinates,
eight production sources, eight retained test classes, old task documents and the
single reactor entry. Existing JDK product code and canonical branches are untouched.

Only the package-private wire codec changes in production. A long-arithmetic frame
budget rejects declared rows that cannot fit in the supplied bytes before allocating
count-sized collections. Standard JDK UTF-8 decoding with REPORT rejects corrupt text
instead of replacing it. Valid V1 wire format and public/protected signatures are
preserved. Malformed-input behavior is deliberately tightened, not called equivalent.

New names are short: `DbProbe`, `DbParseTest`, `DbRecipeTest`; `budget` and `text`
are private operation atoms. No new function wrapper, SQL engine, Java source parser,
public naming hierarchy or dependency is introduced.

One retained assertion is corrected after actual CI evidence: parent queries return
child-first recomposed nodes, as documented by the existing graph owner. The test
previously expected the original leaf fingerprint. Its new expected value is independently
composed; both parent queries and the canonical parent are asserted. Every other
existing test is retained, and the graph implementation is unchanged.

## Reusable recipe ownership

The existing Java-LST and PlainText snapshot engines remain unchanged:
- `DbJava`: 18 final Java afterimages, executed relative to `m3/indexdb`.
- `DbParse`: exact retained-codec repair plus two new test/probe sources.
- `DbCheck`: exact single-file correction of the stale parent expectation.
- `DbMeta`: five repository-relative POM/document/workflow afterimages.

Final-state recovery avoids a restore-old/repair-again loop. Six actual
`DbRecipeTest` JUnit cases cover generation, type, scope refusal, exact materialization,
partial composition and fixed point. Their source exists; engine execution is not
claimed when the runner does not execute.

## Exact verification executed

The sealed source diff and configuration/hash checks precede strict compilation.
All eight production owners compile under Java21 with `-Xlint:all -Werror`.
Their public/protected javap descriptors match the original byte-for-byte.

In three separate JVMs capped at32MB, the original decoder exhausts heap on a24-byte
header declaring50,000,000 rows. A fourth original probe accepts malformed UTF8.
The identical four probes pass against the repaired implementation. The independent
parent-composition probe passes both original and candidate production classes.

Hosted run37183150332 executed the first candidate
`fbfd2141a0f1a9530b1fca35d52a4e5d0ceb9c3b`:51 JUnit tests,1 failure,0 errors,0 skipped.
All nine new parser tests passed. Its one failing retained parent expectation was
then corrected through a separate exact recipe. The original failing artifact is
preserved as11296460013, SHA-256
`dfc59e7fd9ae16fb861e3a7a9f6e162b8493824785e57f4003d9ef0c92a76c1f`.

The production/test/recipe source state
`49a8df37ab6a71581db7b445464c509b36606725` was verified against local source and
template identities. Source tree:
`8a1be2b7ba3aed18fda9aab93c31d22dc79e393f`.

## Exact blockers and failures

For49a8df37, PR run37183817347 and push run37183815512 failed before any job object.
A retry request returned403, "This workflow run cannot be retried". No permission
change or retry bypass is attempted. Therefore the corrected full JUnit suite and
the six OpenRewrite tests remain unexecuted on this source state.

The first run stopped at JUnit, so current-candidate99% coverage is unmeasured.
The original line and branch0.99 gates are unchanged. Neither standalone probes,
Git patch replay, syntax checks, test counts nor historic coverage establish that
this gate passed.

No modified JDK build, jtreg, custom JNI parity, real JavaFX launch, full transactional
database, persistent Java AST integration or complete compatible-backport programme
was verified in this packet. ACTION_QUEUE.tsv preserves their existing owners.

## Reproduction and artifact paths

The exact-head workflow supplies the authoritative future gates:
```
mvn -B -ntp -f m3/indexdb/pom.xml clean verify
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml -Dtest=DbRecipeTest test
```
The second command is focused recipe JUnit, not whole-reactor coverage acceptance.

This directory contains the task, status, provenance and verify/output contracts.
EVIDENCE.tsv separates observed runs and source identities. The delivery bundle adds
raw local command receipts, both immutable CI evidence ZIPs, source snapshots and a
Git-native patch; direct patch replay is never described as OpenRewrite execution.

No canonical merge, rebase, squash, force-push, removed quality gate, global provider
change or renamed public API is performed.
