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

## Portable three-stage bridge

The bridge is intentionally split so source qualification, resource custody and product mutation
remain independently reviewable.

### 1. Export in Synexia

Run the source-side `M3JdkHandoffCli --export`. The result is a new directory containing only
M3JDK21 migration-recipe resources. JAVA destinations are limited to `src/`, `test/` and
`m3/ports/`; TEXT/NATIVE rows cannot target `.java` compilation units.

### 2. Import receiver resources

The destination importer is dependency-free Java 21 and writes only below
`m3/tooling/migration-recipes/src/main/resources`.

```sh
mkdir -p m3/build/synexia-handoff-import
javac --release 21 -Xlint:all -Werror \
  -d m3/build/synexia-handoff-import \
  m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3Jdk21HandoffPaths.java \
  m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/SynexiaHandoffPacket.java \
  m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/SynexiaHandoffImport.java \
  m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/SynexiaHandoffImportCli.java

java -cp m3/build/synexia-handoff-import \
  com.m3.rewrite.backport.SynexiaHandoffImportCli \
  --check /absolute/path/to/handoff-output . synexia-<capability>-v1

java -cp m3/build/synexia-handoff-import \
  com.m3.rewrite.backport.SynexiaHandoffImportCli \
  --apply /absolute/path/to/handoff-output . synexia-<capability>-v1
```

Import preflights the complete exported file set and destination state before writing. It rejects
extra files, unsafe paths, symlinks, payload/packet/alias drift and divergent pre-existing receiver
resources. Re-import of identical resources is a zero-change fixed point.

### 3. Materialize product targets through OpenRewrite

After the migration-recipes module is rebuilt so the imported crate is on its classpath, run the
recipe-backed materializer. It validates the packet guard, loads the actual target files, runs the
existing `M3Jdk21HashPinnedSnapshotRecipe` and
`M3Jdk21HashPinnedTextSnapshotRecipe` in memory, completes the whole preflight, and only then
atomically writes their changes.

The Java entry point is
`com.m3.rewrite.backport.SynexiaHandoffMaterializeCli`. Invoke it through Maven so the retained
OpenRewrite runtime dependencies are on the classpath:

```sh
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml \
  -DskipTests compile \
  org.codehaus.mojo:exec-maven-plugin:3.6.4:java \
  -Dexec.mainClass=com.m3.rewrite.backport.SynexiaHandoffMaterializeCli \
  -Dexec.args="--check /absolute/path/to/M3jdk21 synexia-<capability>-v1"

mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml \
  -DskipTests compile \
  org.codehaus.mojo:exec-maven-plugin:3.6.4:java \
  -Dexec.mainClass=com.m3.rewrite.backport.SynexiaHandoffMaterializeCli \
  -Dexec.args="--apply /absolute/path/to/M3jdk21 synexia-<capability>-v1"
```

The importer must run first, followed by this Maven compilation, so the newly imported crate resources
are present on the materializer classpath.

The materializer is an execution shell, not a second transformation engine. It never interprets
Synexia source itself and cannot bypass the packet guard or the typed Java receiver. Existing POSIX
permissions are retained where supported. The repository must be an exclusive writer during
materialization; per-file moves are atomic, but a multi-file packet is not represented as a
filesystem transaction.

### First real bridged owner

`synexia-mindex-joined-streams-v1` binds:

- Synexia revision `aa37ab2d7bb851bb2e86f314225dc43e118df071`;
- source and target SHA-256
  `a4f0e0cd7b3e8701aba21b8b14b068cc2d3822e17dc066daa442cbc580404a80`;
- packet root
  `6f65a22ef31dc83bc1946564effea1a938b29301de8390dcaec2c7bd446b14ad`;
- target `m3/ports/indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedStreams.java`.

The current target is already byte-identical, so the expected receiver result is a zero-change fixed
point. Canonical custody is recorded in `m3/docs/name-mapping.json` as
`synexia.mindex-joined-streams`.

## Focused gate

```sh
mvn -B -ntp -f m3/tooling/migration-recipes/verification/synexia-handoff/pom.xml verify
```

This focused gate covers the packet guard, external resource import, first-apply/fixed-point/refusal
cases, recipe-backed product materialization, and the real joined-streams receiver. It does not
replace full migration-recipes verification, JDK image, jtreg, JNI/HotSpot or platform acceptance.
