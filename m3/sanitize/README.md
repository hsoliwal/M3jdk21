# m3/sanitize: self-contained build gate and donor repoint tools

M3jdk21 is standalone and self-sufficient: the M3 modules build and test from this repository plus a JDK, with no
`~/.m2`, no network and no sibling checkout. Third-party donor code is absorbed into M3 owners first and its vendored
copy is dropped only after the absorption is proven. These tools measure and move that state. CPU only: `bash`,
`awk`, `sed`, `grep`, `find`, `javac`, `java`. No Maven.

## Tools

| tool | purpose |
| --- | --- |
| `tools/self_contained_gate.sh [plan\|compile\|test]` | Walks the M3 control reactor (`m3/pom.xml`: default modules and every profile's modules), classifies every pom reference against the poms on disk, compiles each reactor module and the in-repo providers of its dependencies with `javac`, and runs each module's tests on the JUnit Platform launcher built from `m3/vendor/third_party` sources. Lists every external reference. |
| `tools/pom_model.awk` | Lexical pom model (RS=`<` tokenizer + element stack): coordinates, parent, properties, modules, dependencies, plugins, extensions, repositories, source roots. |
| `tools/pom_resolve.awk` | Effective model over the pom model: parent chain, property interpolation, dependencyManagement / pluginManagement versions, reactor walk, provider classification, transitive in-repo closure, compile order. |
| `tools/SelfContainedTestMain.java` | Test runner on the JUnit Platform launcher API (surefire default includes); writes a TSV summary, no stdout. |
| `tools/repoint_chain.sh <donor-old> <donor-new> [consumer-pom ...]` | One version-duplicate repoint chain: consumers found from exact pom pins, javac gate of every consumer with its full in-repo closure before and after the repoint (PLAIN or DIFFERENTIAL), `git rm` of the old copy, proof by exact pins and directory mentions. HELD (exit 2) restores every touched path. |

## Classification (gate)

| class | meaning |
| --- | --- |
| `REACTOR` | provided by a module of the control reactor (exact groupId:artifactId:version) |
| `REPO` | provided by another pom on disk (normally `m3/vendor/third_party/<donor>`), exact coordinates |
| `REPO_DRIFT` | the repository holds that groupId:artifactId only under another version: Maven would resolve it outside. The gate compiles against the nearest in-repo version (`<v>-m3-source-1` first) so the drift is measurable, and counts it as external |
| `EXTERNAL` | nothing in the repository provides it |
| `UNRESOLVED` | a `${property}` the model cannot resolve |
| `OUTSIDE_PATH` | a source root, `systemPath` or property path outside the repository (`${user.home}`, `${env.*}`, absolute, `../` above the root) |

Evidence copies never provide: poms under `qualification/`, `verification/`, `evidence/`, `snapshots/`,
`receipts/`, `history/`, `retained-variants/`, `upstream-excluded/`, `META-INF/maven/`, `.m3/`, `.superset/`.

Categories in `EXTERNAL_REFERENCES.tsv`: `DEPENDENCY`, `TEST_DEPENDENCY`, `TEST_RUNTIME` (no in-repo JUnit launcher),
`PARENT`, `PATH` count toward the verdict (the default compile/test path). `BUILD_PLUGIN`, `IMPLICIT_PLUGIN`
(lifecycle plugins bound by `jar` packaging), `PLUGIN_DEPENDENCY`, `PROFILE_DEPENDENCY` and `REPOSITORY` are listed
as the Maven-tooling queue: the gate itself needs none of them.

## Verdict

`self-contained build: yes` when, using only repository paths and the JDK: no external reference on the default
compile/test path, every compile unit compiles, every reactor module's tests pass, and no classpath entry lies outside
the repository or the gate's own output directory. Test JVMs run fenced: `user.home` and `java.io.tmpdir` inside
`OUT`, HTTP/HTTPS/SOCKS proxies pointed at a closed local port.

## Run

```bash
OUT=/path/outside/repo JDK=/path/to/jdk-21 bash m3/sanitize/tools/self_contained_gate.sh plan     # references only
OUT=... JDK=... bash m3/sanitize/tools/self_contained_gate.sh test                                 # full gate
OUT=... JDK=... UNITS_ONLY="m3/arrays/pom.xml m3/collections/pom.xml" bash .../self_contained_gate.sh test
```

In a sparse clone, `plan` writes `NEEDED_PATHS.txt`; add those paths (their `src/main/java`, `src/test/java`,
resource roots) to the sparse set before `compile`/`test`. A unit whose directory is not on disk is reported
`NOT_MATERIALIZED`, never skipped silently. `KEEP_CLASSES=1` keeps class output (default: deleted at the end).

Outputs (in `OUT`): `REACTOR.tsv`, `REFS.tsv` (every reference with class and provider), `EXTERNAL_REFERENCES.tsv`
(the work queue), `EDGES.tsv`, `UNITS.tsv`, `SOURCES.tsv`, `COMPILE.tsv`, `COMPILE_ERRORS.tsv` (file, line, message),
`TESTS.tsv`, `TEST_FAILURES.tsv`, `CLASSPATH_AUDIT.tsv`, `SUMMARY.txt`, `gate.log`.

Records of gate runs are kept under `gate/<date>/`; repoint chain records under
`m3/qualification/sanitize-20261030/`.
