# TQ — Trigram Query

This crate ports Synexia's bounded trigram-fact kernel into
`jdk.internal.mindex.M3TQ` in `java.base`. TQ follows the short counterpart naming
rule already used by VI, CI, CB and PC. Public JDK classes retain their names.
The existing `MIndexStringBacking` owns the text; this kernel owns only derived
facts. Neither backing interface nor mapped implementation is changed.

OpenJDK 21 compatibility is the retained baseline. M3 additions may exceed that
baseline in capability, but must preserve existing Java SE APIs, bytecode/linkage
compatibility and required observable behavior. Extra internal counterparts do
not grant permission to narrow an existing contract. Compatibility and added
behavior need separate evidence before a runtime replacement is admitted.

## Runtime behavior

The explicit backing overload scans a half-open UTF-16 range without exporting
a view or materializing a String. The backing must stay open and immutable
during the scan. The returned facts retain sorted primitive trigram keys,
length and boundary units, with no retained text or backing reference.
The caller must associate facts with the exact owner, id, generation and range.
Concatenation merges facts and at most two boundary trigrams without reading
either payload. Length budgets are enforced before allocation.

Query AND/OR preserves empty and short alternatives. A negative guard can rule
out a necessary literal; a positive guard is never proof of a regex match.
This change does not derive guards from arbitrary Java regex syntax or insert
them into `Pattern`/`Matcher`. The JNI code is an independent test oracle, not
a production backend. No application, Maven or OpenRewrite dependency enters
the JDK image. No format, public API, loader or JVM identity change is made.

## Mapping and subsequent improvements

`m3/docs/name-mapping.json` remains the single mapping authority. Stable record
`synexia.counterpart.MIndexRegexTrigramQuery` records the source and target
symbols, immutable source commit, file hashes, target adaptations, recipe,
tests, license, dependencies and pending runtime consumers. Its state remains
`implemented-unverified` until the complete JDK admission gates are satisfied.
The previous 46 records and 20 gates are retained without changing their state.

For a later donor improvement, resolve its exact commit, compare it with
`pins.json` and the byte-exact originals in `donor/`, then change the recipe's
templates and hashes. Keep the JDK backing bridge and other target adaptations.
Update this same record's source/target lineage rather than minting a parallel
registry. Changes on both sides require reconciliation; a hash match is only a
candidate identity and cannot authorize a runtime optimization. Re-run the
recipe and semantic gates, generate a newly sealed plan, and publish an additive
PR. Exact preimages make concurrent target edits fail closed.

The existing inventory and family records retain the remaining work:
structural owners and all lean collections, String/slices/joins and word facts,
versioned Class/AST and compiler contexts, application/UI structures, native
consumers and the complete JDK route. The 2,494-path VI census is a review queue,
not a claim that every class has an admitted replacement. Use the existing
mapping tools to inspect progress and reconcile a newly captured inventory:

```sh
python3 m3/migration/migration.py validate .
python3 m3/migration/migration.py report .
python3 m3/migration/migration.py reconcile . --inventory <inventory.tsv> --inventory-receipt <receipt.json>
python3 m3/migration/migration.py complete .
```

`complete` deliberately fails while any required family or gate remains open.
Do not mark an uninspected source as replaced, delete an unobserved source, or
interpret a source-only recipe result as HotSpot integration.

## Reproduction and installation

Use Java 21, Maven 3.9.9 and Linux g++ with JNI headers. The mapping validator
also needs the existing `m3/migration/requirements.txt` installed in a tooling
Python environment:

```sh
mvn -B -ntp -f m3/tooling/tq/pom.xml verify
mvn -B -ntp -f m3/tooling/tq/pom.xml -Dtq.sanitize=true verify
python3 m3/migration/recipe.py check --root . --plan m3/tooling/tq/plan.json
```

The crate executes the existing Java and text snapshot recipes. The legacy
`jdk22-m3-tq` resource key is required by the existing Java recipe's key grammar;
it does **not** identify a JDK 22 donor or change the Java 21 target. The Java
recipe covers the product and jtreg source; the text recipe covers the native
oracle, mapping, naming, notice, image script and CI workflow. No recipe engine
or existing gate is weakened. The original bootstrap task recipe also runs with
an exact task-crate path/root and still grants no direct target-write authority.

The checked-in targets are exact postimages of these recipes. For a matching
base, the existing sealed installer can apply or roll back all eight outputs:

```sh
python3 m3/migration/recipe.py apply --root . --plan m3/tooling/tq/plan.json
python3 m3/migration/recipe.py rollback --root . --plan m3/tooling/tq/plan.json
```

Run those mutation commands only in an exclusively owned worktree. They reject
dependency drift, occupied targets, mixed states and foreign edits. No-op replay
must have zero writes. The installer is custody/replay tooling; it does not stand
in for executing OpenRewrite.

## Evidence and admission boundary

The 12-test local suite runs the actual recipes, strict Java/native compilation,
fixed-point and refusal checks, unchanged mapped-owner regressions, independent
UTF-16/range/concatenation oracles, and two deliberately broken implementations.
Every runtime mode uses `-Xcheck:jni`. Interpreter, mixed, C1 and C2 runs each
cover 410 strings, 4,040 ranges, 8,331 compositions and 174 real mapped ranges.
Warm fact evaluation/composition performs zero payload reads. C1/C2 acceptance
requires a compiled `M3TQ` kernel in the compilation log. Both release and
nonrecovering UBSan native builds are required. See `evidence/receipt.json` for
the exact executed boundary, counters, tools, source hashes and remaining work.

Local runtime evidence patches the three internal classes into a stock Java 21
`java.base`; it is not a rebuilt M3JDK image pass. `.github/workflows/m3-tq.yml`
separately builds the complete fastdebug image, then runs `verify-image.sh`
without patch-module in seven modes, including M3-enabled C1/C2 and noncompact
strings. That gate must run against the PR tree; a queued or absent job is not a
pass. The image script also requires JNI checks and compiled kernel evidence.

This bounded crate does not waive the existing full migration-recipes reactor,
99% coverage gate, complete jtreg/TCK, GC/CDS/JVMTI/JFR, platform, memory and
performance gates. Automatic regex guard extraction and consumption remain
open; no speedup or complete MIndex migration is claimed.

## Provenance

The copied kernel and independent JNI oracle are Apache-2.0 Synexia code at
`cf3796cfca75284e9626b1e31703e41314c9f852`. Byte-exact source, LICENSE and NOTICE
are under `donor/`; `pins.json` records Git blob and SHA-256 identities. Port
changes are limited to package/name, public factories inside the unexported
internal package, and the explicit canonical-backing reader. The native oracle
changes its JNI include and generated test symbol prefix. The distribution
notice scopes these files; existing OpenJDK and other donor licenses remain in
force. No challenge-site solution or newly found third-party code was copied.
