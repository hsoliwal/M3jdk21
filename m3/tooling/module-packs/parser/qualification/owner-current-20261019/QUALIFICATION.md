# Parser owner qualification

## Decision and exact source scope

Independent production review approves the four bounded owner refinements. No concrete public API, exception, ordering, ownership or arithmetic defect was found. This review inspected source diffs and existing evidence; it changed no production target and reran no tests.

The current receiving pass has 11 sealed targets emitted by `com.synexia.m3.ParserOwnerCurrent20261019` at `/workspace/scratch/7ea994fb52d9/proofs/sdk-parser-current-01/sdk-output`. Its actual Maven/OpenRewrite qualification passed 14 methods with zero failures, errors or skips. `candidates/parser/SOURCE_SEALS.json` records every before/after hash. `REVIEW.json` records the four production sources and evidence custody.

| Production owner | Change and reviewed invariants |
| --- | --- |
| `src/jdk.compiler/share/classes/com/sun/tools/javac/m3/M3ASTPC.java` | Event-local paired key/value slots and a byte state lane replace IdentityHashMap sentinels/failure wrappers. Projection identity, successful null/Throwable values, exact cached failures, recursion refusal and computation/hit accounting remain distinct. Nested factories re-probe after possible growth. Callback exit drops session/tree/type/source/cache references. |
| `m3/tooling/module-packs/parser/src/main/java/jdk/m3/parser/M3JavacParser.java` | One reusable direct-child scanner appends into the existing array stack. It reverses only the appended segment; its parent rows are all equal, so preorder and parent identity remain intact. Popped Tree references are cleared. Processed plus pending nodes retain the same maxNodes bound. |
| `m3/tooling/module-packs/parser/src/main/java/jdk/m3/parser/M3JavacTree.java` | A private final enum lookup array replaces repeated values-array copies. The array never escapes; returned enum constants and existing node validation are unchanged. Detached primitive lanes retain no live javac Tree or Context. |
| `m3/tooling/module-packs/parser/src/main/java/jdk/m3/parser/M3File.java` | At most 8,192 temporary bytes batch the same big-endian UTF-16 code units into SHA-256. Empty input, exact block boundaries, partial final blocks, NULs and isolated surrogates preserve the byte sequence. The immutable String remains the source payload. |

The projection table retains a vacant key slot at maximum capacity. The supported paired-array length cannot exceed 2^30 and the entry count remains below 2^29, so the pre-increment and long load-factor calculation do not overflow. Parser growth uses the existing long intermediate; its direct-child bound is algebraically equivalent to the former separate child-list bound. No public signature changed in these four diffs.

## Current ordered parser-pack gate

`/tmp/m3-parser-refinement-20261019/current-ordered` records **lint → compile → test → runtime**, all PASS, with one sealed input digest:

`a7258ac8a63127666fe904bb016eeb3abe8c6ddae2f2163b78e476b929cbca64`

Each phase receipt names the previous receipt hash. `STATE.json` has all four completed phases and no active or failed phase. The current build includes 34 parser main sources and the newly upstream M3ASTIndex/M3ASTIngress owners and descriptor.

| Existing contract | Direct module execution | Linked parser-image execution |
| --- | ---: | ---: |
| Framework/grammar/source contract | 316 checks | 316 checks |
| Original-javac preorder/range contract | 2,559 checks | 2,559 checks |
| Projection state/cache contract | 41,959 checks | 41,959 checks |
| Cross-frontend ingress/index contract | 12,071 checks | 12,071 checks |
| Consumer image smoke | PASS | Two executions with identical grammar and javac digests |

The phase commands, logs and artifact hashes are in `lint-receipt.json`, `compile-receipt.json`, `test-receipt.json` and `runtime-receipt.json` under that directory. The final runtime receipt SHA-256 is `48454f147f2739087d15e9e9f5ea655e2280a3c8ea66a36b1ba654b2bc286a47`. The linked-image gate uses the explicitly recorded compiler patch on Temurin 21.0.12.1; it is separate from the integrated fork image below. Cross-frontend fixture checks are finite ingress/index evidence, not full Eclipse or Jackson vendor/platform admission.

## Integrated M3JDK21 image gate

`/tmp/m3-parser-refinement-20261019/emitted-integrated-01/RESULT.json` reports PASS for 20 commands. Its image was relinked from sealed existing source-built M3JDK21 JMODs and the native runtime, with the explicitly compiled compiler owner families and emitted parser module. Runtime uses **no --patch-module**. Payload preservation, original module-hash coverage and class-family custody are recorded; this was not a fresh full JDK/HotSpot source rebuild.

The integrated image passes six compiler suites: shared frontend (70 checks), source sealing (117 checks, 23 digest cases and all 65,536 code units), plugin selection (462 checks/24 cases), projection state (41,959), constant facts (20) and folded image (20,198). It also passes parser framework (316), javac projection (2,559), and consumer smoke.

All four reviewed production sources and their Java tests are byte-identical to this integration's source seals. The final build.py has a later postimage, qualified by the current ordered chain. The current pack also adds the upstream ingress/index owners and fixtures absent from the earlier integrated parser pack. These evidence roles are kept separate; the old integrated artifact is not labeled as the entire final current pack.

The integrated receipt SHA-256 is `6755b629e5a77e4218978820e5a2d0e9e2c266ea68c4a46ae543c3fc8f0fe605`. Its linked `lib/modules` SHA-256 is `d852163d8569ccfe6887615d0be84b6803cb5f463fc4f2f9e0638daf5a62d84d`.

## Measured tradeoffs

### Earlier parser preflight observations

The measurements under `/tmp/m3-parser-refinement-20261019/candidate-preflight` are **preflight observations**, not final current-pack throughput or whole-VM guarantees. JIT observations reported:

| Operation | Original allocated bytes/operation | Candidate bytes/operation | Original → candidate ns/operation |
| --- | ---: | ---: | ---: |
| Kind lookup, 200,000 repeats | 488 | 0 | 363 → 75 |
| Source seal, 32 repeats | 945 | 8,816 | 940,609 → 216,703 |
| Javac projection, 16 repeats | 764,080 | 607,430 | 4,201,804 → 4,370,313 |

The detached shape remains 8,195 nodes, 32,614 UTF-16 units and 131,120 primitive payload bytes. Interpreter logs are retained separately. The source-seal buffer intentionally spends bounded temporary allocation to reduce digest calls. The projection JIT timing was slightly higher in this sample despite lower allocation; no blanket speedup is claimed.

### Projection-cache allocation on the integrated image

The integrated proof ran the same probe with -Xint on original and emitted M3JDK21 images, reporting eight samples per case. These are reported allocated bytes/event:

| Distinct projections/event | Original success | Emitted success | Original cached failure | Emitted cached failure |
| --- | ---: | ---: | ---: | ---: |
| 1 | 344 | 136 | 360 | 136 |
| 8 | 344 | 312 | 472 | 312 |
| 64 | 1,912 | 2,424 | 2,936 | 2,424 |
| 1,024 | 32,696 | 37,112 | 49,080 | 37,112 |
| 8,192 | 262,120 | 295,256 | 393,192 | 295,256 |

**Large successful-only caches allocate more:** at 8,192 projections the emitted case uses 33,136 additional bytes/event, approximately 12.6%. The byte state lane and growth history have a cost when failure wrappers provide no savings. Small events allocate less, and cached failures avoid per-failure wrappers. This is an explicit resource tradeoff, not an across-the-board cache-memory reduction. Raw output is retained in `logs/19-original-cache-allocation.log` and `logs/20-emitted-cache-allocation.log` under the integrated proof.

## Admission boundary

This pass qualifies these concrete owners and the stated parser-pack gates. A fresh full current M3JDK21/HotSpot rebuild, complete jtreg/JCK matrix, other architectures and controlled throughput benchmarking remain outside this evidence. Real Eclipse/JDT vendor qualification is a separate workstream. No claim that every parser or the complete Eclipse platform is fully enhanced follows from these finite checks.
