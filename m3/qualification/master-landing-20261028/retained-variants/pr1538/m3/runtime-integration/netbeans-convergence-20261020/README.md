# M3JDK21 catalogue-driven owner convergence

M3JDK21 is the integrated product. Synexia remains the canonical convergence and
recipe workspace. The changes refine existing arrays, physical indexes, offsets,
column planes and state masks, with unchanged public/protected Java and JNI
surfaces. They retain the existing compiler, original build-model providers,
collection wrappers and filesystem ownership.

## Reviewed behavioral atoms

| Existing owner | Refinement | Contract and evidence |
| --- | --- | --- |
| Filtered lists/views | Validate the original backing cursor before index writes; preserve strict read-only endpoint refusals | Live predicate order, backing optional writes, null/range/comparator policy and stored representatives remain covered. Documented cursor/refusal defects are corrected. |
| Long index projections | Reverse equal-value groups for an admitted opposite order | O(n) physical-index work, equal occurrence order retained; general stable merge sort remains available. |
| SortedSet | Preserve the owner's lazy comparator in recursive split children | Existing sizes, traversal characteristics and public API; frozen split fixture fails on the old source and passes on the generated source. |
| Existing JNI compactor | Gather only the enclosing span of admitted rows per chunk | Fixed 256-byte ordinal scratch; stable output, original count drift/exception rules and unchanged native entry points. |
| Compiler/parser | Scoped identity arrays, reusable preorder traversal, enum-index lookup and bounded UTF-16 source hashing | Original javac trees, null/failure/recursion rules, exact source identity, lifecycle and source geometry remain authoritative. |
| Lexer policy | Pack nineteen private flags into one mask | Getter/setter overwrite, role refusals, fingerprints and token streams verified over every policy vector. |
| Authoritative build models | Enforce existing matching-child, depth and pending-array budgets before retention | Original provider objects and order remain; this does not bound arbitrary provider callback or iteration work. |
| Filesystem directory pages | Compare and hash existing primitive/name columns directly | Exact list/record identity including all UTF-16 units; public entry projections and table structural hash retain their own contracts. |

The FILE_TRANSFORMS.tsv ledger records all 45 ordered source/qualification rows
and their exact pre/post hashes, which compose to 42 final paths. Catalogue and
handoff additions are separately sealed in the canonical catalogue pass.
DONOR_DECISIONS.tsv records the fourteen NetBeans categories, including held
candidates. SOURCE.json records commit/blob provenance and companion sources.
QUALIFICATION.json separates executed module proofs from pending global gates.

## Reuse the catalogue in each subsequent pass

The existing SYNEXIA-CATALOGUE-FILE-REFINEMENT-1 invariant in
m3/docs/m3jdk21-porting-invariant.md remains the authority. Search by problem
category in m3/tooling/shared-frontend/algorithm-catalogue.tsv and the collection
catalogues before modifying an owner. For example, from the repository root:

    rg 'arrays\.mask\.compaction|arrays\.order\.stable' m3/tooling/shared-frontend/algorithm-catalogue.tsv m3/collections/algorithm-catalog
    rg 'grammar\.state|state\.bitmasks|trees\.traversal\.budget' m3/tooling/shared-frontend/algorithm-catalogue.tsv
    rg 'm3-netbeans-' m3/tooling/recipe-catalogue.tsv

Read each complete receiving file and its contracts. Compare existing Synexia/M3
owners with pinned GitHub donors and relevant LeetCode, HackerRank and
GeeksforGeeks problem categories. Record correctness, compatibility, complexity,
measured resource evidence, rights and the reason for selection or refusal.
Keep earlier decisions as history; append the new owner-specific disposition.
Refine meaningful behavioral atoms independently when that improves reuse or
verification. There is no arbitrary decomposition limit.

Freeze baseline counterexamples and valid-contract oracles before changing the
recipe payload. Apply exact per-file preimages through the real SDK, then run
lint, strict compilation, independent tests, runtime and final readback. Preserve
failed logs and require zero actual SDK Results on a repeated final pass. Repair
the recipe and regenerate when a gate exposes a defect; do not hand-edit generated
production output. Source-sealed installation proves file custody; it does not
replace type checking, independent behavior tests or owning Java/JNI gates.

## Receive the reviewed final recipe

Use the canonical commit pinned in the receiving PR description. Its crate is
synexia-openrewrite-recipes/recipes/netbeans-convergence-20261020 in
hsoliwal/com.synexia. From that crate, using Java 21 and Maven:

    mvn -B -ntp test
    mvn -B -ntp dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
    java -cp "target/classes:$(cat target/classpath.txt)" com.synexia.rewrite.M3NetBeansReplay convergence /absolute/path/to/M3jdk21 apply
    java -cp "target/classes:$(cat target/classpath.txt)" com.synexia.rewrite.M3NetBeansReplay convergence /absolute/path/to/M3jdk21 check

This is the existing bounded OpenRewrite 8.17.1 SDK lane, not a claim that the full
source-qualified Synexia reactor was run. The final manifest selects the first
before and last after image for every path and validates all intermediate hash
joins. Its separate SDK composition test compares the ordered leaves with this
collapsed result. Earlier leaves remain reproducible at their own admitted
states; a predecessor correctly refuses a later third image. Unrelated files are
preserved. Symbolic-link components, drift, missing owners and duplicate owners
refuse. The serial receiver rechecks inputs but is not an atomic transaction
across all filesystem writes.

## Scope and remaining gates

Qualification details and exact canonical evidence paths are in
QUALIFICATION.json. Focused Java/JNI, provider, API, source-patch and linked-image
proofs are distinct from a full source-built M3JDK21 image. The actual strict
source build configured successfully and entered compilation, then its disk
reserve guard stopped it before an image existed. The PRs remain draft for the
completed full image, applicable jtreg/platform suites and required hosted checks.
The global receiving phase remains String; this work makes no phase promotion.

The catalogue explicitly holds restartable lexer edits without an existing edit
session contract, embedded-source mapping without a receiving API, flyweight
lifetimes without a consumer, and host WatchService invalidation for logical
SQLite filesystem transactions. These are evaluated contract mismatches, not
implemented features. GapContent's already-received NetBeans lower-bound atom
and existing Maven/Gradle provider ingestion are retained.

No NetBeans or challenge implementation body is copied by these changes. Exact
already-public target successors provide the reused Synexia implementations;
existing per-file Apache-2.0, JDK GPL/ClassPath and third-party notices remain.
