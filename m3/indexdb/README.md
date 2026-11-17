# M3DB (`com.m3:m3indexdb`)

Status: part of `java.base` since 2026-11-17, packages `jdk.m3.db` and `jdk.internal.m3.db` (not exported; the SPDX
headers are kept). M3JDK is the canonical owner of M3DB (owner decision of 2026-11-17); it is not Synexia migration
residue. This folder keeps the Maven build and the records.

## Location and names

| What | Where |
|---|---|
| Java sources | `src/java.base/share/classes/jdk/m3/db` (20 classes) and `src/java.base/share/classes/jdk/internal/m3/db` (`LegacyFingerprint`), compiled into java.base by the JDK build |
| JUnit tests and fixtures | `test/jdk/jdk/m3/db` (the two hash-family fixtures under `resources/`), run by this folder's `pom.xml` (not by jtreg) |
| Maven build and records | this folder: `pom.xml` builds the same artifact `com.m3:m3indexdb` from the folders above; `qualification/`, `recipe/` and `tasks/` are records and still name the old `m3/indexdb/src` paths and type names |

`M3IndexDB` is `M3DB`, the `M3IndexDb*` types drop that stem (`Artifact`, `SemanticIndex`, `SemanticCodec`,
`SemanticEdge`, `SemanticNode`, `SemanticKind`, `SemanticFingerprint`, `ChunkSimHash`), `M3BuildState` and the
hash family drop the `M3` prefix (`MinHash`, `MinHashLsh`, `LogicHash`, `HammingTopK`, `HashDistances`,
`HashLanes`, `LaneFnv1a`, `LHashTokens`, `ChunkCdc`, `ChunkSha`), and `M3IndexDbFingerprintV1` is
`jdk.internal.m3.db.LegacyFingerprint`; the tests follow their classes.
Exception messages and hash domains keep their text (`M3IndexDB ...`, `m3:indexdb:...`): they are behaviour.

On an M3JDK image the two packages are loaded from java.base and are not exported, so a program on the class path
that uses them needs `--add-exports java.base/jdk.m3.db=ALL-UNNAMED` (and
`--add-exports java.base/jdk.internal.m3.db=ALL-UNNAMED` for `LegacyFingerprint`); without it the first access fails
with `IllegalAccessError`. The application class loader resolves both packages in java.base, so on an M3JDK the
class-path classes of this artifact and its same-package tests are never loaded: run the Maven build on a JDK whose
java.base does not contain them (any stock JDK 21). `make/m3/indexdb/test-java-base.sh` runs the same tests on a
java.base patch (JUnit on the boot class path).
