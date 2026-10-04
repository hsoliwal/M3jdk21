# DB recovery and parser repair

The canonical tree preserved PR61/79 ancestry without the DB module files.
This candidate restores the retained owner, original module coordinates,
its reactor entry and existing tests. It changes only the private V1 wire
decoder in production: frame-size minima are checked before allocation and
malformed UTF8 is rejected via the standard JDK CharsetDecoder.

Strict local Java21 compilation succeeded. Three bounded count probes and
one malformed-text probe fail on the original and pass on the candidate.
Hosted run37183150332 then executed51 JUnit tests. All9 new parser tests passed.
One retained test expected the original parent fingerprint, contradicting
the existing documented child-first graph composition. The correction retains
the test and supplies an independently composed expected parent, checks both
parent queries and the canonical parent, and asserts changed fingerprint.

Four recipe crates reuse the existing engines:
- DbJava: all18 final module Java afterimages from an absent module.
- DbParse: three-target private parser repair and probes from retained source.
- DbCheck: exact single-file test-oracle correction.
- DbMeta: five repository-relative POM/document/workflow afterimages.

DbJava is final-state materialization, not a restore-old-then-overwrite loop:
replaying it or the individual repair crates on the final module must do nothing.
Java paths are relative to m3/indexdb; metadata paths are repository-relative.
The generic source engines, database public API and 99percent gates are unchanged.

The first CI failure is preserved as evidence. The subsequent candidate's real
JUnit/coverage and OpenRewrite results are not inferred here; see the PR's exact-
head runs and downloaded proof artifacts. Passing tests do not override coverage
failure. This is not a completed SQL database, new Java parser, native speedup,
modified-JDK release or completion of every discussed JDK task.

No rebase, force-push, squash, canonical merge, new function abstraction,
global provider removal, ignored quality gate or renamed public API is performed.
