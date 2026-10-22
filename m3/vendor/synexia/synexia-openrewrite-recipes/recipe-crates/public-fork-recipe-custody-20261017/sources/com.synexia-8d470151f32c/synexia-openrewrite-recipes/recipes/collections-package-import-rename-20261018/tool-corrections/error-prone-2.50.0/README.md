# Error Prone 2.50.0 Guava namespace receiver

The Guava fork keeps its original native Error Prone checks. Error Prone's official
2.50.0 implementation recognizes several Guava types by their original names.
This pinned receiver adds the fork identities to those existing rules.

The receiver retains every original known immutable and mutable entry. An
immutable alias copies the original `AnnotationInfo.containerOf()` parameters
and changes only its type identity. In particular, an immutable list containing a
mutable element remains an error. Existing explicit entries take precedence.
The lock analyzer additionally recognizes the renamed Monitor guard and leave
operation, and the same immediate Iterables lambda methods as the original rule.
Original identities remain supported.

Only two licensed official source owners are overlaid. The builder pins their
corrected bytes and the original core JAR/POM, compiles against the exact
Maven-resolved 2.50.0 dependency closure, then preserves every other original JAR
entry. The new version is `2.50.0-m3-guava-namespace-20261018`.
The original published artifact and cache entries are never overwritten.
The parent dependency management and upstream private pins remain unchanged.

## Rebuild

Use a genuine JDK 27 and Maven with the normal verified TLS/proxy settings.
Set `OWNER` to this directory, `CACHE` to a task Maven repository, and
`OUT` to a fresh output directory. Resolve the unmodified owner closure:

```sh
mvn -B -f "$OWNER/pom.xml" -Dmaven.repo.local="$CACHE" \
  org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
  -Dmdep.outputFile="$OUT/original-classpath.txt"
python3 "$OWNER/build_receiver.py" --cache "$CACHE" --jdk "$JDK27" \
  --classpath "$OUT/original-classpath.txt" --output "$OUT/receiver"
mvn -B -Dmaven.repo.local="$CACHE" \
  org.apache.maven.plugins:maven-install-plugin:3.1.3:install-file \
  -Dfile="$OUT/receiver/error_prone_core-2.50.0-m3-guava-namespace-20261018.jar" \
  -DpomFile="$OUT/receiver/error_prone_core-2.50.0-m3-guava-namespace-20261018.pom"
```

The builder records each actual dependency artifact SHA-256 and compiler version.
The upstream sources are not presented as strict task classes: they compile with
the upstream API and ordinary compiler diagnostics. The shared task/fixture SDK
suite separately uses `-Xlint:all -Werror`.

## Qualification

`qualify_receiver.py` compiles actual original and renamed Guava classes with the
actual receiver. It requires positive and negative immutable-container and Monitor
cases for each identity. Negative cases must fail the expected named checker,
with exactly one compiler error. It also compares the complete original and
receiver rule maps, proving that the receiver retains every original entry and
adds exactly the namespace aliases with identical container parameters.

The Guava task's `ERROR_PRONE_QUALIFICATION.json` records eight fixture outcomes,
253 preserved original rule rows and 41 exact aliases. Its
`ERROR_PRONE_BUILD.json` binds the actual artifact and dependencies. These checks
are distinct from the clean native JRE and Android suites; their native compiler
options and check severities remain unchanged.


## Public CI reconstruction

The canonical source/build owner can remain private. `materialize_overlay.py`
exports only the eight changed class entries from the qualified build, together
with a pinned manifest, the thin reconstructor and the original upstream COPYING.
The Guava fork carries no copied checker Java source implementation. Its 20,955-byte
class overlay is a derived build input; it is not another checker implementation.

`reconstruct_overlay.py` verifies the official public 2.50.0 core JAR/POM, every
changed class-entry hash, the overlay archive, its own script and the license.
It reconstructs the distinct receiver and requires the exact qualified JAR/POM
hashes before CI can install it. Original-JAR, overlay and notice drift controls
all refuse before creating an output directory. The Guava workflow performs this
reconstruction from public artifacts and has no cross-private-repository checkout,
secret or repository permission requirement.

The exporter also requires the recorded corrected-source pins, upstream/receiver
identity, and qualified JAR/POM hashes to match current canonical provenance
before creating any output. EXPORTER_QUALIFICATION.json records exact valid export
and stale source/upstream/receiver/POM receipt refusal controls.
