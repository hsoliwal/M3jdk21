# SAN: whole-Synexia sanitization into M3JDK21

SAN means contract-preserving preparation and integration of the **entire Synexia
estate**, including its non-MIndex names. It extends the existing A3, migration
mapping and whole-JDK work packets. `name-mapping.json` remains the per-capability
authority. This document does not create another registry or declare unreviewed
implementations accepted.

The scope includes Apache projects, Guava, Eclipse projects and the JDK as donor
capability baselines. Apache and Eclipse are portfolios, not single libraries.
Use Synexia's existing foundation collectors and donor catalogue to enumerate
them. A small seed list, a similar class name or an imported JAR is not a superset
proof. The exact library/release/API/platform denominator must accompany every
coverage claim. Keep missing and conflicting capabilities visible.

## Account for every source obligation

The existing `m3/migration/inventory.py` has an explicit repository scope:

```sh
python3 m3/migration/inventory.py --repo /authorized/complete/com.synexia \
  --commit EXACT_40_HEX_COMMIT --scope repository --out /private/inventory.json
```

This hashes every tracked blob and selects every tracked entry. Non-prefix Java,
C/JNI, tests, resources, templates, build files, generated inputs, binaries and
dormant modules cannot disappear because a filename does not contain MIndex.
Symlinks are recorded without following them; submodules and over-budget content
remain selected and blocked. The legacy family scope remains available with its
original selection semantics. Neither mode grants semantic or production admission.

The inventory is a Git transport/accounting adapter. The existing Java
`com.synexia.m3.inventory.M3InventoryMain` remains the semantic producer, and
`M3RepositorySupersetExecutionCli --strict-llm-task` remains the source repository's
canonical tree/recipe/history admission. Maven/OpenRewrite remains the authoring
path. The private whole-estate file ledger stays in the source repository.

## Assign the correct destination

| Capability | Integration destination | Required boundary |
| --- | --- | --- |
| Immutable text, primitive lanes, collections, algorithms and valid precompute | Existing JDK subsystem/internal owner | Preserve public descriptors, null/error/order/identity/concurrency/serialization contracts and the canonical payload owner |
| Class/version metadata and loader-aware facts | Existing Class bridge and defining-loader context | Actual Class identity, weak lifetime, generation/epoch invalidation, module access and verified definition provenance |
| AST, partial AST, parsing, lowering and compiler analysis | Existing compiler/tool module such as `jdk.compiler` | No compiler dependencies in `java.base`; preserve diagnostics, attribution, source maps and evaluation order |
| IO, filesystem, networking, archive/codec and native acceleration | Matching JDK module or explicit provider | Exact format/ABI, overflow, ownership, cancellation, failure recovery and Java fallback parity |
| Domain services, SAP/ETL/agent/UI/framework integrations | Reviewed optional modules/providers in the M3 distribution | Explicit dependency closure and startup/service/native tests; no hidden application dependency in the bootstrap runtime |
| OpenRewrite, Maven, donor inventory and migration tools | Existing authoring/proof tooling | Not a dependency of the OpenJDK product build or runtime |
| New class-file format, incompatible object/identity/linkage/layout semantics | Separate experimental JVM project | Explicit experiment boundary; cannot silently alter the compatible M3JDK21 lane |

These are routing requirements, not automatic acceptance decisions. Refine mixed
modules to the existing semantic atoms, packages and symbols. Preserve existing
JDK class names and APIs; use established concise M3 terms for internal
counterparts. Preserve source compatibility facades in Synexia where needed.

## All lean collections are mandatory scope

Every lean collection is required in the M3JDK21 programme, including all primitive
variants, packed/dense/sparse/segmented storage, generic compatibility adapters,
concurrent queues/maps/sets, frozen forms, indexes, selection/shapes, JNI, resources
and tests. The existing `m3/collections` module and module-pack assembly remain the
owners; do not create another competing collection hierarchy. Preserve earlier
ported APIs while reconciling the stronger current source/history atoms.

The companion source receipt accounts for 301 main Java, 67 test Java and eight
native files in the pinned common collection tree. The existing port provenance
matches 21 of those paths, including tests/native; matching provenance is not
current-source equivalence. Mat collections, MIndex collections, Eclipse-backed
dense owners and Synexia lazy-event/progress dependencies remain explicit closure
obligations. None may be silently dropped to make a narrow JAR compile. This SAN
packet does not itself port collection runtime bodies or claim that all are linked
into an M3JDK21 image. Follow [the collection programme](whole-jdk-collections.md)
and its compatibility/replacement proof requirements for every promotion.

## Serial sanitization passes

1. Pin source/target trees, all relevant history, donor paths and source obligations.
2. Resolve Java/native/generated dependency and API owners; retain unresolved edges.
3. Compare capabilities and exact semantics across existing Synexia, Apache, Guava,
   Eclipse and JDK owners. Review catalogue, LeetCode, HackerRank and GeeksforGeeks
   categories in order; record contract mismatches instead of copying editorials.
4. Reconcile license/notice and source ownership for every shipped dependency.
   Original Synexia ports may retain Apache-2.0. Preserve existing GPL/Classpath,
   EPL, BSD and other donor terms; a new package name does not change a license.
5. Improve the existing Maven/OpenRewrite recipe. Bind preimages, postimages,
   absent-before additions, dependency closure, refusal and fixed-point tests.
6. Replace a bounded internal atom while preserving all earlier proven behavior
   and work reuse. Preserve primitive density; no retained per-entry wrappers or
   competing canonical spelling stores.
7. Escalate proof from file/package through module, reactor and exact JDK image.
   Include Java oracle parity, JNI lifetime/error checks, applicable jtreg,
   GC/JIT/CDS/JVMTI/platform modes, and separate cold/preparation/warm cost evidence.
8. Promote serially only after admission; update the existing mapping and retain
   fallback/rollback. Never rebase or rewrite `develop` history.

No all-pairs precompute or unconditional JNI conversion is implied. A hash narrows
candidates; it does not prove equality, compatibility, authenticity or readiness.

## Completion and present evidence

Whole-repository selection is implemented and tested in this packet. Its Java
OpenRewrite recipe reuses `M3Jdk21HashPinnedTextSnapshotRecipe`; the original
inventory remains the accounting owner. This is tooling integration, not a runtime
port. The exact source ledger and expanded donor candidate live in the companion
`hsoliwal/com.synexia` SAN crate.

“Superset” requires a disposition for every enumerated capability and dependency,
with exact-source tests for every accepted compatibility promise and a recorded
reason for every unresolved/blocked item. “All sanitized into the JDK” additionally
requires the delivered distribution and every advertised runtime mode to pass.
File accounting, reference seeds, a successful narrow recipe, or a merged PR
cannot establish either claim on their own. Full semantic inventory, donor API
coverage, source closure, whole reactor and runtime/JIT/AOT integration remain open.
