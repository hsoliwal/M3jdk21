# M3 parser JMOD qualification

## Result

The optional `jdk.m3.parser` module passed the ordered source lint, compilation,
contract tests and JMOD/linked-image runtime sequence on 2026-10-18. The final
source is the exact output of the canonical OpenRewrite recipe. The local
Maven profile invocation selected `:m3-parser-jmod-pack` explicitly and exited
zero. This report describes that finite qualification; it does not establish
universal equivalence to the full Synexia parser estate.

| Gate | Executed result | Evidence |
| --- | --- | --- |
| Canonical recipe structural lint | PASS, 36 Java and 13 text targets | Canonical recipe proof 5 |
| Canonical recipe compilation | PASS, Java 21 with `-Xlint:all -Werror` | Canonical recipe proof 5 |
| Actual OpenRewrite SDK tests | 10 tests, zero failures/errors/skips | Canonical recipe proof 5 |
| Actual SDK receiving and fixed point | 49 admitted outputs; every output compared byte for byte with its next-pass result | `APPLICATION_MANIFEST.tsv` |
| Target source/toolchain seal | 133 exact inputs; source drift and phase-order checks enabled | `target-proof-2/INPUTS.json` |
| Existing family receiver gate | PASS; family state and receiving order remain unchanged | `target-proof-2/lint-full-borrow-receiver.log` |
| Target compilation | Four unchanged compiler owners, 32 production Java sources and four test sources compiled before tests | `target-proof-2/compile-commands.json` |
| Grammar/source contracts | 297 checks pass on host and linked image | `target-proof-2/test-M3ParserFrameworkContract.log`, `target-proof-2/runtime-image-M3ParserFrameworkContract.log` |
| Existing M3AST contracts | 116 checks pass on host and linked image | `target-proof-2/test-M3JavacParserContract.log`, `target-proof-2/runtime-image-M3JavacParserContract.log` |
| JAR/JMOD packaging | Named module, expected classes and exact Apache legal entries present | `target-proof-2/runtime-jar-descriptor.log`, `target-proof-2/runtime-jmod-contents.log` |
| Linked-image execution | Both contract suites and two independent smoke runs pass | `target-proof-2/runtime-commands.json` |
| Deterministic consumer replay | The two image smoke outputs are byte-identical; host smoke matches them | `target-proof-2/RUNTIME_PROOF.json`, `INDEPENDENT_AUDIT.json` |

Check counts count assertions in the executable contract suites, not separate
JUnit test methods. The canonical recipe uses 10 actual JUnit methods. No claim
of measured speedup, universal grammar equivalence or exhaustive Java coverage
is made.

## Exact qualified inputs and artifacts

Target baseline: `hsoliwal/M3jdk21@f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`.
Donor baseline: `hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267`.
Canonical recipe: `com.synexia.rewrite.M3ParserJmod20261018`.

Published canonical source: [draft PR 12288](https://github.com/hsoliwal/com.synexia/pull/12288),
commit `08f1ff008f14263da0f5ab4d183746e793ed22ea`.
The [canonical qualification archive](https://github.com/hsoliwal/com.synexia/blob/08f1ff008f14263da0f5ab4d183746e793ed22ea/synexia-openrewrite-recipes/recipes/m3-parser-jmod-20261018/QUALIFICATION.md)
contains the actual recipe execution history and exact application/fixed-point
manifests. `CANONICAL_RECIPE.json` records the publication identity separately
from the tested donor baseline and recipe source seal.

| Identity | SHA-256 |
| --- | --- |
| Final canonical recipe source inventory | `5c395997c11c66bb718c08b922e0f0eb3945861421230999af74c217bcc8cee7` |
| Target source/toolchain input inventory | `e279db46a00e6a15a0d44275a40b32c4a1ed1cff9db13658ee971d7251f30ce5` |
| Modular JAR, 80,476 bytes | `22f2ccf032add6fa4f1124fdd1eb8d67758d18efdcebfabcbd59fc75b5266dc6` |
| JMOD, 85,406 bytes | `c777266f0cbe71ec9828f1565ecf06320b194bb12eb830e5c5f22ac17abc96cd` |
| Linked image `lib/modules` | `28573f29ca92bf542b48ae3cad5f70dc009b03c090b8d899cb2e1d7f37a61a0b` |
| Replayed consumer stdout | `df8c5ff7e53750d37d5de36351d0fd6b6ec81d0e48eb67841cfae8db5e9fda61` |

The toolchain was the pinned downloaded Temurin 21.0.12.1+1 development JDK and
Maven 3.9.11. Archive/tool configuration hashes are in `session/TOOLCHAIN.json`.
The recipe dependency classpath is separately recorded in its compile evidence.
The parser module itself has no Maven/Synexia runtime dependency.

The ordinary compiled class bytes match the packaged class bytes. The JAR tool
adds the standard `ModulePackages` metadata to `module-info.class`; requires
and exports remain unchanged. JAR and JMOD class inventories and bytes match.
Deterministic replay here compares consumer results and admitted source bytes;
it does not assert that independently recreated JMOD archives are byte-identical.

## What the contracts exercise

Grammar/source checks cover left-recursive expression precedence, valid and
invalid syntax, expected-token diagnostics, late nullable waiters, empty and
cyclic-nullable grammars, literal/fingerprint collisions, case policy, tolerant
acceptance versus conformance, token ranges, comments, all 65,536 UTF-16 code
units in the source-seal oracle, immutable String identity, mutable-input
freezing, strict bounded file decoding, raw-byte identity, retained snapshots
after file edits, all six parse limits, and concurrent independent sessions.

M3AST checks compare detached preorder kinds, parents and UTF-16 source ranges
against an independent javac `TreeScanner`. They exercise shared projection
identity, distinct projection identities, a single parser source read, syntax
retention, CRLF/supplementary source text, unavailable positions, access after
closure, refusal of an expired or cross-thread live Unit, wrong event phases,
and exact `maxNodes=N` versus `N-1` boundaries without publishing partial trees.

## Compiler and resource scope

The proof compiles these four unchanged M3JDK source owners as an external
`jdk.compiler` patch:

- `com/sun/source/util/M3AST.java`
- `com/sun/tools/javac/m3/M3ASTPC.java`
- `com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java`
- `com/sun/tools/javac/main/JavaCompiler.java`

Every host and linked-image consumer is launched with an explicit
`--patch-module jdk.compiler=<proof>/compiler-patch`. The linked image does not
embed that patch. The compiler descriptor is the supplied development JDK's
descriptor. Direct M3AST invocation was exercised; `-Xplugin:M3AST` service
discovery was not qualified.

Ordinary String inputs use the existing public String API. This run did not
execute a full custom M3JDK/HotSpot image or measure its M3String storage. The
module introduces no new M3String owner or `java.base` export.

Generic parsing has explicit per-call limits. Grammar construction, analysis
and first decision-index construction are configuration work outside those
limits. M3File construction and the caller's javac task also have their own
ownership boundaries; the javac adapter limits its detached projection. The
received lexer preserves broad donor numeric rules rather than claiming strict
JSON or Java lexical conformance.

Still pending: full M3JDK/HotSpot build and jtreg, compiler plugin discovery in
the rebuilt descriptor, broader original-donor differential qualification,
other operating systems/toolchains and performance measurement. No runtime
family promotion is asserted by this independent Apache module.

## Stopped attempts and corrections

All dependent work stopped at each observed failure. Each source correction
started a new lint/compile/test/runtime pass; prior logs remain retained.

| Attempt | Observed result and correction |
| --- | --- |
| Initial inventory gate | Shallow Git history could not establish the recorded ancestor. Full ancestry was fetched and the unchanged gate passed. |
| Recipe proof 1 | Dependency resolution failed before compilation because Maven did not use the environment proxy. A task-scoped Maven proxy configuration was added. |
| Recipe proof 2 | Dependency resolution failed before compilation because the downloaded JDK lacked the environment CA chain. The existing system Java truststore was selected; TLS validation remained enabled. |
| Recipe proof 3 | Compilation passed; all 10 test methods errored during OpenRewrite logger initialization. Explicit pinned SLF4J API/simple runtime dependencies were added to the standalone recipe POM. |
| Recipe proof 4 | Full recipe sequence passed and its exact outputs were initially received. |
| Aggregate Maven attempt 1 | Existing module-pack-kernel could not resolve its source-built JUnit artifact, before any parser phase. The documented command was corrected to select the parser module explicitly. |
| Recipe proof 5 | The corrected documentation was resealed; all 10 SDK tests and actual receiving/fixed point passed. The sole target amendment was the README command. |
| Selected Maven attempt 2 | All parser phases passed. Maven logged a nonfatal requested-profile warning during selected-project handling; the parser project was selected, built and independently audited successfully. |

The exact Maven arguments, exit values and retained logs are under `session/`.
`SOURCE_POSTIMAGES.tsv` verifies the final target content against all 49 admitted
recipe outputs. `EVIDENCE_COPY_RECEIPT.json` records the original proof paths and
hashes for the retained evidence files. Authoring receipts that say verification
was pending describe their original authoring stage; this report and its phase
receipts describe the executed result.
