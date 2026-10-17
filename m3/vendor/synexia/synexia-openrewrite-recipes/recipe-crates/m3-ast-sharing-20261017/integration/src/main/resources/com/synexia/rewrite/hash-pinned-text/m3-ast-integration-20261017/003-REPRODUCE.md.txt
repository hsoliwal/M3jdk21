<!-- SPDX-License-Identifier: Apache-2.0 -->
# Reproduce the qualified AST implementation

Run from a checkout of this pull request with a Java 21.0.2 JDK, Maven, Python 3,
GCC, `nm`, `curl` and `unzip` on Linux. Set `JAVA_HOME` and configure Maven's
ordinary repository settings for the environment. The commands use the checked-in
public recipe mirror; they do not require access to a private Synexia checkout.
The Maven recipes use OpenRewrite 8.17.1. Generated classes and native libraries
are local build artifacts, not downloaded M3 implementations.

## Qualify the recipes and build their exact emitted candidates

```sh
export MAVEN_OPTS=-Xmx192m
task=m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017
python3 "$task/integration/verify_target_sources.py" --target "$PWD"
mvn -B -ntp -f "$task/cache/pom.xml" test
mvn -B -ntp -f "$task/frontend/pom.xml" -Dm3.frontend.maxHeap=768m test
mvn -B -ntp -f "$task/frontend/hierarchy/pom.xml" test
mvn -B -ntp -f "$task/frontend/module-wiring/pom.xml" test
mvn -B -ntp -f "$task/frontend/registry/pom.xml" test
mvn -B -ntp -f "$task/integration/pom.xml" test
python3 "$task/frontend/build_candidates.py" --jdk "$JAVA_HOME" \
  --hierarchy-candidates "$PWD/$task/frontend/hierarchy/target/candidate"
```

These tests execute the actual class-backed Java and text recipes, compare exact
postimages, reject drift and missing/duplicate inputs, and require fixed points.
They include the narrow existing-harvester local-scope repair and the module
successors. The builder compiles 103 canonical runtime sources, 10 receiving
facades and four compiler sources with strict Java 21 diagnostics. Its
`frontend/target/ARTIFACTS.json` binds the resulting JARs and source hashes.

## Build the original JNI store and run the contracts

```sh
mkdir -p target/m3-ast-native
curl --fail --location --retry 3 \
  https://www.sqlite.org/2026/sqlite-amalgamation-3510300.zip \
  --output target/m3-ast-native/sqlite.zip
unzip -q target/m3-ast-native/sqlite.zip -d target/m3-ast-native
python3 "$task/cache/build_native_proof.py" --jdk-home "$JAVA_HOME" \
  --sqlite-source "$PWD/target/m3-ast-native/sqlite-amalgamation-3510300"
mvn -B -ntp -f "$task/cache/pom-runtime.xml" test
mvn -B -ntp -f "$task/frontend/runtime-tests/pom.xml" test
python3 "$task/frontend/primitive-image/verify.py" --jdk "$JAVA_HOME"
python3 "$task/frontend/hierarchy/build.py" --stage runtime --jdk "$JAVA_HOME"
mvn -B -ntp -f "$task/frontend/hierarchy/runtime-tests/pom.xml" test
```

The native builder validates the official SQLite source digest, source identity
and header digest before compiling. It regenerates both Java ABI headers and
checks all 16 exports of the unchanged shared native unit. Compilation uses a
fresh temporary directory. The cache suite includes real SQLite close/reopen
tests; the frontend and hierarchy suites exercise exact source/context identity,
lifetime refusal, lazy projection, immutable snapshots and invalidation.

## Check the existing compiler API and both plugins

```sh
patch="$PWD/$task/frontend/target/m3-javac-patch.jar"
plugin="$PWD/$task/frontend/target/m3-ast-sharing.jar"
receipt="$PWD/$task/frontend/target/ARTIFACTS.json"
python3 "$task/frontend/upstream-contract/qualify.py" \
  --jdk "$JAVA_HOME" --patch "$patch" --artifact-receipt "$receipt"
python3 "$task/frontend/upstream-contract/coexistence.py" \
  --frontend-jar "$plugin" --patch "$patch" --artifact-receipt "$receipt"
```

The first command assembles a finite linked image using the exact upstream module
descriptor and test, preserving all unrelated stock JMOD entries and hashes. It
uses the standard `jmod` hash update for the scratch compiler module; hash checks
remain enabled. The second command checks the built-in `M3AST` and external
`M3ASTIndex` providers together. The input receipt must match the actual artifacts
and the four sealed compiler source templates. These are finite compatibility
proofs, not a complete JDK or JCK build.

## Check the optional OpenRewrite visitor

```sh
mvn -B -ntp -f "$task/frontend/openrewrite/pom.xml" test
python3 "$task/frontend/openrewrite/verify.py" --jdk "$JAVA_HOME" \
  --plugin "$plugin" --patch "$patch"
```

The optional fixture checks original javac tree/type/source identity, exact source
printing, class-byte equality, full FLOW admission and refusal of extra source
parsing. It verifies that the standalone OpenRewrite parser was never loaded and
that the core works without OpenRewrite on its classpath. Actual source-completion
attempts are detected; no rollback of such attempted reads is claimed.

## Build the ordinary receiving modules

The existing `m3-precompute` dependency must first be installed from its source
module. In a configured source-artifact environment:

```sh
mvn -B -ntp -f m3/ports/precompute/pom.xml install
mvn -B -ntp -f m3/ports/compiletime/pom.xml install
mvn -B -ntp -f m3/ports/compiletime-openrewrite/pom.xml package
```

The repository's existing compile-time CI also supports upstream ASM 9.6 and
JUnit 5.10.2 through `-Dasm.version=9.6 -Djunit.artifact.version=5.10.2`. The final
normal-module receipt records that mode explicitly; the optional module preserves
these overrides for transitive ASM dependencies. The existing workflow installs
the dependency-free precompute source artifact before the tests.

Enable the real native tests explicitly:

```sh
mvn -B -ntp -f m3/ports/compiletime/pom.xml \
  -Dasm.version=9.6 -Djunit.artifact.version=5.10.2 \
  -Pm3-ast-native \
  -Dm3.cache.native="$PWD/$task/cache/target/native/libsynexia_mindex_swap.so" test
```

The selected profile must fail for a missing native library. No assumption or
conditional skip changes that contract. `frontend/module-wiring/verify.py --help`
describes the complete normal/optional/native verifier. Its optional
`--canonical-checkout` enables a separate canonical-reactor availability probe;
public receiving-module qualification does not require that checkout.

The receiving source files are the already-applied recipe outputs. The individual
packet READMEs document explicit materialization for an exact admitted preimage.
Keep the source and target commit identities, generation receipts and source
hashes when replaying on another checkout. A different JDK vendor can change JAR
metadata; a new run must report its own actual artifact identities and pass the
source-bound admission and runtime gates.
