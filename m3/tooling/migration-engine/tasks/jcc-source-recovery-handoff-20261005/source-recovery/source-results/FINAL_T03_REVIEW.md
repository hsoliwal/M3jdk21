# Actual T03 materialization and disk replay review

**The complete mechanical replay passes its defined scope. Overall qualification remains BLOCKED by the separately executed selected-upstream error.** This review admits the accuracy of the completed T03 execution evidence. It does not convert prior failed gates to successful gates, count prior behavioral suites as newly executed, or qualify a full reactor, JDK, export or receiving M3 pipeline.

The exact run is `recovery-final-replay-be92/synexia-openrewrite-recipes/recipes/atom-pattern-mastery-20261005/jcc/recovery-be92/target/replay/run.Jb2fAJEW`. Its integrated `RESULT.json` remains SHA-256 `066c4fa046adf063f2bffb02ac8d6be949e5f85d55de489dd081d56bddcd93dd`.

## Actual execution and exact inputs

All 70 declared stage names in the frozen driver equal its 70 ordered run declarations. The actual status file contains exactly one RUNNING/PASS pair for each, followed by exit 0. There are no failed, omitted, duplicated or unrun stages in this particular T03 driver. All 70 command records and logs were read. This successful mechanical-driver status is distinct from the retained overall qualification status.

| Identity | Exact value |
|---|---|
| Current source acquisition commit | `be92c62ece9023b5c33676716a1076d00e26120a` |
| Current source acquisition root | `3c4f32b66633a251ba2c117090830252a7e2da03` |
| Isolated local seed commit | `917332f32296827ca79371ac1315840c21d01b3a` |
| Isolated local seed tree | `d510293f0d0e464c7cce98f1d788825bd53c69cd` |
| Seed receipt SHA-256 | `d33cf9575e511976bc042f5e9c91f3c3d9154a169567d462b4db9d4c027d9204` |
| Installed driver SHA-256 | `a46e4877d5fd07afa65283652cc1b7407584291425a88740ba8d5dd3411e9441` |
| Installed applicability manifest SHA-256 | `b83efc544a4479e99637dbbf71034f768b33e149d17ef67d9b810a3849c38d24` |

The isolated seed has no parent and no remote ancestry. Its source acquisition commit/root are independently pinned provenance for the selected projection; the local commit is not presented as a whole merged repository checkout. The 1,924-file initial assembly, 14 installed proposal files and one adoption receipt account for the 1,939-file immutable seed. The original assembly, seed and adoption receipts retain their truthful pre-execution labels. The launch record names the exact installed driver and `-Djcc.apply=true`. All 14 installed proposal targets still match their frozen source bytes. The installed applicability manifest is the final manifest, not the preserved older draft.

## Six actual writes, followed by five independent disk replays

Each row below corresponds to two separate actual JUnit invocations, with distinct initial/replay build directories and copied XML reports. Each invokes the same complete `JccApplyTest.exactRecipeResultsAndRefusalsPrecedeAnyRequestedWrite` method. All ten report one test, zero failures, zero errors and zero skips under Java 21.0.2.

| Canonical recipe crate | Initial Results | Initial files written | Later disk Results | Later disk writes |
|---|---:|---:|---:|---:|
| `jcc-be92-parser` | 1 | 1 | 0 | 0 |
| `jcc-be92-contract` | 1 | 1 | 0 | 0 |
| `jcc-be92-convergence` | 2 | 2 | 0 | 0 |
| `jcc-be92-observation-imports` | 1 | 1 | 0 | 0 |
| `jcc-be92-upstream-imports` | 1 | 1 | 0 | 0 |
| **Total** | **6** | **6** | **0** | **0** |

Receipt Results, receipt write rows, independently read initial/final bytes, JSON summaries and actual copied/original XML bytes agree. Initial application is performed on the six original preimages. Each later replay reads the already-written files from the isolated repository and actually invokes the recipe again; it does not bypass the recipe because an input already matches a postimage. All ten invocations also perform an embedded second application and record zero Results.

The frozen test validates exact source paths, complete Result cardinality, printed output, a reparse and embedded fixed point before publishing requested writes. It exercises source drift and required-source absence in each invocation, plus malformed parser context in both convergence invocations. The captured receipt contains 22 expected negative-control callbacks: ten drift callbacks, ten missing-source callbacks and two invalid-context callbacks. These are intentional refusal observations; they are **not** JUnit errors. First-application and embedded-second-application error counts are zero, and the suite XML failure/error/skip counts are zero throughout. Refusal and unchanged-disk/classpath checks precede the actual writer.

The two convergence invocations use the exact ordered 24-artifact parser manifest. The other eight invocations use an explicit empty parser classpath. All ten XML runner classpaths exclude the temporary provider. This preserves the distinction between compiler symbol context and executable recipe-runner dependencies.

## Original-byte comparison and scope preservation

The reviewer read every unique immutable seed blob body using only read-only Git object commands. The 1,433 unique Git blobs account for all 1,939 seed paths. Raw Git blob encoding, size and SHA-256 were validated; the commit object identity and complete recursive path/mode/blob inventory match the seed. Current files were then compared directly with those original bytes, not merely with corresponding digest strings.

Exactly 1,933 seed file bodies are unchanged. Exactly six bodies changed, each byte-identical to its reviewed recipe template. The complete non-build-output path set and file modes are unchanged. Generated `target` output and Git storage are outside that source/payload path denominator.

The six changed files are four production owners and two copied test fixtures:

| Role | File |
|---|---|
| Production parser context | `M3HashPinnedJavaSnapshotRecipe.java` |
| Production bounded contract framing | `M3MasteryContractSurface.java` |
| Production challenge review helper | `CompetitiveProblemReview.java` |
| Production donor ledger helper | `ChallengeDonorPassLedger.java` |
| Copied observation fixture, imports only | `M3MasteryObservationGateCampaign.java` |
| Copied upstream fixture, imports only | `M3DocumentationAttributionExecutionTest.java` |

`SOURCE_BYTE_COMPARISON.json` contains every full repository path, before/after byte length and SHA-256, immutable Git blob identity, mode and direct equality result. For all six targets, initial snapshot bytes equal original seed blob bodies and disk-replay snapshot bytes equal the exact final template bodies.

All 527 current-context original bodies also match the separately preserved current baseline and acquired origin bodies. Exactly 523 remain unchanged and four are the admitted production postimages. The two copied fixtures are outside the 527-context denominator. Preserved raw parser resources, including invalid UTF-8 and NUL fixtures, were compared as bytes within the unchanged payload; they were not normalized or decoded for equality.

## Fresh compilation, resources and applicability

The companion independent review, sealed under `current-owner-upstream-review/t03-provider-execution-review`, verifies actual provider stages 11–14, fresh main stages 22–23 and applicability stages 06a, 24 and 33. Its complete report and artifact seal are bound by `COMPANION_BINDING.json`.

The fresh provider contains 527 context inputs: 525 exact copied source inputs and two reviewed template inputs. Both actual provider and main compilations consume exactly 446 designated first-party sources with release 21, no annotation processing, all lint warnings and warnings-as-errors. All 1,446 provider classes, fresh main classes and retained fourth-owner reference classes are byte-identical with exact path sets. All 34 fresh main resources match the retained reference by path and bytes.

The provider JAR has exactly 1,473 entries: 1,446 classes, 26 copied resources and a manifest. Its entire deterministic ZIP encoding was independently reconstructed in memory and compared byte for byte. Its SHA-256 is `e36b093b83bb99d292ef2e614963ae04c9ac7e250bf6cc92e51cb9c8425a9c83`. Its parser manifest contains that JAR followed by the same 23 ordered compiler dependency JARs. The provider's 26-resource symbol context is distinct from the 34-resource production output.

Each executed applicability stage checks the same 2,325 frozen file identities and reports PASS for the applicability check while retaining BLOCKED qualification. The preimage stage makes no fresh-class equality claim. The materialized and replayed stages report exact fresh output equality, independently confirmed in the companion review. The final proof checksum list has 412 rows representing 380 unique paths, all rechecked. The cache checksum list has 1,198 rows; the exact cache membership remains 432 JARs and 766 POMs. Both final checksum stages complete successfully with quiet logs.

## Prior outcomes retained without reruns

| Gate or campaign | Actual prior result | T03 behavioral execution |
|---|---|---|
| Dedicated parser T02 candidate | 43 tests pass | NOT_RERUN |
| Repaired growth/Mastery/core/parent | 119 tests pass: 1 + 15 + 91 + 12 | NOT_RERUN |
| Current observation suite | 8 tests pass | NOT_RERUN |
| Standalone observation campaign | 19 cases; fresh root `ef73c54a20835369ae541b95bdb7b56ba42c3a4366607b00157a09f7572cc2e7` | NOT_RERUN |
| Complete selected upstream | 67 tests executed: 66 pass, 1 error, no failures/skips | NOT_RERUN |
| Independent native lane | 19 tests pass: 7 donor + 12 parent | NOT_RERUN |

The upstream error remains the absent `org.openrewrite.staticanalysis.AtomicPrimitiveEqualsUsesGet` class, observed in the complete selected suite. It remains a real executed failure, not an unrun suite. The original erroneous generic unrun classification is preserved historically and is corrected by a separately sealed classification receipt. Prior failed heap/lint attempts likewise retain their original outcomes.

The ten fresh T03 materializer tests do not increase or replace these earlier behavioral counts. The separate native lane's actual strict C build, required JNI mode, 19 passing tests and parity evidence retain their own execution epoch and reviewed limits. The fresh T03 replay is not a new JNI execution. The API comparisons and source-specific regression oracles retain their own prior execution and applicability scope.

The accepted offline dependency bracketing is observational evidence of equal artifacts across the admitted interval, with its trusted-installation limitation. It does not establish historical JVM-loaded-byte or operating-system-wide attestation. All 34 resource sources are directly pinned for T03; the eight that lack earlier direct source pre/post freezes retain their historical/scaffold/source/output observational qualification. New T03 pins do not backdate that evidence.

## Independent review seal and disposition

The principal read-only inspector checked 4,417 unique read inputs and rechecked every one unchanged before writing its report. `INPUT_SEALS.json` binds those inputs; `SEED_GIT_READBACK.json` binds the immutable Git object checks. The companion review independently binds 12,729 stable read inputs. These populations overlap and are not additive counts of distinct project source files.

The reviewer executed only a file/JSON/XML inspector and the recorded read-only Git `cat-file`/`ls-tree` operations. No owner program, applicability checker, build, test, recipe, Java command or native command was executed by the reviewer. No source, POM, fixture, owner report, branch or earlier sealed artifact was changed. Function-style command records, logs, receipts, XML and endpoint byte identity are the available execution evidence; no full process-attestation or universal semantic-equivalence claim is made.

**The completed T03 mechanical outcome is reviewed and confirmed.** A `qualification_review_complete` flag may truthfully mean that this outcome and its limitations have been reviewed. It must not mean qualification passed: upstream remains FAIL, overall qualification remains BLOCKED, and `global_reactor_accepted`, `export_accepted` and `M3_accepted` remain false. Final publication payload and remote custody are separate later review steps.
