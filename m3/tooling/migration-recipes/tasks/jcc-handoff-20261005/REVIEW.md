# JCC receiving packet: reviewed candidate epoch 2

This candidate adds the JCC receiving ledger and an original, bounded executable tooling fixture. It is ready for draft review with source export, complete module coverage and JDK acceptance explicitly blocked. Source binding remains [com.synexia `0b8dc32b9e8a616b7b7141bbdd88722839dc64bc`](https://github.com/hsoliwal/com.synexia/tree/0b8dc32b9e8a616b7b7141bbdd88722839dc64bc); final source output qualification will be reconciled in a later additive, validated commit.

The inspected destination production preimage is [M3jdk21 `da958d00d24154c0db87beca0ec80a7df2b43b73`](https://github.com/hsoliwal/M3jdk21/tree/da958d00d24154c0db87beca0ec80a7df2b43b73). The separate documentation-first plan was published and read back at [d07e63755a31b21d5a8d232fa310d99578fff36f](https://github.com/hsoliwal/M3jdk21/commit/d07e63755a31b21d5a8d232fa310d99578fff36f). That commit changed no registry or product file. The reviewed implementation packet does not infer a remote registry commit, PR state or CI result before those are observed.

## Canonical mapping and scope

The existing `m3/docs/name-mapping.json` remains the only operational mapping authority. Its actual schema, validator, replay owner, retained compiler and atom owners, owning POM and workflows are unchanged.

| Mapping change | Recorded state and evidence |
| --- | --- |
| `synexia.jcc-recipe-laboratory` | New `dependency-reuse` record, `blocked`; exact selected source and retained destination owners plus an independently authored candidate fixture. |
| `synexia.jcc-java-jni-regression` | New `pending` kind, `blocked`; Java/regex/JNI applicability and target owners remain unresolved. |
| `synexia.counterpart.MIndexJvmDescriptor` | Existing adapter remains `implemented-unverified`; current target/recipe identities and historical lineage reconciled. |
| Other 43 existing records | Preserved exactly, including their previous incomplete states and evidence. |
| All 20 gates and every non-record global | Preserved exactly; no source inventory, gate or observation flag advanced. |

The two JCC capability `tests[]` arrays remain empty. Their finite tooling receipts are separately scoped; they cannot be read as source-export or whole-JDK proof. No source Mastery engine, duplicate compiler, new eligibility mechanism, second registry, automatic cross-repository scheduler or product dependency is introduced.

The derived source root ledger accounts for all 343 root entries: 255 trees, 87 blobs and one gitlink. The root POM has 201 distinct declarations, comprising 190 direct paths and 11 nested paths across eight root trees. Four of those containers also appear as direct modules; the union is 194 referenced root trees. Descendant file and semantic dependency completeness remain unestablished. See the [handoff](../../../../docs/jcc-source-handoff.md) and the canonical binding/coverage receipts linked there.

## Actual recipe and installer

The retained `M3Jdk21HashPinnedTextSnapshotRecipe` executes crate `jcc-handoff-20261005`; the named declarative entrypoint is `com.m3.rewrite.backport.JccSourceHandoff`. The existing `m3/migration/recipe.py` uses the matching `m3.sealed-install/1` plan. The reviewed epoch is `jcc-handoff-20261005/2` with seal `70283e86d30abf8e30a09b10d77be40f99160371a98927dc151d680e3c2e4a87`.

Both mechanisms describe the same four exact output paths: the canonical map, handoff document, source/destination binding receipt and every-root coverage ledger. Seven unchanged guards bind the schema, validator, installer, text transformer, owning POM, Descriptor Java target and Descriptor recipe manifest. The canonical map preimage is SHA256 `67d40925b97a38c3c1a85f337806524ac590a540bd39647b3e4b0988f2ae3fbb`; the epoch 2 postimage is `536af76dd7b7b7217707662c2f1437af4adaa53af898627b0884e0b4f4603ace`.

The local candidate was first restored to its exact canonical preimage through epoch 1's installer, then the reviewed epoch 2 resources were staged and applied through the same retained owner. Epoch 2 replay is unchanged on a second application. Original local epoch receipts remain separate historical work artifacts; they have not been relabeled as the final epoch's tests.

## Executed verification

| Verification | Exact result | Practical boundary |
| --- | --- | --- |
| Whole canonical `migration.py validate --previous` | Pass on all 46 records and the acquired target/recipe/receipt closure. | Structural, target-hash and historical lineage validation; not migration completion. |
| Separate `migration.py complete` | Expected refusal, exit 2; output retains completion blockers. | No failed or pending gate was suppressed. |
| Focused text-recipe Maven `clean verify` | 8 tests; zero failures, errors or skips. | Actual text transformer, named activation/serialization, exact replay, unchanged fresh pass, blocked flags and refusal controls. |
| Actual sealed-packet Python tests | 6 tests pass, including 81 refusal checks. | Exact four-output lifecycle, rollback/replay, every guard, required/occupied inputs, mixed states and foreign edits; every refusal preserves files, modes and directories. |
| Original receiving fixture Maven `clean verify` | 4 tests; zero failures, errors or skips. | Finite original two-file scalar-int and string-opacity fixture on six unchanged destination owners. |
| Existing migration/installer owner regressions | 26 and 28 tests pass on their unchanged owner/schema bytes. | Existing owner behavior; final packet-specific tests are recorded separately. |

The raw commands, input seals, output hashes, logs and Surefire XML are retained under `ledger-e02`, `text-maven-e02`, `receiving-e02` and `owner-regressions`. Focused Maven uses Java 21.0.2, Maven 3.9.12, OpenRewrite 8.17.1, JUnit 5.10.2, compiler plugin 3.13.0 and Surefire 3.2.5. The ordinary validator used the required `jsonschema` 4.26.0 dependency. No owning dependency pin was replaced.

The original [receiving fixture contract](receiving-e02/CORPUS_CONTRACT.md) defines four private scalar-int leaves over 113 deterministic pairs. A-only and P-only retain their actual different outcomes; AP and PA converge. Each intermediate and quiet sweep compiles using the existing `MemJava` and is compared to both an immutable original and independent arithmetic expectations. Null/string-null, code-looking and marker-looking strings, empty text, Unicode, source and member surfaces, loader-local state, and valid-but-wrong mutants are covered.

Independent review found that the first fixture epoch's `Map.of` iteration could make a supposedly reversed run repeat the same order. The accepted revision explicitly constructs ascending and descending `LinkedHashMap` inputs, asserts their differing orders, and executes and verifies both. Revised test, prose and inputs were frozen before the successful second run. The exact fixture SHA256 is `b034673ea218515cc12ab0b4173eabf06732f29e8a456be45b5ba0d00489ab39`; its mapping commit remains null with `candidate` revision role. A later commit read-back is separate evidence, avoiding self-referential commit hashes.

## Descriptor history is retained, not promoted

The [comparison identities](descriptor-review/COMPARISONS.json) and [target diff](descriptor-review/target-correction.diff) show the inherited drift. The source is byte-identical at historical `7b5d45bea2fce0a042e4b85347c1eb4116846c55` and selected `0b8dc32b...`, with SHA256 `0e0ddfc53acaeb28137319fd20ee5005f9d7231949ebd3106193c287f13be9b8`. Its [source diff](descriptor-review/source-unchanged.diff) is intentionally empty.

The former target hash `df17c48e54de060060e4393e5c2f3e8e2f068bad38a69576a82709cc6d4cbdb9` is verified at `f74592e363466a59b391fbbe47500c7985c9a691`. The former sync target `3ec445df10a3e8fc74aa983181f0700ac7bfbcad` is its parent and lacks that target path. Both the original candidate reference and verified historical target remain in the existing lineage fields, and the complete previous record remains in the binding receipt.

Retained commit `39702564163c017f9dc0350a305b032b06274857` changed `type(Class)` to `Class.descriptorString()`, preserving the five public signatures while correcting hidden-class spelling. The current target SHA256 is `1333a9cf8b96b8c919d1c098899648d0a82cdce7e775167fe3ff2e571c54ec67`. The actual retained `jdk22-descriptor` recipe manifest is now bound by its current bytes; its historical crate name does not change the Java21 language contract. This candidate edits no Descriptor Java and runs no new Descriptor behavior proof. Historical cb/Descriptor receipts retain their original scope, and module/image requalification remains explicit.

## Full-module and product gates remain visible

The [exact frontier](owning-module-frontier/REVIEW.json) rehashes all 143 tree objects in the complete observed 747-entry migration-recipes tree. Of 605 tracked non-tree inputs, 32 bodies are acquired and 573 are not. Missing default inputs include 17 of 34 main Java files, 40 of 45 test Java files, 332 of 335 main resources and all 41 test resources. These are exact acquisition gaps, not inferred deletions or omitted scope.

The selected offline repository lacks the pinned JaCoCo 0.8.15 POM and JAR. The owning module still requires its scope/atom BUNDLE to reach both 0.99 LINE and 0.99 BRANCH coverage. Its default `clean verify` was not run against the incomplete overlay, and neither focused POM replaces it. A full checkout and exact dependency closure are prerequisites for that gate. Existing GitHub workflows can report their own observed results after publication; no new workflow or dispatch is added here.

Source checkpoint/export admission, Java/JNI capability acceptance, JDK build/image/runtime proof, full descendant inventory and final committed capability read-back remain pending. The existing eleven source verification gates are separate from the destination's twenty gates and the finite local test counts. Further source work should arrive in an additive, exactly rebound and validated packet; this candidate makes no automatic synchronization promise.

Before publication, the coordinating reviewer separately observed destination master at `6510485ad7216ac27dc7f7139b96bcddeb9e030d`, while the canonical mapping still had its inspected `a9c2b967920add39044066e815a7dbb1384c4b87` blob. Source mainline also advanced beyond the selected source. Those freshness observations do not rebind this packet's source `0b8dc32b...` or destination-owner `da958d00...` proofs. Integration with newer heads requires its own reconciliation and observed checks.
