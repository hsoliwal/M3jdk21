# M3Lexicons receiver v2

The canonical v1 recipe still reconstructs sixteen historical `com.m3.lexicon` owners. The accepted M3JDK receiver now uses `jdk.m3.lexicon` and contains additional precompute and interned-language owners. Executing v1 against the current tree produces sixteen parallel legacy additions; it does not reconstruct the current receiver.

V2 pins all 33 accepted Java sources at M3JDK21 `ab6ef923dca4d32e1f003db71a800138911b5ffa`: 24 production owners and nine tests/proofs. It includes case/Liang precompute, shared symbols, overflow interning, token ropes/IDs, wire transport, boundary summaries, regex transfer rows and their current proofs. Names, packages, APIs, source text and Apache-2.0 notices are preserved exactly. No production runtime owner is changed. V1 remains untouched as a versioned historical recipe.

## Existing recipe, executed proof

The unchanged Synexia `M3HashPinnedJavaSnapshotRecipe` applies the exact manifest and its existing co-parse mode. Two accepted test templates use the engine's existing, explicitly marked M3ResourceEnvelope representation. Their decoded UTF-8, length and SHA-256 remain exact; corrupted envelopes are refused. The original token-loop/ID fixture text is preserved, with public source blob pins in proof/RESOURCE_ENVELOPE_DECISION.json.

Every generated source is checked against its accepted Git blob; the current tree and a rebuilt tree are fixed points; a foreign occupant is refused.

Maven/RAM qualification passed five recipe JUnit tests and fourteen existing receiver JUnit tests. All 33 generated sources compiled under Java 21 with `-Xlint:all -Werror`. Five unmodified Java proof programs passed:

| Proof | Reported checks |
| --- | ---: |
| M3LexiconReceiverProof | 806799 |
| M3CaseLiangParityProof | 1698185 |
| M3LexiconWireProof | 4315 |
| M3RegexWordTransferBoundaryProof | 203 |
| M3RegexWordTransferCompositionProof | 2734316 |

The composition proof additionally reports 21 character reads, sixteen concurrent tasks and 10,000 queries per task for its specific no-rescan discriminator. This records existing behavior, not a new speedup claim.

## Replay and locations

The canonical crate lives at `hsoliwal/com.synexia:synexia-openrewrite-recipes/crates/m3-lexicons-receiver-v2/`; its identical evidence packet lives at `hsoliwal/M3JDK21:m3/lexicon/recipe/receiver-v2-r244/`. Both repositories receive the descriptor/resources in their existing recipe modules and the recipe test in a normal Maven test source root.

Run `python -I -S -B qualification/replay.py` from this crate on the qualified host. The assembler reads separate, readable source fixtures and verifies every SHA-256 before recreating the exact tested input in RAM; reconstruction and drift refusal were executed. The included POM is the exact Maven execution harness. Provisioned driver, JDK and 47 offline JAR/cache paths are pinned in `qualification/config.json`; another host must qualify its own routes. No dependency downloads occur.

## Scope and custody

This repairs Java receiver reproducibility. Full SYNHIER conversion/admission, native parity, other-platform qualification, and the whole reactor were not run. Existing native proof sources are compiled but their native entry points are not invoked. This is not complete lexicon migration or full hierarchy admission.

New runtime generation remains pending under the user's local Chrome rule; no browser was connected and no generation calls occurred. This slice deterministically replays already accepted source through an existing recipe.

The 545 MiB task cap remained intact: 571469824 bytes of sealed read-only F input plus one exclusive 4096-byte K temporary POM, removed after child reaping. K remained above the 8 GiB reserve, with zero retained build files. Source-only follow-ups are recorded separately.
