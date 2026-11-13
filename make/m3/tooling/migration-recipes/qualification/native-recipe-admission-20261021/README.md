# M3JDK21 source-built recipe dependency admission — 21 October 2026

**Owner:** `hsoliwal/M3jdk21`'s existing Maven/OpenRewrite proof infrastructure. M3JDK21 remains the one Java/HotSpot/JNI product. This repair does not introduce an M3 runtime compatibility layer or alter public collection APIs; it repairs **recipe qualification**, required by every later per-file behavioral-atom refinement.

## Failure reproduced from GitHub Actions

The `recipe-maven-admission` job on PR #1545 called `mvn -f m3/pom.xml -pl tooling/migration-recipes -am ... test` without first building the repository's vendored source producers. Maven failed to resolve `rewrite-java:8.17.1-m3-source-1`, `rewrite-java-21:8.17.1-m3-source-1`, `rewrite-test:8.17.1-m3-source-1` and `junit-jupiter:5.10.2-m3-source-1`. All four have exact source modules under `m3/vendor/third_party` and are deliberately unpublished source-pinned coordinates. The correct fix is to install the pinned **source producers first**, not to change dependencies to released binary artifacts, suppress the Maven failure, or loosen source SHA guards.

## Algorithm and qualified process

`source_closure.py` is a deterministic, read-only dependency-graph **admission program**, not a new OpenRewrite engine. It parses Maven source reactor module POMs, demands the four exact root coordinates, walks all `*-m3-source-1` direct and transitive dependencies, and fails closed for an unknown source GAV, duplicate coordinates, unsafe source path or dependency cycle. Each admitted module must have the existing `verify_source.py` SHA/provenance verifier. Optional `--verify-sources` runs those exact owner verifiers.

CI now runs:
1. Seven hermetic no-network unit tests for source closure, root-pin integrity and fail-closed behavior.
2. `python3 source_closure.py --root . --verify-sources`, storing a sorted tab-separated GAV/module receipt.
3. `mvn -B -ntp -f m3/vendor/third_party/pom.xml -pl rewrite-java,rewrite-java-21,rewrite-test,junit-jupiter-5.10.2 -am -DskipTests install`. Maven remains authoritative for actual source-build dependency order and artifact composition; it is **not** an external SDK layer.
4. For each installed source module from the receipt, verify the resulting `target/{artifactId}-{version}.jar` via its original module `verify_source.py`.
5. Only then execute the unchanged target Java/JNI recipe admission `M3NativeRecipePathAdmissionTest` in the M3JDK21 Maven control reactor.

No classpath substitutions are performed, and the original native C17/JNI contract proof remains independent. If a source-producer fails, **CI must remain red** until the source/build producer is repaired and its own provenance is qualified.

## Source receipt and focused evidence

`SOURCE_CLOSURE.sha256` pins the exact helper and tests. Compare from repository root with:

```bash
sha256sum -c m3/tooling/migration-recipes/qualification/native-recipe-admission-20261021/SOURCE_CLOSURE.sha256
python3 -m unittest discover -v \
  -s m3/tooling/migration-recipes/qualification/native-recipe-admission-20261021 \
  -p test_source_closure.py
```

The seven hermetic tests passed locally on Python 3. This does not claim the complete vendored source producer reactor or Maven/OpenRewrite replay has passed yet. The new GitHub workflow must verify that. `CATEGORY_REVIEW.tsv` documents the problem categories, selected graph algorithms and deliberately rejected binary-shortcut options. HackerRank graph traversal is an algorithmic pattern only; no third-party code copied.

## Remaining product contracts

A successful source build proves dependency custody, not exact Java/JNI semantics. M3JDK21 retains its mandatory Java 21 API contracts, original JNI symbols/GC behavior, full-width masks, parent-owned dynamic arrays, filtered/sorted live wrapper semantics and matched OpenJDK/HotSpot jtreg/JCK acceptance. Android ART and NDK performance proof requires additional ARM64 CPU, RSS/PSS, energy and startup measurements.
