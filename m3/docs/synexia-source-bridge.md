# Synexia source bridge into M3JDK21

## Purpose

The bridge is the executable boundary between **source-qualified Synexia code** and the existing
M3JDK21 receiving recipes. It is not a second migration registry and it does not infer that source
code is ready merely because it exists on `develop`.

Synexia first applies and qualifies the owning M3 recipe. Its `M3JdkHandoff` exporter then emits
only target-ready exact bytes. The packet contains source revision, capability IDs, source and
destination paths, source SHA-256, destination preimage, license, recipe identity, contract root
and gate root.

## Destination execution

Each generated named recipe executes in this order:

1. `M3SynexiaHandoffGuardRecipe` validates the packet root, source revision, row accounting,
   payload hashes, target preimages and the exact Java/text receiver manifests.
2. `M3Jdk21HashPinnedSnapshotRecipe` handles Java compilation units using the existing typed
   Java parser and exact-preimage rules.
3. `M3Jdk21HashPinnedTextSnapshotRecipe` handles text and native postimages as exact UTF-8
   snapshots.

The Java snapshot owner now admits crate names beginning with `synexia-`; its JDK22–JDK27 crate
contract is otherwise unchanged.

## What the bridge does not do

- It does not rename packages or rewrite APIs.
- It does not port a Synexia class directly into `java.base` merely because the class compiles.
- It does not treat LeetCode, HackerRank or GeeksforGeeks solutions as code donors.
- It does not mutate C/C++ structurally; native adaptations require a separate parser-aware,
  source-pinned recipe before the resulting bytes are exported.
- It does not close JDK image, HotSpot, JNI, GC, JIT, CDS, JVMTI/JFR, platform or performance gates.
- It does not supersede `m3/docs/name-mapping.json`. A real capability packet must be reconciled
  with that canonical ledger before promotion.

This separation permits polished Synexia implementation atoms to enter the M3JDK21 source tree
mechanically while keeping semantic ownership and product promotion independent.

## Focused gate

```sh
mvn -B -ntp -f m3/tooling/migration-recipes/verification/synexia-handoff/pom.xml verify
```

The fixture proves packet verification, Java and text generation and no-change replay through the
actual retained receiver recipes. Whole-module and whole-JDK gates remain additional requirements.
