# JCC receiving change plan

## Status and selected inputs

This is the documentation-first plan for a bounded JCC receiving change. It does not change the canonical mapping, install a source export, or establish a new passed gate. Implementation and verification results will be recorded separately after execution and review.

The source preimage is [hsoliwal/com.synexia at 0b8dc32b9e8a616b7b7141bbdd88722839dc64bc](https://github.com/hsoliwal/com.synexia/tree/0b8dc32b9e8a616b7b7141bbdd88722839dc64bc), with root tree `3a270e543f9a547582ebda8ac8b69bfd134c41be`. The destination preimage is [hsoliwal/M3jdk21 at da958d00d24154c0db87beca0ec80a7df2b43b73](https://github.com/hsoliwal/M3jdk21/tree/da958d00d24154c0db87beca0ec80a7df2b43b73), with root tree `548f3d7c434c49c3989fc722f34ebba2ba9f0c5a`. A later source documentation commit does not qualify the production export. A later source implementation commit requires exact source and proof rebinding before any acceptance state advances.

The inspected destination mapping contains 44 operational records and 20 gate records. Its incomplete repository coverage and historical global observations remain meaningful. Passing structural validation cannot establish migration completion.

## Proposed scope

The operational authority remains [name-mapping.json](name-mapping.json), governed by [name-mapping.schema.json](name-mapping.schema.json) and [migration.py](../migration/migration.py). The proposed increment adds `synexia.jcc-recipe-laboratory` and `synexia.jcc-java-jni-regression` in the schema's actual `blocked` lifecycle, with empty capability test claims. Existing target owners are reuse candidates with explicit domain and API differences.

The one permitted existing-row reconciliation is `synexia.counterpart.MIndexJvmDescriptor`. Its target Java remains unchanged and its status remains `implemented-unverified`. The other 43 existing operational records, all 20 gates, legacy mappings, global observations and incomplete coverage flags are preserved exactly.

The planned four materialized outputs are:

- `m3/docs/name-mapping.json`: the reviewed canonical postimage.
- `m3/docs/jcc-source-handoff.md`: the capability, interface and proof crosswalk.
- `m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json`: selected source/target identities, historical Descriptor record and separate acceptance flags.
- `m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv`: explicit disposition obligations for every observed source root entry.

The installation uses the retained `M3Jdk21HashPinnedTextSnapshotRecipe` and the existing `recipe.py` sealed-install format. The exact preimage/postimage crate and plan must describe the same four outputs. Guards bind the schema, validator, replay owner, text transformer, owning POM, Descriptor target and Descriptor recipe. The existing `migration.py extend` command is a one-time initializer and is not an update mechanism for this existing extension. The co-retained `lineage.py` uses a different historical dialect and is not a replacement authority.

## Root coverage obligations

The current source root tree has 343 entries: 255 trees, 87 blobs and one gitlink. Its unchanged root POM blob `98889a040aad5cc3c82f782bb4b173d118207988` declares 201 distinct module paths: 200 project declarations and one profile declaration. Of these, 190 are direct root paths and 11 are nested paths in eight root trees. Four nested containers are also direct modules; four are additional containers. The union is 194 root trees referenced by declarations.

Each root entry receives its own derived coverage-obligation row linked to `family.entire-source-closure`. A directory name does not make it a Maven module, semantic owner or completed mapping. This root accounting does not establish the descendant file denominator, generated-producer closure, native-consumer closure or whole-repository semantic dependency coverage. The 503 exact source/resource bindings from the current source preflight are a separate scoped acquisition.

## Existing receiving owners and bounded executable work

`M3PureIntAtomEligibility`, `M3PureIntConvergenceRecipe` and its atomize/patternize/document children retain their private static scalar-int domain. Existing `MemJava` and `M3IntRecipeProjectTest` own in-memory Java21 compilation and project correctness checks. A receiving fixture may be independently authored in that established domain and tested against immutable inputs, fresh recipe/context instances and every intermediate output. It must not import the source Mastery engine, replace the target compiler, broaden eligibility, or inherit source pass counts.

The Java `M3Java21ConvergenceRecipe` wrapper and the declarative convergence catalogue have different child lists; each activation path must be identified exactly. String and comment opacity, calls, fields, arrays, reference values, regex observations and JNI behavior require their own explicit unchanged, refusal or wider-scope obligations. Tooling fixture development does not admit the source export or qualify the JDK product.

## Descriptor reconciliation rationale

The old recorded target SHA256 `df17c48e54de060060e4393e5c2f3e8e2f068bad38a69576a82709cc6d4cbdb9` matches the target introduced at [f74592e363466a59b391fbbe47500c7985c9a691](https://github.com/hsoliwal/M3jdk21/commit/f74592e363466a59b391fbbe47500c7985c9a691). The former `sync.target_revision` `3ec445df10a3e8fc74aa983181f0700ac7bfbcad` is its parent; that exact target path is absent there. Preserve both the original candidate reference and the independently verified historical target in the existing lineage fields, and retain the complete previous record in the binding receipt.

[Commit 39702564163c017f9dc0350a305b032b06274857](https://github.com/hsoliwal/M3jdk21/commit/39702564163c017f9dc0350a305b032b06274857) changed the target's class-descriptor implementation to `Objects.requireNonNull(type, "type").descriptorString()`. This retained target-only hidden-class correction has current SHA256 `1333a9cf8b96b8c919d1c098899648d0a82cdce7e775167fe3ff2e571c54ec67`. The historical and current selected Synexia source bodies remain byte-identical and retain the older algorithm. Existing public target signatures are unchanged.

Bind the active recipe to the actual `jdk22-descriptor` manifest and `com.m3.rewrite.backport.Descriptor`; the crate label makes no Java22 or JEP claim. Preserve the older cb receipt and separately reported Descriptor experiments as historical evidence. Current source/target parity, complete owning-module coverage and a matching JDK image/runtime gate are not established by a metadata update.

## Verification and advancement requirements

The focused Maven/JUnit verifier must exercise the actual retained text transformer: all four exact outputs, a fresh fixed point, atomic refusal for changed or occupied inputs, duplicate and missing required paths, scan-to-visit integrity, preservation of unrelated records and gates, full historical Descriptor lineage, and explicit false acceptance flags. Use the owning Java21/OpenRewrite/JUnit/tooling versions.

The existing sealed installer must independently verify check, apply, no-op replay, rollback and replay; corrupted guards, mixed snapshots and foreign edits must refuse before mutation. Run ordinary whole-map `migration.py validate` with the previous canonical mapping and the actual pinned target/recipe/receipt closure. Do not waive target-hash validation to hide the inherited drift. The separate `complete` command must continue to refuse the existing incomplete coverage and gates and the new blocked records.

Source known, source tested, mapping reviewed, export admitted, destination capability materialized, destination gates and remote read-back are separate dimensions. JCC export admission, JCC destination capability materialization, destination gates and remote delivered read-back remain unestablished in this plan.

The source's actual verification gate order is DIFF, POLICY_LINT, COMPILE, STATIC_ANALYSIS, UNIT_TEST, INTEGRATION_TEST, API_CONTRACT, FULL_BUILD, RUNTIME, REPLAY and SOURCE_HASH_PRESERVATION. It is distinct from eleven ordered scope passes and the destination's 20 stored gates. Admission requires the owning source checkpoint, complete strict coverage, exact execution root, ordered successful gates, restored develop-bound history and nonempty fully patternized admission. No such checkpoint execution is claimed here.

Maven/OpenRewrite/JUnit remain tooling dependencies. Java21 grammar and types, public contracts, classfile and VM behavior, JNI ABI and MIndex payload authority remain under their existing owners. Final publication requires reviewed diffs, exact executed receipts and read-back of the committed destination objects.
